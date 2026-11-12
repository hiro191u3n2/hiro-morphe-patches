package app.hiro.yahootransit.patches;

import app.morphe.engine.patches.PatchBundleLoader;
import app.morphe.patcher.Patcher;
import app.morphe.patcher.PatcherConfig;
import app.morphe.patcher.PatcherResult;
import app.morphe.patcher.dex.BytecodeMode;
import app.morphe.patcher.dex.NoOpDexVerifier;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.ResourcePatch;
import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.resource.ResourceMode;
import app.morphe.patcher.resource.coder.ResourceCoder;
import app.morphe.patcher.util.Document;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Actual APK and Patcher 2.3 resource decoder/encoder; no Android UI test. */
public final class OnRequestResourceDecodeTest {
    private static final String NS = "http://schemas.android.com/apk/res/android";
    private static final String RESULT = "res/layout/fragment_search_result_list.xml";
    private static final String ACTIONS = "res/layout/view_search_btn.xml";
    private static final String VIEW = "jp.co.yahoo.android.apps.transit.ui.compose.components.nearme.NearMeView";
    private static int assertions;

    private static void check(boolean value, String why) {
        assertions++;
        if (!value) throw new AssertionError(why);
    }

    private static PatcherConfig config(Path apk, Path work) {
        return new PatcherConfig(apk.toFile(), work.toFile(), Collections.emptySet(),
            BytecodeMode.NONE, NoOpDexVerifier.INSTANCE, null, "2.3.0", false, null);
    }

