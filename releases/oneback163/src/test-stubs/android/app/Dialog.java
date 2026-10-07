package android.app;
import android.content.Context;
import android.view.Window;
import android.window.OnBackInvokedDispatcher;
public class Dialog {
    private final Context context;
    private Activity owner;
    private final Window window;
    public Dialog(Context context) { this.context = context; window = new Window(context); }
    public Context getContext() { return context; }
    public Activity getOwnerActivity() { return owner; }
    public void setOwnerActivity(Activity value) { owner = value; }
    public Window getWindow() { return window; }
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() { return window.getOnBackInvokedDispatcher(); }
    public void show() { window.attach(); window.getDecorView().focus(true); }
    public void dismiss() { window.getDecorView().detach(); }
}
