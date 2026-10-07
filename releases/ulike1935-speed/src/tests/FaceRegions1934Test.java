package com.hiro.ulike;

import java.util.Arrays;

public final class FaceRegions1934Test {
    private static int assertions;
    private static void check(boolean condition,String message) { assertions++; if(!condition)throw new AssertionError(message); }
    private static void equal(int actual,int expected,String message){check(actual==expected,message+": "+actual+" != "+expected);}
    private static FaceRegions1934Pixels.Anchor face(float x,float y,float eyes,float roll,float yaw,float score) {
        return new FaceRegions1934Pixels.Anchor(x,y,eyes,roll,yaw,score);
    }
    private static FaceRegions1934Pixels.Raster build(int[]pixels,FaceRegions1934Pixels.Anchor...faces) {
        return FaceRegions1934Pixels.build(pixels,240,240,faces);
    }
    private static int[] skin() {int[]p=new int[240*240];Arrays.fill(p,0xffbc8b76);return p;}
    private static void masks() {
        int[] p=skin();
        FaceRegions1934Pixels.Anchor anchor=face(120,70,40,0,0,.93f);
        FaceRegions1934Pixels.Raster r=build(p,anchor);
        check(r.reliable,"valid detected eyes");
        check((r.skin[94*240+100]&255)>200,"left cheek interior retains skin eligibility");
        check((r.skin[94*240+140]&255)>200,"right cheek interior retains skin eligibility");
        equal(r.skin[20*240+20]&255,0,"skin-colored background cannot enter face mask");
        equal(r.skin[70*240+100]&255,0,"detected eye excludes smoothing");
        check((r.detail[70*240+100]&255)>240,"eye detail protected");
        equal(r.skin[110*240+120]&255,0,"broad mouth zone excludes smoothing");
        check((r.detail[110*240+120]&255)>240,"broad mouth detail protection");
        equal(r.skin[130*240+120]&255,0,"below face not labeled skin");
        for(int i=0;i<p.length;i++)check((r.skin[i]&255)+(r.detail[i]&255)<=256,"soft exclusions do not overlap at "+i);
        p[94*240+100]=0xff151515;
        r=build(p,anchor);equal(r.skin[94*240+100]&255,0,"dark fringe veto");
        p=skin();p[94*240+101]=0xff202020;
        r=build(p,anchor);equal(r.skin[94*240+100]&255,0,"high local detail veto");
        p=skin();p[94*240+100]=0xff5577aa;
        r=build(p,anchor);equal(r.skin[94*240+100]&255,0,"non-skin chroma veto");
        FaceRegions1934Pixels.Anchor[] bad={face(120,70,40,0,0,.59f),face(120,70,40,0,21,.99f),
            face(120,70,40,26,0,.99f),face(120,70,Float.NaN,0,0,.99f),face(Float.NaN,70,40,0,0,.99f),
            face(120,70,40,0,0,Float.NaN),face(8,8,40,0,0,.99f),face(120,70,8,0,0,.99f)};
        for(FaceRegions1934Pixels.Anchor b:bad){r=build(skin(),b);check(!r.reliable,"reject untrustworthy anchor");}
        r=build(skin(),anchor,face(122,71,40,0,0,.99f));check(!r.reliable,"overlapping faces not smoothed");
        r=build(skin(),face(60,70,24,0,0,.9f),face(180,70,24,0,0,.9f));check(r.reliable,"separate multiple faces accepted");
        check((r.skin[84*240+48]&255)>150 && (r.skin[84*240+168]&255)>150,"both faces independent");
        r=build(skin());check(!r.reliable,"no detected face no global skin prior");
        r=FaceRegions1934Pixels.build(new int[3],240,240,new FaceRegions1934Pixels.Anchor[]{anchor});check(!r.reliable,"invalid buffer safe");
    }
    private static void mapping() {
        int w=13,h=9;byte[] a=new byte[w*h],b=new byte[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){a[y*w+x]=(byte)(x*13+y*5);b[y*w+x]=(byte)(x*4+y*17);}
        FaceRegions1934.Mask source=FaceRegions1934.uprightRaster(w,h,w,h,a,b,0);
        for(int rotation:new int[]{0,90,180,270}){
            int ow=rotation%180==0?w:h,oh=rotation%180==0?h:w;
            FaceRegions1934.Mask out=source.resample(rotation,ow,oh);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int ox=rotation==90?h-1-y:rotation==180?w-1-x:rotation==270?y:x;
                int oy=rotation==90?x:rotation==180?h-1-y:rotation==270?w-1-x:y;
                equal(out.skinQ8(ox,oy),source.skinQ8(x,y),"rotation skin "+rotation);
                equal(out.detailQ8(ox,oy),source.detailQ8(x,y),"rotation detail "+rotation);
            }
            FaceRegions1934.Mask round=out.resample((360-rotation)%360,w,h);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++)equal(round.skinQ8(x,y),source.skinQ8(x,y),"round trip");
        }
        FaceRegions1934.Mask crop=source.resample(0,9,9);
        for(int y=0;y<9;y++)for(int x=0;x<9;x++)equal(crop.skinQ8(x,y),source.skinQ8(x+2,y),"center crop");
        FaceRegions1934.Mask up=source.resample(0,w*3,h*3);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)equal(up.skinQ8(x*3+1,y*3+1),source.skinQ8(x,y),"pixel-center scale");
        FaceRegions1934.Mask empty=FaceRegions1934.empty(100,50);
        check(!empty.reliable,"empty not reliable");equal(empty.skinQ8(20,20),0,"empty no prior");
        check(!source.resample(12,5,5).reliable,"invalid transform no prior");
        equal(source.skinQ8(-1,0),0,"out of bounds negative");equal(source.skinQ8(w,0),0,"out of bounds right");
        for(int turn:new int[]{0,90,180,270}) {
            int rw=turn%180==0?w:h,rh=turn%180==0?h:w;
            byte[] raster=new byte[rw*rh];
            for(int y=0;y<rh;y++)for(int x=0;x<rw;x++)raster[y*rw+x]=(byte)(x+15*y);
            FaceRegions1934.Mask base=FaceRegions1934.uprightRaster(w,h,rw,rh,raster,raster,turn);
            FaceRegions1934.Mask upright=base.resample(turn,rw,rh);
            for(int y=0;y<rh;y++)for(int x=0;x<rw;x++)equal(upright.skinQ8(x,y),(int)Math.round((raster[y*rw+x]&255)*256.0/255.0),"saved bitmap orientation "+turn);
        }
    }
    private static void adapter() {
        android.graphics.Bitmap input=android.graphics.Bitmap.createBitmap(240,240,android.graphics.Bitmap.Config.ARGB_8888);
        android.media.FaceDetector.scripted=new android.media.FaceDetector.Face[]{new android.media.FaceDetector.Face(120,70,40,0,0,.93f)};
        int allocations=android.graphics.Bitmap.created,freed=android.graphics.Bitmap.recycledCount;
        FaceRegions1934.Mask face=FaceRegions1934.forBitmap(input,0);
        check(face.reliable,"same bitmap detector fixture accepted");
        equal(android.graphics.Bitmap.created-allocations,1,"one bounded detector image");
        equal(android.graphics.Bitmap.recycledCount-freed,1,"detector image recycled");
        check(!input.isRecycled(),"source bitmap never recycled");
        for(int mode=1;mode<=3;mode++) {
            android.media.FaceDetector.failMode=mode;
            freed=android.graphics.Bitmap.recycledCount;
            check(!FaceRegions1934.forBitmap(input,0).reliable,"detector failure safely disables facial prior");
            equal(android.graphics.Bitmap.recycledCount-freed,1,"failure probe recycled");
            check(!input.isRecycled(),"failure source preserved");
        }
        android.media.FaceDetector.failMode=0;android.graphics.Canvas.failDraw=true;
        freed=android.graphics.Bitmap.recycledCount;check(!FaceRegions1934.forBitmap(input,0).reliable,"draw failure safe");
        equal(android.graphics.Bitmap.recycledCount-freed,1,"draw failure recycle");android.graphics.Canvas.failDraw=false;
        android.graphics.Bitmap.failAllocation=true;check(!FaceRegions1934.forBitmap(input,0).reliable,"allocation failure safe");android.graphics.Bitmap.failAllocation=false;
        check(!FaceRegions1934.forBitmap(null,0).reliable,"null safe");
        input.recycle();check(!FaceRegions1934.forBitmap(input,0).reliable,"recycled source safe");
        android.media.FaceDetector.scripted=new android.media.FaceDetector.Face[0];
        android.graphics.Bitmap big=android.graphics.Bitmap.createBitmap(1300,900,android.graphics.Bitmap.Config.ARGB_8888);
        FaceRegions1934.forBitmap(big,90);
        check(android.graphics.Canvas.lastTarget.getWidth()<=640&&android.graphics.Canvas.lastTarget.getHeight()<=640,"probe bounded");
        check(android.graphics.Canvas.lastTarget.getWidth()%2==0,"probe even width");
        float[] m=android.graphics.Canvas.lastMatrix;
        check(m[0]==0&&m[1]<0&&m[3]>0&&m[4]==0,"upright rotation sent to detector");
        check(android.graphics.Canvas.lastTarget.isRecycled(),"large probe freed");big.recycle();
    }
    public static void main(String[]args){masks();mapping();adapter();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":[\"face-only gating\",\"eye-mouth margins\",\"detail and color veto\",\"invalid-overlap-multiface\",\"rotation-crop-resize pixel centers\"],\"device_detector_accuracy_tested\":false}");}
}
