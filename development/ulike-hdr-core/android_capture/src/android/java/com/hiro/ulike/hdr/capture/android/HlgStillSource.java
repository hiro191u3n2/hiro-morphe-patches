package com.hiro.ulike.hdr.capture.android;

import android.graphics.ColorSpace;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.DataSpace;
import android.hardware.HardwareBuffer;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.ColorSpaceProfiles;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;
import com.hiro.ulike.hdr.capture.CapturePolicy;
import com.hiro.ulike.hdr.capture.StillJoin;
import com.hiro.ulike.hdr.input.CaptureMatch;
import com.hiro.ulike.hdr.input.HdrFrame;
import com.hiro.ulike.hdr.input.P010FrameReader;
import com.hiro.ulike.hdr.input.android.AndroidP010FrameReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Standalone API 34+ one-output still source. Not an ULike hook or an integrated beauty engine.
 * Caller must exclusively own the borrowed CameraDevice and serialize all calls on handler.
 * Creating this session replaces any previous device session. Device ownership stays with caller.
 */
public final class HlgStillSource implements AutoCloseable {
    public interface Listener {
        void onReady(HlgStillSource source);
        void onFrame(CapturedStill frame);
        void onFailure(String stage, Exception error);
    }

    /** Immutable geometry binds later face analysis to these exact owned pixels. */
    public static final class CapturedStill {
        public final HdrFrame pixels;
        public final String sessionEpoch;
        public final long requestSequence;
        public final int sensorOrientationDegrees, lensFacing;
        public final int intendedDisplayQuarterTurnsClockwise;
        public final int appliedPixelQuarterTurnsClockwise = 0;
        public final boolean appliedPixelMirror = false;
        public final int sensorCropLeft, sensorCropTop, sensorCropRight, sensorCropBottom;
        public final int imageCropLeft = 0, imageCropTop = 0;
        public final int imageCropRight, imageCropBottom;
        public final Float actualZoomRatio;
        CapturedStill(HdrFrame pixels, String epoch, long shot, int orientation, int facing,
                int displayRotation, Rect sensorCrop, Float zoomRatio) {
            this.pixels = pixels; sessionEpoch = epoch; requestSequence = shot;
            sensorOrientationDegrees = orientation; lensFacing = facing;
            intendedDisplayQuarterTurnsClockwise = displayRotation;
            sensorCropLeft = sensorCrop.left; sensorCropTop = sensorCrop.top;
            sensorCropRight = sensorCrop.right; sensorCropBottom = sensorCrop.bottom;
            imageCropRight = pixels.width; imageCropBottom = pixels.height;
            actualZoomRatio = zoomRatio;
        }
    }

    private final CameraDevice device;
    private final Handler handler;
    private final Listener listener;
    private final ImageReader reader;
    private final String cameraId, physicalId;
    private final String epoch = UUID.randomUUID().toString();
    private final int width, height, orientation, facing;
    private final long budget;
    private final boolean hasPixelMode;
    private final StillJoin<Image, TotalCaptureResult> join = new StillJoin<Image, TotalCaptureResult>();
    private CameraCaptureSession session;
    private CaptureMatch.Context submitted;
    private long nextShot;
    private int displayRotation;
    private boolean closed;
    private Runnable timeout;

    private HlgStillSource(CameraDevice device, Handler handler, Listener listener,
            ImageReader reader, String physicalId, int width, int height, long budget,
            int orientation, int facing, boolean hasPixelMode) {
        this.device = device; this.handler = handler; this.listener = listener;
        this.reader = reader; cameraId = device.getId(); this.physicalId = physicalId;
        this.width = width; this.height = height; this.budget = budget;
        this.orientation = orientation; this.facing = facing; this.hasPixelMode = hasPixelMode;
    }

