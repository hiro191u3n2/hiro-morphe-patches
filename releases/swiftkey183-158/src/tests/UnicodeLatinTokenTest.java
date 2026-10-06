import hiro.swiftkey.UnicodeLatinToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Independent classifier regression cases, without Android or app test stubs.
 *
 * Usage: UnicodeLatinTokenTest [path/to/patched/CursorLatinBoundary.smali]
 * The optional argument enables separate structural guard-contract checks.
 * Neither category claims execution of SwiftKey's native tokenizer or an editor.
 */
public final class UnicodeLatinTokenTest {
    private static int matcherChecks;
    private static int historicalChecks;
    private static int structuralChecks;
    private static final List<String> failures = new ArrayList<String>();

    private static void expect(String label, String value, boolean expected) {
        matcherChecks++;
        try {
            boolean actual = UnicodeLatinToken.isLatinToken(value);
            if (actual != expected) {
                failures.add(label + ": expected=" + expected + " actual=" + actual
                        + " input=" + codePoints(value));
            }
        } catch (Throwable failure) {
            failures.add(label + ": threw " + failure.getClass().getSimpleName()
                    + " input=" + codePoints(value));
        }
    }

    private static void historical(String label, String value, boolean expected) {
        historicalChecks++;
        boolean actual = oldAsciiMatcher(value);
        if (actual != expected) {
            failures.add("historical " + label + ": expected=" + expected
                    + " actual=" + actual + " input=" + codePoints(value));
        }
    }

    private static boolean oldAsciiMatcher(String value) {
        if (value == null || value.isEmpty()) return false;
        boolean letter = false;
        for (int index = 0; index < value.length(); index++) {
            char valueAt = value.charAt(index);
            if (valueAt < 0x21 || valueAt > 0x7e) return false;
            if ((valueAt >= 'A' && valueAt <= 'Z')
                    || (valueAt >= 'a' && valueAt <= 'z')) letter = true;
        }
        return letter;
    }

    private static String codePoints(String value) {
        if (value == null) return "null";
        if (value.isEmpty()) return "<empty>";
        StringBuilder output = new StringBuilder();
        for (int offset = 0; offset < value.length();) {
            int point = value.codePointAt(offset);
            if (output.length() > 0) output.append(' ');
            output.append(String.format("U+%04X", point));
            offset += Character.charCount(point);
        }
        return output.toString();
    }

    private static void allLatinPrefixes(String label, String value) {
        for (int offset = 0; offset <= value.length(); offset++) {
            // An index inside a surrogate pair is deliberately invalid text.
            boolean splitPair = offset > 0 && offset < value.length()
                    && Character.isHighSurrogate(value.charAt(offset - 1))
                    && Character.isLowSurrogate(value.charAt(offset));
            expect(label + " caret=" + offset, value.substring(0, offset),
                    offset > 0 && !splitPair);
        }
    }

    private static void reproduceReportedPhrase() {
        historical("old plain UNITE", "UNITE", true);
        historical("old plain API", "API", true);
        historical("old accented word escaped protection", "Pok\u00e9mon", false);
        historical("old decomposed word escaped protection", "Poke\u0301mon", false);
        historical("old fullwidth word escaped protection", "\uff21\uff30\uff29", false);
        historical("old mathematical word escaped protection", "\ud835\udc0f\ud835\udc28", false);

        String phrase = "UNITE | Pok\u00e9mon UNITE API";
        // This enumerates the word prefix immediately before every caret index.
        // It verifies classification inputs, not native-tokenizer behavior.
        for (int caret = 0; caret <= phrase.length(); caret++) {
            int start = caret;
            while (start > 0 && phrase.charAt(start - 1) != ' ') start--;
            String prefix = phrase.substring(start, caret);
            expect("reported phrase caret=" + caret, prefix,
                    !prefix.isEmpty() && !prefix.equals("|"));
        }
        expect("whole phrase is not one token", phrase, false);
        allLatinPrefixes("composed Pokemon", "Pok\u00e9mon");
        allLatinPrefixes("decomposed Pokemon", "Poke\u0301mon");
        allLatinPrefixes("fullwidth Pokemon", "\uff30\uff4f\uff4b\uff45\uff4d\uff4f\uff4e");
        allLatinPrefixes("mathematical Pokemon",
                "\ud835\udc0f\ud835\udc28\ud835\udc24\ud835\udc1e"
                        + "\ud835\udc26\ud835\udc28\ud835\udc27");
    }

