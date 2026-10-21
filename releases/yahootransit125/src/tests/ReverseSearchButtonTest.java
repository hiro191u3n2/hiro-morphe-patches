package app.hiro.yahootransit.patches;

import app.morphe.patcher.util.Document;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Authored XML fixtures. Post-load visibility is a DOM model, not an Android runtime test. */
public final class ReverseSearchButtonTest {
    private static final String NS = "http://schemas.android.com/apk/res/android";
    private static final String LABEL = "発着を入れ替えて再検索";
    private static final String NOW = "result_now_condition";
    private static final String BACK = "result_back_condition";
    private static final String[] PROTECTED = {NOW, BACK, "reverse_swap", "reverseStartGoal", "search_btn"};
    private static final String[] ZERO = {
        "layout_width", "layout_height", "minWidth", "minHeight", "maxWidth", "maxHeight",
        "layout_margin", "layout_marginLeft", "layout_marginTop", "layout_marginRight", "layout_marginBottom",
        "layout_marginStart", "layout_marginEnd", "layout_marginHorizontal", "layout_marginVertical",
        "padding", "paddingLeft", "paddingTop", "paddingRight", "paddingBottom", "paddingStart", "paddingEnd",
        "paddingHorizontal", "paddingVertical"
    };
    private static int checks;

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static String attr(Element element, String name) {
        return NearMeLayoutTest.attribute(element, name);
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String button(String tag, String id, String text) {
        String classAttribute = tag.startsWith("view:") ? " class=\"" + tag.substring(5) + "\"" : "";
        if (tag.startsWith("view:")) tag = "view";
        return "<" + tag + classAttribute + (id.isEmpty() ? "" : " android:id=\"@id/" + id + "\"")
                + " android:tag=\"button_identity_" + id + "\" android:text=\"" + xml(text) + "\""
                + " android:layout_width=\"match_parent\" android:layout_height=\"48dp\""
                + " android:layout_margin=\"8dp\" android:padding=\"12dp\" android:visibility=\"visible\""
                + " android:alpha=\"1\" android:enabled=\"true\" android:clickable=\"true\""
                + " android:longClickable=\"true\" android:focusable=\"true\""
                + " android:focusableInTouchMode=\"true\" android:onClick=\"retainedListener\"/>";
    }

    private static String controls(String target) {
        return "<merge xmlns:android=\"" + NS + "\">"
                + button("Button", NOW, "現在時刻で再検索") + target
                + button("Button", BACK, "検索条件を変更") + "</merge>";
    }

    private static Path fixture(Path root, String name, String version) throws Exception {
        return NearMeLayoutTest.fixture(root, name, version, "@id/near_me");
    }

    private static Path controls(Path directory, String config, String target) throws Exception {
        Path file = directory.resolve("res/" + config + "/view_search_btn.xml");
        NearMeLayoutTest.write(file, controls(target));
        return file;
    }

    private static org.w3c.dom.Document read(Path file) throws Exception {
        try (InputStream source = Files.newInputStream(file)) { return new Document(source); }
    }

    private static Element id(org.w3c.dom.Document document, String name) {
        NodeList nodes = document.getElementsByTagName("*");
        Element found = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String value = attr(element, "id");
            if (!(value.equals("@id/" + name) || value.equals("@+id/" + name))) continue;
            check(found == null, "unique ID " + name);
            found = element;
        }
        check(found != null, "retained ID " + name);
        return found;
    }

    private static String fingerprint(Path file) throws Exception {
        return NearMeLayoutTest.fingerprint(read(file).getDocumentElement());
    }

