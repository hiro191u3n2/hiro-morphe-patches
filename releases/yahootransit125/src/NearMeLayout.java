package app.hiro.yahootransit.patches;

import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.util.Document;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Patch-time only: preserve original ad Views and binding IDs behind unaddressed hidden parents. */
public final class NearMeLayout {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String LAYOUT = "fragment_search_result_list.xml";
    private static final String MARKER = "hiro_nearme_pr_hidden_v1";
    private static final String AD_MARKER = "hiro_ad_slot_hidden_v2";
    private static final String VIEW = "jp.co.yahoo.android.apps.transit.ui.compose.components.nearme.NearMeView";
    private static final String RESULTS = "jp.co.yahoo.android.apps.transit.ui.view.navi.list.SearchResultListView";
    private static final String BUTTONS = "jp.co.yahoo.android.apps.transit.ui.view.navi.SearchBtnView";
    private static final String REVERSE_LAYOUT = "view_search_btn.xml";
    private static final String REVERSE_TEXT = "発着を入れ替えて再検索";

    private NearMeLayout() {}

    public static void apply(Object value) {
        ResourcePatchContext context = (ResourcePatchContext) value;
        org.w3c.dom.Document manifest = read(context.get("AndroidManifest.xml", false));
        if (!"jp.co.yahoo.android.apps.transit".equals(manifest.getDocumentElement().getAttribute("package"))) {
            throw new IllegalStateException("Yahoo!乗換案内のパッケージではありません。");
        }
        // Ad compatibility remains structural. The reported 8.5.6 UI must also
        // provide the requested button; a missing match is never a success there.
        File res = context.get("res", false);
        File[] directories = res.listFiles();
        if (directories == null) throw new IllegalStateException("画面リソースがありません。");
        Arrays.sort(directories);
        List<String> layouts = new ArrayList<>();
        List<String> adLayouts = new ArrayList<>();
        List<String> searchLayouts = new ArrayList<>();
        Map<String, List<String>> strings = stringResources(directories);
        boolean reverseTextPresent = hasReverseString(strings);
        for (File directory : directories) {
            String name = directory.getName();
            if (directory.isDirectory() && (name.equals("layout") || name.startsWith("layout-"))
                    && new File(directory, LAYOUT).isFile()) {
                layouts.add("res/" + name + "/" + LAYOUT);
            }
            if (!directory.isDirectory() || !(name.equals("layout") || name.startsWith("layout-"))) continue;
            File[] files = directory.listFiles();
            if (files == null) throw new IllegalStateException("画面リソースを列挙できません。");
            Arrays.sort(files);
            for (File file : files) {
                if (!file.isFile() || !file.getName().endsWith(".xml")) continue;
                // Read-only preflight: do not reserialize unrelated layouts through Morphe Document.
                org.w3c.dom.Document document = read(file);
                if (hasAdTargets(document)) adLayouts.add("res/" + name + "/" + file.getName());
                if (file.getName().equals(REVERSE_LAYOUT)) searchLayouts.add("res/" + name + "/" + file.getName());
                if (hasLiteralReverseText(document)) reverseTextPresent = true;
            }
        }
        if (!layouts.contains("res/layout/" + LAYOUT)) {
            throw new IllegalStateException("検索結果の画面を確認できません。");
        }
        // Validate every configuration before changing any resource.
        for (String path : layouts) {
            target(read(context.get(path, false)));
        }
        Map<String, String> reverseTargets = new LinkedHashMap<>();
        boolean requireReverse = reverseTextPresent
                || "8.5.6".equals(android(manifest.getDocumentElement(), "versionName"));
        if (requireReverse) {
            for (String path : searchLayouts) {
                Element button = reverseButton(read(context.get(path, false)), strings);
                reverseTargets.put(path, android(button, "id"));
            }
            if (reverseTargets.isEmpty()) {
                throw new IllegalStateException("発着を入れ替えて再検索のButtonを確認できません。対象0件のため変更を中止します。");
            }
        }
        for (String path : layouts) {
            try (Document document = context.document(path)) { wrap(document); }
            try (Document document = context.document(path)) { verify(document); }
        }
        int shields = 0;
        for (String path : adLayouts) {
            try (Document document = context.document(path)) { shields += protectAds(document, reverseTargets.get(path)); }
            verifyAds(read(context.get(path, false)), reverseTargets.get(path));
        }
        for (Map.Entry<String, String> entry : reverseTargets.entrySet()) {
            try (Document document = context.document(entry.getKey())) {
                Element button = reverseButton(document, strings);
                if (!entry.getValue().equals(android(button, "id"))) {
                    throw new IllegalStateException("発着を入れ替えて再検索のButton IDが事前確認と異なります。");
                }
                // Keep merge children, binding identities and LayoutParams casts intact.
                hide(button);
                set(button, "enabled", "false");
                set(button, "longClickable", "false");
                set(button, "focusableInTouchMode", "false");
            }
            verifyReverseButton(reverseButton(read(context.get(entry.getKey(), false)), strings));
        }
        System.out.println("Yahoo ads v1.2.5: " + layouts.size() + " NearMe result layout(s); "
                + adLayouts.size() + " ad layout(s); " + shields + " protected ad slot(s). Original Views and IDs retained.");
        System.out.println("Yahoo reverse search v1.2.5: " + reverseTargets.size()
                + " button(s) hidden in place. Other search controls retained.");
    }

