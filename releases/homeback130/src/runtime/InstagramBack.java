package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.*;

/** Comments/layers are dismissed, never interpreted as the Reels/Home screen behind them. */
final class InstagramBack {
    private InstagramBack() {}
    private static final Map<Activity,Guard> GUARDS=new WeakHashMap<Activity,Guard>();
    private static final Map<Activity,java.lang.ref.WeakReference<Object>> PREDICTION_CONTENT=new WeakHashMap<Activity,java.lang.ref.WeakReference<Object>>();
    private static final Map<Activity,Integer> PREDICTIONS=new WeakHashMap<Activity,Integer>();
    private static final ThreadLocal<Boolean> FORWARDING=new ThreadLocal<Boolean>();
    private static final ThreadLocal<Boolean> INTERNAL=new ThreadLocal<Boolean>();
    private static final ThreadLocal<Origin> NATIVE_ORIGIN=new ThreadLocal<Origin>();
    private static final long WINDOW=1200,DUPLICATE=350;
    private static final class Guard {
        final int task;final String pkg;final long start=SystemClock.uptimeMillis();boolean done;
        Guard(Activity a){task=a.getTaskId();pkg=a.getPackageName();}
    }
    private static final class Origin {
        final Activity activity;final Object target;final boolean reels,exit;
        Origin(Activity activity,Snapshot scene){this.activity=activity;target=scene.current.target;
            reels=scene.current.kind==InstagramCurrent.REELS&&scene.current.stateClear
                &&!scene.blocked&&!scene.unknown&&!scene.otherContent;
            exit=ExitScreens.shouldExit(activity,scene);}
    }
    static final class Snapshot {
        boolean comments,blocked,unknown,otherContent;View close;
        InstagramCurrent.Result current;
    }
    static boolean owns(Activity a){return a!=null&&"com.instagram.android".equals(a.getPackageName());}
    private static Guard guard(Activity a){
        Guard g=GUARDS.get(a);
        if(g!=null&&(a.isDestroyed()||g.task!=a.getTaskId()||SystemClock.uptimeMillis()-g.start>WINDOW)){GUARDS.remove(a);return null;}
        return g;
    }
    static boolean ready(View root) {
        if(root==null||!root.isShown()||!root.isAttachedToWindow()||!root.hasWindowFocus())return false;
        try {
            if(Build.VERSION.SDK_INT>=30)return root.getRootWindowInsets()!=null&&!root.getRootWindowInsets().isVisible(android.view.WindowInsets.Type.ime());
            Rect full=new Rect(),visible=new Rect();root.getGlobalVisibleRect(full);root.getWindowVisibleDisplayFrame(visible);
            return full.bottom-visible.bottom<full.height()/5;
        }catch(RuntimeException|LinkageError failed){return false;}
    }
    static Snapshot inspect(Activity a) {
        Snapshot s=new Snapshot();if(!owns(a)||a.isDestroyed()||a.isFinishing()){s.unknown=true;return s;}
        try {
            // Native Reels popups can have no identifying View/resource label.
            // Include their verified state before tryBack and gesture prediction,
            // so request/protect retain the same-gesture guard while native Back
            // closes them, even if that closure immediately reveals clear Reels.
            s.current=InstagramExternalProfile.modal(a);
            if(s.current!=null&&!s.current.nativeRecognized) {
                // The registered modal route failed its read-only contract.
                // Even generic fragment module getters can initialize a tab's
                // lazy value, so preserve native Back without further scanning.
                s.unknown=true;return s;
            }
            Object permittedModal=s.current==null?null:s.current.selected;
            if(s.current==null) {
                s.current=InstagramReelsExit.current(a);
                if(!s.current.nativeRecognized||s.current.kind!=InstagramCurrent.REELS)s.current=InstagramCurrent.inspect(a);
            }
            Object content=s.current.available?s.current.target:InstagramReels.foregroundFragment(a);
            s.blocked=InstagramNativeLayers.blocksBeforeNative(content);
            Set<View> covered;
            if(s.current.available){
                s.unknown=!s.current.nativeRecognized||!s.current.stateClear;
                int nativeLayers=permittedModal==null?InstagramActivityLayers.inspect(a)
                    :InstagramActivityLayers452.inspect(a,permittedModal);
                s.blocked|=nativeLayers==InstagramActivityLayers.BLOCKED;
                s.unknown|=nativeLayers==InstagramActivityLayers.UNKNOWN;
                covered=s.current.backgroundRoots;
            }else covered=InstagramReels.coveredBackground(a);
            Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
            if(s.current.nativeRecognized&&s.current.kind==InstagramCurrent.REELS) {
                // The exact selected Reels receiver owns this Back. Native
                // Activity guards cover external panels/dialogs. Inspect only
                // this receiver's content and child layers, never retained tabs.
                scan(s.current.view,s);
                fragments(BackNavigation.invoke(content,"getChildFragmentManager"),s,seen,0);
            } else {
                scan(a.getWindow()==null?null:a.getWindow().peekDecorView(),s,covered);
                fragments(InstagramReels.supportManager(a),s,seen,0,covered);
                fragments(BackNavigation.invoke(a,"getFragmentManager"),s,seen,0,covered);
            }
        }catch(RuntimeException|LinkageError failed){s.unknown=true;}
        return s;
    }
    static Snapshot window(View root){Snapshot s=new Snapshot();try{scan(root,s);}catch(RuntimeException|LinkageError failed){s.unknown=true;}return s;}
    /** Fragment.isVisible() does not account for a retained pane translated offscreen,
     * nor for a transparent parent. Require pixels in the Activity's visible root. */
    static boolean shown(View view,Rect bounds) {
        if(!TikTokScreen.shown(view,bounds))return false;
        View root=view.getRootView();Rect screen=new Rect();
        if(root!=view&&(!TikTokScreen.shown(root,screen)||bounds.right<=screen.left||bounds.left>=screen.right
                ||bounds.bottom<=screen.top||bounds.top>=screen.bottom))return false;
        View parent=view;int depth=0;
        while(parent!=null&&depth++<64){
            if(!parent.isShown()||parent.getAlpha()<=.01f)return false;
            parent=parent.getParent() instanceof View?(View)parent.getParent():null;
        }
        return parent==null;
    }
    private static boolean reelsContentAbove(View view,Rect screen) {
        // The footer/caption belongs to its Reels content. Looking for a viewer
        // from decor made unrelated retained tabs consume the wrapper budget.
        View decor=view.getRootView();int depth=0;
        for(View parent=view;parent!=null&&parent!=decor&&depth++<64;
                parent=parent.getParent() instanceof View?(View)parent.getParent():null)
            if(InstagramReels.wrapper(parent,screen))return true;
        return false;
    }
    private static boolean inlineCommentPrompt(View view,Rect area,Rect screen) {
        // v447 layout_clips_comment_composer is a permanent Reels footer CTA:
        // its IgTextView is not an editor and does not mean a comments sheet is open.
        String id=TikTokScreen.id(view);
        if(!Arrays.asList("comment_composer_inner_layout","comment_composer_text_view","clips_comment_composer_stub").contains(id)
                ||view.onCheckIsTextEditor()||area.centerY()<screen.top+screen.height()*.65f
                ||area.height()>screen.height()*.25f)return false;
        int depth=0;
        for(View parent=view;parent!=null&&depth++<64;parent=parent.getParent() instanceof View?(View)parent.getParent():null){
            if("clips_footer_mask_container".equals(TikTokScreen.id(parent)))
                return reelsContentAbove(parent,screen);
        }
        return false;
    }
    private static boolean captionText(View view,Rect screen) {
        // A reel caption may literally say "コメント". Its text is content, while
        // a separately hosted comments heading must still block the Reels exit.
        int depth=0;
        for(View parent=view;parent!=null&&depth++<64;parent=parent.getParent() instanceof View?(View)parent.getParent():null)
            if("clips_caption_component".equals(TikTokScreen.id(parent)))return reelsContentAbove(parent,screen);
        return false;
    }
    private static boolean commentContainer(String id) {
        if(id.contains("comment")&&TikTokScreen.contains(id,"button","icon","count","preview","badge"))return false;
        return id.contains("comment")&&TikTokScreen.contains(id,"thread","sheet","panel","container","root","list","recycler","composer","edit_text","layout");
    }
    private static boolean modal(String id){return TikTokScreen.contains(id,"bottom_sheet","bottomsheet","share_sheet","sheet_container","sheet_root","dialog_container","dialogfragment");}
    private static boolean otherContent(String name){return TikTokScreen.contains(name,
        "profilefragment","profile_fragment","profile_view","searchfragment","search_fragment",
        "settingsfragment","detailfragment","storyviewer","story_viewer","reelviewer","reel_viewer",
        "directinbox","directthread","clips_camera","clips_editor","editprofile","profileedit");}
    private static boolean otherView(String id){return Arrays.asList("drawer_content","navigation_drawer",
        "story_viewer","reel_viewer","search_edit_text","clips_camera_container",
        "clips_editor_container","profile_edit_container","settings_container").contains(id);}
    private static boolean namedLayer(String id){return Arrays.asList("bottom_sheet","design_bottom_sheet",
        "comment_sheet","comments_sheet","share_sheet","comment_edit_text").contains(id);}
    private static boolean heading(String s){return s.equals("コメント")||s.equals("comments")||s.matches("(?:コメント|comments)[ :：\\(（]*[0-9０-９,.万千kKmM]+[\\)）]?");}
    private static void scan(View root,Snapshot s) {
        scan(root,s,Collections.<View>emptySet());
    }
    private static void scan(View root,Snapshot s,Set<View> covered) {
        Rect screen=new Rect();if(!TikTokScreen.shown(root,screen)){s.unknown=true;return;}
        Set<View> closes=Collections.newSetFromMap(new IdentityHashMap<View,Boolean>());
        ArrayDeque<View> q=new ArrayDeque<View>();q.add(root);int count=0;
        while(!q.isEmpty()) {
            if(++count>1800){s.unknown=true;break;}
            View v=q.removeFirst();Rect r=new Rect();if(covered.contains(v)||!shown(v,r))continue;
            String id=TikTokScreen.id(v),name=v.getClass().getName().toLowerCase(Locale.ROOT);
            String label=TikTokScreen.text(v.getContentDescription()),text=v instanceof TextView?TikTokScreen.text(((TextView)v).getText()):"";
            boolean wide=r.width()>screen.width()*.35f,large=wide&&r.height()>screen.height()*.12f;
            // Keep scanning children of the footer: a real editor or opened panel
            // still vetoes exit even when its ancestor contains the harmless CTA.
            if(!inlineCommentPrompt(v,r,screen)&&((large&&(commentContainer(id)||commentContainer(name))&&!InstagramReels.wrapper(v,screen))
                    ||(wide&&commentContainer(id)&&TikTokScreen.contains(id,"composer","edit_text"))
                    ||(((wide&&!v.isClickable()&&r.height()<screen.height()*.16f&&(heading(text)||heading(label)))
                        ||(large&&heading(label)))&&!captionText(v,screen))))s.comments=true;
            if(large&&(modal(id)||modal(name))&&!InstagramReels.wrapper(v,screen))s.blocked=true;
            if(s.current!=null&&s.current.available){
                // Preserve the former ExitScreens exact-resource veto, including
                // a narrow editor or sheet that fails the generic size heuristic.
                if(namedLayer(id)&&!InstagramReels.wrapper(v,screen))s.blocked=true;
                if(otherView(id))s.otherContent=true;
            }
            if(BackNavigation.backControl(v,r,screen)) {
                View target=TikTokScreen.clickable(v,screen,false);if(target!=null)closes.add(target);
            }
            if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;if(group.getChildCount()>1800-count){s.unknown=true;break;}for(int i=0;i<group.getChildCount();i++)q.add(group.getChildAt(i));}
        }
        s.blocked|=s.comments;
        if(closes.size()==1)s.close=closes.iterator().next();
    }
    private static void fragments(Object manager,Snapshot s,Set<Object> seen,int depth){
        fragments(manager,s,seen,depth,Collections.<View>emptySet());
    }
    private static void fragments(Object manager,Snapshot s,Set<Object> seen,int depth,Set<View> covered){
        if(manager==null)return;if(depth>8){s.unknown=true;return;}if(!seen.add(manager))return;
        Object list=InstagramReels.fragments(manager);if(!(list instanceof Iterable<?>))return;
        for(Object f:(Iterable<?>)list){
            if(f==null||!Boolean.TRUE.equals(BackNavigation.invoke(f,"isVisible")))continue;
            Object fv=BackNavigation.invoke(f,"getView");View view=fv instanceof View?(View)fv:null;
            if(view!=null&&(covered.contains(view)||!shown(view,new Rect())))continue;
            String name=f.getClass().getName().toLowerCase(Locale.ROOT);Object module=BackNavigation.invoke(f,"getModuleName");
            if(module instanceof String)name+="/"+TikTokScreen.text((String)module);
            if(name.contains("comment")&&TikTokScreen.contains(name,"fragment","thread","sheet","panel","comments")){s.comments=true;s.blocked=true;}
            if(modal(name)&&!InstagramReels.route(f)){
                Rect screen=new Rect();
                if(view==null||!TikTokScreen.shown(view.getRootView(),screen)||!InstagramReels.wrapper(view,screen))s.blocked=true;
            }
            // The native selection identifies the content, but does not grant
            // permission to ignore a separate visible detail or child overlay.
            if(s.current!=null&&s.current.available&&!s.current.path.contains(f)
                    &&!InstagramReels.route(f)&&otherContent(name))s.otherContent=true;
            fragments(BackNavigation.invoke(f,"getChildFragmentManager"),s,seen,depth+1,covered);
        }
    }
    static int action(Activity a) {
        if(!InstagramExternalProfile.modalHost(a)&&Instagram452.deferReels(a))return 0;
        if(!owns(a)||Boolean.TRUE.equals(FORWARDING.get()))return 0;
        View root=a.getWindow()==null?null:a.getWindow().peekDecorView();
        if(!ready(root))return 0;
        Integer predicted=PREDICTIONS.get(a);
        if(predicted!=null&&predicted!=1)return predicted;
        if(guard(a)!=null)return 3;
        Snapshot s=inspect(a);
        if(s.blocked)return 3;
        if(s.unknown||s.otherContent)return 0;
        if(Integer.valueOf(1).equals(predicted)&&s.current!=null&&s.current.available){
            java.lang.ref.WeakReference<Object> initial=PREDICTION_CONTENT.get(a);
            if(initial==null||initial.get()!=s.current.target)return 0;
        }
        return ExitScreens.shouldExit(a,s)?1:0;
    }
    /** The exact bound AndroidX entry may run inside a platform/native wrapper.
     * Reuse the initial transaction identity instead of rejecting native depth.
     * Internal forwarding already returns action zero and cannot recurse. */
    static int boundAction(Activity a) {
        int result=action(a);
        // protect() has already preserved this native layer transaction. The
        // original dispatcher must close it; request() would see the existing
        // guard and do nothing, so reporting it consumed here would swallow Back.
        if(result==3&&NATIVE_ORIGIN.get()!=null)return 0;
        if(result!=1)return result;
        Origin origin=NATIVE_ORIGIN.get();
        if(origin!=null) {
            if(origin.activity!=a||!origin.exit)return 0;
            Snapshot current=inspect(a);
            if(current.current==null||current.current.target!=origin.target)return 0;
        }
        return result;
    }
    /** Conservative receiver classification for Main dispatch and prediction,
     * before the receiver's native popup/layer handlers have run. The exact
     * X.8Zv post-guard hook deliberately does not call this heuristic predicate. */
    static Activity nativeReelsTarget(Object fragment) {
        if(fragment==null||Boolean.TRUE.equals(FORWARDING.get())||Boolean.TRUE.equals(INTERNAL.get()))return null;
        Object owner=BackNavigation.invoke(fragment,"getActivity"),value=BackNavigation.invoke(fragment,"getView");
        if(!(owner instanceof Activity)||!(value instanceof View))return null;
        Activity a=(Activity)owner;View view=(View)value;
        if(!owns(a)||!Boolean.TRUE.equals(BackNavigation.invoke(fragment,"isVisible"))
                ||!HomeScreen.inputReady(a)||guard(a)!=null)return null;
        if(InstagramProfileBack.preserve(a,fragment))return null;
        if(!InstagramRootReels.matches(a,fragment))return null;
        Origin origin=NATIVE_ORIGIN.get();
        if(origin!=null&&(origin.activity!=a||!origin.reels||origin.target!=fragment))return null;
        Integer predicted=PREDICTIONS.get(a);
        if(Integer.valueOf(1).equals(predicted)){
            java.lang.ref.WeakReference<Object> initial=PREDICTION_CONTENT.get(a);
            if(initial!=null&&initial.get()!=fragment)return null;
        }
        if(predicted!=null&&predicted!=1){
            java.lang.ref.WeakReference<Object> initial=PREDICTION_CONTENT.get(a);
            if(predicted!=0||initial==null||initial.get()!=fragment)return null;
        }
        View root=a.getWindow()==null?null:a.getWindow().peekDecorView();Rect area=new Rect(),screen=new Rect();
        if(root==null||view.getRootView()!=root||!shown(view,area)||!shown(root,screen)
                ||!InstagramReels.large(area,screen)
                ||!(InstagramReels.route(fragment)||InstagramReels.wrapper(view,screen)))return null;
        // The broader Main/prediction callers still need to preserve the
        // initial Back identity when a detail or layer exposes Reels.
        Snapshot layer=new Snapshot();
        layer.current=new InstagramCurrent.Result(a);layer.current.available=true;
        layer.current.nativeRecognized=true;layer.current.stateClear=true;
        layer.current.kind=InstagramCurrent.REELS;layer.current.target=fragment;
        layer.current.view=view;layer.current.path.add(fragment);
        try {
            if(InstagramReelsExit.named(a.getClass(),"com.instagram.base.activity.IgFragmentActivity")!=null
                    &&InstagramActivityLayers.supports(a)&&!InstagramActivityLayers.clear(a))return null;
            scan(view,layer);
            fragments(BackNavigation.invoke(fragment,"getChildFragmentManager"),layer,
                Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()),0);
        }catch(RuntimeException|LinkageError failed){return null;}
        return layer.blocked||layer.unknown||layer.otherContent?null:a;
    }

    static void protect(Activity a){if(owns(a)){
        InstagramProfileBack.beginNative(a);InstagramExternalProfile.beginNative(a);
        Snapshot scene=inspect(a);
        // Capture the route before native dispatch. A detail pop can expose
        // Reels, but its later native hook still belongs to that detail Back.
        if(scene.current!=null&&scene.current.available)NATIVE_ORIGIN.set(new Origin(a,scene));
        if(guard(a)==null&&scene.blocked){Guard g=new Guard(a);g.done=true;GUARDS.put(a,g);}
    }}
    static void endNative(){NATIVE_ORIGIN.remove();InstagramProfileBack.endNative();InstagramExternalProfile.endNative();}
    static void freshKey(Activity a){if(owns(a)){Guard g=guard(a);if(g!=null&&g.done)GUARDS.remove(a);}}
    static void systemCommit(Activity a){if(owns(a)&&!PREDICTIONS.containsKey(a)){Guard g=guard(a);if(g!=null&&g.done&&SystemClock.uptimeMillis()-g.start>=DUPLICATE)GUARDS.remove(a);}}
    static void predictionStart(Activity a){if(owns(a)){freshKey(a);PREDICTIONS.remove(a);PREDICTION_CONTENT.remove(a);
        InstagramProfileBack.predictionStart(a);InstagramExternalProfile.predictionStart(a);
        Snapshot scene=inspect(a);
        if(scene.current!=null&&scene.current.available){
            if(ExitScreens.shouldExit(a,scene))PREDICTION_CONTENT.put(a,new java.lang.ref.WeakReference<Object>(scene.current.target));
        }else{
            Object initial=InstagramReels.foregroundFragment(a);
            if(initial!=null&&initial!=Boolean.FALSE&&nativeReelsTarget(initial)!=null)PREDICTION_CONTENT.put(a,new java.lang.ref.WeakReference<Object>(initial));
        }
        PREDICTIONS.put(a,action(a));}}
    static boolean exitPrediction(Activity a){
        if(InstagramExternalProfile.modalHost(a)||Instagram452.deferReels(a))return false;
        if(!owns(a))return false;Integer predicted=PREDICTIONS.get(a);
        if(Integer.valueOf(1).equals(predicted))return true;
        java.lang.ref.WeakReference<Object> initial=PREDICTION_CONTENT.get(a);
        return Integer.valueOf(0).equals(predicted)&&initial!=null&&initial.get()!=null;
    }
    static boolean forwarding(){return Boolean.TRUE.equals(FORWARDING.get())||Boolean.TRUE.equals(INTERNAL.get());}
    static void predictionEnd(Activity a){PREDICTIONS.remove(a);PREDICTION_CONTENT.remove(a);InstagramProfileBack.predictionEnd(a);InstagramExternalProfile.predictionEnd(a);InstagramDispatcherBack.predictionEnd(a);}
    static void cancel(Activity a){GUARDS.remove(a);predictionEnd(a);}
    static void interaction(Activity a){if(!Boolean.TRUE.equals(INTERNAL.get())&&!Boolean.TRUE.equals(FORWARDING.get()))cancel(a);}
    static void request(Activity a) {
        if(!owns(a)||guard(a)!=null)return;
        final Guard g=new Guard(a);GUARDS.put(a,g);
        Snapshot before=inspect(a);
        if(!before.blocked){g.done=true;return;} // Started with comments; they already collapsed during this gesture.
        try {
            boolean clicked=false;
            // A semantic close control belonging to this layer uses the app's own listener.
            // Prefer native Back when a reel toolbar could be behind the layer.
            if(before.close!=null&&layerControl(before.close)){
                INTERNAL.set(Boolean.TRUE);try{clicked=BackNavigation.click(before.close);}finally{INTERNAL.remove();}
            }
            if(!clicked){FORWARDING.set(Boolean.TRUE);try{OneBackExit.tiktokNative(a);}finally{FORWARDING.remove();}}
            android.util.Log.d("HiroBack118",inspect(a).blocked?"Instagram layer native Back forwarded; task exit suppressed":"Instagram layer collapsed; same Back cannot exit");
        }finally{g.done=true;}
    }
    private static boolean layerControl(View view){
        // A generic top-level Reels back arrow is not a comments dismiss control.
        for(int n=0;n<5&&view!=null;n++){
            String id=TikTokScreen.id(view),name=view.getClass().getName().toLowerCase(Locale.ROOT);
            if(commentContainer(id)||commentContainer(name)||modal(id)||modal(name)||Arrays.asList("sheet_close","comments_close","comment_close_button").contains(id))return true;
            view=view.getParent() instanceof View?(View)view.getParent():null;
        }
        return false;
    }
    static boolean terminal(Activity a,int kind){
        if(!owns(a)||guard(a)==null)return false;
        // Native fragments may close the layer, but no Activity/task terminal operation is allowed.
        if(kind==0&&BackNavigation.popKnownStack(a))return true;
        android.util.Log.d("HiroBack118","Instagram task exit blocked for layer Back");return true;
    }
    static boolean terminalTask(ActivityManager.AppTask task){
        if(task==null)return false;
        try {
            ActivityManager.RecentTaskInfo info=task.getTaskInfo();if(info==null)return false;
            int id=Build.VERSION.SDK_INT>=29?info.taskId:info.id;
            String pkg=info.baseActivity==null?null:info.baseActivity.getPackageName();
            if(pkg==null&&info.baseIntent!=null&&info.baseIntent.getComponent()!=null)pkg=info.baseIntent.getComponent().getPackageName();
            for(Activity a:new ArrayList<Activity>(GUARDS.keySet())){Guard g=a==null?null:guard(a);if(g!=null&&g.task==id&&g.pkg.equals(pkg))return true;}
        }catch(RuntimeException|LinkageError ignored){}return false;
    }
    static boolean dialog(Dialog d,Activity a){
        if(d==null||!owns(a)||Boolean.TRUE.equals(FORWARDING.get()))return false;
        View root=d.getWindow()==null?null:d.getWindow().peekDecorView();
        Snapshot s=window(root);if(!s.comments||!ready(root))return false;
        Guard existing=guard(a);if(existing!=null&&existing.done&&SystemClock.uptimeMillis()-existing.start<DUPLICATE)return true;
        Guard g=new Guard(a);GUARDS.put(a,g);
        FORWARDING.set(Boolean.TRUE);OneBackExit.enterNative();
        try{d.onBackPressed();return true;}finally{OneBackExit.leaveNative();FORWARDING.remove();g.done=true;}
    }
}
