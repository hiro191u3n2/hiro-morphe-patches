package com.hiro.ulike;

import android.content.Context;
import android.graphics.Bitmap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Real stage/queue/timing/pool owners with an explicitly controlled GPU and
 * CPU timing source. This tests admission and ownership, never device speed. */
public final class FrontProof19100Test {
    static final long M=1024L*1024;
    static int assertions,created,closed,cpu,gpu;
    static SingleNoise1955.Model model;
    static final String ORIGINAL="front19100-original",REFERENCE="front19100-reference";
    static ProcessingTiming1947.Trace trace;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static long count(int state){synchronized(trace){return trace.frontProofCounts19100[state];}}
    interface Check {boolean okay()throws Exception;}
    static void until(Check check,String why)throws Exception{
        long limit=System.nanoTime()+4000000000L;
        while(!check.okay()){if(System.nanoTime()>limit)throw new AssertionError("timeout: "+why);Thread.sleep(2);}
        check(true,why);
    }
    static int reservations()throws Exception{return ((Collection<?>)field(GpuQualification1961.class,"RESERVED1984").get(null)).size();}
    static void nextCapture()throws Exception{
        SaveQueue1935.idle=false;GpuQualification1961.captureChanged();
        Field epoch=field(ProcessingTiming1947.class,"captureEpoch1953");epoch.setLong(null,epoch.getLong(null)+1);
    }
    static void drain()throws Exception{
        SaveQueue1935.idle=true;field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);
        GpuQualification1961.wake();until(()->GpuQualification1961.retainedBytes()==0,"deferred owner released");
        SaveQueue1935.idle=false;
    }
    static void initialize()throws Exception{
        GpuNoise1960.enabled=true;GpuNoise1960.single=false;GpuNoise1960.residual=true;GpuNoise1960.fault=1;
        GpuQualification1961.initialize(new Context());field(SingleNoise1955.class,"nativeState").setInt(null,-1);
        final int w=33,h=97;final int[] input=new int[w*h];Random r=new Random(19100);
        for(int i=0;i<input.length;i++){int v=87+r.nextInt(51);input[i]=0xff000000|v<<16|v<<8|v;}
        model=SingleNoise1955.probe1978(new SingleNoise1955.Patches(){public void read(int[] out,int x,int y,int a,int b){
            for(int i=0;i<b;i++)System.arraycopy(input,(y+i)*w+x,out,i*a,a);
        }},w,h,false);
        trace=new ProcessingTiming1947.Trace(19100,System.currentTimeMillis(),System.nanoTime());
        nextCapture();
    }
    static SingleStage1981.Factory factory(final String mode){return new SingleStage1981.Factory(){
        public SingleStage1981.Operation create(){
            created++;
            try{check(reservations()==1,"capacity owned before first snapshot allocation");}
            catch(Exception failure){throw new AssertionError(failure);}
            if(mode.equals("exception"))throw new IllegalStateException("controlled snapshot exception");
            if(mode.equals("linkage"))throw new UnsatisfiedLinkError("controlled snapshot ABI failure");
            if(mode.equals("allocation"))throw new OutOfMemoryError("controlled snapshot allocation");
            if(mode.equals("none"))return null;
            if(mode.equals("cancel"))GpuQualification1961.captureChanged();
            if(mode.equals("capture-change"))try{nextCapture();}catch(Exception failure){throw new AssertionError(failure);}
            if(mode.equals("reject-after"))GpuQualification1961.rejectExact(ORIGINAL);
            if(mode.equals("commit-qualified"))GpuQualification1961.qualified(SingleStage1981.key(REFERENCE),1000000000L,1,1);
            return new SingleStage1981.Operation(){
                public String referenceKey(){return mode.equals("reference-change")?"other-strip":REFERENCE;}
                public SingleStage1981.CpuSample cpu(int pass,GpuQualification1961.Cancellation cancellation){
                    cpu++;check(GpuQualification1961.background(),"CPU oracle runs only in deferred worker");
                    return new SingleStage1981.CpuSample(new int[]{pass,19100},mode.equals("slow")?1:1000000000L);
                }
                public int[] run(SingleStage1981.Lease lease,int pass){
                    gpu++;check(GpuQualification1961.background(),"GPU candidate runs only in deferred worker");
                    lease.complete();return new int[]{pass,mode.equals("mismatch")?19101:19100};
                }
                public long preparationNanos(){return 0;}
                public void close(){closed++;}
            };
        }
    };}
    static void offer(String mode,long retained,long snapshot,long execution){
        ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(trace);
        try{SingleStage1981.offer19100(ORIGINAL,REFERENCE,model,SingleStage1981.RESIDUAL,retained,snapshot,execution,factory(mode));}
        finally{ProcessingTiming1947.restore(scope);}
    }
    static void memory(String mode)throws Exception{
        GpuNoise1960.ceiling19100=4*M;
        if(mode.equals("copy-low")){
            offer("good",M,8*M,64*M);
            check(created==0&&GpuQualification1961.retainedBytes()==0&&reservations()==0,"failed copy budget owns no arrays or capacity");
            check(count(SingleStage1981.SNAPSHOT_MEMORY19100)==1,"copy refusal explicitly observed");
            nextCapture();GpuNoise1960.ceiling19100=16*M;
        }
        offer("good",M,2*M,64*M);
        check(created==1&&cpu==0&&gpu==0&&closed==0,"low foreground budget captures only immutable input");
        check(GpuQualification1961.retainedBytes()==M&&reservations()==0,"exact retained ownership transfers into queue");
        check(count(SingleStage1981.QUEUED19100)==1,"queue success recorded on owner");
        check(!CpuExact1978.enabled(SingleStage1981.key(REFERENCE)),"snapshot alone never certifies GPU");
        if(!mode.equals("execute-low"))GpuNoise1960.ceiling19100=128*M;
        drain();check(closed==1&&GpuNoise1960.active==0,"deferred close releases snapshot/session once");
        if(mode.equals("execute-low")){
            check(cpu==0&&gpu==0,"execution budget rechecked before either oracle allocates");
            check(!CpuExact1978.enabled(SingleStage1981.key(REFERENCE)),"memory refusal cannot certify candidate");
            check(GpuQualification1961.attemptSummary1985().contains("検証用メモリ不足"),"deferred memory refusal remains visible");
        }else{
            check(cpu==6&&gpu==6,"warmup plus two measured trials both compare two inputs");
            check(CpuExact1978.enabled(SingleStage1981.key(REFERENCE)),"exact faster private stage can qualify after saving");
            check(!CpuExact1978.enabled(SingleStage1981.key("other-strip"))&&GpuQualification1961.restore(ORIGINAL)==null,
                "one strip proof is never reused as another strip or original GPU certificate");
        }
        check(trace.frontProofLast19100[3]==M&&trace.frontProofLast19100[4]==2*M&&trace.frontProofLast19100[5]==64*M,
            "copy and execution peaks recorded independently");
    }
    static void terminal(String mode)throws Exception{
        offer(mode,M,2*M,4*M);
        int state=mode.equals("none")?SingleStage1981.NO_SNAPSHOT19100:mode.equals("exception")?SingleStage1981.EXCEPTION19100:
            mode.equals("linkage")?SingleStage1981.LINKAGE19100:mode.equals("allocation")?SingleStage1981.ALLOCATION19100:
            mode.equals("capture-change")?SingleStage1981.STALE19100:mode.equals("reject-after")?SingleStage1981.REJECTED_AFTER_COPY19100:
            mode.equals("reference-change")?SingleStage1981.REFERENCE_CHANGED19100:SingleStage1981.COMMIT_DECLINED19100;
        boolean before=mode.equals("none")||mode.equals("exception")||mode.equals("linkage")||mode.equals("allocation");
        check(created==1&&closed==(before?0:1),"snapshot result disposed exactly once when available");
        check(reservations()==0&&GpuQualification1961.retainedBytes()==0,"all refusal paths release reservations");
        check(count(state)==1,"early terminal reason observed: "+mode);
        check(cpu==0&&gpu==0,"foreground failure cannot execute candidate");
    }
    static void guards(String mode)throws Exception{
        if(mode.equals("original-reject"))GpuQualification1961.rejectExact(ORIGINAL);
        if(mode.equals("qualified"))GpuQualification1961.qualified(SingleStage1981.key(REFERENCE),1000000000L,1,1);
        if(mode.equals("interrupted"))Thread.currentThread().interrupt();
        try{offer("good",mode.equals("retained-limit")?97*M:M,mode.equals("snapshot-limit")?513*M:
                mode.equals("retained-limit")?98*M:2*M,mode.equals("execution-limit")?513*M:4*M);}
        finally{Thread.interrupted();}
        int state=mode.equals("original-reject")?SingleStage1981.ORIGINAL_REJECTED19100:mode.equals("qualified")?
            SingleStage1981.QUALIFIED19100:mode.equals("interrupted")?SingleStage1981.INTERRUPTED19100:SingleStage1981.INVALID19100;
        check(created==0&&GpuQualification1961.retainedBytes()==0&&reservations()==0,"guard refuses before allocation");
        check(count(state)==1,"preflight guard reason observed");
    }
    static void copyReason(String mode)throws Exception{
        GpuSnapshotBudget1981.Copy held=null;
        int expected;
        long retained=M;
        if(mode.equals("copy-turn")){
            GpuSnapshotBudget1981.offerStrip1981(GpuSnapshotBudget1981.STRONG_TUNING);
            held=GpuSnapshotBudget1981.tryCopy1985(M,GpuSnapshotBudget1981.STRONG_TUNING).copy;
            check(held!=null&&held.begin(),"previous strong turn allocated");held.close();held=null;nextCapture();
            expected=GpuSnapshotBudget1981.Decision1985.STRIP_TURN;
        }else{
            GpuSnapshotBudget1981.offerStrip1981(GpuSnapshotBudget1981.RESIDUAL_STAGE);
            held=GpuSnapshotBudget1981.tryCopy1985(mode.equals("copy-budget")?20*M:M,GpuSnapshotBudget1981.RESIDUAL_STAGE).copy;
            check(held!=null,"controlled prior copy ticket");
            if(mode.equals("copy-reserved"))expected=GpuSnapshotBudget1981.Decision1985.COPY_RESERVED;
            else{check(held.begin(),"controlled prior copy started");expected=GpuSnapshotBudget1981.Decision1985.COPY_ACTIVE;}
            if(mode.equals("copy-budget")){held.close();held=null;retained=20*M;expected=GpuSnapshotBudget1981.Decision1985.STRIP_BUDGET;}
        }
        try{offer("good",retained,retained+M,64*M);}finally{if(held!=null)held.close();}
        check(created==0&&GpuQualification1961.retainedBytes()==0,"copy refusal does not reserve queue or clone");
        check(trace.frontCopyCounts19100[expected]==1&&count(SingleStage1981.COPY_DECLINED19100)==1,
            "same copy decision distinguishes turn/budget/active/reserved");
    }
    static void gates(String mode)throws Exception{
        offer(mode,M,2*M,4*M);drain();
        check(closed==1&&!CpuExact1978.enabled(SingleStage1981.key(REFERENCE)),"inexact or slow GPU never admitted");
        check(mode.equals("mismatch")?GpuQualification1961.exactRejected(SingleStage1981.key(REFERENCE)):
            GpuQualification1961.attemptSummary1985().contains("速度条件を満たさず"),"unchanged exactness/five-percent speed gates");
    }
    static Runnable worker(Bitmap bitmap)throws Exception{
        Class<?> strips=Class.forName("com.hiro.ulike.QualityPipeline1932$Strips");
        Constructor<?> make=strips.getDeclaredConstructor(Bitmap.class,int.class,int.class);make.setAccessible(true);
        Object values=make.newInstance(bitmap,16,SingleNoise1955.HALO);
        Class<?> type=Class.forName("com.hiro.ulike.QualityPipeline1932$Worker");
        Constructor<?> create=type.getDeclaredConstructor(strips,QualityPixels1932.Plan.class,SingleNoise1955.Model.class,int.class,boolean.class);
        create.setAccessible(true);return (Runnable)create.newInstance(values,null,model,4,true);
    }
    static void workerTrace()throws Exception{
        final ProcessingTiming1947.Trace wrong=new ProcessingTiming1947.Trace(999,System.currentTimeMillis(),System.nanoTime());
        final int[] pixels=new int[33*97];Random r=new Random(19100);for(int i=0;i<pixels.length;i++){int v=87+r.nextInt(51);pixels[i]=0xff000000|v<<16|v<<8|v;}
        Bitmap image=Bitmap.from(33,97,pixels,Bitmap.Config.ARGB_8888,true);
        ProcessingTiming1947.bind(image,trace);final Runnable actual=worker(image);
        final AtomicInteger restored=new AtomicInteger();
        SpeedWorkers1935.run(new Runnable[]{new Runnable(){public void run(){
            ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(wrong);
            try{actual.run();if(ProcessingTiming1947.current()==wrong)restored.incrementAndGet();}
            finally{ProcessingTiming1947.restore(scope);}
        }}});
        check(restored.get()==1,"actual pool worker restores previous lexical trace");
        check(count(SingleStage1981.OFFER19100)>0&&count(SingleStage1981.QUEUED19100)>0,"actual Worker propagates source owner into nested stage collector: "+Arrays.toString(trace.frontProofCounts19100)+" sigma="+model.meanSigma()+" chroma="+model.meanChromaSigma()+" owner="+field(actual.getClass(),"singleTrace1982").get(actual)+" failure="+field(field(actual.getClass(),"strips").get(actual).getClass(),"failure").get(field(actual.getClass(),"strips").get(actual)));
        check(wrong.frontProofCounts19100[SingleStage1981.OFFER19100]==0,"unrelated pool scope receives no source decisions");
        nextCapture();
        Bitmap unknown=Bitmap.from(33,97,pixels,Bitmap.Config.ARGB_8888,true);final Runnable unowned=worker(unknown);
        SpeedWorkers1935.run(new Runnable[]{new Runnable(){public void run(){
            ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(wrong);
            try{unowned.run();check(ProcessingTiming1947.current()==wrong,"unknown owner restores scope");}
            finally{ProcessingTiming1947.restore(scope);}
        }}});
        check(wrong.frontProofCounts19100[SingleStage1981.OFFER19100]==0,"unknown bitmap clears unrelated inherited scope instead of borrowing it");
        nextCapture();
        Method renderer=ProcessingTiming1947.class.getDeclaredMethod("appendFrontProof19100",StringBuilder.class,ProcessingTiming1947.Trace.class);
        renderer.setAccessible(true);StringBuilder text=new StringBuilder();renderer.invoke(null,text,trace);
        check(text.toString().contains("撮影内コピーの追加ピーク")&&text.toString().contains("保存後の実行追加ピーク"),"rendered memory lives are explicitly separate");
        long before=count(SingleStage1981.OFFER19100);trace.state=1;
        ProcessingTiming1947.frontProof19100(trace,2,1,0,M,2*M,4*M);
        check(count(SingleStage1981.OFFER19100)==before,"completed trace immutable under late diagnostics");
    }
    public static void main(String[] args)throws Exception{
        initialize();String mode=args[0];
        if(mode.equals("memory")||mode.equals("copy-low")||mode.equals("execute-low"))memory(mode);
        else if(mode.startsWith("copy-"))copyReason(mode);
        else if(mode.equals("mismatch")||mode.equals("slow"))gates(mode);
        else if(mode.equals("worker-trace"))workerTrace();
        else if(mode.equals("original-reject")||mode.equals("qualified")||mode.equals("interrupted")||mode.endsWith("-limit"))guards(mode);
        else terminal(mode);
        nextCapture();check(reservations()==0&&GpuQualification1961.retainedBytes()==0&&GpuNoise1960.active==0,"final queue/session owners clean");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"scenario\":\""+mode+"\",\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
