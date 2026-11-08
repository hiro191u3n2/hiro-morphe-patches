package app.hiro.mapsbikes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.google.common.collect.ImmutableList;

/** Host regression fixtures model the fields verified against both actual APKs. */
public final class BicycleMarkerFilterTest {
    static int assertions;
    static final String PREFIX = "https://www.google.com/maps/vt/icon/name=assets/icons/poi/quantum/pinlet_v3/";
    static final String BIKE = PREFIX + "bike_pinlet_v3-2-medium.png&highlight=78909c,ffffff&scale=4";
    static final String STORE = PREFIX + "shopping-2-medium.png&highlight=ff0000,ffffff&scale=4";
    static final String BACKGROUND = "assets/icons/poi/quantum/pinlet_v3/Circle_22_Background-2-medium.png";
    static final String BARE_BIKE = "assets/icons/poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png";
    static final String BARE_STORE = "assets/icons/poi/quantum/pinlet_v3/shopping-2-medium.png";
    static void check(boolean ok, String why) { assertions++; if (!ok) throw new AssertionError(why); }
    static Object fresh(String type) throws Exception { return Class.forName(type).getConstructor().newInstance(); }
    static Object get(Object o, String name) throws Exception { return o.getClass().getField(name).get(o); }
    static void set(Object o, String name, Object value) throws Exception { o.getClass().getField(name).set(o, value); }
    @SuppressWarnings("unchecked") static List<Object> list(Object o, String field) throws Exception { return (List<Object>)get(o,field); }
    static Object layer(String... icons) throws Exception {
        Object layer = fresh("cifq"), composite = fresh("cifs");
        for (String icon : icons) {
            Object component = fresh("cifr"), source = fresh("cier");
            set(source,"c",icon); set(component,"c",source); list(composite,"b").add(component);
        }
        set(layer,"d",composite); return layer;
    }
    static Object label(String... icons) throws Exception {
        Object result = fresh("cift"); list(result,"f").add(layer(icons)); return result;
    }
    static void iconTests() {
        String[] positives = {
            BIKE,
            BARE_BIKE,
            "/" + BARE_BIKE,
            "assets/icons/transit/quantum/bikeshare_v2-0-tiny.png",
            "assets/icons/poi/quantum/modern_pinlet/bike_pinlet_v3-2-medium.png",
            BIKE.replace("https://www.google.com/maps/vt/", "https://mts0.google.com/vt/"),
            BIKE.replace("https://www.google.com/maps/vt/", "https://mt0.google.com/vt/"),
            BIKE.replace("https://www.google.com/maps/vt/", "http://mt0.google.com/vt/"),
            BIKE.replace("/icon/name=", "/icon?name="),
            "/maps/vt/icon?scale=4&name=assets/icons/poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png",
            "/icon?name=assets%2Ficons%2Fpoi%2Fquantum%2Fpinlet_v3%2Fbike_pinlet_v3-2-medium.png&scale=4",
            "/icon/name=assets/icons/transit/quantum/bikeshare-2-medium.png",
            "/icon/name=assets/icons/transit/quantum/bikeshare_v2-0-tiny.png",
            "icon/name=assets/icons/transit/quantum/bikeshare_v2-1-small.png&scale=4",
            "/icon/name=assets/icons/transit/quantum/bikeshare_v2-2-medium.png?scale=4",
            "/icon/name=" + BACKGROUND + ",assets/icons/poi/quantum/modern_pinlet/bike_pinlet_v3-2-medium.png&scale=2",
            "https://maps.gstatic.com/mapfiles/vt/icon/name=assets/icons/poi/quantum/pinlet_88/bike_pinlet_v3-2-medium.png?scale=4",
            "/icon/name=assets/icons/road/icons/bike_pinlet_v3-2-medium.png"
        };
        for (String x : positives) check(BicycleMarkerFilter.isBicycleIcon(x), "Expected bicycle glyph: " + x);
        String[] negatives = {
            null,"","bicycle","ループ",STORE,
            BACKGROUND, BARE_STORE,
            BARE_BIKE + ".exe",
            BARE_STORE + "?bike=" + BARE_BIKE,
            BARE_BIKE + "?name=" + BARE_STORE,
            BARE_STORE + "," + BARE_BIKE + "&injected=1",
            BARE_BIKE + "#other",
            "other/" + BARE_BIKE,
            PREFIX + "motorcycle-2-medium.png",
            PREFIX + "motorcycle_rental_service-2-medium.png",
            PREFIX + "cycle_shop_logo-2-medium.png",
            PREFIX + "bicycle_cafe-2-medium.png",
            PREFIX + "shopping-2-medium.png&name=assets/icons/poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png",
            PREFIX + "dot_pinlet-2-medium.png?other=assets/icons/poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png",
            "/icon/name=" + BACKGROUND + "&highlight=78909c,ffffff",
            "/icon/name=assets/icons/navigation/bicycle-2-medium.png",
            "/icon/name=assets/icons/poi/../navigation/bicycle-2-medium.png",
            "https://example.com/icon/name=assets/icons/poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png",
            "/icon/name=assets/icons/poi/quantum/pinlet_v3/bicycle-2-medium.png.exe",
            "/icon/name=assets/icons/poi/quantum/pinlet_v3/%62icycle-2-medium.png",
            "ic_bicycling", "cached_chevron_meta_bicycle",
            "/icon/name=assets/icons/directions/bicycle_modern-1-small.png",
            "/icon/name=assets/icons/directions/bike-1-small.png",
            "/icon/name=assets/icons/transit/quantum/bikeshare_background-1-small.png",
            "/icon/name=assets/icons/transit/quantum/bus-0-tiny.png",
            PREFIX + "bike_pinlet_v3_extra-2-medium.png",
            "/icon/name=assets/icons/poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png.exe",
            "https://mts0.google.com.evil.example/vt/icon/name=assets/icons/poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png",
            "/icon?name=assets/icons/poi/quantum/pinlet_v3/shopping-2-medium.png&other=assets/icons/poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png",
            "/icon?name=assets/icons/poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png&name=other.png",
            "/icon?name=assets%252Ficons%252Fpoi%252Fquantum%252Fpinlet_v3%252Fbike_pinlet_v3-2-medium.png",
            "/icon?name=%xx",
            "not a URL"
        };
        for (String x : negatives) check(!BicycleMarkerFilter.isBicycleIcon(x), "Unrelated icon filtered: " + x);
        check(!BicycleMarkerFilter.isBicycleIcon(BIKE + String.join("", Collections.nCopies(9000,"x"))), "Oversized icon accepted");
    }
    static void schemaTests() throws Exception {
        check(!BicycleMarkerFilter.shouldHide(null), "Null label hidden");
        check(!BicycleMarkerFilter.shouldHide("bicycle"), "Unknown root hidden");
        Object bicycle = label(BIKE), store = label(STORE);
        List<Object> originalLayers = list(bicycle,"f");
        Object originalLayer = originalLayers.get(0), originalComposite = get(originalLayer,"d");
        List<Object> originalComponents = list(originalComposite,"b");
        check(BicycleMarkerFilter.shouldHide(bicycle), "Bicycle point label kept");
        check(!BicycleMarkerFilter.shouldHide(store), "Store point label hidden");
        check(list(bicycle,"f") == originalLayers && originalLayers.size() == 1 && originalLayers.get(0) == originalLayer,
            "Input point layers mutated");
        check(get(originalLayer,"d") == originalComposite && list(originalComposite,"b") == originalComponents,
            "Input composite mutated");
        set(store,"unrelatedPlaceName","bicycle LUUP ループ");
        check(!BicycleMarkerFilter.shouldHide(store), "Place name used as icon classification");
        Object road = label(BIKE); set(road,"c",1);
        check(!BicycleMarkerFilter.shouldHide(road), "Non-point road label hidden");
        Object empty = fresh("cift"); check(!BicycleMarkerFilter.shouldHide(empty), "Empty point hidden");
        Object mixed = label(STORE); list(mixed,"f").add(layer(BIKE));
        check(BicycleMarkerFilter.shouldHide(mixed), "Bicycle on second layer not found");
        check(BicycleMarkerFilter.shouldHide(label(STORE,BIKE)), "Bicycle on second component not found");
        check(BicycleMarkerFilter.shouldHide(label(BACKGROUND,BARE_BIKE)), "Bare native asset glyph kept");
        Object textLayer = layer(BIKE); set(textLayer,"c",3);
        Object textOnly = fresh("cift"); list(textOnly,"f").add(textLayer);
        check(!BicycleMarkerFilter.shouldHide(textOnly), "Text oneof treated as icon");
        Object malformed = label(BIKE); list(malformed,"f").add(new Object());
        check(!BicycleMarkerFilter.shouldHide(malformed), "Malformed layer list did not preserve native path");
        Object wrongComposite = fresh("cift"), wrongLayer = layer(BIKE); set(wrongLayer,"d",new Object());
        list(wrongComposite,"f").add(wrongLayer);
        check(!BicycleMarkerFilter.shouldHide(wrongComposite), "Wrong composite hidden");
        Object noSource = label(BIKE), nsLayer = list(noSource,"f").get(0);
        set(list(get(nsLayer,"d"),"b").get(0),"c",null);
        check(!BicycleMarkerFilter.shouldHide(noSource), "Absent optional source hidden");
        Object numericSource = label(BIKE), nLayer = list(numericSource,"f").get(0);
        Object nSource = get(list(get(nLayer,"d"),"b").get(0),"c"); set(nSource,"b",2);
        check(!BicycleMarkerFilter.shouldHide(numericSource), "Non-URL asset source hidden");
        Object badUrl = label(BIKE), bLayer = list(badUrl,"f").get(0);
        Object bSource = get(list(get(bLayer,"d"),"b").get(0),"c"); set(bSource,"c",123);
        check(!BicycleMarkerFilter.shouldHide(badUrl), "Wrong URL type hidden");
        Object nullLayers = fresh("cift"); set(nullLayers,"f",null);
        check(!BicycleMarkerFilter.shouldHide(nullLayers), "Null list hidden");
        Object huge = fresh("cift"); for(int i=0;i<65;i++) list(huge,"f").add(layer(BIKE));
        check(!BicycleMarkerFilter.shouldHide(huge), "Oversized layer list hidden");
        check(BicycleMarkerFilter.shouldHide(bicycle), "Bad input poisoned following valid input");
        for (int i=0;i<3;i++) {
            check(BicycleMarkerFilter.shouldHide(bicycle), "Repeated bicycle classification unstable");
            check(!BicycleMarkerFilter.shouldHide(store), "Repeated store classification unstable");
        }
        Object immutable = label(BIKE);
        set(immutable,"f",Collections.unmodifiableList(new ArrayList<Object>(list(immutable,"f"))));
        check(BicycleMarkerFilter.shouldHide(immutable), "Immutable protobuf list unsupported");
    }
    static Object legacyStyle(String... icons) throws Exception {
        Object style = fresh("bkjj"), icon = fresh("bkje");
        List<Object> parts = new ArrayList<Object>();
        for (String url : icons) {
            Object item = fresh("bkjc"); set(item,"b",url); parts.add(item);
        }
        set(icon,"c",ImmutableList.copyOf(parts)); set(style,"t",icon);
        return style;
    }
    static Object override(int id, int bits, String url) throws Exception {
        Object value = fresh("chue"); set(value,"c",id); set(value,"b",bits); set(value,"f",url);
        return value;
    }
    static void legacyTests() throws Exception {
        ImmutableList<Object> empty = ImmutableList.copyOf(Collections.emptyList());
        Object bike = legacyStyle(BACKGROUND,BARE_BIKE), store = legacyStyle(BARE_STORE);
        check(!BicycleMarkerFilter.shouldHideLegacy(null,empty), "Null legacy style hidden");
        check(!BicycleMarkerFilter.shouldHideLegacy("bicycle",empty), "Unknown legacy style hidden");
        check(!BicycleMarkerFilter.shouldHideLegacy(bike,null), "Null overrides hidden");
        check(!BicycleMarkerFilter.shouldHideLegacy(bike,Collections.emptyList()), "Unknown override ABI accepted");
        check(BicycleMarkerFilter.shouldHideLegacy(bike,empty), "Legacy bicycle composite kept");
        check(!BicycleMarkerFilter.shouldHideLegacy(store,empty), "Legacy store hidden");
        check(!BicycleMarkerFilter.shouldHideLegacy(legacyStyle(BACKGROUND),empty), "Shared background hidden");
        Object icon = get(bike,"t"); List<Object> parts = list(icon,"c"); Object component = parts.get(1);
        set(component,"d",37);
        Object storeOverride = override(37,1024,BARE_STORE), bikeOverride = override(37,1024,BARE_BIKE);
        check(!BicycleMarkerFilter.shouldHideLegacy(bike,ImmutableList.copyOf(Arrays.asList(storeOverride))),
            "Feature override to store ignored");
        check(!BicycleMarkerFilter.shouldHideLegacy(bike,ImmutableList.copyOf(Arrays.asList(bikeOverride,storeOverride))),
            "Native last-override precedence ignored");
        check(BicycleMarkerFilter.shouldHideLegacy(bike,ImmutableList.copyOf(Arrays.asList(storeOverride,bikeOverride))),
            "Native bicycle override not inspected");
        check(!BicycleMarkerFilter.shouldHideLegacy(bike,ImmutableList.copyOf(Arrays.asList(override(37,4,null)))),
            "Inline bitmap override treated as old bike URL");
        check(BicycleMarkerFilter.shouldHideLegacy(bike,ImmutableList.copyOf(Arrays.asList(override(999,1024,BARE_STORE)))),
            "Unrelated override suppressed matching icon");
        check(get(bike,"t") == icon && list(icon,"c") == parts && get(component,"b").equals(BARE_BIKE),
            "Native source style or icon component mutated");
        Class<?> resolver = Class.forName("bken");
        check(resolver.getField("calls").getInt(null) >= 5, "Actual native resolver entry was bypassed");
        resolver.getField("throwForTest").setBoolean(null,true);
        check(!BicycleMarkerFilter.shouldHideLegacy(bike,ImmutableList.copyOf(Arrays.asList(bikeOverride))),
            "Resolver exception did not preserve native path");
        resolver.getField("throwForTest").setBoolean(null,false);
        Object malformed = legacyStyle(BARE_BIKE);
        set(get(malformed,"t"),"c",ImmutableList.copyOf(Arrays.asList(fresh("bkjc"))));
        check(!BicycleMarkerFilter.shouldHideLegacy(malformed,empty), "Null legacy component URL hidden");
        set(get(malformed,"t"),"c",ImmutableList.copyOf(Arrays.asList(new Object())));
        check(!BicycleMarkerFilter.shouldHideLegacy(malformed,empty), "Unknown legacy component class hidden");
        set(get(malformed,"t"),"c",ImmutableList.copyOf(Collections.nCopies(65,component)));
        check(!BicycleMarkerFilter.shouldHideLegacy(malformed,empty), "Oversized legacy composite hidden");
        check(!BicycleMarkerFilter.shouldHideLegacy(bike,ImmutableList.copyOf(Collections.nCopies(65,bikeOverride))),
            "Oversized override list accepted");
        check(BicycleMarkerFilter.shouldHideLegacy(bike,empty), "Malformed input poisoned next native match");
    }
    public static void main(String[] args) throws Exception {
        iconTests(); schemaTests(); legacyTests();
        assertions += BicycleLegendFilterTest.run();
        System.out.println("PASS bicycle glyph/shared+legacy label regression fixtures: assertions=" + assertions
            + "; bare native assets, per-feature overrides, route-mode artwork, store pins, point oneofs, malformed data and immutable inputs covered; Android UI not executed");
    }
}
