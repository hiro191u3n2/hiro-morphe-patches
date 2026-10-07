package android.media;
import android.view.Surface;
import java.util.ArrayDeque;
public final class ImageReader {
    private final int w,h,format;private final Surface surface=new Surface();
    private final ArrayDeque<Image> images=new ArrayDeque<Image>();public int acquired;
    public ImageReader(int width,int height,int f){w=width;h=height;format=f;}
    public int getWidth(){return w;}public int getHeight(){return h;}public int getImageFormat(){return format;}
    public Surface getSurface(){return surface;}
    public void offer(Image image){images.add(image);}
    public Image acquireNextImage(){acquired++;return images.poll();}
}
