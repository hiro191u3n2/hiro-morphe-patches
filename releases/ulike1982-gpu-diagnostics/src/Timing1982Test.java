package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** All 227 inherited timing assertions, with only new display expectations,
 * plus .82 diagnostics regressions against the actual production helper. */
public final class Timing1982Test {
    static int assertions;
    static final ArrayList<String> tests=new ArrayList<String>();
    interface Case {void run() throws Exception;}
    static void check(boolean condition,String reason) {
        assertions++;if(!condition)throw new AssertionError(reason);
    }
    static Field field(String name) throws Exception {
        Field result=ProcessingTiming1947.class.getDeclaredField(name);result.setAccessible(true);return result;
    }
    static void reset() throws Exception {
        ProcessingTiming1947.enter(null);
        for(String name:new String[]{"TRACES","KEYS","VIEWS"})((ArrayList<?>)field(name).get(null)).clear();
        field("latest").set(null,null);field("persisted").set(null,null);field("preferences").set(null,null);
        field("loaded").setBoolean(null,false);field("displayedId").setLong(null,0);
        field("nextId").setLong(null,1000);field("captureEpoch1953").setLong(null,0);
        GpuQualification1961.status="GPU資格: 未確認";GpuQualification1961.statusCalls=0;GpuQualification1961.wakeStatus=null;GpuQualification1961.captureChanges=0;GpuQualification1961.throwStatus=false;
    }
    static String render(ProcessingTiming1947.Trace trace) throws Exception {
        Method render=ProcessingTiming1947.class.getDeclaredMethod("render",ProcessingTiming1947.Trace.class);
        render.setAccessible(true);return (String)render.invoke(null,trace);
    }
    static void run(String name,Case test) throws Exception {reset();test.run();tests.add(name);System.out.println("PASS "+name);}