    private static void preserveAsciiContract() {
        for (int point = 0; point < 128; point++) {
            String value = String.valueOf((char) point);
            expect("ASCII singleton " + point, value, oldAsciiMatcher(value));
            expect("ASCII inside word " + point, "A" + value + "z",
                    oldAsciiMatcher("A" + value + "z"));
            expect("ASCII after punctuation " + point, "!" + value + "42",
                    oldAsciiMatcher("!" + value + "42"));
        }
        StringBuilder punctuation = new StringBuilder();
        for (int point = 0x21; point <= 0x7e; point++) {
            if (!((point >= 'A' && point <= 'Z') || (point >= 'a' && point <= 'z')))
                punctuation.append((char) point);
        }
        expect("all printable ASCII nonletters", punctuation.toString(), false);
        expect("all printable ASCII nonletters plus letter", punctuation + "A", true);
    }

    private static void acceptedCases() {
        String[] values = {
                "Google", "Microsoft", "SwiftKey", "API_v2", "S26", "123abc456",
                "don't", "foo-bar", "C++", "C#", "https://example.com/a?b=c&d=2",
                "test@example.jp", "Pok\u00e9mon123", "Pok\u00e9mon-UNITE",
                "caf\u00e9", "na\u00efve", "Stra\u00dfe", "\u00c6sir", "\u0152uvre",
                "\u0141\u00f3d\u017a", "\u0130stanbul", "\u00e5ngstr\u00f6m", "\u00de\u00f3r",
                "e\u0301", "a\u0308", "n\u0303", "e\u0301\u0327", "A\u030a\u0301",
                "a\u1ab0", "a\u1dc0", "a\ufe20", "a\u0301\u1ab0\u1dc0\ufe20",
                // The Unicode Script property of U+0345 is INHERITED despite
                // its Greek name; attached inherited marks are within scope.
                "A\u0345",
                "\uff21\uff30\uff29\uff11\uff12\uff13", "\uff13\uff1a\uff21\uff30\uff29",
                "\uff54\uff45\uff53\uff54\uff20\uff45\uff58\uff41\uff4d\uff50\uff4c\uff45\uff0e\uff4a\uff50",
                "\ufb01le", "\ufb00", "\u212aelvin", "\u212bngstr\u00f6m",
                "\ud835\udc00\ud835\udc01\ud835\udc02", "A\ud835\udfd9",
                "l\u2019esprit", "\u2018quoted\u2019", "rock\u02bcn\u02bcroll",
                "re\u2010entry", "non\u2011breaking", "x\u2013y", "x\u2014y",
                // These are classified after NFKC, even when the source glyph is
                // a compatibility symbol or a Roman numeral instead of a letter.
                "\u2122", "\u2120", "\u213b", "\u216b"
        };
        for (int index = 0; index < values.length; index++)
            expect("accepted case " + index, values[index], true);
    }

    private static void rejectedCases() {
        String[] values = {
                null, "", "123", "\uff11\uff12\uff13", "\u2460\u2461\u2462",
                "\ud835\udfd9\ud835\udfda\ud835\udfdb", "!?", "|", "---", "@/#",
                "\u2018\u2019\u02bc\u2010\u2011\u2013\u2014",
                "hello world", "Pok\u00e9mon UNITE", " API", "API ", "A\tB", "A\nB",
                "A\rB", "A\u00a0B", "A\u2000B", "A\u202fB", "A\u3000B",
                "\u3042", "\u65e5\u672c", "\u30dd\u30b1\u30e2\u30f3",
                "Google\u3042", "UNITE\u65e5\u672c", "\u30dd\u30b1\u30e2\u30f3API",
                "API\u30c6\u30b9\u30c8", "\u3042API", "\uff21\u3042", "A\u3099", "A\uff9e",
                "P\u043ek\u00e9mon", "\u03a1ok\u00e9mon", "Pok\u00e9m\u03bfn",
                "A\u03b1", "A\u0430", "A\u05d0", "A\u0627", "A\u0915", "A\uac00",
                "\u0301", "\u0301API", "1\u0301A", "A1\u0301", "A-\u0301B",
                "A\u034f", "A\u20dd", "A\u20e3",
                "A\u0000B", "A\u0001B", "A\u001bB", "A\u007fB", "A\u0085B",
                "A\u00adB", "A\u200bB", "A\u200cB", "A\u200dB", "A\u2060B", "A\ufeffB",
                "A\u202dB", "A\u202eB", "A\u2066B", "A\u2069B",
                "A\ufe0e", "A\ufe0f", "A\udb40\udd00", "A\udb40\udc61",
                "\ud83d\ude00", "A\ud83d\ude00", "A\ud83c\uddef\ud83c\uddf5",
                "A\ud83c\udffb", "A\u2764\ufe0f", "A\ud83d\udc69\u200d\ud83d\udcbb",
                "\ud800", "\udc00", "A\ud800", "A\udc00", "\ud800A", "\udc00A"
        };
        for (int index = 0; index < values.length; index++)
            expect("rejected case " + index, values[index], false);
    }

