package android.app;
import android.content.ContextWrapper;
import android.view.Window;
import android.window.OnBackInvokedDispatcher;
public class Activity extends ContextWrapper {
    private final Application application;
    private final int taskId;
    private final boolean root;
    private final Window window;
    public boolean destroyed;
    public boolean finishing;
    public boolean failRemove;
    public boolean failFinish;
    public int finishCalls;
    public int removeCalls;
    public Activity(Application app, int id, boolean root) {
        super(app); application = app; taskId = id; this.root = root; window = new Window(this);
    }
    public Application getApplication() { return application; }
    public Window getWindow() { return window; }
    public boolean isDestroyed() { return destroyed; }
    public boolean isFinishing() { return finishing; }
    public boolean isTaskRoot() { return root; }
    public int getTaskId() { return taskId; }
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() { return window.getOnBackInvokedDispatcher(); }
    public void finish() {
        finishCalls++;
        if (failFinish) throw new IllegalStateException("activity finish failed");
        finishing = true;
    }
    public void finishAndRemoveTask() {
        removeCalls++;
        if (failRemove) throw new IllegalStateException("activity task removal failed");
        finishing = true;
    }
}
