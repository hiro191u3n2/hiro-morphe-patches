package app.hiro.oneback.runtime;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

/** Positive-only home recognition: ambiguity delegates BACK to the unmodified app. */
final class HomeScreen {
    private HomeScreen() {}
    static boolean restricted(String name) {
        return "com.twitter.android".equals(name) || "com.instagram.android".equals(name)
            || "com.zhiliaoapp.musically".equals(name) || "com.ss.android.ugc.trill".equals(name)
            || "ctrip.english".equals(name);
    }
    static boolean isHome(Activity activity) {
        if (activity == null || !restricted(activity.getPackageName()) || activity.isDestroyed()
                || activity.isFinishing()) return false;
        try {
            if (!mainActivity(activity)) return false;
            View root = activity.getWindow() == null ? null : activity.getWindow().peekDecorView();
            if (root == null || !root.isAttachedToWindow() || !root.hasWindowFocus() || !root.isShown()) return false;
            Rect bounds = new Rect();
            if (!root.getGlobalVisibleRect(bounds) || bounds.width() < 1 || bounds.height() < 1) return false;
            View focus = root.findFocus();
            if (focus != null && focus.onCheckIsTextEditor()) return false;
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsets insets = root.getRootWindowInsets();
                if (insets == null || Api30.imeVisible(insets)) return false;
            } else {
                Rect display = new Rect(); root.getWindowVisibleDisplayFrame(display);
                if (bounds.bottom - display.bottom > bounds.height() / 5) return false;
            }
            if (Build.VERSION.SDK_INT >= 29) {
                for (View window : android.view.inspector.WindowInspector.getGlobalWindowViews())
                    if (window != null && window != root && window.isAttachedToWindow() && window.isShown()
                        && window.hasWindowFocus() && activity.getPackageName().equals(window.getContext().getPackageName())) return false;
            }
            if (fragmentBlocks(activity)) return false;
            return selectedHomeNavigation(root, bounds, "ctrip.english".equals(activity.getPackageName()));
        } catch (RuntimeException | LinkageError ignored) { return false; }
    }
    private static boolean mainActivity(Activity activity) {
        String pkg = activity.getPackageName();
        // Trip builds may change the Activity host. The exact app-owned home layout
        // AND a positively selected bottom home tab must be present below; the
        // package or Activity name alone is never accepted as proof of home.
        if ("ctrip.english".equals(pkg)) return true;
        // Class identity alone never proves home; it must also pass visible selected-tab and overlay checks.
        for (Class<?> type = activity.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if ("com.twitter.android".equals(pkg) && ("com.twitter.android.HomeTimelineActivity".equals(name)
                    || "com.twitter.app.main.MainActivity".equals(name))) return true;
            if ("com.instagram.android".equals(pkg) && "com.instagram.mainactivity.InstagramMainActivity".equals(name)) return true;
            if (("com.zhiliaoapp.musically".equals(pkg) || "com.ss.android.ugc.trill".equals(pkg))
                    && "com.ss.android.ugc.aweme.main.MainActivity".equals(name)) return true;
        }
        return false;
    }
    private static String id(View view) {
        try { return view.getId() == View.NO_ID ? "" : view.getResources().getResourceEntryName(view.getId()).toLowerCase(Locale.ROOT); }
        catch (RuntimeException ignored) { return ""; }
    }
    private static String label(CharSequence value) {
        return value == null ? "" : value.toString().trim().toLowerCase(Locale.ROOT);
    }
    private static boolean homeLabel(String label) {
        return label.equals("home") || label.equals("ホーム") || label.equals("ホームタブ")
            || label.startsWith("home,") || label.startsWith("home, ") || label.startsWith("ホーム、")
            || label.startsWith("ホーム,") || label.startsWith("home tab") || label.startsWith("ホーム タブ");
    }
    private static boolean homeId(String id) {
        return id.equals("tab_home") || id.equals("home_tab") || id.equals("feed_tab")
            || id.equals("home_tab_button") || id.equals("bottom_nav_home") || id.equals("bottom_navigation_home")
            || id.equals("navigation_home") || id.equals("tab_home_icon") || id.equals("home_tab_icon");
    }
    private static boolean blockedOverlay(String id) {
        return id.equals("bottom_sheet") || id.equals("design_bottom_sheet") || id.equals("comment_sheet")
            || id.equals("comments_sheet") || id.equals("share_sheet") || id.equals("drawer_content")
            || id.equals("navigation_drawer") || id.equals("story_viewer") || id.equals("media_viewer")
            || id.equals("reel_viewer") || id.equals("search_edit_text") || id.equals("comment_edit_text");
    }
    private static boolean backLabel(String name) {
        return name.equals("back") || name.equals("navigate up") || name.equals("go back")
            || name.equals("close") || name.equals("戻る") || name.equals("前に戻る") || name.equals("閉じる");
    }
    private static boolean tripHomeContent(View view) {
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass())
            if ("com.ctrip.ibu.home.home.presentation.page.fragment.widget.HomeAppBarLayout".equals(type.getName())) return true;
        return false;
    }
    private static boolean selectedHomeNavigation(View root, Rect screen, boolean trip) {
        ArrayDeque<View> queue = new ArrayDeque<View>(); queue.add(root);
        boolean home = false, tripContent = false; int examined = 0;
        while (!queue.isEmpty()) {
            if (++examined > 1600) return false;
            View view = queue.removeFirst();
            if (!view.isShown() || view.getAlpha() <= 0.01f) continue;
            Rect rect = new Rect(); if (!view.getGlobalVisibleRect(rect) || rect.isEmpty()) continue;
            String resource = id(view), description = label(view.getContentDescription());
            if (trip && tripHomeContent(view)) tripContent = true;
            // Trip bottom labels may be TextViews rather than content descriptions.
            // Retain size, selection and clickability gates; no generic text fallback.
            String text = trip && view instanceof android.widget.TextView
                ? label(((android.widget.TextView)view).getText()) : "";
            if (blockedOverlay(resource)) return false;
            if (view.isClickable() && backLabel(description)
                && rect.centerY() < screen.top + screen.height() / 4) return false;
            boolean navSize = rect.centerY() > screen.top + screen.height() * 0.65f
                && rect.height() < screen.height() * 0.22f && rect.width() < screen.width() * 0.45f;
            if (navSize && (homeId(resource) || homeLabel(description) || (trip && homeLabel(text)))) {
                boolean selected = view.isSelected() || view.isActivated();
                boolean clickable = view.isClickable();
                // Instagram/TikTok sometimes set selection on the tab container instead of its icon.
                View ancestor = view;
                for (int n = 0; n < 2 && ancestor.getParent() instanceof View; n++) {
                    ancestor = (View) ancestor.getParent();
                    Rect area = new Rect();
                    if (!ancestor.getGlobalVisibleRect(area) || area.width() >= screen.width() * 0.45f
                        || area.height() >= screen.height() * 0.22f) break;
                    selected |= ancestor.isSelected() || ancestor.isActivated(); clickable |= ancestor.isClickable();
                }
                AccessibilityNodeInfo info = null;
                try {
                    info = view.createAccessibilityNodeInfo();
                    if (info != null) { selected |= info.isSelected() || (info.isCheckable() && info.isChecked()); clickable |= info.isClickable(); }
                } finally { if (info != null) info.recycle(); }
                if (selected && clickable) home = true;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup)view;
                if (group.getChildCount() > 1600 - examined) return false;
                for (int n = 0; n < group.getChildCount(); n++) queue.add(group.getChildAt(n));
            }
        }
        return home && (!trip || tripContent);
    }
    private static boolean fragmentBlocks(Activity activity) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
        return managerBlocks(invoke(activity, "getFragmentManager"), visited, 0)
            || managerBlocks(invoke(activity, "getSupportFragmentManager"), visited, 0);
    }
    private static Object invoke(Object object, String name) {
        if (object == null) return null;
        // Invoke through a public superclass: platform FragmentManagerImpl is package-private.
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass()) {
            if(!java.lang.reflect.Modifier.isPublic(type.getModifiers()))continue;
            try {
                Method method=type.getMethod(name);
                if(!java.lang.reflect.Modifier.isPublic(method.getDeclaringClass().getModifiers()))continue;
                return method.invoke(object);
            } catch(NoSuchMethodException absent) { /* Try a public superclass. */ }
              catch(ReflectiveOperationException | RuntimeException failed) { return Failure.INSTANCE; }
        }
        return null;
    }
    private enum Failure { INSTANCE }
    private static boolean managerBlocks(Object manager, Set<Object> visited, int depth) {
        if (manager == null) return false;
        if (manager == Failure.INSTANCE || depth > 8) return true;
        if (!visited.add(manager)) return false;
        Object size = invoke(manager, "getBackStackEntryCount");
        if (size == Failure.INSTANCE || (size instanceof Number && ((Number)size).intValue() > 0)) return true;
        Object fragments = invoke(manager, "getFragments");
        if (fragments == Failure.INSTANCE) return true;
        if (fragments instanceof Iterable<?>) for (Object fragment : (Iterable<?>)fragments) {
            if (fragment == null || !Boolean.TRUE.equals(invoke(fragment, "isVisible"))) continue;
            String name = fragment.getClass().getName().toLowerCase(Locale.ROOT);
            if (name.contains("dialogfragment") || name.contains("bottomsheet") || name.contains("commentfragment")
                || name.contains("searchfragment") || name.contains("profilefragment") || name.contains("detailfragment")
                || name.contains("settingsfragment") || name.contains("directinbox") || name.contains("directthread")
                || name.contains("storyviewer") || name.contains("reelviewer")) return true;
            if (managerBlocks(invoke(fragment,"getChildFragmentManager"),visited,depth+1)) return true;
        }
        return false;
    }
    private static final class Api30 {
        static boolean imeVisible(WindowInsets insets) { return insets.isVisible(WindowInsets.Type.ime()); }
    }
}
