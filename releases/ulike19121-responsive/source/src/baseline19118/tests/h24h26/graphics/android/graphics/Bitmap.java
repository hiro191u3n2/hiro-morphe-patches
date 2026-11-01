package android.graphics;
public final class Bitmap {
 public enum Config {RGB_565}
 public static Bitmap createBitmap(int w,int h,Config c){throw new AssertionError("detector not in policy test");}
 public boolean isRecycled(){return false;}
 public int getWidth(){throw new AssertionError();}
 public int getHeight(){throw new AssertionError();}
 public void getPixels(int[] p,int o,int s,int x,int y,int w,int h){throw new AssertionError();}
 public void recycle(){throw new AssertionError();}
}
