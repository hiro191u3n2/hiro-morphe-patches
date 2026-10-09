package com.hiro.ulike;

import android.graphics.Bitmap;
import androidx.heifwriter.HeifWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;

/** The three real production helpers execute through only scripted Android and
 * native-application ABI boundaries. Latches represent actual unfinished work,
 * rather than timing thresholds or an imitation of the queue implementation. */
public class SaveHarness1935 {
    static int assertions;
    static synchronized void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    public static final AtomicInteger codecAllocated=new AtomicInteger(),codecPeak=new AtomicInteger();
    static final BlockingQueue<Photo> preparing=new LinkedBlockingQueue<Photo>(),encoding=new LinkedBlockingQueue<Photo>();
    static final BlockingQueue<Integer> publication=new LinkedBlockingQueue<Integer>();
    static final List<String> receipts=Collections.synchronizedList(new ArrayList<String>());
    static final List<Integer> visible=Collections.synchronizedList(new ArrayList<Integer>());
    static volatile int failCorrection,failEncoding,failValidation,failPublication,finishes;
    static volatile boolean restoredInterrupt;
    static final class Photo {
        final int id;final Bitmap image;
        final CountDownLatch corrected=new CountDownLatch(1),encoded=new CountDownLatch(1),closed=new CountDownLatch(1);
        final CountDownLatch verified=new CountDownLatch(1),publish=new CountDownLatch(1);
        volatile Thread worker;
        Photo(Bitmap b){image=b;id=ShotContext1932.forBitmap(b).id;}
    }
    static void waitLatch(CountDownLatch latch,String why){
        boolean interrupted=false;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        try{for(;;){long remaining=deadline-System.nanoTime();if(remaining<=0)throw new AssertionError(why);try{if(latch.await(remaining,TimeUnit.NANOSECONDS))return;}catch(InterruptedException wake){interrupted=true;}}}
        finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    public static String encode(i.o.a.q.c.c.b.e controller,Bitmap bitmap,int rotation,int direction)throws Exception {
        Photo p=new Photo(bitmap);p.worker=Thread.currentThread();preparing.add(p);
        waitLatch(p.corrected,"correction fixture timeout");
        if(p.id==failCorrection)throw new IllegalStateException("correction failed before encoder");
        AsyncSave1935.encoding(bitmap);
        HeifWriter writer=CodecDrain1945.build(new HeifWriter.Builder());
        controller.e=p.id;controller.f=bitmap.getHeight();encoding.add(p);
        try {
            waitLatch(p.encoded,"encoder fixture timeout");
            if(p.id==failEncoding)throw new IllegalStateException("encoder failed");
        } finally {CodecDrain1945.close(writer);p.closed.countDown();}
        // This is the audited saveStage/saveFinal private verification tail.
        waitLatch(p.verified,"validation fixture timeout");
        if(p.id==failValidation)throw new IllegalStateException("HEIF validation failed");
        // These calls are the two transformer-reviewed public transition hooks.
        AsyncSave1935.awaitPublication1956();AsyncSave1935.awaitPublication1956();
        restoredInterrupt|=Thread.currentThread().isInterrupted();
        publication.add(p.id);
        waitLatch(p.publish,"publication fixture timeout");
        if(p.id==failPublication)throw new IllegalStateException("publication rejected");
        visible.add(p.id);
        check(!bitmap.isRecycled(),"owned input retained until exact verified publication");
        return "photo-"+p.id;
    }
    static void await(BooleanSupplier condition,String why)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(!condition.getAsBoolean()&&System.nanoTime()<deadline)Thread.sleep(2);
        check(condition.getAsBoolean(),why);
    }
    static Photo next(BlockingQueue<Photo> queue,String why)throws Exception {
        Photo p=queue.poll(8,TimeUnit.SECONDS);check(p!=null,why);return p;
    }
    static void noPublication(String why)throws Exception{check(publication.poll(80,TimeUnit.MILLISECONDS)==null,why);}
    static Bitmap submit(i.o.a.b1.a.b.f.a manager,int id)throws Exception {
        await(()->!AsyncSave1935.captureBlocked(),"next capture admitted after exact ownership handoff");
        Bitmap bitmap=new Bitmap(new int[]{id,id+1,id+2,id+3});ShotContext1932.bind(bitmap,id);
        i.f.l.n.s.a.b().image=bitmap;AsyncSave1935.submitAuto(manager,0,0);return bitmap;
    }
    static i.o.a.b1.a.b.f.a manager(){return new i.o.a.b1.a.b.f.a(new i.o.a.b1.a.b.f.a.c(){
        public void a(){finishes++;}
        public void b(boolean success,int id,String path,String error){
            if(success)check(path.equals("photo-"+id),"success receipt belongs to exact verified publication");
            receipts.add(success?path:"failure");
        }
    });}
    static void complete(Photo p){p.corrected.countDown();p.encoded.countDown();p.verified.countDown();p.publish.countDown();}
    static void drained()throws Exception {
        await(()->SaveQueue1935.count()==0&&ExitJobs185.count()==0&&SaveQueue1935.idle1953(),"all queue/exit/stage/publication leases drain");
        check(SaveQueue1935.nativeBytes()==0&&SaveQueue1935.maximumPixels()==0,"native image accounting released after tail completion");
        check(codecAllocated.get()==0&&codecPeak.get()<=1,"hardware ownership remains exclusive and releases");
    }
    static void runOverlap()throws Exception {
        i.o.a.b1.a.b.f.a manager=manager();
        Bitmap original=submit(manager,101);Photo a=next(preparing,"first real preparation");a.corrected.countDown();check(next(encoding,"first real encoding")==a,"first encoder identity");
        check(a.image!=original&&Arrays.equals(a.image.pixels,original.pixels),"source pixels remain exact and independently owned");
        submit(manager,102);Photo b=next(preparing,"second correction overlaps first encoder");b.corrected.countDown();
        HeifWriter.closingEntered=new CountDownLatch(1);HeifWriter.closingRelease=new CountDownLatch(1);
        a.encoded.countDown();waitLatch(HeifWriter.closingEntered,"first close actually entered");
        check(encoding.poll(80,TimeUnit.MILLISECONDS)==null,"second encoder never starts while actual close is unconfirmed");
        check(codecAllocated.get()==1&&receipts.isEmpty()&&visible.isEmpty(),"no early hardware release or save success");
        HeifWriter.closingRelease.countDown();waitLatch(a.closed,"confirmed codec close completes");
        HeifWriter.closingEntered=null;HeifWriter.closingRelease=null;
        check(next(encoding,"second encoder starts while preceding validation stays blocked")==b,"real close boundary enables overlap");
        check(a.verified.getCount()==1&&!a.image.isRecycled()&&SaveQueue1935.count()==2,"blocked verification retains exact first image and capture slot");
        noPublication("no first or second visibility while first validation is blocked");
        submit(manager,103);check(SaveQueue1935.count()==3&&AsyncSave1935.captureBlocked(),"tail overlap retains original three-photo capacity");
        b.encoded.countDown();waitLatch(b.closed,"second hardware closes during first validation");b.verified.countDown();
        noPublication("second verified image waits for preceding actual publication tail");
        b.worker.interrupt();noPublication("interrupt cannot reorder publication or release image ownership");
        a.verified.countDown();check(publication.poll(8,TimeUnit.SECONDS)==101,"first publication enters FIFO");
        noPublication("second visibility also waits while first actual commit is blocked");
        check(visible.isEmpty()&&receipts.isEmpty()&&!a.image.isRecycled(),"entered publication does not claim file completion");
        a.publish.countDown();check(publication.poll(8,TimeUnit.SECONDS)==102,"second visibility enters only after first verified completion");
        await(()->receipts.size()==1,"first receipt enqueued after actual publication");
        check(receipts.equals(Arrays.asList("photo-101"))&&visible.equals(Arrays.asList(101)),"only first real file is complete");
        check(restoredInterrupt,"waiting interruption preserved after ordered publication gate");
        Photo c=next(preparing,"third correction can proceed when first tail exits");c.corrected.countDown();check(next(encoding,"third encode overlaps second publication tail")==c,"second tail does not occupy idle hardware");
        b.publish.countDown();c.encoded.countDown();c.verified.countDown();check(publication.poll(8,TimeUnit.SECONDS)==103,"third verified publication FIFO");c.publish.countDown();
        drained();check(receipts.equals(Arrays.asList("photo-101","photo-102","photo-103")),"success receipts remain original submission FIFO");
        check(visible.equals(Arrays.asList(101,102,103)),"actual visibility commits FIFO");
        check(a.image.isRecycled()&&b.image.isRecycled()&&c.image.isRecycled(),"all owned input images recycled after respective tails");
    }
    static void failurePair(int mode,int base)throws Exception {
        int start=receipts.size();i.o.a.b1.a.b.f.a manager=manager();
        if(mode==1)failValidation=base;else if(mode==2)failPublication=base;else if(mode==3)failCorrection=base+1;else failEncoding=base;
        submit(manager,base);Photo a=next(preparing,"failure sequence first prepare");a.corrected.countDown();next(encoding,"failure sequence first encode");
        submit(manager,base+1);Photo b=next(preparing,"failure sequence second prepare");b.corrected.countDown();
        a.encoded.countDown();waitLatch(a.closed,"failed or succeeding encoder actually closes");
        if(mode!=3){check(next(encoding,"following encoder admitted after predecessor close")==b,"failure successor encoder identity");b.encoded.countDown();waitLatch(b.closed,"following close completes");b.verified.countDown();b.publish.countDown();}
        if(mode!=4)noPublication("failure successor cannot publish while predecessor tail unresolved");
        a.verified.countDown();a.publish.countDown();
        if(mode==3)check(publication.poll(8,TimeUnit.SECONDS)==base,"prior success publication precedes correction failure receipt");
        else if(mode==2){check(publication.poll(8,TimeUnit.SECONDS)==base,"failing commit has original ordered turn");check(publication.poll(8,TimeUnit.SECONDS)==base+1,"rejected publication still releases terminal turn");}
        else check(publication.poll(8,TimeUnit.SECONDS)==base+1,"validation/encode failure releases turn without public success");
        drained();
        List<String> expected=mode==3?Arrays.asList("photo-"+base,"failure"):Arrays.asList("failure","photo-"+(base+1));
        check(receipts.subList(start,receipts.size()).equals(expected),"failure and success receipts preserve submission order");
        check(a.image.isRecycled()&&b.image.isRecycled(),"failure paths release exactly owned inputs");
        failValidation=failPublication=failCorrection=failEncoding=0;
    }
    static void runCloseFault(int fault)throws Exception {
        i.o.a.b1.a.b.f.a manager=manager();submit(manager,900);Photo a=next(preparing,"fault preparation");a.corrected.countDown();next(encoding,"fault encoder");
        if(fault==1)android.media.MediaCodec.failRelease=true;else if(fault==2)android.os.Handler.codecMode=1;else android.media.MediaCodec.failStop=true;
        complete(a);
        await(()->SaveQueue1935.count()==0&&ExitJobs185.count()==0&&SaveQueue1935.idle1953(),"fault ownership and terminal gate cleanup");
        check(SaveQueue1935.nativeBytes()==0&&SaveQueue1935.maximumPixels()==0,"fault image accounting released");
        if(fault<=2){check(receipts.equals(Arrays.asList("failure"))&&visible.isEmpty(),"unconfirmed close never publishes success");try{CodecDrain1945.build(new HeifWriter.Builder());throw new AssertionError("poisoned allocation must not reuse");}catch(RuntimeException expected){}}
        else check(receipts.equals(Arrays.asList("photo-900")),"stop failure with proven actual release preserves legacy save behavior");
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=0)runCloseFault(Integer.parseInt(args[0]));
        else {runOverlap();failurePair(1,201);failurePair(2,301);failurePair(3,401);failurePair(4,501);
            // Native scratch retention is counted alongside every still-live tail.
            SpeedWorkers1935.scratch=64;check(!SaveQueue1935.memoryAllows(SaveQueue1935.RESERVE+96+63,4),"scratch retained byte admission bound enforced");
            check(SaveQueue1935.memoryAllows(SaveQueue1935.RESERVE+96+64,4),"scratch exact memory boundary");
            SpeedWorkers1935.scratch=Long.MAX_VALUE;check(!SaveQueue1935.memoryAllows(Long.MAX_VALUE,4),"saturated native scratch cannot overflow allowance");SpeedWorkers1935.scratch=0;
        }
        System.out.println("PASS h33 actual_helpers assertions="+assertions+" physical_android_tested=false");
    }
}
