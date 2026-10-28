package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import com.instagram.clips.intf.ClipsViewerConfig;
import com.instagram.clips.intf.ClipsViewerSource;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.Back121InstagramDispatchTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent121Fixture.*;

/** Profile origin and native continuation are explicit model inputs. The real
 * APK's enum/getter contracts and unchanged native pop code are audited apart;
 * this suite does not claim a device UI replay. */
public final class Back126InstagramProfileTest {
    static void source(Node video,ClipsViewerSource source){video.config=new ClipsViewerConfig(source);}
    static void allEntrancesPreserve(Scene s,Node initial,Node video)throws Exception {
        ActivityManager.AppTask task=own(s);
        check(!ExitScreens.shouldExit(s.activity),"profile video is not an exit screen");
        check(!OneBackExit.tryBack(s.activity),"ordinary shared Back delegates");
        check(!OneBackExit.key(s.activity,down())&&!OneBackExit.key(s.activity,up()),"both key events stay native");
        check(!OneBackExit.tryInstagramDispatcherBack(Back122InstagramDispatcherTest.bind(s)),"bound AndroidX Back delegates");
        check(!OneBackExit.tryInstagramReelsBack(video),"exact fragment hook delegates after native popup guards");
        check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,initial),"CLIPS bottom tab cannot override video origin");
        OneBackExit.enterBack(s.activity);
        try {check(!OneBackExit.tryInstagramMainReelsBack(s.activity),"Main native-dispatch hook delegates");}
        finally {OneBackExit.leaveNative();}
        preserved(s,task);
    }
    static Map<String,Test> scenarios(){Map<String,Test> result=new LinkedHashMap<String,Test>();
        for(final ClipsViewerSource origin:new ClipsViewerSource[]{ClipsViewerSource.PROFILE,ClipsViewerSource.SELF_PROFILE,
                ClipsViewerSource.GROUP_PROFILE,ClipsViewerSource.FEED_CONTEXTUAL_PROFILE})
            result.put("profile_enum_all_exit_entries_"+origin,()->{try(Scene s=new Scene()){
                Node video=current(s,false);source(video,origin);
                // The actual config A01 may override this analytics module.
                video.module="clips_viewer_clips_tab";
                check(InstagramProfileBack.fromProfile(video),"actual native enum outranks overridden analytics module");
                allEntrancesPreserve(s,video,video);
            }});
        for(final String module:new String[]{"clips_viewer_clips_profile","clips_viewer_self_clips_profile","clips_viewer_profile"})
            result.put("profile_module_fallback_"+module,()->{try(Scene s=new Scene()){
                Node video=current(s,false);video.module=module;
                check(video.config==null&&InstagramProfileBack.fromProfile(video),"exact semantic source is a fallback when config unavailable");
                allEntrancesPreserve(s,video,video);
            }});
        result.put("native_nonprofile_enum_overrides_profile_word_in_module",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.CLIPS_TAB);video.module="clips_viewer_profile";
            check(!InstagramProfileBack.fromProfile(video),"real non-profile source remains authoritative");
            ActivityManager.AppTask task=own(s);check(OneBackExit.tryInstagramReelsBack(video),"root Reels retains immediate exit");removed(s,task);
        }});
        result.put("ordinary_profile_or_caption_name_is_not_video_origin",()->{try(Scene s=new Scene()){
            Node video=current(s,false);
            for(String module:new String[]{"profile_view","profile_caption","clips_viewer_profiled","clips_viewer_clips_tab"}){
                video.module=module;check(!InstagramProfileBack.fromProfile(video),"strict viewer-prefix and exact profile token: "+module);
            }
            ActivityManager.AppTask task=own(s);check(OneBackExit.tryInstagramReelsBack(video),"unrelated profile text cannot disable root exit");removed(s,task);
        }});
        for(final String wrapperType:new String[]{"X.0jG","X.3gY","X.22c","X.9sY"})
            result.put("native_selected_wrapper_profile_"+wrapperType,()->{try(Scene s=new Scene()){
                Node wrapper=s.node(wrapperType,null,MAIN);s.select(wrapper,false);
                if(wrapperType.equals("X.0jG")){
                    s.cacheTab(wrapper,"clips");Node base=s.node("X.8Zv",wrapper,BASE);source(base,ClipsViewerSource.CLIPS_TAB);base.resumed=false;
                }
                if(wrapperType.equals("X.22c"))s.merged(wrapper,1);
                if(wrapperType.equals("X.9sY"))s.pager(wrapper,1);
                Node video=s.node("X.Ze9",wrapper,wrapperType.equals("X.22c")||wrapperType.equals("X.9sY")?1936:MAIN);
                if(wrapperType.equals("X.22c"))s.tag(video,"merged_feed_tab_1");
                if(wrapperType.equals("X.9sY"))s.tag(video,"f1");
                source(video,ClipsViewerSource.PROFILE);footer(s,video);
                check(InstagramProfileBack.current(s.activity),"read only native current-child chain reaches the profile-origin video");
                allEntrancesPreserve(s,wrapper,video);
            }});
        result.put("profile_return_uses_same_original_profile_under_clips_tab",()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.0jG",null,MAIN);s.cacheTab(wrapper,"clips");s.select(wrapper,false);
            Node base=s.node("X.8Zv",wrapper,BASE);source(base,ClipsViewerSource.CLIPS_TAB);base.resumed=false;
            final Node profile=s.node("X.NativeProfile",wrapper,MAIN);profile.resumed=false;
            final Node video=s.node("X.8Zv",wrapper,MAIN);source(video,ClipsViewerSource.PROFILE);footer(s,video);
            ((ManagerState)s.child(wrapper)).count=2;
            ActivityManager.AppTask task=own(s);s.install();final int[] nativeCalls={0};
            OnBackInvokedCallback nativeBack=()->{
                nativeCalls[0]++;check(!OneBackExit.tryInstagramReelsBack(video),"native handler retains its own continuation");
                try {erase(s,video);profile.resumed=true;((ManagerState)s.child(wrapper)).count=1;}
                catch(Exception failure){throw new AssertionError(failure);}
            };
            NativeBack.register(s.activity.getWindow().dispatcher(),0,nativeBack);
            try {
                s.activity.getWindow().dispatcher().invokeBack();
                check(nativeCalls[0]==1,"one native Back, without recursive forwarding");
                check(InstagramCurrent.inspect(s.activity).target==profile,"returns to the exact previous profile instance");
                check(((ManagerState)s.child(wrapper)).count==1,"single profile-video pop preserves remaining navigation");
                preserved(s,task);
            }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),nativeBack);}
        }});
        result.put("profile_origin_survives_native_pop_to_clips_until_scope_end",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.PROFILE);ActivityManager.AppTask task=own(s);
            OneBackExit.enterBack(s.activity);Node clips;
            try {
                erase(s,video);clips=current(s,false);source(clips,ClipsViewerSource.CLIPS_TAB);
                check(!OneBackExit.tryInstagramReelsBack(clips),"same native Back cannot exit a newly exposed CLIPS receiver");
                check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,clips),"same native Back cannot remove task through tab boundary");
                check(!OneBackExit.tryInstagramMainReelsBack(s.activity),"same native Back remains non-exit at Main boundary");preserved(s,task);
            }finally{OneBackExit.leaveNative();}
            check(OneBackExit.tryInstagramReelsBack(s.activity.current),"a subsequent independent root Reels Back still exits");removed(s,task);
        }});
        result.put("direct_profile_native_callback_protects_same_dispatch_without_outer_scope",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.PROFILE);ActivityManager.AppTask task=own(s);
            check(!OneBackExit.tryInstagramReelsBack(video),"profile native fragment handler retains native continuation without outer scope");
            erase(s,video);Node clips=current(s,false);source(clips,ClipsViewerSource.CLIPS_TAB);
            check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,clips),"same dispatch cannot become native tab exit");
            check(!OneBackExit.tryInstagramReelsBack(clips),"same dispatch cannot become a second native Reels exit");preserved(s,task);
            Handler.drain();
            check(OneBackExit.tryInstagramReelsBack(clips),"post-dispatch cleanup immediately permits independent root Back");removed(s,task);
        }});
        result.put("direct_profile_androidx_commit_protects_native_continuation",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.PROFILE);ActivityManager.AppTask task=own(s);
            Object dispatcher=Back122InstagramDispatcherTest.bind(s);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"nonpredictive AndroidX commit preserves native profile pop");
            erase(s,video);Node clips=current(s,false);source(clips,ClipsViewerSource.CLIPS_TAB);
            check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,clips),"same AndroidX continuation cannot become tab exit");
            check(!OneBackExit.tryInstagramReelsBack(clips),"same AndroidX continuation cannot become Reels exit");preserved(s,task);
            Handler.drain();check(OneBackExit.tryInstagramReelsBack(clips),"subsequent independent root Back exits");removed(s,task);
        }});
        result.put("profile_prediction_keeps_native_start_progress_and_cancel",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.PROFILE);ActivityManager.AppTask task=own(s);
            Object dispatcher=Back122InstagramDispatcherTest.bind(s);
            check(!OneBackExit.startInstagramDispatcherGesture(dispatcher),"profile video keeps native pop animation");
            check(!OneBackExit.progressInstagramDispatcherGesture(dispatcher),"native progress remains active");
            check(!OneBackExit.cancelInstagramDispatcherGesture(dispatcher),"native cancellation remains active");preserved(s,task);
            source(video,ClipsViewerSource.CLIPS_TAB);
            check(OneBackExit.tryInstagramReelsBack(video),"cancel clears profile-origin prediction before a later root Back");removed(s,task);
        }});
        result.put("profile_prediction_cannot_exit_newly_exposed_clips",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.PROFILE);ActivityManager.AppTask task=own(s);
            Object dispatcher=Back122InstagramDispatcherTest.bind(s);
            check(!OneBackExit.startInstagramDispatcherGesture(dispatcher),"native profile prediction begins");
            erase(s,video);Node clips=current(s,false);source(clips,ClipsViewerSource.CLIPS_TAB);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"commit cannot borrow the revealed exit screen");
            check(!OneBackExit.tryInstagramReelsBack(clips),"native receiver cannot bypass initial profile prediction");
            check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,clips),"native tab boundary respects initial profile prediction");preserved(s,task);
            Handler.drain();
            check(OneBackExit.tryInstagramReelsBack(clips),"after native event returns a new root Back may exit");removed(s,task);
        }});
        result.put("profile_video_comments_close_before_profile_return",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.PROFILE);
            final View panel=s.group((ViewGroup)video.view,"comments_sheet",new Rect(0,900,1080,2200));
            ActivityManager.AppTask task=own(s);Object dispatcher=Back122InstagramDispatcherTest.bind(s);
            s.activity.nativeBackAction=()->((ViewGroup)video.view).removeView(panel);
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher),"first Back closes the actual comments panel");
            check(s.activity.nativeBackCalls==1,"one native comments close");preserved(s,task);
            check(!OneBackExit.key(s.activity,down())&&!OneBackExit.key(s.activity,up()),"next Back delegates to profile return, not task exit");
            check(!OneBackExit.tryInstagramReelsBack(video),"native fragment continuation returns to profile after comments");preserved(s,task);
        }});
        result.put("native_profile_origin_does_not_depend_on_view_or_ime",()->{try(Scene s=new Scene()){
            Node video=current(s,false);source(video,ClipsViewerSource.PROFILE);video.view=null;video.visible=false;video.resumed=false;
            s.root.insets.ime=true;s.root.focus(false);ActivityManager.AppTask task=own(s);
            check(!OneBackExit.tryInstagramReelsBack(video),"authoritative source protects native fragment boundary without UI heuristics");
            check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,video),"tab boundary also reads actual source without UI heuristics");preserved(s,task);
        }});
        result.put("inactive_profile_video_does_not_block_current_root_reels",()->{try(Scene s=new Scene()){
            Node previous=current(s,false);source(previous,ClipsViewerSource.PROFILE);previous.resumed=false;
            Node clips=current(s,false);source(clips,ClipsViewerSource.CLIPS_TAB);ActivityManager.AppTask task=own(s);
            check(!InstagramProfileBack.current(s.activity),"selected current source excludes retained profile-origin video");
            check(OneBackExit.tryInstagramDispatcherBack(Back122InstagramDispatcherTest.bind(s)),"root Reels still exits through AndroidX");removed(s,task);
        }});
        return result;
    }
    public static void main(String[] args)throws Exception {
        int oldCases=cases,oldAssertions=assertions;
        for(Map.Entry<String,Test> item:scenarios().entrySet())test(item.getKey(),item.getValue());
        System.out.println("PASS Back126InstagramProfileTest: "+(cases-oldCases)+" cases, "+(assertions-oldAssertions)+" assertions");
    }
}
