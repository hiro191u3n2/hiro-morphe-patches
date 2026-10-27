package hiro.swiftkey;

import android.content.res.Resources;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

/**
 * Single alternates for the Japanese QWERTY punctuation keys.
 *
 * The original KeySwap wrapper runs first. Its SHA checked layout swaps, key
 * widths, flags and cache variants remain intact. This class changes only the
 * popup string, top label and a private extra tag in the two native key records.
 * It does not change either key's type or normal input actor.
 */
public final class PunctuationKeys {
    public static final String MARKER = "hiroPunctuationKeys1817";
    private static final String PERIOD = "\u3002";
    private static final String LONG_VOWEL = "\u30fc";
    private static final String QUESTION = "\uff1f";
    private static final String EXCLAMATION = "\uff01";
    private static final String OLD_PERIOD_POPUPS = "\uff01 \uff1f \u3001";
    // KeySwap uses bits 48..55 for options, 56..58 for groups and 61 as a tag.
    private static final long CACHE_TAG = 0x0800000000000000L;
    private static final int MAX_COMPRESSED = 8192;
    private static final int MAX_EXPANDED = 16384;
    private static final int[] STANDARD_KEYS =
            {10, 10, 9, 9, 5, 6, 6, 7, 6, 7, 7, 8, 6, 7, 7, 8, 4};
    private static final int[] PC_KEYS =
            {11, 11, 10, 11, 5, 6, 6, 7, 7, 8, 8, 8, 6, 6, 7, 7, 4};
    private static final int[] ROW_TYPES =
            {0, 0, 0, 0, 11, 2, 8, 5, 12, 3, 7, 4, 6, 9, 1, 10, 0};
    private static final String STANDARD_SHA256 =
            "681a6141e3e71a5e07e9b406ec38e9388d080cee428ed7927936a2a3521fa2f9";
    private static final String PC_SHA256 =
            "39dd032bc1b398593915dadeb6bd235d9efcb9bb38d3e8849946db78c87cc7b0";

    private PunctuationKeys() {}

    /** Called only after the unchanged pre-1.8.17 KeySwap.wrap implementation. */
    public static KeySwap.Wrapped wrap(sc0.c source, Resources resources,
                                       KeySwap.Wrapped previous) {
        if (source == null || resources == null) return previous;
        try {
            String name = resources.getResourceEntryName(source.a);
            int kind = layoutKind(name);
            if (kind < 0) return previous;

            byte[] original;
            long cacheKey;
            if (previous != null) {
                // The original wrapper always returns this exact class. Refuse
                // arbitrary streams so a failed reset cannot damage fallback.
                if (previous.stream == null
                        || previous.stream.getClass() != ByteArrayInputStream.class) {
                    return previous;
                }
                InputStream stream = previous.stream;
                stream.mark(MAX_COMPRESSED + 1);
                try {
                    original = readBounded(stream, MAX_COMPRESSED);
                } finally {
                    stream.reset();
                }
                cacheKey = previous.cacheKey;
            } else {
                // PC has no KeySwap transform. Read a separate handle: the
                // source's native stream must remain pristine on any failure.
                try (InputStream stream = resources.openRawResource(source.a)) {
                    original = readBounded(stream, MAX_COMPRESSED);
                }
                String expected = kind == 1 ? PC_SHA256 : STANDARD_SHA256;
                if (!expected.equals(sha256(original))) return previous;
                cacheKey = source.b;
            }

            byte[] replacement = transform(original, name);
            if (replacement == original) return previous;
            KeySwap.Wrapped result = new KeySwap.Wrapped(
                    new ByteArrayInputStream(replacement), cacheKey ^ CACHE_TAG);
            if (previous == null) closeQuietly(source.c);
            return result;
        } catch (Exception failure) {
            return previous;
        } catch (LinkageError failure) {
            return previous;
        }
    }

    /**
     * Preserve period's native tap/slider action graph, replacing only the
     * popup factory that would otherwise add the primary and language variants.
     * Null tells the native entry hook to run its original implementation.
     */
    public static fb0.f2 periodPopup(fc0.s1 factory, fc0.m1 geometry,
                                     fc0.v1 fields) {
        try {
            if (factory == null || geometry == null
                    || !matches(fields, PERIOD, QUESTION)) return null;
            return factory.i.a(Collections.singletonList(QUESTION), fields, geometry);
        } catch (RuntimeException failure) {
            return null;
        } catch (LinkageError failure) {
            return null;
        }
    }

