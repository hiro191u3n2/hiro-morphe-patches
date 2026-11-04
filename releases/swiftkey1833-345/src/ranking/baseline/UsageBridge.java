package hiro.swiftkey;
import java.util.List;
/** Unchanged bridge rank method, reconstructed for boundary regression tests only. */
public final class UsageBridge {
    private static volatile UsageBridge$Session session;
    private static native boolean eligible(l40.c request);
    private static native boolean allowed(UsageBridge$Session session, wb0.n0 state);
    public static List<m40.b> rank(List<m40.b> original,l40.c request) {
        try {
            UsageBridge$Session current=session;
            if(!eligible(request)||!allowed(current,null)) return original;
            List<m40.b> ranked=FrequencyLearning.rank(original,request,true);
            return allowed(current,null)?ranked:original;
        } catch(RuntimeException ignored) { } catch(LinkageError ignored) { } return original;
    }
}
