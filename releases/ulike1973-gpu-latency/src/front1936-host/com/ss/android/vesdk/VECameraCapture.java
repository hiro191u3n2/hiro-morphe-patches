package com.ss.android.vesdk;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Looper;
import com.hiro.ulike.FrontPreview1936;
import com.hiro.ulike.PreviewInputs1929;
import com.ss.android.ttvecamera.TECameraSettings;
import com.ss.android.vesdk.frame.TECapturePipeline;
import java.util.concurrent.atomic.AtomicBoolean;
import i.s.a.w.q;

/** Audited native ordering: input setup precedes queued provider/start operations. */
public class VECameraCapture {
    public VECameraSettings a=new VECameraSettings();
    public TECameraSettings c=new TECameraSettings(),b=c;
    public Context d=new Context();
    public ConcurrentList<TECapturePipeline> n=new ConcurrentList<>();
    public AtomicBoolean p=new AtomicBoolean(true);
    public i.s.a.w.k o=new i.s.a.w.k();
    public boolean u,legacyBefore,autoPixels=true,pixelsAfterRestart,pixelsAfterRenewal;
    public boolean pendingConfiguration,keepOpenedAfterStart,pixelsAfterCleanup;
    public int starts,accepted,newSurfaceRequests,nativeRenewals,nativeFailures;
    public int forced=Integer.MAX_VALUE;
    public RuntimeException failure;
    public Runnable during;
    public i.s.a.w.l0.b latestProvider;
    public void newSurfaceTexture(){newSurfaceRequests++;u=true;}
    public VECameraSettings.CAMERA_FACING_ID getCameraFacing(){return a.getCameraFacing();}
    public int getCameraState(){return q.INSTANCE.mCurrentCameraState;}
    public boolean isCameraSwitchState(){return q.INSTANCE.mIsCameraSwitchState;}
    public int startPreview() {
        starts++;
        if(failure!=null)throw failure;
        if(during!=null)during.run();
        boolean allow=legacyBefore?PreviewInputs1929.before(this):FrontPreview1936.before(this);
        if(!allow)return -100;
        if(n==null||n.isEmpty())return -100;
        if(!p.get())return -105;
        if(forced!=Integer.MAX_VALUE)return forced;
        TECapturePipeline source=null;
        for(TECapturePipeline pipe:n.getImmutableList())if(pipe!=null&&pipe.isValid()) {
            if(u&&pipe.texture!=null&&pipe.format==i.s.a.w.m.d.PIXEL_FORMAT_OpenGL_OES) {
                pipe.texture.release();pipe.texture=new SurfaceTexture();
                pipe.replacementNotifications++;nativeRenewals++;
            }
            if(pipe.isPreview())source=pipe;
        }
        u=false;
        if(source==null)return -1;
        final TECapturePipeline pipeline=source;
        final SurfaceTexture texture=pipeline.texture;
        final q server=q.INSTANCE;
        Runnable nativeStart=new Runnable(){public void run(){
            if(server.mCameraClient!=o||server.mCameraSettings!=c||server.mCameraInstance==null)return;
            if(server.mCurrentCameraState!=2&&server.mCurrentCameraState!=3)return;
            boolean valid=true;
            if(pipeline.format==i.s.a.w.m.d.PIXEL_FORMAT_OpenGL_OES)
                valid=texture!=null?!texture.isReleased():pipeline.surface!=null&&pipeline.surface.isValid();
            else if(pipeline.format==i.s.a.w.m.d.PIXEL_FORMAT_Recorder)
                valid=pipeline.recorder!=null&&pipeline.recorder.isValid()&&(texture==null||!texture.isReleased());
            if(!valid){nativeFailures++;server.mStartPreviewError=true;server.mCurrentCameraState=3;return;}
            Object camera=server.mCameraInstance;
            i.s.a.w.l0.c manager=camera instanceof i.s.a.w.g?((i.s.a.w.g)camera).g:((i.s.a.w.d)camera).g;
            i.s.a.w.l0.b old=manager.a;
            boolean changed=old==null||old.texture!=texture||old.d!=camera||old.a!=pipeline.listener;
            if(server.mCurrentCameraState==3&&!changed&&!server.mStartPreviewError)return;
            latestProvider=changed?new i.s.a.w.l0.b(camera):old;
            if(changed){
                latestProvider.a=pipeline.listener;
                latestProvider.texture=texture;latestProvider.surface=pipeline.surface==null?new android.view.Surface():pipeline.surface;
                latestProvider.b=pipeline.format;manager.a=latestProvider;server.mProviderManager=manager;
            }
            latestProvider.e=true;latestProvider.h=false;
            server.mCurrentCameraState=3;server.mStartPreviewError=false;accepted++;
            if(camera instanceof i.s.a.w.g) {
                i.s.a.w.g camera2=(i.s.a.w.g)camera;camera2.I=3;camera2.c=true;
                if(!pendingConfiguration&&camera2.M!=null&&camera2.M.d==null)camera2.M.d=new android.hardware.camera2.CameraCaptureSession();
            } else ((i.s.a.w.d)camera).c=true;
            if(keepOpenedAfterStart)server.mCurrentCameraState=2;
            boolean cleaned=camera instanceof i.s.a.w.g&&((i.s.a.w.g)camera).sessionCleanups>0;
            if((autoPixels||pixelsAfterCleanup&&cleaned||pixelsAfterRestart&&server.stops>0||pixelsAfterRenewal&&nativeRenewals>0)&&texture!=null)FrontPreview1936.pixel(latestProvider,new Object());
        }};
        if(Looper.myLooper()==server.mHandler.getLooper())nativeStart.run();else if(!server.mHandler.post(nativeStart))return -112;
        return 0;
    }
}
