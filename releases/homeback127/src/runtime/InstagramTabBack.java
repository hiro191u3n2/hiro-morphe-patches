package app.hiro.oneback.runtime;

import android.app.Activity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Called only at the verified native tab-pop boundary, after the app's current
 * fragment declined Back. Native navigation, rather than UI heuristics, owns
 * this decision. A video opened from a profile retains its native return path.
 * No screen, layer or forwarding classifier runs. */
final class InstagramTabBack {
    private InstagramTabBack() {}
    static boolean matches(Activity activity,Object navigation,Object initialFragment) {
        if(navigation==null||initialFragment==null)return false;
        try {
            Class<?> type=navigation.getClass();boolean native452="X.004K".equals(type.getName());
            if(!(native452||"X.2yE".equals(type.getName()))||!Modifier.isPublic(type.getModifiers())
                    ||!Modifier.isFinal(type.getModifiers()))return false;
            Field owner=type.getDeclaredField("A03");int flags=owner.getModifiers();
            if(!Modifier.isPublic(flags)||!Modifier.isFinal(flags)||Modifier.isStatic(flags)
                    ||!"androidx.fragment.app.FragmentActivity".equals(owner.getType().getName())
                    ||owner.get(navigation)!=activity)return false;
            Method tabGetter=getter(type,"A06",native452?"X.08kM":"X.2yl");Object tab=tabGetter.invoke(navigation);
            if(tab==null||tab.getClass()!=tabGetter.getReturnType()||!(tab instanceof Enum<?>))return false;
            String name=((Enum<?>)tab).name();
            if(!"CLIPS".equals(name)) {
                android.util.Log.d("HiroBack123","Instagram native tab Back: other tab, native continuation");return false;
            }
            Method currentGetter=getter(type,"A05","androidx.fragment.app.Fragment");
            if(!currentGetter.getReturnType().isInstance(initialFragment)
                    ||currentGetter.invoke(navigation)!=initialFragment) {
                android.util.Log.d("HiroBack123","Instagram native tab Back: changed receiver, native continuation");return false;
            }
            if(InstagramProfileBack.preserve(activity,initialFragment)) {
                android.util.Log.d("HiroBack126","Instagram native tab Back: profile video, native continuation");return false;
            }
            return true;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable) {
            android.util.Log.d("HiroBack123","Instagram native tab Back: unavailable contract, native continuation");return false;
        }
    }
    private static Method getter(Class<?> owner,String name,String result)throws ReflectiveOperationException {
        Method method=owner.getDeclaredMethod(name);int flags=method.getModifiers();
        if(!Modifier.isPublic(flags)||!Modifier.isFinal(flags)||Modifier.isStatic(flags)
                ||!result.equals(method.getReturnType().getName()))throw new NoSuchMethodException(name);
        return method;
    }
}
