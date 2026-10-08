#!/usr/bin/env python3
"""Run actual production IO observers against deterministic alias/encoder fixtures."""
from pathlib import Path
import json
import subprocess

FIXTURES = {
 'android/graphics/Bitmap.java': '''package android.graphics;public class Bitmap {public enum CompressFormat {JPEG,PNG,WEBP} }''',
 'android/media/MediaCodec.java': '''package android.media;public class MediaCodec {public static class BufferInfo {} }''',
 'android/media/MediaMuxer.java': '''package android.media;import java.nio.ByteBuffer;public class MediaMuxer {public int writes,starts,stops,releases;public RuntimeException failure;public void writeSampleData(int t,ByteBuffer b,MediaCodec.BufferInfo i){writes++;if(failure!=null)throw failure;}public void start(){starts++;}public void stop(){stops++;}public void release(){releases++;}}''',
 'androidx/heifwriter/HeifWriter.java': '''package androidx.heifwriter;import android.media.MediaMuxer;import java.io.IOException;import com.hiro.ulike.TimedIo1947;public class HeifWriter {public final MediaMuxer muxer=new MediaMuxer();public int starts,stops,closes;public IOException startFailure;public Exception stopFailure;public long timeout;public void start()throws IOException{starts++;TimedIo1947.bindMuxer(this,muxer);if(startFailure!=null)throw startFailure;}public void stop(long t)throws Exception{stops++;timeout=t;if(stopFailure!=null)throw stopFailure;}public void close(){closes++;}}''',
 'com/hiro/ulike/CodecDrain1945.java': '''package com.hiro.ulike;import androidx.heifwriter.HeifWriter;public class CodecDrain1945 {public static void close(HeifWriter w){w.close();}}''',
 'com/hiro/ulike/PhotoDetail.java': '''package com.hiro.ulike;import android.graphics.Bitmap;public class PhotoDetail {public static String status="ノイズ低減：最強／追加処理 10ms";public static Settings settings=new Settings();public static Settings snapshot1932(){return settings;}public static Bitmap applyDetailLegacy177Before1947(Bitmap b,Bitmap r){TimedIo1947.legacyStatus(status);return b;}public static class Settings {public boolean noiseOn,sharpOn;}}''',
 'com/hiro/ulike/QualityPipeline1932.java': '''package com.hiro.ulike;public class QualityPipeline1932 {public static PhotoDetail.Settings settingsForLegacy(PhotoDetail.Settings s){return s;}}''',
 'com/hiro/ulike/SaveFd186.java': '''package com.hiro.ulike;import android.graphics.Bitmap;import java.io.File;public class SaveFd186 {public static Bitmap bitmap;public static File file;public static int quality;public static boolean result=true;public static RuntimeException failure;public static boolean saveStageBefore1947(Bitmap b,File f,int q){bitmap=b;file=f;quality=q;if(failure!=null)throw failure;return result;}public static String publishStageBefore1947(String p){return "saved:"+p;}}''',
 'com/hiro/ulike/SaveQuality2.java': '''package com.hiro.ulike;import android.graphics.Bitmap;import java.io.File;public class SaveQuality2 {public static Bitmap.CompressFormat format;public static boolean saveFinalBefore1947(Bitmap b,File f,Bitmap.CompressFormat c,int q){format=c;return TimedIo1947.saveStage(b,f,q);}public static String publishFinalBefore1947(String p){return TimedIo1947.publishStage(p);}}''',
 'com/hiro/ulike/TimedIo1947Test.java': r'''package com.hiro.ulike;
import android.graphics.Bitmap;import android.media.MediaMuxer;import android.media.MediaCodec;import androidx.heifwriter.HeifWriter;import java.io.File;import java.io.IOException;import java.nio.ByteBuffer;import java.util.List;
public class TimedIo1947Test {
 static int checks;static void yes(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
 static int intervals(ProcessingTiming1947.Trace trace,int stage)throws Exception{Object value=((Object[])trace.stages)[stage];java.lang.reflect.Field f=value.getClass().getDeclaredField("intervals");f.setAccessible(true);return ((List)f.get(value)).size();}
 public static void main(String[]a)throws Exception{
  Bitmap photo=new Bitmap();File file=new File("/tmp/ulike-io1947.heic");
  yes(TimedIo1947.saveFinal(photo,file,Bitmap.CompressFormat.PNG,97),"alias return preserved");
  yes(SaveFd186.bitmap==photo&&SaveFd186.file==file&&SaveFd186.quality==97&&SaveQuality2.format==Bitmap.CompressFormat.PNG,"all original args identity/quality retained");
  ProcessingTiming1947.Trace standalone=ProcessingTiming1947.traceFor(photo);
  yes(standalone!=null&&standalone.state==0&&ProcessingTiming1947.current()==null,"standalone remains pending until publish and restores scope");
  yes(TimedIo1947.publishFinal(file.getPath()).equals("saved:"+file.getPath()),"publish return preserved");
  yes(standalone.state==1&&intervals(standalone,4)>0,"matching path marks same trace complete with save interval");
  yes(TimedIo1947.saveStage(photo,file,97),"repeat save accepted");
  ProcessingTiming1947.Trace second=ProcessingTiming1947.traceFor(photo);
  yes(second!=standalone&&second.state==0,"reprocess same bitmap begins fresh trace");
  TimedIo1947.publishStage(file.getPath());
  ProcessingTiming1947.Trace job=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.Scope outer=ProcessingTiming1947.enter(job);
  yes(TimedIo1947.saveFinal(new Bitmap(),file,Bitmap.CompressFormat.JPEG,100),"job nested stage succeeds");TimedIo1947.publishFinal(file.getPath());
  yes(job.state==0&&ProcessingTiming1947.current()==job,"job never prematurely finishes and scope preserved");
  ProcessingTiming1947.restore(outer);
  Bitmap failurePhoto=new Bitmap();SaveFd186.result=false;yes(!TimedIo1947.saveStage(failurePhoto,file,1),"false alias result retained");
  yes(ProcessingTiming1947.traceFor(failurePhoto).state==2&&ProcessingTiming1947.current()==null,"failed standalone marks error and restores");SaveFd186.result=true;
  RuntimeException fatal=new RuntimeException("alias failure");SaveFd186.failure=fatal;Bitmap thrownPhoto=new Bitmap();boolean retained=false;
  try{TimedIo1947.saveStage(thrownPhoto,file,1);}catch(RuntimeException failure){retained=failure==fatal;}
  yes(retained&&ProcessingTiming1947.traceFor(thrownPhoto).state==2&&ProcessingTiming1947.current()==null,"propagated same exception and error recorded");SaveFd186.failure=null;
  ProcessingTiming1947.Trace encoded=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(encoded);HeifWriter writer=new HeifWriter();TimedIo1947.start(writer);ProcessingTiming1947.restore(scope);
  Thread callback=new Thread(()->TimedIo1947.writeSampleData(writer.muxer,3,ByteBuffer.allocate(1),new MediaCodec.BufferInfo()));callback.start();callback.join();
  yes(writer.muxer.writes==1&&intervals(encoded,4)>0,"async muxer IO bound to exact writer trace without TLS guessing");
  MediaMuxer other=new MediaMuxer();int count=intervals(encoded,4);TimedIo1947.writeSampleData(other,0,ByteBuffer.allocate(1),new MediaCodec.BufferInfo());
  yes(other.writes==1&&intervals(encoded,4)==count,"unbound muxer is not attributed to latest trace");
  Exception stopError=new IOException("stop failure");writer.stopFailure=stopError;boolean same=false;try{TimedIo1947.stop(writer,45000);}catch(Exception failure){same=failure==stopError;}
  yes(same&&writer.timeout==45000&&writer.stops==1&&intervals(encoded,3)>0,"stop exception closes compression timer and preserves timeout");
  TimedIo1947.close(writer);yes(writer.closes==1,"original close called once");
  count=intervals(encoded,4);TimedIo1947.writeSampleData(writer.muxer,0,ByteBuffer.allocate(1),new MediaCodec.BufferInfo());yes(intervals(encoded,4)==count,"closed writer association removed");
  ProcessingTiming1947.Trace legacy=ProcessingTiming1947.begin(photo);scope=ProcessingTiming1947.enter(legacy);PhotoDetail.settings.noiseOn=true;PhotoDetail.settings.sharpOn=false;yes(TimedIo1947.legacyDetail(photo,photo)==photo&&intervals(legacy,1)>0,"noise-only legacy classified by actual settings");
  Object span=new Object();TimedIo1947.spanStarted(span,"verify");TimedIo1947.spanClosed(span);yes(intervals(legacy,4)>0,"verification save span observed");TimedIo1947.spanClosed(span);ProcessingTiming1947.restore(scope);
  ProcessingTiming1947.Trace skipped=ProcessingTiming1947.begin(photo);scope=ProcessingTiming1947.enter(skipped);PhotoDetail.status="別の写真を処理中：追加画質処理を見送り";TimedIo1947.legacyDetail(photo,photo);yes(intervals(skipped,1)==0,"busy bypass never reported as executed noise");ProcessingTiming1947.restore(scope);
  System.out.println("PASS IO1947 checks="+checks);
 }
}'''
}

def test(root, work, android=None):
    root,work=Path(root),Path(work)
    folder=work/'io1947-host'
    src=folder/'src';classes=folder/'classes'
    src.mkdir(parents=True,exist_ok=True);classes.mkdir(parents=True,exist_ok=True)
    sources=list((root/'tests/timing1947-fixtures').rglob('*.java'))
    for name,code in FIXTURES.items():
        p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(code);sources.append(p)
    sources += [root/'ProcessingTiming1947.java',root/'TimedIo1947.java']
    subprocess.run(['javac','-encoding','UTF-8','-d',str(classes),*map(str,sources)],check=True,capture_output=True,text=True)
    result=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.TimedIo1947Test'],check=True,capture_output=True,text=True)
    assertions=int(result.stdout.split('checks=')[1].split()[0])
    report={'status':'passed','assertions':assertions,'original_alias_arguments_results_exceptions_retained':True,'async_mux_identity_and_finally_verified':True}
    (folder/'result.json').write_text(json.dumps(report,indent=2)+'\n')
    return report

if __name__=='__main__':
    import sys
    print(json.dumps(test(Path(__file__).parent,Path(sys.argv[1])),indent=2))
