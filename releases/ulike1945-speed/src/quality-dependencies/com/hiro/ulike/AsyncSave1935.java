package com.hiro.ulike;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.IdentityHashMap;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;

/** Pinned 5.6.2 auto-save route. SDK rendering stays serial. Only an owned,
 * already-rendered photo is queued; next real shutter is released once final
 * postprocessing is complete and the encoder owns that immutable photo. */
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
        Object task=null;boolean reserved=false,exitHeld=false;
        try{
            Class<?> owner=Class.forName("i.o.a.b1.a.b.f.a");
            task=Class.forName("i.o.a.b1.a.b.f.a$a").getConstructor(owner,int.class,int.class).newInstance(manager,direction,rotation);
            final Ticket ticket;
            synchronized(tickets){
                if(SaveQueue1935.count()>=SaveQueue1935.CAPACITY)throw new IllegalStateException("capture queue full");
                SaveQueue1935.reserve();reserved=true;ExitJobs185.begin();exitHeld=true;captureOpen=false;
                ticket=new Ticket(++serial);tickets.put(task,ticket);
            }
            final Object capturedTask=task;
            PREP.execute(new Runnable(){public void run(){captureTask(capturedTask);}});
        }catch(Exception failure){
            if(task!=null)synchronized(tickets){tickets.remove(task);}
            if(reserved)releaseReservation(exitHeld);
            if(task!=null)failureOnMain(task);else showFailure();
        }catch(OutOfMemoryError failure){
            if(task!=null)synchronized(tickets){tickets.remove(task);}
            if(reserved)releaseReservation(exitHeld);
            if(task!=null)failureOnMain(task);else showFailure();
        }
    }
    private static final class Job implements Runnable {
        final Object manager,callback,controller,notification;
        final Bitmap input;
        final int rotation,direction;
        final PhotoDetail.Settings detail;
        final boolean colour,fixed,ownsInput,serialOnly;
        final long pixels,id;
        boolean released;
        Job(Object task,Bitmap bitmap,Object controller,Object notification,Ticket ticket,boolean ownsInput,boolean serialOnly)throws ReflectiveOperationException{
            manager=field(task,"k");callback=field(manager,"b");this.controller=controller;this.notification=notification;
            rotation=((Integer)field(task,"j")).intValue();direction=((Integer)field(task,"c")).intValue();input=bitmap;
            detail=ticket.detail;colour=ticket.colour;fixed=ticket.fixed;id=ticket.id;this.ownsInput=ownsInput;this.serialOnly=serialOnly;
            ShotContext1932.Snapshot context=ShotContext1932.forBitmap(bitmap);
            pixels=Math.max((long)bitmap.getWidth()*bitmap.getHeight(),(long)context.sourceWidth*context.sourceHeight);
        }
        public void run(){
            try {
                CURRENT.set(this);
                set(controller,"g","");set(controller,"d",Long.valueOf(System.currentTimeMillis()));
                Object result=call(controller,"r",new Class<?>[]{Bitmap.class,int.class,int.class,boolean.class},input,rotation,direction,false);
                set(controller,"b",result instanceof String?result:"");
            } catch(Exception failure){
                try{set(controller,"b","");set(controller,"g","保存に失敗しました。");}catch(ReflectiveOperationException ignored){}
            } finally {
                try {CURRENT.remove();}
                finally {
                    try {
                        // Native s/r returns only after writer.close, validation
                        // and publication. The queue owns this bitmap alone.
                        if(ownsInput&&!input.isRecycled())input.recycle();
                    } finally {
                        try {finishOnMain(this);}
                        finally {releaseReservation(true);}
                    }
                }
            }
        }
    }
    public static PhotoDetail.Settings settings(PhotoDetail.Settings live){Job j=CURRENT.get();return j==null?live:j.detail;}
    public static boolean chroma(boolean live){Job j=CURRENT.get();return j==null?live:j.colour;}
    public static boolean fixed(boolean live){Job j=CURRENT.get();return j==null?live:j.fixed;}
    /** Wrap only the existing X.D1() call; its original busy/error callback is retained. */
    public static int readiness(Object camera){
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
            return currentPixels<=0 || !SaveQueue1935.memoryAllows(available(),Math.max(lastPixels,currentPixels));
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
            return Boolean.TRUE.equals(callStatic("com.hiro.ulike.CaptureYuv","isReady",new Class<?>[0]))?pixels:0;
        }catch(ReflectiveOperationException unavailable){return 0;}
        catch(RuntimeException unavailable){return 0;}
        catch(LinkageError unavailable){return 0;}
    }
    /** Invoked on the single capture-handoff worker reserved by submitAuto. */
    public static void captureTask(Object task){
        final Ticket ticket; synchronized(tickets){ticket=tickets.remove(task);}
        if(ticket==null)return;
        Bitmap owned=null;boolean ownsInput=false,serialOnly=false;
        try{
            Object manager=field(task,"k");
            Object store=callStatic("i.f.l.n.s.a","b",new Class<?>[0]);
            Bitmap source=(Bitmap)call(store,"e",new Class<?>[0]);
            if(source==null||source.isRecycled())throw new IllegalStateException("capture bitmap missing");
            // The renderer/global holder may be overwritten by the next shutter.
            // A full exact copy, with bitmap-bound metadata, gives this save sole ownership.
            Bitmap.Config config=source.getConfig();
            if(config==null || config==Bitmap.Config.HARDWARE)config=Bitmap.Config.ARGB_8888;
            // Retain the existing single-photo ownership when a full copy would
            // consume the processing reserve. The shutter stays closed until
            // encoding and publication end; no settings or quality are reduced.
            long pixels=(long)source.getWidth()*source.getHeight();
            if(SaveQueue1935.memoryAllows(available(),pixels)){
                try{owned=source.copy(config,false);}catch(OutOfMemoryError unavailable){}
            }
            if(owned==null){owned=source;serialOnly=true;}else ownsInput=true;
            ShotContext1932.copy(source,owned);
            Bitmap watermark=(Bitmap)callStatic("i.f.l.n.q.y.n","f",new Class<?>[0]);
            if(watermark!=null){
                Bitmap marked=(Bitmap)callStatic("i.p.a.t.e","d",new Class<?>[]{Bitmap.class,Bitmap.class,int.class,double.class,double.class},
                    owned,watermark,field(task,"c"),Double.longBitsToDouble(4595124781300318208L),Double.longBitsToDouble(4585925428429979648L));
                if(marked==null)throw new IllegalStateException("watermark bitmap missing");
                if(marked!=owned){ShotContext1932.copy(owned,marked);if(ownsInput)owned.recycle();owned=marked;ownsInput=true;}
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
            SaveQueue1935.submit(job);owned=null;
        }catch(Exception failure){
            failedCapture(task,ownsInput?owned:null);
        }catch(OutOfMemoryError failure){
            failedCapture(task,ownsInput?owned:null);
        }
    }
    /** Invoked after normalize/detail has completely produced the encoder bitmap. */
    public static void encoding(Bitmap finalBitmap){
        Job job=CURRENT.get();
        if(job==null||job.serialOnly||job.released||finalBitmap==null||finalBitmap.isRecycled())return;
        if(!SaveQueue1935.memoryAllows(available(),job.pixels))return;
        releaseCapture(job);
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
    private static void finishOnMain(final Job job){
        final CountDownLatch finished=new CountDownLatch(1);
        if(!main(new Runnable(){public void run(){
            Object previous=null;boolean bound=false;
            try{
                if(!job.released){
                    job.released=true;
                    synchronized(tickets){if(job.id==serial){captureOpen=true;invokeCaptureFinish(job.callback);}}
                }
                // Native completion reads manager.a for its dimensions and cost.
                // Temporarily bind the exact controller, and wait before the FIFO
                // processes another save. No previous/next receipt can be read.
                previous=field(job.manager,"a");set(job.manager,"a",job.controller);bound=true;
                call(job.notification,"c",new Class<?>[0]);
            }catch(ReflectiveOperationException ignored){showFailure();}
            finally{
                try {if(bound)set(job.manager,"a",previous);}
                catch(ReflectiveOperationException ignored){}
                finally {finished.countDown();}
            }
        }}))return;
        boolean interrupted=false;
        for(;;)try{finished.await();break;}catch(InterruptedException e){interrupted=true;}
        if(interrupted)Thread.currentThread().interrupt();
    }
    private static void failedCapture(Object task,Bitmap owned){
        try {if(owned!=null&&!owned.isRecycled())owned.recycle();}
        finally {try {failureOnMain(task);}finally {releaseReservation(true);}}
    }
    private static void releaseReservation(boolean exitHeld){
        try {SaveQueue1935.release();}finally {if(exitHeld)ExitJobs185.end();}
    }
    private static void failureOnMain(final Object task){
        try {main(new Runnable(){public void run(){
            try{invokeCaptureFinish(field(field(task,"k"),"b"));}catch(ReflectiveOperationException ignored){}
            showFailure();
        }});}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
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
    private static long available(){Runtime r=Runtime.getRuntime();return Math.max(0,r.maxMemory()-(r.totalMemory()-r.freeMemory()));}
    private static Object staticField(String owner,String name)throws ReflectiveOperationException{Field f=Class.forName(owner).getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    private static Object field(Object owner,String name)throws ReflectiveOperationException{for(Class<?> c=owner.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(NoSuchFieldException e){}throw new NoSuchFieldException(name);}
    private static void set(Object owner,String name,Object value)throws ReflectiveOperationException{for(Class<?> c=owner.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(owner,value);return;}catch(NoSuchFieldException e){}throw new NoSuchFieldException(name);}
    private static Object call(Object owner,String name,Class<?>[] types,Object...args)throws ReflectiveOperationException{Method m=owner.getClass().getMethod(name,types);m.setAccessible(true);return m.invoke(owner,args);}
    private static Object callStatic(String owner,String name,Class<?>[] types,Object...args)throws ReflectiveOperationException{Method m=Class.forName(owner).getMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}
}
