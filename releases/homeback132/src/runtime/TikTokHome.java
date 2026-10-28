package app.hiro.oneback.runtime;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Rect;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Audited TikTok 46.2.3 and 47.4.3 navigation contracts, checked when patching. */
final class TikTokHome {
    private TikTokHome() {}
    private static final String ABILITY="com.bytedance.tiktok.homepage.mainfragment.HomeTabAbility";
    private static final String IMPLEMENTATION="com.ss.android.ugc.aweme.main.assems.tabs.TabAbilityAssem";
    private static final String HOX="com.bytedance.hox.Hox";
    private static final String FIELD="communication_gen_field_com_bytedance_tiktok_homepage_mainfragment_HomeTabAbility";
    static final class Route {
        final Object ability;final Method select;
        final String selected;final boolean homeActive,native4743;
        Route(Object a,Method m,String s,boolean active,boolean modern){ability=a;select=m;selected=s;homeActive=active;native4743=modern;}
        boolean home(){return "HOME".equals(selected)&&homeActive;}
    }
    private static Method method(Class<?> c,String name,Class<?> result,Class<?>... params)throws NoSuchMethodException {
        Method m=c.getMethod(name,params);
        if(m.getReturnType()!=result||!Modifier.isPublic(m.getModifiers())||Modifier.isStatic(m.getModifiers()))throw new NoSuchMethodException(name);
        return m;
    }
    /** The exact 47.4.3 ABI is admitted by the patch before any hooks are installed.
     * Child Activities share this ABI even when they do not own a HomeTabAbility.
     * Their Back must remain a single native navigation, never an automatic trip Home. */
    static boolean supportsNativeBack(Activity a) {
        if(a==null||!"com.zhiliaoapp.musically".equals(a.getPackageName()))return false;
        try {
            ClassLoader loader=a.getClass().getClassLoader();
            Class<?> contract=Class.forName(ABILITY,false,loader),implementation=Class.forName(IMPLEMENTATION,false,loader),hox=Class.forName(HOX,false,loader);
            if(!contract.isInterface()||!contract.isAssignableFrom(implementation))return false;
            method(contract,"tB0",Void.TYPE,Bundle.class,String.class);
            method(contract,"tv2",String.class);method(contract,"jI0",hox);method(hox,"w33",Boolean.TYPE,String.class);
            return true;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return false;}
    }
    static Route route(Activity a) {
        if(!TikTokScreen.main(a)||a.isFinishing()||a.isDestroyed())return null;
        try {
            ClassLoader loader=a.getClass().getClassLoader();
            Class<?> contract=Class.forName(ABILITY,false,loader),implementation=Class.forName(IMPLEMENTATION,false,loader),hoxType=Class.forName(HOX,false,loader);
            if(!contract.isInterface()||!contract.isAssignableFrom(implementation))return null;
            // Complete contracts are checked against native DEX at patch time.
            // A retired 47.5.3 ABI is deliberately not recognized here.
            Method select,current,hox,active;boolean modern;
            try {
                select=method(contract,"tB0",Void.TYPE,Bundle.class,String.class);
                current=method(contract,"tv2",String.class);
                hox=method(contract,"jI0",hoxType);
                active=method(hoxType,"w33",Boolean.TYPE,String.class);
                if(!"com.zhiliaoapp.musically".equals(a.getPackageName()))return null;
                modern=true;
            } catch(NoSuchMethodException not4743) {
                select=method(contract,"Lx0",Void.TYPE,Bundle.class,String.class);
                current=method(contract,"Mm2",String.class);
                hox=method(contract,"qE0",hoxType);
                active=method(hoxType,"fU2",Boolean.TYPE,String.class);
                modern=false;
            }
            Object ability=null;
            try{Field f=a.getClass().getField(FIELD);if(f.getType()==contract&&!Modifier.isStatic(f.getModifiers()))ability=f.get(a);}catch(NoSuchFieldException absent){}
            if(ability==null)ability=method(a.getClass(),"getAbility",Object.class,Class.class).invoke(a,contract);
            if(ability==null||ability.getClass()!=implementation)return null;
            Object tree=hox.invoke(ability),selected=current.invoke(ability);
            if(tree==null||tree.getClass()!=hoxType||!(selected instanceof String)||((String)selected).isEmpty())return null;
            Object isActive=active.invoke(tree,"HOME");if(!(isActive instanceof Boolean))return null;
            return new Route(ability,select,(String)selected,(Boolean)isActive,modern);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return null;}
    }
    private static final String STATE="com.ss.android.ugc.aweme.homepage.api.interaction.ScrollSwitchStateManager";
    private static final String OUTER="com.ss.android.ugc.aweme.base.HomePageViewPagerAbility";
    private static final String OUTER_IMPL="com.ss.android.ugc.aweme.main.assems.ui.HomepageViewPagerAssem";
    private static final String OUTER_FIELD="communication_gen_field_com_ss_android_ugc_aweme_base_HomePageViewPagerAbility";
    /** A retained editor outside the visible Activity view cannot own this Back. */
    static boolean visibleEditor(Activity a,View editor) {
        try {
            View root=a.getWindow()==null?null:a.getWindow().peekDecorView();
            Rect screen=new Rect(),bounds=new Rect();
            if(root==null||editor==null||!root.isAttachedToWindow()
                    ||!TikTokScreen.shown(root,screen)||!TikTokScreen.shown(editor,bounds)
                    ||bounds.right<=screen.left||bounds.left>=screen.right
                    ||bounds.bottom<=screen.top||bounds.top>=screen.bottom)return false;
            for(int n=0;n<64&&editor!=null;n++) {
                if(!editor.isShown()||editor.getAlpha()<=.01f)return false;
                if(editor==root)return true;
                editor=editor.getParent() instanceof View?(View)editor.getParent():null;
            }
        }catch(RuntimeException|LinkageError ignored){}
        return false;
    }
    private static Object state(Activity a)throws ReflectiveOperationException {
        Class<?> type=Class.forName(STATE,false,a.getClass().getClassLoader());
        Field binding=a.getClass().getField("stateManager");
        if(binding.getType()!=type||Modifier.isStatic(binding.getModifiers()))return null;
        Object value=binding.get(a);return value!=null&&value.getClass()==type?value:null;
    }
    /** Hox HOME is the inner tab. The outer pager can still show an author's
     * profile, discovery, or camera while retaining that inner tab. */
    static boolean outerFeed(Activity a) {
        if(!TikTokScreen.main(a)||!supportsNativeBack(a))return false;
        try {
            Object state=state(a);
            return state!=null&&Boolean.TRUE.equals(method(state.getClass(),"s33",Boolean.TYPE,String.class).invoke(state,"page_feed"));
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return false;}
    }
    /** Preserve the native outer-page Back guards and transition used by
     * MainRootFragment.onKeyDown/onKeyBack. Never navigate an unknown page. */
    static boolean closeOuterPage(Activity a) {
        if(!TikTokScreen.main(a)||!supportsNativeBack(a))return false;
        try {
            Object state=state(a);if(state==null)return false;
            Object tag=method(state.getClass(),"i33",String.class).invoke(state);
            if(!"page_profile".equals(tag)&&!"page_discover".equals(tag)&&!"page_camera".equals(tag))return false;
            ClassLoader loader=a.getClass().getClassLoader();
            Class<?> page=Class.forName("com.ss.android.ugc.aweme.base.ui.CommonPageFragment",false,loader);
            Object fragment=method(state.getClass(),"j33",page,String.class).invoke(state,tag);
            if(fragment==null||!page.isInstance(fragment))return false;
            if(Boolean.TRUE.equals(method(page,"LJLI",Boolean.TYPE).invoke(fragment)))return true;
            Class<?> contract=Class.forName(OUTER,false,loader),implementation=Class.forName(OUTER_IMPL,false,loader);
            if(!contract.isInterface()||!contract.isAssignableFrom(implementation))return false;
            Field binding=a.getClass().getField(OUTER_FIELD);
            if(binding.getType()!=contract||Modifier.isStatic(binding.getModifiers()))return false;
            Object ability=binding.get(a);
            if(ability==null)ability=method(a.getClass(),"getAbility",Object.class,Class.class).invoke(a,contract);
            if(ability==null||ability.getClass()!=implementation)return false;
            return Boolean.TRUE.equals(method(contract,"Ir1",Boolean.TYPE,Boolean.class,Boolean.TYPE).invoke(ability,null,true));
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return false;}
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
