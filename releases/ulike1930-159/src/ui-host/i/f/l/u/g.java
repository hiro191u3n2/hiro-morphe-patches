package i.f.l.u;

/** Host-only fixture for the already registered camera scene. */
public class g {
    public j state;
    public RuntimeException runtimeFailure;
    public LinkageError linkageFailure;
    public int reads;

    public j k() {
        reads++;
        if (runtimeFailure != null) throw runtimeFailure;
        if (linkageFailure != null) throw linkageFailure;
        return state;
    }
}
