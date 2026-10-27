package hiro.swiftkey;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputConnection;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import com.microsoft.fluency.ResultsFilter;
import com.touchtype.KeyboardService;
import fc0.n2;
import java.util.Collection;
import java.util.List;
import java.lang.ref.WeakReference;
import lh0.a;
import wb0.x;
import xd0.d1;
import xd0.f0;

/** Recognize upward intent once, then retain that gesture until release.
 * Capturing cancels the native key/timer immediately. Only a verified UP invokes
 * its original long-press actor. No text is stored on disk.
 */
public final class FlickSymbols implements oa.k {
    public static final String KEY = "hiro_up_flick_long_press";
    public static final String DIAGNOSTIC_KEY = "hiro_up_flick_diagnostics";
    // Upper-left, up and upper-right sectors all select the same alternate.
    private static final float UP_SLOPE = .41421356f; // 67.5 degrees from up
    private static final long CAPTURE_MILLIS = 500;
    private static SharedPreferences preferences;
    private static Stroke active;
    // Cancellation owns a complete touch stream, independently of whether its
    // editor snapshot remains valid. A reset must never leak its later UP.
    private static WeakReference<d1> draining;
    private static long drainingDownTime;
    private static boolean cancelling;
    private static long generation;
    // Process-local counts and fixed reason codes only. Never retain key labels,
    // editor identity, composition, surrounding text or exception messages here.
    private static long downs, moves, ups, started, succeeded, rejected, inits, resets;
    private static long captured, abortedAfterCapture, drained;
    private static long directStarts, popupStarts;
    private static String stage = "EVENT_NOT_RECEIVED", lastRejection = "なし", lastException = "なし";
    private static String lastPath = "なし", lastCapture = "なし", lastCaptureResult = "なし";
    private static String lastAlternate = "NOT_CHECKED";
    private FlickSymbols() {}

    public static void init(SharedPreferences value) { preferences = value; ++inits; reset(); }
    public static void reset() {
        if (active != null) {
            String reason = active.captured ? "RESET_CAPTURED" : "RESET_ACTIVE";
            reject(reason);
            captureAborted(active, reason);
        }
        active = null; ++generation; ++resets;
    }
    private static boolean enabled() { return preferences != null && preferences.getBoolean(KEY, false); }

    public static void install(PreferenceScreen screen) {
        try {
            if (screen == null || preferences == null) return;
            if (screen.H(KEY) == null) {
                SwitchPreferenceCompat option = new SwitchPreferenceCompat(screen.a, (AttributeSet) null);
                option.l = KEY;
                option.s = false;
                option.B("上フリックで長押し文字を入力");
                option.A("日本語QWERTYで上・斜め上への動きを認識すると、離した時に長押し用の数字・記号を入力します（例：q→1、a→@）。認識後は指がそれても上フリックを優先します。長押しも使えます。初期設定はオフ。");
                option.G(preferences.getBoolean(KEY, false));
                option.e = new oa.j() {
                    public boolean i(Preference ignored, Object value) {
                        try {
                            if (preferences == null || !(value instanceof Boolean)) return false;
                            preferences.edit().putBoolean(KEY, ((Boolean) value).booleanValue()).apply();
                            reset();
                            return true;
                        } catch (RuntimeException | LinkageError failure) { return false; }
                    }
                };
                screen.G(option);
            }
            Preference diagnostic = screen.H(DIAGNOSTIC_KEY);
            if (diagnostic == null) {
                diagnostic = new Preference(screen.a, (AttributeSet) null);
                diagnostic.l = DIAGNOSTIC_KEY;
                diagnostic.s = false;
                diagnostic.B("上フリック診断をコピー");
                diagnostic.f = new FlickSymbols();
                screen.G(diagnostic);
            }
            diagnostic.A(diagnostics());
        } catch (RuntimeException | LinkageError ignored) {}
    }

