package com.hiro.ulike.style;

import java.util.List;

/** Java counterpart of the independently verified style_color equations.
 * Sampler convention is explicitly linear/clamp; this is not proof of the
 * original SDK texture upload, decode gamma, or runtime uniforms.
 */
public final class StyleLutPipeline {
    private StyleLutPipeline() {}
    public static final class Texture {
        public final int width,height;
        private final double[] rgb;
        /** Row zero must be the row sampled near v=0, with no hidden ICC or
         * transfer conversion. Photo and LUT authored RGB encoding must match.
         */
        public Texture(int width,int height,double[] rgb) {
            if(width<1 || height<1 || (long)width*height>1048576)
                throw new IllegalArgumentException("LUT texture budget exceeded");
            SampledMakeupPipeline.unitArray(rgb,width*height*3);
            this.width=width; this.height=height; this.rgb=rgb.clone();
        }
        double sample(double u,double v,int c) {
            double x=Math.max(0,Math.min(width-1,u*width-0.5));
            double y=Math.max(0,Math.min(height-1,v*height-0.5));
            int x0=(int)Math.floor(x), y0=(int)Math.floor(y);
            int x1=Math.min(width-1,x0+1), y1=Math.min(height-1,y0+1);
            double tx=x-x0,ty=y-y0;
            double top=rgb[3*(y0*width+x0)+c]*(1-tx)+rgb[3*(y0*width+x1)+c]*tx;
            double bot=rgb[3*(y1*width+x0)+c]*(1-tx)+rgb[3*(y1*width+x1)+c]*tx;
            return top*(1-ty)+bot*ty;
        }
    }
    public static final class ResolvedLuts {
        public final SampledMakeupPipeline.FrameTile tile;
        public final Texture background,skin,finalLut;
        public final double[] skinMaskAfterShaderFlip;
        public final double skinIntensity,finalIntensity;
        public ResolvedLuts(SampledMakeupPipeline.FrameTile tile, Texture background,
                            Texture skin, double[] skinMaskAfterShaderFlip,
                            double skinIntensity, Texture finalLut,double finalIntensity) {
            this.tile=tile;this.background=background;this.skin=skin;
            this.skinMaskAfterShaderFlip=skinMaskAfterShaderFlip;
            this.skinIntensity=skinIntensity;this.finalLut=finalLut;this.finalIntensity=finalIntensity;
        }
    }
    private static double coord(int tile,double color) {
        // Both original shaders literally use 512, even for a 1024px PNG.
        return tile/8.0+0.5/512.0+(1.0/8.0-1.0/512.0)*color;
    }
    public static double[] skin(SampledMakeupPipeline.Domain domain,
                                SampledMakeupPipeline.FrameTile tile,double[] photo,
                                Texture background,Texture skin,double[] mask,double intensity) {
        if(domain!=SampledMakeupPipeline.Domain.ENCODED_SDR_FULL_RANGE || tile==null
                || background==null || skin==null) throw new IllegalArgumentException("missing SDR LUT contract");
        SampledMakeupPipeline.unitArray(photo,tile.pixels()*3);
        SampledMakeupPipeline.unitArray(mask,tile.pixels()); SampledMakeupPipeline.unit(intensity);
        double[] out=photo.clone(); if(intensity==0) return out;
        for(int i=0;i<tile.pixels();i++) {
            int slice=(int)Math.floor(photo[3*i+2]*63);
            double u=coord(slice%8,photo[3*i]), v=coord(slice/8,photo[3*i+1]);
            for(int c=0;c<3;c++) {
                double edited=background.sample(u,v,c)*(1-mask[i])+skin.sample(u,v,c)*mask[i];
                out[3*i+c]=photo[3*i+c]*(1-intensity)+edited*intensity;
            }
        }
        return out;
    }
    public static double[] finalLut(SampledMakeupPipeline.Domain domain,
                                    SampledMakeupPipeline.FrameTile tile,double[] photo,
                                    Texture lut,double intensity) {
        if(domain!=SampledMakeupPipeline.Domain.ENCODED_SDR_FULL_RANGE || tile==null || lut==null)
            throw new IllegalArgumentException("missing SDR LUT contract");
        SampledMakeupPipeline.unitArray(photo,tile.pixels()*3); SampledMakeupPipeline.unit(intensity);
        double[] out=photo.clone(); if(intensity==0) return out;
        for(int i=0;i<tile.pixels();i++) {
            double b=photo[3*i+2]*63;
            int lo=(int)Math.floor(b), hi=(int)Math.ceil(b);double t=b-lo;
            double u0=coord(lo%8,photo[3*i]), v0=coord(lo/8,photo[3*i+1]);
            double u1=coord(hi%8,photo[3*i]), v1=coord(hi/8,photo[3*i+1]);
            for(int c=0;c<3;c++) {
                double edited=lut.sample(u0,v0,c)*(1-t)+lut.sample(u1,v1,c)*t;
                out[3*i+c]=photo[3*i+c]*(1-intensity)+edited*intensity;
            }
        }
        return out;
    }
    /** Executes every declared makeup pass after an externally produced neural
     * image, then the skin/background LUT and (Purity only) final LUT.
     * This does not generate geometry, segmentation or neural inference.
     */
    public static double[] runPostNeural(SampledMakeupPipeline.Style style,
                                          SampledMakeupPipeline.Domain domain,
                                          SampledMakeupPipeline.FrameTile tile,double[] neuralPhoto,
                                          List<SampledMakeupPipeline.ResolvedPass> makeup,
                                          ResolvedLuts luts) {
        if(luts==null || tile==null || !tile.same(luts.tile))
            throw new IllegalArgumentException("LUT mask is not this still tile");
        if(style==SampledMakeupPipeline.Style.NATURAL_BLUSH && (luts.finalLut!=null || luts.finalIntensity!=0))
            throw new IllegalArgumentException("Natural has no separate final LUT");
        if(style==SampledMakeupPipeline.Style.PURITY2 && luts.finalLut==null)
            throw new IllegalArgumentException("Purity final LUT is required");
        double[] made=SampledMakeupPipeline.run(style,domain,tile,neuralPhoto,makeup);
        double[] colored=skin(domain,tile,made,luts.background,luts.skin,
                              luts.skinMaskAfterShaderFlip,luts.skinIntensity);
        if(style==SampledMakeupPipeline.Style.PURITY2)
            return finalLut(domain,tile,colored,luts.finalLut,luts.finalIntensity);
        return colored;
    }
}
