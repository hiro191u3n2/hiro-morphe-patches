package com.hiro.ulike;
import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import android.content.Context;

public final class JournalReason19105Test {
 static int checks,cases;
 static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 static Field field(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
 static String[] labels(){return new String[]{"連結","連結の画質比較","既存経路とCPU基準が不一致・連結候補は未判定","写真条件の検証","既存経路の全画素比較"};}
 static long[] current(){long[] v=new long[VerificationJournal19101.VALUES];v[0]=1;v[1]=2;v[3]=2;v[4]=2;v[29]=v[30]=-1;
  long[] stages={(2L<<10)|(2L<<26),6,2,3,0xff123456L,0xff123457L,0,1,12,8,4,1,8};System.arraycopy(stages,0,v,47,13);return v;}
 static VerificationJournal19101.Entry entry(long[] v){return new VerificationJournal19101.Entry("prior-process","1.9.105",v,labels());}
 static String render(VerificationJournal19101.Entry e){VerificationJournal19101.Engine engine=new VerificationJournal19101.Engine("current-process");engine.rows.put(e.key(),e);engine.loaded=true;return engine.summary();}
 static String legacy(VerificationJournal19101.Entry e,int schema)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);int[] magic={0,0x564a0101,0x564a0103,0x564a0104};int[] counts={0,37,47,59};out.writeInt(magic[schema]);out.writeInt(1);out.writeUTF(e.process);out.writeUTF("1.9.104");for(int i=0;i<counts[schema];i++)out.writeLong(e.v[i]);for(String label:e.labels)out.writeUTF(label);out.close();byte[] raw=bytes.toByteArray();StringBuilder h=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))h.append(String.format(Locale.ROOT,"%02x",b&255));return "VJ"+schema+":"+h+":"+Base64.getEncoder().encodeToString(raw);
 }
 static void schemas()throws Exception{
  cases++;long[] v=current();String text=render(entry(v));check(text.contains("顔・肌マスク形式未対応")&&text.contains("同じ行の厳密差")&&!text.contains("CPU再生成の対象行署名不一致を検出"),"persisted stable replay and unavailable mask kept distinct");
  String encoded=VerificationJournal19101.encode(Arrays.asList(entry(v)));check(encoded.startsWith("VJ4:"),"new reason schema has own version");check(Arrays.equals(VerificationJournal19101.decode(encoded).get(0).v,v),"all74scalars roundtrip");
  for(int schema=1;schema<=3;schema++){cases++;VerificationJournal19101.Entry old=VerificationJournal19101.decode(legacy(entry(v),schema)).get(0);check(old.version.equals("1.9.104"),"historical recorded version preserved "+schema);int boundary=schema==1?37:schema==2?47:59;for(int i=boundary;i<old.v.length;i++)check(old.v[i]==0,"no fabricated reasons/fixed comparisons "+schema+" "+i);String previous=render(old);check(!previous.contains("CPU再生成の対象行署名不一致を検出"),"legacy inclusive flag does not prove unstable CPU "+schema);if(schema==3)check(previous.contains("詳細理由の記録なし（旧記録の包括フラグ）")&&previous.contains("同じ行の厳密差"),"VJ3 uncertain failure and exact-row evidence both preserved");}
  final String[] labels={"詳細理由未記録","診断時間上限","診断用メモリ予算不足","顔・肌マスク形式未対応","診断処理例外","CPU再生成の対象行署名不一致を検出","撮影優先または割込みによる中断","診断用GPU処理未対応","診断用GPU領域の予約不可","診断用GPU転送・読戻し失敗","診断用GPU領域の解放失敗","診断入力の寸法・容量が対象外","比較用の基準署名なし"};
  for(int i=0;i<labels.length;i++){cases++;long[] row=current();row[59]=1L<<i;if(i==5){row[50]=0;row[54]=0;}String actual=render(entry(row));check(actual.contains(labels[i]),"specific persisted reason "+i);check((i==5)==actual.contains("CPU再生成の対象行署名不一致を検出"),"only actual unstable flag claims unstable CPU "+i);}
  long[] fixed={1,2,1,0,1,1,3,0xff123456L,0xff123456L,0xff123457L,1,1,4,4};System.arraycopy(fixed,0,v,60,14);VerificationJournal19101.Entry round=VerificationJournal19101.decode(VerificationJournal19101.encode(Arrays.asList(entry(v)))).get(0);check(Arrays.equals(round.v,v),"fixed comparison persists unsigned scalars exactly");
 }
 static void rejects(long[] good,int index,long value)throws Exception{cases++;long[] bad=good.clone();bad[index]=value;boolean refused=false;try{entry(bad);}catch(IllegalArgumentException expected){refused=true;}check(refused,"corrupt scalar rejected index"+index);if(index>=60){Method validator=GpuQualification1961.class.getDeclaredMethod("validFixedStages19105",long[].class);validator.setAccessible(true);check(!((Boolean)validator.invoke(null,(Object)Arrays.copyOfRange(bad,47,74))),"queue fixed validator agrees index"+index);}}
 static void corrupt()throws Exception{
  long[] v=current();rejects(v,59,8192);rejects(v,58,0);rejects(v,59,32);long[] fixed={1,2,1,0,1,1,3,0xff123456L,0xff123456L,0xff123457L,1,1,4,4};System.arraycopy(fixed,0,v,60,14);
  rejects(v,60,0);rejects(v,60,5);rejects(v,61,0);rejects(v,62,6);rejects(v,63,5);rejects(v,64,5);rejects(v,65,5);rejects(v,66,0);rejects(v,66,5);rejects(v,67,0x100000000L);rejects(v,68,0x100000000L);rejects(v,69,0x100000000L);rejects(v,69,0xff123456L);rejects(v,70,0);rejects(v,71,3);rejects(v,72,3);rejects(v,73,8193);
 }
 static void waitFor(Callable<Boolean> test,String why)throws Exception{long deadline=System.nanoTime()+5_000_000_000L;while(!test.call()){if(System.nanoTime()>deadline)throw new AssertionError("timeout: "+why);Thread.sleep(2);}check(true,why);}
 static void queue()throws Exception {
  GpuQualification1961.initialize(new Context());final long[] values=current();long[] fixed={1,2,1,0,1,1,3,0xff123456L,0xff123456L,0xff123457L,1,1,4,4};System.arraycopy(fixed,0,values,60,14);
  for(int schema=0;schema<2;schema++){cases++;final long[] payload=Arrays.copyOfRange(values,47,schema==0?59:74);final String key="strong-resident19105:reason-"+schema;SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;ProcessingTiming1947.photo++;
   check(GpuQualification1961.schedule(key,1,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){GpuQualification1961.residentStage1999(5,1,false);GpuQualification1961.residentStages19104(payload);check(!GpuResident1976.acceptComparison19103(key,false,ResidentProof1978.MISMATCH),"diagnostic scalars cannot admit mismatch");}public void close(){}}),"queue fixture scheduled");
   field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3_000_000_000L);SaveQueue1935.idle=true;GpuQualification1961.wake();waitFor(()->GpuQualification1961.retainedBytes()==0,"queue releases ownership");SaveQueue1935.idle=false;
   VerificationJournal19101.Engine journal=(VerificationJournal19101.Engine)field(VerificationJournal19101.class,"JOURNAL").get(null);synchronized(journal.lock){VerificationJournal19101.Entry last=null;for(VerificationJournal19101.Entry row:journal.rows.values())last=row;check(last!=null,"queue emitted a journal row");check(last.v[58]==1&&last.v[54]==1,"old boolean and stable CPU row preserved");check(last.v[59]==(schema==0?0:8),"new reason copied to durable schema without inventing old reason");check(last.v[60]==(schema==0?0:1)&&last.v[73]==(schema==0?0:4),"fixed comparison suffix copied to journal");}
   check(GpuQualification1961.exactRejected(key)&&GpuQualification1961.restore(key)==null,"independent strict mismatch remains rejected");
  }
 }
 public static void main(String[] args)throws Exception {schemas();corrupt();queue();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"cases\":"+cases+",\"journal_v1_v2_v3_v4_compatible\":true,\"legacy_failure_not_instability\":true,\"queue_journal_extended_scalars\":true,\"strict_mismatch_preserved\":true,\"physical_android_tested\":false}");}
}
