package hiro.swiftkey;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Exercises production enrichment with real dictionary rows and candidate objects. */
public final class WordPriorityTest {
    private static int checks;

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static WordCandidates.Index install(String text) throws Exception {
        WordCandidates.Index index = WordCandidates.Index.fromBytes(text.getBytes(StandardCharsets.UTF_8));
        Field field = WordCandidates.class.getDeclaredField("dictionary");
        field.setAccessible(true);
        field.set(null, index);
        return index;
    }

    private static l40.c request(String reading) {
        l40.c request = new l40.c();
        request.k = reading;
        request.d = com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;
        return request;
    }

    private static List<m40.b> candidates(l40.c request, String... texts) {
        List<m40.b> result = new ArrayList<m40.b>();
        for (String text : texts) result.add(new m40.h0(text, request.k, request));
        return result;
    }

    private static int position(List<m40.b> list, String text) {
        for (int i = 0; i < list.size(); i++)
            if (list.get(i) != null && text.equals(list.get(i).getUserFacingText())) return i;
        return -1;
    }

    private static void nativePrefix(List<m40.b> original, List<m40.b> enhanced) {
        for (int i = 0; i < original.size(); i++)
            check(original.get(i) == enhanced.get(i), "native candidate identity and priority " + i);
    }

    public static void main(String[] args) throws Exception {
        boolean fixed = args.length == 0 || "fixed".equals(args[0]);
        install("きょう\t今日\t京\t卿\t竟\t況\n");
        l40.c request = request("きょう");
        List<m40.b> original = candidates(request, "きょう", "キョウ", "今日", "教", "強");
        List<m40.b> before = new ArrayList<m40.b>(original);
        List<m40.b> enhanced = WordCandidates.enhance(original, request);
        if (!fixed) {
            check(position(enhanced, "今日") == 6, "reproduce everyday third choice buried after four dictionary entries");
            check(position(enhanced, "京") < position(enhanced, "今日"), "reproduce rare homonym displacing contextual word");
            check(original.equals(before), "baseline does not mutate native input");
            System.out.println("{\"status\":\"PASSED\",\"mode\":\"baseline\",\"old_priority_defect_reproduced\":true,\"checks\":" + checks + "}");
            return;
        }

        nativePrefix(original, enhanced);
        check(position(enhanced, "今日") == 2, "everyday native third choice stays third");
        check(position(enhanced, "京") == original.size(), "supplement follows all native choices");
        check(enhanced.size() == 9, "duplicate 今日 removed without dropping another dictionary word");
        check(original.equals(before), "native input collection unchanged");
        for (int i = original.size(); i < enhanced.size(); i++) {
            m40.h0 candidate = (m40.h0) enhanced.get(i);
            check(candidate.b.equals(request.k) && candidate.c == request && "".equals(candidate.d),
                    "supplement commits original reading with exact request and no space");
        }

        // A learned native choice is passed in ahead of the ordinary ranking.
        // Enrichment must neither second-guess that rank nor replace its native actor.
        List<m40.b> learned = candidates(request, "強", "今日", "きょう", "教", "キョウ");
        nativePrefix(learned, WordCandidates.enhance(learned, request));
        List<m40.b> learnedSupplement = candidates(request, "卿", "今日", "きょう");
        List<m40.b> learnedResult = WordCandidates.enhance(learnedSupplement, request);
        nativePrefix(learnedSupplement, learnedResult);
        check(position(learnedResult, "卿") == 0 && learnedResult.size() == 6,
                "a learned dictionary word already ranked by native stays first and deduplicates");

        // Exact matches remain unlimited, including rows longer than both historical limits.
        StringBuilder longRow = new StringBuilder("こうき");
        for (int i = 0; i < 80; i++) longRow.append('\t').append("候補").append(i);
        longRow.append('\n');
        WordCandidates.Index longIndex = install(longRow.toString());
        l40.c longRequest = request("こうき");
        List<m40.b> longNative = candidates(longRequest, "後期", "好機", "候補70", "高貴", "候補1");
        List<m40.b> longResult = WordCandidates.enhance(longNative, longRequest);
        nativePrefix(longNative, longResult);
        check(longResult.size() == 83, "all 80 exact dictionary candidates retained with two duplicates removed");
        Set<String> seen = new HashSet<String>();
        for (m40.b candidate : longResult) check(seen.add(candidate.getUserFacingText()), "no duplicate candidate text");
        int next = longNative.size();
        for (int i = 0; i < 80; i++) {
            if (i == 1 || i == 70) continue;
            check(("候補" + i).equals(longResult.get(next++).getUserFacingText()), "exact dictionary relative cost order retained");
        }
        check(WordCandidates.lookup(longIndex, "こうきあ", Collections.<String>emptySet()).isEmpty(), "exact does not widen to unrelated suffix");

        // Prefix search keeps its existing bounds and shortest-completion ordering.
        StringBuilder prefixRows = new StringBuilder();
        for (int i = 0; i < 80; i++) prefixRows.append("あいう").append((char) (0x3041 + i)).append('\t').append("接頭").append(i).append('\n');
        WordCandidates.Index prefixIndex = install(prefixRows.toString());
        check(WordCandidates.lookup(prefixIndex, "あいう", Collections.<String>emptySet()).size() == 12, "prefix suggestion limit retained");
        Set<String> first64 = new HashSet<String>();
        for (int i = 0; i < 64; i++) first64.add("接頭" + i);
        check(WordCandidates.lookup(prefixIndex, "あいう", first64).isEmpty(), "prefix scan remains bounded to 64 rows");
        check(WordCandidates.lookup(prefixIndex, "あい", Collections.<String>emptySet()).isEmpty(), "short prefix exclusion retained");

        install("こーひー\tコーヒー\t珈琲\n");
        l40.c coffee = request("koーhiー");
        List<m40.b> coffeeNative = candidates(coffee, "native1", "native2", "native3");
        com.microsoft.fluency.Japanese.calls = 0;
        List<m40.b> coffeeResult = WordCandidates.enhance(coffeeNative, coffee);
        nativePrefix(coffeeNative, coffeeResult);
        check(com.microsoft.fluency.Japanese.calls == 1 && "koーhiー".equals(com.microsoft.fluency.Japanese.lastInput), "long-mark romaji conversion retained");
        check(coffeeResult.size() == 5 && position(coffeeResult, "珈琲") == 4, "romaji exact candidates retained");

        for (String input : Arrays.asList("helloこーひー", " こーひー", "こーひー ", "", "あ")) {
            coffee.k = input;
            check(WordCandidates.enhance(coffeeNative, coffee) == coffeeNative, "ineligible composition unchanged: " + input);
        }
        coffee.k = "koーhiー";
        coffee.f = true;
        check(WordCandidates.enhance(coffeeNative, coffee) == coffeeNative, "prediction-only request unchanged");
        coffee.f = false;
        coffee.d = com.microsoft.fluency.ResultsFilter.PredictionSearchType.NORMAL;
        check(WordCandidates.enhance(coffeeNative, coffee) == coffeeNative, "English request unchanged");
        check(WordCandidates.enhance(coffeeNative, null) == coffeeNative, "missing request unchanged");
        check(WordCandidates.enhance(null, coffee) == null, "missing candidate list unchanged");
        List<m40.b> empty = new ArrayList<m40.b>();
        check(WordCandidates.enhance(empty, coffee) == empty, "empty native list unchanged");
        install("あい\t愛\n");
        coffee.d = com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;
        check(WordCandidates.enhance(coffeeNative, coffee) == coffeeNative, "no dictionary hit unchanged");

        System.out.println("{\"status\":\"PASSED\",\"mode\":\"fixed\",\"native_priority_preserved\":true,\"candidate_identity_preserved\":true,\"unlimited_exact_dictionary_preserved\":true,\"deduplication_preserved\":true,\"learned_native_rank_preserved\":true,\"checks\":" + checks + ",\"device_tested\":false}");
    }
}
