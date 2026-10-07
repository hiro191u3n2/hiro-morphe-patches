import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.chunk.xml.ResXmlNode;
import com.reandroid.arsc.chunk.xml.ResXmlTextNode;
import com.reandroid.arsc.value.ValueType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Compare only the binary AndroidManifest.xml entries of two APKs using ARSCLib.
 *
 * Usage: VerifyAppManifest ORIGINAL_APK PATCHED_APK REPORT_JSON
 * Exit codes: 0 = PASS, 1 = validation/read failure (JSON written), 2 = usage/report error.
 *
 * Canonical form keeps element order, expanded names, duplicate attributes, attribute
 * resource IDs, typed values, comments and text. Attribute order, namespace prefix
 * spelling, string-pool indexes and binary XML source line numbers are not semantic.
 * Only the exact Android callback attribute on application/activity and the four
 * known compiler attributes on the manifest root are excluded from the comparison.
 * No resources.arsc, DEX, assets, native libraries or whole-APK digest is read.
 */
public final class VerifyAppManifest {
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String CALLBACK_NAME = "enableOnBackInvokedCallback";
    private static final int CALLBACK_ID = 16844396;
    private static final int VERSION_CODE_MAJOR_ID = 16844150;

    private final Map<String, Object> report = new LinkedHashMap<String, Object>();
    private final Map<String, Object> checks = new LinkedHashMap<String, Object>();
    private final List<String> findings = new ArrayList<String>();
    private final List<Object> callbackChecks = new ArrayList<Object>();
    private int assertions;

    private VerifyAppManifest(Path before, Path after) {
        report.put("schema_version", 1);
        report.put("validation", "binary_android_manifest_comparison");
        report.put("original_apk", before.toString());
        report.put("patched_apk", after.toString());
        report.put("apk_entries_read", Arrays.asList("AndroidManifest.xml"));
        report.put("callback_resource_id", CALLBACK_ID);
        report.put("checks", checks);
        report.put("callback_checks", callbackChecks);
        report.put("blocking_findings", findings);
        report.put("physical_device_tested", false);
    }

    private static final class Manifest {
        final byte[] bytes;
        final AndroidManifestBlock block;
        final ResXmlElement root;
        final List<String> canonical = new ArrayList<String>();
        final List<String> canonicalPaths = new ArrayList<String>();
        final List<ResXmlElement> applications = new ArrayList<ResXmlElement>();
        final List<ResXmlElement> activities = new ArrayList<ResXmlElement>();
        final List<Object> compilerMetadata = new ArrayList<Object>();
        int callbacksExcluded;

        Manifest(Path apk) throws Exception {
            bytes = readManifest(apk);
            block = AndroidManifestBlock.load(new ByteArrayInputStream(bytes));
            root = block.getManifestElement();
            if (root == null || !"manifest".equals(root.getName()) || !uri(root.getUri()).isEmpty())
                throw new IOException("Expected an unqualified manifest root: " + apk);
            if (block.getPackageName() == null || block.getPackageName().isEmpty())
                throw new IOException("Manifest package is missing: " + apk);
            canonicalize(root, "/manifest[1]", true);
        }

        private void add(String path, Object... values) {
            canonical.add(json(Arrays.asList(values)));
            canonicalPaths.add(path);
        }

