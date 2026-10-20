package com.hiro.ulike;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Actual save lifecycle helpers; Android and stock ABI boundaries are scripted. */
public class SaveAudit1963 {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void missingSource(){i.f.l.n.s.a.b().image=null;}
    static void runPreparationFailure()throws Exception {
        i.o.a.b1.a.b.f.a manager=SaveHarness1935.manager();
        SaveHarness1935.submit(manager,1001);
        SaveHarness1935.Photo first=SaveHarness1935.next(SaveHarness1935.preparing,"first preparation");
        first.corrected.countDown();SaveHarness1935.next(SaveHarness1935.encoding,"prior encoder held");
        SaveHarness1935.await(()->!AsyncSave1935.captureBlocked(),"first shutter handed off");
        int before=SaveQuality2.failures;
        missingSource();AsyncSave1935.submitAuto(manager,0,0);
        SaveHarness1935.await(()->SaveQuality2.failures==before+1,"failed preparation callback completed");
        check(SaveQueue1935.count()==1&&first.encoded.getCount()==1,"prior save remains live while failed preparation exits");
        check(!AsyncSave1935.captureBlocked(),"failed latest preparation reopens capture while prior save remains active");
        SaveHarness1935.submit(manager,1003);
        SaveHarness1935.Photo following=SaveHarness1935.next(SaveHarness1935.preparing,"capture resumes after missing-source failure");
        SaveHarness1935.complete(first);SaveHarness1935.complete(following);SaveHarness1935.drained();
    }
    static void runStaleFailure()throws Exception {
        i.o.a.b1.a.b.f.a manager=SaveHarness1935.manager();
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(()->{entered.countDown();SaveHarness1935.waitLatch(release,"main-thread blocker");});
        check(entered.await(5,TimeUnit.SECONDS),"main callback queue blocked deterministically");
        int before=SaveQuality2.failures,finishes=SaveHarness1935.finishes;
        missingSource();AsyncSave1935.submitAuto(manager,0,0);
        SaveHarness1935.await(()->SaveQueue1935.count()==0,"failed capture leaves queue while main callback stays pending");
        check(ExitJobs185.count()==1,"failed preparation retains exit guard until main callback");
        Bitmap.failCopy=true;
        Bitmap source=new Bitmap(new int[]{2002,2003,2004,2005});ShotContext1932.bind(source,2002);i.f.l.n.s.a.b().image=source;
        AsyncSave1935.submitAuto(manager,0,0);
        SaveHarness1935.Photo newer=SaveHarness1935.next(SaveHarness1935.preparing,"newer borrowed-source preparation");
        check(AsyncSave1935.captureBlocked(),"newer capture owns serial borrowed source");
        release.countDown();SaveHarness1935.await(()->SaveQuality2.failures==before+1,"old failure callback runs");
        check(SaveHarness1935.finishes==finishes,"old failure never resets newer native shutter");
        check(AsyncSave1935.captureBlocked()&&ExitJobs185.count()==1,"newer admission and exit guard remain owned");
        SaveHarness1935.complete(newer);SaveHarness1935.drained();Bitmap.failCopy=false;
        check(SaveHarness1935.finishes==finishes+1&&!source.isRecycled(),"newer shutter completes exactly once without recycling borrowed source");
    }
    static void runWorkerError()throws Exception {
        for(int n=0;n<2;n++){
            CountDownLatch failed=new CountDownLatch(1);SaveQueue1935.reserve();
            SaveQueue1935.submit(()->{try{throw new AssertionError("scripted helper Error");}finally{SaveQueue1935.release();failed.countDown();}},4,16);
            check(failed.await(5,TimeUnit.SECONDS),"error task ran and returned its reservation");
            SaveHarness1935.await(()->SaveQueue1935.idle1953(),"error stage ownership released");
        }
        CountDownLatch next=new CountDownLatch(1);SaveQueue1935.reserve();
        SaveQueue1935.submit(()->{try{next.countDown();}finally{SaveQueue1935.release();}},4,16);
        check(next.await(5,TimeUnit.SECONDS),"fixed worker pool survives two Error subclasses and runs next save");
        SaveHarness1935.await(()->SaveQueue1935.idle1953(),"worker error test drains");
        check(SaveQueue1935.nativeBytes()==0&&SaveQueue1935.maximumPixels()==0,"worker Error paths release native accounting");
    }
    public static void main(String[] args)throws Exception {
        if(args.length==0){runPreparationFailure();runStaleFailure();runWorkerError();}
        else if("admission".equals(args[0]))runPreparationFailure();
        else if("stale".equals(args[0]))runStaleFailure();
        else if("worker".equals(args[0]))runWorkerError();
        else throw new IllegalArgumentException("unknown focused case");
        System.out.println("PASS save1963 checks="+checks+" boundary_checks="+SaveHarness1935.assertions+" physical_android_tested=false");
    }
}
