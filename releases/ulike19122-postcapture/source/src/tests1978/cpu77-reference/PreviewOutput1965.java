package com.hiro.ulike;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceView;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Observe the renderer's already-owned output Surface without changing it.
 * PixelCopy proves only that its most recently queued buffer was copyable;
 * neither a dark summary nor a copy error diagnoses/restarts the camera.
 * Destinations are tiny, temporary, and never saved or passed to tracing.
 */
public final class PreviewOutput1965 {
    private PreviewOutput1965() {}
    private static final int EDGE=16,MAX_IN_FLIGHT=2;
    private static final long FIRST_MS=200L,SECOND_MS=1000L,CALLBACK_MS=1500L;
    private static final Object LOCK=new Object();
    private static final Map<Object,Session> sessions=new WeakHashMap<Object,Session>();
    private static int inFlight;
    private static ThreadPoolExecutor executor;

    /** Repeated renderer polls cannot renew these two observations. */
    public static void observe(Object recorder,SurfaceView view,Surface surface,long epoch) {
        if(recorder==null||view==null||surface==null||Build.VERSION.SDK_INT<24)return;
        try {
            synchronized(LOCK) {
                Session old=sessions.get(recorder);
                if(old!=null&&!old.cancelled&&old.view.get()==view&&old.surface.get()==surface&&old.epoch==epoch&&old.sameOwner())return;
                if(old!=null)old.cancel();
                Session session=new Session(recorder,view,surface,epoch);
                sessions.put(recorder,session);
                if(!OpticalZoom.MAIN.post(session))drop(session);
            }
        }catch(ReflectiveOperationException unavailable){}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    /** A request already owned by PixelCopy completes before its bitmap is freed. */
    public static void cancelled(Object recorder) {
        if(recorder==null)return;
        try{synchronized(LOCK){Session session=sessions.remove(recorder);if(session!=null)session.cancel();}}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static ThreadPoolExecutor worker() {
        synchronized(LOCK){
            if(executor==null){
                executor=new ThreadPoolExecutor(1,1,30L,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1),new ThreadFactory(){
                    public Thread newThread(Runnable task){Thread thread=new Thread(task,"ULike-output-probe");thread.setDaemon(true);return thread;}
                },new ThreadPoolExecutor.AbortPolicy());
                executor.allowCoreThreadTimeOut(true);
            }
            return executor;
        }
    }
    private static void drop(Session session) {
        try{synchronized(LOCK){Object r=session.recorder.get();if(r!=null&&sessions.get(r)==session)sessions.remove(r);session.cancel();}}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static boolean registered(Session session) {
        synchronized(LOCK){Object r=session.recorder.get();return !session.cancelled&&r!=null&&sessions.get(r)==session;}
    }
    private static void event(Session session,String scalars) {
        try{CameraTrace1965.event("output_surface_probe",session.epoch,scalars);}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static final class Session implements Runnable {
        final WeakReference<Object> recorder;
        final WeakReference<SurfaceView> view;
        final WeakReference<Surface> surface;
        final long epoch,started;
        final Sample first,second;
        final WeakReference<Object> capture,renderView;
        volatile boolean cancelled;
        Session(Object recorder,SurfaceView view,Surface surface,long epoch)throws ReflectiveOperationException {
            this.recorder=new WeakReference<Object>(recorder);this.view=new WeakReference<SurfaceView>(view);
            this.surface=new WeakReference<Surface>(surface);this.epoch=epoch;started=SystemClock.uptimeMillis();
            // Pin the request owner before posting. A known-null owner stays
            // null rather than being silently rebound to a later camera.
            capture=new WeakReference<Object>(ProviderLifecycle1929.call(recorder,"getCurrentCameraCapture"));
            renderView=new WeakReference<Object>(ProviderLifecycle1929.call(recorder,"getRenderView"));
            first=new Sample(this,1);second=new Sample(this,2);
        }
        private boolean sameOwner()throws ReflectiveOperationException {
            Object r=recorder.get();
            return r!=null&&capture.get()==ProviderLifecycle1929.call(r,"getCurrentCameraCapture")&&
                renderView.get()==ProviderLifecycle1929.call(r,"getRenderView");
        }
        /** Worker gate uses ownership state only; no View or holder is read here. */
        private boolean workerCurrent()throws ReflectiveOperationException {
            if(!registered(this)||!ManualLens170.yes("foreground")||ManualLens170.number("epoch")!=epoch||!sameOwner())return false;
            Object r=recorder.get(),owner=ManualLens170.get("capture");Surface s=surface.get();
            return r!=null&&s!=null&&s.isValid()&&capture!=null&&capture.get()!=null&&
                owner instanceof WeakReference&&((WeakReference<?>)owner).get()==capture.get()&&
                !Boolean.TRUE.equals(ProviderLifecycle1929.get(r,"t1"))&&!Boolean.TRUE.equals(ProviderLifecycle1929.get(r,"a1"));
        }
        private boolean valid()throws ReflectiveOperationException {
            if(!registered(this)||Looper.myLooper()!=OpticalZoom.MAIN.getLooper()||
                    !ManualLens170.yes("foreground")||ManualLens170.number("epoch")!=epoch)return false;
            Object r=recorder.get();SurfaceView v=view.get();Surface s=surface.get();
            if(r==null||v==null||s==null||!s.isValid()||!v.isAttachedToWindow()||!v.isShown()||
                    v.getWidth()<1||v.getHeight()<1||v.getHolder().getSurface()!=s||
                    Boolean.TRUE.equals(ProviderLifecycle1929.get(r,"t1"))||
                    Boolean.TRUE.equals(ProviderLifecycle1929.get(r,"a1")))return false;
            Object c=ProviderLifecycle1929.call(r,"getCurrentCameraCapture"),rv=ProviderLifecycle1929.call(r,"getRenderView");
            Object owner=ManualLens170.get("capture");
            if(c==null||rv==null||!(owner instanceof WeakReference)||((WeakReference<?>)owner).get()!=c||
                    ProviderLifecycle1929.call(rv,"getSurface")!=s||ProviderLifecycle1929.call(rv,"getSurfaceView")!=v)return false;
            return capture!=null&&renderView!=null&&capture.get()==c&&renderView.get()==rv;
        }
        public void run() {
            if(!registered(this))return;
            try {
                if(!valid()){drop(this);return;}
                long now=SystemClock.uptimeMillis();
                if(!OpticalZoom.MAIN.postDelayed(first,Math.max(0L,started+FIRST_MS-now))||
                        !OpticalZoom.MAIN.postDelayed(second,Math.max(0L,started+SECOND_MS-now)))drop(this);
            }catch(ReflectiveOperationException unavailable){drop(this);}
             catch(RuntimeException unavailable){drop(this);}catch(LinkageError unavailable){drop(this);}catch(OutOfMemoryError unavailable){drop(this);}
        }
        void cancel() {
            cancelled=true;
            try{OpticalZoom.MAIN.removeCallbacks(this);OpticalZoom.MAIN.removeCallbacks(first);OpticalZoom.MAIN.removeCallbacks(second);}
            catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
            first.cancel();second.cancel();
        }
    }
    private static final class Sample implements Runnable,PixelCopy.OnPixelCopyFinishedListener {
        final Session session;final int ordinal;
        final Runnable deadline=new Runnable(){public void run(){expired();}};
        final Runnable copyTask=new Runnable(){public void run(){copy();}};
        volatile Bitmap destination;
        int width,height;
        volatile boolean issued,completed,expired;
        Sample(Session session,int ordinal){this.session=session;this.ordinal=ordinal;}
        public void run() {
            if(issued||!registered(session))return;issued=true;
            try {
                if(!session.valid()){drop(session);return;}
                SurfaceView view=session.view.get();width=view.getWidth();height=view.getHeight();
                synchronized(LOCK){
                    if(inFlight>=MAX_IN_FLIGHT){event(session,scalars("capacity_skipped",-1));return;}
                    inFlight++;
                }
                try {
                    destination=Bitmap.createBitmap(EDGE,EDGE,Bitmap.Config.ARGB_8888);
                    worker().execute(copyTask);
                }catch(RuntimeException unavailable){failed(false);}
                 catch(LinkageError unavailable){failed(false);}catch(OutOfMemoryError unavailable){failed(false);}
                if(!completed)try{
                    if(!OpticalZoom.MAIN.postDelayed(deadline,CALLBACK_MS))expired=true;
                }catch(RuntimeException unavailable){expired=true;}
                 catch(LinkageError unavailable){expired=true;}catch(OutOfMemoryError unavailable){expired=true;}
            }catch(ReflectiveOperationException unavailable){drop(session);}
             catch(RuntimeException unavailable){drop(session);}catch(LinkageError unavailable){drop(session);}catch(OutOfMemoryError unavailable){drop(session);}
        }
        private void copy() {
            Surface source;Bitmap bitmap;
            try {
                if(completed||expired||!session.workerCurrent()){finish();return;}
                source=session.surface.get();bitmap=destination;
                if(source==null||bitmap==null){finish();return;}
            }catch(ReflectiveOperationException unavailable){failed(false);return;}
             catch(RuntimeException unavailable){failed(false);return;}catch(LinkageError unavailable){failed(false);return;}catch(OutOfMemoryError unavailable){failed(false);return;}
            // Some older Android versions perform the native copy during
            // request(). Keep that work off MAIN using the exact Surface
            // already validated there, without reading any View off-thread.
            try{PixelCopy.request(source,bitmap,this,OpticalZoom.MAIN);}
            catch(IllegalArgumentException rejected){failed(false);}
            catch(RuntimeException unavailable){failed(true);}
            catch(LinkageError unavailable){failed(true);}catch(OutOfMemoryError unavailable){failed(true);}
        }
        private String scalars(String phase,int status) {
            return ("complete".equals(phase)?"":"phase="+phase+" ")+"n="+ordinal+" pixelcopy_status="+status+
                " view="+width+"x"+height+" probe="+EDGE+"x"+EDGE+
                " visible_preview_unverified=1";
        }
        /** Unknown failures after calling PixelCopy may still have native writes. */
        private void failed(final boolean mayOwnDestination) {
            if(mayOwnDestination)expired=true;else finish();
            try {
                Runnable report=new Runnable(){public void run(){
                    try{if(session.valid())event(session,scalars(mayOwnDestination?"request_exception_uncertain":"request_exception",mayOwnDestination?-4:-2));}
                    catch(ReflectiveOperationException unavailable){}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
                }};
                if(Looper.myLooper()==OpticalZoom.MAIN.getLooper())report.run();else OpticalZoom.MAIN.post(report);
            }catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        }
        void cancel() {
            cancelDeadline();
            try{ThreadPoolExecutor pool; synchronized(LOCK){pool=executor;}
                if(pool!=null&&pool.remove(copyTask))finish();}
            catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        }
        void cancelDeadline(){try{OpticalZoom.MAIN.removeCallbacks(deadline);}
            catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}}
        private void expired() {
            if(completed||expired)return;expired=true;
            try{if(session.valid())event(session,scalars("callback_deadline",-3));}
            catch(ReflectiveOperationException unavailable){}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
            // Android still owns the destination until its callback completes.
            // Recycling now would turn diagnostics into a native write-after-free.
            // The global two-slot cap also applies to these stalled requests.
        }
        public void onPixelCopyFinished(int result) {
            if(completed)return;
            try {
                if(expired||!session.valid())return;
                String fields=scalars("complete",result)+" buffer_copy_success="+(result==PixelCopy.SUCCESS?1:0);
                Bitmap bitmap=destination;
                if(result==PixelCopy.SUCCESS&&bitmap!=null) {
                    int[] pixels=new int[EDGE*EDGE];bitmap.getPixels(pixels,0,EDGE,0,0,EDGE,EDGE);
                    long sum=0;int min=255,max=0;
                    for(int pixel:pixels){int y=(77*((pixel>>>16)&255)+150*((pixel>>>8)&255)+29*(pixel&255)+128)>>8;
                        sum+=y;min=Math.min(min,y);max=Math.max(max,y);}
                    fields+=" mean_luma_q8="+(sum*256/pixels.length)+" min_luma="+min+" max_luma="+max;
                }
                event(session,fields);
            }catch(ReflectiveOperationException unavailable){}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
            finally{finish();}
        }
        private void finish() {
            Bitmap bitmap;
            synchronized(this){if(completed)return;completed=true;bitmap=destination;destination=null;}
            cancelDeadline();
            try{if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();}
            catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
            finally{synchronized(LOCK){if(inFlight>0)inFlight--;}}
        }
    }
}
