package com.hiro.ulike;

import android.content.Context;
import android.os.SystemClock;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Scalar observation of the retained camera lifecycle. A request, input frame
 * notice and output buffer are deliberately separate observations. */
public final class CameraSession1965 {
    private static final Object LOCK=new Object();
    private static final WeakHashMap<Object,Sample> samples=new WeakHashMap<>();
    private static final long FRAME_SAMPLE_MS=5000L;
    private CameraSession1965() {}
    private static final class Sample {
        long epoch,time;
        final WeakReference<Object> capture;
        Sample(long epoch,long time,Object capture){this.epoch=epoch;this.time=time;this.capture=new WeakReference<>(capture);}
    }
    public static long epoch(){try{return ManualLens170.number("epoch");}catch(Throwable unavailable){return -1L;}}
    private static Object capture(){try{Object r=ManualLens170.get("capture");return r instanceof WeakReference?((WeakReference<?>)r).get():null;}catch(Throwable unavailable){return null;}}
    private static Object get(Object o,String n){try{return o==null?null:ProviderLifecycle1929.get(o,n);}catch(Throwable unavailable){return null;}}
    private static Object call(Object o,String n){try{return o==null?null:ProviderLifecycle1929.call(o,n);}catch(Throwable unavailable){return null;}}
    private static Object server(){try{return Class.forName("i.s.a.w.q").getField("INSTANCE").get(null);}catch(Throwable unavailable){return null;}}
    private static int identity(Object o){return o==null?0:System.identityHashCode(o);}
    private static String scalar(Object o){return o instanceof Number||o instanceof Boolean?o.toString():o instanceof Enum?((Enum<?>)o).name():"unknown";}
    public static void phase(String name,Object owner) {
        try {
            long e=epoch();Object c=capture();
            boolean foreground=false,recording=false;
            try{foreground=ManualLens170.yes("foreground");recording=ManualLens170.yes("recording");}catch(Throwable unavailable){}
            String facing=scalar(call(get(c,"a"),"getCameraFacing"));
            CameraTrace1965.event(name,e,"owner="+identity(owner)+" capture="+identity(c)+" facing="+facing+" foreground="+foreground+" recording="+recording);
            Object host=server(),camera=get(host,"mCameraInstance"),mode=get(camera,"M");
            Object provider=call(get(host,"mProviderManager"),"h");
            CameraTrace1965.event("native_state",e,"camera="+identity(camera)+" state="+scalar(get(host,"mCurrentCameraState"))+" switching="+scalar(get(host,"mIsCameraSwitchState"))+" closing="+scalar(get(host,"mIsCameraPendingClose"))+" background="+scalar(get(host,"mOnBackGround")));
            CameraTrace1965.event("native_owner",e,"handler_ended="+scalar(get(host,"mHandlerDestroyed"))+" start_error="+scalar(get(host,"mStartPreviewError"))+" client_owned="+(get(host,"mCameraClient")==get(c,"o"))+" settings_owned="+(get(host,"mCameraSettings")==get(c,"c")));
            CameraTrace1965.event("native_mode_ids",e,"mode="+identity(mode)+" session="+identity(get(mode,"d"))+" builder="+identity(get(mode,"c"))+" device="+identity(get(camera,"K")));
            CameraTrace1965.event("native_mode_owner",e,"camera_owned="+(mode!=null&&get(mode,"g")==camera)+" settings_owned="+(mode!=null&&get(mode,"h")==get(c,"c"))+" device_owned="+(mode!=null&&get(mode,"j")==get(camera,"K")));
            CameraTrace1965.event("native_camera",e,"state="+scalar(get(camera,"I"))+" capturing="+scalar(get(camera,"c"))+" sync_open="+scalar(get(get(c,"c"),"s"))+" deferred_outputs="+scalar(get(get(c,"c"),"u0")));
            CameraTrace1965.event("native_provider",e,"provider="+identity(provider)+" camera_owned="+(provider!=null&&get(provider,"d")==camera)+" enabled="+scalar(get(provider,"e"))+" ended="+scalar(get(provider,"h"))+" listener="+identity(get(provider,"a"))+" texture="+identity(call(provider,"f")));
        }catch(Throwable optional){}
    }
    /** Bounded ticket observations are exported with the camera ZIP. Details are
     * fixed scalar tokens only; never exception messages, pixels or app content. */
    public static void frontRetry(Object owner,long requestEpoch,long generation,String action,String reason,int replays,int resumes,long remaining) {
        try { CameraTrace1965.event("front_preview_"+action,requestEpoch,"owner="+identity(owner)+" gen="+generation+" reason="+reason+" replays="+replays+" resumes="+resumes+" left_ms="+Math.max(0L,remaining)); }
        catch(Throwable optional){}
    }
    public static void frontGuard(Object owner,String reason,int input,boolean nativeReady,boolean initialized) {
        try { CameraTrace1965.event("front_preview_guard",epoch(),"owner="+identity(owner)+" reason="+reason+" inputs="+input+" native_ready="+nativeReady+" initialized="+initialized); }
        catch(Throwable optional){}
    }
    public static void init(Context context){try{CameraTrace1965.init(context);}catch(Throwable optional){}}
    public static void track(Object owner,Context context){init(context);phase("capture_track_request",owner);}
    public static void foreground(Object owner){phase("camera_foreground_request",owner);}
    public static void background(Object owner){phase("camera_background_request",owner);}
    public static void opened(Object owner,Object device){try{phase("camera_open_callback",owner);CameraTrace1965.event("camera_open_owner",epoch(),"owner="+identity(owner)+" device="+identity(device));}catch(Throwable optional){}}
    public static void closing(Object owner){phase("camera_close_request",owner);}
    public static void prepared(Object owner,int result){try{phase("camera_prepare_callback",owner);CameraTrace1965.event("camera_prepare_result",epoch(),"owner="+identity(owner)+" result="+result);}catch(Throwable optional){}}
    public static void select(int value){try{CameraTrace1965.event("lens_select_request",epoch(),"selection="+value);phase("lens_selection_state",capture());}catch(Throwable optional){}}
    /** Record known SDK text as fixed tokens. Unknown payloads remain opaque. */
    private static String errorReason(String message) {
        if(message==null)return "none";
        if(message.contains("startCapture : mode is null"))return "start_mode_missing";
        if(message.contains("_stopCapture : mode is null"))return "stop_mode_missing";
        if(message.contains("Device is not ready")||message.contains("camera is null"))return "device_not_ready";
        if(message.contains("mCaptureRequestBuilder is null")||message.contains("CaptureRequest.Builder is null"))return "request_missing";
        if(message.contains("_startCapture error occurred"))return "start_exception";
        if(message.contains("setRepeatingRequest"))return "repeating_request_failed";
        if(message.contains("onConfigureFailed")||message.contains("configureFailed"))return "session_configuration_failed";
        if(message.contains("createCaptureSession"))return "session_creation_failed";
        if(message.contains("StreamConfigurationMap"))return "stream_configuration_missing";
        if(message.contains("getSupportedPreviewSizes"))return "preview_sizes_unavailable";
        if(message.contains("switchFlashMode"))return "flash_unavailable";
        if(message.contains("queryZoomAbility"))return "zoom_ability_unavailable";
        if(message.contains("Env is null"))return "native_environment_missing";
        if(message.contains("Invalidate state")||message.contains("Invalid state")||message.contains("invalid state"))return "native_state_invalid";
        return "unclassified";
    }
    public static void cameraError(Object owner,int code,String message){try{phase("camera_error_callback",owner);String details="owner="+identity(owner)+" code="+code+" sdk_reason="+errorReason(message);CameraTrace1965.event("camera_error_code",epoch(),details);CameraTrace1965.anomaly("camera_error",epoch(),details);}catch(Throwable optional){}}
    public static void previewResult(Object owner,int result){try{CameraTrace1965.event("input_preview_start_result",epoch(),"owner="+identity(owner)+" result="+result);if(result!=0)CameraTrace1965.anomaly("input_preview_start_rejected",epoch(),"owner="+identity(owner)+" result="+result);}catch(Throwable optional){}}
    public static void inputTimeout(Object owner,long requestEpoch,boolean awaitingFrame){try{CameraTrace1965.anomaly("input_preview_timeout",requestEpoch,"owner="+identity(owner)+" awaiting_frame="+awaitingFrame+" current_owned=true visible_confirmed=false");}catch(Throwable optional){}}
    /** Native provider notification only. Never reads, stores or converts frame data.
     * Limit work before reflection to the first notice and one sample per five seconds. */
    public static void input(Object provider,Object frame){
        if(provider==null||frame==null)return;
        try {
            long e=epoch(),now=SystemClock.uptimeMillis();Object c=capture();boolean first;
            synchronized(LOCK){
                Sample old=samples.get(provider);first=old==null||old.epoch!=e||old.capture.get()!=c;
                if(!first&&now-old.time<FRAME_SAMPLE_MS)return;
                if(samples.size()>=16&&!samples.containsKey(provider))samples.clear();
                samples.put(provider,new Sample(e,now,c));
            }
            Object host=server(),manager=get(host,"mProviderManager"),current=call(manager,"h");
            CameraTrace1965.event(first?"provider_first_frame_notice":"provider_frame_sample",e,"provider="+identity(provider)+" current="+(current==provider)+" capture="+identity(c)+" camera="+identity(get(provider,"d"))+" enabled="+scalar(get(provider,"e"))+" ended="+scalar(get(provider,"h")));
        }catch(Throwable optional){}
    }
}
