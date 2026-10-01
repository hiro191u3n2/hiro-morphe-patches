package android.media;
import android.graphics.Rect;
import java.nio.ByteBuffer;
public abstract class Image implements AutoCloseable {
    public abstract int getFormat();
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract int getDataSpace();
    public abstract long getTimestamp();
    public abstract Rect getCropRect();
    public abstract Plane[] getPlanes();
    @Override public abstract void close();
    public abstract static class Plane {
        public abstract ByteBuffer getBuffer();
        public abstract int getRowStride();
        public abstract int getPixelStride();
    }
}
