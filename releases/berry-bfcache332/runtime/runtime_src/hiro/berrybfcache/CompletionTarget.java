package hiro.berrybfcache;
import android.webkit.WebView;
/** Implemented by Berry's existing WebViewClient through the guarded patch. */
public interface CompletionTarget {
    void hiroCompleteBfcache(WebView view, String url);
}
