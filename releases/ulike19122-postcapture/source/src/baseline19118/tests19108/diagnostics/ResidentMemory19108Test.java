package com.hiro.ulike;
import android.content.Context;import android.graphics.Bitmap;import java.io.*;import java.lang.reflect.*;import java.security.MessageDigest;import java.util.*;import java.util.concurrent.Callable;
public final class ResidentMemory19108Test {
 static int checks,cases;
 static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static void waitFor(Callable<Boolean> p,String why)throws Exception{long end=System.nanoTime()+5000000000L;while(!p.call()){if(System.nanoTime()>end)throw new AssertionError("timeout "+why);Thread.sleep(2);}check(true,why);}
 static VerificationJournal19101.Engine journal()throws Exception{return (VerificationJournal19101.Engine)field(VerificationJournal19101.class,"JOURNAL").get(null);}
 static VerificationJournal19101.Entry latest()throws Exception{VerificationJournal19101.Engine j=journal();synchronized(j.lock){VerificationJournal19101.Entry e=null;for(VerificationJournal19101.Entry x:j.rows.values())e=x;return e;}}
 static String latestKey()throws Exception{Object job=field(GpuQualification1961.class,"activeJob").get(null);if(job==null){Deque<?> q=(Deque<?>)field(GpuQualification1961.class,"QUEUED").get(null);job=q.peekFirst();}return (String)field(job.getClass(),"key").get(job);}
 static Object failure(String key)throws Exception{Method env=GpuQualification1961.class.getDeclaredMethod("environment");env.setAccessible(true);Method f=GpuQualification1961.class.getDeclaredMethod("failure",String.class,String.class);f.setAccessible(true);return f.invoke(null,key,env.invoke(null));}
 static void runCase(int mode,int trial,int outcome)throws Exception{
  cases++;SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;ProcessingTiming1947.photo++;QualityPipeline1932.mode=mode;QualityPipeline1932.failAt=trial;QualityPipeline1932.candidateRuns=QualityPipeline1932.comparisons=0;QualityPipeline1932.oracle=null;
  Bitmap source=new Bitmap(65+mode*2+trial,24);QualityPixels1932.Plan p=new QualityPixels1932.Plan();
  check(GpuResident1976.finish(source,p,4,true,null,0,72,28,p,null)==null,"unknown resident stays foreground fallback");
  String key=latestKey();field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);SaveQueue1935.idle=true;GpuQualification1961.wake();
  waitFor(()->GpuQualification1961.retainedBytes()==0,"resident owner drains");SaveQueue1935.idle=false;
  VerificationJournal19101.Entry e=latest();check(e!=null&&e.v[6]==outcome,"typed final outcome "+mode+" got "+(e==null?-1:e.v[6]));
  check(!QualityPipeline1932.open,"dispatch closes before failure consumed");for(Bitmap b:Bitmap.all)if(b!=source)check(b.isRecycled(),"each oracle/destination/snapshot recycled");source.recycle();
  check(e.v[12]==(mode==6?1:0),"only measured completed call can record speed failure");
  if(mode==0){check(e.v[9]==1&&GpuQualification1961.restore(key)!=null,"only complete exact faster route can certify");for(int i=20;i<=24;i++)check(e.v[i]==2,"two completed gates retained");}
  else{
   check(e.v[9]==0&&GpuQualification1961.restore(key)==null,"failed or cancelled route cannot certify");check(!GpuQualification1961.exactRejected(key),"operational failure is not pixel rejection");
   if(mode==5){check(failure(key)==null,"cancellation spends no retry");}
   else {Object f=failure(key);check(f!=null&&!GpuQualification1961.maySchedule(key),"actual failure enters retry cooldown");check(field(f.getClass(),"outcome1991").getInt(f)==outcome,"retry ledger classification agrees");}
   if(mode==6){for(int i=20;i<=24;i++)check(e.v[i]==2,"slow successfulrelief completes2exact2speed");check(e.v[35]>=40000000L&&e.v[35]>e.v[36],"timedcall includesreliefdelay and failsunchangedlimit");check(e.v[104]==1&&e.v[6]==11,"memoryfit cannot override speedfailure");}
   if(mode==1||mode==4){check(e.v[98]==(mode==4?6:8)&&e.v[99]==trial&&e.v[100]==1,"memory snapshot binds originalstage/trial");check(e.v[101]==234L*1048576&&e.v[117]==60L*1048576,"defensive33scalar copy retained");check(e.v[24]==trial-1,"incomplete candidate timing not counted");check(e.v[21]==trial&&e.v[22]==(mode==4?0:trial),"exact gates reflect only completed comparisons");String live=GpuQualification1961.attemptSummary1985();check(live.contains("連結メモリ再判定")&&live.contains("同じ上限で不足")&&live.contains("CPU native合計"),"live bounded details rendered");}
  }
 }
 static long[] suffix(){long[] s=new long[35];s[0]=8;s[1]=1;System.arraycopy(QualityPipeline1932.memory(),0,s,2,33);return s;}
 static long[] record(){long[] v=new long[VerificationJournal19101.VALUES];v[0]=77;v[1]=90;v[3]=2;v[4]=2;v[6]=6;v[29]=v[30]=-1;System.arraycopy(suffix(),0,v,98,35);return v;}
 static VerificationJournal19101.Entry entry(long[] v){return new VerificationJournal19101.Entry("previous-process","1.9.108",v,new String[]{"連結","連結の速度比較","検証用メモリ不足","写真条件の検証","連結候補の速度計測"});}
 static String legacy(VerificationJournal19101.Entry e,int schema)throws Exception{int[] magic={0,0x564a0101,0x564a0103,0x564a0104,0x564a0105,0x564a0106};int[] count={0,37,47,59,74,98};ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(magic[schema]);out.writeInt(1);out.writeUTF(e.process);out.writeUTF(e.version);for(int i=0;i<count[schema];i++)out.writeLong(e.v[i]);for(String s:e.labels)out.writeUTF(s);out.close();byte[] raw=bytes.toByteArray();StringBuilder h=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))h.append(String.format(Locale.ROOT,"%02x",b&255));return "VJ"+schema+":"+h+":"+Base64.getEncoder().encodeToString(raw);}
 static void codec()throws Exception{
  cases++;VerificationJournal19101.Entry e=entry(record());String encoded=VerificationJournal19101.encode(Arrays.asList(e));check(encoded.startsWith("VJ6:"),"versioned new scalar suffix");check(Arrays.equals(e.v,VerificationJournal19101.decode(encoded).get(0).v),"full133scalar roundtrip including-1");
  for(int schema=1;schema<=5;schema++){cases++;VerificationJournal19101.Entry old=VerificationJournal19101.decode(legacy(e,schema)).get(0);for(int i=98;i<133;i++)check(old.v[i]==0,"oldschema no invented memory facts");}
  int[] index={0,1,2,3,4,5,6,7,8,9,10,11};long[] bad={3,0,2,-1,1,8,2,0,0,-1,4,-2};
  for(int i=0;i<index.length;i++){cases++;long[] s=suffix();s[index[i]]=bad[i];check(!VerificationJournal19101.validResidentMemory19108(s),"invalidsuffix "+index[i]);long[] v=record();System.arraycopy(s,0,v,98,35);boolean rejected=false;try{entry(v);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"codec refuses malformed suffix "+index[i]);}
  for(int state:new int[]{0,2,3,5,6}){cases++;long[] invalid=suffix();invalid[5]=state;check(!VerificationJournal19101.validResidentMemory19108(invalid),"secondcheck impossible for skipped trim status "+state);}
  long[] cancelled=suffix();cancelled[5]=4;cancelled[6]=1;check(VerificationJournal19101.validResidentMemory19108(cancelled),"latecancellation preserves alreadyobservedguard");StringBuilder cancelledText=new StringBuilder();VerificationJournal19101.appendResidentMemory19108(cancelledText,cancelled);check(cancelledText.toString().contains("取消で不採用"),"latecancelledfit cannot claimadoption");
  long[] v=record();v[4]=0;boolean rejected=false;try{entry(v);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"memory suffix cannot masquerade as helper");
  v=record();v[104]=1;v[105]=1;rejected=false;try{entry(v);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"fit requires real secondcheck");
  ArrayList<VerificationJournal19101.Entry> rows=new ArrayList<>();char[] chars=new char[40];Arrays.fill(chars,'検');String wide=new String(chars);char[] pid=new char[64];Arrays.fill(pid,'p');char[] ver=new char[24];Arrays.fill(ver,'1');for(int i=0;i<16;i++){v=record();v[0]=i+1;rows.add(new VerificationJournal19101.Entry(new String(pid),new String(ver),v,new String[]{wide,wide,wide,wide,wide}));}check(VerificationJournal19101.encode(rows).length()<=VerificationJournal19101.MAX_TEXT,"worstUTF16rows bounded");
  final String stored=encoded;VerificationJournal19101.Engine next=new VerificationJournal19101.Engine("warm-process");next.start(new VerificationJournal19101.Store(){public String read(){return stored;}public boolean write(String x){return true;}});waitFor(()->next.summary().contains("previous-process"),"warmstart memory history restored");String text=next.summary();check(text.contains("検証用メモリ不足")&&text.contains("連結候補の速度計測 1/2回目")&&text.contains("234.0 MiB")&&text.contains("GPU保持")&&text.contains("未消費GPU予約"),"warmrestore includesboundedbudgetdetails");check(GpuQualification1961.restore("previous-process:77")==null,"restoreddiagnostics cannot grantadmission");synchronized(next.lock){next.stopping=true;next.lock.notifyAll();}
 }
 static void scope()throws Exception{
  cases++;SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;ProcessingTiming1947.photo++;final long[] v=QualityPipeline1932.memory();GpuQualification1961.residentMemory19108(v);final String key="helper-no-resident-memory108";
  check(GpuQualification1961.schedule(key,1,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){GpuQualification1961.residentStage1999(8,1,false);GpuQualification1961.residentMemory19108(v);}public void close(){}}),"helperqueued");field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);SaveQueue1935.idle=true;GpuQualification1961.wake();waitFor(()->GpuQualification1961.retainedBytes()==0,"helperdrained");check(latest().v[100]==0,"unboundandcrossfamilyreportignored");SaveQueue1935.idle=false;
  cases++;ProcessingTiming1947.epoch++;ProcessingTiming1947.photo++;
  check(GpuQualification1961.schedule("strong-resident19108:malformed",1,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){
   GpuQualification1961.residentStage1999(8,1,false);GpuQualification1961.residentMemory19108(v);
   for(int state:new int[]{0,2,3,5,6}){long[] bad=v.clone();bad[3]=state;bad[1]=999;GpuQualification1961.residentMemory19108(bad);}
   long[] bad=v.clone();bad[4]=1;bad[5]=1;bad[1]=998;GpuQualification1961.residentMemory19108(bad);
   bad=v.clone();bad[6]=0;bad[1]=997;GpuQualification1961.residentMemory19108(bad);
   GpuQualification1961.residentStage1999(8,1,true);bad=v.clone();bad[1]=996;GpuQualification1961.residentMemory19108(bad);
  }public void close(){}}),"scopedmalformedfixturequeued");field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);SaveQueue1935.idle=true;GpuQualification1961.wake();waitFor(()->GpuQualification1961.retainedBytes()==0,"scopedfixturedrained");check(latest().v[101]==v[1]&&latest().v[103]==1,"live malformedandcompleted-stage callbacks cannotreplacevalidsnapshot");SaveQueue1935.idle=false;
 }
 public static void main(String[] args)throws Exception{GpuQualification1961.initialize(new Context());runCase(1,1,6);runCase(1,2,6);runCase(4,1,6);runCase(2,1,15);runCase(3,1,15);runCase(5,1,4);runCase(0,1,1);runCase(6,1,11);codec();scope();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"cases\":"+cases+",\"resident_memory_classified\":true,\"resident_memory_retry_not_exact_or_speed\":true,\"resident_memory_scalar_scope\":true,\"resident_memory_vj6_restart\":true,\"resident_memory_history_no_admission\":true,\"resident_two_exact_two_speed_gates\":true,\"physical_android_tested\":false}");}
}
