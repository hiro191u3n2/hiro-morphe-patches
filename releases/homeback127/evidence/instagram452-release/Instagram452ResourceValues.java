import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import com.reandroid.arsc.chunk.*;
import com.reandroid.arsc.container.*;
import com.reandroid.arsc.value.*;

/** Verify numeric values/references and bag parents, complementing aapt2's
 * human-readable output where renamed resource keys change reference spelling.
 * Resource names themselves are separately reported by the Python audit. */
public final class Instagram452ResourceValues {
    static int entries;
    static String value(ValueItem value) {
        int type = value.getType() & 255;
        return type + ":" + (type == 3 ? value.getValueAsString() : value.getData());
    }
    static Map<String,String> read(String path) throws Exception {
        Map<String,String> result = new TreeMap<>();
        try (ZipFile zip = new ZipFile(path);
             InputStream stream = zip.getInputStream(zip.getEntry("resources.arsc"))) {
            TableBlock table = TableBlock.load(stream);
            for (PackageBlock pkg : table) for (SpecTypePair pair : pkg.listSpecTypePairs())
                for (TypeBlock type : pair) for (Entry entry : type) {
                    if (entry.isNull()) continue;
                    String key = Integer.toHexString(entry.getResourceId()) + "/" + type.getQualifiers();
                    StringBuilder values = new StringBuilder();
                    if (entry.isComplex()) {
                        ResTableMapEntry bag = entry.getResTableMapEntry();
                        values.append("parent:").append(bag.getParentId()).append('\n');
                        for (ResValueMap item : bag)
                            values.append(item.getNameId()).append('=').append(value(item)).append('\n');
                    } else values.append(value(entry.getResValue()));
                    if (result.put(key, values.toString()) != null)
                        throw new AssertionError("Duplicate resource/configuration");
                    entries++;
                }
        }
        return result;
    }
    static String sha(String path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(Path.of(path))) {
            byte[] bytes = new byte[1024 * 1024];
            for (int count; (count = in.read(bytes)) >= 0;) digest.update(bytes, 0, count);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format("%02x", b & 255));
        return hex.toString();
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("BEFORE_APK AFTER_APK");
        Map<String,String> before = read(args[0]), after = read(args[1]);
        if (!before.equals(after)) {
            Set<String> all = new TreeSet<>(before.keySet()); all.addAll(after.keySet());
            for (String key : all) if (!Objects.equals(before.get(key), after.get(key)))
                throw new AssertionError("Resource typed value/reference delta at " + key);
        }
        System.out.println("{\"result\":\"PASS\",\"input_sha256\":\"" + sha(args[0]) +
            "\",\"output_sha256\":\"" + sha(args[1]) +
            "\",\"resource_configurations\":" + before.size() +
            ",\"raw_typed_values_references_bag_parents_preserved\":true}");
    }
}
