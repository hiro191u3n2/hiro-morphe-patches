package i.f.l.u;

/** Host-only fixture for the camera screen's facing property. */
public final class j {
    public p<Object> facing;
    public RuntimeException runtimeFailure;
    public LinkageError linkageFailure;
    public int reads;

    public p<Object> x() {
        reads++;
        if (runtimeFailure != null) throw runtimeFailure;
        if (linkageFailure != null) throw linkageFailure;
        return facing;
    }
}
