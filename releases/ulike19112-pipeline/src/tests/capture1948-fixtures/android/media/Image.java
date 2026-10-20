package android.media;
import java.nio.ByteBuffer;
import android.graphics.Rect;
public final class Image {
    public static final class Plane {
        private final ByteBuffer bytes;private final int row,pixel;
        public Plane(ByteBuffer b,int r,int p){bytes=b;row=r;pixel=p;}
        public ByteBuffer getBuffer(){return bytes;}public int getRowStride(){return row;}public int getPixelStride(){return pixel;}
    }
    public final int width,height,format;public final long timestamp;public int closes;public Plane[] planes;public Rect crop;
    public Image(int w,int h,int f,long t,Plane[] p){width=w;height=h;format=f;timestamp=t;planes=p;crop=new Rect(0,0,w,h);}
    public int getWidth(){return width;}public int getHeight(){return height;}public int getFormat(){return format;}
    public long getTimestamp(){if(closes>0)throw new IllegalStateException("closed image");return timestamp;}
    public Plane[] getPlanes(){if(closes>0)throw new IllegalStateException("closed image");return planes;}
    public Rect getCropRect(){return crop;}public void close(){closes++;}
}

