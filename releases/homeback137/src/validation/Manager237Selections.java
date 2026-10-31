import app.morphe.manager.patcher.patch.PatchKeysKt;
import app.morphe.patcher.patch.AppTarget;
import app.morphe.patcher.patch.Compatibility;
import app.morphe.patcher.patch.Option;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.PatchKt;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolve selections using the unmodified Manager 2.3.0-dev.7 host API. */
public final class Manager237Selections {
    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet())
                parts.add(json(entry.getKey().toString()) + ":" + json(entry.getValue()));
            return "{" + String.join(",", parts) + "}";
        }
        if (value instanceof Collection<?> collection) {
            List<String> parts = new ArrayList<>();
            for (Object item : collection) parts.add(json(item));
            return "[" + String.join(",", parts) + "]";
        }
        if (value.getClass().isArray()) {
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) parts.add(json(Array.get(value, i)));
            return "[" + String.join(",", parts) + "]";
        }
        if (!(value instanceof CharSequence) && !(value instanceof Enum<?>))
            throw new IllegalArgumentException("Unsupported option value type: " + value.getClass());
        StringBuilder escaped = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            switch (c) {
                case '\\': escaped.append("\\\\"); break;
                case '"': escaped.append("\\\""); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (c < 32) escaped.append(String.format("\\u%04x", (int) c));
                    else escaped.append(c);
            }
        }
        return escaped.append('"').toString();
    }

    public static void main(String[] args) {
        if (args.length != 3) throw new IllegalArgumentException("MPP PACKAGE VERSION");
        File bundle = new File(args[0]);
        Map<String, Patch<?>> keyed = PatchKeysKt.keyedFor(
                PatchKt.loadPatchesFromJar(Set.of(bundle)), args[1]);
        List<Object> patches = new ArrayList<>();
        for (Map.Entry<String, Patch<?>> entry : keyed.entrySet()) {
            Patch<?> patch = entry.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", entry.getKey());
            row.put("name", patch.getName());
            row.put("default", patch.getDefault());
            List<String> targetVersions = new ArrayList<>();
            boolean unrestricted = patch.getCompatibility() == null;
            if (patch.getCompatibility() != null) {
                for (Compatibility compatibility : patch.getCompatibility()) {
                    if (!args[1].equals(compatibility.getPackageName())) continue;
                    if (compatibility.getTargets() == null || compatibility.getTargets().isEmpty())
                        unrestricted = true;
                    else for (AppTarget target : compatibility.getTargets()) {
                        targetVersions.add(target.getVersion());
                        if (target.getVersion() == null) unrestricted = true;
                    }
                }
            }
            row.put("target_versions", targetVersions);
            row.put("version_compatible", unrestricted || targetVersions.contains(args[2]));
            Map<String, Object> options = new LinkedHashMap<>();
            for (Option<?> option : patch.getOptions().values()) options.put(option.getKey(), option.getDefault());
            row.put("option_defaults", options);
            patches.add(row);
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema", "manager237-selection-manifest-v1");
        manifest.put("package", args[1]);
        manifest.put("version", args[2]);
        manifest.put("patches", patches);
        System.out.println(json(manifest));
    }
}
