package android.graphics;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.hiro.ulike.HostAudit1932;
public final class Bitmap {
 public enum Config {ARGB_8888,RGB_565,RGBA_F16,HARDWARE,ALPHA_8}
 public static final List<Bitmap> ALL=Collections.synchronizedList(new ArrayList<Bitmap>());
 public static volatile boolean failCreateOnce,failCopyOnce,failNextDensity,failNextCreatedWrite,failNextCopyWrite;
 public static final AtomicInteger writesAfterRecycle=new AtomicInteger();
 private final int width,height;private final Config config;private final int[] pixels;
 private final boolean mutable;private volatile boolean recycled;private int density=160;
 private boolean alpha=true,gainmap,premultiplied=true;private ColorSpace colorSpace=new ColorSpace(true);
 private boolean densityFailure,writeFailure;
 public static Runnable afterWriteForTest,beforeRecycleForTest;
 public int reads,writes;
 public int getGenerationId(){check();return 1+writes;}
 private Bitmap(int w,int h,Config c,boolean m){
  if(w<1||h<1||(long)w*h>Integer.MAX_VALUE)throw new IllegalArgumentException("dimensions");
  width=w;height=h;config=c;mutable=m;pixels=new int[w*h];ALL.add(this);
 }
 public static Bitmap from(int w,int h,int[] p,Config c,boolean mutable){
  Bitmap b=new Bitmap(w,h,c,mutable);System.arraycopy(p,0,b.pixels,0,p.length);return b;
 }
 public static Bitmap createBitmap(int w,int h,Config c){
  if(failCreateOnce){failCreateOnce=false;throw new OutOfMemoryError("injected create");}
  Bitmap b=new Bitmap(w,h,c,true);b.densityFailure=failNextDensity;failNextDensity=false;
  b.writeFailure=failNextCreatedWrite;failNextCreatedWrite=false;
  HostAudit1932.event("create:"+w+"x"+h);return b;
 }
 public Bitmap copy(Config c,boolean m){
  check();if(failCopyOnce){failCopyOnce=false;throw new IllegalStateException("injected copy");}
  Bitmap b=new Bitmap(width,height,c,m);System.arraycopy(pixels,0,b.pixels,0,pixels.length);
  b.density=density;b.alpha=alpha;b.premultiplied=premultiplied;b.colorSpace=colorSpace;b.gainmap=gainmap;
  b.writeFailure=failNextCopyWrite;failNextCopyWrite=false;
  HostAudit1932.event("copy:"+width+"x"+height);return b;
 }
 private void check(){if(recycled)throw new IllegalStateException("recycled bitmap");}
 public int getWidth(){check();return width;} public int getHeight(){check();return height;}
 public Config getConfig(){check();return config;} public boolean isMutable(){check();return mutable;}
 public boolean isRecycled(){return recycled;}
 public void recycle(){if(beforeRecycleForTest!=null)beforeRecycleForTest.run();recycled=true;HostAudit1932.event("recycle:"+width+"x"+height);}
 public int getDensity(){check();return density;}
 public void setDensity(int d){check();if(densityFailure){densityFailure=false;throw new IllegalStateException("injected density");}density=d;}
 public boolean isPremultiplied(){check();return premultiplied;} public void setPremultiplied(boolean value){check();premultiplied=value;}
 public boolean hasAlpha(){check();return alpha;}public void setHasAlpha(boolean value){check();alpha=value;}
 public boolean hasGainmap(){check();return gainmap;}public void setGainmapForTest(boolean value){gainmap=value;}
 public ColorSpace getColorSpace(){check();return colorSpace;}public void setColorSpaceForTest(ColorSpace value){colorSpace=value;}
 public synchronized void getPixels(int[] out,int offset,int stride,int x,int y,int w,int h){
  check();bounds(out,offset,stride,x,y,w,h);reads++;
  for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,out,offset+row*stride,w);
 }
 public synchronized void setPixels(int[] in,int offset,int stride,int x,int y,int w,int h){
  if(recycled)writesAfterRecycle.incrementAndGet();check();
  if(!mutable)throw new IllegalStateException("immutable");
  if(writeFailure){writeFailure=false;throw new IllegalStateException("injected write");}
  bounds(in,offset,stride,x,y,w,h);writes++;
  for(int row=0;row<h;row++)System.arraycopy(in,offset+row*stride,pixels,(y+row)*width+x,w);
  if(afterWriteForTest!=null)afterWriteForTest.run();
 }
 private void bounds(int[] data,int offset,int stride,int x,int y,int w,int h){
  if(w<0||h<0||x<0||y<0||x+w>width||y+h>height||stride<w||offset<0||
    (h>0&&(long)offset+(h-1)*stride+w>data.length))throw new IllegalArgumentException("pixel bounds");
 }
 public synchronized int[] snapshot(){check();return pixels.clone();}
}
