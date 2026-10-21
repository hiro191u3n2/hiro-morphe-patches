package app.hiro.yahootransit.patches;

import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.resource.coder.ResourceCoder;
import app.morphe.patcher.util.Document;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Regression fixtures use the production Morphe ResourcePatchContext and Document.
 * Visibility-after-load is a resource DOM ancestry model, not an Android runtime test.
 */
public final class AdSlotsTest {
    private static final String NS = "http://schemas.android.com/apk/res/android";
    private static final String MARKER = "hiro_ad_slot_hidden_v2";
    private static final String LEGACY_MARKER = "hiro_nearme_pr_hidden_v1";
    private static final String NEARME = "jp.co.yahoo.android.apps.transit.ui.compose.components.nearme.NearMeView";
    private static final String RESULTS = "jp.co.yahoo.android.apps.transit.ui.view.navi.list.SearchResultListView";
    private static final String BUTTONS = "jp.co.yahoo.android.apps.transit.ui.view.navi.SearchBtnView";
    private static final String TOP = "jp.co.yahoo.android.apps.transit.ui.view.ad.AdView";
    private static final String[] CONFIGS = {"layout", "layout-land", "layout-night"};
    private static final String[] ADS = {"@id/home_top_slot", "@id/home_bottom_slot", "@id/native_ad_card"};
    private static final String[] ORDINARY = {"@id/search_form", "@id/search_button", "@id/header", "@id/adapter", "@id/address", "@id/road", "@id/load_more", "@id/download", "@id/traffic_banner", "@id/custom_banner_view"};
    private static int assertions;

