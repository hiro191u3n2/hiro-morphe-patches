package com.hiro.ulike;

import android.graphics.Bitmap;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.os.Build;
import android.util.SizeF;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

/**
 * Optional shot metadata for the 1.9.32 still-image processing path.
 *
 * A record follows the exact callback and bitmap identities. Camera/result
 * metadata is accepted only when it belongs to the received Image timestamp;
 * there is deliberately no "most recent capture result" fallback. The helper
 * owns no camera, Image, Bitmap, callback, request, renderer or Activity.
 */
public final class ShotContext1932 {
    public static final int LENS_UNKNOWN = 0;
    public static final int LENS_FRONT = 1;
    public static final int LENS_BACK = 2;
    public static final int LENS_ULTRAWIDE = 3;
    public static final int LENS_TELEPHOTO = 4;

    private static final int MAX_SHOT_KEYS = 96;
    private static final int MAX_BEAUTY_KEYS = 16;
    private static final long SHOT_TTL_NANOS = 120000000000L;
    private static final Object LOCK = new Object();
    private static final ArrayList<ShotKey> SHOTS = new ArrayList<ShotKey>();
    private static final ArrayList<BeautyKey> BEAUTY = new ArrayList<BeautyKey>();
    private static long nextId = 1;

    public static final Snapshot UNKNOWN = new Snapshot(0, 0, 0, 0, 0,
            -1, LENS_UNKNOWN, "", "", false, -1, 0, 0);

    private ShotContext1932() { }

    /** Immutable values used by one invocation of the pixel pipeline. */
    public static final class Snapshot {
        public final long shotId;
        public final long sensorTimestampNanos;
        public final int iso;
        public final long exposureNanos;
        public final float focalLength;
        public final int lensFacing;
        public final int lensKind;
        public final String cameraId;
        public final String physicalId;
        public final boolean metadataReliable;
        /** -1 is unknown; 0 means that a renderer explicitly used no smoothing. */
        public final float beautyStrength;
        public final int sourceWidth;
        public final int sourceHeight;

        private Snapshot(long id, long timestamp, int sensitivity, long exposure,
                float focal, int facing, int kind, String camera, String physical,
                boolean reliable, float beauty, int width, int height) {
            shotId = id;
            sensorTimestampNanos = timestamp;
            iso = sensitivity;
            exposureNanos = exposure;
            focalLength = focal;
            lensFacing = facing;
            lensKind = kind;
            cameraId = camera;
            physicalId = physical;
            metadataReliable = reliable;
            beautyStrength = beauty;
            sourceWidth = width;
            sourceHeight = height;
        }
    }

    private static final class Shot {
        final long id;
        final long createdNanos;
        final float beauty;
        final ProcessingTiming1947.Trace timing;
        boolean nativeStarted;
        boolean delivered;
        boolean invalid;
        Snapshot snapshot;
        // Bounded weak observations; camera timestamps are keys, never elapsed clocks.
        final ArrayList<Arrival19103> arrivals19103 = new ArrayList<Arrival19103>();

        Shot(float smooth,long now,Object callback) {
            this(smooth,now,callback,0);
        }
        Shot(float smooth, long now, Object callback,int origin) {
            id = nextId++;
            createdNanos = now;
            beauty = smooth;
            timing = ProcessingTiming1947.beginCapture1991(callback,now,origin);
            ProcessingTiming1947.shutterDown1994(timing, ShutterTiming1994.consumeDown1994());
            ProcessingTiming1947.associate(callback, id);
            snapshot = new Snapshot(id, 0, 0, 0, 0, -1, LENS_UNKNOWN,
                    "", "", false, smooth, 0, 0);
        }
    }

    private static final class Arrival19103 {
        final WeakReference<Object> owner;
        final WeakReference<Object> input;
        final long timestamp, started, observed;
        final boolean result;
        long guardStarted19104,guardEnded19104,q0Entered19104;
        Arrival19103(Object o,Object value,long timestamp,long started,long observed,boolean result) {
            owner=new WeakReference<Object>(o);input=new WeakReference<Object>(value);
            this.timestamp=timestamp;this.started=started;this.observed=observed;this.result=result;
        }
    }

