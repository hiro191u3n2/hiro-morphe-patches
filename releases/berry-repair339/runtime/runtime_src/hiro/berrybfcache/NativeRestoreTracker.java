package hiro.berrybfcache;

import android.os.Looper;
import android.webkit.WebView;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/** Native Page identity proof; neither a timeout nor a successful JS callback is proof. */
public final class NativeRestoreTracker {
    private static final String BOUNDARY = "org.chromium.support_lib_boundary.";
    private static final int MAX_PAGES = 64;
    private static final WeakHashMap<WebView, WeakReference<State>> STATES = new WeakHashMap<WebView, WeakReference<State>>();
    private static final WeakHashMap<WebView, Boolean> ATTEMPTED = new WeakHashMap<WebView, Boolean>();

    private static final class State {
        final WeakReference<WebView> view;
        final ArrayList<PageRecord> pages = new ArrayList<PageRecord>();
        boolean installed;
        boolean disabled;
        boolean allowCreatePeer;
        long navigationId;
        long activeToken;
        long pendingToken;
        String currentUrl;
        String pendingUrl;
        PageRecord currentPage;
        InvocationHandler navigation;
        Object navigationPeer;
        Boundary api;
        State(WebView view) { this.view = new WeakReference<WebView>(view); }
        void invalidate() { activeToken = 0; pendingToken = 0; pendingUrl = null; }
    }

    /** Uses the boundary names already bundled in the app; never adds competing interfaces. */
    private static final class Boundary {
        final Method addListener;
        final Method sameDocument;
        final Method history;
        final Method reload;
        final Method committed;
        final Method errorPage;
        final Method url;
        final Method page;
        final Method peer;
        Boundary() throws Exception {
            ClassLoader loader = NativeRestoreTracker.class.getClassLoader();
            Class<?> provider = Class.forName(BOUNDARY + "WebViewProviderBoundaryInterface", false, loader);
            Class<?> navigation = Class.forName(BOUNDARY + "WebViewNavigationBoundaryInterface", false, loader);
            Class<?> pageType = Class.forName(BOUNDARY + "WebViewPageBoundaryInterface", false, loader);
            addListener = provider.getMethod("addWebViewNavigationListener", Executor.class, InvocationHandler.class);
            sameDocument = navigation.getMethod("isSameDocument");
            history = navigation.getMethod("isHistory");
            reload = navigation.getMethod("isReload");
            committed = navigation.getMethod("didCommit");
            errorPage = navigation.getMethod("didCommitErrorPage");
            url = navigation.getMethod("getUrl");
            page = navigation.getMethod("getPage");
            peer = pageType.getMethod("getOrCreatePeer", Callable.class);
        }
    }

    private static final class PageRecord {
        final Object peer;
        // Keeping the boundary wrapper alive preserves Chromium's weakly cached AwPage.
        @SuppressWarnings("unused") final InvocationHandler page;
        PageRecord(Object peer, InvocationHandler page) { this.peer = peer; this.page = page; }
    }

    private NativeRestoreTracker() {}

    /** Feature lookup precedes the provider factory; every optional API failure preserves JS fallback. */
    public static void install(WebView view, String featureClass, String featureMethod,
            String factoryClass, String factoryMethod, String providerField, boolean allowCreatePeer) {
        if (view == null || !onMainThread() || ATTEMPTED.containsKey(view)) return;
        State s = new State(view);
        ATTEMPTED.put(view, Boolean.TRUE);
        STATES.put(view, new WeakReference<State>(s));
        try {
            ClassLoader loader = NativeRestoreTracker.class.getClassLoader();
            Method supported = Class.forName(featureClass, false, loader).getMethod(featureMethod, String.class);
            if (!feature(supported, "NAVIGATION_LISTENER") && !feature(supported, "NAVIGATION_LISTENER_V1")) {
                unavailable(s);
                return;
            }
            // No provider creation on unsupported WebView versions, and no private-field access.
            Object adapter = Class.forName(factoryClass, false, loader).getMethod(factoryMethod, WebView.class).invoke(null, view);
            Object provider = adapter.getClass().getField(providerField).get(adapter);
            s.allowCreatePeer = allowCreatePeer;
            s.api = new Boundary();
            s.api.addListener.invoke(provider, new Executor() {
                @Override public void execute(Runnable command) {
                    // Keeping the callback synchronous on UI preserves native/legacy callback order.
                    if (onMainThread() && !s.disabled) command.run();
                    else if (!onMainThread()) unavailable(s);
                }
            }, new Listener(s));
            s.installed = true;
        } catch (Throwable error) {
            fatal(error);
            unavailable(s);
        }
    }

    /** Cross-document native start identity, also used to reject proof for an older legacy start. */
    public static long navigationId(WebView view) {
        if (view == null || !onMainThread()) return 0;
        State s = state(view);
        return usable(s) ? s.navigationId : 0;
    }

    /** A proof is consumed at most once, by the corresponding legacy onPageStarted. */
    public static long consume(WebView view, String startedUrl) {
        if (view == null || !onMainThread()) return 0;
        State s = state(view);
        if (!usable(s) || s.pendingToken == 0 || !equal(startedUrl, s.pendingUrl)
                || !isCurrent(view, s.pendingToken, startedUrl)) return 0;
        long token = s.pendingToken;
        s.pendingToken = 0;
        s.pendingUrl = null;
        return token;
    }

    /** Same-document commits may change the URL while retaining the positively proved Page. */
    public static boolean isCurrent(WebView view, long token, String currentUrl) {
        if (view == null || token == 0 || !onMainThread()) return false;
        State s = state(view);
        if (!usable(s) || token != s.activeToken || token != s.navigationId
                || s.currentPage == null || !equal(currentUrl, s.currentUrl)) return false;
        try { return equal(currentUrl, view.getUrl()); }
        catch (RuntimeException destroyed) { return false; }
    }

