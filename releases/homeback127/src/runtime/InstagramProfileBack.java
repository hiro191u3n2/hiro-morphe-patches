package app.hiro.oneback.runtime;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** A profile-launched video returns through the app's own native Back route.
 * Source belongs to the video, not to the bottom tab retained behind it. Reads
 * only the verified v447 config/current-content contracts; never opens a profile
 * or invents a navigation destination. A Back that began here keeps that policy
 * until native dispatch/prediction ends, even if it reveals the CLIPS tab.
 */
final class InstagramProfileBack {
    private InstagramProfileBack() {}
    private static final String FRAGMENT="androidx.fragment.app.Fragment";
    private static final String CONFIG="com.instagram.clips.intf.ClipsViewerConfig";
    private static final String SOURCE="com.instagram.clips.intf.ClipsViewerSource";
    private static final ThreadLocal<Activity> NATIVE=new ThreadLocal<Activity>();
    private static final Map<Activity,Boolean> PREDICTIONS=new WeakHashMap<Activity,Boolean>();
    private static final Map<Activity,Object> RETURNS=new WeakHashMap<Activity,Object>();
    private static final Handler MAIN_HANDLER=new Handler(Looper.getMainLooper());
    private static final int MAIN=0x7f0b226f,BASE=0x7f0b2275;

