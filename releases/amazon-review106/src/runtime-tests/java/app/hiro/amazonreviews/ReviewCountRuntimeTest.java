package app.hiro.amazonreviews;
import android.webkit.WebView;
import android.webkit.ValueCallback;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReviewCountRuntimeTest {
    private static int assertions;
    private static void eq(Object actual, Object expected) {
        assertions++;
        if (!Objects.equals(actual, expected)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
    private static String wire(String route, String phase, int roots, int cards, int known) {
        return JSONObject.quote("{\"v\":\"1.0.6\",\"stage\":\"ack\",\"route\":\""+route+"\",\"error\":null,\"report\":{\"version\":\"1.0.6\",\"phase\":\""+phase+"\",\"roots\":"+roots+",\"cards\":"+cards+",\"known\":"+known+",\"status\":true,\"error\":null,\"limited\":false}}");
    }
    private static String mutate(String source,String from,String to) {
        return JSONObject.quote(((String)new org.json.JSONTokener(source).nextValue()).replace(from,to));
    }
    private static String route(WebView view) {
        Matcher match=Pattern.compile("setNativeCheckpoint\\('1.0.6','(search|home|other)'\\)").matcher(view.script);
        if(!match.find())throw new AssertionError("Missing checkpoint"); return match.group(1);
    }
    private static String inject(WebView view, String current, String callback) {
        view.url = current;
        ReviewCountRuntime.inject(view, callback);
        Matcher match = Pattern.compile("s\\.setSearchHint\\((true|false),(null|'[0-9a-f]+')\\);").matcher(view.script);
        if (!match.find()) throw new AssertionError("Missing bounded native identity hint");
        String result=match.group(1)+":"+match.group(2);
        view.callback.onReceiveValue(wire(route(view),"waiting",0,0,0));
        eq(ReviewCountRuntime.lastStage(view),"ack");
        return result;
    }
    private static WebView start(String url,String callback) {
        Handler.reset(); Toast.reset();
        try { java.lang.reflect.Field f=ReviewCountRuntime.class.getDeclaredField("FAILURE_NOTICES"); f.setAccessible(true); ((java.util.Set<?>)f.get(null)).clear(); } catch(Exception error) { throw new AssertionError(error); }
        WebView view=new WebView(); view.url=url; ReviewCountRuntime.inject(view,callback); return view;
    }
    private static void drain() { int count=0; while(Handler.pendingCount()>0 && ++count<20)Handler.runNext(); eq(count<20,true); }
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
        eq(view.calls, 1);
        eq(view.callback != null, true);
        view.callback.onReceiveValue(wire("search","waiting",0,0,0));
        eq(view.script.contains("評価件数順"), true);
        String first = inject(view, base + "?k=first&rh=n%3A123", base + "?k=stale");
        eq(first.startsWith("true:'"), true);
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
        eq(homeFirst.startsWith("true:'"), true);
        eq(view.script.contains("private-home-query"), false);
        eq(inject(view, home, base + "?page=2&k=private-home-query&ref=x"), homeFirst);
        eq(inject(view, home, base + "?k=other-home-query").equals(homeFirst), false);
        eq(inject(view, home, "https://www.amazon.com/s?k=wrong-site"), "false:null");
        eq(inject(view, home, "https://evil.example/s"), "false:null");
        eq(inject(view, home, null), "false:null");
        String mobile = inject(view, home, "https://m.amazon.co.jp/s?k=mobile");
        eq(inject(view, home, "https://www.amazon.co.jp/s?k=mobile&page=3"), mobile);
        for (String safeHome : new String[]{home, "https://www.amazon.co.jp/ref=nav", "https://www.amazon.co.jp/gp/aw", "https://www.amazon.co.jp/gp/aw/", "https://www.amazon.co.jp/-/en/", "https://www.amazon.co.jp/gp/homepage.html", "https://www.amazon.co.jp/gp/aw/h.html", "https://www.amazon.co.jp/gp/aw/homepage.html", "https://www.amazon.co.jp/gp/gw/ajax/mshop.html", "https://www.amazon.co.jp/-/ja/gp/homepage.html", "https://www.amazon.co.jp/-/ja_JP/gp/aw/h.html", "https://www.amazon.co.jp/-/en/gp/aw/homepage.html", "https://www.amazon.co.jp/-/en_US/gp/gw/ajax/mshop.html", "https://m.amazon.co.jp/gp/aw/h.html"}) {
            eq(inject(view, safeHome, base + "?k=mobile"), mobile);
        }
        eq(inject(view, "https://www.amazon.co.jp/gp/homepage.html", null), "false:null");
        String beforeProduct = inject(view, home, base + "?k=return-query");
        for (String trusted : new String[]{"https://www.amazon.co.jp/dp/B000000001", "https://www.amazon.co.jp/gp/cart/view.html", "https://www.amazon.co.jp/ap/signin", "https://www.amazon.co.jp/gp/help/customer", "https://www.amazon.co.jp/s/browse"}) {
            eq(inject(view,trusted,base+"?k=return-query"),beforeProduct);
        }
        for (String blocked : new String[]{"https://www.amazon.co.jp.evil.example/s", "https://www.amazon.co.jp:8443/s", "http://www.amazon.co.jp/s"}) {
            int before=view.calls; view.url=blocked; ReviewCountRuntime.inject(view,base); eq(view.calls,before); eq(ReviewCountRuntime.lastStage(view),"blocked");
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
        int calls = view.calls;
        view.url = base;
        view.destroyed = true;
        ReviewCountRuntime.inject(view, base);
        view.flush();
        eq(view.calls, calls);
        eq(Toast.messages.stream().filter(x->x.equals("評価件数順 v1.0.6 読込")).count(),1L);
        // Bounded callback protocol, navigation invalidation and observable failures.
        WebView w=start(base,base);
        eq(w.script.contains("else if(typeof s.schedule==='function'){s.schedule();}"),true);
        eq(w.callback!=null,true);
        w.callback.onReceiveValue(wire("search","frozen",2,5,5));
        eq(ReviewCountRuntime.lastStage(w),"ack"); eq(Handler.pendingCount(),0);
        w=start(base,base); ValueCallback<String> stale=w.callback;
        w.url="https://www.amazon.co.jp/gp/cart/view.html";
        stale.onReceiveValue(wire("search","frozen",1,2,2));
        eq(ReviewCountRuntime.lastStage(w),"superseded"); eq(Handler.pendingCount(),0); eq(w.calls,1);
        w=start(base,base); stale=w.callback;
        ReviewCountRuntime.inject(w,base); int rounds=w.calls;
        stale.onReceiveValue("null"); eq(w.calls,rounds);
        w.callback.onReceiveValue(wire("search","waiting",0,0,0));eq(Handler.pendingCount(),0);
        w=start(base,base); drain(); eq(w.calls,1);eq(Handler.clock(),5000L);eq(ReviewCountRuntime.lastStage(w),"no-ack");
        eq(Toast.messages.stream().anyMatch(x->x.contains("JS応答未確認")&&x.contains("E_ACK")),true);
        w=start(base,base);
        for(int i=0;i<3;i++){w.callback.onReceiveValue("null");if(i<2){Handler.runNext();eq(w.script.contains("else if(typeof s.schedule==='function'){s.schedule();}"),false);}}
        eq(w.calls,3);eq(ReviewCountRuntime.lastStage(w),"no-ack");eq(Handler.pendingCount(),0);
        String valid=wire("search","waiting",1,2,1);
        for(String invalid:new String[]{valid+" trailing", JSONObject.quote(((String)new org.json.JSONTokener(valid).nextValue())+" trailing"), mutate(valid,"\"limited\":false","\"limited\":false,\"unexpected\":0"), mutate(valid,"\"roots\":1","\"roots\":\"1\""), mutate(valid,"\"roots\":1","\"roots\":1.5"), mutate(valid,"\"status\":true","\"status\":\"true\""),JSONObject.quote("{\"report\":{\"nested\":{\"more\":{}}}}"),wire("search","waiting",1,2,3),wire("search","waiting",101,1,1),wire("other","waiting",0,0,0),"{}",JSONObject.quote("{\"v\":\"1.0.6\",\"route\":\"search\",\"stage\":\"js-error\",\"error\":\"PRIVATE_ERROR_URL\",\"report\":null}")}){
            w=start(base,base);w.callback.onReceiveValue(invalid);eq(ReviewCountRuntime.lastStage(w),"retry");eq(w.calls,1);
        }
        w=start(base,base); w.callback.onReceiveValue(JSONObject.quote("{\"v\":\"1.0.6\",\"route\":\"search\",\"stage\":\"js-error\",\"error\":\"TypeError\",\"report\":null}"));
        eq(ReviewCountRuntime.lastStage(w),"js-error");eq(Handler.pendingCount(),0);
        eq(Toast.messages.stream().anyMatch(x->x.contains("E_JS_TypeError")),true);
        w=start(base,base);w.callback.onReceiveValue(wire("search","inactive",2,3,1));eq(ReviewCountRuntime.lastStage(w),"inactive");
        eq(Toast.messages.stream().anyMatch(x->x.contains("E_INACTIVE")&&x.contains("R2 C3 K1")),true);
        for(String normal:new String[]{home,"https://www.amazon.co.jp/dp/B000000001","https://www.amazon.co.jp/gp/cart/view.html","https://www.amazon.co.jp/gp/help/customer"}){
            w=start(normal,base);w.callback.onReceiveValue(wire(route(w),"inactive",0,0,0));eq(ReviewCountRuntime.lastStage(w),"ack");eq(Toast.messages.size(),0);
        }
        w=start("about:blank",base);eq(w.calls,0);w.url="https://www.amazon.co.jp/gp/cart/view.html";Handler.runNext();eq(w.calls,0);eq(ReviewCountRuntime.lastStage(w),"superseded");
        w=start("about:blank",base);w.url="https://www.amazon.co.jp/gp/help/customer";Handler.runNext();eq(w.calls,1);eq(route(w),"other");w.callback.onReceiveValue(wire("other","waiting",1,2,2));eq(ReviewCountRuntime.lastStage(w),"ack");
        w=start("about:blank",base);drain();eq(w.calls,0);eq(ReviewCountRuntime.lastStage(w),"no-document");
        Handler.reset();Toast.reset();Looper.main=false;w=new WebView();w.url=base;ReviewCountRuntime.inject(w,base);eq(w.calls,0);Handler.runNext();eq(w.calls,1);Looper.main=true;w.callback.onReceiveValue(wire("search","waiting",0,0,0));
        Handler.reset();Looper.main=false;Handler.rejectPosts=true;w=new WebView();w.url=base;ReviewCountRuntime.inject(w,base);eq(w.calls,0);eq(ReviewCountRuntime.lastStage(w),"post-failed");Handler.reset();
        System.out.println("PASS runtime URL, opaque search identity, bounded native receipt and lifecycle assertions=" + assertions);
    }
}
