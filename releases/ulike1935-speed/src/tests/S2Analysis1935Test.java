package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Frozen 1.9.34 oracle; all NV21 bytes and all public result fields must agree. */
public final class S2Analysis1935Test {
    private static int checks;
    private static final int W=320,H=256;
    private static final long[] TIMES={4100000000L,4133000000L,4166000000L,4199000000L};
    private static final Map<String,Integer> CASES=new LinkedHashMap<String,Integer>();
    interface Case { void run() throws Exception; }
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void test(String name,Case run)throws Exception{int n=checks;run.run();CASES.put(name,checks-n);}
    private static Object member(Object object,String name)throws Exception{
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    private static int clip(double v){return Math.max(0,Math.min(255,(int)Math.round(v)));}
    private static double chart(int x,int y){return 110+13*Math.sin(x*.035)+12*Math.cos(y*.025)+
        ((Math.floorMod(x+16,80)<28&&Math.floorMod(y+16,80)<28)?34:0)+6*Math.sin((x+y)*.19);}
    private static byte[] frame(double blur,int seed){
        double[] kernel=new double[11];double total=0;
        for(int i=-5;i<=5;i++){double v=blur==0?(i==0?1:0):Math.exp(-(i*i)/(2*blur*blur));kernel[i+5]=v;total+=v;}
        for(int i=0;i<11;i++)kernel[i]/=total;
        byte[] b=new byte[W*H*3/2];Random rng=new Random(seed);
        for(int y=0;y<H;y++)for(int x=0;x<W;x++){
            double v=0;for(int yy=-5;yy<=5;yy++)for(int xx=-5;xx<=5;xx++)v+=chart(x+xx,y+yy)*kernel[xx+5]*kernel[yy+5];
            b[y*W+x]=(byte)clip(v+rng.nextGaussian()*4);
        }
        for(int p=W*H;p<b.length;p+=2){b[p]=(byte)153;b[p+1]=(byte)104;}
        return b;
    }
    private static long[] exposure(int n){long[] a=new long[n];Arrays.fill(a,20000000L);return a;}
    private static int[] iso(int n){int[] a=new int[n];Arrays.fill(a,800);return a;}
    private static void same(FusionPixels1934Reference.Result a,FusionPixels1933.Result b){
        check(Arrays.equals(a.nv21,b.nv21),"exact output bytes");
        check(a.referenceIndex==b.referenceIndex,"reference index");
        check(a.referenceTimestamp==b.referenceTimestamp,"reference timestamp");
        check(a.acceptedCount==b.acceptedCount,"accepted count");
        check(a.fusedPixels==b.fusedPixels,"fused pixel count");
        check(a.actualFusion==b.actualFusion,"actual fusion");check(a.cancelled==b.cancelled,"cancellation");
        check(Float.floatToIntBits(a.motionScore)==Float.floatToIntBits(b.motionScore),"motion bits");
        check(Float.floatToIntBits(a.movingFraction)==Float.floatToIntBits(b.movingFraction),"moving bits");
    }
    private static void same(FusionPixels1934Reference.Motion a,FusionPixels1933.Motion b){
        check(Float.floatToIntBits(a.score)==Float.floatToIntBits(b.score),"probe score");
        check(Float.floatToIntBits(a.globalShiftPixels)==Float.floatToIntBits(b.globalShiftPixels),"probe shift");
        check(Float.floatToIntBits(a.movingFraction)==Float.floatToIntBits(b.movingFraction),"probe moving");
        check(Float.floatToIntBits(a.confidence)==Float.floatToIntBits(b.confidence),"probe confidence");
        check(Float.floatToIntBits(a.darkness)==Float.floatToIntBits(b.darkness),"probe darkness");
        check(Float.floatToIntBits(a.noiseSigma)==Float.floatToIntBits(b.noiseSigma),"probe noise");
        check(a.reliable==b.reliable&&a.duplicate==b.duplicate,"probe flags");
    }
    private static FusionPixels1933.Analysis probe(byte[][] f,int w,int h,long[] ts,long[] es,int[] is){
        FusionPixels1933.Analysis a=new FusionPixels1933.Analysis();
        same(FusionPixels1934Reference.probeMotion(f[0],f[1],w,h,is[0],es[0]),
            FusionPixels1933.probeMotion(f[0],f[1],w,h,is[0],es[0],a,ts[0],ts[1],is[1],es[1]));
        return a;
    }
    private static FusionPixels1933.Result fuse(byte[][] f,int w,int h,long[] ts,long[] es,int[] is,int level,boolean night,int workers,FusionPixels1933.Analysis a){
        FusionPixels1934Reference.Result old=FusionPixels1934Reference.fuse(f,w,h,ts,es,is,level,night,workers);
        FusionPixels1933.Result r=FusionPixels1933.fuse(f,w,h,ts,es,is,level,night,workers,a);same(old,r);return r;
    }
    public static void main(String[] args)throws Exception{
        final byte[][] staticFrames={frame(.8,1),frame(.8,2),frame(.8,3),frame(.8,4)};
        test("matching_probe_consumed_once_and_mesh_reuses_registration",()->{
            byte[][] f=Arrays.copyOf(staticFrames,2);long[] es=exposure(2);int[] is=iso(2);
            FusionPixels1933.Analysis a=probe(f,W,H,TIMES,es,is);check(a.pending(),"cache populated");
            Object p=member(member(a,"probe"),"pair");check(member(p,"flowX")==null,"probe has no local mesh");
            FusionPixels1933.Result r=fuse(f,W,H,TIMES,es,is,4,false,4,a);
            check(r.referenceIndex==0&&r.actualFusion,"fixture selects original and fuses");
            check(member(p,"flowX")!=null,"same ordered pair registration reused by final mesh");
            check(!a.pending(),"cache consumed");fuse(f,W,H,TIMES,es,is,4,false,1,a);
        });
        test("all_strengths_night_and_frame_counts_exact",()->{
            for(int n=2;n<=4;n++)for(int level=0;level<=4;level++)for(boolean night:new boolean[]{false,true}){
                byte[][] f=Arrays.copyOf(staticFrames,n);long[] es=exposure(n);int[] is=iso(n);
                byte[][] unchanged=new byte[n][];for(int i=0;i<n;i++)unchanged[i]=f[i].clone();
                FusionPixels1933.Analysis a=probe(f,W,H,TIMES,es,is);fuse(f,W,H,TIMES,es,is,level,night,(n%4)+1,a);
                check(!a.pending(),"every exit consumes cache");for(int i=0;i<n;i++)check(Arrays.equals(f[i],unchanged[i]),"capture immutable");
            }
        });
        test("reference_switch_realigns_reverse_direction",()->{
            byte[][] f={frame(1.3,11),frame(0,12),frame(.8,13)};long[] es=exposure(3);int[] is=iso(3);
            FusionPixels1933.Analysis a=probe(f,W,H,TIMES,es,is);Object p=member(member(a,"probe"),"pair");
            FusionPixels1933.Result r=fuse(f,W,H,TIMES,es,is,4,false,4,a);
            check(r.referenceIndex==1,"fixture switches reference");
            check(member(p,"flowX")==null,"ordered forward alignment not misused as reverse mesh");
        });
        test("all_identity_geometry_and_metadata_key_misses",()->{
            for(int change=0;change<10;change++){
                byte[][] f=Arrays.copyOf(staticFrames,2);long[] ts=Arrays.copyOf(TIMES,2),es=exposure(2);int[] is=iso(2);int w=W,h=H;
                FusionPixels1933.Analysis a=probe(f,w,h,ts,es,is);Object p=member(member(a,"probe"),"pair");
                switch(change){
                    case 0:f[0]=f[0].clone();break;case 1:f[1]=f[1].clone();break;
                    case 2:byte[] b=f[0];f[0]=f[1];f[1]=b;break;
                    case 3:ts[0]++;break;case 4:ts[1]++;break;
                    case 5:es[0]++;break;case 6:es[1]++;break;
                    case 7:is[0]++;break;case 8:is[1]++;break;
                    case 9:w=H;h=W;break;
                }
                fuse(f,w,h,ts,es,is,4,false,4,a);
                check(member(p,"flowX")==null,"changed cache key must not supply pair "+change);
                check(!a.pending(),"miss drops old references");
            }
        });
        test("regroup_and_explicit_invalidation_before_mutation",()->{
            byte[][] f={staticFrames[0].clone(),staticFrames[1].clone()};long[] es=exposure(2);int[] is=iso(2);
            FusionPixels1933.Analysis a=probe(f,W,H,TIMES,es,is);a.clear();
            check(!a.pending(),"group discarded");
            // Capture ownership contract: clear before modifying/recycling a buffer.
            for(int i=0;i<W*H;i++)f[1][i]=(byte)(255-(f[1][i]&255));
            fuse(f,W,H,TIMES,es,is,4,false,4,a);
            a=probe(Arrays.copyOf(staticFrames,2),W,H,TIMES,es,is);
            fuse(new byte[][]{frame(.5,51),frame(.5,52)},W,H,new long[]{5100000000L,5133000000L},new long[]{10000000L,10000000L},new int[]{1600,1600},4,true,4,a);
        });
        test("legacy_mutable_api_never_reuses_probe",()->{
            byte[][] f={staticFrames[0].clone(),staticFrames[1].clone()};long[] es=exposure(2);int[] is=iso(2);
            FusionPixels1933.probeMotion(f[0],f[1],W,H,800,20000000L);
            for(int y=20;y<130;y++)for(int x=30;x<140;x++)f[1][y*W+x]=(byte)220;
            same(FusionPixels1934Reference.fuse(f,W,H,TIMES,es,is,4,false,4),FusionPixels1933.fuse(f,W,H,TIMES,es,is,4,false,4));
        });
        test("cancellation_duplicate_and_missing_metadata",()->{
            byte[][] f=Arrays.copyOf(staticFrames,2);long[] es=exposure(2);int[] is=iso(2);
            FusionPixels1933.Analysis a=probe(f,W,H,TIMES,es,is);
            Thread.currentThread().interrupt();
            FusionPixels1933.Result stopped=FusionPixels1933.fuse(f,W,H,TIMES,es,is,4,false,4,a);
            check(stopped.cancelled&&stopped.nv21==f[0]&&stopped.referenceTimestamp==TIMES[0],"cancel preserves original");
            check(Thread.interrupted(),"cancel status retained");check(!a.pending(),"cancel drops cache");
            a=probe(new byte[][]{f[0],f[0].clone()},W,H,TIMES,es,is);check(!a.pending(),"duplicate has no cache");
            a=probe(f,W,H,new long[]{0,0},es,is);check(!a.pending(),"missing actual metadata has no cache");
            a=probe(f,W,H,TIMES,es,is);fuse(f,W,H,TIMES,null,null,4,false,4,a);check(!a.pending(),"metadata free consumes without reuse");
        });
        test("overlapping_shots_keep_analysis_and_pixels_isolated",()->{
            final byte[][] aFrames=Arrays.copyOf(staticFrames,4),bFrames={frame(1.3,61),frame(0,62),frame(.8,63)};
            final long[] aTimes=Arrays.copyOf(TIMES,4),bTimes={6200000000L,6233000000L,6266000000L};
            final long[] aExposure=exposure(4),bExposure=exposure(3);final int[] aIso=iso(4),bIso=iso(3);
            final FusionPixels1933.Analysis aa=probe(aFrames,W,H,aTimes,aExposure,aIso),ba=probe(bFrames,W,H,bTimes,bExposure,bIso);
            final FusionPixels1933.Result[] out=new FusionPixels1933.Result[2];final Throwable[] failure=new Throwable[2];
            final java.util.concurrent.CountDownLatch begin=new java.util.concurrent.CountDownLatch(1);
            Thread one=new Thread(()->{try{begin.await();out[0]=FusionPixels1933.fuse(aFrames,W,H,aTimes,aExposure,aIso,4,false,4,aa);}catch(Throwable e){failure[0]=e;}});
            Thread two=new Thread(()->{try{begin.await();out[1]=FusionPixels1933.fuse(bFrames,W,H,bTimes,bExposure,bIso,3,true,4,ba);}catch(Throwable e){failure[1]=e;}});
            one.start();two.start();begin.countDown();one.join(10000);two.join(10000);
            check(!one.isAlive()&&!two.isAlive()&&failure[0]==null&&failure[1]==null,"overlapping shots complete");
            same(FusionPixels1934Reference.fuse(aFrames,W,H,aTimes,aExposure,aIso,4,false,4),out[0]);
            same(FusionPixels1934Reference.fuse(bFrames,W,H,bTimes,bExposure,bIso,3,true,4),out[1]);
            check(!aa.pending()&&!ba.pending(),"both sessions consumed independently");
        });
        test("dispose_and_exception_cannot_retain_capture_arrays",()->{
            byte[][] f=Arrays.copyOf(staticFrames,2);long[] es=exposure(2);int[] is=iso(2);
            FusionPixels1933.Analysis a=probe(f,W,H,TIMES,es,is);a.close();
            check(!a.pending(),"closed clears cached references");
            FusionPixels1933.probeMotion(f[0],f[1],W,H,800,es[0],a,TIMES[0],TIMES[1],800,es[1]);
            check(!a.pending(),"racing probe cannot resurrect closed capture");
            a=probe(f,W,H,TIMES,es,is);
            try{FusionPixels1933.fuse(f,W+2,H,TIMES,es,is,4,false,4,a);throw new AssertionError("validation did not fail");}
            catch(IllegalArgumentException expected){check(!a.pending(),"fuse validation exception clears cache");}
            a=probe(f,W,H,TIMES,es,is);
            try{FusionPixels1933.probeMotion(f[0],f[1],W+2,H,800,es[0],a,TIMES[0],TIMES[1],800,es[1]);throw new AssertionError("probe validation did not fail");}
            catch(IllegalArgumentException expected){check(!a.pending(),"probe validation exception clears cache");}
        });
        StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(checks).append(",\"scenarios\":{");
        boolean comma=false;for(Map.Entry<String,Integer> e:CASES.entrySet()){if(comma)out.append(',');comma=true;out.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        System.out.println(out.append("}}"));
    }
}
