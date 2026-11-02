import android.webkit.WebView;
import hiro.berrybfcache.CompletionTarget;
import hiro.berrybfcache.NativeCompletion;
import hiro.berrybfcache.RestoreCoordinator;

/** Runs the actual native listener and coordinator together through their public APIs. */
public final class RestoreNativeIntegrationTest {
    private static final String URL = "https://example.test/list";
    private static final String OTHER = "https://example.test/product";
    private static int assertions;
    private static int scenarios;

    private static final class Target implements CompletionTarget {
        boolean accept = true;
        boolean fail;
        int attempts;
        int calls;
        String completedUrl;
        @Override public boolean hiroCompleteBfcache(WebView view, String url) {
            attempts++;
            if (fail) throw new IllegalStateException("completion temporarily unavailable");
            if (!accept) return false;
            calls++; completedUrl = url; return true;
        }
    }
    private static final class Case {
        final NativeRestoreTrackerTest.Fixture f = new NativeRestoreTrackerTest.Fixture(URL, true);
        final NativeRestoreTrackerTest.Page page = new NativeRestoreTrackerTest.Page();
        Case() throws Throwable {
            visit(new NativeRestoreTrackerTest.Nav(page, URL));
            visit(new NativeRestoreTrackerTest.Nav(new NativeRestoreTrackerTest.Page(), OTHER));
        }
        void visit(NativeRestoreTrackerTest.Nav nav) throws Throwable {
            f.start(nav); f.complete(nav);
        }
        NativeRestoreTrackerTest.Nav restoring() throws Throwable {
            f.view.url = URL;
            NativeRestoreTrackerTest.Nav nav = new NativeRestoreTrackerTest.Nav(page, URL);
            nav.history = true; f.start(nav); return nav;
        }
        void begin(Target target) {
            RestoreCoordinator.started(f.view, URL, new NativeCompletion(target, f.view, URL));
        }
        void bodyDone() { RestoreCoordinator.startedBodyFinished(f.view, URL); }
    }
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Throwable {
        Case c = new Case(); Target target = new Target();
        c.f.complete(c.restoring()); c.begin(target);
        check(RestoreCoordinator.shouldSkipLoading(c.f.view), "early native proof skips loading initialization");
        check(target.calls == 0, "early proof cannot complete before original start body exits");
        c.bodyDone();
        check(target.calls == 1 && URL.equals(target.completedUrl), "early native proof completes current tab after body");
        check(c.f.view.reads.isEmpty() && c.f.view.visuals.size() == 1 && c.f.view.invalidations == 1,
            "native proof needs no JavaScript response and requests a frame");
        c.bodyDone(); c.f.view.advance(10000);
        check(target.calls == 1 && !RestoreCoordinator.shouldSkipLoading(c.f.view), "completion and body hook are idempotent");
        scenarios++;

        c = new Case(); target = new Target();
        NativeRestoreTrackerTest.Nav restore = c.restoring(); c.begin(target);
        check(!RestoreCoordinator.shouldSkipLoading(c.f.view), "start without native completion has no restore proof");
        c.bodyDone(); check(target.calls == 0, "original body alone does not prove restore");
        c.f.complete(restore);
        check(target.calls == 1 && c.f.view.reads.isEmpty(), "late native proof joins matching completed body without JS");
        scenarios++;

        c = new Case(); target = new Target(); restore = c.restoring(); c.begin(target);
        c.f.complete(restore);
        check(target.calls == 0 && RestoreCoordinator.shouldSkipLoading(c.f.view), "proof during body only opens loading guard");
        c.bodyDone(); check(target.calls == 1, "proof during body waits for body completion"); scenarios++;

        c = new Case(); target = new Target();
        c.f.view.url = URL; c.begin(target); c.bodyDone();
        c.f.complete(c.restoring());
        check(target.attempts == 0 && !RestoreCoordinator.shouldSkipLoading(c.f.view),
            "new same-URL navigation cannot attach native proof to older start state");
        Target next = new Target(); c.begin(next);
        check(RestoreCoordinator.shouldSkipLoading(c.f.view), "unmatched early proof remains consumable by correct start");
        c.bodyDone(); check(next.calls == 1 && target.calls == 0, "only current native generation completes"); scenarios++;

        c = new Case(); target = new Target(); c.f.complete(c.restoring()); c.begin(target);
        check(RestoreCoordinator.shouldSkipLoading(c.f.view), "proof exists before navigation race");
        c.f.start(new NativeRestoreTrackerTest.Nav(new NativeRestoreTrackerTest.Page(), URL));
        check(!RestoreCoordinator.shouldSkipLoading(c.f.view), "new cross-document native start closes loading guard at same URL");
        c.bodyDone(); c.f.view.advance(10000);
        check(target.calls == 0 && c.f.view.reads.isEmpty(), "older body and timers cannot complete new native navigation"); scenarios++;

        c = new Case(); target = new Target(); c.f.complete(c.restoring()); c.begin(target);
        c.f.delete(c.page);
        check(!RestoreCoordinator.shouldSkipLoading(c.f.view), "deleted native Page closes loading guard");
        c.bodyDone(); c.f.view.advance(10000);
        check(target.calls == 0 && c.f.view.reads.size() <= 12 && c.f.view.timers.isEmpty(),
            "deleted Page cannot complete and JS fallback stays bounded"); scenarios++;

        c = new Case(); target = new Target(); target.accept = false;
        c.f.complete(c.restoring()); c.begin(target); c.bodyDone();
        check(target.attempts == 1 && target.calls == 0 && c.f.view.visuals.isEmpty(), "false native result is not completion");
        target.accept = true; c.f.view.advance(120);
        check(target.attempts == 2 && target.calls == 1 && c.f.view.reads.isEmpty(), "false native result keeps proof for finite retry"); scenarios++;

        c = new Case(); target = new Target(); target.fail = true;
        c.f.complete(c.restoring()); c.begin(target); c.bodyDone(); target.fail = false; c.f.view.advance(120);
        check(target.attempts == 2 && target.calls == 1, "completion exception retains native proof for retry"); scenarios++;

        c = new Case(); target = new Target(); target.accept = false;
        c.f.complete(c.restoring()); c.begin(target); c.bodyDone(); c.f.view.advance(10000);
        check(target.attempts <= 13 && target.calls == 0 && c.f.view.timers.isEmpty(), "permanent native rejection has finite retries"); scenarios++;

        c = new Case(); target = new Target(); c.f.complete(c.restoring()); c.begin(target);
        String alias = URL + "?page=2#restored"; c.f.view.url = alias;
        NativeRestoreTrackerTest.Nav sameDocument = new NativeRestoreTrackerTest.Nav(c.page, alias);
        sameDocument.sameDocument = true; c.visit(sameDocument);
        check(RestoreCoordinator.shouldSkipLoading(c.f.view), "native same-Page commit follows changed current URL");
        c.bodyDone();
        check(target.calls == 1 && alias.equals(target.completedUrl), "native same-document restore completes verified current URL"); scenarios++;

        c = new Case(); target = new Target(); c.f.complete(c.restoring()); c.begin(target);
        c.f.view.url = URL + "#unproved";
        check(!RestoreCoordinator.shouldSkipLoading(c.f.view), "URL change alone cannot retarget native Page proof");
        c.bodyDone(); c.f.view.advance(10000);
        check(target.calls == 0, "unproved URL change cannot complete without independent JS evidence"); scenarios++;

        c = new Case(); target = new Target(); c.f.complete(c.restoring()); c.begin(target); c.bodyDone();
        int invalidations = c.f.view.invalidations;
        c.f.start(new NativeRestoreTrackerTest.Nav(new NativeRestoreTrackerTest.Page(), URL)); c.f.view.draw(0);
        check(c.f.view.invalidations == invalidations, "visual callback is invalidated by new native navigation at same URL"); scenarios++;

        c = new Case(); target = new Target(); c.f.view.url = URL; c.begin(target); c.f.view.advance(120);
        check(c.f.view.reads.size() == 1, "ordinary native navigation still uses JS fallback");
        c.f.start(new NativeRestoreTrackerTest.Nav(new NativeRestoreTrackerTest.Page(), URL));
        c.f.view.answer(0, "\"B2|documentx|1|1|https%3A%2F%2Fexample.test%2Flist|https%3A%2F%2Fexample.test%2Flist\"");
        c.f.view.advance(10000);
        check(target.calls == 0 && c.f.view.reads.size() == 1, "JS callback from old native navigation cannot bind same-URL new document"); scenarios++;

        System.out.println("{\"result\":\"PASS\",\"scenarios\":" + scenarios + ",\"assertions\":" + assertions
            + ",\"scope\":\"JVM native listener/coordinator integration; no Android WebView or device execution\"}");
    }
}
