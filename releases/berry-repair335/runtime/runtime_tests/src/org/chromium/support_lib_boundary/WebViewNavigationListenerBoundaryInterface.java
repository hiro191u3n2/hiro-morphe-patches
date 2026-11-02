package org.chromium.support_lib_boundary;
import java.lang.reflect.InvocationHandler;
public interface WebViewNavigationListenerBoundaryInterface {
    String[] getSupportedFeatures();
    void onNavigationStarted(InvocationHandler navigation);
    void onNavigationCompleted(InvocationHandler navigation);
    void onPageDeleted(InvocationHandler page);
}
