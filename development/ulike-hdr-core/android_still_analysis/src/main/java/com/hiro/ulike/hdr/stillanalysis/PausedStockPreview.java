package com.hiro.ulike.hdr.stillanalysis;

import android.content.Context;
import android.os.Looper;
import android.view.Surface;
import com.hiro.ulike.hdr.faceprobe.StockStillFaceProbe;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual normal-SDK pause/release/analyse/restart sequence. The global admission
 * patch must be present; absence is an error, never a no-op lease implementation.
 * This is callable integration code, not evidence of execution on a phone.
 */
public final class PausedStockPreview {
    private PausedStockPreview() {}
    /** AutoCloseable results are owned here until preview/composer restoration succeeds.
     * On failure they are closed rather than silently dropping full-image storage. */
    public interface Work<T> { T run(StockStillFaceProbe.IdleSdkLease lease)throws Exception; }
    public interface RestoreVerification {
        /** Confirm actual original native-init success, app's normal composer
         * restoration and unchanged camera/style request. A start return is not
         * enough. Must finish within the supplied callback deadline. */
        void requireRestored(Object originalRecorder,long timeoutMillis)throws Exception;
    }
    public static <T>T run(Context context,Object originalRecorder,Object expectedBackend,long timeoutMillis,RestoreVerification restored,Work<T> work)throws Exception {
        if(context==null || originalRecorder==null || expectedBackend==null || restored==null || work==null)throw new NullPointerException();
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Pause/analyse/restart off UI thread");
        if(timeoutMillis<1 || timeoutMillis>30000)throw new IllegalArgumentException("Deadline");
        if(!originalRecorder.getClass().getName().equals("com.ss.android.vesdk.VERecorder") ||
                !expectedBackend.getClass().getName().equals("com.ss.android.vesdk.TECameraVideoRecorder") ||
                field(originalRecorder,"b")!=expectedBackend)throw new IllegalArgumentException("Exact legacy camera backend identity required");
        Object nativeInvoker=field(field(expectedBackend,"mRecordPresenter"),"mfbInvoker");
        if(!nativeInvoker.getClass().getName().equals("com.ss.android.medialib.RecordInvoker"))throw new IllegalArgumentException("Unexpected native invoker");
        Surface surface=(Surface)field(expectedBackend,"g1");
        if(surface!=null && !surface.isValid())throw new IllegalStateException("Original preview surface is not valid");
        if(status(expectedBackend)!=2 || handle(nativeInvoker)==0)throw new IllegalStateException("Require original live preview, not recording");
        RecorderAdmission.Session session=RecorderAdmission.acquire(originalRecorder,nativeInvoker);
        boolean released=false,analysisEntered=false,restorationComplete=false,interrupted=false;
        Throwable primary=null;T output=null;
        try {
            Completion stopped=new Completion(context.getClassLoader());
            invoke(originalRecorder,"stopPreviewAsync",new Class<?>[]{stopped.type},stopped.listener);
            stopped.await(timeoutMillis);
            int stoppedState=status(expectedBackend);
            if(stoppedState!=1 && stoppedState!=0)throw new IllegalStateException("Stop callback without stopped recorder state");
            if(stoppedState!=0)invoke(expectedBackend,"releaseInteralRecorder",new Class<?>[0]);
            if(status(expectedBackend)!=0 || handle(nativeInvoker)!=0)throw new IllegalStateException("Original recorder release incomplete");
            released=true;
            RecorderAdmission.analysis(session);analysisEntered=true;
            StockStillFaceProbe.IdleSdkLease lease=new StockStillFaceProbe.IdleSdkLease() {
                @Override public Context context(){return context;}
                @Override public void requireInitializedAndNoOtherRecorder() {
                    RecorderAdmission.requireIdle(session);
                    try { if(status(expectedBackend)!=0 || handle(nativeInvoker)!=0)throw new IllegalStateException("Original recorder restarted during analysis"); }
                    catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot verify original recorder state",e);}
                }
            };
            output=work.run(lease);
        } catch(Exception|Error e) { primary=e;interrupted=e instanceof InterruptedException; }
        finally {
            // Never overlap a still worker that did not join with a restarted
            // original recorder. The admission ledger refuses restoration then.
            if(released && analysisEntered) {
                try {
                    RecorderAdmission.restore(session);
                    if(field(expectedBackend,"g1")!=surface || surface!=null && !surface.isValid())throw new IllegalStateException("Original surface changed during analysis");
                    Completion started=new Completion(context.getClassLoader());
                    invoke(originalRecorder,"startPreviewAsync",new Class<?>[]{Surface.class,started.type},surface,started.listener);
                    started.await(timeoutMillis);
                    if(status(expectedBackend)!=2 || handle(nativeInvoker)==0)throw new IllegalStateException("Preview restart state/handle mismatch");
                    restored.requireRestored(originalRecorder,timeoutMillis);
                    RecorderAdmission.complete(session);restorationComplete=true;
                }catch(Exception|Error e){interrupted|=e instanceof InterruptedException;if(primary==null)primary=e;else if(primary!=e)primary.addSuppressed(e);}
            }
            if(!restorationComplete) {
                RecorderAdmission.quarantine(session);
                if(output instanceof AutoCloseable)try{((AutoCloseable)output).close();}
                catch(Exception|Error e){interrupted|=e instanceof InterruptedException;if(primary==null)primary=e;else if(primary!=e)primary.addSuppressed(e);}
            }
        }
        if(interrupted)Thread.currentThread().interrupt();
        if(primary instanceof Exception)throw (Exception)primary;
        if(primary instanceof Error)throw (Error)primary;
        return output;
    }
    private static Object field(Object value,String name)throws ReflectiveOperationException {return value.getClass().getField(name).get(value);}
    private static Object invoke(Object value,String name,Class<?>[] types,Object...args)throws ReflectiveOperationException {return value.getClass().getMethod(name,types).invoke(value,args);}
    private static int status(Object backend)throws ReflectiveOperationException{return (Integer)invoke(backend,"getRecordStatus",new Class<?>[0]);}
    private static long handle(Object invoker)throws ReflectiveOperationException{return (Long)invoke(invoker,"getHandler",new Class<?>[0]);}
    private static final class Completion {
        final Class<?> type;final Object listener;final CountDownLatch done=new CountDownLatch(1);final AtomicInteger code=new AtomicInteger(Integer.MIN_VALUE);
        Completion(ClassLoader loader)throws ClassNotFoundException {
            type=Class.forName("com.ss.android.vesdk.VEListener$VECallListener",false,loader);
            listener=Proxy.newProxyInstance(loader,new Class<?>[]{type},(proxy,method,args)->{
                switch(method.getName()) {
                    case "hashCode":return System.identityHashCode(proxy);
                    case "equals":return proxy==args[0];
                    case "toString":return "ULikePreviewTransition";
                    case "onDone":if(!code.compareAndSet(Integer.MIN_VALUE,(Integer)args[0]))code.set(Integer.MAX_VALUE);done.countDown();return null;
                    default:throw new IllegalStateException("Unknown transition callback");
                }
            });
        }
        void await(long timeout)throws Exception {
            if(!done.await(timeout,TimeUnit.MILLISECONDS))throw new IllegalStateException("Preview transition callback timeout");
            if(code.get()!=0)throw new IllegalStateException("Preview transition failed or repeated: "+code.get());
        }
    }
}
