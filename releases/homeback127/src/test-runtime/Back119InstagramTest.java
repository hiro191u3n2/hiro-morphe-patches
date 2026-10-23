package app.hiro.oneback.runtime;
import android.app.*;import android.graphics.Rect;import android.view.*;import java.util.*;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Historical conservative classification tests used by Main/prediction. These
 * generic Page doubles cannot model reaching the exact X.8Zv native hook; its
 * authoritative contract is exercised separately by Back123NativeReelsTest. */
public final class Back119InstagramTest {
 public static final class Page {
  final Activity activity; final View view; final String module; final FragmentManager child=new FragmentManager();
  Page(Activity a,View v,String m){activity=a;view=v;module=m;}
  public Activity getActivity(){return activity;}public View getView(){return view;}
  public boolean isVisible(){return true;}public String getModuleName(){return module;}
  public FragmentManager getChildFragmentManager(){return child;}
 }
 static Page reels(F f){ViewGroup v=Back118InstagramTest.group(f,"root_clips_layout",new Rect(0,0,1080,2100),f.root);
  Back118InstagramTest.group(f,"clips_viewer_container",new Rect(0,0,1080,1990),v);
  Page p=new Page(f.activity,v,"clips_viewer_clips_tab");f.activity.fragments.fragments.add(p);return p;}
 static boolean nativeBack(F f,Page p){OneBackExit.enterBack(f.activity);try{return InstagramBack.nativeReelsTarget(p)==f.activity;}finally{OneBackExit.leaveNative();}}
 static void crowded(F f){ViewGroup old=new ViewGroup(f.activity);f.root.addView(old);for(int i=0;i<1801;i++)old.addView(new View(f.activity));}
 static Map<String,Test> scenarios(){Map<String,Test> tests=new LinkedHashMap<String,Test>();
  tests.put("native_reels_boundary_inside_native_scope_exits_once",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);ActivityManager.AppTask task=ownTask(f,11);
   check(nativeBack(f,p),"conservative predicate recognizes a plain Reels scene");
   check(!task.excluded&&task.removeCalls==0,"classification itself does not remove a task");preserved(f);
  }});
  tests.put("toolbar_handler_outside_android_back_scope_stays_native",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);check(!OneBackExit.tryInstagramReelsBack(p),"toolbar does not acquire Android Back authorization");preserved(f);
  }});
  tests.put("own_comment_sheet_stays_native",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);Back118InstagramTest.group(f,"comments_container",new Rect(0,700,1080,2100),(ViewGroup)p.view);
   check(!nativeBack(f,p),"native comments handling cannot be replaced with task exit");preserved(f);
  }});
  tests.put("same_back_comment_forwarding_never_exits_reels",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);View panel=Back118InstagramTest.group(f,"comments_container",new Rect(0,700,1080,2100),(ViewGroup)p.view);
   f.activity.nativeBackAction=()->{((ViewGroup)p.view).removeView(panel);check(!OneBackExit.tryInstagramReelsBack(p),"forwarded layer Back cannot enter exit fallback");};
   check(OneBackExit.tryBack(f.activity),"comments Back consumed");check(f.activity.nativeBackCalls==1,"one native close");preserved(f);
  }});
  tests.put("native_boundary_ime_preserved",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);f.root.insets.ime=true;check(!nativeBack(f,p),"IME Back stays native");preserved(f);
  }});
  tests.put("native_boundary_other_focused_window_preserved",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);f.dialog();check(!nativeBack(f,p),"separate focused dialog stays native");preserved(f);
  }});
  tests.put("native_boundary_detached_fragment_rejected",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);f.root.removeView(p.view);check(!nativeBack(f,p),"stale detached view cannot exit");preserved(f);
  }});
  tests.put("native_boundary_transparent_view_rejected",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);p.view.alpha=0;check(!nativeBack(f,p),"transparent target is not current Reels");preserved(f);
  }});
  tests.put("native_reels_own_view_survives_unrelated_large_hierarchy",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);crowded(f);check(!ExitScreens.shouldExit(f.activity),"old entire-scene scan cannot classify this hierarchy");
   check(nativeBack(f,p),"small valid own view supplies conservative identity despite unrelated content");preserved(f);
  }});
  tests.put("prediction_unknown_scan_same_native_reel_identity_exits",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);crowded(f);InstagramBack.predictionStart(f.activity);
   check(InstagramBack.action(f.activity)==0,"whole-scene start remains unknown");
   check(InstagramBack.exitPrediction(f.activity),"positively captured native Reel suppresses premature native pop animation");
   check(nativeBack(f,p),"same positively captured Reel remains classified");preserved(f);
  }});
  tests.put("prediction_detail_to_reels_same_gesture_does_not_exit",()->{try(F f=new F(IG,true,false)){
   View detail=marker(f,"profile_root",new Rect(0,0,1080,2100));Page prior=new Page(f.activity,detail,"profile_view");f.activity.fragments.fragments.add(prior);
   InstagramBack.predictionStart(f.activity);f.activity.fragments.fragments.remove(prior);f.root.removeView(detail);Page p=reels(f);
   check(!nativeBack(f,p),"a different page at gesture start cannot authorize same-gesture Reels exit");preserved(f);
  }});
  tests.put("prediction_comment_close_same_gesture_does_not_exit",()->{try(F f=new F(IG,true,false)){
   Page p=reels(f);View panel=Back118InstagramTest.group(f,"comments_container",new Rect(0,700,1080,2100),(ViewGroup)p.view);
   InstagramBack.predictionStart(f.activity);((ViewGroup)p.view).removeView(panel);
   check(!nativeBack(f,p),"comments at start remain a layer operation after collapse");preserved(f);
  }});
  for(final String mode:new String[]{"transparent_parent","offscreen"})tests.put("retained_nonvisible_profile_no_longer_vetoes_"+mode,()->{try(F f=new F(IG,true,false)){
   reels(f);ViewGroup parent=new ViewGroup(f.activity);f.root.addView(parent);View old=new View(f.activity);parent.addView(old);
   if(mode.equals("transparent_parent"))parent.alpha=0;else old.bounds=new Rect(1200,0,2280,2100);
   f.activity.fragments.fragments.add(new Page(f.activity,old,"profile_view"));
   check(InstagramReels.foreground(f.activity)==1,"nonvisible retained profile cannot become foreground");
   check(OneBackExit.tryBack(f.activity)&&f.activity.removeCalls==1,"visible Reels exits with retained profile");
  }});
  tests.put("visible_profile_still_vetoes_global_exit",()->{try(F f=new F(IG,true,false)){
   reels(f);View old=marker(f,"profile_root",new Rect(0,0,1080,2100));f.activity.fragments.fragments.add(new Page(f.activity,old,"profile_view"));
   check(InstagramReels.foreground(f.activity)==-1&&!OneBackExit.tryBack(f.activity),"real profile keeps native Back");preserved(f);
  }});
  return tests;
 }
 public static void main(String[] args)throws Exception{int casesBefore=cases,assertionsBefore=assertions;
  for(Map.Entry<String,Test> c:scenarios().entrySet())test(c.getKey(),c.getValue());
  System.out.println("PASS Back119InstagramTest: "+(cases-casesBefore)+" cases, "+(assertions-assertionsBefore)+" assertions");
 }
}
