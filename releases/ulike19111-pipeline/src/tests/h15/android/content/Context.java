package android.content;
public class Context {
    public Object service;
    public Throwable failure;
    public int requests;
    public Context(Object service) { this.service=service; }
    public Object getSystemService(String name) {
        requests++;
        if (!"performance_hint".equals(name)) throw new AssertionError("wrong service " + name);
        if (failure instanceof Error) throw (Error) failure;
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        return service;
    }
}
