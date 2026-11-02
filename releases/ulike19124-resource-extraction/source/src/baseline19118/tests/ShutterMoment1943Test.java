package com.hiro.ulike;

import java.util.Arrays;
import java.util.Random;

/** Image-domain regressions for the first real still owning the shutter moment. */
public final class ShutterMoment1943Test {
    private static final int W=320,H=256;
    private static int assertions;
    private static void require(boolean okay,String message){assertions++;if(!okay)throw new AssertionError(message);}
    private static byte[] scene(){
        byte[] data=new byte[W*H*3/2];
        for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
            double texture=12*Math.sin(x*.051)+10*Math.cos(y*.067);
            int pattern=((x/32+y/32)&1)==0?100:148;
            data[y*W+x]=(byte)Math.round(pattern+texture);
        }
        Arrays.fill(data,W*H,data.length,(byte)128);return data;
    }
    private static byte[] noisy(byte[] clean,int seed){
        byte[] data=clean.clone();Random random=new Random(seed);
        for(int i=0;i<W*H;i++)data[i]=(byte)Math.max(0,Math.min(255,(clean[i]&255)+Math.round(random.nextGaussian()*5)));
        for(int i=W*H;i<data.length;i++)data[i]=(byte)(128+Math.round(random.nextGaussian()*4));
        return data;
    }
    private static byte[] blur(byte[] clean){
        byte[] data=clean.clone();
        for(int y=2;y<H-2;y++)for(int x=2;x<W-2;x++) {
            int sum=0;
            for(int oy=-2;oy<=2;oy++)for(int ox=-2;ox<=2;ox++)sum+=clean[(y+oy)*W+x+ox]&255;
            data[y*W+x]=(byte)Math.round((clean[y*W+x]&255)*.35+sum/25.0*.65);
        }
        return data;
    }
    private static byte[] translated(byte[] clean,int dx,int dy){
        byte[] data=clean.clone();
        for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
            int xx=Math.max(0,Math.min(W-1,x-dx)),yy=Math.max(0,Math.min(H-1,y-dy));
            data[y*W+x]=clean[yy*W+xx];
        }
        return data;
    }
    private static FusionPixels1933.Result fuse(byte[][] frames,long[] times,int workers,FusionPixels1933.Analysis cache){
        long[] exposures=new long[frames.length];int[] isos=new int[frames.length];
        Arrays.fill(exposures,20000000L);Arrays.fill(isos,800);
        return FusionPixels1933.fuse(frames,W,H,times,exposures,isos,4,true,workers,cache);
    }
    private static double error(byte[] image,byte[] clean){
        double total=0;int count=0;
        for(int y=10;y<H-10;y++)for(int x=10;x<W-10;x++) {
            if(x%32<5||x%32>26||y%32<5||y%32>26)continue;
            int d=(image[y*W+x]&255)-(clean[y*W+x]&255);total+=d*d;count++;
        }
        return total/count;
    }
    private static FusionPixels1933.Result sharperLater(){
        byte[] clean=scene(),original=blur(clean);
        return fuse(new byte[][]{original,clean},new long[]{1000000000L,1200000000L},4,null);
    }
    private static void checkOwnership(){
        FusionPixels1933.Result result=sharperLater();
        require(result.referenceIndex==0,"a sharper later still replaced the shutter frame: "+result.referenceIndex);
        require(result.referenceTimestamp==1000000000L,"metadata moved to a later capture");
        require(result.nv21.length==W*H*3/2,"full-size NV21 changed");
    }
    private static void checkMovedScene(){
        byte[] original=scene(),moved=translated(original,14,11);
        FusionPixels1933.Result result=fuse(new byte[][]{original,moved},new long[]{1000000000L,1300000000L},4,null);
        require(result.referenceIndex==0&&result.referenceTimestamp==1000000000L,"movement replaced shutter ownership");
        require(!result.actualFusion&&Arrays.equals(result.nv21,original),"moved-to scene leaked into the shutter image");
    }
    private static void checkStationaryNoise(){
        byte[] clean=scene();byte[][] frames={noisy(clean,11),noisy(clean,22),noisy(clean,33),noisy(clean,44)};
        byte[][] copies=new byte[frames.length][];for(int i=0;i<frames.length;i++)copies[i]=frames[i].clone();
        long[] times={1000000000L,1150000000L,1350000000L,1600000000L};
        FusionPixels1933.Result result=fuse(frames,times,4,null);
        require(result.actualFusion&&result.acceptedCount==4,"stationary four-frame denoise was disabled");
        require(result.referenceIndex==0&&result.referenceTimestamp==times[0],"stationary noise changed the capture moment");
        require(error(result.nv21,clean)<error(frames[0],clean)*.75,"stationary denoise no longer reduces real pixel error");
        FusionPixels1933.Result serial=fuse(frames,times,1,null);
        require(Arrays.equals(serial.nv21,result.nv21),"worker count changes fusion pixels");
        for(int i=0;i<frames.length;i++)require(Arrays.equals(frames[i],copies[i]),"fusion modified a camera input");
        FusionPixels1933.Analysis cache=new FusionPixels1933.Analysis();
        FusionPixels1933.probeMotion(frames[0],frames[1],W,H,800,20000000L,cache,times[0],times[1],800,20000000L);
        require(cache.pending(),"actual capture probe cache missing");
        FusionPixels1933.Result cached=fuse(frames,times,4,cache);
        require(Arrays.equals(cached.nv21,result.nv21),"probe cache changes shutter-pinned fusion");
        require(!cache.pending(),"one-shot probe cache retained camera buffers");
    }
    private static void checkRejectedTimes(){
        byte[] original=scene(),other=noisy(original,77);
        for(long time:new long[]{500000000L,1000000000L,7000000001L}) {
            FusionPixels1933.Result result=fuse(new byte[][]{original,other},new long[]{1000000000L,time},4,null);
            require(!result.actualFusion&&result.referenceIndex==0&&Arrays.equals(original,result.nv21),"invalid/late capture affected shutter pixels");
        }
    }
    private static void checkFallbacks(){
        byte[] original=scene(),other=noisy(original,78);
        FusionPixels1933.Result off=FusionPixels1933.fuse(new byte[][]{original,other},W,H,new long[]{1000000000L,1200000000L},0,true,4);
        require(off.nv21==original&&off.referenceIndex==0,"OFF changed frame owner");
        Thread.currentThread().interrupt();
        FusionPixels1933.Result stopped=fuse(new byte[][]{original,other},new long[]{1000000000L,1200000000L},4,null);
        boolean interrupted=Thread.interrupted();
        require(interrupted&&stopped.cancelled&&stopped.nv21==original,"interruption failed to preserve the shutter frame");
    }
    public static void main(String[] args){
        if(args.length>0&&"baseline".equals(args[0])) {
            FusionPixels1933.Result result=sharperLater();
            System.out.println("{\"reference_index\":"+result.referenceIndex+",\"reference_timestamp\":"+result.referenceTimestamp+"}");return;
        }
        checkOwnership();checkMovedScene();checkStationaryNoise();checkRejectedTimes();checkFallbacks();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"stationary_four_frame_fusion\":true,\"shutter_frame_owns_metadata\":true}");
    }
}
