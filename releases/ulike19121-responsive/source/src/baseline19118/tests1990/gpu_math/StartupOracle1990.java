package com.hiro.ulike;
import java.io.*;import java.util.*;
/** Execute the actual production startup fixtures and production CPU oracle. */
public final class StartupOracle1990 {
 static DataOutputStream out;static long pixels;
 static void verifyNative(GpuStrongDefault1990.Fixture f,int[] expected,int[] confidence)throws Exception{
  String name=confidence==null?"processNative":"processNativeShared";java.lang.reflect.Method target=null;
  for(java.lang.reflect.Method method:StrongNoise1958.class.getDeclaredMethods())if(method.getName().equals(name)){target=method;break;}
  if(target==null)throw new AssertionError("Missing CPU native entry");target.setAccessible(true);
  int[] u=f.u,actual=new int[f.source.length],actualConfidence=confidence==null?null:new int[confidence.length];
  int[][] maps=StrongNoise1958.gpuMaps1960(f.model);
  java.util.ArrayList<Object> args=new java.util.ArrayList<Object>(Arrays.asList(f.source,actual,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,
   StrongNoise1958.gpuEvidence1960(f.model),maps[0],maps[1],maps[2],f.model.width,f.model.height,f.policy,3));
  if(confidence!=null)args.add(actualConfidence);
  if(!Boolean.TRUE.equals(target.invoke(null,args.toArray())))throw new AssertionError("Native CPU did not complete");
  if(!Arrays.equals(actual,expected)||!Arrays.equals(actualConfidence,confidence))throw new AssertionError("Native CPU oracle entry differs from production wrapper");
 }
 static void ints(int[] a)throws Exception{out.writeInt(a.length);for(int x:a)out.writeInt(x);}
 static int[] bits(float[] a){int[] b=new int[a.length];for(int i=0;i<a.length;i++)b[i]=Float.floatToRawIntBits(a[i]);return b;}
 public static void main(String[] args)throws Exception{
  boolean nativeCpu=args.length>1&&args[1].equals("native");
  if(StrongNoise1958.nativeAvailable()!=nativeCpu)throw new AssertionError("Wrong CPU backend availability");
  out=new DataOutputStream(new FileOutputStream(args[0]));out.writeInt(1960001);
  for(int id=0;id<GpuStrongDefault1990.CASES;id++){
   GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(id);int[] u=f.u,expected=new int[f.source.length];int count=u[0]*(u[3]-u[2]);
   int[] confidence=u[12]==0?null:new int[((u[0]+3)/4)*((u[3]-u[2]+3)/4)];
   if(!StrongNoise1958.gpuOracleSnapshot1961(f.source,expected,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,f.model,f.policy,confidence))throw new AssertionError("CPU oracle unavailable");
   if(nativeCpu)verifyNative(f,expected,confidence);
   int[][] maps=StrongNoise1958.gpuMaps1960(f.model);int[][] input={f.source,new int[count],bits(StrongNoise1958.gpuEvidence1960(f.model)),f.policy==null?new int[1]:f.policy,maps[0],maps[1],maps[2],new int[confidence==null?1:confidence.length]};
   out.writeUTF("startup-case"+id);out.writeInt(0);ints(u);out.writeInt(count);out.writeInt(input.length);for(int[] a:input)ints(a);
   ints(Arrays.copyOfRange(expected,u[2]*u[0],u[3]*u[0]));ints(confidence==null?new int[0]:confidence);pixels+=count;
  }
  out.writeUTF("");out.close();System.out.println("STARTUP cases="+GpuStrongDefault1990.CASES+" pixels="+pixels+" native="+nativeCpu);
 }
}
