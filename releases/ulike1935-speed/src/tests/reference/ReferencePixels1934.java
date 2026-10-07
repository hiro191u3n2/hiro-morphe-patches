package com.hiro.ulike;

/**
 * Android-independent, bounded-workspace quality primitives for ULike 1.9.32.
 *
 * Source-domain denoise/chroma correction, resampling and output-domain sharpening
 * are deliberately separate. No method reads a pre-beauty/original image or mixes
 * it back into the corrected image. All filters preserve non-opaque pixels exactly;
 * the resampler uses premultiplied alpha so invisible RGB cannot create fringes.
 */
public final class ReferencePixels1934 {
    private ReferencePixels1934() {}

    public static final int HALO = 32;
    public static final int LENS_UNKNOWN = 0;
    public static final int LENS_FRONT = 1;
    public static final int LENS_WIDE = 2;
    public static final int LENS_ULTRAWIDE = 3;
    public static final int LENS_TELE = 4;
    private static final int HISTOGRAM_SIZE = 1024;

    /** Receives a full, unscaled row. The reader must fill every element. */
    public interface RowSource { void readRow(int row, int[] pixels); }
    /** Consumes a row immediately; do not retain the reusable array. */
    public interface RowSink { void writeRow(int row, int[] pixels); }
    /** Geometric masks are image-bound. No global skin-colour fallback. */
    public interface RegionMask { int skinQ8(int x,int y); int detailQ8(int x,int y); }

    public static final class NoiseStats {
        public final float lumaSigma;
        public final float chromaSigma;
        public final float meanLuma;
        public final float detailFraction;
        public final int samples;

        public NoiseStats(float lumaSigma, float chromaSigma, float meanLuma,
                          float detailFraction, int samples) {
            this.lumaSigma = finiteClamp(lumaSigma, 0f, 64f, 0f);
            this.chromaSigma = finiteClamp(chromaSigma, 0f, 96f, 0f);
            this.meanLuma = finiteClamp(meanLuma, 0f, 255f, 128f);
            this.detailFraction = finiteClamp(detailFraction, 0f, 1f, 0f);
            this.samples = Math.max(0, samples);
        }
    }

    /**
     * Merge several contiguous, source-resolution patches into one noise estimate.
     * Do not feed a filtered thumbnail: its resampling would hide the sensor noise.
     * The diagonal Haar residual rejects constant tones and linear illumination.
     * Robust median and coarse-edge exclusion limit contamination by real structure.
     */
    public static final class NoiseProbe {
        private final int[] luma = new int[HISTOGRAM_SIZE];
        private final int[] chroma = new int[HISTOGRAM_SIZE];
        private int accepted;
        private int examined;
        private long sumLuma;

        public void add(int[] pixels, int width, int height) {
            checkPixels(pixels, width, height);
            if (width < 2 || height < 2 || accepted >= 32768) return;
            int stride = Math.max(1, (int)Math.sqrt((long)width * height / 4096.0));
            for (int y = 0; y < height - 1 && accepted < 32768; y += stride) {
                // Changing phase per row avoids systematically sampling one lattice.
                int phase = stride == 1 ? 0 : (y / stride * 13) % stride;
                for (int x = phase; x < width - 1 && accepted < 32768; x += stride) {
                    int i = y * width + x;
                    int a = pixels[i], b = pixels[i + 1];
                    int c = pixels[i + width], d = pixels[i + width + 1];
                    if ((a >>> 24) != 255 || (b >>> 24) != 255 ||
                        (c >>> 24) != 255 || (d >>> 24) != 255) continue;
                    int ay = luma(a), by = luma(b), cy = luma(c), dy = luma(d);
                    int mean = (ay + by + cy + dy + 2) >> 2;
                    examined++;
                    sumLuma += mean;
                    int gx = Math.abs(ay + cy - by - dy);
                    int gy = Math.abs(ay + by - cy - dy);
                    // Avoid clipping and strong scene edges; the threshold deliberately
                    // leaves moderate random grain available for the estimator.
                    if (mean < 5 || mean > 250 || gx > 72 || gy > 72) continue;
                    int yl = Math.abs(ay - by - cy + dy);
                    int u = Math.abs(blueGreen(a) - blueGreen(b) - blueGreen(c) + blueGreen(d));
                    int v = Math.abs(redGreen(a) - redGreen(b) - redGreen(c) + redGreen(d));
                    luma[Math.min(HISTOGRAM_SIZE - 1, yl)]++;
                    chroma[Math.min(HISTOGRAM_SIZE - 1, (u + v + 1) >> 1)]++;
                    accepted++;
                }
            }
        }

