package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real queue admission under the photographed 91/96 MiB pressure. No GPU
 * readiness, pixel equality or speed certificate is invented by this test. */
public final class QueueModel1994Test {
 static int checks;static final long M=1024L*1024;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static void reset()throws Exception{SaveQueue1935.idle=false;GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
  long deadline=System.nanoTime()+3000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>deadline)throw new AssertionError("owner drain");Thread.sleep(2);}
  synchronized(field("LOCK").get(null)){((Map<?,?>)field("DEFERRED1991").get(null)).clear();field("consecutivePrimary1991").setInt(null,0);field("lastPrimary1984").setInt(null,0);field("lastLegacy1981").setBoolean(null,false);}
  GpuQualification1961.initialize(new Context());
 }
 static final class Probe implements GpuQualification1961.Probe {
  final AtomicInteger closes=new AtomicInteger(),runs=new AtomicInteger();
  public void run(GpuQualification1961.Cancellation c){runs.incrementAndGet();}
  public void close(){closes.incrementAndGet();}
 }
 static Probe queue(String key,long bytes){Probe value=new Probe();check(GpuQualification1961.schedule(key,bytes,value),"queued "+key);return value;}
 static Probe owned(String key,long bytes){Probe value=new Probe();GpuQualification1961.Reservation1984 r=GpuQualification1961.reserve1985(key,bytes);
  try{check(r.accepted()&&r.begin1985(),"reserved before copy "+key);check(r.commit(value).accepted,"ticket commits "+key);}finally{r.close();}return value;}
 public static void main(String[] args)throws Exception {
  final String region="regions1961:v2:3060:4080",cpu="cpu-model1978-region-exact2-time5-v1:3060:4080";
  reset();Probe chain=owned("strong-resident94:3:chain",48*M);Probe largest=queue("cpu-colour1976-exact2-time5-v1:large",15*M);
  Probe[] kept={queue("cpu-small-a",10*M),queue("cpu-small-b",8*M),queue("cpu-small-c",6*M),queue("cpu-small-d",4*M)};
  check(GpuQualification1961.retainedBytes()==91*M,"six jobs reproduce 91MiB retained pressure");
  GpuQualification1961.Reservation1984 model=GpuQualification1961.reserve1985(region,12*M);
  check(model.accepted(),"12MiB region model no longer starved by lower-value cache");
  check(largest.closes.get()==1&&largest.runs.get()==0,"only queued largest lower-rank copy retired");
  check(chain.closes.get()==0,"chain proof retained");for(Probe p:kept)check(p.closes.get()==0,"unneeded retirements avoided");
  check(GpuQualification1961.retainedBytes()==88*M,"actual retained budget updated before region copy");model.close();reset();
  chain=owned("strong-resident94:3:slot-chain",M);Probe[] slots=new Probe[6];for(int i=0;i<6;i++)slots[i]=queue("slot-aux-"+i,1);
  model=GpuQualification1961.reserve1985(region,M);check(model.accepted(),"region reclaims auxiliary slot before early slot refusal");
  int closed=0;for(Probe p:slots)closed+=p.closes.get();check(closed==1&&chain.closes.get()==0,"one slot reclaimed without closing primary");model.close();reset();
  GpuQualification1961.Reservation1984 busy=GpuQualification1961.reserve1985("live-copy",85*M);check(busy.accepted()&&busy.begin1985(),"live copy owns85MiB");
  Probe small=queue("cpu-finish1976-exact2-time5-v1:small",2*M);
  check(!GpuQualification1961.canQueue(region,12*M),"region declines if even complete safe retirement cannot fit");
  check(small.closes.get()==0&&busy.current()&&GpuQualification1961.retainedBytes()==87*M,"failed admission is atomic and leaves live copy intact");busy.close();reset();
  model=GpuQualification1961.reserve1985(region,12*M);check(model.accepted(),"region reserves before original CPU callback");
  check(GpuQualification1961.canQueue1983(cpu,12*M).reason==23,"same CPU region input not copied twice during GPU reservation");
  check(GpuQualification1961.canQueue("cpu-model1978-region-exact2-time5-v1:4080:3060",1),"different dimensions not conflated");
  Probe regionProbe=new Probe();check(model.begin1985()&&model.commit(regionProbe).accepted,"GPU model snapshot ownership transfers");model.close();
  check(GpuQualification1961.canQueue1983(cpu,12*M).reason==23,"queued model also suppresses duplicate CPU snapshot");
  check(!GpuQualification1961.exactRejected(cpu),"suppression never becomes pixel rejection");reset();
  check(GpuQualification1961.canQueue(cpu,12*M),"capture change restores CPU proof opportunity");
  GpuQualification1961.rejectExact(region);check(!GpuQualification1961.reserve1985(region,12*M).accepted(),"priority never bypasses exact rejection");
  check(!GpuQualification1961.canQueue("regions1961:v2:too-large",96*M+1),"unchanged96MiB limit");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"actual_qualification_queue\":true,\"duplicate_region_snapshot_avoided\":true,\"physical_android_tested\":false}");
 }
}
