package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

/** Faults cross the real production adapters and the real persistent queue. */
public final class GpuFailure1980Test {
    static int assertions;static final int W=16,H=16;
    static final int[] source=new int[W*H];
    static SingleNoise1955.Model single;static StrongNoise1958.Model strong;
    static Bitmap bitmap;static String family,key;
    static void check(boolean okay,String message){assertions++;if(!okay)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Object value(Object object,String name)throws Exception{return field(object.getClass(),name).get(object);}
    static SpatialNoise1934.Patches spatialReader(){return new SpatialNoise1934.Patches(){public void read(int[] a,int x,int y,int w,int h){readPixels(a,x,y,w,h);}};}
    static StrongNoise1958.Patches strongReader(){return new StrongNoise1958.Patches(){public void read(int[] a,int x,int y,int w,int h){readPixels(a,x,y,w,h);}};}
    static void readPixels(int[] a,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(source,(y+row)*W+x,a,row*w,w);}
    static void setup(String selected)throws Exception{
        family=selected;Random random=new Random(1980);
        for(int i=0;i<source.length;i++){int y=96+random.nextInt(64);source[i]=0xff000000|y<<16|(y+7)<<8|y-9;}
        single=SingleNoise1955.probe1978(new SingleNoise1955.Patches(){public void read(int[] a,int x,int y,int w,int h){readPixels(a,x,y,w,h);}},W,H,false);
        strong=StrongNoise1958.prepareJava(strongReader(),W,H,2,false);
        bitmap=Bitmap.from(W,H,source,Bitmap.Config.ARGB_8888,false);
        GpuNoise1960.enabled=true;GpuNoise1960.single=!"residual".equals(family);
        GpuQualification1961.initialize(new Context());
        key=key();
    }
    static String key(){
        if(family.equals("single"))return GpuNoise1960.ENV+"|single|"+W+","+H+","+H+",0,"+H+",0,"+H+",0,2,false,"+single.columns+","+single.rows+","+single.gpuPatchWidth1960()+","+single.gpuPatchHeight1960()+",0";
        if(family.equals("residual"))return GpuNoise1960.ENV+"|residual1964-cache-buffer|"+W+","+H+",0,"+H+",0,"+H+",0,2,false,"+H+","+single.columns+","+single.rows+","+single.gpuPatchWidth1960()+","+single.gpuPatchHeight1960()+",0";
        if(family.equals("spatial"))return "spatial1961:v2:"+W+":"+H;
        if(family.equals("regions"))return "regions1961:v2:"+W+":"+H;
        if(family.equals("resident"))return "resident1961:v2:"+W+":"+H+":2:false";
        if(family.equals("protection"))return "protection1962:face:"+W+":"+H+":1";
        if(family.equals("geometry"))return GpuNoise1960.ENV+"|geometry|"+W+","+H+",90,"+W+","+H;
        if(family.equals("chain"))return GpuNoise1960.ENV+"|moire-geometry|"+W+","+H+",90,"+W+","+H;
        if(family.equals("pyramid"))return "pyramid1961:bg:v2:"+W+":"+H;
        if(family.equals("evidence"))return "evidence1961:bg:v2:"+W+":"+H;
        throw new AssertionError(family);
    }
    static Object invoke(){
        if(family.equals("single")||family.equals("residual")){
            int[] out=new int[source.length];Arrays.fill(out,0x34567890);
            SingleNoise1955.processRange(source,out,W,H,0,H,0,H,0,2,false,single,null);return out;
        }
        if(family.equals("spatial"))return GpuAnalysis1961.spatial(spatialReader(),W,H);
        if(family.equals("regions"))return GpuAnalysis1961.regions(strongReader(),W,H,StrongNoise1958.gpuEvidence1960(strong),new GpuAnalysis1961.RegionsCpu(){public float[] compute(StrongNoise1958.Patches p){return StrongNoise1958.estimateRegionsSnapshot1961(p,W,H,StrongNoise1958.gpuEvidence1960(strong));}});
        if(family.equals("resident")){
            int[] half=new int[W*H/4];for(int y=0;y<H/2;y++)for(int x=0;x<W/2;x++)half[y*(W/2)+x]=source[y*2*W+x*2];
            float[] ev=StrongNoise1958.gpuEvidence1960(strong);
            StrongNoise1958.Model got=GpuAnalysis1961.residentIfQualified(half,W,H,2,false,ev,ev);
            if(got==null)GpuAnalysis1961.scheduleResident(half,W,H,2,false,ev,ev,StrongNoise1958.preparedSnapshotCpu1961(half,W,H,2,false,ev,ev),1000000000L);
            else StrongNoise1958.discardResident1961(got);return got;
        }
        if(family.equals("protection")){
            int[] mask=new int[source.length];Arrays.fill(mask,0x10000|96);
            return GpuProtection1961.faceRaster(source,W,H,new int[][]{mask});
        }
        if(family.equals("geometry"))return GpuGeometry1960.resample(bitmap,90,W,H,new GpuGeometry1960.Cpu(){public Bitmap run(){return FastResize1933.resampleCpu1960(bitmap,90,W,H);}});
        if(family.equals("chain"))return GpuChain1961.moireGeometry(bitmap,90,W,H);
        if(family.equals("pyramid"))return GpuPolicy1960.gpuPyramid(source,W,H);
        return GpuPolicy1960.strongEvidence(source,W,H,new GpuPolicy1960.EvidenceCpu(){public float[] compute(){return StrongNoise1958.evidenceSnapshotCpu1961(source,W,H);}});
    }
    static void dispose(Object out){if(out instanceof Bitmap&&out!=bitmap)((Bitmap)out).recycle();}
    static void sameFallback(Object out,Object expected){
        if(out==null)return; // null explicitly delegates to the outer CPU owner.
        if(out instanceof int[])check(Arrays.equals((int[])out,(int[])expected),"fallback pixels exact");
        else if(out instanceof float[])check(CpuExact1978.same(out,expected),"fallback evidence bits exact");
        else if(out instanceof SpatialNoise1934)check(SpatialNoise1934.same1961((SpatialNoise1934)out,(SpatialNoise1934)expected),"fallback spatial values exact");
        else if(out instanceof Bitmap)check(GpuGeometry1960.equal1961((Bitmap)out,(Bitmap)expected),"fallback bitmap exact");
    }
    static boolean queued()throws Exception{
        Object lock=field(GpuQualification1961.class,"LOCK").get(null);
        synchronized(lock){for(Object job:(Collection<?>)field(GpuQualification1961.class,"QUEUED").get(null))if(key.equals(value(job,"key")))return true;}
        return false;
    }
    static void drain()throws Exception{
        field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);
        SaveQueue1935.idle=true;GpuQualification1961.wake();long until=System.nanoTime()+15000000000L;
        while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>until)throw new AssertionError("proof did not release");Thread.sleep(2);}
        SaveQueue1935.idle=false;check(GpuNoise1960.active==0&&GpuNoise1960.opens==GpuNoise1960.closes,"all GPU sessions released");
    }
    static void clearMemory()throws Exception{
        Object lock=field(GpuQualification1961.class,"LOCK").get(null);
        synchronized(lock){((Map<?,?>)field(GpuQualification1961.class,"RECORDS").get(null)).clear();((Map<?,?>)field(GpuQualification1961.class,"FAILURES").get(null)).clear();}
    }
    static void expireFailures()throws Exception{
        Object lock=field(GpuQualification1961.class,"LOCK").get(null);
        synchronized(lock){for(Object failure:((Map<?,?>)field(GpuQualification1961.class,"FAILURES").get(null)).values())field(failure.getClass(),"retryAfter").setLong(failure,System.nanoTime()-1);}
    }
    static int retries()throws Exception{
        Object lock=field(GpuQualification1961.class,"LOCK").get(null);int n=0;
        synchronized(lock){for(Object failure:((Map<?,?>)field(GpuQualification1961.class,"FAILURES").get(null)).values())n+=field(failure.getClass(),"retries").getInt(failure);}return n;
    }
    static void retryLimits()throws Exception{
        for(int i=0;i<3;i++){
            expireFailures();check(GpuQualification1961.maySchedule(key),"retry available after cooldown");
            check(GpuQualification1961.schedule(key,1,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){GpuQualification1961.rejectSpeed(key);}public void close(){}}),"bounded retry queued");
            drain();check(retries()==i+1,"one failed job spends one retry");
        }
        expireFailures();check(!GpuQualification1961.maySchedule(key),"three unsuccessful retries stop");
        check(!GpuQualification1961.exactRejected(key),"retry exhaustion is not false pixel evidence");
    }
    public static void main(String[] args)throws Exception{
        setup(args[0]);String scenario=args[1];int fault=Integer.parseInt(args[2]);boolean baseline=Boolean.parseBoolean(args[3]);
        int[] original=source.clone();Object expected=invoke();check(queued(),"foreground queued target proof: "+family);
        if(scenario.equals("probe")){
            GpuNoise1960.fault=fault;drain();
            boolean mismatch=fault==1;
            check(GpuNoise1960.operations>0,"real adapter crossed injected transport");
            check(GpuQualification1961.exactRejected(key)==(mismatch||baseline&&fault==0),"only real comparison is permanently rejected: "+family);
            if(mismatch){clearMemory();check(GpuQualification1961.exactRejected(key),"exact rejection survived clearing RAM");GpuQualification1961.qualified(key,100,1,0);check(GpuQualification1961.restore(key)==null,"saved mismatch cannot be overwritten");}
            else if(!baseline&&fault<4){check(!GpuQualification1961.maySchedule(key),"failure enters cooldown");retryLimits();}
            else if(fault>=4){check(GpuQualification1961.maySchedule(key),"cancellation consumes no cooldown");check(retries()==0,"cancellation consumes no retry");}
        }else{
            GpuQualification1961.captureChanged();check(GpuQualification1961.retainedBytes()==0,"queued ownership released before foreground certificate");
            GpuQualification1961.qualified(key,1000000000L,1,0);check(GpuQualification1961.restore(key)!=null,"same production key seeded with controlled proof");
            GpuNoise1960.fault=fault;GpuNoise1960.busy=scenario.equals("busy");Object out=null;
            try{out=invoke();sameFallback(out,expected);}finally{dispose(out);}
            check(!GpuQualification1961.exactRejected(key),"foreground transport/busy is not permanent mismatch");
            if(scenario.equals("busy")){check(GpuQualification1961.restore(key)!=null,"busy keeps existing valid proof");check(GpuNoise1960.operations==0,"busy emits no GPU command");}
            else{check(GpuQualification1961.restore(key)==null,"failed foreground proof returns to qualification");check(!GpuQualification1961.maySchedule(key),"foreground failure has cooldown");int count=GpuNoise1960.operations;out=invoke();dispose(out);check(GpuNoise1960.operations==count,"cooldown prevents repeated GPU submissions");}
            check(GpuNoise1960.active==0&&GpuNoise1960.opens==GpuNoise1960.closes,"failed foreground session closed");
        }
        check(Arrays.equals(original,source),"source image unchanged");dispose(expected);GpuQualification1961.captureChanged();bitmap.recycle();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"physical_android_tested\":false,\"family\":\""+family+"\",\"scenario\":\""+scenario+"\",\"fault\":"+fault+",\"baseline\":"+baseline+"}");
    }
}
