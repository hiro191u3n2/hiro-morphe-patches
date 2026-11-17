package app.morphe.extension.crimera.settings;

import java.util.LinkedHashMap;
import java.util.Map;

/** Host-only recording fake; never package this class in Android runtime DEX. */
public final class SettingsRegistry {
    public static final Map<String, Category> categories = new LinkedHashMap<String, Category>();
    public static final Map<String, Toggle> toggles = new LinkedHashMap<String, Toggle>();
    public static final Map<String, Boolean> values = new LinkedHashMap<String, Boolean>();

    public static final class Category {
        public final String id, title, summary, icon;
        public final int order;
        Category(String id, String title, String summary, String icon, int order) {
            this.id=id;this.title=title;this.summary=summary;this.icon=icon;this.order=order;
        }
    }
    public static final class Toggle {
        public final String category, id, title, summary;
        public final int order;
        public boolean defaultValue, restart, configured;
        Toggle(String category, String id, String title, String summary, int order) {
            this.category=category;this.id=id;this.title=title;this.summary=summary;this.order=order;
        }
    }

    public static void registerCategory(String id, String title, String summary, String icon, int order) {
        if (categories.put(id, new Category(id,title,summary,icon,order)) != null)
            throw new AssertionError("duplicate category: " + id);
    }
    public static void registerToggle(String category, String id, String title, String summary, int order) {
        if (!categories.containsKey(category)) throw new AssertionError("unknown category: " + category);
        if (toggles.put(id, new Toggle(category,id,title,summary,order)) != null)
            throw new AssertionError("duplicate toggle: " + id);
    }
    public static void configureToggle(String id, boolean defaultValue, boolean restart) {
        Toggle toggle=toggles.get(id);
        if (toggle==null || toggle.configured) throw new AssertionError("unexpected configuration: " + id);
        toggle.defaultValue=defaultValue;toggle.restart=restart;toggle.configured=true;
    }
    public static boolean getBooleanOrDefault(String id, boolean fallback) {
        Boolean value=values.get(id);
        if (value!=null) return value;
        Toggle toggle=toggles.get(id);
        return toggle!=null && toggle.configured ? toggle.defaultValue : fallback;
    }
}
