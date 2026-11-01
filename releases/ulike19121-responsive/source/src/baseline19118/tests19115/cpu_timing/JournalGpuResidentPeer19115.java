package com.hiro.ulike;
/** Only the caller's existing CPU/cancellation scope is substituted. The timing
 * observer, queue validation, persistence and history are production classes. */
final class GpuResident1976 {
 static final ThreadLocal<GpuQualification1961.Cancellation> owner=new ThreadLocal<GpuQualification1961.Cancellation>();
 static GpuQualification1961.Cancellation cancellation(){return owner.get();}
 static boolean cpuOracle(){return owner.get()!=null;}
}
