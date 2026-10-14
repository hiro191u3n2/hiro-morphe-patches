package com.hiro.ulike;
import java.io.*;import java.util.*;
public final class NativeBankReplay {
 static int records,reads,diffs,cfDiffs;static String first="";
 static int[] ints(DataInputStream i)throws Exception{int n=i.readInt();int[] a=new int[n];for(int x=0;x<n;x++)a[x]=i.readInt();return a;}
 static int compare(int[] expected,int[] actual,String name,boolean cf){if(actual==null||actual.length!=expected.length)throw new AssertionError("read shape "+name);int n=0;for(int x=0;x<expected.length;x++)if(expected[x]!=actual[x]){n++;if(first.length()==0)first=name+" at "+x+" expected="+Integer.toHexString(expected[x])+" got="+Integer.toHexString(actual[x]);}if(cf)cfDiffs+=n;else diffs+=n;return n;}
 public static void main(String[] args)throws Exception{
  try(DataInputStream i=new DataInputStream(new FileInputStream(args[0]))){if(i.readInt()!=1960001)throw new AssertionError();
   for(;;){String name=i.readUTF();if(name.length()==0)break;int shader=i.readInt();int[] u=ints(i);int count=i.readInt(),nb=i.readInt();int[][] input=new int[nb][];for(int x=0;x<nb;x++)input[x]=ints(i);int[] expected=ints(i),confidence=ints(i);
    if(shader!=0||u[10]!=3)continue;records++;
    for(int scenario=0;scenario<2;scenario++){
     int[] uu=u.clone(),we=expected,ce=confidence;int[][] ii=input.clone();int wc=count;
     String label=name+" scenario="+scenario;
     if(scenario==1){
      if(u[1]<65)continue;int width=u[0],start=36,end=Math.min(u[1]-18,84)&~3;if(end<=start)continue;
      int firstRow=start-18,lastRow=end+18;
      uu[1]=lastRow-firstRow;uu[2]=start-firstRow;uu[3]=end-firstRow;uu[4]=0;uu[5]=uu[1];uu[6]=firstRow;
      ii[0]=Arrays.copyOfRange(input[0],firstRow*width,lastRow*width);
      ii[3]=Arrays.copyOfRange(input[3],start*width*2,end*width*2);
      we=Arrays.copyOfRange(expected,start*width,end*width);wc=we.length;
      int cols=(width+3)/4;ce=Arrays.copyOfRange(confidence,(start/4)*cols,(end/4)*cols);
     }
    for(int program:new int[]{0,9,18,30,34,38}){
     if(!GpuNoise1960.supports(program)){System.out.println("UNSUPPORTED "+program);continue;}
     for(int bank=0;bank<2;bank++){
      GpuNoise1960.Session s=GpuNoise1960.open();if(s==null)throw new AssertionError("open "+name);
      int src=bank==0?0:14,out=bank==0?1:15,policy=bank==0?3:16,cf=bank==0?7:17;
      try{
       for(int slot:new int[]{2,4,5,6})if(!s.upload(slot,ii[slot]))throw new AssertionError("model upload");
       int[] poisoned=new int[Math.max(1,ce.length)];Arrays.fill(poisoned,0x10203040);
       GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(src,ii[0]).allocate(out,4L*we.length).upload(policy,ii[3]).upload(cf,poisoned).dispatch(program,new int[]{src,out,2,policy,4,5,6,cf},uu,null,wc);
       GpuNoise1960.Ticket t=s.submit(b,bank);if(t==null)throw new AssertionError("submit "+name+" p="+program+" bank="+bank);
       int[][] actual=s.collect(t,new int[]{out,cf},new int[]{we.length,Math.max(1,ce.length)});if(actual==null)throw new AssertionError("collect "+name+" p="+program+" bank="+bank);
       int pd=compare(we,actual[0],label+" program="+program+" bank="+bank,false),cd=compare(ce,actual[1],label+" confidence program="+program+" bank="+bank,true);reads++;
       if(pd+cd>0)System.out.println("DIFF "+label+" program="+program+" bank="+bank+" pixels="+pd+" confidence="+cd);
      }finally{s.close();}
     }
    }
    }
   }
  }
  System.out.println("ENV "+GpuNoise1960.fingerprint());
  System.out.println("RESULT records="+records+" reads="+reads+" pixelsDiff="+diffs+" confidenceDiff="+cfDiffs+" first="+first);
 }
}
