package hiro.swiftkey;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import com.touchtype.KeyboardService;
import com.touchtype.keyboard.expandedcandidate.ExpandedResultsCloseButton;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.WeakHashMap;

/** Exact native view/controller bridge for SwiftKey Beta 9.13.16.4. */
public final class CandidateToolbar {
    private static final int START_CONTAINER = 0x7f0a04b8;
    private static final int CANDIDATES = 0x7f0a0473;
    private static final int MORE_BUTTON = 0x7f0a0474;
    private static final int OPEN = 0;
    private static final int CLOSED = 2;

    // Both sides are weak: a retired keyboard service or detached ribbon must not
    // retain an input connection, activity context, or obsolete controller.
    private static final WeakHashMap<KeyboardService, ArrayList<WeakReference<ob0.d1>>>
            bindings = new WeakHashMap<>();
    private static final WeakHashMap<KeyboardService, Boolean> emojiAccess = new WeakHashMap<>();
    private static final WeakHashMap<ob0.d1, Hidden> hidden = new WeakHashMap<>();

    private static final class Hidden {
        final float alpha;
        final boolean wrapperEnabled, buttonEnabled;
        final int accessibility;
        WeakReference<View> slot;
        int slotVisibility;
        Hidden(ob0.d1 wrapper, ob0.b1 button) {
            alpha = wrapper.getAlpha();
            wrapperEnabled = wrapper.isEnabled();
            buttonEnabled = button.isEnabled();
            accessibility = wrapper.getImportantForAccessibility();
        }
    }

    private CandidateToolbar() {}

    /** Called once at the end of the native toolbar-button wrapper constructor. */
    public static void bindToolbar(ob0.d1 wrapper) {
        if (wrapper == null) return;
        try {
            KeyboardService service = serviceOf(wrapper.getContext());
            ob0.b1 button = wrapper.getButton();
            if (service == null || button == null || button.c == null) return;
            synchronized (bindings) {
                ArrayList<WeakReference<ob0.d1>> list = bindings.get(service);
                if (list == null) {
                    list = new ArrayList<>();
                    bindings.put(service, list);
                }
                for (int i = list.size() - 1; i >= 0; i--) {
                    ob0.d1 previous = list.get(i).get();
                    if (previous == null || previous == wrapper) list.remove(i);
                }
                list.add(new WeakReference<>(wrapper));
            }
            applyHidden(wrapper, canHide(wrapper, service));
        } catch (Throwable ignored) {
            // A foreign layout must keep its native access path.
        }
    }

