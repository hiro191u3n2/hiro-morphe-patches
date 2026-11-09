package android.app;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.window.OnBackInvokedDispatcher;
public class Activity extends Context {
    public int taskId=1;
    public boolean taskRoot,finishing,destroyed;
    public int finishCalls,removeCalls,startCalls;
    public final Window window=new Window(this);
    public Intent startedIntent;
    public RuntimeException startFailure;
    public int getTaskId(){return taskId;}
    public boolean isTaskRoot(){return taskRoot;}
    public boolean isFinishing(){return finishing;}
    public boolean isDestroyed(){return destroyed;}
    public void finish(){finishCalls++;finishing=true;}
    public void finishAndRemoveTask(){removeCalls++;finishing=true;}
    public void startActivity(Intent intent){startCalls++;startedIntent=intent;if(startFailure!=null)throw startFailure;}
    public Window getWindow(){return window;}
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher(){return window.back;}
    public <T extends View>T findViewById(int id){return window.decor.findViewById(id);}
    public void runOnUiThread(Runnable action){action.run();}
    public void onBackPressed(){finish();}
    public boolean dispatchKeyEvent(android.view.KeyEvent event){return false;}
}
