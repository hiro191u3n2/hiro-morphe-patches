package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.Arrays;

/** Assertions read actual capture-owned scalar storage; no production oracle replacement. */
final class ReasonsAssertions1989 {
    static long assertions,pixels;
    static final boolean FAULT=Boolean.getBoolean("ulike.reasons.fault1989");
    static void check(boolean okay,String why){assertions++;if(!okay)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static int optionalInt(Object owner,String name)throws Exception{
        try{return field(owner.getClass(),name).getInt(owner);}catch(NoSuchFieldException old88){return 0;}
    }
    static ProcessingTiming1947.Trace trace(){return new ProcessingTiming1947.Trace(1989,System.currentTimeMillis(),System.nanoTime());}
    static void exact(int[] expected,int[] actual,String name){
        check(expected.length==actual.length,name+" length");
        for(int i=0;i<expected.length;i++){check(expected[i]==actual[i],name+" ARGB "+i);pixels++;}
    }
    static long count(ProcessingTiming1947.Trace t,int phase,int backend){return t.pipelineWork1988[(phase*3+backend+1)*3];}
    static long reason(ProcessingTiming1947.Trace t,int phase,int why){return t.pipelineReasons1988[phase*20+why];}
    static void selected(ProcessingTiming1947.Trace t,int phase,int backend,long units,int why){
        check(count(t,phase,backend)==(FAULT?0:units),"adopted backend count preserved "+phase);
        for(int b=-1;b<=1;b++)if(b!=backend)check(count(t,phase,b)==0,"no invented adopted backend "+phase);
        if(!FAULT&&backend==0)check(reason(t,phase,why)==units,"REASON1989_MISSING: actual CPU branch "+phase+"/"+why);
    }
    static void untouched(ProcessingTiming1947.Trace t,String why){
        check(!t.pipelineObserved1988,why+" observation stays absent");
        for(long[] a:new long[][]{t.pipelineWork1988,t.pipelineReasons1988,t.pipelineCopies1988,
                t.pipelinePriorReasons1989,t.pipelineCopyQuantities1989,t.pipelineQueueCounts1989,t.pipelineQueueLast1989})
            for(long v:a)check(v==0,why+" retains no scalar from another operation");
    }
    static int copyAt(int phase,int outcome){return (phase*11+outcome)*4;}
    static void copy(ProcessingTiming1947.Trace t,int phase,int outcome,long requested,long acquired){
        int at=copyAt(phase,outcome);long[] q=t.pipelineCopyQuantities1989;
        check(t.pipelineCopies1988[at]==(FAULT?0:1),"legacy snapshot event count unchanged");
        check(q[at]==(FAULT?0:1),"REASON1989_MISSING: requested/acquired snapshot observation");
        if(!FAULT){
            check(q[at+1]==Math.max(0,requested)&&q[at+2]==Math.max(0,acquired),"exact requested closure and acquired pixel quantities");
            check((q[at+3]&3)==(requested<0?2:1),"requested bytes known independently");
            check((q[at+3]&12)==(acquired<0?8:4),"acquired pixel bytes known independently");
        }
    }
    static void report(String mode){
        if(FAULT)try{check(field(PipelineDetail1988.class,"faults1989").getLong(null)>0,"optional facade fault was actually exercised");}
        catch(Exception failure){throw new AssertionError("fault injector unavailable",failure);}
        System.out.println("{\"status\":\"passed\",\"mode\":\""+mode+"\",\"assertions\":"+assertions+
            ",\"pixels\":"+pixels+",\"optional_failure\":"+FAULT+"}");
    }
}
