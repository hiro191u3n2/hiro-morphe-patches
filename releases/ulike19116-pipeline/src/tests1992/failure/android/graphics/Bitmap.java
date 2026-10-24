package android.graphics;
public final class Bitmap {
    public enum Config {ARGB_8888}
    final int width,height;final int[] pixels;boolean recycled,alpha;int density;
    private Bitmap(int w,int h){width=w;height=h;pixels=new int[w*h];}
    public static Bitmap createBitmap(int w,int h,Config c){return new Bitmap(w,h);}
    public Bitmap copy(Config c,boolean mutable){Bitmap b=new Bitmap(width,height);System.arraycopy(pixels,0,b.pixels,0,pixels.length);return b;}
    public int getWidth(){return width;}public int getHeight(){return height;}public Config getConfig(){return Config.ARGB_8888;}
    public boolean isRecycled(){return recycled;}public void recycle(){recycled=true;}public boolean hasGainmap(){return false;}
    public ColorSpace getColorSpace(){return new ColorSpace();}public int getDensity(){return density;}public void setDensity(int d){density=d;}
    public boolean hasAlpha(){return alpha;}public void setHasAlpha(boolean a){alpha=a;}
    public void getPixels(int[] out,int offset,int stride,int x,int y,int w,int h){for(int r=0;r<h;r++)System.arraycopy(pixels,(y+r)*width+x,out,offset+r*stride,w);}
    public void setPixels(int[] in,int offset,int stride,int x,int y,int w,int h){for(int r=0;r<h;r++)System.arraycopy(in,offset+r*stride,pixels,(y+r)*width+x,w);}
}
