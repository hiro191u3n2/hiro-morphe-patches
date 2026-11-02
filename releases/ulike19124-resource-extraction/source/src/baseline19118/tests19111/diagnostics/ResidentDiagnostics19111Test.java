package com.hiro.ulike;
import android.content.Context;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/** Actual queue, fixed scalar bridge and journal. The fixtures provide only
 * Android storage/scheduling; they supply no pixel or speed admission evidence. */
public final class ResidentDiagnostics19111Test {
 static int assertions,cases;
 static final int START=153, WIDTH=29, SLOTS=4;
 static final long M=1048576L, DAY=86400000000000L;
 interface Check {boolean ok()throws Exception;}
 interface Action {void run(GpuQualification1961.Cancellation c)throws Exception;}
 static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
 static void await(Check c,String why)throws Exception{long end=System.nanoTime()+5000000000L;while(!c.ok()){if(System.nanoTime()>end)throw new AssertionError("timeout "+why);Thread.sleep(2);}check(true,why);}
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static VerificationJournal19101.Engine engine()throws Exception{return (VerificationJournal19101.Engine)field(VerificationJournal19101.class,"JOURNAL").get(null);}
 static void flush()throws Exception{VerificationJournal19101.Engine e=engine();await(()->{synchronized(e.lock){return e.loaded&&e.savedGeneration==e.generation;}},"bounded diagnostic commit finishes");}
 static VerificationJournal19101.Entry latest(long photo)throws Exception{for(VerificationJournal19101.Entry e:VerificationJournal19101.decode(Context.journalText()))if(e.process.equals(VerificationJournal19101.processId())&&e.v[1]==photo)return e;return null;}
 static long[] report(int seed,int status){
  long[] v=new long[WIDTH];v[0]=1;v[1]=status;v[5]=status==1?16383:8191;v[6]=status==1?0:8192;
  for(int i=0;i<14;i++){v[7+i]=(i+1)*1000000L+seed*1000L;v[3]+=v[7+i];}
  v[4]=10000000L+seed*1000L;v[2]=v[3]+v[4];
  v[21]=4;v[22]=256;v[23]=128*M;v[24]=16;v[25]=0;v[26]=128;v[27]=45;v[28]=45;return v;
 }
 static long[] row(VerificationJournal19101.Entry e,int slot){return Arrays.copyOfRange(e.v,START+WIDTH*slot,START+WIDTH*(slot+1));}
 static long[] activeRow(int slot)throws Exception{Object job=field(GpuQualification1961.class,"activeJob").get(null);Object attempt=field(job.getClass(),"attempt1984").get(job);long[] all=(long[])field(attempt.getClass(),"residentTiming19111").get(attempt);return Arrays.copyOfRange(all,slot*WIDTH,(slot+1)*WIDTH);}
 static long[] record(){
  long[] v=new long[VerificationJournal19101.VALUES];v[0]=91;v[1]=190;v[3]=2;v[4]=2;v[6]=11;v[18]=9;v[20]=v[21]=v[22]=v[23]=v[24]=2;v[26]=1;v[29]=v[30]=-1;
  for(int slot=0;slot<SLOTS;slot++)System.arraycopy(report(slot+1,1),0,v,START+slot*WIDTH,WIDTH);
  v[13]=v[34]=report(1,1)[2];v[14]=v[35]=report(4,1)[2];v[36]=v[34]-v[34]/20;return v;
 }
 static VerificationJournal19101.Entry entry(long[] v){return new VerificationJournal19101.Entry("prior-process","1.9.111",v,new String[]{"連結","連結の速度比較","速度条件を満たさず","写真条件の検証","認定の最終確認"});}
 static VerificationJournal19101.Entry run(String key,Action action)throws Exception{
  cases++;SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;final long photo=++ProcessingTiming1947.photo;
  check(GpuQualification1961.schedule(key,32,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){try{action.run(c);}catch(RuntimeException e){throw e;}catch(Exception e){throw new RuntimeException(e);}}public void close(){}}),"actual owner scheduled");
  field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);SaveQueue1935.idle=true;GpuQualification1961.wake();
  await(()->GpuQualification1961.retainedBytes()==0,"actual owner drained");SaveQueue1935.idle=false;flush();VerificationJournal19101.Entry e=latest(photo);check(e!=null,"same photo committed");return e;
 }
 static void noTiming(VerificationJournal19101.Entry e,String why){for(int i=START;i<START+WIDTH*SLOTS;i++)check(e.v[i]==0,why);}
 static void live()throws Exception{
  Context.qualificationLock=field(GpuQualification1961.class,"LOCK").get(null);GpuQualification1961.initialize(new Context());flush();
  final Map<String,String> proofBefore=Context.proofCopy();GpuQualification1961.residentTiming19111(report(1,1));
  final String key="strong-resident19111:diagnostics";
  VerificationJournal19101.Entry e=run(key,c->{
   GpuQualification1961.progress1984("resident_speed",0,2);
   for(int trial=1;trial<=2;trial++)for(int route=0;route<2;route++){
    int slot=(trial-1)*2+route;GpuQualification1961.residentStage1999(7+route,trial,false);
    if(slot==0){
     long[] partial=report(0,4);GpuQualification1961.residentTiming19111(partial);
     long[] backwards=report(0,4);backwards[4]--;backwards[2]--;GpuQualification1961.residentTiming19111(backwards);
     check(Arrays.equals(activeRow(0),partial),"partial update cannot make wall duration go backwards");
    }
    long[] value=report(slot+1,1);
    if(slot==3)GpuQualification1961.residentStage1999(7+route,trial,true);
    GpuQualification1961.residentTiming19111(value);Arrays.fill(value,0);
    long[] malformed=report(99,1);malformed[3]++;GpuQualification1961.residentTiming19111(malformed);
    GpuQualification1961.residentTiming19111(report(99,1));
    Thread foreign=new Thread(()->GpuQualification1961.residentTiming19111(report(100,1)));foreign.start();foreign.join();
    if(slot!=3)GpuQualification1961.residentStage1999(7+route,trial,true);
   }
   GpuQualification1961.progress1984("resident_speed",2,2);GpuQualification1961.timings1984(report(1,1)[2],report(4,1)[2]);
   GpuQualification1961.rejectMeasuredThreshold1992(key,"resident_whole","deployed_whole",report(1,1)[2],report(4,1)[2],report(1,1)[2]-report(1,1)[2]/20);
  });
  for(int slot=0;slot<SLOTS;slot++)check(Arrays.equals(row(e,slot),report(slot+1,1)),"four immutable route/trial slots survive mutation, malformed, terminal and foreign updates "+slot);
  check(e.v[23]==2&&e.v[24]==2&&e.v[9]==0&&e.v[10]==0,"diagnostics neither add completed speed passes nor create certificate writes");
  check(GpuQualification1961.restore(key)==null&&!GpuQualification1961.exactRejected(key),"complete timing report is never a qualification or quality rejection");
  String text=GpuQualification1961.attemptSummary1985();
  check(text.contains("既存経路（2回の最小）")&&text.contains("連結候補（2回の最大）"),"current speed summary names the actual aggregate comparator");
  check(!text.contains("検証用の計測: CPU基準")&&!text.contains("基準 deployed_whole")&&!text.contains("比較対象の詳細未記録"),"resident speed never mislabeled as a CPU reference or unspecified route");
  check(text.contains("モデル")&&text.contains("Strong")&&text.contains("未計測"),"current summary renders observed phase wall/exclusive/unmeasured details");
  check(Context.journalText().startsWith("VJ8:"),"new resident timing selects explicit VJ8 wire");
  check(e.v[13]==report(1,1)[2]&&e.v[14]==report(4,1)[2]&&e.v[34]==report(1,1)[2]&&e.v[35]==report(4,1)[2],"observer preserves independent measured-speed inputs verbatim");
  e=run("helper19111:wrongfamily",c->{GpuQualification1961.residentStage1999(7,1,false);GpuQualification1961.residentTiming19111(report(1,1));});noTiming(e,"helper cannot forge resident route timing");
  for(final int stage:new int[]{4,5,6,9}){e=run("strong-resident19111:wrongstage"+stage,c->{GpuQualification1961.residentStage1999(stage,1,false);GpuQualification1961.residentTiming19111(report(1,1));});noTiming(e,"quality/oracle/final gate phases cannot masquerade as speed timing");}
  for(final int trial:new int[]{0,3}){e=run("strong-resident19111:wrongtrial"+trial,c->{GpuQualification1961.residentStage1999(7,trial,false);GpuQualification1961.residentTiming19111(report(1,1));});noTiming(e,"invalid trial cannot write timing");}
  e=run("strong-resident19111:failure",c->{GpuQualification1961.residentStage1999(8,1,false);long[] v=report(2,2);Arrays.fill(v,21,29,-1);GpuQualification1961.residentTiming19111(v);throw new IllegalStateException("bounded fixture failure");});
  check(row(e,1)[1]==2&&row(e,1)[6]==8192&&row(e,1)[21]==-1&&row(e,1)[28]==-1,"failure retains partial exclusive phase and unknown plan metadata");
  check(e.v[24]==0&&e.v[9]==0&&e.v[34]==0&&GpuQualification1961.restore("strong-resident19111:failure")==null,"failure timing cannot become a speed pass or measured rejection");
  e=run("strong-resident19111:cancel",c->{GpuQualification1961.residentStage1999(8,2,false);GpuQualification1961.residentTiming19111(report(3,3));throw new CancellationException("fixture cancellation");});
  check(e.v[6]==4&&row(e,3)[1]==3&&row(e,3)[6]==8192,"cancelled attempt retains bound route/trial observation");
  check(e.v[23]==0&&e.v[24]==0&&e.v[9]==0&&e.v[13]==0&&e.v[14]==0&&GpuQualification1961.restore("strong-resident19111:cancel")==null,"cancellation never grants incomplete timing to speed certification");
  e=run("strong-resident19111:partial",c->{GpuQualification1961.residentStage1999(7,2,false);GpuQualification1961.residentTiming19111(report(8,4));});
  check(row(e,2)[1]==4&&e.v[23]==0&&e.v[9]==0,"unfinished timing is diagnostic only");
  e=run("strong-resident19111:incomplete-counts",c->{GpuQualification1961.residentStage1999(7,1,false);GpuQualification1961.residentTiming19111(report(2,4));GpuQualification1961.timings1984(30000000,40000000);});
  String[] lines=GpuQualification1961.attemptSummary1985().split("\n");String last="";for(String line:lines)if(line.startsWith("#"))last=line;
  check(last.contains("2回分の完了は未確認")&&!last.contains("2回の最小")&&!last.contains("2回の最大"),"partial counts cannot advertise two-trial extrema");
  check(Context.proofCopy().equals(proofBefore),"timing writes do not change proof preference bytes");
  check(Context.journalUnderQueueLock==0&&Context.badJournalThread==0,"journal storage remains outside qualification lock on daemon");
 }
 static String envelope(int schema,byte[] raw)throws Exception{StringBuilder h=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))h.append(String.format(Locale.ROOT,"%02x",b&255));return "VJ"+schema+":"+h+":"+Base64.getEncoder().encodeToString(raw);}
 static String legacy(VerificationJournal19101.Entry e,int schema)throws Exception{
  int[] magic={0,0x564a0101,0x564a0103,0x564a0104,0x564a0105,0x564a0106,0x564a0108,0x564a0110};int[] counts={0,37,47,59,74,98,133,133};
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(magic[schema]);out.writeInt(1);out.writeUTF(e.process);out.writeUTF(e.version);for(int i=0;i<counts[schema];i++)out.writeLong(e.v[i]);
  if(schema==7){for(int i=133;i<141;i++)out.writeByte((int)e.v[i]);for(int i=141;i<152;i++)out.writeLong(e.v[i]);out.writeByte((int)e.v[152]);}
  for(String s:e.labels)out.writeUTF(s);out.close();return envelope(schema,bytes.toByteArray());
 }
 static long[] admission(){long[] s={8,2,1,5,1,1,0,2,0,128*M,100*M,90*M,91*M,20*M,21*M,10*M,11*M,0,0,0};return s;}
 static void malformed(long[] v,String why){cases++;check(!VerificationJournal19101.validResidentTiming19111(v),why);if(v.length==WIDTH){long[] whole=record();System.arraycopy(v,0,whole,START,WIDTH);boolean rejected=false;try{entry(whole);}catch(IllegalArgumentException good){rejected=true;}check(rejected,"wire rejects "+why);}}
 static void rejected(String encoded,String why){cases++;boolean bad=false;try{VerificationJournal19101.decode(encoded);}catch(Exception expected){bad=true;}check(bad,why);}
 static byte[] compressed(byte[] raw)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DeflaterOutputStream out=new DeflaterOutputStream(bytes);out.write(raw);out.close();return bytes.toByteArray();}
 static byte[] expanded(byte[] raw)throws Exception{InflaterInputStream in=new InflaterInputStream(new ByteArrayInputStream(raw));ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[1024];for(int n;(n=in.read(buf))!=-1;)out.write(buf,0,n);return out.toByteArray();}
 static String codec()throws Exception{
  cases++;check(VerificationJournal19101.VALUES==269,"journal has fixed 153 existing plus four29-field timing slots");
  VerificationJournal19101.Entry e=entry(record());String encoded=VerificationJournal19101.encode(Arrays.asList(e));check(encoded.startsWith("VJ8:"),"timing explicitly selects VJ8");check(Arrays.equals(e.v,VerificationJournal19101.decode(encoded).get(0).v),"all269scalar exact round trip");
  long[] without=record();Arrays.fill(without,START,without.length,0);check(VerificationJournal19101.encode(Arrays.asList(entry(without))).startsWith("VJ6:"),"unobserved timing preserves original VJ6 wire");
  System.arraycopy(admission(),0,without,133,20);VerificationJournal19101.Entry oldSource=entry(without);check(VerificationJournal19101.encode(Arrays.asList(oldSource)).startsWith("VJ7:"),"Strong-only history keeps VJ7 unchanged");
  for(int schema=1;schema<=7;schema++){cases++;VerificationJournal19101.Entry old=VerificationJournal19101.decode(legacy(oldSource,schema)).get(0);noTiming(old,"VJ1-VJ7 cannot invent phase timings");check(old.version.equals(oldSource.version),"legacy build identity preserved");check(old.v[13]==oldSource.v[13]&&old.v[14]==oldSource.v[14],"legacy measured pairs preserved");if(schema==7)check(Arrays.equals(Arrays.copyOfRange(old.v,133,153),admission()),"VJ7 Strong admission suffix survives exact legacy envelope");}
  check(new VerificationJournal19101.Entry("legacy-producer","1.9.110",Arrays.copyOf(without,153),oldSource.labels).v.length==269,"previous153scalar producer is padded safely");
  check(new VerificationJournal19101.Entry("legacy-producer","1.9.108",Arrays.copyOf(without,133),oldSource.labels).v.length==269,"previous133scalar producer is padded safely");
  malformed(new long[28],"short timing row");malformed(new long[30],"long timing row");
  int[] positions={0,1,1,2,2,3,4,5,6,7,21,21,22,22,23,23,24,25,26,26,27,28};long[] bad={2,0,5,-1,DAY+1,-1,-1,16384,16384,-1,0,65,0,65537,-2,Long.MAX_VALUE/4+1,1000001,-2,0,65537,-2,1000001};
  for(int i=0;i<positions.length;i++){long[] v=report(1,1);v[positions[i]]=bad[i];malformed(v,"bounded field "+positions[i]+" value "+bad[i]);}
  long[] v=report(1,1);v[3]++;malformed(v,"exclusive subtotal cannot differ from phase sum");v=report(1,1);v[4]++;malformed(v,"unmeasured must be exact wall difference");v=report(1,1);v[5]=0;malformed(v,"positive phase cannot be unobserved");v=report(1,4);v[5]|=8192;malformed(v,"complete and incomplete masks cannot overlap");v=report(1,1);v[5]=8191;v[6]=8192;malformed(v,"successful call cannot have incomplete phase");v=new long[29];v[2]=1;malformed(v,"schema0 requires allzero row");
  v=report(1,1);Arrays.fill(v,21,29,-1);check(VerificationJournal19101.validResidentTiming19111(v),"all plan metadata may remain unknown");
  v=new long[29];check(VerificationJournal19101.validResidentTiming19111(v),"allzero row means unobserved");
  for(int status=1;status<=4;status++){long[] four=new long[116];System.arraycopy(report(1,status),0,four,WIDTH,WIDTH);StringBuilder label=new StringBuilder();VerificationJournal19101.appendResidentTiming19111(label,four);String text=label.toString();
   check(text.contains("連結候補 1/2回目")&&text.contains("排他的工程小計")&&text.contains("未計測差分"),"phase detail names exact route and separates exclusive subtotal "+status);
   check(text.contains(status==1?"認定の成立とは別":"速度認定に使用不可"),"status explicitly separates call completion from qualification "+status);
  }
  v=report(1,1);v[21]=64;v[22]=65536;v[23]=Long.MAX_VALUE/4;v[24]=v[25]=v[27]=v[28]=1000000;v[26]=65536;check(VerificationJournal19101.validResidentTiming19111(v),"inclusive plan metadata upper bounds accepted");
  v=record();v[4]=0;boolean wrong=false;try{entry(v);}catch(IllegalArgumentException expected){wrong=true;}check(wrong,"timing suffix cannot masquerade as auxiliary family");
  rejected(encoded.substring(0,encoded.length()-1),"truncated VJ8 envelope rejected");byte[] packed=Base64.getDecoder().decode(encoded.substring(69));byte[] trailing=Arrays.copyOf(packed,packed.length+1);trailing[trailing.length-1]=1;rejected(envelope(8,trailing),"rehashed compressed trailing byte rejected");rejected(envelope(8,Arrays.copyOf(packed,packed.length-1)),"rehashed truncated compressed body rejected");rejected(envelope(8,compressed(new byte[1048576])),"bounded inflation rejects compressed expansion beyond fixed cap");
  byte[] raw=expanded(packed);DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));in.readInt();in.readInt();in.readUTF();in.readUTF();int offset=raw.length-in.available();
  ByteArrayOutputStream changed=new ByteArrayOutputStream();changed.write(raw,0,offset);changed.write(new byte[]{(byte)0x80,0});changed.write(raw,offset+2,raw.length-offset-2);rejected(envelope(8,compressed(changed.toByteArray())),"overlong zero varint rejected with valid outer checksum");
  changed.reset();changed.write(raw,0,offset);for(int i=0;i<9;i++)changed.write(255);changed.write(2);changed.write(raw,offset+2,raw.length-offset-2);rejected(envelope(8,compressed(changed.toByteArray())),"overflowing ten-byte varint rejected");
  ArrayList<VerificationJournal19101.Entry> rows=new ArrayList<>();Random random=new Random(19111);char[] identifier=new char[64];Arrays.fill(identifier,'p');char[] version=new char[24];Arrays.fill(version,'1');
  for(int n=0;n<16;n++){
   long[] whole=record();whole[0]=n+1;System.arraycopy(admission(),0,whole,133,20);for(int j=142;j<152;j++)whole[j]=random.nextLong()&Long.MAX_VALUE;
   for(int position:new int[]{1,2,13,14,15,16,17,25,32,33,34,35,36})whole[position]=1+(random.nextLong()&Long.MAX_VALUE)/2;
   long[] memory=new long[35];memory[0]=8;memory[1]=2;memory[2]=1;memory[3]=(random.nextLong()&Long.MAX_VALUE)/2;memory[5]=1;memory[6]=1;memory[7]=2;memory[8]=1;memory[9]=(random.nextLong()&Long.MAX_VALUE)/2;memory[10]=3;for(int j=11;j<35;j++)memory[j]=(random.nextLong()&Long.MAX_VALUE)/2;System.arraycopy(memory,0,whole,98,35);
   for(int slot=0;slot<4;slot++){long[] timing=report(slot+1,1);timing[2]=DAY/2+(random.nextLong()&Long.MAX_VALUE)%(DAY/2);timing[3]=0;for(int phase=0;phase<14;phase++){timing[7+phase]=1+((random.nextLong()&Long.MAX_VALUE)%(timing[2]/28));timing[3]+=timing[7+phase];}timing[4]=timing[2]-timing[3];timing[21]=1+random.nextInt(64);timing[22]=1+random.nextInt(65536);timing[23]=(random.nextLong()&Long.MAX_VALUE)/4;timing[24]=random.nextInt(1000001);timing[25]=random.nextInt(1000001);timing[26]=1+random.nextInt(65536);timing[27]=random.nextInt(1000001);timing[28]=random.nextInt(1000001);System.arraycopy(timing,0,whole,START+slot*WIDTH,WIDTH);}
   String[] labels=new String[5];for(int a=0;a<5;a++){char[] chars=new char[40];for(int b=0;b<chars.length;b++)chars[b]=(char)(0x4e00+random.nextInt(0x4fff));labels[a]=new String(chars);}rows.add(new VerificationJournal19101.Entry(new String(identifier),new String(version),whole,labels));
  }
  String dense=VerificationJournal19101.encode(rows);check(dense.length()<=VerificationJournal19101.MAX_TEXT,"sixteen complete four-trial Unicode histories stay inside40KiB");List<VerificationJournal19101.Entry> restored=VerificationJournal19101.decode(dense);check(restored.size()==16,"all16 dense history rows retained");for(int i=0;i<16;i++)check(Arrays.equals(rows.get(i).v,restored.get(i).v),"dense scalar rows remain lossless "+i);
  rows.add(e);boolean countRejected=false;try{VerificationJournal19101.encode(rows);}catch(IOException expected){countRejected=true;}check(countRejected,"seventeenth entry rejected rather than unbounded history");
  return encoded;
 }
 static void restart(String stored)throws Exception{
  cases++;Context.qualificationLock=field(GpuQualification1961.class,"LOCK").get(null);Context.seedJournal(stored);Context.seedProof("unrelated-proof","not-a-certificate");Map<String,String> proof=Context.proofCopy();GpuQualification1961.initialize(new Context());flush();
  await(()->VerificationJournal19101.summary().contains("prior-process"),"independent process reads VJ8 history");String text=VerificationJournal19101.summary();
  check(text.contains("既存経路（2回の最小）")&&text.contains("連結候補（2回の最大）"),"restart retains correct two-trial route comparator names");
  check(text.contains("Strong")&&text.contains("モデル")&&text.contains("未計測")&&text.contains("既存経路")&&text.contains("連結候補"),"restart renders both route timing diagnostics");
  check(!text.contains("検証用の計測: CPU基準")&&!text.contains("比較対象の詳細未記録"),"restart never relabels deployed timing as CPU oracle");
  check(((Map<?,?>)field(GpuQualification1961.class,"RECORDS").get(null)).isEmpty()&&GpuQualification1961.restore("prior-process:91")==null,"historical speed and timing fields cannot populate admission cache");
  check(Context.proofCopy().equals(proof),"independent restoration leaves proof preferences byte-identical");
  check(Context.journalUnderQueueLock==0&&Context.badJournalThread==0,"independent restore storage remains on journal daemon");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"independent_jvm_vj8_restore111\":true}");
 }
 public static void main(String[] args)throws Exception{
  if(args.length==2&&args[0].equals("--restart")){restart(new String(Files.readAllBytes(Paths.get(args[1])),StandardCharsets.UTF_8));return;}
  live();String encoded=codec();Files.write(Paths.get(args[0]),encoded.getBytes(StandardCharsets.UTF_8));
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"resident_route_labels111\":true,\"resident_timing_scalar_scope111\":true,\"resident_timing_vj8_restart111\":true,\"legacy_vj1_vj7_preserved111\":true,\"resident_timing_history_not_admission111\":true,\"physical_android_tested\":false}");
 }
}
