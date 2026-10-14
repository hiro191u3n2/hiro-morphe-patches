package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;

/**
 * Conservative facial detail protection measured from the exact saved Bitmap.
 *
 * Android's bundled detector supplies eyes/midpoint/pose, NOT a semantic face
 * parsing model. The protected eye, mouth and hairline margins are deliberately
 * broad, and only the interiors of cheek/forehead regions can receive the skin
 * prior. This class does not claim a hair mask or precise lip segmentation.
 * No preview detections, timestamp guesses, camera state, or renderer instances
 * are used. A failed/unsupported/ambiguous detection contributes no skin prior.
 */
public final class FaceRegions1934 {
    private FaceRegions1934() { }
    private static final int MAX_EDGE = 640;
    private static final int MAX_FACES = 8;
    private static final long RESERVE = 32L * 1024 * 1024;

    public static final class Mask implements QualityPixels1932.RegionMask {
        public final int width, height;
        public final boolean reliable;
        private final int rw, rh;
        private final byte[] skin, detail;
        // Immutable affine mapping: current-image pixel center -> raster center.
        private final double a, b, c, d, e, f;
        private Mask(int width, int height, int rw, int rh, byte[] skin, byte[] detail,
                double a, double b, double c, double d, double e, double f,
                boolean reliable) {
            this.width=width; this.height=height; this.rw=rw; this.rh=rh;
            this.skin=skin; this.detail=detail; this.a=a; this.b=b; this.c=c;
            this.d=d; this.e=e; this.f=f; this.reliable=reliable;
        }
        public int skinQ8(int x, int y) { return sample(skin,x,y); }
        public int detailQ8(int x, int y) { return sample(detail,x,y); }
        private int sample(byte[] values,int x,int y) {
            if (!reliable || x<0 || y<0 || x>=width || y>=height) return 0;
            double px=a*x+b*y+c, py=d*x+e*y+f;
            if (px<-.5 || py<-.5 || px>rw-.5 || py>rh-.5) return 0;
            px=Math.max(0,Math.min(rw-1,px)); py=Math.max(0,Math.min(rh-1,py));
            int x0=(int)px,y0=(int)py,x1=Math.min(rw-1,x0+1),y1=Math.min(rh-1,y0+1);
            double fx=px-x0,fy=py-y0;
            double top=(values[y0*rw+x0]&255)*(1-fx)+(values[y0*rw+x1]&255)*fx;
            double bottom=(values[y1*rw+x0]&255)*(1-fx)+(values[y1*rw+x1]&255)*fx;
            return (int)Math.round((top*(1-fy)+bottom*fy)*256.0/255.0);
        }
        /** Exact inverse of the pipeline's rotate + centered cover-crop resize. */
        public Mask resample(int rotation,int outWidth,int outHeight) {
            int turn=((rotation%360)+360)%360;
            if (outWidth<=0 || outHeight<=0 || (turn%90)!=0)
                return empty(Math.max(0,outWidth),Math.max(0,outHeight));
            if (!reliable) return empty(outWidth,outHeight);
            double rotW=(turn==90||turn==270)?height:width;
            double rotH=(turn==90||turn==270)?width:height;
            double scale=Math.max(outWidth/rotW,outHeight/rotH);
            double offsetX=(rotW-outWidth/scale)*.5+.5/scale-.5;
            double offsetY=(rotH-outHeight/scale)*.5+.5/scale-.5;
            double s=1/scale,aa,bb,cc,dd,ee,ff;
            if(turn==90){aa=0;bb=s;cc=offsetY;dd=-s;ee=0;ff=height-1-offsetX;}
            else if(turn==180){aa=-s;bb=0;cc=width-1-offsetX;dd=0;ee=-s;ff=height-1-offsetY;}
            else if(turn==270){aa=0;bb=-s;cc=width-1-offsetY;dd=s;ee=0;ff=offsetX;}
            else {aa=s;bb=0;cc=offsetX;dd=0;ee=s;ff=offsetY;}
            return new Mask(outWidth,outHeight,rw,rh,skin,detail,
                a*aa+b*dd,a*bb+b*ee,a*cc+b*ff+c,
                d*aa+e*dd,d*bb+e*ee,d*cc+e*ff+f,true);
        }
    }

    public static Mask empty(int width,int height) {
        return new Mask(width,height,0,0,null,null,1,0,0,0,1,0,false);
    }

