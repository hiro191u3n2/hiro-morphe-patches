package com.ss.android.vesdk.frame;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import i.s.a.w.m;
public class TECapturePipeline {
    public interface CaptureListener extends i.s.a.w.l0.b.c {}
    public CaptureListener listener=new CaptureListener(){};
    public boolean preview=true,valid=true;
    public m.d format=m.d.PIXEL_FORMAT_OpenGL_OES;
    public SurfaceTexture texture=new SurfaceTexture();
    public Surface surface,recorder;
    public int replacementNotifications;
    public boolean isPreview(){return preview;}
    // Audited stock isValid checks positive size and listener; released textures pass.
    public boolean isValid(){return valid;}
    public m.d getFormat(){return format;}
    public SurfaceTexture getSurfaceTexture(){return texture;}
    public Surface getSurface(){return surface;}
    public Surface getRecorderSurface(){return recorder;}
    public CaptureListener getCaptureListener(){return listener;}
}
