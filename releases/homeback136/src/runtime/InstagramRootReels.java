package app.hiro.oneback.runtime;

import android.app.Activity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Positive identity for the selected root Reels tab, independent of a video's
 * analytics source. Contracts come from the supplied 447/452 APKs: the main
 * current-root getter delegates to the tab controller; tab-host Back prefers a
 * MAIN overlay and pops its child manager when A0L() is positive. Such a route
 * must continue natively. Only the selected managers are read; retained tabs,
 * View visibility, pending actions and lifecycle heuristics do not authorize or
 * veto the native root boundary. This class never performs navigation.
 */
final class InstagramRootReels {
    private InstagramRootReels() {}
    private static final String FRAGMENT="androidx.fragment.app.Fragment";
    private static final String ACTIVITY="androidx.fragment.app.FragmentActivity";
    private static final String MAIN_ACTIVITY="com.instagram.mainactivity.InstagramMainActivity";

    static boolean selectedClips(Activity activity) {
        if(!InstagramBack.owns(activity))return false;
        try {return controller(activity,Instagram452.supports(activity))!=null;}
        catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){return false;}
    }

    /** receiver is the actual Reels callback receiver, or null when a tab or
     * platform boundary requests the currently selected root route. */
    static boolean matches(Activity activity,Object receiver) {
        if(!InstagramBack.owns(activity)||activity.isDestroyed()||activity.isFinishing())return false;
        final boolean newer=Instagram452.supports(activity);
        final String managerType=newer?"X.00fk":"X.0dj";
        final int mainId=newer?0x7f0b22da:0x7f0b226f;
        final int baseId=newer?0x7f0b22e0:0x7f0b2275;
        try {
            Object navigation=controller(activity,newer);if(navigation==null)return false;
            Class<?> main=named(activity.getClass(),MAIN_ACTIVITY);
            Object fragment=method(main,newer?"A1f":"A1e",FRAGMENT).invoke(activity);
            if(fragment==null||method(navigation.getClass(),"A05",FRAGMENT).invoke(navigation)!=fragment)return false;
            Object manager=method(named(activity.getClass(),ACTIVITY),newer?"A1Z":"A1Y",managerType).invoke(activity);
            if(manager==null)return false;
            // A swipeable host is addressed by the selected tab's own native
            // controller. All other roots must occupy the main container.
            if(!(newer?"X.00VS":"X.0jG").equals(fragment.getClass().getName())
                    &&find(manager,managerType,mainId)!=fragment)return false;
            Object parent=null;
            Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
            for(int depth=0;depth<12&&fragment!=null&&seen.add(fragment);depth++) {
                Class<?> base=named(fragment.getClass(),FRAGMENT);if(base==null)return false;
                if(method(base,"getActivity",ACTIVITY).invoke(fragment)!=activity
                        ||method(base,"getParentFragment",FRAGMENT).invoke(fragment)!=parent
                        ||method(base,"getParentFragmentManager",managerType).invoke(fragment)!=manager
                        ||count(manager,managerType)!=0)return false;
                Object child=method(base,"getChildFragmentManager",managerType).invoke(fragment);
                if(child==null||count(child,managerType)!=0)return false;
                String type=fragment.getClass().getName();Object next;
                if((newer?"X.00VS":"X.0jG").equals(type)) {
                    // MAIN is a pushed screen even if its source says CLIPS_TAB
                    // or its back-stack entry has already been consumed.
                    if(find(child,managerType,mainId)!=null)return false;
                    next=find(child,managerType,baseId);
                }else if((newer?"X.02Wh":"X.3gY").equals(type)) {
                    next=find(child,managerType,mainId);
                }else if((newer?"X.052y":"X.22c").equals(type)) {
                    String modelType=newer?"X.0mBs":"X.eBE";
                    Object selected=field(fragment.getClass(),newer?"A06":"A05",modelType).get(fragment);
                    if(selected==null)return false;
                    int index=((Integer)method(named(selected.getClass(),modelType),newer?"A05":"A04","int").invoke(selected)).intValue();
                    if(index<0||index>4)return false;
                    next=tag(child,managerType,"merged_feed_tab_"+index);
                }else if((newer?"X.04Bv":"X.9sY").equals(type)) {
                    Object pager=field(fragment.getClass(),"A00","androidx.viewpager2.widget.ViewPager2").get(fragment);
                    if(pager==null)return false;
                    int index=field(pager.getClass(),"A00","int").getInt(pager);
                    if(index<0)return false;
                    next=tag(child,managerType,"f"+index);
                }else {
                    return named(fragment.getClass(),newer?"X.02Hr":"X.8Zv")!=null
                        &&(receiver==null||receiver==fragment);
                }
                parent=fragment;fragment=next;manager=child;
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){ }
        return false;
    }

    private static Object controller(Activity activity,boolean newer)throws ReflectiveOperationException {
        Class<?> main=named(activity.getClass(),MAIN_ACTIVITY);if(main==null)return null;
        String navigationType=newer?"X.00M2":"X.2xZ",controllerType=newer?"X.004K":"X.2yE";
        Object navigation=field(main,newer?"A08":"A07",navigationType).get(activity);
        if(navigation==null||!navigationType.equals(navigation.getClass().getName()))return null;
        Object controller=field(navigation.getClass(),"A02",controllerType).get(navigation);
        if(controller==null||!controllerType.equals(controller.getClass().getName())
                ||field(controller.getClass(),"A03",ACTIVITY).get(controller)!=activity)return null;
        String tabType=newer?"X.08kM":"X.2yl";
        Object tab=method(controller.getClass(),"A06",tabType).invoke(controller);
        if(!(tab instanceof Enum<?>)||!tabType.equals(tab.getClass().getName())
                ||!"CLIPS".equals(((Enum<?>)tab).name()))return null;
        return controller;
    }
    private static int count(Object manager,String type)throws ReflectiveOperationException {
        return ((Integer)method(named(manager.getClass(),type),"A0L","int").invoke(manager)).intValue();
    }
    private static Object find(Object manager,String type,int id)throws ReflectiveOperationException {
        return method(named(manager.getClass(),type),"A0O",FRAGMENT,int.class).invoke(manager,id);
    }
    private static Object tag(Object manager,String type,String value)throws ReflectiveOperationException {
        return method(named(manager.getClass(),type),"A0Q",FRAGMENT,String.class).invoke(manager,value);
    }
    private static Class<?> named(Class<?> type,String name) {
        return InstagramReelsExit.named(type,name);
    }
    private static Method method(Class<?> owner,String name,String returns,Class<?>... args)throws ReflectiveOperationException {
        if(owner==null||!Modifier.isPublic(owner.getModifiers()))throw new NoSuchMethodException(name);
        Method method=owner.getDeclaredMethod(name,args);int flags=method.getModifiers();
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
