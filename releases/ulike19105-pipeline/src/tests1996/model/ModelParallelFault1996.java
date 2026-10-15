package com.hiro.ulike;
/** Test-only decoration around successful real JNI operations. */
final class ModelParallelFault1996 {
    static int mode,fired;
    static GpuNoise1960.Ticket submit(GpuNoise1960.Ticket result){if(result!=null&&mode==1){mode=0;fired++;return null;}return result;}
    static boolean upload(boolean result){if(result&&mode==2){mode=0;fired++;return false;}return result;}
    static int[][] collect(int[][] result){
        if(result==null||mode<3)return result;int fault=mode;mode=0;fired++;
        if(fault==3)return null;
        if(fault==4)throw new IllegalStateException("controlled post-collect failure");
        if(fault==5){Thread.currentThread().interrupt();return result;}
        if(fault==6)throw new OutOfMemoryError("controlled post-collect allocation failure");
        throw new AssertionError("unknown fault");
    }
}
