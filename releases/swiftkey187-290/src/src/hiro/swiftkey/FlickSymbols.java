package hiro.swiftkey;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
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
public final class FlickSymbols {
    public static final String KEY = "hiro_up_flick_long_press";
    private static SharedPreferences preferences;
    private static Stroke active;
    private static boolean cancelling;
    private static long generation;
    private FlickSymbols() {}

    public static void init(SharedPreferences value) { preferences = value; reset(); }
    public static void reset() { active = null; ++generation; }
    private static boolean enabled() { return preferences != null && preferences.getBoolean(KEY, false); }

    public static void install(PreferenceScreen screen) {
        try {
            if (screen == null || preferences == null || screen.H(KEY) != null) return;
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
        } catch (RuntimeException | LinkageError ignored) {}
    }

    /** True only while an undecided stroke must defer JapaneseGlide capture. */
    public static boolean pending(d1 view) { return active != null && active.view == view; }

    public static boolean handle(d1 view, a token, MotionEvent event, boolean glideOwned) {
        if (cancelling || event == null) return false;
        boolean cancelled = false;
        try {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                active = null;
                if (!glideOwned && enabled() && view instanceof f0 && event.getPointerCount() == 1
                        && (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                            || event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS)) {
                    active = begin((f0) view, event);
                }
                return false;
            }
            Stroke s = active;
            if (s == null || s.view != view) return false;
            if (glideOwned || action == MotionEvent.ACTION_CANCEL || !enabled()
                    || event.getPointerCount() != 1 || event.getPointerId(0) != s.pointer
                    || event.getDownTime() != s.downTime || !unchanged(s)
                    || event.getEventTime() - s.downTime > 350 || event.getEventTime() < s.downTime) {
                active = null;
                return false;
            }
            if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP) {
                active = null;
                return false;
            }
            for (int i = 0; i < event.getHistorySize(); i++) {
                if (!s.add(event.getHistoricalX(0, i), event.getHistoricalY(0, i))) {
                    active = null;
                    return false;
                }
            }
            if (!s.add(event.getX(), event.getY())) { active = null; return false; }
            if (action != MotionEvent.ACTION_UP) return false;
            active = null;
            float dx = Math.abs(event.getX() - s.startX), up = s.startY - event.getY();
            if (up < s.minimum || up < dx * 1.8f || !s.editorMatches()) return false;
            // Re-resolve current restrictions; never retain a stale action across a layout/editor change.
            if (alternate(s.key) != s.actor || token == null) return false;
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            cancelling = true;
            cancelled = true;
            try { view.f(token, cancel); }
            finally { cancelling = false; cancel.recycle(); }
            if (enabled() && unchanged(s) && s.editorMatches() && alternate(s.key) == s.actor) s.actor.b(token);
            return true;
        } catch (Exception | LinkageError | OutOfMemoryError ignored) {
            active = null;
            // Once native touch is cancelled, never also deliver UP and double-insert.
            return cancelled;
        }
    }

    private static Stroke begin(f0 view, MotionEvent event) {
        if (!ready(view) || !(view.a instanceof fb0.f0) || view.a.c == null
                || view.a.c.c != qj0.a.x4 || view.a.c() == null
                || view.a.c().b != ResultsFilter.PredictionSearchType.JAPANESE || !(view.z instanceof x)) return null;
        KeyboardService service = service(view.getContext());
        if (service == null || service.i == null || connection(service) == null) return null;
        int type = service.i.inputType, variation = type & 0xff0;
        if ((type & 15) != 1 || variation == 128 || variation == 144 || variation == 224) return null;
        int w = view.getWidth(), h = view.getHeight();
        if (w <= 0 || h <= 0 || view.getWindowToken() == null) return null;
        if (event.getX() < 0 || event.getX() >= w || event.getY() < 0 || event.getY() >= h) return null;
        n2 chosen = null;
        for (Object object : ((fb0.f0) view.a).d) {
            if (!(object instanceof n2)) continue;
            n2 key = (n2) object;
            if (key.e == null || !validRect(key.e.a) || !key.e.a.contains(event.getX()/w, event.getY()/h)) continue;
            if (chosen != null) return null;
            chosen = key;
        }
        if (chosen == null) return null;
        fc0.d1 actor = alternate(chosen);
        if (actor == null) return null;
        Stroke s = new Stroke(view, service, (x) view.z, chosen, actor, event);
        return s.minimum > 0 && s.snapshotValid() ? s : null;
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
        return r != null && r.left >= 0 && r.top >= 0 && r.right <= 1 && r.bottom <= 1
                && r.width() > 0 && r.height() > 0;
    }
    private static boolean ready(f0 view) {
        return view.isShown() && view.isAttachedToWindow() && view.j && view.A != null
                && !view.A.b() && view.I != null && view.I.size() == 0;
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
    private static boolean unchanged(Stroke s) {
        return s.generation == generation && ready(s.view) && s.view.a == s.layout && s.view.z == s.model
                && s.view.getWidth() == s.width && s.view.getHeight() == s.height
                && s.view.getWindowToken() == s.window && s.service.i == s.editor
                && s.service.i.inputType == s.inputType && s.service.i.imeOptions == s.imeOptions
                && connection(s.service) == s.connection && s.layout.c != null && s.layout.c.c == qj0.a.x4
                && s.layout.c() != null && s.layout.c().b == ResultsFilter.PredictionSearchType.JAPANESE
                && ((fb0.f0) s.layout).d.contains(s.key) && s.geometryMatches()
                && s.model.c != null && s.matches(s.model.c.C());
    }
    private static String text(CharSequence s) { return s == null ? null : s.toString(); }
    private static boolean equal(String a, String b) { return a == null ? b == null : a.equals(b); }

    private static final class Stroke {
        final f0 view; final KeyboardService service; final x model; final fb0.t0 layout;
        final Object window, editor; final InputConnection connection; final n2 key; final fc0.d1 actor;
        final int width, height, pointer, start, end, inputType, imeOptions;
        final long downTime, generation;
        final float startX, startY, minimum, maxHorizontal, maxUp, maxDown;
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
            minimum=Math.max(8f, Math.min(kw,kh)*.42f);
            maxHorizontal=Math.max(6f,kw*.28f); maxUp=kh*1.25f; maxDown=kh*.12f;
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
        boolean add(float x, float y) {
            if (Float.isNaN(x)||Float.isNaN(y)||Float.isInfinite(x)||Float.isInfinite(y)) return false;
            float dx=Math.abs(x-startX), up=startY-y;
            if (dx>maxHorizontal || up < -maxDown || up>maxUp || furthestUp-up>minimum*.5f) return false;
            furthestUp=Math.max(furthestUp,up);
            return true;
        }
    }
}
