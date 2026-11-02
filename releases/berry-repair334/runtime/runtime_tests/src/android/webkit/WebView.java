package android.webkit;
import java.util.ArrayList;
public class WebView extends android.view.View {
    public String url;
    public boolean throwOnEvaluate;
    public boolean throwOnVisual;
    public final ArrayList<ValueCallback<String>> reads = new ArrayList<ValueCallback<String>>();
    public final ArrayList<VisualStateCallback> visuals = new ArrayList<VisualStateCallback>();
    public final ArrayList<Long> requestIds = new ArrayList<Long>();
    public WebView(String url) { this.url = url; }
    public String getUrl() { return url; }
    public void evaluateJavascript(String script, ValueCallback<String> callback) { if(throwOnEvaluate)throw new IllegalStateException("evaluate unavailable"); reads.add(callback); }
    public void postVisualStateCallback(long requestId, VisualStateCallback callback) {
        if(throwOnVisual)throw new IllegalStateException("visual unavailable"); requestIds.add(Long.valueOf(requestId)); visuals.add(callback);
    }
    public void answer(int index, String value) { reads.get(index).onReceiveValue(value); }
    public void draw(int index) { visuals.get(index).onComplete(requestIds.get(index).longValue()); }
    public abstract static class VisualStateCallback { public abstract void onComplete(long requestId); }
}
