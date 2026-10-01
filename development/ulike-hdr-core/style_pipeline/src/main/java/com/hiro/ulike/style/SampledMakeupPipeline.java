package com.hiro.ulike.style;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** High precision, sampled-pixel makeup equations for the two pinned style graphs.
 * Geometry, runtime uniforms and texture upload conventions must be resolved by
 * the caller for the same still image. This class neither detects faces nor
 * accepts HDR data. Pixel samples and all intermediates stay floating point.
 */
public final class SampledMakeupPipeline {
    private SampledMakeupPipeline() {}
    public static final int MAX_TILE_PIXELS = 262144;
    public enum Style { NATURAL_BLUSH, PURITY2 }
    public enum Domain { ENCODED_SDR_FULL_RANGE }

    // Ordering between the two eye renderers at the same z-order is a caller
    // assertion. Their declaration in a scene is not GPU execution evidence.
    public enum Pass {
        NATURAL_BLUSHER(Style.NATURAL_BLUSH, 2002, Kind.MULTIPLY, Seg.NONE, true),
        PURITY_LIPS(Style.PURITY2, 2002, Kind.MULTIPLY, Seg.OUTSIDE_ONE, true),
        PURITY_BLUSHER(Style.PURITY2, 2003, Kind.MULTIPLY, Seg.NONE, true),
        PURITY_FACIAL(Style.PURITY2, 2004, Kind.FACIAL_SOFT_LIGHT, Seg.NONE, false),
        PURITY_3D(Style.PURITY2, 2005, Kind.MULTIPLY, Seg.NONE, false),
        PURITY_EYE_MULTIPLY(Style.PURITY2, 2006, Kind.MULTIPLY, Seg.OUTSIDE_ONE, false),
        PURITY_EYE_SCREEN(Style.PURITY2, 2006, Kind.SCREEN, Seg.OUTSIDE_ONE, false),
        PURITY_EYELASH(Style.PURITY2, 2007, Kind.EYELASH, Seg.OUTSIDE_ZERO, true),
        PURITY_SHADOW_MULTIPLY(Style.PURITY2, 2008, Kind.MULTIPLY, Seg.OUTSIDE_ONE, false),
        PURITY_SHADOW_SCREEN(Style.PURITY2, 2008, Kind.SCREEN, Seg.OUTSIDE_ONE, false);
        public final Style style;
        public final int zOrder;
        private final Kind kind;
        private final Seg seg;
        private final boolean permitsColorOverride;
        Pass(Style style, int z, Kind kind, Seg seg, boolean color) {
            this.style=style; this.zOrder=z; this.kind=kind; this.seg=seg;
            this.permitsColorOverride=color;
        }
    }
    private enum Kind { MULTIPLY, SCREEN, FACIAL_SOFT_LIGHT, EYELASH }
    private enum Seg { NONE, OUTSIDE_ONE, OUTSIDE_ZERO }

    /** Coordinates are in an oriented still-image raster, with row zero at top.
     * The identifier must name an owned still capture, not a preview callback.
     */
    public static final class FrameTile {
        public final String captureId;
        public final long sensorTimestampNs;
        public final int imageWidth, imageHeight, x, y, width, height;
        public FrameTile(String id, long timestamp, int iw, int ih,
                         int x, int y, int width, int height) {
            if(id==null || id.isEmpty() || id.length()>256 || timestamp<0 || iw<1 || ih<1
                    || width<1 || height<1 || x<0 || y<0 || (long)x+width>iw
                    || (long)y+height>ih || (long)width*height>MAX_TILE_PIXELS)
                throw new IllegalArgumentException("invalid still frame/tile identity or budget");
            this.captureId=id; this.sensorTimestampNs=timestamp;
            imageWidth=iw; imageHeight=ih; this.x=x; this.y=y;
            this.width=width; this.height=height;
        }
        public int pixels() { return width*height; }
        boolean same(FrameTile b) {
            return b!=null && captureId.equals(b.captureId) && sensorTimestampNs==b.sensorTimestampNs
                && imageWidth==b.imageWidth && imageHeight==b.imageHeight && x==b.x && y==b.y
                && width==b.width && height==b.height;
        }
    }

