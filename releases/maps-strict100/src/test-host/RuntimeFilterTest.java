import app.hiro.mapsstrict.SearchFilter;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.RandomAccess;

/** Host behavioral tests using the exact reflected names/types of Maps' schema. */
public final class RuntimeFilterTest {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        mixedLegacyAndNative();
        onlyMatchingStillDropsNative();
        modernAndLegacyFallback();
        modernTypesAndUnknownMetadata();
        supplementaryPaths();
        unknownNamesArePreserved();
        queryIsolation();
        reusedModelAndSuccessfulMerge();
        staleMarkerAndCategoryMerge();
        categoryBypass();
        originalPreservedOnFailure();
        modelFailureAndFailOpenMerge();
        mutableInputRejected();
        unsupportedAndNull();
        check(Log.messages.size() == 1, "diagnostic is emitted at most once");
        for (String message : Log.messages) {
            check(!message.contains("SECRET_QUERY") && !message.contains("ランダ")
                    && !message.contains("DIANA"), "diagnostic contains no user data");
        }
        System.out.println("RuntimeFilterTest PASS: " + assertions + " assertions");
    }

    private static void mixedLegacyAndNative() throws Exception {
        cqgm source = response(place("RANDA 有楽町マルイ店"), place("ダイアナ 銀座本店"),
                place("ランダ銀座インズ店"));
        source.o = layer();
        source.q = "server query value";
        source.continuation = "opaque pagination token";
        source.viewport = new Object();
        sealed(source);
        String snapshot = snapshot(source);
        Object beforeList = source.d;
        Object beforeLayer = source.o.c;
        cqgm result = (cqgm) apply(source, "ランダ");
        check(result != source, "mixed response cloned");
        check(result.d.size() == 2, "only matching legacy names retained");
        check(result.d.get(0) == source.d.get(0) && result.d.get(1) == source.d.get(2),
                "order and place identity retained");
        check(result.o != source.o && result.o.c == null && result.o.b == 0x20,
                "native layer removed but other flags retained");
        check(result.o.otherMetadata == source.o.otherMetadata, "other layer metadata retained");
        check(result.q == source.q && result.continuation == source.continuation
                && result.viewport == source.viewport, "query pagination viewport preserved");
        check(!result.isMutable() && !result.d.c(), "returned message and list immutable");
        check(source.d == beforeList && source.o.c == beforeLayer, "original field identity preserved");
        check(snapshot.equals(snapshot(source)), "original object graph unchanged");
        assertImmutable(source.d);
        assertImmutable(result.d);
    }

    private static void onlyMatchingStillDropsNative() throws Exception {
        cqgm source = response(place("RANDA 東京店"));
        source.o = layer();
        sealed(source);
        cqgm result = (cqgm) apply(source, "RANDA");
        check(result != source && result.o.c == null, "all-match response still drops native extras");
        check(result.d == source.d, "unchanged immutable list can be shared");
        check(source.o.c != null, "source layer remains present");
        cqgm noLayer = sealed(response(place("RANDA 新宿店")));
        check(apply(noLayer, "RANDA") == noLayer,
                "already matching model without native layer needs no clone");
    }

    private static void modernAndLegacyFallback() throws Exception {
        cqgm source = response(place("DIANA old-format fallback"));
        source.L = list(wrapper(2, place("DIANA new-format")));
        source.o = layer();
        sealed(source);
        cqgm result = (cqgm) apply(source, "ランダ");
        check(result.L.isEmpty(), "modern all-mismatch result is legitimately empty");
        check(result.d.isEmpty(), "legacy fallback also empty, no unrelated resurrection");
        check(result.o.c == null, "empty result has no native pins");
        check(source.L.size() == 1 && source.d.size() == 1, "original empty-case lists untouched");

        cqgm mixed = response(place("RANDA fallback"), place("DIANA fallback"));
        chaq first = wrapper(2, place("RANDA 支店"));
        first.visibilityFlags = 7;
        chaq metadata = wrapper(3, "continuation section metadata");
        mixed.L = list(first, wrapper(2, place("DIANA 店")), metadata);
        sealed(mixed);
        cqgm filtered = (cqgm) apply(mixed, "RANDA");
        check(filtered.L.size() == 2 && filtered.L.get(0) == first
                && filtered.L.get(1) == metadata, "modern order wrapper flags and metadata preserved");
        check(((chaq) filtered.L.get(0)).visibilityFlags == 7, "visibility flags unmodified");
        check(filtered.d.size() == 1, "parallel legacy representation filtered too");
    }

    private static void modernTypesAndUnknownMetadata() throws Exception {
        cqgm source = response();
        chaq altMatch = wrapper(8, alternate("RANDA 渋谷店"));
        chaq altMiss = wrapper(8, alternate("シューファンタジー"));
        cfhm mixedGroup = group(place("DIANA"), place("ランダ 神戸店"));
        chaq groupWrapper = wrapper(5, mixedGroup);
        chaq unknown = wrapper(73, new Object());
        source.L = list(altMatch, altMiss, groupWrapper,
                wrapper(5, group(place("DIANA"))),
                wrapper(6, promoted(place("RANDA 大阪店"))),
                wrapper(6, promoted(place("DIANA"))),
                wrapper(7, pinned(place("RANDA 名古屋店"))),
                wrapper(7, pinned(place("DIANA"))), unknown);
        sealed(source);
        String snapshot = snapshot(source);
        cqgm result = (cqgm) apply(source, "ランダ");
        check(result.L.size() == 5, "all known place-bearing modern tags filtered");
        check(result.L.get(0) == altMatch && result.L.get(4) == unknown,
                "alternative title and unknown metadata handling");
        chaq newGroupWrapper = (chaq) result.L.get(1);
        check(newGroupWrapper != groupWrapper && newGroupWrapper.c == 5,
                "changed group wrapper copied without changing tag");
        cfhm newGroup = (cfhm) newGroupWrapper.d;
        check(newGroup != mixedGroup && newGroup.l.size() == 1,
                "group payload copied and filtered");
        check(((cffs) newGroup.l.get(0)).c.l.equals("ランダ 神戸店"), "correct grouped place retained");
        check(snapshot.equals(snapshot(source)), "modern original graph untouched");
    }

    private static void supplementaryPaths() throws Exception {
        cqgm source = response(place("RANDA 本店"));
        source.i = new cffu();
        source.i.c = new cfhn();
        source.i.c.b = list(group(place("DIANA")), group(place("RANDA 京都店"), place("DIANA")));
        source.i.d = new cfha();
        source.i.d.b = list(pinned(place("DIANA")), pinned(place("ランダ 福岡店")));
        source.i.g = new cfmj();
        source.i.g.b = list(promoted(place("DIANA")), promoted(place("RANDA 札幌店")));
        source.i.unrelatedMetadata = new Object();
        sealed(source);
        String snapshot = snapshot(source);
        cqgm result = (cqgm) apply(source, "ランダ");
        check(result.i != source.i, "supplement container cloned");
        check(result.i.c.b.size() == 1 && ((cfhm) result.i.c.b.get(0)).l.size() == 1,
                "legacy supplemental groups and child entries filtered");
        check(result.i.d.b.size() == 1 && ((cfgz) result.i.d.b.get(0)).b.l.equals("ランダ 福岡店"),
                "pinned supplementary route filtered");
        check(result.i.g.b.size() == 1 && ((cfmi) result.i.g.b.get(0)).b.l.equals("RANDA 札幌店"),
                "promoted supplementary route filtered");
        check(result.i.unrelatedMetadata == source.i.unrelatedMetadata, "supplement metadata retained");
        check(snapshot.equals(snapshot(source)), "supplement original graph untouched");
        assertImmutable(result.i.c.b);
        assertImmutable(((cfhm) result.i.c.b.get(0)).l);
    }

    private static void unknownNamesArePreserved() throws Exception {
        cqgm source = response(place(""), place(null), place("   "), place("DIANA"));
        cfhm unknown = new cfhm();
        cffs missing = new cffs();
        unknown.l = list(missing, groupItem(place("DIANA")));
        cfhm emptyGroup = new cfhm();
        source.L = list(wrapper(8, alternate("")), wrapper(5, unknown), wrapper(5, emptyGroup),
                wrapper(6, promoted(null)), wrapper(7, pinned(null)));
        sealed(source);
        cqgm result = (cqgm) apply(source, "ランダ");
        check(result.d.size() == 3, "unknown names retained, known mismatch removed");
        check(result.L.size() == 5, "unknown or absent supplemental names retained");
        check(((cfhm) ((chaq) result.L.get(1)).d).l.size() == 1,
                "unknown group child retained while mismatch removed");
    }

    private static void queryIsolation() throws Exception {
        final cqgm source = sealed(response(place("RANDA 有楽町"), place("DIANA 銀座")));
        final Object[] results = new Object[2];
        Thread first = new Thread(new Runnable() { public void run() {
            for (int i = 0; i < 25; i++) results[0] = apply(source, "ランダ");
        }});
        Thread second = new Thread(new Runnable() { public void run() {
            for (int i = 0; i < 25; i++) results[1] = apply(source, "DIANA");
        }});
        first.start(); second.start(); first.join(); second.join();
        cqgm randa = (cqgm) results[0];
        cqgm diana = (cqgm) results[1];
        check(randa.d.size() == 1 && ((cqdn) randa.d.get(0)).l.startsWith("RANDA"),
                "first response uses its own query");
        check(diana.d.size() == 1 && ((cqdn) diana.d.get(0)).l.startsWith("DIANA"),
                "concurrent response uses its own query");
        check(source.d.size() == 2 && !source.d.c(), "concurrent filtering preserves shared original");
    }

    private static void reusedModelAndSuccessfulMerge() throws Exception {
        cqgm oldSnapshot = sealed(response(place("DIANA old fail-open row")));
        avdv reused = new avdv();
        reused.setResponse(oldSnapshot);
        reused.setLayer(new awfr());
        Object oldLayer = reused.layer();
        cqgm source = sealed(response(place("RANDA new row"), place("DIANA new row")));
        String before = snapshot(source);
        cqgm filtered = (cqgm) SearchFilter.filterResponse(source, "ランダ", reused);
        check(filtered != source && filtered.d.size() == 1, "reused request response filtered");
        check(oldLayer != null && reused.layer() == null, "successful strict response clears stale model layer");
        check(reused.marker() == filtered, "marker holds the exact returned filtered response");
        check(reused.response() == oldSnapshot, "filter helper does not alter parser-owned S field");
        check(before.equals(snapshot(source)), "model commit does not mutate original response");
        check(reused.lockAcquisitions == 2 && reused.lockReleases == 2,
                "marker reset and success commit use the host write lock");

        avdv receiver = new avdv();
        receiver.setResponse(oldSnapshot);
        receiver.setLayer(new awfr());
        Object receiverLayer = receiver.layer();
        SearchFilter.reconcileModel(receiver, reused);
        check(receiver.layer() == receiverLayer && receiver.marker() == null,
                "marker cannot propagate until the host has parsed the exact response");

        // Simulate the host aj write of this returned object into incoming.S.
        reused.setResponse(filtered);
        SearchFilter.reconcileModel(receiver, reused);
        check(receiver.layer() == null, "accepted successful merge clears receiver's stale layer");
        check(receiver.marker() == receiver.response(), "merge propagates native-state marker against receiver S");
        check(receiver.lockAcquisitions == 0 && receiver.lockReleases == 0,
                "merge helper relies on native caller locks and never re-locks");
        check(receiver.response() == oldSnapshot && oldSnapshot.d.size() == 1,
                "merge marker is not a claim that historical fail-open rows were refiltered");
        check(reused.marker() == filtered && reused.response() == filtered,
                "merge helper does not change incoming marker or snapshot");

        avdv noSnapshot = new avdv();
        noSnapshot.setLayer(new awfr());
        SearchFilter.reconcileModel(noSnapshot, reused);
        check(noSnapshot.layer() == null && noSnapshot.marker() == null,
                "receiver without S clears its stale layer but cannot propagate a marker");
    }

    private static void staleMarkerAndCategoryMerge() {
        avdv incoming = new avdv();
        cqgm first = sealed(response(place("RANDA first")));
        cqgm filtered = (cqgm) SearchFilter.filterResponse(first, "RANDA", incoming);
        incoming.setResponse(filtered);
        check(incoming.marker() == filtered, "valid prior success marker exists");

        cqgm second = sealed(response(place("DIANA direct parser result")));
        incoming.setResponse(second); // Direct aj outside the patched f path.
        avdv receiver = new avdv();
        receiver.setResponse(first);
        receiver.setLayer(new awfr());
        receiver.setMarker(first);
        Object layer = receiver.layer();
        SearchFilter.reconcileModel(receiver, incoming);
        check(receiver.layer() == layer && receiver.marker() == null,
                "stale success identity cannot override a later direct parse");

        incoming.setResponse(null); // A cleared/restored model has no current snapshot identity.
        receiver.setMarker(first);
        SearchFilter.reconcileModel(receiver, incoming);
        check(receiver.layer() == layer && receiver.marker() == null,
                "cleared snapshot invalidates prior success identity");

        incoming.setResponse(filtered);
        incoming.setLayer(new awfr());
        Object categoryLayer = incoming.layer();
        cqgm category = sealed(response(place("RANDA"), place("DIANA")));
        Object returned = SearchFilter.filterResponse(category, "靴屋", incoming);
        check(returned == category && incoming.marker() == null,
                "category reuse resets prior success while retaining original response");
        check(incoming.layer() == categoryLayer, "category reuse does not clear native layer");
        incoming.setResponse(category);
        receiver.setMarker(first);
        SearchFilter.reconcileModel(receiver, incoming);
        check(receiver.layer() == layer && receiver.marker() == null,
                "ordinary category merge preserves native merge behavior");

        avdv restored = new avdv();
        restored.setResponse(first); // Transient patch marker defaults to null after restore.
        receiver.setMarker(first);
        SearchFilter.reconcileModel(receiver, restored);
        check(receiver.layer() == layer && receiver.marker() == null,
                "restored response alone cannot fabricate successful-filter state");
    }

    private static void categoryBypass() {
        cqgm source = response(place("RANDA"), place("DIANA"));
        source.o = layer();
        sealed(source);
        check(apply(source, "靴屋") == source, "shoe category bypasses all modifications");
        check(apply(source, "喫煙所") == source, "smoking-area category bypasses");
        check(apply(source, null) == source, "missing query bypasses");
        check(apply(source, "") == source, "empty query bypasses");
        check(Log.messages.isEmpty(), "successful and category paths have no diagnostics");
    }

    private static void originalPreservedOnFailure() throws Exception {
        cqgm source = response(place("RANDA"), place("DIANA"));
        source.o = layer();
        sealed(source);
        String snapshot = snapshot(source);
        cmzk.failBuild = true;
        Object result;
        try {
            result = apply(source, "SECRET_QUERY");
        } finally {
            cmzk.failBuild = false;
        }
        check(result == source, "failed build returns original response");
        check(snapshot.equals(snapshot(source)), "failed build cannot mutate source");
        check(!source.d.c() && source.o.c != null, "failed clone leaves input list and layer intact");

        cqgm malformed = response();
        malformed.L = list(wrapper(2, "wrong payload class"));
        sealed(malformed);
        check(apply(malformed, "RANDA") == malformed,
                "invalid known payload type returns original");
    }

    private static void modelFailureAndFailOpenMerge() throws Exception {
        avdv incoming = new avdv();
        cqgm before = sealed(response(place("RANDA previous success")));
        cqgm priorFiltered = (cqgm) SearchFilter.filterResponse(before, "RANDA", incoming);
        incoming.setResponse(priorFiltered);
        incoming.setLayer(new awfr());
        Object layer = incoming.layer();
        cqgm source = sealed(response(place("RANDA"), place("DIANA")));
        String snapshot = snapshot(source);
        cmzk.failBuild = true;
        Object result;
        try {
            result = SearchFilter.filterResponse(source, "RANDA", incoming);
        } finally {
            cmzk.failBuild = false;
        }
        check(result == source && incoming.marker() == null,
                "failed filter clears previous success marker and returns original");
        check(incoming.layer() == layer && snapshot.equals(snapshot(source)),
                "failed filtering preserves native layer and every original protobuf field");
        check(incoming.lockAcquisitions == incoming.lockReleases,
                "failed filtering releases every acquired model lock");

        incoming.setResponse(source); // Host parses the unmodified fail-open response.
        avdv receiver = new avdv();
        receiver.setResponse(before);
        receiver.setMarker(before);
        receiver.setLayer(new awfr());
        Object normalMergedLayer = receiver.layer();
        SearchFilter.reconcileModel(receiver, incoming);
        check(receiver.layer() == normalMergedLayer && receiver.marker() == null,
                "fail-open merge cannot propagate a previous successful marker");

        receiver.setMarker(before);
        SearchFilter.reconcileModel(receiver, new Object());
        check(receiver.layer() == normalMergedLayer && receiver.marker() == null,
                "merge schema exception invalidates only patch-owned state");
        check(SearchFilter.filterResponse(source, "RANDA", new Object()) == source,
                "model schema is validated before filtering");

        incoming.setMarker(priorFiltered);
        Object nullResult = SearchFilter.filterResponse(null, "RANDA", incoming);
        check(nullResult == null && incoming.marker() == null && incoming.layer() == layer,
                "null response invalidates stale marker without changing existing native data");
    }

    private static void mutableInputRejected() {
        cqgm source = response(place("RANDA"), place("DIANA"));
        check(source.isMutable() && source.d.c(), "mutable fixture really mutable");
        check(apply(source, "RANDA") == source,
                "unexpected mutable input is rejected without changing it");
        check(source.isMutable() && source.d.c() && source.d.size() == 2,
                "mutable source remains unchanged and mutable");
    }

    private static void unsupportedAndNull() {
        Object unsupported = new Object();
        check(apply(unsupported, "RANDA") == unsupported,
                "unknown schema keeps original");
        check(apply(null, "RANDA") == null, "null response stays null");
    }

    private static Object apply(Object response, String query) {
        return SearchFilter.filterResponse(response, query, new avdv());
    }

    private static cqdn place(String name) { cqdn p = new cqdn(); p.l = name; return p; }
    private static ccfl alternate(String name) { ccfl p = new ccfl(); p.d = name; return p; }
    private static chaq wrapper(int tag, Object payload) { chaq w = new chaq(); w.c = tag; w.d = payload; return w; }
    private static cfmi promoted(cqdn place) { cfmi p = new cfmi(); p.b = place; return p; }
    private static cfgz pinned(cqdn place) { cfgz p = new cfgz(); p.b = place; return p; }
    private static cffs groupItem(cqdn place) { cffs i = new cffs(); i.c = place; return i; }
    private static cfhm group(cqdn... places) {
        cfhm g = new cfhm();
        for (cqdn p : places) g.l.add(groupItem(p));
        return g;
    }
    private static cqgm response(cqdn... places) { cqgm r = new cqgm(); r.d = list((Object[]) places); return r; }
    private static cjme layer() {
        cjme m = new cjme(); m.b = 0x21; m.c = new cbkh(); m.otherMetadata = new Object(); return m;
    }
    private static cnak list(Object... entries) { return new PList(Arrays.asList(entries)); }

    private static <T extends cmzs> T sealed(T value) {
        freeze(value, new IdentityHashMap<Object, Boolean>());
        return value;
    }
    private static void freeze(Object value, IdentityHashMap<Object, Boolean> seen) {
        if (value == null || seen.put(value, Boolean.TRUE) != null) return;
        if (value instanceof cnak) {
            for (Object child : (cnak) value) freeze(child, seen);
            ((cnak) value).b();
        } else if (value instanceof cmzs) {
            try {
                for (Field field : value.getClass().getFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) freeze(field.get(value), seen);
                }
                ((cmzs) value).freezeOne();
            } catch (Exception e) { throw new AssertionError(e); }
        }
    }
    private static String snapshot(Object value) throws Exception {
        if (value == null) return "null";
        if (value instanceof List) {
            StringBuilder b = new StringBuilder("[");
            for (Object child : (List<?>) value) b.append(snapshot(child)).append(';');
            return b.append("]mutable=").append(((cnak) value).c()).toString();
        }
        if (value instanceof cmzs) {
            StringBuilder b = new StringBuilder(value.getClass().getName());
            b.append("(mutable=").append(((cmzs) value).isMutable());
            for (Field field : value.getClass().getFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    b.append(',').append(field.getName()).append('=').append(snapshot(field.get(value)));
                }
            }
            return b.append(')').toString();
        }
        return String.valueOf(value);
    }
    private static void assertImmutable(cnak list) {
        try { list.clear(); throw new AssertionError("list accepted mutation"); }
        catch (UnsupportedOperationException expected) { assertions++; }
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        assertions++;
    }
}

