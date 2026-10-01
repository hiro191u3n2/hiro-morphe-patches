package com.hiro.ulike.hdr.gainmapcodec;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Worker-thread, API 33+ MediaCodec encode/decode boundary for gainmap HEIF items.
 * Requires actual 10-bit P010 on BOTH sides. No Surface, RGB8, HDR-to-SDR or precision fallback.
 * Android implementation is SDK-compiled; device codec execution must be verified separately. */
public final class Main10Codec {
    private Main10Codec() {}
    private static final int MAX_BYTES=64*1024*1024;
    public static final class Options {
        public final CodecSelection.Policy policy;
        public final String exactEncoderName,exactDecoderName;
        public final int bitrate;
        public final long timeoutMs;
        public Options(CodecSelection.Policy policy,String exactEncoderName,String exactDecoderName,int bitrate,long timeoutMs) {
            this.policy=Objects.requireNonNull(policy);this.exactEncoderName=codecName(exactEncoderName);this.exactDecoderName=codecName(exactDecoderName);
            if(bitrate<100_000 || bitrate>400_000_000 || timeoutMs<100 || timeoutMs>120_000) throw new IllegalArgumentException("bitrate/timeout bound");
            this.bitrate=bitrate;this.timeoutMs=timeoutMs;
        }
        private static String codecName(String s) { if(s!=null && (s.trim().isEmpty() || s.length()>256)) throw new IllegalArgumentException("codec name");return s; }
    }
    /** Immutable encoded bytes tied to their exact content hash, role and capture/processing identity.
     * Identity is caller supplied for import; it does not prove that unknown bytes depict that capture. */
    public static final class Encoded {
        public final ColorP010.Identity identity;
        public final ColorP010.Role role;
        public final String sha256,encoderName;
        public final boolean hardwareEncoderReported;
        public final RoleHevcProof.Sps bitstream;
        private final byte[] data;
        private Encoded(byte[] bytes,ColorP010.Identity identity,ColorP010.Role role,String encoderName,boolean hardware) {
            this.identity=Objects.requireNonNull(identity);this.role=Objects.requireNonNull(role);
            if(bytes==null || bytes.length>MAX_BYTES) throw new IllegalArgumentException("HEVC size");
            data=bytes.clone();bitstream=RoleHevcProof.inspect(data,identity.width,identity.height,role);
            sha256=digest(data);this.encoderName=encoderName;hardwareEncoderReported=hardware;
        }
        public static Encoded verify(byte[] bytes,ColorP010.Identity identity,ColorP010.Role role) {
            return new Encoded(bytes,identity,role,"external-unattested",false);
        }
        public byte[] copyAnnexB() { return data.clone(); }
    }
    public static final class Decoded {
        public final ColorP010 frame;
        public final String encodedSha256,decoderName;
        public final boolean hardwareDecoderReported;
        private Decoded(ColorP010 frame,Encoded encoded,CodecSelection.Candidate chosen) {
            this.frame=frame;encodedSha256=encoded.sha256;decoderName=chosen.name;hardwareDecoderReported=chosen.hardware;
        }
    }
    static MediaFormat format(ColorP010.Identity id,ColorP010.Role role,Options options,boolean encode) {
        MediaFormat f=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC,id.width,id.height);
        f.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
        f.setInteger(MediaFormat.KEY_PROFILE,MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10);
        f.setInteger(MediaFormat.KEY_COLOR_STANDARD,MediaFormat.COLOR_STANDARD_BT2020);
        f.setInteger(MediaFormat.KEY_COLOR_TRANSFER,role.androidTransfer);
        f.setInteger(MediaFormat.KEY_COLOR_RANGE,MediaFormat.COLOR_RANGE_FULL);
        if(encode) {
            f.setInteger(MediaFormat.KEY_BITRATE_MODE,MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            f.setInteger(MediaFormat.KEY_BIT_RATE,options.bitrate);f.setInteger(MediaFormat.KEY_FRAME_RATE,1);
            f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,0);f.setInteger(MediaFormat.KEY_MAX_B_FRAMES,0);
        }
        return f;
    }
    public static List<CodecSelection.Candidate> advertised(ColorP010.Identity id,ColorP010.Role role,Options options,boolean encode) {
        requireApi();Objects.requireNonNull(id);Objects.requireNonNull(role);Objects.requireNonNull(options);
        MediaFormat requested=format(id,role,options,encode);List<CodecSelection.Candidate> result=new ArrayList<>();
        for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
            if(info.isEncoder()!=encode || info.isAlias()) continue;
            for(String type:info.getSupportedTypes()) if(type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                boolean main10=false,p010=false,formatOk=false;
                try {
                    MediaCodecInfo.CodecCapabilities cap=info.getCapabilitiesForType(type);
                    for(MediaCodecInfo.CodecProfileLevel p:cap.profileLevels) if(p.profile==MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10) main10=true;
                    for(int c:cap.colorFormats) if(c==MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010) p010=true;
                    boolean rate=!encode || cap.getEncoderCapabilities().isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
                    formatOk=rate && cap.getVideoCapabilities().isSizeSupported(id.width,id.height) && cap.isFormatSupported(requested);
                } catch(IllegalArgumentException ignored) { formatOk=false; }
                result.add(new CodecSelection.Candidate(info.getName(),info.isHardwareAccelerated(),main10,p010,formatOk));break;
            }
        }
        return result;
    }
    public static Encoded encode(ColorP010 frame,Options options) throws IOException {
        requireApi();Objects.requireNonNull(frame);Objects.requireNonNull(options);
        CodecSelection.Candidate selected=CodecSelection.select(advertised(frame.identity,frame.role,options,true),options.policy,options.exactEncoderName);
        MediaCodec codec=MediaCodec.createByCodecName(selected.name);boolean started=false;Throwable failure=null;
        try {
            codec.configure(format(frame.identity,frame.role,options,true),null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            require(codec.getInputFormat(),MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
            codec.start();started=true;
            long deadline=System.nanoTime()+options.timeoutMs*1_000_000L;
            boolean queued=false,eos=false,formatSeen=false;
            ByteArrayOutputStream config=new ByteArrayOutputStream(),samples=new ByteArrayOutputStream();
            MediaCodec.BufferInfo output=new MediaCodec.BufferInfo();
            while(!eos) {
                check(deadline);
                if(!queued) {
                    int index=codec.dequeueInputBuffer(10_000);
                    if(index>=0) {
                        // getInputImage invalidates ByteBuffer; retain only its capacity.
                        ByteBuffer raw=codec.getInputBuffer(index);
                        if(raw==null) throw new IOException("P010 input buffer unavailable");
                        int capacity=raw.capacity();raw=null;
                        Image image=codec.getInputImage(index);
                        if(image==null) throw new IOException("codec advertised P010 but has no writable input Image");
                        try(Image owned=image) { writeImage(frame,owned); }
                        check(deadline);codec.queueInputBuffer(index,0,capacity,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);queued=true;
                    }
                }
                int index=codec.dequeueOutputBuffer(output,10_000);
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if(formatSeen) throw new IOException("encoder changed output format repeatedly");
                    MediaFormat f=codec.getOutputFormat();requireColor(f,frame.role,false);
                    for(int i=0;i<3;i++) { ByteBuffer csd=f.getByteBuffer("csd-"+i);if(csd!=null) append(config,csd.duplicate()); }
                    formatSeen=true;
                } else if(index>=0) {
                    try(OwnedOutput ignored=new OwnedOutput(codec,index)) {
                        if(output.size>0) {
                            ByteBuffer bytes=outputBytes(codec,index,output);
                            if((output.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0) append(config,bytes);
                            else { if(output.presentationTimeUs!=0) throw new IOException("unexpected encoded frame timestamp");append(samples,bytes); }
                        }
                        eos=(output.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    }
                }
            }
            if(!queued || !formatSeen || samples.size()==0) throw new IOException("incomplete still encode");
            append(config,ByteBuffer.wrap(samples.toByteArray()));check(deadline);
            return new Encoded(config.toByteArray(),frame.identity,frame.role,selected.name,selected.hardware);
        } catch(IOException | RuntimeException | Error e) { failure=e;throw e; }
        finally { release(codec,started,failure); }
    }
    /** Decode the exact immutable Encoded object that will be placed in the HEIF item.
     * Parameter NALs are submitted as csd-0 and remaining NALs as one access unit, with
     * normalized Annex-B prefixes. The digest identifies the source of those coded NALs;
     * no original RGB substitute is returned. */
    public static Decoded decode(Encoded encoded,Options options) throws IOException {
        requireApi();Objects.requireNonNull(encoded);Objects.requireNonNull(options);
        CodecSelection.Candidate selected=CodecSelection.select(advertised(encoded.identity,encoded.role,options,false),options.policy,options.exactDecoderName);
        ByteArrayOutputStream config=new ByteArrayOutputStream(),sample=new ByteArrayOutputStream();
        for(byte[] nal:RoleHevcProof.annexB(encoded.data)) {
            int type=(nal[0]>>>1)&63;ByteArrayOutputStream target=type>=32 && type<=34?config:sample;
            append(target,ByteBuffer.wrap(new byte[]{0,0,0,1}));append(target,ByteBuffer.wrap(nal));
        }
        byte[] accessUnit=sample.toByteArray();
        MediaFormat requested=format(encoded.identity,encoded.role,options,false);
        requested.setByteBuffer("csd-0",ByteBuffer.wrap(config.toByteArray()));
        requested.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,accessUnit.length);
        MediaCodec codec=MediaCodec.createByCodecName(selected.name);boolean started=false;Throwable failure=null;
        try {
            codec.configure(requested,null,null,0);codec.start();started=true;
            long deadline=System.nanoTime()+options.timeoutMs*1_000_000L;
            boolean queued=false,eos=false,formatSeen=false;ColorP010 frame=null;
            MediaCodec.BufferInfo output=new MediaCodec.BufferInfo();
            while(!eos) {
                check(deadline);
                if(!queued) {
                    int index=codec.dequeueInputBuffer(10_000);
                    if(index>=0) {
                        ByteBuffer buffer=codec.getInputBuffer(index);
                        if(buffer==null || buffer.capacity()<accessUnit.length) throw new IOException("decoder input cannot fit single still access unit");
                        buffer.clear();buffer.put(accessUnit);
                        codec.queueInputBuffer(index,0,accessUnit.length,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);queued=true;
                    }
                }
                int index=codec.dequeueOutputBuffer(output,10_000);
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if(formatSeen) throw new IOException("decoder changed output format repeatedly");
                    requireColor(codec.getOutputFormat(),encoded.role,true);formatSeen=true;
                } else if(index>=0) {
                    try(OwnedOutput ignored=new OwnedOutput(codec,index)) {
                        if(output.size>0) {
                            if(frame!=null || !formatSeen || output.presentationTimeUs!=0 || (output.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0)
                                throw new IOException("unexpected or multiple decoded still frames");
                            requireColor(codec.getOutputFormat(index),encoded.role,true);
                            Image image=codec.getOutputImage(index);
                            if(image==null) throw new IOException("decoder lacks actual P010 output Image");
                            try(Image owned=image) { frame=readImage(encoded,owned); }
                        }
                        eos=(output.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    }
                }
            }
            check(deadline);
            if(!queued || !formatSeen || frame==null) throw new IOException("incomplete actual P010 decode");
            return new Decoded(frame,encoded,selected);
        } catch(IOException | RuntimeException | Error e) { failure=e;throw e; }
        finally { release(codec,started,failure); }
    }
    private static void writeImage(ColorP010 frame,Image image) throws IOException {
        Rect c=image.getCropRect();ColorP010.Identity id=frame.identity;
        if(image.getFormat()!=ImageFormat.YCBCR_P010 || image.getWidth()!=id.width || image.getHeight()!=id.height
                || c.left!=0 || c.top!=0 || c.right!=id.width || c.bottom!=id.height) throw new IOException("encoder changed input P010 format/geometry");
        ColorP010.Plane[] p=planes(image);frame.copyTo(p[0],p[1],p[2]);
    }
    private static ColorP010 readImage(Encoded encoded,Image image) throws IOException {
        Rect c=image.getCropRect();ColorP010.Identity id=encoded.identity;
        if(image.getFormat()!=ImageFormat.YCBCR_P010 || image.getTimestamp()!=0 || c.left<0 || c.top<0
                || c.right>image.getWidth() || c.bottom>image.getHeight() || c.width()!=id.width || c.height()!=id.height)
            throw new IOException("decoder changed P010 format, timestamp or visible geometry");
        ColorP010.Plane[] p=planes(image);return ColorP010.read(id,encoded.role,c.left,c.top,p[0],p[1],p[2]);
    }
    private static ColorP010.Plane[] planes(Image image) throws IOException {
        Image.Plane[] source=image.getPlanes();if(source.length!=3) throw new IOException("P010 Y/Cb/Cr plane views required");
        ColorP010.Plane[] out=new ColorP010.Plane[3];for(int i=0;i<3;i++) out[i]=new ColorP010.Plane(source[i].getBuffer(),source[i].getRowStride(),source[i].getPixelStride());
        return out;
    }
    private static ByteBuffer outputBytes(MediaCodec codec,int index,MediaCodec.BufferInfo info) throws IOException {
        ByteBuffer data=codec.getOutputBuffer(index);
        if(data==null || info.offset<0 || info.size<0 || (long)info.offset+info.size>data.capacity()) throw new IOException("invalid codec output bounds");
        ByteBuffer out=data.duplicate();out.position(info.offset);out.limit(info.offset+info.size);return out;
    }
    private static void requireColor(MediaFormat format,ColorP010.Role role,boolean p010) throws IOException {
        require(format,MediaFormat.KEY_COLOR_STANDARD,MediaFormat.COLOR_STANDARD_BT2020);
        require(format,MediaFormat.KEY_COLOR_TRANSFER,role.androidTransfer);require(format,MediaFormat.KEY_COLOR_RANGE,MediaFormat.COLOR_RANGE_FULL);
        if(p010) require(format,MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
    }
    private static void require(MediaFormat f,String key,int value) throws IOException {
        if(!f.containsKey(key) || f.getInteger(key)!=value) throw new IOException("codec did not preserve "+key);
    }
    private static void check(long deadline) throws IOException {
        if(Thread.currentThread().isInterrupted()) throw new IOException("codec operation interrupted");
        if(System.nanoTime()-deadline>=0) throw new IOException("codec dequeue/copy deadline exceeded");
    }
    private static void append(ByteArrayOutputStream out,ByteBuffer in) throws IOException {
        if((long)out.size()+in.remaining()>MAX_BYTES) throw new IOException("encoded still exceeds 64MiB");
        byte[] chunk=new byte[Math.min(in.remaining(),65536)];
        while(in.hasRemaining()) { int size=Math.min(in.remaining(),chunk.length);in.get(chunk,0,size);out.write(chunk,0,size); }
    }
    private static String digest(byte[] bytes) {
        try { byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder(64);for(byte b:hash)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString(); }
        catch(NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static final class OwnedOutput implements AutoCloseable {
        final MediaCodec codec;final int index;
        OwnedOutput(MediaCodec codec,int index) { this.codec=codec;this.index=index; }
        @Override public void close() { codec.releaseOutputBuffer(index,false); }
    }
    private static void release(MediaCodec codec,boolean started,Throwable prior) {
        Throwable cleanup=null;
        if(started) { try { codec.stop(); } catch(RuntimeException | Error e) { cleanup=e; } }
        try { codec.release(); } catch(RuntimeException | Error e) { if(cleanup==null)cleanup=e;else cleanup.addSuppressed(e); }
        if(cleanup!=null) {
            if(prior!=null)prior.addSuppressed(cleanup);
            else if(cleanup instanceof RuntimeException)throw (RuntimeException)cleanup;
            else throw (Error)cleanup;
        }
    }
    private static void requireApi() { if(Build.VERSION.SDK_INT<33) throw new UnsupportedOperationException("10-bit P010 requires API 33+"); }
}
