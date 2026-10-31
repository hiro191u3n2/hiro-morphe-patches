package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import com.instagram.clips.intf.ClipsViewerConfig;
import com.instagram.clips.intf.ClipsViewerSource;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Regression for the real policy gap: CLIPS is still selected behind a nested
 * profile/video, and the video's source is absent or reads CLIPS_TAB. These are
 * navigation models using the audited 447/452 descriptors, not device UI runs. */
public final class Back128InstagramProfileTest {
    static final class Scene implements AutoCloseable {
        final boolean modern;
        final InstagramCurrent121Fixture.Scene old;
        final InstagramCurrent452Fixture.Scene fresh;
        final Activity activity;
        final Object controller,manager;
        final int main,base;
        Scene(boolean modern)throws Exception {
            this.modern=modern;old=modern?null:new InstagramCurrent121Fixture.Scene();fresh=modern?new InstagramCurrent452Fixture.Scene():null;
            activity=modern?fresh.activity:old.activity;controller=modern?fresh.controller:old.controller;manager=modern?fresh.manager:old.manager;
            main=modern?InstagramCurrent452Fixture.MAIN:InstagramCurrent121Fixture.MAIN;
            base=modern?InstagramCurrent452Fixture.BASE:InstagramCurrent121Fixture.BASE;
        }
        String type(String older,String newer){return modern?newer:older;}
        Object node(String type,Object parent,int id)throws Exception {
            return modern?fresh.node(type,(InstagramCurrent452Fixture.Node)parent,id):old.node(type,(InstagramCurrent121Fixture.Node)parent,id);
        }
        Object video(Object parent,int id)throws Exception {
            Object v=node(type("X.8Zv","X.02Hr"),parent,id);
            field(v,"config",new ClipsViewerConfig(ClipsViewerSource.CLIPS_TAB));
            field(v,"module","clips_viewer_clips_tab");
            ViewGroup view=(ViewGroup)get(v,"view");
            if(modern)fresh.group(view,"clips_viewer_container",new Rect(0,0,1080,2170));
            else old.group(view,"clips_viewer_container",new Rect(0,0,1080,2170));
            return v;
        }
        void select(Object root,boolean home)throws Exception {
            if(modern)fresh.select((InstagramCurrent452Fixture.Node)root,home);else old.select((InstagramCurrent121Fixture.Node)root,home);
        }
        Object child(Object node)throws Exception{return get(node,"mChildFragmentManager");}
        Object wrapper()throws Exception {
            Object w=node(type("X.0jG","X.00VS"),null,main);select(w,false);
            if(modern)fresh.cacheTab((InstagramCurrent452Fixture.Node)w,"clips");else old.cacheTab((InstagramCurrent121Fixture.Node)w,"clips");
            return w;
        }
        Object rootVideo()throws Exception {Object v=video(null,main);select(v,false);return v;}
        Object nested(String kind,Object parent,int id)throws Exception {
            String type=kind.equals("merged")?type("X.22c","X.052y"):kind.equals("pager")?type("X.9sY","X.04Bv"):type("X.3gY","X.02Wh");
            Object w=node(type,parent,id);
            if(kind.equals("merged")){if(modern)fresh.merged((InstagramCurrent452Fixture.Node)w,1);else old.merged((InstagramCurrent121Fixture.Node)w,1);}
            if(kind.equals("pager")){if(modern)fresh.pager((InstagramCurrent452Fixture.Node)w,1);else old.pager((InstagramCurrent121Fixture.Node)w,1);}
            Object v=video(w,kind.equals("ordinary")?main:1936);
            if(kind.equals("merged"))field(v,"mTag","merged_feed_tab_1");
            if(kind.equals("pager"))field(v,"mTag","f1");
            return v;
        }
        ActivityManager.AppTask own() {
            ActivityManager.RecentTaskInfo info=new ActivityManager.RecentTaskInfo();info.taskId=11;
            info.baseActivity=new ComponentName("com.instagram.android","Main");info.baseIntent=new Intent().setComponent(info.baseActivity);
            ActivityManager.AppTask t=new ActivityManager.AppTask(info);t.activities.add(activity);
            (modern?fresh.tasks:old.tasks).tasks.add(t);return t;
        }
        void erase(Object node)throws Exception {
            Object manager=get(node,"mFragmentManager");
            if(modern)InstagramCurrent452Fixture.added(manager).remove(node);else InstagramCurrent121Fixture.added(manager).remove(node);
            View view=(View)get(node,"view");if(view!=null&&view.getParent() instanceof ViewGroup)((ViewGroup)view.getParent()).removeView(view);
            field(node,"added",false);field(node,"visible",false);field(node,"resumed",false);
        }
        public void close(){if(modern)fresh.close();else old.close();}
    }
    static Object get(Object obj,String name)throws Exception{return obj.getClass().getField(name).get(obj);}
    static void field(Object obj,String name,Object value)throws Exception {Field field=obj.getClass().getField(name);field.set(obj,value);}
    static void preserved(Scene s,ActivityManager.AppTask t){
        check(!s.activity.isFinishing()&&s.activity.finishCalls==0&&s.activity.removeCalls==0,"navigation retains the Activity");
        check(!t.excluded&&t.removeCalls==0,"navigation retains the task card");
    }
    static void removed(Scene s,ActivityManager.AppTask t){check(t.excluded&&t.removeCalls==1,"direct root Reels excludes and removes its task once");}
    static void entry(Scene s,Object initial,Object video,String entry)throws Exception {
        ActivityManager.AppTask t=s.own();boolean consumed;
        if(entry.equals("reels"))consumed=OneBackExit.tryInstagramReelsBack(video);
        else if(entry.equals("tab"))consumed=OneBackExit.tryInstagramTabBack(s.activity,s.controller,initial);
        else if(entry.equals("screen"))consumed=ExitScreens.shouldExit(s.activity);
        else if(entry.equals("main")) {OneBackExit.enterBack(s.activity);try{consumed=OneBackExit.tryInstagramMainReelsBack(s.activity);}finally{OneBackExit.leaveNative();}}
        else {Object d=new Object();OneBackExit.bindInstagramDispatcher(s.activity,d);consumed=OneBackExit.tryInstagramDispatcherBack(d);}
        check(!consumed,"nested profile/video is not consumed by "+entry+" exit");preserved(s,t);
    }
    static Map<String,Test> scenarios(){Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final boolean modern:new boolean[]{false,true}) {
            final String version=modern?"452":"447";
            for(final boolean missing:new boolean[]{false,true})for(final String entry:new String[]{"reels","tab","screen","main","dispatcher"})
                tests.put(version+"_profile_overlay_"+(missing?"unknown_source_":"clips_source_")+entry,()->{try(Scene s=new Scene(modern)){
                    Object w=s.wrapper();Object background=s.video(w,s.base);field(background,"resumed",false);
                    Object profile=s.node("X.NativeProfile",w,s.main);field(profile,"resumed",false);
                    Object video=s.video(w,s.main);if(missing){field(video,"config",null);field(video,"module","UNKNOWN");}
                    field(s.child(w),"count",2);
                    check(!InstagramProfileBack.fromProfile(video),"reproduction does not rely on a profile source token");entry(s,w,video,entry);
                }});
            tests.put(version+"_main_overlay_without_recorded_stack",()->{try(Scene s=new Scene(modern)){
                Object w=s.wrapper();Object base=s.video(w,s.base);field(base,"resumed",false);Object video=s.video(w,s.main);
                check(((Integer)get(s.child(w),"count")).intValue()==0,"overlay exists before/without manager back-stack bookkeeping");entry(s,w,video,"reels");
            }});
            for(final String stack:new String[]{"activity","wrapper","video"})tests.put(version+"_selected_"+stack+"_stack_preserved",()->{try(Scene s=new Scene(modern)){
                Object w=s.wrapper();Object video=s.video(w,s.base);Object manager=stack.equals("activity")?s.manager:stack.equals("wrapper")?s.child(w):s.child(video);
                field(manager,"count",1);entry(s,w,video,"reels");
            }});
            tests.put(version+"_nonreels_receiver_under_clips_tab",()->{try(Scene s=new Scene(modern)){
                Object w=s.wrapper();Object base=s.video(w,s.base);field(base,"resumed",false);Object profile=s.node("X.NativeProfile",w,s.main);
                entry(s,w,profile,"tab");
            }});
            tests.put(version+"_profile_video_from_nonclips_tab",()->{try(Scene s=new Scene(modern)){
                Object video=s.rootVideo();s.select(video,true);entry(s,video,video,"reels");
            }});
            tests.put(version+"_root_receiver_identity_must_match",()->{try(Scene s=new Scene(modern)){
                Object retained=s.video(null,s.main);field(retained,"resumed",false);Object current=s.rootVideo();
                check(retained!=current,"different fragment identities");entry(s,current,retained,"reels");
            }});
            for(final String kind:new String[]{"direct","base","ordinary","merged","pager"})tests.put(version+"_direct_root_"+kind+"_still_exits",()->{try(Scene s=new Scene(modern)){
                Object video;
                if(kind.equals("direct"))video=s.rootVideo();
                else {Object w=s.wrapper();video=kind.equals("base")?s.video(w,s.base):s.nested(kind,w,s.base);}
                ActivityManager.AppTask t=s.own();check(OneBackExit.tryInstagramReelsBack(video),"direct root "+kind+" still exits");removed(s,t);
            }});
            tests.put(version+"_inactive_other_tab_stack_does_not_block_root",()->{try(Scene s=new Scene(modern)){
                Object retained=s.node("X.NativeProfile",null,s.main);field(retained,"resumed",false);field(s.child(retained),"count",4);
                Object video=s.rootVideo();ActivityManager.AppTask t=s.own();check(OneBackExit.tryInstagramReelsBack(video),"unselected retained child stacks are irrelevant");removed(s,t);
            }});
            tests.put(version+"_native_pop_returns_same_profile_instance",()->{try(Scene s=new Scene(modern)){
                Object w=s.wrapper();Object base=s.video(w,s.base);field(base,"resumed",false);
                Object profile=s.node("X.NativeProfile",w,s.main);field(profile,"resumed",false);
                Object video=s.video(w,s.main);field(s.child(w),"count",2);ActivityManager.AppTask t=s.own();
                check(!OneBackExit.tryInstagramReelsBack(video),"patched handler permits native video pop");
                // Model the existing app-owned native pop; never invent a new profile.
                s.erase(video);field(profile,"resumed",true);field(s.child(w),"count",1);
                Object selected=modern?Instagram452.content(s.activity,Instagram452.root(s.activity)):InstagramReelsExit.current(s.activity).target;
                check(selected==profile,"native pop reveals the exact original profile");
                check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,w),"retained CLIPS tab does not consume the profile return");preserved(s,t);
            }});
        }
        for(final String entry:new String[]{"screen","shared","keys","dispatcher","native","prediction"})
            tests.put("452_modal_profile_video_"+entry,()->{try(InstagramCurrent452Fixture.Scene s=new InstagramCurrent452Fixture.Scene("modal")){
                InstagramCurrent452Fixture.Node video=s.node("X.02Hr",null,InstagramCurrent452Fixture.MAIN);
                video.config=new ClipsViewerConfig(ClipsViewerSource.PROFILE);video.module="clips_viewer_clips_profile";
                s.group((ViewGroup)video.view,"clips_viewer_container",new Rect(0,0,1080,2170));
                ActivityManager.AppTask t=Back127Instagram452Test.own(s);
                check(!Instagram452.supports(s.activity),"modal has no MainActivity tab contract");
                check(s.activity.getClass().getMethod("A1Z").getReturnType().getName().equals("X.00fk"),"modal uses actual452 shared FragmentActivity manager");
                Object dispatcher=new Object();OneBackExit.bindInstagramDispatcher(s.activity,dispatcher);boolean consumed;
                if(entry.equals("screen"))consumed=ExitScreens.shouldExit(s.activity);
                else if(entry.equals("shared"))consumed=OneBackExit.tryBack(s.activity);
                else if(entry.equals("keys"))consumed=OneBackExit.key(s.activity,down())||OneBackExit.key(s.activity,up());
                else if(entry.equals("dispatcher"))consumed=OneBackExit.tryInstagramDispatcherBack(dispatcher);
                else if(entry.equals("native"))consumed=OneBackExit.tryInstagramReelsBack(video);
                else {consumed=OneBackExit.startInstagramDispatcherGesture(dispatcher);OneBackExit.cancelInstagramDispatcherGesture(dispatcher);}
                check(!consumed,"separate ModalActivity video keeps native "+entry+" Back");
                Back127Instagram452Test.preserved(s,t);
            }});
        tests.put("452_modal_native_finish_preserves_task",()->{try(InstagramCurrent452Fixture.Scene s=new InstagramCurrent452Fixture.Scene("modal")){
            InstagramCurrent452Fixture.Node video=s.node("X.02Hr",null,InstagramCurrent452Fixture.MAIN);
            video.config=new ClipsViewerConfig(ClipsViewerSource.PROFILE);video.module="clips_viewer_clips_profile";
            s.group((ViewGroup)video.view,"clips_viewer_container",new Rect(0,0,1080,2170));ActivityManager.AppTask t=Back127Instagram452Test.own(s);
            OneBackExit.enterBack(s.activity);
            try {check(!OneBackExit.tryInstagramReelsBack(video),"native video handler remains in charge");
                check(!OneBackExit.tryTerminalBack(s.activity,1,false),"normal nonroot modal finish is not changed into task exit");
                s.activity.finish();
            }finally{OneBackExit.leaveNative();}
            check(s.activity.finishCalls==1&&s.activity.removeCalls==0,"only the modal Activity finishes");
            check(!t.excluded&&t.removeCalls==0,"profile's task stays present");
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception {
        int oldCases=cases,oldAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> entry:scenarios().entrySet()) {
            try {test(entry.getKey(),entry.getValue());}
            catch(AssertionError failure){failed++;System.out.println("FAIL "+entry.getKey()+": "+failure.getMessage());Handler.drain();}
        }
        if(failed!=0)throw new AssertionError("Back128InstagramProfileTest: "+failed+" failing scenarios");
        System.out.println("PASS Back128InstagramProfileTest: "+(cases-oldCases)+" cases, "+(assertions-oldAssertions)+" assertions");
    }
}
