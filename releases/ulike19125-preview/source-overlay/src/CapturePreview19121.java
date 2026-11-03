package com.hiro.ulike;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.Map;

/** Only lifts the audited HD texture-render hold after the save owns its input.
 * This never resumes a camera session, opens the next shutter, invokes the SDK
 * capture-finish callback, or changes a bitmap. Unknown lifecycle/ABI is a no-op.
 */
public final class CapturePreview19121 {
    private static final String ZOOM="com.hiro.ulike.OpticalZoom",YUV="com.hiro.ulike.CaptureYuv";
    private static final Class<?>[] NONE=new Class<?>[0];
    private CapturePreview19121(){}
    static final class Token {
        final Object manager;final long id;
        Object callback,bridge,controller,fragment,capture,route,yuvOwner,yuvState,reader,original;
        long epoch;String unavailable;
        Token(Object manager,long id){this.manager=manager;this.id=id;}
    }
    /** Reads only identity/state under their owners' locks. No Activity/Fragment
     * methods are called from the possibly non-main capture handoff thread. */
    public static Object snapshot19121(Object manager,long id) {
        Token token=new Token(manager,id);
        try {
            token.callback=field(manager,"b");
            if(token.callback==null||!"i.o.a.b1.a.b.c$a".equals(token.callback.getClass().getName())){token.unavailable="撮影通知のABI未確認";return token;}
            token.bridge=field(token.callback,"a");token.controller=field(token.bridge,"c");token.fragment=field(token.controller,"a");
            Object lock=stat(ZOOM,"LOCK");
            synchronized(lock) {
                token.capture=referent(stat(ZOOM,"capture"));token.route=stat(ZOOM,"active");
                token.epoch=((Number)stat(ZOOM,"epoch")).longValue();
                token.unavailable=opticalReason(token);
            }
            if(token.unavailable!=null)return token;
            token.yuvOwner=referent(stat(YUV,"active"));
            Object states=stat(YUV,"states");
            if(token.yuvOwner==null||!(states instanceof Map)){token.unavailable="撮影入力の所有者未確認";return token;}
            synchronized(states) {
                token.yuvState=((Map<?,?>)states).get(token.yuvOwner);
                if(token.yuvState==null){token.unavailable="撮影入力の状態未確認";return token;}
                token.reader=field(token.yuvState,"yuv");token.original=field(token.yuvState,"original");
                token.unavailable=yuvReason(token,states);
            }
            if(token.unavailable==null)token.unavailable=currentReason(token);
        }catch(ReflectiveOperationException failure){token.unavailable="復帰対象のABI未確認";}
        catch(RuntimeException failure){token.unavailable="復帰対象の状態未確認";}
        catch(LinkageError failure){token.unavailable="復帰対象のリンク未確認";}
        catch(OutOfMemoryError failure){token.unavailable="復帰準備のメモリ不足";}
        return token;
    }
    /** Caller has completed get/copy/watermark and registered the save job. The
     * borrowed source remains guarded by captureOpen=false until normal finish. */
    public static void ready19121(Object snapshot,Bitmap input,ProcessingTiming1947.Trace trace,long inputReady,boolean serialOnly) {
        try {
            if(!serialOnly){report(trace,"所有画像あり・通常の復帰条件に従う",inputReady);return;}
            if(!(snapshot instanceof Token)){report(trace,"見送り・復帰対象の記録なし",inputReady);return;}
            Token token=(Token)snapshot;
            if(token.unavailable!=null){report(trace,"見送り・"+token.unavailable,inputReady);return;}
            if(!new Handler(Looper.getMainLooper()).post(new Pending(token,input,trace,inputReady)))
                report(trace,"見送り・UIへの要求受付失敗",inputReady);
        }catch(RuntimeException failure){report(trace,"見送り・復帰要求の準備失敗",inputReady);}
        catch(LinkageError failure){report(trace,"見送り・復帰要求のリンク未確認",inputReady);}
        catch(OutOfMemoryError failure){report(trace,"見送り・復帰要求のメモリ不足",inputReady);}
    }
    static final class Pending implements Runnable {
        final Token token;final Bitmap input;final ProcessingTiming1947.Trace trace;final long inputReady;
        Pending(Token token,Bitmap input,ProcessingTiming1947.Trace trace,long inputReady){this.token=token;this.input=input;this.trace=trace;this.inputReady=inputReady;}
        public void run() {
            String result;
            try{result=restore(token,input);}
            catch(ReflectiveOperationException failure){result="見送り・復帰先のABIまたは呼出し失敗";}
            catch(RuntimeException failure){result="見送り・復帰先の状態または呼出し失敗";}
            catch(LinkageError failure){result="見送り・復帰先のリンク未確認";}
            catch(OutOfMemoryError failure){result="見送り・復帰時のメモリ不足";}
            long returned=System.nanoTime();
            reportAt19125(trace,result,inputReady,returned);
            if("HD描画停止の解除要求を送信（次の撮影は保護を継続）".equals(result))
                observe19125(trace,inputReady,returned);
        }
    }
    static String restore(Token token,Bitmap input)throws ReflectiveOperationException {
        if(Looper.myLooper()!=Looper.getMainLooper())return "見送り・UIスレッド外";
        if(!AsyncSave1935.previewEligible19121(token.id))return "見送り・保存世代または撮影保護状態が変化";
        String reason=currentReason(token);if(reason!=null)return "見送り・"+reason;
        if(token.callback==null||field(token.manager,"b")!=token.callback
                ||!"i.o.a.b1.a.b.c$a".equals(token.callback.getClass().getName()))return "見送り・撮影通知の所有者が変化";
        Object bridge=field(token.callback,"a"),controller=call(bridge,"e"),fragment=call(controller,"O0");
        if(bridge!=token.bridge||controller!=token.controller||fragment!=token.fragment)return "見送り・カメラ画面の所有者が変化";
        if(fragment==null||!"com.bytedance.corecamera.camera.basic.PureCameraFragment".equals(fragment.getClass().getName()))return "見送り・カメラ画面のABI未確認";
        Object activityObject=call(fragment,"getActivity");
        if(!(activityObject instanceof Activity))return "見送り・カメラ画面なし";
        Activity activity=(Activity)activityObject;
        if(activity.isFinishing()||activity.isDestroyed()||!activity.hasWindowFocus()
                ||!Boolean.TRUE.equals(call(fragment,"isAdded"))||!Boolean.TRUE.equals(call(fragment,"isResumed"))
                ||!Boolean.TRUE.equals(call(fragment,"isVisible")))return "見送り・カメラ画面が操作中ではない";
        Object state=call(call(fragment,"getCameraState"),"l");
        if(!Boolean.TRUE.equals(call(call(state,"a"),"a")))return "見送り・自動保存設定が変化";
        if(!Boolean.TRUE.equals(call(call(state,"q"),"a")))return "見送り・HD設定オフ（標準の描画解除に従う）";
        Object camera=call(fragment,"c1"),recorder=call(camera,"l"),supplier=call(camera,"h");
        if(call(recorder,"getCurrentCameraCapture")!=token.capture
                ||!"i.f.l.n.h".equals(supplier.getClass().getName())||field(supplier,"b")!=recorder
                ||field(field(supplier,"h"),"a")!=recorder)return "見送り・描画先のカメラが変化";
        Object store=ReflectionCache1945.method(ReflectionCache1945.type("i.f.l.n.s.a"),"b",NONE).invoke(null);
        if(input==null||input.isRecycled()||field(store,"b")!=input)return "見送り・保存元画像の対応が変化";
        // Recheck mutable owners immediately before this one display operation.
        reason=currentReason(token);if(reason!=null)return "見送り・"+reason;
        if(!AsyncSave1935.previewEligible19121(token.id))return "見送り・保存世代または撮影保護状態が変化";
        ReflectionCache1945.method(supplier.getClass(),"M",new Class<?>[]{boolean.class}).invoke(supplier,false);
        return "HD描画停止の解除要求を送信（次の撮影は保護を継続）";
    }
    private static String currentReason(Token token)throws ReflectiveOperationException {
        Object lock=stat(ZOOM,"LOCK");String reason;
        synchronized(lock){reason=opticalReason(token);}if(reason!=null)return reason;
        Object states=stat(YUV,"states");if(!(states instanceof Map))return "撮影入力の状態が変化";
        synchronized(states){reason=yuvReason(token,states);}if(reason!=null)return reason;
        synchronized(lock){return opticalReason(token);}
    }
    private static String opticalReason(Token token)throws ReflectiveOperationException {
        if(!Boolean.TRUE.equals(stat(ZOOM,"foreground")))return "アプリが背景状態";
        if(Boolean.TRUE.equals(stat(ZOOM,"recording"))||Boolean.TRUE.equals(stat(ZOOM,"switching")))return "録画またはレンズ切替中";
        if(token.capture==null||referent(stat(ZOOM,"capture"))!=token.capture||stat(ZOOM,"active")!=token.route
                ||((Number)stat(ZOOM,"epoch")).longValue()!=token.epoch)return "カメラまたはレンズ世代が変化";
        return null;
    }
    private static String yuvReason(Token token,Object states)throws ReflectiveOperationException {
        if(referent(stat(YUV,"active"))!=token.yuvOwner||((Map<?,?>)states).get(token.yuvOwner)!=token.yuvState)
            return "撮影入力の所有者が変化";
        if(token.yuvState==null||token.reader==null||!Boolean.TRUE.equals(field(token.yuvState,"ready"))
                ||Boolean.TRUE.equals(field(token.yuvState,"closed"))||field(token.yuvState,"yuv")!=token.reader
                ||field(token.yuvState,"original")!=token.original||field(token.yuvOwner,"e0")!=token.original)
            return "撮影入力が終了または再設定中";
        return null;
    }
    private static void observe19125(ProcessingTiming1947.Trace trace,long inputReady,long returned){
        try{ReflectionCache1945.method(ReflectionCache1945.type("com.hiro.ulike.Responsiveness19122"),"previewRequested19125",
            new Class<?>[]{ProcessingTiming1947.Trace.class,long.class,long.class}).invoke(null,trace,inputReady,returned);}
        catch(ReflectiveOperationException ignored){}catch(RuntimeException ignored){}catch(LinkageError ignored){}catch(OutOfMemoryError ignored){}
    }
    private static void report(ProcessingTiming1947.Trace trace,String result,long inputReady){
        reportAt19125(trace,result,inputReady,System.nanoTime());
    }
    private static void reportAt19125(ProcessingTiming1947.Trace trace,String result,long inputReady,long recordedAt){
        try{ProcessingTiming1947.capturePreview19121(trace,result,inputReady>0?Math.max(0,recordedAt-inputReady):-1);}
        catch(RuntimeException ignored){}catch(LinkageError ignored){}catch(OutOfMemoryError ignored){}
    }
    private static Object referent(Object value){return value instanceof WeakReference?((WeakReference<?>)value).get():null;}
    private static Object stat(String owner,String name)throws ReflectiveOperationException{return ReflectionCache1945.declaredField(ReflectionCache1945.type(owner),name).get(null);}
    private static Object field(Object owner,String name)throws ReflectiveOperationException{return ReflectionCache1945.field(owner.getClass(),name).get(owner);}
    private static Object call(Object owner,String name)throws ReflectiveOperationException{return ReflectionCache1945.method(owner.getClass(),name,NONE).invoke(owner);}
}