    /** Only normal P010 sizes; no scaling or default/maximum-mode fallback. */
    public static HlgStillSource configure(CameraManager manager, CameraDevice ownedDevice,
            String physicalId, int width, int height, long maxOwnedSampleBytes,
            Handler serializedHandler, Listener listener) throws CameraAccessException {
        if (Build.VERSION.SDK_INT < 34) throw new IllegalStateException("API 34+ explicit color session required");
        if (manager == null || ownedDevice == null || serializedHandler == null || listener == null)
            throw new NullPointerException("manager, exclusively owned device, handler and listener required");
        if (Looper.myLooper() != serializedHandler.getLooper())
            throw new IllegalStateException("configure on serialized camera handler");
        CameraCharacteristics logical = manager.getCameraCharacteristics(ownedDevice.getId());
        CameraCharacteristics route = logical;
        if (physicalId != null) {
            if (physicalId.isEmpty() || !logical.getPhysicalCameraIds().contains(physicalId))
                throw new IllegalArgumentException("physical ID is not a member of this logical camera");
            route = manager.getCameraCharacteristics(physicalId);
        }
        requireProfiles(logical);
        requireProfiles(route);
        StreamConfigurationMap map = route.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) throw new IllegalArgumentException("normal stream map missing");
        List<int[]> sizes = new ArrayList<int[]>();
        addSizes(sizes, map.getOutputSizes(ImageFormat.YCBCR_P010));
        addSizes(sizes, map.getHighResolutionOutputSizes(ImageFormat.YCBCR_P010));
        CapturePolicy.dimensions(width, height, maxOwnedSampleBytes, sizes.toArray(new int[0][]));
        Integer orientation = route.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Integer facing = route.get(CameraCharacteristics.LENS_FACING);
        if (orientation == null || (orientation % 90) != 0 || orientation < 0 || orientation > 270
                || facing == null) throw new IllegalArgumentException("camera geometry metadata missing");
        int[] rotateModes = logical.get(CameraCharacteristics.SCALER_AVAILABLE_ROTATE_AND_CROP_MODES);
        if (!contains(rotateModes, CaptureRequest.SCALER_ROTATE_AND_CROP_NONE)
                || !logical.getAvailableCaptureRequestKeys().contains(CaptureRequest.SCALER_ROTATE_AND_CROP)
                || !logical.getAvailableCaptureResultKeys().contains(CaptureResult.SCALER_ROTATE_AND_CROP))
            throw new IllegalArgumentException("explicit unrotated pixel output cannot be verified");
        boolean pixelMode = logical.getAvailableCaptureRequestKeys().contains(CaptureRequest.SENSOR_PIXEL_MODE);
        ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.YCBCR_P010, 2,
                HardwareBuffer.USAGE_CPU_READ_OFTEN);
        HlgStillSource source = new HlgStillSource(ownedDevice, serializedHandler, listener,
                reader, physicalId, width, height, maxOwnedSampleBytes, orientation, facing, pixelMode);
        try {
            OutputConfiguration output = new OutputConfiguration(reader.getSurface());
            output.setDynamicRangeProfile(DynamicRangeProfiles.HLG10);
            output.setTimestampBase(OutputConfiguration.TIMESTAMP_BASE_SENSOR);
            output.setReadoutTimestampEnabled(false);
            output.setMirrorMode(OutputConfiguration.MIRROR_MODE_NONE);
            if (physicalId != null) output.setPhysicalCameraId(physicalId);
            Executor executor = command -> {
                if (!serializedHandler.post(command)) throw new IllegalStateException("camera handler stopped");
            };
            SessionConfiguration config = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                    Collections.singletonList(output), executor, source.stateCallback);
            config.setColorSpace(ColorSpace.Named.BT2020_HLG);
            // UnsupportedOperationException is a rejected/unknown query, never a support claim.
            if (!ownedDevice.isSessionConfigurationSupported(config))
                throw new IllegalArgumentException("exact single-output HDR session rejected by device");
            reader.setOnImageAvailableListener(source::imageAvailable, serializedHandler);
            ownedDevice.createCaptureSession(config);
            return source;
        } catch (CameraAccessException | RuntimeException error) {
            source.closed = true;
            try { reader.close(); } catch (RuntimeException closing) { error.addSuppressed(closing); }
            throw error;
        }
    }

    private static void requireProfiles(CameraCharacteristics c) {
        if (!contains(c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT))
            throw new IllegalArgumentException("10-bit camera capability missing");
        DynamicRangeProfiles ranges = c.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES);
        if (ranges == null) throw new IllegalArgumentException("dynamic range profiles missing");
        CapturePolicy.range(DynamicRangeProfiles.HLG10, ranges.getSupportedProfiles(),
                ranges.getSupportedProfiles().contains(DynamicRangeProfiles.HLG10)
                    ? ranges.getProfileCaptureRequestConstraints(DynamicRangeProfiles.HLG10) : null);
        ColorSpaceProfiles colors = c.get(CameraCharacteristics.REQUEST_AVAILABLE_COLOR_SPACE_PROFILES);
        if (colors == null || !colors.getSupportedColorSpacesForDynamicRange(
                ImageFormat.YCBCR_P010, DynamicRangeProfiles.HLG10).contains(ColorSpace.Named.BT2020_HLG))
            throw new IllegalArgumentException("P010/HLG10/BT2020_HLG tuple not advertised");
    }
    private static void addSizes(List<int[]> out, Size[] sizes) {
        if (sizes != null) for (Size s : sizes) out.add(new int[] {s.getWidth(), s.getHeight()});
    }
    private static boolean contains(int[] values, int wanted) {
        if (values != null) for (int value : values) if (value == wanted) return true;
        return false;
    }

    /** No preview/3A convergence controller is provided here. Template defaults are explicit scope. */
    public void capture(int intendedDisplayQuarterTurnsClockwise, long timeoutMillis)
            throws CameraAccessException {
        thread(); CapturePolicy.rotation(intendedDisplayQuarterTurnsClockwise);
        if (closed || session == null || submitted != null || timeoutMillis < 100 || timeoutMillis > 30000)
            throw new IllegalStateException("ready idle source and bounded timeout required");
        Image old;
        while ((old = reader.acquireNextImage()) != null) old.close();
        CaptureRequest.Builder builder = physicalId == null
            ? device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            : device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE, Collections.singleton(physicalId));
        builder.addTarget(reader.getSurface());
        builder.set(CaptureRequest.SCALER_ROTATE_AND_CROP, CaptureRequest.SCALER_ROTATE_AND_CROP_NONE);
        if (hasPixelMode) builder.set(CaptureRequest.SENSOR_PIXEL_MODE, CaptureRequest.SENSOR_PIXEL_MODE_DEFAULT);
        CaptureRequest request = builder.build();
        displayRotation = intendedDisplayQuarterTurnsClockwise;
        CaptureMatch.Context context = new CaptureMatch.Context(this, session, reader, request,
                captureCallback, nextShot++, cameraId, physicalId);
        submitted = context; join.begin(request);
        timeout = () -> { if (submitted == context) fail("timeout", new IllegalStateException("still deadline exceeded")); };
        if (!handler.postDelayed(timeout, timeoutMillis)) {
            fail("handler", new IllegalStateException("camera handler stopped")); return;
        }
        try { session.capture(request, captureCallback, handler); }
        catch (CameraAccessException | RuntimeException error) { fail("submit", error); throw error; }
    }

    private final CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
        @Override public void onConfigured(CameraCaptureSession configured) {
            thread();
            if (closed) { configured.close(); return; }
            if (configured.getDevice() != device) { configured.close(); fatal("configure", new IllegalStateException("wrong device")); return; }
            session = configured; listener.onReady(HlgStillSource.this);
        }
        @Override public void onConfigureFailed(CameraCaptureSession failed) {
            thread(); failed.close(); fatal("configure", new IllegalStateException("HDR session configuration failed"));
        }
        @Override public void onClosed(CameraCaptureSession ended) {
            thread(); if (ended == session && !closed) fatal("session", new IllegalStateException("session closed"));
        }
    };

    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override public void onCaptureCompleted(CameraCaptureSession actual, CaptureRequest request,
                TotalCaptureResult result) {
            thread();
            if (submitted == null || actual != session || request != submitted.request) return;
            CapturedStill captured;
            try {
                CaptureResult metadata = selectedResult(result);
                Long time = metadata.get(CaptureResult.SENSOR_TIMESTAMP);
                if (time == null || result.getRequest() != request)
                    throw new IllegalStateException("same-request sensor timestamp missing");
                captured = finish(join.result(request, result, time));
            } catch (Exception error) { fail("result", error); return; }
            if (captured != null) listener.onFrame(captured);
        }
        @Override public void onCaptureFailed(CameraCaptureSession actual, CaptureRequest request, CaptureFailure failure) {
            thread(); if (submitted != null && actual == session && request == submitted.request)
                fail("capture", new IllegalStateException("capture failure reason=" + failure.getReason()));
        }
        @Override public void onCaptureBufferLost(CameraCaptureSession actual, CaptureRequest request,
                Surface surface, long frameNumber) {
            thread(); if (submitted != null && actual == session && request == submitted.request && surface == reader.getSurface())
                fail("buffer", new IllegalStateException("P010 buffer lost"));
        }
    };

    private void imageAvailable(ImageReader actual) {
        thread();
        if (closed || actual != reader) return;
        CapturedStill captured = null;
        try {
            Image image;
            while ((image = reader.acquireNextImage()) != null) {
                long timestamp;
                try { timestamp = image.getTimestamp(); }
                catch (RuntimeException error) { image.close(); throw error; }
                captured = finish(join.image(image, timestamp));
                if (captured != null) break;
            }
        } catch (Exception error) { fail("image", error); return; }
        if (captured != null) listener.onFrame(captured);
    }

    private CaptureResult selectedResult(TotalCaptureResult total) {
        if (physicalId == null) return total;
        Map<String, CaptureResult> physical = total.getPhysicalCameraResults();
        CaptureResult result = physical == null ? null : physical.get(physicalId);
        if (result == null) throw new IllegalStateException("selected physical result missing");
        return result;
    }

    private CapturedStill finish(StillJoin.Pair<Image, TotalCaptureResult> pair) throws Exception {
        if (pair == null) return null;
        CapturedStill captured;
        try (StillJoin.Pair<Image, TotalCaptureResult> owned = pair) {
            CaptureMatch.Context context = submitted;
            if (context == null || pair.request != context.request) throw new IllegalStateException("stale pair");
            CaptureResult metadata = selectedResult(pair.result);
            Integer rotate = pair.result.get(CaptureResult.SCALER_ROTATE_AND_CROP);
            if (rotate == null || rotate != CaptureResult.SCALER_ROTATE_AND_CROP_NONE)
                throw new IllegalStateException("actual unrotated output was not reported");
            Integer pixel = pair.result.get(CaptureResult.SENSOR_PIXEL_MODE);
            if ((hasPixelMode && pixel == null) || (pixel != null && pixel != CaptureResult.SENSOR_PIXEL_MODE_DEFAULT))
                throw new IllegalStateException("unexpected or unverified sensor pixel mode");
            Rect crop = metadata.get(CaptureResult.SCALER_CROP_REGION);
            if (crop == null || crop.width() <= 0 || crop.height() <= 0)
                throw new IllegalStateException("actual sensor crop missing");
            int data = owned.image().getDataSpace();
            HdrFrame.Encoding encoding;
            if (data == (DataSpace.STANDARD_BT2020 | DataSpace.TRANSFER_HLG | DataSpace.RANGE_FULL))
                encoding = HdrFrame.Encoding.BT2020_NCL_HLG_FULL;
            else if (data == (DataSpace.STANDARD_BT2020 | DataSpace.TRANSFER_HLG | DataSpace.RANGE_LIMITED))
                encoding = HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED;
            else throw new IllegalStateException("actual P010 HLG color/range is unknown");
            CaptureMatch.Source source = new CaptureMatch.Source(this, session, reader, cameraId,
                    physicalId, CaptureMatch.TimestampConvention.SENSOR_START_OF_EXPOSURE);
            HdrFrame frame = AndroidP010FrameReader.copyBorrowed(owned.image(),
                    new P010FrameReader.Request(width, height, encoding, budget), context, source,
                    pair.result, context, () -> submitted);
            captured = new CapturedStill(frame, epoch, context.shot, orientation, facing,
                    displayRotation, crop, metadata.get(CaptureResult.CONTROL_ZOOM_RATIO));
        }
        clearShot();
        return captured;
    }

    private void clearShot() {
        if (timeout != null) handler.removeCallbacks(timeout);
        timeout = null; submitted = null;
    }
    private void fail(String stage, Exception error) {
        boolean hadShot = submitted != null;
        clearShot();
        try { join.cancel(); } catch (Exception closing) { error.addSuppressed(closing); }
        if (hadShot) listener.onFailure(stage, error);
    }
    private void fatal(String stage, Exception error) {
        if (closed) return;
        shutdown(error); listener.onFailure(stage, error);
    }
    private void thread() {
        if (Looper.myLooper() != handler.getLooper()) throw new IllegalStateException("serialized camera handler required");
    }
    private void shutdown(Exception error) {
        closed = true; clearShot();
        try { join.close(); } catch (Exception closing) { error.addSuppressed(closing); }
        try { reader.setOnImageAvailableListener(null, null); }
        catch (RuntimeException closing) { error.addSuppressed(closing); }
        CameraCaptureSession ending = session; session = null;
        if (ending != null) {
            try { ending.close(); } catch (RuntimeException closing) { error.addSuppressed(closing); }
        }
        try { reader.close(); } catch (RuntimeException closing) { error.addSuppressed(closing); }
    }
    @Override public void close() {
        thread(); if (closed) return;
        boolean hadShot = submitted != null;
        Exception error = new IllegalStateException("source closed");
        shutdown(error);
        if (hadShot || error.getSuppressed().length != 0) listener.onFailure("close", error);
    }
}
