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
import static app.hiro.oneback.runtime.InstagramProfileReels137Fixture.*;

/** AndroidX keeps its original body. Normal body completion, after its native
 * pop, is the only point that schedules the profile-to-Reels observation. */
public final class Back133InstagramDispatcherProfileTest {
    static Map<String,Test> scenarios(){
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final String host:new String[]{"","url","launcher"}){
            final String name=host.isEmpty()?"main":host;
            tests.put(name+"_native_dispatch_pop_then_reels",()->{try(Scene s=new Scene(host)){
                Node p=linked(s);ActivityManager.AppTask t=own(s);dispatchToReels(s,p,t,dispatcher(s));
                check(p.profileBackCalls==1,"only the original native handler ran");check(s.activity.nativeBackCalls==0,"no synthetic Activity Back");
            }});
            tests.put(name+"_native_scope_and_dispatch_coalesce",()->{try(Scene s=new Scene(host)){
                Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);Destination destination=new Destination(s);
                OneBackExit.enterBack(s.activity);
                try{check(!OneBackExit.tryInstagramDispatcherBack(d),"nested dispatcher leaves original pop authoritative");
                    check(!profileBack(p),"native profile result remains false");remove(s,p);
                    OneBackExit.completeInstagramDispatcherBack(d);OneBackExit.afterNative();}
                finally{OneBackExit.leaveNative();}
                Handler.drain();noTaskRemoval(t);
                if(isMain(s)){check(s.activity.selectClipsCalls==1,"nested completion selects once");nextReelsBack(s,destination,t);}
                else expectedIntent(s);
            }});
        }
        tests.put("warm_main_modal_dispatch",()->{try(ModalScene f=new ModalScene()){
            dispatchToReels(f.scene,f.profile,f.task,dispatcher(f.scene));check(f.profile.profileBackCalls==1,"modal original handler once");
        }});
        tests.put("native_consumed_then_separate_back",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);p.profileBackConsumed=true;
            check(!OneBackExit.tryInstagramDispatcherBack(d),"native handler runs in original dispatcher body");
            check(profileBack(p),"consumed native result preserved");OneBackExit.completeInstagramDispatcherBack(d);Handler.drain();
            check(s.activity.selectClipsCalls==0,"consumption leaves profile attached");preserved(s,t);
            p.profileBackConsumed=false;dispatchToReels(s,p,t,d);check(p.profileBackCalls==2,"one native handler per Back");
        }});
        tests.put("internal_profile_stays_native",()->{try(Scene s=new Scene()){
            Node p=detached(s);attach(s,p);s.profileTabs(p,true);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            check(!OneBackExit.tryInstagramDispatcherBack(d),"unregistered profile not intercepted");
            remove(s,p);OneBackExit.completeInstagramDispatcherBack(d);Handler.drain();
            check(p.profileBackCalls==0&&s.activity.selectClipsCalls==0,"no registration or synthetic callback");preserved(s,t);
        }});
        tests.put("child_return_cannot_promote_same_dispatch",()->{try(Scene s=new Scene()){
            Node p=linked(s);hide(p);Node child=s.node("X.NativeProfile",null,MAIN);s.select(child,true);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            check(!OneBackExit.tryInstagramDispatcherBack(d),"child pop remains native");erase(s,child);reveal(s,p);
            OneBackExit.completeInstagramDispatcherBack(d);Handler.drain();
            check(p.profileBackCalls==0&&s.activity.selectClipsCalls==0,"child completion does not navigate the revealed entry");preserved(s,t);
            dispatchToReels(s,p,t,d);
        }});
        tests.put("predictive_cancel_has_no_native_action",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            check(!OneBackExit.startInstagramDispatcherGesture(d),"profile prediction stays native");
            check(!OneBackExit.progressInstagramDispatcherGesture(d),"profile progress stays native");
            check(!OneBackExit.cancelInstagramDispatcherGesture(d),"native cancellation retained");Handler.drain();
            check(p.profileBackCalls==0&&s.activity.selectClipsCalls==0,"cancel cannot navigate");preserved(s,t);dispatchToReels(s,p,t,d);
        }});
        tests.put("predictive_commit_waits_for_original_body",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            check(!OneBackExit.startInstagramDispatcherGesture(d),"native preparation is not suppressed");
            check(p.profileBackCalls==0,"start is observation only");preserved(s,t);dispatchToReels(s,p,t,d);
        }});
        tests.put("changed_target_during_native_handler_is_not_double_pop",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            p.profileBackAction=()->{try{hide(p);Node child=s.node("X.NativeProfile",null,MAIN);s.select(child,true);}catch(Exception e){throw new AssertionError(e);}};
            check(!OneBackExit.tryInstagramDispatcherBack(d),"original body retained");check(!profileBack(p),"changed selection preserves native result");
            OneBackExit.completeInstagramDispatcherBack(d);Handler.drain();
            check(p.profileBackCalls==1&&s.activity.nativeBackCalls==0&&s.activity.selectClipsCalls==0,"retained hidden entry is not removal");preserved(s,t);
        }});
        for(final String kind:new String[]{"ime","comments"})tests.put(kind+"_closes_before_later_profile_return",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            final View panel=kind.equals("comments")?s.group((ViewGroup)p.view,"comment_sheet",new Rect(0,650,1080,2200)):null;
            if(panel==null)s.root.insets.ime=true;
            s.activity.nativeBackAction=()->{s.root.insets.ime=false;if(panel!=null)((ViewGroup)p.view).removeView(panel);profileBack(p);};
            if(!OneBackExit.tryInstagramDispatcherBack(d))s.activity.onBackPressed();
            OneBackExit.completeInstagramDispatcherBack(d);Handler.drain();
            check(p.profileBackCalls==1&&s.activity.selectClipsCalls==0,"layer closes without a Reels handoff");preserved(s,t);
            s.activity.nativeBackAction=null;dispatchToReels(s,p,t,d);
        }});
        tests.put("recursive_dispatch_does_not_reinvoke_profile",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            p.profileBackAction=()->check(!OneBackExit.tryInstagramDispatcherBack(d),"recursive entry does not synthesize profile Back");
            dispatchToReels(s,p,t,d);check(p.profileBackCalls==1,"original native handler only once");
        }});
        tests.put("new_intent_invalidates_entry",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            OneBackExit.onNewIntent(s.activity,new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://www.instagram.com/reel/new/")));
            check(!OneBackExit.tryInstagramDispatcherBack(d),"stale profile stays native");remove(s,p);OneBackExit.completeInstagramDispatcherBack(d);Handler.drain();
            check(s.activity.selectClipsCalls==0,"stale entry cannot navigate after unrelated Intent");preserved(s,t);
        }});
        tests.put("native_exception_has_no_normal_completion",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            p.profileBackAction=()->{throw new IllegalStateException("fixture native failure");};
            check(!OneBackExit.tryInstagramDispatcherBack(d),"native body retains its exception semantics");
            try{profileBack(p);throw new AssertionError("expected native exception");}catch(IllegalStateException expected){}
            Handler.drain();check(s.activity.selectClipsCalls==0,"exception did not run completion observer");preserved(s,t);
            p.profileBackAction=null;dispatchToReels(s,p,t,d);
        }});
        tests.put("dialog_closes_before_next_back",()->{try(Scene s=new Scene()){
            Node p=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);Dialog dialog=new Dialog(s.activity);dialog.show();dialog.getWindow().getDecorView().focus(true);
            check(!OneBackExit.tryInstagramDispatcherBack(d),"dialog retains native dispatch");dialog.dismiss();s.root.focus(true);
            OneBackExit.completeInstagramDispatcherBack(d);Handler.drain();
            check(p.profileBackCalls==0&&s.activity.selectClipsCalls==0,"dialog closure did not remove entry");preserved(s,t);dispatchToReels(s,p,t,d);
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception{int beforeCases=cases,beforeAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> item:scenarios().entrySet())try{test(item.getKey(),item.getValue());}catch(AssertionError failure){failed++;System.out.println("FAIL "+item.getKey()+": "+failure.getMessage());Handler.drain();}
        if(failed!=0)throw new AssertionError("Back133InstagramDispatcherProfileTest failed="+failed);
        System.out.println("PASS Back133InstagramDispatcherProfileTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions; explicit native-body models, no device execution");}
}
