package com.hiro.ulike;

import android.content.Context;
import android.graphics.Bitmap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Current .88 foreground entry points, actual CPU pixels and actual sinks.
 * Android pixel ownership and GPU transfer buffers are controlled host peers. */
public final class PipelineCalls1988Test {
    static QualityPixels1932.Plan plan(){return BackendDiagnostics1982Test.plan();}
    static Bitmap bitmap(int w,int h){Bitmap b=Bitmap.from(w,h,BackendDiagnostics1982Test.image(w,h),Bitmap.Config.ARGB_8888,true);b.setHasAlpha(false);return b;}
    static void initialize()throws Exception{
        GpuNoise1960.enabled=true;GpuNoise1960.fault=0;GpuQualification1961.initialize(new Context());
        DetailCalls1988.field(SingleNoise1955.class,"nativeState").setInt(null,-1);
        DetailCalls1988.field(StrongNoise1958.class,"nativeState").setInt(null,-1);
    }
    static final class Source implements StrongNoise1958.Patches {
        final int width,height;final int[] pixels;int reads;
        Source(int w,int h){width=w;height=h;pixels=BackendDiagnostics1982Test.image(w,h);}
        public void read(int[] a,int x,int y,int w,int h){reads++;for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,a,row*w,w);}
    }
    static void model(boolean fixed)throws Exception{
        GpuNoise1960.enabled=false;final int w=33,h=65;Source a=new Source(w,h),b=new Source(w,h);
        StrongNoise1958.Model before=fixed?StrongNoise1958.prepareJava(a,w,h,4,true):StrongNoise1958.prepare(a,w,h,4,true);
        ProcessingTiming1947.Trace owner=DetailCalls1988.trace();ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(owner);
        StrongNoise1958.Preparation1982 selected=new StrongNoise1958.Preparation1982();StrongNoise1958.Model after;
        try{after=fixed?StrongNoise1958.prepareJava(b,w,h,4,true):StrongNoise1958.prepare1982(b,w,h,4,true,null,selected);}
        finally{ProcessingTiming1947.restore(scope);}
        DetailCalls1988.check(StrongNoise1958.sameModel1961(before,after),"all actual model maps and evidence raw bits exact");
        DetailCalls1988.check(a.reads==b.reads,"model diagnostics do not add source preparation reads");
        if(!fixed)DetailCalls1988.check(selected.cpuUnits==3&&selected.gpuUnits==0,"original aggregate selection still exactly CPU3");
        DetailCalls1988.selected(owner,PipelineDetail1988.MODEL_SOURCE,0,1,PipelineDetail1988.CPU_FIXED);
        DetailCalls1988.selected(owner,PipelineDetail1988.MODEL_REGIONS,0,1,fixed?PipelineDetail1988.CPU_FIXED:PipelineDetail1988.PROOF_MISSING);
        DetailCalls1988.selected(owner,PipelineDetail1988.MODEL_MAPS,0,1,fixed?PipelineDetail1988.CPU_FIXED:PipelineDetail1988.PROOF_MISSING);
        if(DetailCalls1988.OPTIONAL_FAILURE)DetailCalls1988.untouched(owner,"failed optional model facade");
    }
    static Method front()throws Exception{
        Method m=QualityPipeline1932.class.getDeclaredMethod("singleNoiseInPlace1955",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class);m.setAccessible(true);return m;
    }
    static void front(String mode)throws Exception{
        GpuNoise1960.enabled=false;Bitmap reference=bitmap(33,65),b=bitmap(33,65);Method run=front();
        run.invoke(null,reference,plan(),4,true);
        ProcessingTiming1947.Trace owner=DetailCalls1988.trace(),unrelated=DetailCalls1988.trace();ProcessingTiming1947.bind(b,owner);
        ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(unrelated);
        ThreadLocal<Object> background=null;
        if(mode.equals("front-background")){background=(ThreadLocal<Object>)DetailCalls1988.field(GpuResident1976.class,"BENCHMARK").get(null);background.set(Boolean.TRUE);}
        if(mode.equals("front-failure"))DetailCalls1988.field(Bitmap.class,"writeFailure").setBoolean(b,true);
        Throwable actual=null;
        try{run.invoke(null,b,plan(),4,true);}catch(InvocationTargetException error){actual=error.getCause();}
        finally{if(background!=null)background.remove();ProcessingTiming1947.restore(scope);}
        boolean failed=mode.equals("front-failure");
        DetailCalls1988.check(failed?actual instanceof IllegalStateException:actual==null,"front original exception behavior unchanged");
        if(!failed)DetailCalls1988.equal(reference.snapshot(),b.snapshot(),"front complete private bitmap");
        DetailCalls1988.untouched(unrelated,"unrelated thread scope");
        if(background!=null)DetailCalls1988.untouched(owner,"benchmark front processing");
        else{
            DetailCalls1988.selected(owner,PipelineDetail1988.FRONT_PROBE,0,1,PipelineDetail1988.CPU_FIXED);
            long committed=owner.backendCounts1982[0]-1;
            DetailCalls1988.check(failed?committed==0:committed>0,"existing committed strip counters preserved");
            DetailCalls1988.check(DetailCalls1988.count(owner,PipelineDetail1988.FRONT_STRIP,0)==(DetailCalls1988.OPTIONAL_FAILURE?0:committed),"PIPELINE_DETAIL_MISSING: front counts only committed writes");
            DetailCalls1988.check(DetailCalls1988.count(owner,PipelineDetail1988.FRONT_STRIP,1)==0,"CPU front success never appears GPU");
            if(!failed&&!DetailCalls1988.OPTIONAL_FAILURE)DetailCalls1988.check(DetailCalls1988.reason(owner,PipelineDetail1988.FRONT_STRIP,PipelineDetail1988.UNKNOWN)==committed,"front CPU reason remains honestly unobserved");
        }
        DetailCalls1988.check(Bitmap.writesAfterRecycle.get()==0,"front workers drain before disposing image");b.recycle();reference.recycle();
    }
    static void geometry(String mode)throws Exception{
        int sw=17,sh=25,w=23,h=35;Bitmap b=bitmap(sw,sh),expected=FastResize1933.resampleCpu1960(b,0,w,h);
        boolean gpu=mode.equals("geometry-gpu"),fallback=mode.equals("geometry-fallback");
        if(gpu||fallback)BackendDiagnostics1982Test.qualify(GpuNoise1960.ENV+"|geometry|"+sw+","+sh+",0,"+w+","+h);
        else GpuNoise1960.enabled=false;
        GpuNoise1960.readback=expected.snapshot();GpuNoise1960.fault=gpu?1:0;
        ProcessingTiming1947.Trace owner=DetailCalls1988.trace(),unrelated=DetailCalls1988.trace();ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(unrelated);
        ThreadLocal<Object> background=null;
        if(mode.equals("geometry-background")){background=(ThreadLocal<Object>)DetailCalls1988.field(GpuResident1976.class,"BENCHMARK").get(null);background.set(Boolean.TRUE);}
        Bitmap out=null;
        try{out=FastResize1933.resample1982(b,0,w,h,owner);}finally{if(background!=null)background.remove();ProcessingTiming1947.restore(scope);}
        DetailCalls1988.equal(expected.snapshot(),out.snapshot(),"geometry accepted/fallback all pixels");
        DetailCalls1988.untouched(unrelated,"geometry unrelated scope");
        if(background!=null)DetailCalls1988.untouched(owner,"background geometry");
        else{
            DetailCalls1988.selected(owner,PipelineDetail1988.CORRECTION_GEOMETRY,gpu?1:0,1,PipelineDetail1988.UNKNOWN);
            DetailCalls1988.check(owner.backendCounts1982[4+(gpu?1:0)]==1,"existing geometry accepted backend count unchanged");
        }
        if(gpu||fallback)DetailCalls1988.check(GpuNoise1960.openAttempts>0,"qualified geometry candidate actually attempted");
        out.recycle();expected.recycle();b.recycle();
    }
    static void correction(String mode)throws Exception{
        GpuNoise1960.enabled=false;boolean moire=!mode.equals("correction-sharp"),sharp=!mode.equals("correction-moire");
        boolean joined=moire&&sharp;int phase=joined?PipelineDetail1988.CORRECTION_JOINED:moire?PipelineDetail1988.CORRECTION_MOIRE:PipelineDetail1988.CORRECTION_SHARP;
        Bitmap reference=bitmap(33,65),b=bitmap(33,65);QualityPixels1932.Plan p=plan();
        if(joined){QualityPipeline1932.finishInPlace(reference,p,true,false);QualityPipeline1932.finishInPlace(reference,p,false,true);}
        else QualityPipeline1932.finishInPlace(reference,p,moire,sharp);
        ProcessingTiming1947.Trace owner=DetailCalls1988.trace(),unrelated=DetailCalls1988.trace();ProcessingTiming1947.bind(b,owner);
        ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(unrelated);Bitmap actual;
        try{actual=QualityPipeline1932.finishRoute1953(b,p,p,moire,sharp,joined);}finally{ProcessingTiming1947.restore(scope);}
        DetailCalls1988.check(actual==b,"CPU correction retains original owned result object");
        DetailCalls1988.equal(reference.snapshot(),actual.snapshot(),"individual/joined correction pixels and pass order");
        DetailCalls1988.selected(owner,phase,0,1,PipelineDetail1988.UNAVAILABLE);
        DetailCalls1988.check(owner.backendCounts1982[4]==1&&owner.backendCounts1982[5]==0,"aggregate correction CPU selection unchanged");
        DetailCalls1988.untouched(unrelated,"correction unrelated scope");
        if(joined)DetailCalls1988.check(DetailCalls1988.count(owner,PipelineDetail1988.CORRECTION_MOIRE,0)==0&&DetailCalls1988.count(owner,PipelineDetail1988.CORRECTION_SHARP,0)==0,"joined result counted once, not invented independent passes");
        actual.recycle();reference.recycle();
    }
    public static void main(String[] args)throws Exception{
        initialize();String mode=args[0];
        if(mode.startsWith("model"))model(mode.equals("model-fixed"));
        else if(mode.startsWith("front"))front(mode);
        else if(mode.startsWith("geometry"))geometry(mode);
        else if(mode.startsWith("correction"))correction(mode);
        else throw new AssertionError("unexpected route mode "+mode);
        GpuQualification1961.captureChanged();DetailCalls1988.report(mode);System.exit(0);
    }
}
