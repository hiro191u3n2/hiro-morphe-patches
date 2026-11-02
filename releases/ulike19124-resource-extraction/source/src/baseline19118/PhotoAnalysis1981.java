package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Read-only analysis of the exact owned photo while another photo corrects.
 * It never borrows SDK storage, changes pixels or replaces post-filter evidence. */
public final class PhotoAnalysis1981 {
    private static final long ANALYSIS_BUDGET=32L*1024*1024;
    private static final AtomicLong RETAINED=new AtomicLong();
    private static final ThreadPoolExecutor ANALYSIS=new ThreadPoolExecutor(1,1,15L,TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(1),new ThreadFactory(){public Thread newThread(Runnable work){
            Thread thread=new Thread(work,"ULike-photo-analysis1981");thread.setDaemon(true);thread.setPriority(Thread.MIN_PRIORITY);return thread;
        }},new ThreadPoolExecutor.AbortPolicy());
    static{ANALYSIS.allowCoreThreadTimeOut(true);}
    private PhotoAnalysis1981(){}

    static final class Context {
        final Bitmap input;
        final int rotation,direction,width,height,generation,density;
        final boolean alpha,premultiplied,owned,colour,fixed,needsFaces,needsSpatial,reduceFirst,readable;
        final Bitmap.Config config;
        final ColorSpace colourSpace;
        final PhotoDetail.Settings settings;
        final ShotContext1932.Snapshot metadata;
        final ProcessingTiming1947.Trace timing;
        final SaveMemory1981.Plan geometry;
        private boolean offered,running,finished,cancelled,closed,faceDone,noiseDone,reserved;
        private FaceResult1981 faces;
        private SpatialNoise1934 noise;