        private void canonicalize(ResXmlElement element, String path, boolean isRoot) throws Exception {
            boolean application = isTag(element, "application");
            boolean activity = isTag(element, "activity");
            if (application) applications.add(element);
            if (activity) activities.add(element);
            add(path, "element", uri(element.getUri()), element.getName(), element.getStartComment());

            List<String> attributes = new ArrayList<String>();
            for (Iterator<ResXmlAttribute> it = element.getAttributes(); it.hasNext();) {
                ResXmlAttribute attribute = it.next();
                if ((application || activity) && isExactCallback(attribute)) {
                    callbacksExcluded++;
                    continue;
                }
                if (isRoot && isCompilerMetadata(attribute)) {
                    compilerMetadata.add(attributeDetails(attribute));
                    continue;
                }
                // Sort a list, not a map: duplicate attribute records must not disappear.
                attributes.add(json(Arrays.asList("attribute", uri(attribute.getUri()),
                    attribute.getName(), attribute.getNameId(), attribute.getType() & 0xff,
                    canonicalValue(attribute))));
            }
            Collections.sort(attributes);
            for (String attribute : attributes) {
                canonical.add(attribute);
                canonicalPaths.add(path + "/@*");
            }

            Map<String, Integer> indexes = new LinkedHashMap<String, Integer>();
            int textIndex = 0;
            for (Iterator<ResXmlNode> it = element.iterator(); it.hasNext();) {
                ResXmlNode node = it.next();
                if (node instanceof ResXmlElement) {
                    ResXmlElement child = (ResXmlElement) node;
                    String key = "{" + uri(child.getUri()) + "}" + child.getName();
                    Integer oldIndex = indexes.get(key);
                    int index = oldIndex == null ? 1 : oldIndex + 1;
                    indexes.put(key, index);
                    canonicalize(child, path + "/" + child.getName() + "[" + index + "]", false);
                } else if (node instanceof ResXmlTextNode) {
                    ResXmlTextNode text = (ResXmlTextNode) node;
                    add(path + "/text()[" + (++textIndex) + "]", "text", text.getText(), text.getComment());
                } else {
                    throw new IOException("Unsupported binary XML node at " + path + ": "
                        + node.getClass().getName());
                }
            }
            add(path, "end", element.getEndComment());
        }

        Map<String, Object> identity() throws Exception {
            ResXmlAttribute major = root.searchAttributeByResourceId(VERSION_CODE_MAJOR_ID);
            return object("package", block.getPackageName(), "version_code", block.getVersionCode(),
                "version_code_major", major == null ? null : canonicalValue(major),
                "version_name", block.getVersionName(), "min_sdk", block.getMinSdkVersion(),
                "target_sdk", block.getTargetSdkVersion(), "application_count", applications.size(),
                "activity_count", activities.size(), "manifest_bytes", bytes.length,
                "manifest_sha256", sha256(bytes));
        }
    }

    private void check(String key, boolean okay, String failure) {
        assertions++;
        checks.put(key, okay);
        if (!okay) findings.add(failure);
    }

    private boolean callback(ResXmlElement element, String path) {
        List<ResXmlAttribute> candidates = new ArrayList<ResXmlAttribute>();
        for (Iterator<ResXmlAttribute> it = element.getAttributes(); it.hasNext();) {
            ResXmlAttribute attribute = it.next();
            if (CALLBACK_NAME.equals(attribute.getName()) || attribute.getNameId() == CALLBACK_ID)
                candidates.add(attribute);
        }
        List<Object> values = new ArrayList<Object>();
        for (ResXmlAttribute attribute : candidates) values.add(attributeDetails(attribute));
        boolean okay = candidates.size() == 1;
        if (okay) {
            ResXmlAttribute attribute = candidates.get(0);
            okay = isExactCallback(attribute) && attribute.getValueType() == ValueType.BOOLEAN
                && attribute.getValueAsBoolean();
        }
        assertions++;
        callbackChecks.add(object("path", path,
            "android_name", AndroidManifestBlock.getAndroidNameValue(element),
            "valid", okay, "attributes", values));
        if (!okay) findings.add(path + ": expected exactly one android:" + CALLBACK_NAME
            + " with resource ID " + CALLBACK_ID + " and BOOLEAN true; found " + json(values));
        return okay;
    }

