package android.os;
public class Handler {
    public int removed;
    public void removeCallbacksAndMessages(Object token) { removed++; }
    public boolean post(Runnable r) { r.run(); return true; }
}
