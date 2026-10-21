package app.hiro.yahootransit.patches;

import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.util.Document;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Patch-time only: keep the existing ad View and its binding ID behind a hidden parent. */
public final class NearMeLayout {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String LAYOUT = "fragment_search_result_list.xml";
    private static final String MARKER = "hiro_nearme_pr_hidden_v1";
    private static final String VIEW = "jp.co.yahoo.android.apps.transit.ui.compose.components.nearme.NearMeView";
    private static final String RESULTS = "jp.co.yahoo.android.apps.transit.ui.view.navi.list.SearchResultListView";
    private static final String BUTTONS = "jp.co.yahoo.android.apps.transit.ui.view.navi.SearchBtnView";

    private NearMeLayout() {}

    public static void apply(Object value) {
        ResourcePatchContext context = (ResourcePatchContext) value;
        try (Document manifest = context.document("AndroidManifest.xml")) {
            if (!"jp.co.yahoo.android.apps.transit".equals(manifest.getDocumentElement().getAttribute("package"))) {
                throw new IllegalStateException("Yahoo!乗換案内のパッケージではありません。");
            }
        }
        // Versions are deliberately not used: class and screen topology must match instead.
        File res = context.get("res", false);
        File[] directories = res.listFiles();
        if (directories == null) throw new IllegalStateException("画面リソースがありません。");
        Arrays.sort(directories);
        List<String> layouts = new ArrayList<>();
        for (File directory : directories) {
            String name = directory.getName();
            if (directory.isDirectory() && (name.equals("layout") || name.startsWith("layout-"))
                    && new File(directory, LAYOUT).isFile()) {
                layouts.add("res/" + name + "/" + LAYOUT);
            }
        }
        if (!layouts.contains("res/layout/" + LAYOUT)) {
            throw new IllegalStateException("検索結果の画面を確認できません。");
        }
        // Validate every configuration before changing any resource.
        for (String path : layouts) {
            try (Document document = context.document(path)) { target(document); }
        }
        for (String path : layouts) {
            try (Document document = context.document(path)) { wrap(document); }
            try (Document document = context.document(path)) { verify(document); }
        }
        System.out.println("NearMe: hidden search-result parent written and verified in " + layouts.size() + " layout(s)");
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
