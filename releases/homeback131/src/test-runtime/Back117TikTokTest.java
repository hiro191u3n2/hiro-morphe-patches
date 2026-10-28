package app.hiro.oneback.runtime;

import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import com.ss.android.ugc.aweme.main.MainActivity;
import com.ss.android.ugc.aweme.main.assems.tabs.TabAbilityAssem;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Reproduces the shipped app's ability contract with absent/ineffective Home labels. */
public final class Back117TikTokTest {
    static TabAbilityAssem profile(final F f,boolean labelled) {
        f.home.description=labelled?"ホーム":null;f.home.selected=false;
        final View profile=marker(f,"user_profile_container",new Rect(0,180,1080,1900));
        final TabAbilityAssem ability=new TabAbilityAssem();
        ((MainActivity)f.activity).communication_gen_field_com_bytedance_tiktok_homepage_mainfragment_HomeTabAbility=ability;
        ability.transition=()->f.root.removeView(profile);
        return ability;
    }
    public static void run()throws Exception {
        for(final String pkg:new String[]{TT,JP}) {
            test("tt117_unlabelled_profile_routes_native_"+pkg,()->{try(F f=new F(pkg)){
                TabAbilityAssem a=profile(f,false);
                check(OneBackExit.tryBack(f.activity),"profile Back owned");Handler.drain();
                check(a.calls==1&&"HOME".equals(a.lastTag)&&"HOME".equals(a.selected),"one verified native Home selection");
                check(TikTokScreen.inspect(f.activity).home,"Home confirmed with feed and active native route");
                check(f.activity.nativeBackCalls==0,"Home route avoids native no-op Back");preserved(f);
                check(OneBackExit.tryBack(f.activity),"same-event duplicate consumed");check(a.calls==1,"no duplicate route");preserved(f);
                check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"next Back exits confirmed Home");
                check(f.activity.removeCalls==1,"next Back removes task");
            }});
            test("tt117_noop_home_click_falls_back_to_native_"+pkg,()->{try(F f=new F(pkg)){
                TabAbilityAssem a=profile(f,true);f.home.clickAction=()->{};
                OneBackExit.tryBack(f.activity);Handler.drain();
                check(f.home.clickCalls==1&&a.calls==1&&"HOME".equals(a.selected),"acknowledged no-op button cannot trap profile");preserved(f);
            }});
        }
        test("tt117_native_home_background_is_not_exit_permission",()->{try(F f=new F(TT)){
            TabAbilityAssem a=profile(f,false);a.selected="HOME";a.tree.homeActive=true;
            check(!TikTokScreen.inspect(f.activity).home,"visible profile vetoes retained native Home");
            OneBackExit.tryBack(f.activity);Handler.drain();check(a.calls==0,"does not refresh already active Home");preserved(f);
        }});
        test("tt117_native_home_inactive_path_is_not_exit_permission",()->{try(F f=new F(TT)){
            TabAbilityAssem a=profile(f,false);a.selected="HOME";a.tree.homeActive=false;
            check(!TikTokScreen.inspect(f.activity).home,"inactive parent Home is not foreground");
            OneBackExit.tryBack(f.activity);Handler.drain();check(a.calls==1&&a.tree.homeActive,"route repairs inactive profile path");preserved(f);
        }});
        test("tt117_profile_comments_close_before_home",()->{try(F f=new F(TT)){
            TabAbilityAssem a=profile(f,false);View layer=marker(f,"comments_container",new Rect(0,650,1080,2200));
            f.activity.nativeBackAction=()->{check(a.calls==0,"Home not routed through open comments");f.root.removeView(layer);};
            OneBackExit.tryBack(f.activity);Handler.drain();check(f.activity.nativeBackCalls==1&&a.calls==1,"close comments then native Home");preserved(f);
        }});
        test("tt117_profile_editor_preserved",()->{try(F f=new F(TT)){
            TabAbilityAssem a=profile(f,false);View edit=new View(f.activity);edit.editor=true;f.root.focusView=edit;
            OneBackExit.tryBack(f.activity);Handler.drain();check(a.calls==0,"focused editor never discarded");preserved(f);
        }});
        for(final String form:new String[]{"edit_profile_container","profile_edit","draft_editor"}) {
            test("tt117_unfocused_form_keeps_native_confirmation_"+form,()->{try(F f=new F(TT)){
                TabAbilityAssem a=profile(f,true);marker(f,form,new Rect(0,100,1080,1850));
                final int[] confirmations={0};f.activity.nativeBackAction=()->{confirmations[0]++;};
                OneBackExit.tryBack(f.activity);Handler.drain();
                check(a.calls==0&&f.home.clickCalls==0,"no tab switch through unfocused form");
                check(confirmations[0]==1,"one native Back retains confirmation");preserved(f);
            }});
        }
        test("tt117_plain_settings_preserves_home_navigation",()->{try(F f=new F(TT)){
            TabAbilityAssem a=profile(f,true);View settings=marker(f,"settings_container",new Rect(0,100,1080,1850));
            f.home.clickAction=()->{a.selected="HOME";a.tree.homeActive=true;a.transition.run();f.root.removeView(settings);f.home.selected=true;};
            OneBackExit.tryBack(f.activity);Handler.drain();
            check(f.home.clickCalls==1&&a.calls==0&&TikTokScreen.inspect(f.activity).home,"plain settings retain existing Home-tab navigation");preserved(f);
        }});
        test("tt117_unlabelled_unfocused_editor_keeps_native_confirmation",()->{try(F f=new F(TT)){
            TabAbilityAssem a=profile(f,true);View editor=new View(f.activity);editor.editor=true;
            editor.bounds=new Rect(100,600,700,750);f.root.addView(editor);
            OneBackExit.tryBack(f.activity);Handler.drain();
            check(a.calls==0&&f.home.clickCalls==0&&f.activity.nativeBackCalls==1,"visible unlabelled text editor retains native Back");preserved(f);
        }});
        test("tt117_async_native_transition_does_not_pop_old_profile",()->{try(F f=new F(TT)){
            TabAbilityAssem a=profile(f,false);final Runnable finish=a.transition;
            final Handler handler=new Handler(android.os.Looper.getMainLooper());
            a.transition=()->handler.post(()->handler.post(finish));
            OneBackExit.tryBack(f.activity);Handler.drain();
            check(a.calls==1&&f.activity.nativeBackCalls==0,"wait for native transition without an extra Back");
            check(TikTokScreen.inspect(f.activity).home,"asynchronous Home transition completes");preserved(f);
        }});
        test("tt117_native_route_noop_is_bounded",()->{try(F f=new F(TT)){
            TabAbilityAssem a=profile(f,false);a.noOp=true;
            OneBackExit.tryBack(f.activity);Handler.drain();check(a.calls==1,"native no-op requested once per gesture");preserved(f);
        }});
        test("tt117_native_route_failure_keeps_native_fallback",()->{try(F f=new F(TT)){
            TabAbilityAssem a=profile(f,false);a.fail=true;
            OneBackExit.tryBack(f.activity);Handler.drain();check(a.calls==1&&f.activity.nativeBackCalls>=1,"reflection failure delegates safely");preserved(f);
        }});
        test("tt117_native_home_requires_visible_feed",()->{try(F f=new F(TT)){
            TabAbilityAssem a=profile(f,false);a.selected="HOME";a.tree.homeActive=true;f.root.removeView(f.root.getChildAt(1));
            check(!TikTokScreen.inspect(f.activity).home,"native tags alone never permit exit");preserved(f);
        }});
    }
    public static void main(String[] args)throws Exception{run();System.out.println("PASS Back117TikTokTest");}
}
