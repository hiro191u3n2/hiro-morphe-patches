package com.hiro.ulike.hdr.color;

import com.hiro.ulike.hdr.input.HdrFrame;
import java.nio.ShortBuffer;

/** Read-only, same-size BT.2020 scene-linear view of the exact owned HLG still. */
public final class P010SceneSource {
    /** Luma sample centres have integer coordinates. Never inferred from P010 packing. */
    public enum ChromaLocation { COSITED, MIDPOINT }
    public static final String CONVERSION = "bt2020-ncl-inverse-hlg-reflected-negative-v1";
    private static final double A = .17883277, B = 1 - 4 * A, C = .5 - A * Math.log(4 * A);
    private final HdrFrame frame;
    private final ShortBuffer y, cb, cr;
    public final ChromaLocation horizontal, vertical;
    public final String chromaLocationEvidence;

    public P010SceneSource(HdrFrame frame, ChromaLocation horizontal,
                           ChromaLocation vertical, String chromaLocationEvidence) {
        if (frame == null || horizontal == null || vertical == null)
            throw new NullPointerException("explicit frame and chroma locations required");
        if (chromaLocationEvidence == null || chromaLocationEvidence.trim().isEmpty()
                || chromaLocationEvidence.length() > 80)
            throw new IllegalArgumentException("explicit chroma-location provenance required (1..80 chars)");
        if (frame.width < 2 || frame.height < 2 || (frame.width & 1) != 0 || (frame.height & 1) != 0
                || frame.encoding == null)
            throw new IllegalArgumentException("owned even-size HLG P010 frame required");
        this.frame = frame; this.horizontal = horizontal; this.vertical = vertical;
        this.chromaLocationEvidence = chromaLocationEvidence;
        y = frame.samples(HdrFrame.Component.Y);
        cb = frame.samples(HdrFrame.Component.CB); cr = frame.samples(HdrFrame.Component.CR);
        if ((long) frame.width * frame.height != y.remaining()
                || (long) (frame.width / 2) * (frame.height / 2) != cb.remaining()
                || cb.remaining() != cr.remaining())
            throw new IllegalArgumentException("P010 plane extent mismatch");
    }

    public int width() { return frame.width; }
    public int height() { return frame.height; }
    public HdrFrame frameIdentity() { return frame; }

    /** Relative scene light, not display-linear light or nits. Negative/superwhite values survive. */
    public void readPixel(int x, int row, double[] rgb) {
        requirePixel(x, row);
        if (rgb == null || rgb.length < 3) throw new IllegalArgumentException("RGB destination required");
        readUnchecked(x, row, rgb, 0);
    }

    /** Packed RGB doubles, caller-owned output. No full-image RGB allocation in this module. */
    public void readRows(int firstRow, int rowCount, double[] rgb, int offset, int rowStride) {
        long packed = (long) width() * 3;
        if (firstRow < 0 || rowCount < 1 || (long) firstRow + rowCount > height()
                || rgb == null || offset < 0 || rowStride < packed
                || (long) offset + (long) (rowCount - 1) * rowStride + packed > rgb.length)
            throw new IllegalArgumentException("row destination/range");
        for (int row = 0; row < rowCount; row++)
            for (int x = 0; x < width(); x++)
                readUnchecked(x, firstRow + row, rgb, offset + row * rowStride + x * 3);
    }

    private void requirePixel(int x, int row) {
        if (x < 0 || row < 0 || x >= width() || row >= height())
            throw new IllegalArgumentException("pixel outside source");
    }

    private void readUnchecked(int x, int row, double[] rgb, int off) {
        double yp = y.get(row * width() + x);
        double u = chroma(cb, x, row) - 512, v = chroma(cr, x, row) - 512;
        if (frame.encoding == HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED) {
            yp = (yp - 64) / 876; u /= 896; v /= 896;
        } else { yp /= 1023; u /= 1023; v /= 1023; }
        double rp = yp + 1.4746 * v, bp = yp + 1.8814 * u;
        double gp = (yp - .2627 * rp - .0593 * bp) / .678;
        rgb[off] = inverseHlg(rp); rgb[off + 1] = inverseHlg(gp); rgb[off + 2] = inverseHlg(bp);
    }

    private double chroma(ShortBuffer plane, int x, int row) {
        double cx = (x - (horizontal == ChromaLocation.MIDPOINT ? .5 : 0)) * .5;
        double cy = (row - (vertical == ChromaLocation.MIDPOINT ? .5 : 0)) * .5;
        int w = width() / 2, h = height() / 2;
        cx = Math.max(0, Math.min(w - 1, cx)); cy = Math.max(0, Math.min(h - 1, cy));
        int x0 = (int) Math.floor(cx), y0 = (int) Math.floor(cy);
        int x1 = Math.min(w - 1, x0 + 1), y1 = Math.min(h - 1, y0 + 1);
        double fx = cx - x0, fy = cy - y0;
        double top = plane.get(y0 * w + x0) * (1 - fx) + plane.get(y0 * w + x1) * fx;
        double bottom = plane.get(y1 * w + x0) * (1 - fx) + plane.get(y1 * w + x1) * fx;
        return top * (1 - fy) + bottom * fy;
    }

    /** BT.2100 inverse OETF on [0,1]; continued above 1, explicitly sign-reflected below 0. */
    public static double inverseHlg(double encoded) {
        if (!Double.isFinite(encoded)) throw new IllegalArgumentException("finite HLG value required");
        double value = Math.abs(encoded);
        double scene = value <= .5 ? value * value / 3 : (Math.exp((value - C) / A) + B) / 12;
        if (!Double.isFinite(scene)) throw new IllegalArgumentException("HLG inverse overflow");
        return Math.copySign(scene, encoded);
    }
}
