package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.Back121InstagramDispatchTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent121Fixture.*;

/** Runtime contract at original X.8Zv.onBackPressed PC31. The actual APK's
 * preserved branch/return ordering is checked separately by the DEX audit.
 * In particular a consumed popup/sheet returns before the bridge, rather than
 * closing the layer and then calling the bridge as older host tests assumed. */
public final class Back123NativeReelsTest {
    static boolean nativeHandler(Node reels,int[] hooks)throws Exception {
        Object popup=reels.getClass().getField("A1Y").get(reels);
        if(popup!=null&&popup.getClass().getField("A05").getBoolean(popup)) {
            set(popup,"A05",false);return true; // A09(), then native return true.
        }
        Object layer=reels.getClass().getField("A0L").get(reels);
        if(layer!=null&&layer.getClass().getField("A15").getBoolean(layer)
                &&layer.getClass().getField("A0U").getBoolean(layer)
                &&((NativeActivity121Fixture.LayerState)layer).current!=null) {
            ((NativeActivity121Fixture.LayerState)layer).current=null;
            return true; // A0p() dismisses the sheet and returns true.
        }
        hooks[0]++;return OneBackExit.tryInstagramReelsBack(reels);
    }
    static Map<String,Test> scenarios(){Map<String,Test> cases=new LinkedHashMap<String,Test>();
        cases.put("native_reels_receiver_exits_without_platform_scope",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);ActivityManager.AppTask task=own(s);
            check(OneBackExit.tryInstagramReelsBack(reels),"actual native receiver is sufficient after native guards");removed(s,task);
            check(!OneBackExit.tryInstagramReelsBack(reels),"finishing receiver cannot exit twice");
        }});
        cases.put("native_reels_subclass_keeps_exact_handler_contract",()->{try(Scene s=new Scene()){
            Node reels=s.node("X.Ze9",null,MAIN);s.select(reels,false);ActivityManager.AppTask task=own(s);
            check(OneBackExit.tryInstagramReelsBack(reels),"real subclasses inherit the verified X.8Zv handler");removed(s,task);
        }});
        cases.put("native_reels_boundary_ignores_view_and_input_heuristics",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);reels.view=null;reels.visible=false;reels.resumed=false;s.root.insets.ime=true;s.root.focus(false);
            ActivityManager.AppTask task=own(s);check(!HomeScreen.inputReady(s.activity),"generic input gate rejects this model");
            check(OneBackExit.tryInstagramReelsBack(reels),"exact native dispatch does not repeat that heuristic");removed(s,task);
        }});
        cases.put("native_reels_boundary_ignores_false_guard_origin_and_prediction",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);s.group((ViewGroup)reels.view,"comments_sheet",new Rect(0,900,1080,2200));
            ActivityManager.AppTask task=own(s);InstagramBack.predictionStart(s.activity);OneBackExit.enterBack(s.activity);
            try {
                check(InstagramBack.action(s.activity)==3,"generic scan installed its false layer/prediction veto");
                check(OneBackExit.tryInstagramReelsBack(reels),"native handlers declined Back despite the false heuristic");removed(s,task);
            }finally{OneBackExit.leaveNative();}
        }});
        cases.put("native_reels_boundary_handles_false_layer_forwarding",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);s.group((ViewGroup)reels.view,"comments_sheet",new Rect(0,900,1080,2200));
            ActivityManager.AppTask task=own(s);s.activity.nativeBackAction=()->{
                check(InstagramBack.forwarding(),"actual internal forwarding state reached");
                check(OneBackExit.tryInstagramReelsBack(reels),"native Reels boundary overrides false forwarding veto");
            };
            InstagramBack.request(s.activity);check(s.activity.nativeBackCalls==1,"native call occurs once");removed(s,task);
        }});
        for(final boolean popup:new boolean[]{true,false})cases.put("native_consumed_"+(popup?"popup":"sheet")+"_returns_before_bridge",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);int[] hooks={0};ActivityManager.AppTask task=own(s);
            if(popup){Object state=s.create("X.21x");set(state,"A05",true);set(reels,"A1Y",state);}
            else Back122InstagramDispatcherTest.layer(s,reels,true,s.create("X.NativeProfile"));
            check(nativeHandler(reels,hooks),"native popup/sheet consumes Back");
            check(hooks[0]==0,"native early return does not enter the exact exit hook");preserved(s,task);
            check(nativeHandler(reels,hooks)&&hooks[0]==1,"next independent native Back reaches the hook");removed(s,task);
        }});
        cases.put("native_receiver_rejects_wrong_type_null_and_missing_owner",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);Node detail=s.node("X.NativeProfile",null,MAIN);ActivityManager.AppTask task=own(s);
            check(!OneBackExit.tryInstagramReelsBack(detail)&&!OneBackExit.tryInstagramReelsBack(new Object())
                    &&!OneBackExit.tryInstagramReelsBack(null),"only an actual Reels handler receiver is accepted");
            reels.activity=null;check(!OneBackExit.tryInstagramReelsBack(reels),"detached fragment with no Activity stays native");preserved(s,task);
        }});
        cases.put("native_receiver_rejects_foreign_or_dead_owner",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);ActivityManager.AppTask task=own(s);
            Activity foreign=(Activity)s.activity.getClass().getConstructor(Application.class,int.class,boolean.class)
                    .newInstance(new Application("com.example.other"),31,true);
            reels.activity=foreign;check(!OneBackExit.tryInstagramReelsBack(reels),"foreign package rejected");
            reels.activity=s.activity;s.activity.destroyed=true;check(!OneBackExit.tryInstagramReelsBack(reels),"destroyed owner rejected");
            s.activity.destroyed=false;s.activity.finishing=true;check(!OneBackExit.tryInstagramReelsBack(reels),"finishing owner rejected");
            s.activity.finishing=false;preserved(s,task);
        }});
        cases.put("native_receiver_rejects_worker_thread",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);ActivityManager.AppTask task=own(s);boolean[] result={true};
            Thread thread=new Thread(()->result[0]=OneBackExit.tryInstagramReelsBack(reels));thread.start();thread.join();
            check(!result[0],"only main-thread native Back may remove the task");preserved(s,task);
        }});
        return cases;
    }
    public static void main(String[] args)throws Exception {
        int oldCases=cases,oldAssertions=assertions;
        for(Map.Entry<String,Test> test:scenarios().entrySet())test(test.getKey(),test.getValue());
        System.out.println("PASS Back123NativeReelsTest: "+(cases-oldCases)+" cases, "+(assertions-oldAssertions)+" assertions");
    }
}
