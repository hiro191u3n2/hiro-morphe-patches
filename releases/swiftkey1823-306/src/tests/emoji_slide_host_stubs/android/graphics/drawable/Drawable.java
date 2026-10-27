package android.graphics.drawable;
public abstract class Drawable {
 private final android.graphics.Rect bounds=new android.graphics.Rect();public int invalidates;
 public abstract void draw(android.graphics.Canvas canvas);
 public abstract void setAlpha(int alpha);
 public abstract void setColorFilter(android.graphics.ColorFilter colorFilter);
 public abstract int getOpacity();
 public void setBounds(int l,int t,int r,int b){bounds.set(l,t,r,b);}
 public void setBounds(android.graphics.Rect r){setBounds(r.left,r.top,r.right,r.bottom);}
 public android.graphics.Rect getBounds(){return bounds;}
 public void invalidateSelf(){invalidates++;}
}
