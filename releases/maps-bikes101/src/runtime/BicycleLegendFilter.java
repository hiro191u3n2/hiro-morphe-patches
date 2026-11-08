package app.hiro.mapsbikes;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Selective transform of the verified CompactLegend wire schema.
 *
 * Root 9 holds IconStyle records; root 10 holds icon path strings. Root 3 holds
 * LabelStyle records. LabelStyle field 8 references root-9 icon records and field
 * 7 is the existing hide flag. Setting that flag hides the complete icon style,
 * including its shared circle/background layers. No shared icon is blanked.
 *
 * The input is never mutated. Unknown/malformed formats return the same array.
 */
public final class BicycleLegendFilter {
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final int MAX_FIELDS = 300000;
    private static final int MAX_ENTRIES = 65536;
    private static final int MAX_COMPONENTS = 64;
    private static final int MAX_PATH_BYTES = 8192;
    private static final String PREFIX = "icon/name=assets/icons/";
    private static final String SUFFIX = ".png&scale=4";

    private BicycleLegendFilter() {}

    public static byte[] filter(byte[] original) {
        if (original == null || original.length < 16 || original.length > MAX_BYTES) return original;
        try {
            return filterChecked(original);
        } catch (RuntimeException ignored) {
            // The original native parser owns all unknown or malformed input.
            return original;
        } catch (LinkageError ignored) {
            return original;
        }
    }

    private static byte[] filterChecked(byte[] data) {
        Cursor root = new Cursor(data, 0, data.length);
        List<Slice> styles = new ArrayList<Slice>();
        List<Slice> icons = new ArrayList<Slice>();
        List<String> paths = new ArrayList<String>();
        String prefix = null, suffix = null;
        int fields = 0;
        while (root.next()) {
            if (++fields > MAX_FIELDS) return data;
            if (root.number == 3 && root.wire == 2) {
                if (styles.size() >= MAX_ENTRIES) return data;
                styles.add(root.slice());
            } else if (root.number == 9 && root.wire == 2) {
                if (icons.size() >= MAX_ENTRIES) return data;
                icons.add(root.slice());
            } else if (root.number == 10 && root.wire == 2) {
                if (paths.size() >= MAX_ENTRIES) return data;
                paths.add(root.string());
            } else if (root.number == 11 && root.wire == 2) {
                if (prefix != null) return data;
                prefix = root.string();
            } else if (root.number == 12 && root.wire == 2) {
                if (suffix != null) return data;
                suffix = root.string();
            }
        }
        if (!PREFIX.equals(prefix) || !SUFFIX.equals(suffix) || styles.isEmpty()
                || paths.isEmpty()) return data;

        boolean[] bicycles = new boolean[icons.size()];
        for (int i = 0; i < icons.size(); i++) {
            bicycles[i] = bicycleIcon(data, icons.get(i), paths, prefix, suffix);
        }

        List<Slice> changes = new ArrayList<Slice>();
        for (Slice style : styles) {
            if (styleNeedsHide(data, style, bicycles, paths, prefix, suffix)) changes.add(style);
        }
        if (changes.isEmpty()) return data;

        ByteArrayOutputStream output = new ByteArrayOutputStream(data.length + changes.size() * 4);
        int copied = 0;
        for (Slice style : changes) {
            output.write(data, copied, style.lengthStart - copied);
            writeVarint(output, style.end - style.payloadStart + 2);
            output.write(data, style.payloadStart, style.end - style.payloadStart);
            // Appending a scalar gives standard protobuf last-value semantics.
            // Existing bytes, including any earlier false field 7, stay intact.
            output.write(0x38);
            output.write(0x01);
            copied = style.end;
        }
        output.write(data, copied, data.length - copied);
        return output.toByteArray();
    }

    private static boolean bicycleIcon(byte[] data, Slice icon, List<String> paths,
            String prefix, String suffix) {
        Cursor fields = new Cursor(data, icon.payloadStart, icon.end);
        String raw = "";
        int pathIndex = -1, count = 0;
        boolean seenRaw = false, seenIndex = false;
        while (fields.next()) {
            if (++count > MAX_COMPONENTS) return false;
            if (fields.number == 1) {
                if (fields.wire != 2 || seenRaw) return false;
                raw = fields.string();
                if (raw == null) return false;
                seenRaw = true;
            } else if (fields.number == 5) {
                if (fields.wire != 0 || seenIndex || fields.scalar > Integer.MAX_VALUE) return false;
                pathIndex = (int)fields.scalar;
                seenIndex = true;
            } else if (fields.number == 8) {
                // IconStyle.image_id (Lchuz.d -> Lbkjc.d) allows the feature to
                // replace this asset with an unrelated icon. Such a template
                // must be inspected after resolution by the legacy label hook,
                // never globally hidden at legend level. Color-only field 13
                // does not replace the glyph and remains eligible.
                if (fields.wire != 0 || fields.scalar != 0) return false;
            }
        }
        if (!seenIndex) return BicycleMarkerFilter.isBicycleIcon(raw);
        if (pathIndex < 0 || pathIndex >= paths.size() || paths.get(pathIndex) == null) return false;
        // Matches Lbklf.a: explicitly absolute/data URLs bypass prefix expansion.
        if (raw.startsWith("http:") || raw.startsWith("https:") || raw.startsWith("data:")) {
            return BicycleMarkerFilter.isBicycleIcon(raw);
        }
        return BicycleMarkerFilter.isBicycleIcon(prefix + paths.get(pathIndex) + raw + suffix);
    }

