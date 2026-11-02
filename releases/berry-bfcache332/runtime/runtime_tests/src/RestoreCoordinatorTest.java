import android.webkit.WebView;
import hiro.berrybfcache.RestoreCoordinator;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RestoreCoordinatorTest {
    private static final String URL = "https://example.test/list";
    private static final String OTHER = "https://example.test/product";
    private static int assertions;
    private static int scenarios;
    public interface Boundary {
        void onPostMessage(WebView view, InvocationHandler message, Object origin, boolean mainFrame, InvocationHandler reply);
        String[] getSupportedFeatures();
    }
    private static final Method POST;
    static {
        try { POST = Boundary.class.getMethod("onPostMessage", WebView.class, InvocationHandler.class, Object.class, boolean.class, InvocationHandler.class); }
        catch (Exception error) { throw new RuntimeException(error); }
    }
    private static class Counter implements Runnable { int n; public void run() { n++; } }
    private static void check(boolean ok, String message) {
        assertions++; if (!ok) throw new AssertionError(message);
    }
    private static String marker(String doc, int serial, boolean pending) {
        return "\"B1|" + doc + "|" + serial + "|" + (pending ? "1" : "0") + "\"";
    }
    private static void wake(WebView view) throws Throwable { send(view, true, "hiro-bfcache-restored-v1"); }
    private static void send(WebView view, boolean main, final String payload) throws Throwable {
        InvocationHandler data = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) { return payload; }
        };
        RestoreCoordinator.listener().invoke(null, POST, new Object[] {view, data, null, Boolean.valueOf(main), null});
    }
    private static WebView begin(Counter counter) {
        WebView view = new WebView(URL);
        RestoreCoordinator.started(view, URL, counter);
        view.advance(120);
        return view;
    }
    private static void confirm(WebView view, int first, String doc, int serial) {
        view.answer(first, marker(doc,serial,true));
        view.answer(first+1, marker(doc,serial,true));
    }
    public static void main(String[] args) throws Throwable {
        if (args.length > 0) {
            Path directory = Path.of(args[0]); Files.createDirectories(directory);
            Files.writeString(directory.resolve("document_start.js"), RestoreCoordinator.script() + "\n");
            java.lang.reflect.Field field = RestoreCoordinator.class.getDeclaredField("PROBE"); field.setAccessible(true);
            Files.writeString(directory.resolve("probe.js"), (String) field.get(null) + "\n");
        }
        final int[] nativeCalls = {0};
        hiro.berrybfcache.CompletionTarget target = new hiro.berrybfcache.CompletionTarget() {
            public void hiroCompleteBfcache(WebView view, String url) { nativeCalls[0]++; }
        };
        WebView nativeView = new WebView(URL);
        hiro.berrybfcache.NativeCompletion nativeAction = new hiro.berrybfcache.NativeCompletion(target, nativeView, URL);
        nativeAction.run(); nativeView.url = OTHER; nativeAction.run();
        check(nativeCalls[0] == 1, "native completion checks current URL");
        int weakFields = 0;
        for (java.lang.reflect.Field field : nativeAction.getClass().getDeclaredFields()) {
            if (field.getType() == java.lang.ref.WeakReference.class) weakFields++;
            check(field.getType() != WebView.class && field.getType() != hiro.berrybfcache.CompletionTarget.class,
                "native action does not retain view/client strongly");
        }
        check(weakFields == 2, "native references are weak"); scenarios++;

        Counter c = new Counter(); WebView v = begin(c);
        v.answer(0, marker("documenta",0,false)); v.advance(10000);
        check(c.n == 0 && v.visuals.isEmpty() && v.invalidations == 0, "ordinary load never completed by timeout");
        check(v.reads.size() <= 13 && v.timers.isEmpty(), "missing callbacks have finite watchdog budget"); scenarios++;

        c = new Counter(); v = begin(c); confirm(v,0,"documentb",1);
        check(c.n == 1 && v.visuals.size() == 1 && v.invalidations == 1, "missing commit and missing visual callback do not prevent completion");
        v.draw(0); check(v.invalidations == 2, "visual callback also requests ready frame"); scenarios++;

        c = new Counter(); v = begin(c); v.answer(0,"null"); wake(v); confirm(v,1,"documentc",1);
        check(c.n == 1, "null first query does not prevent late document binding"); scenarios++;

        c = new Counter(); v = begin(c); v.advance(520);
        check(v.reads.size() == 2, "lost first callback releases active request");
        v.answer(0,marker("documentd",1,true)); check(c.n == 0 && v.reads.size() == 2, "expired callback ignored");
        confirm(v,1,"documentd",1); check(c.n == 1, "live query recovers after dropped callback"); scenarios++;

        c = new Counter(); v = begin(c); wake(v); v.answer(0,marker("documente",0,false));
        check(v.reads.size() == 2,"inflight wake re-reads"); confirm(v,1,"documente",1);
        check(c.n == 1,"inflight wake retained"); scenarios++;

        wake(v); RestoreCoordinator.committed(v,URL,c); v.advance(10000);
        check(c.n == 1 && v.reads.size() == 3,"duplicate wake and commit do not repeat completion"); scenarios++;

        c = new Counter(); v = begin(c); RestoreCoordinator.started(v,OTHER,c); v.url = OTHER;
        v.answer(0,marker("documentf",1,true)); check(c.n == 0,"old result after navigation ignored");
        v.advance(120); confirm(v,1,"documentg",1); check(c.n == 1,"new generation can recover"); scenarios++;

        c = new Counter(); v = begin(c); v.answer(0,marker("documenth",1,true));
        RestoreCoordinator.started(v,URL,c); v.answer(1,marker("documenth",1,true));
        check(c.n == 0,"same-URL newer generation rejects pending confirmation");
        v.advance(120); confirm(v,2,"documenti",1); check(c.n == 1,"new same-URL document accepted"); scenarios++;

        c = new Counter(); v = begin(c); v.answer(0,marker("documentj",1,true));
        v.answer(1,marker("documentk",0,false)); check(c.n == 0,"document replacement before confirmation rejected");
        wake(v); confirm(v,2,"documentk",1); check(c.n == 1,"later true restore binds new document"); scenarios++;

        c = new Counter(); v = begin(c); v.answer(0,marker("documentl",0,false));
        send(v,false,"hiro-bfcache-restored-v1");send(v,true,"unrelated message");
        check(v.reads.size() == 1,"subframes and unrelated payloads ignored");
        wake(v); v.answer(1,marker("documentl",0,false)); check(c.n == 0,"message alone is not restore proof"); scenarios++;

        c = new Counter(); v = begin(c); v.answer(0,"\"B1|documentm|1|notboolean\"");
        v.advance(10000); check(c.n == 0,"invalid marker cannot complete"); scenarios++;

        c = new Counter(); v = begin(c); v.attached = false; confirm(v,0,"documentn",1); v.draw(0);
        check(c.n == 1 && v.invalidations == 0,"detached tab state completes without drawing"); scenarios++;

        c = new Counter(); v = new WebView(URL); android.os.Looper.background = true;
        RestoreCoordinator.started(v,URL,c); RestoreCoordinator.committed(v,URL,c);
        check(v.reads.isEmpty() && v.posts.size() == 2,"callbacks marshal to main queue");
        v.flushPosts(); confirm(v,0,"documento",1); check(c.n == 1,"marshalled callbacks preserve order"); scenarios++;

        c = new Counter(); v = begin(c); RestoreCoordinator.finished(v,URL); v.answer(0,marker("documentp",1,true));
        wake(v); v.advance(10000); check(c.n == 0 && v.reads.size() == 1,"ordinary finish cancels outstanding work"); scenarios++;

        c = new Counter(); v = begin(c); RestoreCoordinator.finished(v,OTHER); confirm(v,0,"documentq",1);
        check(c.n == 1,"stale wrong-URL finished ignored"); scenarios++;

        c = new Counter(); v = begin(c); confirm(v,0,"documentr",1); RestoreCoordinator.started(v,URL,c);
        v.draw(0); check(v.invalidations == 1,"old visual callback ignored after new same-URL generation");
        v.advance(120); confirm(v,2,"documentr",2); check(c.n == 2,"successive restore serials complete independently"); scenarios++;

        c = new Counter(); v = begin(c); v.throwOnVisual = true; confirm(v,0,"documents",1);
        check(c.n == 1 && v.invalidations == 1,"visual exception cannot block native completion"); scenarios++;

        c = new Counter(); v = new WebView(URL); v.throwOnEvaluate = true; RestoreCoordinator.started(v,URL,c);v.advance(10000);
        check(c.n == 0 && v.timers.isEmpty(),"evaluate exception retries terminate without fake completion");
        v.throwOnEvaluate = false; wake(v); confirm(v,0,"documentt",1);check(c.n == 1,"late wake recovers after earlier request failures");scenarios++;

        final int[] attempts = {0}; Counter retry = new Counter() { public void run() { if(attempts[0]++ == 0)throw new IllegalStateException("temporary");super.run(); } };
        v = begin(retry); confirm(v,0,"documentu",1);check(retry.n == 0,"failed completion does not consume event");
        v.advance(120);v.answer(2,marker("documentu",1,true));check(retry.n == 1,"failed completion can retry same verified event");scenarios++;

        c = new Counter(); v = new WebView(URL); RestoreCoordinator.started(v,URL);RestoreCoordinator.committed(v,URL,c);
        confirm(v,0,"documentv",1);check(c.n == 1,"legacy started overload works with commit");scenarios++;

        c = new Counter(); v = new WebView(OTHER);RestoreCoordinator.started(v,URL,c);v.advance(120);
        check(v.reads.isEmpty(),"URL not yet updated is not probed");v.url=URL;v.advance(120);confirm(v,0,"documentw",1);
        check(c.n == 1,"URL update after start is handled by bounded retry");scenarios++;

        c = new Counter();v=begin(c);v.answer(0,marker("documentx",1,true));v.answer(1,marker("documentx",1,false));
        check(c.n == 0,"pagehide between proof and confirmation cancels completion");scenarios++;

        c = new Counter();v=begin(c);v.advance(10000);int previous=v.reads.size();wake(v);confirm(v,previous,"documenty",1);
        check(c.n == 1,"late pageshow after finite initial window can complete");scenarios++;

        c = new Counter();v=begin(c);
        for(int i=0;i<11;i++){v.answer(i,"null");v.advance(120);}
        confirm(v,11,"documentz",1);check(c.n == 1 && v.reads.size() == 13,"last budgeted proof gets one confirmation credit");scenarios++;

        c = new Counter();v=new WebView(URL);RestoreCoordinator.started(v,URL,c);RestoreCoordinator.committed(v,URL,c);
        v.answer(0,marker("olddocument",0,false));wake(v);v.answer(1,marker("newdocument",1,true));
        check(c.n == 0,"new same-URL document before native started cannot use old committed document");
        RestoreCoordinator.started(v,URL,c);v.advance(120);confirm(v,2,"newdocument",1);
        check(c.n == 1,"late started admits same-URL new document without consuming its event early");scenarios++;

        c = new Counter();v=new WebView(URL);RestoreCoordinator.started(v,URL,c);RestoreCoordinator.committed(v,URL,c);
        v.answer(0,"null");wake(v);confirm(v,1,"latedocument",1);
        check(c.n == 1,"null commit query may bind later positive document");scenarios++;

        c = new Counter();v=begin(c);
        for(int i=0;i<12;i++){v.answer(i,"null");v.advance(120);}
        check(v.reads.size() == 12,"initial null probes exhaust finite budget");
        RestoreCoordinator.committed(v,URL,c);confirm(v,12,"commitlate",1);
        check(c.n == 1,"first native commit after exhausted window grants bounded retry");scenarios++;

        c = new Counter();v=new WebView(URL);RestoreCoordinator.started(v,URL,c);RestoreCoordinator.committed(v,URL,c);
        v.advance(10000);int exhausted=v.reads.size();RestoreCoordinator.committed(v,URL,c);v.advance(10000);
        check(v.reads.size() == exhausted && c.n == 0,"duplicate commits cannot renew an exhausted retry budget");scenarios++;

        System.out.println("{\"result\":\"PASS\",\"scenarios\":" + scenarios + ",\"assertions\":" + assertions +
            ",\"scope\":\"JVM lifecycle and callback-loss simulation; no Android renderer or device execution\"}");
    }
}
