package android.graphics;
import java.util.Arrays;
public final class Bitmap {
 public enum Config {ARGB_8888, RGB_565}
 private final int width,height;private final Config config;private final int[]pixels;private boolean recycled;
 public static int created,recycledCount;public static boolean failAllocation;
 private Bitmap(int w,int h,Config c){width=w;height=h;config=c;pixels=new int[w*h];Arrays.fill(pixels,0xffbc8b76);created++;}
 public static Bitmap createBitmap(int w,int h,Config c){if(failAllocation)throw new OutOfMemoryError("scripted");return new Bitmap(w,h,c);}
 public int getWidth(){return width;}public int getHeight(){return height;}public Config getConfig(){return config;}public boolean isRecycled(){return recycled;}
 public void recycle(){if(!recycled){recycled=true;recycledCount++;}}
 public void getPixels(int[]p,int offset,int stride,int x,int y,int w,int h){for(int yy=0;yy<h;yy++)System.arraycopy(pixels,(y+yy)*width+x,p,offset+yy*stride,w);}
}
