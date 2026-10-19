package com.hiro.ulike;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.CancellationException;

/** GX1/2/4/7/8 exact NR9--NR13 GPU candidates. No photograph is retained by
 * the bounded admission cache. A stage owns a private session until endStage;
 * immutable evidence/maps stay on GPU while the strip source/output are reused.
 * During qualification the original CPU oracle provides the published output.
 * Both full ARGB and sparse NR13 confidence must match, twice consecutively,
 * and upload/dispatch/read/close-inclusive GPU time must be at least 5% lower.
 */
public final class GpuStrong1960 {
    private GpuStrong1960() {}
    interface Oracle { boolean run(int[] destination,int[] confidence); }
    private static final int[] BINDINGS={0,1,2,3,4,5,6,7};
    private static final int LIMIT=64;
    private static final LinkedHashMap<String,Gate> GATES=new LinkedHashMap<String,Gate>(LIMIT,.75f,true);
    private static volatile Stage active;
    private static final class Gate {int consecutive;long cpuBaseline;boolean rejected,accepted;}
    private static final class Sample {
        final String key;final long gpu,cpu;final int pixels;final boolean exact,observation;
        Sample(String key,long gpu,long cpu,int pixels,boolean exact,boolean observation){this.key=key;this.gpu=gpu;this.cpu=cpu;this.pixels=pixels;this.exact=exact;this.observation=observation;}
    }
    private static final class Stage {
        final StrongNoise1958.Model model;GpuNoise1960.Session session;
        final ArrayList<Sample> samples=new ArrayList<Sample>();
        long setup,setupBegin,setupEnd,pixels;boolean closed,unavailable,failed;
        Stage(StrongNoise1958.Model model){this.model=model;}
    }
    private static boolean interrupted(){return Thread.currentThread().isInterrupted();}
    private static void check(){if(interrupted())throw new CancellationException("GPU strong NR interrupted");}
    private static boolean accepted(String key){synchronized(GATES){Gate g=GATES.get(key);return g!=null&&g.accepted&&!g.rejected;}}
    private static boolean rejected(String key){synchronized(GATES){Gate g=GATES.get(key);return g!=null&&g.rejected;}}
    private static void record(String key,boolean exact,long gpu,long cpu){
        synchronized(GATES){
            Gate gate=GATES.get(key);if(gate==null){gate=new Gate();GATES.put(key,gate);}
            if(!exact||cpu<=0||gpu<=0||gpu>cpu-cpu/20){gate.rejected=true;gate.accepted=false;gate.consecutive=0;}
            else if(!gate.rejected){gate.cpuBaseline=gate.cpuBaseline==0?cpu:Math.min(gate.cpuBaseline,cpu);if(++gate.consecutive>=2)gate.accepted=true;}
            while(GATES.size()>LIMIT){String first=GATES.keySet().iterator().next();GATES.remove(first);}
        }
    }
    private static void observe(String key,long gpu){
        synchronized(GATES){Gate gate=GATES.get(key);if(gate!=null&&gate.accepted&&!gate.rejected&&
            (gate.cpuBaseline<=0||gpu>gate.cpuBaseline-gate.cpuBaseline/20)){gate.accepted=false;gate.rejected=true;gate.consecutive=0;}}
    }
    private static void fail(String key){record(key,false,1,1);}
    private static String key(int[] u){
        // All branches, alpha/bounds handling and representative-cell alignment
        // belong to the key. No source pixels, mask or model evidence are stored.
        return GpuNoise1960.fingerprint()+":"+u[10]+":"+u[0]+":"+u[1]+":"+(u[3]-u[2])+":"+
               u[4]+":"+u[5]+":"+(u[2]&7)+":"+(u[6]&63)+":"+u[7]+":"+u[8]+":"+u[9]+":"+u[11]+":"+u[12]+":"+u[18]+":"+u[19];
    }
    /** Called once immediately before starting this model's strip tasks. */
    public static void beginStage(StrongNoise1958.Model model){
        if(model==null||interrupted())return;
        synchronized(GpuStrong1960.class){
            if(active==null)active=new Stage(model);
        }
    }
    /** Called under stage's monitor only after an eligible non-rejected key.
     * A CPU-only stage never opens EGL or transfers a model. */
    private static boolean initialize(Stage stage,long rangePeak){
        if(stage.closed||stage.unavailable)return false;if(stage.session!=null)return true;
        float[] evidence=StrongNoise1958.gpuEvidence1960(stage.model);int[][] maps=StrongNoise1958.gpuMaps1960(stage.model);
        long bytes=4L*(evidence.length+(long)maps[0].length+maps[1].length+maps[2].length);
        if(maps[0].length==0||maps[1].length==0||maps[2].length==0||bytes>GpuNoise1960.MAX_BYTES||
           !GpuNoise1960.workspaceFits(bytes+rangePeak)){stage.unavailable=true;return false;}
        stage.setupBegin=System.nanoTime();GpuNoise1960.Session session=GpuNoise1960.open();
        if(session==null){stage.unavailable=true;return false;}
        boolean success=false;
        try{
            if(!session.upload(2,evidence)||!session.upload(4,maps[0])||!session.upload(5,maps[1])||!session.upload(6,maps[2]))return false;
            check();stage.session=session;stage.setupEnd=System.nanoTime();stage.setup=stage.setupEnd-stage.setupBegin;success=true;return true;
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){return false;}catch(LinkageError unavailable){return false;}catch(OutOfMemoryError unavailable){return false;}
        finally{if(!success){stage.unavailable=true;stage.failed=true;session.close();}}
    }
    /** Worker tasks must all join before this call. End closes/frees GPU images
     * and accounts the actual stage setup/close cost before admitting a key. */
    public static void endStage(StrongNoise1958.Model model){
        Stage stage;
        synchronized(GpuStrong1960.class){stage=active;if(stage==null||stage.model!=model)return;active=null;}
        synchronized(stage){
            if(stage.closed)return;stage.closed=true;if(stage.session==null){stage.samples.clear();return;}
            long start=System.nanoTime();stage.session.close();stage.session=null;
            long overhead=stage.setup+System.nanoTime()-start;
            for(Sample sample:stage.samples){
                long share=stage.pixels==0?overhead:(long)((double)overhead*sample.pixels/stage.pixels);
                // The original CPU stage may execute several strips together.
                // Compare against its conservative parallel throughput bound,
                // as well as including the actual GPU monitor queue wait.
                if(sample.observation)observe(sample.key,sample.gpu+share);
                else record(sample.key,sample.exact,sample.gpu+share,sample.cpu/Math.max(1,SpeedWorkers1935.maxWorkers()));
            }
            stage.samples.clear();
        }
    }
    static boolean prepare(int[] source,int[] destination,int width,int height,int noise,
            boolean shadows,float[] evidence,int mode,Oracle oracle){
        if(interrupted()||source==null||destination==null||width<1||height<1||
           source.length<(long)width*height||destination.length<(long)width*height||
           evidence==null||evidence.length<16||mode<0||mode>2)return false;
        int[] u=new int[32];u[0]=width;u[1]=height;u[3]=height;u[5]=height;u[7]=height;u[8]=noise;u[9]=shadows?1:0;u[10]=mode;
        if(active!=null)return false;
        if(rejected(key(u))||!GpuNoise1960.supports(GpuNoise1960.STRONG))return false;
        String key=key(u);if(rejected(key))return false;
        if(!GpuNoise1960.workspaceFits(8L*source.length+8L*width*height+4L*evidence.length+1048576L))return false;
        long start=System.nanoTime();Result candidate=ephemeral(source,evidence,null,null,u,null);
        long gpu=System.nanoTime()-start;check();
        if(candidate==UNAVAILABLE)return false;if(candidate==null){fail(key);return false;}
        if(accepted(key)){observe(key,gpu);System.arraycopy(candidate.pixels,0,destination,0,width*height);return true;}
        start=System.nanoTime();boolean ok=oracle.run(destination,null);long cpu=System.nanoTime()-start;check();
        if(!ok){fail(key);return false;}
        boolean exact=true;for(int i=0;i<width*height;i++)if(candidate.pixels[i]!=destination[i]){exact=false;break;}
        record(key,exact,gpu,cpu);return true;
    }
    static boolean process(int[] source,int[] destination,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            StrongNoise1958.Model model,int[] policy,int[] confidence,
            GpuPolicy1960.Protection gpuProtection,Oracle oracle){
        if(model==null||noise<=0||end<=begin||interrupted())return false;
        int[] u=new int[32];u[0]=width;u[1]=rows;u[2]=begin;u[3]=end;u[4]=validBegin;u[5]=validEnd;
        u[6]=originY;u[7]=model.height;u[8]=noise;u[9]=shadows?1:0;u[10]=3;u[11]=policy==null?0:1;u[12]=confidence==null?0:1;
        if(gpuProtection!=null&&gpuProtection.plan!=null){u[18]=gpuProtection.plan.beautyQ8;u[19]=gpuProtection.plan.shadowBudgetQ8;}
        if(rejected(key(u))||!GpuNoise1960.supports(GpuNoise1960.STRONG))return false;
        String key=key(u);if(rejected(key))return false;
        long extra=8L*source.length+24L*width*(end-begin)+1048576L;
        if(gpuProtection!=null)extra+=16L*width*(end-begin);
        if(!GpuNoise1960.workspaceFits(extra))return false;
        Stage stage=active;Result candidate;long start=System.nanoTime(),gpu;int count=width*(end-begin);
        if(stage!=null&&stage.model!=model)return false;
        GpuPolicy1960.PolicyData gpuPolicy=gpuProtection==null?null:gpuProtection.data(width,end-begin,originY+begin);
        if(stage!=null&&stage.model==model){
            synchronized(stage){
                if(!initialize(stage,extra)){if(stage.failed)fail(key);return false;}
                // Model upload/open is shared once across the stage. Remove
                // only the startup interval overlapping this call; setup/close
                // is later apportioned once, and GPU dispatch queue wait remains.
                long startupOverlap=Math.max(0L,stage.setupEnd-Math.max(start,stage.setupBegin));
                candidate=execute(stage.session,source,policy,confidence,u,gpuPolicy);
                gpu=System.nanoTime()-start-startupOverlap;
            }
        }else{candidate=ephemeral(source,StrongNoise1958.gpuEvidence1960(model),StrongNoise1958.gpuMaps1960(model),policy,u,gpuPolicy);gpu=System.nanoTime()-start;stage=null;}
        check();if(candidate==UNAVAILABLE)return false;if(candidate==null){fail(key);return false;}
        if(accepted(key)){
            observe(key,gpu); // Disable a slowed route before other queued strips.
            if(stage!=null)synchronized(stage){if(!stage.closed){stage.samples.add(new Sample(key,gpu,0,count,true,true));stage.pixels+=count;}else fail(key);}
            System.arraycopy(candidate.pixels,0,destination,begin*width,count);
            if(confidence!=null)System.arraycopy(candidate.confidence,0,confidence,0,candidate.confidence.length);
            return true;
        }
        start=System.nanoTime();boolean ok=oracle.run(destination,confidence);long cpu=System.nanoTime()-start;check();
        if(!ok){fail(key);return false;}
        boolean exact=true;for(int i=0;i<count;i++)if(candidate.pixels[i]!=destination[begin*width+i]){exact=false;break;}
        if(exact&&confidence!=null)for(int i=0;i<candidate.confidence.length;i++)if(candidate.confidence[i]!=confidence[i]){exact=false;break;}
        if(stage==null)record(key,exact,gpu,cpu/Math.max(1,SpeedWorkers1935.maxWorkers()));
        else synchronized(stage){if(!stage.closed){stage.samples.add(new Sample(key,gpu,cpu,count,exact,false));stage.pixels+=count;}else fail(key);}
        return true; // Qualification publishes the independently computed CPU output.
    }
    private static final class Result {
        final int[] pixels,confidence;
        Result(int[] pixels,int[] confidence){this.pixels=pixels;this.confidence=confidence;}
    }
    private static final Result UNAVAILABLE=new Result(null,null);
    private static Result ephemeral(int[] source,float[] evidence,int[][] maps,int[] policy,
            int[] u,GpuPolicy1960.PolicyData gpuPolicy){
        long additional=8L*source.length+24L*u[0]*(u[3]-u[2])+4L*evidence.length+1048576L;
        if(maps!=null)for(int[] map:maps)additional+=4L*map.length;
        if(gpuPolicy!=null)additional+=16L*gpuPolicy.count+4L*gpuPolicy.grid.length;
        if(!GpuNoise1960.workspaceFits(additional))return UNAVAILABLE;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return UNAVAILABLE;
        try{
            if(!session.upload(2,evidence))return null;
            for(int k=0;k<3;k++)if(!session.upload(4+k,maps==null?new int[]{0}:maps[k]))return null;
            return execute(session,source,policy,null,u,gpuPolicy);
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){return null;}catch(LinkageError unavailable){return null;}catch(OutOfMemoryError unavailable){return null;}
        finally{session.close();}
    }
    private static Result execute(GpuNoise1960.Session session,int[] source,int[] policy,
            int[] confidence,int[] u,GpuPolicy1960.PolicyData gpuPolicy){
        int count=u[0]*(u[3]-u[2]);int confidenceCount=u[12]!=0?((u[0]+3)/4)*((u[3]-u[2]+3)/4):1;
        try{
            if(!session.upload(0,source)||!session.allocate(1,4L*count)||
               !session.upload(3,policy==null?new int[]{0}:policy)||!session.allocate(7,4L*confidenceCount))return null;
            // The immutable face/detail inputs are exported by the existing
            // detector. The GPU computes the original protection amounts and
            // sigma; its result is checked before NR is allowed to consume it.
            if(gpuPolicy!=null){
                if(!GpuPolicy1960.uploadPolicy(session,gpuPolicy,3,8,9,10,11))return null;
                int[] measured=session.readInts(3,count*2);if(measured==null||policy==null)return null;
                for(int i=0;i<count*2;i++)if(measured[i]!=policy[i])return null;
            }
            if(!session.dispatch(GpuNoise1960.STRONG,BINDINGS,u,null,count))return null;
            int[] pixels=session.readInts(1,count);if(pixels==null)return null;
            int[] cf=u[12]!=0?session.readInts(7,confidenceCount):null;if(u[12]!=0&&cf==null)return null;
            return new Result(pixels,cf);
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){return null;}catch(LinkageError unavailable){return null;}catch(OutOfMemoryError unavailable){return null;}
    }
}
