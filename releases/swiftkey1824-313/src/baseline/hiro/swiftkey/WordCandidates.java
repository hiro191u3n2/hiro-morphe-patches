package hiro.swiftkey;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Bundled reading-to-word supplement; no editor access, logging, training or networking. */
public final class WordCandidates {
    static final String ASSET = "hiro/words_ja.tsv";
    static final int MAX_SUGGESTIONS = 12;
    static final int MAX_PREFIX_ROWS = 64;
    static final int MAX_WORDS_PER_READING = 32;
    static final int MAX_ASSET_BYTES = 48 * 1024 * 1024;
    private static final int MAX_ROWS = 1000000;
    private static volatile Index dictionary;
    private static boolean started;

    private WordCandidates() {}

    /** Background loader keeps one UTF-8 byte array and one primitive row-offset array. */
    public static synchronized void init(Context context) {
        if (started || context == null) return;
        started = true;
        final AssetManager assets = context.getApplicationContext().getAssets();
        Thread loader = new Thread(new Runnable() {
            public void run() {
                try {
                    // Two reads avoid a growing buffer plus a second full-sized copy at startup.
                    int size = 0;
                    byte[] buffer = new byte[65536];
                    InputStream stream = assets.open(ASSET);
                    try {
                        int count;
                        while ((count = stream.read(buffer)) != -1) {
                            size += count;
                            if (size > MAX_ASSET_BYTES) throw new IOException("Dictionary too large");
                        }
                    } finally { stream.close(); }
                    byte[] bytes = new byte[size];
                    stream = assets.open(ASSET);
                    try {
                        int offset = 0;
                        while (offset < size) {
                            int count = stream.read(bytes, offset, size - offset);
                            if (count < 0) throw new IOException("Short dictionary read");
                            if (count == 0) continue;
                            offset += count;
                        }
                        if (stream.read() != -1) throw new IOException("Dictionary size changed");
                    } finally { stream.close(); }
                    dictionary = Index.fromBytes(bytes);
                } catch (Exception ignored) {
                    // This optional supplement must never prevent the native keyboard from working.
                } catch (LinkageError ignored) {
                    // Class/API incompatibility leaves the native candidate list intact.
                }
            }
        }, "JapaneseWordDictionary");
        loader.setPriority(Thread.MIN_PRIORITY);
        loader.setDaemon(true);
        loader.start();
    }

    /** Invoke before EmojiCandidates.enhance so the native first two and emoji visibility remain. */
    public static List<m40.b> enhance(List<m40.b> original, l40.c request) {
        try {
            Index current = dictionary;
            if (current == null || request == null || original == null || original.isEmpty()
                    || request.f
                    || request.d != com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE)
                return original;
            String input = request.k;
            if (input == null || input.length() == 0) return original;
            String reading = input;
            if (isRomaji(input)) reading = com.microsoft.fluency.Japanese.romajiToHiragana(input);
            if (reading == null) return original;
            String query = normalize(reading);
            // Whole composition only: never trim spaces or search the Japanese suffix of English.
            if (!isReading(query)) return original;

            Set<String> existing = new HashSet<String>();
            for (m40.b candidate : original) {
                if (candidate != null) existing.add(candidate.getUserFacingText());
            }
            LinkedHashSet<String> words = lookup(current, query, existing);
            if (words.isEmpty()) return original;
            ArrayList<m40.b> output = new ArrayList<m40.b>(original.size() + words.size());
            int preserve = Math.min(2, original.size());
            output.addAll(original.subList(0, preserve));
            for (String word : words) {
                m40.h0 candidate = new m40.h0(word, input, request);
                candidate.setTrailingSeparator("");
                output.add(candidate);
            }
            output.addAll(original.subList(preserve, original.size()));
            return output;
        } catch (Exception ignored) {
            return original;
        } catch (LinkageError ignored) {
            return original;
        }
    }

    static LinkedHashSet<String> lookup(Index source, String query, Set<String> existing) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        if (!isReading(query)) return result;
        byte[] key = query.getBytes(StandardCharsets.UTF_8);
        int first = source.lowerBound(key);
        if (first >= source.starts.length) return result;
        if (source.compareKey(first, key) == 0) {
            addWords(result, source.words(first), existing);
            return result; // An exact reading never expands into longer unrelated readings.
        }
        if (query.length() < 3) return result;

