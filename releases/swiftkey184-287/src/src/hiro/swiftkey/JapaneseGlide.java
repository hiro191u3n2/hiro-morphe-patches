package hiro.swiftkey;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;
import com.microsoft.fluency.Japanese;
import com.microsoft.fluency.ResultsFilter;
import com.touchtype.KeyboardService;
import com.touchtype_fluency.service.j0;
import ec0.m;
import fb0.t0;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Arrays;
import k40.l;
import lh0.a;
import vb0.b;
import vb0.c;
import wb0.x;
import wb0.x0;
import xd0.d1;
import xd0.f0;

/** Local Japanese glide recognition. All editor and queue state is confined to the UI thread. */
public final class JapaneseGlide {
    public static final String KEY = "hiro_japanese_qwerty_glide";
    private static final int MAX_PENDING = 8;
    private static final long MAX_STROKE_MILLIS = 10000;
    private static volatile SharedPreferences preferences;
    private static volatile boolean loading;
    private static volatile long retryAfter;
    private static long epoch;
    private static Stroke active, worker, choosing;
    private static WeakReference<d1> draining;
    private static final ArrayDeque<Stroke> pending = new ArrayDeque<Stroke>();
    private static Snapshot expected;

    private JapaneseGlide() { }

    public static void init(SharedPreferences value) { preferences = value; }
    private static boolean enabled() {
        SharedPreferences p = preferences;
        return p != null && p.getBoolean(KEY, false);
    }

