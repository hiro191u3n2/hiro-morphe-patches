package i.f.l.u;

/** Host-only fixture for the selected front-camera Boolean. */
public class p<T> {
    public T value;
    public RuntimeException runtimeFailure;
    public LinkageError linkageFailure;
    public int reads;

    public T a() {
        reads++;
        if (runtimeFailure != null) throw runtimeFailure;
        if (linkageFailure != null) throw linkageFailure;
        return value;
    }
}
