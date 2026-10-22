package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CancellationException;
public final class OptionalModel1995Test {
 static int checks;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void reset()throws Exception{
  ModelLayout1994Test.reset();GpuQualification1961.jobs.clear();GpuQualification1961.bytes=0;GpuQualification1961.optionalAllowed1995=false;
  for(String n:new String[]{"pending1994","attempted1994"}){Field f=GpuModelDefault1990.class.getDeclaredField(n);f.setAccessible(true);Arrays.fill((boolean[])f.get(null),false);}
 }
 public static void main(String[] args)throws Exception{
  reset();GpuModelDefault1990.requestOptional1994();check(GpuQualification1961.jobs.isEmpty(),"no heavy model jobs before first save");
  boolean cancelled=false;try{GpuModelDefault1990.qualifyVariant1994(1,GO);}catch(CancellationException expected){cancelled=true;}
  check(cancelled&&GpuAnalysis1961.calls==0,"direct entry does not execute pixels before save");
  reset();GpuCertificates1990.passed(GpuModelDefault1990.layoutSuite1994(1),32,1,1,1);GpuCertificates1990.passed(GpuModelDefault1990.speedSuite1994(1),4,1,1,1);
  GpuModelDefault1990.requestOptional1994();check(GpuQualification1961.jobs.size()==1,"only cached exact-and-fast model links pre-first");
  GpuQualification1961.Probe p=GpuQualification1961.jobs.remove(0);p.run(GO);p.close();check(GpuAnalysis1961.calls==0&&GpuModelDefault1990.admitted1994(1),"cached prepare performs no synthetic pixels");
  reset();GpuCertificates1990.passed(GpuModelDefault1990.layoutSuite1994(1),32,1,1,1);GpuCertificates1990.passed(GpuModelDefault1990.speedSuite1994(1),4,1,1,1);
  GpuModelDefault1990.requestOptional1994();p=GpuQualification1961.jobs.remove(0);GpuCertificates1990.values.remove(GpuModelDefault1990.speedSuite1994(1));GpuQualification1961.optionalAllowed1995=true;
  cancelled=false;try{p.run(GO);}catch(CancellationException expected){cancelled=true;}finally{p.close();}
  check(cancelled&&GpuAnalysis1961.calls==0,"cached owner cannot silently become benchmark after certificate vanishes");
  GpuModelDefault1990.requestOptional1994();check(GpuQualification1961.jobs.size()==2,"cancelled prepare rearmed after successful publication");
  for(GpuQualification1961.Probe q:GpuQualification1961.jobs)q.close();GpuQualification1961.jobs.clear();
  reset();GpuQualification1961.optionalAllowed1995=true;GpuNoise1960.supported=false;GpuModelDefault1990.requestOptional1994();
  for(GpuQualification1961.Probe q:GpuQualification1961.jobs){q.run(GO);q.close();}GpuQualification1961.jobs.clear();GpuModelDefault1990.requestOptional1994();check(GpuQualification1961.jobs.isEmpty(),"completed unavailable layouts not repeated per shot");
  Constructor<?>[] constructors=Class.forName("com.hiro.ulike.GpuModelDefault1990$1").getDeclaredConstructors();check(constructors.length==1&&Arrays.equals(constructors[0].getParameterTypes(),new Class<?>[]{int.class}),"model anonymous owner ABI preserves sole captured variant int");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"controlled_lifecycle\":true,\"physical_android_tested\":false}");
 }
}
