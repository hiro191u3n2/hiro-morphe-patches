package com.hiro.ulike;
import java.util.*;
final class GpuResident1976 {
 static final ThreadLocal<Boolean> cpu=new ThreadLocal<Boolean>();
 static GpuQualification1961.Cancellation cancellation(){return GpuQualification1961.owner.get();}
 static boolean cpuOracle(){return Boolean.TRUE.equals(cpu.get());}
}
final class GpuQualification1961 {
 interface Cancellation {boolean cancelled();}
 static final ThreadLocal<Cancellation> owner=new ThreadLocal<Cancellation>();
 static final ThreadLocal<Boolean> bg=new ThreadLocal<Boolean>();
 static final List<long[]> records=Collections.synchronizedList(new ArrayList<long[]>());
 static volatile boolean throwSink;
 static boolean background(){return Boolean.TRUE.equals(bg.get());}
 public static void residentTiming19111(long[] value){if(throwSink)throw new AssertionError("controlled unavailable sink");records.add(value);}
}
