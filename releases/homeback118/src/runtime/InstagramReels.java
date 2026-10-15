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
        return TikTokScreen.shown(v,r)&&large(r,screen)
            &&(viewer(TikTokScreen.id(v))||viewerClass(v.getClass()));
    }
    private static boolean viewerClass(Class<?> type) {
        for(Class<?> c=type;c!=null;c=c.getSuperclass())if(viewer(c.getName()))return true;
        return false;
    }
    static boolean wrapper(View root,Rect screen) {
        // Exempt ONLY the ancestor of a positively identified full-size viewer.
        // Children are still inspected: a comments/share sheet inside it is not exempt.
        Rect bounds=new Rect();if(!TikTokScreen.shown(root,bounds)||!large(bounds,screen))return false;
        ArrayDeque<View> q=new ArrayDeque<View>();q.add(root);int count=0;
        while(!q.isEmpty()&&++count<=600){
            View v=q.removeFirst();Rect r=new Rect();if(!TikTokScreen.shown(v,r))continue;
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
    static int foreground(Activity a) {
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
        if(view instanceof View&&!TikTokScreen.shown((View)view,new Rect()))return 0;
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
