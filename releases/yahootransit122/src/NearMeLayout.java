package app.hiro.yahootransit.patches;

import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.util.Document;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

    private NearMeLayout() {}

    public static void apply(Object value) {
        ResourcePatchContext context = (ResourcePatchContext) value;
        org.w3c.dom.Document manifest = read(context.get("AndroidManifest.xml", false));
        if (!"jp.co.yahoo.android.apps.transit".equals(manifest.getDocumentElement().getAttribute("package"))) {
            throw new IllegalStateException("Yahoo!乗換案内のパッケージではありません。");
        }
        // Versions are deliberately not used: class and screen topology must match instead.
        File res = context.get("res", false);
        File[] directories = res.listFiles();
        if (directories == null) throw new IllegalStateException("画面リソースがありません。");
        Arrays.sort(directories);
        List<String> layouts = new ArrayList<>();
        List<String> adLayouts = new ArrayList<>();
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
            }
        }
        if (!layouts.contains("res/layout/" + LAYOUT)) {
            throw new IllegalStateException("検索結果の画面を確認できません。");
        }
        // Validate every configuration before changing any resource.
        for (String path : layouts) {
            target(read(context.get(path, false)));
        }
        for (String path : layouts) {
            try (Document document = context.document(path)) { wrap(document); }
            try (Document document = context.document(path)) { verify(document); }
        }
        int shields = 0;
        for (String path : adLayouts) {
            try (Document document = context.document(path)) { shields += protectAds(document); }
            verifyAds(read(context.get(path, false)));
        }
        System.out.println("Yahoo ads v1.2.2: " + layouts.size() + " NearMe result layout(s); "
                + adLayouts.size() + " ad layout(s); " + shields + " protected ad slot(s). Original Views and IDs retained.");
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
        if (adToken(android(element, "id"))) return true;
        if ((tag.equals("include") || tag.equals("ViewStub") || tag.equals("android.view.ViewStub"))
                && (adToken(element.getAttribute("layout"))
                || adToken(android(element, "layout")))) return true;
        if (tag.equals("view")) tag = element.getAttribute("class");
        if (!tag.contains(".")) return false;
        String simple = tag.substring(tag.lastIndexOf('.') + 1);
        return simple.endsWith("AdView") || simple.endsWith("AdLayout") || simple.endsWith("AdContainer")
                || simple.endsWith("AdAutoSizeView") || simple.endsWith("AdvertisementView")
                || simple.endsWith("BannerAdView");
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

    private static boolean bindingHierarchy(Element element) {
        for (Node node = element; node instanceof Element; node = node.getParentNode()) {
            Element view = (Element) node;
            String tag = android(view, "tag");
            if ("layout".equals(view.getTagName()) || tag.startsWith("layout/")
                    || tag.startsWith("layout-") || tag.startsWith("binding_")) return true;
        }
        return false;
    }

    private static int protectAds(org.w3c.dom.Document document) {
        NodeList nodes = document.getElementsByTagName("*");
        List<Element> targets = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (isShield(element)) hide(element);
            if (isAdTarget(element)) targets.add(element);
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
            if (wrapper == null || bindingHierarchy(element)) {
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
            if (!isAdTarget(element) || behindShield(element)) continue;
            Node parent = element.getParentNode();
            if ((parent instanceof Element && !"layout".equals(((Element) parent).getTagName())
                    && wrapperType((Element) parent) != null && !bindingHierarchy(element))
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
                "layout_margin", "layout_marginLeft", "layout_marginTop", "layout_marginRight", "layout_marginBottom",
                "layout_marginStart", "layout_marginEnd", "padding", "paddingLeft", "paddingTop", "paddingRight",
                "paddingBottom", "paddingStart", "paddingEnd"}) set(element, name, "0dp");
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
