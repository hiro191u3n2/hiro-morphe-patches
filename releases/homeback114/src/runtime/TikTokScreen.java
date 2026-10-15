package app.hiro.oneback.runtime;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/** TikTok-only, positive foreground evidence. Unknown screens are never exit permission. */
final class TikTokScreen {
    private TikTokScreen() {}
    static boolean owns(Activity a) { return a!=null && owns(a.getPackageName()); }
    static boolean owns(String p) { return "com.ss.android.ugc.trill".equals(p)||"com.zhiliaoapp.musically".equals(p); }
    static boolean main(Activity a) {
        if(!owns(a))return false;
        for(Class<?> c=a.getClass();c!=null;c=c.getSuperclass())
            if("com.ss.android.ugc.aweme.main.MainActivity".equals(c.getName()))return true;
        return false;
    }
    static final class Snapshot {
        boolean home,blocked,feed,otherSelected,comment,editor,unknown,unwind,selectedHome;
        View homeTarget,closeTarget;
    }
    static String text(CharSequence v){return v==null?"":v.toString().trim().toLowerCase(Locale.ROOT);}
    static String id(View v){try{return v.getId()==View.NO_ID?"":text(v.getResources().getResourceEntryName(v.getId()));}catch(RuntimeException ignored){return "";}}
    static boolean label(String s,String... words){for(String w:words)if(s.equals(w)||s.startsWith(w+",")||s.startsWith(w+"、")||s.startsWith(w+" tab")||s.startsWith(w+" タブ"))return true;return false;}
    static boolean homeLabel(String s){return label(s,"home","ホーム","ホームタブ");}
    static boolean homeId(String s){return Arrays.asList("tab_home","home_tab","feed_tab","home_tab_button","bottom_nav_home","bottom_navigation_home","navigation_home","tab_home_icon","home_tab_icon").contains(s);}
    static boolean nav(Rect r,Rect s){return r.centerY()>s.top+s.height()*.65f&&r.width()<s.width()*.45f&&r.height()<s.height()*.22f;}
    static boolean shown(View v,Rect r){return v!=null&&v.isShown()&&v.getAlpha()>.01f&&v.getGlobalVisibleRect(r)&&!r.isEmpty();}
    static boolean contains(String s,String... words){for(String w:words)if(s.contains(w))return true;return false;}
    static boolean secondary(String s){return contains(s,"comment","profile","channel","userdetail","user_detail","detailfragment","detail_fragment","search","setting","inbox","message","chat","directthread","direct_thread","bottomsheet","bottom_sheet","dialog","popup","share_sheet","drawer","liveplay","live_play","broadcast","editor","camera","record","publish","login","accountswitch","storyviewer","story_viewer","media_viewer","reel_viewer","reelviewer");}
    static View clickable(View v,Rect screen,boolean bottom) {
        for(int n=0;n<4&&v!=null;n++) {
            Rect r=new Rect();if(!shown(v,r)||!v.isEnabled()||(bottom&&!nav(r,screen)))return null;
            if(v.isClickable())return v;
            v=v.getParent() instanceof View?(View)v.getParent():null;
        }
        return null;
    }
    static boolean selected(View v,Rect screen) {
        for(int n=0;n<3&&v!=null;n++) {
            Rect r=new Rect();if(!shown(v,r)||!nav(r,screen))break;
            if(v.isSelected()||v.isActivated())return true;
            AccessibilityNodeInfo info=null;
            try{info=v.createAccessibilityNodeInfo();if(info!=null&&(info.isSelected()||(info.isCheckable()&&info.isChecked())))return true;}
            catch(RuntimeException ignored){}finally{if(info!=null)info.recycle();}
            v=v.getParent() instanceof View?(View)v.getParent():null;
        }
        return false;
    }
    static Snapshot inspect(Activity a) {
        Snapshot s=new Snapshot();
        if(!owns(a)||a.isDestroyed()||a.isFinishing()){s.unknown=true;return s;}
        try {
            View root=a.getWindow()==null?null:a.getWindow().peekDecorView();
            scan(root,s);
            Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
            fragments(invoke(a,"getFragmentManager"),seen,s,0);
            fragments(invoke(a,"getSupportFragmentManager"),seen,s,0);
            s.home=!s.unknown&&!s.blocked&&!s.otherSelected&&s.feed&&s.selectedHome&&s.homeTarget!=null&&main(a)&&HomeScreen.inputReady(a);
        }catch(RuntimeException|LinkageError ignored){s.unknown=true;s.home=false;}
        return s;
    }
    static Snapshot window(View root){Snapshot s=new Snapshot();try{scan(root,s);}catch(RuntimeException|LinkageError ignored){s.unknown=true;}return s;}
    private static void scan(View root,Snapshot s) {
        Rect screen=new Rect();if(!shown(root,screen)){s.unknown=true;return;}
        View focus=root.findFocus();s.editor=focus!=null&&focus.onCheckIsTextEditor();
        Set<View> homes=Collections.newSetFromMap(new IdentityHashMap<View,Boolean>()),closes=Collections.newSetFromMap(new IdentityHashMap<View,Boolean>());
        ArrayDeque<View> q=new ArrayDeque<View>();q.add(root);int count=0;
        while(!q.isEmpty()) {
            if(++count>1600){s.unknown=true;break;}
            View v=q.removeFirst();Rect r=new Rect();if(!shown(v,r))continue;
            String resource=id(v),name=v.getClass().getName().toLowerCase(Locale.ROOT),desc=text(v.getContentDescription()),value=v instanceof TextView?text(((TextView)v).getText()):"";
            boolean bottom=nav(r,screen),isHome=homeId(resource)||homeLabel(desc)||homeLabel(value);
            if(bottom&&isHome){View target=clickable(v,screen,true);if(target!=null){homes.add(target);s.selectedHome|=selected(v,screen);}}
            if(bottom&&!isHome&&selected(v,screen)&&clickable(v,screen,true)!=null
                    &&(label(desc,"profile","me","inbox","messages","friends","shop","discover","プロフィール","メッセージ","受信トレイ","友達","ショップ","検索")
                    ||label(value,"profile","me","inbox","messages","friends","shop","discover","プロフィール","メッセージ","受信トレイ","友達","ショップ","検索")
                    ||contains(resource,"tab_profile","profile_tab","tab_inbox","inbox_tab","tab_message","message_tab","tab_friend","friends_tab","tab_shop","shop_tab")))s.otherSelected=true;
            if(r.centerY()<screen.top+screen.height()*.25f&&(label(desc,"for you","following","おすすめ","フォロー中")||label(value,"for you","following","おすすめ","フォロー中")))s.feed=true;
            boolean substantial=r.width()>screen.width()*.35f&&r.height()>screen.height()*.12f;
            if(substantial&&!bottom) {
                if(contains(resource,"feed_view_pager","feed_pager","feed_container","recommend_feed")||contains(name,"feedviewpager","feedfragmentview"))s.feed=true;
                if(secondary(resource)||secondary(name)){s.blocked=true;if(contains(resource,"comment")||contains(name,"comment"))s.comment=true;
                    if(contains(resource,"comment","sheet","dialog","popup","drawer")||contains(name,"comment","sheet","dialog","popup","drawer"))s.unwind=true;}
            }
            // A top Back/Close control is foreground-page evidence, not a Home screen.
            if(v.isClickable()&&r.centerY()<screen.top+screen.height()*.5f&&label(desc,"back","go back","navigate up","close","dismiss","戻る","前に戻る","閉じる")) {
                s.blocked=true;s.unwind=true;closes.add(v);
            }
            if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;if(g.getChildCount()>1600-count){s.unknown=true;break;}for(int i=0;i<g.getChildCount();i++)q.add(g.getChildAt(i));}
        }
        if(!s.unknown&&homes.size()==1)s.homeTarget=homes.iterator().next();
        if(!s.unknown&&closes.size()==1)s.closeTarget=closes.iterator().next();
    }
    private enum Failure{INSTANCE}
    private static Object invoke(Object o,String name) {
        if(o==null)return null;
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass()) {
            if(!Modifier.isPublic(c.getModifiers()))continue;
            try{Method m=c.getMethod(name);if(Modifier.isPublic(m.getDeclaringClass().getModifiers()))return m.invoke(o);}
            catch(NoSuchMethodException absent){}catch(ReflectiveOperationException|RuntimeException bad){return Failure.INSTANCE;}
        }
        return null;
    }
    private static void fragments(Object manager,Set<Object> seen,Snapshot s,int depth) {
        if(manager==null)return;
        if(manager==Failure.INSTANCE||depth>8){s.unknown=true;return;}
        if(!seen.add(manager))return;
        Object n=invoke(manager,"getBackStackEntryCount"),list=invoke(manager,"getFragments");
        if(n==Failure.INSTANCE||list==Failure.INSTANCE){s.unknown=true;return;}
        if(n instanceof Number&&((Number)n).intValue()>0){s.blocked=true;s.unwind=true;}
        if(!(list instanceof Iterable<?>))return;
        for(Object f:(Iterable<?>)list){if(f==null||!Boolean.TRUE.equals(invoke(f,"isVisible")))continue;
            String name=f.getClass().getName().toLowerCase(Locale.ROOT);Object module=invoke(f,"getModuleName");
            if(module instanceof String)name+="/"+text((String)module);
            if(secondary(name)){s.blocked=true;if(contains(name,"comment")){s.comment=true;s.unwind=true;}
                if(contains(name,"sheet","dialog","popup","drawer"))s.unwind=true;}
            if(contains(name,"feedfragment","feed_fragment","recommendfragment","homepagefragment"))s.feed=true;
            fragments(invoke(f,"getChildFragmentManager"),seen,s,depth+1);
        }
    }
}
