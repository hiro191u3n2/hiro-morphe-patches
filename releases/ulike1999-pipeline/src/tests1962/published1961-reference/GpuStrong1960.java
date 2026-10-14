package com.hiro.ulike;

import java.util.ArrayList;
import java.util.Arrays;
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
    // GX19: two disjoint mutable banks share only immutable model2/4/5/6.
    // Order:source,out,policy,confidence,mask,grid,sigma,dummy,expected,diff.
    private static final int[][] BANKS={{0,1,3,7,8,9,10,11,12,13},{14,15,16,17,18,19,20,21,22,23}};
    private static final int LIMIT=64;
    private static final LinkedHashMap<String,Gate> GATES=new LinkedHashMap<String,Gate>(LIMIT,.75f,true);
    private static volatile Stage active;
    private static final class Gate {int consecutive,variant;long cpuBaseline;boolean rejected,accepted;}
    private static final class Sample {
        final String key;final long gpu,cpu;final int pixels;final boolean exact,observation;
        Sample(String key,long gpu,long cpu,int pixels,boolean exact,boolean observation){this.key=key;this.gpu=gpu;this.cpu=cpu;this.pixels=pixels;this.exact=exact;this.observation=observation;}
    }
    private static final class Stage {
        final StrongNoise1958.Model model;GpuNoise1960.Session session;
        final ArrayList<Sample> samples=new ArrayList<Sample>();
        final boolean[] banks=new boolean[2];
        long setup,setupBegin,setupEnd,pixels;boolean closed,unavailable,failed;
        Stage(StrongNoise1958.Model model){this.model=model;}
    }
    private static boolean interrupted(){return Thread.currentThread().isInterrupted();}
    private static void check(){if(interrupted())throw new CancellationException("GPU strong NR interrupted");}
    private static boolean accepted(String key){synchronized(GATES){Gate g=GATES.get(key);return g!=null&&g.accepted&&!g.rejected;}}
    private static int choice(String key){synchronized(GATES){Gate g=GATES.get(key);return g==null?0:g.variant;}}
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
        boolean invalid=false;
        synchronized(GATES){Gate gate=GATES.get(key);if(gate!=null&&gate.accepted&&!gate.rejected&&
            (gate.cpuBaseline<=0||gpu>gate.cpuBaseline-gate.cpuBaseline/20)){gate.accepted=false;gate.rejected=true;gate.consecutive=0;invalid=true;}}
        if(invalid)GpuQualification1961.reject(key);
    }
    private static void fail(String key){record(key,false,1,1);GpuQualification1961.reject(key);}
    private static void restore(String key){
        if(accepted(key)||rejected(key))return;
        GpuQualification1961.Record saved=GpuQualification1961.restore(key);if(saved==null)return;
        synchronized(GATES){Gate gate=GATES.get(key);if(gate==null){gate=new Gate();GATES.put(key,gate);}
            if(!gate.rejected){gate.accepted=true;gate.consecutive=2;gate.cpuBaseline=saved.cpuNanos;gate.variant=saved.variant;}
            while(GATES.size()>LIMIT){String first=GATES.keySet().iterator().next();GATES.remove(first);}}
    }
    private static String key(int[] u){
        // All branches, alpha/bounds handling and representative-cell alignment
        // belong to the key. No source pixels, mask or model evidence are stored.
        return "strong-gx1961-cache-policy-bank-v1:"+u[10]+":"+u[0]+":"+u[1]+":"+(u[3]-u[2])+":"+
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
        // GX20: a qualified pyramid/evidence builder may hand over the exact
        // resident model. Ownership is consumed once; no maps are reuploaded.
        GpuAnalysis1961.Resident carried=StrongNoise1958.takeResident1961(stage.model);
        if(carried!=null){
            if(!GpuNoise1960.workspaceFits(rangePeak)){carried.close();stage.unavailable=true;return false;}
            stage.setupBegin=System.nanoTime();stage.session=carried.session;
            stage.setupEnd=System.nanoTime();stage.setup=carried.setupNanos+stage.setupEnd-stage.setupBegin;return true;
        }
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
        synchronized(GpuStrong1960.class){stage=active;if(stage==null||stage.model!=model){StrongNoise1958.discardResident1961(model);return;}active=null;}
        synchronized(stage){
            if(stage.closed)return;stage.closed=true;if(stage.session==null){
                GpuAnalysis1961.Resident unused=StrongNoise1958.takeResident1961(model);if(unused!=null)unused.close();stage.samples.clear();return;
            }
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
        String key=key(u);restore(key);
        if(!accepted(key)||GpuQualification1961.background()){
            boolean ok=oracle.run(destination,null);check();
            if(ok&&!rejected(key))schedulePrepare(key,source,evidence,u);return ok;
        }
        if(active!=null||!GpuNoise1960.supports(GpuNoise1960.variant(GpuNoise1960.STRONG,choice(key))))return false;
        if(!GpuNoise1960.workspaceFits(8L*source.length+8L*width*height+4L*evidence.length+1048576L))return false;
        long start=System.nanoTime();Result candidate=ephemeral(source,evidence,null,null,u,null,choice(key));
        long gpu=System.nanoTime()-start;check();
        if(candidate==UNAVAILABLE)return false;if(candidate==null){fail(key);return false;}
        observe(key,gpu);System.arraycopy(candidate.pixels,0,destination,0,width*height);return true;
    }
    static boolean process(int[] source,int[] destination,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            StrongNoise1958.Model model,int[] policy,int[] confidence,
            GpuPolicy1960.Protection gpuProtection,Oracle oracle){
        if(model==null||noise<=0||end<=begin||interrupted())return false;
        int[] u=new int[32];u[0]=width;u[1]=rows;u[2]=begin;u[3]=end;u[4]=validBegin;u[5]=validEnd;
        u[6]=originY;u[7]=model.height;u[8]=noise;u[9]=shadows?1:0;u[10]=3;u[11]=policy==null?0:1;u[12]=confidence==null?0:1;
        if(gpuProtection!=null&&gpuProtection.plan!=null){u[18]=gpuProtection.plan.beautyQ8;u[19]=gpuProtection.plan.shadowBudgetQ8;}
        String key=key(u);restore(key);
        if(!accepted(key)||GpuQualification1961.background()){
            boolean ok=oracle.run(destination,confidence);check();
            if(ok&&!rejected(key))scheduleFull(key,source,policy,u,model,gpuProtection);return ok;
        }
        if(!GpuNoise1960.supports(GpuNoise1960.variant(GpuNoise1960.STRONG,choice(key))))return false;
        long extra=8L*source.length+24L*width*(end-begin)+1048576L;
        if(gpuProtection!=null)extra+=16L*width*(end-begin);
        if(!GpuNoise1960.workspaceFits(extra))return false;
        Stage stage=active;Result candidate;long start=System.nanoTime(),gpu;int count=width*(end-begin);
        if(stage!=null&&stage.model!=model)return false;
        GpuPolicy1960.PolicyData gpuPolicy=gpuProtection==null?null:gpuProtection.data(width,end-begin,originY+begin);
        if(stage!=null&&stage.model==model){
            int bank=-1;GpuNoise1960.Ticket ticket=null;
            synchronized(stage){
                if(!initialize(stage,extra)){if(stage.failed)fail(key);return false;}
                for(int i=0;i<2;i++)if(!stage.banks[i]){bank=i;stage.banks[i]=true;break;}
                if(bank<0)return false; // Bounded banks: other strip workers keep CPU fallback.
                // Model upload/open is shared once across the stage. Remove
                // only the startup interval overlapping this call; setup/close
                // is later apportioned once, and GPU dispatch queue wait remains.
                long startupOverlap=Math.max(0L,stage.setupEnd-Math.max(start,stage.setupBegin));
                try{ticket=stage.session.submit(commands(source,policy,u,gpuPolicy,BANKS[bank],choice(key)),bank);}
                catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
                gpu=startupOverlap;
            }
            try{
                if(ticket==null)candidate=null;
                else candidate=collect(stage.session,ticket,u,BANKS[bank]);
                gpu=System.nanoTime()-start-gpu;
            }finally{synchronized(stage){stage.banks[bank]=false;stage.notifyAll();}}
        }else{candidate=ephemeral(source,StrongNoise1958.gpuEvidence1960(model),StrongNoise1958.gpuMaps1960(model),policy,u,gpuPolicy,choice(key));gpu=System.nanoTime()-start;stage=null;}
        check();if(candidate==UNAVAILABLE)return false;if(candidate==null){fail(key);return false;}
        observe(key,gpu); // Disable a slowed route before other queued strips.
        if(stage!=null)synchronized(stage){if(!stage.closed){stage.samples.add(new Sample(key,gpu,0,count,true,true));stage.pixels+=count;}else fail(key);}
        System.arraycopy(candidate.pixels,0,destination,begin*width,count);
        if(confidence!=null)System.arraycopy(candidate.confidence,0,confidence,0,candidate.confidence.length);
        return true;
    }
    private static final class Result {
        final int[] pixels,confidence;
        Result(int[] pixels,int[] confidence){this.pixels=pixels;this.confidence=confidence;}
    }
    private static boolean canSchedule(){return !GpuQualification1961.background()&&!interrupted()&&GpuQualification1961.retainedBytes()==0;}
    private static void schedulePrepare(String key,int[] source,float[] evidence,int[] u){
        if(!canSchedule())return;long count=(long)u[0]*u[1],bytes=4L*count+64;
        if(count>Integer.MAX_VALUE||bytes>96L*1024*1024||!GpuNoise1960.workspaceFits(bytes+12L*count))return;
        try{GpuQualification1961.schedule(key,bytes,new Proof(key,Arrays.copyOf(source,(int)count),null,Arrays.copyOf(evidence,16),u.clone(),null,null));}
        catch(RuntimeException unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static void scheduleFull(String key,int[] source,int[] policy,int[] u,StrongNoise1958.Model model,GpuPolicy1960.Protection protection){
        if(!canSchedule())return;long count=(long)u[0]*u[1],core=(long)u[0]*(u[3]-u[2]);
        int[][] maps=StrongNoise1958.gpuMaps1960(model);float[] evidence=StrongNoise1958.gpuEvidence1960(model);
        long bytes=4L*(count+(policy==null?0:core*2)+maps[0].length+(long)maps[1].length+maps[2].length+evidence.length);
        if(protection!=null)bytes+=8L*core;
        if(count>Integer.MAX_VALUE||core>Integer.MAX_VALUE/2||bytes>96L*1024*1024||!GpuNoise1960.workspaceFits(bytes+12L*count))return;
        try{
            GpuPolicy1960.PolicyData descriptor=protection==null?null:protection.data(u[0],u[3]-u[2],u[6]+u[2]);
            GpuQualification1961.schedule(key,bytes,new Proof(key,Arrays.copyOf(source,(int)count),policy==null?null:Arrays.copyOf(policy,(int)core*2),null,u.clone(),model,descriptor));
        }catch(RuntimeException unavailable){}catch(OutOfMemoryError unavailable){}
    }
    /** GX22/GX23: private idle-only complete-output proofs for every supported
     * 32/64/128 layout. Source and policy are copied before foreground return;
     * model maps/evidence are immutable and retained only for this one job.
     * No foreground Workspace, pooled input or Oracle closure is reused. */
    private static final class Proof implements GpuQualification1961.Probe {
        final String key;int[] source,policy,u;float[] evidence;
        StrongNoise1958.Model model;GpuPolicy1960.PolicyData descriptor;
        Proof(String key,int[] source,int[] policy,float[] evidence,int[] u,StrongNoise1958.Model model,GpuPolicy1960.PolicyData descriptor){
            this.key=key;this.source=source;this.policy=policy;this.evidence=evidence;this.u=u;this.model=model;this.descriptor=descriptor;
        }
        private boolean cpu(int[] destination,int[] confidence){
            if(u[10]!=3)return StrongNoise1958.gpuPrepareOracleSnapshot1961(source,destination,u[0],u[1],u[8],u[9]!=0,evidence,u[10]);
            return StrongNoise1958.gpuOracleSnapshot1961(source,destination,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,model,policy,confidence);
        }
        public void run(GpuQualification1961.Cancellation cancellation){
            if(cancellation.cancelled()||source==null)return;
            boolean[] supported=new boolean[3];boolean any=false;
            for(int choice=0;choice<3;choice++){if(cancellation.cancelled())return;supported[choice]=GpuNoise1960.supports(GpuNoise1960.variant(GpuNoise1960.STRONG,choice));any|=supported[choice];}
            if(!any||cancellation.cancelled())return;
            int core=u[0]*(u[3]-u[2]),cfCount=u[12]!=0?((u[0]+3)/4)*((u[3]-u[2]+3)/4):0;
            float[] bins=model==null?evidence:StrongNoise1958.gpuEvidence1960(model);int[][] maps=model==null?null:StrongNoise1958.gpuMaps1960(model);
            long nativeModel=4L*bins.length;if(maps!=null)for(int[] map:maps)nativeModel+=4L*map.length;
            if(!GpuNoise1960.workspaceFits(nativeModel+24L*source.length+24L*core))return;
            int[][] expected=new int[2][],confidence=new int[2][];long[] cpuNanos=new long[2];
            for(int trial=0;trial<2;trial++){
                if(cancellation.cancelled())return;expected[trial]=new int[source.length];confidence[trial]=cfCount==0?null:new int[cfCount];
                long start=System.nanoTime();if(!cpu(expected[trial],confidence[trial]))return;
                cpuNanos[trial]=System.nanoTime()-start;if(u[10]==3)cpuNanos[trial]/=Math.max(1,SpeedWorkers1935.maxWorkers());
                if(cancellation.cancelled())return;
            }
            int best=-1;long bestGpu=Long.MAX_VALUE,bestCpu=Math.min(cpuNanos[0],cpuNanos[1]);
            for(int choice=0;choice<3;choice++)if(supported[choice]){
                boolean wins=true;long worst=0;
                for(int trial=0;trial<2;trial++){
                    if(cancellation.cancelled())return;long start=System.nanoTime();
                    Result actual=ephemeral(source,bins,maps,policy,u,descriptor,choice);long gpu=System.nanoTime()-start;
                    if(cancellation.cancelled())return;if(actual==UNAVAILABLE)return;
                    if(actual==null||actual.pixels.length!=core){wins=false;break;}
                    for(int i=0;i<core;i++)if(actual.pixels[i]!=expected[trial][u[2]*u[0]+i]){wins=false;break;}
                    if(wins&&cfCount!=0&&!Arrays.equals(actual.confidence,confidence[trial]))wins=false;
                    if(gpu<=0||cpuNanos[trial]<=0||gpu>cpuNanos[trial]-cpuNanos[trial]/20)wins=false;
                    worst=Math.max(worst,gpu);if(!wins)break;
                }
                if(wins&&worst<=bestCpu-bestCpu/20&&worst<bestGpu){best=choice;bestGpu=worst;}
            }
            if(cancellation.cancelled())return;
            if(best<0){fail(key);return;}
            GpuQualification1961.qualified(key,bestCpu,bestGpu,best);
            if(!cancellation.cancelled())restore(key);
        }
        public void close(){source=null;policy=null;u=null;evidence=null;model=null;descriptor=null;}
    }
    private static final Result UNAVAILABLE=new Result(null,null);
    private static Result ephemeral(int[] source,float[] evidence,int[][] maps,int[] policy,
            int[] u,GpuPolicy1960.PolicyData gpuPolicy,int variant){
        long additional=8L*source.length+24L*u[0]*(u[3]-u[2])+4L*evidence.length+1048576L;
        if(maps!=null)for(int[] map:maps)additional+=4L*map.length;
        if(gpuPolicy!=null)additional+=16L*gpuPolicy.count+4L*gpuPolicy.grid.length;
        if(!GpuNoise1960.workspaceFits(additional))return UNAVAILABLE;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return UNAVAILABLE;
        try{
            if(!session.upload(2,evidence))return null;
            for(int k=0;k<3;k++)if(!session.upload(4+k,maps==null?new int[]{0}:maps[k]))return null;
            return execute(session,source,policy,u,gpuPolicy,variant);
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){return null;}catch(LinkageError unavailable){return null;}catch(OutOfMemoryError unavailable){return null;}
        finally{session.close();}
    }
    private static GpuNoise1960.Batch commands(int[] source,int[] policy,int[] u,
            GpuPolicy1960.PolicyData gpuPolicy,int[] bank,int variant){
        int count=u[0]*(u[3]-u[2]);int confidenceCount=u[12]!=0?((u[0]+3)/4)*((u[3]-u[2]+3)/4):1;
        GpuNoise1960.Batch batch=new GpuNoise1960.Batch();
        batch.upload(bank[0],source).allocate(bank[1],4L*count).allocate(bank[3],4L*confidenceCount).upload(bank[9],new int[]{0});
        if(gpuPolicy==null)batch.upload(bank[2],policy==null?new int[]{0}:policy);
        else{
            // GX17: exact integer policy comparison remains entirely on GPU.
            // Only its one-word failure flag accompanies the final pixels.
            batch.upload(bank[4],gpuPolicy.masks).upload(bank[5],gpuPolicy.grid)
                .allocate(bank[2],8L*count).allocate(bank[6],4L*count).allocate(bank[7],4)
                .dispatch(GpuNoise1960.ANALYSIS,new int[]{bank[7],bank[7],bank[6],bank[2],bank[5],bank[4]},gpuPolicy.u,gpuPolicy.f,count)
                .upload(bank[8],policy);
            int[] compare=new int[32];compare[0]=count*2;
            batch.dispatch(GpuNoise1960.COMPARE1961,new int[]{bank[8],bank[2],bank[9]},compare,null,count*2);
        }
        return batch.dispatch(GpuNoise1960.variant(GpuNoise1960.STRONG,variant),
            new int[]{bank[0],bank[1],2,bank[2],4,5,6,bank[3]},u,null,count);
    }
    private static int[] readSlots(int[] u,int[] bank){return u[12]!=0?new int[]{bank[1],bank[3],bank[9]}:new int[]{bank[1],bank[9]};}
    private static int[] readCounts(int[] u){int count=u[0]*(u[3]-u[2]);return u[12]!=0?new int[]{count,((u[0]+3)/4)*((u[3]-u[2]+3)/4),1}:new int[]{count,1};}
    private static Result result(int[][] values,int[] u){
        if(values==null||values.length!=(u[12]!=0?3:2)||values[values.length-1]==null||values[values.length-1].length!=1||values[values.length-1][0]!=0)return null;
        return new Result(values[0],u[12]!=0?values[1]:null);
    }
    private static Result collect(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int[] u,int[] bank){
        try{return result(session.collect(ticket,readSlots(u,bank),readCounts(u)),u);}
        catch(RuntimeException unavailable){return null;}catch(LinkageError unavailable){return null;}catch(OutOfMemoryError unavailable){return null;}
    }
    private static Result execute(GpuNoise1960.Session session,int[] source,int[] policy,
            int[] u,GpuPolicy1960.PolicyData gpuPolicy,int variant){
        try{
            return result(session.execute(commands(source,policy,u,gpuPolicy,BANKS[0],variant),readSlots(u,BANKS[0]),readCounts(u)),u);
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){return null;}catch(LinkageError unavailable){return null;}catch(OutOfMemoryError unavailable){return null;}
    }
}
