package android.window;
import java.util.LinkedHashMap;
import java.util.Map;
/** Models the documented priority ordering and reverse order within the same priority. */
public final class FakeDispatcher implements OnBackInvokedDispatcher {
    private final LinkedHashMap<OnBackInvokedCallback, Integer> callbacks =
            new LinkedHashMap<OnBackInvokedCallback, Integer>();
    public int registrations;
    public int unregistrations;
    @Override public void registerOnBackInvokedCallback(int priority, OnBackInvokedCallback callback) {
        registrations++;
        callbacks.remove(callback);
        callbacks.put(callback, Integer.valueOf(priority));
    }
    @Override public void unregisterOnBackInvokedCallback(OnBackInvokedCallback callback) {
        unregistrations++;
        callbacks.remove(callback);
    }
    public OnBackInvokedCallback top() {
        OnBackInvokedCallback top = null;
        int priority = Integer.MIN_VALUE;
        for (Map.Entry<OnBackInvokedCallback, Integer> entry : callbacks.entrySet()) {
            if (entry.getValue().intValue() >= priority) {
                top = entry.getKey(); priority = entry.getValue().intValue();
            }
        }
        return top;
    }
    public void invokeBack() {
        OnBackInvokedCallback callback = top();
        if (callback == null) throw new AssertionError("no BACK callback registered");
        callback.onBackInvoked();
    }
    public int count() { return callbacks.size(); }
}