    private static Map<String, List<String>> stringResources(File[] directories) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (File directory : directories) {
            String name = directory.getName();
            if (!directory.isDirectory() || !(name.equals("values") || name.startsWith("values-"))) continue;
            File[] files = directory.listFiles();
            if (files == null) throw new IllegalStateException("文字列リソースを列挙できません。");
            Arrays.sort(files);
            for (File file : files) {
                if (!file.isFile() || !file.getName().endsWith(".xml")) continue;
                Element root = read(file).getDocumentElement();
                for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
                    if (!(node instanceof Element)) continue;
                    Element value = (Element) node;
                    if (!("string".equals(value.getTagName())
                            || ("item".equals(value.getTagName()) && "string".equals(value.getAttribute("type"))))) continue;
                    String key = value.getAttribute("name");
                    if (key.isEmpty()) continue;
                    List<String> definitions = result.get(key);
                    if (definitions == null) {
                        definitions = new ArrayList<>();
                        result.put(key, definitions);
                    }
                    String text = value.getTextContent();
                    if (!definitions.contains(text)) definitions.add(text);
                }
            }
        }
        return result;
    }

    private static boolean hasReverseString(Map<String, List<String>> strings) {
        for (List<String> definitions : strings.values()) {
            for (String value : definitions) if (REVERSE_TEXT.equals(displayText(value))) return true;
        }
        return false;
    }

    private static boolean hasLiteralReverseText(org.w3c.dom.Document document) {
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            if (REVERSE_TEXT.equals(displayText(android((Element) nodes.item(i), "text")))) return true;
        }
        return false;
    }

    private static String displayText(String raw) {
        String value = raw.trim();
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            value = value.substring(1, value.length() - 1);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 == value.length()) { out.append(c); continue; }
            char escaped = value.charAt(i + 1);
            if (escaped == 'u' && i + 5 < value.length()) {
                int code = 0;
                boolean valid = true;
                for (int n = i + 2; n <= i + 5; n++) {
                    int digit = Character.digit(value.charAt(n), 16);
                    if (digit < 0) { valid = false; break; }
                    code = (code << 4) | digit;
                }
                if (valid) { out.append((char) code); i += 5; continue; }
            }
            if (escaped == 'n' || escaped == 'r' || escaped == 't') {
                out.append(escaped == 'n' ? '\n' : escaped == 'r' ? '\r' : '\t'); i++;
            } else if (escaped == '\\' || escaped == '"' || escaped == '\'' || escaped == '@' || escaped == '?') {
                out.append(escaped); i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean reverseText(String raw, Map<String, List<String>> strings, Set<String> resolving) {
        String value = raw.trim();
        String prefix = "@string/";
        if (value.startsWith("@jp.co.yahoo.android.apps.transit:string/")) {
            prefix = "@jp.co.yahoo.android.apps.transit:string/";
        }
        if (!value.startsWith(prefix)) return REVERSE_TEXT.equals(displayText(value));
        String name = value.substring(prefix.length());
        List<String> definitions = strings.get(name);
        if (definitions == null) return false;
        // Alias branches from different locales may form a graph cycle. A
        // visited edge supplies no match; a real exact-label definition must
        // still be reachable. A pure cycle fails the required-button preflight.
        if (!resolving.add(name)) return false;
        boolean matches = false;
        try {
            // A localized translation still names the same control. Match the
            // resource identity if any locale resolves to the exact Japanese label.
            for (String definition : definitions) matches |= reverseText(definition, strings, resolving);
        } finally {
            resolving.remove(name);
        }
        return matches;
    }

    private static String idName(Element element) {
        String id = android(element, "id");
        int slash = id.lastIndexOf('/');
        return slash < 0 ? id : id.substring(slash + 1);
    }

    private static boolean protectedSearchId(Element element) {
        String id = idName(element);
        return id.equals("result_now_condition") || id.equals("result_back_condition")
                || id.equals("reverse_swap") || id.equals("reverseStartGoal") || id.equals("search_btn");
    }

    private static boolean leafButton(Element element) {
        String tag = element.getTagName();
        if (tag.equals("view")) tag = element.getAttribute("class");
        return elementCount(element) == 0 && (tag.equals("Button") || tag.equals("android.widget.Button")
                || tag.equals("androidx.appcompat.widget.AppCompatButton")
                || tag.equals("com.google.android.material.button.MaterialButton"));
    }

    private static Element reverseButton(org.w3c.dom.Document document, Map<String, List<String>> strings) {
        Element found = null, now = null, back = null;
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String id = idName(element);
            if (id.equals("result_now_condition")) {
                if (now != null) throw new IllegalStateException("現在時刻で再検索のButtonが重複しています。");
                now = element;
            }
            if (id.equals("result_back_condition")) {
                if (back != null) throw new IllegalStateException("検索条件を変更のButtonが重複しています。");
                back = element;
            }
            if (!reverseText(android(element, "text"), strings, new HashSet<String>())) continue;
            if (found != null || !leafButton(element) || android(element, "id").isEmpty() || protectedSearchId(element)) {
                throw new IllegalStateException("発着を入れ替えて再検索を単独の安全なButtonとして特定できません。");
            }
            found = element;
        }
        if (found == null || now == null || back == null || !leafButton(now) || !leafButton(back)
                || !(found.getParentNode() instanceof Element)
                || found.getParentNode() != now.getParentNode() || found.getParentNode() != back.getParentNode()) {
            throw new IllegalStateException("発着を入れ替えて再検索と前後2つの操作の構造を確認できません。変更を中止します。");
        }
        int sameId = 0;
        for (int i = 0; i < nodes.getLength(); i++) if (idName(found).equals(idName((Element) nodes.item(i)))) sameId++;
        if (sameId != 1) throw new IllegalStateException("発着を入れ替えて再検索のButton IDが重複しています。");
        return found;
    }

    private static void verifyReverseButton(Element button) {
        for (String attribute : new String[]{"layout_width", "layout_height", "minWidth", "minHeight", "maxWidth", "maxHeight",
                "layout_margin", "layout_marginHorizontal", "layout_marginVertical", "padding", "paddingHorizontal", "paddingVertical"}) {
            if (!"0dp".equals(android(button, attribute))) throw new IllegalStateException("再検索Buttonの寸法を確認できません。");
        }
        if (!"gone".equals(android(button, "visibility")) || !"0".equals(android(button, "alpha"))
                || !"false".equals(android(button, "enabled")) || !"false".equals(android(button, "clickable"))
                || !"false".equals(android(button, "longClickable")) || !"false".equals(android(button, "focusable"))
                || !"false".equals(android(button, "focusableInTouchMode"))
                || !"noHideDescendants".equals(android(button, "importantForAccessibility"))) {
            throw new IllegalStateException("再検索Buttonの非表示を確認できません。");
        }
    }

    private static org.w3c.dom.Document read(File file) {
        try (InputStream input = new FileInputStream(file)) {
            // Use the SDK's Android-compatible parser, without its file-backed write-on-close mode.
            return new Document(input);
        } catch (Exception e) {
            throw new IllegalStateException("画面リソースを確認できません: " + file.getName(), e);
        }
    }

    private static boolean adToken(String value) {
        String name = value.toLowerCase(Locale.ROOT);
        for (String token : name.split("[^a-z0-9]+")) {
            if (token.equals("ad") || token.equals("ads") || token.equals("advertisement")
                    || token.equals("ydn")) return true;
        }
        return false;
    }

    private static boolean isAdTarget(Element element) {
        String tag = element.getTagName();
        if (isShield(element) || tag.equals("data") || tag.equals("variable") || tag.equals("import")) return false;
        if (VIEW.equals(tag)) return true;
        if (knownFarePromotion(element)) return true;
        if (adToken(android(element, "id"))) return true;
        if ((tag.equals("include") || tag.equals("ViewStub") || tag.equals("android.view.ViewStub"))
                && (adToken(element.getAttribute("layout"))
                || adToken(android(element, "layout")))) return true;
        if (tag.equals("view")) tag = element.getAttribute("class");
        if ("jp.co.yahoo.android.ads.util.AdViewGroup".equals(tag)) return true;
        if (!tag.contains(".")) return false;
        String simple = tag.substring(tag.lastIndexOf('.') + 1);
        return simple.endsWith("AdView") || simple.endsWith("AdLayout") || simple.endsWith("AdContainer")
                || simple.endsWith("AdAutoSizeView") || simple.endsWith("AdvertisementView")
                || simple.endsWith("BannerAdView");
    }

    private static boolean knownFarePromotion(Element element) {
        // Confirmed PR-only block in the real 8.4.1/8.5.0 fare resource. Do not
        // classify general information/emergency banners by the word "banner".
        String id = android(element, "id");
        if (!("@id/promotion_banner_container".equals(id)
                || "@+id/promotion_banner_container".equals(id))) return false;
        NodeList children = element.getElementsByTagName("*");
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            String childId = android(child, "id");
            if (("@id/promotion_banner_pr".equals(childId) || "@+id/promotion_banner_pr".equals(childId))
                    && "@string/fare_free_pass_pr".equals(android(child, "text"))) return true;
        }
        return false;
    }

    private static boolean hasAdTargets(org.w3c.dom.Document document) {
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) if (isAdTarget((Element) nodes.item(i))) return true;
        return false;
    }

    private static boolean isShield(Element element) {
        String tag = android(element, "tag");
        return MARKER.equals(tag) || AD_MARKER.equals(tag);
    }

    private static boolean behindShield(Element element) {
        Node parent = element.getParentNode();
        while (parent instanceof Element) {
            if (isShield((Element) parent)) return true;
            parent = parent.getParentNode();
        }
        return false;
    }

    private static String wrapperType(Element parent) {
        // Preserve known LayoutParams and direct-child sibling relationships.
        // Constraint/Relative anchors cannot follow a View moved behind an ID-less parent.
        String tag = parent.getTagName();
        if (tag.equals("LinearLayout") || tag.equals("android.widget.LinearLayout")) return "LinearLayout";
        if (tag.equals("FrameLayout") || tag.equals("android.widget.FrameLayout")
                || tag.equals("ScrollView") || tag.equals("android.widget.ScrollView")
                || tag.equals("HorizontalScrollView") || tag.equals("android.widget.HorizontalScrollView")
                || tag.equals("androidx.core.widget.NestedScrollView")) return "FrameLayout";
        return null;
    }

    private static boolean bindingBoundary(Element element) {
        String ownTag = android(element, "tag");
        // Data Binding resolves normal IDs and binding_N tags recursively. A neutral,
        // ID-less parent does not change those indices. Included layout/merge roots
        // are different: their owner matches direct children and sibling sequences.
        if (ownTag.startsWith("layout/") || ownTag.startsWith("layout-")
                || "merge".equals(element.getTagName())) return true;
        if (!"include".equals(element.getTagName())) return false;
        for (Node node = element; node instanceof Element; node = node.getParentNode()) {
            Element view = (Element) node;
            String tag = android(view, "tag");
            if ("layout".equals(view.getTagName()) || tag.startsWith("layout/")
                    || tag.startsWith("layout-") || tag.startsWith("binding_")) return true;
        }
        return false;
    }

    private static int protectAds(org.w3c.dom.Document document) {
        return protectAds(document, null);
    }

    private static int protectAds(org.w3c.dom.Document document, String reverseId) {
        NodeList nodes = document.getElementsByTagName("*");
        List<Element> targets = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (isShield(element)) hide(element);
            if (isAdTarget(element) && !android(element, "id").equals(reverseId)) targets.add(element);
        }
        int count = 0;
        for (Element element : targets) {
            if (behindShield(element)) { count++; continue; }
            Node parentNode = element.getParentNode();
            // Preserve XML root types: generated bindings and custom inflation can cast them.
            if (!(parentNode instanceof Element) || "layout".equals(((Element) parentNode).getTagName())) {
                hide(element);
                continue;
            }
            Element parent = (Element) parentNode;
            String wrapper = wrapperType(parent);
            if (wrapper == null || bindingBoundary(element)) {
                // Keep existing hierarchy/casts/anchors intact in unverified parent types.
                hide(element);
                continue;
            }
            // An already matched ancestor is shielded first, so no normal siblings are hidden.
            Element hidden = document.createElement(wrapper);
            set(hidden, "tag", AD_MARKER);
            hide(hidden);
            parent.insertBefore(hidden, element);
            hidden.appendChild(element);
            count++;
        }
        return count;
    }

    private static void verifyAds(org.w3c.dom.Document document) {
        verifyAds(document, null);
    }

    private static void verifyAds(org.w3c.dom.Document document, String reverseId) {
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (isShield(element)) {
                if (!android(element, "id").isEmpty() || elementCount(element) != 1
                        || !"gone".equals(android(element, "visibility"))
                        || !"0dp".equals(android(element, "layout_height"))
                        || !"0dp".equals(android(element, "layout_width"))
                        || !"0".equals(android(element, "alpha"))) {
                    throw new IllegalStateException("広告の非表示枠を確認できません。");
                }
            }
            if (!isAdTarget(element) || behindShield(element) || android(element, "id").equals(reverseId)) continue;
            Node parent = element.getParentNode();
            if ((parent instanceof Element && !"layout".equals(((Element) parent).getTagName())
                    && wrapperType((Element) parent) != null && !bindingBoundary(element))
                    || !"gone".equals(android(element, "visibility"))) {
                throw new IllegalStateException("広告枠が保護されていません: " + element.getTagName());
            }
        }
    }

    private static Element target(org.w3c.dom.Document document) {
        NodeList views = document.getElementsByTagName(VIEW);
        if (views.getLength() != 1) throw new IllegalStateException("NearMeの広告枠を一意に特定できません。");
        Element view = (Element) views.item(0);
        if (android(view, "id").isEmpty()) throw new IllegalStateException("NearMeのView IDがありません。");
        if (!(view.getParentNode() instanceof Element)) throw new IllegalStateException("NearMeの親枠がありません。");
        Element parent = (Element) view.getParentNode();
        if (MARKER.equals(android(parent, "tag"))) {
            if (!"FrameLayout".equals(parent.getTagName()) || !android(parent, "id").isEmpty()
                    || elementCount(parent) != 1 || !(parent.getParentNode() instanceof Element)) {
                throw new IllegalStateException("既存のNearMe非表示枠が想定と異なります。");
            }
            parent = (Element) parent.getParentNode();
        }
        if (!"LinearLayout".equals(parent.getTagName()) || directCount(parent, RESULTS) != 1
                || directCount(parent, BUTTONS) != 1) {
            throw new IllegalStateException("検索結果・再検索ボタンの構造が想定と異なります。");
        }
        return view;
    }

    static void wrap(org.w3c.dom.Document document) {
        Element view = target(document);
        Element parent = (Element) view.getParentNode();
        if (MARKER.equals(android(parent, "tag"))) { hide(parent); return; }
        Element hidden = document.createElement("FrameLayout");
        set(hidden, "tag", MARKER);
        hide(hidden);
        parent.insertBefore(hidden, view);
        hidden.appendChild(view);
    }

    private static void verify(org.w3c.dom.Document document) {
        Element parent = (Element) target(document).getParentNode();
        if (!MARKER.equals(android(parent, "tag")) || !"gone".equals(android(parent, "visibility"))
                || !"0dp".equals(android(parent, "layout_height")) || !"0dp".equals(android(parent, "layout_width"))
                || !"0dp".equals(android(parent, "layout_margin")) || !"0".equals(android(parent, "alpha"))) {
            throw new IllegalStateException("NearMe非表示枠の保存を確認できません。");
        }
    }

    private static int directCount(Element parent, String name) {
        int count = 0;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && name.equals(((Element) node).getTagName())) count++;
        }
        return count;
    }

    private static int elementCount(Element parent) {
        int count = 0;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) count++;
        }
        return count;
    }

    private static String android(Element element, String name) {
        String value = element.getAttributeNS(ANDROID, name);
        return value.isEmpty() ? element.getAttribute("android:" + name) : value;
    }

    private static void set(Element element, String name, String value) {
        // Morphe also parses existing attributes without namespace awareness.
        // Remove both spellings before writing so a repeated patch never duplicates an attribute.
        element.removeAttribute("android:" + name);
        element.removeAttributeNS(ANDROID, name);
        element.setAttributeNS(ANDROID, "android:" + name, value);
    }

    private static void hide(Element element) {
        for (String name : new String[]{"layout_width", "layout_height", "minWidth", "minHeight",
                "maxWidth", "maxHeight", "layout_marginHorizontal", "layout_marginVertical",
                "layout_margin", "layout_marginLeft", "layout_marginTop", "layout_marginRight", "layout_marginBottom",
                "layout_marginStart", "layout_marginEnd", "padding", "paddingLeft", "paddingTop", "paddingRight",
                "paddingBottom", "paddingStart", "paddingEnd", "paddingHorizontal", "paddingVertical"}) set(element, name, "0dp");
        set(element, "visibility", "gone");
        set(element, "layout_weight", "0");
        set(element, "alpha", "0");
        set(element, "clipChildren", "true");
        set(element, "clipToPadding", "true");
        set(element, "clickable", "false");
        set(element, "focusable", "false");
        set(element, "importantForAccessibility", "noHideDescendants");
    }
}
