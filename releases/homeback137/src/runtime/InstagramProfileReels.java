package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** A committed native Back must actually remove the registered URL profile
 * before returning to Reels. Opening a child, dismissing a layer, and retained
 * tab state are not removal evidence. Selection uses the verified 452 native
 * controller; the selected wrapper must attach before its old stack is reset.
 */
final class InstagramProfileReels {
    private InstagramProfileReels() {}
    private static final String PROFILE="com.instagram.profile.fragment.UserDetailFragment";
    private static final String MAIN="com.instagram.mainactivity.InstagramMainActivity";
    private static final String FRAGMENT="androidx.fragment.app.Fragment";
    private static final String ACTIVITY="androidx.fragment.app.FragmentActivity";
    private static final String MANAGER="X.00fk",TAB="X.08kM";
    private static final String ENTRANCE="INSTAGRAM_MAIN_ACTIVITY_INTENT_ENTRANCE_MODULE_EXTRA_FIELD";
    private static final String HANDOFF="app.hiro.oneback.runtime.PROFILE_REELS_452";
    private static final Handler HANDLER=new Handler(Looper.getMainLooper());
    private static final Map<Activity,Entry> ENTRIES=new WeakHashMap<Activity,Entry>();
    private static final Map<Activity,Pending> PENDING=new WeakHashMap<Activity,Pending>();
    private static final Map<Activity,WeakReference<Intent>> INTENTS=new WeakHashMap<Activity,WeakReference<Intent>>();
    private static final Map<Application,Object> GENERATIONS=new WeakHashMap<Application,Object>();

    private static final class Entry {
        final WeakReference<Object> profile;final int task;
        boolean revoked,used;
        Entry(Activity activity,Object fragment){profile=new WeakReference<Object>(fragment);task=activity.getTaskId();}
    }
    private static final class Attempt {
        final Activity activity;final Entry entry;final Object profile,parent,generation;
        boolean completed;
        Attempt(Activity activity,Entry entry,Object profile,Object parent){
            this.activity=activity;this.entry=entry;this.profile=profile;this.parent=parent;generation=generation(activity);
        }
    }
    private static final class Pending {
        final int task;final Object generation;boolean selecting,queued;
        Pending(Activity activity){task=activity.getTaskId();generation=generation(activity);}
    }

    /** Called only after the existing native-route envelope was validated. */
    static void remember(Activity activity,Object profile) {
        if(!mainThread()||!InstagramBack.owns(activity)||profile==null||!PROFILE.equals(profile.getClass().getName()))return;
        try {
            if(activity.isDestroyed()||activity.isFinishing())return;
            advance(activity);revoke(activity);PENDING.remove(activity);
            ENTRIES.put(activity,new Entry(activity,profile));
        }catch(RuntimeException|LinkageError unknown){ }
    }

    /** Snapshot only. A child or overlay may still consume this native Back. */
    static Object begin(Activity activity) {
        if(!mainThread()||!InstagramBack.owns(activity))return null;
        try {
            Entry entry=ENTRIES.get(activity);Object profile=entry==null?null:entry.profile.get();
            if(entry==null||entry.revoked||entry.used||profile==null||entry.task<0||entry.task!=activity.getTaskId()
                    ||activity.isDestroyed()||activity.isFinishing()||activity.isChangingConfigurations()
                    ||!allowedHost(activity)||fragment(profile,"getActivity",ACTIVITY)!=activity
                    ||!yes(profile,"isAdded")||yes(profile,"isRemoving")||!yes(profile,"isResumed"))return null;
            View decor=decor(activity);Object value=fragment(profile,"getView",View.class.getName());
            if(!(value instanceof View)||decor==null||!decor.isAttachedToWindow()||!decor.hasWindowFocus()
                    ||!((View)value).isAttachedToWindow()||((View)value).getRootView()!=decor)return null;
            Rect area=new Rect(),screen=new Rect();
            if(!InstagramBack.shown((View)value,area)||!InstagramBack.shown(decor,screen)
                    ||!InstagramReels.large(area,screen))return null;
            return new Attempt(activity,entry,profile,modalParent(activity,profile));
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){return null;}
    }

