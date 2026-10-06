import app.hiro.tripcom.patches.HomeResourceDocuments;
import app.morphe.patcher.patch.ResourcePatchContext;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import kotlin.jvm.functions.Function1;
import org.w3c.dom.*;

/** Runs the actual baseline and amended Home JVM closures on original Trip XML. */
public final class HomeResourceDocumentsTest {
    static final String CLOSURE = "app.hiro.tripcom.patches.HideHomeRecommendationsPatchKt$hideHomeRecommendationsPatch$1$1";
    static final String HEAD = "com.ctrip.ibu.home.home.presentation.head.container.HomeHeadContainer";
    static final String TABS = "com.ctrip.ibu.home.home.presentation.feeds.citylist.FeedsCityTabLayout";
    static final String PAGER = "androidx.viewpager2.widget.ViewPager2";
    static final Set<String> LOGICAL = new TreeSet<>(Arrays.asList("t0.xml", "pf.xml", "s9.xml", "p8.xml"));
    static final Set<String> COLLISIONS = new TreeSet<>(Arrays.asList("t4.xml", "pj.xml", "sc.xml", "pb.xml"));
    static void req(boolean test, String message) { if (!test) throw new AssertionError(message); }

    static org.w3c.dom.Document read(Path file) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
    }
    static boolean desired(Element e) {
        return HEAD.equals(e.getTagName()) || TABS.equals(e.getTagName())
                || (PAGER.equals(e.getTagName()) && "@id/b4r".equals(e.getAttribute("android:id")));
    }
    static String tree(Node node, boolean maskChanges) {
        if (!(node instanceof Element)) {
            String text = node.getNodeValue();
            return text == null || text.trim().isEmpty() ? "" : text;
        }
        Element e = (Element) node;
        StringBuilder out = new StringBuilder("<").append(e.getTagName());
        TreeMap<String, String> attributes = new TreeMap<>();
        NamedNodeMap map = e.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node attr = map.item(i);
            if (maskChanges && desired(e) && (((HEAD.equals(e.getTagName()) || TABS.equals(e.getTagName()))
                    && "app:layout_scrollFlags".equals(attr.getNodeName()))
                    || ((!HEAD.equals(e.getTagName())) && "android:visibility".equals(attr.getNodeName())))) continue;
            attributes.put(attr.getNodeName(), attr.getNodeValue());
        }
        for (Map.Entry<String, String> a : attributes.entrySet()) out.append('|').append(a.getKey()).append('=').append(a.getValue());
        out.append('>');
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) out.append(tree(children.item(i), maskChanges));
        return out.append("</").append(e.getTagName()).append('>').toString();
    }
    static Path fixture(Path source, Path work, String name) throws Exception {
        Path result = work.resolve(name).resolve("res");
        Files.createDirectories(result.resolve("layout"));
        Set<String> files = new TreeSet<>(LOGICAL); files.addAll(COLLISIONS);
        for (String file : files) Files.copy(source.resolve("layout").resolve(file), result.resolve("layout").resolve(file));
        return result;
    }
    static Map<String, String> mergedAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("res/layout/t0.xml", "res/layout/t4.xml");
        aliases.put("res/layout/pf.xml", "res/layout/pj.xml");
        aliases.put("res/layout/s9.xml", "res/layout/sc.xml");
        aliases.put("res/layout/p8.xml", "res/layout/pb.xml");
        return aliases;
    }
    @SuppressWarnings("unchecked")
    static void apply(URL location, ResourcePatchContext context) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[]{location}, HomeResourceDocumentsTest.class.getClassLoader())) {
            Class<?> type = Class.forName(CLOSURE, true, loader);
            req(type.getClassLoader() == loader, "Test accidentally loaded another Home closure");
            Field field = type.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            ((Function1<ResourcePatchContext, Object>) field.get(null)).invoke(context);
        }
    }
    static void after(Path fixtures, Path candidate, boolean expectRawChanged) throws Exception {
        for (String name : LOGICAL) {
            Element before = read(fixtures.resolve("layout").resolve(name)).getDocumentElement();
            Element result = read(candidate.resolve("layout").resolve(name)).getDocumentElement();
            req(tree(before, true).equals(tree(result, true)), "Unrelated XML structure or attribute changed: " + name);
            if (name.equals("t0.xml") || name.equals("pf.xml")) {
                NodeList heads = result.getElementsByTagName(HEAD), tabs = result.getElementsByTagName(TABS);
                req(heads.getLength() == 1 && tabs.getLength() == 1, "Original Home widgets disappeared");
                req(!((Element) heads.item(0)).hasAttribute("app:layout_scrollFlags"), "Header scroll flags remain");
                Element tab = (Element) tabs.item(0);
                req(!tab.hasAttribute("app:layout_scrollFlags") && "gone".equals(tab.getAttribute("android:visibility")), "Recommendation tabs not hidden");
            } else {
                NodeList pagers = result.getElementsByTagName(PAGER); int matches = 0;
                for (int i = 0; i < pagers.getLength(); i++) {
                    Element pager = (Element) pagers.item(i);
                    if ("@id/b4r".equals(pager.getAttribute("android:id"))) {
                        matches++;
                        req("gone".equals(pager.getAttribute("android:visibility")), "Recommendation pager remains visible");
                    }
                }
                req(matches == 1, "Original recommendation pager missing or duplicated");
            }
            if (expectRawChanged) req(!Arrays.equals(Files.readAllBytes(fixtures.resolve("layout").resolve(name)),
                    Files.readAllBytes(candidate.resolve("layout").resolve(name))), "Logical file was not written: " + name);
        }
        for (String name : COLLISIONS) req(Arrays.equals(Files.readAllBytes(fixtures.resolve("layout").resolve(name)),
                Files.readAllBytes(candidate.resolve("layout").resolve(name))), "Wrong mapped file was modified: " + name);
    }
    public static void main(String[] args) throws Exception {
        req(args.length == 4, "BASELINE_MPP AMENDED_CLASS_DIR FIXTURE_RES WORK");
        Path fixtures = Paths.get(args[2]), work = Paths.get(args[3]);
        URL original = Paths.get(args[0]).toUri().toURL(), amended = Paths.get(args[1]).toUri().toURL();
        Path oldBase = fixture(fixtures, work, "original-base");
        ResourcePatchContext baseContext = new ResourcePatchContext(oldBase.toFile(), Collections.emptyMap());
        apply(original, baseContext);
        after(fixtures, oldBase, true);
        req(baseContext.assetCalls == 1 && baseContext.mappedDocuments == 4, "Original Home asset/document behavior differs");

        Path oldMerged = fixture(fixtures, work, "original-merged");
        ResourcePatchContext collisionContext = new ResourcePatchContext(oldMerged.toFile(), mergedAliases());
        boolean reproduced = false;
        try { apply(original, collisionContext); }
        catch (IllegalStateException ex) { reproduced = ex.getMessage().contains("ホーム画面のヘッダーが見つかりません"); }
        req(reproduced && collisionContext.mappedDocuments == 1, "Original split-merge resource alias regression did not reproduce");

        for (String mode : Arrays.asList("base", "merged")) {
            Path next = fixture(fixtures, work, "amended-" + mode);
            ResourcePatchContext context = new ResourcePatchContext(next.toFile(), mode.equals("base") ? Collections.emptyMap() : mergedAliases());
            apply(amended, context);
            after(fixtures, next, true);
            for (String name : LOGICAL) req(tree(read(oldBase.resolve("layout").resolve(name)).getDocumentElement(), false)
                    .equals(tree(read(next.resolve("layout").resolve(name)).getDocumentElement(), false)),
                    "Amended Home behavior differs from successful original base behavior: " + name);
            req(context.assetCalls == 1 && context.rootGets == 4 && context.mappedDocuments == 0,
                    "Original asset call lost or filename alias resolver was used");
        }
        boolean unknown = false;
        try { HomeResourceDocuments.document(baseContext, "res/layout/t4.xml"); }
        catch (IllegalArgumentException expected) { unknown = true; }
        req(unknown, "Unrelated layout accepted by helper");
        Path malformed = fixture(fixtures, work, "wrong-logical-root");
        Files.copy(malformed.resolve("layout/t4.xml"), malformed.resolve("layout/t0.xml"), StandardCopyOption.REPLACE_EXISTING);
        boolean rejected = false;
        try { apply(amended, new ResourcePatchContext(malformed.toFile(), mergedAliases())); }
        catch (IllegalStateException expected) { rejected = expected.getMessage().contains("Unexpected Trip Home layout root"); }
        req(rejected, "Wrong logical structure accepted");
        System.out.println("{\"result\":\"PASS_HOME_BASE_AND_MERGED_RESOURCE_REGRESSION\",\"actual_original_and_amended_jvm_closures_executed\":true,\"original_split_merge_failure_reproduced\":true,\"base_and_merged_match_original_successful_base_behavior\":true,\"exact_four_logical_layouts_updated\":true,\"wrong_alias_layout_bytes_unchanged\":true,\"original_ai_planner_asset_call_retained\":true,\"unrelated_layout_and_wrong_root_rejected\":true,\"context_api_stubbed\":true,\"android_device_tested\":false}");
    }
}
