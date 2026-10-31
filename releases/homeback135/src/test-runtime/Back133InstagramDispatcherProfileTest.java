package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;
import static app.hiro.oneback.runtime.Back130InstagramProfileLinkTest.*;

/** Recovered host regressions for the retained317 dispatcher bridge.
 * The current profile's original handler is modeled by the fixture's existing
 * begin/native/finish wrapper. These tests do not simulate Android execution. */
public final class Back133InstagramDispatcherProfileTest {
    static Map<String,Test> scenarios() {
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final String host:new String[]{"","url","launcher"}) {
            final String name=host.isEmpty()?"main":host;
            tests.put(name+"_cold_dispatch_calls_native_profile_once",()->{try(Scene s=new Scene(host)){
                Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
                check(OneBackExit.tryInstagramDispatcherBack(d),"dispatcher consumes external profile Back");
                check(p.profileBackCalls==1,"existing profile handler executed once");
                check(s.activity.nativeBackCalls==0,"no second Activity Back synthesized");removed(s,t);
            }});
            tests.put(name+"_native_scope_dispatch_exits",()->{try(Scene s=new Scene(host)){
                Node p=linked(s);set(s.manager,"count",1);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
                OneBackExit.enterBack(s.activity);
                try{check(OneBackExit.tryInstagramDispatcherBack(d),"same initial native scope dispatches profile");}
                finally{OneBackExit.leaveNative();}
                check(p.profileBackCalls==1,"one native profile call in parent scope");removed(s,t);
            }});
        }
        tests.put("warm_main_modal_dispatch",()->{try(ModalScene f=new ModalScene()){
            check(OneBackExit.tryInstagramDispatcherBack(dispatcher(f.scene)),"live modal profile bridge handled");
            check(f.profile.profileBackCalls==1,"registered modal profile executed once");removed(f.scene,f.task);
        }});
        tests.put("native_consumed_then_separate_back",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);p.profileBackConsumed=true;
            check(OneBackExit.tryInstagramDispatcherBack(d),"native consumed response handled");
            check(p.profileBackCalls==1,"native action once");preserved(s,t);
            Handler.drain();p.profileBackConsumed=false;
            check(OneBackExit.tryInstagramDispatcherBack(d),"separate unconsumed Back handled");
            check(p.profileBackCalls==2,"one handler per separate Back");removed(s,t);
        }});
        tests.put("internal_profile_stays_native",()->{try(Scene s=new Scene()){
            Node p=detached(s);attach(s,p);s.profileTabs(p,true);ActivityManager.AppTask t=own(s);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"unregistered profile not intercepted");
            check(p.profileBackCalls==0,"unregistered native handler not synthesized");preserved(s,t);
        }});
        tests.put("child_return_cannot_promote_same_dispatch",()->{try(Scene s=new Scene()){
            Node p=linked(s);hide(p);Node child=s.node("X.NativeProfile",null,MAIN);child.module="media_detail";s.select(child,true);
            ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            check(!OneBackExit.tryInstagramDispatcherBack(d),"child navigation remains native");
            erase(s,child);reveal(s,p);
            check(!OneBackExit.tryInstagramDispatcherBack(d),"same synchronous return does not invoke entry");
            check(p.profileBackCalls==0,"no profile handler during completed child pop");preserved(s,t);
            Handler.drain();check(OneBackExit.tryInstagramDispatcherBack(d),"independent next Back dispatches entry");removed(s,t);
        }});
        tests.put("predictive_cancel_has_no_native_action",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            check(OneBackExit.startInstagramDispatcherGesture(d),"manager prediction suppressed");
            check(OneBackExit.progressInstagramDispatcherGesture(d),"suppression remains paired");
            check(OneBackExit.cancelInstagramDispatcherGesture(d),"cancel suppressed prediction");
            check(p.profileBackCalls==0&&s.activity.nativeBackCalls==0,"cancel has no native action");preserved(s,t);
            Handler.drain();check(OneBackExit.tryInstagramDispatcherBack(d),"new Back after cancellation works");removed(s,t);
        }});
        tests.put("predictive_commit_waits_for_native_profile",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            check(OneBackExit.startInstagramDispatcherGesture(d),"prevents predictive fragment pop");
            check(p.profileBackCalls==0,"start cannot invoke native handler");preserved(s,t);
            check(OneBackExit.tryInstagramDispatcherBack(d),"commit runs profile native guard");
            check(p.profileBackCalls==1,"native handler exactly once at commit");removed(s,t);
        }});
        tests.put("changed_target_during_native_handler_is_not_double_pop",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            p.profileBackAction=()->{try{hide(p);Node next=s.node("X.NativeProfile",null,MAIN);next.module="media_detail";s.select(next,true);}catch(Exception e){throw new AssertionError(e);}};
            check(OneBackExit.tryInstagramDispatcherBack(d),"native execution consumes dispatcher even if target changes");
            check(p.profileBackCalls==1&&s.activity.nativeBackCalls==0,"no fallback pop after native execution");preserved(s,t);
        }});
        tests.put("ime_closes_before_next_back_exit",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);s.root.insets.ime=true;
            s.activity.nativeBackAction=()->{s.root.insets.ime=false;profileBack(p);};
            if(!OneBackExit.tryInstagramDispatcherBack(d))s.activity.onBackPressed();
            check(!s.root.insets.ime,"native keyboard closure ran");check(p.profileBackCalls==1,"one profile continuation for keyboard");preserved(s,t);
            Handler.drain();s.activity.nativeBackAction=null;check(OneBackExit.tryInstagramDispatcherBack(d),"later Back on bare profile exits");removed(s,t);
        }});
        tests.put("comments_close_before_next_back_exit",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            View panel=s.group((ViewGroup)p.view,"comment_sheet",new Rect(0,650,1080,2200));
            s.activity.nativeBackAction=()->{((ViewGroup)p.view).removeView(panel);profileBack(p);};
            if(!OneBackExit.tryInstagramDispatcherBack(d))s.activity.onBackPressed();
            check(p.profileBackCalls==1,"one profile continuation for comment closure");preserved(s,t);
            Handler.drain();s.activity.nativeBackAction=null;check(OneBackExit.tryInstagramDispatcherBack(d),"next independent Back exits");removed(s,t);
        }});
        tests.put("recursive_dispatch_does_not_reinvoke_profile",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            p.profileBackAction=()->check(!OneBackExit.tryInstagramDispatcherBack(d),"recursive bridge cannot invoke profile again");
            check(OneBackExit.tryInstagramDispatcherBack(d),"outer native invocation consumed");
            check(p.profileBackCalls==1,"recursive dispatch executes profile exactly once");
            removed(s,t);
        }});
        tests.put("new_intent_invalidates_entry",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            OneBackExit.onNewIntent(s.activity,new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://www.instagram.com/reel/new/")));
            check(!OneBackExit.tryInstagramDispatcherBack(d),"stale entry not dispatched after new route");
            check(p.profileBackCalls==0,"no stale native profile call");preserved(s,t);
        }});
        tests.put("native_exception_consumed_and_call_token_cleared",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            p.profileBackAction=()->{throw new IllegalStateException("fixture native failure");};
            check(OneBackExit.tryInstagramDispatcherBack(d),"invoked native exception does not trigger another pop");
            check(p.profileBackCalls==1,"failing native handler ran only once");preserved(s,t);
            Handler.drain();p.profileBackAction=null;
            check(OneBackExit.tryInstagramDispatcherBack(d),"next independent Back can dispatch after exception");
            check(p.profileBackCalls==2,"stale call token removed");removed(s,t);
        }});
        tests.put("dialog_closes_before_next_back_exit",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);Dialog dialog=new Dialog(s.activity);
            dialog.show();dialog.getWindow().getDecorView().focus(true);
            check(!OneBackExit.tryInstagramDispatcherBack(d),"dialog does not route to bare profile bridge");
            check(p.profileBackCalls==0,"profile handler not invoked behind focused dialog");
            dialog.dismiss();s.root.focus(true);preserved(s,t);
            Handler.drain();check(OneBackExit.tryInstagramDispatcherBack(d),"separate Back after dialog exit runs profile");removed(s,t);
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception {
        int beforeCases=cases,beforeAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> item:scenarios().entrySet())try{test(item.getKey(),item.getValue());}
        catch(AssertionError failure){failed++;System.out.println("FAIL "+item.getKey()+": "+failure.getMessage());Handler.drain();}
        if(failed!=0)throw new AssertionError("Back133InstagramDispatcherProfileTest failed="+failed);
        System.out.println("PASS Back133InstagramDispatcherProfileTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions; recovered host models, no device execution");
    }
}
