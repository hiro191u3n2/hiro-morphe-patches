package com.hiro.ulike;

import android.content.Context;
import android.graphics.Bitmap;
import java.lang.reflect.*;
import java.util.*;

/** Real CPU mathematics and production dispatch with explicit test readback. */
public final class BackendDiagnostics1982Test {
    static long assertions;
    static void check(boolean okay,String why){assertions++;if(!okay)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception {Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static void same(int[] expected,int[] actual,String why){check(expected.length==actual.length,why+" shape");for(int i=0;i<expected.length;i++)check(expected[i]==actual[i],why+" pixel "+i);}
    static int[] image(int w,int h){int[] p=new int[w*h];Random r=new Random(1982);for(int i=0;i<p.length;i++){int g=112+r.nextInt(51)-25;p[i]=0xff000000|g<<16|g<<8|g;}return p;}
    static SingleNoise1955.Model model(final int[] p,final int w,final int h){return SingleNoise1955.probe1978(new SingleNoise1955.Patches(){public void read(int[] a,int x,int y,int width,int height){for(int n=0;n<height;n++)System.arraycopy(p,(y+n)*w+x,a,n*width,width);}},w,h,false);}
    static ProcessingTiming1947.Trace trace(){return new ProcessingTiming1947.Trace(1982,System.currentTimeMillis(),System.nanoTime());}
    static QualityPixels1932.Plan plan(){return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(3,4,112,.2f,100),200,10000000L,2,.8f,4,2,true,true,1);}
    static void qualify(String key){GpuQualification1961.qualified(key,1000000000L,1,0);check(GpuQualification1961.restore(key)!=null,"test-local transport certificate");}
    static void single(String mode)throws Exception {
        final boolean residual=mode.startsWith("residual");GpuNoise1960.single=!residual;
        final int w=33,h=97,begin=7,end=80;int[] p=image(w,h),pristine=p.clone();SingleNoise1955.Model model=model(p,w,h);
        check(model.meanSigma()>=.30f,"nontrivial noise model");
        int[] expected=new int[p.length];Arrays.fill(expected,0x13579bdf);SingleNoise1955.processCpuRange(p,expected,w,h,begin,end,0,h,0,4,true,model,null);
        SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();int[] out=new int[p.length];Arrays.fill(out,0x13579bdf);
        try {
            SingleNoise1955.processRange(p,out,w,h,begin,end,0,h,0,4,true,model,null,workspace);
            check(workspace.selectedBackend1982==0,"true facade result is actually CPU on cold path");same(expected,out,"cold exact output");
            GpuQualification1961.captureChanged();
            Map<?,?> gates=(Map<?,?>)field(residual?GpuResidual1961.class:GpuSingle1960.class,"GATES").get(null);
            check(gates.size()==1,"one production geometry key");String key=(String)gates.keySet().iterator().next();qualify(key);
            String reference=SingleCpu1981.referenceKey(key,w,h,begin,end,0,h,0,4,true,model,null);qualify(SingleCpu1981.speedKey(reference,false));
            GpuNoise1960.readback=Arrays.copyOfRange(expected,begin*w,end*w);
            GpuNoise1960.fault=mode.endsWith("gpu")?1:mode.endsWith("link")?3:0;
            int attempts=GpuNoise1960.openAttempts;Arrays.fill(out,0x13579bdf);workspace.selectedBackend1982=1;
            SingleNoise1955.processRange(p,out,w,h,begin,end,0,h,0,4,true,model,null,workspace);
            check(GpuNoise1960.openAttempts>attempts,"admitted GPU was actually attempted");
            check(workspace.selectedBackend1982==(mode.endsWith("gpu")?1:0),"only accepted GPU output has GPU marker");
            same(expected,out,"accepted/fallback all pixels");same(pristine,p,"input unchanged");
            check(!field(SingleNoise1955.Workspace.class,"inUse").getBoolean(workspace),"caller workspace released");
        }finally{workspace.close();GpuQualification1961.captureChanged();}
    }
    static void worker(String mode)throws Exception {
        GpuNoise1960.enabled=false;int w=33,h=65;int[] p=image(w,h);Bitmap b=Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);
        ProcessingTiming1947.Trace owner=trace(),unrelated=trace();ProcessingTiming1947.bind(b,owner);ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(unrelated);
        ThreadLocal<Object> gate=null;
        if(mode.equals("worker-background")||mode.equals("worker-oracle")){
            gate=(ThreadLocal<Object>)field(GpuResident1976.class,mode.endsWith("oracle")?"CPU":"BENCHMARK").get(null);gate.set(Boolean.TRUE);
        }
        if(mode.equals("worker-write-failure"))field(Bitmap.class,"writeFailure").setBoolean(b,true);
        Method run=QualityPipeline1932.class.getDeclaredMethod("singleNoiseInPlace1955",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class);run.setAccessible(true);
        boolean failed=false;try{run.invoke(null,b,plan(),4,true);}catch(InvocationTargetException failure){failed=true;check(mode.equals("worker-write-failure"),"only requested write fault fails");}
        finally{if(gate!=null)gate.remove();ProcessingTiming1947.restore(scope);}
        check(failed==mode.equals("worker-write-failure"),"write failure behavior preserved");
        check(unrelated.backendCounts1982[0]==0&&unrelated.backendCounts1982[1]==0,"thread's unrelated trace untouched");
        if(gate!=null)check(owner.backendCounts1982[0]==0&&owner.backendCounts1982[1]==0,"benchmark/reference work not attached to capture");
        else if(failed)check(owner.backendCounts1982[0]==1&&owner.backendCounts1982[1]==0,"failed Bitmap write never commits strip count (probe only)");
        else check(owner.backendCounts1982[0]>1&&owner.backendCounts1982[1]==0,"worker output is attributed to bound capture after write");
        check(Bitmap.writesAfterRecycle.get()==0,"workers drain before disposal");b.recycle();
    }
    static void geometry(String mode)throws Exception {
        int sw=17,sh=25,w=23,h=35;int[] p=image(sw,sh);Bitmap b=Bitmap.from(sw,sh,p,Bitmap.Config.ARGB_8888,true);b.setHasAlpha(false);
        Bitmap expected=FastResize1933.resampleCpu1960(b,0,w,h);ProcessingTiming1947.Trace owner=trace();
        if(!mode.endsWith("cpu"))qualify(GpuNoise1960.ENV+"|geometry|"+sw+","+sh+",0,"+w+","+h);
        else GpuNoise1960.enabled=false;
        GpuNoise1960.readback=expected.snapshot();GpuNoise1960.fault=mode.endsWith("gpu")?1:0;
        Bitmap out=FastResize1933.resample1982(b,0,w,h,owner);same(expected.snapshot(),out.snapshot(),"geometry exact output");
        int selected=mode.endsWith("gpu")?1:0;check(owner.backendCounts1982[4+selected]==1&&owner.backendCounts1982[5-selected]==0,"geometry reports returned CPU fallback or GPU result");
        check(owner.backendTimed1982[4+selected],"geometry selected work timed");out.recycle();expected.recycle();b.recycle();
    }
    static void regions(String mode)throws Exception {
        final int w=17,h=25;final int[] p=image(w,h);final float[] reference={2.125f,3.25f,.15625f};int[] selected={-1};
        if(!mode.endsWith("cpu"))qualify("regions1961:v2:"+w+":"+h);else GpuNoise1960.enabled=false;
        GpuNoise1960.readback=new int[reference.length];for(int i=0;i<reference.length;i++)GpuNoise1960.readback[i]=Float.floatToRawIntBits(reference[i]);
        GpuNoise1960.fault=mode.endsWith("gpu")?1:0;
        float[] result=GpuAnalysis1961.regions1982(new StrongNoise1958.Patches(){public void read(int[] a,int x,int y,int width,int height){for(int n=0;n<height;n++)System.arraycopy(p,(y+n)*w+x,a,n*width,width);}},w,h,new float[16],new GpuAnalysis1961.RegionsCpu(){public float[] compute(StrongNoise1958.Patches ignored){return reference;}},selected);
        check(selected[0]==(mode.endsWith("gpu")?1:0),"region result reports actual selected implementation");
        check(result.length==reference.length,"regional shape preserved");for(int i=0;i<result.length;i++)check(Float.floatToRawIntBits(result[i])==Float.floatToRawIntBits(reference[i]),"region raw bits "+i);
    }
    static void modelPreparation()throws Exception {
        GpuNoise1960.enabled=false;final int w=33,h=65;final int[] p=image(w,h);
        StrongNoise1958.Patches reader=new StrongNoise1958.Patches(){public void read(int[] a,int x,int y,int width,int height){for(int n=0;n<height;n++)System.arraycopy(p,(y+n)*w+x,a,n*width,width);}};
        StrongNoise1958.Model before=StrongNoise1958.prepare(reader,w,h,4,true);StrongNoise1958.Preparation1982 record=new StrongNoise1958.Preparation1982();
        StrongNoise1958.Model after=StrongNoise1958.prepare1982(reader,w,h,4,true,null,record);
        check(StrongNoise1958.sameModel1961(before,after),"diagnostics preserve all model maps and raw evidence bits");check(record.cpuUnits==3&&record.gpuUnits==0,"CPU source, regions and maps separately observed");check(record.cpuNanos>0,"model selected work measured");
    }
    static void defaults()throws Exception {
        ProcessingTiming1947.Trace t=trace();PipelineDiagnostics1982.defaultRoute(t);check(t.correctionRoute1976==0,"ordinary completion explicitly filled");
        for(int route=1;route<=4;route++){ProcessingTiming1947.detailRoute1976(t,route);PipelineDiagnostics1982.defaultRoute(t);check(t.correctionRoute1976==route,"accepted GPU route remains "+route);}
        t.state=1;PipelineDiagnostics1982.selected(t,0,1,1,100);check(t.backendCounts1982[1]==0,"completed capture cannot be changed");
    }
    public static void main(String[] args)throws Exception {
        String mode=args[0];if(args.length>1)System.load(args[1]);GpuNoise1960.enabled=true;GpuNoise1960.fault=0;
        GpuQualification1961.initialize(new Context());field(SingleNoise1955.class,"nativeState").setInt(null,-1);field(StrongNoise1958.class,"nativeState").setInt(null,-1);
        if(mode.startsWith("single")||mode.startsWith("residual"))single(mode);else if(mode.startsWith("worker"))worker(mode);else if(mode.startsWith("geometry"))geometry(mode);else if(mode.startsWith("regions"))regions(mode);else if(mode.equals("model"))modelPreparation();else defaults();
        GpuQualification1961.captureChanged();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+"}");System.exit(0);
    }
}
