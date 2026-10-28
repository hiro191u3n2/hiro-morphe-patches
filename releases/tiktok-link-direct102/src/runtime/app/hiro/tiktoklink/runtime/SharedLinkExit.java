/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktoklink.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowInsets;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Process-owned authority for a natively admitted external shared-video route.
 * No system Back callback is registered: native detail guards run before tryExit.
 * Platform cleanup is invoked reflectively so another selected patch cannot
 * rewrite these calls into its Home-only navigation transaction.
 */
public final class SharedLinkExit {
    private static final String PACKAGE = "com.zhiliaoapp.musically";
    private static final String DETAIL = "com.ss.android.ugc.aweme.detail.ui.DetailActivity";
    private static final String FRAGMENT = "com.ss.android.ugc.aweme.detail.ui.saf.DetailSafRootFragment";
    private static final String TOKEN = "app.hiro.tiktoklink.issued_session102";
    private static final long PENDING_MS = 300000L;
    private static final long KILL_DELAY_MS = 100L;
    private static final int LIMIT = 64;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<Intent, Pending> PENDING = new WeakHashMap<Intent, Pending>();
    private static final Map<String, Session> ISSUED = new LinkedHashMap<String, Session>();
    private static final Map<Object, Session> BOUND = new WeakHashMap<Object, Session>();
    private static final Map<Activity, Boolean> ACTIVITIES = new WeakHashMap<Activity, Boolean>();
    private static final Map<Activity, Boolean> CLOSING = new WeakHashMap<Activity, Boolean>();
    private static final Map<Application, Boolean> APPLICATIONS = new WeakHashMap<Application, Boolean>();
    private static boolean exiting;
    private static long exitGeneration;

    private SharedLinkExit() {}
    private static boolean main() { return Looper.myLooper() == Looper.getMainLooper(); }
    private static final class Pending {
        final String id;
        final long time;
        Pending(String id) { this.id=id; time=SystemClock.uptimeMillis(); }
    }
    private static final class Session {
        final String token, id;
        final long time;
        WeakReference<Object> fragment;
        WeakReference<Activity> activity;
        boolean bound;
        Session(String token,String id) { this.token=token; this.id=id; time=SystemClock.uptimeMillis(); }
    }
    private static boolean named(Class<?> type,String name) {
        for (Class<?> c=type;c!=null;c=c.getSuperclass()) if(name.equals(c.getName())) return true;
        return false;
    }
    private static Object invoke(Object receiver,String name) {
        if(receiver==null)return null;
        try { return receiver.getClass().getMethod(name).invoke(receiver); }
        catch(ReflectiveOperationException|RuntimeException|LinkageError ignored) { return null; }
    }
    private static Activity owner(Object fragment) {
        if(fragment==null||!named(fragment.getClass(),FRAGMENT))return null;
        Object value=invoke(fragment,"getActivity");
        return value instanceof Activity?(Activity)value:null;
    }
    private static boolean owned(Activity a) { return a!=null&&PACKAGE.equals(a.getPackageName()); }
    private static Intent route(Object fragment) {
        Object value=invoke(fragment,"eY");
        return value instanceof Intent?(Intent)value:null;
    }
    private static String target(Uri uri) {
        if(uri==null||!uri.isHierarchical())return null;
        String host=uri.getHost(),id=null;
        if("aweme".equals(host)) {
            String path=uri.getPath();
            if(path!=null&&path.matches("/detail/[1-9][0-9]{0,19}/?")) {
                id=path.substring(8); if(id.endsWith("/"))id=id.substring(0,id.length()-1);
            }
        } else if("detail".equals(host)||"item".equals(host)||"musical".equals(host)) id=uri.getQueryParameter("id");
        return id!=null&&id.matches("[1-9][0-9]{0,19}")?id:null;
    }
    private static boolean detail(Intent intent,String id) {
        if(intent==null||id==null)return false;
        ComponentName component=intent.getComponent();
        return component!=null&&PACKAGE.equals(component.getPackageName())&&DETAIL.equals(component.getClassName())
            &&id.equals(intent.getStringExtra("id"));
    }
    private static void revoke(Session s) {
        if(s==null)return;
        ISSUED.remove(s.token);
        Object f=s.fragment==null?null:s.fragment.get();
        if(f!=null&&BOUND.get(f)==s)BOUND.remove(f);
    }
    private static void prune() {
        long now=SystemClock.uptimeMillis();
        for(Iterator<Map.Entry<Intent,Pending>> i=PENDING.entrySet().iterator();i.hasNext();)
            if(now-i.next().getValue().time>PENDING_MS)i.remove();
        for(Session s:new ArrayList<Session>(ISSUED.values())) {
            if((!s.bound&&now-s.time>PENDING_MS)||(s.bound&&(s.fragment.get()==null||s.activity.get()==null)))revoke(s);
        }
        while(ISSUED.size()>LIMIT)revoke(ISSUED.values().iterator().next());
    }
    private static void cancelExitForNewEntry() {
        if(exiting) { exiting=false; exitGeneration++; CLOSING.clear(); }
    }

