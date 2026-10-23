package com.hiro.ulike;

import android.graphics.Bitmap;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual production whole-image worker/strong/resident handoff. Android bitmap
 * ownership is a functional host fixture; all shaders and JNI execute on Mesa. */
public final class ResidentPipeline1976Test {
    static long assertions,pixels,confidenceValues;static int pipelineCases,layoutCases;
    static final int[][] BANKS={{0,1,3,7,8,9,10,11,12,13},{14,15,16,17,18,19,20,21,22,23}};
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked")static <T> ThreadLocal<T> local(String name)throws Exception{return (ThreadLocal<T>)field(GpuResident1976.class,name).get(null);}
    static long dispatches(){return Native1960Test.faultFacts()[3];}
    static void equal(int[] expected,int[] actual,String label){check(actual!=null&&actual.length==expected.length,label+" shape");for(int i=0;i<expected.length;i++){assertions++;pixels++;if(expected[i]!=actual[i])throw new AssertionError(label+" at"+i+" expected="+Integer.toHexString(expected[i])+" actual="+Integer.toHexString(actual[i]));}}
    static void released(String label)throws Exception{
        check(!GpuNoise1960.sessionBusy(),label+" session released");GpuNoise1960.trimIdle();check(GpuNoise1960.retainedBytes()==0,label+" nativebuffer released");
        check(field(GpuStrong1960.class,"active").get(null)==null,label+" stage released");
        synchronized(field(GpuQualification1961.class,"LOCK").get(null)){check(((Deque<?>)field(GpuQualification1961.class,"QUEUED").get(null)).isEmpty(),label+" no recursive qualification queue");}
    }
    static Bitmap finish(Bitmap source,QualityPixels1932.Plan plan,int rotation,int width,int height,QualityPixels1932.Plan output,boolean resident,boolean cpu)throws Exception {
        local(cpu?"CPU":"BENCHMARK").set(Boolean.TRUE);
        try{return QualityPipeline1932.strongFinish1976(source,plan,4,true,null,rotation,width,height,output,resident,cpu);}
        finally{local("CPU").remove();local("BENCHMARK").remove();}
    }
    static QualityPixels1932.Plan plan(Bitmap image,int width,int height,boolean mask){
        QualityPixels1932.Plan p=Chain1962Oracle.plan(image,width,height,0);
        if(mask)p=p.withFaceRegions(new FaceRegions1934.Mask());
        return p;
    }
    /** Real two-trial full-image equality comes first. Timing admission is
     * deliberately seeded afterward: software Mesa is not the target phone and
     * this test exercises the gate without claiming a physical speed result. */
    static void qualifyOrdinary(Bitmap source,QualityPixels1932.Plan sourcePlan,int rotation,int width,int height,QualityPixels1932.Plan outputPlan)throws Exception {
        Bitmap clean=source.copy(Bitmap.Config.ARGB_8888,true);
        try {
            Method smooth=QualityPipeline1932.class.getDeclaredMethod("smoothNoiseInPlace1976",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class,int[].class,GpuNoise1960.Session[].class);smooth.setAccessible(true);
            Object regions;local("CPU").set(Boolean.TRUE);
            try{regions=smooth.invoke(null,clean,sourcePlan,4,true,null,null);}finally{local("CPU").remove();}
            check(regions!=null,"actual strong mask prepared for baseline proof");
            Method outputMask=regions.getClass().getDeclaredMethod("outputMask",int.class,int.class,int.class);outputMask.setAccessible(true);
            QualityPixels1932.Plan complete=outputPlan.withSmoothedRegions((QualityPixels1932.SmoothMask)outputMask.invoke(regions,rotation,width,height));
            for(int trial=0;trial<2;trial++){
                Bitmap cpu=null,gpu=null;
                try{
                    cpu=GpuChain1961.cpuFinish(clean,rotation,width,height,complete,true);
                    gpu=GpuChain1961.runFinish1976(clean,rotation,width,height,complete,true,null,32);
                    check(cpu!=null&&gpu!=null,"actual ordinary baseline pair"+trial);
                    equal(cpu.snapshot(),gpu.snapshot(),"complete ordinary two exact trials "+trial);
                }finally{if(cpu!=null)cpu.recycle();if(gpu!=null)gpu.recycle();}
            }
            Method key=GpuChain1961.class.getDeclaredMethod("finishKey1976",Bitmap.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);key.setAccessible(true);
            String proof=(String)key.invoke(null,clean,rotation,width,height,complete,true);
            // Controlled timing metadata only. All pixels above were computed by
            // the real JNI/shader graph and the independent CPU route twice.
            GpuQualification1961.qualified(proof,1000000000L,1000000L,0);
            check(GpuChain1961.residentBaseline1976(source,rotation,width,height,outputPlan)!=null,"exact whole-finish prerequisite available");
            released("qualified ordinary baseline");
        }finally{clean.recycle();}
    }
    static void pipeline(int rotation,boolean mask)throws Exception{
        Bitmap source=Chain1962Oracle.image(65,259,2);int[] pristine=source.snapshot();int originalWrites=source.writes;
        QualityPixels1932.Plan p=plan(source,65,259,mask),out=plan(source,71,267,mask);
        Bitmap cpu=null,ordinary=null,resident=null;
        qualifyOrdinary(source,p,rotation,71,267,out);
        long initial=dispatches();
        try {
            cpu=finish(source,p,rotation,71,267,out,false,true);check(cpu!=null,"CPU complete image");
            check(dispatches()==initial,"captured CPU flag applies to child strip workers");released("CPU complete");
            long before=dispatches();ordinary=finish(source,p,rotation,71,267,out,false,false);
            check(dispatches()>before,"ordinary benchmark really executes JNI shaders");check(ordinary!=null,"ordinary complete image");
            equal(cpu.snapshot(),ordinary.snapshot(),"ordinary full pixels rotation"+rotation+" mask"+mask);released("ordinary complete");
            before=dispatches();resident=finish(source,p,rotation,71,267,out,true,false);
            check(dispatches()>before,"resident benchmark really executes JNI shaders");check(resident!=null,"resident complete image");
            equal(cpu.snapshot(),resident.snapshot(),"resident full pixels rotation"+rotation+" mask"+mask);
            equal(pristine,source.snapshot(),"immutable original source");check(!source.isRecycled(),"original retained");check(source.writes==originalWrites,"no original Bitmap commit during candidate route");
            check(resident.getWidth()==71&&resident.getHeight()==267,"resident geometry");check(resident.getDensity()==source.getDensity(),"density preserved");released("resident complete");pipelineCases++;
        }finally{if(cpu!=null)cpu.recycle();if(ordinary!=null)ordinary.recycle();if(resident!=null)resident.recycle();source.recycle();}
    }
    static void failures()throws Exception{
        Bitmap source=Chain1962Oracle.image(65,259,2);int[] pristine=source.snapshot();QualityPixels1932.Plan p=plan(source,65,259,true),out=plan(source,71,267,true);
        Bitmap result=null;boolean failed=false,returned=false;
        try {
            Native1960Test.setFault(2);
            try{result=finish(source,p,90,71,267,out,true,false);returned=result!=null;}catch(RuntimeException unavailable){failed=true;}
            finally{Native1960Test.setFault(0);if(result!=null){result.recycle();result=null;}}
            check(!returned,"failed readback withheld before disposal");equal(pristine,source.snapshot(),"failed GPU preserves source");released("injected readback");
            result=finish(source,p,90,71,267,out,true,false);check(result!=null,"fresh session recovers after known-complete read failure");result.recycle();result=null;released("recovery");
            final long before=dispatches();final AtomicBoolean observed=new AtomicBoolean();
            local("CANCELLATION").set(new GpuQualification1961.Cancellation(){public boolean cancelled(){boolean stop=dispatches()>before;if(stop)observed.set(true);return stop;}});
            failed=false;
            try{result=finish(source,p,90,71,267,out,true,false);}catch(RuntimeException cancelled){failed=true;}
            finally{local("CANCELLATION").remove();Thread.interrupted();if(result!=null){result.recycle();result=null;}}
            check(observed.get()&&failed,"cancel after real shader dispatch propagates through captured Stage token");
            equal(pristine,source.snapshot(),"cancelled GPU preserves source");released("cancelled actual GPU stage");
            result=finish(source,p,90,71,267,out,true,false);check(result!=null,"next complete call recovers after cancellation");released("postcancel recovery");
        }finally{Native1960Test.setFault(0);local("CANCELLATION").remove();if(result!=null)result.recycle();source.recycle();}
    }
    static StrongNoise1958.Model model(final Bitmap source){return StrongNoise1958.prepare(new StrongNoise1958.Patches(){public void read(int[] p,int x,int y,int w,int h){source.getPixels(p,0,w,x,y,w,h);}},source.getWidth(),source.getHeight(),4,true);}
    static void layout(int coreRows,int profile,Bitmap image,StrongNoise1958.Model model,QualityPixels1932.Plan plan,int[] expected,int[] expectedConfidence)throws Exception{
        int width=image.getWidth(),height=image.getHeight();GpuPolicy1960.Protection protection=new GpuPolicy1960.Protection(plan,true);
        int[] output=new int[width*height],confidence=new int[((width+3)/4)*((height+3)/4)];
        GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"layout native session");
        try {
            float[] evidence=StrongNoise1958.gpuEvidence1960(model);int[][] maps=StrongNoise1958.gpuMaps1960(model);
            check(session.upload(2,evidence)&&session.upload(4,maps[0])&&session.upload(5,maps[1])&&session.upload(6,maps[2]),"layout resident immutable model upload");
            for(int first=0,number=0;first<height;first+=coreRows,number++){
                int count=Math.min(coreRows,height-first),top=Math.max(0,first-18),bottom=Math.min(height,first+count+18),rows=bottom-top,begin=first-top;
                int[] source=new int[width*rows];image.getPixels(source,0,width,0,top,width,rows);
                int[] policy=new int[width*count*2];protection.freezePolicy1964(width,count,first,policy,false);
                int[] u=new int[32];u[0]=width;u[1]=rows;u[2]=begin;u[3]=begin+count;u[4]=0;u[5]=rows;u[6]=top;u[7]=height;u[8]=4;u[9]=1;u[10]=3;u[11]=1;u[12]=1;u[18]=plan.beautyQ8;u[19]=plan.shadowBudgetQ8;
                GpuPolicy1960.PolicyData d=protection.data(width,count,first);int[] bank=BANKS[number%2];GpuNoise1960.Lease1971 lease=GpuStrong1960.reserveRange1971(session,source,policy,u,d,bank);
                check(lease!=null,"layout strip capacity");
                try {
                    check(lease.revalidate1971(),"layout strip capacity revalidated");GpuNoise1960.Ticket ticket=session.submit(GpuStrong1960.commands1973(source,policy,u,d,bank,0,profile),number%2);
                    check(ticket!=null,"layout actual shader submitted");GpuStrong1960.Read1971 read=GpuStrong1960.read1971(session,ticket,u,bank);check(read!=null&&read.failure==null&&read.result!=null,"layout complete guarded readback");
                    System.arraycopy(read.result.pixels,0,output,first*width,width*count);
                    System.arraycopy(read.result.confidence,0,confidence,(first/4)*((width+3)/4),((width+3)/4)*((count+3)/4));
                }finally{lease.close();}
            }
        }finally{session.close();}
        equal(expected,output,"actual strong layout"+coreRows+" profile"+profile);equal(expectedConfidence,confidence,"actual strong confidence"+coreRows+" profile"+profile);confidenceValues+=confidence.length;released("actual layout");layoutCases++;
    }
    static void layouts()throws Exception{
        Bitmap image=Chain1962Oracle.image(65,289,2);QualityPixels1932.Plan plan=plan(image,65,289,true);StrongNoise1958.Model model=model(image);
        int[] source=image.snapshot(),expected=new int[source.length],confidence=new int[((65+3)/4)*((289+3)/4)],policy=new int[source.length*2];
        GpuPolicy1960.Protection protection=new GpuPolicy1960.Protection(plan,true);protection.freezePolicy1964(65,289,0,policy,false);
        check(StrongNoise1958.gpuOracleSnapshot1961(source,expected,65,289,0,289,0,289,0,4,true,model,policy,confidence),"wholeimage independent CPU strong oracle");
        try{for(int profile:new int[]{0,3})for(int core:new int[]{128,256})layout(core,profile,image,model,plan,expected,confidence);}
        finally{StrongNoise1958.discardResident1961(model);image.recycle();}
    }
    public static void main(String[] args)throws Exception{
        android.os.Build.FINGERPRINT="resident-pipeline1976-actual-host";GpuQualification1961.initialize(new Context());
        check(GpuNoise1960.supports(GpuNoise1960.STRONG)&&GpuNoise1960.supports(GpuNoise1960.FINISH1961),"actual native compute required");
        final ProcessingTiming1947.Trace foreground=ProcessingTiming1947.begin(new Object());final long foregroundUpdated=foreground.updatedMillis;
        final CountDownLatch done=new CountDownLatch(1);final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        GpuQualification1961.Probe probe=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation ignored){try{
            check(GpuQualification1961.background(),"actual qualification thread context");check(ProcessingTiming1947.current()==null,"background thread carries no foreground trace");
            final int diagnosticBefore=CameraTrace1965.events;
            for(int rotation:new int[]{0,90,180,270})for(boolean mask:new boolean[]{false,true})pipeline(rotation,mask);
            failures();layouts();
            check(CameraTrace1965.events==diagnosticBefore,"private benchmark emits no foreground diagnostic events");
        }catch(Throwable error){failure.set(error);}finally{done.countDown();}}public void close(){}};
        check(GpuQualification1961.schedule("strong-resident-pipeline-test1976:3:1",4096,probe),"real private idle job admitted");
        field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();
        check(done.await(180,TimeUnit.SECONDS),"actual whole pipeline completed");
        if(failure.get()!=null)throw new AssertionError("pipeline native test",failure.get());
        long deadline=System.nanoTime()+3000000000L;while(GpuQualification1961.retainedBytes()!=0&&System.nanoTime()<deadline)Thread.sleep(5);
        check(foreground.updatedMillis==foregroundUpdated&&foreground.strongGpuStrips==0&&foreground.strongCpuStrips==0&&foreground.strongCpuVerifications==0&&foreground.noiseStrongNanos1976==0,"detached trial cannot overwrite foreground capture diagnostics");
        check(GpuQualification1961.retainedBytes()==0,"job snapshot ownership drained");released("complete test");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"pipelineCases\":"+pipelineCases+",\"layoutCases\":"+layoutCases+",\"exactPixels\":"+pixels+",\"exactConfidenceValues\":"+confidenceValues+",\"actualWholePipelineJni\":true,\"hostTimingAdmissionFixture\":true,\"actualLayout128256\":true,\"physicalAndroidTested\":false}");
    }
}
