package android.view;
public class View {
 public android.content.Context context;
 public boolean shown=true,attached=true,focused=true;
 public int width=1000,height=400,invalidates;
 public android.os.IBinder window=new android.os.IBinder(){};
 public final ViewOverlay overlay=new ViewOverlay();
 public ViewParent parent;
 public final ViewTreeObserver observer=new ViewTreeObserver();
 public final java.util.List<OnAttachStateChangeListener> attachments=new java.util.ArrayList<>();
 public View(android.content.Context c){context=c;}
 public android.content.Context getContext(){return context;}
 public android.content.res.Resources getResources(){return context.getResources();}
 public boolean isShown(){return shown;}
 public boolean isAttachedToWindow(){return attached;}
 public boolean hasWindowFocus(){return focused;}
 public int getWidth(){return width;}
 public int getHeight(){return height;}
 public android.os.IBinder getWindowToken(){return window;}
 public ViewOverlay getOverlay(){return overlay;}
 public ViewParent getParent(){return parent;}
 public ViewTreeObserver getViewTreeObserver(){return observer;}
 public void invalidate(){invalidates++;}
 public void addOnAttachStateChangeListener(OnAttachStateChangeListener l){attachments.add(l);}
 public void removeOnAttachStateChangeListener(OnAttachStateChangeListener l){attachments.remove(l);}
 public void testDetach(){attached=false;for(OnAttachStateChangeListener l:new java.util.ArrayList<>(attachments))l.onViewDetachedFromWindow(this);}
 public void testAttach(){attached=true;for(OnAttachStateChangeListener l:new java.util.ArrayList<>(attachments))l.onViewAttachedToWindow(this);}
 public interface OnAttachStateChangeListener{void onViewAttachedToWindow(View v);void onViewDetachedFromWindow(View v);}
}
