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
    private static final ThreadLocal<PhotoDetail.Settings> LEGACY = new ThreadLocal<PhotoDetail.Settings>();
    private static final Map<ChromaPipeline186.State, QualityPixels1932.Plan> STATES =
        Collections.synchronizedMap(new WeakHashMap<ChromaPipeline186.State, QualityPixels1932.Plan>());
    private static final Map<Bitmap, Prepared> PREPARED =
        Collections.synchronizedMap(new WeakHashMap<Bitmap, Prepared>());
    private static final int CORE = 128;
    private static final long RESERVE = 32L * 1024 * 1024;

    private static final class Prepared {
        final WeakReference<Bitmap> source;
        Prepared(Bitmap source) { this.source = new WeakReference<Bitmap>(source); }
    }

    /** Called only from the two final-save normalization sites. */
    public static Bitmap normalize(Bitmap input, int rotation, boolean fixed245) {
        Bitmap working = input;
        QualityPixels1932.Plan previous = CURRENT.get();
        PhotoDetail.Settings oldLegacy = LEGACY.get();
        try {
            if (!readable(input)) return SaveQuality2.normalize186(input, rotation, fixed245);
            final PhotoDetail.Settings requested = PhotoDetail.snapshot1932();
            if (requested == null) return SaveQuality2.normalize186(input, rotation, fixed245);
            final int turn = normalizeRotation(rotation);
            final int[] size = SaveQuality2.output186(input.getWidth(), input.getHeight(), turn, fixed245);
            final int rw = turn == 90 || turn == 270 ? input.getHeight() : input.getWidth();
            final int rh = turn == 90 || turn == 270 ? input.getWidth() : input.getHeight();
            final float scale = Math.max((float)size[0] / rw, (float)size[1] / rh);
            final boolean reduceFirst = scale < 0.999999f;
            final ShotContext1932.Snapshot shot = ShotContext1932.forBitmap(input);
            final boolean chroma = ChromaPipeline177.enabled1932();

            // Anti-aliased reduction first bounds work for very large sensor images.
            // Enlargement is always deferred until source-domain cleanup is finished.
            if (reduceFirst) working = resample(input, turn, size[0], size[1]);
            QualityPixels1932.NoiseStats stats = probe(working);
            QualityPixels1932.Plan plan = QualityPixels1932.plan(stats,
                shot.metadataReliable ? shot.iso : 0,
                shot.metadataReliable ? shot.exposureNanos : 0,
                lensKind(shot), shot.beautyStrength,
                requested.noiseOn ? requested.noiseLevel : 0,
                requested.sharpOn ? requested.sharpLevel : 0,
                requested.texturePriority, requested.shadowPriority,
                reduceFirst ? 1f : scale).withHaloSuppression(requested.haloSuppression);
            PhotoDetail.Settings sourceSettings = new PhotoDetail.Settings(
                plan.noiseLevel > 0, Math.max(1, plan.noiseLevel), false, 1,
                requested.texturePriority, requested.haloSuppression, requested.shadowPriority);
            CURRENT.set(plan);
            LEGACY.set(sourceSettings);

            // The legacy fallback reads its settings through settingsForLegacy().
            // Passing the same Bitmap twice keeps this helper in charge of ownership.
            Bitmap filtered = ChromaPipeline177.apply(working, working, sourceSettings);
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
                Bitmap resized = resample(working, turn, size[0], size[1]);
                if (resized != working) {
                    Bitmap old = working;
                    working = resized;
                    recycleOwned(old, input);
                }
            }
            if (plan.sharpLevel > 0) {
                working = mutableOwned(working, input);
                finishInPlace(working, plan, false, true);
            }
            if (working.getWidth() != size[0] || working.getHeight() != size[1])
                throw new IllegalStateException("quality output dimensions");
            ShotContext1932.copy(input, working);
            PREPARED.put(working, new Prepared(input));
            return working;
        } catch (RuntimeException failure) {
            recycleOwned(working, input);
            return SaveQuality2.normalize186(input, rotation, fixed245);
        } catch (OutOfMemoryError failure) {
            recycleOwned(working, input);
            return SaveQuality2.normalize186(input, rotation, fixed245);
        } finally {
            restore(CURRENT, previous);
            restore(LEGACY, oldLegacy);
        }
    }

    /** Receives the already-existing PhotoDetail settings snapshot. */
    public static Bitmap applyDetail(Bitmap bitmap, Bitmap original, PhotoDetail.Settings settings) {
        Prepared prepared = PREPARED.remove(bitmap);
        if (prepared != null && original != null && prepared.source.get() == original) return bitmap;
        // Non-save callers and preparation fallbacks retain the established behavior.
        return ChromaPipeline177.apply(bitmap, original, settings);
    }

    public static PhotoDetail.Settings settingsForLegacy(PhotoDetail.Settings original) {
        PhotoDetail.Settings override = LEGACY.get();
        return override == null ? original : override;
    }

    /** Added after State's existing constructor has completed its field writes. */
    public static void stateCreated(ChromaPipeline186.State state) {
        QualityPixels1932.Plan plan = CURRENT.get();
        if (state != null && plan != null) STATES.put(state, plan);
    }

    /** Bridge used by the existing four-worker scheduler; no extra NR worker pool. */
    public static void run(ChromaPipeline186.State state, ChromaPipeline186.Buffer buffer) {
        QualityPixels1932.Plan old = CURRENT.get();
        QualityPixels1932.Plan plan = STATES.get(state);
        if (plan == null) CURRENT.remove(); else CURRENT.set(plan);
        try { ShadowDetail1923.run(state, buffer); }
        finally { restore(CURRENT, old); }
    }

    public static int shadowBudgetQ8(int pixel, int localRange) {
        return QualityPixels1932.shadowBudgetQ8(pixel, localRange, CURRENT.get());
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

    private static QualityPixels1932.NoiseStats probe(Bitmap bitmap) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        int pw = Math.min(80, width), ph = Math.min(80, height);
        int[] pixels = new int[pw * ph];
        QualityPixels1932.NoiseProbe probe = new QualityPixels1932.NoiseProbe();
        // Nine original-resolution patches; never estimate sensor grain from a thumbnail.
        for (int iy = 0; iy < 3; iy++) for (int ix = 0; ix < 3; ix++) {
            int x = (width - pw) * ix / 2, y = (height - ph) * iy / 2;
            bitmap.getPixels(pixels, 0, pw, x, y, pw, ph);
            probe.add(pixels, pw, ph);
        }
        return probe.finish();
    }

    private static Bitmap mutableOwned(Bitmap bitmap, Bitmap original) {
        if (bitmap != original && bitmap.isMutable() && bitmap.getConfig() == Bitmap.Config.ARGB_8888)
            return bitmap;
        requireMemory((long)bitmap.getWidth() * bitmap.getHeight() * 4);
        Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        if (copy == null) throw new IllegalStateException("quality bitmap copy");
        try {
            ShotContext1932.copy(bitmap, copy);
            recycleOwned(bitmap, original);
            return copy;
        } catch (RuntimeException error) { copy.recycle(); throw error; }
          catch (OutOfMemoryError error) { copy.recycle(); throw error; }
    }

    static Bitmap resample(final Bitmap bitmap, final int rotation, final int width, final int height) {
        final int sourceWidth = bitmap.getWidth(), sourceHeight = bitmap.getHeight();
        final int rw = rotation == 90 || rotation == 270 ? sourceHeight : sourceWidth;
        final int rh = rotation == 90 || rotation == 270 ? sourceWidth : sourceHeight;
        if (rotation == 0 && rw == width && rh == height) return bitmap;
        requireMemory((long)width * height * 4 + (long)Math.max(rw, width) * 256);
        final Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final double scale = Math.max((double)width / rw, (double)height / rh);
        final double cw = Math.min((double)rw, width / scale);
        final double ch = Math.min((double)rh, height / scale);
        try {
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
                    result.setPixels(pixels, 0, width, 0, row, width, 1);
                }
            }, width, height, Math.max(0, (rw - cw) * 0.5), Math.max(0, (rh - ch) * 0.5), cw, ch);
            return result;
        } catch (RuntimeException error) { result.recycle(); throw error; }
          catch (OutOfMemoryError error) { result.recycle(); throw error; }
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
        if (bytes < 0 || bytes > availableMemory() - RESERVE)
            throw new OutOfMemoryError("bounded quality workspace");
    }

    private static void recycleOwned(Bitmap bitmap, Bitmap original) {
        if (bitmap != null && bitmap != original && !bitmap.isRecycled()) bitmap.recycle();
    }

    /** Sequential reads preserve original top halos even when worker writes finish out of order. */
    private static final class Strips {
        final Bitmap bitmap;
        final int width, height, halo;
        final int[] preceding;
        int next, precedingRows;
        volatile Throwable failure;
        Strips(Bitmap bitmap) {
            this.bitmap = bitmap; width = bitmap.getWidth(); height = bitmap.getHeight();
            halo = QualityPixels1932.HALO; preceding = new int[width * halo];
        }
        synchronized int read(Worker worker) {
            if (failure != null || next >= height) return -1;
            int first = next, count = Math.min(CORE, height - first);
            int top = Math.max(0, first - halo), bottom = Math.min(height, first + count + halo);
            int rows = bottom - top, start = first - top;
            bitmap.getPixels(worker.input, 0, width, 0, top, width, rows);
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
            int size = strips.width * Math.min(strips.height, CORE + 2 * strips.halo);
            input = new int[size]; output = new int[size];
        }
        public void run() {
            try {
                while (strips.failure == null && !Thread.currentThread().isInterrupted() && strips.read(this) >= 0) {
                    QualityPixels1932.finishStrip(input, output, strips.width, rows,
                        start, start + count, plan, moire, sharp);
                    strips.write(this);
                }
                if (Thread.currentThread().isInterrupted()) strips.fail(new IllegalStateException("quality interrupted"));
            } catch (Throwable error) { strips.fail(error); }
        }
    }

    static void finishInPlace(Bitmap bitmap, QualityPixels1932.Plan plan, boolean moire, boolean sharp) {
        if (!moire && (!sharp || plan.sharpLevel == 0)) return;
        long bytes = (long)bitmap.getWidth() * Math.min(bitmap.getHeight(), CORE + 2 * QualityPixels1932.HALO) * 8;
        int count = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors()));
        count = Math.min(count, (bitmap.getHeight() + CORE - 1) / CORE);
        count = Math.max(1, Math.min(count, (int)Math.min(4, Math.max(0, availableMemory() - RESERVE) / Math.max(1, bytes))));
        requireMemory(bytes * count + (long)bitmap.getWidth() * QualityPixels1932.HALO * 4);
        Strips strips = new Strips(bitmap);
        Worker[] workers = new Worker[count];
        for (int i = 0; i < count; i++) workers[i] = new Worker(strips, plan, moire, sharp);
        Thread[] threads = new Thread[Math.max(0, count - 1)];
        int started = 0;
        boolean interrupted = false;
        try {
            for (int i = 0; i < threads.length; i++) {
                try {
                    threads[i] = new Thread(workers[i + 1], "ULikeQuality1932");
                    threads[i].setDaemon(true);
                    threads[i].start(); started++;
                } catch (OutOfMemoryError threadLimit) { break; }
            }
            workers[0].run();
        } finally {
            for (int i = 0; i < started; i++) {
                while (threads[i].isAlive()) {
                    try { threads[i].join(); }
                    catch (InterruptedException error) {
                        interrupted = true;
                        strips.fail(error);
                        for (int j = 0; j < started; j++) threads[j].interrupt();
                    }
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
        if (strips.failure != null) throw new IllegalStateException("quality strip failed", strips.failure);
    }
}
