package app.hiro.oneback.runtime;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** The app's selected-content getters identify Reels. Retained view counts,
 * tab-cache implementation, pending transactions and adapter bookkeeping are
 * not requirements for handling Back on that selected screen. */
final class InstagramReelsExit452 {
    private InstagramReelsExit452() {}
    private static final String FRAGMENT="androidx.fragment.app.Fragment";
    private static final String MANAGER="X.00fk";
    private static final int MAIN=0x7f0b22da,BASE=0x7f0b22e0;
    static Class<?> named(Class<?> type,String name) {
        for(Class<?> c=type;c!=null;c=c.getSuperclass())if(name.equals(c.getName()))return c;
        return null;
    }
    private static Method method(Class<?> owner,String name,String returns,boolean global,Class<?>... args)throws ReflectiveOperationException {
        if(owner==null||!Modifier.isPublic(owner.getModifiers()))throw new NoSuchMethodException(name);
        Method m=owner.getDeclaredMethod(name,args);int f=m.getModifiers();
        if(!Modifier.isPublic(f)||!Modifier.isFinal(f)||Modifier.isStatic(f)!=global
                ||!returns.equals(m.getReturnType().getName()))throw new NoSuchMethodException(name);
        return m;
    }
    private static Object childManager(Object f)throws ReflectiveOperationException {
        return method(named(f.getClass(),FRAGMENT),"getChildFragmentManager",MANAGER,false).invoke(f);
    }
    private static Object find(Object manager,int id)throws ReflectiveOperationException {
        if(manager==null)return null;
        return method(named(manager.getClass(),MANAGER),"A0O",FRAGMENT,false,int.class).invoke(manager,id);
    }
    /** A1f is the app's current root. The verified wrapper getters read its
     * current child; their optional lazy cache initialization does not navigate. */
    static InstagramCurrent.Result current(Activity a) {
        InstagramCurrent.Result r=new InstagramCurrent.Result(a);
        if(!InstagramBack.owns(a))return r;
        Class<?> main=named(a.getClass(),"com.instagram.mainactivity.InstagramMainActivity");
        if(main==null||named(a.getClass(),"com.instagram.base.activity.IgFragmentActivity")==null)return r;
        r.available=true;
        try {
            Object f=method(main,"A1f",FRAGMENT,false).invoke(a);r.selected=f;
            Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
            Object parent=null;
            for(int depth=0;depth<12&&f!=null&&seen.add(f);depth++) {
                if(BackNavigation.invoke(f,"getActivity")!=a
                        ||!Boolean.TRUE.equals(BackNavigation.invoke(f,"isVisible"))
                        ||!Boolean.TRUE.equals(BackNavigation.invoke(f,"isResumed")))return r;
                // Ensure a selected child's owner still belongs to this path.
                if(parent!=null&&BackNavigation.invoke(f,"getParentFragment")!=parent)return r;
                r.path.add(f);String type=f.getClass().getName();Object next;
                if(type.equals("X.00VS")) {
                    Object manager=childManager(f);next=find(manager,MAIN);if(next==null)next=find(manager,BASE);
                } else if(type.equals("X.02Wh")) {
                    next=method(f.getClass(),"A03",FRAGMENT,false).invoke(f);
                } else if(type.equals("X.052y")) {
                    next=method(f.getClass(),"A07",FRAGMENT,false).invoke(f);
                } else if(type.equals("X.04Bv")) {
                    next=method(f.getClass(),"A00",FRAGMENT,true,f.getClass()).invoke(null,f);
                } else {
                    Object v=BackNavigation.invoke(f,"getView");View decor=a.getWindow()==null?null:a.getWindow().peekDecorView();
                    if(!(v instanceof View)||decor==null||((View)v).getRootView()!=decor)return r;
                    Rect area=new Rect(),screen=new Rect();
                    if(!InstagramBack.shown((View)v,area)||!InstagramBack.shown(decor,screen)
                            ||!InstagramReels.large(area,screen))return r;
                    r.target=f;r.view=(View)v;r.nativeRecognized=true;r.stateClear=true;
                    r.kind=named(f.getClass(),"X.02Hr")!=null?InstagramCurrent.REELS:InstagramCurrent.OTHER;
                    return r;
                }
                parent=f;f=next;
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported) { }
        return r;
    }
}
