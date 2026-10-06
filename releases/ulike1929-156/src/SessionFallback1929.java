package com.hiro.ulike;

import android.hardware.camera2.CameraCaptureSession;
import android.os.Handler;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;

/** Let the existing YUV/normal-mode fallback submit before declaring a lens failed. */
public final class SessionFallback1929 {
    private SessionFallback1929() {}
    private static Object local(String name)throws ReflectiveOperationException {
        Field f=ManualLens170.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);
    }
    static boolean current(OpticalZoom.Route route,long token)throws ReflectiveOperationException {
        if(route==null||route.failed||route!=ManualLens170.get("active")||route.epoch!=ManualLens170.number("epoch")
                ||!ManualLens170.yes("foreground"))return false;
        Object lock=local("PINNED");synchronized(lock){return Long.valueOf(token).equals(((Map<?,?>)local("SESSION_TOKENS")).get(route));}
    }
    public static void failed(Object callback,CameraCaptureSession session) {
        try {
            final OpticalZoom.Route route=(OpticalZoom.Route)OpticalZoom.field(callback,"r");
            final long token=((Number)OpticalZoom.field(callback,"token")).longValue();
            CameraCaptureSession.StateCallback next=(CameraCaptureSession.StateCallback)OpticalZoom.field(callback,"next");
            boolean owned=current(route,token);
            Object lock=local("PINNED");synchronized(lock){
                ((Map<?,?>)lock).remove(session);
                if(owned) {
                    route.configured=false;
                    Map<?,?> sessions=(Map<?,?>)local("CURRENT_SESSIONS");
                    Object ref=sessions.get(route);
                    if(ref==null||(ref instanceof WeakReference&&((WeakReference<?>)ref).get()==session))sessions.remove(route);
                }
            }
            if(!owned){close(session);return;}
            // No route.failed flag is set here. The nested callbacks may synchronously
            // create a supported replacement with the same camera and a new token.
            try { if(next!=null)next.onConfigureFailed(session); }
            catch(RuntimeException|LinkageError e){Log.w("ULikeSession1929","Native configuration-failure callback failed",e);}
            finally{close(session);}
            if(!current(route,token))return;
            final WeakReference<OpticalZoom.Route> ref=new WeakReference<>(route);
            Runnable failure=new Runnable(){public void run(){
                try {OpticalZoom.Route r=ref.get();if(current(r,token)&&!r.failed)OpticalZoom.fail(r,"出力構成の復帰先がないため再接続します");}
                catch(ReflectiveOperationException|RuntimeException|LinkageError e){Log.w("ULikeSession1929","Stale configuration failure ignored",e);}
            }};
            // An asynchronously enqueued native fallback gets one camera queue turn.
            Object mode=route.mode.get(), h=mode==null?null:OpticalZoom.field(mode,"k");
            Handler handler=h instanceof Handler?(Handler)h:OpticalZoom.MAIN;
            if(!handler.post(failure))failure.run();
        } catch(ReflectiveOperationException|RuntimeException|LinkageError e) {
            close(session);Log.w("ULikeSession1929","Configuration-failure handoff failed",e);
        }
    }
    private static void close(CameraCaptureSession session){if(session!=null)try{session.close();}catch(RuntimeException ignored){}}
}
