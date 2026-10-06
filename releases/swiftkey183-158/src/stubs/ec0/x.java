package ec0;
public final class x {
    public f a;
    public final wb0.z0 e = new wb0.z0();
    public boolean active = true;
    public boolean failActive;
    public boolean failActiveLinkage;
    public boolean C() {
        if (failActive) throw new IllegalStateException("test active-context failure");
        if (failActiveLinkage) throw new NoClassDefFoundError("test active-context linkage");
        return active && a != null;
    }
}
