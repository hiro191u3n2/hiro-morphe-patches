package com.hiro.ulike;

public final class SaveMemory1981Test {
    static int assertions;
    static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static void comparison(int outputWidth,int outputHeight)throws Exception {
        java.lang.reflect.Method make=AsyncSave1935.class.getDeclaredMethod("memoryPlan1981",int.class,int.class,int.class,int.class,int.class,long.class,PhotoDetail.Settings.class);make.setAccessible(true);
        int width=4080,height=3060;long input=(long)width*height*4L;
        SaveMemory1981.Plan plan=(SaveMemory1981.Plan)make.invoke(null,width,height,outputWidth,outputHeight,0,input,new PhotoDetail.Settings(4));
        check(plan.valid(),"verbatim production model/worker formulas provide valid camera plan");
        long output=(long)outputWidth*outputHeight*4L;
        long heap=SaveMemory1981.managedNeed(plan,1,0,0),system=SaveMemory1981.systemNeed(plan,1,output,0,0,0);
        long legacy=SaveQueue1935.RESERVE+Math.max((long)width*height,(long)outputWidth*outputHeight)*24L;
        check(heap<legacy,"Java budget no longer treats all native bitmap storage as Java arrays");
        check(system>heap,"separate system limit still reserves future native images");
        System.out.println("MEMORY_1981 {\"source_width\":"+width+",\"source_height\":"+height+",\"output_width\":"+outputWidth+",\"output_height\":"+outputHeight+",\"source_input_bytes\":"+input+",\"strong_model_bytes\":"+plan.modelBytes+",\"bounded_worker_bytes\":"+plan.workerBytes+",\"new_java_required_bytes\":"+heap+",\"new_system_required_bytes_with_one_queued_argb_job\":"+system+",\"old_scalar_base_required_bytes\":"+legacy+",\"old_scalar_required_bytes_with_same_queued_job\":"+(legacy+input+output)+",\"device_measurement\":false}");
    }
    static void observations()throws Exception {
        java.lang.reflect.Method read=AsyncSave1935.class.getDeclaredMethod("availableDomains1981");read.setAccessible(true);
        SaveQuality2.systemInfo=true;android.app.ActivityManager.available=512L*1024*1024;android.app.ActivityManager.threshold=64L*1024*1024;
        SaveMemory1981.Snapshot snapshot=(SaveMemory1981.Snapshot)read.invoke(null);
        check(snapshot.knownSystem&&!snapshot.lowMemory&&snapshot.system==448L*1024*1024,"actual MemoryInfo fields keep system threshold separate from Java heap");
        android.app.ActivityManager.low=true;snapshot=(SaveMemory1981.Snapshot)read.invoke(null);check(snapshot.lowMemory,"low-memory signal closes admission");android.app.ActivityManager.low=false;
        android.app.ActivityManager.fail=true;snapshot=(SaveMemory1981.Snapshot)read.invoke(null);check(!snapshot.knownSystem,"unavailable MemoryInfo retains conservative fallback");android.app.ActivityManager.fail=false;
        android.os.Build.VERSION.SDK_INT=25;snapshot=(SaveMemory1981.Snapshot)read.invoke(null);check(!snapshot.knownSystem&&snapshot.heap<=448L*1024*1024,"pre-native-Bitmap Android retains min(heap,system) fallback");android.os.Build.VERSION.SDK_INT=34;
        android.app.ActivityManager.available=-1;snapshot=(SaveMemory1981.Snapshot)read.invoke(null);check(snapshot.lowMemory,"invalid system counter fails closed");
        SaveQuality2.systemInfo=false;
    }
    static void rotationBudget()throws Exception {
        java.lang.reflect.Method exact=AsyncSave1935.class.getDeclaredMethod("memoryPlan1981",int.class,int.class,int.class,boolean.class,long.class,PhotoDetail.Settings.class);exact.setAccessible(true);
        java.lang.reflect.Method next=AsyncSave1935.class.getDeclaredMethod("futureInputPlan1981",int.class,int.class,boolean.class,PhotoDetail.Settings.class);next.setAccessible(true);
        PhotoDetail.Settings settings=new PhotoDetail.Settings(4);
        SaveMemory1981.Plan portrait=(SaveMemory1981.Plan)exact.invoke(null,3060,4080,0,false,3060L*4080*8,settings);
        SaveMemory1981.Plan landscape=(SaveMemory1981.Plan)exact.invoke(null,3060,4080,90,false,3060L*4080*8,settings);
        SaveMemory1981.Plan future=(SaveMemory1981.Plan)next.invoke(null,3060,4080,false,settings);
        check(landscape.workerBytes>portrait.workerBytes,"same pixel count can have a wider rotated output workspace");
        check(future.workerBytes==landscape.workerBytes&&future.modelBytes>=portrait.modelBytes,"rotation-unknown next shutter reserves larger actual orientation work budget");
        check(future.pixels==portrait.pixels&&future.inputBytes==portrait.inputBytes,"orientation safety does not invent a larger rectangular capture");
    }
    static void residentAccounting()throws Exception {
        final java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
        SaveMemory1981.Plan plan=new SaveMemory1981.Plan(2,2,2,2,16,0,0);
        Runnable job=new Runnable(){public void run(){try{entered.countDown();release.await();SaveQueue1935.terminalTurn1956(this);}catch(InterruptedException error){throw new AssertionError(error);}finally{SaveQueue1935.release();}}};
        SaveQueue1935.reserve();SaveQueue1935.submit1981(job,4,32,plan,16);check(entered.await(3,java.util.concurrent.TimeUnit.SECONDS),"real queued image accounting registered");
        long heap=SaveMemory1981.managedNeed(plan,1,0,0),system=SaveMemory1981.systemNeed(plan,1,16,0,0,0);
        check(SaveQueue1935.memoryAllows1981(new SaveMemory1981.Snapshot(heap,system,true,false),plan,1),"known live input is not deducted from system headroom twice");
        check(!SaveQueue1935.memoryAllows1981(new SaveMemory1981.Snapshot(heap,system-1,true,false),plan,1),"not-yet-created final image stays reserved");
        SaveQueue1935.nativeCost1981(job,32,32);system=SaveMemory1981.systemNeed(plan,1,0,0,0,0);
        final SaveMemory1981.Snapshot fullyResident=new SaveMemory1981.Snapshot(heap,system,true,false);
        check(SaveQueue1935.memoryAllows1981(fullyResident,plan,1),"encoding marks both real images resident without removing ownership costs");check(SaveQueue1935.nativeBytes()==32,"full live image cost retained for diagnostics/legacy callers");
        java.util.concurrent.atomic.AtomicBoolean stop=new java.util.concurrent.atomic.AtomicBoolean();
        Thread updates=new Thread(()->{while(!stop.get()){SaveQueue1935.nativeCost1981(job,64,64);SaveQueue1935.nativeCost1981(job,32,32);}});updates.start();boolean coherent=true;
        for(int i=0;i<2000;i++)coherent&=SaveQueue1935.memoryAllows1981(fullyResident,plan,1);
        stop.set(true);updates.join(2000);check(coherent,"concurrent cost/live updates are one atomic admission snapshot");
        release.countDown();long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(3);while(!SaveQueue1935.idle1953()&&System.nanoTime()<until)Thread.yield();check(SaveQueue1935.idle1953()&&SaveQueue1935.nativeBytes()==0,"all actual/budget maps released at terminal boundary");
    }
    static void analysisPeak()throws Exception {
        final java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
        long mib=1024L*1024,extra=32*mib;
        SaveMemory1981.Plan plan=new SaveMemory1981.Plan(4000,3000,5712,4284,48000000,80*mib,48*mib);
        Runnable job=new Runnable(){public void run(){try{entered.countDown();release.await();SaveQueue1935.terminalTurn1956(this);}catch(InterruptedException error){throw new AssertionError(error);}finally{SaveQueue1935.release();}}};
        SaveQueue1935.reserve();SaveQueue1935.submit1981(job,plan.pixels,96000000,plan,48000000);
        check(entered.await(3,java.util.concurrent.TimeUnit.SECONDS),"foreground correction peak registered before model allocation");
        long generation=SaveQueue1935.memoryGeneration1981();
        long heap=SaveMemory1981.managedNeed(plan,SaveMemory1981.ANALYSIS,0,extra);
        long system=SaveMemory1981.systemNeed(plan,SaveMemory1981.ANALYSIS,48000000,0,0,extra);
        check(heap-1>=SaveMemory1981.RESERVE+extra,"boundary would pass a gate checking only reserve plus optional analysis");
        check(!SaveQueue1935.analysisMemoryAllows1981(new SaveMemory1981.Snapshot(heap-1,Long.MAX_VALUE,true,false),extra,generation),"optional reader cannot crowd out an unallocated foreground model/workspace peak");
        check(!SaveQueue1935.analysisMemoryAllows1981(new SaveMemory1981.Snapshot(Long.MAX_VALUE,system-1,true,false),extra,generation),"native working destinations remain reserved beside the optional reader");
        SaveMemory1981.Snapshot exact=new SaveMemory1981.Snapshot(heap,system,true,false);
        check(SaveQueue1935.analysisMemoryAllows1981(exact,extra,generation),"exact analysis boundary includes peak and future growth but not already-live input twice");
        SaveQueue1935.reserve();long reservedGeneration=SaveQueue1935.memoryGeneration1981();
        check(reservedGeneration!=generation&&!SaveQueue1935.analysisMemoryAllows1981(new SaveMemory1981.Snapshot(Long.MAX_VALUE,Long.MAX_VALUE,true,false),extra,reservedGeneration),"reserved SDK handoff without a registered copy/plan blocks optional work even with abundant observed memory");
        SaveQueue1935.release();long releasedGeneration=SaveQueue1935.memoryGeneration1981();
        check(releasedGeneration!=reservedGeneration&&!SaveQueue1935.analysisMemoryAllows1981(exact,extra,generation),"reserve/release cycle invalidates an earlier memory observation even when tracked counts match again");
        generation=releasedGeneration;check(SaveQueue1935.analysisMemoryAllows1981(exact,extra,generation),"fresh fully-tracked observation reopens the unchanged optional-analysis boundary");
        check(!SaveQueue1935.analysisMemoryAllows1981(new SaveMemory1981.Snapshot(Long.MAX_VALUE,Long.MAX_VALUE,false,false),extra,generation),"optional peak cannot use unknown system accounting");
        SaveQueue1935.nativeCost1981(job,96000000,96000000);
        check(!SaveQueue1935.analysisMemoryAllows1981(exact,extra,generation),"memory observation is rejected after concurrent ownership/accounting change");
        long residentSystem=SaveMemory1981.systemNeed(plan,SaveMemory1981.ANALYSIS,0,0,0,extra);
        check(SaveQueue1935.analysisMemoryAllows1981(new SaveMemory1981.Snapshot(heap,residentSystem,true,false),extra,SaveQueue1935.memoryGeneration1981()),"new observation removes only confirmed resident native growth");
        release.countDown();long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(3);while(!SaveQueue1935.idle1953()&&System.nanoTime()<until)Thread.yield();check(SaveQueue1935.idle1953(),"analysis peak test releases foreground ownership");
    }
    public static void main(String[]args)throws Exception {
        SaveMemory1981.Plan plan=new SaveMemory1981.Plan(4000,3000,5712,4284,48000000,36000000,24000000);
        check(plan.valid(),"actual source/output geometry accepted");long proof=1024,scratch=2048,analysis=4096,queued=96000000;
        long heap=SaveMemory1981.managedNeed(plan,SaveMemory1981.NEXT_CAPTURE,proof,analysis);
        long system=SaveMemory1981.systemNeed(plan,SaveMemory1981.NEXT_CAPTURE,queued,proof,scratch,analysis);
        check(SaveMemory1981.allows(new SaveMemory1981.Snapshot(heap,system,true,false),plan,1,queued,proof,scratch,analysis),"both exact domain boundaries admit");
        check(!SaveMemory1981.allows(new SaveMemory1981.Snapshot(heap-1,Long.MAX_VALUE,true,false),plan,1,queued,proof,scratch,analysis),"plentiful system RAM never overrides Java shortage");
        check(!SaveMemory1981.allows(new SaveMemory1981.Snapshot(Long.MAX_VALUE,system-1,true,false),plan,1,queued,proof,scratch,analysis),"plentiful heap never overrides native/system shortage");
        check(!SaveMemory1981.allows(new SaveMemory1981.Snapshot(Long.MAX_VALUE,Long.MAX_VALUE,false,false),plan,1,queued,proof,scratch,analysis),"unknown system memory cannot use relaxed domain gate");
        check(!SaveMemory1981.allows(new SaveMemory1981.Snapshot(Long.MAX_VALUE,Long.MAX_VALUE,true,true),plan,1,queued,proof,scratch,analysis),"Android low-memory signal closes gate");
        long copied=SaveMemory1981.systemNeed(plan,0,queued,proof,scratch,analysis);
        check(copied<system,"copy phase does not reserve another native capture/NV21 pack");
        check(SaveMemory1981.managedNeed(plan,1,0,0)-SaveMemory1981.managedNeed(plan,0,0,0)==36000000L,"single shot reserves two exact-size NV21 arrays");
        SaveMemory1981.Plan f16=new SaveMemory1981.Plan(4000,3000,5712,4284,96000000,36000000,24000000);
        check(SaveMemory1981.systemNeed(f16,0,0,0,0,0)-SaveMemory1981.systemNeed(plan,0,0,0,0,0)==48000000L,"copy budget retains actual 8-byte pixel depth");
        check(SaveMemory1981.systemNeed(plan,1,0,0,0,0)==SaveMemory1981.systemNeed(f16,1,0,0,0,0),"future capture never inherits a prior 4-byte depth assumption");
        for(int mode=0;mode<4;mode++)check(!SaveMemory1981.allows(new SaveMemory1981.Snapshot(Long.MAX_VALUE,Long.MAX_VALUE,true,false),plan,1,mode==0?Long.MAX_VALUE:0,mode==1?Long.MAX_VALUE:0,mode==2?Long.MAX_VALUE:0,mode==3?Long.MAX_VALUE:0),"saturated retained domain closes gate "+mode);
        check(SaveMemory1981.add(Long.MAX_VALUE,1)==Long.MAX_VALUE,"addition overflow saturates");check(SaveMemory1981.multiply(Long.MAX_VALUE,8)==Long.MAX_VALUE,"pixel-byte multiplication overflow saturates");
        check(!new SaveMemory1981.Plan(0,3000,4000,3000,1,0,0).valid(),"zero source rejected");check(!new SaveMemory1981.Plan(8000,5000,4000,3000,1,0,0).valid(),"unsupported source extent rejected");
        check(!new SaveMemory1981.Plan(2,2,2,2,16,-1,0).valid(),"unknown model cannot enable domain gate");
        check(!SaveMemory1981.allows(new SaveMemory1981.Snapshot(-1,Long.MAX_VALUE,true,false),plan,1,0,0,0,0),"invalid observed heap rejected");
        comparison(4080,3060);comparison(5712,4284);observations();rotationBudget();residentAccounting();analysisPeak();
        System.out.println("PASS SaveMemory1981 assertions="+assertions);
    }
}
