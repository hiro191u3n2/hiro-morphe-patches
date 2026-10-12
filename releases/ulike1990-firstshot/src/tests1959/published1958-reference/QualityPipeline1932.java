package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Source-size denoise/chroma -> crop/rotation/resample -> output-only sharpening.
 * The two existing save entry points call normalize(); their subsequent detail call
 * consumes an identity-bound completion record instead of applying the filters twice.
 * Encoder preparation, HEIF output, user options and camera lifecycle stay outside
 * this helper. A failed preparation returns to the original normalization/detail path.
 */
public final class QualityPipeline1932 {
    private QualityPipeline1932() {}

    private static final ThreadLocal<QualityPixels1932.Plan> CURRENT = new ThreadLocal<QualityPixels1932.Plan>();
    private static final ThreadLocal<Integer> ROW_ORIGIN = new ThreadLocal<Integer>();
    private static final ThreadLocal<PhotoDetail.Settings> LEGACY = new ThreadLocal<PhotoDetail.Settings>();
    private static final Map<ChromaPipeline186.State, QualityPixels1932.Plan> STATES =
        Collections.synchronizedMap(new WeakHashMap<ChromaPipeline186.State, QualityPixels1932.Plan>());
    // A State belongs to one shot. The explicit mapping keeps pool workers from
    // inheriting a trace left by another capture on the same thread.
    private static final Map<ChromaPipeline186.State, ProcessingTiming1947.Trace> STATE_TRACES =
        Collections.synchronizedMap(new WeakHashMap<ChromaPipeline186.State, ProcessingTiming1947.Trace>());
    private static final Map<Bitmap, Prepared> PREPARED =
        Collections.synchronizedMap(new WeakHashMap<Bitmap, Prepared>());
    private static final long RESERVE = 32L * 1024 * 1024;
    private static final ThreadLocal<CpuLayout1953[]> CPU_LAYOUT_1953 = new ThreadLocal<CpuLayout1953[]>();
    private static final IdentityHashMap<Bitmap,Frozen1954> FROZEN_1954=new IdentityHashMap<Bitmap,Frozen1954>();

    /** Only private pipeline-owned images enter this table. A recycle request
     * transfers disposal to its last proof lease; it never clones image data. */
    private static final class Frozen1954 {
        final Bitmap bitmap;int leases;boolean dispose;
        Frozen1954(Bitmap bitmap){this.bitmap=bitmap;}
    }
    private static Frozen1954 freeze1954(Bitmap bitmap) {
        synchronized(FROZEN_1954) {
            if(bitmap==null||bitmap.isRecycled())throw new IllegalStateException("missing frozen image");
            Frozen1954 lease=FROZEN_1954.get(bitmap);
            if(lease==null){lease=new Frozen1954(bitmap);FROZEN_1954.put(bitmap,lease);}
            lease.leases++;return lease;
        }
    }
    private static void thaw1954(Frozen1954 lease) {
        if(lease==null)return;
        synchronized(FROZEN_1954) {
            if(lease.leases<=0)throw new IllegalStateException("frozen image ownership");
            if(--lease.leases==0) {
                FROZEN_1954.remove(lease.bitmap);
                if(lease.dispose&&!lease.bitmap.isRecycled())lease.bitmap.recycle();
            }
        }
    }
    /** Audited pipeline/save recycle call sites use this exact ownership gate. */
    public static void recycle1954(Bitmap bitmap) {
        if(bitmap==null)return;
        synchronized(FROZEN_1954) {
            Frozen1954 lease=FROZEN_1954.get(bitmap);
            if(lease!=null&&lease.leases>0){lease.dispose=true;return;}
            if(!bitmap.isRecycled())bitmap.recycle();
        }
    }
    private static boolean frozen1954(Bitmap bitmap) {
        synchronized(FROZEN_1954){Frozen1954 lease=FROZEN_1954.get(bitmap);return lease!=null&&lease.leases>0;}
    }

    private static final class Prepared {
        final WeakReference<Bitmap> source;
        Prepared(Bitmap source) { this.source = new WeakReference<Bitmap>(source); }
    }

