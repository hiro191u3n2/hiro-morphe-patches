package com.hiro.ulike;
import android.graphics.Bitmap;
import android.media.FaceDetector;
import java.util.*;
import java.util.concurrent.*;

/** The real detector owner, completion observer and photo-analysis owner are
 * compiled together. Only Android APIs and unrelated save services are peers. */
public final class FaceReuse19104Test {
 static int assertions,cases,savedCalls;static java.security.MessageDigest masks;
 static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
 static Bitmap image(){Bitmap b=new Bitmap(64,64);Arrays.fill(b.pixels,0xffc69880);return b;}
 static PhotoAnalysis1981.Context context(Bitmap b){return new PhotoAnalysis1981.Context(b,0,1,new PhotoDetail.Settings(),true,false,new ShotContext1932.Snapshot(),null,true,new SaveMemory1981.Plan(b.width,b.height));}
 static void one(int mode,boolean fixed)throws Exception{
  Bitmap input=image();int[] pristine=input.pixels.clone();FaceDetector.mode.set(mode);FaceDetector.calls.set(0);Bitmap.failCreate=mode==7;android.graphics.Canvas.fail=mode==8;
  PhotoAnalysis1981.Context c=context(input);check(c.offer()&&c.start(),"real owner admits and starts immutable image");
  FaceResult1981 prepared=FaceResult1981.analyze(input,0);boolean successful=mode==0||mode==1;
  check(prepared.completed==(fixed&&successful),"only successful empty/reliable result records completion mode="+mode);
  c.completeFaces(prepared);c.finished();AsyncSave1935.context.set(c);
  FaceRegions1934.Mask finalMask=PhotoAnalysis1981.faces1981(input,0);
  int expected=mode==7||mode==8?0:fixed&&successful?1:2;
  check(FaceDetector.calls.get()==expected,"actual detector invocation count mode="+mode+" expected="+expected+" actual="+FaceDetector.calls.get());
  if(fixed&&successful){check(finalMask==prepared.mask,"reuses same immutable result without rerunning detector");savedCalls++;}
  int nonzeroSkin=0,nonzeroDetail=0;
  for(int y=0;y<64;y++)for(int x=0;x<64;x++){
   check(finalMask.skinQ8(x,y)==prepared.mask.skinQ8(x,y)&&finalMask.detailQ8(x,y)==prepared.mask.detailQ8(x,y),"output skin/detail unchanged");int pair=finalMask.samplePair1976(x,y);for(int shift=0;shift<32;shift+=8)masks.update((byte)(pair>>>shift));if(finalMask.skinQ8(x,y)>0)nonzeroSkin++;if(finalMask.detailQ8(x,y)>0)nonzeroDetail++;
  }
  check(mode==1?nonzeroSkin>0&&nonzeroDetail>0:nonzeroSkin==0&&nonzeroDetail==0,"reliable case exercises nonzero skin and detail planes");
  check(Arrays.equals(input.pixels,pristine),"read-only analysis retains exact source pixels");
  c.closeAndAwait1981();AsyncSave1935.context.remove();check(PhotoAnalysis1981.retainedBytes1981()==0,"optional budget released");
  Bitmap.failCreate=false;android.graphics.Canvas.fail=false;cases++;
 }
 static void guards(boolean fixed){
  FaceDetector.mode.set(0);Bitmap input=image();PhotoAnalysis1981.Context c=context(input);check(c.offer()&&c.start(),"guard owner started");FaceResult1981 r=FaceResult1981.analyze(input,0);c.completeFaces(r);c.finished();AsyncSave1935.context.set(c);
  int before=FaceDetector.calls.get();input.generation++;PhotoAnalysis1981.faces1981(input,0);check(FaceDetector.calls.get()==before+1,"changed bitmap generation forces original detector");input.generation--;
  before=FaceDetector.calls.get();PhotoAnalysis1981.faces1981(image(),0);check(FaceDetector.calls.get()==before+1,"different bitmap identity forces original detector");
  before=FaceDetector.calls.get();PhotoAnalysis1981.faces1981(input,180);check(FaceDetector.calls.get()==before+1,"different rotation forces original detector");
  c.closeAndAwait1981();before=FaceDetector.calls.get();PhotoAnalysis1981.faces1981(input,0);check(FaceDetector.calls.get()==before+1,"closed analysis owner cannot reuse");AsyncSave1935.context.remove();
 }
 static void isolated(final boolean fixed)throws Exception{
  ExecutorService pool=Executors.newFixedThreadPool(2);try{List<Future<Boolean>> out=new ArrayList<Future<Boolean>>();for(int i=0;i<2;i++){final int mode=i==0?0:4;out.add(pool.submit(new Callable<Boolean>(){public Boolean call(){FaceDetector.mode.set(mode);for(int j=0;j<16;j++){FaceResult1981 r=FaceResult1981.analyze(image(),0);if(r.completed!=(fixed&&mode==0))return false;}return true;}}));}for(Future<Boolean> f:out)check(f.get(),"completion observation remains thread-local");}finally{pool.shutdownNow();}
 }
 public static void main(String[] args)throws Exception{masks=java.security.MessageDigest.getInstance("SHA-256");boolean fixed=Boolean.parseBoolean(args[0]);for(int mode=0;mode<=8;mode++)one(mode,fixed);guards(fixed);isolated(fixed);StringBuilder maskHash=new StringBuilder();for(byte b:masks.digest())maskHash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"avoided_detector_calls\":"+savedCalls+",\"mask_sha256\":\""+maskHash+"\",\"successful_results_reused\":"+fixed+",\"failure_fallback_preserved\":true,\"identity_guards_preserved\":true,\"thread_local_completion_verified\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");}
}