        int[] rows = new int[MAX_PREFIX_ROWS];
        int count = 0;
        for (int row = first; row < source.starts.length && count < rows.length; row++) {
            if (!source.startsWith(row, key)) break;
            rows[count++] = row;
        }
        // Prefer the shortest completion within the bounded contiguous range.
        // Stable insertion sorting retains dictionary order for equally long readings.
        for (int i = 1; i < count; i++) {
            int row = rows[i], length = source.keyBytes(row), j = i - 1;
            while (j >= 0 && source.keyBytes(rows[j]) > length) {
                rows[j + 1] = rows[j];
                j--;
            }
            rows[j + 1] = row;
        }
        String[][] values = new String[count][];
        for (int i = 0; i < count; i++) values[i] = source.words(rows[i]);
        // Round-robin the cost-ranked columns instead of filling the list with one homonym family.
        for (int rank = 0; rank < MAX_WORDS_PER_READING && result.size() < MAX_SUGGESTIONS; rank++) {
            for (int i = 0; i < count && result.size() < MAX_SUGGESTIONS; i++) {
                if (rank < values[i].length && !existing.contains(values[i][rank]))
                    result.add(values[i][rank]);
            }
        }
        return result;
    }

    private static void addWords(LinkedHashSet<String> output, String[] words, Set<String> existing) {
        for (String word : words) {
            if (!existing.contains(word)) output.add(word);
            if (output.size() >= MAX_SUGGESTIONS) return;
        }
    }

    static String normalize(String input) {
        String value = Normalizer.normalize(input, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '\u30a1' && c <= '\u30f6') c -= 0x60;
            result.append(c);
        }
        return result.toString();
    }

    private static boolean isRomaji(String input) {
        if (input.length() == 0) return false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && c != '\'' && c != '-')
                return false;
        }
        return true;
    }

    static boolean isReading(String value) {
        if (value.length() < 2 || value.length() > 32) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c < '\u3041' || c > '\u3096') && c != '\u30fc') return false;
        }
        return true;
    }

    /** Keys are strictly ascending normalized BMP hiragana; UTF-8 order equals Java String order. */
    static final class Index {
        final byte[] bytes;
        final int[] starts;

        private Index(byte[] bytes, int[] starts) { this.bytes = bytes; this.starts = starts; }

        static Index fromBytes(byte[] bytes) throws IOException {
            if (bytes.length > MAX_ASSET_BYTES) throw new IOException("Dictionary too large");
            int count = 0;
            for (int i = 0; i < bytes.length; i++) if (bytes[i] == '\n') count++;
            if (bytes.length != 0 && bytes[bytes.length - 1] != '\n') count++;
            if (count > MAX_ROWS) throw new IOException("Too many dictionary rows");
            int[] starts = new int[count];
            int row = 0;
            for (int i = 0; i < bytes.length; i++) {
                if (i == 0 || bytes[i - 1] == '\n') starts[row++] = i;
            }
            Index result = new Index(bytes, starts);
            for (int i = 0; i < count; i++) {
                int start = starts[i], end = result.keyEnd(i);
                int length = end - start;
                if (end == bytes.length || bytes[end] != '\t' || length < 6 || length > 96
                        || length % 3 != 0) throw new IOException("Invalid reading column");
                for (int offset = start; offset < end; offset += 3) {
                    int a = bytes[offset] & 255, b = bytes[offset + 1] & 255, c = bytes[offset + 2] & 255;
                    if ((a & 240) != 224 || (b & 192) != 128 || (c & 192) != 128)
                        throw new IOException("Invalid reading encoding");
                    int cp = ((a & 15) << 12) | ((b & 63) << 6) | (c & 63);
                    if ((cp < 0x3041 || cp > 0x3096) && cp != 0x30fc)
                        throw new IOException("Reading must be normalized hiragana");
                }
                int lineEnd = result.lineEnd(i);
                if (end + 1 >= lineEnd) throw new IOException("Empty word column");
                for (int offset = end + 1; offset < lineEnd; offset++) {
                    if (bytes[offset] == '\r' || bytes[offset] == 0
                            || (bytes[offset] == '\t' && (offset == end + 1 || bytes[offset - 1] == '\t')))
                        throw new IOException("Invalid word column");
                }
                if (bytes[lineEnd - 1] == '\t') throw new IOException("Empty final word column");
                if (i > 0 && result.compareRows(i - 1, i) >= 0)
                    throw new IOException("Reading rows must be strictly ascending");
            }
            return result;
        }

        int lowerBound(byte[] key) {
            int low = 0, high = starts.length;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (compareKey(mid, key) < 0) low = mid + 1;
                else high = mid;
            }
            return low;
        }

        int compareKey(int row, byte[] key) {
            int start = starts[row], end = keyEnd(row), length = end - start;
            for (int i = 0; i < Math.min(length, key.length); i++) {
                int difference = (bytes[start + i] & 255) - (key[i] & 255);
                if (difference != 0) return difference;
            }
            return length - key.length;
        }

        private int compareRows(int a, int b) {
            int startA = starts[a], startB = starts[b];
            int lengthA = keyEnd(a) - startA, lengthB = keyEnd(b) - startB;
            for (int i = 0; i < Math.min(lengthA, lengthB); i++) {
                int difference = (bytes[startA + i] & 255) - (bytes[startB + i] & 255);
                if (difference != 0) return difference;
            }
            return lengthA - lengthB;
        }

        boolean startsWith(int row, byte[] key) {
            int start = starts[row];
            if (keyEnd(row) - start < key.length) return false;
            for (int i = 0; i < key.length; i++) if (bytes[start + i] != key[i]) return false;
            return true;
        }

        int keyBytes(int row) { return keyEnd(row) - starts[row]; }

        private int keyEnd(int row) {
            int end = lineEnd(row), offset = starts[row];
            while (offset < end && bytes[offset] != '\t') offset++;
            return offset;
        }

        private int lineEnd(int row) {
            int end = row + 1 < starts.length ? starts[row + 1] : bytes.length;
            return end > 0 && bytes[end - 1] == '\n' ? end - 1 : end;
        }

        String[] words(int row) {
            int start = keyEnd(row) + 1, end = lineEnd(row);
            ArrayList<String> result = new ArrayList<String>();
            for (int offset = start; offset <= end && result.size() < MAX_WORDS_PER_READING; offset++) {
                if (offset == end || bytes[offset] == '\t') {
                    if (offset > start) result.add(new String(bytes, start, offset - start, StandardCharsets.UTF_8));
                    start = offset + 1;
                }
            }
            return result.toArray(new String[result.size()]);
        }
    }
}
