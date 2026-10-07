package com.hiro.ulike;
import android.os.*;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.view.Surface;
import com.ss.android.vesdk.*;
import com.ss.android.vesdk.frame.TECapturePipeline;
import com.ss.android.ttvecamera.TECameraSettings;
import i.s.a.w.q;
import java.lang.ref.WeakReference;
import java.util.*;

/** External effects are asserted; the helper's private Ticket implementation is not mirrored. */
public final class FrontTest1931 {
    static int checks;
    static VECameraCapture capture;
    static OpticalZoom.Route route;
    static i.s.a.w.h0.b mode;
    static i.s.a.w.g camera2;
    static i.s.a.w.d camera1;
    static void ck(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static void reset(boolean legacy,boolean api1) {
        if(capture!=null)FrontPreview1931.cancel(capture);
        Handler.reset();SystemClock.now=100;Build.VERSION.SDK_INT=36;
        OpticalZoom.MAIN=new Handler(Looper.getMainLooper());
        ManualLens170.f.clear();ExitBusy1921.busy=false;RearRestart1926.ready=true;
        capture=new VECameraCapture();capture.legacyBefore=legacy;
        q server=q.INSTANCE;
        server.mHandler=new Handler();server.mCameraClient=capture.o;server.mCameraSettings=capture.c;
        server.mCurrentCameraState=2;server.mIsCameraPendingClose=false;server.mIsCameraSwitchState=false;
        server.mHandlerDestroyed=false;server.mOnBackGround=false;server.mIsInitialized=true;
        server.mIsForegroundVisible=true;server.mStartPreviewError=false;server.mIsCameraProviderChanged=false;
        if(api1) {
            capture.c.j=1;camera1=new i.s.a.w.d();camera1.b=capture.c;
            server.mCameraInstance=camera1;server.mProviderManager=camera1.g;camera2=null;mode=null;
        } else {
            camera2=new i.s.a.w.g();camera2.b=capture.c;mode=new i.s.a.w.h0.b();
            camera2.M=mode;mode.g=camera2;mode.j=camera2.K;mode.h=capture.c;mode.k=server.mHandler;
            server.mCameraInstance=camera2;server.mProviderManager=camera2.g;camera1=null;
        }
        route=api1?null:new OpticalZoom.Route(mode);
        ManualLens170.f.put("capture",new WeakReference<Object>(capture));
        ManualLens170.f.put("active",route);ManualLens170.f.put("epoch",7L);
        ManualLens170.f.put("foreground",true);ManualLens170.f.put("recording",false);
        ManualLens170.f.put("lastFrame",0L);ManualLens170.f.put("LOCK",new Object());
        ManualLens170.f.put("rejected",new HashSet<String>());
    }
    static TECapturePipeline ready(){TECapturePipeline pipeline=new TECapturePipeline();if(capture.n==null)capture.n=new ConcurrentList<>();capture.n.list.add(pipeline);return pipeline;}
    static void tick(){Handler.until(SystemClock.now+200L);}
    static void settle(){Handler.until(SystemClock.now+6000L);}
    static int begin(){return FrontPreview1931.start(capture);}
    static void lost(){ck(begin()==-100,"native missing-input return is retained");}
    static void sourceEvent(){FrontPreview1931.pipelines(capture);tick();}

    static void correctedRegressions() {
        reset(true,false);
        ck(PreviewStart1927.start(capture)==-100,"before: missing front pipeline returns native error");
        ready();PreviewStart1927.pipelines(capture);settle();
        ck(capture.starts==1&&capture.accepted==0,"before: late front inputs permanently lose the preview start");
        System.out.println("REPRODUCED before: late front pipeline leaves preview unstarted");

        reset(false,false);lost();ready();sourceEvent();settle();
        ck(capture.starts==2&&capture.accepted==1,"after: late front pipeline starts exactly once");

        reset(true,false);TECapturePipeline before=ready();before.texture.released=true;
        ck(before.isValid(),"stock isValid does not detect released SurfaceTexture");
        PreviewStart1927.start(capture);settle();
        ck(capture.newSurfaceRequests==0&&capture.nativeFailures==1&&capture.accepted==0,"before: released front texture reaches the native failure path");
        System.out.println("REPRODUCED before: released front texture is accepted by stock isValid");

        reset(false,false);TECapturePipeline after=ready();SurfaceTexture old=after.texture;old.released=true;
        begin();settle();
        ck(capture.newSurfaceRequests==1&&capture.nativeRenewals==1,"after: one native texture renewal is requested");
        ck(after.texture!=old&&!after.texture.released&&after.replacementNotifications==1,"after: native replacement notifies the renderer once");
        ck(capture.accepted==1&&capture.nativeFailures==0,"after: renewed front input starts without the released texture failure");
    }
    static void ordering() {
        reset(false,false);lost();tick();ready();tick();settle();
        ck(capture.accepted==1&&capture.starts==2,"late readiness is polled even without a setter event");
        reset(false,false);lost();ready();for(int n=0;n<25;n++)FrontPreview1931.pipelines(capture);settle();
        ck(capture.accepted==1&&capture.starts==2,"pipeline event storm cannot duplicate native preview");
        reset(false,false);mode.d=new Object();route.configured=true;lost();ready();sourceEvent();settle();
        ck(capture.accepted==1,"front lost start is recovered with a pre-existing deferred session");
        reset(false,true);ck(route==null,"Camera1 has no Camera2 OpticalZoom route");lost();ready();sourceEvent();settle();
        ck(capture.accepted==1&&capture.starts==2,"Camera1 route-free front startup recovers");
        reset(false,false);lost();ready();ck(begin()==0,"new native start supersedes the wait");settle();
        ck(capture.starts==2&&capture.accepted==1,"external native success prevents later duplicate retries");
        reset(false,false);lost();settle();ready();sourceEvent();
        ck(capture.starts==1&&capture.accepted==0,"expired front request is never replayed after five seconds");
        reset(false,false);ready();begin();settle();
        ck(capture.starts==1&&capture.accepted==1&&capture.newSurfaceRequests==0,"normal front path has no extra starts or texture renewals");
        reset(false,false);capture.c.l=0;capture.a.facing=VECameraSettings.CAMERA_FACING_ID.FACING_BACK;route.rear=true;
        ready();begin();settle();
        ck(capture.starts==1&&capture.accepted==1&&capture.newSurfaceRequests==0,"normal rear path has no extra starts or texture renewals");
    }
    static void cancellation() {
        reset(false,false);lost();FrontPreview1931.cancel(capture);ready();sourceEvent();settle();
        ck(capture.starts==1&&capture.accepted==0,"explicit stop/switch/shutdown cancellation removes queued front work");
        for(int stale=0;stale<12;stale++) {
            reset(false,stale==7);lost();
            switch(stale) {
                case 0:ManualLens170.f.put("foreground",false);break;
                case 1:ManualLens170.f.put("epoch",8L);break;
                case 2:ManualLens170.f.put("capture",new WeakReference<Object>(new VECameraCapture()));break;
                case 3:q.INSTANCE.mCameraClient=new i.s.a.w.k();break;
                case 4:q.INSTANCE.mCameraSettings=new TECameraSettings();break;
                case 5:capture.c=new TECameraSettings();break;
                case 6:camera2.K=new CameraDevice();break;
                case 7:camera1.H=new Camera();break;
                case 8:q.INSTANCE.mCameraInstance=new i.s.a.w.g();break;
                case 9:capture.c.l=0;capture.a.facing=VECameraSettings.CAMERA_FACING_ID.FACING_BACK;break;
                case 10:capture.d=null;break;
                case 11:capture.p.set(false);break;
            }
            ready();sourceEvent();settle();
            ck(capture.starts==1&&capture.accepted==0,"stale capture/native generation cannot start "+stale);
        }
        for(int state:new int[]{0,1,4}) {
            reset(false,false);lost();q.INSTANCE.mCurrentCameraState=state;ready();sourceEvent();settle();
            ck(capture.accepted==0,"native unopened/closing state blocks replay "+state);
        }
        for(int flag=0;flag<4;flag++) {
            reset(false,false);lost();
            if(flag==0)q.INSTANCE.mIsCameraPendingClose=true;
            if(flag==1)q.INSTANCE.mIsCameraSwitchState=true;
            if(flag==2)q.INSTANCE.mHandlerDestroyed=true;
            if(flag==3)q.INSTANCE.mOnBackGround=true;
            ready();sourceEvent();settle();
            ck(capture.accepted==0,"native shutdown/switch/foreground guard "+flag);
        }
    }
    static void busyAndHandlerGates() {
        for(int gate=0;gate<3;gate++) {
            reset(false,false);lost();ready();
            if(gate==0)ManualLens170.f.put("recording",true);
            if(gate==1)ExitBusy1921.busy=true;
            if(gate==2)RearRestart1926.ready=false;
            sourceEvent();ck(capture.starts==1&&capture.accepted==0,"busy/window gate preserves the native start "+gate);
            ManualLens170.f.put("recording",false);ExitBusy1921.busy=false;RearRestart1926.ready=true;
            tick();settle();ck(capture.accepted==1,"pending front start resumes when busy gate clears "+gate);
        }
        for(int gate=0;gate<2;gate++) {
            reset(false,false);TECapturePipeline p=ready();p.texture.released=true;
            if(gate==0)ManualLens170.f.put("recording",true);else ExitBusy1921.busy=true;
            ck(!FrontPreview1931.before(capture)&&capture.newSurfaceRequests==0,"released texture never renewed during capture/recording "+gate);
        }
        reset(false,false);OpticalZoom.MAIN.accept=false;lost();ready();sourceEvent();settle();
        ck(capture.starts==1&&capture.accepted==0,"rejected main-handler work does not escape cancellation");
        reset(false,false);lost();q.INSTANCE.mHandler.accept=false;ready();sourceEvent();settle();
        ck(capture.accepted==0&&capture.starts==1,"rejected native-handler work cannot start the camera");
        ck(Handler.queued()==0,"handler rejection finishes with no queued work");
        reset(false,false);lost();q.INSTANCE.mHandler.held=true;ready();sourceEvent();settle();
        q.INSTANCE.mHandler.held=false;tick();
        ck(capture.starts==1&&capture.accepted==0&&Handler.queued()==0,"native handler stall cannot outlive the five-second ticket deadline");
        reset(false,false);capture.during=new Runnable(){public void run(){FrontPreview1931.cancel(capture);}};lost();
        capture.during=null;ready();sourceEvent();settle();
        ck(capture.starts==1,"cancel during the original call prevents wait resurrection");
        for(int error:new int[]{-401,-402,-407,-408,-409,-410,-425}) {
            reset(false,false);ready();capture.forced=error;ck(begin()==error,"native error return retained "+error);settle();
            ck(capture.starts==1,"front helper does not repeat permission/driver errors "+error);
        }
    }
    static void inputVariants() {
        reset(false,false);TECapturePipeline p=ready();p.texture=null;p.surface=new Surface();begin();settle();
        ck(capture.accepted==1&&capture.newSurfaceRequests==0,"external Surface-only front preview remains native");
        reset(false,false);p=ready();p.format=i.s.a.w.m.d.PIXEL_FORMAT_YUV420;p.texture=null;begin();settle();
        ck(capture.accepted==1&&capture.newSurfaceRequests==0,"native buffer input remains supported");
        reset(false,false);p=ready();p.format=i.s.a.w.m.d.PIXEL_FORMAT_Recorder;p.recorder=new Surface();begin();settle();
        ck(capture.accepted==1&&capture.newSurfaceRequests==0,"valid Recorder input does not fabricate a texture");
        reset(false,false);p=ready();p.texture=null;p.surface=new Surface();p.surface.valid=false;
        ck(!FrontPreview1931.before(capture)&&capture.newSurfaceRequests==0,"invalid external Surface is withheld without fabrication");
        reset(false,false);p=ready();p.valid=false;
        ck(!FrontPreview1931.before(capture),"invalid pipeline remains unready");
        reset(false,false);p=ready();p.preview=false;
        ck(!FrontPreview1931.before(capture),"no preview target is not treated as ready");
    }
    static void preparationAndPixels() {
        reset(false,false);FrontPreview1931.prepared(mode,0);
        ck(!mode.h.u0,"front initialization waits for concrete preview outputs");
        reset(false,false);FrontPreview1931.prepared(mode,-1);
        ck(mode.h.u0,"failed native mode preparation is not modified");
        reset(false,false);capture.c.l=0;capture.a.facing=VECameraSettings.CAMERA_FACING_ID.FACING_BACK;route.rear=true;
        FrontPreview1931.prepared(mode,0);
        ck(mode.h.u0,"rear deferred-session policy is delegated to its existing helper");
        reset(false,false);lost();TECapturePipeline p=ready();
        i.s.a.w.l0.b provider=new i.s.a.w.l0.b(camera2);provider.a=p.listener;provider.texture=p.texture;
        camera2.g.a=provider;q.INSTANCE.mCurrentCameraState=3;camera2.I=3;
        FrontPreview1931.pixel(provider,new Object());sourceEvent();settle();
        ck(capture.starts==1,"actual owned front image stops an outstanding bootstrap before replay");
        for(int bad=0;bad<7;bad++) {
            reset(false,false);lost();p=ready();provider=new i.s.a.w.l0.b(camera2);
            provider.a=p.listener;provider.texture=p.texture;camera2.g.a=provider;
            if(bad==0)provider.d=new i.s.a.w.g();
            if(bad==1)provider.a=provider.j;
            if(bad==2)provider.a=provider.k;
            if(bad==3)provider.h=true;
            if(bad==4)provider.e=false;
            if(bad==5)camera2.g.a=new i.s.a.w.l0.b(camera2);
            if(bad==6)provider.a=new Object();
            FrontPreview1931.pixel(provider,new Object());sourceEvent();settle();
            ck(capture.accepted==1,"foreign/stopped/dummy frame cannot cancel live front recovery "+bad);
        }
    }
    public static void main(String[] args) {
        correctedRegressions();ordering();cancellation();busyAndHandlerGates();inputVariants();preparationAndPixels();
        System.out.println("PASS1931 front lifecycle assertions="+checks);
    }
}
