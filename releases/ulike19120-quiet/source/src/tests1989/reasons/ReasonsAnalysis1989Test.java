package com.hiro.ulike;

/** Current production capture path and exact region/spatial estimators. */
public final class ReasonsAnalysis1989Test {
    static final int W=129,H=131,PHASE=PipelineDetail1988.MODEL_REGIONS;
    static void check(boolean v,String s){ReasonsAssertions1989.check(v,s);}
    static void reset(){AnalysisCapture1987Test.reset();}
    static void same(float[] a,float[] b){check(AnalysisCapture1987Test.same(a,b),"all region evidence raw float bits exact");}
    static long acquired(){return 4L*AnalysisCapture1987Test.regionCount(W,H)*32*32;}
    static void run(String mode)throws Exception{
        reset();AnalysisCapture1987Test.Source source=new AnalysisCapture1987Test.Source(W,H,89);
        float[] evidence=AnalysisCapture1987Test.evidence(),expected=StrongNoise1958.estimateRegionsSnapshot1961(source.copy(),W,H,evidence);
        int reason=0,outcome=PipelineDetail1988.QUEUED;
        if(mode.startsWith("denied-")){reason=Integer.parseInt(mode.substring(7));GpuQualification1961.reserveReason=reason;outcome=PipelineDetail1988.QUEUE_DECLINED;}
        if(mode.equals("commit-declined")){reason=9;GpuQualification1961.commitReason=reason;outcome=PipelineDetail1988.QUEUE_DECLINED;}
        if(mode.equals("begin-declined")){GpuQualification1961.beginAllowed=false;outcome=PipelineDetail1988.INTERRUPTED;}
        if(mode.equals("allocation")){GpuQualification1961.throwReserve=true;outcome=PipelineDetail1988.ALLOCATION_FAILED;}
        if(mode.equals("partial")){source.cancelAt=2;outcome=PipelineDetail1988.INCOMPLETE;}
        if(mode.equals("background"))GpuQualification1961.background=true;
        if(mode.equals("null-metadata"))GpuQualification1961.nullDecision=true;
        if(mode.equals("null-commit"))GpuQualification1961.nullCommit=true;
        if(mode.equals("source-failure")){source.throwAt=2;source.failure=new IllegalStateException("original source failure");}
        ProcessingTiming1947.Trace owner=ReasonsAssertions1989.trace(),other=ReasonsAssertions1989.trace();
        if(mode.equals("terminal"))owner.state=1;
        ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(owner);int[] selected={9,9};Throwable failure=null;
        float[] result=null;long requested=0;
        try{result=GpuAnalysis1961.regions1982(source,W,H,evidence,AnalysisCapture1987Test.regionCpu(W,H,evidence,0),selected);}
        catch(Throwable e){failure=e;}finally{ProcessingTiming1947.restore(scope);}
        if(mode.equals("source-failure")){
            check(failure==source.failure&&source.reads==2,"original source exception identity/order");
            check(GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"source failure frees all ownership");return;
        }
        check(failure==null,"optional telemetry/copy state cannot fail CPU");same(expected,result);
        check(selected[0]==0&&selected[1]==PipelineDetail1988.PROOF_MISSING,"missing-proof CPU selection unchanged");
        check(source.reads==AnalysisCapture1987Test.regionCount(W,H),"single source traversal unchanged");
        ReasonsAssertions1989.untouched(other,"analysis unrelated trace");
        if(mode.equals("background")||mode.equals("terminal")||ReasonsAssertions1989.FAULT){
            ReasonsAssertions1989.untouched(owner,"excluded analysis owner/failing facade");GpuQualification1961.clear();return;
        }
        int reserveAt=ReasonsAssertions1989.copyAt(PHASE,PipelineDetail1988.RESERVED);
        int outcomeAt=ReasonsAssertions1989.copyAt(PHASE,outcome);
        requested=owner.pipelineCopies1988[outcomeAt+2];
        if(mode.startsWith("denied-")){
            ReasonsAssertions1989.copy(owner,PHASE,outcome,requested,0);
            check(requested>0&&GpuQualification1961.commits==0&&GpuQualification1961.pending==null,"preallocation denial uses request size without acquired pixels");
        }else if(mode.equals("begin-declined")||mode.equals("allocation")){
            ReasonsAssertions1989.copy(owner,PHASE,outcome,requested,0);
            check(GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"precopy failure acquires no pixel payload");
        }else if(mode.equals("partial")){
            ReasonsAssertions1989.copy(owner,PHASE,outcome,requested,acquired());
            check(GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"cancelled full traversal remains no queued probe");
        }else if(mode.equals("null-metadata")){
            check(owner.pipelineCopyQuantities1989[reserveAt]==1&&owner.pipelineQueueCounts1989[PHASE*22]==0,"null decision retains quantity with queue metadata unknown");
            check(GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"null metadata fallback closes capture");
        }else if(mode.equals("null-commit")){
            check(owner.pipelineCopyQuantities1989[ReasonsAssertions1989.copyAt(PHASE,PipelineDetail1988.CAPTURED)]==1,"null final decision cannot erase observed complete capture");
        }else{
            ReasonsAssertions1989.copy(owner,PHASE,PipelineDetail1988.RESERVED,requested,0);
            ReasonsAssertions1989.copy(owner,PHASE,PipelineDetail1988.CAPTURED,requested,acquired());
            ReasonsAssertions1989.copy(owner,PHASE,outcome,requested,acquired());
            check(requested>acquired(),"closure request includes data beyond acquired pixel payload");
        }
        if(mode.startsWith("denied-")||mode.equals("cold")||mode.equals("commit-declined")){
            int last=PHASE*10;long[] d=owner.pipelineQueueLast1989;int expectedReason=mode.equals("cold")?1:reason;
            check(owner.pipelineQueueCounts1989[PHASE*22+expectedReason]==1&&d[last+1]==expectedReason,
                "REASON1989_MISSING: exact existing queue decision reason retained");
            check(d[last+2]==1234567L&&d[last+3]==2&&d[last+4]==3&&d[last+5]==1&&d[last+6]==9876543L,
                "existing retry/queued/running/retained scalar snapshot copied without refresh");
            check(d[last+7]==requested&&d[last+8]==(mode.startsWith("denied-")?0:acquired()),"last decision owns distinct request/acquired facts");
        }
        check(GpuQualification1961.permissionCalls==1,"no additional maySchedule/canQueue query for diagnostics");
        GpuQualification1961.clear();check(GpuQualification1961.retained==0,"test drains original probe ownership");
    }
    static void unusual(boolean invalid)throws Exception{
        reset();final AnalysisCapture1987Test.Source source=new AnalysisCapture1987Test.Source(W,H,90);
        float[] evidence=AnalysisCapture1987Test.evidence();
        GpuAnalysis1961.RegionsCpu cpu=new GpuAnalysis1961.RegionsCpu(){public float[] compute(StrongNoise1958.Patches p){
            int[] patch=new int[32*32];p.read(patch,invalid?0:16,invalid?0:16,invalid?4:32,invalid?4:32);
            return new float[]{Float.intBitsToFloat(patch[0])};
        }};
        float[] expected=cpu.compute(source.copy());ProcessingTiming1947.Trace t=ReasonsAssertions1989.trace();ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(t);
        float[] actual;try{actual=GpuAnalysis1961.regions1982(source,W,H,evidence,cpu,new int[2]);}finally{ProcessingTiming1947.restore(scope);}
        same(expected,actual);check(source.reads==1,"custom callback source read unchanged");
        long requested=t.pipelineCopies1988[ReasonsAssertions1989.copyAt(PHASE,PipelineDetail1988.INCOMPLETE)+2];
        if(!ReasonsAssertions1989.FAULT)ReasonsAssertions1989.copy(t,PHASE,PipelineDetail1988.INCOMPLETE,requested,invalid?-1:4096);
        check(GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"invalid/partial callback cannot retain partial proof");
    }
    public static void main(String[] args)throws Exception{
        String mode=args[0];if(mode.equals("invalid-traversal")||mode.equals("valid-partial"))unusual(mode.equals("invalid-traversal"));else run(mode);
        GpuQualification1961.clear();ReasonsAssertions1989.report("analysis-"+mode);
    }
}