    public static synchronized void prepare(Context context) {
        if (!enabled() || loading || JapaneseGlideDecoder.isReady() || context == null
                || SystemClock.uptimeMillis() < retryAfter) return;
        try {
            Context app = context.getApplicationContext();
            final AssetManager assets = (app == null ? context : app).getAssets();
            loading = true;
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    boolean success = false;
                    try (InputStream stream = assets.open(JapaneseGlideDecoder.ASSET)) {
                        JapaneseGlideDecoder.load(stream);
                        success = JapaneseGlideDecoder.isReady();
                    } catch (Exception | LinkageError | OutOfMemoryError ignored) { }
                    synchronized (JapaneseGlide.class) {
                        loading = false;
                        retryAfter = success ? 0 : SystemClock.uptimeMillis() + 3000;
                    }
                    final boolean ready = success;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            if (!ready && !pending.isEmpty()) {
                                notice(pending.peek().view, "グライド辞書を読み込めませんでした。もう一度なぞってください");
                                clearPending();
                            } else pump();
                        }
                    });
                }
            }, "JapaneseGlideDictionary");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        } catch (Exception | LinkageError | OutOfMemoryError ignored) {
            loading = false;
            retryAfter = SystemClock.uptimeMillis() + 3000;
        }
    }

    public static void start(KeyboardService service) { reset(); prepare(service); }

    public static void reset() {
        LineEnter.reset();
        if (active != null && active.captured) draining = new WeakReference<d1>(active.view);
        active = null;
        clearPending();
    }

    private static void clearPending() {
        ++epoch;
        pending.clear();
        expected = null;
        choosing = null;
        JapaneseGlideCandidates.dismiss();
        // A running decoder has no editor access. Its epoch/head check discards stale results.
    }

    public static boolean handle(d1 view, a token, MotionEvent event) {
        if (event == null) return false;
        int action = event.getActionMasked();
        if (LineEnter.handle(view, token, event)) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (active != null && active.captured) draining = new WeakReference<d1>(active.view);
                active = null;
                clearPending();
            }
            return true;
        }
        if (draining != null && draining.get() == view) {
            if (action != MotionEvent.ACTION_DOWN) {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) draining = null;
                return true;
            }
            draining = null;
        }
        Stroke stroke = active;
        boolean owned = stroke != null && stroke.view == view && stroke.captured;
        try {
            if (!pending.isEmpty() && !validPending(action == MotionEvent.ACTION_DOWN)) clearPending();
            if (action == MotionEvent.ACTION_DOWN) {
                if (stroke != null && stroke.captured && stroke.view != view)
                    draining = new WeakReference<d1>(stroke.view);
                active = null;
                if (view instanceof f0 && event.getPointerCount() == 1 && supportedTool(event.getToolType(0)) && enabled()) {
                    prepare(view.getContext());
                    active = begin((f0) view, token, event);
                }
                // A second eligible DOWN is not an editor change and must not discard word one.
                if (active == null && !pending.isEmpty()) clearPending();
                return false;
            }
            if (stroke == null || stroke.view != view) return false;
            if (action == MotionEvent.ACTION_CANCEL) {
                active = null;
                return owned;
            }
            if (action == MotionEvent.ACTION_POINTER_DOWN || event.getPointerCount() != 1
                    || event.getPointerId(0) != stroke.pointer || !unchanged(stroke)
                    || event.getEventTime() - stroke.downTime > MAX_STROKE_MILLIS) return abandon(stroke, action);
            if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP) return owned;
            for (int i = 0; i < event.getHistorySize(); i++)
                if (!stroke.add(event.getHistoricalX(0, i), event.getHistoricalY(0, i))) return abandon(stroke, action);
            if (!stroke.add(event.getX(), event.getY())) return abandon(stroke, action);
            if (!stroke.captured && stroke.maxDisplacementSq >= stroke.captureDistanceSq) {
                // Native DOWN has already been delivered. Cancel it exactly once before owning the trace.
                // The actual native popup state, not the configured long-press timeout, decides availability.
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                stroke.captured = true;
                try { view.f(token, cancel); } finally { cancel.recycle(); }
                if (!unchanged(stroke)) return abandon(stroke, action);
            }
            if (action == MotionEvent.ACTION_UP) {
                active = null;
                if (stroke.captured) { enqueue(stroke); return true; }
                // The native tap will change the editor after this hook returns.
                if (!pending.isEmpty()) clearPending();
                return false;
            }
            return stroke.captured;
        } catch (Exception | LinkageError | OutOfMemoryError ignored) {
            if (stroke != null && stroke.captured) return abandon(stroke, action);
            active = null;
            return owned;
        }
    }

    private static boolean supportedTool(int tool) {
        return tool == MotionEvent.TOOL_TYPE_FINGER || tool == MotionEvent.TOOL_TYPE_STYLUS;
    }

    private static boolean abandon(Stroke stroke, int action) {
        active = null;
        if (stroke.captured && action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL)
            draining = new WeakReference<d1>(stroke.view);
        return stroke.captured;
    }

    private static void enqueue(Stroke stroke) {
        if (!unchanged(stroke) || !stroke.snapshot.editorMatches(stroke.connection)) return;
        if (pending.size() >= MAX_PENDING) {
            notice(stroke.view, "先にグライド候補を選んでください");
            return;
        }
        if (pending.isEmpty()) expected = Snapshot.read(stroke.model, stroke.connection);
        stroke.path = Arrays.copyOf(stroke.path, stroke.count * 2);
        pending.add(stroke);
        if (!JapaneseGlideDecoder.isReady()) {
            if (pending.size() == 1) notice(stroke.view, "グライド辞書を準備中…入力した軌跡を待機しています");
            prepare(stroke.view.getContext());
            if (!loading) {
                notice(stroke.view, "グライド辞書を読み込めませんでした。少し待ってもう一度なぞってください");
                clearPending();
            }
            return;
        }
        pump();
    }

    private static boolean validPending() {
        return validPending(true);
    }

    private static boolean validPending(boolean checkSurroundingText) {
        try {
            Stroke head = pending.peek();
            return head != null && head.epoch == epoch && contextUnchanged(head)
                    && expected != null && expected.matches(head.model)
                    && (!checkSurroundingText || expected.editorMatches(head.connection));
        } catch (Exception | LinkageError | OutOfMemoryError ignored) {
            return false;
        }
    }

    private static void pump() {
        if (worker != null || choosing != null || pending.isEmpty() || !JapaneseGlideDecoder.isReady()) return;
        if (!validPending()) { clearPending(); return; }
        final Stroke job = pending.peek();
        worker = job;
        try {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    JapaneseGlideDecoder.Result[] found = null;
                    try { found = JapaneseGlideDecoder.decodeCandidates(job.path, job.centers, job.codes); }
                    catch (Exception | LinkageError | OutOfMemoryError ignored) { }
                    final JapaneseGlideDecoder.Result[] values = found;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() { recognized(job, values); }
                    });
                }
            }, "JapaneseGlideRecognition");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        } catch (Exception | LinkageError | OutOfMemoryError ignored) {
            worker = null;
            notice(job.view, "グライド候補を作成できませんでした。もう一度なぞってください");
            pending.remove();
            finishHead();
        }
    }

    private static void recognized(final Stroke job, JapaneseGlideDecoder.Result[] values) {
        if (worker == job) worker = null;
        if (job.epoch != epoch || pending.peek() != job) { pump(); return; }
        if (!validPending()) { clearPending(); return; }
        // Filter once before presenting choices. Unsupported readings must never produce
        // a repeated unselectable popup or fall back to inserting raw Latin text.
        if (values != null) {
            JapaneseGlideDecoder.Result[] compatible = new JapaneseGlideDecoder.Result[Math.min(12, values.length)];
            int count = 0;
            for (JapaneseGlideDecoder.Result value : values) {
                if (count == compatible.length) break;
                try {
                    if (value != null && validRomaji(value.romaji) && value.hiragana != null
                            && value.hiragana.equals(Japanese.romajiToHiragana(value.romaji)))
                        compatible[count++] = value;
                } catch (Exception | LinkageError ignored) { }
            }
            values = Arrays.copyOf(compatible, count);
        }
        if (values == null || values.length == 0) {
            notice(job.view, "候補が見つかりませんでした。もう一度なぞるか、タップして入力してください");
            pending.remove();
            finishHead();
            return;
        }
        choosing = job;
        JapaneseGlideCandidates.show(job.view, values, new JapaneseGlideCandidates.Listener() {
            public void choose(JapaneseGlideDecoder.Result value) {
                if (choosing != job || pending.peek() != job || job.epoch != epoch) return;
                choosing = null;
                if (!validPending()) { clearPending(); return; }
                try {
                    job.model.P(job.token, value.romaji, value.romaji, (m) null, false, (Integer) null);
                    expected = Snapshot.read(job.model, job.connection);
                    // The only permitted state rebase is our own selected insertion.
                    if (active != null && active.epoch == epoch && sameEditor(active, job)) active.snapshot = expected;
                    pending.remove();
                    finishHead();
                } catch (Exception | LinkageError | OutOfMemoryError ignored) {
                    notice(job.view, "グライド候補を入力できませんでした");
                    clearPending();
                }
            }
            public void cancel() {
                if (choosing != job || pending.peek() != job) return;
                choosing = null;
                pending.remove();
                finishHead();
            }
        });
    }

    private static void finishHead() {
        if (pending.isEmpty()) expected = null;
        else pump();
    }

    private static void notice(f0 view, String message) {
        try { Toast.makeText(view.getContext(), message, Toast.LENGTH_SHORT).show(); }
        catch (Exception | LinkageError ignored) { }
    }

    private static Stroke begin(f0 view, a token, MotionEvent event) {
        if (!viewReady(view) || view.a == null || view.a.c == null || view.a.c.c != qj0.a.x4
                || !(view.z instanceof x)) return null;
        KeyboardService service = service(view.getContext());
        x model = (x) view.z;
        x0 keys = view.a.c();
        if (service == null || !allowed(service, model) || keys == null
                || keys.b != ResultsFilter.PredictionSearchType.JAPANESE) return null;
        Stroke stroke = new Stroke(view, model, service, token, event);
        if (stroke.width <= 0 || stroke.height <= 0 || stroke.window == null) return null;
        RectF[] rectangles = new RectF[27];
        Matrix matrix = new Matrix();
        for (Object entry : keys.a) {
            j0 key = (j0) entry;
            if (key == null || key.b == null || key.b.b == null || key.b.b.isEmpty()) continue;
            Object raw = key.b.b.get(0);
            if (!(raw instanceof String)) continue;
            String text = (String) raw;
            int index = text.length() == 1 && text.charAt(0) >= 'a' && text.charAt(0) <= 'z'
                    ? text.charAt(0) - 'a' : ("ー".equals(text) || "-".equals(text) ? 26 : -1);
            if (index < 0) continue;
            RectF bounds = key.a.c(matrix);
            if (rectangles[index] != null || !validRect(bounds)) return null;
            rectangles[index] = bounds;
        }
        float[] widths = new float[26], heights = new float[26];
        stroke.left = stroke.top = Float.MAX_VALUE;
        stroke.right = stroke.bottom = -Float.MAX_VALUE;
        for (int i = 0; i < 26; i++) {
            RectF r = rectangles[i];
            if (r == null) return null;
            widths[i] = r.width() * stroke.width;
            heights[i] = r.height() * stroke.height;
            stroke.left = Math.min(stroke.left, r.left * stroke.width);
            stroke.right = Math.max(stroke.right, r.right * stroke.width);
            stroke.top = Math.min(stroke.top, r.top * stroke.height);
            stroke.bottom = Math.max(stroke.bottom, r.bottom * stroke.height);
        }
        Arrays.sort(widths); Arrays.sort(heights);
        float keyWidth = (widths[12] + widths[13]) * .5f;
        float keyHeight = (heights[12] + heights[13]) * .5f;
        stroke.tolerance = Math.min(keyWidth, keyHeight) * .65f;
        float distance = Math.max(6f, Math.min(keyWidth, keyHeight) * .32f);
        stroke.captureDistanceSq = distance * distance;
        int size = rectangles[26] == null ? 26 : 27;
        stroke.centers = new float[size * 2]; stroke.codes = new int[size]; stroke.rects = rectangles;
        for (int i = 0; i < size; i++) {
            stroke.centers[i * 2] = rectangles[i].centerX() * stroke.width;
            stroke.centers[i * 2 + 1] = rectangles[i].centerY() * stroke.height;
            stroke.codes[i] = i == 26 ? '-' : 'a' + i;
        }
        stroke.snapshot = Snapshot.read(model, stroke.connection);
        if (!stroke.startsOnLetter(event.getX(), event.getY()) || !stroke.snapshot.collapsed()
                || !stroke.add(event.getX(), event.getY())) return null;
        return stroke;
    }

    private static boolean viewReady(f0 view) {
        return view.isShown() && view.isAttachedToWindow() && view.j && view.A != null && !view.A.b()
                && view.I != null && view.I.size() == 0;
    }

    private static KeyboardService service(Context context) {
        for (int i = 0; i < 8 && context != null; i++) {
            if (context instanceof KeyboardService) return (KeyboardService) context;
            if (!(context instanceof ContextWrapper)) return null;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) return null;
            context = next;
        }
        return null;
    }

    private static InputConnection connection(KeyboardService service) {
        InputConnection value = service.h;
        return value == null ? service.getCurrentInputConnection() : value;
    }

    private static boolean allowed(KeyboardService service, x model) {
        EditorInfo editor = service.i;
        if (editor == null || connection(service) == null || model.e == null || !model.e.Y || !model.e.L
                || model.e.g || model.e.G != l.b || model.e.c == null || model.e.c.u0()
                || model.i == null || !(model.i.b instanceof b)) return false;
        c mode = ((b) model.i.b).b;
        // Native wb0.x.k0/ac0.b.i suppress learning when vb0.c.b() is true. A private
        // editor is eligible only after SwiftKey has actually entered that native mode.
        if (mode == null || ((editor.imeOptions & 0x1000000) != 0 && !mode.b())) return false;
        int type = editor.inputType, variation = type & 0xff0;
        return (type & 15) == 1 && variation != 128 && variation != 144 && variation != 224
                && (type & 0x80000) == 0;
    }

    private static boolean contextUnchanged(Stroke s) {
        return enabled() && s.epoch == epoch && viewReady(s.view) && s.view.getWindowToken() == s.window
                && s.view.getWidth() == s.width && s.view.getHeight() == s.height && s.view.a == s.layout
                && s.view.a.c != null && s.view.a.c.c == qj0.a.x4 && s.view.a.c() != null
                && s.view.a.c().b == ResultsFilter.PredictionSearchType.JAPANESE && s.view.z == s.model
                && s.service.i == s.editor && connection(s.service) == s.connection && allowed(s.service, s.model)
                && ((b) s.model.i.b).b == s.privacyMode;
    }

    private static boolean unchanged(Stroke s) {
        return contextUnchanged(s) && s.snapshot != null && s.snapshot.matches(s.model);
    }
    private static boolean sameEditor(Stroke a, Stroke b) {
        return a.service == b.service && a.editor == b.editor && a.connection == b.connection && a.model == b.model;
    }
    private static boolean validRect(RectF r) {
        return r != null && r.left >= -.001f && r.top >= -.001f && r.right <= 1.001f && r.bottom <= 1.001f
                && r.width() > 0 && r.height() > 0;
    }
    private static boolean validRomaji(String text) {
        if (text == null || text.length() < 1 || text.length() > 64) return false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch < 'a' || ch > 'z') && ch != '-') return false;
        }
        return true;
    }

    private static final class Snapshot {
        final int start, end;
        final String composition, raw, before, after;
        Snapshot(dc0.a state, InputConnection connection) {
            start = state.k(); end = state.S(); composition = state.s(); raw = state.G();
            before = text(connection.getTextBeforeCursor(48, 0));
            after = text(connection.getTextAfterCursor(16, 0));
        }
        static Snapshot read(x model, InputConnection connection) { return new Snapshot(model.c.C(), connection); }
        private static String text(CharSequence value) { return value == null ? null : value.toString(); }
        private static boolean equal(String a, String b) { return a == null ? b == null : a.equals(b); }
        boolean editorMatches(InputConnection connection) {
            return equal(before, text(connection.getTextBeforeCursor(48, 0)))
                    && equal(after, text(connection.getTextAfterCursor(16, 0)));
        }
        boolean collapsed() { return start == end && composition != null && raw != null; }
        boolean matches(x model) {
            dc0.a state = model.c.C();
            return state.k() == start && state.S() == end && composition != null && raw != null
                    && composition.equals(state.s()) && raw.equals(state.G());
        }
    }

    private static final class Stroke {
        final f0 view; final x model; final KeyboardService service; final a token;
        final EditorInfo editor; final InputConnection connection; final t0 layout;
        final Object window; final c privacyMode; final int width, height, pointer; final long downTime, epoch;
        final float startX, startY;
        float[] centers, path = new float[512]; int[] codes; RectF[] rects;
        Snapshot snapshot;
        float left, top, right, bottom, tolerance, captureDistanceSq, maxDisplacementSq;
        int count; boolean captured;
        Stroke(f0 view, x model, KeyboardService service, a token, MotionEvent event) {
            this.view = view; this.model = model; this.service = service; this.token = token;
            editor = service.i; connection = connection(service); layout = view.a; window = view.getWindowToken();
            privacyMode = ((b) model.i.b).b;
            width = view.getWidth(); height = view.getHeight(); downTime = event.getDownTime();
            pointer = event.getPointerId(0); startX = event.getX(); startY = event.getY(); epoch = JapaneseGlide.epoch;
        }
        boolean startsOnLetter(float x, float y) {
            for (int i = 0; i < 26; i++) if (rects[i].contains(x / width, y / height)) return true;
            return false;
        }
        boolean add(float x, float y) {
            if (Float.isNaN(x) || Float.isNaN(y) || x < left - tolerance || x > right + tolerance
                    || y < top - tolerance || y > bottom + tolerance) return false;
            // Small edge excursions are clamped to the letter area rather than deleting the word.
            x = Math.max(left, Math.min(right, x)); y = Math.max(top, Math.min(bottom, y));
            float dx = x - startX, dy = y - startY;
            maxDisplacementSq = Math.max(maxDisplacementSq, dx * dx + dy * dy);
            if (count > 0) {
                dx = path[(count - 1) * 2] - x; dy = path[(count - 1) * 2 + 1] - y;
                if (dx * dx + dy * dy < 1f) return true;
            }
            if (count == 256) {
                for (int i = 1; i < 128; i++) { path[i * 2] = path[i * 4]; path[i * 2 + 1] = path[i * 4 + 1]; }
                count = 128;
            }
            path[count * 2] = x; path[count * 2 + 1] = y; count++;
            return true;
        }
    }
}