        Context(Bitmap input,int rotation,int direction,PhotoDetail.Settings settings,boolean colour,boolean fixed,
                ShotContext1932.Snapshot metadata,ProcessingTiming1947.Trace timing,boolean owned,SaveMemory1981.Plan geometry) {
            this.input=input;this.rotation=turn(rotation);this.direction=direction;this.settings=settings;
            this.colour=colour;this.fixed=fixed;this.metadata=metadata;this.timing=timing;this.owned=owned;this.geometry=geometry;
            width=input.getWidth();height=input.getHeight();generation=input.getGenerationId();density=input.getDensity();
            alpha=input.hasAlpha();premultiplied=input.isPremultiplied();config=input.getConfig();colourSpace=input.getColorSpace();
            readable=readable(input);
            int rw=this.rotation==90||this.rotation==270?height:width,rh=this.rotation==90||this.rotation==270?width:height;
            reduceFirst=geometry!=null&&Math.max((float)geometry.outputWidth/rw,(float)geometry.outputHeight/rh)<0.999999f;
            boolean needsNoise=settings!=null&&settings.noiseOn&&settings.noiseLevel>0;
            boolean needsSharp=settings!=null&&settings.sharpOn&&settings.sharpLevel>0;
            needsFaces=needsNoise||needsSharp&&settings.texturePriority;
            needsSpatial=(needsNoise||needsSharp)&&!reduceFirst;
        }
        boolean matches(Bitmap bitmap,int turn) {
            if(bitmap!=input||this.rotation!=turn||closed||bitmap.isRecycled()||bitmap.getGenerationId()!=generation
                    ||bitmap.getWidth()!=width||bitmap.getHeight()!=height||bitmap.getConfig()!=config||bitmap.getDensity()!=density
                    ||bitmap.hasAlpha()!=alpha||bitmap.isPremultiplied()!=premultiplied||!readable(bitmap))return false;
            ColorSpace current=bitmap.getColorSpace();return colourSpace==null?current==null:colourSpace.equals(current);
        }
        synchronized boolean offer() {
            if(offered||closed||!owned||!readable||geometry==null||!geometry.valid()||(!needsFaces&&!needsSpatial))return false;
            offered=true;reserved=true;RETAINED.addAndGet(ANALYSIS_BUDGET);return true;
        }
        synchronized boolean start(){if(closed||cancelled||finished)return false;running=true;return true;}
        synchronized void failBeforeStart(){if(!running){cancelled=true;finished=true;faceDone=true;noiseDone=true;releaseBudget();notifyAll();}}
        private void releaseBudget(){if(reserved){reserved=false;RETAINED.addAndGet(-ANALYSIS_BUDGET);}}
        synchronized void completeFaces(FaceResult1981 result){faces=result;faceDone=true;notifyAll();}
        synchronized void completeNoise(SpatialNoise1934 result){noise=result;noiseDone=true;notifyAll();}
        synchronized void finished(){running=false;finished=true;faceDone=true;noiseDone=true;notifyAll();}
        private synchronized void await(boolean face) {
            if(!running&&!finished){cancelled=true;finished=true;faceDone=true;noiseDone=true;releaseBudget();notifyAll();}
            boolean interrupted=false;
            while(running&&!(face?faceDone:noiseDone))try{wait();}catch(InterruptedException wait){interrupted=true;}
            if(interrupted)Thread.currentThread().interrupt();
        }
        FaceRegions1934.Mask faces(Bitmap bitmap,int turn) {
            await(true);
            synchronized(this){return matches(bitmap,turn)&&faces!=null&&faces.completed?faces.mask:null;}
        }
        SpatialNoise1934 spatial(Bitmap bitmap) {
            await(false);
            synchronized(this){return !reduceFirst&&matches(bitmap,rotation)?noise:null;}
        }
        void closeAndAwait1981() {
            boolean interrupted=false;
            synchronized(this) {
                closed=true;cancelled=true;
                if(!running){finished=true;faceDone=true;noiseDone=true;}
                while(running)try{wait();}catch(InterruptedException wait){interrupted=true;}
                faces=null;noise=null;releaseBudget();notifyAll();
            }
            if(interrupted)Thread.currentThread().interrupt();
        }
        synchronized boolean cancelled(){return cancelled||closed;}
    }
    public static long retainedBytes1981(){return RETAINED.get();}
    private static int turn(int rotation){int turn=rotation==-1?0:(rotation%360+360)%360;if(turn!=0&&turn!=90&&turn!=180&&turn!=270)throw new IllegalArgumentException("photo rotation");return turn;}
    private static boolean readable(Bitmap bitmap) {
        if(bitmap==null||bitmap.isRecycled())return false;
        Bitmap.Config config=bitmap.getConfig();
        if(config!=Bitmap.Config.ARGB_8888&&config!=Bitmap.Config.RGB_565)return false;
        ColorSpace space=bitmap.getColorSpace();
        return (space==null||space.isSrgb())&&(Build.VERSION.SDK_INT<34||!bitmap.hasGainmap());
    }
    static void offer1981(final Context context,final Runnable job) {
        if(context==null||!SaveQueue1935.analysisWaiting1981(job)||!SpeedWorkers1935.cpuIdle1944()
                ||!AsyncSave1935.analysisMemoryAllows1981(ANALYSIS_BUDGET)||!context.offer())return;
        try {ANALYSIS.execute(new Runnable(){public void run(){analyze(context,job);}});}
        catch(RuntimeException failure){context.failBeforeStart();}
        catch(LinkageError failure){context.failBeforeStart();}
        catch(OutOfMemoryError failure){context.failBeforeStart();}
    }
    private static void analyze(Context context,Runnable job) {
        boolean cpu=false,started=false;
        ProcessingTiming1947.Scope scope=null;
        try {
            if(!SaveQueue1935.analysisWaiting1981(job)||!context.matches(context.input,context.rotation))return;
            cpu=SpeedWorkers1935.tryEnterAnalysis1981();if(!cpu)return;
            if(!context.start())return;started=true;
            scope=ProcessingTiming1947.enter(context.timing);
            if(context.needsFaces&&!context.cancelled()) {
                ProcessingTiming1947.Token timing=ProcessingTiming1947.beginStage(context.timing,ProcessingTiming1947.CORRECTION);
                FaceResult1981 result;
                try{result=FaceResult1981.analyze(context.input,context.rotation);}
                finally{ProcessingTiming1947.end(timing);}
                context.completeFaces(result);
            }else context.completeFaces(null);
            if(context.needsSpatial&&!context.cancelled())context.completeNoise(probe(context.input,context.timing));
            else context.completeNoise(null);
        }catch(RuntimeException optional){}catch(LinkageError optional){}catch(OutOfMemoryError optional){}
        finally {
            try{if(scope!=null)ProcessingTiming1947.restore(scope);}
            finally {
                try{SpeedWorkers1935.leaveAnalysis1981(cpu);}
                finally{if(started)context.finished();else context.failBeforeStart();}
            }
        }
    }
    /** The caller retains its normal face-stage interval. Valid zero faces is a
     * non-null completed mask; failed speculative work invokes the original. */
    public static FaceRegions1934.Mask faces1981(Bitmap input,int rotation) {
        Context context=AsyncSave1935.photoContext1981(input);
        FaceRegions1934.Mask ready=context==null?null:context.faces(input,rotation);
        return ready!=null?ready:FaceRegions1934.forBitmap(input,rotation);
    }
    /** Hook ONLY the initial probe in normalize(). Cropped/reduced inputs and
     * every post-filter probe retain their original pixels and operation order. */
    public static SpatialNoise1934 spatial1981(Bitmap input) {
        Context context=AsyncSave1935.photoContext1981(input);
        SpatialNoise1934 ready=context==null?null:context.spatial(input);
        return ready!=null?ready:probe(input,ProcessingTiming1947.traceFor(input));
    }
    private static SpatialNoise1934 probe(final Bitmap bitmap,ProcessingTiming1947.Trace trace) {
        ProcessingTiming1947.Token timing=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.NOISE);
        try{return SpatialNoise1934.probe(new SpatialNoise1934.Patches(){public void read(int[] pixels,int x,int y,int width,int height){bitmap.getPixels(pixels,0,width,x,y,width,height);}},bitmap.getWidth(),bitmap.getHeight());}
        finally{ProcessingTiming1947.end(timing);}
    }
}
