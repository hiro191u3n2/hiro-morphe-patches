package android.view;
public final class KeyEvent {
    public static final int KEYCODE_BACK = 4;
    public static final int KEYCODE_VOLUME_UP = 24;
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MULTIPLE = 2;
    private final int action;
    private final int code;
    private final boolean cancelled;
    public KeyEvent(int action, int code) { this(action, code, false); }
    public KeyEvent(int action, int code, boolean cancelled) {
        this.action = action; this.code = code; this.cancelled = cancelled;
    }
    public int getAction() { return action; }
    public int getKeyCode() { return code; }
    public boolean isCanceled() { return cancelled; }
}
