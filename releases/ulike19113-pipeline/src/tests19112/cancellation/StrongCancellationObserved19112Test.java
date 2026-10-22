package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.concurrent.CancellationException;
/** The runner injects one interrupt into the unchanged map loop in throwaway
 * source copies of both revisions. No numeric expression is instrumented. */
public final class StrongCancellationObserved19112Test {
 public static void main(String[] args)throws Exception{
  boolean wanted=Boolean.parseBoolean(args[0]),cancelled=false,completed=false;
  try{
   Method optional=null;
   try{optional=GpuStrongDefault1990.class.getDeclaredMethod("fixture19112",Integer.TYPE,GpuQualification1961.Cancellation.class);}catch(NoSuchMethodException old){}
   if(optional==null){GpuStrongDefault1990.fixture(64);completed=true;}
   else{optional.invoke(null,64,new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}});completed=true;}
  }catch(InvocationTargetException failure){if(failure.getCause() instanceof CancellationException)cancelled=true;else throw failure;}
  finally{if(!Thread.interrupted())throw new AssertionError("fixture interrupt injection did not execute");}
  if(cancelled!=wanted||completed==wanted)throw new AssertionError("optional map generation did not honor requested interrupt behavior");
  System.out.println("{\"status\":\"passed\",\"assertions\":3,\"cancelled\":"+cancelled+",\"completed_after_interrupt\":"+completed+"}");
 }
}
