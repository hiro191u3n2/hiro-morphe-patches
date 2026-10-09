package com.hiro.ulike;
import java.io.*;import java.lang.reflect.*;import java.util.*;
/** Actual production Strong facade and JNI; fixed CPU outputs were generated
 * separately by frozen published Java. Only Android accounting/model holders
 * and preference storage are fixtures. */
public final class NativeFacadeReplay1978 {
 static int records,captures,oracleCalls,assertions;static Method key;static Map<?,?> gates;
 static int[] ints(DataInputStream i)throws Exception{int n=i.readInt();int[] a=new int[n];for(int x=0;x<n;x++)a[x]=i.readInt();return a;}
 static void require(boolean a,String detail){assertions++;if(!a)throw new AssertionError(detail);}
 static float[] floats(int[] a){float[] f=new float[a.length];for(int x=0;x<a.length;x++)f[x]=Float.intBitsToFloat(a[x]);return f;}
 public static void main(String[] args)throws Exception{
  require(GpuNoise1960.available()&&GpuNoise1960.warmEnvironment1973(),"actual .78 JNI context");
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
     String oldKey=(String)key.invoke(null,(Object)uu);
     String[] profileKeys=new String[8];for(int p=0;p<8;p++)profileKeys[p]=GpuStrong1960.profileKey1973(uu,p);
     for(int profile=0;profile<8;profile++){
     GpuQualification1961.reset();gates.clear();GpuStrongTuning1975.chosen=null;for(int other=0;other<8;other++)if(other!=profile)GpuQualification1961.rejectExact(profileKeys[other]);
     int beforeCalls=oracleCalls;
     for(int capture=0;capture<4;capture++){
      if(capture==1){GpuQualification1961.qualifiedStrongPreferred1970(profileKeys[profile],100,200,0);if(profile<4)GpuStrongTuning1975.chosen=new GpuStrongTuning1975.Choice(profile,0,profileKeys[profile]);}
      if(capture==2)GpuStrongTuning1975.chosen=new GpuStrongTuning1975.Choice(profile,0,profileKeys[profile]);
      boolean expectedGpu=capture>0&&(profile<4||capture>=2);
      int descriptorBefore=protection==null?0:protection.dataCalls;int dispatchBefore=NativeSpeed1978Test.dispatches();
      ProcessingTiming1947.reset();StrongNoise1958.Model model=new StrongNoise1958.Model();model.height=uu[7];model.evidence=floats(in[2]);model.maps=new int[][]{in[4],in[5],in[6]};
      int[] out=new int[in[0].length];Arrays.fill(out,0x12345678);int[] cf=new int[cfWant.length+11];Arrays.fill(cf,0x76543210);
      GpuStrong1960.Oracle oracle=new GpuStrong1960.Oracle(){public boolean run(int[] dst,int[] conf){oracleCalls++;System.arraycopy(want,0,dst,uu[2]*uu[0],want.length);System.arraycopy(cfWant,0,conf,0,cfWant.length);return true;}};
      GpuStrong1960.beginStage(model);boolean ok;
      try{ok=GpuStrong1960.process(in[0],out,uu[0],uu[1],uu[2],uu[3],uu[4],uu[5],uu[6],uu[8],uu[9]!=0,model,in[3],cf,protection,oracle);}finally{GpuStrong1960.endStage(model);gates.clear();}
      require(ok,name+" process "+capture);require(ProcessingTiming1947.lastGpu==(expectedGpu?1:0)&&ProcessingTiming1947.lastCpu==(expectedGpu?0:1),name+" exactly one chosen backend committed "+capture);
      require(ProcessingTiming1947.verification==0,name+" verification "+capture);
      require(Arrays.equals(want,Arrays.copyOfRange(out,uu[2]*uu[0],uu[3]*uu[0])),name+" ARGB "+capture);
      require(Arrays.equals(cfWant,Arrays.copyOf(cf,cfWant.length)),name+" confidence "+capture);
      for(int k=cfWant.length;k<cf.length;k++)require(cf[k]==0x76543210,"pooled tail");
      for(int other=0;other<8;other++)if(other!=profile)require(GpuQualification1961.exactRejected(profileKeys[other]),"other profile negative retained");
      require((GpuQualification1961.restore(profileKeys[profile])!=null)==(capture>0),"no foreground proof invented "+profile);
      require(NativeSpeed1978Test.dispatches()-dispatchBefore==(expectedGpu?(profile>=4||protection==null?1:3):0),"exact production command graph dispatch count "+profile);
      if(profile>=4&&protection!=null)require(protection.dataCalls==descriptorBefore,"direct GPU and deferred CPU never reconstruct policy descriptor");
      if(!expectedGpu)require(ProcessingTiming1947.routeMode==2,"unproved/new-child-only uses deferred CPU");captures++;
     }
     require(oracleCalls-beforeCalls==(profile<4?1:2),name+" ordinary CPU only, never duplicated foreground oracle profile "+profile);
     }
    }
   }
  }
  System.out.println("ENV "+GpuNoise1960.fingerprint());
  System.out.println("RESULT records="+records+" captures="+captures+" oracleCalls="+oracleCalls+" assertions="+assertions+" foregroundProofsZero=true oldNegativeRetained=true actualProductionStrong1978=true actualJNI=true");
 }
}
