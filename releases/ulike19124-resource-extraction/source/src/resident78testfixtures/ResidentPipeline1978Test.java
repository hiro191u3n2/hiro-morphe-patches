package com.hiro.ulike;

import android.graphics.Bitmap;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Current production JNI, with a separately written preserved CPU sequence:
 * Strong -> residual evidence -> moire -> sharp for the original joined route;
 * Strong -> moire -> residual evidence -> sharp after reduce-first geometry.
 * This reference never calls any new resident-plan or resident-graph helper. */
public final class ResidentPipeline1978Test {
    static long assertions,pixels,negativePixels;static int identityCases,reducedCases,streamCases;
    static final GpuQualification1961.Cancellation LIVE=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(Class<?> owner,String name)throws Exception{Field value=owner.getDeclaredField(name);value.setAccessible(true);return value;}
    @SuppressWarnings("unchecked")static <T> ThreadLocal<T> local(String name)throws Exception{return (ThreadLocal<T>)field(GpuResident1976.class,name).get(null);}
    static long dispatches(){return Native1960Test.faultFacts()[3];}
    static void released(String label)throws Exception {
        check(!GpuNoise1960.sessionBusy(),label+" session released");GpuNoise1960.trimIdle();
        check(GpuNoise1960.retainedBytes()==0&&GpuNoise1960.reservedBytes1971()==0,label+" native and reserved memory released");
        check(field(GpuStrong1960.class,"active").get(null)==null,label+" worker stage drained");
        synchronized(field(GpuQualification1961.class,"LOCK").get(null)){check(((Deque<?>)field(GpuQualification1961.class,"QUEUED").get(null)).isEmpty(),label+" no nested queue");}
    }
    static void equal(Bitmap expected,Bitmap actual,String label) {
        check(expected!=null&&actual!=null,label+" complete output");
        int[] a=expected.snapshot(),b=actual.snapshot();check(a.length==b.length,label+" shape");
        for(int i=0;i<a.length;i++){assertions++;pixels++;if(a[i]!=b[i])throw new AssertionError(label+" pixel "+i+" expected="+Integer.toHexString(a[i])+" actual="+Integer.toHexString(b[i]));}
    }
    static long difference(Bitmap a,Bitmap b){int[] x=a.snapshot(),y=b.snapshot();long result=0;for(int i=0;i<x.length;i++)if(x[i]!=y[i])result++;return result;}
    static QualityPixels1932.Plan plan(Bitmap source,boolean mask) {
        QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,source.getWidth(),source.getHeight(),0).withLocalNoise(Chain1962Oracle.noise(source),4);
        return mask?plan.withFaceRegions(new FaceRegions1934.Mask()):plan;
    }
    static Object smooth(Bitmap source,QualityPixels1932.Plan plan,int[] half)throws Exception {
        Method method=QualityPipeline1932.class.getDeclaredMethod("smoothNoiseInPlace1976",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class,int[].class,GpuNoise1960.Session[].class);method.setAccessible(true);
        local("CPU").set(Boolean.TRUE);
        try{return method.invoke(null,source,plan,4,true,half,null);}
        catch(InvocationTargetException failure){Throwable cause=failure.getCause();if(cause instanceof Exception)throw (Exception)cause;throw (Error)cause;}
        finally {local("CPU").remove();}
    }
    static Bitmap preserved(Bitmap source,QualityPixels1932.Plan sourcePlan,QualityPixels1932.Plan output,boolean beforeMoire)throws Exception {
        Bitmap image=source.copy(Bitmap.Config.ARGB_8888,true);boolean done=false;
        try {
            Object mask=smooth(image,sourcePlan,null);
            QualityPixels1932.Plan sharp=output.withSmoothedRegions((QualityPixels1932.SmoothMask)mask);
            if(beforeMoire)sharp=sharp.withOutputNoise(Chain1962Oracle.noise(image));
            QualityPipeline1932.finishInPlace(image,sourcePlan,true,false);
            if(!beforeMoire)sharp=sharp.withOutputNoise(Chain1962Oracle.noise(image));
            QualityPipeline1932.finishInPlace(image,sharp,false,true);done=true;return image;
        } finally {if(!done)image.recycle();}
    }
    static String finishKey(Bitmap source,QualityPixels1932.Plan plan,boolean refresh)throws Exception {
        Method key=GpuChain1961.class.getDeclaredMethod("finishKey1976",Bitmap.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);key.setAccessible(true);
        return (String)key.invoke(null,source,0,source.getWidth(),source.getHeight(),plan,refresh);
    }
    static GpuNoise1960.Session carried(Bitmap source) {
        GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"actual carried session");
        GpuNoise1960.Lease1971 lease=session.reserveCapacity1971(new int[]{24},new long[]{4L*source.getWidth()*source.getHeight()},0);
        try {check(lease!=null&&lease.revalidate1971()&&session.upload(24,source.snapshot()),"actual resident source upload");return session;}
        catch(Throwable error){session.close();throw error;}
        finally{if(lease!=null)lease.close();}
    }
    /** Exact two complete comparisons precede a controlled timing token. The
     * token only reaches the ordinary route on software Mesa; it says nothing
     * about performance on Android hardware. */
    static void qualifyOrdinary(Bitmap source,QualityPixels1932.Plan sourcePlan,QualityPixels1932.Plan output,boolean identity)throws Exception {
        Bitmap strong=source.copy(Bitmap.Config.ARGB_8888,true);
        try {
            Object smooth=smooth(strong,sourcePlan,null);Method outputMask=smooth.getClass().getDeclaredMethod("outputMask",int.class,int.class,int.class);outputMask.setAccessible(true);
            QualityPixels1932.Plan complete=output.withSmoothedRegions((QualityPixels1932.SmoothMask)outputMask.invoke(smooth,0,source.getWidth(),source.getHeight()));
            if(identity)complete=complete.withOutputNoise(Chain1962Oracle.noise(strong));
            for(int trial=0;trial<2;trial++) {
                Bitmap cpu=null,gpu=null;
                try {cpu=GpuChain1961.cpuFinish(strong,0,strong.getWidth(),strong.getHeight(),complete,!identity);gpu=GpuChain1961.runFinish1976(strong,0,strong.getWidth(),strong.getHeight(),complete,!identity,null,64);equal(cpu,gpu,"actual finish prerequisite "+trial);}
                finally {if(cpu!=null)cpu.recycle();if(gpu!=null)gpu.recycle();}
            }
            if(identity) {
                SpatialNoise1934 expected=Chain1962Oracle.noise(strong);GpuNoise1960.Session session=carried(strong);
                try {SpatialNoise1934 actual=GpuAnalysis1961.residentSpatial1962(session,24,strong.getWidth(),strong.getHeight());check(SpatialNoise1934.same1961(expected,actual),"resident evidence is exactly post-Strong pre-moire CPU evidence");}
                finally {session.close();}
            }
            GpuQualification1961.qualified(finishKey(strong,complete,!identity),1000000000000L,1000000L,1);
            check(GpuChain1961.residentBaseline1978(source,0,source.getWidth(),source.getHeight(),output,identity)!=null,"exact ordinary identity dependency available");
        } finally {strong.recycle();}
        released("ordinary prerequisite");
    }
    static Bitmap candidate(Bitmap source,QualityPixels1932.Plan sourcePlan,QualityPixels1932.Plan output,int[] half,boolean resident,boolean identity)throws Exception {
        local("BENCHMARK").set(Boolean.TRUE);
        try{return QualityPipeline1932.strongFinish1978(source,sourcePlan,4,true,half,0,source.getWidth(),source.getHeight(),output,resident,false,identity);}
        finally {local("BENCHMARK").remove();}
    }
    static int compare(Bitmap source,QualityPixels1932.Plan sourcePlan,QualityPixels1932.Plan output,int[] half,boolean resident,boolean identity,Bitmap expected,GpuQualification1961.Cancellation cancellation)throws Exception {
        local("BENCHMARK").set(Boolean.TRUE);
        try{return QualityPipeline1932.compareStrongFinish1978(source,sourcePlan,4,true,half,0,source.getWidth(),source.getHeight(),output,resident,identity,expected,cancellation);}
        finally {local("BENCHMARK").remove();}
    }
    static void identity(int pattern,boolean mask,boolean identity)throws Exception {
        Bitmap source=Chain1962Oracle.image(65,259,pattern);int[] pristine=source.snapshot();int writes=source.writes;
        QualityPixels1932.Plan p=plan(source,mask),out=p.withOutputNoise(p.localNoise);int[] half=ResidentProof1978.half(source,LIVE);
        qualifyOrdinary(source,p,out,identity);
        try {
            for(int trial=0;trial<2;trial++) {
                Bitmap expected=null,ordinary=null,resident=null,wrong=null;
                try {
                    long before=dispatches();expected=preserved(source,p,out,identity);check(before==dispatches(),"independent reference never dispatches GPU");
                    ordinary=candidate(source,p,out,half,false,identity);equal(expected,ordinary,"complete ordinary identity="+identity+" pattern="+pattern+" trial="+trial);ordinary.recycle();ordinary=null;released("ordinary complete");
                    before=dispatches();resident=candidate(source,p,out,half,true,identity);check(dispatches()>before,"whole resident actually dispatched shader");equal(expected,resident,"complete resident identity="+identity+" pattern="+pattern+" trial="+trial);
                    check(resident.getDensity()==source.getDensity()&&resident.hasAlpha()==source.hasAlpha(),"output metadata preserved");resident.recycle();resident=null;released("resident complete");
                    for(boolean mode:new boolean[]{false,true}) {
                        int beforeBitmaps=Bitmap.ALL.size();before=dispatches();int outcome=compare(source,p,out,half,mode,identity,expected,LIVE);
                        check(outcome==ResidentProof1978.EXACT,"all actual final rows compare exactly");check(dispatches()>before,"stream comparator uses actual JNI");
                        check(Bitmap.ALL.size()==beforeBitmaps+(mode?0:1),"comparison has no full destination Bitmap, only ordinary mutable source copy");released("full stream comparison");streamCases++;
                    }
                    if(identity&&trial==0) {wrong=preserved(source,p,out,false);negativePixels+=difference(expected,wrong);}
                } finally {if(expected!=null)expected.recycle();if(ordinary!=null)ordinary.recycle();if(resident!=null)resident.recycle();if(wrong!=null)wrong.recycle();}
            }
            check(Arrays.equals(pristine,source.snapshot())&&source.writes==writes&&!source.isRecycled(),"all candidate/reference/stream routes preserve original");
            if(identity)identityCases++;else reducedCases++;
        } finally {source.recycle();}
    }
    static void failures()throws Exception {
        Bitmap source=Chain1962Oracle.image(65,259,2);int[] pristine=source.snapshot();QualityPixels1932.Plan p=plan(source,true),out=p.withOutputNoise(p.localNoise);int[] half=ResidentProof1978.half(source,LIVE);Bitmap expected=preserved(source,p,out,true);
        try {
            // Final-pixel corruption is detected through the production GPU row
            // collector, with no target Bitmap materialized by the candidate.
            int[] last={expected.snapshot()[65*259-1]^1};expected.setPixels(last,0,1,64,258,1,1);
            check(compare(source,p,out,half,true,true,expected,LIVE)==ResidentProof1978.MISMATCH,"actual resident final-pixel negative control");released("last pixel mismatch");expected.recycle();expected=preserved(source,p,out,true);
            Native1960Test.setFault(2);boolean unavailable=false;
            try {unavailable=compare(source,p,out,half,true,true,expected,LIVE)!=ResidentProof1978.EXACT;}
            catch(RuntimeException failure){unavailable=true;}
            finally{Native1960Test.setFault(0);}
            check(unavailable,"failed readback cannot pass streaming proof");released("readback failure");
            final long started=dispatches();final AtomicBoolean sawCancel=new AtomicBoolean();GpuQualification1961.Cancellation token=new GpuQualification1961.Cancellation(){public boolean cancelled(){boolean stop=dispatches()>started;if(stop)sawCancel.set(true);return stop;}};
            local("CANCELLATION").set(token);boolean stopped=false;
            try {stopped=compare(source,p,out,half,true,true,expected,token)!=ResidentProof1978.EXACT;}
            catch(RuntimeException cancelled){stopped=true;}
            finally {local("CANCELLATION").remove();Thread.interrupted();}
            check(sawCancel.get()&&stopped,"cancellation after actual dispatch prevents admission");released("actual dispatch cancellation");
            Bitmap wrongShape=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);GpuNoise1960.Session owned=carried(source);
            try {check(GpuChain1961.compareFinish1978(source,0,65,259,out,false,wrongShape,64,true,owned,true,LIVE)==ResidentProof1978.UNAVAILABLE,"invalid target shape rejected");}
            finally {wrongShape.recycle();}
            released("invalid proof image closes transferred session");
            check(compare(source,p,out,half,true,true,expected,LIVE)==ResidentProof1978.EXACT,"next complete proof recovers after cancel and invalid input");released("postcancel recovery");
            check(Arrays.equals(pristine,source.snapshot())&&!source.isRecycled(),"failed and cancelled proof input immutable");
        } finally {Native1960Test.setFault(0);local("CANCELLATION").remove();expected.recycle();source.recycle();}
    }
    public static void main(String[] args)throws Exception {
        android.os.Build.FINGERPRINT="resident-pipeline1978-actual-host";GpuQualification1961.initialize(new Context());
        check(GpuNoise1960.supports(GpuNoise1960.STRONG)&&GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961),"actual JNI compute available");
        final ProcessingTiming1947.Trace foreground=ProcessingTiming1947.begin(new Object());final long updated=foreground.updatedMillis;
        final CountDownLatch done=new CountDownLatch(1);final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        GpuQualification1961.Probe probe=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation ignored){try {
            check(GpuQualification1961.background()&&ProcessingTiming1947.current()==null,"detached after-save work");int events=CameraTrace1965.events;
            for(int pattern=0;pattern<3;pattern++)for(boolean mask:new boolean[]{false,true})for(boolean identity:new boolean[]{true,false})identity(pattern,mask,identity);
            check(negativePixels>0,"moving identity noise observation after moire changes real pixels and is detected");failures();
            check(CameraTrace1965.events==events,"private proof leaves foreground camera diagnostics untouched");
        } catch(Throwable error){failure.set(error);}finally{done.countDown();}}public void close(){}};
        check(GpuQualification1961.schedule("strong-resident-pipeline-test1978:3:1",4096,probe),"same real after-save queue");field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();
        check(done.await(360,TimeUnit.SECONDS),"actual identity pipeline completed");if(failure.get()!=null)throw new AssertionError("resident identity native test",failure.get());
        long deadline=System.nanoTime()+3000000000L;while(GpuQualification1961.retainedBytes()!=0&&System.nanoTime()<deadline)Thread.sleep(5);
        check(GpuQualification1961.retainedBytes()==0&&foreground.updatedMillis==updated&&foreground.strongCpuVerifications==0,"snapshot and trace isolation");released("complete test");
        check(identityCases==6&&reducedCases==6&&streamCases==48,"all semantic combinations and two complete trials executed");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"identityCases\":"+identityCases+",\"reducedFirstCases\":"+reducedCases+",\"streamCases\":"+streamCases+",\"exactPixels\":"+pixels+",\"wrongNoisePositionPixels\":"+negativePixels+",\"resident1978_identity_before_moire_actual_jni\":true,\"resident1978_reducefirst_after_moire_preserved\":true,\"resident1978_stream_all_pixels_actual_jni\":true,\"resident1978_no_comparison_output_bitmap\":true,\"resident1978_cancellation_and_ownership_actual_jni\":true,\"resident1978_noise_position_negative_control\":true,\"physical_android_tested\":false}");
    }
}
