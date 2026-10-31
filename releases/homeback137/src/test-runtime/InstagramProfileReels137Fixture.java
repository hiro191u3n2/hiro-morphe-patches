package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Handler;
import android.view.ViewGroup;
import com.instagram.clips.intf.ClipsViewerConfig;
import com.instagram.clips.intf.ClipsViewerSource;
import java.util.ArrayList;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;
import static app.hiro.oneback.runtime.Back130InstagramProfileLinkTest.*;

/** Explicit native actions for the revised policy. These host actions are
 * separate from production classification: a test chooses when native removal
 * and tab binding occur. They do not claim Android execution. */
final class InstagramProfileReels137Fixture {
    static final String MARKER="app.hiro.oneback.runtime.PROFILE_REELS_452";
    static final String ENTRANCE="INSTAGRAM_MAIN_ACTIVITY_INTENT_ENTRANCE_MODULE_EXTRA_FIELD";
    static final class Destination {
        final Scene scene;final boolean modern;Node wrapper,reel;
        boolean deferred;int bindings;
        Destination(Scene scene){this(scene,true);}
        Destination(Scene scene,boolean modern){
            this.scene=scene;this.modern=modern;
            scene.activity.selectClipsAction=()->{try{set(scene.controller,"current",scene.enumTab(false));if(deferred){set(scene.controller,"selected",null);scene.activity.current=null;}else bind();}catch(Exception e){throw new AssertionError(e);}};
            scene.activity.resetClipsAction=()->reset();
        }
        void bind(){try{
            if(wrapper==null){
                wrapper=scene.node(modern?"X.00VS":"X.02Wh",null,modern?0:MAIN);
                if(modern){scene.tag(wrapper,"f1");scene.cacheTab(wrapper,"clips");}
                reel=scene.node("X.02Hr",wrapper,modern?BASE:MAIN);
                reel.config=new ClipsViewerConfig(ClipsViewerSource.CLIPS_TAB);
                Back127Instagram452Test.footer(scene,reel);
            }
            bindings++;scene.select(wrapper,false);
        }catch(Exception failure){throw new AssertionError(failure);}}
        void reset(){try{
            check(wrapper!=null,"native reset is not called before wrapper binding");
            for(Object object:new ArrayList<Object>(added(scene.child(wrapper))))
                if(object!=reel)erase(scene,(Node)object);
            if(!added(scene.child(wrapper)).contains(reel))added(scene.child(wrapper)).add(reel);
            if(reel.view.getParent()==null)((ViewGroup)wrapper.view).addView(reel.view);
            reel.added=true;reel.removing=false;reel.visible=true;reel.resumed=true;reel.view.hidden=false;
            set(scene.child(wrapper),"count",0);scene.select(wrapper,false);
        }catch(Exception failure){throw new AssertionError(failure);}}
        void assertRoot(){
            check("CLIPS".equals(((Enum<?>)scene.activity.requestedTab).name()),"requested destination is native CLIPS");
            check(scene.activity.resetClipsCalls==1,"destination history reset exactly once");
            check(InstagramRootReels.matches(scene.activity,reel),"result is selected root Reels");
        }
    }
    static boolean isMain(Scene scene){return scene.activity.getClass().getName().equals("com.instagram.mainactivity.InstagramMainActivity");}
    static void remove(Scene scene,Node profile)throws Exception{
        Object manager=value(profile,"mFragmentManager");
        profile.removing=true;erase(scene,profile);set(manager,"count",0);
    }
    static void noTaskRemoval(ActivityManager.AppTask task){
        check(!task.excluded&&task.excludeCalls==0&&task.removeCalls==0,"profile return does not remove/exclude the task");
    }
    static void expectedIntent(Scene scene){
        check(scene.activity.startCalls==1,"one Main handoff request");Intent intent=scene.activity.lastStart;
        check(intent!=null&&intent.getComponent()!=null&&"com.instagram.android".equals(intent.getComponent().getPackageName())
            &&"com.instagram.mainactivity.InstagramMainActivity".equals(intent.getComponent().getClassName()),"explicit same-app Main destination");
        check("CLIPS".equals(intent.getStringExtra(ENTRANCE))&&intent.getBooleanExtra("is_cold_start_reel_tab",false),"native cold/warm Reels flags");
        check("CLIPS".equals(intent.getStringExtra("MainActivityAccountHelper.STARTUP_TAB")),"explicit CLIPS startup overrides neither native nor Piko default selection");
        check(intent.getBooleanExtra(MARKER,false)&&intent.getFlags()==0x14000000,"one-shot private handoff marker and native task flags");
    }
    static void nextReelsBack(Scene scene,Destination destination,ActivityManager.AppTask task){
        Handler.drain();destination.assertRoot();
        check(OneBackExit.tryInstagramReelsBack(destination.reel),"independent root Reels Back exits");
        Back127Instagram452Test.removed(scene,task);
    }
    static void dispatchToReels(Scene scene,Node profile,ActivityManager.AppTask task,Object dispatcher)throws Exception{
        Destination destination=new Destination(scene);
        int calls=profile.profileBackCalls;
        check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"AndroidX retains the original profile pop");
        check(profile.profileBackCalls==calls,"pre-dispatch does not synthesize a native profile callback");
        check(!profileBack(profile),"original bare profile handler returns native false");
        remove(scene,profile);
        Object parent=value(profile,"mParentFragment");
        if(parent instanceof Node&&"X.03CG".equals(parent.getClass().getName()))remove(scene,(Node)parent);
        OneBackExit.completeInstagramDispatcherBack(dispatcher);
        check(scene.activity.selectClipsCalls==0&&scene.activity.startCalls==0,"dispatcher completion only posts the observer");
        noTaskRemoval(task);Handler.drain();
        if(isMain(scene)){check(scene.activity.selectClipsCalls==1,"completed native pop selects Reels once");nextReelsBack(scene,destination,task);}
        else{expectedIntent(scene);noTaskRemoval(task);}
    }
    /** An accepted profile's native false result allows the original caller to
     * pop it; only normal completion may schedule navigation. */
    static void toReels(Scene scene,Node profile,ActivityManager.AppTask task)throws Exception{
        Destination destination=new Destination(scene);
        OneBackExit.enterBack(scene.activity);
        try{
            check(!back(profile),"bare profile returns native false instead of terminating");
            remove(scene,profile);
            Object parent=value(profile,"mParentFragment");
            if(parent instanceof Node&&"X.03CG".equals(parent.getClass().getName()))remove(scene,(Node)parent);
            OneBackExit.afterNative();
        }finally{OneBackExit.leaveNative();}
        check(scene.activity.selectClipsCalls==0&&scene.activity.startCalls==0,"navigation waits until after native body completion");
        noTaskRemoval(task);Handler.drain();
        if(isMain(scene)){
            check(scene.activity.selectClipsCalls==1,"removed external entry selects Reels once");
            noTaskRemoval(task);nextReelsBack(scene,destination,task);
        }else{
            expectedIntent(scene);noTaskRemoval(task);
            // A separate Activity represents native Main creation by the
            // verified Intent. Its task exit is checked independently.
            try(Scene target=new Scene()){
                Destination nativeTarget=new Destination(target);nativeTarget.bind();
                target.activity.setIntent(scene.activity.lastStart);
                InstagramProfileReels.installed(target.activity);Handler.drain();
                ActivityManager.AppTask targetTask=own(target);nextReelsBack(target,nativeTarget,targetTask);
            }
        }
    }
}
