package com.hiro.ulike;

import android.content.SharedPreferences;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Untouched arithmetic, real asynchronous JNI and retained Strong session.
 * The source callback stays on the caller and never outlives prepare(). */
public final class ModelParallel1996Test {
    static int assertions,cases;static long words;
    static final Map<String,String> preferences=new HashMap<String,String>();
    static void check(boolean b,String s){assertions++;if(!b)throw new AssertionError(s);}
    static void field(Class<?> c,String name,Object v)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(null,v);}
    static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static void store()throws Exception{
        final Object[] editor=new Object[1];
        editor[0]=Proxy.newProxyInstance(ModelParallel1996Test.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){String n=m.getName();if(n.equals("putString"))preferences.put((String)a[0],(String)a[1]);else if(n.equals("remove"))preferences.remove(a[0]);else if(n.equals("clear"))preferences.clear();else if(n.equals("commit"))return true;else if(n.equals("apply"))return null;return editor[0];}});
        Object prefs=Proxy.newProxyInstance(ModelParallel1996Test.class.getClassLoader(),new Class[]{SharedPreferences.class},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){String n=m.getName();if(n.equals("edit"))return editor[0];if(n.equals("getString"))return preferences.containsKey(a[0])?preferences.get(a[0]):a[1];if(n.equals("contains"))return preferences.containsKey(a[0]);if(n.equals("getAll"))return preferences;return null;}});
        field(GpuCertificates1990.class,"preferences",prefs);field(GpuQualification1961.class,"base","real-host-model-parallel1996");
        check(GpuModelDefault1990.qualify(new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}}),"actual model startup corpus pass");
        field(GpuStartup1990.class,"state",Integer.valueOf(GpuStartup1990.READY));
        field(GpuStartup1990.class,"modelReady",Boolean.TRUE);field(GpuStartup1990.class,"sharedReleased1991",Boolean.TRUE);
    }
    static void owner(StrongNoise1958.Model model,float[] runtime)throws Exception{
        GpuAnalysis1961.Resident owner=StrongNoise1958.takeResident1961(model);
        check(owner!=null&&!owner.closed,"completed model transfers exactly one owner");
        try{
            check(StrongNoise1958.takeResident1961(model)==null,"lease cannot transfer twice");
            Object[] pending=(Object[])field(owner.session,"pending");check(pending[0]==null&&pending[1]==null,"model ticket joined before Strong handoff");
            int[] uploaded=owner.session.readInts(2,runtime.length);check(uploaded!=null,"runtime resident slot readable");
            for(int i=0;i<uploaded.length;i++)check(uploaded[i]==Float.floatToRawIntBits(runtime[i]),"runtime upload complete "+i);
        }finally{StrongNoise1958.attachResident1961(model,owner);}
    }
    static void corpus()throws Exception{
        Method parallel=GpuAnalysis1961.class.getDeclaredMethod("residentCandidate1996",int[].class,int.class,int.class,int.class,boolean.class,float[].class,float[].class,int.class,boolean.class,Runnable.class);parallel.setAccessible(true);
        for(int id=0;id<GpuModelDefault1990.CASES;id++){
            final GpuModelDefault1990.Fixture f=GpuModelDefault1990.fixture(id);
            StrongNoise1958.Model cpu=StrongNoise1958.preparedSnapshotCpu1961(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime);
            for(int variant=0;variant<3;variant++){
                final float[] runtime=new float[f.runtime.length];System.arraycopy(f.runtime,0,runtime,0,16);
                final int[] callbacks={0};final Thread caller=Thread.currentThread();StrongNoise1958.Model model=null;
                try{
                    model=(StrongNoise1958.Model)parallel.invoke(null,f.half,f.width,f.height,f.noise,f.shadows,f.evidence,runtime,variant,true,new Runnable(){public void run(){
                        callbacks[0]++;check(Thread.currentThread()==caller,"CPU callback remains on foreground owner");check(GpuNoise1960.sessionBusy(),"GPU ticket session remains alive during CPU work");System.arraycopy(f.runtime,16,runtime,16,f.runtime.length-16);
                    }});
                    check(model!=null&&StrongNoise1958.sameModel1961(cpu,model),"async full model bitwise equals CPU case="+id+" variant="+variant);
                    check(callbacks[0]==1,"one callback per graph");owner(model,f.runtime);
                    for(int[] map:StrongNoise1958.gpuMaps1960(model))words+=map.length;words+=64+runtime.length;cases++;
                }finally{if(model!=null)StrongNoise1958.discardResident1961(model);}
                check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"all async owners released");
            }
        }
    }
    static final class Source implements StrongNoise1958.Patches {
        final int width,height;final int[] pixels;final Thread owner=Thread.currentThread();int pendingReads,reads;boolean fail;
        Source(int w,int h,int seed){width=w;height=h;pixels=new int[w*h];for(int i=0;i<pixels.length;i++){int n=(i*1103515245+seed*12345);pixels[i]=(seed%3==0&&i%17==0?0x70000000:0xff000000)|(n&0xffffff);}}
        public void read(int[] dst,int x,int y,int w,int h){check(Thread.currentThread()==owner,"source never escapes caller");reads++;if(GpuNoise1960.sessionBusy()){pendingReads++;if(fail)throw new IllegalStateException("controlled source failure");}for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,dst,row*w,w);}
    }
    static void prepare()throws Exception{
        for(int id=0;id<12;id++){
            Source source=new Source(63+id%3,67+id%4,id+1);int[] before=source.pixels.clone();
            StrongNoise1958.Model oracle=StrongNoise1958.prepareJava(source,source.width,source.height,1+id%4,id%2==0);
            source.pendingReads=0;StrongNoise1958.Preparation1982 selected=new StrongNoise1958.Preparation1982();StrongNoise1958.Model model=null;
            try{
                model=StrongNoise1958.prepare1982(source,source.width,source.height,1+id%4,id%2==0,null,selected);
                check(StrongNoise1958.sameModel1961(oracle,model),"real source region/map parallel equals serial oracle "+id);
                check(source.pendingReads>0,"real region traversal runs inside pending model interval");check(selected.gpuUnits==1&&selected.cpuUnits==2,"source/regions CPU plus maps GPU actual backends");
                owner(model,StrongNoise1958.gpuEvidence1960(model));check(Arrays.equals(before,source.pixels),"source unchanged");cases++;
            }finally{if(model!=null)StrongNoise1958.discardResident1961(model);}
            check(!GpuNoise1960.sessionBusy(),"preparation releases ownership after consumption");
        }
    }
    static void admission()throws Exception{
        // Controlled existing record, after the real model corpus above passed.
        // This checks selection/ownership, not a device speed certificate.
        Source source=new Source(65,67,2);String regional="regions1961:v2:65:67";
        GpuQualification1961.qualified(regional,1000000L,900000L,0);check(GpuQualification1961.restore(regional)!=null,"controlled existing regional record installed");
        check(GpuAnalysis1961.regionsQualified1996(65,67),"existing regional GPU admission observed before opening model");
        StrongNoise1958.Preparation1982 selected=new StrongNoise1958.Preparation1982();StrongNoise1958.Model model=null;
        try{
            model=StrongNoise1958.prepare1982(source,65,67,4,true,null,selected);
            check(source.pendingReads==0,"existing regional GPU route does not become pending-ticket CPU work");
            check(selected.regionBackend[0]==1&&selected.modelBackend1988[1]!=PipelineDetail1988.CANDIDATE_FAILURE,"regional GPU route remains selected");
        }finally{if(model!=null)StrongNoise1958.discardResident1961(model);}
        check(!GpuNoise1960.sessionBusy(),"serial regional admission closes owner");
        final GpuModelDefault1990.Fixture fixture=GpuModelDefault1990.fixture(0);
        String key=GpuAnalysis1961.residentExactKey1991(fixture.width,fixture.height,fixture.noise,fixture.shadows);
        preferences.clear();GpuQualification1961.qualified(key,1000000L,900000L,0);check(GpuQualification1961.restore(key)!=null,"controlled shape record without standard corpus");
        final float[] runtime=fixture.runtime.clone();final int[] calls={0};
        model=GpuAnalysis1961.residentIfQualified1996(fixture.half,fixture.width,fixture.height,fixture.noise,fixture.shadows,
            fixture.evidence,runtime,new int[2],new Runnable(){public void run(){calls[0]++;runtime[16]=Float.NaN;}});
        try{check(model==null&&calls[0]==1,"completed runtime invalidates nonstandard exact candidate");}
        finally{if(model!=null)StrongNoise1958.discardResident1961(model);}
        check(!GpuNoise1960.sessionBusy(),"invalid completed runtime discards resident session");
    }
    static void faults()throws Exception{
        for(int mode=1;mode<=6;mode++){
            Source source=new Source(65,67,2);StrongNoise1958.Model oracle=StrongNoise1958.prepareJava(source,65,67,4,true);int[] original=source.pixels.clone();
            ModelParallelFault1996.mode=mode;ModelParallelFault1996.fired=0;source.pendingReads=0;StrongNoise1958.Model model=null;
            try{
                model=StrongNoise1958.prepare(source,65,67,4,true);
                check(mode!=5,"cancellation propagated");check(StrongNoise1958.sameModel1961(oracle,model),"complete CPU fallback after transport fault "+mode);
                check(StrongNoise1958.takeResident1961(model)==null,"failed model never carries resident lease");
            }catch(CancellationException cancelled){check(mode==5,"only cancellation mode aborts capture");}
            finally{Thread.interrupted();if(model!=null)StrongNoise1958.discardResident1961(model);}
            check(ModelParallelFault1996.fired==1,"fault fired once "+mode);check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"pending bank/session released fault "+mode);
            check(Arrays.equals(original,source.pixels),"transport failure preserves original source "+mode);
            check(source.pendingReads<=4,"fallback never recomputes completed 2x2 regions "+mode);
        }
        Source source=new Source(65,67,2);source.fail=true;boolean failed=false;
        try{StrongNoise1958.prepare(source,65,67,4,true);}catch(IllegalStateException expected){failed=true;}
        check(failed&&source.pendingReads==1,"source failure propagates without retrying traversal");check(!GpuNoise1960.sessionBusy(),"source failure drains model ticket");
    }
    public static void main(String[] args)throws Exception{
        check(StrongNoise1958.nativeAvailable(),"real CPU JNI loaded");store();
        if(args.length==0){corpus();prepare();admission();}else faults();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"model_cases\":"+cases+",\"compared_words\":"+words+",\"fault_modes\":"+(args.length==0?0:7)+",\"actual_jni\":true,\"actual_native_cpu\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