    /** Use native singleton construction without generic language additions. */
    public static xc0.a longVowelPopup(fc0.s1 factory, fc0.m1 geometry,
                                      fc0.v1 fields) {
        try {
            if (factory == null || geometry == null
                    || !matches(fields, LONG_VOWEL, EXCLAMATION)) return null;
            xc0.h popup = factory.H(geometry, fields);
            if (popup == null) return null;
            Set<?> characters = popup.c();
            if (characters == null || characters.size() != 1
                    || !characters.contains(EXCLAMATION)) return null;
            // Fullwidth punctuation is identical in both shift states. H also
            // preserves the native accessibility-specific popup construction.
            return new xc0.a(popup, popup);
        } catch (RuntimeException failure) {
            return null;
        } catch (LinkageError failure) {
            return null;
        }
    }

    private static boolean matches(fc0.v1 fields, String primary, String alternate) {
        return fields != null && fields.g != null && fields.g.contains(MARKER)
                && primary.equals(fields.b) && primary.equals(fields.b())
                // fb0.h1.c clears the top label when native secondary labels
                // are hidden. That setting must not change the actual output.
                && (fields.a == null || alternate.equals(fields.a)) && fields.o != null
                && fields.o.size() == 1 && alternate.equals(fields.o.get(0));
    }

    /**
     * Pure, transactional transform of the compressed native binary resource.
     * Unknown, malformed, partially changed and already changed layouts return
     * the identical input array. No input bytes are ever modified in place.
     */
    public static byte[] transform(byte[] compressed, String resourceName) {
        int kind = layoutKind(resourceName);
        if (kind < 0 || compressed == null || compressed.length == 0
                || compressed.length > MAX_COMPRESSED) return compressed;
        try {
            byte[] expanded = inflate(compressed);
            byte[] replacement = transformExpanded(expanded, kind);
            if (replacement == expanded) return compressed;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(compressed.length + 128);
            try (DeflaterOutputStream zip = new DeflaterOutputStream(buffer)) {
                zip.write(replacement);
            }
            byte[] result = buffer.toByteArray();
            return result.length <= MAX_COMPRESSED ? result : compressed;
        } catch (Exception failure) {
            return compressed;
        } catch (LinkageError failure) {
            return compressed;
        }
    }

    private static int layoutKind(String name) {
        if ("keyboard_layout_standard_romaji".equals(name)
                || "keyboard_layout_standard_romaji_compact".equals(name)) return 0;
        if ("keyboard_layout_standard_romaji_pc".equals(name)) return 1;
        return -1;
    }

