package app.hiro.oneback.runtime;

import android.os.Build;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Decorates public app registrations, retaining exact native behavior outside home. API 33+. */
public final class NativeBack {
    private NativeBack() {}
    private static final List<Record> RECORDS=new ArrayList<Record>();
    private static long sequence;
    private static final class Record {
        final WeakReference<OnBackInvokedDispatcher> dispatcher;
        final WeakReference<OnBackInvokedCallback> original,wrapper;
        final int priority; final long order;
        Record(OnBackInvokedDispatcher d,OnBackInvokedCallback o,OnBackInvokedCallback w,int p) {
            dispatcher=new WeakReference<OnBackInvokedDispatcher>(d);original=new WeakReference<OnBackInvokedCallback>(o);
            wrapper=new WeakReference<OnBackInvokedCallback>(w);priority=p;order=++sequence;
        }
    }
    private static Record find(OnBackInvokedDispatcher d,OnBackInvokedCallback original) {
        Record found=null;
        for(Iterator<Record> it=RECORDS.iterator();it.hasNext();) {
            Record r=it.next();if(r.dispatcher.get()==null||r.original.get()==null||r.wrapper.get()==null)it.remove();
            else if(r.dispatcher.get()==d&&r.original.get()==original)found=r;
        }
        return found;
    }
    private static class Callback implements OnBackInvokedCallback {
        final WeakReference<OnBackInvokedDispatcher> dispatcher;
        final OnBackInvokedCallback original;
        Callback(OnBackInvokedDispatcher d,OnBackInvokedCallback o){dispatcher=new WeakReference<OnBackInvokedDispatcher>(d);original=o;}
        @Override public void onBackInvoked(){
            try {
                if(OneBackExit.tryFocusedHome(dispatcher.get()))return;
                OneBackExit.enterFocusedBack(dispatcher.get());try{original.onBackInvoked();OneBackExit.afterNative();}finally{OneBackExit.leaveNative();}
            } finally {OneBackExit.predictionEnd(dispatcher.get());}
        }
    }
    public static synchronized void register(OnBackInvokedDispatcher dispatcher,int priority,OnBackInvokedCallback callback) {
        if(callback==null){dispatcher.registerOnBackInvokedCallback(priority,callback);return;}
        Record old=find(dispatcher,callback);
        OnBackInvokedCallback wrapped=old==null?null:old.wrapper.get();
        if(wrapped==null)wrapped=Build.VERSION.SDK_INT>=34?Api34.wrap(dispatcher,callback):new Callback(dispatcher,callback);
        // Commit bookkeeping only after the platform accepts registration.
        dispatcher.registerOnBackInvokedCallback(priority,wrapped);
        if(old!=null)RECORDS.remove(old);
        RECORDS.add(new Record(dispatcher,callback,wrapped,priority));
        OneBackExit.refreshTwitterObserver(dispatcher);
    }
    public static synchronized void unregister(OnBackInvokedDispatcher dispatcher,OnBackInvokedCallback callback) {
        Record r=find(dispatcher,callback);OnBackInvokedCallback wrapped=r==null?null:r.wrapper.get();
        dispatcher.unregisterOnBackInvokedCallback(wrapped==null?callback:wrapped);
        if(r!=null)RECORDS.remove(r);
        OneBackExit.refreshTwitterObserver(dispatcher);
    }
    static synchronized boolean hasAppCallbacks(Object dispatcher) {
        boolean found=false;
        for(Iterator<Record> it=RECORDS.iterator();it.hasNext();) {
            Record r=it.next();
            if(r.dispatcher.get()==null||r.original.get()==null||r.wrapper.get()==null)it.remove();
            else if(r.dispatcher.get()==dispatcher&&r.priority>=OnBackInvokedDispatcher.PRIORITY_DEFAULT)found=true;
        }
        return found;
    }
    static boolean forward(Object dispatcher) {
        OnBackInvokedCallback target=null;Record best=null;
        synchronized(NativeBack.class) {
            for(Record r:RECORDS)if(r.dispatcher.get()==dispatcher&&r.original.get()!=null&&r.wrapper.get()!=null
                &&(best==null||r.priority>best.priority||(r.priority==best.priority&&r.order>best.order)))best=r;
            if(best!=null)target=best.original.get();
        }
        if(target==null)return false;target.onBackInvoked();return true;
    }
    private static final class Api34 {
        static OnBackInvokedCallback wrap(OnBackInvokedDispatcher d,OnBackInvokedCallback o) {
            return o instanceof android.window.OnBackAnimationCallback?new Animation(d,(android.window.OnBackAnimationCallback)o)
                :OneBackExit.isTikTokDispatcher(d)?new Capture(d,o):new Callback(d,o);
        }
        private static final class Capture extends Callback implements android.window.OnBackAnimationCallback {
            Capture(OnBackInvokedDispatcher d,OnBackInvokedCallback o){super(d,o);}
            public void onBackStarted(android.window.BackEvent e){OneBackExit.predictionStart(dispatcher.get());}
            public void onBackProgressed(android.window.BackEvent e){}
            public void onBackCancelled(){OneBackExit.predictionEnd(dispatcher.get());}
        }
        private static final class Animation extends Callback implements android.window.OnBackAnimationCallback {
            final android.window.OnBackAnimationCallback animation;
            boolean nativeAnimation;
            Animation(OnBackInvokedDispatcher d,android.window.OnBackAnimationCallback o){super(d,o);animation=o;}
            @Override public void onBackStarted(android.window.BackEvent event){
                OneBackExit.predictionStart(dispatcher.get());
                // Reels/Home exit is our committed action. Do not let the native
                // pop animation change tabs before that action is evaluated.
                nativeAnimation=!OneBackExit.suppressNativeAnimation(dispatcher.get());
                if(nativeAnimation)animation.onBackStarted(event);
            }
            @Override public void onBackProgressed(android.window.BackEvent event){if(nativeAnimation)animation.onBackProgressed(event);}
            @Override public void onBackCancelled(){try{if(nativeAnimation)animation.onBackCancelled();}finally{nativeAnimation=false;OneBackExit.predictionEnd(dispatcher.get());}}
            @Override public void onBackInvoked(){try{super.onBackInvoked();}finally{nativeAnimation=false;}}
        }
    }
}
