package app.hiro.oneback.runtime;

import android.app.Activity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Read-only contracts independently extracted from Instagram 452.1.0.53.64.
 * The app's selected-content getters determine the route. Unknown shapes retain
 * native Back. No navigation methods or uninitialized lazy values are invoked. */
final class Instagram452 {
    private Instagram452() {}
    private static final String FRAGMENT="androidx.fragment.app.Fragment",MANAGER="X.00fk";
    private static final int MAIN=0x7f0b22da,BASE=0x7f0b22e0;
    static boolean supports(Activity activity) {
        if(!InstagramBack.owns(activity))return false;
        try {
            Class<?> main=InstagramReelsExit.named(activity.getClass(),"com.instagram.mainactivity.InstagramMainActivity");
            return main!=null&&method(main,"A1f",FRAGMENT,false)!=null
                &&field(main,"A08","X.00M2")!=null;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){return false;}
    }
    static boolean reels(Object fragment) {
        return fragment!=null&&InstagramReelsExit.named(fragment.getClass(),"X.02Hr")!=null;
    }
    static Object root(Activity activity) {
        try {return method(InstagramReelsExit.named(activity.getClass(),"com.instagram.mainactivity.InstagramMainActivity"),"A1f",FRAGMENT,false).invoke(activity);}
        catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){return null;}
    }
    /** 452 adds an additional native Reels guard before its exit boundary.
     * Its original code must run; neither OS prediction nor AndroidX pre-dispatch
     * grants an early exit. The exact post-guard and tab-pop hooks handle exit. */
    static boolean deferReels(Activity activity) {
        if(!supports(activity))return false;
        Object selected=content(activity,root(activity));
        // An unresolved selector must not fall through to a getter that may
        // initialize its cache and newly authorize a pre-guard Reels exit.
        return selected==null||reels(selected);
    }
    static Object content(Activity activity,Object first) {
        if(first==null)return null;
        try {
            Object fragment=first,parent=null;
            Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
            for(int depth=0;depth<12&&fragment!=null&&seen.add(fragment);depth++) {
                if(BackNavigation.invoke(fragment,"getActivity")!=activity)return null;
                if(parent!=null&&BackNavigation.invoke(fragment,"getParentFragment")!=parent)return null;
                String type=fragment.getClass().getName();Object next;
                if("X.00VS".equals(type)) {
                    Object manager=child(fragment);next=find(manager,MAIN);if(next==null)next=find(manager,BASE);
                }else if("X.02Wh".equals(type)) {
                    next=method(fragment.getClass(),"A03",FRAGMENT,false).invoke(fragment);
                }else if("X.052y".equals(type)) {
                    Object selected=field(fragment.getClass(),"A06","X.0mBs").get(fragment);
                    if(selected==null)return null;
                    int index=((Integer)method(InstagramReelsExit.named(selected.getClass(),"X.0mBs"),"A05","int",false).invoke(selected)).intValue();
                    if(index<0||index>4)return null;
                    next=tag(child(fragment),"merged_feed_tab_"+index);
                }else if("X.04Bv".equals(type)) {
                    next=method(fragment.getClass(),"A00",FRAGMENT,true,fragment.getClass()).invoke(null,fragment);
                }else return fragment;
                parent=fragment;fragment=next;
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){ }
        return null;
    }
    private static Object child(Object fragment)throws ReflectiveOperationException {
        return method(InstagramReelsExit.named(fragment.getClass(),FRAGMENT),"getChildFragmentManager",MANAGER,false).invoke(fragment);
    }
    private static Object find(Object manager,int id)throws ReflectiveOperationException {
        if(manager==null)return null;
        return method(InstagramReelsExit.named(manager.getClass(),MANAGER),"A0O",FRAGMENT,false,int.class).invoke(manager,id);
    }
    private static Object tag(Object manager,String tag)throws ReflectiveOperationException {
        if(manager==null)return null;
        return method(InstagramReelsExit.named(manager.getClass(),MANAGER),"A0Q",FRAGMENT,false,String.class).invoke(manager,tag);
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
}