    /** Called only from the two final-save normalization sites. */
    public static Bitmap normalize(Bitmap input, int rotation, boolean fixed245) {
        Bitmap working = input;
        final ProcessingTiming1947.Trace trace = ProcessingTiming1947.traceFor(input);
        final ProcessingTiming1947.Scope timingScope = ProcessingTiming1947.enter(trace);
        QualityPixels1932.Plan previous = CURRENT.get();
        PhotoDetail.Settings oldLegacy = LEGACY.get();
        try {
            if (!readable(input)) return legacyNormalize(input, rotation, fixed245, trace);
            final PhotoDetail.Settings requested = AsyncSave1935.settings(PhotoDetail.snapshot1932());
            if (requested == null) return legacyNormalize(input, rotation, fixed245, trace);
            ProcessingTiming1947.settings(trace, "ノイズ低減:" + (requested.noiseOn ? requested.noiseLevel : 0)
                + "／くっきり補正:" + (requested.sharpOn ? requested.sharpLevel : 0)
                + "／質感保護:" + (requested.texturePriority ? "オン" : "オフ")
                + "／白フチ抑制:" + (requested.haloSuppression ? "オン" : "オフ")
                + "／暗部優先:" + (requested.shadowPriority ? "オン" : "オフ"));
            final int turn = normalizeRotation(rotation);
            final int[] size = SaveQuality2.output186(input.getWidth(), input.getHeight(), turn, fixed245);
            final int rw = turn == 90 || turn == 270 ? input.getHeight() : input.getWidth();
            final int rh = turn == 90 || turn == 270 ? input.getWidth() : input.getHeight();
            final float scale = Math.max((float)size[0] / rw, (float)size[1] / rh);
            final boolean reduceFirst = scale < 0.999999f;
            final ShotContext1932.Snapshot shot = ShotContext1932.forBitmap(input);
            final boolean chroma = AsyncSave1935.chroma(ChromaPipeline177.enabled1932());
            // These analyses have no consumer when both user filters are OFF.
            // Chroma/moire still run independently with the same corrected pixels.
            final boolean needsNoise = requested.noiseOn && requested.noiseLevel > 0;
            final boolean needsSharp = requested.sharpOn && requested.sharpLevel > 0;
            FaceRegions1934.Mask faceRegions = null;
            if (needsNoise || (needsSharp && requested.texturePriority)) {
                ProcessingTiming1947.Token preparation = ProcessingTiming1947.beginStage(trace, ProcessingTiming1947.CORRECTION);
                try { faceRegions = FaceRegions1934.forBitmap(input,turn); }
                finally { ProcessingTiming1947.end(preparation); }
            }

            // Anti-aliased reduction first bounds work for very large sensor images.
            // Enlargement is always deferred until source-domain cleanup is finished.
            if (reduceFirst) {
                working = timedResize(input, turn, size[0], size[1], trace);
                if (faceRegions != null) faceRegions = timedFaceResize(faceRegions, turn, size[0], size[1], trace);
            }
            SpatialNoise1934 localNoise = needsNoise || needsSharp ? probeSpatial(working) : null;
            if (!needsNoise && !needsSharp) ProcessingTiming1947.skip(trace, ProcessingTiming1947.NOISE);
            QualityPixels1932.NoiseStats stats = localNoise == null ? null : localNoise.global;
            QualityPixels1932.Plan plan;
            PhotoDetail.Settings sourceSettings;
            ProcessingTiming1947.Token preparation = ProcessingTiming1947.beginStage(trace, ProcessingTiming1947.CORRECTION);
            try {
            plan = QualityPixels1932.plan(stats,
                shot.metadataReliable ? shot.iso : 0,
                shot.metadataReliable ? shot.exposureNanos : 0,
                lensKind(shot), shot.beautyStrength,
                requested.noiseOn ? requested.noiseLevel : 0,
                requested.sharpOn ? requested.sharpLevel : 0,
                requested.texturePriority, requested.shadowPriority,
                reduceFirst ? 1f : scale).withHaloSuppression(requested.haloSuppression)
                .withLocalNoise(localNoise, requested.noiseOn ? requested.noiseLevel : 0)
                .withFaceRegions(faceRegions);
            sourceSettings = new PhotoDetail.Settings(
                false, Math.max(1, plan.noiseLevel), false, 1,
                requested.texturePriority, requested.haloSuppression, requested.shadowPriority);
            } finally { ProcessingTiming1947.end(preparation); }
            CURRENT.set(plan);
            LEGACY.set(sourceSettings);

            // The legacy fallback reads its settings through settingsForLegacy().
            // Passing the same Bitmap twice keeps this helper in charge of ownership.
            Bitmap filtered = ChromaPipeline177.apply(working, working, sourceSettings);
            ProcessingTiming1947.transfer(working, filtered);
            if (filtered != null && filtered != working) {
                Bitmap old = working;
                working = filtered;
                recycleOwned(old, input);
            }
            // NR1-NR4 use one already beauty/colour-corrected image. Legacy
            // primary and residual NR were disabled in sourceSettings above;
            // this replacement also runs when the colour correction is OFF.
            SmoothRegions1958 smoothedRegions = null;
            if (needsNoise) {
                working = mutableOwned(working, input);
                singleNoiseInPlace1955(working, plan, requested.noiseLevel,
                    requested.shadowPriority);
                // NR9-NR12 share coherent structure and region evidence. NR13
                // records the actually admitted flat regions before strip writes.
                smoothedRegions = smoothNoiseInPlace1958(working, plan, requested.noiseLevel,
                    requested.shadowPriority);
                ProcessingTiming1947.note(trace, "単写ノイズ低減 NR9〜NR13");
            }
            // Refresh the evidence after correction and NR, retaining the
            // conservative original floor so residual grain is not re-sharpened.
            if (needsNoise && needsSharp) localNoise = probeSpatial(working);
            // Only a geometry no-op can join the formerly separate moire and
            // sharpening passes. Sharpening must see the moire-corrected neighbours.
            final boolean joinedFinish = chroma && plan.sharpLevel > 0 && !reduceFirst
                && turn == 0 && size[0] == working.getWidth() && size[1] == working.getHeight();
            if (joinedFinish) {
                QualityPixels1932.Plan sharpPlan = plan;
                if (faceRegions != null)
                    sharpPlan=sharpPlan.withFaceRegions(timedFaceResize(faceRegions,turn,size[0],size[1],trace));
                sharpPlan=sharpPlan.withOutputNoise(localNoise).withSmoothedRegions(smoothedRegions);
                working = mutableOwned(working, input);
                working = finishOwned1953(working,input,plan,sharpPlan,true,true,true);
            } else if (chroma) {
                working = mutableOwned(working, input);
                working = finishOwned1953(working,input,plan,null,true,false,false);
            }
            if (!reduceFirst) {
                Bitmap resized = timedResize(working, turn, size[0], size[1], trace);
                if (resized != working) {
                    Bitmap old = working;
                    working = resized;
                    recycleOwned(old, input);
                }
            }
            if (plan.sharpLevel > 0 && !joinedFinish) {
                if (!reduceFirst) {
                    if (faceRegions != null) faceRegions=timedFaceResize(faceRegions, turn, size[0], size[1], trace);
                    plan=plan.withFaceRegions(faceRegions);
                }
                // A source noise grid cannot be sampled at final-save coordinates
                // after crop, rotation or resize. Refresh only that bounded map;
                // retain the source plan's gain/floor and all captured user options.
                boolean moved = turn != 0 || size[0] != input.getWidth() || size[1] != input.getHeight();
                plan = plan.withOutputNoise((moved || needsNoise) ? probeSpatial(working) : localNoise)
                    .withSmoothedRegions(smoothedRegions == null ? null :
                        smoothedRegions.outputMask(reduceFirst ? 0 : turn,size[0],size[1]));
                working = mutableOwned(working, input);
                working = finishOwned1953(working,input,plan,null,false,true,false);
            }
            if (working.getWidth() != size[0] || working.getHeight() != size[1])
                throw new IllegalStateException("quality output dimensions");
            ShotContext1932.copy(input, working);
            ProcessingTiming1947.transfer(input, working);
            ProcessingTiming1947.output(trace, working.getWidth(), working.getHeight(), "画質補正");
            PREPARED.put(working, new Prepared(input));
            return working;
        } catch (RuntimeException failure) {
            recycleOwned(working, input);
            if(Thread.currentThread().isInterrupted() || failure instanceof java.util.concurrent.CancellationException)
                throw failure;
            return legacyNormalize(input, rotation, fixed245, trace);
        } catch (OutOfMemoryError failure) {
            SpeedWorkers1935.trim();
            recycleOwned(working, input);
            return legacyNormalize(input, rotation, fixed245, trace);
        } finally {
            restore(CURRENT, previous);
            restore(LEGACY, oldLegacy);
            ProcessingTiming1947.restore(timingScope);
        }
    }

    /** The old path has mixed stages; label its durations unknown rather than
     * claiming that a partially measured failed attempt is the full operation. */
    private static Bitmap legacyNormalize(Bitmap input, int rotation, boolean fixed245,
            ProcessingTiming1947.Trace trace) {
        ProcessingTiming1947.unmeasured(trace, ProcessingTiming1947.NOISE);
        ProcessingTiming1947.unmeasured(trace, ProcessingTiming1947.CORRECTION);
        ProcessingTiming1947.note(trace, "従来経路へ切替");
        Bitmap output = SaveQuality2.normalize186(input, rotation, fixed245);
        ProcessingTiming1947.transfer(input, output);
        return output;
    }

    private static Bitmap timedResize(Bitmap input, int turn, int width, int height,
            ProcessingTiming1947.Trace trace) {
        // A dimensions-preserving no-op is not a measured pixel stage.
        if (turn == 0 && input.getWidth() == width && input.getHeight() == height) {
            Bitmap result = FastResize1933.resample(input, turn, width, height);
            ProcessingTiming1947.transfer(input, result);
            return result;
        }
        ProcessingTiming1947.Token timing = ProcessingTiming1947.beginStage(trace, ProcessingTiming1947.CORRECTION);
        try {
            Bitmap result = FastResize1933.resample(input, turn, width, height);
            ProcessingTiming1947.transfer(input, result);
            return result;
        } finally { ProcessingTiming1947.end(timing); }
    }

    private static FaceRegions1934.Mask timedFaceResize(FaceRegions1934.Mask mask, int turn,
            int width, int height, ProcessingTiming1947.Trace trace) {
        ProcessingTiming1947.Token timing = ProcessingTiming1947.beginStage(trace, ProcessingTiming1947.CORRECTION);
        try { return mask.resample(turn, width, height); }
        finally { ProcessingTiming1947.end(timing); }
    }

    /** Receives the already-existing PhotoDetail settings snapshot. */
    public static Bitmap applyDetail(Bitmap bitmap, Bitmap original, PhotoDetail.Settings settings) {
        Prepared prepared = PREPARED.remove(bitmap);
        if (prepared != null && original != null && prepared.source.get() == original) return bitmap;
        // Non-save callers and preparation fallbacks retain the established behavior.
        ProcessingTiming1947.Trace trace = ProcessingTiming1947.traceFor(bitmap);
        ProcessingTiming1947.Scope scope = ProcessingTiming1947.enter(trace);
        try {
            Bitmap result = ChromaPipeline177.apply(bitmap, original, settings);
            ProcessingTiming1947.transfer(bitmap, result);
            return result;
        } finally { ProcessingTiming1947.restore(scope); }
    }

    public static PhotoDetail.Settings settingsForLegacy(PhotoDetail.Settings original) {
        PhotoDetail.Settings override = LEGACY.get();
        return override == null ? AsyncSave1935.settings(original) : override;
    }

    /** Added after State's existing constructor has completed its field writes. */
    public static void stateCreated(ChromaPipeline186.State state) {
        QualityPixels1932.Plan plan = CURRENT.get();
        if (state != null && plan != null) STATES.put(state, plan);
        ProcessingTiming1947.Trace trace = ProcessingTiming1947.traceFor(null);
        if (state != null && trace != null) {
            try { STATE_TRACES.put(state, trace); }
            catch (Throwable ignored) {
                ProcessingTiming1947.unmeasured(trace, ProcessingTiming1947.NOISE);
                ProcessingTiming1947.unmeasured(trace, ProcessingTiming1947.CORRECTION);
            }
        }
    }

