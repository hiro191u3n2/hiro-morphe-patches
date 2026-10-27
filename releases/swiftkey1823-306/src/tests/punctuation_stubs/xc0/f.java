package xc0;
public class f implements h {
    public fb0.f2 a;
    public f() {}
    public f(fb0.f2 value) { a = value; }
    public java.util.Set c() {
        java.util.Set result = new java.util.HashSet();
        if (a != null) for (Object key : a.d) {
            hc0.a handler = ((oc0.m) ((fc0.n2) key).b).b;
            for (Object entry : (java.util.Collection) handler.b.b) {
                Object actor = ((hc0.o) entry).action;
                if (actor instanceof fb0.l2) result.add(((fb0.l2) actor).b);
            }
        }
        return result;
    }
}
