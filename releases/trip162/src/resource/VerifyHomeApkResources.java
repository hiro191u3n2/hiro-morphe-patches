import com.reandroid.apk.ApkModule;
import com.reandroid.apk.ResFile;
import com.reandroid.arsc.chunk.xml.*;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;

/** Read-only audit of encoded resources/assets in the APK produced by real MPP apply. */
public final class VerifyHomeApkResources {
    static final Set<String> TARGETS = Set.of("t0", "pf", "s9", "p8");
    static final Set<String> COLLISIONS = Set.of("t4", "pj", "sc", "pb");
    static final String HOME = "com.ctrip.ibu.home.home.presentation.page.fragment.widget.HomeAppBarLayout";
    static final String HEAD = "com.ctrip.ibu.home.home.presentation.head.container.HomeHeadContainer";
    static final String TABS = "com.ctrip.ibu.home.home.presentation.feeds.citylist.FeedsCityTabLayout";
    static final String PAGER = "androidx.viewpager2.widget.ViewPager2";
    static final int PAGER_ID = 0x7f0a09ec; // Trip 8.54.2 @id/b4r, unchanged by split merge.
    static final int XML_GONE = 2; // Compiled android:visibility enum; View.GONE itself is 8.
    static final int ORIGINAL_GONE_FRAME_ID = 0x7f0a2a07; // Original @id/h3b in p8.xml.
    static final String ORIGINAL_P8_FIXTURE_SHA = "c088e46150bc576a28cd49835f743192dc87e2e32ed2740b50d1e6824b048a00";
    static final String ANDROID = "http://schemas.android.com/apk/res/android";
    static final String APP = "http://schemas.android.com/apk/res-auto";
    static final String ASSET = "assets/webapp/rn_xtaro_ibu_schedule-431156475-30041599.7z";
    static final String REPLACEMENT = "replacements/rn_xtaro_ibu_schedule-431156475-30041599.7z";
    static void req(boolean test, String text) { if (!test) throw new IllegalStateException(text); }
    record Layout(String path, int resourceId, ResXmlElement root) { }
    static String sha(InputStream stream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (stream) {
            byte[] data = new byte[65536]; int n;
            while ((n = stream.read(data)) != -1) digest.update(data, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    static String sha(Path path) throws Exception { return sha(Files.newInputStream(path)); }
    static String sha(String text) throws Exception { return sha(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))); }
    static String entryHash(ZipFile zip, String name) throws Exception {
        ZipEntry entry = zip.getEntry(name);
        req(entry != null && !entry.isDirectory(), "Missing APK/MPP entry: " + name);
        return sha(zip.getInputStream(entry));
    }
    static Set<String> entries(ZipFile zip, String prefix) {
        Set<String> result = new TreeSet<>();
        var iterator = zip.entries();
        while (iterator.hasMoreElements()) {
            ZipEntry entry = iterator.nextElement();
            if (entry.getName().startsWith(prefix) && !entry.isDirectory()) req(result.add(entry.getName()), "Duplicate ZIP entry");
        }
        return result;
    }
    static Map<String, Layout> layouts(ApkModule module) throws Exception {
        Map<String, Layout> result = new TreeMap<>();
        for (ResFile file : module.listResFiles()) for (var entry : file) {
            if (!entry.getTypeName().equals("layout") || !entry.isDefault()
                    || (!TARGETS.contains(entry.getName()) && !COLLISIONS.contains(entry.getName()))) continue;
            req(result.put(entry.getName(), new Layout(file.getFilePath(), entry.getResourceId(),
                    file.readAsXmlDocument().getDocumentElement())) == null, "Duplicate logical layout");
        }
        Set<String> expected = new TreeSet<>(TARGETS); expected.addAll(COLLISIONS);
        req(result.keySet().equals(expected), "Required eight logical layouts missing");
        return result;
    }
    static ResXmlAttribute attribute(ResXmlElement e, String uri, String name) {
        var attributes = e.getAttributes();
        while (attributes.hasNext()) {
            ResXmlAttribute a = attributes.next();
            if (uri.equals(a.getUri()) && name.equals(a.getName())) return a;
        }
        return null;
    }
    static boolean pager(ResXmlElement element) {
        ResXmlAttribute id = attribute(element, ANDROID, "id");
        return PAGER.equals(element.getName()) && id != null && id.getValueType() == ValueType.REFERENCE && id.getData() == PAGER_ID;
    }
    static boolean allowed(ResXmlElement e, ResXmlAttribute a) {
        if ((HEAD.equals(e.getName()) || TABS.equals(e.getName())) && APP.equals(a.getUri()) && "layout_scrollFlags".equals(a.getName())) return true;
        return (TABS.equals(e.getName()) || pager(e)) && ANDROID.equals(a.getUri()) && "visibility".equals(a.getName());
    }
    static String value(ResXmlAttribute a) {
        ValueType type = a.getValueType();
        // DEC and HEX are two XML spellings of the same runtime integer value.
        if (type == ValueType.DEC || type == ValueType.HEX) return "INT:" + a.getData();
        if (type == ValueType.STRING) return "STRING:" + a.getValueAsString();
        return type.name() + ":" + a.getData();
    }
    static String tree(ResXmlNode node, boolean mask) {
        if (node instanceof ResXmlTextNode text) return text.isIndent() ? "" : "TEXT:" + text.getText();
        req(node instanceof ResXmlElement, "Unexpected binary XML node kind");
        ResXmlElement e = (ResXmlElement) node;
        Map<String, String> attrs = new TreeMap<>();
        var attributes = e.getAttributes();
        while (attributes.hasNext()) {
            ResXmlAttribute a = attributes.next();
            if (mask && allowed(e, a)) continue;
            String key = a.getNameId() + ":" + a.getUri() + ":" + a.getName();
            req(attrs.put(key, value(a)) == null, "Duplicate binary XML attribute");
        }
        StringBuilder out = new StringBuilder().append('<').append(e.getUri()).append(':').append(e.getName()).append('>');
        for (var a : attrs.entrySet()) out.append(a.getKey().length()).append(':').append(a.getKey())
                .append(a.getValue().length()).append(':').append(a.getValue());
        var children = e.iterator();
        while (children.hasNext()) out.append(tree(children.next(), mask));
        return out.append("</>").toString();
    }
    static void expected(String name, ResXmlElement root) {
        if (name.equals("t0") || name.equals("pf")) {
            req(root.getName().equals(HOME), "Incorrect Home root: " + name);
            int heads = 0, tabs = 0;
            var children = root.getElements();
            while (children.hasNext()) {
                ResXmlElement e = (ResXmlElement) children.next();
                if (HEAD.equals(e.getName())) {
                    heads++;
                    req(attribute(e, APP, "layout_scrollFlags") == null, "Header scroll flags retained");
                } else if (TABS.equals(e.getName())) {
                    tabs++;
                    req(attribute(e, APP, "layout_scrollFlags") == null, "Tab scroll flags retained");
                    ResXmlAttribute visibility = attribute(e, ANDROID, "visibility");
                    req(visibility != null && visibility.getData() == XML_GONE, "Recommendation tabs not gone");
                }
            }
            req(heads == 1 && tabs == 1, "Home widgets missing or duplicated");
        } else {
            req(root.getName().equals("FrameLayout"), "Incorrect pager layout root");
            int matches = 0;
            var descendants = root.recursive(ResXmlElement.class);
            while (descendants.hasNext()) {
                ResXmlElement e = descendants.next();
                if (pager(e)) {
                    matches++;
                    ResXmlAttribute visibility = attribute(e, ANDROID, "visibility");
                    req(visibility != null && visibility.getData() == XML_GONE, "Recommendation pager not gone");
                }
            }
            req(matches == 1, "Expected @id/b4r pager missing or duplicated");
        }
    }
    static Map<String, Object> validateGoneEnum(ResXmlElement originalP8, Path fixture) throws Exception {
        req(sha(fixture).equals(ORIGINAL_P8_FIXTURE_SHA), "Unmodified decoded p8 fixture differs");
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var decoded = factory.newDocumentBuilder().parse(fixture.toFile());
        var frames = decoded.getElementsByTagName("FrameLayout");
        int decodedMatches = 0;
        for (int i = 0; i < frames.getLength(); i++) {
            var e = (org.w3c.dom.Element) frames.item(i);
            if ("@id/h3b".equals(e.getAttributeNS(ANDROID, "id"))) {
                decodedMatches++;
                req("gone".equals(e.getAttributeNS(ANDROID, "visibility")), "Original reference FrameLayout is not decoded as gone");
            }
        }
        req(decodedMatches == 1, "Original decoded visibility reference widget missing");
        int binaryMatches = 0;
        var elements = originalP8.recursive(ResXmlElement.class);
        while (elements.hasNext()) {
            var e = elements.next();
            var id = attribute(e, ANDROID, "id");
            if (e.getName().equals("FrameLayout") && id != null && id.getValueType() == ValueType.REFERENCE
                    && id.getData() == ORIGINAL_GONE_FRAME_ID) {
                binaryMatches++;
                var visibility = attribute(e, ANDROID, "visibility");
                req(visibility != null && visibility.getValueType() == ValueType.DEC && visibility.getData() == XML_GONE,
                        "Original compiled gone enum is not DEC=2");
            }
        }
        req(binaryMatches == 1, "Original encoded visibility reference widget missing");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unmodified_layout", "p8"); result.put("widget", "FrameLayout");
        result.put("android_id", "@id/h3b"); result.put("resource_id", "0x7f0a2a07");
        result.put("original_decoded_visibility", "gone"); result.put("original_encoded_visibility_type", "DEC");
        result.put("original_encoded_visibility_data", XML_GONE); result.put("decoded_fixture_sha256", ORIGINAL_P8_FIXTURE_SHA);
        return result;
    }
    public static void main(String[] args) throws Exception {
        req(args.length == 5, "ORIGINAL_BASE_OR_MERGED_APK PATCHED_APK FINAL_MPP UNMODIFIED_P8_XML OUTPUT_JSON");
        Files.deleteIfExists(Path.of(args[4]));
        Path beforePath = Path.of(args[0]), afterPath = Path.of(args[1]), mppPath = Path.of(args[2]);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("result", "PASS_ENCODED_HOME_RESOURCES_AND_ASSETS");
        report.put("original_apk_sha256", sha(beforePath)); report.put("patched_apk_sha256", sha(afterPath));
        report.put("final_mpp_sha256", sha(mppPath));
        List<Object> layoutRecords = new ArrayList<>();
        try (ApkModule old = ApkModule.loadApkFile(beforePath.toFile()); ApkModule next = ApkModule.loadApkFile(afterPath.toFile());
                ZipFile before = new ZipFile(beforePath.toFile()); ZipFile after = new ZipFile(afterPath.toFile()); ZipFile mpp = new ZipFile(mppPath.toFile())) {
            req(old.getPackageName().equals("ctrip.english") && next.getPackageName().equals("ctrip.english"), "Unexpected APK package");
            Map<String, Layout> a = layouts(old), b = layouts(next);
            report.put("compiled_gone_enum_independently_anchored", validateGoneEnum(a.get("p8").root(), Path.of(args[3])));
            for (String name : a.keySet()) {
                Layout original = a.get(name), patched = b.get(name);
                req(original.resourceId() == patched.resourceId(), "Resource ID changed: " + name);
                boolean target = TARGETS.contains(name);
                req(tree(original.root(), target).equals(tree(patched.root(), target)), "Unrelated encoded XML semantics changed: " + name);
                if (target) expected(name, patched.root());
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("logical_layout", name); entry.put("resource_id", String.format("0x%08x", original.resourceId()));
                entry.put("original_zip_path", original.path()); entry.put("patched_zip_path", patched.path());
                entry.put("targeted", target); entry.put("unrelated_semantics_unchanged", true);
                entry.put("original_binary_sha256", entryHash(before, original.path()));
                entry.put("patched_binary_sha256", entryHash(after, patched.path()));
                entry.put("unchanged_semantics_sha256", sha(tree(original.root(), target)));
                layoutRecords.add(entry);
            }
            Set<String> originalAssets = entries(before, "assets/"), patchedAssets = entries(after, "assets/");
            req(originalAssets.equals(patchedAssets), "Asset inventory changed");
            req(entryHash(after, ASSET).equals(entryHash(mpp, REPLACEMENT)), "Packaged MyPlan 7z differs from final MPP");
            int preservedAssets = 0, otherCrnAssets = 0;
            for (String name : originalAssets) if (!name.equals(ASSET)) {
                req(entryHash(before, name).equals(entryHash(after, name)), "Unrelated asset modified: " + name);
                preservedAssets++;
                if (name.startsWith("assets/webapp/")) otherCrnAssets++;
            }
            Set<String> oldLibraries = entries(before, "lib/"), newLibraries = entries(after, "lib/");
            req(oldLibraries.equals(newLibraries), "Native library inventory changed");
            int arm64 = 0;
            for (String name : oldLibraries) {
                req(entryHash(before, name).equals(entryHash(after, name)), "Native library modified: " + name);
                if (name.startsWith("lib/arm64-v8a/")) arm64++;
            }
            report.put("myplan_asset_matches_final_mpp", true);
            report.put("myplan_asset_sha256", entryHash(after, ASSET));
            report.put("other_assets_preserved_byte_for_byte", preservedAssets);
            report.put("other_webapp_assets_preserved_byte_for_byte", otherCrnAssets);
            report.put("native_libraries_preserved_byte_for_byte", oldLibraries.size());
            report.put("arm64_libraries_preserved_byte_for_byte", arm64);
        }
        report.put("four_logical_layout_effects_verified_from_encoded_apk", true);
        report.put("four_alias_collision_layout_semantics_unchanged", true);
        report.put("layout_evidence", layoutRecords);
        report.put("android_device_tested", false); report.put("blocking_findings", List.of());
        String result = new JSONObject(report).toString(2) + "\n";
        Files.writeString(Path.of(args[4]), result);
        System.out.print(result);
    }
}
