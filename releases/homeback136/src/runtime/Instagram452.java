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
                }else if("X.0A4c".equals(type)) {
                    // Native A0e selects a stable-ID 052y page before A07.
                    // Read the cached adapter/model, never initialize A07.
                    Object pager=field(fragment.getClass(),"A0B","androidx.viewpager2.widget.ViewPager2").get(fragment);
                    Object adapter=field(fragment.getClass(),"A0G","X.0Clp").get(fragment);
                    if(pager==null||!"androidx.viewpager2.widget.ViewPager2".equals(pager.getClass().getName())
                            ||adapter==null||!"X.0Clp".equals(adapter.getClass().getName())
                            ||field(adapter.getClass(),"A00",FRAGMENT).get(adapter)!=fragment)return null;
                    Method adapterGetter=pager.getClass().getDeclaredMethod("getAdapter");
                    if(!Modifier.isPublic(adapterGetter.getModifiers())||Modifier.isStatic(adapterGetter.getModifiers())
                            ||!"X.04b8".equals(adapterGetter.getReturnType().getName())||adapterGetter.invoke(pager)!=adapter)return null;
                    Object value=field(adapter.getClass(),"A02",java.util.List.class.getName()).get(adapter);
                    if(!(value instanceof java.util.List<?>))return null;
                    java.util.List<?> pages=new java.util.ArrayList<Object>((java.util.List<?>)value);
                    int index=field(pager.getClass(),"A00","int").getInt(pager);
                    if(pages.isEmpty()||pages.size()>5||index<0||index>=pages.size())return null;
                    java.util.Set<Integer> ids=new java.util.HashSet<Integer>();int id=-1;
                    for(int position=0;position<pages.size();position++) {
                        Object model=pages.get(position);if(model==null)return null;
                        int valueId=((Integer)method(InstagramReelsExit.named(model.getClass(),"X.0mBs"),"A05","int",false).invoke(model)).intValue();
                        if(valueId<0||valueId>4||!ids.add(valueId))return null;
                        if(position==index)id=valueId;
                    }
                    next=tag(child(fragment),"f"+id);
                    if(next==null||!"X.052y".equals(next.getClass().getName()))return null;
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
