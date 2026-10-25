package com.hiro.ulike;
import android.content.Context;
import java.io.*;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Actual queue/journal, controlled Android storage only. Reports describe
 * admission observations; they never emulate the admission decision itself. */
public final class StrongAdmissionDiagnostics19110Test {
 static int assertions,cases;
 static final long M=1048576L;
 interface Check {boolean ok()throws Exception;}
 interface Action {void run(GpuQualification1961.Cancellation c)throws Exception;}
 static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
 static void await(Check c,String why)throws Exception{long end=System.nanoTime()+5000000000L;while(!c.ok()){if(System.nanoTime()>end)throw new AssertionError("timeout "+why);Thread.sleep(2);}check(true,why);}
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static VerificationJournal19101.Engine engine()throws Exception{return (VerificationJournal19101.Engine)field(VerificationJournal19101.class,"JOURNAL").get(null);}
 static void flush()throws Exception{VerificationJournal19101.Engine e=engine();await(()->{synchronized(e.lock){return e.loaded&&e.savedGeneration==e.generation;}},"bounded diagnostic commit finishes");}
 static VerificationJournal19101.Entry latest(long photo)throws Exception{for(VerificationJournal19101.Entry e:VerificationJournal19101.decode(Context.journalText()))if(e.process.equals(VerificationJournal19101.processId())&&e.v[1]==photo)return e;return null;}
 static long[] report(){return new long[]{1,6,0,1,1,1,7000000L,200*M,232*M,199*M,260*M,40*M,88*M,60*M,0,0,0,1};}
 static long[] suffix(){long[] s=new long[20];s[0]=6;s[1]=2;System.arraycopy(report(),0,s,2,18);return s;}
 static long[] record(){long[] v=new long[VerificationJournal19101.VALUES];v[0]=91;v[1]=190;v[3]=2;v[4]=2;v[6]=16;v[29]=v[30]=-1;System.arraycopy(suffix(),0,v,133,20);return v;}
 static VerificationJournal19101.Entry entry(long[] v){return new VerificationJournal19101.Entry("prior-process","1.9.110",v,new String[]{"連結","連結の画質比較","認定記録なし","写真条件の検証","連結候補の全画素比較"});}
 static VerificationJournal19101.Entry run(String key,Action action)throws Exception{
  cases++;SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;final long photo=++ProcessingTiming1947.photo;
  check(GpuQualification1961.schedule(key,32,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){try{action.run(c);}catch(RuntimeException e){throw e;}catch(Exception e){throw new RuntimeException(e);}}public void close(){}}),"actual owner scheduled");
  field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);SaveQueue1935.idle=true;GpuQualification1961.wake();
  await(()->GpuQualification1961.retainedBytes()==0,"actual owner drained");SaveQueue1935.idle=false;flush();VerificationJournal19101.Entry e=latest(photo);check(e!=null,"same photo committed");return e;
 }
 static void live()throws Exception{
  Context.qualificationLock=field(GpuQualification1961.class,"LOCK").get(null);GpuQualification1961.initialize(new Context());flush();
  final Map<String,String> proofBefore=Context.proofCopy();GpuQualification1961.residentStrong19110(report());
  final String key="strong-resident19110:diagnostics";
  VerificationJournal19101.Entry e=run(key,c->{
   GpuQualification1961.progress1984("resident_compare",1,2);GpuQualification1961.residentStage1999(6,2,false);
   long[] v=report();GpuQualification1961.residentStrong19110(v);Arrays.fill(v,0);
   long[] bad=report();bad[4]=2;bad[7]=1;GpuQualification1961.residentStrong19110(bad);
   Thread stranger=new Thread(()->{long[] other=report();other[7]=1;GpuQualification1961.residentStrong19110(other);});stranger.start();stranger.join();
   GpuQualification1961.residentStage1999(6,2,true);bad=report();bad[7]=2;GpuQualification1961.residentStrong19110(bad);
  });
  check(Arrays.equals(Arrays.copyOfRange(e.v,133,153),suffix()),"immutable valid snapshot survives caller mutation malformed foreign and completed callbacks");
  check(e.v[22]==1&&e.v[9]==0&&e.v[10]==0,"admission diagnostic cannot complete extra comparisons or create certificates");
  check(GpuQualification1961.restore(key)==null&&!GpuQualification1961.exactRejected(key),"accepted admission report is not a qualification or exact rejection");
  String text=GpuQualification1961.attemptSummary1985();check(text.contains("Strong連結受付（画質比較 2/2回目）")&&text.contains("初回 実メモリ予算不足")&&text.contains("最終 受付成立")&&text.contains("再判定 1回"),"live output binds exact candidate trial and classified reason");
  check(text.contains("必要量 200.0 MiB")&&text.contains("CPU native保持 前 60.0 MiB・後 0.0 MiB")&&text.contains("認定完了を意味しません"),"only observed scalar capacities are explained");
  check(Context.journalText().startsWith("VJ7:"),"new Strong fact selects versioned VJ7 wire schema");
  e=run("helper19110:diagnostics",c->{GpuQualification1961.residentStage1999(6,2,false);GpuQualification1961.residentStrong19110(report());});check(e.v[135]==0,"other job family cannot report resident admission");
  for(final int stage:new int[]{4,5,7}){e=run("strong-resident19110:wrongstage"+stage,c->{GpuQualification1961.residentStage1999(stage,1,false);GpuQualification1961.residentStrong19110(report());});check(e.v[135]==0,"CPU or ordinary route cannot report Strong resident admission");}
  e=run("strong-resident19110:cancel",c->{GpuQualification1961.residentStage1999(8,1,false);long[] v=report();v[2]=1;v[5]=4;v[17]=0;GpuQualification1961.residentStrong19110(v);throw new CancellationException("test cancellation");});
  check(e.v[6]==4&&e.v[133]==8&&e.v[134]==1&&e.v[140]==4&&e.v[152]==0,"cancelled attempt retains bounded observation but no admission");
  check(e.v[9]==0&&e.v[24]==0&&GpuQualification1961.restore("strong-resident19110:cancel")==null,"cancelled observations cannot certify incomplete speed pass");
  check(Context.proofCopy().equals(proofBefore),"diagnostics leave proof preference bytes unchanged");
  check(Context.journalUnderQueueLock==0&&Context.badJournalThread==0,"all storage remains off queue lock on daemon");
 }
 static String oldEnvelope(VerificationJournal19101.Entry e,int schema)throws Exception{
  int[] magic={0,0x564a0101,0x564a0103,0x564a0104,0x564a0105,0x564a0106,0x564a0108};int[] counts={0,37,47,59,74,98,133};
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(magic[schema]);out.writeInt(1);out.writeUTF(e.process);out.writeUTF(e.version);for(int i=0;i<counts[schema];i++)out.writeLong(e.v[i]);for(String s:e.labels)out.writeUTF(s);out.close();byte[] raw=bytes.toByteArray();StringBuilder h=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))h.append(String.format(Locale.ROOT,"%02x",b&255));return "VJ"+schema+":"+h+":"+Base64.getEncoder().encodeToString(raw);
 }
 static void malformed(long[] v,String why){check(!VerificationJournal19101.validResidentStrong19110(v),why);long[] row=record();System.arraycopy(v,0,row,133,20);boolean bad=false;try{entry(row);}catch(IllegalArgumentException rejected){bad=true;}check(bad,"wire rejects "+why);}
 static void codec()throws Exception{
  cases++;VerificationJournal19101.Entry e=entry(record());String encoded=VerificationJournal19101.encode(Arrays.asList(e));check(encoded.startsWith("VJ7:"),"VJ7 explicitly marks appended scalars");check(Arrays.equals(e.v,VerificationJournal19101.decode(encoded).get(0).v),"all153scalar roundtrip");
  long[] zero=record();Arrays.fill(zero,133,153,0);check(VerificationJournal19101.encode(Arrays.asList(entry(zero))).startsWith("VJ6:"),"unobserved suffix preserves old VJ6 wire layout");
  for(int schema=1;schema<=6;schema++){cases++;VerificationJournal19101.Entry old=VerificationJournal19101.decode(oldEnvelope(e,schema)).get(0);for(int i=133;i<153;i++)check(old.v[i]==0,"VJ1-VJ6 never invent Strong admission facts");check(old.version.equals(e.version),"old record build label is not rewritten");}
  for(int reason=1;reason<=12;reason++){cases++;long[] v=suffix();v[4]=v[5]=reason;v[6]=v[7]=v[8]=v[19]=0;for(int i=9;i<19;i++)v[i]=-1;check(VerificationJournal19101.validResidentStrong19110(v),"typed initial reason with unknown capacities valid "+reason);StringBuilder text=new StringBuilder();VerificationJournal19101.appendResidentStrong19110(text,v);check(text.toString().contains("未観測"),"missing capacity never displayed as zero");}
  int[] positions={0,1,2,3,4,5,6,7,8,9,19};long[] invalid={5,0,2,7,13,0,2,8,-1,-2,2};
  for(int i=0;i<positions.length;i++){cases++;long[] v=suffix();v[positions[i]]=invalid[i];malformed(v,"bounded field "+positions[i]);}
  for(int i=9;i<19;i++){long[] v=suffix();v[i]=-2;malformed(v,"unknown must be exactly -1 at "+i);}
  long[] v=suffix();v[19]=0;malformed(v,"success reason requires accepted fact");v=suffix();v[5]=9;malformed(v,"retry may only follow real memory shortage");v=suffix();v[6]=0;malformed(v,"trim request cannot fabricate no retry");v=suffix();v[7]=2;malformed(v,"busy trim cannot fabricate retry");
  v=record();v[4]=0;boolean rejected=false;try{entry(v);}catch(IllegalArgumentException good){rejected=true;}check(rejected,"Strong suffix cannot masquerade as helper family");
  ArrayList<VerificationJournal19101.Entry> rows=new ArrayList<>();char[] chars=new char[40];Arrays.fill(chars,'検');String wide=new String(chars);char[] pid=new char[64];Arrays.fill(pid,'p');char[] ver=new char[24];Arrays.fill(ver,'1');for(int i=0;i<16;i++){v=record();v[0]=i+1;rows.add(new VerificationJournal19101.Entry(new String(pid),new String(ver),v,new String[]{wide,wide,wide,wide,wide}));}check(VerificationJournal19101.encode(rows).length()<=VerificationJournal19101.MAX_TEXT,"full16 unicode records stay within original byte cap");
  final String stored=encoded;VerificationJournal19101.Engine warm=new VerificationJournal19101.Engine("warm-process");warm.start(new VerificationJournal19101.Store(){public String read(){return stored;}public boolean write(String text){return true;}});await(()->warm.summary().contains("prior-process"),"restart loads VJ7 diagnostics");String summary=warm.summary();check(summary.contains("Strong連結受付")&&summary.contains("200.0 MiB")&&summary.contains("実メモリ予算不足"),"restart renders bounded classified admission facts");check(GpuQualification1961.restore("prior-process:91")==null,"restored diagnostic cannot authorize GPU");synchronized(warm.lock){warm.stopping=true;warm.lock.notifyAll();}
 }
 public static void main(String[] args)throws Exception{live();codec();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"strong_admission_scalar_scope\":true,\"strong_admission_vj7_restart\":true,\"legacy_vj1_vj6_preserved\":true,\"diagnostic_history_cannot_grant_admission\":true,\"physical_android_tested\":false}");}
}
