package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.Field;
import java.util.*;

/** Production frontend dispatch and CPU math; declared transport/readiness
 * controls only select branches and prevent asynchronous execution. */
public final class FrontModel1991Test {
 static int assertions;static long pixels;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 static int frontendJobs()throws Exception {
  int count=0;Object lock=field(GpuQualification1961.class,"LOCK").get(null);
  synchronized(lock){for(Object job:(Iterable<?>)field(GpuQualification1961.class,"QUEUED").get(null)){
   String key=(String)field(job.getClass(),"key").get(job);
   if(key.startsWith(GpuNoise1960.fingerprint()+"|single|")||key.startsWith(GpuNoise1960.fingerprint()+"|residual1964-cache-buffer|"))count++;
  }}return count;
 }
 static int stageJobs()throws Exception {
  int count=0;Object lock=field(GpuQualification1961.class,"LOCK").get(null);
  synchronized(lock){for(Object job:(Iterable<?>)field(GpuQualification1961.class,"QUEUED").get(null)){
   String key=(String)field(job.getClass(),"key").get(job);
   if(key.startsWith("single-stage1981-"))count++;
  }}return count;
 }
 static void run(boolean residual,boolean ready)throws Exception {
  GpuQualification1961.captureChanged();GpuStrongDefault1990.value=ready;GpuFinishDefault1990.value=ready;
  GpuNoise1960.enabled=true;GpuNoise1960.single=!residual;
  int w=33,h=97,begin=7,end=80;int[] input=new int[w*h];Random random=new Random(1990);
  for(int i=0;i<input.length;i++){int v=87+random.nextInt(51);input[i]=0xff000000|v<<16|v<<8|v;}
  final int[] source=input;final int width=w;
  SingleNoise1955.Model model=SingleNoise1955.probe1978(new SingleNoise1955.Patches(){
   public void read(int[] out,int x,int y,int rw,int rh){for(int n=0;n<rh;n++)System.arraycopy(source,(y+n)*width+x,out,n*rw,rw);}
  },w,h,false);
  int[] expected=new int[input.length],actual=new int[input.length],pristine=input.clone();
  Arrays.fill(expected,0x13579bdf);Arrays.fill(actual,0x13579bdf);
  SingleNoise1955.processCpuRange(input,expected,w,h,begin,end,0,h,0,4,true,model,null);
  SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();
  boolean completed=residual?GpuResidual1961.process1981(input,actual,w,h,begin,end,0,h,0,4,true,model,null,workspace):
   GpuSingle1960.process1981(input,actual,w,h,begin,end,0,h,0,4,true,model,null,workspace);
  check(completed,"CPU fallback completed");check(Arrays.equals(expected,actual),"actual CPU output unchanged");pixels+=actual.length;
  check(Arrays.equals(pristine,input),"source not modified");check(workspace.selectedBackend1982==0,"honest CPU backend");
  check(workspace.selectedReason1989==PipelineDetail1988.PROOF_MISSING,"same missing-proof reason");
  check(stageJobs()>0,"bounded frontend stage proof progresses despite downstream readiness");
  check(ready?frontendJobs()==0:frontendJobs()>0,
   ready?"legacy duplicate copies suppressed after bounded stage proof":"legacy proof collection remains when standard route unavailable");
  check(GpuQualification1961.retainedBytes()>0&&GpuQualification1961.retainedBytes()<=96L*1024*1024,"queue ownership bounded");
  GpuQualification1961.captureChanged();
  if(!residual&&ready) {
   Map<?,?> gates=(Map<?,?>)field(GpuSingle1960.class,"GATES").get(null);String key=(String)gates.keySet().iterator().next();
   GpuQualification1961.qualified(key,1000000000L,1L,0);
   String reference=SingleCpu1981.referenceKey(key,w,h,begin,end,0,h,0,4,true,model,null);
   GpuQualification1961.qualified(SingleCpu1981.speedKey(reference,false),1000000000L,1L,0);
   GpuNoise1960.fault=1;GpuNoise1960.readback=Arrays.copyOfRange(expected,begin*w,end*w);int before=GpuNoise1960.openAttempts;
   Arrays.fill(actual,0x13579bdf);
   check(GpuSingle1960.process1981(input,actual,w,h,begin,end,0,h,0,4,true,model,null,workspace),"existing admitted route completes");
   check(GpuNoise1960.openAttempts>before&&workspace.selectedBackend1982==1,"existing GPU admission is not bypassed");
   check(Arrays.equals(expected,actual),"existing GPU output copy contract remains exact");
  }
 }
 public static void main(String[] args)throws Exception {
  android.os.Build.FINGERPRINT="front-copy1990-host";GpuNoise1960.enabled=true;GpuQualification1961.initialize(new Context());
  field(SingleNoise1955.class,"nativeState").setInt(null,-1);
  run(args[0].startsWith("residual"),args[0].endsWith("standard"));
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cpu_pixels_verified\":"+pixels+",\"physical_android_tested\":false,\"device_speedup_verified\":false,\"bounded_stage_proof_progress_executed\":true,\"unchanged_cpu_pixels\":true,\"existing_gpu_admission_preserved\":true}");
 }
}
class GpuStrongDefault1990 {static boolean value;static boolean ready(){return value;}}
class GpuFinishDefault1990 {static boolean value;static boolean ready(){return value;}}
