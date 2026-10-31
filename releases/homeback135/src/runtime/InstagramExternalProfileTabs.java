package app.hiro.oneback.runtime;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** The profile's ordinary grid is a child Fragment in a native ViewPager.
 * Only the exact selected, attached native tab is content. A different visible
 * child, a nested child stack, a pending transaction, or a moving pager vetoes
 * exit. The native getters used below are read-only and verified at patch time.
 */
final class InstagramExternalProfileTabs {
    private InstagramExternalProfileTabs() {}
    private static final String PROFILE="com.instagram.profile.fragment.UserDetailFragment";
    private static final String CONTROLLER="com.instagram.profile.fragment.UserDetailTabController";
    private static final String FRAGMENT="androidx.fragment.app.Fragment",MANAGER="X.00fk";
    private static final String PAGER="androidx.viewpager.widget.ViewPager";
    private static final String NESTABLE="com.instagram.ui.widget.nestablescrollingview.NestableViewPager";

    static boolean clear(Object profile,Object children) {
        try {
            if(profile==null||!PROFILE.equals(profile.getClass().getName())||children==null)return false;
            Class<?> manager=InstagramReelsExit.named(children.getClass(),MANAGER);
            Object store=field(manager,"A0V","X.00fz").get(children);
            if(store==null||!"X.00fz".equals(store.getClass().getName()))return false;
            Object raw=field(store.getClass(),"A01",ArrayList.class.getName()).get(store);
            if(!(raw instanceof ArrayList<?>))return false;
            // No tab exists yet during a verified empty/loading profile.
            synchronized(raw) {
                if(((ArrayList<?>)raw).isEmpty())return InstagramExternalProfile.managerClear(children,null,true);
            }
            Object controller=field(profile.getClass(),"A0b",CONTROLLER).get(profile);
            if(!exact(controller,CONTROLLER))return false;
            Object adapter=field(controller.getClass(),"A0A","X.0EIp").get(controller);
            Object holder=field(controller.getClass(),"viewHolder","X.0EdW").get(controller);
            if(!exact(adapter,"X.0EIp")||!exact(holder,"X.0EdW"))return false;
            Object pager=field(holder.getClass(),"A0F",NESTABLE).get(holder);
            if(!exact(pager,NESTABLE)||!(pager instanceof ViewGroup))return false;
            Class<?> pagerBase=InstagramReelsExit.named(pager.getClass(),PAGER);
            if(field(pagerBase,"A0B","X.00ft").get(pager)!=adapter
                    ||field(pagerBase,"A0W","int").getInt(pager)!=0)return false;
            int index=field(pagerBase,"A02","int").getInt(pager);
            Object pages=field(adapter.getClass(),"A01",List.class.getName()).get(adapter);
            if(!(pages instanceof List<?>)||index<0||index>=((List<?>)pages).size())return false;
            Class<?> adapterBase=InstagramReelsExit.named(adapter.getClass(),"X.00fy");
            if(field(adapterBase,"A06",MANAGER).get(adapter)!=children)return false;
            Object selected=method(controller.getClass(),"A0D","X.0NgK",true).invoke(controller);
            if(selected==null||method(adapter.getClass(),"A0J","X.0NgK",true,int.class).invoke(adapter,index)!=selected
                    ||field(adapterBase,"A01",FRAGMENT).get(adapter)!=selected)return false;
            if(InstagramReelsExit.named(selected.getClass(),FRAGMENT)==null
                    ||call(selected,"getParentFragment",FRAGMENT)!=profile
                    ||call(selected,"getParentFragmentManager",MANAGER)!=children
                    ||call(selected,"getActivity","androidx.fragment.app.FragmentActivity")
                        !=call(profile,"getActivity","androidx.fragment.app.FragmentActivity")
                    ||!Boolean.TRUE.equals(call(selected,"isAdded","boolean"))
                    ||!Boolean.TRUE.equals(call(selected,"isVisible","boolean"))
                    ||!Boolean.TRUE.equals(call(selected,"isResumed","boolean")))return false;
            Object selectedView=call(selected,"getView",View.class.getName());
            Object profileView=call(profile,"getView",View.class.getName());
            if(!(selectedView instanceof View)||!(profileView instanceof View)
                    ||!inside((View)pager,(View)profileView)||!inside((View)selectedView,(View)pager))return false;
            // Offscreen retained sibling tabs may remain attached. Existing
            // managerClear rejects every other visible child and every dialog.
            return InstagramExternalProfile.managerClear(children,selected,true)
                &&InstagramExternalProfile.managerClear(call(selected,"getChildFragmentManager",MANAGER),null,true);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){return false;}
    }
    private static boolean inside(View child,View ancestor) {
        for(int depth=0;child!=null&&depth<64;depth++) {
            if(child==ancestor)return true;
            ViewParent parent=child.getParent();child=parent instanceof View?(View)parent:null;
        }
        return false;
    }
    private static boolean exact(Object value,String name){return value!=null&&name.equals(value.getClass().getName());}
    private static Field field(Class<?> owner,String name,String type)throws ReflectiveOperationException {
        if(owner==null)throw new NoSuchFieldException(name);Field field=owner.getDeclaredField(name);
        if(!Modifier.isPublic(field.getModifiers())||Modifier.isStatic(field.getModifiers())
                ||!type.equals(field.getType().getName()))throw new NoSuchFieldException(name);
        return field;
    }
    private static Method method(Class<?> owner,String name,String result,boolean requireFinal,Class<?>... args)throws ReflectiveOperationException {
        if(owner==null)throw new NoSuchMethodException(name);Method method=owner.getDeclaredMethod(name,args);
        if(!Modifier.isPublic(method.getModifiers())||Modifier.isStatic(method.getModifiers())
                ||requireFinal&&!Modifier.isFinal(method.getModifiers())||!result.equals(method.getReturnType().getName()))
            throw new NoSuchMethodException(name);
        return method;
    }
    private static Object call(Object target,String name,String result)throws ReflectiveOperationException {
        return method(InstagramReelsExit.named(target.getClass(),FRAGMENT),name,result,!name.equals("getView")).invoke(target);
    }
}
