package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.*;

/** Comments/layers are dismissed, never interpreted as the Reels/Home screen behind them. */
final class InstagramBack {
    private InstagramBack() {}
    private static final Map<Activity,Guard> GUARDS=new WeakHashMap<Activity,Guard>();
    private static final Map<Activity,Integer> PREDICTIONS=new WeakHashMap<Activity,Integer>();
    private static final ThreadLocal<Boolean> FORWARDING=new ThreadLocal<Boolean>();
    private static final ThreadLocal<Boolean> INTERNAL=new ThreadLocal<Boolean>();
    private static final long WINDOW=1200,DUPLICATE=350;
    private static final class Guard {
        final int task;final String pkg;final long start=SystemClock.uptimeMillis();boolean done;
        Guard(Activity a){task=a.getTaskId();pkg=a.getPackageName();}
    }
    static final class Snapshot { boolean comments,blocked,unknown;View close; }
    static boolean owns(Activity a){return a!=null&&"com.instagram.android".equals(a.getPackageName());}
    private static Guard guard(Activity a){
        Guard g=GUARDS.get(a);
        if(g!=null&&(a.isDestroyed()||g.task!=a.getTaskId()||SystemClock.uptimeMillis()-g.start>WINDOW)){GUARDS.remove(a);return null;}
        return g;
    }
    static boolean ready(View root) {
        if(root==null||!root.isShown()||!root.isAttachedToWindow()||!root.hasWindowFocus())return false;
        try {
            if(Build.VERSION.SDK_INT>=30)return root.getRootWindowInsets()!=null&&!root.getRootWindowInsets().isVisible(android.view.WindowInsets.Type.ime());
            Rect full=new Rect(),visible=new Rect();root.getGlobalVisibleRect(full);root.getWindowVisibleDisplayFrame(visible);
            return full.bottom-visible.bottom<full.height()/5;
        }catch(RuntimeException|LinkageError failed){return false;}
    }
    static Snapshot inspect(Activity a) {
        Snapshot s=new Snapshot();if(!owns(a)||a.isDestroyed()||a.isFinishing()){s.unknown=true;return s;}
        try {
            scan(a.getWindow()==null?null:a.getWindow().peekDecorView(),s);
            Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
            fragments(BackNavigation.invoke(a,"getSupportFragmentManager"),s,seen,0);
            fragments(BackNavigation.invoke(a,"getFragmentManager"),s,seen,0);
        }catch(RuntimeException|LinkageError failed){s.unknown=true;}
        return s;
    }
    static Snapshot window(View root){Snapshot s=new Snapshot();try{scan(root,s);}catch(RuntimeException|LinkageError failed){s.unknown=true;}return s;}
    private static boolean commentContainer(String id) {
        if(id.contains("comment")&&TikTokScreen.contains(id,"button","icon","count","preview","badge"))return false;
        return id.contains("comment")&&TikTokScreen.contains(id,"thread","sheet","panel","container","root","list","recycler","composer","edit_text","layout");
    }
    private static boolean modal(String id){return TikTokScreen.contains(id,"bottom_sheet","bottomsheet","sheet_container","sheet_root","dialog_container","dialogfragment");}
    private static boolean heading(String s){return s.equals("コメント")||s.equals("comments")||s.matches("(?:コメント|comments)[ :：\\(（]*[0-9０-９,.万千kKmM]+[\\)）]?");}
    private static void scan(View root,Snapshot s) {
        Rect screen=new Rect();if(!TikTokScreen.shown(root,screen)){s.unknown=true;return;}
        Set<View> closes=Collections.newSetFromMap(new IdentityHashMap<View,Boolean>());
        ArrayDeque<View> q=new ArrayDeque<View>();q.add(root);int count=0;
        while(!q.isEmpty()) {
            if(++count>1800){s.unknown=true;break;}
            View v=q.removeFirst();Rect r=new Rect();if(!TikTokScreen.shown(v,r))continue;
            String id=TikTokScreen.id(v),name=v.getClass().getName().toLowerCase(Locale.ROOT);
            String label=TikTokScreen.text(v.getContentDescription()),text=v instanceof TextView?TikTokScreen.text(((TextView)v).getText()):"";
            boolean wide=r.width()>screen.width()*.35f,large=wide&&r.height()>screen.height()*.12f;
            // A narrow comment action icon on a reel is NOT an open comments panel.
            if((large&&(commentContainer(id)||commentContainer(name)))
                    ||(wide&&commentContainer(id)&&TikTokScreen.contains(id,"composer","edit_text"))
                    ||(wide&&!v.isClickable()&&r.height()<screen.height()*.16f&&(heading(text)||heading(label)))
                    ||(large&&heading(label)))s.comments=true;
            if(large&&(modal(id)||modal(name)))s.blocked=true;
            if(BackNavigation.backControl(v,r,screen)) {
                View target=TikTokScreen.clickable(v,screen,false);if(target!=null)closes.add(target);
            }
            if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;if(group.getChildCount()>1800-count){s.unknown=true;break;}for(int i=0;i<group.getChildCount();i++)q.add(group.getChildAt(i));}
        }
        s.blocked|=s.comments;
        if(closes.size()==1)s.close=closes.iterator().next();
    }
    private static void fragments(Object manager,Snapshot s,Set<Object> seen,int depth){
        if(manager==null)return;if(depth>8){s.unknown=true;return;}if(!seen.add(manager))return;
        Object list=BackNavigation.invoke(manager,"getFragments");if(!(list instanceof Iterable<?>))return;
        for(Object f:(Iterable<?>)list){
            if(f==null||!Boolean.TRUE.equals(BackNavigation.invoke(f,"isVisible")))continue;
            String name=f.getClass().getName().toLowerCase(Locale.ROOT);Object module=BackNavigation.invoke(f,"getModuleName");
            if(module instanceof String)name+="/"+TikTokScreen.text((String)module);
            if(name.contains("comment")&&TikTokScreen.contains(name,"fragment","thread","sheet","panel","comments")){s.comments=true;s.blocked=true;}
            if(modal(name))s.blocked=true;
            fragments(BackNavigation.invoke(f,"getChildFragmentManager"),s,seen,depth+1);
        }
    }
    static int action(Activity a) {
        if(!owns(a)||Boolean.TRUE.equals(FORWARDING.get()))return 0;
        View root=a.getWindow()==null?null:a.getWindow().peekDecorView();
        if(!ready(root))return 0;
        Integer predicted=PREDICTIONS.get(a);
        if(predicted!=null&&predicted!=1)return predicted;
        if(guard(a)!=null)return 3;
        Snapshot s=inspect(a);
        if(s.blocked)return 3;
        if(s.unknown)return 0;
        return ExitScreens.shouldExit(a)?1:0;
    }
    static void protect(Activity a){if(owns(a)&&guard(a)==null&&inspect(a).blocked){Guard g=new Guard(a);g.done=true;GUARDS.put(a,g);}}
    static void freshKey(Activity a){if(owns(a)){Guard g=guard(a);if(g!=null&&g.done)GUARDS.remove(a);}}
    static void systemCommit(Activity a){if(owns(a)&&!PREDICTIONS.containsKey(a)){Guard g=guard(a);if(g!=null&&g.done&&SystemClock.uptimeMillis()-g.start>=DUPLICATE)GUARDS.remove(a);}}
    static void predictionStart(Activity a){if(owns(a)){freshKey(a);PREDICTIONS.remove(a);PREDICTIONS.put(a,action(a));}}
    static void predictionEnd(Activity a){PREDICTIONS.remove(a);}
    static void cancel(Activity a){GUARDS.remove(a);PREDICTIONS.remove(a);}
    static void interaction(Activity a){if(!Boolean.TRUE.equals(INTERNAL.get())&&!Boolean.TRUE.equals(FORWARDING.get()))cancel(a);}
    static void request(Activity a) {
        if(!owns(a)||guard(a)!=null)return;
        final Guard g=new Guard(a);GUARDS.put(a,g);
        Snapshot before=inspect(a);
        if(!before.blocked){g.done=true;return;} // Started with comments; they already collapsed during this gesture.
        try {
            boolean clicked=false;
            // A semantic close control belonging to this layer uses the app's own listener.
            // Prefer native Back when a reel toolbar could be behind the layer.
            if(before.close!=null&&layerControl(before.close)){
                INTERNAL.set(Boolean.TRUE);try{clicked=BackNavigation.click(before.close);}finally{INTERNAL.remove();}
            }
            if(!clicked){FORWARDING.set(Boolean.TRUE);try{OneBackExit.tiktokNative(a);}finally{FORWARDING.remove();}}
            android.util.Log.d("HiroBack115",inspect(a).blocked?"Instagram layer native Back forwarded; task exit suppressed":"Instagram layer collapsed; same Back cannot exit");
        }finally{g.done=true;}
    }
    private static boolean layerControl(View view){
        // A generic top-level Reels back arrow is not a comments dismiss control.
        for(int n=0;n<5&&view!=null;n++){
            String id=TikTokScreen.id(view),name=view.getClass().getName().toLowerCase(Locale.ROOT);
            if(commentContainer(id)||commentContainer(name)||modal(id)||modal(name)||Arrays.asList("sheet_close","comments_close","comment_close_button").contains(id))return true;
            view=view.getParent() instanceof View?(View)view.getParent():null;
        }
        return false;
    }
    static boolean terminal(Activity a,int kind){
        if(!owns(a)||guard(a)==null)return false;
        // Native fragments may close the layer, but no Activity/task terminal operation is allowed.
        if(kind==0&&BackNavigation.popKnownStack(a))return true;
        android.util.Log.d("HiroBack115","Instagram task exit blocked for layer Back");return true;
    }
    static boolean terminalTask(ActivityManager.AppTask task){
        if(task==null)return false;
        try {
            ActivityManager.RecentTaskInfo info=task.getTaskInfo();if(info==null)return false;
            int id=Build.VERSION.SDK_INT>=29?info.taskId:info.id;
            String pkg=info.baseActivity==null?null:info.baseActivity.getPackageName();
            if(pkg==null&&info.baseIntent!=null&&info.baseIntent.getComponent()!=null)pkg=info.baseIntent.getComponent().getPackageName();
            for(Activity a:new ArrayList<Activity>(GUARDS.keySet())){Guard g=a==null?null:guard(a);if(g!=null&&g.task==id&&g.pkg.equals(pkg))return true;}
        }catch(RuntimeException|LinkageError ignored){}return false;
    }
    static boolean dialog(Dialog d,Activity a){
        if(d==null||!owns(a)||Boolean.TRUE.equals(FORWARDING.get()))return false;
        View root=d.getWindow()==null?null:d.getWindow().peekDecorView();
        Snapshot s=window(root);if(!s.comments||!ready(root))return false;
        Guard existing=guard(a);if(existing!=null&&existing.done&&SystemClock.uptimeMillis()-existing.start<DUPLICATE)return true;
        Guard g=new Guard(a);GUARDS.put(a,g);
        FORWARDING.set(Boolean.TRUE);OneBackExit.enterNative();
        try{d.onBackPressed();return true;}finally{OneBackExit.leaveNative();FORWARDING.remove();g.done=true;}
    }
}
