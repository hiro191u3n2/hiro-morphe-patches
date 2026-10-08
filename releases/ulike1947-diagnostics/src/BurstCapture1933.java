package com.hiro.ulike;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.RggbChannelVector;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Range;
import android.view.Surface;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Full-resolution, pre-beauty temporal denoise. The first ordinary still must
 * succeed before this helper can own a capture. Supplemental requests reuse the
 * live device/session/reader and never replace preview, recreate sessions, or
 * request a screenshot. Only one final NV21 frame reaches the original renderer.
 */
public final class BurstCapture1933 {
    private BurstCapture1933() { }

    private static final Object LOCK = new Object();
    private static final ArrayList<Source> SOURCES = new ArrayList<Source>();
    private static final ThreadLocal<Replay> REPLAY = new ThreadLocal<Replay>();
    private static final ArrayList<Retired> RETIRED = new ArrayList<Retired>();
    private static final ArrayList<Ticket> TICKETS = new ArrayList<Ticket>();
    private static final ArrayList<Deferred> DEFERRED = new ArrayList<Deferred>();
    private static final ArrayList<ResultStamp> RESULTS = new ArrayList<ResultStamp>();
    private static final ArrayList<Epoch> EPOCHS = new ArrayList<Epoch>();
    private static State active;
    private static boolean fusionRunning;
    private static final long CAPTURE_DEADLINE_MS = 4800;
    private static final long ONE_FRAME_DEADLINE_MS = 1100;
    private static final long RETIRED_TTL_MS = 8000;
    private static final int MAX_TICKETS = 24;

    private static final class Epoch {
        final WeakReference<Object> owner;
        WeakReference<Object> notified;
        Epoch(Object o){owner=new WeakReference<Object>(o);}
    }

    private static final class Source {
        final WeakReference<Object> owner;
        final WeakReference<Image> image;
        final WeakReference<ImageReader> reader;
        final WeakReference<Object> origin;
        final Epoch epoch;
        final boolean custom;
        Source(Object o, Image i, ImageReader r, Object source, boolean c) {
            owner = new WeakReference<Object>(o);
            image = new WeakReference<Image>(i);
            reader = new WeakReference<ImageReader>(r);
            origin = new WeakReference<Object>(source); custom=c;
            epoch=epoch(o);
        }
    }

    private static final class Replay {
        final Object origin; final ImageReader reader; final Image image;
        boolean acquired,guardOwned,closed;
        Replay(Object o,ImageReader r,Image i){origin=o;reader=r;image=i;}
    }

    private static final class Ticket {
        final WeakReference<Object> owner;
        final WeakReference<ImageReader> reader;
        final CaptureRequest request;
        final String physical;
        final long created;
        volatile long timestamp;
        volatile boolean resolved,canceled;
        Ticket(State s,CaptureRequest r){owner=new WeakReference<Object>(s.owner);
            reader=new WeakReference<ImageReader>(s.reader);request=r;
            physical=s.physicalId;created=SystemClock.elapsedRealtime();}
    }

    private static final class Deferred {
        final Object owner,origin,callback;
        final ImageReader reader; final Image image; final Handler handler;
        final boolean custom; final long timestamp,created;
        final Epoch epoch;
        boolean released;
        Deferred(Source s,Image i,long time,Handler h){owner=s.owner.get();origin=s.origin.get();
            reader=s.reader.get();image=i;custom=s.custom;handler=h;
            callback=field(owner,"x0");timestamp=time;created=SystemClock.elapsedRealtime();epoch=s.epoch;}
    }

    private static final class ResultStamp {
        final WeakReference<Object> owner,callback;
        final CaptureResult result;
        final long created;
        final Epoch epoch;
        ResultStamp(Object o,CaptureResult r){owner=new WeakReference<Object>(o);
            callback=new WeakReference<Object>(field(o,"x0"));result=r;created=SystemClock.elapsedRealtime();epoch=epoch(o);}
    }

    private static final class Retired {
        final WeakReference<ImageReader> reader;
        final long timestamp;
        final long expires;
        Retired(ImageReader r, long t) {
            reader = new WeakReference<ImageReader>(r); timestamp = t;
            expires = SystemClock.elapsedRealtime() + RETIRED_TTL_MS;
        }
    }

    private static final class Frame {
        final byte[] data;
        final long timestamp;
        final TotalCaptureResult total;
        final CaptureResult result;
        final int iso;
        final long exposure;
        Frame(byte[] bytes, long time, TotalCaptureResult t, CaptureResult r) {
            data = bytes; timestamp = time; total = t; result = r;
            iso = positive(r.get(CaptureResult.SENSOR_SENSITIVITY));
            exposure = positive(r.get(CaptureResult.SENSOR_EXPOSURE_TIME));
        }
    }

    private static final class State {
        final Object owner;
        final Object callback;
        final Object cameraInfo;
        final Object route;
        final CameraDevice device;
        final CameraCaptureSession session;
        final CameraCharacteristics logical;
        final CameraCharacteristics sensor;
        final ImageReader reader;
        final Handler handler;
        final NativeBridge bridge;
        final Epoch epoch;
        final String physicalId;
        final int width, height, rotation, noiseLevel, maxFrames;
        final long created;
        final boolean manual;
        final long minExposure, maxExposure;
        final int minIso, maxIso;
        final ArrayList<Frame> frames = new ArrayList<Frame>();
        final FusionPixels1933.Analysis analysis = new FusionPixels1933.Analysis();
        final Frame fallback;
        final ProcessingTiming1947.Trace timing;
        volatile CaptureRequest pendingRequest;
        Ticket ticket;
        TotalCaptureResult pendingResult;
        byte[] pendingBytes;
        long pendingImageTimestamp;
        long pendingStartedTimestamp;
        long requestSerial;
        long desiredExposure;
        int desiredIso;
        int wantedFrames;
        boolean probing = true;
        boolean night;
        volatile boolean closed;
        volatile boolean finishing;
        volatile Thread worker;
        boolean job;

