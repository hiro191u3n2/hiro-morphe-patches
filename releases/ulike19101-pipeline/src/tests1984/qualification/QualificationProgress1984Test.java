package com.hiro.ulike;

import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Exercises real queue ownership with independently controlled save/copy
 * gates. No fixture result stands in for GPU pixel or physical-device proof. */
public final class QualificationProgress1984Test {
    static final long M=1024L*1024;
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    interface Ready {boolean get();}
    interface Task {void run(GpuQualification1961.Cancellation cancellation);}
    static void await(Ready value,String why)throws Exception{
        long end=System.nanoTime()+4000000000L;
        while(!value.get()){if(System.nanoTime()>=end)throw new AssertionError("timeout: "+why);Thread.sleep(2);}
        check(true,why);
    }
    static Field field(String name)throws Exception{Field value=GpuQualification1961.class.getDeclaredField(name);value.setAccessible(true);return value;}
    static void reset()throws Exception{
        Thread.interrupted();SaveQueue1935.idle=false;GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
        await(()->GpuQualification1961.retainedBytes()==0,"all preceding owners released");
        synchronized(field("LOCK").get(null)){
            ((Map<?,?>)field("RECORDS").get(null)).clear();((Map<?,?>)field("FAILURES").get(null)).clear();
            check(((Map<?,?>)field("SHARED1984").get(null)).isEmpty(),"no model pinned after final owner closes");
            check(((Collection<?>)field("RESERVED1984").get(null)).isEmpty(),"no abandoned copier ticket");
            field("lastLegacy1981").setBoolean(null,false);field("lastPrimary1984").setInt(null,0);
            Arrays.fill((Object[])field("ATTEMPTS1984").get(null),null);field("attemptCursor1984").setInt(null,0);
            field("attemptCount1984").setInt(null,0);field("attemptSequence1984").setLong(null,0);
        }
        GpuNoise1960.busy=false;WholeRoute1953.retained=0;GpuQualification1961.initialize(new Context());
        CameraTrace1965.fail=false;CameraTrace1965.events.clear();
    }
    static void quiet()throws Exception{
        SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();
    }
    static class Probe implements GpuQualification1961.Probe{
        final AtomicInteger runs=new AtomicInteger(),closes=new AtomicInteger();final Task task;
        Probe(){this(null);}Probe(Task task){this.task=task;}
        public void run(GpuQualification1961.Cancellation c){runs.incrementAndGet();if(task!=null)task.run(c);}
        public void close(){closes.incrementAndGet();}
    }
    static String group(String shape){return "strong-gx1978-tuning-policy-bank-v1:3:"+shape;}
    static String cpu(String shape){return "strong-gx1978-cpu-tuning-policy-bank-v1:3:"+shape+":4";}
    static GpuQualification1961.Reservation1984 shared(String shape,long bytes,Object owner,long shared){
        return GpuQualification1961.reserveStrongShared1984(group(shape),cpu(shape),bytes,owner,shared);
    }
    static void submit(GpuQualification1961.Reservation1984 reservation,Probe probe){
        try{check(reservation.accepted()&&reservation.current(),"copy begins only with a current budget ticket");
            check(reservation.commit(probe).reason==GpuQualification1961.QueueDecision1983.SCHEDULED,"ticket transfers to a queued probe");}
        finally{reservation.close();}
    }
    static void observedCapacityRegression()throws Exception{
        reset();Probe[] old=new Probe[7];long total=772*M/10,part=total/7;
        for(int i=0;i<7;i++){old[i]=new Probe();check(GpuQualification1961.schedule("observed-aux-"+i,i==6?total-6*part:part,old[i]),"observed auxiliary reservation");}
        String chain="strong-resident1978:3:v1:sensitive-device-key";long copy=477*M/10;
        check(GpuQualification1961.canQueue1983(chain,copy).reason==14,"published .83 reproduces refusal at seven jobs");
        GpuQualification1961.Reservation1984 ticket=GpuQualification1961.reserve1984(chain,copy);
        check(ticket.accepted()&&ticket.decision.reason==0,"new chain ticket secures space before cloning");
        check(GpuQualification1961.retainedBytes()<=96*M&&GpuQualification1961.retainedBytes()>=copy,"actual held budget includes the future copy and remains within cap");
        int retired=0;for(Probe probe:old){retired+=probe.closes.get();check(probe.runs.get()==0,"no foreground qualification executed");}
        check(retired>0&&retired<7,"only enough lower-priority owners retired");
        long held=GpuQualification1961.retainedBytes();
        check(!GpuQualification1961.canQueue("strong-test:3:cannot-overbook-ticket",96*M),"legacy caller sees copier's actual memory reservation");
        check(GpuQualification1961.retainedBytes()==held,"failed legacy admission cannot retire protected ticket");
        check(GpuQualification1961.canQueue1983(chain,1).reason==13,"same-key legacy admission sees copying reservation");
        Probe chainProbe=new Probe();submit(ticket,chainProbe);
        check(GpuQualification1961.retainedBytes()==held,"commit transfers budget without release/reacquire gap");
        GpuQualification1961.captureChanged();check(chainProbe.closes.get()==1,"committed chain released once on capture cancellation");
        check(GpuQualification1961.retainedBytes()==0,"reclaimed plus remaining owners fully released");
    }
    static final class EqualModel {public boolean equals(Object other){return other instanceof EqualModel;}public int hashCode(){return 1;}}
    static void sharedOwnership()throws Exception{
        reset();Object model=new EqualModel();
        GpuQualification1961.Reservation1984 first=shared("first",10*M,model,60*M),second=shared("second",10*M,model,60*M);
        check(first.accepted()&&second.accepted(),"two exact snapshots share one immutable model");
        check(GpuQualification1961.retainedBytes()==80*M,"same model charged once across copier tickets");
        check(second.decision.requestedBytes==10*M,"second admission reports only incremental ownership");
        GpuQualification1961.Reservation1984 tail=shared("tail",16*M,model,60*M);
        check(tail.accepted()&&GpuQualification1961.retainedBytes()==96*M,"shared ownership respects inclusive 96 MiB boundary");
        check(!shared("overflow",1,model,60*M).accepted(),"shared input never permits cap overflow");
        check(!shared("changed-size",1,model,59*M).accepted(),"same model cannot claim a conflicting immutable size");
        check(!shared("equals-is-not-identity",1,new EqualModel(),60*M).accepted(),"distinct but equal model remains independently charged");
        first.close();check(GpuQualification1961.retainedBytes()==86*M,"one closed copier releases only its exclusive copy");
        Probe p=new Probe();submit(second,p);tail.close();
        check(GpuQualification1961.retainedBytes()==70*M,"model remains charged through queued ownership");
        GpuQualification1961.captureChanged();check(p.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"last queued model owner releases the shared charge exactly once");
    }
    static void cancelledCopierAndSlotBound()throws Exception{
        reset();GpuQualification1961.Reservation1984 ticket=GpuQualification1961.reserve1984("strong-resident1978:3:copy",40*M);
        check(ticket.accepted(),"copier admitted");
        for(int i=0;i<7;i++)check(GpuQualification1961.schedule("strong-test:3:slot-"+i,1,new Probe()),"legacy job coexists with reserved copy slot");
        check(!GpuQualification1961.schedule("strong-test:3:ninth",1,new Probe()),"copy ticket counts toward legacy eight-job limit");
        GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
        check(!ticket.current()&&GpuQualification1961.retainedBytes()==40*M,"capture cancels copier without releasing buffers still being allocated");
        Probe stale=new Probe();check(ticket.commit(stale).reason==6,"old copier cannot transfer ownership to a new capture");
        check(stale.closes.get()==1&&stale.runs.get()==0&&GpuQualification1961.retainedBytes()==0,"cancelled commit closes exactly once and releases held budget");
        ticket.close();check(GpuQualification1961.retainedBytes()==0,"duplicate finally close cannot underflow memory");
    }
    static void concurrentCopiesAndAllocationFailure()throws Exception{
        reset();final Object model=new Object();final CountDownLatch start=new CountDownLatch(1),reserved=new CountDownLatch(2),release=new CountDownLatch(1);
        final AtomicInteger accepted=new AtomicInteger();final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        Thread[] workers=new Thread[2];
        for(int i=0;i<workers.length;i++){final int index=i;workers[i]=new Thread(()->{
            GpuQualification1961.Reservation1984 ticket=null;
            try{start.await();ticket=shared("concurrent-"+index,18*M,model,60*M);if(ticket.accepted())accepted.incrementAndGet();
                reserved.countDown();release.await();
                // A failing copy still owns its complete promised bytes until
                // this caller's finally, independently of the other copier.
                throw new OutOfMemoryError("injected detached copy failure");
            }catch(OutOfMemoryError expected){}catch(Throwable failure){error.compareAndSet(null,failure);}
            finally{if(ticket!=null)ticket.close();}
        });workers[i].start();}
        start.countDown();check(reserved.await(4,TimeUnit.SECONDS),"concurrent copier reservations returned");
        check(error.get()==null&&accepted.get()==2&&GpuQualification1961.retainedBytes()==96*M,"simultaneous shared model reservations atomically reach but never exceed cap");
        check(!shared("concurrent-overflow",1,model,60*M).accepted(),"third caller cannot overbook two in-flight copies");
        release.countDown();for(Thread worker:workers)worker.join(4000);
        check(error.get()==null&&GpuQualification1961.retainedBytes()==0,"both injected copy allocation failures release exactly their own reservations");
        for(Thread worker:workers)check(!worker.isAlive(),"allocation-failure copier completed");
    }
    static void activeOwnerAndFailedReclaim()throws Exception{
        reset();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);Object model=new Object();
        Probe running=new Probe(c->{entered.countDown();for(;;)try{release.await();break;}catch(InterruptedException expected){}});
        submit(shared("running",10*M,model,40*M),running);quiet();check(entered.await(4,TimeUnit.SECONDS),"shared owner became active");
        Probe other=new Probe();check(GpuQualification1961.schedule("small-independent",8*M,other),"queued lower-priority copy");
        long before=GpuQualification1961.retainedBytes();
        GpuQualification1961.Reservation1984 impossible=GpuQualification1961.reserve1984("strong-resident1978:3:too-large",80*M);
        check(!impossible.accepted()&&other.closes.get()==0&&GpuQualification1961.retainedBytes()==before,"failed full admission simulation cannot discard another useful snapshot");
        GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
        check(running.closes.get()==0&&GpuQualification1961.retainedBytes()==50*M,"running shared input survives cancellation until worker joins");
        release.countDown();await(()->running.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"active shared owner finally released");
    }
    static void fairnessAndCompanionIsolation()throws Exception{
        reset();List<String> order=Collections.synchronizedList(new ArrayList<String>());Object model=new Object();
        Probe aux=new Probe(c->order.add("aux"));check(GpuQualification1961.schedule("first-aux",1,aux),"auxiliary arrives first");
        submit(GpuQualification1961.reserve1984("strong-resident1978:3:chain-one",1),new Probe(c->order.add("chain1")));
        submit(shared("strong-one",1,model,1),new Probe(c->order.add("strong1")));
        submit(shared("strong-two",1,model,1),new Probe(c->order.add("strong2")));
        submit(GpuQualification1961.reserve1984("strong-resident1978:3:chain-two",1),new Probe(c->order.add("chain2")));
        quiet();await(()->GpuQualification1961.retainedBytes()==0,"primary and auxiliary jobs finish");
        check(order.equals(Arrays.asList("strong1","chain1","strong2","chain2","aux")),"Strong and chain alternate FIFO ahead of auxiliary work");
        reset();String shape="separate-speed-gate";GpuQualification1961.rejectSpeed(cpu(shape));
        check(!GpuQualification1961.maySchedule(cpu(shape)),"CPU fallback speed gate is actually cooling down");
        GpuQualification1961.Reservation1984 ticket=shared(shape,10,new Object(),10);
        check(ticket.accepted(),"CPU companion cooldown does not strand independent Strong exact recovery");ticket.close();
        GpuQualification1961.rejectExact(group(shape));check(!shared(shape,10,new Object(),10).accepted(),"independent recovery never bypasses main exact mismatch");
    }
    static void commitRacesAndCertificateGates()throws Exception{
        reset();String key="strong-resident1978:3:race";
        GpuQualification1961.Reservation1984 ticket=GpuQualification1961.reserve1984(key,1024);
        GpuQualification1961.rejectExact(key);Probe p=new Probe();
        check(ticket.commit(p).reason==8&&p.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"exact rejection arriving during copy defeats stale ticket");
        ticket.close();GpuQualification1961.qualified(key,100,80,0);check(GpuQualification1961.restore(key)==null,"known mismatch remains authoritative");
        String second="strong-resident1978:3:qualified-race";ticket=GpuQualification1961.reserve1984(second,1024);
        GpuQualification1961.qualified(second,100,96,0);check(GpuQualification1961.restore(second)==null,"ticket does not weaken five-percent speed requirement");
        GpuQualification1961.qualified(second,100,95,0);p=new Probe();
        check(ticket.commit(p).reason==11&&p.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"completed certificate arriving during copy prevents redundant proof");
    }
    static String execute(String key,Task task)throws Exception{
        Probe p=new Probe(task);check(GpuQualification1961.schedule(key,1,p),"diagnostic exercise admitted");quiet();
        await(()->p.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"diagnostic exercise ended");
        check(p.runs.get()==1,"job runs exactly once");return GpuQualification1961.attemptSummary1984();
    }
    static void diagnosticTruthAndIsolation()throws Exception{
        reset();String falseSuccess=execute("private-key-never-in-log",c->{GpuQualification1961.progress1984("strong_profile",1,24);
            GpuQualification1961.outcome1984("strong_qualified");GpuQualification1961.timings1984(100000000L,90000000L);});
        check(falseSuccess.contains("終了理由の詳細なし")&&!falseSuccess.contains("認定記録を作成"),"caller success label cannot manufacture certificate success");
        check(falseSuccess.contains("Strong候補の比較 1/24")&&falseSuccess.contains("100ms・GPU 90ms"),"last partial progress and measured timing retained");
        check(!falseSuccess.contains("private-key")&&!CameraTrace1965.events.toString().contains("private-key"),"key and input identity excluded from UI and rolling trace");
        reset();String success=execute("actual-record",c->{GpuQualification1961.progress1984("resident_compare",2,2);GpuQualification1961.qualified("actual-record",100,95,0);});
        check(success.contains("認定記録を作成")&&GpuQualification1961.restore("actual-record")!=null,"only committed record yields automatic successful terminal state");
        reset();String memory=execute("allocation-error",c->{throw new OutOfMemoryError("injected");});check(memory.contains("検証用メモリ不足"),"uncaught allocation exit gets a concrete cause");
        reset();String error=execute("runtime-error",c->{throw new IllegalStateException("injected");});check(error.contains("検証中の例外で終了"),"uncaught runtime exit is not silently completed");
        reset();CameraTrace1965.fail=true;
        String resilient=execute("trace-failure",c->{GpuQualification1961.progress1984("resident_compare",2,2);GpuQualification1961.outcome1984("unknown-sensitive-string");
            GpuQualification1961.qualified("trace-failure",100,90,0);});
        check(resilient.contains("認定記録を作成")&&GpuQualification1961.restore("trace-failure")!=null,"diagnostic writer exceptions do not alter qualification or release");
        for(Class<?> type:GpuQualification1961.class.getDeclaredClasses())if(type.getSimpleName().equals("Attempt1984"))
            for(Field f:type.getDeclaredFields())check(f.getType().isPrimitive(),"attempt history retains scalars only: "+f.getName());
        String before=GpuQualification1961.status1967(),history=GpuQualification1961.attemptSummary1984();
        for(int i=0;i<4;i++)check(history.equals(GpuQualification1961.attemptSummary1984())&&before.equals(GpuQualification1961.status1967()),"reading history cannot mutate queue or persisted certificate state");
    }
    public static void main(String[] args)throws Exception{
        observedCapacityRegression();sharedOwnership();cancelledCopierAndSlotBound();concurrentCopiesAndAllocationFailure();activeOwnerAndFailedReclaim();
        fairnessAndCompanionIsolation();commitRacesAndCertificateGates();diagnosticTruthAndIsolation();reset();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"qualification_ticket_budget1984\":true,\"qualification_shared_model_ownership1984\":true,\"qualification_priority_fairness1984\":true,\"qualification_terminal_progress1984\":true,\"qualification_legacy_gates_preserved1984\":true}");
    }
}
