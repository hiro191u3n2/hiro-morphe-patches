package com.hiro.ulike;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.os.SystemClock;
import com.ss.android.vesdk.frame.TECapturePipeline;
import i.s.a.w.q;
import java.lang.ref.WeakReference;

/** Observe starts/teardown/output identities, never duplicate the ticket state machine. */
public final class FrontRetained1971Test {
    static int checks;
    static void ck(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static void reset(){FrontTest1936.reset(false,false);FrontTest1936.ready();CameraTrace1965.rows.clear();}
    static boolean trace(String needle){for(String row:CameraTrace1965.rows)if(row.contains(needle))return true;return false;}
    static void noFrame(boolean pending,boolean opened){
        FrontTest1936.capture.autoPixels=false;
        FrontTest1936.capture.pendingConfiguration=pending;
        FrontTest1936.capture.keepOpenedAfterStart=opened;
    }
    static void retainedOpenedSession() {
        reset();noFrame(false,true);FrontTest1936.capture.pixelsAfterCleanup=true;
        CameraDevice originalDevice=FrontTest1936.camera2.K;
        Object originalMode=FrontTest1936.camera2.M,originalSettings=FrontTest1936.capture.c;
        SurfaceTexture originalTexture=FrontTest1936.capture.n.list.get(0).texture;
        ck(FrontTest1936.begin()==0,"caller retains accepted return");
        Handler.until(SystemClock.now+1900L);
        ck(FrontTest1936.camera2.sessionCleanups==0&&FrontTest1936.capture.starts==1,"frame grace avoids premature teardown");
        FrontTest1936.settle();
        ck(FrontTest1936.camera2.sessionCleanups==1&&q.INSTANCE.stops==0,"state2 retained session uses exactly one native mode teardown");
        ck(FrontTest1936.capture.starts==2&&FrontTest1936.capture.accepted==2,"same pending request starts exactly once after cleanup");
        ck(FrontTest1936.capture.newSurfaceRequests==1&&FrontTest1936.capture.nativeRenewals==1,"cleared retained session renews detached native texture once");
        ck(originalTexture.isReleased()&&FrontTest1936.capture.n.list.get(0).texture!=originalTexture,"native renderer receives replacement input");
        ck(FrontTest1936.camera2.K==originalDevice&&FrontTest1936.camera2.M==originalMode&&FrontTest1936.capture.c==originalSettings,"device mode settings retained");
        ck(FrontTest1936.capture.c.l==1&&Handler.queued()==0,"front selection retained and matching frame ends all work");
        ck(trace("front_preview_session_cleanup ")&&trace("front_preview_session_cleanup_result ")&&trace("front_preview_replay_result "),"cleanup and replay outcomes reach trace ZIP sink");
        ck(trace("front_preview_frame "),"actual provider notice confirms completion");
    }
    static void pendingSessionSafety() {
        for(boolean opened:new boolean[]{true,false}) {
            reset();noFrame(true,opened);
            SurfaceTexture texture=FrontTest1936.capture.n.list.get(0).texture;
            FrontTest1936.begin();Handler.until(SystemClock.now+3000L);
            ck(FrontTest1936.capture.starts==1&&FrontTest1936.capture.newSurfaceRequests==0&&FrontTest1936.camera2.sessionCleanups==0,"accepted pending configuration retains original output in native state "+(opened?2:3));
            ck(texture==FrontTest1936.capture.n.list.get(0).texture&&!texture.isReleased(),"pending onConfigured owns unchanged texture");
            ck(trace("reason=camera2_session_pending"),"missing session wait reason is exported");
            FrontTest1936.mode.d=new CameraCaptureSession();
            FrontPreview1936.pixel(FrontTest1936.capture.latestProvider,new Object());
            FrontTest1936.settle();
            ck(FrontTest1936.capture.starts==1&&FrontTest1936.capture.newSurfaceRequests==0&&Handler.queued()==0,"late original session callback completes without replay or release");
        }
        reset();noFrame(true,true);FrontTest1936.begin();FrontTest1936.settle();
        ck(FrontTest1936.capture.starts==1&&FrontTest1936.capture.newSurfaceRequests==0&&FrontTest1936.camera2.sessionCleanups==0&&Handler.queued()==0,"never configured session times out without unsafe renewal");
        ck(trace("front_preview_drop ")&&trace("reason=deadline"),"no-session deadline is visible");
        FrontTest1936.mode.d=new CameraCaptureSession();FrontPreview1936.pipelines(FrontTest1936.capture);FrontTest1936.settle();
        ck(FrontTest1936.capture.starts==1,"late pipeline notifications never revive expired recovery");
    }
    static void failedCleanup() {
        for(int fault=0;fault<2;fault++) {
            reset();noFrame(false,true);
            if(fault==0)FrontTest1936.camera2.cleanupResult=-1;
            else FrontTest1936.camera2.cleanupFailure=new IllegalStateException("private_sdk_exception_text");
            FrontTest1936.begin();FrontTest1936.settle();
            ck(FrontTest1936.camera2.sessionCleanups==1&&FrontTest1936.capture.starts==1&&FrontTest1936.capture.newSurfaceRequests==0,"failed cleanup preserves device/output without second start "+fault);
            ck(Handler.queued()==0,"failed cleanup removes all optional work "+fault);
            ck(trace(fault==0?"reason=retained_session_cleanup_failed":"reason=replay_exception_InvocationTargetException"),"failure class or return is exported "+fault);
            ck(!trace("private_sdk_exception_text"),"diagnostics never export exception messages");
        }
    }
    static void cleanupCallbackLifetime() {
        for(int stale=0;stale<8;stale++) {
            reset();noFrame(false,true);final int fault=stale;
            FrontTest1936.camera2.duringCleanup=new Runnable(){public void run(){
                switch(fault) {
                    case 0:ManualLens170.f.put("foreground",false);break;
                    case 1:ManualLens170.f.put("epoch",8L);break;
                    case 2:q.INSTANCE.mCameraClient=new i.s.a.w.k();break;
                    case 3:q.INSTANCE.mCameraSettings=new com.ss.android.ttvecamera.TECameraSettings();break;
                    case 4:FrontTest1936.camera2.K=new CameraDevice();break;
                    case 5:q.INSTANCE.mHandler=new Handler();break;
                    case 6:FrontTest1936.camera2.M=new i.s.a.w.h0.b();break;
                    default:ManualLens170.f.put("capture",new WeakReference<Object>(new com.ss.android.vesdk.VECameraCapture()));
                }
            }};
            FrontTest1936.begin();FrontTest1936.settle();
            ck(FrontTest1936.camera2.sessionCleanups==1&&FrontTest1936.capture.starts==1&&FrontTest1936.capture.newSurfaceRequests==0,"stop callback cannot restart foreign lifetime "+stale);
            ck(Handler.queued()==0&&trace("front_preview_drop "),"foreign cleanup lifetime is bounded and logged "+stale);
        }
        reset();noFrame(false,true);FrontTest1936.capture.pixelsAfterCleanup=true;
        FrontTest1936.camera2.duringCleanup=new Runnable(){public void run(){FrontPreview1936.pixel(FrontTest1936.capture.latestProvider,new Object());}};
        FrontTest1936.begin();FrontTest1936.settle();
        ck(FrontTest1936.capture.starts==2&&FrontTest1936.capture.nativeRenewals==1,"final old frame during teardown cannot cancel required matching restart");
    }
    static void busyAndBounds() {
        for(int gate=0;gate<3;gate++) {
            reset();noFrame(false,true);FrontTest1936.begin();FrontTest1936.tick();
            if(gate==0)ManualLens170.f.put("recording",true);
            else if(gate==1)ExitBusy1921.busy=true;
            else RearRestart1926.ready=false;
            Handler.until(SystemClock.now+2500L);
            ck(FrontTest1936.camera2.sessionCleanups==0&&FrontTest1936.capture.newSurfaceRequests==0,"busy or recovery window protects current session "+gate);
            ManualLens170.f.put("recording",false);ExitBusy1921.busy=false;RearRestart1926.ready=true;
            FrontTest1936.capture.pixelsAfterCleanup=true;FrontTest1936.settle();
            ck(FrontTest1936.camera2.sessionCleanups==1&&FrontTest1936.capture.starts==2&&Handler.queued()==0,"owned request can resume when safe window returns "+gate);
        }
        reset();noFrame(false,true);FrontTest1936.begin();FrontTest1936.settle();
        ck(FrontTest1936.camera2.sessionCleanups==1&&FrontTest1936.capture.starts==2&&Handler.queued()==0,"failure after cleanup does not cause endless native restarts");
        ck(trace("reason=recovery_already_used")&&(trace("reason=deadline")||trace("reason=insufficient_restart_window")),"used recovery and remaining timeout recorded");
        FrontTest1936.reset(false,true);FrontTest1936.ready();FrontTest1936.capture.autoPixels=false;FrontTest1936.capture.pixelsAfterRenewal=true;
        FrontTest1936.begin();FrontTest1936.settle();
        ck(q.INSTANCE.stops==1&&FrontTest1936.capture.starts==2,"Camera1 retains original native stop/replay path");
    }
    static void nativeHandshake() {
        for(int state:new int[]{0,1,4}) {
            reset();FrontTest1936.camera2.I=state;
            ck(!FrontPreview1936.before(FrontTest1936.capture),"invalid wrapper state cannot enter native void start "+state);
            ck(FrontTest1936.begin()==-100,"invalid wrapper state preserves native not-ready result "+state);
            FrontTest1936.tick();ck(FrontTest1936.capture.accepted==0,"server opened alone cannot fabricate wrapper readiness");
            FrontTest1936.camera2.I=2;FrontTest1936.settle();
            ck(FrontTest1936.capture.accepted==1&&FrontTest1936.camera2.sessionCleanups==0,"owned wrapper readiness arrives within original deadline");
        }
        reset();FrontTest1936.mode.a.facing=1;
        ck(FrontTest1936.begin()==-100,"previous rear characteristics with shared settings cannot start front");
        FrontTest1936.tick();ck(FrontTest1936.capture.accepted==0,"mismatched physical facing cannot be replayed");
        FrontTest1936.mode.a.facing=0;FrontTest1936.settle();
        ck(FrontTest1936.capture.accepted==1,"matching actual front characteristics complete original request");
    }
    static void scalarDiagnostics() {
        reset();FrontTest1936.mode.d=new CameraCaptureSession();
        CameraSession1965.phase("test_snapshot",FrontTest1936.capture);
        ck(trace("native_mode_ids ")&&trace("session="+System.identityHashCode(FrontTest1936.mode.d)),"snapshot exports native mode session identity");
        ck(trace("native_camera ")&&trace("state=2"),"snapshot exports wrapper state separately from server");
        ck(trace("native_provider ")&&trace("native_mode_owner "),"provider and owner observations preserved");
        int first=CameraTrace1965.rows.size();
        CameraSession1965.cameraError(FrontTest1936.camera2,-439,"getSupportedPreviewSizes: camera is null. SECRET_USER_FRAME");
        CameraSession1965.cameraError(FrontTest1936.camera2,-425,"_startCapture error occurred SECRET_USER_FRAME");
        ck(trace("camera_error_code ")&&trace("code=-439")&&trace("code=-425"),"every distinct native error survives anomaly suppression");
        ck(trace("sdk_reason=device_not_ready")&&trace("sdk_reason=start_exception"),"recognized SDK text is converted to fixed reason tokens");
        ck(!trace("SECRET_USER_FRAME"),"raw SDK callback contents never enter ZIP");
        for(String row:CameraTrace1965.rows) {
            // phase and epoch add at most forty chars here; measure the actual
            // detail field independently against production Message's160 bound.
            int separator=row.indexOf(' ',row.indexOf(' ')+1);
            ck(separator<0||row.length()-separator-1<=160,"all scalar detail fields fit production160char trace bound");
        }
        reset();noFrame(false,true);ExitBusy1921.busy=true;FrontTest1936.begin();FrontTest1936.settle();
        int waits=0;for(String row:CameraTrace1965.rows)if(row.contains("front_preview_wait "))waits++;
        ck(waits<=2,"repeated busy polling records transitions once instead of flooding persisted trace");
    }
    public static void main(String[] args){retainedOpenedSession();pendingSessionSafety();failedCleanup();cleanupCallbackLifetime();busyAndBounds();nativeHandshake();scalarDiagnostics();System.out.println("PASS front retained1971 assertions="+checks);}
}
