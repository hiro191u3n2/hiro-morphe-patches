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
    public boolean enabled=true;public int clickCalls;public Runnable clickAction;
    public boolean isEnabled(){return enabled;}
    public boolean performClick(){clickCalls++;if(clickAction!=null){clickAction.run();return true;}return false;}
    public static final int NO_ID=-1;
    public int id=NO_ID;public boolean selected,activated,clickable,hidden,editor;public float alpha=1;
    public CharSequence description;public ViewParent parent;public View focusView;
    public android.graphics.Rect bounds=new android.graphics.Rect(0,0,1080,2200);
    public WindowInsets insets=new WindowInsets();
    public int getId(){return id;}public android.content.res.Resources getResources(){return new android.content.res.Resources();}
    public boolean isSelected(){return selected;}public boolean isActivated(){return activated;}public boolean isClickable(){return clickable;}
    public boolean isShown(){return !hidden;}public float getAlpha(){return alpha;}public CharSequence getContentDescription(){return description;}
    public ViewParent getParent(){return parent;}public View findFocus(){return focusView;}
    public boolean onCheckIsTextEditor(){return editor;}public WindowInsets getRootWindowInsets(){return insets;}
    public void getWindowVisibleDisplayFrame(android.graphics.Rect out){out.set(bounds.left,bounds.top,bounds.right,bounds.bottom);}
    public boolean getGlobalVisibleRect(android.graphics.Rect out){out.set(bounds.left,bounds.top,bounds.right,bounds.bottom);return !hidden;}
    public android.view.accessibility.AccessibilityNodeInfo createAccessibilityNodeInfo(){return new android.view.accessibility.AccessibilityNodeInfo(selected,clickable);}
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
