package com.hiro.ulike;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.HashSet;

/** Bounded per-capture comparison controls. Nothing is persisted: normal
 * quality is the process default and every comparison is consumed once. */
public final class CapturePolicy19122 {
    private CapturePolicy19122() {}
    private static final Object LOCK=new Object();
    private static final HashSet<Long> ACTIVE=new HashSet<Long>();
    private static int pending;
    private static volatile int active;
    private static final String KEY="hiro_capture_comparison19122";
    private static final String[] LABELS={
        "通常：画質を維持して操作負荷を抑える",
        "次の1枚だけ：1.9.121相当の負荷制御",
        "次の1枚だけ：CPUの上限を4並列に戻す",
        "次の1枚だけ：GPUの同時投入を2枠に戻す",
        "次の1枚だけ：保存中のHDプレビュー早期解除を有効",
        "次の1枚だけ：保存後の未使用領域整理を省略",
        "診断用の次の1枚：追加の画質処理を省略（画質が変わります）"
    };
    public static void begin19122(long id) {
        if(id<=0)return;
        synchronized(LOCK){
            if(ACTIVE.isEmpty()){active=pending;pending=0;}
            ACTIVE.add(Long.valueOf(id));
        }
    }
    public static void end19122(long id) {
        synchronized(LOCK){if(ACTIVE.remove(Long.valueOf(id))&&ACTIVE.isEmpty())active=0;}
    }
    /** Only arms a future capture; never changes an already-running save. */
    public static boolean arm19122(int mode) {
        synchronized(LOCK){
            if(mode<0||mode>=LABELS.length||!ACTIVE.isEmpty())return false;
            pending=mode;return true;
        }
    }
    public static int cpuWorkerLimit(){int m=active;return m==1||m==2?4:2;}
    public static int gpuBanks(){int m=active;return m==1||m==3?2:1;}
    public static long gpuSubmissionGapNanos(){int m=active;return m==1||m==3?0L:2000000L;}
    public static boolean earlyPreviewEnabled(){int m=active;return m==1||m==4;}
    public static boolean cleanupEnabled(){int m=active;return m!=1&&m!=5;}
    public static boolean bypassProcessing(){return active==6;}
    public static boolean diagnostic19122(){return active!=0;}
    public static String activeName19122(){return LABELS[active];}
    public static String summary19122(){synchronized(LOCK){return "撮影時の制御: "+LABELS[active]+"\n次の撮影: "+LABELS[pending]+"\n比較は1枚で通常に戻ります。通常は画質・解像度を維持します。";}}

    public static void install19122(PreferenceActivity activity) {
        if(activity==null)return;
        try{
            PreferenceScreen screen=activity.getPreferenceScreen();if(screen==null)return;
            if(activity.findPreference(KEY)!=null)return;
            Preference row=new Preference(activity);row.setKey(KEY);
            row.setTitle("撮影後の重さを比較する（次の1枚だけ）");
            row.setSummary(summary19122());row.setOnPreferenceClickListener(new Open(activity));
            screen.addPreference(row);
        }catch(RuntimeException ignored){}catch(LinkageError ignored){}catch(OutOfMemoryError ignored){}
    }
    private static final class Open implements Preference.OnPreferenceClickListener {
        private final java.lang.ref.WeakReference<PreferenceActivity> activity;
        Open(PreferenceActivity value){activity=new java.lang.ref.WeakReference<PreferenceActivity>(value);}
        public boolean onPreferenceClick(Preference row){
            PreferenceActivity a=activity.get();if(a==null||a.isFinishing()||a.isDestroyed())return true;
            try{new AlertDialog.Builder(a).setTitle("比較する処理を選択").setItems(LABELS,new Select(row)).setNegativeButton("閉じる",null).show();}
            catch(RuntimeException ignored){}catch(LinkageError ignored){}catch(OutOfMemoryError ignored){}
            return true;
        }
    }
    private static final class Select implements DialogInterface.OnClickListener {
        private final java.lang.ref.WeakReference<Preference> row;
        Select(Preference value){row=new java.lang.ref.WeakReference<Preference>(value);}
        public void onClick(DialogInterface dialog,int which){
            boolean accepted=arm19122(which);Preference p=row.get();
            if(p!=null)p.setSummary(accepted?summary19122():"保存処理中のため比較設定は変更しませんでした。保存後に選び直してください。");
        }
    }
}
