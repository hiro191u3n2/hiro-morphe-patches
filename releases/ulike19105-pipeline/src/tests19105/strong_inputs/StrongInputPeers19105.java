package com.hiro.ulike;
import java.util.*;

final class GpuQualification1961 {interface Cancellation {boolean cancelled();}}
final class ResidentProof1978 {static final int UNAVAILABLE=0,EXACT=1,MISMATCH=-1;}
final class QualityPixels1932 {
 static final class Plan {int beautyQ8=137,shadowBudgetQ8=91;Object faceRegions,localNoise;}
}
final class GpuPolicy1960 {
 static int calls;static int lastRow;
 static final class Protection {
  final QualityPixels1932.Plan plan;Protection(QualityPixels1932.Plan p,boolean strong){plan=p;if(!strong)throw new AssertionError("wrong policy");}
  Protection freezePolicy1981(int width,int rows,int row,int[] policy,boolean descriptor){
   if(rows!=1||descriptor||policy.length!=width*2)throw new AssertionError("not a single fixed raw policy");calls++;lastRow=row;
   for(int x=0;x<width;x++){policy[x*2]=x%257;policy[x*2+1]=(row+x)%257;}return this;
  }
 }
}
final class StrongNoise1958 {
 static final int HALO=18;static int calls,nativeCalls,javaCalls,backend=1,javaDelta=-1;
 static boolean fail,oom,mutateSource,cancelAfterNative;static int[] sharedInput,sharedPolicy,sharedU;
 static final class Model {final int width,height;final int[][] maps;private final float[] evidence,runtimeEvidence;
  Model(int w,int h){width=w;height=h;maps=new int[][]{new int[((w+1)/2)*((h+1)/2)],new int[]{0},new int[]{0}};evidence=new float[64];runtimeEvidence=new float[64];}}
 static int[][] gpuMaps1960(Model model){return model.maps;}
 static float[] gpuEvidence1960(Model model){return model.runtimeEvidence;}
 static long workspaceBytes(int width,int rows){return 1008L*width+1048576L;}
 static int diagnosticCpu19105(int[] in,int[] out,int[] u,Model m,int[] policy,boolean javaOnly){
  calls++;if(javaOnly)javaCalls++;else nativeCalls++;
  if(fail)throw new IllegalStateException("scripted CPU failure");if(oom)throw new OutOfMemoryError("scripted budget loss");
  if(u.length!=32||u[3]-u[2]!=1||u[10]!=3||u[11]!=1||u[12]!=0||u[7]!=m.height||u[18]!=137||u[19]!=91)throw new AssertionError("uniform contract");
  if(!javaOnly){sharedInput=in;sharedPolicy=policy;sharedU=u;}
  else if(sharedInput!=in||sharedPolicy!=policy||sharedU!=u)throw new AssertionError("inputs not shared");
  int first=u[2]*u[0];System.arraycopy(in,first,out,first,u[0]);if(javaOnly&&javaDelta>=0)out[first+javaDelta]^=1;
  if(mutateSource&&!javaOnly)in[0]^=1;
  if(cancelAfterNative&&!javaOnly)StrongInputs19105Test.cancel.value=true;
  return javaOnly?2:backend;
 }
}
final class GpuStrong1960 {
 static int calls,delta=-1;static boolean fail,mutateU,mutatePolicy,mutateModel;static int[] lastU,lastSource;
 static final class Result {final int[] pixels;Result(int[] p){pixels=p;}}
 static final class Read1971 {final Result result;final String failure;Read1971(Result r,String f){result=r;failure=f;}}
 static Read1971 ephemeral1973(int[] source,float[] evidence,int[][] maps,int[] policy,int[] u,Object descriptor,int variant,int profile){
  calls++;if(descriptor!=null||profile!=4||variant!=0||source!=StrongNoise1958.sharedInput||policy!=StrongNoise1958.sharedPolicy||u!=StrongNoise1958.sharedU)throw new AssertionError("fixed GPU input contract");
  lastU=u.clone();lastSource=source.clone();if(fail)return new Read1971(null,"scripted GPU failure");
  int[] out=Arrays.copyOfRange(source,u[2]*u[0],u[3]*u[0]);if(delta>=0)out[delta]^=1;
  if(mutateU)u[31]^=1;if(mutatePolicy)policy[0]^=1;if(mutateModel)maps[0][0]^=1;
  return new Read1971(new Result(out),null);
 }
}
final class GpuNoise1960 {
 static final int GEOMETRY=17;static boolean memory=true;static long requested;static int calls;
 static boolean workspaceFits(long n){calls++;requested=n;return memory;}
 static boolean supports(int program){return false;}
 static final class Batch {Batch allocate(int s,long n){return this;}Batch dispatch(int p,int[] s,int[] u,float[] f,int n){return this;}}
 static final class Lease1971 {boolean revalidate1971(){return false;}void close(){}}
 static final class Session {
  Lease1971 reserveCapacity1983(int[] a,long[] b,long[] c,long d){throw new AssertionError("unexpected existing GPU context");}
  boolean run(Batch b){return false;}int[] readInts(int s,int n){return null;}boolean releaseSlots1991(int[] slots){return true;}
 }
}
