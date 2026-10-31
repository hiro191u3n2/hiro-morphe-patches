package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;
import static app.hiro.oneback.runtime.Back130InstagramProfileLinkTest.*;

/** Diagnostic reporting must observe the retained319 decisions. Host models
 * verify callback results and task effects alongside coded-only output; the
 * separate source inverse verifies that original executable decisions remain. */
public final class Back136InstagramDiagnosticTest {
    static void reset(){Handler.drain();Toast.SHOWN.clear();Toast.fail=false;}
    static String report(){Handler.drain();check(Toast.SHOWN.size()==1,"one report for the existing synchronous Back callback");String code=Toast.SHOWN.get(0);
        check(code.matches("HB320 H[MULO] R[0-9]+ E[-01] S(?:-|[0-9]+)/(?:-|[0-9]+) P(?:-|[0-9]+) C[-MBO] Q[-01]\\nA[0-9]+ D[0-9]+ N[-01] F(?:-|[0-9]+) O[0-9]+ T[01] W[01] B(?:-|[0-9]+)"),"only the bounded diagnostic alphabet is displayed");return code;}
    static void has(String code,String token){check(code.contains(token),"diagnostic token "+token+" in "+code);}
    @SuppressWarnings("unchecked") static Map<Activity,?> states()throws Exception{Field f=InstagramBackTrace.class.getDeclaredField("STATES");f.setAccessible(true);return (Map<Activity,?>)f.get(null);}
    static Map<String,Test> scenarios(){Map<String,Test> out=new LinkedHashMap<String,Test>();
        out.put("registered_exit_is_unchanged_and_private",()->{reset();try(Scene s=new Scene()){
            String user="private_account_42",url="https://www.instagram.com/"+user+"/?utm_source=private_campaign_17";
            Node profile=linked(s,route(s,url,user));profile.view.description="private visible profile text";ActivityManager.AppTask task=own(s);
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"retained319 registered entry dispatches");
            check(profile.profileBackCalls==1,"native profile handler still runs once");removed(s,task);
            String code=report();has(code," R1 E1 ");has(code," P0 CM Q1");has(code," D1 N0 F3 ");has(code," B0");
            check(!code.contains(user)&&!code.contains(url)&&!code.contains("private")&&!code.contains("instagram"),"no route, account, content or class name in report");
            Object state=states().get(s.activity);if(state!=null)for(Field f:state.getClass().getDeclaredFields())if(f.getType()==String.class){f.setAccessible(true);String value=(String)f.get(state);check(value==null||value.matches("[-MUBLO]"),"trace state retains only bounded host/container strings");}
        }});
        out.put("native_consumption_remains_consumption",()->{reset();try(Scene s=new Scene()){
            Node p=linked(s);p.profileBackConsumed=true;ActivityManager.AppTask task=own(s);
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"native consumed Back remains handled");check(p.profileBackCalls==1,"single native handler");preserved(s,task);
            has(report()," D1 N1 F1 ");
        }});
        out.put("missing_registration_remains_native",()->{reset();try(Scene s=new Scene()){
            Node p=detached(s);attach(s,p);s.profileTabs(p,true);ActivityManager.AppTask task=own(s);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"unregistered entry remains native");check(p.profileBackCalls==0,"diagnostic does not call profile handler");preserved(s,task);
            String code=report();has(code," R0 E0 ");has(code," D3 N- F- ");
        }});
        out.put("root_base_still_rejected_with_code",()->{reset();try(Back135InstagramInitialProfileTest.Initial f=new Back135InstagramInitialProfileTest.Initial()){
            set(f.profile,"mFragmentId",BASE);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher(f.s)),"diagnostic does not add the unproven rootBASE fix");
            check(f.profile.profileBackCalls==0,"rootBASE remains outside existing profile dispatch");preserved(f.s,f.task);
            String code=report();has(code," R1 E1 S7/7 P0 CB Q-");has(code," D3 N- F- ");
        }});
        for(final String rejection:new String[]{"internal","envelope","url","uri","special"})out.put("registration_reason_"+rejection,()->{reset();try(Scene s=new Scene()){
            Bundle route=route(s,URL,"yoasobi_staff_");int reason;
            if(rejection.equals("internal")){route.putBoolean("InstagramUrlLaunchConstants.EXTRA_IS_INTERNAL_LINK",true);reason=4;}
            else if(rejection.equals("envelope")){route.putString("com.instagram.url.constants.ARGUMENTS_KEY_ANALYTICS_MODULE_NAME","wrong");reason=5;}
            else if(rejection.equals("url")){route.putString("original_url","https://example.org/private_account/");reason=6;}
            else if(rejection.equals("uri")){route.fixtureObject("uri",Uri.parse(URL+"?igsh=private"));reason=7;}
            else{route.putString("InstagramUrlLaunchConstants.EXTRA_STARTING_TAB","private_tab");reason=8;}
            Node p=detached(s);OneBackExit.rememberInstagramExternalProfile(s.activity,p,route);attach(s,p);s.profileTabs(p,true);ActivityManager.AppTask task=own(s);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"rejected route remains native");preserved(s,task);String code=report();has(code," R"+reason+" E0 ");check(!code.contains("private"),"rejection does not expose rejected input");
        }});
        out.put("nested_callbacks_coalesce",()->{reset();try(Scene s=new Scene()){
            Node p=linked(s);p.profileBackConsumed=true;ActivityManager.AppTask task=own(s);Object d=dispatcher(s);
            OneBackExit.enterBack(s.activity);try{check(OneBackExit.tryInstagramDispatcherBack(d),"nested dispatcher uses native profile once");}finally{OneBackExit.leaveNative();}
            check(p.profileBackCalls==1,"coalescing never replays native action");preserved(s,task);has(report(),"\nA7 D1 N1 F1 ");
            Toast.SHOWN.clear();check(OneBackExit.tryInstagramDispatcherBack(d),"next independent callback remains handled");has(report(),"\nA6 D1 N1 F1 ");check(p.profileBackCalls==2,"one native call per separate callback");
        }});
        out.put("new_intent_clears_history_but_keeps_queued_report",()->{reset();try(Scene s=new Scene()){
            Node p=linked(s);p.profileBackConsumed=true;ActivityManager.AppTask task=own(s);Object d=dispatcher(s);
            check(OneBackExit.tryInstagramDispatcherBack(d),"first native consumed callback");OneBackExit.onNewIntent(s.activity,new Intent());
            check(!states().containsKey(s.activity),"new Intent clears diagnostic state");has(report()," R1 E1 ");
            Toast.SHOWN.clear();check(!OneBackExit.tryInstagramDispatcherBack(d),"new Intent also retains319 provenance reset");has(report()," R0 E0 ");preserved(s,task);
        }});
        out.put("same_intent_dedup_preserves_registration_code",()->{reset();try(Scene s=new Scene()){
            Intent intent=new Intent();OneBackExit.onNewIntent(s.activity,intent);Node p=linked(s);p.profileBackConsumed=true;
            OneBackExit.onNewIntent(s.activity,intent);check(OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"duplicate inherited Intent notification retains existing entry");has(report()," R1 E1 ");
        }});
        out.put("destroy_clears_map_without_losing_completed_report",()->{reset();try(Scene s=new Scene()){
            Node p=linked(s);p.profileBackConsumed=true;check(OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"callback completed before destroy");
            OneBackExit.onDestroy(s.activity);check(!states().containsKey(s.activity),"destroy clears trace map");has(report()," R1 E1 ");
        }});
        out.put("toast_failure_preserves_consumption_and_recovers",()->{reset();try(Scene s=new Scene()){
            Node p=linked(s);p.profileBackConsumed=true;ActivityManager.AppTask task=own(s);Object d=dispatcher(s);Toast.fail=true;
            check(OneBackExit.tryInstagramDispatcherBack(d),"Toast failure cannot affect Back result");Handler.drain();check(Toast.SHOWN.isEmpty(),"failed Toast produces no report");preserved(s,task);check(p.profileBackCalls==1,"native consumption not retried");
            Toast.fail=false;check(OneBackExit.tryInstagramDispatcherBack(d),"next callback unaffected by prior Toast failure");has(report()," N1 F1 ");check(p.profileBackCalls==2,"one handler for next callback");
        }finally{Toast.fail=false;}});
        out.put("toast_failure_preserves_exit",()->{reset();try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask task=own(s);Toast.fail=true;
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"exit result unchanged when Toast unavailable");removed(s,task);Handler.drain();check(Toast.SHOWN.isEmpty()&&p.profileBackCalls==1,"exit did not depend on report delivery");
        }finally{Toast.fail=false;}});
        out.put("foreign_activity_never_reports",()->{reset();Activity a=new Activity(new Application("com.example.browser"),31,true);
            OneBackExit.enterBack(a);try{check(a.nativeBackCalls==0,"diagnostic does not invoke foreign Back");}finally{OneBackExit.leaveNative();}
            Handler.drain();check(Toast.SHOWN.isEmpty(),"no report from another application");check(!states().containsKey(a),"foreign Back creates no trace state");
        });
        out.put("prediction_and_cancel_do_not_report",()->{reset();try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask task=own(s);Object d=dispatcher(s);
            check(OneBackExit.startInstagramDispatcherGesture(d),"existing entry suppresses native predictive pop");check(OneBackExit.progressInstagramDispatcherGesture(d),"progress keeps existing suppression");check(OneBackExit.cancelInstagramDispatcherGesture(d),"cancel keeps existing suppression");
            Handler.drain();check(Toast.SHOWN.isEmpty(),"prediction/start/progress/cancel cannot schedule a report");check(p.profileBackCalls==0,"prediction never invokes native handler");preserved(s,task);
        }});
        out.put("input_rejection_is_reported_without_changing_back",()->{reset();try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask task=own(s);s.root.insets.ime=true;
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"IME Back remains native");check(p.profileBackCalls==0,"diagnostic does not bypass keyboard");preserved(s,task);has(report()," S1/1 ");
        }});
        out.put("snapshot_bits_are_coded_without_new_inspection",()->{reset();try(Scene s=new Scene()){
            InstagramBackTrace.begin(s.activity,1);InstagramBack.Snapshot snapshot=new InstagramBack.Snapshot();snapshot.blocked=true;snapshot.otherContent=true;
            InstagramBackTrace.layers(s.activity,snapshot);has(report()," B5");
            Toast.SHOWN.clear();InstagramBackTrace.begin(s.activity,1);snapshot.blocked=false;snapshot.otherContent=false;snapshot.unknown=true;
            InstagramBackTrace.scene(s.activity,new Object(),snapshot);has(report()," B34");
        }});
        out.put("identity_wrappers_preserve_all_values",()->{reset();try(Scene s=new Scene()){
            Object value=new Object();check(InstagramBackTrace.observeParent(s.activity,value)==value,"object identity preserved");check(InstagramBackTrace.observeParent(s.activity,null)==null,"null preserved");
            check(InstagramBackTrace.observeContainer(s.activity,937)==937,"unknown container value preserved");
            for(boolean flag:new boolean[]{false,true}){check(InstagramBackTrace.observeReject(s.activity,7,flag)==flag,"reject boolean preserved");check(InstagramBackTrace.observeAllow(s.activity,7,flag)==flag,"allow boolean preserved");check(InstagramBackTrace.observeMismatch(s.activity,flag)==flag,"comparison boolean preserved");}
            Handler.drain();check(Toast.SHOWN.isEmpty(),"observation alone never schedules a report");
        }});
        return out;
    }
    public static void main(String[] args)throws Exception{int beforeCases=cases,beforeAssertions=assertions;for(Map.Entry<String,Test> e:scenarios().entrySet())test(e.getKey(),e.getValue());System.out.println("PASS Back136InstagramDiagnosticTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions; coded-only reports, retained319 Back results, lifecycle isolation, no predictive or foreign reports");}
}
