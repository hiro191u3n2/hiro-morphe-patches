package fc0;

/** Models the inspected H branch: one configured entry is direct unless
 * touch exploration selects a one-entry popup. No language variants are read.
 */
public class s1 {
    public fb0.h2 i;
    public int hCalls;
    public boolean accessible, hNull, hEmpty;
    public RuntimeException failure;
    public xc0.h override;
    public m1 lastGeometry;
    public v1 lastFields;
    public xc0.h H(m1 geometry, v1 fields) {
        hCalls++;
        lastGeometry = geometry;
        lastFields = fields;
        if (failure != null) throw failure;
        if (override != null) return override;
        if (hNull) return null;
        if (hEmpty || fields.o.isEmpty()) return xc0.d.a;
        if (!accessible && fields.o.size() == 1) return new xc0.i(fields.o.get(0));
        return new xc0.f(i.a(fields.o, fields, geometry));
    }
}