    /** Called once per exact source bitmap at a final-save entry. */
    public static Mask forBitmap(Bitmap input,int uprightRotation) {
        if(input==null || input.isRecycled()) return empty(0,0);
        final int width=input.getWidth(),height=input.getHeight();
        if(width<32 || height<32) return empty(width,height);
        int turn=((uprightRotation%360)+360)%360;
        if(turn%90!=0) return empty(width,height);
        int uprightW=turn==90||turn==270?height:width;
        int uprightH=turn==90||turn==270?width:height;
        double scale=Math.min(1.0,(double)MAX_EDGE/Math.max(uprightW,uprightH));
        // FaceDetector explicitly requires an even width and RGB_565 input.
        int pw=Math.max(2,((int)Math.round(uprightW*scale))&~1);
        int ph=Math.max(2,(int)Math.round(uprightH*scale));
        Runtime rt=Runtime.getRuntime();
        long free=rt.maxMemory()-(rt.totalMemory()-rt.freeMemory());
        if(free-RESERVE<(long)pw*ph*14) return empty(width,height);
        Bitmap probe=null;
        try {
            probe=Bitmap.createBitmap(pw,ph,Bitmap.Config.RGB_565);
            Matrix matrix=new Matrix();
            float sx=(float)pw/uprightW,sy=(float)ph/uprightH;
            float[] values;
            if(turn==90) values=new float[]{0,-sx,height*sx,sy,0,0,0,0,1};
            else if(turn==180) values=new float[]{-sx,0,width*sx,0,-sy,height*sy,0,0,1};
            else if(turn==270) values=new float[]{0,sx,0,-sy,0,width*sy,0,0,1};
            else values=new float[]{sx,0,0,0,sy,0,0,0,1};
            matrix.setValues(values);
            new Canvas(probe).drawBitmap(input,matrix,new Paint(Paint.FILTER_BITMAP_FLAG));
            FaceDetector.Face[] found=new FaceDetector.Face[MAX_FACES];
            int count=new FaceDetector(pw,ph,MAX_FACES).findFaces(probe,found);
            if(count<=0 || count>MAX_FACES) return empty(width,height);
            FaceRegions1934Pixels.Anchor[] anchors=new FaceRegions1934Pixels.Anchor[count];
            for(int i=0;i<count;i++) {
                FaceDetector.Face face=found[i];
                if(face==null) continue;
                PointF center=new PointF(); face.getMidPoint(center);
                anchors[i]=new FaceRegions1934Pixels.Anchor(center.x,center.y,
                    face.eyesDistance(),face.pose(FaceDetector.Face.EULER_Z),
                    face.pose(FaceDetector.Face.EULER_Y),face.confidence());
            }
            int[] pixels=new int[pw*ph]; probe.getPixels(pixels,0,pw,0,0,pw,ph);
            FaceRegions1934Pixels.Raster raster=FaceRegions1934Pixels.build(pixels,pw,ph,anchors);
            if(!raster.reliable) return empty(width,height);
            return uprightRaster(width,height,pw,ph,raster.skin,raster.detail,turn);
        } catch(RuntimeException unavailable) { return empty(width,height); }
          catch(LinkageError unavailable) { return empty(width,height); }
          catch(OutOfMemoryError unavailable) { return empty(width,height); }
        finally { if(probe!=null && !probe.isRecycled()) probe.recycle(); }
    }

    /** Raster ownership is transferred; only package-private production/test call sites. */
    static Mask uprightRaster(int width,int height,int rw,int rh,byte[] skin,byte[] detail,int turn) {
        double sx=(double)rw/((turn==90||turn==270)?height:width);
        double sy=(double)rh/((turn==90||turn==270)?width:height);
        if(turn==90) return new Mask(width,height,rw,rh,skin,detail,0,-sx,(height-.5)*sx-.5,sy,0,.5*sy-.5,true);
        if(turn==180) return new Mask(width,height,rw,rh,skin,detail,-sx,0,(width-.5)*sx-.5,0,-sy,(height-.5)*sy-.5,true);
        if(turn==270) return new Mask(width,height,rw,rh,skin,detail,0,sx,.5*sx-.5,-sy,0,(width-.5)*sy-.5,true);
        return new Mask(width,height,rw,rh,skin,detail,sx,0,.5*sx-.5,0,sy,.5*sy-.5,true);
    }
}
