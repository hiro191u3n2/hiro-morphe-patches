package app.hiro.amazonreviews;
import android.webkit.WebView;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReviewCountRuntimeTest {
    private static int assertions;
    private static void eq(Object actual, Object expected) {
        assertions++;
        if (!Objects.equals(actual, expected)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
    private static String inject(WebView view, String current, String callback) {
        view.url = current;
        ReviewCountRuntime.inject(view, callback);
        view.flush();
        Matcher match = Pattern.compile("setSearchHint\\((true|false),(null|\"[0-9a-f]+\")\\);}$").matcher(view.script);
        if (!match.find()) throw new AssertionError("Missing bounded native identity hint");
        return match.group(1) + ":" + match.group(2);
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
        String first = inject(view, base + "?k=first&rh=n%3A123", base + "?k=stale");
        eq(first.startsWith("true:\""), true);
        eq(inject(view, base + "?rh=n%3A123&k=first&page=2&ref=tracking&s=review-count-rank", null), first);
        eq(inject(view, base + "?k=first&rh=n%3A123&sort=price&sprefix=random", "https://evil.example/s"), first);
        String second = inject(view, base + "?k=second&rh=n%3A123", base + "?k=first");
        eq(second.equals(first), false);
        eq(inject(view, base + "?rh=n%3A123&k=second", base + "?k=private-search-text-should-never-enter-script"), second);
        eq(view.script.contains("private-search-text-should-never-enter-script"), false);
        String filtered = inject(view, base + "?k=second&rh=n%3A456", null);
        eq(filtered.equals(second), false);
        String seller = inject(view, base + "?k=second&rh=n%3A456&me=seller1", null);
        eq(seller.equals(filtered), false);
        eq(inject(view, base + "?me=seller2&rh=n%3A456&k=second", null).equals(seller), false);
        String pFilter = inject(view, base + "?k=second&p_n_feature=123", null);
        eq(inject(view, base + "?p_n_feature=456&k=second", null).equals(pFilter), false);
        String encoded = inject(view, base + "?k=hello+world&rh=n%3A123", null);
        eq(inject(view, base + "?rh=n:123&k=hello%20world", null), encoded);
        String duplicated = inject(view, base + "?k=hello&rh=a&rh=b", null);
        eq(inject(view, base + "?rh=b&k=hello&rh=a", null), duplicated);
        eq(inject(view, base + "?k=hello&rh=a&rh=c", null).equals(duplicated), false);
        String home = "https://www.amazon.co.jp/";
        String homeFirst = inject(view, home, base + "?k=private-home-query");
        eq(homeFirst.startsWith("true:\""), true);
        eq(view.script.contains("private-home-query"), false);
        eq(inject(view, home, base + "?page=2&k=private-home-query&ref=x"), homeFirst);
        eq(inject(view, home, base + "?k=other-home-query").equals(homeFirst), false);
        eq(inject(view, home, "https://www.amazon.com/s?k=wrong-site"), "false:null");
        eq(inject(view, home, "https://evil.example/s"), "false:null");
        eq(inject(view, home, null), "false:null");
        String mobile = inject(view, home, "https://m.amazon.co.jp/s?k=mobile");
        eq(inject(view, home, "https://www.amazon.co.jp/s?k=mobile&page=3"), mobile);
        for (String safeHome : new String[]{home, "https://www.amazon.co.jp/ref=nav", "https://www.amazon.co.jp/gp/aw", "https://www.amazon.co.jp/gp/aw/", "https://www.amazon.co.jp/-/en/"}) {
            eq(inject(view, safeHome, base + "?k=mobile"), mobile);
        }
        String beforeProduct = inject(view, home, base + "?k=return-query");
        int calls = view.calls;
        for (String blocked : new String[]{"https://www.amazon.co.jp/dp/B000000001", "https://www.amazon.co.jp/gp/cart/view.html", "https://www.amazon.co.jp/ap/signin", "https://www.amazon.co.jp/s/browse", "https://www.amazon.co.jp.evil.example/s", "https://www.amazon.co.jp:8443/s", "http://www.amazon.co.jp/s", null}) {
            view.url = base;
            ReviewCountRuntime.inject(view, base + "?k=stale-search");
            view.url = blocked;
            view.flush();
            eq(view.calls, calls);
        }
        eq(inject(view, home, base + "?k=return-query"), beforeProduct);
        WebView another = new WebView();
        String own = inject(view, home, base + "?k=same");
        eq(inject(another, home, base + "?k=same").equals(own), false);
        eq(inject(view, home, base + "?k=same&page=4"), own);
        StringBuilder huge = new StringBuilder(base + "?k=");
        for (int i = 0; i < 17000; i++) huge.append('x');
        eq(inject(view, home, huge.toString()), "true:null");
        eq(view.script.contains(huge.toString()), false);
        calls = view.calls;
        view.url = base;
        view.destroyed = true;
        ReviewCountRuntime.inject(view, base);
        view.flush();
        eq(view.calls, calls);
        System.out.println("PASS runtime URL, opaque search identity and WebView lifecycle assertions=" + assertions);
    }
}
