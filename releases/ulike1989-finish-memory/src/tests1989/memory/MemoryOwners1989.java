package com.hiro.ulike;

/** Controlled independent memory owners and the output-only diagnostic sink.
 * Actual GpuNoise, SpeedWorkers and FinishMemory production bodies execute. */
final class WholeRoute1953 {
    static long bytes;static int queries,faultAt;
    static long retainedBytes(){if(++queries==faultAt)throw new OutOfMemoryError("optional observation peer");return bytes;}
}
final class GpuFinish1953 {static long bytes;static long retainedBytes(){return bytes;}}
final class GpuQualification1961 {
    static boolean inBackground=true,cancelled;static int reports,fault;static long[] last;
    static boolean background(){return inBackground;}
    static boolean cancelled(){return cancelled;}
    static void finishMemory1989(long[] values){
        reports++;
        if(fault==1)throw new IllegalStateException("optional memory report");
        if(fault==2)throw new LinkageError("optional memory report");
        if(fault==3)throw new OutOfMemoryError("optional memory report");
        if(fault==4)throw new AssertionError("optional memory report");
        last=values.clone();
        java.util.Arrays.fill(values,Long.MAX_VALUE); // A hostile observer cannot alter the fit result.
    }
}
final class MemoryNative1989 {
    static{System.loadLibrary("memory_native1989");}
    static native long strongAcquire(int bytes,int value);
    static native boolean strongRelease(long handle,int bytes,int value);
    static native long strongBytes();static native void strongTrim();
    static native long h8Acquire(int bytes,int value);
    static native boolean h8Release(long handle,int bytes,int value);
    static native long h8Bytes();static native void h8Trim();
}
