package com.hiro.ulike;
import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/** Exercises overlap, exact photo identity, stale UI/persistence, order and fail-open behavior. */
public final class ProcessingTiming1947Test {
 static int checks;
 static void ok(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static void has(String part){ok(ProcessingTiming1947.summary().contains(part),"Missing "+part+" in "+ProcessingTiming1947.summary());}
 static void reset()throws Exception{
  for(String name:new String[]{"TRACES","KEYS","VIEWS"}){Field f=ProcessingTiming1947.class.getDeclaredField(name);f.setAccessible(true);((ArrayList<?>)f.get(null)).clear();}
  for(String name:new String[]{"latest","persisted","preferences"}){Field f=ProcessingTiming1947.class.getDeclaredField(name);f.setAccessible(true);f.set(null,null);}
  Field l=ProcessingTiming1947.class.getDeclaredField("loaded");l.setAccessible(true);l.setBoolean(null,false);
  Field d=ProcessingTiming1947.class.getDeclaredField("displayedId");d.setAccessible(true);d.setLong(null,0);
  ProcessingTiming1947.restore(ProcessingTiming1947.enter(null));
 }
 public static void main(String[] args)throws Exception {
  reset();
  Context context=new Context();
  context.getSharedPreferences("hiro_photo_detail",0).edit().putString("last_result","v1.7.5 2738ms").apply();
  ProcessingTiming1947.init(context);
  has("v1.9.52");has("新しい撮影の計測待ち");ok(!ProcessingTiming1947.summary().contains("2738"),"Never load legacy status");
  Object key=new Object();ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(key);
  ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.FUSION,0,10000000);
  ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.FUSION,5000000,15000000);
  ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.FUSION,20000000,22000000);
  has("合成: 17 ms"); // summed workers and first-to-last would both incorrectly show 22ms.
  ProcessingTiming1947.skip(trace,ProcessingTiming1947.FUSION);has("合成: 17 ms");
  ProcessingTiming1947.skip(trace,ProcessingTiming1947.ENCODE);has("圧縮: 実施なし");has("保存: 未計測");
  ProcessingTiming1947.unmeasured(trace,ProcessingTiming1947.FUSION);has("合成: 未計測（途中のみ計測）");
  ProcessingTiming1947.settings(trace,"ノイズ: 最強 / 質感保護: オン");
  ProcessingTiming1947.output(trace,4284,5712,"HEIF");has("4284×5712 / HEIF");has("質感保護: オン");
  ProcessingTiming1947.finish(trace,true);has("完了");has("撮影日時:");has("最終更新:");

  // Shared shot ID, copied bitmap identity, scope nesting, no global fallback.
  ProcessingTiming1947.Trace a=ProcessingTiming1947.associate(key,70);
  ok(a==trace,"Existing exact capture identity reused");
  Object copy=new Object();ProcessingTiming1947.transfer(key,copy);
  ok(ProcessingTiming1947.traceFor(copy)==trace,"Copy preserves shot");
  ok(ProcessingTiming1947.traceFor(new Object())==null,"No latest-shot guessing");
  ProcessingTiming1947.Trace b=ProcessingTiming1947.associate(new Object(),71);
  ProcessingTiming1947.Scope outer=ProcessingTiming1947.enter(a);
  ok(ProcessingTiming1947.current()==a,"outer binding");
  ProcessingTiming1947.Scope inner=ProcessingTiming1947.enter(b);
  ok(ProcessingTiming1947.current()==b,"nested binding");ProcessingTiming1947.restore(inner);
  ok(ProcessingTiming1947.current()==a,"nested restore");ProcessingTiming1947.restore(outer);
  ok(ProcessingTiming1947.current()==null,"outer cleanup");
  Object unknown=new Object();ProcessingTiming1947.transfer(unknown,copy);
  ok(ProcessingTiming1947.traceFor(copy)==null,"Unknown transform erases unrelated old identity");
  ok(ProcessingTiming1947.associate(new Object(),71)==b,"Same shot ID uses same trace");

  // Overlapping real worker callbacks merge atomically, disjoint gaps remain excluded.
  final ProcessingTiming1947.Trace parallel=ProcessingTiming1947.begin(new Object());
  final CountDownLatch start=new CountDownLatch(1),done=new CountDownLatch(32);
  for(int i=0;i<32;i++){final int n=i;new Thread(new Runnable(){public void run(){try{start.await();ProcessingTiming1947.recordInterval(parallel,ProcessingTiming1947.NOISE,n*1000000L,(n+2)*1000000L);}catch(Exception e){throw new AssertionError(e);}finally{done.countDown();}}}).start();}
  start.countDown();done.await();
  ProcessingTiming1947.recordInterval(parallel,ProcessingTiming1947.NOISE,100000000,110000000);
  ProcessingTiming1947.recordInterval(parallel,ProcessingTiming1947.NOISE,105000000,106000000);
  has("ノイズ除去: 43 ms");
  ProcessingTiming1947.Token token=ProcessingTiming1947.beginStage(parallel,ProcessingTiming1947.CORRECTION);
  has("補正: 処理中");ProcessingTiming1947.end(token);ProcessingTiming1947.end(token);
  ok(!ProcessingTiming1947.summary().contains("補正: 処理中"),"Double end is idempotent");
  ProcessingTiming1947.recordInterval(parallel,ProcessingTiming1947.SAVE,100,50);has("保存: 未計測");

  // Completion while a worker is still active freezes the terminal UI and persisted record.
  ProcessingTiming1947.Trace terminal=ProcessingTiming1947.begin(new Object());
  ProcessingTiming1947.recordInterval(terminal,ProcessingTiming1947.NOISE,0,10000000);
  ProcessingTiming1947.settings(terminal,"撮影時設定");ProcessingTiming1947.output(terminal,100,200,"HEIF");
  ProcessingTiming1947.Token late=ProcessingTiming1947.beginStage(terminal,ProcessingTiming1947.FUSION);
  ProcessingTiming1947.finish(terminal,true);
  String frozen=ProcessingTiming1947.summary();
  Context.Prefs terminalPrefs=context.stores.get("hiro_ulike_timing_1947");
  ok(frozen.equals(terminalPrefs.getString("summary","")),"Terminal UI initially equals persistence");
  ok(frozen.contains("合成: 未計測"),"Unjoined worker is unresolved at finish");
  ProcessingTiming1947.end(late);ProcessingTiming1947.end(late);
  ProcessingTiming1947.recordInterval(terminal,ProcessingTiming1947.ENCODE,0,99000000);
  ProcessingTiming1947.unmeasured(terminal,ProcessingTiming1947.NOISE);
  ProcessingTiming1947.skip(terminal,ProcessingTiming1947.SAVE);
  ProcessingTiming1947.settings(terminal,"遅延設定変更");ProcessingTiming1947.output(terminal,999,999,"JPEG");
  ProcessingTiming1947.note(terminal,"遅延注記");ProcessingTiming1947.finish(terminal,false);
  ok(frozen.equals(ProcessingTiming1947.summary()),"All late callbacks leave terminal summary immutable");
  ok(frozen.equals(terminalPrefs.getString("summary","")),"Terminal UI and persistence remain identical after late callbacks");

  // Newer photo can finish first; late older completion cannot replace UI or persisted result.
  ProcessingTiming1947.Trace newer=ProcessingTiming1947.begin(new Object());
  ProcessingTiming1947.finish(newer,true);String expected=ProcessingTiming1947.summary();
  ProcessingTiming1947.finish(parallel,false);
  ok(ProcessingTiming1947.summary().equals(expected),"Late older callback cannot overwrite latest photo");
  Context.Prefs prefs=context.stores.get("hiro_ulike_timing_1947");
  ok(prefs.getLong("id",0)==newer.id,"Persistence uses latest photo ID");

  // Existing result row, including its tap listener, is replaced in place.
  PreferenceActivity activity=new PreferenceActivity();PreferenceGroup category=new PreferenceGroup();category.setKey("hiro_photo_detail_category");
  Preference row=new Preference();row.setTitle("直近の画質処理結果（タップで更新）");row.setSummary("v1.7.5 2738ms");
  row.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){public boolean onPreferenceClick(Preference p){p.setSummary("v1.7.5 2738ms");return true;}});
  category.addPreference(row);activity.screen.addPreference(category);
  ProcessingTiming1947.install(activity);ok(row.getSummary().toString().equals(expected),"Install replaces stale text");
  row.click();ok(!row.getSummary().toString().contains("2738"),"Tap cannot resurrect legacy persisted data");
  ProcessingTiming1947.Trace ui=ProcessingTiming1947.begin(new Object());
  ok(row.getSummary().toString().contains("撮影 #"+ui.id),"New capture publishes into existing UI");
  int before=activity.dispatchCount;
  for(int i=0;i<100;i++)ProcessingTiming1947.recordInterval(ui,ProcessingTiming1947.NOISE,i*1000000L,(i+2)*1000000L);
  has("ノイズ除去: 101 ms");ok(activity.dispatchCount-before<25,"Progress throttle avoids dispatching one UI render per strip");
  ProcessingTiming1947.note(ui,"従来経路へ切替");ProcessingTiming1947.note(ui,"標準美顔処理は未計測");ProcessingTiming1947.note(ui,"従来経路へ切替");
  has("従来経路へ切替");has("標準美顔処理は未計測");has("標準美顔処理・撮影待機は工程別計測の対象外");
  ProcessingTiming1947.finish(ui,false);ok(row.getSummary().toString().contains("エラー"),"Error outcome is visible immediately despite throttle");
  ok(row.getSummary().toString().contains("ノイズ除去: 101 ms"),"Throttling never loses exact intervals");

  // The helper is observational even when preferences storage fails.
  Context broken=new Context();broken.broken=true;ProcessingTiming1947.init(broken);
  activity.stores.get("hiro_ulike_timing_1947").broken=true;
  ProcessingTiming1947.Trace disk=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.finish(disk,true);
  has("完了");ProcessingTiming1947.beginStage(null,-1);ProcessingTiming1947.end(null);ProcessingTiming1947.skip(null,0);
  ProcessingTiming1947.output(null,0,0,null);ProcessingTiming1947.finish(null,false);ok(true,"All optional malformed/no-context calls fail open");

  // Process restart restores only current-version data, version mismatches are not reused.
  reset();Context restore=new Context();restore.getSharedPreferences("hiro_ulike_timing_1947",0).edit().putString("version","1.9.52").putLong("id",99).putString("summary","Current version saved result").apply();
  ProcessingTiming1947.init(restore);ok(ProcessingTiming1947.summary().equals("Current version saved result"),"Current record restore");
  reset();Context stale=new Context();stale.getSharedPreferences("hiro_ulike_timing_1947",0).edit().putString("version","1.9.50").putLong("id",100).putString("summary","Old build result").apply();
  ProcessingTiming1947.init(stale);has("新しい撮影の計測待ち");

  // Bounded metadata retains no strong image references and cannot grow indefinitely.
  for(int i=0;i<300;i++)ProcessingTiming1947.begin(new Object());
  for(String name:new String[]{"KEYS","TRACES"}){Field f=ProcessingTiming1947.class.getDeclaredField(name);f.setAccessible(true);ok(((ArrayList<?>)f.get(null)).size()<=(name.equals("KEYS")?192:64),name+" bounded");}
  System.out.println("PASS "+checks+" checks: interval union, asynchronous order, identity/scope, current-version persistence, stale UI, errors and bounded metadata");
 }
}
