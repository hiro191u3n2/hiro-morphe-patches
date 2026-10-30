package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.Dialog;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Read-only mirror of v447 MainActivity's native guards, before an OS callback
 * can dispatch directly to AndroidX. Never invokes a consuming Back handler. */
final class InstagramActivityLayers {
    private InstagramActivityLayers() {}
    static final int UNKNOWN=0,CLEAR=1,BLOCKED=2;
    private static final String MAIN="com.instagram.mainactivity.InstagramMainActivity";
    private static final String IG="com.instagram.base.activity.IgFragmentActivity";
    private static final String FRAGMENT="androidx.fragment.app.Fragment";

    private static Class<?> named(Class<?> type,String name) {
        for(Class<?> c=type;c!=null;c=c.getSuperclass())if(name.equals(c.getName()))return c;
        return null;
    }
    private static Field field(Class<?> owner,String name,String type)throws ReflectiveOperationException {
        if(owner==null)throw new NoSuchFieldException("Missing native Activity guard owner");
        Field value=owner.getDeclaredField(name);
        if(!Modifier.isPublic(value.getModifiers())||Modifier.isStatic(value.getModifiers())
                ||!type.equals(value.getType().getName()))throw new NoSuchFieldException("Changed native Activity guard field");
        return value;
    }
    private static Method getter(Class<?> owner,String name,String result)throws ReflectiveOperationException {
        if(owner==null)throw new NoSuchMethodException("Missing native Activity guard owner");
        Method value=owner.getDeclaredMethod(name);
        if(!Modifier.isPublic(value.getModifiers())||!Modifier.isFinal(value.getModifiers())
                ||Modifier.isStatic(value.getModifiers())||!result.equals(value.getReturnType().getName()))
            throw new NoSuchMethodException("Changed native Activity guard getter");
        return value;
    }
    static boolean supports(Activity activity) {
        try {
            return activity!=null&&"com.instagram.android".equals(activity.getPackageName())
                    &&named(activity.getClass(),MAIN)!=null;
        }catch(RuntimeException|LinkageError unknown){return false;}
    }
    /** False includes unsupported hosts and unverified native shapes. Callers
     * must retain native Back in those cases; false is not proof of comments. */
    static boolean clear(Activity activity) {
        return inspect(activity)==CLEAR;
    }
    /** A known native layer is distinct from a changed or unavailable API. */
    static int inspect(Activity activity) {
        if(Instagram452.supports(activity))return InstagramActivityLayers452.inspect(activity);
        if(!supports(activity))return UNKNOWN;
        try {
            Class<?> main=named(activity.getClass(),MAIN),ig=named(main,IG);
            ClassLoader loader=main.getClassLoader();
            Class<?> fragment=Class.forName(FRAGMENT,false,loader);
            Method added=getter(fragment,"isAdded","boolean");

            Field modalField=field(main,"A08","X.3MM");Class<?> modalType=modalField.getType();
            Field modalReference=field(modalType,"A02",WeakReference.class.getName());
            Class<?> modalPage=Class.forName("X.9ld",false,loader);
            if(!Modifier.isFinal(modalType.getModifiers())||!Modifier.isFinal(modalPage.getModifiers())
                    ||!fragment.isAssignableFrom(modalPage))return UNKNOWN;
            Object modal=modalField.get(activity);
            if(modal!=null) {
                if(modal.getClass()!=modalType)return UNKNOWN;
                WeakReference<?> reference=(WeakReference<?>)modalReference.get(modal);
                Object page=reference==null?null:reference.get();
                if(page!=null) {
                    if(page.getClass()!=modalPage)return UNKNOWN;
                    if(Boolean.TRUE.equals(added.invoke(page)))return BLOCKED;
                }
            }

            Field layerField=field(ig,"A00","X.0wS");Class<?> layerBase=layerField.getType();
            Class<?> layerType=Class.forName("X.16z",false,loader);
            if(!Modifier.isFinal(layerType.getModifiers())||layerType.getSuperclass()!=layerBase)return UNKNOWN;
            Field layerEnabled=field(layerType,"A0U","boolean");
            // Actual A0M only reads A1Z's manager and finds the fragment by A1R.
            // A0p would dismiss that fragment and must never be called here.
            Method layerCurrent=getter(layerBase,"A0M",FRAGMENT);
            Object layer=layerField.get(activity);
            if(layer!=null) {
                if(layer.getClass()!=layerType)return UNKNOWN;
                if(layerEnabled.getBoolean(layer)&&layerCurrent.invoke(layer)!=null)return BLOCKED;
            }

            Class<?> owner=named(main,"androidx.fragment.app.FragmentActivity");
            Method managerGetter=getter(owner,"A1Y","X.0dj");
            Object manager=managerGetter.invoke(activity);if(manager==null)return UNKNOWN;
            Class<?> managerType=managerGetter.getReturnType();
            Field storeField=field(managerType,"A0V","X.0dw");
            Object store=storeField.get(manager);
            if(store==null||store.getClass()!=storeField.getType())return UNKNOWN;
            Field listField=field(storeField.getType(),"A01",ArrayList.class.getName());
            Object list=listField.get(store);if(!(list instanceof List<?>))return UNKNOWN;
            List<?> fragments;synchronized(list){fragments=new ArrayList<Object>((List<?>)list);}

            Class<?> dialogFragment=Class.forName("X.07d",false,loader);
            if(!fragment.isAssignableFrom(dialogFragment))return UNKNOWN;
            // RxR shadows A00 with an unrelated field. Resolve the Dialog on 07d.
            Field dialogField=field(dialogFragment,"A00",Dialog.class.getName());
            for(Object page:fragments) {
                if(page==null)continue;
                if(!fragment.isInstance(page))return UNKNOWN;
                if(dialogFragment.isInstance(page)) {
                    Dialog dialog=(Dialog)dialogField.get(page);
                    // The native Main loop checks RxR. Other actually showing
                    // DialogFragments also retain their own foreground Back.
                    if(dialog!=null&&dialog.isShowing())return BLOCKED;
                }
            }
            return CLEAR;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){return UNKNOWN;}
    }
}
