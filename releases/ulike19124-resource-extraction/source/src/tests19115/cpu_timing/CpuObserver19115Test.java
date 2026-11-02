package com.hiro.ulike;
import java.lang.reflect.*;import java.util.*;
public final class CpuObserver19115Test {
 static int assertions,cases;
 static void check(boolean b,String s){assertions++;if(!b)throw new AssertionError(s);}
 static void send(String op,long...v){ResidentTiming19111.dispatch(op,v);}
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 @SuppressWarnings("unchecked") static ThreadLocal<Object> current()throws Exception{return (ThreadLocal<Object>)field(ResidentTiming19111.class,"CPU19115").get(null);}
 static ResidentTiming19111Test.Cancel reset()throws Exception{current().remove();GpuQualification1961.cpuRecords.clear();ResidentTiming19111Test.Cancel c=ResidentTiming19111Test.eligible();GpuResident1976.cpu.set(true);return c;}
 static Object begin()throws Exception{send("beginCpu19115",0);Object s=current().get();check(s!=null,"CPU oracle existing call starts observation");return s;}
 static void phase(Object s,long phase,long now)throws Exception{ResidentTiming19111Test.invoke(s,"phase",new Class<?>[]{long.class,long.class},phase,now);}
 static void stop(Object s,long now,int state)throws Exception{ResidentTiming19111Test.invoke(s,"stop",new Class<?>[]{long.class,int.class},now,state);}
 static long[] publish()throws Exception{send("publishCpu19115");check(current().get()==null,"CPU owner detached");check(GpuQualification1961.cpuRecords.size()==1,"one CPU record");check(GpuQualification1961.records.isEmpty(),"no extra GPU trial");return GpuQualification1961.cpuRecords.get(0);}
 public static void main(String[] args)throws Exception{
  cases++;reset();Object s=begin();phase(s,0,10);phase(s,1,20);phase(s,2,1000);phase(s,3,1010);phase(s,4,1030);phase(s,5,10000);phase(s,13,20000);stop(s,30000,1);long[] v=publish();
  check(v[2]==10&&v[3]==20&&v[4]==3&&v[5]==0,"setup and work exclude model and post-Strong signatures");
  check(v[9]==0,"no hook means unobserved counts");for(int i=6;i<9;i++)check(v[i]==-1,"unobserved plan stays unknown");for(int i=10;i<17;i++)check(v[i]==-1,"unobserved backend stays unknown");send("publishCpu19115");check(GpuQualification1961.cpuRecords.size()==1,"no duplicate publish");
  cases++;reset();s=begin();send("strongPlan",4,256,12000);send("strongPlan",0,256,1);send("cpuStrongCounts19115",12,3,1,0,0,2,1);send("cpuStrongCounts19115",99,0,0,0,0,0,0);phase(s,2,10);phase(s,3,20);phase(s,4,40);stop(s,50,1);v=publish();check(Arrays.equals(Arrays.copyOfRange(v,6,9),new long[]{4,256,12000}),"exact observed plan only");check(Arrays.equals(Arrays.copyOfRange(v,10,17),new long[]{12,3,1,0,0,2,1}),"backend packet copied once");
  cases++;reset();s=begin();send("cpuStrongCounts19115",-1,0,0,0,0,0,0);send("cpuStrongCounts19115",1000001,0,0,0,0,0,0);stop(s,10,1);v=publish();check(v[9]==0&&v[10]==-1,"malformed backend does not invent observation");check(v[2]==-1&&v[3]==-1,"completed call with missing phase is unknown");
  cases++;reset();s=begin();phase(s,2,10);phase(s,3,20);stop(s,30,1);v=publish();check(v[1]==4&&v[4]==0&&v[5]==3,"missing phase4 cannot complete Strong");
  cases++;reset();s=begin();phase(s,2,10);phase(s,4,30);stop(s,40,1);v=publish();check(v[1]==4&&v[5]==1&&v[3]==-1,"skipped work boundary cannot complete setup or invent work");
  for(int status:new int[]{2,3}){cases++;reset();s=begin();phase(s,2,10);phase(s,3,20);phase(s,4,30);stop(s,40,status);v=publish();check(v[1]==status&&v[4]==0&&v[5]==3,"failed/cancelled call is incomplete");}
  cases++;ResidentTiming19111Test.Cancel c=reset();s=begin();phase(s,3,10);phase(s,4,20);stop(s,30,1);c.value=true;v=publish();check(v[1]==3&&v[4]==0&&v[5]==2,"late owner cancellation cannot complete work");
  cases++;reset();s=begin();phase(s,3,10);phase(s,4,20);stop(s,30,1);Thread.currentThread().interrupt();try{v=publish();check(v[1]==3&&Thread.currentThread().isInterrupted(),"interrupt cancellation remains set");}finally{Thread.interrupted();}
  cases++;reset();begin();send("publishCpu19115");check(GpuQualification1961.cpuRecords.get(0)[1]==2,"exception before stop is failed");
  for(int mode=0;mode<6;mode++){cases++;c=reset();if(mode==0)GpuQualification1961.owner.remove();if(mode==1)c.value=true;if(mode==2)GpuQualification1961.bg.set(false);if(mode==3)GpuResident1976.cpu.set(false);send("beginCpu19115",mode==4?2:mode==5?-1:0);check(current().get()==null,"scope eligibility");send("publishCpu19115");check(GpuQualification1961.cpuRecords.isEmpty(),"ineligible no observation");}
  cases++;reset();s=begin();final Object own=s;Thread other=new Thread(()->{send("cpuStrongCounts19115",1,0,0,0,0,0,0);send("publishCpu19115");});other.start();other.join();check(current().get()==own&&GpuQualification1961.cpuRecords.isEmpty(),"foreign thread cannot observe queue owner");stop(s,1,1);v=publish();check(v[9]==0,"foreign backend packet discarded");
  for(int mode=0;mode<3;mode++){cases++;reset();s=begin();phase(s,2,100);if(mode==0){phase(s,3,99);}else if(mode==1){phase(s,3,200);phase(s,2,300);}else{phase(s,3,100+86400000000001L);}stop(s,300,1);send("publishCpu19115");check(GpuQualification1961.cpuRecords.isEmpty()&&current().get()==null,"invalid chronology detached without false result");}
  cases++;reset();s=begin();stop(s,1,1);GpuQualification1961.throwSink=true;send("publishCpu19115");check(current().get()==null&&GpuQualification1961.cpuRecords.isEmpty(),"sink failure isolated");GpuQualification1961.throwSink=false;
  for(Class<?> k:ResidentTiming19111.class.getDeclaredClasses())for(Field f:k.getDeclaredFields())check(f.getType().isPrimitive()||f.getType()==long[].class,"no pixels or model retained");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"cpu_phase3_to4_excludes_signatures\":true,\"cpu_unknown_cancel_incomplete_preserved\":true,\"cpu_thread_scope_and_sink_isolation\":true}");
 }
}
