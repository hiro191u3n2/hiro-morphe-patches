package com.hiro.ulike;
/** Same production dispatcher runs when an optional collector is absent/incompatible. */
public final class StrongCountsMissing19111Test {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        StrongDefaultRouting1990Test.reset();GpuResident1976.benchmark=true;GpuResident1976.oracle=false;
        StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);check(d.process(d.route()),"real GPU selection completes with missing optional sink");GpuStrong1960.endStage(d.model);d.exact();
        check(GpuNoise1960.active==0&&GpuNoise1960.leases==0,"missing/incompatible sink cannot interrupt owner release");
        check(ProcessingTiming1947.gpu==0&&ProcessingTiming1947.cpu==0&&ProcessingTiming1947.workCalls==0,"missing sink cannot redirect counters to foreground photo trace");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"missing_optional_sink_preserved\":true}");
    }
}
