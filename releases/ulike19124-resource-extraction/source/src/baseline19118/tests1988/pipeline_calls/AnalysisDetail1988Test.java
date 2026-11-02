package com.hiro.ulike;

/** Actual .88 capture/admission bodies and unchanged Java pixel estimators. */
public final class AnalysisDetail1988Test {
    static final int W=129,H=131;
    static void reset(){AnalysisCapture1987Test.reset();}
    static void exact(float[] a,float[] b){DetailCalls1988.check(AnalysisCapture1987Test.same(a,b),"all regional raw float bits unchanged");}
    static void cold(boolean spatial)throws Exception{
        reset();AnalysisCapture1987Test.Source source=new AnalysisCapture1987Test.Source(W,H,81);
        ProcessingTiming1947.Trace owner=DetailCalls1988.trace();ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(owner);
        int phase=spatial?PipelineDetail1988.AUX_SPATIAL:PipelineDetail1988.MODEL_REGIONS;
        try{
            if(spatial){SpatialNoise1934 expected=SpatialNoise1934.probeCpu1961(source.copy(),W,H);
                SpatialNoise1934 actual=GpuAnalysis1961.spatial(source,W,H);
                DetailCalls1988.check(SpatialNoise1934.same1961(expected,actual),"all spatial raw float bits unchanged");
                DetailCalls1988.selected(owner,phase,0,1,PipelineDetail1988.PROOF_MISSING);
            }else{
                float[] ev=AnalysisCapture1987Test.evidence(),expected=StrongNoise1958.estimateRegionsSnapshot1961(source.copy(),W,H,ev);int[] selected={8,8};
                exact(expected,GpuAnalysis1961.regions1982(source,W,H,ev,AnalysisCapture1987Test.regionCpu(W,H,ev,0),selected));
                DetailCalls1988.check(selected[0]==0&&selected[1]==PipelineDetail1988.PROOF_MISSING,"cold regional actual CPU reason");
            }
        }finally{ProcessingTiming1947.restore(scope);}
        DetailCalls1988.check(source.reads==(spatial?AnalysisCapture1987Test.spatialCount(W,H):AnalysisCapture1987Test.regionCount(W,H)),"telemetry never rereads original patches");
        DetailCalls1988.check(GpuQualification1961.reservations==1&&GpuQualification1961.commits==1&&GpuQualification1961.pending!=null,"same single reservation and commit");
        DetailCalls1988.copied(owner,phase,PipelineDetail1988.RESERVED,true,false);
        DetailCalls1988.copied(owner,phase,PipelineDetail1988.CAPTURED,true,true);
        DetailCalls1988.copied(owner,phase,PipelineDetail1988.QUEUED,true,false);
        if(!DetailCalls1988.OPTIONAL_FAILURE){long reserved=owner.pipelineCopies1988[DetailCalls1988.copyAt(phase,PipelineDetail1988.RESERVED)+2];
            DetailCalls1988.check(reserved==GpuQualification1961.retained,"snapshot diagnostics use actual complete reservation bytes");}
        GpuQualification1961.clear();
    }
    static void declines()throws Exception{
        for(int mode=0;mode<4;mode++){
            reset();AnalysisCapture1987Test.Source source=new AnalysisCapture1987Test.Source(W,H,82);
            if(mode==0)GpuQualification1961.allow=false;
            if(mode==1)GpuQualification1961.beginAllowed=false;
            if(mode==2)GpuQualification1961.throwReserve=true;
            if(mode==3)source.cancelAt=2;
            ProcessingTiming1947.Trace t=DetailCalls1988.trace();ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(t);
            try{SpatialNoise1934 expected=SpatialNoise1934.probeCpu1961(source.copy(),W,H);
                DetailCalls1988.check(SpatialNoise1934.same1961(expected,GpuAnalysis1961.spatial(source,W,H)),"declined optional capture preserves complete CPU result "+mode);
            }finally{ProcessingTiming1947.restore(scope);}
            int outcome=mode==0?PipelineDetail1988.SKIPPED:mode==1?PipelineDetail1988.INTERRUPTED:mode==2?PipelineDetail1988.ALLOCATION_FAILED:PipelineDetail1988.INCOMPLETE;
            DetailCalls1988.copied(t,PipelineDetail1988.AUX_SPATIAL,outcome,mode!=0,false);
            DetailCalls1988.check(source.reads==AnalysisCapture1987Test.spatialCount(W,H)&&GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"decline frees every reservation without reread "+mode);
            DetailCalls1988.check(GpuQualification1961.speedFailures+GpuQualification1961.exactFailures==0,"capture decline spends no proof retry "+mode);
        }
    }
    static void replayAndAdopt()throws Exception{
        for(int mode=0;mode<4;mode++){
            reset();AnalysisCapture1987Test.Source source=new AnalysisCapture1987Test.Source(W,H,83);
            boolean spatial=mode==0;String key=spatial?AnalysisCapture1987Test.spatialKey(W,H):AnalysisCapture1987Test.regionKey(W,H);
            GpuQualification1961.records.put(key,new GpuQualification1961.Record(1000000000L,1,0));
            float[] ev=AnalysisCapture1987Test.evidence(),expected=StrongNoise1958.estimateRegionsSnapshot1961(source.copy(),W,H,ev);
            if(mode==2)GpuNoise1960.result=AnalysisCapture1987Test.bits(expected);
            if(mode==3)GpuNoise1960.duringExecute=new Runnable(){public void run(){throw new UnsatisfiedLinkError("controlled unavailable candidate");}};
            ProcessingTiming1947.Trace t=DetailCalls1988.trace();ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(t);
            try{
                if(spatial){SpatialNoise1934 wanted=SpatialNoise1934.probeCpu1961(source.copy(),W,H);
                    DetailCalls1988.check(SpatialNoise1934.same1961(wanted,GpuAnalysis1961.spatial(source,W,H)),"qualified failed GPU uses same immutable spatial pixels");
                    DetailCalls1988.selected(t,PipelineDetail1988.AUX_SPATIAL,0,1,PipelineDetail1988.CANDIDATE_UNAVAILABLE);
                }else{int[] selected={8,8};exact(expected,GpuAnalysis1961.regions1982(source,W,H,ev,AnalysisCapture1987Test.regionCpu(W,H,ev,0),selected));
                    DetailCalls1988.check(selected[0]==(mode==2?1:0),"GPU attempt cannot masquerade as adopted output");
                    if(mode!=2)DetailCalls1988.check(selected[1]==(mode==3?PipelineDetail1988.CANDIDATE_FAILURE:PipelineDetail1988.CANDIDATE_UNAVAILABLE),"observed candidate failure reason only");}
            }finally{ProcessingTiming1947.restore(scope);}
            DetailCalls1988.check(source.reads==(spatial?AnalysisCapture1987Test.spatialCount(W,H):AnalysisCapture1987Test.regionCount(W,H)),"completed GPU gather read once even after failure");
            DetailCalls1988.check(GpuNoise1960.executions==1&&GpuQualification1961.reservations==0&&GpuQualification1961.pending==null,"same one GPU attempt and no new reservation");
            if(mode!=2)DetailCalls1988.copied(t,spatial?PipelineDetail1988.AUX_SPATIAL:PipelineDetail1988.MODEL_REGIONS,PipelineDetail1988.REPLAYED,true,false);
        }
    }
    static void noOwnerAndFailure()throws Exception{
        reset();ProcessingTiming1947.Trace t=DetailCalls1988.trace();ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(t);
        GpuQualification1961.background=true;
        try{GpuAnalysis1961.spatial(new AnalysisCapture1987Test.Source(W,H,1),W,H);}finally{ProcessingTiming1947.restore(scope);}
        DetailCalls1988.untouched(t,"background qualification");
        reset();AnalysisCapture1987Test.Source source=new AnalysisCapture1987Test.Source(W,H,2);source.throwAt=2;source.failure=new IllegalStateException("original source failure");
        t=DetailCalls1988.trace();scope=ProcessingTiming1947.enter(t);Throwable actual=null;
        try{GpuAnalysis1961.spatial(source,W,H);}catch(Throwable failure){actual=failure;}finally{ProcessingTiming1947.restore(scope);}
        DetailCalls1988.check(actual==source.failure&&source.reads==2,"same original source exception and ordering");
        DetailCalls1988.check(GpuQualification1961.retained==0&&GpuQualification1961.pending==null,"exception frees capture ownership");
        DetailCalls1988.check(DetailCalls1988.count(t,PipelineDetail1988.AUX_SPATIAL,0)==0,"failed spatial result is not counted as completed CPU output");
    }
    public static void main(String[] args)throws Exception{
        cold(true);cold(false);declines();replayAndAdopt();noOwnerAndFailure();GpuQualification1961.clear();
        DetailCalls1988.report("analysis");
    }
}
