package com.hiro.ulike;
import android.graphics.Bitmap;

/** Real foreground and deferred geometry branches; deterministic transport faults. */
public final class GeometryFailure1992Test {
    static int assertions;
    static void check(boolean pass,String why){assertions++;if(!pass)throw new AssertionError(why);}
    static final class Cpu implements GpuGeometry1960.CpuReason1989 {
        int calls,reason=-1;Bitmap output;
        public void cpuReason1989(int r){reason=r;}
        public Bitmap run(){calls++;return output=FastResize1933.output(4,3);}
    }
    static Bitmap reset(boolean admitted,int fault){
        GpuNoise1960.serial++;GpuNoise1960.fault=fault;GpuNoise1960.fits=true;GpuNoise1960.opened=GpuNoise1960.closed=0;
        GpuQualification1961.admitted=admitted;GpuQualification1961.cancelled=GpuQualification1961.exact=false;
        GpuQualification1961.record=new GpuQualification1961.Record(1000000000L,1000);
        GpuQualification1961.reason=GpuQualification1961.candidate=null;GpuQualification1961.pending=null;
        GpuQualification1961.measuredCpu=GpuQualification1961.measuredGpu=0;GpuQualification1961.qualified=0;
        return FastResize1933.output(4,4);
    }
    static void foreground(int fault,String expected,int reason)throws Exception{
        Bitmap source=reset(true,fault);Cpu cpu=new Cpu();Bitmap got=GpuGeometry1960.resample(source,0,4,3,cpu);
        check(expected.equals(GpuQualification1961.reason),"foreground typed reason "+fault);
        check(!GpuQualification1961.exact,"transport failures never become exact foreground rejection");
        check(cpu.calls==1&&cpu.reason==reason&&got==cpu.output,"unchanged CPU result and reason on failure "+fault);
        check(!source.isRecycled()&&!got.isRecycled(),"caller owns live source and returned result");
        check(GpuNoise1960.opened==GpuNoise1960.closed,"failed foreground session closes");
        check(GpuQualification1961.measuredCpu==0&&GpuQualification1961.measuredGpu==0,"non-speed failures have no speed samples");
    }
    static void deferred(int fault,String expected,boolean exact)throws Exception{
        Bitmap source=reset(false,fault);Cpu cpu=new Cpu();Bitmap got=GpuGeometry1960.resample(source,0,4,3,cpu);
        check(got==cpu.output&&cpu.calls==1,"unqualified geometry preserves foreground CPU result");
        GpuQualification1961.Probe p=GpuQualification1961.pending;check(p!=null,"deferred proof owns snapshot");
        try{p.run(new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}});}finally{p.close();}
        check(expected.equals(GpuQualification1961.reason),"deferred typed reason "+fault+": "+GpuQualification1961.reason);
        check(GpuQualification1961.exact==exact,"only true deferred pixel mismatch is exact");
        check(GpuNoise1960.opened==GpuNoise1960.closed,"deferred session closes on every tested exit");
        check(!source.isRecycled()&&!got.isRecycled(),"deferred cleanup cannot recycle foreground ownership");
        if("speed_condition".equals(expected))check(GpuQualification1961.measuredCpu>0&&GpuQualification1961.measuredGpu>GpuQualification1961.measuredCpu,"deferred slow result contains measured positive pair");
        else check(GpuQualification1961.measuredCpu==0&&GpuQualification1961.measuredGpu==0,"deferred non-speed failure has no fabricated timing");
    }
    public static void main(String[] args)throws Exception{
        foreground(1,"transport_failure",PipelineDetail1988.CANDIDATE_UNAVAILABLE);
        foreground(2,"exception",PipelineDetail1988.CANDIDATE_FAILURE);
        foreground(3,"memory_budget",PipelineDetail1988.MEMORY_LIMIT);
        foreground(4,"exception",PipelineDetail1988.CANDIDATE_FAILURE);
        deferred(1,"transport_failure",false);deferred(2,"exception",false);deferred(3,"memory_budget",false);
        deferred(4,"exception",false);deferred(5,"quality_mismatch",true);deferred(6,"speed_condition",false);deferred(7,"execution_unavailable",false);
        Bitmap source=reset(true,0);GpuQualification1961.record=new GpuQualification1961.Record(1,1);Cpu cpu=new Cpu();
        Bitmap out=GpuGeometry1960.resample(source,0,4,3,cpu);
        check("speed_condition".equals(GpuQualification1961.reason)&&"geometry_live".equals(GpuQualification1961.candidate),"live route records complete-call speed miss");
        check(cpu.calls==0&&out!=null&&!out.isRecycled(),"successful GPU output retained despite loss of future speed admission");
        check(GpuQualification1961.measuredCpu==1&&GpuQualification1961.measuredGpu>1,"live failure reports actual old baseline and observed candidate");
        source=reset(true,0);GpuQualification1961.record=new GpuQualification1961.Record(0,1);cpu=new Cpu();GpuGeometry1960.resample(source,0,4,3,cpu);
        check("comparison_incomplete".equals(GpuQualification1961.reason),"unavailable live baseline is incomplete rather than measured slow");
        source=reset(true,0);GpuNoise1960.fits=false;cpu=new Cpu();out=GpuGeometry1960.resample(source,0,4,3,cpu);
        check(cpu.calls==1&&cpu.reason==PipelineDetail1988.UNAVAILABLE&&out==cpu.output,"temporary workspace refusal preserves CPU fallback");
        check(GpuQualification1961.reason==null&&!GpuQualification1961.exact,"temporary eligibility refusal does not invalidate exact proof");
        check(GpuNoise1960.opened==0,"workspace refusal occurs before GPU allocation");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"foreground_cases\":7,\"deferred_cases\":7}");
    }
}
