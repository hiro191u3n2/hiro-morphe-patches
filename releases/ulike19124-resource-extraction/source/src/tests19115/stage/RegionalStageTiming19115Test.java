package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic, warmed full Strong Stage benchmark. The Stage, four-worker pool,
 * two-bank admission, shaders, JNI and readback are production implementations.
 * Prepared model/source/policy generation and output comparison are untimed.
 * This is not camera, Bitmap, correction, codec or publication timing. */
public final class RegionalStageTiming19115Test {
    static int assertions;static long comparedPixels,comparedConfidence;
    static Map<String,String> standard,candidate;
    static void check(boolean ok,String label){assertions++;if(!ok)throw new AssertionError(label);}
    static final class Band {
        final int first,last,origin,begin,rows;final int[] source,policy;
        Band(int first,int last,int origin,int begin,int rows,int[] source,int[] policy){
            this.first=first;this.last=last;this.origin=origin;this.begin=begin;this.rows=rows;this.source=source;this.policy=policy;
        }
    }
    static final class Frame {
        final int width,height;final StrongNoise1958.Model model;final Band[] bands;
        Frame(int id){
            GpuStrongDefault1990.Fixture seed=GpuStrongDefault1990.fixture(id);
            width=seed.model.width;height=seed.model.height;model=seed.model;
            int[] full=new int[width*height];int seedRows=seed.u[1],seedCore=seed.u[3]-seed.u[2];
            for(int y=0;y<height;y++)System.arraycopy(seed.source,(y%seedRows)*width,full,y*width,width);
            bands=new Band[(height+255)/256];
            for(int i=0;i<bands.length;i++){
                int first=i*256,last=Math.min(height,first+256),origin=Math.max(0,first-StrongNoise1958.HALO);
                int limit=Math.min(height,last+StrongNoise1958.HALO),rows=limit-origin,begin=first-origin;
                int[] input=Arrays.copyOfRange(full,origin*width,limit*width),policy=new int[width*(last-first)*2];
                for(int y=first;y<last;y++)System.arraycopy(seed.policy,(y%seedCore)*width*2,policy,(y-first)*width*2,width*2);
                bands[i]=new Band(first,last,origin,begin,rows,input,policy);
            }
        }
    }
    static final class Result {
        final int[] output,confidence;final long nanos;final int gpu,cpu,regional,direct,workers,peakWorkers;final long regionalPixels;
        Result(int[] output,int[] confidence,long nanos,int gpu,int cpu,int regional,int direct,int workers,int peakWorkers,long regionalPixels){
            this.output=output;this.confidence=confidence;this.nanos=nanos;this.gpu=gpu;this.cpu=cpu;this.regional=regional;this.direct=direct;this.workers=workers;this.peakWorkers=peakWorkers;this.regionalPixels=regionalPixels;
        }
        String json(int width,int height,boolean useNew,int trial,boolean warm){
            return "{\"width\":"+width+",\"height\":"+height+",\"candidate\":"+useNew+",\"warmup\":"+warm+",\"trial\":"+trial+
                ",\"elapsed_ns\":"+nanos+",\"gpu_bands\":"+gpu+",\"cpu_bands\":"+cpu+",\"regional_bands\":"+regional+
                ",\"regional_pixels\":"+regionalPixels+",\"direct_bands\":"+direct+",\"configured_workers\":"+workers+",\"peak_worker_tasks\":"+peakWorkers+"}";
        }
    }
    @SuppressWarnings("unchecked")
    static ThreadLocal<Boolean> benchmark()throws Exception{
        Field f=GpuResident1976.class.getDeclaredField("BENCHMARK");f.setAccessible(true);return (ThreadLocal<Boolean>)f.get(null);
    }
    static Result run(final Frame f,boolean useNew)throws Exception{
        StrongTransport1995Test.preferences.clear();StrongTransport1995Test.preferences.putAll(useNew?candidate:standard);
        check(GpuRegionalDefault19115.faster()==useNew,"controlled candidate selection");
        check(SpeedWorkers1935.maxWorkers()==4,"four production worker permits");
        final AtomicInteger next=new AtomicInteger(),live=new AtomicInteger(),peak=new AtomicInteger();
        ThreadLocal<Boolean> local=benchmark();local.set(Boolean.TRUE);
        long start=System.nanoTime();Object stage=null;final int[] output=new int[f.width*f.height],confidence=new int[((f.width+3)/4)*((f.height+3)/4)];
        try{
            // This is the already-qualified startup entry used by the real-JNI
            // transport tests. Optional preparation/qualification are untimed.
            stage=StrongTransport1995Test.stage(f.model,false);GpuStrong1960.configureWorkers1978(f.model,4);
            Runnable[] jobs=new Runnable[4];
            for(int worker=0;worker<jobs.length;worker++)jobs[worker]=new Runnable(){public void run(){
                int entered=live.incrementAndGet(),old;
                do{old=peak.get();if(entered<=old)break;}while(!peak.compareAndSet(old,entered));
                try{
                    int[] privateOutput=new int[f.width*(256+2*StrongNoise1958.HALO)+11];
                    int[] privateConfidence=new int[((f.width+3)/4)*64];
                    for(int index;(index=next.getAndIncrement())<f.bands.length;){
                        final Band b=f.bands[index];int core=b.last-b.first;
                        boolean ok=GpuStrong1960.process1981(b.source,privateOutput,f.width,b.rows,b.begin,b.begin+core,0,b.rows,b.origin,4,true,
                            f.model,b.policy,privateConfidence,null,null,new GpuStrong1960.Oracle(){public boolean run(int[] dst,int[] cf){throw new AssertionError("timed Stage selected CPU fallback");}});
                        if(!ok)throw new AssertionError("timed GPU strip unavailable: "+index);
                        System.arraycopy(privateOutput,b.begin*f.width,output,b.first*f.width,core*f.width);
                        System.arraycopy(privateConfidence,0,confidence,(b.first/4)*((f.width+3)/4),((core+3)/4)*((f.width+3)/4));
                    }
                }finally{live.decrementAndGet();}
            }};
            SpeedWorkers1935.run(jobs);
        }finally{GpuStrong1960.endStage(f.model);local.remove();}
        long elapsed=System.nanoTime()-start;
        int gpu=(Integer)StrongTransport1995Test.field(stage,"gpuStrips"),cpu=(Integer)StrongTransport1995Test.field(stage,"cpuStrips");
        int regional=(Integer)StrongTransport1995Test.field(stage,"regionalStrips19115"),direct=(Integer)StrongTransport1995Test.field(stage,"directStrips1995");
        long regionalPixels=(Long)StrongTransport1995Test.field(stage,"regionalPixels19115");
        int wanted=useNew?f.height/256:0;
        check(gpu==f.bands.length&&cpu==0&&direct==f.bands.length,"every full-frame strip used direct GPU");
        check(regional==wanted&&regionalPixels==(long)wanted*f.width*256,"only eligible complete bands use prepass");
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"timed close released ownership");
        long[] pool=RegionalStage19115Test.probeNative();check(pool[2]==0&&pool[3]==0,"timed close removed prepass storage");
        check(peak.get()==4,"all four real workers ran");
        return new Result(output,confidence,elapsed,gpu,cpu,regional,direct,4,peak.get(),regionalPixels);
    }
    static void equal(Result a,Result b){
        check(Arrays.equals(a.output,b.output),"whole-frame ARGB equality");check(Arrays.equals(a.confidence,b.confidence),"whole-frame confidence equality");
        comparedPixels+=a.output.length;comparedConfidence+=a.confidence.length;
    }
    public static void main(String[] args)throws Exception{
        StrongTransport1995Test.store();check(StrongNoise1958.nativeAvailable(),"native CPU oracle available for untimed startup proof");
        check(GpuStrongDefault1990.qualify(StrongTransport1995Test.GO),"actual untimed startup quality proof");
        standard=new HashMap<String,String>(StrongTransport1995Test.preferences);
        check(GpuNoise1960.prepareRegional19115(),"untimed optional preparation");
        check(GpuCertificates1990.passed(GpuRegionalDefault19115.EXACT,70,100000,200000,100000),"controlled exact switch");
        check(GpuCertificates1990.passed(GpuRegionalDefault19115.FAST,70,100000,200000,100000),"controlled fast switch");
        candidate=new HashMap<String,String>(StrongTransport1995Test.preferences);
        for(int id:new int[]{64,65}){
            Frame f=new Frame(id);System.out.println("FRAME_READY "+f.width+"x"+f.height);
            Result warmA=run(f,false),warmB=run(f,true);equal(warmA,warmB);
            System.out.println("SAMPLE "+warmA.json(f.width,f.height,false,-1,true));System.out.println("SAMPLE "+warmB.json(f.width,f.height,true,-1,true));
            for(int trial=0;trial<3;trial++){
                Result a,b;
                if((trial&1)==0){a=run(f,false);System.out.println("SAMPLE "+a.json(f.width,f.height,false,trial,false));b=run(f,true);System.out.println("SAMPLE "+b.json(f.width,f.height,true,trial,false));}
                else{b=run(f,true);System.out.println("SAMPLE "+b.json(f.width,f.height,true,trial,false));a=run(f,false);System.out.println("SAMPLE "+a.json(f.width,f.height,false,trial,false));}
                equal(a,b);equal(warmA,a);
            }
        }
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"compared_pixels\":"+comparedPixels+",\"compared_confidence_words\":"+comparedConfidence+",\"actual_jni\":true,\"physical_android_tested\":false}");
    }
}
