package android.view;
public class View {
    public boolean post(Runnable runnable) { return false; }
    public boolean postDelayed(Runnable runnable, long delay) { return false; }
    public boolean isAttachedToWindow() { return false; }
    public boolean isShown() { return false; }
    public void postInvalidateOnAnimation() {}
}
