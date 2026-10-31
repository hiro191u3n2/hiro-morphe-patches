package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.*;

/** Navigation transactions never acquire exit permission, even after arriving at Home. */
final class TikTokBack {
    private TikTokBack() {}
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Map<Activity,Session> SESSIONS=new WeakHashMap<Activity,Session>();
    private static final Map<Activity,Integer> PREDICTIONS=new WeakHashMap<Activity,Integer>();
    private static final Map<Activity,Boolean> NATIVE_STARTS=new WeakHashMap<Activity,Boolean>();
    private static final ThreadLocal<Boolean> FORWARDING=new ThreadLocal<Boolean>();
    private static final ThreadLocal<Boolean> INTERNAL=new ThreadLocal<Boolean>();
    private static final ThreadLocal<WeakReference<Activity>> KEY_EVENT=new ThreadLocal<WeakReference<Activity>>();
    private static final long WINDOW_MS=1600, DUPLICATE_MS=350;
    private static final int MAX_STEPS=10;
    private static final class Session {
        final WeakReference<Activity> owner;
        final int task;final String pkg;final long deadline;
        final boolean native4743,startedHome;
        final WeakReference<Object> nativeAbility;
        boolean mainDispatched,rootChecked,nativeKey,keyReleased,keyCancelled;
        long duplicateUntil,navigationStamp;boolean stampKnown;int attempts,nativeHomeWaits;boolean queued,done,forwarded,clicked,closeClicked,cancelled,awaitFocus,stackPopped,nativeHomeRequested;
        Session(Activity a){
            owner=new WeakReference<Activity>(a);task=a.getTaskId();pkg=a.getPackageName();
            deadline=SystemClock.uptimeMillis()+WINDOW_MS;duplicateUntil=SystemClock.uptimeMillis()+DUPLICATE_MS;
            native4743=TikTokHome.supportsNativeBack(a);
            TikTokHome.Route route=native4743?TikTokHome.route(a):null;
            Boolean captured=NATIVE_STARTS.remove(a);
            startedHome=route!=null&&route.native4743&&route.home()&&TikTokHome.outerFeed(a)
                &&(captured==null||captured.booleanValue());
            nativeAbility=new WeakReference<Object>(route==null?null:route.ability);
        }
    }
    static boolean forwarding(){return Boolean.TRUE.equals(FORWARDING.get());}
    static boolean focused(Activity a) {
        if(!TikTokScreen.owns(a)||a.isDestroyed()||a.isFinishing())return false;
        try{View v=a.getWindow()==null?null:a.getWindow().peekDecorView();return v!=null&&v.isAttachedToWindow()&&v.hasWindowFocus()&&v.isShown();}
        catch(RuntimeException ignored){return false;}
    }
    private static Session session(Activity a) {
        Session s=SESSIONS.get(a);
        if(s!=null&&(s.cancelled||s.deadline<SystemClock.uptimeMillis()||a.isDestroyed()||s.task!=a.getTaskId())){SESSIONS.remove(a);return null;}
        return s;
    }
    static int action(Activity a) {
        if(!TikTokScreen.owns(a)||!HomeScreen.inputReady(a))return 0;
        if(forwarding())return 0;
        // In 47.4.3 the native Back guards decide what is in front. Feed labels,
        // clipped/retained fragments and user-hidden tabs cannot grant or deny exit.
        Integer captured=PREDICTIONS.get(a);
        if(TikTokHome.supportsNativeBack(a))return captured!=null&&captured==0?0:2;
        if(captured!=null)return captured==0?0:(captured==1&&TikTokScreen.inspect(a).home?1:2);
        Session s=session(a);
        if(s!=null)return 2;
        return TikTokScreen.inspect(a).home?1:2;
    }
    static void systemCommit(Activity a) {
        if(!TikTokScreen.owns(a)||PREDICTIONS.containsKey(a))return;
        Session s=session(a);
        if(s!=null&&s.done&&SystemClock.uptimeMillis()>=s.duplicateUntil&&(s.native4743||TikTokScreen.inspect(a).home))SESSIONS.remove(a);
    }
    static void freshKey(Activity a) {
        if(!TikTokScreen.owns(a))return;
        Session s=session(a);
        if(s!=null&&s.done&&(s.native4743||TikTokScreen.inspect(a).home))SESSIONS.remove(a);
        if(TikTokHome.supportsNativeBack(a)) {
            TikTokHome.Route route=TikTokHome.route(a);
            NATIVE_STARTS.put(a,route!=null&&route.home()&&TikTokHome.outerFeed(a)&&HomeScreen.inputReady(a));
        } else NATIVE_STARTS.remove(a);
    }
    static void predictionStart(Activity a) {
        if(!TikTokScreen.owns(a))return;
        freshKey(a);PREDICTIONS.remove(a);PREDICTIONS.put(a,action(a));
    }
    static void predictionEnd(Activity a){if(a!=null){PREDICTIONS.remove(a);NATIVE_STARTS.remove(a);}}
    static void protect(Activity a) {
        if(!TikTokScreen.owns(a)||a.isDestroyed()||a.isFinishing())return;
        if(session(a)==null){Session s=new Session(a);s.done=true;SESSIONS.put(a,s);}
    }
    static void cancel(Activity a) {
        if(!TikTokScreen.owns(a))return;
        Session s=SESSIONS.remove(a);if(s!=null)s.cancelled=true;PREDICTIONS.remove(a);NATIVE_STARTS.remove(a);
    }
    static void interaction(Activity a) {
        WeakReference<Activity> key=KEY_EVENT.get();
        if(!forwarding()&&!Boolean.TRUE.equals(INTERNAL.get())&&(key==null||key.get()!=a))cancel(a);
    }
    /** Preserve the native KeyDown/KeyUp chain. TikTok handles outer profile,
     * camera and discovery pages on KeyDown before onBackPressed is called. */
    static void nativeKey(Activity a,KeyEvent event) {
        if(!TikTokHome.supportsNativeBack(a)||event==null)return;
        if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0) {
            freshKey(a);
            Session s=new Session(a);s.nativeKey=true;SESSIONS.put(a,s);
            KEY_EVENT.set(new WeakReference<Activity>(a));
        } else {
            Session s=session(a);
            if(s!=null&&s.nativeKey&&!s.done) {
                if(event.getAction()==KeyEvent.ACTION_UP){s.keyReleased=true;s.keyCancelled=event.isCanceled();}
                KEY_EVENT.set(new WeakReference<Activity>(a));
            }
        }
    }
    /** Called by the outermost exception-safe native wrapper's finally block. */
    static void endNativeKeyEvent() {
        WeakReference<Activity> key=KEY_EVENT.get();KEY_EVENT.remove();
        Activity a=key==null?null:key.get();Session s=a==null?null:session(a);
        if(s!=null&&s.nativeKey&&s.keyReleased)finish(s,"47.4.3 physical Back completed; native KeyDown retained");
    }
    static void request(Activity a) {
        if(!TikTokScreen.owns(a)||!focused(a))return;
        Session old=session(a);
        if(old!=null&&(!old.done||SystemClock.uptimeMillis()<old.duplicateUntil))return;
        Session s=new Session(a);SESSIONS.put(a,s);
        if(s.native4743) {
            // One physical Back performs one native action. A consumed comment,
            // tutorial, dialog, child or profile pop does not enqueue another Back.
            s.forwarded=true;
            try{forward(a);}finally{if(session(a)==s)finish(s,"47.4.3 native Back completed; no automatic follow-up");}
        } else advance(s);
    }
    /** Mark the exact patched SAFTikTokActivity entry; a dispatcher fallback can
     * route into it at most once even if native code returns to AndroidX again. */
    static void enterMain(Activity a) {
        Session s=session(a);if(s!=null&&s.native4743&&TikTokScreen.main(a))s.mainDispatched=true;
    }
    static boolean dispatchMain(Activity a) {
        Session s=session(a);
        if(s==null||!s.native4743||s.done||s.mainDispatched||!TikTokScreen.main(a))return false;
        s.mainDispatched=true;
        a.onBackPressed();return true;
    }
    /** Native interceptor + comment guards have declined this Back. Skip only
     * the app's two-Back timer; the caller's guide/profile guards must still run. */
    static boolean skipDoubleBack(Activity a) {
        Session s=session(a);
        if(s==null||!s.native4743||s.done||s.keyCancelled||!TikTokScreen.main(a)||!focused(a))return false;
        s.mainDispatched=true;
        return true;
    }
    /** Called only AFTER native interceptor, comment and tutorial guards.
     * 0=keep native tab handling, 1=exit confirmed root, 2=outer-page Back consumed. */
    static int nativeRootAction(Activity a) {
        Session s=session(a);
        if(s==null||!s.native4743||s.done||s.keyCancelled||s.rootChecked
                ||!TikTokScreen.main(a)||!focused(a)||!HomeScreen.inputReady(a))return 0;
        s.rootChecked=true;
        // Physical KeyDown already offered the Back to the outer-page guards.
        // Repeating them on KeyUp could consume a second layer in one gesture.
        if(!TikTokHome.outerFeed(a))return !s.nativeKey&&TikTokHome.closeOuterPage(a)?2:0;
        if(!s.startedHome)return 0;
        TikTokHome.Route route=TikTokHome.route(a);
        return route!=null&&route.native4743&&route.home()&&route.ability==s.nativeAbility.get()?1:0;
    }
    private static void later(final Session s) {
        if(s.native4743||s.queued||s.done||s.cancelled)return;
        if(s.attempts>=MAX_STEPS){finish(s,"navigation attempt limit; exit suppressed");return;}
        s.queued=true;MAIN.postDelayed(new Runnable(){public void run(){s.queued=false;advance(s);}},80);
    }
    private static boolean click(View v) {
        if(v==null||!v.isShown()||!v.isEnabled()||!v.isClickable())return false;
        INTERNAL.set(Boolean.TRUE);try{return v.performClick();}catch(RuntimeException|LinkageError ignored){return false;}finally{INTERNAL.remove();}
    }
    private static void advance(Session s) {
        if(s.native4743)return;
        Activity a=s.owner.get();if(a==null||session(a)!=s||s.cancelled||s.done)return;
        if(!focused(a)&&s.awaitFocus){if(++s.attempts<MAX_STEPS)later(s);else finish(s,"main focus unavailable; exit suppressed");return;}
        if(!focused(a)||!HomeScreen.inputReady(a)){finish(s,"navigation paused for input/window");return;}
        s.awaitFocus=false;
        if(++s.attempts>MAX_STEPS){finish(s,"navigation attempt limit; exit suppressed");return;}
        TikTokScreen.Snapshot page=TikTokScreen.inspect(a);
        if(page.home){finish(s,"Home confirmed; current Back cannot exit");return;}
        // Respect input/composition: never force-discard an editor or confirmation dialog.
        if(page.editor){finish(s,"editor protected; exit suppressed");return;}
        // Unfocused edit/profile/composition forms still own their native Back behavior
        // (including unsaved-change confirmation). Never jump tabs through them.
        if(page.nativeOnly){if(!s.forwarded){s.forwarded=true;forward(a);}finish(s,"native form navigation retained; exit suppressed");return;}
        // Hox selection dispatches app lifecycle listeners. Give its page transition
        // a bounded four polls before considering native Back on the old view tree.
        if(s.nativeHomeRequested&&s.nativeHomeWaits>0){s.nativeHomeWaits--;later(s);return;}
        // One-shot flags belong to a foreground page, not to the entire trip Home.
        // A profile -> profile -> feed stack needs a fresh native pop at each page.
        if(s.stampKnown&&s.navigationStamp!=page.navigationStamp){
            s.closeClicked=false;s.forwarded=false;s.stackPopped=false;s.clicked=false;
        }
        s.navigationStamp=page.navigationStamp;s.stampKnown=true;
        boolean changed=false;
        // A profile/root tab can still have a toolbar Back control. That control
        // must not permanently exclude the real bottom Home tab (the old else-if).
        // An actual comment/dialog/sheet remains higher priority than any tab.
        if(!page.layer&&!s.clicked&&page.homeTarget!=null
                &&(!page.unwind||page.otherSelected||page.profile)){
            s.clicked=true;changed=click(page.homeTarget);
        }
        // The shipped app can have obfuscated/unlabelled bottom tabs. Its audited
        // HomeTabAbility routes through Hox, so a missing/no-op UI target must not
        // turn this Back into a swallowed native fallback. One request per gesture.
        if(!changed&&!page.layer&&!page.unknown&&!s.nativeHomeRequested&&page.nativeRoute!=null
                &&(page.profile||page.otherSelected)) {
            s.nativeHomeRequested=true;
            INTERNAL.set(Boolean.TRUE);
            try{changed=TikTokHome.select(page.nativeRoute);if(changed)s.nativeHomeWaits=4;}finally{INTERNAL.remove();}
        }
        if(!changed&&page.unwind) {
            if(!s.closeClicked&&page.closeTarget!=null){s.closeClicked=true;changed=click(page.closeTarget);}
            if(!changed&&!s.forwarded){s.forwarded=true;forward(a);changed=true;}
            if(!changed&&!s.stackPopped&&!TikTokScreen.inspect(a).home){
                s.stackPopped=true;changed=BackNavigation.popKnownStack(a);
            }
            // A no-op toolbar/native Back must not swallow all subsequent work.
            // Only use the real, still-visible Home target after layers have gone.
            if(!changed&&!page.layer&&!s.clicked&&page.homeTarget!=null){
                s.clicked=true;changed=click(page.homeTarget);
            }
        }
        if(!changed&&!TikTokScreen.main(a)) {
            Activity main=OneBackExit.tiktokMain(a);
            if(main!=null&&main!=a) {
                try {
                    Session target=new Session(main);target.awaitFocus=true;SESSIONS.put(main,target);
                    a.startActivity(new Intent(a,main.getClass()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    later(target);finish(s,"known same-task main requested; exit suppressed");return;
                }catch(RuntimeException|LinkageError ignored){/* Do not restart an unknown launcher or another task. */}
            }
        }
        if(!changed&&!s.forwarded){s.forwarded=true;forward(a);changed=true;}
        if(!changed&&!s.stackPopped&&!TikTokScreen.inspect(a).home){
            s.stackPopped=true;changed=BackNavigation.popKnownStack(a);
        }
        if(a.isFinishing()||a.isDestroyed()){
            Activity main=OneBackExit.tiktokMain(a);if(main!=null&&main!=a){Session target=new Session(main);target.awaitFocus=true;SESSIONS.put(main,target);later(target);}
            finish(s,"child Activity closed; task exit suppressed");return;
        }
        // Click acknowledgement alone does not prove a transition. Re-read foreground evidence.
        if(TikTokScreen.inspect(a).home){finish(s,"Home confirmed; current Back cannot exit");return;}
        if(changed||s.attempts<MAX_STEPS)later(s);else finish(s,"Home not confirmed; exit suppressed");
    }
    private static void finish(Session s,String reason){s.done=true;android.util.Log.d("HiroBack117",reason);}
    private static void forward(Activity a) {
        FORWARDING.set(Boolean.TRUE);
        try{OneBackExit.tiktokNative(a);}finally{FORWARDING.remove();}
    }
    static void resumed(Activity a) {
        Session s=session(a);if(s!=null&&!s.done&&!s.native4743)later(s);
    }
    static boolean terminal(Activity a,int kind) {
        if(!TikTokScreen.owns(a))return false;
        Session s=session(a);
        if(s==null)return false;
        if(s.native4743) {
            // AndroidX's fallback invokes Activity.super directly, bypassing
            // SAFTikTokActivity. Re-enter that verified native chain once.
            if(kind==0&&dispatchMain(a))return true;
            // A child Activity's ordinary finish/pop is navigation, including a
            // settings screen opened as the first Activity of a separate task.
            if(!TikTokScreen.main(a))return false;
            android.util.Log.d("HiroBack4743","unapproved native task exit blocked during one Back");
            return true;
        }
        // A child finish is normal navigation; affinity/removal/background affect the whole task.
        if((kind==0||kind==1)&&!a.isTaskRoot())return false;
        if(kind==0) {
            // AndroidX/child fragment stacks are not Android framework fragment entries.
            // Consume one known stack pop before blocking the platform task-root fallback.
            try{if(a.getFragmentManager()!=null&&a.getFragmentManager().getBackStackEntryCount()>0)return false;

            }catch(RuntimeException ignored){}
            if(BackNavigation.popKnownStack(a))return true;
        }
        android.util.Log.d("HiroBack117","native task exit blocked during protected Back");return true;
    }
    static boolean terminalTask(ActivityManager.AppTask task) {
        if(task==null)return false;
        try {
            ActivityManager.RecentTaskInfo info=task.getTaskInfo();if(info==null)return false;
            int id=android.os.Build.VERSION.SDK_INT>=29?info.taskId:info.id;
            String pkg=info.baseActivity==null?null:info.baseActivity.getPackageName();
            if(pkg==null&&info.baseIntent!=null&&info.baseIntent.getComponent()!=null)pkg=info.baseIntent.getComponent().getPackageName();
            for(Map.Entry<Activity,Session> e:new ArrayList<Map.Entry<Activity,Session>>(SESSIONS.entrySet())) {
                Activity a=e.getKey();Session s=e.getValue();
                if(a!=null&&s.task==id&&s.pkg.equals(pkg)&&session(a)==s&&(!s.native4743||TikTokScreen.main(a)))return true;
            }
        }catch(RuntimeException|LinkageError ignored){}
        return false;
    }
    static boolean dialog(Dialog d,Activity owner) {
        if(d==null||!TikTokScreen.owns(owner)||forwarding())return false;
        protect(owner);
        try {
            View root=d.getWindow()==null?null:d.getWindow().peekDecorView();
            TikTokScreen.Snapshot page=TikTokScreen.window(root);
            if(!page.comment||page.editor)return false;
            // Execute the dialog's real close behavior, not a forced dismiss that discards input.
            FORWARDING.set(Boolean.TRUE);OneBackExit.enterNative();
            try{d.onBackPressed();}finally{OneBackExit.leaveNative();FORWARDING.remove();}
            // A 47.4.3 comment dialog owns this Back. Do not route its underlying
            // profile/Home after the real dialog has already consumed the action.
            if(TikTokHome.supportsNativeBack(owner))return true;
            final WeakReference<Activity> ref=new WeakReference<Activity>(owner);
            MAIN.post(new Runnable(){public void run(){Activity a=ref.get();if(a!=null&&focused(a)&&HomeScreen.inputReady(a)){
                Session old=SESSIONS.remove(a);if(old!=null)old.cancelled=true;request(a);
            }}});
            return true;
        }catch(RuntimeException|LinkageError ignored){return false;}
    }
}