    /** Called only on the admitted positive branch of the pinned native selector. */
    public static synchronized void prepare(Intent original,Uri uri) {
        if(original==null)return;
        try {
            prune(); String id=target(uri); if(id==null)return;
            cancelExitForNewEntry();
            PENDING.put(original,new Pending(id));
            if(PENDING.size()>LIMIT)PENDING.clear();
        } catch(RuntimeException|LinkageError ignored) { PENDING.remove(original); }
    }
    /** The pinned native factories supply their original temporary and final Intents. */
    public static synchronized void transfer(Intent original,Intent actual) {
        if(original==null||actual==null)return;
        try {
            prune(); Pending pending=PENDING.remove(original);
            if(pending==null||!detail(actual,pending.id))return;
            String token=UUID.randomUUID().toString();
            actual.putExtra(TOKEN,token);
            ISSUED.put(token,new Session(token,pending.id));
            prune();
        } catch(RuntimeException|LinkageError ignored) { /* Unrecognized routes retain native behavior. */ }
    }
    private static Session issued(Intent intent) {
        if(intent==null)return null;
        String token=intent.getStringExtra(TOKEN);
        Session s=token==null?null:ISSUED.get(token);
        return s!=null&&detail(intent,s.id)?s:null;
    }
    private static Session issuedRoute(Object fragment) {
        Intent intent=route(fragment);if(intent==null)return null;
        Session direct=issued(intent);if(direct!=null)return direct;
        // Native 06fM.LJFF reconstructs a SAF route as a componentless Intent
        // containing the complete fragment arguments. Final factory/animation
        // admission stays strict; only this verified fragment binding accepts it.
        if(intent.getComponent()!=null)return null;
        Object value=invoke(fragment,"getArguments");if(!(value instanceof Bundle))return null;
        Bundle args=(Bundle)value;if(args.getInt("use_saf",0)<1)return null;
        String token=intent.getStringExtra(TOKEN);
        Session s=token==null?null:ISSUED.get(token);
        return s!=null&&s.token.equals(args.getString(TOKEN))&&s.id.equals(args.getString("id"))
            &&s.id.equals(intent.getStringExtra("id"))?s:null;
    }
    /** A Boolean or copied stale token supplied by a caller never authorizes this. */
    public static synchronized boolean isPrepared(Intent intent) {
        if(exiting)return false;
        try { prune(); return issued(intent)!=null; }
        catch(RuntimeException|LinkageError ignored) { return false; }
    }
    private static boolean currentRoute(Object fragment,Activity a,Session s) {
        if(!owned(a)||a.isDestroyed()||a.isFinishing()||issuedRoute(fragment)!=s)return false;
        // Dedicated Detail lazily caches its route Intent. Reject that old cache
        // when onNewIntent has installed a different current Activity Intent.
        return !named(a.getClass(),DETAIL)||issued(a.getIntent())==s;
    }