    /** Runs after the original native/AndroidX commit body, including a normal
     * Activity finish. Destruction alone cannot grant or revoke that finish. */
    static void complete(Object value) {
        if(!mainThread()||!(value instanceof Attempt))return;
        final Attempt attempt=(Attempt)value;
        if(attempt.completed)return;attempt.completed=true;
        try {HANDLER.post(new Runnable(){public void run(){observe(attempt);}});}
        catch(RuntimeException|LinkageError unknown){ }
    }
    private static void observe(Attempt attempt) {
        Activity activity=attempt.activity;Entry entry=attempt.entry;
        try {
            int task=activity.getTaskId();
            if(entry.revoked||entry.used||attempt.generation!=generation(activity)
                    ||(entry.task!=task&&(task>=0||!activity.isFinishing()))||activity.isChangingConfigurations()
                    ||(activity.isDestroyed()&&!activity.isFinishing()))return;
            if(!activity.isFinishing()&&!removed(attempt.profile)
                    &&(attempt.parent==null||!removed(attempt.parent)))return;
            entry.used=true;
            if(named(activity.getClass(),MAIN)!=null&&!activity.isFinishing()&&!activity.isDestroyed()) {
                Pending pending=new Pending(activity);PENDING.put(activity,pending);drive(activity,pending);
            } else handoff(activity);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){ }
    }

    static void newIntent(Activity activity,Intent intent) {
        if(!mainThread()||!InstagramBack.owns(activity)||intent==null)return;
        try {
            WeakReference<Intent> previous=INTENTS.get(activity);
            if(previous!=null&&previous.get()==intent)return;
            INTENTS.put(activity,new WeakReference<Intent>(intent));
            advance(activity);revoke(activity);PENDING.remove(activity);
            consume(activity,intent);
        }catch(RuntimeException|LinkageError unknown){ }
    }
    static void destroyed(Activity activity) {
        if(!mainThread()||activity==null)return;
        // A completed native Back may have finished this owner before its
        // observer runs. Keep the attempt's entry valid; newIntent/remember
        // explicitly revoke superseded entries instead.
        ENTRIES.remove(activity);PENDING.remove(activity);INTENTS.remove(activity);
    }
    static void installed(Activity activity) {
        if(!mainThread()||!InstagramBack.owns(activity))return;
        try {
            consume(activity,activity.getIntent());
            Pending pending=PENDING.get(activity);if(pending!=null)queue(activity,pending);
        }catch(RuntimeException|LinkageError unknown){ }
    }
    static void draw(Activity activity) {
        if(!mainThread()||!InstagramBack.owns(activity))return;
        Pending pending=PENDING.get(activity);if(pending!=null)drive(activity,pending);
    }
    /** A later user action owns navigation even if a wrapper is still loading. */
    static void interaction(Activity activity) {
        if(mainThread()&&activity!=null)PENDING.remove(activity);
    }
    private static Object generation(Activity activity) {
        Application application=activity.getApplication();Object token=GENERATIONS.get(application);
        if(token==null){token=new Object();GENERATIONS.put(application,token);}return token;
    }
    private static void advance(Activity activity){GENERATIONS.put(activity.getApplication(),new Object());}
    private static void revoke(Activity activity) {
        Entry previous=ENTRIES.remove(activity);if(previous!=null)previous.revoked=true;
    }
    private static void consume(Activity activity,Intent intent) {
        if(named(activity.getClass(),MAIN)==null||intent==null||!intent.getBooleanExtra(HANDOFF,false))return;
        intent.putExtra(HANDOFF,false);revoke(activity);
        Pending pending=new Pending(activity);PENDING.put(activity,pending);queue(activity,pending);
    }
    private static void queue(final Activity activity,final Pending pending) {
        if(pending.queued)return;pending.queued=true;
        HANDLER.post(new Runnable(){public void run(){pending.queued=false;drive(activity,pending);}});
    }
    private static void handoff(Activity activity) {
        Intent intent=new Intent().setComponent(new ComponentName(activity.getPackageName(),MAIN))
            .addFlags(0x14000000).putExtra(ENTRANCE,"CLIPS")
            // The native account-startup selector has priority over the cold
            // flags. An explicit value also prevents Piko's startup preference
            // hook from inserting a different tab into this handoff.
            .putExtra("MainActivityAccountHelper.STARTUP_TAB","CLIPS")
            .putExtra("is_cold_start_reel_tab",true).putExtra(HANDOFF,true);
        activity.startActivity(intent);
        if(named(activity.getClass(),MAIN)==null&&!activity.isDestroyed()&&!activity.isFinishing())activity.finish();
    }

