package app.hiro.oneback.runtime;

import android.app.Activity;
import android.os.Bundle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Audited TikTok 46.2.3 and 47.5.3 navigation contracts, checked when patching. */
final class TikTokHome {
    private TikTokHome() {}
    private static final String ABILITY="com.bytedance.tiktok.homepage.mainfragment.HomeTabAbility";
    private static final String IMPLEMENTATION="com.ss.android.ugc.aweme.main.assems.tabs.TabAbilityAssem";
    private static final String HOX="com.bytedance.hox.Hox";
    private static final String FIELD="communication_gen_field_com_bytedance_tiktok_homepage_mainfragment_HomeTabAbility";
    static final class Route {
        final Object ability;final Method select;
        final String selected;final boolean homeActive;
        Route(Object a,Method m,String s,boolean active){ability=a;select=m;selected=s;homeActive=active;}
        boolean home(){return "HOME".equals(selected)&&homeActive;}
    }
    private static Method method(Class<?> c,String name,Class<?> result,Class<?>... params)throws NoSuchMethodException {
        Method m=c.getMethod(name,params);
        if(m.getReturnType()!=result||!Modifier.isPublic(m.getModifiers())||Modifier.isStatic(m.getModifiers()))throw new NoSuchMethodException(name);
        return m;
    }
    static Route route(Activity a) {
        if(!TikTokScreen.main(a)||a.isFinishing()||a.isDestroyed())return null;
        try {
            ClassLoader loader=a.getClass().getClassLoader();
            Class<?> contract=Class.forName(ABILITY,false,loader),implementation=Class.forName(IMPLEMENTATION,false,loader),hoxType=Class.forName(HOX,false,loader);
            if(!contract.isInterface()||!contract.isAssignableFrom(implementation))return null;
            // R8 changed all four names in 47.5.3. These two complete contracts
            // are checked against the native DEX before the patch is accepted.
            // Never combine names from different builds or guess by descriptor.
            Method select,current,hox,active;
            try {
                select=method(contract,"IA0",Void.TYPE,Bundle.class,String.class);
                current=method(contract,"Nu2",String.class);
                hox=method(contract,"EH0",hoxType);
                active=method(hoxType,"i33",Boolean.TYPE,String.class);
            } catch(NoSuchMethodException not4753) {
                select=method(contract,"Lx0",Void.TYPE,Bundle.class,String.class);
                current=method(contract,"Mm2",String.class);
                hox=method(contract,"qE0",hoxType);
                active=method(hoxType,"fU2",Boolean.TYPE,String.class);
            }
            Object ability=null;
            try{Field f=a.getClass().getField(FIELD);if(f.getType()==contract&&!Modifier.isStatic(f.getModifiers()))ability=f.get(a);}catch(NoSuchFieldException absent){}
            if(ability==null)ability=method(a.getClass(),"getAbility",Object.class,Class.class).invoke(a,contract);
            if(ability==null||ability.getClass()!=implementation)return null;
            Object tree=hox.invoke(ability),selected=current.invoke(ability);
            if(tree==null||tree.getClass()!=hoxType||!(selected instanceof String)||((String)selected).isEmpty())return null;
            Object isActive=active.invoke(tree,"HOME");if(!(isActive instanceof Boolean))return null;
            return new Route(ability,select,(String)selected,(Boolean)isActive);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return null;}
    }
    /** Request the app's own Hox Home transition; acknowledgement is not proof of arrival. */
    static boolean select(Route route) {
        if(route==null||route.home())return false;
        try{route.select.invoke(route.ability,new Bundle(),"HOME");return true;}
        catch(ReflectiveOperationException|RuntimeException|LinkageError failed){return false;}
    }
    /** Compatibility entry point retained for existing patched APKs. */
    static boolean go(Activity a) {
        if(!TikTokScreen.owns(a)||!HomeScreen.inputReady(a))return false;
        TikTokBack.request(a);return true;
    }
}