    /** Samples are already mapped into exactly this photo tile. RGBA samples
     * use premultiplied RGB, sampled AFTER premultiplication. Neither PNG row
     * orientation nor source alpha convention is guessed here. Coverage is
     * the geometry rasterizer's [0,1] sample coverage, not a generated skin mask.
     * Arrays are borrowed for this synchronous call; callers must not mutate
     * them concurrently. Final intensity/opacity are required, never inferred
     * from the exported slider history or demographic attributes.
     */
    public static final class ResolvedPass {
        public final Pass pass;
        public final FrameTile tile;
        public final double[] rgba, coverage, segmentation, colorOverride, shaderBaseRgb;
        public final boolean[] segmentationInside;
        public final double intensity, opacity;
        public ResolvedPass(Pass pass, FrameTile tile, double[] rgba, double[] coverage,
                            double intensity, double opacity, double[] colorOverride,
                            double[] segmentation, boolean[] segmentationInside) {
            this(pass,tile,rgba,coverage,intensity,opacity,colorOverride,segmentation,segmentationInside,null);
        }
        /** Purity blusher reads videoImageTexture and 3D reads u_basic, which must be explicitly
         * routed by the caller; its shader base can differ from the current
         * framebuffer destination. No source-equals-destination inference.
         */
        public ResolvedPass(Pass pass, FrameTile tile, double[] rgba, double[] coverage,
                            double intensity, double opacity, double[] colorOverride,
                            double[] segmentation, boolean[] segmentationInside, double[] shaderBaseRgb) {
            if(pass==null || tile==null) throw new IllegalArgumentException("missing pass/frame");
            this.pass=pass; this.tile=tile; this.rgba=rgba; this.coverage=coverage;
            this.intensity=intensity; this.opacity=opacity; this.colorOverride=colorOverride;
            this.segmentation=segmentation; this.segmentationInside=segmentationInside;
            this.shaderBaseRgb=shaderBaseRgb;
        }
    }

    static void unit(double v) {
        if(!Double.isFinite(v) || v<0 || v>1) throw new IllegalArgumentException("expected finite unit sample");
    }
    static void unitArray(double[] a, int length) {
        if(a==null || a.length!=length) throw new IllegalArgumentException("sample shape mismatch");
        for(double v:a) unit(v);
    }
    private static void validate(FrameTile tile, ResolvedPass p) {
        if(p==null || !tile.same(p.tile)) throw new IllegalArgumentException("samples are not this still tile");
        int n=tile.pixels();
        unitArray(p.rgba, n*4); unitArray(p.coverage,n); unit(p.intensity); unit(p.opacity);
        if(p.pass==Pass.PURITY_BLUSHER || p.pass==Pass.PURITY_3D) unitArray(p.shaderBaseRgb,n*3);
        else if(p.shaderBaseRgb!=null) throw new IllegalArgumentException("external shader base is only used by Purity blusher/3D");
        if(p.colorOverride!=null) {
            if(!p.pass.permitsColorOverride) throw new IllegalArgumentException("pass has no color override");
            unitArray(p.colorOverride,3);
        }
        if(p.pass.seg==Seg.NONE) {
            if(p.segmentation!=null || p.segmentationInside!=null)
                throw new IllegalArgumentException("segmentation is not used by this pass");
        } else {
            unitArray(p.segmentation,n);
            if(p.segmentationInside==null || p.segmentationInside.length!=n)
                throw new IllegalArgumentException("segmentation coordinate validity is required");
        }
        for(int i=0;i<n;i++) {
            double alpha=p.rgba[4*i+3];
            for(int c=0;c<3;c++) if(p.rgba[4*i+c]>alpha)
                throw new IllegalArgumentException("overlay RGB must be premultiplied before sampling");
            if(p.pass.kind==Kind.FACIAL_SOFT_LIGHT && alpha==0 && p.coverage[i]>0
                    && p.intensity>0 && p.opacity>0)
                throw new IllegalArgumentException("facial opacity undefined for zero-alpha covered sample");
        }
    }
    private static double clamp(double x) { return Math.max(0,Math.min(1,x)); }
    private static double softLight(double b, double s) {
        return s<0.5 ? 2*b*s+b*b*(1-2*s) : Math.sqrt(b)*(2*s-1)+2*b*(1-s);
    }

