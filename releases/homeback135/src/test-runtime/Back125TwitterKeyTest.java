package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import java.util.Arrays;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.Back124TwitterTest.*;

/** Legacy key/public-onBackPressed ordering plus the actual emitted i.dispatchKeyEvent and
 * p.onBackPressed wrapper structure. This is a host regression, not a device
 * execution claim. The Activity double supplies the framework interaction. */
public final class Back125TwitterKeyTest {
    private static boolean reproduceOldRuntime;
    static final class PatchedMain extends com.twitter.app.main.MainActivity {
        PatchedMain(ProcessApp app){super(app,11,true);}
        @Override public void onUserInteraction(){OneBackExit.onUserInteraction(this);super.onUserInteraction();}
        @Override public boolean dispatchKeyEvent(KeyEvent event){
            if(OneBackExit.key(this,event))return true;
            OneBackExit.enterKey(this,event);
            try{boolean consumed=super.dispatchKeyEvent(event);OneBackExit.afterNative();return consumed;}
            finally{OneBackExit.leaveNative();}
        }
        @Override public void onBackPressed(){
            if(OneBackExit.tryBack(this))return;
            // The optional old-runtime reproducer retains the released v124
            // wrapper ABI; ordinary regression tests use the v125 wrapper.
            if(reproduceOldRuntime)OneBackExit.enterBack(this);
            else OneBackExit.enterTwitterBack(this);
            try{super.onBackPressed();OneBackExit.afterNative();}
            finally{OneBackExit.leaveNative();}
        }
    }
    static final class KeyScene implements AutoCloseable {
        final Disk disk=new Disk();final ProcessApp app=new ProcessApp(disk);
        final PatchedMain activity=new PatchedMain(app);
        final ActivityManager manager=new ActivityManager();final ActivityManager.AppTask task;
        final ViewGroup root;final View home;
        String current;String guard;int hookCalls,homeSwitches,guardCalls;Runnable beforeHook;
        KeyScene(String tab){
            app.setService(Context.ACTIVITY_SERVICE,manager);root=(ViewGroup)activity.getWindow().getDecorView();
            home=new View(activity);home.description="ホーム";home.clickable=true;home.bounds=new Rect(0,1990,190,2170);
            home.clickAction=()->select(HOME);root.addView(home);select(tab);
            ActivityManager.RecentTaskInfo info=new ActivityManager.RecentTaskInfo();info.taskId=11;
            info.baseActivity=new ComponentName(X,"com.twitter.app.main.MainActivity");info.baseIntent=new Intent().setComponent(info.baseActivity);
            task=new ActivityManager.AppTask(info);task.activities.add(activity);manager.tasks.add(task);
            activity.nativeBackAction=()->nativeBack();OneBackExit.install(activity);activity.getWindow().attach();root.focus(true);OneBackExit.onResume(activity);
        }
        void select(String tab){current=tab;home.selected=HOME.equals(tab);}
        void nativeBack(){
            if(guard!=null){guardCalls++;guard=null;return;}
            if(beforeHook!=null)beforeHook.run();
            hookCalls++;
            if(OneBackExit.tryTwitterRootBack(activity,Uri.parse(current)))return;
            if(!HOME.equals(current)){homeSwitches++;select(HOME);}
            // Ordinary native platform exit retains the task when the hook is
            // rejected. Model it distinctly from finishAndRemoveTask.
            else activity.finish();
        }
        void keyBack(){activity.dispatchKeyEvent(down());activity.dispatchKeyEvent(up());}
        void preserved(){check(task.removeCalls==0&&!task.excluded&&!activity.finishing,"native consumption preserves task");}
        void removed(){check(task.removeCalls==1&&task.excluded,"platform key Back excludes and removes task");
            check(task.events.equals(Arrays.asList("exclude","remove")),"exclusion happens before task removal");
            check(homeSwitches==0,"native root exit does not switch Home first");}
        @Override public void close(){OneBackExit.onDestroy(activity);activity.destroyed=true;root.detach();Handler.drain();android.view.inspector.WindowInspector.clear();}
    }
    public static void main(String[] args)throws Exception {
        if(args.length==1&&"--expect-old-rejection".equals(args[0])){
            reproduceOldRuntime=true;
            int rejected=0;
            for(String tab:TABS)try(KeyScene s=new KeyScene(tab)){
                s.keyBack();
                if(s.activity.userInteractionCalls==2&&s.hookCalls==1&&s.task.removeCalls==0&&!marker(s.app))rejected++;
            }
            check(rejected==4,"all four v1.1.14 root hooks reject framework key-up interaction");
            System.out.println("REPRODUCED v1.1.14: 4/4 root tabs reject key-up; no task removal and no Home marker");return;
        }
        int oldCases=cases,oldAssertions=assertions;
        for(final String tab:TABS)test("x125_platform_key_exit_and_cold_home_"+tab,()->{
            Disk persisted;
            try(KeyScene s=new KeyScene(tab)){
                persisted=s.disk;s.keyBack();
                check(s.activity.userInteractionCalls==2,"framework interaction runs on key down AND key up");
                check(s.hookCalls==1&&s.activity.nativeBackCalls==1,"one original native Back reaches root");s.removed();
                check(marker(s.app),"successful platform key exit persists Home marker");
            }
            try(Scene next=new Scene(new ProcessApp(persisted),tab,launcher(),true)){
                check(HOME.equals(next.current)&&HOME.equals(next.activity.getIntent().getStringExtra("page")),"cold launcher uses native explicit Home route");
                Handler.drain();check(!marker(next.app),"observed Home consumes persisted marker");next.preserved();
            }
        });
        for(final String layer:new String[]{"fab","drawer","typeahead","onboarding","child"})test("x125_platform_key_native_"+layer+"_consumes",()->{
            try(KeyScene s=new KeyScene(CHAT)){s.guard=layer;s.keyBack();
                check(s.guardCalls==1&&s.hookCalls==0,"native layer consumes before root hook");s.preserved();
                check(!marker(s.app),"consumed child does not request launcher reset");
                s.keyBack();s.removed();check(s.hookCalls==1,"later distinct root Back exits");}
        });
        test("x125_platform_cancelled_key_up",()->{try(KeyScene s=new KeyScene(CHAT)){
            s.activity.dispatchKeyEvent(down());s.activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,true));
            check(s.activity.userInteractionCalls==2&&s.hookCalls==0,"cancel forwards framework interaction without committed Back");s.preserved();
            check(!OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(CHAT)),"cancel leaves no active root authority");
        }});
        for(final String event:new String[]{"interaction","new_intent","resume"})test("x125_after_exit_"+event+"_cancels_removal_retry",()->{
            try(KeyScene s=new KeyScene(CHAT)){
                s.task.silentRemove=true;s.keyBack();s.removed();
                if(event.equals("interaction"))s.activity.onUserInteraction();
                else if(event.equals("new_intent"))OneBackExit.onNewIntent(s.activity,new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://x.com/i/chat")));
                else OneBackExit.onResume(s.activity);
                Handler.drain();check(s.task.removeCalls==1,"new user navigation cancels queued task removal");
            }
        });
        test("x125_interaction_after_consumed_key_cancels_pending",()->{try(KeyScene s=new KeyScene(CHAT)){
            s.guard="child";s.keyBack();check(TwitterBack.pending(s.activity),"successful native key scope creates bounded terminal ticket");
            s.activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_UP));
            check(!TwitterBack.pending(s.activity),"ordinary later key still invalidates stale terminal evidence");s.preserved();
        }});
        for(final String event:new String[]{"new_intent","resume","leave","other_owner"})test("x125_explicit_"+event+"_invalidates_nested_scope",()->{
            try(KeyScene s=new KeyScene(CHAT)){
                s.beforeHook=()->{
                    if(event.equals("new_intent"))OneBackExit.onNewIntent(s.activity,new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://x.com/i/chat")));
                    else if(event.equals("resume"))OneBackExit.onResume(s.activity);
                    else if(event.equals("leave"))OneBackExit.onUserLeaveHint(s.activity);
                    else OneBackExit.onUserInteraction(new com.twitter.app.main.MainActivity(s.app,12,true));
                };
                s.keyBack();check(s.hookCalls==1&&s.homeSwitches==1,"distinct lifecycle/navigation event still rejects obsolete root authority");
                s.preserved();check(!marker(s.app),"rejected stale scope does not persist Home marker");
            }
        });
        test("x125_exception_clears_key_and_nested_back_scope",()->{try(KeyScene s=new KeyScene(CHAT)){
            s.beforeHook=()->{throw new IllegalStateException("original Back failed");};
            boolean threw=false;try{s.keyBack();}catch(IllegalStateException expected){threw=true;}
            check(threw,"original exception is preserved");s.preserved();
            check(!OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(CHAT)),"exception finally removes active Back authority");
            s.beforeHook=null;s.keyBack();s.removed();
        }});
        System.out.println("PASS Back125TwitterKeyTest: "+(cases-oldCases)+" cases, "+(assertions-oldAssertions)+" assertions");
    }
}
