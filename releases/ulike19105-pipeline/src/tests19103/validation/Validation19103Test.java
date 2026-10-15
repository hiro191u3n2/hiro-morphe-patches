package com.hiro.ulike;
import android.content.Context;
import android.graphics.Bitmap;
import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public final class Validation19103Test {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static final GpuQualification1961.Cancellation LIVE=()->false;
 static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 static void proof()throws Exception{
  Bitmap b=new Bitmap(7,67);int[] actual=b.pixels.clone();actual[7*34+5]=0xfe123655;
  ResidentProof1978.beginEvidence19103(7,67);ResidentProof1978 proof=new ResidentProof1978(b,LIVE);
  check(proof.rows(0,32,actual,0),"first band exact");check(!proof.rows(32,35,actual,32*7),"mismatch observed beyond chunk boundary");
  check(proof.result()==ResidentProof1978.MISMATCH,"proof mismatch result");
  ResidentProof1978.Evidence19103 e=ResidentProof1978.endEvidence19103();
  check(e.x==5&&e.y==34&&e.compared==244,"exact first mismatch coordinate and compared count");
  check(e.expectedArgb==0xff123456&&e.actualArgb==0xfe123655,"ARGB including alpha and individual channel difference retained");
  check(b.pixels[243]==0xff123456,"reference untouched");check(ResidentProof1978.endEvidence19103()==null,"observer cleared after call");
  check(!proof.rows(35,1,actual,35*7)&&proof.pixelsCompared()==244,"first mismatch cannot be overwritten or counted again");
  // Out-of-order GPU completion bands still compare exactly once.
  actual=b.pixels.clone();ResidentProof1978.beginEvidence19103(7,67);proof=new ResidentProof1978(b,LIVE);
  check(proof.rows(32,35,actual,32*7)&&proof.result()==ResidentProof1978.UNAVAILABLE,"partial coverage is unavailable");
  check(proof.rows(0,32,actual,0)&&proof.result()==ResidentProof1978.EXACT,"complete out-of-order coverage is exact");
  e=ResidentProof1978.endEvidence19103();check(e.compared==469&&e.x==-1&&e.y==-1,"exact result has no fabricated mismatch");
  // A memory-fallback reattempt must reset coverage, not accumulate it.
  ResidentProof1978.beginEvidence19103(7,67);proof=new ResidentProof1978(b,LIVE);proof.rows(0,1,actual,0);
  proof=new ResidentProof1978(b,LIVE);proof.rows(0,2,actual,0);e=ResidentProof1978.endEvidence19103();
  check(e.compared==14,"replacement comparison resets prior incomplete pass");
  final boolean[] stop={false};ResidentProof1978.beginEvidence19103(7,67);proof=new ResidentProof1978(b,()->stop[0]);proof.rows(0,1,actual,0);stop[0]=true;
  boolean cancelled=false;try{proof.rows(1,1,actual,7);}catch(CancellationException expected){cancelled=true;}finally{e=ResidentProof1978.endEvidence19103();}
  check(cancelled&&e.compared==7&&e.x==-1,"cancellation preserves partial observation without difference claim");
  ResidentProof1978.beginEvidence19103(7,67);proof=new ResidentProof1978(b,LIVE);boolean failed=false;
  try{proof.rows(66,2,actual,0);}catch(IllegalArgumentException expected){failed=true;}finally{e=ResidentProof1978.endEvidence19103();}
  check(failed&&e.compared==0&&e.x==-1,"bounds failure produces no fabricated mismatch");
  Bitmap out=b.copy(Bitmap.Config.ARGB_8888,false);out.pixels[468]^=1;
  ResidentProof1978.beginEvidence19103(7,67);check(ResidentProof1978.compareBitmaps19103(b,out,LIVE)==ResidentProof1978.MISMATCH,"CPU fallback uses exact proof gate");e=ResidentProof1978.endEvidence19103();
  check(e.compared==469&&e.x==6&&e.y==66&&e.actualArgb==0xff123457,"fallback final pixel difference measured");
  ResidentProof1978.beginEvidence19103(7,67);
  check(ResidentProof1978.compareBitmaps19103(b,new Bitmap(1,1),LIVE)==ResidentProof1978.MISMATCH,"shape mismatch retains legacy safety failure");
  e=ResidentProof1978.endEvidence19103();check(e.shapeMismatch&&e.compared==0&&e.x==-1,"shape failure has no fabricated compared pixel");
  ResidentProof1978.beginEvidence19103(7,67);final boolean[] inherited={true};Thread child=new Thread(()->inherited[0]=ResidentProof1978.endEvidence19103()!=null);child.start();child.join();
  check(!inherited[0]&&ResidentProof1978.endEvidence19103()!=null,"observer is scoped to owning thread");
 }
 static void waitFor(Callable<Boolean> predicate,String why)throws Exception{long until=System.nanoTime()+5000000000L;while(!predicate.call()){if(System.nanoTime()>until)throw new AssertionError("timeout "+why);Thread.sleep(2);}check(true,why);}
 static void classify(final String key,final boolean candidate,final int result,final long[] observation)throws Exception{
  classify(key,candidate,result,observation,false);
 }
 static void classify(final String key,final boolean candidate,final int result,final long[] observation,final boolean cancelAfter)throws Exception{
  SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;ProcessingTiming1947.photo++;
  check(GpuQualification1961.schedule(key,1,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){
   GpuQualification1961.residentStage1999(candidate?6:5,1,false);
   if(observation!=null)GpuQualification1961.residentComparison19103(observation);
   check(!GpuResident1976.acceptComparison19103(key,candidate,result),"failed comparison blocks admission");
   if(cancelAfter)throw new CancellationException("controlled optional replay cancellation after measured mismatch");
  }public void close(){}}),"test comparison scheduled");
  field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);SaveQueue1935.idle=true;GpuQualification1961.wake();
  waitFor(()->GpuQualification1961.retainedBytes()==0,"comparison ownership drained");SaveQueue1935.idle=false;
 }
 static void classification()throws Exception{
  GpuQualification1961.initialize(new Context());
  classify("strong-resident19103:baseline",false,ResidentProof1978.MISMATCH,new long[]{1,2,244,469,6,35,0xff123456L,0xfe123655L,7,67});
  check(GpuQualification1961.exactRejected("strong-resident19103:baseline"),"existing mismatch preserves permanent prerequisite safety stop");
  check("cached_resident_reference_mismatch".equals(GpuQualification1961.exactFailure1971("strong-resident19103:baseline")),"persistent cause distinguishes existing route from candidate");
  Object lock=field(GpuQualification1961.class,"LOCK").get(null);synchronized(lock){((Map<?,?>)field(GpuQualification1961.class,"FAILURES").get(null)).clear();}
  check(GpuQualification1961.exactRejected("strong-resident19103:baseline")&&"cached_resident_reference_mismatch".equals(GpuQualification1961.exactFailure1971("strong-resident19103:baseline")),"prerequisite safety stop and cause restored from exact journal");
  check(GpuQualification1961.restore("strong-resident19103:baseline")==null&&!GpuQualification1961.maySchedule("strong-resident19103:baseline"),"existing mismatch has no proof and remains blocked");
  String summary=GpuQualification1961.attemptSummary1985();
  check(summary.contains("既存経路とCPU基準が不一致・連結候補は未判定"),"reference-specific terminal outcome");
  check(summary.contains("差分検出で終了")&&summary.contains("比較済み 244/469画素")&&summary.contains("(x,y)=(5,34)"),"live history separates observed difference from incomplete pass");
  check(summary.contains("0xff123456")&&summary.contains("0xfe123655")&&summary.contains("(-1,0,2,-1)"),"ARGB evidence and signed channel deltas rendered");
  classify("strong-resident19103:candidate",true,ResidentProof1978.MISMATCH,new long[]{2,2,244,469,6,35,0xff123456L,0xfe123655L,7,67});
  check(GpuQualification1961.exactRejected("strong-resident19103:candidate"),"actual connected candidate mismatch remains hard rejection");
  check(GpuQualification1961.attemptSummary1985().contains("連結候補とCPU基準が不一致"),"candidate-specific terminal outcome");
  classify("strong-resident19103:incomplete",true,ResidentProof1978.UNAVAILABLE,new long[]{2,3,7,469,0,0,0,0,7,67});
  check(!GpuQualification1961.exactRejected("strong-resident19103:incomplete")&&GpuQualification1961.restore("strong-resident19103:incomplete")==null,"incomplete result never creates rejection or qualification");
  check(GpuQualification1961.attemptSummary1985().contains("連結候補対CPU基準・未完了"),"partial comparison is explicit");
  for(int which=0;which<2;which++){
   boolean candidate=which==1;String key="strong-resident19104:cancel-after-mismatch-"+which;
   classify(key,candidate,ResidentProof1978.MISMATCH,new long[]{candidate?2:1,2,244,469,6,35,0xff123456L,0xfe123655L,7,67},true);
   check(GpuQualification1961.exactRejected(key)&&GpuQualification1961.restore(key)==null,"optional replay cancellation preserves measured permanent rejection");
   synchronized(lock){Object[] attempts=(Object[])field(GpuQualification1961.class,"ATTEMPTS1984").get(null);
    int cursor=field(GpuQualification1961.class,"attemptCursor1984").getInt(null);Object last=attempts[(cursor+attempts.length-1)%attempts.length];
    check(field(last.getClass(),"outcome").getInt(last)==(candidate?18:17)&&field(last.getClass(),"finished").getBoolean(last),"late diagnostic cancellation cannot erase classified exact mismatch");}
  }
  GpuQualification1961.rejectExact("old-persisted-negative");
  GpuResident1976.acceptComparison19103("old-persisted-negative",false,ResidentProof1978.MISMATCH);
  check(GpuQualification1961.exactRejected("old-persisted-negative"),"existing permanent negative is never cleared by new classification");
  check(GpuResident1976.acceptComparison19103("merely-exact",true,ResidentProof1978.EXACT)&&GpuQualification1961.restore("merely-exact")==null,"exact comparison alone cannot qualify without timing trials");
 }
 static String legacy(VerificationJournal19101.Entry e)throws Exception{return legacy(e,1);}
 static String legacy(VerificationJournal19101.Entry e,int schema)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(schema==1?0x564a0101:0x564a0103);out.writeInt(1);out.writeUTF(e.process);out.writeUTF(e.version);
  for(int i=0;i<(schema==1?37:47);i++)out.writeLong(e.v[i]);for(String label:e.labels)out.writeUTF(label);out.close();byte[] raw=bytes.toByteArray();StringBuilder hex=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))hex.append(String.format("%02x",b&255));
  return "VJ"+schema+":"+hex+":"+Base64.getEncoder().encodeToString(raw);
 }
 static void journal()throws Exception{
  long[] v=new long[VerificationJournal19101.VALUES];v[0]=33;v[1]=55;v[3]=2;v[4]=2;v[6]=17;v[18]=5;v[19]=1;v[29]=v[30]=-1;
  long[] e={1,2,244,469,6,35,0xff123456L,0xfe123655L,7,67};System.arraycopy(e,0,v,37,e.length);
  String[] labels={"連結","連結の画質比較","既存経路とCPU基準が不一致・連結候補は未判定","写真条件の検証","既存経路の全画素比較"};
  VerificationJournal19101.Entry row=new VerificationJournal19101.Entry("prior-process","1.9.103",v,labels);
  String encoded=VerificationJournal19101.encode(Arrays.asList(row));check(encoded.startsWith("VJ4:"),"new scalar evidence uses versioned journal envelope");
  VerificationJournal19101.Entry decoded=VerificationJournal19101.decode(encoded).get(0);check(Arrays.equals(decoded.v,v)&&decoded.version.equals("1.9.103"),"evidence and writer version roundtrip exactly");
  VerificationJournal19101.Engine engine=new VerificationJournal19101.Engine("current-process");engine.rows.put(row.key(),decoded);engine.loaded=true;
  check(engine.summary().contains("差分検出で終了")&&engine.summary().contains("(-1,0,2,-1)"),"persisted result renders same classified observation");
  long[] old=v.clone();Arrays.fill(old,37,old.length,0);old[6]=12;VerificationJournal19101.Entry previous=new VerificationJournal19101.Entry("old-process","1.9.101",old,new String[]{"連結","連結の画質比較","今回の比較が不一致","写真条件の検証","既存経路の全画素比較"});
  VerificationJournal19101.Entry restored=VerificationJournal19101.decode(legacy(previous)).get(0);
  check(restored.version.equals("1.9.101")&&restored.v[6]==12&&restored.v[37]==0,"legacy history preserves original version/outcome and has no invented new evidence");
  VerificationJournal19101.Entry vj2=VerificationJournal19101.decode(legacy(row,2)).get(0);
  check(vj2.version.equals("1.9.103")&&Arrays.equals(Arrays.copyOf(vj2.v,47),Arrays.copyOf(v,47)),"VJ2 keeps all recorded pixel evidence and source version");
  for(int i=47;i<vj2.v.length;i++)check(vj2.v[i]==0,"VJ2 contains no fabricated later stage evidence");
  engine.offer(v,labels);check(engine.rows.get("current-process:33").version.equals(ProcessingTiming1947.VERSION),"new writes use common runtime version");
  long[] bad=v.clone();bad[39]=470;boolean refused=false;try{new VerificationJournal19101.Entry("bad","1.9.103",bad,labels);}catch(IllegalArgumentException expected){refused=true;}check(refused,"invalid compared count rejected");
  bad=v.clone();bad[43]=bad[44];refused=false;try{new VerificationJournal19101.Entry("bad","1.9.103",bad,labels);}catch(IllegalArgumentException expected){refused=true;}check(refused,"equal ARGB cannot be saved as measured mismatch");
 }
 public static void main(String[] args)throws Exception{proof();classification();journal();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"first_difference_scalar_evidence\":true,\"reference_candidate_incomplete_distinct\":true,\"negative_admission_preserved\":true,\"mismatch_survives_optional_replay_cancel\":true,\"legacy_journal_and_current_version\":true,\"journal_v1_v2_v3_compatible\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");}
}
