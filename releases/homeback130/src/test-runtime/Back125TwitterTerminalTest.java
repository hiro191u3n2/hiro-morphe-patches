package app.hiro.oneback.runtime;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.Back124TwitterTest.*;
import static app.hiro.oneback.runtime.Back125TwitterKeyTest.*;

/** An original native child explicitly requests a terminal Activity operation
 * before the l1 root URI gate. No PC87 call or selected-URI claim grants exit. */
public final class Back125TwitterTerminalTest {
    static void retainedEditor(KeyScene s){
        View editor=new View(s.activity);editor.editor=true;editor.hidden=true;s.root.focusView=editor;
    }
    static void rejected(Activity a,String reason){
        for(int kind=1;kind<=4;kind++)check(!OneBackExit.tryTerminalBack(a,kind,false),reason+" kind="+kind);
    }
    public static void main(String[] args)throws Exception {
        int oldCases=cases,oldAssertions=assertions;
        for(final int kind:new int[]{1,2,3,4})for(final String tab:TABS)test("x125_native_explicit_terminal_"+kind+"_"+tab,()->{
            Disk persisted;
            try(KeyScene s=new KeyScene(tab)){
                persisted=s.disk;retainedEditor(s);check(!HomeScreen.inputReady(s.activity),"retained editor vetoes pre-dispatch UI eligibility");
                int[] terminals={0},fallbacks={0};
                s.activity.nativeBackAction=()->{
                    terminals[0]++;
                    // Mirrors the existing bridge at the app's original
                    // finish/moveTaskToBack/finishAndRemoveTask/finishAffinity.
                    if(!OneBackExit.tryTerminalBack(s.activity,kind,false)){
                        fallbacks[0]++;if(kind!=2)s.activity.finish();
                    }
                };
                s.keyBack();
                check(terminals[0]==1&&fallbacks[0]==0,"original native exit becomes one task-removing exit");
                check(s.hookCalls==0,"explicit native child exit never reaches the URI root gate");s.removed();
                check(marker(s.app),"explicit native exit persists next-launch Home marker");
            }
            try(Scene next=new Scene(new ProcessApp(persisted),tab,launcher(),true)){
                check(HOME.equals(next.current),"cold normal launcher restores Home after explicit native terminal");
                Handler.drain();check(!marker(next.app),"observed Home consumes marker");next.preserved();
            }
        });
        test("x125_explicit_terminal_inactive_scope_rejected",()->{try(KeyScene s=new KeyScene(CHAT)){
            rejected(s.activity,"ordinary app terminal without Android Back keeps native behavior");s.preserved();
            check(!marker(s.app),"ordinary terminal probe never writes a Home marker");
        }});
        test("x125_explicit_terminal_ended_ineligible_scope_rejected",()->{try(KeyScene s=new KeyScene(CHAT)){
            retainedEditor(s);OneBackExit.enterTwitterBack(s.activity);OneBackExit.afterNative();OneBackExit.leaveNative();
            rejected(s.activity,"ended ineligible Back cannot authorize a delayed terminal");s.preserved();
        }});
        test("x125_explicit_terminal_stale_scope_rejected",()->{try(KeyScene s=new KeyScene(CHAT)){
            retainedEditor(s);OneBackExit.enterTwitterBack(s.activity);try{
                OneBackExit.onUserLeaveHint(s.activity);rejected(s.activity,"departure invalidates live Back authority");
            }finally{OneBackExit.leaveNative();}s.preserved();
        }});
        test("x125_explicit_terminal_other_owner_rejected",()->{try(KeyScene s=new KeyScene(CHAT)){
            retainedEditor(s);Activity other=new com.twitter.app.main.MainActivity(s.app,12,true);
            OneBackExit.enterTwitterBack(s.activity);try{rejected(other,"different Activity cannot borrow native Back authority");}
            finally{OneBackExit.leaveNative();}s.preserved();
        }});
        test("x125_explicit_terminal_nonroot_main_rejected",()->{
            ProcessApp app=new ProcessApp(new Disk());Activity child=new com.twitter.app.main.MainActivity(app,11,false);
            OneBackExit.enterTwitterBack(child);try{rejected(child,"native Main inside another Activity task cannot remove root");}
            finally{OneBackExit.leaveNative();}
            check(!child.finishing&&!marker(app),"nonroot original terminal is not replaced");
        });
        test("x125_explicit_terminal_legacy_host_heuristic_retained",()->{try(F s=new F(X,true,false)){
            View editor=new View(s.activity);editor.editor=true;s.root.focusView=editor;
            check(!HomeScreen.inputReady(s.activity),"legacy host input is ineligible");
            OneBackExit.enterBack(s.activity);try{rejected(s.activity,"old HomeTimeline host retains prior eligibility checks");}
            finally{OneBackExit.leaveNative();}preserved(s);
        }});
        test("x125_explicit_terminal_unknown_host_rejected",()->{try(F s=new F(X,false,false)){
            View editor=new View(s.activity);editor.editor=true;s.root.focusView=editor;
            OneBackExit.enterBack(s.activity);try{rejected(s.activity,"unknown X host does not acquire the native Main exception");}
            finally{OneBackExit.leaveNative();}preserved(s);
        }});
        test("x125_explicit_terminal_dead_or_changing_owner_rejected",()->{try(KeyScene s=new KeyScene(CHAT)){
            retainedEditor(s);OneBackExit.enterTwitterBack(s.activity);try{
                s.activity.destroyed=true;rejected(s.activity,"destroyed native owner rejected");s.activity.destroyed=false;
                s.activity.finishing=true;rejected(s.activity,"already-finishing owner rejected");s.activity.finishing=false;
                s.activity.changingConfigurations=true;rejected(s.activity,"configuration transition rejected");s.activity.changingConfigurations=false;
            }finally{OneBackExit.leaveNative();}s.preserved();
        }});
        System.out.println("PASS Back125TwitterTerminalTest: "+(cases-oldCases)+" cases, "+(assertions-oldAssertions)+" assertions");
    }
}
