package android.view;
import android.content.Context;
public class View {
 public static final int VISIBLE=0,INVISIBLE=4,GONE=8;
 public View(){} public View(Context context){}
 public Context getContext(){return null;}
 public int getWidth(){return 0;} public int getHeight(){return 0;} public int getMeasuredWidth(){return 0;} public int getMeasuredHeight(){return 0;}
 public int getLeft(){return 0;} public int getTop(){return 0;} public int getRight(){return 0;} public int getBottom(){return 0;}
 public int getPaddingLeft(){return 0;} public int getPaddingTop(){return 0;} public int getPaddingRight(){return 0;} public int getPaddingBottom(){return 0;}
 public int getScrollX(){return 0;} public int getScrollY(){return 0;}
 public ViewParent getParent(){return null;} public ViewGroup.LayoutParams getLayoutParams(){return null;}
 public boolean isLayoutRequested(){return false;} public void requestLayout(){}
 public void setPadding(int l,int t,int r,int b){} public void setContentDescription(CharSequence text){} public void setBackgroundColor(int color){} public void bringToFront(){}
 public void setVisibility(int visibility){} public int getVisibility(){return 0;}
 public interface OnClickListener{void onClick(View view);} public void setOnClickListener(OnClickListener listener){} public boolean performClick(){return false;}
 public interface OnAttachStateChangeListener{void onViewAttachedToWindow(View view);void onViewDetachedFromWindow(View view);}
 public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener){} public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener){}
 public ViewTreeObserver getViewTreeObserver(){return null;}
 public boolean post(Runnable r){return false;} public boolean postDelayed(Runnable r,long delay){return false;} public boolean removeCallbacks(Runnable r){return false;}
 public boolean isAttachedToWindow(){return false;} public boolean isShown(){return false;} public void postInvalidateOnAnimation(){}
}
