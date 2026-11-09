package android.window;
import java.util.LinkedHashMap;
import java.util.Map;
public final class TestBackDispatcher implements OnBackInvokedDispatcher {
    public final Map<OnBackInvokedCallback,Integer> callbacks = new LinkedHashMap<>();
    public int registrations, unregistrations;
    @Override public void registerOnBackInvokedCallback(int priority, OnBackInvokedCallback callback) {
        registrations++; callbacks.remove(callback); callbacks.put(callback,priority);
    }
    @Override public void unregisterOnBackInvokedCallback(OnBackInvokedCallback callback) {
        unregistrations++; callbacks.remove(callback);
    }
    public OnBackInvokedCallback top() {
        OnBackInvokedCallback result = null; int priority = Integer.MIN_VALUE;
        for (Map.Entry<OnBackInvokedCallback,Integer> e : callbacks.entrySet()) {
            if (e.getValue() >= priority) { result = e.getKey(); priority = e.getValue(); }
        }
        return result;
    }
    public void invoke() { OnBackInvokedCallback c = top(); if (c != null) c.onBackInvoked(); }
}