        public NoiseStats finish() {
            if (accepted == 0)
                return new NoiseStats(0f, 0f, examined == 0 ? 128f :
                    (float)sumLuma / examined, examined == 0 ? 0f : 1f, 0);
            // For independent Gaussian samples, this Haar residual has sigma 2*sigma;
            // median(abs(residual)) is 1.34898*sigma. This is an image-domain estimate,
            // not a claim that ISP-processed RGB follows a sensor RAW noise model.
            float sy = median(luma, accepted) / 1.34898f;
            float sc = median(chroma, accepted) / 1.34898f;
            return new NoiseStats(sy, sc, (float)sumLuma / Math.max(1, examined),
                1f - (float)accepted / Math.max(1, examined), accepted);
        }
    }

    public static NoiseStats estimate(int[] pixels, int width, int height) {
        NoiseProbe probe = new NoiseProbe();
        probe.add(pixels, width, height);
        return probe.finish();
    }

    /** Immutable per-shot policy; safe to share between the existing four workers. */
    public static final class Plan {
        public final int noiseLevel;
        public final int sharpLevel;
        public final int shadowBudgetQ8;
        public final int beautyQ8;
        public final int sharpGainQ8;
        public final int sharpFloorQ8;
        public final int sharpLimit;
        public final float sourceSigma;
        public final float outputScale;
        public final boolean texturePriority;
        public final boolean shadowPriority;
        public final boolean haloSuppression;
        public final SpatialNoise1934 localNoise;
        public final RegionMask faceRegions;

        private Plan(int noiseLevel, int sharpLevel, int shadowBudgetQ8, int beautyQ8,
                     int sharpGainQ8, int sharpFloorQ8, int sharpLimit,
                     float sourceSigma, float outputScale,
                     boolean texturePriority, boolean shadowPriority, boolean haloSuppression, SpatialNoise1934 localNoise, RegionMask faceRegions) {
            this.noiseLevel = noiseLevel;
            this.sharpLevel = sharpLevel;
            this.shadowBudgetQ8 = shadowBudgetQ8;
            this.beautyQ8 = beautyQ8;
            this.sharpGainQ8 = sharpGainQ8;
            this.sharpFloorQ8 = sharpFloorQ8;
            this.sharpLimit = sharpLimit;
            this.sourceSigma = sourceSigma;
            this.outputScale = outputScale;
            this.texturePriority = texturePriority;
            this.shadowPriority = shadowPriority;
            this.haloSuppression = haloSuppression;
            this.localNoise = localNoise;
            this.faceRegions = faceRegions;
        }

        public Plan withFaceRegions(RegionMask mask) {
            return new Plan(noiseLevel,sharpLevel,shadowBudgetQ8,beautyQ8,sharpGainQ8,
                sharpFloorQ8,sharpLimit,sourceSigma,outputScale,texturePriority,shadowPriority,
                haloSuppression,localNoise,mask);
        }

        public Plan withLocalNoise(SpatialNoise1934 localNoise, int requestedNoise) {
            // Use the user's selected level as the filter maximum and attenuate per pixel.
            return new Plan(clamp(requestedNoise,0,4),sharpLevel,
                requestedNoise<=0?0:Math.max(128,shadowBudgetQ8),beautyQ8,
                sharpGainQ8,sharpFloorQ8,sharpLimit,sourceSigma,outputScale,
                texturePriority,shadowPriority,haloSuppression,localNoise,faceRegions);
        }

        /** Keep the existing user halo option when sharpening moves to the output stage. */
        public Plan withHaloSuppression(boolean enabled) {
            if (enabled == haloSuppression) return this;
            return new Plan(noiseLevel, sharpLevel, shadowBudgetQ8, beautyQ8,
                sharpGainQ8, sharpFloorQ8, sharpLimit, sourceSigma, outputScale,
                texturePriority, shadowPriority, enabled, localNoise, faceRegions);
        }
    }

