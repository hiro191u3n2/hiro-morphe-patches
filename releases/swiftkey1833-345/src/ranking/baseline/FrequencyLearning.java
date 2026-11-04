package hiro.swiftkey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Reconstructed 1.8.32 rank method. Only rank is executable baseline evidence;
 * the declarations below it are compilation-only stand-ins for retained DEX code.
 */
public final class FrequencyLearning {
    private static volatile FrequencyLearning$Store store;

    public static List<m40.b> rank(List<m40.b> original, l40.c request, boolean enabled) {
        try {
            FrequencyLearning$Store current = store;
            if (original == null || original.isEmpty() || request == null || request.f
                    || request.d != com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE) {
                return original;
            }
            String reading = reading(request.k);
            if (reading == null) return original;
            original = KatakanaGlossCleanup.cleanCandidates(original, request, reading);
            if (!enabled || current == null || original.isEmpty()) return original;
            LinkedHashMap<String, m40.b> available = new LinkedHashMap<>();
            for (m40.b candidate : original) {
                if (candidate == null) continue;
                String surface = candidate.getUserFacingText();
                if (!available.containsKey(surface)) available.put(surface, candidate);
            }
            List<FrequencyLearning$Entry> learned = current.ranked(reading, available.keySet());
            if (learned.isEmpty()) return original;
            ArrayList<m40.b> output = new ArrayList<>(original.size() + learned.size());
            HashSet<String> emitted = new HashSet<>();
            for (FrequencyLearning$Entry entry : learned) {
                m40.b candidate = available.get(entry.surface);
                if (candidate == null) {
                    m40.h0 added = new m40.h0(entry.surface, request.k, request);
                    added.setTrailingSeparator("");
                    candidate = added;
                }
                output.add(candidate);
                emitted.add(entry.surface);
            }
            for (m40.b candidate : original) {
                if (candidate == null || emitted.add(candidate.getUserFacingText())) output.add(candidate);
            }
            return KatakanaGlossCleanup.cleanCandidates(output, request, reading);
        } catch (RuntimeException ignored) {
            return original;
        } catch (LinkageError ignored) {
            return original;
        }
    }

    private static native String reading(String input);
}
