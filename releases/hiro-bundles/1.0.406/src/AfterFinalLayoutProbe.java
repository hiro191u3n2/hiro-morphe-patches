import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;

/** Device-side regression for the actual result-layout hierarchy and Android's remove/add semantics.
 *  The leaf controls have no search/network dependency. Input is the compiled Yahoo APK. */
public final class AfterFinalLayoutProbe {
    private static final String NS = "http://schemas.android.com/apk/res/android";
    private final Map<String,View> ids = new HashMap<>();
    private final Context context;
    private AfterFinalLayoutProbe(Context context) { this.context = context; }

    private void inflateHierarchy(String apk) throws Exception {
        AssetManager assets = AssetManager.class.getConstructor().newInstance();
        int cookie = (int) AssetManager.class.getMethod("addAssetPath", String.class).invoke(assets, apk);
        if (cookie == 0) throw new IllegalStateException("Cannot load test APK");
        Resources res = new Resources(assets, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
        int layout = res.getIdentifier("fragment_search_result_list", "layout", "jp.co.yahoo.android.apps.transit");
        ArrayDeque<ViewGroup> stack = new ArrayDeque<>();
        try (XmlResourceParser xml = res.getLayout(layout)) {
            for (int event = xml.next(); event != XmlPullParser.END_DOCUMENT; event = xml.next()) {
                if (event == XmlPullParser.END_TAG) { stack.pop(); continue; }
                if (event != XmlPullParser.START_TAG) continue;
                ViewGroup view = xml.getName().equals("LinearLayout") ? new LinearLayout(context) : new FrameLayout(context);
                if (!stack.isEmpty()) stack.peek().addView(view);
                stack.push(view);
                int id = xml.getAttributeResourceValue(NS, "id", 0);
                if (id != 0) ids.put(res.getResourceEntryName(id), view);
            }
        }
    }

    private void move() {
        LinearLayout root = (LinearLayout) ids.get("result_list_root");
        View header = ids.get("header"), near = ids.get("near_me"), taxi = ids.get("taxi");
        if (root == null || header == null || near == null || taxi == null)
            throw new AssertionError("Missing Yahoo result controls");
        // SearchResultListFragment's after-final branch for first-route arrival hour >= 4.
        root.removeView(taxi);
        root.removeView(near);
        int index = root.indexOfChild(header);
        root.addView(near, index + 1);
        root.addView(taxi, index + 2);
        if (root.getChildAt(index + 1) != near || root.getChildAt(index + 2) != taxi)
            throw new AssertionError("Unexpected result order");
    }

    public static void main(String[] args) throws Exception {
        if (Looper.myLooper() == null) Looper.prepareMainLooper();
        Class<?> type = Class.forName("android.app.ActivityThread");
        Object thread = type.getMethod("systemMain").invoke(null);
        Context context = (Context) type.getMethod("getSystemContext").invoke(thread);
        for (int index = 0; index < args.length; index += 2) {
            AfterFinalLayoutProbe probe = new AfterFinalLayoutProbe(context);
            probe.inflateHierarchy(args[index + 1]);
            boolean shouldCrash = args[index].equals("legacy");
            try {
                for (int pass = 0; pass < 3; pass++) probe.move();
                if (shouldCrash) throw new AssertionError("Old release unexpectedly succeeded");
                System.out.println("PASS: three consecutive reorders on " + args[index + 1]);
            } catch (IllegalStateException err) {
                if (!shouldCrash || !err.getMessage().contains("already has a parent")) throw err;
                System.out.println("REPRODUCED on " + args[index + 1] + ": " + err);
            }
        }
        System.out.println("AFTER_FINAL_LAYOUT_PASS");
        System.exit(0);
    }
}