    private static void check(boolean condition, String reason) {
        assertions++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void write(Path path, String text) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, text.getBytes(StandardCharsets.UTF_8));
    }

    private static String attribute(Element element, String name) {
        String value = element.getAttributeNS(NS, name);
        return value.isEmpty() ? element.getAttribute("android:" + name) : value;
    }

    private static void set(Element element, String name, String value) {
        // Morphe's Document is namespace-unaware for existing qualified attributes.
        // Remove the old spelling before simulating an SDK write to avoid duplicate DOM keys.
        element.removeAttribute("android:" + name);
        element.removeAttributeNS(NS, name);
        element.setAttributeNS(NS, "android:" + name, value);
    }

    private static ResourcePatchContext context(Path directory) throws Exception {
        Field singleton = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        Object unsafe = singleton.get(null);
        ResourcePatchContext context = (ResourcePatchContext) unsafe.getClass()
            .getMethod("allocateInstance", Class.class).invoke(unsafe, ResourcePatchContext.class);
        ResourceCoder coder = (ResourceCoder) Proxy.newProxyInstance(ResourceCoder.class.getClassLoader(),
            new Class<?>[]{ResourceCoder.class}, (proxy, method, args) -> {
                if (method.getName().equals("getFile")) return directory.resolve((String) args[0]).toFile();
                if (method.getName().equals("close")) return null;
                throw new UnsupportedOperationException(method.getName());
            });
        Field field = ResourcePatchContext.class.getDeclaredField("resourceCoder");
        field.setAccessible(true);
        field.set(context, coder);
        return context;
    }

    private static String nearMeXml(boolean legacyWrapped) {
        String near = "<" + NEARME + " android:id=\"@id/near_me\" android:visibility=\"visible\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>";
        if (legacyWrapped) near = "<FrameLayout android:tag=\"" + LEGACY_MARKER + "\" android:visibility=\"gone\" android:layout_width=\"0dp\" android:layout_height=\"0dp\" android:alpha=\"0\">" + near + "</FrameLayout>";
        return "<LinearLayout xmlns:android=\"" + NS + "\" android:id=\"@id/result_screen\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" android:orientation=\"vertical\">"
            + "<" + RESULTS + " android:id=\"@id/results\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"
            + near
            + "<" + BUTTONS + " android:id=\"@id/result_buttons\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"
            + "</LinearLayout>";
    }

    private static String ordinaryChildren() {
        return "<LinearLayout android:id=\"@id/search_form\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
            + "<EditText android:id=\"@id/departure\" android:hint=\"出発\" android:layout_width=\"match_parent\" android:layout_height=\"48dp\"/>"
            + "<EditText android:id=\"@id/address\" android:hint=\"駅・住所\" android:layout_width=\"match_parent\" android:layout_height=\"48dp\"/>"
            + "<Button android:id=\"@id/search_button\" android:text=\"検索\" android:layout_width=\"match_parent\" android:layout_height=\"48dp\"/></LinearLayout>"
            + "<TextView android:id=\"@id/header\" android:text=\"経路案内\"/>"
            + "<FrameLayout android:id=\"@id/adapter\"/>"
            + "<TextView android:id=\"@id/road\" android:text=\"道路\"/>"
            + "<Button android:id=\"@id/load_more\" android:text=\"もっと見る\"/>"
            + "<Button android:id=\"@id/download\" android:text=\"保存\"/>"
            + "<TextView android:id=\"@id/traffic_banner\" android:text=\"運行情報\"/>"
            + "<jp.co.yahoo.android.apps.transit.ui.view.CustomBannerView android:id=\"@id/custom_banner_view\" android:contentDescription=\"通常のお知らせ\"/>";
    }

    private static String homeXml() {
        return "<LinearLayout xmlns:android=\"" + NS + "\" android:id=\"@id/home\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
            + "<" + TOP + " android:id=\"@id/home_top_slot\" android:visibility=\"visible\" android:layout_width=\"match_parent\" android:layout_height=\"90dp\"/>"
            + ordinaryChildren()
            + "<include android:id=\"@id/home_bottom_slot\" layout=\"@layout/view_ydn_banner\" android:visibility=\"visible\" android:layout_width=\"match_parent\" android:layout_height=\"250dp\"/>"
            + "<FrameLayout android:id=\"@id/native_ad_card\" android:visibility=\"visible\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"><TextView android:id=\"@id/sponsor_label\" android:text=\"PR\"/></FrameLayout>"
            + "</LinearLayout>";
    }

    private static Path fixture(Path root, String name, boolean legacyWrapped) throws Exception {
        Path directory = root.resolve(name);
        write(directory.resolve("AndroidManifest.xml"), "<manifest xmlns:android=\"" + NS + "\" package=\"jp.co.yahoo.android.apps.transit\" android:versionName=\"8.6.0\"/>");
        write(directory.resolve("res/layout/fragment_search_result_list.xml"), nearMeXml(legacyWrapped));
        return directory;
    }

    private static String fingerprint(Node node) {
        StringBuilder result = new StringBuilder(node.getNodeName());
        if (node.getAttributes() != null) {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < node.getAttributes().getLength(); i++) {
                Node attribute = node.getAttributes().item(i);
                values.add(attribute.getNodeName() + "=" + attribute.getNodeValue());
            }
            Collections.sort(values);
            result.append(values);
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) result.append(fingerprint(child));
            else if (child.getNodeType() == Node.TEXT_NODE && !child.getNodeValue().trim().isEmpty()) result.append(child.getNodeValue().trim());
        }
        return result.toString();
    }

    private static String fingerprint(Path file) throws Exception {
        try (Document document = new Document(file.toFile())) {
            return fingerprint(document.getDocumentElement());
        }
    }

    private static Element byId(org.w3c.dom.Document document, String id) {
        NodeList all = document.getElementsByTagName("*");
        Element found = null;
        for (int i = 0; i < all.getLength(); i++) {
            Element candidate = (Element) all.item(i);
            if (id.equals(attribute(candidate, "id"))) {
                check(found == null, "unique retained binding ID " + id);
                found = candidate;
            }
        }
        check(found != null, "retained binding ID " + id);
        return found;
    }

    private static List<String> ordinaryFingerprints(Path file) throws Exception {
        try (Document document = new Document(file.toFile())) {
            List<String> result = new ArrayList<>();
            for (String id : ORDINARY) result.add(fingerprint(byId(document, id)));
            return result;
        }
    }

    private static int markerCount(org.w3c.dom.Document document, String marker) {
        int count = 0;
        NodeList all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) if (marker.equals(attribute((Element) all.item(i), "tag"))) count++;
        return count;
    }

    private static void hiddenWrapper(Element view, String marker) {
        check(view.getParentNode() instanceof Element, "advertisement keeps parent");
        Element parent = (Element) view.getParentNode();
        check(marker.equals(attribute(parent, "tag")), "advertisement has dedicated hidden parent");
        check(LEGACY_MARKER.equals(marker) ? "FrameLayout".equals(parent.getTagName())
            : Arrays.asList("FrameLayout", "LinearLayout", "RelativeLayout", "androidx.constraintlayout.widget.ConstraintLayout").contains(parent.getTagName()), "hidden parent retains an appropriate layout container type");
        check(attribute(parent, "id").isEmpty(), "wrapper does not take an application binding ID");
        check("gone".equals(attribute(parent, "visibility")), "parent remains gone");
        check("0dp".equals(attribute(parent, "layout_width")) && "0dp".equals(attribute(parent, "layout_height")), "parent remains zero-sized");
        check("0".equals(attribute(parent, "alpha")), "parent remains transparent");
        int children = 0;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) if (n instanceof Element) children++;
        check(children == 1, "wrapper contains only the intended advertisement");
    }

    private static boolean visibleThroughAncestors(Element element) {
        for (Node current = element; current instanceof Element; current = current.getParentNode()) {
            Element node = (Element) current;
            if ("gone".equals(attribute(node, "visibility")) || "invisible".equals(attribute(node, "visibility"))
                    || "0".equals(attribute(node, "alpha")) || "0dp".equals(attribute(node, "layout_width"))
                    || "0dp".equals(attribute(node, "layout_height"))) return false;
        }
        return true;
    }

    private static void verifyHome(Path file, List<String> ordinaryBefore) throws Exception {
        check(ordinaryBefore.equals(ordinaryFingerprints(file)), "search inputs, search button and non-ad tokens survive unchanged");
        try (Document document = new Document(file.toFile())) {
            check("LinearLayout".equals(document.getDocumentElement().getTagName()), "home root type retained");
            check("@id/home".equals(attribute(document.getDocumentElement(), "id")), "home root ID retained");
            for (String id : ADS) hiddenWrapper(byId(document, id), MARKER);
            check(markerCount(document, MARKER) == ADS.length, "exactly the three advertised slots are wrapped");
            check(visibleThroughAncestors(byId(document, "@id/search_button")), "search button remains visible in ancestry model");
        }
    }

    private static void reject(Path directory, String reason) throws Exception {
        boolean rejected = false;
        try { NearMeLayout.apply(context(directory)); }
        catch (IllegalStateException expected) { rejected = true; }
        check(rejected, "reject " + reason);
    }

    public static void main(String[] args) throws Exception {
        Path root = new File(args[0]).toPath();
        Files.createDirectories(root);
        Path directory = fixture(root, "all-configurations", false);
        List<List<String>> ordinary = new ArrayList<>();
        for (String configuration : CONFIGS) {
            Path home = directory.resolve("res/" + configuration + "/fragment_search.xml");
            write(home, homeXml());
            ordinary.add(ordinaryFingerprints(home));
            write(directory.resolve("res/" + configuration + "/view_ydn_banner.xml"),
                "<jp.co.yahoo.android.apps.transit.ui.view.ad.AdLayout xmlns:android=\"" + NS + "\" android:id=\"@id/ad_root\" android:layout_width=\"match_parent\" android:layout_height=\"250dp\"><WebView android:id=\"@id/creative_content\"/></jp.co.yahoo.android.apps.transit.ui.view.ad.AdLayout>");
        }
        Path regular = directory.resolve("res/layout/ordinary_screen.xml");
        write(regular, "<LinearLayout xmlns:android=\"" + NS + "\" android:id=\"@id/address_screen\">" + ordinaryChildren() + "</LinearLayout>");
        String regularBefore = fingerprint(regular);
        NearMeLayout.apply(context(directory));
        check(regularBefore.equals(fingerprint(regular)), "ordinary layout is semantically unchanged");
        List<String> afterFirst = new ArrayList<>();
        for (int i = 0; i < CONFIGS.length; i++) {
            Path home = directory.resolve("res/" + CONFIGS[i] + "/fragment_search.xml");
            verifyHome(home, ordinary.get(i));
            afterFirst.add(fingerprint(home));
            try (Document document = new Document(directory.resolve("res/" + CONFIGS[i] + "/view_ydn_banner.xml").toFile())) {
                check("jp.co.yahoo.android.apps.transit.ui.view.ad.AdLayout".equals(document.getDocumentElement().getTagName()), "standalone custom advertisement root type retained");
                check("@id/ad_root".equals(attribute(document.getDocumentElement(), "id")), "standalone root binding ID retained");
                check("0dp".equals(attribute(document.getDocumentElement(), "layout_width")) && "0dp".equals(attribute(document.getDocumentElement(), "layout_height")), "existing custom root dimensions actually changed on disk");
                check("gone".equals(attribute(document.getDocumentElement(), "visibility")), "custom advertisement root remains hidden");
            }
        }
        NearMeLayout.apply(context(directory));
        for (int i = 0; i < CONFIGS.length; i++) {
            Path home = directory.resolve("res/" + CONFIGS[i] + "/fragment_search.xml");
            check(afterFirst.get(i).equals(fingerprint(home)), "second pass does not add nesting or alter ordinary contents");
            verifyHome(home, ordinary.get(i));
        }

        // Model the app/SDK setting child visibility and dimensions after the ad loads.
        Path home = directory.resolve("res/layout/fragment_search.xml");
        try (Document document = new Document(home.toFile())) {
            for (String id : ADS) {
                Element ad = byId(document, id);
                set(ad, "visibility", "visible"); set(ad, "alpha", "1");
                set(ad, "layout_width", "match_parent"); set(ad, "layout_height", "300dp");
                hiddenWrapper(ad, MARKER);
                check(!visibleThroughAncestors(ad), "SDK child redisplay cannot bypass hidden parent in DOM model");
            }
        }
        try (Document document = new Document(home.toFile())) {
            for (String id : ADS) {
                Element ad = byId(document, id);
                check("visible".equals(attribute(ad, "visibility")) && "300dp".equals(attribute(ad, "layout_height")), "simulated SDK child writes persisted");
                hiddenWrapper(ad, MARKER);
                check(!visibleThroughAncestors(ad), "hidden ancestor persists across resource close and reopen");
            }
        }
        check(ordinary.get(0).equals(ordinaryFingerprints(home)), "SDK redisplay model leaves search form intact");

        Path legacy = fixture(root, "legacy-nearme-parent", true);
        write(legacy.resolve("res/layout/fragment_search.xml"), homeXml());
        NearMeLayout.apply(context(legacy));
        Path legacyResult = legacy.resolve("res/layout/fragment_search_result_list.xml");
        String legacyFirst = fingerprint(legacyResult);
        NearMeLayout.apply(context(legacy));
        check(legacyFirst.equals(fingerprint(legacyResult)), "legacy hidden NearMe parent remains idempotent");
        try (Document document = new Document(legacyResult.toFile())) {
            hiddenWrapper(byId(document, "@id/near_me"), LEGACY_MARKER);
            check(markerCount(document, LEGACY_MARKER) == 1, "single legacy NearMe wrapper retained");
            check(markerCount(document, MARKER) == 0, "no second wrapper around legacy NearMe");
        }
        // Existing wrapper attributes must be replaceable after deserialization, too.
        try (Document document = new Document(legacyResult.toFile())) {
            Element parent = (Element) byId(document, "@id/near_me").getParentNode();
            set(parent, "visibility", "visible"); set(parent, "alpha", "1");
            set(parent, "layout_width", "match_parent"); set(parent, "layout_height", "80dp");
        }
        NearMeLayout.apply(context(legacy));
        try (Document document = new Document(legacyResult.toFile())) {
            hiddenWrapper(byId(document, "@id/near_me"), LEGACY_MARKER);
        }

        // Parent layouts with sibling ID relationships must retain the original hierarchy.
        String[] parents = {"RelativeLayout", "androidx.constraintlayout.widget.ConstraintLayout",
            "androidx.coordinatorlayout.widget.CoordinatorLayout", "jp.co.yahoo.android.apps.transit.ui.view.CustomParent"};
        String[] references = {"android:layout_below=\"@id/ad_slot\"",
            "app:layout_constraintTop_toBottomOf=\"@id/ad_slot\"",
            "app:layout_anchor=\"@id/ad_slot\"", "app:referenceView=\"@id/ad_slot\""};
        Path dependent = fixture(root, "sibling-reference-layouts", false);
        List<String> siblingBefore = new ArrayList<>();
        for (int i = 0; i < parents.length; i++) {
            Path file = dependent.resolve("res/layout/reference_" + i + ".xml");
            write(file, "<" + parents[i] + " xmlns:android=\"" + NS + "\" xmlns:app=\"http://schemas.android.com/apk/res-auto\" android:id=\"@id/reference_root\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                + "<TextView android:id=\"@id/above\" android:text=\"上の通常内容\"/>"
                + "<FrameLayout android:id=\"@id/ad_slot\" android:visibility=\"visible\" android:layout_width=\"match_parent\" android:layout_height=\"100dp\"/>"
                + "<Button android:id=\"@id/referenced_sibling\" android:text=\"通常の操作\" " + references[i] + "/>"
                + "</" + parents[i] + ">");
            try (Document document = new Document(file.toFile())) {
                siblingBefore.add(fingerprint(byId(document, "@id/above")) + fingerprint(byId(document, "@id/referenced_sibling")));
            }
        }
        NearMeLayout.apply(context(dependent));
        for (int i = 0; i < parents.length; i++) {
            Path file = dependent.resolve("res/layout/reference_" + i + ".xml");
            try (Document document = new Document(file.toFile())) {
                Element rootElement = document.getDocumentElement();
                Element ad = byId(document, "@id/ad_slot");
                check(parents[i].equals(rootElement.getTagName()), "dependent layout root type retained");
                check("@id/reference_root".equals(attribute(rootElement, "id")), "dependent layout root ID retained");
                check(ad.getParentNode() == rootElement, "dependent layout target stays a direct child");
                List<String> order = new ArrayList<>();
                for (Node n = rootElement.getFirstChild(); n != null; n = n.getNextSibling()) if (n instanceof Element) order.add(attribute((Element) n, "id"));
                check(order.equals(Arrays.asList("@id/above", "@id/ad_slot", "@id/referenced_sibling")), "dependent layout direct-child order retained");
                check(markerCount(document, MARKER) == 0, "dependent layout receives no hierarchy-changing wrapper");
                check("gone".equals(attribute(ad, "visibility")), "unwrapped advertisement is hidden");
                check("0dp".equals(attribute(ad, "layout_width")) && "0dp".equals(attribute(ad, "layout_height")), "unwrapped advertisement is zero-sized");
                check(siblingBefore.get(i).equals(fingerprint(byId(document, "@id/above")) + fingerprint(byId(document, "@id/referenced_sibling"))), "sibling content and ID references remain unchanged");
            }
        }

        Path binding = fixture(root, "data-binding-preservation", false);
        Path bindingFile = binding.resolve("res/layout/binding_screen.xml");
        write(bindingFile, "<layout xmlns:android=\"" + NS + "\"><data/><LinearLayout android:tag=\"layout/binding_screen_0\"><FrameLayout android:id=\"@id/ad_slot\" android:tag=\"binding_1\" android:layout_height=\"80dp\"/><Button android:id=\"@id/binding_button\" android:text=\"検索\"/></LinearLayout></layout>");
        NearMeLayout.apply(context(binding));
        try (Document document = new Document(bindingFile.toFile())) {
            Element ad = byId(document, "@id/ad_slot");
            check("LinearLayout".equals(((Element) ad.getParentNode()).getTagName()), "Data Binding container index preserved");
            check("binding_1".equals(attribute(ad, "tag")), "Data Binding view tag preserved");
            check(markerCount(document, MARKER) == 0, "Data Binding includes and views not reparented");
            check("gone".equals(attribute(ad, "visibility")), "Data Binding advertisement collapsed in place");
            check(byId(document, "@id/binding_button").getParentNode() == ad.getParentNode(), "Data Binding sibling hierarchy preserved");
        }

        Path indirect = fixture(root, "indirect-inflation", false);
        Path indirectFile = indirect.resolve("res/layout/indirect.xml");
        write(indirectFile, "<LinearLayout xmlns:android=\"" + NS + "\"><view class=\"sdk.BannerAdView\" android:id=\"@id/inflated_slot\"/><ViewStub android:id=\"@id/lazy_slot\" android:layout=\"@layout/view_ydn_ad\"/></LinearLayout>");
        NearMeLayout.apply(context(indirect));
        try (Document document = new Document(indirectFile.toFile())) {
            hiddenWrapper(byId(document, "@id/inflated_slot"), MARKER);
            hiddenWrapper(byId(document, "@id/lazy_slot"), MARKER);
        }

        for (boolean alternate : Arrays.asList(false, true)) {
            Path malformed = fixture(root, alternate ? "malformed-night-nearme" : "malformed-base-nearme", false);
            Path untouchedHome = malformed.resolve("res/layout/aaa_home.xml");
            write(untouchedHome, homeXml());
            String beforeHome = fingerprint(untouchedHome);
            Path baseResult = malformed.resolve("res/layout/fragment_search_result_list.xml");
            if (alternate) write(malformed.resolve("res/layout-night/fragment_search_result_list.xml"), "<LinearLayout/>");
            else write(baseResult, nearMeXml(false).replace("</LinearLayout>", "<" + NEARME + " android:id=\"@id/second_near_me\"/></LinearLayout>"));
            String beforeResult = fingerprint(baseResult);
            reject(malformed, "malformed NearMe topology before home edits");
            check(beforeHome.equals(fingerprint(untouchedHome)), "invalid NearMe preflight leaves home advertisements untouched");
            check(beforeResult.equals(fingerprint(baseResult)), "invalid NearMe preflight leaves base results untouched");
        }
        System.out.println("PASS AdSlotsTest: " + assertions + " assertions; home custom ad/include/ID slots, ordinary search and informational banners preserved, all/land/night configurations, persisted hidden ancestors after simulated child redisplay, root types, repeat application, legacy NearMe parent, sibling-reference layouts retain hierarchy, and invalid-layout preflight. Real Morphe ResourcePatchContext/Document with directory ResourceCoder. Resource DOM visibility model; no Android device test.");
    }
}