    /**
     * Existing user levels remain upper bounds, and an OFF setting remains OFF.
     * Image evidence dominates weak ISO/lens priors; a high ISO does not re-smooth
     * an already clean beauty-processed image. Exposure is used only as a conservative
     * sharpness prior, not as a claim that a long exposure is inherently noisy.
     */
    public static Plan plan(NoiseStats stats, int iso, long exposureNanos, int lensKind,
                            float beautyStrength, int requestedNoise, int requestedSharp,
                            boolean texturePriority, boolean shadowPriority,
                            float outputScale) {
        if (stats == null) stats = new NoiseStats(3f, 4f, 128f, 0f, 0);
        int noise = clamp(requestedNoise, 0, 4);
        int sharp = clamp(requestedSharp, 0, 4);
        float scale = finiteClamp(outputScale, 0.01f, 64f, 1f);
        float beauty = finiteClamp(beautyStrength, 0f, 1f, 0f);
        float measured = Math.max(stats.lumaSigma, stats.chromaSigma * 0.42f);
        float prior = iso > 0 ? (float)(1.8 * (Math.sqrt(Math.max(1, iso) / 100.0) - 1)) : 0f;
        prior = Math.max(0f, prior);
        if (lensKind == LENS_FRONT || lensKind == LENS_TELE || lensKind == LENS_ULTRAWIDE)
            prior *= 1.06f;
        float sigma;
        if (stats.samples >= 48) {
            // Even an unexpectedly large metadata prior can move the estimate by
            // at most 0.24 code values when the measured image is clean.
            sigma = measured * 0.88f + Math.min(prior, measured + 2f) * 0.12f;
        } else {
            sigma = Math.max(measured, Math.min(8f, 2f + prior * 0.3f));
        }
        sigma = finiteClamp(sigma, 0f, 32f, 3f);
        if (stats.samples >= 48) {
            if (sigma < 0.8f) noise = Math.max(0, noise - 2);
            else if (sigma < 3.8f) noise = Math.max(0, noise - 1);
        }
        // The third cleanup pass gets the remaining budget after the primary NR.
        // Beauty is applied locally by shadowBudgetQ8(), never globally to backgrounds.
        int shadowBudget = noise == 0 ? 0 : Math.round(256f *
            (0.50f + 0.40f * Math.min(1f, sigma / 12f)));
        float gain = sharp == 0 ? 0f : 0.17f + 0.17f * sharp;
        // Enlarging cannot create new captured detail; use gentler final sharpening.
        if (scale > 1f) gain /= (float)Math.sqrt(scale);
        if (scale < 1f) gain *= 0.90f + 0.10f * scale;
        if (exposureNanos > 50000000L) gain *= 0.94f;
        if (lensKind == LENS_TELE && exposureNanos > 25000000L) gain *= 0.95f;
        gain *= 1f / (1f + sigma * 0.012f);
        // Keep a conservative source-derived floor even after NR: flat random grain
        // must not be converted into artificial detail by the final output stage.
        float effectiveSigma = sigma * (scale < 1f ? (float)Math.sqrt(scale) : 1f);
        float floor = 0.65f + effectiveSigma * 1.25f;
        return new Plan(noise, sharp, clamp(shadowBudget, 0, 256), Math.round(beauty * 256f),
            Math.round(gain * 256f), Math.round(floor * 256f), 3 + sharp * 2,
            sigma, scale, texturePriority, shadowPriority, true, null, null);
    }

    /**
     * Scale a main-NR mix or the existing final shadow mix, before colour correction.
     * localRange should be the current processed-neighbourhood maxY-minY.
     * Only skin-like, already smooth pixels receive the beauty overlap reduction.
     */
    public static int shadowBudgetQ8(int argb, int localRange, Plan plan) {
        return shadowBudgetQ8(argb,localRange,plan,-1,-1);
    }
    public static int shadowBudgetQ8(int argb,int localRange,Plan plan,int x,int y) {
        if (plan == null) return 256;
        int budget = plan.shadowBudgetQ8;
        if (budget == 0 || plan.beautyQ8 == 0 || (argb >>> 24) != 255) return budget;
        int skin = plan.faceRegions == null || x<0 || y<0 ? 0 : plan.faceRegions.skinQ8(x,y);
        if (skin == 0) return budget;
        int smoothLimit = 10 + Math.round(Math.min(12f, plan.sourceSigma) * 2f);
        int smooth = clamp((smoothLimit - Math.max(0, localRange)) * 256 /
            Math.max(1, smoothLimit), 0, 256);
        int reduction = (int)((long)skin * smooth * plan.beautyQ8 * 112 >> 24);
        return budget * (256 - reduction) >> 8;
    }

