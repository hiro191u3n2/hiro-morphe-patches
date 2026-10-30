package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The external profile may be pushed into Main's already open native modal.
 * Only that service's exact live 03CG and its selected registered profile are
 * content. Other layers retain their ordinary native Back priority. Nested
 * source-manager and tablet modal routes are deliberately not inferred here.
 */
final class InstagramExternalProfileModal {
    private InstagramExternalProfileModal() {}
    private static final String MAIN="com.instagram.mainactivity.InstagramMainActivity";
    private static final String FRAGMENT="androidx.fragment.app.Fragment",MANAGER="X.00fk";
    private static final int MAIN_CONTAINER=0x7f0b22da,MODAL_CONTENT=0x7f0b0aba;

    static InstagramCurrent.Result inspect(Activity activity,Object profile) {
        if(profile==null||!Instagram452.supports(activity))return null;
        try {
            Class<?> main=named(activity.getClass(),MAIN);
            Object service=field(main,"A09","X.0A4v",false).get(activity);
            if(!exact(service,"X.0A4v"))return null;
            if(field(service.getClass(),"A0A","boolean",false).getBoolean(service)
                    ||field(service.getClass(),"A04","boolean",false).getBoolean(service)
                    ||field(service.getClass(),"A0B","boolean",false).getBoolean(service))return null;
            Object reference=field(service.getClass(),"A06",WeakReference.class.getName(),false).get(service);
            if(!(reference instanceof WeakReference<?>))return null;
            Object modal=((WeakReference<?>)reference).get();
            if(!exact(modal,"X.03CG")||!"X.0CTn".equals(modal.getClass().getSuperclass().getName()))return null;
            if(field(modal.getClass(),"A08","boolean",false).getBoolean(modal)
                    ||field(modal.getClass(),"A07","boolean",false).getBoolean(modal))return null;
            Object arguments=call(modal,FRAGMENT,"getArguments",Bundle.class.getName());
            if(arguments!=null&&(!(arguments instanceof Bundle)
                    ||((Bundle)arguments).getBoolean("MODAL_FRAGMENT_ARG_ROUTE_THROUGH_SOURCE_FM")))return null;
            Object root=call(activity,"androidx.fragment.app.FragmentActivity","A1Z",MANAGER);
            if(root==null||!node(modal,activity,null,root,true))return null;
            int host=field(service.getClass(),"A05","int",false).getInt(service);
            if(host<=0||field(named(modal.getClass(),FRAGMENT),"mFragmentId","int",false).getInt(modal)!=host
                    ||find(root,host)!=modal)return null;
            Object children=call(modal,FRAGMENT,"getChildFragmentManager",MANAGER);
            if(find(children,MODAL_CONTENT)!=profile||!node(profile,activity,modal,children,true)
                    ||field(named(profile.getClass(),FRAGMENT),"mFragmentId","int",false).getInt(profile)!=MODAL_CONTENT)return null;
            if(activity.isDestroyed()||activity.isFinishing()||activity.getWindow()==null)return null;
            View decor=activity.getWindow().peekDecorView();
            Object modalValue=call(modal,FRAGMENT,"getView",View.class.getName());
            Object profileValue=call(profile,FRAGMENT,"getView",View.class.getName());
            if(!(modalValue instanceof View)||!(profileValue instanceof View)||decor==null)return null;
            View modalView=(View)modalValue,profileView=(View)profileValue;Rect screen=new Rect(),bounds=new Rect();
            if(modalView==decor||profileView==modalView||modalView.getRootView()!=decor||profileView.getRootView()!=decor
                    ||!ancestor(modalView,profileView)||!InstagramBack.shown(modalView,bounds)
                    ||!InstagramBack.shown(decor,screen)||!InstagramBack.shown(profileView,bounds)
                    ||!InstagramReels.large(bounds,screen))return null;

            InstagramCurrent.Frame rootFrame=frame(root,null,modal,host);
            InstagramCurrent.Frame childFrame=frame(children,modal,profile,MODAL_CONTENT);
            if(!clear(rootFrame)||!clear(childFrame)||!contains(rootFrame.added,modal)
                    ||!InstagramExternalProfile.managerClear(children,profile,false))return null;
            // A0B natively dismisses an auto-close modal after a consumed child
            // Back at depth zero. Do not override that separate configuration.
            if(childFrame.backStackCount==0&&field(service.getClass(),"A0C","boolean",false).getBoolean(service))return null;

            // Main's ordinary selected tab remains behind the separate modal.
            // Native modal creation can either leave it RESUMED or cap it to
            // STARTED. Its exact native identity, not its lifecycle alone,
            // proves that this one root is background content.
            // Main.A1f's native tab lookup calls each wrapper's lazy getValue.
            // Prove every such value is already cached before invoking it.
            for(Object page:rootFrame.added)if(page!=null&&"X.00VS".equals(page.getClass().getName())
                    &&cachedTabKey(page)==null)return null;
            Object navigation=field(main,"A08","X.00M2",false).get(activity);
            if(!exact(navigation,"X.00M2"))return null;
            Object controller=field(navigation.getClass(),"A02","X.004K",false).get(navigation);
            if(!exact(controller,"X.004K")
                    ||field(controller.getClass(),"A03","androidx.fragment.app.FragmentActivity",true).get(controller)!=activity)return null;
            Object selected=call(activity,MAIN,"A1f",FRAGMENT);
            if(selected==null||selected==modal||selected==profile
                    ||call(controller,"X.004K","A05",FRAGMENT)!=selected
                    ||!contains(rootFrame.added,selected)||!node(selected,activity,null,root,false))return null;
            Object tab=call(controller,"X.004K","A06","X.08kM");
            if(tab==null||!"X.08kM".equals(tab.getClass().getName())||!tab.getClass().isEnum())return null;
            String selectedKey=(String)field(tab.getClass(),"A03",String.class.getName(),true).get(tab);
            Set<String> keys=tabKeys(tab);
            if(selectedKey==null||!keys.contains(selectedKey))return null;
            if("X.00VS".equals(selected.getClass().getName())) {
                if(!selectedKey.equals(cachedTabKey(selected)))return null;
            }else if(find(root,MAIN_CONTAINER)!=selected)return null;

            InstagramCurrent.Result result=new InstagramCurrent.Result(activity);
            result.available=true;result.nativeRecognized=true;result.stateClear=true;
            // This adapter never grants generic home/Reels exit permission.
            result.kind=InstagramCurrent.OTHER;result.selected=modal;result.target=profile;
            result.selectedTab=tab;result.view=profileView;result.path.add(modal);result.path.add(profile);
            result.frames.add(rootFrame);result.frames.add(childFrame);
            for(Object page:rootFrame.added) {
                if(page==null||page==modal)continue;
                if(named(page.getClass(),FRAGMENT)==null||"X.03CG".equals(page.getClass().getName())||showingDialog(page))return null;
                boolean background=page==selected;
                if(!background&&"X.00VS".equals(page.getClass().getName())
                        &&node(page,activity,null,root,false)
                        &&Boolean.FALSE.equals(call(page,FRAGMENT,"isResumed","boolean"))) {
                    String key=cachedTabKey(page);
                    background=key!=null&&keys.contains(key)&&!selectedKey.equals(key);
                }
                Object value=call(page,FRAGMENT,"getView",View.class.getName());
                if(background) {
                    if(value==null)continue;
                    if(!(value instanceof View))return null;
                    View view=(View)value;
                    if(view==decor||view.getRootView()!=decor||ancestor(view,modalView)||ancestor(modalView,view))return null;
                    result.backgroundRoots.add(view);
                } else if(Boolean.TRUE.equals(call(page,FRAGMENT,"isVisible","boolean"))
                        &&(!(value instanceof View)||InstagramBack.shown((View)value,new Rect())))return null;
            }
            return result;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){return null;}
    }
    private static boolean exact(Object value,String name) {
        return value!=null&&name.equals(value.getClass().getName())&&Modifier.isFinal(value.getClass().getModifiers());
    }
    private static boolean node(Object fragment,Activity activity,Object parent,Object manager,boolean foreground)throws ReflectiveOperationException {
        return Boolean.TRUE.equals(call(fragment,FRAGMENT,"isAdded","boolean"))
            &&call(fragment,FRAGMENT,"getActivity","androidx.fragment.app.FragmentActivity")==activity
            &&call(fragment,FRAGMENT,"getParentFragment",FRAGMENT)==parent
            &&call(fragment,FRAGMENT,"getParentFragmentManager",MANAGER)==manager
            &&(!foreground||(Boolean.TRUE.equals(call(fragment,FRAGMENT,"isVisible","boolean"))
                &&Boolean.TRUE.equals(call(fragment,FRAGMENT,"isResumed","boolean"))));
    }
    private static InstagramCurrent.Frame frame(Object manager,Object owner,Object selected,int id)throws ReflectiveOperationException {
        Class<?> type=named(manager.getClass(),MANAGER);
        int count=((Integer)method(type,"A0L","int").invoke(manager)).intValue();
        boolean saved=Boolean.TRUE.equals(method(type,"A15","boolean").invoke(manager));
        boolean executing=field(type,"mExecutingActions","boolean",false).getBoolean(manager);
        Object pending=field(type,"A0W",ArrayList.class.getName(),true).get(manager);
        if(!(pending instanceof ArrayList<?>))throw new IllegalStateException("Changed modal pending queue");
        boolean queued;synchronized(pending){queued=!((ArrayList<?>)pending).isEmpty();}
        Object store=field(type,"A0V","X.00fz",true).get(manager);
        if(!exact(store,"X.00fz"))throw new IllegalStateException("Changed modal fragment store");
        Object raw=field(store.getClass(),"A01",ArrayList.class.getName(),true).get(store);
        if(!(raw instanceof ArrayList<?>))throw new IllegalStateException("Changed modal fragment inventory");
        List<?> pages;synchronized(raw){pages=new ArrayList<Object>((ArrayList<?>)raw);}
        return new InstagramCurrent.Frame(manager,owner,selected,id,null,count,saved,executing,queued,pages,-1);
    }
    private static boolean clear(InstagramCurrent.Frame frame) {
        return frame.backStackCount>=0&&!frame.stateSaved&&!frame.executing&&!frame.pending;
    }
    private static boolean showingDialog(Object page)throws ReflectiveOperationException {
        Class<?> type=named(page.getClass(),"X.007r");
        if(type==null)return false;
        Object value=field(type,"A00",Dialog.class.getName(),false).get(page);
        return value instanceof Dialog&&((Dialog)value).isShowing();
    }
    private static Set<String> tabKeys(Object tab)throws ReflectiveOperationException {
        Set<String> values=new HashSet<String>();Class<?> type=tab.getClass();
        Field key=field(type,"A03",String.class.getName(),true);
        for(Field constant:type.getDeclaredFields())if(constant.isEnumConstant()) {
            if(!Modifier.isPublic(constant.getModifiers())||!Modifier.isStatic(constant.getModifiers())
                    ||!Modifier.isFinal(constant.getModifiers())||constant.getType()!=type)throw new NoSuchFieldException("Changed native tab");
            Object value=constant.get(null);if(value==null||value.getClass()!=type)throw new NoSuchFieldException("Missing native tab");
            Object text=key.get(value);if(text instanceof String&&((String)text).length()>0)values.add((String)text);
        }
        return values;
    }
    private static String cachedTabKey(Object wrapper)throws ReflectiveOperationException {
        Object cache=field(wrapper.getClass(),"A07","X.0JWL",true).get(wrapper);
        if(!exact(cache,"X.0072"))return null;
        Object value=field(cache.getClass(),"A00",Object.class.getName(),false).get(cache);
        return value instanceof String?(String)value:null;
    }
    private static boolean contains(List<?> pages,Object value){for(Object page:pages)if(page==value)return true;return false;}
    private static boolean ancestor(View parent,View child) {
        for(int depth=0;child!=null&&depth<128;depth++,child=child.getParent() instanceof View?(View)child.getParent():null)
            if(parent==child)return true;
        return false;
    }
    private static Object find(Object manager,int id)throws ReflectiveOperationException {
        return method(named(manager.getClass(),MANAGER),"A0O",FRAGMENT,int.class).invoke(manager,id);
    }
    private static Object call(Object value,String owner,String name,String result)throws ReflectiveOperationException {
        return method(named(value.getClass(),owner),name,result).invoke(value);
    }
    private static Class<?> named(Class<?> type,String name) {
        for(Class<?> current=type;current!=null;current=current.getSuperclass())if(name.equals(current.getName()))return current;
        return null;
    }
    private static Field field(Class<?> owner,String name,String type,boolean last)throws ReflectiveOperationException {
        if(owner==null||!Modifier.isPublic(owner.getModifiers()))throw new NoSuchFieldException(name);
        Field field=owner.getDeclaredField(name);int flags=field.getModifiers();
        if(!Modifier.isPublic(flags)||Modifier.isStatic(flags)||(last&&!Modifier.isFinal(flags))
                ||!type.equals(field.getType().getName()))throw new NoSuchFieldException(name);
        return field;
    }
    private static Method method(Class<?> owner,String name,String result,Class<?>... args)throws ReflectiveOperationException {
        if(owner==null||!Modifier.isPublic(owner.getModifiers()))throw new NoSuchMethodException(name);
        Method method=owner.getDeclaredMethod(name,args);int flags=method.getModifiers();
        if(!Modifier.isPublic(flags)||Modifier.isStatic(flags)||(!name.equals("getView")&&!Modifier.isFinal(flags))
                ||!result.equals(method.getReturnType().getName()))throw new NoSuchMethodException(name);
        return method;
    }
}
