package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.Arrays;

/** Test assertions inspect the real capture-owned bounded scalar storage. */
final class DetailCalls1988 {
    static long assertions;
    static final boolean OPTIONAL_FAILURE=Boolean.getBoolean("ulike.detail.failure1988");
    static void check(boolean value,String name){assertions++;if(!value)throw new AssertionError(name);}
    static ProcessingTiming1947.Trace trace(){return new ProcessingTiming1947.Trace(1988,System.currentTimeMillis(),System.nanoTime());}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static long count(ProcessingTiming1947.Trace trace,int phase,int backend){return trace.pipelineWork1988[(phase*3+backend+1)*3];}
    static long reason(ProcessingTiming1947.Trace trace,int phase,int reason){return trace.pipelineReasons1988[phase*12+reason];}
    static int copyAt(int phase,int outcome){return (phase*11+outcome)*4;}
    static long copies(ProcessingTiming1947.Trace trace,int phase,int outcome){return trace.pipelineCopies1988[copyAt(phase,outcome)];}
    static void selected(ProcessingTiming1947.Trace trace,int phase,int backend,long units,int why){
        check(count(trace,phase,backend)==(OPTIONAL_FAILURE?0:units),"PIPELINE_DETAIL_MISSING: selected phase "+phase+" backend "+backend);
        for(int other=-1;other<=1;other++)if(other!=backend)check(count(trace,phase,other)==0,"unselected backend not counted "+phase);
        if(!OPTIONAL_FAILURE){
            int at=(phase*3+backend+1)*3;
            check(trace.pipelineWork1988[at+1]>=0&&(trace.pipelineWork1988[at+2]&1)!=0,"actual phase time measured "+phase);
            if(backend==0)check(reason(trace,phase,why)==units,"only observed CPU reason retained "+phase);
        }
    }
    static void copied(ProcessingTiming1947.Trace trace,int phase,int outcome,boolean bytesKnown,boolean timed){
        int at=copyAt(phase,outcome);
        check(copies(trace,phase,outcome)==(OPTIONAL_FAILURE?0:1),"PIPELINE_DETAIL_MISSING: copy phase/outcome "+phase+"/"+outcome);
        if(!OPTIONAL_FAILURE){
            long flags=trace.pipelineCopies1988[at+3];
            check((flags&(bytesKnown?4:8))!=0&&(flags&(bytesKnown?8:4))==0,"copy known/unknown bytes preserved");
            check((flags&(timed?1:2))!=0&&(flags&(timed?2:1))==0,"copy measured/unmeasured time preserved");
            if(bytesKnown)check(trace.pipelineCopies1988[at+2]>=0,"observed copy bytes nonnegative");
        }
    }
    static void untouched(ProcessingTiming1947.Trace trace,String label){
        check(!trace.pipelineObserved1988,label+" never receives another owner's optional telemetry");
        for(long[] a:new long[][]{trace.pipelineWork1988,trace.pipelineReasons1988,trace.pipelineCopies1988})
            for(long v:a)check(v==0,label+" all scalar fields unchanged");
    }
    static void equal(int[] expected,int[] actual,String name){check(Arrays.equals(expected,actual),name+" all ARGB values exact");}
    static void report(String group)throws Exception{
        if(OPTIONAL_FAILURE){long calls=field(PipelineDetail1988.class,"faults1988").getLong(null);
            check(group.equals("front-background")?calls==0:calls>0,group.equals("front-background")?
                "existing foreground gate excludes diagnostics before facade invocation":"optional facade failure actually reached from production route");}
        System.out.println("{\"status\":\"passed\",\"group\":\""+group+"\",\"assertions\":"+assertions+",\"optional_failure\":"+OPTIONAL_FAILURE+"}");
    }
}
