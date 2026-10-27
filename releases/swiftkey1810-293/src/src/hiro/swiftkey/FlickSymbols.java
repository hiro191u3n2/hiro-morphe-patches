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
import java.util.List;
import lh0.a;
import wb0.x;
import xd0.d1;
import xd0.f0;

/** A short upward stroke selects the native key's single long-press character.
 * Native DOWN/MOVE remain intact. Only a verified UP cancels native touch and
 * invokes the same insertion actor used by long press. No text is stored on disk.
 */
public final class FlickSymbols implements oa.k {
    public static final String KEY = "hiro_up_flick_long_press";
    public static final String DIAGNOSTIC_KEY = "hiro_up_flick_diagnostics";
    private static SharedPreferences preferences;
    private static Stroke active;
    private static boolean cancelling;
    private static long generation;
    // Process-local counts and fixed reason codes only. Never retain key labels,
    // editor identity, composition, surrounding text or exception messages here.
    private static long downs, moves, ups, started, succeeded, rejected, inits, resets;
    private static String stage = "EVENT_NOT_RECEIVED", lastRejection = "なし", lastException = "なし";
    private static String lastPath = "なし";
    private FlickSymbols() {}

    public static void init(SharedPreferences value) { preferences = value; ++inits; reset(); }
    public static void reset() {
        if (active != null) reject("RESET_ACTIVE");
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
                option.A("日本語QWERTYのキーを短く上へフリックすると、長押し用の数字・記号を入力します（例：q→1、a→@）。長押しも使えます。初期設定はオフ。");
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
        return "SwiftKey 上フリック 1.8.10\n設定: " + setting
                + "\nイベント: DOWN=" + downs + " MOVE=" + moves + " UP=" + ups
                + "\n開始=" + started + " 成功=" + succeeded + " 拒否=" + rejected
                + "\n初期化=" + inits + " リセット=" + resets
                + "\n最後の段階: " + stage + "\n最後の拒否: " + lastRejection
                + "\n最後の移動拒否: " + lastPath
                + "\n最後の例外: " + lastException
                + "\n入力文字は記録しません。起動後の集計です。";
    }
    private static boolean reject(String reason) {
        ++rejected; lastRejection = reason; stage = reason; return false;
    }
    private static Stroke beginRejected(String reason) { reject(reason); return null; }
    private static boolean rejectPath(String reason, Stroke s, float x, float y) {
        // Relative movement and the limits from this exact failed sample only.
        // No touch position, key label, editor text or app identity is reported.
        float up = s.startY - y;
        lastPath = reason + " dx=" + amount(Math.abs(x - s.startX))
                + "px up=" + amount(up) + "px reverse=" + amount(s.furthestUp - up)
                + "px min=" + amount(s.minimum) + "px horizontal=" + amount(s.maxHorizontal)
                + "px down=" + amount(s.maxDown) + "px upLimit=" + amount(s.maxUp)
                + "px reverseLimit=" + amount(s.maxReverse) + "px slop=" + amount(s.jitterSlop) + "px";
        return reject(reason);
    }
    private static String amount(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return Float.toString(value);
        return Double.toString(Math.rint((double) value * 10.0) / 10.0);
    }

    /** True only while an undecided stroke must defer JapaneseGlide capture. */
    public static boolean pending(d1 view) { return active != null && active.view == view; }

