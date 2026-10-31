package android.app;
import android.content.ContextWrapper;
import android.view.Window;
import android.window.OnBackInvokedDispatcher;
public class Activity extends ContextWrapper {
    private android.content.Intent intent;
    public android.content.Intent getIntent(){return intent;}
    public void setIntent(android.content.Intent value){intent=value;}
    private final Application application;
    private final int taskId;
    private final boolean root;
    private final Window window;
    public ActionBar actionBar;
    public ActionBar getActionBar(){return actionBar;}
    public int nativeBackCalls; public Runnable nativeBackAction; public Runnable startAction; public int startCalls; public android.content.Intent lastStart;
    public int userInteractionCalls;
    private boolean backKeyTracking;
    public void onUserInteraction(){userInteractionCalls++;}
    /** Android 16 Activity.dispatchKeyEvent calls onUserInteraction for BOTH
     * key actions, before Window/KeyEvent dispatch. Model that ordering rather
     * than invoking an app Back callback directly. The Back branch below is
     * the legacy/public-onBackPressed path; API33+ registered system callbacks
     * are exercised separately, not assumed to dispatch a legacy key. */
    public boolean dispatchKeyEvent(android.view.KeyEvent event){
        onUserInteraction();
        if(event.getKeyCode()!=android.view.KeyEvent.KEYCODE_BACK)return false;
        if(event.getAction()==android.view.KeyEvent.ACTION_DOWN){backKeyTracking=true;return true;}
        if(event.getAction()!=android.view.KeyEvent.ACTION_UP)return false;
        boolean tracked=backKeyTracking;backKeyTracking=false;
        if(tracked&&!event.isCanceled()){onBackPressed();return true;}
        return false;
    }
    public void startActivity(android.content.Intent intent){startCalls++;lastStart=intent;if(startAction!=null)startAction.run();}
    public final FragmentManager fragments=new FragmentManager();
    public FragmentManager getFragmentManager(){return fragments;}
    public void onBackPressed(){nativeBackCalls++;if(nativeBackAction!=null)nativeBackAction.run();}
    public boolean changingConfigurations;
    public boolean isChangingConfigurations(){return changingConfigurations;}
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
