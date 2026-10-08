package android.graphics;
public class SurfaceTexture {
    public boolean released;
    public int releases;
    public boolean isReleased(){return released;}
    public void release(){released=true;releases++;}
}
