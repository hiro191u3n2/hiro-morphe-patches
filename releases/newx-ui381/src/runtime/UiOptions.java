package app.hiro.newxui;

import app.morphe.extension.crimera.settings.SettingsRegistry;

/** Independent, persisted controls in the existing Piko NewX settings screen. */
public final class UiOptions {
    public static final String CATEGORY = "newx.hiro_ui";
    public static final String CHAT = "newx.hiro_ui.hide_chat_recommendations";
    public static final String BARS = "newx.hiro_ui.pin_bars";
    public static final String REFRESH = "newx.hiro_ui.force_120hz";
    public static final String VIEWS = "newx.hiro_ui.full_view_counts";
    private UiOptions() {}
    public static void register() {
        SettingsRegistry.registerCategory(CATEGORY, "hiro_newx_ui_title", "hiro_newx_ui_summary", "ic_vector_settings_stroke", 550);
        add(CHAT, "hiro_newx_ui_chat_title", "hiro_newx_ui_chat_summary", 100, true);
        add(BARS, "hiro_newx_ui_bars_title", "hiro_newx_ui_bars_summary", 200, true);
        add(REFRESH, "hiro_newx_ui_refresh_title", "hiro_newx_ui_refresh_summary", 300, false);
        add(VIEWS, "hiro_newx_ui_views_title", "hiro_newx_ui_views_summary", 400, true);
    }
    private static void add(String id, String title, String summary, int order, boolean restart) {
        SettingsRegistry.registerToggle(CATEGORY, id, title, summary, order);
        SettingsRegistry.configureToggle(id, true, restart);
    }
    public static boolean hideChatRecommendations() { return SettingsRegistry.getBooleanOrDefault(CHAT, true); }
    public static boolean pinBars() { return SettingsRegistry.getBooleanOrDefault(BARS, true); }
    public static boolean force120Hz() { return SettingsRegistry.getBooleanOrDefault(REFRESH, true); }
    public static boolean fullViewCounts() { return SettingsRegistry.getBooleanOrDefault(VIEWS, true); }
}
