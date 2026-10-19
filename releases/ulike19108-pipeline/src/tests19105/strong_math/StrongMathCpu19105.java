package com.hiro.ulike;
import java.io.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Production native JNI and the explicit Java fallback share one fixed band. */
public final class StrongMathCpu19105 {
    static long assertions,words;static int rows,records,nativeRows,javaRows;
    static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static int[] ints(DataInputStream i)throws Exception{int n=i.readInt();int[] a=new int[n];for(int x=0;x<n;x++)a[x]=i.readInt();return a;}
    static void row(String name,int y,int[] u,int[][] in,int[] expected,StrongNoise1958.Model model,boolean nativeAvailable){
        int w=u[0],lo=Math.max(0,y-18),hi=Math.min(u[1],y+19);int[] q=u.clone();
        q[1]=hi-lo;q[2]=y-lo;q[3]=q[2]+1;q[4]=0;q[5]=hi-lo;q[6]=lo;q[12]=0;
        int[] source=Arrays.copyOfRange(in[0],lo*w,hi*w),policy=Arrays.copyOfRange(in[3],y*w*2,(y+1)*w*2),original=source.clone(),oldPolicy=policy.clone();
        for(boolean javaOnly:new boolean[]{false,true}){
            int[] output=new int[source.length];Arrays.fill(output,0x12345678);
            int route=StrongNoise1958.diagnosticCpu19105(source,output,q,model,policy,javaOnly);
            check(route==(javaOnly?2:nativeAvailable?1:3),"observed CPU route "+route);if(route==1)nativeRows++;else javaRows++;
            for(int x=0;x<output.length;x++){int want=x>=q[2]*w&&x<q[3]*w?expected[y*w+x-q[2]*w]:0x12345678;check(output[x]==want,name+" y="+y+" at="+x+" Java="+javaOnly);words++;}
            check(Arrays.equals(original,source)&&Arrays.equals(oldPolicy,policy),"fixed source and policy unchanged");
        }
        if(rows==0){
            int[] output=new int[source.length];Arrays.fill(output,0xabcdef01);Thread.currentThread().interrupt();boolean cancelled=false;
            try{StrongNoise1958.diagnosticCpu19105(source,output,q,model,policy,false);}catch(CancellationException expectedCancel){cancelled=true;}finally{Thread.interrupted();}
            check(cancelled,"cancellation propagates");for(int x:output)check(x==0xabcdef01,"cancelled output unchanged");
            int[] invalid=q.clone();invalid[3]++;boolean rejected=false;try{StrongNoise1958.diagnosticCpu19105(source,output,invalid,model,policy,true);}catch(IllegalArgumentException expectedInvalid){rejected=true;}check(rejected,"multirow rejected");
        }
        rows++;
    }
    public static void main(String[] args)throws Exception{
        boolean nativeAvailable=args[1].equals("native");check(StrongNoise1958.nativeAvailable()==nativeAvailable,"actual native availability");
        try(DataInputStream i=new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])))){
            check(i.readInt()==1960001,"corpus magic");
            for(;;){String name=i.readUTF();if(name.length()==0)break;i.readInt();int[] u=ints(i);i.readInt();int n=i.readInt();int[][] in=new int[n][];for(int k=0;k<n;k++)in[k]=ints(i);int[] expected=ints(i);ints(i);records++;
                float[] ev=new float[in[2].length];for(int k=0;k<ev.length;k++)ev[k]=Float.intBitsToFloat(in[2][k]);
                StrongNoise1958.Model model=StrongNoise1958.model1961(u[0],u[7],in[4],in[5],in[6],Arrays.copyOf(ev,48),ev);
                for(int y:new int[]{0,1,Math.min(32,u[1]-1),u[1]-1})row(name,y,u,in,expected,model,nativeAvailable);
                if(u[1]>278)for(int y:new int[]{94,255,256,278})row(name,y,u,in,expected,model,nativeAvailable);
            }
        }
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"records\":"+records+",\"rows\":"+rows+",\"native_rows\":"+nativeRows+",\"java_rows\":"+javaRows+",\"compared_words\":"+words+",\"physical_android_tested\":false}");
    }
}
