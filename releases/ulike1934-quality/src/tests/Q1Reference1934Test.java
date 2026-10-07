package com.hiro.ulike;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.lang.reflect.Method;

/** Q1: real-image quality and identity regressions for sharp-reference selection. */
public final class Q1Reference1934Test {
    private static int assertions;
    private static final int W=320,H=256;
    private static final long[] TIMES={4100000000L,4133000000L,4166000000L,4199000000L};
    private static final Map<String,Integer> CASES=new LinkedHashMap<String,Integer>();
    private static final Map<String,Long> METRICS=new LinkedHashMap<String,Long>();
    interface Case { void run() throws Exception; }
    private static void check(boolean b,String message){assertions++;if(!b)throw new AssertionError(message);}
    private static void test(String name,Case run)throws Exception{
        int before=assertions;run.run();CASES.put(name,assertions-before);
    }
    private static int clip(double value){return Math.max(0,Math.min(255,(int)Math.round(value)));}
    private static int u(byte value){return value&255;}
    private static double chart(int x,int y){
        return 110+13*Math.sin(x*.035)+12*Math.cos(y*.025)+
            ((Math.floorMod(x+16,80)<28&&Math.floorMod(y+16,80)<28)?34:0)+6*Math.sin((x+y)*.19);
    }
    /** Optical blur precedes sensor noise; blur must not get rewarded for smoothing the noise. */
    private static byte[] frame(double blur,double noise,int seed,int dx,int dy){
        double[] kernel=new double[11];double total=0;
        for(int k=-5;k<=5;k++){double v=blur==0?(k==0?1:0):Math.exp(-(k*k)/(2*blur*blur));kernel[k+5]=v;total+=v;}
        for(int k=0;k<kernel.length;k++)kernel[k]/=total;
        byte[] f=new byte[W*H*3/2];Random random=new Random(seed);
        for(int y=0;y<H;y++)for(int x=0;x<W;x++){
            double signal=0;for(int yy=-5;yy<=5;yy++)for(int xx=-5;xx<=5;xx++)
                signal+=chart(x+xx-dx,y+yy-dy)*kernel[xx+5]*kernel[yy+5];
            f[y*W+x]=(byte)clip(signal+random.nextGaussian()*noise);
        }
        for(int p=W*H;p<f.length;p+=2){f[p]=(byte)153;f[p+1]=(byte)104;}
        return f;
    }
    private static byte[] frame(double blur,double noise,int seed){return frame(blur,noise,seed,0,0);}
    private static FusionPixels1933.Result fuse(byte[][] frames,int workers){
        long[] exposures=new long[frames.length];int[] isos=new int[frames.length];
        Arrays.fill(exposures,20000000L);Arrays.fill(isos,800);
        return FusionPixels1933.fuse(frames,W,H,Arrays.copyOf(TIMES,frames.length),exposures,isos,4,false,workers);
    }
    private static double mse(byte[] actual,byte[] target){
        double sum=0;int n=0;for(int y=12;y<H-12;y++)for(int x=12;x<W-12;x++){
            int d=u(actual[y*W+x])-u(target[y*W+x]);sum+=d*d;n++;}return sum/n;
    }
    private static byte[] shiftBrightness(byte[] frame,int delta){
        byte[] copy=frame.clone();for(int i=0;i<W*H;i++)copy[i]=(byte)clip(u(copy[i])+delta);return copy;
    }
    public static void main(String[] args)throws Exception{
        test("real_sharp_frame_and_exact_metadata",()->{
            byte[][] frames={frame(1.3,4,1),frame(0,4,2),frame(.8,4,3)};
            byte[][] before={frames[0].clone(),frames[1].clone(),frames[2].clone()};
            FusionPixels1933.Result r=fuse(frames,4);
            check(r.referenceIndex==1,"sharper second real frame must own geometry");
            check(r.referenceTimestamp==TIMES[1],"selected exact sensor timestamp");
            check(r.actualFusion&&r.acceptedCount>=2,"static compatible captures still denoised");
            byte[] truth=frame(0,0,0);double original=mse(frames[0],truth),selected=mse(frames[1],truth),fused=mse(r.nv21,truth);
            METRICS.put("sharp_reference_mse_vs_original_ppm",Math.round(fused/original*1000000));
            METRICS.put("sharp_reference_mse_vs_single_ppm",Math.round(fused/selected*1000000));
            check(fused<original*.72,"focus detail and noise improved over blurry first "+fused+"/"+original);
            check(fused<selected*.96,"fusion still improves sharper single "+fused+"/"+selected);
            double brightness=0;for(int i=0;i<W*H;i++)brightness+=u(r.nv21[i])-u(frames[1][i]);
            check(Math.abs(brightness/(W*H))<.5,"no synthetic exposure gain");
            for(int i=0;i<frames.length;i++)check(Arrays.equals(before[i],frames[i]),"camera capture mutated "+i);
            boolean sameChroma=true;for(int i=W*H;i<r.nv21.length;i++)sameChroma&=r.nv21[i]==frames[1][i];
            check(sameChroma,"VU colour changed");
            FusionPixels1933.Result one=fuse(frames,1);
            check(one.referenceIndex==r.referenceIndex&&one.referenceTimestamp==r.referenceTimestamp,"parallel identity differs");
            check(Arrays.equals(one.nv21,r.nv21),"parallel pixels differ");
        });
        test("third_frame_selection_and_translation",()->{
            byte[][] frames={frame(2,4,11),frame(.8,4,12),frame(0,4,13,2,-2)};
            FusionPixels1933.Result r=fuse(frames,4);
            check(r.referenceIndex==2&&r.referenceTimestamp==TIMES[2],"best frame may be third and translated");
            byte[] truth=frame(0,0,0,2,-2);
            check(mse(r.nv21,truth)<mse(frames[0],truth)*.55,"output geometry follows actual selected capture");
        });
        test("noise_detail_and_photometric_false_winners",()->{
            byte[] first=frame(.9,4,21),same=frame(.9,4,22),noisy=frame(.9,17,23),sharp=frame(0,4,24);
            check(fuse(new byte[][]{first,same},4).referenceIndex==0,"equal optical focus keeps moment");
            check(fuse(new byte[][]{first,noisy},4).referenceIndex==0,"noise is not sharp detail");
            check(fuse(new byte[][]{first,shiftBrightness(sharp,12)},4).referenceIndex==0,"brighter capture cannot win on contrast");
            byte[] gain=first.clone();for(int i=0;i<W*H;i++)gain[i]=(byte)clip(115+(u(gain[i])-115)*1.12);
            check(fuse(new byte[][]{first,gain},4).referenceIndex==0,"global contrast alone is not focus");
            byte[] colour=sharp.clone();for(int i=W*H;i<colour.length;i++)colour[i]=(byte)clip(u(colour[i])+14);
            check(fuse(new byte[][]{first,colour},4).referenceIndex==0,"colour shift cannot change reference");
            byte[] flat0=new byte[W*H*3/2],flat1=new byte[flat0.length];Random random=new Random(15);
            for(int i=0;i<W*H;i++){flat0[i]=(byte)clip(100+random.nextGaussian()*5);flat1[i]=(byte)clip(100+random.nextGaussian()*13);}
            Arrays.fill(flat0,W*H,flat0.length,(byte)128);Arrays.fill(flat1,W*H,flat1.length,(byte)128);
            check(fuse(new byte[][]{flat0,flat1},4).referenceIndex==0,"flat/noisy image never supplies focus evidence");
        });
        test("moving_scene_and_local_false_detail",()->{
            byte[] first=frame(1.3,4,31),second=frame(0,4,32);
            // Large foreground movement: sharpened background is insufficient to
            // change the moment or put the foreground at the other location.
            for(int y=40;y<205;y++)for(int x=30;x<140;x++)first[y*W+x]=(byte)195;
            for(int y=40;y<205;y++)for(int x=165;x<275;x++)second[y*W+x]=(byte)195;
            FusionPixels1933.Result r=fuse(new byte[][]{first,second},4);
            check(r.referenceIndex==0,"large subject motion keeps first capture");
            double change=0;int n=0;for(int y=65;y<175;y++)for(int x=45;x<120;x++){change+=Math.abs(u(r.nv21[y*W+x])-195);n++;}
            check(change/n<1,"moving subject not ghosted into other position");
            byte[] local=frame(1.3,4,33),base=frame(1.3,4,34),focused=frame(0,4,35);
            for(int y=32;y<65;y++)for(int x=25;x<62;x++)local[y*W+x]=focused[y*W+x];
            check(fuse(new byte[][]{base,local},4).referenceIndex==0,"one local edge change is insufficient evidence");
        });
        test("capture_identity_exposure_and_duplicate_gates",()->{
            byte[][] frames={frame(1.3,4,41),frame(0,4,42)};
            long[] ts={TIMES[0],TIMES[1]};
            FusionPixels1933.Result exposure=FusionPixels1933.fuse(frames,W,H,ts,new long[]{100,200},new int[]{800,800},4,false,4);
            check(exposure.nv21==frames[0]&&exposure.referenceIndex==0,"different exposure cannot become owner");
            FusionPixels1933.Result iso=FusionPixels1933.fuse(frames,W,H,ts,new long[]{100,100},new int[]{800,1600},4,false,4);
            check(iso.nv21==frames[0]&&iso.referenceIndex==0,"different ISO cannot become owner");
            check(FusionPixels1933.fuse(frames,W,H,new long[]{1,1},4,false,4).referenceIndex==0,"duplicate time cannot be selected");
            check(FusionPixels1933.fuse(frames,W,H,new long[]{1,0},4,false,4).referenceIndex==0,"missing time cannot be selected");
            check(fuse(new byte[][]{frames[0],frames[0].clone()},4).acceptedCount==1,"byte duplicate is not independent capture");
            FusionPixels1933.Result off=FusionPixels1933.fuse(frames,W,H,ts,0,false,4);
            check(off.nv21==frames[0]&&off.referenceTimestamp==ts[0],"OFF retains original identity");
            FusionPixels1933.Result single=FusionPixels1933.fuse(new byte[][]{frames[1]},W,H,new long[]{ts[1]},4,false,4);
            check(single.nv21==frames[1]&&single.referenceTimestamp==ts[1]&&!single.actualFusion,"single retains supplied capture identity");
            check(FusionPixels1933.fuse(frames,W,H,null,4,false,4).referenceTimestamp==0,"missing metadata not fabricated");
        });
        test("cancelled_and_selected_single_fallback_identity",()->{
            byte[][] frames={frame(1.3,4,51),frame(0,4,52)};
            check(fuse(frames,4).referenceIndex==1,"cancellation fixture really selects nonzero reference normally");
            Thread.currentThread().interrupt();FusionPixels1933.Result interrupted=fuse(frames,4);
            check(interrupted.cancelled&&interrupted.referenceIndex==0&&interrupted.referenceTimestamp==TIMES[0]&&interrupted.nv21==frames[0],"cancel always restores original source and metadata");
            check(Thread.interrupted(),"interruption retained");
            // Exercise the common zero-contribution result builder with a chosen
            // index. The capture boundary must accept this exact real-frame fallback.
            Method method=FusionPixels1933.class.getDeclaredMethod("single",byte[].class,int.class,long.class,boolean.class);
            method.setAccessible(true);FusionPixels1933.Result single=(FusionPixels1933.Result)method.invoke(null,frames[1],1,TIMES[1],false);
            check(single.nv21==frames[1]&&single.referenceIndex==1&&single.referenceTimestamp==TIMES[1],"selected fallback keeps pixels/index/time together");
            check(!single.actualFusion&&single.acceptedCount==1&&single.fusedPixels==0&&!single.cancelled,"single fallback does not claim fusion");
        });
        StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"scenarios\":{");
        boolean comma=false;for(Map.Entry<String,Integer> e:CASES.entrySet()){if(comma)out.append(',');comma=true;out.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        out.append("},\"metrics\":{");comma=false;for(Map.Entry<String,Long> e:METRICS.entrySet()){if(comma)out.append(',');comma=true;out.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        System.out.println(out.append("}}"));
    }
}
