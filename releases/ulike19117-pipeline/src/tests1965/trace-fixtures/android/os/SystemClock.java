package android.os;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
public final class SystemClock {
 public static volatile long offset;
 public static volatile boolean bootCleanupFault;
 public static volatile CountDownLatch closeEntered=new CountDownLatch(1),closeRelease=new CountDownLatch(1);
 public static final AtomicInteger faultCalls=new AtomicInteger();
 public static void armBootCleanupFault(){faultCalls.set(0);closeEntered=new CountDownLatch(1);closeRelease=new CountDownLatch(1);bootCleanupFault=true;}
 public static long elapsedRealtime(){return System.nanoTime()/1000000L+offset;}
 public static long uptimeMillis(){
  if(bootCleanupFault&&Thread.currentThread().getName().equals("ULike-camera-trace1965")){
   int call=faultCalls.incrementAndGet();
   if(call==2)throw new IllegalStateException("scripted boot-record clock failure");
   if(call==3){closeEntered.countDown();try{if(!closeRelease.await(8,TimeUnit.SECONDS))throw new IllegalStateException("test cleanup barrier timed out");}catch(InterruptedException e){throw new IllegalStateException(e);}}
  }
  return elapsedRealtime();
 }
 public static long elapsedRealtimeNanos(){return System.nanoTime()+offset*1000000L;}
}
