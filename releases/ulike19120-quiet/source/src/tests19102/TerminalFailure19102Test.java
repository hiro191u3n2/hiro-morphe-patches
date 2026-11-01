package com.hiro.ulike;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/** Executes the actual modified queue; fixture peers do not implement its logic. */
public final class TerminalFailure19102Test {
 static int assertions;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static void terminal(final int kind,int expected,String label)throws Exception{
  TypedFailure1992Test.clean();final String key="uncaught19102-"+kind;final AtomicInteger closed=new AtomicInteger();
  check(GpuQualification1961.schedule(key,1,new GpuQualification1961.Probe(){
   public void run(GpuQualification1961.Cancellation cancellation){
    switch(kind){case 0:throw new IllegalStateException("controlled runtime");case 1:throw new AssertionError("controlled assertion");case 2:throw new LinkageError("controlled linkage");default:throw new OutOfMemoryError("controlled memory");}
   }
   public void close(){closed.incrementAndGet();}
  }),"queued terminal case "+kind);
  TypedFailure1992Test.age();TypedFailure1992Test.waitDone();
  check(closed.get()==1,"one owner close");
  check(TypedFailure1992Test.failure(key)!=null,"actual terminal creates failure record");
  check(TypedFailure1992Test.outcome(key)==expected,"typed terminal ledger outcome");
  check(!GpuQualification1961.exactRejected(key),"operational error never means quality mismatch");
  check(!GpuQualification1961.maySchedule(key),"retry cooldown applied");
  check(GpuQualification1961.status1967().contains(label+"1件"),"rendered ledger count agrees with exception");
  check(GpuQualification1961.retainedBytes()==0,"retained source memory released");
 }
 static void cancellation()throws Exception{
  TypedFailure1992Test.clean();final AtomicInteger closed=new AtomicInteger();
  check(GpuQualification1961.schedule("cancel19102",1,new GpuQualification1961.Probe(){
   public void run(GpuQualification1961.Cancellation c){throw new CancellationException("controlled");}
   public void close(){closed.incrementAndGet();}
  }),"queued cancellation");
  TypedFailure1992Test.age();TypedFailure1992Test.waitDone();
  check(closed.get()==1,"cancelled owner closes once");
  check(TypedFailure1992Test.failure("cancel19102")==null,"cancellation does not spend retry or create failure");
  check(GpuQualification1961.status1967().contains("例外0件"),"cancel not rendered as exception");
 }
 static void alreadyRegistered()throws Exception{
  TypedFailure1992Test.clean();final Object[] registered={null};final AtomicInteger closed=new AtomicInteger();
  check(GpuQualification1961.schedule("double19102",1,new GpuQualification1961.Probe(){
   public void run(GpuQualification1961.Cancellation c){
    GpuQualification1961.rejectNonSpeed1988("double19102","exception");
    try{registered[0]=TypedFailure1992Test.failure("double19102");}catch(Exception e){throw new AssertionError(e);}
    throw new IllegalStateException("already registered");
   }
   public void close(){closed.incrementAndGet();}
  }),"queued explicit then uncaught case");
  TypedFailure1992Test.age();TypedFailure1992Test.waitDone();
  check(registered[0]!=null&&registered[0]==TypedFailure1992Test.failure("double19102"),"terminal does not rewrite existing retry record");
  check(closed.get()==1,"double path owner closes once");
  check(GpuQualification1961.status1967().contains("例外1件"),"single counted error");
 }
 public static void main(String[] args)throws Exception{
  terminal(0,15,"例外");terminal(1,15,"例外");terminal(2,7,"GPU未対応");terminal(3,6,"メモリ不足");cancellation();alreadyRegistered();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_qualification_queue\":true,\"physical_android_tested\":false}");
 }
}