    /** Native enum names include PROFILE and SELF_PROFILE; compound profile
     * sources use the same underscore-delimited token. No UI text is examined.
     */
    private static boolean profileToken(String value) {
        if(value==null)return false;
        for(String token:value.toLowerCase(Locale.ROOT).split("_"))if("profile".equals(token))return true;
        return false;
    }
    static boolean fromProfile(Object fragment) {
        if(fragment==null||fragment==Boolean.FALSE)return false;
        boolean native452=Instagram452.reels(fragment);
        Class<?> reels=InstagramReelsExit.named(fragment.getClass(),native452?"X.02Hr":"X.8Zv");
        if(reels!=null)try {
            Object config=method(reels,native452?"A0o":"A2F",CONFIG,false).invoke(fragment);
            if(config!=null&&CONFIG.equals(config.getClass().getName())) {
                Field field=field(config.getClass(),"A00",SOURCE);
                Object source=field.get(config);
                if(source instanceof Enum<?> && source.getClass()==field.getType()) {
                    // This actual per-video enum is authoritative even when the
                    // analytics module is overridden in ClipsViewerConfig.A01.
                    return profileToken(((Enum<?>)source).name());
                }
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable) { }
        // Earlier supported hosts may expose only the semantic module getter.
        // An exact viewer prefix keeps ordinary profiles/captions out of scope.
        Object module=BackNavigation.invoke(fragment,"getModuleName");
        if(!(module instanceof String))return false;
        String name=((String)module).trim().toLowerCase(Locale.ROOT);
        return name.startsWith("clips_viewer_")&&profileToken(name.substring("clips_viewer_".length()));
    }
    private static Method method(Class<?> owner,String name,String returns,boolean global,Class<?>... args)throws ReflectiveOperationException {
        if(owner==null||!Modifier.isPublic(owner.getModifiers()))throw new NoSuchMethodException(name);
        Method method=owner.getDeclaredMethod(name,args);int flags=method.getModifiers();
        if(!Modifier.isPublic(flags)||!Modifier.isFinal(flags)||Modifier.isStatic(flags)!=global
                ||!returns.equals(method.getReturnType().getName()))throw new NoSuchMethodException(name);
        return method;
    }
    private static Field field(Class<?> owner,String name,String type)throws ReflectiveOperationException {
        Field field=owner.getDeclaredField(name);int flags=field.getModifiers();
        if(!Modifier.isPublic(flags)||Modifier.isStatic(flags)||!type.equals(field.getType().getName()))throw new NoSuchFieldException(name);
        return field;
    }
    private static Object child(Object fragment)throws ReflectiveOperationException {
        return method(InstagramReelsExit.named(fragment.getClass(),FRAGMENT),"getChildFragmentManager","X.0dj",false).invoke(fragment);
    }
    private static Object find(Object manager,int id)throws ReflectiveOperationException {
        if(manager==null)return null;
        return method(InstagramReelsExit.named(manager.getClass(),"X.0dj"),"A0O",FRAGMENT,false,int.class).invoke(manager,id);
    }
    private static Object tag(Object manager,String tag)throws ReflectiveOperationException {
        if(manager==null)return null;
        return method(InstagramReelsExit.named(manager.getClass(),"X.0dj"),"A0Q",FRAGMENT,false,String.class).invoke(manager,tag);
    }
    /** Native current-child reads without repeating view/IME/lifecycle heuristics
     * at the authoritative fragment/tab Back boundaries. In particular 0jG's
     * MAIN overlay takes precedence over its retained swipeable CLIPS base.
     */
    private static Object content(Activity activity,Object first) {
        if(Instagram452.supports(activity))return Instagram452.content(activity,first);
        if(first==null)return null;
        try {
            Object fragment=first,parent=null;
            Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
            for(int depth=0;depth<12&&fragment!=null&&seen.add(fragment);depth++) {
                if(BackNavigation.invoke(fragment,"getActivity")!=activity)return null;
                if(parent!=null&&BackNavigation.invoke(fragment,"getParentFragment")!=parent)return null;
                String type=fragment.getClass().getName();Object next;
                if("X.0jG".equals(type)) {
                    Object manager=child(fragment);next=find(manager,MAIN);if(next==null)next=find(manager,BASE);
                }else if("X.3gY".equals(type)) {
                    next=method(fragment.getClass(),"A03",FRAGMENT,false).invoke(fragment);
                }else if("X.22c".equals(type)) {
                    Object selected=field(fragment.getClass(),"A05","X.eBE").get(fragment);
                    if(selected==null)return null;
                    int index=((Integer)method(InstagramReelsExit.named(selected.getClass(),"X.eBE"),"A04","int",false).invoke(selected)).intValue();
                    if(index<0||index>4)return null;
                    next=tag(child(fragment),"merged_feed_tab_"+index);
                }else if("X.9sY".equals(type)) {
                    // The actual getter reads the selected fN child and has a
                    // resumed-child fallback. Reuse it when present in the app.
                    try {next=method(fragment.getClass(),"A00",FRAGMENT,true,fragment.getClass()).invoke(null,fragment);}
                    catch(NoSuchMethodException unavailable) {
                        Object pager=field(fragment.getClass(),"A00","androidx.viewpager2.widget.ViewPager2").get(fragment);
                        if(pager==null)return null;
                        int index=field(pager.getClass(),"A00","int").getInt(pager);
                        if(index<0)return null;next=tag(child(fragment),"f"+index);
                    }
                }else return fragment;
                parent=fragment;fragment=next;
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable) { }
        return null;
    }
    static boolean current(Activity activity) {
        if(Instagram452.supports(activity))return fromProfile(Instagram452.content(activity,Instagram452.root(activity)));
        if(!InstagramBack.owns(activity))return false;
        Class<?> main=InstagramReelsExit.named(activity.getClass(),"com.instagram.mainactivity.InstagramMainActivity");
        if(main!=null)try {
            Object root=method(main,"A1e",FRAGMENT,false).invoke(activity);
            Object selected=content(activity,root);
            if(selected!=null)return fromProfile(selected);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable) { }
        return fromProfile(InstagramReels.foregroundFragment(activity));
    }
    static boolean preserve(Activity activity,Object receiver) {
        if(!InstagramBack.owns(activity))return false;
        if(NATIVE.get()==activity||Boolean.TRUE.equals(PREDICTIONS.get(activity))||RETURNS.containsKey(activity))return true;
        if(receiver!=null) {
            Object selected=content(activity,receiver);
            if(fromProfile(selected==null?receiver:selected))return true;
        }
        return current(activity);
    }
    /** Some native fragment callbacks have no outer platform scope. Preserve
     * their positive per-video origin for the rest of this main dispatch only.
     * The normal native pop consumes Back; this also protects another boundary
     * reached synchronously after it without imposing a timed delay on Back.
     */
    static void nativeReturn(final Activity activity,Object receiver) {
        if(!fromProfile(receiver))return;
        keepReturn(activity);
    }
    static void dispatcherReturn(Activity activity){if(current(activity))keepReturn(activity);}
    private static void keepReturn(final Activity activity) {
        final Object token=new Object();RETURNS.put(activity,token);
        MAIN_HANDLER.post(new Runnable(){public void run(){if(RETURNS.get(activity)==token)RETURNS.remove(activity);}});
    }
    static void beginNative(Activity activity){if(current(activity))NATIVE.set(activity);else NATIVE.remove();}
    static void endNative(){NATIVE.remove();}
    static void predictionStart(Activity activity){PREDICTIONS.remove(activity);if(current(activity))PREDICTIONS.put(activity,Boolean.TRUE);}
    static void predictionEnd(Activity activity){PREDICTIONS.remove(activity);}
}
