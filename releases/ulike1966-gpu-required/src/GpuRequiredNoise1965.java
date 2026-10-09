package com.hiro.ulike;

import java.util.concurrent.CancellationException;

/** Mandatory-GPU image-domain single-frame noise path. Host code copies image
 * buffers and immutable model metadata; every noise estimate, coefficient,
 * residual, safeguard, mask and final pixel calculation executes in shaders.
 * There is no live-photo CPU oracle, residual preparation or CPU fallback.
 * A caller may retry the entire immutable shot after the private EGL context
 * is reset. This class throws on an incomplete result, never commits a partial
 * strip, and never converts a capability/error condition into untreated success.
 */
public final class GpuRequiredNoise1965 {
    private GpuRequiredNoise1965() {}

    private static void interrupted() {
        if(Thread.currentThread().isInterrupted())
            throw new CancellationException("GPU-required noise cancelled");
    }
    private static void require(boolean completed,String stage) {
        interrupted();
        if(!completed)throw new GpuRequiredFailure1965("noise",stage+": "+GpuNoise1960.failure1965());
    }
    private static int[] programs() {
        interrupted();
        if(GpuNoise1960.fp64Verified1965()&&GpuNoise1960.supports(GpuNoise1960.SINGLE)&&
                GpuNoise1960.supports(GpuNoise1960.MODEL1965))
            return new int[]{GpuNoise1960.MODEL1965,GpuNoise1960.SINGLE};
        require(GpuNoise1960.soft64Verified1965(),"exact GPU binary64 arithmetic unavailable");
        require(GpuNoise1960.supports(GpuNoise1960.MODEL_SOFT1965),"portable binary64 noise model unavailable");
        require(GpuNoise1960.supports(GpuNoise1960.SINGLE_SOFT1965),"portable binary64 transform unavailable");
        return new int[]{GpuNoise1960.MODEL_SOFT1965,GpuNoise1960.SINGLE_SOFT1965};
    }
    /** The patch locations are unchanged command metadata. Source.read only
     * copies original pixels; it must not render a CPU noise/correction result.
     * The compact immutable sample buffer bounds upload memory independently
     * of photograph resolution and avoids uploading a full frame for evidence.
     */
    public static SingleNoise1955.Model probe(SingleNoise1955.Patches source,int width,int height) {
        if(source==null||width<1||height<1)throw new IllegalArgumentException("GPU noise source");
        int[] programs=programs();
        GpuRequired1965.note("noise-model","precision="+(programs[0]==GpuNoise1960.MODEL1965?
            "hardware-binary64":"portable-binary64-on-GPU")+" shader="+programs[0]);
        int pw=Math.min(48,width),ph=Math.min(48,height);
        int columns=Math.max(1,Math.min(13,(width+255)/256));
        int rows=Math.max(1,Math.min(13,(height+255)/256)),cells=columns*rows;
        int patchCount=pw*ph,blocks=(pw/8)*(ph/8),modelCount=cells*3+88;
        int[] samples=new int[cells*patchCount],patch=new int[patchCount];
        for(int gy=0;gy<rows;gy++)for(int gx=0;gx<columns;gx++) {
            interrupted();
            int px=columns==1?(width-pw)/2:Math.round((float)(width-pw)*gx/(columns-1));
            int py=rows==1?(height-ph)/2:Math.round((float)(height-ph)*gy/(rows-1));
            source.read(patch,px,py,pw,ph);
            System.arraycopy(patch,0,samples,(gy*columns+gx)*patchCount,patchCount);
        }
        long bytes=4L*samples.length+4L*cells*2048+4L*72*1024+
            12L*Math.max(1,cells*blocks)+4L*(modelCount+3)+1048576L;
        require(bytes<=GpuNoise1960.MAX_BYTES&&GpuNoise1960.workspaceFits(bytes),"noise model workspace");
        GpuNoise1960.Session session=GpuNoise1960.open();
        require(session!=null,"noise model session");
        try {
            int[] u=new int[32];u[1]=width;u[2]=height;u[3]=columns;u[4]=rows;
            u[5]=pw;u[6]=ph;u[7]=pw/8;u[8]=ph/8;
            int[] bindings={0,1,2,3,4,5};
            require(session.upload(0,samples)&&session.allocate(1,4L*cells*2048)&&
                session.allocate(2,4L*72*1024)&&session.allocate(3,8L*Math.max(1,cells*blocks))&&
                session.allocate(4,4L*Math.max(1,cells*blocks))&&session.allocate(5,4L*(modelCount+3)),
                "noise model buffers");
            u[0]=0;require(session.dispatch(programs[0],bindings,u,null,
                Math.max(cells*2048,Math.max(72*1024,cells*blocks))),"noise model histograms clear");
            if(blocks>0){u[0]=1;require(session.dispatch(programs[0],bindings,u,null,cells*blocks),"noise model binary64 coefficients");}
            u[0]=2;require(session.dispatch(programs[0],bindings,u,null,cells),"noise model local statistics");
            u[0]=3;require(session.dispatch(programs[0],bindings,u,null,1),"noise model spectra");
            int[] words=session.readInts(5,modelCount+3);require(words!=null,"noise model readback");
            float[] data=new float[modelCount];
            for(int i=0;i<modelCount;i++)data[i]=Float.intBitsToFloat(words[i]);
            float globalY=Float.intBitsToFloat(words[modelCount]),globalC=Float.intBitsToFloat(words[modelCount+1]);
            int accepted=words[modelCount+2];
            interrupted();
            return SingleNoise1955.fromGpu1965(width,height,columns,rows,accepted,pw,ph,data,globalY,globalC);
        } finally {session.close();}
    }

