package com.hiro.ulike;
import android.graphics.Bitmap;import android.os.Handler;import java.lang.ref.WeakReference;import java.util.*;import java.util.concurrent.*;
import com.bytedance.corecamera.camera.basic.PureCameraFragment;
/** Uses the production queue, save adapter, worker pool, preview helper and
 * reflection cache. Only SDK/Android/encoder boundaries are fixtures. */
public class AsyncPreview19121Test {
    static int assertions;
    static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    static void until(java.util.function.BooleanSupplier test,String why)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!test.getAsBoolean()&&System.nanoTime()<end)Thread.yield();check(test.getAsBoolean(),why);}
    public static void main(String[] args)throws Exception {
        SaveQuality2.systemInfo=true;PhotoDetail.live=new PhotoDetail.Settings(false,1,false,1,true,true,true);Bitmap.failCopy=true;
        final Bitmap source=new Bitmap(new int[]{71,72,73,74});int[] pristine=source.pixels.clone();
        for(String mode:new String[]{"success","sdk-throw","post-fail","epoch-change","background","source-replaced","finish-before-ui","reused-source"}){
            OpticalZoom.foreground=true;OpticalZoom.recording=false;OpticalZoom.switching=false;OpticalZoom.epoch++;
            OpticalZoom.active=new Object();OpticalZoom.capture=new WeakReference<Object>(new Object());
            CaptureYuv.Owner owner=new CaptureYuv.Owner();CaptureYuv.State state=new CaptureYuv.State();state.original=owner.e0;
            CaptureYuv.active=new WeakReference<Object>(owner);CaptureYuv.states.clear();CaptureYuv.states.put(owner,state);
            PureCameraFragment fragment=new PureCameraFragment();fragment.camera.recorder.capture=OpticalZoom.capture.get();fragment.camera.supplier.b=fragment.camera.recorder;fragment.camera.supplier.h.a=fragment.camera.recorder;
            PreviewFixtures19121.Controller controller=new PreviewFixtures19121.Controller();controller.a=fragment;
            i.o.a.b1.a.b.c bridge=new i.o.a.b1.a.b.c();bridge.c=controller;i.o.a.b1.a.b.c.a callback=new i.o.a.b1.a.b.c.a();callback.a=bridge;
            i.o.a.b1.a.b.f.a manager=new i.o.a.b1.a.b.f.a(callback);
            i.f.l.n.s.a.b().image=source;i.f.l.n.s.a.b().b=source;ShotContext1932.bind(source,19121);
            ProcessingTiming1947.Trace trace=ProcessingTiming1947.forKey(source);
            CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
            HostRouter1981.encoder=(save,input,r,d)->{check(input==source,"actual serial save borrows exact source");check(!input.isRecycled()&&Arrays.equals(input.pixels,pristine),"source exact before encode");entered.countDown();release.await();AsyncSave1935.encoding(input);AsyncSave1935.hardwareClosed1956();return "saved-"+mode;};
            Handler.rejectPreview=mode.equals("post-fail");fragment.camera.supplier.fail=mode.equals("sdk-throw");
            AsyncSave1935.submitAuto(manager,0,0);
            check(entered.await(5,TimeUnit.SECONDS),"actual save entered "+mode);
            until(()->Handler.previewCount()>0||trace.previewResult!=null,"preview request was processed "+mode);
            check(AsyncSave1935.captureBlocked()&&SaveQueue1935.count()==1&&ExitJobs185.count()==1,"serial source retains shutter and exactly one reservation "+mode);
            check(callback.finished==0,"SDK capture finish deferred until terminal save "+mode);
            if(mode.equals("epoch-change"))OpticalZoom.epoch++;
            if(mode.equals("background"))OpticalZoom.foreground=false;
            if(mode.equals("source-replaced"))i.f.l.n.s.a.b().b=new Bitmap(new int[]{1,2,3,4});
            if(!mode.equals("finish-before-ui"))Handler.drainPreview();
            check(callback.finished==0&&AsyncSave1935.captureBlocked(),"preview cannot release capture "+mode);
            check(!source.isRecycled()&&Arrays.equals(pristine,source.pixels),"preview cannot mutate borrowed image "+mode);
            release.countDown();until(()->callback.receipts==1&&callback.finished==1&&SaveQueue1935.count()==0&&ExitJobs185.count()==0,"normal terminal receipt and capture finish exactly once "+mode);
            Handler.drainPreview();check(callback.receipts==1&&callback.finished==1&&callback.saved,"preview failure/delay cannot fail or duplicate save "+mode);
            int expected=(mode.equals("success")||mode.equals("sdk-throw")||mode.equals("reused-source"))?1:0;
            check(fragment.camera.supplier.calls==expected,"expected display-only invocation "+mode+" result="+trace.previewResult);
            check(!AsyncSave1935.captureBlocked()&&!source.isRecycled()&&source.recycleCalls==0&&Arrays.equals(pristine,source.pixels),"terminal keeps SDK bitmap pristine and reopens original admission "+mode);
        }
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_async_adapter\":true,\"serial_source_ownership_preserved\":true,\"one_terminal_callback\":true}");
    }
}