    private static byte[] transformExpanded(byte[] raw, int kind) throws IOException {
        int periodCount = kind == 1 ? 1 : 12;
        int originalSize = kind == 1 ? 12875 : 12408;
        int changedSize = originalSize + (periodCount + 1) * MARKER.length()
                - periodCount * 8 + 6;
        if (raw.length != originalSize && raw.length != changedSize) return raw;

        Cursor cursor = new Cursor(raw);
        require(cursor.integer() == 1 && cursor.integer() == 3);
        require(!cursor.bool() && cursor.utf().isEmpty());
        require(cursor.number() == 2.0 && cursor.number() == 2.0);
        require(cursor.utf().isEmpty());
        int[] counts = kind == 1 ? PC_KEYS : STANDARD_KEYS;
        require(cursor.integer() == counts.length);

        ArrayList<Edit> edits = new ArrayList<Edit>();
        int periods = 0;
        int vowels = 0;
        int originalTargets = 0;
        int changedTargets = 0;
        for (int row = 0; row < counts.length; row++) {
            require(cursor.small() == ROW_TYPES[row]);
            require(cursor.small() == (row == 0 ? 3 : row == 16 ? 1 : 0));
            require(!cursor.bool());
            double height = cursor.number();
            require(height == (row == 0 ? 0.85 : row == 16 ? 0.75 : 1.0));
            require(cursor.integer() == counts[row]);
            for (int column = 0; column < counts[row]; column++) {
                int[] attributes = new int[6];
                for (int i = 0; i < attributes.length; i++) attributes[i] = cursor.small();
                cursor.number();
                cursor.number();
                boolean[] flags = new boolean[4];
                for (int i = 0; i < flags.length; i++) flags[i] = cursor.bool();
                String[] texts = new String[6];
                int[] starts = new int[6];
                int[] ends = new int[6];
                for (int i = 0; i < texts.length; i++) {
                    starts[i] = cursor.position();
                    texts[i] = cursor.utf();
                    ends[i] = cursor.position();
                }
                double width = cursor.number();
                require(width > 0.0);
                boolean cycles = cursor.bool();
                boolean topIsNull = cursor.bool();
                int topStart = cursor.position();
                String top = topIsNull ? null : cursor.utf();
                int topEnd = cursor.position();
                for (int i = 0; i < 4; i++) cursor.number();

                boolean period = PERIOD.equals(texts[4]);
                boolean vowel = LONG_VOWEL.equals(texts[4]);
                if (!period && !vowel) continue;
                if (period) periods++; else vowels++;

                // These checks distinguish the actual target key records from
                // an incidental occurrence in another text, tag or layout type.
                require(attributes[0] == (period ? 4 : 0));
                require(attributes[1] == 0 && attributes[2] == 0);
                require(attributes[3] == (period ? 0 : 2));
                require(attributes[4] == 0 && attributes[5] == 0);
                require(!flags[0] && !flags[1] && !flags[2] && !flags[3]);
                require(texts[0].isEmpty() && texts[2].isEmpty() && texts[3].isEmpty());
                require(cycles && !topIsNull);

                String alternate = period ? QUESTION : EXCLAMATION;
                boolean original = texts[1].isEmpty()
                        && (period ? OLD_PERIOD_POPUPS : "").equals(texts[5])
                        && (period ? ",!?" : "").equals(top);
                boolean changed = MARKER.equals(texts[1])
                        && alternate.equals(texts[5]) && alternate.equals(top);
                require(original || changed);
                if (changed) {
                    changedTargets++;
                } else {
                    originalTargets++;
                    edits.add(new Edit(starts[1], ends[1], MARKER));
                    edits.add(new Edit(starts[5], ends[5], alternate));
                    edits.add(new Edit(topStart, topEnd, alternate));
                }
            }
        }

        // Native layout-level suffix (zi0.k), retained byte for byte.
        require(cursor.small() == 4 && cursor.bool() && !cursor.bool());
        require("} % ^ ~ | [ ] < > {".equals(cursor.utf()));
        require(!cursor.bool() && !cursor.bool() && cursor.bool() && !cursor.bool());
        require(cursor.utf().isEmpty() && cursor.position() == raw.length);
        require(periods == periodCount && vowels == 1);
        require(originalTargets == 0 || changedTargets == 0);
        if (originalTargets == 0) {
            require(raw.length == changedSize);
            return raw;
        }
        require(raw.length == originalSize);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(changedSize);
        DataOutputStream output = new DataOutputStream(buffer);
        int copied = 0;
        for (Edit edit : edits) {
            require(edit.start >= copied && edit.end <= raw.length);
            output.write(raw, copied, edit.start - copied);
            output.writeUTF(edit.text);
            copied = edit.end;
        }
        output.write(raw, copied, raw.length - copied);
        output.flush();
        require(buffer.size() == changedSize);
        return buffer.toByteArray();
    }

    private static byte[] inflate(byte[] compressed) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                require(count > 0 && output.size() <= MAX_EXPANDED - count);
                output.write(buffer, 0, count);
            }
            require(inflater.getRemaining() == 0);
            return output.toByteArray();
        } finally {
            inflater.end();
        }
    }

    private static byte[] readBounded(InputStream stream, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        for (;;) {
            int count = stream.read(buffer);
            if (count == -1) return output.toByteArray();
            require(count > 0 && output.size() <= limit - count);
            output.write(buffer, 0, count);
        }
    }

    private static String sha256(byte[] value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
        char[] hex = "0123456789abcdef".toCharArray();
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte item : digest) {
            result.append(hex[(item >>> 4) & 15]);
            result.append(hex[item & 15]);
        }
        return result.toString();
    }

    private static void closeQuietly(InputStream stream) {
        if (stream == null) return;
        try { stream.close(); } catch (Exception failure) { /* Result is ready. */ }
        catch (LinkageError failure) { /* Result is ready. */ }
    }

    private static void require(boolean valid) throws IOException {
        if (!valid) throw new IOException("Unrecognized Japanese punctuation layout");
    }

    private static final class Edit {
        final int start;
        final int end;
        final String text;
        Edit(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }

    /** Read the native DataInputStream schema while retaining original spans. */
    private static final class Cursor {
        private final int length;
        private final ByteArrayInputStream bytes;
        private final DataInputStream input;
        Cursor(byte[] raw) {
            length = raw.length;
            bytes = new ByteArrayInputStream(raw);
            input = new DataInputStream(bytes);
        }
        int position() { return length - bytes.available(); }
        int integer() throws IOException { return input.readInt(); }
        int small() throws IOException { return input.readUnsignedShort(); }
        boolean bool() throws IOException {
            int value = input.readUnsignedByte();
            require(value == 0 || value == 1);
            return value != 0;
        }
        String utf() throws IOException { return input.readUTF(); }
        double number() throws IOException {
            double value = input.readDouble();
            require(!Double.isNaN(value) && !Double.isInfinite(value));
            return value;
        }
    }
}
