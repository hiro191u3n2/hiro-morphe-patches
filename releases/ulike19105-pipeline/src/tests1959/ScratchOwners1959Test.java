package com.hiro.ulike;

import java.util.concurrent.atomic.AtomicInteger;

/** Each invocation gets a fresh JVM. Both registration orders and failed
 * queries/trim are exercised against actual worker memory admission. */
public final class ScratchOwners1959Test {
    private static int assertions;
    private static void check(boolean p,String why){assertions++;if(!p)throw new AssertionError(why);}
    private static final class Owner implements SpeedWorkers1935.ScratchMemory{
        long bytes;int failure;boolean badTrim;final AtomicInteger trimmed=new AtomicInteger();
        Owner(long n){bytes=n;}public long retainedBytes(){if(failure==1)throw new IllegalStateException("query");if(failure==2)throw new UnsatisfiedLinkError("query");if(failure==3)throw new OutOfMemoryError("query");return bytes;}
        public void trim(){trimmed.incrementAndGet();if(badTrim)throw new IllegalStateException("trim");}
    }
    public static void main(String[] args){Owner old=new Owner(111),smooth=new Owner(222);
        if(args[0].equals("reverse")){SpeedWorkers1935.installScratchMemory1959(smooth);SpeedWorkers1935.installScratchMemory1956(old);}else{SpeedWorkers1935.installScratchMemory1956(old);SpeedWorkers1935.installScratchMemory1959(smooth);}
        check(SpeedWorkers1935.nativeRetainedBytes1956()==333,"both owner registrations survive");
        old.bytes=Long.MAX_VALUE-10;check(SpeedWorkers1935.nativeRetainedBytes1956()==Long.MAX_VALUE,"native memory overflow saturates");
        old.bytes=-1;check(SpeedWorkers1935.nativeRetainedBytes1956()==Long.MAX_VALUE,"invalid native count closes admission");
        old.bytes=111;for(int failure=1;failure<=3;failure++){smooth.failure=failure;check(SpeedWorkers1935.nativeRetainedBytes1956()==Long.MAX_VALUE,"failed query closes admission");}smooth.failure=0;
        for(int owner=0;owner<2;owner++){old.badTrim=owner==0;smooth.badTrim=owner==1;int a=old.trimmed.get(),b=smooth.trimmed.get();SpeedWorkers1935.trim();check(old.trimmed.get()==a+1&&smooth.trimmed.get()==b+1,"failed trim still trims other owner");}
        SpeedWorkers1935.installScratchMemory1956(null);check(SpeedWorkers1935.nativeRetainedBytes1956()==222,"clear one owner preserves other");SpeedWorkers1935.installScratchMemory1959(null);check(SpeedWorkers1935.nativeRetainedBytes1956()==0,"owners clear independently");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"registrationOrder\":\""+args[0]+"\"}");
    }
}