    private static final class Listener implements InvocationHandler {
        private final State state;
        Listener(State state) { this.state = state; }
        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getSupportedFeatures".equals(name)) return new String[] { "WEB_VIEW_NAVIGATION_LISTENER_V1" };
            if ("equals".equals(name)) return Boolean.valueOf(args != null && args.length == 1 && args[0] == proxy);
            if ("hashCode".equals(name)) return Integer.valueOf(System.identityHashCode(this));
            if ("toString".equals(name)) return "Berry native Page restore listener";
            if (state.disabled || state.view.get() == null) return null;
            if (!onMainThread()) {
                unavailable(state);
                return null;
            }
            if (args == null || args.length == 0 || !(args[0] instanceof InvocationHandler)) return null;
            InvocationHandler value = (InvocationHandler) args[0];
            try {
                if ("onNavigationStarted".equals(name)) {
                    if (!flag(value, state.api.sameDocument)) {
                        state.navigationId++;
                        if (state.navigationId <= 0) state.navigationId = 1;
                        state.invalidate();
                        state.navigationPeer = peer(state, value, state.allowCreatePeer);
                        state.navigation = state.navigationPeer == null ? null : value;
                    }
                } else if ("onNavigationCompleted".equals(name)) {
                    completed(state, value);
                } else if ("onPageDeleted".equals(name)) {
                    deleted(state, peer(state, value, false));
                }
            } catch (Throwable error) {
                fatal(error);
                unavailable(state);
            }
            return null;
        }
    }

    private static void completed(State s, InvocationHandler navigation) throws Throwable {
        boolean sameDocument = flag(navigation, s.api.sameDocument);
        if (!sameDocument) {
            Object navigationPeer = peer(s, navigation, false);
            if (s.navigation == null || navigationPeer == null || s.navigationPeer != navigationPeer) return;
            s.navigation = null;
            s.navigationPeer = null;
        }
        if (!flag(navigation, s.api.committed)) return;
        String url = (String) call(navigation, s.api.url);
        InvocationHandler page = (InvocationHandler) call(navigation, s.api.page);
        Object identity = page == null ? null : peer(s, page, s.allowCreatePeer);
        if (identity == null) {
            s.invalidate();
            s.currentPage = null;
            return;
        }
        Object previous = s.currentPage == null ? null : s.currentPage.peer;
        boolean known = remember(s, identity, page);
        s.currentUrl = url;
        if (sameDocument) {
            // A native same-document commit is the only basis for following an updated URL.
            if (previous != identity) s.invalidate();
            s.pendingToken = 0;
            s.pendingUrl = null;
            return;
        }
        s.invalidate();
        if (!known || s.navigationId == 0 || !flag(navigation, s.api.history)
                || flag(navigation, s.api.reload) || flag(navigation, s.api.errorPage) || url == null) return;
        s.activeToken = s.navigationId;
        s.pendingToken = s.activeToken;
        s.pendingUrl = url;
        WebView view = s.view.get();
        if (view != null) RestoreCoordinator.nativeRestored(view, url, s.activeToken);
    }

    private static Object peer(State s, InvocationHandler page, final boolean create) throws Throwable {
        return page.invoke(null, s.api.peer, new Object[] { new Callable<Object>() {
            @Override public Object call() { return create ? new Object() : null; }
        } });
    }

    /** Bounded native wrappers live with the provider listener, never through a static strong State. */
    private static boolean remember(State s, Object identity, InvocationHandler page) {
        boolean known = false;
        for (int i = s.pages.size() - 1; i >= 0; i--) {
            if (s.pages.get(i).peer == identity) {
                known = true;
                s.pages.remove(i);
            }
        }
        while (s.pages.size() >= MAX_PAGES) s.pages.remove(0);
        s.currentPage = new PageRecord(identity, page);
        s.pages.add(s.currentPage);
        return known;
    }

    private static void deleted(State s, Object page) {
        if (page == null) return;
        for (int i = s.pages.size() - 1; i >= 0; i--) {
            if (s.pages.get(i).peer == page) s.pages.remove(i);
        }
        if (s.currentPage != null && s.currentPage.peer == page) {
            s.currentPage = null;
            s.invalidate();
        }
    }

    private static State state(WebView view) {
        WeakReference<State> state = STATES.get(view);
        return state == null ? null : state.get();
    }

    private static Object call(InvocationHandler handler, Method method) throws Throwable {
        return handler.invoke(null, method, new Object[0]);
    }
    private static boolean flag(InvocationHandler handler, Method method) throws Throwable {
        return Boolean.TRUE.equals(call(handler, method));
    }
    private static boolean feature(Method method, String name) {
        try { return Boolean.TRUE.equals(method.invoke(null, name)); }
        catch (Throwable error) { fatal(error); return false; }
    }
    private static boolean equal(String a, String b) { return a != null && a.equals(b); }
    private static boolean usable(State s) { return s != null && s.installed && !s.disabled; }
    private static boolean onMainThread() { return Looper.myLooper() == Looper.getMainLooper(); }
    private static void unavailable(State s) {
        s.disabled = true;
        s.invalidate();
        s.pages.clear();
        s.currentPage = null;
        s.navigation = null;
        s.navigationPeer = null;
    }
    @SuppressWarnings("removal")
    private static void fatal(Throwable error) {
        if (error instanceof ThreadDeath) throw (ThreadDeath) error;
        if (error instanceof VirtualMachineError) throw (VirtualMachineError) error;
    }
}
