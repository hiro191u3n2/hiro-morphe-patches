package com.hiro.ulike;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Explicit host control of renderer outcomes and elapsed times. Every accepted
 * comparison still traverses all rows through the production ResidentProof.
 * This fixture makes no claim about native pixels or Android performance. */
public final class FinishControl1986 {
    static final class Proof {
        final long cpu,gpu;final int variant;
        Proof(long c,long g,int v){cpu=c;gpu=g;variant=v;}
    }
    static long clock;
    static int cpuCalls,diagnosticFault,cancelMode,copyFailure;
    static boolean unstable,workspace;
    static Bitmap lastCpu;
    static long[] cpuTimes;
    static long[][] gpuTimes;
    static int[][] compareFault,outputFault;
    static int[] comparisons,outputs;
    static long[] comparedPixels;
    static final Map<String,Proof> proofs=new HashMap<String,Proof>();
    static final Set<String> exactRejections=new HashSet<String>(),speedRejections=new HashSet<String>();
    static final List<long[]> notes=new ArrayList<long[]>();
    static void reset(){
        clock=1;cpuCalls=diagnosticFault=cancelMode=copyFailure=0;unstable=false;workspace=true;lastCpu=null;
        cpuTimes=new long[]{1000,1000};gpuTimes=new long[][]{{800,800},{850,850},{700,700},{900,900}};
        compareFault=new int[4][2];outputFault=new int[4][2];comparisons=new int[4];outputs=new int[4];comparedPixels=new long[4];
        proofs.clear();exactRejections.clear();speedRejections.clear();notes.clear();Thread.interrupted();
    }
    public static long nanoTime(){return clock;}
    static boolean cancelled(){
        if(cancelMode==1)return true;
        if(cancelMode==2&&cpuCalls>0)return true;
        if(cancelMode==3&&lastCpu!=null&&lastCpu.reads>0&&Arrays.stream(comparisons).sum()==0)return true;
        if(cancelMode==6&&cpuCalls==2&&outputs[3]==2)return true;
        if(cancelMode==7&&cpuCalls>0)return true;
        return false;
    }
    static int pixel(int x,int y){return 0xff000000|((x*31+y*17)&255)<<16|((x*7+y*53)&255)<<8|((x*37+y*3)&255);}
    static Bitmap image(int width,int height,boolean change){
        Bitmap value=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        int[] row=new int[width];for(int y=0;y<height;y++){
            for(int x=0;x<width;x++)row[x]=pixel(x,y);
            if(change&&y==height-1)row[width-1]^=1;
            value.setPixels(row,0,width,0,y,width,1);
        }
        return value;
    }
    public static Bitmap cpu(Bitmap input,int rotation,int width,int height,QualityPixels1932.Plan plan,boolean refresh){
        int trial=cpuCalls++;
        try {
            clock+=cpuTimes[trial];
            if(copyFailure==1)return null;
            if(copyFailure==2)throw new OutOfMemoryError("controlled CPU output allocation");
            if(cancelMode==7)throw new IllegalStateException("controlled CPU failure coincides with cancellation");
            lastCpu=image(width,height,unstable&&trial==1);return lastCpu;
        } finally {if(!input.isRecycled())input.recycle();}
    }
    private static void fault(int code){
        if(code==3)throw new IllegalStateException("controlled renderer failure");
        if(code==4)throw new UnsatisfiedLinkError("controlled renderer entry");
        if(code==5)throw new OutOfMemoryError("controlled renderer allocation");
        if(code==6)throw new java.util.concurrent.CancellationException("controlled renderer cancellation");
    }
    public static int compare(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,boolean refresh,
            Bitmap expected,int tileRows,boolean pipeline,GpuNoise1960.Session carried,boolean identityNoiseBefore,
            GpuQualification1961.Cancellation cancellation){
        int index=pipeline?1+(tileRows==32?0:tileRows==64?1:2):0;
        int trial=comparisons[index]++,code=compareFault[index][trial];fault(code);
        if(cancelMode==4&&index==1)throw new java.util.concurrent.CancellationException("cancel during full comparison");
        if(code==1)return ResidentProof1978.UNAVAILABLE;
        ResidentProof1978 proof=new ResidentProof1978(expected,cancellation);
        int[] row=new int[width];
        for(int y=0;y<height;y++){
            for(int x=0;x<width;x++)row[x]=pixel(x,y);
            if(code==2&&y==height-1)row[width-1]^=1;
            if(!proof.rows(y,1,row,0))break;
        }
        comparedPixels[index]+=proof.pixelsCompared();return proof.result();
    }
    public static Bitmap output(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,
            boolean refresh,boolean pipeline,int tileRows){
        int index=pipeline?1+(tileRows==32?0:tileRows==64?1:2):0;
        int trial=outputs[index]++,code=outputFault[index][trial];clock+=gpuTimes[index][trial];fault(code);
        if(cancelMode==5&&index==1)throw new java.util.concurrent.CancellationException("cancel during output");
        if(code==1)return null;
        return image(code==2?width-1:width,height,false);
    }
    static void note(int candidate,int reason,int exact,int speed,int reference,long cpu,long legacy,long gpu){
        if(diagnosticFault==1)throw new IllegalStateException("diagnostic fault");
        if(diagnosticFault==2)throw new LinkageError("diagnostic link fault");
        if(diagnosticFault==3)throw new OutOfMemoryError("diagnostic allocation fault");
        notes.add(new long[]{candidate,reason,exact,speed,reference,cpu,legacy,gpu});
    }
    static long[] last(int candidate){for(int i=notes.size()-1;i>=0;i--)if(notes.get(i)[0]==candidate)return notes.get(i);return null;}
}
