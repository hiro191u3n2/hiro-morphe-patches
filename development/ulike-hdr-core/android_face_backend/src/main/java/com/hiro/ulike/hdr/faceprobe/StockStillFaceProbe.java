package com.hiro.ulike.hdr.faceprobe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.PointF;
import android.os.Build;
import android.os.Looper;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Callable stock-SDK diagnostic, deliberately NOT StillFaceAnalysis.Detector.
 * It submits one owned SDR proxy to a new recorder, records raw 106-point
 * callbacks, and tears that recorder down. No existing preview is sampled.
 * Coordinates and normal SDK initialization still require device validation.
 */
public final class StockStillFaceProbe {
    private static final Object SERIAL = new Object();
    private static Object[] quarantined;
    private StockStillFaceProbe() {}

    /**
     * The application must hold its REAL recorder lifecycle lock, fully release
     * all pre-existing SDK recorders, and prevent any OTHER recorder until run
     * returns. The sole recorder created internally by this probe is excluded
     * from requireInitializedAndNoOtherRecorder; no other instance is excluded.
     * Merely creating
     * an implementation does not establish exclusivity. Normal SDK licensing /
     * model-resource initialization must already have succeeded in this app.
     */
    public interface IdleSdkLease {
        Context context();
        void requireInitializedAndNoOtherRecorder() throws Exception;
    }

    /**
     * Optional synchronous observation hook for one owned still. The recorder is
     * already initialized under the exclusive lease. beforeSubmit may configure
     * normal effects / a message listener; it MUST NOT start/stop another recorder,
     * change licensing, submit frames, or retain the recorder beyond this run.
     * face runs on an SDK worker: copy values before returning, never retain SDK
     * objects or join/stop that worker. awaitAdditional runs on the caller worker
     * and may await bounded MessageCenter delivery; no render/preview cache is used.
     */
    public interface RawObserver {
        default void beforeSubmit(Object ownedInitializedRecorder) throws Exception {}
        default void submitted() throws Exception {}
        default boolean observesImage() { return false; }
        /** Receives an owned clone, never the SDK callback buffer. */
        default void image(int[] ownedPixels, int width, int height) throws Exception {}
        default void face(Object attributeInfo, Object faceDetectInfo) throws Exception {}
        default void awaitAdditional(long timeoutMillis) throws Exception {}
    }

    public static ProbeLedger.Snapshot run(IdleSdkLease lease, Bitmap encodedSdrProxy,
                                           File newWorkspace, long callbackTimeoutMillis) throws Exception {
        return run(lease, encodedSdrProxy, newWorkspace, callbackTimeoutMillis, new RawObserver() {});
    }

    public static ProbeLedger.Snapshot run(IdleSdkLease lease, Bitmap encodedSdrProxy,
                                           File newWorkspace, long callbackTimeoutMillis, RawObserver observer) throws Exception {
        if(encodedSdrProxy==null)throw new NullPointerException("proxy");
        AnalysisCapacity capacity=AnalysisCapacity.legacyDiagnostic();
        capacity.requireGrid(encodedSdrProxy.getWidth(),encodedSdrProxy.getHeight());
        if(encodedSdrProxy.isRecycled() || encodedSdrProxy.getConfig()!=Bitmap.Config.ARGB_8888 ||
                !ColorSpace.get(ColorSpace.Named.SRGB).equals(encodedSdrProxy.getColorSpace()) ||
                (Build.VERSION.SDK_INT>=34 && encodedSdrProxy.hasGainmap()))throw new IllegalArgumentException("Explicit sRGB proxy required");
        Bitmap copy=encodedSdrProxy.copy(Bitmap.Config.ARGB_8888,false);
        if(copy==null)throw new IllegalStateException("Cannot own input Bitmap");
        OwnedBitmapSubmission moved=null;
        try { moved=OwnedBitmapSubmission.adopt(copy,capacity);return runOwned(lease,moved,newWorkspace,callbackTimeoutMillis,observer); }
        finally { if(moved==null)copy.recycle();else moved.close(); }
    }

