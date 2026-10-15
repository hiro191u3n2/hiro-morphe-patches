package app.hiro.oneback.runtime;
import android.app.*;import android.content.*;import android.graphics.Rect;import android.os.*;import android.view.*;import android.view.inspector.WindowInspector;import android.window.*;
import java.util.*;

/** Executes production recognition and exit code against deterministic Android doubles, not a physical device. */
public final class HomeBackHostTest {
 static int assertions,cases;static final String X="com.twitter.android",IG="com.instagram.android",TT="com.zhiliaoapp.musically",JP="com.ss.android.ugc.trill",TRIP="ctrip.english";
 static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
 interface Test{void run()throws Exception;}
 static void test(String name,Test t)throws Exception{Build.VERSION.SDK_INT=36;WindowInspector.clear();t.run();Handler.drain();check(Handler.queuedCount()==0,"no persistent work loop");cases++;System.out.println("PASS "+name);}
 static final class F implements AutoCloseable {
  final Application app;final Activity activity;final ViewGroup root;final View home;final ActivityManager tasks=new ActivityManager();final List<Dialog> dialogs=new ArrayList<Dialog>();
  F(String pkg){this(pkg,true,true);}
  F(String pkg,boolean recognized,boolean selected){
   app=new Application(pkg);app.setService(Context.ACTIVITY_SERVICE,tasks);
   activity=!recognized?new Activity(app,11,true):X.equals(pkg)?new com.twitter.android.HomeTimelineActivity(app,11,true):IG.equals(pkg)?new com.instagram.mainactivity.InstagramMainActivity(app,11,true):(TT.equals(pkg)||JP.equals(pkg))?new com.ss.android.ugc.aweme.main.MainActivity(app,11,true):new Activity(app,11,true);
   root=(ViewGroup)activity.getWindow().getDecorView();home=new View(activity);home.description="ホーム";home.selected=selected;home.clickable=true;home.bounds=new Rect(0,1990,190,2170);root.addView(home);
   if(TT.equals(pkg)||JP.equals(pkg)){android.widget.TextView feed=new android.widget.TextView(activity);feed.text="おすすめ";feed.bounds=new Rect(300,40,600,140);root.addView(feed);}
   if(TRIP.equals(pkg)&&recognized){View content=new com.ctrip.ibu.home.home.presentation.page.fragment.widget.HomeAppBarLayout(activity);content.bounds=new Rect(0,100,1080,1600);root.addView(content);}
   OneBackExit.install(activity);activity.getWindow().attach();root.focus(true);OneBackExit.onResume(activity);Handler.drain();
  }
  FakeDispatcher dispatcher(){return activity.getWindow().dispatcher();}
  void draw(){SystemClock.time+=130;root.getViewTreeObserver().draw();Handler.drain();}
  Dialog dialog(){Dialog d=new Dialog(activity);dialogs.add(d);d.show();d.getWindow().getDecorView().focus(true);OneBackExit.dialogShown(d);Handler.drain();return d;}
  @Override public void close(){for(Dialog d:dialogs)d.dismiss();OneBackExit.onDestroy(activity);activity.destroyed=true;root.detach();Handler.drain();WindowInspector.clear();}
 }
 static void preserved(F f){check(!f.activity.finishing&&f.activity.removeCalls==0&&f.activity.finishCalls==0,"non-home preserved");}
 static KeyEvent down(){return new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK);}static KeyEvent up(){return new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK);}
 public static final class DetailFragment {public boolean isVisible(){return true;}public Object getChildFragmentManager(){return null;}}
 public static final class ChildFragment {public FragmentManager child=new FragmentManager();public boolean isVisible(){return true;}public FragmentManager getChildFragmentManager(){return child;}}
 static int markerId=100;
 static View marker(F f,String name,Rect bounds){View view=new View(f.activity);view.id=++markerId;android.content.res.Resources.IDS.put(view.id,name);view.bounds=bounds;f.root.addView(view);return view;}
 static void backArrow(F f){View arrow=new View(f.activity);arrow.description="戻る";arrow.clickable=true;arrow.bounds=new Rect(0,20,100,100);f.root.addView(arrow);}
 static ActivityManager.AppTask ownTask(F f,int id){ActivityManager.RecentTaskInfo info=new ActivityManager.RecentTaskInfo();info.taskId=id;info.baseActivity=new ComponentName(f.activity.getPackageName(),"Main");info.baseIntent=new Intent().setComponent(info.baseActivity);ActivityManager.AppTask task=new ActivityManager.AppTask(info);if(id==11)task.activities.add(f.activity);f.tasks.tasks.add(task);return task;}
 public static final class SettingsFragment {public boolean isVisible(){return true;}public Object getChildFragmentManager(){return null;}}
 static View tiktokTarget(F f){View target=new View(f.activity);target.description="ホーム";target.clickable=true;target.bounds=new Rect(0,1990,190,2170);target.clickAction=()->{f.home.selected=false;target.selected=true;};f.root.addView(target);return target;}
 static void armTrip(F f){OneBackExit.enterBack(f.activity);try{OneBackExit.afterNative();}finally{OneBackExit.leaveNative();}}

 public static final class SupportMain extends com.ss.android.ugc.aweme.main.MainActivity {
  public final FragmentManager support=new FragmentManager();
  public SupportMain(Application app,int id,boolean root){super(app,id,root);}
  public FragmentManager getSupportFragmentManager(){return support;}
 }
 public static final class ObfuscatedCommentsFragment {
  public boolean isVisible(){return true;}public String getModuleName(){return "comments_thread";}
  public Object getChildFragmentManager(){return null;}
 }
 static View igPanel(F f,String name){return marker(f,name,new Rect(0,650,1080,2200));}
 static void closedIG(F f,View panel){
  f.activity.nativeBackAction=()->f.root.removeView(panel);
  check(!ExitScreens.shouldExit(f.activity),"foreground comments veto Home/Reels");
  check(OneBackExit.tryBack(f.activity),"comments Back consumed");
  check(f.activity.nativeBackCalls==1,"one native close");check(!InstagramBack.inspect(f.activity).blocked,"comments no longer open");preserved(f);
  check(OneBackExit.tryBack(f.activity),"same transaction duplicate consumed");check(f.activity.nativeBackCalls==1,"duplicate does not advance navigation");preserved(f);
  check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"new physical Back exits Home/Reels");check(f.activity.removeCalls==1,"one task removal");
 }
 public static void main(String[] args)throws Exception {
  for(final String pkg:new String[]{X,IG,TT,JP,TRIP}) {
   test("confirmed_home_"+pkg,()->{try(F f=new F(pkg)){check(HomeScreen.isHome(f.activity),"positive selected home");check(OneBackExit.tryBack(f.activity),"home handled");check(f.activity.removeCalls==1,"home removes task once");}});
   test("other_tab_"+pkg,()->{try(F f=new F(pkg,true,false)){check(!HomeScreen.isHome(f.activity),"unselected home rejected");check(OneBackExit.tryBack(f.activity)==(TT.equals(pkg)||JP.equals(pkg)),"TikTok consumes non-home; other apps retain native delegation");check(f.dispatcher().count()==(TRIP.equals(pkg)||TT.equals(pkg)||JP.equals(pkg)?1:0),"TikTok protects unknown pages; Trip observes native Back");preserved(f);}});
  }
  test("unknown_activity_is_not_home",()->{try(F f=new F(X,false,true)){check(!HomeScreen.isHome(f.activity),"root+home text not enough");check(!OneBackExit.tryBack(f.activity),"unknown delegates");preserved(f);}});
  test("unknown_package",()->{try(F f=new F("com.example.other")){OneBackExit.back(f.activity);check(!OneBackExit.key(f.activity,up()),"foreign key delegated");check(f.app.registrations==0,"foreign lifecycle untouched");preserved(f);}});
  test("keyboard_visible",()->{try(F f=new F(IG)){f.root.insets.ime=true;check(!HomeScreen.isHome(f.activity),"IME blocks");f.draw();check(f.dispatcher().count()==0,"callback withdrawn while IME shown");check(!OneBackExit.key(f.activity,down()),"IME down passed");check(!OneBackExit.keyFromView(f.home,up()),"pre-IME always native");preserved(f);}});
  test("unknown_insets",()->{try(F f=new F(TT)){f.root.insets=null;check(!HomeScreen.isHome(f.activity),"unknown IME state is conservative");preserved(f);}});
  test("focused_editor",()->{try(F f=new F(X)){View editor=new View(f.activity);editor.editor=true;f.root.focusView=editor;check(!HomeScreen.isHome(f.activity),"text editing blocks");preserved(f);}});
  test("dialog_over_home",()->{try(F f=new F(IG)){Dialog d=f.dialog();check(!HomeScreen.isHome(f.activity),"another focused window blocks");check(!OneBackExit.tryDialogBack(d),"dialog back native");check(!OneBackExit.dialogKey(d,up()),"dialog key native");check(d.getWindow().dispatcher().count()==0,"dialog has no exit callback");preserved(f);}});
  test("fragment_back_stack",()->{try(F f=new F(X)){f.activity.fragments.count=1;check(!HomeScreen.isHome(f.activity),"detail in same activity blocked");check(!OneBackExit.tryBack(f.activity),"detail delegated");preserved(f);}});
  test("visible_detail_fragment",()->{try(F f=new F(IG)){f.activity.fragments.fragments.add(new DetailFragment());check(!HomeScreen.isHome(f.activity),"details fragment blocked");preserved(f);}});
  test("nested_navigation_stack",()->{try(F f=new F(JP)){ChildFragment child=new ChildFragment();child.child.count=1;f.activity.fragments.fragments.add(child);check(!HomeScreen.isHome(f.activity),"child stack blocks");preserved(f);}});
  test("top_back_arrow",()->{try(F f=new F(X)){View arrow=new View(f.activity);arrow.clickable=true;arrow.description="戻る";arrow.bounds=new Rect(0,20,100,100);f.root.addView(arrow);check(!HomeScreen.isHome(f.activity),"toolbar back blocks selected home behind");preserved(f);}});
  test("comments_bottom_sheet",()->{try(F f=new F(TT)){View sheet=new View(f.activity);sheet.id=71;android.content.res.Resources.IDS.put(71,"comment_sheet");sheet.bounds=new Rect(0,600,1080,2200);f.root.addView(sheet);check(!HomeScreen.isHome(f.activity),"sheet blocks");preserved(f);}});
  test("ordinary_home_text_is_not_nav",()->{try(F f=new F(X)){f.home.bounds=new Rect(0,500,190,680);check(!HomeScreen.isHome(f.activity),"content text not nav");preserved(f);}});
  test("home_not_clickable",()->{try(F f=new F(IG)){f.home.clickable=false;check(!HomeScreen.isHome(f.activity),"noninteractive label rejected");preserved(f);}});
  test("invisible_or_transparent_home",()->{try(F f=new F(TT)){f.home.hidden=true;check(!HomeScreen.isHome(f.activity),"hidden rejected");f.home.hidden=false;f.home.alpha=0;check(!HomeScreen.isHome(f.activity),"transparent rejected");preserved(f);}});
  test("parent_tab_selection",()->{try(F f=new F(IG)){f.root.removeAllViews();ViewGroup tab=new ViewGroup(f.activity);tab.bounds=new Rect(0,1980,200,2190);tab.selected=true;tab.clickable=true;f.home.selected=false;f.home.clickable=false;tab.addView(f.home);f.root.addView(tab);check(HomeScreen.isHome(f.activity),"icon inherits tab selection");}});
  test("home_resource_identity",()->{try(F f=new F(X)){f.home.description="";f.home.id=72;android.content.res.Resources.IDS.put(72,"home_tab");check(HomeScreen.isHome(f.activity),"semantic home id");}});
  test("keyboard_key_sequence",()->{try(F f=new F(X)){check(OneBackExit.key(f.activity,down()),"home down armed");preserved(f);check(OneBackExit.key(f.activity,up()),"home up handled");check(f.activity.removeCalls==1,"only release exits");}});
  test("cancelled_release",()->{try(F f=new F(IG)){check(OneBackExit.key(f.activity,down()),"armed");check(OneBackExit.key(f.activity,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,true)),"cancel release consumed");preserved(f);}});
  test("nonhome_down_home_up_does_not_exit",()->{try(F f=new F(TT,true,false)){check(OneBackExit.key(f.activity,down()),"nonhome down captured for navigation");f.home.selected=true;check(OneBackExit.key(f.activity,up()),"same up consumed without acquiring exit permission");preserved(f);}});
  test("home_down_detail_up_does_not_exit",()->{try(F f=new F(X)){check(OneBackExit.key(f.activity,down()),"home down captured");f.activity.fragments.count=1;check(OneBackExit.key(f.activity,up()),"paired release consumed");preserved(f);}});
  test("release_without_down",()->{try(F f=new F(IG)){check(!OneBackExit.key(f.activity,up()),"orphan release not exit");preserved(f);}});
  test("nested_native_back_suppression",()->{try(F f=new F(X,true,false)){OneBackExit.enterNative();try{f.home.selected=true;check(!OneBackExit.tryBack(f.activity),"same back after returning home cannot exit");OneBackExit.enterNative();try{check(!OneBackExit.tryBack(f.activity),"super back protected");}finally{OneBackExit.leaveNative();}}finally{OneBackExit.leaveNative();}preserved(f);check(OneBackExit.tryBack(f.activity),"next separate back exits");}});
  test("native_scope_exception_cleanup",()->{try(F f=new F(IG)){try{OneBackExit.enterNative();try{throw new IllegalStateException();}finally{OneBackExit.leaveNative();}}catch(IllegalStateException expected){}check(OneBackExit.tryBack(f.activity),"exception cannot permanently suppress exit");}});
  test("native_callback_detail_to_home_once",()->{try(F f=new F(TT,true,false)){final int[] calls={0};OnBackInvokedCallback cb=()->{calls[0]++;f.home.selected=true;check(!OneBackExit.tryBack(f.activity),"nested app callback protected");};NativeBack.register(f.dispatcher(),0,cb);f.dispatcher().invokeBack();check(calls[0]==1,"one native action");preserved(f);SystemClock.time+=400;f.draw();f.dispatcher().invokeBack();check(f.activity.removeCalls==1,"next home back exit");NativeBack.unregister(f.dispatcher(),cb);}});
  test("native_callback_unregister_identity",()->{try(F f=new F(IG,true,false)){OnBackInvokedCallback cb=()->{};NativeBack.register(f.dispatcher(),0,cb);NativeBack.register(f.dispatcher(),0,cb);check(f.dispatcher().count()==1,"repeat registration keeps one instance");NativeBack.unregister(f.dispatcher(),cb);check(f.dispatcher().count()==0,"unregister uses wrapped identity");}});
  test("stale_home_callback_forwards_native",()->{try(F f=new F(X)){final int[] calls={0};OnBackInvokedCallback cb=()->calls[0]++;NativeBack.register(f.dispatcher(),0,cb);OneBackExit.onResume(f.activity);Handler.drain();f.home.selected=false;f.dispatcher().invokeBack();Handler.drain();check(calls[0]==1,"stale callback native forwarded");preserved(f);NativeBack.unregister(f.dispatcher(),cb);}});
  test("stale_callback_without_app_registration",()->{try(F f=new F(IG)){f.home.selected=false;f.dispatcher().invokeBack();Handler.drain();check(f.activity.nativeBackCalls==1,"legacy native fallback one call");preserved(f);}});
  test("predictive_animation_preserved",()->{try(F f=new F(TT,true,false)){final int[] count={0,0,0,0};OnBackAnimationCallback cb=new OnBackAnimationCallback(){public void onBackStarted(BackEvent e){count[0]++;}public void onBackProgressed(BackEvent e){count[1]++;}public void onBackCancelled(){count[2]++;}public void onBackInvoked(){count[3]++;}};NativeBack.register(f.dispatcher(),0,cb);check(f.dispatcher().top() instanceof OnBackAnimationCallback,"animation interface retained");OnBackAnimationCallback wrapped=(OnBackAnimationCallback)f.dispatcher().top();wrapped.onBackStarted(new BackEvent());wrapped.onBackProgressed(new BackEvent());wrapped.onBackCancelled();preserved(f);check(count[3]==0,"cancel never commits");wrapped.onBackInvoked();check(Arrays.equals(count,new int[]{1,1,1,1}),"all native callbacks forwarded once");NativeBack.unregister(f.dispatcher(),cb);}});
  test("default_priority_does_not_override_ime",()->{try(F f=new F(X)){final int[] ime={0};f.dispatcher().registerOnBackInvokedCallback(1000000,()->ime[0]++);f.dispatcher().invokeBack();check(ime[0]==1,"overlay receives Back first");preserved(f);}});
  test("destroy_removes_listener",()->{try(F f=new F(IG)){check(f.root.getViewTreeObserver().drawCount()==1,"one bounded pre-draw observer");OneBackExit.onDestroy(f.activity);check(f.dispatcher().count()==0,"destroy unregisters");check(f.root.getViewTreeObserver().drawCount()==0,"destroy releases observer");}});
  test("home_transition_rearms_on_draw",()->{try(F f=new F(JP,true,false)){check(f.dispatcher().count()==1,"TikTok navigation guard armed off home");f.home.selected=true;f.draw();check(f.dispatcher().count()==1,"armed on home");f.home.selected=false;f.draw();check(f.dispatcher().count()==1,"TikTok retains navigation guard off home");}});
  test("trip_keyboard_preserved",()->{try(F f=new F(TRIP)){f.root.insets.ime=true;check(!HomeScreen.isHome(f.activity),"Trip IME blocks");check(!OneBackExit.tryBack(f.activity),"Trip keyboard delegates");f.draw();check(f.dispatcher().count()==0,"Trip callback withdrawn for IME");check(!OneBackExit.keyFromView(f.home,up()),"Trip pre-IME delegated");preserved(f);}});
  test("trip_dialog_preserved",()->{try(F f=new F(TRIP)){Dialog dialog=f.dialog();check(!OneBackExit.tryDialogBack(dialog),"Trip dialog is native");check(!OneBackExit.dialogKey(dialog,up()),"Trip dialog key native");check(!OneBackExit.tryBack(f.activity),"overlay blocks activity exit");preserved(f);}});
  test("trip_detail_activity_without_home_layout",()->{try(F f=new F(TRIP,false,true)){check(!HomeScreen.isHome(f.activity),"home label alone not Trip home");check(!OneBackExit.tryBack(f.activity),"unknown Trip screen delegates");check(f.dispatcher().count()==1,"unknown Trip screen keeps native Back observer");preserved(f);}});
  test("trip_hidden_home_layout",()->{try(F f=new F(TRIP)){f.root.getChildAt(1).hidden=true;check(!HomeScreen.isHome(f.activity),"hidden home not accepted");check(!OneBackExit.tryBack(f.activity),"hidden delegates");preserved(f);}});
  test("trip_transparent_home_layout",()->{try(F f=new F(TRIP)){f.root.getChildAt(1).alpha=0;check(!HomeScreen.isHome(f.activity),"transparent home not accepted");preserved(f);}});
  test("trip_unselected_home_cannot_exit",()->{try(F f=new F(TRIP,true,false)){check(!OneBackExit.key(f.activity,down()),"other tab native down");check(!OneBackExit.key(f.activity,up()),"other tab native up");preserved(f);}});
  test("trip_booking_backstack",()->{try(F f=new F(TRIP)){f.activity.fragments.count=1;check(!HomeScreen.isHome(f.activity),"booking stack blocks home behind");check(!OneBackExit.tryBack(f.activity),"booking native");preserved(f);}});
  test("trip_detail_fragment",()->{try(F f=new F(TRIP)){f.activity.fragments.fragments.add(new DetailFragment());check(!HomeScreen.isHome(f.activity),"visible detail blocks");preserved(f);}});
  test("trip_toolbar_back",()->{try(F f=new F(TRIP)){View arrow=new View(f.activity);arrow.description="戻る";arrow.clickable=true;arrow.bounds=new Rect(0,20,100,100);f.root.addView(arrow);check(!HomeScreen.isHome(f.activity),"toolbar back blocks");preserved(f);}});
  test("trip_text_home_tab",()->{try(F f=new F(TRIP)){f.home.description="";f.home.selected=false;android.widget.TextView tab=new android.widget.TextView(f.activity);tab.text="ホーム";tab.selected=true;tab.clickable=true;tab.bounds=new Rect(0,1990,190,2170);f.root.addView(tab);check(HomeScreen.isHome(f.activity),"selected TextView home accepted with real home layout");check(OneBackExit.tryBack(f.activity),"text tab home exits");check(f.activity.removeCalls==1,"text home removed once");}});
  test("trip_text_home_not_selected",()->{try(F f=new F(TRIP)){f.home.description="";f.home.selected=false;android.widget.TextView tab=new android.widget.TextView(f.activity);tab.text="ホーム";tab.clickable=true;tab.bounds=new Rect(0,1990,190,2170);f.root.addView(tab);check(!HomeScreen.isHome(f.activity),"text alone cannot prove selection");preserved(f);}});
  test("trip_nonhome_to_home_same_key",()->{try(F f=new F(TRIP,true,false)){check(!OneBackExit.key(f.activity,down()),"detail down native");f.home.selected=true;check(!OneBackExit.key(f.activity,up()),"same key cannot exit newly returned home");preserved(f);check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"next separate key exits");check(f.activity.removeCalls==1,"one task removal");}});
  test("trip_native_callback_returns_home",()->{try(F f=new F(TRIP,true,false)){final int[] calls={0};OnBackInvokedCallback cb=()->{calls[0]++;f.home.selected=true;check(!OneBackExit.tryBack(f.activity),"native scope protects home transition");};NativeBack.register(f.dispatcher(),0,cb);f.dispatcher().invokeBack();check(calls[0]==1,"one native back");preserved(f);f.draw();f.dispatcher().invokeBack();check(f.activity.removeCalls==1,"next home back exits");NativeBack.unregister(f.dispatcher(),cb);}});
  test("trip_keyboard_closes_then_home",()->{try(F f=new F(TRIP)){f.root.insets.ime=true;f.draw();check(!OneBackExit.key(f.activity,down()),"IME down native");f.root.insets.ime=false;check(!OneBackExit.key(f.activity,up()),"IME release cannot exit");preserved(f);f.draw();check(f.dispatcher().count()==1,"home rearmed after IME");check(OneBackExit.tryBack(f.activity),"new Back exits");}});
  test("trip_prediction_cancelled",()->{try(F f=new F(TRIP)){check(OneBackExit.key(f.activity,down()),"home key armed");check(OneBackExit.key(f.activity,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,true)),"cancel handled");preserved(f);}});
  test("trip_default_priority_below_overlay",()->{try(F f=new F(TRIP)){final int[] ime={0};f.dispatcher().registerOnBackInvokedCallback(1000000,()->ime[0]++);f.dispatcher().invokeBack();check(ime[0]==1,"overlay first");preserved(f);}});
  test("trip_home_to_detail_withdraws_callback",()->{try(F f=new F(TRIP)){check(f.dispatcher().count()==1,"home armed");f.home.selected=false;f.draw();check(f.dispatcher().count()==1,"Trip detail keeps pass-through observer");preserved(f);}});
  test("torip_different_package_untouched",()->{try(F f=new F("com.torip.app")){check(!HomeScreen.restricted(f.activity.getPackageName()),"only ctrip.english added");check(!OneBackExit.tryBack(f.activity),"unrelated app untouched");preserved(f);}});
  test("api29_no_modern_callback",()->{Build.VERSION.SDK_INT=29;try(F f=new F(X)){check(f.dispatcher().count()==0,"API29 no API33 registration");check(HomeScreen.isHome(f.activity),"legacy home detection");check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"legacy home back handled");}});
  for(final String pkg:new String[]{IG}) {
   for(final String label:(IG.equals(pkg)?new String[]{"リール","Reels"}:new String[]{"メッセージ","プロフィール","Inbox","Profile"})) {
    test("explicit_exit_"+pkg+"_"+label,()->{try(F f=new F(pkg)){f.home.description=label;f.draw();check(!HomeScreen.isHome(f.activity),"not mislabeled as home");check(ExitScreens.shouldExit(f.activity),"explicit screen recognized");check(f.dispatcher().count()==1,"modern Back active");ActivityManager.AppTask task=ownTask(f,11);check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"one Back exits");check(task.excluded&&task.removeCalls==1,"removed and excluded");check(task.events.equals(Arrays.asList("exclude","remove")),"exclusion precedes removal");}});
   }
  }
  test("ig_fullscreen_clips_with_backstack",()->{try(F f=new F(IG,true,false)){marker(f,"clips_viewer_view_pager",new Rect(0,0,1080,2100));f.activity.fragments.count=1;backArrow(f);check(ExitScreens.shouldExit(f.activity),"viewer with own Back stack exits");check(OneBackExit.tryBack(f.activity),"full screen viewer exit");}});
  test("ig_story_not_reels",()->{try(F f=new F(IG)){f.home.description="Reels";marker(f,"reel_viewer",new Rect(0,0,1080,2100));check(!ExitScreens.shouldExit(f.activity),"legacy reel_viewer is Stories");check(!OneBackExit.tryBack(f.activity),"stories retains native Back");preserved(f);}});
  test("ig_reels_comments_first",()->{try(F f=new F(IG)){f.home.description="リール";marker(f,"clips_viewer_view_pager",new Rect(0,0,1080,2100));View panel=marker(f,"comment_sheet",new Rect(0,700,1080,2200));f.activity.nativeBackAction=()->f.root.removeView(panel);check(OneBackExit.tryBack(f.activity),"comments close path consumed");check(f.activity.nativeBackCalls==1&&!InstagramBack.inspect(f.activity).blocked,"native collapse invoked once");preserved(f);}});
  test("ig_reels_ime_first",()->{try(F f=new F(IG)){f.home.description="リール";f.root.insets.ime=true;check(!OneBackExit.tryBack(f.activity),"IME first");preserved(f);}});
  test("ig_reels_dialog_first",()->{try(F f=new F(IG)){f.home.description="リール";Dialog d=f.dialog();check(!OneBackExit.tryBack(f.activity)&&!OneBackExit.tryDialogBack(d),"dialog first");preserved(f);}});
  test("ig_reels_preview_not_viewer",()->{try(F f=new F(IG,true,false)){marker(f,"clips_viewer_view_pager",new Rect(0,500,300,800));check(!ExitScreens.shouldExit(f.activity),"small preview not full screen");preserved(f);}});
  test("ig_reels_hidden_not_viewer",()->{try(F f=new F(IG,true,false)){marker(f,"clips_viewer_view_pager",new Rect(0,0,1080,2100)).hidden=true;check(!ExitScreens.shouldExit(f.activity),"hidden viewer ignored");}});
  test("ig_unknown_host_tab_native",()->{try(F f=new F(IG,false,true)){f.home.description="リール";check(!OneBackExit.tryBack(f.activity),"tab label in unknown host insufficient");preserved(f);}});
  test("ig_unselected_reels_native",()->{try(F f=new F(IG,true,false)){f.home.description="リール";check(!OneBackExit.tryBack(f.activity),"unselected label is not active screen");preserved(f);}});
  test("tt_unselected_profile_native",()->{try(F f=new F(TT,true,false)){f.home.description="プロフィール";check(OneBackExit.tryBack(f.activity),"unknown profile consumes Back without exit");preserved(f);}});
  test("tt_profile_settings_native",()->{try(F f=new F(TT)){f.home.description="プロフィール";f.activity.fragments.fragments.add(new SettingsFragment());check(OneBackExit.tryBack(f.activity),"settings over profile navigation protected");preserved(f);}});
  test("tt_profile_detail_arrow_native",()->{try(F f=new F(TT)){f.home.description="プロフィール";backArrow(f);check(OneBackExit.tryBack(f.activity),"detail above tab navigation protected");preserved(f);}});
  test("tt_messages_ime_first",()->{try(F f=new F(TT)){f.home.description="メッセージ";f.root.insets.ime=true;check(!OneBackExit.tryBack(f.activity),"message composer IME first");preserved(f);}});
  test("tt_profile_parent_selection",()->{try(F f=new F(JP)){f.root.removeAllViews();ViewGroup tab=new ViewGroup(f.activity);tab.bounds=new Rect(850,1980,1050,2190);tab.selected=true;tab.clickable=true;f.home.bounds=new Rect(860,1990,1040,2170);f.home.description="プロフィール";f.home.selected=false;f.home.clickable=false;tab.addView(f.home);f.root.addView(tab);check(ExitScreens.isTikTokRootTab(f.activity)&&!ExitScreens.shouldExit(f.activity),"parent selected profile navigates rather than exits");}});
  test("tt_profile_textview_selection",()->{try(F f=new F(TT,true,false)){android.widget.TextView tab=new android.widget.TextView(f.activity);tab.text="プロフィール";tab.selected=true;tab.clickable=true;tab.bounds=new Rect(860,1990,1040,2170);f.root.addView(tab);check(ExitScreens.isTikTokRootTab(f.activity)&&!ExitScreens.shouldExit(f.activity),"profile TextView navigates rather than exits");}});
  test("native_return_to_reels_not_same_back",()->{try(F f=new F(IG,true,false)){check(!OneBackExit.key(f.activity,down()),"unknown screen down native");f.home.description="リール";f.home.selected=true;check(!OneBackExit.key(f.activity,up()),"same key not instant exit on return");preserved(f);check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"next key exits");}});
  test("trip_unknown_home_platform_terminal",()->{try(F f=new F(TRIP,false,false)){check(!OneBackExit.tryBack(f.activity),"unknown UI delegates");ActivityManager.AppTask task=ownTask(f,11);OneBackExit.enterBack(f.activity);try{check(OneBackExit.tryTerminalBack(f.activity,0,false),"platform root terminal cleaned");}finally{OneBackExit.leaveNative();}check(task.excluded&&task.removeCalls==1,"terminal task excluded and removed");}});
  test("trip_unknown_home_move_terminal",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);OneBackExit.enterBack(f.activity);try{check(OneBackExit.tryTerminalBack(f.activity,2,true),"native background replaced with exit");}finally{OneBackExit.leaveNative();}check(task.excluded&&task.removeCalls==1,"move path cleaned");}});
  test("trip_unknown_home_finish_terminal",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);OneBackExit.enterBack(f.activity);try{check(OneBackExit.tryTerminalBack(f.activity,1,false),"native root finish cleaned");}finally{OneBackExit.leaveNative();}check(task.excluded&&task.removeCalls==1,"finish path cleaned");}});
  test("trip_no_back_context_no_cleanup",()->{try(F f=new F(TRIP)){ActivityManager.AppTask task=ownTask(f,11);check(!OneBackExit.tryTerminalBack(f.activity,2,true),"ordinary lifecycle/home key not intercepted");check(!task.excluded&&task.removeCalls==0,"no recents mutation outside Back");preserved(f);}});
  test("trip_down_not_terminal_context",()->{try(F f=new F(TRIP)){OneBackExit.enterKey(f.activity,down());try{check(!OneBackExit.tryTerminalBack(f.activity,2,true),"key down not committed");}finally{OneBackExit.leaveNative();}preserved(f);}});
  test("trip_cancel_not_terminal_context",()->{try(F f=new F(TRIP)){OneBackExit.enterKey(f.activity,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,true));try{check(!OneBackExit.tryTerminalBack(f.activity,2,true),"cancel not committed");}finally{OneBackExit.leaveNative();}preserved(f);}});
  test("trip_other_key_not_terminal_context",()->{try(F f=new F(TRIP)){OneBackExit.enterKey(f.activity,new KeyEvent(KeyEvent.ACTION_UP,24));try{check(!OneBackExit.tryTerminalBack(f.activity,2,true),"volume key not Back");}finally{OneBackExit.leaveNative();}preserved(f);}});
  test("trip_key_up_terminal_context",()->{try(F f=new F(TRIP,false,false)){OneBackExit.enterKey(f.activity,up());try{check(OneBackExit.tryTerminalBack(f.activity,2,true),"Back key up terminal valid");}finally{OneBackExit.leaveNative();}check(f.activity.removeCalls==1,"fallback removed");}});
  test("trip_terminal_scope_exception_cleanup",()->{try(F f=new F(TRIP)){try{OneBackExit.enterBack(f.activity);try{throw new IllegalStateException();}finally{OneBackExit.leaveNative();}}catch(IllegalStateException expected){}check(!OneBackExit.tryTerminalBack(f.activity,2,true),"no stale exit context");preserved(f);}});
  test("trip_platform_fragment_pop_native",()->{try(F f=new F(TRIP)){f.activity.fragments.count=1;OneBackExit.enterBack(f.activity);try{check(!OneBackExit.tryTerminalBack(f.activity,0,false),"platform fragment pop preserved");}finally{OneBackExit.leaveNative();}preserved(f);}});
  test("trip_platform_actionbar_native",()->{try(F f=new F(TRIP)){f.activity.actionBar=new ActionBar(){};OneBackExit.enterBack(f.activity);try{check(!OneBackExit.tryTerminalBack(f.activity,0,false),"actionbar collapse preserved");}finally{OneBackExit.leaveNative();}preserved(f);}});
  test("trip_nonroot_finish_native",()->{try(F f=new F(TRIP)){Activity child=new Activity(f.app,11,false);OneBackExit.enterBack(child);try{check(!OneBackExit.tryTerminalBack(child,1,false),"detail Activity finish not terminal");check(!OneBackExit.tryTerminalBack(child,2,false),"nonRoot false not terminal");}finally{OneBackExit.leaveNative();}preserved(f);}});
  test("trip_native_callback_terminal",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);OnBackInvokedCallback cb=()->check(OneBackExit.tryTerminalBack(f.activity,2,true),"callback carries genuine Back context");NativeBack.register(f.dispatcher(),0,cb);f.dispatcher().invokeBack();check(task.excluded&&task.removeCalls==1,"callback terminal cleans recents");NativeBack.unregister(f.dispatcher(),cb);}});
  test("trip_native_callback_navigation_keeps_recents",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);OnBackInvokedCallback cb=()->f.home.selected=true;NativeBack.register(f.dispatcher(),0,cb);f.dispatcher().invokeBack();check(!task.excluded&&task.removeCalls==0,"native navigation without terminal call untouched");NativeBack.unregister(f.dispatcher(),cb);preserved(f);}});
  test("trip_terminal_ime_guard",()->{try(F f=new F(TRIP)){f.root.insets.ime=true;OneBackExit.enterBack(f.activity);try{check(!OneBackExit.tryTerminalBack(f.activity,0,false),"IME closure not exit");}finally{OneBackExit.leaveNative();}preserved(f);}});
  test("x_terminal_fallback_not_changed",()->{try(F f=new F(X,true,false)){OneBackExit.enterBack(f.activity);try{check(!OneBackExit.tryTerminalBack(f.activity,0,false),"X terminal rule unchanged");}finally{OneBackExit.leaveNative();}preserved(f);}});
  test("exclude_even_silent_remove",()->{try(F f=new F(TRIP)){ActivityManager.AppTask task=ownTask(f,11);task.silentRemove=true;check(OneBackExit.tryBack(f.activity),"explicit exit accepted");check(task.excluded&&task.removeCalls==1,"silent/async removal still excludes card");}});
  test("exclude_failure_does_not_block_remove",()->{try(F f=new F(TRIP)){ActivityManager.AppTask task=ownTask(f,11);task.failExclude=true;check(OneBackExit.tryBack(f.activity),"exit still handled");check(task.removeCalls==1,"remove attempted after exclude error");}});
  test("remove_failure_still_excludes_and_finishes",()->{try(F f=new F(TRIP)){ActivityManager.AppTask task=ownTask(f,11);task.failRemove=true;check(OneBackExit.tryBack(f.activity),"exit handled");check(task.excluded&&task.removeCalls==1,"failed removal hidden");check(f.activity.removeCalls==1,"root fallback still attempted");}});
  test("all_own_tasks_not_foreign",()->{try(F f=new F(TRIP)){ActivityManager.AppTask first=ownTask(f,11),second=ownTask(f,12);ActivityManager.RecentTaskInfo info=new ActivityManager.RecentTaskInfo();info.taskId=13;info.baseActivity=new ComponentName("com.example.browser","Browser");info.baseIntent=new Intent().setComponent(new ComponentName("com.example.browser","Browser"));ActivityManager.AppTask foreign=new ActivityManager.AppTask(info);f.tasks.tasks.add(foreign);check(OneBackExit.tryBack(f.activity),"explicit exit");check(first.excluded&&second.excluded&&first.removeCalls==1&&second.removeCalls==1,"all own cards removed");check(!foreign.excluded&&foreign.removeCalls==0,"other app intact");}});
  test("actual_task_base_not_stale_intent",()->{try(F f=new F(TRIP)){ActivityManager.AppTask task=ownTask(f,11);task.info.baseIntent=new Intent();check(OneBackExit.tryBack(f.activity),"exit handles missing launch component");check(task.excluded&&task.removeCalls==1,"actual root identifies owner");}});
  test("new_instance_can_exit_again",()->{try(F f=new F(TRIP)){check(OneBackExit.tryBack(f.activity),"first exits");}try(F next=new F(TRIP)){check(OneBackExit.tryBack(next.activity),"reopened instance can exit");check(next.activity.removeCalls==1,"reopened task removed");}});

  for(final String pkg:new String[]{TT,JP})for(final String label:new String[]{"メッセージ","プロフィール","Inbox","Profile"}) {
   test("tt_tab_home_"+pkg+"_"+label,()->{try(F f=new F(pkg)){
    f.home.description=label;View target=tiktokTarget(f);ActivityManager.AppTask task=ownTask(f,11);f.draw();
    check(ExitScreens.isTikTokRootTab(f.activity)&&!ExitScreens.shouldExit(f.activity),"root tab classified as navigation");
    check(OneBackExit.key(f.activity,down()),"navigation down captured");check(target.clickCalls==0,"do not click on down");
    check(OneBackExit.key(f.activity,up()),"release navigates");check(target.clickCalls==1&&target.selected,"real Home click");
    check(!task.excluded&&task.removeCalls==0,"moving to home does not remove recents");preserved(f);
    check(!OneBackExit.key(f.activity,up()),"duplicate up not exit");preserved(f);
    check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"next distinct Back exits home");
    check(task.excluded&&task.removeCalls==1,"home exit removes recents");
   }});
  }
  test("tt_callback_home_once",()->{try(F f=new F(TT)){f.home.description="プロフィール";View target=tiktokTarget(f);f.draw();f.dispatcher().invokeBack();check(target.clickCalls==1&&target.selected,"system callback clicks Home once");preserved(f);}});
  test("tt_missing_home_never_exits",()->{try(F f=new F(TT)){f.home.description="プロフィール";check(OneBackExit.tryBack(f.activity),"confirmed target tab consumes Back even if Home is unknown");preserved(f);}});
  test("tt_disabled_home_never_exits",()->{try(F f=new F(TT)){f.home.description="プロフィール";View target=tiktokTarget(f);target.enabled=false;check(OneBackExit.tryBack(f.activity)&&target.clickCalls==0,"disabled Home not clicked");preserved(f);}});
  test("tt_hidden_home_never_exits",()->{try(F f=new F(TT)){f.home.description="メッセージ";View target=tiktokTarget(f);target.hidden=true;check(OneBackExit.tryBack(f.activity)&&target.clickCalls==0,"hidden Home not clicked");preserved(f);}});
  test("tt_ambiguous_home_no_guess",()->{try(F f=new F(TT)){f.home.description="プロフィール";View a=tiktokTarget(f),b=tiktokTarget(f);check(OneBackExit.tryBack(f.activity)&&a.clickCalls==0&&b.clickCalls==0,"duplicate target not guessed");preserved(f);}});
  test("tt_home_click_parent",()->{try(F f=new F(JP)){f.home.description="プロフィール";View target=tiktokTarget(f);f.root.removeView(target);ViewGroup parent=new ViewGroup(f.activity);parent.bounds=target.bounds;parent.clickable=true;parent.clickAction=target.clickAction;target.clickable=false;parent.addView(target);f.root.addView(parent);check(OneBackExit.tryBack(f.activity)&&parent.clickCalls==1&&target.selected,"click actual parent tab");preserved(f);}});
  test("tt_target_changed_before_key_up",()->{try(F f=new F(TT)){f.home.description="プロフィール";View target=tiktokTarget(f);check(OneBackExit.key(f.activity,down()),"navigation armed");target.clickAction.run();check(OneBackExit.key(f.activity,up()),"paired up consumed");check(target.clickCalls==0,"stale nav sequence not a new exit");preserved(f);}});
  test("tt_target_cancelled_key",()->{try(F f=new F(TT)){f.home.description="メッセージ";View target=tiktokTarget(f);check(OneBackExit.key(f.activity,down()),"navigation armed");check(OneBackExit.key(f.activity,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,true)),"cancel consumed");check(target.clickCalls==0,"cancel no movement");preserved(f);}});
  test("tt_profile_fragment_backstack_native",()->{try(F f=new F(TT)){f.home.description="プロフィール";tiktokTarget(f);f.activity.fragments.count=1;check(OneBackExit.tryBack(f.activity),"nested detail routes through protected native Back");preserved(f);}});
  test("trip_async_move_after_native_returns",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);armTrip(f);check(TripBack.pending(f.activity),"successful native callback arms bounded token");f.root.focus(false);check(OneBackExit.tryTerminalBack(f.activity,2,true),"async move after focus loss caught");check(task.excluded&&task.removeCalls==1,"async terminal removes card");}});
  test("trip_async_finish_after_native_returns",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);armTrip(f);check(OneBackExit.tryTerminalBack(f.activity,1,false),"async finish caught");check(task.excluded&&task.removeCalls==1,"async finish removes card");}});
  test("trip_platform_background_stop",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);armTrip(f);f.root.focus(false);OneBackExit.onStopped(f.activity);Handler.drain();check(task.excluded&&task.removeCalls>=1,"Back stop cleans task without intercepted terminal call");}});
  test("trip_stop_without_back_keeps_recents",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);f.root.focus(false);OneBackExit.onStopped(f.activity);Handler.drain();check(!task.excluded&&task.removeCalls==0,"ordinary lifecycle stop untouched");preserved(f);}});
  test("trip_home_key_cancels_pending",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);armTrip(f);OneBackExit.onUserLeaveHint(f.activity);f.root.focus(false);OneBackExit.onStopped(f.activity);Handler.drain();check(!task.excluded&&task.removeCalls==0,"Home key never causes recents removal");preserved(f);}});
  test("trip_user_input_cancels_pending",()->{try(F f=new F(TRIP,false,false)){armTrip(f);OneBackExit.onUserInteraction(f.activity);check(!OneBackExit.tryTerminalBack(f.activity,1,false),"new interaction cancels old Back");preserved(f);}});
  test("trip_pending_expiry",()->{try(F f=new F(TRIP,false,false)){armTrip(f);SystemClock.time+=1501;check(!OneBackExit.tryTerminalBack(f.activity,2,true),"old Back cannot cause later unrelated exit");preserved(f);}});
  test("trip_new_activity_cancels_pending",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);armTrip(f);Activity child=new Activity(f.app,11,false);OneBackExit.onResume(child);OneBackExit.onStopped(f.activity);Handler.drain();check(!TripBack.pending(f.activity)&&!task.excluded,"in-app navigation cancels pending exit");OneBackExit.onDestroy(child);preserved(f);}});
  test("trip_config_change_not_exit",()->{try(F f=new F(TRIP,false,false)){armTrip(f);f.activity.changingConfigurations=true;OneBackExit.onStopped(f.activity);Handler.drain();check(!TripBack.pending(f.activity),"rotation invalidates ticket");preserved(f);}});
  test("trip_ime_native_return_not_armed",()->{try(F f=new F(TRIP,false,false)){f.root.insets.ime=true;armTrip(f);check(!TripBack.pending(f.activity),"keyboard Back cannot become async exit");preserved(f);}});
  test("trip_exception_return_not_armed",()->{try(F f=new F(TRIP,false,false)){OneBackExit.enterBack(f.activity);OneBackExit.leaveNative();check(!TripBack.pending(f.activity),"only successful return commits ticket");preserved(f);}});
  test("trip_native_back_observer_unknown_ui",()->{try(F f=new F(TRIP,false,false)){check(f.dispatcher().count()==1,"unknown home still has public dispatcher observer");f.dispatcher().invokeBack();check(f.activity.nativeBackCalls==1&&TripBack.pending(f.activity),"one native call and observer armed");preserved(f);}});
  test("trip_forwarded_callback_arms_observer",()->{try(F f=new F(TRIP,false,false)){OnBackInvokedCallback cb=()->{};NativeBack.register(f.dispatcher(),0,cb);f.dispatcher().invokeBack();check(TripBack.pending(f.activity),"wrapped app callback arms observer");NativeBack.unregister(f.dispatcher(),cb);preserved(f);}});
  test("trip_failed_callback_does_not_arm",()->{try(F f=new F(TRIP,false,false)){OnBackInvokedCallback cb=()->{throw new IllegalStateException();};NativeBack.register(f.dispatcher(),0,cb);try{f.dispatcher().invokeBack();}catch(IllegalStateException expected){}check(!TripBack.pending(f.activity),"exception cannot leave async ticket");NativeBack.unregister(f.dispatcher(),cb);preserved(f);}});
  test("trip_retry_only_original_own_task_ids",()->{try(F f=new F(TRIP)){ActivityManager.AppTask task=ownTask(f,11);task.silentRemove=true;check(OneBackExit.tryBack(f.activity),"explicit exit");ActivityManager.AppTask added=ownTask(f,99);Handler.drain();check(task.removeCalls==4&&task.excluded,"three bounded retries for original task");check(added.removeCalls==0&&!added.excluded,"new task ID untouched");}});
  test("trip_reopen_cancels_retry",()->{try(F f=new F(TRIP)){ActivityManager.AppTask task=ownTask(f,11);task.silentRemove=true;OneBackExit.tryBack(f.activity);Activity next=new Activity(f.app,11,true);OneBackExit.onResume(next);Handler.drain();check(task.removeCalls==1,"resumed app not killed by stale retry");OneBackExit.onDestroy(next);}});
  test("trip_foreign_task_id_reuse_protected",()->{try(F f=new F(TRIP)){ActivityManager.AppTask task=ownTask(f,11);task.silentRemove=true;OneBackExit.tryBack(f.activity);task.info.baseActivity=new ComponentName("com.example.foreign","Main");Handler.drain();check(task.removeCalls==1,"retry validates ownership, not task ID alone");}});
  test("trip_same_instance_resume_cancels_retry",()->{try(F f=new F(TRIP)){ActivityManager.AppTask task=ownTask(f,11);task.silentRemove=true;OneBackExit.tryBack(f.activity);OneBackExit.onResume(f.activity);Handler.drain();check(task.removeCalls==1,"reopened same activity protected from stale retry");check(OneBackExit.tryBack(f.activity),"reopened same activity can handle a new exit");}});
  test("trip_screen_off_stop_preserved",()->{try(F f=new F(TRIP,false,false)){ActivityManager.AppTask task=ownTask(f,11);PowerManager power=new PowerManager();f.app.setService(Context.POWER_SERVICE,power);armTrip(f);power.interactive=false;OneBackExit.onStopped(f.activity);Handler.drain();check(!TripBack.pending(f.activity)&&task.removeCalls==0,"screen off is not a Back exit");preserved(f);}});
  test("trip_lock_stop_preserved",()->{try(F f=new F(TRIP,false,false)){KeyguardManager lock=new KeyguardManager();f.app.setService(Context.KEYGUARD_SERVICE,lock);armTrip(f);lock.locked=true;OneBackExit.onStopped(f.activity);Handler.drain();check(!TripBack.pending(f.activity),"locking phone cancels pending Back");preserved(f);}});
  for(final String pkg:new String[]{TT,JP})for(final String name:new String[]{"comment_panel_container","channel_page_container","user_profile_container","search_results_container","settings_container","chat_detail_container","share_sheet_container","live_play_container"}) {
   test("tt114_nonhome_"+pkg+"_"+name,()->{try(F f=new F(pkg)){
    View panel=marker(f,name,new Rect(0,350,1080,2200));ActivityManager.AppTask task=ownTask(f,11);
    f.home.clickAction=()->{f.root.removeView(panel);f.home.selected=true;};
    f.activity.nativeBackAction=()->{check(OneBackExit.tryTerminalBack(f.activity,2,true),"native task background blocked while unwinding");f.root.removeView(panel);f.home.selected=true;};
    check(!TikTokScreen.inspect(f.activity).home,"background Home is not foreground evidence");
    check(OneBackExit.tryBack(f.activity),"non-home Back consumed");Handler.drain();
    check(TikTokScreen.inspect(f.activity).home,"arrived at actual Home");check(task.removeCalls==0&&!task.excluded,"navigation preserves app task");preserved(f);
    check(OneBackExit.tryBack(f.activity),"duplicate callback consumed");check(task.removeCalls==0,"same operation cannot exit newly reached Home");
    check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"fresh paired Back exits Home");check(task.excluded&&task.removeCalls==1,"new Home Back removes Recents");
   }});
  }
  test("tt114_home_needs_foreground_feed",()->{try(F f=new F(TT)){f.root.removeView(f.root.getChildAt(1));check(HomeScreen.isHome(f.activity),"legacy selected tab evidence exists");check(!TikTokScreen.inspect(f.activity).home,"tab alone cannot allow exit");check(OneBackExit.tryBack(f.activity),"unknown consumed safely");preserved(f);}});
  test("tt114_conflicting_selected_tabs",()->{try(F f=new F(TT)){View profile=marker(f,"tab_profile",new Rect(850,1990,1050,2170));profile.description="プロフィール";profile.selected=true;profile.clickable=true;check(!TikTokScreen.inspect(f.activity).home,"profile selection overrides stale Home selection");check(OneBackExit.tryBack(f.activity),"conflict navigates not exits");preserved(f);}});
  test("tt114_profile_fragment_real_home_click",()->{try(F f=new F(TT)){f.home.description="プロフィール";SettingsFragment fragment=new SettingsFragment();f.activity.fragments.fragments.add(fragment);View target=tiktokTarget(f);target.clickAction=()->{f.activity.fragments.fragments.clear();f.home.selected=false;target.selected=true;};check(OneBackExit.tryBack(f.activity),"non-root-tab recognition still routes Home");Handler.drain();check(target.clickCalls==1&&TikTokScreen.inspect(f.activity).home,"real Home transition verified");preserved(f);}});
  test("tt114_comment_native_callback_unwinds",()->{try(F f=new F(JP)){View panel=marker(f,"comment_panel_container",new Rect(0,500,1080,2200));final int[] calls={0};OnBackInvokedCallback cb=()->{calls[0]++;check(OneBackExit.tryTerminalBack(f.activity,0,false),"platform task-root back blocked");f.root.removeView(panel);};NativeBack.register(f.dispatcher(),0,cb);f.dispatcher().invokeBack();Handler.drain();check(calls[0]==1&&TikTokScreen.inspect(f.activity).home,"one original callback closes comments");preserved(f);NativeBack.unregister(f.dispatcher(),cb);}});
  for(final int kind:new int[]{0,1,2,3,4})test("tt114_native_terminal_kind_"+kind,()->{try(F f=new F(TT,true,false)){check(!OneBackExit.tryTerminalBack(f.activity,kind,true),"ordinary non-Back call unchanged");OneBackExit.enterBack(f.activity);try{check(OneBackExit.tryTerminalBack(f.activity,kind,true),"genuine Back root terminal guarded");OneBackExit.afterNative();}finally{OneBackExit.leaveNative();}check(OneBackExit.tryTerminalBack(f.activity,kind,true),"delayed terminal within same transaction guarded");preserved(f);OneBackExit.onUserLeaveHint(f.activity);check(!OneBackExit.tryTerminalBack(f.activity,kind,true),"Home key clears stale transaction");}});
  test("tt114_child_finish_not_task_exit",()->{try(F f=new F(TT)){Activity child=new Activity(f.app,11,false);OneBackExit.enterBack(child);try{check(!OneBackExit.tryTerminalBack(child,1,false),"child finish can pop a page");check(OneBackExit.tryTerminalBack(child,4,false),"child affinity cannot end entire app");check(OneBackExit.tryTerminalBack(child,3,false),"child task removal suppressed");check(OneBackExit.tryTerminalBack(child,2,true),"child backgrounding suppressed");}finally{OneBackExit.leaveNative();OneBackExit.onDestroy(child);}preserved(f);}});
  test("tt114_native_fragment_pop_allowed",()->{try(F f=new F(TT)){f.activity.fragments.count=1;OneBackExit.enterBack(f.activity);try{check(!OneBackExit.tryTerminalBack(f.activity,0,false),"real platform fragment stack pop is not app exit");}finally{OneBackExit.leaveNative();}preserved(f);}});
  test("tt114_app_task_removal_scope",()->{try(F f=new F(TT,true,false)){ActivityManager.AppTask own=ownTask(f,11),other=ownTask(f,99);TikTokBack.protect(f.activity);check(OneBackExit.tryTaskTerminal(own),"same task removal intercepted");check(!OneBackExit.tryTaskTerminal(other),"different task not intercepted");own.info.baseActivity=new ComponentName("com.example.foreign","Main");check(!OneBackExit.tryTaskTerminal(own),"foreign task with same number not intercepted");preserved(f);}});
  test("tt114_unknown_activity_never_task_exit",()->{try(F f=new F(TT,false,false)){f.activity.nativeBackAction=()->{if(!OneBackExit.tryTerminalBack(f.activity,1,false))f.activity.finish();};check(OneBackExit.tryBack(f.activity),"unknown Activity handled without guessed launcher");Handler.drain();check(f.activity.nativeBackCalls==1,"one bounded native attempt");preserved(f);}});
  test("tt114_rejected_transition_stays_open",()->{try(F f=new F(TT)){f.home.description="プロフィール";View target=tiktokTarget(f);target.clickAction=()->{};check(OneBackExit.tryBack(f.activity),"handled");Handler.drain();check(target.clickCalls==1,"not an unbounded click loop");check(!TikTokScreen.inspect(f.activity).home,"click return value is not Home confirmation");preserved(f);}});
  test("tt114_prediction_nonhome_to_home_same_gesture",()->{try(F f=new F(TT,true,false)){OnBackAnimationCallback cb=(OnBackAnimationCallback)f.dispatcher().top();cb.onBackStarted(new BackEvent());f.home.selected=true;cb.onBackInvoked();preserved(f);cb.onBackStarted(new BackEvent());cb.onBackInvoked();check(f.activity.removeCalls==1,"separate Home gesture may exit");}});
  test("tt114_prediction_home_to_comment_same_gesture",()->{try(F f=new F(TT)){OnBackAnimationCallback cb=(OnBackAnimationCallback)f.dispatcher().top();cb.onBackStarted(new BackEvent());View panel=marker(f,"comment_panel_container",new Rect(0,500,1080,2200));f.activity.nativeBackAction=()->f.root.removeView(panel);cb.onBackInvoked();Handler.drain();check(TikTokScreen.inspect(f.activity).home,"comment closed");preserved(f);}});
  test("tt114_prediction_cancel_does_not_navigate",()->{try(F f=new F(TT)){f.home.description="プロフィール";View target=tiktokTarget(f);OnBackAnimationCallback cb=(OnBackAnimationCallback)f.dispatcher().top();cb.onBackStarted(new BackEvent());cb.onBackCancelled();Handler.drain();check(target.clickCalls==0,"cancel never clicks Home");preserved(f);}});
  test("tt114_wrapped_prediction_captures_start",()->{try(F f=new F(TT,true,false)){final int[] called={0};OnBackAnimationCallback original=new OnBackAnimationCallback(){public void onBackStarted(BackEvent e){}public void onBackProgressed(BackEvent e){}public void onBackCancelled(){}public void onBackInvoked(){called[0]++;}};NativeBack.register(f.dispatcher(),0,original);OnBackAnimationCallback wrapped=(OnBackAnimationCallback)f.dispatcher().top();wrapped.onBackStarted(new BackEvent());f.home.selected=true;wrapped.onBackInvoked();check(called[0]==0,"original terminal callback not called after protected navigation");preserved(f);NativeBack.unregister(f.dispatcher(),original);}});
  test("tt114_ime_back_is_not_exit",()->{try(F f=new F(TT)){f.root.insets.ime=true;check(!OneBackExit.key(f.activity,down()),"IME down delegates");OneBackExit.enterBack(f.activity);try{f.root.insets.ime=false;check(OneBackExit.tryTerminalBack(f.activity,1,false),"IME completion cannot fall through to root finish");}finally{OneBackExit.leaveNative();}check(!OneBackExit.key(f.activity,up()),"same release cannot exit");preserved(f);}});
  test("tt114_comment_dialog_closes_then_home",()->{try(F f=new F(JP)){f.home.description="プロフィール";View target=tiktokTarget(f);Dialog d=f.dialog();ViewGroup dialogRoot=(ViewGroup)d.getWindow().getDecorView();View comments=new View(f.activity);comments.id=++markerId;android.content.res.Resources.IDS.put(comments.id,"comment_dialog_container");comments.bounds=new Rect(0,500,1080,2200);dialogRoot.addView(comments);OneBackExit.dialogShown(d);Handler.drain();check(OneBackExit.tryDialogBack(d),"comment dialog routed");Handler.drain();check(d.nativeBackCalls==1,"normal dialog close executed once");check(target.selected&&target.clickCalls==1,"underlying profile goes Home");preserved(f);}});
  test("tt114_unknown_dialog_preserves_native_confirmation",()->{try(F f=new F(TT)){Dialog d=f.dialog();check(!OneBackExit.tryDialogBack(d),"unknown confirmation dialog keeps original Back");check(d.nativeBackCalls==0,"not force-dismissed");check(OneBackExit.tryTerminalBack(f.activity,4,false),"dialog fallback still cannot end app");preserved(f);}});
  test("tt114_input_editor_not_force_discarded",()->{try(F f=new F(TT)){f.home.description="プロフィール";View target=tiktokTarget(f),editor=new View(f.activity);editor.editor=true;f.root.focusView=editor;check(!OneBackExit.tryBack(f.activity),"editing retains original input Back");check(target.clickCalls==0,"no unprompted navigation away from draft");preserved(f);}});
  test("tt114_key_repeat_no_early_exit",()->{try(F f=new F(JP)){f.home.description="プロフィール";View target=tiktokTarget(f);check(OneBackExit.key(f.activity,down()),"down captured");check(target.clickCalls==0,"down not navigation");check(OneBackExit.key(f.activity,up()),"up navigates");check(!OneBackExit.key(f.activity,up()),"duplicate up ignored");preserved(f);}});
  test("tt114_interaction_cancels_delayed_guard",()->{try(F f=new F(TT)){TikTokBack.protect(f.activity);check(OneBackExit.tryTerminalBack(f.activity,1,false),"initial guard");OneBackExit.onUserInteraction(f.activity);check(!OneBackExit.tryTerminalBack(f.activity,1,false),"different user action invalidates old Back");preserved(f);}});
  test("tt114_guard_expires",()->{try(F f=new F(TT)){TikTokBack.protect(f.activity);SystemClock.time+=1700;check(!OneBackExit.tryTerminalBack(f.activity,1,false),"expired transaction does not block unrelated later actions");preserved(f);}});
  test("tt114_other_apps_new_terminal_kinds_unchanged",()->{for(String pkg:new String[]{X,IG,TRIP})try(F f=new F(pkg)){check(!OneBackExit.tryTerminalBack(f.activity,3,false),"new TikTok direct-removal hook inactive for "+pkg);preserved(f);}});
  test("tt114_plain_callback_predictive_capture",()->{try(F f=new F(TT,true,false)){OnBackInvokedCallback plain=()->{};NativeBack.register(f.dispatcher(),0,plain);check(f.dispatcher().top() instanceof OnBackAnimationCallback,"plain TikTok callback also captures gesture start");OnBackAnimationCallback wrapped=(OnBackAnimationCallback)f.dispatcher().top();wrapped.onBackStarted(new BackEvent());f.home.selected=true;wrapped.onBackInvoked();preserved(f);NativeBack.unregister(f.dispatcher(),plain);}});
  test("tt114_delayed_programmatic_back_no_exit",()->{try(F f=new F(TT)){f.home.description="プロフィール";View target=tiktokTarget(f);check(OneBackExit.tryBack(f.activity)&&target.selected,"first Back reaches Home");SystemClock.time+=700;check(OneBackExit.tryBack(f.activity),"delayed programmatic duplicate consumed");preserved(f);check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"fresh physical Back exits");check(f.activity.removeCalls==1,"only fresh event removes task");}});
  test("tt114_textview_home_positive",()->{try(F f=new F(TT)){f.root.removeView(f.home);android.widget.TextView home=new android.widget.TextView(f.activity);home.text="ホーム";home.bounds=new Rect(0,1990,190,2170);home.selected=true;home.clickable=true;f.root.addView(home);check(TikTokScreen.inspect(f.activity).home,"selected Home TextView plus foreground feed recognized");check(OneBackExit.tryBack(f.activity)&&f.activity.removeCalls==1,"confirmed text tab Home can exit");}});
  test("tt114_child_channel_returns_existing_main",()->{try(F f=new F(TT)){
   f.home.description="プロフィール";View target=tiktokTarget(f);Activity child=new Activity(f.app,11,false);OneBackExit.install(child);child.getWindow().attach();f.root.focus(false);child.getWindow().getDecorView().focus(true);OneBackExit.onResume(child);Handler.drain();
   child.startAction=()->{child.getWindow().getDecorView().focus(false);f.root.focus(true);OneBackExit.onResume(f.activity);};
   check(OneBackExit.tryBack(child),"child screen handled");Handler.drain();
   check(child.startCalls==1&&child.lastStart.getComponent().getPackageName().equals(TT),"same app main requested once");
   check(child.lastStart.flags==(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP),"reuse same task without a new launcher task");
   check(target.selected&&target.clickCalls==1,"existing main profile changes to Home");preserved(f);check(child.removeCalls==0,"child does not remove task");
   OneBackExit.onDestroy(child);child.destroyed=true;child.getWindow().getDecorView().detach();
  }});
  test("tt114_comment_dialog_system_callback",()->{try(F f=new F(TT)){Dialog d=f.dialog();View comments=new View(f.activity);comments.id=++markerId;android.content.res.Resources.IDS.put(comments.id,"comment_dialog_container");comments.bounds=new Rect(0,500,1080,2200);((ViewGroup)d.getWindow().getDecorView()).addView(comments);OneBackExit.dialogShown(d);Handler.drain();check(d.getWindow().dispatcher().count()==1,"comment window has its own protected callback");d.getWindow().dispatcher().invokeBack();Handler.drain();check(d.nativeBackCalls==1,"dialog-specific Back closes comments");preserved(f);}});
  test("tt114_comment_dialog_late_app_callback",()->{try(F f=new F(TT)){Dialog d=f.dialog();View comments=new View(f.activity);comments.id=++markerId;android.content.res.Resources.IDS.put(comments.id,"comment_dialog_container");comments.bounds=new Rect(0,500,1080,2200);((ViewGroup)d.getWindow().getDecorView()).addView(comments);OneBackExit.dialogShown(d);Handler.drain();final int[] accidental={0};OnBackInvokedCallback original=()->{accidental[0]++;f.activity.finish();};NativeBack.register(d.getWindow().dispatcher(),0,original);d.getWindow().dispatcher().invokeBack();Handler.drain();check(accidental[0]==0&&d.nativeBackCalls==1,"late dialog callback cannot win and finish underlying app");preserved(f);NativeBack.unregister(d.getWindow().dispatcher(),original);}});

  for(final String pkg:new String[]{TT,JP}) {
   test("tt115_profile_parent_click_"+pkg,()->{try(F f=new F(pkg)){
    ViewGroup target=new ViewGroup(f.activity);target.clickable=true;target.bounds=new Rect(0,10,110,130);
    View icon=new View(f.activity);icon.description="戻る";icon.bounds=new Rect(10,20,100,120);target.addView(icon);f.root.addView(target);
    target.clickAction=()->f.root.removeView(target);
    check(TikTokScreen.inspect(f.activity).closeTarget==target,"icon resolves clickable parent");
    check(OneBackExit.tryBack(f.activity),"Back handled");Handler.drain();check(target.clickCalls==1,"actual app listener called exactly once");
    check(TikTokScreen.inspect(f.activity).home,"profile unwound to Home");preserved(f);
   }});
   test("tt115_profile_resource_back_"+pkg,()->{try(F f=new F(pkg)){
    View arrow=marker(f,"title_bar_back",new Rect(0,20,100,120));arrow.clickable=true;arrow.clickAction=()->f.root.removeView(arrow);
    check(TikTokScreen.inspect(f.activity).closeTarget==arrow,"resource semantic recognized without label");
    check(OneBackExit.tryBack(f.activity),"Back handled");Handler.drain();check(arrow.clickCalls==1,"resource control activated");preserved(f);
   }});
   test("tt115_nonroot_native_profile_back_"+pkg,()->{
    Application app=new Application(pkg);app.setService(Context.ACTIVITY_SERVICE,new ActivityManager());Activity child=new Activity(app,71,false);
    OneBackExit.install(child);child.getWindow().attach();child.getWindow().getDecorView().focus(true);OneBackExit.onResume(child);Handler.drain();
    child.nativeBackAction=()->{if(!OneBackExit.tryTerminalBack(child,0,false))child.finish();};
    check(OneBackExit.tryBack(child),"child Back handled");Handler.drain();
    check(child.finishCalls==1&&child.removeCalls==0,"native child finish allowed, task not removed");
    OneBackExit.onDestroy(child);child.destroyed=true;child.getWindow().getDecorView().detach();
   });
   test("tt115_support_stack_not_frozen_"+pkg,()->{
    Application app=new Application(pkg);app.setService(Context.ACTIVITY_SERVICE,new ActivityManager());SupportMain a=new SupportMain(app,72,true);
    a.support.count=1;OneBackExit.install(a);a.getWindow().attach();a.getWindow().getDecorView().focus(true);OneBackExit.onResume(a);Handler.drain();
    OneBackExit.enterBack(a);try{check(OneBackExit.tryTerminalBack(a,0,false),"root fallback handled by support stack pop");}finally{OneBackExit.leaveNative();}
    check(a.support.popCalls==1&&a.support.count==0,"exactly one public AndroidX stack pop");check(a.finishCalls==0&&a.removeCalls==0,"root preserved");
    OneBackExit.onDestroy(a);a.destroyed=true;a.getWindow().getDecorView().detach();
   });
  }
  test("tt115_support_saved_state_not_mutated",()->{
   Application app=new Application(TT);SupportMain a=new SupportMain(app,73,true);a.support.count=1;a.support.saved=true;
   TikTokBack.protect(a);check(OneBackExit.tryTerminalBack(a,0,false),"unsafe root fallback still blocked");check(a.support.popCalls==0&&a.support.count==1,"saved stack not mutated");TikTokBack.cancel(a);
  });
  test("tt115_nested_stack_pop",()->{try(F f=new F(TT)){
   ChildFragment top=new ChildFragment();top.child.count=1;f.activity.fragments.primary=top;f.activity.fragments.fragments.add(top);
   TikTokBack.protect(f.activity);check(OneBackExit.tryTerminalBack(f.activity,0,false),"nested stack handled");check(top.child.popCalls==1,"top child popped");preserved(f);
  }});
  test("tt115_disabled_back_not_clicked",()->{try(F f=new F(TT)){
   View arrow=marker(f,"back_button",new Rect(0,20,100,120));arrow.enabled=false;arrow.clickable=true;
   check(TikTokScreen.inspect(f.activity).closeTarget==null,"disabled target ignored");OneBackExit.tryBack(f.activity);check(arrow.clickCalls==0,"disabled control never activated");
  }});
  for(final String label:new String[]{"ホーム","リール"})for(final String id:new String[]{"layout_comment_thread","comments_container","clips_comments_sheet","comment_thread_recycler_view","comments_panel_root","bottom_sheet_container"}) {
   test("ig115_layer_"+label+"_"+id,()->{try(F f=new F(IG)){f.home.description=label;View panel=igPanel(f,id);closedIG(f,panel);}});
  }
  test("ig115_obfuscated_fragment_module",()->{try(F f=new F(IG)){
   ObfuscatedCommentsFragment page=new ObfuscatedCommentsFragment();f.activity.fragments.fragments.add(page);
   f.activity.nativeBackAction=()->f.activity.fragments.fragments.remove(page);
   check(!ExitScreens.shouldExit(f.activity),"module comments veto");check(OneBackExit.tryBack(f.activity),"module layer Back consumed");check(f.activity.nativeBackCalls==1,"native close forwarded");preserved(f);
  }});
  test("ig115_accessibility_header",()->{try(F f=new F(IG)){
   android.widget.TextView title=new android.widget.TextView(f.activity);title.text="コメント 35";title.bounds=new Rect(200,650,880,750);f.root.addView(title);
   closedIG(f,title);
  }});
  test("ig115_small_reel_comment_icon_not_layer",()->{try(F f=new F(IG)){
   f.home.description="リール";View icon=marker(f,"clips_comment_button",new Rect(930,1300,1030,1420));icon.description="コメント 35";icon.clickable=true;
   check(!InstagramBack.inspect(f.activity).blocked,"small icon is not open comments");check(OneBackExit.tryBack(f.activity)&&f.activity.removeCalls==1,"Reels exit maintained");
  }});
  test("ig115_hidden_or_transparent_comments_ignored",()->{try(F f=new F(IG)){
   View a=igPanel(f,"comments_container"),b=igPanel(f,"layout_comment_thread");a.hidden=true;b.alpha=0;
   check(!InstagramBack.inspect(f.activity).blocked,"hidden views cannot block exit");check(OneBackExit.tryBack(f.activity)&&f.activity.removeCalls==1,"plain Home exits");
  }});
  test("ig115_key_cancel_preserves_comments",()->{try(F f=new F(IG)){
   View panel=igPanel(f,"layout_comment_thread");f.activity.nativeBackAction=()->f.root.removeView(panel);
   check(OneBackExit.key(f.activity,down()),"key captures layer");KeyEvent cancel=new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,true);check(OneBackExit.key(f.activity,cancel),"canceled key consumed");
   check(f.activity.nativeBackCalls==0&&InstagramBack.inspect(f.activity).blocked,"cancel does not close comments");preserved(f);
  }});
  test("ig115_key_duplicate_after_comments",()->{try(F f=new F(IG)){
   View panel=igPanel(f,"layout_comment_thread");f.activity.nativeBackAction=()->f.root.removeView(panel);
   check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"physical key collapses comments");
   check(!OneBackExit.key(f.activity,up()),"duplicate release cannot exit");check(f.activity.nativeBackCalls==1,"one collapse");preserved(f);
  }});
  test("ig115_prediction_comments_removed_before_commit",()->{try(F f=new F(IG)){
   View panel=igPanel(f,"layout_comment_thread");f.draw();OnBackAnimationCallback callback=(OnBackAnimationCallback)f.dispatcher().top();
   callback.onBackStarted(new BackEvent());f.root.removeView(panel);callback.onBackInvoked();
   check(f.activity.nativeBackCalls==0,"gesture already collapsed layer; do not forward again");preserved(f);
  }});
  test("ig115_prediction_cancel",()->{try(F f=new F(IG)){
   igPanel(f,"layout_comment_thread");f.draw();OnBackAnimationCallback callback=(OnBackAnimationCallback)f.dispatcher().top();callback.onBackStarted(new BackEvent());callback.onBackCancelled();
   check(f.activity.nativeBackCalls==0,"canceled gesture does not close or exit");preserved(f);
  }});
  test("ig115_prediction_home_to_comments",()->{try(F f=new F(IG)){
   OnBackAnimationCallback callback=(OnBackAnimationCallback)f.dispatcher().top();callback.onBackStarted(new BackEvent());View panel=igPanel(f,"layout_comment_thread");
   f.activity.nativeBackAction=()->f.root.removeView(panel);callback.onBackInvoked();check(f.activity.nativeBackCalls==1,"new layer takes precedence");preserved(f);
  }});
  test("ig115_keyboard_before_comments",()->{try(F f=new F(IG)){
   View panel=igPanel(f,"layout_comment_thread");f.root.insets.ime=true;check(!OneBackExit.key(f.activity,down()),"IME down delegated");
   OneBackExit.enterBack(f.activity);try{f.root.insets.ime=false;check(OneBackExit.tryTerminalBack(f.activity,1,false),"IME event cannot finish Activity");}finally{OneBackExit.leaveNative();}
   check(!OneBackExit.key(f.activity,up()),"same IME release cannot exit");
   f.activity.nativeBackAction=()->f.root.removeView(panel);check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"next Back collapses comments");check(f.activity.nativeBackCalls==1,"one native close");preserved(f);
  }});
  for(final int kind:new int[]{0,1,2,3,4})test("ig115_layer_blocks_terminal_"+kind,()->{try(F f=new F(IG)){
   View panel=igPanel(f,"layout_comment_thread");f.activity.nativeBackAction=()->{check(OneBackExit.tryTerminalBack(f.activity,kind,false),"terminal blocked");f.root.removeView(panel);};
   OneBackExit.tryBack(f.activity);check(OneBackExit.tryTerminalBack(f.activity,kind,false),"same transaction remains protected after collapse");preserved(f);
  }});
  test("ig115_apptask_guard_scoped",()->{try(F f=new F(IG)){
   igPanel(f,"layout_comment_thread");InstagramBack.protect(f.activity);ActivityManager.AppTask own=ownTask(f,11),other=ownTask(f,14);
   check(OneBackExit.tryTaskTerminal(own),"origin task protected");check(!OneBackExit.tryTaskTerminal(other),"different task untouched");preserved(f);
  }});
  test("ig115_layer_close_control",()->{try(F f=new F(IG)){
   ViewGroup panel=new ViewGroup(f.activity);panel.id=++markerId;android.content.res.Resources.IDS.put(panel.id,"comments_container");panel.bounds=new Rect(0,500,1080,2200);
   View close=new View(f.activity);close.description="閉じる";close.clickable=true;close.bounds=new Rect(930,520,1030,620);panel.addView(close);f.root.addView(panel);close.clickAction=()->f.root.removeView(panel);
   OneBackExit.tryBack(f.activity);check(close.clickCalls==1&&f.activity.nativeBackCalls==0,"layer's actual close listener used");preserved(f);
  }});
  test("ig115_reel_back_arrow_not_layer_close",()->{try(F f=new F(IG)){
   f.home.description="リール";View arrow=marker(f,"back_button",new Rect(0,30,100,140));arrow.clickable=true;View panel=igPanel(f,"layout_comment_thread");f.activity.nativeBackAction=()->f.root.removeView(panel);
   OneBackExit.tryBack(f.activity);check(arrow.clickCalls==0&&f.activity.nativeBackCalls==1,"background Reels back is not clicked");preserved(f);
  }});
  test("ig115_comment_dialog",()->{try(F f=new F(IG)){
   f.home.description="リール";Dialog dialog=f.dialog();View panel=new View(f.activity);panel.id=++markerId;android.content.res.Resources.IDS.put(panel.id,"layout_comment_thread");panel.bounds=new Rect(0,500,1080,2200);((ViewGroup)dialog.getWindow().getDecorView()).addView(panel);
   OneBackExit.dialogShown(dialog);Handler.drain();dialog.getWindow().dispatcher().invokeBack();Handler.drain();
   check(dialog.nativeBackCalls==1,"dialog native close once");check(!dialog.getWindow().getDecorView().isAttachedToWindow(),"dialog closed");preserved(f);
  }});
  test("ig115_late_callback_collapses_not_exits",()->{try(F f=new F(IG)){
   View panel=igPanel(f,"layout_comment_thread");OnBackInvokedCallback nativeCb=()->{check(OneBackExit.tryTerminalBack(f.activity,3,false),"late callback terminal blocked");f.root.removeView(panel);};
   NativeBack.register(f.dispatcher(),0,nativeCb);f.dispatcher().invokeBack();check(!InstagramBack.inspect(f.activity).blocked,"native callback collapsed comments");preserved(f);NativeBack.unregister(f.dispatcher(),nativeCb);
  }});
  test("ig115_guard_cancels_and_expires",()->{try(F f=new F(IG)){
   igPanel(f,"layout_comment_thread");InstagramBack.protect(f.activity);check(OneBackExit.tryTerminalBack(f.activity,1,false),"initial guard");OneBackExit.onUserInteraction(f.activity);check(!OneBackExit.tryTerminalBack(f.activity,1,false),"unrelated interaction clears guard");
   InstagramBack.protect(f.activity);SystemClock.time+=1300;check(!OneBackExit.tryTerminalBack(f.activity,1,false),"bounded guard expires");preserved(f);
  }});
  Back116Regression.run();
  Back117InstagramTest.run();
  Back117TikTokTest.run();
  System.out.println("SUCCESS: "+cases+" cases, "+assertions+" assertions");
 }
}
