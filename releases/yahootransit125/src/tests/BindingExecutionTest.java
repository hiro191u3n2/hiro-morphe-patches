package app.hiro.yahootransit.patches;

import app.morphe.patcher.util.Document;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import javax.tools.ToolProvider;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Executes the unmodified Google Maven Data Binding 8.13.2 mapBindings bytecode.
 * The Android View tree is a host stub built from DOM elements. IDs and child
 * order are a minimal structural projection of the real 8.4.1/8.5.0 home resource;
 * other attributes and all text are omitted. ID-to-array indexes are a
 * test table, not the application's generated binding classes. Include binding
 * objects use a test generated mapper. This does not test Android inflation,
 * generated application casts, layout measurement, rendering, or a device.
 *
 * CLI: BindingExecutionTest WORK_DIRECTORY DEPENDENCY_DIRECTORY ANDROID_JAR
 * Dependencies are raw Google Maven artifacts, verified before extraction.
 */
public final class BindingExecutionTest {
    private static final String NS = "http://schemas.android.com/apk/res/android";
    private static final String MARKER = "hiro_ad_slot_hidden_v2";
    private static final String MAVEN = "https://dl.google.com/dl/android/maven2/";
    private static final String HOME_RAW_SHA = "70423f9f9fd0f13c8747bf47a8d6e5dc14aea0c79c8be71165226802b79a3090";
    private static final String HOME_FIXTURE_SHA = "0efbe0ae603e13249c8e0b2bbe684bb6b389076e081523d18debfeef5214e8b0";
    private static final String ANDROID_SHA = "4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b";
    private static int assertions;

    private static final String HOME_XML = """
        <androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android" android:id="@id/transit_scroll_view" android:tag="layout/fragment_navi_search_0">
          <LinearLayout>
            <LinearLayout android:id="@id/shortcut_description">
              <TextView />
              <View />
            </LinearLayout>
            <jp.co.yahoo.android.appnativeemg.appnativeemg.EmgViews android:id="@id/emg_views" />
            <FrameLayout android:id="@id/banner_emg_module_container" />
            <jp.co.yahoo.android.apps.transit.ad.NaviSearchTopAdView android:id="@id/top_ad_view" />
            <jp.co.yahoo.android.apps.transit.ui.view.navi.top.DetourLineListView android:id="@id/detour_line_list" />
            <jp.co.yahoo.android.apps.transit.ui.view.navi.top.InputStationView android:id="@id/via_input_view" />
            <jp.co.yahoo.android.apps.transit.ui.view.navi.top.SearchDateStateView android:id="@id/time_detail" android:tag="binding_1" />
            <androidx.appcompat.widget.LinearLayoutCompat android:tag="binding_2">
              <TextView />
              <jp.co.yahoo.android.apps.transit.ui.view.navi.top.SearchCondStateView android:id="@id/cond_state_view" />
            </androidx.appcompat.widget.LinearLayoutCompat>
            <Button android:id="@id/button_search" android:tag="binding_3" />
            <jp.co.yahoo.android.apps.transit.ui.view.navi.top.LocationUseInfoView android:id="@id/location_use_info" />
            <FrameLayout android:id="@id/banner_promo_module_container" />
            <jp.co.yahoo.android.apps.transit.ad.NaviSearchBottomAdView android:id="@id/bottom_ad_view" />
          </LinearLayout>
        </androidx.core.widget.NestedScrollView>
        """.stripTrailing();

