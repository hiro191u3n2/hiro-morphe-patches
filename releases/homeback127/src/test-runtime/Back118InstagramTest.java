package app.hiro.oneback.runtime;

import android.app.*;
import android.graphics.Rect;
import android.view.*;
import android.widget.TextView;
import java.util.*;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Uses IDs/tree shape decoded from the supplied v447 assets/layouts.bin.xz. */
public final class Back118InstagramTest {
 static ViewGroup group(F f,String id,Rect bounds,ViewGroup parent){
  ViewGroup v=new ViewGroup(f.activity);v.bounds=bounds;
  if(id!=null){v.id=++markerId;android.content.res.Resources.IDS.put(v.id,id);}
  parent.addView(v);return v;
 }
 static final class ReelsFooter {
  final ViewGroup page,viewer,footer,inner;final TextView prompt;
  ReelsFooter(F f){
   page=group(f,"root_clips_layout",new Rect(0,0,1080,2200),f.root);
   ViewGroup linear=group(f,"clips_linear_layout_container",new Rect(0,0,1080,2170),page);
   ViewGroup gestures=group(f,"gesture_manager",new Rect(0,0,1080,1990),linear);
   viewer=group(f,"clips_viewer_container",new Rect(0,0,1080,1990),gestures);
   ViewGroup refresh=group(f,"clips_swipe_refresh_container",new Rect(0,0,1080,1990),viewer);
   group(f,"clips_viewer_view_pager",new Rect(0,0,1080,1990),refresh);
   footer=group(f,"clips_footer_mask_container",new Rect(0,1990,1080,2110),linear);
   // ViewStub has no inflatedId: the inflated IgFrameLayout itself has no ID.
   ViewGroup outer=group(f,null,new Rect(0,1990,1080,2110),footer);
   inner=group(f,"comment_composer_inner_layout",new Rect(24,2000,1056,2100),outer);
   prompt=new TextView(f.activity);prompt.id=++markerId;android.content.res.Resources.IDS.put(prompt.id,"comment_composer_text_view");
   prompt.bounds=new Rect(40,2005,950,2095);prompt.text="コメントを追加…";inner.addView(prompt);
  }
 }
 static Map<String,Test> scenarios(){
  Map<String,Test> cases=new LinkedHashMap<String,Test>();
  cases.put("ig118_actual_footer_is_not_open_comments",()->{try(F f=new F(IG,true,false)){
   new ReelsFooter(f);ActivityManager.AppTask task=ownTask(f,11);
   check(!InstagramBack.inspect(f.activity).blocked,"APK's noneditable Reels footer is not an open comments panel");
   check(OneBackExit.tryBack(f.activity),"Reels Back consumed");
   check(task.excluded&&task.removeCalls==1,"bare Reels footer still exits and removes Recents");
   check(f.activity.nativeBackCalls==0,"Back never delegates to previous page for bare footer");
  }});
  cases.put("ig118_actual_footer_android_key_exits_once",()->{try(F f=new F(IG,true,false)){
   new ReelsFooter(f);ActivityManager.AppTask task=ownTask(f,11);
   check(OneBackExit.key(f.activity,down()),"Android Back down captured");preserved(f);
   check(OneBackExit.key(f.activity,up()),"Android Back up consumed");
   check(task.excluded&&task.removeCalls==1&&f.activity.nativeBackCalls==0,"one Back exits with no native tab switch");
  }});
  cases.put("ig118_footer_with_real_comments_collapses_then_exits",()->{try(F f=new F(IG,true,false)){
   ReelsFooter ui=new ReelsFooter(f);View panel=igPanel(f,"comments_container");
   f.activity.nativeBackAction=()->f.root.removeView(panel);ActivityManager.AppTask task=ownTask(f,11);
   check(InstagramBack.inspect(f.activity).blocked,"actual comments panel still blocks exit");
   check(OneBackExit.tryBack(f.activity),"comments Back handled");preserved(f);
   check(f.activity.nativeBackCalls==1&&!task.excluded,"comments first Back only closes panel");
   check(!InstagramBack.inspect(f.activity).blocked,"remaining native footer no longer traps Back");
   check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"next Back exits");
   check(task.excluded&&task.removeCalls==1,"next Back removes task exactly once");
  }});
  cases.put("ig118_footer_with_real_share_sheet_not_exit",()->{try(F f=new F(IG,true,false)){
   new ReelsFooter(f);View panel=igPanel(f,"share_sheet");f.activity.nativeBackAction=()->f.root.removeView(panel);
   check(InstagramBack.inspect(f.activity).blocked,"real share sheet retains precedence");
   check(OneBackExit.tryBack(f.activity),"share Back handled");preserved(f);
   check(f.activity.nativeBackCalls==1,"native share close is preserved");
  }});
  cases.put("ig118_editor_inside_footer_is_not_exempt",()->{try(F f=new F(IG,true,false)){
   ReelsFooter ui=new ReelsFooter(f);View edit=new View(f.activity);edit.editor=true;
   edit.id=++markerId;android.content.res.Resources.IDS.put(edit.id,"comment_composer_edit_text");
   edit.bounds=new Rect(20,2000,1030,2100);ui.inner.addView(edit);
   check(InstagramBack.inspect(f.activity).blocked,"a real editor inside footer is not a CTA exemption");preserved(f);
  }});
  cases.put("ig118_same_composer_outside_footer_remains_layer",()->{try(F f=new F(IG,true,false)){
   ReelsFooter ui=new ReelsFooter(f);ui.footer.removeView(ui.inner.getParent() instanceof View?(View)ui.inner.getParent():ui.inner);
   ViewGroup panel=group(f,"comments_panel_root",new Rect(0,650,1080,2200),f.root);
   TextView prompt=new TextView(f.activity);prompt.id=++markerId;android.content.res.Resources.IDS.put(prompt.id,"comment_composer_text_view");
   prompt.bounds=new Rect(40,2005,950,2095);prompt.text="コメントを追加…";panel.addView(prompt);
   check(InstagramBack.inspect(f.activity).blocked,"same composer name outside native footer cannot authorize exit");preserved(f);
  }});
  for(final String text:new String[]{"コメント","comments","コメント 123"})
   cases.put("ig118_caption_word_not_comments_heading_"+text,()->{try(F f=new F(IG,true,false)){
    ReelsFooter ui=new ReelsFooter(f);ui.footer.hidden=true;ViewGroup caption=group(f,"clips_caption_component",new Rect(40,1500,1000,1650),ui.viewer);
    TextView content=new TextView(f.activity);content.text=text;content.bounds=new Rect(40,1520,940,1620);caption.addView(content);
    check(!InstagramBack.inspect(f.activity).blocked,"caption content does not prove comments are open");
    check(OneBackExit.tryBack(f.activity)&&f.activity.removeCalls==1,"caption wording cannot stop requested Reels exit");
   }});
  return cases;
 }
 static void run()throws Exception {for(Map.Entry<String,Test> c:scenarios().entrySet())test(c.getKey(),c.getValue());}
 public static void main(String[] args)throws Exception {
  int failed=0;for(Map.Entry<String,Test> c:scenarios().entrySet())try{test(c.getKey(),c.getValue());}catch(AssertionError e){failed++;System.out.println("FAIL "+c.getKey()+": "+e.getMessage());}
  System.out.println("REGRESSION Instagram118 layout: "+scenarios().size()+" cases, "+failed+" failed");if(failed>0)System.exit(1);
 }
}
