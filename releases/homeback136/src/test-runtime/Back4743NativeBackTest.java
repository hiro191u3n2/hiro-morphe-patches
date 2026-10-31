package app.hiro.oneback.runtime;

import android.app.*;
import android.content.*;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.window.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import com.ss.android.ugc.aweme.main.assems.tabs.TabAbilityAssem;
import com.ss.android.ugc.aweme.main.assems.ui.HomepageViewPagerAssem;
import com.ss.android.ugc.aweme.homepage.api.interaction.ScrollSwitchStateManager;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Host model of the audited 47.4.3 native branches. The companion DEX audit
 * proves actual hook locations and preserves every native guard and branch.
 * No claim of execution on an Android device is made by this fixture. */
public final class Back4743NativeBackTest {
    private static boolean optional(String name,Activity activity) {
        try {
            Object result=OneBackExit.class.getMethod(name,Activity.class).invoke(null,activity);
            return Boolean.TRUE.equals(result);
        } catch(NoSuchMethodException releasedWithoutHook) {return false;}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static void enterMain(Activity activity) {
        try {OneBackExit.class.getMethod("enterTikTokBack",Activity.class).invoke(null,activity);}
        catch(NoSuchMethodException releasedWithoutHook){OneBackExit.enterBack(activity);}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    public static final class PatchedMain extends com.ss.android.ugc.aweme.main.MainActivity {
        BooleanSupplier nativeDown;boolean downConsumed;
        PatchedMain(Application app){super(app,11,true);}
        @Override public void onUserInteraction(){OneBackExit.onUserInteraction(this);super.onUserInteraction();}
        @Override public boolean dispatchKeyEvent(KeyEvent event) {
            // Exact generated wrapper shape: key -> enterKey -> original body
            // -> afterNative -> finally leaveNative, including framework interaction.
            if(OneBackExit.key(this,event))return true;
            OneBackExit.enterKey(this,event);
            try {
                boolean handled;
                if(event.getAction()==KeyEvent.ACTION_DOWN&&nativeDown!=null&&nativeDown.getAsBoolean()) {
                    onUserInteraction();downConsumed=true;handled=true;
                } else if(event.getAction()==KeyEvent.ACTION_UP&&downConsumed) {
                    onUserInteraction();downConsumed=false;handled=true;
                } else handled=super.dispatchKeyEvent(event);
                OneBackExit.afterNative();return handled;
            } finally {OneBackExit.leaveNative();}
        }
        @Override public void onBackPressed() {
            if(OneBackExit.tryBack(this))return;
            enterMain(this);
            try{super.onBackPressed();OneBackExit.afterNative();}
            finally{OneBackExit.leaveNative();}
        }
        @Override public void finish(){if(!OneBackExit.tryTerminalBack(this,1,false))super.finish();}
        @Override public void finishAndRemoveTask(){if(!OneBackExit.tryTerminalBack(this,3,false))super.finishAndRemoveTask();}
    }
    static final class GuardedTask extends ActivityManager.AppTask {
        GuardedTask(ActivityManager.RecentTaskInfo info){super(info);}
        @Override public void finishAndRemoveTask(){if(!OneBackExit.tryTaskTerminal(this))super.finishAndRemoveTask();}
    }
    static final class Scene implements AutoCloseable {
        final Application app=new Application(TT);final PatchedMain activity=new PatchedMain(app);
        final ViewGroup root=(ViewGroup)activity.getWindow().getDecorView();
        final ActivityManager manager=new ActivityManager();final GuardedTask task;
        final TabAbilityAssem tabs=new TabAbilityAssem();final ScrollSwitchStateManager outer=new ScrollSwitchStateManager();
        final HomepageViewPagerAssem pager=new HomepageViewPagerAssem(outer);
        String guard;int nativeCalls,guardCalls,timerCalls,skipCalls,rootCalls,homeSwitches,refreshCalls;
        Runnable beforeBoundary;boolean recursiveFallback;
        Scene(String tab) {
            activity.communication_gen_field_com_bytedance_tiktok_homepage_mainfragment_HomeTabAbility=tabs;
            activity.stateManager=outer;
            activity.communication_gen_field_com_ss_android_ugc_aweme_base_HomePageViewPagerAbility=pager;
            select(tab);
            // Deliberately NO "For You", "おすすめ", feed resource id or tab label.
            View unlabelledVideo=new View(activity);unlabelledVideo.bounds=new Rect(0,0,1080,2200);root.addView(unlabelledVideo);
            ActivityManager.RecentTaskInfo info=new ActivityManager.RecentTaskInfo();info.taskId=11;
            info.baseActivity=new ComponentName(TT,"com.ss.android.ugc.aweme.main.MainActivity");info.baseIntent=new Intent().setComponent(info.baseActivity);
            task=new GuardedTask(info);task.activities.add(activity);manager.tasks.add(task);app.setService(Context.ACTIVITY_SERVICE,manager);
            activity.nativeBackAction=()->nativeBack();
            OneBackExit.install(activity);activity.getWindow().attach();root.focus(true);OneBackExit.onResume(activity);Handler.drain();
        }
        void select(String tab){tabs.selected=tab;tabs.tree.homeActive="HOME".equals(tab);}
        void nativeBack() {
            nativeCalls++;
            if(recursiveFallback){check(OneBackExit.tryTerminalBack(activity,0,false),"recursive platform fallback suppressed");return;}
            // LX/1AA5 native interceptor and CommentService guards are before gate 1.
            if("interceptor".equals(guard)||"comments".equals(guard)) {guardCalls++;guard=null;return;}
            skipCalls++;
            if(!optional("skipTikTokDoubleBack",activity))timerCalls++;
            // BackRefreshStrategyAssem preserves both MainBusiness tutorial guards.
            if("swipe_up_guide".equals(guard)||"not_interested_tutorial".equals(guard)){guardCalls++;guard=null;return;}
            if(beforeBoundary!=null)beforeBoundary.run();
            rootCalls++;
            if(optional("tryTikTokRootBack",activity))return;
            if(!"HOME".equals(tabs.selected)){homeSwitches++;tabs.tB0(new Bundle(),"HOME");}
            else refreshCalls++;
        }
        void back(){check(OneBackExit.tryBack(activity),"modern request consumed");Handler.drain();}
        void fresh(){SystemClock.time+=400;OneBackExit.onUserInteraction(activity);}
        void keyBack(){activity.dispatchKeyEvent(down());activity.dispatchKeyEvent(up());Handler.drain();}
        void preserved(){check(task.removeCalls==0&&activity.removeCalls==0&&!activity.finishing,"one native navigation preserves task");}
        void removed(){
            check(task.removeCalls==1&&task.excluded&&activity.finishing,"one Back excludes and removes its own task");
            check(task.events.equals(Arrays.asList("exclude","remove")),"Recents exclusion precedes task removal");
            check(activity.removeCalls==0,"successful AppTask removal avoids a second Activity removal");
        }
        @Override public void close(){OneBackExit.onDestroy(activity);activity.destroyed=true;root.detach();Handler.drain();android.view.inspector.WindowInspector.clear();}
    }
    public static final class RetainedProfileFragment {
        final View view;RetainedProfileFragment(View value){view=value;}
        public boolean isVisible(){return true;}public View getView(){return view;}public Object getChildFragmentManager(){return null;}
    }
    private static void rootWithoutLabels() {
        try(Scene s=new Scene("HOME")) {
            check(!TikTokScreen.inspect(s.activity).home,"old visible-feed classifier has no Home evidence");
            s.back();
            check(s.nativeCalls==1,"exactly one verified native Main Back");
            s.removed();check(s.timerCalls==0&&s.homeSwitches==0,"native double-Back timer skipped without tab jump");
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length==1&&"--reproduce-root".equals(args[0])) {
            // Compile this SAME scenario against current classes, then run it
            // with exact released 1.1.21 runtime first on the class path. Missing
            // new hooks execute the original native path, not a synthetic error.
            rootWithoutLabels();System.out.println("PASS Back4743NativeBackTest root regression");return;
        }
        int startCases=cases,startAssertions=assertions;
        test("tt4743_hidden_labels_native_root_exit",()->rootWithoutLabels());
        test("tt4743_retained_profile_is_not_foreground_authority",()->{try(Scene s=new Scene("HOME")){
            View offscreen=new View(s.activity);offscreen.hidden=true;s.activity.fragments.fragments.add(new RetainedProfileFragment(offscreen));
            s.back();s.removed();
        }});
        for(final String guard:new String[]{"interceptor","comments","swipe_up_guide","not_interested_tutorial"})
            test("tt4743_preserve_native_"+guard,()->{try(Scene s=new Scene("USER")){
                s.guard=guard;s.back();s.preserved();
                check(s.guardCalls==1&&s.rootCalls==0&&s.homeSwitches==0&&"USER".equals(s.tabs.selected),"native guard consumes exactly one Back before root gate");
                check(s.nativeCalls==1,"no automatic second Back after layer closes");
                check(s.skipCalls==(("comments".equals(guard)||"interceptor".equals(guard))?0:1),"helper/caller guard order retained");
            }});
        for(final String tab:new String[]{"USER","NOTIFICATION"})
            test("tt4743_"+tab+"_home_then_fresh_back_exit",()->{try(Scene s=new Scene(tab)){
                s.back();s.preserved();check(s.homeSwitches==1&&"HOME".equals(s.tabs.selected)&&s.tabs.tree.homeActive,"first Back selects the native Home tab");
                s.fresh();s.back();s.removed();check(s.nativeCalls==2,"only the next distinct Back exits");
            }});
        test("tt4743_comments_dialog_stops_after_close",()->{try(Scene s=new Scene("USER")){
            Dialog dialog=new Dialog(s.activity);dialog.show();dialog.getWindow().getDecorView().focus(true);
            View comments=new View(s.activity);comments.id=++markerId;android.content.res.Resources.IDS.put(comments.id,"comment_sheet");comments.bounds=new Rect(0,500,1080,2200);
            ((ViewGroup)dialog.getWindow().getDecorView()).addView(comments);OneBackExit.dialogShown(dialog);Handler.drain();
            check(OneBackExit.tryDialogBack(dialog),"comment dialog handled");Handler.drain();
            check(dialog.nativeBackCalls==1&&!dialog.getWindow().getDecorView().isAttachedToWindow(),"comment dialog closes once");
            check(s.nativeCalls==0&&s.tabs.calls==0&&"USER".equals(s.tabs.selected),"dialog close does not enqueue Home navigation");s.preserved();dialog.dismiss();
        }});
        test("tt4743_androidx_platform_fallback_visits_native_main_once",()->{try(Scene s=new Scene("HOME")){
            final int[] calls={0};OnBackInvokedCallback original=()->{calls[0]++;check(OneBackExit.tryTerminalBack(s.activity,0,false),"platform fallback rerouted");};
            NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            try{s.activity.getWindow().dispatcher().invokeBack();s.removed();check(calls[0]==1&&s.nativeCalls==1&&s.rootCalls==1,"one AndroidX fallback and one native Main dispatch");}
            finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        test("tt4743_androidx_child_handler_consumes_without_main",()->{try(Scene s=new Scene("HOME")){
            final int[] calls={0};OnBackInvokedCallback original=()->calls[0]++;NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            try{s.activity.getWindow().dispatcher().invokeBack();s.preserved();check(calls[0]==1&&s.nativeCalls==0,"enabled native child handler remains first");}
            finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        test("tt4743_recursive_platform_fallback_is_bounded",()->{try(Scene s=new Scene("HOME")){
            s.recursiveFallback=true;OnBackInvokedCallback original=()->OneBackExit.tryTerminalBack(s.activity,0,false);NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            try{s.activity.getWindow().dispatcher().invokeBack();s.preserved();check(s.nativeCalls==1,"native Main dispatch is never replayed recursively");}
            finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        test("tt4743_physical_full_wrapper_home_exit",()->{try(Scene s=new Scene("HOME")){
            s.keyBack();s.removed();check(s.activity.userInteractionCalls==2&&s.nativeCalls==1&&s.timerCalls==0,"framework interaction on both actions retains native key session");
        }});
        test("tt4743_physical_profile_then_next_back",()->{try(Scene s=new Scene("USER")){
            s.keyBack();s.preserved();check(s.homeSwitches==1,"first physical Back reaches Home");
            s.keyBack();s.removed();check(s.activity.userInteractionCalls==4&&s.nativeCalls==2,"new key DOWN resets completed session");
        }});
        test("tt4743_key_down_outer_pop_cannot_exit_on_same_up",()->{try(Scene s=new Scene("HOME")){
            s.outer.selected="page_profile";
            s.activity.nativeDown=()->{if("page_profile".equals(s.outer.selected)){s.outer.selected="page_feed";return true;}return false;};
            s.keyBack();s.preserved();check(s.nativeCalls==0&&s.activity.userInteractionCalls==2,"consumed native KeyDown and paired KeyUp never reach root gate");
            s.keyBack();s.removed();check(s.nativeCalls==1,"separate fresh Back exits returned feed");
        }});
        test("tt4743_key_transition_between_actions_cannot_acquire_exit",()->{try(Scene s=new Scene("USER")){
            s.activity.dispatchKeyEvent(down());s.select("HOME");s.activity.dispatchKeyEvent(up());s.preserved();
            s.keyBack();s.removed();
        }});
        test("tt4743_key_up_does_not_repeat_native_outer_guard",()->{try(Scene s=new Scene("HOME")){
            s.outer.selected="page_profile";
            s.activity.nativeDown=()->{s.outer.page.LJLI();return false;};
            s.keyBack();s.preserved();
            check(s.outer.page.backCalls==1&&s.pager.calls==0,"outer page handled only by native KeyDown, never replayed on KeyUp");
        }});
        test("tt4743_cancelled_and_orphan_key_release",()->{try(Scene s=new Scene("HOME")){
            s.activity.dispatchKeyEvent(up());s.preserved();check(s.nativeCalls==0,"orphan release has no root dispatch");
            s.activity.dispatchKeyEvent(down());s.activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,true));
            s.preserved();check(s.nativeCalls==0,"cancelled release cannot commit");s.keyBack();s.removed();
        }});
        test("tt4743_resume_between_key_actions_never_runs_legacy_trip",()->{try(Scene s=new Scene("USER")){
            s.activity.dispatchKeyEvent(down());OneBackExit.onResume(s.activity);Handler.drain();
            check(s.nativeCalls==0&&s.tabs.calls==0,"resume does not queue a modern navigation poll");
            s.activity.dispatchKeyEvent(up());s.preserved();check(s.nativeCalls==1&&s.homeSwitches==1,"release retains one native tab transition");
        }});
        test("tt4743_prediction_nonhome_start_never_acquires_exit",()->{try(Scene s=new Scene("USER")){
            TikTokBack.predictionStart(s.activity);s.select("HOME");s.back();s.preserved();
            s.fresh();s.back();s.removed();
        }});
        test("tt4743_prediction_cancel_preserves_task",()->{try(Scene s=new Scene("HOME")){
            TikTokBack.predictionStart(s.activity);TikTokBack.predictionEnd(s.activity);s.preserved();check(s.nativeCalls==0,"cancel never dispatches");s.back();s.removed();
        }});
        test("tt4743_ime_down_then_hidden_up_still_no_exit",()->{try(Scene s=new Scene("HOME")){
            s.root.insets.ime=true;s.activity.dispatchKeyEvent(down());s.root.insets.ime=false;s.activity.dispatchKeyEvent(up());s.preserved();
            s.keyBack();s.removed();
        }});
        test("tt4743_visible_editor_blocks_but_hidden_editor_does_not",()->{try(Scene s=new Scene("HOME")){
            View editor=new View(s.activity);editor.editor=true;s.root.addView(editor);s.root.focusView=editor;
            check(!OneBackExit.tryBack(s.activity),"visible focused editor delegated");s.preserved();editor.hidden=true;
            s.back();s.removed();
        }});
        test("tt4743_editor_under_transparent_parent_is_not_foreground",()->{try(Scene s=new Scene("HOME")){
            ViewGroup pane=new ViewGroup(s.activity);View editor=new View(s.activity);editor.editor=true;pane.addView(editor);s.root.addView(pane);s.root.focusView=editor;
            check(!OneBackExit.tryBack(s.activity),"visible editor under visible parent blocks");s.preserved();pane.alpha=0;
            s.back();s.removed();
        }});
        for(final String outer:new String[]{"page_profile","page_discover","page_camera"})
            test("tt4743_outer_"+outer+"_before_root_exit",()->{try(Scene s=new Scene("HOME")){
                s.outer.selected=outer;s.back();s.preserved();
                check(s.outer.page.backCalls==1&&s.pager.calls==1&&s.pager.lastAnimate==null&&s.pager.lastBackPress,"native outer guard precedes native Ir1 transition");
                check("page_feed".equals(s.outer.selected)&&s.homeSwitches==0,"outer pager returns without disturbing inner Home");
                s.fresh();s.back();s.removed();
            }});
        test("tt4743_outer_native_guard_consumes",()->{try(Scene s=new Scene("HOME")){
            s.outer.selected="page_profile";s.outer.page.consume=true;s.back();s.preserved();
            check(s.outer.page.backCalls==1&&s.pager.calls==0&&"page_profile".equals(s.outer.selected),"author page native guard retains control");
        }});
        test("tt4743_outer_noop_or_unknown_never_grants_exit",()->{try(Scene s=new Scene("HOME")){
            s.outer.selected="page_profile";s.pager.noOp=true;s.back();s.preserved();check(s.pager.calls==1,"no-op transition is not retried");
            s.fresh();s.outer.selected="unrecognized_page";s.back();s.preserved();check(s.pager.calls==1,"unknown page is never navigated through guessed identifiers");
        }});
        test("tt4743_changed_home_binding_does_not_acquire_exit",()->{try(Scene s=new Scene("HOME")){
            s.beforeBoundary=()->{TabAbilityAssem replacement=new TabAbilityAssem();replacement.selected="HOME";replacement.tree.homeActive=true;s.activity.communication_gen_field_com_bytedance_tiktok_homepage_mainfragment_HomeTabAbility=replacement;};
            s.back();s.preserved();
        }});
        test("tt4743_child_settings_activity_closes_once",()->{
            Application app=new Application(TT);Activity child=new Activity(app,12,true);ViewGroup root=(ViewGroup)child.getWindow().getDecorView();
            child.nativeBackAction=()->{if(!OneBackExit.tryTerminalBack(child,1,false))child.finish();};
            OneBackExit.install(child);child.getWindow().attach();root.focus(true);OneBackExit.onResume(child);Handler.drain();
            try{check(OneBackExit.tryBack(child),"modern settings Back dispatched");Handler.drain();check(child.nativeBackCalls==1&&child.finishCalls==1&&child.removeCalls==0&&child.startCalls==0,"child closes once without launcher/Home redirection");}
            finally{OneBackExit.onDestroy(child);child.destroyed=true;root.detach();Handler.drain();android.view.inspector.WindowInspector.clear();}
        });
        System.out.println("DETAIL Back4743NativeBackTest: "+(cases-startCases)+" cases, "+(assertions-startAssertions)+" assertions");
        System.out.println("PASS Back4743NativeBackTest: native-guarded single Back; label-independent; host simulation, not Android device");
    }
}