    /** Exact shot generation at the existing camera observer entry. The token
     * cannot be replaced by a later reuse of the same native callback object. */
    public static Object cameraOwner19103(Object owner) {
        if(owner==null)return null;
        try { synchronized(LOCK) {
            Object callback=field(owner,"x0"),terminal=unwrapCallback(callback);
            Shot shot=find(callback);if(shot==null)shot=find(terminal);
            if(shot==null||shot.invalid||shot.delivered||!shot.nativeStarted
                    ||!stillBound(callback,terminal,shot)||ProcessingTiming1947.finished1953(shot.timing))return null;
            return shot;
        }} catch(Throwable optional) { return null; }
    }

    /** The real acquireNextImage call, before any replay or pending-result wait.
     * called only for a newly acquired image, not a replay of a retained image. */
    public static void acquired19103(Object owner,Object token,Image image,long started,long observed) {
        if(image==null||token==null)return;
        try { rememberArrival19103(owner,token,image,image.getTimestamp(),started,observed,false); }
        catch(Throwable optional) { }
    }

    /** Observer of the existing ownership guard. Never changes its decision or Image ownership. */
    public static void cameraGuard19104(Object owner,Image image,long started,long ended) {
        try { rememberPhase19104(owner,image,started,ended,false); } catch(Throwable optional) { }
    }

    /** The stock Q0 entry before received(). This performs no pixel work or supplemental capture. */
    public static void q0Entry19104(Object owner,Image image,CaptureResult result) {
        final long entered=System.nanoTime();
        try { rememberPhase19104(owner,image,entered,entered,true); } catch(Throwable optional) { }
    }

    private static void rememberPhase19104(Object owner,Image image,long started,long ended,boolean q0) {
        if(owner==null||image==null||started<=0||ended<started||ended>System.nanoTime())return;
        synchronized(LOCK) {
            Object token=cameraOwner19103(owner);if(!(token instanceof Shot))return;Shot shot=(Shot)token;
            if(started<shot.createdNanos)return;
            for(Arrival19103 value:shot.arrivals19103) {
                if(value.result||value.owner.get()!=owner||value.input.get()!=image||started<value.observed)continue;
                if(q0){if(value.q0Entered19104==0)value.q0Entered19104=started;}
                else if(value.guardStarted19104==0){value.guardStarted19104=started;value.guardEnded19104=ended;}
                return;
            }
        }
    }

    /** Existing ordinary STILL result hook. The caller filters capture intent
     * and supplemental burst tickets before invoking this observer. */
    public static void resultObserved19103(Object owner,Object token,CaptureResult result,long observed) {
        if(result==null||token==null)return;
        try {
            rememberResult19103(owner,token,result,observed);
            if(Build.VERSION.SDK_INT>=28&&result instanceof TotalCaptureResult) {
                Map<String,CaptureResult> physical=((TotalCaptureResult)result).getPhysicalCameraResults();
                if(physical!=null)for(CaptureResult value:physical.values())rememberResult19103(owner,token,value,observed);
            }
        } catch(Throwable optional) { }
    }

    private static void rememberResult19103(Object owner,Object token,CaptureResult result,long observed) {
        if(result==null)return;
        Long timestamp=result.get(CaptureResult.SENSOR_TIMESTAMP);
        if(timestamp!=null)rememberArrival19103(owner,token,result,timestamp.longValue(),0,observed,true);
    }