    private static org.w3c.dom.Document read(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file)) { return new Document(input); }
    }

    private static String attr(Element element, String name) {
        String value = element.getAttributeNS(NS, name);
        return value.isEmpty() ? element.getAttribute("android:" + name) : value;
    }

    private static boolean shield(Element element) {
        String tag = attr(element, "tag");
        return "hiro_nearme_pr_hidden_v1".equals(tag) || "hiro_ad_slot_hidden_v2".equals(tag);
    }

    private static String identity(Node node) {
        if (!(node instanceof Element)) return "";
        Element element = (Element) node;
        StringBuilder out = new StringBuilder();
        if (!shield(element)) out.append('<').append(element.getTagName()).append('|')
            .append(attr(element, "id")).append('|').append(attr(element, "tag")).append('|')
            .append(element.getAttribute("class")).append('|').append(attr(element, "text")).append('>');
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) out.append(identity(child));
        if (!shield(element)) out.append("</>");
        return out.toString();
    }

    private static String fingerprint(Element element) {
        List<String> attrs = new ArrayList<>();
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            Node a = element.getAttributes().item(i); attrs.add(a.getNodeName() + "=" + a.getNodeValue());
        }
        Collections.sort(attrs);
        return element.getTagName() + attrs + identity(element);
    }

    private static Element id(org.w3c.dom.Document document, String name) {
        NodeList nodes = document.getElementsByTagName("*");
        Element match = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (attr(element, "id").endsWith("/" + name)) {
                check(match == null, "unique control " + name); match = element;
            }
        }
        check(match != null, "existing control " + name);
        return match;
    }

    private static Map<String, String> identities(ResourcePatchContext context) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (String path : context.decode(new String[]{"res/layout*"}, null)) {
            if (path.endsWith(".xml")) result.put(path, identity(read(context.get(path, false)).getDocumentElement()));
        }
        return result;
    }

    private static Map<String, String> searchControls(ResourcePatchContext context) throws Exception {
        Map<String, String> result = new TreeMap<>();
        org.w3c.dom.Document doc = read(context.get(ACTIONS, true));
        for (String name : new String[]{"result_now_condition", "result_back_condition"}) result.put(name, fingerprint(id(doc, name)));
        return result;
    }

    private static String semantic(Node node) {
        if (node instanceof Element) {
            Element element = (Element) node;
            List<String> attrs = new ArrayList<>();
            for (int i = 0; i < element.getAttributes().getLength(); i++) {
                Node a = element.getAttributes().item(i); attrs.add(a.getNodeName() + "=" + a.getNodeValue());
            }
            Collections.sort(attrs);
            StringBuilder out = new StringBuilder(element.getTagName()).append(attrs).append('[');
            for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) out.append(semantic(child));
            return out.append(']').toString();
        }
        return node.getNodeType() == Node.TEXT_NODE ? node.getNodeValue().trim() : "";
    }

    private static Map<String, String> semantics(ResourcePatchContext context) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (String path : context.decode(new String[]{"res/layout*"}, null)) {
            if (path.endsWith(".xml")) result.put(path, semantic(read(context.get(path, false)).getDocumentElement()));
        }
        return result;
    }

    private static String sha(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static boolean zero(String value) { return value.matches("0(?:\\.0+)?(?:dp|dip|px)"); }

    private static void verifyDecodeFailure(ResourcePatch selected, Path directory) throws Exception {
        Files.createDirectories(directory.resolve("res/layout"));
        Files.writeString(directory.resolve("AndroidManifest.xml"), "<manifest package=\"jp.co.yahoo.android.apps.transit\"/>", StandardCharsets.UTF_8);
        Path sentinel = directory.resolve("res/layout/unrelated.xml");
        Files.writeString(sentinel, "<LinearLayout/>", StandardCharsets.UTF_8);
        byte[] before = Files.readAllBytes(sentinel);
        Field field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        field.setAccessible(true); Object unsafe = field.get(null);
        ResourcePatchContext context = (ResourcePatchContext) unsafe.getClass()
            .getMethod("allocateInstance", Class.class).invoke(unsafe, ResourcePatchContext.class);
        final IllegalStateException injected = new IllegalStateException("injected production decoder failure");
        final int[] calls = {0};
        ResourceCoder coder = (ResourceCoder) Proxy.newProxyInstance(ResourceCoder.class.getClassLoader(),
            new Class<?>[]{ResourceCoder.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("getFile")) return directory.resolve((String) arguments[0]).toFile();
                if (method.getName().equals("decode")) {
                    calls[0]++;
                    check(arguments[0].equals(List.of("res/layout*", "res/values*")), "decode both required resource families only");
                    throw injected;
                }
                if (method.getName().equals("close")) return null;
                throw new UnsupportedOperationException(method.getName());
            });
        field = ResourcePatchContext.class.getDeclaredField("resourceCoder"); field.setAccessible(true); field.set(context, coder);
        Throwable failure = null;
        try { selected.execute(context); } catch (Throwable error) { failure = error; }
        boolean retained = false, described = false;
        for (Throwable error = failure; error != null; error = error.getCause()) {
            retained |= error == injected;
            described |= "画面リソースの展開に失敗しました。".equals(error.getMessage());
        }
        check(calls[0] == 1 && retained && described, "present decode API failure propagated with original cause");
        check(java.util.Arrays.equals(before, Files.readAllBytes(sentinel)), "decode failure does not edit layouts");
    }

    private static int verify(ResourcePatchContext context) throws Exception {
        org.w3c.dom.Document doc = read(context.get(RESULT, true));
        NodeList views = doc.getElementsByTagName(VIEW);
        check(views.getLength() == 1, "NearMe View retained exactly once");
        Element view = (Element) views.item(0), parent = (Element) view.getParentNode();
        check("hiro_nearme_pr_hidden_v1".equals(attr(parent, "tag")), "NearMe hidden parent present");
        check("gone".equals(attr(parent, "visibility")) && zero(attr(parent, "layout_height")), "NearMe collapsed");
        int wrappers = 0;
        for (String path : context.decode(new String[]{"res/layout*"}, null)) {
            if (!path.endsWith(".xml")) continue;
            NodeList all = read(context.get(path, false)).getElementsByTagName("*");
            for (int i = 0; i < all.getLength(); i++) {
                Element element = (Element) all.item(i);
                if (!shield(element)) continue;
                wrappers++;
                check("gone".equals(attr(element, "visibility")) && zero(attr(element, "layout_height")), "shield stays collapsed: " + path);
                check(attr(element, "id").isEmpty(), "shield never steals original binding ID");
            }
        }
        check(wrappers > 1, "general ads and NearMe both protected");
        doc = read(context.get(ACTIONS, true));
        NodeList all = doc.getElementsByTagName("*");
        int hiddenButtons = 0;
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            if (element.getTagName().endsWith("Button") && "gone".equals(attr(element, "visibility"))) {
                hiddenButtons++; check(zero(attr(element, "layout_height")), "reverse-search button collapsed");
            }
        }
        check(hiddenButtons == 1, "only reverse-search button hidden");
        for (String name : new String[]{"result_now_condition", "result_back_condition"}) {
            check(!"gone".equals(attr(id(doc, name), "visibility")), "ordinary action visible: " + name);
        }
        // These pre-existing non-ad targets belong to the legacy entrypoint,
        // which must run after materialization even on the FIRST execution.
        doc = read(context.get("res/layout/fragment_search_result_detail.xml", true));
        for (String name : new String[]{"button_search_mistake_form", "result_footer_co2_area", "navi_detail_menu"}) {
            Element hidden = id(doc, name);
            check("gone".equals(attr(hidden, "visibility")) && zero(attr(hidden, "layout_height")), "legacy unwanted footer collapsed on first run: " + name);
        }
        Element taxi = read(context.get("res/layout/view_taxi.xml", true)).getDocumentElement();
        check("gone".equals(attr(taxi, "visibility")) && zero(attr(taxi, "layout_height")), "legacy taxi layout collapsed on first run");
        return wrappers;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) throw new IllegalArgumentException("MPP APK fresh-work [baseline]");
        Path mpp = Paths.get(args[0]), apk = Paths.get(args[1]), work = Paths.get(args[2]);
        boolean baseline = args.length == 4 && args[3].equals("baseline");
        check(!Files.exists(work), "fresh test work directory");
        ResourcePatch selected = null;
        for (Patch patch : PatchBundleLoader.INSTANCE.loadFlat(Collections.singleton(mpp.toFile()))) {
            if ("広告・不要欄を非表示".equals(patch.getName())) {
                check(selected == null && patch instanceof ResourcePatch, "one registered Yahoo resource patch");
                selected = (ResourcePatch) patch;
            }
        }
        check(selected != null, "registered Yahoo patch discovered");
        Map<String, String> expectedIdentities = null, expectedControls = null;
        if (!baseline) {
            verifyDecodeFailure(selected, work.resolve("injected-failure"));
            PatcherConfig referenceConfig = config(apk, work.resolve("reference"));
            try (Patcher reference = new Patcher(referenceConfig)) {
                reference.plusAssign(Collections.singleton(selected));
                ResourcePatchContext context = reference.getContext().getResourceContext$morphe_patcher();
                context.decodeResources$morphe_patcher(ResourceMode.FULL);
                expectedIdentities = identities(context);
                expectedControls = searchControls(context);
            }
        }
        PatcherConfig config = config(apk, work.resolve("apply"));
        try (Patcher patcher = new Patcher(config)) {
            patcher.plusAssign(Collections.singleton(selected));
            check(config.getBehaviors$morphe_patcher().toString().contains("ON_REQUEST_RESOURCE_DECODING"), "production on-request decoder enabled");
            ResourcePatchContext context = patcher.getContext().getResourceContext$morphe_patcher();
            context.decodeResources$morphe_patcher(ResourceMode.FULL);
            Element manifest = read(context.get("AndroidManifest.xml", true)).getDocumentElement();
            check("jp.co.yahoo.android.apps.transit".equals(manifest.getAttribute("package")), "actual Yahoo package");
            check("8.5.9".equals(attr(manifest, "versionName")), "actual reported app version");
            check(context.listDecodableFiles(RESULT, null).contains(RESULT), "APK contains the base result layout");
            check(!context.get(RESULT, false).exists(), "result layout is not materialized before patch execution");
            if (baseline) {
                Throwable failure = null;
                try { selected.execute(context); } catch (Throwable error) { failure = error; }
                boolean reproduced = false;
                for (Throwable error = failure; error != null; error = error.getCause()) {
                    if ("検索結果の画面を確認できません。".equals(error.getMessage())) reproduced = true;
                }
                check(reproduced, "old MPP reproduces the exact user-reported error");
                System.out.println("PASS OnRequestResourceDecodeTest baseline: exact missing-result-layout failure reproduced with real Yahoo 8.5.9 and production Patcher 2.3 decoder. APK sha256=" + sha(apk));
                return;
            }
            selected.execute(context);
            check(expectedIdentities.equals(identities(context)), "all original layout types/IDs/text and topology retained");
            check(expectedControls.equals(searchControls(context)), "normal search controls and attributes unchanged");
            int wrappers = verify(context);
            Map<String, String> after = semantics(context);
            selected.execute(context);
            check(after.equals(semantics(context)), "repeat execution retains every layout element and attribute");
            context.finishDecoding$morphe_patcher();
            PatcherResult.PatchedResources compiled = context.get();
            File encoded = compiled.getResourcesApk();
            check(encoded != null && encoded.isFile(), "real modified resources compiled to binary APK");
            PatcherConfig roundConfig = config(encoded.toPath(), work.resolve("roundtrip"));
            try (Patcher round = new Patcher(roundConfig)) {
                round.plusAssign(Collections.singleton(selected));
                ResourcePatchContext decoded = round.getContext().getResourceContext$morphe_patcher();
                decoded.decodeResources$morphe_patcher(ResourceMode.FULL);
                check(expectedIdentities.equals(identities(decoded)), "binary roundtrip retains original identities and topology");
                check(wrappers == verify(decoded), "binary roundtrip retains every hidden wrapper");
            }
            System.out.println("PASS OnRequestResourceDecodeTest: " + assertions + " assertions; actual Yahoo 8.5.9 APK; production Patcher 2.3 lazy decoder, registered resource callback, all original layout identities/topology, ordinary search controls, hidden NearMe/ads/reverse button, repeated execution, binary encode/redecode. layouts=" + after.size() + "; wrappers=" + wrappers + "; APK sha256=" + sha(apk) + "; MPP sha256=" + sha(mpp) + ". No Android runtime test; bytecode dependency scheduler not exercised by this resource-only regression.");
        }
    }
}
