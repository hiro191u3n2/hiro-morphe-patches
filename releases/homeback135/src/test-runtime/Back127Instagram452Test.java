package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import com.instagram.clips.intf.ClipsViewerConfig;
import com.instagram.clips.intf.ClipsViewerSource;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;

/** The 452 descriptors come from the supplied APK. Route transitions and the
 * new native Reels guard are explicit host-model inputs, not an Android replay.
 * The actual guard-preserving insertion is checked by the separate DEX audits. */
public final class Back127Instagram452Test {
    static ActivityManager.AppTask own(Scene s){
        ActivityManager.RecentTaskInfo info=new ActivityManager.RecentTaskInfo();info.taskId=11;
        info.baseActivity=new ComponentName("com.instagram.android","Main");info.baseIntent=new Intent().setComponent(info.baseActivity);
        ActivityManager.AppTask task=new ActivityManager.AppTask(info);task.activities.add(s.activity);s.tasks.tasks.add(task);return task;
    }
    static void preserved(Scene s,ActivityManager.AppTask task){
        check(!s.activity.finishing&&s.activity.finishCalls==0&&s.activity.removeCalls==0,"Activity remains open");
        check(!task.excluded&&task.removeCalls==0,"owned task stays in Recents");
    }
    static void removed(Scene s,ActivityManager.AppTask task){
        check(task.excluded&&task.removeCalls==1,"owned task excluded and removed once");
        check(task.events.equals(Arrays.asList("exclude","remove")),"exclude precedes remove");
    }
    static void footer(Scene s,Node page){
        s.group((ViewGroup)page.view,"clips_viewer_container",new Rect(0,0,1080,2170));
    }
    static Node current(Scene s,boolean home)throws Exception {
        Node page=s.node(home?"X.00nV":"X.02Hr",null,MAIN);s.select(page,home);if(!home)footer(s,page);return page;
    }
    static void source(Node page,ClipsViewerSource origin){page.config=new ClipsViewerConfig(origin);}
    static Object bind(Scene s){Object dispatcher=new Object();OneBackExit.bindInstagramDispatcher(s.activity,dispatcher);return dispatcher;}
    static void erase(Scene s,Node page)throws Exception {
        added(page.getClass().getField("mFragmentManager").get(page)).remove(page);
        if(page.view!=null&&page.view.getParent() instanceof ViewGroup)((ViewGroup)page.view.getParent()).removeView(page.view);
        page.added=false;page.visible=false;page.resumed=false;
    }
    static void earlyNative(Scene s,Node page,Object dispatcher)throws Exception {
        check(!OneBackExit.tryBack(s.activity),"ordinary Back reaches 452 native guard");
        check(!OneBackExit.key(s.activity,down())&&!OneBackExit.key(s.activity,up()),"key pair reaches native guard");
        check(!OneBackExit.startInstagramDispatcherGesture(dispatcher),"prediction retains native guard");
        check(!OneBackExit.progressInstagramDispatcherGesture(dispatcher),"native gesture progresses");
        check(!OneBackExit.cancelInstagramDispatcherGesture(dispatcher),"native cancellation retained");
        check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"AndroidX dispatch reaches native guard");
        OneBackExit.enterBack(s.activity);
        try{check(!OneBackExit.tryInstagramMainReelsBack(s.activity),"Main hook cannot bypass new guard");}
        finally{OneBackExit.leaveNative();}
    }
    static Map<String,Test> scenarios(){Map<String,Test> tests=new LinkedHashMap<String,Test>();
        tests.put("452_home_current_and_exit",()->{try(Scene s=new Scene()){
            Node home=current(s,true);ActivityManager.AppTask task=own(s);
            check(Instagram452.supports(s.activity),"A1f/A08 identify verified 452 contract");
            InstagramCurrent.Result result=InstagramCurrent.inspect(s.activity);
            check(result.nativeRecognized&&result.stateClear&&result.kind==InstagramCurrent.HOME&&result.target==home,"452 HOME recognized");
            check(InstagramActivityLayers.inspect(s.activity)==InstagramActivityLayers.CLEAR,"452 Activity layers read clear");
            check(OneBackExit.tryBack(s.activity),"verified HOME still exits directly");removed(s,task);
        }});
        tests.put("452_reels_defers_to_new_native_guard_then_exits",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.CLIPS_TAB);ActivityManager.AppTask task=own(s);
            InstagramCurrent.Result result=InstagramCurrent.inspect(s.activity);
            check(result.nativeRecognized&&result.kind==InstagramCurrent.REELS&&result.target==video,"452 Reels recognized");
            check(InstagramReelsExit.current(s.activity).target==video,"exact current receiver mapped");
            earlyNative(s,video,bind(s));preserved(s,task);
            check(OneBackExit.tryInstagramReelsBack(video),"post-guard native receiver exits");removed(s,task);
        }});
        for(final ClipsViewerSource origin:new ClipsViewerSource[]{ClipsViewerSource.PROFILE,ClipsViewerSource.SELF_PROFILE,
                ClipsViewerSource.GROUP_PROFILE,ClipsViewerSource.FEED_CONTEXTUAL_PROFILE})
            tests.put("452_profile_origin_"+origin,()->{try(Scene s=new Scene()){
                Node video=current(s,false);source(video,origin);video.module="clips_viewer_clips_tab";ActivityManager.AppTask task=own(s);
                check(InstagramProfileBack.fromProfile(video)&&InstagramProfileBack.current(s.activity),"A0o config outranks analytics override");
                earlyNative(s,video,bind(s));
                check(!OneBackExit.tryInstagramReelsBack(video),"profile native continuation retained");
                check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,video),"CLIPS tab cannot override profile source");preserved(s,task);
            }});
        tests.put("452_nonprofile_enum_is_authoritative",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.CLIPS_TAB);video.module="clips_viewer_profile";ActivityManager.AppTask task=own(s);
            check(!InstagramProfileBack.fromProfile(video),"nonprofile config overrides misleading module");
            check(OneBackExit.tryInstagramReelsBack(video),"native root exits");removed(s,task);
        }});
        for(final String wrapperType:new String[]{"X.00VS","X.02Wh","X.052y","X.04Bv"})
            tests.put("452_wrapper_profile_"+wrapperType,()->{try(Scene s=new Scene()){
                Node wrapper=s.node(wrapperType,null,MAIN);s.select(wrapper,false);
                if(wrapperType.equals("X.00VS")){s.cacheTab(wrapper,"clips");Node base=s.node("X.02Hr",wrapper,BASE);source(base,ClipsViewerSource.CLIPS_TAB);base.resumed=false;}
                if(wrapperType.equals("X.052y"))s.merged(wrapper,1);
                if(wrapperType.equals("X.04Bv"))s.pager(wrapper,1);
                Node video=s.node("X.Ze9",wrapper,wrapperType.equals("X.052y")||wrapperType.equals("X.04Bv")?1936:MAIN);
                if(wrapperType.equals("X.052y"))s.tag(video,"merged_feed_tab_1");
                if(wrapperType.equals("X.04Bv"))s.tag(video,"f1");
                source(video,ClipsViewerSource.PROFILE);footer(s,video);ActivityManager.AppTask task=own(s);
                check(Instagram452.content(s.activity,wrapper)==video,"selected native child chain follows 452 descriptor");
                check(InstagramProfileBack.current(s.activity),"profile origin follows selected child");
                check(Instagram452.deferReels(s.activity),"all selected Reels wrappers preserve native guard");
                check(!OneBackExit.tryInstagramReelsBack(video),"profile receiver returns natively");
                check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,wrapper),"selected wrapper preserves profile source");preserved(s,task);
            }});
        tests.put("452_uninitialized_selector_stays_native",()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.052y",null,MAIN);s.select(wrapper,false);s.merged(wrapper,null);
            ActivityManager.AppTask task=own(s);
            check(Instagram452.content(s.activity,wrapper)==null,"uninitialized selector remains unread");
            check(Instagram452.deferReels(s.activity),"unknown selector cannot authorize early Reels exit");
            check(!OneBackExit.tryInstagramDispatcherBack(bind(s)),"unresolved selected child keeps native dispatch");preserved(s,task);
        }});
        tests.put("452_profile_native_scope_survives_pop",()->{try(Scene s=new Scene()){
            Node profile=current(s,false);source(profile,ClipsViewerSource.PROFILE);ActivityManager.AppTask task=own(s);OneBackExit.enterBack(s.activity);Node clips;
            try{erase(s,profile);clips=current(s,false);source(clips,ClipsViewerSource.CLIPS_TAB);
                check(!OneBackExit.tryInstagramReelsBack(clips),"same Back cannot exit revealed Reels");
                check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,clips),"same Back cannot exit via tab");preserved(s,task);
            }finally{OneBackExit.leaveNative();}
            check(OneBackExit.tryInstagramReelsBack(s.activity.current),"next independent root Back exits");removed(s,task);
        }});
        tests.put("452_profile_dispatch_survives_pop",()->{try(Scene s=new Scene()){
            Node profile=current(s,false);source(profile,ClipsViewerSource.PROFILE);ActivityManager.AppTask task=own(s);
            check(!OneBackExit.tryInstagramDispatcherBack(bind(s)),"AndroidX profile dispatch retained");
            erase(s,profile);Node clips=current(s,false);source(clips,ClipsViewerSource.CLIPS_TAB);
            check(!OneBackExit.tryInstagramReelsBack(clips),"same dispatch remembers source");
            check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,clips),"same tab continuation remembers source");preserved(s,task);
            Handler.drain();check(OneBackExit.tryInstagramReelsBack(clips),"later independent native Back exits");removed(s,task);
        }});
        tests.put("452_comments_close_at_native_guard_before_exit",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.CLIPS_TAB);ActivityManager.AppTask task=own(s);
            View panel=s.group((ViewGroup)video.view,"comments_sheet",new Rect(0,900,1080,2200));
            Object dispatcher=bind(s);check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"comments retain native dispatch");
            check(!OneBackExit.tryInstagramMainReelsBack(s.activity),"Main cannot exit behind comments");
            // The actual 452 guard may consume Back before the inserted hook.
            // Model that native return by removing the panel without invoking it.
            ((ViewGroup)video.view).removeView(panel);preserved(s,task);Handler.drain();
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"next dispatch still runs original native guard");
            check(OneBackExit.tryInstagramReelsBack(video),"subsequent clear native receiver exits");removed(s,task);
        }});
        tests.put("452_profile_comments_then_original_profile",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.PROFILE);ActivityManager.AppTask task=own(s);
            View panel=s.group((ViewGroup)video.view,"comments_sheet",new Rect(0,900,1080,2200));
            check(!OneBackExit.tryInstagramDispatcherBack(bind(s)),"profile comments dispatch stays native");
            ((ViewGroup)video.view).removeView(panel);Handler.drain();
            check(!OneBackExit.tryInstagramReelsBack(video),"next native Back preserves original profile route");preserved(s,task);
        }});
        tests.put("452_tab_boundary_uses_exact_receiver_and_enum",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.CLIPS_TAB);ActivityManager.AppTask task=own(s);
            set(s.controller,"current",s.enumTab(true));check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,video),"FEED remains native at CLIPS boundary");
            set(s.controller,"current",s.enumTab(false));check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,new Object()),"receiver mismatch remains native");
            check(OneBackExit.tryInstagramTabBack(s.activity,s.controller,video),"verified CLIPS pop boundary exits");removed(s,task);
        }});
        tests.put("452_home_saved_pending_and_activity_layers_block_exit",()->{try(Scene s=new Scene()){
            current(s,true);ActivityManager.AppTask task=own(s);((ManagerState)s.manager).saved=true;
            check(!InstagramCurrent.inspect(s.activity).stateClear&&!OneBackExit.tryBack(s.activity),"A15 saved state blocks Home exit");
            ((ManagerState)s.manager).saved=false;((java.util.List<Object>)s.manager.getClass().getField("A0W").get(s.manager)).add(new Object());
            check(!InstagramCurrent.inspect(s.activity).stateClear,"pending transactions block Home exit");
            ((java.util.List<?>)s.manager.getClass().getField("A0W").get(s.manager)).clear();
            Object modal=s.create("X.0A4v");Node page=(Node)s.create("X.03CG");set(modal,"A06",new WeakReference<Object>(page));set(s.activity,"A09",modal);
            check(InstagramActivityLayers.inspect(s.activity)==InstagramActivityLayers.BLOCKED,"452 modal weak reference is read");preserved(s,task);
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception {
        int beforeCases=cases,beforeAssertions=assertions;
        for(Map.Entry<String,Test> entry:scenarios().entrySet())test(entry.getKey(),entry.getValue());
        System.out.println("PASS Back127Instagram452Test: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions");
    }
}
