package com.hiro.ulike;
import android.graphics.Bitmap;import android.os.*;import java.lang.ref.WeakReference;
import com.bytedance.corecamera.camera.basic.PureCameraFragment;
public class CapturePreview19121Test {
    static int assertions,cases;
    static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    static class Setup {
        final PreviewFixtures19121.Manager manager=new PreviewFixtures19121.Manager();
        final i.o.a.b1.a.b.c bridge=new i.o.a.b1.a.b.c();final i.o.a.b1.a.b.c.a callback=new i.o.a.b1.a.b.c.a();
        final PreviewFixtures19121.Controller controller=new PreviewFixtures19121.Controller();
        final PureCameraFragment fragment=new PureCameraFragment();final Bitmap input=new Bitmap();
        final CaptureYuv.Owner owner=new CaptureYuv.Owner();final CaptureYuv.State state=new CaptureYuv.State();
        final ProcessingTiming1947.Trace trace=new ProcessingTiming1947.Trace();Object token;long inputReady;
        Setup(){Responsiveness19122.reset();Looper.ui=false;Handler.tasks.clear();Handler.accepted=true;OpticalZoom.foreground=true;OpticalZoom.recording=false;OpticalZoom.switching=false;OpticalZoom.epoch=9;
            OpticalZoom.active=new Object();OpticalZoom.capture=new WeakReference<Object>(new Object());
            CaptureYuv.active=new WeakReference<Object>(owner);CaptureYuv.states.clear();state.original=owner.e0;CaptureYuv.states.put(owner,state);
            manager.b=callback;callback.a=bridge;bridge.c=controller;controller.a=fragment;
            fragment.camera.recorder.capture=OpticalZoom.capture.get();fragment.camera.supplier.b=fragment.camera.recorder;fragment.camera.supplier.h.a=fragment.camera.recorder;
            i.f.l.n.s.a.b().b=input;AsyncSave1935.serial=7;AsyncSave1935.captureOpen=false;AsyncSave1935.held=1;
            token=CapturePreview19121.snapshot19121(manager,7);check(token!=null,"snapshot created without UI reads");
        }
        void post(){inputReady=System.nanoTime()-1000;CapturePreview19121.ready19121(token,input,trace,inputReady,true);check(fragment.camera.supplier.calls==0,"always posted before display mutation");check(Responsiveness19122.requests==0,"observation waits for the posted API call");}
        void done(int expected){Handler.drain();check(fragment.camera.supplier.calls==expected,"display calls expected="+expected+" actual="+fragment.camera.supplier.calls+" result="+trace.result);
            check(callback.finished==0,"native capture-finish remains pending");check(!AsyncSave1935.captureOpen,"next shutter stays closed");
            check(input.pixels[0]==11&&input.pixels[3]==44,"source pixels unchanged");check(trace.reports==1&&trace.elapsed>=0,"bounded per-photo diagnostic");
            int observed=expected==1&&!fragment.camera.supplier.fail?1:0;
            check(Responsiveness19122.requests==observed,"only successful release API completion reaches observation");
            if(observed==1)check(Responsiveness19122.trace==trace&&Responsiveness19122.inputReady==inputReady&&Responsiveness19122.returned>=inputReady,"observer binds exact trace and ordered input/API times");cases++;}
    }
    interface Change {void apply(Setup s);}
    static void reject(String label,Change change){Setup s=new Setup();s.post();change.apply(s);int originalHeld=AsyncSave1935.held;boolean originalOpen=AsyncSave1935.captureOpen;Handler.drain();check(s.fragment.camera.supplier.calls==0,label+" blocks display call: "+s.trace.result);check(s.callback.finished==0,label+" leaves SDK completion pending");check(AsyncSave1935.held==originalHeld&&AsyncSave1935.captureOpen==originalOpen,label+" preserves queue ownership");check(s.trace.reports==1&&s.trace.result.startsWith("見送り"),label+" records skip");check(Responsiveness19122.requests==0,label+" cannot claim a release request completed");cases++;}
    public static void main(String[] args)throws Exception {
        Setup success=new Setup();success.post();success.done(1);
        reject("stale generation",s->OpticalZoom.epoch++);
        reject("camera changed",s->OpticalZoom.capture=new WeakReference<Object>(new Object()));
        reject("route changed",s->OpticalZoom.active=new Object());
        reject("background",s->OpticalZoom.foreground=false);
        reject("recording",s->OpticalZoom.recording=true);
        reject("lens switching",s->OpticalZoom.switching=true);
        reject("input owner changed",s->CaptureYuv.active=new WeakReference<Object>(new Object()));
        reject("input state replaced",s->CaptureYuv.states.put(s.owner,new CaptureYuv.State()));
        reject("input closed",s->s.state.closed=true);
        reject("input reconfiguring",s->s.state.ready=false);
        reject("input reader replaced",s->s.state.yuv=new Object());
        reject("input original changed",s->s.state.original=new Object());
        reject("camera reader changed",s->s.owner.e0=new Object());
        reject("callback changed",s->s.manager.b=new Object());
        reject("controller changed",s->s.bridge.c=new PreviewFixtures19121.Controller());
        reject("fragment changed",s->s.controller.a=new PureCameraFragment());
        reject("activity finishing",s->s.fragment.activity.finishing=true);
        reject("activity destroyed",s->s.fragment.activity.destroyed=true);
        reject("activity focus lost",s->s.fragment.activity.focus=false);
        reject("fragment detached",s->s.fragment.added=false);
        reject("fragment paused",s->s.fragment.resumed=false);
        reject("fragment hidden",s->s.fragment.visible=false);
        reject("auto save changed",s->s.fragment.state.state.auto.value=false);
        reject("non HD already unfreezes in native",s->s.fragment.state.state.hd.value=false);
        reject("recorder camera changed",s->s.fragment.camera.recorder.capture=new Object());
        reject("supplier recorder changed",s->s.fragment.camera.supplier.b=new Object());
        reject("render bridge changed",s->s.fragment.camera.supplier.h.a=new Object());
        reject("source overwritten",s->i.f.l.n.s.a.b().b=new Bitmap());
        reject("source recycled",s->s.input.recycled=true);
        reject("save generation changed",s->AsyncSave1935.serial++);
        reject("capture opened by normal finish",s->AsyncSave1935.captureOpen=true);
        reject("save ended",s->AsyncSave1935.held=0);
        reject("epoch changes during lookup",s->s.fragment.camera.beforeRead=()->OpticalZoom.epoch++);
        Setup fail=new Setup();fail.fragment.camera.supplier.fail=true;fail.post();fail.done(1);check(fail.trace.result.contains("失敗"),"SDK error recorded without callback release");
        Setup observerFail=new Setup();Responsiveness19122.fail=true;observerFail.post();observerFail.done(1);check(observerFail.trace.result.startsWith("HD描画停止"),"optional observation failure cannot replace a successful release result");
        Setup noPost=new Setup();Handler.accepted=false;noPost.post();noPost.done(0);check(noPost.trace.result.contains("受付失敗"),"rejected post recorded");
        Setup own=new Setup();CapturePreview19121.ready19121(own.token,own.input,own.trace,System.nanoTime(),false);check(Handler.tasks.isEmpty(),"owned save uses original release policy");own.done(0);
        Setup missing=new Setup();CapturePreview19121.ready19121(null,missing.input,missing.trace,System.nanoTime(),true);check(Handler.tasks.isEmpty(),"missing snapshot does not queue work");missing.done(0);
        Setup bad=new Setup();OpticalZoom.foreground=false;bad.token=CapturePreview19121.snapshot19121(bad.manager,7);OpticalZoom.foreground=true;bad.post();bad.done(0);check(bad.trace.result.contains("背景"),"unverified snapshot never promoted after foreground change");
        Setup off=new Setup();check(CapturePreview19121.restore((CapturePreview19121.Token)off.token,off.input).contains("UIスレッド外"),"direct worker-side restoration rejected");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+"}");
    }
}
