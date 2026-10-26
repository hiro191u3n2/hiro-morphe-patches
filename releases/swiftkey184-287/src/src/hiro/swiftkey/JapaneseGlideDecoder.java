package hiro.swiftkey;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Local, bounded Japanese glide lookup. Ambiguous paths are choices, not failures. */
public final class JapaneseGlideDecoder {
    public static final String ASSET = "hiro/glide_ja.bin";
    private static final int HEADER = 2936, MAX_BYTES = 33554432, MAX_ENTRIES = 1000000;
    private static final int MAX_SCANNED = 150000, TOP = 192, POINTS = 64, COARSE = 16;
    private static final int MAX_RESULTS = 12;
    private static final String SHA = "8f21e641a8b7f86c4bc26252be07515f9a4f402e4a2a516187d6194e7b264a4e";
    private static final Result[] EMPTY = new Result[0];
    private static volatile Index index;

    /* The shipped dictionary intentionally excludes 1/2-letter forms and some greetings.
       These are additive readings; the existing 895,801 dictionary rows are preserved. */
    private static final String[] EXTRA_ROMAJI = (
        "ai ie ii e a i u o n " +
        "kore koe kote koi koite are sore dore dare nani doko koko soko asoko " +
        "toukyou tyoukyou neko nekko kata katta gakou gakkou suki sukki saka sakka kita kiita kitta " +
        "hana hanna sushi susi inu yume kuruma densha dennsha sora asa yoru haha anata hai iie " +
        "arigatou ohayou ohayougozaimasu arigatougozaimasu konnnichiha konnnichiwa konnbanha konnbanwa " +
        "oyasumi oyasuminasai sumimasenn gomennnasai onegaishimasu yoroshiku yoroshikuonegaishimasu " +
        "watashi nihonn taberu okashi okayu yokohama kyou asita ashita kinou itumo itsumo " +
        "ka ki ku ke ko sa si shi su se so ta ti chi tu tsu te to na ni nu ne no " +
        "ha hi hu fu he ho ma mi mu me mo ya yu yo ra ri ru re ro wa wo nn " +
        "ga gi gu ge go za zi ji zu ze zo da di du de do ba bi bu be bo pa pi pu pe po " +
        "kya kyu kyo sha shu sho sya syu syo cha chu cho tya tyu tyo nya nyu nyo " +
        "hya hyu hyo mya myu myo rya ryu ryo gya gyu gyo ja ju jo bya byu byo pya pyu pyo " +
        "konnichiha konnichiwa konbanha konbanwa sumimasen gomen gomennasai nihon denwa tenki genki " +
        "daijoubu daisuki wakatta wakarimasita wakarimashita dekita mattete chotto chottomatte " +
        "omedetou otsukaresama otsukaresamadesu ittekimasu itterasshai tadaima okaeri " +
        "itadakimasu gochisousama gochisousamadeshita mosimosi moshimoshi"
    ).split(" ");
    private static final String[][] KANA = {
        {"kya","きゃ"},{"kyu","きゅ"},{"kyo","きょ"},{"gya","ぎゃ"},{"gyu","ぎゅ"},{"gyo","ぎょ"},
        {"sha","しゃ"},{"shi","し"},{"shu","しゅ"},{"she","しぇ"},{"sho","しょ"},
        {"sya","しゃ"},{"syu","しゅ"},{"syo","しょ"},{"cha","ちゃ"},{"chi","ち"},{"chu","ちゅ"},{"che","ちぇ"},{"cho","ちょ"},
        {"tya","ちゃ"},{"tyu","ちゅ"},{"tyo","ちょ"},{"tsu","つ"},
        {"nya","にゃ"},{"nyu","にゅ"},{"nyo","にょ"},{"hya","ひゃ"},{"hyu","ひゅ"},{"hyo","ひょ"},
        {"mya","みゃ"},{"myu","みゅ"},{"myo","みょ"},{"rya","りゃ"},{"ryu","りゅ"},{"ryo","りょ"},
        {"bya","びゃ"},{"byu","びゅ"},{"byo","びょ"},{"pya","ぴゃ"},{"pyu","ぴゅ"},{"pyo","ぴょ"},
        {"jya","じゃ"},{"jyu","じゅ"},{"jyo","じょ"},{"zya","じゃ"},{"zyu","じゅ"},{"zyo","じょ"},
        {"ka","か"},{"ki","き"},{"ku","く"},{"ke","け"},{"ko","こ"},
        {"sa","さ"},{"si","し"},{"su","す"},{"se","せ"},{"so","そ"},
        {"ta","た"},{"ti","ち"},{"tu","つ"},{"te","て"},{"to","と"},
        {"na","な"},{"ni","に"},{"nu","ぬ"},{"ne","ね"},{"no","の"},
        {"ha","は"},{"hi","ひ"},{"hu","ふ"},{"fu","ふ"},{"he","へ"},{"ho","ほ"},
        {"ma","ま"},{"mi","み"},{"mu","む"},{"me","め"},{"mo","も"},
        {"ya","や"},{"yu","ゆ"},{"yo","よ"},{"ra","ら"},{"ri","り"},{"ru","る"},{"re","れ"},{"ro","ろ"},
        {"wa","わ"},{"wo","を"},{"ga","が"},{"gi","ぎ"},{"gu","ぐ"},{"ge","げ"},{"go","ご"},
        {"za","ざ"},{"zi","じ"},{"ji","じ"},{"zu","ず"},{"ze","ぜ"},{"zo","ぞ"},{"ja","じゃ"},{"ju","じゅ"},{"jo","じょ"},
        {"da","だ"},{"di","ぢ"},{"du","づ"},{"de","で"},{"do","ど"},
        {"ba","ば"},{"bi","び"},{"bu","ぶ"},{"be","べ"},{"bo","ぼ"},{"pa","ぱ"},{"pi","ぴ"},{"pu","ぷ"},{"pe","ぺ"},{"po","ぽ"},
        {"fa","ふぁ"},{"fi","ふぃ"},{"fe","ふぇ"},{"fo","ふぉ"},
        {"a","あ"},{"i","い"},{"u","う"},{"e","え"},{"o","お"},{"-","ー"}
    };
    private static final String[] EXTRA_READING = makeExtraReadings();
    private static final byte[][] EXTRA_UTF8 = extraUtf8();

