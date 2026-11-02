package com.hiro.ulike;
import java.util.Arrays;
import java.util.concurrent.*;
/** Host-only decoration after a successful real native read. Never compiled
 * into the patch. Corrupts partial private output to prove full CPU recovery. */
final class GpuTransportFault1995 {
    static volatile int mode,fired;static volatile boolean cancelled;
    static volatile CountDownLatch both,release;
    static void arm(int value){mode=value;fired=0;cancelled=false;}
    static void clear(){mode=0;cancelled=false;}
    static void before(){CountDownLatch b=both,r=release;if(b==null)return;b.countDown();try{if(!r.await(15,TimeUnit.SECONDS))throw new AssertionError("two-bank read barrier timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
    static boolean after(boolean ok,int[] counts,int[][] targets,int[] offsets){
        int fault=mode;if(!ok||fault==0)return ok;mode=0;fired++;
        // Every mode starts after the real read has completed, and damages a
        // prefix of the private core (including transparent/budget-zero pixels).
        Arrays.fill(targets[0],offsets[0],offsets[0]+Math.max(1,counts[0]/2),0x11223344);
        if(fault==1)return false;
        if(fault==2)throw new IllegalStateException("controlled exception after real read");
        if(fault==3)throw new OutOfMemoryError("controlled allocation failure after real read");
        if(fault==4){StrongTransport1995Test.preferences.clear();return true;}
        if(fault==5){cancelled=true;return true;}
        throw new AssertionError("unknown fault");
    }
}
