package app.hiro.oneback.runtime;

import android.app.*;
import android.graphics.Rect;
import android.view.*;
import java.util.*;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Cases derived from v447 APK class/resource/module evidence. Android UI is
 * simulated. These identifiers remain useful for layer classification, but128
 * requires a native root-tab path before permitting task exit. */
public final class Back117InstagramTest {
 public static class ModuleFragment {
  final String module; public boolean visible=true;
  public ModuleFragment(String m){module=m;}
  public String getModuleName(){return module;}
  public boolean isVisible(){return visible;}
  public Object getChildFragmentManager(){return null;}
 }
 public static class ViewerSubclass extends instagram.features.clips.viewer.ui.ClipsSwipeRefreshLayout {
  ViewerSubclass(android.content.Context c){super(c);}
 }
 static ViewGroup viewer(F f,boolean subclass) {
  ViewGroup v=subclass?new ViewerSubclass(f.activity):new instagram.features.clips.viewer.ui.ClipsSwipeRefreshLayout(f.activity);
  v.bounds=new Rect(0,80,1080,2050);f.root.addView(v);return v;
 }
 static Map<String,Test> scenarios(){
  Map<String,Test> tests=new LinkedHashMap<String,Test>();
  for(final String module:new String[]{"clips_viewer_clips_tab","clips_viewer_homecoming_fyp","clips_viewer_profile","clips_viewer_explore"})
   tests.put("ig117_v447_module_"+module,()->{try(F f=new F(IG,true,false)){
    f.activity.fragments.fragments.add(new ModuleFragment(module));
    ActivityManager.AppTask task=ownTask(f,11);
    if("clips_viewer_profile".equals(module)) {
     check(!OneBackExit.tryBack(f.activity),"profile-origin viewer retains native profile return");
     check(!task.excluded&&task.removeCalls==0,"profile navigation keeps its task");
     check(f.activity.nativeBackCalls==0,"caller continues native Back exactly once");
    }else {
     check(!OneBackExit.tryBack(f.activity),"viewer module alone does not prove a root Reels tab");
     check(!task.excluded&&task.removeCalls==0,"unverified viewer preserves its task");
     check(f.activity.nativeBackCalls==0,"native continuation is left to the caller exactly once");
    }
   }});
  for(final boolean inherited:new boolean[]{false,true})
   tests.put("ig117_v447_view_class_"+inherited,()->{try(F f=new F(IG,true,false)){
    viewer(f,inherited);backArrow(f);ActivityManager.AppTask task=ownTask(f,11);
    check(!OneBackExit.key(f.activity,down()),"viewer class alone does not capture key down");preserved(f);
    check(!OneBackExit.key(f.activity,up()),"viewer class alone does not capture key up");
    check(!task.excluded&&task.removeCalls==0,"class-recognized viewer preserves the unverified root task");
   }});
  for(final String id:new String[]{"clips_swipe_refresh_container","clips_viewer_fragment_container"})
   tests.put("ig117_v447_resource_"+id,()->{try(F f=new F(IG,true,false)){
    marker(f,id,new Rect(0,40,1080,2050));backArrow(f);
    check(!OneBackExit.tryBack(f.activity),"actual viewer resource does not identify its launch route");preserved(f);
   }});
  tests.put("ig117_two_pane_host_not_open_comment_panel",()->{try(F f=new F(IG,true,false)){
   ViewGroup pane=new ViewGroup(f.activity);pane.id=++markerId;
   android.content.res.Resources.IDS.put(pane.id,"clips_viewer_comments_two_pane_layout");
   f.root.addView(pane);ViewGroup v=viewer(f,false);f.root.removeView(v);pane.addView(v);
   check(!InstagramBack.inspect(f.activity).blocked,"viewer ancestor is not proof that comments are open");
   check(!OneBackExit.tryBack(f.activity),"bare viewer layout without a root contract stays native");preserved(f);
  }});
  tests.put("ig117_actual_class_comment_child_collapses_only",()->{try(F f=new F(IG,true,false)){
   ViewGroup v=viewer(f,false);View comments=igPanel(f,"comments_container");f.root.removeView(comments);v.addView(comments);
   f.activity.nativeBackAction=()->v.removeView(comments);ActivityManager.AppTask task=ownTask(f,11);
   check(OneBackExit.tryBack(f.activity),"comments Back handled");preserved(f);
   check(f.activity.nativeBackCalls==1&&!task.excluded,"comment panel closes without recents changes");
   check(!OneBackExit.key(f.activity,down())&&!OneBackExit.key(f.activity,up()),"separate Back delegates for the remaining unverified viewer");
   check(!task.excluded&&task.removeCalls==0,"comment closure does not authorize task removal");
  }});
  tests.put("ig117_small_actual_class_preview_delegates",()->{try(F f=new F(IG,true,false)){
   viewer(f,false).bounds=new Rect(0,100,900,650);
   check(!OneBackExit.tryBack(f.activity),"embedded preview is not full Reels");preserved(f);
  }});
  tests.put("ig117_hidden_actual_class_delegates",()->{try(F f=new F(IG,true,false)){
   viewer(f,false).hidden=true;
   check(!OneBackExit.tryBack(f.activity),"hidden viewer is not current screen");preserved(f);
  }});
  tests.put("ig117_profile_over_actual_module_delegates",()->{try(F f=new F(IG,true,false)){
   viewer(f,false);f.activity.fragments.fragments.add(new ModuleFragment("clips_viewer_clips_tab"));
   f.activity.fragments.fragments.add(new Back116Regression.ProfileFragment());
   check(!OneBackExit.tryBack(f.activity),"foreground profile vetoes Reels behind");preserved(f);
  }});
  tests.put("ig117_selected_home_under_foreground_profile_delegates",()->{try(F f=new F(IG)){
   f.activity.fragments.fragments.add(new Back116Regression.ProfileFragment());
   check(!OneBackExit.tryBack(f.activity),"underlying selected Home cannot override foreground profile");preserved(f);
  }});
  tests.put("ig117_exact_v447_obfuscated_manager",()->{
   Activity a=Instagram447Fixture.create(new Application(IG),false);
   Instagram447Fixture.added(a).add(new ModuleFragment("clips_viewer_clips_tab"));
   check(InstagramReels.foreground(a)==1,"exact A1Y/A0V/A01 path finds foreground Reels");
   Instagram447Fixture.added(a).add(new Back116Regression.ProfileFragment());
   check(InstagramReels.foreground(a)==-1,"ordered v447 manager identifies profile in front of Reels");
   ViewGroup root=(ViewGroup)a.getWindow().getDecorView();View home=new View(a);
   home.description="ホーム";home.selected=true;home.clickable=true;home.bounds=new Rect(0,1990,190,2170);root.addView(home);
   a.getWindow().attach();root.focus(true);
   try{check(HomeScreen.isHome(a),"old Home scanner still sees the selected underlying tab");
    check(!ExitScreens.shouldExit(a),"new foreground adapter veto runs before selected-Home exit");
   }finally{root.detach();}

  });
  tests.put("ig117_v447_manager_type_mismatch_rejected",()->{
   Activity a=Instagram447Fixture.create(new Application(IG),true);
   Instagram447Fixture.added(a).add(new ModuleFragment("clips_viewer_clips_tab"));
   check(InstagramReels.foreground(a)==0,"different field signature is not read as a verified manager");
  });
  return tests;
 }
 static void run()throws Exception {for(Map.Entry<String,Test> e:scenarios().entrySet())test(e.getKey(),e.getValue());}
 public static void main(String[] args)throws Exception {
  int failed=0;for(Map.Entry<String,Test> e:scenarios().entrySet())try{test(e.getKey(),e.getValue());}catch(AssertionError x){failed++;System.out.println("FAIL "+e.getKey()+": "+x.getMessage());}
  System.out.println("REGRESSION Instagram: "+scenarios().size()+" cases, "+failed+" failed");if(failed>0)System.exit(1);
 }
}