// Minimal structural fixtures mirror the verified obfuscated Maps schema.
// They do not reimplement filtering; their sole behavior is protobuf copy/freeze.
interface cnak extends List<Object>, RandomAccess {
    void b();
    boolean c();
    cnak e(int capacity);
}
final class PList extends ArrayList<Object> implements cnak {
    private boolean mutable = true;
    PList() {}
    PList(List<?> from) { super(from); }
    private void writable() { if (!mutable) throw new UnsupportedOperationException(); }
    public void b() { mutable = false; }
    public boolean c() { return mutable; }
    public cnak e(int capacity) {
        if (capacity < size()) throw new IllegalArgumentException();
        return new PList(this);
    }
    public boolean add(Object v) { writable(); return super.add(v); }
    public void add(int i, Object v) { writable(); super.add(i, v); }
    public boolean addAll(java.util.Collection<?> v) { writable(); return super.addAll(v); }
    public boolean addAll(int i, java.util.Collection<?> v) { writable(); return super.addAll(i, v); }
    public Object set(int i, Object v) { writable(); return super.set(i, v); }
    public Object remove(int i) { writable(); return super.remove(i); }
    public boolean remove(Object v) { writable(); return super.remove(v); }
    public void clear() { writable(); super.clear(); }
}
class cmzs implements Cloneable {
    private boolean mutable = true;
    public final cmzk toBuilder() { return new cmzk(copyMutable()); }
    final boolean isMutable() { return mutable; }
    final cmzs copyMutable() {
        try { cmzs copy = (cmzs) super.clone(); copy.mutable = true; return copy; }
        catch (CloneNotSupportedException e) { throw new AssertionError(e); }
    }
    final void freezeOne() {
        try {
            for (Field field : getClass().getFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    Object child = field.get(this);
                    if (child instanceof cnak) ((cnak) child).b();
                }
            }
            mutable = false;
        } catch (IllegalAccessException e) { throw new AssertionError(e); }
    }
}
class cmzk {
    public cmzs instance;
    static volatile boolean failBuild;
    cmzk(cmzs instance) { this.instance = instance; }
    public final void copyOnWrite() { if (!instance.isMutable()) instance = instance.copyMutable(); }
    public final cmzs build() {
        if (failBuild) throw new IllegalStateException("simulated build rejection SECRET_QUERY");
        instance.freezeOne();
        return instance;
    }
}
class cqgm extends cmzs {
    public cnak d = new PList();
    public cnak L = new PList();
    public cffu i;
    public cjme o;
    public String q;
    public String continuation;
    public Object viewport;
}
class cqdn extends cmzs { public String l = ""; }
class chaq extends cmzs { public int c; public Object d; public int visibilityFlags; }
class ccfl extends cmzs { public String d = ""; }
class cjme extends cmzs { public int b; public cbkh c; public Object otherMetadata; }
class cbkh extends cmzs {}
class cffu extends cmzs { public cfhn c; public cfha d; public cfmj g; public Object unrelatedMetadata; }
class cfha extends cmzs { public cnak b = new PList(); }
class cfgz extends cmzs { public cqdn b; }
class cfhn extends cmzs { public cnak b = new PList(); }
class cfhm extends cmzs { public cnak l = new PList(); }
class cffs extends cmzs { public cqdn c; }
class cfmj extends cmzs { public cnak b = new PList(); }
class cfmi extends cmzs { public cqdn b; }
class awfr {}
interface one extends AutoCloseable { void close(); }
class onf {
    private final java.util.concurrent.locks.ReentrantReadWriteLock lock =
            new java.util.concurrent.locks.ReentrantReadWriteLock();
    int lockAcquisitions;
    int lockReleases;
    public final one e() {
        lock.writeLock().lock();
        lockAcquisitions++;
        return new one() { public void close() {
            lockReleases++;
            lock.writeLock().unlock();
        }};
    }
}
class avdv extends onf {
    private awfr ag;
    private transient cqgm S;
    // The production DEX patch also marks this field synthetic (flags 4226).
    private transient Object hiroStrictSearchResponse;
    awfr layer() { return ag; }
    cqgm response() { return S; }
    Object marker() { return hiroStrictSearchResponse; }
    void setLayer(awfr layer) { ag = layer; }
    void setResponse(cqgm response) { S = response; }
    void setMarker(Object marker) { hiroStrictSearchResponse = marker; }
}