    public static boolean handle(d1 view, a token, MotionEvent event, boolean glideOwned) {
        if (cancelling || event == null) return false;
        boolean cancelled = false;
        try {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) ++downs;
            else if (action == MotionEvent.ACTION_MOVE) ++moves;
            else if (action == MotionEvent.ACTION_UP) ++ups;
            if (action == MotionEvent.ACTION_DOWN) {
                active = null;
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
            Stroke s = active;
            // Keep the reason which rejected DOWN; subsequent native MOVE/UP
            // must not replace it with an unhelpful "no active stroke" result.
            if (s == null) return false;
            if (s.view != view) return reject("EVENT_VIEW");
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
                else if (event.getEventTime() - s.downTime > 350 || event.getEventTime() < s.downTime)
                    reason = "EVENT_TIMEOUT";
            }
            if (reason != null) { active = null; return reject(reason); }
            if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP) {
                active = null;
                return reject("EVENT_ACTION");
            }
            for (int i = 0; i < event.getHistorySize(); i++) {
                float x = event.getHistoricalX(0, i), y = event.getHistoricalY(0, i);
                String path = s.add(x, y);
                if (path != null) {
                    active = null;
                    return rejectPath("PATH_HISTORY_" + path, s, x, y);
                }
            }
            String path = s.add(event.getX(), event.getY());
            if (path != null) {
                active = null;
                return rejectPath("PATH_CURRENT_" + path, s, event.getX(), event.getY());
            }
            if (action != MotionEvent.ACTION_UP) { stage = "TRACKING"; return false; }
            active = null;
            float dx = Math.abs(event.getX() - s.startX), up = s.startY - event.getY();
            if (up < s.minimum) return reject("UP_TOO_SHORT");
            if (up < dx * 1.35f) return reject("UP_DIRECTION");
            if (!s.editorMatches()) return reject("UP_EDITOR_TEXT_CHANGED");
            // Re-resolve current restrictions; never retain a stale action across a layout/editor change.
            if (alternate(s.key) != s.actor) return reject("UP_ACTOR_CHANGED");
            if (token == null) return reject("UP_TOKEN");
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            cancelling = true;
            cancelled = true;
            stage = "NATIVE_CANCEL";
            try { view.f(token, cancel); }
            finally { cancelling = false; cancel.recycle(); }
            if (!enabled()) { reject("POST_CANCEL_DISABLED"); return true; }
            String changed = changedReason(s);
            if (changed != null) { reject("POST_CANCEL_" + changed); return true; }
            if (!s.editorMatches()) { reject("POST_CANCEL_EDITOR_TEXT_CHANGED"); return true; }
            if (alternate(s.key) != s.actor) { reject("POST_CANCEL_ACTOR_CHANGED"); return true; }
            stage = "INSERTING";
            s.actor.b(token);
            ++succeeded; stage = "SUCCESS";
            return true;
        } catch (Exception | LinkageError | OutOfMemoryError failure) {
            active = null;
            lastException = failure.getClass().getName();
            reject(cancelled ? "EXCEPTION_AFTER_CANCEL" : "EXCEPTION_BEFORE_CANCEL");
            // Once native touch is cancelled, never also deliver UP and double-insert.
            return cancelled;
        }
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
        fc0.d1 actor = alternate(chosen);
        if (actor == null) return beginRejected("BEGIN_ALTERNATE_NONE");
        Stroke s = new Stroke(view, service, (x) view.z, chosen, actor, event);
        return s.minimum > 0 && s.snapshotValid() ? s : beginRejected("BEGIN_SNAPSHOT");
    }

    /** Reads the same restricted release action list used by native long press. */
    private static fc0.d1 alternate(n2 key) {
        if (!(key.b instanceof oc0.m) || key.d == null) return null;
        hc0.a handler = ((oc0.m) key.b).b;
        if (handler == null || handler.c == null || handler.c.d == null) return null;
        List actions = hc0.o.a(handler.c.d, key.d);
        fc0.d1 selected = null;
        for (Object action : actions) {
            if (!(action instanceof ic0.e) || !(((ic0.e) action).a instanceof fc0.d1)) continue;
            fc0.d1 candidate = (fc0.d1) ((ic0.e) action).a;
            if (selected != null || !candidate.d || !singleCharacter(candidate.b) || !singleCharacter(candidate.c)) return null;
            selected = candidate;
        }
        return selected;
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
        final Object window, editor; final InputConnection connection; final n2 key; final fc0.d1 actor;
        final int width, height, pointer, start, end, inputType, imeOptions;
        final long downTime, generation;
        final float startX, startY, minimum, maxHorizontal, maxUp, maxDown, maxReverse, jitterSlop;
        final float left, top, right, bottom;
        final String composition, raw, before, after;
        float furthestUp;
        Stroke(f0 view, KeyboardService service, x model, n2 key, fc0.d1 actor, MotionEvent event) {
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
            maxHorizontal=Math.max(6f,kw*.38f); maxUp=kh*1.25f; maxDown=Math.max(kh*.12f,jitterSlop);
            // Retain the previous size-based allowance and respect device-scaled jitter.
            maxReverse=Math.max(Math.max(4f, Math.min(kw,kh)*.21f),jitterSlop);
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
            float dx=Math.abs(x-startX), up=startY-y;
            if (dx>maxHorizontal) return "HORIZONTAL";
            if (up < -maxDown) return "DOWN";
            if (up>maxUp) return "UP_LIMIT";
            if (furthestUp-up>maxReverse) return "REVERSE";
            furthestUp=Math.max(furthestUp,up);
            return null;
        }
    }
}
