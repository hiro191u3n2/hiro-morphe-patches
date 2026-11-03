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
    private static final class NativeCounter implements hiro.berrybfcache.CompletionTarget {
        int n, attempts;
        boolean accept = true;
        String completedUrl;
        public boolean hiroCompleteBfcache(WebView view, String url) {
            attempts++;
            if (!accept) return false;
            n++; completedUrl = url; return true;
        }
    }
    private static void nativeBegin(WebView view, String startedUrl, NativeCounter target) {
        RestoreCoordinator.started(view, startedUrl, new hiro.berrybfcache.NativeCompletion(target, view, startedUrl));
    }
    private static void confirmAt(WebView view, int first, String doc, int serial, String restored, String current) {
        view.answer(first, marker(doc,serial,true,restored,current));
        view.answer(first+1, marker(doc,serial,true,restored,current));
    }
    private static void check(boolean ok, String message) {
        assertions++; if (!ok) throw new AssertionError(message);
    }
    private static String marker(String doc, int serial, boolean pending) {
        return marker(doc, serial, pending, pending ? URL : "", URL);
    }
    private static String encoded(String url) {
        try { return java.net.URLEncoder.encode(url, "UTF-8").replace("+", "%20"); }
        catch (Exception error) { throw new RuntimeException(error); }
    }
    private static String marker(String doc, int serial, boolean pending, String restored, String current) {
        return "\"B2|" + doc + "|" + serial + "|" + (pending ? "1" : "0") + "|" + encoded(restored) + "|" + encoded(current) + "\"";
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
        view.answer(first, marker(doc,serial,true,view.url,view.url));
        view.answer(first+1, marker(doc,serial,true,view.url,view.url));
    }
    private static void readyForVisual(WebView view) {
        view.getViewTreeObserver().dispatchOnPreDraw();
        view.getViewTreeObserver().dispatchOnPreDraw();
    }
    /** No observation deadline survives the bounded presentation lifecycle. */
    private static boolean recoveryTimersDrained(WebView view) {
        return view.timers.isEmpty() && view.observer.preDraws.isEmpty() && view.observer.draws.isEmpty();
    }
    private static void observeFrame(WebView view) {
        view.getViewTreeObserver().dispatchOnDraw();
        view.flushPosts();
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
            public boolean hiroCompleteBfcache(WebView view, String url) { nativeCalls[0]++; return true; }
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
        check(c.n == 1 && v.visuals.isEmpty() && v.invalidations == 1, "loading completes before presentation readiness or visual callback");
        readyForVisual(v); int beforeVisual = v.invalidations;
        check(v.visuals.size() == 1 && c.n == 1, "stable pre-draw schedules one visual barrier without repeating native completion");
        v.draw(0); check(v.invalidations == beforeVisual + 1, "ready visual callback requests a new frame");
        observeFrame(v); scenarios++;

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

        c = new Counter(); v = begin(c); v.attached = false; confirm(v,0,"documentn",1);
        check(c.n == 1 && v.invalidations == 0 && v.visuals.isEmpty(),"detached tab state completes without drawing");
        v.attached = true; v.advance(100); readyForVisual(v);
        check(v.visuals.size() == 1 && c.n == 1,"reattached tab reaches visual readiness without repeating loading completion");
        v.draw(0); observeFrame(v); scenarios++;

        c = new Counter(); v = new WebView(URL); android.os.Looper.background = true;
        RestoreCoordinator.started(v,URL,c); RestoreCoordinator.committed(v,URL,c);
        check(v.reads.isEmpty() && v.posts.size() == 2,"callbacks marshal to main queue");
        v.flushPosts(); confirm(v,0,"documento",1); check(c.n == 1,"marshalled callbacks preserve order"); scenarios++;

        c = new Counter(); v = begin(c); RestoreCoordinator.finished(v,URL); v.answer(0,marker("documentp",1,true));
        wake(v); v.advance(10000); check(c.n == 0 && v.reads.size() == 1,"ordinary finish cancels outstanding work"); scenarios++;

        c = new Counter(); v = begin(c); RestoreCoordinator.finished(v,OTHER); confirm(v,0,"documentq",1);
        check(c.n == 1,"stale wrong-URL finished ignored"); scenarios++;

        c = new Counter(); v = begin(c); confirm(v,0,"documentr",1); readyForVisual(v);
        int staleInvalidations = v.invalidations; RestoreCoordinator.started(v,URL,c);
        v.draw(0); check(v.invalidations == staleInvalidations,"old visual callback ignored after new same-URL generation");
        v.advance(120); confirm(v,2,"documentr",2); check(c.n == 2,"successive restore serials complete independently"); scenarios++;

        c = new Counter(); v = begin(c); v.throwOnVisual = true; confirm(v,0,"documents",1); readyForVisual(v);
        check(c.n == 1 && v.visuals.isEmpty(),"visual exception cannot undo native completion"); scenarios++;

        c = new Counter(); v = new WebView(URL); v.throwOnEvaluate = true; RestoreCoordinator.started(v,URL,c);v.advance(10000);
        check(c.n == 0 && v.timers.isEmpty(),"evaluate exception retries terminate without fake completion");
        v.throwOnEvaluate = false; wake(v); confirm(v,0,"documentt",1);check(c.n == 1,"late wake recovers after earlier request failures");scenarios++;

        final int[] attempts = {0}; Counter retry = new Counter() { public void run() { if(attempts[0]++ == 0)throw new IllegalStateException("temporary");super.run(); } };
        v = begin(retry); confirm(v,0,"documentu",1);check(retry.n == 0,"failed completion does not consume event");
        v.advance(120);v.answer(2,marker("documentu",1,true));check(retry.n == 1,"failed completion can retry same verified event");scenarios++;

        c = new Counter(); v = new WebView(URL); RestoreCoordinator.started(v,URL);RestoreCoordinator.committed(v,URL,c);
        confirm(v,0,"documentv",1);check(c.n == 1,"legacy started overload works with commit");scenarios++;

        c = new Counter(); v = new WebView(OTHER);RestoreCoordinator.started(v,URL,c);v.advance(120);
        check(v.reads.size() == 1,"URL mismatch permits observation only");
        v.answer(0,marker("oldurlpage",1,true,OTHER,OTHER));check(c.n == 0,"other URL restore is not completed");
        v.url=URL;v.advance(120);confirm(v,1,"documentw",1);
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

        String alias = URL + "?page=2&name=日本語|+\"#section";
        NativeCounter nc = new NativeCounter(); v = new WebView(URL); nativeBegin(v,URL,nc);
        v.url=alias;v.advance(120);confirmAt(v,0,"urlreplace",1,URL,alias);
        check(nc.n == 1 && alias.equals(nc.completedUrl),"same-document replaceState before first probe completes at verified current URL");
        check(v.invalidations == 1,"retargeted completion requests redraw");scenarios++;

        nc=new NativeCounter();v=new WebView(URL);nativeBegin(v,URL,nc);v.url=URL+"#part";v.advance(120);
        confirmAt(v,0,"urlhashchange",1,URL,v.url);
        check(nc.n == 1 && nc.completedUrl.endsWith("#part"),"hash change preserves original restore binding");scenarios++;

        nc=new NativeCounter();v=new WebView(URL);nativeBegin(v,URL,nc);v.url=URL+"?a=1";v.advance(120);
        v.answer(0,marker("urlracechange",1,true,URL,v.url));
        v.url=URL+"?a=2";v.answer(1,marker("urlracechange",1,true,URL,URL+"?a=1"));
        check(nc.n==0,"URL change during confirmation discards that callback");v.advance(120);
        confirmAt(v,2,"urlracechange",1,URL,v.url);
        check(nc.n==1 && v.url.equals(nc.completedUrl),"bounded fresh pair follows later stable same-document URL");scenarios++;

        nc=new NativeCounter();v=new WebView(URL);nativeBegin(v,URL,nc);v.url=alias;v.advance(120);
        v.answer(0,marker("wrongrestore",1,true,OTHER,alias));v.advance(10000);
        check(nc.n==0,"matching current URL without original restore URL cannot complete");scenarios++;

        nc=new NativeCounter();v=new WebView(URL);nativeBegin(v,URL,nc);v.url=alias;v.advance(120);
        v.answer(0,marker("mismatchedjs",1,true,URL,OTHER));v.advance(10000);
        check(nc.n==0,"JavaScript current URL must match the native URL of its own query");scenarios++;

        c=new Counter();v=new WebView(URL);RestoreCoordinator.started(v,URL,c);v.url=alias;v.advance(120);
        confirmAt(v,0,"legacyrunnable",1,URL,alias);v.advance(10000);
        check(c.n==0,"legacy Runnable has no permission to retarget");v.url=URL;wake(v);
        int nextRead=v.reads.size()-1;confirmAt(v,nextRead,"legacyrunnable",1,URL,URL);
        check(c.n==1,"rejected legacy retarget does not consume restore serial");scenarios++;

        nc=new NativeCounter();v=new WebView(URL);nc.accept=false;nativeBegin(v,URL,nc);v.advance(120);
        confirmAt(v,0,"nativeownership",1,URL,URL);
        check(nc.n==0 && nc.attempts==1 && v.visuals.isEmpty(),"native ownership no-op is not success");
        nc.accept=true;v.advance(120);v.answer(2,marker("nativeownership",1,true));
        check(nc.n==1,"native ownership rejection may retry without consuming proof");scenarios++;

        nc=new NativeCounter();v=new WebView(URL);
        hiro.berrybfcache.NativeCompletion guarded=new hiro.berrybfcache.NativeCompletion(nc,v,URL);
        v.url=OTHER;check(!guarded.completeAtUrl(URL) && nc.n==0,"native call rechecks URL immediately before dispatch");
        check(guarded.completeAtUrl(OTHER) && nc.completedUrl.equals(OTHER),"verified retarget method forwards supplied current URL");
        java.lang.reflect.Field targetField=guarded.getClass().getDeclaredField("target");targetField.setAccessible(true);
        ((java.lang.ref.WeakReference<?>)targetField.get(guarded)).clear();
        check(!guarded.completeAtUrl(OTHER),"lost weak completion target reports failure");scenarios++;

        for(String bad : new String[]{"%ZZ","%C0%AF","%ED%A0%80","%","%ＦＦ","https+invalid"}) {
            nc=new NativeCounter();v=new WebView(URL);nativeBegin(v,URL,nc);v.advance(120);
            v.answer(0,"\"B2|invalidurl|1|1|"+bad+"|"+encoded(URL)+"\"");v.advance(10000);
            check(nc.n==0,"malformed percent/UTF-8 URL fails closed: "+bad);
        }scenarios++;

        nc=new NativeCounter();v=new WebView(URL);nativeBegin(v,URL,nc);v.url=alias;v.advance(120);
        for(int i=0;i<12;i++) {
            v.url=URL+"?moving="+i;
            v.answer(i,marker("unstableurl",1,true,URL,alias));
            v.advance(120);
        }
        v.advance(10000);
        check(nc.n==0 && v.reads.size()==12 && v.timers.isEmpty(),"repeated current URL changes cannot make retry infinite");scenarios++;

        nc=new NativeCounter();v=new WebView(URL);nativeBegin(v,URL,nc);
        RestoreCoordinator.committed(v,URL,new hiro.berrybfcache.NativeCompletion(nc,v,URL));
        v.answer(0,marker("bounddocument",0,false));v.url=alias;wake(v);
        v.answer(1,marker("otherdocument",1,true,URL,alias));v.advance(10000);
        check(nc.n==0,"URL alias cannot replace a committed document identity");scenarios++;

        nc=new NativeCounter();v=new WebView(URL);nativeBegin(v,URL,nc);v.url=alias;v.advance(120);
        confirmAt(v,0,"retargetvisual",1,URL,alias);readyForVisual(v);
        int aliasInvalidations=v.invalidations;v.url=OTHER;v.draw(0);
        check(v.invalidations==aliasInvalidations,"visual callback is bound to proven current URL");scenarios++;

        c=new Counter();v=begin(c);v.shown=false;confirm(v,0,"hiddenrender",1);
        check(c.n==1 && v.visuals.isEmpty() && v.invalidations==0,"hidden restore completes loading while presentation remains deferred");
        v.advance(500);check(c.n==1 && v.visuals.isEmpty(),"hidden wait does not restart loading or request a visual barrier");
        v.shown=true;v.advance(100);readyForVisual(v);
        check(v.visuals.size()==1,"revealed restore resumes the independent presentation lifecycle");
        int shownInvalidations=v.invalidations;v.draw(0);observeFrame(v);
        check(c.n==1 && v.invalidations==shownInvalidations+1,"revealed frame request does not repeat loading completion");
        check(v.observer.preDraws.isEmpty() && v.observer.draws.isEmpty() && v.attachListeners.isEmpty(),
            "observed draw releases recovery observers without starting diagnostic observation");scenarios++;

        c=new Counter();v=begin(c);confirm(v,0,"lostvisual",1);readyForVisual(v);v.advance(10000);
        check(c.n==1 && v.visuals.size()==1 && recoveryTimersDrained(v),"missing visual callback leaves loading complete and drains recovery work within the original timing bound");
        int visualReads=v.reads.size();wake(v);v.advance(10000);
        check(c.n==1 && v.reads.size()==visualReads && v.visuals.size()==1,"visual timeout cannot reopen loading probes or accumulate native callbacks");scenarios++;

        c=new Counter();v=begin(c);confirm(v,0,"cancelpredraw",1);
        java.util.ArrayList<Runnable> pendingPresentation=new java.util.ArrayList<Runnable>();
        for(android.view.View.Timer timer:v.timers)
            if(timer.r.getClass().getName().startsWith("hiro.berrybfcache.PresentationRecovery$"))pendingPresentation.add(timer.r);
        check(!pendingPresentation.isEmpty(),"confirmed restore owns a pending presentation check");
        RestoreCoordinator.started(v,URL,c);readyForVisual(v);
        boolean oldTimer=false;
        for(android.view.View.Timer timer:v.timers)if(pendingPresentation.contains(timer.r))oldTimer=true;
        check(!oldTimer && v.visuals.isEmpty(),"new started removes prior presentation timers and pre-draw listener before visual submission");
        check(c.n==1,"cancelling presentation does not undo previous loading completion");scenarios++;

        c=new Counter();v=begin(c);confirm(v,0,"boundedvisual",1);readyForVisual(v);
        for(int serial=2;serial<=4;serial++) {
            RestoreCoordinator.started(v,URL,c);v.advance(120);
            int firstRead=v.reads.size()-1;confirm(v,firstRead,"boundedvisual",serial);readyForVisual(v);
        }
        check(c.n==4 && v.visuals.size()==1,"successive restored generations complete while one lost native visual request caps the queue");
        int accumulatedInvalidations=v.invalidations;v.draw(0);
        check(v.invalidations==accumulatedInvalidations && v.visuals.size()==2,"old callback releases its lease without painting and current proof gets a distinct visual barrier");
        v.draw(1);observeFrame(v);v.advance(10000);
        check(c.n==4 && recoveryTimersDrained(v),"latest restored generation finishes presentation within the original timing bound without recovery timers");
        scenarios++;

        System.out.println("{\"result\":\"PASS\",\"scenarios\":" + scenarios + ",\"assertions\":" + assertions +
            ",\"scope\":\"JVM lifecycle and callback-loss simulation; no Android renderer or device execution\"}");
    }
}
