package com.hiro.ulike.hdr.input.android;

import android.graphics.Rect;
import android.hardware.DataSpace;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.os.Build;
import java.nio.ByteBuffer;
import java.util.Map;
import com.hiro.ulike.hdr.input.CaptureMatch;
import com.hiro.ulike.hdr.input.HdrFrame;
import com.hiro.ulike.hdr.input.InputRejected;
import com.hiro.ulike.hdr.input.P010FrameReader;

/** API 33+ adapter. It neither acquires an Image nor configures/opens/captures a camera. */
public final class AndroidP010FrameReader {
    private AndroidP010FrameReader() {}

    /**
     * Borrowed Image: caller retains close responsibility on success and on every exception.
     * Call only with an exclusively owned, untouched Image and serialized camera lifecycle.
     * resultSubmitted is captured at submission, never synthesized from the current selection.
     */
    public static HdrFrame copyBorrowed(Image image, P010FrameReader.Request request,
            CaptureMatch.Context capture, CaptureMatch.Source source,
            TotalCaptureResult total, CaptureMatch.Context resultSubmitted,
            CaptureMatch.Current current) throws InputRejected {
        if (Build.VERSION.SDK_INT < 33)
            throw reject(InputRejected.Reason.COLOR, "API 33+ actual Image dataspace required");
        if (image == null || total == null || capture == null)
            throw reject(InputRejected.Reason.ARGUMENT, "Image, capture and TotalCaptureResult required");
        if (image.getFormat() != P010FrameReader.FORMAT_P010)
            throw reject(InputRejected.Reason.FORMAT, "Image is not P010");
        HdrFrame.Encoding encoding = encoding(image.getDataSpace());
        CaptureResult timestampResult = total;
        if (capture.physicalId != null) {
            Map<String, CaptureResult> physical = total.getPhysicalCameraResults();
            timestampResult = physical == null ? null : physical.get(capture.physicalId);
            if (timestampResult == null)
                throw reject(InputRejected.Reason.RESULT, "selected physical camera result is missing");
        }
        Long sensorTimestamp = timestampResult.get(CaptureResult.SENSOR_TIMESTAMP);
        if (sensorTimestamp == null)
            throw reject(InputRejected.Reason.TIMESTAMP, "SENSOR_TIMESTAMP is missing");
        CaptureMatch.Result result = new CaptureMatch.Result(resultSubmitted, total.getRequest(),
                sensorTimestamp, total.getFrameNumber(), capture.physicalId,
                total.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID),
                timestampResult.get(CaptureResult.SENSOR_EXPOSURE_TIME),
                timestampResult.get(CaptureResult.SENSOR_SENSITIVITY));
        Rect crop = image.getCropRect();
        if (crop == null) throw reject(InputRejected.Reason.CROP, "image crop is missing");
        Image.Plane[] nativePlanes = image.getPlanes();
        if (nativePlanes == null || nativePlanes.length != 3)
            throw reject(InputRejected.Reason.FORMAT, "P010 must expose Y/Cb/Cr logical planes");
        P010FrameReader.Plane[] planes = new P010FrameReader.Plane[3];
        for (int i = 0; i < 3; i++) {
            Image.Plane p = nativePlanes[i];
            ByteBuffer data = p == null ? null : p.getBuffer();
            if (data == null || data.position() != 0)
                throw reject(InputRejected.Reason.BUFFER_BOUNDS, "missing or previously consumed Image plane");
            planes[i] = new P010FrameReader.Plane(data, p.getRowStride(), p.getPixelStride());
        }
        P010FrameReader.Input input = new P010FrameReader.Input(image.getFormat(), image.getWidth(),
                image.getHeight(), crop.left, crop.top, crop.right, crop.bottom, image.getTimestamp(),
                encoding, source, planes);
        return P010FrameReader.copy(input, request, capture, result, current);
    }

    /**
     * Ownership transfer at entry: the adapter closes the Image once on success or failure.
     * The caller must not close or use the Image again, or access its planes concurrently.
     */
    public static HdrFrame copyAndClose(Image ownedImage, P010FrameReader.Request request,
            CaptureMatch.Context capture, CaptureMatch.Source source,
            TotalCaptureResult total, CaptureMatch.Context resultSubmitted,
            CaptureMatch.Current current) throws InputRejected {
        // try-with-resources preserves the original rejection when close itself also fails.
        try (Image closing = ownedImage) {
            return copyBorrowed(closing, request, capture, source, total, resultSubmitted, current);
        }
    }

    static HdrFrame.Encoding encoding(int dataSpace) throws InputRejected {
        if (DataSpace.getStandard(dataSpace) != DataSpace.STANDARD_BT2020
                || DataSpace.getTransfer(dataSpace) != DataSpace.TRANSFER_HLG)
            throw reject(InputRejected.Reason.COLOR, "actual BT2020 non-constant-luminance HLG dataspace required");
        int range = DataSpace.getRange(dataSpace);
        if (dataSpace != (DataSpace.STANDARD_BT2020 | DataSpace.TRANSFER_HLG | range))
            throw reject(InputRejected.Reason.COLOR, "unrecognized dataspace bits");
        if (range == DataSpace.RANGE_FULL) return HdrFrame.Encoding.BT2020_NCL_HLG_FULL;
        if (range == DataSpace.RANGE_LIMITED) return HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED;
        throw reject(InputRejected.Reason.COLOR, "explicit FULL or LIMITED range required");
    }
    private static InputRejected reject(InputRejected.Reason reason, String message) {
        return new InputRejected(reason, message);
    }
}
