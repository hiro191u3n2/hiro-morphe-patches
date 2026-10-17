package app.hiro.oneback.runtime;

import android.app.*;
import android.graphics.Rect;
import android.view.*;
import android.window.*;
import java.lang.reflect.*;
import java.util.*;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** MainActivity's native AndroidX branch may return before the Reels callback.
 * These scenarios exercise the same dispatch boundary against runtime doubles;
 * the real APK's branch insertion and preservation are audited separately. */
public final class Back120MainDispatchTest {
    static boolean boundary(Activity a) throws Exception {
        try {
            return Boolean.TRUE.equals(OneBackExit.class.getMethod("tryInstagramMainReelsBack",Activity.class).invoke(null,a));
        } catch(NoSuchMethodException previousVersion) { return false; }
          catch(InvocationTargetException failed) {
            Throwable cause=failed.getCause();
            if(cause instanceof Exception)throw (Exception)cause;
            if(cause instanceof Error)throw (Error)cause;
            throw failed;
        }
    }
    static boolean nativeMain(F f) throws Exception {
        OneBackExit.enterBack(f.activity);
        try{return boundary(f.activity);}finally{OneBackExit.leaveNative();}
    }
    static void rejectSameGestureExit(F f,NativeReels120Fixture.Page page) {
        try {
            check(!boundary(f.activity),"native layer closure cannot enter main task exit in the same Back");
            check(!OneBackExit.tryInstagramReelsBack(page),"native layer closure cannot enter fragment task exit in the same Back");
        }catch(Exception failed){throw new AssertionError("Nested native Back check failed",failed);}
    }
    static Map<String,Test> scenarios() {
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        tests.put("main_androidx_branch_removes_reels_task_before_native_return",()->{try(F f=new F(IG,true,false)){
            NativeReels120Fixture.reels(f);ActivityManager.AppTask task=ownTask(f,11);
            check(nativeMain(f),"main native boundary handles Reels without reaching boolean fragment callback");
            check(task.excluded&&task.removeCalls==1&&f.activity.nativeBackCalls==0,"task excluded and removed once without native tab pop");
            check(!nativeMain(f)&&task.removeCalls==1,"reentrant callback cannot remove the task twice");
        }});
        tests.put("main_unknown_whole_scene_uses_actual_reels_fragment",()->{try(F f=new F(IG,true,false)){
            NativeReels120Fixture.reels(f);Back119InstagramTest.crowded(f);ActivityManager.AppTask task=ownTask(f,11);
            check(!OneBackExit.tryBack(f.activity),"ordinary scene recognition remains unknown for the oversized retained tree");
            check(nativeMain(f)&&task.excluded&&task.removeCalls==1,"main native boundary can exit known visible Reels despite unrelated scan limit");
        }});
        tests.put("main_callback_without_android_back_transaction_cannot_exit",()->{try(F f=new F(IG,true,false)){
            NativeReels120Fixture.reels(f);check(!boundary(f.activity),"internal app calls cannot authorize task exit");preserved(f);
        }});
        tests.put("main_comment_panel_closes_before_reels_exit",()->{try(F f=new F(IG,true,false)){
            NativeReels120Fixture.Page page=NativeReels120Fixture.reels(f);
            View comments=Back118InstagramTest.group(f,"comments_container",new Rect(0,700,1080,2100),(ViewGroup)page.view);
            InstagramBack.predictionStart(f.activity);
            ((ViewGroup)page.view).removeView(comments);
            check(!nativeMain(f),"comment closure cannot turn the same committed Back into task exit");preserved(f);
            InstagramBack.predictionEnd(f.activity);OneBackExit.onUserInteraction(f.activity);
            check(nativeMain(f)&&f.activity.removeCalls==1,"a later distinct Back on Reels can exit");
        }});
        tests.put("main_foreground_profile_keeps_native_navigation",()->{try(F f=new F(IG,true,false)){
            NativeReels120Fixture.reels(f);View profile=marker(f,"profile_root",new Rect(0,0,1080,2100));
            f.activity.fragments.fragments.add(new Back119InstagramTest.Page(f.activity,profile,"profile_view"));
            check(!nativeMain(f),"profile on top of Reels is still a profile Back");preserved(f);
        }});
        tests.put("main_ime_stays_native",()->{try(F f=new F(IG,true,false)){
            NativeReels120Fixture.reels(f);f.root.insets.ime=true;
            check(!nativeMain(f),"keyboard Back is not task exit");preserved(f);
        }});
        tests.put("main_other_focused_window_stays_native",()->{try(F f=new F(IG,true,false)){
            NativeReels120Fixture.reels(f);f.dialog();
            check(!nativeMain(f),"separate focused dialog retains its own Back");preserved(f);
        }});
        tests.put("main_detail_gesture_arriving_at_reels_cannot_exit",()->{try(F f=new F(IG,true,false)){
            View detail=marker(f,"profile_root",new Rect(0,0,1080,2100));
            Back119InstagramTest.Page prior=new Back119InstagramTest.Page(f.activity,detail,"profile_view");
            f.activity.fragments.fragments.add(prior);InstagramBack.predictionStart(f.activity);
            f.activity.fragments.fragments.remove(prior);f.root.removeView(detail);NativeReels120Fixture.reels(f);
            check(!nativeMain(f),"a Back begun on another page cannot exit newly exposed Reels");preserved(f);
        }});
        tests.put("main_without_fragment_proof_does_not_exit",()->{try(F f=new F(IG,true,false)){
            marker(f,"clips_viewer_container",new Rect(0,0,1080,2100));
            check(!nativeMain(f),"main host and Reels-looking view alone are insufficient for native fallback");preserved(f);
        }});
        tests.put("main_internal_reels_popup_stays_native",()->{try(F f=new F(IG,true,false)){
            NativeReels120Fixture.Page page=NativeReels120Fixture.reels(f);NativeReels120Fixture.popup(page);
            check(InstagramBack.inspect(f.activity).blocked,"exact native popup flag blocks initial classification despite anonymous view tree");
            check(!nativeMain(f),"main hook does not skip the Reels-native popup guard");preserved(f);
        }});
        tests.put("main_internal_reels_sheet_stays_native",()->{try(F f=new F(IG,true,false)){
            NativeReels120Fixture.Page page=NativeReels120Fixture.reels(f);NativeReels120Fixture.sheet(page);
            check(!nativeMain(f),"main hook leaves native Reels sheet close logic in control");preserved(f);
        }});
        tests.put("main_unverified_native_layer_signature_cannot_exit",()->{try(F f=new F(IG,true,false)){
            NativeReels120Fixture.reels(f,true);
            check(!nativeMain(f),"mismatched native guard descriptors fail closed");preserved(f);
            OneBackExit.onUserInteraction(f.activity);
            check(OneBackExit.tryBack(f.activity)&&f.activity.nativeBackCalls==1,"changed native field shape forwards initial Back once");preserved(f);
        }});
        for(final boolean popup:new boolean[]{true,false}) {
            final String kind=popup?"popup":"sheet";
            tests.put("main_initial_back_closes_native_"+kind+"_only",()->{try(F f=new F(IG,true,false)){
                NativeReels120Fixture.Page page=NativeReels120Fixture.reels(f);
                if(popup)NativeReels120Fixture.popup(page);else NativeReels120Fixture.sheet(page);
                ActivityManager.AppTask task=ownTask(f,11);
                check(!ExitScreens.shouldExit(f.activity),"native layer flag vetoes the initial direct exit path");
                f.activity.nativeBackAction=()->{NativeReels120Fixture.closeLayers(page);rejectSameGestureExit(f,page);};
                check(OneBackExit.tryBack(f.activity),"initial Back is handled by forwarding native layer closure");
                check(f.activity.nativeBackCalls==1&&!InstagramBack.inspect(f.activity).blocked,"native closure runs once and reveals clear Reels");
                check(!task.excluded&&task.removeCalls==0,"layer Back preserves the recent task");preserved(f);
                check(!nativeMain(f),"same Back remains guarded after the native callback returned");
                check(OneBackExit.key(f.activity,down())&&OneBackExit.key(f.activity,up()),"a later distinct Back can exit Reels");
                check(task.excluded&&task.removeCalls==1&&f.activity.nativeBackCalls==1,"later Back removes the task without repeating layer closure");
            }});
            tests.put("main_predictive_native_"+kind+"_closure_preserves_same_gesture",()->{try(F f=new F(IG,true,false)){
                NativeReels120Fixture.Page page=NativeReels120Fixture.reels(f);
                if(popup)NativeReels120Fixture.popup(page);else NativeReels120Fixture.sheet(page);
                ActivityManager.AppTask task=ownTask(f,11);final int[] calls={0,0,0};
                OnBackAnimationCallback original=new OnBackAnimationCallback(){
                    public void onBackStarted(BackEvent e){calls[0]++;if(popup)NativeReels120Fixture.closeLayers(page);}
                    public void onBackProgressed(BackEvent e){calls[1]++;}
                    public void onBackCancelled(){}
                    public void onBackInvoked(){calls[2]++;NativeReels120Fixture.closeLayers(page);rejectSameGestureExit(f,page);}
                };
                NativeBack.register(f.dispatcher(),0,original);
                try {
                    OnBackAnimationCallback wrapped=(OnBackAnimationCallback)f.dispatcher().top();
                    wrapped.onBackStarted(new BackEvent());wrapped.onBackProgressed(new BackEvent());
                    check(!InstagramBack.exitPrediction(f.activity),"native layer-started gesture is never a Reels exit prediction");
                    wrapped.onBackInvoked();
                    check(Arrays.equals(calls,new int[]{1,1,popup?0:1}),"native animation runs and already-closed layer is not closed again");
                    check(!InstagramBack.inspect(f.activity).blocked&&!task.excluded&&task.removeCalls==0,"first gesture only exposes Reels and preserves Recents");preserved(f);
                    check(!nativeMain(f),"first gesture cannot cascade into main task exit after closure");
                    wrapped.onBackStarted(new BackEvent());wrapped.onBackInvoked();
                    check(task.excluded&&task.removeCalls==1,"next distinct gesture exits clear Reels once");
                }finally{NativeBack.unregister(f.dispatcher(),original);}
            }});
        }
        return tests;
    }
    public static void main(String[] args) throws Exception {
        int casesBefore=cases,assertionsBefore=assertions;
        for(Map.Entry<String,Test> item:scenarios().entrySet())test(item.getKey(),item.getValue());
        System.out.println("PASS Back120MainDispatchTest: "+(cases-casesBefore)+" cases, "+(assertions-assertionsBefore)+" assertions");
    }
}
