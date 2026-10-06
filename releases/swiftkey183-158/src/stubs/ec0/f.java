package ec0;
public final class f implements n {
    public String a;
    public int b;
    public int c;
    public int d;
    public final de0.g i = new de0.g();
    public boolean failText;
    public boolean failTextLinkage;
    public boolean failComposition;
    public String getText() {
        if (failText) throw new IllegalStateException("test text failure");
        if (failTextLinkage) throw new NoClassDefFoundError("test text linkage");
        return a;
    }
    public int P() { return c; }
    public int A() { return b; }
    public int I() { return d; }
    public String s() {
        if (failComposition) throw new IllegalStateException("test composition failure");
        return (String) i.c;
    }
}
