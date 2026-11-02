package com.hiro.ulike;
public final class SpeedWorkers1935 {
 public interface ScratchMemory {long retainedBytes();void trim();}
 public static void installScratchMemory1956(ScratchMemory m) {}

 public static int[] borrowInts(int n){return new int[n];}
 public static void release(int[]v){}
}
