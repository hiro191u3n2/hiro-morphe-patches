package hiro.berrybfcache;
import android.webkit.WebView;
import java.lang.ref.WeakReference;

/** Does not retain a tab, Activity, WebViewClient, or WebView strongly. */
public final class NativeCompletion implements Runnable {
    private final WeakReference<CompletionTarget> target;
    private final WeakReference<WebView> view;
    private final String url;
    public NativeCompletion(CompletionTarget target, WebView view, String url) {
        this.target = new WeakReference<CompletionTarget>(target);
        this.view = new WeakReference<WebView>(view);
        this.url = url;
    }
    @Override public void run() {
        CompletionTarget t = target.get();
        WebView v = view.get();
        if (t == null || v == null || url == null) return;
        String current;
        try { current = v.getUrl(); }
        catch (RuntimeException destroyed) { return; }
        if (url.equals(current)) t.hiroCompleteBfcache(v, url);
    }
}
