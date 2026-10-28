package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import com.instagram.clips.intf.ClipsViewerConfig;
import com.instagram.clips.intf.ClipsViewerSource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;

/** Native-link and Back transaction models using the audited 452 descriptors.
 * Registration is deliberately performed BEFORE attachment, as A09 does in the
 * supplied APK. Native method execution is represented by explicit actions;
 * the separate emitted-DEX audit proves the actual hook placement. */
public final class Back130InstagramProfileLinkTest {
    static final String PROFILE="com.instagram.profile.fragment.UserDetailFragment";
    static final String URL="https://www.instagram.com/yoasobi_staff_/";
    static final String USER="InstagramUrlLaunchConstants.EXTRA_USER_NAME";

    static Bundle route(String url,String user) {
        return route("main_activity",url,user);
    }
    static Bundle route(Scene scene,String url,String user) {
        String host=scene.activity.getClass().getName();
        String module=host.equals("com.instagram.url.UrlHandlerActivity")?"url_handler"
            :host.equals("com.instagram.url.UrlHandlerLauncherActivity")?"url_handler_launcher":"main_activity";
        return route(module,url,user);
    }
    private static Bundle route(String module,String url,String user) {
        Bundle args=new Bundle();args.fixtureObject("uri",Uri.parse(url));args.putString(USER,user);
        // The external delegate merges this envelope with the parser result
        // before A09. A parser-only bundle is not an external-entry fixture.
        args.putString("original_url",url);args.putBoolean("com.instagram.url.extra.IS_ON_CREATE",true);
        args.putString("com.instagram.url.constants.ARGUMENTS_KEY_ANALYTICS_MODULE_NAME",module);
        return args;
    }
    static Node detached(Scene s)throws Exception {
        Node node=(Node)s.create(PROFILE);node.added=false;node.visible=false;node.resumed=false;
        node.activity=null;node.view=null;node.arguments=new Bundle();node.module="profile";return node;
    }
    static void attach(Scene s,Node node)throws Exception {
        node.activity=s.activity;node.view=new ViewGroup(s.activity);node.added=true;node.visible=true;node.resumed=true;
        set(node,"mFragmentManager",s.manager);set(node,"mParentFragment",null);set(node,"mFragmentId",MAIN);set(node,"mChildFragmentManager",s.newManager());
        added(s.manager).add(node);s.root.addView(node.view);s.select(node,true);
    }
    static Node linked(Scene s)throws Exception {return linked(s,route(s,URL,"yoasobi_staff_"));}
    static Node linked(Scene s,Bundle args)throws Exception {
        Node node=detached(s);OneBackExit.rememberInstagramExternalProfile(s.activity,node,args);
        check(node.activity==null&&!node.added,"link registration precedes native attachment");attach(s,node);s.profileTabs(node,true);return node;
    }
    static boolean back(Node node,boolean consumed,Runnable nativeAction) {
        OneBackExit.beginInstagramExternalProfileBack(node);
        if(nativeAction!=null)nativeAction.run();
        return OneBackExit.finishInstagramExternalProfileBack(node,consumed);
    }
    static boolean back(Node node){return back(node,false,null);}
    static ActivityManager.AppTask task(Scene s,String packageName,int id,Activity... activities){
        ActivityManager.RecentTaskInfo info=new ActivityManager.RecentTaskInfo();info.taskId=id;
        info.baseActivity=new ComponentName(packageName,"Root");info.baseIntent=new Intent().setComponent(info.baseActivity);
        ActivityManager.AppTask task=new ActivityManager.AppTask(info);task.activities.addAll(Arrays.asList(activities));s.tasks.tasks.add(task);return task;
    }
    static ActivityManager.AppTask own(Scene s){return task(s,"com.instagram.android",11,s.activity);}
    static void preserved(Scene s,ActivityManager.AppTask task){Back127Instagram452Test.preserved(s,task);}
    static void removed(Scene s,ActivityManager.AppTask task){Back127Instagram452Test.removed(s,task);}
    static void hide(Node node){node.resumed=false;node.visible=false;node.view.hidden=true;}
    static void reveal(Scene s,Node node)throws Exception {node.resumed=true;node.visible=true;node.view.hidden=false;s.select(node,true);}
    static void erase(Scene s,Node node)throws Exception {Back127Instagram452Test.erase(s,node);}
    static Object dispatcher(Scene s){Object d=new Object();OneBackExit.bindInstagramDispatcher(s.activity,d);return d;}
    static Object value(Object target,String name)throws Exception{return target.getClass().getField(name).get(target);}
    static Object tabController(Node profile)throws Exception{return value(profile,"A0b");}
    static Object tabAdapter(Node profile)throws Exception{return value(tabController(profile),"A0A");}
    static Object tabPager(Node profile)throws Exception{return value(value(tabController(profile),"viewHolder"),"A0F");}
    static Node tab(Node profile)throws Exception{return (Node)currentProfileTab(tabController(profile));}
    static final int MODAL_CONTENT=0x7f0b0aba;
    static final int MODAL_HOST=0x7f0b28fd;
    /** A live 03CG remains separate from Main's retained selected feed. */
    static final class ModalScene implements AutoCloseable {
        final Scene scene;final Node home,modal,profile;final Object host,manager;final ActivityManager.AppTask task;
        ModalScene()throws Exception{this(true,null);}
        ModalScene(boolean register)throws Exception{this(register,null);}
        ModalScene(Bundle route)throws Exception{this(true,route);}
        private ModalScene(boolean register,Bundle args)throws Exception {
            scene=new Scene();home=scene.node("X.00nV",null,MAIN);home.resumed=false;scene.select(home,true);
            modal=scene.node("X.03CG",null,MODAL_HOST);modal.module="profile";
            host=scene.create("X.0A4v");set(host,"A05",MODAL_HOST);set(host,"A06",new java.lang.ref.WeakReference<Object>(modal));set(scene.activity,"A09",host);
            manager=scene.child(modal);profile=detached(scene);
            if(register)OneBackExit.rememberInstagramExternalProfile(scene.activity,profile,args==null?route(scene,URL,"yoasobi_staff_"):args);
            profile.activity=scene.activity;profile.view=new ViewGroup(scene.activity);profile.added=true;profile.visible=true;profile.resumed=true;
            set(profile,"mFragmentManager",manager);set(profile,"mParentFragment",modal);set(profile,"mFragmentId",MODAL_CONTENT);set(profile,"mChildFragmentManager",scene.newManager());
            added(manager).add(profile);((ViewGroup)modal.view).addView(profile.view);scene.profileTabs(profile,true);task=own(scene);
        }
        Node push(String type)throws Exception {hide(profile);return scene.node(type,modal,MODAL_CONTENT);}
        void pop(Node child)throws Exception {erase(scene,child);profile.resumed=true;profile.visible=true;profile.view.hidden=false;}
        Node retainFeedBehindClips()throws Exception {
            erase(scene,home);Node clips=scene.node("X.00VS",null,MAIN);scene.cacheTab(clips,"clips");scene.select(clips,false);
            Node reel=scene.node("X.02Hr",clips,BASE);reel.config=new ClipsViewerConfig(ClipsViewerSource.CLIPS_TAB);reel.resumed=false;
            Node retained=scene.node("X.00VS",null,MAIN);scene.cacheTab(retained,"feed");retained.resumed=false;
            scene.root.removeView(modal.view);scene.root.addView(modal.view);return retained;
        }
        public void close(){scene.close();}
    }

