package app.hiro.oneback.runtime;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/** Public UI/navigation operations only; never launches an unknown activity or fakes a touch. */
final class BackNavigation {
    private BackNavigation() {}
    static Object invoke(Object object,String name) {
        if(object==null)return null;
        for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass()) {
            if(!Modifier.isPublic(c.getModifiers()))continue;
            try {
                Method m=c.getMethod(name);
                if(Modifier.isPublic(m.getDeclaringClass().getModifiers()))return m.invoke(object);
            }catch(NoSuchMethodException absent){}catch(ReflectiveOperationException|RuntimeException failed){return null;}
        }
        return null;
    }
    static boolean backControl(View v,Rect area,Rect screen) {
        // Match the semantic icon/ID, then resolve its actual clickable parent.
        // Do not click an unlabeled coordinate, avatar, share button or arbitrary header.
        if(area.centerY()>=screen.top+screen.height()*.5f||area.width()>screen.width()*.4f||area.height()>screen.height()*.18f)return false;
        String d=TikTokScreen.text(v.getContentDescription()),id=TikTokScreen.id(v);
        return TikTokScreen.label(d,"back","go back","navigate up","close","dismiss","戻る","前に戻る","閉じる")
            ||Arrays.asList("back","back_button","back_btn","btn_back","iv_back","ic_back","back_icon","back_arrow","title_bar_back","title_back","nav_back","navigation_back","action_bar_button_back","close_button","close_btn","btn_close","iv_close","sheet_close","comments_close","comment_close_button").contains(id);
    }
    static boolean click(View view) {
        if(view==null||!view.isShown()||view.getAlpha()<=.01f||!view.isEnabled()||!view.isClickable())return false;
        try{return view.performClick();}catch(RuntimeException|LinkageError failed){return false;}
    }
    static boolean popKnownStack(Activity activity) {
        // Only pop an existing entry. Never finish a root or change saved fragment state.
        Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
        return pop(invoke(activity,"getSupportFragmentManager"),seen,0)||pop(invoke(activity,"getFragmentManager"),seen,0);
    }
    private static boolean pop(Object manager,Set<Object> seen,int depth) {
        if(manager==null||depth>8||!seen.add(manager)||Boolean.TRUE.equals(invoke(manager,"isStateSaved")))return false;
        Object primary=invoke(manager,"getPrimaryNavigationFragment");
        if(primary!=null&&Boolean.TRUE.equals(invoke(primary,"isVisible"))
                &&pop(invoke(primary,"getChildFragmentManager"),seen,depth+1))return true;
        Object fragments=invoke(manager,"getFragments");
        if(fragments instanceof List<?>) {
            List<?> list=(List<?>)fragments;
            for(int i=list.size()-1;i>=0;i--) {
                Object f=list.get(i);
                if(f!=null&&f!=primary&&Boolean.TRUE.equals(invoke(f,"isVisible"))
                    &&pop(invoke(f,"getChildFragmentManager"),seen,depth+1))return true;
            }
        }
        Object count=invoke(manager,"getBackStackEntryCount");
        return count instanceof Number&&((Number)count).intValue()>0
            &&Boolean.TRUE.equals(invoke(manager,"popBackStackImmediate"));
    }
}
