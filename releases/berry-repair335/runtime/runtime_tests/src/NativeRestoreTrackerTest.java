import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import hiro.berrybfcache.NativeRestoreTracker;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import org.chromium.support_lib_boundary.*;

/** Fake public boundary, including newly-created callback handlers and weak native wrapper caches. */
public class NativeRestoreTrackerTest {
    private static int assertions;
    private static int scenarios;
    public static final class FeatureCheck {
        public static boolean supported = true;
        public static boolean oldAlias;
        public static boolean check(String name) {
            if (oldAlias && "NAVIGATION_LISTENER".equals(name)) throw new IllegalArgumentException("unknown");
            return supported && (oldAlias ? "NAVIGATION_LISTENER_V1" : "NAVIGATION_LISTENER").equals(name);
        }
    }
    public static final class Adapter {
        public WebViewProviderBoundaryInterface boundary;
        public Adapter(Provider provider) { boundary = provider; }
    }
    public static final class Factory {
        public static Provider next;
        public static int calls;
        public static Adapter create(WebView view) { calls++; Provider value = next; next = null; return new Adapter(value); }
    }
    public static final class Provider implements WebViewProviderBoundaryInterface {
        public Executor executor;
        public InvocationHandler listener;
        public int installs;
        @Override public void addWebViewNavigationListener(Executor executor, InvocationHandler listener) {
            this.executor = executor; this.listener = listener; installs++;
        }
        public void event(String name, InvocationHandler value) {
            final Method method;
            try { method = WebViewNavigationListenerBoundaryInterface.class.getMethod(name, InvocationHandler.class); }
            catch (Exception error) { throw new AssertionError(error); }
            executor.execute(new Runnable() { public void run() {
                try { listener.invoke(null, method, new Object[] { value }); }
                catch (Throwable error) { throw new AssertionError(error); }
            } });
        }
    }
    private static final class PageWrapper {
        Object peer;
        final Page page;
        PageWrapper(Page page) { this.page = page; peer = page.foreignPeer; }
        InvocationHandler handler() {
            return new InvocationHandler() { public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("getOrCreatePeer".equals(method.getName())) {
                    checkPeerMethod(method);
                    if (peer == null) peer = ((Callable<?>) args[0]).call();
                    return peer;
                }
                if ("getUrl".equals(method.getName())) return page.url;
                throw new AssertionError(method);
            } };
        }
    }
    public static final class Page {
        public String url;
        public Object foreignPeer;
        private WeakReference<PageWrapper> wrapper = new WeakReference<PageWrapper>(null);
        private PageWrapper wrapper() {
            PageWrapper value = wrapper.get();
            if (value == null) { value = new PageWrapper(this); wrapper = new WeakReference<PageWrapper>(value); }
            return value;
        }
        public InvocationHandler handler() { return wrapper().handler(); }
        public Object currentPeer() { PageWrapper value = wrapper.get(); return value == null ? null : value.peer; }
    }
    public static final class Nav {
        public final Page page;
        public final String url;
        public boolean history;
        public boolean sameDocument;
        public boolean reload;
        public boolean error;
        public boolean committed = true;
        public Object foreignPeer;
        private Object peer;
        public Nav(Page page, String url) { this.page = page; this.url = url; }
        public InvocationHandler handler() {
            return new InvocationHandler() { public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("getOrCreatePeer".equals(name)) {
                    checkPeerMethod(method);
                    if (peer == null) peer = foreignPeer == null ? ((Callable<?>) args[0]).call() : foreignPeer;
                    return peer;
                }
                if ("getPage".equals(name)) return page == null ? null : page.handler();
                if ("getUrl".equals(name)) return url;
                if ("isHistory".equals(name)) return Boolean.valueOf(history);
                if ("isSameDocument".equals(name)) return Boolean.valueOf(sameDocument);
                if ("isReload".equals(name)) return Boolean.valueOf(reload);
                if ("didCommit".equals(name)) return Boolean.valueOf(committed);
                if ("didCommitErrorPage".equals(name)) return Boolean.valueOf(error);
                throw new AssertionError(method);
            } };
        }
    }
    public static final class Fixture {
        public final WebView view;
        public final Provider provider;
        public Fixture(String url, boolean allowCreatePeer) {
            FeatureCheck.supported = true; FeatureCheck.oldAlias = false;
            view = new WebView(url); provider = new Provider(); install(view, provider, allowCreatePeer);
        }
        public void start(Nav nav) { provider.event("onNavigationStarted", nav.handler()); }
        public void complete(Nav nav) {
            if (nav.committed) { view.url = nav.url; if (nav.page != null) nav.page.url = nav.url; }
            provider.event("onNavigationCompleted", nav.handler());
        }
        public void visit(Nav nav) { start(nav); complete(nav); }
        public void delete(Page page) { provider.event("onPageDeleted", page.handler()); }
    }
    private static void install(WebView view, Provider provider, boolean create) {
        Factory.next = provider;
        NativeRestoreTracker.install(view, FeatureCheck.class.getName(), "check", Factory.class.getName(), "create", "boundary", create);
    }
    private static void checkPeerMethod(Method method) {
        if (method.getDeclaringClass() != IsomorphicObjectBoundaryInterface.class) throw new AssertionError("incorrect boundary declaring class");
    }
    private static void check(boolean value, String description) {
        assertions++; if (!value) throw new AssertionError(description);
    }
    private static Nav back(Page page, String url) { Nav n = new Nav(page, url); n.history = true; return n; }
    private static Object state(WebView view) throws Exception {
        Field field = NativeRestoreTracker.class.getDeclaredField("STATES"); field.setAccessible(true);
        return ((WeakReference<?>) ((Map<?, ?>) field.get(null)).get(view)).get();
    }
    private static Object field(Object value, String name) throws Exception {
        Field field = value.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(value);
    }
    private static void basicProof() throws Throwable {
        scenarios++;
        Fixture f = new Fixture("https://a/", true); Page a = new Page(); Page b = new Page();
        f.visit(new Nav(a, "https://a/"));
        check(NativeRestoreTracker.consume(f.view, f.view.url) == 0, "initial visit is not a restore");
        Object identity = a.currentPeer();
        for (int i = 0; i < 3; i++) System.gc();
        check(a.currentPeer() == identity, "retained page handler preserves weak native wrapper across GC");
        f.visit(new Nav(b, "https://b/")); f.visit(back(a, "https://a/"));
        long token = NativeRestoreTracker.navigationId(f.view);
        check(token > 0 && NativeRestoreTracker.consume(f.view, "https://a/") == token, "known exact Page history is native proof");
        check(NativeRestoreTracker.consume(f.view, "https://a/") == 0, "consume is one-shot");
        check(NativeRestoreTracker.isCurrent(f.view, token, "https://a/"), "active proof survives consume");
        check(!NativeRestoreTracker.isCurrent(f.view, token, "https://wrong/"), "wrong URL cannot use proof");
        Nav alias = new Nav(a, "https://a/#next"); alias.sameDocument = true; f.visit(alias);
        check(NativeRestoreTracker.navigationId(f.view) == token, "same-document does not advance native ID");
        check(NativeRestoreTracker.isCurrent(f.view, token, alias.url), "same native Page follows URL");
        check(!NativeRestoreTracker.isCurrent(f.view, token, "https://a/"), "old URL rejected after alias");
        f.delete(a);
        check(!NativeRestoreTracker.isCurrent(f.view, token, alias.url), "deletion invalidates active proof");
        f.visit(back(a, "https://a/"));
        check(NativeRestoreTracker.consume(f.view, "https://a/") == 0, "deleted Page no longer known");
        check(f.provider.installs == 1, "additive listener installed once");
        install(f.view, new Provider(), true);
        check(f.provider.installs == 1, "repeat install is ignored");
        String[] features = (String[]) f.provider.listener.invoke(null,
            WebViewNavigationListenerBoundaryInterface.class.getMethod("getSupportedFeatures"), null);
        check(features.length == 1 && "WEB_VIEW_NAVIGATION_LISTENER_V1".equals(features[0]), "exact provider listener capability");
    }
    private static void staleAndNegativeProofs() throws Exception {
        scenarios++;
        Fixture f = new Fixture("https://same/", true); Page a = new Page(); f.visit(new Nav(a, f.view.url));
        Nav older = back(a, f.view.url); Nav newer = back(a, f.view.url);
        f.start(older); f.start(newer); long newerId = NativeRestoreTracker.navigationId(f.view);
        f.complete(older);
        check(NativeRestoreTracker.consume(f.view, f.view.url) == 0, "old same-URL completion cannot prove newer navigation");
        f.complete(newer);
        check(NativeRestoreTracker.consume(f.view, f.view.url) == newerId, "matching newer navigation can prove restore");
        f.complete(newer);
        check(NativeRestoreTracker.consume(f.view, f.view.url) == 0, "duplicate completion cannot mint a new pending proof");
        check(field(state(f.view), "navigation") == null, "completed navigation handler released");
        Nav cancellation = back(a, f.view.url); cancellation.committed = false; f.visit(cancellation);
        check(!NativeRestoreTracker.isCurrent(f.view, newerId, f.view.url), "new cross-document start invalidates old token");
        check(field(state(f.view), "navigation") == null, "cancelled navigation handler released");
        for (int kind = 0; kind < 4; kind++) {
            Nav n = back(kind == 0 ? new Page() : a, f.view.url);
            if (kind == 1) n.reload = true; if (kind == 2) n.error = true; if (kind == 3) n.history = false;
            f.visit(n); check(NativeRestoreTracker.consume(f.view, f.view.url) == 0, "cache miss/reload/error/normal load cannot prove restore " + kind);
        }
        f.visit(back(a, f.view.url)); long token = NativeRestoreTracker.consume(f.view, f.view.url);
        Nav inconsistent = new Nav(new Page(), f.view.url); inconsistent.sameDocument = true; f.visit(inconsistent);
        check(!NativeRestoreTracker.isCurrent(f.view, token, f.view.url), "same-document callback for a different Page invalidates proof");
    }
    private static void lifecycleAndForeignPeers() throws Exception {
        scenarios++;
        Fixture f = new Fixture("https://p/0", true); Page first = new Page(); f.visit(new Nav(first, f.view.url));
        for (int i = 1; i <= 80; i++) f.visit(new Nav(new Page(), "https://p/" + i));
        check(((List<?>) field(state(f.view), "pages")).size() == 64, "page retention is bounded");
        f.visit(back(first, "https://p/0")); check(NativeRestoreTracker.consume(f.view, f.view.url) == 0, "evicted page falls back safely");
        Field states = NativeRestoreTracker.class.getDeclaredField("STATES"); states.setAccessible(true);
        check(((Map<?, ?>) states.get(null)).get(f.view) instanceof WeakReference, "static map does not strongly retain state/provider/WebView");
        Fixture noCreate = new Fixture("https://foreign/", false); Page empty = new Page(); Nav emptyNav = new Nav(empty, noCreate.view.url);
        noCreate.visit(emptyNav); noCreate.visit(back(empty, noCreate.view.url));
        check(empty.currentPeer() == null && emptyNav.peer == null, "unsafe empty Page and Navigation slots stay untouched");
        check(NativeRestoreTracker.consume(noCreate.view, noCreate.view.url) == 0, "unknown foreign peer falls back");
        Page foreign = new Page(); foreign.foreignPeer = new Object();
        Nav firstNav = new Nav(foreign, noCreate.view.url); firstNav.foreignPeer = new Object(); noCreate.visit(firstNav);
        Nav restore = back(foreign, noCreate.view.url); restore.foreignPeer = new Object(); noCreate.visit(restore);
        check(NativeRestoreTracker.consume(noCreate.view, noCreate.view.url) > 0, "existing foreign peers may be compared without replacement");
        check(foreign.currentPeer() == foreign.foreignPeer && restore.peer == restore.foreignPeer, "existing peer objects preserved");
    }
    private static void unsupportedAndThreadGuard() throws Exception {
        scenarios++;
        WebView view = new WebView("https://unsupported/"); Provider provider = new Provider();
        FeatureCheck.supported = false; FeatureCheck.oldAlias = false;
        int calls = Factory.calls, warnings = Log.warnings; install(view, provider, true);
        check(Factory.calls == calls && provider.listener == null, "unsupported feature never touches provider factory");
        System.gc(); install(view, provider, true);
        check(Log.warnings == warnings + 1 && Factory.calls == calls, "unsupported install/logging stays once after GC");
        check(NativeRestoreTracker.navigationId(view) == 0 && NativeRestoreTracker.consume(view, view.url) == 0, "unsupported reports no native proof");
        FeatureCheck.supported = true; FeatureCheck.oldAlias = true;
        WebView oldAlias = new WebView("https://alias/"); Provider aliasProvider = new Provider(); install(oldAlias, aliasProvider, true);
        check(aliasProvider.listener != null, "old public feature alias accepted when new name is unknown");
        Fixture f = new Fixture("https://ui/", true); Looper.background = true;
        f.start(new Nav(new Page(), f.view.url)); Looper.background = false;
        check(NativeRestoreTracker.navigationId(f.view) == 0, "off-UI callback disables native proof instead of reordering it");
    }
    private static void optionalApiFailures() throws Exception {
        scenarios++;
        FeatureCheck.supported = true; FeatureCheck.oldAlias = false;
        WebView missing = new WebView("https://missing/"); int warnings = Log.warnings;
        NativeRestoreTracker.install(missing, FeatureCheck.class.getName(), "check", Factory.class.getName(), "missingMethod", "boundary", true);
        NativeRestoreTracker.install(missing, FeatureCheck.class.getName(), "check", Factory.class.getName(), "missingMethod", "boundary", true);
        check(Log.warnings == warnings + 1 && NativeRestoreTracker.navigationId(missing) == 0, "missing optional provider API logs once and returns no proof");
        Fixture f = new Fixture("https://broken/", true); Page page = new Page(); f.visit(new Nav(page, f.view.url));
        warnings = Log.warnings;
        InvocationHandler broken = new InvocationHandler() { public Object invoke(Object proxy, Method method, Object[] args) {
            throw new IllegalStateException("provider callback unavailable");
        } };
        f.provider.event("onNavigationStarted", broken); f.provider.event("onNavigationStarted", broken);
        check(Log.warnings == warnings + 1 && NativeRestoreTracker.navigationId(f.view) == 0, "broken native callback disables only optional path and logs once");
        check(((List<?>) field(state(f.view), "pages")).isEmpty(), "callback failure releases retained Page handlers");
    }
    public static void main(String[] args) throws Throwable {
        basicProof(); staleAndNegativeProofs(); lifecycleAndForeignPeers(); unsupportedAndThreadGuard(); optionalApiFailures();
        System.out.println("{\"result\":\"PASS\",\"scenarios\":" + scenarios + ",\"assertions\":" + assertions + "}");
    }
}
