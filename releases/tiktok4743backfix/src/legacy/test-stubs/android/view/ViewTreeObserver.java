package android.view;

import java.util.ArrayList;
import java.util.List;

/** Host-only lifecycle fixture. Never packaged in the Android extension. */
public final class ViewTreeObserver {
    public interface OnPreDrawListener { boolean onPreDraw(); }
    private final List<OnPreDrawListener> listeners = new ArrayList<>();
    private boolean alive = true;
    public boolean isAlive() { return alive; }
    public void addOnPreDrawListener(OnPreDrawListener listener) { listeners.add(listener); }
    public void removeOnPreDrawListener(OnPreDrawListener listener) { listeners.remove(listener); }
    public int listenerCount() { return listeners.size(); }
    public boolean draw() {
        boolean draw = true;
        for (OnPreDrawListener listener : new ArrayList<>(listeners)) draw &= listener.onPreDraw();
        return draw;
    }
    public void kill() { alive = false; listeners.clear(); }
}
