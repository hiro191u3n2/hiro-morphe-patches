package android.app;
import android.content.Context;
import android.content.DialogInterface;
public class AlertDialog extends Dialog {
    public AlertDialog(Context context){super(context);}
    public static class Builder {
        private final Context context;
        public DialogInterface.OnCancelListener cancelListener;
        public DialogInterface.OnKeyListener keyListener;
        public Builder(Context context){this.context=context;}
        public Context getContext(){return context;}
        public Builder setOnCancelListener(DialogInterface.OnCancelListener value){cancelListener=value;return this;}
        public Builder setOnKeyListener(DialogInterface.OnKeyListener value){keyListener=value;return this;}
        public AlertDialog create(){AlertDialog d=new AlertDialog(context);d.cancelListener=cancelListener;d.keyListener=keyListener;return d;}
        public AlertDialog show(){AlertDialog d=create();d.show();return d;}
    }
}