    /** Called only after a native emoji-key action was successfully configured. */
    public static void setEmojiAccess(KeyboardService service, boolean available) {
        if (service == null) return;
        try {
            synchronized (bindings) { emojiAccess.put(service, available); }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                refresh(service);
            } else {
                final WeakReference<KeyboardService> serviceRef = new WeakReference<>(service);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() { refresh(serviceRef.get()); }
                });
            }
        } catch (Throwable ignored) {}
    }

    private static boolean canHide(ob0.d1 wrapper, KeyboardService service) {
        synchronized (bindings) {
            if (!Boolean.TRUE.equals(emojiAccess.get(service))) return false;
        }
        ob0.b1 button = wrapper.getButton();
        return button != null && button.c != null && button.c.b != null && button.c.b.G();
    }

    private static void refresh(KeyboardService service) {
        if (service == null) return;
        ArrayList<WeakReference<ob0.d1>> copy;
        synchronized (bindings) {
            ArrayList<WeakReference<ob0.d1>> list = bindings.get(service);
            if (list == null) return;
            copy = new ArrayList<>(list);
        }
        for (WeakReference<ob0.d1> ref : copy) {
            ob0.d1 wrapper = ref.get();
            if (wrapper == null) continue;
            applyHidden(wrapper, canHide(wrapper, service));
            ViewParent parent = wrapper.getParent();
            if (parent != null && parent.getParent() instanceof View) {
                layoutContainer((View) parent.getParent());
            }
        }
    }

    private static void applyHidden(ob0.d1 wrapper, boolean hide) {
        ob0.b1 button = wrapper.getButton();
        if (button == null) return;
        synchronized (bindings) {
            Hidden previous = hidden.get(wrapper);
            if (hide) {
                if (previous == null) hidden.put(wrapper, new Hidden(wrapper, button));
                // Visibility animations remain native. Keep their result, but
                // suppress rendering and input while the emoji route is ready.
                if (wrapper.getAlpha() != 0f) wrapper.setAlpha(0f);
                if (wrapper.isEnabled()) wrapper.setEnabled(false);
                if (button.isEnabled()) button.setEnabled(false);
                if (wrapper.getImportantForAccessibility() != View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
                    wrapper.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            } else if (previous != null) {
                wrapper.setAlpha(previous.alpha);
                wrapper.setEnabled(previous.wrapperEnabled);
                button.setEnabled(previous.buttonEnabled);
                wrapper.setImportantForAccessibility(previous.accessibility);
                View slot = previous.slot == null ? null : previous.slot.get();
                if (slot != null) slot.setVisibility(previous.slotVisibility);
                hidden.remove(wrapper);
            }
        }
    }

    private static KeyboardService serviceOf(Context context) {
        for (int depth = 0; context != null && depth < 16; depth++) {
            if (context instanceof KeyboardService) return (KeyboardService) context;
            if (!(context instanceof ContextWrapper)) return null;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) return null;
            context = next;
        }
        return null;
    }

    private static boolean bound(ob0.d1 wrapper) {
        KeyboardService service = serviceOf(wrapper.getContext());
        if (service == null) return false;
        synchronized (bindings) {
            ArrayList<WeakReference<ob0.d1>> list = bindings.get(service);
            if (list == null) return false;
            for (WeakReference<ob0.d1> ref : list) if (ref.get() == wrapper) return true;
        }
        return false;
    }

    private static kd0.e0 controller(KeyboardService service) {
        if (service == null) return null;
        synchronized (bindings) {
            ArrayList<WeakReference<ob0.d1>> list = bindings.get(service);
            if (list == null) return null;
            for (int i = list.size() - 1; i >= 0; i--) {
                ob0.d1 wrapper = list.get(i).get();
                if (wrapper == null) { list.remove(i); continue; }
                if (!wrapper.isAttachedToWindow() || serviceOf(wrapper.getContext()) != service) continue;
                if (!canHide(wrapper, service)) continue;
                ob0.b1 button = wrapper.getButton();
                if (button != null && button.c != null && button.c.g != null) return button.c;
            }
        }
        return null;
    }

    /** The dedicated emoji key uses this before invoking its original short action. */
    public static boolean closeIfOpen(KeyboardService service) {
        try {
            kd0.e0 model = controller(service);
            if (model == null || model.g.a != OPEN) return false;
            return setState(model, CLOSED);
        } catch (Throwable ignored) { return false; }
    }

    /** Long press uses the native state transition, persistence and notification. */
    public static boolean toggle(KeyboardService service) {
        try {
            kd0.e0 model = controller(service);
            if (model == null) return false;
            int current = model.g.a;
            if (current != OPEN && current != CLOSED) return false;
            return setState(model, current == OPEN ? CLOSED : OPEN);
        } catch (Throwable ignored) { return false; }
    }

    private static boolean setState(kd0.e0 model, int target) {
        try { kd0.e0.n(model, target); }
        catch (Throwable ignored) {
            // n updates state before persistence/notification. If those fail,
            // still consume the tap that already closed it; do not open emoji.
        }
        return model.g != null && model.g.a == target;
    }

    /** Reclaims the former arrow slot in the enclosing native candidate ribbon. */
    public static void layoutContainer(View container) {
        if (container == null) return;
        try {
            View start = container.findViewById(START_CONTAINER);
            if (!(start instanceof ViewGroup)) return;
            ViewGroup group = (ViewGroup) start;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ob0.d1 && bound((ob0.d1) child)) {
                    ob0.d1 wrapper = (ob0.d1) child;
                    boolean hide = canHide(wrapper, serviceOf(wrapper.getContext()));
                    applyHidden(wrapper, hide);
                    if (!hide) return;
                    synchronized (bindings) {
                        Hidden previous = hidden.get(wrapper);
                        if (previous != null && (previous.slot == null || previous.slot.get() != start)) {
                            previous.slot = new WeakReference<>(start);
                            previous.slotVisibility = start.getVisibility();
                        }
                    }
                    if (start.getVisibility() != View.GONE) start.setVisibility(View.GONE);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    /** Moves the real native [+] button and its hit box; never reverses candidates. */
    public static void layoutCollapsed(View bar) {
        if (bar == null) return;
        try {
            refresh(serviceOf(bar.getContext()));
            View list = bar.findViewById(CANDIDATES);
            View button = bar.findViewById(MORE_BUTTON);
            if (list == null || !(list.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
            int width = 0;
            if (button != null && button.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) button.getLayoutParams();
                width = params.width > 0 ? params.width : button.getMeasuredWidth();
                int gravity = Gravity.LEFT | Gravity.TOP;
                if (params.gravity != gravity) {
                    params.gravity = gravity;
                    button.setLayoutParams(params);
                }
                setHorizontalMargins(button, 0, 0);
            }
            setHorizontalMargins(list, Math.max(0, width), 0);
        } catch (Throwable ignored) {}
    }

    /** Moves native [-] to the physical left and keeps results clear of its hit box. */
    public static void layoutExpandedClose(ExpandedResultsCloseButton close) {
        if (close == null) return;
        try {
            if (!(close.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) close.getLayoutParams();
            int width = close.a != null ? close.a.d() : close.getMeasuredWidth();
            if (width <= 0) width = params.width;
            if (width <= 0) return;
            int gravity = Gravity.LEFT | Gravity.TOP;
            if (params.gravity != gravity || params.width != width || params.height != width) {
                params.gravity = gravity;
                params.width = width;
                params.height = width;
                close.setLayoutParams(params);
            }
            setHorizontalMargins(close, 0, 0);
            ViewParent header = close.getParent();
            if (!(header instanceof FrameLayout)) return;
            ViewParent outer = header.getParent();
            if (!(outer instanceof FrameLayout)) return;
            FrameLayout overlay = (FrameLayout) outer;
            for (int i = 0; i < overlay.getChildCount(); i++) {
                View child = overlay.getChildAt(i);
                if (child instanceof androidx.recyclerview.widget.RecyclerView
                        && child.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    setHorizontalMargins(child, width, 0);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void setHorizontalMargins(View view, int left, int right) {
        if (!(view.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        boolean rtl = view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        int start = rtl ? right : left;
        int end = rtl ? left : right;
        if (params.leftMargin == left && params.rightMargin == right
                && params.getMarginStart() == start && params.getMarginEnd() == end) return;
        params.setMarginStart(start);
        params.setMarginEnd(end);
        params.leftMargin = left;
        params.rightMargin = right;
        view.setLayoutParams(params);
    }
}
