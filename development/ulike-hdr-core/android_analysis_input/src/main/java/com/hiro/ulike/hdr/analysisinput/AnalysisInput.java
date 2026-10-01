package com.hiro.ulike.hdr.analysisinput;

import com.hiro.ulike.hdr.color.SdrRendition;
import com.hiro.ulike.hdr.input.HdrFrame;
import com.hiro.ulike.hdr.faceprobe.AnalysisCapacity;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** Actual captured-rendition pixels for a single native analysis submission.
 * The analysis-only RGB8 raster has no resize, rotation, crop, JPEG, or gainmap.
 * The original owned P010 frame is retained unchanged. This is not calibration.
 */
public final class AnalysisInput {
    private AnalysisInput() {}
    /** Historical name; this is our legacy diagnostic policy, not a measured SDK limit. */
    public static final int SDK_MAX_PIXELS = 4194304;
    public static final class Budget {
        public final int maxPixels;
        public final long maxOwnedArrayBytes;
        public Budget(int maxPixels,long maxOwnedArrayBytes) {
            require(maxPixels>0 && maxPixels<=SDK_MAX_PIXELS && maxOwnedArrayBytes>0,"explicit bounded analysis policy required");
            this.maxPixels=maxPixels;this.maxOwnedArrayBytes=maxOwnedArrayBytes;
        }
        public static Budget currentSdk(){return new Budget(SDK_MAX_PIXELS,32L*1024*1024);}
    }
    /** Immutable provenance; a source digest does not prove native landmark coordinates. */
    public static final class Descriptor {
        public final HdrFrame frameIdentity;
        public final int width,height,nonce;
        public final long sensorTimestampNs;
        public final String sourceSha256,settingsSha256,proxySha256,geometryId,renderPolicy;
        public final boolean resized=false,nativeGeometryCalibrated=false;
        private Descriptor(HdrFrame frame,int nonce,String source,String settings,String proxy,String geometry,String policy) {
            frameIdentity=frame;width=frame.width;height=frame.height;this.nonce=nonce;sensorTimestampNs=frame.timestampNs;
            sourceSha256=source;settingsSha256=settings;proxySha256=proxy;geometryId=geometry;renderPolicy=policy;
        }
    }
    /** Rows are borrowed, opaque ARGB and valid only during the call. Never retain them. */
    public interface RowTarget {
        void begin(int width,int height) throws Exception;
        void row(int y,int[] opaqueArgb) throws Exception;
        void complete() throws Exception;
        void abort() throws Exception;
    }
    public static final class Submission {
        public final Descriptor descriptor;
        private Submission(Descriptor descriptor){this.descriptor=descriptor;}
    }
    /** One-shot buffer. A failed or successful transfer consumes and clears it. */
    public static final class Owned implements AutoCloseable {
        private final Descriptor descriptor;
        private int[] pixels;
        private boolean transferring;
        private Owned(Descriptor descriptor,int[] pixels){this.descriptor=descriptor;this.pixels=pixels;}
        public Descriptor descriptor(){return descriptor;}
        public synchronized boolean available(){return pixels!=null && !transferring;}
        public synchronized Submission transfer(RowTarget target) throws Exception {
            if(target==null)throw new NullPointerException("target");
            if(pixels==null || transferring)throw new IllegalStateException("Analysis raster already consumed");
            transferring=true;int[] row=null;boolean begun=false;
            try {
                row=new int[descriptor.width];interrupted();begun=true;target.begin(descriptor.width,descriptor.height);
                for(int y=0;y<descriptor.height;y++) {
                    interrupted();System.arraycopy(pixels,y*descriptor.width,row,0,descriptor.width);target.row(y,row);
                }
                interrupted();target.complete();return new Submission(descriptor);
            } catch(Exception|Error failure) {
                if(begun)try{target.abort();}catch(Exception|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}
                throw failure;
            } finally {
                if(row!=null)Arrays.fill(row,0);Arrays.fill(pixels,0);pixels=null;transferring=false;
            }
        }
        @Override public synchronized void close(){
            if(transferring)throw new IllegalStateException("Reentrant close during analysis transfer");
            if(pixels!=null){Arrays.fill(pixels,0);pixels=null;}
        }
    }
    public static Owned prepare(SdrRendition rendition,String geometryId,byte[] exactApplicationSettings,int nonce,Budget budget) throws IOException {
        if(rendition==null || budget==null)throw new NullPointerException("rendition/budget");
        require(nonce>0,"positive request nonce required");id(geometryId);
        require(exactApplicationSettings!=null && exactApplicationSettings.length>0 && exactApplicationSettings.length<=1048576,"exact bounded settings bytes required");
        HdrFrame frame=rendition.hdrSource().frameIdentity();long pixels=(long)frame.width*frame.height;
        require(pixels>0 && pixels<=budget.maxPixels,"Same-size analysis exceeds native backend bound; resizing is not performed");
        // Both our retained array and the borrowed transfer row are covered. Android/native copies are separate.
        require(pixels*4L+frame.width*4L<=budget.maxOwnedArrayBytes,"analysis array budget exceeded");
        require(rendition.frameIdentity()==frame && rendition.width()==frame.width && rendition.height()==frame.height,"exact same-size captured rendition required");
        String policy=rendition.policyName();require(policy!=null && policy.length()>0 && policy.length()<=512,"bounded render policy required");
        String settings=hex(digest().digest(exactApplicationSettings.clone()));
        interrupted();String source=sourceDigest(frame);int[] argb=new int[(int)pixels];boolean succeeded=false;
        try {
            MessageDigest proxy=digest();proxy.update(new byte[]{'U','L','S','P',1});putInt(proxy,frame.width);putInt(proxy,frame.height);
            for(int y=0,p=0;y<frame.height;y++) {
                interrupted();
                for(int x=0;x<frame.width;x++,p++) {
                    int rgb=rendition.rgb8(frame,x,y);require((rgb&0xff000000)==0,"RGB24 renderer required");
                    argb[p]=0xff000000|rgb;putInt(proxy,argb[p]);
                }
            }
            Descriptor descriptor=new Descriptor(frame,nonce,source,settings,hex(proxy.digest()),geometryId,policy);
            succeeded=true;return new Owned(descriptor,argb);
        } finally {if(!succeeded)Arrays.fill(argb,0);}
    }
    /** One-shot row renderer. Holds the immutable P010 source, never an int[width*height] raster.
     * Its proxy descriptor becomes available only after successful transfer of every row.
     */
    public static final class Streaming implements AutoCloseable {
        public final int width,height;
        public final AnalysisCapacity capacity;
        private SdrRendition rendition;
        private final HdrFrame frame;
        private final String geometry,settings,source,policy;
        private final int nonce;
        private Descriptor descriptor;
        private boolean transferring,consumed;
        private Streaming(SdrRendition rendition,String geometry,String settings,String source,String policy,int nonce,AnalysisCapacity capacity) {
            this.rendition=rendition;frame=rendition.hdrSource().frameIdentity();width=frame.width;height=frame.height;this.geometry=geometry;
            this.settings=settings;this.source=source;this.policy=policy;this.nonce=nonce;this.capacity=capacity;
        }
        public synchronized boolean available(){return !consumed && !transferring;}
        public synchronized Descriptor descriptor(){
            if(descriptor==null)throw new IllegalStateException("Proxy digest is established only after successful row transfer");return descriptor;
        }
        public synchronized Submission transfer(RowTarget target)throws Exception {
            if(target==null)throw new NullPointerException("target");
            if(consumed || transferring)throw new IllegalStateException("Streaming analysis already consumed");
            transferring=true;int[] row=null;boolean begun=false;
            try {
                interrupted();row=new int[width];
                MessageDigest proxy=digest();proxy.update(new byte[]{'U','L','S','P',1});putInt(proxy,width);putInt(proxy,height);
                begun=true;target.begin(width,height);
                for(int y=0;y<height;y++) {
                    interrupted();
                    for(int x=0;x<width;x++) {
                        int rgb=rendition.rgb8(frame,x,y);require((rgb&0xff000000)==0,"RGB24 renderer required");
                        row[x]=0xff000000|rgb;putInt(proxy,row[x]);
                    }
                    target.row(y,row);
                }
                interrupted();
                Descriptor complete=new Descriptor(frame,nonce,source,settings,hex(proxy.digest()),geometry,policy);
                target.complete();descriptor=complete;return new Submission(complete);
            } catch(Exception|Error failure) {
                if(begun)try{target.abort();}catch(Exception|Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}
                throw failure;
            } finally {
                if(row!=null)Arrays.fill(row,0);rendition=null;consumed=true;transferring=false;
            }
        }
        @Override public synchronized void close(){
            if(transferring)throw new IllegalStateException("Reentrant close during streaming transfer");
            rendition=null;consumed=true;
        }
    }
    /** Preflights the actual raster before SDK pause/start; no resize, Bitmap, or full RGB array here. */
    public static Streaming prepareStreaming(SdrRendition rendition,String geometryId,byte[] exactApplicationSettings,int nonce,AnalysisCapacity capacity)throws IOException {
        if(rendition==null || capacity==null)throw new NullPointerException("rendition/capacity");
        require(!capacity.comparePhotographicOrientation,"Streaming candidate requires separate explicit orientation calibration");
        require(nonce>0,"positive request nonce required");id(geometryId);
        require(exactApplicationSettings!=null && exactApplicationSettings.length>0 && exactApplicationSettings.length<=1048576,"exact bounded settings bytes required");
        HdrFrame frame=rendition.hdrSource().frameIdentity();capacity.requireTransferredPayload(frame.width,frame.height);
        require(rendition.frameIdentity()==frame && rendition.width()==frame.width && rendition.height()==frame.height,"exact same-size captured rendition required");
        String policy=rendition.policyName();require(policy!=null && policy.length()>0 && policy.length()<=512,"bounded render policy required");
        String settings=hex(digest().digest(exactApplicationSettings.clone()));
        interrupted();return new Streaming(rendition,geometryId,settings,sourceDigest(frame),policy,nonce,capacity);
    }
    /** Streaming canonical digest of metadata and every immutable owned 10-bit sample. */
    public static String sourceDigest(HdrFrame frame) throws IOException {
        if(frame==null)throw new NullPointerException("frame");MessageDigest hash=digest();
        OutputStream discard=new OutputStream(){@Override public void write(int b){} @Override public void write(byte[] b,int o,int n){}};
        DataOutputStream out=new DataOutputStream(new DigestOutputStream(discard,hash));
        out.write(new byte[]{'U','L','H','F',1});out.writeInt(frame.width);out.writeInt(frame.height);string(out,frame.encoding.name());
        out.writeLong(frame.timestampNs);out.writeLong(frame.frameNumber);out.writeLong(frame.shot);
        string(out,frame.cameraId);string(out,frame.physicalId);string(out,frame.activePhysicalId);
        out.writeBoolean(frame.exposureTimeNs!=null);if(frame.exposureTimeNs!=null)out.writeLong(frame.exposureTimeNs);
        out.writeBoolean(frame.sensitivityIso!=null);if(frame.sensitivityIso!=null)out.writeInt(frame.sensitivityIso);
        byte[] block=new byte[8192];
        for(HdrFrame.Component component:HdrFrame.Component.values()) {
            ShortBuffer values=frame.samples(component);out.writeInt(component.ordinal());out.writeInt(values.remaining());
            while(values.hasRemaining()) {
                interrupted();int n=Math.min(values.remaining(),block.length/2);
                for(int i=0;i<n;i++){int v=values.get();require(v>=0 && v<=1023,"owned P010 sample range");block[2*i]=(byte)(v>>>8);block[2*i+1]=(byte)v;}
                out.write(block,0,n*2);
            }
        }
        out.flush();return hex(hash.digest());
    }
    private static void string(DataOutputStream out,String text)throws IOException {
        if(text==null){out.writeInt(-1);return;}byte[] bytes=text.getBytes(StandardCharsets.UTF_8);require(bytes.length<=1024,"bounded source metadata");out.writeInt(bytes.length);out.write(bytes);
    }
    private static void putInt(MessageDigest d,int v){d.update((byte)(v>>>24));d.update((byte)(v>>>16));d.update((byte)(v>>>8));d.update((byte)v);}
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static String hex(byte[] bytes){StringBuilder out=new StringBuilder(64);for(byte b:bytes)out.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));return out.toString();}
    private static void id(String v){require(v!=null && !v.trim().isEmpty() && v.length()<=256 && v.indexOf('\n')<0 && v.indexOf('\r')<0,"explicit bounded geometry identity");}
    private static void interrupted()throws IOException{if(Thread.currentThread().isInterrupted())throw new IOException("Analysis input interrupted");}
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalArgumentException(reason);}
}
