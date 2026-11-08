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
        onlyMatchingKeepsNative();
        modernAndLegacyFallback();
        modernTypesAndUnknownMetadata();
        supplementaryPaths();
        unknownNamesArePreserved();
        queryIsolation();
        reusedModelAndSuccessfulMerge();
        staleMarkerAndCategoryMerge();
        nativeOnlyRowsAndSinglePin();
        nativeSelectedRoots();
        nativeIdRepresentations();
        emptyPagePreservesPreviousPins();
        categoryBypass();
        originalPreservedOnFailure();
        modelFailureAndFailOpenMerge();
        nativeIdFallbacks();
        mutableInputRejected();
        unsupportedAndNull();
        check(Log.messages.size() == 2, "list and native-pin diagnostics are separately bounded");
        for (String message : Log.messages) {
            check(!message.contains("SECRET_QUERY") && !message.contains("ランダ")
                    && !message.contains("DIANA"), "diagnostic contains no user data");
        }
        System.out.println("RuntimeFilterTest PASS: " + assertions + " assertions");
    }

    private static void mixedLegacyAndNative() throws Exception {
        cqgm source = response(place("RANDA 有楽町マルイ店", "0x11:0x101"),
                place("ダイアナ 銀座本店", "0x22:0x102"), place("ランダ銀座インズ店", "259"));
        cbki randaOne = pin(0x11, 0x101);
        cbki diana = pin(0x22, 0x102);
        cbki randaTwo = pin(0x33, 0x103);
        source.o = layer(randaOne, diana, randaTwo);
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
        check(result.o != source.o && result.o.c != null && result.o.b == 0x21,
                "native layer remains present with original mode flags");
        check(result.o.c.e.size() == 2 && result.o.c.e.get(0) == randaOne
                && result.o.c.e.get(1) == randaTwo, "only accepted native pin objects retained");
        check(result.o.otherMetadata == source.o.otherMetadata, "other layer metadata retained");
        check(result.q == source.q && result.continuation == source.continuation
                && result.viewport == source.viewport, "query pagination viewport preserved");
        check(!result.isMutable() && !result.d.c(), "returned message and list immutable");
        check(source.d == beforeList && source.o.c == beforeLayer, "original field identity preserved");
        check(snapshot.equals(snapshot(source)), "original object graph unchanged");
        assertImmutable(source.d);
        assertImmutable(result.d);
        assertImmutable(result.o.c.e);
    }

    private static void onlyMatchingKeepsNative() throws Exception {
        cqgm source = response(place("RANDA 東京店", "101"));
        source.o = layer(pin(17, 101));
        sealed(source);
        cqgm result = (cqgm) apply(source, "RANDA");
        check(result == source && result.o.c == source.o.c,
                "all-matching native response is preserved unchanged");
        check(result.d == source.d, "unchanged immutable list can be shared");
        check(source.o.c != null, "source layer remains present");
        cqgm noLayer = sealed(response(place("RANDA 新宿店")));
        check(apply(noLayer, "RANDA") == noLayer,
                "already matching model without native layer needs no clone");
    }

    private static void modernAndLegacyFallback() throws Exception {
        cqgm source = response(place("DIANA old-format fallback", "123"));
        source.L = list(wrapper(2, place("DIANA new-format", "123")));
        source.o = layer(pin(10, 123));
        sealed(source);
        cqgm result = (cqgm) apply(source, "ランダ");
        check(result.L.isEmpty(), "modern all-mismatch result is legitimately empty");
        check(result.d.isEmpty(), "legacy fallback also empty, no unrelated resurrection");
        check(result.o.c != null && result.o.c.e.isEmpty(), "empty result keeps an explicit empty native layer");
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
        check(receiver.layer() == receiverLayer,
                "accepted successful merge preserves the host's merged native layer");
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
        check(noSnapshot.layer() != null && noSnapshot.marker() == null,
                "receiver without S preserves native layer and cannot propagate a marker");
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

    private static void nativeOnlyRowsAndSinglePin() throws Exception {
        cqgm source = response();
        chaq one = wrapper(2, place("RANDA 有楽町", "0xabc:0x111"));
        chaq two = wrapper(8, alternate("ランダ銀座インズ店", "546"));
        chaq other = wrapper(2, place("DIANA 銀座", "0xdef:0x333"));
        one.b = 1; two.b = 1; other.b = 1;
        source.L = list(one, two, other);
        cbki first = pin(0xabc, 0x111);
        cbki second = pin(0x999, 0x222);
        cbki rejected = pin(0xdef, 0x333);
        Object coordinate = first.d;
        Object labelToken = first.labelToken;
        byte[] opaque = first.opaqueBytes.clone();
        source.o = layer(first, second, rejected);
        source.o.c.c = 0x8 | 0x10000;
        source.o.c.f = 7;
        Object style = source.o.c.styleToken;
        byte[] layerBytes = source.o.c.opaqueBytes.clone();
        sealed(source);
        String original = snapshot(source);
        cqgm filtered = (cqgm) apply(source, "ランダ");
        check(filtered.L.size() == 2, "native-only modern rows keep only matching names");
        check(((chaq) filtered.L.get(0)).b == 1 && ((chaq) filtered.L.get(1)).b == 1,
                "native-only wrapper flags remain intact");
        check(filtered.o.c != null && filtered.o.c.e.size() == 2,
                "multiple native-only rows retain matching native pins");
        check(filtered.o.c.e.get(0) == first && filtered.o.c.e.get(1) == second,
                "native pin order and whole objects are retained");
        check(first.d == coordinate && first.labelToken == labelToken
                && Arrays.equals(first.opaqueBytes, opaque), "pin coordinates label and opaque bytes unchanged");
        check(filtered.o.c.styleToken == style && Arrays.equals(filtered.o.c.opaqueBytes, layerBytes)
                && filtered.o.c.c == source.o.c.c && filtered.o.c.f == 7,
                "native mode style and unrelated wire metadata unchanged");
        check(original.equals(snapshot(source)), "native-only original graph unchanged");

        cqgm single = response(place("SNIDEL 有楽町店", "700"), place("DIANA", "800"));
        cbki snidel = pin(17, 700);
        single.o = layer(snidel, pin(18, 800));
        sealed(single);
        cqgm onePin = (cqgm) apply(single, "SNIDEL");
        check(onePin.d.size() == 1 && onePin.o.c.e.size() == 1,
                "single accepted store retains one native pin");
        check(onePin.o.c.e.get(0) == snidel && (onePin.o.b & 1) != 0,
                "single pin uses present native layer instead of zero-pin fallback");
    }

    private static void nativeSelectedRoots() throws Exception {
        cqgm source = response(place("RANDA", "901"), place("DIANA", "902"));
        cbki kept = pin(1, 901);
        cbki removed = pin(2, 902);
        source.o = layer(kept, removed);
        source.o.c.c = 0x2 | 0x20 | 0x1000 | 0x8 | 0x10000;
        source.o.c.d = new cbkj(); source.o.c.d.c = removed;
        cbke acceptedSpecial = new cbke(kept);
        source.o.c.special(acceptedSpecial);
        cbkg rejectedGeometry = new cbkg(removed);
        source.o.c.geometry(rejectedGeometry);
        sealed(source);
        String before = snapshot(source);
        cqgm filtered = (cqgm) apply(source, "RANDA");
        check(filtered.o.c.e.size() == 1 && filtered.o.c.e.get(0) == kept,
                "ordinary native results filtered alongside selected points");
        check(filtered.o.c.d == null && (filtered.o.c.c & 0x2) == 0,
                "nonmatching selected root d and presence bit removed");
        check(filtered.o.c.special() == acceptedSpecial && (filtered.o.c.c & 0x20) != 0,
                "matching selected root i retained by identity");
        check(filtered.o.c.geometry() == null && (filtered.o.c.c & 0x1000) == 0,
                "nonmatching selected root j and presence bit removed");
        check((filtered.o.c.c & (0x8 | 0x10000)) == (0x8 | 0x10000),
                "other native mode presence bits retained");
        check(before.equals(snapshot(source)) && source.o.c.geometry() == rejectedGeometry,
                "selected original metadata graph untouched");

        cqgm empty = response(place("DIANA", "902"));
        empty.o = layer(removed);
        empty.o.c.c = 0x2 | 0x20 | 0x1000;
        empty.o.c.d = new cbkj(); empty.o.c.d.c = removed;
        empty.o.c.special(new cbke(removed));
        empty.o.c.geometry(new cbkg(removed));
        sealed(empty);
        avdv model = new avdv(); model.setLayer(new awfr());
        cqgm none = (cqgm) SearchFilter.filterResponse(empty, "RANDA", model);
        check(none.d.isEmpty() && none.o.c.e.isEmpty(), "confirmed zero matches leave empty native result list");
        check(none.o.c.d == null && none.o.c.special() == null && none.o.c.geometry() == null
                && none.o.c.c == 0, "confirmed zero matches also remove selected feature roots");
        check(model.layer() == null && model.marker() == none,
                "confirmed zero still clears stale pre-parser model layer");
    }

    private static void nativeIdRepresentations() throws Exception {
        String[] ids = {
            "0x123456789abcdef0:0xffffffffffffffff", "18446744073709551615", "-1",
            "9223372036854775808", "-9223372036854775808", "0xffffffffffffffff:0x7fffffffffffffff",
            "101", "0x0:0x65"
        };
        long[] bits = {-1L, -1L, -1L, Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, 101L, 101L};
        for (int i = 0; i < ids.length; i++) {
            cqgm source = response(place("RANDA", ids[i]), place("DIANA", "202"));
            cbki matched = pin(0x777, bits[i]);
            source.o = layer(matched, pin(0x888, 202));
            sealed(source);
            cqgm filtered = (cqgm) apply(source, "RANDA");
            check(filtered.o.c.e.size() == 1 && filtered.o.c.e.get(0) == matched,
                    "unsigned/hex/decimal CID bit pattern matches: case " + i);
        }

        cqgm conflict = response(place("RANDA official", "500"), place("DIANA alternate label", "500"));
        cbki shared = pin(10, 500);
        conflict.o = layer(shared);
        sealed(conflict);
        cqgm filtered = (cqgm) apply(conflict, "RANDA");
        check(filtered.d.size() == 1 && filtered.o.c.e.get(0) == shared,
                "accepted title takes precedence for a shared feature CID");
    }

    private static void emptyPagePreservesPreviousPins() {
        cqgm page = response(place("DIANA next page", "1002"));
        page.o = layer(pin(20, 1002));
        sealed(page);
        avdv incoming = new avdv();
        cqgm filtered = (cqgm) SearchFilter.filterResponse(page, "RANDA", incoming);
        incoming.setResponse(filtered);
        check(filtered.o.c.e.isEmpty(), "zero-match next page has an empty filtered native list");

        cqgm firstPage = response(place("RANDA earlier page", "1001"));
        cbki firstPin = pin(19, 1001);
        firstPage.o = layer(firstPin);
        sealed(firstPage);
        avdv receiver = new avdv(); receiver.setResponse(firstPage);
        awfr nativeMerged = new awfr(); nativeMerged.value = firstPage.o.c;
        receiver.setLayer(nativeMerged);
        SearchFilter.reconcileModel(receiver, incoming);
        check(receiver.layer() == nativeMerged && nativeMerged.value.e.size() == 1
                && nativeMerged.value.e.get(0) == firstPin,
                "zero-match pagination merge cannot erase an earlier accepted native pin");
        check(receiver.marker() == firstPage, "pagination merge updates only the control marker");
    }

    private static void nativeIdFallbacks() throws Exception {
        String[] unsupportedIds = {"ChIJ-not-a-cdocid", "0x1:0xgarbage", "18446744073709551616",
                "-9223372036854775809", "0x10000000000000000:0x1", "", null};
        for (int i = 0; i < unsupportedIds.length; i++) {
            cqgm source = response(place("RANDA", unsupportedIds[i]), place("DIANA", "42"));
            cbki unknownAccepted = pin(7, 77);
            source.o = layer(unknownAccepted, pin(8, 42));
            sealed(source);
            String before = snapshot(source);
            cqgm filtered = (cqgm) apply(source, "RANDA");
            check(filtered != source && filtered.d.size() == 1, "unparsed ID does not cancel list filtering: " + i);
            check(filtered.o.c.e.size() == 1 && filtered.o.c.e.get(0) == unknownAccepted,
                    "incomplete ID coverage preserves unknown pins and removes known rejected pins: " + i);
            check(before.equals(snapshot(source)), "unparsed ID fallback leaves original untouched: " + i);
        }

        cqgm disjoint = response(place("RANDA", "101"), place("DIANA", "102"));
        disjoint.o = layer(pin(9, 999));
        sealed(disjoint);
        cqgm retained = (cqgm) apply(disjoint, "RANDA");
        check(retained.d.size() == 1 && retained.o == disjoint.o,
                "nonempty accepted list with zero matched native IDs preserves original native metadata");
        check(retained.o.c.e.size() == 1,
                "ID disagreement cannot silently remove every native pin");

        cqgm absentNativeId = response(place("RANDA", "101"), place("DIANA", "102"));
        cbki unknown = pin(9, 0); unknown.c = null;
        absentNativeId.o = layer(unknown, pin(10, 102));
        sealed(absentNativeId);
        cqgm unknownKept = (cqgm) apply(absentNativeId, "RANDA");
        check(unknownKept.o.c.e.size() == 1 && unknownKept.o.c.e.get(0) == unknown,
                "native pin with absent feature ID is conservatively retained");

        cqgm malformed = response(place("RANDA", "101"), place("DIANA", "102"));
        malformed.o = layer(); malformed.o.c.e = list("unrecognized native point");
        sealed(malformed);
        cqgm malformedKept = (cqgm) apply(malformed, "RANDA");
        check(malformedKept.d.size() == 1 && malformedKept.o == malformed.o,
                "unsupported native point type preserves only native metadata, list filter remains effective");
        boolean sawPinDiagnostic = false;
        for (String message : Log.messages) {
            if (message.contains("lists filtered; native pins partly preserved")) sawPinDiagnostic = true;
        }
        check(sawPinDiagnostic, "partial native-pin filtering has a separate query-free diagnostic");
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
    private static cqdn place(String name, String id) {
        cqdn p = place(name); p.h = new ckiu(); p.h.c = identity(id); return p;
    }
    private static ccff identity(String id) { ccff value = new ccff(); value.c = id; return value; }
    private static ccfl alternate(String name) { ccfl p = new ccfl(); p.d = name; return p; }
    private static ccfl alternate(String name, String id) {
        ccfl p = alternate(name); p.c = identity(id); return p;
    }
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
    private static cjme layer(cbki... pins) {
        cjme m = new cjme(); m.b = 0x21; m.c = new cbkh();
        m.c.e = list((Object[]) pins); m.otherMetadata = new Object(); return m;
    }
    private static cbki pin(long high, long low) {
        cbki pin = new cbki();
        pin.c = new ccpa(); pin.c.c = high; pin.c.d = low;
        pin.d = new ccpb(); pin.d.c = 356700000; pin.d.d = 1397640000;
        pin.e = "gcid:shoe_store";
        pin.opaqueBytes = new byte[] {1, 3, 7, (byte) low};
        pin.labelToken = new Object();
        return pin;
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
                for (Field field : value.getClass().getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        freeze(field.get(value), seen);
                    }
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
            for (Field field : value.getClass().getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
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
class cqdn extends cmzs { public String l = ""; public ckiu h; }
class chaq extends cmzs { public int b; public int c; public Object d; public int visibilityFlags; }
class ccfl extends cmzs { public ccff c; public String d = ""; }
class cjme extends cmzs { public int b; public cbkh c; public Object otherMetadata; }
class cbkh extends cmzs {
    public int c;
    public cbkj d;
    public cnak e = new PList();
    public int f;
    private cbke i;
    private cbkg j;
    public byte[] opaqueBytes = new byte[] {2, 4, 8};
    public Object styleToken = new Object();
    void special(cbke value) { i = value; }
    cbke special() { return i; }
    void geometry(cbkg value) { j = value; }
    cbkg geometry() { return j; }
}
class cbki extends cmzs {
    public ccpa c;
    public ccpb d;
    public String e;
    public cnak f = new PList();
    public byte[] opaqueBytes;
    public Object labelToken;
}
class ccpa extends cmzs { public long c; public long d; }
class ccpb extends cmzs { public int c; public int d; }
class ckiu extends cmzs { public ccff c; }
class ccff extends cmzs { public String c; }
class cbkj extends cmzs { public cbki c; }
class cbke extends cmzs {
    private cbki c;
    cbke(cbki value) { c = value; }
    cbki point() { return c; }
}
class cbkg extends cmzs {
    private cbki c;
    cbkg(cbki value) { c = value; }
    cbki point() { return c; }
}
class cffu extends cmzs { public cfhn c; public cfha d; public cfmj g; public Object unrelatedMetadata; }
class cfha extends cmzs { public cnak b = new PList(); }
class cfgz extends cmzs { public cqdn b; }
class cfhn extends cmzs { public cnak b = new PList(); }
class cfhm extends cmzs { public cnak l = new PList(); }
class cffs extends cmzs { public cqdn c; }
class cfmj extends cmzs { public cnak b = new PList(); }
class cfmi extends cmzs { public cqdn b; }
class awfr { cbkh value; }
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