    private static boolean styleNeedsHide(byte[] data, Slice style, boolean[] bicycles,
            List<String> paths, String prefix, String suffix) {
        Cursor fields = new Cursor(data, style.payloadStart, style.end);
        boolean hasBicycle = false, hidden = false;
        int count = 0;
        while (fields.next()) {
            if (++count > 256) return false;
            if (fields.number == 7) {
                if (fields.wire != 0) return false;
                hidden = fields.scalar != 0;
            } else if (fields.number == 8) {
                hasBicycle |= referencesBicycle(fields, bicycles);
            } else if (fields.number == 3) {
                if (fields.wire != 2) return false;
                hasBicycle |= bicycleIcon(data, fields.slice(), paths, prefix, suffix);
            } else if (fields.number == 5) {
                if (fields.wire != 2) return false;
                hasBicycle |= boxHasBicycle(data, fields.slice(), bicycles, paths, prefix, suffix);
            }
        }
        return hasBicycle && !hidden;
    }

    private static boolean boxHasBicycle(byte[] data, Slice box, boolean[] bicycles,
            List<String> paths, String prefix, String suffix) {
        // LabelStyle field 5 is the verified TextBoxStyle (Lchyj): field 6
        // contains inline icons, field 9 contains packed root-9 icon indices.
        Cursor fields = new Cursor(data, box.payloadStart, box.end);
        int count = 0;
        boolean found = false;
        while (fields.next()) {
            if (++count > 256) return false;
            if (fields.number == 9) found |= referencesBicycle(fields, bicycles);
            else if (fields.number == 6) {
                if (fields.wire != 2) return false;
                found |= bicycleIcon(data, fields.slice(), paths, prefix, suffix);
            }
        }
        return found;
    }

    private static boolean referencesBicycle(Cursor field, boolean[] bicycles) {
        if (field.wire == 0) return indexIsBicycle(field.scalar, bicycles);
        if (field.wire != 2) throw new IllegalArgumentException("Unknown icon index encoding");
        Cursor packed = new Cursor(field.data, field.payloadStart, field.position);
        int count = 0;
        boolean found = false;
        while (packed.position < packed.end) {
            if (++count > MAX_COMPONENTS) throw new IllegalArgumentException("Too many icon components");
            found |= indexIsBicycle(packed.varint(), bicycles);
        }
        return found;
    }

    private static boolean indexIsBicycle(long index, boolean[] bicycles) {
        if (index < 0 || index >= bicycles.length) throw new IllegalArgumentException("Unknown icon index");
        return bicycles[(int)index];
    }

    private static void writeVarint(ByteArrayOutputStream out, int value) {
        while ((value & ~127) != 0) {
            out.write((value & 127) | 128);
            value >>>= 7;
        }
        out.write(value);
    }

    private static final class Slice {
        final int lengthStart, payloadStart, end;
        Slice(int lengthStart, int payloadStart, int end) {
            this.lengthStart = lengthStart;
            this.payloadStart = payloadStart;
            this.end = end;
        }
    }

    private static final class Cursor {
        final byte[] data;
        final int end;
        int position, number, wire, lengthStart, payloadStart;
        long scalar;

        Cursor(byte[] data, int position, int end) {
            this.data = data;
            this.position = position;
            this.end = end;
        }

        boolean next() {
            if (position == end) return false;
            long tag = varint();
            if (tag < 8 || tag > 0xffffffffL) throw new IllegalArgumentException("Invalid protobuf tag");
            number = (int)(tag >>> 3);
            wire = (int)(tag & 7);
            lengthStart = position;
            payloadStart = position;
            scalar = 0;
            if (wire == 0) scalar = varint();
            else if (wire == 1) advance(8);
            else if (wire == 2) {
                long size = varint();
                if (size < 0 || size > end - position) throw new IllegalArgumentException("Invalid protobuf length");
                payloadStart = position;
                advance((int)size);
            } else if (wire == 5) advance(4);
            else throw new IllegalArgumentException("Unsupported protobuf wire type");
            return true;
        }

        void advance(int size) {
            if (size < 0 || size > end - position) throw new IllegalArgumentException("Truncated protobuf field");
            position += size;
        }

        long varint() {
            long result = 0;
            for (int i = 0; i < 10; i++) {
                if (position >= end) throw new IllegalArgumentException("Truncated protobuf varint");
                int value = data[position++] & 255;
                if (i == 9 && value > 1) throw new IllegalArgumentException("Oversized protobuf varint");
                result |= (long)(value & 127) << (i * 7);
                if ((value & 128) == 0) return result;
            }
            throw new IllegalArgumentException("Unterminated protobuf varint");
        }

        String string() {
            int length = position - payloadStart;
            if (wire != 2 || length > MAX_PATH_BYTES) return null;
            return new String(data, payloadStart, length, StandardCharsets.UTF_8);
        }

        Slice slice() { return new Slice(lengthStart, payloadStart, position); }
    }
}
