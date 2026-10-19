package com.hiro.ulike;
import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Callable;
import android.content.Context;

/** Actual scalar validators, journal codec, queue and rendering. Arithmetic
 * traces are fixed observations here; GPU execution is tested independently. */
public final class JournalTrace19106Test {
 static int checks,cases;
 static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static String[] labels(){return new String[]{"連結","連結の画質比較","既存経路とCPU基準が不一致・連結候補は未判定","写真条件の検証","既存経路の全画素比較"};}
 static long[] trace(){long[] t=new long[24];t[0]=1;t[1]=1;t[2]=3;t[3]=2;t[4]=1;t[5]=0x3f800000L;t[6]=0x3f800001L;t[12]=0xff123456L;t[13]=0xff123457L;t[14]=5;t[15]=3;for(int i=16;i<22;i++)t[i]=0x3f800000L;t[22]=100;t[23]=2048;return t;}
 static long[] current(){long[] v=new long[VerificationJournal19101.VALUES];v[0]=1;v[1]=2;v[3]=2;v[4]=2;v[29]=v[30]=-1;
  long[] stage={(2L<<10)|(2L<<26),6,2,3,0xff123456L,0xff123457L,0,1,12,8,4,0,0};System.arraycopy(stage,0,v,47,13);
  long[] fixed={1,2,1,0,1,1,3,0xff123456L,0xff123456L,0xff123457L,1,1,4,4};System.arraycopy(fixed,0,v,60,14);System.arraycopy(trace(),0,v,74,24);return v;}
 static VerificationJournal19101.Entry entry(long[] v){return new VerificationJournal19101.Entry("prior-process","1.9.106",v,labels());}
 static String render(VerificationJournal19101.Entry e){VerificationJournal19101.Engine engine=new VerificationJournal19101.Engine("current-process");engine.rows.put(e.key(),e);engine.loaded=true;return engine.summary();}
 static String legacy(VerificationJournal19101.Entry e,int schema)throws Exception{
  int[] magic={0,0x564a0101,0x564a0103,0x564a0104,0x564a0105};int[] counts={0,37,47,59,74};ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(magic[schema]);out.writeInt(1);out.writeUTF(e.process);out.writeUTF("1.9.105");for(int i=0;i<counts[schema];i++)out.writeLong(e.v[i]);for(String name:e.labels)out.writeUTF(name);out.close();byte[] raw=bytes.toByteArray();StringBuilder h=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))h.append(String.format(Locale.ROOT,"%02x",b&255));return "VJ"+schema+":"+h+":"+Base64.getEncoder().encodeToString(raw);
 }
 static void roundtrip()throws Exception {
  cases++;long[] v=current();check(StrongTrace19106.valid(trace()),"complete trace fixture valid");check(ResidentStages19104.valid(Arrays.copyOfRange(v,47,98)),"stage accepts bound trace");VerificationJournal19101.Entry e=entry(v);String encoded=VerificationJournal19101.encode(Arrays.asList(e));check(encoded.startsWith("VJ5:"),"new journal schema version");check(Arrays.equals(VerificationJournal19101.decode(encoded).get(0).v,v),"all98 unsigned scalars roundtrip");
  v[79]=7;check(e.v[79]!=7,"journal snapshot defensive copy");String text=render(e);check(text.contains("Java")&&text.contains("GPU")&&text.contains("3f800000")&&text.contains("3f800001"),"persisted first raw-bit checkpoint rendered");
  for(int schema=1;schema<=4;schema++){cases++;VerificationJournal19101.Entry old=VerificationJournal19101.decode(legacy(e,schema)).get(0);check(old.version.equals("1.9.105"),"legacy origin version preserved");for(int i=74;i<98;i++)check(old.v[i]==0,"old journal has no fabricated trace "+schema+"/"+i);check(!render(old).contains("Strong中間値"),"legacy record has no trace claim");if(schema==4)for(int i=60;i<74;i++)check(old.v[i]==e.v[i],"VJ4 fixed input evidence preserved");}
  ArrayList<VerificationJournal19101.Entry> rows=new ArrayList<>();for(int i=1;i<=16;i++){long[] many=current();many[0]=i;rows.add(entry(many));}check(VerificationJournal19101.encode(rows).length()<=VerificationJournal19101.MAX_TEXT,"16 complete traces stay bounded");
  rows.clear();char[] wideChars=new char[40];Arrays.fill(wideChars,'検');String wide=new String(wideChars);char[] pidChars=new char[64];Arrays.fill(pidChars,'p');char[] versionChars=new char[24];Arrays.fill(versionChars,'1');for(int i=1;i<=16;i++){long[] many=current();many[0]=i;rows.add(new VerificationJournal19101.Entry(new String(pidChars),new String(versionChars),many,new String[]{wide,wide,wide,wide,wide}));}check(VerificationJournal19101.encode(rows).length()<=VerificationJournal19101.MAX_TEXT,"16 rows with longest UTF labels fit unchanged byte limit");
 }
 static void reject(long[] good,int index,long value)throws Exception {cases++;long[] bad=good.clone();bad[index]=value;boolean no=false;try{entry(bad);}catch(IllegalArgumentException expected){no=true;}check(no,"journal rejects contradictory scalar "+index+"/"+value);check(!ResidentStages19104.valid(Arrays.copyOfRange(bad,47,98)),"stage agrees with journal rejection "+index);}
 static void invalid()throws Exception {
  long[] v=current();reject(v,74,0);reject(v,74,2);reject(v,75,7);reject(v,76,0);reject(v,76,5);reject(v,77,0);reject(v,77,3);reject(v,78,0);reject(v,78,2049);reject(v,78,120);reject(v,79,0x100000000L);reject(v,80,0x3f800000L);reject(v,81,2);reject(v,88,3);reject(v,89,4);reject(v,96,2049);reject(v,97,0);
  reject(v,62,2);reject(v,63,1);reject(v,70,2);reject(v,71,2);reject(v,48,5);
  long[] zero=new long[24];check(ResidentStages19104.validTrace19106(zero),"zero means unobserved");zero[5]=1;check(!ResidentStages19104.validTrace19106(zero),"unobserved cannot carry data");
 }
 static void statuses()throws Exception {
  for(int state=1;state<=6;state++){cases++;long[] v=current(),t=trace();t[1]=state;if(state==2){t[4]=t[5]=t[6]=0;}if(state>=3){t=new long[24];t[0]=1;t[1]=state;t[2]=3;t[3]=2;if(state==6)t[14]=6;}System.arraycopy(t,0,v,74,24);check(StrongTrace19106.valid(t),"trace status fixture "+state);VerificationJournal19101.Entry e=VerificationJournal19101.decode(VerificationJournal19101.encode(Arrays.asList(entry(v)))).get(0);check(Arrays.equals(v,e.v),"status preserved "+state);String text=render(e);check(text.contains("Strong中間値（固定1画素・Java基準対GPU・認定に不使用）")&&!text.contains("Strong中間値診断: 詳細表示未対応"),"status render available "+state);}
 }
 static void observer()throws Exception {
  cases++;int[] cpu={1,2,3,4,5,6,7,8,9,10,11,12},gpu=cpu.clone();gpu[6]++;
  ResidentStages19104.begin(new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}});ResidentStages19104.traceReport19106(trace());long[] early=ResidentStages19104.finish(-1);for(int i=27;i<51;i++)check(early[i]==0,"no foreground trace accepted");
  ResidentStages19104.begin(new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}});ResidentStages19104.Context context=(ResidentStages19104.Context)((ThreadLocal<?>)field(ResidentStages19104.class,"ACTIVE").get(null)).get();context.role=2;context.first=5;context.row=1;context.referenceWidth=4;context.expectedWords=12;context.actualWords=8;context.actualRow=Arrays.copyOfRange(gpu,4,8);context.states[5]=2;long[] t=trace();ResidentStages19104.traceReport19106(t);t[5]=8;long[] v=current();ResidentStages19104.fixedReport19105(Arrays.copyOfRange(v,60,74));long[] report=ResidentStages19104.finish(-1);check(report[32]==0x3f800000L,"observer defensively copies trace scalars");check(ResidentStages19104.valid(report),"observer trace bound to Strong source row");
 }
 static void waitFor(Callable<Boolean> test,String why)throws Exception{long until=System.nanoTime()+5_000_000_000L;while(!test.call()){if(System.nanoTime()>until)throw new AssertionError("timeout "+why);Thread.sleep(2);}check(true,why);}
 static void queue()throws Exception {
  cases++;GpuQualification1961.initialize(new Context());final long[] values=current(),payload=Arrays.copyOfRange(values,47,98);final String key="strong-resident19106:journal-trace";SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;ProcessingTiming1947.photo++;
  check(GpuQualification1961.schedule(key,1,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){GpuQualification1961.residentStage1999(5,1,false);GpuQualification1961.residentStages19104(payload);long[] corrupt=payload.clone();corrupt[29]=4;GpuQualification1961.residentStages19104(corrupt);corrupt=payload.clone();corrupt[32]=0x100000000L;GpuQualification1961.residentStages19104(corrupt);check(!GpuResident1976.acceptComparison19103(key,false,ResidentProof1978.MISMATCH),"trace scalars cannot admit mismatch");}public void close(){}}),"queue trace fixture scheduled");
  field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3_000_000_000L);SaveQueue1935.idle=true;GpuQualification1961.wake();waitFor(()->GpuQualification1961.retainedBytes()==0,"queue ownership released");SaveQueue1935.idle=false;
  VerificationJournal19101.Engine journal=(VerificationJournal19101.Engine)field(VerificationJournal19101.class,"JOURNAL").get(null);synchronized(journal.lock){VerificationJournal19101.Entry last=null;for(VerificationJournal19101.Entry row:journal.rows.values())last=row;check(last!=null,"journal snapshot emitted");check(Arrays.equals(Arrays.copyOfRange(last.v,74,98),trace()),"queue copies all trace scalars to journal");}
  check(GpuQualification1961.exactRejected(key)&&GpuQualification1961.restore(key)==null,"strict mismatch cannot be changed by trace");
 }
 public static void main(String[] args)throws Exception {roundtrip();invalid();statuses();observer();queue();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"cases\":"+cases+",\"journal_v1_v2_v3_v4_v5_compatible\":true,\"trace_scope_bound\":true,\"strict_mismatch_preserved\":true,\"physical_android_tested\":false}");}
}
