package hiro.swiftkey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import m40.b;
import m40.h0;
import l40.c;
import com.microsoft.fluency.ResultsFilter;

/**
 * Method donor only. The build grafts rank(List,c,boolean) into the published
 * class. These field/method declarations and every compile stub are discarded.
 * The original store, reading normalizer, records and file format are retained.
 */
public final class FrequencyLearning {
    private static volatile FrequencyLearning$Store store;
    private static native String reading(String input);

    public static List<b> rank(List<b> original, c request, boolean enabled) {
        try {
            FrequencyLearning$Store current = store;
            if (original == null || original.isEmpty() || request == null
                    || request.f || request.d != ResultsFilter.PredictionSearchType.JAPANESE) {
                return original;
            }
            String normalized = reading(request.k);
            if (normalized == null) return original;
            original = KatakanaGlossCleanup.cleanCandidates(original, request, normalized);
            if (!enabled || current == null || original.isEmpty()) return original;

            HashSet<String> seen = new HashSet<String>();
            for (b candidate : original) {
                if (candidate != null) seen.add(candidate.getUserFacingText());
            }
            List<FrequencyLearning$Entry> history = current.ranked(normalized, seen);
            if (history.isEmpty()) return original;

            ArrayList<b> output = new ArrayList<b>(original.size() + history.size());
            // Native context ranking and all current supplement positions survive.
            // Only a historical spelling absent from the current list is appended.
            output.addAll(original);
            for (FrequencyLearning$Entry entry : history) {
                if (!seen.add(entry.surface)) continue;
                h0 candidate = new h0(entry.surface, request.k, request);
                candidate.setTrailingSeparator("");
                output.add(candidate);
            }
            return KatakanaGlossCleanup.cleanCandidates(output, request, normalized);
        } catch (RuntimeException ignored) {
            return original;
        } catch (LinkageError ignored) {
            return original;
        }
    }
}
