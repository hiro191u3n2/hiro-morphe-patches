package com.hiro.ulike.hdr.input;

/** Owned synthetic smooth P010 capture, used only to test the real-model photo path. */
public final class SmoothPhotoFixture {
    private SmoothPhotoFixture() {}
    public static HdrFrame create(int width,int height) {
        short[][] p={new short[width*height],new short[width*height/4],new short[width*height/4]};
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            double ramp=(x+.4*y)/(width-1+.4*(height-1));
            p[0][y*width+x]=(short)Math.round(300+610*ramp);
        }
        for(int y=0;y<height/2;y++)for(int x=0;x<width/2;x++) {
            int i=y*(width/2)+x;
            p[1][i]=(short)Math.round(506+10.0*x/(width/2-1));
            p[2][i]=(short)Math.round(508+8.0*y/(height/2-1));
        }
        Object source=new Object();
        CaptureMatch.Context context=new CaptureMatch.Context(source,source,source,source,source,22,"0",null);
        CaptureMatch.Result result=new CaptureMatch.Result(context,source,2234567,124,null,"5",10000000L,200);
        return new HdrFrame(width,height,HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED,context,result,p,(long)width*height*3);
    }
}