    /** Explicit settings click only: the clipboard is never changed by typing. */
    public void k(Preference option) {
        if (option == null) return;
        String report = null;
        try {
            report = diagnostics();
            Object service = option.a.getSystemService(Context.CLIPBOARD_SERVICE);
            if (service instanceof ClipboardManager) {
                ((ClipboardManager) service).setPrimaryClip(ClipData.newPlainText("SwiftKey 上フリック診断", report));
                option.A(report + "\nコピーしました。");
            } else option.A(report + "\nコピーできませんでした。診断表示は更新しました。");
        } catch (RuntimeException | LinkageError ignored) {
            try {
                option.A((report == null ? "上フリック診断を取得できませんでした。" : report)
                        + "\nコピーできませんでした。");
            } catch (RuntimeException | LinkageError unavailable) {}
        }
    }

    private static String diagnostics() {
        String setting;
        try { setting = enabled() ? "ON" : "OFF"; }
        catch (RuntimeException | LinkageError unavailable) { setting = "UNKNOWN"; }
        return "SwiftKey 上フリック 1.8.13\n設定: " + setting
                + "\nイベント: DOWN=" + downs + " MOVE=" + moves + " UP=" + ups
                + "\n開始=" + started + " 成功=" + succeeded + " 拒否=" + rejected
                + "\n認識=" + captured + " 認識後中止=" + abortedAfterCapture + " 消費=" + drained
                + "\n直接開始=" + directStarts + " 候補開始=" + popupStarts
                + "\n初期化=" + inits + " リセット=" + resets
                + "\n最後の段階: " + stage + "\n最後の拒否: " + lastRejection
                + "\n最後の移動拒否: " + lastPath
                + "\n最後の認識: " + lastCapture + "\n最後の認識結果: " + lastCaptureResult
                + "\n最後の候補判定: " + lastAlternate
                + "\n最後の例外: " + lastException
                + "\n入力文字は記録しません。起動後の集計です。";
    }
    private static boolean reject(String reason) {
        ++rejected; lastRejection = reason; stage = reason; return false;
    }
    private static Stroke beginRejected(String reason) { reject(reason); return null; }
    private static void recordPath(String reason, Stroke s, float x, float y) {
        // Relative movement and limits only: no key, absolute position or text.
        lastPath = reason + movement(s, x, y);
    }
    private static String movement(Stroke s, float x, float y) {
        return " dx=" + amount(Math.abs(x - s.startX)) + "px up=" + amount(s.startY - y)
                + "px min=" + amount(s.minimum) + "px minUp=" + amount(s.minimumUp)
                + "px directionDeadzone=" + amount(s.directionSlop)
                + "px slop=" + amount(s.jitterSlop)
                + "px entryAngle=67.5deg captureTimeLimit=" + CAPTURE_MILLIS + "ms mode=CAPTURE_THEN_RELEASE";
    }
    private static String amount(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return Float.toString(value);
        return Double.toString(Math.rint((double) value * 10.0) / 10.0);
    }

    /** An undecided stroke defers glide; an owned stroke consumes its events. */
    public static boolean pending(d1 view) { return active != null && active.view == view; }

    private static boolean terminal(int action) {
        return action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
    }
    private static boolean drains(d1 view, MotionEvent event) {
        return draining != null && draining.get() == view && drainingDownTime == event.getDownTime();
    }
    private static void finishDrain(d1 view, MotionEvent event) {
        if (terminal(event.getActionMasked()) && drains(view, event)) draining = null;
    }
    private static void captureAborted(Stroke s, String reason) {
        if (s.captured && !s.finished) {
            s.finished = true;
            ++abortedAfterCapture;
            lastCaptureResult = reason;
        }
    }
    private static boolean abandon(Stroke s, String reason, MotionEvent event) {
        if (active == s) active = null;
        reject(reason);
        captureAborted(s, reason);
        if (s.captured) finishDrain(s.view, event);
        return s.captured;
    }

