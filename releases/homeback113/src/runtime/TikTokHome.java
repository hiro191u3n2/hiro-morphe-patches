package app.hiro.oneback.runtime;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

/** Click the app's real visible Home tab; never simulate coordinates or restart the Activity. */
final class TikTokHome {
    private TikTokHome() {}
    private static boolean home(String value) {
        return value.equals("home")||value.equals("ホーム")||value.equals("ホームタブ")
            ||value.startsWith("home,")||value.startsWith("ホーム、")||value.startsWith("ホーム,")
            ||value.startsWith("home tab")||value.startsWith("ホーム タブ");
    }
    private static String text(CharSequence value){return value==null?"":value.toString().trim().toLowerCase(Locale.ROOT);}
    private static boolean nav(Rect r,Rect screen){return r.centerY()>screen.top+screen.height()*.65f
        &&r.width()<screen.width()*.45f&&r.height()<screen.height()*.22f;}
    private static boolean matches(View v) {
        if(home(text(v.getContentDescription()))||(v instanceof TextView&&home(text(((TextView)v).getText()))))return true;
        try {
            String id=v.getId()==View.NO_ID?"":v.getResources().getResourceEntryName(v.getId());
            return id.equals("tab_home")||id.equals("home_tab")||id.equals("feed_tab")||id.equals("home_tab_button")
                ||id.equals("bottom_nav_home")||id.equals("bottom_navigation_home")||id.equals("navigation_home")
                ||id.equals("tab_home_icon")||id.equals("home_tab_icon");
        }catch(RuntimeException ignored){return false;}
    }
    static boolean go(Activity activity) {
        if(!ExitScreens.isTikTokRootTab(activity))return false;
        try {
            View root=activity.getWindow().peekDecorView();Rect screen=new Rect();
            if(root==null||!root.getGlobalVisibleRect(screen))return false;
            ArrayDeque<View> queue=new ArrayDeque<View>();queue.add(root);
            Set<View> candidates=Collections.newSetFromMap(new IdentityHashMap<View,Boolean>());
            int count=0;
            while(!queue.isEmpty()) {
                if(++count>1600)return false;
                View view=queue.removeFirst();Rect area=new Rect();
                if(!view.isShown()||view.getAlpha()<=.01f||!view.getGlobalVisibleRect(area)||area.isEmpty())continue;
                if(nav(area,screen)&&matches(view)) {
                    View candidate=view;
                    for(int depth=0;depth<3;depth++) {
                        Rect box=new Rect();
                        if(!candidate.isShown()||!candidate.isEnabled()||!candidate.getGlobalVisibleRect(box)||!nav(box,screen))break;
                        if(candidate.isClickable()){candidates.add(candidate);break;}
                        if(!(candidate.getParent() instanceof View))break;
                        candidate=(View)candidate.getParent();
                    }
                }
                if(view instanceof ViewGroup) {
                    ViewGroup group=(ViewGroup)view;if(group.getChildCount()>1600-count)return false;
                    for(int i=0;i<group.getChildCount();i++)queue.add(group.getChildAt(i));
                }
            }
            if(candidates.size()!=1){android.util.Log.d("HiroBack113","TikTok Home target missing or ambiguous; exit suppressed");return false;}
            boolean clicked=candidates.iterator().next().performClick();
            android.util.Log.d("HiroBack113",clicked?"TikTok Home click handled":"TikTok Home click rejected; exit suppressed");
            return clicked;
        }catch(RuntimeException|LinkageError ignored){return false;}
    }
}
