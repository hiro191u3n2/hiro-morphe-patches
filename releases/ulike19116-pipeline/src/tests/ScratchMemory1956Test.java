package com.hiro.ulike;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ScratchMemory1956Test {
    private static int assertions;
    private static void check(boolean condition,String message) {
        assertions++;if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception {
        check(SpeedWorkers1935.nativeRetainedBytes1956()==0,"absent native owner has no retained allocation");
        final AtomicInteger trims=new AtomicInteger();
        final CountDownLatch otherPoolUser=new CountDownLatch(1);
        SpeedWorkers1935.installScratchMemory1956(new SpeedWorkers1935.ScratchMemory(){
            public long retainedBytes(){return 32L*1024*1024;}
            public void trim(){
                trims.incrementAndGet();
                Thread reader=new Thread(new Runnable(){public void run(){SpeedWorkers1935.retainedBytes();otherPoolUser.countDown();}});
                reader.setDaemon(true);reader.start();
                try{if(!otherPoolUser.await(2,TimeUnit.SECONDS))throw new AssertionError("native trim retained Java pool lock");}
                catch(InterruptedException e){throw new AssertionError(e);}
            }
        });
        check(SpeedWorkers1935.nativeRetainedBytes1956()==33554432L,"native allocation is externally accounted");
        int[] scratch=SpeedWorkers1935.borrowInts(4096);
        SpeedWorkers1935.release(scratch);
        check(SpeedWorkers1935.retainedBytes()>=16384,"real Java pool can retain an owned array");
        SpeedWorkers1935.trim();
        check(trims.get()==1,"native trim is forwarded once");
        check(otherPoolUser.getCount()==0,"other pool owner progresses during native trim");
        check(SpeedWorkers1935.retainedBytes()==0,"Java retained allocation is released");
        SpeedWorkers1935.installScratchMemory1956(new SpeedWorkers1935.ScratchMemory(){public long retainedBytes(){return -1;}public void trim(){}});
        check(SpeedWorkers1935.nativeRetainedBytes1956()==Long.MAX_VALUE,"invalid accounting blocks admission conservatively");
        SpeedWorkers1935.release(new int[16]);
        check(SpeedWorkers1935.retainedBytes()==0,"unknown native budget prevents Java cache retention");
        SpeedWorkers1935.installScratchMemory1956(new SpeedWorkers1935.ScratchMemory(){public long retainedBytes(){throw new UnsatisfiedLinkError("test");}public void trim(){throw new UnsatisfiedLinkError("test");}});
        check(SpeedWorkers1935.nativeRetainedBytes1956()==Long.MAX_VALUE,"link failure cannot report false free memory");
        SpeedWorkers1935.trim();
        SpeedWorkers1935.installScratchMemory1956(null);
        check(SpeedWorkers1935.nativeRetainedBytes1956()==0,"optional owner removal is reflected");
        System.out.println("PASS ScratchMemory1956 assertions="+assertions);
    }
}
