package app.hiro.yahootransit.patches;

import app.morphe.patcher.util.Document;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Calls the unchanged legacy resource entrypoint after translating the actual shipped DEX to JVM. */
public final class FullPatchExecutionTest {
    static int checks;
    static final String NS = "http://schemas.android.com/apk/res/android";
    static final String MARKER = "hiro_ad_slot_hidden_v2";
    static void check(boolean condition, String reason) { checks++; if (!condition) throw new AssertionError(reason); }
    static Element id(org.w3c.dom.Document document, String name) {
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            if (name.equals(NearMeLayoutTest.attribute(e, "id"))) return e;
        }
        throw new AssertionError("Missing original View ID " + name);
    }
    static void execute(Path directory) throws Exception {
        Class<?> patch = Class.forName("jp.hiro.patches.yahootransit.YahooTransitAdPatch");
        Method method = patch.getDeclaredMethod("collapseLayouts", Object.class);
        method.setAccessible(true);
        try { method.invoke(null, NearMeLayoutTest.context(directory)); }
        catch (InvocationTargetException e) { throw new AssertionError("Shipped resource entrypoint failed", e.getCause()); }
    }
    static void verifyHome(Path home, String formBefore) throws Exception {
        try (Document document = new Document(home.toFile())) {
            check(formBefore.equals(NearMeLayoutTest.fingerprint(id(document, "@id/search_form"))), "normal search form preserved");
            check("@id/home".equals(NearMeLayoutTest.attribute(document.getDocumentElement(), "id")), "home root preserved");
            for (String name : new String[]{"@id/top_slot", "@id/bottom_slot"}) {
                Element ad = id(document, name), parent = (Element) ad.getParentNode();
                check(MARKER.equals(NearMeLayoutTest.attribute(parent, "tag")), "outer ad parent installed through actual resource entrypoint");
                check("LinearLayout".equals(parent.getTagName()), "home original LayoutParams family retained");
                check(NearMeLayoutTest.attribute(parent, "id").isEmpty(), "parent cannot replace application binding ID");
                check("gone".equals(NearMeLayoutTest.attribute(parent, "visibility")), "outer parent persists as gone");
                check("0dp".equals(NearMeLayoutTest.attribute(parent, "layout_height")), "outer parent persists with no height");
                check("0".equals(NearMeLayoutTest.attribute(parent, "alpha")), "outer parent stays transparent");
                check(parent.getParentNode() == document.getDocumentElement(), "no duplicate nested parent");
            }
        }
    }
    static void set(Element e, String name, String value) {
        e.removeAttribute("android:" + name);
        e.removeAttributeNS(NS, name);
        e.setAttributeNS(NS, "android:" + name, value);
    }
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]);
        Files.createDirectories(root);
        Path directory = NearMeLayoutTest.fixture(root, "complete-resource-entry", "8.5.0", "@id/near_me");
        Path home = directory.resolve("res/layout/fragment_home.xml");
        NearMeLayoutTest.write(home, "<LinearLayout xmlns:android=\"" + NS + "\" android:id=\"@id/home\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
            + "<jp.co.yahoo.android.apps.transit.ui.view.ad.NaviSearchTopAdView android:id=\"@id/top_slot\" android:layout_width=\"match_parent\" android:layout_height=\"90dp\"/>"
            + "<LinearLayout android:id=\"@id/search_form\"><EditText android:id=\"@id/departure\"/><EditText android:id=\"@id/destination\"/><Button android:id=\"@id/search_button\" android:text=\"検索\"/></LinearLayout>"
            + "<include android:id=\"@id/bottom_slot\" layout=\"@layout/view_navi_search_bottom_ad\" android:layout_width=\"match_parent\" android:layout_height=\"250dp\"/>"
            + "</LinearLayout>");
        Path legacyAd = directory.resolve("res/layout/view_navi_search_bottom_ad.xml");
        NearMeLayoutTest.write(legacyAd, "<FrameLayout xmlns:android=\"" + NS + "\" android:id=\"@id/creative_root\" android:layout_width=\"match_parent\" android:layout_height=\"250dp\"><WebView android:id=\"@id/creative\"/></FrameLayout>");
        String formBefore;
        try (Document document = new Document(home.toFile())) { formBefore = NearMeLayoutTest.fingerprint(id(document, "@id/search_form")); }
        execute(directory);
        if (args.length > 1 && "baseline".equals(args[1])) {
            try (Document document = new Document(home.toFile())) {
                check(id(document, "@id/top_slot").getParentNode() == document.getDocumentElement(), "baseline lacks home top shield");
                check(id(document, "@id/bottom_slot").getParentNode() == document.getDocumentElement(), "baseline lacks home bottom shield");
            }
            System.out.println("PASS FullPatchExecutionTest baseline: home advertisement parent protection absent as expected.");
            return;
        }
        verifyHome(home, formBefore);
        NearMeLayoutTest.verify(directory.resolve("res/layout/fragment_search_result_list.xml"), "@id/near_me");
        try (Document document = new Document(legacyAd.toFile())) {
            check("FrameLayout".equals(document.getDocumentElement().getTagName()), "legacy ad resource root type retained");
            check("gone".equals(NearMeLayoutTest.attribute(document.getDocumentElement(), "visibility")), "legacy collapse still executes before new helper");
        }
        try (Document document = new Document(home.toFile())) {
            for (String name : new String[]{"@id/top_slot", "@id/bottom_slot"}) {
                Element ad = id(document, name);
                set(ad, "visibility", "visible"); set(ad, "layout_height", "300dp"); set(ad, "layout_width", "match_parent"); set(ad, "alpha", "1");
            }
        }
        verifyHome(home, formBefore);
        execute(directory);
        verifyHome(home, formBefore);
        System.out.println("PASS FullPatchExecutionTest: " + checks + " assertions; actual shipped resource entrypoint, legacy collapse plus new home/NearMe protection, original search form and IDs, persisted redisplay protection and repeat execution. Generated resource fixtures; no APK or Android device test.");
    }
}
