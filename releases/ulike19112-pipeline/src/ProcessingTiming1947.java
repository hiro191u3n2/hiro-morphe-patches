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
    public static final String VERSION = "1.9.112";
    public static final int FUSION=0, NOISE=1, CORRECTION=2, ENCODE=3, SAVE=4;
    public static final int REQUEST1991=0,NATIVE_REQUEST1991=1,IMAGE_RECEIVED1991=2,
        BITMAP_DELIVERED1991=3,PREP_START1991=4,QUEUE_SUBMIT1991=5,WORKER_START1991=6,
        PIPELINE_START1991=7,PIPELINE_END1991=8,ENCODE_START1991=9,ENCODE_END1991=10,
        PUBLISH_START1991=11,PUBLISH_END1991=12;
    public static final int IMAGE_GET_START1992=13,IMAGE_GET_END1992=14;
    public static final int FINISH_CPU_POLICY1992=0,FINISH_GPU_POLICY1992=1,
        FINISH_SOURCE_UPLOAD1992=2,FINISH_METADATA_UPLOAD1992=3,FINISH_PREPARATION1992=4,
        FINISH_GPU_SUBMIT1992=5,FINISH_READBACK1992=6,FINISH_BITMAP_WRITE1992=7,
        FINISH_UPLOAD_SUBMIT1992=8,FINISH_TRANSFER_SUBMIT1992=8,FINISH_CPU_MASK_PACK1992=9,
        FINISH_VALIDATION_COPY1997=10;
    private static final String[] FINISH_LABELS1992={"CPU補正ポリシー生成","GPU補正ポリシー生成API",
        "原画像転送API","補助データ転送API","GPU補正準備・領域確保","GPU投入API",
        "GPU結果取得API（待機・読戻し）","出力Bitmap書込み","補助データ転送・GPU投入一体API",
        "CPU顔・肌マスク記述子作成","保存後の追加検証用コピー（保存公開前の取得）"};
    private static final String[] FRONT_LABELS1996={"Bitmap読取りロック待ち","Bitmap読取り・元ハロー保全",
        "CPU保護マスク生成","native利用可否の確認","native呼出し経路","Java代替経路",
        "CPU作業領域検証の予約判定","次段half生成","Bitmap書込みロック待ち","Bitmap書込み","次段half確定・透過読戻し","GPU前段候補の検証予約・入力準備"};
    private static final String[] FRONT_ROUTE_LABELS1996={"native成功","native false","nativeシンボル失敗",
        "Java開始","native読込み・リンク不可","native読込み権限不可","native ABI不一致","native条件対象外",
        "Java完了","nativeシンボル失敗後の無効化"};
    private static final String[] LABELS={"合成", "ノイズ除去", "補正", "圧縮", "保存"};
    private static final String STORE="hiro_ulike_timing_1947";
    private static final int MAX_TRACES=64, MAX_KEYS=192, MAX_INTERVALS=256;
    private static final long TTL=300000000000L;
    private static final Object LOCK=new Object();
    private static final Object RECENT_LOCK1998=new Object();
    private static final ArrayList<RecentCapture1998> RECENT1998=new ArrayList<RecentCapture1998>();
    private static final ThreadLocal<Trace> CURRENT=new ThreadLocal<Trace>();
    private static final ArrayList<Trace> TRACES=new ArrayList<Trace>();
    private static final ArrayList<Key> KEYS=new ArrayList<Key>();
    private static final ArrayList<ViewBinding> VIEWS=new ArrayList<ViewBinding>();
    private static long nextId=Math.max(1L, System.currentTimeMillis()), displayedId;
    private static volatile long captureEpoch1953;
    private static long captureOrdinal1990;
    private static volatile CaptureIdentity1989 captureIdentity1989;
    private static final class CaptureIdentity1989 {
        final long epoch,traceId;
        CaptureIdentity1989(long epoch,long traceId){this.epoch=epoch;this.traceId=traceId;}
    }
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
        "撮影世代の変更または中断","検証画像コピー失敗","GPU連結の実行失敗","GPU結果未成立","終了理由未記録",
        "標準GPU経路を優先・追加検証を省略"};
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
        "検証予約処理を開始できず","検証予約処理のメモリ確保失敗","検証画像コピーの開始が未成立",
        "CPU仕上げキャッシュ検証は同時1件まで","同じ入力のモデルGPU検証を先に実施","GPUセッション使用中"};
    private static final String[] COPY_LABELS_1985={"コピー用の枠を予約","コピー要求が不正","このコピーの容量上限を超過",
        "処理スレッドの中断","今回の全画像コピー枠は使用済み","別工程の全画像コピーを優先",
        "今回の区間コピー容量の上限","別工程の区間コピーを優先","別の検証画像をコピー開始済み","別の検証画像がコピー開始前の予約中"};
    private static final String[] FORECAST_LABELS1986={"直近観測なし","観測が2撮影未満","保持した実測を使用",
        "短い冷却中・実測は保持","冷却終了後も実測を保持","観測期限切れ・認定値で再測定"};
    // .88 fixed IDs mirror PipelineDetail1988 without adding a provider to rendering.
    private static final int PIPELINE_PHASES1988=11, PIPELINE_REASONS1988=22, PIPELINE_COPIES1988=11;
    private static final String[] PIPELINE_LABELS1988={"前段・事前解析","前段・画素処理","モデル・元画像と縮小入力準備",
        "モデル・領域推定","モデル・多段マップ","補正・モアレ","補正・回転とリサイズ","補正・くっきり",
        "補助・空間ノイズ解析","補助・保護情報準備","補正・モアレとくっきり一括"};
    private static final String[] PIPELINE_REASON_LABELS1988={"理由未観測","CPU固定処理","有効なGPU認定なし",
        "検証の許可条件未成立（詳細未観測）","GPUセッション使用中","背景検証内",
        "GPU利用条件未成立（詳細未観測）","使用可能メモリ不足","GPU候補結果なし","GPU候補実行の例外",
        "別の検証画像を保持中","専用処理または既存の省略経路","候補画質不一致の記録",
        "CPU比較経路がGPUを見送り","経路枠が使用中","共用GPU枠を取得できず",
        "GPU環境識別なし","処理幅のGPU認定なし","入力条件対象外","処理中断を観測","単写GPUシェーダー未対応（残差候補へ切替）","残差GPUシェーダー未対応"};
    private static final String[] PIPELINE_COPY_LABELS1988={"未観測","予約成立","取得完了","検証登録",
        "検証登録見送り","取得見送り","中断","確保失敗","同じ取得済み入力を再利用","取得未完了","不要な補助準備を省略"};
    private static final String[] STRONG_TRIALS_1971={"argb_mismatch","confidence_mismatch","policy_failure",
        "submit_failed","readback_failed","output_shape_failed","upload_failed","execution_failed","native_failure_unknown"};
    private static final String[] STRONG_TRIAL_LABELS_1971={"ARGB不一致","信頼度不一致","保護情報不一致",
        "投入失敗","結果取得失敗","結果形式不正","転送失敗","実行失敗","内部失敗（詳細未取得）"};

    private ProcessingTiming1947() { }

    public static final class Trace {
        public final long id;
        public final long timestampMillis;
        // Fixed with this photograph, never replaced by the process opening it.
        public final String processId19101;
        final long started;
        final Stage[] stages={new Stage(),new Stage(),new Stage(),new Stage(),new Stage()};
        long shotId, finished;
        long shutterDownNanos1994;
        // Same exact input; values are all System.nanoTime observations. No sensor clock subtraction.
        final long[] cameraHandoff19103=new long[3]; // acquire entry/end, ordinary result observation
        boolean cameraHandoffObserved19103;
        final long[] cameraPhases19104=new long[3]; // existing ownership guard entry/exit, Q0 entry
        boolean cameraPhasesObserved19104;
        boolean captureMetadataObserved1995;
        // Scalar provenance from each exact handoff. Actual Bitmap dimensions
        // stay independent of Camera2 metadata, including when it is unavailable.
        final int[] bitmapDimensions1999=new int[10];
        final String[] shaderFailures19100=new String[2];
        int bitmapConflictMask1999,metadataReason1999;
        long saveTicket1999;
        int saveBinding1999,saveOwnership1999; // 0 unobserved; binding 1 exact, 2 absent, 3 completed
        boolean metadataRejected1999;
        // Optional frontend proof collection: bounded decisions only. A copied
        // strip and a completed GPU qualification are different observations.
        final long[] frontProofCounts19100=new long[23],frontCopyCounts19100=new long[10];
        final long[] frontProofLast19100=new long[]{0,0,-1,-1,-1,-1};
        int captureIso1995,captureLens1995,captureWidth1995,captureHeight1995;
        long captureExposureNanos1995;
        long captureProbeId1995;
        String captureProbeKind1995="",captureProbePhase1995="",startupWideStatus1995="";
        boolean captureProbeObserved1995,captureProbeCancelled1995;
        long strongDirectStrips1995,strongDirectPixels1995;
        int finishSelectionCount1995,finishFirstReason1995,finishFirstRows1995,finishLastReason1995,finishLastRows1995;
        long captureOrdinal1990, publishedNanos1990;
        boolean publicationObserved1990, publicationSucceeded1990;
        // Exact-owner monotonic events, never a latest-camera or UI-flash guess.
        final long[] events1991=new long[15];
        int requestOrigin1991; // 0 unavailable, 1 recorder request, 2 native request
        // Per-category tuple: calls, cumulative API elapsed, logical bytes, flags.
        // No image, callback, bitmap or GPU provider is retained by diagnostics.
        final long[] finishWork1992=new long[11*4];
        // Last foreground admission refusal and its one bounded retry. Values
        // belong to this photograph, never to a previous background probe.
        final long[] finishMemory1997=new long[33];
        int finishMemoryCount1997;
        int finishValidationSkipped1997;
        int startupState1990=-1,startupPrepared1990,startupFailed1990;
        long startupBegan1990,startupElapsed1990;
        boolean startupReady1990;
        boolean startupWaitObserved1990,startupWaitReady1990;
        long startupWaitNanos1990;
        long updatedMillis;
        int state; // 0 processing, 1 complete, 2 error
        String requested="設定: 未計測", format="", detail="";
        boolean settingsComparable1998,outputComparable1998;
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
        // Wait/submit/collect API elapsed and count, accumulated once per stage.
        final long[] strongBreakdown1994=new long[6];
        boolean strongBreakdownObserved1994;
        final long[] strongProfiles1994=new long[24];
        final long[] modelVariants1994=new long[3];
        final long[] backendCounts1982=new long[6],backendNanos1982=new long[6];
        final boolean[] backendTimed1982=new boolean[6],backendUntimed1982=new boolean[6];
        boolean strongMismatchMeasured;
        long strongArgbMismatchCount= -1,strongConfidenceMismatchCount= -1,strongConfidenceMaxDelta= -1;
        int strongArgbMaxDelta= -1;
        String gpuQualification="";
        long noiseFirstNanos1976,noiseModelNanos1976,noiseStrongNanos1976;
        boolean noiseFirstMeasured1976,noiseModelMeasured1976,noiseStrongMeasured1976;
        int correctionRoute1976= -1;
        int correctionRows1994;
        long geometryBands1994,geometryApiNanos1994;
        int geometryMinRows1994,geometryMaxRows1994;
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
        // Fixed primitive storage is allocated only with its capture owner.
        // Work tuple: count, elapsed nanos, measured/unknown flags.
        final long[] pipelineWork1988=new long[PIPELINE_PHASES1988*3*3];
        final long[] pipelineReasons1988=new long[PIPELINE_PHASES1988*PIPELINE_REASONS1988];
        // Copy tuple: event count, elapsed nanos, bytes, observed/unknown flags.
        final long[] pipelineCopies1988=new long[PIPELINE_PHASES1988*PIPELINE_COPIES1988*4];
        boolean pipelineObserved1988;
        final long[] frontWork1996=new long[24],frontRoutes1996=new long[10];
        long frontObserved1996;
        boolean frontPlanObserved1997,frontHalfObserved1997;
        int frontWorkers1997,frontRows1997,frontHalfReason1997,frontHalfPixels1997,
            frontTrim1997,frontAvailableWorkers1997;
        long frontBudgetBefore1997,frontBudgetAfter1997,frontWorkerBytes1997,frontHalfBytes1997;
        // calls, pending-ticket CPU work, submit API, collect API, submitted, joined.
        final long[] modelOverlap1996=new long[9];
        // .89 reason handoffs and copy quantities are independent of output adoption.
        final long[] pipelinePriorReasons1989=new long[PIPELINE_PHASES1988*PIPELINE_REASONS1988];
        // Detailed copy tuple: events, requested closure bytes, acquired pixel bytes, flags.
        final long[] pipelineCopyQuantities1989=new long[PIPELINE_PHASES1988*PIPELINE_COPIES1988*4];
        final long[] pipelineQueueCounts1989=new long[PIPELINE_PHASES1988*QUEUE_LABELS_1983.length];
        // Last actual decision per phase: count, reason, retry ns, retries, queued, running,
        // retained bytes, requested bytes, acquired pixel bytes, outcome. No live provider.
        final long[] pipelineQueueLast1989=new long[PIPELINE_PHASES1988*10];
        Trace(long value,long wall,long monotonic) {
            id=value; timestampMillis=wall; started=monotonic; updatedMillis=wall;
            processId19101=currentProcessId19101();
            try {
                GpuStartup1990.Snapshot ready=GpuStartup1990.snapshot();
                startupState1990=ready.state;startupPrepared1990=ready.preparedPrograms;startupFailed1990=ready.failedPrograms;
                startupBegan1990=ready.startedNanos;startupElapsed1990=ready.elapsedNanos;
                startupReady1990=ready.domainReady&&ready.readyNanos>0&&ready.readyNanos<=monotonic;
                startupWideStatus1995=shortText(ready.finishWideStatus1995,240);
            }catch(Throwable optional){}
        }
    }
    /** Successful saved captures only. Immutable scalars/strings, never a Trace,
     * image, context or callback. Independent of the expiring live trace index. */
    private static final class RecentCapture1998 {
        final long id, ordinal, elapsed, iso, exposure, started, timestampMillis;
        final long[] times;
        final long[] arrivalTimes19103;
        final String settings, format, routes, provenance1999;
        final long[] bitmaps1999;
        final int bitmapConflicts1999,saveBinding1999;
        final int inputWidth,inputHeight,width,height,lens,origin;
        final boolean comparable;
        RecentCapture1998(Trace t) {
            id=t.id;ordinal=t.captureOrdinal1990;elapsed=t.publishedNanos1990-t.started;started=t.started;timestampMillis=t.timestampMillis;
            settings=t.requested;format=t.format;iso=t.captureIso1995;exposure=t.captureExposureNanos1995;
            inputWidth=t.captureWidth1995;inputHeight=t.captureHeight1995;width=t.width;height=t.height;
            lens=t.captureLens1995;origin=t.requestOrigin1991;
            bitmaps1999=new long[10];for(int i=0;i<10;i++)bitmaps1999[i]=t.bitmapDimensions1999[i];bitmapConflicts1999=t.bitmapConflictMask1999;saveBinding1999=t.saveBinding1999;
            StringBuilder provenance=new StringBuilder(200);appendBitmap1999(provenance,t);provenance1999=provenance.toString();
            comparable=t.settingsComparable1998&&t.outputComparable1998&&t.captureMetadataObserved1995&&
                iso>0&&exposure>0&&lens>0&&origin>0;
            times=new long[]{gap1998(t,NATIVE_REQUEST1991,IMAGE_RECEIVED1991),
                gap1998(t,IMAGE_RECEIVED1991,BITMAP_DELIVERED1991),gap1998(t,QUEUE_SUBMIT1991,WORKER_START1991),
                gap1998(t,PIPELINE_START1991,PIPELINE_END1991),
                t.noiseFirstMeasured1976?t.noiseFirstNanos1976:-1,
                t.noiseModelMeasured1976?t.noiseModelNanos1976:-1,
                t.noiseStrongMeasured1976?t.noiseStrongNanos1976:-1,
                stage1998(t.stages[CORRECTION]),stage1998(t.stages[ENCODE]),stage1998(t.stages[SAVE]),
                gap1998(t,PUBLISH_START1991,PUBLISH_END1991)};
            arrivalTimes19103=new long[]{cameraGap19103(t.events1991[NATIVE_REQUEST1991],t.cameraHandoff19103[0]),
                cameraGap19103(t.cameraHandoff19103[0],t.cameraHandoff19103[1]),
                cameraGap19103(t.cameraHandoff19103[1],t.events1991[IMAGE_RECEIVED1991]),
                cameraGap19103(t.events1991[NATIVE_REQUEST1991],t.cameraHandoff19103[2]),
                cameraGap19103(t.cameraHandoff19103[1],t.cameraPhases19104[0]),
                cameraGap19103(t.cameraPhases19104[0],t.cameraPhases19104[1]),
                cameraGap19103(t.cameraPhases19104[1],t.cameraPhases19104[2]),
                cameraGap19103(t.cameraPhases19104[2],t.events1991[IMAGE_RECEIVED1991])};
            StringBuilder r=new StringBuilder(160);
            r.append("前段 ");
            if(t.frontPlanObserved1997)r.append(t.frontWorkers1997).append("並列/").append(t.frontRows1997).append("行/空き枠").append(t.frontAvailableWorkers1997);
            else r.append("未観測");
            String[] half={"準備予定","引継ぎ成功","形式対象外","予算不足","確保失敗","生成失敗","生成未完了"};
            r.append("・half ").append(t.frontHalfObserved1997?half[t.frontHalfReason1997]:"未観測");
            String[] correction={"分離処理","GPU連結","GPU連結","GPU連結","強ノイズからGPU連結"};
            r.append("・補正 ").append(t.correctionRoute1976<0?"未観測":correction[t.correctionRoute1976]);
            if(t.correctionRows1994>0)r.append('/').append(t.correctionRows1994).append("行");
            r.append("・モデルvariant ");boolean model=false;
            for(int v=0;v<t.modelVariants1994.length;v++)if(t.modelVariants1994[v]>0){if(model)r.append(',');r.append(v);model=true;}
            if(!model)r.append("未観測");
            r.append("・Strong GPU/CPU ");
            if(t.strongGpuStrips+t.strongCpuStrips==0)r.append("未観測");else r.append(t.strongGpuStrips).append('/').append(t.strongCpuStrips);
            r.append("・Strong profile.variant ");boolean strong=false;
            for(int v=0;v<t.strongProfiles1994.length;v++)if(t.strongProfiles1994[v]>0){if(strong)r.append(',');r.append(v/3).append('.').append(v%3).append('×').append(t.strongProfiles1994[v]);strong=true;}
            if(!strong)r.append("未観測");
            r.append("・開始時GPU準備 ").append(t.startupReady1990?"完了":t.startupState1990<0?"未観測":"未完了");
            r.append("・GPU準備待ち ");appendTime1998(r,t.startupWaitObserved1990?t.startupWaitNanos1990:-1);
            r.append("・開始時の背景検証 ");
            if(!t.captureProbeObserved1995)r.append("未観測");
            else if(t.captureProbeId1995<=0)r.append("なし");
            else r.append('#').append(t.captureProbeId1995).append(' ').append(t.captureProbeKind1995).append('/').append(t.captureProbePhase1995);
            r.append("・前段予算 ");if(t.frontPlanObserved1997){appendMiB1983(r,t.frontBudgetBefore1997);r.append("→");appendMiB1983(r,t.frontBudgetAfter1997);}else r.append("未観測");
            routes=r.toString();
        }
        boolean same1998(RecentCapture1998 b) {
            return comparable&&b.comparable&&settings.equals(b.settings)&&format.equals(b.format)&&
                inputWidth==b.inputWidth&&inputHeight==b.inputHeight&&width==b.width&&height==b.height&&
                lens==b.lens&&iso==b.iso&&exposure==b.exposure&&origin==b.origin&&
                bitmapConflicts1999==0&&b.bitmapConflicts1999==0&&saveBinding1999==b.saveBinding1999&&
                java.util.Arrays.equals(bitmaps1999,b.bitmaps1999);
        }
    }
    private static long gap1998(Trace t,int from,int to) {
        long a=t.events1991[from],b=t.events1991[to];return a>0&&b>=a&&b<=t.publishedNanos1990?b-a:-1;
    }
    private static long stage1998(Stage s) {
        return s.incomplete||s.active>0||s.intervals.isEmpty()?-1:duration(s);
    }
    /** The trace monitor is released before entering this independent ring. */
    private static void remember1998(RecentCapture1998 value) {
        if(value==null)return;
        synchronized(RECENT_LOCK1998){
            int at=0;while(at<RECENT1998.size()&&RECENT1998.get(at).id<value.id)at++;
            if(at<RECENT1998.size()&&RECENT1998.get(at).id==value.id)return;
            RECENT1998.add(at,value);while(RECENT1998.size()>16)RECENT1998.remove(0);
        }
    }
    private static void appendTime1998(StringBuilder text,long nanos) {
        if(nanos<0)text.append("未観測");else text.append(milliseconds(nanos)).append("ms");
    }
    private static void appendStats1998(StringBuilder text,long[] nanos,int count) {
        java.util.Arrays.sort(nanos,0,count);
        long median=nanos[count/2];
        if((count&1)==0){long lower=nanos[count/2-1];median=lower+(median-lower)/2;}
        text.append(count).append("枚 / 最小 ");appendTime1998(text,nanos[0]);
        text.append("・中央値 ");appendTime1998(text,median);text.append("・最大 ");appendTime1998(text,nanos[count-1]);
        text.append("・幅 ");appendTime1998(text,nanos[count-1]-nanos[0]);
    }
    /** Called by the opened/copyable live view only. No pixel-path work, file
     * reads, thermal/GC sampling or persistence. Live provider locks are absent. */
    private static String recentSummary1998() {
        RecentCapture1998[] rows;
        synchronized(RECENT_LOCK1998){rows=RECENT1998.toArray(new RecentCapture1998[RECENT1998.size()]);}
        if(rows.length==0)return "";
        StringBuilder text=new StringBuilder(800);
        text.append("直近の保存時間比較（このプロセス・最大16枚・撮影順）\n撮影記録開始→保存公開成功。公開成功と処理完了を観測した写真だけです。");
        long[] nanos=new long[rows.length];for(int i=0;i<rows.length;i++)nanos[i]=rows[i].elapsed;
        text.append("\n全履歴（条件混在・参考）: ");appendStats1998(text,nanos,rows.length);
        int[] groups=new int[rows.length];int group=0;
        for(int i=0;i<rows.length;i++){
            if(groups[i]!=0||!rows[i].comparable)continue;
            int count=0;for(int j=i;j<rows.length;j++)if(rows[i].same1998(rows[j]))count++;
            if(count<2)continue;
            group++;count=0;for(int j=i;j<rows.length;j++)if(rows[i].same1998(rows[j])){groups[j]=group;nanos[count++]=rows[j].elapsed;}
            text.append("\n記録条件一致 G").append(group).append(": ");appendStats1998(text,nanos,count);
        }
        if(group==0)text.append("\n記録条件一致: 比較可能な2枚以上の組なし");
        String[] names={"受信","描画","キュー","画素","前段","モデル","Strong","補正","圧縮","保存","公開"};
        String[] lenses={"未観測","前面","背面","超広角","望遠"};
        SimpleDateFormat clock=new SimpleDateFormat("HH:mm:ss.SSS",Locale.US);
        for(int i=0;i<rows.length;i++){
            RecentCapture1998 r=rows[i];text.append("\n#").append(r.id);
            if(r.ordinal>0)text.append("（").append(r.ordinal).append("枚目）");
            text.append(' ').append(clock.format(new Date(r.timestampMillis)));
            text.append(groups[i]>0?" G"+groups[i]:r.comparable?" 一致相手なし":" 条件未観測あり");
            text.append(" / 前の記録からの撮影間隔 ");appendTime1998(text,i>0&&r.started>=rows[i-1].started?r.started-rows[i-1].started:-1);
            text.append(" / 全体 ");appendTime1998(text,r.elapsed);
            for(int j=0;j<names.length;j++){text.append(" / ").append(names[j]).append(' ');appendTime1998(text,r.times[j]);}
            text.append("\n  ").append(r.settings).append(" / ISO ").append(r.iso>0?String.valueOf(r.iso):"未観測")
                .append(" / 露光 ").append(r.exposure>0?String.valueOf(r.exposure)+"ns":"未観測")
                .append(" / ").append(lenses[r.lens]).append(" / 入力 ").append(r.inputWidth>0&&r.inputHeight>0?r.inputWidth+"×"+r.inputHeight:"未観測")
                .append(" → 出力 ").append(r.width>0&&r.height>0?r.width+"×"+r.height:"未観測").append(' ').append(r.format)
                .append(" / 開始点 ").append(r.origin==1?"VERecorder要求":r.origin==2?"ネイティブ要求":"記録開始のみ");
            text.append("\n  ").append(r.routes);
            if(r.arrivalTimes19103[0]>=0||r.arrivalTimes19103[3]>=0){
                String[] arrivalLabels={"取得API開始まで","取得API","取得完了→受信","撮影結果通知まで","取得→guard","guard","guard→Q0","Q0→受信"};
                text.append("\n  同じ入力の受信前内訳（重複加算不可）: ");
                for(int k=0;k<arrivalLabels.length;k++){if(k>0)text.append(" / ");text.append(arrivalLabels[k]).append(' ');appendTime1998(text,r.arrivalTimes19103[k]);}
            }
            text.append("\n  ").append(r.provenance1999.replace("\n", " / "));
        }
        text.append("\n※Gは設定・入力/出力・各受渡しで観測したBitmap寸法・ISO・露光・レンズ・計測開始点の観測値が一致する組です。場面・端末温度・他アプリ負荷・回転など未記録の処理条件の一致は未確認です。受信/描画は受渡しの実経過、前段/モデル/Strongは画素工程内、保存は公開を含むため各列は加算できません。開始時の背景検証との因果は未測定です。再起動でこの比較履歴は消えます。");
        return text.toString();
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
        initializeJournal19101(context);
        try { PerformanceHints1952.init(context); } catch(Throwable optional) { }
        try { WholeRoute1953.initialize(context); } catch(Throwable optional) { }
        try { GpuQualification1961.initialize(context); } catch(Throwable optional) { }
        try { GpuStartup1990.initialize(context); } catch(Throwable optional) { }
        try {
            Context app=context.getApplicationContext();
            SharedPreferences p=(app==null?context:app).getSharedPreferences(STORE, Context.MODE_PRIVATE);
            synchronized(LOCK) {
                preferences=p;
                if(loaded) return;
                loaded=true;
                long saved=p.getLong("id",0L);
                if(saved>=nextId && saved<Long.MAX_VALUE-1) nextId=saved+1;
                String savedVersion=p.getString("version", "");
                // .100 had no process journal. Preserve its original snapshot,
                // but never infer its capture process or missing verification.
                if(VERSION.equals(savedVersion)||"1.9.100".equals(savedVersion)) {
                    String text=p.getString("summary", "");
                    if(text.startsWith("ULike v"+savedVersion+" / ") && (latest==null || saved<latest.id)) {
                        // latest in this process remains authoritative, regardless of saved state.
                        if(latest==null) { persisted=text;displayedId=saved; }
                    }
                }
            }
        } catch(Throwable ignored) { }
    }

    /** Existing exact Image/result binding supplies scalar metadata only.
     * Missing values stay unknown; no global camera lookup or clock conversion. */
    public static void captureMetadata1995(Trace trace,int iso,long exposureNanos,int lensKind,int width,int height) {
        if(trace==null||iso<0||exposureNanos<0||lensKind<0||lensKind>4||width<=0||height<=0)return;
        synchronized(trace){
            if(trace.state!=0||trace.captureMetadataObserved1995||trace.metadataRejected1999)return;
            trace.captureMetadataObserved1995=true;trace.captureIso1995=iso;trace.captureExposureNanos1995=exposureNanos;
            trace.captureLens1995=lensKind;trace.captureWidth1995=width;trace.captureHeight1995=height;
        }
    }
    /** Adopt only after ShotContext validates the same input and generation.
     * Missing early hooks remain zero, never reconstructed from SENSOR_TIMESTAMP. */
    public static void cameraHandoff19103(Trace trace,long acquireStart,long acquired,long resultAt,long received) {
        if(trace==null||received<=0)return;
        synchronized(trace) {
            if(trace.state!=0||trace.metadataRejected1999||trace.cameraHandoffObserved19103
                    ||!trace.captureMetadataObserved1995||trace.events1991[IMAGE_RECEIVED1991]!=received)return;
            if(received<trace.started||received>System.nanoTime())return;
            if(acquireStart<trace.started||acquired<acquireStart||acquired>received){acquireStart=0;acquired=0;}
            if(resultAt<trace.started||resultAt>received)resultAt=0;
            trace.cameraHandoff19103[0]=acquireStart;trace.cameraHandoff19103[1]=acquired;
            trace.cameraHandoff19103[2]=resultAt;trace.cameraHandoffObserved19103=true;
        }
    }
    /** Late adoption of exact-image phases; the guard and Q0 retain original ownership. */
    public static void cameraPhases19104(Trace trace,long guardStart,long guardEnd,long q0Entry,long received) {
        if(trace==null||received<=0)return;
        synchronized(trace) {
            if(trace.state!=0||trace.metadataRejected1999||trace.cameraPhasesObserved19104
                    ||!trace.captureMetadataObserved1995||trace.events1991[IMAGE_RECEIVED1991]!=received)return;
            long acquired=trace.cameraHandoff19103[1];
            if(acquired<=0||received<acquired||received>System.nanoTime())return;
            if(guardStart<acquired||guardEnd<guardStart||guardEnd>received){guardStart=0;guardEnd=0;}
            if(q0Entry<acquired||q0Entry>received)q0Entry=0;
            trace.cameraPhases19104[0]=guardStart;trace.cameraPhases19104[1]=guardEnd;trace.cameraPhases19104[2]=q0Entry;
            trace.cameraPhasesObserved19104=true;
        }
    }
    /** Observed callback/image decision only; never borrows a latest result. */
    public static void captureMetadataReason1999(Trace trace,int reason,boolean reject) {
        if(trace==null||reason<1||reason>7)return;
        synchronized(trace){
            if(trace.state!=0)return;
            trace.metadataReason1999=reason;
            if(reject){
                trace.metadataRejected1999=true;trace.captureMetadataObserved1995=false;
                trace.captureIso1995=0;trace.captureExposureNanos1995=0;trace.captureLens1995=0;
                trace.captureWidth1995=0;trace.captureHeight1995=0;
                for(int i=0;i<trace.cameraHandoff19103.length;i++)trace.cameraHandoff19103[i]=0;
                trace.cameraHandoffObserved19103=false;
                for(int i=0;i<trace.cameraPhases19104.length;i++)trace.cameraPhases19104[i]=0;
                trace.cameraPhasesObserved19104=false;
            }
        }
    }
    /** stage: renderer delivery, holder retrieval, owned save input, normalize
     * entry, actual noise input. These are observed full-frame domains, not GPU
     * dispatch totals. Repeat disagreement is retained instead of overwritten. */
    public static void bitmapInput1999(Trace trace,int stage,int width,int height) {
        if(trace==null||stage<0||stage>=5||width<=0||height<=0)return;
        synchronized(trace){
            if(trace.state!=0)return;
            int at=stage*2;
            if(trace.bitmapDimensions1999[at]==0){trace.bitmapDimensions1999[at]=width;trace.bitmapDimensions1999[at+1]=height;}
            else if(trace.bitmapDimensions1999[at]!=width||trace.bitmapDimensions1999[at+1]!=height)trace.bitmapConflictMask1999|=1<<stage;
        }
    }
    /** Capture/save identities are process-local counters, not inferred image IDs. */
    public static void saveInput1999(Trace trace,long ticket,int binding,int ownership) {
        if(trace==null||ticket<=0||binding<1||binding>3||ownership<1||ownership>2)return;
        synchronized(trace){if(trace.state==0&&trace.saveTicket1999==0){
            trace.saveTicket1999=ticket;trace.saveBinding1999=binding;trace.saveOwnership1999=ownership;
        }}
    }
    /** Exact identity lookup for shared-holder admission; a caller thread's
     * active trace cannot identify an unrelated/reused input bitmap. */
    public static Trace exactForKey1999(Object key) {
        if(key==null)return null;
        try{synchronized(LOCK){prune(System.nanoTime());return findKey(key);}}
        catch(Throwable ignored){return null;}
    }
    /** An explicit foreground worker supplies this trace. Never look up the
     * latest photo or retain a callback/model/queue decision for diagnostics. */
    public static void frontProof19100(Trace trace,int backend,int state,int copyReason,
            long retained,long snapshotPeak,long executionPeak) {
        if(trace==null||backend<1||backend>2||state<1||state>=23||copyReason< -1||copyReason>=10)return;
        synchronized(trace){
            if(trace.state!=0)return;
            trace.frontProofCounts19100[state]=addWork1973(trace.frontProofCounts19100[state],1);
            trace.pipelineObserved1988=true;
            if(copyReason>=0&&(state==7||state==11))
                trace.frontCopyCounts19100[copyReason]=addWork1973(trace.frontCopyCounts19100[copyReason],1);
            long[] last=trace.frontProofLast19100;
            last[0]=backend;last[1]=state;if(copyReason>=0)last[2]=copyReason;
            last[3]=Math.max(-1,retained);last[4]=Math.max(-1,snapshotPeak);last[5]=Math.max(-1,executionPeak);
        }
    }
    /** Counts only committed private-worker GPU reads, not failed candidates. */
    public static void strongReadback1995(Trace trace,int strips,long pixels) {
        if(trace==null||strips<=0||pixels<strips)return;
        synchronized(trace){
            if(trace.state!=0)return;
            trace.strongDirectStrips1995=addWork1973(trace.strongDirectStrips1995,strips);
            trace.strongDirectPixels1995=addWork1973(trace.strongDirectPixels1995,pixels);
        }
    }
    /** Observed selection/fallback only; first and latest step stay bounded. */
    public static void finishSelection1995(Trace trace,int reason,int rows) {
        if(trace==null||reason<1||reason>7||rows<0||rows>128)return;
        synchronized(trace){
            if(trace.state!=0)return;
            if(trace.finishSelectionCount1995==0){trace.finishFirstReason1995=reason;trace.finishFirstRows1995=rows;}
            trace.finishLastReason1995=reason;trace.finishLastRows1995=rows;
            if(trace.finishSelectionCount1995<Integer.MAX_VALUE)trace.finishSelectionCount1995++;
        }
    }
    /** New diagnostic photo; no global last-bitmap or last-shot guessing. */
    public static Trace begin(Object key) {
        return begin1991(key,System.nanoTime(),0);
    }
    /** The observed SDK request is not the user's UI touch. Preserve that
     * distinction, including fallback captures with only native entry observed. */
    public static Trace beginCapture1991(Object key,long requestNanos,int origin) {
        if(origin!=1&&origin!=2)return begin(key);
        long now=System.nanoTime();
        if(requestNanos<=0||requestNanos>now||now-requestNanos>TTL)return begin(key);
        return begin1991(key,requestNanos,origin);
    }
    /** A consumed ACTION_DOWN lexical scope is supplied only by the exact shot
     * callback constructor. Never infer a touch from the latest UI event. */
    public static void shutterDown1994(Trace trace,long downNanos) {
        if(trace==null||downNanos<=0||downNanos>trace.started||trace.started-downNanos>TTL)return;
        try{synchronized(trace){
            if(trace.state==0&&trace.shutterDownNanos1994==0)trace.shutterDownNanos1994=downNanos;
        }}catch(Throwable optional){}
    }
    private static Trace begin1991(Object key,long start,int origin) {
        try {
            Trace trace;
            synchronized(LOCK) {
                long now=System.nanoTime();prune(now);
                trace=new Trace(nextId++, System.currentTimeMillis(),start);
                trace.requestOrigin1991=origin;
                if(origin!=0)trace.events1991[REQUEST1991]=start;
                TRACES.add(trace);
                while(TRACES.size()>MAX_TRACES) TRACES.remove(0);
                if(key!=null) bindLocked(key,trace);
                latest=trace;displayedId=trace.id;
                captureIdentity1989=null;
            }
            WholeRoute1953.foregroundStarted();
            // Stop optional saved-photo probes as soon as this capture begins.
            // associate() still advances the shot epoch and rejects late commits.
            // Snapshot the running private probe without holding timing LOCK
            // or the trace monitor. Only bounded scalars cross the boundary.
            try {
                GpuQualification1961.CaptureProbe1995 probe=GpuQualification1961.captureProbe1995();
                synchronized(trace){
                    trace.captureProbeObserved1995=true;
                    if(probe!=null){trace.captureProbeId1995=probe.id;
                        trace.captureProbeKind1995=shortText(probe.kind,64);trace.captureProbePhase1995=shortText(probe.phase,96);
                        trace.captureProbeCancelled1995=probe.alreadyCancelled;}
                }
            }catch(Throwable optional){}
            GpuQualification1961.captureChanged();
            publish(trace,false);
            refreshViews(true);
            return trace;
        } catch(Throwable ignored) {return null;}
    }

    /** Scalar-only diagnostic: no rendering, persistence, GPU query or allocation
     * on camera/save workers. First occurrence wins; late callbacks are inert. */
    public static void mark1991(Trace trace,int event) {
        markAt1991(trace,event,System.nanoTime());
    }
    static void markAt1991(Trace trace,int event,long now) {
        if(trace==null||event<=REQUEST1991||event==PUBLISH_END1991||event>=trace.events1991.length||now<=0)return;
        try{synchronized(trace){
            if(trace.state!=0||now<trace.started||trace.events1991[event]!=0)return;
            trace.events1991[event]=now;
        }}catch(Throwable ignored){}
    }
    /** Exact explicit capture owner only. API elapsed includes any wait within
     * that boundary; it is not a GPU timer or a disjoint share of wall time.
     * bytes is the logical payload/allocation requested by this category,
     * never measured bus traffic or peak resident memory. -1 stays unknown.
     * Failed attempts also count; background probes pass no capture owner. */
    public static void finishWork1992(Trace trace,int category,long nanos,long bytes) {
        if(trace==null||category<0||category>=FINISH_LABELS1992.length||nanos< -1||bytes< -1)return;
        try{synchronized(trace){
            if(trace.state!=0)return;
            int at=category*4;long[] work=trace.finishWork1992;
            work[at]=addWork1973(work[at],1);
            if(nanos>=0){work[at+1]=addWork1973(work[at+1],nanos);work[at+3]|=1;}
            else work[at+3]|=2;
            if(bytes>=0){work[at+2]=addWork1973(work[at+2],bytes);work[at+3]|=4;}
            else work[at+3]|=8;
        }}catch(Throwable ignored){}
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
                if(trace.shotId!=shotId){GpuQualification1961.captureChanged();captureEpoch1953++;noteCapture1989(captureEpoch1953,trace.id);}
                if(trace.shotId==0)trace.captureOrdinal1990=++captureOrdinal1990;
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
        try{synchronized(trace){if(trace.state!=0)return;trace.requested=shortText(description,500);trace.settingsComparable1998=description.length()>0&&description.length()<=500;}publish(trace,false);}catch(Throwable ignored){}
    }
    public static void output(Trace trace,int width,int height,String format) {
        if(trace==null)return;
        try{synchronized(trace){if(trace.state!=0)return;trace.width=Math.max(0,width);trace.height=Math.max(0,height);trace.format=shortText(format,60);trace.outputComparable1998=width>0&&height>0&&format!=null&&format.length()>0&&format.length()<=60;}publish(trace,false);}catch(Throwable ignored){}
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
    /** Scalar-only sink. Foreground exclusion belongs to PipelineDetail1988;
     * terminal ownership is checked again under this capture's monitor. There
     * are no provider calls, strings, UI updates or per-operation allocations. */
    /** Explicit-owner worker snapshot. These intervals overlap existing front
     * work and are never added to photo/stage wall time. No array is retained. */
    public static void front1996(Trace trace,long[] work,long[] routes) {
        if(trace==null||work==null||work.length!=24||routes==null||routes.length!=10)return;
        for(long n:work)if(n<0)return;for(long n:routes)if(n<0)return;
        for(int i=0;i<24;i+=2)if(work[i]==0&&work[i+1]!=0)return;
        try{synchronized(trace){if(trace.state!=0)return;
            for(int i=0;i<24;i++)trace.frontWork1996[i]=addWork1973(trace.frontWork1996[i],work[i]);
            for(int i=0;i<10;i++)trace.frontRoutes1996[i]=addWork1973(trace.frontRoutes1996[i],routes[i]);
            trace.frontObserved1996=addWork1973(trace.frontObserved1996,1);
        }}catch(Throwable optional){}
    }
    public static void frontPlan1997(Trace trace,int workers,int rows,int halfReason,
            long budgetBefore,long budgetAfter,long workerBytes,long halfBytes,int trimOutcome,int availableWorkers) {
        if(trace==null||workers<1||rows<1||halfReason<0||halfReason>6||budgetBefore<0||budgetAfter<0
                ||workerBytes<0||halfBytes<0||trimOutcome< -1||trimOutcome>2||availableWorkers<1)return;
        try{synchronized(trace){if(trace.state!=0)return;
            trace.frontPlanObserved1997=true;trace.frontWorkers1997=workers;trace.frontRows1997=rows;
            trace.frontHalfReason1997=halfReason;trace.frontBudgetBefore1997=budgetBefore;
            trace.frontBudgetAfter1997=budgetAfter;trace.frontWorkerBytes1997=workerBytes;
            trace.frontHalfBytes1997=halfBytes;trace.frontTrim1997=trimOutcome;
            trace.frontAvailableWorkers1997=availableWorkers;
        }}catch(Throwable optional){}
    }
    public static void frontHalf1997(Trace trace,int reason,int pixels) {
        if(trace==null||reason<0||reason>6||pixels<0||reason==1&&pixels==0||reason!=1&&pixels!=0)return;
        try{synchronized(trace){if(trace.state!=0)return;
            trace.frontHalfObserved1997=true;trace.frontHalfReason1997=reason;trace.frontHalfPixels1997=pixels;
        }}catch(Throwable optional){}
    }
    public static void finishMemory1997(Trace trace,long[] values) {
        if(trace==null||values==null||values.length!=33||values[0]!=1||values[6]!=0
                ||values[1]<0||values[2]!=0||values[3]<0||values[3]>7||values[4]<0||values[4]>1
                ||values[5]<1||values[5]>2||values[7]<0||values[8]<0||values[8]>3)return;
        try{synchronized(trace){if(trace.state!=0)return;
            System.arraycopy(values,0,trace.finishMemory1997,0,33);
            if(trace.finishMemoryCount1997<Integer.MAX_VALUE)trace.finishMemoryCount1997++;
        }}catch(Throwable optional){}
    }
    public static void finishValidationSkipped1997(Trace trace,int reason) {
        if(trace==null||reason!=1)return;
        try{synchronized(trace){if(trace.state==0)trace.finishValidationSkipped1997=reason;}}
        catch(Throwable optional){}
    }
    public static void modelOverlap1996(Trace trace,long regionNanos,long submitNanos,long collectNanos,
            boolean submitted,boolean joined) {
        if(trace==null||regionNanos< -1||submitNanos< -1||collectNanos< -1||joined&&!submitted)return;
        try{synchronized(trace){if(trace.state!=0)return;long[] m=trace.modelOverlap1996;
            m[0]=addWork1973(m[0],1);
            if(regionNanos>=0){m[1]=addWork1973(m[1],regionNanos);m[6]|=1;}else m[6]|=2;
            if(submitNanos>=0){m[2]=addWork1973(m[2],submitNanos);m[7]|=1;}else m[7]|=2;
            if(collectNanos>=0){m[3]=addWork1973(m[3],collectNanos);m[8]|=1;}else m[8]|=2;
            if(submitted)m[4]=addWork1973(m[4],1);if(joined)m[5]=addWork1973(m[5],1);
        }}catch(Throwable optional){}
    }
    public static void pipelineDetail1988(Trace trace,int phase,int backend,int reason,int units,long nanos) {
        if(trace==null||phase<0||phase>=PIPELINE_PHASES1988||backend< -1||backend>1||reason<0||
                reason>=PIPELINE_REASONS1988||units<=0||nanos< -1||backend!=0&&reason!=0)return;
        if(phase==1&&(reason==20||reason==21))shaderFailure19100(trace,reason==20?1:5);
        try{synchronized(trace){
            if(trace.state!=0)return;
            int index=(phase*3+backend+1)*3;
            long[] work=trace.pipelineWork1988;
            work[index]=addWork1973(work[index],units);
            if(nanos>=0){work[index+1]=addWork1973(work[index+1],nanos);work[index+2]|=1;}
            else work[index+2]|=2;
            if(backend==0){int slot=phase*PIPELINE_REASONS1988+reason;
                trace.pipelineReasons1988[slot]=addWork1973(trace.pipelineReasons1988[slot],units);}
            trace.pipelineObserved1988=true;
        }}catch(Throwable optional){}
    }
    public static void pipelineCopy1988(Trace trace,int phase,int outcome,long bytes,long nanos) {
        if(trace==null||phase<0||phase>=PIPELINE_PHASES1988||outcome<1||outcome>=PIPELINE_COPIES1988||bytes< -1||nanos< -1)return;
        try{synchronized(trace){
            if(trace.state!=0)return;
            int index=(phase*PIPELINE_COPIES1988+outcome)*4;
            long[] copies=trace.pipelineCopies1988;
            copies[index]=addWork1973(copies[index],1);
            if(nanos>=0){copies[index+1]=addWork1973(copies[index+1],nanos);copies[index+3]|=1;}
            else copies[index+3]|=2;
            if(bytes>=0){copies[index+2]=addWork1973(copies[index+2],bytes);copies[index+3]|=4;}
            else copies[index+3]|=8;
            trace.pipelineObserved1988=true;
        }}catch(Throwable optional){}
    }
    public static void pipelinePriorReason1989(Trace trace,int phase,int reason,int units) {
        if(trace==null||phase<0||phase>=PIPELINE_PHASES1988||reason<=0||reason>=PIPELINE_REASONS1988||units<=0)return;
        if(phase==1&&(reason==20||reason==21))shaderFailure19100(trace,reason==20?1:5);
        try{synchronized(trace){
            if(trace.state!=0)return;
            int index=phase*PIPELINE_REASONS1988+reason;
            trace.pipelinePriorReasons1989[index]=addWork1973(trace.pipelinePriorReasons1989[index],units);
            trace.pipelineObserved1988=true;
        }}catch(Throwable optional){}
    }
    /** Snapshot only an already observed failure for this unfinished photo.
     * Optional reflection preserves old timing peers and never starts GPU work. */
    private static void shaderFailure19100(Trace trace,int shader) {
        if(trace==null||(shader!=1&&shader!=5))return;
        int slot=shader==1?0:1;
        try {
            synchronized(trace){if(trace.state!=0||trace.shaderFailures19100[slot]!=null)return;}
            Object value=Class.forName("com.hiro.ulike.GpuNoise1960",false,ProcessingTiming1947.class.getClassLoader())
                .getMethod("programFailure19100",int.class).invoke(null,Integer.valueOf(shader));
            if(!(value instanceof String)||((String)value).length()==0)return;
            String raw=(String)value;StringBuilder clean=new StringBuilder(Math.min(240,raw.length()));
            for(int i=0;i<raw.length()&&i<240;i++){char c=raw.charAt(i);clean.append(c<' '||c==127?' ':c);}
            synchronized(trace){if(trace.state==0&&trace.shaderFailures19100[slot]==null)trace.shaderFailures19100[slot]=clean.toString();}
        }catch(Throwable optional){}
    }
    /** New quantity fields supplement the existing copy event once; this sink
     * never increments the old event or output counters and retains only scalars. */
    public static void pipelineCopyState1989(Trace trace,int phase,int outcome,long requestedBytes,
            long acquiredPixelBytes,long nanos,int queueReason,long retryRemainingNanos,int retries,
            int queuedJobs,int runningJobs,long retainedBytes) {
        if(trace==null||phase<0||phase>=PIPELINE_PHASES1988||outcome<1||outcome>=PIPELINE_COPIES1988||
                requestedBytes< -1||acquiredPixelBytes< -1||nanos< -1||queueReason< -1||queueReason>=QUEUE_LABELS_1983.length||
                retryRemainingNanos< -1||retries< -1||queuedJobs< -1||runningJobs< -1||retainedBytes< -1)return;
        try{synchronized(trace){
            if(trace.state!=0)return;
            int index=(phase*PIPELINE_COPIES1988+outcome)*4;
            long[] quantities=trace.pipelineCopyQuantities1989;
            quantities[index]=addWork1973(quantities[index],1);
            if(requestedBytes>=0){quantities[index+1]=addWork1973(quantities[index+1],requestedBytes);quantities[index+3]|=1;}
            else quantities[index+3]|=2;
            if(acquiredPixelBytes>=0){quantities[index+2]=addWork1973(quantities[index+2],acquiredPixelBytes);quantities[index+3]|=4;}
            else quantities[index+3]|=8;
            if(queueReason>=0){
                int reasonIndex=phase*QUEUE_LABELS_1983.length+queueReason;
                trace.pipelineQueueCounts1989[reasonIndex]=addWork1973(trace.pipelineQueueCounts1989[reasonIndex],1);
                int last=phase*10;long[] decision=trace.pipelineQueueLast1989;
                decision[last]=addWork1973(decision[last],1);decision[last+1]=queueReason;
                decision[last+2]=retryRemainingNanos;decision[last+3]=retries;decision[last+4]=queuedJobs;
                decision[last+5]=runningJobs;decision[last+6]=retainedBytes;decision[last+7]=requestedBytes;
                decision[last+8]=acquiredPixelBytes;decision[last+9]=outcome;
            }
            trace.pipelineObserved1988=true;
        }}catch(Throwable optional){}
    }
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
    /** Record only the actual completed correction tile height. Route IDs are
     * legacy categories and must not override a fallback's observed rows. */
    public static void detailRows1994(Trace trace,int rows) {
        if(trace==null||(rows!=32&&rows!=64&&rows!=128))return;
        try{synchronized(trace){if(trace.state==0)trace.correctionRows1994=rows;}}
        catch(Throwable optional){}
    }
    /** Successful geometry API bands only, including partial final bands and
     * adaptive memory fallback. No per-pixel observation or UI refresh. */
    public static void geometryBand1994(Trace trace,int rows,long nanos) {
        if(trace==null||rows<=0||rows>65535||nanos<0)return;
        try{synchronized(trace){
            if(trace.state!=0)return;
            trace.geometryBands1994=addWork1973(trace.geometryBands1994,1);
            trace.geometryApiNanos1994=addWork1973(trace.geometryApiNanos1994,nanos);
            if(trace.geometryMinRows1994==0||rows<trace.geometryMinRows1994)trace.geometryMinRows1994=rows;
            if(rows>trace.geometryMaxRows1994)trace.geometryMaxRows1994=rows;
        }}catch(Throwable optional){}
    }
    /** Called once after the Strong workers join. These are API wall intervals,
     * not GPU kernel timers or additional disjoint photo latency. */
    public static void strongBreakdown1994(Trace trace,long waitNanos,long waits,
            long submitNanos,long submits,long collectNanos,long collects) {
        if(trace==null||waitNanos<0||waits<0||submitNanos<0||submits<0||collectNanos<0||collects<0)return;
        try{synchronized(trace){
            if(trace.state!=0)return;
            long[] values={waitNanos,waits,submitNanos,submits,collectNanos,collects};
            for(int i=0;i<values.length;i++)trace.strongBreakdown1994[i]=addWork1973(trace.strongBreakdown1994[i],values[i]);
            trace.strongBreakdownObserved1994=true;
        }}catch(Throwable optional){}
    }
    /** Selected final GPU outputs only; stage preparation is not adoption. */
    public static void strongProfile1994(Trace trace,int profile,int variant,int strips) {
        if(trace==null||profile<0||profile>=8||variant<0||variant>=3||strips<=0)return;
        try{synchronized(trace){if(trace.state!=0)return;int at=profile*3+variant;
            trace.strongProfiles1994[at]=addWork1973(trace.strongProfiles1994[at],strips);
        }}catch(Throwable optional){}
    }
    public static void modelVariant1994(Trace trace,int variant) {
        if(trace==null||variant<0||variant>=3)return;
        try{synchronized(trace){if(trace.state==0)
            trace.modelVariants1994[variant]=addWork1973(trace.modelVariants1994[variant],1);
        }}catch(Throwable optional){}
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
    /** Observe the real publication return, never a thumbnail or early UI receipt.
     * No URI/path or image is retained. A terminal photo is immutable. */
    public static void publication1990(Trace trace,boolean success) {
        if(trace==null)return;
        long now=System.nanoTime();
        synchronized(trace){
            if(trace.state!=0||now<trace.started||trace.publicationSucceeded1990)return;
            trace.publicationObserved1990=true;
            trace.publicationSucceeded1990=success;
            trace.publishedNanos1990=success?now:0;
            if(success)trace.events1991[PUBLISH_END1991]=now;
        }
        // Queue admission never takes its lock while the trace monitor is held.
        // Every accepted publication resets quiet time and rearms cancelled jobs;
        // family owners retain once-per-process completed-attempt limits.
        if(success){
            try{GpuQualification1961.publicationSucceeded1995();}catch(Throwable optional){}
            try{GpuStrongDefault1990.request1994();}catch(Throwable optional){}
            try{GpuModelDefault1990.requestOptional1994();}catch(Throwable optional){}
            try{GpuFinishDefault1990.requestWide1995();}catch(Throwable optional){}
        }
    }

    /** The foreground owner reports its actual wait, not the startup worker's
     * cumulative work. No live GPU/certificate lookup or UI refresh occurs. */
    public static void startupWait1990(Trace trace,long nanos,boolean ready) {
        if(trace==null||nanos<0)return;
        synchronized(trace){
            if(trace.state!=0)return;
            trace.startupWaitObserved1990=true;trace.startupWaitReady1990=ready;
            trace.startupWaitNanos1990=nanos;
        }
    }

    public static void finish(Trace trace,boolean success) {
        if(trace==null)return;
        try {
            RecentCapture1998 recent=null;
            synchronized(trace) {
                if(trace.state!=0)return;
                trace.gpuQualification=shortText(GpuQualification1961.status1967().replace("（現在）",""),640);
                trace.finished=System.nanoTime();trace.state=success?1:2;
                for(Stage stage:trace.stages)if(stage.active>0)stage.incomplete=true;
                // Optional diagnostics must not prevent publication bookkeeping.
                if(success&&trace.publicationSucceeded1990&&trace.publishedNanos1990>=trace.started)
                    try{recent=new RecentCapture1998(trace);}catch(Throwable optional){}
            }
            try{remember1998(recent);}catch(Throwable optional){}
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
    /** The displayed Trace.id is bound to the exact associated epoch. Read it
     * without the timing monitor: qualification may already own its monitor.
     * There is no latest-photo fallback or ShotContext-ID substitution. */
    static long captureId1989(long expectedEpoch) {
        CaptureIdentity1989 identity=captureIdentity1989;
        return identity!=null&&identity.epoch==expectedEpoch&&expectedEpoch==captureEpoch1953?identity.traceId:0;
    }
    private static void noteCapture1989(long epoch,long traceId) {
        try{captureIdentity1989=new CaptureIdentity1989(epoch,traceId);}
        catch(Throwable optional){captureIdentity1989=null;}
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
        return "ULike v"+VERSION+" / 新しい撮影の計測待ち\n強ノイズ処理: 計測待ち（新しい撮影後に判定）\n強ノイズGPU方針: 起動時の実出力検証済み標準経路を優先。追加候補は別途認定\n合成: 未計測 / ノイズ除去: 未計測 / 補正: 未計測 / 圧縮: 未計測 / 保存: 未計測";
    }

    // Optional reflection preserves old diagnostic-only host fixtures. Release
    // integration tests require the real journal class and exercise this bridge.
    private static void initializeJournal19101(Context context) {
        try { Class.forName("com.hiro.ulike.VerificationJournal19101")
            .getMethod("initialize",Context.class).invoke(null,context); } catch(Throwable optional) { }
    }
    private static String journalText19101(String method,int limit) {
        try {
            Object value=Class.forName("com.hiro.ulike.VerificationJournal19101").getMethod(method).invoke(null);
            return value instanceof String?shortText((String)value,limit):"";
        }catch(Throwable optional){return "";}
    }
    private static String validProcessId19101(String value) {
        if(value==null||value.length()==0||value.length()>96)return "";
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='-'||c=='_'))return "";
        }
        return value;
    }
    private static String currentProcessId19101() {
        return validProcessId19101(journalText19101("processId",97));
    }
    /** Read only the fixed line immediately after the GPU policy in the same snapshot.
     * No latest-trace lookup: a new capture can begin after summary() returns. */
    private static String savedProcessId19101(String saved) {
        if(saved==null)return "";
        int policy=saved.indexOf("\n強ノイズGPU方針: "),at=policy<0?0:saved.indexOf('\n',policy+1)+1;
        String prefix="撮影記録のプロセスID: ";
        if(at<=0||!saved.startsWith(prefix,at))return "";
        int begin=at+prefix.length(),end=saved.indexOf('\n',begin);
        return validProcessId19101(saved.substring(begin,end<0?saved.length():end));
    }
    private static String processView19101(String saved) {
        String current=currentProcessId19101(),captured=savedProcessId19101(saved);
        StringBuilder text=new StringBuilder(220);
        text.append("表示時点のプロセスID: ").append(current.length()>0?current:"未記録");
        text.append("\n表示時点の実行版: ULike v").append(VERSION);
        text.append("\n表示中の撮影記録: ");
        int end=saved.indexOf('\n');String heading=end<0?saved:saved.substring(0,end);
        if(!heading.contains(" / 撮影 #"))text.append("なし（新しい撮影の計測待ち）");
        else if(captured.length()==0)text.append("撮影した起動のID未記録（現在の起動との一致は判定できません。撮影順は記録当時の起動内の順番です。この撮影と過去の検証履歴との対応は判定できません）");
        else if(current.length()==0)text.append("現在の起動ID未記録（撮影した起動との一致は判定できません）");
        else if(current.equals(captured))text.append("現在の起動で作成した記録");
        else text.append("別の起動で保存された記録（撮影順は保存当時の起動内の順番です）");
        text.append("\n以下の現在状況は表示時点の起動の情報です。過去起動の検証履歴は別欄に表示します。");
        return text.toString();
    }

    /** Live queue state belongs only to this opened view, never to the saved
     * photograph's immutable counters, elapsed time, or persisted summary. */
    public static String liveSummary1979() {
        String saved=summary();
        try {
            String live=saved+"\n\n"+processView19101(saved)+"\n\n表示時点の"+GpuQualification1961.status1967()
                +"\n検証キューと条件別の記録は、この表示を開き直すと更新されます。写真のGPU区間数と処理時間は撮影時の記録です。";
            // The attempt ledger describes later private verification work.
            // Never append it to the persisted photograph or count a successful
            // certificate as output adopted by that photograph.
            try {
                String attempts=GpuQualification1961.attemptSummary1986();
                if(attempts!=null&&attempts.length()>0)live+="\n\n"+shortText(attempts,6000);
            }catch(Throwable optional){}
            try{String wide=GpuFinishDefault1990.summaryWide1995();if(wide!=null&&wide.length()>0)live+="\n\n表示時点の128行補正検証: "+shortText(wide,600);}catch(Throwable optional){}
            try{String recent=recentSummary1998();live+="\n\n"+(recent.length()>0?recent:"直近の保存時間比較（現在の起動）: 現在の起動では保存完了を確認した撮影記録なし");}catch(Throwable optional){}
            try{String journal=journalText19101("summary",14000);if(journal.length()>0)live+="\n\n"+journal;}catch(Throwable optional){}
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
            for(int at=0;at<trace.strongProfiles1994.length;at++)if(trace.strongProfiles1994[at]>0){
                int profile=at/3;
                text.append("\n強ノイズGPU実採用: ").append(profile==4?"標準方式":profile==6?"追加認定方式・2の累乗除算":profile==7?"追加認定方式・タイル8":"認定方式")
                    .append(" / profile ").append(profile).append(" / variant ").append(at%3)
                    .append(" / ").append(trace.strongProfiles1994[at]).append("区間");
            }
            for(int variant=0;variant<trace.modelVariants1994.length;variant++)if(trace.modelVariants1994[variant]>0)
                text.append("\nモデルGPU実採用: variant ").append(variant).append(" / 1作業グループ ")
                    .append(variant==0?64:variant==1?32:128).append("スレッド / ")
                    .append(trace.modelVariants1994[variant]).append("回");
            text.append("\n強ノイズGPU方針: 起動時の実出力検証済み標準経路を優先。追加候補は別途認定");
            text.append("\n撮影記録のプロセスID: ").append(trace.processId19101.length()>0?trace.processId19101:"未記録");
            appendSaveLatency1990(text,trace);
            if(trace.strongRoute1978>=0) {
                text.append("\n今回の強ノイズ配分: ");
                if(trace.strongRoute1978==1)text.append("実並列速度を確認済み・CPU併用");
                else if(trace.strongRoute1978==2)text.append("採用条件が成立しない区間はCPU（理由は下記）");
                else text.append("GPU優先・CPU併用の速度は未認定");
            }
            if(trace.strongDirectStrips1995>0){
                text.append("\n強ノイズGPU直接読戻し: ").append(trace.strongDirectStrips1995).append("区間 / ")
                    .append(trace.strongDirectPixels1995).append("画素（成功した出力のみ。Javaの中間ARGBコピーを省略）");
            }
            text.append("\n強ノイズGPU確認: CPU照合 ").append(trace.strongCpuVerifications).append("回");
            if(trace.strongCpuOracleMeasured||trace.strongGpuRequestMeasured||trace.strongWaitMeasured||trace.strongOracleReuses>0||trace.strongLockWaitMeasured1982) {
                text.append("\n強ノイズCPU経路・照合作業累計: ").append(trace.strongCpuOracleMeasured?milliseconds(trace.strongCpuOracleNanos)+" ms":"未計測");
                text.append("\nGPU準備・転送・実行・読戻し作業累計: ").append(trace.strongGpuRequestMeasured?milliseconds(trace.strongGpuRequestNanos)+" ms":"未計測");
                text.append("\n強ノイズ撮影内ワーカーのGPU枠待機・判定累計: ").append(trace.strongWaitMeasured?milliseconds(trace.strongWaitNanos)+" ms":"未計測");
                text.append("\nGPU枠受付ロック取得待ち累計: ").append(trace.strongLockWaitMeasured1982?milliseconds(trace.strongLockWaitNanos1982)+" ms":"未計測");
                if(trace.strongOracleReuses>0)text.append("\n同じ区間のCPU参照結果を追加照合に再利用: ").append(trace.strongOracleReuses).append("回");
            }
            if(trace.strongBreakdownObserved1994){
                long[] work=trace.strongBreakdown1994;
                text.append("\n強ノイズGPU枠待ちの内訳: 待機開始→復帰 ").append(milliseconds(work[0]))
                    .append(" ms / ").append(work[1]).append("回（撮影内ワーカーの枠解放待ち・ロック再取得を含む。背景検証の待機時間ではありません）");
                text.append("\n強ノイズ転送・投入API累計: ").append(milliseconds(work[2])).append(" ms / ").append(work[3]).append("回");
                text.append("\n強ノイズ結果取得API累計: ").append(milliseconds(work[4])).append(" ms / ").append(work[5])
                    .append("回（GPU完了待ち・読戻しを含む）");
                text.append("\n※これらは並行ワーカーの作業累計です。GPUカーネル単独の時間ではなく、上のGPU要求・枠待ち時間に含まれるため撮影時間に加算しません。");
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
            appendFrontPlan1997(text,trace);
            appendFront1996(text,trace);
            long[] overlap1996=trace.modelOverlap1996;
            if(overlap1996[0]>0)text.append("\nモデル準備のCPU/GPU連携: 投入 ").append(overlap1996[4])
                .append("回 / 回収完了 ").append(overlap1996[5]).append("回 / GPU要求が未回収の間のCPU領域推定 ")
                .append(measuredWork1996(overlap1996[1],overlap1996[6])).append(" / GPU投入API ").append(measuredWork1996(overlap1996[2],overlap1996[7]))
                .append(" / GPU回収API ").append(measuredWork1996(overlap1996[3],overlap1996[8]))
                .append("\n※GPU要求の保留中に行ったCPU作業の記録です。GPUカーネルとの同時実行時間ではなく、モデル工程に含まれます。");
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
                    case 1:case 2:case 3:
                        text.append("GPU連結");
                        if(trace.correctionRows1994>0)text.append('（').append(trace.correctionRows1994).append("行・実行記録）");
                        else text.append("（実行行数は未観測）");
                        break;
                    case 4:
                        text.append("強ノイズからGPU連結");
                        if(trace.correctionRows1994>0)text.append('（').append(trace.correctionRows1994).append("行・実行記録）");
                        break;
                }
            } else if(trace.state!=0)text.append("\n補正経路: 採否未記録");
            if(trace.finishSelectionCount1995>0){
                text.append("\n補正行数の選択理由: ");appendFinishSelection1995(text,trace.finishFirstReason1995,trace.finishFirstRows1995);
                if(trace.finishSelectionCount1995>1){text.append(" → ");appendFinishSelection1995(text,trace.finishLastReason1995,trace.finishLastRows1995);}
                if(trace.finishSelectionCount1995>2)text.append("（全").append(trace.finishSelectionCount1995).append("判断の最初と最後）");
            }
            appendFinishMemory1997(text,trace);
            if(trace.geometryBands1994>0){
                text.append("\n回転・リサイズGPU API: ").append(trace.geometryBands1994).append("回 / 区間 ")
                    .append(trace.geometryMinRows1994);
                if(trace.geometryMaxRows1994!=trace.geometryMinRows1994)text.append('～').append(trace.geometryMaxRows1994);
                text.append("行 / 呼出し累計 ").append(milliseconds(trace.geometryApiNanos1994))
                    .append(" ms（GPUカーネル単独の時間ではありません）");
            }
            int pipelineStart1988=text.length();
            try{appendPipelineDetail1988(text,trace);}catch(Throwable optional){text.setLength(pipelineStart1988);}
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
            // Saved photos use the publication boundary consistently. Diagnostic
            // completion/persistence after publication is not user save latency.
            long until=trace.publicationSucceeded1990?trace.publishedNanos1990:
                trace.finished==0?System.nanoTime():trace.finished;
            long totalStart=trace.shutterDownNanos1994>0?trace.shutterDownNanos1994:trace.started;
            text.append("\n全体経過: ").append(milliseconds(Math.max(0L,until-totalStart))).append(" ms");
            text.append(trace.publicationSucceeded1990?(trace.shutterDownNanos1994>0?
                "（シャッターボタン押下受付→保存公開完了。並行工程は加算しません）":
                "（撮影記録開始→保存公開完了。並行工程は加算しません）"):
                "（保存公開未完了。並行工程は加算しません）");
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
    /** Rendering runs only at existing publication/read points and queries no
     * live route provider. Auxiliary work can be nested inside another phase. */
    private static String measuredWork1996(long nanos,long flags) {
        return (flags&1)==0?"未計測":milliseconds(nanos)+" ms"+((flags&2)!=0?"（ほか未計測）":"");
    }
    private static void appendFrontPlan1997(StringBuilder text,Trace trace) {
        if(!trace.frontPlanObserved1997&&!trace.frontHalfObserved1997)return;
        if(trace.frontPlanObserved1997){
            text.append("\n前段の実行計画: ").append(trace.frontWorkers1997).append("ワーカー / 区間 ")
                .append(trace.frontRows1997).append("行 / 選択時のCPU空き枠 ").append(trace.frontAvailableWorkers1997);
            text.append("\n前段のメモリ予算: 初回 ");appendMiB1983(text,trace.frontBudgetBefore1997);
            text.append(" / 計画時 ");appendMiB1983(text,trace.frontBudgetAfter1997);
            text.append(" / ワーカーと共有領域 ");appendMiB1983(text,trace.frontWorkerBytes1997);
            text.append(" / 縮小画像要求量 ");appendMiB1983(text,trace.frontHalfBytes1997);
            text.append(" / 未使用CPU作業領域の整理: ").append(trace.frontTrim1997<0?"不要・未実施":
                trace.frontTrim1997==0?"CPU使用中のため見送り":trace.frontTrim1997==1?"整理を要求":"一部の整理に失敗");
        }
        String[] reasons={"準備予定","引継ぎ成功","画像形式が対象外","同じ並列数での予算不足","確保失敗","生成失敗","生成未完了"};
        text.append("\n前段からモデルへの縮小画像: ").append(reasons[trace.frontHalfReason1997]);
        if(trace.frontHalfReason1997==1)text.append(" / ").append(trace.frontHalfPixels1997).append("画素");
        else if(trace.frontHalfReason1997>=2)text.append("（モデル側で必要な入力を生成）");
    }
    private static void appendFinishMemory1997(StringBuilder text,Trace trace) {
        if(trace.finishMemoryCount1997>0){
            long[] m=trace.finishMemory1997;
            text.append("\nこの撮影の補正メモリ再判定: 初期拒否 ").append(trace.finishMemoryCount1997)
                .append("回 / 最後の必要量 ");appendMiB1983(text,m[1]);
            text.append(" / 未使用CPU領域の整理: ");
            String[] status={"なし","整理を要求","CPU使用中","GPU使用中","中断","撮影中のため見送り","容量上限超過","一部失敗"};
            text.append(status[(int)m[3]]).append(" / 再判定: ").append(m[5]<2?"未実施":m[4]==1?"同じ上限内で成立":"同じ上限で不足")
                .append(" / 整理API ").append(milliseconds(m[7])).append(" ms");
            if((m[8]&1)!=0)appendMemoryOwners1997(text,"初回不足後",m,9);
            if((m[8]&2)!=0)appendMemoryOwners1997(text,"整理・再判定後",m,21);
            text.append("\n※この写真の不足判定直後の観測です。空きは既存の予備と所有領域を差し引いた予算で、物理RAM残量ではありません。");
        }
        if(trace.finishValidationSkipped1997==1)
            text.append("\n仕上げの追加検証コピー: 今回は本番補正の容量不足を優先して見送り（既存認定は維持）");
    }
    private static void appendMemoryOwners1997(StringBuilder text,String label,long[] m,int at) {
        text.append("\n  ").append(label).append(": 観測空き ");appendObservedMiB1997(text,m[at+11]);
        text.append(" / GPU保持 ");appendObservedMiB1997(text,m[at+3]);
        text.append(" / 全体経路保持 ");appendObservedMiB1997(text,m[at+4]);
        text.append(" / 旧仕上げ保持 ");appendObservedMiB1997(text,m[at+5]);
        text.append(" / CPU native合計 ");appendObservedMiB1997(text,m[at+6]);
        text.append("（Strong ");appendObservedMiB1997(text,m[at+8]);text.append("） / 未消費GPU予約 ");
        appendObservedMiB1997(text,m[at+10]);
    }
    private static void appendObservedMiB1997(StringBuilder text,long bytes) {
        if(bytes<0)text.append("未観測");else appendMiB1983(text,bytes);
    }
    private static void appendFront1996(StringBuilder text,Trace trace) {
        if(trace.frontObserved1996==0){text.append("\n前段のnative/Java・作業内訳: 未観測");return;}
        text.append("\n前段のnative/Java経路（呼出し結果）:");
        for(int i=0;i<FRONT_ROUTE_LABELS1996.length;i++) {
            if(i>0)text.append(" / ");else text.append(' ');
            text.append(FRONT_ROUTE_LABELS1996[i]).append(' ').append(trace.frontRoutes1996[i]).append("回");
        }
        text.append("\n前段の区間作業内訳（並行ワーカー累計）:");
        for(int i=0;i<FRONT_LABELS1996.length;i++) {
            text.append("\n  ").append(FRONT_LABELS1996[i]).append(": ");int at=i*2;
            if(trace.frontWork1996[at]==0)text.append("記録なし");
            else text.append(trace.frontWork1996[at]).append("回 / ")
                .append(milliseconds(trace.frontWork1996[at+1])).append(" ms");
        }
        text.append("\n※native/JavaはCPU経路の内訳です。native失敗後のJava代替は同じ区間を数えます。API経路には領域準備・再利用判定を含みます。並行作業のため前段の実経過へ加算できません。");
        text.append("\nCPU再利用候補の検証: 通常撮影の検証用コピーを省略（既存の認定は使用）");
    }
    private static void appendPipelineDetail1988(StringBuilder text,Trace trace) {
        if(!trace.pipelineObserved1988)return;
        text.append("\n処理単位別の作業累計（並行ワーカー分を合算）:");
        for(int phase=0;phase<PIPELINE_PHASES1988;phase++) {
            boolean work=false,copied=false,prior=false;
            for(int backend=0;backend<3;backend++)if(trace.pipelineWork1988[(phase*3+backend)*3]>0)work=true;
            for(int outcome=1;outcome<PIPELINE_COPIES1988;outcome++)if(trace.pipelineCopies1988[(phase*PIPELINE_COPIES1988+outcome)*4]>0)copied=true;
            for(int reason=1;reason<PIPELINE_REASONS1988;reason++)if(trace.pipelinePriorReasons1989[phase*PIPELINE_REASONS1988+reason]>0)prior=true;
            if(!work&&!copied&&!prior&&trace.pipelineQueueLast1989[phase*10]==0)continue;
            text.append('\n').append(PIPELINE_LABELS1988[phase]).append(": ");
            if(!work)text.append("出力採用は未観測");
            else {
                boolean separator=false;
                for(int backend=0;backend<3;backend++) {
                    int index=(phase*3+backend)*3;long count=trace.pipelineWork1988[index];
                    if(count==0)continue;
                    if(separator)text.append(" / ");separator=true;
                    text.append(backend==0?"経路未観測 ":backend==1?"CPU ":"GPU ").append(count).append("回 ");
                    appendPipelineTime1988(text,trace.pipelineWork1988[index+1],trace.pipelineWork1988[index+2]);
                }
                if(trace.pipelineWork1988[(phase*3+1)*3]>0) {
                    text.append(" / CPU選択理由: ");boolean separatorReason=false;
                    for(int reason=0;reason<PIPELINE_REASONS1988;reason++) {
                        long count=trace.pipelineReasons1988[phase*PIPELINE_REASONS1988+reason];if(count==0)continue;
                        if(separatorReason)text.append("、");separatorReason=true;
                        text.append(PIPELINE_REASON_LABELS1988[reason]).append(' ').append(count).append("回");
                    }
                }
            }
            if(prior){
                text.append("\n  GPU候補の切替理由: ");boolean separator=false;
                for(int reason=1;reason<PIPELINE_REASONS1988;reason++){
                    long count=trace.pipelinePriorReasons1989[phase*PIPELINE_REASONS1988+reason];if(count==0)continue;
                    if(separator)text.append("、");separator=true;
                    text.append(PIPELINE_REASON_LABELS1988[reason]).append(' ').append(count).append("回");
                }
            }
            if(phase==1)for(int shader=0;shader<2;shader++)if(trace.shaderFailures19100[shader]!=null)
                text.append("\n  GPU候補作成の実記録: ").append(shader==0?"単写 / ":"残差 / ").append(trace.shaderFailures19100[shader]);
            if(copied) {
                text.append("\n  検証入力・補助準備: ");boolean separator=false;
                for(int outcome=1;outcome<PIPELINE_COPIES1988;outcome++) {
                    int index=(phase*PIPELINE_COPIES1988+outcome)*4;long count=trace.pipelineCopies1988[index];
                    if(count==0)continue;
                    if(separator)text.append(" / ");separator=true;
                    text.append(PIPELINE_COPY_LABELS1988[outcome]).append(' ').append(count).append("回（");
                    appendPipelineTime1988(text,trace.pipelineCopies1988[index+1],trace.pipelineCopies1988[index+3]);
                    long flags=trace.pipelineCopies1988[index+3];
                    long detailed=trace.pipelineCopyQuantities1989[index];
                    if(detailed==count){
                        appendPipelineQuantity1989(text,"予約要求量",trace.pipelineCopyQuantities1989[index+1],trace.pipelineCopyQuantities1989[index+3],1,2);
                        appendPipelineQuantity1989(text,"取得済み画素量",trace.pipelineCopyQuantities1989[index+2],trace.pipelineCopyQuantities1989[index+3],4,8);
                    }else{
                        if((flags&4)!=0){text.append("、記録量 ");appendMiB1983(text,trace.pipelineCopies1988[index+2]);
                            if((flags&8)!=0)text.append("・一部未観測");}
                        else text.append("、容量未観測");
                        if(detailed>0){
                            text.append("、内訳観測 ").append(detailed).append("回");
                            appendPipelineQuantity1989(text,"予約要求量",trace.pipelineCopyQuantities1989[index+1],trace.pipelineCopyQuantities1989[index+3],1,2);
                            appendPipelineQuantity1989(text,"取得済み画素量",trace.pipelineCopyQuantities1989[index+2],trace.pipelineCopyQuantities1989[index+3],4,8);
                        }
                    }
                    text.append('）');
                }
            }
            appendPipelineQueue1989(text,trace,phase);
        }
        appendFrontProof19100(text,trace);
        text.append("\n上の工程時間は実経過、ここは作業累計です。並行処理や補助準備の時間が重複するため各行は加算できません。");
        text.append("\n記録のない処理は未観測です。検証入力の各段階は同じ入力を数える場合があり、GPU出力の採用回数ではありません。");
    }
    private static void appendPipelineQuantity1989(StringBuilder text,String label,long bytes,long flags,int observed,int unknown) {
        text.append('、').append(label).append(' ');
        if((flags&observed)==0)text.append("未観測");
        else {appendMiB1983(text,bytes);if((flags&unknown)!=0)text.append("・一部未観測");}
    }
    private static final String[] FRONT_PROOF_LABELS19100={"未観測","入力取得の判定開始","元候補の画質不一致記録",
        "同じ区間の認定あり","要求条件不正","処理中断","背景検証からの取得を抑制","コピー枠の取得見送り",
        "撮影内コピーのメモリ不足","検証キューの予約見送り","コピー開始前に取消","コピー前の予約完了",
        "入力取得結果なし","入力取得完了","取得中に撮影変更","取得中に画質不一致を記録","CPU比較条件が変化",
        "保存後の検証へ登録","取得後の検証登録見送り","入力取得中の例外","入力取得API利用不可","入力取得の割当失敗",
        "保護情報の種類が対象外"};
    private static void appendFrontProof19100(StringBuilder text,Trace trace) {
        long[] last=trace.frontProofLast19100;if(last[0]==0)return;
        text.append("\n前段GPU検証の入力取得: ");boolean separator=false;
        for(int i=1;i<FRONT_PROOF_LABELS19100.length;i++)if(trace.frontProofCounts19100[i]>0){
            if(separator)text.append(" / ");separator=true;
            text.append(FRONT_PROOF_LABELS19100[i]).append(' ').append(trace.frontProofCounts19100[i]).append("回");
        }
        text.append(" / 最後の対象: ").append(last[0]==1?"単写GPU":"残差GPU").append("・").append(FRONT_PROOF_LABELS19100[(int)last[1]]);
        separator=false;
        for(int i=0;i<trace.frontCopyCounts19100.length;i++)if(trace.frontCopyCounts19100[i]>0){
            if(!separator)text.append("\n前段検証のコピー枠判定: ");else text.append(" / ");separator=true;
            text.append(COPY_LABELS_1985[i]).append(' ').append(trace.frontCopyCounts19100[i]).append("回");
        }
        text.append("\n前段検証のメモリ見積: 保持する入力 ");
        if(last[3]<0)text.append("未観測");else appendMiB1983(text,last[3]);
        text.append(" / 撮影内コピーの追加ピーク ");
        if(last[4]<0)text.append("未観測");else appendMiB1983(text,last[4]);
        text.append(" / 保存後の実行追加ピーク ");
        if(last[5]<0)text.append("未観測");else appendMiB1983(text,last[5]);
        text.append("（コピーと実行は別時点。保存後も実行前に再判定。入力取得・予約はGPU採用を示しません）");
    }
    private static void appendPipelineQueue1989(StringBuilder text,Trace trace,int phase) {
        int at=phase*10;long[] last=trace.pipelineQueueLast1989;if(last[at]==0)return;
        text.append("\n  検証予約の判断: ");boolean separator=false;
        for(int reason=0;reason<QUEUE_LABELS_1983.length;reason++){
            long count=trace.pipelineQueueCounts1989[phase*QUEUE_LABELS_1983.length+reason];if(count==0)continue;
            if(separator)text.append("、");separator=true;
            text.append(QUEUE_LABELS_1983[reason]).append(' ').append(count).append("回");
        }
        text.append(" / 最後に記録した判断: ").append(PIPELINE_COPY_LABELS1988[(int)last[at+9]])
            .append("・").append(QUEUE_LABELS_1983[(int)last[at+1]]);
        if(last[at+2]>=0)text.append(" / 再試行まで ").append(milliseconds(last[at+2])).append(" ms");
        if(last[at+3]>=0)text.append(" / 再試行数 ").append(last[at+3]);
        if(last[at+4]>=0)text.append(" / 予約 ").append(last[at+4]).append("件");
        if(last[at+5]>=0)text.append(" / 実行中 ").append(last[at+5]).append("件");
        if(last[at+6]>=0){text.append(" / 判断時保持量 ");appendMiB1983(text,last[at+6]);}
        if(last[at+7]>=0){text.append(" / 予約要求量 ");appendMiB1983(text,last[at+7]);}
        if(last[at+8]>=0){text.append(" / 取得済み画素量 ");appendMiB1983(text,last[at+8]);}
    }
    private static void appendPipelineTime1988(StringBuilder text,long nanos,long flags) {
        if((flags&1)==0)text.append("時間未測定");
        else {text.append(milliseconds(nanos)).append(" ms");if((flags&2)!=0)text.append("（一部未測定）");}
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
    private static final String[] FINISH_SELECTION_LABELS1995={"", "実出力検証済み標準128行", "128行の画質確認未成立・32行へ", "128行の画質不一致記録・32行へ", "128行のメモリ枠不足・32行へ", "写真条件で速度認定済み", "既存32行経路", "GPU補正実行失敗"};
    private static void appendFinishSelection1995(StringBuilder text,int reason,int rows){
        text.append(FINISH_SELECTION_LABELS1995[reason]);if(rows>0)text.append("（").append(rows).append("行）");
    }
    private static final String[] BITMAP_LABELS1999={"描画受渡し","保存取得","保存所有画像","画素工程入力","ノイズ処理入力"};
    private static final String[] METADATA_REASONS1999={"受信照合の記録なし","Imageと撮影結果の時刻一致","撮影結果なし","対応する物理カメラ結果なし","Imageと撮影結果の時刻不一致","コールバック対応が変化","複数の入力時刻が競合・撮影条件は無効化","メタデータ取得失敗"};
    private static void appendBitmap1999(StringBuilder text,Trace trace) {
        text.append("保存入力の対応: 撮影ID ").append(trace.shotId>0?String.valueOf(trace.shotId):"未観測")
            .append(" / 保存ID ").append(trace.saveTicket1999>0?String.valueOf(trace.saveTicket1999):"未観測");
        String[] binding={"取得対応未観測","取得Bitmapの元撮影記録に一致","取得Bitmapに元撮影記録なし","取得Bitmapの記録が完了済み・新規記録へ分離"};
        text.append(" / ").append(binding[trace.saveBinding1999]);
        if(trace.saveOwnership1999>0)text.append(trace.saveOwnership1999==1?" / 保存専用画像を所有":" / 元画像を直列保持");
        text.append("\n撮影条件の照合: ").append(METADATA_REASONS1999[trace.metadataReason1999]);
        text.append("\n実Bitmap寸法・画素数: ");
        for(int i=0;i<5;i++){
            if(i>0)text.append(" / ");text.append(BITMAP_LABELS1999[i]).append(' ');
            int w=trace.bitmapDimensions1999[2*i],h=trace.bitmapDimensions1999[2*i+1];
            if(w<=0||h<=0)text.append("未観測");else text.append(w).append('×').append(h).append("（").append((long)w*h).append("画素）");
            if((trace.bitmapConflictMask1999&(1<<i))!=0)text.append("・複数寸法を観測");
        }
        long sensor=(long)trace.captureWidth1995*trace.captureHeight1995;
        long retrieved=(long)trace.bitmapDimensions1999[2]*trace.bitmapDimensions1999[3];
        if(sensor>0&&retrieved>0&&retrieved<sensor)text.append("\n入力画素数の差: 保存取得Bitmapは一致確認済みCamera2入力より小さい（縮小・切出し等の由来は未判定）");
        long output=(long)trace.width*trace.height;
        if(retrieved>0&&output>retrieved)text.append("\n出力との画素数比較: 保存取得 ").append(retrieved).append("画素 → 出力 ").append(output).append("画素（保存出力サイズは入力解像度を保証しません）");
    }
    private static void appendSaveLatency1990(StringBuilder text,Trace trace) {
        text.append("\n一致確認済み入力の撮影条件: ");
        if(!trace.captureMetadataObserved1995)text.append("未観測");
        else {
            text.append("ISO ").append(trace.captureIso1995>0?String.valueOf(trace.captureIso1995):"不明");
            text.append(" / 露光 ").append(trace.captureExposureNanos1995>0?String.valueOf(trace.captureExposureNanos1995)+" ns":"不明");
            String[] lens={"不明","前面","背面","超広角","望遠"};
            text.append(" / レンズ ").append(lens[trace.captureLens1995]).append(" / 入力 ").append(trace.captureWidth1995).append('×').append(trace.captureHeight1995);
        }
        text.append("\n");appendBitmap1999(text,trace);
        if(trace.captureProbeObserved1995){
            text.append("\n撮影開始時の保存後検証: ");
            if(trace.captureProbeId1995<=0)text.append("実行中なし（観測時点）");
            else text.append('#').append(trace.captureProbeId1995).append(' ').append(trace.captureProbeKind1995).append(" / ").append(trace.captureProbePhase1995)
                .append(trace.captureProbeCancelled1995?" / 中断要求済み":" / 撮影開始の中断要求前に観測").append("（撮影遅延との因果は未測定）");
        }
        if(trace.startupWideStatus1995.length()>0)text.append("\n起動時に記録した128行補正検証: ").append(trace.startupWideStatus1995);
        if(trace.captureOrdinal1990>0)text.append("\n撮影した起動内の撮影順: ").append(trace.captureOrdinal1990).append("枚目");
        text.append("\n撮影開始時の標準GPU準備: ");
        if(trace.startupReady1990)text.append("起動時の実出力検証済み");
        else if(trace.startupState1990==1)text.append("準備中");
        else if(trace.startupState1990==3)text.append("初期化済み・実出力検証未成立");
        else if(trace.startupState1990==4)text.append("利用不可");
        else text.append("未確認");
        if(trace.startupState1990>=0)text.append(" / 準備経過 ").append(milliseconds(trace.startupElapsed1990)).append(" ms");
        if(trace.startupWaitObserved1990)text.append("\n撮影処理が待ったGPU準備: ").append(milliseconds(trace.startupWaitNanos1990))
            .append(" ms / ").append(trace.startupWaitReady1990?"準備完了":"準備未成立・利用可能な経路へ切替");
        text.append("\n撮影記録開始→保存公開完了: ");
        if(trace.publicationObserved1990&&trace.publicationSucceeded1990&&trace.publishedNanos1990>=trace.started)
            text.append(milliseconds(trace.publishedNanos1990-trace.started)).append(" ms");
        else text.append(trace.publicationObserved1990?"公開失敗":"完了未観測");
        if(trace.captureOrdinal1990==1&&trace.publicationSucceeded1990&&trace.startupBegan1990>0&&trace.startupBegan1990<=trace.started)
            text.append("\nGPU初期化開始→一枚目の保存公開: ").append(milliseconds(trace.publishedNanos1990-trace.startupBegan1990)).append(" ms（撮影前の操作待ちを含む参考値）");
        appendCaptureTimeline1991(text,trace);
        appendFinishWork1992(text,trace);
        if(trace.state!=0){
            long until=trace.publicationSucceeded1990?trace.publishedNanos1990:trace.finished;
            long elapsed=Math.max(0L,until-trace.started);
            text.append("\n工程別計測に含まれない経過: ").append(milliseconds(Math.max(0L,elapsed-coverageNanos1990(trace,until)))).append(" ms");
            text.append("（撮影・標準美顔・待機など。並行工程の重複を除外）");
        }
    }
    private static void appendFinishWork1992(StringBuilder text,Trace trace) {
        boolean any=false;for(int i=0;i<FINISH_LABELS1992.length;i++)if(trace.finishWork1992[i*4]>0){any=true;break;}
        if(!any)return;
        text.append("\nGPU補正経路内の作業記録（API呼出し単位の累計）:");
        for(int i=0;i<FINISH_LABELS1992.length;i++){
            int at=i*4;long[] work=trace.finishWork1992;if(work[at]==0)continue;
            text.append("\n  ").append(FINISH_LABELS1992[i]).append(": ").append(work[at]).append("回 / ");
            appendPipelineTime1988(text,work[at+1],work[at+3]);
            text.append(" / 論理量 ");
            if((work[at+3]&4)==0)text.append("未計測");
            else {appendMiB1983(text,work[at+2]);if((work[at+3]&8)!=0)text.append("（一部未計測）");}
        }
        text.append("\n※GPU採用経路にもCPUでのポリシー生成や準備を含みます。時間は各APIの待機・転送を含む実経過の作業累計で、GPUカーネル単独の時間ではありません。失敗した試行も含み、並行・内包する区間は加算できません。論理量は各処理の対象バッファ量の累計で、実転送量・現在のメモリ使用量ではありません。");
    }
    private static void appendCaptureTimeline1991(StringBuilder text,Trace trace) {
        text.append("\nシャッターボタン受付→保存公開: ");
        if(trace.shutterDownNanos1994<=0)text.append("未計測（ボタン押下とこの撮影の同期対応は未観測）");
        else if(!trace.publicationSucceeded1990)text.append(trace.publicationObserved1990?"公開失敗":"完了未観測");
        else text.append(milliseconds(trace.publishedNanos1990-trace.shutterDownNanos1994))
            .append(" ms（ACTION_DOWN受付から・同じ同期処理の撮影）");
        text.append("\n撮影要求受付→保存公開: ");
        if(trace.requestOrigin1991==0)text.append("未計測（撮影要求の記録なし）");
        else if(!trace.publicationSucceeded1990)text.append(trace.publicationObserved1990?"公開失敗":"完了未観測");
        else {
            text.append(milliseconds(trace.publishedNanos1990-trace.events1991[REQUEST1991])).append(" ms");
            text.append(trace.requestOrigin1991==1?"（VERecorder撮影要求から）":"（ネイティブ撮影要求から・前段未観測）");
        }
        text.append("\n撮影・保存受渡しの実経過:");
        appendGap1991(text,trace,"撮影要求→ネイティブ要求",REQUEST1991,NATIVE_REQUEST1991);
        appendGap1991(text,trace,"ネイティブ要求→対応画像受信",NATIVE_REQUEST1991,IMAGE_RECEIVED1991);
        appendCameraHandoff19103(text,trace);
        appendGap1991(text,trace,"対応画像受信→描画済み画像受取",IMAGE_RECEIVED1991,BITMAP_DELIVERED1991);
        appendGap1991(text,trace,"保存準備ワーカー起動→画像取得呼出し",PREP_START1991,IMAGE_GET_START1992);
        appendGap1991(text,trace,"画像取得呼出し→取得完了（画像完成待ちを含む）",IMAGE_GET_START1992,IMAGE_GET_END1992);
        appendGap1991(text,trace,"描画済み画像受取→保存用画像取得完了",BITMAP_DELIVERED1991,IMAGE_GET_END1992);
        appendGap1991(text,trace,"画像取得後の保存準備→キュー投入開始",IMAGE_GET_END1992,QUEUE_SUBMIT1991);
        appendImageWaitOverlap1992(text,trace);
        appendGap1991(text,trace,"キュー投入開始→保存ワーカー実行",QUEUE_SUBMIT1991,WORKER_START1991);
        appendGap1991(text,trace,"保存ワーカー実行→画素処理開始",WORKER_START1991,PIPELINE_START1991);
        appendGap1991(text,trace,"保存画素処理開始→終了",PIPELINE_START1991,PIPELINE_END1991);
        appendGap1991(text,trace,"保存画素処理終了→圧縮開始",PIPELINE_END1991,ENCODE_START1991);
        appendGap1991(text,trace,"圧縮開始→完了",ENCODE_START1991,ENCODE_END1991);
        appendGap1991(text,trace,"圧縮完了→公開開始",ENCODE_END1991,PUBLISH_START1991);
        appendGap1991(text,trace,"公開開始→公開成功",PUBLISH_START1991,PUBLISH_END1991);
        text.append("\n※画像受信→描画済み画像には標準美顔・変換・受渡しが含まれます。個別の内訳は未計測です。並行する工程は加算できません。");
    }
    private static void appendCameraHandoff19103(StringBuilder text,Trace trace) {
        long begin=trace.cameraHandoff19103[0],acquired=trace.cameraHandoff19103[1],result=trace.cameraHandoff19103[2];
        text.append("\n  同じ入力の受信前内訳（アプリでの観測）:");
        appendCameraGap19103(text,"ネイティブ要求→Image取得API開始",trace.events1991[NATIVE_REQUEST1991],begin);
        appendCameraGap19103(text,"Image取得API開始→取得完了",begin,acquired);
        appendCameraGap19103(text,"Image取得完了→対応画像受信",acquired,trace.events1991[IMAGE_RECEIVED1991]);
        appendCameraGap19103(text,"Image取得完了→所有権guard開始",acquired,trace.cameraPhases19104[0]);
        appendCameraGap19103(text,"所有権guard開始→完了",trace.cameraPhases19104[0],trace.cameraPhases19104[1]);
        appendCameraGap19103(text,"guard完了→Q0入場（State同期・入力受渡し等）",trace.cameraPhases19104[1],trace.cameraPhases19104[2]);
        appendCameraGap19103(text,"Q0入場→対応画像受信",trace.cameraPhases19104[2],trace.events1991[IMAGE_RECEIVED1991]);
        if(trace.cameraPhases19104[2]>0)text.append("\n    単写のQ0入場: 追加撮影・融合準備を実行せず元の入力受渡しへ継続");
        appendCameraGap19103(text,"ネイティブ要求→撮影結果通知",trace.events1991[NATIVE_REQUEST1991],result);
        appendCameraGap19103(text,"撮影結果通知→対応画像受信",result,trace.events1991[IMAGE_RECEIVED1991]);
        if(acquired>0&&result>0) {
            if(acquired>=result)appendCameraGap19103(text,"撮影結果通知→Image取得完了",result,acquired);
            else appendCameraGap19103(text,"Image取得完了→撮影結果通知",acquired,result);
        }
        text.append("\n    Camera2撮影開始通知: 未観測（露光開始までの待ちと撮像処理は未分離）");
        text.append("\n    ※取得と結果通知は並行するため重複加算不可。結果通知はStateへの格納完了時点ではありません。センサー時刻は入力照合だけに使用し、アプリの時計との差分は計算しません。");
    }
    private static long cameraGap19103(long from,long to) {return from>0&&to>=from?to-from:-1;}
    private static void appendCameraGap19103(StringBuilder text,String label,long from,long to) {
        text.append("\n    ").append(label).append(": ");
        if(from<=0||to<=0)text.append("未観測");
        else if(to<from)text.append("順序外（差分は加算不可）");
        else text.append(milliseconds(to-from)).append(" ms");
    }
    private static void appendImageWaitOverlap1992(StringBuilder text,Trace trace) {
        long prep=trace.events1991[PREP_START1991],delivered=trace.events1991[BITMAP_DELIVERED1991];
        if(prep>0&&delivered>prep)text.append("\n  保存準備ワーカーは描画済み画像受取の ")
            .append(milliseconds(delivered-prep)).append(" ms 前に起動（画像完成前の起動は正常です）");
        long getStart=trace.events1991[IMAGE_GET_START1992],getEnd=trace.events1991[IMAGE_GET_END1992];
        long received=trace.events1991[IMAGE_RECEIVED1991];
        if(getStart>0&&getEnd>=getStart&&received>0&&delivered>=received){
            long overlap=Math.max(0L,Math.min(getEnd,delivered)-Math.max(getStart,received));
            text.append("\n  画像取得呼出しと画像受信→描画完了の重複: ").append(milliseconds(overlap))
                .append(" ms（同じ実時間のため再加算不可）");
        }
    }
    private static void appendGap1991(StringBuilder text,Trace trace,String label,int from,int to) {
        long a=trace.events1991[from],b=trace.events1991[to];
        text.append("\n  ").append(label).append(": ");
        if(a==0||b==0)text.append("未計測");
        else if(b<a)text.append("並行・順序外（差分は加算不可）");
        else text.append(milliseconds(b-a)).append(" ms");
    }
    /** Five-way union of already sorted stage intervals, clipped to this trace.
     * Uses fixed indices; no full per-pixel logging or live GPU queries. */
    static long coverageNanos1990(Trace trace,long until) {
        if(trace==null||until<=trace.started)return 0;
        int[] next=new int[5];long total=0,start=0,end=0;boolean have=false;
        for(;;){
            int selected=-1;long earliest=Long.MAX_VALUE;
            for(int i=0;i<5;i++)if(next[i]<trace.stages[i].intervals.size()){
                long value=trace.stages[i].intervals.get(next[i]).start;
                if(value<earliest){earliest=value;selected=i;}
            }
            if(selected<0)break;
            Interval value=trace.stages[selected].intervals.get(next[selected]++);
            long a=Math.max(trace.started,value.start),b=Math.min(until,value.end);
            if(b<=a)continue;
            if(!have){start=a;end=b;have=true;}
            else if(a<=end)end=Math.max(end,b);
            else {total+=end-start;start=a;end=b;}
        }
        return total+(have?end-start:0);
    }
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
