package com.hiro.ulike;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Per-photo coarse timers. This helper never owns a Bitmap or changes image pixels. */
public final class ProcessingTiming1947 {
    public static final String VERSION = "1.9.50";
    public static final int FUSION=0, NOISE=1, CORRECTION=2, ENCODE=3, SAVE=4;
    private static final String[] LABELS={"合成", "ノイズ除去", "補正", "圧縮", "保存"};
    private static final String STORE="hiro_ulike_timing_1947";
    private static final int MAX_TRACES=64, MAX_KEYS=192, MAX_INTERVALS=256;
    private static final long TTL=300000000000L;
    private static final Object LOCK=new Object();
    private static final ThreadLocal<Trace> CURRENT=new ThreadLocal<Trace>();
    private static final ArrayList<Trace> TRACES=new ArrayList<Trace>();
    private static final ArrayList<Key> KEYS=new ArrayList<Key>();
    private static final ArrayList<ViewBinding> VIEWS=new ArrayList<ViewBinding>();
    private static long nextId=Math.max(1L, System.currentTimeMillis()), displayedId;
    private static long lastViewRefresh;
    private static final long VIEW_INTERVAL=100000000L;
    private static Trace latest;
    private static String persisted;
    private static SharedPreferences preferences;
    private static boolean loaded;

    private ProcessingTiming1947() { }

    public static final class Trace {
        public final long id;
        public final long timestampMillis;
        final long started;
        final Stage[] stages={new Stage(),new Stage(),new Stage(),new Stage(),new Stage()};
        long shotId, finished;
        long updatedMillis;
        int state; // 0 processing, 1 complete, 2 error
        String requested="設定: 未計測", format="", detail="";
        int width, height;
        Trace(long value,long wall,long monotonic) { id=value; timestampMillis=wall; started=monotonic; updatedMillis=wall; }
    }
    public static final class Scope {
        final Trace previous;
        Scope(Trace value) { previous=value; }
    }
    public static final class Token {
        final Trace trace;
        final int stage;
        final long start;
        boolean ended;
        Token(Trace value,int kind,long now) { trace=value;stage=kind;start=now; }
    }
    private static final class Interval {
        long start,end;
        Interval(long a,long b) {start=a;end=b;}
    }
    private static final class Stage {
        final ArrayList<Interval> intervals=new ArrayList<Interval>();
        boolean skipped, incomplete;
        int active;
    }
    private static final class Key {
        final WeakReference<Object> object;
        final Trace trace;
        Key(Object key,Trace value) {object=new WeakReference<Object>(key);trace=value;}
    }
    private static final class ViewBinding {
        final WeakReference<PreferenceActivity> activity;
        final WeakReference<Preference> preference;
        ViewBinding(PreferenceActivity a,Preference p) {
            activity=new WeakReference<PreferenceActivity>(a);
            preference=new WeakReference<Preference>(p);
        }
    }

    public static void init(Context context) {
        if(context==null) return;
        try {
            Context app=context.getApplicationContext();
            SharedPreferences p=(app==null?context:app).getSharedPreferences(STORE, Context.MODE_PRIVATE);
            synchronized(LOCK) {
                preferences=p;
                if(loaded) return;
                loaded=true;
                long saved=p.getLong("id",0L);
                if(saved>=nextId && saved<Long.MAX_VALUE-1) nextId=saved+1;
                if(VERSION.equals(p.getString("version", ""))) {
                    String text=p.getString("summary", "");
                    if(text.length()>0 && (latest==null || saved<latest.id)) {
                        // latest in this process remains authoritative, regardless of saved state.
                        if(latest==null) { persisted=text;displayedId=saved; }
                    }
                }
            }
        } catch(Throwable ignored) { }
    }

    /** New diagnostic photo; no global last-bitmap or last-shot guessing. */
    public static Trace begin(Object key) {
        try {
            Trace trace;
            synchronized(LOCK) {
                long now=System.nanoTime();prune(now);
                trace=new Trace(nextId++, System.currentTimeMillis(),now);
                TRACES.add(trace);
                while(TRACES.size()>MAX_TRACES) TRACES.remove(0);
                if(key!=null) bindLocked(key,trace);
                latest=trace;displayedId=trace.id;
            }
            publish(trace,false);
            refreshViews(true);
            return trace;
        } catch(Throwable ignored) {return null;}
    }

