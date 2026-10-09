package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import java.util.LinkedHashMap;

/** GX20: source-domain moire correction feeds exact geometry on the same GPU
 * session. The intermediate ARGB image is never downloaded or re-uploaded.
 * Subsequent output-noise measurement and output-only sharpening keep their
 * established order. Full private CPU/GPU proof gates this optional route. */
final class GpuChain1961 {
    private GpuChain1961() {}
    private static final LinkedHashMap<String,Boolean> REJECTED=new LinkedHashMap<String,Boolean>();
    static Bitmap moireGeometry(final Bitmap source,final int rotation,final int width,final int height) {
        if(!eligible(source,rotation,width,height)||GpuNoise1960.sessionBusy()||
                GpuQualification1961.background()||!GpuNoise1960.supports(GpuNoise1960.FINISH1961)||
                !GpuNoise1960.supports(GpuNoise1960.GEOMETRY))return null;
        final String key=GpuNoise1960.fingerprint()+"|moire-geometry|"+source.getWidth()+","+source.getHeight()+","+rotation+","+width+","+height;
        synchronized(REJECTED){if(REJECTED.containsKey(key))return null;}
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null) {
            try {
                long start=System.nanoTime();Bitmap result=run(source,rotation,width,height);long elapsed=System.nanoTime()-start;
                if(result==null){reject(key);return null;}
                if(elapsed>proof.cpuNanos-proof.cpuNanos/20)GpuQualification1961.reject(key);
                return result;
            } catch(DeferredChain unavailable){return null;}
              catch(RuntimeException unavailable){reject(key);return null;}
              catch(LinkageError unavailable){reject(key);return null;}
              catch(OutOfMemoryError unavailable){return null;}
        }
        long retained=4L*source.getWidth()*source.getHeight();
        if(retained>96L*1024*1024||!GpuNoise1960.workspaceFits(retained*3+8L*width*height))return null;
        Bitmap snapshot=null;
        try {
            snapshot=source.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)return null;
            final Bitmap owned=snapshot;
            GpuQualification1961.schedule(key,retained,new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation cancellation) {
                    long cpu=Long.MAX_VALUE,gpu=0;
                    for(int trial=0;trial<2;trial++) {
                        if(cancellation.cancelled())return;
                        Bitmap expected=null,candidate=null;
                        try {
                            long started=System.nanoTime();expected=cpu(owned,rotation,width,height);long cpuTime=System.nanoTime()-started;
                            if(cancellation.cancelled())return;
                            started=System.nanoTime();candidate=GpuChain1961.run(owned,rotation,width,height);long gpuTime=System.nanoTime()-started;
                            if(cancellation.cancelled())return;
                            if(candidate==null||!GpuGeometry1960.equal1961(expected,candidate)){reject(key);return;}
                            if(cpuTime<=0||gpuTime<=0||gpuTime>cpuTime-cpuTime/20)return;
                            cpu=Math.min(cpu,cpuTime);gpu=Math.max(gpu,gpuTime);
                        } finally {if(expected!=null&&expected!=owned)expected.recycle();if(candidate!=null&&candidate!=owned)candidate.recycle();}
                    }
                    if(!cancellation.cancelled()&&gpu<=cpu-cpu/20)GpuQualification1961.qualified(key,cpu,gpu,0);
                }
                public void close(){if(!owned.isRecycled())owned.recycle();}
            });
            snapshot=null;
        } catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        finally{if(snapshot!=null&&!snapshot.isRecycled())snapshot.recycle();}
        return null;
    }
    private static final class DeferredChain extends RuntimeException {}
    private static void reject(String key){GpuQualification1961.reject(key);synchronized(REJECTED){REJECTED.put(key,Boolean.TRUE);while(REJECTED.size()>24)REJECTED.remove(REJECTED.keySet().iterator().next());}}
    private static boolean eligible(Bitmap source,int rotation,int width,int height) {
        if(source==null||source.isRecycled()||source.getConfig()!=Bitmap.Config.ARGB_8888||width<=0||height<=0||
                rotation!=0&&rotation!=90&&rotation!=180&&rotation!=270||
                rotation==0&&source.getWidth()==width&&source.getHeight()==height)return false;
        ColorSpace color=source.getColorSpace();return (color==null||color.isSrgb())&&
            !(Build.VERSION.SDK_INT>=34&&source.hasGainmap())&&
            (long)source.getWidth()*source.getHeight()<=Integer.MAX_VALUE&&(long)width*height<=Integer.MAX_VALUE;
    }
    static Bitmap cpu(Bitmap source,int rotation,int width,int height) {
        int sw=source.getWidth(),sh=source.getHeight();int[] input=new int[Math.multiplyExact(sw,sh)],output=new int[input.length];
        source.getPixels(input,0,sw,0,0,sw,sh);
        QualityPixels1932.finishStripAtBefore1951(input,output,sw,sh,0,sh,null,true,false,0);
        Bitmap corrected=Bitmap.createBitmap(sw,sh,Bitmap.Config.ARGB_8888);
        try {corrected.setPixels(output,0,sw,0,0,sw,sh);corrected.setDensity(source.getDensity());corrected.setHasAlpha(source.hasAlpha());
            return FastResize1933.resampleCpu1960(corrected,rotation,width,height);
        } finally {corrected.recycle();}
    }
    static Bitmap run(Bitmap source,int rotation,int width,int height) {
        GpuPolicy1960.GeometryData data=GpuPolicy1960.geometry(source.getWidth(),source.getHeight(),rotation,width,height);
        int sw=source.getWidth(),sh=source.getHeight(),n=Math.multiplyExact(sw,sh),out=Math.multiplyExact(width,height);
        long extra=16L*n+4L*out+4L*(data.tables.length+data.weights.length)+
            16L*width*64+65536L;
        if(!GpuNoise1960.workspaceFits(extra))throw new DeferredChain();
        int[] pixels=new int[n];source.getPixels(pixels,0,sw,0,0,sw,sh);
        for(int p:pixels)if((p>>>24)!=255)return null;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new DeferredChain();
        try {
            int[] finish=new int[32];finish[1]=sw;finish[2]=sh;finish[5]=sh;finish[8]=1;
            GpuNoise1960.Batch batch=new GpuNoise1960.Batch().upload(0,pixels).allocate(1,4L*n)
                .allocate(3,4).allocate(5,4)
                .upload(6,data.weights).upload(7,data.tables)
                .dispatch(GpuNoise1960.FINISH1961,new int[]{0,0,1,3},finish,null,n);
            if(!session.run(batch))return null;
            int[] geometryBindings={1,2,4,5,6,7};
            Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);boolean done=false;
            try {
                int[] base=data.uniforms(0);
                for(int first=0;first<height;) {
                    if(Thread.currentThread().isInterrupted()||GpuQualification1961.cancelled())return null;
                    int rows=Math.min(32,height-first),lo,hi;
                    for(;;) {
                        lo=Integer.MAX_VALUE;hi=-1;
                        if(data.exactCrop){lo=first+base[7];hi=lo+rows;}
                        else {
                            for(int y=first;y<first+rows;y++)for(int t=data.tables[base[10]+y];t<data.tables[base[10]+y+1];t++)
                                {int row=data.tables[base[11]+t];lo=Math.min(lo,row);hi=Math.max(hi,row);}
                            hi++;
                        }
                        long intermediate=data.exactCrop?4:12L*width*(hi-lo);
                        if(lo<0||hi<=lo||hi>base[14])return null;
                        if(intermediate<=GpuNoise1960.MAX_BYTES&&GpuNoise1960.workspaceFits(intermediate+12L*width*rows+65536L))break;
                        if(rows==1)throw new DeferredChain();rows=Math.max(1,rows/2);
                    }
                    int count=Math.multiplyExact(width,rows);int[] u=base.clone();
                    u[15]=first;u[16]=first+rows;u[17]=lo;u[18]=hi-lo;u[20]=1;u[21]=1;
                    batch=new GpuNoise1960.Batch().allocate(2,4L*count).allocate(4,data.exactCrop?4:12L*width*(hi-lo));
                    if(data.exactCrop)batch.dispatch(GpuNoise1960.GEOMETRY,geometryBindings,u,null,count);
                    else {
                        u[0]=1;batch.dispatch(GpuNoise1960.GEOMETRY,geometryBindings,u,null,Math.multiplyExact(width,hi-lo));
                        u[0]=2;batch.dispatch(GpuNoise1960.GEOMETRY,geometryBindings,u,null,count);
                    }
                    int[][] read=session.execute(batch,new int[]{2},new int[]{count});if(read==null||Thread.currentThread().isInterrupted())return null;
                    result.setPixels(read[0],0,width,0,first,width,rows);first+=rows;
                }
                result.setDensity(source.getDensity());result.setHasAlpha(source.hasAlpha());done=true;return result;
            }
            finally{if(!done)result.recycle();}
        } finally {session.close();}
    }
}
