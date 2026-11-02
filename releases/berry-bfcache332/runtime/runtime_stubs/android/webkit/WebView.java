package android.webkit;
public class WebView extends android.view.View {
    public String getUrl() { return null; }
    public void evaluateJavascript(String script, ValueCallback<String> callback) {}
    public void postVisualStateCallback(long requestId, VisualStateCallback callback) {}
    public abstract static class VisualStateCallback { public abstract void onComplete(long requestId); }
}
