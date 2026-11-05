package android.webkit;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
public class WebView {
    public String url, script;
    public ValueCallback<String> callback;
    public int calls;
    public boolean destroyed;
    public String getUrl(){return url;}
    public Context getContext(){return new Context();}
    public boolean post(Runnable action){return new Handler(Looper.getMainLooper()).post(action);}
    public void evaluateJavascript(String input, ValueCallback<String> response){
        if(!Looper.main)throw new AssertionError("WebView off main thread");
        if(destroyed)throw new IllegalStateException("destroyed");
        script=input;callback=response;calls++;
    }
    public void flush(){}
}
