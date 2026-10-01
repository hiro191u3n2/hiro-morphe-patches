package com.hiro.ulike.hdr.stillanalysis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import com.hiro.ulike.hdr.faceprobe.ProbeLedger;
import com.hiro.ulike.hdr.faceprobe.AnalysisCapacity;
import com.hiro.ulike.hdr.faceprobe.OwnedBitmapSubmission;
import com.hiro.ulike.hdr.faceprobe.StockStillFaceProbe;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.lang.reflect.Proxy;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Callable same-owned-still CPU analysis and script export. Calibration is not silently inferred. */
public final class StockStillAnalysis {
    private StockStillAnalysis() {}
    private static final SecureRandom RANDOM=new SecureRandom();
    public static int newNonce() { return RANDOM.nextInt(Integer.MAX_VALUE-1)+1; }

    /** Replay actual selected composer resources/settings through ordinary SDK methods only. */
    public interface EffectSetup {
        void configure(Object ownedInitializedRecordInvoker,int requestNonce) throws Exception;
    }
    public static final class Request {
        public final int nonce;
        public final boolean captureRenderedDiagnostic;
        public final long sensorTimestampNs;
        public final String declaredSourceFrameSha256,declaredSettingsSha256;
        private final Set<String> features;
        public Request(int nonce,long sensorTimestampNs,String sourceSha256,String settingsSha256,Set<String> expectedFeatures) {
            this(nonce,sensorTimestampNs,sourceSha256,settingsSha256,expectedFeatures,false);
        }
        public Request(int nonce,long sensorTimestampNs,String sourceSha256,String settingsSha256,Set<String> expectedFeatures,boolean captureRenderedDiagnostic) {
            this.captureRenderedDiagnostic=captureRenderedDiagnostic;
            if(nonce<1 || sensorTimestampNs<0 || !hash(sourceSha256) || !hash(settingsSha256))throw new IllegalArgumentException("Request identity");
            new StillMessageCollector(nonce,expectedFeatures); // Validate once before native work.
            this.nonce=nonce;this.sensorTimestampNs=sensorTimestampNs;declaredSourceFrameSha256=sourceSha256;declaredSettingsSha256=settingsSha256;
            features=Collections.unmodifiableSet(new LinkedHashSet<>(expectedFeatures));
        }
        public Set<String> expectedFeatures() { return features; }
        private static boolean hash(String s) { return s!=null && s.matches("[0-9a-f]{64}"); }
    }
    public static Outcome run(StockStillFaceProbe.IdleSdkLease lease,Bitmap encodedSdrProxy,File newWorkspace,long timeoutMillis,Request request,EffectSetup setup) throws Exception {
        if(lease==null || encodedSdrProxy==null || request==null)throw new NullPointerException();
        if(setup==null && !request.features.isEmpty())throw new IllegalArgumentException("Script features require an actual effect setup");
        if(setup!=null)verifyEffectLibrary(lease.context());
        if(encodedSdrProxy.isRecycled() || encodedSdrProxy.getConfig()!=Bitmap.Config.ARGB_8888 ||
                (long)encodedSdrProxy.getWidth()*encodedSdrProxy.getHeight()>4194304)throw new IllegalArgumentException("Bounded explicit analysis proxy required");
        if(!ColorSpace.get(ColorSpace.Named.SRGB).equals(encodedSdrProxy.getColorSpace()) ||
                (Build.VERSION.SDK_INT>=34 && encodedSdrProxy.hasGainmap()))throw new IllegalArgumentException("Explicit sRGB proxy without gain map required");
        Bitmap owned=encodedSdrProxy.copy(Bitmap.Config.ARGB_8888,false);
        if(owned==null)throw new IllegalStateException("Cannot own proxy");
        OwnedBitmapSubmission moved=null;
        try { moved=OwnedBitmapSubmission.adopt(owned,AnalysisCapacity.legacyDiagnostic());
            return runOwned(lease,moved,newWorkspace,timeoutMillis,request,setup);
        } finally { if(moved==null)owned.recycle();else moved.close(); }
    }
    /** Same private Bitmap is digested and moved to the probe. Only that probe joins/recycles it. */
    public static Outcome runOwned(StockStillFaceProbe.IdleSdkLease lease,OwnedBitmapSubmission input,
            File newWorkspace,long timeoutMillis,Request request,EffectSetup setup)throws Exception {
        if(lease==null || input==null || request==null)throw new NullPointerException();
        if(setup==null && !request.features.isEmpty())throw new IllegalArgumentException("Script features require an actual effect setup");
        if(setup!=null)verifyEffectLibrary(lease.context());
        input.capacity.requireTransferredPayload(input.width,input.height);
        StillMessageCollector collector=new StillMessageCollector(request.nonce,request.features);
        Observer observer=new Observer(lease,request,setup,collector,input.capacity);
        boolean delivered=false;
        try {
            String proxySha=digest(input.borrowBeforeTransfer());
            ProbeLedger.Snapshot nativeEvidence=StockStillFaceProbe.runOwned(lease,input,newWorkspace,timeoutMillis,observer);
            StillMessageCollector.Snapshot messages=collector.finish();
            Outcome result=new Outcome(request,proxySha,nativeEvidence,observer.snapshot(),messages,observer.imageSnapshot());
            delivered=true;return result;
        } finally {
            collector.fail("Run ended");observer.close();
            if(!delivered && observer.imageSnapshot()!=null)observer.imageSnapshot().close();
            // No Bitmap recycle here: unsafe native joins retain the transferred Bitmap in quarantine.
            input.close();
        }
    }
    private static final class Observer implements StockStillFaceProbe.RawObserver {
        private final StockStillFaceProbe.IdleSdkLease lease;
        private final Request request;
        private final EffectSetup setup;
        private final StillMessageCollector collector;
        private final AnalysisCapacity capacity;
        private boolean closed,submitted;
        private SdkFaceSnapshot snapshot;
        private RenderedDiagnostic image;
        private Object messageListener;
        Observer(StockStillFaceProbe.IdleSdkLease lease,Request request,EffectSetup setup,StillMessageCollector collector,AnalysisCapacity capacity) {
            this.lease=lease;this.request=request;this.setup=setup;this.collector=collector;this.capacity=capacity;
        }
        @Override public void beforeSubmit(Object recorder) throws Exception {
            ClassLoader loader=lease.context().getClassLoader();
            Class<?> listener=Class.forName("com.bef.effectsdk.message.MessageCenter$Listener",false,loader);
            messageListener=Proxy.newProxyInstance(loader,new Class<?>[]{listener},(proxy,method,args)->{
                switch(method.getName()) {
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy==args[0];
                    case "toString": return "ULikeOwnedStillExport";
                    case "onMessageReceived":
                        try { collector.accept((Integer)args[0],(Integer)args[1],(Integer)args[2],(String)args[3]); }
                        catch(RuntimeException e) { collector.fail("Invalid SDK message callback"); }
                        return null;
                    default: throw new IllegalStateException("Unknown SDK message method");
                }
            });
            recorder.getClass().getMethod("setMessageListenerV2",listener).invoke(recorder,messageListener);
            if(setup!=null)setup.configure(recorder,request.nonce);
        }
        @Override public synchronized void submitted() {
            if(closed || submitted)throw new IllegalStateException("Observer already used");
            collector.submitted();submitted=true;
        }
        @Override public synchronized void face(Object attributes,Object detect) throws Exception {
            if(closed || !submitted || snapshot!=null)throw new IllegalStateException("Unexpected repeated face callback");
            snapshot=SdkFaceSnapshot.copy(detect);
        }
        @Override public boolean observesImage() { return request.captureRenderedDiagnostic; }
        @Override public synchronized void image(int[] ownedPixels,int width,int height) {
            if(closed || !submitted || image!=null)throw new IllegalStateException("Unexpected repeated diagnostic image");
            image=new RenderedDiagnostic(request.nonce,width,height,ownedPixels,capacity);
        }
        @Override public void awaitAdditional(long timeoutMillis) throws Exception { collector.awaitComplete(timeoutMillis); }
        synchronized RenderedDiagnostic imageSnapshot() { return image; }
        synchronized SdkFaceSnapshot snapshot() { return snapshot; }
        synchronized void close() { closed=true; }
    }
    private static void verifyEffectLibrary(Context context) throws Exception {
        if(context==null)throw new IllegalArgumentException("Missing app context");
        File extracted=new File(context.getApplicationInfo().nativeLibraryDir,"libeffect.so");
        if(extracted.isFile()) { try(InputStream in=new FileInputStream(extracted)){verifyEffectStream(in);}return; }
        List<String> paths=new ArrayList<>();paths.add(context.getApplicationInfo().sourceDir);
        String[] splits=context.getApplicationInfo().splitSourceDirs;
        if(splits!=null)Collections.addAll(paths,splits);
        for(String path:paths)try(ZipFile zip=new ZipFile(path)) {
            ZipEntry entry=zip.getEntry("lib/arm64-v8a/libeffect.so");
            if(entry!=null) { try(InputStream in=zip.getInputStream(entry)){verifyEffectStream(in);}return; }
        }
        throw new IllegalStateException("Pinned arm64 effect library missing");
    }
    private static void verifyEffectStream(InputStream stream) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[65536];long bytes=0;
        for(int n;(n=stream.read(buffer))!=-1;) { bytes+=n;if(bytes>67108864)throw new IllegalArgumentException("Oversized effect library");digest.update(buffer,0,n); }
        StringBuilder hex=new StringBuilder(64);for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        if(!hex.toString().equals("d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e"))throw new IllegalArgumentException("Unsupported effect library");
    }
    private static String digest(Bitmap bitmap) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        digest.update(new byte[]{'U','L','S','P',1});
        int w=bitmap.getWidth(),h=bitmap.getHeight();
        updateInt(digest,w);updateInt(digest,h);
        int[] row=new int[w];
        try {
            for(int y=0;y<h;y++) {
                if(Thread.currentThread().isInterrupted())throw new InterruptedException("Analysis digest cancelled");
                bitmap.getPixels(row,0,w,0,y,w,1);for(int pixel:row)updateInt(digest,pixel);
            }
            StringBuilder out=new StringBuilder(64);for(byte b:digest.digest())out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
        } finally { java.util.Arrays.fill(row,0); }
    }
    private static void updateInt(MessageDigest d,int value) { d.update((byte)(value>>>24));d.update((byte)(value>>>16));d.update((byte)(value>>>8));d.update((byte)value); }
    /** Raw packed SDK render pixels. Channel packing, transfer and mask-grid
     * alignment are observations to calibrate, not silently assumed contracts. */
    public interface DiagnosticConsumer<T> {
        /** Borrowed owned array is wiped after return/throw. Do not retain it or return an alias. */
        T consume(int[] borrowedPixels,int width,int height)throws Exception;
    }
    public static final class RenderedDiagnostic implements AutoCloseable {
        public final int nonce,width,height;
        private int[] pixels;
        private boolean consuming;
        private RenderedDiagnostic(int nonce,int width,int height,int[] ownedPixels,AnalysisCapacity capacity) {
            capacity.requireDiagnostic(width,height);
            if(ownedPixels==null || ownedPixels.length!=(long)width*height)throw new IllegalArgumentException("Diagnostic grid");
            this.nonce=nonce;this.width=width;this.height=height;pixels=ownedPixels;
        }
        /** Legacy small-image snapshot. Native-size candidates must use consuming conversion. */
        public synchronized int[] pixels() {
            if(pixels==null || consuming)throw new IllegalStateException("Diagnostic already consumed");
            if(pixels.length>AnalysisCapacity.LEGACY_MAX_PIXELS)throw new IllegalStateException("Native-size diagnostic requires consuming conversion");
            return pixels.clone();
        }
        public synchronized <T> T consume(DiagnosticConsumer<T> consumer)throws Exception {
            if(consumer==null)throw new NullPointerException("consumer");
            if(pixels==null || consuming)throw new IllegalStateException("Diagnostic already consumed");
            consuming=true;
            try {
                if(Thread.currentThread().isInterrupted())throw new InterruptedException("Diagnostic conversion cancelled");
                T value=consumer.consume(pixels,width,height);
                if(value==pixels)throw new IllegalArgumentException("Borrowed diagnostic alias cannot escape");
                return value;
            } finally { java.util.Arrays.fill(pixels,0);pixels=null;consuming=false; }
        }
        @Override public synchronized void close() {
            if(consuming)throw new IllegalStateException("Reentrant close during diagnostic conversion");
            if(pixels!=null){java.util.Arrays.fill(pixels,0);pixels=null;}
        }
    }
    public static final class Outcome {
        public final Request request;
        public final String submittedProxySha256;
        public final ProbeLedger.Snapshot nativeEvidence;
        public final SdkFaceSnapshot faces;
        public final StillMessageCollector.Snapshot scriptExports;
        public final RenderedDiagnostic renderedDiagnostic;
        public final boolean observationsComplete;
        /** Native coordinates, SDK mesh projection and source proxy binding require device calibration. */
        public final boolean productionGeometryVerified=false;
        public final boolean declaredHdrSourceBindingVerified=false;
        private Outcome(Request r,String sha,ProbeLedger.Snapshot evidence,SdkFaceSnapshot faces,StillMessageCollector.Snapshot messages,RenderedDiagnostic renderedDiagnostic) {
            request=r;submittedProxySha256=sha;nativeEvidence=evidence;this.faces=faces;scriptExports=messages;this.renderedDiagnostic=renderedDiagnostic;
            observationsComplete=evidence.callbacksObserved && faces!=null && messages.allExpectedExportsObserved && (!r.captureRenderedDiagnostic || renderedDiagnostic!=null);
        }
    }
}
