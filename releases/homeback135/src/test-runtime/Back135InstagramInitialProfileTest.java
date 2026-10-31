package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;
import static app.hiro.oneback.runtime.Back130InstagramProfileLinkTest.*;

/** Native A09/01PZ can attach the entry to root MAIN while the tab selector
 * remains null or points at a retained tab. Deliberately never select(profile).
 * Host relationships model the audited native shape, not device execution. */
public final class Back135InstagramInitialProfileTest {
    static final class Initial implements AutoCloseable {
        final Scene s=new Scene();final Node profile;final ActivityManager.AppTask task;
        Initial()throws Exception{this(true);}
        Initial(boolean register)throws Exception {
            profile=detached(s);
            if(register)OneBackExit.rememberInstagramExternalProfile(s.activity,profile,route(s,URL,"yoasobi_staff_"));
            check(profile.activity==null&&!profile.added,"native registration precedes attachment");
            profile.activity=s.activity;profile.view=new ViewGroup(s.activity);profile.added=true;profile.visible=true;profile.resumed=true;
            set(profile,"mFragmentManager",s.manager);set(profile,"mParentFragment",null);set(profile,"mFragmentId",MAIN);
            set(profile,"mChildFragmentManager",s.newManager());added(s.manager).add(profile);s.root.addView(profile.view);
            s.profileTabs(profile,true);task=own(s);
            check(s.activity.current==null&&value(s.controller,"selected")==null,"Main/tab getters are not fabricated as profile");
        }
        Node push(String type)throws Exception{hide(profile);return s.node(type,null,MAIN);}
        void pop(Node child)throws Exception{erase(s,child);profile.resumed=true;profile.visible=true;profile.view.hidden=false;}
        void rejected()throws Exception {
            check(!InstagramExternalProfile.canDispatchBack(s.activity),"root guard cannot grant profile dispatch");
            check(!back(profile),"failed root guard leaves native profile result");preserved(s,task);
        }
        public void close(){s.close();}
    }
    interface Change{void run(Initial f)throws Exception;}
    static void reject(Map<String,Test> tests,String name,Change change) {
        tests.put(name,()->{try(Initial f=new Initial()){change.run(f);f.rejected();}});
    }
    static Map<String,Test> scenarios() {
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        tests.put("first_root_profile_native_back_with_null_tab",()->{try(Initial f=new Initial()){
            check(back(f.profile),"first direct-root profile exits after native false result");removed(f.s,f.task);
            check(f.s.activity.nativeCurrentReads==0,"direct root eligibility never asks Main for a Home tab");
        }});
        tests.put("first_root_profile_dispatch_with_null_tab",()->{try(Initial f=new Initial()){
            check(InstagramExternalProfile.canDispatchBack(f.s.activity),"root transaction independently authorizes native profile dispatch");
            check(f.s.activity.nativeCurrentReads==0,"root eligibility does not inspect tab selection");
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher(f.s)),"dispatcher reaches first root profile native guard");
            check(f.profile.profileBackCalls==1,"native profile handler executes once");removed(f.s,f.task);
        }});
        tests.put("first_root_profile_without_selected_tab_enum",()->{try(Initial f=new Initial()){
            set(f.s.controller,"current",null);check(back(f.profile),"startup tab stack can be empty");removed(f.s,f.task);
        }});
        tests.put("first_root_profile_before_navigation_controller",()->{try(Initial f=new Initial()){
            set(f.s.activity,"A08",null);check(back(f.profile),"root profile does not depend on navigation initialization");removed(f.s,f.task);
        }});
        tests.put("first_root_profile_with_retained_uncached_tab",()->{try(Initial f=new Initial()){
            Node retained=f.s.node("X.00VS",null,0);f.s.tag(retained,"f0");retained.resumed=false;retained.visible=false;
            f.s.root.removeView(retained.view);retained.view=null;f.s.cacheTab(retained,null);
            f.s.activity.current=retained;set(f.s.controller,"selected",retained);
            check(back(f.profile),"root MAIN profile remains current above an uncreated retained tab");removed(f.s,f.task);
            check(f.s.activity.nativeCurrentReads==0,"retained tab lazy is not evaluated");
        }});
        tests.put("first_root_profile_does_not_call_throwing_main_getter",()->{try(Initial f=new Initial()){
            f.s.activity.failCurrent=true;check(back(f.profile),"root transaction needs no Main getter");removed(f.s,f.task);
            check(f.s.activity.nativeCurrentReads==0,"Main getter was never invoked");
        }});
        tests.put("later_root_profile_with_initialized_tab_still_exits",()->{try(Initial f=new Initial()){
            Node retained=f.s.node("X.00VS",null,0);f.s.tag(retained,"f0");retained.resumed=false;retained.visible=false;retained.view.hidden=true;
            f.s.activity.current=retained;set(f.s.controller,"selected",retained);
            check(back(f.profile),"root profile also exits with a real initialized tab wrapper");removed(f.s,f.task);
        }});
        tests.put("initial_root_failed_guard_does_not_read_tab_selector",()->{try(Initial f=new Initial()){
            ((ManagerState)f.s.manager).saved=true;
            check(!InstagramExternalProfile.canDispatchBack(f.s.activity),"saved root is authoritative rejection");
            check(f.s.activity.nativeCurrentReads==0,"failed root guard never falls through to Main getter");preserved(f.s,f.task);
        }});
        tests.put("initial_root_profile_native_consumption_preserved",()->{try(Initial f=new Initial()){
            check(back(f.profile,true,null),"native consumed result is preserved");preserved(f.s,f.task);
            Handler.drain();check(back(f.profile),"later independent false result exits");removed(f.s,f.task);
        }});
        tests.put("initial_root_profile_requires_registration",()->{try(Initial f=new Initial(false)){f.rejected();}});
        reject(tests,"initial_root_wrong_manager",f->set(f.profile,"mFragmentManager",f.s.newManager()));
        reject(tests,"initial_root_wrong_container",f->set(f.profile,"mFragmentId",MAIN+1));
        reject(tests,"initial_root_different_profile_selected",f->f.s.node(PROFILE,null,MAIN));
        reject(tests,"initial_root_profile_not_added",f->f.profile.added=false);
        reject(tests,"initial_root_profile_not_visible",f->f.profile.visible=false);
        reject(tests,"initial_root_profile_not_resumed",f->f.profile.resumed=false);
        reject(tests,"initial_root_saved",f->((ManagerState)f.s.manager).saved=true);
        reject(tests,"initial_root_executing",f->set(f.s.manager,"mExecutingActions",true));
        reject(tests,"initial_root_pending",f->((java.util.List<Object>)value(f.s.manager,"A0W")).add(new Object()));
        reject(tests,"initial_root_visible_other_sibling",f->f.s.node("X.NativeProfile",null,123));
        reject(tests,"initial_root_visible_retained_tab",f->{Node tab=f.s.node("X.00VS",null,0);tab.resumed=false;});
        reject(tests,"initial_root_child_stack",f->((ManagerState)f.s.child(f.profile)).count=1);
        reject(tests,"initial_root_child_pending",f->((java.util.List<Object>)value(f.s.child(f.profile),"A0W")).add(new Object()));
        reject(tests,"initial_root_ime",f->f.s.root.insets.ime=true);
        reject(tests,"initial_root_comments_sheet",f->f.s.group((ViewGroup)f.profile.view,"comment_sheet",new Rect(0,650,1080,2200)));
        reject(tests,"initial_root_live_modal_service",f->{Object service=f.s.create("X.0A4v");Node modal=(Node)f.s.create("X.03CG");
            set(service,"A06",new WeakReference<Object>(modal));set(f.s.activity,"A09",service);});
        reject(tests,"initial_root_native_activity_layer",f->{Object layer=f.s.create("X.00i7");set(layer,"A0W",true);
            ((NativeActivity452Fixture.LayerState)layer).current=f.profile;f.s.loader.loadClass("com.instagram.base.activity.IgFragmentActivity").getField("A00").set(f.s.activity,layer);});
        tests.put("initial_root_dialog_preserved",()->{try(Initial f=new Initial()){
            Dialog dialog=new Dialog(f.s.activity);dialog.show();dialog.getWindow().getDecorView().focus(true);
            try{f.rejected();}finally{dialog.dismiss();f.s.root.focus(true);}
        }});
        tests.put("initial_root_parented_profile_stays_on_existing_route",()->{try(Initial f=new Initial()){
            Node parent=(Node)f.s.create("X.03CG");set(f.profile,"mParentFragment",parent);
            check(!back(f.profile),"unverified parent is not a direct root profile");preserved(f.s,f.task);
        }});
        tests.put("initial_root_popup_close_then_next_back",()->{try(Initial f=new Initial()){
            Object presenter=f.s.create("X.0DiR");final PopupState popup=(PopupState)f.s.create("X.01MU");popup.open=true;
            set(presenter,"A01",popup);set(f.profile,"A0p",presenter);
            check(back(f.profile,false,()->popup.open=false),"native popup closure consumes this Back");preserved(f.s,f.task);
            Handler.drain();check(back(f.profile),"next independent Back exits entry");removed(f.s,f.task);
        }});
        tests.put("initial_root_ime_close_then_next_back",()->{try(Initial f=new Initial()){
            f.s.root.insets.ime=true;check(back(f.profile,false,()->f.s.root.insets.ime=false),"IME closure cannot become same-Back exit");preserved(f.s,f.task);
            Handler.drain();check(back(f.profile),"next independent clear Back exits");removed(f.s,f.task);
        }});
        tests.put("initial_root_child_return_requires_next_back",()->{try(Initial f=new Initial()){
            Node child=f.push("X.NativeProfile");Object d=dispatcher(f.s);
            check(!OneBackExit.tryInstagramDispatcherBack(d),"child uses native navigation");f.pop(child);
            check(back(f.profile),"same child-return Back is consumed without exit");preserved(f.s,f.task);
            Handler.drain();check(back(f.profile),"new Back exits original profile");removed(f.s,f.task);
        }});
        tests.put("initial_root_profile_video_return_requires_next_back",()->{try(Initial f=new Initial()){
            Node video=f.push("X.02Hr");video.config=new com.instagram.clips.intf.ClipsViewerConfig(com.instagram.clips.intf.ClipsViewerSource.PROFILE);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher(f.s)),"profile video remains native");f.pop(video);
            check(back(f.profile),"same video-return Back cannot exit profile");preserved(f.s,f.task);
            Handler.drain();check(back(f.profile),"following Back exits original external profile");removed(f.s,f.task);
        }});
        tests.put("initial_root_target_changes_inside_native_handler",()->{try(Initial f=new Initial()){
            check(!back(f.profile,false,()->{try{f.push("X.NativeProfile");}catch(Exception e){throw new RuntimeException(e);}}),"native navigation wins over entry exit");preserved(f.s,f.task);
        }});
        tests.put("initial_root_physical_back",()->{try(Initial f=new Initial()){
            check(!OneBackExit.key(f.s.activity,down())&&!OneBackExit.key(f.s.activity,up()),"physical key remains native until profile handler");
            OneBackExit.enterBack(f.s.activity);try{check(back(f.profile),"first physical Back exits at native profile boundary");}finally{OneBackExit.leaveNative();}
            removed(f.s,f.task);
        }});
        tests.put("initial_root_predictive_cancel",()->{try(Initial f=new Initial()){
            Object d=dispatcher(f.s);check(OneBackExit.startInstagramDispatcherGesture(d),"first root profile prediction suppresses pop");
            check(OneBackExit.cancelInstagramDispatcherGesture(d),"cancel does not dispatch native profile");
            check(f.profile.profileBackCalls==0,"cancel invokes no native handler");preserved(f.s,f.task);
        }});
        tests.put("initial_root_predictive_commit",()->{try(Initial f=new Initial()){
            Object d=dispatcher(f.s);check(OneBackExit.startInstagramDispatcherGesture(d),"first root prediction starts");
            check(OneBackExit.tryInstagramDispatcherBack(d),"commit runs native profile guard");
            check(f.profile.profileBackCalls==1,"one native invocation on commit");removed(f.s,f.task);
        }});
        tests.put("initial_root_canceled_key_preserved",()->{try(Initial f=new Initial()){
            OneBackExit.key(f.s.activity,down());OneBackExit.key(f.s.activity,new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_BACK,true));
            check(back(f.profile),"canceled key cannot authorize exit");preserved(f.s,f.task);
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception {
        int beforeCases=cases,beforeAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> item:scenarios().entrySet())try{test(item.getKey(),item.getValue());}
            catch(Throwable problem){failed++;System.out.println("FAIL "+item.getKey()+": "+problem);}
        if(failed!=0)throw new AssertionError("Back135InstagramInitialProfileTest failed="+failed);
        System.out.println("PASS Back135InstagramInitialProfileTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions; host models, no device execution");
    }
}
