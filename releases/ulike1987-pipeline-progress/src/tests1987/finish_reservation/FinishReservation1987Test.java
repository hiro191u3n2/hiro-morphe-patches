package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Real Finish producer, Copy budget and qualification queue. The Android
 * Bitmap fixture exposes allocation boundaries; no renderer or queue method
 * is replaced. Saving stays busy, so these tests do not execute a GPU proof. */
public final class FinishReservation1987Test {
    static final long M=1024L*1024;
    static final int WIDTH=512,HEIGHT=512;
    static int assertions;static boolean baseline;
    static final Map<String,Integer> cases=new LinkedHashMap<String,Integer>();
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void section(String name,int start){cases.put(name,assertions-start);}
    static Field field(String name)throws Exception{return QualificationProgress1984Test.field(name);}
    static Field member(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
    static void reset()throws Exception{
        Bitmap.beforeCopy1987=Bitmap.afterCopy1987=null;Bitmap.copyFault1987=0;
        QualificationProgress1984Test.reset();
        for(Field f:GpuSnapshotBudget1981.class.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())&&!Modifier.isFinal(f.getModifiers())){
            f.setAccessible(true);Class<?> type=f.getType();
            if(type==long.class)f.setLong(null,f.getName().equals("epoch")?Long.MIN_VALUE:0L);
            else if(type==int.class)f.setInt(null,0);else if(type==boolean.class)f.setBoolean(null,false);else f.set(null,null);
        }
        for(Bitmap b:Bitmap.ALL)if(!b.isRecycled())b.recycle();Bitmap.ALL.clear();
        Bitmap.copyCalls1987=0;GpuNoise1960.fits1987=true;
    }
    static Bitmap source(){
        int[] pixels=new int[WIDTH*HEIGHT];for(int i=0;i<pixels.length;i++)pixels[i]=0xff000000|((i*977+31)&0xffffff);
        return Bitmap.from(WIDTH,HEIGHT,pixels,Bitmap.Config.ARGB_8888,true);
    }
    static QualityPixels1932.Plan plan(){return QualityPixels1932.plan(null,800,16666666L,0,0,4,4,true,true,1.2f).withHaloSuppression(true);}
    static String key(Bitmap b)throws Exception{
        Method method=GpuChain1961.class.getDeclaredMethod("finishKey1976",Bitmap.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);
        method.setAccessible(true);return (String)method.invoke(null,b,0,WIDTH,HEIGHT,plan(),true);
    }
    static Bitmap invoke(Bitmap b){return GpuChain1961.finish(b,0,WIDTH,HEIGHT,plan(),true,null);}
    static Object queued(String key)throws Exception{
        synchronized(field("LOCK").get(null)){for(Object job:(Collection<?>)field("QUEUED").get(null))if(key.equals(member(job,"key").get(job)))return job;return null;}
    }
    static int reservations()throws Exception{synchronized(field("LOCK").get(null)){return ((Collection<?>)field("RESERVED1984").get(null)).size();}}
    static void noCopyOwner()throws Exception{
        Field copying=GpuSnapshotBudget1981.class.getDeclaredField("copying");copying.setAccessible(true);
        check(!copying.getBoolean(null),"the finished producer releases its actual copy owner");
    }
    static void sourceIntact(Bitmap b,int[] expected){check(!b.isRecycled()&&b.recycleCalls1987==0&&Arrays.equals(expected,b.snapshot()),"Finish qualification leaves caller pixels and lifetime unchanged");}
    static void clean(Bitmap b,int[] expected)throws Exception{
        Bitmap.beforeCopy1987=Bitmap.afterCopy1987=null;
        GpuQualification1961.captureChanged();
        check(GpuQualification1961.retainedBytes()==0&&reservations()==0,"all snapshot bytes and job reservations are released");
        check(((Collection<?>)field("QUEUED").get(null)).isEmpty(),"no old Finish probe remains queued");
        for(Bitmap image:Bitmap.ALL)if(image!=b)check(image.isRecycled()&&image.recycleCalls1987==1,"each detached bitmap is recycled exactly once");
        sourceIntact(b,expected);noCopyOwner();b.recycle();
    }
    static void reservedSlot()throws Exception{
        reset();int start=assertions;Bitmap b=source();int[] expected=b.snapshot();String key=key(b);
        for(int i=0;i<7;i++)check(GpuQualification1961.schedule("finish87-aux-slot-"+i,1,new QualificationProgress1984Test.Probe()),"seven auxiliary jobs occupy ordinary queue slots");
        check(invoke(b)==null,"a queue change never adopts an uncertified foreground GPU result");
        Object job=queued(key);
        check((job==null)==baseline,"Finish gets its intended primary slot only with atomic reservation");
        check(Bitmap.copyCalls1987==(baseline?0:1),"only an admitted Finish request copies its source");
        if(!baseline){check(member(job,"priority1984").getInt(job)==1,"the actual Finish job retains primary priority one");
            check(GpuQualification1961.retainedBytes()==M+7,"the queue owns exact future-copy bytes and existing jobs");}
        check(reservations()==0,"successful commit transfers the ticket into one queued job");
        clean(b,expected);section("reserved_primary_slot",start);
    }
    static void beforeAllocation()throws Exception{
        reset();int start=assertions;final Bitmap b=source();int[] expected=b.snapshot();final String key=key(b);
        final long[] held={-1};final int[] tickets={-1};
        Bitmap.beforeCopy1987=()->{try{held[0]=GpuQualification1961.retainedBytes();tickets[0]=reservations();
            check(queued(key)==null,"the private source has not become a queued job before allocation");
        }catch(Exception failure){throw new AssertionError(failure);}};
        check(invoke(b)==null,"uncertified foreground output stays with the established fallback");
        check(held[0]==(baseline?0:M)&&tickets[0]==(baseline?0:1),"real bytes and slot are held at the actual Bitmap.copy boundary");
        Object job=queued(key);check(job!=null,"Finish queues one immutable source when capacity is available");
        check(member(job,"priority1984").getInt(job)==(baseline?0:1),"producer carries primary priority into the real queue job");
        check(Bitmap.copyCalls1987==1&&GpuQualification1961.retainedBytes()==M,"commit makes one copy with one byte charge");
        check(reservations()==0,"the materialized ticket transfers without a second reservation");
        clean(b,expected);section("bytes_and_slot_before_allocation",start);
    }
    static void capacityRace()throws Exception{
        reset();int start=assertions;final Bitmap b=source();int[] expected=b.snapshot();final String key=key(b);
        final GpuQualification1961.Reservation1984[] competitor={null};
        Bitmap.beforeCopy1987=()->{competitor[0]=GpuQualification1961.reserve1985("strong-resident1978:3:finish87-race",96*M);};
        invoke(b);
        check(competitor[0]!=null&&competitor[0].accepted()==baseline,"another copier cannot consume bytes already reserved by Finish");
        check((queued(key)!=null)!=baseline,"concurrent capacity admission cannot discard an already reserved Finish snapshot");
        check(Bitmap.copyCalls1987==1,"capacity race makes no duplicate snapshot");
        check(GpuQualification1961.retainedBytes()==(baseline?96*M:M),"real retained accounting remains at or below 96 MiB");
        competitor[0].close();clean(b,expected);section("capacity_race_without_reacquisition",start);
    }
    static void primaryFairness()throws Exception{
        reset();int start=assertions;Bitmap b=source();int[] expected=b.snapshot();String key=key(b);
        invoke(b);check(queued(key)!=null,"Finish is queued before a competing Strong request");
        for(int i=0;i<6;i++)check(GpuQualification1961.schedule("finish87-aux-bytes-"+i,M,new QualificationProgress1984Test.Probe()),"six auxiliary snapshots fill remaining ordinary slots");
        field("lastPrimary1984").setInt(null,2);
        GpuQualification1961.Reservation1984 strong=GpuQualification1961.reserve1985(QualificationProgress1984Test.group("finish87-priority"),95*M);
        check(strong.accepted(),"late Strong can reclaim only the necessary lower-priority ownership");
        check((queued(key)==null)==baseline,"when finishing owns the fair turn, auxiliary retirement preserves Finish");
        check(GpuQualification1961.retainedBytes()==96*M,"reclamation and reservation keep the exact unchanged byte cap");
        QualificationProgress1984Test.Probe probe=new QualificationProgress1984Test.Probe();
        check(strong.begin1985()&&strong.commit(probe).accepted,"a competing primary proof enters through its same guarded handoff");strong.close();
        Method next=GpuQualification1961.class.getDeclaredMethod("nextJob1984");next.setAccessible(true);
        synchronized(field("LOCK").get(null)){
            Object selected=next.invoke(null);check(selected!=null,"the scheduler has a queued primary candidate");
            check(key.equals(member(selected,"key").get(selected))!=baseline,"real scheduler dispatches Finish first on its primary turn");
        }
        clean(b,expected);check(probe.closes.get()==1&&probe.runs.get()==0,"cancelled competitor releases without executing foreground proof work");
        section("actual_primary_fairness_and_retirement",start);
    }
    static void captureCancellation()throws Exception{
        int start=assertions;
        for(int moment=0;moment<2;moment++){
            reset();final Bitmap b=source();int[] expected=b.snapshot();String key=key(b);
            Runnable cancel=()->{try{
                GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
                check(GpuQualification1961.retainedBytes()==(baseline?0:M),"a started old Finish copy remains charged until its owner exits");
                check(GpuSnapshotBudget1981.tryCopy(1,0)==null,"a new capture cannot overlap a started old bitmap allocation");
            }catch(Exception failure){throw new AssertionError(failure);}};
            if(moment==0)Bitmap.beforeCopy1987=cancel;else Bitmap.afterCopy1987=cancel;
            invoke(b);check(queued(key)==null,"an obsolete source never commits into the new capture");
            check(Bitmap.copyCalls1987==1,"capture interruption consumes only the already-started copy");
            clean(b,expected);
        }
        section("capture_epoch_and_started_owner_lifetime",start);
    }
    static void proofArrivals()throws Exception{
        int start=assertions;
        for(int mode=0;mode<2;mode++){
            reset();final Bitmap b=source();int[] expected=b.snapshot();final String key=key(b);final int action=mode;
            Bitmap.afterCopy1987=()->{if(action==0)GpuQualification1961.rejectExact(key);else GpuQualification1961.qualified(key,100,80,0);};
            invoke(b);check(queued(key)==null,"exact rejection or an independent proof remains authoritative at commit");
            check(Bitmap.copyCalls1987==1&&GpuQualification1961.retainedBytes()==0,"commit refusal closes the copied image and reserved bytes");
            if(mode==0)check(GpuQualification1961.exactRejected(key)&&GpuQualification1961.restore(key)==null,"queue optimization cannot bypass an exact mismatch");
            else check(GpuQualification1961.restore(key)!=null,"queue optimization preserves the independently established certificate");
            clean(b,expected);
        }
        section("authoritative_proof_arrivals",start);
    }
    static void copyFailures()throws Exception{
        int start=assertions;
        for(int fault=1;fault<=5;fault++){
            reset();Bitmap b=source();int[] expected=b.snapshot();String key=key(b);Bitmap.copyFault1987=fault;
            boolean fatal=false;try{invoke(b);}catch(AssertionError expectedFatal){fatal=true;}
            check(fatal==(fault==5),"fatal allocation errors retain their existing propagation contract");
            check(Bitmap.copyCalls1987==1&&queued(key)==null,"failed allocation queues no nonexistent source");
            check(GpuQualification1961.retainedBytes()==0&&reservations()==0,"every handled or fatal allocation exit releases the reservation");
            Bitmap.copyFault1987=0;invoke(b);
            check(Bitmap.copyCalls1987==1,"failed allocation still spends this capture's one-image allowance");
            clean(b,expected);
        }
        section("allocation_failure_and_single_copy_cap",start);
    }
    static void refusedBeforeCopy()throws Exception{
        int start=assertions;
        for(int mode=0;mode<3;mode++){
            reset();Bitmap b=source();int[] expected=b.snapshot();String key=key(b);GpuQualification1961.Reservation1984 held=null;
            if(mode==0)held=GpuQualification1961.reserve1985("strong-resident1978:3:finish87-full",96*M);
            if(mode==1)GpuNoise1960.fits1987=false;
            if(mode==2)GpuQualification1961.rejectSpeed(key);
            invoke(b);check(Bitmap.copyCalls1987==0&&queued(key)==null,"closed capacity, workspace or cooldown gates allocate no Finish snapshot");
            if(held!=null)held.close();clean(b,expected);
        }
        section("preallocation_capacity_workspace_and_retry_gates",start);
    }
    public static void main(String[] args)throws Exception{
        baseline=args.length>0&&args[0].equals("published86");
        reservedSlot();beforeAllocation();capacityRace();primaryFairness();captureCancellation();proofArrivals();copyFailures();refusedBeforeCopy();
        StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions)
            .append(",\"physical_android_tested\":false,\"device_speedup_verified\":false,\"actual_finish_producer_executed1987\":true,\"actual_qualification_queue_executed1987\":true,\"cases\":{");
        boolean comma=false;for(Map.Entry<String,Integer> entry:cases.entrySet()){if(comma)out.append(',');comma=true;out.append('"').append(entry.getKey()).append("\":").append(entry.getValue());}
        out.append("},\"finish_published86_priority_loss_reproduced1987\":").append(baseline)
            .append(",\"finish_primary_reservation_handoff_verified1987\":").append(!baseline).append('}');
        System.out.println(out);
    }
}
