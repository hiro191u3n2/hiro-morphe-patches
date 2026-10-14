package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;

/** Actual Async jobs and shared queue/CPU permits; only detector and SDK calls
 * are scripted. The overlap proves an eligible window, not device speed. */
public final class PhotoAnalysis1981Test {
    static int assertions;
    static synchronized void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static void await(CountDownLatch latch,String reason)throws Exception{check(latch.await(4,TimeUnit.SECONDS),reason);}
    static void until(BooleanSupplier condition,String reason)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
        while(!condition.getAsBoolean()&&System.nanoTime()<end)Thread.yield();
        check(condition.getAsBoolean(),reason);
    }
    static Bitmap image(int value){return new Bitmap(new int[]{value,value+1,value+2,value+3});}
    static PhotoAnalysis1981.Context context(Bitmap bitmap,boolean owned,SaveMemory1981.Plan plan){
        return new PhotoAnalysis1981.Context(bitmap,90,1,new PhotoDetail.Settings(4),true,false,
            new ShotContext1932.Snapshot(701),ProcessingTiming1947.begin(bitmap),owned,plan);
    }
    static void completionAndGuards()throws Exception {
        Bitmap original=image(11);FaceRegions1934.fail=false;FaceRegions1934.entered=null;FaceRegions1934.release=null;
        FaceRegions1934.found=0;FaceResult1981 empty=FaceResult1981.analyze(original,90);
        check(empty.completed&&empty.mask!=null&&empty.mask.value==0,"zero faces is completed reusable evidence");
        FaceRegions1934.found=-1;check(!FaceResult1981.analyze(original,90).completed,"negative detector return is not successful empty evidence");
        FaceRegions1934.found=9;check(!FaceResult1981.analyze(original,90).completed,"out-of-contract face count is not successful empty evidence");
        FaceRegions1934.found=1;FaceRegions1934.fail=true;check(!FaceResult1981.analyze(original,90).completed,"legacy empty fallback after detector failure is not reused");
        FaceRegions1934.fail=false;FaceResult1981 mask=FaceResult1981.analyze(original,90);
        check(mask.completed&&mask.mask.value==101,"successful mask preserves original pixels and rotation");
        SaveMemory1981.Plan plan=new SaveMemory1981.Plan(2,2,2,2,16,0,0);
        for(int mode=0;mode<4;mode++){
            Bitmap bitmap=image(21);if(mode==0)bitmap.gainmap=true;if(mode==1)bitmap.config=Bitmap.Config.RGBA_F16;if(mode==2)bitmap.space=new ColorSpace(false);
            PhotoAnalysis1981.Context c=context(bitmap,mode!=3,plan);
            check(!c.offer(),"HDR/wide/F16/borrowed input never enters read-only side analysis: "+mode);c.closeAndAwait1981();
        }
        PhotoAnalysis1981.Context valid=context(original,true,plan);
        check(valid.offer()&&valid.start(),"owned software sRGB context reserves and starts");valid.completeFaces(mask);valid.completeNoise(new SpatialNoise1934(11));valid.finished();
        check(valid.faces(original,90)==mask.mask&&valid.spatial(original).value==11,"same owned image obtains its exact ready analyses");
        check(valid.faces(image(11),90)==null,"equal pixels from another bitmap cannot reuse face coordinates");
        check(valid.faces(original,0)==null,"different rotation cannot reuse face coordinates");
        original.generation++;check(valid.faces(original,90)==null&&valid.spatial(original)==null,"mutated generation invalidates every prepared pixel result");original.generation--;
        original.density++;check(valid.faces(original,90)==null,"density metadata change invalidates prepared context");original.density--;
        valid.closeAndAwait1981();check(PhotoAnalysis1981.retainedBytes1981()==0,"closed context releases optional budget exactly once");valid.closeAndAwait1981();check(PhotoAnalysis1981.retainedBytes1981()==0,"repeated close has no negative budget");
        PhotoAnalysis1981.Context reduced=context(image(31),true,new SaveMemory1981.Plan(2,2,1,1,16,0,0));
        check(reduced.reduceFirst&&!reduced.needsSpatial&&reduced.needsFaces,"reduced photo keeps face analysis but does not precompute noise from wrong pixels");reduced.closeAndAwait1981();
        PhotoAnalysis1981.Context queued=context(image(41),true,plan);check(queued.offer(),"queued optional work reserves budget");
        check(queued.faces(queued.input,90)==null&&!queued.start(),"foreground arriving before reader starts cancels optional task and uses ordinary analysis");queued.closeAndAwait1981();
        check(PhotoAnalysis1981.retainedBytes1981()==0,"cancel-before-start releases optional reservation");
    }
    static void overlap(final int mode)throws Exception {
        final boolean failedAnalysis=mode==1,normalEmpty=mode==2,failNormalization=mode==3;
        SaveQuality2.systemInfo=true;android.app.ActivityManager.available=2L*1024*1024*1024;android.app.ActivityManager.threshold=64L*1024*1024;android.app.ActivityManager.low=false;
        final CountDownLatch firstEntered=new CountDownLatch(1),firstRelease=new CountDownLatch(1),secondEntered=new CountDownLatch(1),secondReturned=new CountDownLatch(1);
        final CountDownLatch readerEntered=new CountDownLatch(1),readerRelease=new CountDownLatch(1),bothReceipts=new CountDownLatch(1);
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();final AtomicInteger routes=new AtomicInteger();
        final List<Boolean> receipts=Collections.synchronizedList(new ArrayList<Boolean>());
        final PhotoDetail.Settings frozen=new PhotoDetail.Settings(4);final AtomicInteger snapshotsAtSecond=new AtomicInteger();
        FaceRegions1934.calls.set(0);SpatialNoise1934.calls=0;FaceRegions1934.found=normalEmpty?0:1;FaceRegions1934.fail=failedAnalysis;FaceRegions1934.entered=readerEntered;FaceRegions1934.release=readerRelease;
        i.o.a.b1.a.b.f.a manager=new i.o.a.b1.a.b.f.a(new i.o.a.b1.a.b.f.a.c(){public void a(){}public void b(boolean ok,int n,String path,String error){receipts.add(ok);if(receipts.size()==2)bothReceipts.countDown();}});
        final Bitmap first=image(101);ShotContext1932.bind(first,801);i.f.l.n.s.a.b().image=first;PhotoDetail.live=new PhotoDetail.Settings(1);
        HostRouter1981.encoder=(controller,input,rotation,direction)->{
            int route=routes.incrementAndGet();
            try {
                if(route==1){firstEntered.countDown();if(!firstRelease.await(4,TimeUnit.SECONDS))throw new AssertionError("first correction wait");}
                else {
                    secondEntered.countDown();
                    if(failNormalization)throw new java.io.IOException("scripted correction failure while reader active");
                    check(input!=first&&!input.isRecycled()&&input.pixels[0]==201,"next photo uses independent exact captured pixels");
                    check(AsyncSave1935.settingsForPhoto1981()==frozen,"job uses admitted settings after live UI changes");
                    check(PhotoDetail.snapshots==snapshotsAtSecond.get(),"photo settings lookup does not allocate an unused live snapshot");
                    check(AsyncSave1935.snapshotForPhoto1981(input).id==802,"job keeps exact shot metadata despite global map update");
                    FaceRegions1934.Mask mask=PhotoAnalysis1981.faces1981(input,rotation);
                    check(mask.value==(normalEmpty?0:291),"face result keeps admitted rotation and same-photo pixels");
                    check(FaceRegions1934.calls.get()==(failedAnalysis?2:1),"only failed analysis retries; successful empty or mask is reused");
                    SpatialNoise1934 noise=PhotoAnalysis1981.spatial1981(input);
                    check(noise.value==201&&SpatialNoise1934.calls==1,"initial noise result is reused only for original unchanged pixels");
                    check(input.pixels[0]==201,"optional analysis has no pixel mutation");
                }
                AsyncSave1935.encoding(input);AsyncSave1935.hardwareClosed1956();return "analysis-"+route;
            }catch(java.io.IOException expected){if(!failNormalization)failure.compareAndSet(null,expected);throw expected;}
            catch(Exception error){failure.compareAndSet(null,error);throw error;}
            catch(Error error){failure.compareAndSet(null,error);throw error;}
            finally{if(route==2)secondReturned.countDown();}
        };
        AsyncSave1935.submitAuto(manager,0,0);await(firstEntered,"first real Async job occupies correction slot");
        until(()->!AsyncSave1935.captureBlocked(),"independent owned handoff reopens next shutter while correction waits");
        final Bitmap second=image(201);ShotContext1932.bind(second,802);i.f.l.n.s.a.b().image=second;PhotoDetail.live=frozen;
        AsyncSave1935.submitAuto(manager,90,1);await(readerEntered,"queued next owned photo begins read-only analysis before earlier correction completes");
        check(firstRelease.getCount()==1&&routes.get()==1,"analysis overlaps an actual occupied preparation slot with no CPU group");
        final Bitmap owned=SaveHarness1935.lastOwned;check(owned!=second&&!owned.isRecycled(),"reader borrows a job-owned copy, never SDK storage");
        second.pixels[0]=999;check(owned.pixels[0]==201,"SDK holder reuse cannot change queued analysis image");
        ShotContext1932.map.put(owned,new ShotContext1932.Snapshot(999));PhotoDetail.live=new PhotoDetail.Settings(13);snapshotsAtSecond.set(PhotoDetail.snapshots);
        if(failNormalization){
            firstRelease.countDown();await(secondEntered,"following correction starts while optional reader still owns input");await(secondReturned,"normalization failure reaches Job cleanup");
            check(!owned.isRecycled()&&PhotoAnalysis1981.retainedBytes1981()>0,"failed normalization retains input and memory while reader is active");
            check(!bothReceipts.await(100,TimeUnit.MILLISECONDS)&&!owned.isRecycled(),"terminal receipt and disposal wait until the actual bitmap reader returns");
            readerRelease.countDown();
        }else {
            readerRelease.countDown();until(()->SpeedWorkers1935.cpuIdle1944(),"read-only analysis completes and returns shared CPU permit");
            FaceRegions1934.fail=false;firstRelease.countDown();await(secondReturned,"following normalize consumes prepared results");
        }
        until(()->SaveQueue1935.idle1953()&&ExitJobs185.count()==0&&receipts.size()==2,"both real jobs finish FIFO publication and exit ownership");
        check(failure.get()==null,"analysis/correction has no unexpected failure: "+failure.get());
        check(receipts.equals(Arrays.asList(Boolean.TRUE,Boolean.valueOf(!failNormalization))),"analysis never invents success or changes FIFO photo receipts");
        check(owned.isRecycled()&&!second.isRecycled(),"only owned handoff is disposed after its last reader");
        if(failNormalization)check(SpatialNoise1934.calls==0,"cancelled job skips subsequent optional read after face reader finishes");
        check(PhotoAnalysis1981.retainedBytes1981()==0&&SpeedWorkers1935.cpuIdle1944(),"reader permits and optional memory reservation fully drain");
        HostRouter1981.encoder=null;FaceRegions1934.entered=null;FaceRegions1934.release=null;FaceRegions1934.fail=false;
    }
    public static void main(String[]args)throws Exception {
        completionAndGuards();for(int mode=0;mode<4;mode++)overlap(mode);
        System.out.println("PASS PhotoAnalysis1981 assertions="+assertions);
    }
}
