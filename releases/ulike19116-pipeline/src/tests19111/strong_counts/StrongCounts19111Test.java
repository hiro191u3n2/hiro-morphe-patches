package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
/** Executes the real production dispatcher with controlled transport/evidence.
 * No reflected assignment invents any GPU or CPU selection counter. */
public final class StrongCounts19111Test {
    static int assertions,cases;
    static void check(boolean v,String why){assertions++;if(!v)throw new AssertionError(why);}
    static void start(boolean benchmark,boolean oracle)throws Exception{
        StrongDefaultRouting1990Test.reset();GpuResident1976.benchmark=benchmark;GpuResident1976.oracle=oracle;
        ResidentTiming19111.calls=ResidentTiming19111.fault=0;ResidentTiming19111.key=null;ResidentTiming19111.values=null;cases++;
    }
    static Object stage()throws Exception{return StrongDefaultRouting1990Test.active();}
    static int count(Object stage,String name)throws Exception{return StrongDefaultRouting1990Test.field(stage.getClass(),name).getInt(stage);}
    static void untouched(){check(ProcessingTiming1947.gpu==0&&ProcessingTiming1947.cpu==0&&ProcessingTiming1947.workCalls==0&&ProcessingTiming1947.lockCalls==0,"benchmark endStage does not publish foreground trace counts/work");}
    static void drained(){check(GpuNoise1960.leases==0&&GpuNoise1960.active==0,"original GPU ownership drains completely");}
    static void mixed()throws Exception{
        start(true,false);StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);Object saved=stage();
        check(d.process(d.route())&&d.process(d.route()),"two actual GPU selections complete");GpuQualification1961.rejectExact1971(d.key(4),"controlled argb rejection");check(d.process(d.route()),"real revoked-certificate CPU fallback completes");
        check(count(saved,"gpuStrips")==2&&count(saved,"cpuStrips")==1&&d.calls==1,"production commit counters reflect actual chosen routes");check(ResidentTiming19111.calls==0,"workers do not publish partial counts");
        GpuStrong1960.endStage(d.model);d.exact();check(ResidentTiming19111.calls==1&&"strongCounts".equals(ResidentTiming19111.key)&&Arrays.equals(ResidentTiming19111.values,new long[]{2,1}),"one final bounded snapshot contains actual selection counts");
        check(count(saved,"gpuStrips")==2&&count(saved,"cpuStrips")==1,"hostile sink cannot mutate stage counters");untouched();drained();check(stage()==null,"active stage ownership retired before return");
        GpuStrong1960.endStage(d.model);check(ResidentTiming19111.calls==1,"repeated end does not double count");
    }
    static void cpuOnlyAndEmpty()throws Exception{
        for(int mode=0;mode<2;mode++){
            start(true,false);GpuStrongDefault1990.ready=false;StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);
            if(mode==0)check(d.process(d.route())&&d.calls==1,"CPU-only stage completes without opening GPU");
            GpuStrong1960.endStage(d.model);check(Arrays.equals(ResidentTiming19111.values,new long[]{0,mode==0?1:0}),"no-session finally still reports observed zero and CPU counts");check(ResidentTiming19111.calls==1,"no-session stage reports once");untouched();drained();
        }
    }
    static void contexts()throws Exception{
        start(false,false);StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);check(d.process(d.route()),"foreground selects original GPU route");GpuStrong1960.endStage(d.model);d.exact();check(ResidentTiming19111.calls==0&&ProcessingTiming1947.gpu==1&&ProcessingTiming1947.cpu==0,"foreground remains exclusively in its established photo counters");drained();
        for(boolean benchmark:new boolean[]{false,true}){
            start(benchmark,true);d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);check(d.process(d.route())&&d.calls==1,"CPU oracle executes its real CPU route");GpuStrong1960.endStage(d.model);d.exact();check(ResidentTiming19111.calls==0,"quality oracle cannot populate timing trial counters");untouched();drained();
        }
    }
    static void failures()throws Exception{
        for(int fault=1;fault<=4;fault++){
            start(true,false);StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);check(d.process(d.route()),"GPU selection precedes optional failure");ResidentTiming19111.fault=fault;GpuStrong1960.endStage(d.model);d.exact();check(ResidentTiming19111.calls==1&&Arrays.equals(ResidentTiming19111.values,new long[]{1,0}),"optional observer failure cannot change counts or end outcome");untouched();drained();
        }
        start(true,false);StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);Object saved=stage();check(d.process(d.route()),"one complete GPU strip precedes cancellation");Thread.currentThread().interrupt();try{check(!d.process(d.route()),"interrupted strip cannot report a completed commit");}catch(CancellationException expected){check(true,"cancellation preserves exception path");}finally{Thread.interrupted();}
        GpuStrong1960.endStage(d.model);check(Arrays.equals(ResidentTiming19111.values,new long[]{1,0})&&count(saved,"gpuStrips")==1,"cancelled unfinished work cannot inflate observed counts");untouched();drained();
    }
    public static void main(String[] args)throws Exception{
        mixed();cpuOnlyAndEmpty();contexts();failures();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"production_dispatcher_executed\":true,\"actual_commit_counts\":true,\"foreground_trace_isolated\":true,\"cpu_oracle_excluded\":true,\"optional_sink_failures_preserved\":true,\"ownership_preserved\":true,\"actual_jni\":false,\"physical_android_tested\":false}");
    }
}
