package android.app;
import android.content.Context;
import android.content.DialogInterface;
/** Observable host fake for the real Android standard single-choice dialog API. */
public final class AlertDialog implements DialogInterface {
    public static AlertDialog lastShown;
    public static boolean failShow;
    public Context context;
    public CharSequence title,negativeTitle;
    public CharSequence[] items;
    public int checkedIndex,dismissals;
    public DialogInterface.OnClickListener choiceListener,negativeListener;
    public static void reset(){lastShown=null;failShow=false;}
    public void dismiss(){dismissals++;}
    public void select(int index){choiceListener.onClick(this,index);}
    public void cancel(){if(negativeListener!=null)negativeListener.onClick(this,-2);dismiss();}
    public static final class Builder {
        private final AlertDialog dialog=new AlertDialog();
        public Builder(Context context){dialog.context=context;}
        public Builder setTitle(CharSequence title){dialog.title=title;return this;}
        public Builder setSingleChoiceItems(CharSequence[] items,int selected,DialogInterface.OnClickListener listener){dialog.items=items.clone();dialog.checkedIndex=selected;dialog.choiceListener=listener;return this;}
        public Builder setNegativeButton(CharSequence title,DialogInterface.OnClickListener listener){dialog.negativeTitle=title;dialog.negativeListener=listener;return this;}
        public AlertDialog show(){if(failShow)throw new IllegalStateException("HOST_WINDOW_FAILURE");lastShown=dialog;return dialog;}
    }
}
