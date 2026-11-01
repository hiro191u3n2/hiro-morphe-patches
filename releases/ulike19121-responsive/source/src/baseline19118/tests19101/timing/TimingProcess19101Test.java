package com.hiro.ulike;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Actual Timing plus actual Journal, exercised in two independent JVMs. */
public final class TimingProcess19101Test {
 static int assertions;
 static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
 static Field field(String name)throws Exception{Field f=ProcessingTiming1947.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static void reset()throws Exception{Timing1982Test.reset();((List<?>)field("RECENT1998").get(null)).clear();}
 static final class Preferences implements SharedPreferences {
  final Map<String,Object> values=new HashMap<String,Object>();int writes;
  public synchronized long getLong(String key,long fallback){Object v=values.get(key);return v instanceof Long?(Long)v:fallback;}
  public synchronized String getString(String key,String fallback){Object v=values.get(key);return v instanceof String?(String)v:fallback;}
  public Editor edit(){return new Editor(){final Map<String,Object> changed=new HashMap<String,Object>();
   public Editor putString(String key,String value){changed.put(key,value);return this;}
   public Editor putLong(String key,long value){changed.put(key,value);return this;}
   public boolean commit(){synchronized(Preferences.this){values.putAll(changed);writes++;}return true;}
   public void apply(){commit();}
  };}
 }
 static final class Stores extends Context {
  final Map<String,Preferences> all=new HashMap<String,Preferences>();
  public synchronized SharedPreferences getSharedPreferences(String name,int mode){Preferences p=all.get(name);if(p==null){p=new Preferences();all.put(name,p);}return p;}
  Preferences timing(){return (Preferences)getSharedPreferences("hiro_ulike_timing_1947",0);}
 }
 static void load(Stores stores,String summary,long id,String version){Preferences p=stores.timing();p.values.put("summary",summary);p.values.put("id",Long.valueOf(id));p.values.put("version",version);ProcessingTiming1947.init(stores);}
 static void produce(String path)throws Exception{
  reset();String id=VerificationJournal19101.processId();check(id.length()>0,"actual journal process ID exists before Context");
  ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(19101,System.currentTimeMillis(),System.nanoTime());t.captureOrdinal1990=7;
  check(t.processId19101.equals(id),"capture owns actual pre-init journal ID");
  check(Modifier.isFinal(ProcessingTiming1947.Trace.class.getDeclaredField("processId19101").getModifiers()),"capture process ID is immutable");
  Stores stores=new Stores();ProcessingTiming1947.init(stores);check(id.equals(VerificationJournal19101.processId()),"initialize cannot change process identity");
  ProcessingTiming1947.noiseBackend(t,16,0);ProcessingTiming1947.publication1990(t,true);ProcessingTiming1947.finish(t,true);
  String saved=ProcessingTiming1947.summary();check(saved.equals(stores.timing().getString("summary","")),"capture process ID persists in exact saved snapshot");
  check(saved.contains("\n撮影記録のプロセスID: "+id+"\n"),"stored snapshot has fixed process identity after GPU policy");
  check(saved.contains("撮影した起動内の撮影順: 7枚目")&&!saved.contains("このプロセスの撮影順:"),"saved ordinal explicitly belongs to capture startup");
  int writes=stores.timing().writes;long finished=t.finished,updated=t.updatedMillis;
  String view=ProcessingTiming1947.liveSummary1979();check(view.startsWith(saved+"\n\n表示時点の"),"live snapshot keeps exact saved prefix");
  check(view.contains("表示中の撮影記録: 現在の起動で作成した記録"),"actual current-process capture is distinguished");
  check(view.contains("表示時点のプロセスID: "+id),"live process identity uses same real journal");
  check(!saved.contains("検証履歴の永続記録:")&&view.contains("検証履歴の永続記録:"),"real journal appears only in live display, never saved photo");
  check(view.indexOf("検証履歴の永続記録:")==view.lastIndexOf("検証履歴の永続記録:"),"real historical journal appended exactly once");
  ProcessingTiming1947.noiseBackend(t,0,99);ProcessingTiming1947.finish(t,false);ProcessingTiming1947.liveSummary1979();
  check(ProcessingTiming1947.summary().equals(saved)&&stores.timing().writes==writes,"live and late callbacks do not rewrite saved summary");
  check(t.finished==finished&&t.updatedMillis==updated&&t.strongGpuStrips==16&&t.strongCpuStrips==0,"completed times and GPU counters remain immutable");
  try(DataOutputStream out=new DataOutputStream(new FileOutputStream(path))){out.writeUTF(id);out.writeUTF(ProcessingTiming1947.VERSION);out.writeLong(t.id);out.writeUTF(saved);}
 }
 static void consume(String path)throws Exception{
  String previous,version,saved;long number;
  try(DataInputStream in=new DataInputStream(new FileInputStream(path))){previous=in.readUTF();version=in.readUTF();number=in.readLong();saved=in.readUTF();}
  String current=VerificationJournal19101.processId();check(!current.equals(previous),"fresh JVM has distinct real process identity");
  reset();Stores stores=new Stores();load(stores,saved,number,version);
  check(ProcessingTiming1947.summary().equals(saved),"previous process photo restores byte-for-byte");
  String view=ProcessingTiming1947.liveSummary1979();check(view.startsWith(saved+"\n\n表示時点の"),"old photo retains its saved process ID and ordinal");
  check(view.contains("表示中の撮影記録: 別の起動で保存された記録"),"previous process photo cannot appear current");
  check(view.contains("表示時点のプロセスID: "+current),"new live queue owns new process ID");
  check(view.contains("現在の起動では保存完了を確認した撮影記録なし"),"empty current recent history remains explicit beside old photo");
  check(stores.timing().writes==0&&saved.equals(stores.timing().getString("summary","")),"opening old photo cannot persist live data");
  reset();String legacy=saved.replace("ULike v"+version+" / ","ULike v1.9.100 / ").replace("撮影記録のプロセスID: "+previous+"\n","").replace("撮影した起動内の撮影順:","このプロセスの撮影順:");
  load(stores,legacy,number,"1.9.100");check(ProcessingTiming1947.summary().equals(legacy),"v100 legacy summary is preserved without relabeling");
  view=ProcessingTiming1947.liveSummary1979();check(view.contains("撮影した起動のID未記録"),"missing old process ID stays unknown");
  check(view.contains("この撮影と過去の検証履歴との対応は判定できません"),"missing ID does not deny separately restored journal history");
  check(!view.contains("表示中の撮影記録: 現在の起動で作成した記録"),"v100 capture is never assigned current identity");
  check(view.contains("表示時点の実行版: ULike v"+ProcessingTiming1947.VERSION)&&view.startsWith("ULike v1.9.100 / "),"old photo version and current installed version remain separately visible");
  check(view.contains("撮影順は記録当時の起動内の順番です"),"unknown legacy identity never makes old ordinal a current capture count");
  check(view.startsWith(legacy+"\n\n")&&stores.timing().writes==0,"v100 saved ordinal is not rewritten by current display");
  reset();String spoof="ULike v"+version+" / 撮影 #99 / 完了\n旧形式: 未記録\n撮影記録のプロセスID: "+current;
  load(stores,spoof,99,version);check(ProcessingTiming1947.liveSummary1979().contains("撮影した起動のID未記録"),"later settings/detail line cannot masquerade as captured identity");
 }
 public static void main(String[] args)throws Exception{
  check(args.length==2,"mode and transfer path required");
  if(args[0].equals("produce"))produce(args[1]);else if(args[0].equals("consume"))consume(args[1]);else throw new AssertionError("mode");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"real_journal_process_identity\":true,\"saved_snapshot_process_identity\":true,\"cross_process_photo_live_separation\":true,\"legacy_photo_identity_unknown\":true,\"empty_current_history_explicit\":true,\"diagnostic_snapshot_immutable\":true}");
 }
}
