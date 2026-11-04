package hiro.swiftkey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** Model of retained Store.ranked, including its existing cross-reading totals. */
final class FrequencyLearning$Store {
    final List<FrequencyLearning$Entry> entries = new ArrayList<FrequencyLearning$Entry>();
    int lookups;
    Throwable failure;
    synchronized List<FrequencyLearning$Entry> ranked(String reading, Set<String> surfaces) {
        lookups++;
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        if (failure instanceof LinkageError) throw (LinkageError) failure;
        LinkedHashMap<String, FrequencyLearning$Entry> eligible = new LinkedHashMap<String, FrequencyLearning$Entry>();
        for (String surface : surfaces) eligible.put(surface, null);
        for (FrequencyLearning$Entry entry : entries) {
            if (reading.equals(entry.reading) && !eligible.containsKey(entry.surface)) {
                eligible.put(entry.surface, null);
            }
        }
        for (FrequencyLearning$Entry entry : entries) {
            if (!eligible.containsKey(entry.surface)) continue;
            FrequencyLearning$Entry total = eligible.get(entry.surface);
            if (total == null) {
                total = new FrequencyLearning$Entry(reading, entry.surface, 0, 0);
                eligible.put(entry.surface, total);
            }
            total.count = total.count > Long.MAX_VALUE - entry.count ? Long.MAX_VALUE : total.count + entry.count;
            total.recent = Math.max(total.recent, entry.recent);
        }
        List<FrequencyLearning$Entry> result = new ArrayList<FrequencyLearning$Entry>();
        for (FrequencyLearning$Entry entry : eligible.values()) if (entry != null) result.add(entry);
        Collections.sort(result, new Comparator<FrequencyLearning$Entry>() {
            public int compare(FrequencyLearning$Entry a, FrequencyLearning$Entry b) {
                int count = Long.compare(b.count, a.count);
                return count == 0 ? Long.compare(b.recent, a.recent) : count;
            }
        });
        return result;
    }
}
