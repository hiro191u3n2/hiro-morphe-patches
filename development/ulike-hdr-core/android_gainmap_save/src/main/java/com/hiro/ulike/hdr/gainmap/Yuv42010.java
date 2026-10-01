package com.hiro.ulike.hdr.gainmap;

import java.nio.ShortBuffer;
import java.util.Objects;

/** Explicit BT.2020 non-constant-luminance full-range 10-bit RGB/YUV conversion.
 * Encoder chroma: box average over each 2x2 block. Decoder: nearest block.
 * This is lossy chroma sampling; final actual-decoded HDR quality is always measured. */
public strictfp final class Yuv42010 {
    private static final double KR=0.2627,KB=0.0593,KG=1-KR-KB;
    public final GainmapMath.Frame frame;
    private final short[][] planes;
    private Yuv42010(GainmapMath.Frame frame,short[][] owned) { this.frame=frame; this.planes=owned; }
    public Yuv42010(GainmapMath.Frame frame,ShortBuffer y,ShortBuffer cb,ShortBuffer cr) {
        this.frame=Objects.requireNonNull(frame);
        if(frame.width<2 || frame.height<2 || (frame.width&1)!=0 || (frame.height&1)!=0)
            GainmapMath.fail("even YUV420 geometry required");
        planes=new short[][]{copy(y,frame.width*frame.height),copy(cb,frame.width*frame.height/4),copy(cr,frame.width*frame.height/4)};
    }
    private static short[] copy(ShortBuffer source,int count) {
        if(source==null || source.remaining()!=count) GainmapMath.fail("YUV plane count mismatch");
        short[] result=new short[count]; source.duplicate().get(result);
        for(short v:result) if(v<0 || v>1023) GainmapMath.fail("YUV plane is not ten-bit");
        return result;
    }
    public ShortBuffer plane(int component) { return ShortBuffer.wrap(planes[component]).asReadOnlyBuffer(); }
    private static int code(double v) { return (int)StrictMath.floor(Math.max(0,Math.min(1023,v))+0.5); }
    public static Yuv42010 fromRgb(GainmapMath.Codes rgb) {
        GainmapMath.Frame f=rgb.frame; int w=f.width,h=f.height;
        if(w<2 || h<2 || (w&1)!=0 || (h&1)!=0) GainmapMath.fail("even RGB geometry required");
        short[] y=new short[w*h],cb=new short[w*h/4],cr=new short[w*h/4];
        int[][] rows={new int[w*3],new int[w*3]};
        for(int row=0;row<h;row+=2) {
            if(Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("RGB/P010 conversion interrupted");
            rgb.read(row,rows[0]); rgb.read(row+1,rows[1]);
            for(int col=0;col<w;col+=2) {
                double sumCb=0,sumCr=0;
                for(int dy=0;dy<2;dy++) for(int dx=0;dx<2;dx++) {
                    int x=(col+dx)*3; double r=rows[dy][x],g=rows[dy][x+1],b=rows[dy][x+2];
                    double l=KR*r+KG*g+KB*b; y[(row+dy)*w+col+dx]=(short)code(l);
                    sumCb+=(b-l)/(2*(1-KB)); sumCr+=(r-l)/(2*(1-KR));
                }
                int i=(row/2)*(w/2)+col/2;
                cb[i]=(short)code(512+sumCb/4); cr[i]=(short)code(512+sumCr/4);
            }
        }
        return new Yuv42010(f,new short[][]{y,cb,cr});
    }
    /** Numerical RGB is explicitly clipped to [0,1023] after inverse matrix, as required by
     * normalized map/base storage; no transfer function is applied to a gainmap. The save's
     * actual-decoded error gate includes this clipping and 4:2:0 sampling loss. */
    public GainmapMath.Codes rgb() {
        return new GainmapMath.Codes(frame,(row,into)-> {
            int w=frame.width;
            for(int x=0;x<w;x++) {
                int chroma=(row/2)*(w/2)+x/2; double l=planes[0][row*w+x],cb=planes[1][chroma]-512,cr=planes[2][chroma]-512;
                double r=l+2*(1-KR)*cr,b=l+2*(1-KB)*cb,g=(l-KR*r-KB*b)/KG;
                into[x*3]=code(r); into[x*3+1]=code(g); into[x*3+2]=code(b);
            }
        });
    }
}
