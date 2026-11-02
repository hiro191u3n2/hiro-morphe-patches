package android.view;
public class ViewTreeObserver {
 public interface OnScrollChangedListener{void onScrollChanged();}
 public interface OnPreDrawListener{boolean onPreDraw();} public interface OnDrawListener{void onDraw();}
 public void addOnScrollChangedListener(OnScrollChangedListener l){}
 public void removeOnScrollChangedListener(OnScrollChangedListener l){}
 public boolean isAlive(){return false;}
 public void addOnPreDrawListener(OnPreDrawListener l){} public void removeOnPreDrawListener(OnPreDrawListener l){}
 public void addOnDrawListener(OnDrawListener l){} public void removeOnDrawListener(OnDrawListener l){}
}