    static Map<String,Test> scenarios(){Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final String host:new String[]{"","url"}) {
            final String name=host.isEmpty()?"main":"url";
            tests.put(name+"_cold_profile_native_back_exits",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);s.install();
                check(!OneBackExit.tryBack(s.activity),"profile native guards run before exit");
                check(!ExitScreens.shouldExit(s.activity),"OTHER is not granted generic early exit permission");
                check(back(profile),"unconsumed native initial-profile Back exits");removed(s,t);
            }});
            tests.put(name+"_native_consumption_precedes_exit",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);
                check(back(profile,true,null),"native consumed result preserved");preserved(s,t);
                Handler.drain();check(back(profile),"separate unconsumed Back can exit");removed(s,t);
            }});
            tests.put(name+"_internal_profile_not_an_exit",()->{try(Scene s=new Scene(host)){
                Node profile=detached(s);attach(s,profile);ActivityManager.AppTask t=own(s);
                check(!back(profile),"ordinary profile has no external entry registration");preserved(s,t);
            }});
            tests.put(name+"_ime_closes_without_same_back_exit",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);s.root.insets.ime=true;
                check(back(profile,false,()->s.root.insets.ime=false),"keyboard closure is consumed without granting this Back exit");preserved(s,t);
                Handler.drain();check(back(profile),"next Back after keyboard closure exits");removed(s,t);
            }});
            tests.put(name+"_comments_close_without_same_back_exit",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);
                View panel=s.group((ViewGroup)profile.view,"comment_sheet",new Rect(0,650,1080,2200));
                check(back(profile,false,()->((ViewGroup)profile.view).removeView(panel)),"native false after comment closure is consumed before parent finish");preserved(s,t);
                Handler.drain();check(back(profile),"subsequent Back after comments exits");removed(s,t);
            }});
            tests.put(name+"_public_comments_request_blocks_native_finish_cascade",()->{try(Scene s=new Scene(host)){
                final Node profile=linked(s);ActivityManager.AppTask t=own(s);
                final View panel=s.group((ViewGroup)profile.view,"comment_sheet",new Rect(0,650,1080,2200));
                s.activity.nativeBackAction=()->{((ViewGroup)profile.view).removeView(panel);if(!back(profile))s.activity.finish();};
                check(OneBackExit.tryBack(s.activity),"public Back routes comments through one native close action");
                check(s.activity.nativeBackCalls==1,"one native forward, with no second synthetic Back");preserved(s,t);
                Handler.drain();check(back(profile),"later independent Back exits after forwarding ends");removed(s,t);
            }});
            tests.put(name+"_layer_opened_during_back_vetoes_exit",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);
                check(!back(profile,false,()->s.group((ViewGroup)profile.view,"bottom_sheet",new Rect(0,700,1080,2200))),"post-native layer state is checked too");preserved(s,t);
            }});
            tests.put(name+"_child_stack_preserved",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);set(s.child(profile),"count",1);
                check(!back(profile),"profile child navigation owns Back");preserved(s,t);
            }});
            tests.put(name+"_visible_child_without_stack_preserved",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);s.node("X.NativeProfile",profile,MAIN);
                check(!back(profile),"an unknown visible profile child blocks even before stack bookkeeping");preserved(s,t);
            }});
            tests.put(name+"_native_popup_closed_false_still_preserves",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);Object presenter=s.create("X.0DiR");
                final PopupState popup=(PopupState)s.create("X.01MU");popup.open=true;set(presenter,"A01",popup);set(profile,"A0p",presenter);
                check(back(profile,false,()->popup.open=false),"native popup closure becomes consumed and cannot cascade into parent finish");preserved(s,t);
                Handler.drain();check(back(profile),"next Back after native popup closure exits");removed(s,t);
            }});
            tests.put(name+"_pending_navigation_preserved",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);set(s.manager,"mExecutingActions",true);
                check(!back(profile),"manager transaction in progress is not a settled external page");preserved(s,t);
            }});
            tests.put(name+"_native_profile_to_child_preserved",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);
                OneBackExit.beginInstagramExternalProfileBack(profile);hide(profile);Node child=s.node("X.NativeProfile",null,MAIN);s.select(child,true);
                check(!OneBackExit.finishInstagramExternalProfileBack(profile,false),"receiver identity must remain selected after native handling");preserved(s,t);
            }});
            tests.put(name+"_profile_reel_return_requires_next_back",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);hide(profile);Node video=s.node("X.02Hr",null,MAIN);
                video.config=new ClipsViewerConfig(ClipsViewerSource.PROFILE);video.module="clips_viewer_clips_profile";
                s.group((ViewGroup)video.view,"clips_viewer_container",new Rect(0,0,1080,2170));s.select(video,false);set(s.manager,"count",1);ActivityManager.AppTask t=own(s);
                check(!OneBackExit.tryInstagramReelsBack(video),"reel native navigation retains profile return");
                erase(s,video);reveal(s,profile);set(s.manager,"count",0);
                check(back(profile),"same native return is consumed without exiting the revealed initial profile");preserved(s,t);
                Handler.drain();check(back(profile),"independent next profile Back exits");removed(s,t);
            }});
            tests.put(name+"_stale_receiver_never_exits",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);hide(profile);Node child=s.node("X.NativeProfile",null,MAIN);s.select(child,true);
                check(!back(profile),"hidden old profile receiver cannot exit selected child");preserved(s,t);
            }});
            tests.put(name+"_destroy_clears_registration",()->{try(Scene s=new Scene(host)){
                Node profile=linked(s);ActivityManager.AppTask t=own(s);OneBackExit.onDestroy(s.activity);
                check(!back(profile),"destroyed owner registration is not reused");preserved(s,t);
            }});
        }
        tests.put("warm_main_profile_above_existing_feed",()->{try(Scene s=new Scene()){
            Node home=s.node("X.00nV",null,MAIN);s.select(home,true);hide(home);
            Node profile=linked(s);set(s.manager,"count",1);ActivityManager.AppTask t=own(s);
            check(back(profile),"native feed history behind the initial external page does not require another Back");removed(s,t);
        }});
        tests.put("new_link_replaces_old_entry_identity",()->{try(Scene s=new Scene()){
            Node old=linked(s);hide(old);Node fresh=linked(s,route("https://www.instagram.com/other_user/","other_user"));ActivityManager.AppTask t=own(s);
            hide(fresh);reveal(s,old);check(!back(old),"previous link receiver loses exit permission after new link");preserved(s,t);
            hide(old);reveal(s,fresh);Handler.drain();check(back(fresh),"new exact link receiver owns exit");removed(s,t);
        }});
        tests.put("foreign_browser_task_preserved",()->{try(Scene s=new Scene("url_nonroot")){
            Node profile=linked(s);Activity browser=new Activity(new Application("com.android.chrome"),11,true);
            ActivityManager.AppTask caller=task(s,"com.android.chrome",11,browser,s.activity);
            Activity other=new Activity(s.app,12,true);ActivityManager.AppTask own=task(s,"com.instagram.android",12,other);
            check(back(profile),"nonroot externally opened profile still consumes exit");
            check(!caller.excluded&&caller.excludeCalls==0&&caller.removeCalls==0,"caller task is neither excluded nor removed");
            check(!browser.finishing&&browser.finishCalls==0&&browser.removeCalls==0,"browser Activity survives");
            check(s.activity.finishCalls==1&&s.activity.removeCalls==0,"only Instagram child is finished inside foreign task");
            check(own.excluded&&own.removeCalls==1,"separate Instagram-owned task is removed");
        }});
        tests.put("finish_without_matching_begin_is_native",()->{try(Scene s=new Scene()){
            Node profile=linked(s);ActivityManager.AppTask t=own(s);
            check(!OneBackExit.finishInstagramExternalProfileBack(profile,false),"a missing begin cannot authorize exit");preserved(s,t);
        }});
        tests.put("native_scope_child_pop_does_not_exit_profile",()->{try(Scene s=new Scene()){
            Node profile=linked(s);hide(profile);Node child=s.node("X.NativeProfile",null,MAIN);s.select(child,true);ActivityManager.AppTask t=own(s);
            OneBackExit.enterBack(s.activity);
            try{erase(s,child);reveal(s,profile);check(back(profile),"Back beginning on a child is consumed after its native pop");preserved(s,t);}
            finally{OneBackExit.leaveNative();}
            Handler.drain();check(back(profile),"later profile Back exits");removed(s,t);
        }});
        tests.put("prediction_cancel_never_exits",()->{try(Scene s=new Scene()){
            Node profile=linked(s);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            check(!OneBackExit.startInstagramDispatcherGesture(d),"native prediction begins normally");
            check(!OneBackExit.cancelInstagramDispatcherGesture(d),"native cancellation stays native");preserved(s,t);
            Handler.drain();check(back(profile),"cancelled prediction does not poison later Back");removed(s,t);
        }});
        tests.put("prediction_started_on_child_cannot_exit_revealed_profile",()->{try(Scene s=new Scene()){
            Node profile=linked(s);hide(profile);Node child=s.node("X.NativeProfile",null,MAIN);s.select(child,true);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            check(!OneBackExit.startInstagramDispatcherGesture(d),"child predictive Back stays native");erase(s,child);reveal(s,profile);
            check(!OneBackExit.tryInstagramDispatcherBack(d),"commit remains native");
            check(back(profile),"gesture's completed child return is consumed and cannot exit profile");preserved(s,t);
            OneBackExit.cancelInstagramDispatcherGesture(d);Handler.drain();check(back(profile),"subsequent independent profile Back exits");removed(s,t);
        }});
        tests.put("nonpredictive_dispatcher_child_pop_cannot_exit_profile",()->{try(Scene s=new Scene()){
            Node profile=linked(s);hide(profile);Node child=s.node("X.NativeProfile",null,MAIN);child.module="media_detail";s.select(child,true);ActivityManager.AppTask t=own(s);Object d=dispatcher(s);
            check(!OneBackExit.tryInstagramDispatcherBack(d),"ordinary child commit retains native navigation");
            erase(s,child);reveal(s,profile);check(back(profile),"synchronous profile callback consumes the completed child return");preserved(s,t);
            Handler.drain();check(back(profile),"later native profile callback can exit");removed(s,t);
        }});
        tests.put("physical_key_child_down_profile_up_cannot_exit",()->{try(Scene s=new Scene()){
            Node profile=linked(s);hide(profile);Node child=s.node("X.NativeProfile",null,MAIN);child.module="media_detail";s.select(child,true);ActivityManager.AppTask t=own(s);
            check(!OneBackExit.key(s.activity,down()),"child Back key down stays native");erase(s,child);reveal(s,profile);
            check(!OneBackExit.key(s.activity,up()),"same key release remains native");check(back(profile),"release consumes completed return without authorizing profile exit");preserved(s,t);
            Handler.drain();check(!OneBackExit.key(s.activity,down()),"independent profile key down reaches native guards");check(!OneBackExit.key(s.activity,up()),"independent profile key up reaches native guards");
            check(back(profile),"new key pair on initial profile exits");removed(s,t);
        }});
        tests.put("cancelled_key_never_authorizes_profile_exit",()->{try(Scene s=new Scene()){
            Node profile=linked(s);ActivityManager.AppTask t=own(s);
            check(!OneBackExit.key(s.activity,down()),"profile key reaches native guards");
            check(!OneBackExit.key(s.activity,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,true)),"cancelled release remains native");
            check(back(profile),"unexpected same-transaction callback after cancellation is stopped before parent exit");preserved(s,t);
            Handler.drain();check(back(profile),"later distinct Back still works");removed(s,t);
        }});
        tests.put("preime_duplicate_down_preserves_start_then_fresh_key_works",()->{try(Scene s=new Scene()){
            Node profile=linked(s);ActivityManager.AppTask t=own(s);s.root.insets.ime=true;
            KeyEvent first=down();first.downTime=100;
            check(!OneBackExit.keyFromView(profile.view,first),"pre-IME Back is delegated");s.root.insets.ime=false;
            check(!OneBackExit.key(s.activity,first),"inherited duplicate DOWN does not refresh initial IME state");
            check(back(profile),"IME closing consumes the completed action without same key exit");preserved(s,t);
            Handler.drain();KeyEvent next=down();next.downTime=200;KeyEvent release=up();release.downTime=200;
            check(!OneBackExit.key(s.activity,next)&&!OneBackExit.key(s.activity,release),"fresh key after swallowed IME UP reaches native profile handler");
            check(back(profile),"distinct downTime replaces obsolete IME key origin");removed(s,t);
        }});
        tests.put("new_intent_invalidates_stale_entry_before_navigation",()->{try(Scene s=new Scene()){
            Node profile=linked(s);ActivityManager.AppTask t=own(s);
            OneBackExit.onNewIntent(s.activity,new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://www.instagram.com/reel/other/")));
            check(!back(profile),"new route removes the previous profile's permission before target changes");preserved(s,t);
        }});
        tests.put("inherited_new_intent_does_not_erase_new_profile_capture",()->{try(Scene s=new Scene()){
            Intent incoming=new Intent(Intent.ACTION_VIEW).setData(Uri.parse(URL));OneBackExit.onNewIntent(s.activity,incoming);
            Node profile=linked(s);ActivityManager.AppTask t=own(s);OneBackExit.onNewIntent(s.activity,incoming);
            check(back(profile),"same incoming Intent through super callback preserves newly captured exact profile");removed(s,t);
        }});
        tests.put("dialog_closure_still_requires_separate_back",()->{try(Scene s=new Scene()){
            Node profile=linked(s);ActivityManager.AppTask t=own(s);Dialog dialog=new Dialog(s.activity);dialog.show();dialog.getWindow().getDecorView().focus(true);
            try{check(back(profile,false,()->dialog.dismiss()),"foreground dialog closure consumes this Back before parent finish");preserved(s,t);}
            finally{dialog.dismiss();s.root.focus(true);}
            Handler.drain();check(back(profile),"subsequent profile Back after dialog closure exits");removed(s,t);
        }});
        tests.put("loading_profile_without_created_tabs_is_supported",()->{try(Scene s=new Scene()){
            Node profile=detached(s);OneBackExit.rememberInstagramExternalProfile(s.activity,profile,route(URL,"yoasobi_staff_"));attach(s,profile);ActivityManager.AppTask t=own(s);
            check(back(profile),"verified initial loading profile does not require fabricated grid views");removed(s,t);
        }});
        tests.put("selected_second_profile_tab_with_retained_first_exits",()->{try(Scene s=new Scene()){
            Node profile=linked(s);Object adapter=tabAdapter(profile);Node first=tab(profile),second=(Node)profileCached(adapter,1);
            first.resumed=false;first.view.bounds=new Rect(-1080,500,0,2170);second.resumed=true;second.view.bounds=new Rect(0,500,1080,2170);
            set(tabPager(profile),"A02",1);s.loader.loadClass("X.00fy").getDeclaredField("A01").set(adapter,second);ActivityManager.AppTask t=own(s);
            check(back(profile),"verified nonzero selected profile tab is ordinary initial-profile content");removed(s,t);
        }});
        for(final String fault:new String[]{"moving_pager","wrong_primary","wrong_child_manager","wrong_adapter","missing_cache","out_of_range_index","selected_not_resumed","selected_wrong_parent","selected_child_stack","selected_child_overlay","visible_adjacent_tab"})
            tests.put("native_profile_grid_rejects_"+fault,()->{try(Scene s=new Scene()){
                Node profile=linked(s);Node selected=tab(profile);Object adapter=tabAdapter(profile),pager=tabPager(profile);ActivityManager.AppTask t=own(s);
                if(fault.equals("moving_pager"))set(pager,"A0W",1);
                else if(fault.equals("wrong_primary"))s.loader.loadClass("X.00fy").getDeclaredField("A01").set(adapter,profileCached(adapter,1));
                else if(fault.equals("wrong_child_manager"))set(adapter,"A06",s.newManager());
                else if(fault.equals("wrong_adapter"))set(pager,"A0B",s.create("X.0EIp"));
                else if(fault.equals("missing_cache"))((android.util.SparseArray<?>)value(adapter,"A05")).remove(0);
                else if(fault.equals("out_of_range_index"))set(pager,"A02",2);
                else if(fault.equals("selected_not_resumed"))selected.resumed=false;
                else if(fault.equals("selected_wrong_parent"))set(selected,"mParentFragment",null);
                else if(fault.equals("selected_child_stack"))set(s.child(selected),"count",1);
                else if(fault.equals("selected_child_overlay"))s.node("X.NativeProfile",selected,MAIN);
                else {Node adjacent=(Node)profileCached(adapter,1);adjacent.view.bounds=new Rect(0,500,1080,2170);}
                check(!back(profile),"unsettled or unverified native profile tab remains native: "+fault);preserved(s,t);
            }});
        tests.put("url_return_to_sender_root_uses_native_profile_chain",()->{try(Scene s=new Scene("url")){
            Node profile=linked(s);ActivityManager.AppTask t=own(s);check(((ManagerState)s.manager).count==0,"native URL root stack is empty");
            check(OneBackExit.dispatchInstagramExternalProfile(s.activity),"URL return-to-sender decision selects existing native profile Back chain");
            check(back(profile),"native profile can exit after guards");removed(s,t);
        }});
        tests.put("url_internal_or_child_route_retains_original_delegate",()->{try(Scene s=new Scene("url")){
            Node internal=detached(s);attach(s,internal);check(!OneBackExit.dispatchInstagramExternalProfile(s.activity),"unregistered profile does not change URL delegation");
            hide(internal);Node profile=linked(s);hide(profile);Node child=s.node("X.NativeProfile",null,MAIN);s.select(child,true);ActivityManager.AppTask t=own(s);
            check(!OneBackExit.dispatchInstagramExternalProfile(s.activity),"a different current child retains original URL behavior");preserved(s,t);
        }});
        tests.put("url_popup_false_closure_cannot_fall_through_to_parent_finish",()->{try(Scene s=new Scene("url")){
            Node profile=linked(s);ActivityManager.AppTask t=own(s);Object presenter=s.create("X.0DiR");final PopupState popup=(PopupState)s.create("X.01MU");popup.open=true;set(presenter,"A01",popup);set(profile,"A0p",presenter);
            OneBackExit.enterBack(s.activity);
            try{check(OneBackExit.dispatchInstagramExternalProfile(s.activity),"blocked profile still reaches its native popup handler");
                boolean consumed=back(profile,false,()->popup.open=false);if(!consumed)s.activity.finish();
                check(consumed,"completed popup closure stops native parent's finish fallback");preserved(s,t);
            }finally{OneBackExit.leaveNative();}
            Handler.drain();check(back(profile),"next independent native Back exits");removed(s,t);
        }});
        tests.put("warm_main_live_modal_profile_grid_exits",()->{try(ModalScene f=new ModalScene()){
            check(f.scene.activity.current==f.home&&f.home.visible,"Main selection remains visible feed behind live modal");
            check(!OneBackExit.tryBack(f.scene.activity),"modal profile native Back runs before exit");
            check(back(f.profile),"registered initial profile in exact live Main modal exits");removed(f.scene,f.task);
        }});
        tests.put("warm_main_modal_unregistered_profile_stays_native",()->{try(ModalScene f=new ModalScene(false)){
            check(!back(f.profile),"ordinary modal profile is never granted external-entry exit");preserved(f.scene,f.task);
        }});
        for(final String stale:new String[]{"detached","hidden","replaced"})
            tests.put("warm_main_stale_modal_"+stale+"_restores_main_inspection",()->{try(ModalScene f=new ModalScene()){
                f.home.resumed=true;
                if(stale.equals("detached"))f.modal.added=false;
                else if(stale.equals("hidden"))f.modal.visible=false;
                else {Node replacement=f.scene.node("X.03CG",null,MODAL_HOST+1);set(f.host,"A06",new java.lang.ref.WeakReference<Object>(replacement));}
                int reads=f.scene.activity.nativeCurrentReads;
                InstagramBack.Snapshot current=InstagramBack.inspect(f.scene.activity);
                check(f.scene.activity.nativeCurrentReads>reads,"stale modal registration no longer suppresses ordinary Main getter: "+stale);
                check(current.current!=null&&current.current.nativeRecognized&&current.current.target==f.home,"ordinary Main content remains inspectable after modal "+stale);
                check(!back(f.profile),"profile retained under a stale modal cannot exit: "+stale);preserved(f.scene,f.task);
            }});
        tests.put("warm_main_modal_entry_above_existing_modal_history_exits",()->{try(ModalScene f=new ModalScene()){
            set(f.manager,"count",1);
            check(back(f.profile),"external entry may be a native push over earlier modal history");removed(f.scene,f.task);
        }});
        tests.put("warm_main_modal_auto_close_with_child_history_uses_profile_guard",()->{try(ModalScene f=new ModalScene()){
            set(f.manager,"count",1);set(f.host,"A0C",true);
            check(back(f.profile),"nonempty native modal history still reaches its selected profile Back guard");removed(f.scene,f.task);
        }});
        tests.put("warm_main_modal_selected_background_may_remain_resumed",()->{try(ModalScene f=new ModalScene()){
            f.home.resumed=true;
            check(back(f.profile),"native selected Main source can remain resumed below distinct live modal container");removed(f.scene,f.task);
        }});
        tests.put("warm_main_modal_from_clips_with_retained_feed_exits",()->{try(ModalScene f=new ModalScene()){
            f.retainFeedBehindClips();
            check(back(f.profile),"selected CLIPS and initialized inactive FEED caches remain background to the external profile");removed(f.scene,f.task);
        }});
        for(final String fault:new String[]{"resumed","unrecognized_key","uninitialized_cache"})
            tests.put("warm_main_modal_retained_tab_rejects_"+fault,()->{try(ModalScene f=new ModalScene()){
                Node retained=f.retainFeedBehindClips();
                if(fault.equals("resumed"))retained.resumed=true;
                else {Object cache=value(retained,"A07");set(cache,"A00",fault.equals("uninitialized_cache")?null:"unknown_tab");}
                int reads=f.scene.activity.nativeCurrentReads,modules=retained.moduleReads;
                check(!back(f.profile),"unverified retained tab does not become ignored background: "+fault);preserved(f.scene,f.task);
                if(fault.equals("uninitialized_cache")) {
                    check(f.scene.activity.nativeCurrentReads==reads,"uninitialized cache is rejected before any potentially initializing Main getter");
                    check(retained.moduleReads==modules,"uninitialized wrapper never reaches its potentially initializing module getter");
                    for(String entry:new String[]{"try_back","predictive","native_scope"})try(ModalScene g=new ModalScene()) {
                        Node uncached=g.retainFeedBehindClips();set(value(uncached,"A07"),"A00",null);
                        int before=g.scene.activity.nativeCurrentReads,beforeModules=uncached.moduleReads;
                        if(entry.equals("try_back"))check(!OneBackExit.tryBack(g.scene.activity),"unverified modal public Back stays native");
                        else if(entry.equals("predictive")) {
                            Object dispatcher=dispatcher(g.scene);
                            check(!OneBackExit.startInstagramDispatcherGesture(dispatcher),"uncached modal prediction stays native");
                            check(!OneBackExit.progressInstagramDispatcherGesture(dispatcher),"uncached modal progress stays native");
                            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher),"uncached modal commit stays native");
                        } else {
                            OneBackExit.enterBack(g.scene.activity);
                            try {check(!back(g.profile),"uncached modal native scope cannot authorize profile exit");}
                            finally {OneBackExit.leaveNative();}
                        }
                        check(g.scene.activity.nativeCurrentReads==before,"uncached modal "+entry+" never initializes Main tab selection");
                        check(uncached.moduleReads==beforeModules,"uncached modal "+entry+" never invokes native wrapper module getter");
                        preserved(g.scene,g.task);
                    }
                }
            }});
        tests.put("warm_main_modal_predictive_dispatch_reaches_profile_guard",()->{try(ModalScene f=new ModalScene()){
            Object d=dispatcher(f.scene);
            check(!OneBackExit.startInstagramDispatcherGesture(d),"native modal profile owns predictive Back start");
            check(!OneBackExit.progressInstagramDispatcherGesture(d),"native modal profile owns predictive progress");
            check(!OneBackExit.tryInstagramDispatcherBack(d),"committed gesture reaches original modal/profile handlers");
            check(back(f.profile),"same initial modal profile may exit at its native post-guard boundary");removed(f.scene,f.task);
        }});
        tests.put("warm_main_modal_physical_key_reaches_profile_guard",()->{try(ModalScene f=new ModalScene()){
            check(!OneBackExit.key(f.scene.activity,down())&&!OneBackExit.key(f.scene.activity,up()),"physical key remains native until modal profile guards run");
            check(back(f.profile),"initial modal profile Back key exits once");removed(f.scene,f.task);
        }});
        tests.put("warm_main_modal_public_comments_request_preserves_profile",()->{try(ModalScene f=new ModalScene()){
            final View panel=f.scene.group((ViewGroup)f.profile.view,"comment_sheet",new Rect(0,650,1080,2200));
            f.scene.activity.nativeBackAction=()->{((ViewGroup)f.profile.view).removeView(panel);if(!back(f.profile))f.scene.activity.finish();};
            check(OneBackExit.tryBack(f.scene.activity),"real comments still route to one native close through specialized modal snapshot");
            check(f.scene.activity.nativeBackCalls==1,"specialized modal adds no second Back");preserved(f.scene,f.task);
            Handler.drain();check(back(f.profile),"next independent Back exits after modal comments close");removed(f.scene,f.task);
        }});
        tests.put("warm_main_modal_profile_reel_return_then_next_back",()->{try(ModalScene f=new ModalScene()){
            Node reel=f.push("X.02Hr");reel.config=new ClipsViewerConfig(ClipsViewerSource.PROFILE);reel.module="clips_viewer_clips_profile";
            f.scene.group((ViewGroup)reel.view,"clips_viewer_container",new Rect(0,0,1080,2170));set(f.manager,"count",1);
            check(!OneBackExit.tryInstagramReelsBack(reel),"modal profile reel retains original native pop");f.pop(reel);set(f.manager,"count",0);
            check(back(f.profile),"same return is consumed before another modal pop or exit");preserved(f.scene,f.task);
            Handler.drain();check(back(f.profile),"next independent Back exits initial modal profile");removed(f.scene,f.task);
        }});
        tests.put("warm_main_modal_generic_child_pop_does_not_exit",()->{try(ModalScene f=new ModalScene()){
            Node child=f.push("X.NativeProfile");child.module="media_detail";set(f.manager,"count",1);Object d=dispatcher(f.scene);
            f.scene.activity.nativeBackAction=()->{try{f.pop(child);set(f.manager,"count",0);if(!back(f.profile))f.scene.activity.finish();}catch(Exception failure){throw new AssertionError(failure);}};
            if(!OneBackExit.tryInstagramDispatcherBack(d))f.scene.activity.onBackPressed();
            check(f.scene.activity.nativeBackCalls==1,"ordinary modal child receives exactly one native Back action");
            check(findId(f.manager,MODAL_CONTENT)==f.profile,"native pop returns exact registered initial profile");preserved(f.scene,f.task);
            Handler.drain();check(back(f.profile),"next independent initial-profile Back exits");removed(f.scene,f.task);
        }});
        tests.put("warm_main_modal_popup_closure_prevents_parent_finish",()->{try(ModalScene f=new ModalScene()){
            Object presenter=f.scene.create("X.0DiR");final PopupState popup=(PopupState)f.scene.create("X.01MU");popup.open=true;set(presenter,"A01",popup);set(f.profile,"A0p",presenter);
            boolean consumed=back(f.profile,false,()->popup.open=false);if(!consumed)f.scene.activity.finish();
            check(consumed,"native popup false-return cannot cascade into modal finish");preserved(f.scene,f.task);
            Handler.drain();check(back(f.profile),"separate Back after popup closure exits");removed(f.scene,f.task);
        }});
        tests.put("warm_main_modal_native_layer_return_requires_next_back",()->{try(ModalScene f=new ModalScene()){
            Object layer=f.scene.create("X.00i7");set(layer,"A0W",true);((NativeActivity452Fixture.LayerState)layer).current=f.profile;set(f.modal,"A01",layer);
            OneBackExit.enterBack(f.scene.activity);
            try{((NativeActivity452Fixture.LayerState)layer).current=null;
                check(back(f.profile),"native modal-layer closure cannot authorize a second action on exposed profile");preserved(f.scene,f.task);
            }finally{OneBackExit.leaveNative();}
            Handler.drain();check(back(f.profile),"next independent Back after native modal-layer closure exits");removed(f.scene,f.task);
        }});
        for(final String fault:new String[]{"different_live_modal","missing_live_modal","wrong_root_modal_container","modal_not_added","modal_wrong_owner","background_wrong_parent","arbitrary_visible_root_sibling","wrong_selected_child","wrong_profile_container","wrong_profile_manager","root_pending","root_state_saved","modal_pending","modal_state_saved","modal_executing","visible_other_modal","visible_child_overlay","root_dialog","modal_dialog","native_modal_layer","service_two_pane","service_right_pane","service_source_manager","modal_two_pane","modal_right_pane","modal_source_argument","empty_auto_close"})
            tests.put("warm_main_modal_rejects_"+fault,()->{try(ModalScene f=new ModalScene()){
                if(fault.equals("different_live_modal")){Node other=f.scene.node("X.03CG",null,1943);set(f.host,"A06",new java.lang.ref.WeakReference<Object>(other));}
                else if(fault.equals("missing_live_modal"))set(f.host,"A06",new java.lang.ref.WeakReference<Object>(null));
                else if(fault.equals("wrong_root_modal_container"))set(f.host,"A05",MAIN);
                else if(fault.equals("modal_not_added"))f.modal.added=false;
                else if(fault.equals("modal_wrong_owner"))f.modal.activity=new Activity(f.scene.app,12,true);
                else if(fault.equals("background_wrong_parent"))set(f.home,"mParentFragment",f.modal);
                else if(fault.equals("arbitrary_visible_root_sibling"))f.scene.node("X.NativeProfile",null,1946);
                else if(fault.equals("wrong_selected_child")){Node child=f.scene.node("X.NativeProfile",f.modal,MODAL_CONTENT);child.module="media_detail";}
                else if(fault.equals("wrong_profile_container"))set(f.profile,"mFragmentId",MAIN);
                else if(fault.equals("wrong_profile_manager"))set(f.profile,"mFragmentManager",f.scene.newManager());
                else if(fault.equals("root_pending"))((java.util.List<Object>)value(f.scene.manager,"A0W")).add(new Object());
                else if(fault.equals("root_state_saved"))set(f.scene.manager,"saved",true);
                else if(fault.equals("modal_pending"))((java.util.List<Object>)value(f.manager,"A0W")).add(new Object());
                else if(fault.equals("modal_state_saved"))set(f.manager,"saved",true);
                else if(fault.equals("modal_executing"))set(f.manager,"mExecutingActions",true);
                else if(fault.equals("visible_other_modal"))f.scene.node("X.03CG",null,1943);
                else if(fault.equals("visible_child_overlay"))f.scene.node("X.NativeProfile",f.modal,1944);
                else if(fault.equals("service_two_pane"))set(f.host,"A0A",true);
                else if(fault.equals("service_right_pane"))set(f.host,"A04",true);
                else if(fault.equals("service_source_manager"))set(f.host,"A0B",true);
                else if(fault.equals("modal_two_pane"))set(f.modal,"A08",true);
                else if(fault.equals("modal_right_pane"))set(f.modal,"A07",true);
                else if(fault.equals("modal_source_argument")){f.modal.arguments=new Bundle();f.modal.arguments.putBoolean("MODAL_FRAGMENT_ARG_ROUTE_THROUGH_SOURCE_FM",true);}
                else if(fault.equals("empty_auto_close"))set(f.host,"A0C",true);
                else if(fault.equals("native_modal_layer")){Object layer=f.scene.create("X.00i7");set(layer,"A0W",true);((NativeActivity452Fixture.LayerState)layer).current=f.profile;set(f.modal,"A01",layer);}
                else {Node dialog=f.scene.node("X.007r",fault.equals("root_dialog")?null:f.modal,1945);Dialog window=new Dialog(f.scene.activity);window.show();set(dialog,"A00",window);}
                check(!back(f.profile),"unverified modal relationship/layer cannot exit: "+fault);preserved(f.scene,f.task);
            }});
        for(final String bad:new String[]{"https://www.instagram.com/reel/abc/","https://www.instagram.com/yoasobi_staff_/reels/","https://www.instagram.com/yoasobi_staff_/?launch_reel=1","https://instagram.com.evil.example/yoasobi_staff_/","https://example.com/yoasobi_staff_/","ig://user?username=yoasobi_staff_"})
            tests.put("nonplain_profile_route_rejected_"+bad,()->{try(Scene s=new Scene()){
                Node profile=linked(s,route(bad,"yoasobi_staff_"));ActivityManager.AppTask t=own(s);
                check(!back(profile),"only the verified plain HTTP(S) profile entry qualifies");preserved(s,t);
            }});
        tests.put("route_username_mismatch_rejected",()->{try(Scene s=new Scene()){
            Node profile=linked(s,route(URL,"other_user"));ActivityManager.AppTask t=own(s);
            check(!back(profile),"URI and native parsed username must agree");preserved(s,t);
        }});
        tests.put("tracking_query_profile_supported",()->{try(Scene s=new Scene()){
            Node profile=linked(s,route(URL+"?igsh=abc123&utm_source=browser","yoasobi_staff_"));ActivityManager.AppTask t=own(s);
            check(back(profile),"sharing/tracking query retains plain profile semantics");removed(s,t);
        }});
        tests.put("unattached_registered_fragment_cannot_exit",()->{try(Scene s=new Scene()){
            Node profile=detached(s);OneBackExit.rememberInstagramExternalProfile(s.activity,profile,route(URL,"yoasobi_staff_"));ActivityManager.AppTask t=own(s);
            check(!back(profile),"native navigation must actually attach the registered profile");preserved(s,t);
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception {
        int previousCases=cases,previousAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> entry:scenarios().entrySet())try{test(entry.getKey(),entry.getValue());}
        catch(AssertionError error){failed++;System.out.println("FAIL "+entry.getKey()+": "+error.getMessage());Handler.drain();}
        if(failed!=0)throw new AssertionError("Back130InstagramProfileLinkTest failed="+failed);
        System.out.println("PASS Back130InstagramProfileLinkTest: "+(cases-previousCases)+" cases, "+(assertions-previousAssertions)+" assertions; host models, no device execution");
    }
}
