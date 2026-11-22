package app.hiro.berry.links;

import android.net.Uri;
import android.webkit.WebResourceRequest;
import java.util.Collections;
import java.util.Map;

public final class ShareRedirectCases {
    private static final String SHORT = "https://share.google/Example1";
    private static final String RELAY = "https://www.google.com/share.google?q=Example1";
    private static final String POST = "https://www.instagram.com/p/Example/";
    private static int count;

    private static boolean request(String url, boolean gesture, boolean redirect, boolean main, String method) {
        WebResourceRequest req = new WebResourceRequest() {
            public Uri getUrl() { return Uri.parse(url); }
            public boolean isForMainFrame() { return main; }
            public boolean isRedirect() { return redirect; }
            public boolean hasGesture() { return gesture; }
            public String getMethod() { return method; }
            public Map<String,String> getRequestHeaders() { return Collections.emptyMap(); }
        };
        boolean allow = InstagramPostLinks.allowsHandoff(null, req);
        return allow && InstagramPostLinks.instagramUrl(req.getUrl()) != null;
    }
    private static boolean tap(String url) { return request(url, true, false, true, "GET"); }
    private static boolean follow(String url) { return request(url, false, true, true, "GET"); }
    private static void check(boolean ok) { if (!ok) throw new AssertionError("case " + (count + 1)); count++; }

    public static void main(String[] args) {
        check(tap(POST));
        check(!follow(POST));
        check(!tap(SHORT)); check(!follow(RELAY)); check(follow(POST)); check(!follow(POST));
        InstagramPostLinks.browserLoad(null, SHORT); check(!follow(RELAY)); check(follow(POST));
        InstagramPostLinks.browserLoad(null, RELAY); check(follow(POST));
        tap(SHORT); check(!follow("https://www.google.com/share.google?q=Other")); check(!follow(POST));
        tap(SHORT); check(!follow(RELAY)); check(!follow("https://example.org/")); check(!follow(POST));
        tap(SHORT); check(!request(RELAY, false, true, false, "GET")); check(!follow(RELAY)); check(follow(POST));
        tap(SHORT); follow(RELAY); check(!request(POST,false,false,true,"GET")); check(!follow(POST));
        tap(SHORT); follow(RELAY); check(!request(POST,false,true,true,"POST")); check(!follow(POST));
        tap(SHORT); follow(RELAY); check(!follow("https://www.instagram.com/accounts/login/"));
        tap(SHORT); follow(RELAY); check(!follow("https://www.instagram.com.example.org/p/x/"));
        tap(SHORT); follow(RELAY); check(!follow("http://www.instagram.com/p/x/"));
        tap(SHORT); InstagramPostLinks.pageFinished(null, SHORT); check(!follow(POST));
        tap(SHORT); follow(RELAY); InstagramPostLinks.pageFinished(null, RELAY); check(!follow(POST));
        tap(SHORT); InstagramPostLinks.browserLoad(null, "https://example.org/"); check(!follow(POST));
        check(!request(SHORT,false,false,true,"GET")); check(!follow(RELAY)); check(!follow(POST));
        check(!tap("https://share.google.example.org/Example1")); check(!follow(RELAY)); check(!follow(POST));
        check(!tap("https://share.google/Example1%2Ffoo")); check(!follow(RELAY)); check(!follow(POST));
        System.out.println("Passed " + count + " share redirect checks");
    }
}
