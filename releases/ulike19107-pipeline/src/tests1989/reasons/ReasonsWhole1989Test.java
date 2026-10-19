package com.hiro.ulike;

import java.lang.reflect.Method;

/** Actual WholeRoute1953 engine with scalar clocks and controlled work ownership. */
public final class ReasonsWhole1989Test {
    static void check(boolean v,String s){ReasonsAssertions1989.check(v,s);}
    static final class Clock extends WholeRoute1953.Clock {long ticks;long now(){return ticks;}}
    static final class History implements WholeRoute1953.History {
        String environment="whole-reasons89";long baseline;int calls,restores,rejects;
        public String environment(){calls++;return environment;}
        public long restore(int[] k,String e){restores++;return baseline;}
        public void qualified(int[] k,String e,long b){}
        public void rejected(int[] k,String e){rejects++;}
    }
    static final class Work implements WholeRoute1953.Work {
        final String mode;final Clock clock;int reason,cpuCalls,gpuCalls,reasonCalls,discardCalls,published,probes,probeDiscards;
        final RuntimeException original=new IllegalStateException("original CPU sentinel");
        Work(String mode,Clock clock){this.mode=mode;this.clock=clock;}
        public void cpuReason1989(int value){reasonCalls++;clock.ticks+=1000000L;if(mode.equals("optional"))throw new AssertionError("optional scalar callback failure");reason=value;}
        public boolean cpu(){cpuCalls++;clock.ticks+=1000;if(mode.equals("cpu-throws"))throw original;return true;}
        public boolean gpu(){gpuCalls++;clock.ticks+=100;
            if(mode.equals("gpu-runtime"))throw new IllegalStateException("candidate failure");
            if(mode.equals("gpu-link"))throw new UnsatisfiedLinkError("candidate linkage");
            if(mode.equals("gpu-memory"))throw new OutOfMemoryError("candidate allocation");
            return mode.equals("gpu")||mode.equals("publish");}
        public boolean equal(){return true;}
        public boolean dispatchSelection(){return mode.equals("dispatch");}
        public void publishGpu(){if(mode.equals("publish"))throw new IllegalStateException("candidate publication");published++;}
        public void discardGpu(){discardCalls++;}
        public long probeBytes(){return mode.equals("clock")?12345:0;}
        public WholeRoute1953.Probe snapshotProbe(){probes++;return new WholeRoute1953.Probe(){
            public boolean captureReference(){return true;}public long bytes(){return 12345;}
            public boolean gpu(WholeRoute1953.Cancellation c){throw new AssertionError("background GPU was not authorized by save");}
            public boolean equal(WholeRoute1953.Cancellation c){return true;}public boolean idle(){return false;}
            public void discard(){probeDiscards++;}
        };}
    }
    public static void main(String[] args)throws Exception{
        String mode=args[0];Clock clock=new Clock();History history=new History();
        if(mode.startsWith("gpu")||mode.equals("publish")||mode.equals("dispatch"))history.baseline=1000000000L;
        if(mode.equals("environment"))history.environment=null;
        WholeRoute1953.Engine engine=new WholeRoute1953.Engine(clock,history);Work work=new Work(mode,clock);int[] key={1989,1,2};
        int why=mode.equals("environment")?16:mode.equals("dispatch")?17:mode.equals("gpu-null")?8:
            mode.equals("gpu-runtime")||mode.equals("gpu-link")||mode.equals("publish")?9:mode.equals("gpu-memory")?7:
            mode.equals("unavailable")?6:mode.equals("busy")?14:mode.equals("gpu")?0:2;
        if(mode.equals("unavailable"))ReasonsAssertions1989.field(engine.getClass(),"unavailable").setBoolean(engine,true);
        if(mode.equals("busy")){
            Method claim=engine.getClass().getDeclaredMethod("claim",int[].class,String.class);claim.setAccessible(true);
            check(claim.invoke(engine,key,history.environment)!=null,"test owns actual route state");history.calls=history.restores=0;
        }
        Throwable failure=null;boolean completed=false;Object owner=new Object();
        try{completed=engine.run(key,work,owner);}catch(Throwable e){failure=e;}
        if(mode.equals("cpu-throws"))check(failure==work.original&&work.cpuCalls==1,"original CPU exception is neither wrapped nor retried");
        else check(failure==null&&completed,"actual engine completes unchanged output route");
        check(work.cpuCalls==(mode.equals("gpu")?0:1),"exact original CPU invocation count");
        check(work.gpuCalls==((mode.startsWith("gpu")||mode.equals("publish"))?1:0),"no invented GPU attempt");
        check(work.published==(mode.equals("gpu")?1:0),"publication only on accepted candidate");
        if(mode.equals("optional"))check(work.reasonCalls==2&&work.cpuCalls==1,"optional reason callback exception isolated");
        else check(work.reason==why,"REASON1989_MISSING: WholeRoute real CPU branch "+mode+" expected "+why+" actual "+work.reason);
        check(history.calls==(mode.equals("unavailable")?0:1),"no after-the-fact environment lookup");
        if(mode.equals("clock")){
            Object pending=ReasonsAssertions1989.field(engine.getClass(),"pending").get(engine);
            check(pending!=null&&ReasonsAssertions1989.field(pending.getClass(),"cpuNanos").getLong(pending)==1000,
                "CPU oracle baseline excludes optional callback duration");
            check(work.probes==1&&engine.retainedBytes==12345,"same real snapshot retention");
            engine.foregroundStarted();check(work.probeDiscards==1&&engine.retainedBytes==0,"cancellation releases pending exactly once");
        }
        ReasonsAssertions1989.report("whole-"+mode);System.exit(0);
    }
}
