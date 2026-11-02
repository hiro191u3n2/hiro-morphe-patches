package com.hiro.ulike;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaMuxer;
import androidx.heifwriter.HeifWriter;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;

/** Observes the existing HEIF and publishing calls without modifying their arguments.
 * Encode is elapsed writer start through stop; its wait can overlap muxer IO. */
public final class TimedIo1947 {
    private TimedIo1947() { }
    private static final Object LOCK=new Object();
    private static final int MAX=128;
    private static final IdentityHashMap<Object,Encoding> WRITERS=new IdentityHashMap<Object,Encoding>();
    private static final IdentityHashMap<Object,ProcessingTiming1947.Trace> MUXERS=new IdentityHashMap<Object,ProcessingTiming1947.Trace>();
    private static final IdentityHashMap<Object,ProcessingTiming1947.Token> SPANS=new IdentityHashMap<Object,ProcessingTiming1947.Token>();
    private static final LinkedHashMap<String,ProcessingTiming1947.Trace> PATHS=new LinkedHashMap<String,ProcessingTiming1947.Trace>();
    private static final ThreadLocal<Legacy> LEGACY=new ThreadLocal<Legacy>();
    private static final class Legacy {String status;}
    private static final class Encoding {
        final ProcessingTiming1947.Trace trace;
        final ProcessingTiming1947.Token token;
        Encoding(ProcessingTiming1947.Trace value) {
            trace=value;token=ProcessingTiming1947.beginStage(value,ProcessingTiming1947.ENCODE);
        }
    }
    private static final class Saving {
        final ProcessingTiming1947.Trace trace;
        final ProcessingTiming1947.Scope scope;
        final boolean standalone;
        Saving(ProcessingTiming1947.Trace value,boolean owns) {
            trace=value;standalone=owns;scope=ProcessingTiming1947.enter(value);
        }
    }
    private static String key(String path) {
        if(path==null)return null;
        try{return new File(path).getCanonicalPath();}catch(Throwable ignored){return path;}
    }
    private static String key(File file) {
        if(file==null)return null;
        try{return file.getCanonicalPath();}catch(Throwable ignored){try{return file.getAbsolutePath();}catch(Throwable absent){return null;}}
    }
    private static void remember(String path,ProcessingTiming1947.Trace trace) {
        if(path==null||trace==null)return;
        try{synchronized(LOCK){PATHS.put(path,trace);while(PATHS.size()>MAX)PATHS.remove(PATHS.keySet().iterator().next());}}catch(Throwable ignored){}
    }
    private static Saving saving(Bitmap bitmap,File file) {
        try{
            ProcessingTiming1947.Trace current=ProcessingTiming1947.current();
            ProcessingTiming1947.Trace trace=current==null?ProcessingTiming1947.traceFor(bitmap):current;
            boolean standalone=current==null;
            if(trace==null||trace.state!=0)trace=ProcessingTiming1947.begin(bitmap);
            ProcessingTiming1947.bind(bitmap,trace);remember(key(file),trace);
            return new Saving(trace,standalone);
        }catch(Throwable ignored){return null;}
    }
    private static void saved(Saving save,boolean success) {
        if(save==null)return;
        try{if(save.standalone&&!success)ProcessingTiming1947.finish(save.trace,false);}
        finally{ProcessingTiming1947.restore(save.scope);}
    }
    public static boolean saveStage(Bitmap bitmap,File file,int quality) {
        Saving save=saving(bitmap,file);boolean success=false;
        try{success=SaveFd186.saveStageBefore1947(bitmap,file,quality);return success;}
        finally{saved(save,success);}
    }
    public static boolean saveFinal(Bitmap bitmap,File file,Bitmap.CompressFormat format,int quality) {
        Saving save=saving(bitmap,file);boolean success=false;
        try{success=SaveQuality2.saveFinalBefore1947(bitmap,file,format,quality);return success;}
        finally{saved(save,success);}
    }
    public static Bitmap legacyDetail(Bitmap bitmap,Bitmap reference) {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.traceFor(bitmap);
        ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(trace);
        Legacy prior=null,observed=new Legacy();int stage=-1;
        try{
            prior=LEGACY.get();LEGACY.set(observed);
            PhotoDetail.Settings settings=QualityPipeline1932.settingsForLegacy(PhotoDetail.snapshot1932());
            if(settings.noiseOn&&!settings.sharpOn)stage=ProcessingTiming1947.NOISE;
            else if(!settings.noiseOn&&settings.sharpOn)stage=ProcessingTiming1947.CORRECTION;
            else if(settings.noiseOn&&settings.sharpOn){
                ProcessingTiming1947.unmeasured(trace,ProcessingTiming1947.NOISE);
                ProcessingTiming1947.unmeasured(trace,ProcessingTiming1947.CORRECTION);
                ProcessingTiming1947.note(trace,"旧経路のノイズ・輪郭補正は一体処理のため内訳未計測");
            }else{
                ProcessingTiming1947.skip(trace,ProcessingTiming1947.NOISE);
                ProcessingTiming1947.skip(trace,ProcessingTiming1947.CORRECTION);
            }
        }catch(Throwable ignored){}
        long started=System.nanoTime();
        try{
            Bitmap result=PhotoDetail.applyDetailLegacy177Before1947(bitmap,reference);
            ProcessingTiming1947.transfer(bitmap,result);return result;
        }finally{
            long ended=System.nanoTime();
            try{
                if(stage>=0){
                    String status=observed.status;
                    if(status!=null&&status.startsWith("ノイズ低減：")&&status.contains("／追加処理 "))ProcessingTiming1947.recordInterval(trace,stage,started,ended);
                    else{
                        ProcessingTiming1947.unmeasured(trace,stage);
                        ProcessingTiming1947.note(trace,status==null?"旧経路の処理完了を確認できず内訳未計測":status);
                    }
                }
                if(prior==null)LEGACY.remove();else LEGACY.set(prior);
            }catch(Throwable ignored){}
            ProcessingTiming1947.restore(scope);
        }
    }
    /** Only the active fallback wrapper uses this status to distinguish actual
     * processing from BUSY/main-thread/format/memory bypasses. No old ms is displayed. */
    public static void legacyStatus(String status) {
        try{Legacy legacy=LEGACY.get();if(legacy!=null)legacy.status=status;}catch(Throwable ignored){}
        ProcessingTiming1947.legacyStatus(status);
    }
    private static Saving publishing(String path) {
        try{
            ProcessingTiming1947.Trace trace=ProcessingTiming1947.current();boolean standalone=trace==null;
            String exact=key(path);
            if(trace==null&&exact!=null)synchronized(LOCK){trace=PATHS.get(exact);}
            if(trace==null)trace=ProcessingTiming1947.begin(path);
            return new Saving(trace,standalone);
        }catch(Throwable ignored){return null;}
    }
    private static void published(Saving save,String path,String result) {
        if(save==null)return;
        try{
            boolean success=result!=null&&result.length()>0;
            if(save.standalone)ProcessingTiming1947.finish(save.trace,success);
            String exact=key(path);if(exact!=null)synchronized(LOCK){PATHS.remove(exact);}
        }catch(Throwable ignored){}
        finally{ProcessingTiming1947.restore(save.scope);}
    }
    public static String publishStage(String path) {
        Saving save=publishing(path);String result=null;
        ProcessingTiming1947.Token token=ProcessingTiming1947.beginStage(save==null?null:save.trace,ProcessingTiming1947.SAVE);
        try{result=SaveFd186.publishStageBefore1947(path);return result;}
        finally{ProcessingTiming1947.end(token);published(save,path,result);}
    }
    public static String publishFinal(String path) {
        Saving save=publishing(path);String result=null;
        ProcessingTiming1947.Token token=ProcessingTiming1947.beginStage(save==null?null:save.trace,ProcessingTiming1947.SAVE);
        try{result=SaveQuality2.publishFinalBefore1947(path);return result;}
        finally{ProcessingTiming1947.end(token);published(save,path,result);}
    }
    public static void start(HeifWriter writer) throws IOException {
        Encoding encoding=null;
        try{
            encoding=new Encoding(ProcessingTiming1947.current());
            synchronized(LOCK){if(WRITERS.size()<MAX)WRITERS.put(writer,encoding);}
        }catch(Throwable ignored){}
        boolean success=false;
        try{writer.start();success=true;}
        finally{if(!success)endEncoding(writer,encoding);}
    }
    public static void stop(HeifWriter writer,long timeout) throws Exception {
        try{writer.stop(timeout);}
        finally{endEncoding(writer,null);}
    }
    public static void close(HeifWriter writer) {
        try{CodecDrain1945.close(writer);}
        finally{endEncoding(writer,null);removeWriter(writer);}
    }
    private static void endEncoding(Object writer,Encoding fallback) {
        try{
            Encoding found;synchronized(LOCK){found=WRITERS.get(writer);}
            ProcessingTiming1947.end(found==null?(fallback==null?null:fallback.token):found.token);
        }catch(Throwable ignored){}
    }
    private static void removeWriter(Object writer) {
        try{synchronized(LOCK){Encoding encoding=WRITERS.remove(writer);if(encoding!=null)for(java.util.Iterator<ProcessingTiming1947.Trace> i=MUXERS.values().iterator();i.hasNext();)if(i.next()==encoding.trace)i.remove();}}
        catch(Throwable ignored){}
    }
    /** Injected at WriterBase.start: associates this precise writer and destination. */
    public static void bindMuxer(Object writer,MediaMuxer muxer) {
        if(writer==null||muxer==null)return;
        try{synchronized(LOCK){Encoding encoding=WRITERS.get(writer);if(encoding!=null&&MUXERS.size()<MAX)MUXERS.put(muxer,encoding.trace);}}
        catch(Throwable ignored){}
    }
    private static ProcessingTiming1947.Token io(MediaMuxer muxer) {
        try{synchronized(LOCK){return ProcessingTiming1947.beginStage(MUXERS.get(muxer),ProcessingTiming1947.SAVE);}}
        catch(Throwable ignored){return null;}
    }
    public static void writeSampleData(MediaMuxer muxer,int track,ByteBuffer data,MediaCodec.BufferInfo info) {
        ProcessingTiming1947.Token token=io(muxer);
        try{muxer.writeSampleData(track,data,info);}finally{ProcessingTiming1947.end(token);}
    }
    public static void muxerStart(MediaMuxer muxer) {
        ProcessingTiming1947.Token token=io(muxer);
        try{muxer.start();}finally{ProcessingTiming1947.end(token);}
    }
    public static void muxerStop(MediaMuxer muxer) {
        ProcessingTiming1947.Token token=io(muxer);
        try{muxer.stop();}finally{ProcessingTiming1947.end(token);}
    }
    public static void muxerRelease(MediaMuxer muxer) {
        ProcessingTiming1947.Token token=io(muxer);
        try{muxer.release();}finally{ProcessingTiming1947.end(token);}
    }
    public static void spanStarted(Object span,String phase) {
        if(!"verify".equals(phase)&&!"pending_publish".equals(phase))return;
        try{synchronized(LOCK){if(SPANS.size()<MAX)SPANS.put(span,ProcessingTiming1947.beginStage(ProcessingTiming1947.SAVE));}}
        catch(Throwable ignored){}
    }
    public static void spanClosed(Object span) {
        try{ProcessingTiming1947.Token token;synchronized(LOCK){token=SPANS.remove(span);}ProcessingTiming1947.end(token);}
        catch(Throwable ignored){}
    }
}
