package com.hiro.ulike;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** Bounded scheduling observations, not a presentation/FPS or GPU utilization
 * profiler. No image, camera, Surface or capture callback is retained here. */
public final class Responsiveness19122 {
    private static final Object LOCK=new Object();
    private static final long SECOND=1000000000L, PRE=2L*SECOND, POST=10L*SECOND, HARD=60L*SECOND;
    private static final ArrayDeque<Record> RECORDS=new ArrayDeque<Record>();
    private static final Ring PRE_UI=new Ring(),PRE_CAMERA=new Ring();
    private static final ScheduledThreadPoolExecutor WORK=new ScheduledThreadPoolExecutor(1,new Factory());
    private static WeakReference<Activity> activity=new WeakReference<Activity>(null);
    private static WeakReference<Object> cameraOwner=new WeakReference<Object>(null);
    private static long cameraEpoch=-1,cameraVerifiedAt,cameraOwnerAttempt;
    private static Context app;
    private static boolean registered;
    private static Handler main;
    private static Pump pump;
    private static Record current;
    private static long generation,preUntil;
    private static volatile boolean watching;
    static {WORK.setRemoveOnCancelPolicy(true);WORK.setKeepAliveTime(15L,TimeUnit.SECONDS);WORK.allowCoreThreadTimeOut(true);}
    private Responsiveness19122(){}

    private static final class Factory implements ThreadFactory {
        public Thread newThread(Runnable r){Thread t=new Thread(new Worker(r),"ULike-response19122");t.setDaemon(true);return t;}
    }
    private static final class Worker implements Runnable {
        final Runnable work;
        Worker(Runnable value){work=value;}
        public void run(){try{android.os.Process.setThreadPriority(10);}catch(Throwable optional){}work.run();}
    }
    /** 1 ms upper-bound bins; overflow is never reported as a small interval. */
    static final class Gaps {
        final int[] bins=new int[257];
        long count,max,total;
        void add(long gap){if(gap<=0)return;int bin=(int)Math.min(256L,(gap+999999L)/1000000L);bins[bin]++;count++;total+=gap;if(gap>max)max=gap;}
        long percentile(int percent){if(count==0)return -1;long need=(count*percent+99)/100,total=0;for(int i=1;i<bins.length;i++){total+=bins[i];if(total>=need)return i==256?-2:i;}return -1;}
        String text(){return count==0?"未観測":count+"間隔 / p95 "+quantile(percentile(95))+" / p99 "+quantile(percentile(99))+" / 最大 "+ms(max)+" ms";}
    }
    static final class Ring {
        final long[] times=new long[256];int cursor,count;
        void add(long now){times[cursor]=now;cursor=(cursor+1)%times.length;if(count<times.length)count++;}
        void clear(){cursor=count=0;}
        Gaps before(long now){Gaps out=new Gaps();long prior=0;for(int i=0;i<count;i++){long t=times[(cursor-count+i+times.length)%times.length];if(t<=0||t>now||now-t>PRE)continue;if(prior>0)out.add(t-prior);prior=t;}return out;}
    }
    static final class Reading {
        final long at,javaBytes,nativeBytes;final int thermal;
        Reading(long time,long javaHeap,long nativeHeap,int status){at=time;javaBytes=javaHeap;nativeBytes=nativeHeap;thermal=status;}
    }
    static final class Record {
        final long id,start,generation;
        final WeakReference<ProcessingTiming1947.Trace> trace;
        final Gaps beforeUi,beforeCamera;
        final Gaps[] ui=new Gaps[11],camera=new Gaps[11];
        final Reading[] readings=new Reading[3];
        long saved,deadline,lastUi,lastCamera,firstUi,firstCamera,terminalQueued,terminalStart,terminalEnd,ended;
        long uiSilence,cameraSilence,providerChanges;
        boolean done,foregroundEnded;
        String reason="計測中",cleanup="未観測",policy="撮影時の制御: 未観測";
        ScheduledFuture<?> expiry;
        Record(ProcessingTiming1947.Trace t,long now,long gen){id=t.id;start=now;generation=gen;trace=new WeakReference<ProcessingTiming1947.Trace>(t);beforeUi=PRE_UI.before(now);beforeCamera=PRE_CAMERA.before(now);deadline=now+HARD;for(int i=0;i<ui.length;i++){ui[i]=new Gaps();camera[i]=new Gaps();}}
        int phase(long now){if(saved==0||now<saved)return 0;long delta=now-saved;return delta>=POST?-1:1+(int)(delta/SECOND);}
        void notice(boolean isUi,long now){if(done||now>deadline||now<start)return;int phase=phase(now);if(phase<0)return;long old=isUi?lastUi:lastCamera;if(old>0&&now>old)(isUi?ui[phase]:camera[phase]).add(now-old);if(isUi){if(firstUi==0)firstUi=now;lastUi=now;}else {if(firstCamera==0)firstCamera=now;lastCamera=now;}}
    }