    private JapaneseGlideDecoder() {}
    public static boolean isReady() { return index != null; }

    public static synchronized void load(InputStream in) throws IOException {
        if (index != null) return;
        byte[] header = new byte[HEADER];
        readFully(in, header, 0, HEADER);
        int count = integer(header, 8), payload = integer(header, 12);
        if (!"JGDI0001".equals(new String(header, 0, 8, StandardCharsets.US_ASCII)) ||
                count < 1 || count > MAX_ENTRIES || payload < 1 || payload > MAX_BYTES - HEADER)
            throw new IOException("Glide index header");
        byte[] bytes = new byte[HEADER + payload];
        System.arraycopy(header, 0, bytes, 0, HEADER);
        readFully(in, bytes, HEADER, payload);
        if (in.read() != -1) throw new IOException("Glide trailing data");
        try {
            StringBuilder digest = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes))
                digest.append(String.format(Locale.ROOT, "%02x", b & 255));
            if (!SHA.equals(digest.toString())) throw new IOException("Glide index digest mismatch");
        } catch (NoSuchAlgorithmException e) { throw new IOException(e); }
        int[] buckets = new int[730], offsets = new int[count + 1];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = integer(bytes, 16 + i * 4);
            if (buckets[i] < 0 || buckets[i] > count || (i > 0 && buckets[i] < buckets[i - 1]))
                throw new IOException("Glide bucket bounds");
        }
        if (buckets[0] != 0 || buckets[729] != count) throw new IOException("Glide bucket ends");
        int p = HEADER;
        for (int i = 0; i < count; i++) {
            offsets[i] = p;
            if (p + 2 > bytes.length) throw new IOException("Glide row bounds");
            int n = bytes[p] & 255, h = bytes[p + 1] & 255;
            if (n < 3 || n > 24 || h < 3 || h > 48 || p + 2 + n + h > bytes.length)
                throw new IOException("Glide row size");
            for (int j = 0; j < n; j++) if (code(bytes[p + 2 + j] & 255) < 0)
                throw new IOException("Glide letter");
            p += 2 + n + h;
        }
        if (p != bytes.length) throw new IOException("Glide trailing data");
        offsets[count] = p;
        index = new Index(bytes, offsets, buckets);
    }

    /** Compatibility entry point. The new UI uses decodeCandidates and explicit selection. */
    public static Result decode(float[] trace, float[] centers, int[] codes) {
        Result[] candidates = decodeCandidates(trace, centers, codes);
        if (candidates.length == 0) return null;
        Result r = candidates[0];
        return r.score <= .28f && r.margin >= .08f && !r.fallback && !r.searchLimited ? r : null;
    }

    /** Returns up to twelve distinct readings, including ambiguous interpretations. */
    public static Result[] decodeCandidates(float[] trace, float[] centers, int[] codes) {
        Index dict = index;
        if (dict == null || trace == null || centers == null || codes == null ||
                trace.length < 2 || trace.length > 4096 || (trace.length & 1) != 0 ||
                codes.length < 26 || codes.length > 27 || centers.length != codes.length * 2) return EMPTY;
        float[] x = new float[27], y = new float[27];
        boolean[] present = new boolean[27];
        for (int i = 0; i < codes.length; i++) {
            int c = code(codes[i]);
            if (c < 0 || present[c] || !finite(centers[i * 2]) || !finite(centers[i * 2 + 1])) return EMPTY;
            present[c] = true; x[c] = centers[i * 2]; y[c] = centers[i * 2 + 1];
        }
        float[] spacing = new float[26];
        for (int a = 0; a < 26; a++) {
            if (!present[a]) return EMPTY;
            float best = Float.POSITIVE_INFINITY;
            for (int b = 0; b < 26; b++) if (a != b) best = Math.min(best, distance(x[a], y[a], x[b], y[b]));
            spacing[a] = (float)Math.sqrt(best);
        }
        Arrays.sort(spacing);
        float scale = spacing[13], ox = x[0], oy = y[0];
        if (scale <= .00001f) return EMPTY;
        for (int i = 0; i < 27; i++) { x[i] = (x[i] - ox) / scale; y[i] = (y[i] - oy) / scale; }
        float[] path = new float[trace.length];
        for (int i = 0; i < trace.length; i += 2) {
            if (!finite(trace[i]) || !finite(trace[i + 1])) return EMPTY;
            path[i] = (trace[i] - ox) / scale; path[i + 1] = (trace[i + 1] - oy) / scale;
        }
        float total = length(path, path.length / 2), diameter = 0f;
        for (int a = 0; a < 26; a++) for (int b = a + 1; b < 26; b++)
            diameter = Math.max(diameter, distance(x[a], y[a], x[b], y[b]));
        /* Split layouts have a large gap in key-spacing units. A fixed 120-unit cap
           incorrectly rejected valid long words even with perfect coordinates. */
        if (total > (float)Math.sqrt(diameter) * 48f + 10f) return EMPTY;
        int[] starts = nearest(path[0], path[1], x, y, present);
        int[] ends = nearest(path[path.length - 2], path[path.length - 1], x, y, present);
        if (starts[0] < 0 || ends[0] < 0) return EMPTY;
        float[] sampled = new float[POINTS * 2], coarseTrace = new float[COARSE * 2];
        resample(path, path.length / 2, total, sampled, POINTS);
        resample(path, path.length / 2, total, coarseTrace, COARSE);
        int[] top = new int[TOP]; Arrays.fill(top, Integer.MIN_VALUE);
        float[] topScore = new float[TOP]; Arrays.fill(topScore, Float.POSITIVE_INFINITY);
        float[] shape = new float[96], coarseShape = new float[COARSE * 2];
        for (int i = 0; i < EXTRA_ROMAJI.length; i++) {
            String roman = EXTRA_ROMAJI[i];
            if (EXTRA_READING[i] == null || !contains(starts, code(roman.charAt(0))) ||
                    !contains(ends, code(roman.charAt(roman.length() - 1)))) continue;
            consider(dict, -i - 1, x, y, present, shape, coarseShape, coarseTrace, total, top, topScore);
        }
        int[] positions = new int[starts.length * ends.length], limits = new int[positions.length];
        int ranges = 0;
        for (int a : starts) for (int b : ends) if (a >= 0 && b >= 0) {
            int bucket = a * 27 + b;
            positions[ranges] = dict.buckets[bucket]; limits[ranges++] = dict.buckets[bucket + 1];
        }
        int scanned = 0; boolean limited = false, more = true;
        /* Round robin prevents an imprecise endpoint's first bucket from consuming the budget. */
        scan: while (more) {
            more = false;
            for (int r = 0; r < ranges; r++) {
                int stop = Math.min(positions[r] + 128, limits[r]);
                while (positions[r] < stop) {
                    int row = positions[r]++;
                    consider(dict, row, x, y, present, shape, coarseShape, coarseTrace, total, top, topScore);
                    scanned++;
                    if (scanned >= MAX_SCANNED) {
                        limited = true; break scan;
                    }
                }
                if (positions[r] < limits[r]) more = true;
            }
        }
        List<Scored> scored = new ArrayList<Scored>();
        float[] fineShape = new float[POINTS * 2], prev = new float[POINTS + 1], next = new float[POINTS + 1];
        for (int i = 0; i < TOP && top[i] != Integer.MIN_VALUE; i++) {
            int row = top[i], count = shape(dict, row, x, y, present, shape);
            float len = length(shape, count);
            resample(shape, count, len, fineShape, POINTS);
            float score = dtw(sampled, fineShape, prev, next) + .08f * Math.abs(total - len) / Math.max(.1f, Math.max(total, len));
            if (score <= .48f) scored.add(new Scored(roman(dict, row), reading(dict, row), score, row < 0 ? 0f : .015f, false));
        }
        Collections.sort(scored, new Comparator<Scored>() {
            public int compare(Scored a, Scored b) {
                int c = Float.compare(a.score + a.prior, b.score + b.prior);
                if (c != 0) return c;
                c = Integer.compare(a.romaji.length(), b.romaji.length());
                return c != 0 ? c : a.romaji.compareTo(b.romaji);
            }
        });
        List<Scored> choices = new ArrayList<Scored>();
        float best = scored.isEmpty() ? Float.POSITIVE_INFINITY : scored.get(0).score;
        for (Scored s : scored) {
            if (s.score > best + .20f || hasReading(choices, s.hiragana)) continue;
            choices.add(s); if (choices.size() == MAX_RESULTS) break;
        }
        /* A novel reading may be offered only as an explicitly marked alternative.
           Geometric corners cannot recover all collinear/repeated letters. Never auto-commit it. */
        String fallback = cornerReading(path, x, y, present);
        if (fallback != null) {
            String hira = toHiragana(fallback);
            if (hira != null && !hasReading(choices, hira)) {
                if (choices.size() == MAX_RESULTS) choices.remove(choices.size() - 1);
                choices.add(new Scored(fallback, hira, .49f, 0f, true));
            }
        }
        Result[] result = new Result[choices.size()];
        for (int i = 0; i < choices.size(); i++) {
            Scored s = choices.get(i); float other = Float.POSITIVE_INFINITY;
            for (Scored t : scored) if (!s.hiragana.equals(t.hiragana)) other = Math.min(other, t.score);
            float margin = Math.max(0f, other - s.score);
            result[i] = new Result(s.romaji, s.hiragana, s.score, margin, scanned, s.fallback, limited);
        }
        return result;
    }

    private static void consider(Index dict, int row, float[] x, float[] y, boolean[] present,
            float[] path, float[] sampled, float[] trace, float length, int[] top, float[] scores) {
        int n = shape(dict, row, x, y, present, path);
        if (n < 1) return;
        float len = length(path, n);
        if (len < .15f) {
            for (int i = 0; i < COARSE; i++) if (distance(trace[i * 2], trace[i * 2 + 1], path[0], path[1]) > .25f) return;
        } else if (length < .15f || len < length * .45f || len > length * 2f) return;
        resample(path, n, len, sampled, COARSE);
        float d = 0f;
        for (int i = 0; i < COARSE; i++) {
            d += distance(trace[i * 2], trace[i * 2 + 1], sampled[i * 2], sampled[i * 2 + 1]);
            if (d > scores[TOP - 1] * COARSE) return;
        }
        float score = d / COARSE + .018f * Math.abs(length - len);
        if (score > scores[TOP - 1]) return;
        int same = -1;
        for (int i = 0; i < TOP && top[i] != Integer.MIN_VALUE; i++) if (sameReading(dict, row, top[i])) { same = i; break; }
        if (same >= 0) {
            if (scores[same] <= score) return;
            for (int i = same; i < TOP - 1; i++) { top[i] = top[i + 1]; scores[i] = scores[i + 1]; }
        }
        int p = TOP - 1;
        while (p > 0 && score < scores[p - 1]) { top[p] = top[p - 1]; scores[p] = scores[p - 1]; p--; }
        top[p] = row; scores[p] = score;
    }

    private static int shape(Index dict, int row, float[] x, float[] y, boolean[] present, float[] out) {
        String extra = row < 0 ? EXTRA_ROMAJI[-row - 1] : null;
        int offset = row < 0 ? 0 : dict.offsets[row], n = row < 0 ? extra.length() : dict.bytes[offset] & 255;
        int count = 0, last = -1;
        for (int i = 0; i < n; i++) {
            int c = code(row < 0 ? extra.charAt(i) : dict.bytes[offset + 2 + i] & 255);
            if (c < 0 || !present[c]) return -1;
            if (c != last) { out[count * 2] = x[c]; out[count * 2 + 1] = y[c]; count++; last = c; }
        }
        return count;
    }
    private static boolean sameReading(Index dict, int a, int b) {
        if (a < 0 && b < 0) return EXTRA_READING[-a - 1].equals(EXTRA_READING[-b - 1]);
        if (a < 0 || b < 0) {
            int extra = a < 0 ? -a - 1 : -b - 1, row = a < 0 ? b : a;
            int p = dict.offsets[row], n = dict.bytes[p + 1] & 255;
            byte[] utf8 = EXTRA_UTF8[extra];
            if (n != utf8.length) return false;
            p += 2 + (dict.bytes[p] & 255);
            for (int i = 0; i < n; i++) if (dict.bytes[p + i] != utf8[i]) return false;
            return true;
        }
        int pa = dict.offsets[a], pb = dict.offsets[b], n = dict.bytes[pa + 1] & 255;
        if (n != (dict.bytes[pb + 1] & 255)) return false;
        pa += 2 + (dict.bytes[pa] & 255); pb += 2 + (dict.bytes[pb] & 255);
        for (int i = 0; i < n; i++) if (dict.bytes[pa + i] != dict.bytes[pb + i]) return false;
        return true;
    }
    private static String roman(Index dict, int row) {
        if (row < 0) return EXTRA_ROMAJI[-row - 1];
        int p = dict.offsets[row]; return new String(dict.bytes, p + 2, dict.bytes[p] & 255, StandardCharsets.US_ASCII);
    }
    private static String reading(Index dict, int row) {
        if (row < 0) return EXTRA_READING[-row - 1];
        int p = dict.offsets[row]; return new String(dict.bytes, p + 2 + (dict.bytes[p] & 255), dict.bytes[p + 1] & 255, StandardCharsets.UTF_8);
    }
    private static int[] nearest(float px, float py, float[] x, float[] y, boolean[] present) {
        int first = -1, second = -1; float a = Float.POSITIVE_INFINITY, b = a;
        for (int i = 0; i < 27; i++) if (present[i]) {
            float d = distance(px, py, x[i], y[i]);
            if (d < a) { b = a; second = first; a = d; first = i; }
            else if (d < b) { b = d; second = i; }
        }
        if (a > .9025f) return new int[]{-1, -1};
        if (b > .81f || b > a + .45f) second = -1;
        return new int[]{first, second};
    }
    private static boolean contains(int[] a, int b) { for (int n : a) if (n == b) return true; return false; }
    private static boolean hasReading(List<Scored> list, String reading) { for (Scored s : list) if (s.hiragana.equals(reading)) return true; return false; }

    private static float dtw(float[] a, float[] b, float[] prev, float[] next) {
        Arrays.fill(prev, Float.POSITIVE_INFINITY); prev[0] = 0f;
        for (int i = 1; i <= POINTS; i++) {
            Arrays.fill(next, Float.POSITIVE_INFINITY);
            for (int j = Math.max(1, i - 12); j <= Math.min(POINTS, i + 12); j++)
                next[j] = distance(a[(i - 1) * 2], a[(i - 1) * 2 + 1], b[(j - 1) * 2], b[(j - 1) * 2 + 1]) +
                        Math.min(prev[j - 1], Math.min(prev[j], next[j - 1]));
            float[] swap = prev; prev = next; next = swap;
        }
        return (float)Math.sqrt(prev[POINTS] / POINTS);
    }
    private static void resample(float[] in, int n, float total, float[] out, int count) {
        if (n == 1 || total <= .00001f) {
            for (int i = 0; i < count; i++) { out[i * 2] = in[0]; out[i * 2 + 1] = in[1]; } return;
        }
        int segment = 1; float consumed = 0f;
        float len = (float)Math.sqrt(distance(in[0], in[1], in[2], in[3]));
        for (int i = 0; i < count; i++) {
            float at = i * total / (count - 1);
            while (segment < n - 1 && consumed + len < at) {
                consumed += len; segment++;
                len = (float)Math.sqrt(distance(in[(segment - 1) * 2], in[(segment - 1) * 2 + 1], in[segment * 2], in[segment * 2 + 1]));
            }
            float t = len <= .00001f ? 0f : Math.max(0f, Math.min(1f, (at - consumed) / len));
            out[i * 2] = in[(segment - 1) * 2] * (1 - t) + in[segment * 2] * t;
            out[i * 2 + 1] = in[(segment - 1) * 2 + 1] * (1 - t) + in[segment * 2 + 1] * t;
        }
    }
    private static float length(float[] a, int n) {
        float total = 0f;
        for (int i = 1; i < n; i++) total += (float)Math.sqrt(distance(a[(i - 1) * 2], a[(i - 1) * 2 + 1], a[i * 2], a[i * 2 + 1]));
        return total;
    }
    private static float distance(float a, float b, float c, float d) { float x = a - c, y = b - d; return x * x + y * y; }
    private static int code(int c) { return c == '-' ? 26 : c >= 'a' && c <= 'z' ? c - 'a' : -1; }
    private static boolean finite(float n) { return !Float.isNaN(n) && !Float.isInfinite(n) && Math.abs(n) < 1000000f; }
    private static int integer(byte[] b, int p) { return (b[p] & 255) << 24 | (b[p + 1] & 255) << 16 | (b[p + 2] & 255) << 8 | b[p + 3] & 255; }
    private static void readFully(InputStream in, byte[] bytes, int p, int n) throws IOException {
        while (n > 0) {
            int read = in.read(bytes, p, n);
            if (read < 0) throw new EOFException("Truncated glide index");
            if (read == 0) { int one = in.read(); if (one < 0) throw new EOFException("Truncated glide index"); bytes[p++] = (byte)one; n--; }
            else { p += read; n -= read; }
        }
    }

    /** Simplified path fallback is deliberately opt-in in the candidate UI. */
    private static String cornerReading(float[] path, float[] x, float[] y, boolean[] present) {
        int n = path.length / 2;
        boolean[] keep = new boolean[n]; keep[0] = keep[n - 1] = true;
        int[] from = new int[n], to = new int[n]; int stack = 0;
        from[stack] = 0; to[stack++] = n - 1;
        while (stack > 0) {
            int a = from[--stack], b = to[stack], split = -1; float best = .0324f;
            float dx = path[b * 2] - path[a * 2], dy = path[b * 2 + 1] - path[a * 2 + 1], denom = dx * dx + dy * dy;
            for (int i = a + 1; i < b; i++) {
                float t = denom < .00001f ? 0 : ((path[i * 2] - path[a * 2]) * dx + (path[i * 2 + 1] - path[a * 2 + 1]) * dy) / denom;
                t = Math.max(0, Math.min(1, t));
                float d = distance(path[i * 2], path[i * 2 + 1], path[a * 2] + t * dx, path[a * 2 + 1] + t * dy);
                if (d > best) { best = d; split = i; }
            }
            if (split >= 0) { keep[split] = true; from[stack] = a; to[stack++] = split; from[stack] = split; to[stack++] = b; }
        }
        StringBuilder letters = new StringBuilder(); int last = -1;
        for (int i = 0; i < n; i++) if (keep[i]) {
            int c = nearest(path[i * 2], path[i * 2 + 1], x, y, present)[0];
            if (c < 0) return null;
            if (c != last) { letters.append(c == 26 ? '-' : (char)('a' + c)); last = c; }
        }
        return letters.length() > 0 && letters.length() <= 32 ? letters.toString() : null;
    }
    private static String[] makeExtraReadings() {
        String[] result = new String[EXTRA_ROMAJI.length];
        for (int i = 0; i < result.length; i++) result[i] = toHiragana(EXTRA_ROMAJI[i]);
        return result;
    }
    private static byte[][] extraUtf8() {
        byte[][] result = new byte[EXTRA_READING.length][];
        for (int i = 0; i < result.length; i++) result[i] = EXTRA_READING[i] == null ? new byte[0] : EXTRA_READING[i].getBytes(StandardCharsets.UTF_8);
        return result;
    }
    private static String toHiragana(String roman) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < roman.length();) {
            char c = roman.charAt(i), next = i + 1 < roman.length() ? roman.charAt(i + 1) : 0;
            if (c == 'n' && (next == 0 || (next != 'y' && "aiueo".indexOf(next) < 0))) {
                out.append('ん');
                boolean keepSecondN = next == 'n' && i + 2 < roman.length() && "aiueoy".indexOf(roman.charAt(i + 2)) >= 0;
                i += next == 'n' && !keepSecondN ? 2 : 1; continue;
            }
            if (c == next && c != 'n' && c != '-' && "aiueo".indexOf(c) < 0) { out.append('っ'); i++; continue; }
            boolean found = false;
            for (String[] pair : KANA) if (roman.startsWith(pair[0], i)) {
                out.append(pair[1]); i += pair[0].length(); found = true; break;
            }
            if (!found) return null;
        }
        return out.toString();
    }
    private static final class Index {
        final byte[] bytes; final int[] offsets, buckets;
        Index(byte[] bytes, int[] offsets, int[] buckets) { this.bytes = bytes; this.offsets = offsets; this.buckets = buckets; }
    }
    private static final class Scored {
        final String romaji, hiragana; final float score, prior; final boolean fallback;
        Scored(String romaji, String hiragana, float score, float prior, boolean fallback) {
            this.romaji = romaji; this.hiragana = hiragana; this.score = score; this.prior = prior; this.fallback = fallback;
        }
    }
    public static final class Result {
        public final String romaji, hiragana;
        public final float score, margin;
        public final int scanned;
        public final boolean fallback, searchLimited;
        Result(String romaji, String hiragana, float score, float margin, int scanned) {
            this(romaji, hiragana, score, margin, scanned, false, false);
        }
        Result(String romaji, String hiragana, float score, float margin, int scanned, boolean fallback, boolean searchLimited) {
            this.romaji = romaji; this.hiragana = hiragana; this.score = score; this.margin = margin;
            this.scanned = scanned; this.fallback = fallback; this.searchLimited = searchLimited;
        }
    }
}
