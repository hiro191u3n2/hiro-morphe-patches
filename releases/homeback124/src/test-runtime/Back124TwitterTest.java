package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Host contract tests. Native l1.goBack guard order and patched Activity Back
 * scope are independently verified in the real APK; this is not a device test. */
public final class Back124TwitterTest {
    static final String HOME="twitter://timeline/home",SEARCH="twitter://moments",
        NOTIFICATIONS="twitter://notifications",CHAT="twitter://xchat";
    static final String[] TABS={HOME,SEARCH,NOTIFICATIONS,CHAT};
    static final String PREFS="app.hiro.oneback.twitter",RESET="home_after_exit";
    /** A persistent preference store is shared by distinct Application/process
     * models, rather than reusing TwitterBack's per-Application Session. */
    static final class Disk {
        final Map<String,SharedPreferences> files=new HashMap<String,SharedPreferences>();
        SharedPreferences open(String name){
            SharedPreferences prefs=files.get(name);if(prefs!=null)return prefs;
            prefs=new SharedPreferences(){
                final Map<String,Boolean> values=new HashMap<String,Boolean>();
                public boolean getBoolean(String key,boolean fallback){return values.containsKey(key)?values.get(key):fallback;}
                public Editor edit(){return new Editor(){final Map<String,Boolean> changes=new HashMap<String,Boolean>();
                    public Editor putBoolean(String key,boolean value){changes.put(key,value);return this;}
                    public boolean commit(){values.putAll(changes);return true;}};}
            };files.put(name,prefs);return prefs;
        }
    }
    static final class ProcessApp extends Application {
        final Disk disk;
        ProcessApp(Disk value){super(X);disk=value;}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){return disk.open(name);}
    }
    static boolean marker(Application app){return app.getSharedPreferences(PREFS,0).getBoolean(RESET,false);}
    static Intent launcher(){return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);}
    static final class Scene implements AutoCloseable {
        final Application app;final com.twitter.app.main.MainActivity activity;
        final ActivityManager manager=new ActivityManager();final ActivityManager.AppTask task;
        final ViewGroup root;final View home;String current;
        boolean fab,drawer,typeahead,onboarding,child;
        int hookCalls,homeSwitches,childPops;
        Scene(Application owner,String initial,Intent intent,boolean nativeStartup){
            app=owner;app.setService(Context.ACTIVITY_SERVICE,manager);activity=new com.twitter.app.main.MainActivity(app,11,true);
            activity.setIntent(intent);root=(ViewGroup)activity.getWindow().getDecorView();
            home=new View(activity);home.description="ホーム";home.clickable=true;home.bounds=new Rect(0,1990,190,2170);
            home.clickAction=()->select(HOME);root.addView(home);select(initial);
            ActivityManager.RecentTaskInfo info=new ActivityManager.RecentTaskInfo();info.taskId=11;
            info.baseActivity=new ComponentName(X,"com.twitter.app.main.MainActivity");info.baseIntent=new Intent().setComponent(info.baseActivity);
            task=new ActivityManager.AppTask(info);task.activities.add(activity);manager.tasks.add(task);
            // Production onCreate hook runs before l1.I3's explicit-page reader.
            OneBackExit.install(activity);
            if(nativeStartup)readNativeStartup(initial);
            activity.nativeBackAction=()->nativeBack();activity.getWindow().attach();root.focus(true);OneBackExit.onResume(activity);
        }
        Scene(String initial){this(new ProcessApp(new Disk()),initial,null,false);}
        void select(String uri){current=uri;home.selected=HOME.equals(uri);}
        void readNativeStartup(String remembered){
            String explicit=activity.getIntent()==null?null:activity.getIntent().getStringExtra("page");
            select(explicit==null?remembered:explicit);
        }
        boolean nativeBack(){
            // These precede PC87 in the unchanged real l1.goBack implementation.
            if(fab){fab=false;return true;}
            if(drawer){drawer=false;return true;}
            if(typeahead){typeahead=false;return true;}
            if(onboarding)return true;
            if(child){child=false;childPops++;return true;}
            hookCalls++;
            if(OneBackExit.tryTwitterRootBack(activity,Uri.parse(current)))return true;
            if(!HOME.equals(current)){homeSwitches++;select(HOME);return true;}
            return false;
        }
        boolean committedBack(){OneBackExit.enterBack(activity);try{return nativeBack();}finally{OneBackExit.leaveNative();}}
        void preserved(){check(!activity.finishing&&task.removeCalls==0&&!task.excluded,"native navigation keeps the task and Recents");}
        void removed(){check(task.excluded&&task.removeCalls==1,"task excluded and removed exactly once");
            check(task.events.equals(Arrays.asList("exclude","remove")),"exclusion precedes task removal");
            check(homeSwitches==0,"no intermediate switch to native Home");}
        @Override public void close(){OneBackExit.onDestroy(activity);activity.destroyed=true;root.detach();Handler.drain();android.view.inspector.WindowInspector.clear();}
    }
    static Map<String,Test> scenarios(){Map<String,Test> cases=new LinkedHashMap<String,Test>();
        for(final String tab:TABS)cases.put("x124_native_exit_and_cold_launcher_home_"+tab,()->{
            Disk disk=new Disk();ProcessApp previous=new ProcessApp(disk);
            try(Scene s=new Scene(previous,tab,null,false)){
                check(!OneBackExit.tryBack(s.activity),"current Main always reaches its native guards");
                check(s.committedBack(),"verified selected root Back consumed");s.removed();check(marker(previous),"every exit tab persists the Home marker");
            }
            ProcessApp next=new ProcessApp(disk);check(next!=previous&&marker(next),"new process model sees persisted marker without old Session state");
            try(Scene s=new Scene(next,tab,launcher(),true)){
                check(HOME.equals(s.activity.getIntent().getStringExtra("page")),"actual Main receives Home extra before its native reader");
                check(HOME.equals(s.current)&&s.home.clickCalls==0,"native startup overrides remembered tab without a synthetic click");
                Handler.drain();check(!marker(next),"marker consumed only after real Home selection is observed");s.preserved();
            }
        });
        for(final String tab:TABS)cases.put("x124_api33_callback_reaches_guarded_root_"+tab,()->{try(Scene s=new Scene(tab)){
            OnBackInvokedCallback original=()->s.nativeBack();NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            try{s.activity.getWindow().dispatcher().invokeBack();check(s.hookCalls==1,"one committed native root callback");s.removed();}
            finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        for(final String guard:new String[]{"fab","drawer","typeahead","onboarding","child"})cases.put("x124_home_native_"+guard+"_precedes_exit",()->{try(Scene s=new Scene(HOME)){
            if(guard.equals("fab"))s.fab=true;else if(guard.equals("drawer"))s.drawer=true;
            else if(guard.equals("typeahead"))s.typeahead=true;else if(guard.equals("onboarding"))s.onboarding=true;else s.child=true;
            check(HomeScreen.isHome(s.activity),"generic selected Home alone would prematurely exit this model");
            check(!OneBackExit.tryBack(s.activity),"generic Home shortcut is disabled for native Main");
            check(s.committedBack()&&s.hookCalls==0,"native consumed/skipped branch never enters root hook");s.preserved();
        }});
        cases.put("x124_chat_child_pop_then_distinct_root_back",()->{try(Scene s=new Scene(CHAT)){
            s.child=true;check(s.committedBack()&&s.childPops==1&&s.hookCalls==0,"conversation detail consumes its own Back");s.preserved();
            check(s.committedBack()&&s.hookCalls==1,"a later independent Back exits Chat root");s.removed();
        }});
        cases.put("x124_native_boundary_ignores_pre_native_input_heuristic",()->{try(Scene s=new Scene(SEARCH)){
            s.root.insets.ime=true;s.root.focus(false);
            check(!HomeScreen.inputReady(s.activity),"generic eligibility is deliberately false");
            check(s.committedBack(),"post-handler native root decision does not reuse Attempt.eligible");s.removed();
        }});
        cases.put("x124_internal_navigator_without_back_scope_stays_native",()->{try(Scene s=new Scene(CHAT)){
            check(!OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(CHAT)),"toolbar/internal navigation has no Android Back owner");s.preserved();
        }});
        cases.put("x124_nested_back_keeps_initial_owner",()->{try(Scene s=new Scene(SEARCH)){
            Activity other=new com.twitter.app.main.MainActivity(s.app,12,true);
            OneBackExit.enterBack(s.activity);try{OneBackExit.enterBack(other);try{
                check(!OneBackExit.tryTwitterRootBack(other,Uri.parse(SEARCH)),"nested wrapper cannot replace original Back owner");
                check(OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(SEARCH)),"initial owner survives nested native wrapper");
            }finally{OneBackExit.leaveNative();}}finally{OneBackExit.leaveNative();}s.removed();
        }});
        cases.put("x124_ended_or_stale_back_scope_cannot_exit",()->{try(Scene s=new Scene(NOTIFICATIONS)){
            OneBackExit.enterBack(s.activity);OneBackExit.leaveNative();
            check(!OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(NOTIFICATIONS)),"ended scope is not authority");
            OneBackExit.enterBack(s.activity);try{OneBackExit.onUserInteraction(s.activity);
                check(!OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(NOTIFICATIONS)),"later interaction invalidates the captured epoch");
            }finally{OneBackExit.leaveNative();}s.preserved();
        }});
        cases.put("x124_unknown_routes_and_missing_uri_stay_native",()->{try(Scene s=new Scene(HOME)){
            OneBackExit.enterBack(s.activity);try{
                for(String route:new String[]{"twitter://grok","twitter://dms","twitter://messages","twitter://search","twitter://xchat/thread/1"})
                    check(!OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(route)),"only verified four native root routes are enabled");
                check(!OneBackExit.tryTwitterRootBack(s.activity,null),"null native selection is not a root proof");
            }finally{OneBackExit.leaveNative();}s.preserved();
        }});
        cases.put("x124_dead_or_changing_owner_stays_native",()->{try(Scene s=new Scene(HOME)){
            OneBackExit.enterBack(s.activity);try{
                s.activity.destroyed=true;check(!OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(HOME)),"destroyed owner rejected");s.activity.destroyed=false;
                s.activity.finishing=true;check(!OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(HOME)),"finishing owner rejected");s.activity.finishing=false;
                s.activity.changingConfigurations=true;check(!OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(HOME)),"configuration transition rejected");s.activity.changingConfigurations=false;
            }finally{OneBackExit.leaveNative();}s.preserved();
        }});
        cases.put("x124_wrong_thread_or_host_stays_native",()->{try(Scene s=new Scene(HOME)){
            OneBackExit.enterBack(s.activity);try{
                boolean[] accepted={true};Thread thread=new Thread(()->accepted[0]=OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(HOME)));thread.start();thread.join();
                check(!accepted[0],"worker thread cannot remove task");
                check(!OneBackExit.tryTwitterRootBack(new Activity(s.app,11,true),Uri.parse(HOME)),"package alone does not prove the native Main host");
                check(!OneBackExit.tryTwitterRootBack(null,Uri.parse(HOME)),"null owner rejected");
            }finally{OneBackExit.leaveNative();}s.preserved();
        }});
        cases.put("x124_predictive_cancel_does_not_create_back_scope",()->{try(Scene s=new Scene(CHAT)){
            int[] calls={0};OnBackAnimationCallback original=new OnBackAnimationCallback(){
                public void onBackStarted(BackEvent event){calls[0]++;}public void onBackProgressed(BackEvent event){}
                public void onBackCancelled(){calls[0]++;}public void onBackInvoked(){s.nativeBack();}
            };NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            try{OnBackAnimationCallback wrapped=(OnBackAnimationCallback)s.activity.getWindow().dispatcher().top();
                wrapped.onBackStarted(new BackEvent());wrapped.onBackCancelled();
                check(calls[0]==2&&s.hookCalls==0,"cancel forwards animation without committing native Back");
                check(!OneBackExit.tryTwitterRootBack(s.activity,Uri.parse(CHAT)),"cancel leaves no usable root Back scope");s.preserved();
            }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        cases.put("x124_expired_launcher_keeps_marker_for_next_real_launch",()->{
            ProcessApp app=new ProcessApp(new Disk());app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Scene s=new Scene(app,CHAT,launcher(),false)){
                SystemClock.time+=10001;Handler.drain();check(marker(app)&&CHAT.equals(s.current),"expired bounded restoration does not claim Home success");
                Intent fresh=launcher();OneBackExit.onNewIntent(s.activity,fresh);s.activity.setIntent(fresh);s.readNativeStartup(CHAT);Handler.drain();
                check(HOME.equals(s.current)&&!marker(app),"next ordinary launcher still restores Home using retained marker");
            }
        });
        cases.put("x124_external_chat_destination_preserves_marker",()->{
            ProcessApp app=new ProcessApp(new Disk());app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Scene s=new Scene(app,CHAT,new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://x.com/i/chat")),false)){
                Handler.drain();check(CHAT.equals(s.current)&&marker(app)&&s.home.clickCalls==0,"explicit incoming route remains native until a normal launcher");s.preserved();
            }
        });
        return cases;
    }
    public static void main(String[] args)throws Exception {
        int oldCases=cases,oldAssertions=assertions;
        for(Map.Entry<String,Test> test:scenarios().entrySet())test(test.getKey(),test.getValue());
        System.out.println("PASS Back124TwitterTest: "+(cases-oldCases)+" cases, "+(assertions-oldAssertions)+" assertions");
    }
}
