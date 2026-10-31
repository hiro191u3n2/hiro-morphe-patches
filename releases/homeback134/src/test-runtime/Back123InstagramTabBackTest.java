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

/** Native code reaches this hook only after its current fragment declined Back
 * and its tab stack has another entry. These tests exercise that exact runtime
 * contract; bytecode control-flow tests separately prove the hook placement. */
public final class Back123InstagramTabBackTest {
    static boolean invoke(Scene s,Node initial){return OneBackExit.tryInstagramTabBack(s.activity,s.controller,initial);}
    static Map<String,Test> scenarios(){Map<String,Test> cases=new LinkedHashMap<String,Test>();
        cases.put("native_clips_exit_excludes_then_removes_task",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);ActivityManager.AppTask task=own(s);
            check(invoke(s,initial),"native unchanged CLIPS tab is handled");removed(s,task);
            check(!invoke(s,initial),"finishing Activity cannot handle another exit");
        }});
        cases.put("native_tab_boundary_ignores_unknown_view_and_lifecycle_heuristics",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);initial.resumed=false;initial.visible=false;initial.view.alpha=0;
            s.root.insets.ime=true;s.root.focus(false);ActivityManager.AppTask task=own(s);
            check(!HomeScreen.inputReady(s.activity)&&InstagramBack.action(s.activity)==0,"the generic input/visibility classifier deliberately refuses this model");
            check(invoke(s,initial),"native app's unconsumed tab Back remains authoritative");removed(s,task);
        }});
        cases.put("native_tab_boundary_ignores_false_layer_guard_and_origin",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);s.group((ViewGroup)initial.view,"comments_sheet",new Rect(0,900,1080,2200));
            ActivityManager.AppTask task=own(s);OneBackExit.enterBack(s.activity);
            try {
                check(InstagramBack.action(s.activity)==3,"old heuristic classified a false layer and protects this native transaction");
                check(invoke(s,initial),"native tab-pop decision overrides old layer/origin veto");removed(s,task);
            }finally{OneBackExit.leaveNative();}
        }});
        cases.put("native_tab_boundary_handles_internal_forwarding_without_recursion",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);s.group((ViewGroup)initial.view,"comments_sheet",new Rect(0,900,1080,2200));ActivityManager.AppTask task=own(s);
            s.activity.nativeBackAction=()->{
                check(InstagramBack.forwarding(),"fixture reaches actual internal native forwarding state");
                check(invoke(s,initial),"native unconsumed CLIPS Back can exit despite forwarding flag");
            };
            InstagramBack.request(s.activity);check(s.activity.nativeBackCalls==1,"native handler invoked once");removed(s,task);
        }});
        cases.put("native_tab_boundary_clears_false_predictive_action",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);View panel=s.group((ViewGroup)initial.view,"comments_sheet",new Rect(0,900,1080,2200));
            InstagramBack.predictionStart(s.activity);((ViewGroup)initial.view).removeView(panel);ActivityManager.AppTask task=own(s);
            check(InstagramBack.action(s.activity)==3,"prediction still reflects the old false layer");
            check(invoke(s,initial),"native unchanged CLIPS receiver bypasses a stale generic prediction");removed(s,task);
        }});
        cases.put("native_other_tab_keeps_native_stack_pop",()->{try(Scene s=new Scene()){
            Node initial=current(s,true);ActivityManager.AppTask task=own(s);
            check(!invoke(s,initial),"FEED is not CLIPS");preserved(s,task);
        }});
        cases.put("native_changed_current_receiver_keeps_native",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);Node replacement=s.node("X.8Zv",null,MAIN);s.select(replacement,false);ActivityManager.AppTask task=own(s);
            check(!invoke(s,initial),"a callback that changes current fragment cannot authorize the original receiver");preserved(s,task);
        }});
        cases.put("native_wrong_activity_owner_keeps_native",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);Activity other=(Activity)s.activity.getClass().getConstructor(Application.class,int.class,boolean.class).newInstance(s.app,99,true);
            set(s.controller,"A03",other);ActivityManager.AppTask task=own(s);
            check(!invoke(s,initial),"navigation owner must be this Activity");preserved(s,task);
        }});
        for(final String fault:new String[]{"tabback_current_nonfinal","tabback_current_throws"})cases.put("native_contract_failure_"+fault,()->{try(Scene s=new Scene(fault)){
            Node initial=current(s,false);ActivityManager.AppTask task=own(s);
            check(!invoke(s,initial),"changed or throwing native current getter stays native");preserved(s,task);
        }});
        cases.put("native_null_inputs_and_wrong_controller_keep_native",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);ActivityManager.AppTask task=own(s);
            check(!OneBackExit.tryInstagramTabBack(s.activity,null,initial),"null navigation cannot authorize exit");
            check(!OneBackExit.tryInstagramTabBack(s.activity,new Object(),initial),"unverified controller cannot authorize exit");
            check(!OneBackExit.tryInstagramTabBack(s.activity,s.controller,null),"null original receiver cannot authorize exit");preserved(s,task);
        }});
        cases.put("native_destroyed_or_finishing_activity_keeps_native",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);ActivityManager.AppTask task=own(s);
            s.activity.destroyed=true;check(!invoke(s,initial),"destroyed Activity rejected");s.activity.destroyed=false;
            s.activity.finishing=true;check(!invoke(s,initial),"finishing Activity rejected");s.activity.finishing=false;preserved(s,task);
        }});
        cases.put("native_nonmain_thread_keeps_native",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);ActivityManager.AppTask task=own(s);final boolean[] result={true};
            Thread thread=new Thread(()->result[0]=invoke(s,initial));thread.start();thread.join();
            check(!result[0],"only main-thread native Back is handled");preserved(s,task);
        }});
        cases.put("native_foreign_package_is_never_exited",()->{try(Scene s=new Scene()){
            Node initial=current(s,false);Activity foreign=new Activity(new Application("com.example.other"),31,true);
            check(!OneBackExit.tryInstagramTabBack(foreign,s.controller,initial),"foreign package rejected");
            check(foreign.finishCalls==0&&foreign.removeCalls==0,"foreign Activity untouched");
        }});
        return cases;
    }
    public static void main(String[] args)throws Exception {
        int oldCases=cases,oldAssertions=assertions;
        for(Map.Entry<String,Test> test:scenarios().entrySet())test(test.getKey(),test.getValue());
        System.out.println("PASS Back123InstagramTabBackTest: "+(cases-oldCases)+" cases, "+(assertions-oldAssertions)+" assertions");
    }
}
