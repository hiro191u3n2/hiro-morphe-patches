package org.chromium.support_lib_boundary;
import java.lang.reflect.InvocationHandler;
public interface WebViewNavigationBoundaryInterface extends IsomorphicObjectBoundaryInterface {
    boolean isSameDocument(); boolean isHistory(); boolean isReload();
    boolean didCommit(); boolean didCommitErrorPage(); String getUrl(); InvocationHandler getPage();
}