    private static final String[][] DEPENDENCIES = {
        {"androidx/databinding", "databinding-runtime", "8.13.2", "aar", "5d5d11738bd4cb27a4075ab7ea2e30d1e3255afe95cb32410a7cfbc6c3241086"},
        {"androidx/databinding", "databinding-common", "8.13.2", "jar", "66cab82639dac0f6c2433464c093b074d608c4bb887ec38a9b8bc4ac98126732"},
        {"androidx/databinding", "viewbinding", "8.13.2", "aar", "b9a3ae329638ab623dc1daff2ac07489e0e4dbcf90d77ce2d8759f767cd7a135"},
        {"androidx/lifecycle", "lifecycle-common", "2.6.1", "jar", "f34831b6c71cd844e1d35d1be49d5e79447c5ab856346531b1e8676fda7374b1"},
        {"androidx/lifecycle", "lifecycle-livedata-core", "2.6.1", "aar", "2256780a3cff4a1e57fbb3d442557c17dc363ab8af105bcaf5261d8e2d5db949"},
        {"androidx/collection", "collection", "1.0.0", "jar", "9c8d117b5c2bc120a1cdfeb857e05b495b16c36013570372a708f7827e3ac9f9"}
    };

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static String sha(byte[] value) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(value)) result.append(String.format("%02x", b & 255));
        return result.toString();
    }

    private static String attr(Element element, String name) {
        String value = element.getAttributeNS(NS, name);
        return value.isEmpty() ? element.getAttribute("android:" + name) : value;
    }

    private static void set(Element element, String name, String value) {
        element.removeAttribute("android:" + name);
        element.removeAttributeNS(NS, name);
        element.setAttributeNS(NS, "android:" + name, value);
    }

    private static List<Element> elements(org.w3c.dom.Document document) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) result.add((Element) nodes.item(i));
        return result;
    }

    private static Element byId(org.w3c.dom.Document document, String id) {
        Element result = null;
        for (Element element : elements(document)) if (id.equals(attr(element, "id"))) {
            check(result == null, "unique ID " + id);
            result = element;
        }
        check(result != null, "ID present " + id);
        return result;
    }

    private static String signature(Element element) {
        return element.getTagName() + ":" + attr(element, "id") + ":" + attr(element, "tag");
    }

    private static void shielded(Element element) {
        check(element.getParentNode() instanceof Element, "target has parent");
        Element parent = (Element) element.getParentNode();
        check(MARKER.equals(attr(parent, "tag")), "binding-tagged screen target gains shield");
        check(attr(parent, "id").isEmpty(), "shield has no application ID");
        check("gone".equals(attr(parent, "visibility")), "shield is gone");
        check("0dp".equals(attr(parent, "layout_width")) && "0dp".equals(attr(parent, "layout_height")), "shield has zero dimensions");
    }

    private static void protect(org.w3c.dom.Document document) throws Exception {
        Method method = NearMeLayout.class.getDeclaredMethod("protectAds", org.w3c.dom.Document.class);
        method.setAccessible(true);
        method.invoke(null, document);
    }

    private static Map<String, String> stubSources() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("android/view/View.java", """
            package android.view;
            public class View {
                public int id = -1;
                public Object tag;
                public ViewParent parent;
                public final java.util.Map<Integer,Object> tags = new java.util.HashMap<>();
                public int getId() { return id; }
                public Object getTag() { return tag; }
                public Object getTag(int key) { return tags.get(key); }
                public void setTag(int key, Object value) { tags.put(key, value); }
                public ViewParent getParent() { return parent; }
                public interface OnAttachStateChangeListener {
                    void onViewAttachedToWindow(View view);
                    void onViewDetachedFromWindow(View view);
                }
            }
            """);
        sources.put("android/view/ViewParent.java", "package android.view; public interface ViewParent {}\n");
        sources.put("android/view/ViewGroup.java", """
            package android.view;
            public class ViewGroup extends View implements ViewParent {
                public final java.util.List<View> children = new java.util.ArrayList<>();
                public int getChildCount() { return children.size(); }
                public View getChildAt(int index) { return children.get(index); }
            }
            """);
        sources.put("android/util/SparseIntArray.java", """
            package android.util;
            public class SparseIntArray {
                private final java.util.Map<Integer,Integer> values = new java.util.HashMap<>();
                public int get(int key, int fallback) { return values.getOrDefault(key, fallback); }
                public void put(int key, int value) { values.put(key, value); }
            }
            """);
        sources.put("android/text/TextUtils.java", """
            package android.text;
            public final class TextUtils {
                public static boolean equals(CharSequence a, CharSequence b) {
                    return a == b || a != null && b != null && a.toString().contentEquals(b);
                }
            }
            """);
        sources.put("android/os/Build.java", "package android.os; public final class Build { public static final class VERSION { public static int SDK_INT = 35; }}\n");
        sources.put("androidx/databinding/DataBindingComponent.java", "package androidx.databinding; public interface DataBindingComponent {}\n");
        sources.put("androidx/databinding/library/R.java", "package androidx.databinding.library; public final class R { public static final class id { public static int dataBinding = 0x7f0f0001; }}\n");
        // App-specific mapper glue is normally generated by the Android compiler.
        // It records actual DataBindingUtil.bind calls; mapBindings itself stays untouched.
        sources.put("androidx/databinding/DataBinderMapperImpl.java", """
            package androidx.databinding;
            import android.view.View;
            public final class DataBinderMapperImpl extends DataBinderMapper {
                public ViewDataBinding getDataBinder(DataBindingComponent component, View view, int layoutId) {
                    return Bound.create(new View[]{view}, layoutId);
                }
                public ViewDataBinding getDataBinder(DataBindingComponent component, View[] views, int layoutId) {
                    return Bound.create(views, layoutId);
                }
                public int getLayoutId(String tag) { throw new AssertionError("Unexpected tag-based mapper lookup"); }
                public String convertBrIdToString(int id) { return null; }
                public static final class Bound extends ViewDataBinding {
                    public View[] roots;
                    public int layoutId;
                    private Bound() { super((DataBindingComponent)null, (View)null, 0); }
                    static Bound create(View[] views, int layoutId) {
                        try {
                            java.lang.reflect.Field field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
                            field.setAccessible(true);
                            Object unsafe = field.get(null);
                            Bound result = (Bound)unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, Bound.class);
                            result.roots = views.clone();
                            result.layoutId = layoutId;
                            return result;
                        } catch (Exception e) { throw new AssertionError(e); }
                    }
                    public View getRoot() { return roots[0]; }
                    protected boolean onFieldChange(int field, Object object, int id) { return false; }
                    public boolean setVariable(int id, Object object) { return false; }
                    protected void executeBindings() {}
                    public void invalidateAll() {}
                    public boolean hasPendingBindings() { return false; }
                }
            }
            """);
        return sources;
    }

    private static final class Runtime implements AutoCloseable {
        final URLClassLoader loader;
        final Class<?> view, group, sparse, includes, binding;
        final Method map;
        final Map<String, String> provenance = new LinkedHashMap<>();

        Runtime(Path work, Path dependencies, Path androidJar) throws Exception {
            check(ANDROID_SHA.equals(sha(Files.readAllBytes(androidJar))), "pinned Android API signature jar");
            Path extracted = Files.createDirectories(work.resolve("runtime-jars"));
            List<Path> jars = new ArrayList<>();
            for (String[] spec : DEPENDENCIES) {
                String name = spec[1] + "-" + spec[2] + "." + spec[3];
                Path raw = dependencies.resolve(name);
                check(spec[4].equals(sha(Files.readAllBytes(raw))), "pinned official dependency " + name);
                provenance.put(name + ".source_url", MAVEN + spec[0] + "/" + spec[1] + "/" + spec[2] + "/" + name);
                provenance.put(name + ".sha256", spec[4]);
                if (spec[3].equals("aar")) {
                    Path jar = extracted.resolve(spec[1] + "-" + spec[2] + ".jar");
                    try (ZipFile zip = new ZipFile(raw.toFile()); InputStream input = zip.getInputStream(zip.getEntry("classes.jar"))) {
                        Files.write(jar, input.readAllBytes());
                    }
                    provenance.put(jar.getFileName() + ".sha256", sha(Files.readAllBytes(jar)));
                    jars.add(jar);
                } else jars.add(raw);
            }
            jars.add(androidJar);
            Path sources = Files.createDirectories(work.resolve("stub-sources"));
            Path classes = Files.createDirectories(work.resolve("stub-classes"));
            List<String> compiler = new ArrayList<>(Arrays.asList("-encoding", "UTF-8", "-cp",
                String.join(File.pathSeparator, jars.stream().map(Path::toString).toList()), "-d", classes.toString()));
            for (Map.Entry<String, String> source : stubSources().entrySet()) {
                Path path = sources.resolve(source.getKey());
                Files.createDirectories(path.getParent());
                Files.writeString(path, source.getValue(), StandardCharsets.UTF_8);
                compiler.add(path.toString());
                provenance.put("stub." + source.getKey() + ".sha256", sha(source.getValue().getBytes(StandardCharsets.UTF_8)));
            }
            check(ToolProvider.getSystemJavaCompiler() != null, "JDK compiler available");
            check(ToolProvider.getSystemJavaCompiler().run(null, null, null, compiler.toArray(new String[0])) == 0, "minimal stub compilation");
            List<URL> urls = new ArrayList<>();
            urls.add(classes.toUri().toURL());
            for (Path jar : jars) urls.add(jar.toUri().toURL());
            loader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
            view = loader.loadClass("android.view.View");
            group = loader.loadClass("android.view.ViewGroup");
            sparse = loader.loadClass("android.util.SparseIntArray");
            includes = loader.loadClass("androidx.databinding.ViewDataBinding$IncludedLayouts");
            binding = Class.forName("androidx.databinding.ViewDataBinding", true, loader);
            check(binding.getProtectionDomain().getCodeSource().getLocation().equals(jars.get(0).toUri().toURL()), "ViewDataBinding loaded from verified official runtime jar");
            String resource = "androidx/databinding/ViewDataBinding.class";
            byte[] official;
            try (ZipFile zip = new ZipFile(jars.get(0).toFile()); InputStream input = zip.getInputStream(zip.getEntry(resource))) {
                official = input.readAllBytes();
            }
            try (InputStream input = loader.getResourceAsStream(resource)) {
                check(Arrays.equals(official, input.readAllBytes()), "ViewDataBinding bytecode is unmodified");
            }
            provenance.put("executed_ViewDataBinding.class.sha256", sha(official));
            map = binding.getDeclaredMethod("mapBindings", loader.loadClass("androidx.databinding.DataBindingComponent"), view, int.class, includes, sparse);
            map.setAccessible(true);
        }

        Object[] map(Object root, int count, Object included, Map<Integer, Integer> idIndexes) throws Exception {
            Object ids = sparse.getConstructor().newInstance();
            Method put = sparse.getMethod("put", int.class, int.class);
            for (Map.Entry<Integer, Integer> entry : idIndexes.entrySet()) put.invoke(ids, entry.getKey(), entry.getValue());
            return (Object[]) map.invoke(null, null, root, count, included, ids);
        }

        Object includeFixture() throws Exception {
            Object value = includes.getConstructor(int.class).newInstance(7);
            includes.getMethod("setIncludes", int.class, String[].class, int[].class, int[].class)
                .invoke(value, 2, new String[]{"card_ad", "merge_ad"}, new int[]{3, 4}, new int[]{101, 102});
            return value;
        }

        public void close() throws Exception { loader.close(); }
    }

    private static final class Tree {
        final Runtime runtime;
        final Map<String, Integer> ids = new LinkedHashMap<>();
        final IdentityHashMap<Element, Object> views = new IdentityHashMap<>();

        Tree(Runtime runtime) { this.runtime = runtime; }

        @SuppressWarnings("unchecked")
        Object sync(Element element) throws Exception {
            // Every DOM element may be represented by an empty ViewGroup; mapBindings
            // examines only ID/tag and children, not the application's concrete types.
            Object value = views.get(element);
            if (value == null) { value = runtime.group.getConstructor().newInstance(); views.put(element, value); }
            String id = attr(element, "id"), tag = attr(element, "tag");
            if (!id.isEmpty() && !ids.containsKey(id)) ids.put(id, ids.size() + 1);
            runtime.view.getField("id").setInt(value, id.isEmpty() ? -1 : ids.get(id));
            runtime.view.getField("tag").set(value, tag.isEmpty() ? null : tag);
            List<Object> children = (List<Object>) runtime.group.getField("children").get(value);
            children.clear();
            for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) if (child instanceof Element) {
                Object childView = sync((Element) child);
                runtime.view.getField("parent").set(childView, value);
                children.add(childView);
            }
            return value;
        }
    }

    private static final class HomeBindings {
        final Map<Integer, Element> expected = new LinkedHashMap<>();
        final Map<Integer, Integer> idIndexes = new LinkedHashMap<>();
        int size;

        HomeBindings(org.w3c.dom.Document document, Tree tree) {
            expected.put(0, document.getDocumentElement());
            size = 1;
            for (Element element : elements(document)) {
                String tag = attr(element, "tag");
                if (tag.matches("binding_[0-9]+")) {
                    int index = Integer.parseInt(tag.substring(8));
                    check(expected.put(index, element) == null, "unique original binding index");
                    size = Math.max(size, index + 1);
                }
            }
            for (Element element : elements(document)) {
                String id = attr(element, "id");
                if (id.isEmpty() || expected.containsValue(element)) continue;
                expected.put(size, element);
                idIndexes.put(tree.ids.get(id), size++);
            }
        }

        List<String> verify(Runtime runtime, Tree tree, Object root) throws Exception {
            Object[] actual = runtime.map(root, size, null, idIndexes);
            List<String> result = new ArrayList<>();
            for (Map.Entry<Integer, Element> entry : expected.entrySet()) {
                check(actual[entry.getKey()] == tree.views.get(entry.getValue()), "actual mapBindings returns original View at index " + entry.getKey());
                result.add(entry.getKey() + "=" + signature(entry.getValue()));
            }
            return result;
        }
    }

    private static void home(Runtime runtime, Path work, String version) throws Exception {
        Path file = work.resolve("home-" + version + ".xml");
        Files.writeString(file, HOME_XML, StandardCharsets.UTF_8);
        List<String> before;
        try (Document document = new Document(file.toFile())) {
            Tree tree = new Tree(runtime);
            Object root = tree.sync(document.getDocumentElement());
            HomeBindings spec = new HomeBindings(document, tree);
            before = spec.verify(runtime, tree, root);
            check(spec.expected.get(1) == byId(document, "@id/time_detail"), "real home binding_1");
            check(spec.expected.get(3) == byId(document, "@id/button_search"), "real home binding_3");
            check("androidx.appcompat.widget.LinearLayoutCompat".equals(spec.expected.get(2).getTagName()), "real home binding_2");
            protect(document);
            shielded(byId(document, "@id/top_ad_view"));
            shielded(byId(document, "@id/bottom_ad_view"));
            check(root == tree.sync(document.getDocumentElement()), "root View identity retained");
            check(before.equals(spec.verify(runtime, tree, root)), "all real home binding and ID slots unchanged");
        }
        try (InputStream input = Files.newInputStream(file); Document document = new Document(input)) {
            Tree tree = new Tree(runtime);
            Object root = tree.sync(document.getDocumentElement());
            HomeBindings spec = new HomeBindings(document, tree);
            check(before.equals(spec.verify(runtime, tree, root)), "saved home bindings unchanged");
            shielded(byId(document, "@id/top_ad_view"));
            shielded(byId(document, "@id/bottom_ad_view"));
            check("androidx.core.widget.NestedScrollView".equals(document.getDocumentElement().getTagName()), "real home root class retained");
            check("layout/fragment_navi_search_0".equals(attr(document.getDocumentElement(), "tag")), "real home root tag retained");
        }
    }

    private static final String INCLUDE_XML = """
        <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:tag="layout/binding_fixture_0">
          <example.AdView android:id="@id/tagged_ad" android:tag="binding_1"/>
          <FrameLayout android:id="@id/bound_container" android:tag="binding_2">
            <FrameLayout android:id="@id/include_ad_root" android:tag="layout/card_ad_0">
              <TextView android:id="@id/include_text" android:tag="binding_1"/>
            </FrameLayout>
            <FrameLayout android:id="@id/merge_ad_one" android:tag="layout/merge_ad_0"/>
            <FrameLayout android:id="@id/merge_ad_two" android:tag="layout/merge_ad_1"/>
            <example.AdView android:id="@id/ordinary_ad"/>
            <TextView android:id="@id/normal_text"/>
          </FrameLayout>
        </LinearLayout>
        """;

    private static Map<Integer, Integer> includeIdIndexes(Tree tree) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        result.put(tree.ids.get("@id/ordinary_ad"), 5);
        result.put(tree.ids.get("@id/normal_text"), 6);
        return result;
    }

    private static void bound(Object binding, int layoutId, Object... expected) throws Exception {
        check(binding != null, "actual include binder populated");
        check(binding.getClass().getField("layoutId").getInt(binding) == layoutId, "actual include layout ID");
        Object[] roots = (Object[]) binding.getClass().getField("roots").get(binding);
        check(Arrays.equals(roots, expected), "actual include/merge receives original ordered root Views");
    }

    private static void includes(Runtime runtime, Path work) throws Exception {
        Path file = work.resolve("bound-includes.xml");
        Files.writeString(file, INCLUDE_XML, StandardCharsets.UTF_8);
        try (Document document = new Document(file.toFile())) {
            Tree tree = new Tree(runtime);
            Object root = tree.sync(document.getDocumentElement());
            Element tagged = byId(document, "@id/tagged_ad");
            Element ordinary = byId(document, "@id/ordinary_ad");
            Element parent = byId(document, "@id/bound_container");
            Element card = byId(document, "@id/include_ad_root");
            Element merge0 = byId(document, "@id/merge_ad_one");
            Element merge1 = byId(document, "@id/merge_ad_two");
            Object info = runtime.includeFixture();
            Map<Integer, Integer> ids = includeIdIndexes(tree);
            Object[] before = runtime.map(root, 7, info, ids);
            bound(before[3], 101, tree.views.get(card));
            bound(before[4], 102, tree.views.get(merge0), tree.views.get(merge1));
            protect(document);
            shielded(tagged);
            shielded(ordinary);
            check("binding_1".equals(attr(tagged, "tag")), "binding_N stays on original wrapped ad");
            check(card.getParentNode() == parent && merge0.getParentNode() == parent && merge1.getParentNode() == parent, "include and merge roots are not reparented");
            Object[] after = runtime.map(tree.sync(document.getDocumentElement()), 7, info, ids);
            for (int index : new int[]{0, 1, 2, 5, 6}) check(after[index] == before[index], "ordinary binding identity retained alongside includes");
            bound(after[3], 101, tree.views.get(card));
            bound(after[4], 102, tree.views.get(merge0), tree.views.get(merge1));
            check(before[1] == tree.views.get(tagged), "include binding_1 never leaks into outer binding");

            // Negative controls prove that the real runtime detects the unsafe boundary.
            Element bad = document.createElement("FrameLayout");
            parent.insertBefore(bad, card);
            bad.appendChild(card);
            Object[] broken = runtime.map(tree.sync(document.getDocumentElement()), 7, info, ids);
            check(broken[3] == null, "negative control: wrapping include root loses its binding");
            parent.insertBefore(card, bad);
            parent.removeChild(bad);
            parent.insertBefore(bad, merge1);
            bad.appendChild(merge1);
            broken = runtime.map(tree.sync(document.getDocumentElement()), 7, info, ids);
            bound(broken[4], 102, tree.views.get(merge0));
            parent.insertBefore(merge1, bad);
            parent.removeChild(bad);
        }
    }

    private static String json(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("WORK_DIRECTORY DEPENDENCY_DIRECTORY ANDROID_JAR required");
        Path work = Path.of(args[0]).toAbsolutePath();
        Path dependencies = Path.of(args[1]).toAbsolutePath();
        Path androidJar = Path.of(args[2]).toAbsolutePath();
        Files.createDirectories(work);
        check(HOME_FIXTURE_SHA.equals(sha(HOME_XML.getBytes(StandardCharsets.UTF_8))), "embedded real home structural fixture digest");
        try (Runtime runtime = new Runtime(work, dependencies, androidJar)) {
            home(runtime, work, "8.4.1");
            home(runtime, work, "8.5.0");
            includes(runtime, work);
            Map<String, String> provenance = runtime.provenance;
            provenance.put("result", "PASS");
            provenance.put("runtime_version", "8.13.2");
            provenance.put("runtime_bytecode_transformed", "false");
            provenance.put("home_resource", "res/layout/fragment_navi_search.xml; identical decoded 8.4.1 and 8.5.0 inputs");
            provenance.put("home_resource_original_sha256", HOME_RAW_SHA);
            provenance.put("embedded_home_structural_fixture_sha256", HOME_FIXTURE_SHA);
            provenance.put("fixture_projection", "Original class names, ID/tag attributes, parent-child topology and child order; all other attributes and text omitted.");
            provenance.put("id_index_mapping", "Test table synthesized from every original home ID; binding_1/2/3 use original numeric tags.");
            provenance.put("include_mapper", "Test-generated DataBinderMapperImpl records roots passed by official DataBindingUtil.bind.");
            provenance.put("limitations", "Host View stubs; no actual Android inflation, application generated binding classes, LayoutParams/cast behavior, rendering, or physical device.");
            provenance.put("assertions", Integer.toString(assertions));
            StringBuilder report = new StringBuilder("{\n");
            int index = 0;
            for (Map.Entry<String, String> entry : provenance.entrySet()) {
                if (index++ > 0) report.append(",\n");
                report.append("  ").append(json(entry.getKey())).append(": ").append(json(entry.getValue()));
            }
            report.append("\n}\n");
            Files.writeString(work.resolve("binding-execution-provenance.json"), report, StandardCharsets.UTF_8);
        }
        System.out.println("PASS BindingExecutionTest: " + assertions + " assertions; unmodified Data Binding 8.13.2; real home ID/tag identity; include/merge positive and negative controls.");
    }
}
