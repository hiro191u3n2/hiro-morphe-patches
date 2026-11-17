package app.hiro.newxui;

import app.morphe.extension.crimera.settings.SettingsRegistry;

public final class UiOptionsTest {
    private static int assertions;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static void checkFlags(String id, boolean restart, int order, String suffix) {
        SettingsRegistry.Toggle t=SettingsRegistry.toggles.get(id);
        check(t!=null && t.configured, id+" registered and configured");
        check(t.defaultValue, id+" defaults ON");
        check(t.restart==restart, id+" restart policy");
        check(t.category.equals(UiOptions.CATEGORY), id+" belongs to Hiro category");
        check(t.order==order, id+" expected display order");
        check(t.title.equals("hiro_newx_ui_"+suffix+"_title") && t.summary.equals("hiro_newx_ui_"+suffix+"_summary"), id+" correct resource keys");
    }
    public static void main(String[] args) {
        check(UiOptions.hideChatRecommendations(),"chat pre-registration fallback ON");
        check(UiOptions.pinBars(),"bars pre-registration fallback ON");
        check(UiOptions.force120Hz(),"refresh pre-registration fallback ON");
        check(UiOptions.fullViewCounts(),"views pre-registration fallback ON");
        UiOptions.register();
        check(SettingsRegistry.categories.size()==1,"exactly one category");
        check(SettingsRegistry.toggles.size()==4,"exactly four unique toggles");
        SettingsRegistry.Category category=SettingsRegistry.categories.get(UiOptions.CATEGORY);
        check(category!=null && category.id.equals("newx.hiro_ui"),"Hiro category ID");
        check(category.title.equals("hiro_newx_ui_title") && category.summary.equals("hiro_newx_ui_summary"),"category resource keys");
        check(category.icon.equals("ic_vector_settings_stroke") && category.order==550,"category icon and order");
        checkFlags("newx.hiro_ui.hide_chat_recommendations",true,100,"chat");
        checkFlags("newx.hiro_ui.pin_bars",true,200,"bars");
        checkFlags("newx.hiro_ui.force_120hz",false,300,"refresh");
        checkFlags("newx.hiro_ui.full_view_counts",true,400,"views");
        check(UiOptions.hideChatRecommendations() && UiOptions.pinBars() && UiOptions.force120Hz() && UiOptions.fullViewCounts(),"registered defaults are all ON");

        // Every combination exercises independence and changing values after an earlier read.
        for (int bits=0;bits<16;bits++) {
            boolean chat=(bits&1)!=0, bars=(bits&2)!=0, refresh=(bits&4)!=0, views=(bits&8)!=0;
            SettingsRegistry.values.put(UiOptions.CHAT,chat);
            SettingsRegistry.values.put(UiOptions.BARS,bars);
            SettingsRegistry.values.put(UiOptions.REFRESH,refresh);
            SettingsRegistry.values.put(UiOptions.VIEWS,views);
            check(UiOptions.hideChatRecommendations()==chat,"chat independent at mask "+bits);
            check(UiOptions.pinBars()==bars,"bars independent at mask "+bits);
            check(UiOptions.force120Hz()==refresh,"refresh independent at mask "+bits);
            check(UiOptions.fullViewCounts()==views,"views independent at mask "+bits);
        }
        SettingsRegistry.values.clear();
        check(UiOptions.hideChatRecommendations() && UiOptions.pinBars() && UiOptions.force120Hz() && UiOptions.fullViewCounts(),"fallback values remain ON after explicit values cleared");
        check(SettingsRegistry.toggles.get(UiOptions.CHAT).restart && SettingsRegistry.toggles.get(UiOptions.BARS).restart && !SettingsRegistry.toggles.get(UiOptions.REFRESH).restart && SettingsRegistry.toggles.get(UiOptions.VIEWS).restart,"reads do not modify restart flags");
        System.out.println("PASS: "+assertions+" UiOptions registration and independent-toggle assertions");
    }
}
