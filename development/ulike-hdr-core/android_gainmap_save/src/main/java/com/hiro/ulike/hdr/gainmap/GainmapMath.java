package com.hiro.ulike.hdr.gainmap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/** FP64, row-bounded, processed SDR/HDR gainmap math. RGB is linear BT.2020,
 * in units of 203-nit SDR white. It is never inferred from an 8-bit SDR image. */
public strictfp final class GainmapMath {
    private GainmapMath() {}
    public static final int MAX_PIXELS = 32_000_000;
    private static final double LOG2 = StrictMath.log(2.0);
    public static final double OFFSET = 1.0 / 64.0;
    public static final class Frame {
        public final int width, height;
        public final String captureId, geometryId, processingId;
        public Frame(int width, int height, String captureId, String geometryId, String processingId) {
            if (width < 1 || height < 1 || width > 32768 || height > 32768 ||
                    (long) width * height > MAX_PIXELS) fail("invalid or over-budget frame");
            this.width=width; this.height=height;
            this.captureId=id(captureId); this.geometryId=id(geometryId); this.processingId=id(processingId);
        }
        private static String id(String value) {
            if(value==null || value.trim().isEmpty() || value.length()>256) fail("explicit bounded provenance IDs required");
            return value;
        }
        @Override public boolean equals(Object object) {
            if(!(object instanceof Frame)) return false;
            Frame f=(Frame)object;
            return width==f.width && height==f.height && captureId.equals(f.captureId)
                    && geometryId.equals(f.geometryId) && processingId.equals(f.processingId);
        }
        @Override public int hashCode() { return Objects.hash(width,height,captureId,geometryId,processingId); }
    }
    /** Source must fill exactly width*3 samples and remain immutable throughout the operation.
     * Only this row is allocated by math. The source owns any backing storage. */
    public interface Rows { void read(int y, double[] rgb); }
    public interface CodeRows { void read(int y, int[] rgb); }
    public interface CodeSink { void accept(int y, int[] rgb); }
    public static final class Image {
        public final Frame frame;
        private final Rows rows;
        public Image(Frame frame, Rows rows) { this.frame=Objects.requireNonNull(frame); this.rows=Objects.requireNonNull(rows); }
        public void read(int y, double[] into) {
            if(y<0 || y>=frame.height || into.length!=frame.width*3) fail("row shape mismatch");
            // Poison the reusable row so incomplete callbacks cannot silently retain old samples.
            Arrays.fill(into,Double.NaN); rows.read(y,into);
            for(double v:into) if(!finite(v) || v<0) fail("linear RGB must be finite and nonnegative");
        }
    }
    public static final class Codes {
        public final Frame frame;
        private final CodeRows rows;
        public Codes(Frame frame, CodeRows rows) { this.frame=Objects.requireNonNull(frame); this.rows=Objects.requireNonNull(rows); }
        public void read(int y, int[] into) {
            if(y<0 || y>=frame.height || into.length!=frame.width*3) fail("code row shape mismatch");
            Arrays.fill(into,-1); rows.read(y,into);
            for(int v:into) if(v<0 || v>1023) fail("codes must be actual 10-bit RGB");
        }
    }
    public static final class Metadata {
        public final Frame frame;
        public final double headroom;
        private final double[] low,high;
        private final byte[] pairDigest;
        private Metadata(Frame frame,double headroom,double[] low,double[] high,byte[] digest) {
            this.frame=frame; this.headroom=headroom; this.low=low.clone(); this.high=high.clone();
            pairDigest=digest.clone();
        }
        public double low(int channel) { return low[channel]; }
        public double high(int channel) { return high[channel]; }
        public double alternateHeadroomLog2() { return log2(headroom); }
        public byte[] pairDigest() { return pairDigest.clone(); }
    }
    static void fail(String message) { throw new IllegalArgumentException(message); }
    static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }
    static double log2(double v) { return StrictMath.log(v)/LOG2; }
    static void headroom(double value) {
        if(!finite(value) || value<=1 || value>10000.0/203.0) fail("headroom must be explicit and within PQ range");
    }
    private static void pair(Image sdr,Image hdr,double headroom) {
        Objects.requireNonNull(sdr); Objects.requireNonNull(hdr); headroom(headroom);
        if(!sdr.frame.equals(hdr.frame)) fail("capture, geometry or processing revision mismatch");
    }
    private static double gain(double sdr,double hdr,double headroom) {
        if(sdr>1 || hdr>headroom) fail("processed samples exceed declared range; no clipping");
        return log2(hdr+OFFSET)-log2(sdr+OFFSET);
    }
    static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch(NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    static void update(MessageDigest digest,double[] values) {
        for(double value:values) {
            long bits=Double.doubleToRawLongBits(value);
            for(int i=7;i>=0;i--) digest.update((byte)(bits>>>(i*8)));
        }
    }
    public static Metadata analyze(Image decodedBase,Image processedHdr,double headroom) {
        pair(decodedBase,processedHdr,headroom);
        double[] lo={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] hi={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        double[] base=new double[decodedBase.frame.width*3],hdr=new double[base.length];
        MessageDigest digest=digest();
        for(int y=0;y<decodedBase.frame.height;y++) {
            cancellation();
            decodedBase.read(y,base); processedHdr.read(y,hdr); update(digest,base); update(digest,hdr);
            for(int x=0;x<base.length;x++) {
                double v=gain(base[x],hdr[x],headroom); int c=x%3;
                lo[c]=Math.min(lo[c],v); hi[c]=Math.max(hi[c],v);
            }
        }
        return new Metadata(decodedBase.frame,headroom,lo,hi,digest.digest());
    }
    /** Emits codes, then checks that both analysis inputs were unchanged. A caller must discard
     * output if this throws (the save pipeline stages bytes and never publishes partial output). */
    public static void encode10(Image decodedBase,Image processedHdr,Metadata metadata,CodeSink sink) {
        pair(decodedBase,processedHdr,metadata.headroom); Objects.requireNonNull(sink);
        if(!decodedBase.frame.equals(metadata.frame)) fail("analysis belongs to another pair");
        double[] base=new double[metadata.frame.width*3],hdr=new double[base.length]; int[] codes=new int[base.length];
        MessageDigest digest=digest();
        for(int y=0;y<metadata.frame.height;y++) {
            cancellation();
            decodedBase.read(y,base); processedHdr.read(y,hdr); update(digest,base); update(digest,hdr);
            for(int x=0;x<base.length;x++) {
                int c=x%3; double v=gain(base[x],hdr[x],metadata.headroom),span=metadata.high[c]-metadata.low[c];
                if(v<metadata.low[c] || v>metadata.high[c]) fail("pixels changed after gainmap analysis");
                codes[x]=quantize(span==0?0:(v-metadata.low[c])/span);
            }
            sink.accept(y,codes);
        }
        if(!MessageDigest.isEqual(digest.digest(),metadata.pairDigest)) fail("processed pair changed after analysis");
    }
    /** Decode the actual final base codes to the same linear domain as processed HDR. */
    public static Image linearBase(Codes decoded) {
        return new Image(decoded.frame,(y,row)-> {
            int[] codes=new int[row.length]; decoded.read(y,codes);
            for(int i=0;i<row.length;i++) row[i]=sdrDecode(codes[i]/1023.0);
        });
    }
    public static Codes baseCodes(Image sdr) {
        return new Codes(sdr.frame,(y,row)-> {
            double[] linear=new double[row.length]; sdr.read(y,linear);
            for(int i=0;i<row.length;i++) {
                if(linear[i]>1) fail("SDR exceeds white; no silent clipping");
                row[i]=quantize(sdrEncode(linear[i]));
            }
        });
    }
    public static double sdrEncode(double v) {
        if(!finite(v) || v<0 || v>1) fail("SDR linear sample out of range");
        return v<0.018?4.5*v:1.099*StrictMath.pow(v,0.45)-0.099;
    }
    public static double sdrDecode(double v) {
        if(!finite(v) || v<0 || v>1) fail("SDR code out of range");
        return v<0.081?v/4.5:StrictMath.pow((v+0.099)/1.099,1.0/0.45);
    }
    public static int quantize(double v) {
        if(!finite(v) || v<0 || v>1) fail("map sample outside [0,1]");
        return (int)StrictMath.floor(v*1023.0+0.5);
    }
    public static double reconstruct(double base,int mapCode,Metadata metadata,int channel) {
        if(!finite(base) || base<0 || base>1 || mapCode<0 || mapCode>1023 || channel<0 || channel>2)
            fail("invalid reconstruction sample");
        double logGain=metadata.low[channel]+(metadata.high[channel]-metadata.low[channel])*(mapCode/1023.0);
        return (base+OFFSET)*StrictMath.pow(2.0,logGain)-OFFSET;
    }
    private static void cancellation() {
        if(Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("gainmap processing interrupted");
    }
}
