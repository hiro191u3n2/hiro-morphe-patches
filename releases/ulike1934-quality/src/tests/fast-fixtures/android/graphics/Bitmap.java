package android.graphics;
import java.util.*;
/** Host pixel storage fixture. This is not packaged or an Android emulator. */
public final class Bitmap {
 public enum Config{ARGB_8888,RGB_565}
 public static final List<Bitmap> ALL=Collections.synchronizedList(new ArrayList<Bitmap>());
 public static volatile boolean failCreate,failDensity,failWrite;
 public static int writesAfterRecycle;
 private final int width,height;private final int[] data;private int density=160;
 private boolean alpha=true,recycled,writeFailure,densityFailure;
 public int reads;
 private Bitmap(int w,int h){width=w;height=h;data=new int[w*h];ALL.add(this);}
 public static Bitmap from(int w,int h,int[] p){Bitmap b=new Bitmap(w,h);System.arraycopy(p,0,b.data,0,p.length);return b;}
 public static Bitmap createBitmap(int w,int h,Config c){
  if(failCreate){failCreate=false;throw new OutOfMemoryError("create failure");}
  Bitmap b=new Bitmap(w,h);b.writeFailure=failWrite;failWrite=false;b.densityFailure=failDensity;failDensity=false;return b;
 }
 private void check(){if(recycled)throw new IllegalStateException("recycled");}
 public int getWidth(){check();return width;}public int getHeight(){check();return height;}
 public boolean isRecycled(){return recycled;}public void recycle(){recycled=true;}
 public int getDensity(){check();return density;}
 public void setDensity(int d){check();if(densityFailure){densityFailure=false;throw new IllegalStateException("density failure");}density=d;}
 public boolean hasAlpha(){check();return alpha;}public void setHasAlpha(boolean a){check();alpha=a;}
 public synchronized void getPixels(int[] p,int o,int s,int x,int y,int w,int h){
  check();bounds(p,o,s,x,y,w,h);reads++;
  for(int r=0;r<h;r++)System.arraycopy(data,(y+r)*width+x,p,o+r*s,w);
 }
 public synchronized void setPixels(int[] p,int o,int s,int x,int y,int w,int h){
  if(recycled)writesAfterRecycle++;check();if(writeFailure){writeFailure=false;throw new IllegalStateException("write failure");}
  bounds(p,o,s,x,y,w,h);for(int r=0;r<h;r++)System.arraycopy(p,o+r*s,data,(y+r)*width+x,w);
 }
 private void bounds(int[] p,int o,int s,int x,int y,int w,int h){
  if(w<0||h<0||x<0||y<0||x+w>width||y+h>height||s<w||o<0||(h>0&&(long)o+(h-1)*s+w>p.length))
   throw new IllegalArgumentException("bounds");
 }
 public int[] pixels(){check();return data.clone();}
}
