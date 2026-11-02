package com.hiro.ulike;

import android.content.Context;
import android.graphics.Bitmap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

/** Current production dispatchers, CPU arithmetic, owner routing and real scalar sinks. */
public final class ReasonsRoutes1989Test {
    static void check(boolean v,String s){ReasonsAssertions1989.check(v,s);}
    static void initialize()throws Exception{
        GpuNoise1960.enabled=true;GpuQualification1961.initialize(new Context());
        ReasonsAssertions1989.field(SingleNoise1955.class,"nativeState").setInt(null,-1);
        ReasonsAssertions1989.field(StrongNoise1958.class,"nativeState").setInt(null,-1);
    }
    static void qualify(String key,int variant){GpuQualification1961.qualified(key,1000000000L,1L,variant);check(GpuQualification1961.restore(key)!=null,"controlled certificate installed");}
    static void single(String mode)throws Exception{
        boolean residual=mode.startsWith("residual-");String kind=mode.substring(mode.indexOf('-')+1);
        GpuNoise1960.single=!residual;
        final int w=33,h=97,begin=7,end=80;int[] p=BackendDiagnostics1982Test.image(w,h),pristine=p.clone();
        SingleNoise1955.Model model=BackendDiagnostics1982Test.model(p,w,h);
        check(model.meanSigma()>=.30f,"nonconstant image actually dispatches Single");
        int[] expected=new int[p.length];Arrays.fill(expected,0x13579bdf);
        SingleNoise1955.processCpuRange(p,expected,w,h,begin,end,0,h,0,4,true,model,null);
        SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();
        int[] actual=new int[p.length];Arrays.fill(actual,0x13579bdf);
        SingleStage1981 stage=null;SingleStage1981.Lease held=null;
        int why=PipelineDetail1988.UNKNOWN,backend=0;
        try{
            boolean warm=!(kind.equals("unavailable")||kind.equals("memory")||kind.equals("cold"));
            String key=null,reference=null;Object gate=null;
            if(warm){
                SingleNoise1955.processRange(p,actual,w,h,begin,end,0,h,0,4,true,model,null,workspace);
                GpuQualification1961.captureChanged();
                Map<?,?> gates=(Map<?,?>)ReasonsAssertions1989.field(residual?GpuResidual1961.class:GpuSingle1960.class,"GATES").get(null);
                check(gates.size()==1,"exact real driver key captured");key=(String)gates.keySet().iterator().next();gate=gates.get(key);
                qualify(key,0);reference=SingleCpu1981.referenceKey(key,w,h,begin,end,0,h,0,4,true,model,null);
                qualify(SingleCpu1981.speedKey(reference,false),0);
            }
            if(kind.equals("unavailable")){GpuNoise1960.enabled=false;why=PipelineDetail1988.UNAVAILABLE;}
            else if(kind.equals("memory")){GpuNoise1960.budget=false;why=PipelineDetail1988.MEMORY_LIMIT;}
            else if(kind.equals("cold"))why=PipelineDetail1988.PROOF_MISSING;
            else if(kind.equals("exact")){GpuQualification1961.rejectExact(key);why=PipelineDetail1988.EXACT_REJECTED;}
            else if(kind.equals("busy")){ReasonsAssertions1989.field(gate.getClass(),"busy").setBoolean(gate,true);why=PipelineDetail1988.ROUTE_BUSY;}
            else if(kind.equals("policy")){qualify(SingleCpu1981.speedKey(reference,false),1);why=PipelineDetail1988.CPU_POLICY_SELECTED;}
            else if(kind.equals("session")){GpuNoise1960.busy=true;why=PipelineDetail1988.SESSION_BUSY;}
            else if(kind.equals("stage")){
                qualify(SingleStage1981.key(reference),1);stage=SingleStage1981.begin(model);GpuNoise1960.fault=1;
                held=SingleStage1981.acquire(model,residual?SingleStage1981.RESIDUAL:SingleStage1981.SINGLE,reference);
                check(held!=null,"other worker holds a real selected GPU lease");why=PipelineDetail1988.STAGE_LEASE;
            }else if(kind.equals("noresult")){GpuNoise1960.fault=0;why=PipelineDetail1988.CANDIDATE_UNAVAILABLE;}
            else if(kind.equals("link")){GpuNoise1960.fault=3;why=PipelineDetail1988.CANDIDATE_FAILURE;}
            else if(kind.equals("memory-failure")){GpuNoise1960.fault=6;why=PipelineDetail1988.MEMORY_LIMIT;}
            else if(kind.equals("open")){GpuNoise1960.denyOpen=true;why=PipelineDetail1988.UNAVAILABLE;}
            else if(kind.equals("gpu")){GpuNoise1960.fault=1;backend=1;}
            else throw new AssertionError(mode);
            GpuNoise1960.readback=Arrays.copyOfRange(expected,begin*w,end*w);
            Arrays.fill(actual,0x13579bdf);
            int attempts=GpuNoise1960.openAttempts;
            SingleNoise1955.processRange(p,actual,w,h,begin,end,0,h,0,4,true,model,null,workspace);
            ReasonsAssertions1989.exact(expected,actual,"actual Single/Residual output "+mode);
            ReasonsAssertions1989.exact(pristine,p,"caller source retained "+mode);
            check(workspace.selectedBackend1982==backend,"actual accepted backend retained "+mode);
            check(!ReasonsAssertions1989.field(workspace.getClass(),"inUse").getBoolean(workspace),"worker lease relinquished");
            if(kind.equals("stage")||kind.equals("session"))check(GpuNoise1960.openAttempts==attempts,"unavailable lease does not retry a legacy GPU open");
            int observed=ReasonsAssertions1989.optionalInt(workspace,"selectedReason1989");
            check(observed==why,"REASON1989_MISSING: Single final branch "+mode+" expected "+why+" actual "+observed);
            int primary=ReasonsAssertions1989.optionalInt(workspace,"primaryGpuReason1989");
            check(primary==((residual||kind.equals("unavailable"))?PipelineDetail1988.UNAVAILABLE:0),"REASON1989_MISSING: primary handoff remains independent "+mode);
            // Reusing exactly this worker for a CPU fixed no-op must clear both
            // the old cause and the primary-to-residual handoff unconditionally.
            if(held!=null){held.complete();held.close();held=null;}if(stage!=null){stage.close();stage=null;}
            GpuNoise1960.busy=false;GpuNoise1960.budget=true;
            Arrays.fill(actual,0x13579bdf);int[] fixed=new int[p.length];Arrays.fill(fixed,0x13579bdf);
            SingleNoise1955.processCpuRange(p,fixed,w,h,begin,end,0,h,0,0,true,model,null);
            SingleNoise1955.processRange(p,actual,w,h,begin,end,0,h,0,0,true,model,null,workspace);
            ReasonsAssertions1989.exact(fixed,actual,"same workspace next fixed strip");
            check(ReasonsAssertions1989.optionalInt(workspace,"selectedReason1989")==PipelineDetail1988.CPU_FIXED&&
                ReasonsAssertions1989.optionalInt(workspace,"primaryGpuReason1989")==0,"both reason scalars reset per strip");
        }finally{if(held!=null){held.complete();held.close();}if(stage!=null)stage.close();workspace.close();GpuQualification1961.captureChanged();}
        check(GpuNoise1960.active==0,"all transport sessions released");
    }
    static Bitmap bitmap(int w,int h){Bitmap b=Bitmap.from(w,h,BackendDiagnostics1982Test.image(w,h),Bitmap.Config.ARGB_8888,true);b.setHasAlpha(false);return b;}
    static void front(String mode)throws Exception{
        GpuNoise1960.enabled=false;Bitmap reference=bitmap(33,65),actual=bitmap(33,65);
        Method run=QualityPipeline1932.class.getDeclaredMethod("singleNoiseInPlace1955",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class);run.setAccessible(true);
        run.invoke(null,reference,BackendDiagnostics1982Test.plan(),4,true);
        ProcessingTiming1947.Trace owner=ReasonsAssertions1989.trace(),other=ReasonsAssertions1989.trace();
        ProcessingTiming1947.bind(actual,owner);ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(other);
        ThreadLocal<Object> background=null;
        if(mode.equals("front-background")){background=(ThreadLocal<Object>)ReasonsAssertions1989.field(GpuResident1976.class,"BENCHMARK").get(null);background.set(Boolean.TRUE);}
        if(mode.equals("front-terminal"))owner.state=1;
        if(mode.equals("front-write-failure"))ReasonsAssertions1989.field(Bitmap.class,"writeFailure").setBoolean(actual,true);
        Throwable failure=null;
        try{run.invoke(null,actual,BackendDiagnostics1982Test.plan(),4,true);}catch(InvocationTargetException e){failure=e.getCause();}
        finally{if(background!=null)background.remove();ProcessingTiming1947.restore(scope);}
        boolean writeFailure=mode.equals("front-write-failure");check(writeFailure?failure instanceof IllegalStateException:failure==null,"original front completion/exception");
        if(!writeFailure)ReasonsAssertions1989.exact(reference.snapshot(),actual.snapshot(),"actual committed front result");
        ReasonsAssertions1989.untouched(other,"unrelated entered owner");
        if(background!=null||mode.equals("front-terminal"))ReasonsAssertions1989.untouched(owner,"background/terminal owner");
        else if(writeFailure){check(ReasonsAssertions1989.count(owner,1,0)==0&&owner.pipelinePriorReasons1989[20+6]==0,"failed write never commits reasons or adopted output");}
        else {
            long units=owner.backendCounts1982[0]-1;check(units>0,"actual committed worker intervals observed");
            ReasonsAssertions1989.selected(owner,1,0,units,PipelineDetail1988.UNAVAILABLE);
            check(owner.pipelinePriorReasons1989[20+PipelineDetail1988.UNAVAILABLE]==(ReasonsAssertions1989.FAULT?0:units),"REASON1989_MISSING: committed primary-to-Residual handoff");
        }
        check(Bitmap.writesAfterRecycle.get()==0,"workers joined before bitmap disposal");reference.recycle();actual.recycle();
    }
    static void geometry(String mode)throws Exception{
        final int sw=17,sh=25,w=23,h=35;String kind=mode.substring("geometry-".length());
        Bitmap source=bitmap(sw,sh),expected=FastResize1933.resampleCpu1960(source,0,w,h);
        ProcessingTiming1947.Trace owner=ReasonsAssertions1989.trace(),other=ReasonsAssertions1989.trace();
        int why=PipelineDetail1988.PROOF_MISSING,backend=0;
        String key=GpuNoise1960.ENV+"|geometry|"+sw+","+sh+",0,"+w+","+h;
        if(kind.equals("unavailable")){GpuNoise1960.enabled=false;why=PipelineDetail1988.UNAVAILABLE;}
        else if(kind.equals("session")){GpuNoise1960.busy=true;why=PipelineDetail1988.SESSION_BUSY;}
        else if(kind.equals("environment")){GpuNoise1960.environmentAvailable=false;why=PipelineDetail1988.ENVIRONMENT_MISSING;}
        else if(kind.equals("exact")){GpuQualification1961.rejectExact(key);why=PipelineDetail1988.EXACT_REJECTED;}
        else if(kind.equals("noresult")||kind.equals("link")||kind.equals("memory")||kind.equals("gpu")||kind.equals("open")){
            qualify(key,0);GpuNoise1960.readback=expected.snapshot();
            if(kind.equals("gpu")){GpuNoise1960.fault=1;backend=1;why=0;}
            else if(kind.equals("noresult")){GpuNoise1960.fault=0;why=PipelineDetail1988.CANDIDATE_UNAVAILABLE;}
            else if(kind.equals("link")){GpuNoise1960.fault=3;why=PipelineDetail1988.CANDIDATE_FAILURE;}
            else if(kind.equals("memory")){GpuNoise1960.fault=6;why=PipelineDetail1988.MEMORY_LIMIT;}
            else {GpuNoise1960.denyOpen=true;why=PipelineDetail1988.UNAVAILABLE;}
        }else if(!(kind.equals("cold")||kind.equals("background")||kind.equals("terminal")))throw new AssertionError(mode);
        ThreadLocal<Object> background=null;if(kind.equals("background")){background=(ThreadLocal<Object>)ReasonsAssertions1989.field(GpuResident1976.class,"BENCHMARK").get(null);background.set(Boolean.TRUE);}
        if(kind.equals("terminal"))owner.state=1;
        ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(other);Bitmap actual;
        try{actual=FastResize1933.resample1982(source,0,w,h,owner);}finally{if(background!=null)background.remove();ProcessingTiming1947.restore(scope);}
        ReasonsAssertions1989.exact(expected.snapshot(),actual.snapshot(),"actual geometry complete output "+mode);
        check(!source.isRecycled(),"geometry retains caller source");ReasonsAssertions1989.untouched(other,"geometry unrelated scope");
        if(kind.equals("background")||kind.equals("terminal"))ReasonsAssertions1989.untouched(owner,"geometry excluded owner");
        else ReasonsAssertions1989.selected(owner,6,backend,1,why);
        actual.recycle();expected.recycle();source.recycle();check(GpuNoise1960.active==0,"geometry releases transport");
    }
    static void correction(String mode)throws Exception{
        GpuNoise1960.enabled=false;ReasonsAssertions1989.field(GpuFinish1953.class,"loaded").setInt(null,1);
        WholeRoute1953.Engine engine=(WholeRoute1953.Engine)ReasonsAssertions1989.field(WholeRoute1953.class,"ENGINE").get(null);
        engine.history=new WholeRoute1953.History(){public String environment(){return "reason89-correction";}public long restore(int[] k,String e){return 0;}public void qualified(int[] k,String e,long b){}public void rejected(int[] k,String e){}};
        boolean moire=!mode.equals("correction-sharp"),sharp=!mode.equals("correction-moire"),joined=moire&&sharp;
        int phase=joined?10:moire?5:7;
        Bitmap reference=bitmap(33,65),actual=bitmap(33,65);QualityPixels1932.Plan plan=BackendDiagnostics1982Test.plan();
        if(joined){QualityPipeline1932.finishInPlace(reference,plan,true,false);QualityPipeline1932.finishInPlace(reference,plan,false,true);}else QualityPipeline1932.finishInPlace(reference,plan,moire,sharp);
        ProcessingTiming1947.Trace owner=ReasonsAssertions1989.trace(),other=ReasonsAssertions1989.trace();ProcessingTiming1947.bind(actual,owner);
        ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(other);Bitmap result;
        try{result=QualityPipeline1932.finishRoute1953(actual,plan,plan,moire,sharp,joined);}finally{ProcessingTiming1947.restore(scope);}
        check(result==actual,"CPU WholeRoute retains owned result");ReasonsAssertions1989.exact(reference.snapshot(),result.snapshot(),"actual correction CPU pixels and order");
        ReasonsAssertions1989.selected(owner,phase,0,1,PipelineDetail1988.PROOF_MISSING);
        ReasonsAssertions1989.untouched(other,"correction unrelated scope");reference.recycle();actual.recycle();
    }
    public static void main(String[] args)throws Exception{
        if(args.length>1)System.load(args[1]);initialize();String mode=args[0];
        if(mode.startsWith("single-")||mode.startsWith("residual-"))single(mode);
        else if(mode.startsWith("front-"))front(mode);
        else if(mode.startsWith("geometry-"))geometry(mode);
        else if(mode.startsWith("correction-"))correction(mode);
        else throw new AssertionError(mode);
        GpuQualification1961.captureChanged();ReasonsAssertions1989.report(mode);System.exit(0);
    }
}
