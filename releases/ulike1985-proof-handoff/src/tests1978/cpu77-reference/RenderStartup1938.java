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
 * Observation may follow a camera epoch rollover only while the exact capture,
 * render view and attached output Surface remain owned by this recorder. It
 * retains the original deadline and replay budget. No camera switch, Surface
 * construction, GL attach, or active renderer restart.
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
    private static void trace(String phase,long epoch,String fields) {
        try { CameraTrace1965.event(phase,epoch,fields); } catch(Throwable ignored) {}
    }
    private static void anomaly(String reason,long epoch,String fields) {
        try { CameraTrace1965.anomaly(reason,epoch,fields); } catch(Throwable ignored) {}
    }
    private static String identities(Object recorder,Surface surface) {
        return "recorder="+System.identityHashCode(recorder)+" surface="+System.identityHashCode(surface);
    }
    private static void drop(Object recorder,Pending p) {
        synchronized(LOCK){if(pending.get(recorder)==p)pending.remove(recorder);}
        OpticalZoom.MAIN.removeCallbacks(p);
    }
    private static void schedule(Pending p,long delay) {
        synchronized(LOCK) {
            Object recorder=p.recorder.get();
            if(recorder==null || pending.get(recorder)!=p)return;
            // An asynchronous native completion can arrive after stop/destroy
            // or replacement. It cannot enqueue another poll for that ticket.
            OpticalZoom.MAIN.removeCallbacks(p);
            if(!OpticalZoom.MAIN.postDelayed(p,delay))drop(recorder,p);
        }
    }
    /** Entry before native start preserves a renderer-created callback that can arrive
     * synchronously. Repeated/native-replay requests never renew an existing deadline. */
    public static void requested(Object recorder,Surface surface) {
        if(recorder==null || surface==null)return;
        try {
            // Camera switches commonly retain the same output Surface. The
            // camera/renderer lifetime, not Surface identity alone, owns a wait.
            Object capture=call(recorder,"getCurrentCameraCapture");
            Object renderView=call(recorder,"getRenderView");
            long epoch=ManualLens170.number("epoch");
            synchronized(LOCK) {
                Pending old=pending.get(recorder);
                if(old!=null && old.surface.get()==surface && old.sameRequest(capture,renderView,epoch))return;
                if(old!=null) {
                    trace("renderer_request_replaced",old.requestEpoch,identities(recorder,surface));
                    drop(recorder,old);
                }
                // A normal native request can accompany an internal camera reopen.
                // Same output owners keep the original recovery budget; repeated
                // epoch changes must not create an endless series of retries.
                boolean retained=old!=null && old.sameOutput(capture,renderView,surface);
                Pending p=new Pending(recorder,surface,retained?old.started:SystemClock.uptimeMillis(),capture,renderView,epoch);
                if(retained)p.replayed=old.replayed;
                pending.put(recorder,p);
                trace("renderer_requested",epoch,identities(recorder,surface));
                schedule(p,POLL_MS);
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported) {
            anomaly("renderer_request_ownership_unavailable",0L,identities(recorder,surface));
            Log.w(TAG,"Optional renderer observation unavailable; native request retained",unsupported);
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
        try { PreviewOutput1965.cancelled(recorder); } catch(Throwable ignored) {}
        long epoch=0L;try{epoch=ManualLens170.number("epoch");}catch(Throwable ignored){}
        trace("renderer_cancelled",epoch,"recorder="+System.identityHashCode(recorder));
    }
    /** 1000 is verified native 'Render Env Created'; 1001 means destroyed.
     * The camera's onFrameCaptured callback is deliberately not used here. */
    public static void event(Object recorder,int event) {
        if(event!=1000 && event!=1001)return;
        long epoch=0L;try{epoch=ManualLens170.number("epoch");}catch(Throwable ignored){}
        trace(event==1000?"render_env_created":"render_env_destroyed",epoch,
            "recorder="+System.identityHashCode(recorder)+" visible_confirmed=false");
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
                if(!supportedFacing(facing)
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
    private static boolean supportedFacing(Object facing) {
        if(!(facing instanceof Enum))return false;
        String name=((Enum<?>)facing).name();
        return name.equals("FACING_FRONT") || name.equals("FACING_BACK");
    }
    static final class Pending implements Runnable {
        final WeakReference<Object> recorder;
        final WeakReference<Surface> surface;
        final long started,deadline;
        final WeakReference<Object> requestCapture,requestRenderView;
        final long requestEpoch;
        final boolean requestCaptureKnown,requestRenderViewKnown;
        WeakReference<Object> capture,renderView;
        WeakReference<SurfaceView> view;
        long epoch;
        boolean bound,replayed,admitted,requestRejected;
        Pending(Object recorder,Surface surface,long now,Object capture,Object renderView,long epoch) {
            this.recorder=new WeakReference<>(recorder);this.surface=new WeakReference<>(surface);
            started=now;deadline=now+WAIT_MS;
            requestCapture=new WeakReference<>(capture);requestRenderView=new WeakReference<>(renderView);requestEpoch=epoch;
            requestCaptureKnown=capture!=null;requestRenderViewKnown=renderView!=null;
        }
        boolean sameRequest(Object capture,Object renderView,long epoch) {
            return requestEpoch==epoch && requestCaptureKnown==(capture!=null) && requestRenderViewKnown==(renderView!=null)
                && requestCapture.get()==capture && requestRenderView.get()==renderView;
        }
        boolean sameOutput(Object capture,Object renderView,Surface surface) {
            return requestCaptureKnown && requestRenderViewKnown && capture!=null && renderView!=null
                && requestCapture.get()==capture && requestRenderView.get()==renderView && this.surface.get()==surface;
        }
        private boolean current(Object r,Surface s)throws ReflectiveOperationException {
            return ownedOutput(r,s);
        }
        /** Front and rear share the same native output renderer contract. Camera
         * epoch changes alone do not destroy its retained attached Surface. */
        private boolean ownedOutput(Object r,Surface s)throws ReflectiveOperationException {
            if(!ManualLens170.yes("foreground") || yes(r,"t1") || yes(r,"a1"))return false;
            Object c=call(r,"getCurrentCameraCapture");
            Object owner=ManualLens170.get("capture");
            if(c==null || !(owner instanceof WeakReference) || ((WeakReference<?>)owner).get()!=c)return false;
            Object facing=call(get(c,"a"),"getCameraFacing");
            if(!supportedFacing(facing))return false;
            Object rv=call(r,"getRenderView");
            if(rv==null || call(rv,"getSurface")!=s)return false;
            Object output=call(rv,"getSurfaceView");
            if(!(output instanceof SurfaceView))return false;
            SurfaceView v=(SurfaceView)output;
            if(!v.isAttachedToWindow() || !v.isShown() || !v.hasWindowFocus()
                    || v.getWidth()<=0 || v.getHeight()<=0 || !s.isValid()
                    || v.getHolder().getSurface()!=s)return false;
            long e=ManualLens170.number("epoch");
            if(e!=(bound?epoch:requestEpoch)) {
                // An output request can precede an internal camera close/reopen.
                // Keep observing that exact live output under a new ticket; old
                // queued replay callbacks cannot acquire the replacement ticket.
                if(!sameOutput(c,rv,s) || bound && (capture.get()!=c || renderView.get()!=rv || view.get()!=v)) {
                    requestRejected=true;return false;
                }
                synchronized(LOCK) {
                    if(pending.get(r)!=this || SystemClock.uptimeMillis()>=deadline)return false;
                    Pending next=new Pending(r,s,started,c,rv,e);
                    next.replayed=replayed;
                    drop(r,this);pending.put(r,next);
                    trace("renderer_epoch_rebound",e,identities(r,s)+" previous_epoch="+(bound?epoch:requestEpoch)
                        +" deadline_retained=true replay_budget_retained=true native_start_cancelled=false");
                    schedule(next,0L);
                }
                return false;
            }
            if(!bound) {
                // Missing owners may bind late within the same epoch. A cleared
                // known owner or a different capture/render view remains stale.
                if(e!=requestEpoch || requestCaptureKnown && requestCapture.get()!=c
                        || requestRenderViewKnown && requestRenderView.get()!=rv) {
                    requestRejected=true;return false;
                }
                capture=new WeakReference<>(c);renderView=new WeakReference<>(rv);
                view=new WeakReference<>(v);epoch=e;bound=true;
                trace("renderer_bound",epoch,identities(r,s)+" view="+System.identityHashCode(v)
                    +" width="+v.getWidth()+" height="+v.getHeight()+" facing="+((Enum<?>)facing).name());
            }
            boolean valid=capture.get()==c && renderView.get()==rv && view.get()==v && epoch==e;
            if(valid)try{PreviewOutput1965.observe(r,v,s,e);}catch(Throwable ignored){}
            return valid;
        }
        public void run() {
            Object r=recorder.get();Surface s=surface.get();if(r==null)return;
            synchronized(LOCK){if(pending.get(r)!=this)return;}
            if(Looper.myLooper()!=OpticalZoom.MAIN.getLooper()) {schedule(this,0L);return;}
            try {
                long now=SystemClock.uptimeMillis();
                if(s==null || !s.isValid() || now>=deadline || yes(r,"t1")) {
                    if(now>=deadline)anomaly("output_wait_timeout",bound?epoch:requestEpoch,
                        identities(r,s)+" request_epoch="+requestEpoch+" visible_confirmed=false");
                    drop(r,this);return;
                }
                // Surface/capture binding may lag the initial request. Owner,
                // foreground or Surface changes invalidate it permanently.
                if(!current(r,s)) {
                    synchronized(LOCK){if(pending.get(r)!=this)return;}
                    if(bound || requestRejected || !ManualLens170.yes("foreground")) {
                        trace("renderer_stale_owner_rejected",requestEpoch,identities(r,s)+" observer_only=true native_start_cancelled=false");drop(r,this);
                    }else schedule(this,POLL_MS);
                    return;
                }
                int state=number(r,"mCurRecordStatus");boolean active=yes(r,"mRenderEnvActive");
                if((state==2 || state==3) && active) {
                    // The bounded restart helper has no useful work once native
                    // rendering is running. This is not evidence of a visible
                    // image: the independent Surface-buffer probe stays armed.
                    trace("renderer_native_active",epoch,identities(r,s)+" native_state="+state+" visible_confirmed=false");
                    drop(r,this);return;
                }
                if(replayed || now-started<GRACE_MS || state!=0 && state!=1 || active
                        || ManualLens170.yes("recording")) {
                    schedule(this,POLL_MS);return;
                }
                if(ExitBusy1921.captureBusy(false)) {
                    schedule(this,Scheduling1944.optionalRetryDelay(true,POLL_MS,now,deadline));return;
                }
                // A second foreground/native-state check immediately before the one
                // bounded replay closes races with stop/camera-switch callbacks.
                synchronized(LOCK){if(pending.get(r)!=this)return;}
                if(!current(r,s) || yes(r,"mRenderEnvActive")) {drop(r,this);return;}
                state=number(r,"mCurRecordStatus");if(state!=0 && state!=1){schedule(this,POLL_MS);return;}
                replayed=true;
                r.getClass().getMethod("startPreviewAsync",Surface.class,VEListener.VECallListener.class).invoke(r,s,new Replay(this));
                trace("renderer_stopped_replay_requested",epoch,identities(r,s)+" native_state="+state+" replay_limit=1");
                Log.i(TAG,"Replayed one stopped renderer on its current valid surface");
                schedule(this,POLL_MS);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported) {
                drop(r,this);Log.w(TAG,"Optional output renderer recovery stopped",unsupported);
            }
        }
    }
}
