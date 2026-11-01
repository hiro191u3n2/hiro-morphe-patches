package com.hiro.ulike;

import android.content.Context;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual queue/copy-budget code; controlled save state and probe barriers. */
public final class GpuQueue1981Test {
    static int assertions;
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static final long M=1024L*1024;
    static void capture(){SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();}
    static void quiet()throws Exception{field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);SaveQueue1935.idle=true;GpuQualification1961.wake();}
    static void drain()throws Exception{long limit=System.nanoTime()+10000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>limit)throw new AssertionError("queue ownership did not drain");Thread.sleep(2);}}
    static void copyBudget()throws Exception{
        int[] counts=new int[3];
        for(int shot=0;shot<9;shot++){
            capture();
            for(int f:new int[]{1,2,4})GpuSnapshotBudget1981.offerWhole(f);
            int copied=0;
            for(int f:new int[]{1,2,4}){
                GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryCopy(80*M,f);
                if(copy!=null){try{check(copy.begin(),"large complete image can begin");check(copy.current(),"copy owns current epoch");copied++;counts[f==1?0:f==2?1:2]++;}finally{copy.close();}}
            }
            check(copied==1,"at most one foreground whole image per capture");
        }
        check(Arrays.equals(counts,new int[]{3,3,3}),"resident/finish/moire all make progress despite fixed call order");
        Arrays.fill(counts,0);
        for(int shot=0;shot<6;shot++){
            capture();for(int f:new int[]{1,2,4})GpuSnapshotBudget1981.offerWhole(f);
            // Finish is still unknown but its memory/retry gate never opens.
            // It must not keep the other two stages behind its selected turn.
            for(int f:new int[]{1,4}){
                GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryCopy(80*M,f);
                if(copy!=null)try{check(copy.begin(),"eligible family progresses while another family is resource-denied");counts[f==1?0:2]++;}finally{copy.close();}
            }
        }
        check(Arrays.equals(counts,new int[]{2,0,2}),"a permanently denied preferred family cannot starve other whole stages");
        capture();GpuSnapshotBudget1981.Copy first=GpuSnapshotBudget1981.tryCopy(16*M,0);
        check(first!=null&&first.begin(),"first bounded strip copy");
        check(GpuSnapshotBudget1981.tryCopy(1,0)==null,"two workers cannot copy proof inputs concurrently");first.close();
        GpuSnapshotBudget1981.Copy second=GpuSnapshotBudget1981.tryCopy(16*M,0);check(second!=null&&second.begin(),"second strip fits exact 32MiB capture budget");second.close();
        check(GpuSnapshotBudget1981.tryCopy(1,0)==null,"cumulative strip budget is checked before allocation");
        capture();GpuSnapshotBudget1981.Copy large=GpuSnapshotBudget1981.tryCopy(80*M,0);
        check(large!=null&&large.begin(),"complete large Strong strip is not permanently excluded");large.close();
        check(GpuSnapshotBudget1981.tryCopy(1,0)==null,"one oversized strip cannot become many copies");
        capture();GpuSnapshotBudget1981.Copy old=GpuSnapshotBudget1981.tryCopy(M,0);check(old!=null&&old.begin(),"old copy owns its allocation");
        capture();check(!old.current(),"capture change invalidates snapshot publication");
        check(GpuSnapshotBudget1981.tryCopy(M,0)==null,"new capture does not overlap an old unfinished copy");old.close();old.close();
        GpuSnapshotBudget1981.Copy fresh=GpuSnapshotBudget1981.tryCopy(M,0);check(fresh!=null&&fresh.begin(),"new capture retries only after old owner closes");fresh.close();
        check(GpuSnapshotBudget1981.tryCopy(96*M+1,0)==null,"oversized snapshot rejected before allocation");
        capture();GpuSnapshotBudget1981.Copy refused=GpuSnapshotBudget1981.tryCopy(32*M,0);check(refused!=null,"tentative owner before queue admission");refused.close();
        GpuSnapshotBudget1981.Copy retry=GpuSnapshotBudget1981.tryCopy(32*M,0);check(retry!=null&&retry.begin(),"queue refusal before copying refunds the copy allowance");retry.close();
        Arrays.fill(counts,0);
        for(int shot=0;shot<9;shot++){
            capture();for(int f:new int[]{16,32,8})GpuSnapshotBudget1981.offerStrip1981(f);
            int copied=0;
            for(int f:new int[]{16,32,8}){
                GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryStripCopy1981(80*M,f);
                if(copy!=null)try{check(copy.begin(),"named large NR strip owns this capture turn");counts[f==8?0:f==16?1:2]++;copied++;}finally{copy.close();}
            }
            check(copied==1,"large preliminary NR cannot consume Strong's subsequent capture turns");
        }
        check(Arrays.equals(counts,new int[]{3,3,3}),"Strong and both preliminary NR families progress despite fixed call order");
        Arrays.fill(counts,0);
        for(int shot=0;shot<6;shot++){
            capture();for(int f:new int[]{16,32,8})GpuSnapshotBudget1981.offerStrip1981(f);
            for(int f:new int[]{16,8}){
                GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryStripCopy1981(80*M,f);
                if(copy!=null)try{check(copy.begin(),"NR family progresses when another route is denied");counts[f==8?0:1]++;}finally{copy.close();}
            }
        }
        check(Arrays.equals(counts,new int[]{2,2,0}),"blocked residual qualification cannot starve Strong or Single");
    }
    static String legacy(String tag){return "strong-gx1978-tuning-policy-bank-v1:3:queue1981:"+tag;}
    static final class Probe implements GpuQualification1961.Probe {
        final String name;final List<String> order;final AtomicInteger closes=new AtomicInteger();
        final CountDownLatch started,release;
        Probe(String name,List<String> order){this(name,order,null,null);}
        Probe(String name,List<String> order,CountDownLatch started,CountDownLatch release){this.name=name;this.order=order;this.started=started;this.release=release;}
        public void run(GpuQualification1961.Cancellation cancellation){
            order.add(name);if(started!=null)started.countDown();
            if(release!=null)try{release.await();}catch(InterruptedException stop){Thread.currentThread().interrupt();}
        }
        public void close(){closes.incrementAndGet();}
    }
    static void fairness()throws Exception{
        capture();drain();field(GpuQualification1961.class,"lastLegacy1981").setBoolean(null,false);
        List<String> order=Collections.synchronizedList(new ArrayList<String>());
        Probe l1=new Probe("L1",order),l2=new Probe("L2",order),f1=new Probe("F1",order),f2=new Probe("F2",order);
        check(GpuQualification1961.schedule("finish1981:A",1,f1),"first finish queued");
        check(GpuQualification1961.schedule(legacy("A"),1,l1),"first legacy queued");
        check(GpuQualification1961.schedule(legacy("B"),1,l2),"second legacy queued");
        check(GpuQualification1961.schedule("finish1981:B",1,f2),"second finish queued");
        Thread.sleep(20);check(order.isEmpty(),"capture/save path does not execute any queued proof");quiet();drain();
        check(order.equals(Arrays.asList("L1","F1","L2","F2")),"legacy bootstrap first, fair FIFO groups thereafter");
        for(Probe p:new Probe[]{l1,l2,f1,f2})check(p.closes.get()==1,"completed snapshot closes exactly once");
        capture();field(GpuQualification1961.class,"lastLegacy1981").setBoolean(null,false);
        CountDownLatch start=new CountDownLatch(1),release=new CountDownLatch(1);
        Probe blocked=new Probe("blocked",order,start,release),queued=new Probe("discarded",order);
        check(GpuQualification1961.schedule(legacy("cancel"),1,blocked),"cancellable legacy queued");
        check(GpuQualification1961.schedule("finish1981:cancel",1,queued),"queued snapshot owned before capture change");
        quiet();check(start.await(5,TimeUnit.SECONDS),"actual worker entered legacy proof");capture();release.countDown();drain();
        check(blocked.closes.get()==1&&queued.closes.get()==1,"active and queued cancellation release their own snapshots once");
        check(!order.contains("discarded"),"cancelled queued image is never run");
        order.clear();Probe nextLegacy=new Probe("next-L",order),nextFinish=new Probe("next-F",order);
        check(GpuQualification1961.schedule(legacy("fresh"),1,nextLegacy),"fresh legacy snapshot can retry");
        check(GpuQualification1961.schedule("finish1981:fresh",1,nextFinish),"fresh other snapshot can retry");quiet();drain();
        check(order.equals(Arrays.asList("next-F","next-L")),"interrupted legacy turn cannot starve the next fresh finish");
        capture();field(GpuQualification1961.class,"lastLegacy1981").setBoolean(null,false);
        Probe image=new Probe("large-image",order),strip=new Probe("large-strip",order);
        check(GpuQualification1961.schedule("finish1981:large",80*M,image),"large image fits physical retention bound");
        check(GpuQualification1961.canQueue(legacy("large"),80*M),"first legacy progress can retire queued image before clone");
        check(image.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"retired image ownership ends before new snapshot bytes");
        check(GpuQualification1961.schedule(legacy("large"),80*M,strip),"large legacy proof admitted");capture();drain();
        field(GpuQualification1961.class,"lastLegacy1981").setBoolean(null,true);
        Probe oldStrip=new Probe("old-strip",order),freshImage=new Probe("fresh-image",order);
        check(GpuQualification1961.schedule(legacy("large-next"),80*M,oldStrip),"next capture can temporarily retain a legacy snapshot");
        check(GpuQualification1961.canQueue("finish1981:large-next",80*M),"the other group's next turn can make room for a large image");
        check(oldStrip.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"large queue preemption retains no old image");
        check(GpuQualification1961.schedule("finish1981:large-next",80*M,freshImage),"large full-image proof is not starved by strip size");capture();drain();
        check(freshImage.closes.get()==1,"last queued large image released on capture cancellation");
    }
    public static void main(String[] args)throws Exception{
        GpuQualification1961.initialize(new Context());copyBudget();fairness();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"snapshot_round_robin1981_verified\":true,\"large_snapshot_progress1981_verified\":true,\"denied_family_progress1981_verified\":true,\"queue_cancel_fresh_retry1981_verified\":true,\"physical_android_tested\":false}");
    }
}
