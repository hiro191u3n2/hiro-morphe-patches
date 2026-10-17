package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.*;

/** Navigation transactions never acquire exit permission, even after arriving at Home. */
final class TikTokBack {
    private TikTokBack() {}
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Map<Activity,Session> SESSIONS=new WeakHashMap<Activity,Session>();
    private static final Map<Activity,Integer> PREDICTIONS=new WeakHashMap<Activity,Integer>();
    private static final ThreadLocal<Boolean> FORWARDING=new ThreadLocal<Boolean>();
    private static final ThreadLocal<Boolean> INTERNAL=new ThreadLocal<Boolean>();
    private static final long WINDOW_MS=1600, DUPLICATE_MS=350;
    private static final int MAX_STEPS=10;
    private static final class Session {
        final WeakReference<Activity> owner;
        final int task;final String pkg;final long deadline;
        long duplicateUntil,navigationStamp;boolean stampKnown;int attempts,nativeHomeWaits;boolean queued,done,forwarded,clicked,closeClicked,cancelled,awaitFocus,stackPopped,nativeHomeRequested;
        Session(Activity a){owner=new WeakReference<Activity>(a);task=a.getTaskId();pkg=a.getPackageName();deadline=SystemClock.uptimeMillis()+WINDOW_MS;duplicateUntil=SystemClock.uptimeMillis()+DUPLICATE_MS;}
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
        Integer captured=PREDICTIONS.get(a);
        if(captured!=null)return captured==0?0:(captured==1&&TikTokScreen.inspect(a).home?1:2);
        Session s=session(a);
        if(s!=null)return 2;
        return TikTokScreen.inspect(a).home?1:2;
    }
    static void systemCommit(Activity a) {
        if(!TikTokScreen.owns(a)||PREDICTIONS.containsKey(a))return;
        Session s=session(a);
        if(s!=null&&s.done&&SystemClock.uptimeMillis()>=s.duplicateUntil&&TikTokScreen.inspect(a).home)SESSIONS.remove(a);
    }
    static void freshKey(Activity a) {
        if(!TikTokScreen.owns(a))return;
        Session s=session(a);
        if(s!=null&&s.done&&TikTokScreen.inspect(a).home)SESSIONS.remove(a);
    }
    static void predictionStart(Activity a) {
        if(!TikTokScreen.owns(a))return;
        freshKey(a);PREDICTIONS.remove(a);PREDICTIONS.put(a,action(a));
    }
    static void predictionEnd(Activity a){if(a!=null)PREDICTIONS.remove(a);}
    static void protect(Activity a) {
        if(!TikTokScreen.owns(a)||a.isDestroyed()||a.isFinishing())return;
        if(session(a)==null){Session s=new Session(a);s.done=true;SESSIONS.put(a,s);}
    }
    static void cancel(Activity a) {
        if(!TikTokScreen.owns(a))return;
        Session s=SESSIONS.remove(a);if(s!=null)s.cancelled=true;PREDICTIONS.remove(a);
    }
    static void interaction(Activity a) {if(!forwarding()&&!Boolean.TRUE.equals(INTERNAL.get()))cancel(a);}
    static void request(Activity a) {
        if(!TikTokScreen.owns(a)||!focused(a))return;
        Session old=session(a);
        if(old!=null&&(!old.done||SystemClock.uptimeMillis()<old.duplicateUntil))return;
        Session s=new Session(a);SESSIONS.put(a,s);advance(s);
    }
    private static void later(final Session s) {
        if(s.queued||s.done||s.cancelled)return;
        if(s.attempts>=MAX_STEPS){finish(s,"navigation attempt limit; exit suppressed");return;}
        s.queued=true;MAIN.postDelayed(new Runnable(){public void run(){s.queued=false;advance(s);}},80);
    }
    private static boolean click(View v) {
        if(v==null||!v.isShown()||!v.isEnabled()||!v.isClickable())return false;
        INTERNAL.set(Boolean.TRUE);try{return v.performClick();}catch(RuntimeException|LinkageError ignored){return false;}finally{INTERNAL.remove();}
    }
    private static void advance(Session s) {
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
        Session s=session(a);if(s!=null&&!s.done)later(s);
    }
    static boolean terminal(Activity a,int kind) {
        if(!TikTokScreen.owns(a))return false;
        Session s=session(a);
        if(s==null)return false;
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
                if(a!=null&&s.task==id&&s.pkg.equals(pkg)&&session(a)==s)return true;
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
            final WeakReference<Activity> ref=new WeakReference<Activity>(owner);
            MAIN.post(new Runnable(){public void run(){Activity a=ref.get();if(a!=null&&focused(a)&&HomeScreen.inputReady(a)){
                Session old=SESSIONS.remove(a);if(old!=null)old.cancelled=true;request(a);
            }}});
            return true;
        }catch(RuntimeException|LinkageError ignored){return false;}
    }
}
