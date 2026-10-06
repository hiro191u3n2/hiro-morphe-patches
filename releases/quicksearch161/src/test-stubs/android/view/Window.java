package android.view;
import android.content.Context;
import android.window.OnBackInvokedDispatcher;
import android.window.TestBackDispatcher;
public class Window {
    public final ViewGroup decor;
    public final TestBackDispatcher back = new TestBackDispatcher();
    public Window(Context context) {decor=new ViewGroup(context);}
    public View getDecorView() {return decor;}
    public View peekDecorView() {return decor;}
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {return back;}
}
