package com.hiro.ulike.hdr.input;

import java.nio.ShortBuffer;

/** Owned, full-size, planar 10-bit code values. No RGB, tone mapping, resampling or encoding. */
public final class HdrFrame {
    /** BT.2020 non-constant-luminance YCbCr, HLG transfer. Range is never inferred. */
    public enum Encoding { BT2020_NCL_HLG_FULL, BT2020_NCL_HLG_LIMITED }
    public enum Component { Y, CB, CR }

    public final int width, height;
    public final Encoding encoding;
    public final long timestampNs, frameNumber, copiedSampleBytes;
    public final String cameraId, physicalId, activePhysicalId;
    public final long shot;
    public final Long exposureTimeNs;
    public final Integer sensitivityIso;
    private final short[][] samples;

    // The reader exclusively transfers these fresh arrays; no external mutable alias is retained.
    HdrFrame(int width, int height, Encoding encoding, CaptureMatch.Context capture,
             CaptureMatch.Result result, short[][] samples, long copiedSampleBytes) {
        this.width = width; this.height = height; this.encoding = encoding;
        this.timestampNs = result.sensorTimestampNs; this.frameNumber = result.frameNumber;
        this.cameraId = capture.cameraId; this.physicalId = capture.physicalId;
        this.activePhysicalId = result.activePhysicalId; this.shot = capture.shot;
        this.exposureTimeNs = result.exposureTimeNs; this.sensitivityIso = result.sensitivityIso;
        this.samples = samples; this.copiedSampleBytes = copiedSampleBytes;
    }

    /** Each unsigned 10-bit code is stored losslessly as a positive Java short, 0..1023. */
    public ShortBuffer samples(Component component) {
        if (component == null) throw new NullPointerException("component");
        return ShortBuffer.wrap(samples[component.ordinal()]).asReadOnlyBuffer();
    }
    public int planeWidth(Component component) {
        if (component == null) throw new NullPointerException("component");
        return component == Component.Y ? width : width / 2;
    }
    public int planeHeight(Component component) {
        if (component == null) throw new NullPointerException("component");
        return component == Component.Y ? height : height / 2;
    }
}