    /** One makeup pass on opaque RGB. Premultiplied-output blusher/facial
     * fragments are composited using explicit ONE/ONE_MINUS_SRC_ALPHA policy.
     * That policy is part of this replacement, not a claim of GPU bit parity.
     */
    public static double[] apply(Domain domain, FrameTile tile, double[] photo, ResolvedPass p) {
        if(domain!=Domain.ENCODED_SDR_FULL_RANGE || tile==null)
            throw new IllegalArgumentException("explicit encoded SDR still tile required");
        unitArray(photo,tile.pixels()*3); validate(tile,p);
        double[] out=photo.clone();
        for(int i=0;i<tile.pixels();i++) {
            double a=p.rgba[i*4+3];
            if(p.pass==Pass.PURITY_3D) {
                // This is a full opaque fragment, not a premultiplied decal.
                // Even zero intensity writes its explicit u_basic source;
                // only geometry coverage determines whether destination stays.
                double cov=p.coverage[i]; if(cov==0) continue;
                double alpha=a*p.intensity;
                for(int c=0;c<3;c++) {
                    double base=p.shaderBaseRgb[3*i+c];
                    double straight=a>0 ? p.rgba[4*i+c]/a : 0;
                    double frag=base*(1-alpha)+base*straight*alpha;
                    out[3*i+c]=photo[3*i+c]*(1-cov)+frag*cov;
                }
                continue;
            }
            double w=p.coverage[i]*p.intensity;
            // Lash declares opacity but never reads it; 3D has no such uniform.
            if(p.pass.kind!=Kind.EYELASH && p.pass!=Pass.PURITY_3D) w*=p.opacity;
            if(w==0) continue;
            if(p.pass.kind==Kind.FACIAL_SOFT_LIGHT) {
                double red=clamp(p.rgba[i*4]/a);
                w*=clamp((Math.abs(red-0.5)-2.0/255.0)*32.0);
            } else w*=a;
            if(p.pass.seg!=Seg.NONE) {
                // USE_SEG lips/eyes discard almost-transparent samples;
                // eyelashes do not contain that discard.
                if(p.pass.seg==Seg.OUTSIDE_ONE && a<0.001) continue;
                double seg=p.segmentationInside[i] ? p.segmentation[i]
                    : (p.pass.seg==Seg.OUTSIDE_ONE ? 1.0 : 0.0);
                w*=seg;
            }
            if(w==0) continue; // preserve unchanged samples, including signed zero
            for(int c=0;c<3;c++) {
                double b=photo[i*3+c];
                double shaderBase=p.shaderBaseRgb==null ? b : p.shaderBaseRgb[i*3+c];
                double s=p.colorOverride==null ? clamp(p.rgba[i*4+c]/a) : p.colorOverride[c];
                double effect;
                if(p.pass.kind==Kind.SCREEN) effect=1-(1-b)*(1-s);
                else if(p.pass.kind==Kind.FACIAL_SOFT_LIGHT) effect=softLight(b,s);
                else effect=shaderBase*s;
                out[i*3+c]=b*(1-w)+effect*w;
            }
        }
        return out;
    }

    /** Exact makeup-pass coverage is required: one Natural, nine Purity.
     * The caller supplies explicit order within equal-z groups. Neural and LUT
     * stages are not silently implied by this makeup-only entry point.
     */
    public static double[] run(Style style, Domain domain, FrameTile tile,
                               double[] photo, List<ResolvedPass> passes) {
        if(style==null || passes==null || tile==null || domain==null)
            throw new IllegalArgumentException("missing stack contract");
        unitArray(photo,tile.pixels()*3);
        Set<Pass> seen=new HashSet<>(); int z=Integer.MIN_VALUE;
        for(ResolvedPass p:passes) {
            if(p==null || p.pass.style!=style || !seen.add(p.pass) || p.pass.zOrder<z)
                throw new IllegalArgumentException("wrong, repeated or out-of-order makeup pass");
            validate(tile,p); z=p.pass.zOrder;
        }
        for(Pass p:Pass.values()) if(p.style==style && !seen.contains(p))
            throw new IllegalArgumentException("missing makeup pass: "+p);
        double[] result=photo.clone();
        for(ResolvedPass p:passes) result=apply(domain,tile,result,p);
        return result;
    }
}
