package com.hiro.ulike;
import java.io.*;
import java.util.*;

/** Actual program39/JNI readback against byte-frozen published Java output.
 * All buffers belong to this test. Neither admission nor timing is inferred. */
public final class StrongMathReplay19105 {
    static long assertions,pixels;static int records,reads,singleRows,confidenceToggles;
    static int[] ints(DataInputStream i)throws Exception{int n=i.readInt();int[] a=new int[n];for(int x=0;x<n;x++)a[x]=i.readInt();return a;}
    static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static void compare(int[] expected,int[] actual,String name){
        check(actual!=null&&actual.length==expected.length,"shape "+name);
        for(int x=0;x<expected.length;x++){check(expected[x]==actual[x],name+" at "+x+" expected="+Integer.toHexString(expected[x])+" got="+Integer.toHexString(actual[x]));pixels++;}
    }
    static void row(String name,int[] u,int[][] input,int[] expected,int first,int end,boolean rebased)throws Exception{
        int w=u[0],lo=Math.max(0,first-18),hi=Math.min(u[1],end+18);int[] q=u.clone();
        int[][] data=input.clone();data[0]=Arrays.copyOfRange(input[0],lo*w,hi*w);data[3]=Arrays.copyOfRange(input[3],first*w*2,end*w*2);
        q[1]=hi-lo;q[2]=first-lo;q[3]=end-lo;q[4]=0;q[5]=hi-lo;q[6]=lo;
        if(rebased){q[1]=u[1];q[2]=first;q[3]=end;q[4]=0;q[5]=u[1];q[6]=0;q[14]=lo;}
        dispatch(name+" first="+first+" end="+end+" rebased="+rebased,q,data,Arrays.copyOfRange(expected,first*w,end*w));
    }
    static void dispatch(String name,int[] u,int[][] data,int[] expected)throws Exception{
        for(int bank=0;bank<2;bank++)for(int capture=0;capture<2;capture++){
            int[] q=u.clone();q[12]=capture;
            int src=bank==0?0:14,out=bank==0?1:15,policy=bank==0?3:16,cf=bank==0?7:17;
            GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"session");
            try{
                for(int slot:new int[]{2,4,5,6})check(session.upload(slot,data[slot]),"model upload");
                int cfCount=((q[0]+3)/4)*((q[3]-q[2]+3)/4);int[] poisoned=new int[cfCount];Arrays.fill(poisoned,0x1234abcd);
                GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(src,data[0]).allocate(out,4L*expected.length).upload(policy,data[3]).upload(cf,poisoned)
                    .dispatch(39,new int[]{src,out,2,policy,4,5,6,cf},q,null,expected.length);
                GpuNoise1960.Ticket ticket=session.submit(b,bank);check(ticket!=null,"submit");
                int[][] actual=session.collect(ticket,new int[]{out,cf},new int[]{expected.length,cfCount});check(actual!=null,"collect");
                compare(expected,actual[0],name+" confidence="+capture+" bank="+bank);reads++;
                // Off really is off: this also detects accidental scratch aliasing.
                if(capture==0){check(Arrays.equals(poisoned,actual[1]),"confidence off buffer preserved");confidenceToggles++;}
            }finally{session.close();}
        }
    }
    public static void main(String[] args)throws Exception{
        check(GpuNoise1960.supports(39),"program39 supported");
        try(DataInputStream i=new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])))){
            check(i.readInt()==1960001,"corpus magic");
            for(;;){String name=i.readUTF();if(name.length()==0)break;i.readInt();int[] u=ints(i);i.readInt();int n=i.readInt();int[][] input=new int[n][];for(int k=0;k<n;k++)input[k]=ints(i);int[] expected=ints(i);ints(i);
                check(u[10]==3,"full mode only");records++;
                if(args.length>1&&records==1)expected[0]^=1;
                if(u[1]>256){row(name,u,input,expected,0,256,false);row(name,u,input,expected,256,u[1],false);row(name,u,input,expected,256,u[1],true);}
                else dispatch(name,u,input,expected);
                // Deliberately unaligned core rows, boundaries and real log rows.
                for(int y:new int[]{0,1,Math.min(32,u[1]-1),u[1]-1}){row(name,u,input,expected,y,y+1,false);singleRows++;}
                if(u[1]>278)for(int y:new int[]{94,255,256,278}){row(name,u,input,expected,y,y+1,false);singleRows++;}
            }
        }
        System.out.println("ENV "+GpuNoise1960.fingerprint());
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"records\":"+records+",\"reads\":"+reads+",\"compared_pixels\":"+pixels+",\"single_rows\":"+singleRows+",\"confidence_off_checks\":"+confidenceToggles+",\"argb_differences\":0,\"physical_android_tested\":false}");
    }
}