    /** Called after native onResume. eY resolves both dedicated and SAF route Intents. */
    public static synchronized void bind(Object fragment) {
        if(!main()||fragment==null)return;
        try {
            prune(); Activity a=owner(fragment); Session old=BOUND.get(fragment);
            Session s=issuedRoute(fragment);
            if(old!=null&&old!=s)revoke(old);
            if(s==null||!currentRoute(fragment,a,s)) { if(old!=null)revoke(old); return; }
            if(s.bound&&(s.fragment.get()!=fragment||s.activity.get()!=a))return;
            if(exiting&&!CLOSING.containsKey(a))cancelExitForNewEntry();
            if(exiting)return;
            s.bound=true; s.fragment=new WeakReference<Object>(fragment); s.activity=new WeakReference<Activity>(a);
            BOUND.put(fragment,s); ACTIVITIES.put(a,Boolean.TRUE); observe(a.getApplication());
        } catch(RuntimeException|LinkageError ignored) { forget(fragment); }
    }
    public static synchronized void forget(Object fragment) {
        if(!main()||fragment==null)return;
        Session s=BOUND.remove(fragment); if(s!=null)revoke(s);
    }
    private static boolean ready(Object fragment,Activity a) {
        if(!Boolean.TRUE.equals(invoke(fragment,"isResumed"))||!Boolean.TRUE.equals(invoke(fragment,"isVisible")))return false;
        Object fv=invoke(fragment,"getView"); if(!(fv instanceof View))return false;
        View view=(View)fv,root=a.getWindow()==null?null:a.getWindow().peekDecorView();
        if(root==null||!root.isAttachedToWindow()||!root.hasWindowFocus()||!root.isShown()
                ||!view.isAttachedToWindow()||!view.isShown()||view.getRootView()!=root.getRootView())return false;
        Rect bounds=new Rect();
        if(!view.getGlobalVisibleRect(bounds)||bounds.width()<=0||bounds.height()<=0)return false;
        View focus=root.findFocus();
        if(focus!=null&&focus.onCheckIsTextEditor()&&focus.isShown()&&focus.getGlobalVisibleRect(new Rect()))return false;
        if(Build.VERSION.SDK_INT>=30) {
            WindowInsets insets=root.getRootWindowInsets();
            if(insets==null||insets.isVisible(WindowInsets.Type.ime()))return false;
        } else {
            Rect visible=new Rect(),full=new Rect();root.getWindowVisibleDisplayFrame(visible);
            if(!root.getGlobalVisibleRect(full)||full.bottom-visible.bottom>full.height()/5)return false;
        }
        if(Build.VERSION.SDK_INT>=29)for(View window:android.view.inspector.WindowInspector.getGlobalWindowViews())
            if(window!=null&&window!=root&&window.isAttachedToWindow()&&window.isShown()&&window.hasWindowFocus()
                &&PACKAGE.equals(window.getContext().getPackageName()))return false;
        return true;
    }

