package android.graphics;
import java.util.ArrayList;
import java.util.List;
public final class Bitmap {
 public enum Config {ARGB_8888}
 public static final List<Bitmap> ALL=new ArrayList<Bitmap>();
 public static boolean failCreate;
 public final int width,height;public final int[] pixels;public boolean recycled;
 private Bitmap(int width,int height){this.width=width;this.height=height;pixels=new int[width*height];ALL.add(this);}
 public static Bitmap createBitmap(int width,int height,Config config){if(failCreate)throw new OutOfMemoryError("injected destination allocation");return new Bitmap(width,height);}
 public void getPixels(int[] destination,int offset,int stride,int x,int y,int width,int height){
  if(recycled)throw new AssertionError("read after recycle");for(int row=0;row<height;row++)System.arraycopy(pixels,(y+row)*this.width+x,destination,offset+row*stride,width);
 }
 public boolean isRecycled(){return recycled;}
 public void recycle(){if(recycled)throw new AssertionError("double recycle");recycled=true;}
}
