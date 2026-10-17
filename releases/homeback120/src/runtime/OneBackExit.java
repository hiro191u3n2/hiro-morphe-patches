package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Supported apps exit only from confirmed home; all other BACK routes retain native handling. */
public final class OneBackExit {
    private OneBackExit() { }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    // All registries are accessed on the main thread. Their entries never own an Activity or View.
    private static final List<Scope> SCOPES = new ArrayList<Scope>();
    private static final List<State> STATES = new ArrayList<State>();
    private static final List<Root> ROOTS = new ArrayList<Root>();
    private static long sequence;
    private static final ThreadLocal<Integer> NATIVE_DEPTH = new ThreadLocal<Integer>();
    public static void enterNative() { Integer value=NATIVE_DEPTH.get(); NATIVE_DEPTH.set(value==null?1:value+1); }
    private static final ThreadLocal<Integer> TERMINAL_DEPTH = new ThreadLocal<Integer>();
    public static void enterBack(Activity activity) {
        enterNative();
        if (owned(activity) && TERMINAL_DEPTH.get()==null) {
            TERMINAL_DEPTH.set(NATIVE_DEPTH.get());TripBack.begin(activity);TwitterBack.begin(activity);TikTokBack.protect(activity);InstagramBack.protect(activity);
        }
    }
    public static void enterKey(Activity activity, KeyEvent event) {
        if (event!=null && event.getKeyCode()==KeyEvent.KEYCODE_BACK
                && event.getAction()==KeyEvent.ACTION_UP && !event.isCanceled()) enterBack(activity);
        else enterNative();
    }
    public static void afterNative() {
        Integer depth=NATIVE_DEPTH.get(),terminal=TERMINAL_DEPTH.get();
        if(terminal!=null&&terminal.equals(depth)){TripBack.commit();TwitterBack.commit();}
    }
    public static void leaveNative() {
        Integer value=NATIVE_DEPTH.get(), terminal=TERMINAL_DEPTH.get();
        if (terminal!=null && (value==null || value<=terminal)) { TERMINAL_DEPTH.remove();TripBack.end();TwitterBack.end(); }
        if(value==null||value<=1)NATIVE_DEPTH.remove();else NATIVE_DEPTH.set(value-1);
    }
    static void enterFocusedBack(Object dispatcher) {
        Activity owner=null;
        for(Root root:new ArrayList<Root>(ROOTS)) {
            View view=root.view.get();Activity activity=root.owner.activity.get();
            if((root.activityRoot||TikTokScreen.owns(activity)||InstagramBack.owns(activity)) && view!=null && activity!=null && !root.released && !root.detached
                && view.hasWindowFocus() && Build.VERSION.SDK_INT>=33 && Api33.matches(view,dispatcher)) {owner=activity;break;}
        }
        enterBack(owner);
    }
    /** Called only at the preserved native callsite, not while classifying a page.
     * kind 0=platform onBackPressed, 1=finish/finishAffinity/finishAfterTransition,
     * kind 2=moveTaskToBack. Never converts an ordinary detail pop into an exit.
     */
    public static boolean tryTerminalBack(Activity activity, int kind, boolean nonRoot) {
        if(Looper.myLooper()==Looper.getMainLooper()&&TwitterBack.owns(activity)&&TwitterBack.terminal(activity,kind))return true;
        if(Looper.myLooper()==Looper.getMainLooper()&&TikTokScreen.owns(activity))return TikTokBack.terminal(activity,kind);
        if(Looper.myLooper()==Looper.getMainLooper()&&InstagramBack.owns(activity)&&InstagramBack.terminal(activity,kind))return true;
        if(kind==3)return false;
        if(kind==4)kind=1;
        if (Looper.myLooper()!=Looper.getMainLooper() || (TERMINAL_DEPTH.get()==null && !TripBack.pending(activity))
                || !owned(activity) || !"ctrip.english".equals(activity.getPackageName())) return false;
        State current=state(activity,false);
        if (current!=null && (current.closing||current.scope.exiting)) return false;
        try {
            if (!activity.isTaskRoot() && !(kind==2 && nonRoot)) return false;
            if (!TripBack.pending(activity) && !HomeScreen.inputReady(activity)) return false;
            if (kind==0) {
                // The platform may still consume BACK for its own action bar or fragments.
                if (activity.getActionBar()!=null) return false;
                if (activity.getFragmentManager()!=null && activity.getFragmentManager().getBackStackEntryCount()>0) return false;
            }
            backNow(activity); return true;
        } catch(RuntimeException | LinkageError ignored) { return false; }
    }
    private static boolean nativePassThrough() { Integer value=NATIVE_DEPTH.get();return value!=null&&value>0; }
    private static boolean mayExit(Activity activity) {
        return owned(activity) && !nativePassThrough()
            && (!HomeScreen.restricted(activity.getPackageName()) || ExitScreens.shouldExit(activity));
    }
    private static int action(Activity activity) {
        if(!owned(activity)||nativePassThrough())return 0;
        if(TikTokScreen.owns(activity))return TikTokBack.action(activity);
        if(InstagramBack.owns(activity))return InstagramBack.action(activity);
        return mayExit(activity)?1:0;
    }
    public static boolean tryBack(Activity activity) {
        if(Looper.myLooper()!=Looper.getMainLooper())return false;
        int action=action(activity);
        if(action==2) { TikTokBack.request(activity);TikTokBack.predictionEnd(activity);return true; }
        if(action==3){InstagramBack.request(activity);InstagramBack.predictionEnd(activity);return true;}
        if(action!=1)return false;
        TikTokBack.predictionEnd(activity);InstagramBack.predictionEnd(activity);backNow(activity);return true;
    }
    /** Dedicated native Reels boundary; keep ordinary nested callbacks suppressed. */
    public static boolean tryInstagramReelsBack(Object fragment) {
        if(Looper.myLooper()!=Looper.getMainLooper()||TERMINAL_DEPTH.get()==null)return false;
        Activity activity=InstagramBack.nativeReelsTarget(fragment);
        if(activity==null)return false;
        android.util.Log.d("HiroBack119","Instagram verified native Reels Back boundary: task exit");
        InstagramBack.predictionEnd(activity);backNow(activity);return true;
    }
    /** The main Activity can dispatch straight to AndroidX and return before the
     * Reels boolean handler. This verified callsite follows its native popup and
     * dialog guards. Reuse the visible Reels identity and layer/gesture checks;
     * an Activity name or selected tab alone never grants permission to exit. */
    public static boolean tryInstagramMainReelsBack(Activity activity) {
        if(Looper.myLooper()!=Looper.getMainLooper()||TERMINAL_DEPTH.get()==null
                ||!InstagramBack.owns(activity))return false;
        Object fragment=InstagramReels.foregroundFragment(activity);
        if(InstagramBack.nativeReelsTarget(fragment)!=activity||!InstagramNativeLayers.clear(fragment))return false;
        android.util.Log.d("HiroBack120","Instagram main native Back boundary: visible Reels task exit");
        InstagramBack.predictionEnd(activity);backNow(activity);return true;
    }
    public static boolean tryDialogBack(Dialog dialog) {
        if(dialog!=null&&"com.instagram.android".equals(dialog.getContext().getPackageName())&&!nativePassThrough())return InstagramBack.dialog(dialog,dialogOwner(dialog));
        if(dialog!=null&&TikTokScreen.owns(dialog.getContext().getPackageName())&&!nativePassThrough())return TikTokBack.dialog(dialog,dialogOwner(dialog));
        if(dialog==null || HomeScreen.restricted(dialog.getContext().getPackageName()) || nativePassThrough()) return false;
        Activity activity=dialogOwner(dialog);return tryBack(activity);
    }
    static boolean tryFocusedHome(Object dispatcher) {
        for(Root root:new ArrayList<Root>(ROOTS)) {
            View view=root.view.get(); Activity activity=root.owner.activity.get();
            if(view!=null && activity!=null && !root.released && !root.detached
                && view.hasWindowFocus() && Build.VERSION.SDK_INT>=33 && Api33.matches(view,dispatcher)) {
                if(root.activityRoot){TikTokBack.systemCommit(activity);InstagramBack.systemCommit(activity);return tryBack(activity);}
                if((TikTokScreen.owns(activity)||InstagramBack.owns(activity))&&root.dialog!=null)return tryDialogBack(root.dialog.get());
            }
        }
        return false;
    }
    private static void nativeBack(Activity activity, Object dispatcher) {
        enterBack(activity);
        try {
            if(!(Build.VERSION.SDK_INT>=33 && NativeBack.forward(dispatcher))) {
                if(activity!=null&&!activity.isDestroyed())activity.onBackPressed();
            }
            afterNative();
        } finally { leaveNative(); }
    }

