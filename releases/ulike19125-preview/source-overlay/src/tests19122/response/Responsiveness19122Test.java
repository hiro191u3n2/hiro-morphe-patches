package com.hiro.ulike;
import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.view.Choreographer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public final class Responsiveness19122Test {
    static int assertions;
    static void ck(boolean yes,String message){assertions++;if(!yes)throw new AssertionError(message);}
    static Object field(String name)throws Exception{Field f=Responsiveness19122.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    static void set(String name,Object value)throws Exception{Field f=Responsiveness19122.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    static Responsiveness19122.Record record()throws Exception{return (Responsiveness19122.Record)field("current");}
    static Object invoke(String name,Class<?>[] types,Object...args)throws Exception{Method m=Responsiveness19122.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}
    static void drainProbe(Responsiveness19122.Record r,int slot)throws Exception{long end=System.nanoTime()+1000000000L;while(r.readings[slot]==null&&System.nanoTime()<end)Thread.sleep(1);ck(r.readings[slot]!=null,"bounded background probe completes "+slot);}
    static String rendered(Responsiveness19122.Record r)throws Exception{return (String)invoke("render",new Class<?>[]{Responsiveness19122.Record.class},r);}
    static ProcessingTiming1947.Trace newPreviewTrace(long id)throws Exception{
        ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(id);Responsiveness19122.captured19122(t);Handler.drain();return t;
    }
    static void previewObservations()throws Exception{
        ProcessingTiming1947.Trace t=newPreviewTrace(1000);Responsiveness19122.Record r=record();Object p=new Object();
        Responsiveness19122.cameraOwner19122(p,100,true);long ready=System.nanoTime(),returned=System.nanoTime();
        Responsiveness19122.previewRequested19125(null,ready,returned);
        Responsiveness19122.previewRequested19125(new ProcessingTiming1947.Trace(9999),ready,returned);
        Responsiveness19122.previewRequested19125(t,r.start-1,returned);
        Responsiveness19122.previewRequested19125(t,returned,returned-1);
        Responsiveness19122.previewRequested19125(t,ready,System.nanoTime()+1000000000L);
        ck(r.previewRequested19125==0,"invalid/foreign preview request cannot attach an API completion");
        Responsiveness19122.previewRequested19125(t,ready,returned);
        ck(r.previewInput19125==ready&&r.previewRequested19125==returned&&r.previewOwner19125.get()==p,"release request preserves exact capture and verified owner at API return");
        Responsiveness19122.previewRequested19125(t,System.nanoTime(),System.nanoTime());
        ck(r.previewInput19125==ready&&r.previewRequested19125==returned,"duplicate preview hook cannot move the observation boundary");
        Responsiveness19122.cameraNotice19122(new Object(),100);Responsiveness19122.cameraNotice19122(p,99);
        ck(r.previewCamera19125==0,"foreign owner and epoch cannot be called post-release camera recovery");
        Responsiveness19122.cameraNotice19122(p,100);long first=r.previewCamera19125;
        ck(first>=returned&&first==r.lastCamera,"same provider first subsequent input is observed independently from presentation");
        Responsiveness19122.cameraNotice19122(p,100);ck(r.previewCamera19125==first,"later input notices cannot replace first post-API notice");
        String text=rendered(r);
        ck(text.contains("要求API完了→次のカメラ入力通知")&&text.contains("表示完了や解除による改善量ではありません"),"request-to-input observation explains its presentation and causation limits");

        t=newPreviewTrace(1001);r=record();Responsiveness19122.cameraOwner19122(p,101,true);Responsiveness19122.cameraNotice19122(p,101);
        ck(r.previewRequested19125==0&&r.previewCamera19125==0&&!rendered(r).contains("HDプレビュー解除要求の観測"),"disabled/skipped release has no request or recovery claim even with input notices");

        t=newPreviewTrace(1002);r=record();Responsiveness19122.cameraOwner19122(p,102,true);ready=System.nanoTime();returned=System.nanoTime();
        Responsiveness19122.cameraNotice19122(p,102);long beforeHook=r.lastCamera;
        Responsiveness19122.previewRequested19125(t,ready,returned);
        ck(r.previewCamera19125==beforeHook&&beforeHook>=returned,"input between API return and optional hook is recovered from the bounded owner ring");

        t=newPreviewTrace(1003);r=record();Responsiveness19122.cameraOwner19122(p,103,false);ready=System.nanoTime();returned=System.nanoTime();
        Responsiveness19122.previewRequested19125(t,ready,returned);
        ck(r.previewOwner19125.get()==null,"unknown owner at API completion remains unknown");
        Responsiveness19122.cameraOwner19122(p,103,true);Responsiveness19122.cameraNotice19122(p,103);
        ck(r.previewCamera19125==0&&rendered(r).contains("要求時provider未確認"),"later owner validation cannot manufacture an observed request owner");

        t=newPreviewTrace(1004);r=record();Responsiveness19122.cameraOwner19122(p,104,true);
        set("cameraVerifiedAt",Long.valueOf(System.nanoTime()-7000000000L));ready=System.nanoTime();returned=System.nanoTime();
        Responsiveness19122.previewRequested19125(t,ready,returned);Responsiveness19122.cameraOwner19122(p,104,true);Responsiveness19122.cameraNotice19122(p,104);
        ck(r.previewCamera19125==0&&r.previewOwner19125.get()==null,"expired owner proof at request cannot recover merely through later verification");

        t=newPreviewTrace(1005);r=record();Responsiveness19122.cameraOwner19122(p,105,true);ready=System.nanoTime();returned=System.nanoTime();
        Object q=new Object();Responsiveness19122.cameraOwner19122(q,106,true);Responsiveness19122.cameraNotice19122(q,106);
        Responsiveness19122.previewRequested19125(t,ready,returned);Responsiveness19122.cameraNotice19122(q,106);
        ck(r.previewOwner19125.get()==null&&r.previewCamera19125==0,"provider first verified after API return cannot be attributed to the earlier request");

        t=newPreviewTrace(1006);r=record();Responsiveness19122.cameraOwner19122(p,106,true);ready=System.nanoTime();returned=System.nanoTime();
        Responsiveness19122.previewRequested19125(t,ready,returned);Responsiveness19122.cameraOwner19122(q,107,true);Responsiveness19122.cameraNotice19122(q,107);
        Responsiveness19122.cameraOwner19122(p,106,true);Responsiveness19122.cameraNotice19122(p,106);
        ck(r.previewCamera19125==0&&rendered(r).contains("provider切替"),"provider switch and switch-back cannot bridge request ownership");

        t=newPreviewTrace(1007);r=record();Responsiveness19122.cameraOwner19122(p,107,true);ready=System.nanoTime();returned=System.nanoTime();
        Responsiveness19122.previewRequested19125(t,ready,returned);Responsiveness19122.cameraOwner19122(p,108,true);Responsiveness19122.cameraNotice19122(p,108);
        ck(r.previewCamera19125==0,"same provider object with changed camera epoch cannot satisfy prior release request");

        t=newPreviewTrace(1011);r=record();Responsiveness19122.cameraOwner19122(p,111,true);ready=System.nanoTime();returned=System.nanoTime();
        Responsiveness19122.previewRequested19125(t,ready,returned);Responsiveness19122.cameraOwner19122(p,111,false);Responsiveness19122.cameraOwner19122(p,111,true);Responsiveness19122.cameraNotice19122(p,111);
        ck(r.previewCamera19125==0&&rendered(r).contains("provider切替"),"invalidated owner revalidated as same object cannot bridge unknown camera interval");

        t=newPreviewTrace(1012);r=record();Responsiveness19122.cameraOwner19122(p,112,true);ready=System.nanoTime();returned=System.nanoTime();
        Responsiveness19122.previewRequested19125(t,ready,returned);r.previewOwner19125.clear();Responsiveness19122.cameraNotice19122(p,112);
        ck(r.previewCamera19125==0&&rendered(r).contains("参照終了"),"expired weak request-owner reference remains unknown even if a current provider exists");

        t=newPreviewTrace(1013);r=record();Responsiveness19122.cameraOwner19122(p,113,true);ready=System.nanoTime();returned=System.nanoTime();
        for(int i=0;i<260;i++)Responsiveness19122.cameraNotice19122(p,113);
        Responsiveness19122.previewRequested19125(t,ready,returned);
        ck(r.previewHistoryLost19125&&r.previewCamera19125==0&&rendered(r).contains("保持範囲外"),"bounded ring overwrite cannot substitute a later notice for a lost first notice");
        Responsiveness19122.cameraNotice19122(p,113);ck(r.previewCamera19125==0,"later live notice cannot turn lost first-notice history into a measured recovery");

        ProcessingTiming1947.Trace previous=t;Responsiveness19122.Record old=r;
        t=newPreviewTrace(1008);r=record();Responsiveness19122.previewRequested19125(previous,System.nanoTime(),System.nanoTime());Responsiveness19122.cameraNotice19122(p,108);
        ck(old.done&&old.previewCamera19125==0&&r.previewRequested19125==0&&r.previewCamera19125==0,"old hook and next-shot notices cannot update either photo as old release recovery");

        t=newPreviewTrace(1009);r=record();Responsiveness19122.cameraOwner19122(p,109,true);r.deadline=r.start;
        Responsiveness19122.previewRequested19125(t,System.nanoTime(),System.nanoTime());
        ck(r.previewRequested19125==0,"request outside bounded observation window is rejected before expiry task runs");

        t=newPreviewTrace(1010);r=record();Responsiveness19122.cameraOwner19122(p,110,true);ready=System.nanoTime();returned=System.nanoTime();Responsiveness19122.previewRequested19125(t,ready,returned);
        invoke("complete",new Class<?>[]{Responsiveness19122.Record.class,String.class,long.class},r,"preview test complete",Long.valueOf(System.nanoTime()));Responsiveness19122.cameraNotice19122(p,110);
        ck(r.previewCamera19125==0,"completed window cannot record a later notice as recovery");
        Responsiveness19122.Record completed=r;newPreviewTrace(1014);Responsiveness19122.cameraOwner19122(q,114,true);
        ck(!completed.previewInvalidated19125&&!rendered(completed).contains("provider切替"),"later photo provider change cannot alter completed photo request status");
    }
    public static void main(String[] args)throws Exception{
        Responsiveness19122.Gaps gaps=new Responsiveness19122.Gaps();
        ck(gaps.percentile(95)==-1,"no intervals means unknown, not zero lag");
        gaps.add(-1);gaps.add(0);ck(gaps.count==0,"invalid/non-forward intervals ignored");
        for(int i=0;i<100;i++)gaps.add(16666667L);
        ck(gaps.count==100&&gaps.percentile(95)==17&&gaps.percentile(99)==17,"histogram has documented upper bounds");
        gaps.add(700000000L);ck(gaps.max==700000000L,"max preserves overflow value");
        for(int i=0;i<20;i++)gaps.add(900000000L);
        ck(gaps.percentile(95)==-2&&gaps.text().contains(">255 ms"),"long stall cannot masquerade as short p95");
        Responsiveness19122.Ring ring=new Responsiveness19122.Ring();
        for(int i=0;i<600;i++)ring.add(10000000000L+i*10000000L);
        ck(ring.count==256,"pre-capture timestamp ring bounded");
        ck(ring.before(16000000000L).count==199,"only two-second recent window is used");
        ck(ring.before(19000000000L).count==0,"stale baseline is unknown");
        ring.clear();ck(ring.count==0,"camera epoch switch clears stale baseline");
        ck(ring.firstAtOrAfter19125(10,20)==0,"empty ring has no post-API notice");
        ring.add(10);ring.add(15);ring.add(20);
        ck(ring.firstAtOrAfter19125(15,20)==15&&ring.firstAtOrAfter19125(16,20)==20&&ring.firstAtOrAfter19125(16,19)==0,"ring selects first notice within inclusive API/hook bounds");
        ring.clear();for(int i=1;i<=300;i++)ring.add(i);
        ck(ring.firstAtOrAfter19125(1,300)==-1&&ring.firstAtOrAfter19125(44,300)==-1,"overwritten first notice is explicitly unknown");
        ck(ring.firstAtOrAfter19125(45,300)==45&&ring.firstAtOrAfter19125(300,300)==300,"retained ring tail remains measurable at boundary");
        ring.clear();ck(ring.firstAtOrAfter19125(1,300)==0,"owner clear removes prior overwrite marker");
        long syntheticStart=10000000000L;
        Responsiveness19122.Record delayed=new Responsiveness19122.Record(new ProcessingTiming1947.Trace(700),syntheticStart,700);
        delayed.notice(true,syntheticStart+2000000000L);
        delayed.notice(true,syntheticStart+2016666667L);
        delayed.notice(false,syntheticStart+3000000000L);
        delayed.notice(false,syntheticStart+3033333334L);
        ck(delayed.firstUi==syntheticStart+2000000000L&&delayed.firstCamera==syntheticStart+3000000000L,"first arrival delay survives later regular intervals");
        ck(delayed.ui[0].count==1&&delayed.ui[0].percentile(95)==17,"first delay is separate from callback interval histogram");
        String delayedText=(String)invoke("render",new Class<?>[]{Responsiveness19122.Record.class},delayed);
        ck(delayedText.contains("初回通知: メイン 2000 ms / カメラ入力 3000 ms"),"initial two-second UI stall appears explicitly in public diagnostics");
        Responsiveness19122.Record absent=new Responsiveness19122.Record(new ProcessingTiming1947.Trace(701),syntheticStart,701);
        absent.done=true;absent.ended=syntheticStart+12000000000L;
        String absentText=(String)invoke("render",new Class<?>[]{Responsiveness19122.Record.class},absent);
        ck(absentText.contains("初回通知: メイン 未観測（観測窓 12000 ms） / カメラ入力 未観測（観測窓 12000 ms）"),"no initial callback reports unknown with elapsed observation window");
        absent.ended=syntheticStart+100000000000L;
        absentText=(String)invoke("render",new Class<?>[]{Responsiveness19122.Record.class},absent);
        ck(absentText.contains("初回通知: メイン 未観測（観測窓 60000 ms）"),"no-callback observation duration respects hard collection deadline");
        Application app=new Application();Activity activity=new Activity(app);
        Responsiveness19122.init19122(activity);ck(app.callbacks!=null,"public lifecycle registered");
        Object callbacks=app.callbacks;Responsiveness19122.init19122(activity);ck(app.callbacks==callbacks,"single lifecycle registration");
        Responsiveness19122.foreground19122();Handler.drain();ck(Choreographer.callback!=null,"bounded foreground pre-sample armed");
        Choreographer.fire();ck(((Responsiveness19122.Ring)field("PRE_UI")).count==1,"real main frame callback arrival recorded");
        activity.focus=false;Choreographer.fire();ck(((Responsiveness19122.Ring)field("PRE_UI")).count==1,"hidden callback does not count as visible UI response");
        activity.focus=true;set("preUntil",Long.valueOf(0L));Choreographer.fire();ck(!((Boolean)field("watching")),"pre-sample stops when bounded window expires");
        ck(Choreographer.callback==null,"no perpetual frame callback after foreground sample");
        ProcessingTiming1947.Trace trace=new ProcessingTiming1947.Trace(11);
        Responsiveness19122.captured19122(trace);Handler.drain();Responsiveness19122.Record r=record();
        ck(r.id==11&&!r.done,"capture creates exact record");
        Responsiveness19122.captured19122(trace);ck(record()==r,"duplicate capture event does not reset sample");
        drainProbe(r,0);ck(r.readings[0].nativeBytes==123456L&&r.readings[0].thermal==3,"native heap and public thermal values sampled");
        ck(android.os.Process.nice==10,"sensor worker sets background priority");
        Responsiveness19122.policy19122(trace);ck(trace.summary.contains("mode-normal")&&trace.summary.contains("CPU上限 2"),"capture policy snapshot labelled exact photo");
        Object provider=new Object();
        Responsiveness19122.cameraNotice19122(provider,9);ck(r.lastCamera==0,"unverified provider notice ignored");
        ck(Responsiveness19122.requestCameraOwner19122(provider,9),"first window can request owner lookup");
        for(int i=0;i<10;i++)ck(!Responsiveness19122.requestCameraOwner19122(new Object(),9),"foreign notices cannot force per-frame reflective lookups");
        Responsiveness19122.cameraOwner19122(provider,9,true);
        Responsiveness19122.cameraNotice19122(provider,8);ck(r.lastCamera==0,"wrong epoch rejected");
        Responsiveness19122.cameraNotice19122(new Object(),9);ck(r.lastCamera==0,"wrong identity rejected");
        Responsiveness19122.cameraNotice19122(provider,9);ck(r.lastCamera>0,"verified current provider accepted");
        long accepted=r.lastCamera;Responsiveness19122.cameraOwner19122(provider,9,false);Responsiveness19122.cameraNotice19122(provider,9);ck(r.lastCamera==accepted,"known stale provider immediately rejected");
        Object newProvider=new Object();Responsiveness19122.cameraOwner19122(provider,9,true);Responsiveness19122.cameraOwner19122(newProvider,10,true);
        ck(r.providerChanges==1&&r.lastCamera==0,"provider switch separates gaps instead of bridging epochs");
        set("cameraVerifiedAt",Long.valueOf(System.nanoTime()-7000000000L));Responsiveness19122.cameraNotice19122(newProvider,10);ck(r.lastCamera==0,"expired ownership cannot count frames");
        Choreographer.fire();ck(r.lastUi>0,"capture records actual UI callback, independently of camera");
        long firstUi=r.lastUi;
        r.notice(true,firstUi+20000000L);ck(r.ui[0].count==1,"during-capture interval recorded");
        Responsiveness19122.published19122(trace);long saved=r.saved;ck(saved>0&&r.deadline==saved+10000000000L,"post window anchored to actual publication observation");
        Responsiveness19122.published19122(trace);ck(r.saved==saved,"duplicate publication does not extend window");
        drainProbe(r,1);
        r.lastUi=saved-10000000L;r.notice(true,saved+10000000L);ck(r.ui[1].count==1,"cross-publication gap assigned by receiving timestamp");
        r.notice(true,saved+1100000000L);ck(r.ui[2].count==1,"next second classified separately");
        long count=r.ui[10].count;r.notice(true,saved+10000000000L);ck(r.ui[10].count==count,"exact post deadline is exclusive");
        r.notice(true,saved+20000000000L);ck(r.ui[10].count==count,"after-deadline notices ignored");
        Responsiveness19122.terminalQueued19122(trace);long queued=r.terminalQueued;
        Responsiveness19122.terminalQueued19122(trace);ck(r.terminalQueued==queued,"terminal enqueue first occurrence wins");
        Responsiveness19122.terminalStarted19122(trace);Responsiveness19122.terminalFinished19122(trace);
        ck(r.terminalStart>=r.terminalQueued&&r.terminalEnd>=r.terminalStart,"terminal callback boundaries ordered");
        Responsiveness19122.cleanup19122(999,"foreign",1,1);ck(r.cleanup.equals("未観測"),"foreign cleanup cannot attach to latest trace");
        Responsiveness19122.cleanup19122(trace.id,"idle only",3000000L,1048576L);ck(r.cleanup.contains("1.0 MiB")&&trace.summary.contains("OSへの返却量ではない"),"cleanup reference bytes never called resident memory reduction");
        Responsiveness19122.cleanup19122(trace.id,"unknown",1,-1);ck(r.cleanup.contains("参照解除 未観測"),"unknown released byte count never becomes zero");
        Choreographer.FrameCallback stale=Choreographer.callback;
        ProcessingTiming1947.Trace next=new ProcessingTiming1947.Trace(12);Responsiveness19122.captured19122(next);Handler.drain();Responsiveness19122.Record r2=record();
        ck(r.done&&r.reason.contains("次の撮影"),"new capture terminates prior window");
        ck(r.expiry.isCancelled(),"superseded expiry cancelled");
        ck(r2!=r&&!r2.done,"new generation remains active");
        long ui2=r2.lastUi;if(stale!=null)stale.doFrame(1);ck(r2.lastUi==ui2,"stale main callback cannot update new capture");
        ck(r.readings[2]==null,"superseded photo does not sample next capture memory");
        drainProbe(r2,0);
        android.os.Debug.fail=true;android.os.PowerManager.fail=true;
        Responsiveness19122.Reading unavailable=(Responsiveness19122.Reading)invoke("reading",new Class<?>[0]);
        ck(unavailable.nativeBytes==-1&&unavailable.thermal==-1&&unavailable.javaBytes>=0,"optional sensor failures stay unknown and preserve Java sample");
        android.os.Debug.fail=false;android.os.PowerManager.fail=false;
        android.os.PowerManager.status=8;Responsiveness19122.Reading invalid=(Responsiveness19122.Reading)invoke("reading",new Class<?>[0]);ck(invalid.thermal==-1,"unknown thermal enum is not asserted as valid");android.os.PowerManager.status=0;
        app.callbacks.onActivityPaused(new Activity(app));ck(!r2.done,"unrelated Activity pause cannot stop current measurement");
        app.callbacks.onActivityPaused(activity);Handler.drain();ck(r2.done&&r2.foregroundEnded,"foreground owner pause ends window");
        ck(Choreographer.callback==null&&!((Boolean)field("watching")),"background ends frame callback work");
        Responsiveness19122.captured19122(new ProcessingTiming1947.Trace(13));Handler.drain();Responsiveness19122.Record r3=record();
        invoke("complete",new Class<?>[]{Responsiveness19122.Record.class,String.class,long.class},r3,"test deadline",Long.valueOf(System.nanoTime()));Handler.drain();
        ck(r3.done&&!((Boolean)field("watching")),"bounded deadline stops sample even without UI callbacks");
        ck(r3.uiSilence==-1&&r3.cameraSilence==-1,"no notifications remains unobserved, not perfect responsiveness");
        ck(Choreographer.callback==null,"deadline removes pending frame callback");
        drainProbe(r3,2);
        ck(r3.trace.get().summary.contains("実際の表示完了・FPS・GPU使用率ではありません"),"output documents measurement boundary");
        ck(r3.trace.get().summary.contains("GPUメモリやアプリ総常駐量ではありません"),"heap values are not mislabelled process resident memory");
        for(int i=20;i<35;i++)Responsiveness19122.captured19122(new ProcessingTiming1947.Trace(i));
        ck(((ArrayDeque<?>)field("RECORDS")).size()==8,"history bounded despite repeated captures");
        Responsiveness19122.failed19122(record().trace.get());Handler.drain();ck(record().done&&record().reason.contains("保存失敗"),"failed capture ends window");
        previewObservations();
        ((ScheduledThreadPoolExecutor)field("WORK")).shutdownNow();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"negative_controls\":{\"stale_generation\":true,\"hidden_activity\":true,\"wrong_provider_epoch\":true,\"foreign_provider\":true,\"unavailable_native_thermal_api\":true,\"post_deadline\":true,\"histogram_overflow\":true,\"superseded_memory_sample\":true}}");
    }
}
