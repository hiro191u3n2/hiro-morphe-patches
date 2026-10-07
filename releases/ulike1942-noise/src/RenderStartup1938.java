package com.hiro.ulike;

import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import com.ss.android.vesdk.VEListener;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Supervise the OUTPUT renderer independently of camera input-frame receipt.
 * Only a stopped native renderer (state 0/1 and no GL environment) is replayed.
 * Native SDK creates the GL environment/textures and queues work on its executor.
 * No camera switch, Surface construction, GL attach, or active renderer restart.
 */
public final class RenderStartup1938 {
    static final long WAIT_MS=8000L, GRACE_MS=1800L, POLL_MS=150L;
    private static final String TAG="ULikeRender1938";
    private static final Object LOCK=new Object();
    private static final Map<Object,Pending> pending=new WeakHashMap<>();
    private RenderStartup1938() {}
    private static Object get(Object owner,String name)throws ReflectiveOperationException {
        return ProviderLifecycle1929.get(owner,name);
    }
    private static Object call(Object owner,String name)throws ReflectiveOperationException {
        return ProviderLifecycle1929.call(owner,name);
    }
    private static boolean yes(Object owner,String name)throws ReflectiveOperationException {
        return Boolean.TRUE.equals(get(owner,name));
    }
    private static int number(Object owner,String name)throws ReflectiveOperationException {
        return ((Number)get(owner,name)).intValue();
    }
    private static void drop(Object recorder,Pending p) {
        synchronized(LOCK){if(pending.get(recorder)==p)pending.remove(recorder);}
        OpticalZoom.MAIN.removeCallbacks(p);
    }
    private static void schedule(Pending p,long delay) {
        OpticalZoom.MAIN.removeCallbacks(p);
        if(!OpticalZoom.MAIN.postDelayed(p,delay)) {
            Object recorder=p.recorder.get();if(recorder!=null)drop(recorder,p);
        }
    }
    /** Entry before native start preserves a renderer-created callback that can arrive
     * synchronously. Repeated/native-replay requests never renew an existing deadline. */
    public static void requested(Object recorder,Surface surface) {
        if(recorder==null || surface==null)return;
        synchronized(LOCK) {
            Pending old=pending.get(recorder);
            if(old!=null && old.surface.get()==surface)return;
            if(old!=null)drop(recorder,old);
            Pending p=new Pending(recorder,surface,SystemClock.uptimeMillis());
            pending.put(recorder,p);schedule(p,POLL_MS);
        }
    }
    /** A recovery replay never owns a fresh ticket, even if cancellation raced its
     * call into the native asynchronous API. */
    public static void requestedAsync(Object recorder,Surface surface,VEListener.VECallListener callback) {
        if(!(callback instanceof Replay))requested(recorder,surface);
    }
    /** Pause, explicit stop and destroy cancel even a queued replay. No rearming
     * comes from camera input frames, so user stop can never be mistaken for failure. */
    public static void cancelled(Object recorder) {
        synchronized(LOCK){Pending p=pending.get(recorder);if(p!=null)drop(recorder,p);}
    }
    /** 1000 is verified native 'Render Env Created'; 1001 means destroyed.
     * The camera's onFrameCaptured callback is deliberately not used here. */
    public static void event(Object recorder,int event) {
        if(event!=1000 && event!=1001)return;
        synchronized(LOCK) {
            Pending p=pending.get(recorder);if(p==null)return;
            schedule(p,POLL_MS);
        }
    }
    /** Native $11.run calls this immediately before startRecordPreview. All normal
     * SDK requests pass unchanged; only our marker callback is ticket-gated. No View
     * reads occur here, because the SDK executes this on its own command thread. */
    public static boolean admitted(VEListener.VECallListener callback) {
        if(!(callback instanceof Replay))return true;
        try {
            Pending p=((Replay)callback).pending;
            Object r=p.recorder.get();Surface s=p.surface.get();
            boolean busy=ExitBusy1921.captureBusy(false);
            synchronized(LOCK) {
                if(r==null || pending.get(r)!=p || p.admitted)return false;
                if(SystemClock.uptimeMillis()>=p.deadline || busy || s==null || !s.isValid()
                        || !ManualLens170.yes("foreground") || ManualLens170.yes("recording")
                        || ManualLens170.number("epoch")!=p.epoch || yes(r,"t1") || yes(r,"a1")
                        || yes(r,"mRenderEnvActive")) {drop(r,p);return false;}
                Object c=call(r,"getCurrentCameraCapture"), rv=call(r,"getRenderView");
                Object owner=ManualLens170.get("capture");
                if(c!=p.capture.get() || rv!=p.renderView.get() || !(owner instanceof WeakReference)
                        || ((WeakReference<?>)owner).get()!=c || call(rv,"getSurface")!=s) {drop(r,p);return false;}
                Object facing=call(get(c,"a"),"getCameraFacing");
                int state=number(r,"mCurRecordStatus");
                if(!(facing instanceof Enum) || !((Enum<?>)facing).name().equals("FACING_FRONT")
                        || state!=0 && state!=1) {drop(r,p);return false;}
                p.admitted=true;return true;
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported) {
            Log.w(TAG,"Rejected renderer replay with unavailable ownership",unsupported);return false;
        }
    }
    static final class Replay implements VEListener.VECallListener {
        final Pending pending;
        Replay(Pending p){pending=p;}
        public void onDone(int result){schedule(pending,POLL_MS);}
    }
    static final class Pending implements Runnable {
        final WeakReference<Object> recorder;
        final WeakReference<Surface> surface;
        final long started,deadline;
        WeakReference<Object> capture,renderView;
        WeakReference<SurfaceView> view;
        long epoch;
        boolean bound,replayed,admitted;
        Pending(Object recorder,Surface surface,long now) {
            this.recorder=new WeakReference<>(recorder);this.surface=new WeakReference<>(surface);
            started=now;deadline=now+WAIT_MS;
        }
        private boolean current(Object r,Surface s)throws ReflectiveOperationException {
            if(!ManualLens170.yes("foreground") || yes(r,"t1") || yes(r,"a1"))return false;
            Object c=call(r,"getCurrentCameraCapture");
            Object owner=ManualLens170.get("capture");
            if(c==null || !(owner instanceof WeakReference) || ((WeakReference<?>)owner).get()!=c)return false;
            Object facing=call(get(c,"a"),"getCameraFacing");
            if(!(facing instanceof Enum) || !((Enum<?>)facing).name().equals("FACING_FRONT"))return false;
            Object rv=call(r,"getRenderView");
            if(rv==null || call(rv,"getSurface")!=s)return false;
            Object output=call(rv,"getSurfaceView");
            if(!(output instanceof SurfaceView))return false;
            SurfaceView v=(SurfaceView)output;
            if(!v.isAttachedToWindow() || !v.isShown() || !v.hasWindowFocus()
                    || v.getWidth()<=0 || v.getHeight()<=0 || !s.isValid()
                    || v.getHolder().getSurface()!=s)return false;
            long e=ManualLens170.number("epoch");
            if(!bound) {
                capture=new WeakReference<>(c);renderView=new WeakReference<>(rv);
                view=new WeakReference<>(v);epoch=e;bound=true;
            }
            return capture.get()==c && renderView.get()==rv && view.get()==v && epoch==e;
        }
        public void run() {
            Object r=recorder.get();Surface s=surface.get();if(r==null)return;
            synchronized(LOCK){if(pending.get(r)!=this)return;}
            if(Looper.myLooper()!=OpticalZoom.MAIN.getLooper()) {schedule(this,0L);return;}
            try {
                long now=SystemClock.uptimeMillis();
                if(s==null || now>=deadline || yes(r,"t1")) {drop(r,this);return;}
                // Surface/capture binding may lag the initial request. Once established,
                // any owner/foreground/surface change invalidates it permanently.
                if(!current(r,s)) {
                    if(bound || !ManualLens170.yes("foreground"))drop(r,this);else schedule(this,POLL_MS);
                    return;
                }
                int state=number(r,"mCurRecordStatus");boolean active=yes(r,"mRenderEnvActive");
                if((state==2 || state==3) && active) {drop(r,this);return;}
                if(replayed || now-started<GRACE_MS || state!=0 && state!=1 || active
                        || ManualLens170.yes("recording") || ExitBusy1921.captureBusy(false)) {
                    schedule(this,POLL_MS);return;
                }
                // A second foreground/native-state check immediately before the one
                // bounded replay closes races with stop/camera-switch callbacks.
                synchronized(LOCK){if(pending.get(r)!=this)return;}
                if(!current(r,s) || yes(r,"mRenderEnvActive")) {drop(r,this);return;}
                state=number(r,"mCurRecordStatus");if(state!=0 && state!=1){schedule(this,POLL_MS);return;}
                replayed=true;
                r.getClass().getMethod("startPreviewAsync",Surface.class,VEListener.VECallListener.class).invoke(r,s,new Replay(this));
                Log.i(TAG,"Replayed one stopped front renderer on its current valid surface");
                schedule(this,POLL_MS);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported) {
                drop(r,this);Log.w(TAG,"Optional output renderer recovery stopped",unsupported);
            }
        }
    }
}