    public static void init19122(Context context){
        if(context==null)return;
        try{
            Context application=context.getApplicationContext();
            Activity found=null;Context next=context;
            for(int i=0;i<8&&next!=null;i++){if(next instanceof Activity){found=(Activity)next;break;}if(!(next instanceof ContextWrapper))break;Context base=((ContextWrapper)next).getBaseContext();if(base==next)break;next=base;}
            synchronized(LOCK){if(app==null&&application!=null)app=application;if(found!=null)activity=new WeakReference<Activity>(found);if(main==null)main=new Handler(Looper.getMainLooper());
                if(!registered&&application instanceof Application){((Application)application).registerActivityLifecycleCallbacks(new Lifecycle());registered=true;}}
        }catch(Throwable optional){}
    }
    public static void foreground19122(){
        try{synchronized(LOCK){preUntil=System.nanoTime()+PRE;watching=true;postArmLocked();}}catch(Throwable optional){}
    }
    public static void background19122(){
        try{Record end;synchronized(LOCK){preUntil=0;watching=false;end=current;if(end!=null&&!end.done)end.foregroundEnded=true;}
            if(end!=null)complete(end,"画面が非表示になり終了",System.nanoTime());stopPump();
        }catch(Throwable optional){}
    }
    private static final class Lifecycle implements Application.ActivityLifecycleCallbacks {
        public void onActivityResumed(Activity a){synchronized(LOCK){activity=new WeakReference<Activity>(a);}foreground19122();}
        public void onActivityPaused(Activity a){synchronized(LOCK){if(activity.get()!=a)return;}background19122();}
        public void onActivityDestroyed(Activity a){synchronized(LOCK){if(activity.get()!=a)return;activity.clear();}background19122();}
        public void onActivityCreated(Activity a,Bundle b){}
        public void onActivityStarted(Activity a){}
        public void onActivityStopped(Activity a){}
        public void onActivitySaveInstanceState(Activity a,Bundle b){}
    }
    public static void captured19122(ProcessingTiming1947.Trace trace){
        if(trace==null)return;
        try{
            Record previous,next;long now=System.nanoTime();
            synchronized(LOCK){for(Record r:RECORDS)if(r.id==trace.id)return;previous=current;next=new Record(trace,now,++generation);current=next;RECORDS.addLast(next);while(RECORDS.size()>8)RECORDS.removeFirst();watching=true;postArmLocked();next.expiry=WORK.schedule(new Expire(next),60L,TimeUnit.SECONDS);}
            if(previous!=null&&!previous.done)complete(previous,"次の撮影開始により終了",now);
            WORK.execute(new Probe(next,0));
        }catch(Throwable optional){}
    }
    public static void published19122(ProcessingTiming1947.Trace trace){
        if(trace==null)return;
        try{Record r;synchronized(LOCK){r=find(trace.id);if(r==null||r.done||r.saved!=0)return;r.saved=System.nanoTime();r.deadline=r.saved+POST;if(r.expiry!=null)r.expiry.cancel(false);r.expiry=WORK.schedule(new Expire(r),10L,TimeUnit.SECONDS);}
            WORK.execute(new Probe(r,1));
        }catch(Throwable optional){}
    }
    public static void failed19122(ProcessingTiming1947.Trace trace){
        if(trace==null)return;try{Record r;synchronized(LOCK){r=find(trace.id);}if(r!=null)complete(r,"保存失敗で終了",System.nanoTime());}catch(Throwable optional){}
    }
    public static void policy19122(ProcessingTiming1947.Trace trace){
        if(trace==null)return;
        try{synchronized(LOCK){Record r=find(trace.id);if(r==null)return;r.policy="撮影時の制御: "+CapturePolicy19122.activeName19122()+" / CPU上限 "+CapturePolicy19122.cpuWorkerLimit()+" / Strong GPU枠 "+CapturePolicy19122.gpuBanks()+" / GPU投入間隔指定 "+ms(CapturePolicy19122.gpuSubmissionGapNanos())+" ms / HD早期解除 "+CapturePolicy19122.earlyPreviewEnabled()+" / 保存後整理 "+CapturePolicy19122.cleanupEnabled();}publish(trace.id);}catch(Throwable optional){}
    }
    public static void terminalQueued19122(ProcessingTiming1947.Trace trace){terminal(trace,0);}
    public static void terminalStarted19122(ProcessingTiming1947.Trace trace){terminal(trace,1);}
    public static void terminalFinished19122(ProcessingTiming1947.Trace trace){terminal(trace,2);}
    private static void terminal(ProcessingTiming1947.Trace trace,int event){
        if(trace==null)return;try{synchronized(LOCK){Record r=find(trace.id);if(r==null)return;long now=System.nanoTime();if(event==0&&r.terminalQueued==0)r.terminalQueued=now;else if(event==1&&r.terminalStart==0)r.terminalStart=now;else if(event==2&&r.terminalEnd==0)r.terminalEnd=now;}if(event==2)publish(trace.id);}catch(Throwable optional){}
    }
    public static void cleanup19122(long traceId,String status,long elapsedNanos,long releasedJavaBytes){
        try{synchronized(LOCK){Record r=find(traceId);if(r==null)return;r.cleanup=bounded(status,140)+" / 作業 "+ms(Math.max(0L,elapsedNanos))+" ms / Javaプール参照解除 "+(releasedJavaBytes<0?"未観測":mib(releasedJavaBytes)+" MiB")+"（OSへの返却量ではない）";}publish(traceId);}catch(Throwable optional){}
    }
    /** The original provider observer verifies current ownership at most every
     * five seconds. Keep that distinction; an input notice is not presentation. */
    /** Reserve at most one extra current-owner lookup per second while a
     * bounded response window is active. Failed/foreign providers cannot turn
     * a per-frame observation into a per-frame reflective query. */
    public static boolean requestCameraOwner19122(Object owner,long epoch){
        if(!watching||owner==null)return false;
        try{long now=System.nanoTime();synchronized(LOCK){if(!watching)return false;if(cameraOwner.get()==owner&&cameraEpoch==epoch&&now-cameraVerifiedAt<5L*SECOND)return false;if(cameraOwnerAttempt>0&&now-cameraOwnerAttempt<SECOND)return false;cameraOwnerAttempt=now;return true;}}catch(Throwable optional){return false;}
    }
    public static void cameraOwner19122(Object owner,long epoch,boolean currentOwner){
        if(!watching)return;
        try{synchronized(LOCK){if(!currentOwner){if(cameraOwner.get()==owner){cameraOwner.clear();cameraEpoch=-1;}return;}Object old=cameraOwner.get();if(old!=owner||cameraEpoch!=epoch){if(current!=null&&!current.done&&old!=null){current.providerChanges++;current.lastCamera=0;}PRE_CAMERA.clear();cameraOwner=new WeakReference<Object>(owner);cameraEpoch=epoch;}cameraVerifiedAt=System.nanoTime();}}
        catch(Throwable optional){}
    }
    public static void cameraNotice19122(Object owner,long epoch){
        if(!watching)return;
        try{long now=System.nanoTime();synchronized(LOCK){if(!watching||owner==null||cameraOwner.get()!=owner||cameraEpoch!=epoch||now-cameraVerifiedAt>6L*SECOND)return;PRE_CAMERA.add(now);if(current!=null)current.notice(false,now);}}
        catch(Throwable optional){}
    }
    private static void postArmLocked(){if(main!=null)main.post(new Arm(generation));}
    private static final class Arm implements Runnable {
        final long generation;
        Arm(long value){generation=value;}
        public void run(){try{synchronized(LOCK){if(generation!=Responsiveness19122.generation||!watching)return;if(pump!=null)Choreographer.getInstance().removeFrameCallback(pump);pump=new Pump(generation);Choreographer.getInstance().postFrameCallback(pump);}}catch(Throwable optional){}}
    }
    private static final class Stop implements Runnable {
        public void run(){try{synchronized(LOCK){if(watching)return;if(pump!=null)Choreographer.getInstance().removeFrameCallback(pump);pump=null;}}catch(Throwable optional){}}
    }
    private static void stopPump(){try{Handler handler;synchronized(LOCK){handler=main;}if(handler!=null)handler.post(new Stop());}catch(Throwable optional){}}
    private static final class Pump implements Choreographer.FrameCallback {
        final long generation;
        Pump(long value){generation=value;}
        public void doFrame(long ignoredVsyncTime){
            try{
                long now=System.nanoTime();Activity a;boolean valid;
                synchronized(LOCK){if(pump!=this||generation!=Responsiveness19122.generation||!watching)return;a=activity.get();}
                valid=a!=null&&!a.isFinishing()&&!a.isDestroyed()&&a.hasWindowFocus();
                synchronized(LOCK){if(pump!=this||generation!=Responsiveness19122.generation||!watching)return;
                    if(valid){PRE_UI.add(now);if(current!=null)current.notice(true,now);}
                    boolean active=current!=null&&!current.done&&now<=current.deadline;
                    if(!active&&now>=preUntil){watching=false;pump=null;return;}
                    Choreographer.getInstance().postFrameCallback(this);
                }
            }catch(Throwable optional){synchronized(LOCK){if(pump==this)pump=null;}}
        }
    }
    private static final class Expire implements Runnable {
        final Record record;
        Expire(Record r){record=r;}
        public void run(){complete(record,record.saved>0?"保存公開後10秒の観測終了":"保存公開未観測・60秒で終了",System.nanoTime());}
    }
    private static final class Probe implements Runnable {
        final Record record;final int slot;
        Probe(Record r,int at){record=r;slot=at;}
        public void run(){try{synchronized(LOCK){if(slot<2&&record.done)return;if(slot==2&&(current!=record||record.foregroundEnded))return;}Reading value=reading();synchronized(LOCK){if(record.readings[slot]==null)record.readings[slot]=value;}publish(record.id);}catch(Throwable optional){}}
    }
    private static Reading reading(){
        long used=-1,nativeHeap=-1;int thermal=-1;
        try{Runtime r=Runtime.getRuntime();used=r.totalMemory()-r.freeMemory();}catch(Throwable optional){}
        try{nativeHeap=Debug.getNativeHeapAllocatedSize();}catch(Throwable optional){}
        try{Context c;synchronized(LOCK){c=app;}if(c!=null){Object power=c.getSystemService("power");if(power!=null){Object value=Class.forName("android.os.PowerManager").getMethod("getCurrentThermalStatus").invoke(power);if(value instanceof Integer){int n=((Integer)value).intValue();if(n>=0&&n<=6)thermal=n;}}}}catch(Throwable unavailable){}
        return new Reading(System.nanoTime(),used,nativeHeap,thermal);
    }
    private static void complete(Record record,String reason,long now){
        try{boolean stop=false;synchronized(LOCK){if(record.done)return;record.done=true;record.reason=reason;record.ended=now;record.uiSilence=record.lastUi>0?Math.max(0L,now-record.lastUi):-1;record.cameraSilence=record.lastCamera>0?Math.max(0L,now-record.lastCamera):-1;if(record.expiry!=null)record.expiry.cancel(false);if(current==record&&System.nanoTime()>=preUntil){watching=false;stop=true;}}
            WORK.execute(new Probe(record,2));if(stop)stopPump();publish(record.id);
        }catch(Throwable optional){}
    }
    private static Record find(long id){for(Record r:RECORDS)if(r.id==id)return r;return null;}
    private static void publish(long id){
        ProcessingTiming1947.Trace trace;String result;
        synchronized(LOCK){Record r=find(id);if(r==null)return;trace=r.trace.get();if(trace==null)return;result=render(r);}
        ProcessingTiming1947.responsiveness19122(trace,result);
    }
    private static String render(Record r){
        StringBuilder out=new StringBuilder(1400);
        out.append("操作応答の観測: ").append(r.reason).append(" / 撮影記録 #").append(r.id).append("\n").append(r.policy);
        out.append("\n撮影観測開始→初回通知: メイン ").append(firstNotice(r,r.firstUi)).append(" / カメラ入力 ").append(firstNotice(r,r.firstCamera));
        out.append("\nメインフレームコールバック受付間隔: 撮影前最大2秒 ").append(r.beforeUi.text()).append(" / 撮影中 ").append(r.ui[0].text());
        out.append("\nカメラ入力通知間隔: 撮影前最大2秒 ").append(r.beforeCamera.text()).append(" / 撮影中 ").append(r.camera[0].text());
        if(r.saved>0){for(int i=1;i<=10;i++){out.append("\n保存公開後 ").append(i-1).append('～').append(i).append("秒: メイン ").append(r.ui[i].text()).append(" / カメラ入力 ").append(r.camera[i].text());}}
        out.append("\n保存後のメイン通知: 待ち ").append(duration(r.terminalQueued,r.terminalStart)).append(" / 実行 ").append(duration(r.terminalStart,r.terminalEnd)).append(" / 公開→通知終了 ").append(duration(r.saved,r.terminalEnd));
        out.append("\n保存後の資源整理: ").append(r.cleanup);
        String[] labels={"撮影開始後","保存公開後","観測終了時"};String[] thermal={"なし","軽度","中程度","重度","危険","緊急","停止"};
        for(int i=0;i<r.readings.length;i++){Reading v=r.readings[i];out.append("\n").append(labels[i]).append("のメモリ・熱状態: ");if(v==null){out.append("未観測");continue;}
            out.append("撮影開始から ").append(ms(Math.max(0L,v.at-r.start))).append(" ms / Java使用 ").append(v.javaBytes<0?"未観測":mib(v.javaBytes)+" MiB").append(" / native heap確保 ").append(v.nativeBytes<0?"未観測":mib(v.nativeBytes)+" MiB").append(" / thermal ").append(v.thermal<0?"未観測":thermal[v.thermal]+" ("+v.thermal+")");}
        if(r.done)out.append("\n観測終了までの最後の通知からの無通知時間: メイン ").append(r.uiSilence<0?"未観測":ms(r.uiSilence)+" ms").append(" / カメラ入力 ").append(r.cameraSilence<0?"未観測":ms(r.cameraSilence)+" ms");
        out.append("\n※受付間隔は実際の表示完了・FPS・GPU使用率ではありません。p95/p99は1ms刻みの上限値。保存後の区間は通知受信時刻で分類し、区間をまたぐ間隔を含みます。撮影前は直近の短い観測窓に限り、ない場合は未観測です。");
        out.append("\n※カメラ入力は同一provider・撮影世代の通知。現在providerの通常照合は5秒間隔、観測窓内の追加照合は最大毎秒1回、切替 ").append(r.providerChanges).append("回。画像や表示内容を読みません。Java/native heapは別の領域で、GPUメモリやアプリ総常駐量ではありません。thermalはOS報告で、周波数低下や原因の確定ではありません。");
        return out.toString();
    }
    private static String firstNotice(Record record,long first){
        if(first>=record.start&&first>0)return ms(first-record.start)+" ms";
        long until=Math.min(record.deadline,record.done?record.ended:System.nanoTime());
        return "未観測（観測窓 "+ms(Math.max(0L,until-record.start))+" ms）";
    }
    private static String quantile(long n){return n==-1?"未観測":n==-2?">255 ms":"≤"+n+" ms";}
    private static String duration(long from,long to){return from>0&&to>=from?ms(to-from)+" ms":"未観測";}
    private static String ms(long nanos){return Long.toString(Math.max(0L,nanos)/1000000L);}
    private static String mib(long bytes){return bytes/1048576L+"."+((bytes%1048576L)*10L/1048576L);}
    private static String bounded(String s,int max){return s==null?"未観測":s.length()>max?s.substring(0,max):s;}
}
