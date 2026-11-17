package android.app;
import android.view.Window;
import android.view.WindowManager;
public class Activity {
    public Window window;
    public WindowManager manager;
    public Window getWindow() { return window; }
    public WindowManager getWindowManager() { return manager; }
}
