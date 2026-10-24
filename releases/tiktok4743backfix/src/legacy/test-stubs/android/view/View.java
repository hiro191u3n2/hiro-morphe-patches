package android.view;

import java.util.ArrayList;
import java.util.List;

/** Host-only fixture for detach/reattach and native recycler visibility changes. */
public final class View {
    public static final int VISIBLE = 0, INVISIBLE = 4, GONE = 8;
    public interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View view);
        void onViewDetachedFromWindow(View view);
    }
    private final List<OnAttachStateChangeListener> listeners = new ArrayList<>();
    private int visibility = VISIBLE;
    private boolean attached;
    private ViewTreeObserver observer = new ViewTreeObserver();
    public void setVisibility(int value) { visibility = value; }
    public int getVisibility() { return visibility; }
    public boolean isAttachedToWindow() { return attached; }
    public ViewTreeObserver getViewTreeObserver() { return observer; }
    public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) { listeners.add(listener); }
    public int attachmentListenerCount() { return listeners.size(); }
    public void attach() {
        if (attached) throw new AssertionError("already attached");
        attached = true;
        if (!observer.isAlive()) observer = new ViewTreeObserver();
        for (OnAttachStateChangeListener listener : new ArrayList<>(listeners)) listener.onViewAttachedToWindow(this);
    }
    public void detach() {
        if (!attached) throw new AssertionError("not attached");
        attached = false;
        for (OnAttachStateChangeListener listener : new ArrayList<>(listeners)) listener.onViewDetachedFromWindow(this);
    }
}
