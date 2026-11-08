package app.hiro.mapsbikes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;

/** Read-only adapter for the verified SharedLabeler point-label protobuf schema. */
public final class BicycleMarkerFilter {
    private static volatile Schema cachedSchema;
    private static final int MAX_LAYERS = 64;
    private static final int MAX_COMPONENTS = 64;
    private static final int MAX_ICON_LENGTH = 8192;

    private BicycleMarkerFilter() {}

    /** Called before creating a placed label. False preserves the native path unchanged. */
    public static boolean shouldHide(Object label) {
        if (label == null) return false;
        try {
            if (!label.getClass().getName().equals("cift")) return false;
            Schema s = cachedSchema;
            if (s == null || s.labelType != label.getClass()) {
                s = new Schema(label.getClass());
                cachedSchema = s;
            }
            return s.containsBicycleGlyph(label);
        } catch (Exception ignored) {
            return false;
        } catch (LinkageError ignored) {
            return false;
        }
    }

    /** Recognizes a bicycle glyph asset, never a place name, route mode or arbitrary URL text. */
    static boolean isBicycleIcon(String raw) {
        if (raw == null || raw.length() > MAX_ICON_LENGTH) return false;
        String names = iconNames(raw);
        if (names == null) return false;
        // Names are from the official CompactLegend-Roadmap resource and the
        // corresponding Google vt PNGs, not guessed category/store names.
        for (String name : names.split(",", -1)) {
            if (name.equals("assets/icons/transit/quantum/bikeshare-2-medium.png")
                    || name.equals("assets/icons/transit/quantum/bikeshare_v2-0-tiny.png")
                    || name.equals("assets/icons/transit/quantum/bikeshare_v2-1-small.png")
                    || name.equals("assets/icons/transit/quantum/bikeshare_v2-2-medium.png")) return true;
            if (name.equals("assets/icons/poi/quantum/modern_pinlet/bike_pinlet_v3-2-medium.png")
                    || name.equals("assets/icons/poi/quantum/pinlet_88/bike_pinlet_v3-2-medium.png")
                    || name.equals("assets/icons/poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png")
                    || name.equals("assets/icons/road/icons/bike_pinlet_v3-2-medium.png")) return true;
        }
        return false;
    }

    private static String iconNames(String raw) {
        try {
            URI uri = new URI(raw.toLowerCase(Locale.ROOT));
            String host = uri.getHost();
            if (host != null) {
                String scheme = uri.getScheme();
                if (scheme != null && !scheme.equals("https") && !scheme.equals("http")) return null;
                if (!host.equals("www.google.com") && !host.equals("maps.google.com")
                        && !host.equals("maps.gstatic.com") && !host.equals("mts0.google.com")
                        && !host.equals("mt0.google.com")) return null;
                if (uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443 && uri.getPort() != 80)) return null;
            } else if (uri.isAbsolute() || uri.getRawAuthority() != null) {
                return null;
            }
            String path = uri.getRawPath();
            if (path == null) return null;
            if (path.startsWith("/maps/vt/")) path = path.substring("/maps/vt/".length());
            else if (path.startsWith("/mapfiles/vt/")) path = path.substring("/mapfiles/vt/".length());
            else if (path.startsWith("/vt/")) path = path.substring("/vt/".length());
            else if (path.startsWith("/")) path = path.substring(1);
            String value;
            if (path.startsWith("icon/name=")) {
                value = path.substring("icon/name=".length());
                int amp = value.indexOf('&');
                if (amp >= 0) value = value.substring(0, amp);
            } else if (path.equals("icon")) {
                value = null;
                String query = uri.getRawQuery();
                if (query == null) return null;
                for (String part : query.split("&", -1)) {
                    if (!part.startsWith("name=")) continue;
                    if (value != null) return null; // An ambiguous request is left to the native renderer.
                    value = part.substring(5);
                }
                if (value == null) return null;
            } else {
                return null;
            }
            // Parse separators before decoding so text in another query parameter
            // cannot become an icon component. Decode exactly once.
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Field field(Class<?> owner, String name, Class<?> expected) throws Exception {
        Field f = owner.getField(name);
        if (f.getDeclaringClass() != owner || Modifier.isStatic(f.getModifiers())
                || (expected == List.class ? !List.class.isAssignableFrom(f.getType()) : f.getType() != expected)) {
            throw new IllegalArgumentException("Unsupported label schema");
        }
        return f;
    }

    private static final class Schema {
        final Class<?> labelType, layerType, compositeType, componentType, sourceType;
        final Field pointKind, layers, layerKind, layerValue, components, source, sourceKind, sourceValue;

        Schema(Class<?> type) throws Exception {
            ClassLoader loader = type.getClassLoader();
            labelType = type;
            layerType = Class.forName("cifq", false, loader);
            compositeType = Class.forName("cifs", false, loader);
            componentType = Class.forName("cifr", false, loader);
            sourceType = Class.forName("cier", false, loader);
            pointKind = field(labelType, "c", int.class);
            layers = field(labelType, "f", List.class);
            layerKind = field(layerType, "c", int.class);
            layerValue = field(layerType, "d", Object.class);
            components = field(compositeType, "b", List.class);
            source = field(componentType, "c", sourceType);
            sourceKind = field(sourceType, "b", int.class);
            sourceValue = field(sourceType, "c", Object.class);
        }

        boolean containsBicycleGlyph(Object label) throws Exception {
            if (pointKind.getInt(label) != 2) return false;
            Object rawLayers = layers.get(label);
            if (!(rawLayers instanceof List)) return false;
            List<?> allLayers = (List<?>)rawLayers;
            if (allLayers.size() > MAX_LAYERS) return false;
            boolean found = false;
            for (Object layer : allLayers) {
                if (layer == null || layer.getClass() != layerType) return false;
                if (layerKind.getInt(layer) != 4) continue;
                Object composite = layerValue.get(layer);
                if (composite == null || composite.getClass() != compositeType) return false;
                Object rawComponents = components.get(composite);
                if (!(rawComponents instanceof List)) return false;
                List<?> allComponents = (List<?>)rawComponents;
                if (allComponents.size() > MAX_COMPONENTS) return false;
                for (Object component : allComponents) {
                    if (component == null || component.getClass() != componentType) return false;
                    Object asset = source.get(component);
                    if (asset == null) continue;
                    if (asset.getClass() != sourceType) return false;
                    if (sourceKind.getInt(asset) != 1) continue;
                    Object url = sourceValue.get(asset);
                    if (!(url instanceof String)) return false;
                    found |= isBicycleIcon((String)url);
                }
            }
            return found;
        }
    }
}
