package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Deterministic regressions for X's actual-terminal-only exit and one-shot launcher reset. */
public final class Back119TwitterTest {
    static final String PREFS="app.hiro.oneback.twitter",RESET="home_after_exit";
    static boolean pendingHome(Application a){return a.getSharedPreferences(PREFS,0).getBoolean(RESET,false);}
    static Intent launcher(){return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);}
    static void arm(F f){OneBackExit.enterBack(f.activity);try{OneBackExit.afterNative();}finally{OneBackExit.leaveNative();}}
    static final class Launch implements AutoCloseable {
        final Application app;final Activity activity;final ViewGroup root;final View home;
        Launch(Application application,Intent intent,boolean selected){
            app=application;activity=new com.twitter.android.HomeTimelineActivity(app,22,true);activity.setIntent(intent);
            root=(ViewGroup)activity.getWindow().getDecorView();home=new View(activity);home.description="ホーム";home.selected=selected;home.clickable=true;
            home.bounds=new Rect(0,1990,190,2170);home.clickAction=()->home.selected=true;root.addView(home);
            OneBackExit.install(activity);activity.getWindow().attach();root.focus(true);OneBackExit.onResume(activity);
        }
        @Override public void close(){OneBackExit.onDestroy(activity);activity.destroyed=true;root.detach();Handler.drain();android.view.inspector.WindowInspector.clear();}
    }
    public static void run()throws Exception {
        for(final int kind:new int[]{0,1,2,3,4})test("x119_native_exit_kind_"+kind,()->{try(F f=new F(X,true,false)){
            android.app.ActivityManager.AppTask task=ownTask(f,11);
            check(!OneBackExit.tryBack(f.activity),"other tab keeps native Back decision");
            OneBackExit.enterBack(f.activity);try{check(OneBackExit.tryTerminalBack(f.activity,kind,kind==2),"actual root task exit intercepted");}finally{OneBackExit.leaveNative();}
            check(task.excluded&&task.removeCalls>=1,"own task excluded and removed");check(pendingHome(f.app),"restart Home marker persisted");
        }});
        test("x119_nonback_finish_keeps_native_semantics",()->{try(F f=new F(X,true,false)){
            check(!OneBackExit.tryTerminalBack(f.activity,1,false),"ordinary lifecycle finish is not Back");check(!pendingHome(f.app),"no reset marker without exit");preserved(f);
        }});
        test("x119_home_exit_also_marks_launcher_reset",()->{try(F f=new F(X)){
            check(OneBackExit.tryBack(f.activity),"confirmed Home exit handled");check(pendingHome(f.app),"all patched exits request next Home");
        }});
        test("x119_native_page_pop_does_not_exit",()->{try(F f=new F(X,true,false)){
            OneBackExit.enterBack(f.activity);try{f.home.selected=true;OneBackExit.afterNative();}finally{OneBackExit.leaveNative();}
            preserved(f);check(!pendingHome(f.app),"page pop never requests Home reset");
        }});
        test("x119_platform_fragment_pop_is_preserved",()->{try(F f=new F(X,true,false)){
            f.activity.fragments.count=1;
            OneBackExit.enterBack(f.activity);try{check(!OneBackExit.tryTerminalBack(f.activity,0,false),"platform Fragment stack consumes Back");}finally{OneBackExit.leaveNative();}
            preserved(f);
        }});
        test("x119_nonroot_detail_finish_preserved",()->{
            Application a=new Application(X);Activity detail=new Activity(a,31,false);View root=detail.getWindow().getDecorView();OneBackExit.install(detail);detail.getWindow().attach();root.focus(true);OneBackExit.onResume(detail);
            OneBackExit.enterBack(detail);try{check(!OneBackExit.tryTerminalBack(detail,1,false),"detail Activity finish remains a page pop");}finally{OneBackExit.leaveNative();}
            check(detail.removeCalls==0,"detail did not remove task");OneBackExit.onDestroy(detail);detail.destroyed=true;root.detach();
        });
        test("x119_keyboard_back_does_not_arm_exit",()->{try(F f=new F(X,true,false)){
            f.root.insets.ime=true;OneBackExit.enterBack(f.activity);try{check(!OneBackExit.tryTerminalBack(f.activity,1,false),"keyboard guard survives terminal bridge");}finally{OneBackExit.leaveNative();}preserved(f);
        }});
        test("x119_async_exit_cleans_recents",()->{try(F f=new F(X,true,false)){
            arm(f);check(OneBackExit.tryTerminalBack(f.activity,2,true),"bounded async native exit captured");check(f.activity.removeCalls>=1,"async exit removed task");
        }});
        test("x119_stopped_after_native_back_cleans_retained_task",()->{try(F f=new F(X,true,false)){
            arm(f);OneBackExit.onStopped(f.activity);Handler.drain();check(f.activity.removeCalls>=1,"stopped retained task removed after Back");
        }});
        test("x119_user_home_cancels_pending_back_cleanup",()->{try(F f=new F(X,true,false)){
            arm(f);OneBackExit.onUserLeaveHint(f.activity);OneBackExit.onStopped(f.activity);Handler.drain();preserved(f);
        }});
        test("x119_user_interaction_cancels_pending_back_cleanup",()->{try(F f=new F(X,true,false)){
            arm(f);OneBackExit.onUserInteraction(f.activity);check(!OneBackExit.tryTerminalBack(f.activity,1,false),"new input invalidates delayed terminal evidence");preserved(f);
        }});
        test("x119_lock_cancels_pending_back_cleanup",()->{try(F f=new F(X,true,false)){
            android.app.KeyguardManager keyguard=new android.app.KeyguardManager();f.app.setService(android.content.Context.KEYGUARD_SERVICE,keyguard);
            arm(f);keyguard.locked=true;OneBackExit.onStopped(f.activity);Handler.drain();preserved(f);
        }});
        test("x119_screen_off_cancels_pending_back_cleanup",()->{try(F f=new F(X,true,false)){
            android.os.PowerManager power=new android.os.PowerManager();f.app.setService(android.content.Context.POWER_SERVICE,power);
            arm(f);power.interactive=false;OneBackExit.onStopped(f.activity);Handler.drain();preserved(f);
        }});
        test("x119_same_instance_resume_cancels_removal_retry",()->{try(F f=new F(X)){
            android.app.ActivityManager.AppTask task=ownTask(f,11);task.silentRemove=true;
            check(OneBackExit.tryBack(f.activity),"initial exit requested");check(task.removeCalls==1,"one initial task removal");
            OneBackExit.onResume(f.activity);Handler.drain();check(task.removeCalls==1,"no delayed removal of resumed instance");
            check(OneBackExit.anyResumed(X),"same instance returned to live state");
        }});
        test("x119_next_launcher_from_saved_other_tab_selects_home",()->{
            Application app;try(F f=new F(X,true,false)){app=f.app;OneBackExit.enterBack(f.activity);try{OneBackExit.tryTerminalBack(f.activity,1,false);}finally{OneBackExit.leaveNative();}}
            try(Launch next=new Launch(app,launcher(),false)){check("twitter://timeline/home".equals(next.activity.getIntent().getStringExtra("page")),"native presenter receives explicit Home before startup");Handler.drain();check(next.home.clickCalls==1&&next.home.selected,"one Home click on launcher");check(!pendingHome(app),"marker cleared only after verified Home");}
        });
        test("x119_cold_process_marker_restores_home",()->{
            Application cold=new Application(X);cold.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(cold,launcher(),false)){Handler.drain();check(next.home.selected&&next.home.clickCalls==1,"persisted marker independent of runtime process memory");}
        });
        test("x119_launcher_already_home_never_refreshes",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,launcher(),true)){Handler.drain();check(next.home.clickCalls==0&&!pendingHome(app),"already-selected Home consumes reset without click");}
        });
        test("x119_deeplink_keeps_route_and_marker",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://x.com/user/status/1")),false)){
                Handler.drain();check(next.home.clickCalls==0&&pendingHome(app),"deep link unchanged; marker retained for actual launcher");
            }
        });
        test("x119_explicit_nonlauncher_intent_keeps_route",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,new Intent(),false)){Handler.drain();check(next.home.clickCalls==0&&pendingHome(app),"unknown explicit route cannot grant Home reset");}
        });
        test("x119_onnewintent_launcher_overrides_stale_deeplink",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://x.com/user")),false)){
                Handler.drain();Intent fresh=launcher();OneBackExit.onNewIntent(next.activity,fresh);check("twitter://timeline/home".equals(fresh.getStringExtra("page")),"warm launch mutates incoming Intent, not stale Activity intent");Handler.drain();
                check(next.home.clickCalls==1&&next.home.selected,"incoming launcher recognized independently of getIntent");
            }
        });
        test("x119_user_navigation_cancels_launch_retry",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,launcher(),false)){
                OneBackExit.onUserInteraction(next.activity);Handler.drain();check(next.home.clickCalls==0,"queued reset cannot override fresh user navigation");
            }
        });
        test("x119_new_deeplink_cancels_queued_launcher_restore",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,launcher(),false)){
                OneBackExit.onNewIntent(next.activity,new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://x.com/user")));Handler.drain();
                check(next.home.clickCalls==0,"new link cancels earlier launcher intent");
            }
        });
        test("x119_noop_home_click_is_bounded_and_keeps_marker",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,launcher(),false)){
                next.home.clickAction=()->{};Handler.drain();check(next.home.clickCalls==1&&pendingHome(app),"unverified click never counts as reset success");check(Handler.queuedCount()==0,"bounded retry stops");
            }
        });
        test("x119_hidden_home_label_cannot_redirect",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,launcher(),false)){
                next.home.hidden=true;Handler.drain();check(next.home.clickCalls==0&&pendingHome(app),"hidden nav label has no reset authority");
            }
        });
        test("x119_launcher_trampoline_preserves_same_task_home_contract",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            Activity start=new com.twitter.android.StartActivity(app,22,true);start.setIntent(launcher());OneBackExit.install(start);
            try(Launch next=new Launch(app,new Intent().putExtra("other","kept"),false)){
                check("twitter://timeline/home".equals(next.activity.getIntent().getStringExtra("page")),"same-task main receives native Home route");
                check("kept".equals(next.activity.getIntent().getStringExtra("other")),"unrelated extras retained");Handler.drain();
            }finally{OneBackExit.onDestroy(start);start.destroyed=true;}
        });
        test("x119_launcher_trampoline_does_not_override_explicit_notification_page",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            Activity start=new com.twitter.android.StartActivity(app,22,true);start.setIntent(launcher());OneBackExit.install(start);
            try(Launch next=new Launch(app,new Intent().putExtra("page","twitter://messages"),false)){
                Handler.drain();check("twitter://messages".equals(next.activity.getIntent().getStringExtra("page")),"explicit internal destination preserved");check(next.home.clickCalls==0,"fallback cannot override explicit destination");
            }finally{OneBackExit.onDestroy(start);start.destroyed=true;}
        });
        test("x119_launcher_like_explicit_page_is_preserved",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,launcher().putExtra("page","twitter://messages"),false)){
                Handler.drain();check("twitter://messages".equals(next.activity.getIntent().getStringExtra("page")),"explicit destination survives launcher-like notification");
                check(next.home.clickCalls==0,"fallback respects explicit destination");
            }
        });
        test("x119_not_yet_attached_click_listener_retries_boundedly",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,launcher(),false)){
                next.home.clickAction=null;new Handler(android.os.Looper.getMainLooper()).post(()->next.home.clickAction=()->next.home.selected=true);
                Handler.drain();check(next.home.clickCalls==2&&next.home.selected&&!pendingHome(app),"false click dispatch retries after listener becomes available");
            }
        });
        test("x119_failing_click_listener_keeps_app_alive",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,launcher(),false)){
                next.home.clickAction=()->{throw new IllegalStateException("startup transition");};Handler.drain();
                check(next.home.clickCalls==1&&pendingHome(app),"automatic click failure is contained and retains reset marker");
            }
        });
        test("x119_actual_home_id_requires_bottom_nav_container",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,launcher(),false)){
                next.home.description=null;next.home.id=0x7f0b08ec;android.content.res.Resources.IDS.put(next.home.id,"home");
                next.root.removeView(next.home);ViewGroup nav=new ViewGroup(next.activity);nav.id=0x7f0b0241;android.content.res.Resources.IDS.put(nav.id,"bottom_navigation_bar");
                nav.bounds=new Rect(0,1980,1080,2200);nav.addView(next.home);next.root.addView(nav);Handler.drain();
                check(next.home.selected&&next.home.clickCalls==1&&!pendingHome(app),"APK home id works only under verified bottom navigation");
            }
        });
        test("x119_generic_home_id_outside_nav_is_not_a_button",()->{
            Application app=new Application(X);app.getSharedPreferences(PREFS,0).edit().putBoolean(RESET,true).commit();
            try(Launch next=new Launch(app,launcher(),false)){
                next.home.description=null;next.home.id=0x7f0b08ec;android.content.res.Resources.IDS.put(next.home.id,"home");Handler.drain();
                check(next.home.clickCalls==0&&pendingHome(app),"unscoped home id cannot navigate");
            }
        });
    }
    public static void main(String[] args)throws Exception{int before=cases,assertBefore=assertions;run();System.out.println("PASS Back119TwitterTest: "+(cases-before)+" cases, "+(assertions-assertBefore)+" assertions");}
}