    private void verify(Path beforePath, Path afterPath) throws Exception {
        Manifest before = new Manifest(beforePath);
        Manifest after = new Manifest(afterPath);
        Map<String, Object> original = before.identity();
        Map<String, Object> patched = after.identity();
        report.put("original", original);
        report.put("patched", patched);
        report.put("package", after.block.getPackageName());

        for (String field : Arrays.asList("package", "version_code", "version_code_major",
                "version_name", "min_sdk", "target_sdk")) {
            check(field + "_preserved", Objects.equals(original.get(field), patched.get(field)),
                field + " changed: " + json(original.get(field)) + " -> " + json(patched.get(field)));
        }
        check("one_application", before.applications.size() == 1 && after.applications.size() == 1,
            "Expected one application in each manifest; original=" + before.applications.size()
                + ", patched=" + after.applications.size());
        check("activity_count_preserved", !before.activities.isEmpty()
                && before.activities.size() == after.activities.size(),
            "Activity count changed or no activities found: original=" + before.activities.size()
                + ", patched=" + after.activities.size());

        boolean allApplications = after.applications.size() == 1;
        for (int i = 0; i < after.applications.size(); i++)
            allApplications &= callback(after.applications.get(i), "/manifest/application[" + (i + 1) + "]");
        boolean allActivities = !after.activities.isEmpty();
        for (int i = 0; i < after.activities.size(); i++)
            allActivities &= callback(after.activities.get(i), "/manifest/application/activity[" + (i + 1) + "]");
        check("application_callback_enabled", allApplications, "Application callback validation failed");
        check("all_activity_callbacks_enabled", allActivities, "One or more Activity callback validations failed");
        report.put("activity_callback_flags", after.activities.size());

        boolean canonicalEqual = before.canonical.equals(after.canonical);
        check("canonical_manifest_unchanged", canonicalEqual,
            "Manifest changed outside the exact callback attributes and root compiler metadata");
        Map<String, Object> canonical = object(
            "format", "ordered expanded-name XML events; sorted attributes with resource IDs and typed values; comments/text retained",
            "original_sha256", sha256(join(before.canonical).getBytes(StandardCharsets.UTF_8)),
            "patched_sha256", sha256(join(after.canonical).getBytes(StandardCharsets.UTF_8)),
            "original_event_count", before.canonical.size(), "patched_event_count", after.canonical.size(),
            "equal", canonicalEqual);
        if (!canonicalEqual) {
            int index = 0;
            while (index < before.canonical.size() && index < after.canonical.size()
                    && before.canonical.get(index).equals(after.canonical.get(index))) index++;
            canonical.put("first_difference", object("event_index", index,
                "original_path", at(before.canonicalPaths, index), "patched_path", at(after.canonicalPaths, index),
                "original_event", at(before.canonical, index), "patched_event", at(after.canonical, index)));
        }
        report.put("canonical", canonical);
        report.put("excluded_attributes", object(
            "original_callback_count", before.callbacksExcluded, "patched_callback_count", after.callbacksExcluded,
            "original_root_compiler_metadata", before.compilerMetadata,
            "patched_root_compiler_metadata", after.compilerMetadata));
    }

    private static String at(List<String> values, int index) {
        return index < values.size() ? values.get(index) : null;
    }

    private static boolean isTag(ResXmlElement element, String name) {
        return name.equals(element.getName()) && uri(element.getUri()).isEmpty();
    }

    private static boolean isExactCallback(ResXmlAttribute attribute) {
        return CALLBACK_NAME.equals(attribute.getName()) && attribute.getNameId() == CALLBACK_ID
            && ANDROID_NS.equals(attribute.getUri());
    }

    private static boolean isCompilerMetadata(ResXmlAttribute attribute) {
        String name = attribute.getName();
        if (uri(attribute.getUri()).isEmpty() && attribute.getNameId() == 0)
            return "platformBuildVersionCode".equals(name) || "platformBuildVersionName".equals(name);
        return ANDROID_NS.equals(attribute.getUri())
            && (("compileSdkVersion".equals(name) && attribute.getNameId() == 16844146)
                || ("compileSdkVersionCodename".equals(name) && attribute.getNameId() == 16844147));
    }

    private static Object canonicalValue(ResXmlAttribute attribute) {
        if (attribute.getValueType() == ValueType.STRING) return attribute.getValueAsString();
        if (attribute.getValueType() == ValueType.BOOLEAN) return attribute.getValueAsBoolean();
        // Numeric/resource/float/complex values compare exact typed data, without resource resolution.
        return attribute.getData();
    }

