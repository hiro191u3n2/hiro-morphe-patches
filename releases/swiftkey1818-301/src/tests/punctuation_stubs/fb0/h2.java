package fb0;

import java.util.ArrayList;
import java.util.List;

/** Host reconstruction of h2.a's ordinary popup-child graph.
 * The native builder is statically reviewed separately; it is not run here.
 */
public class h2 {
    public wb0.x owner;
    public int calls;
    public List lastCandidates;
    public fc0.v1 lastFields;
    public fc0.m1 lastGeometry;
    public RuntimeException failure;
    public boolean returnNull;
    public f2 a(List candidates, fc0.v1 fields, fc0.m1 geometry) {
        calls++;
        lastCandidates = new ArrayList(candidates);
        lastFields = fields;
        lastGeometry = geometry;
        if (failure != null) throw failure;
        if (returnNull) return null;
        f2 popup = new f2();
        popup.l = 0;
        for (Object value : candidates) {
            fc0.n2 key = new fc0.n2();
            key.d = new fc0.d2() {};
            key.e = geometry;
            oc0.m behavior = new oc0.m();
            key.b = behavior;
            behavior.b = new hc0.a();
            behavior.b.b = new bf0.g();
            behavior.b.b.b = new ArrayList();
            final fb0.l2 actor = new fb0.l2();
            actor.a = new fb0.n2();
            actor.a.a = owner;
            actor.b = actor.c = (String) value;
            actor.callback = new Runnable() {
                public void run() { owner.P(null, actor.b, actor.c, null, true, null); }
            };
            hc0.o entry = new hc0.o();
            entry.action = actor;
            ((List) behavior.b.b.b).add(entry);
            popup.d.add(key);
        }
        return popup;
    }
}
