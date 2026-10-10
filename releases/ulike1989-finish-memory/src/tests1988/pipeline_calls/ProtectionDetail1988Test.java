package com.hiro.ulike;

import java.lang.reflect.Method;

/** Uses the independently retained all-coordinate policy oracle and fixtures. */
public final class ProtectionDetail1988Test {
    static final int W=67,H=43,FIRST=3,LAST=37;
    static Object call(String name,Class<?>[] types,Object...args)throws Exception{
        Method m=ProtectionOpportunity1987Test.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);
    }
    static QualityPixels1932.Plan plan()throws Exception{return (QualityPixels1932.Plan)call("plan",new Class<?>[]{int.class},0);}
    static int[] oracle(QualityPixels1932.Plan p)throws Exception{return (int[])call("oracle",new Class<?>[]{QualityPixels1932.Plan.class},p);}
    static void exact(FinishPolicy1953.Band band,int[] expected)throws Exception{
        try{DetailCalls1988.check(band!=null,"actual finishing policy completes");
            int[] actual=(int[])call("expand",new Class<?>[]{FinishPolicy1953.Band.class},band);DetailCalls1988.equal(expected,actual,"all original protection policy integers");
        }finally{if(band!=null)band.close();}
        DetailCalls1988.check(SpeedWorkers1935.owned.isEmpty(),"all policy array leases released");
    }
    static void route(int mode)throws Exception{
        ProtectionPeers1987.reset();QualityPixels1932.Plan p=plan();int[] expected=oracle(p);
        if(mode==1)GpuQualification1961.held=65536;
        if(mode==2||mode==3||mode==4){GpuQualification1961.record=new GpuQualification1961.Record(100000000000L,1,0);GpuNoise1960.output=expected;}
        if(mode==3)GpuNoise1960.executeOom=true;
        if(mode==4)GpuQualification1961.inBackground=true;
        ProcessingTiming1947.Trace owner=DetailCalls1988.trace();ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(owner);
        try{exact(FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0),expected);}finally{ProcessingTiming1947.restore(scope);}
        if(mode==0){
            DetailCalls1988.selected(owner,PipelineDetail1988.AUX_PROTECTION,0,1,PipelineDetail1988.PROOF_MISSING);
            DetailCalls1988.copied(owner,PipelineDetail1988.AUX_PROTECTION,PipelineDetail1988.CAPTURED,true,true);
            DetailCalls1988.copied(owner,PipelineDetail1988.AUX_PROTECTION,PipelineDetail1988.QUEUED,true,false);
            DetailCalls1988.check(GpuQualification1961.queued==1&&GpuNoise1960.executions==0,"non-null CPU reference never recorded as GPU");
        }else if(mode==1){
            DetailCalls1988.copied(owner,PipelineDetail1988.AUX_PROTECTION,PipelineDetail1988.LEGACY_UNUSED_PREP_SKIPPED,true,false);
            DetailCalls1988.check(owner.pipelineCopies1988[DetailCalls1988.copyAt(PipelineDetail1988.AUX_PROTECTION,PipelineDetail1988.LEGACY_UNUSED_PREP_SKIPPED)+2]==0,"observed skipped preparation copied exactly zero bytes");
            DetailCalls1988.check(DetailCalls1988.count(owner,PipelineDetail1988.AUX_PROTECTION,0)==0&&DetailCalls1988.count(owner,PipelineDetail1988.AUX_PROTECTION,1)==0,"declined optional preparation does not invent a selected result");
            DetailCalls1988.check(GpuQualification1961.queued==0&&GpuNoise1960.executions==0,"blocked proof keeps original .87 skip");
        }else if(mode==2){
            DetailCalls1988.selected(owner,PipelineDetail1988.AUX_PROTECTION,1,1,0);
            DetailCalls1988.check(GpuNoise1960.executions==1&&GpuQualification1961.queued==0,"actual accepted GPU result is counted once");
        }else if(mode==3){
            DetailCalls1988.check(GpuNoise1960.executions==1&&DetailCalls1988.count(owner,PipelineDetail1988.AUX_PROTECTION,1)==0,"failed GPU allocation cannot count a GPU output");
        }else DetailCalls1988.untouched(owner,"background protection work");
        if(GpuQualification1961.probe!=null){GpuQualification1961.probe.close();GpuQualification1961.probe=null;}
    }
    public static void main(String[] args)throws Exception{
        for(int mode=0;mode<5;mode++)route(mode);
        ProtectionPeers1987.reset();QualityPixels1932.Plan p=plan();Thread.currentThread().interrupt();Throwable actual=null;
        try{FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0);}catch(Throwable error){actual=error;}
        DetailCalls1988.check(actual instanceof IllegalStateException&&Thread.currentThread().isInterrupted(),"original protection cancellation survives optional diagnostics");Thread.interrupted();
        DetailCalls1988.check(GpuNoise1960.executions==0&&SpeedWorkers1935.owned.isEmpty(),"cancelled route owns no GPU result or leased policy");
        DetailCalls1988.report("protection");
    }
}