    /** Link the exact ShotContext ID to the capture's diagnostic trace. */
    public static Trace associate(Object key,long shotId) {
        if(shotId<=0) return traceFor(key);
        try {
            Trace trace;
            boolean created=false;
            synchronized(LOCK) {
                long now=System.nanoTime();prune(now);
                trace=findShot(shotId);
                if(trace==null) {
                    trace=findKey(key);
                    if(trace!=null && trace.shotId!=0 && trace.shotId!=shotId) trace=null;
                }
                if(trace==null) {
                    trace=new Trace(nextId++,System.currentTimeMillis(),now);
                    TRACES.add(trace);
                    while(TRACES.size()>MAX_TRACES)TRACES.remove(0);
                    latest=trace;displayedId=trace.id;created=true;
                }
                trace.shotId=shotId;
                if(key!=null) bindLocked(key,trace);
            }
            if(created){publish(trace,false);refreshViews(true);}
            return trace;
        } catch(Throwable ignored) {return null;}
    }

    public static Trace current() { try{return CURRENT.get();}catch(Throwable ignored){return null;} }
    public static Trace forShot(long id) {
        if(id<=0)return null;
        try{synchronized(LOCK){prune(System.nanoTime());return findShot(id);}}catch(Throwable ignored){return null;}
    }
    /** Identity lookup first; only an explicitly entered thread scope may be fallback. */
    public static Trace traceFor(Object key) {
        try {
            synchronized(LOCK) { prune(System.nanoTime());Trace t=findKey(key);if(t!=null)return t; }
            return CURRENT.get();
        } catch(Throwable ignored) {return null;}
    }
    public static Trace forKey(Object key) {return traceFor(key);}
    public static void bind(Object key,Trace trace) {
        if(key==null || trace==null)return;
        try{synchronized(LOCK){prune(System.nanoTime());bindLocked(key,trace);}}catch(Throwable ignored){}
    }
    public static void transfer(Object source,Object destination) {
        if(destination==null || source==destination)return;
        try {
            synchronized(LOCK) {
                prune(System.nanoTime());Trace trace=findKey(source);
                // A scope can explicitly describe a transformed source without a bitmap binding.
                if(trace==null)trace=CURRENT.get();
                removeKey(destination);
                if(trace!=null)bindLocked(destination,trace);
            }
        } catch(Throwable ignored) { }
    }
    public static Scope enter(Trace trace) {
        try {Scope scope=new Scope(CURRENT.get());if(trace==null)CURRENT.remove();else CURRENT.set(trace);return scope;}
        catch(Throwable ignored){return null;}
    }
    public static Scope enterKey(Object key) {return enter(traceFor(key));}
    public static void restore(Scope scope) {
        if(scope==null)return;
        try {if(scope.previous==null)CURRENT.remove();else CURRENT.set(scope.previous);}catch(Throwable ignored){}
    }

