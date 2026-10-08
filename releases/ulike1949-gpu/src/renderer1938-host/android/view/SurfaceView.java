package android.view;
public class SurfaceView {
 public boolean attached=true,shown=true,focused=true;public int width=742,height=989;
 public Surface surface=new Surface();
 public boolean isAttachedToWindow(){return attached;}public boolean isShown(){return shown;}public boolean hasWindowFocus(){return focused;}
 public int getWidth(){return width;}public int getHeight(){return height;}
 public Holder getHolder(){return new Holder();}
 public final class Holder {public Surface getSurface(){return surface;}}
}
