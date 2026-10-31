package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;
import static app.hiro.oneback.runtime.Back130InstagramProfileLinkTest.*;
import static app.hiro.oneback.runtime.InstagramProfileReels137Fixture.*;

/** Two-step return policy, with native attachment/removal/queue events modeled
 * explicitly. No fixture claims that tab selection synchronously binds a pager. */
public final class Back137InstagramProfileReelsTest {
    private static Node nested(Scene scene,boolean modern)throws Exception{
        Node wrapper=scene.node(modern?"X.00VS":"X.02Wh",null,modern?0:MAIN);
        if(modern){scene.tag(wrapper,"f0");scene.cacheTab(wrapper,"feed");}
        scene.select(wrapper,true);
        Node profile=detached(scene);OneBackExit.rememberInstagramExternalProfile(scene.activity,profile,route(scene,URL,"yoasobi_staff_"));
        profile.activity=scene.activity;profile.added=true;profile.visible=true;profile.resumed=true;profile.view=new android.view.ViewGroup(scene.activity);
        set(profile,"mFragmentManager",scene.child(wrapper));set(profile,"mParentFragment",wrapper);set(profile,"mFragmentId",modern?BASE:MAIN);set(profile,"mChildFragmentManager",scene.newManager());
        added(scene.child(wrapper)).add(profile);((android.view.ViewGroup)wrapper.view).addView(profile.view);set(scene.child(wrapper),"count",1);scene.profileTabs(profile,true);
        return profile;
    }
    private static void normalNative(Scene scene,Runnable body){
        OneBackExit.enterBack(scene.activity);try{body.run();OneBackExit.afterNative();}finally{OneBackExit.leaveNative();}
    }
    private static void removeUnchecked(Scene scene,Node profile){try{remove(scene,profile);}catch(Exception e){throw new AssertionError(e);}}
    private static void none(Scene scene,ActivityManager.AppTask task){
        check(scene.activity.selectClipsCalls==0&&scene.activity.resetClipsCalls==0&&scene.activity.startCalls==0,"no Reels navigation without a qualifying removal");noTaskRemoval(task);
    }
    static Map<String,Test> scenarios(){Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final String shape:new String[]{"modern","legacy","direct"})tests.put(shape+"_profile_native_pop_then_reels_back_exit",()->{try(Scene scene=new Scene()){
            Node profile=shape.equals("direct")?linked(scene):nested(scene,shape.equals("modern"));ActivityManager.AppTask task=own(scene);Destination destination=new Destination(scene,!shape.equals("legacy"));
            normalNative(scene,()->removeUnchecked(scene,profile));check(scene.activity.selectClipsCalls==0,"completion defers observation");noTaskRemoval(task);Handler.drain();
            check(scene.activity.selectClipsCalls==1,"actual native pop selects CLIPS once");destination.assertRoot();noTaskRemoval(task);nextReelsBack(scene,destination,task);
        }});
        tests.put("second_link_same_live_Main_uses_new_entry",()->{try(Scene scene=new Scene()){
            Node first=linked(scene);ActivityManager.AppTask task=own(scene);Destination destination=new Destination(scene);
            normalNative(scene,()->removeUnchecked(scene,first));Handler.drain();destination.assertRoot();noTaskRemoval(task);
            OneBackExit.onNewIntent(scene.activity,new Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse(URL)));
            Node second=linked(scene);normalNative(scene,()->removeUnchecked(scene,second));Handler.drain();
            check(scene.activity.selectClipsCalls==2&&scene.activity.resetClipsCalls==2,"both independent links receive one Reels transition");noTaskRemoval(task);
            check(InstagramRootReels.matches(scene.activity,destination.reel),"second native removal also reveals root Reels");
        }});
        tests.put("queued_native_pop_runs_before_post_body_observer",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);Object d=dispatcher(scene);Destination destination=new Destination(scene);final boolean[] popped={false};
            scene.activity.selectClipsAction=()->{check(popped[0],"selector cannot run before queued native transaction");destination.bind();};
            check(!OneBackExit.tryInstagramDispatcherBack(d),"original AndroidX body is used");
            new Handler(Looper.getMainLooper()).post(()->{removeUnchecked(scene,profile);popped[0]=true;});
            OneBackExit.completeInstagramDispatcherBack(d);Handler.drain();destination.assertRoot();noTaskRemoval(task);nextReelsBack(scene,destination,task);
        }});
        tests.put("uncompleted_native_body_cannot_schedule_navigation",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);new Destination(scene);
            OneBackExit.enterBack(scene.activity);try{remove(scene,profile);}finally{OneBackExit.leaveNative();}
            Handler.drain();none(scene,task);
        }});
        tests.put("unchanged_profile_comment_close_does_not_navigate",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);new Destination(scene);
            normalNative(scene,()->{profile.profileBackCalls++;});Handler.drain();none(scene,task);
        }});
        tests.put("child_return_only_reveals_entry",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);hide(profile);Node child=scene.node("X.NativeProfile",null,MAIN);scene.select(child,true);ActivityManager.AppTask task=own(scene);new Destination(scene);
            normalNative(scene,()->{try{erase(scene,child);reveal(scene,profile);}catch(Exception e){throw new AssertionError(e);}});Handler.drain();none(scene,task);
        }});
        tests.put("modal_wrapper_removal_detected_with_retained_child_flags",()->{try(ModalScene f=new ModalScene()){
            Destination destination=new Destination(f.scene);normalNative(f.scene,()->removeUnchecked(f.scene,f.modal));
            check(f.profile.added,"child flag intentionally remains until deferred teardown");Handler.drain();destination.assertRoot();noTaskRemoval(f.task);nextReelsBack(f.scene,destination,f.task);
        }});
        tests.put("unverified_modal_parent_removal_not_authority",()->{try(ModalScene f=new ModalScene()){
            set(f.host,"A06",new WeakReference<Object>(null));new Destination(f.scene);normalNative(f.scene,()->removeUnchecked(f.scene,f.modal));Handler.drain();none(f.scene,f.task);
        }});
        for(final String host:new String[]{"url","launcher","url_nonroot"})tests.put(host+"_normal_finish_requests_Main_Reels",()->{try(Scene scene=new Scene(host)){
            linked(scene);ActivityManager.AppTask task=own(scene);normalNative(scene,()->scene.activity.finish());Handler.drain();expectedIntent(scene);noTaskRemoval(task);
        }});
        tests.put("finishing_Main_uses_new_native_Main_intent",()->{try(Scene scene=new Scene()){
            linked(scene);ActivityManager.AppTask task=own(scene);normalNative(scene,()->scene.activity.finish());Handler.drain();expectedIntent(scene);noTaskRemoval(task);
        }});
        tests.put("destroyed_finishing_URL_does_not_lose_normal_completion",()->{try(Scene scene=new Scene("url")){
            linked(scene);ActivityManager.AppTask task=own(scene);
            normalNative(scene,()->{scene.activity.finish();scene.activity.destroyed=true;scene.activity.taskIdOverride=-1;OneBackExit.onDestroy(scene.activity);});
            Handler.drain();expectedIntent(scene);noTaskRemoval(task);
        }});
        for(final String kind:new String[]{"destroyed","configuration"})tests.put(kind+"_without_finish_cannot_handoff",()->{try(Scene scene=new Scene("url")){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);
            normalNative(scene,()->{removeUnchecked(scene,profile);if(kind.equals("configuration"))scene.activity.changingConfigurations=true;else scene.activity.destroyed=true;});Handler.drain();none(scene,task);
        }});
        tests.put("predictive_preparation_remove_before_commit",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);Destination destination=new Destination(scene);Object d=dispatcher(scene);
            check(!OneBackExit.startInstagramDispatcherGesture(d),"native predictive preparation retained");remove(scene,profile);
            check(!OneBackExit.tryInstagramDispatcherBack(d),"commit retains original completion even after preparation removed profile");noTaskRemoval(task);
            OneBackExit.completeInstagramDispatcherBack(d);Handler.drain();destination.assertRoot();noTaskRemoval(task);nextReelsBack(scene,destination,task);
        }});
        tests.put("predictive_cancel_restores_entry_without_handoff",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);new Destination(scene);Object d=dispatcher(scene);
            check(!OneBackExit.startInstagramDispatcherGesture(d),"native prediction retained");profile.removing=true;
            check(!OneBackExit.cancelInstagramDispatcherGesture(d),"cancel preserves native restoration");profile.removing=false;Handler.drain();none(scene,task);
        }});
        tests.put("new_intent_revokes_completed_old_attempt",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);new Destination(scene);normalNative(scene,()->removeUnchecked(scene,profile));
            OneBackExit.onNewIntent(scene.activity,new Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse("https://www.instagram.com/reel/other/")));Handler.drain();none(scene,task);
        }});
        tests.put("selection_waits_for_real_wrapper_binding",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);Destination destination=new Destination(scene);destination.deferred=true;
            normalNative(scene,()->removeUnchecked(scene,profile));Handler.drain();
            check(scene.activity.selectClipsCalls==1&&scene.activity.resetClipsCalls==0,"selection request does not invent an attached wrapper");
            check(Handler.queuedCount()==0,"no polling or forced layout queued");noTaskRemoval(task);
            destination.bind();InstagramProfileReels.draw(scene.activity);destination.assertRoot();nextReelsBack(scene,destination,task);
        }});
        tests.put("retained_CLIPS_child_is_cleared_after_selection",()->{try(Scene scene=new Scene()){
            Destination destination=new Destination(scene);destination.bind();hide(destination.reel);Node child=scene.node("X.NativeProfile",destination.wrapper,BASE);set(scene.child(destination.wrapper),"count",1);
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);normalNative(scene,()->removeUnchecked(scene,profile));Handler.drain();
            check(!child.added,"native HNw cleared retained destination child");destination.assertRoot();noTaskRemoval(task);nextReelsBack(scene,destination,task);
        }});
        for(final String fault:new String[]{"pending","executing","saved","destroyed"})tests.put("waits_for_"+fault+"_destination_manager",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);Destination destination=new Destination(scene);final Object[] manager={null};
            scene.activity.selectClipsAction=()->{try{destination.bind();manager[0]=scene.child(destination.wrapper);if(fault.equals("pending"))((java.util.List<Object>)value(manager[0],"A0W")).add(new Object());else set(manager[0],fault.equals("executing")?"mExecutingActions":fault.equals("saved")?"saved":"A0G",true);}catch(Exception e){throw new AssertionError(e);}};
            normalNative(scene,()->removeUnchecked(scene,profile));Handler.drain();check(scene.activity.resetClipsCalls==0,"unsafe manager cannot be reset");noTaskRemoval(task);
            if(fault.equals("pending"))((java.util.List<?>)value(manager[0],"A0W")).clear();else set(manager[0],fault.equals("executing")?"mExecutingActions":fault.equals("saved")?"saved":"A0G",false);
            InstagramProfileReels.draw(scene.activity);destination.assertRoot();nextReelsBack(scene,destination,task);
        }});
        tests.put("explicit_tab_change_cancels_pending_reset",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);Destination destination=new Destination(scene);destination.deferred=true;
            normalNative(scene,()->removeUnchecked(scene,profile));Handler.drain();set(scene.controller,"current",scene.enumTab(true));InstagramProfileReels.draw(scene.activity);
            destination.bind();InstagramProfileReels.draw(scene.activity);check(scene.activity.resetClipsCalls==0,"later user tab choice is not overwritten");noTaskRemoval(task);
        }});
        tests.put("new_Main_link_revokes_old_URL_completion",()->{try(Scene old=new Scene("url")){
            Node profile=linked(old);ActivityManager.AppTask task=own(old);Object attempt=InstagramProfileReels.begin(old.activity);
            check(attempt!=null,"old URL attempt captured while profile attached");old.activity.finish();old.activity.destroyed=true;old.activity.taskIdOverride=-1;OneBackExit.onDestroy(old.activity);
            try(Scene fresh=new Scene("",old.app)){
                linked(fresh);InstagramProfileReels.complete(attempt);Handler.drain();
                check(old.activity.startCalls==0&&fresh.activity.selectClipsCalls==0,"new accepted Main link wins over stale URL observer");noTaskRemoval(task);
            }
        }});
        tests.put("touch_cancels_pending_wrapper_reset",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);Destination destination=new Destination(scene);destination.deferred=true;
            normalNative(scene,()->removeUnchecked(scene,profile));Handler.drain();InstagramProfileReels.interaction(scene.activity);
            destination.bind();InstagramProfileReels.draw(scene.activity);check(scene.activity.resetClipsCalls==0,"a later user interaction cancels the pending reset");noTaskRemoval(task);
        }});
        for(final String fault:new String[]{"pending","saved"})tests.put("root_"+fault+"_waits_before_selection",()->{try(Scene scene=new Scene()){
            Node profile=linked(scene);ActivityManager.AppTask task=own(scene);Destination destination=new Destination(scene);
            if(fault.equals("pending"))((java.util.List<Object>)value(scene.manager,"A0W")).add(new Object());else set(scene.manager,"saved",true);
            normalNative(scene,()->removeUnchecked(scene,profile));Handler.drain();
            check(scene.activity.selectClipsCalls==0&&scene.activity.resetClipsCalls==0,"native selector is not invoked while root manager defers actions");noTaskRemoval(task);
            if(fault.equals("pending"))((java.util.List<?>)value(scene.manager,"A0W")).clear();else set(scene.manager,"saved",false);
            InstagramProfileReels.draw(scene.activity);destination.assertRoot();nextReelsBack(scene,destination,task);
        }});
        tests.put("fresh_handoff_overrides_startup_default_without_mutating_browser_intent",()->{try(Scene scene=new Scene("url")){
            Intent original=new Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse(URL))
                .putExtra("MainActivityAccountHelper.STARTUP_TAB","FEED").putExtra(ENTRANCE,"FEED");
            scene.activity.setIntent(original);linked(scene);ActivityManager.AppTask task=own(scene);
            normalNative(scene,()->scene.activity.finish());Handler.drain();expectedIntent(scene);noTaskRemoval(task);
            check(scene.activity.lastStart!=original,"handoff is a separate explicit native Intent");
            check("FEED".equals(original.getStringExtra("MainActivityAccountHelper.STARTUP_TAB"))
                &&"FEED".equals(original.getStringExtra(ENTRANCE))&&URL.equals(original.getData().toString()),"original browser route and startup preference are unchanged");
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception{int beforeCases=cases,beforeAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> item:scenarios().entrySet())try{test(item.getKey(),item.getValue());}catch(AssertionError failure){failed++;System.out.println("FAIL "+item.getKey()+": "+failure.getMessage());Handler.drain();}
        if(failed!=0)throw new AssertionError("Back137InstagramProfileReelsTest failed="+failed);
        System.out.println("PASS Back137InstagramProfileReelsTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions; explicit native removal/tab binding models, no device execution");}
}
