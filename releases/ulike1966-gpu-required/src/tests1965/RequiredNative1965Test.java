package com.hiro.ulike;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.content.Context;
public final class RequiredNative1965Test {
 static int assertions,attempts;static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
 static native void fault(int value);static native long[] facts();static native boolean external(int operation);
 static int[] compute(boolean fail){
  GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"private session opened");
  try{GpuNoise1960.Batch batch=session.newBatch();int[] u=new int[32];u[0]=4;
   batch.upload(0,new int[]{1,2,3,4}).upload(1,new int[]{1,2,3,4}).allocate(2,4).upload(2,new int[]{0}).dispatch(GpuNoise1960.COMPARE1961,new int[]{0,1,2},u,null,4);
   fault(fail?2:0);int[][] candidate=session.execute(batch,new int[]{2},new int[]{1});
   return candidate==null?null:candidate[0];
  }finally{fault(0);session.close();}
 }
 static void retries(boolean alwaysFail){
  check(external(0),"unrelated camera-style context exists");
  long generation=GpuNoise1960.diagnostics1965()[0];GpuRequiredFailure1965 failure=null;int[] result=null;
  try{result=GpuRequired1965.run("noise-test",99,new GpuRequired1965.Work<int[]>(){public int[] run(){attempts++;return compute(alwaysFail||attempts<3);}});}catch(GpuRequiredFailure1965 expected){failure=expected;}
  check(attempts==3,"initial attempt plus exactly two safe retries");check(GpuNoise1960.diagnostics1965()[0]>=generation+2,"recovery changed private context generation");
  check(external(1),"private recoveries preserved unrelated context");check(external(2),"unrelated context remains destroyable");
  check(facts()[0]>=3&&facts()[2]>=3,"actual GL dispatch and failed readback injection executed");
  if(alwaysFail){check(failure!=null&&failure.getCause()!=null&&result==null,"three failing GPU attempts propagate with no substitute");}
  else{check(failure==null&&result!=null&&result[0]==0,"third real GPU candidate succeeds");}
 }
 static void quarantine(){
  GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"quarantine session");check(session.upload(0,new int[]{4,3,2,1}),"real upload before timeout");long retained=GpuNoise1960.retainedBytes();
  fault(1);try{check(session.readInts(0,4)==null,"unknown fence completion cannot publish candidate");}finally{fault(0);session.close();}
  long[] d=GpuNoise1960.diagnostics1965();check(d[7]==1&&d[8]==1,"unknown completion explicitly recorded");check(GpuNoise1960.retainedBytes()==retained,"unknown allocations remain counted");
  check(!GpuNoise1960.recoverPrivate1965(),"unknown completion cannot reset context");check(GpuNoise1960.open()==null,"quarantined context cannot reopen");check(facts()[1]>0,"real native fence reached fault adapter");
 }
 static void stale()throws Exception{
  GpuNoise1960.Session old=GpuNoise1960.open();check(old!=null,"old session");Field field=old.getClass().getDeclaredField("token");field.setAccessible(true);long stale=((Long)field.get(old)).longValue();
  check(!GpuNoise1960.recoverPrivate1965(),"open session refuses recovery");old.close();check(GpuNoise1960.recoverPrivate1965(),"closed known-complete session safely recovers");
  GpuNoise1960.Session fresh=GpuNoise1960.open();check(fresh!=null,"new generation session");long current=((Long)field.get(fresh)).longValue();check(current!=stale,"tokens cannot alias generations");
  Method method=GpuNoise1960.class.getDeclaredMethod("allocateNative",long.class,int.class,long.class);method.setAccessible(true);check(!((Boolean)method.invoke(null,stale,0,16L)).booleanValue(),"stale token rejected before driver access");
  check(fresh.upload(0,new int[]{7,8})&&fresh.readInts(0,2)[1]==8,"fresh session unaffected by stale token");fresh.close();
 }
 static void fp64(){
  boolean verified=GpuNoise1960.fp64Verified1965();long[] d=GpuNoise1960.diagnostics1965();
  if(verified){check(d[11]==1&&facts()[0]>0&&facts()[1]>0,"capability passed actual dispatch and bounded fence");}
  else{check(d[11]==-1,"unsupported true fp64 explicitly rejected");check(!GpuNoise1960.supports(GpuNoise1960.MODEL1965)||d[11]!=1,"no claimed fp32 substitute for exact model");}
 }
 static void soft64(){
  boolean verified=GpuNoise1960.soft64Verified1965();check(verified,"actual GPU software binary64 conformance proof: "+GpuNoise1960.failure1965());long[] d=GpuNoise1960.diagnostics1965();check(d[14]==1&&facts()[0]>0&&facts()[1]>0,"integer-word binary64 proof dispatched and fenced on actual GLES");
  GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"opacity GPU admission session");long dispatched=facts()[0];check(GpuNoise1960.soft64Verified1965(),"completed precision proof available during owned session");check(facts()[0]==dispatched,"cached precision proof never mutates an active photograph workspace");
  try{GpuNoise1960.Batch batch=session.newBatch();int[] u=new int[32];u[0]=1;u[2]=3;
   batch.upload(0,new int[]{0xff112233,0x00112233,0xff778899}).upload(1,new int[]{0}).dispatch(GpuNoise1960.SOFT64_PROBE1965,new int[]{0,1},u,null,3);
   int[][] output=session.execute(batch,new int[]{1},new int[]{1});check(output!=null&&output[0][0]==1,"GPU alpha admission detects nonopaque source without CPU scan");
  }finally{session.close();}
 }
 static void forbidden(){
  final GpuRequiredFailure1965 expected=GpuRequiredFailure1965.forbidden("noise-source");GpuRequiredFailure1965 observed=null;
  try{GpuRequired1965.run("noise-source",77,new GpuRequired1965.Work<int[]>(){public int[] run(){attempts++;throw expected;}});}catch(GpuRequiredFailure1965 failure){observed=failure;}
  check(attempts==1,"forbidden source fallback never retries into a substitute");check(observed==expected&&observed.fallbackForbidden,"forbidden fallback exception identity propagates");
 }
 static void logs(File directory){
  GpuRequired1965.initialize(new Context(directory));GpuRequired1965.beginShot(321);
  for(int i=0;i<3500;i++)GpuRequired1965.note("bounded-log","event="+i+" "+new String(new char[1300]).replace('\0','x'));
  GpuRequired1965.fail("privacy-test",new IllegalStateException("/private/photos/source-face.jpg pixel (72,91)"));
  final GpuRequiredFailure1965 privateFailure=new GpuRequiredFailure1965("privacy-stage","/private/photos/source-face.jpg pixel (72,91)",null);
  try{GpuRequired1965.run("privacy-stage",321,new GpuRequired1965.Work<Object>(){public Object run(){throw privateFailure;}});}catch(GpuRequiredFailure1965 expected){}
  GpuRequired1965.endShot();String text=GpuRequired1965.diagnosticText1965();
  check(text.contains("stage=bounded-log")&&text.contains("shot="),"persistent diagnostic text includes stage and shot");
  check(text.contains("IllegalStateException")&&text.contains("GpuRequiredFailure1965"),"persistent failures preserve exception classification");
  check(!text.contains("/private/photos/")&&!text.contains("pixel (72,91)"),"private paths and coordinates from exception messages excluded");
  File[] files=directory.listFiles();long bytes=0;for(File file:files){check(file.length()<=128*1024,"individual diagnostic file bounded");bytes+=file.length();}check(bytes<=256*1024,"persistent log storage bounded");
 }
 static void androidSoftwareDriver(){
  check(external(0),"unrelated camera context exists during Android-target refusal");
  GpuRequired1965.beginShot(654);GpuRequiredFailure1965 failure=null;Object output=null;
  try{output=GpuRequired1965.run("android-hardware-required",654,new GpuRequired1965.Work<Object>(){public Object run(){attempts++;GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return null;try{throw new AssertionError("software GLES entered an image-processing session");}finally{session.close();}}});}
  catch(GpuRequiredFailure1965 expected){failure=expected;}finally{GpuRequired1965.endShot();}
  long[] d=GpuNoise1960.diagnostics1965();String error=GpuNoise1960.failure1965(),fingerprint=GpuNoise1960.fingerprint(),log=GpuRequired1965.diagnosticText1965();
  check(failure!=null&&output==null,"known software GLES fails required transaction with no image substitute");
  check(attempts>=1&&attempts<=3,"software refusal retries stay bounded");
  check(error.contains("known software GLES driver refused"),"explicit software-driver refusal reason visible");
  check(fingerprint.toLowerCase(java.util.Locale.ROOT).contains("llvmpipe"),"actual Mesa software renderer fingerprint retained");
  check(log.contains("known software GLES driver refused")&&log.contains("llvmpipe"),"refusal reason and driver fingerprint visible in diagnostics");
  check(d[0]==0&&d[5]==0&&d[6]==0&&d[9]==0&&d[12]==0,"no initialized image session, submitted work, native image buffers or token");
  check(d[1]==0&&d[2]==0&&d[3]==0&&d[4]==0&&facts()[0]==0&&facts()[1]==0&&facts()[2]==0,"Android-target refusal executes no upload, dispatch, readback, wait or unmap");
  check(d[8]==1&&d[13]==1,"required backend explicitly disabled and failed");
  check(external(1)&&external(2),"Android refusal preserves unrelated camera context and surface");
  System.out.println("ANDROID_GUARD "+error+" fingerprint="+fingerprint);
 }
 public static void main(String[] args)throws Exception{
  check(GpuNoise1960.available(),"actual JNI library loaded");String mode=args[0];
  if(mode.equals("recovery"))retries(false);else if(mode.equals("allfail"))retries(true);else if(mode.equals("quarantine"))quarantine();else if(mode.equals("stale"))stale();else if(mode.equals("fp64"))fp64();else if(mode.equals("soft64"))soft64();else if(mode.equals("forbidden"))forbidden();else if(mode.equals("logs"))logs(new File(args[1]));else if(mode.equals("android-software-driver"))androidSoftwareDriver();else throw new IllegalArgumentException(mode);
  System.out.println("RESULT {\"status\":\"passed\",\"mode\":\""+mode+"\",\"assertions\":"+assertions+",\"attempts\":"+attempts+",\"actualDispatches\":"+facts()[0]+",\"physicalAndroidTested\":false}");
 }
}
