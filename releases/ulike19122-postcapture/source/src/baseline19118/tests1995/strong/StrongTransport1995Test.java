package com.hiro.ulike;

import android.content.SharedPreferences;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Real production stage/caller, private-output transport and native CPU/GLES.
 * Reflection starts a stage only after the real startup corpus passed. Android
 * preferences are an in-memory adapter; no image math or admission is mocked. */
public final class StrongTransport1995Test {
    static int assertions,cases;static long pixels;
    static final int SENTINEL=0x5a17394b;
    static final Map<String,String> preferences=new ConcurrentHashMap<String,String>();
    static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
    static Object field(Object owner,String name)throws Exception {Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    static void store()throws Exception{
        final Object[] editor=new Object[1];
        editor[0]=Proxy.newProxyInstance(StrongTransport1995Test.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){String n=m.getName();if(n.equals("putString"))preferences.put((String)a[0],(String)a[1]);else if(n.equals("remove"))preferences.remove(a[0]);else if(n.equals("clear"))preferences.clear();else if(n.equals("commit"))return true;else if(n.equals("apply"))return null;return editor[0];}});
        Object prefs=Proxy.newProxyInstance(StrongTransport1995Test.class.getClassLoader(),new Class[]{SharedPreferences.class},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){String n=m.getName();if(n.equals("edit"))return editor[0];if(n.equals("getString"))return preferences.containsKey(a[0])?preferences.get(a[0]):a[1];if(n.equals("contains"))return preferences.containsKey(a[0]);if(n.equals("getAll"))return preferences;return null;}});
        Field f=GpuCertificates1990.class.getDeclaredField("preferences");f.setAccessible(true);f.set(null,prefs);
        f=GpuQualification1961.class.getDeclaredField("base");f.setAccessible(true);f.set(null,"actual-host-private-output1995");
    }
    static Object stage(StrongNoise1958.Model model,boolean cancellable)throws Exception{
        Field c=GpuResident1976.class.getDeclaredField("CANCELLATION");c.setAccessible(true);
        @SuppressWarnings("unchecked") ThreadLocal<GpuQualification1961.Cancellation> local=(ThreadLocal<GpuQualification1961.Cancellation>)c.get(null);
        if(cancellable)local.set(new GpuQualification1961.Cancellation(){public boolean cancelled(){return GpuTransportFault1995.cancelled;}});
        Object value;
        try{Class<?> cls=Class.forName("com.hiro.ulike.GpuStrong1960$Stage");Constructor<?> constructor=cls.getDeclaredConstructor(StrongNoise1958.Model.class,boolean.class);constructor.setAccessible(true);value=constructor.newInstance(model,true);}
        finally{local.remove();}
        Field active=GpuStrong1960.class.getDeclaredField("active");active.setAccessible(true);check(active.get(null)==null,"previous stage released");active.set(null,value);
        GpuStrong1960.configureWorkers1978(model,2);return value;
    }
    static int[] confidence(GpuStrongDefault1990.Fixture f){return f.u[12]==0?null:new int[((f.u[0]+3)/4)*((f.u[3]-f.u[2]+3)/4)];}
    static int[] output(GpuStrongDefault1990.Fixture f){int[] out=new int[f.source.length+11];Arrays.fill(out,SENTINEL);return out;}
    static int[] expected(GpuStrongDefault1990.Fixture f,int[] cf){int[] out=output(f);int[] u=f.u;
        check(StrongNoise1958.gpuOracleSnapshot1961(f.source,out,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,f.model,f.policy,cf),"real CPU oracle");return out;}
    static boolean run(final GpuStrongDefault1990.Fixture f,int[] out,int[] cf){int[] u=f.u;
        return GpuStrong1960.process1981(f.source,out,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,f.model,f.policy,cf,null,null,new GpuStrong1960.Oracle(){public boolean run(int[] destination,int[] mask){throw new AssertionError("certified direct case unexpectedly chose CPU");}});}
    static void bankStorage(Object stage,int[] out)throws Exception{
        Object[][] banks=(Object[][])field(stage,"readbacks1975");int live=0;
        for(Object[] entries:banks)if(entries[0]!=null){live++;int[][] values=(int[][])field(entries[0],"values");
            check(values[0].length==0,"no Java ARGB bank allocation");for(int[] v:values)check(v!=out,"no bank retains worker output");
            Object result=field(entries[0],"result");check(field(result,"pixels")!=out,"result retains no output");}
        check(live>0,"real bank created");
        for(Field f:stage.getClass().getDeclaredFields()){f.setAccessible(true);check(f.get(stage)!=out,"no stage output reference: "+f.getName());}
    }
    static void same(int[] expected,int[] actual,String label){check(Arrays.equals(expected,actual),label);}
    static void corpus()throws Exception{
        for(int id=0;id<GpuStrongDefault1990.CASES;id++){
            GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(id);int[] cf=confidence(f),wantCf=confidence(f),want=expected(f,wantCf),original=f.source.clone();
            Object s=stage(f.model,false);
            try{
                for(int repeat=0;repeat<2;repeat++){
                    int[] actual=output(f);if(cf!=null)Arrays.fill(cf,0x61728394);
                    check(run(f,actual,cf),"direct corpus result "+id);same(want,actual,"whole destination incl halo/tail guards "+id);same(wantCf,cf,"confidence "+id);same(original,f.source,"immutable source "+id);
                    bankStorage(s,actual);cases++;pixels+=(long)f.u[0]*(f.u[3]-f.u[2]);
                }
                check(((Integer)field(s,"directStrips1995"))==2,"only two successful direct commits");
                check(((Long)field(s,"directPixels1995"))==2L*f.u[0]*(f.u[3]-f.u[2]),"direct pixel accounting");
            }finally{GpuStrong1960.endStage(f.model);}
            check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"drained corpus stage "+id);
        }
    }
    static void aliases()throws Exception{
        GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(63);int[] u=f.u,output=output(f),cf=confidence(f);
        check(GpuStrong1960.directTargets1995(f.source,output,u,f.policy,cf),"valid private target");
        check(!GpuStrong1960.directTargets1995(f.source,f.source,u,f.policy,cf),"reject in-place pixels");
        check(!GpuStrong1960.directTargets1995(f.source,output,u,output,cf),"reject output-policy alias");
        check(!GpuStrong1960.directTargets1995(f.source,output,u,f.policy,output),"reject output-confidence alias");
        check(!GpuStrong1960.directTargets1995(f.source,output,u,f.source,cf),"reject source-policy alias");
        check(!GpuStrong1960.directTargets1995(f.source,output,u,f.policy,f.source),"reject source-confidence alias");
        check(!GpuStrong1960.directTargets1995(f.source,output,u,f.policy,f.policy),"reject policy-confidence alias");
        check(!GpuStrong1960.directTargets1995(f.source,new int[f.source.length-1],u,f.policy,cf),"reject truncated worker destination");
        check(!GpuStrong1960.directTargets1995(new int[f.source.length-1],output,u,f.policy,cf),"reject truncated source");
        check(!GpuStrong1960.directTargets1995(f.source,output,u,new int[f.policy.length-1],cf),"reject short policy");
        check(!GpuStrong1960.directTargets1995(f.source,output,u,f.policy,new int[cf.length-1]),"reject short confidence");
        for(int bad:new int[]{-1,0,Integer.MAX_VALUE}){int[] badU=u.clone();badU[0]=bad;check(!GpuStrong1960.directTargets1995(f.source,output,badU,f.policy,cf),"reject invalid/overflow width");}
    }
    static void faults()throws Exception{
        final GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(63);final int[] u=f.u;
        check(StrongNoise1958.nativeAvailable(),"actual native CPU fallback loaded");int[] wantCf=confidence(f),want=expected(f,wantCf),original=f.source.clone();
        int alpha=0,zero=0;for(int i=0;i<u[0]*(u[3]-u[2]);i++){if((f.source[u[2]*u[0]+i]>>>24)!=255)alpha++;if(f.policy[i*2]==0)zero++;}
        check(alpha>0&&zero>0,"fault corpus covers alpha and zero-budget passthrough");
        StrongNoise1958.Protection protection=new StrongNoise1958.Protection(){public int budgetQ8(int x,int y){return f.policy[((y-u[6]-u[2])*u[0]+x)*2];}public int detailQ8(int x,int y){return f.policy[((y-u[6]-u[2])*u[0]+x)*2+1];}};
        for(int mode=1;mode<=5;mode++){
            Map<String,String> saved=new HashMap<String,String>(preferences);int[] actual=output(f);Object s=stage(f.model,true);StrongNoise1958.Workspace workspace=new StrongNoise1958.Workspace(true);
            GpuTransportFault1995.arm(mode);boolean cancelled=false;
            try{
                try{StrongNoise1958.processRange(f.source,actual,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,f.model,protection,workspace);}
                catch(CancellationException expected){cancelled=true;}
                check(GpuTransportFault1995.fired==1,"real JNI read fault fired once "+mode);
                check(cancelled==(mode==5),"only cancellation escapes unpublished caller "+mode);
                if(mode!=5){same(want,actual,"full core overwritten after partially corrupted GPU destination "+mode);same(wantCf,workspace.confidence(),"fallback confidence "+mode);}
                same(original,f.source,"fault leaves source untouched "+mode);check(((Integer)field(s,"directStrips1995"))==0,"failed/cancelled read never counted as committed "+mode);bankStorage(s,actual);
            }finally{GpuTransportFault1995.clear();preferences.clear();preferences.putAll(saved);workspace.close();GpuStrong1960.endStage(f.model);}
            check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"fault drains tickets and leases "+mode);
        }
    }
    static void concurrent()throws Exception{
        final GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(63);final int[][] actual={output(f),output(f)},cf={confidence(f),confidence(f)};
        int[] wantCf=confidence(f),want=expected(f,wantCf);Object s=stage(f.model,false);final Throwable[] error=new Throwable[2];
        GpuTransportFault1995.both=new CountDownLatch(2);GpuTransportFault1995.release=new CountDownLatch(1);
        Thread[] workers=new Thread[2];
        try{
            for(int i=0;i<2;i++){final int n=i;workers[i]=new Thread(new Runnable(){public void run(){try{if(!StrongTransport1995Test.run(f,actual[n],cf[n]))throw new AssertionError("concurrent GPU result unavailable");}catch(Throwable t){error[n]=t;}}});workers[i].start();}
            check(GpuTransportFault1995.both.await(10,TimeUnit.SECONDS),"two real tickets concurrently awaiting readback");
            boolean[] banks=(boolean[])field(s,"banks");check(banks[0]&&banks[1],"distinct bank leases both live");GpuTransportFault1995.release.countDown();
            for(Thread t:workers){t.join(15000);check(!t.isAlive(),"concurrent worker finished");}
            for(int i=0;i<2;i++){if(error[i]!=null)throw new AssertionError("worker "+i,error[i]);same(want,actual[i],"private concurrent output "+i);same(wantCf,cf[i],"private concurrent confidence "+i);bankStorage(s,actual[i]);}
            check(((Integer)field(s,"directStrips1995"))==2,"both direct commits counted");
        }finally{GpuTransportFault1995.release.countDown();for(Thread t:workers)if(t!=null)t.join(15000);GpuTransportFault1995.both=null;GpuTransportFault1995.release=null;GpuStrong1960.endStage(f.model);}
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"two-bank stage drained");
    }
    public static void main(String[] args)throws Exception{
        store();check(StrongNoise1958.nativeAvailable(),"real native CPU loaded");check(GpuStrongDefault1990.qualify(GO),"actual startup corpus exact pass: "+GpuStrongDefault1990.summary());
        check(GpuStrongDefault1990.ready(),"real startup certificate restored");
        if(args.length==0){corpus();aliases();}else{faults();concurrent();}
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"direct_cases\":"+cases+",\"compared_pixels\":"+pixels+",\"fault_modes\":"+(args.length==0?0:5)+",\"actual_jni\":true,\"actual_native_cpu\":true,\"physical_android_tested\":false}");
    }
}
