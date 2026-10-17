package app.hiro.oneback.runtime;

import android.app.*;
import android.content.Context;
import android.graphics.Rect;
import android.view.*;
import java.io.*;
import java.util.*;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Real visibility semantics preserve covered siblings in getGlobalVisibleRect.
 * These doubles distinguish that state from a displayed comments sheet.
 * JVM assertions are not a claim of physical Android verification. */
public final class Back120InstagramTest {
 public static final class Group extends ViewGroup {
  float z;boolean reverse;
  Group(Context c){super(c);}
  public float getZ(){return z;}
  public int getChildDrawingOrder(int position){return reverse?getChildCount()-1-position:position;}
 }
 public static class Page {
  final Activity activity;final View view;String module;boolean resumed;
  final FragmentManager child=new FragmentManager();
  public Page(Activity a,View v,String m,boolean r){activity=a;view=v;module=m;resumed=r;}
  public Activity getActivity(){return activity;}public View getView(){return view;}
  public boolean isVisible(){return true;}public boolean isResumed(){return resumed;}
  public String getModuleName(){return module;}public FragmentManager getChildFragmentManager(){return child;}
 }
 static final class Scene {
  final F f;final Group container,background,viewer;final Page old,reel;
  Scene(F value){
   f=value;container=group(f,"content",new Rect(0,0,1080,2100),f.root);
   background=group(f,"profile_root",new Rect(0,0,1080,2100),container);
   group(f,"comments_container",new Rect(0,700,1080,2100),background);
   old=new Page(f.activity,background,"profile_view",false);f.activity.fragments.fragments.add(old);
   viewer=group(f,"root_clips_layout",new Rect(0,0,1080,2100),container);
   group(f,"clips_viewer_container",new Rect(0,0,1080,1990),viewer);
   reel=new Page(f.activity,viewer,"clips_viewer_clips_tab",true);f.activity.fragments.fragments.add(reel);
  }
 }
 static Group group(F f,String id,Rect bounds,ViewGroup parent){
  Group v=new Group(f.activity);v.id=++markerId;android.content.res.Resources.IDS.put(v.id,id);v.bounds=bounds;parent.addView(v);return v;
 }
 static void blocked(F f,String reason){
  check(InstagramBack.inspect(f.activity).blocked,reason);
  check(!ExitScreens.shouldExit(f.activity),"genuine or ambiguous layer prevents exit");preserved(f);
 }
 /** Exact JVM identifiers/signatures from the supplied APK; normal Java cannot
  * name X.8Zv or X.16z. Public fixture fields model those read-only adapters. */
 static final class NativeF implements AutoCloseable {
  final Application app=new Application(IG);final Activity activity;final ViewGroup root;final ClassLoader loader;
  NativeF()throws Exception{this(false,false);}
  NativeF(boolean mismatch,boolean badLayerDescriptor)throws Exception{
   final Map<String,byte[]> generated=new HashMap<String,byte[]>();
   String fragment="androidx/fragment/app/Fragment",main="com/instagram/mainactivity/InstagramMainActivity";
   generated.put(fragment.replace('/','.'),fixtureClass(fragment,"app/hiro/oneback/runtime/Back120InstagramTest$Page",2,new String[0],false,0x21));
   generated.put("X.8Zv",fixtureClass("X/8Zv",fragment,2,new String[]{"A1Y",badLayerDescriptor?"Ljava/lang/Object;":"LX/21x;","A0L","LX/0wS;"},false,0x31));
   generated.put("X.21x",fixtureClass("X/21x","java/lang/Object",0,new String[]{"A05","Z"},false,0x31));
   generated.put("X.0wS",fixtureClass("X/0wS","java/lang/Object",0,new String[0],false,0x21));
   generated.put("X.16z",fixtureClass("X/16z","X/0wS",0,new String[]{"A15","Z"},false,0x31));
   generated.put(main.replace('/','.'),fixtureClass(main,"android/app/Activity",1,new String[]{"active","L"+fragment+";"},mismatch,0x21));
   loader=new ClassLoader(Back120InstagramTest.class.getClassLoader()){
    @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
     if(!generated.containsKey(name))return super.loadClass(name,resolve);
     Class<?> type=findLoadedClass(name);if(type==null){byte[] bytes=generated.get(name);type=defineClass(name,bytes,0,bytes.length);}
     if(resolve)resolveClass(type);return type;
    }
   };
   activity=(Activity)loader.loadClass(main.replace('/','.')).getConstructor(Application.class,int.class,boolean.class).newInstance(app,11,true);
   ViewGroup decor=(ViewGroup)activity.getWindow().getDecorView();root=new Group(activity);root.bounds=new Rect(0,0,1080,2100);decor.addView(root);
   OneBackExit.install(activity);activity.getWindow().attach();decor.focus(true);OneBackExit.onResume(activity);android.os.Handler.drain();
  }
  Page page(String module,boolean resumed)throws Exception{
   Group view=new Group(activity);view.bounds=new Rect(0,0,1080,2100);view.id=++markerId;
   android.content.res.Resources.IDS.put(view.id,module.startsWith("clips_viewer")?"root_clips_layout":"profile_root");root.addView(view);
   if(module.startsWith("clips_viewer")){Group content=new Group(activity);content.id=++markerId;
    android.content.res.Resources.IDS.put(content.id,"clips_viewer_container");content.bounds=new Rect(0,0,1080,1990);view.addView(content);}
   return (Page)loader.loadClass("X.8Zv").getConstructor(Activity.class,View.class,String.class,boolean.class).newInstance(activity,view,module,resumed);
  }
  void select(Page page)throws Exception{activity.getClass().getField("active").set(activity,page);}
  void added(Page...pages){activity.fragments.fragments.addAll(Arrays.asList(pages));}
  void state(Page page,String field,String type,String flag,boolean active)throws Exception{
   Object value=loader.loadClass(type).getConstructor().newInstance();value.getClass().getField(flag).setBoolean(value,active);page.getClass().getField(field).set(page,value);
  }
  @Override public void close(){OneBackExit.onDestroy(activity);activity.destroyed=true;activity.getWindow().getDecorView().detach();android.os.Handler.drain();android.view.inspector.WindowInspector.clear();}
 }
 static byte[] fixtureClass(String name,String parent,int constructor,String[] fields,boolean mismatch,int flags)throws Exception{
  Instagram447Fixture.Pool p=new Instagram447Fixture.Pool();int self=p.cls(name),sup=p.cls(parent),code=p.utf("Code");
  int[] names=new int[fields.length/2],types=new int[names.length];for(int i=0;i<names.length;i++){names[i]=p.utf(fields[i*2]);types[i]=p.utf(fields[i*2+1]);}
  String descriptor=constructor==1?"(Landroid/app/Application;IZ)V":constructor==2?"(Landroid/app/Activity;Landroid/view/View;Ljava/lang/String;Z)V":"()V";
  int init=p.utf("<init>"),it=p.utf(descriptor),superInit=p.ref(10,parent,"<init>",descriptor);
  ByteArrayOutputStream body=new ByteArrayOutputStream();DataOutputStream b=new DataOutputStream(body);b.writeByte(0x2a);
  if(constructor==1){b.writeByte(0x2b);b.writeByte(0x1c);b.writeByte(0x1d);}
  if(constructor==2){b.writeByte(0x2b);b.writeByte(0x2c);b.writeByte(0x2d);b.writeByte(0x15);b.writeByte(4);}
  b.writeByte(0xb7);b.writeShort(superInit);b.writeByte(0xb1);
  int getter=0,gt=0,ref=0;if(constructor==1){getter=p.utf("A1e");gt=p.utf(mismatch?"()Ljava/lang/Object;":"()Landroidx/fragment/app/Fragment;");ref=p.ref(9,name,"active","Landroidx/fragment/app/Fragment;");}
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
  out.writeInt(0xcafebabe);out.writeShort(0);out.writeShort(52);out.writeShort(p.values.size()+1);for(byte[] value:p.values)out.write(value);
  out.writeShort(flags);out.writeShort(self);out.writeShort(sup);out.writeShort(0);out.writeShort(names.length);
  for(int i=0;i<names.length;i++){out.writeShort(1);out.writeShort(names[i]);out.writeShort(types[i]);out.writeShort(0);}
  out.writeShort(constructor==1?2:1);fixtureMethod(out,init,it,code,body.toByteArray(),constructor==2?5:constructor==1?4:1,1);
  if(constructor==1)fixtureMethod(out,getter,gt,code,new byte[]{0x2a,(byte)0xb4,(byte)(ref>>>8),(byte)ref,(byte)0xb0},1,17);
  out.writeShort(0);return bytes.toByteArray();
 }
 static void fixtureMethod(DataOutputStream out,int name,int type,int code,byte[] body,int words,int flags)throws Exception{
  out.writeShort(flags);out.writeShort(name);out.writeShort(type);out.writeShort(1);out.writeShort(code);out.writeInt(body.length+12);
  out.writeShort(words);out.writeShort(words);out.writeInt(body.length);out.write(body);out.writeShort(0);out.writeShort(0);
 }
 static Map<String,Test> scenarios(){Map<String,Test> tests=new LinkedHashMap<String,Test>();
  tests.put("retained_covered_profile_comments_do_not_block_reels",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);ActivityManager.AppTask task=ownTask(f,11);
   check(!InstagramBack.inspect(f.activity).blocked,"covered non-resumed sibling comments are background");
   check(OneBackExit.tryBack(f.activity),"foreground Reels Back consumed");
   check(task.excluded&&task.removeCalls==1&&f.activity.nativeBackCalls==0,"task removed from recents exactly once");
  }});
  tests.put("retained_covered_comment_fragment_do_not_block_reels",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);s.old.module="comments_fragment";
   check(!InstagramBack.inspect(f.activity).blocked,"fragment and view scanners agree on proven background");
   check(ExitScreens.shouldExit(f.activity)&&OneBackExit.tryBack(f.activity),"explicit Reels target exits");
   check(f.activity.removeCalls==1,"one task removal");
  }});
  tests.put("retained_covered_comments_native_guard_does_not_poison_reels",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);OneBackExit.enterBack(f.activity);
   try{check(OneBackExit.tryInstagramReelsBack(s.reel),"native transaction not protected by covered background");}
   finally{OneBackExit.leaveNative();}
   check(f.activity.removeCalls==1,"one native-boundary task removal");
  }});
  tests.put("retained_covered_comments_prediction_targets_reels",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);InstagramBack.predictionStart(f.activity);
   check(InstagramBack.exitPrediction(f.activity),"gesture captures foreground Reels without underlying comments");
   check(OneBackExit.tryBack(f.activity)&&f.activity.removeCalls==1,"predicted Reels commits once");
  }});
  tests.put("retained_covered_comments_key_pair_exits_once",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);check(OneBackExit.key(f.activity,down()),"Reels key down captured");preserved(f);
   check(OneBackExit.key(f.activity,up()),"Reels key release consumed");
   check(f.activity.removeCalls==1&&f.activity.nativeBackCalls==0,"key pair exits instead of forwarding background close");
  }});
  tests.put("reels_own_comments_still_close_before_exit",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);View panel=group(f,"comments_container",new Rect(0,700,1080,2100),s.viewer);
   f.activity.nativeBackAction=()->s.viewer.removeView(panel);
   check(OneBackExit.tryBack(f.activity),"current comments close");
   check(f.activity.nativeBackCalls==1,"one native comments dismissal");preserved(f);
   check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"next physical Back exits Reels");
   check(f.activity.removeCalls==1,"one next-Back task removal");
  }});
  tests.put("later_comment_fragment_is_never_background",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);View panel=group(f,"comments_container",new Rect(0,700,1080,2100),s.container);
   f.activity.fragments.fragments.add(new Page(f.activity,panel,"comments_fragment",false));
   blocked(f,"later comments fragment retained as foreground layer even before RESUMED");
  }});
  tests.put("resumed_older_comments_are_not_excluded",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);s.old.resumed=true;blocked(f,"RESUMED sibling is ambiguous");
  }});
  tests.put("unknown_lifecycle_comments_are_not_excluded",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);f.activity.fragments.fragments.set(0,new Back119InstagramTest.Page(f.activity,s.background,"profile_view"));
   blocked(f,"missing lifecycle evidence is not background proof");
  }});
  tests.put("uncovered_older_comments_are_not_excluded",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);s.background.bounds=new Rect(0,0,1080,2200);blocked(f,"background has an uncovered visible area");
  }});
  tests.put("older_comments_brought_to_front_are_not_excluded",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);s.container.removeView(s.background);s.container.addView(s.background);
   blocked(f,"actual sibling drawing order overrides added-fragment order");
  }});
  tests.put("elevated_older_comments_are_not_excluded",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);s.background.z=12;blocked(f,"higher elevation is foreground even if inserted earlier");
  }});
  tests.put("custom_drawing_order_comments_are_not_excluded",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);s.container.reverse=true;blocked(f,"public custom drawing order is respected");
  }});
  tests.put("nonresumed_reels_cannot_hide_background_layers",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);s.reel.resumed=false;blocked(f,"Reels must be resumed to establish covering foreground");
  }});
  tests.put("translucent_reels_cannot_hide_background_layers",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);s.viewer.alpha=.5f;blocked(f,"translucent Reels do not fully cover lower content");
  }});
  tests.put("different_parent_comments_are_not_excluded",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);s.container.removeView(s.background);f.root.addView(s.background);
   blocked(f,"unrelated view parents are not ordered by fragment history");
  }});
  tests.put("keyboard_over_reels_with_retained_background_is_native",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);f.root.insets.ime=true;check(!OneBackExit.tryBack(f.activity),"keyboard Back retains native priority");preserved(f);
  }});
  tests.put("focused_dialog_over_reels_with_retained_background_is_native",()->{try(F f=new F(IG,true,false)){
   Scene s=new Scene(f);f.dialog();check(!HomeScreen.inputReady(f.activity),"focused dialog cannot authorize Reels exit");
   OneBackExit.tryBack(f.activity);preserved(f);
  }});
  tests.put("native_selected_reels_resolves_later_retained_background",()->{try(NativeF n=new NativeF()){
   Page old=n.page("profile_view",false),reel=n.page("clips_viewer_clips_tab",true);n.select(reel);n.added(reel,old);
   check(InstagramReels.foregroundFragment(n.activity)==reel,"native selection plus actual draw order resolves stale added-list order");
   check(OneBackExit.tryBack(n.activity)&&n.activity.removeCalls==1,"selected Reels exits despite covered retained profile");
  }});
  tests.put("native_selected_reels_keeps_real_profile_overlay",()->{try(NativeF n=new NativeF()){
   Page reel=n.page("clips_viewer_clips_tab",true),profile=n.page("profile_view",false);n.select(reel);n.added(reel,profile);
   check(InstagramReels.foregroundFragment(n.activity)==Boolean.FALSE,"profile actually drawn above selected Reels still vetoes");
   check(!OneBackExit.tryBack(n.activity)&&n.activity.removeCalls==0,"overlay does not exit selected tab underneath");
  }});
  tests.put("native_selected_profile_rejects_stale_reels_list",()->{try(NativeF n=new NativeF()){
   Page reel=n.page("clips_viewer_clips_tab",false),profile=n.page("profile_view",true);n.select(profile);n.added(profile,reel);
   check(InstagramReels.foreground(n.activity)==-1,"actual selected profile overrides stale Reels list entry");
   check(!OneBackExit.tryBack(n.activity)&&n.activity.removeCalls==0,"profile Back remains native");
  }});
  tests.put("native_selected_wrapper_descends_to_reels",()->{try(NativeF n=new NativeF()){
   Page wrapper=n.page("tab_host",true),reel=n.page("clips_viewer_homecoming_fyp",true);
   n.root.removeView(reel.view);((ViewGroup)wrapper.view).addView(reel.view);wrapper.child.fragments.add(reel);n.select(wrapper);n.added(wrapper);
   check(InstagramReels.foregroundFragment(n.activity)==reel,"typed current-tab wrapper resolves its visible Reels child");
   check(OneBackExit.tryBack(n.activity)&&n.activity.removeCalls==1,"wrapper hosted Reels exits");
  }});
  tests.put("native_selected_reels_child_profile_keeps_native_back",()->{try(NativeF n=new NativeF()){
   Page reel=n.page("clips_viewer_clips_tab",true),profile=n.page("profile_view",true);
   n.root.removeView(profile.view);((ViewGroup)reel.view).addView(profile.view);reel.child.fragments.add(profile);n.select(reel);n.added(reel);
   check(InstagramReels.foregroundFragment(n.activity)==Boolean.FALSE,"actual current child profile vetoes selected Reels");
   check(!OneBackExit.tryBack(n.activity)&&n.activity.removeCalls==0,"child profile cannot remove app task");
  }});
  tests.put("native_empty_selection_rejects_retained_reels",()->{try(NativeF n=new NativeF()){
   n.added(n.page("clips_viewer_clips_tab",false));
   check(InstagramReels.foregroundFragment(n.activity)==Boolean.FALSE,"known empty native selection is not stale Reels");
   check(!OneBackExit.tryBack(n.activity)&&n.activity.removeCalls==0,"no live selection authorizes exit");
  }});
  tests.put("mismatched_native_getter_uses_existing_safe_fallback",()->{try(NativeF n=new NativeF(true,false)){
   Page reel=n.page("clips_viewer_clips_tab",true);n.added(reel);
   check(InstagramReels.foregroundFragment(n.activity)==reel,"Object-returning A1e is not the verified adapter");
   check(OneBackExit.tryBack(n.activity)&&n.activity.removeCalls==1,"existing positively visible Reels fallback remains functional");
  }});
  return tests;
 }
 public static void main(String[] args)throws Exception{
  int beforeCases=cases,beforeAssertions=assertions,failed=0;boolean compare=args.length>0&&"compare".equals(args[0]);
  for(Map.Entry<String,Test> item:scenarios().entrySet())try{test(item.getKey(),item.getValue());}
  catch(AssertionError e){if(!compare)throw e;failed++;System.out.println("FAIL "+item.getKey()+": "+e.getMessage());}
  System.out.println((failed==0?"PASS ":"REGRESSION ")+"Back120InstagramTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions"+(failed==0?"":", "+failed+" failed"));
  if(failed!=0&&!compare)throw new AssertionError("Instagram v120 regression");
 }
}
