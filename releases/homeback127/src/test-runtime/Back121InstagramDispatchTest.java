package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import app.hiro.oneback.runtime.InstagramCurrent121Fixture.Scene;
import app.hiro.oneback.runtime.InstagramCurrent121Fixture.Node;
import app.hiro.oneback.runtime.InstagramCurrent121Fixture.ManagerState;
import static app.hiro.oneback.runtime.InstagramCurrent121Fixture.MAIN;
import static app.hiro.oneback.runtime.InstagramCurrent121Fixture.BASE;
import static app.hiro.oneback.runtime.InstagramCurrent121Fixture.added;
import static app.hiro.oneback.runtime.InstagramCurrent121Fixture.set;

/** Exercises production registration, OS wrappers, key pairing and native
 * boundaries against exact-name native-container models. The AndroidX callback
 * deliberately pops without either patched Instagram method. Its audited APK
 * route is evidence for the model, not execution of that APK on Android. */
public final class Back121InstagramDispatchTest {
    static ActivityManager.AppTask task(Scene s,int id,String pkg){
        ActivityManager.RecentTaskInfo info=new ActivityManager.RecentTaskInfo();info.taskId=id;
        info.baseActivity=new ComponentName(pkg,"Main");info.baseIntent=new Intent().setComponent(info.baseActivity);
        ActivityManager.AppTask task=new ActivityManager.AppTask(info);
        if(id==11&&pkg.equals(IG))task.activities.add(s.activity);
        s.tasks.tasks.add(task);return task;
    }
    static ActivityManager.AppTask own(Scene s){return task(s,11,IG);}
    static void preserved(Scene s,ActivityManager.AppTask task){
        check(!s.activity.finishing&&s.activity.finishCalls==0&&s.activity.removeCalls==0,"the Activity remains open");
        check(!task.excluded&&task.removeCalls==0,"Recents is retained while navigating or closing a layer");
    }
    static void removed(Scene s,ActivityManager.AppTask task){
        check(task.excluded&&task.removeCalls==1,"Back excludes and removes the owned task exactly once");
        check(task.events.equals(Arrays.asList("exclude","remove")),"Recents exclusion precedes task removal");
    }
    static void huge(Scene s,Node parent,int size){
        ViewGroup branch=s.group((ViewGroup)parent.view,"retained_content",new Rect(0,0,1080,2200));
        for(int i=0;i<size;i++)branch.addView(new View(s.activity));
    }
    static void footer(Scene s,Node page){
        ViewGroup linear=s.group((ViewGroup)page.view,"clips_linear_layout_container",new Rect(0,0,1080,2170));
        ViewGroup gestures=s.group(linear,"gesture_manager",new Rect(0,0,1080,1990));
        ViewGroup viewer=s.group(gestures,"clips_viewer_container",new Rect(0,0,1080,1990));
        s.group(viewer,"clips_viewer_view_pager",new Rect(0,0,1080,1990));
        ViewGroup footer=s.group(linear,"clips_footer_mask_container",new Rect(0,1990,1080,2110));
        ViewGroup inner=s.group(footer,"comment_composer_inner_layout",new Rect(24,2000,1056,2100));
        TextView prompt=new TextView(s.activity);prompt.id=++markerId;
        android.content.res.Resources.IDS.put(prompt.id,"comment_composer_text_view");
        prompt.bounds=new Rect(40,2005,950,2095);prompt.text="コメントを追加…";inner.addView(prompt);
    }
    static Node current(Scene s,boolean home)throws Exception{
        Node page=s.node(home?"X.3mI":"X.8Zv",null,MAIN);
        s.select(page,home);if(!home)footer(s,page);return page;
    }
    static Node older(Scene s,int size)throws Exception{
        Node background=s.node("X.NativeProfile",null,MAIN);background.resumed=false;huge(s,background,size);return background;
    }
    static void draw(Scene s){SystemClock.time+=130;s.root.getViewTreeObserver().draw();Handler.drain();}
    static void erase(Scene s,Node node)throws Exception{
        Object manager=node.getClass().getField("mFragmentManager").get(node);added(manager).remove(node);
        if(node.view.getParent() instanceof ViewGroup)((ViewGroup)node.view.getParent()).removeView(node.view);
        node.added=false;node.visible=false;node.resumed=false;
    }
    static void noCascade(Scene s,Node reels){
        check(!OneBackExit.tryInstagramMainReelsBack(s.activity),"same native Back cannot exit newly exposed Main Reels");
        // Consuming a detail/layer returns to the native caller; invoking the
        // later X.8Zv hook manually here would invent a nonexistent fallthrough.
    }
    static Map<String,Test> scenarios(){
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final int count:new int[]{601,1801,4001})for(final boolean home:new boolean[]{false,true}){
            tests.put("direct_androidx_"+(home?"home":"reels")+"_retained_"+count,()->{try(Scene s=new Scene()){
                older(s,count);final Node initial=current(s,home);ActivityManager.AppTask task=own(s);
                // This optional visible tab proves the old budget can reject even
                // a selected Home, rather than only missing accessibility state.
                if(home){View tab=s.group(s.root,"feed_tab",new Rect(0,1990,190,2170));tab.selected=true;tab.clickable=true;}
                s.install();final int[] nativeCalls={0};
                OnBackInvokedCallback original=()->{
                    nativeCalls[0]++;
                    try{erase(s,initial);current(s,true);}catch(Exception failed){throw new AssertionError(failed);}
                };
                NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
                try{
                    s.activity.getWindow().dispatcher().invokeBack();
                    check(nativeCalls[0]==0,"the initial OS Back exits before AndroidX can pop to Home");removed(s,task);
                }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
            }});
        }
        tests.put("native_home_without_selected_accessibility_view",()->{try(Scene s=new Scene()){
            current(s,true);ActivityManager.AppTask task=own(s);s.install();
            check(!HomeScreen.isHome(s.activity),"fixture deliberately supplies no selected visual Home button");
            check(OneBackExit.tryBack(s.activity),"verified FEED plus actual Home content authorizes exit");removed(s,task);
        }});
        tests.put("owned_root_callback_stays_registered_for_native_reels",()->{try(Scene s=new Scene()){
            older(s,4001);current(s,false);ActivityManager.AppTask task=own(s);s.install();draw(s);
            check(s.activity.getWindow().dispatcher().count()==1,"large inactive content no longer withdraws the owned OS callback");
            s.activity.getWindow().dispatcher().invokeBack();removed(s,task);
        }});
        tests.put("inactive_tab_tree_does_not_poison_current_reels",()->{try(Scene s=new Scene()){
            Node old=s.node("X.0jG",null,MAIN);s.cacheTab(old,"feed");old.resumed=false;huge(s,old,4001);
            Node selected=s.node("X.0jG",null,MAIN);s.cacheTab(selected,"clips");
            Node reels=s.node("X.8Zv",selected,BASE);footer(s,reels);s.select(selected,false);
            ActivityManager.AppTask task=own(s);s.install();
            check(OneBackExit.key(s.activity,down())&&OneBackExit.key(s.activity,up()),"paired Back exits exact current tab through large retained tabs");removed(s,task);
        }});
        for(final int index:new int[]{0,1}){
            tests.put("clips_pager_os_back_exits_current_reels_"+index,()->{try(Scene s=new Scene()){
                Node clips=s.node("X.9sY",null,MAIN);ViewGroup pager=s.pager(clips,index);s.select(clips,false);
                Node previous=s.node("X.NativeProfile",clips,0x1936);s.tag(previous,"f"+(1-index));previous.resumed=false;huge(s,previous,4001);
                Node reels=s.node("X.Ze9",clips,0x1936);s.tag(reels,"f"+index);footer(s,reels);
                ((ViewGroup)clips.view).removeView(reels.view);pager.addView(reels.view);
                ActivityManager.AppTask task=own(s);s.install();final int[] calls={0};OnBackInvokedCallback original=()->calls[0]++;
                NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
                try{
                    s.activity.getWindow().dispatcher().invokeBack();
                    check(calls[0]==0,"a known current Reels subpage exits before a native page reset");removed(s,task);
                }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
            }});
        }
        tests.put("clips_pager_other_page_keeps_native_reset_then_next_reels_back_exits",()->{try(Scene s=new Scene()){
            Node clips=s.node("X.9sY",null,MAIN);final ViewGroup pager=s.pager(clips,1);s.select(clips,false);
            final Node reels=s.node("X.8Zv",clips,0x1936);s.tag(reels,"f0");reels.resumed=false;footer(s,reels);
            final Node other=s.node("X.NativeProfile",clips,0x1936);s.tag(other,"f1");ActivityManager.AppTask task=own(s);s.install();
            OnBackInvokedCallback original=()->{try{set(pager,"A00",0);other.resumed=false;reels.resumed=true;noCascade(s,reels);}catch(Exception failed){throw new AssertionError(failed);}};
            NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            try{
                s.activity.getWindow().dispatcher().invokeBack();preserved(s,task);
                check(OneBackExit.key(s.activity,down())&&OneBackExit.key(s.activity,up()),"a later distinct Back may exit the revealed Reels page");removed(s,task);
            }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        tests.put("main_overlay_reels_wins_over_inactive_home_base",()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.0jG",null,MAIN);s.select(wrapper,true);
            Node base=s.node("X.3mI",wrapper,BASE);base.resumed=false;huge(s,base,4001);
            Node reels=s.node("X.8Zv",wrapper,MAIN);footer(s,reels);
            ((ManagerState)s.child(wrapper)).count=1;ActivityManager.AppTask task=own(s);s.install();
            check(OneBackExit.tryBack(s.activity),"Reels overlay can exit despite native stack and retained Home base");removed(s,task);
        }});
        tests.put("profile_overlay_wins_over_home_base_and_stale_reels",()->{try(Scene s=new Scene()){
            Node wrapper=s.node("X.0jG",null,MAIN);s.select(wrapper,true);
            Node home=s.node("X.3mI",wrapper,BASE);home.resumed=false;
            Node stale=s.node("X.8Zv",wrapper,MAIN);stale.resumed=false;footer(s,stale);
            s.node("X.NativeProfile",wrapper,MAIN);ActivityManager.AppTask task=own(s);s.install();
            check(!OneBackExit.tryBack(s.activity),"native current profile cannot be overridden by retained Reels or Home");preserved(s,task);
        }});
        tests.put("same_native_detail_back_cannot_exit_exposed_reels",()->{try(Scene s=new Scene()){
            final Node reels=current(s,false);final Node detail=s.node("X.NativeProfile",reels,9911);ActivityManager.AppTask task=own(s);s.install();
            OnBackInvokedCallback original=()->{try{erase(s,detail);noCascade(s,reels);}catch(Exception failed){throw new AssertionError(failed);}};
            NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            try{
                s.activity.getWindow().dispatcher().invokeBack();preserved(s,task);
                check(OneBackExit.key(s.activity,down())&&OneBackExit.key(s.activity,up()),"a distinct subsequent Back on clear Reels exits");removed(s,task);
            }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        for(final String layer:new String[]{"main_modal","main_layer","native_reels_popup","rxr_dialog"}){
            tests.put("native_"+layer+"_closes_once_then_next_back_exits",()->{try(Scene s=new Scene()){
                final Node reels=current(s,false);final Runnable close;
                if(layer.equals("main_modal")){
                    final Node modal=(Node)s.create("X.9ld");Object host=s.create("X.3MM");set(host,"A02",new WeakReference<Object>(modal));set(s.activity,"A08",host);close=()->modal.added=false;
                }else if(layer.equals("main_layer")){
                    final Object host=s.create("X.16z");set(host,"A0U",true);
                    ((NativeActivity121Fixture.LayerState)host).current=s.create("androidx.fragment.app.Fragment");
                    s.loader.loadClass("com.instagram.base.activity.IgFragmentActivity").getDeclaredField("A00").set(s.activity,host);
                    close=()->((NativeActivity121Fixture.LayerState)host).current=null;
                }else if(layer.equals("native_reels_popup")){
                    final Object popup=s.create("X.21x");set(popup,"A05",true);set(reels,"A1Y",popup);
                    close=()->{try{set(popup,"A05",false);}catch(Exception failed){throw new AssertionError(failed);}};
                }else{
                    Object dialogFragment=s.create("X.RxR");final NativeActivity121Fixture.ShowingDialog dialog=new NativeActivity121Fixture.ShowingDialog(s.activity);dialog.showing=true;
                    s.loader.loadClass("X.07d").getDeclaredField("A00").set(dialogFragment,dialog);added(s.manager).add(dialogFragment);close=()->dialog.showing=false;
                }
                ActivityManager.AppTask task=own(s);s.install();final int[] nativeCalls={0};
                OnBackInvokedCallback original=()->{nativeCalls[0]++;close.run();noCascade(s,reels);};
                NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
                try{
                    s.activity.getWindow().dispatcher().invokeBack();check(nativeCalls[0]==1,"one native layer closure runs");preserved(s,task);
                    check(OneBackExit.key(s.activity,down())&&OneBackExit.key(s.activity,up()),"next distinct Back exits the revealed Reels");removed(s,task);
                    check(nativeCalls[0]==1,"task exit does not repeat native layer closure");
                }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
            }});
        }
        for(final String resource:new String[]{"comment_edit_text","comment_sheet","comments_sheet","share_sheet","bottom_sheet","design_bottom_sheet"}){
            tests.put("small_identified_layer_"+resource,()->{try(Scene s=new Scene()){
                Node reels=current(s,false);ViewGroup layer=s.group((ViewGroup)reels.view,resource,new Rect(10,500,210,600));
                ActivityManager.AppTask task=own(s);s.install();
                s.activity.nativeBackAction=()->((ViewGroup)reels.view).removeView(layer);
                check(OneBackExit.tryBack(s.activity),"an identified small layer forwards its native Back");
                check(s.activity.nativeBackCalls==1,"native layer closure runs once despite narrow geometry");preserved(s,task);
            }});
        }
        for(final String state:new String[]{"stack","pending","saved","executing","ime"}){
            tests.put("native_home_"+state+"_retains_back",()->{try(Scene s=new Scene()){
                current(s,true);
                if(state.equals("stack"))((ManagerState)s.manager).count=1;
                else if(state.equals("pending"))((java.util.List<Object>)s.manager.getClass().getField("A0W").get(s.manager)).add(new Object());
                else if(state.equals("saved"))((ManagerState)s.manager).saved=true;
                else if(state.equals("executing"))set(s.manager,"mExecutingActions",true);
                else s.root.insets.ime=true;
                ActivityManager.AppTask task=own(s);s.install();
                check(!OneBackExit.tryBack(s.activity),"Back on an unready/stacked Home remains native: "+state);preserved(s,task);
            }});
        }
        tests.put("gesture_from_comments_to_reels_does_not_exit_until_next_gesture",()->{try(Scene s=new Scene()){
            Node reels=current(s,false);final ViewGroup comments=s.group((ViewGroup)reels.view,"comments_container",new Rect(0,650,1080,2200));
            ActivityManager.AppTask task=own(s);s.install();final int[] calls={0,0};
            OnBackAnimationCallback original=new OnBackAnimationCallback(){
                public void onBackStarted(BackEvent event){calls[0]++;((ViewGroup)reels.view).removeView(comments);}
                public void onBackProgressed(BackEvent event){}public void onBackCancelled(){}
                public void onBackInvoked(){calls[1]++;noCascade(s,reels);}
            };
            NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
            try{
                OnBackAnimationCallback wrapped=(OnBackAnimationCallback)s.activity.getWindow().dispatcher().top();
                wrapped.onBackStarted(new BackEvent());wrapped.onBackInvoked();
                check(calls[0]==1&&calls[1]==0,"already collapsed comments are not closed a second time at commit");preserved(s,task);
                wrapped.onBackStarted(new BackEvent());wrapped.onBackInvoked();removed(s,task);
                check(calls[0]==1,"clear Reels exit suppresses the native pop animation");
            }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
        }});
        tests.put("cancelled_native_reels_exit_gesture_preserves_recents",()->{try(Scene s=new Scene()){
            current(s,false);ActivityManager.AppTask task=own(s);s.install();
            OnBackAnimationCallback callback=(OnBackAnimationCallback)s.activity.getWindow().dispatcher().top();
            callback.onBackStarted(new BackEvent());callback.onBackCancelled();preserved(s,task);
            callback.onBackStarted(new BackEvent());callback.onBackInvoked();removed(s,task);
        }});
        tests.put("exit_gesture_cannot_transfer_to_a_different_home_object",()->{try(Scene s=new Scene()){
            Node first=current(s,true);ActivityManager.AppTask task=own(s);s.install();
            OnBackAnimationCallback callback=(OnBackAnimationCallback)s.activity.getWindow().dispatcher().top();
            callback.onBackStarted(new BackEvent());erase(s,first);current(s,true);callback.onBackInvoked();preserved(s,task);
            Handler.drain();check(OneBackExit.key(s.activity,down())&&OneBackExit.key(s.activity,up()),"a fresh Back may exit the new Home");removed(s,task);
        }});
        for(final String fault:new String[]{"missing_controller","controller_type","current_nonfinal","find_type","pending_type"}){
            tests.put("selected_native_getter_contract_"+fault,()->{try(Scene s=new Scene(fault)){
                current(s,false);ActivityManager.AppTask task=own(s);s.install();
                if(fault.equals("current_nonfinal")) {
                    check(!OneBackExit.tryBack(s.activity),"the selected native getter itself must retain its verified shape");preserved(s,task);
                } else {
                    check(OneBackExit.tryBack(s.activity),"a directly selected native Reels receiver does not depend on unrelated controller, find or pending-queue bookkeeping");removed(s,task);
                    check(task.removeCalls==1,"independent native receiver still removes exactly its owned task");
                }
            }});
        }
        tests.put("native_exit_only_removes_instagram_tasks",()->{try(Scene s=new Scene()){
            current(s,false);ActivityManager.AppTask first=own(s),second=task(s,12,IG),foreign=task(s,13,"com.example.browser");s.install();
            check(OneBackExit.tryBack(s.activity),"verified native exit handles Back");removed(s,first);removed(s,second);
            check(!foreign.excluded&&foreign.removeCalls==0,"another application's task is unchanged");
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception{
        int beforeCases=cases,beforeAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> item:scenarios().entrySet()){
            try{test(item.getKey(),item.getValue());}
            catch(Exception|AssertionError failure){failed++;System.out.println("FAIL "+item.getKey()+": "+failure);failure.printStackTrace(System.out);}
        }
        if(failed!=0)throw new AssertionError("Back121InstagramDispatchTest: "+failed+" failed");
        System.out.println("PASS Back121InstagramDispatchTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions");
    }
}
