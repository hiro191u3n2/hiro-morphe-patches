package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;
import static app.hiro.oneback.runtime.Back127Instagram452Test.*;

/** Host models of the native feed callback/result/exit-prompt sequence.
 * The separate DEX audit establishes the real APK insertion boundaries.
 */
public final class Back134InstagramNativeHomeTest {
    private static Node home(Scene scene)throws Exception {
        Node home=scene.node("X.00nV",null,MAIN);scene.select(home,true);scene.install();return home;
    }
    private static void nativeFalse(Node home) {
        InstagramNativeHome.begin(home);
        check(!InstagramNativeHome.finish(home,false),"native false is preserved");
    }
    private static boolean prompt(Scene scene,Node home) {
        InstagramNativeHome.beginScope(scene.activity);
        try{nativeFalse(home);return InstagramNativeHome.prompt(home);}
        finally{InstagramNativeHome.endScope();}
    }
    static Map<String,Test> scenarios(){Map<String,Test> tests=new LinkedHashMap<String,Test>();
        tests.put("native_prompt_removes_home_once",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);
            InstagramNativeHome.beginScope(scene.activity);
            try{nativeFalse(page);check(InstagramNativeHome.prompt(page),"native prompt exits Home");
                check(!InstagramNativeHome.prompt(page),"same prompt cannot exit again");}
            finally{InstagramNativeHome.endScope();}removed(scene,task);
        }});
        tests.put("ordinary_caption_does_not_veto_native_authority",()->{try(Scene scene=new Scene()){
            Node page=home(scene);TextView caption=new TextView(scene.activity);caption.text="コメント";caption.bounds=new Rect(0,700,1080,800);
            ((ViewGroup)page.view).addView(caption);ActivityManager.AppTask task=own(scene);
            check(InstagramBack.inspect(scene.activity).blocked,"legacy heuristic mistakes caption for comments");
            check(prompt(scene,page),"verified native exit prompt bypasses content heuristic");removed(scene,task);
        }});
        tests.put("native_outer_wrapper_integration",()->{try(Scene scene=new Scene()){
            Node page=home(scene);TextView caption=new TextView(scene.activity);caption.text="コメント";caption.bounds=new Rect(0,700,1080,800);
            ((ViewGroup)page.view).addView(caption);ActivityManager.AppTask task=own(scene);
            OneBackExit.enterBack(scene.activity);
            try{OneBackExit.beginInstagramNativeHomeBack(page);check(!OneBackExit.finishInstagramNativeHomeBack(page,false),"public hook preserves native return");
                check(OneBackExit.tryInstagramNativeHomePrompt(page),"outer native wrapper reaches native Home fallback");}
            finally{OneBackExit.leaveNative();}
            check(!OneBackExit.tryInstagramNativeHomePrompt(page),"outer leave clears scope");removed(scene,task);
        }});
        for(final String type:new String[]{"X.00VS","X.02Wh"})tests.put("native_wrapper_"+type,()->{try(Scene scene=new Scene()){
            Node wrapper=scene.node(type,null,MAIN);scene.select(wrapper,true);
            Node page=scene.node("X.00nV",wrapper,type.equals("X.00VS")?BASE:MAIN);scene.install();ActivityManager.AppTask task=own(scene);
            check(prompt(scene,page),"same initially selected native Home receiver exits");removed(scene,task);
        }});
        for(final String type:new String[]{"X.00VS","X.02Wh"})tests.put("native_feed_switcher_"+type,()->{try(Scene scene=new Scene()){
            Node[] route=Back134InstagramFeedSwitcherTest.feed(scene,type);scene.install();ActivityManager.AppTask task=own(scene);
            check(prompt(scene,route[3]),"native Home fallback follows reordered stable feed ID without lazy getters");removed(scene,task);
        }});
        tests.put("no_native_scope",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);nativeFalse(page);
            check(!InstagramNativeHome.prompt(page),"programmatic callback has no exit permission");preserved(scene,task);
        }});
        tests.put("no_feed_result",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.beginScope(scene.activity);
            try{InstagramNativeHome.begin(page);check(!InstagramNativeHome.prompt(page),"prompt before native child result is rejected");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        tests.put("native_child_consumed",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.beginScope(scene.activity);
            try{InstagramNativeHome.begin(page);check(InstagramNativeHome.finish(page,true),"native true is preserved");
                check(!InstagramNativeHome.prompt(page),"consumed child cannot become terminal");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        tests.put("scope_end_revokes_token",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.beginScope(scene.activity);nativeFalse(page);InstagramNativeHome.endScope();
            check(!InstagramNativeHome.prompt(page),"completed native transaction cannot exit later");preserved(scene,task);
        }});
        tests.put("same_back_profile_return_is_not_home_exit",()->{try(Scene scene=new Scene()){
            Node profile=scene.node("X.NativeProfile",null,MAIN);scene.select(profile,true);scene.install();ActivityManager.AppTask task=own(scene);
            InstagramNativeHome.beginScope(scene.activity);
            try{profile.visible=false;profile.resumed=false;profile.view.hidden=true;Node page=scene.node("X.00nV",null,MAIN);scene.select(page,true);
                nativeFalse(page);check(!InstagramNativeHome.prompt(page),"Back that began on profile cannot acquire Home exit");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        tests.put("initial_home_changed",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.beginScope(scene.activity);
            try{nativeFalse(page);Node profile=scene.node("X.NativeProfile",null,MAIN);scene.select(profile,true);
                check(!InstagramNativeHome.prompt(page),"new selected child invalidates original Home");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        tests.put("home_view_replaced",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.beginScope(scene.activity);
            try{nativeFalse(page);scene.root.removeView(page.view);page.view=new ViewGroup(scene.activity);scene.root.addView(page.view);
                check(!InstagramNativeHome.prompt(page),"recreated view invalidates original Home");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        tests.put("home_view_replaced_before_native_entry",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.beginScope(scene.activity);
            try{scene.root.removeView(page.view);page.view=new ViewGroup(scene.activity);scene.root.addView(page.view);
                nativeFalse(page);check(!InstagramNativeHome.prompt(page),"scope captures view before native handler begins");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        tests.put("reentrant_feed_handler",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.beginScope(scene.activity);
            try{nativeFalse(page);nativeFalse(page);check(!InstagramNativeHome.prompt(page),"second native callback does not reuse initial token");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        for(final String kind:new String[]{"ime","editor","comments","share","dialog","focus"})tests.put("initial_"+kind+"_closed_by_same_back",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);View layer=null;Dialog dialog=null;
            if(kind.equals("ime"))scene.root.insets.ime=true;
            if(kind.equals("editor")){layer=new View(scene.activity);layer.editor=true;((ViewGroup)page.view).addView(layer);scene.root.focusView=layer;}
            if(kind.equals("comments")||kind.equals("share"))layer=scene.group((ViewGroup)page.view,kind.equals("comments")?"comments_sheet":"share_sheet",new Rect(0,650,1080,2200));
            if(kind.equals("dialog")){dialog=new Dialog(scene.activity);dialog.show();dialog.getWindow().getDecorView().focus(true);}
            if(kind.equals("focus"))scene.root.focus(false);
            InstagramNativeHome.beginScope(scene.activity);
            try{scene.root.insets.ime=false;scene.root.focusView=null;if(layer!=null)((ViewGroup)page.view).removeView(layer);
                if(dialog!=null)dialog.dismiss();scene.root.focus(true);nativeFalse(page);
                check(!InstagramNativeHome.prompt(page),"initial input/layer closure is not Home exit");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        tests.put("layer_opened_during_native",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.beginScope(scene.activity);
            try{nativeFalse(page);scene.group((ViewGroup)page.view,"comments_sheet",new Rect(0,650,1080,2200));
                check(!InstagramNativeHome.prompt(page),"new layer after native result remains open");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        tests.put("canceled_back_key",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.key(scene.activity,down());
            InstagramNativeHome.key(scene.activity,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,true));
            check(!prompt(scene,page),"canceled key cannot authorize native prompt");preserved(scene,task);
        }});
        tests.put("key_down_on_ime_stays_nonterminal",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);scene.root.insets.ime=true;InstagramNativeHome.key(scene.activity,down());
            scene.root.insets.ime=false;InstagramNativeHome.key(scene.activity,up());
            check(!prompt(scene,page),"key closing IME cannot become Home exit after focus return");preserved(scene,task);
        }});
        tests.put("unmatched_key_up",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.key(scene.activity,up());
            check(!prompt(scene,page),"unmatched key release has no initial screen authority");preserved(scene,task);
        }});
        tests.put("key_down_on_different_home",()->{try(Scene scene=new Scene()){
            Node first=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.key(scene.activity,down());
            first.resumed=false;first.view.hidden=true;Node second=scene.node("X.00nV",null,MAIN);scene.select(second,true);
            InstagramNativeHome.key(scene.activity,up());check(!prompt(scene,second),"key cannot switch Home identity");preserved(scene,task);
        }});
        tests.put("duplicate_down_cannot_refresh_ime_origin",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);scene.root.insets.ime=true;
            InstagramNativeHome.key(scene.activity,down());scene.root.insets.ime=false;InstagramNativeHome.key(scene.activity,down());
            InstagramNativeHome.key(scene.activity,up());check(!prompt(scene,page),"pre-IME origin survives duplicate Activity DOWN");preserved(scene,task);
        }});
        tests.put("duplicate_down_cannot_refresh_profile_origin",()->{try(Scene scene=new Scene()){
            Node profile=scene.node("X.NativeProfile",null,MAIN);scene.select(profile,true);scene.install();ActivityManager.AppTask task=own(scene);
            InstagramNativeHome.key(scene.activity,down());profile.view.hidden=true;profile.resumed=false;
            Node page=scene.node("X.00nV",null,MAIN);scene.select(page,true);InstagramNativeHome.key(scene.activity,down());
            InstagramNativeHome.key(scene.activity,up());check(!prompt(scene,page),"profile origin survives duplicate Activity DOWN");preserved(scene,task);
        }});
        tests.put("predictive_child_to_home",()->{try(Scene scene=new Scene()){
            Node child=scene.node("X.NativeProfile",null,MAIN);scene.select(child,true);scene.install();ActivityManager.AppTask task=own(scene);
            InstagramNativeHome.predictionStart(scene.activity);child.resumed=false;child.view.hidden=true;
            Node page=scene.node("X.00nV",null,MAIN);scene.select(page,true);
            check(!prompt(scene,page),"predictive child pop does not acquire Home exit");preserved(scene,task);
            InstagramNativeHome.predictionEnd(scene.activity);
        }});
        tests.put("predictive_home_identity_changed",()->{try(Scene scene=new Scene()){
            Node first=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.predictionStart(scene.activity);
            first.resumed=false;first.view.hidden=true;Node second=scene.node("X.00nV",null,MAIN);scene.select(second,true);
            check(!prompt(scene,second),"predictive callback cannot switch Home identity");preserved(scene,task);
            InstagramNativeHome.predictionEnd(scene.activity);
        }});
        tests.put("duplicate_predictive_start_keeps_child_origin",()->{try(Scene scene=new Scene()){
            Node child=scene.node("X.NativeProfile",null,MAIN);scene.select(child,true);scene.install();ActivityManager.AppTask task=own(scene);
            InstagramNativeHome.predictionStart(scene.activity);child.resumed=false;child.view.hidden=true;
            Node page=scene.node("X.00nV",null,MAIN);scene.select(page,true);InstagramNativeHome.predictionStart(scene.activity);
            check(!prompt(scene,page),"platform and AndroidX duplicate start retain original child");preserved(scene,task);
            InstagramNativeHome.predictionEnd(scene.activity);
        }});
        tests.put("predictive_cancel_never_exits",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.predictionStart(scene.activity);
            InstagramNativeHome.predictionCancel(scene.activity);InstagramNativeHome.predictionEnd(scene.activity);
            check(!prompt(scene,page),"same event after prediction cancel remains nonterminal");preserved(scene,task);
        }});
        tests.put("predictive_home_commit",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.predictionStart(scene.activity);
            preserved(scene,task);check(prompt(scene,page),"unchanged committed Home may exit");removed(scene,task);
            InstagramNativeHome.predictionEnd(scene.activity);
        }});
        tests.put("fresh_key_after_cancel",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.key(scene.activity,down());
            InstagramNativeHome.key(scene.activity,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,true));Handler.drain();
            InstagramNativeHome.key(scene.activity,down());InstagramNativeHome.key(scene.activity,up());
            check(prompt(scene,page),"independent new key can exit");removed(scene,task);
        }});
        tests.put("destroyed_activity",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.beginScope(scene.activity);
            try{nativeFalse(page);scene.activity.destroyed=true;check(!InstagramNativeHome.prompt(page),"destroyed owner cannot exit");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        tests.put("changing_configuration",()->{try(Scene scene=new Scene()){
            Node page=home(scene);ActivityManager.AppTask task=own(scene);InstagramNativeHome.beginScope(scene.activity);
            try{nativeFalse(page);scene.activity.changingConfigurations=true;check(!InstagramNativeHome.prompt(page),"configuration transition cannot exit");}
            finally{InstagramNativeHome.endScope();}preserved(scene,task);
        }});
        return tests;
    }
    public static void main(String[]args)throws Exception {
        int beforeCases=cases,beforeAssertions=assertions;
        for(Map.Entry<String,Test> item:scenarios().entrySet())try{test(item.getKey(),item.getValue());}
        finally{InstagramNativeHome.endScope();Handler.drain();}
        System.out.println("PASS Back134InstagramNativeHomeTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions; host models, no device execution");
    }
}