    private static List<String> childOrder(Element parent) {
        List<String> result = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) result.add(((Element) child).getTagName() + ":" + attr((Element) child, "id"));
        }
        return result;
    }

    private static Map<String, byte[]> files(Path directory) throws Exception {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path file : (Iterable<Path>) paths.filter(Files::isRegularFile).sorted()::iterator) {
                result.put(directory.relativize(file).toString(), Files.readAllBytes(file));
            }
        }
        return result;
    }

    private static void unchanged(Map<String, byte[]> before, Path directory) throws Exception {
        Map<String, byte[]> after = files(directory);
        check(before.keySet().equals(after.keySet()), "rejection creates no resource files");
        for (String name : before.keySet()) check(Arrays.equals(before.get(name), after.get(name)), "no partial write: " + name);
    }

    private static void reject(Path directory, String reason) throws Exception {
        Map<String, byte[]> before = files(directory);
        boolean rejected = false;
        try { NearMeLayout.apply(NearMeLayoutTest.context(directory)); }
        catch (IllegalStateException expected) {
            rejected = true;
            check(expected.getMessage() != null && !expected.getMessage().isEmpty(), "explicit rejection reason");
        }
        check(rejected, "reject " + reason);
        unchanged(before, directory);
    }

    private static void set(Element element, String name, String value) {
        element.removeAttribute("android:" + name);
        element.removeAttributeNS(NS, name);
        element.setAttributeNS(NS, "android:" + name, value);
    }

    private static boolean visibleModel(Element element) {
        return !"gone".equals(attr(element, "visibility")) && !"invisible".equals(attr(element, "visibility"))
                && !"0".equals(attr(element, "alpha")) && !"0dp".equals(attr(element, "layout_width"))
                && !"0dp".equals(attr(element, "layout_height"));
    }

    private static void verifyControl(Path file, String targetId, org.w3c.dom.Document before) throws Exception {
        org.w3c.dom.Document after = read(file);
        Element oldTarget = id(before, targetId), target = id(after, targetId);
        check(oldTarget.getTagName().equals(target.getTagName()), "original Button class retained");
        check(oldTarget.getAttribute("class").equals(target.getAttribute("class")), "explicit View class retained");
        for (String name : new String[]{"id", "tag", "text", "onClick"}) {
            check(attr(oldTarget, name).equals(attr(target, name)), "original " + name + " retained");
        }
        Element oldParent = (Element) oldTarget.getParentNode(), parent = (Element) target.getParentNode();
        check(oldParent.getTagName().equals(parent.getTagName()), "merge/binding parent type retained");
        check(childOrder(oldParent).equals(childOrder(parent)), "all original direct children and order retained");
        check(attr(oldParent, "tag").equals(attr(parent, "tag")), "root binding tag retained");
        for (String name : new String[]{NOW, BACK}) {
            check(NearMeLayoutTest.fingerprint(id(before, name)).equals(NearMeLayoutTest.fingerprint(id(after, name))),
                    "ordinary control unchanged: " + name);
            check(id(after, name).getParentNode() == parent, "ordinary control keeps the same parent");
            check(visibleModel(id(after, name)), "ordinary control remains visible in model");
        }
        for (String name : ZERO) check("0dp".equals(attr(target, name)), "collapsed " + name);
        check("gone".equals(attr(target, "visibility")), "target gone");
        check("0".equals(attr(target, "alpha")), "target alpha zero");
        check("0".equals(attr(target, "layout_weight")), "target has no weighted space");
        for (String name : new String[]{"enabled", "clickable", "longClickable", "focusable", "focusableInTouchMode"}) {
            check("false".equals(attr(target, name)), "target interaction disabled: " + name);
        }
        check("noHideDescendants".equals(attr(target, "importantForAccessibility")), "target hidden from accessibility");
        check(!visibleModel(target), "target initially absent in model");
        set(target, "visibility", "visible");
        check(!visibleModel(target), "setVisibility(VISIBLE) alone cannot undo zero dimensions and alpha");
        check("0dp".equals(attr(target, "layout_height")) && "0".equals(attr(target, "alpha")), "post-load guards retained");
    }

    private static String unicode(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) out.append('\\').append('u').append(String.format("%04x", (int) text.charAt(i)));
        return out.toString();
    }

    private static void literalAndScope(Path root) throws Exception {
        Path directory = fixture(root, "literal-and-scope", "8.5.6");
        Path file = controls(directory, "layout", button("Button", "arbitrary_target", LABEL));
        org.w3c.dom.Document before = read(file);
        Path other = directory.resolve("res/layout/other_screen.xml");
        StringBuilder foreign = new StringBuilder("<LinearLayout xmlns:android=\"" + NS + "\">");
        for (String protectedId : PROTECTED) foreign.append(button("Button", protectedId, LABEL));
        foreign.append(button("Button", "unrelated_same_caption", LABEL)).append("</LinearLayout>");
        NearMeLayoutTest.write(other, foreign.toString());
        byte[] otherBefore = Files.readAllBytes(other);
        Path home = directory.resolve("res/layout/fragment_navi_search.xml");
        NearMeLayoutTest.write(home, "<LinearLayout xmlns:android=\"" + NS + "\" android:tag=\"layout/fragment_navi_search_0\">"
                + "<jp.co.yahoo.android.apps.transit.ad.NaviSearchTopAdView android:id=\"@id/top_ad_view\"/>"
                + button("Button", "search_button", "検索")
                + "<jp.co.yahoo.android.apps.transit.ad.NaviSearchBottomAdView android:id=\"@id/bottom_ad_view\"/></LinearLayout>");
        NearMeLayout.apply(NearMeLayoutTest.context(directory));
        verifyControl(file, "arbitrary_target", before);
        check(Arrays.equals(otherBefore, Files.readAllBytes(other)), "same-caption other screen and input swaps are byte-identical");
        org.w3c.dom.Document homeAfter = read(home);
        for (String name : new String[]{"top_ad_view", "bottom_ad_view"}) {
            check("hiro_ad_slot_hidden_v2".equals(attr((Element) id(homeAfter, name).getParentNode(), "tag")), "existing ad shield preserved");
        }
        check(visibleModel(id(homeAfter, "search_button")), "normal home search remains visible");
        org.w3c.dom.Document result = read(directory.resolve("res/layout/fragment_search_result_list.xml"));
        check("hiro_nearme_pr_hidden_v1".equals(attr((Element) id(result, "near_me").getParentNode(), "tag")), "NearMe shield preserved");
        check(id(result, "buttons").getParentNode() == result.getDocumentElement(), "SearchBtnView host keeps its direct parent");
        String once = fingerprint(file), homeOnce = fingerprint(home);
        NearMeLayout.apply(NearMeLayoutTest.context(directory));
        check(once.equals(fingerprint(file)), "target output is semantically idempotent");
        check(homeOnce.equals(fingerprint(home)), "existing advertisement output is idempotent");
        verifyControl(file, "arbitrary_target", before);
    }

    private static void aliasesAndConfigurations(Path root) throws Exception {
        Path directory = fixture(root, "aliases-and-configurations", "8.5.6");
        NearMeLayoutTest.write(directory.resolve("res/values-ja/strings.xml"),
                "<resources><string name=\"exact_label\">" + unicode(LABEL) + "</string></resources>");
        NearMeLayoutTest.write(directory.resolve("res/values-en/strings.xml"),
                "<resources><string name=\"exact_label\">Search again with origin and destination reversed</string></resources>");
        NearMeLayoutTest.write(directory.resolve("res/values/aliases.xml"),
                "<resources><item type=\"string\" name=\"alias_two\">@string/exact_label</item>"
                + "<string name=\"alias_one\">@string/alias_two</string></resources>");
        String[] configs = {"layout", "layout-land", "layout-night"};
        String[] tags = {"android.widget.Button", "view:androidx.appcompat.widget.AppCompatButton", "com.google.android.material.button.MaterialButton"};
        List<org.w3c.dom.Document> before = new ArrayList<>();
        for (int i = 0; i < configs.length; i++) {
            Path file = controls(directory, configs[i], button(tags[i], "target_" + i, "@string/alias_one"));
            if (i == 2) NearMeLayoutTest.write(file, Files.readString(file).replace("<merge ", "<LinearLayout android:tag=\"layout/view_search_btn_0\" ")
                    .replace("</merge>", "</LinearLayout>").replace("button_identity_target_2", "binding_7"));
            before.add(read(file));
        }
        Map<String, byte[]> values = files(directory.resolve("res/values-ja"));
        NearMeLayout.apply(NearMeLayoutTest.context(directory));
        for (int i = 0; i < configs.length; i++) verifyControl(directory.resolve("res/" + configs[i] + "/view_search_btn.xml"), "target_" + i, before.get(i));
        unchanged(values, directory.resolve("res/values-ja"));
        Map<String, String> once = new LinkedHashMap<>();
        for (String config : configs) once.put(config, fingerprint(directory.resolve("res/" + config + "/view_search_btn.xml")));
        NearMeLayout.apply(NearMeLayoutTest.context(directory));
        for (String config : configs) check(once.get(config).equals(fingerprint(directory.resolve("res/" + config + "/view_search_btn.xml"))), "variant idempotence " + config);
        directory = fixture(root, "escaped-literal", "8.5.6");
        Path file = controls(directory, "layout", button("Button", "target", unicode(LABEL)));
        org.w3c.dom.Document original = read(file);
        NearMeLayout.apply(NearMeLayoutTest.context(directory));
        verifyControl(file, "target", original);
        directory = fixture(root, "ad-looking-arbitrary-id", "8.5.6");
        file = controls(directory, "layout", button("Button", "arbitrary_ad_action", LABEL));
        NearMeLayoutTest.write(file, Files.readString(file).replace("<merge ", "<LinearLayout ").replace("</merge>", "</LinearLayout>"));
        original = read(file);
        NearMeLayout.apply(NearMeLayoutTest.context(directory));
        verifyControl(file, "arbitrary_ad_action", original);
        directory = fixture(root, "translated-alias-graph-cycle", "8.5.6");
        NearMeLayoutTest.write(directory.resolve("res/values/strings.xml"),
                "<resources><string name=\"a\">@string/b</string><string name=\"b\">" + LABEL + "</string></resources>");
        NearMeLayoutTest.write(directory.resolve("res/values-en/strings.xml"),
                "<resources><string name=\"a\">Search again with origin and destination reversed</string>"
                + "<string name=\"b\">@string/a</string></resources>");
        file = controls(directory, "layout", button("Button", "target", "@string/a"));
        original = read(file);
        NearMeLayout.apply(NearMeLayoutTest.context(directory));
        verifyControl(file, "target", original);
        check("gone".equals(attr(id(read(file), "target"), "visibility")),
                "locale alias graph cycle does not reject reachable exact Japanese label");
    }

    private static void rejectedStructures(Path root) throws Exception {
        int index = 0;
        for (String protectedId : PROTECTED) {
            Path directory = fixture(root, "protected-" + index++, "8.5.6");
            controls(directory, "layout", button("Button", protectedId, LABEL));
            reject(directory, "protected ID " + protectedId);
        }
        for (String tag : new String[]{"LinearLayout", "TextView", "jp.co.yahoo.android.apps.transit.ui.view.navi.SearchBtnView"}) {
            Path directory = fixture(root, "unsafe-class-" + index++, "8.5.6");
            controls(directory, "layout", button(tag, "unsafe", LABEL));
            reject(directory, "non-Button class " + tag);
        }
        Path directory = fixture(root, "non-leaf-button", "8.5.6");
        controls(directory, "layout", "<Button android:id=\"@id/target\" android:text=\"" + LABEL + "\"><TextView/></Button>");
        reject(directory, "Button with children");
        directory = fixture(root, "missing-id", "8.5.6");
        controls(directory, "layout", button("Button", "", LABEL));
        reject(directory, "missing ID");
        directory = fixture(root, "duplicate-target", "8.5.6");
        controls(directory, "layout", button("Button", "one", LABEL) + button("Button", "two", LABEL));
        reject(directory, "ambiguous exact label");
        directory = fixture(root, "duplicate-target-id", "8.5.6");
        controls(directory, "layout", button("Button", "one", LABEL) + button("Button", "one", "other"));
        reject(directory, "ambiguous ID");
        for (String required : new String[]{NOW, BACK}) {
            directory = fixture(root, "missing-" + required, "8.5.6");
            Path file = controls(directory, "layout", button("Button", "one", LABEL));
            NearMeLayoutTest.write(file, Files.readString(file).replace("@id/" + required, "@id/renamed_normal_control"));
            reject(directory, "missing normal operation " + required);
            directory = fixture(root, "duplicate-" + required, "8.5.6");
            controls(directory, "layout", button("Button", "one", LABEL) + button("Button", required, "other"));
            reject(directory, "duplicate normal operation " + required);
        }
        directory = fixture(root, "different-parent", "8.5.6");
        controls(directory, "layout", "<FrameLayout>" + button("Button", "one", LABEL) + "</FrameLayout>");
        reject(directory, "target outside the common operation row");
        directory = fixture(root, "bad-later-variant", "8.5.6");
        controls(directory, "layout", button("Button", "one", LABEL));
        controls(directory, "layout-land", button("Button", "one", LABEL));
        controls(directory, "layout-night", button("Button", "one", LABEL) + button("Button", "two", LABEL));
        reject(directory, "later configuration is ambiguous, with no earlier configuration writes");
        directory = fixture(root, "missing-later-variant", "8.5.6");
        controls(directory, "layout", button("Button", "one", LABEL));
        controls(directory, "layout-land", "");
        reject(directory, "later configuration lacks requested control");
    }

    private static void zeroMatchRules(Path root) throws Exception {
        Path directory = fixture(root, "reported-version-no-layout", "8.5.6");
        reject(directory, "reported 8.5.6 has zero matches");
        directory = fixture(root, "reported-version-two-controls", "8.5.6");
        controls(directory, "layout", "");
        reject(directory, "8.5.6 operation absent");
        directory = fixture(root, "unreferenced-exact-string", "8.5.0");
        NearMeLayoutTest.write(directory.resolve("res/values/strings.xml"), "<resources><string name=\"unused\">" + LABEL + "</string></resources>");
        controls(directory, "layout", "");
        reject(directory, "exact string exists but no safe Button");
        directory = fixture(root, "other-layout-only", "8.5.0");
        NearMeLayoutTest.write(directory.resolve("res/layout/other.xml"), "<LinearLayout xmlns:android=\"" + NS + "\">" + button("Button", "one", LABEL) + "</LinearLayout>");
        reject(directory, "same-caption other screen is not a target");
        directory = fixture(root, "alias-cycle", "8.5.6");
        NearMeLayoutTest.write(directory.resolve("res/values/strings.xml"), "<resources><string name=\"a\">@string/b</string><string name=\"b\">@string/a</string></resources>");
        controls(directory, "layout", button("Button", "target", "@string/a"));
        reject(directory, "alias cycle does not invent an exact match");
        for (String version : new String[]{"8.4.1", "8.5.0", "8.6.0", "9.0.0"}) {
            directory = fixture(root, "older-or-unknown-no-label-" + version, version);
            Path file = controls(directory, "layout", button("Button", "reverse_named", "発着を入れ替えて検索"));
            byte[] before = Files.readAllBytes(file);
            NearMeLayout.apply(NearMeLayoutTest.context(directory));
            check(Arrays.equals(before, Files.readAllBytes(file)), "non-exact label and other controls remain byte-identical for " + version);
            org.w3c.dom.Document result = read(directory.resolve("res/layout/fragment_search_result_list.xml"));
            check("hiro_nearme_pr_hidden_v1".equals(attr((Element) id(result, "near_me").getParentNode(), "tag")), "existing ads continue without new label");
        }
    }

    public static void main(String[] args) throws Exception {
        check(args.length == 1, "Expected fixture output directory");
        Path root = Paths.get(args[0]);
        Files.createDirectories(root);
        literalAndScope(root);
        aliasesAndConfigurations(root);
        rejectedStructures(root);
        zeroMatchRules(root);
        System.out.println("PASS ReverseSearchButtonTest: " + checks
                + " assertions; exact literal/resource/Unicode/alias text, translated locales, all layout configurations,"
                + " protected controls and input swaps, unchanged merge/binding topology, preflight rejection,"
                + " zero-match guard and repeat execution. Visibility redisplay is a DOM model, not an Android device test.");
    }
}
