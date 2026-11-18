import hiro.berrybfcache.RestoreCoordinator;

/** Exercise the real NativeRestoreTracker callbacks with the previous release's WebView test double. */
public final class YahooNativeProofTest {
    private static int scenarios;
    static void check(String url, boolean known, boolean history, boolean reload,
                      boolean error, boolean sameDoc, int expectedDispatches) {
        NativeRestoreTrackerTest.Fixture f = new NativeRestoreTrackerTest.Fixture(url, true);
        NativeRestoreTrackerTest.Page list = new NativeRestoreTrackerTest.Page();
        f.visit(new NativeRestoreTrackerTest.Nav(list,url));
        f.visit(new NativeRestoreTrackerTest.Nav(new NativeRestoreTrackerTest.Page(),"https://news.yahoo.co.jp/articles/fixture"));
        RestoreCoordinator.nativeRestored(f.view, url, 0L);
        if (!f.view.reads.isEmpty()) throw new AssertionError("unproved notification issued JS");
        NativeRestoreTrackerTest.Nav nav = new NativeRestoreTrackerTest.Nav(
                known ? list : new NativeRestoreTrackerTest.Page(),url);
        nav.history = history; nav.reload = reload; nav.error = error; nav.sameDocument = sameDoc;
        f.visit(nav);
        if (f.view.reads.size()!=expectedDispatches || f.view.reads.stream().anyMatch(c -> c!=null))
            throw new AssertionError("unexpected native dispatch for " + url + ", history="+history+", reload="+reload);
        scenarios++;
    }
    public static void main(String[] args) {
        String url = "https://news.yahoo.co.jp/categories/entertainment?topic=a%20b#tab";
        check(url,true,true,false,false,false,1);
        check(url,false,true,false,false,false,0);
        check(url,true,false,false,false,false,0);
        check(url,true,true,true,false,false,0);
        check(url,true,true,false,true,false,0);
        check(url,true,true,false,false,true,0);
        check("https://example.com/",true,true,false,false,false,0);
        System.out.println("{\"result\":\"PASS\",\"real_tracker_mock_provider_scenarios\":"+scenarios+"}");
    }
}
