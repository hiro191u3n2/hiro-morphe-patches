package app.hiro.oneback.runtime;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Reels content identity is independent of the sheet/Activity used to host it.
 * Singular reel_viewer is Stories and must never be treated as a Reels exit target.
 */
final class InstagramReels {
    private InstagramReels() {}
    static boolean viewer(String value) {
        String s=TikTokScreen.text(value);
        if(TikTokScreen.contains(s,"comment","profile","editor","camera","preview","button","icon","story"))return false;
        return Arrays.asList("clips_viewer","clips_viewer_view_pager","clips_viewer_pager","clips_viewer_container",
            "clips_viewer_root","clips_viewer_layout","clips_viewer_recycler_view","clips_viewer_viewpager",
            "clips_swipe_refresh_container","clips_viewer_fragment_container",
            "reels_viewer","reels_viewer_container","reels_viewer_root",
            "instagram.features.clips.viewer.ui.clipsswiperefreshlayout",
            "instagram.features.clips.viewer.ui.homecomingswiperefreshlayout").contains(s)
            ||(s.startsWith("com.instagram.clips.")&&s.contains("viewer")
                &&TikTokScreen.contains(s,"viewpager","view_pager","container","layout","root"));
    }
    static boolean large(Rect r,Rect screen) {
        return r.width()>screen.width()*.6f&&r.height()>screen.height()*.55f;
    }
    static boolean viewer(View v,Rect screen) {
        Rect r=new Rect();
        return InstagramBack.shown(v,r)&&large(r,screen)
            &&(viewer(TikTokScreen.id(v))||viewerClass(v.getClass()));
    }
    private static boolean viewerClass(Class<?> type) {
        for(Class<?> c=type;c!=null;c=c.getSuperclass())if(viewer(c.getName()))return true;
        return false;
    }
    static boolean wrapper(View root,Rect screen) {
        // Exempt ONLY the ancestor of a positively identified full-size viewer.
        // Children are still inspected: a comments/share sheet inside it is not exempt.
        Rect bounds=new Rect();if(!InstagramBack.shown(root,bounds)||!large(bounds,screen))return false;
        ArrayDeque<View> q=new ArrayDeque<View>();q.add(root);int count=0;
        while(!q.isEmpty()&&++count<=600){
            View v=q.removeFirst();Rect r=new Rect();if(!InstagramBack.shown(v,r))continue;
            if(viewer(v,screen))return true;
            if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;if(g.getChildCount()>600-count)return false;
                for(int i=0;i<g.getChildCount();i++)q.add(g.getChildAt(i));}
        }
        return false;
    }
    static boolean route(String name,String module) {
        if(TikTokScreen.contains(name,"storyviewer","reelviewer","editor","camera","comment","profile"))return false;
        return (name.startsWith("com.instagram.clips.")&&name.contains("viewer"))
            ||Arrays.asList("clips_viewer","clips_feed","clips_tab","clips_viewer_tray","clips_video_viewer","reels_viewer","reels_feed","reels_tab").contains(module)
            // v447 viewer modules include the entry source; the former exact list
            // missed clips_viewer_clips_tab and clips_viewer_homecoming_fyp.
            ||(module.startsWith("clips_viewer_")&&!TikTokScreen.contains(module,
                "bottom_sheet","bottomsheet","comment_sheet","share_sheet","dialog","editor","camera","settings","preview"));
    }
    static boolean route(Object fragment) {
        if(fragment==null)return false;
        Object m=BackNavigation.invoke(fragment,"getModuleName");
        return route(fragment.getClass().getName().toLowerCase(Locale.ROOT),m instanceof String?TikTokScreen.text((String)m):"");
    }
    /** Read-only v447 adapter. AndroidX APIs are renamed in this APK, so calling
     * getSupportFragmentManager/getFragments by name silently returned null.
     * The complete declaring/return/field types are checked before reading;
     * there is no guessed navigation method and no mutation of app state.
     */
    static Object supportManager(Activity activity) {
        Object standard=BackNavigation.invoke(activity,"getSupportFragmentManager");
        if(standard!=null)return standard;
        if(!InstagramBack.owns(activity))return null;
        try {
            Class<?> owner=named(activity.getClass(),"androidx.fragment.app.FragmentActivity");
            if(owner==null)return null;
            Method getter=owner.getDeclaredMethod("A1Y");
            if(!Modifier.isPublic(getter.getModifiers())||Modifier.isStatic(getter.getModifiers())
                    ||!"X.0dj".equals(getter.getReturnType().getName()))return null;
            return getter.invoke(activity);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){return null;}
    }
    private static Class<?> named(Class<?> type,String name) {
        for(Class<?> c=type;c!=null;c=c.getSuperclass())if(name.equals(c.getName()))return c;
        return null;
    }
    static Object fragments(Object manager) {
        Object standard=BackNavigation.invoke(manager,"getFragments");
        if(standard instanceof Iterable<?>)return standard;
        if(manager==null)return null;
        try {
            Class<?> owner=named(manager.getClass(),"X.0dj");if(owner==null)return null;
            Field storeField=owner.getDeclaredField("A0V");
            if(!Modifier.isPublic(storeField.getModifiers())||Modifier.isStatic(storeField.getModifiers())
                    ||!"X.0dw".equals(storeField.getType().getName()))return null;
            Object store=storeField.get(manager);if(store==null||store.getClass()!=storeField.getType())return null;
            Field added=store.getClass().getDeclaredField("A01");
            if(!Modifier.isPublic(added.getModifiers())||Modifier.isStatic(added.getModifiers())
                    ||added.getType()!=ArrayList.class)return null;
            Object list=added.get(store);if(!(list instanceof List<?>))return null;
            synchronized(list){return new ArrayList<Object>((List<?>)list);}
        }catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){return null;}
    }
    /** getGlobalVisibleRect reports clipping, not sibling occlusion. Exclude a
     * retained page only with independent lifecycle, geometry and draw-order
     * proof that a resumed full Reels viewer covers it. Later/active fragments,
     * elevation and custom drawing order must still protect real comments. */
    static Set<View> coveredBackground(Activity activity) {
        InstagramCurrent.Result current=InstagramCurrent.inspect(activity);
        if(current.available)return current.backgroundRoots;
        return coveredBackground(activity,foregroundFragment(activity),nativeSelected(activity)!=null);
    }
    private static Set<View> coveredBackground(Activity activity,Object current,boolean nativeOrder) {
        Set<View> covered=Collections.newSetFromMap(new IdentityHashMap<View,Boolean>());
        if(current==null||current==Boolean.FALSE||!Boolean.TRUE.equals(BackNavigation.invoke(current,"isResumed")))return covered;
        Object value=BackNavigation.invoke(current,"getView");if(!(value instanceof View))return covered;
        View foreground=(View)value;Rect area=new Rect(),screen=new Rect();
        if(!InstagramBack.shown(foreground,area)||foreground.getAlpha()<.999f
                ||!InstagramBack.shown(foreground.getRootView(),screen)||!large(area,screen)
                ||!wrapper(foreground,screen))return covered;
        Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
        if(!coveredInManager(supportManager(activity),current,foreground,area,covered,seen,0,nativeOrder))
            coveredInManager(BackNavigation.invoke(activity,"getFragmentManager"),current,foreground,area,covered,seen,0,nativeOrder);
        return covered;
    }
    private static boolean coveredInManager(Object manager,Object current,View foreground,Rect area,
            Set<View> covered,Set<Object> seen,int depth,boolean nativeOrder) {
        if(manager==null||depth>8||!seen.add(manager))return false;
        Object objects=fragments(manager);if(!(objects instanceof List<?>))return false;
        List<?> list=(List<?>)objects;int active=-1;
        for(int i=0;i<list.size();i++)if(list.get(i)==current){active=i;break;}
        if(active>=0){
            // An exact native selection can outlive the added-list order. Even
            // then, physical draw order and non-resumed lifecycle remain required.
            for(int i=0;i<(nativeOrder?list.size():active);i++){
                Object f=list.get(i);
                if(!Boolean.FALSE.equals(BackNavigation.invoke(f,"isResumed")))continue;
                Object value=BackNavigation.invoke(f,"getView");if(!(value instanceof View))continue;
                View background=(View)value;Rect bounds=new Rect();
                if(background==foreground||background.getRootView()!=foreground.getRootView()
                        ||!InstagramBack.shown(background,bounds)
                        ||bounds.left<area.left||bounds.top<area.top||bounds.right>area.right||bounds.bottom>area.bottom)continue;
                if(siblingBehind(background,foreground))covered.add(background);
            }
            return true;
        }
        for(Object f:list)if(f!=null&&Boolean.TRUE.equals(BackNavigation.invoke(f,"isVisible"))
                &&coveredInManager(BackNavigation.invoke(f,"getChildFragmentManager"),current,foreground,area,covered,seen,depth+1,nativeOrder))return true;
        return false;
    }
    private static boolean siblingBehind(View background,View foreground) {
        if(!(foreground.getParent() instanceof ViewGroup)||background.getParent()!=foreground.getParent())return false;
        Object bz=BackNavigation.invoke(background,"getZ"),fz=BackNavigation.invoke(foreground,"getZ");
        if(!(bz instanceof Number)||!(fz instanceof Number))return false;
        float lower=((Number)bz).floatValue(),upper=((Number)fz).floatValue();
        if(Float.isNaN(lower)||Float.isInfinite(lower)||Float.isNaN(upper)||Float.isInfinite(upper)||lower>upper)return false;
        ViewGroup parent=(ViewGroup)foreground.getParent();int size=parent.getChildCount();
        if(size>200)return false;
        try {
            // API 29's public accessor includes a ViewGroup's custom child order.
            // Resolve on public declaring classes, as with the other read adapters.
            Method order=null;
            for(Class<?> c=parent.getClass();c!=null&&order==null;c=c.getSuperclass())if(Modifier.isPublic(c.getModifiers())){
                Method method=c.getMethod("getChildDrawingOrder",int.class);
                if(Modifier.isPublic(method.getDeclaringClass().getModifiers()))order=method;
            }
            if(order==null)return false;
            int before=-1,after=-1;Set<Integer> indices=new HashSet<Integer>();
            for(int i=0;i<size;i++){
                Object result=order.invoke(parent,i);if(!(result instanceof Integer))return false;
                int index=(Integer)result;if(index<0||index>=size||!indices.add(index))return false;
                View child=parent.getChildAt(index);if(child==background)before=i;if(child==foreground)after=i;
            }
            return before>=0&&after>=0&&(lower<upper||before<after);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){return false;}
    }
    /** A gesture may commit only against the same positively identified Reels
     * content that was present at start, even when the whole Activity scan fails. */
    static Object foregroundFragment(Activity a) {
        InstagramCurrent.Result current=InstagramCurrent.inspect(a);
        if(current.available)return current.kind==InstagramCurrent.REELS?current.target
            :current.kind==InstagramCurrent.OTHER||current.kind==InstagramCurrent.HOME?Boolean.FALSE:null;
        Object selected=nativeSelected(a);
        if(selected!=null)return nativeForeground(a,selected);
        Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
        Object support=foregroundObject(supportManager(a),seen,0);
        return support!=null?support:foregroundObject(BackNavigation.invoke(a,"getFragmentManager"),seen,0);
    }
    /** Verified v447 getter: MainActivity.A1e -> its navigation controller's
     * current tab fragment. The return may be a tab wrapper, so descend its child
     * manager. A selected tab alone must never overrule a real profile overlay. */
    private static Object nativeSelected(Activity a) {
        if(!InstagramBack.owns(a))return null;
        Class<?> owner=named(a.getClass(),"com.instagram.mainactivity.InstagramMainActivity");
        if(owner==null)return null;
        Method getter;
        try {
            getter=owner.getDeclaredMethod("A1e");
            if(!Modifier.isPublic(getter.getModifiers())||!Modifier.isFinal(getter.getModifiers())
                    ||Modifier.isStatic(getter.getModifiers())
                    ||!"androidx.fragment.app.Fragment".equals(getter.getReturnType().getName()))return null;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return null;}
        try {Object value=getter.invoke(a);return value==null?Boolean.FALSE:value;}
        catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return Boolean.FALSE;}
    }
    private static Object nativeForeground(Activity a,Object selected) {
        if(selected==Boolean.FALSE)return Boolean.FALSE;
        Object current=foregroundItem(selected,Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()),0,Collections.<View>emptySet());
        if(current==null||current==Boolean.FALSE)return current;
        Set<View> covered=coveredBackground(a,current,true);
        Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
        Object global=foregroundObject(supportManager(a),seen,0,covered);
        if(global==null)global=foregroundObject(BackNavigation.invoke(a,"getFragmentManager"),seen,0,covered);
        return global==Boolean.FALSE?Boolean.FALSE:current;
    }
    private static Object foregroundObject(Object manager,Set<Object> seen,int depth) {
        return foregroundObject(manager,seen,depth,Collections.<View>emptySet());
    }
    private static Object foregroundObject(Object manager,Set<Object> seen,int depth,Set<View> covered) {
        if(manager==null||depth>8||!seen.add(manager))return null;
        Object objects=fragments(manager);if(!(objects instanceof List<?>))return null;
        List<?> list=(List<?>)objects;
        for(int i=list.size()-1;i>=0;i--){Object candidate=foregroundItem(list.get(i),seen,depth,covered);if(candidate!=null)return candidate;}
        return null;
    }
    private static Object foregroundItem(Object f,Set<Object> seen,int depth,Set<View> covered) {
        if(f==null||!Boolean.TRUE.equals(BackNavigation.invoke(f,"isVisible")))return null;
        Object v=BackNavigation.invoke(f,"getView");
        if(v instanceof View&&(covered.contains(v)||!InstagramBack.shown((View)v,new Rect())))return null;
        Object child=foregroundObject(BackNavigation.invoke(f,"getChildFragmentManager"),seen,depth+1,covered);
        if(child!=null)return child;
        if(route(f))return f;
        String name=f.getClass().getName().toLowerCase(Locale.ROOT);
        Object m=BackNavigation.invoke(f,"getModuleName");if(m instanceof String)name+="/"+TikTokScreen.text((String)m);
        return TikTokScreen.contains(name,"profilefragment","profile_fragment","profile_view","searchfragment","search_fragment",
            "settingsfragment","detailfragment","storyviewer","story_viewer","reelviewer","reel_viewer",
            "directinbox","directthread","clips_camera","clips_editor")?Boolean.FALSE:null;
    }
    static int foreground(Activity a) {
        InstagramCurrent.Result current=InstagramCurrent.inspect(a);
        if(current.available)return current.kind==InstagramCurrent.REELS?1:current.kind==InstagramCurrent.OTHER?-1:0;
        Object selected=nativeSelected(a);
        if(selected!=null){Object legacyCurrent=nativeForeground(a,selected);return legacyCurrent==Boolean.FALSE?-1:legacyCurrent==null?0:1;}
        Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
        int support=manager(supportManager(a),seen,0);
        return support!=0?support:manager(BackNavigation.invoke(a,"getFragmentManager"),seen,0);
    }
    // +1 positively foreground Reels, -1 positively another content page, 0 unknown.
    private static int manager(Object manager,Set<Object> seen,int depth) {
        if(manager==null||depth>8||!seen.add(manager))return 0;
        Object primary=BackNavigation.invoke(manager,"getPrimaryNavigationFragment");
        Object objects=fragments(manager);
        if(!(objects instanceof List<?>))return fragment(primary,seen,depth);
        List<?> list=(List<?>)objects;
        // Added fragments are ordered; inspect the foreground candidate before the
        // still-visible profile/search page underneath the Reels presentation.
        for(int i=list.size()-1;i>=0;i--){Object f=list.get(i);
            int result=fragment(f,seen,depth);if(result!=0)return result;}
        return fragment(primary,seen,depth);
    }
    private static int fragment(Object f,Set<Object> seen,int depth) {
        if(f==null||!Boolean.TRUE.equals(BackNavigation.invoke(f,"isVisible")))return 0;
        Object view=BackNavigation.invoke(f,"getView");
        if(view instanceof View&&!InstagramBack.shown((View)view,new Rect()))return 0;
        int child=manager(BackNavigation.invoke(f,"getChildFragmentManager"),seen,depth+1);
        if(child!=0)return child;
        if(route(f))return 1;
        String name=f.getClass().getName().toLowerCase(Locale.ROOT);
        Object m=BackNavigation.invoke(f,"getModuleName");if(m instanceof String)name+="/"+TikTokScreen.text((String)m);
        return TikTokScreen.contains(name,"profilefragment","profile_fragment","profile_view","searchfragment","search_fragment",
            "settingsfragment","detailfragment","storyviewer","story_viewer","reelviewer","reel_viewer",
            "directinbox","directthread","clips_camera","clips_editor")?-1:0;
    }
}
