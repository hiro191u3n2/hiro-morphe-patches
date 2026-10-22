package com.hiro.ulike;
import android.graphics.Bitmap;import java.lang.reflect.*;
/** Actual unchanged foreground Finish pressure exit and copy arbiter. */
public final class CopyRotation19110Test {
 static int checks;static final long M=1024L*1024;
 static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
  // First photo queues Resident and offers Finish, which loses the one-image turn.
  GpuSnapshotBudget1981.offerWhole(GpuSnapshotBudget1981.RESIDENT);
  GpuSnapshotBudget1981.Copy first=GpuSnapshotBudget1981.tryCopy(48*M,GpuSnapshotBudget1981.RESIDENT);
  check(first!=null&&first.begin(),"first capture starts one immutable Resident copy");first.close();
  GpuSnapshotBudget1981.offerWhole(GpuSnapshotBudget1981.FINISH);
  check(GpuSnapshotBudget1981.tryCopy(48*M,GpuSnapshotBudget1981.FINISH)==null,"Finish cannot create a second full-image copy");
  // Next photo: Resident occurs BEFORE mandatory Strong and Finish. Finish gets
  // its fair turn; a later memory relief result cannot be known here.
  ProcessingTiming1947.epoch++;
  GpuSnapshotBudget1981.offerWhole(GpuSnapshotBudget1981.RESIDENT);
  GpuSnapshotBudget1981.Decision1985 second=GpuSnapshotBudget1981.tryCopy1985(48*M,GpuSnapshotBudget1981.RESIDENT);
  check(second.copy==null&&second.reason==GpuSnapshotBudget1981.Decision1985.WHOLE_TURN&&second.preferredFamily==GpuSnapshotBudget1981.FINISH,"log's early Resident refusal reproduced");
  check(second.activeFamily==-1&&!second.activeStarted,"no stale owner is needed for fair-turn refusal");
  ThreadLocal<Boolean> pressure=(ThreadLocal<Boolean>)field(GpuChain1961.class,"FINISH_PRESSURE1997").get(null);
  Method schedule=GpuChain1961.class.getDeclaredMethod("scheduleFinish1992",Bitmap.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class,ProcessingTiming1947.Trace.class,String.class);schedule.setAccessible(true);
  pressure.set(Boolean.TRUE);
  try{schedule.invoke(null,null,0,4284,5712,null,false,null,"controlled-pressure");}finally{pressure.remove();}
  check(ProcessingTiming1947.skipped==1,"actual Finish pressure branch reports skipped verification");
  check(!(Boolean)field(GpuSnapshotBudget1981.class,"wholeUsed").get(null)&&field(GpuSnapshotBudget1981.class,"owner1985").get(null)==null,"pressure adds no full image or held copy token");
  check(field(GpuSnapshotBudget1981.class,"seen").getInt(null)==GpuSnapshotBudget1981.RESIDENT,"skipped Finish never offers another future turn");
  // The next capture already rotates to Resident. No extra source retention,
  // past-source reconstruction, priority override or budget increase is needed.
  ProcessingTiming1947.epoch++;
  GpuSnapshotBudget1981.Decision1985 third=GpuSnapshotBudget1981.tryCopy1985(48*M,GpuSnapshotBudget1981.RESIDENT);
  check(third.copy!=null&&third.preferredFamily==GpuSnapshotBudget1981.RESIDENT&&third.copy.begin(),"next capture gives Resident its turn");third.copy.close();
  check(GpuSnapshotBudget1981.tryCopy(48*M,GpuSnapshotBudget1981.FINISH)==null,"one-full-image limit remains active");
  check(GpuSnapshotBudget1981.tryCopy(96*M+1,GpuSnapshotBudget1981.RESIDENT)==null,"96MiB cap unchanged");
  // A stale started owner must keep excluding new captures until it closes.
  ProcessingTiming1947.epoch++;GpuSnapshotBudget1981.Copy held=GpuSnapshotBudget1981.tryCopy(M,0);check(held!=null&&held.begin(),"bounded strip copy held");
  ProcessingTiming1947.epoch++;check(GpuSnapshotBudget1981.tryCopy(M,0)==null,"new capture cannot revoke an active copy");held.close();
  GpuSnapshotBudget1981.Copy fresh=GpuSnapshotBudget1981.tryCopy(M,0);check(fresh!=null&&fresh.begin(),"new capture proceeds after owner closes");fresh.close();
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"finish_pressure_order_reproduced19110\":true,\"next_capture_resident_rotation_verified19110\":true,\"copy_ownership_and_limits_preserved19110\":true,\"actual_finish_pressure_and_copy_budget_executed19110\":true,\"physical_android_tested\":false}");
 }
}