    /** Local main-NR coordination, separate from the third-pass shadow budget. */
    public static int denoiseBudgetQ8(int argb, int localRange, Plan plan) {
        return denoiseBudgetQ8(argb,localRange,plan,-1,-1);
    }
    public static int denoiseBudgetQ8(int argb,int localRange,Plan plan,int x,int y) {
        if (plan == null) return 256;
        int base = plan.shadowBudgetQ8;
        if (base <= 0) return 256;
        // Reuse only the local beauty reduction, without globally weakening main NR.
        int budget=clamp(shadowBudgetQ8(argb,localRange,plan,x,y)*256/base,128,256);
        if(plan.faceRegions!=null && x>=0 && y>=0) {
            int detail=clamp(plan.faceRegions.detailQ8(x,y),0,256);
            budget=budget*(256-(detail*208>>8))>>8;
        }
        return budget;
    }

    public static void localDenoiseMix(int[] source,int[] filtered,int width,int begin,int end,
            int top,ReferencePixels1934.Plan plan) {
        localDenoiseMix(source,filtered,width,source.length/width,begin,end,top,plan);
    }
    public static void localDenoiseMix(int[] source,int[] filtered,int width,int rows,int begin,int end,
            int top,ReferencePixels1934.Plan plan) {
        for(int row=begin;row<end;row++)for(int x=0;x<width;x++) {
            int at=row*width+x,p=source[at],q=filtered[at];
            if((p>>>24)!=255 || (q>>>24)!=255){filtered[at]=p;continue;}
            int budget=plan.localNoise==null?256:plan.localNoise.budgetQ8(x,row+top);
            // The existing skin/beauty overlap is also applied before colour correction.
            int localRange=0;
            if(x>0&&x<width-1&&row>0&&row+1<rows)
                localRange=Math.max(Math.abs(ReferencePixels1934.luma(source[at-1])-ReferencePixels1934.luma(source[at+1])),
                    Math.abs(ReferencePixels1934.luma(source[at-width])-ReferencePixels1934.luma(source[at+width])));
            budget=budget*ReferencePixels1934.denoiseBudgetQ8(p,localRange,plan,x,row+top)>>8;
            if(budget>=256)continue;
            filtered[at]=(p&0xff000000)|(mix((p>>>16)&255,(q>>>16)&255,budget)<<16)
                |(mix((p>>>8)&255,(q>>>8)&255,budget)<<8)|mix(p&255,q&255,budget);
        }
    }
    private static int mix(int p,int q,int a){return (p*(256-a)+q*a+128)>>8;}

    public static void finishStrip(int[] processed, int[] output, int width, int rows,
                                   int begin, int end, Plan plan,
                                   boolean moire, boolean sharp) {
        finishStripAt(processed,output,width,rows,begin,end,plan,moire,sharp,0);
    }
    public static void finishStripAt(int[] processed,int[] output,int width,int rows,
            int begin,int end,Plan plan,boolean moire,boolean sharp,int originY) {
        checkStrip(processed, output, width, rows, begin, end);
        for (int row = begin; row < end; row++) {
            if ((row & 15) == 0 && Thread.currentThread().isInterrupted())
                throw new IllegalStateException("quality interrupted");
            int offset = row * width;
            for (int x = 0; x < width; x++) {
                int at = offset + x;
                int original = processed[at];
                int p = original;
                if ((p >>> 24) == 255) {
                    if (moire && row >= 4 && row < rows - 4 && x >= 4 && x < width - 4) {
                        p = removePeriodicChroma(processed, at, width, p);
                        if (p == original && row >= HALO && row < rows-HALO && x >= HALO && x < width-HALO)
                            p = LongMoire1934.correct(processed,at,width,p);
                    }
                    if (sharp && plan != null && plan.sharpGainQ8 > 0 &&
                        row > 0 && row < rows - 1 && x > 0 && x < width - 1)
                        p = sharpen(processed, at, width, p, plan,x,row+originY);
                }
                output[at] = p;
            }
        }
    }

