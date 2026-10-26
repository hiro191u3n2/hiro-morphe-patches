package hiro.swiftkey;

import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import oa.j;

/** Optional, reversible filtering of two controls inside this IME's own window. */
public final class EmojiPanelUi {
    public static final String HIDE_SEARCH_KEY = "hiro_hide_emoji_search";
    public static final String HIDE_AI_KEY = "hiro_hide_ai_creation";
    private static final int SEARCH_ROW = 0x7f0a020d;
    private static final int SEARCH_EDIT = 0x7f0a0261;
    private static final int SEARCH_HINT = 0x7f1401e8;
    private static final int TOP_ROW = 0x7f0a0212;
    private static final int AI_BUTTON = 0x7f0a0048;
    private static final int AI_LABEL = 0x7f14003f;

    private static volatile SharedPreferences preferences;
    private static WeakReference<InputMethodService> service = new WeakReference<InputMethodService>(null);
    // The watcher contains only weak references to roots, observers and filtered views.
    private static Watcher watcher;
    private static final SharedPreferences.OnSharedPreferenceChangeListener PREFERENCE_LISTENER =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override public void onSharedPreferenceChanged(SharedPreferences p, String key) {
                    if (HIDE_SEARCH_KEY.equals(key) || HIDE_AI_KEY.equals(key)) refreshOnMain();
                }
            };

    private EmojiPanelUi() {}

    public static void init(SharedPreferences value) {
        try {
            SharedPreferences previous = preferences;
            if (previous != value) {
                if (previous != null) previous.unregisterOnSharedPreferenceChangeListener(PREFERENCE_LISTENER);
                preferences = value;
                if (value != null) value.registerOnSharedPreferenceChangeListener(PREFERENCE_LISTENER);
            }
            refreshOnMain();
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {}
    }

    public static void install(PreferenceScreen screen) {
        try {
            if (screen == null || preferences == null) return;
            addOption(screen, HIDE_SEARCH_KEY, "絵文字の検索欄を非表示",
                    "絵文字画面の検索欄と右側の矢印を非表示にします。オフにすると元の表示に戻ります。");
            addOption(screen, HIDE_AI_KEY, "「AI の作成」ボタンを非表示",
                    "絵文字などの画面の「AI の作成」ボタンを非表示にします。オフにすると元の表示に戻ります。");
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {}
    }

    private static void addOption(PreferenceScreen screen, String key, String title, String summary) {
        if (screen.H(key) != null) return;
        SwitchPreferenceCompat option = new SwitchPreferenceCompat(screen.a, (AttributeSet) null);
        option.l = key;
        // Use the same explicit preference store as the existing KeySwap options.
        option.s = false;
        option.B(title);
        option.A(summary);
        option.G(preferences.getBoolean(key, false));
        option.e = new OptionChange(key);
        screen.G(option);
    }

    private static final class OptionChange implements j {
        private final String key;
        OptionChange(String key) { this.key = key; }
        @Override public boolean i(Preference ignored, Object value) {
            try {
                SharedPreferences p = preferences;
                if (p == null || !(value instanceof Boolean)) return false;
                p.edit().putBoolean(key, ((Boolean) value).booleanValue()).apply();
                refreshOnMain();
                return true;
            } catch (RuntimeException ignoredFailure) {
                return false;
            } catch (LinkageError ignoredFailure) {
                return false;
            }
        }
    }

    /** Called by the existing unconditional onStartInput hook, independently of glide. */
    public static void start(final InputMethodService value) {
        try {
            onMain(new Runnable() {
                @Override public void run() {
                    try {
                        service = new WeakReference<InputMethodService>(value);
                        refresh();
                        // The native onStartInput body runs after the hook and can replace content.
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override public void run() { refreshSafely(); }
                        });
                    } catch (RuntimeException ignored) {
                    } catch (LinkageError ignored) {}
                }
            });
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {}
    }

    private static void onMain(Runnable action) {
        Looper main = Looper.getMainLooper();
        if (Looper.myLooper() == main) action.run();
        else new Handler(main).post(action);
    }

    private static void refreshOnMain() {
        try {
            onMain(new Runnable() {
                @Override public void run() { refreshSafely(); }
            });
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {}
    }

    private static void refreshSafely() {
        try { refresh(); }
        catch (RuntimeException ignored) {}
        catch (LinkageError ignored) {}
    }

    private static boolean enabled(String key) {
        try {
            SharedPreferences p = preferences;
            return p != null && p.getBoolean(key, false);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static void refresh() {
        if (!enabled(HIDE_SEARCH_KEY) && !enabled(HIDE_AI_KEY)) {
            if (watcher != null) watcher.release();
            watcher = null;
            return;
        }
        InputMethodService current = service.get();
        View root = null;
        if (current != null && current.getWindow() != null) {
            Window window = current.getWindow().getWindow();
            if (window != null) root = window.getDecorView();
        }
        if (watcher != null && watcher.root.get() != root) {
            watcher.release();
            watcher = null;
        }
        if (root == null) return;
        if (watcher == null) {
            watcher = new Watcher(root);
            watcher.attach();
        }
        watcher.apply();
    }

    private static final class Watcher implements ViewTreeObserver.OnPreDrawListener,
            ViewTreeObserver.OnGlobalLayoutListener, View.OnAttachStateChangeListener {
        final WeakReference<View> root;
        private WeakReference<ViewTreeObserver> observer = new WeakReference<ViewTreeObserver>(null);
        private final WeakHashMap<View, Integer> hidden = new WeakHashMap<View, Integer>();
        private boolean released;
        private boolean pendingLayout;

        Watcher(View value) { root = new WeakReference<View>(value); }

        void attach() {
            View view = root.get();
            if (view == null || released) return;
            view.addOnAttachStateChangeListener(this);
            registerObserver();
        }

        private void unregisterObserver() {
            ViewTreeObserver previous = observer.get();
            if (previous != null && previous.isAlive()) {
                previous.removeOnPreDrawListener(this);
                previous.removeOnGlobalLayoutListener(this);
            }
            observer = new WeakReference<ViewTreeObserver>(null);
        }

        private void registerObserver() {
            View view = root.get();
            if (view == null || released) return;
            unregisterObserver();
            ViewTreeObserver next = view.getViewTreeObserver();
            if (!next.isAlive()) return;
            // Attaching can transfer listeners from a previously detached observer.
            next.removeOnPreDrawListener(this);
            next.removeOnGlobalLayoutListener(this);
            next.addOnPreDrawListener(this);
            next.addOnGlobalLayoutListener(this);
            observer = new WeakReference<ViewTreeObserver>(next);
        }

        @Override public void onViewAttachedToWindow(View view) {
            try { registerObserver(); apply(); }
            catch (RuntimeException ignored) {}
            catch (LinkageError ignored) {}
        }

        @Override public void onViewDetachedFromWindow(View view) {
            try { unregisterObserver(); restoreAll(); }
            catch (RuntimeException ignored) {}
            catch (LinkageError ignored) {}
        }

        @Override public void onGlobalLayout() {
            try { apply(); }
            catch (RuntimeException ignored) {}
            catch (LinkageError ignored) {}
        }

        @Override public boolean onPreDraw() {
            try {
                apply();
                if (pendingLayout) {
                    pendingLayout = false;
                    return false;
                }
            } catch (RuntimeException ignored) {
            } catch (LinkageError ignored) {}
            return true;
        }

        void apply() {
            View view = root.get();
            if (view == null || released) return;
            boolean search = enabled(HIDE_SEARCH_KEY), ai = enabled(HIDE_AI_KEY);
            Set<View> wanted = Collections.newSetFromMap(new IdentityHashMap<View, Boolean>());
            if (search || ai) collect(view, view, search, ai, wanted);
            for (View target : wanted) {
                int visibility = target.getVisibility();
                if (visibility != View.GONE) {
                    // Native GONE controls are never adopted, so disabling cannot reveal them.
                    hidden.put(target, Integer.valueOf(visibility));
                    target.setVisibility(View.GONE);
                    pendingLayout = true;
                }
            }
            Iterator<Map.Entry<View, Integer>> iterator = hidden.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<View, Integer> entry = iterator.next();
                if (!wanted.contains(entry.getKey())) {
                    restore(entry.getKey(), entry.getValue().intValue());
                    iterator.remove();
                }
            }
            if (pendingLayout) view.requestLayout();
        }

        private void collect(View current, View boundary, boolean search, boolean ai, Set<View> wanted) {
            if (search && current.getId() == SEARCH_ROW && emojiSearch(current)) wanted.add(current);
            if (ai && current.getId() == AI_BUTTON && current instanceof TextView
                    && insideTopRow(current, boundary) && textEquals(current, AI_LABEL, ((TextView) current).getText())) {
                wanted.add(current);
            }
            if (current instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) current;
                for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), boundary, search, ai, wanted);
            }
        }

        private boolean emojiSearch(View row) {
            View edit = row.findViewById(SEARCH_EDIT);
            return edit instanceof EditText && textEquals(edit, SEARCH_HINT, ((EditText) edit).getHint());
        }

        private boolean insideTopRow(View view, View boundary) {
            ViewParent parent = view.getParent();
            while (parent instanceof View) {
                View ancestor = (View) parent;
                if (ancestor.getId() == TOP_ROW) return true;
                if (ancestor == boundary) return false;
                parent = ancestor.getParent();
            }
            return false;
        }

        private boolean textEquals(View view, int resource, CharSequence value) {
            return value != null && view.getResources().getString(resource).contentEquals(value);
        }

        private void restore(View view, int visibility) {
            // If native code has already changed visibility, its newer decision wins.
            if (view != null && view.getVisibility() == View.GONE) {
                view.setVisibility(visibility);
                pendingLayout = true;
            }
        }

        private void restoreAll() {
            for (Map.Entry<View, Integer> entry : hidden.entrySet()) restore(entry.getKey(), entry.getValue().intValue());
            hidden.clear();
        }

        void release() {
            if (released) return;
            released = true;
            unregisterObserver();
            View view = root.get();
            if (view != null) view.removeOnAttachStateChangeListener(this);
            restoreAll();
        }
    }
}
