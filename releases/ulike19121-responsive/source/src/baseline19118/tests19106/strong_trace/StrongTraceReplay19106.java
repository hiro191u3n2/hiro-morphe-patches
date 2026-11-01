package com.hiro.ulike;
import java.io.*;
import java.util.*;
/** Real JNI program60 trace versus uninstrumented program39 and CPU raw checkpoint file. */
public final class StrongTraceReplay19106 {
    static long assertions,comparedWords,comparedPixels;static int records,negatives;
    static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static int[] ints(DataInputStream in)throws Exception{int n=in.readInt();int[] a=new int[n];for(int x=0;x<n;x++)a[x]=in.readInt();return a;}
    static void equal(int[] a,int[] b,String why){check(a!=null&&b!=null&&a.length==b.length,"length "+why);for(int x=0;x<a.length;x++)check(a[x]==b[x],why+" index="+x+" expected="+Integer.toHexString(a[x])+" actual="+Integer.toHexString(b[x]));}
    static int mismatch(int[] a,int[] b){for(int id=0;id<2048;id++)if(a[id+2048]!=b[id+2048]||(a[id+2048]==1&&a[id]!=b[id]))return id;return -1;}
    static int[][] dispatch(int program,int[] u,int[][] data)throws Exception{
        GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"GPU session");try{
            for(int slot:new int[]{0,2,3,4,5,6})check(s.upload(slot,data[slot]),"upload slot "+slot);
            GpuNoise1960.Batch b=new GpuNoise1960.Batch().allocate(1,4L*u[0]).upload(7,new int[4096]).dispatch(program,new int[]{0,1,2,3,4,5,6,7},u,null,u[0]);
            GpuNoise1960.Ticket t=s.submit(b,0);check(t!=null,"submit "+program);int[][] out=s.collect(t,new int[]{1,7},new int[]{u[0],4096});check(out!=null,"collect "+program);return out;
        }finally{s.close();}
    }
    public static void main(String[] args)throws Exception{
        boolean standard=GpuNoise1960.supports(39),tracing=GpuNoise1960.supports(60);check(standard&&tracing,"both shader programs standard="+standard+" trace="+tracing+" standardFailure="+GpuNoise1960.programFailure19100(39)+" traceFailure="+GpuNoise1960.programFailure19100(60));
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])))){
            check(in.readInt()==1910601,"trace corpus magic");for(;;){String name=in.readUTF();if(name.isEmpty())break;int[] u=ints(in);int[][] data=new int[8][];data[0]=ints(in);data[3]=ints(in);data[2]=ints(in);for(int slot=4;slot<=6;slot++)data[slot]=ints(in);int[] expected=ints(in),trace=ints(in);
                int[][] baseline=dispatch(39,u,data),actual=dispatch(60,u,data);equal(expected,baseline[0],"production / CPU row "+name);equal(baseline[0],actual[0],"trace instrumentation / production row "+name);comparedPixels+=u[0]*2L;
                int difference=mismatch(trace,actual[1]);check(difference==-1,"raw checkpoint "+name+" id="+difference+(difference<0?"":" expected="+Integer.toHexString(trace[difference])+" actual="+Integer.toHexString(actual[1][difference])+" presence="+trace[2048+difference]+"/"+actual[1][2048+difference]));
                for(int id=0;id<2048;id++){check(actual[1][2048+id]==0||actual[1][2048+id]==1,"valid GPU presence");if(trace[2048+id]==1)comparedWords++;}
                check(actual[1][93]==actual[0][u[30]],"trace packed value reproduces selected output");
                if(records==0){
                    int[] corrupt=actual[1].clone();corrupt[93]^=1;check(mismatch(trace,corrupt)==93,"negative arithmetic bit mutation");negatives++;
                    corrupt=actual[1].clone();corrupt[2048+93]=0;check(mismatch(trace,corrupt)==93,"negative missing presence");negatives++;
                    corrupt=actual[1].clone();corrupt[31]^=1;check(mismatch(trace,corrupt)==31,"negative branch mutation");negatives++;
                    corrupt=actual[1].clone();corrupt[2047]=999;check(mismatch(trace,corrupt)==-1,"absent value is not observed");negatives++;
                    int[] out=actual[0].clone();out[u[30]]^=1;check(out[u[30]]!=actual[1][93],"negative instrumentation output mutation");negatives++;
                }
                records++;
            }
        }
        System.out.println("ENV "+GpuNoise1960.fingerprint());
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"records\":"+records+",\"compared_checkpoint_words\":"+comparedWords+",\"compared_row_pixels\":"+comparedPixels+",\"negative_controls\":"+negatives+",\"raw_differences\":0}");
    }
}
