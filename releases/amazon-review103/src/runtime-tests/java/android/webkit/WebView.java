package android.webkit;
public class WebView {
    public String url;
    public Runnable pending;
    public String script;
    public int calls;
    public boolean destroyed;
    public String getUrl() { return url; }
    public boolean post(Runnable runnable) { pending = runnable; return true; }
    public void evaluateJavascript(String input, ValueCallback<String> callback) {
        if (destroyed) throw new IllegalStateException("destroyed");
        script = input;
        calls++;
    }
    public void flush() { if (pending != null) { Runnable next = pending; pending = null; next.run(); } }
}
