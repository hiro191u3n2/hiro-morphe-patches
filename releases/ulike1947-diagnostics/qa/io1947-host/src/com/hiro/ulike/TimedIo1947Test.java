package com.hiro.ulike;
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
}