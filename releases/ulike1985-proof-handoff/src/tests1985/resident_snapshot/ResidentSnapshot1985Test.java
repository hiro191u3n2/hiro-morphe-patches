package com.hiro.ulike;

import android.graphics.Bitmap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Actual Resident producer and copy budget, with explicit queue/bitmap peers. */
public final class ResidentSnapshot1985Test {
    static final int M=1024*1024;
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void reset()throws Exception{ResidentTicket1984Test.reset();GpuQualification1961.resetSnapshot1985();}
    static Bitmap image(){return ResidentDiagnostics1982Test.image();}
    static Bitmap invoke(Bitmap image,ProcessingTiming1947.Trace trace){
        return GpuResident1976.finish1978(image,ResidentDiagnostics1982Test.plan(),4,true,new int[]{7,8},0,91,183,
            ResidentDiagnostics1982Test.plan(),false,trace);
    }
    static void await(CountDownLatch latch){
        try{if(!latch.await(5,TimeUnit.SECONDS))throw new AssertionError("Resident fixture barrier timed out");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
    }
    static void noOwner(Bitmap image)throws Exception {
        check(!image.isRecycled()&&image.recycleCalls1984==0&&image.pixels[0]==0xff123456,"caller image remains unchanged and owned by the caller");
        check(GpuQualification1961.held==0&&GpuQualification1961.reserved1984==0&&GpuQualification1961.queued==null,"no retained snapshot or queue ticket leaks");
        ResidentTicket1984Test.noCopyLock();
    }

    static void staleBeforeBegin(final boolean baseline)throws Exception {
        reset();
        final Bitmap oldImage=image(),newImage=image();
        // Controlled accounting makes two simultaneous tickets exceed 96 MiB
        // without allocating large host bitmaps; actual pixels remain real.
        oldImage.slack=oldImage.copySlack=80*M;newImage.slack=newImage.copySlack=80*M;
        final ProcessingTiming1947.Trace oldTrace=new ProcessingTiming1947.Trace(),newTrace=new ProcessingTiming1947.Trace();
        final CountDownLatch reserved=new CountDownLatch(1),resume=new CountDownLatch(1);
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        final GpuQualification1961.Reservation1984[] oldTicket={null};
        final Thread[] owner={null};
        GpuQualification1961.afterReserve1984=new Runnable(){public void run(){
            if(Thread.currentThread()==owner[0]){
                oldTicket[0]=GpuQualification1961.lastReservation1984;reserved.countDown();await(resume);
            }
        }};
        owner[0]=new Thread(new Runnable(){public void run(){
            try{if(invoke(oldImage,oldTrace)!=null)throw new AssertionError("uncertified old Resident adopted GPU");}
            catch(Throwable error){failure.set(error);}
        }},"resident85-paused-reservation");
        int copies=Bitmap.copies;owner[0].start();GpuQualification1961.Reservation1984 freshTicket=null;
        try {
            check(reserved.await(5,TimeUnit.SECONDS),"old capture reached reserve before Bitmap.copy");
            check(Bitmap.copies==copies&&GpuQualification1961.held>80L*M,"old ticket accounts for bytes before allocation");
            check(!oldTicket[0].begun1985,"the paused old reservation has not materialized any pixels");
            ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged1985();
            check(invoke(newImage,newTrace)==null,"new uncertified capture keeps its CPU output route");
            if(baseline){
                check(Bitmap.copies==copies&&GpuQualification1961.queued==null,"published .84 prevents a fresh Resident copy behind a stale unstarted token");
                check(GpuQualification1961.held>80L*M,"published .84 keeps the unstarted old byte reservation until its caller returns");
            }else{
                freshTicket=GpuQualification1961.lastReservation1984;
                check(Bitmap.copies==copies+1&&GpuQualification1961.queued!=null,"new capture copies and queues one fresh image while old caller is paused");
                check(GpuQualification1961.held==ResidentTicket1984Test.bytes(newImage),"old unmaterialized bytes are released before the fresh reservation");
                check(oldTicket[0].effectiveReleases==1&&!oldTicket[0].owns,"obsolete ticket loses its reservation exactly once");
                check(freshTicket.begun1985&&freshTicket.committed,"new ticket explicitly begins before ownership transfers to the queue");
                check(newTrace.copyReason1985==0&&newTrace.copyEpoch1985==ProcessingTiming1947.epoch,"new photo saves its own admission snapshot");
            }
        }finally{resume.countDown();owner[0].join(5000);GpuQualification1961.afterReserve1984=null;}
        check(!owner[0].isAlive()&&failure.get()==null,"old caller exits without a late allocation or ownership exception");
        check(!oldTicket[0].begun1985&&oldTicket[0].effectiveReleases==1,"stale caller never materializes pixels or releases its ticket twice");
        check(!oldImage.isRecycled()&&!newImage.isRecycled(),"both photographs keep their original image ownership");
        if(!baseline){
            check(GpuQualification1961.held==ResidentTicket1984Test.bytes(newImage)&&freshTicket.effectiveReleases==0,
                  "old finally cannot release the new queued snapshot's bytes");
            check(newTrace.copyReason1985==0&&oldTrace.copyEpoch1985!=newTrace.copyEpoch1985,
                  "late old completion cannot rewrite the new photo's copy decision");
            GpuQualification1961.drop();
            check(freshTicket.effectiveReleases==1,"fresh queued snapshot is released once by its own owner");
        }
        noOwner(oldImage);noOwner(newImage);
    }

    static void refusalReasons()throws Exception {
        reset();Bitmap image=image();ProcessingTiming1947.Trace trace=new ProcessingTiming1947.Trace();
        GpuSnapshotBudget1981.Copy used=GpuSnapshotBudget1981.tryCopy(1,1);check(used!=null&&used.begin(),"another whole-image validation uses this capture's single copy");used.close();
        int copies=Bitmap.copies;invoke(image,trace);
        check(trace.copyReason1985==4&&ProcessingTiming1947.reason==10,"whole-copy use is distinguished from other admission deferrals");
        check(Bitmap.copies==copies&&GpuQualification1961.reserveCalls1985==0,"copy quota refusal does not reserve queue memory or copy pixels");
        long savedEpoch=trace.copyEpoch1985;ProcessingTiming1947.epoch++;
        GpuSnapshotBudget1981.Copy later=GpuSnapshotBudget1981.tryCopy(1,1);check(later!=null,"next fresh capture receives the Resident turn");later.close();
        check(trace.copyReason1985==4&&trace.copyEpoch1985==savedEpoch,"later copy availability cannot rewrite the saved rejection");noOwner(image);

        reset();image=image();trace=new ProcessingTiming1947.Trace();GpuSnapshotBudget1981.offerWhole(2);ProcessingTiming1947.epoch++;
        copies=Bitmap.copies;invoke(image,trace);
        check(trace.copyReason1985==5&&trace.copyPreferred1985==2&&trace.copyActive1985==-1,"round-robin deferral identifies the selected finish family");
        check(Bitmap.copies==copies&&GpuQualification1961.reserveCalls1985==0,"another family's turn allocates neither image nor reservation");noOwner(image);

        for(boolean started:new boolean[]{false,true}){
            reset();image=image();trace=new ProcessingTiming1947.Trace();GpuSnapshotBudget1981.Copy other=GpuSnapshotBudget1981.tryCopy(1,0);
            check(other!=null,"independent strip owns the shared copy exclusion");if(started)check(other.begin(),"strip copy enters allocation phase");
            copies=Bitmap.copies;
            try {
                invoke(image,trace);check(trace.copyReason1985==(started?8:9)&&trace.copyActive1985==0&&trace.copyStarted1985==started,
                    "actual allocation and tentative reservation get different saved reasons");
                check(Bitmap.copies==copies&&GpuQualification1961.reserveCalls1985==0,"copy contention does not retain a Resident image or queue ticket");
                if(!started){check(other.begin(),"other owner can later progress");check(!trace.copyStarted1985&&trace.copyReason1985==9,"saved reservation reason does not become a live copying label");}
            }finally{other.close();}
            noOwner(image);
        }
        reset();image=image();trace=new ProcessingTiming1947.Trace();copies=Bitmap.copies;Thread.currentThread().interrupt();
        try{invoke(image,trace);check(trace.copyReason1985==3&&ProcessingTiming1947.reason==11,"caller cancellation is saved as its actual admission reason");
            check(Thread.currentThread().isInterrupted()&&Bitmap.copies==copies,"diagnostics preserve cancellation and do not copy");}
        finally{Thread.interrupted();}noOwner(image);
    }

    static void beginGate()throws Exception {
        reset();Bitmap image=image();int copies=Bitmap.copies;
        GpuQualification1961.beforeBegin1985=new Runnable(){public void run(){ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged1985();}};
        invoke(image,new ProcessingTiming1947.Trace());
        check(GpuQualification1961.beginCalls1985==1&&Bitmap.copies==copies,"capture between Copy.begin and ticket.begin cannot materialize an unreserved image");
        check(ProcessingTiming1947.reason==11&&GpuQualification1961.releases1984==1,"late begin cancellation is reported and releases its reserved bytes once");noOwner(image);
        GpuQualification1961.beforeBegin1985=null;invoke(image,new ProcessingTiming1947.Trace());
        check(Bitmap.copies==copies+1&&GpuQualification1961.queued!=null,"new capture can retry after cancelled begin without increasing a per-capture limit");GpuQualification1961.drop();noOwner(image);

        reset();image=image();copies=Bitmap.copies;GpuQualification1961.beginReject1985=true;invoke(image,new ProcessingTiming1947.Trace());
        check(Bitmap.copies==copies&&GpuQualification1961.commitCalls1984==0,"ticket begin refusal stops before clone and commit");
        check(GpuQualification1961.releases1984==1&&ProcessingTiming1947.reason==11,"begin refusal releases once and records cancellation");noOwner(image);
        for(int fault=1;fault<=5;fault++){
            reset();image=image();copies=Bitmap.copies;GpuQualification1961.beginFault1985=fault;Throwable seen=null;
            try{invoke(image,new ProcessingTiming1947.Trace());}catch(Throwable error){seen=error;}
            check((seen!=null)==(fault==5),"begin fault preserves the existing ordinary/fatal exception boundary "+fault);
            check(Bitmap.copies==copies&&GpuQualification1961.beginCalls1985==1&&GpuQualification1961.commitCalls1984==0,
                  "begin exception cannot create or publish a clone "+fault);
            check(GpuQualification1961.releases1984==1,"failed begin releases its ticket once "+fault);noOwner(image);
        }
    }

    static void diagnosticNoninterference()throws Exception {
        for(int fault=1;fault<=5;fault++){
            reset();Bitmap image=image();int copies=Bitmap.copies,created=Bitmap.created;ProcessingTiming1947.Trace trace=new ProcessingTiming1947.Trace();
            ProcessingTiming1947.copySinkFault1985=fault;
            check(invoke(image,trace)==null,"optional copy explanation cannot change unknown foreground CPU fallback "+fault);
            check(Bitmap.copies==copies+1&&GpuQualification1961.beginCalls1985==1&&GpuQualification1961.queued!=null,
                  "optional sink exception cannot block a properly reserved copy "+fault);
            Bitmap clone=Bitmap.all.get(created);check(clone!=image&&!clone.isRecycled(),"queue owns a separate immutable clone");
            GpuQualification1961.run();
            check(GpuQualification1961.admissions==1&&QualityPipeline1932.cpu==2&&QualityPipeline1932.normalCompare==2&&QualityPipeline1932.residentCompare==2,
                  "all complete independent CPU/proof comparisons still execute despite sink failure "+fault);
            check(QualityPipeline1932.normal==2&&QualityPipeline1932.resident==2,"both foreground-shaped speed routes still run twice "+fault);
            check(clone.isRecycled()&&clone.recycleCalls1984==1,"finished snapshot is recycled once "+fault);
            noOwner(image);ResidentTicket1984Test.threadState();
        }
    }

    public static void main(String[] args)throws Exception {
        boolean baseline=args.length>0&&args[0].equals("published84");staleBeforeBegin(baseline);
        if(!baseline){refusalReasons();beginGate();diagnosticNoninterference();}
        int total=assertions+ResidentTicket1984Test.assertions+ResidentDiagnostics1982Test.assertions;
        System.out.println("{\"status\":\"passed\",\"assertions\":"+total+
            ",\"published84_resident_stale_copy_reproduced1985\":"+baseline+
            ",\"resident_snapshot_epoch_handoff1985\":"+(!baseline)+
            ",\"resident_snapshot_begin_gate1985\":"+(!baseline)+
            ",\"resident_snapshot_diagnostics_noninterference1985\":"+(!baseline)+"}");
    }
}