    private static void rememberArrival19103(Object owner,Object token,Object input,long timestamp,
            long started,long observed,boolean result) {
        if(!(token instanceof Shot)||owner==null||input==null||timestamp<=0||observed<=0)return;
        synchronized(LOCK) {
            Shot shot=(Shot)token;
            if(cameraOwner19103(owner)!=shot||observed<shot.createdNanos||observed>System.nanoTime()
                    ||(!result&&(started<shot.createdNanos||started>observed)))return;
            // Keep the first real acquire/notification; callback replay is not a new arrival.
            for(Arrival19103 old:shot.arrivals19103)if(old.owner.get()==owner&&old.input.get()==input
                    &&old.result==result&&old.timestamp==timestamp)return;
            while(shot.arrivals19103.size()>=16)shot.arrivals19103.remove(0);
            shot.arrivals19103.add(new Arrival19103(owner,input,timestamp,started,observed,result));
        }
    }

    /** Invoked only after exact Image/result timestamp and callback validation.
     * Distinct equal-timestamp objects are ambiguous and remain unobserved. */
    private static void adoptArrival19103(Shot shot,Object owner,Image image,CaptureResult result,
            long timestamp,long received) {
        long acquiredStart=0,acquiredEnd=0,resultAt=0,guardStart=0,guardEnd=0,q0Entry=0;int acquisitions=0,results=0;
        for(Arrival19103 value:shot.arrivals19103) {
            Object input=value.input.get();
            if(value.owner.get()!=owner||input==null||value.timestamp!=timestamp||value.observed>received)continue;
            if(value.result) {
                if(input==result){results++;resultAt=value.observed;}
            } else if(image==null||input==image) {
                acquisitions++;acquiredStart=value.started;acquiredEnd=value.observed;
                guardStart=value.guardStarted19104;guardEnd=value.guardEnded19104;q0Entry=value.q0Entered19104;
            }
        }
        ProcessingTiming1947.cameraHandoff19103(shot.timing,
                acquisitions==1?acquiredStart:0,acquisitions==1?acquiredEnd:0,results==1?resultAt:0,received);
        ProcessingTiming1947.cameraPhases19104(shot.timing,acquisitions==1?guardStart:0,
                acquisitions==1?guardEnd:0,acquisitions==1?q0Entry:0,received);
    }

    private static final class ShotKey {
        final WeakReference<Object> key;
        final Shot shot;
        ShotKey(Object object, Shot value) {
            key = new WeakReference<Object>(object);
            shot = value;
        }
    }

    private static final class BeautyKey {
        final WeakReference<Object> key;
        float strength;
        BeautyKey(Object object, float value) {
            key = new WeakReference<Object>(object);
            strength = value;
        }
    }

    /** Observe the ACTUAL, post-TextureBeauty smoothing value for one renderer. */
    public static void observeBeauty(Object recorder, String parameter, float actual) {
        if (recorder == null || !"smooth".equals(parameter)) return;
        // This comparison also rejects NaN; an invalid value cannot mean off.
        float value = actual >= 0 && actual <= 1 ? actual : -1;
        synchronized (LOCK) {
            for (int i = BEAUTY.size() - 1; i >= 0; i--) {
                BeautyKey entry = BEAUTY.get(i);
                Object key = entry.key.get();
                if (key == null) BEAUTY.remove(i);
                else if (key == recorder) {
                    entry.strength = value;
                    return;
                }
            }
            while (BEAUTY.size() >= MAX_BEAUTY_KEYS) BEAUTY.remove(0);
            BEAUTY.add(new BeautyKey(recorder, value));
        }
    }

    public static void observeBeautyArray(Object recorder, String[] parameters,
            float[] actual) {
        if (recorder == null || parameters == null || actual == null
                || parameters.length != actual.length) return;
        for (int i = 0; i < parameters.length; i++) {
            observeBeauty(recorder, parameters[i], actual[i]);
        }
    }

    /** Only successful SDK updates describe the renderer's effective state. */
    public static void observeBeautyResult(Object recorder, String parameter,
            float actual, int resultCode) {
        if (resultCode == 0) observeBeauty(recorder, parameter, actual);
    }

    public static void observeBeautyArrayResult(Object recorder, String[] parameters,
            float[] actual, int resultCode) {
        if (resultCode == 0) observeBeautyArray(recorder, parameters, actual);
    }

