package android.graphics;

/** Geometry/readiness fixture only. No image processing is claimed here. */
public final class Bitmap {
 public enum Config {ARGB_8888,RGB_565}
 public final int width,height;public final int[] pixels={0xff123456};private final Config config;private boolean recycled;
 public Bitmap(int w,int h){this(w,h,Config.ARGB_8888);}
 public Bitmap(int w,int h,Config c){if(w<=0||h<=0)throw new IllegalArgumentException();width=w;height=h;config=c;}
 public int getWidth(){check();return width;}public int getHeight(){check();return height;}public Config getConfig(){check();return config;}public boolean isRecycled(){return recycled;}
 public int getRowBytes(){check();return width*4;}public int getAllocationByteCount(){check();return width*height*4;}
 public void recycle(){recycled=true;}private void check(){if(recycled)throw new IllegalStateException("recycled caller");}
 public Bitmap copy(Config c,boolean mutable){check();return new Bitmap(width,height,c);}
 public void getPixels(int[] out,int offset,int stride,int x,int y,int w,int h){throw new AssertionError("geometry fixture cannot supply image pixels");}
}
