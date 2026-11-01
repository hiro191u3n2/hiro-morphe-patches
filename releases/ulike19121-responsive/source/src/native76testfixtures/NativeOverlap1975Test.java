package com.hiro.ulike;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CancellationException;
/** Production Strong + Java engine + native engine + shaders, with frozen
 * independent Java expected outputs and bounded observation/fault holders. */
public final class NativeOverlap1975Test {
 static int assertions;
 static native int largeUploads();static native void failRead();static native void clearFault();
 static void check(boolean v,String label){assertions++;if(!v)throw new AssertionError(label);}
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static final class Data {
  String name;int[] u,want,confidence;int[][] input;
  StrongNoise1958.Model model(){StrongNoise1958.Model m=new StrongNoise1958.Model();m.height=u[7];m.maps=new int[][]{input[4],input[5],input[6]};m.evidence=new float[input[2].length];for(int i=0;i<m.evidence.length;i++)m.evidence[i]=Float.intBitsToFloat(input[2][i]);return m;}
 }
 static Data data(String path)throws Exception {
  try(DataInputStream in=new DataInputStream(new FileInputStream(path))){check(in.readInt()==1960001,"frozen oracle format");
   for(;;){String name=in.readUTF();if(name.isEmpty())throw new AssertionError("Strong oracle absent");int shader=in.readInt();int[] u=NativeFacadeReplay.ints(in);in.readInt();int n=in.readInt();int[][] inputs=new int[n][];for(int j=0;j<n;j++)inputs[j]=NativeFacadeReplay.ints(in);int[] want=NativeFacadeReplay.ints(in),cf=NativeFacadeReplay.ints(in);
    if(shader==0&&u[10]==3&&want.length>32&&u[8]>0){Data d=new Data();d.name=name;d.u=u;d.input=inputs;d.want=want;d.confidence=cf;return d;}}
  }
 }
 static Object stage()throws Exception{return field(GpuStrong1960.class,"active").get(null);}
 static GpuNoise1960.Session session()throws Exception{Object s=stage();return s==null?null:(GpuNoise1960.Session)field(s.getClass(),"session").get(s);}
 static int tickets()throws Exception {GpuNoise1960.Session s=session();if(s==null)return 0;Object[] p=(Object[])field(GpuNoise1960.Session.class,"pending").get(s);int n=0;for(Object o:p)if(o!=null)n++;return n;}
 static void reset()throws Exception {
  check(stage()==null,"prior stage drained");((Map<?,?>)field(GpuStrong1960.class,"GATES").get(null)).clear();
  GpuQualification1961.reset();ProcessingTiming1947.reset();StrongNoise1958.scratchHint1975=1048576L;WholeRoute1953.retained=0;
 }
 static int exercise(final Data d,final String mode)throws Exception {
  reset();if("serial".equals(mode))StrongNoise1958.scratchHint1975=GpuNoise1960.MAX_BYTES+1;
  final int[] out=new int[d.input[0].length],confidence=new int[d.confidence.length+13];Arrays.fill(out,0x13579bdf);Arrays.fill(confidence,0x2468ace0);
  final int[] calls={0},firstLarge={-1},secondLarge={-1};final Thread worker=Thread.currentThread();
  final StrongNoise1958.Model model=d.model();GpuNoise1960.Session observed=null;boolean completed=false,cancelled=false;
  GpuStrong1960.Oracle oracle=new GpuStrong1960.Oracle(){public boolean run(int[] target,int[] cf){
   calls[0]++;check(Thread.currentThread()==worker,"oracle remains on owning strip worker");
   try {
    Object owner=stage();check(!Thread.holdsLock(owner),"CPU oracle outside stage monitor");
    check(tickets()==("serial".equals(mode)?0:1),"GPU ticket overlaps oracle or bounded serial fallback");
    int uploaded=largeUploads();if(calls[0]==1)firstLarge[0]=uploaded;else {secondLarge[0]=uploaded;if(!"serial".equals(mode))check(uploaded==firstLarge[0],"second proof reuses immutable large input uploads");}
   }catch(Exception e){throw new AssertionError(e);}
   if("false".equals(mode)&&calls[0]==1)return false;
   if("throw".equals(mode)&&calls[0]==1)throw new IllegalStateException("controlled oracle failure");
   System.arraycopy(d.want,0,target,d.u[2]*d.u[0],d.want.length);System.arraycopy(d.confidence,0,cf,0,d.confidence.length);
   if("readback".equals(mode)&&calls[0]==1)failRead();
   if("interrupt".equals(mode)&&calls[0]==1)Thread.currentThread().interrupt();
   return true;
  }};
  GpuStrong1960.beginStage(model);
  try {
   completed=GpuStrong1960.process(d.input[0],out,d.u[0],d.u[1],d.u[2],d.u[3],d.u[4],d.u[5],d.u[6],d.u[8],d.u[9]!=0,model,d.input[3],confidence,null,oracle);
   if("overlap".equals(mode)||"serial".equals(mode)) {
    check(secondLarge[0]==largeUploads(),"trial2 dispatch adds no immutable large upload");
    Object owner=stage();Object[][] before=(Object[][])field(owner.getClass(),"readbacks1975").get(owner);
    Object bank=before[0][0];int[][] cached=(int[][])field(bank.getClass(),"values").get(bank);int[] pixels=cached[0];
    Arrays.fill(out,0x13579bdf);Arrays.fill(confidence,0,d.confidence.length,0x2468ace0);
    GpuStrong1960.Oracle forbidden=new GpuStrong1960.Oracle(){public boolean run(int[] a,int[] b){throw new AssertionError("certified result cannot rerun CPU proof");}};
    check(GpuStrong1960.process(d.input[0],out,d.u[0],d.u[1],d.u[2],d.u[3],d.u[4],d.u[5],d.u[6],d.u[8],d.u[9]!=0,model,d.input[3],confidence,null,forbidden),"same-stage certified strip completes");
    Object[][] after=(Object[][])field(owner.getClass(),"readbacks1975").get(owner);
    check(after[0][0]==bank&&((int[][])field(bank.getClass(),"values").get(bank))[0]==pixels,"same-shape native readback reuses owned Strong buffers");
   }
  }catch(CancellationException expected){cancelled=true;}
  finally {observed=session();GpuStrong1960.endStage(model);Thread.interrupted();clearFault();StrongNoise1958.scratchHint1975=1048576L;}
  check(GpuNoise1960.reservedBytes1971()==0,"all native/Java/scratch reservations released");check(!GpuNoise1960.sessionBusy(),"closed stage leaves no active native session");
  if(observed!=null){Object[] pending=(Object[])field(GpuNoise1960.Session.class,"pending").get(observed);check(pending[0]==null&&pending[1]==null,"all submitted tickets retired on close");}
  check(((Set<?>)field(GpuStrong1960.class,"FLIGHTS1973").get(null)).isEmpty(),"cold proof owner released");
  if("false".equals(mode)||"throw".equals(mode))check(!completed&&!cancelled,"partial/failed CPU oracle requests caller repair");
  else if("interrupt".equals(mode))check(cancelled&&!completed,"oracle interruption propagates without committed GPU");
  else {check(completed&&!cancelled,"valid CPU/GPU result completes");check(Arrays.equals(d.want,Arrays.copyOfRange(out,d.u[2]*d.u[0],d.u[3]*d.u[0])),"complete ARGB equals frozen reference");check(Arrays.equals(d.confidence,Arrays.copyOf(confidence,d.confidence.length)),"complete confidence equals frozen reference");}
  for(int i=d.confidence.length;i<confidence.length;i++)check(confidence[i]==0x2468ace0,"pooled confidence tail untouched");
  boolean success="overlap".equals(mode)||"serial".equals(mode);
  check(success?ProcessingTiming1947.lastGpu==2:ProcessingTiming1947.lastGpu==0,"only validated candidate increments GPU output counter");
  check(success?calls[0]==2:calls[0]==1,"two independent successful oracle trials; failure stops promptly");
  check(success?!GpuQualification1961.records.isEmpty():GpuQualification1961.records.isEmpty(),"certificate only after both complete proofs");
  return calls[0];
 }
 public static void main(String[] args)throws Exception {
  Data d=data(args[0]);String mode=args[1];int calls=exercise(d,mode);
  if(!"overlap".equals(mode)&&!"serial".equals(mode))exercise(d,"overlap");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"oracle_calls\":"+calls+",\"actual_jni\":true,\"physical_android_tested\":false}");
 }
}