    /** Same scheduler and processed-halo ownership as ShadowDetail1923. The local
     * NR mix runs before chroma/tone correction, never restoring pre-correction RGB. */
    public static void run(ChromaPipeline186.State state, ChromaPipeline186.Buffer buffer) {
        QualityPixels1932.Plan old = CURRENT.get();
        Integer oldOrigin = ROW_ORIGIN.get();
        QualityPixels1932.Plan plan = STATES.get(state);
        ProcessingTiming1947.Trace stateTrace = null;
        try { stateTrace = STATE_TRACES.get(state); } catch (Throwable ignored) {}
        final ProcessingTiming1947.Trace trace = stateTrace;
        final ProcessingTiming1947.Scope timingScope = ProcessingTiming1947.enter(trace);
        if (plan == null) CURRENT.remove(); else CURRENT.set(plan);
        boolean cpu = false;
        try {
            cpu = SpeedWorkers1935.enterLegacy();
            while(!state.failed && !Thread.currentThread().isInterrupted()) {
                int first=state.read(buffer); if(first<0)return;
                int count=Math.min(state.core,state.height-first),top=Math.max(0,first-state.radius-7);
                int rows=Math.min(state.height,first+count+state.radius+7)-top,start=first-top;
                // Production Chroma179.radius() is at least 12, so its existing
                // radius+7 source halo covers the primary NR's six-row reach and
                // the residual support below. Bound unusual/small-radius
                // callers too: extra+6 must never exceed the loaded source halo.
                int extra=state.noise>0?Math.min(QualityShadow1932.RESIDUAL_RADIUS,Math.max(1,state.radius+1)):0;
                int lo=Math.max(0,start-extra),hi=Math.min(rows,start+count+extra);
                ROW_ORIGIN.set(top);
                PolicyCache1945 policy=state.noise>0?PolicyCache1945.borrow(plan,state.width,top+lo,top+hi):null;
                try {
                QualityPixels1932.Plan stripPlan=policy==null?plan:plan.withPolicyCache(policy);
                if(stripPlan==null)CURRENT.remove();else CURRENT.set(stripPlan);
                DetailPixels.Work w=buffer.pixels; int[] result;
                if(state.noise>0 || state.sharp>0) {
                    ProcessingTiming1947.Token denoise = ProcessingTiming1947.beginStage(trace, ProcessingTiming1947.NOISE);
                    try {
                        DetailSerial186.filter(w,state.width,rows,lo,hi-lo,state.noise,state.sharp,
                            state.texture,state.halos,state.shadows);
                        if(state.noise>0 && state.sharp==0 && plan!=null)
                            QualityPixels1932.localDenoiseMix(w.source,w.output,state.width,rows,lo,hi,top,stripPlan);
                    } finally { ProcessingTiming1947.end(denoise); }
                    ProcessingTiming1947.Token correction = ProcessingTiming1947.beginStage(trace, ProcessingTiming1947.CORRECTION);
                    try {
                        // Both correction kernels initialize each consumed pixel before reading it.
                        // The focused API leaves unconsumed scratch halo rows untouched.
                        Chroma186.finishConsumed1950(w.source,w.output,w.horizontal,state.width,rows,lo,hi,
                            state.radius,buffer.columns,buffer.covariance,state.nativeAllowed);
                    } finally { ProcessingTiming1947.end(correction); }
                    result=w.horizontal;
                } else {
                    ProcessingTiming1947.Token correction = ProcessingTiming1947.beginStage(trace, ProcessingTiming1947.CORRECTION);
                    try {
                        Chroma186.finishConsumed1950(w.source,w.source,w.output,state.width,rows,start,start+count,
                            state.radius,buffer.columns,buffer.covariance,state.nativeAllowed);
                    } finally { ProcessingTiming1947.end(correction); }
                    result=w.output;
                }
                if(extra>0) {
                    System.arraycopy(result,lo*state.width,w.denoised,lo*state.width,(hi-lo)*state.width);
                    // The residual cleanup sees the same shot plan as the primary
                    // NR. Pass it explicitly: nested CPU workers must not infer
                    // face/noise budgets from an unrelated thread-local capture.
                    ProcessingTiming1947.Token residual = ProcessingTiming1947.beginStage(trace, ProcessingTiming1947.NOISE);
                    try {
                        QualityShadow1932.smoothSavedRange1951(w.denoised,result,state.width,rows,start,start+count,
                            lo,hi,state.noise,state.shadows,extra,stripPlan,top);
                    } finally { ProcessingTiming1947.end(residual); }
                }
                if(Thread.currentThread().isInterrupted()){state.failed=true;return;}
                state.write(result,start,first,count);
                } finally {
                    if(plan==null)CURRENT.remove();else CURRENT.set(plan);
                    if(policy!=null)policy.close();
                }
            }
            state.failed=true;
        } catch(Throwable error) {
            state.failed=true;
            ProcessingTiming1947.unmeasured(trace, ProcessingTiming1947.NOISE);
            ProcessingTiming1947.unmeasured(trace, ProcessingTiming1947.CORRECTION);
        }
        finally {
            restore(CURRENT, old); restore(ROW_ORIGIN, oldOrigin);
            SpeedWorkers1935.leaveLegacy(cpu);
            ProcessingTiming1947.restore(timingScope);
        }
    }

    public static int shadowBudgetQ8(int pixel, int localRange) {
        return QualityPixels1932.shadowBudgetQ8(pixel, localRange, CURRENT.get());
    }
    public static int shadowBudgetQ8(int pixel,int localRange,int x,int stripRow) {
        QualityPixels1932.Plan plan=CURRENT.get();
        Integer origin=ROW_ORIGIN.get();
        int absoluteY=origin==null?-1:stripRow+origin.intValue();
        int result=QualityPixels1932.shadowBudgetQ8(pixel,localRange,plan,x,absoluteY);
        if(plan!=null && plan.faceRegions!=null && absoluteY>=0)
            result=result*(256-(plan.detailAt(x,absoluteY)*208>>8))>>8;
        if(plan!=null && plan.localNoise!=null && origin!=null)
            result=result*plan.localNoiseBudgetAt(x,stripRow+origin.intValue())>>8;
        return result;
    }

    private static <T> void restore(ThreadLocal<T> local, T value) {
        if (value == null) local.remove(); else local.set(value);
    }

    private static int lensKind(ShotContext1932.Snapshot shot) {
        if (!shot.metadataReliable) return QualityPixels1932.LENS_UNKNOWN;
        if (shot.lensKind == ShotContext1932.LENS_FRONT) return QualityPixels1932.LENS_FRONT;
        if (shot.lensKind == ShotContext1932.LENS_BACK) return QualityPixels1932.LENS_WIDE;
        if (shot.lensKind == ShotContext1932.LENS_TELEPHOTO) return QualityPixels1932.LENS_TELE;
        if (shot.lensKind == ShotContext1932.LENS_ULTRAWIDE) return QualityPixels1932.LENS_ULTRAWIDE;
        return QualityPixels1932.LENS_UNKNOWN;
    }

    static int normalizeRotation(int rotation) {
        if (rotation == -1) return 0;
        int r = (rotation % 360 + 360) % 360;
        if (r <= 45 || r >= 315) return 0;
        if (r <= 135) return 90;
        if (r <= 225) return 180;
        return 270;
    }

    private static boolean readable(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return false;
        Bitmap.Config config = bitmap.getConfig();
        if (config != Bitmap.Config.ARGB_8888 && config != Bitmap.Config.RGB_565) return false;
        ColorSpace space = bitmap.getColorSpace();
        if (space != null && !space.isSrgb()) return false;
        return Build.VERSION.SDK_INT < 34 || !bitmap.hasGainmap();
    }

    private static SpatialNoise1934 probeSpatial(final Bitmap bitmap) {
        ProcessingTiming1947.Token timing = ProcessingTiming1947.beginStage(
            ProcessingTiming1947.traceFor(bitmap), ProcessingTiming1947.NOISE);
        try {
            return SpatialNoise1934.probe(new SpatialNoise1934.Patches() {
                public void read(int[] pixels,int x,int y,int width,int height) {
                    bitmap.getPixels(pixels,0,width,x,y,width,height);
                }
            },bitmap.getWidth(),bitmap.getHeight());
        } finally { ProcessingTiming1947.end(timing); }
    }

