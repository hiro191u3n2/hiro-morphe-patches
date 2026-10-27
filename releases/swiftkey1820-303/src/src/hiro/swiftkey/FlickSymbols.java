package hiro.swiftkey;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.app.AlertDialog;
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
public final class FlickSymbols {
    public static final String KEY = "hiro_up_flick_long_press";
    public static final String SENSITIVITY_KEY = "hiro_up_flick_sensitivity";
    private static final int DEFAULT_SENSITIVITY = 2;
    private static final String[] SENSITIVITY_LABELS = {
        "とても低い", "低い", "標準", "高い", "とても高い"
    };
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
    private FlickSymbols() {}

    public static void init(SharedPreferences value) { preferences = value; reset(); }
    public static void reset() {
        active = null;
        ++generation;
    }
    private static boolean enabled() { return preferences != null && preferences.getBoolean(KEY, false); }

    private static int sensitivity() {
        try {
            if (preferences != null) {
                int value = preferences.getInt(SENSITIVITY_KEY, DEFAULT_SENSITIVITY);
                if (value >= 0 && value < SENSITIVITY_LABELS.length) return value;
            }
        } catch (RuntimeException | LinkageError unavailable) {}
        return DEFAULT_SENSITIVITY;
    }

    private static float sensitivityMultiplier() {
        switch (sensitivity()) {
            case 0: return 2f;
            case 1: return 1.5f;
            case 3: return .8f;
            case 4: return .65f;
            default: return 1f;
        }
    }

