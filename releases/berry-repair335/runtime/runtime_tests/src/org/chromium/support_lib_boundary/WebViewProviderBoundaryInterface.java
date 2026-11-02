package org.chromium.support_lib_boundary;
import java.lang.reflect.InvocationHandler;
import java.util.concurrent.Executor;
public interface WebViewProviderBoundaryInterface {
    void addWebViewNavigationListener(Executor executor, InvocationHandler listener);
}
