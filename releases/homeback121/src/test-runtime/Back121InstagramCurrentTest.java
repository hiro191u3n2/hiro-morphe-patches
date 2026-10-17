package app.hiro.oneback.runtime;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent121Fixture.*;

/** Exercises native routing authority and the exact boundaries of background
 * exclusion. These are counterexample fixtures, not claims about a device tree.
 */
public final class Back121InstagramCurrentTest {
    private static InstagramCurrent.Result current(Scene s,int kind) {
        InstagramCurrent.Result r=InstagramCurrent.inspect(s.activity);
        check(r.available&&r.nativeRecognized&&r.kind==kind,"verified current route kind "+kind);
        return r;
    }
    public static void main(String[] args)throws Exception {
        int before=cases,checks=assertions;
        test("current_native_home_does_not_require_a_selected_tab_view",()->{try(Scene s=new Scene()){
            Node home=s.node("X.3mI",null,MAIN);s.select(home,true);
            check(!HomeScreen.isHome(s.activity),"generic UI-only Home has no selected navigation proof");
            InstagramCurrent.Result r=current(s,InstagramCurrent.HOME);
            check(r.target==home&&r.view==home.view&&r.stateClear&&r.stackClear,"native FEED and exact Home own the displayed content");
        }});
        test("current_home_class_on_another_tab_is_other",()->{try(Scene s=new Scene()){
            Node home=s.node("X.3mI",null,MAIN);s.select(home,false);
            current(s,InstagramCurrent.OTHER);
        }});
        test("current_native_reels_subclass_keeps_base_identity",()->{try(Scene s=new Scene()){
            Node reels=s.node("X.Ze9",null,MAIN);s.select(reels,false);
            check(current(s,InstagramCurrent.REELS).target==reels,"native factory subclass retains verified Reels base identity");
        }});
        test("current_swipeable_overlay_precedes_home_base",()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.0jG",null,MAIN),home=s.node("X.3mI",wrapper,BASE);
            Node profile=s.node("X.NativeProfile",wrapper,MAIN);home.resumed=false;s.select(wrapper,true);
            InstagramCurrent.Result r=current(s,InstagramCurrent.OTHER);
            check(r.target==profile&&r.backgroundRoots.contains(home.view),"native MAIN overlay wins and inactive BASE is excluded");
            check(!r.backgroundRoots.contains(profile.view)&&r.path.size()==2,"current overlay remains in scope");
        }});
        test("current_reels_overlay_excludes_large_inactive_base",()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.0jG",null,MAIN),home=s.node("X.3mI",wrapper,BASE);home.resumed=false;
            ViewGroup nested=s.group((ViewGroup)home.view,"retained_home_tree",new Rect(0,0,1080,2200));
            for(int i=0;i<2001;i++)nested.addView(new View(s.activity));
            Node reels=s.node("X.8Zv",wrapper,MAIN);s.select(wrapper,true);
            InstagramCurrent.Result r=current(s,InstagramCurrent.REELS);
            check(r.target==reels&&r.backgroundRoots.contains(home.view),"unrelated retained count does not change native current identity");
            check(!r.backgroundRoots.contains(wrapper.view)&&!r.backgroundRoots.contains(reels.view),"selected ancestors and viewer are never removed");
        }});
        test("current_inactive_native_tab_excluded_without_sibling_geometry_guess",()->{try(Scene s=new Scene()){
            Node old=s.node("X.0jG",null,MAIN);s.cacheTab(old,"clips");old.resumed=false;
            Node prior=s.node("X.NativeProfile",old,BASE);prior.resumed=false;
            ViewGroup nested=s.group((ViewGroup)prior.view,"retained_profile",new Rect(0,0,1080,2200));
            for(int i=0;i<2001;i++)nested.addView(new View(s.activity));
            Node selected=s.node("X.0jG",null,MAIN),home=s.node("X.3mI",selected,BASE);s.select(selected,true);
            InstagramCurrent.Result r=current(s,InstagramCurrent.HOME);
            check(r.target==home&&r.backgroundRoots.contains(old.view),"exact different tab cache and inactive lifecycle prune whole retained tab branch");
        }});
        test("current_same_container_previous_fragment_and_live_overlay_boundaries",()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.3gY",null,MAIN),prior=s.node("X.NativeProfile",wrapper,MAIN);prior.resumed=false;
            Node reels=s.node("X.8Zv",wrapper,MAIN);s.select(wrapper,false);
            Node unrelated=s.node("X.NativeProfile",wrapper,0x1234);unrelated.resumed=false;
            Node activeSibling=s.node("X.NativeProfile",wrapper,0x4321);
            InstagramCurrent.Result r=current(s,InstagramCurrent.REELS);
            check(r.backgroundRoots.contains(prior.view),"native find-by-id proves inactive prior content in same container");
            check(!r.backgroundRoots.contains(unrelated.view)&&!r.backgroundRoots.contains(activeSibling.view),"different container and active overlays retain scope");
        }});
        test("current_resumed_or_unknown_native_tabs_are_not_pruned",()->{try(Scene s=new Scene()){
            Node active=s.node("X.0jG",null,MAIN);s.cacheTab(active,"clips");
            Node unknown=s.node("X.0jG",null,MAIN);unknown.resumed=false;s.cacheTab(unknown,null);
            Node unmapped=s.node("X.0jG",null,MAIN);unmapped.resumed=false;s.cacheTab(unmapped,"arbitrary_overlay_key");
            Node selected=s.node("X.0jG",null,MAIN);s.node("X.3mI",selected,BASE);s.select(selected,true);
            InstagramCurrent.Result r=current(s,InstagramCurrent.HOME);
            check(!r.backgroundRoots.contains(active.view)&&!r.backgroundRoots.contains(unknown.view)&&!r.backgroundRoots.contains(unmapped.view),"only cached distinct inactive enumerated native tabs are excluded");
        }});
        test("current_merged_feed_uses_only_cached_selected_tag",()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.0jG",null,MAIN),merged=s.node("X.22c",wrapper,BASE);s.merged(merged,0);
            Node home=s.node("X.3mI",merged,0x2674);s.tag(home,"merged_feed_tab_0");
            Node prior=s.node("X.NativeProfile",merged,0x2674);s.tag(prior,"merged_feed_tab_1");prior.resumed=false;
            Node unrelated=s.node("X.NativeProfile",merged,0x2674);s.tag(unrelated,"other_panel");unrelated.resumed=false;
            s.select(wrapper,true);InstagramCurrent.Result r=current(s,InstagramCurrent.HOME);
            check(r.target==home&&r.backgroundRoots.contains(prior.view),"cached selected tag wins despite added order");
            check(!r.backgroundRoots.contains(unrelated.view),"merged tag proof cannot exclude arbitrary same-container panels");
        }});
        test("current_merged_feed_uncached_selection_is_unknown",()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.0jG",null,MAIN),merged=s.node("X.22c",wrapper,BASE);s.merged(merged,null);s.select(wrapper,true);
            Node home=s.node("X.3mI",merged,0x2674);s.tag(home,"merged_feed_tab_0");
            InstagramCurrent.Result r=InstagramCurrent.inspect(s.activity);
            check(r.available&&!r.nativeRecognized&&r.kind==InstagramCurrent.UNKNOWN,"uncached native selection never invokes lazy fallback or guesses an added child");
        }});
        for(final int index:new int[]{0,1})test("current_clips_pager_positive_reels_index_"+index,()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.0jG",null,MAIN),clips=s.node("X.9sY",wrapper,BASE);ViewGroup pager=s.pager(clips,index);
            Node reels=s.node("X.Ze9",clips,0x1936);s.tag(reels,"f"+index);((ViewGroup)clips.view).removeView(reels.view);pager.addView(reels.view);
            Node prior=s.node("X.NativeProfile",clips,0x1936);s.tag(prior,"f"+(1-index));prior.resumed=false;
            Node outside=s.node("X.NativeProfile",clips,0x1936);s.tag(outside,"f9999");outside.resumed=false;
            s.select(wrapper,true);InstagramCurrent.Result r=current(s,InstagramCurrent.REELS);
            check(r.target==reels&&r.backgroundRoots.contains(prior.view),"selected native pager tag wins at any Reels index");
            check(!r.backgroundRoots.contains(outside.view),"a tag outside the actual cached adapter range is retained");
        }});
        test("current_clips_pager_nonzero_other_content_stays_other",()->{try(Scene s=new Scene()){
            Node clips=s.node("X.9sY",null,MAIN);s.pager(clips,1);
            Node reels=s.node("X.8Zv",clips,0x1936);s.tag(reels,"f0");reels.resumed=false;
            Node profile=s.node("X.NativeProfile",clips,0x1936);s.tag(profile,"f1");s.select(clips,false);
            check(current(s,InstagramCurrent.OTHER).target==profile,"a different selected pager page cannot borrow retained Reels identity");
        }});
        test("current_clips_pager_missing_or_stale_selection_is_unknown",()->{try(Scene s=new Scene()){
            Node clips=s.node("X.9sY",null,MAIN);s.select(clips,false);
            Node reels=s.node("X.8Zv",clips,0x1936);s.tag(reels,"f0");
            check(!InstagramCurrent.inspect(s.activity).nativeRecognized,"null pager cannot guess a resumed child");
            s.pager(clips,1);check(!InstagramCurrent.inspect(s.activity).nativeRecognized,"missing selected f1 never falls through to f0");
            s.pager(clips,0);reels.resumed=false;check(!InstagramCurrent.inspect(s.activity).nativeRecognized,"stale selected pager fragment cannot exit");
            reels.resumed=true;s.pager(clips,2,2);s.tag(reels,"f2");check(!InstagramCurrent.inspect(s.activity).nativeRecognized,"out-of-range pager index is unknown even with a matching child tag");
            ViewGroup pager=s.pager(clips,0);s.tag(reels,"f0");set(pager,"adapter",s.create("X.A9v"));
            check(!InstagramCurrent.inspect(s.activity).nativeRecognized,"replaced pager adapter cannot borrow stale cached page count");
        }});
        test("current_null_throwing_and_paused_targets_are_unknown",()->{try(Scene s=new Scene()){
            check(InstagramCurrent.inspect(s.activity).available&&!InstagramCurrent.inspect(s.activity).nativeRecognized,"null current in supported native family is authoritative unknown");
            Node reels=s.node("X.8Zv",null,MAIN);s.select(reels,false);reels.resumed=false;
            check(!InstagramCurrent.inspect(s.activity).nativeRecognized,"stale retained Reels cannot be current");
            reels.resumed=true;s.activity.failCurrent=true;
            check(!InstagramCurrent.inspect(s.activity).nativeRecognized,"throwing native getter cannot fall through to stale views");
        }});
        test("current_foreign_owner_and_detached_view_are_unknown",()->{try(Scene s=new Scene();Scene other=new Scene()){
            Node reels=s.node("X.8Zv",null,MAIN);s.select(reels,false);reels.activity=other.activity;
            check(!InstagramCurrent.inspect(s.activity).nativeRecognized,"foreign fragment owner cannot authorize this task");
            reels.activity=s.activity;s.root.removeView(reels.view);
            check(!InstagramCurrent.inspect(s.activity).nativeRecognized,"a detached stale view is not displayed in this Activity");
        }});
        test("current_stack_and_transaction_state_are_explicit",()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.0jG",null,MAIN),home=s.node("X.3mI",wrapper,BASE);s.select(wrapper,true);
            ((ManagerState)s.child(wrapper)).count=1;
            InstagramCurrent.Result r=current(s,InstagramCurrent.HOME);check(!r.stackClear&&r.stateClear,"Home stack cannot be mistaken for root Home");
            ((ManagerState)s.child(wrapper)).count=0;((ManagerState)s.child(home)).saved=true;
            check(!current(s,InstagramCurrent.HOME).stateClear,"saved child manager state is exposed");
            ((ManagerState)s.child(home)).saved=false;set(s.manager,"mExecutingActions",true);
            check(!current(s,InstagramCurrent.HOME).stateClear,"executing root transaction is exposed");
            set(s.manager,"mExecutingActions",false);set(s.manager,"A0W",new ArrayList<Object>(){{add(new Object());}});
            check(!current(s,InstagramCurrent.HOME).stateClear,"queued native transaction is exposed");
        }});
        for(final String fault:new String[]{"missing_controller","controller_type","current_nonfinal","find_type","pending_type"})
            test("current_native_family_shape_mismatch_"+fault,()->{try(Scene s=new Scene(fault)){
                Node home=s.node("X.3mI",null,MAIN);s.select(home,true);
                InstagramCurrent.Result r=InstagramCurrent.inspect(s.activity);
                check(r.available&&!r.nativeRecognized&&r.kind==InstagramCurrent.UNKNOWN,"known native family shape mismatch fails closed");
            }});
        test("current_legacy_host_keeps_existing_fallback",()->{try(F f=new F(HomeBackHostTest.IG)){
            check(!InstagramCurrent.inspect(f.activity).available,"generic host without native hierarchy retains prior supported path");
        }});
        System.out.println("PASS Back121InstagramCurrentTest: "+(cases-before)+" cases, "+(assertions-checks)+" assertions");
    }
}