    /** Composer replacement/removal can make the last numeric value obsolete. */
    public static void invalidateBeauty(Object recorder) {
        if (recorder == null) return;
        synchronized (LOCK) {
            for (int i = BEAUTY.size() - 1; i >= 0; i--) {
                Object key = BEAUTY.get(i).key.get();
                if (key == null || key == recorder) BEAUTY.remove(i);
            }
        }
    }

    /** Called at a canonical VERecorder capture entry with its bitmap callback. */
    public static void beginRecorder(Object recorder, Object bitmapCallback) {
        final long requestedNanos1991=System.nanoTime();
        if (recorder == null || bitmapCallback == null) return;
        synchronized (LOCK) {
            long now = System.nanoTime();
            prune(now);
            Shot shot = new Shot(beautyFor(recorder), requestedNanos1991, bitmapCallback,1);
            bind(bitmapCallback, shot);
        }
    }

    /** Called with the exact native still callback from VECameraCapture. */
    public static void begin(Object camera, Object nativeCallback) {
        final long requestedNanos1991=System.nanoTime();
        if (nativeCallback == null) return;
        Object callback = unwrapCallback(nativeCallback);
        Object recorder = recorderFromCallback(nativeCallback);
        synchronized (LOCK) {
            long now = System.nanoTime();
            prune(now);
            Shot shot = find(callback);
            if (shot == null || shot.invalid || shot.nativeStarted || shot.delivered) {
                shot = new Shot(beautyFor(recorder), requestedNanos1991, callback,2);
            }
            shot.nativeStarted = true;
            ProcessingTiming1947.markAt1991(shot.timing,ProcessingTiming1947.NATIVE_REQUEST1991,requestedNanos1991);
            bind(nativeCallback, shot);
            bind(callback, shot);
        }
    }

