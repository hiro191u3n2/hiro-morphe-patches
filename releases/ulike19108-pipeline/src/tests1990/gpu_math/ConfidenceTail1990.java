package com.hiro.ulike;
import java.io.*;import java.util.*;
/** A one-row core ends before the global NR13 sample row. CPU leaves zero.
 * Poison the reused GPU destination to make its missing write observable. */
public final class ConfidenceTail1990 {
 static int[] ints(DataInputStream in)throws Exception{int n=in.readInt();int[] r=new int[n];for(int i=0;i<n;i++)r[i]=in.readInt();return r;}
 public static void main(String[] args)throws Exception{
  try(DataInputStream in=new DataInputStream(new FileInputStream(args[0]))){if(in.readInt()!=1960001)throw new AssertionError();
   for(;;){String name=in.readUTF();if(name.length()==0)throw new AssertionError("No matching oracle");int shader=in.readInt();int[] u=ints(in);in.readInt();int n=in.readInt();int[][] data=new int[n][];for(int i=0;i<n;i++)data[i]=ints(in);int[] expected=ints(in);ints(in);
    if(shader!=0||u[10]!=3||u[1]<65)continue;
    u[2]=36;u[3]=37;int w=u[0];data[3]=Arrays.copyOfRange(data[3],36*w*2,37*w*2);int[] poisoned=new int[(w+3)/4];Arrays.fill(poisoned,0x10203040);
    GpuNoise1960.Session s=GpuNoise1960.open();try{
     for(int slot:new int[]{2,4,5,6})if(!s.upload(slot,data[slot]))throw new AssertionError("upload");
     GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(0,data[0]).allocate(1,w*4L).upload(3,data[3]).upload(7,poisoned).dispatch(39,new int[]{0,1,2,3,4,5,6,7},u,null,w);
     int[][] actual=s.execute(b,new int[]{1,7},new int[]{w,poisoned.length});if(actual==null)throw new AssertionError("output");
     for(int i=0;i<w;i++)if(actual[0][i]!=expected[36*w+i])throw new AssertionError("ARGB mismatch");
     int diff=0;for(int v:actual[1])if(v!=0)diff++;
     System.out.println("TAIL confidenceMismatches="+diff+" columns="+poisoned.length+" first="+Integer.toHexString(actual[1][0]));
     if(args.length<2&&diff>0)throw new AssertionError("Unwritten partial confidence cell");
     if(args.length>1&&diff==0)throw new AssertionError("Historical bug did not reproduce");return;
    }finally{s.close();}
   }
  }
 }
}
