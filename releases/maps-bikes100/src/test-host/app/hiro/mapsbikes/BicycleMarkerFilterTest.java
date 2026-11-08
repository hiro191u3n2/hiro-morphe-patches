package app.hiro.mapsbikes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Host regression fixtures model the fields verified against both actual APKs. */
public final class BicycleMarkerFilterTest {
    static int assertions;
    static final String PREFIX = "https://www.google.com/maps/vt/icon/name=assets/icons/poi/quantum/pinlet_v3/";
    static final String BIKE = PREFIX + "bike_pinlet_v3-2-medium.png&highlight=78909c,ffffff&scale=4";
    static final String STORE = PREFIX + "shopping-2-medium.png&highlight=ff0000,ffffff&scale=4";
    static final String BACKGROUND = "assets/icons/poi/quantum/pinlet_v3/Circle_22_Background-2-medium.png";
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
    public static void main(String[] args) throws Exception {
        iconTests(); schemaTests();
        System.out.println("PASS bicycle glyph/point-label regression fixtures: assertions=" + assertions
            + "; route-mode artwork, store pins, point oneofs, null/malformed data, read-only input and repeated rendering preserved; Android UI not executed");
    }
}