    private static int sharpen(int[] src, int at, int width, int corrected, Plan plan,int x,int y) {
        int center = luma(src[at]);
        int left = luma(src[at - 1]), right = luma(src[at + 1]);
        int up = luma(src[at - width]), down = luma(src[at + width]);
        // Do not sample invisible pixels into a visible edge.
        if ((src[at - 1] >>> 24) != 255 || (src[at + 1] >>> 24) != 255 ||
            (src[at - width] >>> 24) != 255 || (src[at + width] >>> 24) != 255 ||
            (src[at - width - 1] >>> 24) != 255 || (src[at - width + 1] >>> 24) != 255 ||
            (src[at + width - 1] >>> 24) != 255 || (src[at + width + 1] >>> 24) != 255)
            return corrected;
        int nw = luma(src[at - width - 1]), ne = luma(src[at - width + 1]);
        int sw = luma(src[at + width - 1]), se = luma(src[at + width + 1]);
        int blurQ4 = center * 4 + (left + right + up + down) * 2 + nw + ne + sw + se;
        int detailQ8 = (center * 16 - blurQ4) * 16;
        int magnitude = Math.abs(detailQ8) - plan.sharpFloorQ8;
        if (magnitude <= 0) return corrected;
        int gain = plan.sharpGainQ8;
        if (center < 64) gain = gain * (96 + center * 160 / 64) >> 8;
        if (plan.texturePriority) {
            int skin = plan.faceRegions==null?0:plan.faceRegions.skinQ8(x,y);
            gain = gain * (256 - (skin * (20 + (plan.beautyQ8 >> 3)) >> 8)) >> 8;
        }
        int delta = (int)Math.min(plan.sharpLimit, ((long)magnitude * gain + 32768L) >> 16);
        if (delta == 0) return corrected;
        if (detailQ8 < 0) delta = -delta;
        int minY = Math.min(center, Math.min(Math.min(left, right), Math.min(up, down)));
        int maxY = Math.max(center, Math.max(Math.max(left, right), Math.max(up, down)));
        // Default ON forbids overshoot of local extrema. OFF preserves the existing
        // option's effect while the sharpLimit above still bounds every increment.
        if (plan.haloSuppression) delta = clamp(delta, minY - center, maxY - center);
        int r = red(corrected), g = green(corrected), b = blue(corrected);
        // A shared RGB offset changes luma without changing r-g or b-g. Clipping
        // the offset, rather than each channel independently, preserves hue.
        delta = clamp(delta, -Math.min(r, Math.min(g, b)), 255 - Math.max(r, Math.max(g, b)));
        return (corrected & 0xff000000) | ((r + delta) << 16) | ((g + delta) << 8) | (b + delta);
    }

