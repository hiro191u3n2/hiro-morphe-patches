package i.s.a.w.l0;
import android.graphics.SurfaceTexture;
import android.view.Surface;
public class b {
    public interface c {}
    public Object d,a=new Object(),j=new Object(),k=new Object();
    public boolean e=true,h;
    public i.s.a.w.m.d b=i.s.a.w.m.d.PIXEL_FORMAT_OpenGL_OES;
    public SurfaceTexture texture=new SurfaceTexture();
    public Surface surface=new Surface();
    public int detached;
    public b(Object camera){d=camera;}
    public SurfaceTexture f(){return texture;}
    public Surface d(){return surface;}
    public void p(){if(texture!=null)texture.release();}
    public void o(){detached++;a=j;if(surface!=null)surface.release();}
}
