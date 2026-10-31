package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.Back121InstagramDispatchTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent121Fixture.*;

/** The production guard APIs are called directly, with no platform wrapper or
 * owned root callback. This models the verified AndroidX interception points,
 * not physical Android execution. */
public final class Back122InstagramDispatcherTest {
    static Object bind(Scene s){Object dispatcher=new Object();OneBackExit.bindInstagramDispatcher(s.activity,dispatcher);return dispatcher;}
    static Object layer(Scene s,Node reel,boolean enabled,Object page)throws Exception {
        Object layer=s.create("X.16z");set(layer,"A15",true);set(layer,"A0U",enabled);
        ((NativeActivity121Fixture.LayerState)layer).current=page;set(reel,"A0L",layer);return layer;
    }
    static void unrelated(Scene s,int count) {
        ViewGroup branch=s.group(s.root,"unrelated_retained_content",new Rect(0,0,1080,2200));
        for(int i=0;i<count;i++)branch.addView(new View(s.activity));
    }
    static Map<String,Test> scenarios(){Map<String,Test> cases=new LinkedHashMap<String,Test>();
        for(final boolean enabled:new boolean[]{false,true})cases.put("a15_true_without_consumable_layer_"+enabled,()->{try(Scene s=new Scene()){
            Node reels=current(s,false);Object object=layer(s,reels,enabled,null);ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            check(InstagramNativeLayers.clear(reels),"A15 does not imply an open sheet: A0p would return false");
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"clear Reels exits at exact dispatcher without platform registration");removed(s,task);
            check(enabled?((NativeActivity121Fixture.LayerState)object).lookups>0:((NativeActivity121Fixture.LayerState)object).lookups==0,"disabled A0U never requires A0M");
        }});
        cases.put("a15_true_disabled_even_with_retained_layer_page",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);layer(s,reels,false,s.create("X.NativeProfile"));ActivityManager.AppTask task=own(s);
            check(InstagramNativeLayers.clear(reels),"A0p returns false before looking up a retained disabled layer");
            check(OneBackExit.tryInstagramDispatcherBack(bind(s)),"disabled retained controller does not poison Reels");removed(s,task);
        }});
        cases.put("a15_true_enabled_real_layer_is_native",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);Object object=layer(s,reels,true,s.create("X.NativeProfile"));ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            check(!InstagramNativeLayers.clear(reels),"a real native layer preserves native Back");
            // X.0wS.A0p dismisses its current sheet and returns true. X.8Zv
            // then returns before the exact Reels hook; do not call it here.
            s.activity.nativeBackAction=()->{((NativeActivity121Fixture.LayerState)object).current=null;};
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"layer Back is forwarded exactly once");
            check(s.activity.nativeBackCalls==1,"one actual native close");preserved(s,task);
            SystemClock.time+=400;check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"next independent Back exits Reels after layer closure");removed(s,task);
        }});
        cases.put("exact_dispatch_ignores_1801_unrelated_views",()->{try(Scene s=new Scene()){
            current(s,false);unrelated(s,1801);ActivityManager.AppTask task=own(s);
            check(OneBackExit.tryInstagramDispatcherBack(bind(s)),"only selected receiver content is classified");removed(s,task);
        }});
        cases.put("exact_dispatch_works_inside_native_scope_without_tracked_root",()->{try(Scene s=new Scene()){
            current(s,false);unrelated(s,1801);ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            OneBackExit.enterBack(null);try{check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"exact binding resolves Activity even when outer platform root lookup failed");}finally{OneBackExit.leaveNative();}
            removed(s,task);
        }});
        cases.put("exact_dispatch_works_inside_real_initial_reels_scope",()->{try(Scene s=new Scene()){
            current(s,false);ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            OneBackExit.enterBack(s.activity);try{check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"verified initial Reels identity survives native depth");}finally{OneBackExit.leaveNative();}
            removed(s,task);
        }});
        cases.put("queued_manager_bookkeeping_is_not_reels_identity",()->{try(Scene s=new Scene()){
            current(s,false);set(s.manager,"A0W",new ArrayList<Object>(){{add(new Object());}});ActivityManager.AppTask task=own(s);
            check(OneBackExit.tryInstagramDispatcherBack(bind(s)),"app-selected current receiver does not depend on pending bookkeeping");removed(s,task);
        }});
        cases.put("native_boundary_is_independent_of_unrelated_views",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);unrelated(s,1801);ActivityManager.AppTask task=own(s);
            OneBackExit.enterBack(s.activity);try{check(OneBackExit.tryInstagramReelsBack(reels),"exact receiver boundary does not repeat decor scan");}finally{OneBackExit.leaveNative();}
            removed(s,task);
        }});
        cases.put("exact_reels_handler_does_not_require_platform_tracking",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);ActivityManager.AppTask task=own(s);
            check(OneBackExit.tryInstagramReelsBack(reels),"verified native callsite authorizes its real receiver without terminal-depth bookkeeping");removed(s,task);
        }});
        cases.put("predictive_reels_start_progress_cancel_do_not_exit",()->{try(Scene s=new Scene()){
            current(s,false);ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            check(OneBackExit.startInstagramDispatcherGesture(dispatcher),"native pop start is suppressed");
            check(OneBackExit.progressInstagramDispatcherGesture(dispatcher),"unmatched native progress is suppressed");
            check(OneBackExit.cancelInstagramDispatcherGesture(dispatcher),"unmatched native cancel is suppressed");preserved(s,task);
            check(!OneBackExit.progressInstagramDispatcherGesture(dispatcher),"cancel releases prediction");
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"fresh button Back remains usable after cancel");removed(s,task);
        }});
        cases.put("predictive_reels_commit_exits_exactly_once",()->{try(Scene s=new Scene()){
            current(s,false);ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            check(OneBackExit.startInstagramDispatcherGesture(dispatcher),"start intercepted");
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"commit exits instead of native pop");removed(s,task);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"finishing owner cannot exit again");
        }});
        cases.put("predictive_changed_reels_identity_consumes_without_exit",()->{try(Scene s=new Scene()){
            Node first=current(s,false);ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            check(OneBackExit.startInstagramDispatcherGesture(dispatcher),"first Reels predicted");erase(s,first);current(s,false);
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"no native commit for suppressed but changed scene");preserved(s,task);
        }});
        cases.put("native_detail_pop_cannot_exit_exposed_reels",()->{try(Scene s=new Scene()){
            Node detail=s.node("X.NativeProfile",null,MAIN);s.select(detail,false);ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            OneBackExit.enterBack(s.activity);try {
                erase(s,detail);Node reels=current(s,false);
                check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"initial detail origin remains native after pop");
                // A consumed detail pop returns; it does not subsequently call
                // X.8Zv.onBackPressed on the newly exposed Reels fragment.
            }finally{OneBackExit.leaveNative();}preserved(s,task);
        }});
        cases.put("predictive_detail_to_reels_keeps_initial_action",()->{try(Scene s=new Scene()){
            Node detail=s.node("X.NativeProfile",null,MAIN);s.select(detail,false);ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            check(!OneBackExit.startInstagramDispatcherGesture(dispatcher),"native detail animation remains native");erase(s,detail);Node reels=current(s,false);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"commit cannot borrow newly exposed Reels");
            // The native detail callback returns after its pop. Calling the
            // exact Reels hook here would fabricate a second native dispatch.
            preserved(s,task);Handler.drain();
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"subsequent Back may exit now-current Reels");removed(s,task);
        }});
        cases.put("predictive_keyboard_close_does_not_exit_reels",()->{try(Scene s=new Scene()){
            current(s,false);s.root.insets.ime=true;ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            check(!OneBackExit.startInstagramDispatcherGesture(dispatcher),"IME retains native Back");s.root.insets.ime=false;
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"gesture that started with keyboard does not exit");preserved(s,task);Handler.drain();
        }});
        cases.put("comments_scope_remains_native",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);View panel=s.group((ViewGroup)reels.view,"comments_sheet",new Rect(0,900,1080,2200));ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            s.activity.nativeBackAction=()->{((ViewGroup)reels.view).removeView(panel);check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"recursive native forwarding cannot reenter exit");};
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"comment close handled without task exit");preserved(s,task);
            check(s.activity.nativeBackCalls==1,"no forwarding recursion");
        }});
        for(final boolean popup:new boolean[]{false,true})cases.put("native_scope_layer_back_is_not_swallowed_"+popup,()->{try(Scene s=new Scene()){
            Node reels=current(s,false);Object nativeLayer=popup?s.create("X.21x"):null;
            View panel=popup?null:s.group((ViewGroup)reels.view,"comments_sheet",new Rect(0,900,1080,2200));
            if(popup){set(nativeLayer,"A05",true);set(reels,"A1Y",nativeLayer);}
            ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            OneBackExit.enterBack(s.activity);try {
                check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"existing native layer transaction must proceed into its native dispatcher");
                if(popup)set(nativeLayer,"A05",false);else ((ViewGroup)reels.view).removeView(panel);
                // The native popup/sheet guard returns true after closure,
                // before the exact Reels hook. No second hook is dispatched.
            }finally{OneBackExit.leaveNative();}preserved(s,task);
        }});
        cases.put("predictive_layer_appears_before_commit_forwards_once",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            check(OneBackExit.startInstagramDispatcherGesture(dispatcher),"clear Reels start suppressed native prediction");
            View panel=s.group((ViewGroup)reels.view,"comments_sheet",new Rect(0,900,1080,2200));
            s.activity.nativeBackAction=()->{
                check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"internal native forward is not consumed by the outer suppressed gesture");
                ((ViewGroup)reels.view).removeView(panel);
            };
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"outer commit closes newly appeared layer");
            check(s.activity.nativeBackCalls==1,"one native layer close despite suppressed original prediction");preserved(s,task);
        }});
        cases.put("shared_prediction_cleanup_removes_exact_dispatcher_state",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);ActivityManager.AppTask task=own(s);Object dispatcher=bind(s);
            check(OneBackExit.startInstagramDispatcherGesture(dispatcher),"exact prediction began");
            InstagramBack.predictionEnd(s.activity);erase(s,reels);Node detail=s.node("X.NativeProfile",null,MAIN);s.select(detail,false);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"shared platform cleanup cannot leave a stale suppress decision for later detail Back");preserved(s,task);
        }});
        cases.put("unknown_dispatcher_is_native",()->{try(Scene s=new Scene()){
            current(s,false);ActivityManager.AppTask task=own(s);Object unknown=new Object();
            check(!OneBackExit.tryInstagramDispatcherBack(unknown),"unbound dispatch never guesses an Activity");
            check(!OneBackExit.startInstagramDispatcherGesture(unknown)&&!OneBackExit.cancelInstagramDispatcherGesture(unknown),"unbound predictive callbacks remain native");preserved(s,task);
        }});
        return cases;
    }
    public static void main(String[] args)throws Exception {
        int oldCases=cases,oldAssertions=assertions;
        for(Map.Entry<String,Test> test:scenarios().entrySet())test(test.getKey(),test.getValue());
        System.out.println("PASS Back122InstagramDispatcherTest: "+(cases-oldCases)+" cases, "+(assertions-oldAssertions)+" assertions");
    }
}