    public static boolean handle(d1 view, a token, MotionEvent event, boolean glideOwned) {
        if (cancelling || event == null) return false;
        Stroke s = null;
        boolean owned = false;
        try {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) ++downs;
            else if (action == MotionEvent.ACTION_MOVE) ++moves;
            else if (action == MotionEvent.ACTION_UP) ++ups;
            if (action == MotionEvent.ACTION_DOWN) {
                // A fresh DOWN is a new stream, including after a missing UP.
                if (active != null) captureAborted(active, "NEW_DOWN");
                active = null;
                draining = null;
                stage = "DOWN";
                if (glideOwned) return reject("DOWN_GLIDE_OWNED");
                if (!enabled()) return reject("DOWN_DISABLED");
                if (!(view instanceof f0)) return reject("DOWN_VIEW_TYPE");
                if (event.getPointerCount() != 1) return reject("DOWN_POINTER_COUNT");
                if (event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER
                        && event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) return reject("DOWN_TOOL");
                active = begin((f0) view, event);
                if (active != null) { ++started; stage = "TRACKING"; }
                return false;
            }
            owned = drains(view, event);
            s = active;
            if (owned && (s == null || s.view != view || !s.captured || s.downTime != event.getDownTime())) {
                // A lifecycle change invalidated insertion after native CANCEL.
                // Keep swallowing this stream without retaining its editor data.
                ++drained;
                finishDrain(view, event);
                return true;
            }
            // Preserve the DOWN refusal rather than replacing it on every MOVE.
            if (s == null) return false;
            if (s.view != view) return reject("EVENT_VIEW");
            owned = s.captured;
            String reason = null;
            if (glideOwned) reason = "EVENT_GLIDE_OWNED";
            else if (action == MotionEvent.ACTION_CANCEL) reason = "EVENT_CANCEL";
            else if (!enabled()) reason = "EVENT_DISABLED";
            else if (event.getPointerCount() != 1) reason = "EVENT_POINTER_COUNT";
            else if (event.getPointerId(0) != s.pointer) reason = "EVENT_POINTER_ID";
            else if (event.getDownTime() != s.downTime) reason = "EVENT_DOWN_TIME";
            else {
                String changed = changedReason(s);
                if (changed != null) reason = "EVENT_" + changed;
                else if (event.getEventTime() < s.downTime
                        || (!s.captured && event.getEventTime() - s.downTime > CAPTURE_MILLIS))
                    reason = "EVENT_TIMEOUT";
            }
            if (reason != null) return abandon(s, reason, event);
            if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP)
                return abandon(s, "EVENT_ACTION", event);
            for (int i = 0; i < event.getHistorySize(); i++) {
                float x = event.getHistoricalX(0, i), y = event.getHistoricalY(0, i);
                String path = s.add(x, y);
                if (path != null) {
                    reason = "PATH_HISTORY_" + path;
                    recordPath(reason, s, x, y);
                    return abandon(s, reason, event);
                }
                if (s.qualified && !s.captured && !capture(s, token, event, "HISTORY", x, y)) return s.captured;
            }
            float x = event.getX(), y = event.getY();
            String path = s.add(x, y);
            if (path != null) {
                reason = "PATH_CURRENT_" + path;
                recordPath(reason, s, x, y);
                return abandon(s, reason, event);
            }
            if (s.qualified && !s.captured && !capture(s, token, event,
                    action == MotionEvent.ACTION_UP ? "UP" : "MOVE", x, y)) return s.captured;
            if (!s.captured) {
                if (action == MotionEvent.ACTION_UP)
                    return abandon(s, s.distanceMet ? "UP_DIRECTION" : "UP_TOO_SHORT", event);
                stage = "TRACKING";
                return false;
            }
            owned = true;
            if (action != MotionEvent.ACTION_UP) { stage = "CAPTURED"; return true; }
            // Geometry no longer votes after recognition. Revalidate only the
            // editor and original action before inserting exactly once on UP.
            String invalid = insertionReason(s, token);
            if (invalid != null) return abandon(s, "UP_" + invalid, event);
            active = null;
            finishDrain(view, event);
            stage = "INSERTING";
            s.actor.b(token);
            s.finished = true;
            ++succeeded; stage = "SUCCESS"; lastCaptureResult = "SUCCESS";
            return true;
        } catch (Exception | LinkageError | OutOfMemoryError failure) {
            // capture() marks ownership before invoking native code, so even a
            // throwing CANCEL or a reentrant reset cannot leak its later UP.
            owned = owned || (s != null && s.captured);
            if (active == s) active = null;
            lastException = failure.getClass().getName();
            String reason = owned ? "EXCEPTION_AFTER_CANCEL" : "EXCEPTION_BEFORE_CANCEL";
            reject(reason);
            if (s != null) captureAborted(s, reason);
            if (owned) finishDrain(view, event);
            return owned;
        }
    }

    private static String insertionReason(Stroke s, a token) {
        if (!enabled()) return "DISABLED";
        String changed = changedReason(s);
        if (changed != null) return changed;
        if (!s.editorMatches()) return "EDITOR_TEXT_CHANGED";
        if (alternate(s.key, s.model, s) != s.actor) return "ACTOR_CHANGED";
        if (token == null) return "TOKEN";
        return null;
    }
    private static boolean capture(Stroke s, a token, MotionEvent event, String source, float x, float y) {
        String invalid = insertionReason(s, token);
        if (invalid != null) {
            abandon(s, "CAPTURE_" + invalid, event);
            return false;
        }
        String recognition = source + movement(s, x, y);
        MotionEvent cancel = MotionEvent.obtain(event);
        try {
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            // Install the drain BEFORE native code can reset us or throw.
            draining = new WeakReference<d1>(s.view);
            drainingDownTime = s.downTime;
            s.captured = true;
            ++captured;
            lastCapture = recognition;
            lastCaptureResult = "CAPTURED";
            cancelling = true;
            stage = "NATIVE_CANCEL";
            try { s.view.f(token, cancel); }
            finally { cancelling = false; }
        } finally { cancel.recycle(); }
        invalid = insertionReason(s, token);
        if (invalid != null) {
            abandon(s, "POST_CANCEL_" + invalid, event);
            return false;
        }
        stage = "CAPTURED";
        return true;
    }

    private static Stroke begin(f0 view, MotionEvent event) {
        String readiness = readyReason(view);
        if (readiness != null) return beginRejected("BEGIN_" + readiness);
        if (!(view.a instanceof fb0.f0)) return beginRejected("BEGIN_LAYOUT_TYPE");
        if (view.a.c == null || view.a.c.c != qj0.a.x4) return beginRejected("BEGIN_LAYOUT_NOT_ROMAJI");
        if (view.a.c() == null || view.a.c().b != ResultsFilter.PredictionSearchType.JAPANESE)
            return beginRejected("BEGIN_SEARCH_NOT_JAPANESE");
        if (!(view.z instanceof x)) return beginRejected("BEGIN_MODEL_TYPE");
        KeyboardService service = service(view.getContext());
        if (service == null) return beginRejected("BEGIN_SERVICE");
        if (service.i == null) return beginRejected("BEGIN_EDITOR");
        if (connection(service) == null) return beginRejected("BEGIN_CONNECTION");
        int type = service.i.inputType, variation = type & 0xff0;
        if ((type & 15) != 1 || variation == 128 || variation == 144 || variation == 224)
            return beginRejected("BEGIN_INPUT_TYPE");
        int w = view.getWidth(), h = view.getHeight();
        if (w <= 0 || h <= 0 || view.getWindowToken() == null) return beginRejected("BEGIN_VIEW_SIZE");
        if (event.getX() < 0 || event.getX() >= w || event.getY() < 0 || event.getY() >= h)
            return beginRejected("BEGIN_OUTSIDE_VIEW");
        n2 chosen = null;
        for (Object object : ((fb0.f0) view.a).d) {
            if (!(object instanceof n2)) continue;
            n2 key = (n2) object;
            if (key.e == null || !validRect(key.e.a) || !key.e.a.contains(event.getX()/w, event.getY()/h)) continue;
            if (chosen != null) return beginRejected("BEGIN_KEY_OVERLAP");
            chosen = key;
        }
        if (chosen == null) return beginRejected("BEGIN_KEY_NONE");
        ic0.a actor = alternate(chosen, (x) view.z, null);
        if (actor == null) return beginRejected("BEGIN_ALTERNATE_NONE");
        Stroke s = new Stroke(view, service, (x) view.z, chosen, actor, event);
        if (!(s.minimum > 0) || !s.snapshotValid()) return beginRejected("BEGIN_SNAPSHOT");
        // Bind the route as well as the actor. Reparenting the same actor or
        // changing a popup's default index must not change an in-flight flick.
        if (alternate(chosen, s.model, s) != actor) return beginRejected("BEGIN_ALTERNATE_CHANGED");
        if (actor instanceof fc0.d1) ++directStarts;
        else ++popupStarts;
        return s;
    }

    private static ic0.a alternateRejected(String reason) {
        lastAlternate = reason;
        return null;
    }

    /** Resolve an existing native insertion actor without opening a popup.
     * Single-character long presses have a direct release actor. Multi-choice
     * long presses instead open a mini keyboard whose default entry owns its
     * own insertion actor. Those are distinct native routes, not sensitivities.
     */
    private static ic0.a alternate(n2 key, x model, Stroke snapshot) {
        if (!(key.b instanceof oc0.m) || key.d == null) return alternateRejected("KEY_HANDLER");
        hc0.a handler = ((oc0.m) key.b).b;
        if (handler == null || handler.c == null || handler.c.d == null)
            return alternateRejected("LONG_PRESS_HANDLER");
        List actions = hc0.o.a(handler.c.d, key.d);
        if (actions == null) return alternateRejected("DIRECT_ACTIONS");
        fc0.d1 selected = null;
        for (Object action : actions) {
            if (!(action instanceof ic0.e) || !(((ic0.e) action).a instanceof fc0.d1)) continue;
            fc0.d1 candidate = (fc0.d1) ((ic0.e) action).a;
            if (selected != null) return alternateRejected("DIRECT_AMBIGUOUS");
            if (!candidate.d || !singleCharacter(candidate.b) || !singleCharacter(candidate.c))
                return alternateRejected("DIRECT_CHARACTER");
            selected = candidate;
        }
        if (selected != null)
            return bindAlternate(selected, snapshot, null, null, null, null, null, -1, "DIRECT");

        if (handler.c.b == null) return alternateRejected("POPUP_START_ACTIONS");
        List starts = hc0.o.a(handler.c.b, key.d);
        if (starts == null) return alternateRejected("POPUP_START_ACTIONS");
        fc0.b1 opening = null;
        e60.b binding = null;
        androidx.lifecycle.h provider = null;
        xc0.f popupSource = null;
        for (Object action : starts) {
            if (!(action instanceof ic0.e) || !(((ic0.e) action).a instanceof fc0.b1)) continue;
            fc0.b1 candidate = (fc0.b1) ((ic0.e) action).a;
            if (opening != null) return alternateRejected("POPUP_AMBIGUOUS");
            if (candidate.a != 1 || !(candidate.b instanceof e60.b))
                return alternateRejected("POPUP_OPENER");
            e60.b candidateBinding = (e60.b) candidate.b;
            if (candidateBinding.b != key.d || !(candidateBinding.a instanceof androidx.lifecycle.h))
                return alternateRejected("POPUP_BINDING");
            androidx.lifecycle.h candidateProvider = (androidx.lifecycle.h) candidateBinding.a;
            if (candidateProvider.a != 7 || !(candidateProvider.b instanceof xc0.f))
                return alternateRejected("POPUP_PROVIDER");
            opening = candidate;
            binding = candidateBinding;
            provider = candidateProvider;
            popupSource = (xc0.f) candidateProvider.b;
        }
        if (popupSource == null) return alternateRejected("POPUP_NONE");
        fb0.f2 popup = popupSource.a;
        if (popup == null || popup.d == null || popup.l < 0 || popup.l >= popup.d.size())
            return alternateRejected("POPUP_DEFAULT_INDEX");
        Object defaultEntry = popup.d.get(popup.l);
        if (!(defaultEntry instanceof n2)) return alternateRejected("POPUP_DEFAULT_KEY");
        n2 popupKey = (n2) defaultEntry;
        if (popupKey.d == null || !(popupKey.b instanceof oc0.m))
            return alternateRejected("POPUP_KEY_HANDLER");
        hc0.a popupHandler = ((oc0.m) popupKey.b).b;
        if (popupHandler == null || popupHandler.b == null || !(popupHandler.b.b instanceof Collection))
            return alternateRejected("POPUP_RELEASE_ACTIONS");
        List releases = hc0.o.a((Collection) popupHandler.b.b, popupKey.d);
        if (releases == null) return alternateRejected("POPUP_RELEASE_ACTIONS");
        fb0.l2 popupActor = null;
        for (Object action : releases) {
            // Unlike the single-character route, normal popup-key releases
            // contain the actor directly, not an ic0.e long-press wrapper.
            if (!(action instanceof fb0.l2)) continue;
            fb0.l2 candidate = (fb0.l2) action;
            if (popupActor != null) return alternateRejected("POPUP_ACTOR_AMBIGUOUS");
            if (candidate.a == null || candidate.a.a != model)
                return alternateRejected("POPUP_ACTOR_MODEL");
            if (!candidate.d || !candidate.e || !singleCharacter(candidate.b) || !singleCharacter(candidate.c))
                return alternateRejected("POPUP_CHARACTER");
            popupActor = candidate;
        }
        if (popupActor == null) return alternateRejected("POPUP_ACTOR_NONE");
        return bindAlternate(popupActor, snapshot, opening, binding, provider, popup, popupKey,
                popup.l, "POPUP_DEFAULT");
    }

    private static ic0.a bindAlternate(ic0.a actor, Stroke snapshot, Object opening, Object binding,
            Object provider, fb0.f2 popup, n2 popupKey, int popupIndex, String route) {
        if (snapshot != null) {
            if (snapshot.actor != actor) return alternateRejected("ACTOR_CHANGED");
            if (snapshot.alternateBound) {
                if (snapshot.popupOpening != opening || snapshot.popupBinding != binding
                        || snapshot.popupProvider != provider || snapshot.popup != popup
                        || snapshot.popupKey != popupKey || snapshot.popupIndex != popupIndex)
                    return alternateRejected("ALTERNATE_ROUTE_CHANGED");
            } else {
                snapshot.popupOpening = opening;
                snapshot.popupBinding = binding;
                snapshot.popupProvider = provider;
                snapshot.popup = popup;
                snapshot.popupKey = popupKey;
                snapshot.popupIndex = popupIndex;
                snapshot.alternateBound = true;
            }
        }
        lastAlternate = route;
        return actor;
    }
    private static boolean singleCharacter(String s) {
        return s != null && !s.isEmpty() && s.codePointCount(0, s.length()) == 1
                && !Character.isISOControl(s.codePointAt(0));
    }
    private static boolean validRect(RectF r) {
        // Native row widths accumulate float rounding (e.g. ten 0.1f keys end
        // at 1.000000119f). Accept only that tiny boundary error, not bad bounds.
        final float epsilon = .0001f;
        return r != null && r.left >= -epsilon && r.top >= -epsilon
                && r.right <= 1 + epsilon && r.bottom <= 1 + epsilon
                && r.width() > 0 && r.height() > 0;
    }
    private static String readyReason(f0 view) {
        if (!view.isShown() || !view.isAttachedToWindow() || !view.j) return "VIEW_NOT_READY";
        if (view.A == null) return "ACCESSIBILITY_STATE";
        if (view.A.b()) return "TOUCH_EXPLORATION";
        if (view.I == null || view.I.size() != 0) return "POPUP_ROUTING";
        return null;
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
    private static InputConnection connection(KeyboardService s) {
        return s.h == null ? s.getCurrentInputConnection() : s.h;
    }
    private static String changedReason(Stroke s) {
        if (s.generation != generation) return "RESET";
        String readiness = readyReason(s.view);
        if (readiness != null) return readiness;
        if (s.view.a != s.layout) return "LAYOUT_CHANGED";
        if (s.view.z != s.model) return "MODEL_CHANGED";
        if (s.view.getWidth() != s.width || s.view.getHeight() != s.height) return "SIZE_CHANGED";
        if (s.view.getWindowToken() != s.window) return "WINDOW_CHANGED";
        if (s.service.i != s.editor || s.service.i.inputType != s.inputType
                || s.service.i.imeOptions != s.imeOptions) return "EDITOR_CHANGED";
        if (connection(s.service) != s.connection) return "CONNECTION_CHANGED";
        if (s.layout.c == null || s.layout.c.c != qj0.a.x4) return "LAYOUT_NOT_ROMAJI";
        if (s.layout.c() == null || s.layout.c().b != ResultsFilter.PredictionSearchType.JAPANESE)
            return "SEARCH_NOT_JAPANESE";
        if (!((fb0.f0) s.layout).d.contains(s.key)) return "KEY_REMOVED";
        if (!s.geometryMatches()) return "GEOMETRY_CHANGED";
        if (s.model.c == null || !s.matches(s.model.c.C())) return "MODEL_STATE_CHANGED";
        return null;
    }
    private static String text(CharSequence s) { return s == null ? null : s.toString(); }
    private static boolean equal(String a, String b) { return a == null ? b == null : a.equals(b); }

    private static final class Stroke {
        final f0 view; final KeyboardService service; final x model; final fb0.t0 layout;
        final Object window, editor; final InputConnection connection; final n2 key; final ic0.a actor;
        final int width, height, pointer, start, end, inputType, imeOptions;
        final long downTime, generation;
        final float startX, startY, minimum, minimumUp, jitterSlop, directionSlop;
        final float left, top, right, bottom;
        final String composition, raw, before, after;
        Object popupOpening, popupBinding, popupProvider;
        fb0.f2 popup; n2 popupKey;
        int popupIndex = -1;
        boolean alternateBound;
        boolean qualified, distanceMet, captured, finished;
        Stroke(f0 view, KeyboardService service, x model, n2 key, ic0.a actor, MotionEvent event) {
            this.view=view; this.service=service; this.model=model; this.key=key; this.actor=actor;
            layout=view.a; window=view.getWindowToken(); editor=service.i; connection=connection(service);
            inputType=service.i.inputType; imeOptions=service.i.imeOptions; generation=FlickSymbols.generation;
            width=view.getWidth(); height=view.getHeight(); pointer=event.getPointerId(0); downTime=event.getDownTime();
            startX=event.getX(); startY=event.getY();
            RectF rect=key.e.a;
            left=rect.left; top=rect.top; right=rect.right; bottom=rect.bottom;
            float kw=rect.width()*width, kh=rect.height()*height;
            float jitter = 0f;
            try {
                // Android's density-aware movement tolerance protects contact
                // and lift jitter; never allow an anomalous value over half a key.
                jitter = Math.max(0f, Math.min((float) ViewConfiguration.get(view.getContext()).getScaledTouchSlop(),
                        Math.min(kw, kh) * .5f));
            } catch (RuntimeException | LinkageError unavailable) {
                // Keep the previous limits if configuration is unavailable.
            }
            jitterSlop = jitter;
            minimum=Math.max(6f, Math.min(kw,kh)*.24f);
            // Locking a wide diagonal sector needs meaningful upward travel;
            // mostly sideways contact jitter must not permanently become a flick.
            minimumUp=Math.max(minimum*.5f,jitterSlop*.5f);
            // Brief contact drift does not settle the gesture's direction.
            // Clearly horizontal/downward movement still hands off to native/glide.
            directionSlop=Math.max(minimum*2f,jitterSlop*2f);
            dc0.a state=model.c.C();
            start=state.k(); end=state.S(); composition=state.s(); raw=state.G();
            before=text(connection.getTextBeforeCursor(32,0)); after=text(connection.getTextAfterCursor(16,0));
        }
        boolean snapshotValid() { return start==end && composition!=null && raw!=null; }
        boolean geometryMatches() {
            RectF r=key.e.a;
            return r!=null && r.left==left && r.top==top && r.right==right && r.bottom==bottom;
        }
        boolean matches(dc0.a state) {
            return state!=null && start==state.k() && end==state.S()
                    && equal(composition,state.s()) && equal(raw,state.G());
        }
        boolean editorMatches() {
            return equal(before,text(connection.getTextBeforeCursor(32,0)))
                    && equal(after,text(connection.getTextAfterCursor(16,0)));
        }
        String add(float x, float y) {
            if (Float.isNaN(x)||Float.isNaN(y)||Float.isInfinite(x)||Float.isInfinite(y)) return "NON_FINITE";
            if (captured) return null;
            double dx=Math.abs((double)x-startX), up=(double)startY-y;
            double distanceSq=dx*dx+up*up;
            distanceMet=distanceSq>=(double)minimum*minimum;
            if (distanceMet && up>=minimumUp && up>=dx*UP_SLOPE) {
                qualified=true;
                return null;
            }
            if (distanceSq>=(double)directionSlop*directionSlop)
                return up<0 ? "DOWN" : "HORIZONTAL";
            return null;
        }
    }
}
