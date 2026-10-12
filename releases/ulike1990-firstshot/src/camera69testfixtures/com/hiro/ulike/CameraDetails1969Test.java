package com.hiro.ulike;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.widget.Toast;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/** Exercises the real production menu with controlled inherited Android boundaries. */
public final class CameraDetails1969Test {
    static int checks;
    static final String SUFFIX="\n\nGPU/CPUの区間数は、この写真の強ノイズ処理だけの記録です。未観測はGPU使用の有無を判定できていない状態です。";
    static final String FAILURE="処理時間・GPU使用状況を表示できませんでした。";
    interface Condition{boolean ok()throws Exception;}
    static void check(boolean yes,String name){checks++;if(!yes)throw new AssertionError(name);}
    static void until(Condition condition,String label)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(System.nanoTime()<end){if(condition.ok())return;Thread.sleep(10);}
        throw new AssertionError("timeout: "+label);
    }
    static Object field(String name)throws Exception{Field f=CameraTrace1965.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    static List<File> files(File directory)throws Exception{
        List<File> result=new ArrayList<File>();
        if(directory.exists())try(java.util.stream.Stream<Path> paths=Files.walk(directory.toPath())){paths.filter(p->p.toString().endsWith(".jsonl")).sorted().forEach(p->result.add(p.toFile()));}
        return result;
    }
    static String records(File root)throws Exception{StringBuilder result=new StringBuilder();for(File f:files(root))result.append(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));return result.toString();}
    static AlertDialog menu(Preference preference)throws Exception{AlertDialog.last=null;CameraTrace1965.open(preference);Handler.dispatchAll();check(AlertDialog.last!=null,"menu opens");return AlertDialog.last;}
    static final class Effects {
        final long sequence;final int inserts,updates,deletes,streams,snapshots;final Intent started;final String status,records;
        Effects(Activity app,CameraTrace1965.Storage storage)throws Exception{
            sequence=((Long)field("sequence")).longValue();ContentResolver r=app.resolver;inserts=r.inserts;updates=r.updates;deletes=r.deletes;streams=r.streams;
            started=app.started;status=CameraTrace1965.exportStatus1967();snapshots=files(new File(storage.directory,"protected")).size();records=records(storage.directory);
        }
        void same(Activity app,CameraTrace1965.Storage storage)throws Exception{
            Handler.dispatchAll();Effects now=new Effects(app,storage);
            check(now.sequence==sequence,"details do not enqueue any diagnostic event or snapshot request");
            check(now.inserts==inserts&&now.updates==updates&&now.deletes==deletes&&now.streams==streams,"details perform no provider export transaction");
            check(now.snapshots==snapshots,"details produce no protected diagnostic snapshot");
            check(now.records.equals(records),"details leave camera evidence unchanged");
            check(now.started==started,"details do not launch share UI");
            check(now.status.equals(status),"details do not alter export status");
        }
    }
    static void run(File root)throws Exception{
        Looper.getMainLooper();root.mkdirs();Activity app=new Activity(root);Preference preference=new Preference(app);
        preference.summary="existing preference summary";
        AlertDialog first=menu(preference);
        check(Arrays.equals(first.items,new String[]{"直前の不具合を記録（アプリ内）","診断記録を共有","診断ZIPを保存（ダウンロード/ULike）","処理時間・GPU使用状況を表示"}),"original menu indices preserved and details appended as fourth item");
        until(()->field("STORE")!=null,"writer initialized");final CameraTrace1965.Storage storage=(CameraTrace1965.Storage)field("STORE");
        until(()->records(storage.directory).contains("process_start"),"startup record persisted before side effect comparison");
        StringBuilder longSummary=new StringBuilder("ULike v1.9.69 / 完了\n");
        for(int i=0;i<800;i++)longSummary.append("工程別の処理情報 ").append(i).append(" 暗部・細部・GPU認証の状態\n");
        longSummary.append("強ノイズ処理: GPU 13区間 / CPU 0区間\n最後の詳細: 全出力と末尾のGPU使用状況");
        String complete=longSummary.toString();ProcessingTiming1947.value=complete;ProcessingTiming1947.failure=null;
        Effects before=new Effects(app,storage);int calls=ProcessingTiming1947.calls;first.clickItem(3);AlertDialog details=AlertDialog.last;
        check(details!=first,"fourth action opens a separate detail dialog");
        check("撮影の処理時間・GPU使用状況".equals(details.title),"detail title clearly identifies timing and GPU status");
        check(details.message.equals(complete+SUFFIX),"full summary is byte-for-byte intact followed only by explanatory suffix");
        check(details.message.length()>20000&&details.message.contains("最後の詳細"),"long summary and tail details survive without clipping");
        check(details.items==null,"detail content uses native dialog message without competing menu");
        check(details.clicks.containsKey("negative")&&details.clicks.get("negative")==null,"native close button uses default dismiss without anonymous callback");
        check(ProcessingTiming1947.calls==calls+1,"detail action reads the full timing summary once");
        check("existing preference summary".contentEquals(preference.summary),"opening details does not rewrite timing row summary");
        before.same(app,storage);
        ProcessingTiming1947.value="未観測\n強ノイズ処理: GPU/CPU区間数 未観測";
        AlertDialog unknownMenu=menu(preference);before=new Effects(app,storage);unknownMenu.clickItem(3);details=AlertDialog.last;
        check(details.message.equals(ProcessingTiming1947.value+SUFFIX),"unobserved summary is preserved and explained");
        check(!details.message.contains("GPU 0")&&!details.message.contains("CPU 0"),"details never invent zero counts for unobserved processing");before.same(app,storage);
        for(int fault=0;fault<3;fault++){
            ProcessingTiming1947.failure=fault==0?new IllegalStateException("private driver data that must not appear in the UI "+new String(new char[10000])):null;
            ProcessingTiming1947.value=fault==1?null:Integer.valueOf(3);Toast.last=null;AlertDialog failedMenu=menu(preference);before=new Effects(app,storage);failedMenu.clickItem(3);Handler.dispatchAll();
            check(AlertDialog.last==failedMenu,"invalid summary cannot create a misleading details dialog");
            check(FAILURE.equals(Toast.last)&&Toast.last.length()<64,"optional reflection failure displays only bounded generic Japanese toast");before.same(app,storage);
        }
        ProcessingTiming1947.failure=null;ProcessingTiming1947.value="復旧済み\nGPU 13区間 / CPU 0区間";AlertDialog recovery=menu(preference);recovery.clickItem(3);
        check(AlertDialog.last.message.equals(ProcessingTiming1947.value+SUFFIX),"details recover after optional reflection failure");
        // Existing menu close behavior must still refresh the original row summary.
        AlertDialog closeMenu=menu(preference);closeMenu.click("negative");check(ProcessingTiming1947.value.equals(preference.summary),"original menu close still refreshes timing row summary");
        // Indices 0, 1 and 2 retain their real private-record/share/direct-save effects.
        ProcessingTiming1947.value="not used by diagnostic actions";
        int summaryCalls=ProcessingTiming1947.calls;int count=files(new File(storage.directory,"protected")).size();long sequence=((Long)field("sequence")).longValue();int inserts=app.resolver.inserts;
        AlertDialog privateMenu=menu(preference);privateMenu.clickItem(0);
        until(()->files(new File(storage.directory,"protected")).size()>count,"original private snapshot action");Handler.dispatchAll();
        check(((Long)field("sequence")).longValue()==sequence+1,"index zero queues only its original private request");
        check(records(storage.directory).contains("user_snapshot"),"index zero retains snapshot evidence");
        check(app.resolver.inserts==inserts&&app.started==null,"index zero stays private");
        final int sharePrevious=app.resolver.updates;AlertDialog shareMenu=menu(preference);shareMenu.clickItem(1);
        until(()->app.resolver.updates>sharePrevious&&CameraTrace1965.exportStatus1967().contains("保存済み"),"original share commits ZIP");
        until(()->{Handler.dispatchAll();return app.started!=null;},"original share chooser");
        Intent share=app.started.target==null?app.started:app.started.target;
        check(Intent.ACTION_SEND.equals(share.action),"index one keeps existing share action");
        check((share.flags&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0&&(share.flags&Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==0,"share permissions remain read only");
        app.started=null;final int directPrevious=app.resolver.updates;AlertDialog directMenu=menu(preference);directMenu.clickItem(2);
        until(()->app.resolver.updates>directPrevious&&CameraTrace1965.exportStatus1967().contains("保存済み"),"original direct Downloads save");Handler.dispatchAll();
        check(app.started==null,"index two retains direct save without chooser");
        check("Download/ULike".equals(app.resolver.insertedValues.get(app.resolver.insertedValues.size()-1).get("relative_path")),"index two retains existing Downloads directory");
        check(app.resolver.data.get(app.resolver.last).length>0,"original direct save still emits actual ZIP bytes");
        check(ProcessingTiming1947.calls==summaryCalls,"original diagnostic actions do not invoke GPU summary helper");
        check(!app.filesOnMain&&!app.resolver.mainIo,"new UI preserves background filesystem and provider work");
    }
    public static void main(String[] args)throws Exception{
        run(new File(args[0]));System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"physical_android_tested\":false}");
    }
}
