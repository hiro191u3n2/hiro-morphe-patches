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
import java.util.LinkedHashMap;
import java.util.Locale;

/** Per-photo coarse timers. This helper never owns a Bitmap or changes image pixels. */
public final class ProcessingTiming1947 {
    public static final String VERSION = "1.9.87";
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
    private static volatile long captureEpoch1953;
    private static long lastViewRefresh;
    private static final long VIEW_INTERVAL=100000000L;
    private static Trace latest;
    private static String persisted;
    private static SharedPreferences preferences;
    private static boolean loaded;
    private static final String[] STRONG_REASONS_1970={"awaiting_quality","quality_rejected","shader_unsupported",
        "memory_budget","session_unavailable","bank_busy","upload_failed","execution_failed",
        "readback_failed","policy_mismatch","cancelled","unknown",
        "cached_legacy_negative","cached_argb_negative","cached_confidence_negative","cached_policy_negative",
        "cache_limit_negative","new_argb_mismatch","new_confidence_mismatch","policy_failure",
        "submit_failed","output_shape_failed","native_failure_unknown",
        "cold_proof_budget","proof_in_flight","cold_bank_busy","proof_already_attempted",
        "deferred_quality1978","cpu_faster1978",
        "route_unqualified1982","route_exact_rejected1982","route_speed_condition1982",
        "route_unqualified_exact1982","route_unqualified_speed1982","route_exact_speed1982",
        "route_unqualified_exact_speed1982","route_unavailable1982",
        "bank_unavailable1982","bank_exact_rejected1982","bank_unqualified1982","bank_speed_condition1982",
        "bank_budget1982","bank_waiter1982","bank_no_prediction1982","bank_prediction_budget1982"};
    private static final String[] STRONG_REASON_LABELS_1970={"画質確認待ち","画質確認・実行失敗","GPU機能未対応",
        "メモリ上限","GPU開始不可","GPU処理枠を取得できず（詳細未記録）","転送失敗","GPU実行失敗",
        "GPU結果取得失敗","保護情報不一致","中断","理由未判定",
        "保存済み拒否（旧記録・原因不明）","保存済みARGB不一致","保存済み信頼度不一致","保存済み保護情報不一致",
        "拒否記録数上限","今回のARGB不一致","今回の信頼度不一致","GPU保護情報不一致",
        "GPU投入失敗","GPU結果形式不正","GPU内部失敗（詳細未取得）",
        "今回のGPU検証枠上限","同じ条件のGPU画質確認中","初回検証用GPU処理枠不足","同じ条件は今回すでに検証",
        "採用可能なGPU候補なし（詳細未記録）","完了時間短縮のためCPUへ配分",
        "候補の認定未成立","候補の画質不一致記録","候補の速度条件未達",
        "候補の認定未成立・画質不一致記録","候補の認定未成立・速度条件未達",
        "候補の画質不一致記録・速度条件未達","候補の認定未成立・画質不一致記録・速度条件未達",
        "GPU候補の利用条件未成立","GPU処理枠の利用不可","枠取得時に画質不一致記録を確認",
        "枠取得時に有効な認定なし","枠取得時に速度条件未達","GPU枠の残り時間予算不足",
        "GPU枠の待機者上限","GPU枠の解放予測なし","GPU枠の解放予測が残り予算を超過"};
    private static final String[] BACKEND_LABELS_1982={"ノイズ前段","モデル準備","補正"};
    private static final String[] RESIDENT_LABELS_1982={"検討中","画質・速度認定済みを採用",
        "保存後の全画素検証へ登録","この入力・GPU機能・処理条件には未対応","画質不一致記録により不採用",
        "保持上限または使用可能メモリ不足","検証キュー・再試行条件未成立","前提経路または速度条件未成立",
        "GPUセッション使用中","保存後の検証実行中","今回の検証画像コピー枠により延期",
        "撮影世代の変更または中断","検証画像コピー失敗","GPU連結の実行失敗","GPU結果未成立","終了理由未記録"};
    // Stable scalar QueueDecision1983 codes; no private configuration key or
    // environment fingerprint is retained in the photo's public diagnostics.
    private static final String[] QUEUE_LABELS_1983={"検証予約の事前条件成立","保存後の検証を予約",
        "検証条件を識別できず","検証画像の保持量が不正","この候補だけで検証保持上限96 MiBを超過",
        "保存後の検証内からの再予約を停止","処理スレッドの中断","端末・GPU環境の認定情報を取得できず",
        "同じ条件の画質不一致記録あり","画質不一致以外の失敗後の再試行間隔待ち",
        "画質不一致以外の失敗の再試行上限","同じ条件の認定が先に成立",
        "同じ条件の検証を実行中","同じ条件の検証を予約済み","強ノイズ検証用の予約枠を確保（この候補は予約見送り）",
        "検証ジョブ数の上限8件","検証画像の合計保持上限96 MiBまでの空き不足",
        "検証ジョブ数8件と合計保持96 MiBの両方が不足","検証処理が未生成",
        "検証予約処理を開始できず","検証予約処理のメモリ確保失敗","検証画像コピーの開始が未成立"};
    private static final String[] COPY_LABELS_1985={"コピー用の枠を予約","コピー要求が不正","このコピーの容量上限を超過",
        "処理スレッドの中断","今回の全画像コピー枠は使用済み","別工程の全画像コピーを優先",
        "今回の区間コピー容量の上限","別工程の区間コピーを優先","別の検証画像をコピー開始済み","別の検証画像がコピー開始前の予約中"};
    private static final String[] FORECAST_LABELS1986={"直近観測なし","観測が2撮影未満","保持した実測を使用",
        "短い冷却中・実測は保持","冷却終了後も実測を保持","観測期限切れ・認定値で再測定"};
    private static final String[] STRONG_TRIALS_1971={"argb_mismatch","confidence_mismatch","policy_failure",
        "submit_failed","readback_failed","output_shape_failed","upload_failed","execution_failed","native_failure_unknown"};
    private static final String[] STRONG_TRIAL_LABELS_1971={"ARGB不一致","信頼度不一致","保護情報不一致",
        "投入失敗","結果取得失敗","結果形式不正","転送失敗","実行失敗","内部失敗（詳細未取得）"};

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
        long strongGpuStrips, strongCpuStrips;
        long strongCpuVerifications;
        final LinkedHashMap<String,Long> strongGpuReasons=new LinkedHashMap<String,Long>();
        final LinkedHashMap<String,Long> strongGpuTrials=new LinkedHashMap<String,Long>();
        String strongGpuFirstFailure="";
        long strongCpuOracleNanos,strongGpuRequestNanos,strongWaitNanos,strongOracleReuses;
        boolean strongCpuOracleMeasured,strongGpuRequestMeasured,strongWaitMeasured;
        long strongLockWaitNanos1982;
        boolean strongLockWaitMeasured1982;
        final long[] backendCounts1982=new long[6],backendNanos1982=new long[6];
        final boolean[] backendTimed1982=new boolean[6],backendUntimed1982=new boolean[6];
        boolean strongMismatchMeasured;
        long strongArgbMismatchCount= -1,strongConfidenceMismatchCount= -1,strongConfidenceMaxDelta= -1;
        int strongArgbMaxDelta= -1;
        String gpuQualification="";
        long noiseFirstNanos1976,noiseModelNanos1976,noiseStrongNanos1976;
        boolean noiseFirstMeasured1976,noiseModelMeasured1976,noiseStrongMeasured1976;
        int correctionRoute1976= -1;
        int strongRoute1978= -1;
        int residentReason1978= -1;
        int residentSourceWidth1978,residentSourceHeight1978,residentOutputWidth1978,residentOutputHeight1978;
        int residentRotation1978,residentHalfLength1978;
        boolean residentIdentity1978;
        long residentRetainedBytes1978;
        int residentQueueReason1983= -1,residentQueueRetries1983;
        int residentQueuedJobs1983= -1,residentRunningJobs1983= -1;
        long residentRetryRemainingNanos1983,residentQueueRetainedBytes1983= -1,residentQueueRequestedBytes1983= -1;
        int residentCopyReason1985= -1,residentCopyPreferred1985= -1,residentCopyActive1985= -1;
        boolean residentCopyStarted1985;long residentCopyEpoch1985= -1;
        int strongForecastState1986= -1,strongForecastSamples1986;
        long strongForecastAge1986= -1,strongForecastCertified1986,strongForecastObserved1986,strongForecastChosen1986;
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
        try { PerformanceHints1952.init(context); } catch(Throwable optional) { }
        try { WholeRoute1953.initialize(context); } catch(Throwable optional) { }
        try { GpuQualification1961.initialize(context); } catch(Throwable optional) { }
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
            WholeRoute1953.foregroundStarted();
            // Stop optional saved-photo probes as soon as this capture begins.
            // associate() still advances the shot epoch and rejects late commits.
            GpuQualification1961.captureChanged();
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
                if(trace.shotId!=shotId){GpuQualification1961.captureChanged();captureEpoch1953++;}
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
    /** Actual Strong NR13 strip commits and CPU selections, attached to the
     * stage owner's trace instead of the worker's unrelated thread scope. */
    public static void noiseBackend(Trace trace,int gpu,int cpu) {
        if(trace==null||gpu<0||cpu<0)return;
        try {
            synchronized(trace) {
                if(trace.state!=0)return;
                trace.strongGpuStrips+=gpu;trace.strongCpuStrips+=cpu;
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** Bounded scalar reason counts for this photo's selected Strong CPU routes.
     * Only the fixed code list is retained; no key, image or arbitrary text is stored. */
    public static void strongGpuReason1970(Trace trace,String reason,int count) {
        if(trace==null||reason==null||count<=0)return;
        int index=-1;for(int i=0;i<STRONG_REASONS_1970.length;i++)if(STRONG_REASONS_1970[i].equals(reason)){index=i;break;}
        if(index<0)return;
        try {
            synchronized(trace) {
                if(trace.state!=0)return;
                String code=STRONG_REASONS_1970[index];Long old=trace.strongGpuReasons.get(code);
                trace.strongGpuReasons.put(code,Long.valueOf((old==null?0L:old.longValue())+count));
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** Successful CPU reference runs used to check a GPU candidate. These are
     * separate from the strips whose final selected output came from the CPU. */
    public static void strongGpuVerification1970(Trace trace,int count) {
        if(trace==null||count<=0)return;
        try {
            synchronized(trace) {
                if(trace.state!=0)return;
                trace.strongCpuVerifications+=count;
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** GPU candidate failures count attempts, separately from selected CPU strips. */
    public static void strongGpuTrial1971(Trace trace,String kind,int count) {
        if(trace==null||kind==null||count<=0)return;
        int index=-1;for(int i=0;i<STRONG_TRIALS_1971.length;i++)if(STRONG_TRIALS_1971[i].equals(kind)){index=i;break;}
        if(index<0)return;
        try {
            synchronized(trace) {
                if(trace.state!=0)return;
                String code=STRONG_TRIALS_1971[index];Long old=trace.strongGpuTrials.get(code);
                trace.strongGpuTrials.put(code,Long.valueOf((old==null?0L:old.longValue())+count));
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** First failed attempt's bounded structural scalars, never photograph values.
     * -1 identifies unavailable program/layout/coordinates/difference. Delta is
     * the absolute difference at that observation, not a whole-image maximum. */
    public static void strongGpuFailure1971(Trace trace,String kind,int program,int layout,
            int width,int coreRows,int x,int y,long absoluteDelta) {
        if(trace==null||kind==null||program< -1||program>63||layout< -1||layout>2||
                width<0||width>65535||coreRows<0||coreRows>65535||absoluteDelta< -1||absoluteDelta>4294967295L)return;
        if(!((x==-1&&y==-1)||(width>0&&coreRows>0&&x>=0&&x<width&&y>=0&&y<coreRows)))return;
        int index=-1;for(int i=0;i<STRONG_TRIALS_1971.length;i++)if(STRONG_TRIALS_1971[i].equals(kind)){index=i;break;}
        if(index<0)return;
        try {
            synchronized(trace) {
                if(trace.state!=0||trace.strongGpuFirstFailure.length()>0)return;
                trace.strongGpuFirstFailure=STRONG_TRIAL_LABELS_1971[index]+" / program="+program+" / layout="+layout+
                    " / 幅="+width+" / 区間行数="+coreRows+" / 位置="+x+","+y+" / 観測箇所の絶対差="+absoluteDelta;
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** Work durations are cumulative worker time, not the stage's interval union.
     * -1 leaves a duration unknown. A reused CPU reference avoids another run. */
    public static void strongGpuWork1973(Trace trace,long cpuOracleNanos,long gpuRequestReadbackNanos,
            long waitNanos,int cachedOracleUses) {
        if(trace==null||cpuOracleNanos< -1||gpuRequestReadbackNanos< -1||waitNanos< -1||cachedOracleUses<0)return;
        if(cpuOracleNanos<0&&gpuRequestReadbackNanos<0&&waitNanos<0&&cachedOracleUses==0)return;
        try {
            synchronized(trace) {
                if(trace.state!=0)return;
                if(cpuOracleNanos>=0){trace.strongCpuOracleMeasured=true;trace.strongCpuOracleNanos=addWork1973(trace.strongCpuOracleNanos,cpuOracleNanos);}
                if(gpuRequestReadbackNanos>=0){trace.strongGpuRequestMeasured=true;trace.strongGpuRequestNanos=addWork1973(trace.strongGpuRequestNanos,gpuRequestReadbackNanos);}
                if(waitNanos>=0){trace.strongWaitMeasured=true;trace.strongWaitNanos=addWork1973(trace.strongWaitNanos,waitNanos);}
                trace.strongOracleReuses=addWork1973(trace.strongOracleReuses,cachedOracleUses);
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** Separately observe the stage-monitor wait; the historical bank timer
     * starts after acquiring this monitor and keeps its original meaning. */
    public static void strongGpuLockWait1982(Trace trace,long nanos) {
        if(trace==null||nanos<0)return;
        try {synchronized(trace){
            if(trace.state!=0)return;
            trace.strongLockWaitMeasured1982=true;
            trace.strongLockWaitNanos1982=addWork1973(trace.strongLockWaitNanos1982,nanos);
        }}catch(Throwable ignored){}
    }
    /** Accepted work only. Callers pass the foreground owner's trace and never
     * report idle probes, CPU reference comparisons or failed GPU candidates.
     * part: 0 initial NR, 1 Strong model, 2 correction. backend: 0 CPU, 1 GPU.
     * Durations include the selected path's preparation and transfers, not
     * isolated hardware execution. -1 marks an observed but untimed selection.
     * Fixed scalar storage and no per-operation UI refresh keep this optional. */
    public static void backend1982(Trace trace,int part,int backend,int units,long nanos) {
        if(trace==null||part<0||part>=3||backend<0||backend>=2||units<=0||nanos< -1)return;
        try {synchronized(trace){
            if(trace.state!=0)return;
            int index=part*2+backend;
            trace.backendCounts1982[index]=addWork1973(trace.backendCounts1982[index],units);
            if(nanos<0)trace.backendUntimed1982[index]=true;
            else {
                trace.backendTimed1982[index]=true;
                trace.backendNanos1982[index]=addWork1973(trace.backendNanos1982[index],nanos);
            }
        }}catch(Throwable ignored){}
    }
    private static long addWork1973(long previous,long delta){return previous>Long.MAX_VALUE-delta?Long.MAX_VALUE:previous+delta;}
    /** Main noise-stage wall durations, separately from summed GPU worker work.
     * Parts are 0: initial NR, 1: Strong model, 2: Strong strip stage. No image
     * reference is retained, and a completed photo cannot be changed later. */
    public static void detailWork1976(Trace trace,int part,long elapsedNanos) {
        if(trace==null||part<0||part>2||elapsedNanos<0)return;
        try {
            synchronized(trace) {
                if(trace.state!=0)return;
                if(part==0){trace.noiseFirstMeasured1976=true;trace.noiseFirstNanos1976=addWork1973(trace.noiseFirstNanos1976,elapsedNanos);}
                else if(part==1){trace.noiseModelMeasured1976=true;trace.noiseModelNanos1976=addWork1973(trace.noiseModelNanos1976,elapsedNanos);}
                else {trace.noiseStrongMeasured1976=true;trace.noiseStrongNanos1976=addWork1973(trace.noiseStrongNanos1976,elapsedNanos);}
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** Record the route which actually committed the final correction result.
     * A failed or merely benchmarked candidate must not call this method. */
    public static void detailRoute1976(Trace trace,int route) {
        if(trace==null||route<0||route>4)return;
        try {
            synchronized(trace) {if(trace.state!=0)return;trace.correctionRoute1976=route;}
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** The ordinary path can still contain individually accepted GPU work.
     * A completed chain's more specific route is never overwritten. */
    public static void detailDefaultRoute1982(Trace trace) {
        if(trace==null)return;
        try {synchronized(trace){if(trace.state==0&&trace.correctionRoute1976<0)trace.correctionRoute1976=0;}}
        catch(Throwable ignored){}
    }
    /** Observed foreground routing only. A real CPU allocation wins over later
     * pending-proof events; idle benchmarks have no photo trace. */
    public static void strongRoute1978(Trace trace,int mode) {
        if(trace==null||mode<0||mode>2)return;
        try {
            synchronized(trace) {
                if(trace.state!=0)return;
                if(mode==1||trace.strongRoute1978<0||(trace.strongRoute1978==0&&mode==2))
                    trace.strongRoute1978=mode;
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** Scalar resident admission diagnostics, with no image or mask retention. */
    public static void resident1978(Trace trace,int sourceWidth,int sourceHeight,
            int outputWidth,int outputHeight,int rotation,boolean identity,
            int halfLength,long retainedBytes,int reason) {
        if(trace==null||sourceWidth<=0||sourceHeight<=0||outputWidth<=0||outputHeight<=0
                ||rotation<0||rotation>=360||halfLength<0||retainedBytes<0||reason<0||reason>=RESIDENT_LABELS_1982.length)return;
        try {
            synchronized(trace) {
                if(trace.state!=0)return;
                trace.residentSourceWidth1978=sourceWidth;trace.residentSourceHeight1978=sourceHeight;
                trace.residentOutputWidth1978=outputWidth;trace.residentOutputHeight1978=outputHeight;
                trace.residentRotation1978=rotation;trace.residentIdentity1978=identity;
                trace.residentHalfLength1978=halfLength;trace.residentRetainedBytes1978=retainedBytes;
                trace.residentReason1978=reason;
                trace.residentQueueReason1983= -1;
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** Final admission outcome also works when invalid input prevented geometry
     * observation. It retains no input object and never changes route decisions. */
    public static void residentExit1982(Trace trace,long retainedBytes,int reason) {
        residentExit1983(trace,retainedBytes,reason,-1,0,0,-1,-1,-1,-1);
    }
    /** Final route and its existing queue decision are copied together. No live
     * scheduler query occurs here or while rendering a saved photograph. */
    public static void residentExit1983(Trace trace,long retainedBytes,int reason,int queueReason,
            long retryRemainingNanos,int retries,int queuedJobs,int runningJobs,long queueRetained,long queueRequested) {
        if(trace==null||retainedBytes<0||reason<1||reason>=RESIDENT_LABELS_1982.length||
                queueReason< -1||queueReason>=QUEUE_LABELS_1983.length||retryRemainingNanos<0||retries<0||
                queuedJobs< -1||queuedJobs>8||runningJobs< -1||runningJobs>1||queueRetained< -1||queueRequested< -1)return;
        try {
            synchronized(trace){
                if(trace.state!=0)return;
                trace.residentRetainedBytes1978=retainedBytes;trace.residentReason1978=reason;
                trace.residentQueueReason1983=queueReason;trace.residentRetryRemainingNanos1983=retryRemainingNanos;
                trace.residentQueueRetries1983=retries;trace.residentQueuedJobs1983=queuedJobs;trace.residentRunningJobs1983=runningJobs;
                trace.residentQueueRetainedBytes1983=queueRetained;trace.residentQueueRequestedBytes1983=queueRequested;
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    /** Scalars from the very same copy admission. Saved photographs never
     * query a later owner or turn a delayed proof into adopted image output. */
    public static void residentCopy1985(Trace trace,int reason,int preferredFamily,int activeFamily,
            boolean activeStarted,long copyEpoch){
        if(trace==null||reason<0||reason>=COPY_LABELS_1985.length||!copyFamily1985(preferredFamily)||
                !copyFamily1985(activeFamily)||copyEpoch<0)return;
        try{
            synchronized(trace){
                if(trace.state!=0)return;
                trace.residentCopyReason1985=reason;trace.residentCopyPreferred1985=preferredFamily;
                trace.residentCopyActive1985=activeFamily;trace.residentCopyStarted1985=activeStarted;
                trace.residentCopyEpoch1985=copyEpoch;
            }
            publish(trace,false);
        }catch(Throwable optional){}
    }
    private static boolean copyFamily1985(int family){return family== -1||family==0||family>0&&family<=32&&(family&(family-1))==0;}
    private static String copyFamilyLabel1985(int family){
        switch(family){case 0:return "区間検証";case 1:return "Strongから連結";case 2:return "仕上げ";
            case 4:return "モアレ・形状";case 8:return "Strong候補";case 16:return "単独工程";
            case 32:return "残差工程";default:return "なし";}
    }
    private static void appendResidentCopy1985(StringBuilder text,Trace trace){
        if(trace.residentCopyReason1985<0)return;
        text.append("\n連結検証のコピー枠判定: ").append(COPY_LABELS_1985[trace.residentCopyReason1985]);
        text.append(" / 判定時の優先対象: ").append(trace.residentCopyPreferred1985==0?"未選択":copyFamilyLabel1985(trace.residentCopyPreferred1985));
        text.append(" / 保持中: ").append(copyFamilyLabel1985(trace.residentCopyActive1985));
        if(trace.residentCopyActive1985>=0)text.append(trace.residentCopyStarted1985?"（コピー開始済み）":"（コピー開始前）");
        text.append(" / 撮影世代 ").append(trace.residentCopyEpoch1985);
    }
    /** Capture the scalars used by this same foreground forecast lookup.
     * This hot observer allocates no strings/arrays and performs no redraw,
     * preference, logger or live cache query. Rendering happens at the usual
     * photograph publication points and completed photographs stay immutable. */
    public static void strongForecast1986(Trace trace,int state,int samples,long ageNanos,
            long certifiedNanos,long observedNanos,long chosenNanos){
        if(trace==null||state<0||state>=FORECAST_LABELS1986.length||samples<0||samples>3||ageNanos< -1||
                certifiedNanos<=0||observedNanos<0||chosenNanos<=0)return;
        try{synchronized(trace){
            if(trace.state!=0)return;
            trace.strongForecastState1986=state;trace.strongForecastSamples1986=samples;trace.strongForecastAge1986=ageNanos;
            trace.strongForecastCertified1986=certifiedNanos;trace.strongForecastObserved1986=observedNanos;trace.strongForecastChosen1986=chosenNanos;
        }}catch(Throwable optional){}
    }
    private static void appendStrongForecast1986(StringBuilder text,Trace trace){
        if(trace.strongForecastState1986<0)return;
        text.append("\nGPU所要時間の直近予測: ").append(FORECAST_LABELS1986[trace.strongForecastState1986])
            .append(" / 有効観測 ").append(trace.strongForecastSamples1986).append("撮影");
        if(trace.strongForecastAge1986>=0)text.append(" / 最も古い有効観測 ").append(trace.strongForecastAge1986/1000000L).append("ms前");
        text.append(" / 認定値 ").append(milliseconds(trace.strongForecastCertified1986)).append("ms");
        text.append(" / 保持実測の最大値 ");
        if(trace.strongForecastObserved1986>0)text.append(milliseconds(trace.strongForecastObserved1986)).append("ms");else text.append("未取得");
        text.append(" / この照会の予測 ").append(milliseconds(trace.strongForecastChosen1986)).append("ms");
        text.append("\n※撮影間の実測は2撮影から使用し、この写真内の観測も下限に含めたGPU区間予測です。現在の待ち時間や写真全体の実測時間ではありません。");
    }
    /** Full comparison scalars from the same first failed comparison candidate.
     * ARGB and logical confidence are compared independently. These are not
     * maxima over other layouts/trials; no pixel values or arrays are retained. */
    public static void strongGpuMismatch1973(Trace trace,long argbMismatchCount,int argbMaxDelta,
            long confidenceMismatchCount,long confidenceMaxDelta) {
        if(trace==null||!mismatchPair1973(argbMismatchCount,argbMaxDelta,255L)||
                !mismatchPair1973(confidenceMismatchCount,confidenceMaxDelta,4294967295L))return;
        if(argbMismatchCount<0&&confidenceMismatchCount<0)return;
        try {
            synchronized(trace) {
                if(trace.state!=0||trace.strongMismatchMeasured)return;
                trace.strongMismatchMeasured=true;
                trace.strongArgbMismatchCount=argbMismatchCount;trace.strongArgbMaxDelta=argbMaxDelta;
                trace.strongConfidenceMismatchCount=confidenceMismatchCount;trace.strongConfidenceMaxDelta=confidenceMaxDelta;
            }
            publish(trace,false);
        }catch(Throwable ignored){}
    }
    private static boolean mismatchPair1973(long count,long maximum,long limit) {
        return count== -1&&maximum== -1||count>=0&&count<=Integer.MAX_VALUE&&maximum>=0&&maximum<=limit&&
            (count==0?maximum==0:maximum>0);
    }
    public static void finish(Trace trace,boolean success) {
        if(trace==null)return;
        try {
            synchronized(trace) {
                if(trace.state!=0)return;
                trace.gpuQualification=shortText(GpuQualification1961.status1967().replace("（現在）",""),640);
                trace.finished=System.nanoTime();trace.state=success?1:2;
                for(Stage stage:trace.stages)if(stage.active>0)stage.incomplete=true;
            }
            publish(trace,true);
            WholeRoute1953.saved(trace,success);
            // Another capture may have blocked a previously saved probe. Its
            // terminal trace must wake idle admission even when identities differ.
            WholeRoute1953.wake();
            GpuQualification1961.wake();
        }catch(Throwable ignored){}
    }

    /** H21 observes completion independently from metadata validity. */
    public static boolean finished1953(Trace trace) {
        if(trace==null)return false;
        synchronized(trace){return trace.state!=0;}
    }

    /** Capture/beauty work is outside the shared CPU pool. Exclude only this
     * exact photograph, whose capture necessarily precedes its CPU baseline. */
    public static boolean otherCapturesIdle1953(Trace owner) {
        synchronized(LOCK) {
            prune(System.nanoTime());
            for(Trace trace:TRACES)if(trace!=owner&&trace.shotId>0)
                synchronized(trace){if(trace.state==0)return false;}
            return true;
        }
    }
    /** Starts that finish during a baseline still make that timing unreliable. */
    public static long captureEpoch1953() {return captureEpoch1953;}

    /** Legacy PhotoDetail.status is intentionally not imported into this independent record. */
    public static void legacyStatus(String ignored) {refreshViews(true);}
    public static String summary() {
        try {
            synchronized(LOCK) {
                if(latest!=null)return render(latest);
                if(persisted!=null)return persisted;
            }
        }catch(Throwable ignored){}
        return "ULike v"+VERSION+" / 新しい撮影の計測待ち\n強ノイズ処理: 計測待ち（新しい撮影後に判定）\n強ノイズGPU方針: 画質・速度条件を満たす候補を採用。追加認定は保存後\n合成: 未計測 / ノイズ除去: 未計測 / 補正: 未計測 / 圧縮: 未計測 / 保存: 未計測";
    }

    /** Live queue state belongs only to this opened view, never to the saved
     * photograph's immutable counters, elapsed time, or persisted summary. */
    public static String liveSummary1979() {
        String saved=summary();
        try {
            String live=saved+"\n\n表示時点の"+GpuQualification1961.status1967()
                +"\n検証キューと条件別の記録は、この表示を開き直すと更新されます。写真のGPU区間数と処理時間は撮影時の記録です。";
            // The attempt ledger describes later private verification work.
            // Never append it to the persisted photograph or count a successful
            // certificate as output adopted by that photograph.
            try {
                String attempts=GpuQualification1961.attemptSummary1986();
                if(attempts!=null&&attempts.length()>0)live+="\n\n"+shortText(attempts,6000);
            }catch(Throwable optional){}
            return live;
        }catch(Throwable optional){return saved;}
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
            if(trace.strongGpuStrips+trace.strongCpuStrips>0) {
                text.append("\n強ノイズ処理: GPU ").append(trace.strongGpuStrips)
                    .append("区間 / CPU ").append(trace.strongCpuStrips).append("区間")
                    .append(trace.strongGpuStrips>0?"（GPU使用あり）":"（今回はGPU出力の採用なし）");
            }
            else text.append("\n強ノイズ処理: 未観測（GPU使用は未判定）");
            text.append("\n強ノイズGPU方針: 画質・速度条件を満たす候補を採用。追加認定は保存後");
            if(trace.strongRoute1978>=0) {
                text.append("\n今回の強ノイズ配分: ");
                if(trace.strongRoute1978==1)text.append("実並列速度を確認済み・CPU併用");
                else if(trace.strongRoute1978==2)text.append("採用条件が成立しない区間はCPU（理由は下記）");
                else text.append("GPU優先・CPU併用の速度は未認定");
            }
            text.append("\n強ノイズGPU確認: CPU照合 ").append(trace.strongCpuVerifications).append("回");
            if(trace.strongCpuOracleMeasured||trace.strongGpuRequestMeasured||trace.strongWaitMeasured||trace.strongOracleReuses>0||trace.strongLockWaitMeasured1982) {
                text.append("\n強ノイズCPU経路・照合作業累計: ").append(trace.strongCpuOracleMeasured?milliseconds(trace.strongCpuOracleNanos)+" ms":"未計測");
                text.append("\nGPU準備・転送・実行・読戻し作業累計: ").append(trace.strongGpuRequestMeasured?milliseconds(trace.strongGpuRequestNanos)+" ms":"未計測");
                text.append("\nGPU枠内の待機・判定作業累計: ").append(trace.strongWaitMeasured?milliseconds(trace.strongWaitNanos)+" ms":"未計測");
                text.append("\nGPU枠受付ロック取得待ち累計: ").append(trace.strongLockWaitMeasured1982?milliseconds(trace.strongLockWaitNanos1982)+" ms":"未計測");
                if(trace.strongOracleReuses>0)text.append("\n同じ区間のCPU参照結果を追加照合に再利用: ").append(trace.strongOracleReuses).append("回");
            }
            if(!trace.strongGpuReasons.isEmpty()) {
                text.append("\n強ノイズCPU退避理由: ");boolean separator=false;
                for(int i=0;i<STRONG_REASONS_1970.length;i++) {
                    Long count=trace.strongGpuReasons.get(STRONG_REASONS_1970[i]);if(count==null||count.longValue()<=0)continue;
                    if(separator)text.append(" / ");separator=true;
                    text.append(STRONG_REASON_LABELS_1970[i]).append(' ').append(count.longValue()).append("区間");
                }
            }
            if(!trace.strongGpuTrials.isEmpty()) {
                text.append("\n強ノイズGPU候補失敗: ");boolean separator=false;
                for(int i=0;i<STRONG_TRIALS_1971.length;i++) {
                    Long count=trace.strongGpuTrials.get(STRONG_TRIALS_1971[i]);if(count==null||count.longValue()<=0)continue;
                    if(separator)text.append(" / ");separator=true;
                    text.append(STRONG_TRIAL_LABELS_1971[i]).append(' ').append(count.longValue()).append("試行");
                }
            }
            if(trace.strongGpuFirstFailure.length()>0)text.append("\n最初のGPU候補失敗: ").append(trace.strongGpuFirstFailure);
            if(trace.strongMismatchMeasured) {
                text.append("\n最初の失敗候補（全比較領域）: ");
                if(trace.strongArgbMismatchCount<0)text.append("ARGB未取得");
                else text.append("ARGB不一致 ").append(trace.strongArgbMismatchCount).append("画素 / 最大チャンネル差 ").append(trace.strongArgbMaxDelta);
                text.append("\n同じ候補の信頼度: ");
                if(trace.strongConfidenceMismatchCount<0)text.append("未取得");
                else text.append("不一致 ").append(trace.strongConfidenceMismatchCount).append("点 / 最大差 ").append(trace.strongConfidenceMaxDelta);
            }
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
            if(trace.noiseFirstMeasured1976||trace.noiseModelMeasured1976||trace.noiseStrongMeasured1976) {
                text.append("\nノイズ主工程の内訳: 前段 ").append(trace.noiseFirstMeasured1976?milliseconds(trace.noiseFirstNanos1976)+" ms":"未計測")
                    .append(" / モデル準備 ").append(trace.noiseModelMeasured1976?milliseconds(trace.noiseModelNanos1976)+" ms":"未計測")
                    .append(" / 強ノイズ ").append(trace.noiseStrongMeasured1976?milliseconds(trace.noiseStrongNanos1976)+" ms":"未計測");
            }
            for(int part=0;part<BACKEND_LABELS_1982.length;part++) {
                int cpu=part*2,gpu=cpu+1;
                text.append('\n').append(BACKEND_LABELS_1982[part]).append("の経路記録: ");
                if(trace.backendCounts1982[cpu]==0&&trace.backendCounts1982[gpu]==0)text.append("未観測（CPU/GPU未判定）");
                else {
                    text.append("GPU採用 ").append(trace.backendCounts1982[gpu]).append("回 / CPU採用 ")
                        .append(trace.backendCounts1982[cpu]).append("回");
                    text.append("\n  記録した作業累計: GPU ").append(backendDuration1982(trace,gpu))
                        .append(" / CPU ").append(backendDuration1982(trace,cpu));
                }
            }
            if(trace.correctionRoute1976>=0) {
                text.append("\n補正経路: ");
                switch(trace.correctionRoute1976) {
                    case 0:text.append("個別処理（CPU/GPU採用は経路記録を参照）");break;
                    case 1:text.append("GPU連結（32行）");break;
                    case 2:text.append("GPU連結（64行）");break;
                    case 3:text.append("GPU連結（128行）");break;
                    case 4:text.append("強ノイズからGPU連結");break;
                }
            } else if(trace.state!=0)text.append("\n補正経路: 採否未記録");
            if(trace.residentReason1978>=0) {
                text.append("\n連結候補の入力/出力: ");
                if(trace.residentSourceWidth1978>0&&trace.residentSourceHeight1978>0&&trace.residentOutputWidth1978>0&&trace.residentOutputHeight1978>0)
                    text.append(trace.residentSourceWidth1978).append('×').append(trace.residentSourceHeight1978)
                        .append(" → ").append(trace.residentOutputWidth1978).append('×').append(trace.residentOutputHeight1978)
                        .append(" / 回転 ").append(trace.residentRotation1978).append("度 / ")
                        .append(trace.residentIdentity1978?"等倍専用":"等倍専用以外");
                else text.append("未計測");
                long bytes=trace.residentRetainedBytes1978;
                text.append("\n連結検証の保持見積: ");
                if(bytes>0)text.append(bytes/1048576L).append('.').append((bytes%1048576L)*10L/1048576L).append(" MiB");
                else text.append("未計算");
                text.append(" / 前段half ").append(trace.residentHalfLength1978).append("画素");
                text.append("\n強ノイズ→補正のGPU連結: ");
                int reason=trace.residentReason1978==0&&trace.state!=0?15:trace.residentReason1978;
                text.append(RESIDENT_LABELS_1982[reason]);
                int queueReason=trace.residentQueueReason1983;
                if(queueReason>=0) {
                    text.append("\n連結検証の判定: ").append(QUEUE_LABELS_1983[queueReason]);
                    if(queueReason==9) {
                        long nanos=trace.residentRetryRemainingNanos1983;
                        text.append("（判定時の残り ").append(nanos/1000000000L+(nanos%1000000000L==0?0:1)).append("秒）");
                    }
                    if(queueReason==9||queueReason==10)
                        text.append(" / 再試行実施済み ").append(trace.residentQueueRetries1983).append("回");
                    if(trace.residentQueuedJobs1983>=0&&trace.residentRunningJobs1983>=0)
                        text.append(" / 予約済み ").append(trace.residentQueuedJobs1983).append("件 / 実行中 ")
                            .append(trace.residentRunningJobs1983).append("件");
                    if(trace.residentQueueRetainedBytes1983>=0&&trace.residentQueueRequestedBytes1983>=0) {
                        text.append(queueReason==1?"\n登録後の検証保持: 使用 ":"\n判定時の検証保持: 使用 ");
                        appendMiB1983(text,trace.residentQueueRetainedBytes1983);
                        text.append(" / 上限96 MiB / この候補 ");appendMiB1983(text,trace.residentQueueRequestedBytes1983);
                    }
                }
            }
            appendResidentCopy1985(text,trace);
            appendStrongForecast1986(text,trace);
            long until=trace.finished==0?System.nanoTime():trace.finished;
            text.append("\n全体経過: ").append(milliseconds(Math.max(0L,until-trace.started))).append(" ms");
            text.append("（並行工程の時間は重複します）");
            text.append("\n標準美顔処理・撮影待機は工程別計測の対象外です。");
            text.append("\nGPU/CPU区間数は強ノイズ処理の最終出力の選択です。GPU出力でも画質確認にCPU照合を使う場合があります。");
            text.append("\n前段・モデル・補正の採用回数は処理単位ごとの記録で、画素比率やGPU使用率ではありません。経路の作業累計は準備・転送を含みます。");
            text.append("\nGPU候補失敗の試行数はCPU区間数とは別です。失敗位置は区間内、絶対差は観測箇所のみです。-1は未取得です。");
            if(trace.strongMismatchMeasured)
                text.append("\n全比較領域の件数と最大差は最初の失敗候補だけの集計で、全候補の最大ではありません。");
            if(trace.strongCpuOracleMeasured||trace.strongGpuRequestMeasured||trace.strongWaitMeasured)
                text.append("\n強ノイズ作業累計は並行ワーカー分とCPU・GPU同時処理分が重複し、工程時間とは一致しません。GPU要求の累計には並行したCPU照合の時間を含む場合があります。GPU最終出力が0区間でも候補のGPU実行時間を含む場合があります。");
            if(trace.gpuQualification.length()>0)
                text.append("\n保存時の").append(trace.gpuQualification);
            if(trace.detail.length()>0)text.append("\n").append(trace.detail);
            return text.toString();
        }
    }
    private static String backendDuration1982(Trace trace,int index) {
        if(trace.backendCounts1982[index]==0)return "記録なし";
        if(!trace.backendTimed1982[index])return "未計測";
        String value=milliseconds(trace.backendNanos1982[index])+" ms";
        return trace.backendUntimed1982[index]?value+"（一部未計測）":value;
    }
    private static void appendMiB1983(StringBuilder text,long bytes) {
        text.append(bytes/1048576L).append('.').append((bytes%1048576L)*10L/1048576L).append(" MiB");
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