        State(Object o, Object cb, Object info, Object rt, CameraDevice d,
                CameraCaptureSession se, CameraCharacteristics lo,
                CameraCharacteristics sc, ImageReader ir, Handler h,
                NativeBridge b, String physical, int w, int he, int rot,
                int noise, int limit, boolean m, long minE, long maxE,
                int minI, int maxI, Frame first) {
            owner=o; callback=cb; cameraInfo=info; route=rt; device=d; session=se;
            logical=lo; sensor=sc; reader=ir; handler=h; bridge=b;
            epoch=b.epoch;
            physicalId=physical; width=w; height=he; rotation=rot;
            noiseLevel=noise; maxFrames=limit; manual=m;
            minExposure=minE; maxExposure=maxE; minIso=minI; maxIso=maxI;
            created=SystemClock.elapsedRealtime(); fallback=first;
            timing=ProcessingTiming1947.forKey(cb);
            frames.add(first); desiredExposure=first.exposure; desiredIso=first.iso;
            wantedFrames=Math.min(3,limit);
        }
    }

    /** Capture the actual reader identity, including delayed/other-handler Q0. */
    public static Image acquireYuv(Object owner,Object sourceState,ImageReader reader) {
        Image image=acquire(sourceState,reader);
        try { remember(new Source(owner,image,reader,sourceState,true)); }
        catch (RuntimeException ignored) { }
        catch (OutOfMemoryError ignored) { }
        return image;
    }

    public static Image acquireStill(Object listener,ImageReader reader) {
        Image image=acquire(listener,reader);
        try { remember(new Source(field(listener,"a"),image,reader,listener,false)); }
        catch (RuntimeException ignored) { }
        catch (OutOfMemoryError ignored) { }
        return image;
    }

    private static Image acquire(Object origin,ImageReader reader) {
        Replay replay=REPLAY.get();
        if(replay!=null && replay.origin==origin && replay.reader==reader && !replay.acquired){
            replay.acquired=true;return replay.image;
        }
        return reader.acquireNextImage();
    }

    /** Called immediately after CaptureYuv's own reader acquires an Image. */
    public static boolean yuvImage(Object owner, Object sourceState, Image image) {
        Source source=sourceFor(image,false);
        if(source==null){Object r=field(sourceState,"yuv");
            if(!(r instanceof ImageReader))return false;
            source=new Source(owner,image,(ImageReader)r,sourceState,true);remember(source);}
        boolean handled=imageArrived(source,image);
        Replay replay=REPLAY.get();
        if(handled && replay!=null && replay.image==image)replay.guardOwned=true;
        return handled;
    }

    /** Called immediately after the stock still reader's listener acquires an Image.
     * The listener's a field is the exact Image2Mode owner, as pinned in 5.6.2. */
    public static boolean stillImage(Object listener, Image image) {
        Source source=sourceFor(image,false);
        if(source==null)return false;
        boolean handled=imageArrived(source,image);
        Replay replay=REPLAY.get();
        if(handled && replay!=null && replay.image==image)replay.guardOwned=true;
        return handled;
    }

    private static boolean imageArrived(Source source,Image image) {
        Object owner=source.owner.get(),origin=source.origin.get();
        ImageReader reader=source.reader.get();
        if (image == null || owner == null || reader == null) return false;
        if(source.epoch!=epoch(owner)){close(image);return true;}
        // An old listener may run after a replacement reader has been installed.
        // Never infer the originating reader from the owner's new e0 field.
        if(!source.custom && field(owner,"e0")!=reader){close(image);return true;}
        if(source.custom && (Boolean.TRUE.equals(field(origin,"closed"))
                || (field(origin,"original")!=null && field(origin,"original")!=field(owner,"e0")))){
            close(image);return true;
        }
        long timestamp;
        try { timestamp=image.getTimestamp(); }
        catch (RuntimeException e) { return false; }
        State s;
        synchronized (LOCK) {
            pruneRetired();
            for (Retired r : RETIRED) if (r.reader.get()==reader && r.timestamp==timestamp) {
                close(image); return true;
            }
            s=active;
            if(ambiguous(reader) && !ordinaryProof(owner,field(owner,"x0"),timestamp)
                    && !pendingProof(s,owner,reader,timestamp)){
                Handler handler=castHandler(field(owner,"k"));
                if(handler==null || origin==null){close(image);return true;}
                final Deferred held=new Deferred(source,image,timestamp,handler);
                if(DEFERRED.size()>=4){close(image);failureFor(held,"unmatched camera frame queue");return true;}
                DEFERRED.add(held);
                if(!handler.post(new Runnable(){public void run(){resolveDeferred(held);}})){
                    DEFERRED.remove(held);held.released=true;close(image);
                    failureFor(held,"camera handler unavailable");return true;}
                if(!handler.postDelayed(new Runnable(){public void run(){resolveDeferred(held);}},ONE_FRAME_DEADLINE_MS)){
                    DEFERRED.remove(held);held.released=true;close(image);
                    failureFor(held,"camera handler unavailable");}
                return true;
            }
        }
        if (s==null || s.closed || s.owner!=owner || s.reader!=reader
                || s.pendingRequest==null || !current(s)) {
            return false;
        }
        // A still reader is requested once at a time by this coordinator. A
        // result timestamp, never arrival order, decides whether a copied frame
        // can enter a fusion group.
        final byte[] bytes;
        try {
            if (timestamp<=s.fallback.timestamp || image.getWidth()!=s.width
                    || image.getHeight()!=s.height || image.getFormat()!=ImageFormat.YUV_420_888) {
                close(image); return true;
            }
            bytes=copyNv21(image);
        } catch (RuntimeException e) {
            close(image); finishSoon(s); return true;
        } catch (OutOfMemoryError e) {
            close(image); finishSoon(s); return true;
        }
        close(image);
        final State shot=s;
        final long time=timestamp;
        if (!s.handler.post(new Runnable() { public void run() {
            if (!current(shot) || shot.closed || shot.finishing) return;
            if (shot.pendingBytes!=null || shot.pendingRequest==null) return;
            shot.pendingBytes=bytes; shot.pendingImageTimestamp=time; pair(shot);
        }})) finishSoon(s);
        return true;
    }

