package i.s.a.w;
import android.hardware.camera2.TotalCaptureResult;
public final class m {
    public enum d {PIXEL_FORMAT_NV21}
    public static final class e {public long c;public TotalCaptureResult d;}
    public final byte[] bytes;public final int width,height,rotation;public e metadata;
    public m(byte[] b,d format,int w,int h,int r){bytes=b;width=w;height=h;rotation=r;}
    public void u(e x){metadata=x;}
}