    public static boolean tryTaskTerminal(android.app.ActivityManager.AppTask task) {
        return Looper.myLooper()==Looper.getMainLooper()&&(TikTokBack.terminalTask(task)||InstagramBack.terminalTask(task));
    }
    static void tiktokNative(Activity activity) {
        Object dispatcher=null;
        try{if(Build.VERSION.SDK_INT>=33&&activity.getWindow()!=null){View root=activity.getWindow().peekDecorView();if(root!=null)dispatcher=root.findOnBackInvokedDispatcher();}}catch(RuntimeException ignored){}
        nativeBack(activity,dispatcher);
    }
    static Activity tiktokMain(Activity from) {
        for(State s:new ArrayList<State>(STATES)) {
            Activity a=s.activity.get();
            if(a!=null&&a!=from&&!s.destroyed&&!s.closing&&!a.isDestroyed()&&!a.isFinishing()
                &&TikTokScreen.main(a)&&a.getTaskId()==from.getTaskId()&&a.getApplication()==from.getApplication())return a;
        }
        return null;
    }
    private static Activity focusedActivity(Object dispatcher) {
        for(Root r:new ArrayList<Root>(ROOTS)) {
            View v=r.view.get();Activity a=r.owner.activity.get();
            if(r.activityRoot&&v!=null&&a!=null&&!r.released&&!r.detached&&v.hasWindowFocus()
                &&Build.VERSION.SDK_INT>=33&&Api33.matches(v,dispatcher))return a;
        }
        return null;
    }
    static boolean isTikTokDispatcher(Object dispatcher) {
        if(Build.VERSION.SDK_INT<33)return false;
        for(State s:new ArrayList<State>(STATES)){Activity a=s.activity.get();
            if((TikTokScreen.owns(a)||InstagramBack.owns(a))&&!s.destroyed&&!a.isDestroyed())try{
                if(a.getOnBackInvokedDispatcher()==dispatcher)return true;
                View v=a.getWindow()==null?null:a.getWindow().peekDecorView();
                if(v!=null&&v.findOnBackInvokedDispatcher()==dispatcher)return true;
            }catch(RuntimeException ignored){}
        }
        return false;
    }
    static void predictionStart(Object dispatcher){Activity a=focusedActivity(dispatcher);TikTokBack.predictionStart(a);InstagramBack.predictionStart(a);}
    static boolean suppressNativeAnimation(Object dispatcher){return InstagramBack.exitPrediction(focusedActivity(dispatcher));}
    static void predictionEnd(Object dispatcher){Activity a=focusedActivity(dispatcher);TikTokBack.predictionEnd(a);InstagramBack.predictionEnd(a);}
    private static boolean commentRoot(Root root) {
        Activity a=root.owner.activity.get();View v=root.view.get();
        if(root.activityRoot||v==null||root.dialog==null||root.dialog.get()==null)return false;
        if(InstagramBack.owns(a))return InstagramBack.window(v).comments&&InstagramBack.ready(v);
        if(!TikTokScreen.owns(a))return false;
        TikTokScreen.Snapshot page=TikTokScreen.window(v);
        return page.comment&&!page.editor&&!page.unknown;
    }
    private static boolean allowed(String name) {
        return "com.ss.android.ugc.trill".equals(name)
                || "com.zhiliaoapp.musically".equals(name)
                || "com.instagram.android".equals(name)
                || "com.twitter.android".equals(name)
                || "ctrip.english".equals(name);
    }

