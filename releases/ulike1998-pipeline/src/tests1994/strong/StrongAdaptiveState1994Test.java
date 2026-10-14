package com.hiro.ulike;
import java.util.concurrent.CancellationException;
/** Explicit transport/certificate fault inputs; no numerical quality claim. */
public final class StrongAdaptiveState1994Test {
 static int assertions;static void check(boolean v,String s){assertions++;if(!v)throw new AssertionError(s);}
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void reset(){GpuCertificates1990.proofs.clear();GpuCertificates1990.rejects.clear();GpuStrong1960.fault=0;GpuStartup1990.invalidated=false;GpuStrong1960.slowCandidate=false;GpuNoise1960.supported=true;GpuCertificates1990.passed(GpuStrongDefault1990.SUITE,68,1,1,1);}
 static void preserved(){check(GpuStrongDefault1990.ready()&&GpuStrongDefault1990.profile1994()==4,"optional failure preserves baseline");}
 public static void main(String[] args)throws Exception{
  reset();GpuNoise1960.supported=false;check(!GpuStrongDefault1990.Adaptive1994.qualify(GO),"unsupported optional layout");preserved();
  for(int fault:new int[]{3,4}){reset();GpuStrong1960.fault=fault;check(!GpuStrongDefault1990.Adaptive1994.qualify(GO)&&!GpuCertificates1990.rejected(GpuStrongDefault1990.Adaptive1994.EXACT)&&GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)==null,"transport/OOM never becomes proof or mismatch");preserved();}
  for(int fault:new int[]{1,2}){reset();GpuStrong1960.fault=fault;check(!GpuStrongDefault1990.Adaptive1994.qualify(GO)&&GpuCertificates1990.rejected(GpuStrongDefault1990.Adaptive1994.EXACT),"pixel/confidence mismatch rejects only optional");preserved();}
  reset();boolean cancelled=false;try{GpuStrongDefault1990.Adaptive1994.qualify(new GpuQualification1961.Cancellation(){int n;public boolean cancelled(){return ++n>4;}});}catch(CancellationException expected){cancelled=true;}
  check(cancelled&&GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)==null,"mid-corpus cancellation no incomplete proof");preserved();
  reset();GpuStrong1960.slowCandidate=true;check(!GpuStrongDefault1990.Adaptive1994.qualify(GO),"measured slower candidate not adopted");
  check(GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)!=null&&GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.FAST)==null,"independent quality retained but failed speed not certified");preserved();
  reset();GpuStrong1960.fault=5;check(!GpuStrongDefault1990.Adaptive1994.qualify(GO),"broader benchmark detects standard output mismatch");
  check(!GpuStrongDefault1990.ready()&&GpuCertificates1990.rejected(GpuStrongDefault1990.SUITE)&&GpuStartup1990.invalidated,"observed baseline mismatch revokes only its actual certificate and readiness");
  check(!GpuCertificates1990.rejected(GpuStrongDefault1990.Adaptive1994.EXACT)&&GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)==null,"baseline failure does not falsely reject or certify candidate");
  reset();GpuCertificates1990.passed(GpuStrongDefault1990.Adaptive1994.EXACT,70,1,1,1);GpuCertificates1990.passed(GpuStrongDefault1990.Adaptive1994.FAST,70,1,1,1);
  GpuNoise1960.warmed=false;int supportCalls=GpuNoise1960.supportCalls;
  check(GpuStrongDefault1990.profile1994()==4&&GpuNoise1960.supportCalls==supportCalls,"cached candidate with no linked shader keeps foreground baseline without compile");
  check(GpuStrongDefault1990.Adaptive1994.qualify(GO)&&GpuNoise1960.warmed&&GpuStrongDefault1990.profile1994()==7,"idle cached certificate path links candidate before admission");
  reset();GpuNoise1960.supported=false;GpuQualification1961.acceptQueue=true;GpuQualification1961.scheduled=0;
  GpuStrongDefault1990.request1994();GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1,"only one optional queued owner");
  GpuQualification1961.Probe queued=GpuQualification1961.pending;
  try{queued.run(new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}catch(CancellationException expected){}finally{queued.close();}
  GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==2,"foreground-cancelled optional proof rearmed at next request");
  queued=GpuQualification1961.pending;try{queued.run(GO);}finally{queued.close();}
  GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==2,"completed unsupported probe not repeated every photograph");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_jni\":false,\"controlled_faults\":true}");
 }
}
