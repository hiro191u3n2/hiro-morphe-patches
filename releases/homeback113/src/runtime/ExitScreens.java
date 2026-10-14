package app.hiro.oneback.runtime;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

/** Explicit user-selected exit screens. Instagram "reel_viewer" is Stories, NOT Reels. */
final class ExitScreens {
    private ExitScreens() {}
    static boolean shouldExit(Activity activity) {
        if (HomeScreen.isHome(activity)) return true;
        if (!HomeScreen.inputReady(activity)) return false;
        String pkg=activity.getPackageName();
        boolean instagram="com.instagram.android".equals(pkg);
        boolean tiktok="com.zhiliaoapp.musically".equals(pkg)||"com.ss.android.ugc.trill".equals(pkg);
        if (!instagram) return false;
        try {
            View root=activity.getWindow().peekDecorView();Rect screen=new Rect();
            if(!root.getGlobalVisibleRect(screen))return false;
            Evidence evidence=new Evidence(instagram);
            scanViews(root,screen,evidence);
            if(evidence.blocked)return false;
            Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
            scanManager(invoke(activity,"getFragmentManager"),seen,evidence,0);
            scanManager(invoke(activity,"getSupportFragmentManager"),seen,evidence,0);
            if(evidence.blocked)return false;
            // A full-screen Reels viewer may be opened from a profile/search result,
            // so its own back arrow/back stack is not grounds to disable explicit exit.
            if(evidence.viewer || evidence.route)return true;
            return mainHost(activity,instagram) && evidence.tab && !evidence.backArrow && !evidence.stack;
        } catch(RuntimeException | LinkageError ignored) {return false;}
    }
    static boolean isTikTokRootTab(Activity activity) {
        if (!HomeScreen.inputReady(activity)) return false;
        String pkg=activity.getPackageName();
        if (!("com.zhiliaoapp.musically".equals(pkg)||"com.ss.android.ugc.trill".equals(pkg))
                || !mainHost(activity,false)) return false;
        try {
            View root=activity.getWindow().peekDecorView();Rect screen=new Rect();
            if(!root.getGlobalVisibleRect(screen))return false;
            Evidence evidence=new Evidence(false);scanViews(root,screen,evidence);
            Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
            scanManager(invoke(activity,"getFragmentManager"),seen,evidence,0);
            scanManager(invoke(activity,"getSupportFragmentManager"),seen,evidence,0);
            return evidence.tab&&!evidence.blocked&&!evidence.backArrow&&!evidence.stack;
        } catch(RuntimeException|LinkageError ignored){return false;}
    }
    private static final class Evidence {
        final boolean instagram;
        boolean tab,viewer,route,blocked,backArrow,stack;
        Evidence(boolean ig){instagram=ig;}
    }
    private static boolean mainHost(Activity a,boolean ig) {
        for(Class<?> c=a.getClass();c!=null;c=c.getSuperclass())
            if((ig?"com.instagram.mainactivity.InstagramMainActivity":"com.ss.android.ugc.aweme.main.MainActivity").equals(c.getName()))return true;
        return false;
    }
    private static String normalized(CharSequence value) {
        return value==null?"":value.toString().trim().toLowerCase(Locale.ROOT);
    }
    private static String id(View view) {
        try{return view.getId()==View.NO_ID?"":normalized(view.getResources().getResourceEntryName(view.getId()));}
        catch(RuntimeException ignored){return "";}
    }
    private static boolean isLabel(String value,String... labels) {
        for(String label:labels)if(value.equals(label)||value.startsWith(label+",")
            ||value.startsWith(label+"、")||value.startsWith(label+" tab")||value.startsWith(label+" タブ"))return true;
        return false;
    }
    private static boolean targetLabel(String value,boolean ig) {
        return ig?isLabel(value,"reels","リール")
            :isLabel(value,"inbox","messages","message","profile","me","メッセージ","受信トレイ","プロフィール","マイページ");
    }
    private static boolean targetId(String value,boolean ig) {
        return ig?oneOf(value,"clips_tab","reels_tab","tab_reels","tab_clips","reels_tab_button","clips_tab_icon","navigation_reels")
            :oneOf(value,"tab_inbox","inbox_tab","tab_messages","messages_tab","tab_message","message_tab","bottom_tab_messages",
                "tab_profile","profile_tab","tab_me","me_tab","bottom_tab_profile","bottom_nav_profile","bottom_nav_inbox");
    }
    private static boolean oneOf(String value,String... values){for(String item:values)if(item.equals(value))return true;return false;}
    private static boolean modal(String resource) {
        return oneOf(resource,"bottom_sheet","design_bottom_sheet","comment_sheet","comments_sheet","share_sheet",
            "drawer_content","navigation_drawer","story_viewer","reel_viewer","search_edit_text","comment_edit_text",
            "clips_camera_container","clips_editor_container","profile_edit_container","settings_container");
    }
    private static boolean fullViewer(String resource,boolean ig) {
        return ig && oneOf(resource,"clips_viewer","clips_viewer_view_pager","clips_viewer_pager","clips_viewer_container",
            "clips_viewer_root","clips_viewer_layout","reels_viewer","reels_viewer_container");
    }
    private static boolean large(Rect area,Rect screen){return area.width()>screen.width()*0.6f && area.height()>screen.height()*0.55f;}
    private static boolean nav(Rect area,Rect screen) {
        return area.centerY()>screen.top+screen.height()*0.65f && area.height()<screen.height()*0.22f && area.width()<screen.width()*0.45f;
    }
    private static void scanViews(View root,Rect screen,Evidence evidence) {
        ArrayDeque<View> pending=new ArrayDeque<View>();pending.add(root);int count=0;
        while(!pending.isEmpty()) {
            if(++count>1600){evidence.blocked=true;return;}
            View view=pending.removeFirst();
            if(!view.isShown()||view.getAlpha()<=0.01f)continue;
            Rect area=new Rect();if(!view.getGlobalVisibleRect(area)||area.isEmpty())continue;
            String resource=id(view),description=normalized(view.getContentDescription());
            String text=view instanceof TextView?normalized(((TextView)view).getText()):"";
            if(modal(resource)){evidence.blocked=true;return;}
            if(fullViewer(resource,evidence.instagram)&&large(area,screen))evidence.viewer=true;
            if(view.isClickable()&&area.centerY()<screen.top+screen.height()/4
                &&oneOf(description,"back","navigate up","go back","close","戻る","前に戻る","閉じる"))evidence.backArrow=true;
            if(nav(area,screen)&&(targetId(resource,evidence.instagram)||targetLabel(description,evidence.instagram)||targetLabel(text,evidence.instagram))) {
                boolean selected=view.isSelected()||view.isActivated(),clickable=view.isClickable();
                View ancestor=view;
                for(int depth=0;depth<2&&ancestor.getParent() instanceof View;depth++) {
                    ancestor=(View)ancestor.getParent();Rect parent=new Rect();
                    if(!ancestor.getGlobalVisibleRect(parent)||!nav(parent,screen))break;
                    selected|=ancestor.isSelected()||ancestor.isActivated();clickable|=ancestor.isClickable();
                }
                AccessibilityNodeInfo info=null;
                try {
                    info=view.createAccessibilityNodeInfo();
                    if(info!=null){selected|=info.isSelected()||(info.isCheckable()&&info.isChecked());clickable|=info.isClickable();}
                } finally {if(info!=null)info.recycle();}
                if(selected&&clickable)evidence.tab=true;
            }
            if(view instanceof ViewGroup) {
                ViewGroup group=(ViewGroup)view;
                if(group.getChildCount()>1600-count){evidence.blocked=true;return;}
                for(int i=0;i<group.getChildCount();i++)pending.add(group.getChildAt(i));
            }
        }
    }
    private enum Failure{INSTANCE}
    private static Object invoke(Object value,String name) {
        if(value==null)return null;
        for(Class<?> c=value.getClass();c!=null;c=c.getSuperclass()) {
            if(!Modifier.isPublic(c.getModifiers()))continue;
            try {
                Method method=c.getMethod(name);
                if(!Modifier.isPublic(method.getDeclaringClass().getModifiers()))continue;
                return method.invoke(value);
            } catch(NoSuchMethodException absent) {} catch(ReflectiveOperationException|RuntimeException failed){return Failure.INSTANCE;}
        }
        return null;
    }
    private static void scanManager(Object manager,Set<Object> seen,Evidence evidence,int depth) {
        if(manager==null)return;
        if(manager==Failure.INSTANCE||depth>8){evidence.blocked=true;return;}
        if(!seen.add(manager))return;
        Object size=invoke(manager,"getBackStackEntryCount"),fragments=invoke(manager,"getFragments");
        if(size==Failure.INSTANCE||fragments==Failure.INSTANCE){evidence.blocked=true;return;}
        if(size instanceof Number&&((Number)size).intValue()>0)evidence.stack=true;
        if(!(fragments instanceof Iterable<?>))return;
        for(Object fragment:(Iterable<?>)fragments) {
            if(fragment==null||!Boolean.TRUE.equals(invoke(fragment,"isVisible")))continue;
            String name=fragment.getClass().getName().toLowerCase(Locale.ROOT);
            Object moduleValue=invoke(fragment,"getModuleName");String module=moduleValue instanceof String?normalized((String)moduleValue):"";
            boolean route=evidence.instagram
                ?((name.startsWith("com.instagram.clips.")&&name.contains("viewer"))||oneOf(module,"clips_viewer","clips_feed","clips_tab"))
                :(name.startsWith("com.ss.android.ugc.aweme.")&&(name.endsWith(".userprofilefragment")||name.endsWith(".myprofilefragment")
                    ||name.endsWith(".inboxfragment")||name.endsWith(".messagefragment")||name.endsWith(".chatfragment")));
            if((!evidence.instagram&&(name.contains("chatfragment")||name.contains("conversationfragment")||name.contains("messagethread")))||name.contains("dialogfragment")||name.contains("bottomsheet")||name.contains("commentfragment")
                ||name.contains("searchfragment")||name.contains("settingsfragment")||name.contains("editprofile")
                ||name.contains("profileedit")||name.contains("storyviewer")||name.contains("reelviewer")
                ||name.contains("directinbox")||name.contains("directthread")
                ||(!route&&(name.contains("detailfragment")||name.contains("profilefragment")))) {evidence.blocked=true;return;}
            if(route)evidence.route=true;
            scanManager(invoke(fragment,"getChildFragmentManager"),seen,evidence,depth+1);
            if(evidence.blocked)return;
        }
    }
}
