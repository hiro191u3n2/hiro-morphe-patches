package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.concurrent.CancellationException;
public final class GpuGate19119Test {
 static int assertions;
 static void check(boolean c,String label){assertions++;if(!c)throw new AssertionError(label);}
 static final GpuQualification1961.Cancellation GO=()->false;
 static void reset()throws Exception{
  for(String name:new String[]{"requested","attempted","disabled"}){Field f=GpuStrongOptimized19119.class.getDeclaredField(name);f.setAccessible(true);f.setBoolean(null,false);}
  for(String name:new String[]{"exactKey","fastKey"}){Field f=GpuStrongOptimized19119.class.getDeclaredField(name);f.setAccessible(true);f.set(null,null);}
  GpuNoiseOptimized19119.preparedState=false;GpuNoiseOptimized19119.supported=true;GpuNoiseOptimized19119.identity="device|candidate=A";
  GpuCertificates1990.passes.clear();GpuCertificates1990.rejects.clear();GpuCertificates1990.writes=0;
  GpuStrongDefault1990.Adaptive1994.baseline=true;GpuStrong1960.workloadCalls=0;GpuStrong1960.workloadMemory=GpuStrong1960.workloadFail=GpuStrong1960.workloadWrongCounts=GpuStrong1960.workloadWrongShape=false;GpuStrong1960.calls=GpuStrong1960.mutateArgb=GpuStrong1960.mutateConfidence=GpuStrong1960.fail=0;
  GpuStrong1960.oldTime=100;GpuStrong1960.newTime=89;GpuQualification1961.allowed=true;GpuQualification1961.outcome=null;GpuQualification1961.pending=null;GpuQualification1961.schedules=0;
 }
 public static void main(String[] args)throws Exception{
  reset();check(!GpuStrongOptimized19119.ready(3060,4080),"old certificate cannot authorize new arithmetic");
  check(GpuStrongOptimized19119.qualify(GO),"full proof success");check(GpuStrong1960.calls==70&&GpuStrong1960.workloadCalls==10,"70 exact and 10 full workload warm/reverse trials");
  check(GpuCertificates1990.writes==2&&GpuCertificates1990.passes.size()==2,"separate exact and speed");check(GpuStrongOptimized19119.ready(3060,4080),"qualified portrait");
  check(GpuStrongOptimized19119.ready(4080,3060),"qualified landscape");check(!GpuStrongOptimized19119.ready(3059,4080),"unmeasured dimensions excluded");
  int calls=GpuStrong1960.calls;check(GpuStrongOptimized19119.qualify(GO)&&GpuStrong1960.calls==calls,"cached proof prepares without recomputation");
  GpuStrongDefault1990.Adaptive1994.baseline=false;check(!GpuStrongOptimized19119.ready(3060,4080),"lost baseline cannot revive new stage");
  reset();GpuStrong1960.mutateArgb=1;check(!GpuStrongOptimized19119.qualify(GO)&&GpuCertificates1990.writes==0,"ARGB mutant rejected before write");check(GpuCertificates1990.rejects.size()==1,"exact rejection persisted");
  reset();GpuStrong1960.mutateConfidence=1;check(!GpuStrongOptimized19119.qualify(GO)&&GpuCertificates1990.writes==0,"confidence mutant rejected");
  reset();GpuStrong1960.newTime=91;check(!GpuStrongOptimized19119.qualify(GO),"slower than ten percent rejected");check(GpuCertificates1990.writes==1&&!GpuStrongOptimized19119.ready(3060,4080),"exact alone never selects candidate");
  reset();GpuStrong1960.fail=10;check(!GpuStrongOptimized19119.qualify(GO)&&GpuCertificates1990.writes==0&&GpuCertificates1990.rejects.isEmpty(),"transport failure no quality rejection/pass");
  reset();GpuNoiseOptimized19119.supported=false;check(!GpuStrongOptimized19119.qualify(GO)&&GpuStrong1960.calls==0,"unsupported uses no output");
  reset();final int[] queries={0};boolean cancelled=false;try{GpuStrongOptimized19119.qualify(()->++queries[0]>30);}catch(CancellationException expected){cancelled=true;}
  check(cancelled&&GpuCertificates1990.writes==0,"capture cancellation no incomplete proof");
  reset();GpuQualification1961.allowed=false;cancelled=false;try{GpuStrongOptimized19119.qualify(GO);}catch(CancellationException expected){cancelled=true;}
  check(cancelled&&GpuStrong1960.calls==0,"deferred heavy work only prepares");
  reset();GpuStrongOptimized19119.request();GpuStrongOptimized19119.request();check(GpuQualification1961.schedules==1,"one optional owner");
  GpuQualification1961.pending.run(GO);GpuQualification1961.pending.close();GpuStrongOptimized19119.request();check(GpuQualification1961.schedules==1,"complete job not repeatedly benchmarked");
  reset();GpuStrongOptimized19119.qualify(GO);GpuNoiseOptimized19119.identity="device|candidate=B";GpuStrong1960.mutateArgb=1;
  check(!GpuStrongOptimized19119.qualify(GO)&&!GpuStrongOptimized19119.ready(3060,4080),"changed arithmetic identity cannot reuse A");
  check(GpuStrongOptimized19119.wins(100,90)&&!GpuStrongOptimized19119.wins(100,91)&&!GpuStrongOptimized19119.wins(Long.MAX_VALUE,1)&&!GpuStrongOptimized19119.wins(0,1),"inclusive margin invalid time guards");
  reset();GpuStrongOptimized19119.qualify(GO);GpuStrongOptimized19119.unavailable();check(!GpuStrongOptimized19119.ready(3060,4080),"context failure disables current candidate");
  reset();GpuStrong1960.workloadMemory=true;check(!GpuStrongOptimized19119.qualify(GO)&&GpuCertificates1990.writes==0,"whole workload reservation refusal no certificate");
  reset();GpuStrong1960.workloadFail=true;check(!GpuStrongOptimized19119.qualify(GO)&&GpuCertificates1990.writes==0,"whole workload transport failure no certificate");
  reset();GpuStrong1960.workloadWrongCounts=true;check(!GpuStrongOptimized19119.qualify(GO)&&GpuCertificates1990.writes==0,"incomplete two-bank workload no certificate");
  reset();cancelled=false;try{GpuStrongOptimized19119.qualify(()->GpuStrong1960.workloadCalls>=2);}catch(CancellationException expected){cancelled=true;}
  check(cancelled&&GpuCertificates1990.writes==0,"mid-workload cancellation no certificate");
  reset();GpuStrong1960.workloadWrongShape=true;check(!GpuStrongOptimized19119.qualify(GO)&&GpuCertificates1990.writes==0,"other orientation cannot satisfy this workload");
  System.out.println("PASS optimizedGPU19119 gate assertions="+assertions+" actualProductionHelper=true");
 }
}