    /** Guard at the very start of Q0, before the single-frame metadata hook.
     * False means no ownership transfer and the original method continues. */
    public static boolean beginImage(Object owner, Image image, TotalCaptureResult total) {
        Source source=sourceFor(image,true);
        State created=null;
        try {
            if (owner==null || image==null || total==null || source==null
                    || source.owner.get()!=owner || source.image.get()!=image) return false;
            if(source.epoch!=epoch(owner))return true;
            final ImageReader reader=source.reader.get();
            if (reader==null || image.getFormat()!=ImageFormat.YUV_420_888
                    || reader.getImageFormat()!=ImageFormat.YUV_420_888
                    || reader.getWidth()!=image.getWidth() || reader.getHeight()!=image.getHeight()) return false;
            synchronized (LOCK) {
                pruneRetired();
                if (active!=null || fusionRunning || TICKETS.size()>=MAX_TICKETS) return false;
            }
            PhotoDetail.Settings settings=PhotoDetail.snapshot1932();
            if (settings==null || !settings.noiseOn || settings.noiseLevel<=0) return false;
            int width=image.getWidth(), height=image.getHeight();
            int limit=CapturePolicy1933.frameLimit(width,height,availableMemory());
            if (limit<2) return false;
            CameraDevice device=castDevice(field(owner,"j"));
            CameraCaptureSession session=castSession(field(owner,"d"));
            CameraCharacteristics logical=castCharacteristics(field(owner,"a"));
            Handler handler=castHandler(field(owner,"k"));
            Object callback=field(owner,"x0"), info=field(owner,"g");
            if (device==null || session==null || logical==null || handler==null
                    || callback==null || info==null || field(owner,"y0")!=null
                    || !recognized(callback)) return false;
            Object route=route(device);
            if (Boolean.TRUE.equals(field(route,"failed"))) return false;
            String physical=string(field(route,"physicalId"));
            CameraCharacteristics sensor=castCharacteristics(field(route,"lens"));
            if (sensor==null) { if (physical.length()!=0) return false; sensor=logical; }
            CaptureResult result=select(total,physical);
            if (result==null || positive(result.get(CaptureResult.SENSOR_TIMESTAMP))!=image.getTimestamp()) return false;
            int iso=positive(result.get(CaptureResult.SENSOR_SENSITIVITY));
            long exposure=positive(result.get(CaptureResult.SENSOR_EXPOSURE_TIME));
            if (iso==0 || exposure==0 || exposure>250000000L || total.getRequest()==null
                    || total.getRequest().isReprocess() || flash(result)) return false;
            Integer intent=total.getRequest().get(CaptureRequest.CONTROL_CAPTURE_INTENT);
            if (intent==null || intent.intValue()!=CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE) return false;
            // AWB lock is required even for manual sensor exposure, because
            // changing white balance between frames defeats same-colour fusion.
            if (!Boolean.TRUE.equals(logical.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE))) return false;
            boolean manual=contains(logical.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
                    && contains(sensor.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),CaptureRequest.CONTROL_AE_MODE_OFF);
            if (!manual && !Boolean.TRUE.equals(logical.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE))) return false;
            Range<Long> exposureRange=sensor.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            Range<Integer> isoRange=sensor.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            long minE=exposureRange==null?exposure:exposureRange.getLower();
            long maxE=exposureRange==null?exposure:exposureRange.getUpper();
            Long maximumFrameDuration=sensor.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION);
            if(maximumFrameDuration!=null && maximumFrameDuration>0)maxE=Math.min(maxE,maximumFrameDuration);
            int minI=isoRange==null?iso:isoRange.getLower();
            int maxI=isoRange==null?iso:isoRange.getUpper();
            if (minE<=0 || maxE<minE || minI<=0 || maxI<minI) return false;
            if (manual && (exposure<minE || exposure>maxE || iso<minI || iso>maxI)) return false;
            if (manual && (exposureRange==null || isoRange==null)) manual=false;
            if (!manual && !Boolean.TRUE.equals(logical.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE))) return false;
            int facing=integer(field(owner,"m0"),-1);
            if (facing!=0 && facing!=1) return false;
            NativeBridge bridge=new NativeBridge(owner,callback,info);
            byte[] bytes=copyNv21(image);
            Frame first=new Frame(bytes,image.getTimestamp(),total,result);
            final State s=new State(owner,callback,info,route,device,session,logical,sensor,
                    reader,handler,bridge,physical,width,height,facing==1?270:90,
                    settings.noiseLevel,limit,manual,minE,maxE,minI,maxI,first);
            created=s;
            synchronized (LOCK) {
                if (active!=null || fusionRunning) return false;
                ExitJobs185.begin(); s.job=true; active=s;
            }
            if (!handler.post(new Runnable() { public void run() { requestNext(s); }})) {
                dispose(s); return false;
            }
            return true;
        } catch (ReflectiveOperationException e) { if(created!=null)dispose(created); return false;
        } catch (RuntimeException e) { if(created!=null)dispose(created); return false;
        } catch (LinkageError e) { if(created!=null)dispose(created); return false;
        } catch (OutOfMemoryError e) { if(created!=null)dispose(created); return false; }
    }

    /** New still and existing release/failure paths only cancel this helper's work. */
    public static void canceled(Object owner) {
        State s;
        ArrayList<Image> abandoned=new ArrayList<Image>();
        synchronized (LOCK) {
            s=active;
            for(int i=EPOCHS.size()-1;i>=0;i--){Epoch e=EPOCHS.get(i);
                if(e.owner.get()==null || e.owner.get()==owner)EPOCHS.remove(i);}
            for(int i=DEFERRED.size()-1;i>=0;i--){Deferred d=DEFERRED.get(i);
                if(d.owner==owner || d.callback==owner){d.released=true;DEFERRED.remove(i);abandoned.add(d.image);}}
        }
        if (s!=null && (owner==s.owner || owner==s.callback)) dispose(s);
        for(Image image:abandoned)close(image);
    }

    /** Observe ordinary still results. Preview results are never kept. A result
     * can prove a deferred image belongs to the fresh ordinary shot while an
     * older canceled supplemental request still has no timestamp callback. */
    public static void observedResult(Object owner,CaptureResult result) {
        if(owner==null || result==null)return;
        try{
            CaptureRequest request=result.getRequest();
            if(request==null || !Integer.valueOf(CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE)
                    .equals(request.get(CaptureRequest.CONTROL_CAPTURE_INTENT)))return;
            synchronized(LOCK){
                for(Ticket t:TICKETS)if(t.request==request)return;
                if(RESULTS.size()>=16)RESULTS.remove(0);
                RESULTS.add(new ResultStamp(owner,result));
            }
            wakeDeferred();
        }catch(RuntimeException ignored){}
    }

    private static void requestNext(final State s) {
        if (!current(s) || s.closed) { dispose(s); return; }
        if (s.finishing || s.pendingRequest!=null) return;
        if (SystemClock.elapsedRealtime()-s.created>=CAPTURE_DEADLINE_MS) { finish(s); return; }
        try {
            CaptureRequest.Builder b=newBuilder(s);
            final CaptureRequest request=b.build();
            s.pendingRequest=request;
            final Ticket ticket=new Ticket(s,request);
            s.ticket=ticket;
            synchronized(LOCK){
                pruneRetired();
                if(TICKETS.size()>=MAX_TICKETS)throw new IllegalStateException("camera request history full");
                TICKETS.add(ticket);
            }
            s.pendingBytes=null; s.pendingResult=null;
            s.pendingImageTimestamp=0; s.pendingStartedTimestamp=0;
            final long serial=++s.requestSerial;
            CameraCaptureSession.CaptureCallback cb=new CameraCaptureSession.CaptureCallback() {
                @Override public void onCaptureStarted(CameraCaptureSession se,CaptureRequest r,long timestamp,long frame) {
                    if (se!=s.session || r!=request) return;
                    if(ticket.physical.length()==0){ticket.timestamp=timestamp;ticket.resolved=timestamp>0;}
                    if (s.closed || s.finishing) {
                        ticket.canceled=true;
                        if(ticket.physical.length()==0)retire(s.reader,timestamp);
                        wakeDeferred();return;
                    }
                    if (s.pendingRequest==request) s.pendingStartedTimestamp=timestamp;
                    wakeDeferred();
                }
                @Override public void onCaptureCompleted(CameraCaptureSession se,CaptureRequest r,TotalCaptureResult result) {
                    if (se!=s.session || r!=request) return;
                    CaptureResult matched=select(result,s.physicalId);
                    long timestamp=matched==null?0:positive(matched.get(CaptureResult.SENSOR_TIMESTAMP));
                    ticket.timestamp=timestamp;ticket.resolved=timestamp>0;
                    if (s.closed || s.finishing || !current(s)) {
                        ticket.canceled=true;retire(s.reader,timestamp);wakeDeferred();return;
                    }
                    if (s.pendingRequest!=request || result.getRequest()!=request) return;
                    s.pendingResult=result; pair(s);
                    wakeDeferred();
                }
                @Override public void onCaptureFailed(CameraCaptureSession se,CaptureRequest r,CaptureFailure failure) {
                    if (se==s.session && r==request && !s.closed) finish(s);
                }
                @Override public void onCaptureBufferLost(CameraCaptureSession se,CaptureRequest r,Surface target,long frame) {
                    if (se==s.session && r==request && target==s.reader.getSurface() && !s.closed) finish(s);
                }
                @Override public void onCaptureSequenceAborted(CameraCaptureSession se,int id) {
                    if (se==s.session && !s.closed && s.pendingRequest==request) finish(s);
                }
            };
            s.session.capture(request,cb,s.handler);
            if(!s.handler.postDelayed(new Runnable() { public void run() {
                if (!s.closed && !s.finishing && s.requestSerial==serial && s.pendingRequest==request) finish(s);
            }},ONE_FRAME_DEADLINE_MS))finish(s);
        } catch (Exception e) { finish(s);
        } catch (LinkageError e) { finish(s);
        } catch (OutOfMemoryError e) { finish(s); }
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private static CaptureRequest.Builder newBuilder(State s) throws Exception {
        CaptureRequest original=s.fallback.total.getRequest();
        CaptureRequest.Builder b;
        if (s.physicalId.length()!=0 && Build.VERSION.SDK_INT>=28) {
            if (!s.logical.getPhysicalCameraIds().contains(s.physicalId)) throw new IllegalStateException("physical camera changed");
            b=s.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE,Collections.singleton(s.physicalId));
        } else b=s.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        for (CaptureRequest.Key key:original.getKeys()) {
            Object value=original.get(key);
            if (value!=null) b.set(key,value);
        }
        b.addTarget(s.reader.getSurface());
        b.setTag("ULikeBurst1933");
        b.set(CaptureRequest.CONTROL_CAPTURE_INTENT,CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
        b.set(CaptureRequest.CONTROL_ENABLE_ZSL,false);
        b.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        b.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
        b.set(CaptureRequest.CONTROL_AWB_LOCK,true);
        if (s.manual) {
            b.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
            b.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);
            b.set(CaptureRequest.CONTROL_AE_LOCK,false);
            b.set(CaptureRequest.SENSOR_EXPOSURE_TIME,s.desiredExposure);
            b.set(CaptureRequest.SENSOR_SENSITIVITY,s.desiredIso);
            long duration=positive(s.fallback.result.get(CaptureResult.SENSOR_FRAME_DURATION));
            duration=Math.max(s.desiredExposure,duration);
            Long maximum=s.sensor.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION);
            if (maximum!=null && maximum>0) duration=Math.min(duration,maximum);
            b.set(CaptureRequest.SENSOR_FRAME_DURATION,duration);
        } else b.set(CaptureRequest.CONTROL_AE_LOCK,true);
        Float focus=s.fallback.result.get(CaptureResult.LENS_FOCUS_DISTANCE);
        Integer lens=s.fallback.result.get(CaptureResult.LENS_STATE);
        if (focus!=null && focus>=0 && lens!=null && lens==CaptureResult.LENS_STATE_STATIONARY
                && contains(s.sensor.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),CaptureRequest.CONTROL_AF_MODE_OFF)) {
            b.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_OFF);
            b.set(CaptureRequest.LENS_FOCUS_DISTANCE,focus);
        }
        if (s.physicalId.length()!=0 && Build.VERSION.SDK_INT>=28) {
            List<CaptureRequest.Key<?>> keys=s.logical.getAvailablePhysicalCameraRequestKeys();
            if (keys!=null) for (CaptureRequest.Key key:keys) {
                Object value=b.get(key);
                if (value!=null) b.setPhysicalCameraKey(key,value,s.physicalId);
            }
        }
        return b;
    }

    private static void pair(final State s) {
        if (s.closed || s.finishing || s.pendingBytes==null || s.pendingResult==null) return;
        CaptureResult matched=select(s.pendingResult,s.physicalId);
        long timestamp=matched==null?0:positive(matched.get(CaptureResult.SENSOR_TIMESTAMP));
        if (timestamp<=0 || timestamp!=s.pendingImageTimestamp
                || (s.pendingStartedTimestamp>0 && s.physicalId.length()==0 && timestamp!=s.pendingStartedTimestamp)) {
            // Mismatched arrivals cannot be substituted for the requested frame.
            finish(s); return;
        }
        Frame candidate=new Frame(s.pendingBytes,timestamp,s.pendingResult,matched);
        synchronized(LOCK){
            if(s.ticket!=null)TICKETS.remove(s.ticket);
            retireLocked(s.reader,timestamp);
        }
        s.ticket=null;
        s.pendingBytes=null; s.pendingResult=null; s.pendingRequest=null;
        Frame reference=s.frames.get(0);
        if (!sameGeometry(reference.result,candidate.result)
                || !sameColour(reference.result,candidate.result)) { finish(s); return; }
        // The original shutter frame permanently owns composition and metadata.
        // Replanning exposure after a probe formerly cleared it and replaced it
        // with a future shot, often after the user had already moved the phone.
        if (candidate.timestamp<=reference.timestamp
                || candidate.timestamp-reference.timestamp>FusionPixels1933.CAPTURE_WINDOW_NANOS
                || !CapturePolicy1933.compatible(reference.timestamp,candidate.timestamp,
                    reference.exposure,candidate.exposure,reference.iso,candidate.iso)) { finish(s); return; }
        s.frames.add(candidate);
        if (s.probing && s.frames.size()==2) {
            s.probing=false;
            final Frame first=s.frames.get(0), second=s.frames.get(1);
            // Registration is CPU work and never runs on a camera/reader handler.
            runWorker(s,new Work() { public Runnable run() {
                final FusionPixels1933.Motion motion;
                ProcessingTiming1947.Token timing=ProcessingTiming1947.beginStage(s.timing,ProcessingTiming1947.FUSION);
                try {
                    motion=FusionPixels1933.probeMotion(
                            first.data,second.data,s.width,s.height,first.iso,first.exposure,
                            s.analysis,first.timestamp,second.timestamp,second.iso,second.exposure);
                } finally { ProcessingTiming1947.end(timing); }
                return new Runnable() { public void run() {
                    if (!current(s) || s.closed || s.finishing) { dispose(s); return; }
                    s.night=CapturePolicy1933.night(first.iso,first.exposure,motion.darkness);
                    CapturePolicy1933.Decision d=CapturePolicy1933.choose(first.exposure,first.iso,
                            s.minExposure,s.maxExposure,s.minIso,s.maxIso,s.maxFrames,
                            s.manual,s.night,motion.reliable && !motion.duplicate,
                            motion.score,motion.movingFraction,motion.globalShiftPixels,
                            second.timestamp-first.timestamp);
                    s.wantedFrames=d.frames;
                    if (!FusionPixels1933.withinShutterMotion(motion,s.width,s.height)) {
                        // Motion cannot be repaired by photographing a new scene
                        // with a shorter exposure. Keep the real shutter image.
                        s.frames.remove(1); s.analysis.clear(); finish(s); return;
                    }
                    // Honor the noise-policy frame count, including four frames
                    // for stationary night shots, but keep this shot's original
                    // exposure/ISO. Applying d.changedExposure would require a
                    // replacement group and move the picture to a later moment.
                    if (s.frames.size()>=s.wantedFrames) finish(s);
                    else requestNext(s);
                }};
            }});
        } else if (s.frames.size()>=s.wantedFrames) finish(s);
        else requestNext(s);
    }

    private static void finishSoon(final State s) {
        if (!s.handler.post(new Runnable() { public void run() { finish(s); }})) failUnavailable(s);
    }

    private static void finish(final State s) {
        if (s.closed || s.finishing) return;
        if (!current(s)) { dispose(s); return; }
        s.finishing=true;
        if(s.ticket!=null)s.ticket.canceled=true;
        if (s.pendingStartedTimestamp>0 && s.physicalId.length()==0) retire(s.reader,s.pendingStartedTimestamp);
        s.pendingBytes=null; s.pendingResult=null; s.pendingRequest=null;
        runWorker(s,new Work() { public Runnable run() {
            byte[] output=s.frames.get(0).data;
            Frame reference=s.frames.get(0);
            try {
                int n=s.frames.size();
                byte[][] bytes=new byte[n][]; long[] times=new long[n],exposures=new long[n]; int[] isos=new int[n];
                for (int i=0;i<n;i++) {
                    Frame f=s.frames.get(i); bytes[i]=f.data; times[i]=f.timestamp;
                    exposures[i]=f.exposure; isos[i]=f.iso;
                }
                FusionPixels1933.Result result;
                ProcessingTiming1947.Token timing=ProcessingTiming1947.beginStage(s.timing,ProcessingTiming1947.FUSION);
                try {
                    result=FusionPixels1933.fuse(bytes,s.width,s.height,
                            times,exposures,isos,s.noiseLevel,s.night,4,s.analysis);
                } finally { ProcessingTiming1947.end(timing); }
                if (result!=null && result.nv21!=null && result.nv21.length==output.length
                        && result.referenceIndex==0 && !result.cancelled
                        && result.referenceTimestamp==s.fallback.timestamp) {
                    output=result.nv21;
                    reference=s.frames.get(result.referenceIndex);
                }
            } catch (RuntimeException e) { /* Keep the real first frame. */
            } catch (OutOfMemoryError e) { /* Keep the real first frame. */ }
            final byte[] finalBytes=output;
            final Frame finalReference=reference;
            return new Runnable() { public void run() {
                try {
                    if (s.closed || !current(s)) return;
                    s.analysis.close();
                    if (!ShotContext1932.receivedValues1933(s.owner,s.callback,finalReference.timestamp,
                            s.width,s.height,finalReference.total)) {
                        s.bridge.fail(new IllegalStateException("capture metadata association unavailable"),integer(field(s.owner,"m0"),0));
                        return;
                    }
                    s.bridge.deliver(finalBytes,s.width,s.height,s.rotation,finalReference.total);
                } catch (Exception e) {
                    if (!s.closed && current(s)) s.bridge.fail(e,integer(field(s.owner,"m0"),0));
                } finally { dispose(s); }
            }};
        }});
    }

    private interface Work { Runnable run(); }

    /** One coordinator thread; the fusion kernel includes it in its <=4 workers.
     * A canceled still cannot start another simultaneous fusion worker group. */
    private static void runWorker(final State s,final Work work) {
        boolean busy;
        synchronized (LOCK) {
            if (s.closed) return;
            busy=fusionRunning;
            if(!busy)fusionRunning=true;
        }
        if(busy){deliverFallbackSoon(s);return;}
        try {
            Thread thread=new Thread(new Runnable() { public void run() {
                Runnable next=null;
                boolean failed=false;
                ProcessingTiming1947.Scope timing=ProcessingTiming1947.enter(s.timing);
                try { if (!s.closed) next=work.run(); }
                catch (RuntimeException e) { failed=true; }
                catch (OutOfMemoryError e) { failed=true; }
                finally {
                    ProcessingTiming1947.restore(timing);
                    synchronized (LOCK) {
                        fusionRunning=false;
                        if(s.worker==Thread.currentThread())s.worker=null;
                    }
                }
                if (failed) {
                    if (s.finishing) deliverFallbackSoon(s);
                    else finishSoon(s);
                } else if (next!=null && !s.handler.post(next)) failUnavailable(s);
            }},"ULikeBurst1933");
            thread.setDaemon(true); s.worker=thread; thread.start();
        } catch (RuntimeException e) {
            synchronized (LOCK) { fusionRunning=false; s.worker=null; }
            deliverFallbackSoon(s);
        } catch (OutOfMemoryError e) {
            synchronized (LOCK) { fusionRunning=false; s.worker=null; }
            deliverFallbackSoon(s);
        }
    }

    private static void deliverFallbackSoon(final State s) {
        s.finishing=true;
        if(s.ticket!=null)s.ticket.canceled=true;
        if (!s.handler.post(new Runnable() { public void run() {
            try {
                Frame reference=s.frames.get(0);
                if (!s.closed && current(s)) {
                    s.analysis.close();
                    if(ShotContext1932.receivedValues1933(s.owner,s.callback,
                            reference.timestamp,s.width,s.height,reference.total))
                        s.bridge.deliver(reference.data,s.width,s.height,s.rotation,reference.total);
                    else s.bridge.fail(new IllegalStateException("capture metadata association unavailable"),integer(field(s.owner,"m0"),0));
                }
            } catch (Exception failure) {
                if (!s.closed && current(s)) s.bridge.fail(failure,integer(field(s.owner,"m0"),0));
            } finally { dispose(s); }
        }})) failUnavailable(s);
    }

    private static void failUnavailable(State s) {
        try {
            if(!s.closed && current(s))s.bridge.fail(
                    new IllegalStateException("camera handler unavailable"),integer(field(s.owner,"m0"),0));
        } finally { dispose(s); }
    }

    private static boolean current(State s) {
        if (s.closed || s.epoch!=epoch(s.owner) || field(s.owner,"x0")!=s.callback
                || field(s.owner,"j")!=s.device || field(s.owner,"d")!=s.session
                || route(s.device)!=s.route) return false;
        if (Boolean.TRUE.equals(field(s.route,"failed"))) return false;
        synchronized (LOCK) { return active==s; }
    }

    private static void dispose(State s) {
        boolean end=false;
        Thread worker;
        synchronized (LOCK) {
            if (s.closed) return;
            s.closed=true; s.analysis.close();
            if(s.ticket!=null)s.ticket.canceled=true;
            if (s.pendingStartedTimestamp>0 && s.physicalId.length()==0) retireLocked(s.reader,s.pendingStartedTimestamp);
            if (active==s) active=null;
            s.pendingBytes=null; s.pendingResult=null; s.pendingRequest=null;
            if (s.job) { s.job=false; end=true; }
            worker=s.worker;
        }
        if(worker!=null && worker!=Thread.currentThread())worker.interrupt();
        if (end) ExitJobs185.end();
        wakeDeferred();
    }

    private static void retire(ImageReader reader,long timestamp) {
        if (timestamp<=0) return;
        synchronized (LOCK) { retireLocked(reader,timestamp); }
    }
    private static void retireLocked(ImageReader reader,long timestamp) {
        if (timestamp<=0) return;
        pruneRetired();
        if (RETIRED.size()>=24) RETIRED.remove(0);
        RETIRED.add(new Retired(reader,timestamp));
    }
    private static void pruneRetired() {
        long now=SystemClock.elapsedRealtime();
        for (int i=RETIRED.size()-1;i>=0;i--) {
            Retired r=RETIRED.get(i);
            if (r.reader.get()==null || now>=r.expires) RETIRED.remove(i);
        }
        for(int i=TICKETS.size()-1;i>=0;i--){Ticket t=TICKETS.get(i);
            if(t.reader.get()==null || t.owner.get()==null || now-t.created>=RETIRED_TTL_MS)TICKETS.remove(i);}
        for(int i=RESULTS.size()-1;i>=0;i--){ResultStamp r=RESULTS.get(i);
            if(r.owner.get()==null || r.callback.get()==null || now-r.created>=RETIRED_TTL_MS)RESULTS.remove(i);}
    }

    private static void remember(Source source){
        if(source.image.get()==null)return;
        synchronized(LOCK){
            for(int i=SOURCES.size()-1;i>=0;i--){Source old=SOURCES.get(i);
                if(old.image.get()==null || old.image.get()==source.image.get() || old.owner.get()==null)SOURCES.remove(i);}
            if(SOURCES.size()>=24)SOURCES.remove(0);
            SOURCES.add(source);
        }
    }

    private static Source sourceFor(Image image,boolean remove){
        if(image==null)return null;
        synchronized(LOCK){
            for(int i=SOURCES.size()-1;i>=0;i--){Source source=SOURCES.get(i);
                if(source.image.get()==image){if(remove)SOURCES.remove(i);return source;}
                if(source.image.get()==null || source.owner.get()==null)SOURCES.remove(i);}
        }
        return null;
    }

    private static boolean ambiguous(ImageReader reader){
        for(Ticket t:TICKETS)if(t.reader.get()==reader && t.canceled && !t.resolved)return true;
        return false;
    }

    private static boolean pendingProof(State s,Object owner,ImageReader reader,long timestamp){
        return s!=null && !s.closed && !s.finishing && s.owner==owner && s.reader==reader
                && s.ticket!=null && !s.ticket.canceled && s.ticket.resolved
                && s.ticket.timestamp==timestamp && s.pendingRequest==s.ticket.request
                && current(s);
    }

    private static boolean ordinaryProof(Object owner,Object callback,long timestamp){
        for(ResultStamp r:RESULTS)if(r.owner.get()==owner && r.callback.get()==callback && r.epoch==epoch(owner)){
            if(positive(r.result.get(CaptureResult.SENSOR_TIMESTAMP))==timestamp)return true;
            if(r.result instanceof TotalCaptureResult && Build.VERSION.SDK_INT>=28){
                Map<String,CaptureResult> map=((TotalCaptureResult)r.result).getPhysicalCameraResults();
                if(map!=null)for(CaptureResult physical:map.values())
                    if(positive(physical.get(CaptureResult.SENSOR_TIMESTAMP))==timestamp)return true;
            }
        }
        return false;
    }

    private static void wakeDeferred(){
        ArrayList<Deferred> held;
        synchronized(LOCK){held=new ArrayList<Deferred>(DEFERRED);}
        for(final Deferred d:held)if(!d.handler.post(new Runnable(){public void run(){resolveDeferred(d);}})){
            boolean abandoned=false;
            synchronized(LOCK){if(!d.released){d.released=true;DEFERRED.remove(d);abandoned=true;}}
            if(abandoned){close(d.image);failureFor(d,"camera handler unavailable");}
        }
    }

    private static void resolveDeferred(Deferred d){
        int action=0; // 1 close late/stale; 2 continue original listener; 3 fail ambiguous current shot
        synchronized(LOCK){
            if(d.released)return;
            pruneRetired();
            for(Retired r:RETIRED)if(r.reader.get()==d.reader && r.timestamp==d.timestamp){action=1;break;}
            if(action==0 && d.epoch!=epoch(d.owner))action=1;
            if(action==0 && field(d.owner,"x0")!=d.callback)action=1;
            if(action==0 && !d.custom && field(d.owner,"e0")!=d.reader)action=1;
            if(action==0 && d.custom && (Boolean.TRUE.equals(field(d.origin,"closed"))
                    || (field(d.origin,"original")!=null && field(d.origin,"original")!=field(d.owner,"e0"))))action=1;
            if(action==0 && (ordinaryProof(d.owner,d.callback,d.timestamp)
                    || pendingProof(active,d.owner,d.reader,d.timestamp) || !ambiguous(d.reader)))action=2;
            if(action==0 && SystemClock.elapsedRealtime()-d.created>=ONE_FRAME_DEADLINE_MS)action=3;
            if(action!=0){d.released=true;DEFERRED.remove(d);}
        }
        if(action==1)close(d.image);
        else if(action==2)replay(d);
        else if(action==3){close(d.image);failureFor(d,"camera frame ownership timeout");}
    }

    private static void replay(Deferred d){
        Replay previous=REPLAY.get(),replay=new Replay(d.origin,d.reader,d.image);
        REPLAY.set(replay);
        try{
            if(d.custom){
                Method method=Class.forName("com.hiro.ulike.CaptureYuv").getDeclaredMethod("receive",
                        Object.class,d.origin.getClass(),ImageReader.class);
                method.setAccessible(true);method.invoke(null,d.owner,d.origin,d.reader);
            }else{
                Method method=d.origin.getClass().getMethod("onImageAvailable",ImageReader.class);
                method.setAccessible(true);method.invoke(d.origin,d.reader);
            }
        }catch(ReflectiveOperationException failure){
            reclaimReplay(d,replay);
            failureFor(d,"camera frame continuation unavailable");
        }catch(RuntimeException failure){
            reclaimReplay(d,replay);
            failureFor(d,"camera frame continuation unavailable");
        }finally{
            if(previous==null)REPLAY.remove();else REPLAY.set(previous);
        }
    }

    private static void reclaimReplay(Deferred d,Replay replay){
        if(replay.closed || replay.guardOwned)return;
        // Stock a$g has no finally after acquire. CaptureYuv.receive, by
        // contrast, closes on every exceptional exit and retains successful
        // unmatched input in State.pending until its result arrives. Preserve
        // that receiver's ownership instead of closing an in-flight Image.
        if(!replay.acquired || !d.custom)close(d.image);
    }

    private static void failureFor(Deferred d,String reason){
        if(d.owner==null || d.callback==null || d.epoch!=epoch(d.owner) || field(d.owner,"x0")!=d.callback)return;
        try{
            Method failed=Class.forName("com.hiro.ulike.CaptureYuv").getDeclaredMethod("failed",Object.class);
            failed.setAccessible(true);failed.invoke(null,d.owner);
        }catch(ReflectiveOperationException ignored){}
        failCallback(d.epoch,d.callback,new IllegalStateException(reason),integer(field(d.owner,"m0"),0));
    }

    /** Native callbacks are anonymous/package-private classes in the APK. Mark
     * completion by identity so two simultaneously expiring held images cannot
     * report two failures for the same shutter. Only one overload is invoked. */
    private static Epoch epoch(Object owner) {
        synchronized(LOCK){
            for(int i=EPOCHS.size()-1;i>=0;i--){Epoch e=EPOCHS.get(i);Object prior=e.owner.get();
                if(prior==owner)return e;
                if(prior==null)EPOCHS.remove(i);
            }
            if(EPOCHS.size()>=32)EPOCHS.remove(0);
            Epoch epoch=new Epoch(owner);EPOCHS.add(epoch);return epoch;
        }
    }

    private static boolean notification(Epoch epoch,Object callback) {
        synchronized(LOCK){
            if(epoch.notified!=null && epoch.notified.get()==callback)return false;
            epoch.notified=new WeakReference<Object>(callback);
            return true;
        }
    }

    private static void failCallback(Epoch epoch,Object callback,Exception failure,int facing) {
        if(callback==null || !notification(epoch,callback))return;
        try {
            Method method;
            try { method=callback.getClass().getMethod("onTakenFail",Exception.class); }
            catch(NoSuchMethodException absent){
                method=callback.getClass().getMethod("onTakenFail",Exception.class,int.class);
            }
            method.setAccessible(true);
            if(method.getParameterTypes().length==2)method.invoke(callback,failure,facing);
            else method.invoke(callback,failure);
        } catch(ReflectiveOperationException ignored) { }
        catch(RuntimeException ignored) { }
    }

    /** Owned tight NV21 copy; Image and all source ByteBuffer positions stay unchanged. */
    static byte[] copyNv21(Image image) {
        return YuvPlanes1934.owned(image);
    }

    private static boolean sameGeometry(CaptureResult a,CaptureResult b) {
        return equal(a.get(CaptureResult.SCALER_CROP_REGION),b.get(CaptureResult.SCALER_CROP_REGION))
                && (Build.VERSION.SDK_INT<28 || equal(a.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID),b.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)))
                && nearNullable(a.get(CaptureResult.LENS_FOCAL_LENGTH),b.get(CaptureResult.LENS_FOCAL_LENGTH),0.005)
                && nearNullable(a.get(CaptureResult.LENS_FOCUS_DISTANCE),b.get(CaptureResult.LENS_FOCUS_DISTANCE),0.04)
                && (Build.VERSION.SDK_INT<30 || nearNullable(a.get(CaptureResult.CONTROL_ZOOM_RATIO),b.get(CaptureResult.CONTROL_ZOOM_RATIO),0.005));
    }
    private static boolean sameColour(CaptureResult a,CaptureResult b) {
        RggbChannelVector x=a.get(CaptureResult.COLOR_CORRECTION_GAINS),y=b.get(CaptureResult.COLOR_CORRECTION_GAINS);
        if (x==null || y==null) return x==y;
        return near(x.getRed(),y.getRed(),0.035) && near(x.getGreenEven(),y.getGreenEven(),0.035)
                && near(x.getGreenOdd(),y.getGreenOdd(),0.035) && near(x.getBlue(),y.getBlue(),0.035);
    }
    private static boolean flash(CaptureResult r) {
        Integer mode=r.get(CaptureResult.FLASH_MODE),state=r.get(CaptureResult.FLASH_STATE);
        return (mode!=null && mode!=CaptureResult.FLASH_MODE_OFF)
                || (state!=null && (state==CaptureResult.FLASH_STATE_FIRED || state==CaptureResult.FLASH_STATE_PARTIAL));
    }
    private static CaptureResult select(CaptureResult r,String id) {
        if (id.length()==0) return r;
        if (Build.VERSION.SDK_INT<28 || !(r instanceof TotalCaptureResult)) return null;
        Map<String,CaptureResult> p=((TotalCaptureResult)r).getPhysicalCameraResults();
        return p==null?null:p.get(id);
    }
    private static boolean recognized(Object cb) {
        Object value=cb;
        for (int i=0;i<3 && value!=null;i++) {
            String name=value.getClass().getName();
            if ("com.ss.android.vesdk.TECameraVideoRecorder$60".equals(name)) return true;
            if ("i.s.a.w.q$g$a".equals(name)) value=field(field(value,"a"),"c");
            else return false;
        }
        return false;
    }

    private static final class NativeBridge {
        final Object callback,info,format;
        final Epoch epoch;
        final Constructor<?> constructor;
        final Method deliver;
        NativeBridge(Object owner,Object cb,Object i) throws ReflectiveOperationException {
            callback=cb; info=i;epoch=epoch(owner);
            Class<?> frame=Class.forName("i.s.a.w.m"),pixel=Class.forName("i.s.a.w.m$d");
            format=pixel.getField("PIXEL_FORMAT_NV21").get(null);
            constructor=frame.getConstructor(byte[].class,pixel,int.class,int.class,int.class);
            deliver=cb.getClass().getMethod("onPictureTaken",frame,Class.forName("i.s.a.w.i"));
            deliver.setAccessible(true);
        }
        void deliver(byte[] bytes,int w,int h,int rotation,TotalCaptureResult result) throws ReflectiveOperationException {
            Object frame=constructor.newInstance(bytes,format,w,h,rotation);
            // Preserve native metadata when the SDK exposes its existing holder.
            try {
                Class<?> metadata=Class.forName("i.s.a.w.m$e");
                Object values=metadata.getConstructor().newInstance();
                metadata.getField("d").set(values,result);
                metadata.getField("c").setLong(values,System.currentTimeMillis());
                frame.getClass().getMethod("u",metadata).invoke(frame,values);
            } catch (ReflectiveOperationException ignored) { }
            if(notification(epoch,callback)){
                try { deliver.invoke(callback,frame,info); }
                catch(ReflectiveOperationException failure){
                    synchronized(LOCK){if(epoch.notified!=null && epoch.notified.get()==callback)epoch.notified=null;}
                    throw failure;
                }
                catch(RuntimeException failure){
                    synchronized(LOCK){if(epoch.notified!=null && epoch.notified.get()==callback)epoch.notified=null;}
                    throw failure;
                }
            }
        }
        void fail(Exception failure,int facing) {
            failCallback(epoch,callback,failure,facing);
        }
    }

    private static Object route(CameraDevice device) {
        try {
            Method m=Class.forName("com.hiro.ulike.OpticalZoom").getDeclaredMethod("route",CameraDevice.class);
            m.setAccessible(true); return m.invoke(null,device);
        } catch (ReflectiveOperationException e) { return null;
        } catch (RuntimeException e) { return null; }
    }
    private static Object field(Object o,String name) {
        if (o==null) return null;
        for (Class<?> c=o.getClass();c!=null;c=c.getSuperclass()) {
            try { Field f=c.getDeclaredField(name); f.setAccessible(true); return f.get(o); }
            catch (NoSuchFieldException e) { }
            catch (ReflectiveOperationException e) { return null; }
            catch (RuntimeException e) { return null; }
        }
        return null;
    }
    private static long availableMemory() {
        Runtime r=Runtime.getRuntime(); return Math.max(0,r.maxMemory()-(r.totalMemory()-r.freeMemory()));
    }
    private static void close(Image image) {
        Replay replay=REPLAY.get();
        if(replay!=null && replay.image==image){if(replay.closed)return;replay.closed=true;}
        try { image.close(); } catch (RuntimeException ignored) { }
    }
    private static boolean contains(int[] a,int x) { if (a!=null) for (int v:a) if(v==x)return true; return false; }
    private static boolean equal(Object a,Object b) { return a==null?b==null:a.equals(b); }
    private static boolean near(double a,double b,double tolerance) {
        return a>0 && b>0 && Math.abs(a/b-1)<=tolerance;
    }
    private static boolean nearNullable(Float a,Float b,double tolerance) {
        if (a==null || b==null) return a==b;
        if (!Float.isFinite(a) || !Float.isFinite(b)) return false;
        return Math.abs(a-b)<=Math.max(0.015,Math.max(Math.abs(a),Math.abs(b))*tolerance);
    }
    private static int positive(Integer i) { return i!=null&&i>0?i:0; }
    private static long positive(Long i) { return i!=null&&i>0?i:0; }
    private static int integer(Object o,int fallback) { return o instanceof Integer?(Integer)o:fallback; }
    private static String string(Object o) { return o instanceof String?(String)o:""; }
    private static CameraDevice castDevice(Object o) { return o instanceof CameraDevice?(CameraDevice)o:null; }
    private static CameraCaptureSession castSession(Object o) { return o instanceof CameraCaptureSession?(CameraCaptureSession)o:null; }
    private static CameraCharacteristics castCharacteristics(Object o) { return o instanceof CameraCharacteristics?(CameraCharacteristics)o:null; }
    private static Handler castHandler(Object o) { return o instanceof Handler?(Handler)o:null; }
}
