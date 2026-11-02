package com.hiro.ulike;
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
final class GpuFinish1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {
 static long nativeRetainedBytes1956(){return 0;}
 static long nativeRetainedBytes1989(long[] out,int offset){return 0;}
 public static Runnable backgroundWorker19121(Runnable task){return task;}
}
final class PostSaveCleanup19122 {
 static volatile long generation=7;
 static volatile boolean idle=true;
 static volatile String checkedThread;
 static boolean isCurrentIdle19122(long expected){checkedThread=Thread.currentThread().getName();return idle&&generation==expected;}
}