    private static String sensitivitySummary(int value) {
        return "現在：" + SENSITIVITY_LABELS[value]
                + "。低くすると、上方向へ長めに動かしたときに反応します。標準はこれまでと同じ感度です。";
    }

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
            Preference sensitivityOption = screen.H(SENSITIVITY_KEY);
            if (sensitivityOption == null) {
                sensitivityOption = new Preference(screen.a, (AttributeSet) null);
                sensitivityOption.l = SENSITIVITY_KEY;
                sensitivityOption.s = false;
                sensitivityOption.B("上フリックの感度");
                sensitivityOption.f = new oa.k() {
                    public void k(Preference option) { chooseSensitivity(option); }
                };
                screen.G(sensitivityOption);
            }
            sensitivityOption.A(sensitivitySummary(sensitivity()));
        } catch (RuntimeException | LinkageError ignored) {}
    }

    private static void chooseSensitivity(final Preference option) {
        try {
            final SharedPreferences source = preferences;
            if (source == null || option == null) return;
            new AlertDialog.Builder(option.a)
                    .setTitle("上フリックの感度")
                    .setSingleChoiceItems(SENSITIVITY_LABELS, sensitivity(), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                if (source != preferences || which < 0 || which >= SENSITIVITY_LABELS.length) return;
                                source.edit().putInt(SENSITIVITY_KEY, which).apply();
                                option.A(sensitivitySummary(which));
                                // Each Stroke snapshots its distance on DOWN.
                                // A setting change must not revoke an owned touch stream.
                                dialog.dismiss();
                            } catch (RuntimeException | LinkageError ignored) {}
                        }
                    })
                    .setNegativeButton("キャンセル", null)
                    .show();
        } catch (RuntimeException | LinkageError ignored) {}
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
    private static boolean abandon(Stroke s, MotionEvent event) {
        if (active == s) active = null;
        if (s.captured) finishDrain(s.view, event);
        return s.captured;
    }

    public static boolean handle(d1 view, a token, MotionEvent event, boolean glideOwned) {
        if (cancelling || event == null) return false;
        Stroke s = null;
        boolean owned = false;
        try {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                // A fresh DOWN is a new stream, including after a missing UP.
                active = null;
                draining = null;
                if (glideOwned || !enabled() || !(view instanceof f0)) return false;
                if (event.getPointerCount() != 1) return false;
                if (event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER
                        && event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) return false;
                active = begin((f0) view, event);
                return false;
            }
            owned = drains(view, event);
            s = active;
            if (owned && (s == null || s.view != view || !s.captured || s.downTime != event.getDownTime())) {
                // A lifecycle change invalidated insertion after native CANCEL.
                // Keep swallowing this stream without retaining its editor data.
                finishDrain(view, event);
                return true;
            }
            if (s == null || s.view != view) return false;
            owned = s.captured;
            if (glideOwned || action == MotionEvent.ACTION_CANCEL || !enabled()
                    || event.getPointerCount() != 1 || event.getPointerId(0) != s.pointer
                    || event.getDownTime() != s.downTime || changed(s)
                    || event.getEventTime() < s.downTime
                    || (!s.captured && event.getEventTime() - s.downTime > CAPTURE_MILLIS))
                return abandon(s, event);
            if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP)
                return abandon(s, event);
            for (int i = 0; i < event.getHistorySize(); i++) {
                float x = event.getHistoricalX(0, i), y = event.getHistoricalY(0, i);
                if (!s.add(x, y)) return abandon(s, event);
                if (s.qualified && !s.captured && !capture(s, token, event)) return s.captured;
            }
            if (!s.add(event.getX(), event.getY())) return abandon(s, event);
            if (s.qualified && !s.captured && !capture(s, token, event)) return s.captured;
            if (!s.captured) {
                if (action == MotionEvent.ACTION_UP) return abandon(s, event);
                return false;
            }
            owned = true;
            if (action != MotionEvent.ACTION_UP) return true;
            // Geometry no longer votes after recognition. Revalidate only the
            // editor and original action before inserting exactly once on UP.
            if (!insertionValid(s, token)) return abandon(s, event);
            active = null;
            finishDrain(view, event);
            s.actor.b(token);
            return true;
        } catch (Exception | LinkageError | OutOfMemoryError failure) {
            // capture() marks ownership before invoking native code, so even a
            // throwing CANCEL or a reentrant reset cannot leak its later UP.
            owned = owned || (s != null && s.captured);
            if (active == s) active = null;
            if (owned) finishDrain(view, event);
            return owned;
        }
    }

    private static boolean insertionValid(Stroke s, a token) {
        return enabled() && !changed(s) && s.editorMatches()
                && alternate(s.key, s.model, s) == s.actor && token != null;
    }
    private static boolean capture(Stroke s, a token, MotionEvent event) {
        if (!insertionValid(s, token)) {
            abandon(s, event);
            return false;
        }
        MotionEvent cancel = MotionEvent.obtain(event);
        try {
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            // Install the drain BEFORE native code can reset us or throw.
            draining = new WeakReference<d1>(s.view);
            drainingDownTime = s.downTime;
            s.captured = true;
            cancelling = true;
            try { s.view.f(token, cancel); }
            finally { cancelling = false; }
        } finally { cancel.recycle(); }
        if (!insertionValid(s, token)) {
            abandon(s, event);
            return false;
        }
        return true;
    }

    private static Stroke begin(f0 view, MotionEvent event) {
        if (!ready(view)) return null;
        if (!(view.a instanceof fb0.f0)) return null;
        if (view.a.c == null || view.a.c.c != qj0.a.x4) return null;
        if (view.a.c() == null || view.a.c().b != ResultsFilter.PredictionSearchType.JAPANESE)
            return null;
        if (!(view.z instanceof x)) return null;
        KeyboardService service = service(view.getContext());
        if (service == null) return null;
        if (service.i == null) return null;
        if (connection(service) == null) return null;
        int type = service.i.inputType, variation = type & 0xff0;
        if ((type & 15) != 1 || variation == 128 || variation == 144 || variation == 224)
            return null;
        int w = view.getWidth(), h = view.getHeight();
        if (w <= 0 || h <= 0 || view.getWindowToken() == null) return null;
        if (event.getX() < 0 || event.getX() >= w || event.getY() < 0 || event.getY() >= h)
            return null;
        n2 chosen = null;
        for (Object object : ((fb0.f0) view.a).d) {
            if (!(object instanceof n2)) continue;
            n2 key = (n2) object;
            if (key.e == null || !validRect(key.e.a) || !key.e.a.contains(event.getX()/w, event.getY()/h)) continue;
            if (chosen != null) return null;
            chosen = key;
        }
        if (chosen == null) return null;
        ic0.a actor = alternate(chosen, (x) view.z, null);
        if (actor == null) return null;
        Stroke s = new Stroke(view, service, (x) view.z, chosen, actor, event);
        if (!(s.minimum > 0) || !s.snapshotValid()) return null;
        // Bind the route as well as the actor. Reparenting the same actor or
        // changing a popup's default index must not change an in-flight flick.
        if (alternate(chosen, s.model, s) != actor) return null;
        return s;
    }

    /** Resolve an existing native insertion actor without opening a popup.
     * Single-character long presses have a direct release actor. Multi-choice
     * long presses instead open a mini keyboard whose default entry owns its
     * own insertion actor. Those are distinct native routes, not sensitivities.
     */
    private static ic0.a alternate(n2 key, x model, Stroke snapshot) {
        if (!(key.b instanceof oc0.m) || key.d == null) return null;
        hc0.a handler = ((oc0.m) key.b).b;
        if (handler == null || handler.c == null || handler.c.d == null)
            return null;
        List actions = hc0.o.a(handler.c.d, key.d);
        if (actions == null) return null;
        fc0.d1 selected = null;
        for (Object action : actions) {
            if (!(action instanceof ic0.e) || !(((ic0.e) action).a instanceof fc0.d1)) continue;
            fc0.d1 candidate = (fc0.d1) ((ic0.e) action).a;
            if (selected != null) return null;
            if (!candidate.d || !singleCharacter(candidate.b) || !singleCharacter(candidate.c))
                return null;
            selected = candidate;
        }
        if (selected != null)
            return bindAlternate(selected, snapshot, null, null, null, null, null, -1);

        if (handler.c.b == null) return null;
        List starts = hc0.o.a(handler.c.b, key.d);
        if (starts == null) return null;
        fc0.b1 opening = null;
        e60.b binding = null;
        androidx.lifecycle.h provider = null;
        xc0.f popupSource = null;
        for (Object action : starts) {
            if (!(action instanceof ic0.e) || !(((ic0.e) action).a instanceof fc0.b1)) continue;
            fc0.b1 candidate = (fc0.b1) ((ic0.e) action).a;
            if (opening != null) return null;
            if (candidate.a != 1 || !(candidate.b instanceof e60.b))
                return null;
            e60.b candidateBinding = (e60.b) candidate.b;
            if (candidateBinding.b != key.d || !(candidateBinding.a instanceof androidx.lifecycle.h))
                return null;
            androidx.lifecycle.h candidateProvider = (androidx.lifecycle.h) candidateBinding.a;
            if (candidateProvider.a != 7 || !(candidateProvider.b instanceof xc0.f))
                return null;
            opening = candidate;
            binding = candidateBinding;
            provider = candidateProvider;
            popupSource = (xc0.f) candidateProvider.b;
        }
        if (popupSource == null) return null;
        fb0.f2 popup = popupSource.a;
        if (popup == null || popup.d == null || popup.l < 0 || popup.l >= popup.d.size())
            return null;
        Object defaultEntry = popup.d.get(popup.l);
        if (!(defaultEntry instanceof n2)) return null;
        n2 popupKey = (n2) defaultEntry;
        if (popupKey.d == null || !(popupKey.b instanceof oc0.m))
            return null;
        hc0.a popupHandler = ((oc0.m) popupKey.b).b;
        if (popupHandler == null || popupHandler.b == null || !(popupHandler.b.b instanceof Collection))
            return null;
        List releases = hc0.o.a((Collection) popupHandler.b.b, popupKey.d);
        if (releases == null) return null;
        fb0.l2 popupActor = null;
        for (Object action : releases) {
            // Unlike the single-character route, normal popup-key releases
            // contain the actor directly, not an ic0.e long-press wrapper.
            if (!(action instanceof fb0.l2)) continue;
            fb0.l2 candidate = (fb0.l2) action;
            if (popupActor != null) return null;
            if (candidate.a == null || candidate.a.a != model)
                return null;
            if (!candidate.d || !candidate.e || !singleCharacter(candidate.b) || !singleCharacter(candidate.c))
                return null;
            popupActor = candidate;
        }
        if (popupActor == null) return null;
        return bindAlternate(popupActor, snapshot, opening, binding, provider, popup, popupKey,
                popup.l);
    }

    private static ic0.a bindAlternate(ic0.a actor, Stroke snapshot, Object opening, Object binding,
            Object provider, fb0.f2 popup, n2 popupKey, int popupIndex) {
        if (snapshot != null) {
            if (snapshot.actor != actor) return null;
            if (snapshot.alternateBound) {
                if (snapshot.popupOpening != opening || snapshot.popupBinding != binding
                        || snapshot.popupProvider != provider || snapshot.popup != popup
                        || snapshot.popupKey != popupKey || snapshot.popupIndex != popupIndex)
                    return null;
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
    private static boolean ready(f0 view) {
        return view.isShown() && view.isAttachedToWindow() && view.j
                && view.A != null && !view.A.b()
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
    private static InputConnection connection(KeyboardService s) {
        return s.h == null ? s.getCurrentInputConnection() : s.h;
    }
    private static boolean changed(Stroke s) {
        if (s.generation != generation) return true;
        if (!ready(s.view)) return true;
        if (s.view.a != s.layout) return true;
        if (s.view.z != s.model) return true;
        if (s.view.getWidth() != s.width || s.view.getHeight() != s.height) return true;
        if (s.view.getWindowToken() != s.window) return true;
        if (s.service.i != s.editor || s.service.i.inputType != s.inputType
                || s.service.i.imeOptions != s.imeOptions) return true;
        if (connection(s.service) != s.connection) return true;
        if (s.layout.c == null || s.layout.c.c != qj0.a.x4) return true;
        if (s.layout.c() == null || s.layout.c().b != ResultsFilter.PredictionSearchType.JAPANESE)
            return true;
        if (!((fb0.f0) s.layout).d.contains(s.key)) return true;
        if (!s.geometryMatches()) return true;
        if (s.model.c == null || !s.matches(s.model.c.C())) return true;
        return false;
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
        boolean qualified, captured;
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
            // Standard multiplies by exactly 1, preserving the previous limits.
            // Read once per gesture so a settings change affects the next DOWN.
            minimum=Math.max(6f, Math.min(kw,kh)*.24f) * sensitivityMultiplier();
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
        boolean add(float x, float y) {
            if (Float.isNaN(x)||Float.isNaN(y)||Float.isInfinite(x)||Float.isInfinite(y)) return false;
            if (captured) return true;
            double dx=Math.abs((double)x-startX), up=(double)startY-y;
            double distanceSq=dx*dx+up*up;
            boolean distanceMet=distanceSq>=(double)minimum*minimum;
            if (distanceMet && up>=minimumUp && up>=dx*UP_SLOPE) {
                qualified=true;
                return true;
            }
            if (distanceSq>=(double)directionSlop*directionSlop)
                return false;
            return true;
        }
    }
}
