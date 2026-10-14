package com.hiro.ulike;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import com.ss.android.vesdk.VECameraCapture;
import com.ss.android.vesdk.frame.TECapturePipeline;
import java.lang.ref.WeakReference;

/** Validate real preview inputs, and distinguish metadata from delivered image frames. */
public final class PreviewInputs1929 {
    private PreviewInputs1929() {}
    private static volatile Delivery delivery;
    private static final class Delivery {
        final WeakReference<Object> provider; final WeakReference<OpticalZoom.Route> route; final long epoch;
        volatile long time;
        Delivery(Object p,OpticalZoom.Route r,long now){provider=new WeakReference<>(p);route=new WeakReference<>(r);epoch=r.epoch;time=now;}
    }
    private static boolean rear(VECameraCapture c)throws ReflectiveOperationException {
        Object owner=ManualLens170.get("capture"), route=ManualLens170.get("active");
        return c!=null&&owner instanceof WeakReference&&((WeakReference<?>)owner).get()==c
                &&ManualLens170.yes("foreground")&&route instanceof OpticalZoom.Route
                &&((OpticalZoom.Route)route).rear&&!((OpticalZoom.Route)route).failed
                &&((OpticalZoom.Route)route).epoch==ManualLens170.number("epoch");
    }
    /** Background already advances the epoch. Preserve any new route opened before
     * onForeground, but reset the bounded recovery budget for the new foreground. */
    public static void foreground(Object c) {
        try {
            synchronized(ManualLens170.get("LOCK")) {
                Object owner=ManualLens170.get("capture");
                if(!(owner instanceof WeakReference)||((WeakReference<?>)owner).get()!=c||ManualLens170.yes("foreground"))return;
                ManualLens170.put("attempt",Integer.valueOf(0));
                ManualLens170.put("failureReason","");
                ManualLens170.put("queueUntil",Long.valueOf(0));
                ManualLens170.put("readyUntil",Long.valueOf(0));
                Object rejected=ManualLens170.get("rejected");
                if(rejected instanceof java.util.Set)((java.util.Set<?>)rejected).clear();
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError e){Log.w("ULikeInputs1929","Foreground recovery reset could not complete",e);}
    }
    /** Native startPreview entry: refresh a released texture using its own GL bridge. */
    public static boolean before(Object object) {
        if(!(object instanceof VECameraCapture))return true;
        VECameraCapture c=(VECameraCapture)object;
        try { return !rear(c)||ready(c,true); }
        catch(ReflectiveOperationException|RuntimeException|LinkageError e){Log.w("ULikeInputs1929","Preview inputs not ready",e);return false;}
    }
    public static boolean sourcesReady(VECameraCapture c) {
        try{return ready(c,false);}
        catch(ReflectiveOperationException|RuntimeException|LinkageError e){return false;}
    }
    private static boolean ready(VECameraCapture c,boolean refresh)throws ReflectiveOperationException {
        if(c==null||c.n==null||c.n.isEmpty())return false;
        boolean validPreview=false,renew=false;
        for(TECapturePipeline p:c.n.getImmutableList()) {
            if(p==null||!p.isPreview())continue;
            if(!p.isValid())return false;
            Object format=ProviderLifecycle1929.call(p,"getFormat");
            String name=format instanceof Enum?((Enum<?>)format).name():"";
            Object texture=ProviderLifecycle1929.call(p,"getSurfaceTexture");
            boolean released=Build.VERSION.SDK_INT>=26&&texture instanceof SurfaceTexture&&((SurfaceTexture)texture).isReleased();
            if(name.equals("PIXEL_FORMAT_OpenGL_OES")) {
                if(texture!=null)renew|=released;
                else {Object s=ProviderLifecycle1929.call(p,"getSurface");if(!(s instanceof Surface)||!((Surface)s).isValid())return false;}
            } else if(name.equals("PIXEL_FORMAT_Recorder")) {
                Object s=ProviderLifecycle1929.call(p,"getRecorderSurface");
                if(released||!(s instanceof Surface)||!((Surface)s).isValid())return false;
            }
            validPreview=true;
        }
        if(validPreview&&renew&&refresh) {
            if(!c.p.get()||ManualLens170.yes("recording")||ExitBusy1921.captureBusy(false))return false;
            c.newSurfaceTexture(); // existing native creation + renderer notification, no fabricated GL objects
            Log.i("ULikeInputs1929","Released rear preview texture scheduled for native renewal");
        }
        return validPreview;
    }
    /** Called only after the native provider delivered a non-null image to its listener. */
    public static void pixel(Object provider,Object frame) {
        if(provider==null||frame==null)return;
        try {
            OpticalZoom.Route route=(OpticalZoom.Route)ManualLens170.get("active");
            if(route==null||!route.rear||route.failed||route.epoch!=ManualLens170.number("epoch")||!ManualLens170.yes("foreground"))return;
            Delivery d=delivery;long now=SystemClock.uptimeMillis();
            Object mode=route.mode.get(), camera=mode==null?null:OpticalZoom.field(mode,"g");
            if(camera==null||ProviderLifecycle1929.get(provider,"d")!=camera
                    ||Boolean.TRUE.equals(ProviderLifecycle1929.get(provider,"h"))
                    ||!Boolean.TRUE.equals(ProviderLifecycle1929.get(provider,"e")))return;
            Object manager=ProviderLifecycle1929.call(camera,"U"), listener=ProviderLifecycle1929.get(provider,"a");
            if(ProviderLifecycle1929.call(manager,"h")!=provider||listener==null
                    ||listener==ProviderLifecycle1929.get(provider,"j")||listener==ProviderLifecycle1929.get(provider,"k"))return;
            if(d!=null&&d.provider.get()==provider&&d.route.get()==route&&d.epoch==route.epoch)d.time=now;
            else delivery=new Delivery(provider,route,now);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){/* never disturb the frame callback */}
    }
    /** Existing watchdog only: no new restart loop or additional camera client. */
    public static long deliveredTime() {
        try { return deliveredTimeChecked(); }
        catch(ReflectiveOperationException|RuntimeException|LinkageError e){return 0L;}
    }
    private static long deliveredTimeChecked()throws ReflectiveOperationException {
        long metadata=ManualLens170.number("lastFrame");
        OpticalZoom.Route r=(OpticalZoom.Route)ManualLens170.get("active");
        if(r==null||!r.rear)return metadata;
        Object mode=r.mode.get(),camera=mode==null?null:OpticalZoom.field(mode,"g");
        Object provider=ProviderLifecycle1929.call(ProviderLifecycle1929.call(camera,"U"),"h");
        if(provider==null)return 0L;
        Object format=ProviderLifecycle1929.get(provider,"b"),texture=ProviderLifecycle1929.call(provider,"f");
        // External Surface-only consumers do not send provider image callbacks.
        // Do not invent a missing-frame failure for those or non-texture modes.
        if(!(format instanceof Enum)||!((Enum<?>)format).name().equals("PIXEL_FORMAT_OpenGL_OES")||texture==null)return metadata;
        Delivery d=delivery;
        return d!=null&&d.route.get()==r&&d.epoch==r.epoch&&d.provider.get()==provider?Math.min(metadata,d.time):0L;
    }
}
