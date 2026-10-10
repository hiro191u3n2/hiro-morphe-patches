package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.*;
import java.util.concurrent.*;

/** Real production qualification queue; controlled work duration tests admission
 * only. Numerical kernels are independently exercised by the .77 differential. */
public final class CpuExactGate1978Test {
    static int checks,factories;static Work latest;
    static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
    static String key(int tag){return "cpu-single1978-gate-test:"+tag;}
    static final class Work implements CpuExact1978.Work {
        final int mode;int oldCalls,newCalls,closes;volatile boolean closed;CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        Work(int mode){this.mode=mode;}
        public Object run(boolean reused) {
            if(reused)newCalls++;else oldCalls++;
            if(mode==5){entered.countDown();try{release.await();}catch(InterruptedException cancelled){Thread.currentThread().interrupt();return null;}}
            if(mode==4&&reused)return null;
            if(mode==6)throw new OutOfMemoryError("controlled optional scratch failure");
            try{Thread.sleep((mode==1?reused:!reused)?12:1);}catch(InterruptedException cancelled){Thread.currentThread().interrupt();return null;}
            int[] pixels=new int[97];float[] model=new float[31];for(int i=0;i<pixels.length;i++)pixels[i]=0xff121314+i;
            model[30]=mode==3?(reused?newCalls:oldCalls):0f;
            if(mode==2&&reused)model[30]=Float.intBitsToFloat(0x80000000);
            return new Object[]{pixels,model,new int[]{17,19,256}};
        }
        public void close(){closes++;closed=true;}
    }
    static void offer(int tag,final int mode,long bytes){CpuExact1978.offer(key(tag),bytes,1048576,new CpuExact1978.Factory(){public CpuExact1978.Work create(){factories++;return latest=new Work(mode);}});}
    static void age()throws Exception{SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
    static void drain()throws Exception{long stop=System.nanoTime()+10000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>stop)throw new AssertionError("undrained CPU proof");Thread.sleep(3);}check(true,"proof ownership drained");}
    static void clean()throws Exception{SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();if(latest!=null)latest.release.countDown();drain();GpuNoise1960.budget=true;latest=null;factories=0;}
    public static void main(String[] args)throws Exception {
        GpuQualification1961.initialize(new Context());clean();
        check(!CpuExact1978.enabled(key(0)),"unknown preserves original");offer(0,0,4096);Work w=latest;check(w!=null,"candidate snapshot created");offer(0,0,4096);check(factories==1,"same key has one detached owner");Thread.sleep(25);check(w.oldCalls+w.newCalls==0,"no benchmark before save idle");age();drain();check(w.oldCalls==3&&w.newCalls==3,"warmup plus two independent complete pairs");check(CpuExact1978.enabled(key(0)),"stable exact faster candidate admitted");check(w.closed&&w.closes==1,"accepted owner closed once");
        clean();offer(1,1,4096);w=latest;age();drain();check(!CpuExact1978.enabled(key(1))&&!GpuQualification1961.exactRejected(key(1)),"slower exact candidate remains original without exact rejection");check(w.closed,"slow proof released");
        clean();offer(2,2,4096);age();drain();check(GpuQualification1961.exactRejected(key(2))&&!CpuExact1978.enabled(key(2)),"last model float signed-zero raw bit mismatch rejected");
        clean();offer(3,3,4096);age();drain();check(GpuQualification1961.exactRejected(key(3)),"unstable original across full pairs rejected");
        clean();offer(4,4,4096);age();drain();check(!CpuExact1978.enabled(key(4))&&!GpuQualification1961.exactRejected(key(4)),"unavailable optional workspace is not an exact mismatch");
        clean();GpuNoise1960.budget=false;offer(5,0,4096);check(factories==0&&latest==null,"memory rejection before copying");GpuNoise1960.budget=true;offer(5,0,4096);w=latest;GpuNoise1960.budget=false;age();drain();check(w.oldCalls+w.newCalls==0&&w.closed&&!CpuExact1978.enabled(key(5)),"memory rechecked before background work");
        clean();offer(6,5,4096);w=latest;age();check(w.entered.await(5,TimeUnit.SECONDS),"active proof entered");ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();w.release.countDown();drain();check(w.closed&&!CpuExact1978.enabled(key(6))&&!GpuQualification1961.exactRejected(key(6)),"new capture cancels and drains without poisoning certificate");
        clean();offer(7,6,4096);w=latest;age();drain();check(w.closed&&!CpuExact1978.enabled(key(7)),"allocation failure closes private proof");
        clean();offer(8,0,96L*1024*1024+1);check(factories==0,"retained snapshot cap enforced");Thread.currentThread().interrupt();try{offer(9,0,4096);check(factories==0,"interrupted caller cannot enqueue");}finally{Thread.interrupted();}
        check(CpuExact1978.same(new float[]{Float.intBitsToFloat(0x7fc00001)},new float[]{Float.intBitsToFloat(0x7fc00001)}),"equal NaN payload retained");check(!CpuExact1978.same(new float[]{Float.intBitsToFloat(0x7fc00001)},new float[]{Float.intBitsToFloat(0x7fc00002)}),"NaN payload inequality retained");
        clean();System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"cpu78_idle_exact2_speed_gate_verified\":true,\"cpu78_probe_cancel_memory_ownership_verified\":true,\"cpu78_original_instability_and_floatbits_rejected\":true}");
    }
}