    /** Called only after native thumbnail/key/comment/child consumers declined Back. */
    public static synchronized boolean tryExit(Object fragment) {
        if(!main())return false;
        try {
            Activity a=owner(fragment);
            if(exiting&&CLOSING.containsKey(a))return true;
            Session s=BOUND.get(fragment);
            if(s==null||!currentRoute(fragment,a,s)) { if(s!=null)revoke(s); return false; }
            if(!ready(fragment,a))return false;
            return exit(a);
        } catch(RuntimeException|LinkageError ignored) { return false; }
    }
    /** bY contains back_to/launcher navigation; it must not recreate TikTok during our exit. */
    public static synchronized boolean suppressNativeReturn(Object fragment) {
        return main()&&exiting&&CLOSING.containsKey(owner(fragment));
    }
    private static void observe(Application app) {
        if(app==null||APPLICATIONS.containsKey(app))return;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private void active(Activity a) {
                synchronized(SharedLinkExit.class) {
                    if(!owned(a))return;
                    if(exiting&&!CLOSING.containsKey(a))cancelExitForNewEntry();
                    ACTIVITIES.put(a,Boolean.TRUE);
                }
            }
            public void onActivityCreated(Activity a,Bundle b) { active(a); }
            public void onActivityStarted(Activity a) {}
            public void onActivityResumed(Activity a) { active(a); }
            public void onActivityPaused(Activity a) {}
            public void onActivityStopped(Activity a) {}
            public void onActivitySaveInstanceState(Activity a,Bundle b) {}
            public void onActivityDestroyed(Activity a) {
                synchronized(SharedLinkExit.class) {
                    ACTIVITIES.remove(a);
                    for(Session s:new ArrayList<Session>(ISSUED.values()))
                        if(s.bound&&s.activity.get()==a)revoke(s);
                }
            }
        });
        APPLICATIONS.put(app,Boolean.TRUE);
    }
    private static boolean platform(Object target,Class<?> type,String name,Class<?>[] types,Object... args) {
        try { Method m=type.getMethod(name,types);m.invoke(target,args);return true; }
        catch(ReflectiveOperationException|RuntimeException|LinkageError ignored) { return false; }
    }
    private static String taskPackage(ActivityManager.RecentTaskInfo info) {
        ComponentName base=Build.VERSION.SDK_INT>=29?info.baseActivity:null;
        ComponentName intentBase=info.baseIntent==null?null:info.baseIntent.getComponent();
        if(base!=null&&intentBase!=null&&!base.getPackageName().equals(intentBase.getPackageName()))return null;
        if(base==null)base=intentBase;
        return base==null?null:base.getPackageName();
    }
    private static boolean exit(Activity trigger) {
        exiting=true; final long generation=++exitGeneration;
        List<Activity> closing=new ArrayList<Activity>(ACTIVITIES.keySet());
        if(!closing.contains(trigger))closing.add(trigger);
        CLOSING.clear();for(Activity a:closing)if(owned(a))CLOSING.put(a,Boolean.TRUE);
        ISSUED.clear();BOUND.clear();PENDING.clear();
        Set<Integer> removed=new HashSet<Integer>(),foreign=new HashSet<Integer>();
        boolean requested=false;
        try {
            Object service=trigger.getSystemService(android.content.Context.ACTIVITY_SERVICE);
            if(service instanceof ActivityManager) {
                List<ActivityManager.AppTask> tasks=((ActivityManager)service).getAppTasks();
                if(tasks!=null)for(ActivityManager.AppTask task:new ArrayList<ActivityManager.AppTask>(tasks))try {
                    if(task==null)continue;
                    ActivityManager.RecentTaskInfo info=task.getTaskInfo();if(info==null)continue;
                    int id=Build.VERSION.SDK_INT>=29?info.taskId:info.id;
                    if(!PACKAGE.equals(taskPackage(info))) { if(id>=0)foreign.add(id);continue; }
                    platform(task,ActivityManager.AppTask.class,"setExcludeFromRecents",new Class<?>[]{boolean.class},true);
                    if(platform(task,ActivityManager.AppTask.class,"finishAndRemoveTask",new Class<?>[0])) {
                        requested=true;if(id>=0)removed.add(id);
                    }
                } catch(RuntimeException|LinkageError ignored) { /* Continue with the other tasks. */ }
            }
        } catch(RuntimeException|LinkageError ignored) { /* Tracked Activity fallback remains available. */ }
        for(Activity a:closing)try {
            if(!owned(a)||a.isDestroyed()||a.isFinishing()||removed.contains(a.getTaskId()))continue;
            if(a.isTaskRoot()&&!foreign.contains(a.getTaskId())) {
                if(platform(a,Activity.class,"finishAndRemoveTask",new Class<?>[0])) { requested=true;continue; }
            }
            // Never remove the caller's task. bY's exit guard prevents its native return launcher.
            if(platform(a,Activity.class,"finish",new Class<?>[0]))requested=true;
        } catch(RuntimeException|LinkageError ignored) { /* Continue with other tracked activities. */ }
        if(!requested) { exiting=false;exitGeneration++;CLOSING.clear();return false; }
        android.util.Log.d("HiroLink102","shared-video Back: task removal requested; own-process exit scheduled");
        MAIN.postDelayed(new Runnable() { public void run() {
            synchronized(SharedLinkExit.class) {
                if(!exiting||exitGeneration!=generation)return;
                Process.killProcess(Process.myPid());
            }
        }},KILL_DELAY_MS);
        return true;
    }
}