    /** A strip commits only after every GPU operation and full readback succeeds.
     * Source/output must be distinct, source is immutable, and HALO rows are
     * present except at physical image borders, matching the original NR route.
     */
    public static void processRange(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            SingleNoise1955.Model model,QualityPixels1932.Plan plan) {
        if(input==null||output==null||input==output||model==null||width<1||rows<1||width!=model.width||
            noise<0||noise>4||validBegin<0||validEnd>rows||begin<validBegin||end<begin||end>validEnd||
            (long)width*rows>input.length||(long)width*rows>output.length||
            (long)originY+validBegin<0||(long)originY+validEnd>model.height)
            throw new IllegalArgumentException("GPU-required noise strip");
        if(begin==end)return;
        if(noise==0){System.arraycopy(input,begin*width,output,begin*width,(end-begin)*width);return;}
        if(originY+validBegin>Math.max(0,originY+begin-SingleNoise1955.HALO)||
            originY+validEnd<Math.min(model.height,originY+end+SingleNoise1955.HALO))
            throw new IllegalArgumentException("GPU-required noise immutable halo");
        int[] programs=programs();
        if(originY+begin==0)GpuRequired1965.note("single-noise","precision="+
            (programs[1]==GpuNoise1960.SINGLE?"hardware-binary64":"portable-binary64-on-GPU")+
            " shader="+programs[1]);
        long count=(long)width*(end-begin),blockCols=(width+3L)/4+1;
        long firstBy=(long)Math.floorDiv(originY+begin-7,4)*4;
        long blockRows=((long)originY+end-firstBy+3)/4,blocks=blockCols*blockRows;
        long scratchBytes=blocks*992L,total=scratchBytes+8L*input.length+count*44L+
            4L*model.gpuData1960().length+1048576L;
        require(count<=Integer.MAX_VALUE/2&&blocks<=Integer.MAX_VALUE&&scratchBytes<=GpuNoise1960.MAX_BYTES&&
            total<=GpuNoise1960.MAX_BYTES&&GpuNoise1960.workspaceFits(total),"FP64 transform workspace");
        GpuNoise1960.Session session=GpuNoise1960.open();require(session!=null,"FP64 transform session");
        try {
            require(session.upload(0,input)&&session.allocate(1,count*4L)&&
                session.upload(2,model.gpuData1960())&&session.allocate(4,scratchBytes),"FP64 transform buffers");
            require(GpuRequiredPolicy1965.preparePolicy1965(session,plan,width,end-begin,originY+begin,
                false,3,5,6,7,8,9),"GPU noise protection");
            int[] u=new int[32];u[1]=width;u[2]=model.height;u[3]=rows;u[4]=begin;u[5]=end;
            u[6]=originY;u[7]=validBegin;u[8]=validEnd;u[9]=model.columns;u[10]=model.rows;
            u[11]=model.gpuPatchWidth1960();u[12]=model.gpuPatchHeight1960();u[13]=noise;u[14]=shadows?1:0;
            u[15]=1;u[16]=(int)firstBy;u[17]=(int)blockCols;u[18]=(int)blockRows;
            int[] bindings={0,1,2,3,4};
            u[0]=0;require(session.dispatch(programs[1],bindings,u,null,(int)blocks),"binary64 transforms and residual safeguards");
            u[0]=1;require(session.dispatch(programs[1],bindings,u,null,(int)count),"noise ordered accumulation");
            int[] candidate=session.readInts(1,(int)count);require(candidate!=null,"noise full strip readback");
            interrupted();System.arraycopy(candidate,0,output,begin*width,(int)count);
        } finally {session.close();}
    }
}