    static void workerIdentity() throws Exception {
        final Object original=new Object(),transformed=new Object(),unrelated=new Object();
        final ProcessingTiming1947.Trace owner=ProcessingTiming1947.begin(original);
        final ProcessingTiming1947.Trace wrong=ProcessingTiming1947.begin(unrelated);
        final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        Thread worker=new Thread(new Runnable(){public void run(){try{
            ProcessingTiming1947.enter(wrong);
            check(ProcessingTiming1947.current()==wrong,"fixture has wrong current worker trace");
            check(ProcessingTiming1947.traceFor(original)==owner,"bitmap identity beats unrelated worker scope");
            ProcessingTiming1947.transfer(original,transformed);
            check(ProcessingTiming1947.traceFor(transformed)==owner,"transformation retains exact owner");
            ProcessingTiming1947.noiseBackend(owner,2,3);
        }catch(Throwable failure){error.set(failure);}}});
        worker.start();worker.join(10000);check(!worker.isAlive(),"worker completed");
        if(error.get()!=null)throw new AssertionError(error.get());
        check(owner.strongGpuStrips==2&&owner.strongCpuStrips==3,"explicit owner receives backend counters");
        check(wrong.strongGpuStrips==0&&wrong.strongCpuStrips==0,"unrelated current worker receives no counts");
        check(render(owner).contains("強ノイズ処理: GPU 2区間 / CPU 3区間"),"counts render accurately");
        check(ProcessingTiming1947.summary().contains("強ノイズ処理: 未観測（GPU使用は未判定）"),"older owner cannot replace newer displayed shot with its backend counts");
    }
    static void concurrentCounts() throws Exception {
        final ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        final int count=8,iterations=250;
        final CountDownLatch ready=new CountDownLatch(count),start=new CountDownLatch(1);
        final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        Thread[] workers=new Thread[count];
        for(int i=0;i<count;i++){workers[i]=new Thread(new Runnable(){public void run(){try{
            ready.countDown();start.await();for(int n=0;n<iterations;n++)ProcessingTiming1947.noiseBackend(trace,1,2);
        }catch(Throwable failure){error.set(failure);}}});workers[i].start();}
        ready.await();start.countDown();for(Thread worker:workers){worker.join(10000);check(!worker.isAlive(),"count worker terminated");}
        if(error.get()!=null)throw new AssertionError(error.get());
        check(trace.strongGpuStrips==count*iterations,"concurrent GPU increments are never lost");
        check(trace.strongCpuStrips==2*count*iterations,"concurrent CPU increments are never lost");
        check(ProcessingTiming1947.summary().contains("強ノイズ処理: GPU 2000区間 / CPU 4000区間"),"summary contains completed concurrent counts");
    }
    static void invalidInputs() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(null,1,1);ProcessingTiming1947.noiseBackend(trace,-1,3);
        ProcessingTiming1947.noiseBackend(trace,3,-1);ProcessingTiming1947.noiseBackend(trace,0,0);
        check(trace.strongGpuStrips==0&&trace.strongCpuStrips==0,"invalid deltas make no partial update");
        check(ProcessingTiming1947.summary().contains("強ノイズ処理: 未観測（GPU使用は未判定）"),"absence of execution evidence remains explicitly unknown");
        check(!ProcessingTiming1947.summary().contains("保存時の"),"qualification is not invented before completion");
    }
    static void terminalSnapshot() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(trace,3,5);
        ProcessingTiming1947.strongGpuReason1970(trace,"memory_budget",5);
        ProcessingTiming1947.strongGpuVerification1970(trace,4);
        ProcessingTiming1947.strongGpuTrial1971(trace,"argb_mismatch",3);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,0,5712,320,4,7,1L);
        ProcessingTiming1947.strongGpuWork1973(trace,8000000L,6000000L,1000000L,2);
        ProcessingTiming1947.strongGpuMismatch1973(trace,17L,2,3L,5L);
        GpuQualification1961.status="GPU資格: 認定済み（現在）";GpuQualification1961.wakeStatus="GPU資格: 次の撮影で検査中";
        ProcessingTiming1947.finish(trace,true);
        String saved=ProcessingTiming1947.summary();long updated=trace.updatedMillis;
        check(trace.state==1,"successful shot becomes terminal");
        check(saved.contains("保存時のGPU資格: 認定済み"),"qualification snapshots before wake changes global status");
        check(!saved.contains("（現在）"),"saved qualification does not contradict its historical snapshot label");
        check(GpuQualification1961.status.equals("GPU資格: 次の撮影で検査中"),"fixture changed live qualification after save");
        ProcessingTiming1947.noiseBackend(trace,17,19);ProcessingTiming1947.finish(trace,false);
        ProcessingTiming1947.strongGpuReason1970(trace,"quality_rejected",99);
        ProcessingTiming1947.strongGpuVerification1970(trace,99);
        ProcessingTiming1947.strongGpuTrial1971(trace,"confidence_mismatch",99);
        ProcessingTiming1947.strongGpuFailure1971(trace,"confidence_mismatch",37,1,5712,320,8,9,99L);
        ProcessingTiming1947.strongGpuWork1973(trace,99000000L,99000000L,99000000L,99);
        ProcessingTiming1947.strongGpuMismatch1973(trace,99L,99,99L,99L);
        ProcessingTiming1947.note(trace,"late mutation");ProcessingTiming1947.settings(trace,"late settings");
        ProcessingTiming1947.output(trace,99,99,"late output");
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,0,999000000L);
        check(ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.NOISE)==null,"terminal stage cannot restart");
        check(trace.strongGpuStrips==3&&trace.strongCpuStrips==5,"late worker backend counts ignored");
        check(trace.strongGpuReasons.size()==1&&trace.strongGpuReasons.get("memory_budget")==5L
            &&trace.strongCpuVerifications==4,"late worker reason and verification updates ignored");
        check(GpuQualification1961.statusCalls==1,"repeat finish does not resnapshot qualification");
        check(trace.updatedMillis==updated,"late operations preserve saved timestamp");
        check(saved.equals(ProcessingTiming1947.summary()),"terminal summary remains byte stable");
        check(trace.strongGpuTrials.size()==1&&trace.strongGpuTrials.get("argb_mismatch")==3L,
            "late trial observations cannot alter terminal counts");
        check(trace.strongCpuOracleNanos==8000000L&&trace.strongGpuRequestNanos==6000000L&&trace.strongWaitNanos==1000000L
            &&trace.strongOracleReuses==2,"late latency and reference reuse updates cannot alter terminal work totals");
        check(trace.strongArgbMismatchCount==17L&&trace.strongArgbMaxDelta==2&&trace.strongConfidenceMismatchCount==3L
            &&trace.strongConfidenceMaxDelta==5L,"late full comparison scalars cannot alter terminal first-candidate evidence");
    }
    static void failedTerminal() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(trace,0,7);GpuQualification1961.status="GPU資格: 保留";
        ProcessingTiming1947.strongGpuReason1970(trace,"shader_unsupported",7);
        ProcessingTiming1947.strongGpuVerification1970(trace,2);
        ProcessingTiming1947.strongGpuTrial1971(trace,"submit_failed",1);
        ProcessingTiming1947.strongGpuFailure1971(trace,"submit_failed",38,2,5712,320,-1,-1,-1L);
        ProcessingTiming1947.strongGpuWork1973(trace,7000000L,0L,-1L,0);
        ProcessingTiming1947.strongGpuMismatch1973(trace,-1L,-1,3L,2L);
        ProcessingTiming1947.finish(trace,false);String saved=ProcessingTiming1947.summary();
        check(trace.state==2&&saved.contains(" / エラー"),"failed shot becomes terminal error");
        check(saved.contains("GPU 0区間 / CPU 7区間"),"CPU-only execution shown explicitly");
        GpuQualification1961.status="GPU資格: 後から認定済み";
        ProcessingTiming1947.noiseBackend(trace,4,4);ProcessingTiming1947.finish(trace,true);
        ProcessingTiming1947.strongGpuReason1970(trace,"unknown",8);ProcessingTiming1947.strongGpuVerification1970(trace,8);
        ProcessingTiming1947.strongGpuTrial1971(trace,"submit_failed",8);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,0,5712,320,4,7,1L);
        ProcessingTiming1947.strongGpuWork1973(trace,99000000L,99000000L,99000000L,99);
        ProcessingTiming1947.strongGpuMismatch1973(trace,99L,99,99L,99L);
        check(saved.equals(ProcessingTiming1947.summary()),"failed result cannot be rewritten by late success");
    }
    static final class MemoryPreferences implements SharedPreferences {
        final Map<String,Object> values=new HashMap<String,Object>();int saves;
        public synchronized long getLong(String key,long fallback){Object value=values.get(key);return value instanceof Long?(Long)value:fallback;}
        public synchronized String getString(String key,String fallback){Object value=values.get(key);return value instanceof String?(String)value:fallback;}
        public Editor edit(){return new Editor(){final Map<String,Object> staged=new HashMap<String,Object>();
            public Editor putString(String key,String value){staged.put(key,value);return this;}
            public Editor putLong(String key,long value){staged.put(key,value);return this;}
            public void apply(){synchronized(MemoryPreferences.this){values.putAll(staged);saves++;}}
        };}
    }
    static final class MemoryContext extends Context {
        final MemoryPreferences preferences;MemoryContext(MemoryPreferences value){preferences=value;}
        public SharedPreferences getSharedPreferences(String name,int mode){return preferences;}
    }
    static void persistence() throws Exception {
        MemoryPreferences preferences=new MemoryPreferences();ProcessingTiming1947.init(new MemoryContext(preferences));
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(trace,9,2);GpuQualification1961.status="GPU資格: 撮影時の状態";
        ProcessingTiming1947.strongGpuReason1970(trace,"execution_failed",2);
        ProcessingTiming1947.strongGpuVerification1970(trace,11);
        ProcessingTiming1947.strongGpuReason1970(trace,"cached_legacy_negative",1);
        ProcessingTiming1947.strongGpuTrial1971(trace,"argb_mismatch",4);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",37,1,5712,320,6,13,1L);
        ProcessingTiming1947.strongGpuWork1973(trace,11000000L,9000000L,3000000L,4);
        ProcessingTiming1947.strongGpuMismatch1973(trace,5L,3,2L,7L);
        ProcessingTiming1947.finish(trace,true);String saved=ProcessingTiming1947.summary();
        check(preferences.saves==1,"result saved exactly once");
        check(saved.equals(preferences.getString("summary","")),"stored summary matches terminal result");
        check(preferences.getString("version","").equals(ProcessingTiming1947.VERSION),"storage version tracks real helper");
        check(saved.contains("GPU実行失敗 2区間")&&saved.contains("CPU照合 11回"),"terminal storage includes actual fallback and CPU verification counters");
        check(saved.contains("保存済み拒否（旧記録・原因不明） 1区間")&&saved.contains("ARGB不一致 4試行")
            &&saved.contains("位置=6,13 / 観測箇所の絶対差=1"),"saved diagnostics retain cached cause, attempt count and first structural mismatch");
        check(saved.contains("強ノイズCPU経路・照合作業累計: 11 ms")&&saved.contains("GPU準備・転送・実行・読戻し作業累計: 9 ms")
            &&saved.contains("GPU枠内の待機・判定作業累計: 3 ms")&&saved.contains("追加照合に再利用: 4回"),
            "work totals and reference reuse survive the same immutable summary persistence");
        check(saved.contains("ARGB不一致 5画素 / 最大チャンネル差 3")&&saved.contains("同じ候補の信頼度: 不一致 2点 / 最大差 7"),
            "independent full ARGB and confidence statistics persist with the same first candidate");
        ProcessingTiming1947.noiseBackend(trace,90,20);ProcessingTiming1947.finish(trace,false);
        check(preferences.saves==1,"late operations never overwrite persisted result");
        reset();GpuQualification1961.status="GPU資格: 再起動後の別状態";
        ProcessingTiming1947.init(new MemoryContext(preferences));
        check(saved.equals(ProcessingTiming1947.summary()),"restart restores exact saved backend and qualification snapshot");
        check(GpuQualification1961.statusCalls==0,"reading saved result never uses live qualification");
    }
    static void unionDuration() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());long ms=1000000L;
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,0,10*ms);
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,5*ms,20*ms);
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,7*ms,9*ms);
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,30*ms,40*ms);
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,40*ms,50*ms);
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.CORRECTION,0,60*ms);
        ProcessingTiming1947.noiseBackend(trace,1,2);
        check(ProcessingTiming1947.summary().contains("ノイズ除去: 40 ms"),"overlap/nesting union counts once and excludes 10ms gap");
        check(ProcessingTiming1947.summary().contains("補正: 60 ms"),"different stages retain separate timing scopes");
        check(!ProcessingTiming1947.summary().contains("ノイズ除去: 54 ms"),"not additive worker interval duration");
    }
    static void liveTokens() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.Token outer=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.NOISE);
        ProcessingTiming1947.Token nested=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.NOISE);
        check(ProcessingTiming1947.summary().contains("ノイズ除去: 処理中"),"live overlapping stages report active state");
        ProcessingTiming1947.end(nested);ProcessingTiming1947.end(nested);
        check(ProcessingTiming1947.summary().contains("ノイズ除去: 処理中"),"ending nested token twice cannot consume outer token");
        ProcessingTiming1947.end(outer);
        check(!ProcessingTiming1947.summary().contains("ノイズ除去: 処理中"),"drained tokens finalize actual duration");
        check(!ProcessingTiming1947.summary().contains("ノイズ除去: 未計測"),"ended work remains measured");
    }
    static void initialWait() throws Exception {
        String[] lines=ProcessingTiming1947.summary().split("\n");
        check(lines[0].contains("ULike v1.9.82 / 新しい撮影の計測待ち"),"initial summary uses follow-up version and waits for a fresh shot");
        check(lines[1].equals("強ノイズ処理: 計測待ち（新しい撮影後に判定）"),"initial backend state is visible directly below header");
        check(!ProcessingTiming1947.summary().contains("GPU 0区間"),"initial absence cannot imply measured zero GPU work");
        check(lines[2].equals("強ノイズGPU方針: 画質・速度条件を満たす候補を採用。追加認定は保存後"),"initial UI exposes policy without claiming a completed GPU result");
        check(!ProcessingTiming1947.summary().contains("候補失敗"),"initial wait does not invent any failed candidate");
    }
    /** A Material Preference may show only ten lines. Verify status placement
     * using a realistic long settings record; this is a host order/budget test,
     * not a physical Android typography or screenshot assertion. */
    static void firstScreenBudget() throws Exception {
        String requested="ノイズ低減:4／くっきり補正:4／質感保護:オン／白フチ抑制:オン／暗部優先:オン";
        int[][] variants={{0,0},{0,48},{31,17},{48,0}};
        for(int[] variant:variants){
            ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
            ProcessingTiming1947.settings(trace,requested);ProcessingTiming1947.output(trace,5712,4284,"画質補正");
            ProcessingTiming1947.skip(trace,ProcessingTiming1947.FUSION);
            long ms=1000000L;
            ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,0,2372*ms);
            ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.CORRECTION,0,820*ms);
            ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.ENCODE,0,183*ms);
            ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.SAVE,0,85*ms);
            ProcessingTiming1947.noiseBackend(trace,variant[0],variant[1]);
            if(variant[1]>0)ProcessingTiming1947.strongGpuReason1970(trace,"session_unavailable",variant[1]);
            ProcessingTiming1947.strongGpuVerification1970(trace,variant[0]+variant[1]);
            GpuQualification1961.status="GPU資格: 保存時に確認された状態（現在）";ProcessingTiming1947.finish(trace,true);
            String summary=ProcessingTiming1947.summary();String[] lines=summary.split("\n");
            String expected=variant[0]+variant[1]==0?"強ノイズ処理: 未観測（GPU使用は未判定）":
                "強ノイズ処理: GPU "+variant[0]+"区間 / CPU "+variant[1]+"区間"+(variant[0]>0?"（GPU使用あり）":"（今回はGPU出力の採用なし）");
            check(lines[1].equals(expected),"unknown/CPU/mixed/GPU observation remains on second logical line");
            check(lines[2].equals("強ノイズGPU方針: 画質・速度条件を満たす候補を採用。追加認定は保存後"),"policy remains directly after selected-output counters");
            check(summary.contains("CPU照合 "+(variant[0]+variant[1])+"回"),"CPU oracle work remains honest even for GPU-only selected output");
            check(summary.contains("GPU出力でも画質確認にCPU照合を使う場合があります"),"selected GPU output never implies every calculation used the GPU");
            check(summary.indexOf(expected)<summary.indexOf(requested),"long wrapped settings cannot push backend status below them");
            check(summary.contains(requested)&&summary.contains("出力: 5712×4284 / 画質補正"),"full settings and dimensions remain intact");
            check(summary.contains("ノイズ除去: 2372 ms")&&summary.contains("補正: 820 ms")
                &&summary.contains("圧縮: 183 ms")&&summary.contains("保存: 85 ms"),"moving status preserves all actual stage times");
            check(summary.contains("全体経過:")&&summary.contains("標準美顔処理・撮影待機"),"unabridged timing explanation remains available");
            check(summary.contains("保存時のGPU資格: 保存時に確認された状態")&&!summary.contains("（現在）"),"historical qualification remains a separate saved-state record");
            check(summary.indexOf("保存時のGPU資格:")>summary.indexOf("保存: 85 ms"),"historical qualification is retained after stage measurements");
            int appearances=0;for(String line:lines)if(line.startsWith("強ノイズ処理:"))appearances++;
            check(appearances==1,"backend row is neither duplicated nor left hidden at old position");
            // The shortest ten logical lines already contain the backend. A
            // narrow wrap budget cannot change its order relative to settings.
            String firstTen=String.join("\n",java.util.Arrays.copyOfRange(lines,0,Math.min(10,lines.length)));
            check(firstTen.contains(expected),"ten-line summary budget includes the observation row");
        }
    }
    static void boundedReasons() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        String[] codes={"awaiting_quality","quality_rejected","shader_unsupported","memory_budget",
            "session_unavailable","bank_busy","upload_failed","execution_failed","readback_failed",
            "policy_mismatch","cancelled","unknown"};
        ProcessingTiming1947.strongGpuReason1970(null,"unknown",1);
        ProcessingTiming1947.strongGpuReason1970(trace,null,1);
        ProcessingTiming1947.strongGpuReason1970(trace,"memory_budget",0);
        ProcessingTiming1947.strongGpuReason1970(trace,"memory_budget",-1);
        ProcessingTiming1947.strongGpuVerification1970(null,1);
        ProcessingTiming1947.strongGpuVerification1970(trace,0);
        ProcessingTiming1947.strongGpuVerification1970(trace,-1);
        check(trace.strongGpuReasons.isEmpty()&&trace.strongCpuVerifications==0,"null/nonpositive telemetry never invents events");
        for(String code:codes)ProcessingTiming1947.strongGpuReason1970(trace,code,1);
        ProcessingTiming1947.strongGpuReason1970(trace,"quality_rejected",2);
        for(int i=0;i<100;i++)ProcessingTiming1947.strongGpuReason1970(trace,"secret-image-or-key-"+i,1);
        ProcessingTiming1947.strongGpuReason1970(trace," memory_budget ",1);
        check(trace.strongGpuReasons.size()==12,"all allowed reasons occupy exactly twelve bounded slots");
        check(trace.strongGpuReasons.get("quality_rejected")==3L,"repeated fixed reasons aggregate instead of creating slots");
        String summary=ProcessingTiming1947.summary();
        check(!summary.contains("secret-image-or-key-"),"untrusted keys/images never reach diagnostic output");
        check(summary.contains("画質確認・実行失敗 3区間")&&!summary.contains("画素不一致"),"failure caption does not claim proven pixel inequality");
        check(summary.contains("保護情報不一致 1区間")&&summary.contains("理由未判定 1区間"),"distinct bounded reason categories retain their counts");
        check(trace.strongGpuStrips==0&&trace.strongCpuStrips==0,"reason metadata does not change output-selection counters");
        String[] newCodes={"cached_legacy_negative","cached_argb_negative","cached_confidence_negative","cached_policy_negative",
            "cache_limit_negative","new_argb_mismatch","new_confidence_mismatch","policy_failure",
            "submit_failed","output_shape_failed","native_failure_unknown"};
        for(String code:newCodes)ProcessingTiming1947.strongGpuReason1970(trace,code,1);
        check(trace.strongGpuReasons.size()==23&&trace.strongGpuReasons.size()<=24,"all CPU fallback categories retain a fixed bounded list");
        summary=ProcessingTiming1947.summary();
        check(summary.contains("保存済み拒否（旧記録・原因不明） 1区間")&&summary.contains("今回のARGB不一致 1区間"),
            "old unknown negative and freshly observed ARGB inequality remain distinct");
        check(summary.contains("今回の信頼度不一致 1区間")&&summary.contains("GPU内部失敗（詳細未取得） 1区間"),
            "confidence inequality and unclassified runtime failures never imply an unobserved GL code");
        for(String code:new String[]{"cold_proof_budget","proof_in_flight","cold_bank_busy","proof_already_attempted"})
            ProcessingTiming1947.strongGpuReason1970(trace,code,1);
        check(trace.strongGpuReasons.size()==27,"all new cold-proof fallback reasons are still fixed and bounded");
        summary=ProcessingTiming1947.summary();
        check(summary.contains("今回のGPU検証枠上限 1区間")&&summary.contains("同じ条件のGPU画質確認中 1区間")
            &&summary.contains("初回検証用GPU処理枠不足 1区間")&&summary.contains("同じ条件は今回すでに検証 1区間"),
            "budget, another proof worker, bank availability and previously attempted condition remain distinct");
    }
    static void trialsAndStructuralSnapshot() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        String[] kinds={"argb_mismatch","confidence_mismatch","policy_failure","submit_failed","readback_failed",
            "output_shape_failed","upload_failed","execution_failed","native_failure_unknown"};
        ProcessingTiming1947.strongGpuTrial1971(null,"argb_mismatch",1);
        ProcessingTiming1947.strongGpuTrial1971(trace,null,1);
        ProcessingTiming1947.strongGpuTrial1971(trace,"argb_mismatch",0);
        ProcessingTiming1947.strongGpuTrial1971(trace,"argb_mismatch",-1);
        for(int i=0;i<100;i++)ProcessingTiming1947.strongGpuTrial1971(trace,"raw-pixels-or-secret-key-"+i,1);
        check(trace.strongGpuTrials.isEmpty(),"untrusted and invalid trial events occupy no slots");
        for(String kind:kinds)ProcessingTiming1947.strongGpuTrial1971(trace,kind,1);
        ProcessingTiming1947.strongGpuTrial1971(trace,"argb_mismatch",5);
        ProcessingTiming1947.noiseBackend(trace,0,2);
        ProcessingTiming1947.strongGpuReason1970(trace,"new_argb_mismatch",2);
        check(trace.strongGpuTrials.size()==kinds.length&&trace.strongGpuTrials.get("argb_mismatch")==6L,
            "trial categories remain fixed and repeated failed layouts aggregate as trials");
        String before=ProcessingTiming1947.summary();
        check(before.contains("GPU 0区間 / CPU 2区間")&&before.contains("今回のARGB不一致 2区間")&&before.contains("ARGB不一致 6試行"),
            "six failed attempts cannot be mistaken for six CPU output strips");
        check(!before.contains("最初のGPU候補失敗:"),"candidate counter alone cannot invent structural metadata");
        ProcessingTiming1947.strongGpuFailure1971(trace,"raw-pixel",36,0,5712,320,3,4,1L);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",64,0,5712,320,3,4,1L);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,3,5712,320,3,4,1L);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,0,-1,320,3,4,1L);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,0,5712,320,5712,4,1L);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,0,5712,320,3,320,1L);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,0,5712,320,-1,4,1L);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,0,5712,320,3,4,-2L);
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,0,5712,320,3,4,4294967296L);
        check(trace.strongGpuFirstFailure.isEmpty(),"invalid structural fields cannot claim the first failure slot");
        ProcessingTiming1947.strongGpuFailure1971(trace,"confidence_mismatch",38,2,5712,320,3,4,4294967295L);
        String first=trace.strongGpuFirstFailure;
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,0,5712,320,1,1,1L);
        check(first.equals(trace.strongGpuFirstFailure),"subsequent failure cannot replace the first observation");
        check(first.contains("信頼度不一致 / program=38 / layout=2 / 幅=5712 / 区間行数=320")
            &&first.contains("位置=3,4 / 観測箇所の絶対差=4294967295"),"only bounded structural scalars and long-safe absolute difference survive");
        String summary=ProcessingTiming1947.summary();
        check(summary.contains("GPU候補失敗の試行数はCPU区間数とは別")&&summary.contains("絶対差は観測箇所のみ"),
            "details explain separate denominators and never present one difference as image maximum");
        check(!summary.contains("raw-pixels-or-secret-key-")&&!summary.contains("raw-pixel"),"arbitrary image/key strings never enter diagnostic persistence");
        ProcessingTiming1947.Trace unknown=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.strongGpuFailure1971(unknown,"native_failure_unknown",-1,-1,0,0,-1,-1,-1L);
        check(ProcessingTiming1947.summary().contains("内部失敗（詳細未取得） / program=-1")
            &&ProcessingTiming1947.summary().contains("-1は未取得です"),"unavailable metadata stays explicitly unavailable");
    }
    static void boundedWorkTelemetry() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        check(!ProcessingTiming1947.summary().contains("強ノイズCPU経路・照合作業累計:"),"missing time telemetry remains absent instead of invented zero work");
        ProcessingTiming1947.strongGpuWork1973(null,1,1,1,1);
        ProcessingTiming1947.strongGpuWork1973(trace,-1,-1,-1,0);
        ProcessingTiming1947.strongGpuWork1973(trace,-2,1,1,1);
        ProcessingTiming1947.strongGpuWork1973(trace,1,-2,1,1);
        ProcessingTiming1947.strongGpuWork1973(trace,1,1,-2,1);
        ProcessingTiming1947.strongGpuWork1973(trace,1,1,1,-1);
        check(!trace.strongCpuOracleMeasured&&!trace.strongGpuRequestMeasured&&!trace.strongWaitMeasured&&trace.strongOracleReuses==0,
            "invalid work inputs make no partial update or invented observations");
        ProcessingTiming1947.strongGpuWork1973(trace,-1,0,-1,0);
        String summary=ProcessingTiming1947.summary();
        check(summary.contains("強ノイズCPU経路・照合作業累計: 未計測")&&summary.contains("GPU準備・転送・実行・読戻し作業累計: 0 ms")
            &&summary.contains("GPU枠内の待機・判定作業累計: 未計測"),"unknown durations differ from an explicitly measured zero");
        check(!summary.contains("追加照合に再利用:"),"zero reuse remains hidden and cannot imply avoided CPU reference calls");
        ProcessingTiming1947.noiseBackend(trace,0,16);long ms=1000000L;
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,0,2919L*ms);
        ProcessingTiming1947.strongGpuWork1973(trace,6000L*ms,400L*ms,80L*ms,3);
        ProcessingTiming1947.strongGpuWork1973(trace,2400L*ms,500L*ms,20L*ms,1);
        summary=ProcessingTiming1947.summary();
        check(summary.contains("GPU 0区間 / CPU 16区間")&&summary.contains("強ノイズCPU経路・照合作業累計: 8400 ms")
            &&summary.contains("GPU準備・転送・実行・読戻し作業累計: 900 ms")&&summary.contains("GPU枠内の待機・判定作業累計: 100 ms"),
            "CPU final output still reports actual GPU candidate work and sums worker work separately");
        check(summary.contains("ノイズ除去: 2919 ms")&&summary.contains("並行ワーカー分とCPU・GPU同時処理分が重複し、工程時間とは一致しません")
            &&summary.contains("GPU最終出力が0区間でも候補のGPU実行時間を含む場合があります"),
            "work sum never changes stage union or implies final output selected the GPU");
        check(summary.contains("追加照合に再利用: 4回"),"additional same-strip reference reuse aggregates as a count");
        check(summary.contains("GPU要求の累計には並行したCPU照合の時間を含む場合があります"),"overlapped GPU request time explains CPU overlap without claiming GPU-only duration");
        ProcessingTiming1947.strongGpuWork1973(trace,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Integer.MAX_VALUE);
        ProcessingTiming1947.strongGpuWork1973(trace,1,1,1,Integer.MAX_VALUE);
        check(trace.strongCpuOracleNanos==Long.MAX_VALUE&&trace.strongGpuRequestNanos==Long.MAX_VALUE&&trace.strongWaitNanos==Long.MAX_VALUE,
            "bounded scalar work accumulators saturate instead of becoming negative on overflow");
        check(trace.strongOracleReuses==4L+2L*Integer.MAX_VALUE,"reference reuse counts retain long-safe arithmetic");
    }
    static void fullMismatchScalars() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.strongGpuMismatch1973(null,1L,1,1L,1L);
        ProcessingTiming1947.strongGpuMismatch1973(trace,-1L,-1,-1L,-1L);
        ProcessingTiming1947.strongGpuMismatch1973(trace,-2L,-1,1L,1L);
        ProcessingTiming1947.strongGpuMismatch1973(trace,1L,-1,1L,1L);
        ProcessingTiming1947.strongGpuMismatch1973(trace,1L,256,1L,1L);
        ProcessingTiming1947.strongGpuMismatch1973(trace,0L,1,1L,1L);
        ProcessingTiming1947.strongGpuMismatch1973(trace,1L,1,0L,1L);
        ProcessingTiming1947.strongGpuMismatch1973(trace,1L,1,1L,4294967296L);
        ProcessingTiming1947.strongGpuMismatch1973(trace,1L,1,(long)Integer.MAX_VALUE+1L,1L);
        check(!trace.strongMismatchMeasured&&trace.strongArgbMismatchCount== -1L&&trace.strongConfidenceMismatchCount== -1L,
            "unavailable or invalid scalar pairs cannot consume the first full comparison slot");
        check(!ProcessingTiming1947.summary().contains("最初の失敗候補（全比較領域）:"),"absence of a full scan is not represented as a full-image maximum");
        ProcessingTiming1947.strongGpuFailure1971(trace,"argb_mismatch",36,0,5712,320,3,4,1L);
        ProcessingTiming1947.strongGpuMismatch1973(trace,23000L,17,123L,4294967295L);
        String summary=ProcessingTiming1947.summary();
        check(summary.contains("位置=3,4 / 観測箇所の絶対差=1")&&summary.contains("ARGB不一致 23000画素 / 最大チャンネル差 17"),
            "one first observed difference does not limit or invent the maximum across the full compared ARGB core");
        check(summary.contains("同じ候補の信頼度: 不一致 123点 / 最大差 4294967295"),
            "logical confidence comparison evidence remains independent even when ARGB mismatches");
        check(summary.contains("全比較領域の件数と最大差は最初の失敗候補だけの集計で、全候補の最大ではありません"),
            "candidate-local complete scan cannot be confused with all-layout aggregate statistics");
        String firstFailure=trace.strongGpuFirstFailure;
        // Keep the capture active so this exercises first-mismatch retention,
        // not the terminal-state guard. A deliberate clock tick must be harmless.
        Thread.sleep(3L);
        ProcessingTiming1947.strongGpuMismatch1973(trace,1L,255,1L,1L);
        check(trace.state==0&&trace.finished==0&&trace.strongMismatchMeasured&&
            trace.strongArgbMismatchCount==23000L&&trace.strongArgbMaxDelta==17&&
            trace.strongConfidenceMismatchCount==123L&&trace.strongConfidenceMaxDelta==4294967295L&&
            firstFailure.equals(trace.strongGpuFirstFailure),
            "an active capture retains the first full mismatch scalars and candidate identity");
        String elapsed="(?m)(^全体経過: )(?:<1|[0-9]+)( ms)";
        check(summary.replaceAll(elapsed,"$1<live-clock>$2").equals(
            ProcessingTiming1947.summary().replaceAll(elapsed,"$1<live-clock>$2")),
            "later trial leaves every diagnostic field unchanged apart from live elapsed time");
        ProcessingTiming1947.Trace partial=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.strongGpuMismatch1973(partial,0L,0,-1L,-1L);
        summary=ProcessingTiming1947.summary();
        check(summary.contains("ARGB不一致 0画素 / 最大チャンネル差 0")&&summary.contains("同じ候補の信頼度: 未取得"),
            "an observed exact ARGB comparison is separate from missing confidence evidence");
        check(partial.strongGpuStrips==0&&partial.strongCpuStrips==0&&partial.strongCpuVerifications==0,
            "full comparison metadata does not create selected outputs or successful CPU reference calls");
    }
    static void telemetryOwnerAndConcurrency() throws Exception {
        final ProcessingTiming1947.Trace owner=ProcessingTiming1947.begin(new Object());
        final ProcessingTiming1947.Trace unrelated=ProcessingTiming1947.begin(new Object());
        final int workers=4,iterations=400;
        final CountDownLatch start=new CountDownLatch(1);final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        Thread[] threads=new Thread[workers];
        for(int i=0;i<workers;i++){final int lane=i;threads[i]=new Thread(new Runnable(){public void run(){try{
            ProcessingTiming1947.enter(unrelated);start.await();
            for(int n=0;n<iterations;n++){
                ProcessingTiming1947.strongGpuReason1970(owner,"bank_busy",1);
                ProcessingTiming1947.strongGpuVerification1970(owner,2);
                ProcessingTiming1947.strongGpuTrial1971(owner,"argb_mismatch",1);
                ProcessingTiming1947.strongGpuFailure1971(owner,"argb_mismatch",36+lane%3,lane%3,5712,320,lane,lane,lane);
                ProcessingTiming1947.strongGpuWork1973(owner,1000000L,2000000L,3000000L,1);
            }
        }catch(Throwable failure){error.set(failure);}}});threads[i].start();}
        start.countDown();for(Thread thread:threads){thread.join(10000);check(!thread.isAlive(),"telemetry worker terminates without lock inversion");}
        if(error.get()!=null)throw new AssertionError(error.get());
        check(owner.strongGpuReasons.get("bank_busy")==workers*iterations,"concurrent reason increments retain exact owner and count");
        check(owner.strongCpuVerifications==2L*workers*iterations,"concurrent CPU verification increments are exact");
        check(unrelated.strongGpuReasons.isEmpty()&&unrelated.strongCpuVerifications==0,"unrelated thread scope cannot receive metadata");
        check(owner.strongGpuTrials.get("argb_mismatch")==workers*iterations&&unrelated.strongGpuTrials.isEmpty(),
            "concurrent trial counts retain explicit owner and exact total");
        check(!owner.strongGpuFirstFailure.isEmpty()&&unrelated.strongGpuFirstFailure.isEmpty(),
            "concurrent first snapshot attaches only to explicit stage owner");
        boolean intact=false;for(int lane=0;lane<workers;lane++)if(owner.strongGpuFirstFailure.equals(
            "ARGB不一致 / program="+(36+lane%3)+" / layout="+(lane%3)+" / 幅=5712 / 区間行数=320 / 位置="+lane+","+lane+" / 観測箇所の絶対差="+lane))intact=true;
        check(intact,"first concurrent failure is one complete observation without mixed worker fields");
        check(owner.strongCpuOracleNanos==1600000000L&&owner.strongGpuRequestNanos==3200000000L&&owner.strongWaitNanos==4800000000L
            &&owner.strongOracleReuses==1600,"concurrent work durations and reference reuse retain exact explicit-owner sums");
        check(!unrelated.strongCpuOracleMeasured&&!unrelated.strongGpuRequestMeasured&&!unrelated.strongWaitMeasured
            &&unrelated.strongOracleReuses==0,"unrelated worker thread scope cannot receive work telemetry");
        check(render(owner).contains("GPU処理枠を取得できず（詳細未記録） 1600区間")&&render(owner).contains("CPU照合 3200回"),"aggregated metadata renders actual counts");
        check(ProcessingTiming1947.summary().contains("CPU照合 0回"),"older worker owner cannot overwrite latest displayed photo");
        check(!ProcessingTiming1947.summary().contains("GPU候補失敗:"),"older trace failed trials never enter the newer photo summary");
    }
    static void stageDetails1976() throws Exception {
        ProcessingTiming1947.Trace owner=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.Trace unrelated=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.enter(unrelated);
        check(!render(owner).contains("ノイズ主工程の内訳"),"unknown details are not invented");
        check(!render(owner).contains("補正経路:"),"unknown route does not claim CPU or GPU");
        ProcessingTiming1947.detailWork1976(owner,0,12000000L);
        ProcessingTiming1947.detailWork1976(owner,1,23000000L);
        ProcessingTiming1947.detailWork1976(owner,2,34000000L);
        ProcessingTiming1947.detailWork1976(owner,0,8000000L);
        ProcessingTiming1947.detailRoute1976(owner,4);
        String text=render(owner);
        check(text.contains("前段 20 ms / モデル準備 23 ms / 強ノイズ 34 ms"),"whole stage durations remain separate from worker sums");
        check(text.contains("補正経路: 強ノイズからGPU連結"),"actual completed resident route is observable");
        check(!render(unrelated).contains("ノイズ主工程の内訳")&&!render(unrelated).contains("補正経路:"),"explicit owner keeps current unrelated photo clean");
        check(owner.strongGpuStrips==0&&owner.strongCpuStrips==0&&!owner.strongGpuRequestMeasured,"detail telemetry cannot fabricate output backend counts or GPU work");
        check(owner.stages[ProcessingTiming1947.NOISE]!=null,"original stage remains present");
        ProcessingTiming1947.detailWork1976(owner,-1,99000000L);ProcessingTiming1947.detailWork1976(owner,3,99000000L);
        ProcessingTiming1947.detailWork1976(owner,1,-1);ProcessingTiming1947.detailRoute1976(owner,5);
        ProcessingTiming1947.detailWork1976(null,0,1);ProcessingTiming1947.detailRoute1976(null,0);
        check(owner.noiseFirstNanos1976==20000000L&&owner.noiseModelNanos1976==23000000L&&owner.correctionRoute1976==4,"invalid input cannot partially update details");
        ProcessingTiming1947.finish(owner,true);text=render(owner);
        ProcessingTiming1947.detailWork1976(owner,0,99000000L);ProcessingTiming1947.detailRoute1976(owner,0);
        check(text.equals(render(owner)),"late workers cannot overwrite saved timing or committed route");
        ProcessingTiming1947.Trace peak=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.detailWork1976(peak,0,Long.MAX_VALUE);ProcessingTiming1947.detailWork1976(peak,0,99);
        check(peak.noiseFirstNanos1976==Long.MAX_VALUE,"elapsed counter saturates without negative wrap");
        ProcessingTiming1947.detailWork1976(peak,1,0);
        check(render(peak).contains("モデル準備 0 ms / 強ノイズ 未計測"),"measured zero differs from an unobserved part");
    }
    static void exactSpeedDiagnostics1978() throws Exception {
        final ProcessingTiming1947.Trace owner=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.Trace unrelated=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.enter(unrelated);
        check(owner.strongRoute1978==-1&&owner.residentReason1978==-1,"new route and resident state begin unobserved");
        check(!render(owner).contains("今回の強ノイズ配分:")&&!render(owner).contains("強ノイズ→補正のGPU連結:"),"no unobserved route is claimed");
        ProcessingTiming1947.strongRoute1978(owner,0);
        ProcessingTiming1947.strongRoute1978(owner,2);
        ProcessingTiming1947.strongRoute1978(owner,0);
        check(owner.strongRoute1978==2,"later initial state cannot erase pending proof");
        Thread[] workers=new Thread[6];
        for(int i=0;i<workers.length;i++){final int mode=i%3;workers[i]=new Thread(new Runnable(){public void run(){for(int n=0;n<20;n++)ProcessingTiming1947.strongRoute1978(owner,mode);}});workers[i].start();}
        for(Thread worker:workers){worker.join(10000);check(!worker.isAlive(),"routing diagnostics worker joined");}
        check(owner.strongRoute1978==1,"actual hybrid allocation survives concurrent unqualified strip events");
        check(render(owner).contains("実並列速度を確認済み・CPU併用"),"actual hybrid allocation has an explicit caption");
        ProcessingTiming1947.strongRoute1978(owner,-1);ProcessingTiming1947.strongRoute1978(owner,3);ProcessingTiming1947.strongRoute1978(null,1);
        check(owner.strongRoute1978==1&&unrelated.strongRoute1978==-1,"invalid input and unrelated scope cannot replace route state");
        ProcessingTiming1947.strongGpuReason1970(owner,"deferred_quality1978",2);
        ProcessingTiming1947.strongGpuReason1970(owner,"cpu_faster1978",3);
        check(render(owner).contains("採用可能なGPU候補なし（詳細未記録） 2区間")&&render(owner).contains("完了時間短縮のためCPUへ配分 3区間"),"quality deferral and intentional CPU allocation have distinct fixed reasons");
        check(owner.strongGpuStrips==0&&owner.strongCpuStrips==0&&owner.strongCpuVerifications==0,"route metadata never fabricates execution or quality trial counts");
        ProcessingTiming1947.resident1978(owner,4284,5712,4284,5712,0,false,6117552,97946368L,0);
        check(render(owner).contains("4284×5712 → 4284×5712 / 回転 0度 / 等倍専用以外"),"equal post-reduction dimensions do not imply the identity-specific analysis order");
        String[] labels={"検討中","画質・速度認定済みを採用","保存後の全画素検証へ登録","この入力・GPU機能・処理条件には未対応","画質不一致記録により不採用","保持上限または使用可能メモリ不足","検証キュー・再試行条件未成立","前提経路または速度条件未成立"};
        for(int reason=0;reason<labels.length;reason++){
            ProcessingTiming1947.resident1978(owner,4284,5712,4284,5712,0,true,6117552,97946368L,reason);
            check(render(owner).contains("強ノイズ→補正のGPU連結: "+labels[reason]),"each bounded resident admission reason is visible");
        }
        check(render(owner).contains("4284×5712 → 4284×5712 / 回転 0度 / 等倍専用")&&render(owner).contains("93.4 MiB / 前段half 6117552画素"),"input/output identity and source retention bytes are distinguished from half observation");
        check(unrelated.residentReason1978==-1,"resident diagnostics use the explicit photo owner");
        ProcessingTiming1947.resident1978(owner,0,5712,4284,5712,0,true,0,1,1);
        ProcessingTiming1947.resident1978(owner,4284,5712,4284,5712,360,true,0,1,1);
        ProcessingTiming1947.resident1978(owner,4284,5712,4284,5712,0,true,-1,1,1);
        ProcessingTiming1947.resident1978(owner,4284,5712,4284,5712,0,true,0,-1,1);
        ProcessingTiming1947.resident1978(owner,4284,5712,4284,5712,0,true,0,1,16);
        ProcessingTiming1947.resident1978(null,4284,5712,4284,5712,0,true,0,1,1);
        check(owner.residentReason1978==7&&owner.residentHalfLength1978==6117552&&owner.residentRetainedBytes1978==97946368L,"invalid calls cannot partially overwrite observed resident state");
        for(Field value:ProcessingTiming1947.Trace.class.getDeclaredFields())if(value.getName().endsWith("1978"))check(value.getType().isPrimitive(),"new trace state owns no source/mask/output reference");
        ProcessingTiming1947.finish(owner,true);String terminal=render(owner);
        ProcessingTiming1947.resident1978(owner,2,2,2,2,0,true,1,16,1);ProcessingTiming1947.strongRoute1978(owner,2);
        ProcessingTiming1947.strongGpuReason1970(owner,"deferred_quality1978",1);
        check(terminal.equals(render(owner)),"saved snapshot cannot be rewritten by late proof or worker events");
    }
    static void liveQualification1979() throws Exception {
        MemoryPreferences preferences=new MemoryPreferences();ProcessingTiming1947.init(new MemoryContext(preferences));
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(trace,0,16);
        GpuQualification1961.status="CPU/GPU再判定（現在）: 待ち7件 / 実行中0件 / 採用済み0件";
        ProcessingTiming1947.finish(trace,true);
        String historical=ProcessingTiming1947.summary(),persisted=preferences.getString("summary","");
        long finished=trace.finished,updated=trace.updatedMillis;
        GpuQualification1961.status="CPU/GPU再判定（現在）: 待ち6件 / 実行中1件 / 採用済み3件";
        String first=ProcessingTiming1947.liveSummary1979();
        check(first.startsWith(historical+"\n\n表示時点の"),"live view retains exact immutable historical prefix");
        check(first.contains("保存時のCPU/GPU再判定: 待ち7件 / 実行中0件 / 採用済み0件"),"save-time queue state remains labelled historical");
        check(first.contains("表示時点のCPU/GPU再判定（現在）: 待ち6件 / 実行中1件 / 採用済み3件"),"opening dialog reads actual current queue separately");
        GpuQualification1961.status="CPU/GPU再判定（現在）: 待ち0件 / 実行中0件 / 採用済み8件";
        String second=ProcessingTiming1947.liveSummary1979();
        check(second.contains("表示時点のCPU/GPU再判定（現在）: 待ち0件 / 実行中0件 / 採用済み8件"),"reopening sees qualification progress");
        check(!first.equals(second),"live status is not a frozen save-time snapshot");
        check(historical.equals(ProcessingTiming1947.summary())&&persisted.equals(preferences.getString("summary","")),"current qualification cannot rewrite saved or persisted photo record");
        check(trace.finished==finished&&trace.updatedMillis==updated&&trace.strongGpuStrips==0&&trace.strongCpuStrips==16,"live reads cannot claim retroactive GPU use or alter elapsed bounds");
        check(preferences.saves==1,"live status is never persisted into the historical record");
        GpuQualification1961.throwStatus=true;
        check(ProcessingTiming1947.liveSummary1979().equals(historical),"unavailable current status preserves the complete saved report");
        GpuQualification1961.throwStatus=false;
    }
    static void earlyCaptureCancellation1979() throws Exception {
        Object key=new Object();
        check(GpuQualification1961.captureChanges==0,"no phantom capture cancellation");
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(key);
        check(trace!=null&&GpuQualification1961.captureChanges==1,"capture begin cancels older optional proofs before shot association");
        check(ProcessingTiming1947.captureEpoch1953()==0,"early cancellation does not fabricate a shot epoch");
        ProcessingTiming1947.associate(key,12345);
        check(GpuQualification1961.captureChanges==2&&ProcessingTiming1947.captureEpoch1953()==1,"real shot association retains independent epoch cancellation");
        ProcessingTiming1947.associate(key,12345);
        check(GpuQualification1961.captureChanges==2&&ProcessingTiming1947.captureEpoch1953()==1,"same shot reassociation does not add cancellation or epoch");
        check(trace.strongGpuStrips==0&&trace.strongCpuStrips==0,"early cancellation cannot fabricate photo backend counts");
    }
    static void selectedBackends1982() throws Exception {
        ProcessingTiming1947.Trace owner=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.Trace unrelated=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.enter(unrelated);
        check(render(owner).contains("ノイズ前段の経路記録: 未観測（CPU/GPU未判定）"),"unobserved initial work cannot imply CPU");
        ProcessingTiming1947.backend1982(owner,0,0,2,23000000L);
        ProcessingTiming1947.backend1982(owner,0,1,1,-1L);
        ProcessingTiming1947.backend1982(owner,1,1,7,0L);
        ProcessingTiming1947.backend1982(owner,1,0,1,-1L);
        ProcessingTiming1947.backend1982(owner,2,1,2,6000000L);
        ProcessingTiming1947.backend1982(owner,2,1,1,-1L);
        String text=render(owner);
        check(text.contains("ノイズ前段の経路記録: GPU採用 1回 / CPU採用 2回\n  記録した作業累計: GPU 未計測 / CPU 23 ms"),"initial accepted counts and unknown time stay separate");
        check(text.contains("モデル準備の経路記録: GPU採用 7回 / CPU採用 1回\n  記録した作業累計: GPU 0 ms / CPU 未計測"),"observed zero time differs from unmeasured model time");
        check(text.contains("補正の経路記録: GPU採用 3回 / CPU採用 0回\n  記録した作業累計: GPU 6 ms（一部未計測） / CPU 記録なし"),"partial timing cannot imply all accepted correction work was measured");
        check(text.contains("画素比率やGPU使用率ではありません"),"different work units cannot be mistaken for a hardware utilization percentage");
        check(owner.strongGpuStrips==0&&owner.strongCpuStrips==0&&owner.strongCpuVerifications==0,"backend detail cannot fabricate Strong selection or validation counts");
        check(unrelated.backendCounts1982[0]==0&&unrelated.backendCounts1982[5]==0,"captured explicit owner beats unrelated thread scope");
        check(ProcessingTiming1947.summary().contains("補正の経路記録: 未観測（CPU/GPU未判定）"),"older work does not replace latest displayed photo");
        ProcessingTiming1947.finish(owner,true);text=render(owner);
        ProcessingTiming1947.backend1982(owner,0,1,99,99);ProcessingTiming1947.strongGpuLockWait1982(owner,99);
        ProcessingTiming1947.detailDefaultRoute1982(owner);ProcessingTiming1947.residentExit1982(owner,100,1);
        check(text.equals(render(owner)),"finished photograph rejects all new late telemetry");
    }
    static void invalidBackendInputs1982() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.backend1982(null,0,0,1,1);
        for(int[] args:new int[][]{{-1,0,1},{3,0,1},{0,-1,1},{0,2,1},{0,0,0},{0,0,-1}})
            ProcessingTiming1947.backend1982(trace,args[0],args[1],args[2],1);
        ProcessingTiming1947.backend1982(trace,0,0,1,-2);
        for(int i=0;i<6;i++)check(trace.backendCounts1982[i]==0&&trace.backendNanos1982[i]==0&&!trace.backendTimed1982[i]&&!trace.backendUntimed1982[i],"invalid inputs cannot partially fill fixed slots");
        check(trace.backendCounts1982.length==6&&trace.backendNanos1982.length==6&&trace.backendTimed1982.length==6&&trace.backendUntimed1982.length==6,"diagnostics own only three fixed CPU/GPU pairs");
        trace.backendCounts1982[0]=Long.MAX_VALUE-1;trace.backendNanos1982[0]=Long.MAX_VALUE-1;
        ProcessingTiming1947.backend1982(trace,0,0,3,3);
        check(trace.backendCounts1982[0]==Long.MAX_VALUE&&trace.backendNanos1982[0]==Long.MAX_VALUE,"counts and selected-work time saturate without wrap");
        ProcessingTiming1947.strongGpuLockWait1982(null,5);ProcessingTiming1947.strongGpuLockWait1982(trace,-1);
        check(!trace.strongLockWaitMeasured1982,"invalid lock timing remains unobserved");
        ProcessingTiming1947.strongGpuLockWait1982(trace,Long.MAX_VALUE);ProcessingTiming1947.strongGpuLockWait1982(trace,1);
        check(trace.strongLockWaitNanos1982==Long.MAX_VALUE,"lock accumulator saturates");
        check(!trace.strongWaitMeasured&&trace.strongWaitNanos==0,"new monitor timing never rewrites the historical bank-work timer");
    }
    static void concurrentBackendInputs1982() throws Exception {
        final ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        final ProcessingTiming1947.Trace wrong=ProcessingTiming1947.begin(new Object());
        final int count=6,iterations=200;
        final CountDownLatch start=new CountDownLatch(1);
        final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        Thread[] workers=new Thread[count];
        for(int i=0;i<count;i++){final int part=i%3;workers[i]=new Thread(new Runnable(){public void run(){try{
            ProcessingTiming1947.enter(wrong);start.await();
            for(int n=0;n<iterations;n++){
                ProcessingTiming1947.backend1982(trace,part,0,2,3);
                ProcessingTiming1947.backend1982(trace,part,1,1,5);
                ProcessingTiming1947.strongGpuLockWait1982(trace,7);
            }
        }catch(Throwable failure){error.set(failure);}}});workers[i].start();}
        start.countDown();for(Thread worker:workers){worker.join(10000);check(!worker.isAlive(),"new telemetry worker terminates");}
        if(error.get()!=null)throw new AssertionError(error.get());
        for(int part=0;part<3;part++){
            check(trace.backendCounts1982[part*2]==800&&trace.backendCounts1982[part*2+1]==400,"parallel selected counts are exact per stage");
            check(trace.backendNanos1982[part*2]==1200&&trace.backendNanos1982[part*2+1]==2000,"parallel selected timing stays per backend");
            check(wrong.backendCounts1982[part*2]==0&&wrong.backendCounts1982[part*2+1]==0,"thread-local unrelated trace remains untouched");
        }
        check(trace.strongLockWaitNanos1982==8400&&!wrong.strongLockWaitMeasured1982,"parallel lock wait retains explicit owner");
    }
    static final class CountingGroup1982 extends android.preference.PreferenceGroup {
        int summaries;public String getKey(){return "detail_last_result";}
        public void setSummary(CharSequence value){summaries++;}
    }
    static void noPerOperationRefresh1982() throws Exception {
        final CountingGroup1982 group=new CountingGroup1982();final MemoryPreferences prefs=new MemoryPreferences();
        android.preference.PreferenceActivity activity=new android.preference.PreferenceActivity(){
            public android.preference.PreferenceGroup getPreferenceScreen(){return group;}
            public SharedPreferences getSharedPreferences(String name,int mode){return prefs;}
        };
        ProcessingTiming1947.install(activity);ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        int summaries=group.summaries,status=GpuQualification1961.statusCalls;
        trace.updatedMillis=123;field("lastViewRefresh").setLong(null,0);
        for(int i=0;i<20;i++){
            ProcessingTiming1947.backend1982(trace,0,0,1,1);
            ProcessingTiming1947.strongGpuLockWait1982(trace,1);
            ProcessingTiming1947.detailDefaultRoute1982(trace);
        }
        check(group.summaries==summaries,"per-operation scalar recording never schedules UI refresh");
        check(trace.updatedMillis==123,"per-operation recording does not publish a whole-photo update");
        check(GpuQualification1961.statusCalls==status&&prefs.saves==0,"recording has no qualification query or preference writes");
        ProcessingTiming1947.finish(trace,true);
        check(group.summaries>summaries&&prefs.saves==1,"existing terminal publish delivers completed work and persists exactly once");
        check(prefs.getString("summary","").contains("GPU採用 0回 / CPU採用 20回"),"deferred UI refresh still includes all accepted selections");
    }
    static void residentFinalReasons1982() throws Exception {
        String[] labels={"GPUセッション使用中","保存後の検証実行中","今回の検証画像コピー枠により延期","撮影世代の変更または中断","検証画像コピー失敗","GPU連結の実行失敗","GPU結果未成立","終了理由未記録"};
        for(int i=0;i<labels.length;i++){
            ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
            ProcessingTiming1947.residentExit1982(trace,0,i+8);String text=render(trace);
            check(text.contains("強ノイズ→補正のGPU連結: "+labels[i]),"all new terminal reasons render their actual distinct outcome");
            check(text.contains("連結候補の入力/出力: 未計測")&&text.contains("保持見積: 未計算"),"missing input geometry or estimate is not fabricated as a measured zero");
            check(!text.contains("0.0 MiB")&&!text.contains("検討開始"),"terminal early exits do not leave ambiguous initial placeholders");
            check(trace.strongGpuStrips==0&&trace.backendCounts1982[5]==0,"admission reasons do not fabricate accepted GPU work");
        }
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.resident1978(trace,3060,4080,4284,5712,0,false,3121200,0,0);
        check(render(trace).contains("強ノイズ→補正のGPU連結: 検討中"),"live consideration remains distinguishable");
        ProcessingTiming1947.finish(trace,true);String saved=render(trace);
        check(saved.contains("強ノイズ→補正のGPU連結: 終了理由未記録")&&!saved.contains("GPU連結: 検討中"),"missing producer event after finish remains honest unknown");
        check(trace.residentReason1978==0,"renderer never changes actual recorded state to invent an admission cause");
        check(saved.contains("等倍専用以外")&&!saved.contains("通常経路"),"geometry label does not imply CPU execution");
        ProcessingTiming1947.Trace valid=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.residentExit1982(valid,42,10);
        ProcessingTiming1947.residentExit1982(valid,-1,1);ProcessingTiming1947.residentExit1982(valid,0,0);
        ProcessingTiming1947.residentExit1982(valid,0,16);ProcessingTiming1947.residentExit1982(null,0,1);
        check(valid.residentRetainedBytes1978==42&&valid.residentReason1978==10,"invalid terminal events cannot overwrite actual exit");
    }
    static void defaultRouteAndSeparateWait1982() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.detailDefaultRoute1982(trace);
        check(trace.correctionRoute1976==0&&render(trace).contains("個別処理（CPU/GPU採用は経路記録を参照）"),"ordinary correction is recorded without claiming CPU-only work");
        for(int route=1;route<=4;route++){
            ProcessingTiming1947.detailRoute1976(trace,route);ProcessingTiming1947.detailDefaultRoute1982(trace);
            check(trace.correctionRoute1976==route,"default path cannot overwrite more precise completed chain route");
        }
        ProcessingTiming1947.strongGpuLockWait1982(trace,11000000L);
        check(render(trace).contains("GPU枠受付ロック取得待ち累計: 11 ms"),"lock timing renders even without the historical work counters");
        check(render(trace).contains("GPU枠内の待機・判定作業累計: 未計測"),"monitor wait cannot stand in for unmeasured bank work");
        ProcessingTiming1947.strongGpuWork1973(trace,0,0,3000000L,0);
        check(render(trace).contains("GPU枠内の待機・判定作業累計: 3 ms")&&render(trace).contains("GPU枠受付ロック取得待ち累計: 11 ms"),"separate clocks preserve their independent scopes");
    }
    static void preciseReasonsAndScreenshot1982() throws Exception {
        String[] codes={"route_unqualified1982","route_exact_rejected1982","route_speed_condition1982","route_unqualified_exact1982","route_unqualified_speed1982","route_exact_speed1982","route_unqualified_exact_speed1982","route_unavailable1982","bank_unavailable1982","bank_exact_rejected1982","bank_unqualified1982","bank_speed_condition1982","bank_budget1982","bank_waiter1982","bank_no_prediction1982","bank_prediction_budget1982"};
        String[] labels={"候補の認定未成立","候補の画質不一致記録","候補の速度条件未達","候補の認定未成立・画質不一致記録","候補の認定未成立・速度条件未達","候補の画質不一致記録・速度条件未達","候補の認定未成立・画質不一致記録・速度条件未達","GPU候補の利用条件未成立","GPU処理枠の利用不可","枠取得時に画質不一致記録を確認","枠取得時に有効な認定なし","枠取得時に速度条件未達","GPU枠の残り時間予算不足","GPU枠の待機者上限","GPU枠の解放予測なし","GPU枠の解放予測が残り予算を超過"};
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        for(int i=0;i<codes.length;i++){
            ProcessingTiming1947.strongGpuReason1970(trace,codes[i],1);
            check(render(trace).contains(labels[i]+" 1区間"),"new bounded candidate/bank reason survives exact whitelist mapping");
        }
        check(trace.strongGpuReasons.size()==16,"new reason vocabulary occupies only fixed diagnostic slots");
        ProcessingTiming1947.strongGpuReason1970(trace,"private-image-key",1);
        check(trace.strongGpuReasons.size()==16&&!render(trace).contains("private-image-key"),"new vocabulary does not admit arbitrary keys");
        ProcessingTiming1947.strongRoute1978(trace,2);
        check(render(trace).contains("採用条件が成立しない区間はCPU（理由は下記）")&&!render(trace).contains("保存後のGPU確認待ち"),"route summary never promises that every excluded condition is queued");
        ProcessingTiming1947.Trace shot=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(shot,12,4);
        ProcessingTiming1947.strongGpuWork1973(shot,723000000L,915000000L,700000L,0);
        ProcessingTiming1947.detailWork1976(shot,0,755000000L);
        ProcessingTiming1947.detailWork1976(shot,1,610000000L);
        ProcessingTiming1947.detailWork1976(shot,2,715000000L);
        String text=render(shot);
        check(text.contains("GPU 12区間 / CPU 4区間（GPU使用あり）")&&text.contains("CPU照合 0回"),"reported screenshot conditions unambiguously show GPU use despite zero extra CPU verification");
        check(text.contains("強ノイズCPU経路・照合作業累計: 723 ms")&&text.contains("GPU準備・転送・実行・読戻し作業累計: 915 ms"),"CPU selected path work is no longer labelled solely as reference calculation");
        check(text.contains("強ノイズ 715 ms")&&text.contains("工程時間とは一致しません"),"parallel accumulated work is not forced to equal wall time");
    }
    static void expandedSnapshotAndVersion1982() throws Exception {
        MemoryPreferences preferences=new MemoryPreferences();ProcessingTiming1947.init(new MemoryContext(preferences));
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        StringBuilder status=new StringBuilder("検証キュー（現在）: 待機7件 / 実行中0件 / 条件別記録: ");
        while(status.length()<400)status.append("再試行条件・");status.append("末尾の条件記録");
        GpuQualification1961.status=status.toString();ProcessingTiming1947.finish(trace,true);
        String saved=ProcessingTiming1947.summary();
        check(saved.contains("末尾の条件記録"),"categorical qualification snapshot is not truncated at old 240-character bound");
        check(saved.equals(preferences.getString("summary","")),"expanded snapshot persists byte accurately");
        GpuQualification1961.status="検証キュー（現在）: 待機0件 / 実行中0件";
        check(ProcessingTiming1947.liveSummary1979().startsWith(saved+"\n\n表示時点の"),"live queue changes keep expanded saved record immutable");
        check(ProcessingTiming1947.liveSummary1979().contains("検証キューと条件別の記録は"),"live caption separates queued jobs from cached condition records");
        reset();preferences.values.put("version","1.9.81");ProcessingTiming1947.init(new MemoryContext(preferences));
        check(ProcessingTiming1947.summary().startsWith("ULike v1.9.82 / 新しい撮影の計測待ち"),"old-version diagnostic snapshot cannot masquerade as new instrumentation");
        check(GpuQualification1961.statusCalls==0,"version restore does not query or reset qualification state");
    }
    public static void main(String[] args) throws Exception {
        run("live_qualification_separate_from_immutable_photo_snapshot1979",new Case(){public void run()throws Exception{liveQualification1979();}});
        run("early_capture_probe_cancellation1979",new Case(){public void run()throws Exception{earlyCaptureCancellation1979();}});
        run("exact_speed_route_resident_scalars_concurrency_owner_terminal",new Case(){public void run()throws Exception{exactSpeedDiagnostics1978();}});
        run("noise_wall_parts_and_committed_route_preserve_owner_and_terminal_snapshot",new Case(){public void run()throws Exception{stageDetails1976();}});
        run("explicit_owner_ignores_unrelated_worker_scope",new Case(){public void run()throws Exception{workerIdentity();}});
        run("concurrent_backend_counts_are_exact",new Case(){public void run()throws Exception{concurrentCounts();}});
        run("invalid_or_missing_backend_evidence_remains_unknown",new Case(){public void run()throws Exception{invalidInputs();}});
        run("completed_snapshot_ignores_late_workers_and_live_qualification",new Case(){public void run()throws Exception{terminalSnapshot();}});
        run("failed_snapshot_cannot_be_rewritten",new Case(){public void run()throws Exception{failedTerminal();}});
        run("restart_preserves_saved_backend_and_qualification",new Case(){public void run()throws Exception{persistence();}});
        run("stage_wall_time_is_union_not_worker_sum_or_first_to_last",new Case(){public void run()throws Exception{unionDuration();}});
        run("nested_tokens_remain_idempotent",new Case(){public void run()throws Exception{liveTokens();}});
        run("initial_wait_state_is_visible_and_does_not_claim_gpu_use",new Case(){public void run()throws Exception{initialWait();}});
        run("backend_observations_fit_before_realistic_long_settings",new Case(){public void run()throws Exception{firstScreenBudget();}});
        run("fallback_reasons_are_fixed_bounded_and_do_not_change_route_counts",new Case(){public void run()throws Exception{boundedReasons();}});
        run("reason_and_verification_updates_preserve_owner_under_concurrency",new Case(){public void run()throws Exception{telemetryOwnerAndConcurrency();}});
        run("trial_failures_and_bounded_first_snapshot_remain_separate_from_selected_output",new Case(){public void run()throws Exception{trialsAndStructuralSnapshot();}});
        run("work_time_is_explicit_cumulative_and_preserves_stage_union",new Case(){public void run()throws Exception{boundedWorkTelemetry();}});
        run("first_failed_candidate_full_argb_and_confidence_scalars_are_independent_and_immutable",new Case(){public void run()throws Exception{fullMismatchScalars();}});
        check(assertions==227,"all 227 inherited assertions execute before new diagnostics cases");
        run("selected_backend_counts_timing_owner_and_terminal1982",new Case(){public void run()throws Exception{selectedBackends1982();}});
        run("fixed_scalar_slots_invalid_inputs_and_saturation1982",new Case(){public void run()throws Exception{invalidBackendInputs1982();}});
        run("concurrent_selected_backend_and_lock_counts1982",new Case(){public void run()throws Exception{concurrentBackendInputs1982();}});
        run("per_operation_recording_never_refreshes_ui_or_queries_qualification1982",new Case(){public void run()throws Exception{noPerOperationRefresh1982();}});
        run("resident_terminal_reasons_and_missing_estimates1982",new Case(){public void run()throws Exception{residentFinalReasons1982();}});
        run("default_route_preserves_completed_chain_and_wait_scopes1982",new Case(){public void run()throws Exception{defaultRouteAndSeparateWait1982();}});
        run("precise_reason_whitelist_and_reported_screenshot_conditions1982",new Case(){public void run()throws Exception{preciseReasonsAndScreenshot1982();}});
        run("expanded_snapshot_and_old_version_restore1982",new Case(){public void run()throws Exception{expandedSnapshotAndVersion1982();}});
        StringBuilder names=new StringBuilder("[");for(String name:tests){if(names.length()>1)names.append(',');names.append('"').append(name).append('"');}names.append(']');
        System.out.println("RESULT {\"status\":\"passed\",\"live_qualification_status1979_verified\":true,\"diagnostic_snapshot_immutable\":true,\"early_capture_probe_cancel1979_verified\":true,\"timing_diagnostics1982_verified\":true,\"selected_backend_timing1982_verified\":true,\"resident_exit_display1982_verified\":true,\"separate_bank_lock_timing1982_verified\":true,\"diagnostic_updates_no_hot_ui_refresh1982_verified\":true,\"all_227_inherited_timing_assertions_preserved\":true,\"assertions\":"+assertions+",\"backend_diagnostics_regressions_passed\":true,\"backend_visibility_regressions_passed\":true,\"strong_preference_telemetry_regressions_passed\":true,\"strong_failure_diagnostics_regressions_passed\":true,\"strong_work_latency_regressions_passed\":true,\"strong_full_mismatch_regressions_passed\":true,\"stage_details1976_regressions_passed\":true,\"exact_speed_diagnostics1978_verified\":true,\"physical_android_tested\":false,\"physical_android_ui_tested\":false,\"tests\":"+names+"}");
    }
}
