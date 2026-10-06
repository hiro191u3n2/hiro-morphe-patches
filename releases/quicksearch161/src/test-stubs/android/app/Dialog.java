package android.app;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.window.OnBackInvokedDispatcher;
public class Dialog implements DialogInterface {
    private final Context context;
    public final Window window;
    public int shows,dismissals,cancels;
    private boolean showing;
    public DialogInterface.OnCancelListener cancelListener;
    public DialogInterface.OnDismissListener dismissListener;
    public DialogInterface.OnKeyListener keyListener;
    private Activity owner;
    public Dialog(Context context){this.context=context;window=new Window(context);window.decor.dispatchDetach();}
    public Context getContext(){return context;}
    public Window getWindow(){return window;}
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher(){return window.back;}
    public boolean isShowing(){return showing;}
    public void show(){shows++;showing=true;window.decor.dispatchAttach();}
    public void dismiss(){if(!showing)return;dismissals++;showing=false;window.decor.dispatchDetach();if(dismissListener!=null)dismissListener.onDismiss(this);}
    public void cancel(){cancels++;if(cancelListener!=null)cancelListener.onCancel(this);dismiss();}
    public void setOnCancelListener(DialogInterface.OnCancelListener listener){cancelListener=listener;}
    public void setOnDismissListener(DialogInterface.OnDismissListener listener){dismissListener=listener;}
    public void setOnKeyListener(DialogInterface.OnKeyListener listener){keyListener=listener;}
    public void setOwnerActivity(Activity owner){this.owner=owner;}
    public Activity getOwnerActivity(){return owner;}
    public <T extends View>T findViewById(int id){return window.decor.findViewById(id);}
}
