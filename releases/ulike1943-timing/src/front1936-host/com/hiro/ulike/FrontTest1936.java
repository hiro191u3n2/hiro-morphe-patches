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
public final class FrontTest1936 {
    static int checks;
    static VECameraCapture capture;
    static OpticalZoom.Route route;
    static i.s.a.w.h0.b mode;
    static i.s.a.w.g camera2;
    static i.s.a.w.d camera1;
    static void ck(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static void reset(boolean legacy,boolean api1) {
        if(capture!=null)FrontPreview1936.cancel(capture);
        Handler.reset();SystemClock.now=100;Build.VERSION.SDK_INT=36;
        OpticalZoom.MAIN=new Handler(Looper.getMainLooper());
        ManualLens170.f.clear();ExitBusy1921.busy=false;RearRestart1926.ready=true;
        capture=new VECameraCapture();capture.legacyBefore=legacy;
        q server=q.INSTANCE;
        server.mHandler=new Handler();server.mCameraClient=capture.o;server.mCameraSettings=capture.c;
        server.mCurrentCameraState=2;server.mIsCameraPendingClose=false;server.mIsCameraSwitchState=false;
        server.mHandlerDestroyed=false;server.mOnBackGround=false;server.mIsInitialized=true;
        server.mIsForegroundVisible=true;server.mStartPreviewError=false;server.mIsCameraProviderChanged=false;
        server.stops=0;server.stopResult=0;server.duringStop=null;
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
    static void settle(){Handler.until(SystemClock.now+21000L);}
    static int begin(){return FrontPreview1936.start(capture);}
    static void lost(){ck(begin()==-100,"native missing-input return is retained");}
    static void sourceEvent(){FrontPreview1936.pipelines(capture);tick();}

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
        reset(false,false);lost();ready();for(int n=0;n<25;n++)FrontPreview1936.pipelines(capture);settle();
        ck(capture.accepted==1&&capture.starts==2,"pipeline event storm cannot duplicate native preview");
        reset(false,false);mode.d=new Object();route.configured=true;lost();ready();sourceEvent();settle();
        ck(capture.accepted==1,"front lost start is recovered with a pre-existing deferred session");
        reset(false,true);ck(route==null,"Camera1 has no Camera2 OpticalZoom route");lost();ready();sourceEvent();settle();
        ck(capture.accepted==1&&capture.starts==2,"Camera1 route-free front startup recovers");
        reset(false,false);lost();ready();ck(begin()==0,"new native start supersedes the wait");settle();
        ck(capture.starts==2&&capture.accepted==1,"external native success prevents later duplicate retries");
        reset(false,false);lost();settle();ready();sourceEvent();
        ck(capture.starts==1&&capture.accepted==0,"expired front request is never replayed after its bounded input window");
        reset(false,false);ready();begin();settle();
        ck(capture.starts==1&&capture.accepted==1&&capture.newSurfaceRequests==0,"normal front path has no extra starts or texture renewals");
        reset(false,false);capture.c.l=0;capture.a.facing=VECameraSettings.CAMERA_FACING_ID.FACING_BACK;route.rear=true;
        ready();begin();settle();
        ck(capture.starts==1&&capture.accepted==1&&capture.newSurfaceRequests==0,"normal rear path has no extra starts or texture renewals");
    }
    static void cancellation() {
        reset(false,false);lost();FrontPreview1936.cancel(capture);ready();sourceEvent();settle();
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
            ck(!FrontPreview1936.before(capture)&&capture.newSurfaceRequests==0,"released texture never renewed during capture/recording "+gate);
        }
        reset(false,false);OpticalZoom.MAIN.accept=false;lost();ready();sourceEvent();settle();
        ck(capture.starts==1&&capture.accepted==0,"rejected main-handler work does not escape cancellation");
        reset(false,false);lost();q.INSTANCE.mHandler.accept=false;ready();sourceEvent();settle();
        ck(capture.accepted==0&&capture.starts==1,"rejected native-handler work cannot start the camera");
        ck(Handler.queued()==0,"handler rejection finishes with no queued work");
        reset(false,false);lost();q.INSTANCE.mHandler.held=true;ready();sourceEvent();settle();
        q.INSTANCE.mHandler.held=false;tick();
        ck(capture.starts==1&&capture.accepted==0&&Handler.queued()==0,"native handler stall cannot outlive the bounded input ticket deadline");
        reset(false,false);capture.during=new Runnable(){public void run(){FrontPreview1936.cancel(capture);}};lost();
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
        ck(!FrontPreview1936.before(capture)&&capture.newSurfaceRequests==0,"invalid external Surface is withheld without fabrication");
        reset(false,false);p=ready();p.valid=false;
        ck(!FrontPreview1936.before(capture),"invalid pipeline remains unready");
        reset(false,false);p=ready();p.preview=false;
        ck(!FrontPreview1936.before(capture),"no preview target is not treated as ready");
    }
    static void preparationAndPixels() {
        reset(false,false);FrontPreview1936.prepared(mode,0);
        ck(!mode.h.u0,"front initialization waits for concrete preview outputs");
        reset(false,false);FrontPreview1936.prepared(mode,-1);
        ck(mode.h.u0,"failed native mode preparation is not modified");
        reset(false,false);capture.c.l=0;capture.a.facing=VECameraSettings.CAMERA_FACING_ID.FACING_BACK;route.rear=true;
        FrontPreview1936.prepared(mode,0);
        ck(mode.h.u0,"rear deferred-session policy is delegated to its existing helper");
        reset(false,false);lost();TECapturePipeline p=ready();
        i.s.a.w.l0.b provider=new i.s.a.w.l0.b(camera2);provider.a=p.listener;provider.texture=p.texture;
        camera2.g.a=provider;q.INSTANCE.mCurrentCameraState=3;camera2.I=3;
        FrontPreview1936.pixel(provider,new Object());sourceEvent();settle();
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
            FrontPreview1936.pixel(provider,new Object());sourceEvent();settle();
            ck(capture.accepted==1,"foreign/stopped/dummy frame cannot cancel live front recovery "+bad);
        }
    }
    static void asynchronousAcceptance() {
        reset(false,false);ready();capture.autoPixels=false;capture.pixelsAfterRestart=true;
        ck(FrontPreview1931.start(capture)==0,"before: asynchronous native request is accepted");
        settle();
        ck(capture.accepted==1&&capture.newSurfaceRequests==0&&q.INSTANCE.stops==0,"before: accepted start without frames is never recovered");
        System.out.println("REPRODUCED before: asynchronous zero result permanently abandons front first-frame startup");

        reset(false,false);ready();capture.autoPixels=false;capture.pixelsAfterRestart=true;
        ck(begin()==0,"after: original asynchronous zero result is retained");
        Handler.until(SystemClock.now+FrontPreview1936.FRAME_GRACE_MS-1);
        ck(capture.newSurfaceRequests==0&&q.INSTANCE.stops==0,"after: queued start gets a full first-frame grace period");
        tick();settle();
        ck(capture.accepted==2&&capture.nativeRenewals==1&&capture.newSurfaceRequests==1&&q.INSTANCE.stops==1,"after: missing first frame triggers one native detached-texture renewal and session restart");
        ck(Handler.queued()==0,"after: successful rebuilt first frame removes all observation work");

        reset(false,false);ready();capture.autoPixels=false;begin();tick();
        FrontPreview1936.pixel(capture.latestProvider,new Object());settle();
        ck(capture.starts==1&&capture.newSurfaceRequests==0,"ordinary asynchronous first frame never triggers a renewal");

        reset(false,false);ready();q.INSTANCE.mHandler.post(new Runnable(){public void run(){begin();}});settle();
        ck(capture.starts==1&&capture.newSurfaceRequests==0&&Handler.queued()==0,"synchronous native-handler first frame cannot resurrect a completed ticket");

        reset(false,false);ready();capture.autoPixels=false;begin();settle();
        ck(q.INSTANCE.stops==1&&capture.newSurfaceRequests==1&&capture.starts==2&&Handler.queued()==0,"permanent HAL or renderer failure has one session retry and no infinite loop");

        reset(false,false);TECapturePipeline p=ready();p.textureId=8;capture.autoPixels=false;begin();settle();
        ck(capture.newSurfaceRequests==0&&q.INSTANCE.stops==1&&capture.starts==2,"nonzero GL texture id is preserved during a camera-handler session restart");

        for(int gate=0;gate<3;gate++){
            reset(false,false);ready();capture.autoPixels=false;begin();tick();
            if(gate==0)ManualLens170.f.put("recording",true);
            if(gate==1)ExitBusy1921.busy=true;
            if(gate==2)RearRestart1926.ready=false;
            settle();
            ck(capture.newSurfaceRequests==0&&q.INSTANCE.stops==0,"accepted but frameless preview is not disturbed while busy/window inactive "+gate);
        }

        reset(false,false);p=ready();capture.autoPixels=false;begin();tick();
        p.texture=new SurfaceTexture();
        FrontPreview1936.pixel(capture.latestProvider,new Object());
        capture.pixelsAfterRestart=true;settle();
        ck(q.INSTANCE.stops==1,"old provider texture cannot falsely complete the new preview input");
    }
    static void transientNativeLifecycle() {
        for(int flag=0;flag<2;flag++){
            reset(false,false);ready();
            if(flag==0)q.INSTANCE.mOnBackGround=true;else q.INSTANCE.mIsCameraSwitchState=true;
            ck(begin()==-100,"native transition keeps original not-ready result "+flag);
            tick();ck(capture.accepted==0,"native transition does not force preview start "+flag);
            q.INSTANCE.mOnBackGround=false;q.INSTANCE.mIsCameraSwitchState=false;
            settle();
            ck(capture.accepted==1&&capture.starts==2,"front request survives transient native transition until ready "+flag);
        }
        reset(false,false);ready();q.INSTANCE.mOnBackGround=true;begin();
        FrontPreview1936.cancel(capture);q.INSTANCE.mOnBackGround=false;settle();
        ck(capture.starts==1&&capture.accepted==0,"real background cancellation still invalidates transient wait");
    }
    static void retainedSessionRestart() {
        for(int api=0;api<2;api++){
            reset(false,api==1);TECapturePipeline p=ready();p.textureId=9;
            capture.autoPixels=false;capture.pixelsAfterRestart=true;begin();tick();
            Object device=api==0?camera2.K:camera1.H;
            Object texture=p.texture,provider=capture.latestProvider;
            settle();
            ck(q.INSTANCE.stops==1&&capture.accepted==2,"missing first frame restarts exactly one native preview session "+api);
            ck((api==0?camera2.K:camera1.H)==device&&p.texture==texture&&capture.latestProvider==provider,
                    "session restart preserves opened device, provider and nonzero GL texture "+api);
            ck(capture.newSurfaceRequests==0&&p.replacementNotifications==0,"watchdog never releases/recreates or renotifies GL texture "+api);
        }
        for(int changed=0;changed<6;changed++){
            reset(false,false);ready();capture.autoPixels=false;begin();
            final int event=changed;
            q.INSTANCE.duringStop=new Runnable(){public void run(){
                if(event==0)FrontPreview1936.cancel(capture);
                if(event==1)ManualLens170.f.put("foreground",false);
                if(event==2)camera2.K=new CameraDevice();
                if(event==3)q.INSTANCE.mCameraClient=new i.s.a.w.k();
                if(event==4)q.INSTANCE.mCameraSettings=new TECameraSettings();
                if(event==5)q.INSTANCE.mHandler=new Handler();
            }};
            settle();
            ck(q.INSTANCE.stops==1&&capture.starts==1,"stop callback lifecycle change never restarts stale preview "+changed);
            ck(Handler.queued()==0,"stale or expired stop completion leaves no queued retry "+changed);
        }
        reset(false,false);ready();capture.autoPixels=false;capture.pixelsAfterRestart=true;
        q.INSTANCE.duringStop=new Runnable(){public void run(){ExitBusy1921.busy=true;}};
        begin();Handler.until(SystemClock.now+2500L);
        ck(q.INSTANCE.stops==1&&capture.starts==1&&q.INSTANCE.mCurrentCameraState==2,"busy reentry after stop waits with its same retained camera");
        ExitBusy1921.busy=false;settle();
        ck(q.INSTANCE.stops==1&&capture.starts==2&&capture.accepted==2,"cleared busy gate resumes stopped preview without a second stop");

        reset(false,false);ready();capture.autoPixels=false;q.INSTANCE.stopResult=-105;begin();settle();
        ck(q.INSTANCE.stops==0&&capture.starts==1&&Handler.queued()==0,"failed native stop is never followed by a forced start");

        reset(false,false);ready();capture.autoPixels=false;begin();settle();
        ck(q.INSTANCE.stops==1,"initial frameless generation consumes one recovery");
        begin();settle();ck(q.INSTANCE.stops==1,"new start call after timeout cannot replenish same generation recovery budget");

        reset(false,false);ready();capture.autoPixels=false;begin();
        Handler.until(SystemClock.now+1500L);begin();Handler.until(SystemClock.now+1000L);
        ck(q.INSTANCE.stops==0,"external accepted start receives a fresh grace period within the original deadline");
        settle();ck(q.INSTANCE.stops==1,"external start does not add another bounded session recovery");

        reset(false,false);ready();capture.autoPixels=false;capture.pixelsAfterRestart=true;
        q.INSTANCE.duringStop=new Runnable(){public void run(){FrontPreview1936.pixel(capture.latestProvider,new Object());}};
        begin();settle();
        ck(q.INSTANCE.stops==1&&capture.starts==2&&q.INSTANCE.mCurrentCameraState==3,
                "final frame delivered reentrantly during native stop cannot cancel the matching resume");
        ck(Handler.queued()==0,"resumed first frame completes the in-flight recovery phase");

        reset(false,false);ready();capture.autoPixels=false;capture.pixelsAfterRestart=true;
        q.INSTANCE.duringStop=new Runnable(){public void run(){ExitBusy1921.busy=true;FrontPreview1936.pixel(capture.latestProvider,new Object());}};
        begin();Handler.until(SystemClock.now+2500L);
        FrontPreview1936.pixel(capture.latestProvider,new Object());
        ck(capture.starts==1&&q.INSTANCE.mCurrentCameraState==2,"late stopped-session frame does not remove busy-deferred resume");
        ExitBusy1921.busy=false;settle();
        ck(capture.starts==2&&q.INSTANCE.stops==1&&q.INSTANCE.mCurrentCameraState==3,"busy deferred session resumes despite trailing old image callbacks");

        reset(false,false);ready();capture.autoPixels=false;begin();tick();ExitBusy1921.busy=true;
        Handler.until(SystemClock.now+3500L);ExitBusy1921.busy=false;settle();
        ck(q.INSTANCE.stops==0&&capture.starts==1,"late busy release cannot initiate native stop without its full bounded wait margin");

        reset(false,false);ready();capture.autoPixels=false;capture.pixelsAfterRestart=true;
        q.INSTANCE.duringStop=new Runnable(){public void run(){SystemClock.now+=FrontPreview1936.WAIT_MS;}};
        begin();settle();
        ck(q.INSTANCE.stops==1&&capture.starts==2&&q.INSTANCE.mCurrentCameraState==3,
                "unexpected native stop overrun completes one safe same-owner resume instead of orphaning a stopped preview");
        ck(Handler.queued()==0,"native completion overrun never grants another recovery window");

        reset(false,false);ready();capture.autoPixels=false;capture.pixelsAfterRestart=true;
        q.INSTANCE.duringStop=new Runnable(){public void run(){Handler.until(SystemClock.now+FrontPreview1936.WAIT_MS);}};
        begin();settle();
        ck(q.INSTANCE.stops==1&&capture.starts==2&&q.INSTANCE.mCurrentCameraState==3,
                "main timeout firing inside native stop defers only its safe matching resume");
        ck(Handler.queued()==0,"deferred timeout leaves no extra observation after native completion");

        for(int earlierMissing=0;earlierMissing<2;earlierMissing++){
            reset(false,false);capture.autoPixels=false;capture.pixelsAfterRestart=true;
            if(earlierMissing==1){lost();ready();sourceEvent();}else{ready();begin();tick();}
            q.INSTANCE.duringStop=new Runnable(){public void run(){
                capture.during=new Runnable(){public void run(){q.INSTANCE.mOnBackGround=true;capture.during=null;}};
            }};
            Handler.until(SystemClock.now+2100L);
            ck(q.INSTANCE.stops==1&&q.INSTANCE.mCurrentCameraState==2,"transient native foreground at first resume leaves a retained pending resume "+earlierMissing);
            q.INSTANCE.mOnBackGround=false;
            FrontPreview1936.pixel(capture.latestProvider,new Object());
            settle();
            ck(q.INSTANCE.stops==1&&q.INSTANCE.mCurrentCameraState==3&&capture.accepted==2,
                    "old frame cannot cancel retryable matching resume after readiness returns "+earlierMissing);
        }

        reset(false,false);ready();capture.autoPixels=false;capture.pixelsAfterRestart=true;
        final RuntimeException oldFailure=new RuntimeException("old generation failed");
        capture.during=new Runnable(){public void run(){
            capture.during=null;FrontPreview1936.cancel(capture);begin();throw oldFailure;
        }};
        try{begin();throw new AssertionError("native exception lost");}catch(RuntimeException expected){ck(expected==oldFailure,"old native exception remains unchanged");}
        settle();
        ck(q.INSTANCE.stops==1&&capture.accepted==2,"old generation completion cannot cancel newly armed front recovery");
    }
    static void lateInputsAndDetachedRenewal() {
        for (long delayed : new long[]{6200L, 14800L}) {
            reset(false, false);lost();
            Handler.until(SystemClock.now + delayed);
            TECapturePipeline pipeline=ready();capture.autoPixels=false;capture.pixelsAfterRestart=true;
            sourceEvent();settle();
            ck(capture.accepted==2&&q.INSTANCE.stops==1&&capture.nativeRenewals==1,
                    "late native inputs receive a full first-frame recovery window " + delayed);
            ck(Handler.queued()==0,"late-input completion remains bounded " + delayed);
        }
        reset(false,false);TECapturePipeline p=ready();Object original=p.texture;
        capture.autoPixels=false;capture.pixelsAfterRenewal=true;begin();settle();
        ck(capture.starts==2&&capture.accepted==2&&p.texture!=original&&capture.nativeRenewals==1,
                "stale but unreleased detached texture is replaced once before matching resume");
        ck(p.replacementNotifications==1&&Handler.queued()==0,
                "native renderer receives replacement and first frame finishes detached recovery");

        reset(false,false);final TECapturePipeline released=ready();capture.autoPixels=false;capture.pixelsAfterRenewal=true;
        q.INSTANCE.duringStop=new Runnable(){public void run(){released.texture.release();}};
        begin();settle();
        ck(q.INSTANCE.stops==1&&capture.accepted==2&&capture.nativeRenewals==1,
                "texture released reentrantly during stop still receives its required native renewal and resume");

        reset(false,false);p=ready();p.textureId=7;capture.autoPixels=false;capture.pixelsAfterRestart=true;begin();settle();
        ck(q.INSTANCE.stops==1&&capture.accepted==2&&capture.nativeRenewals==0,
                "nonzero GL texture never recreated off its owning GL context");

        reset(false,false);p=ready();TECapturePipeline nonpreview=ready();nonpreview.preview=false;
        capture.autoPixels=false;capture.pixelsAfterRestart=true;begin();settle();
        ck(q.INSTANCE.stops==1&&capture.accepted==2&&capture.nativeRenewals==0,
                "mixed pipeline capture does not receive global native texture renewal");
    }
    public static void main(String[] args) {
        correctedRegressions();ordering();cancellation();busyAndHandlerGates();inputVariants();preparationAndPixels();
        asynchronousAcceptance();transientNativeLifecycle();retainedSessionRestart();lateInputsAndDetachedRenewal();
        System.out.println("PASS1936 front lifecycle assertions="+checks);
    }
}