    /**
     * Conservative directional period-2/period-4 chroma rejection, used BEFORE
     * resampling. An isolated colour edge, a linear warm gradient, saturated colours,
     * and a matching luma pattern do not qualify as periodic false colour.
     */
    private static int removePeriodicChroma(int[] src, int at, int width, int center) {
        int cU = blueGreen(center), cV = redGreen(center), y = luma(center);
        if (Math.max(Math.abs(cU), Math.abs(cV)) > 104) return center;
        int bestAmount = 0, bestU = cU, bestV = cV;
        for (int direction = 0; direction < 4; direction++) {
            int step = direction == 0 ? 1 : direction == 1 ? width :
                direction == 2 ? width + 1 : width - 1;
            int m1 = src[at - step], p1 = src[at + step];
            if ((m1 >>> 24) != 255 || (p1 >>> 24) != 255) continue;
            int m2 = src[at - 2 * step], p2 = src[at + 2 * step];
            if ((m2 >>> 24) != 255 || (p2 >>> 24) != 255) continue;
            int m1u = blueGreen(m1), p1u = blueGreen(p1);
            int m1v = redGreen(m1), p1v = redGreen(p1);
            int m2u = blueGreen(m2), p2u = blueGreen(p2);
            int m2v = redGreen(m2), p2v = redGreen(p2);
            int amp1 = Math.max(Math.abs(2*cU-m1u-p1u), Math.abs(2*cV-m1v-p1v)) / 2;
            int amp2 = Math.max(Math.abs(2*cU-m2u-p2u), Math.abs(2*cV-m2v-p2v)) / 2;
            if (amp1 < 8 && amp2 < 8) continue;
            int m4 = src[at - 4 * step], p4 = src[at + 4 * step];
            if ((m4 >>> 24) != 255 || (p4 >>> 24) != 255) continue;
            int y1m = luma(m1), y1p = luma(p1);
            int y2m = luma(m2), y2p = luma(p2);
            for (int period = 2; period <= 4; period += 2) {
                int amplitude = period == 2 ? amp1 : amp2;
                if (amplitude < 8 || amplitude > 112) continue;
                int repeat = period == 2 ?
                    Math.max(Math.abs(cU-m2u)+Math.abs(cU-p2u), Math.abs(cV-m2v)+Math.abs(cV-p2v)) / 2 :
                    Math.max(Math.abs(cU-blueGreen(m4))+Math.abs(cU-blueGreen(p4)),
                             Math.abs(cV-redGreen(m4))+Math.abs(cV-redGreen(p4))) / 2;
                if (repeat > 2 + amplitude / 5) continue;
                int opposition = period == 2 ?
                    Math.max(Math.abs(m1u-p1u), Math.abs(m1v-p1v)) :
                    Math.max(Math.abs(m2u-p2u), Math.abs(m2v-p2v));
                if (opposition > 3 + amplitude / 4) continue;
                int lumaPattern = period == 2 ? Math.abs(2*y-y1m-y1p)/2 :
                    Math.abs(2*y-y2m-y2p)/2;
                if (lumaPattern > 3 + amplitude / 5) continue;
                // Require stable repeat on BOTH sides. This also prevents a lone
                // warm line or one-sided colour boundary from being desaturated.
                int meanU = period == 2 ? (2*cU + m1u + p1u) / 4 :
                    (m2u + p2u + 2*(m1u+cU+p1u)) / 8;
                int meanV = period == 2 ? (2*cV + m1v + p1v) / 4 :
                    (m2v + p2v + 2*(m1v+cV+p1v)) / 8;
                if (Math.max(Math.abs(meanU), Math.abs(meanV)) > 76) continue;
                int amount = clamp((amplitude - 6) * 12, 0, 216);
                amount = amount * (amplitude + 2 - Math.min(amplitude, repeat * 2)) /
                    (amplitude + 2);
                amount = amount * (amplitude + 4 - Math.min(amplitude, lumaPattern * 3)) /
                    (amplitude + 4);
                if (amount > bestAmount) { bestAmount = amount; bestU = meanU; bestV = meanV; }
            }
        }
        if (bestAmount <= 0) return center;
        int u = cU + roundDiv((bestU-cU) * bestAmount, 256);
        int v = cV + roundDiv((bestV-cV) * bestAmount, 256);
        // y=(77*r+150*g+29*b)/256; r=g+v, b=g+u.
        int g = roundDiv(256*y - 77*v - 29*u, 256);
        int r = g + v, b = g + u;
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) return center;
        return (center & 0xff000000) | (r << 16) | (g << 8) | b;
    }

    public static void resize(RowSource source, int sourceWidth, int sourceHeight,
                              RowSink sink, int width, int height) {
        resizeCrop(source, sourceWidth, sourceHeight, sink, width, height,
            0.0, 0.0, sourceWidth, sourceHeight);
    }

    /**
     * Continuous centred-crop capable separable resampler. Pixel centres are at
     * n+0.5 in the supplied source coordinate system; the caller may expose rotation
     * through RowSource without allocating a full intermediate rotated bitmap.
     * Enlargement uses clamped Catmull-Rom; reduction scales a Lanczos-2 low-pass
     * kernel with the reduction ratio. Fixed eight-row cache bounds pixel workspace.
     */
    public static void resizeCrop(RowSource source, int sourceWidth, int sourceHeight,
                                  RowSink sink, int width, int height,
                                  double left, double top, double cropWidth, double cropHeight) {
        if (source == null || sink == null || sourceWidth < 1 || sourceHeight < 1 ||
            width < 1 || height < 1 || width > Integer.MAX_VALUE / 4 ||
            !Double.isFinite(left) || !Double.isFinite(top) || !Double.isFinite(cropWidth) ||
            !Double.isFinite(cropHeight) || left < 0.0 || top < 0.0 ||
            cropWidth <= 0.0 || cropHeight <= 0.0 ||
            left + cropWidth > sourceWidth + 0.000001 || top + cropHeight > sourceHeight + 0.000001)
            throw new IllegalArgumentException("quality resize crop");
        int[] out = new int[width];
        if (left == 0.0 && top == 0.0 && cropWidth == sourceWidth && cropHeight == sourceHeight &&
            width == sourceWidth && height == sourceHeight) {
            for (int y = 0; y < height; y++) {
                if ((y & 15) == 0 && Thread.currentThread().isInterrupted())
                    throw new IllegalStateException("quality interrupted");
                source.readRow(y, out);
                sink.writeRow(y, out);
            }
            return;
        }
        Axis horizontal = new Axis(sourceWidth, width, left, cropWidth);
        Axis vertical = new Axis(sourceHeight, height, top, cropHeight);
        RowCache cache = new RowCache(source, sourceWidth, width, horizontal);
        float[] accum = new float[width * 4];
        float[] min = new float[width * 4];
        float[] max = new float[width * 4];
        for (int y = 0; y < height; y++) {
            if ((y & 7) == 0 && Thread.currentThread().isInterrupted())
                throw new IllegalStateException("quality interrupted");
            java.util.Arrays.fill(accum, 0f);
            java.util.Arrays.fill(min, Float.POSITIVE_INFINITY);
            java.util.Arrays.fill(max, Float.NEGATIVE_INFINITY);
            for (int t = vertical.offset[y]; t < vertical.offset[y+1]; t++) {
                float weight = vertical.weights[t];
                float[] row = cache.get(vertical.indices[t]);
                for (int i = 0; i < accum.length; i++) {
                    float value = row[i];
                    accum[i] += value * weight;
                    if (value < min[i]) min[i] = value;
                    if (value > max[i]) max[i] = value;
                }
            }
            for (int x = 0; x < width; x++) {
                int i = x * 4;
                float a = clampFloat(accum[i+3], min[i+3], max[i+3]);
                int alpha = clamp(Math.round(a), 0, 255);
                if (alpha == 0 || a < 0.00001f) { out[x] = 0; continue; }
                float inverseAlpha = 255f / a;
                int r = clamp(Math.round(clampFloat(accum[i], min[i], max[i]) * inverseAlpha), 0, 255);
                int g = clamp(Math.round(clampFloat(accum[i+1], min[i+1], max[i+1]) * inverseAlpha), 0, 255);
                int b = clamp(Math.round(clampFloat(accum[i+2], min[i+2], max[i+2]) * inverseAlpha), 0, 255);
                out[x] = (alpha << 24) | (r << 16) | (g << 8) | b;
            }
            sink.writeRow(y, out);
        }
    }

    private static final class Axis {
        final int[] offset;
        final int[] indices;
        final float[] weights;
        final boolean reduce;
        Axis(int source, int target, double start, double extent) {
            double ratio = extent / target;
            reduce = ratio > 1.0000001;
            double radius = 2.0 * Math.max(1.0, ratio);
            offset = new int[target + 1];
            long total = 0;
            for (int x = 0; x < target; x++) {
                double center = start + (x + 0.5) * ratio - 0.5;
                int first = Math.max(0, (int)Math.ceil(center - radius));
                int last = Math.min(source - 1, (int)Math.floor(center + radius));
                total += Math.max(1, last - first + 1);
                if (total > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("quality axis size");
                offset[x+1] = (int)total;
            }
            indices = new int[(int)total];
            weights = new float[(int)total];
            for (int x = 0; x < target; x++) {
                double center = start + (x + 0.5) * ratio - 0.5;
                int first = Math.max(0, (int)Math.ceil(center - radius));
                double sum = 0.0;
                for (int i = offset[x]; i < offset[x+1]; i++) {
                    int index = Math.min(source - 1, first + i - offset[x]);
                    indices[i] = index;
                    double distance = (index - center) / (reduce ? ratio : 1.0);
                    double value = reduce ? lanczos2(distance) : cubic(distance);
                    weights[i] = (float)value;
                    sum += value;
                }
                if (Math.abs(sum) < 0.000000001) {
                    java.util.Arrays.fill(weights, offset[x], offset[x+1], 0f);
                    weights[offset[x]] = 1f;
                } else {
                    for (int i = offset[x]; i < offset[x+1]; i++) weights[i] /= (float)sum;
                }
            }
        }
    }

    private static final class RowCache {
        final RowSource source;
        final Axis horizontal;
        final int width;
        final int[] raw;
        final int[] rowIds = new int[8];
        final long[] used = new long[8];
        final float[][] rows;
        long clock;
        RowCache(RowSource source, int sourceWidth, int width, Axis horizontal) {
            this.source = source;
            this.horizontal = horizontal;
            this.width = width;
            this.raw = new int[sourceWidth];
            this.rows = new float[8][width * 4];
            java.util.Arrays.fill(rowIds, -1);
        }
        float[] get(int row) {
            for (int i = 0; i < rowIds.length; i++) {
                if (rowIds[i] == row) { used[i] = ++clock; return rows[i]; }
            }
            int slot = 0;
            for (int i = 1; i < rowIds.length; i++) if (used[i] < used[slot]) slot = i;
            source.readRow(row, raw);
            float[] out = rows[slot];
            for (int x = 0; x < width; x++) {
                float r = 0f, g = 0f, b = 0f, a = 0f;
                float minR = Float.POSITIVE_INFINITY, minG = minR, minB = minR, minA = minR;
                float maxR = Float.NEGATIVE_INFINITY, maxG = maxR, maxB = maxR, maxA = maxR;
                for (int t = horizontal.offset[x]; t < horizontal.offset[x+1]; t++) {
                    int p = raw[horizontal.indices[t]];
                    float pa = p >>> 24;
                    float alphaScale = pa / 255f;
                    float pr = red(p) * alphaScale, pg = green(p) * alphaScale, pb = blue(p) * alphaScale;
                    float weight = horizontal.weights[t];
                    r += pr * weight; g += pg * weight; b += pb * weight; a += pa * weight;
                    minR = Math.min(minR, pr); maxR = Math.max(maxR, pr);
                    minG = Math.min(minG, pg); maxG = Math.max(maxG, pg);
                    minB = Math.min(minB, pb); maxB = Math.max(maxB, pb);
                    minA = Math.min(minA, pa); maxA = Math.max(maxA, pa);
                }
                int i = x * 4;
                out[i] = clampFloat(r, minR, maxR);
                out[i+1] = clampFloat(g, minG, maxG);
                out[i+2] = clampFloat(b, minB, maxB);
                out[i+3] = clampFloat(a, minA, maxA);
            }
            rowIds[slot] = row;
            used[slot] = ++clock;
            return out;
        }
    }

    private static double cubic(double x) {
        x = Math.abs(x);
        if (x < 1.0) return ((1.5*x - 2.5)*x)*x + 1.0;
        if (x < 2.0) return ((-0.5*x + 2.5)*x - 4.0)*x + 2.0;
        return 0.0;
    }
    private static double lanczos2(double x) {
        x = Math.abs(x);
        if (x < 0.00000001) return 1.0;
        if (x >= 2.0) return 0.0;
        double p = Math.PI * x;
        return Math.sin(p) * Math.sin(p/2.0) * 2.0 / (p*p);
    }
    private static int skinWeight(int p) {
        int r = red(p), g = green(p), b = blue(p);
        int rg = r-g, gb = g-b;
        if (r < 45 || g < 24 || b < 12 || rg < 2 || rg > 88 || gb < -8 || gb > 88) return 0;
        int chroma = Math.max(r, Math.max(g,b)) - Math.min(r, Math.min(g,b));
        if (chroma < 8 || chroma > 144) return 0;
        return clamp(Math.min(rg-1, 89-rg) * 20, 0, 256);
    }
    private static int median(int[] histogram, int count) {
        int target = (count + 1) / 2, sum = 0;
        for (int i = 0; i < histogram.length; i++) {
            sum += histogram[i];
            if (sum >= target) return i;
        }
        return 0;
    }
    public static int luma(int p) { return (77*red(p) + 150*green(p) + 29*blue(p) + 128) >> 8; }
    private static int red(int p) { return (p >>> 16) & 255; }
    private static int green(int p) { return (p >>> 8) & 255; }
    private static int blue(int p) { return p & 255; }
    private static int redGreen(int p) { return red(p)-green(p); }
    private static int blueGreen(int p) { return blue(p)-green(p); }
    private static int roundDiv(int n, int d) { return n >= 0 ? (n+d/2)/d : -((-n+d/2)/d); }
    private static int clamp(int x, int low, int high) { return Math.max(low, Math.min(high,x)); }
    private static float clampFloat(float x, float low, float high) { return Math.max(low, Math.min(high,x)); }
    private static float finiteClamp(float x, float low, float high, float fallback) {
        return Float.isNaN(x) || Float.isInfinite(x) ? fallback : clampFloat(x, low, high);
    }
    private static void checkPixels(int[] pixels, int width, int height) {
        if (pixels == null || width < 1 || height < 1 || (long)width*height > pixels.length)
            throw new IllegalArgumentException("quality pixels");
    }
    private static void checkStrip(int[] source, int[] output, int width, int rows, int begin, int end) {
        checkPixels(source, width, rows);
        checkPixels(output, width, rows);
        if (source == output || begin < 0 || end < begin || end > rows)
            throw new IllegalArgumentException("quality strip");
    }
}
