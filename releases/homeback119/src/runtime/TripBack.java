package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Bounded, successful-BACK-only observation. HOME, user input, new Activity and config changes cancel it. */
final class TripBack {
    private TripBack() {}
    private static final long WINDOW_MS=1500;
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Map<Activity,Ticket> PENDING=new WeakHashMap<Activity,Ticket>();
    private static final Map<Application,Long> EPOCHS=new WeakHashMap<Application,Long>();
    private static final ThreadLocal<Attempt> ATTEMPT=new ThreadLocal<Attempt>();
    private static long epoch(Application app){Long value=EPOCHS.get(app);return value==null?0:value;}
    private static boolean trip(Activity activity){return activity!=null&&"ctrip.english".equals(activity.getPackageName());}
    private static final class Ticket {
        final int task;final long until,epoch;
        Ticket(Activity activity,long generation){task=activity.getTaskId();until=SystemClock.uptimeMillis()+WINDOW_MS;epoch=generation;}
    }
    private static final class Attempt {
        final WeakReference<Activity> activity;final long epoch;final boolean eligible;
        Attempt(Activity a){activity=new WeakReference<Activity>(a);epoch=TripBack.epoch(a.getApplication());
            eligible=observe(a)&&a.isTaskRoot()&&!a.isChangingConfigurations();}
    }
    private static boolean interactive(Activity activity) {
        try {
            Object power=activity.getSystemService(Context.POWER_SERVICE),keyguard=activity.getSystemService(Context.KEYGUARD_SERVICE);
            return (!(power instanceof android.os.PowerManager)||((android.os.PowerManager)power).isInteractive())
                &&(!(keyguard instanceof android.app.KeyguardManager)||!((android.app.KeyguardManager)keyguard).isKeyguardLocked());
        }catch(RuntimeException ignored){return false;}
    }
    static boolean observe(Activity activity){return trip(activity)&&interactive(activity)&&HomeScreen.inputReady(activity);}
    static void begin(Activity activity){
        if(trip(activity)&&Looper.myLooper()==Looper.getMainLooper())ATTEMPT.set(new Attempt(activity));
    }
    static void commit(){
        Attempt attempt=ATTEMPT.get();if(attempt==null||!attempt.eligible)return;
        Activity activity=attempt.activity.get();
        if(activity==null||activity.isDestroyed()||attempt.epoch!=epoch(activity.getApplication()))return;
        PENDING.put(activity,new Ticket(activity,attempt.epoch));
        android.util.Log.d("HiroBack113","Trip native Back returned; terminal observation armed");
    }
    static void end(){ATTEMPT.remove();}
    static boolean pending(Activity activity){
        if(!trip(activity)||Looper.myLooper()!=Looper.getMainLooper())return false;
        Ticket ticket=PENDING.get(activity);
        if(ticket==null)return false;
        if(SystemClock.uptimeMillis()>ticket.until||ticket.epoch!=epoch(activity.getApplication())
            ||ticket.task!=activity.getTaskId()||activity.isChangingConfigurations()||!interactive(activity)) {
            PENDING.remove(activity);return false;
        }
        return true;
    }
    static void cancel(Activity activity){
        if(!trip(activity)||Looper.myLooper()!=Looper.getMainLooper())return;
        Application app=activity.getApplication();EPOCHS.put(app,epoch(app)+1);PENDING.remove(activity);
    }
    static void resumed(Activity activity){cancel(activity);}
    static void consume(Activity activity){cancel(activity);}
    static void destroyed(Activity activity){if(trip(activity))PENDING.remove(activity);}
    static void stopped(Activity activity){
        if(!pending(activity))return;
        final WeakReference<Activity> ref=new WeakReference<Activity>(activity);
        final Ticket expected=PENDING.get(activity);
        MAIN.post(new Runnable(){@Override public void run(){
            Activity current=ref.get();
            if(current==null||current.isDestroyed()||!pending(current)||PENDING.get(current)!=expected
                ||!current.isTaskRoot()||OneBackExit.anyResumed(current.getPackageName()))return;
            android.util.Log.d("HiroBack113","Trip stopped after Back; removing retained task");
            consume(current);OneBackExit.terminalStopped(current);
        }});
    }
    static void retryTasks(Activity activity,Set<Integer> ids){
        if(!trip(activity)||ids.isEmpty())return;
        final Application app=activity.getApplication();final long generation=epoch(app);
        final Set<Integer> targets=new HashSet<Integer>(ids);
        for(long delay:new long[]{80,300,900})MAIN.postDelayed(new Runnable(){@Override public void run(){
            if(epoch(app)!=generation||OneBackExit.anyResumed(app.getPackageName()))return;
            try {
                Object service=app.getSystemService(Context.ACTIVITY_SERVICE);
                if(!(service instanceof ActivityManager))return;
                java.util.List<ActivityManager.AppTask> tasks=((ActivityManager)service).getAppTasks();
                if(tasks==null)return;
                for(ActivityManager.AppTask task:new ArrayList<ActivityManager.AppTask>(tasks)) {
                    try {
                        ActivityManager.RecentTaskInfo info=task.getTaskInfo();
                        if(info==null||!targets.contains(info.taskId))continue;
                        ComponentName base=info.baseActivity;
                        Intent intent=info.baseIntent;
                        if(base==null&&intent!=null)base=intent.getComponent();
                        // Never infer ownership from a saved ID alone: it may now be another task.
                        if(base==null||!app.getPackageName().equals(base.getPackageName()))continue;
                        try{task.setExcludeFromRecents(true);}catch(RuntimeException ignored){}
                        try{task.finishAndRemoveTask();}catch(RuntimeException ignored){}
                        android.util.Log.d("HiroBack113","Trip verified own task cleanup retry");
                    }catch(RuntimeException ignored){}
                }
            }catch(RuntimeException ignored){}
        }},delay);
    }
}