    private static Bitmap mutableOwned(Bitmap bitmap, Bitmap original) {
        if (bitmap != original && bitmap.isMutable() && bitmap.getConfig() == Bitmap.Config.ARGB_8888
                &&!frozen1954(bitmap))
            return bitmap;
        ProcessingTiming1947.Token timing = ProcessingTiming1947.beginStage(
            ProcessingTiming1947.traceFor(bitmap), ProcessingTiming1947.CORRECTION);
        try {
            requireMemory((long)bitmap.getWidth() * bitmap.getHeight() * 4);
            Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            if (copy == null) throw new IllegalStateException("quality bitmap copy");
            try {
                ShotContext1932.copy(bitmap, copy);
                ProcessingTiming1947.transfer(bitmap, copy);
                recycleOwned(bitmap, original);
                return copy;
            } catch (RuntimeException error) { copy.recycle(); throw error; }
              catch (OutOfMemoryError error) { copy.recycle(); throw error; }
        } finally { ProcessingTiming1947.end(timing); }
    }

    static Bitmap resample(final Bitmap bitmap, final int rotation, final int width, final int height) {
        final int sourceWidth = bitmap.getWidth(), sourceHeight = bitmap.getHeight();
        final int rw = rotation == 90 || rotation == 270 ? sourceHeight : sourceWidth;
        final int rh = rotation == 90 || rotation == 270 ? sourceWidth : sourceHeight;
        if (rotation == 0 && rw == width && rh == height) return bitmap;
        requireMemory((long)width * height * 4 + (long)Math.max(rw, width) * 256);
        final Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] allocatedBatch = null;
        final double scale = Math.max((double)width / rw, (double)height / rh);
        final double cw = Math.min((double)rw, width / scale);
        final double ch = Math.min((double)rh, height / scale);
        try {
            final int batchRows = Math.min(16,height);
            final int[] batch = allocatedBatch = SpeedWorkers1935.borrowInts(width * batchRows);
            result.setDensity(bitmap.getDensity());
            result.setHasAlpha(bitmap.hasAlpha());
            QualityPixels1932.resizeCrop(new QualityPixels1932.RowSource() {
                public void readRow(int row, int[] pixels) {
                    if (rotation == 90) {
                        bitmap.getPixels(pixels, 0, 1, row, 0, 1, sourceHeight);
                        reverse(pixels, rw);
                    } else if (rotation == 180) {
                        bitmap.getPixels(pixels, 0, rw, 0, sourceHeight - 1 - row, rw, 1);
                        reverse(pixels, rw);
                    } else if (rotation == 270) {
                        bitmap.getPixels(pixels, 0, 1, sourceWidth - 1 - row, 0, 1, sourceHeight);
                    } else bitmap.getPixels(pixels, 0, rw, 0, row, rw, 1);
                }
            }, rw, rh, new QualityPixels1932.RowSink() {
                public void writeRow(int row, int[] pixels) {
                    int offset = row % batchRows;
                    System.arraycopy(pixels,0,batch,offset*width,width);
                    if(offset+1==batchRows || row+1==height)
                        result.setPixels(batch,0,width,0,row-offset,width,offset+1);
                }
            }, width, height, Math.max(0, (rw - cw) * 0.5), Math.max(0, (rh - ch) * 0.5), cw, ch);
            ProcessingTiming1947.transfer(bitmap, result);
            return result;
        } catch (RuntimeException error) { result.recycle(); throw error; }
          catch (OutOfMemoryError error) { result.recycle(); SpeedWorkers1935.trim(); throw error; }
          finally { SpeedWorkers1935.release(allocatedBatch); }
    }

    private static void reverse(int[] pixels, int count) {
        for (int left = 0, right = count - 1; left < right; left++, right--) {
            int p = pixels[left]; pixels[left] = pixels[right]; pixels[right] = p;
        }
    }

    private static long availableMemory() {
        Runtime runtime = Runtime.getRuntime();
        long available=runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
        long retained=WholeRoute1953.retainedBytes(),gpu=GpuFinish1953.retainedBytes();
        if(gpu>Long.MAX_VALUE-retained)return 0;
        retained+=gpu;
        long scratch=SpeedWorkers1935.nativeRetainedBytes1956();
        if(scratch>Long.MAX_VALUE-retained)return 0;
        retained+=scratch;
        return available>retained?available-retained:0;
    }

    private static void requireMemory(long bytes) {
        if (bytes < 0 || bytes > availableMemory() - RESERVE) {
            SpeedWorkers1935.trim();
            if (bytes < 0 || bytes > availableMemory() - RESERVE)
                throw new OutOfMemoryError("bounded quality workspace");
        }
    }

    private static void recycleOwned(Bitmap bitmap, Bitmap original) {
        if (bitmap != null && bitmap != original) recycle1954(bitmap);
    }

    /** Sequential reads preserve original top halos even when worker writes finish out of order. */
    private static final class Strips {
        final Bitmap bitmap;
        final int width, height, halo, core;
        final int[] preceding;
        int next, precedingRows;
        volatile Throwable failure;
        Strips(Bitmap bitmap,int core) {this(bitmap,core,QualityPixels1932.HALO);}
        Strips(Bitmap bitmap,int core,int dependencyHalo) {
            this.bitmap = bitmap; width = bitmap.getWidth(); height = bitmap.getHeight(); this.core=core;
            halo = dependencyHalo; preceding = SpeedWorkers1935.borrowInts(width * halo);
        }
        synchronized int read(Worker worker) {
            if (failure != null || next >= height) return -1;
            int first = next, count = Math.min(core, height - first);
            int top = Math.max(0, first - halo), bottom = Math.min(height, first + count + halo);
            int rows = bottom - top, start = first - top;
            // The preserved top halo is copied below, so reading those same rows
            // again from the (possibly already written) Bitmap is redundant.
            bitmap.getPixels(worker.input,start*width,width,0,first,width,rows-start);
            if (start > 0) {
                if (precedingRows != start) throw new IllegalStateException("quality halo sequence");
                System.arraycopy(preceding, 0, worker.input, 0, start * width);
            }
            precedingRows = Math.min(halo, first + count);
            System.arraycopy(worker.input, (start + count - precedingRows) * width,
                preceding, 0, precedingRows * width);
            next += count;
            worker.first = first; worker.rows = rows; worker.start = start; worker.count = count;
            return first;
        }
        synchronized void write(Worker worker) {
            if (failure == null)
                bitmap.setPixels(worker.output, worker.start * width, width,
                    0, worker.first, width, worker.count);
        }
        synchronized void fail(Throwable error) { if (failure == null) failure = error; }
    }

    private static final class Worker implements Runnable {
        final Strips strips;
        final QualityPixels1932.Plan plan;
        final boolean moire, sharp;
        final SingleNoise1955.Model singleModel;
        final SingleNoise1955.Protection singleProtection;
        final StrongNoise1958.Model strongModel;
        final StrongNoise1958.Protection strongProtection;
        final SmoothRegions1958 smoothRegions;
        final int singleLevel;
        final boolean singleShadows;
        final int[] input, output;
        int first, rows, start, count;
        Worker(Strips strips, QualityPixels1932.Plan plan, boolean moire, boolean sharp) {
            this(strips, plan, moire, sharp, null, null, null, 0, false);
        }
        Worker(Strips strips, QualityPixels1932.Plan plan, SingleNoise1955.Model model,
                int level, boolean shadows) {
            this(strips, plan, false, false, model, null, null, level, shadows);
        }
        Worker(Strips strips, QualityPixels1932.Plan plan, StrongNoise1958.Model model,
                SmoothRegions1958 smoothRegions,int level, boolean shadows) {
            this(strips, plan, false, false, null, model, smoothRegions, level, shadows);
        }
        private Worker(Strips strips, final QualityPixels1932.Plan plan, boolean moire,
                boolean sharp, SingleNoise1955.Model model, StrongNoise1958.Model strong,
                SmoothRegions1958 smoothRegions,int level, boolean shadows) {
            this.strips = strips; this.plan = plan; this.moire = moire; this.sharp = sharp;
            this.singleModel = model; this.singleLevel = level; this.singleShadows = shadows;
            this.strongModel = strong;
            this.smoothRegions = smoothRegions;
            this.singleProtection = model == null ? null : new SingleNoise1955.Protection() {
                public int budgetQ8(int x, int y) {
                    if (plan == null) return 256;
                    int skin = plan.skinAt(x,y);
                    return plan.shadowBudgetQ8 * (256 - ((skin * plan.beautyQ8) >> 9)) >> 8;
                }
                public int detailQ8(int x, int y) {
                    return plan == null ? 0 : plan.detailAt(x,y);
                }
            };
            this.strongProtection = strong == null ? null : new StrongNoise1958.Protection() {
                public int budgetQ8(int x, int y) {
                    // Scale-local evidence controls the strong pass. Applying
                    // the original global noise budget again would suppress
                    // coarse shadow cleanup on low-fine-noise photographs.
                    return plan == null ? 256 : 256 - ((plan.skinAt(x,y) * plan.beautyQ8) >> 9);
                }
                public int detailQ8(int x, int y) {
                    return plan == null ? 0 : plan.detailAt(x,y);
                }
            };
            int size = strips.width * Math.min(strips.height, strips.core + 2 * strips.halo);
            int[] first = SpeedWorkers1935.borrowInts(size);
            try { output = SpeedWorkers1935.borrowInts(size); }
            catch (Throwable failure) { SpeedWorkers1935.release(first); throw failure; }
            input = first;
        }
        public void run() {
            try {
                while (strips.failure == null && !Thread.currentThread().isInterrupted() && strips.read(this) >= 0) {
                    if (strongModel != null) {
                        if(smoothRegions!=null)smoothRegions.record(input,strips.width,rows,start,start+count,
                            first-start,strongModel,strongProtection);
                        StrongNoise1958.processRange(input, output, strips.width, rows,
                            start, start + count, 0, rows, first-start,
                            singleLevel, singleShadows, strongModel, strongProtection);
                    } else if (singleModel != null)
                        SingleNoise1955.processRange(input, output, strips.width, rows,
                            start, start + count, 0, rows, first-start,
                            singleLevel, singleShadows, singleModel, singleProtection);
                    else QualityPixels1932.finishStripAt(input, output, strips.width, rows,
                        start, start + count, plan, moire, sharp, first-start);
                    strips.write(this);
                }
                if (Thread.currentThread().isInterrupted()) strips.fail(new IllegalStateException("quality interrupted"));
            } catch (Throwable error) { strips.fail(error); }
        }
    }


    /** One byte per 4x4 block, populated from immutable strong-pass input. Only
     * the worker owning a block's first source row writes that cell. Publication
     * follows the joined worker barrier; no model/bitmap is retained afterwards. */
    private static final class SmoothRegions1958 implements QualityPixels1932.SmoothMask {
        final int width,height,columns,rows;
        final byte[] confidence;
        SmoothRegions1958(int width,int height) {
            this.width=width;this.height=height;columns=(width+3)/4;rows=(height+3)/4;
            confidence=new byte[Math.multiplyExact(columns,rows)];
        }
        static long memoryBytes(int width,int height) {return ((long)width+3)/4*(((long)height+3)/4);}
        void record(int[] input,int width,int stripRows,int begin,int end,int origin,
                StrongNoise1958.Model model,StrongNoise1958.Protection protection) {
            float[] flatScratch=new float[2];
            for(int row=begin;row<end;row++) {
                int y=row+origin;
                if((y&3)!=0)continue;
                if(Thread.currentThread().isInterrupted())throw new IllegalStateException("smooth mask interrupted");
                for(int x=0;x<width;x+=4) {
                    int xx=Math.min(width-1,x+1),rr=Math.min(stripRows-1,row+1);
                    int value=model.smoothingQ8(input,width,stripRows,xx,rr,0,stripRows,origin,flatScratch);
                    int protectionMinimum=256;
                    for(int dy=0;dy<4 && y+dy<height;dy++)for(int dx=0;dx<4 && x+dx<width;dx++) {
                        int r=row+dy,c=x+dx;
                        if(r>=stripRows || (input[r*width+c]>>>24)!=255){protectionMinimum=0;continue;}
                        if(protection!=null) {
                            int budget=Math.max(0,Math.min(256,protection.budgetQ8(c,y+dy)));
                            budget=budget*(256-Math.max(0,Math.min(256,protection.detailQ8(c,y+dy))))>>8;
                            protectionMinimum=Math.min(protectionMinimum,budget);
                        }
                    }
                    // NR9's neighbourhood guide covers the whole 4x4 cell. Any
                    // protected face contour or nonopaque cell pixel vetoes it.
                    int q=Math.min(255,value*protectionMinimum>>8);
                    confidence[(y/4)*columns+x/4]=(byte)q;
                }
            }
        }
        public int smoothingQ8(int x,int y) {return sample(x,y);}
        int sample(double x,double y) {
            if(x<0 || y<0 || x>width-1 || y>height-1)return 0;
            double gx=Math.max(0,Math.min(columns-1,(x-1.5)/4));
            double gy=Math.max(0,Math.min(rows-1,(y-1.5)/4));
            int ix=(int)gx,iy=(int)gy,nx=Math.min(columns-1,ix+1),ny=Math.min(rows-1,iy+1);
            double fx=gx-ix,fy=gy-iy;
            double a=(confidence[iy*columns+ix]&255)*(1-fx)+(confidence[iy*columns+nx]&255)*fx;
            double b=(confidence[ny*columns+ix]&255)*(1-fx)+(confidence[ny*columns+nx]&255)*fx;
            return (int)Math.round(a*(1-fy)+b*fy);
        }
        QualityPixels1932.SmoothMask outputMask(final int turn,final int outputWidth,final int outputHeight) {
            if(turn==0 && width==outputWidth && height==outputHeight)return this;
            final int rw=turn==90 || turn==270?height:width;
            final int rh=turn==90 || turn==270?width:height;
            final double scale=Math.max((double)outputWidth/rw,(double)outputHeight/rh);
            final double cw=Math.min((double)rw,outputWidth/scale),ch=Math.min((double)rh,outputHeight/scale);
            final double left=Math.max(0,(rw-cw)*.5),top=Math.max(0,(rh-ch)*.5);
            return new QualityPixels1932.SmoothMask() {
                public int smoothingQ8(int x,int y) {
                    if(x<0 || y<0 || x>=outputWidth || y>=outputHeight)return 0;
                    // Same pixel-centre transform and centre crop as resample().
                    double rx=left+(x+.5)*cw/outputWidth-.5;
                    double ry=top+(y+.5)*ch/outputHeight-.5;
                    if(turn==90)return sample(ry,height-1-rx);
                    if(turn==180)return sample(width-1-rx,height-1-ry);
                    if(turn==270)return sample(width-1-ry,rx);
                    return sample(rx,ry);
                }
            };
        }
    }

    /** Streaming single-frame replacement. Writes only a private saved bitmap;
     * an interrupted/failed preparation is discarded by normalize's rollback. */
    private static void singleNoiseInPlace1955(final Bitmap bitmap,
            QualityPixels1932.Plan plan, int level, boolean shadows) {
        ProcessingTiming1947.Token timing = ProcessingTiming1947.beginStage(
            ProcessingTiming1947.traceFor(bitmap), ProcessingTiming1947.NOISE);
        try {
            final SingleNoise1955.Model model = SingleNoise1955.probe(new SingleNoise1955.Patches() {
                public void read(int[] pixels,int x,int y,int width,int height) {
                    bitmap.getPixels(pixels,0,width,x,y,width,height);
                }
            },bitmap.getWidth(),bitmap.getHeight());
            final long budget = Math.max(0,availableMemory()-RESERVE);
            final int upper = SpeedWorkers1935.maxWorkers();
            final int halo = SingleNoise1955.HALO;
            // The transform accumulator and JNI transaction are additional to
            // the two strip arrays. Keep native admission <=256 owned rows and
            // reduce strip size before giving up on a low-memory single shot.
            final long shared = (long)bitmap.getWidth()*halo*4;
            int selected = Math.min(256,Scheduling1944.coreRows(
                bitmap.getWidth(),bitmap.getHeight(),halo,upper,budget));
            long perWorker;
            while (true) {
                perWorker = (long)bitmap.getWidth()*Math.min(bitmap.getHeight(),selected+2*halo)*8
                    + SingleNoise1955.workspaceBytes(bitmap.getWidth(),Math.min(bitmap.getHeight(),selected));
                if (perWorker <= Math.max(0,budget-shared) || selected <= 16) break;
                selected = Math.max(16,selected/2);
            }
            final int core = selected;
            final long bytes = perWorker;
            final int maximum = Math.max(1,(int)Math.min(SpeedWorkers1935.availableWorkers1944(),
                Math.min(upper,Math.max(0,budget-shared)/Math.max(1,bytes))));
            final int count = Math.max(1,Math.min(maximum,(bitmap.getHeight()+core-1)/core));
            requireMemory(bytes*count+shared);
            final Strips strips = new Strips(bitmap,core,halo);
            final Worker[] workers = new Worker[count];
            try {
                for(int i=0;i<count;i++) workers[i]=new Worker(strips,plan,model,level,shadows);
                SpeedWorkers1935.run(workers);
            } finally {
                for(Worker worker:workers) if(worker!=null) {
                    SpeedWorkers1935.release(worker.input);SpeedWorkers1935.release(worker.output);
                }
                SpeedWorkers1935.release(strips.preceding);
            }
            if(strips.failure instanceof OutOfMemoryError) SpeedWorkers1935.trim();
            if(strips.failure != null) throw new IllegalStateException("single noise strip failed",strips.failure);
        } finally { ProcessingTiming1947.end(timing); }
    }

    /** Single-image strong cleanup. The global model is completed before
     * streaming writes; failures discard the private image in normalize(). */
    private static SmoothRegions1958 smoothNoiseInPlace1958(final Bitmap bitmap,
            QualityPixels1932.Plan plan, int level, boolean shadows) {
        if (level <= 0) return null;
        ProcessingTiming1947.Token timing = ProcessingTiming1947.beginStage(
            ProcessingTiming1947.traceFor(bitmap), ProcessingTiming1947.NOISE);
        try {
            final int width = bitmap.getWidth(), height = bitmap.getHeight();
            final boolean captureSmooth = plan != null && plan.sharpLevel > 0;
            requireMemory(StrongNoise1958.modelMemoryBytes(width,height)
                + (captureSmooth ? SmoothRegions1958.memoryBytes(width,height) : 0));
            final SmoothRegions1958 smoothRegions = captureSmooth ? new SmoothRegions1958(width,height) : null;
            final StrongNoise1958.Model model = StrongNoise1958.prepare(new StrongNoise1958.Patches() {
                public void read(int[] pixels,int x,int y,int patchWidth,int patchHeight) {
                    bitmap.getPixels(pixels,0,patchWidth,x,y,patchWidth,patchHeight);
                }
            },width,height,level,shadows);
            final int halo = StrongNoise1958.HALO;
            final long shared = (long)width*halo*4;
            // availableMemory already subtracts the resident Java model. Native
            // map row copies are included in each worker's workspace estimate.
            final long budget = Math.max(0,availableMemory()-RESERVE);
            final int upper = SpeedWorkers1935.maxWorkers();
            int selected = Math.min(256,Scheduling1944.coreRows(width,height,halo,upper,budget));
            long perWorker;
            while (true) {
                perWorker = (long)width*Math.min(height,selected+2*halo)*8
                    + StrongNoise1958.workspaceBytes(width,Math.min(height,selected));
                if (perWorker <= Math.max(0,budget-shared) || selected <= 16) break;
                selected = Math.max(16,selected/2);
            }
            final int core = selected;
            final int maximum = Math.max(1,(int)Math.min(SpeedWorkers1935.availableWorkers1944(),
                Math.min(upper,Math.max(0,budget-shared)/Math.max(1,perWorker))));
            final int count = Math.max(1,Math.min(maximum,(height+core-1)/core));
            requireMemory(perWorker*count+shared);
            final Strips strips = new Strips(bitmap,core,halo);
            final Worker[] workers = new Worker[count];
            try {
                for (int i=0;i<count;i++) workers[i]=new Worker(strips,plan,model,smoothRegions,level,shadows);
                SpeedWorkers1935.run(workers);
            } finally {
                for (Worker worker:workers) if (worker!=null) {
                    SpeedWorkers1935.release(worker.input); SpeedWorkers1935.release(worker.output);
                }
                SpeedWorkers1935.release(strips.preceding);
            }
            if (strips.failure instanceof OutOfMemoryError) SpeedWorkers1935.trim();
            if (strips.failure != null) throw new IllegalStateException("smooth noise strip failed",strips.failure);
            return smoothRegions;
        } finally { ProcessingTiming1947.end(timing); }
    }

    /** Full-photo GPU candidates remain private until their exact environment
     * has qualified against saved CPU references. Encoding boundaries are kept. */
    private static Bitmap finishOwned1953(Bitmap bitmap,Bitmap original,
            QualityPixels1932.Plan plan,QualityPixels1932.Plan sharpPlan,
            boolean moire,boolean sharp,boolean sequential) {
        // mutableOwned established this distinct image's exclusive ownership.
        Bitmap result=finishRoute1954(bitmap,plan,sharpPlan,moire,sharp,sequential,
            bitmap!=original&&bitmap.isMutable()&&bitmap.getConfig()==Bitmap.Config.ARGB_8888
            // A source-domain moire pass preceding a later sharpening pass
            // is not terminal. Calibrate the final sharp pass instead.
            &&(sequential||sharp||plan.sharpLevel<=0));
        if(result!=bitmap)recycleOwned(bitmap,original);
        return result;
    }
    static Bitmap finishRoute1953(Bitmap bitmap,QualityPixels1932.Plan plan,
            QualityPixels1932.Plan sharpPlan,boolean moire,boolean sharp,boolean sequential) {
        return finishRoute1954(bitmap,plan,sharpPlan,moire,sharp,sequential,false);
    }
    private static Bitmap finishRoute1954(Bitmap bitmap,QualityPixels1932.Plan plan,
            QualityPixels1932.Plan sharpPlan,boolean moire,boolean sharp,boolean sequential,boolean owned) {
        if(!readable(bitmap)||bitmap.getConfig()!=Bitmap.Config.ARGB_8888||!GpuFinish1953.available()) {
            finishCpu1953(bitmap,plan,sharpPlan,moire,sharp,sequential);
            return bitmap;
        }
        FinishWork1953 work=new FinishWork1953(bitmap,plan,sharpPlan,moire,sharp,sequential,owned);
        QualityPixels1932.Plan finalPlan=sequential?sharpPlan:plan;
        int[] key={1954,bitmap.getWidth(),bitmap.getHeight(),moire?1:0,sharp?1:0,sequential?1:0,
            finalPlan.sharpLevel,finalPlan.noiseLevel,finalPlan.sharpFloorQ8/1024,
            finalPlan.texturePriority?1:0,finalPlan.haloSuppression?1:0,
            work.layouts[0].count,work.layouts[0].core,
            work.layouts[work.layouts.length-1].count,work.layouts[work.layouts.length-1].core,work.preferredCore,
            finalPlan.localNoise==null?0:1,finalPlan.faceRegions==null?0:1,
            finalPlan.noiseMapAtOutput?1:0,Float.floatToIntBits(finalPlan.outputScale),
            finalPlan.smoothedRegions==null?0:1,
            bitmap.hasAlpha()?1:0,bitmap.isPremultiplied()?1:0};
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.traceFor(bitmap);
        ProcessingTiming1947.Token timer=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.CORRECTION);
        try {
            if(!WholeRoute1953.run(key,work,trace))throw new IllegalStateException("quality route incomplete");
            return work.published?work.candidate:work.cpuBitmap==null?bitmap:work.cpuBitmap;
        } finally {ProcessingTiming1947.end(timer);}
    }
    private static void finishCpu1953(Bitmap bitmap,QualityPixels1932.Plan plan,
            QualityPixels1932.Plan sharpPlan,boolean moire,boolean sharp,boolean sequential) {
        if(sequential) {
            finishInPlace(bitmap,plan,true,false);
            finishInPlace(bitmap,sharpPlan,false,true);
        } else finishInPlace(bitmap,plan,moire,sharp);
    }
    private static final class FinishWork1953 implements WholeRoute1953.Work {
        final Bitmap bitmap;final QualityPixels1932.Plan plan,sharpPlan;
        final boolean moire,sharp,sequential,idleAtStart;final long epoch,captureEpoch;
        final boolean owned;
        final ProcessingTiming1947.Trace trace;
        final CpuLayout1953[] layouts;final long cpuWorkspace;
        final int preferredCore;final long reservation;
        Bitmap candidate,cpuBitmap;boolean published,complete,cpuComplete;
        int dispatchRows=32;
        FinishWork1953(Bitmap b,QualityPixels1932.Plan p,QualityPixels1932.Plan sp,
                boolean m,boolean s,boolean seq) {this(b,p,sp,m,s,seq,false);}
        FinishWork1953(Bitmap b,QualityPixels1932.Plan p,QualityPixels1932.Plan sp,
                boolean m,boolean s,boolean seq,boolean privateOwned) {this(b,p,sp,m,s,seq,null,0,0,privateOwned);}
        FinishWork1953(Bitmap b,QualityPixels1932.Plan p,QualityPixels1932.Plan sp,
                boolean m,boolean s,boolean seq,CpuLayout1953[] fixed,int core,long reserved,boolean privateOwned) {
            bitmap=b;plan=p;sharpPlan=sp;moire=m;sharp=s;sequential=seq;reservation=reserved;
            owned=privateOwned;
            epoch=SpeedWorkers1935.competitionEpoch1944();
            captureEpoch=ProcessingTiming1947.captureEpoch1953();
            trace=ProcessingTiming1947.traceFor(b);
            idleAtStart=SpeedWorkers1935.cpuIdle1944()&&SaveQueue1935.count()<=1&&AsyncSave1935.codecIdle1953()
                &&ProcessingTiming1947.otherCapturesIdle1953(trace);
            // Freeze CPU geometry before the separate CPU destination allocates.
            layouts=fixed!=null?fixed:seq?new CpuLayout1953[]{new CpuLayout1953(b,1),new CpuLayout1953(b,2)}
                :new CpuLayout1953[]{new CpuLayout1953(b,(m?1:0)|(s?2:0))};
            long peak=0;for(CpuLayout1953 layout:layouts)peak=Math.max(peak,layout.workspace);
            cpuWorkspace=peak;
            long copy=(long)b.getWidth()*b.getHeight()*4;
            // Qualification must fit at this same route geometry. Selecting a
            // 512-row foreground shape whose proof cannot fit would keep it on
            // CPU forever even when the 256-row complete proof is affordable.
            long copies=owned?copy*3:copy;
            preferredCore=core>0?core:copies+gpuWorkspace(512)+cpuWorkspace<=availableMemory()-RESERVE?512:256;
        }
        private long gpuWorkspace(int core) {
            // Two native source/policy/output/intermediate slots, native staging
            // and Java producer/policy/consumer arrays. Include simultaneous
            // PACKED2 + RAW4 promotion, even on the first cold allocation.
            return (long)bitmap.getWidth()*Math.min(bitmap.getHeight(),core+72)*112L;
        }
        public boolean dispatchSelection() {return true;}
        public void setDispatchRows(int rows) {
            if(rows!=32&&rows!=64&&rows!=0)throw new IllegalArgumentException("GPU dispatch rows");
            dispatchRows=rows;
        }
        public boolean gpu() {return gpu(null);}
        boolean gpu(WholeRoute1953.Cancellation cancellation) {
            int w=bitmap.getWidth(),h=bitmap.getHeight();
            if(reservation==0)requireMemory((long)w*h*4+gpuWorkspace(preferredCore)+cpuWorkspace);
            check1953(cancellation);
            // H20: every destination pixel is written from the immutable input;
            // no initial full-image copy and no partially written publication.
            candidate=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            GpuFinish1953.Session session=null;int[] input=null,output=null;
            try {
                candidate.setDensity(bitmap.getDensity());candidate.setHasAlpha(bitmap.hasAlpha());
                candidate.setPremultiplied(bitmap.isPremultiplied());
                session=GpuFinish1953.open(w,h,sequential?sharpPlan:plan,moire,sharp,sequential,preferredCore,dispatchRows);
                if(session==null)return false;
                int core=session.coreRows(),halo=session.halo();
                input=SpeedWorkers1935.borrowInts(w*Math.min(h,core+2*halo));
                output=SpeedWorkers1935.borrowInts(w*Math.min(h,core));
                GpuFinish1953.Ticket[] ring=new GpuFinish1953.Ticket[2];int head=0,count=0,written=0;
                for(int first=0;first<h;first+=core) {
                    check1953(cancellation);
                    if(count==2) {
                        GpuFinish1953.Ticket done=ring[head];
                        if(!session.collectCandidate(done,output,0))return false;
                        check1953(cancellation);
                        candidate.setPixels(output,0,w,0,done.first,w,done.last-done.first);
                        written+=done.last-done.first;ring[head]=null;head=(head+1)%2;count--;
                    }
                    int last=Math.min(h,first+core),origin=Math.max(0,first-halo);
                    int rows=Math.min(h,last+halo)-origin;
                    // Native coverage is queried on the owning session. Unknown
                    // coverage reports origin, retaining the full transfer path.
                    int fresh=session.freshInputOrigin(first,last);
                    if(fresh<origin||fresh>origin+rows)fresh=origin;
                    int freshRows=origin+rows-fresh;
                    if(freshRows>0)bitmap.getPixels(input,(fresh-origin)*w,w,0,fresh,w,freshRows);
                    QualityPixels1932.Plan gpuPlan=sequential?sharpPlan:plan;
                    FinishPolicy1953.Band policy=sharp&&gpuPlan.sharpGainQ8>0
                        ?FinishPolicy1953.prepare(gpuPlan,w,rows,first-origin,last-origin,origin)
                        :FinishPolicy1953.unused(w*(last-first));
                    try {
                        check1953(cancellation);
                        if(policy==null)return false;
                        GpuFinish1953.Ticket ticket=session.submit(input,origin,rows,first,last,policy);
                        if(ticket==null)return false;
                        ring[(head+count)%2]=ticket;count++;
                    } finally {if(policy!=null)policy.close();}
                    // Submit has copied producer arrays but deliberately has not
                    // waited for GPU completion. The next preparation overlaps it.
                }
                while(count>0) {
                    check1953(cancellation);GpuFinish1953.Ticket done=ring[head];
                    if(!session.collectCandidate(done,output,0))return false;
                    check1953(cancellation);
                    candidate.setPixels(output,0,w,0,done.first,w,done.last-done.first);
                    written+=done.last-done.first;head=(head+1)%2;count--;
                }
                complete=written==h;return complete;
            } finally {
                // close drains real fences even after interruption before arrays
                // or independent probe images can return to their owners.
                if(session!=null)session.close();
                SpeedWorkers1935.release(input);SpeedWorkers1935.release(output);
            }
        }
        public boolean cpu() {
            CpuLayout1953[] old=CPU_LAYOUT_1953.get();CPU_LAYOUT_1953.set(layouts);
            try {finishCpu1953(cpuBitmap==null?bitmap:cpuBitmap,plan,sharpPlan,moire,sharp,sequential);cpuComplete=true;return true;}
            finally {restore(CPU_LAYOUT_1953,old);}
        }
        public boolean equal() {return equal1953(bitmap,candidate,null);}
        public void publishGpu() {
            if(!complete||candidate==null)throw new IllegalStateException("incomplete quality candidate");
            ProcessingTiming1947.transfer(bitmap,candidate);ShotContext1932.copy(bitmap,candidate);published=true;
        }
        public void discardGpu() {if(candidate!=null&&!published&&!candidate.isRecycled())candidate.recycle();}
        public boolean timingReliable() {
            return idleAtStart&&epoch==SpeedWorkers1935.competitionEpoch1944()
                &&captureEpoch==ProcessingTiming1947.captureEpoch1953()
                &&ProcessingTiming1947.otherCapturesIdle1953(trace)
                &&SpeedWorkers1935.cpuIdle1944()&&SaveQueue1935.count()<=1&&AsyncSave1935.codecIdle1953();
        }
        public long probeBytes() {
            if(!owned||bitmap.getConfig()!=Bitmap.Config.ARGB_8888||!timingReliable())return 0;
            // Count retained source, CPU destination and background candidate.
            // The source is already allocated; accounting remains conservative
            // through resize/save after its foreground owner requests disposal.
            long bytes=(long)bitmap.getWidth()*bitmap.getHeight()*12+gpuWorkspace(preferredCore);
            return bytes>0&&bytes+cpuWorkspace<=availableMemory()-RESERVE?bytes:0;
        }
        public WholeRoute1953.Probe snapshotProbe() {
            if(!owned||bitmap.getConfig()!=Bitmap.Config.ARGB_8888)return null;
            long bytes=(long)bitmap.getWidth()*bitmap.getHeight()*12+gpuWorkspace(preferredCore);
            Frozen1954 source=freeze1954(bitmap),reference=null;Bitmap target=null;
            try {
                // One real CPU destination replaces both former calibration
                // snapshots. Its exact completed pixels become the reference;
                // the existing source stays frozen, without a source clone.
                target=bitmap.copy(Bitmap.Config.ARGB_8888,true);
                if(target==null)return null;
                reference=freeze1954(target);
                ProcessingTiming1947.transfer(bitmap,target);ShotContext1932.copy(bitmap,target);
                FinishProbe1953 probe=new FinishProbe1953(this,source,reference,bytes);
                cpuBitmap=target;source=null;reference=null;target=null;return probe;
            } finally {
                if(target!=null)recycle1954(target);
                thaw1954(reference);thaw1954(source);
            }
        }
    }
    private static final class FinishProbe1953 implements WholeRoute1953.Probe {
        final Bitmap input,reference;final long budget;
        final Frozen1954 inputLease,referenceLease;
        final FinishWork1953 original;
        final QualityPixels1932.Plan plan,sharpPlan;
        final boolean moire,sharp,sequential;
        final CpuLayout1953[] layouts;final int preferredCore;
        FinishWork1953 work;int dispatchRows=32;boolean discarded;
        FinishProbe1953(FinishWork1953 original,Frozen1954 input,Frozen1954 reference,long budget) {
            this.original=original;inputLease=input;referenceLease=reference;
            this.input=input.bitmap;this.reference=reference.bitmap;this.budget=budget;
            plan=original.plan;sharpPlan=original.sharpPlan;moire=original.moire;
            sharp=original.sharp;sequential=original.sequential;
            layouts=original.layouts;preferredCore=original.preferredCore;
        }
        public boolean captureReference() {
            return original.cpuComplete&&!input.isRecycled()&&!reference.isRecycled();
        }
        public long bytes() {return budget;}
        public boolean dispatchSelection() {return true;}
        public void setDispatchRows(int rows) {dispatchRows=rows;}
        public boolean gpu(WholeRoute1953.Cancellation cancellation) {
            check1953(cancellation);
            discardCandidate();
            work=new FinishWork1953(input,plan,sharpPlan,moire,sharp,sequential,layouts,preferredCore,budget,false);
            work.setDispatchRows(dispatchRows);
            return work.gpu(cancellation);
        }
        public boolean equal(WholeRoute1953.Cancellation cancellation) {
            return work!=null&&work.complete&&equal1953(reference,work.candidate,cancellation);
        }
        public boolean idle() {return SaveQueue1935.idle1953()&&ShotContext1932.idle1953()&&SpeedWorkers1935.cpuIdle1944();}
        public boolean timingReliable() {return work!=null&&work.timingReliable();}
        public void discardCandidate() {if(work!=null){work.discardGpu();work=null;}}
        public synchronized void discard() {
            if(discarded)return;discarded=true;
            try {discardCandidate();}
            finally {
                // Failed CPU output never transferred to normalization/save.
                if(!original.cpuComplete)recycle1954(reference);
                try {thaw1954(referenceLease);}finally {thaw1954(inputLease);}
            }
        }
    }
    private static void check1953(WholeRoute1953.Cancellation cancellation) {
        if(Thread.currentThread().isInterrupted()||(cancellation!=null&&cancellation.cancelled()))
            throw new IllegalStateException("quality interrupted");
    }
    private static boolean equal1953(Bitmap reference,Bitmap candidate,WholeRoute1953.Cancellation cancellation) {
        if(reference==null||candidate==null||reference.getWidth()!=candidate.getWidth()
            ||reference.getHeight()!=candidate.getHeight()||reference.getDensity()!=candidate.getDensity()
            ||reference.hasAlpha()!=candidate.hasAlpha()||reference.isPremultiplied()!=candidate.isPremultiplied()
            ||!readable(reference)||!readable(candidate))return false;
        int w=reference.getWidth();int[] left=SpeedWorkers1935.borrowInts(w),right=null;
        try {
            right=SpeedWorkers1935.borrowInts(w);
            for(int y=0;y<reference.getHeight();y++) {
                check1953(cancellation);
                reference.getPixels(left,0,w,0,y,w,1);candidate.getPixels(right,0,w,0,y,w,1);
                for(int x=0;x<w;x++)if(left[x]!=right[x])return false;
            }
            return true;
        } finally {SpeedWorkers1935.release(left);SpeedWorkers1935.release(right);}
    }
    private static final class CpuLayout1953 {
        final int stage,width,height,core,count;final long workspace;
        CpuLayout1953(Bitmap bitmap,int phase) {
            stage=phase;width=bitmap.getWidth();height=bitmap.getHeight();
            long budget=Math.max(0,availableMemory()-RESERVE);
            int upper=SpeedWorkers1935.maxWorkers();
            core=Scheduling1944.coreRows(width,height,QualityPixels1932.HALO,upper,budget);
            long bytes=(long)width*Math.min(height,core+2*QualityPixels1932.HALO)*8;
            int maximum=Math.max(1,(int)Math.min(SpeedWorkers1935.availableWorkers1944(),Math.min(upper,budget/Math.max(1,bytes))));
            count=Scheduling1944.workers(stage,(long)width*height,(height+core-1)/core,maximum);
            workspace=bytes*count+(long)width*QualityPixels1932.HALO*4;
        }
    }
    static void finishInPlace(Bitmap bitmap, QualityPixels1932.Plan plan, boolean moire, boolean sharp) {
        if (!moire && (!sharp || plan.sharpLevel == 0)) return;
        ProcessingTiming1947.Token timing = ProcessingTiming1947.beginStage(
            ProcessingTiming1947.traceFor(bitmap), ProcessingTiming1947.CORRECTION);
        try {
        final int stage=(moire?1:0)|(sharp?2:0);
        final long pixels=(long)bitmap.getWidth()*bitmap.getHeight();
        final long budget=Math.max(0,availableMemory()-RESERVE);
        int upper=SpeedWorkers1935.maxWorkers();
        CpuLayout1953 fixed=null;CpuLayout1953[] layouts=CPU_LAYOUT_1953.get();
        if(layouts!=null)for(CpuLayout1953 layout:layouts)
            if(layout.stage==stage && layout.width==bitmap.getWidth() && layout.height==bitmap.getHeight())fixed=layout;
        final int core=fixed==null?Scheduling1944.coreRows(bitmap.getWidth(),bitmap.getHeight(),QualityPixels1932.HALO,upper,budget):fixed.core;
        long bytes=(long)bitmap.getWidth()*Math.min(bitmap.getHeight(),core+2*QualityPixels1932.HALO)*8;
        int maximum=Math.max(1,(int)Math.min(SpeedWorkers1935.availableWorkers1944(),
            Math.min(upper,budget/Math.max(1,bytes))));
        int count=fixed==null?Scheduling1944.workers(stage,pixels,(bitmap.getHeight()+core-1)/core,maximum):fixed.count;
        requireMemory(bytes*count+(long)bitmap.getWidth()*QualityPixels1932.HALO*4);
        Strips strips=new Strips(bitmap,core);
        final long epoch=SpeedWorkers1935.competitionEpoch1944();
        final boolean idle=SpeedWorkers1935.cpuIdle1944();
        final long started=System.nanoTime();
        Worker[] workers = new Worker[count];
        try {
            for (int i = 0; i < count; i++) workers[i] = new Worker(strips, plan, moire, sharp);
            SpeedWorkers1935.run(workers);
        } finally {
            for (Worker worker : workers) if (worker != null) {
                SpeedWorkers1935.release(worker.input);
                SpeedWorkers1935.release(worker.output);
            }
            SpeedWorkers1935.release(strips.preceding);
        }
        if(strips.failure==null)Scheduling1944.measured(stage,count,pixels,System.nanoTime()-started,
            idle && epoch==SpeedWorkers1935.competitionEpoch1944());
        if (strips.failure instanceof OutOfMemoryError) SpeedWorkers1935.trim();
        if (strips.failure != null) throw new IllegalStateException("quality strip failed", strips.failure);
        } finally { ProcessingTiming1947.end(timing); }
    }
}
