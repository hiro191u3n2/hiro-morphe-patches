package com.hiro.ulike;
import java.io.*;import java.lang.reflect.*;import java.util.*;
/** Actual production Strong facade and JNI; fixed CPU outputs were generated
 * separately by frozen published Java. Only Android accounting/model holders
 * and preference storage are fixtures. */
public final class NativeFacadeReplay {
 static int records,captures,oracleCalls,assertions;static Method key;static Map<?,?> gates;
 static int[] ints(DataInputStream i)throws Exception{int n=i.readInt();int[] a=new int[n];for(int x=0;x<n;x++)a[x]=i.readInt();return a;}
 static void require(boolean a,String detail){assertions++;if(!a)throw new AssertionError(detail);}
 static float[] floats(int[] a){float[] f=new float[a.length];for(int x=0;x<a.length;x++)f[x]=Float.intBitsToFloat(a[x]);return f;}
 public static void main(String[] args)throws Exception{
  key=GpuStrong1960.class.getDeclaredMethod("key",int[].class);key.setAccessible(true);Field gf=GpuStrong1960.class.getDeclaredField("GATES");gf.setAccessible(true);gates=(Map<?,?>)gf.get(null);
  try(DataInputStream i=new DataInputStream(new FileInputStream(args[0]))){require(i.readInt()==1960001,"format");
   for(;;){String name=i.readUTF();if(name.length()==0)break;int shader=i.readInt();int[] u=ints(i);int count=i.readInt(),nb=i.readInt();int[][] input=new int[nb][];for(int x=0;x<nb;x++)input[x]=ints(i);int[] expected=ints(i),confidence=ints(i);
    if(shader!=0||u[10]!=3)continue;records++;
    for(int scenario=0;scenario<4;scenario++){
     final int[] uu=u.clone();int[] wantPixels=expected,wantConfidence=confidence;int[][] ii=input.clone();
     if((scenario&1)!=0){
      if(u[1]<65)continue;int width=u[0],start=36,end=Math.min(u[1]-18,84)&~3;if(end<=start)continue;
      int firstRow=start-18,lastRow=end+18;
      uu[1]=lastRow-firstRow;uu[2]=start-firstRow;uu[3]=end-firstRow;uu[4]=0;uu[5]=uu[1];uu[6]=firstRow;
      ii[0]=Arrays.copyOfRange(input[0],firstRow*width,lastRow*width);ii[3]=Arrays.copyOfRange(input[3],start*width*2,end*width*2);
      wantPixels=Arrays.copyOfRange(expected,start*width,end*width);int cols=(width+3)/4;wantConfidence=Arrays.copyOfRange(confidence,(start/4)*cols,(end/4)*cols);
     }
     final int[] want=wantPixels,cfWant=wantConfidence;final int[][] in=ii;
     GpuPolicy1960.Protection protection=null;
     if(scenario>=2){
      protection=new GpuPolicy1960.Protection();protection.plan=new GpuPolicy1960.Plan();protection.plan.beautyQ8=512;protection.plan.shadowBudgetQ8=256;
      GpuPolicy1960.PolicyData pd=new GpuPolicy1960.PolicyData();pd.count=uu[0]*(uu[3]-uu[2]);pd.masks=new int[pd.count*2];pd.grid=new int[]{0};
      pd.u[0]=4;pd.u[1]=uu[0];pd.u[2]=uu[3]-uu[2];pd.u[3]=uu[6]+uu[2];pd.u[6]=256;pd.u[7]=512;pd.u[8]=1;pd.f[0]=4;
      for(int m=0;m<pd.count;m++){pd.masks[m*2]=256-in[3][m*2];pd.masks[m*2+1]=in[3][m*2+1];}
      protection.descriptor=pd;uu[18]=512;uu[19]=256;
     }
     GpuQualification1961.reset();gates.clear();String oldKey=(String)key.invoke(null,(Object)uu);GpuQualification1961.rejectExact(oldKey);
     String genericKey=oldKey.replace("strong-gx1964-parallel-policy-bank-v1:","strong-gx1971-generic-policy-bank-v1:");
     int beforeCalls=oracleCalls;
     for(int capture=0;capture<3;capture++){
      ProcessingTiming1947.reset();StrongNoise1958.Model model=new StrongNoise1958.Model();model.height=uu[7];model.evidence=floats(in[2]);model.maps=new int[][]{in[4],in[5],in[6]};
      int[] out=new int[in[0].length];Arrays.fill(out,0x12345678);int[] cf=new int[cfWant.length+11];Arrays.fill(cf,0x76543210);
      GpuStrong1960.Oracle oracle=new GpuStrong1960.Oracle(){public boolean run(int[] dst,int[] conf){oracleCalls++;System.arraycopy(want,0,dst,uu[2]*uu[0],want.length);System.arraycopy(cfWant,0,conf,0,cfWant.length);return true;}};
      GpuStrong1960.beginStage(model);boolean ok;
      try{ok=GpuStrong1960.process(in[0],out,uu[0],uu[1],uu[2],uu[3],uu[4],uu[5],uu[6],uu[8],uu[9]!=0,model,in[3],cf,protection,oracle);}finally{GpuStrong1960.endStage(model);gates.clear();}
      require(ok,name+" process "+capture);require(ProcessingTiming1947.lastGpu==1&&ProcessingTiming1947.lastCpu==0,name+" GPU committed "+capture);
      require(ProcessingTiming1947.verification==(capture==0?2:0),name+" verification "+capture);
      require(Arrays.equals(want,Arrays.copyOfRange(out,uu[2]*uu[0],uu[3]*uu[0])),name+" ARGB "+capture);
      require(Arrays.equals(cfWant,Arrays.copyOf(cf,cfWant.length)),name+" confidence "+capture);
      for(int k=cfWant.length;k<cf.length;k++)require(cf[k]==0x76543210,"pooled tail");
      require(GpuQualification1961.exactRejected(oldKey),"old negative retained");require(GpuQualification1961.restore(genericKey)!=null,"generic certified");captures++;
     }
     require(oracleCalls-beforeCalls==2,name+" independent first proof only");
    }
   }
  }
  System.out.println("ENV "+GpuNoise1960.fingerprint());
  System.out.println("RESULT records="+records+" captures="+captures+" oracleCalls="+oracleCalls+" assertions="+assertions+" firstProofOnly=true oldNegativeRetained=true actualProductionStrong=true actualJNI=true");
 }
}