    /** HNv updates the selected tab immediately, but ViewPager2 can attach its
     * wrapper during a later layout. Draw/install callbacks supply that event;
     * there is no timeout, forced layout, or self-reposting polling loop. */
    private static void drive(Activity activity,Pending pending) {
        if(PENDING.get(activity)!=pending)return;
        try {
            if(activity.isDestroyed()||activity.isFinishing()||pending.task!=activity.getTaskId()
                    ||pending.generation!=generation(activity)) {
                PENDING.remove(activity);return;
            }
            Class<?> main=named(activity.getClass(),MAIN);if(main==null){PENDING.remove(activity);return;}
            Object controller=controller(activity,main);if(controller==null)return;
            Class<?> tabType=Class.forName(TAB,false,activity.getClass().getClassLoader());
            Field clipsField=tabType.getDeclaredField("A08");int flags=clipsField.getModifiers();
            if(!Modifier.isPublic(flags)||!Modifier.isStatic(flags)||!Modifier.isFinal(flags)
                    ||clipsField.getType()!=tabType)throw new NoSuchFieldException("CLIPS");
            Object clips=clipsField.get(null);
            if(!(clips instanceof Enum<?>)||!"CLIPS".equals(((Enum<?>)clips).name()))throw new NoSuchFieldException("CLIPS");
            if(!pending.selecting) {
                // Native A09 defers the tab request when the Activity manager
                // is unavailable or saved. Wait before HNv so that a deferred
                // request is not mistaken for a later user tab change.
                Object root=method(named(activity.getClass(),ACTIVITY),"A1Z",MANAGER).invoke(activity);
                if(!managerReady(root))return;
                pending.selecting=true;
                method(main,"HNv","void",tabType).invoke(activity,clips);
            }
            // A later explicit tab choice cancels this pending reset.
            if(method(controller.getClass(),"A06",TAB).invoke(controller)!=clips){PENDING.remove(activity);return;}
            Object wrapper=method(controller.getClass(),"A05",FRAGMENT).invoke(controller);
            if(wrapper==null)return;
            String type=wrapper.getClass().getName();
            if(!"X.00VS".equals(type)&&!"X.02Wh".equals(type)){PENDING.remove(activity);return;}
            if(fragment(wrapper,"getActivity",ACTIVITY)!=activity||!yes(wrapper,"isAdded")||yes(wrapper,"isRemoving"))return;
            View decor=decor(activity);Object view=fragment(wrapper,"getView",View.class.getName());
            if(!(view instanceof View)||decor==null||!decor.isAttachedToWindow()
                    ||!((View)view).isAttachedToWindow()||((View)view).getRootView()!=decor
                    ||!InstagramBack.shown((View)view,new Rect()))return;
            Object manager=fragment(wrapper,"getChildFragmentManager",MANAGER);
            if(!managerReady(manager))return;
            // Remove first so a synchronous lifecycle callback cannot reset
            // twice. HNw itself selects CLIPS and pops its old child stack.
            PENDING.remove(activity);
            method(main,"HNw","void",tabType).invoke(activity,clips);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){PENDING.remove(activity);}
    }
    private static Object controller(Activity activity,Class<?> main)throws ReflectiveOperationException {
        Object navigation=field(main,"A08","X.00M2").get(activity);
        if(navigation==null)return null;
        if(!"X.00M2".equals(navigation.getClass().getName()))throw new NoSuchFieldException("navigation");
        Object controller=field(navigation.getClass(),"A02","X.004K").get(navigation);
        if(controller==null)return null;
        if(!"X.004K".equals(controller.getClass().getName())
                ||field(controller.getClass(),"A03",ACTIVITY).get(controller)!=activity)throw new NoSuchFieldException("controller");
        return controller;
    }
    private static boolean managerReady(Object manager)throws ReflectiveOperationException {
        if(manager==null||named(manager.getClass(),"X.00fn")==null)return false;
        Class<?> type=named(manager.getClass(),MANAGER);
        if(Boolean.TRUE.equals(method(type,"A15","boolean").invoke(manager))
                ||field(type,"A0G","boolean").getBoolean(manager)
                ||field(type,"mExecutingActions","boolean").getBoolean(manager))return false;
        Object value=field(type,"A0W",ArrayList.class.getName()).get(manager);
        if(!(value instanceof ArrayList<?>))return false;
        synchronized(value){return ((ArrayList<?>)value).isEmpty();}
    }
    private static Object modalParent(Activity activity,Object profile)throws ReflectiveOperationException {
        Object parent=fragment(profile,"getParentFragment",FRAGMENT);
        if(parent==null||!"X.03CG".equals(parent.getClass().getName())||named(activity.getClass(),MAIN)==null
                ||fragment(parent,"getActivity",ACTIVITY)!=activity||!yes(parent,"isAdded")||yes(parent,"isRemoving"))return null;
        Object service=field(named(activity.getClass(),MAIN),"A09","X.0A4v").get(activity);
        if(service==null||!"X.0A4v".equals(service.getClass().getName()))return null;
        Object reference=field(service.getClass(),"A06",WeakReference.class.getName()).get(service);
        return reference instanceof WeakReference<?> && ((WeakReference<?>)reference).get()==parent?parent:null;
    }
    private static boolean removed(Object fragment)throws ReflectiveOperationException {
        return !yes(fragment,"isAdded")||yes(fragment,"isRemoving");
    }
    private static boolean yes(Object object,String name)throws ReflectiveOperationException {
        return Boolean.TRUE.equals(fragment(object,name,"boolean"));
    }
    private static Object fragment(Object object,String name,String returns)throws ReflectiveOperationException {
        Class<?> owner=named(object.getClass(),FRAGMENT);
        if("getView".equals(name)) {
            Method method=owner.getDeclaredMethod(name);int flags=method.getModifiers();
            if(!Modifier.isPublic(flags)||Modifier.isStatic(flags)||!returns.equals(method.getReturnType().getName()))throw new NoSuchMethodException(name);
            return method.invoke(object);
        }
        return method(owner,name,returns).invoke(object);
    }
    private static boolean allowedHost(Activity activity) {
        String type=activity.getClass().getName();
        return named(activity.getClass(),MAIN)!=null||"com.instagram.url.UrlHandlerActivity".equals(type)
            ||"com.instagram.url.UrlHandlerLauncherActivity".equals(type);
    }
    private static View decor(Activity activity){return activity.getWindow()==null?null:activity.getWindow().peekDecorView();}
    private static boolean mainThread(){return Looper.myLooper()==Looper.getMainLooper();}
    private static Class<?> named(Class<?> type,String name){return InstagramReelsExit.named(type,name);}
    private static Method method(Class<?> owner,String name,String returns,Class<?>... parameters)throws ReflectiveOperationException {
        if(owner==null||!Modifier.isPublic(owner.getModifiers()))throw new NoSuchMethodException(name);
        Method method=owner.getDeclaredMethod(name,parameters);int flags=method.getModifiers();
        if(!Modifier.isPublic(flags)||!Modifier.isFinal(flags)||Modifier.isStatic(flags)
                ||!returns.equals(method.getReturnType().getName()))throw new NoSuchMethodException(name);
        return method;
    }
    private static Field field(Class<?> owner,String name,String type)throws ReflectiveOperationException {
        if(owner==null||!Modifier.isPublic(owner.getModifiers()))throw new NoSuchFieldException(name);
        Field field=owner.getDeclaredField(name);int flags=field.getModifiers();
        if(!Modifier.isPublic(flags)||Modifier.isStatic(flags)||!type.equals(field.getType().getName()))throw new NoSuchFieldException(name);
        return field;
    }
}
