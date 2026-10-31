package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.Back124TwitterTest.*;

/** Models actual API33+ entries, preserving the registered AndroidX/Compose
 * callback. Only its verified platform fallback may visit X's native p/l1
 * route. Actual APK contracts and failing v124 reproductions are retained.
 */
public final class Back125TwitterSystemTest {
    static void retainedEditor(Scene s){
        View editor=new View(s.activity);editor.editor=true;editor.hidden=true;s.root.focusView=editor;
    }
    static void nativeEntry(Scene s){
        // The emitted exact p.onBackPressed wrapper uses enterTwitterBack.
        s.activity.nativeBackAction=()->{OneBackExit.enterTwitterBack(s.activity);try{s.nativeBack();OneBackExit.afterNative();}finally{OneBackExit.leaveNative();}};
    }
    static OnBackInvokedCallback fallback(Scene s,int[] calls){return ()->{
        calls[0]++;
        // l0.d -> Runnable o -> ComponentActivity.access$onBackPressed...
        // -> existing terminal$0 bridge. No native l1 call before this point.
        if(!OneBackExit.tryTerminalBack(s.activity,0,false))s.activity.finish();
    };}
    static void top(Scene s,boolean resumeAfterRegistration){
        OnBackInvokedCallback before=s.activity.getWindow().dispatcher().top();
        check(before!=null,"native decorator is registered before testing callback order");
        if(resumeAfterRegistration)OneBackExit.onResume(s.activity);
        check(s.activity.getWindow().dispatcher().top()==before&&s.activity.getWindow().dispatcher().count()==1,
            "native callback retains sole registration even after lifecycle resume");
    }
    static final class TransactionalDispatcher implements OnBackInvokedDispatcher {
        final android.window.FakeDispatcher platform=new android.window.FakeDispatcher();
        boolean rejectRegister,rejectUnregister;
        public void registerOnBackInvokedCallback(int priority,OnBackInvokedCallback callback){
            if(rejectRegister)throw new IllegalStateException("registration rejected");
            platform.registerOnBackInvokedCallback(priority,callback);
        }
        public void unregisterOnBackInvokedCallback(OnBackInvokedCallback callback){
            if(rejectUnregister)throw new IllegalStateException("unregistration rejected");
            platform.unregisterOnBackInvokedCallback(callback);
        }
    }
    static Map<String,Test> scenarios(){Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(String route:TABS){final String tab=route;
            tests.put("x125_observer_without_androidx_callback_"+tab,()->{try(Scene s=new Scene(tab)){
                nativeEntry(s);retainedEditor(s);OneBackExit.onResume(s.activity);
                check(!HomeScreen.inputReady(s.activity)&&HomeScreen.twitterInputReady(s.activity),"observer ignores retained editor but keeps native exit heuristics separate");
                check(s.activity.getWindow().dispatcher().top()!=null,"focused Main registers a real observer even with no AndroidX callback");
                s.activity.getWindow().dispatcher().invokeBack();
                check(s.activity.nativeBackCalls==1&&s.hookCalls==1,"otherwise-unhandled system Back reaches public native Main entry");
                check(marker(s.app),"default system route persists next-launch Home marker");s.removed();
            }});
        }
        for(boolean owner:new boolean[]{false,true})for(String route:TABS){
            final boolean resumeAfterRegistration=owner;final String tab=route;
            tests.put("x125_system_fallback_root_"+resumeAfterRegistration+"_"+tab,()->{
                Disk disk=new Disk();ProcessApp app=new ProcessApp(disk);
                try(Scene s=new Scene(app,tab,null,false)){
                    nativeEntry(s);retainedEditor(s);
                    check(!HomeScreen.inputReady(s.activity),"retained hidden editor reproduces rejected pre-native eligibility");
                    int[] calls={0};OnBackInvokedCallback original=fallback(s,calls);
                    NativeBack.register(s.activity.getWindow().dispatcher(),0,original);top(s,resumeAfterRegistration);
                    try{s.activity.getWindow().dispatcher().invokeBack();
                        check(calls[0]==1,"original AndroidX callback runs first exactly once");
                        check(s.activity.nativeBackCalls==1&&s.hookCalls==1,"platform fallback visits exact Main's native handler once");
                        s.removed();check(marker(app),"native exit persists Home marker despite hidden-editor heuristic");
                    }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
                }
                ProcessApp next=new ProcessApp(disk);
                try(Scene s=new Scene(next,tab,launcher(),true)){
                    check(HOME.equals(s.current),"next process ordinary launcher selects Home through native page extra");
                    Handler.drain();check(!marker(next),"Home marker consumed after Home observation");s.preserved();
                }
            });
        }
        for(boolean owner:new boolean[]{false,true})for(String layer:new String[]{"fab","drawer","typeahead","onboarding","child"}){
            final boolean resumeAfterRegistration=owner;final String guard=layer;
            tests.put("x125_system_fallback_native_guard_"+resumeAfterRegistration+"_"+guard,()->{try(Scene s=new Scene(CHAT)){
                nativeEntry(s);
                if(guard.equals("fab"))s.fab=true;else if(guard.equals("drawer"))s.drawer=true;
                else if(guard.equals("typeahead"))s.typeahead=true;
                else if(guard.equals("onboarding"))s.onboarding=true;else s.child=true;
                int[] calls={0};OnBackInvokedCallback original=fallback(s,calls);
                NativeBack.register(s.activity.getWindow().dispatcher(),0,original);top(s,resumeAfterRegistration);
                try{s.activity.getWindow().dispatcher().invokeBack();
                    check(calls[0]==1&&s.activity.nativeBackCalls==1&&s.hookCalls==0,"native layer/child consumes fallback before root gate");
                    check(!marker(s.app),"consumed native Back creates no Home marker");s.preserved();
                }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
            }});
        }
        for(boolean owner:new boolean[]{false,true}){
            final boolean resumeAfterRegistration=owner;
            tests.put("x125_system_enabled_compose_child_"+resumeAfterRegistration,()->{try(Scene s=new Scene(CHAT)){
                nativeEntry(s);int[] calls={0};OnBackInvokedCallback original=()->calls[0]++;
                NativeBack.register(s.activity.getWindow().dispatcher(),0,original);top(s,resumeAfterRegistration);
                try{s.activity.getWindow().dispatcher().invokeBack();
                    check(calls[0]==1&&s.activity.nativeBackCalls==0&&s.hookCalls==0,"enabled same-window Compose handler consumes before native p/l1");
                    check(!marker(s.app),"child consumption never sets next-launch marker");s.preserved();
                }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
            }});
        }
        tests.put("x125_observer_returns_immediately_after_last_native_unregister",()->{try(Scene s=new Scene(SEARCH)){
            nativeEntry(s);android.window.FakeDispatcher d=s.activity.getWindow().dispatcher();
            OnBackInvokedCallback first=()->{},second=()->{};
            NativeBack.register(d,0,first);NativeBack.register(d,0,second);
            check(NativeBack.hasAppCallbacks(d)&&d.count()==2,"observer yields while both native callbacks are registered");
            NativeBack.unregister(d,second);
            check(NativeBack.hasAppCallbacks(d)&&d.count()==1,"removing one callback does not add an observer");
            NativeBack.unregister(d,first);
            check(!NativeBack.hasAppCallbacks(d)&&d.count()==1,"last native unregister synchronously restores observer without pre-draw");
            d.invokeBack();check(s.activity.nativeBackCalls==1,"immediate next Back uses the restored native Main route");s.removed();
        }});
        tests.put("x125_native_callback_registered_before_root_binding",()->{try(Scene s=new Scene(CHAT)){
            nativeEntry(s);s.root.detach();TransactionalDispatcher d=new TransactionalDispatcher();int[] calls={0};
            OnBackInvokedCallback original=()->calls[0]++;NativeBack.register(d,0,original);
            try{s.root.attach(d);s.root.focus(true);OneBackExit.onResume(s.activity);
                check(d.platform.count()==1,"binding and resume never add observer above existing native app callback");
                d.platform.invokeBack();check(calls[0]==1&&s.activity.nativeBackCalls==0,"existing enabled child receives Back");s.preserved();
            }finally{NativeBack.unregister(d,original);}
        }});
        tests.put("x125_native_registration_failure_preserves_observer_state",()->{try(Scene s=new Scene(HOME)){
            nativeEntry(s);s.root.detach();TransactionalDispatcher d=new TransactionalDispatcher();s.root.attach(d);s.root.focus(true);
            OnBackInvokedCallback observer=d.platform.top(),original=()->{};
            check(observer!=null&&d.platform.count()==1,"fixture has a real fallback observer");
            d.rejectRegister=true;boolean rejected=false;
            try{NativeBack.register(d,0,original);}catch(IllegalStateException expected){rejected=true;}
            check(rejected&&!NativeBack.hasAppCallbacks(d)&&d.platform.top()==observer,"rejected native registration neither records callback nor removes observer");
            d.rejectRegister=false;NativeBack.register(d,0,original);
            OnBackInvokedCallback wrapper=d.platform.top();
            check(wrapper!=observer&&d.platform.count()==1&&NativeBack.hasAppCallbacks(d),"successful native registration replaces only observer");
            d.rejectUnregister=true;rejected=false;
            try{NativeBack.unregister(d,original);}catch(IllegalStateException expected){rejected=true;}
            OneBackExit.onResume(s.activity);
            check(rejected&&NativeBack.hasAppCallbacks(d)&&d.platform.top()==wrapper&&d.platform.count()==1,"rejected native unregister keeps native bookkeeping and callback");
            d.rejectUnregister=false;NativeBack.unregister(d,original);
            check(!NativeBack.hasAppCallbacks(d)&&d.platform.count()==1&&d.platform.top()!=wrapper,"successful final unregister immediately restores observer");
            d.platform.invokeBack();s.removed();
        }});
        tests.put("x125_worker_registration_refresh_is_marshaled_to_main",()->{try(Scene s=new Scene(CHAT)){
            nativeEntry(s);android.window.FakeDispatcher d=s.activity.getWindow().dispatcher();OnBackInvokedCallback original=()->{};
            Thread worker=new Thread(()->NativeBack.register(d,0,original));worker.start();worker.join();Handler.drain();
            try{check(d.count()==1&&NativeBack.hasAppCallbacks(d),"off-main registration refresh runs on main and removes only observer");
                d.invokeBack();check(s.activity.nativeBackCalls==0,"native child still receives Back");s.preserved();
            }finally{NativeBack.unregister(d,original);}
        }});
        tests.put("x125_system_overlay_and_priority_reregistration",()->{try(Scene s=new Scene(HOME)){
            nativeEntry(s);int[] calls={0};OnBackInvokedCallback original=()->calls[0]++;
            NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            NativeBack.register(s.activity.getWindow().dispatcher(),OnBackInvokedDispatcher.PRIORITY_OVERLAY,original);
            try{s.activity.getWindow().dispatcher().invokeBack();
                check(calls[0]==1&&s.activity.nativeBackCalls==0,"overlay retains native priority and callback");s.preserved();
                NativeBack.register(s.activity.getWindow().dispatcher(),0,original);s.activity.getWindow().dispatcher().invokeBack();
                check(calls[0]==2&&s.activity.nativeBackCalls==0,"default-priority enabled handler also retains consumption");s.preserved();
            }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        tests.put("x125_system_dialog_window_remains_native",()->{try(Scene s=new Scene(HOME)){
            nativeEntry(s);s.root.focus(false);Dialog dialog=new Dialog(s.activity);dialog.setOwnerActivity(s.activity);dialog.show();
            OneBackExit.dialogShown(dialog);int[] calls={0};OnBackInvokedCallback original=()->{calls[0]++;dialog.onBackPressed();};
            NativeBack.register(dialog.getWindow().dispatcher(),0,original);
            try{dialog.getWindow().dispatcher().invokeBack();
                check(calls[0]==1&&dialog.nativeBackCalls==1,"dialog callback closes only its own window");
                check(s.activity.nativeBackCalls==0&&s.hookCalls==0&&!marker(s.app),"dialog dispatcher never becomes Main root authority");s.preserved();
            }finally{NativeBack.unregister(dialog.getWindow().dispatcher(),original);dialog.dismiss();}
        }});
        tests.put("x125_observer_yields_to_unwrapped_framework_ime",()->{try(Scene s=new Scene(SEARCH)){
            nativeEntry(s);retainedEditor(s);int[] calls={0};OnBackInvokedCallback ime=()->{calls[0]++;s.root.insets.ime=false;};
            s.activity.getWindow().dispatcher().registerOnBackInvokedCallback(0,ime);
            s.root.insets.ime=true;OneBackExit.onResume(s.activity);
            try{check(!HomeScreen.twitterInputReady(s.activity)&&s.activity.getWindow().dispatcher().top()==ime,"IME-visible resume cannot rearm observer above framework IME");
                s.activity.getWindow().dispatcher().invokeBack();
                check(calls[0]==1&&s.activity.nativeBackCalls==0&&!marker(s.app),"first Back closes IME without native root exit");s.preserved();
            }finally{s.activity.getWindow().dispatcher().unregisterOnBackInvokedCallback(ime);}
            SystemClock.time+=121;s.root.getViewTreeObserver().draw();
            check(s.activity.getWindow().dispatcher().top()!=null,"observer returns after IME closes despite retained editor focus");
            s.activity.getWindow().dispatcher().invokeBack();s.removed();
        }});
        tests.put("x125_observer_requires_focused_window_and_known_insets",()->{try(Scene s=new Scene(HOME)){
            nativeEntry(s);s.root.focus(false);Handler.drain();
            check(s.activity.getWindow().dispatcher().top()==null,"unfocused Main has no active observer");
            s.root.focus(true);check(s.activity.getWindow().dispatcher().top()!=null,"focused Main restores observer");
            android.view.WindowInsets saved=s.root.insets;s.root.insets=null;OneBackExit.onResume(s.activity);
            check(s.activity.getWindow().dispatcher().top()==null,"unknown IME insets fail closed");
            s.root.insets=saved;SystemClock.time+=121;s.root.getViewTreeObserver().draw();
            check(s.activity.getWindow().dispatcher().top()!=null,"known IME-hidden insets restore observer");s.preserved();
        }});
        tests.put("x125_system_nested_fallback_cannot_replay_native_entry",()->{try(Scene s=new Scene(HOME)){
            retainedEditor(s);int[] entries={0},nativeTerminals={0},calls={0};
            s.activity.nativeBackAction=()->{OneBackExit.enterTwitterBack(s.activity);try{
                entries[0]++;
                if(!OneBackExit.tryTerminalBack(s.activity,0,false))nativeTerminals[0]++;
            }finally{OneBackExit.leaveNative();}};
            OnBackInvokedCallback original=fallback(s,calls);NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            try{s.activity.getWindow().dispatcher().invokeBack();
                check(entries[0]==1&&calls[0]==1&&nativeTerminals[0]==1,"p-declined nested fallback reaches platform once without recursion");s.preserved();
            }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        tests.put("x125_legacy_native_entry_is_not_replayed_by_platform_fallback",()->{try(Scene s=new Scene(HOME)){
            retainedEditor(s);OneBackExit.enterTwitterBack(s.activity);try{
                check(!OneBackExit.tryTerminalBack(s.activity,0,false),"native p already visited so fallback remains native");
                check(s.activity.nativeBackCalls==0,"legacy entry is not replayed");
            }finally{OneBackExit.leaveNative();}s.preserved();
        }});
        tests.put("x125_nested_native_marker_retains_outer_entry",()->{try(Scene s=new Scene(HOME)){
            retainedEditor(s);OneBackExit.enterTwitterBack(s.activity);try{
                OneBackExit.enterTwitterBack(s.activity);OneBackExit.leaveNative();
                check(!OneBackExit.tryTerminalBack(s.activity,0,false),"inner p wrapper does not clear outer native-entry marker");
            }finally{OneBackExit.leaveNative();}
            nativeEntry(s);OneBackExit.enterBack(s.activity);try{
                check(OneBackExit.tryTerminalBack(s.activity,0,false),"later independent system fallback can enter native p again");
            }finally{OneBackExit.leaveNative();}s.removed();
        }});
        for(boolean resume:new boolean[]{false,true})for(int priority:new int[]{0,OnBackInvokedDispatcher.PRIORITY_OVERLAY}){
            final int p=priority;final boolean resumeAfterRegistration=resume;
            tests.put("x125_system_predictive_child_cancel_and_commit_"+p+"_resume_"+resumeAfterRegistration,()->{try(Scene s=new Scene(CHAT)){
                nativeEntry(s);int[] calls={0,0,0,0};OnBackAnimationCallback original=new OnBackAnimationCallback(){
                    public void onBackStarted(BackEvent e){calls[0]++;}public void onBackProgressed(BackEvent e){calls[1]++;}
                    public void onBackCancelled(){calls[2]++;}public void onBackInvoked(){calls[3]++;}
                };NativeBack.register(s.activity.getWindow().dispatcher(),p,original);top(s,resumeAfterRegistration);
                try{OnBackAnimationCallback wrapped=(OnBackAnimationCallback)s.activity.getWindow().dispatcher().top();
                    wrapped.onBackStarted(new BackEvent());wrapped.onBackProgressed(new BackEvent());wrapped.onBackCancelled();
                    check(calls[0]==1&&calls[1]==1&&calls[2]==1&&s.activity.nativeBackCalls==0&&!marker(s.app),"cancel preserves native animation without invoking Main or creating marker");s.preserved();
                    wrapped.onBackStarted(new BackEvent());wrapped.onBackInvoked();
                    check(calls[0]==2&&calls[3]==1&&s.activity.nativeBackCalls==0,"native predictive child retains committed consumption");s.preserved();
                }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
            }});
        }
        tests.put("x125_system_predictive_fallback_commit",()->{try(Scene s=new Scene(NOTIFICATIONS)){
            nativeEntry(s);retainedEditor(s);int[] calls={0};OnBackInvokedCallback terminal=fallback(s,calls);
            OnBackAnimationCallback original=new OnBackAnimationCallback(){
                public void onBackStarted(BackEvent e){}public void onBackProgressed(BackEvent e){}public void onBackCancelled(){}
                public void onBackInvoked(){terminal.onBackInvoked();}
            };NativeBack.register(s.activity.getWindow().dispatcher(),0,original);top(s,true);
            try{OnBackAnimationCallback wrapped=(OnBackAnimationCallback)s.activity.getWindow().dispatcher().top();
                wrapped.onBackStarted(new BackEvent());wrapped.onBackCancelled();check(calls[0]==0&&!marker(s.app),"cancel never reaches fallback");s.preserved();
                wrapped.onBackStarted(new BackEvent());wrapped.onBackInvoked();
                check(calls[0]==1&&s.activity.nativeBackCalls==1,"committed predictive fallback visits native p once");s.removed();
            }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        tests.put("x125_system_fallback_requires_real_back_owner",()->{try(Scene s=new Scene(HOME)){
            nativeEntry(s);check(!OneBackExit.tryTerminalBack(s.activity,0,false),"internal platform call has no committed Back attempt");
            OneBackExit.enterBack(s.activity);try{
                Activity other=new com.twitter.app.main.MainActivity(s.app,12,true);
                check(!OneBackExit.tryTerminalBack(other,0,false),"different Activity is not the attempt owner");
                boolean[] accepted={true};Thread thread=new Thread(()->accepted[0]=OneBackExit.tryTerminalBack(s.activity,0,false));thread.start();thread.join();
                check(!accepted[0],"worker thread cannot reroute a platform fallback");
            }finally{OneBackExit.leaveNative();}
            check(s.activity.nativeBackCalls==0,"rejected ownership never invokes native Main");s.preserved();
        }});
        tests.put("x125_system_unrelated_app_callback_preserved",()->{
            Activity other=new Activity(new Application("com.example.other"),71,true);other.getWindow().attach();other.getWindow().getDecorView().focus(true);
            int[] calls={0};OnBackInvokedCallback original=()->calls[0]++;
            NativeBack.register(other.getWindow().dispatcher(),0,original);
            try{other.getWindow().dispatcher().invokeBack();check(calls[0]==1&&other.nativeBackCalls==0,"other package retains original callback");}
            finally{NativeBack.unregister(other.getWindow().dispatcher(),original);other.getWindow().getDecorView().detach();}
        });
        tests.put("x125_system_native_exception_unwinds_scope",()->{try(Scene s=new Scene(HOME)){
            RuntimeException expected=new IllegalStateException("native test failure");
            s.activity.nativeBackAction=()->{OneBackExit.enterTwitterBack(s.activity);try{throw expected;}finally{OneBackExit.leaveNative();}};
            int[] calls={0};OnBackInvokedCallback original=fallback(s,calls);NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            try{RuntimeException actual=null;try{s.activity.getWindow().dispatcher().invokeBack();}catch(RuntimeException caught){actual=caught;}
                check(actual==expected&&calls[0]==1,"native exception is preserved without replay");s.preserved();
                nativeEntry(s);s.activity.getWindow().dispatcher().invokeBack();
                check(s.hookCalls==1,"exception left neither a stale native-entry marker nor scope");s.removed();
            }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception{
        int oldCases=cases,oldAssertions=assertions;
        for(Map.Entry<String,Test> entry:scenarios().entrySet())test(entry.getKey(),entry.getValue());
        System.out.println("PASS Back125TwitterSystemTest: "+(cases-oldCases)+" cases, "+(assertions-oldAssertions)+" assertions");
    }
}