    public static Token beginStage(int stage) {return beginStage(current(),stage);}
    public static Token beginStage(Trace trace,int stage) {
        if(trace==null || stage<0 || stage>=5)return null;
        try {
            Token token=new Token(trace,stage,System.nanoTime());
            synchronized(trace) {if(trace.state!=0)return null;Stage s=trace.stages[stage];s.active++;s.skipped=false;}
            return token;
        }catch(Throwable ignored){return null;}
    }
    public static void end(Token token) {
        if(token==null)return;
        try {
            Trace trace=token.trace;
            synchronized(trace) {
                if(token.ended)return;token.ended=true;
                Stage s=trace.stages[token.stage];if(s.active>0)s.active--;
                if(trace.state!=0)return; // Terminal record is immutable, including late worker callbacks.
                merge(s,token.start,System.nanoTime());
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** Explicit monotonic endpoints preserve overlap across asynchronous IO workers. */
    public static void recordInterval(Trace trace,int stage,long startNanos,long endNanos) {
        if(trace==null || stage<0 || stage>=5)return;
        try{synchronized(trace){if(trace.state!=0)return;merge(trace.stages[stage],startNanos,endNanos);trace.stages[stage].skipped=false;}publish(trace,false);}catch(Throwable ignored){}
    }
    /** Mark only a known execution bypass as skipped; unavailable instrumentation stays unknown. */
    public static void skip(Trace trace,int stage) {
        if(trace==null || stage<0 || stage>=5)return;
        try{synchronized(trace){if(trace.state!=0)return;Stage s=trace.stages[stage];if(s.active==0 && s.intervals.isEmpty() && !s.incomplete)s.skipped=true;}publish(trace,false);}catch(Throwable ignored){}
    }
    public static void unmeasured(Trace trace,int stage) {
        if(trace==null || stage<0 || stage>=5)return;
        try{synchronized(trace){if(trace.state!=0)return;trace.stages[stage].incomplete=true;trace.stages[stage].skipped=false;}publish(trace,false);}catch(Throwable ignored){}
    }
    public static void settings(Trace trace,String description) {
        if(trace==null || description==null)return;
        try{synchronized(trace){if(trace.state!=0)return;trace.requested=shortText(description,500);}publish(trace,false);}catch(Throwable ignored){}
    }
    public static void output(Trace trace,int width,int height,String format) {
        if(trace==null)return;
        try{synchronized(trace){if(trace.state!=0)return;trace.width=Math.max(0,width);trace.height=Math.max(0,height);trace.format=shortText(format,60);}publish(trace,false);}catch(Throwable ignored){}
    }
    public static void note(Trace trace,String description) {
        if(trace==null || description==null)return;
        try{
            synchronized(trace){
                if(trace.state!=0)return;
                String value=shortText(description,180);
                if(value.length()>0 && !trace.detail.contains(value))
                    trace.detail=shortText(trace.detail.length()==0?value:trace.detail+"\n"+value,540);
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    public static void finish(Trace trace,boolean success) {
        if(trace==null)return;
        try {
            synchronized(trace) {
                if(trace.state!=0)return;
                trace.finished=System.nanoTime();trace.state=success?1:2;
                for(Stage stage:trace.stages)if(stage.active>0)stage.incomplete=true;
            }
            publish(trace,true);
        }catch(Throwable ignored){}
    }

    /** Legacy PhotoDetail.status is intentionally not imported into this independent record. */
    public static void legacyStatus(String ignored) {refreshViews(true);}
    public static String summary() {
        try {
            synchronized(LOCK) {
                if(latest!=null)return render(latest);
                if(persisted!=null)return persisted;
            }
        }catch(Throwable ignored){}
        return "ULike v"+VERSION+" / 新しい撮影の計測待ち\n合成: 未計測 / ノイズ除去: 未計測 / 補正: 未計測 / 圧縮: 未計測 / 保存: 未計測";
    }

    /** Replaces the existing result row and its stale-result tap action in place. */
    public static void install(PreferenceActivity activity) {
        if(activity==null)return;
        try {
            init(activity);
            Preference target=findPreference(activity.getPreferenceScreen());
            if(target==null)return;
            target.setTitle("直近の撮影・工程別処理時間（タップで更新）");
            target.setSummary(summary());
            target.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
                public boolean onPreferenceClick(Preference preference){
                    try{preference.setSummary(summary());}catch(Throwable ignored){}
                    return true;
                }
            });
            synchronized(LOCK) {
                for(int i=VIEWS.size()-1;i>=0;i--){PreferenceActivity a=VIEWS.get(i).activity.get();if(a==null || a==activity)VIEWS.remove(i);}
                while(VIEWS.size()>=4)VIEWS.remove(0);
                VIEWS.add(new ViewBinding(activity,target));
            }
        }catch(Throwable ignored){}
    }

    private static Preference findPreference(Preference preference) {
        if(preference==null)return null;
        String key=preference.getKey();CharSequence title=preference.getTitle();
        if("detail_last_result".equals(key) || "hiro_photo_detail_last_result".equals(key)
            || (title!=null && (title.toString().contains("直近の画質処理結果") || title.toString().contains("直近の撮影・工程別"))))return preference;
        if(preference instanceof PreferenceGroup){PreferenceGroup group=(PreferenceGroup)preference;for(int i=0;i<group.getPreferenceCount();i++){Preference result=findPreference(group.getPreference(i));if(result!=null)return result;}}
        return null;
    }
    private static void publish(Trace trace,boolean save) {
        if(trace==null)return;
        try {
            synchronized(LOCK) {
                if(trace.id<displayedId)return;
                displayedId=trace.id;latest=trace;
                synchronized(trace){if(trace.state!=0 && !save)return;trace.updatedMillis=System.currentTimeMillis();}
                if(save && preferences!=null) {
                    String text=render(trace);
                    preferences.edit().putString("version",VERSION).putLong("id",trace.id).putString("summary",text).apply();
                    persisted=text;
                }
            }
            refreshViews(save);
        }catch(Throwable ignored){}
    }
    private static void refreshViews(boolean force) {
        try {
            ArrayList<ViewBinding> copy;
            synchronized(LOCK){
                if(VIEWS.isEmpty())return;
                long now=System.nanoTime();
                if(!force && now-lastViewRefresh<VIEW_INTERVAL)return;
                lastViewRefresh=now;
                copy=new ArrayList<ViewBinding>(VIEWS);
            }
            for(final ViewBinding binding:copy){final Activity activity=binding.activity.get();if(activity==null)continue;
                activity.runOnUiThread(new Runnable(){public void run(){try{Preference p=binding.preference.get();if(p!=null)p.setSummary(summary());}catch(Throwable ignored){}}});
            }
        }catch(Throwable ignored){}
    }
    private static String render(Trace trace) {
        synchronized(trace) {
            StringBuilder text=new StringBuilder(520);
            text.append("ULike v").append(VERSION).append(" / 撮影 #").append(trace.id)
                .append(" / ").append(trace.state==0?"処理中":trace.state==1?"完了":"エラー");
            text.append("\n撮影日時: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.JAPAN).format(new Date(trace.timestampMillis)));
            text.append("\n最終更新: ").append(new SimpleDateFormat("HH:mm:ss.SSS",Locale.JAPAN).format(new Date(trace.updatedMillis)));
            text.append("\n").append(trace.requested);
            if(trace.width>0 && trace.height>0)text.append("\n出力: ").append(trace.width).append('×').append(trace.height).append(trace.format.length()==0?"":" / "+trace.format);
            for(int i=0;i<5;i++) {
                Stage s=trace.stages[i];text.append('\n').append(LABELS[i]).append(": ");
                if(s.incomplete)text.append(s.intervals.isEmpty()?"未計測":"未計測（途中のみ計測）");
                else if(s.active>0)text.append("処理中");
                else if(!s.intervals.isEmpty())text.append(milliseconds(duration(s))).append(" ms");
                else text.append(s.skipped?"実施なし":"未計測");
            }
            long until=trace.finished==0?System.nanoTime():trace.finished;
            text.append("\n全体経過: ").append(milliseconds(Math.max(0L,until-trace.started))).append(" ms");
            text.append("（並行工程の時間は重複します）");
            text.append("\n標準美顔処理・撮影待機は工程別計測の対象外です。");
            if(trace.detail.length()>0)text.append("\n").append(trace.detail);
            return text.toString();
        }
    }
    private static String milliseconds(long nanos) {return nanos>0 && nanos<1000000L?"<1":String.valueOf(nanos/1000000L);}
    private static String shortText(String text,int limit) {if(text==null)return "";return text.length()>limit?text.substring(0,limit):text;}
    private static long duration(Stage stage) {long value=0;for(Interval i:stage.intervals)value+=Math.max(0L,i.end-i.start);return value;}
    /** Sorted union, including nested and disjoint worker intervals. Never first-to-last span. */
    private static void merge(Stage stage,long start,long end) {
        if(end<start){stage.incomplete=true;return;}
        int at=0;
        while(at<stage.intervals.size() && stage.intervals.get(at).end<start)at++;
        while(at<stage.intervals.size() && stage.intervals.get(at).start<=end){Interval i=stage.intervals.remove(at);start=Math.min(start,i.start);end=Math.max(end,i.end);}
        stage.intervals.add(at,new Interval(start,end));
        if(stage.intervals.size()>MAX_INTERVALS){stage.incomplete=true;stage.intervals.clear();}
    }
    private static Trace findShot(long id) {for(int i=TRACES.size()-1;i>=0;i--){Trace t=TRACES.get(i);if(t.shotId==id)return t;}return null;}
    private static Trace findKey(Object key) {if(key==null)return null;for(int i=KEYS.size()-1;i>=0;i--){Key value=KEYS.get(i);if(value.object.get()==key)return value.trace;}return null;}
    private static void removeKey(Object key) {for(int i=KEYS.size()-1;i>=0;i--){Object object=KEYS.get(i).object.get();if(object==null || object==key)KEYS.remove(i);}}
    private static void bindLocked(Object key,Trace trace) {removeKey(key);while(KEYS.size()>=MAX_KEYS)KEYS.remove(0);KEYS.add(new Key(key,trace));}
    private static void prune(long now) {
        for(int i=KEYS.size()-1;i>=0;i--){Key key=KEYS.get(i);if(key.object.get()==null || now-key.trace.started>TTL)KEYS.remove(i);}
        for(int i=TRACES.size()-1;i>=0;i--){if(now-TRACES.get(i).started>TTL)TRACES.remove(i);}
    }
}
