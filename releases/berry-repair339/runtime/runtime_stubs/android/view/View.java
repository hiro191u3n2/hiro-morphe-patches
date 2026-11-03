package android.view;
public class View {
 public int getWidth(){return 0;} public int getHeight(){return 0;} public int getMeasuredWidth(){return 0;} public int getMeasuredHeight(){return 0;}
 public int getLeft(){return 0;} public int getTop(){return 0;} public int getRight(){return 0;} public int getBottom(){return 0;}
 public ViewParent getParent(){return null;}
 public boolean isLayoutRequested(){return false;} public void requestLayout(){}
 public interface OnAttachStateChangeListener{void onViewAttachedToWindow(View view);void onViewDetachedFromWindow(View view);}
 public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener){} public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener){}
 public ViewTreeObserver getViewTreeObserver(){return null;}
 public boolean post(Runnable r){return false;} public boolean postDelayed(Runnable r,long delay){return false;} public boolean removeCallbacks(Runnable r){return false;}
 public boolean isAttachedToWindow(){return false;} public boolean isShown(){return false;} public void postInvalidateOnAnimation(){}
}