    private static String method(String source, String signature) {
        String needle = " " + signature + "\n";
        int signatureAt = source.indexOf(needle);
        if (signatureAt < 0) throw new AssertionError("Missing method: " + signature);
        int start = source.lastIndexOf(".method", signatureAt);
        int end = source.indexOf(".end method", signatureAt);
        if (start < 0 || end < 0) throw new AssertionError("Malformed method: " + signature);
        return source.substring(start, end);
    }

    private static void structural(String label, boolean valid) {
        structuralChecks++;
        if (!valid) failures.add("structural: " + label);
    }

    private static void guardContracts(Path cursorSmali) throws Exception {
        String source = new String(Files.readAllBytes(cursorSmali), StandardCharsets.UTF_8);
        String after = method(source, "afterSelection(Lec0/x;Lec0/n;)V");
        String selection = method(source, "selection(Lec0/x;Lec0/n;IIIZ)V");
        String sync = method(source,
                "afterExternalSync(Lec0/x;Lec0/n;[Ljava/lang/Object;)V");
        structural("Unicode classifier is connected only at afterSelection",
                after.contains("Lhiro/swiftkey/UnicodeLatinToken;->isLatinToken(Ljava/lang/String;)Z")
                && source.indexOf("Lhiro/swiftkey/UnicodeLatinToken;")
                        == source.lastIndexOf("Lhiro/swiftkey/UnicodeLatinToken;"));
        structural("Japanese state is checked before classification",
                Pattern.compile("Lwb0/z0;->Y:Z\\s+if-eqz v0,").matcher(after).find()
                && after.indexOf("Lwb0/z0;->Y:Z") < after.indexOf("UnicodeLatinToken"));
        structural("active native context identity is checked",
                after.contains("Lec0/x;->a:Lec0/f;")
                && after.contains("if-ne v0, p1,") && after.contains("Lec0/x;->C()Z"));
        structural("intentional selection is excluded by the wrapper",
                selection.contains("if-ne p2, p3,"));
        structural("intentional selection is excluded again by the native context",
                after.contains("Lec0/f;->k()I") && after.contains("Lec0/f;->S()I")
                && after.contains("if-eq p0, v0,"));
        structural("wrapper preserves real movement guard and native q call",
                selection.contains("if-ne v2, p2,") && selection.contains("if-ne v1, p3,")
                && selection.contains("Lec0/n;->q(IIIZ)V"));
        structural("external no-change synchronization remains guarded",
                sync.contains("Lec0/f;->k()I") && sync.contains("Lec0/f;->S()I")
                && sync.contains("Ljava/util/Objects;->equals") && sync.contains("return-void"));
        structural("native span is checked against actual document characters",
                after.contains("Lr40/d;->k()Lr40/c;") && after.contains("Lr40/c;->c()I")
                && after.contains("Ljava/lang/String;->substring(II)")
                && after.contains("Ljava/lang/String;->equals(Ljava/lang/Object;)Z"));
        structural("afterSelection never writes document or cursor coordinates",
                !Pattern.compile("(?m)^\\s*iput(?:-\\w+)?\\s+[^\\n]*Lec0/f;->[abcd]:")
                        .matcher(after).find());
        structural("afterSelection has no editor replacement or cursor-reset operation",
                !after.contains("->commitText(") && !after.contains("->deleteSurroundingText(")
                && !after.contains("->setSelection(") && !after.contains("->replace("));
    }

    public static void main(String[] args) throws Exception {
        reproduceReportedPhrase();
        preserveAsciiContract();
        acceptedCases();
        rejectedCases();
        if (args.length > 1) throw new IllegalArgumentException("Expected at most one smali path");
        if (args.length == 1) guardContracts(Paths.get(args[0]));
        if (!failures.isEmpty()) {
            for (String failure : failures) System.err.println("FAIL " + failure);
            throw new AssertionError(failures.size() + " independent regression check(s) failed");
        }
        System.out.println("PASS UnicodeLatinToken: " + matcherChecks + " classifier cases; "
                + historicalChecks + " historical ASCII-leak checks; " + structuralChecks
                + " optional smali guard-contract checks.");
        System.out.println("Native tokenizer, live Android selection and language switching were not executed.");
    }
}
