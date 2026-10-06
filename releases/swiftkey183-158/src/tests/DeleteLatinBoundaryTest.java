import hiro.swiftkey.CursorLatinBoundary;
import hiro.swiftkey.DeleteLatinBoundary;
import java.util.ArrayList;
import java.util.List;

/** Tests the production gate with native-descriptor stubs, not an Android editor. */
public final class DeleteLatinBoundaryTest {
    private static int checks;
    private static int scenarios;
    private static final List<String> failures = new ArrayList<String>();

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) failures.add(description);
    }

    private static r40.c span(int markerState) {
        r40.c result = new r40.c();
        if (markerState >= 0) {
            ec0.y marker = new ec0.y();
            marker.k = markerState;
            result.b = marker;
        }
        return result;
    }

    private static ec0.x fixture(String text, int cursor, String composition, int markerState) {
        ec0.x owner = new ec0.x();
        owner.a = new ec0.f();
        nativeResult(owner, text, cursor, composition, markerState);
        return owner;
    }

    private static void nativeResult(ec0.x owner, String text, int cursor,
            String composition, int markerState) {
        owner.a.a = text;
        owner.a.b = cursor;
        owner.a.c = cursor;
        owner.a.i.c = composition;
        owner.a.i.d = span(markerState);
    }

    private static Object[] capture(ec0.x owner, String label) {
        String oldText = owner.a.a;
        Object oldComposition = owner.a.i.c;
        Object oldSpan = owner.a.i.d;
        int oldStart = owner.a.c;
        int oldEnd = owner.a.b;
        int oldOffset = owner.a.d;
        Object[] result = DeleteLatinBoundary.beforeDelete(owner);
        check(result != null, label + " snapshot should be available");
        check(owner.a.a == oldText && owner.a.i.c == oldComposition && owner.a.i.d == oldSpan
                && owner.a.c == oldStart && owner.a.b == oldEnd && owner.a.d == oldOffset,
                label + " beforeDelete must be read-only");
        return result;
    }

    private static void verifyAfter(ec0.x owner, Object[] state, boolean nativeSuccess,
            int expectedCalls, String label) {
        scenarios++;
        String text = owner.a.a;
        Object composition = owner.a.i.c;
        Object currentSpan = owner.a.i.d;
        int cursor = owner.a.c;
        int end = owner.a.b;
        int offset = owner.a.d;
        CursorLatinBoundary.reset();
        DeleteLatinBoundary.afterDelete(owner, state, nativeSuccess);
        check(CursorLatinBoundary.calls == expectedCalls,
                label + " expected delegate calls=" + expectedCalls
                        + " actual=" + CursorLatinBoundary.calls);
        check(owner.a.a == text && owner.a.i.c == composition && owner.a.i.d == currentSpan
                && owner.a.c == cursor && owner.a.b == end && owner.a.d == offset,
                label + " gate must preserve native text/selection/metadata");
        if (expectedCalls == 1) {
            check(CursorLatinBoundary.lastOwner == owner && CursorLatinBoundary.lastContext == owner.a,
                    label + " must delegate to the same native context");
        }
    }

    private static void scenario(String label, String oldText, int oldCursor, String oldCompose,
            int oldMarker, String newText, int newCursor, String newCompose, int expectedCalls) {
        ec0.x owner = fixture(oldText, oldCursor, oldCompose, oldMarker);
        Object[] state = capture(owner, label);
        // The marker may change to EDITING_BEFORE_COMMIT inside the original edit.
        // Eligibility must come from the pre-edit snapshot, not this new marker.
        nativeResult(owner, newText, newCursor, newCompose, 2);
        verifyAfter(owner, state, true, expectedCalls, label);
    }

    private static void existingAndFreshCases() {
        scenario("pasted ASCII backspace", "API", 3, "", -1, "AP", 2, "AP", 1);
        scenario("pasted accented backspace", "Pok\u00e9mon", 7, "", -1,
                "Pok\u00e9mo", 6, "Pok\u00e9mo", 1);
        scenario("pasted decomposed grapheme delete", "Poke\u0301", 5, "", -1,
                "Pok", 3, "Pok", 1);
        scenario("pasted fullwidth delete", "\uff21\uff30\uff29", 3, "", -1,
                "\uff21\uff30", 2, "\uff21\uff30", 1);
        scenario("pasted mathematical codepoint delete", "\ud835\udc00\ud835\udc01", 4,
                "", -1, "\ud835\udc00", 2, "\ud835\udc00", 1);
        scenario("existing Latin has no marker", "Pok\u00e9mon", 7, "Pok\u00e9mon", -1,
                "Pok\u00e9mo", 6, "Pok\u00e9mo", 1);
        scenario("existing Latin has ORIGINAL_TEXT marker", "Pok\u00e9mon", 7, "Pok\u00e9mon", 1,
                "Pok\u00e9mo", 6, "Pok\u00e9mo", 1);
        scenario("new Japanese romaji survives partial delete", "APIni", 5, "ni", 2,
                "APIn", 4, "n", 0);
        scenario("fresh romaji partial delete with right suffix", "APIniZ", 5, "ni", 2,
                "APInZ", 4, "n", 0);
        scenario("longer new Japanese romaji survives", "APInihon", 8, "nihon", 2,
                "APIniho", 7, "niho", 0);
        scenario("fresh Latin word stays composing", "Pok\u00e9mon", 7, "Pok\u00e9mon", 2,
                "Pok\u00e9mo", 6, "Pok\u00e9mo", 0);
        scenario("fresh kana partial deletion", "API\u304b\u306a", 5, "\u304b\u306a", 2,
                "API\u304b", 4, "\u304b", 0);
        scenario("all fresh romaji removed reveals existing word", "APIn", 4, "n", 2,
                "API", 3, "API", 1);
        scenario("all fresh romaji removed while preserving right suffix", "APInZ", 4, "n", 2,
                "APIZ", 3, "API", 1);
        scenario("all fresh kana removed reveals existing word", "API\u3042", 4, "\u3042", 2,
                "API", 3, "API", 1);
        scenario("all multichar fresh composition removed", "API\u304b\u306a", 5, "\u304b\u306a", 2,
                "API", 3, "API", 1);
        scenario("deletion crosses entire fresh composition into existing word", "APIni", 5, "ni", 2,
                "AP", 2, "AP", 1);
        scenario("all fresh input gone at start of field", "n", 1, "n", 2,
                "", 0, "", 0);
        scenario("previous Japanese text stays native", "\u65e5\u672c\u3042", 3, "\u3042", 2,
                "\u65e5\u672c", 2, "\u65e5\u672c", 0);
        scenario("Japanese pasted text remains native", "\u65e5\u672c\u8a9e", 3, "", -1,
                "\u65e5\u672c", 2, "\u65e5\u672c", 0);
        scenario("numeric-only remainder is excluded", "123A", 4, "", -1,
                "123", 3, "123", 0);
        scenario("mixed Japanese remainder is excluded", "API\u3042B", 5, "", -1,
                "API\u3042", 4, "API\u3042", 0);
        scenario("mixed Greek remainder is excluded", "API\u03b1B", 5, "", -1,
                "API\u03b1", 4, "API\u03b1", 0);
        scenario("inside-word backspace preserves right suffix", "Pok\u00e9mon", 5, "", -1,
                "Pok\u00e9on", 4, "Pok\u00e9", 1);
        for (int marker : new int[] {2, 3, 4, 5, 6, 7, 0, 100}) {
            scenario("unknown-or-editing marker conservative " + marker,
                    "ABC", 3, "ABC", marker, "AB", 2, "AB", 0);
        }
    }

    private static String simulatedComposition(String text) {
        if (text.isEmpty() || text.charAt(text.length() - 1) == ' ') return "";
        return text.substring(text.lastIndexOf(' ') + 1);
    }

    private static void reportedRepeatedDeletion() {
        String text = "Meta for Pok\u00e9mon UNITE | Pok\u00e9mon UNITE API";
        ec0.x owner = fixture(text, text.length(), "", -1);
        int deletions = 0;
        while (!text.isEmpty()) {
            String label = "reported phrase deletion " + deletions;
            Object[] state = capture(owner, label);
            text = text.substring(0, text.length() - 1);
            String composition = simulatedComposition(text);
            nativeResult(owner, text, text.length(), composition, -1);
            boolean LatinRemainder = !composition.isEmpty() && !composition.equals("|");
            verifyAfter(owner, state, true, LatinRemainder ? 1 : 0, label);
            // Model the existing Cursor helper's successful detachment for the
            // next edit. The real helper itself is not replaced by this test stub.
            if (LatinRemainder) owner.a.i.c = "";
            deletions++;
        }
        check(deletions == "Meta for Pok\u00e9mon UNITE | Pok\u00e9mon UNITE API".length(),
                "reported repeated deletion must remove precisely the simulated characters");
    }

    private static void pureDeletionContract() {
        String text = "ABPok\u00e9mon7Z";
        // Check the gate's interval arithmetic at every deletion interval and
        // every old cursor inside it. Installed hooks still determine UI coverage.
        for (int left = 0; left < text.length(); left++) {
            for (int right = left + 1; right <= text.length(); right++) {
                for (int oldCursor = left; oldCursor <= right; oldCursor++) {
                    String remainder = text.substring(0, left) + text.substring(right);
                    scenario("pure interval " + left + ":" + right + " oldCursor=" + oldCursor,
                            text, oldCursor, "", -1, remainder, left,
                            remainder.substring(0, left), left > 0 ? 1 : 0);
                }
            }
        }
        scenario("no-op", "ABCDE", 5, "", -1, "ABCDE", 5, "ABCDE", 0);
        scenario("insertion", "ABCDE", 5, "", -1, "ABCDEZ", 6, "ABCDEZ", 0);
        scenario("equal-length substitution", "ABCDE", 5, "", -1, "ABCDZ", 5, "ABCDZ", 0);
        scenario("shorter replacement is not pure backspace", "ABCDE", 5, "", -1,
                "ABCE", 4, "ABCE", 0);
        scenario("cursor moved farther than removed text", "ABCDE", 5, "", -1,
                "ABCD", 3, "ABC", 0);
        scenario("deletion with cursor moving right", "ABCDE", 3, "", -1,
                "ABCE", 4, "ABCE", 0);
        scenario("changed left prefix", "ABCDE", 3, "", -1, "ABDE", 3, "ABD", 0);
        scenario("changed right suffix", "ABCDE", 3, "", -1, "ABEC", 3, "ABE", 0);
        scenario("noncontiguous deletion", "ABCDEFG", 5, "", -1, "ABCEG", 3, "ABC", 0);
    }

    private static void guardCases() {
        ec0.x owner = fixture("API", 3, "", -1);
        Object[] state = capture(owner, "failed native operation");
        nativeResult(owner, "AP", 2, "AP", -1);
        verifyAfter(owner, state, false, 0, "failed native operation");

        owner = fixture("API", 3, "", -1);
        owner.e.Y = false;
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "English mode has no snapshot");
        owner.e.Y = true;
        state = capture(owner, "English mode after edit");
        nativeResult(owner, "AP", 2, "AP", -1);
        owner.e.Y = false;
        verifyAfter(owner, state, true, 0, "English mode after edit");

        owner = fixture("API", 3, "", -1);
        owner.active = false;
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "inactive context has no snapshot");
        owner.active = true;
        state = capture(owner, "inactive after edit");
        nativeResult(owner, "AP", 2, "AP", -1);
        owner.active = false;
        verifyAfter(owner, state, true, 0, "inactive after edit");

        owner = fixture("API", 2, "", -1);
        owner.a.b = 3;
        state = DeleteLatinBoundary.beforeDelete(owner);
        check(state == null, "intentional selected range has no snapshot");
        check(owner.a.c == 2 && owner.a.b == 3 && owner.a.a.equals("API"),
                "intentional selected range is left intact");
        nativeResult(owner, "AP", 2, "AP", -1);
        verifyAfter(owner, state, true, 0, "range deletion retains original native behavior");

        owner = fixture("API", 3, "", -1);
        state = capture(owner, "new selected range");
        nativeResult(owner, "AP", 1, "A", -1);
        owner.a.b = 2;
        verifyAfter(owner, state, true, 0, "new selected range is never collapsed by gate");

        owner = fixture("API", 3, "", -1);
        owner.a.d = 100;
        state = capture(owner, "stable extraction offset");
        nativeResult(owner, "AP", 2, "AP", -1);
        verifyAfter(owner, state, true, 1, "stable extraction offset");
        owner.a.d = 101;
        verifyAfter(owner, state, true, 0, "changed extraction offset");

        owner = fixture("API", 3, "", -1);
        state = capture(owner, "new native context");
        owner.a = fixture("AP", 2, "AP", -1).a;
        verifyAfter(owner, state, true, 0, "new native context rejects stale snapshot");

        ec0.x first = fixture("API", 3, "", -1);
        state = capture(first, "new owner with same context");
        ec0.x second = fixture("AP", 2, "AP", -1);
        second.a = first.a;
        nativeResult(second, "AP", 2, "AP", -1);
        verifyAfter(second, state, true, 0, "new owner with same context rejects snapshot");

        first = fixture("API", 3, "", -1);
        second = fixture("UNITE", 5, "", -1);
        Object[] firstState = capture(first, "independent first edit");
        Object[] secondState = capture(second, "independent second edit");
        nativeResult(first, "AP", 2, "AP", -1);
        nativeResult(second, "UNIT", 4, "UNIT", -1);
        verifyAfter(first, firstState, true, 1, "independent first edit");
        verifyAfter(second, secondState, true, 1, "independent second edit");

        check(DeleteLatinBoundary.beforeDelete(null) == null, "null owner");
        owner = new ec0.x();
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "missing native context");
        for (int badCursor : new int[] {-1, 4}) {
            owner = fixture("API", badCursor, "", -1);
            check(DeleteLatinBoundary.beforeDelete(owner) == null, "invalid old cursor " + badCursor);
        }
        owner = fixture("API", 3, "LONGER", -1);
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "composition exceeds left text");
        owner = fixture("API", 3, "BAD", -1);
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "composition differs from actual left text");
        owner = fixture("API", 3, null, -1);
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "missing composition");
        owner = fixture(null, 0, "", -1);
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "missing document");
        owner = fixture("API", 3, "", -1);
        owner.a.d = -1;
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "invalid extraction offset");
    }

    private static void failuresAndMalformedSnapshots() {
        ec0.x owner = fixture("API", 3, "", -1);
        owner.failActive = true;
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "active-state exception recovery");
        owner.failActive = false;
        owner.failActiveLinkage = true;
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "active-state linkage recovery");
        owner.failActiveLinkage = false;
        owner.a.failText = true;
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "before text exception recovery");
        owner.a.failText = false;
        owner.a.failTextLinkage = true;
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "before text linkage recovery");
        owner.a.failTextLinkage = false;
        owner.a.failComposition = true;
        check(DeleteLatinBoundary.beforeDelete(owner) == null, "before composition exception recovery");
        owner.a.failComposition = false;

        Object[] state = capture(owner, "post-read recovery");
        nativeResult(owner, "AP", 2, "AP", -1);
        owner.a.failText = true;
        verifyAfter(owner, state, true, 0, "post text exception recovery");
        owner.a.failText = false;
        owner.a.failTextLinkage = true;
        verifyAfter(owner, state, true, 0, "post text linkage recovery");
        owner.a.failTextLinkage = false;
        owner.a.failComposition = true;
        verifyAfter(owner, state, true, 0, "post composition exception recovery");
        owner.a.failComposition = false;

        verifyAfter(owner, null, true, 0, "null snapshot");
        verifyAfter(owner, new Object[0], true, 0, "empty snapshot");
        verifyAfter(owner, new Object[7], true, 0, "uninitialized snapshot");
        Object[] wrongType = state.clone();
        wrongType[3] = "not an integer";
        verifyAfter(owner, wrongType, true, 0, "wrong snapshot member type");
        Object[] invalidCursor = state.clone();
        invalidCursor[3] = Integer.valueOf(Integer.MAX_VALUE);
        verifyAfter(owner, invalidCursor, true, 0, "invalid snapshot cursor");
        Object[] invalidStart = state.clone();
        invalidStart[5] = Integer.valueOf(-1);
        verifyAfter(owner, invalidStart, true, 0, "invalid snapshot composition start");
        CursorLatinBoundary.reset();
        DeleteLatinBoundary.afterDelete(null, state, true);
        check(CursorLatinBoundary.calls == 0, "null post owner");

        CursorLatinBoundary.reset();
        CursorLatinBoundary.throwRuntime = true;
        DeleteLatinBoundary.afterDelete(owner, state, true);
        check(CursorLatinBoundary.calls == 1 && owner.a.a.equals("AP") && owner.a.c == 2,
                "downstream exception preserves the completed native deletion");
        CursorLatinBoundary.reset();
        CursorLatinBoundary.throwLinkage = true;
        DeleteLatinBoundary.afterDelete(owner, state, true);
        check(CursorLatinBoundary.calls == 1 && owner.a.a.equals("AP") && owner.a.c == 2,
                "downstream linkage failure preserves completed deletion");
        CursorLatinBoundary.reset();
    }

    public static void main(String[] args) {
        existingAndFreshCases();
        reportedRepeatedDeletion();
        pureDeletionContract();
        guardCases();
        failuresAndMalformedSnapshots();
        if (!failures.isEmpty()) {
            for (String failure : failures) System.err.println("FAIL " + failure);
            throw new AssertionError(failures.size() + " DeleteLatinBoundary checks failed");
        }
        System.out.println("PASS DeleteLatinBoundary: " + checks + " assertions across " + scenarios
                + " edit scenarios; existing-Latin/fresh-romaji/range/native-result/pure-deletion/ABI-failure guards.");
        System.out.println("The production gate ran against native-descriptor stubs; native tokenization and Android InputConnection were not executed.");
    }
}
