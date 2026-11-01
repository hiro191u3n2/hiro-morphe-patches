package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Real production GLES/JNI candidate, bitmap strips, CPU route and ownership. */
public final class PipelineGpu1952Test {
    static int assertions,cases,pixels;
    static void yes(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static final Class<?> WORK;
    static final Constructor<?> NEW;
    static final Method GPU,CPU,EQUAL,PUBLISH,DISCARD;
    static final Field CANDIDATE;
    static {
        try {
            WORK=Class.forName("com.hiro.ulike.QualityPipeline1932$FinishWork1952");
            NEW=WORK.getDeclaredConstructor(Bitmap.class,QualityPixels1932.Plan.class,QualityPixels1932.Plan.class,boolean.class,boolean.class,boolean.class);NEW.setAccessible(true);
            GPU=method("gpu");CPU=method("cpu");EQUAL=method("equal");PUBLISH=method("publishGpu");DISCARD=method("discardGpu");
            CANDIDATE=WORK.getDeclaredField("candidate");CANDIDATE.setAccessible(true);
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    static Method method(String name)throws Exception{Method m=WORK.getDeclaredMethod(name);m.setAccessible(true);return m;}
    static Object work(Bitmap b,QualityPixels1932.Plan p,boolean seq)throws Exception{return NEW.newInstance(b,p,seq?p:null,true,true,seq);}
    static int[] pattern(int width,int height,long seed){
        Random r=new Random(seed);int[] p=new int[width*height];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int luma=50+(x*3+y*5)%140+r.nextInt(33)-16;
            int red=Math.min(255,luma+(x%7)*4),green=luma,blue=Math.max(0,luma-(y%5)*3);
            p[y*width+x]=((x+y)%17==0?0x88000000:0xff000000)|(red<<16)|(green<<8)|blue;
        }return p;
    }
    static QualityPixels1932.Plan plan(final Bitmap b,boolean texture,boolean halo){
        SpatialNoise1934 map=PipelineQuality1942Test.probe(b);
        return QualityPixels1932.plan(map.global,1200,30000000L,1,.4f,3,4,texture,true,1)
            .withHaloSuppression(halo).withOutputNoise(map)
            .withFaceRegions(FaceRegions1934.forBitmap(b,0));
    }
    static Bitmap image(int width,int height,long seed){return Bitmap.from(width,height,pattern(width,height,seed),Bitmap.Config.ARGB_8888,true);}
    static void exact(int width,int height,boolean seq,boolean texture,boolean halo)throws Exception{
        Bitmap owned=image(width,height,19521234L+height);owned.setDensity(320);owned.setHasAlpha(true);
        int[] before=owned.snapshot();QualityPixels1932.Plan p=plan(owned,texture,halo);
        ShotContext1932.SHOTS.put(owned,PipelineQuality1942Test.metadata(1200));
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(owned);
        Object w=work(owned,p,seq);
        yes(Boolean.TRUE.equals(GPU.invoke(w)),"actual GPU complete route executes");
        Bitmap candidate=(Bitmap)CANDIDATE.get(w);
        yes(candidate!=owned && !candidate.isRecycled(),"GPU owns disjoint unpublished bitmap");
        yes(Arrays.equals(owned.snapshot(),before),"candidate stage never changes CPU source");
        yes(Boolean.TRUE.equals(CPU.invoke(w)),"complete baseline CPU bitmap route executes");
        yes(Boolean.TRUE.equals(EQUAL.invoke(w)),"actual GPU and full CPU final pixels match");
        int[] expected=owned.snapshot(),actual=candidate.snapshot();
        for(int i=0;i<actual.length;i++){yes(expected[i]==actual[i],"exact consumed bitmap pixel "+i);pixels++;}
        PUBLISH.invoke(w);DISCARD.invoke(w);
        yes(!candidate.isRecycled(),"publication hands bitmap ownership to caller");
        yes(ProcessingTiming1947.traceFor(candidate)==trace,"published GPU bitmap retains exact shot trace");
        yes(ShotContext1932.forBitmap(candidate)==ShotContext1932.forBitmap(owned),"published GPU bitmap retains exact captured metadata");
        yes(candidate.getDensity()==320 && candidate.hasAlpha(),"density and alpha survive GPU candidate route");
        ProcessingTiming1947.finish(trace,true);candidate.recycle();owned.recycle();cases++;
    }
    static void failure(int mode)throws Exception{
        Bitmap owned=image(77,399,93001+mode);int[] original=owned.snapshot();
        QualityPixels1932.Plan p=plan(owned,true,true);Object w=work(owned,p,true);
        if(mode==0)Bitmap.failCopyOnce=true;
        if(mode==1)Bitmap.failNextCopyWrite=true;
        if(mode==2)Thread.currentThread().interrupt();
        boolean failed=false;
        try{GPU.invoke(w);}catch(java.lang.reflect.InvocationTargetException expected){failed=true;}
        finally{Thread.interrupted();}
        yes(failed,"copy/write/interruption failure rejects incomplete GPU candidate "+mode);
        yes(Arrays.equals(original,owned.snapshot()) && !owned.isRecycled(),"GPU failure keeps original CPU bitmap untouched "+mode);
        Bitmap candidate=(Bitmap)CANDIDATE.get(w);DISCARD.invoke(w);
        yes(candidate==null || candidate.isRecycled(),"unpublished partial candidate released "+mode);
        Bitmap expected=Bitmap.from(77,399,original,Bitmap.Config.ARGB_8888,true);
        QualityPipeline1932.finishInPlace(expected,p,true,false);QualityPipeline1932.finishInPlace(expected,p,false,true);
        yes(Boolean.TRUE.equals(CPU.invoke(w)),"full original CPU route remains executable "+mode);
        yes(Arrays.equals(expected.snapshot(),owned.snapshot()),"failed candidate fallback exactly restores established route "+mode);
        yes(Bitmap.writesAfterRecycle.get()==0,"GPU failure drains before recycling owned bitmap "+mode);
        expected.recycle();owned.recycle();cases++;
    }
    static void wholeRoute()throws Exception{
        int width=59,height=271;int[] original=pattern(width,height,94194L);
        for(int shot=0;shot<4;shot++){
            Bitmap owned=Bitmap.from(width,height,original,Bitmap.Config.ARGB_8888,true),expected=Bitmap.from(width,height,original,Bitmap.Config.ARGB_8888,true);
            QualityPixels1932.Plan p=plan(owned,true,true);
            QualityPipeline1932.finishInPlace(expected,p,true,false);QualityPipeline1932.finishInPlace(expected,p,false,true);
            ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(owned);
            ShotContext1932.SHOTS.put(owned,PipelineQuality1942Test.metadata(800));
            Bitmap output=QualityPipeline1932.finishRoute1952(owned,p,p,true,true,true);
            yes(Arrays.equals(output.snapshot(),expected.snapshot()),"production route chooses only exact final saved pixels "+shot);
            yes(!output.isRecycled() && !owned.isRecycled(),"route leaves successful result alive for caller "+shot);
            yes(ProcessingTiming1947.traceFor(output)==trace,"real gate keeps exact shot trace "+shot);
            // A distinct published candidate survives close, while an unpublished
            // candidate is recycled. Caller recycles its prior owned bitmap.
            if(output!=owned)output.recycle();owned.recycle();expected.recycle();ProcessingTiming1947.finish(trace,true);cases++;
        }
    }
    public static void main(String[] args)throws Exception{
        yes(GpuFinish1952.available(),"production host JNI library is actually loaded");
        for(boolean sequential:new boolean[]{false,true})for(boolean texture:new boolean[]{false,true})for(boolean halo:new boolean[]{false,true})
            for(int height:new int[]{37,257,399})exact(77,height,sequential,texture,halo);
        for(int mode=0;mode<3;mode++)failure(mode);
        wholeRoute();SpeedWorkers1935.trim();
        yes(Bitmap.writesAfterRecycle.get()==0,"all candidate strips drained before recycle");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"pixels_compared\":"+pixels+",\"actual_bitmap_gpu_candidate_executed\":true,\"whole_cpu_bitmap_candidate_pixel_exact\":true,\"gpu_copy_write_interrupt_failure_fallback_exact\":true,\"published_candidate_shot_ownership_preserved\":true,\"source_unmodified_before_cpu_fallback\":true,\"device_tested\":false}");
    }
}
