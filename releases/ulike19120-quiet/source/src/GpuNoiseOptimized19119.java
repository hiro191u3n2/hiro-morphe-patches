package com.hiro.ulike;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/** Optional .119 engine. Its separate owner keeps its EGL context current on
 * exactly one thread. It never changes the published baseline fingerprint.
 * New tokens are opened only through GpuNoise1960's shared session/lease gate. */
final class GpuNoiseOptimized19119 {
    private GpuNoiseOptimized19119(){}
    private static volatile int loaded;
    private static volatile long retained;
    private static volatile String identity="";
    private static volatile boolean prepared;
    private static final ExecutorService OWNER=Executors.newSingleThreadExecutor(new OwnerFactory());
    private static final class OwnerFactory implements ThreadFactory{
        public Thread newThread(Runnable task){Thread t=new Thread(task,"ULike-GPU-optimized19119");t.setDaemon(true);return t;}
    }
    private static boolean available(){
        if(loaded==0)synchronized(GpuNoiseOptimized19119.class){if(loaded==0)try{
            System.loadLibrary("ulike_gpu19119");loaded=rawnativeAbi()==19601?1:-1;
        }catch(LinkageError unavailable){loaded=-1;}catch(SecurityException unavailable){loaded=-1;}}
        return loaded>0;
    }
    static long retainedBytes(){return retained;}
    static String fingerprint(){return identity;}
    static boolean prepared(){return prepared;}
    static boolean prepare(){
        if(!available()||Thread.currentThread().isInterrupted())return false;
        String value=environmentNative();
        if(value==null||value.length()==0)return false;
        identity=value;prepared=supportedNative(48);return prepared;
    }
    static boolean trimIdle(){return loaded!=1||trimNative();}
    private static <T> T call(Callable<T> action,T fallback){
        if(!available())return fallback;
        Future<T> future;
        try{future=OWNER.submit(action);}catch(RuntimeException refused){return fallback;}
        boolean interrupted=false;
        try{for(;;)try{return future.get();}
            catch(InterruptedException cancelled){interrupted=true;}
            catch(ExecutionException failed){return fallback;}}
        finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    static boolean batchNative(long token,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts){return call(new Call_batchNative(token,kinds,slots,bytes,shaders,payloads,bindings,u,f,counts),Boolean.FALSE);}
    private static final class Call_batchNative implements Callable<Boolean> {
        final long token;
        final int[] kinds;
        final int[] slots;
        final long[] bytes;
        final int[] shaders;
        final Object[] payloads;
        final int[][] bindings;
        final int[][] u;
        final float[][] f;
        final int[] counts;
        Call_batchNative(long token,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts){this.token=token;this.kinds=kinds;this.slots=slots;this.bytes=bytes;this.shaders=shaders;this.payloads=payloads;this.bindings=bindings;this.u=u;this.f=f;this.counts=counts;}
        public Boolean call(){try{return rawbatchNative(token,kinds,slots,bytes,shaders,payloads,bindings,u,f,counts);}finally{retained=rawretainedNative();}}
    }
    static int[][] executeNative(long token,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts,int[] readSlots,int[] readCounts){return call(new Call_executeNative(token,kinds,slots,bytes,shaders,payloads,bindings,u,f,counts,readSlots,readCounts),null);}
    private static final class Call_executeNative implements Callable<int[][]> {
        final long token;
        final int[] kinds;
        final int[] slots;
        final long[] bytes;
        final int[] shaders;
        final Object[] payloads;
        final int[][] bindings;
        final int[][] u;
        final float[][] f;
        final int[] counts;
        final int[] readSlots;
        final int[] readCounts;
        Call_executeNative(long token,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts,int[] readSlots,int[] readCounts){this.token=token;this.kinds=kinds;this.slots=slots;this.bytes=bytes;this.shaders=shaders;this.payloads=payloads;this.bindings=bindings;this.u=u;this.f=f;this.counts=counts;this.readSlots=readSlots;this.readCounts=readCounts;}
        public int[][] call(){try{return rawexecuteNative(token,kinds,slots,bytes,shaders,payloads,bindings,u,f,counts,readSlots,readCounts);}finally{retained=rawretainedNative();}}
    }
    static long submitNative(long token,int bank,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts){return call(new Call_submitNative(token,bank,kinds,slots,bytes,shaders,payloads,bindings,u,f,counts),Long.valueOf(0));}
    private static final class Call_submitNative implements Callable<Long> {
        final long token;
        final int bank;
        final int[] kinds;
        final int[] slots;
        final long[] bytes;
        final int[] shaders;
        final Object[] payloads;
        final int[][] bindings;
        final int[][] u;
        final float[][] f;
        final int[] counts;
        Call_submitNative(long token,int bank,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts){this.token=token;this.bank=bank;this.kinds=kinds;this.slots=slots;this.bytes=bytes;this.shaders=shaders;this.payloads=payloads;this.bindings=bindings;this.u=u;this.f=f;this.counts=counts;}
        public Long call(){try{return rawsubmitNative(token,bank,kinds,slots,bytes,shaders,payloads,bindings,u,f,counts);}finally{retained=rawretainedNative();}}
    }
    static int[][] readManyNative(long token,long ticket,int[] slots,int[] counts){return call(new Call_readManyNative(token,ticket,slots,counts),null);}
    private static final class Call_readManyNative implements Callable<int[][]> {
        final long token;
        final long ticket;
        final int[] slots;
        final int[] counts;
        Call_readManyNative(long token,long ticket,int[] slots,int[] counts){this.token=token;this.ticket=ticket;this.slots=slots;this.counts=counts;}
        public int[][] call(){try{return rawreadManyNative(token,ticket,slots,counts);}finally{retained=rawretainedNative();}}
    }
    static int[][] readManyObservedNative19117(long token,long ticket,int[] slots,int[] counts,long[] times){return call(new Call_readManyObservedNative19117(token,ticket,slots,counts,times),null);}
    private static final class Call_readManyObservedNative19117 implements Callable<int[][]> {
        final long token;
        final long ticket;
        final int[] slots;
        final int[] counts;
        final long[] times;
        Call_readManyObservedNative19117(long token,long ticket,int[] slots,int[] counts,long[] times){this.token=token;this.ticket=ticket;this.slots=slots;this.counts=counts;this.times=times;}
        public int[][] call(){try{return rawreadManyObservedNative19117(token,ticket,slots,counts,times);}finally{retained=rawretainedNative();}}
    }
    static boolean readIntoNative(long token,long ticket,int slot,int count,int[] target,int offset){return call(new Call_readIntoNative(token,ticket,slot,count,target,offset),Boolean.FALSE);}
    private static final class Call_readIntoNative implements Callable<Boolean> {
        final long token;
        final long ticket;
        final int slot;
        final int count;
        final int[] target;
        final int offset;
        Call_readIntoNative(long token,long ticket,int slot,int count,int[] target,int offset){this.token=token;this.ticket=ticket;this.slot=slot;this.count=count;this.target=target;this.offset=offset;}
        public Boolean call(){try{return rawreadIntoNative(token,ticket,slot,count,target,offset);}finally{retained=rawretainedNative();}}
    }
    static boolean readManyIntoNative(long token,long ticket,int[] slots,int[] counts,int[][] targets,int[] offsets){return call(new Call_readManyIntoNative(token,ticket,slots,counts,targets,offsets),Boolean.FALSE);}
    private static final class Call_readManyIntoNative implements Callable<Boolean> {
        final long token;
        final long ticket;
        final int[] slots;
        final int[] counts;
        final int[][] targets;
        final int[] offsets;
        Call_readManyIntoNative(long token,long ticket,int[] slots,int[] counts,int[][] targets,int[] offsets){this.token=token;this.ticket=ticket;this.slots=slots;this.counts=counts;this.targets=targets;this.offsets=offsets;}
        public Boolean call(){try{return rawreadManyIntoNative(token,ticket,slots,counts,targets,offsets);}finally{retained=rawretainedNative();}}
    }
    static boolean readManyIntoObservedNative19117(long token,long ticket,int[] slots,int[] counts,int[][] targets,int[] offsets,long[] times){return call(new Call_readManyIntoObservedNative19117(token,ticket,slots,counts,targets,offsets,times),Boolean.FALSE);}
    private static final class Call_readManyIntoObservedNative19117 implements Callable<Boolean> {
        final long token;
        final long ticket;
        final int[] slots;
        final int[] counts;
        final int[][] targets;
        final int[] offsets;
        final long[] times;
        Call_readManyIntoObservedNative19117(long token,long ticket,int[] slots,int[] counts,int[][] targets,int[] offsets,long[] times){this.token=token;this.ticket=ticket;this.slots=slots;this.counts=counts;this.targets=targets;this.offsets=offsets;this.times=times;}
        public Boolean call(){try{return rawreadManyIntoObservedNative19117(token,ticket,slots,counts,targets,offsets,times);}finally{retained=rawretainedNative();}}
    }
    static int ticketReadyNative(long token,long ticket){return call(new Call_ticketReadyNative(token,ticket),Integer.valueOf(-1));}
    private static final class Call_ticketReadyNative implements Callable<Integer> {
        final long token;
        final long ticket;
        Call_ticketReadyNative(long token,long ticket){this.token=token;this.ticket=ticket;}
        public Integer call(){try{return rawticketReadyNative(token,ticket);}finally{retained=rawretainedNative();}}
    }
    static boolean timeoutNative(long token){return call(new Call_timeoutNative(token),Boolean.FALSE);}
    private static final class Call_timeoutNative implements Callable<Boolean> {
        final long token;
        Call_timeoutNative(long token){this.token=token;}
        public Boolean call(){try{return rawtimeoutNative(token);}finally{retained=rawretainedNative();}}
    }
    static long[] capacityNative1971(long token){return call(new Call_capacityNative1971(token),null);}
    private static final class Call_capacityNative1971 implements Callable<long[]> {
        final long token;
        Call_capacityNative1971(long token){this.token=token;}
        public long[] call(){try{return rawcapacityNative1971(token);}finally{retained=rawretainedNative();}}
    }
    static boolean allocateNative(long token,int slot,long bytes){return call(new Call_allocateNative(token,slot,bytes),Boolean.FALSE);}
    private static final class Call_allocateNative implements Callable<Boolean> {
        final long token;
        final int slot;
        final long bytes;
        Call_allocateNative(long token,int slot,long bytes){this.token=token;this.slot=slot;this.bytes=bytes;}
        public Boolean call(){try{return rawallocateNative(token,slot,bytes);}finally{retained=rawretainedNative();}}
    }
    static boolean uploadIntsNative(long token,int slot,int[] values){return call(new Call_uploadIntsNative(token,slot,values),Boolean.FALSE);}
    private static final class Call_uploadIntsNative implements Callable<Boolean> {
        final long token;
        final int slot;
        final int[] values;
        Call_uploadIntsNative(long token,int slot,int[] values){this.token=token;this.slot=slot;this.values=values;}
        public Boolean call(){try{return rawuploadIntsNative(token,slot,values);}finally{retained=rawretainedNative();}}
    }
    static boolean uploadFloatsNative(long token,int slot,float[] values){return call(new Call_uploadFloatsNative(token,slot,values),Boolean.FALSE);}
    private static final class Call_uploadFloatsNative implements Callable<Boolean> {
        final long token;
        final int slot;
        final float[] values;
        Call_uploadFloatsNative(long token,int slot,float[] values){this.token=token;this.slot=slot;this.values=values;}
        public Boolean call(){try{return rawuploadFloatsNative(token,slot,values);}finally{retained=rawretainedNative();}}
    }
    static boolean dispatchNative(long token,int shader,int[] bindings,int[] u,float[] f,int invocations){return call(new Call_dispatchNative(token,shader,bindings,u,f,invocations),Boolean.FALSE);}
    private static final class Call_dispatchNative implements Callable<Boolean> {
        final long token;
        final int shader;
        final int[] bindings;
        final int[] u;
        final float[] f;
        final int invocations;
        Call_dispatchNative(long token,int shader,int[] bindings,int[] u,float[] f,int invocations){this.token=token;this.shader=shader;this.bindings=bindings;this.u=u;this.f=f;this.invocations=invocations;}
        public Boolean call(){try{return rawdispatchNative(token,shader,bindings,u,f,invocations);}finally{retained=rawretainedNative();}}
    }
    static int[] readIntsNative(long token,int slot,int count){return call(new Call_readIntsNative(token,slot,count),null);}
    private static final class Call_readIntsNative implements Callable<int[]> {
        final long token;
        final int slot;
        final int count;
        Call_readIntsNative(long token,int slot,int count){this.token=token;this.slot=slot;this.count=count;}
        public int[] call(){try{return rawreadIntsNative(token,slot,count);}finally{retained=rawretainedNative();}}
    }
    static boolean copyNative1976(long token,int source,int destination,int sourceOffset,int destinationOffset,int count){return call(new Call_copyNative1976(token,source,destination,sourceOffset,destinationOffset,count),Boolean.FALSE);}
    private static final class Call_copyNative1976 implements Callable<Boolean> {
        final long token;
        final int source;
        final int destination;
        final int sourceOffset;
        final int destinationOffset;
        final int count;
        Call_copyNative1976(long token,int source,int destination,int sourceOffset,int destinationOffset,int count){this.token=token;this.source=source;this.destination=destination;this.sourceOffset=sourceOffset;this.destinationOffset=destinationOffset;this.count=count;}
        public Boolean call(){try{return rawcopyNative1976(token,source,destination,sourceOffset,destinationOffset,count);}finally{retained=rawretainedNative();}}
    }
    static boolean uploadRangeNative1976(long token,int destination,int destinationOffset,int[] values,int offset,int count){return call(new Call_uploadRangeNative1976(token,destination,destinationOffset,values,offset,count),Boolean.FALSE);}
    private static final class Call_uploadRangeNative1976 implements Callable<Boolean> {
        final long token;
        final int destination;
        final int destinationOffset;
        final int[] values;
        final int offset;
        final int count;
        Call_uploadRangeNative1976(long token,int destination,int destinationOffset,int[] values,int offset,int count){this.token=token;this.destination=destination;this.destinationOffset=destinationOffset;this.values=values;this.offset=offset;this.count=count;}
        public Boolean call(){try{return rawuploadRangeNative1976(token,destination,destinationOffset,values,offset,count);}finally{retained=rawretainedNative();}}
    }
    static boolean releaseSlotsNative1991(long token,int[] slots){return call(new Call_releaseSlotsNative1991(token,slots),Boolean.FALSE);}
    private static final class Call_releaseSlotsNative1991 implements Callable<Boolean> {
        final long token;
        final int[] slots;
        Call_releaseSlotsNative1991(long token,int[] slots){this.token=token;this.slots=slots;}
        public Boolean call(){try{return rawreleaseSlotsNative1991(token,slots);}finally{retained=rawretainedNative();}}
    }
    static boolean closeNative(long token){return call(new Call_closeNative(token),Boolean.FALSE);}
    private static final class Call_closeNative implements Callable<Boolean> {
        final long token;
        Call_closeNative(long token){this.token=token;}
        public Boolean call(){try{return rawcloseNative(token);}finally{retained=rawretainedNative();}}
    }
    static boolean trimNative(){return call(new Call_trimNative(),Boolean.FALSE);}
    private static final class Call_trimNative implements Callable<Boolean> {
        Call_trimNative(){}
        public Boolean call(){try{return rawtrimNative();}finally{retained=rawretainedNative();}}
    }
    static boolean supportedNative(int id){return call(new Call_supportedNative(id),Boolean.FALSE);}
    private static final class Call_supportedNative implements Callable<Boolean> {
        final int id;
        Call_supportedNative(int id){this.id=id;}
        public Boolean call(){try{return rawsupportedNative(id);}finally{retained=rawretainedNative();}}
    }
    static String programFailureNative19100(int id){return call(new Call_programFailureNative19100(id),null);}
    private static final class Call_programFailureNative19100 implements Callable<String> {
        final int id;
        Call_programFailureNative19100(int id){this.id=id;}
        public String call(){try{return rawprogramFailureNative19100(id);}finally{retained=rawretainedNative();}}
    }
    static long retainedNative(){return call(new Call_retainedNative(),Long.valueOf(0));}
    private static final class Call_retainedNative implements Callable<Long> {
        Call_retainedNative(){}
        public Long call(){try{return rawretainedNative();}finally{retained=rawretainedNative();}}
    }
    static int failureCodeNative1971(){return call(new Call_failureCodeNative1971(),Integer.valueOf(-1));}
    private static final class Call_failureCodeNative1971 implements Callable<Integer> {
        Call_failureCodeNative1971(){}
        public Integer call(){try{return rawfailureCodeNative1971();}finally{retained=rawretainedNative();}}
    }
    static String environmentNative(){return call(new Call_environmentNative(),null);}
    private static final class Call_environmentNative implements Callable<String> {
        Call_environmentNative(){}
        public String call(){try{return rawenvironmentNative();}finally{retained=rawretainedNative();}}
    }
    private static native boolean rawbatchNative(long token,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts);
    private static native int[][] rawexecuteNative(long token,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts,int[] readSlots,int[] readCounts);
    private static native long rawsubmitNative(long token,int bank,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts);
    private static native int[][] rawreadManyNative(long token,long ticket,int[] slots,int[] counts);
    private static native int[][] rawreadManyObservedNative19117(long token,long ticket,int[] slots,int[] counts,long[] times);
    private static native boolean rawreadIntoNative(long token,long ticket,int slot,int count,int[] target,int offset);
    private static native boolean rawreadManyIntoNative(long token,long ticket,int[] slots,int[] counts,int[][] targets,int[] offsets);
    private static native boolean rawreadManyIntoObservedNative19117(long token,long ticket,int[] slots,int[] counts,int[][] targets,int[] offsets,long[] times);
    private static native int rawticketReadyNative(long token,long ticket);
    private static native boolean rawtimeoutNative(long token);
    private static native long[] rawcapacityNative1971(long token);
    private static native boolean rawallocateNative(long token,int slot,long bytes);
    private static native boolean rawuploadIntsNative(long token,int slot,int[] values);
    private static native boolean rawuploadFloatsNative(long token,int slot,float[] values);
    private static native boolean rawdispatchNative(long token,int shader,int[] bindings,int[] u,float[] f,int invocations);
    private static native int[] rawreadIntsNative(long token,int slot,int count);
    private static native boolean rawcopyNative1976(long token,int source,int destination,int sourceOffset,int destinationOffset,int count);
    private static native boolean rawuploadRangeNative1976(long token,int destination,int destinationOffset,int[] values,int offset,int count);
    private static native boolean rawreleaseSlotsNative1991(long token,int[] slots);
    private static native boolean rawcloseNative(long token);
    private static native boolean rawtrimNative();
    private static native int rawnativeAbi();
    private static native boolean rawsupportedNative(int id);
    private static native String rawprogramFailureNative19100(int id);
    private static native long rawretainedNative();
    private static native int rawfailureCodeNative1971();
    private static native String rawenvironmentNative();
    static long openNative(){return call(new Call_openNative(),Long.valueOf(0));}
    private static final class Call_openNative implements Callable<Long>{
        public Long call(){try{return rawopenNative();}finally{retained=rawretainedNative();}}
    }
    private static native long rawopenNative();
}
