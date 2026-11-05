package app.hiro.amazonreviews;
import android.webkit.WebView;
import java.util.Objects;

public final class ReviewCountRuntimeTest {
    private static int assertions;
    private static void eq(Object actual, Object expected) {
        assertions++;
        if (!Objects.equals(actual, expected)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
    public static void main(String[] args) {
        String base = "https://www.amazon.co.jp/s";
        eq(ReviewCountRuntime.normalizeUrl(null), null);
        eq(ReviewCountRuntime.normalizeUrl(base), base + "?s=review-count-rank");
        eq(ReviewCountRuntime.normalizeUrl(base + "?k=a+b%26c&ref=x%2Fy&s=price-asc-rank&page=2#keep"), base + "?k=a+b%26c&ref=x%2Fy&page=2&s=review-count-rank#keep");
        eq(ReviewCountRuntime.normalizeUrl(base + "?sort=old&s=x&%73=old2&k=%E6%9C%AC&sort=z"), base + "?k=%E6%9C%AC&s=review-count-rank");
        String once = ReviewCountRuntime.normalizeUrl(base + "?k=a");
        eq(ReviewCountRuntime.normalizeUrl(once), once);
        for (String url : new String[]{"https://www.amazon.com/s/ref=sr", "https://m.amazon.co.jp/gp/aw/s", "https://www.amazon.co.jp/gp/aw/search/s", "https://www.amazon.co.jp/gp/aw/s/ref=x", "https://www.amazon.co.uk/s", "https://www.amazon.co.jp:443/s", "https://www.amazon.co.jp/batteries/s", "https://www.amazon.co.jp/-/en/s", "https://www.amazon.co.jp/gp/search", "https://www.amazon.co.jp/mn/search", "https://www.amazon.co.jp/search-by-list/asins"}) {
            eq(ReviewCountRuntime.normalizeUrl(url), url + "?s=review-count-rank");
        }
        for (String url : new String[]{"http://www.amazon.co.jp/s", "https://amazon.co.jp.evil.example/s", "https://www.amazon.co.jp/dp/B000000001", "https://www.amazon.co.jp/gp/cart/view.html", "https://www.amazon.co.jp/suspicious", "https://www.amazon.co.jp/s/browse", "https://www.amazon.co.jp/s/browse/ref=x", "https://www.amazon.co.jp:8443/s", "https://user@www.amazon.co.jp/s", "/s?k=a", "javascript:alert(1)", "https://www.amazon.co.jp/s?k=bad%zz", "https://www.amazon.co.jp/s?k=a b"}) {
            eq(ReviewCountRuntime.normalizeUrl(url), url);
        }
        ReviewCountRuntime.inject(null, base);
        WebView view = new WebView();
        view.url = base;
        ReviewCountRuntime.inject(view, base);
        eq(view.calls, 0);
        view.flush();
        eq(view.calls, 1);
        eq(view.script.contains("評価件数順"), true);
        eq(view.script.contains("fetch("), false);
        eq(view.script.endsWith("setSearchHint(true);}"), true);
        view.url = base + "?k=next";
        ReviewCountRuntime.inject(view, base);
        view.url = "https://www.amazon.co.jp/dp/B000000001";
        view.flush();
        eq(view.calls, 2); // trusted document bootstrap is allowed; JS rejects product sorting
        view.url = base;
        ReviewCountRuntime.inject(view, "https://evil.example/s");
        view.flush();
        eq(view.calls, 3); // actual current document determines bootstrap origin
        eq(view.script.endsWith("setSearchHint(false);}"), true);
        view.url = "https://www.amazon.co.jp/";
        ReviewCountRuntime.inject(view, base + "?k=private-search-text-should-never-enter-script");
        view.flush();
        eq(view.calls, 4);
        eq(view.script.endsWith("setSearchHint(true);}"), true);
        eq(view.script.contains("private-search-text-should-never-enter-script"), false);
        ReviewCountRuntime.inject(view, base);
        view.url = "https://www.amazon.co.jp.evil.example/s";
        view.flush();
        eq(view.calls, 4);
        view.url = null;
        ReviewCountRuntime.inject(view, base);
        view.flush();
        eq(view.calls, 4);
        view.url = "https://www.amazon.co.jp/";
        ReviewCountRuntime.inject(view, null);
        view.flush();
        eq(view.calls, 5);
        eq(view.script.endsWith("setSearchHint(false);}"), true);
        view.url = "https://www.amazon.co.jp:8443/s";
        ReviewCountRuntime.inject(view, base);
        view.flush();
        eq(view.calls, 5);
        view.url = base;
        view.destroyed = true;
        ReviewCountRuntime.inject(view, base);
        view.flush();
        eq(view.calls, 5);
        System.out.println("PASS runtime URL and WebView lifecycle assertions=" + assertions);
    }
}
