package android.view;
public class KeyEvent {
    public static final int ACTION_DOWN=0, ACTION_UP=1, ACTION_MULTIPLE=2, KEYCODE_BACK=4;
    private final int action, keyCode;
    public boolean canceled;
    public int repeatCount;
    public KeyEvent(int action, int keyCode) { this.action=action; this.keyCode=keyCode; }
    public int getAction() { return action; }
    public int getKeyCode() { return keyCode; }
    public boolean isCanceled() { return canceled; }
    public int getRepeatCount() { return repeatCount; }
}
