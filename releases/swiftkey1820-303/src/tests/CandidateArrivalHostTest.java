import hiro.swiftkey.CandidateScroll;
import java.util.*;
import testutil.NativeBridge;

/** Replays the pinned native callback/lifecycle contract with explicit host UI doubles. */
public final class CandidateArrivalHostTest {
    static int assertions, scenarios;
    static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    static l40.c request(int count) {
        l40.c value = CandidateViewportHostTest.query(); value.g = count; return value;
    }
    static final String[] INITIAL = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
    static CandidateViewportHostTest.Grid grid(boolean panelFirst) {
        CandidateViewportHostTest.Grid g = new CandidateViewportHostTest.Grid(panelFirst, INITIAL);
        g.context.g = 120;
        g.adapter.l.G = 4; g.view.height = 160; g.view.renderGeometry = true;
        g.adapter.H(); g.view.runDeferredLayout(); g.adapter.a.clear(); g.adapter.fullCalls = 0;
        g.visible(2, 6, -17);
        return g;
    }
    static void order(CandidateViewportHostTest.Grid g, String... expected) {
        ArrayList<String> actual = new ArrayList<>();
        for (Object value : g.provider.b) actual.add(((m40.b) value).getUserFacingText());
        check(actual.equals(Arrays.asList(expected)), "Survivor order changed: " + actual);
    }
    static int index(CandidateViewportHostTest.Grid g, String text) {
        for (int i = 0; i < g.provider.b.size(); i++)
            if (text.equals(((m40.b) g.provider.b.get(i)).getUserFacingText())) return i;
        throw new AssertionError("Missing displayed candidate " + text);
    }
    static void pixel(CandidateViewportHostTest.Grid g, String word, int offset) {
        g.view.runDeferredLayout();
        android.view.View child = g.adapter.l.q(index(g, word));
        check(child != null && child.top - g.adapter.l.r.padding == offset,
                "Candidate pixel anchor moved after row packing: " + word);
        check(g.view.stopCalls == 0 && g.view.scrollState == 2, "Native reset stopped the current scroll");
    }
    static void fresh(CandidateViewportHostTest.Grid g, int version, int oldCount) {
        for (int i = 0; i < g.provider.b.size(); i++) {
            Object candidate = g.provider.b.get(i);
            check(((CandidateScrollHostTest.C) candidate).metadata == version, "Fresh metadata discarded at " + i);
            if (i < oldCount) check(g.adapter.bound.get(i) == candidate, "Old commit/long-press actor retained at " + i);
        }
    }
    // The native main Handler callback delivers its captured type, with no
    // current-task check. The grid subscribes to every type, not just g.
    static void nonExpanded() {
        for (ib0.m type : new ib0.m[]{ib0.m.a, ib0.m.b, ib0.m.i}) {
            for (boolean panelFirst : new boolean[]{true, false}) {
                final CandidateViewportHostTest.Grid g = grid(panelFirst);
                final l40.c next = request(120);
                ArrayDeque<Runnable> mainQueue = new ArrayDeque<>();
                mainQueue.add(() -> g.update(CandidateViewportHostTest.words(next, 2,
                        "L", "C", "A", "D", "F", "G", "H", "I", "J", "K", "N", "O"), next, type));
                mainQueue.removeFirst().run();
                check(g.view.stopCalls == 0, "A queued non-EXPANDED result reset the visible expanded grid");
                order(g, "A", "C", "D", "F", "G", "H", "I", "J", "K", "L", "N", "O");
                pixel(g, "C", -17); fresh(g, 2, 12); scenarios++;
            }
        }
    }
    static void smaller() {
        for (ib0.m type : new ib0.m[]{ib0.m.a, ib0.m.g, ib0.m.i}) {
            CandidateViewportHostTest.Grid g = grid(true);
            Object before = g.provider.b;
            l40.c small = request(12);
            g.update(CandidateViewportHostTest.words(small, 2, "C", "A", "D", "E"), small, type);
            check(g.provider.b == before && g.notifier.dispatches == 0,
                    "Late smaller-limit result replaced an already-scrolled expanded result");
            check(g.view.stopCalls == 0, "Late smaller-limit result stopped scrolling");
            order(g, INITIAL); fresh(g, 1, 12); scenarios++;
        }
        // A pending preserved layout also survives an older queued request.
        CandidateViewportHostTest.Grid g = grid(false);
        l40.c newer = request(150);
        g.update(CandidateViewportHostTest.words(newer, 2,
                "L", "C", "A", "D", "F", "G", "H", "I", "J", "K", "N", "O"), newer, ib0.m.g);
        Object latest = g.provider.b;
        l40.c older = request(120);
        g.update(CandidateViewportHostTest.words(older, 3, "A", "C", "D"), older, ib0.m.a);
        check(g.provider.b == latest, "Delayed request displaced a pending logical viewport");
        pixel(g, "C", -17); g.panel.n.b(g.view, 0, 0);
        check(g.panel.n.fetches == 0, "Rejected late result invalidated the layout watermark"); scenarios++;
    }
    static void survivors() {
        CandidateViewportHostTest.Grid g = grid(true);
        l40.c next = request(150);
        List<m40.b> incoming = CandidateViewportHostTest.words(next, 2,
                "Z", "L", "C", "A", "D", "F", "G", "H", "I", "J", "K", "N", "O", "P");
        g.update(incoming, next, ib0.m.g);
        order(g, "A", "C", "D", "F", "G", "H", "I", "J", "K", "L", "Z", "N", "O", "P");
        for (Object value : g.provider.b) check(incoming.contains(value), "Removed/old candidate was resurrected");
        check(g.provider.b.size() == incoming.size(), "Fresh candidate duplicated or lost");
        pixel(g, "C", -17); fresh(g, 2, 12); scenarios++;
    }
    static void sameSize() {
        for (ib0.m type : new ib0.m[]{ib0.m.g, ib0.m.a}) {
            CandidateViewportHostTest.Grid g = grid(false);
            l40.c next = request(120);
            g.update(CandidateViewportHostTest.words(next, 2,
                    "C", "L", "A", "B", "D", "E", "F", "G", "H", "I", "J", "K"), next, type);
            order(g, INITIAL);
            check(g.notifier.dispatches == 1, "Native same-text early return discarded reordered fresh actors");
            pixel(g, "C", -17); fresh(g, 2, 12); scenarios++;
        }
    }
    static void collision() {
        for (boolean growth : new boolean[]{false, true}) for (boolean alreadyOrdered : new boolean[]{false, true}) {
            CandidateViewportHostTest.Grid g = grid(true);
            l40.c old = request(120), next = request(120);
            List<m40.b> before = Arrays.asList(new CandidateScrollHostTest.C("A", "x", "", old, 1),
                    new CandidateScrollHostTest.C("B", "x", "", old, 1),
                    new CandidateScrollHostTest.C("C", "x", "", old, 1));
            g.provider.load(before); g.view.height = 60; g.adapter.H(); g.view.runDeferredLayout();
            g.adapter.a.clear(); g.visible(2, 1, -17);
            ArrayList<m40.b> after = new ArrayList<>(Arrays.asList(
                    new CandidateScrollHostTest.C("C", "x", "", next, 2),
                    new CandidateScrollHostTest.C("A", "x", "", next, 2),
                    new CandidateScrollHostTest.C("D", "x", "", next, 2)));
            if (alreadyOrdered) Collections.swap(after, 0, 1);
            if (growth) after.add(new CandidateScrollHostTest.C("E", "y", "", next, 2));
            g.update(after, next, ib0.m.g);
            if (growth) order(g, "A", "C", "D", "E"); else order(g, "A", "C", "D");
            fresh(g, 2, 3); pixel(g, "C", -17);
            rb0.l foreign = new rb0.l(g.adapter.l);
            NativeBridge.watermark(foreign, g.view, 0, 0);
            check(foreign.c == 0, "A foreign listener consumed reset=false preservation");
            g.panel.n.b(g.view, 0, 0);
            check(g.panel.n.c == g.adapter.l.W0() && g.panel.n.fetches == 0,
                    "Reset=false layout retained an obsolete fetch watermark");
            int nextLast = g.panel.n.c + 1;
            g.adapter.l.last = nextLast; g.panel.n.b(g.view, 0, 8);
            check(g.panel.n.fetches == 1 && g.panel.n.c == nextLast,
                    "Real movement after reset=false preservation did not request more candidates");
            if (growth) check(g.adapter.a.full == 0 && g.adapter.a.changes == 1
                    && g.adapter.a.payload != null && g.adapter.a.inserts == 1,
                    "Commit-prefix growth lost native full-prefix fresh payload rebinding");
            scenarios++;
        }
    }
    static void reopen() {
        // Native detach clears r and listeners; super.onAttached delivers the
        // cached result before r=true, G/notify and the final native w0(0).
        CandidateViewportHostTest.Grid g = grid(true);
        g.adapter.r = false; g.view.attached = false; g.notifier.listeners.clear();
        l40.c small = request(12);
        g.view.attached = true;
        g.update(CandidateViewportHostTest.words(small, 2, "A", "C", "D", "E"), small, ib0.m.a);
        check(g.provider.b.size() == 4, "Reattach cached result was rejected using the previous session's limit");
        g.notifier.listeners.add(g.panel); g.notifier.listeners.add(g.adapter); g.adapter.r = true;
        g.adapter.H(); g.view.w0(0);
        g.update(CandidateViewportHostTest.words(small, 3, "C", "D", "A"), small, ib0.m.a);
        check(g.provider.b.size() == 3, "Native pending-to-zero was mistaken for a preserved viewport"); scenarios++;

        g = grid(false); g.visible(0, 4, 0);
        g.update(CandidateViewportHostTest.words(small, 2, "C", "D", "A"), small, ib0.m.a);
        check(g.provider.b.size() == 3, "Unscrolled first display rejected an initial small result"); scenarios++;
        g = grid(true); small.k = "different input";
        g.update(CandidateViewportHostTest.words(small, 2, "C", "D", "A"), small, ib0.m.a);
        check(g.provider.b.size() == 3 && g.view.stopCalls > 0, "Changed input inherited the old expanded request limit"); scenarios++;
        g = grid(true); small = request(12); small.c = new Object();
        g.update(CandidateViewportHostTest.words(small, 2, "C", "D", "A"), small, ib0.m.a);
        check(g.provider.b.size() == 3 && g.view.stopCalls > 0, "Changed context inherited old request suppression"); scenarios++;
    }
    static void guards() throws Exception {
        CandidateViewportHostTest.Grid g = grid(true); l40.c next = request(120);
        ib0.a source = new ib0.a(new lh0.a(), CandidateViewportHostTest.words(next, 2,
                "C", "L", "A", "B", "D", "E", "F", "G", "H", "I", "J", "K"), ib0.m.g, next);
        ib0.a prepared = CandidateScroll.stabilize(g.provider, source);
        check(!NativeBridge.skip(g.provider, prepared), "Prepared equal-text update was not eligible");
        check(NativeBridge.skip(g.provider, new ib0.a(prepared.d, prepared.a, prepared.b, prepared.c)),
                "Different incoming container borrowed the prepared no-op bypass");
        g.view.width++;
        check(NativeBridge.skip(g.provider, prepared), "Changed layout borrowed the prepared no-op bypass");
        g.view.width--; CandidateScroll.finishUpdate(g.provider);
        check(NativeBridge.skip(g.provider, prepared), "No-op bypass survived its update scope"); scenarios++;

        g = grid(true); Object before = g.provider.b;
        g.update(CandidateViewportHostTest.words(next, 2, INITIAL), next, ib0.m.g);
        check(g.provider.b == before && g.notifier.dispatches == 0, "Ordinary native identical-result no-op changed"); scenarios++;
        g = grid(true); prepared = CandidateScroll.stabilize(g.provider, source);
        CandidateScroll.stabilize(g.provider, new ib0.a(new lh0.a(), CandidateViewportHostTest.words(next, 3, INITIAL), ib0.m.g, next));
        check(NativeBridge.skip(g.provider, prepared), "Old epoch borrowed a later update's no-op bypass");
        CandidateScroll.finishUpdate(g.provider); scenarios++;

        // The first callback after a reset=false restore may be real movement.
        // Consume the token without rebasing c; a later 0/0 must not borrow it.
        g = grid(false); next = request(120);
        g.update(CandidateViewportHostTest.words(next, 2,
                "C", "L", "A", "B", "D", "E", "F", "G", "H", "I", "J", "K"), next, ib0.m.g);
        g.view.runDeferredLayout();
        g.adapter.l.last = 8; g.panel.n.b(g.view, 0, 12);
        check(g.panel.n.fetches == 1 && g.panel.n.c == 8,
                "First real movement after reset=false update was suppressed");
        g.adapter.l.last = 6; g.panel.n.b(g.view, 0, 0);
        check(g.panel.n.fetches == 1 && g.panel.n.c == 8,
                "Consumed reset=false token rebased an unrelated later callback"); scenarios++;
    }
    public static void main(String[] args) throws Exception {
        String selected = args.length == 0 ? "all" : args[0];
        if (selected.equals("all") || selected.equals("nonexpanded")) nonExpanded();
        if (selected.equals("all") || selected.equals("smaller")) smaller();
        if (selected.equals("all") || selected.equals("survivors")) survivors();
        if (selected.equals("all") || selected.equals("samesize")) sameSize();
        if (selected.equals("all") || selected.equals("collision")) collision();
        if (selected.equals("all") || selected.equals("reopen")) reopen();
        if (selected.equals("all") || selected.equals("guards")) guards();
        System.out.println("PASS CandidateArrival " + scenarios + " scenarios, " + assertions + " assertions");
        System.out.println("REPORT {\"status\":\"PASSED\",\"scenarios\":" + scenarios
                + ",\"assertions\":" + assertions + "}");
    }
}
