package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Real dispatchers + CPU fallback; faults occur only at declared GPU I/O. */
public final class ProducerFailure1992Test {
 static int assertions;static void check(boolean v,String why){assertions++;if(!v)throw new AssertionError(why);}
 static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 static Object failure(String key)throws Exception{Method m=GpuQualification1961.class.getDeclaredMethod("failure",String.class,String.class);m.setAccessible(true);return m.invoke(null,key,GpuQualification1961.environment1990());}
 static int outcome(String key)throws Exception{Object value=failure(key);return value==null?0:field(value.getClass(),"outcome1991").getInt(value);}
 static int[] image(int w,int h){int[] p=new int[w*h];Random r=new Random(92);for(int i=0;i<p.length;i++){int v=80+r.nextInt(64);p[i]=0xff000000|v<<16|v<<8|v;}return p;}
 static void frontend(boolean residual,int fault)throws Exception {
  GpuNoise1960.single=!residual;final int w=33,h=97,b=7,e=80;final int[] src=image(w,h),pristine=src.clone();
  SingleNoise1955.Model model=SingleNoise1955.probe1978(new SingleNoise1955.Patches(){public void read(int[] out,int x,int y,int rw,int rh){for(int n=0;n<rh;n++)System.arraycopy(src,(y+n)*w+x,out,n*rw,rw);}},w,h,false);
  int[] out=new int[src.length];SingleNoise1955.Workspace ws=new SingleNoise1955.Workspace();
  if(residual)GpuResidual1961.process1981(src,out,w,h,b,e,0,h,0,4,true,model,null,ws);else GpuSingle1960.process1981(src,out,w,h,b,e,0,h,0,4,true,model,null,ws);
  GpuQualification1961.captureChanged();
  Map<?,?> gates=(Map<?,?>)field(residual?GpuResidual1961.class:GpuSingle1960.class,"GATES").get(null);check(gates.size()==1,"one actual shape key");String key=(String)gates.keySet().iterator().next();
  String speed=SingleCpu1981.speedKey(SingleCpu1981.referenceKey(key,w,h,b,e,0,h,0,4,true,model,null),false);
  GpuQualification1961.qualified(key,1000000000L,1,0);GpuQualification1961.qualified(speed,1000000000L,1,0);
  Arrays.fill(out,0x13579bdf);GpuNoise1960.fault=fault;boolean completed=false,cancelled=false;
  try{completed=residual?GpuResidual1961.process1981(src,out,w,h,b,e,0,h,0,4,true,model,null,ws):GpuSingle1960.process1981(src,out,w,h,b,e,0,h,0,4,true,model,null,ws);}catch(CancellationException stop){cancelled=true;}finally{Thread.interrupted();}
  check(!completed,"failed GPU candidate never adopted");check(Arrays.equals(src,pristine),"caller source unchanged");
  for(int value:out)check(value==0x13579bdf,"failed candidate commits no partial pixel");
  int expected=fault==0?8:fault==2?15:fault==3?7:0;
  check(outcome(speed)==expected,"producer failure type fault="+fault+" residual="+residual+" expected="+expected+" actual="+outcome(speed));
  check(!GpuQualification1961.exactRejected(key),"execution failure never becomes quality mismatch");
  check(!GpuQualification1961.status1967().contains("実測速度未達1件"),"uncompleted GPU cannot create measured speed failure");
  if(fault==5)check(cancelled,"cancellation propagated");
  check(GpuNoise1960.active==0,"all failed GPU owners released");ws.close();
 }
 static void analysis(int fault)throws Exception {
  final int w=96,h=96;final int[] src=image(w,h);SpatialNoise1934.Patches patches=new SpatialNoise1934.Patches(){public void read(int[] out,int x,int y,int rw,int rh){for(int n=0;n<rh;n++)System.arraycopy(src,(y+n)*w+x,out,n*rw,rw);}};
  SpatialNoise1934 expected=SpatialNoise1934.probeCpu1961(patches,w,h);String key="spatial1961:v2:"+w+":"+h;
  GpuQualification1961.qualified(key,1000000000L,1,0);GpuNoise1960.fault=fault;
  SpatialNoise1934 actual=null;boolean cancelled=false;
  try{actual=GpuAnalysis1961.spatial(patches,w,h);}catch(CancellationException stop){cancelled=true;}finally{Thread.interrupted();}
  if(fault==4||fault==5)check(cancelled,"analysis cancellation propagated");
  else check(SpatialNoise1934.same1961(expected,actual),"analysis failure returns exact CPU fallback");
  int expectedType=fault==0?8:fault==2?15:fault==3?7:0;
  check(outcome(key)==expectedType,"analysis typed error fault="+fault+" expected="+expectedType+" actual="+outcome(key));
  check(!GpuQualification1961.exactRejected(key),"analysis transport failure is not numerical rejection");
  check(GpuNoise1960.active==0,"analysis session released");
 }
 public static void main(String[] args)throws Exception {
  android.os.Build.FINGERPRINT="producer-failure92";GpuNoise1960.enabled=true;GpuQualification1961.initialize(new Context());field(SingleNoise1955.class,"nativeState").setInt(null,-1);
  int fault=Integer.parseInt(args[1]);if(args[0].equals("analysis"))analysis(fault);else frontend(args[0].equals("residual"),fault);
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_dispatchers\":true,\"controlled_gpu_faults\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
