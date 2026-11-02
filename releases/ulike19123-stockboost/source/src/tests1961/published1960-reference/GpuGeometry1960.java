package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import java.util.concurrent.CancellationException;

/** GX6 exact opaque saved-photo geometry. Candidate has no access to the camera,
 * preview, beauty engine or encoder. A bounded, process-local driver/geometry
 * gate requires two complete CPU/GPU pixel matches and a5% inclusive speed win;
 * unsupported color/alpha/memory/compiler conditions preserve the original path. */
public final class GpuGeometry1960 {
    private GpuGeometry1960() {}
    interface Cpu {Bitmap run();}
    private static final long RESERVE=64L*1024*1024;
    private static final State[] STATES=new State[24];
    private static int victim;
    /** Busy context, changing opacity and temporary memory pressure are not
     * evidence that this driver/geometry kernel is wrong. */
    private static final class DeferredGpu extends RuntimeException {}
    private static final class State {
        final String environment;final int sw,sh,rotation,width,height;
        int matched;long cpuNanos,gpuNanos;boolean admitted,rejected,busy;
        State(String e,int sw,int sh,int r,int w,int h){environment=e;this.sw=sw;this.sh=sh;rotation=r;width=w;height=h;}
        boolean same(String e,int sw,int sh,int r,int w,int h){return environment.equals(e)&&this.sw==sw&&this.sh==sh&&rotation==r&&width==w&&height==h;}
    }
    static Bitmap resample(Bitmap bitmap,int rotation,int width,int height,Cpu cpu) {
        if(!eligible(bitmap,rotation,width,height)||GpuNoise1960.sessionBusy()||!GpuNoise1960.supports(GpuNoise1960.GEOMETRY))return cpu.run();
        String environment=GpuNoise1960.fingerprint();if(environment==null||environment.isEmpty())return cpu.run();
        final State state;final boolean fallback;
        synchronized(STATES) {
            State found=null;for(State candidate:STATES)if(candidate!=null&&candidate.same(environment,bitmap.getWidth(),bitmap.getHeight(),rotation,width,height)){found=candidate;break;}
            if(found==null){found=new State(environment,bitmap.getWidth(),bitmap.getHeight(),rotation,width,height);STATES[victim++%STATES.length]=found;}
            state=found;fallback=state.rejected||state.busy;if(!fallback)state.busy=true;
        }
        if(fallback)return cpu.run();
        Bitmap candidate=null,reference=null;
        try {
            if(state.admitted) {
                long start=System.nanoTime();
                candidate=gpu(bitmap,rotation,width,height);
                long elapsed=System.nanoTime()-start;interrupted();
                if(candidate!=null){
                    long baseline=state.cpuNanos/Math.max(1,state.matched);
                    if(elapsed>=baseline-baseline/20){state.rejected=true;state.admitted=false;}
                    Bitmap result=candidate;candidate=null;return result;
                }
                state.rejected=true;state.admitted=false;return cpu.run();
            }
            // Qualification remains private. GPU timing includes the exact axis
            // descriptor, full source upload, intermediate storage, fence/readback
            // and actual candidate Bitmap creation, never just shader dispatch.
            long cpuElapsed,gpuElapsed;
            if((state.matched&1)==0) {
                long start=System.nanoTime();reference=cpu.run();cpuElapsed=System.nanoTime()-start;
                start=System.nanoTime();candidate=gpu(bitmap,rotation,width,height);gpuElapsed=System.nanoTime()-start;
            } else {
                long start=System.nanoTime();candidate=gpu(bitmap,rotation,width,height);gpuElapsed=System.nanoTime()-start;
                start=System.nanoTime();reference=cpu.run();cpuElapsed=System.nanoTime()-start;
            }
            interrupted();
            if(candidate==null||!equal(reference,candidate)||cpuElapsed<=0||gpuElapsed<=0||
                gpuElapsed>=cpuElapsed-cpuElapsed/20)state.rejected=true;
            else {
                state.matched++;state.cpuNanos+=cpuElapsed;state.gpuNanos+=gpuElapsed;
                if(state.matched>=2) {
                    state.admitted=true;
                }
            }
            Bitmap result=reference;reference=null;return result;
        } catch(DeferredGpu unavailable){interrupted();if(reference!=null){Bitmap result=reference;reference=null;return result;}return cpu.run();}
          catch(CancellationException cancelled){throw cancelled;}
          catch(RuntimeException optionalFailure){state.rejected=true;interrupted();if(reference!=null){Bitmap result=reference;reference=null;return result;}return cpu.run();}
          catch(LinkageError optionalFailure){state.rejected=true;interrupted();if(reference!=null){Bitmap result=reference;reference=null;return result;}return cpu.run();}
          catch(OutOfMemoryError optionalFailure){state.rejected=true;interrupted();if(reference!=null){Bitmap result=reference;reference=null;return result;}return cpu.run();}
        finally {
            if(candidate!=null&&candidate!=bitmap&&!candidate.isRecycled())candidate.recycle();
            if(reference!=null&&reference!=bitmap&&!reference.isRecycled())reference.recycle();
            synchronized(STATES){state.busy=false;}
        }
    }
    private static void interrupted(){if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU geometry cancelled before commit");}
    private static boolean eligible(Bitmap b,int rotation,int width,int height) {
        if(b==null||b.isRecycled()||b.getConfig()!=Bitmap.Config.ARGB_8888||width<1||height<1||
            rotation!=0&&rotation!=90&&rotation!=180&&rotation!=270||
            rotation==0&&width==b.getWidth()&&height==b.getHeight())return false;
        ColorSpace color=b.getColorSpace();if(color!=null&&!color.isSrgb()||Build.VERSION.SDK_INT>=34&&b.hasGainmap())return false;
        return (long)b.getWidth()*b.getHeight()<=Integer.MAX_VALUE&&(long)width*height<=Integer.MAX_VALUE;
    }
    private static long available(){Runtime r=Runtime.getRuntime();return r.maxMemory()-(r.totalMemory()-r.freeMemory());}
    private static Bitmap gpu(Bitmap source,int rotation,int width,int height) {
        GpuPolicy1960.GeometryData data=GpuPolicy1960.geometry(source.getWidth(),source.getHeight(),rotation,width,height);
        int sourceCount=source.getWidth()*source.getHeight(),outputCount=width*height;
        long nativeBytes=4L*sourceCount+4L*outputCount+data.workspaceBytes()+65536L;
        long javaBytes=4L*sourceCount+12L*outputCount+4L*(data.tables.length+data.weights.length);
        if(nativeBytes>GpuNoise1960.MAX_BYTES||!GpuNoise1960.workspaceFits(javaBytes+nativeBytes))throw new DeferredGpu();
        int[] input=new int[sourceCount];source.getPixels(input,0,source.getWidth(),0,0,source.getWidth(),source.getHeight());
        // Actual pixels, not hasAlpha or sample points, prove opaque eligibility.
        int alpha=-1;for(int p:input)alpha&=p;if((alpha>>>24)!=255)throw new DeferredGpu();
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new DeferredGpu();
        try {
            if(!session.upload(0,input)||!session.allocate(1,4L*outputCount)||
                !session.allocate(2,data.exactCrop?4:12L*data.horizontalInvocations)||
                !session.allocate(3,4)||!session.upload(4,data.weights)||!session.upload(5,data.tables))return null;
            int[] bindings={0,1,2,3,4,5};float[] floats=new float[32];
            if(data.exactCrop) {
                if(!session.dispatch(GpuNoise1960.GEOMETRY,bindings,data.uniforms(0),floats,data.outputInvocations))return null;
            } else if(!session.dispatch(GpuNoise1960.GEOMETRY,bindings,data.uniforms(1),floats,data.horizontalInvocations)||
                !session.dispatch(GpuNoise1960.GEOMETRY,bindings,data.uniforms(2),floats,data.outputInvocations))return null;
            int[] pixels=session.readInts(1,outputCount);if(pixels==null||Thread.currentThread().isInterrupted())return null;
            Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);boolean complete=false;
            try {
                result.setPixels(pixels,0,width,0,0,width,height);result.setDensity(source.getDensity());result.setHasAlpha(source.hasAlpha());
                complete=true;return result;
            } finally {if(!complete)result.recycle();}
        } finally {session.close();}
    }
    private static boolean equal(Bitmap a,Bitmap b) {
        if(a==null||b==null||a.isRecycled()||b.isRecycled()||a.getWidth()!=b.getWidth()||a.getHeight()!=b.getHeight())return false;
        int width=a.getWidth(),height=a.getHeight(),rows=Math.min(32,height);
        int[] left=new int[width*rows],right=new int[left.length];
        for(int y=0;y<height;y+=rows) {
            if(Thread.currentThread().isInterrupted())return false;
            int n=Math.min(rows,height-y);a.getPixels(left,0,width,0,y,width,n);b.getPixels(right,0,width,0,y,width,n);
            for(int i=0;i<width*n;i++)if(left[i]!=right[i])return false;
        }
        return true;
    }
}
