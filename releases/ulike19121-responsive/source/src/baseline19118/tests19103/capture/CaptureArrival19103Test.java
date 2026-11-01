package com.hiro.ulike;

import android.graphics.Bitmap;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;

/** Real capture owner and diagnostic classes; camera and renderer APIs alone are fixtures. */
public final class CaptureArrival19103Test {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static ProcessingTiming1947.Trace start(Object callback){CaptureBinding1991Test.start(callback);return ProcessingTiming1947.exactForKey1999(callback);}
    static String render(ProcessingTiming1947.Trace trace)throws Exception{return Timing1982Test.render(trace);}
    static void exactAndClocks()throws Exception {
        Object callback=new Object();ProcessingTiming1947.Trace trace=start(callback);
        CaptureBinding1991Test.Owner owner=new CaptureBinding1991Test.Owner(callback);
        Object token=ShotContext1932.cameraOwner19103(owner);
        // Intentionally unrelated to the app clock; sensor timestamps only serve as identity keys.
        long sensor=Long.MAX_VALUE-12;Image image=new Image(sensor,4080,3060);
        TotalCaptureResult result=CaptureBinding1991Test.result(sensor);
        long start=System.nanoTime(),acquired=System.nanoTime();
        ShotContext1932.acquired19103(owner,token,image,start,acquired);
        long observed=System.nanoTime();ShotContext1932.resultObserved19103(owner,token,result,observed);
        check(trace.cameraHandoff19103[0]==0&&trace.cameraHandoff19103[2]==0,"early observations are not adopted before Q0 exact matching");
        ShotContext1932.received(owner,image,result);
        check(trace.cameraHandoff19103[0]==start&&trace.cameraHandoff19103[1]==acquired,"same exact image adopts actual acquisition boundaries");
        check(trace.cameraHandoff19103[2]==observed,"same exact result adopts observation boundary");
        String text=render(trace);
        check(text.contains("Image取得API開始→取得完了")&&text.contains("Image取得完了→撮影結果通知"),"arrival order and acquisition split shown");
        check(text.contains("センサー時刻は入力照合だけに使用")&&text.contains("Camera2撮影開始通知: 未観測"),"unknown sensor start and distinct clock domains explicit");
        ShotContext1932.acquired19103(owner,token,image,System.nanoTime(),System.nanoTime());
        ShotContext1932.received(owner,image,result);
        check(trace.cameraHandoff19103[0]==start&&trace.cameraHandoff19103[1]==acquired,"duplicate or replay cannot rewrite first real acquire");
        ShotContext1932.bindDelivery(callback,new Object(),new Bitmap(3060,4080));
        ProcessingTiming1947.finish(trace,true);String terminal=render(trace);
        ShotContext1932.resultObserved19103(owner,token,result,System.nanoTime());
        check(terminal.equals(render(trace)),"late notification cannot mutate terminal diagnostic");
    }
    static void generationAndTimestamp()throws Exception {
        Object callback=new Object();ProcessingTiming1947.Trace old=start(callback);
        CaptureBinding1991Test.Owner owner=new CaptureBinding1991Test.Owner(callback);
        Object oldToken=ShotContext1932.cameraOwner19103(owner);
        ProcessingTiming1947.Trace fresh=start(callback);Image image=new Image(2001,4080,3060);
        TotalCaptureResult result=CaptureBinding1991Test.result(2001);
        ShotContext1932.acquired19103(owner,oldToken,image,System.nanoTime(),System.nanoTime());
        ShotContext1932.resultObserved19103(owner,oldToken,result,System.nanoTime());
        ShotContext1932.received(owner,image,result);
        check(fresh.cameraHandoff19103[0]==0&&fresh.cameraHandoff19103[2]==0,"callback reuse cannot transfer old shot token into new photo");
        check(old.cameraHandoff19103[0]==0&&old.cameraHandoff19103[2]==0,"old photo not rewritten by new generation");
        check(render(fresh).contains("Image取得API開始→取得完了: 未観測"),"missing hook stays unobserved");
        ProcessingTiming1947.finish(old,true);ProcessingTiming1947.finish(fresh,true);

        callback=new Object();ProcessingTiming1947.Trace mismatch=start(callback);owner=new CaptureBinding1991Test.Owner(callback);
        Object token=ShotContext1932.cameraOwner19103(owner);
        Image wrong=new Image(3000,4080,3060);TotalCaptureResult wrongResult=CaptureBinding1991Test.result(3000);
        ShotContext1932.acquired19103(owner,token,wrong,System.nanoTime(),System.nanoTime());
        ShotContext1932.resultObserved19103(owner,token,wrongResult,System.nanoTime());
        ShotContext1932.received(owner,new Image(3001,4080,3060),CaptureBinding1991Test.result(3001));
        check(mismatch.cameraHandoff19103[0]==0&&mismatch.cameraHandoff19103[2]==0,"different timestamp never borrows latest image or result");
        ProcessingTiming1947.finish(mismatch,true);
    }
    static void identitiesAndRejection()throws Exception {
        Object callback=new Object();ProcessingTiming1947.Trace trace=start(callback);
        CaptureBinding1991Test.Owner owner=new CaptureBinding1991Test.Owner(callback);Object token=ShotContext1932.cameraOwner19103(owner);
        Image image=new Image(4001,4080,3060),equalTimestamp=new Image(4001,4080,3060);
        TotalCaptureResult result=CaptureBinding1991Test.result(4001),otherResult=CaptureBinding1991Test.result(4001);
        ShotContext1932.acquired19103(owner,token,equalTimestamp,System.nanoTime(),System.nanoTime());
        ShotContext1932.resultObserved19103(owner,token,otherResult,System.nanoTime());
        ShotContext1932.received(owner,image,result);
        check(trace.cameraHandoff19103[0]==0&&trace.cameraHandoff19103[2]==0,"equal timestamp alone never substitutes different image/result identities");
        ProcessingTiming1947.finish(trace,true);

        callback=new Object();trace=start(callback);owner=new CaptureBinding1991Test.Owner(callback);token=ShotContext1932.cameraOwner19103(owner);
        image=new Image(5001,4080,3060);result=CaptureBinding1991Test.result(5001);
        long observed=System.nanoTime();ShotContext1932.resultObserved19103(owner,token,result,observed);
        ShotContext1932.acquired19103(owner,token,image,System.nanoTime(),System.nanoTime());
        ShotContext1932.received(owner,image,result);
        check(trace.cameraHandoff19103[2]==observed&&render(trace).contains("撮影結果通知→Image取得完了"),"result-before-image is supported without ordering assumption");
        ShotContext1932.received(owner,new Image(5002,4080,3060),CaptureBinding1991Test.result(5002));
        check(trace.metadataRejected1999&&!trace.cameraHandoffObserved19103&&trace.cameraHandoff19103[0]==0&&trace.cameraHandoff19103[2]==0,"ambiguous input invalidates all metadata-derived arrival evidence");
        ProcessingTiming1947.finish(trace,true);
    }
    static void invalidClockAndOwner()throws Exception {
        Object callback=new Object();ProcessingTiming1947.Trace trace=start(callback);
        CaptureBinding1991Test.Owner owner=new CaptureBinding1991Test.Owner(callback);Object token=ShotContext1932.cameraOwner19103(owner);
        Image image=new Image(6001,4080,3060);TotalCaptureResult result=CaptureBinding1991Test.result(6001);
        ShotContext1932.acquired19103(owner,token,image,1,2);
        ShotContext1932.resultObserved19103(owner,token,result,Long.MAX_VALUE);
        ShotContext1932.received(owner,image,result);
        check(trace.cameraHandoff19103[0]==0&&trace.cameraHandoff19103[2]==0,"pre-shot and future app timestamps rejected");
        check(ShotContext1932.cameraOwner19103(new Object())==null,"unrelated owner has no latest-shot fallback");
        ProcessingTiming1947.finish(trace,true);
    }
    static void physicalAndOwnerIsolation()throws Exception {
        Object callback=new Object();ProcessingTiming1947.Trace trace=start(callback);
        CaptureBinding1991Test.Owner owner=new CaptureBinding1991Test.Owner(callback);
        OpticalZoom.register(owner.j,new OpticalZoom.Route("tele",owner.a,5));
        Object token=ShotContext1932.cameraOwner19103(owner);
        Image image=new Image(7001,4080,3060);TotalCaptureResult logical=CaptureBinding1991Test.result(7999);
        TotalCaptureResult physical=CaptureBinding1991Test.result(7001);logical.physical.put("tele",physical);
        long observed=System.nanoTime();ShotContext1932.resultObserved19103(owner,token,logical,observed);
        ShotContext1932.acquired19103(owner,token,image,System.nanoTime(),System.nanoTime());
        ShotContext1932.received(owner,image,logical);
        check(trace.cameraHandoff19103[2]==observed&&trace.cameraHandoff19103[0]>0,"physical selected result uses exact physical object and its matching sensor timestamp");
        ProcessingTiming1947.finish(trace,true);

        callback=new Object();trace=start(callback);owner=new CaptureBinding1991Test.Owner(callback);
        CaptureBinding1991Test.Owner otherOwner=new CaptureBinding1991Test.Owner(callback);
        token=ShotContext1932.cameraOwner19103(owner);image=new Image(8001,4080,3060);
        TotalCaptureResult result=CaptureBinding1991Test.result(8001);
        ShotContext1932.acquired19103(owner,token,image,System.nanoTime(),System.nanoTime());
        ShotContext1932.resultObserved19103(owner,token,result,System.nanoTime());
        ShotContext1932.received(otherOwner,image,result);
        check(trace.cameraHandoff19103[0]==0&&trace.cameraHandoff19103[2]==0,"a different camera owner cannot borrow arrival evidence even if callback and timestamp match");
        ProcessingTiming1947.finish(trace,true);
    }
    public static void main(String[] args)throws Exception {
        exactAndClocks();generationAndTimestamp();identitiesAndRejection();invalidClockAndOwner();physicalAndOwnerIsolation();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"exact_capture_arrival_verified\":true,\"sensor_clock_subtraction_absent\":true,\"generation_reuse_rejected\":true}");
    }
}
