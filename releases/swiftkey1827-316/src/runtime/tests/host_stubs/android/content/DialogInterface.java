package android.content;
/** Host fake: platform window rendering is not exercised. */
public interface DialogInterface {
    void dismiss();
    interface OnClickListener {void onClick(DialogInterface dialog,int which);}
}

