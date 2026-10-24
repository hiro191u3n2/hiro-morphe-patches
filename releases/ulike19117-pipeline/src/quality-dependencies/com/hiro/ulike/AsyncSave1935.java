package com.hiro.ulike;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;

/** Pinned 5.6.2 auto-save route. SDK rendering stays serial. Only an owned,
 * already-rendered photo is queued. Exact source pixels, settings and metadata
 * are owned before the next real shutter is released. One preparation stage
 * overlaps one FIFO encoder; completion UI never blocks the save workers. */
public final class AsyncSave1935 {
    private AsyncSave1935() {}
    private static final ThreadLocal<Job> CURRENT=new ThreadLocal<Job>();
    private static volatile long lastPixels;
    private static volatile boolean captureOpen;
    private static long serial;
    private static final IdentityHashMap<Object,Ticket> tickets=new IdentityHashMap<Object,Ticket>();
    private static final ThreadPoolExecutor PREP=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1),new ThreadFactory(){public Thread newThread(Runnable r){Thread t=new Thread(r,"ULike-capture1935");t.setDaemon(true);return t;}});
    private static final class Ticket {
        final long id;
        final PhotoDetail.Settings detail;
        final boolean colour,fixed;
        Ticket(long id){this.id=id;detail=PhotoDetail.snapshot1932();colour=ChromaPipeline177.enabled1932();fixed=SaveQuality2.isFixed245Enabled();}
    }
    /** Called synchronously by the auto-save manager while native compose still owns the shutter. */
    public static void submitAuto(Object manager,int rotation,int direction){
        WholeRoute1953.foregroundStarted();
        Object task=null;boolean reserved=false,exitHeld=false;long captureId=0;
        try{
            Class<?> owner=Class.forName("i.o.a.b1.a.b.f.a");
            task=Class.forName("i.o.a.b1.a.b.f.a$a").getConstructor(owner,int.class,int.class).newInstance(manager,direction,rotation);
            final Ticket ticket;
            synchronized(tickets){
                if(SaveQueue1935.count()>=SaveQueue1935.CAPACITY)throw new IllegalStateException("capture queue full");
                SaveQueue1935.reserve();reserved=true;ExitJobs185.begin();exitHeld=true;captureOpen=false;
                captureId=++serial;ticket=new Ticket(captureId);tickets.put(task,ticket);
            }
            final Object capturedTask=task;
            PREP.execute(new Runnable(){public void run(){captureTask(capturedTask);}});
        }catch(Exception failure){
            if(task!=null)synchronized(tickets){tickets.remove(task);}
            submissionFailure(task,captureId,reserved,exitHeld);
        }catch(OutOfMemoryError failure){
            if(task!=null)synchronized(tickets){tickets.remove(task);}
            submissionFailure(task,captureId,reserved,exitHeld);
        }
    }
    private static final class Job implements Runnable {
        final Object manager,callback,controller,notification;
        final Bitmap input;
        final int rotation,direction;
        final PhotoDetail.Settings detail;
        final boolean colour,fixed,ownsInput,serialOnly;
        final long pixels,id,inputBytes,anticipatedBytes;
        final ProcessingTiming1947.Trace timing;
        boolean released,hardwareCompleted1956;
        ExecutorService codecExecutor;
        Runnable codecTask;
        CountDownLatch codecPreparation;
        Job(Object task,Bitmap bitmap,Object controller,Object notification,Ticket ticket,boolean ownsInput,boolean serialOnly)throws ReflectiveOperationException{
            manager=field(task,"k");callback=field(manager,"b");this.controller=controller;this.notification=notification;
            rotation=((Integer)field(task,"j")).intValue();direction=((Integer)field(task,"c")).intValue();input=bitmap;
            detail=ticket.detail;colour=ticket.colour;fixed=ticket.fixed;id=ticket.id;this.ownsInput=ownsInput;this.serialOnly=serialOnly;
            ShotContext1932.Snapshot context=ShotContext1932.forBitmap(bitmap);
            ProcessingTiming1947.Trace trace=ProcessingTiming1947.forKey(bitmap);
            timing=trace!=null?trace:ProcessingTiming1947.begin(bitmap);
            long sourcePixels=Math.max((long)bitmap.getWidth()*bitmap.getHeight(),(long)context.sourceWidth*context.sourceHeight);
            inputBytes=bitmapBytes(bitmap);
            long outputPixels=sourcePixels;
            try{
                Object expected=callStatic("com.hiro.ulike.SaveQuality2","output186",new Class<?>[]{int.class,int.class,int.class,boolean.class},bitmap.getWidth(),bitmap.getHeight(),rotation,fixed);
                if(expected instanceof int[]){int[] size=(int[])expected;if(size.length==2&&size[0]>0&&size[1]>0)outputPixels=Math.max(outputPixels,(long)size[0]*size[1]);}
            }catch(ReflectiveOperationException unavailable){if(fixed)outputPixels=Math.max(outputPixels,5712L*4284L);}
            pixels=outputPixels;
            anticipatedBytes=inputBytes+Math.max(outputPixels*4L,inputBytes);
        }
        public void run(){
            // submit + early capture release must finish before this worker can
            // post a save receipt, including unusually fast or failed saves.
            synchronized(this){}
            boolean saved=false;
            ProcessingTiming1947.Scope timingScope=ProcessingTiming1947.enter(timing);
            try {
                CURRENT.set(this);
                set(controller,"g","");set(controller,"d",Long.valueOf(System.currentTimeMillis()));
                Object result=call(controller,"r",new Class<?>[]{Bitmap.class,int.class,int.class,boolean.class},input,rotation,direction,false);
                set(controller,"b",result instanceof String?result:"");
                saved=result instanceof String&&((String)result).length()>0;
            } catch(Exception failure){
                try{set(controller,"b","");set(controller,"g","保存に失敗しました。");}catch(ReflectiveOperationException ignored){}
            } finally {
                // RUNNING PrepTicket.abandon returns before its producer has
                // disposed the writer. Join that producer before handing out
                // the next codec permit, then drain its asynchronous close.
                finishCodecPreparation(this);
                // The hardware turn can end before file verification/publication.
                // Both success and failed correction retain submission-order receipt
                // ownership until the preceding file tail has actually completed.
                SaveQueue1935.terminalTurn1956(this);
                try {
                    // Drain/validation failures can clear an otherwise returned
                    // path. A failed photo gets its own terminal diagnostic.
                    try { saved=saved&&"".equals(field(controller,"g"))&&field(controller,"b") instanceof String&&((String)field(controller,"b")).length()>0; }
                    catch(ReflectiveOperationException unknown){saved=false;}
                    ProcessingTiming1947.finish(timing,saved);
                    ProcessingTiming1947.restore(timingScope);
                    CURRENT.remove();
                }
                finally {
                    try {
                        // Native s/r returns only after writer.close, validation
                        // and publication. The queue owns this bitmap alone.
                        if(ownsInput)QualityPipeline1932.recycle1954(input);
                    } finally {
                        boolean posted=false;
                        try {posted=finishOnMain(this);}
                        finally {
                            try {SaveQueue1935.release();}
                            finally {if(!posted)ExitJobs185.end();}
                        }
                    }
                }
            }
        }
    }
    public static PhotoDetail.Settings settings(PhotoDetail.Settings live){Job j=CURRENT.get();return j==null?live:j.detail;}
    public static boolean chroma(boolean live){Job j=CURRENT.get();return j==null?live:j.colour;}
    public static boolean fixed(boolean live){Job j=CURRENT.get();return j==null?live:j.fixed;}
    /** H21 excludes a CPU reference timed while its codec constructor works. */
    public static boolean codecIdle1953() {
        Job job=CURRENT.get();
        if(job==null)return true;
        CountDownLatch complete=job.codecPreparation;
        return job.codecTask==null&&(complete==null||complete.getCount()==0);
    }
    /** Wrap only the existing X.D1() call; its original busy/error callback is retained. */
    public static int readiness(Object camera){
        WholeRoute1953.foregroundStarted();
        try{int original=((Integer)call(camera,"D1",new Class<?>[0])).intValue();return original!=0?original:(captureBlocked()?3:0);}
        catch(ReflectiveOperationException unavailable){return 3;}
        catch(RuntimeException unavailable){return 3;}
    }
    /** Extra admission guard only: original shutter readiness/busy checks remain. */
    public static boolean captureBlocked(){
        synchronized(tickets){
            int count=SaveQueue1935.count();
            if(count==0)return false;
            if(count>=SaveQueue1935.CAPACITY||!captureOpen)return true;
            long currentPixels=currentInputPixels();
            return currentPixels<=0 || !SaveQueue1935.memoryAllows(available(),Math.max(SaveQueue1935.maximumPixels(),Math.max(lastPixels,currentPixels)));
        }
    }
    /** Read the actual configured next-shot YUV reader. Camera/lens changes are
     * allowed during saving, so the previous photo's dimensions are insufficient.
     * Unknown/reconfiguring/non-YUV routes keep the original serial admission. */
    private static long currentInputPixels(){
        try{
            if(!Boolean.TRUE.equals(callStatic("com.hiro.ulike.CaptureYuv","isReady",new Class<?>[0])))return 0;
            Object active=staticField("com.hiro.ulike.CaptureYuv","active");
            if(!(active instanceof WeakReference))return 0;
            Object owner=((WeakReference<?>)active).get();if(owner==null)return 0;
            Object map=staticField("com.hiro.ulike.CaptureYuv","states");if(!(map instanceof Map))return 0;
            Object state; synchronized(map){state=((Map<?,?>)map).get(owner);}
            if(state==null||!Boolean.TRUE.equals(field(state,"ready"))||Boolean.TRUE.equals(field(state,"closed")))return 0;
            if(field(state,"original")!=field(owner,"e0"))return 0;
            Object reader=field(state,"yuv");if(reader==null)return 0;
            int w=((Integer)call(reader,"getWidth",new Class<?>[0])).intValue();
            int h=((Integer)call(reader,"getHeight",new Class<?>[0])).intValue();
            int format=((Integer)call(reader,"getImageFormat",new Class<?>[0])).intValue();
            long pixels=(long)w*h;
            if(w<2||h<2||(w&1)!=0||(h&1)!=0||format!=35||pixels>32000000L)return 0;
            synchronized(map){if(((Map<?,?>)map).get(owner)!=state)return 0;}
            Object latest=staticField("com.hiro.ulike.CaptureYuv","active");
            if(!(latest instanceof WeakReference)||((WeakReference<?>)latest).get()!=owner||field(state,"yuv")!=reader)return 0;
            if(Boolean.TRUE.equals(field(state,"closed"))||field(state,"original")!=field(owner,"e0"))return 0;
            Object expected=callStatic("com.hiro.ulike.SaveQuality2","output186",new Class<?>[]{int.class,int.class,int.class,boolean.class},w,h,0,SaveQuality2.isFixed245Enabled());
            if(!(expected instanceof int[]))return 0;int[] size=(int[])expected;
            if(size.length!=2||size[0]<=0||size[1]<=0)return 0;
            long planned=Math.max(pixels,(long)size[0]*size[1]);
            return Boolean.TRUE.equals(callStatic("com.hiro.ulike.CaptureYuv","isReady",new Class<?>[0]))?planned:0;
        }catch(ReflectiveOperationException unavailable){return 0;}
        catch(RuntimeException unavailable){return 0;}
        catch(LinkageError unavailable){return 0;}
        catch(OutOfMemoryError unavailable){return 0;}
    }
    /** Invoked on the single capture-handoff worker reserved by submitAuto. */
    public static void captureTask(Object task){
        final Ticket ticket; synchronized(tickets){ticket=tickets.remove(task);}
        if(ticket==null)return;
        Bitmap owned=null;boolean ownsInput=false,serialOnly=false,submitted=false;
        ProcessingTiming1947.Trace timing=null;
        ProcessingTiming1947.Scope timingScope=null;
        try{
            Object manager=field(task,"k");
            Object store=callStatic("i.f.l.n.s.a","b",new Class<?>[0]);
            Bitmap source=(Bitmap)call(store,"e",new Class<?>[0]);
            if(source==null||source.isRecycled())throw new IllegalStateException("capture bitmap missing");
            timing=ProcessingTiming1947.forKey(source);
            if(timing==null)timing=ProcessingTiming1947.begin(source);
            timingScope=ProcessingTiming1947.enter(timing);
            // Native watermark d() was audited: it allocates a separate ARGB_8888
            // canvas and only reads both inputs. For software ARGB_8888 the exact
            // source-copy followed by that draw is redundant; draw while the SDK
            // shutter still holds the source, then own only the resulting bitmap.
            Bitmap watermark=(Bitmap)callStatic("i.f.l.n.q.y.n","f",new Class<?>[0]);
            Bitmap.Config config=source.getConfig();
            boolean directMark=watermark!=null&&canDirectMark(source,config);
            if(directMark){
                owned=mark(source,watermark,task);
                if(owned==source)throw new IllegalStateException("watermark ownership ABI");
                ownsInput=true;ShotContext1932.copy(source,owned);
            }else{
                // Keep a pristine exact source for normalization rollback. The
                // SDK/global holder may be overwritten by the next shutter.
                if(config==null || config==Bitmap.Config.HARDWARE)config=Bitmap.Config.ARGB_8888;
                long pixels=(long)source.getWidth()*source.getHeight();
                if(SaveQueue1935.memoryAllows(available(),pixels)){
                    try{owned=source.copy(config,false);}catch(OutOfMemoryError unavailable){}
                }
                if(owned==null){owned=source;serialOnly=true;}else ownsInput=true;
                ShotContext1932.copy(source,owned);
                if(watermark!=null){
                    Bitmap marked=mark(owned,watermark,task);
                    if(marked!=owned){ShotContext1932.copy(owned,marked);if(ownsInput)QualityPipeline1932.recycle1954(owned);owned=marked;ownsInput=true;}
                }
            }
            Class<?> controllerClass=Class.forName("i.o.a.q.c.c.b.e"),listener=Class.forName("i.o.a.q.c.c.b.e$c");
            Object controller=controllerClass.getConstructor(listener).newInstance(field(manager,"c"));
            Class<?> notifyClass=Class.forName("i.o.a.q.c.c.b.e$a");
            Constructor<?> construct=notifyClass.getConstructor(controllerClass,Bitmap.class,int.class,int.class,boolean.class);
            Object notification=construct.newInstance(controller,owned,field(task,"j"),field(task,"c"),false);
            Job job=new Job(task,owned,controller,notification,ticket,ownsInput,serialOnly);lastPixels=job.pixels;
            // Preserve native analytics/start marker without submitting its
            // shared-controller save task a second time.
            try{Object state=staticField("i.o.a.b0.f.a","a");call(state,"d",new Class<?>[]{boolean.class},false);Object marker=staticField("i.o.a.p.b","a");call(marker,"f",new Class<?>[0]);}catch(ReflectiveOperationException ignored){}
            synchronized(job){
                SaveQueue1935.submit(job,job.pixels,job.anticipatedBytes);submitted=true;owned=null;
                // All SDK/global data has been copied, watermark has finished,
                // and the immutable options/context/controller are isolated.
                if(!job.serialOnly&&SaveQueue1935.memoryAllows(available(),job.pixels))releaseCapture(job);
            }
        }catch(Exception failure){
            if(!submitted){ProcessingTiming1947.finish(timing,false);failedCapture(task,ownsInput?owned:null,ticket.id);}
        }catch(OutOfMemoryError failure){
            // Once queued, the Job owns bitmap/lease cleanup. An optional early
            // admission probe must never release the reservation a second time.
            if(!submitted){ProcessingTiming1947.finish(timing,false);failedCapture(task,ownsInput?owned:null,ticket.id);}
        }finally{if(timingScope!=null)ProcessingTiming1947.restore(timingScope);}
    }
    private static boolean canDirectMark(Bitmap source,Bitmap.Config config){
        if(config!=Bitmap.Config.ARGB_8888||!source.isPremultiplied())return false;
        // Keep Bitmap.copy semantics for extended HDR gain-map sources.
        try{return !Boolean.TRUE.equals(call(source,"hasGainmap",new Class<?>[0]));}
        catch(NoSuchMethodException oldApi){return true;}
        catch(ReflectiveOperationException uncertain){return false;}
    }
    private static Bitmap mark(Bitmap image,Bitmap watermark,Object task)throws ReflectiveOperationException{
        ProcessingTiming1947.Token timing=ProcessingTiming1947.beginStage(ProcessingTiming1947.traceFor(image),ProcessingTiming1947.CORRECTION);
        try{
            Bitmap result=(Bitmap)callStatic("i.p.a.t.e","d",new Class<?>[]{Bitmap.class,Bitmap.class,int.class,double.class,double.class},
                image,watermark,field(task,"c"),Double.longBitsToDouble(4595124781300318208L),Double.longBitsToDouble(4585925428429979648L));
            if(result==null)throw new IllegalStateException("watermark bitmap missing");return result;
        }finally{ProcessingTiming1947.end(timing);}
    }
    /** Invoked after normalize/detail has completely produced the encoder bitmap. */
    public static void encoding(Bitmap finalBitmap){
        Job job=CURRENT.get();
        if(job==null||finalBitmap==null||finalBitmap.isRecycled())return;
        ProcessingTiming1947.bind(finalBitmap,job.timing);
        // Hardware ownership is released only after proven close; file publication
        // and terminal receipts keep their separate ordered ownership.
        SaveQueue1935.beginEncoding(job);
        if(job.codecTask!=null)executeCodecPreparation(job);
        SaveQueue1935.nativeCost(job,job.inputBytes+(finalBitmap==job.input?0:bitmapBytes(finalBitmap)));
        if(!job.serialOnly&&!job.released&&SaveQueue1935.memoryAllows(available(),job.pixels))releaseCapture(job);
    }
    /** Invoked from CodecDrain only after an owned writer closed successfully,
     * its callback barrier drained and the real hardware permit was released.
     * Early preparation/abort on another thread has no active save Job here. */
    public static void hardwareClosed1956(){
        Job job=CURRENT.get();
        if(job!=null&&SaveQueue1935.hardwareClosed1956(job))job.hardwareCompleted1956=true;
    }
    /** Entry hook at the reviewed pending-MediaStore and fallback publish paths.
     * Nested calls for the same Job are idempotent. Standalone editing keeps its
     * existing behavior. This never creates a success receipt or publishes data. */
    public static void awaitPublication1956(){
        Job job=CURRENT.get();
        if(job!=null)SaveQueue1935.awaitPublication1956(job);
    }
    /** Existing preparation task with its exact output186 dimensions/options.
     * Only an idle codec may prepare during correction. This never releases
     * the correction slot or admits another native SDK render. */
    public static void prepareCodec(ExecutorService executor,Runnable task){
        if(executor==null||task==null)throw new NullPointerException("codec preparation");
        Job job=CURRENT.get();
        if(job==null){
            final ProcessingTiming1947.Trace trace=ProcessingTiming1947.traceFor(task);
            if(trace==null){executor.execute(task);return;}
            executor.execute(new Runnable(){public void run(){
                ProcessingTiming1947.Scope timing=ProcessingTiming1947.enter(trace);
                try{task.run();}finally{ProcessingTiming1947.restore(timing);}
            }});
            return;
        }
        if(job.codecTask!=null)throw new IllegalStateException("duplicate codec preparation");
        job.codecExecutor=executor;job.codecTask=task;
        if(SaveQueue1935.tryCodecPreparation(job))executeCodecPreparation(job);
    }
    private static void executeCodecPreparation(Job job){
        final Runnable task=job.codecTask;
        final CountDownLatch complete=new CountDownLatch(1);
        ExecutorService executor=job.codecExecutor;
        job.codecTask=null;job.codecExecutor=null;job.codecPreparation=complete;
        try{executor.execute(new Runnable(){public void run(){
            ProcessingTiming1947.Scope timing=ProcessingTiming1947.enter(job.timing);
            try{task.run();}finally{ProcessingTiming1947.restore(timing);complete.countDown();}
        }});}
        catch(RuntimeException failure){complete.countDown();throw failure;}
        catch(Error failure){complete.countDown();throw failure;}
    }
    private static void finishCodecPreparation(Job job){
        boolean interrupted=false;
        CountDownLatch complete=job.codecPreparation;
        if(complete!=null){for(;;)try{complete.await();break;}catch(InterruptedException wait){interrupted=true;}}
        job.codecTask=null;job.codecExecutor=null;job.codecPreparation=null;
        // A proven close belongs to this exact encoding Job. A second global
        // barrier after early release could observe the next photo's callbacks.
        try{if(!job.hardwareCompleted1956)CodecDrain1945.awaitClosed();}
        catch(RuntimeException unavailable){codecFailure(job);}
        catch(LinkageError unavailable){codecFailure(job);}
        catch(OutOfMemoryError unavailable){codecFailure(job);}
        finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    private static void codecFailure(Job job){
        try{set(job.controller,"b","");set(job.controller,"g","保存に失敗しました。");}
        catch(ReflectiveOperationException ignored){}
        catch(RuntimeException ignored){}
        catch(LinkageError ignored){}
        catch(OutOfMemoryError ignored){}
    }
    private static void releaseCapture(final Job job){
        synchronized(job){if(job.released)return;job.released=true;}
        boolean posted=false;
        try {posted=main(new Runnable(){public void run(){
            synchronized(tickets){
                if(job.id!=serial)return;
                captureOpen=true;
                // Do not resume/reset a newer capture through an old callback.
                invokeCaptureFinish(job.callback);
            }
        }});}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        if(!posted)synchronized(job){job.released=false;}
    }
    private static boolean finishOnMain(final Job job){
        return main(new Runnable(){public void run(){
            Object previous=null;boolean bound=false;
            try{
                if(!job.released){
                    job.released=true;
                    synchronized(tickets){if(job.id==serial){captureOpen=true;invokeCaptureFinish(job.callback);}}
                }
                // Native completion reads manager.a for its dimensions and cost.
                // Bind only within one main-thread callback. Main FIFO order and
                // private controllers prevent previous/next receipt confusion.
                previous=field(job.manager,"a");set(job.manager,"a",job.controller);bound=true;
                call(job.notification,"c",new Class<?>[0]);
            }catch(ReflectiveOperationException ignored){showFailure();}
            finally{
                try {if(bound)set(job.manager,"a",previous);}
                catch(ReflectiveOperationException ignored){}
                finally {ExitJobs185.end();}
            }
        }});
    }
    private static void failedCapture(Object task,Bitmap owned,long captureId){
        try {QualityPipeline1932.recycle1954(owned);}
        finally {submissionFailure(task,captureId,true,true);}
    }
    private static void submissionFailure(Object task,long captureId,boolean reserved,boolean exitHeld){
        boolean posted=false;
        try {if(task!=null)posted=failureOnMain(task,captureId,exitHeld);else showFailure();}
        finally {
            if(reserved)releaseReservation(exitHeld&&!posted);
            else if(exitHeld&&!posted)ExitJobs185.end();
        }
    }
    private static void releaseReservation(boolean exitHeld){
        try {SaveQueue1935.release();}finally {if(exitHeld)ExitJobs185.end();}
    }
    /** A failed preparation owns only its own shutter generation. Keep its
     * exit guard until the queued main callback completes, just like a save
     * receipt, and never reset a newer capture through an old failure. */
    private static boolean failureOnMain(final Object task,final long captureId,final boolean exitHeld){
        try {return main(new Runnable(){public void run(){
            try {
                synchronized(tickets){
                    if(captureId>0&&captureId==serial){
                        captureOpen=true;
                        try{invokeCaptureFinish(field(field(task,"k"),"b"));}catch(ReflectiveOperationException ignored){}
                    }
                }
                showFailure();
            } finally {if(exitHeld)ExitJobs185.end();}
        }});}catch(RuntimeException unavailable){return false;}catch(LinkageError unavailable){return false;}catch(OutOfMemoryError unavailable){return false;}
    }
    private static void showFailure(){
        try{SaveQuality2.showSaveFailure();}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static void invokeCaptureFinish(Object callback){
        if(callback==null)return;
        try{call(callback,"a",new Class<?>[0]);}catch(ReflectiveOperationException ignored){}
    }
    private static boolean main(Runnable task){
        try{return new Handler(Looper.getMainLooper()).post(task);}
        catch(RuntimeException unavailable){return false;}
        catch(LinkageError unavailable){return false;}
        catch(OutOfMemoryError unavailable){return false;}
    }
    private static long bitmapBytes(Bitmap bitmap){
        try{long bytes=((Integer)call(bitmap,"getAllocationByteCount",new Class<?>[0])).intValue();if(bytes>0)return bytes;}
        catch(ReflectiveOperationException unknown){}
        return Math.max(0,(long)bitmap.getWidth()*bitmap.getHeight()*8L);
    }
    private static long available(){
        Runtime r=Runtime.getRuntime();long heap=Math.max(0,r.maxMemory()-(r.totalMemory()-r.freeMemory()));
        try{
            Object app=callStatic("com.hiro.ulike.SaveQuality2","app186",new Class<?>[0]);
            if(app==null)return heap;
            Object manager=call(app,"getSystemService",new Class<?>[]{String.class},"activity");if(manager==null)return heap;
            Class<?> infoClass=Class.forName("android.app.ActivityManager$MemoryInfo");Object info=infoClass.getConstructor().newInstance();
            call(manager,"getMemoryInfo",new Class<?>[]{infoClass},info);
            if(Boolean.TRUE.equals(field(info,"lowMemory")))return 0;
            long free=((Long)field(info,"availMem")).longValue(),threshold=((Long)field(info,"threshold")).longValue();
            return Math.min(heap,Math.max(0,free-threshold));
        }catch(ReflectiveOperationException unavailable){return heap;}
        catch(RuntimeException unavailable){return heap;}
        catch(LinkageError unavailable){return heap;}
        catch(OutOfMemoryError unavailable){return 0;}
    }
    private static Object staticField(String owner,String name)throws ReflectiveOperationException{return ReflectionCache1945.declaredField(ReflectionCache1945.type(owner),name).get(null);}
    private static Object field(Object owner,String name)throws ReflectiveOperationException{return ReflectionCache1945.field(owner.getClass(),name).get(owner);}
    private static void set(Object owner,String name,Object value)throws ReflectiveOperationException{ReflectionCache1945.field(owner.getClass(),name).set(owner,value);}
    private static Object call(Object owner,String name,Class<?>[] types,Object...args)throws ReflectiveOperationException{return ReflectionCache1945.method(owner.getClass(),name,types).invoke(owner,args);}
    private static Object callStatic(String owner,String name,Class<?>[] types,Object...args)throws ReflectiveOperationException{return ReflectionCache1945.method(ReflectionCache1945.type(owner),name,types).invoke(null,args);}
}