    /**
     * Existing Q0(Image, TotalCaptureResult) calls CaptureYuv.received before
     * converting/delivering the image. Read x0 here while Q0 uses that same
     * callback; never associate the result with an unrelated camera-global slot.
     */
    public static void received(Object owner, Image image, CaptureResult result) {
        final long receivedNanos1991=System.nanoTime();
        if (owner == null || image == null) return;
        Shot observedShot1999 = null;
        try {
            Object callback = field(owner, "x0");
            if (callback == null) return;
            Object terminal = unwrapCallback(callback);
            Shot shot;
            synchronized (LOCK) {
                prune(System.nanoTime());
                shot = find(callback);
                if (shot == null) shot = find(terminal);
                if (shot == null || shot.invalid || shot.delivered) return;
                observedShot1999 = shot;
            }
            if (result == null) { ProcessingTiming1947.captureMetadataReason1999(shot.timing,2,false); return; }

            CameraDevice device = asDevice(field(owner, "j"));
            Object route = device == null ? null : opticalRoute(device);
            String cameraId = device == null ? "" : safeString(device.getId());
            String physicalId = safeString(field(route, "physicalId"));
            CameraCharacteristics characteristics = asCharacteristics(field(route, "lens"));
            if (characteristics == null && physicalId.length() == 0) {
                characteristics = asCharacteristics(field(owner, "a"));
            }

            CaptureResult matched = selectResult(result, physicalId);
            if (matched == null) { ProcessingTiming1947.captureMetadataReason1999(shot.timing,3,false); return; }
            Long timestamp = matched.get(CaptureResult.SENSOR_TIMESTAMP);
            long imageTimestamp = image.getTimestamp();
            if (timestamp == null || imageTimestamp <= 0
                    || timestamp.longValue() != imageTimestamp) { ProcessingTiming1947.captureMetadataReason1999(shot.timing,4,false); return; }

            int iso = positiveInt(matched.get(CaptureResult.SENSOR_SENSITIVITY));
            long exposure = positiveLong(matched.get(CaptureResult.SENSOR_EXPOSURE_TIME));
            float focal = positiveFloat(matched.get(CaptureResult.LENS_FOCAL_LENGTH));
            int facing = facing(characteristics);
            int kind = lensKind(facing, focal, characteristics, route, physicalId);
            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0) return;
            Snapshot snapshot = new Snapshot(shot.id, imageTimestamp, iso, exposure,
                    focal, facing, kind, cameraId, physicalId, true, shot.beauty,
                    width, height);
            synchronized (LOCK) {
                // A second frame must not overwrite a record already tied to a
                // different input frame, even if an SDK reuses a callback object.
                if (shot.invalid || shot.delivered) return;
                if (field(owner, "x0") != callback || !stillBound(callback, terminal, shot)) {
                    shot.invalid=true;ProcessingTiming1947.captureMetadataReason1999(shot.timing,5,true); return;
                }
                long previous = shot.snapshot.sensorTimestampNanos;
                if (previous != 0 && previous != imageTimestamp) {
                    shot.invalid = true;
                    ProcessingTiming1947.captureMetadataReason1999(shot.timing,6,true);
                    return;
                }
                shot.snapshot = snapshot;
                ProcessingTiming1947.captureMetadataReason1999(shot.timing,1,false);
                ProcessingTiming1947.captureMetadata1995(shot.timing,iso,exposure,kind,width,height);
                ProcessingTiming1947.markAt1991(shot.timing,ProcessingTiming1947.IMAGE_RECEIVED1991,receivedNanos1991);
                adoptArrival19103(shot,owner,image,matched,imageTimestamp,receivedNanos1991);
                bind(callback, shot);
                bind(terminal, shot);
                bind(image, shot);
            }
        } catch (RuntimeException ignored) {
            if(observedShot1999!=null)ProcessingTiming1947.captureMetadataReason1999(observedShot1999.timing,7,false);
            // Unavailable metadata must never prevent still capture or saving.
        } catch (LinkageError ignored) {
            // Old OEM camera implementations can omit otherwise public keys.
        }
    }

    /** Metadata for one real, copied burst reference; never a latest-result guess. */
    public static boolean receivedValues1933(Object owner, Object expectedCallback, long imageTimestamp, int width, int height, CaptureResult result) {
        final long receivedNanos1991=System.nanoTime();
        if (owner == null || expectedCallback == null || result == null || imageTimestamp <= 0 || width <= 0 || height <= 0) return false;
        try {
            Object callback = field(owner, "x0");
            if (callback != expectedCallback) return false;
            Object terminal = unwrapCallback(callback);
            Shot shot;
            synchronized (LOCK) {
                prune(System.nanoTime());
                shot = find(callback);
                if (shot == null) shot = find(terminal);
                if (shot == null || shot.invalid || shot.delivered) return false;
            }

            CameraDevice device = asDevice(field(owner, "j"));
            Object route = device == null ? null : opticalRoute(device);
            String cameraId = device == null ? "" : safeString(device.getId());
            String physicalId = safeString(field(route, "physicalId"));
            CameraCharacteristics characteristics = asCharacteristics(field(route, "lens"));
            if (characteristics == null && physicalId.length() == 0) {
                characteristics = asCharacteristics(field(owner, "a"));
            }

            CaptureResult matched = selectResult(result, physicalId);
            if (matched == null) return false;
            Long timestamp = matched.get(CaptureResult.SENSOR_TIMESTAMP);
            if (timestamp == null || imageTimestamp <= 0
                    || timestamp.longValue() != imageTimestamp) return false;

            int iso = positiveInt(matched.get(CaptureResult.SENSOR_SENSITIVITY));
            long exposure = positiveLong(matched.get(CaptureResult.SENSOR_EXPOSURE_TIME));
            float focal = positiveFloat(matched.get(CaptureResult.LENS_FOCAL_LENGTH));
            int facing = facing(characteristics);
            int kind = lensKind(facing, focal, characteristics, route, physicalId);
            if (width <= 0 || height <= 0) return false;
            Snapshot snapshot = new Snapshot(shot.id, imageTimestamp, iso, exposure,
                    focal, facing, kind, cameraId, physicalId, true, shot.beauty,
                    width, height);
            synchronized (LOCK) {
                // A second frame must not overwrite a record already tied to a
                // different input frame, even if an SDK reuses a callback object.
                if (shot.invalid || shot.delivered) return false;
                if (field(owner, "x0") != expectedCallback || !stillBound(callback, terminal, shot)) {
                    shot.invalid=true;ProcessingTiming1947.captureMetadataReason1999(shot.timing,5,true); return false;
                }
                long previous = shot.snapshot.sensorTimestampNanos;
                if (previous != 0 && previous != imageTimestamp) {
                    shot.invalid = true;
                    ProcessingTiming1947.captureMetadataReason1999(shot.timing,6,true);
                    return false;
                }
                shot.snapshot = snapshot;
                ProcessingTiming1947.captureMetadataReason1999(shot.timing,1,false);
                ProcessingTiming1947.captureMetadata1995(shot.timing,iso,exposure,kind,width,height);
                ProcessingTiming1947.markAt1991(shot.timing,ProcessingTiming1947.IMAGE_RECEIVED1991,receivedNanos1991);
                adoptArrival19103(shot,owner,null,matched,imageTimestamp,receivedNanos1991);
                bind(callback, shot);
                bind(terminal, shot);
                return true;
            }
        } catch (RuntimeException ignored) {
            // Unavailable metadata must never prevent still capture or saving.
        } catch (LinkageError ignored) {
            // Old OEM camera implementations can omit otherwise public keys.
        }
        return false;
    }

    /** Called at the existing render-success callback/bitmap binding point. */
    public static void bindDelivery(Object callback, Object delivery, Bitmap bitmap) {
        final long deliveredNanos1991=System.nanoTime();
        if (callback == null || bitmap == null) return;
        try {
            if (bitmap.isRecycled() || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) return;
            synchronized (LOCK) {
                prune(System.nanoTime());
                Shot shot = find(callback);
                if (shot == null || shot.delivered || ProcessingTiming1947.finished1953(shot.timing)) return;
                shot.delivered = true;
                ProcessingTiming1947.bitmapInput1999(shot.timing,0,bitmap.getWidth(),bitmap.getHeight());
                ProcessingTiming1947.markAt1991(shot.timing,ProcessingTiming1947.BITMAP_DELIVERED1991,deliveredNanos1991);
                bind(bitmap, shot);
                if (delivery != null) bind(delivery, shot);
                // A known still delivery without a temporal kernel has no
                // fusion work. skip() preserves any recorded/active interval.
                ProcessingTiming1947.skip(shot.timing, ProcessingTiming1947.FUSION);
            }
        } catch (RuntimeException ignored) { }
    }

    /** Explicit propagation for a copied/rotated/resampled version of one bitmap. */
    public static void copy(Bitmap source, Bitmap destination) {
        transfer(source, destination);
    }

    public static void transfer(Object source, Object destination) {
        if (source == null || destination == null || source == destination) return;
        ProcessingTiming1947.transfer(source, destination);
        synchronized (LOCK) {
            prune(System.nanoTime());
            Shot shot = find(source);
            if (shot != null && !shot.invalid) {
                bind(destination, shot);
            } else {
                // A reused destination must not retain a different photo's
                // metadata when this source has no verifiable shot context.
                for (int i = SHOTS.size() - 1; i >= 0; i--) {
                    if (SHOTS.get(i).key.get() == destination) SHOTS.remove(i);
                }
            }
        }
    }

    /** Recover only this bitmap's exact owner if the larger timing-key index
     * was independently evicted. Metadata validity is separate from ownership;
     * the save caller still rejects completed traces before using a result. */
    public static ProcessingTiming1947.Trace timingForBitmap1999(Bitmap bitmap) {
        if(bitmap==null)return null;
        synchronized(LOCK){prune(System.nanoTime());Shot shot=find(bitmap);return shot==null?null:shot.timing;}
    }

    public static Snapshot forBitmap(Bitmap bitmap) {
        if (bitmap == null) return UNKNOWN;
        synchronized (LOCK) {
            prune(System.nanoTime());
            Shot shot = find(bitmap);
            return shot == null || shot.invalid ? UNKNOWN : shot.snapshot;
        }
    }

    /** Optional explicit cleanup once all processing of a source bitmap is done. */
    public static void forget(Object key) {
        if (key == null) return;
        synchronized (LOCK) {
            for (int i = SHOTS.size() - 1; i >= 0; i--) {
                Object existing = SHOTS.get(i).key.get();
                if (existing == null || existing == key) SHOTS.remove(i);
            }
        }
    }

    public static void failed(Object callback, int resultCode) {
        if (callback == null || resultCode == 0) return;
        synchronized (LOCK) {
            Shot shot = find(callback);
            if (shot != null && !shot.delivered) {
                shot.invalid = true;
                ProcessingTiming1947.finish(shot.timing, false);
            }
        }
    }

    /** Metadata rejection does not mean the real capture has finished. The
     * original timing trace stays bound through delivery and successful save. */
    public static boolean idle1953() {
        synchronized(LOCK) {
            prune(System.nanoTime());
            for(ShotKey key:SHOTS)if(!ProcessingTiming1947.finished1953(key.shot.timing))return false;
            return true;
        }
    }

    private static float beautyFor(Object recorder) {
        if (recorder == null) return -1;
        for (int i = BEAUTY.size() - 1; i >= 0; i--) {
            BeautyKey entry = BEAUTY.get(i);
            if (entry.key.get() == recorder) return entry.strength;
        }
        return -1;
    }

    private static Shot find(Object key) {
        if (key == null) return null;
        for (int i = SHOTS.size() - 1; i >= 0; i--) {
            ShotKey entry = SHOTS.get(i);
            if (entry.key.get() == key) return entry.shot;
        }
        return null;
    }

    /** A wrapper may still refer to the old shot after its terminal callback is
     * reused. Never let that old wrapper overwrite the new callback binding. */
    private static boolean stillBound(Object callback, Object terminal, Shot shot) {
        Shot direct = find(callback), target = find(terminal);
        return (direct == shot || target == shot)
                && (direct == null || direct == shot)
                && (target == null || target == shot);
    }

    private static void bind(Object key, Shot shot) {
        if (key == null) return;
        ProcessingTiming1947.bind(key, shot.timing);
        for (int i = SHOTS.size() - 1; i >= 0; i--) {
            Object existing = SHOTS.get(i).key.get();
            if (existing == key || existing == null) SHOTS.remove(i);
        }
        while (SHOTS.size() >= MAX_SHOT_KEYS) {
            int victim = 0;
            // Keep metadata of in-flight captures when completed bitmap records
            // already provide reclaimable entries. The hard cap still applies.
            for (int i = 0; i < SHOTS.size(); i++) {
                Shot value = SHOTS.get(i).shot;
                if (value.invalid || value.delivered) { victim = i; break; }
            }
            SHOTS.remove(victim);
        }
        SHOTS.add(new ShotKey(key, shot));
    }

    private static void prune(long now) {
        for (int i = SHOTS.size() - 1; i >= 0; i--) {
            ShotKey entry = SHOTS.get(i);
            if (entry.key.get() == null || now - entry.shot.createdNanos > SHOT_TTL_NANOS) {
                SHOTS.remove(i);
            }
        }
    }

    private static Object unwrapCallback(Object callback) {
        Object value = callback;
        for (int i = 0; i < 5 && value != null; i++) {
            String name = value.getClass().getName();
            Object next;
            if ("i.s.a.w.q$g$a".equals(name)) {
                next = field(field(value, "a"), "c");
            } else if ("com.ss.android.vesdk.TECameraVideoRecorder$60".equals(name)
                    || "com.ss.android.vesdk.VERecorder$13".equals(name)) {
                next = field(value, "a");
            } else {
                return value;
            }
            if (next == null || next == value) return value;
            value = next;
        }
        return value;
    }

    private static Object recorderFromCallback(Object callback) {
        Object value = callback;
        for (int i = 0; i < 5 && value != null; i++) {
            String name = value.getClass().getName();
            if ("com.ss.android.vesdk.VERecorder$13".equals(name)) {
                Object recorder = field(value, "d");
                return recorder != null && "com.ss.android.vesdk.VERecorder".equals(
                        recorder.getClass().getName()) ? recorder : null;
            }
            if ("i.s.a.w.q$g$a".equals(name)) value = field(field(value, "a"), "c");
            else if ("com.ss.android.vesdk.TECameraVideoRecorder$60".equals(name)) {
                value = field(value, "a");
            } else return null;
        }
        return null;
    }

    private static CaptureResult selectResult(CaptureResult result, String physicalId) {
        if (physicalId.length() == 0) return result;
        if (Build.VERSION.SDK_INT < 28 || !(result instanceof TotalCaptureResult)) return null;
        Map<String, CaptureResult> physical = ((TotalCaptureResult) result).getPhysicalCameraResults();
        return physical == null ? null : physical.get(physicalId);
    }

    private static Object opticalRoute(CameraDevice device) {
        try {
            Class<?> type = Class.forName("com.hiro.ulike.OpticalZoom");
            Method method = type.getDeclaredMethod("route", CameraDevice.class);
            method.setAccessible(true);
            return method.invoke(null, device);
        } catch (ReflectiveOperationException ignored) {
            return null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Object field(Object owner, String name) {
        if (owner == null) return null;
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                // The relevant camera fields are inherited from the base mode.
            } catch (ReflectiveOperationException ignored) {
                return null;
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static CameraDevice asDevice(Object value) {
        return value instanceof CameraDevice ? (CameraDevice) value : null;
    }

    private static CameraCharacteristics asCharacteristics(Object value) {
        return value instanceof CameraCharacteristics ? (CameraCharacteristics) value : null;
    }

    private static String safeString(Object value) {
        return value instanceof String ? (String) value : "";
    }

    private static int positiveInt(Integer value) {
        return value != null && value.intValue() > 0 ? value.intValue() : 0;
    }

    private static long positiveLong(Long value) {
        return value != null && value.longValue() > 0 ? value.longValue() : 0;
    }

    private static float positiveFloat(Float value) {
        if (value == null) return 0;
        float result = value.floatValue();
        return result > 0 && result < 1000 ? result : 0;
    }

    private static int facing(CameraCharacteristics characteristics) {
        if (characteristics == null) return -1;
        Integer value = characteristics.get(CameraCharacteristics.LENS_FACING);
        return value == null ? -1 : value.intValue();
    }

    private static int lensKind(int facing, float focal,
            CameraCharacteristics characteristics, Object route, String physicalId) {
        if (facing == CameraCharacteristics.LENS_FACING_FRONT) return LENS_FRONT;
        if (facing != CameraCharacteristics.LENS_FACING_BACK) return LENS_UNKNOWN;
        // A digital zoom label does not change the actual sensor or lens. Prefer
        // optical field of view derived from this lens's public characteristics.
        if (characteristics != null && focal > 0) {
            SizeF sensor = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            if (sensor != null) {
                float width = Math.max(sensor.getWidth(), sensor.getHeight());
                float equivalent = width > 0 ? focal * 36f / width : 0;
                if (equivalent > 0 && equivalent < 20f) return LENS_ULTRAWIDE;
                if (equivalent > 45f && equivalent < 2000f) return LENS_TELEPHOTO;
                if (equivalent >= 20f && equivalent <= 45f) return LENS_BACK;
            }
        }
        // Without sensor dimensions, only an explicitly routed physical lens
        // supports using the route's lens label. A logical crop stays generic.
        if (physicalId.length() > 0) {
            Object nominal = field(route, "nominal");
            if (nominal instanceof Integer) {
                int value = ((Integer) nominal).intValue();
                if (value > 1) return LENS_TELEPHOTO;
                if (value == 0) return LENS_ULTRAWIDE;
            }
        }
        return LENS_BACK;
    }
}
