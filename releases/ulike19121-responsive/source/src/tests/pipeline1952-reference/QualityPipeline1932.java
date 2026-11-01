package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import java.lang.ref.WeakReference;
import java.util.Collections;
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
                plan.noiseLevel > 0, Math.max(1, plan.noiseLevel), false, 1,
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
            if (chroma) {
                working = mutableOwned(working, input);
                finishInPlace(working, plan, true, false);
            }
            if (!reduceFirst) {
                Bitmap resized = timedResize(working, turn, size[0], size[1], trace);
                if (resized != working) {
                    Bitmap old = working;
                    working = resized;
                    recycleOwned(old, input);
                }
            }
            if (plan.sharpLevel > 0) {
                if (!reduceFirst) {
                    if (faceRegions != null) faceRegions=timedFaceResize(faceRegions, turn, size[0], size[1], trace);
                    plan=plan.withFaceRegions(faceRegions);
                }
                // A source noise grid cannot be sampled at final-save coordinates
                // after crop, rotation or resize. Refresh only that bounded map;
                // retain the source plan's gain/floor and all captured user options.
                boolean moved = turn != 0 || size[0] != input.getWidth() || size[1] != input.getHeight();
                plan = plan.withOutputNoise(moved ? probeSpatial(working) : localNoise);
                working = mutableOwned(working, input);
                finishInPlace(working, plan, false, true);
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
        if (bitmap != original && bitmap.isMutable() && bitmap.getConfig() == Bitmap.Config.ARGB_8888)
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
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
    }

    private static void requireMemory(long bytes) {
        if (bytes < 0 || bytes > availableMemory() - RESERVE) {
            SpeedWorkers1935.trim();
            if (bytes < 0 || bytes > availableMemory() - RESERVE)
                throw new OutOfMemoryError("bounded quality workspace");
        }
    }

    private static void recycleOwned(Bitmap bitmap, Bitmap original) {
        if (bitmap != null && bitmap != original && !bitmap.isRecycled()) bitmap.recycle();
    }

    /** Sequential reads preserve original top halos even when worker writes finish out of order. */
    private static final class Strips {
        final Bitmap bitmap;
        final int width, height, halo, core;
        final int[] preceding;
        int next, precedingRows;
        volatile Throwable failure;
        Strips(Bitmap bitmap,int core) {
            this.bitmap = bitmap; width = bitmap.getWidth(); height = bitmap.getHeight(); this.core=core;
            halo = QualityPixels1932.HALO; preceding = SpeedWorkers1935.borrowInts(width * halo);
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
        final int[] input, output;
        int first, rows, start, count;
        Worker(Strips strips, QualityPixels1932.Plan plan, boolean moire, boolean sharp) {
            this.strips = strips; this.plan = plan; this.moire = moire; this.sharp = sharp;
            int size = strips.width * Math.min(strips.height, strips.core + 2 * strips.halo);
            int[] first = SpeedWorkers1935.borrowInts(size);
            try { output = SpeedWorkers1935.borrowInts(size); }
            catch (Throwable failure) { SpeedWorkers1935.release(first); throw failure; }
            input = first;
        }
        public void run() {
            try {
                while (strips.failure == null && !Thread.currentThread().isInterrupted() && strips.read(this) >= 0) {
                    QualityPixels1932.finishStripAt(input, output, strips.width, rows,
                        start, start + count, plan, moire, sharp, first-start);
                    strips.write(this);
                }
                if (Thread.currentThread().isInterrupted()) strips.fail(new IllegalStateException("quality interrupted"));
            } catch (Throwable error) { strips.fail(error); }
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
        final int core=Scheduling1944.coreRows(bitmap.getWidth(),bitmap.getHeight(),QualityPixels1932.HALO,upper,budget);
        long bytes=(long)bitmap.getWidth()*Math.min(bitmap.getHeight(),core+2*QualityPixels1932.HALO)*8;
        int maximum=Math.max(1,(int)Math.min(SpeedWorkers1935.availableWorkers1944(),
            Math.min(upper,budget/Math.max(1,bytes))));
        int count=Scheduling1944.workers(stage,pixels,(bitmap.getHeight()+core-1)/core,maximum);
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
