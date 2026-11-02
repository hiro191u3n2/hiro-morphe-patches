package com.hiro.ulike;
public final class Responsiveness19122 {
    static volatile long lastTrace=-1,lastReleased=-1;
    static volatile int calls;
    public static void cleanup19122(long trace,String status,long elapsed,long released){
        if(elapsed<0)throw new AssertionError("negative duration");
        lastTrace=trace;lastReleased=released;calls++;
    }
}
