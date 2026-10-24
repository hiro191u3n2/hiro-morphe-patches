package app.hiro.oneback.runtime;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/** Binds the app's actual AndroidX dispatcher to its Activity. This entry does
 * not depend on which platform callback won, decor registration, or the app
 * eventually reaching MainActivity/Reels onBackPressed. */
final class InstagramDispatcherBack {
    private InstagramDispatcherBack() {}
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Map<Object,WeakReference<Activity>> OWNERS=new WeakHashMap<Object,WeakReference<Activity>>();
    private static final Map<Object,Gesture> GESTURES=new WeakHashMap<Object,Gesture>();
    private static final class Gesture {
        final WeakReference<Activity> owner;
        final boolean suppress;
        Gesture(Activity activity,boolean suppress){owner=new WeakReference<Activity>(activity);this.suppress=suppress;}
    }
    static void bind(Activity a,Object dispatcher) {
        if(Looper.myLooper()!=Looper.getMainLooper()||!InstagramBack.owns(a)||dispatcher==null)return;
        WeakReference<Activity> previous=OWNERS.get(dispatcher);
        if(previous!=null&&previous.get()!=a)GESTURES.remove(dispatcher);
        OWNERS.put(dispatcher,new WeakReference<Activity>(a));
        if(previous==null||previous.get()!=a)android.util.Log.d("HiroBack122","Instagram AndroidX bound: "+a.getClass().getName());
    }
    private static Activity owner(Object dispatcher) {
        WeakReference<Activity> ref=dispatcher==null?null:OWNERS.get(dispatcher);Activity a=ref==null?null:ref.get();
        return a!=null&&!a.isDestroyed()&&!a.isFinishing()?a:null;
    }
    static boolean start(Object dispatcher) {
        if(Looper.myLooper()!=Looper.getMainLooper())return false;
        Activity a=owner(dispatcher);if(a==null)return false;
        InstagramBack.predictionStart(a);
        boolean suppress=InstagramBack.exitPrediction(a);
        GESTURES.put(dispatcher,new Gesture(a,suppress));return suppress;
    }
    static boolean progress(Object dispatcher) {
        if(Looper.myLooper()!=Looper.getMainLooper())return false;
        Gesture g=GESTURES.get(dispatcher);return g!=null&&g.owner.get()==owner(dispatcher)&&g.suppress;
    }
    static boolean cancel(Object dispatcher) {
        if(Looper.myLooper()!=Looper.getMainLooper())return false;
        Gesture g=GESTURES.remove(dispatcher);Activity a=g==null?owner(dispatcher):g.owner.get();
        InstagramBack.predictionEnd(a);return g!=null&&g.suppress;
    }
    static boolean commit(final Object dispatcher) {
        if(Looper.myLooper()!=Looper.getMainLooper()||InstagramBack.forwarding())return false;
        final Activity a=owner(dispatcher);if(a==null){android.util.Log.d("HiroBack122","Instagram AndroidX Back: no live bound Activity");return false;}
        final Gesture gesture=GESTURES.get(dispatcher);
        if(gesture==null)InstagramBack.systemCommit(a);
        // This dispatcher may be the first and only scope around a native pop.
        // Preserve a profile video's origin through its synchronous continuation.
        InstagramProfileBack.dispatcherReturn(a);
        boolean handled=OneBackExit.tryBoundInstagramBack(a);
        if(handled||gesture!=null&&gesture.suppress) {
            // If the target changed after a suppressed start, consume the
            // gesture without playing an unmatched native pop animation.
            GESTURES.remove(dispatcher);InstagramBack.predictionEnd(a);return true;
        }
        // Keep an unsuppressed gesture's initial non-exit decision until its
        // synchronous native callback returns. Native pop may expose Reels;
        // that is still the same Back and must not become an exit.
        if(gesture!=null)MAIN.post(new Runnable(){public void run(){
            if(GESTURES.get(dispatcher)==gesture){GESTURES.remove(dispatcher);InstagramBack.predictionEnd(a);}
        }});
        return false;
    }
    static void predictionEnd(Activity a) {
        if(a==null)return;
        for(Iterator<Map.Entry<Object,Gesture>> it=GESTURES.entrySet().iterator();it.hasNext();)
            if(it.next().getValue().owner.get()==a)it.remove();
    }
    static void destroyed(Activity a) {
        for(Iterator<Map.Entry<Object,WeakReference<Activity>>> it=OWNERS.entrySet().iterator();it.hasNext();) {
            Map.Entry<Object,WeakReference<Activity>> item=it.next();
            if(item.getValue().get()==null||item.getValue().get()==a){GESTURES.remove(item.getKey());it.remove();}
        }
    }
}