    private static Map<String, Object> attributeDetails(ResXmlAttribute attribute) {
        return object("namespace", uri(attribute.getUri()), "name", attribute.getName(),
            "resource_id", attribute.getNameId(), "type", String.valueOf(attribute.getValueType()),
            "type_code", attribute.getType() & 0xff, "value", canonicalValue(attribute));
    }

    private static String uri(String value) { return value == null ? "" : value; }

    private static byte[] readManifest(Path path) throws IOException {
        try (ZipFile apk = new ZipFile(path.toFile())) {
            int matches = 0;
            for (Enumeration<? extends ZipEntry> entries = apk.entries(); entries.hasMoreElements();)
                if ("AndroidManifest.xml".equals(entries.nextElement().getName())) matches++;
            if (matches != 1) throw new IOException("Expected exactly one AndroidManifest.xml in " + path
                + "; found " + matches);
            ZipEntry entry = apk.getEntry("AndroidManifest.xml");
            try (InputStream in = apk.getInputStream(entry); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[16384];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                return out.toByteArray();
            }
        }
    }

    private static String sha256(byte[] value) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(value);
        StringBuilder out = new StringBuilder();
        for (byte b : hash) out.append(String.format("%02x", b & 0xff));
        return out.toString();
    }

    private static String join(List<String> lines) {
        StringBuilder out = new StringBuilder();
        for (String line : lines) out.append(line).append('\n');
        return out.toString();
    }

    private static Map<String, Object> object(Object... pairs) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) out.put((String) pairs[i], pairs[i + 1]);
        return out;
    }

    /** Small deterministic JSON writer, also used for unambiguous canonical records. */
    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map) {
            StringBuilder out = new StringBuilder("{");
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (out.length() != 1) out.append(',');
                out.append(json(entry.getKey().toString())).append(':').append(json(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable) {
            StringBuilder out = new StringBuilder("[");
            for (Object child : (Iterable<?>) value) {
                if (out.length() != 1) out.append(',');
                out.append(json(child));
            }
            return out.append(']').toString();
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20 || Character.isSurrogate(c)) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.append('"').toString();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: VerifyAppManifest ORIGINAL_APK PATCHED_APK REPORT_JSON");
            System.exit(2);
            return;
        }
        Path before = Paths.get(args[0]).toAbsolutePath().normalize();
        Path after = Paths.get(args[1]).toAbsolutePath().normalize();
        Path destination = Paths.get(args[2]).toAbsolutePath().normalize();
        try {
            if (destination.equals(before) || destination.equals(after)
                    || (Files.exists(destination) && ((Files.exists(before) && Files.isSameFile(destination, before))
                        || (Files.exists(after) && Files.isSameFile(destination, after)))))
                throw new IOException("Report path must not overwrite either APK");
        } catch (IOException error) {
            System.err.println(error.getMessage());
            System.exit(2);
            return;
        }
        VerifyAppManifest verifier = new VerifyAppManifest(before, after);
        try {
            verifier.verify(before, after);
        } catch (Exception error) {
            verifier.findings.add(error.getClass().getName() + ": " + error.getMessage());
            verifier.report.put("exception", object("class", error.getClass().getName(), "message", error.getMessage()));
        }
        boolean passed = verifier.findings.isEmpty();
        verifier.report.put("result", passed ? "PASS" : "FAIL");
        verifier.report.put("assertions", verifier.assertions);
        try {
            if (destination.getParent() != null) Files.createDirectories(destination.getParent());
            Files.write(destination, (json(verifier.report) + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            System.err.println("Could not write QA report: " + error);
            System.exit(2);
            return;
        }
        System.out.println((passed ? "PASS" : "FAIL") + " binary manifest: " + verifier.report.get("package")
            + "; activity_callback_flags=" + verifier.report.get("activity_callback_flags")
            + "; assertions=" + verifier.assertions + "; report=" + destination);
        if (!passed) for (String finding : verifier.findings) System.err.println(finding);
        System.exit(passed ? 0 : 1);
    }
}
