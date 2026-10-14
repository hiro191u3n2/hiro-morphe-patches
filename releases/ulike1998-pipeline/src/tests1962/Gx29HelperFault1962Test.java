package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

/** Execute unchanged helper probes with controlled transport responses. This
 * checks their classification/fallback paths, not GPU shader or device speed. */
public final class Gx29HelperFault1962Test {
    private static long assertions;
    private static int cases;
    private static final int W=8,H=8;
    private static final int[] INPUT=new int[W*H];
    private static final GpuQualification1961.Cancellation TOKEN=new GpuQualification1961.Cancellation(){
        public boolean cancelled(){return GpuNoise1960.cancelled;}
    };
    private static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    private static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static Object value(Class<?> type,String name)throws Exception{return field(type,name).get(null);}
    private static Object make(String name,Object... args)throws Exception{
        Class<?> type=Class.forName("com.hiro.ulike."+name);
        for(Constructor<?> c:type.getDeclaredConstructors())if(c.getParameterTypes().length==args.length){c.setAccessible(true);return c.newInstance(args);}
        throw new AssertionError("constructor missing "+name);
    }
    private static Object call(Class<?> type,String name,Class<?>[] types,Object... args)throws Exception{
        Method m=type.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);
    }
    private static Object failure(String key)throws Exception{
        Object env=call(GpuQualification1961.class,"environment",new Class<?>[0]);
        return call(GpuQualification1961.class,"failure",new Class<?>[]{String.class,String.class},key,env);
    }
    private static void clear()throws Exception{
        Object lock=value(GpuQualification1961.class,"LOCK");
        synchronized(lock){((Map<?,?>)value(GpuQualification1961.class,"FAILURES")).clear();((Map<?,?>)value(GpuQualification1961.class,"RECORDS")).clear();}
        field(GpuQualification1961.class,"base").set(null,"actual-helper-fault1962");
    }
    private static void classified(String key,String mode)throws Exception{
        cases++;
        check(GpuNoise1960.opens>0,key+" entered actual candidate transport");
        if("slow".equals(mode)){
            check(!GpuQualification1961.exactRejected(key),key+" slow is not exact failure");
            check(GpuQualification1961.restore(key)==null&&!GpuQualification1961.maySchedule(key),key+" slow invalidates proof and cools down");
            Object f=failure(key);check(f!=null,key+" speed record stored");
            field(f.getClass(),"retryAfter").setLong(f,System.nanoTime()-1);
            check(GpuQualification1961.maySchedule(key),key+" cooldown permits fresh proof");
        }else if("null".equals(mode)||"runtime".equals(mode)||"linkage".equals(mode)){
            check(GpuQualification1961.exactRejected(key)&&!GpuQualification1961.maySchedule(key),key+" hard candidate failure excluded");
            check(GpuQualification1961.restore(key)==null,key+" hard failure never certifies");
        }else{
            check(failure(key)==null&&!GpuQualification1961.exactRejected(key)&&GpuQualification1961.maySchedule(key),key+" transient/cancel neutral");
            check(GpuQualification1961.restore(key)==null,key+" neutral failure never certifies");
        }
        check(GpuNoise1960.closes==("defer".equals(mode)||"oom".equals(mode)?0:GpuNoise1960.opens),key+" all opened candidate sessions closed");
    }
    private static SingleNoise1955.Model model(){
        return SingleNoise1955.probe(new SingleNoise1955.Patches(){public void read(int[] out,int x,int y,int w,int h){
            for(int row=0;row<h;row++)System.arraycopy(INPUT,(y+row)*W+x,out,row*w,w);
        }},W,H);
    }
    private static GpuQualification1961.Probe probe(String helper,String key,int[] expected,SingleNoise1955.Model model)throws Exception{
        if("policy".equals(helper))return (GpuQualification1961.Probe)make("GpuPolicy1960$PolicyProbe",key,INPUT.clone(),W,H,expected,null,1L);
        if("single".equals(helper))return (GpuQualification1961.Probe)make("GpuSingle1960$SingleProbe",key,INPUT.clone(),expected,W,H,0,H,0,H,0,1,false,model,null,-8,3,4,12,W*H,11904L);
        if("protection".equals(helper))return (GpuQualification1961.Probe)make("GpuProtection1961$ProtectionProbe",key,new int[][]{INPUT,new int[]{0},new int[W*H],new int[]{0}},null,new int[32],null,expected,1L);
        Object state=make("GpuGeometry1960$State",GpuNoise1960.fingerprint(),W,H,180,W,H);
        Bitmap owned=Bitmap.from(W,H,INPUT,Bitmap.Config.ARGB_8888,false);
        // Anonymous production idle probe owns its state and immutable snapshot.
        return (GpuQualification1961.Probe)make("GpuGeometry1960$1",state,owned);
    }
    private static void probes()throws Exception{
        SingleNoise1955.Model model=model();
        for(String helper:new String[]{"policy","single","protection","geometry"}){
            int[] expected;
            if("policy".equals(helper))expected=(int[])call(GpuPolicy1960.class,"pyramidReference",new Class<?>[]{int[].class,int.class,int.class},INPUT,W,H);
            else if("protection".equals(helper))expected=new int[W*H*2];
            else expected=INPUT.clone();
            for(String mode:new String[]{"slow","null","runtime","linkage","defer","oom","cancel"}){
                clear();GpuNoise1960.reset(mode,expected);
                String key="geometry".equals(helper)?GpuNoise1960.fingerprint()+"|geometry|8,8,180,8,8":"helper-"+helper+"-"+mode;
                GpuQualification1961.Probe p=probe(helper,key,expected,model);
                try{p.run(TOKEN);}finally{p.close();}
                classified(key,mode);
                for(int pixel:INPUT)check(pixel==0xff807060,helper+" probe preserves source");
            }
        }
    }
    private static void foregroundFallbacks()throws Exception{
        SingleNoise1955.Model model=model();
        String singleKey=GpuNoise1960.fingerprint()+"|single|8,8,8,0,8,0,8,0,1,false,"+model.columns+","+model.rows+","+model.gpuPatchWidth1960()+","+model.gpuPatchHeight1960()+",0";
        for(String mode:new String[]{"null","runtime","linkage","defer","oom"}){
            clear();GpuNoise1960.reset(mode,INPUT);GpuQualification1961.qualified(singleKey,1000000000L,1L,0);
            int[] output=new int[INPUT.length];Arrays.fill(output,0x12345678);
            boolean complete=GpuSingle1960.process(INPUT,output,W,H,0,H,0,H,0,1,false,model,null);
            check(!complete,"single "+mode+" asks caller for original CPU fallback");
            for(int p:output)check(p==0x12345678,"single "+mode+" cannot commit partial candidate");
            SingleNoise1955.processCpuRange(INPUT,output,W,H,0,H,0,H,0,1,false,model,null);
            check(Arrays.equals(output,INPUT),"single "+mode+" original fallback output preserved");
        }
        final Bitmap source=Bitmap.from(W,H,INPUT,Bitmap.Config.ARGB_8888,false);
        String geometryKey=GpuNoise1960.fingerprint()+"|geometry|8,8,180,8,8";
        for(String mode:new String[]{"null","runtime","linkage","defer","oom"}){
            clear();GpuNoise1960.reset(mode,INPUT);GpuQualification1961.qualified(geometryKey,1000000000L,1L,0);
            final int[] calls={0};final Bitmap expected=Bitmap.from(W,H,INPUT,Bitmap.Config.ARGB_8888,true);
            Bitmap out=GpuGeometry1960.resample(source,180,W,H,new GpuGeometry1960.Cpu(){public Bitmap run(){calls[0]++;return expected;}});
            check(out==expected&&calls[0]==1,"geometry "+mode+" returns exactly one original CPU result");
            check(Arrays.equals(source.snapshot(),INPUT)&&!source.isRecycled(),"geometry "+mode+" preserves original photograph");out.recycle();
        }
        source.recycle();
    }
    public static void main(String[] args)throws Exception{
        Arrays.fill(INPUT,0xff807060);probes();foregroundFallbacks();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"helper_probe_cases\":"+cases+",\"foreground_fallback_cases\":10,\"actual_helper_paths_checked\":true,\"hard_failure_rejection_verified\":true,\"slow_retry_verified\":true,\"deferred_oom_cancel_neutral\":true,\"productionArithmeticChanged\":false,\"controlled_transport\":true,\"gpu_shader_execution\":false,\"physicalAndroidTested\":false}");
    }
}
