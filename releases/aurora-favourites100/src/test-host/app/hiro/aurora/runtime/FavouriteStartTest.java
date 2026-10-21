package app.hiro.aurora.runtime;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import com.aurora.store.compose.navigation.Screen;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Host state-transition tests. Android lifecycle delivery is verified structurally, not executed here. */
public final class FavouriteStartTest {
    private static int assertions, scenarios;
    private static final Object FAVOURITE = Screen.k.INSTANCE;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static ArrayList<Object> stack(Object... screens) {
        return new ArrayList<Object>(Arrays.asList(screens));
    }
    private static void favourite(List<?> screens, String reason) {
        check(screens.size() == 1, reason + ": stack must have one root");
        check(screens.get(0) == FAVOURITE, reason + ": favourite identity");
    }
    private static Object nativeMain(List<Object> screens) {
        Object main = new Screen.p();
        screens.clear(); // Reviewed native success branch clears before constructing Main.
        Object selected = AuroraFavourites.selectMain(screens, main);
        screens.add(selected);
        return selected;
    }
    private static void policy() {
        scenarios++;
        check(AuroraFavourites.normalLaunch(null), "null framework intent is ordinary startup");
        check(AuroraFavourites.normalLaunch(new Intent()), "internal no-action startup");
        check(AuroraFavourites.normalLaunch(new Intent(Intent.ACTION_MAIN)), "launcher MAIN startup");
        for (String action : new String[]{Intent.ACTION_VIEW, Intent.ACTION_SEND, "android.intent.action.SHOW_APP_INFO", "custom.action"})
            check(!AuroraFavourites.normalLaunch(new Intent(action)), "explicit action preserved: " + action);
        check(!AuroraFavourites.normalLaunch(new Intent(Intent.ACTION_MAIN).setData(Uri.parse("market://details?id=example"))), "URI always preserves explicit route");
        for (String extra : new String[]{"SCREEN", "packageName", "android.intent.extra.PACKAGE_NAME", "notification", "other"})
            check(!AuroraFavourites.normalLaunch(new Intent(Intent.ACTION_MAIN).putExtra(extra, "target")), "any explicit extras preserved: " + extra);
        for (String blocked : new String[]{"com.aurora.store.compose.navigation.Screen$z", "com.aurora.store.compose.navigation.Screen$s", "com.aurora.store.compose.navigation.Screen$l"})
            check(AuroraFavourites.blockedName(blocked), "auth/onboarding route guarded");
        check(!AuroraFavourites.blockedName("com.aurora.store.compose.navigation.Screen$p"), "Main is ready");
        check(!AuroraFavourites.blockedName("com.aurora.store.compose.navigation.Screen$k"), "Favourite is ready");
        check(AuroraFavourites.blocked(null), "missing stack guarded");
        check(AuroraFavourites.blocked(stack()), "empty stack guarded");
        check(AuroraFavourites.blocked(stack((Object) null)), "null screen guarded");
        for (Object gate : new Object[]{new Screen.z(), new Screen.s(), new Screen.l()}) {
            check(AuroraFavourites.blocked(stack(gate)), "gate at root guarded");
            check(AuroraFavourites.blocked(stack(new Screen.p(), gate)), "visible gate above root guarded");
            check(!AuroraFavourites.blocked(stack(gate, new Screen.c())), "historical gate below ready details must not block reopening");
        }
        check(!AuroraFavourites.blocked(stack(new Screen.p(), new Screen.c())), "ready navigation allowed");
    }
    private static void coldAuthentication() {
        scenarios++;
        Activity activity = new Activity();
        Object splash = new Screen.z();
        ArrayList<Object> screens = stack(splash);
        AuroraFavourites.bind(activity, screens, FAVOURITE);
        check(screens.size() == 1 && screens.get(0) == splash, "cold launch must first preserve Splash");
        check(nativeMain(screens) == FAVOURITE, "authentication success routes to Favourite");
        favourite(screens, "cold authenticated startup");
        Object next = new Screen.p();
        check(AuroraFavourites.selectMain(screens, next) == next, "startup selection consumed once");
        Object details = new Screen.c();
        screens.add(details);
        AuroraFavourites.bind(activity, screens, FAVOURITE);
        check(screens.size() == 2 && screens.get(1) == details, "recomposition preserves user navigation");
        AuroraFavourites.restart(activity);
        favourite(screens, "warm foreground startup");
    }
    private static void restoredAndConfiguration() {
        scenarios++;
        Activity restored = new Activity();
        ArrayList<Object> screens = stack(new Screen.p(), new Screen.c());
        AuroraFavourites.bind(restored, screens, FAVOURITE);
        favourite(screens, "cold process-restored ready navigation");
        for (Object historicalGate : new Object[]{new Screen.z(), new Screen.s(), new Screen.l()}) {
            Activity afterAuth = new Activity();
            ArrayList<Object> readyDetails = stack(historicalGate, new Screen.c());
            AuroraFavourites.bind(afterAuth, readyDetails, FAVOURITE);
            favourite(readyDetails, "restored ready details above historical authentication screen");
            readyDetails.clear();
            readyDetails.add(historicalGate);
            readyDetails.add(new Screen.c());
            AuroraFavourites.restart(afterAuth);
            favourite(readyDetails, "warm reopen after native details navigation above historical authentication");
        }
        Activity rotation = new Activity();
        rotation.setLastNonConfigurationInstance(new Object());
        Object details = new Screen.c();
        ArrayList<Object> rotated = stack(new Screen.p(), details);
        AuroraFavourites.bind(rotation, rotated, FAVOURITE);
        check(rotated.size() == 2 && rotated.get(1) == details, "configuration recreation preserves current screen");
        check(AuroraFavourites.selectMain(rotated, details) == details, "configuration does not arm later unexpected reroute");
        AuroraFavourites.restart(rotation);
        favourite(rotated, "later explicit foreground after rotation");
        Object splash = new Screen.z();
        ArrayList<Object> auth = stack(splash);
        Activity rotatingAuth = new Activity();
        rotatingAuth.setLastNonConfigurationInstance(new Object());
        AuroraFavourites.bind(rotatingAuth, auth, FAVOURITE);
        check(auth.get(0) == splash, "configuration during auth still preserves Splash");
        check(nativeMain(auth) == FAVOURITE, "first auth completion after rotation still uses Favourite");
    }
    private static void explicitTargets() {
        scenarios++;
        Intent[] intents = {
                new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=example")),
                new Intent(Intent.ACTION_SEND).putExtra("android.intent.extra.TEXT", "app"),
                new Intent(Intent.ACTION_MAIN).putExtra("SCREEN", "AppDetails"),
                new Intent().putExtra("notification", "updates")};
        for (Intent intent : intents) {
            Activity activity = new Activity();
            activity.setIntent(intent);
            Object details = new Screen.c();
            ArrayList<Object> screens = stack(details);
            AuroraFavourites.bind(activity, screens, FAVOURITE);
            check(screens.size() == 1 && screens.get(0) == details, "explicit target preserved on bind");
            Object main = new Screen.p();
            check(AuroraFavourites.selectMain(screens, main) == main, "initial explicit target cannot arm default override");
            AuroraFavourites.external(activity);
            AuroraFavourites.restart(activity);
            check(screens.size() == 1 && screens.get(0) == details, "external return preserves explicit target once");
            AuroraFavourites.resumed(activity);
            AuroraFavourites.restart(activity);
            favourite(screens, "later ordinary reopen of task originally created from explicit target");
            check(AuroraFavourites.selectMain(screens, main) == main, "later reopen does not arm unrelated native navigation override");
        }
    }
    private static void authReentry() {
        scenarios++;
        for (Object gate : new Object[]{new Screen.z(), new Screen.s(), new Screen.l()}) {
            Activity activity = new Activity();
            ArrayList<Object> screens = stack(gate);
            AuroraFavourites.bind(activity, screens, FAVOURITE);
            AuroraFavourites.restart(activity);
            check(screens.size() == 1 && screens.get(0) == gate, "warm foreground cannot bypass auth/onboarding");
            check(nativeMain(screens) == FAVOURITE, "native auth completion applies pending initial favourite");
            favourite(screens, "completed native auth");
        }
    }
    private static void externalReturns() {
        scenarios++;
        Activity activity = new Activity();
        ArrayList<Object> screens = stack(new Screen.p());
        AuroraFavourites.bind(activity, screens, FAVOURITE);
        Object details = new Screen.c();
        screens.add(details);
        AuroraFavourites.external(activity);
        AuroraFavourites.restart(activity);
        check(screens.size() == 2 && screens.get(1) == details, "installer/settings return preserves original screen");
        AuroraFavourites.resumed(activity);
        AuroraFavourites.restart(activity);
        favourite(screens, "later normal foreground after external return");
        screens.add(details);
        AuroraFavourites.external(activity);
        AuroraFavourites.resumed(activity); // Accepted external activity can finish without ever stopping Aurora.
        AuroraFavourites.restart(activity);
        favourite(screens, "no-stop external call does not suppress future startup");
    }
    private static void ownershipAndInvalidInputs() {
        scenarios++;
        Activity first = new Activity(), second = new Activity();
        Object gateA = new Screen.z(), gateB = new Screen.s();
        ArrayList<Object> a = stack(gateA), b = stack(gateB);
        AuroraFavourites.bind(new ContextWrapper(new ContextWrapper(first)), a, FAVOURITE);
        AuroraFavourites.bind(second, b, FAVOURITE);
        nativeMain(a);
        favourite(a, "wrapped activity registration");
        check(b.size() == 1 && b.get(0) == gateB, "separate activity pending state isolated");
        nativeMain(b);
        favourite(b, "second activity own completion");
        ArrayList<Object> ignored = stack(new Screen.p());
        Object original = ignored.get(0);
        AuroraFavourites.bind(new Context(), ignored, FAVOURITE);
        check(ignored.size() == 1 && ignored.get(0) == original, "non-activity context ignored");
        AuroraFavourites.bind(first, null, FAVOURITE);
        AuroraFavourites.bind(first, ignored, null);
        check(ignored.get(0) == original, "invalid binding arguments ignored");
        Activity finishing = new Activity();finishing.setFinishing(true);
        AuroraFavourites.bind(finishing, ignored, FAVOURITE);
        check(ignored.get(0) == original, "finishing activity not registered");
        Activity destroyed = new Activity();destroyed.setDestroyed(true);
        AuroraFavourites.bind(destroyed, ignored, FAVOURITE);
        check(ignored.get(0) == original, "destroyed activity not registered");
        a.add(original);first.setFinishing(true);AuroraFavourites.restart(first);
        check(a.size() == 2 && a.get(1) == original, "finishing registered activity not mutated");
        b.add(original);second.setDestroyed(true);AuroraFavourites.restart(second);
        check(b.size() == 2 && b.get(1) == original, "destroyed registered activity not mutated");
        AuroraFavourites.restart(new Activity()); // Unbound lifecycle callback is harmless.
        Object untouched = new Object();
        check(AuroraFavourites.selectMain(stack(), untouched) == untouched, "unregistered navigation keeps original object");
    }
    private static final class Counted extends ArrayList<Object> {
        int edits;
        @Override public void clear() { edits++;super.clear(); }
        @Override public boolean add(Object value) { edits++;return super.add(value); }
    }
    private static void idempotent() {
        scenarios++;
        Activity activity = new Activity();
        Counted screens = new Counted();screens.add(FAVOURITE);screens.edits=0;
        AuroraFavourites.bind(activity, screens, FAVOURITE);
        AuroraFavourites.restart(activity);
        AuroraFavourites.resumed(activity);
        AuroraFavourites.restart(activity);
        check(screens.edits == 0, "existing favourite root is not rewritten on repeated lifecycle callbacks");
        favourite(screens, "stable favourite root");
    }
    public static void main(String[] args) {
        policy();coldAuthentication();restoredAndConfiguration();explicitTargets();authReentry();externalReturns();ownershipAndInvalidInputs();idempotent();
        System.out.println("PASS Aurora favourite startup host tests: scenarios=" + scenarios + "; assertions=" + assertions
                + "; covers cold/warm, auth/onboarding, explicit targets, external returns, configuration and ownership; physical Android not tested.");
    }
}
