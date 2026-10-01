package com.hiro.ulike.hdr.encoder;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** API 33+ single-image P010 encoder boundary. Output is HEVC, not a HEIF file. */
public final class AndroidMain10Encoder {
    private AndroidMain10Encoder() {}
    public static final class Options {
        public final EncoderChoice.Policy policy;
        public final String exactCodecName;
        public final int bitrate;
        public Options(EncoderChoice.Policy policy, String exactCodecName, int bitrate) {
            this.policy=Objects.requireNonNull(policy);
            if(exactCodecName!=null && exactCodecName.trim().isEmpty()) throw new IllegalArgumentException("empty codec name");
            if(bitrate<100_000 || bitrate>400_000_000) throw new IllegalArgumentException("bitrate out of bounds");
            this.exactCodecName=exactCodecName; this.bitrate=bitrate;
        }
    }
    public static final class Result {
        public final String codecName, captureId, geometryId, processingId;
        public final boolean hardwareReported;
        public final HevcProof.Sps bitstream;
        private final byte[] data;
        Result(EncoderChoice.Candidate chosen,P010Frame frame,byte[] bytes,HevcProof.Sps proof) {
            codecName=chosen.name; hardwareReported=chosen.hardware;
            captureId=frame.captureId; geometryId=frame.geometryId; processingId=frame.processingId;
            data=bytes; bitstream=proof;
        }
        public byte[] copyAnnexB() { return data.clone(); }
    }
    static MediaFormat format(P010Frame frame,Options options) {
        MediaFormat f=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC,frame.width,frame.height);
        f.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
        f.setInteger(MediaFormat.KEY_PROFILE,MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10);
        f.setInteger(MediaFormat.KEY_COLOR_STANDARD,MediaFormat.COLOR_STANDARD_BT2020);
        f.setInteger(MediaFormat.KEY_COLOR_TRANSFER,MediaFormat.COLOR_TRANSFER_HLG);
        f.setInteger(MediaFormat.KEY_COLOR_RANGE,isFull(frame)?MediaFormat.COLOR_RANGE_FULL:MediaFormat.COLOR_RANGE_LIMITED);
        f.setInteger(MediaFormat.KEY_BITRATE_MODE,MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        f.setInteger(MediaFormat.KEY_BIT_RATE,options.bitrate);
        f.setInteger(MediaFormat.KEY_FRAME_RATE,1);
        f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,0);
        f.setInteger(MediaFormat.KEY_MAX_B_FRAMES,0);
        return f;
    }
    private static boolean isFull(P010Frame f) { return f.encoding==P010Frame.Encoding.BT2020_NCL_HLG_FULL; }
    public static List<EncoderChoice.Candidate> advertised(P010Frame frame,Options options) {
        requireApi(); Objects.requireNonNull(frame); Objects.requireNonNull(options);
        MediaFormat f=format(frame,options); List<EncoderChoice.Candidate> result=new ArrayList<>();
        for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
            if(!info.isEncoder() || info.isAlias()) continue;
            for(String type:info.getSupportedTypes()) if(type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                try {
                    MediaCodecInfo.CodecCapabilities cap=info.getCapabilitiesForType(type);
                    boolean main10=false,p010=false;
                    for(MediaCodecInfo.CodecProfileLevel p:cap.profileLevels)
                        if(p.profile==MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10) main10=true;
                    for(int c:cap.colorFormats) if(c==MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010) p010=true;
                    boolean vbr=cap.getEncoderCapabilities().isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
                    boolean supported=cap.getVideoCapabilities().isSizeSupported(frame.width,frame.height)
                            && cap.isFormatSupported(f);
                    result.add(new EncoderChoice.Candidate(info.getName(),info.isHardwareAccelerated(),main10,p010,supported,vbr));
                } catch(IllegalArgumentException ignored) {
                    result.add(new EncoderChoice.Candidate(info.getName(),info.isHardwareAccelerated(),false,false,false,false));
                }
                break;
            }
        }
        return result;
    }
    /** Blocking worker-thread call; bounded dequeue waits, timeout, interruption and no precision fallback. */
    public static Result encode(P010Frame frame,Options options) throws IOException {
        requireApi(); Objects.requireNonNull(frame); Objects.requireNonNull(options);
        EncoderChoice.Candidate chosen=EncoderChoice.select(advertised(frame,options),options.policy,options.exactCodecName);
        MediaCodec codec=MediaCodec.createByCodecName(chosen.name); boolean started=false;
        try {
            codec.configure(format(frame,options),null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            requireInteger(codec.getInputFormat(),MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
            codec.start(); started=true;
            long deadline=System.nanoTime()+30_000_000_000L;
            boolean queued=false, eos=false, formatSeen=false;
            ByteArrayOutputStream configuration=new ByteArrayOutputStream(), samples=new ByteArrayOutputStream();
            MediaCodec.BufferInfo output=new MediaCodec.BufferInfo();
            while(!eos) {
                if(Thread.currentThread().isInterrupted()) throw new IOException("encoding interrupted");
                if(System.nanoTime()-deadline>=0) throw new IOException("encoding timed out");
                if(!queued) {
                    int index=codec.dequeueInputBuffer(10_000);
                    if(index>=0) {
                        // getInputImage invalidates the ByteBuffer view: save ONLY capacity first.
                        ByteBuffer raw=codec.getInputBuffer(index);
                        if(raw==null) throw new IOException("P010 input buffer unavailable");
                        int capacity=raw.capacity(); raw=null;
                        Image image=codec.getInputImage(index);
                        if(image==null) throw new IOException("advertised P010 has no writable Image");
                        try { copy(frame,image); } finally { image.close(); }
                        codec.queueInputBuffer(index,0,capacity,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        queued=true;
                    }
                }
                int index=codec.dequeueOutputBuffer(output,10_000);
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if(formatSeen) throw new IOException("output format changed more than once");
                    MediaFormat f=codec.getOutputFormat();
                    requireInteger(f,MediaFormat.KEY_COLOR_STANDARD,MediaFormat.COLOR_STANDARD_BT2020);
                    requireInteger(f,MediaFormat.KEY_COLOR_TRANSFER,MediaFormat.COLOR_TRANSFER_HLG);
                    requireInteger(f,MediaFormat.KEY_COLOR_RANGE,isFull(frame)?MediaFormat.COLOR_RANGE_FULL:MediaFormat.COLOR_RANGE_LIMITED);
                    for(int i=0;i<3;i++) { ByteBuffer csd=f.getByteBuffer("csd-"+i); if(csd!=null) append(configuration,csd.duplicate()); }
                    formatSeen=true;
                } else if(index>=0) {
                    try {
                        if(output.size>0) {
                            ByteBuffer data=codec.getOutputBuffer(index);
                            if(data==null || output.offset<0 || output.size<0 || (long)output.offset+output.size>data.capacity())
                                throw new IOException("invalid codec output buffer");
                            ByteBuffer view=data.duplicate(); view.position(output.offset); view.limit(output.offset+output.size);
                            if((output.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0) append(configuration,view);
                            else { if(output.presentationTimeUs!=0) throw new IOException("unexpected image timestamp"); append(samples,view); }
                        }
                        eos=(output.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    } finally { codec.releaseOutputBuffer(index,false); }
                }
            }
            if(!queued || !formatSeen || samples.size()==0) throw new IOException("incomplete still output");
            append(configuration,ByteBuffer.wrap(samples.toByteArray())); byte[] bytes=configuration.toByteArray();
            HevcProof.Sps proof=HevcProof.inspect(bytes,frame.width,frame.height,isFull(frame));
            return new Result(chosen,frame,bytes,proof);
        } finally {
            if(started) { try { codec.stop(); } catch(RuntimeException ignored) { /* release must still happen */ } }
            codec.release();
        }
    }
    private static void copy(P010Frame frame,Image image) throws IOException {
        Rect crop=image.getCropRect();
        if(image.getFormat()!=ImageFormat.YCBCR_P010 || image.getWidth()!=frame.width || image.getHeight()!=frame.height
                || crop.left!=0 || crop.top!=0 || crop.right!=frame.width || crop.bottom!=frame.height)
            throw new IOException("codec changed P010 image format or geometry");
        Image.Plane[] planes=image.getPlanes();
        if(planes.length!=3) throw new IOException("P010 Y/Cb/Cr plane views required");
        P010Frame.Plane[] views=new P010Frame.Plane[3];
        for(int i=0;i<3;i++) views[i]=new P010Frame.Plane(planes[i].getBuffer(),planes[i].getRowStride(),planes[i].getPixelStride());
        frame.copyTo(views[0],views[1],views[2]);
    }
    private static void append(ByteArrayOutputStream out,ByteBuffer bytes) throws IOException {
        if((long)out.size()+bytes.remaining()>64*1024*1024) throw new IOException("encoded output exceeds 64 MiB");
        byte[] chunk=new byte[Math.min(bytes.remaining(),65536)];
        while(bytes.hasRemaining()) { int n=Math.min(bytes.remaining(),chunk.length); bytes.get(chunk,0,n); out.write(chunk,0,n); }
    }
    private static void requireInteger(MediaFormat f,String key,int value) throws IOException {
        if(!f.containsKey(key) || f.getInteger(key)!=value) throw new IOException("codec did not preserve "+key);
    }
    private static void requireApi() { if(Build.VERSION.SDK_INT<33) throw new UnsupportedOperationException("P010 encoder requires API 33+"); }
}
