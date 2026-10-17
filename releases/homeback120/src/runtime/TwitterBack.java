package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** X retains native page navigation; only an actual native task exit is upgraded.
 * A private one-shot marker restores Home on the next verified launcher launch.
 * No application preference, saved instance state, account, draft or link is erased.
 */
final class TwitterBack {
    private TwitterBack() {}
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final String PREFS="app.hiro.oneback.twitter",RESET="home_after_exit";
    private static final long BACK_MS=1500,LAUNCH_MS=10000;
    private static final Map<Activity,Boolean> INSTALLED=new WeakHashMap<Activity,Boolean>();
    private static final Map<Activity,Ticket> PENDING=new WeakHashMap<Activity,Ticket>();
    private static final Map<Application,Session> SESSIONS=new WeakHashMap<Application,Session>();
    private static final ThreadLocal<Attempt> ATTEMPT=new ThreadLocal<Attempt>();
    static boolean owns(Activity a){return a!=null&&"com.twitter.android".equals(a.getPackageName());}
    private static final class Session {long epoch,launcherUntil;int launcherTask;Restore restore;}
    private static Session session(Activity a){Session s=SESSIONS.get(a.getApplication());if(s==null){s=new Session();SESSIONS.put(a.getApplication(),s);}return s;}
    private static final class Attempt {
        final WeakReference<Activity> activity;final long epoch;final boolean eligible;
        Attempt(Activity a){activity=new WeakReference<Activity>(a);epoch=session(a).epoch;
            eligible=a.isTaskRoot()&&!a.isChangingConfigurations()&&interactive(a)&&HomeScreen.inputReady(a);}
    }
    private static final class Ticket {
        final long epoch,until;final int task;
        Ticket(Activity a,long e){epoch=e;until=SystemClock.uptimeMillis()+BACK_MS;task=a.getTaskId();}
    }
    static void begin(Activity a){if(owns(a)&&Looper.myLooper()==Looper.getMainLooper())ATTEMPT.set(new Attempt(a));}
    static void commit(){Attempt t=ATTEMPT.get();if(t==null||!t.eligible)return;Activity a=t.activity.get();
        if(a!=null&&!a.isDestroyed()&&!a.isFinishing()&&session(a).epoch==t.epoch)PENDING.put(a,new Ticket(a,t.epoch));}
    static void end(){ATTEMPT.remove();}
    static boolean pending(Activity a){
        if(!owns(a))return false;Ticket t=PENDING.get(a);
        if(t==null)return false;
        if(t.epoch!=session(a).epoch||SystemClock.uptimeMillis()>t.until||t.task!=a.getTaskId()||a.isChangingConfigurations()||!interactive(a)){
            PENDING.remove(a);return false;}
        return true;
    }
    /** Receives only an original terminal callsite while a successful BACK is active. */
    static boolean terminal(Activity a,int kind){
        if(!owns(a)||Looper.myLooper()!=Looper.getMainLooper()||kind<0||kind>4)return false;
        Attempt t=ATTEMPT.get();boolean active=t!=null&&t.activity.get()==a&&t.eligible&&t.epoch==session(a).epoch;
        if(!active&&!pending(a))return false;
        try {
            if(!a.isTaskRoot()||a.isDestroyed()||a.isFinishing()||a.isChangingConfigurations())return false;
            // A platform onBackPressed call can still close ActionBar/Fragment state.
            // Explicit finish/moveTaskToBack calls have already made the native exit decision.
            if(kind==0&&(a.getActionBar()!=null||backStack(a)))return false;
            OneBackExit.terminalStopped(a);return true;
        }catch(RuntimeException|LinkageError ignored){return false;}
    }
    static void stopped(final Activity a){
        if(!pending(a))return;final Ticket t=PENDING.get(a);final WeakReference<Activity> ref=new WeakReference<Activity>(a);
        MAIN.post(new Runnable(){@Override public void run(){Activity current=ref.get();
            if(current==null||current.isDestroyed()||!current.isTaskRoot()||!pending(current)||PENDING.get(current)!=t
                    ||OneBackExit.anyResumed(current.getPackageName()))return;
            OneBackExit.terminalStopped(current);
        }});
    }
    static void cancel(Activity a){if(!owns(a))return;Session s=session(a);s.epoch++;s.launcherUntil=0;s.restore=null;PENDING.remove(a);}
    static void interaction(Activity a){cancel(a);}
    static void destroyed(Activity a){if(!owns(a))return;PENDING.remove(a);INSTALLED.remove(a);
        Session s=session(a);if(s.restore!=null&&s.restore.activity.get()==a)s.restore=null;}
    static void prepareExit(Activity a){
        if(!owns(a))return;
        cancel(a);setReset(a,true);
    }
    private static boolean reset(Activity a){try{return a.getApplication().getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(RESET,false);}catch(RuntimeException ignored){return false;}}
    private static void setReset(Activity a,boolean value){try{a.getApplication().getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(RESET,value).commit();}catch(RuntimeException ignored){}}
    static void installed(Activity a){
        if(!owns(a)||INSTALLED.containsKey(a))return;INSTALLED.put(a,true);
        // A newly created Activity invalidates old task-cleanup tickets before resume.
        Session s=session(a);s.epoch++;s.restore=null;
        try{incoming(a,a.getIntent());stampHomeIntent(a,a.getIntent());}catch(RuntimeException ignored){}
    }
    static void newIntent(Activity a,Intent intent){if(owns(a)){
        // onNewIntent is an external navigation event, not a launcher trampoline.
        // Its actual argument takes precedence over Activity.getIntent's stale value.
        Session s=session(a);s.epoch++;s.restore=null;s.launcherUntil=0;
        incoming(a,intent);stampHomeIntent(a,intent);schedule(a);
    }}
    private static void incoming(Activity a,Intent intent){
        if(intent==null)return;
        Session s=session(a);
        // A link, notification or share overrides a retained launcher session.
        if(intent.getData()!=null||(!Intent.ACTION_MAIN.equals(intent.getAction())&&intent.getAction()!=null)){
            s.launcherUntil=0;s.restore=null;return;
        }
        if(Intent.ACTION_MAIN.equals(intent.getAction())&&intent.hasCategory(Intent.CATEGORY_LAUNCHER)&&reset(a)){
            s.epoch++;s.restore=null;s.launcherTask=a.getTaskId();s.launcherUntil=SystemClock.uptimeMillis()+LAUNCH_MS;
        }
    }
    private static void stampHomeIntent(Activity a,Intent intent){
        Session s=session(a);
        if(intent==null||!mainHost(a)||!reset(a)||s.launcherUntil<=SystemClock.uptimeMillis()
                ||s.launcherTask!=a.getTaskId()||intent.getData()!=null
                ||(intent.getAction()!=null&&!Intent.ACTION_MAIN.equals(intent.getAction())))return;
        String page=intent.getStringExtra("page");
        if(page!=null&&!page.isEmpty()&&!"twitter://timeline/home".equals(page)){
            // Even a launcher-like notification/shortcut can carry an explicit page.
            s.launcherUntil=0;s.restore=null;return;
        }
        // Verified in X v12.19.1: l1.I3 -> main.api.b.d reads `page` before
        // consulting database/LRU `last_tab_used/tag`. This selects the real native
        // Home route without deleting remembered app state or forging a deep link.
        intent.putExtra("page","twitter://timeline/home");
    }
    static void resumed(Activity a){if(!owns(a))return;PENDING.remove(a);installed(a);
        // Cancel removal retries even when Android reuses the same Activity after
        // a silent/asynchronous task-removal request. Recreate only an active
        // launcher restore, with the same original bounded deadline.
        Session s=session(a);s.epoch++;s.restore=null;schedule(a);
    }
    private static boolean mainHost(Activity a){for(Class<?> c=a.getClass();c!=null;c=c.getSuperclass()){
        String n=c.getName();if("com.twitter.android.HomeTimelineActivity".equals(n)||"com.twitter.app.main.MainActivity".equals(n))return true;}return false;}
    private static boolean interactive(Activity a){try{
        Object power=a.getSystemService(Context.POWER_SERVICE),keyguard=a.getSystemService(Context.KEYGUARD_SERVICE);
        return (!(power instanceof android.os.PowerManager)||((android.os.PowerManager)power).isInteractive())
            &&(!(keyguard instanceof android.app.KeyguardManager)||!((android.app.KeyguardManager)keyguard).isKeyguardLocked());
    }catch(RuntimeException e){return false;}}
    private static void schedule(Activity a){
        Session s=session(a);
        if(!mainHost(a)||!reset(a)||s.launcherUntil<=SystemClock.uptimeMillis()||s.launcherTask!=a.getTaskId())return;
        if(s.restore!=null&&s.restore.activity.get()==a)return;
        Restore r=new Restore(a,s.epoch);s.restore=r;MAIN.post(r);
    }
    private static final class Restore implements Runnable {
        final WeakReference<Activity> activity;final long epoch;int tries;boolean clicked;
        Restore(Activity a,long e){activity=new WeakReference<Activity>(a);epoch=e;}
        @Override public void run(){
            Activity a=activity.get();if(a==null)return;Session s=session(a);
            if(s.restore!=this||s.epoch!=epoch||s.launcherUntil<=SystemClock.uptimeMillis()
                    ||a.isDestroyed()||a.isFinishing()||++tries>60)return;
            if(HomeScreen.inputReady(a)&&!backStack(a)){
                if(HomeScreen.isHome(a)){setReset(a,false);s.restore=null;s.launcherUntil=0;return;}
                if(!clicked){View home=homeButton(a);if(home!=null){
                    try{clicked=home.performClick();}
                    catch(RuntimeException|LinkageError failed){s.restore=null;s.launcherUntil=0;return;}
                    if(HomeScreen.isHome(a)){setReset(a,false);s.restore=null;s.launcherUntil=0;return;}}}
            }
            MAIN.postDelayed(this,100);
        }
    }
    private static String norm(CharSequence v){return v==null?"":v.toString().trim().toLowerCase(Locale.ROOT);}
    private static boolean homeName(String s){return s.equals("home")||s.equals("ホーム")||s.equals("ホームタブ")||s.startsWith("home,")||s.startsWith("home tab")||s.startsWith("ホーム、")||s.startsWith("ホーム,")||s.startsWith("ホーム タブ");}
    private static boolean homeId(String s){return s.equals("tab_home")||s.equals("home_tab")||s.equals("home_tab_button")||s.equals("bottom_nav_home")||s.equals("bottom_navigation_home")||s.equals("navigation_home")||s.equals("tab_home_icon")||s.equals("home_tab_icon");}
    static boolean homeResource(View v){
        // v12.19.1 defines a generic `home` id. Accept it only inside the actual
        // bottom-navigation container, never an arbitrary page's home label.
        if(!"home".equals(id(v)))return false;
        for(int n=0;n<5&&v.getParent() instanceof View;n++){
            v=(View)v.getParent();if("bottom_navigation_bar".equals(id(v)))return true;
        }
        return false;
    }
    private static String id(View v){try{return v.getId()==View.NO_ID?"":v.getResources().getResourceEntryName(v.getId()).toLowerCase(Locale.ROOT);}catch(RuntimeException e){return "";}}
    private static boolean nav(Rect r,Rect b){return !r.isEmpty()&&r.centerY()>b.top+b.height()*.65f&&r.height()<b.height()*.22f&&r.width()<b.width()*.45f;}
    private static boolean blocked(String s){return s.equals("bottom_sheet")||s.equals("design_bottom_sheet")||s.equals("drawer_content")||s.equals("navigation_drawer")||s.equals("media_viewer")||s.equals("search_edit_text")||s.equals("comment_edit_text")||s.equals("tweet_compose")||s.equals("composer");}
    private static View homeButton(Activity a){
        try {
            View root=a.getWindow().peekDecorView();Rect screen=new Rect();if(root==null||!root.getGlobalVisibleRect(screen))return null;
            ArrayDeque<View> q=new ArrayDeque<View>();q.add(root);View target=null;int count=0;
            while(!q.isEmpty()){
                if(++count>1600)return null;View v=q.removeFirst();Rect r=new Rect();
                if(!v.isShown()||v.getAlpha()<=.01f||!v.getGlobalVisibleRect(r)||r.isEmpty())continue;
                String resource=id(v),name=norm(v.getContentDescription());
                if(v.onCheckIsTextEditor()||blocked(resource))return null;
                if(v.isClickable()&&r.centerY()<screen.top+screen.height()/4&&(name.equals("back")||name.equals("navigate up")||name.equals("戻る")||name.equals("閉じる")))return null;
                if(nav(r,screen)&&(homeId(resource)||homeName(name)||homeResource(v))){
                    View button=v;
                    for(int n=0;n<2&&!button.isClickable()&&button.getParent() instanceof View;n++){
                        View parent=(View)button.getParent();Rect area=new Rect();if(!parent.getGlobalVisibleRect(area)||!nav(area,screen))break;button=parent;}
                    if(button.isClickable()&&button.isEnabled())target=button;
                }
                if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;if(g.getChildCount()>1600-count)return null;for(int n=0;n<g.getChildCount();n++)q.add(g.getChildAt(n));}
            }
            return target;
        }catch(RuntimeException|LinkageError ignored){return null;}
    }
    private enum Failure { INSTANCE }
    private static Object invoke(Object o,String name){if(o==null)return null;for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass()){
        if(!Modifier.isPublic(c.getModifiers()))continue;
        try{Method m=c.getMethod(name);if(Modifier.isPublic(m.getDeclaringClass().getModifiers()))return m.invoke(o);}
        catch(NoSuchMethodException absent){}catch(ReflectiveOperationException|RuntimeException e){return Failure.INSTANCE;}}
        return null;
    }
    private static boolean backStack(Activity a){Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
        return stack(invoke(a,"getFragmentManager"),seen,0)||stack(invoke(a,"getSupportFragmentManager"),seen,0);}
    private static boolean stack(Object manager,Set<Object> seen,int depth){
        if(manager==null)return false;if(manager==Failure.INSTANCE||depth>8)return true;if(!seen.add(manager))return false;
        Object count=invoke(manager,"getBackStackEntryCount"),fragments=invoke(manager,"getFragments");
        if(count==Failure.INSTANCE||fragments==Failure.INSTANCE||(count instanceof Number&&((Number)count).intValue()>0))return true;
        if(fragments instanceof Iterable<?>)for(Object f:(Iterable<?>)fragments)if(f!=null&&Boolean.TRUE.equals(invoke(f,"isVisible"))&&stack(invoke(f,"getChildFragmentManager"),seen,depth+1))return true;
        return false;
    }
    static void retryTasks(Activity a,Set<Integer> ids){
        if(!owns(a)||ids.isEmpty())return;final Application app=a.getApplication();final Session s=session(a);final long e=s.epoch;
        final Set<Integer> targets=new HashSet<Integer>(ids);
        for(long delay:new long[]{80,300,900})MAIN.postDelayed(new Runnable(){@Override public void run(){
            if(s.epoch!=e||OneBackExit.anyResumed(app.getPackageName()))return;
            try{Object svc=app.getSystemService(Context.ACTIVITY_SERVICE);if(!(svc instanceof ActivityManager))return;
                java.util.List<ActivityManager.AppTask> tasks=((ActivityManager)svc).getAppTasks();if(tasks==null)return;
                for(ActivityManager.AppTask task:new ArrayList<ActivityManager.AppTask>(tasks))try{
                    ActivityManager.RecentTaskInfo info=task.getTaskInfo();if(info==null||!targets.contains(info.taskId))continue;
                    ComponentName base=info.baseActivity;if(base==null&&info.baseIntent!=null)base=info.baseIntent.getComponent();
                    if(base==null||!app.getPackageName().equals(base.getPackageName()))continue;
                    try{task.setExcludeFromRecents(true);}catch(RuntimeException ignored){}
                    try{task.finishAndRemoveTask();}catch(RuntimeException ignored){}
                }catch(RuntimeException ignored){}
            }catch(RuntimeException ignored){}
        }},delay);
    }
}
