package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;

/** Exact two-stage CPU finishing for the existing geometry-identity route.
 * Moire is rounded to ARGB integers before sharpening reads those neighbours.
 * Only a bounded band and its original/source and corrected halos are retained. */
final class CpuIdentityFinish1981 {
    private CpuIdentityFinish1981() {}
    private static final int CORE=256;
    private static final int SHARP_HALO=QualityShadow1932.RESIDUAL_RADIUS;
    private static final int SOURCE_HALO=QualityPixels1932.HALO+SHARP_HALO;
    private static boolean eligible(Bitmap bitmap,QualityPixels1932.Plan sharp) {
        if(bitmap==null||bitmap.isRecycled()||!bitmap.isMutable()||bitmap.getConfig()!=Bitmap.Config.ARGB_8888||
                bitmap.getWidth()<1||bitmap.getHeight()<1||!CpuFinishPolicy1978.immutable(sharp)||sharp.sharpLevel<1)return false;
        ColorSpace color=bitmap.getColorSpace();
        return (color==null||color.isSrgb())&&(Build.VERSION.SDK_INT<34||!bitmap.hasGainmap());
    }
    private static long workspace(int width,int height,int core,int workers) {
        return 8L*width*Math.min(height,core+2*SOURCE_HALO)*workers+4L*width*SOURCE_HALO+65536;
    }
    static String key(QualityPixels1932.Plan sharp,int width,int height,int core,int workers,boolean alpha) {
        return "cpu-identity-finish1981-exact2-time5-v1:"+width+":"+height+":"+core+":"+workers+":"+alpha+":"+
            sharp.sharpLevel+":"+sharp.sharpGainQ8+":"+sharp.sharpFloorQ8+":"+sharp.sharpLimit+":"+
            sharp.beautyQ8+":"+sharp.texturePriority+":"+sharp.haloSuppression+":"+
            Float.floatToRawIntBits(sharp.sourceSigma)+":"+Float.floatToRawIntBits(sharp.outputScale)+":"+
            sharp.noiseMapAtOutput+":"+(sharp.localNoise!=null)+":"+(sharp.faceRegions!=null)+":"+
            (sharp.smoothedRegions==null?"none":sharp.smoothedRegions.getClass().getName());
    }
    /** False is returned only before writing any pixel. A failed active stream
     * propagates to normalize's existing pristine-input rollback. */
    static boolean tryRun(final Bitmap bitmap,final QualityPixels1932.Plan moire,final QualityPixels1932.Plan sharp) {
        if(!eligible(bitmap,sharp))return false;
        final int width=bitmap.getWidth(),height=bitmap.getHeight(),core=Math.min(CORE,height);
        final int workers=Math.max(1,Math.min((height+core-1)/core,
            Math.min(SpeedWorkers1935.maxWorkers(),SpeedWorkers1935.availableWorkers1944())));
        final long memory=workspace(width,height,core,workers);
        final String key=key(sharp,width,height,core,workers,bitmap.hasAlpha());
        if(CpuExact1978.enabled(key)&&GpuNoise1960.workspaceFits(memory)) {
            // A translucent Bitmap may round through premultiplied storage
            // between the original passes. Such inputs keep the original route.
            ProcessingTiming1947.Token timing=ProcessingTiming1947.beginStage(
                ProcessingTiming1947.traceFor(bitmap),ProcessingTiming1947.CORRECTION);
            try {if(!FastResize1933.opaque(bitmap))return false;stream(bitmap,moire,sharp,core,workers);return true;}
            finally {ProcessingTiming1947.end(timing);}
        }
        long count=(long)width*height,held=CpuFinishPolicy1978.retainedPlan(sharp),sourceHeld=CpuFinishPolicy1978.retainedPlan(moire);
        if(sourceHeld<0||sourceHeld>Long.MAX_VALUE-held)return false;
        held+=sourceHeld;
        if(count>Integer.MAX_VALUE||held<0||held>96L*1024*1024-4L*count-4096)return false;
        CpuExact1978.offer(key,4L*count+held+4096,20L*count+memory*2+16L*1024*1024,new CpuExact1978.Factory(){
            public CpuExact1978.Work create() {
                Bitmap copy=bitmap.copy(Bitmap.Config.ARGB_8888,true);if(copy==null)return null;
                boolean accepted=false;
                try {
                    if(!FastResize1933.opaque(copy))return null;
                    Proof proof=new Proof(copy,moire,sharp,core,workers);accepted=true;return proof;
                } finally {if(!accepted)copy.recycle();}
            }
        });
        return false;
    }
    /** Same method is executed by the foreground, detached proof and host
     * complete-image differential. The scheduler still owns all worker joins. */
    static void stream(Bitmap bitmap,QualityPixels1932.Plan moire,QualityPixels1932.Plan sharp,int core,int count) {
        if(bitmap==null||bitmap.isRecycled()||!bitmap.isMutable()||bitmap.getConfig()!=Bitmap.Config.ARGB_8888||
                core<1||core>CORE||count<1||count>4||sharp==null)throw new IllegalArgumentException("CPU identity stream");
        Group group=null;Worker[] workers=null;
        try {
            group=new Group(bitmap,core);workers=new Worker[count];
            for(int i=0;i<count;i++)workers[i]=new Worker(group,moire,sharp);
            SpeedWorkers1935.run(workers);
            if(group.failure instanceof Error)throw (Error)group.failure;
            if(group.failure instanceof RuntimeException)throw (RuntimeException)group.failure;
            if(group.failure!=null)throw new IllegalStateException("CPU identity worker failure",group.failure);
        } finally {
            if(workers!=null)for(Worker worker:workers)if(worker!=null)worker.close();
            if(group!=null)SpeedWorkers1935.release(group.preceding);
        }
    }
    private static void interrupted(){if(Thread.currentThread().isInterrupted())throw new IllegalStateException("CPU identity interrupted");}
    private static final class Group {
        final Bitmap bitmap;final int width,height,core;final int[] preceding;
        int next,precedingRows;volatile Throwable failure;
        Group(Bitmap bitmap,int core) {
            this.bitmap=bitmap;this.core=core;width=bitmap.getWidth();height=bitmap.getHeight();
            preceding=SpeedWorkers1935.borrowInts(Math.multiplyExact(width,Math.min(height,SOURCE_HALO)));
        }
        synchronized boolean read(Worker worker) {
            interrupted();if(failure!=null||next>=height)return false;
            int first=next,count=Math.min(core,height-first),last=first+count;
            int neededTop=Math.max(0,first-SHARP_HALO),neededEnd=Math.min(height,last+SHARP_HALO);
            int top=Math.max(0,neededTop-QualityPixels1932.HALO),end=Math.min(height,neededEnd+QualityPixels1932.HALO);
            int preserved=first-top,rows=end-top;
            if(preserved!=precedingRows)throw new IllegalStateException("CPU identity original halo sequence");
            if(preserved>0)System.arraycopy(preceding,0,worker.input,0,preserved*width);
            bitmap.getPixels(worker.input,preserved*width,width,0,first,width,end-first);
            precedingRows=Math.min(SOURCE_HALO,last);
            System.arraycopy(worker.input,(last-top-precedingRows)*width,preceding,0,precedingRows*width);
            worker.first=first;worker.count=count;worker.top=top;worker.rows=rows;
            worker.neededTop=neededTop;worker.neededEnd=neededEnd;next=last;return true;
        }
        synchronized void write(Worker worker) {
            interrupted();if(failure==null)bitmap.setPixels(worker.input,
                (worker.first-worker.neededTop)*width,width,0,worker.first,width,worker.count);
        }
        synchronized void failed(Throwable error){if(failure==null)failure=error;}
    }
    private static final class Worker implements Runnable {
        final Group group;final QualityPixels1932.Plan moire,sharp;final int[] input,middle;
        int first,count,top,rows,neededTop,neededEnd;
        Worker(Group group,QualityPixels1932.Plan moire,QualityPixels1932.Plan sharp) {
            this.group=group;this.moire=moire;this.sharp=sharp;
            int size=Math.multiplyExact(group.width,Math.min(group.height,group.core+2*SOURCE_HALO));
            input=SpeedWorkers1935.borrowInts(size);
            try{middle=SpeedWorkers1935.borrowInts(size);}catch(Throwable failure){SpeedWorkers1935.release(input);throw failure;}
        }
        public void run() {
            try {
                while(group.read(this)) {
                    int width=group.width;
                    QualityPixels1932.finishStripAt(input,middle,width,rows,neededTop-top,neededEnd-top,moire,true,false,top);
                    // This exact integer intermediate replaces only the former
                    // Bitmap.setPixels/getPixels boundary, never the rounding.
                    int correctedRows=neededEnd-neededTop;
                    System.arraycopy(middle,(neededTop-top)*width,middle,0,correctedRows*width);
                    QualityPixels1932.finishStripAt(middle,input,width,correctedRows,first-neededTop,
                        first-neededTop+count,sharp,false,true,neededTop);
                    group.write(this);
                }
            }catch(Throwable failure){group.failed(failure);}
        }
        void close(){SpeedWorkers1935.release(input);SpeedWorkers1935.release(middle);}
    }
    private static final class Proof implements CpuExact1978.Work {
        Bitmap source;QualityPixels1932.Plan moire,sharp;final int core,workers;
        Proof(Bitmap source,QualityPixels1932.Plan moire,QualityPixels1932.Plan sharp,int core,int workers){this.source=source;this.moire=moire;this.sharp=sharp;this.core=core;this.workers=workers;}
        public Object run(boolean streamed) {
            if(source==null)return null;
            Bitmap result=source.copy(Bitmap.Config.ARGB_8888,true);if(result==null)return null;
            try {
                if(streamed) {
                    // hasAlpha=true makes this a complete image scan on the
                    // foreground route; its cost also belongs to admission.
                    if(!FastResize1933.opaque(result))return null;
                    stream(result,moire,sharp,core,workers);
                }
                else {QualityPipeline1932.finishInPlace(result,moire,true,false);QualityPipeline1932.finishInPlace(result,sharp,false,true);}
                int[] pixels=new int[Math.multiplyExact(result.getWidth(),result.getHeight())];
                result.getPixels(pixels,0,result.getWidth(),0,0,result.getWidth(),result.getHeight());return pixels;
            }finally{result.recycle();}
        }
        public void close(){try{if(source!=null)source.recycle();}finally{source=null;moire=null;sharp=null;}}
    }
}
