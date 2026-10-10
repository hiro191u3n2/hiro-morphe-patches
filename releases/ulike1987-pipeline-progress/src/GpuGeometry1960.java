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
        int matched;long cpuNanos,gpuNanos;boolean admitted,busy;
        State(String e,int sw,int sh,int r,int w,int h){environment=e;this.sw=sw;this.sh=sh;rotation=r;width=w;height=h;}
        boolean same(String e,int sw,int sh,int r,int w,int h){return environment.equals(e)&&this.sw==sw&&this.sh==sh&&rotation==r&&width==w&&height==h;}
    }
    static Bitmap resample(Bitmap bitmap,int rotation,int width,int height,Cpu cpu) {
        if(GpuQualification1961.background()||!eligible(bitmap,rotation,width,height)||GpuNoise1960.sessionBusy()||!GpuNoise1960.supports(GpuNoise1960.GEOMETRY))return cpu.run();
        String environment=GpuNoise1960.fingerprint();if(environment==null||environment.isEmpty())return cpu.run();
        final State state;final boolean fallback;
        synchronized(STATES) {
            State found=null;for(State candidate:STATES)if(candidate!=null&&candidate.same(environment,bitmap.getWidth(),bitmap.getHeight(),rotation,width,height)){found=candidate;break;}
            if(found==null){found=new State(environment,bitmap.getWidth(),bitmap.getHeight(),rotation,width,height);STATES[victim++%STATES.length]=found;}
            if(!found.admitted&&!GpuQualification1961.exactRejected(key1961(found))) {
                GpuQualification1961.Record proof=GpuQualification1961.restore(key1961(found));
                if(proof!=null){found.admitted=true;found.matched=2;found.cpuNanos=proof.cpuNanos*2;found.gpuNanos=proof.gpuNanos*2;}
            }
            state=found;fallback=GpuQualification1961.exactRejected(key1961(state))||state.busy;if(!fallback)state.busy=true;
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
                    if(elapsed>baseline-baseline/20){state.admitted=false;state.matched=0;GpuQualification1961.rejectSpeed(key1961(state));}
                    Bitmap result=candidate;candidate=null;return result;
                }
                state.admitted=false;GpuQualification1961.rejectSpeed(key1961(state));return cpu.run();
            }
            // GX23: foreground emits the unchanged CPU result. Full geometry
            // proof, inclusive route timing and shader work happen while idle.
            reference=cpu.run();interrupted();schedule1961(bitmap,state);
            Bitmap result=reference;reference=null;return result;
        } catch(DeferredGpu unavailable){interrupted();if(reference!=null){Bitmap result=reference;reference=null;return result;}return cpu.run();}
          catch(CancellationException cancelled){throw cancelled;}
          catch(RuntimeException optionalFailure){state.admitted=false;GpuQualification1961.rejectSpeed(key1961(state));interrupted();if(reference!=null){Bitmap result=reference;reference=null;return result;}return cpu.run();}
          catch(LinkageError optionalFailure){state.admitted=false;GpuQualification1961.rejectSpeed(key1961(state));interrupted();if(reference!=null){Bitmap result=reference;reference=null;return result;}return cpu.run();}
          catch(OutOfMemoryError optionalFailure){state.admitted=false;interrupted();if(reference!=null){Bitmap result=reference;reference=null;return result;}return cpu.run();}
        finally {
            if(candidate!=null&&candidate!=bitmap&&!candidate.isRecycled())candidate.recycle();
            if(reference!=null&&reference!=bitmap&&!reference.isRecycled())reference.recycle();
            synchronized(STATES){state.busy=false;}
        }
    }
    private static String key1961(State state){return state.environment+"|geometry|"+state.sw+","+state.sh+","+state.rotation+","+state.width+","+state.height;}
    private static void schedule1961(Bitmap bitmap,final State state) {
        // Only one idle proof can own a snapshot. Skip the full-frame copy
        // before the scheduler declines it because another proof is pending.
        if(!GpuQualification1961.maySchedule(key1961(state))||GpuQualification1961.retainedBytes()!=0)return;
        long bytes=4L*bitmap.getWidth()*bitmap.getHeight();
        if(bytes>96L*1024*1024||!GpuNoise1960.workspaceFits(bytes+8L*state.width*state.height))return;
        Bitmap snapshot=null;
        try {
            snapshot=bitmap.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)return;
            final Bitmap owned=snapshot;
            GpuQualification1961.schedule(key1961(state),bytes,new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation cancellation) {
                    synchronized(STATES){if(state.busy||GpuQualification1961.exactRejected(key1961(state))||cancellation.cancelled())return;state.busy=true;}
                    long cpu=Long.MAX_VALUE,gpuTime=0;boolean passed=false;
                    try {
                        for(int trial=0;trial<2;trial++) {
                            if(cancellation.cancelled())return;
                            Bitmap expected=null,candidate=null;
                            try {
                                long start=System.nanoTime();expected=FastResize1933.resampleCpu1960(owned,state.rotation,state.width,state.height);long cpuNanos=System.nanoTime()-start;
                                if(cancellation.cancelled())return;
                                start=System.nanoTime();candidate=gpu(owned,state.rotation,state.width,state.height);long gpuNanos=System.nanoTime()-start;
                                if(cancellation.cancelled())return;
                                if(candidate==null){GpuQualification1961.rejectSpeed(key1961(state));return;}
                                if(!equal(expected,candidate)){synchronized(STATES){state.admitted=false;}GpuQualification1961.reject(key1961(state));return;}
                                if(cpuNanos<=0||gpuNanos<=0||gpuNanos>cpuNanos-cpuNanos/20){GpuQualification1961.rejectSpeed(key1961(state));return;}
                                cpu=Math.min(cpu,cpuNanos);gpuTime=Math.max(gpuTime,gpuNanos);
                            } finally {if(expected!=null&&expected!=owned&&!expected.isRecycled())expected.recycle();if(candidate!=null&&candidate!=owned&&!candidate.isRecycled())candidate.recycle();}
                        }
                        if(cancellation.cancelled())return;
                        if(gpuTime>cpu-cpu/20){GpuQualification1961.rejectSpeed(key1961(state));return;}
                        GpuQualification1961.qualified(key1961(state),cpu,gpuTime,0);
                        if(GpuQualification1961.restore(key1961(state))==null)return;
                        synchronized(STATES){if(cancellation.cancelled())return;state.cpuNanos=cpu*2;state.gpuNanos=gpuTime*2;state.matched=2;state.admitted=true;passed=true;}
                    } catch(DeferredGpu unavailable) { }
                      catch(CancellationException cancelled){throw cancelled;}
                      catch(RuntimeException failure){if(!cancellation.cancelled())GpuQualification1961.rejectSpeed(key1961(state));}
                      catch(LinkageError failure){if(!cancellation.cancelled())GpuQualification1961.rejectSpeed(key1961(state));}
                      catch(OutOfMemoryError unavailable) { }
                    finally {synchronized(STATES){state.busy=false;}}
                }
                public void close(){if(!owned.isRecycled())owned.recycle();}
            });
            snapshot=null;
        } catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        finally{if(snapshot!=null&&!snapshot.isRecycled())snapshot.recycle();}
    }
    private static void interrupted(){if(Thread.currentThread().isInterrupted()||GpuQualification1961.cancelled())throw new CancellationException("GPU geometry cancelled before commit");}
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
        if(nativeBytes>GpuNoise1960.MAX_BYTES||!GpuNoise1960.workspaceFits(javaBytes+nativeBytes))
            return gpuTiled(source,rotation,width,height,data);
        int[] input=new int[sourceCount];source.getPixels(input,0,source.getWidth(),0,0,source.getWidth(),source.getHeight());
        // Actual pixels, not hasAlpha or sample points, prove opaque eligibility.
        int alpha=-1;for(int p:input)alpha&=p;if((alpha>>>24)!=255)throw new DeferredGpu();
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new DeferredGpu();
        try {
            GpuNoise1960.Batch batch=new GpuNoise1960.Batch().upload(0,input).allocate(1,4L*outputCount)
                .allocate(2,data.exactCrop?4:12L*data.horizontalInvocations).allocate(3,4)
                .upload(4,data.weights).upload(5,data.tables);
            int[] bindings={0,1,2,3,4,5};float[] floats=new float[32];
            if(data.exactCrop) {
                batch.dispatch(GpuNoise1960.GEOMETRY,bindings,data.uniforms(0),floats,data.outputInvocations);
            } else batch.dispatch(GpuNoise1960.GEOMETRY,bindings,data.uniforms(1),floats,data.horizontalInvocations)
                .dispatch(GpuNoise1960.GEOMETRY,bindings,data.uniforms(2),floats,data.outputInvocations);
            int[][] read=session.execute(batch,new int[]{1},new int[]{outputCount});
            int[] pixels=read==null?null:read[0];if(pixels==null||Thread.currentThread().isInterrupted())return null;
            Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);boolean complete=false;
            try {
                result.setPixels(pixels,0,width,0,0,width,height);result.setDensity(source.getDensity());result.setHasAlpha(source.hasAlpha());
                complete=true;return result;
            } finally {if(!complete)result.recycle();}
        } finally {session.close();}
    }
    /** GX15: exact global coefficients and every original tap are preserved.
     * Windows include all referenced source rows, including the full filter
     * halo. Only storage is tiled; neither precision nor output size changes. */
    private static Bitmap gpuTiled(Bitmap source,int rotation,int width,int height,
            GpuPolicy1960.GeometryData data) {
        int[] base=data.uniforms(0);
        int rw=rotation==90||rotation==270?source.getHeight():source.getWidth();
        long fixed=4L*(data.tables.length+data.weights.length)+4L*width*height+65536L;
        if(fixed>GpuNoise1960.MAX_BYTES||!GpuNoise1960.workspaceFits(fixed+1048576L))throw new DeferredGpu();
        // Match the whole-image opacity admission of the original GPU route.
        int scanRows=Math.min(16,source.getHeight());
        int[] scan=new int[Math.multiplyExact(source.getWidth(),scanRows)];
        for(int y=0;y<source.getHeight();y+=scanRows) {
            interrupted();int n=Math.min(scanRows,source.getHeight()-y);
            source.getPixels(scan,0,source.getWidth(),0,y,source.getWidth(),n);
            for(int i=0;i<source.getWidth()*n;i++)if((scan[i]>>>24)!=255)throw new DeferredGpu();
        }
        scan=null;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new DeferredGpu();
        Bitmap result=null;boolean complete=false;
        try {
            if(!session.run(new GpuNoise1960.Batch().upload(4,data.weights).upload(5,data.tables).allocate(3,4)))return null;
            result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            int[] bindings={0,1,2,3,4,5};float[] floats=new float[32];
            for(int first=0;first<height;) {
                interrupted();int rows=Math.min(32,height-first),lo=0,hi=0;
                for(;;) {
                    if(data.exactCrop){lo=first+base[7];hi=lo+rows;}
                    else {
                        lo=Integer.MAX_VALUE;hi=-1;
                        for(int y=first;y<first+rows;y++) {
                            int begin=data.tables[base[10]+y],end=data.tables[base[10]+y+1];
                            for(int t=begin;t<end;t++){int row=data.tables[base[11]+t];lo=Math.min(lo,row);hi=Math.max(hi,row);}
                        }
                        hi++;
                    }
                    if(lo<0||hi<=lo||hi>base[14])return null;
                    long inputCount=(long)rw*(hi-lo),outputCount=(long)width*rows;
                    long intermediate=data.exactCrop?4:12L*width*(hi-lo);
                    long peak=fixed+20L*inputCount+12L*outputCount+intermediate;
                    if(inputCount<=Integer.MAX_VALUE&&outputCount<=Integer.MAX_VALUE&&
                            intermediate<=GpuNoise1960.MAX_BYTES&&GpuNoise1960.workspaceFits(peak))break;
                    if(rows==1)throw new DeferredGpu();rows=Math.max(1,rows/2);
                }
                int[] input=rotatedWindow(source,rotation,lo,hi);
                for(int value:input)if((value>>>24)!=255)throw new DeferredGpu();
                int count=Math.multiplyExact(width,rows);
                GpuNoise1960.Batch batch=new GpuNoise1960.Batch().upload(0,input).allocate(1,4L*count)
                    .allocate(2,data.exactCrop?4:12L*width*(hi-lo));
                int[] u=base.clone();u[15]=first;u[16]=first+rows;u[17]=lo;u[18]=hi-lo;u[19]=1;u[20]=1;
                if(data.exactCrop) {
                    u[0]=0;batch.dispatch(GpuNoise1960.GEOMETRY,bindings,u,floats,count);
                } else {
                    u[0]=1;batch.dispatch(GpuNoise1960.GEOMETRY,bindings,u,floats,Math.multiplyExact(width,hi-lo));
                    u[0]=2;batch.dispatch(GpuNoise1960.GEOMETRY,bindings,u,floats,count);
                }
                int[][] read=session.execute(batch,new int[]{1},new int[]{count});int[] pixels=read==null?null:read[0];if(pixels==null)return null;
                interrupted();result.setPixels(pixels,0,width,0,first,width,rows);first+=rows;
            }
            result.setDensity(source.getDensity());result.setHasAlpha(source.hasAlpha());complete=true;return result;
        } finally {session.close();if(!complete&&result!=null&&!result.isRecycled())result.recycle();}
    }
    private static int[] rotatedWindow(Bitmap source,int rotation,int first,int end) {
        int sw=source.getWidth(),sh=source.getHeight(),rows=end-first;
        int rw=rotation==90||rotation==270?sh:sw;
        int[] input=new int[Math.multiplyExact(rw,rows)];
        if(rotation==0){source.getPixels(input,0,sw,0,first,sw,rows);return input;}
        int rectWidth=rotation==180?sw:rows,rectHeight=rotation==180?rows:sh;
        int left=rotation==90?first:rotation==270?sw-end:0;
        int top=rotation==180?sh-end:0;
        int[] rect=new int[input.length];source.getPixels(rect,0,rectWidth,left,top,rectWidth,rectHeight);
        for(int y=0;y<rows;y++) {
            interrupted();
            for(int x=0;x<rw;x++) {
                int at=rotation==180?(rows-1-y)*sw+sw-1-x:
                    rotation==90?(sh-1-x)*rows+y:x*rows+rows-1-y;
                input[y*rw+x]=rect[at];
            }
        }
        return input;
    }
    static boolean equal1961(Bitmap a,Bitmap b) {return equal(a,b);}
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
