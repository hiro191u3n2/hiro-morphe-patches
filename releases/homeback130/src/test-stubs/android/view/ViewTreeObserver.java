package android.view;
import java.util.ArrayList;
import java.util.List;
public final class ViewTreeObserver {
    public interface OnWindowFocusChangeListener { void onWindowFocusChanged(boolean focused); }
    private final List<OnWindowFocusChangeListener> listeners = new ArrayList<OnWindowFocusChangeListener>();
    public interface OnPreDrawListener {boolean onPreDraw();}
    private final List<OnPreDrawListener> draws=new ArrayList<OnPreDrawListener>();
    public void addOnPreDrawListener(OnPreDrawListener v){draws.add(v);}public void removeOnPreDrawListener(OnPreDrawListener v){draws.remove(v);}
    public void draw(){for(OnPreDrawListener d:new ArrayList<OnPreDrawListener>(draws))d.onPreDraw();}
    public int drawCount(){return draws.size();}
    private boolean alive = true;
    public boolean isAlive() { return alive; }
    public void addOnWindowFocusChangeListener(OnWindowFocusChangeListener listener) { listeners.add(listener); }
    public void removeOnWindowFocusChangeListener(OnWindowFocusChangeListener listener) { listeners.remove(listener); }
    public void dispatchFocus(boolean focused) {
        for (OnWindowFocusChangeListener listener : new ArrayList<OnWindowFocusChangeListener>(listeners))
            listener.onWindowFocusChanged(focused);
    }
    public int listenerCount() { return listeners.size(); }
    public void kill() { alive = false; listeners.clear();draws.clear(); }
}
