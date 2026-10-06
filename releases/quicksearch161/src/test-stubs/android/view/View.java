package android.view;
import android.content.Context;
import android.content.res.Resources;
import java.util.ArrayList;
import java.util.List;
public class View implements ViewParent {
    public static final int VISIBLE=0, GONE=8;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_YES=1;
    public interface OnClickListener { void onClick(View view); }
    public interface OnKeyListener { boolean onKey(View view,int code,KeyEvent event); }
    public interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View view);
        void onViewDetachedFromWindow(View view);
    }
    private final Context context;
    private int id, minWidth, minHeight, visibility;
    private boolean enabled=true, clickable, focusable, attached=true;
    private CharSequence description;
    private ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-2,-2);
    public ViewParent parent;
    public OnClickListener clickListener;
    public OnKeyListener keyListener;
    public final List<OnAttachStateChangeListener> attachListeners=new ArrayList<>();
    public View(Context context) { this.context=context; }
    public Context getContext() { return context; }
    public Resources getResources() { return context.getResources(); }
    public void setId(int id) { this.id=id; }
    public int getId() { return id; }
    @SuppressWarnings("unchecked") public <T extends View> T findViewById(int requested) { return requested==id?(T)this:null; }
    public void setOnClickListener(OnClickListener listener) { clickListener=listener; clickable=listener!=null; }
    public boolean performClick() { if (enabled && clickListener!=null) {clickListener.onClick(this);return true;} return false; }
    public void setOnKeyListener(OnKeyListener listener) { keyListener=listener; }
    public void setMinimumWidth(int value) { minWidth=value; }
    public void setMinimumHeight(int value) { minHeight=value; }
    public int getMinimumWidth() { return minWidth; }
    public int getMinimumHeight() { return minHeight; }
    public void setEnabled(boolean value) { enabled=value; }
    public boolean isEnabled() { return enabled; }
    public void setClickable(boolean value) { clickable=value; }
    public boolean isClickable() { return clickable; }
    public void setFocusable(boolean value) { focusable=value; }
    public boolean isFocusable() { return focusable; }
    public void setContentDescription(CharSequence value) { description=value; }
    public CharSequence getContentDescription() { return description; }
    public void setVisibility(int value) { visibility=value; }
    public int getVisibility() { return visibility; }
    public void setImportantForAccessibility(int value) {}
    public ViewGroup.LayoutParams getLayoutParams() { return layoutParams; }
    public void setLayoutParams(ViewGroup.LayoutParams value) { layoutParams=value; }
    public ViewParent getParent() { return parent; }
    public View getRootView() { return parent instanceof View?((View)parent).getRootView():this; }
    public boolean post(Runnable value) { value.run(); return true; }
    public void requestLayout() {}
    public void bringToFront() {}
    public void setPadding(int left,int top,int right,int bottom) {}
    public void addOnAttachStateChangeListener(OnAttachStateChangeListener value) { attachListeners.add(value); }
    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener value) { attachListeners.remove(value); }
    public boolean isAttachedToWindow() { return attached; }
    public Object getWindowToken() { return attached?this:null; }
    public void dispatchAttach() { attached=true;for(OnAttachStateChangeListener l:new ArrayList<>(attachListeners))l.onViewAttachedToWindow(this); }
    public void dispatchDetach() { attached=false;for(OnAttachStateChangeListener l:new ArrayList<>(attachListeners))l.onViewDetachedFromWindow(this); }
}
