package hiro.swiftkey;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Local Japanese CLDR annotations. Never reads editor text, records input, or uses networking. */
public final class EmojiCandidates {
    private static volatile TreeMap<String, String[]> dictionary;
    private static boolean started;

    private EmojiCandidates() {}

    /** Call once from Application.onCreate; loading stays off the IME main thread. */
    public static synchronized void init(Context context) {
        if (started || context == null) return;
        started = true;
        final AssetManager assets = context.getApplicationContext().getAssets();
        Thread loader = new Thread(new Runnable() {
            public void run() {
                try {
                    TreeMap<String, String[]> result = new TreeMap<String, String[]>();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            assets.open("hiro/emoji_ja.tsv"), "UTF-8"));
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.length() == 0 || line.charAt(0) == '#') continue;
                            String[] columns = line.split("\\t");
                            if (columns.length < 2) continue;
                            String[] values = new String[columns.length - 1];
                            System.arraycopy(columns, 1, values, 0, values.length);
                            result.put(normalize(columns[0]), values);
                        }
                    } finally { reader.close(); }
                    dictionary = result;
                } catch (Exception ignored) {
                    // Retain the app's original list if the optional local dictionary cannot load.
                }
            }
        }, "JapaneseEmojiDictionary");
        loader.setPriority(Thread.MIN_PRIORITY);
        loader.setDaemon(true);
        loader.start();
    }

    /** Runs on the existing candidate worker after its normal filters and ranking complete. */
    public static List<m40.b> enhance(List<m40.b> original, l40.c request) {
        try {
            TreeMap<String, String[]> current = dictionary;
            if (current == null || request == null || original == null || original.size() < 2
                    || request.f || request.d != com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE) return original;
            String input = request.k;
            if (input == null || input.length() == 0) return original;
            // TouchText is the exact active composition, not previously committed surrounding text.
            String reading = input;
            if (!hasJapanese(reading)) {
                reading = com.microsoft.fluency.Japanese.romajiToHiragana(input);
                if (reading == null || !hasJapanese(reading)) return original;
            }
            String query = normalize(reading);
            if (query.length() == 0) return original;
            LinkedHashSet<String> matches = lookup(current, query);
            if (matches.isEmpty()) return original;

            Set<String> existing = new HashSet<String>();
            for (m40.b candidate : original) {
                if (candidate != null) existing.add(candidate.getUserFacingText());
            }
            matches.removeAll(existing);
            if (matches.isEmpty()) return original;

            ArrayList<m40.b> output = new ArrayList<m40.b>(original.size() + matches.size());
            int preserve = 2;
            output.addAll(original.subList(0, preserve));
            java.util.Iterator<String> iterator = matches.iterator();
            // Keep native top choices intact and make one related emoji visible in the short bar.
            output.add(candidate(iterator.next(), input, request));
            output.addAll(original.subList(preserve, original.size()));
            // Every remaining related emoji, including variants, reaches the expanded candidate list.
            while (iterator.hasNext()) output.add(candidate(iterator.next(), input, request));
            return output;
        } catch (Exception ignored) {
            return original;
        } catch (LinkageError ignored) {
            return original;
        }
    }

    private static m40.h0 candidate(String emoji, String input, l40.c request) {
        m40.h0 result = new m40.h0(emoji, input, request);
        result.setTrailingSeparator("");
        return result;
    }

    static LinkedHashSet<String> lookup(TreeMap<String, String[]> source, String query) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        String[] exact = source.get(query);
        if (exact != null) {
            for (String emoji : exact) result.add(emoji);
            return result;
        }
        // Prefix matching starts at two codepoints; one-character exact annotations remain valid.
        if (query.codePointCount(0, query.length()) < 2) return result;
        for (Map.Entry<String, String[]> entry : source.tailMap(query).entrySet()) {
            if (!entry.getKey().startsWith(query)) break;
            for (String emoji : entry.getValue()) result.add(emoji);
        }
        return result;
    }

    static String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).trim();
        StringBuilder result = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch >= '\u30a1' && ch <= '\u30f6') ch -= 0x60;
            result.append(ch);
        }
        return result.toString();
    }

    private static boolean hasJapanese(String input) {
        if (input == null) return false;
        for (int i = 0; i < input.length();) {
            int cp = input.codePointAt(i);
            if ((cp >= 0x3040 && cp <= 0x30ff) || (cp >= 0x3400 && cp <= 0x9fff)
                    || (cp >= 0xff66 && cp <= 0xff9f)) return true;
            i += Character.charCount(cp);
        }
        return false;
    }
}
