package app.hiro.oneback.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;

/** Read-only mirror of the two guards preceding the verified v447 Reels Back
 * boundary. The main Activity hook must not skip an internal Reels popup or
 * layer before its native handler gets a chance to close it. */
final class InstagramNativeLayers {
    private InstagramNativeLayers() {}

    private static Class<?> named(Class<?> type,String name) {
        for(Class<?> c=type;c!=null;c=c.getSuperclass())if(name.equals(c.getName()))return c;
        return null;
    }
    private static Field field(Class<?> owner,String name,String type)throws ReflectiveOperationException {
        Field f=owner.getDeclaredField(name);
        if(!Modifier.isPublic(f.getModifiers())||Modifier.isStatic(f.getModifiers())
                ||!type.equals(f.getType().getName()))throw new NoSuchFieldException("Unverified native Reels field");
        return f;
    }
    /** Only the verified native Reels class has these private-to-app states.
     * A changed field shape on that class must stay native; unrelated historical
     * fragment types continue through their existing view/fragment checks. */
    static boolean blocksBeforeNative(Object fragment) {
        return fragment!=null&&named(fragment.getClass(),"X.8Zv")!=null&&!clear(fragment);
    }
    static boolean clear(Object fragment) {
        if(fragment==null)return false;
        try {
            Class<?> reels=named(fragment.getClass(),"X.8Zv");if(reels==null)return false;
            Field popupField=field(reels,"A1Y","X.21x"),layerField=field(reels,"A0L","X.0wS");
            Field popupOpen=field(popupField.getType(),"A05","boolean");
            Class<?> layerType=Class.forName("X.16z",false,reels.getClassLoader());
            if(!Modifier.isFinal(layerType.getModifiers())||layerType.getSuperclass()!=layerField.getType())return false;
            Field layerOpen=field(layerType,"A15","boolean");
            Object popup=popupField.get(fragment),layer=layerField.get(fragment);
            if(popup!=null&&(popup.getClass()!=popupField.getType()||popupOpen.getBoolean(popup)))return false;
            if(layer==null)return true;
            if(layer.getClass()!=layerType)return false;
            if(!layerOpen.getBoolean(layer))return true;
            // A15 only enables this Back branch. A0p returns false unless A0U
            // is enabled AND A0M identifies an actual current sheet. Treating
            // A15 alone as an open layer permanently disabled clear Reels.
            Field enabled=field(layerType,"A0U","boolean");
            if(!enabled.getBoolean(layer))return true;
            Method current=layerField.getType().getDeclaredMethod("A0M");
            if(!Modifier.isPublic(current.getModifiers())||!Modifier.isFinal(current.getModifiers())
                    ||Modifier.isStatic(current.getModifiers())
                    ||!"androidx.fragment.app.Fragment".equals(current.getReturnType().getName()))return false;
            return current.invoke(layer)==null;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){return false;}
    }
}
