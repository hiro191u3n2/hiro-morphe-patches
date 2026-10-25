package com.hiro.ulike;

import android.graphics.Bitmap;
import java.util.*;
import java.util.concurrent.*;

/** Public boundary fault injection. The image/SDK adapter is the only fake. */
public class SaveHarness1935 {
    static int assertions;
    public static volatile boolean failSnapshot,failTransfer,failAfterSubmit,assertion,threadDeath,failTimingLookup,failTimingFinish;
    public static volatile int failedTimingFinishes;
    static volatile Error injected;
    public static volatile Bitmap lastOwned;
    static volatile int finishes;
    static final List<String> receipts=Collections.synchronizedList(new ArrayList<String>());
    static final CountDownLatch encoded=new CountDownLatch(1),release=new CountDownLatch(1);
    static final List<Throwable> uncaught=Collections.synchronizedList(new ArrayList<Throwable>());
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    public static Error failure(){injected=threadDeath?new ThreadDeath():assertion?new AssertionError("scripted helper initialization failure"):new NoSuchMethodError("scripted optional helper resolution failure");return injected;}
    static void await(java.util.function.BooleanSupplier test,String reason)throws Exception{
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
        while(!test.getAsBoolean()&&System.nanoTime()<until)Thread.sleep(2);
        check(test.getAsBoolean(),reason);
    }
    public static String encode(i.o.a.q.c.c.b.e controller,Bitmap bitmap,int rotation,int direction)throws Exception{
        AsyncSave1935.encoding(bitmap);encoded.countDown();
        check(release.await(3,TimeUnit.SECONDS),"scripted save release");
        check(!bitmap.isRecycled(),"owned image must survive through encoding");
        return "photo-"+ShotContext1932.forBitmap(bitmap).id;
    }
    static Bitmap source(int id){
        Bitmap value=new Bitmap(new int[]{id,id+1,id+2,id+3});
        ShotContext1932.bind(value,id);i.f.l.n.s.a.b().image=value;return value;
    }
    public static void main(String[] args)throws Exception{
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{uncaught.add(error);error.printStackTrace();});
        String mode=args[0];assertion=mode.endsWith("assertion");threadDeath=mode.endsWith("thread-death");
        failSnapshot=mode.startsWith("snapshot");failTransfer=mode.startsWith("transfer");
        failAfterSubmit=mode.equals("after-submit");
        failTimingLookup=mode.equals("timing-link");failTimingFinish=failTimingLookup||mode.contains("-finish-");
        boolean failedDiagnostic=failTimingFinish;
        boolean after=failAfterSubmit;
        i.o.a.b1.a.b.f.a manager=new i.o.a.b1.a.b.f.a(new i.o.a.b1.a.b.f.a.c(){
            public void a(){finishes++;}
            public void b(boolean saved,int ignored,String path,String error){receipts.add(saved?path:"failure");}
        });
        Bitmap nativeSource=source(101);
        Throwable escaped=null;
        try{AsyncSave1935.submitAuto(manager,0,0);}catch(Throwable error){escaped=error;}
        if(after){
            check(encoded.await(3,TimeUnit.SECONDS),"admitted job reaches encoder after optional Error");
            check(SaveQueue1935.count()==1&&ExitJobs185.count()==1,"submitted job keeps exactly one reservation");
            check(!lastOwned.isRecycled(),"submitted photo remains owned by encoder");
            release.countDown();
        }
        // Baseline must reach this assertion instead of merely timing out or
        // throwing a linkage error that would not prove the ownership defect.
        await(()->SaveQueue1935.count()==0&&ExitJobs185.count()==0,"capture and exit reservation leaked");
        if(failedDiagnostic)check(failedTimingFinishes==1,"diagnostic finish LinkageError reached the terminal cleanup boundary");
        boolean fatal=assertion||threadDeath;
        if(fatal&&mode.startsWith("snapshot"))check(escaped==injected,"same fatal synchronous Error is rethrown after cleanup");
        else check(escaped==null,"recoverable failure remains contained at admission boundary");
        if(fatal&&mode.startsWith("transfer")){
            await(()->uncaught.size()==1,"worker propagates fatal Error after cleanup");
            check(uncaught.get(0)==injected,"same fatal worker Error is rethrown after cleanup");
        }else check(uncaught.isEmpty(),"capture worker must not terminate on recoverable helper error");
        check(!nativeSource.isRecycled(),"SDK source never recycled by failed handoff");
        check(!AsyncSave1935.captureBlocked(),"shutter admission reopens after terminal cleanup");
        await(()->finishes==1,"exactly one capture finish callback");
        if(after){
            await(()->receipts.size()==1,"success receipt delivered");
            check(receipts.equals(Arrays.asList("photo-101")),"post-submit optional failure preserves actual save");
            check(lastOwned.isRecycled(),"submitted image disposed after encoding");
            check(SaveQuality2.failures==0,"post-submit error does not invent failed capture receipt");
        }else{
            check(SaveQuality2.failures==1,"pre-submit error reports one failed capture");
            check(receipts.isEmpty(),"failed handoff never sends successful save receipt");
            if(mode.startsWith("transfer"))check(lastOwned!=nativeSource&&lastOwned.isRecycled(),"owned handoff copy disposed on LinkageError");
            // A successful next shutter proves the queue and native generation
            // can continue after each failure, with one terminal receipt.
            source(202);AsyncSave1935.submitAuto(manager,90,0);
            check(encoded.await(3,TimeUnit.SECONDS),"new capture accepted after failed handoff");
            release.countDown();
            await(()->SaveQueue1935.count()==0&&ExitJobs185.count()==0&&receipts.size()==1,"next saved capture drains");
            check(receipts.equals(Arrays.asList("photo-202")),"only the next actual image is reported as saved");
            check(finishes==2,"each generation finishes exactly once");
        }
        await(()->SaveQueue1935.idle1953(),"all stages and native budgets return to idle");
        check(SaveQueue1935.nativeBytes()==0&&SaveQueue1935.maximumPixels()==0,"no stale native reservation retained");
        System.out.println("PASS audit-save1980 assertions="+assertions+" mode="+mode);
    }
}
