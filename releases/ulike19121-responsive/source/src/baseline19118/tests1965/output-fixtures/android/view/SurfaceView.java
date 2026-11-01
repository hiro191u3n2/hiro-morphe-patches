package android.view;
import android.os.Looper;
public class SurfaceView {
 public boolean attached=true,shown=true,focused=true;public int width=742,height=989;public Surface surface=new Surface();
 private void main(){if(Looper.myLooper()!=Looper.getMainLooper())throw new AssertionError("View read outside MAIN");}
 public boolean isAttachedToWindow(){main();return attached;}public boolean isShown(){main();return shown;}
 public int getWidth(){main();return width;}public int getHeight(){main();return height;}
 public Holder getHolder(){main();return new Holder();}
 public final class Holder {public Surface getSurface(){main();return surface;}}
}
