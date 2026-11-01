package android.graphics;
import java.util.*;
public final class Bitmap {
 public enum Config {ARGB_8888,RGB_565}
 public static int copies,recycles,created,copyFault;public static Runnable afterCopy;public static final List<Bitmap> all=new ArrayList<Bitmap>();
 public final int width,height;public final int[] pixels;public boolean wide,hdr,recycled;public int padding,slack,copySlack;private final Config config;
 public Bitmap(int w,int h){this(w,h,Config.ARGB_8888);}
 public Bitmap(int w,int h,Config c){if(w<1||h<1||(long)w*h>Integer.MAX_VALUE)throw new IllegalArgumentException();width=w;height=h;config=c;pixels=new int[w*h];Arrays.fill(pixels,0xff123456);created++;all.add(this);}
 public int getWidth(){check();return width;}public int getHeight(){check();return height;}public Config getConfig(){check();return config;}
 public boolean isRecycled(){return recycled;}private void check(){if(recycled)throw new IllegalStateException("recycled");}
 public int getRowBytes(){return width*4+padding;}public int getAllocationByteCount(){return getRowBytes()*height+slack;}
 public Bitmap copy(Config c,boolean mutable){check();copies++;if(copyFault==1)return null;if(copyFault==2)throw new OutOfMemoryError("injected copy");if(copyFault==3)throw new java.util.concurrent.CancellationException("injected copy cancellation");Bitmap b=new Bitmap(width,height,c);System.arraycopy(pixels,0,b.pixels,0,pixels.length);b.wide=wide;b.hdr=hdr;b.slack=copySlack;if(afterCopy!=null)afterCopy.run();return b;}
 public void recycle(){if(!recycled){recycled=true;recycles++;}}
 public void getPixels(int[] out,int offset,int stride,int x,int y,int w,int h){check();if(x<0||y<0||x+w>width||y+h>height||stride<w)throw new IllegalArgumentException();for(int r=0;r<h;r++)System.arraycopy(pixels,(y+r)*width+x,out,offset+r*stride,w);}
}