    /** Consumes a private Bitmap through an exclusive claim; never copies its pixels for orientation
     * unless the explicit capacity policy requests that separate photographic diagnostic. */
    public static ProbeLedger.Snapshot runOwned(IdleSdkLease lease,OwnedBitmapSubmission submission,
            File newWorkspace,long callbackTimeoutMillis,RawObserver observer)throws Exception {
        if(observer==null || lease==null || submission==null || newWorkspace==null)throw new NullPointerException();
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Run off the UI thread");
        if(callbackTimeoutMillis<1 || callbackTimeoutMillis>30000)throw new IllegalArgumentException("Deadline");
        final AnalysisCapacity capacity=submission.capacity;
        final int width=submission.width,height=submission.height;
        capacity.requireTransferredPayload(width,height);
        submission.borrowBeforeTransfer(); // Reject reuse before acquiring/initializing anything native.
        final ProbeLedger ledger=new ProbeLedger(width,height,capacity);
        final AtomicBoolean submitted=new AtomicBoolean(),imageSeen=new AtomicBoolean(),callbacksActive=new AtomicBoolean(true);
        synchronized (SERIAL) {
            if (quarantined != null) throw new IllegalStateException("Previous native teardown failed; do not reuse process");
            lease.requireInitializedAndNoOtherRecorder();
            Context context = lease.context();
            if (context == null) throw new IllegalArgumentException("Missing app context");
            ledger.verifiedSdk(verifyInstalledLibrary(context));
            File cache = context.getCacheDir().getCanonicalFile();
            if (!newWorkspace.getCanonicalPath().startsWith(cache.getPath() + File.separator))
                throw new IllegalArgumentException("Workspace must be inside this app's cache directory");
            if (!newWorkspace.mkdir()) throw new IllegalArgumentException("Require a fresh app-private probe directory");
            ClassLoader loader = context.getClassLoader();
            Class<?> runtimeType = Class.forName("com.ss.android.vesdk.runtime.VERuntime", false, loader);
            Object runtime = runtimeType.getMethod("getInstance").invoke(null);
            if (call(runtime, "getContext", new Class<?>[0]) == null)
                throw new IllegalStateException("Normal SDK runtime is not initialized");
            Object env = call(runtime, "getEnv", new Class<?>[0]);
            String models = (String)call(env, "getDetectModelsDir", new Class<?>[0]);
            if (models == null || models.isEmpty()) throw new IllegalStateException("Normal SDK model directory unavailable");

            // Transfer occurs only after read-only preflight. Any failure thereafter is protected by joins/finally.
            final int[][] originalPixels=new int[1][];
            final Object[] quarantineSlots=new Object[8];
            List<Object> callbackRefs = new ArrayList<>();
            final SubmissionOwnership.Claim<Bitmap> claim=submission.transferToProbe();
            final Bitmap owned=claim.resource();
            Object recorder = null;
            boolean initializedAttempt = false, startedAttempt = false, registeredAttempt = false, teardown = true;
            try {
                if(Thread.currentThread().isInterrupted())throw new InterruptedException("Analysis cancelled before initialization");
                if(capacity.comparePhotographicOrientation) {
                    originalPixels[0]=new int[width*height];
                    owned.getPixels(originalPixels[0],0,width,0,0,width,height);
                }
                Class<?> recorderType = Class.forName("com.ss.android.medialib.RecordInvoker", true, loader);
                recorder = recorderType.getConstructor().newInstance();
                Class<?> initType = Class.forName("com.ss.android.medialib.listener.NativeInitListener", false, loader);
                Object init = callback(loader, initType, (name, values) -> {
                    if (name.equals("onNativeInitCallBack")) ledger.initialized((Integer)values[0]);
                    else if (name.equals("onNativeInitHardEncoderRetCallback")) ledger.fail("Unexpected video encoder initialization");
                });
                callbackRefs.add(init);
                call(recorder, "setNativeInitListener2", new Class<?>[]{initType}, init);
                initializedAttempt = true;
                requireZero(call(recorder, "initBeautyPlay", new Class<?>[]{int.class,int.class,String.class,int.class,int.class,String.class,int.class},
                        width, height, newWorkspace.getCanonicalPath(), width, height, models, 0), "initBeautyPlay");
                call(recorder, "setEffectBuildChainType", new Class<?>[]{int.class}, 1);
                call(recorder, "setDetectionMode", new Class<?>[]{boolean.class}, false);
                call(recorder, "forceFirstFrameHasEffect", new Class<?>[]{boolean.class}, true);
                call(recorder, "setCaptureRenderWidth", new Class<?>[]{int.class,int.class}, width, width);
                call(recorder, "initFaceDetectExtParam", new Class<?>[]{int.class,boolean.class,boolean.class}, 0, true, false);
                startedAttempt = true;
                // These final two zeroes explicitly set native rotation and mirror.
                // Do not use the high-level null-view wrapper's four -1 values.
                requireZero(call(recorder, "startPlay", new Class<?>[]{int.class,int.class,String.class,int.class,int.class},
                        width, height, Build.DEVICE, 0, 0), "startPlay");
                if (!ledger.await(true, callbackTimeoutMillis)) throw new IllegalStateException("SDK initialization was not observed successfully");
                lease.requireInitializedAndNoOtherRecorder();
                Class<?> faceType = Class.forName("com.ss.android.medialib.RecordInvoker$FaceResultCallback", false, loader);
                Object face = callback(loader, faceType, (name, values) -> {
                    if (!name.equals("onResult") || !callbacksActive.get()) return;
                    try {
                        copyFaces(values[1], ledger);
                        if (submitted.get()) observer.face(values[0], values[1]);
                    }
                    catch (Exception | Error e) { ledger.fail("Face callback rejected: " + e.getClass().getSimpleName()); }
                });
                callbackRefs.add(face); registeredAttempt = true;
                call(recorder, "registerFaceResultCallback", new Class<?>[]{boolean.class,faceType}, true, face);
                Class<?> pictureType = Class.forName("com.ss.android.medialib.RecordInvoker$OnPictureCallbackV2", false, loader);
                Object picture = callback(loader, pictureType, (name, values) -> {
                    if (name.equals("onImage")) {
                        try {
                            if(!callbacksActive.get())return;
                            if(!submitted.get() || !imageSeen.compareAndSet(false,true))throw new IllegalArgumentException("Early/repeated image callback");
                            int[] pixels=(int[])values[0];int rw=(Integer)values[1],rh=(Integer)values[2];
                            capacity.requireDiagnostic(rw,rh);
                            if(rw!=width || rh!=height || pixels==null || pixels.length!=(long)width*height)
                                throw new IllegalArgumentException("Unexpected callback grid");
                            if(originalPixels[0]!=null)ledger.orientationEvidence(PixelOrientationEvidence.measure(
                                originalPixels[0],width,height,pixels,rw,rh,capacity));
                            if(observer.observesImage())OwnedRenderPixels.copyAndDeliver(pixels,rw,rh,width,height,capacity,observer::image);
                            ledger.rendered(rw,rh,pixels.length); // Completion only after synchronous ownership conversion.
                        } catch(Exception|Error e) { ledger.fail("Diagnostic callback rejected: "+e.getClass().getSimpleName()); }
                    } else if (name.equals("onResult")) ledger.status((Integer)values[0], (Integer)values[1]);
                });
                callbackRefs.add(picture);
                Class<?> frameType = Class.forName("com.ss.android.medialib.camera.ImageFrame", false, loader);
                Object imageFrame = frameType.getConstructor(Bitmap.class, int.class).newInstance(owned, 2);
                observer.beforeSubmit(recorder);
                lease.requireInitializedAndNoOtherRecorder();
                if(Thread.currentThread().isInterrupted())throw new InterruptedException("Analysis cancelled before submission");
                ledger.submit();
                observer.submitted();
                claim.submitted();
                submitted.set(true);
                requireZero(call(recorder, "renderPicture", new Class<?>[]{frameType,int.class,int.class,pictureType},
                        imageFrame, width, height, picture), "renderPicture");
                if (ledger.await(false, callbackTimeoutMillis)) observer.awaitAdditional(callbackTimeoutMillis);
            } catch (Exception | LinkageError e) {
                ledger.fail("Probe failed: " + e.getClass().getSimpleName());
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            } finally {
                // Late callbacks after timeout/closure must not allocate another full diagnostic.
                // Already in-flight callbacks still require the real joins below; this flag is no join receipt.
                callbacksActive.set(false);
                // Never call stop/unregister from the face callback worker itself:
                // both can join that worker. Keep bitmap/callbacks alive until join.
                if (recorder != null && startedAttempt) teardown &= cleanup(recorder, "stopPlay", ledger, true);
                if (recorder != null && registeredAttempt) teardown &= cleanup(recorder, "unRegisterFaceResultCallback", ledger, false);
                if (recorder != null && initializedAttempt) teardown &= cleanup(recorder, "uninitBeautyPlay", ledger, true);
                if(teardown) {
                    try { claim.joined(true); }
                    catch(RuntimeException|Error failure) { teardown=false;ledger.fail("Bitmap release failed"); }
                    if(teardown && originalPixels[0]!=null)java.util.Arrays.fill(originalPixels[0],0);
                } else claim.joined(false);
                if(!teardown) {
                    quarantineSlots[0]=recorder;quarantineSlots[1]=submission;quarantineSlots[2]=claim;quarantineSlots[3]=owned;
                    quarantineSlots[4]=originalPixels;quarantineSlots[5]=callbackRefs;quarantineSlots[6]=lease;quarantineSlots[7]=observer;
                    quarantined=quarantineSlots;
                }
            }
            return ledger.finish(teardown);
        }
    }
    private interface Callback { void invoke(String name, Object[] args); }
    private static Object callback(ClassLoader loader, Class<?> type, Callback target) {
        return Proxy.newProxyInstance(loader, new Class<?>[]{type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "toString": return "ULikeStillProbe:" + type.getSimpleName();
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == args[0];
                default: target.invoke(method.getName(), args); return null;
            }
        });
    }
    private static void copyFaces(Object info, ProbeLedger ledger) throws Exception {
        if (info == null) throw new IllegalArgumentException("Null face info");
        Object array = call(info, "getInfo", new Class<?>[0]);
        if (array == null || Array.getLength(array) > 10) throw new IllegalArgumentException("Invalid face array");
        int count = Array.getLength(array); float[][] all = new float[count][]; float[] scores = new float[count];
        for (int i = 0; i < count; i++) {
            Object f = Array.get(array, i);
            PointF[] points = (PointF[])call(f, "getPoints", new Class<?>[0]);
            if (points == null || points.length != 106) throw new IllegalArgumentException("Expected SDK106");
            all[i] = new float[212];
            for (int j = 0; j < 106; j++) { all[i][j*2] = points[j].x; all[i][j*2+1] = points[j].y; }
            scores[i] = (Float)call(f, "getScore", new Class<?>[0]);
        }
        ledger.face(all, scores);
    }
    private static Object call(Object receiver, String name, Class<?>[] types, Object... args) throws Exception {
        if (receiver == null) throw new IllegalStateException("Missing SDK object");
        Method method = receiver.getClass().getMethod(name, types);
        return method.invoke(receiver, args);
    }
    private static void requireZero(Object value, String operation) {
        if (!(value instanceof Integer) || (Integer)value != 0)
            throw new IllegalStateException(operation + " returned " + value);
    }
    private static boolean cleanup(Object receiver, String name, ProbeLedger ledger, boolean result) {
        try {
            Object value = call(receiver, name, new Class<?>[0]);
            if (result) requireZero(value, name);
            return true;
        } catch (Exception | Error e) { ledger.fail("Teardown failed: " + name); return false; }
    }
    private static StockSdkIdentity verifyInstalledLibrary(Context context) throws Exception {
        File extracted = new File(context.getApplicationInfo().nativeLibraryDir, "libttvesdk.so");
        if (extracted.isFile()) {
            try (InputStream in = new FileInputStream(extracted)) { return StockSdkIdentity.verify(in); }
        }
        String[] splits = context.getApplicationInfo().splitSourceDirs;
        List<String> apks = new ArrayList<>(); apks.add(context.getApplicationInfo().sourceDir);
        if (splits != null) for (String path : splits) apks.add(path);
        for (String path : apks) try (ZipFile zip = new ZipFile(path)) {
            ZipEntry entry = zip.getEntry("lib/arm64-v8a/libttvesdk.so");
            if (entry != null) { try (InputStream in = zip.getInputStream(entry)) { return StockSdkIdentity.verify(in); } }
        }
        throw new IllegalStateException("Pinned installed arm64 SDK library was not found");
    }
}
