package android.content;
public interface DialogInterface {
    void dismiss();
    void cancel();
    interface OnDismissListener { void onDismiss(DialogInterface dialog); }
    interface OnCancelListener { void onCancel(DialogInterface dialog); }
    interface OnKeyListener { boolean onKey(DialogInterface dialog, int keyCode, android.view.KeyEvent event); }
}
