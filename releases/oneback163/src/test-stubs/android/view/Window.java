package android.view;
import android.content.Context;
import android.window.FakeDispatcher;
import android.window.OnBackInvokedDispatcher;
public class Window {
    private final Context context;
    private View decor;
    private FakeDispatcher dispatcher = new FakeDispatcher();
    public int decorCreations;
    public Window(Context context) { this.context = context; }
    public View getDecorView() {
        if (decor == null) { decor = new View(context); decorCreations++; }
        return decor;
    }
    public View peekDecorView() { return decor; }
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() { return dispatcher; }
    public FakeDispatcher dispatcher() { return dispatcher; }
    public void replaceDispatcher(FakeDispatcher value) { dispatcher = value; }
    public void attach() { getDecorView().attach(dispatcher); }
}
