package android.view;
import android.content.Context;
import android.view.inspector.WindowInspector;
import android.window.OnBackInvokedDispatcher;
import java.util.ArrayList;
import java.util.List;
public class View {
    public interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View view);
        void onViewDetachedFromWindow(View view);
    }
    private final Context context;
    private final List<OnAttachStateChangeListener> listeners = new ArrayList<OnAttachStateChangeListener>();
    private ViewTreeObserver observer = new ViewTreeObserver();
    private OnBackInvokedDispatcher dispatcher;
    private boolean attached;
    private boolean focused;
    public View(Context context) { this.context = context; }
    public Context getContext() { return context; }
    public boolean isAttachedToWindow() { return attached; }
    public boolean hasWindowFocus() { return focused; }
    public ViewTreeObserver getViewTreeObserver() { return observer; }
    public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) { listeners.add(listener); }
    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener) { listeners.remove(listener); }
    public OnBackInvokedDispatcher findOnBackInvokedDispatcher() { return attached ? dispatcher : null; }
    public void attach(OnBackInvokedDispatcher value) {
        dispatcher = value; attached = true;
        if (!observer.isAlive()) observer = new ViewTreeObserver();
        WindowInspector.add(this);
        for (OnAttachStateChangeListener listener : new ArrayList<OnAttachStateChangeListener>(listeners))
            listener.onViewAttachedToWindow(this);
    }
    public void detach() {
        attached = false; focused = false; WindowInspector.remove(this);
        for (OnAttachStateChangeListener listener : new ArrayList<OnAttachStateChangeListener>(listeners))
            listener.onViewDetachedFromWindow(this);
        observer.kill();
    }
    public void focus(boolean value) { focused = value; observer.dispatchFocus(value); }
    public int attachListenerCount() { return listeners.size(); }
}