    private static boolean owned(Activity activity) {
        return activity != null && allowed(activity.getPackageName());
    }

    private static void main(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) action.run();
        else MAIN.post(action);
    }

    private static final class Scope {
        final WeakReference<Application> application;
        final String packageName;
        final Application.ActivityLifecycleCallbacks lifecycle;
        boolean registered;
        boolean scanPending;
        boolean exiting;

        Scope(Application app, String name) {
            application = new WeakReference<Application>(app);
            packageName = name;
            lifecycle = new Lifecycle();
        }
    }

    private static final class State {
        final WeakReference<Activity> activity;
        final Scope scope;
        long lastActive;
        boolean resumed;
        boolean closing;
        boolean destroyed;
        int keyAction;

        State(Activity value, Scope owner) {
            activity = new WeakReference<Activity>(value);
            scope = owner;
            lastActive = ++sequence;
        }
    }

    private static final class Root {
        final WeakReference<View> view;
        final State owner;
        final View.OnAttachStateChangeListener attachListener;
        final ViewTreeObserver.OnWindowFocusChangeListener focusListener;
        final ViewTreeObserver.OnPreDrawListener drawListener;
        long lastProbe;
        WeakReference<Dialog> dialog;
        WeakReference<ViewTreeObserver> observer;
        boolean activityRoot;
        boolean detached;
        boolean released;
        Object registration;

        Root(View root, State state) {
            view = new WeakReference<View>(root);
            owner = state;
            detached = !root.isAttachedToWindow();
            attachListener = new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View ignored) {
                    if (released || owner.destroyed) return;
                    detached = false;
                    observeFocus(Root.this);
                    register(Root.this, true);
                    scanLater(owner.scope);
                }

                @Override public void onViewDetachedFromWindow(View ignored) {
                    detached = true;
                    unregister(Root.this);
                    unobserveFocus(Root.this);
                    // Keep the weak attach listener: a reused Dialog may attach this root again.
                }
            };
            drawListener = new ViewTreeObserver.OnPreDrawListener() {
                @Override public boolean onPreDraw() {
                    Activity activity=owner.activity.get();
                    if(activity!=null && HomeScreen.restricted(activity.getPackageName())) {
                        long now=android.os.SystemClock.uptimeMillis();
                        if(now-lastProbe>=120) { lastProbe=now;register(Root.this,false); }
                    }
                    return true;
                }
            };
            focusListener = new ViewTreeObserver.OnWindowFocusChangeListener() {
                @Override public void onWindowFocusChanged(boolean focused) {
                    if (released || owner.destroyed || owner.closing) return;
                    if (focused) register(Root.this, true);
                    // Focus loss is also important: a new Dialog/Popup now owns the BACK event.
                    scanLater(owner.scope);
                }
            };
        }
    }

    private static Scope scope(Activity activity) {
        Application app = activity.getApplication();
        if (app == null) return null;
        Scope found = null;
        for (Iterator<Scope> it = SCOPES.iterator(); it.hasNext();) {
            Scope candidate = it.next();
            Application existing = candidate.application.get();
            if (existing == null) it.remove();
            else if (existing == app) found = candidate;
        }
        if (found == null) {
            found = new Scope(app, activity.getPackageName());
            SCOPES.add(found);
        }
        if (!found.registered) {
            try {
                app.registerActivityLifecycleCallbacks(found.lifecycle);
                found.registered = true;
            } catch (RuntimeException ignored) { /* Direct activity hooks still work. */ }
        }
        return found;
    }

    private static State state(Activity activity, boolean create) {
        for (Iterator<State> it = STATES.iterator(); it.hasNext();) {
            State candidate = it.next();
            Activity existing = candidate.activity.get();
            if (existing == null) it.remove();
            else if (existing == activity) return candidate;
        }
        if (!create || !owned(activity) || activity.isDestroyed()) return null;
        Scope owner = scope(activity);
        if (owner == null) return null;
        State value = new State(activity, owner);
        STATES.add(value);
        return value;
    }

    /** Called from patched Activity creation; registering the lifecycle covers later activities. */
    public static void install(final Activity activity) {
        if (!owned(activity)) return;
        main(new Runnable() {
            @Override public void run() { installNow(activity, false); }
        });
    }

    /** Called after app resume code so equal-priority callbacks cannot retain precedence. */
    public static void onResume(final Activity activity) {
        if (!owned(activity)) return;
        main(new Runnable() {
            @Override public void run() { installNow(activity, true); }
        });
    }

    public static void onNewIntent(Activity activity,Intent intent) {
        if(Looper.myLooper()==Looper.getMainLooper())TwitterBack.newIntent(activity,intent);
    }

    private static void installNow(Activity activity, boolean resumed) {
        if (!owned(activity) || activity.isDestroyed()) return;
        TwitterBack.installed(activity);
        if(resumed){TripBack.resumed(activity);TikTokBack.resumed(activity);TwitterBack.resumed(activity);}
        State owner = state(activity, true);
        if(owner!=null&&resumed&&("ctrip.english".equals(activity.getPackageName())||TwitterBack.owns(activity))&&!activity.isFinishing())owner.closing=false;
        if (owner == null || owner.closing || owner.destroyed) return;
        if (resumed) {
            owner.resumed = true;
            owner.lastActive = ++sequence;
        }
        try {
            Window window = activity.getWindow();
            // The DEX hook may run at onCreate entry, before setTheme/requestWindowFeature.
            // Never force decor creation here; the post-lifecycle/attach hooks bind it later.
            if (window != null) bind(window.peekDecorView(), owner, true, null);
        } catch (RuntimeException ignored) { /* A no-display or tearing-down window has no UI. */ }
        scanLater(owner.scope);
    }

    private static final class Lifecycle implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityCreated(Activity activity, Bundle saved) { install(activity); }
        @Override public void onActivityStarted(Activity activity) { install(activity); }
        @Override public void onActivityResumed(Activity activity) { onResume(activity); }
        @Override public void onActivityPaused(Activity activity) {
            State owner = state(activity, false);
            if (owner != null) owner.resumed = false;
        }
        @Override public void onActivityStopped(Activity activity) { onStopped(activity); }
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle saved) { }
        @Override public void onActivityDestroyed(Activity activity) { onDestroy(activity); }
        // Extra virtual methods are harmless below API 29; there are no calls to new API types here.
        @Override public void onActivityPostCreated(Activity activity, Bundle saved) { install(activity); }
        @Override public void onActivityPostResumed(Activity activity) { onResume(activity); }
    }

    private static Activity contextActivity(Context context) {
        for (int depth = 0; depth < 24 && context != null; depth++) {
            if (context instanceof Activity) {
                Activity result = (Activity) context;
                return owned(result) ? result : null;
            }
            if (!(context instanceof ContextWrapper)) return null;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) return null;
            context = next;
        }
        return null;
    }

    private static State bestState(String packageName) {
        State best = null;
        for (State value : STATES) {
            Activity activity = value.activity.get();
            if (!packageName.equals(value.scope.packageName) || value.closing || value.destroyed
                    || activity == null || activity.isDestroyed()) continue;
            if (best == null || (value.resumed && !best.resumed)
                    || (value.resumed == best.resumed && value.lastActive > best.lastActive)) best = value;
        }
        return best;
    }

    private static Activity contextOwner(Context context) {
        Activity activity = contextActivity(context);
        if (activity != null && !activity.isDestroyed()) return activity;
        if (context == null || !allowed(context.getPackageName())) return null;
        State best = bestState(context.getPackageName());
        return best == null ? null : best.activity.get();
    }

    private static Activity dialogOwner(Dialog dialog) {
        Activity explicit = dialog.getOwnerActivity();
        if (owned(explicit) && !explicit.isDestroyed()) return explicit;
        return contextOwner(dialog.getContext());
    }

    /** Call after Dialog.show(), including the Dialog returned by Builder.show(). */
    public static void dialogShown(final Dialog dialog) {
        if (dialog == null) return;
        main(new Runnable() {
            @Override public void run() {
                Activity activity = dialogOwner(dialog);
                if (!owned(activity)) return;
                installNow(activity, false);
                State owner = state(activity, false);
                if (owner == null || owner.closing || owner.destroyed) return;
                try {
                    Window window = dialog.getWindow();
                    if (window != null) bind(window.getDecorView(), owner, false, dialog);
                } catch (RuntimeException ignored) { /* The dialog may already be dismissed. */ }
            }
        });
    }

    private static void bind(View view, State owner, boolean activityRoot, Dialog dialog) {
        if (view == null || owner.destroyed || owner.closing) return;
        Root found = null;
        for (Iterator<Root> it = ROOTS.iterator(); it.hasNext();) {
            Root candidate = it.next();
            View existing = candidate.view.get();
            if (existing == null || candidate.owner.destroyed || candidate.owner.activity.get() == null) {
                release(candidate);
                it.remove();
            } else if (existing == view) {
                if (candidate.owner != owner) {
                    release(candidate);
                    it.remove();
                } else found = candidate;
            }
        }
        if (found == null) {
            found = new Root(view, owner);
            ROOTS.add(found);
            view.addOnAttachStateChangeListener(found.attachListener);
        }
        found.activityRoot |= activityRoot;
        if (dialog != null) found.dialog = new WeakReference<Dialog>(dialog);
        found.detached = !view.isAttachedToWindow();
        observeFocus(found);
        register(found, true);
    }

    private static void observeFocus(Root root) {
        View view = root.view.get();
        if (view == null || root.released) return;
        ViewTreeObserver observer = view.getViewTreeObserver();
        ViewTreeObserver previous = root.observer == null ? null : root.observer.get();
        if (previous == observer) return;
        unobserveFocus(root);
        if (observer != null && observer.isAlive()) {
            observer.addOnWindowFocusChangeListener(root.focusListener);
            Activity activity=root.owner.activity.get();
            if(activity!=null&&HomeScreen.restricted(activity.getPackageName()))observer.addOnPreDrawListener(root.drawListener);
            root.observer = new WeakReference<ViewTreeObserver>(observer);
        }
    }

    private static void unobserveFocus(Root root) {
        ViewTreeObserver observer = root.observer == null ? null : root.observer.get();
        root.observer = null;
        if (observer != null && observer.isAlive()) {
            try { observer.removeOnWindowFocusChangeListener(root.focusListener); observer.removeOnPreDrawListener(root.drawListener); }
            catch (RuntimeException ignored) { }
        }
    }

    private static void scanLater(final Scope scope) {
        if (Build.VERSION.SDK_INT < 29 || scope.scanPending) return;
        scope.scanPending = true;
        MAIN.post(new Runnable() {
            @Override public void run() {
                scope.scanPending = false;
                if (scope.exiting || bestState(scope.packageName) == null) return;
                List<View> windows;
                try { windows = Api29.roots(); }
                catch (RuntimeException ignored) { return; }
                for (View root : windows) {
                    try {
                        if (root == null || !root.isAttachedToWindow()
                                || !scope.packageName.equals(root.getContext().getPackageName())) continue;
                        Activity activity = contextOwner(root.getContext());
                        if (activity == null) continue;
                        State owner = state(activity, true);
                        if (owner == null || owner.scope != scope) continue;
                        Window window = activity.getWindow();
                        bind(root, owner, window != null && window.peekDecorView() == root, null);
                    } catch (RuntimeException ignored) {
                        // One disappearing Window must not prevent binding the other roots.
                    }
                }
            }
        });
    }

    private static void register(Root root, boolean rearm) {
        if (Build.VERSION.SDK_INT < 33 || root.released || root.owner.closing || root.owner.destroyed) return;
        Activity activity=root.owner.activity.get();
        if(activity!=null && HomeScreen.restricted(activity.getPackageName())
                && ((!root.activityRoot&&!commentRoot(root)) || (root.activityRoot&&!(action(activity)!=0 || TripBack.observe(activity))))) { unregister(root);return; }
        try { root.registration = Api33.register(root, root.registration, rearm); }
        catch (RuntimeException ignored) { /* Legacy hooks remain usable if this window rejects registration. */ }
    }

    private static void unregister(Root root) {
        Object previous = root.registration;
        root.registration = null;
        if (Build.VERSION.SDK_INT >= 33 && previous != null) Api33.unregister(previous);
    }

    private static void release(Root root) {
        if (root.released) return;
        root.released = true;
        OneBackExit.unregister(root);
        unobserveFocus(root);
        View view = root.view.get();
        if (view != null) {
            try { view.removeOnAttachStateChangeListener(root.attachListener); }
            catch (RuntimeException ignored) { }
        }
    }

    public static void onUserLeaveHint(Activity activity) { TwitterBack.cancel(activity);TripBack.cancel(activity);TikTokBack.cancel(activity);InstagramBack.cancel(activity); }
    public static void onUserInteraction(Activity activity) { TwitterBack.interaction(activity);TripBack.cancel(activity);TikTokBack.interaction(activity);InstagramBack.interaction(activity); }
    public static void onStopped(Activity activity) {
        State owner=state(activity,false);if(owner!=null)owner.resumed=false;
        TripBack.stopped(activity);TwitterBack.stopped(activity);
    }
    static boolean anyResumed(String pkg) {
        for(State s:STATES){Activity a=s.activity.get();if(s.resumed&&!s.closing&&!s.destroyed&&a!=null
            &&pkg.equals(a.getPackageName())&&!a.isDestroyed()&&!a.isFinishing())return true;}
        return false;
    }
    static void terminalStopped(Activity activity) { backNow(activity); }
    public static void onDestroy(final Activity activity) {
        TwitterBack.destroyed(activity);TripBack.destroyed(activity);TikTokBack.cancel(activity);InstagramBack.cancel(activity);
        if (!owned(activity)) return;
        main(new Runnable() {
            @Override public void run() {
                State owner = state(activity, false);
                if (owner == null) return;
                owner.destroyed = true;
                owner.resumed = false;
                for (Iterator<Root> it = ROOTS.iterator(); it.hasNext();) {
                    Root root = it.next();
                    if (root.owner == owner) { release(root); it.remove(); }
                }
                STATES.remove(owner);
            }
        });
    }

    /** Preserve a whole non-home key sequence; never exit on a release whose down was delegated. */
    public static boolean key(Activity activity, KeyEvent event) {
        if (!owned(activity)||event==null||event.getKeyCode()!=KeyEvent.KEYCODE_BACK||nativePassThrough())return false;
        if (!HomeScreen.restricted(activity.getPackageName())) {
            if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())back(activity);return true;
        }
        State state=state(activity,true);if(state==null)return false;
        if(event.getAction()==KeyEvent.ACTION_DOWN) {
            if(event.getRepeatCount()==0){TikTokBack.freshKey(activity);InstagramBack.freshKey(activity);state.keyAction=action(activity);}
            return state.keyAction!=0;
        }
        if(event.getAction()!=KeyEvent.ACTION_UP)return false;
        int armed=state.keyAction;state.keyAction=0;
        if(armed!=0&&!event.isCanceled()&&action(activity)==armed)tryBack(activity);
        return armed!=0;
    }
    public static boolean dialogKey(Dialog dialog, KeyEvent event) {
        if(dialog!=null&&event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_BACK&&"com.instagram.android".equals(dialog.getContext().getPackageName())&&!nativePassThrough()){
            if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())return InstagramBack.dialog(dialog,dialogOwner(dialog));
            return false;
        }
        if(dialog!=null&&event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_BACK&&TikTokScreen.owns(dialog.getContext().getPackageName())&&!nativePassThrough()){
            Activity owner=dialogOwner(dialog);TikTokBack.protect(owner);
            if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())return TikTokBack.dialog(dialog,owner);
            return false;
        }
        if(dialog==null||event==null||event.getKeyCode()!=KeyEvent.KEYCODE_BACK||nativePassThrough()
            ||HomeScreen.restricted(dialog.getContext().getPackageName())||!allowed(dialog.getContext().getPackageName()))return false;
        if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())dialogBack(dialog);return true;
    }
    public static void dialogBack(final Dialog dialog) {
        if(dialog==null||HomeScreen.restricted(dialog.getContext().getPackageName()))return;
        main(new Runnable(){@Override public void run(){tryDialogBack(dialog);}});
    }
    /** Social applications always leave pre-IME BACK to the keyboard/app. */
    public static boolean keyFromView(final View view,KeyEvent event) {
        if(view==null||event==null||event.getKeyCode()!=KeyEvent.KEYCODE_BACK||nativePassThrough()
            ||HomeScreen.restricted(view.getContext().getPackageName())||!allowed(view.getContext().getPackageName()))return false;
        if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled()) {
            main(new Runnable(){@Override public void run(){tryBack(contextOwner(view.getContext()));}});
        }
        return true;
    }
    public static void back(final Activity activity) {
        if(!owned(activity))return;
        main(new Runnable(){@Override public void run(){tryBack(activity);}});
    }

    private static void backNow(Activity activity) {
        if (!owned(activity) || activity.isDestroyed()) return;
        State source = state(activity, true);
        if (source == null || source.closing || source.destroyed || source.scope.exiting) return;
        TwitterBack.prepareExit(activity);
        String packageName = source.scope.packageName;
        List<State> closing = new ArrayList<State>();
        for (State candidate : STATES) {
            Activity value = candidate.activity.get();
            if (packageName.equals(candidate.scope.packageName) && !candidate.destroyed
                    && value != null && !value.isDestroyed()) {
                candidate.closing = true;
                closing.add(candidate);
            }
        }
        Set<Integer> removed = new HashSet<Integer>();
        Set<Integer> targets = new HashSet<Integer>();
        source.scope.exiting = true;
        try {
            Set<Integer> foreign = new HashSet<Integer>();
            removeTasks(activity, packageName, removed, foreign);
            targets.addAll(removed);
            for(State candidate:closing) {Activity a=candidate.activity.get();
                if(a!=null&&a.isTaskRoot()&&!foreign.contains(a.getTaskId()))targets.add(a.getTaskId());}
            TripBack.consume(activity);
            // Root first avoids revealing an intermediate app screen when the task service failed.
            finishRemaining(closing, removed, foreign, true);
            finishRemaining(closing, removed, foreign, false);
            TripBack.retryTasks(activity,targets);TwitterBack.retryTasks(activity,targets);
        } finally {
            // A later, newly created Activity must be able to open and exit in this same process.
            source.scope.exiting = false;
            for (State candidate : closing) {
                Activity value = candidate.activity.get();
                try {
                    if (value != null && !value.isDestroyed() && !value.isFinishing()
                            && !removed.contains(Integer.valueOf(value.getTaskId()))) {
                        // If every exit API rejected the request, keep the visible Activity retryable.
                        candidate.closing = false;
                    }
                } catch (RuntimeException ignored) { candidate.closing = false; }
            }
        }
    }

    private static String taskPackage(ActivityManager.RecentTaskInfo info) {
        String actual = Build.VERSION.SDK_INT >= 29 ? Api29.basePackage(info) : null;
        if (actual != null) return actual;
        Intent intent = info.baseIntent;
        ComponentName component = intent == null ? null : intent.getComponent();
        if (component != null) return component.getPackageName();
        if (intent != null && intent.getPackage() != null) return intent.getPackage();
        return Build.VERSION.SDK_INT >= 29 ? Api29.basePackage(info) : null;
    }

    private static int taskId(ActivityManager.RecentTaskInfo info) {
        return Build.VERSION.SDK_INT >= 29 ? Api29.taskId(info)
                : (info.id >= 0 ? info.id : info.persistentId);
    }

    private static void removeTasks(Activity activity, String packageName,
            Set<Integer> removed, Set<Integer> foreign) {
        List<ActivityManager.AppTask> tasks;
        try {
            Object manager = activity.getSystemService(Context.ACTIVITY_SERVICE);
            if (!(manager instanceof ActivityManager)) return;
            List<ActivityManager.AppTask> values = ((ActivityManager) manager).getAppTasks();
            if (values == null) return;
            tasks = new ArrayList<ActivityManager.AppTask>(values);
        } catch (RuntimeException ignored) { return; }
        for (ActivityManager.AppTask task : tasks) {
            if (task == null) continue;
            try {
                ActivityManager.RecentTaskInfo info = task.getTaskInfo();
                if (info == null) continue;
                int id = taskId(info);
                String base = taskPackage(info);
                if (!packageName.equals(base)) {
                    if (base != null && id >= 0) foreign.add(Integer.valueOf(id));
                    continue;
                }
                // Exclude BEFORE removal: a successful/async removal request can still leave
                // a Recents card until the task service completes. Failure of either API
                // must not suppress attempting the other API.
                try { task.setExcludeFromRecents(true); } catch(RuntimeException ignored) { }
                try {
                    task.finishAndRemoveTask();
                    if (id >= 0) removed.add(Integer.valueOf(id));
                } catch (RuntimeException ignored) {
                    // finishRemaining still handles tracked Activities if removal failed.
                }
            } catch (RuntimeException ignored) {
                // A vanished first task must not prevent removal of the remaining app tasks.
            }
        }
    }

    private static void finishRemaining(List<State> closing, Set<Integer> removed,
            Set<Integer> foreign, boolean roots) {
        for (State state : closing) {
            Activity activity = state.activity.get();
            if (activity == null || activity.isDestroyed() || activity.isFinishing()) continue;
            try {
                boolean root = activity.isTaskRoot();
                if (root != roots) continue;
                int id = activity.getTaskId();
                if (id >= 0 && removed.contains(Integer.valueOf(id))) continue;
                if (root && !foreign.contains(Integer.valueOf(id))) {
                    try {
                        activity.finishAndRemoveTask();
                        if (id >= 0) removed.add(Integer.valueOf(id));
                        continue;
                    } catch (RuntimeException ignored) { /* Finish just our Activity below. */ }
                }
                // An Activity embedded in another package's task must not delete that task.
                activity.finish();
            } catch (RuntimeException ignored) { /* Continue with the other tracked activities. */ }
        }
    }

    private static final class Api29 {
        static List<View> roots() {
            return android.view.inspector.WindowInspector.getGlobalWindowViews();
        }
        static int taskId(ActivityManager.RecentTaskInfo info) { return info.taskId; }
        static String basePackage(ActivityManager.RecentTaskInfo info) {
            return info.baseActivity == null ? null : info.baseActivity.getPackageName();
        }
    }

    /** Keep API 33 types out of the common class's signatures and fields for older Android. */
    private static final class Api33 {
        private static final class Registration {
            final WeakReference<android.window.OnBackInvokedDispatcher> dispatcher;
            final android.window.OnBackInvokedCallback callback;

            Registration(android.window.OnBackInvokedDispatcher target, final Root root) {
                dispatcher = new WeakReference<android.window.OnBackInvokedDispatcher>(target);
                android.window.OnBackInvokedCallback committed = new android.window.OnBackInvokedCallback() {
                    @Override public void onBackInvoked() {
                        if (!root.released && !root.detached && !root.owner.destroyed && !root.owner.closing) {
                            Activity activity=root.owner.activity.get();
                            if(!root.activityRoot&&root.dialog!=null&&(TikTokScreen.owns(activity)||InstagramBack.owns(activity))) {
                                Dialog dialog=root.dialog.get();
                                if(dialog!=null){if(!tryDialogBack(dialog)){TikTokBack.protect(activity);enterNative();try{dialog.onBackPressed();}finally{leaveNative();}}return;}
                            }
                            TikTokBack.systemCommit(activity);InstagramBack.systemCommit(activity);
                            if(!tryBack(activity) && activity!=null && HomeScreen.restricted(activity.getPackageName())) {
                                OneBackExit.unregister(root);
                                nativeBack(activity, dispatcher.get());
                                MAIN.post(new Runnable(){@Override public void run(){OneBackExit.register(root,false);}});
                            }
                        }
                    }
                };
                Activity activity=root.owner.activity.get();
                callback=Build.VERSION.SDK_INT>=34&&(TikTokScreen.owns(activity)||InstagramBack.owns(activity))?Api34.predictive(target,committed):committed;
            }
        }

        static Object register(Root root, Object previous, boolean rearm) {
            View view = root.view.get();
            if (view == null) return previous;
            android.window.OnBackInvokedDispatcher dispatcher = view.findOnBackInvokedDispatcher();
            if (dispatcher == null && root.activityRoot) {
                Activity activity = root.owner.activity.get();
                if (activity != null) dispatcher = activity.getOnBackInvokedDispatcher();
            }
            if (dispatcher == null && root.dialog != null) {
                Dialog dialog = root.dialog.get();
                if (dialog != null) dispatcher = dialog.getOnBackInvokedDispatcher();
            }
            if (dispatcher == null) return previous;
            Registration value = previous == null ? null : (Registration) previous;
            if (value == null || value.dispatcher.get() != dispatcher) {
                if (value != null) unregister(value);
                value = new Registration(dispatcher, root);
                rearm = true;
            }
            // The public API explicitly reorders an already-registered instance to the end.
            // Re-register directly: a separate unregister also cancels an in-progress gesture.
            if (rearm) {
                Activity activity=root.owner.activity.get();
                int priority=activity!=null&&HomeScreen.restricted(activity.getPackageName())
                    ? android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT
                    : android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY;
                dispatcher.registerOnBackInvokedCallback(priority,value.callback);
            }
            return value;
        }

        static boolean matches(View view,Object dispatcher) { return view.findOnBackInvokedDispatcher()==dispatcher; }
        static void unregister(Object previous) {
            Registration value = (Registration) previous;
            android.window.OnBackInvokedDispatcher dispatcher = value.dispatcher.get();
            if (dispatcher != null) {
                try { dispatcher.unregisterOnBackInvokedCallback(value.callback); }
                catch (RuntimeException ignored) { }
            }
        }
    }
    private static final class Api34 {
        static android.window.OnBackInvokedCallback predictive(final Object dispatcher,final android.window.OnBackInvokedCallback committed) {
            return new android.window.OnBackAnimationCallback() {
                public void onBackStarted(android.window.BackEvent e){predictionStart(dispatcher);}
                public void onBackProgressed(android.window.BackEvent e){}
                public void onBackCancelled(){predictionEnd(dispatcher);}
                public void onBackInvoked(){try{committed.onBackInvoked();}finally{predictionEnd(dispatcher);}}
            };
        }
    }

}
