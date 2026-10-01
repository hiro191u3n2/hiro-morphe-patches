package com.hiro.ulike.hdr.gainmapcodec;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;

/** Owned full-range BT.2020 non-constant-luminance 10-bit 4:2:0 codes.
 * A numerical gainmap is linear numerical data, never HLG. No RGB conversion occurs here. */
public final class ColorP010 {
    public enum Role {
        SDR_BASE(1,3), NUMERICAL_GAINMAP(8,1);
        public final int isoTransfer, androidTransfer;
        Role(int isoTransfer,int androidTransfer) { this.isoTransfer=isoTransfer; this.androidTransfer=androidTransfer; }
    }
    public static final class Identity {
        public final int width,height;
        public final String captureId,geometryId,processingId;
        public Identity(int width,int height,String captureId,String geometryId,String processingId) {
            if(width<2 || height<2 || width>32768 || height>32768 || (width&1)!=0 || (height&1)!=0
                    || (long)width*height>32_000_000) throw new IllegalArgumentException("even dimensions up to 32MP required");
            this.width=width;this.height=height;this.captureId=id(captureId);this.geometryId=id(geometryId);this.processingId=id(processingId);
        }
        private static String id(String value) {
            if(value==null || value.trim().isEmpty() || value.length()>256) throw new IllegalArgumentException("bounded provenance required");
            return value;
        }
        @Override public boolean equals(Object obj) {
            if(!(obj instanceof Identity)) return false; Identity b=(Identity)obj;
            return width==b.width && height==b.height && captureId.equals(b.captureId) && geometryId.equals(b.geometryId) && processingId.equals(b.processingId);
        }
        @Override public int hashCode() { return Objects.hash(width,height,captureId,geometryId,processingId); }
    }
    public final Identity identity;
    public final Role role;
    private final short[][] codes;
    public ColorP010(Identity identity,Role role,ShortBuffer y,ShortBuffer cb,ShortBuffer cr) {
        this.identity=Objects.requireNonNull(identity); this.role=Objects.requireNonNull(role);
        int pixels=identity.width*identity.height;
        codes=new short[][]{copy(y,pixels),copy(cb,pixels/4),copy(cr,pixels/4)};
    }
    private ColorP010(Identity identity,Role role,short[][] owned) { this.identity=identity;this.role=role;codes=owned; }
    private static short[] copy(ShortBuffer input,int length) {
        if(input==null || input.remaining()!=length) throw new IllegalArgumentException("plane sample count");
        short[] result=new short[length];input.duplicate().get(result);
        for(short s:result) if(s<0 || s>1023) throw new IllegalArgumentException("10-bit code required");
        return result;
    }
    public ShortBuffer samples(int component) {
        if(component<0 || component>2) throw new IllegalArgumentException("component");
        return ShortBuffer.wrap(codes[component]).asReadOnlyBuffer();
    }
    /** Starting buffer position is the top-left uncropped plane origin, not the crop origin. */
    public static final class Plane {
        final ByteBuffer data;
        final int rowStride,pixelStride;
        public Plane(ByteBuffer data,int rowStride,int pixelStride) {
            this.data=Objects.requireNonNull(data).duplicate(); this.rowStride=rowStride;this.pixelStride=pixelStride;
        }
        void validate(int width,int height,int x,int y,int step,boolean writable) {
            long rowEnd=((long)x+width-1)*pixelStride+2;
            long end=(long)data.position()+((long)y+height-1)*rowStride+rowEnd;
            if(x<0 || y<0 || width<1 || height<1 || pixelStride!=step || rowStride<=0 || (rowStride&1)!=0
                    || ((long)x+width)*pixelStride>rowStride || rowEnd>rowStride || end>data.limit() || (writable && data.isReadOnly()))
                throw new IllegalArgumentException("invalid P010 plane layout");
        }
        int get(int x,int y) {
            int offset=data.position()+y*rowStride+x*pixelStride;
            int packed=(data.get(offset)&255)|((data.get(offset+1)&255)<<8);
            if((packed&63)!=0) throw new IllegalArgumentException("nonzero P010 unused bits");
            return packed>>>6;
        }
        void put(int x,int y,int code) {
            int offset=data.position()+y*rowStride+x*pixelStride, packed=code<<6;
            data.put(offset,(byte)packed);data.put(offset+1,(byte)(packed>>>8));
        }
    }
    private static void validate(Plane[] planes,int width,int height,int cropX,int cropY,boolean writable) {
        if(cropX<0 || cropY<0 || (cropX&1)!=0 || (cropY&1)!=0) throw new IllegalArgumentException("even crop origin required");
        if(planes[1].rowStride!=planes[2].rowStride) throw new IllegalArgumentException("chroma row strides mismatch");
        for(int n=0;n<3;n++) planes[n].validate(n==0?width:width/2,n==0?height:height/2,n==0?cropX:cropX/2,n==0?cropY:cropY/2,n==0?2:4,writable);
    }
    public void copyTo(Plane y,Plane cb,Plane cr) {
        Plane[] planes={Objects.requireNonNull(y),Objects.requireNonNull(cb),Objects.requireNonNull(cr)};
        validate(planes,identity.width,identity.height,0,0,true); // Validate all before the first write.
        for(int n=0;n<3;n++) {
            int w=n==0?identity.width:identity.width/2,h=n==0?identity.height:identity.height/2;
            for(int row=0;row<h;row++) { cancellation(); for(int col=0;col<w;col++) planes[n].put(col,row,codes[n][row*w+col]); }
        }
    }
    /** Copy actual decoder Image samples while their buffer is owned by the caller. */
    public static ColorP010 read(Identity identity,Role role,int cropX,int cropY,Plane y,Plane cb,Plane cr) {
        Objects.requireNonNull(identity);Objects.requireNonNull(role);
        Plane[] planes={Objects.requireNonNull(y),Objects.requireNonNull(cb),Objects.requireNonNull(cr)};
        validate(planes,identity.width,identity.height,cropX,cropY,false);
        int pixels=identity.width*identity.height;
        short[][] result={new short[pixels],new short[pixels/4],new short[pixels/4]};
        for(int n=0;n<3;n++) {
            int w=n==0?identity.width:identity.width/2,h=n==0?identity.height:identity.height/2;
            int sx=n==0?cropX:cropX/2,sy=n==0?cropY:cropY/2;
            for(int row=0;row<h;row++) { cancellation(); for(int col=0;col<w;col++) result[n][row*w+col]=(short)planes[n].get(col+sx,row+sy); }
        }
        return new ColorP010(identity,role,result);
    }
    private static void cancellation() { if(Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("P010 copy interrupted"); }
}
