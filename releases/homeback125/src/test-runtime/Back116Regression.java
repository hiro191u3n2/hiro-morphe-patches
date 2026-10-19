package app.hiro.oneback.runtime;

import android.app.*;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.window.*;
import java.util.*;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** New bug-shaped regressions. Also runnable against the unchanged v1.1.5 runtime. */
public final class Back116Regression {
    public static class Route {
        final String module;public FragmentManager child=new FragmentManager();public boolean visible=true;
        Route(String m){module=m;}public boolean isVisible(){return visible;}
        public String getModuleName(){return module;}public Object getChildFragmentManager(){return child;}
    }
    public static class ProfileFragment extends Route {ProfileFragment(){super("profile_view");}}
    public static class ClipsBottomSheet extends Route {ClipsBottomSheet(){super("clips_viewer");}}
    static ViewGroup reelsWrapper(F f){
        f.home.selected=false;
        ViewGroup wrapper=new ViewGroup(f.activity);wrapper.bounds=new Rect(0,0,1080,2200);
        wrapper.id=++markerId;android.content.res.Resources.IDS.put(wrapper.id,"bottom_sheet");
        View viewer=new View(f.activity);viewer.id=++markerId;android.content.res.Resources.IDS.put(viewer.id,"clips_viewer_container");
        viewer.bounds=new Rect(0,20,1080,2170);wrapper.addView(viewer);f.root.addView(wrapper);return wrapper;
    }
    static Map<String,Test> scenarios(){
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        tests.put("ig116_reels_sheet_is_viewer_not_comments",()->{try(F f=new F(IG)){
            reelsWrapper(f);ActivityManager.AppTask task=ownTask(f,11);
            check(!InstagramBack.inspect(f.activity).blocked,"Reels host is not an open layer");
            check(OneBackExit.tryBack(f.activity),"Reels Back consumed");
            check(task.excluded&&task.removeCalls==1,"Reels Back excludes and removes task");
            check(f.activity.nativeBackCalls==0,"not native pop to previous tab");
        }});
        tests.put("ig116_reels_over_profile_underlay",()->{try(F f=new F(IG,true,false)){
            f.activity.fragments.fragments.add(new ProfileFragment());f.activity.fragments.fragments.add(new Route("clips_viewer"));f.activity.fragments.count=2;
            check(ExitScreens.shouldExit(f.activity),"foreground Reels overrides underlying profile/stack");
            OneBackExit.tryBack(f.activity);check(f.activity.removeCalls==1,"Reels exits");
        }});
        tests.put("ig116_profile_over_reels_not_exit",()->{try(F f=new F(IG,true,false)){
            marker(f,"clips_viewer_container",new Rect(0,0,1080,2150));
            f.activity.fragments.fragments.add(new Route("clips_viewer"));f.activity.fragments.fragments.add(new ProfileFragment());
            check(!ExitScreens.shouldExit(f.activity),"foreground profile still delegates");preserved(f);
        }});
        tests.put("ig116_reels_inside_profile_child",()->{try(F f=new F(IG,true,false)){
            ProfileFragment parent=new ProfileFragment();parent.child.fragments.add(new Route("clips_viewer"));
            f.activity.fragments.fragments.add(parent);
            check(OneBackExit.tryBack(f.activity),"nested Reels handled");check(f.activity.removeCalls==1,"nested Reels exits");
        }});
        tests.put("ig116_reels_route_host_bottomsheet",()->{try(F f=new F(IG,true,false)){
            f.activity.fragments.fragments.add(new ClipsBottomSheet());
            check(OneBackExit.tryBack(f.activity),"Reels fragment host handled");check(f.activity.removeCalls==1,"Reels route host exits");
        }});
        for(final String route:new String[]{"clips_viewer_tray","clips_video_viewer","reels_viewer"})
            tests.put("ig116_route_"+route,()->{try(F f=new F(IG,true,false)){
                f.activity.fragments.fragments.add(new Route(route));
                check(OneBackExit.tryBack(f.activity),"explicit Reels module handled");check(f.activity.removeCalls==1,"Reels module exit");
            }});
        tests.put("ig116_comments_inside_reels_sheet_close_only",()->{try(F f=new F(IG)){
            ViewGroup wrapper=reelsWrapper(f);View panel=igPanel(f,"comments_container");f.root.removeView(panel);wrapper.addView(panel);
            f.activity.nativeBackAction=()->wrapper.removeView(panel);ActivityManager.AppTask task=ownTask(f,11);
            check(OneBackExit.tryBack(f.activity),"comment close handled");
            check(f.activity.nativeBackCalls==1&&!InstagramBack.inspect(f.activity).blocked,"only comment collapses");
            check(!task.excluded&&task.removeCalls==0,"comment close preserves Recents");preserved(f);
            check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"next Back exits Reels");
            check(task.excluded&&task.removeCalls==1,"next Back removes Recents");
        }});
        tests.put("ig116_share_inside_reels_sheet_is_layer",()->{try(F f=new F(IG)){
            ViewGroup wrapper=reelsWrapper(f);View share=marker(f,"share_sheet",new Rect(0,800,1080,2200));
            f.root.removeView(share);wrapper.addView(share);f.activity.nativeBackAction=()->wrapper.removeView(share);
            check(!ExitScreens.shouldExit(f.activity),"share panel vetoes Reels exit");
            OneBackExit.tryBack(f.activity);preserved(f);check(f.activity.nativeBackCalls==1,"native share close");
        }});
        tests.put("ig116_story_in_sheet_never_reels",()->{try(F f=new F(IG,true,false)){
            ViewGroup wrapper=reelsWrapper(f);View viewer=wrapper.getChildAt(0);android.content.res.Resources.IDS.put(viewer.id,"reel_viewer");
            check(!ExitScreens.shouldExit(f.activity),"singular reel_viewer remains Stories");preserved(f);
        }});
        tests.put("ig116_reels_native_animation_cannot_pop_first",()->{try(F f=new F(IG)){
            f.home.description="リール";final int[] calls={0,0,0,0};
            OnBackAnimationCallback original=new OnBackAnimationCallback(){
                public void onBackStarted(BackEvent e){calls[0]++;f.home.description="プロフィール";f.home.selected=false;}
                public void onBackProgressed(BackEvent e){calls[1]++;}public void onBackCancelled(){calls[2]++;}
                public void onBackInvoked(){calls[3]++;}
            };
            NativeBack.register(f.dispatcher(),0,original);OnBackAnimationCallback wrapped=(OnBackAnimationCallback)f.dispatcher().top();
            wrapped.onBackStarted(new BackEvent());wrapped.onBackProgressed(new BackEvent());wrapped.onBackInvoked();
            check(Arrays.equals(calls,new int[]{0,0,0,0}),"native pop animation withheld for explicit exit");
            check(f.activity.removeCalls==1,"gesture begun on Reels exits");NativeBack.unregister(f.dispatcher(),original);
        }});
        tests.put("ig116_reels_cancel_does_not_move_tab",()->{try(F f=new F(IG)){
            f.home.description="リール";
            OnBackAnimationCallback original=new OnBackAnimationCallback(){public void onBackStarted(BackEvent e){f.home.selected=false;}
                public void onBackProgressed(BackEvent e){}public void onBackCancelled(){}public void onBackInvoked(){}};
            NativeBack.register(f.dispatcher(),0,original);OnBackAnimationCallback wrapped=(OnBackAnimationCallback)f.dispatcher().top();
            wrapped.onBackStarted(new BackEvent());wrapped.onBackCancelled();check(f.home.selected,"cancel leaves Reels in place");preserved(f);
            NativeBack.unregister(f.dispatcher(),original);
        }});
        for(final String pkg:new String[]{TT,JP}){
            tests.put("tt116_profile_toolbar_and_home_"+pkg,()->{try(F f=new F(pkg)){
                f.home.description="プロフィール";View target=tiktokTarget(f);View profile=marker(f,"profile_root",new Rect(0,120,1080,1900));
                View arrow=marker(f,"back_button",new Rect(0,20,100,120));arrow.clickable=true;
                target.clickAction=()->{f.root.removeView(profile);f.root.removeView(arrow);f.home.selected=false;target.selected=true;};
                check(OneBackExit.tryBack(f.activity),"profile Back consumed");Handler.drain();
                check(target.clickCalls==1&&TikTokScreen.inspect(f.activity).home,"profile Back reaches Home");preserved(f);
                check(OneBackExit.tryBack(f.activity),"duplicate cannot exit");preserved(f);
                check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"separate next key exits");
                check(f.activity.removeCalls==1,"next Home Back removes task");
            }});
            tests.put("tt116_multilevel_profile_native_"+pkg,()->{try(F f=new F(pkg)){
                f.home.hidden=true;final View[] page={marker(f,"profile_root",new Rect(0,120,1080,1900))};final int[] calls={0};
                f.activity.nativeBackAction=()->{f.root.removeView(page[0]);if(++calls[0]==1)page[0]=marker(f,"profile_root",new Rect(0,120,1080,1900));else f.home.hidden=false;};
                OneBackExit.tryBack(f.activity);Handler.drain();check(calls[0]==2,"one native Back per changed profile page");
                check(TikTokScreen.inspect(f.activity).home,"profile stack reaches Home");preserved(f);
            }});
            tests.put("tt116_deep_home_tab_parent_"+pkg,()->{try(F f=new F(pkg)){
                f.home.description="プロフィール";ViewGroup outer=new ViewGroup(f.activity);outer.bounds=new Rect(0,1970,205,2200);outer.clickable=true;
                ViewGroup parent=outer;for(int i=0;i<5;i++){ViewGroup nested=new ViewGroup(f.activity);nested.bounds=new Rect(0,1980,200,2190);parent.addView(nested);parent=nested;}
                View icon=new View(f.activity);icon.bounds=new Rect(20,1990,180,2170);icon.description="ホーム";parent.addView(icon);f.root.addView(outer);
                outer.clickAction=()->{f.home.selected=false;outer.selected=true;};
                OneBackExit.tryBack(f.activity);Handler.drain();check(outer.clickCalls==1,"semantic icon resolves actual parent listener");
                check(TikTokScreen.inspect(f.activity).home,"deep parent selection is Home");preserved(f);
            }});
            tests.put("tt116_home_label_button_role_"+pkg,()->{try(F f=new F(pkg)){
                f.home.description="プロフィール";View target=tiktokTarget(f);target.description="ホーム ボタン";
                OneBackExit.tryBack(f.activity);Handler.drain();check(target.clickCalls==1&&TikTokScreen.inspect(f.activity).home,"role suffix Home target works");preserved(f);
            }});
            tests.put("tt116_profile_comments_close_before_tab_"+pkg,()->{try(F f=new F(pkg)){
                f.home.description="プロフィール";View target=tiktokTarget(f);View comment=marker(f,"comments_container",new Rect(0,600,1080,2200));
                f.activity.nativeBackAction=()->{check(target.clickCalls==0,"never click behind open comments");f.root.removeView(comment);};
                OneBackExit.tryBack(f.activity);Handler.drain();check(f.activity.nativeBackCalls==1&&target.clickCalls==1,"close layer then actual Home tab");preserved(f);
            }});
        }
        tests.put("ig116_stale_primary_reels_below_profile",()->{try(F f=new F(IG,true,false)){
            Route reels=new Route("clips_viewer");f.activity.fragments.primary=reels;
            f.activity.fragments.fragments.add(reels);f.activity.fragments.fragments.add(new ProfileFragment());
            check(!ExitScreens.shouldExit(f.activity),"old primary route below new profile is not foreground");preserved(f);
        }});
        tests.put("ig116_comment_animation_still_native",()->{try(F f=new F(IG)){
            f.home.description="リール";View panel=igPanel(f,"comments_container");final int[] calls={0,0,0};
            OnBackAnimationCallback original=new OnBackAnimationCallback(){public void onBackStarted(BackEvent e){calls[0]++;}
                public void onBackProgressed(BackEvent e){calls[1]++;}public void onBackCancelled(){}
                public void onBackInvoked(){calls[2]++;f.root.removeView(panel);}};
            NativeBack.register(f.dispatcher(),0,original);OnBackAnimationCallback wrapped=(OnBackAnimationCallback)f.dispatcher().top();
            wrapped.onBackStarted(new BackEvent());wrapped.onBackProgressed(new BackEvent());wrapped.onBackInvoked();
            check(Arrays.equals(calls,new int[]{1,1,1}),"comments keep the original collapse animation/callback");
            check(!InstagramBack.inspect(f.activity).blocked,"comments closed");preserved(f);NativeBack.unregister(f.dispatcher(),original);
        }});
        tests.put("ig116_small_reels_preview_not_full_viewer",()->{try(F f=new F(IG,true,false)){
            ViewGroup wrapper=reelsWrapper(f);wrapper.getChildAt(0).bounds=new Rect(0,200,900,700);
            check(!ExitScreens.shouldExit(f.activity),"embedded small preview cannot exempt sheet");preserved(f);
        }});
        tests.put("ig116_hidden_reels_not_foreground",()->{try(F f=new F(IG,true,false)){
            ViewGroup wrapper=reelsWrapper(f);wrapper.getChildAt(0).hidden=true;
            check(!ExitScreens.shouldExit(f.activity),"hidden viewer cannot exempt sheet");preserved(f);
        }});
        for(final int api:new int[]{29,33,36}){
            tests.put("ig116_reels_key_api"+api,()->{try(F f=new F(IG)){
                Build.VERSION.SDK_INT=api;reelsWrapper(f);ActivityManager.AppTask task=ownTask(f,11);
                check(OneBackExit.key(f.activity,down()),"physical down captures Reels exit");preserved(f);
                check(OneBackExit.key(f.activity,up()),"physical up commits Reels exit");
                check(task.excluded&&task.removeCalls==1,"physical Reels exit removes Recents");
            }});
        }
        return tests;
    }
    static void run()throws Exception {for(Map.Entry<String,Test> e:scenarios().entrySet())test(e.getKey(),e.getValue());}
    public static void main(String[] args)throws Exception {
        int failed=0;for(Map.Entry<String,Test> e:scenarios().entrySet())try{test(e.getKey(),e.getValue());}catch(AssertionError error){failed++;System.out.println("FAIL "+e.getKey()+": "+error.getMessage());}
        System.out.println("REGRESSION: "+scenarios().size()+" cases, "+failed+" failed");if(failed>0)System.exit(1);
    }
}
