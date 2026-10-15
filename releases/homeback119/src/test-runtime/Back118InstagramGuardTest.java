package app.hiro.oneback.runtime;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import java.util.*;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** The old fragment/focus flags can stay true after a comment pane leaves the screen. */
public final class Back118InstagramGuardTest {
    public static final class RetainedPane {
        final View view;
        RetainedPane(View v){view=v;}
        public boolean isVisible(){return true;}
        public String getModuleName(){return "comments_thread";}
        public View getView(){return view;}
        public Object getChildFragmentManager(){return null;}
    }
    static void reels(F f){marker(f,"clips_viewer_view_pager",new Rect(0,0,1080,1980));}
    static Map<String,Test> scenarios(){
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final String mode:new String[]{"hidden","offscreen","transparent_parent"}){
            tests.put("ig118_retained_comment_fragment_"+mode,()->{try(F f=new F(IG,true,false)){
                reels(f);ViewGroup parent=new ViewGroup(f.activity);f.root.addView(parent);
                View pane=new View(f.activity);pane.bounds=new Rect(0,400,1080,2200);parent.addView(pane);
                if("hidden".equals(mode))pane.hidden=true;
                if("offscreen".equals(mode))pane.bounds=new Rect(1100,0,2180,2200);
                if("transparent_parent".equals(mode))parent.alpha=0;
                f.activity.fragments.fragments.add(new RetainedPane(pane));
                check(!InstagramBack.inspect(f.activity).blocked,"nonvisible retained fragment is not an open comments sheet");
                check(OneBackExit.tryBack(f.activity)&&f.activity.removeCalls==1,"Reels exits after closed retained comments");
                check(f.activity.nativeBackCalls==0,"does not pop Reels as a supposed comment layer");
            }});
            tests.put("ig118_retained_focus_"+mode,()->{try(F f=new F(IG,true,false)){
                reels(f);ViewGroup parent=new ViewGroup(f.activity);f.root.addView(parent);
                View editor=new View(f.activity);editor.editor=true;editor.bounds=new Rect(0,1900,1000,2050);parent.addView(editor);
                if("hidden".equals(mode))editor.hidden=true;
                if("offscreen".equals(mode))editor.bounds=new Rect(1100,1900,2100,2050);
                if("transparent_parent".equals(mode))parent.alpha=0;
                f.root.focusView=editor;
                check(HomeScreen.inputReady(f.activity),"hidden IME plus nonvisible retained focus is ready");
                check(OneBackExit.tryBack(f.activity)&&f.activity.removeCalls==1,"retained focus cannot trap Reels");
            }});
        }
        tests.put("ig118_visible_comment_fragment_still_blocks",()->{try(F f=new F(IG,true,false)){
            reels(f);View pane=new View(f.activity);pane.bounds=new Rect(0,600,1080,2200);f.root.addView(pane);
            f.activity.fragments.fragments.add(new RetainedPane(pane));
            check(InstagramBack.inspect(f.activity).comments&&!ExitScreens.shouldExit(f.activity),"physically visible comments retain veto");
            OneBackExit.tryBack(f.activity);check(f.activity.nativeBackCalls==1,"native comment close forwarded");preserved(f);
        }});
        tests.put("ig118_unmeasurable_comment_fragment_still_blocks",()->{try(F f=new F(IG,true,false)){
            reels(f);f.activity.fragments.fragments.add(new RetainedPane(null));
            check(!ExitScreens.shouldExit(f.activity),"unknown fragment view remains conservative");preserved(f);
        }});
        tests.put("ig118_visible_focused_editor_ime_hidden_still_blocks",()->{try(F f=new F(IG,true,false)){
            reels(f);View editor=new View(f.activity);editor.editor=true;editor.bounds=new Rect(0,1800,1000,2000);f.root.addView(editor);f.root.focusView=editor;
            check(!HomeScreen.inputReady(f.activity)&&!ExitScreens.shouldExit(f.activity),"visible editing preserved even after keyboard hides");preserved(f);
        }});
        tests.put("ig118_hidden_editor_with_visible_ime_still_blocks",()->{try(F f=new F(IG,true,false)){
            reels(f);View editor=new View(f.activity);editor.editor=true;editor.hidden=true;f.root.focusView=editor;f.root.insets.ime=true;
            check(!HomeScreen.inputReady(f.activity)&&!OneBackExit.tryBack(f.activity),"IME keeps ownership of Back");preserved(f);
        }});
        for(final String pkg:new String[]{X,TT,JP,TRIP})
            tests.put("ig118_other_app_focus_guard_unchanged_"+pkg,()->{try(F f=new F(pkg)){
                View editor=new View(f.activity);editor.editor=true;editor.hidden=true;f.root.focusView=editor;
                check(!HomeScreen.inputReady(f.activity),"other apps keep prior focused editor guard");preserved(f);
            }});
        return tests;
    }
    static void run()throws Exception {for(Map.Entry<String,Test> e:scenarios().entrySet())test(e.getKey(),e.getValue());}
    public static void main(String[] args)throws Exception {
        int failed=0;for(Map.Entry<String,Test> e:scenarios().entrySet())try{test(e.getKey(),e.getValue());}catch(AssertionError error){failed++;System.out.println("FAIL "+e.getKey()+": "+error.getMessage());}
        System.out.println("REGRESSION Instagram guards: "+scenarios().size()+" cases, "+failed+" failed");if(failed>0)System.exit(1);
    }
}
