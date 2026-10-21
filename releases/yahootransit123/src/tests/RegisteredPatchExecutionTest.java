package app.hiro.yahootransit.patches;

import app.morphe.engine.patches.PatchBundleLoader;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.ResourcePatch;
import app.morphe.patcher.util.Document;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.w3c.dom.Element;

/** Public registered execution through the production loader. Generated XML, no original APK. */
public final class RegisteredPatchExecutionTest {
    private static int assertions;
    private static void check(boolean value, String why) {
        assertions++;
        if (!value) throw new AssertionError(why);
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: RegisteredPatchExecutionTest shipped-dex-jvm-bridge.mpp fixture-directory");
        ResourcePatch selected = null;
        for (Patch<?> patch : PatchBundleLoader.INSTANCE.loadFlat(Collections.singleton(new File(args[0])))) {
            if ("広告・不要欄を非表示".equals(patch.getName())) {
                check(selected == null, "unique Yahoo patch");
                check(patch instanceof ResourcePatch, "Yahoo resource patch type");
                selected = (ResourcePatch)patch;
            }
        }
        check(selected != null, "production loader discovers Yahoo patch");
        check(selected.getDefault(), "Yahoo patch selected by default");
        check(selected.getDependencies().size() == 1, "PreserveYahooCode dependency retained");
        for (String version : new String[]{"8.4.1", "8.5.0", "8.6.0", "9.0.0"}) {
            Path directory = NearMeLayoutTest.fixture(Paths.get(args[1]), "registered-v" + version, version, "@id/near_me");
            Path home = directory.resolve("res/layout/fragment_home.xml");
            NearMeLayoutTest.write(home,
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@id/home\" android:tag=\"layout/fragment_home_0\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                + "<jp.co.yahoo.android.apps.transit.ad.NaviSearchTopAdView android:id=\"@id/top_ad_view\" android:layout_width=\"match_parent\" android:layout_height=\"90dp\"/>"
                + "<LinearLayout android:id=\"@id/search_form\"><Button android:id=\"@id/search_button\" android:tag=\"binding_1\" android:text=\"検索\"/></LinearLayout>"
                + "<jp.co.yahoo.android.apps.transit.ad.NaviSearchBottomAdView android:id=\"@id/bottom_ad_view\" android:layout_width=\"match_parent\" android:layout_height=\"250dp\"/>"
                + "</LinearLayout>");
            for (int pass = 0; pass < 2; pass++) {
                selected.execute(NearMeLayoutTest.context(directory));
                try (Document document = new Document(home.toFile())) {
                    for (String id : new String[]{"@id/top_ad_view", "@id/bottom_ad_view"}) {
                        Element view = FullPatchExecutionTest.id(document, id);
                        Element parent = (Element)view.getParentNode();
                        check("hiro_ad_slot_hidden_v2".equals(NearMeLayoutTest.attribute(parent, "tag")), "registered callback shields " + id);
                        check("gone".equals(NearMeLayoutTest.attribute(parent, "visibility")), "hidden ad parent");
                        check(parent.getParentNode() == document.getDocumentElement(), "one shield, no nesting after repeated execution");
                    }
                    Element button = FullPatchExecutionTest.id(document, "@id/search_button");
                    check("binding_1".equals(NearMeLayoutTest.attribute(button, "tag")), "normal binding tag retained");
                    check("@id/search_form".equals(NearMeLayoutTest.attribute((Element)button.getParentNode(), "id")), "normal search button remains outside shield");
                    check("layout/fragment_home_0".equals(NearMeLayoutTest.attribute(document.getDocumentElement(), "tag")), "layout binding root retained");
                }
            }
        }
        System.out.println("PASS RegisteredPatchExecutionTest: " + assertions + " assertions; production bundle loader and public registered execute; generated resource fixtures only; original APK, dependency scheduler and Android runtime not exercised.");
    }
}
