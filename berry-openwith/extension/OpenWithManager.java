package hiro.berry.openwith;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OpenWithManager {
    private static final String PREF = "hiro_berry_openwith_manager";
    private static final String KEY_HIDDEN = "hidden_components_v1";

    private OpenWithManager() {}

    public static boolean isVisible(Context context, ComponentName component) {
        if (context == null || component == null) return true;
        return !hidden(context).contains(component.flattenToString());
    }

    public static boolean onLongPress(View anchor, ComponentName component, int visibleCount) {
        if (anchor == null || component == null) return false;
        final Context context = anchor.getContext();
        if (context == null) return false;

        final Set<String> currentHidden = hidden(context);
        final String label = label(context, component);
        final List<String> actions = new ArrayList<>();
        final int hideIndex;
        final int manageIndex;

        if (visibleCount > 1) {
            hideIndex = actions.size();
            actions.add("「" + label + "」を非表示");
        } else {
            hideIndex = -1;
            actions.add("最後の候補なので非表示にできません");
        }

        if (!currentHidden.isEmpty()) {
            manageIndex = actions.size();
            actions.add("非表示アプリを管理（" + currentHidden.size() + "）");
        } else {
            manageIndex = -1;
        }

        new AlertDialog.Builder(context)
                .setTitle("他のブラウザで開く")
                .setItems(actions.toArray(new String[0]), (dialog, which) -> {
                    if (hideIndex >= 0 && which == hideIndex) {
                        Set<String> updated = new LinkedHashSet<>(hidden(context));
                        updated.add(component.flattenToString());
                        save(context, updated);
                        Toast.makeText(context, "「" + label + "」を非表示にしました", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (hideIndex < 0 && which == 0) {
                        Toast.makeText(context, "候補を0件にしないため、最後の1件は非表示にできません", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (manageIndex >= 0 && which == manageIndex) {
                        showHiddenManager(context);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        return true;
    }

    private static void showHiddenManager(Context context) {
        final List<Entry> entries = hiddenEntries(context);
        if (entries.isEmpty()) {
            Toast.makeText(context, "非表示のアプリはありません", Toast.LENGTH_SHORT).show();
            return;
        }

        final boolean[] checked = new boolean[entries.size()];
        final String[] labels = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) labels[i] = entries.get(i).label;

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("非表示アプリを復元")
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("選択したアプリを復元", (d, which) -> {
                    Set<String> updated = new LinkedHashSet<>(hidden(context));
                    int restored = 0;
                    for (int i = 0; i < entries.size(); i++) {
                        if (checked[i] && updated.remove(entries.get(i).flattened)) restored++;
                    }
                    save(context, updated);
                    Toast.makeText(context,
                            restored == 0 ? "復元するアプリを選択してください" : restored + "件を復元しました",
                            Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("すべて復元", (d, which) -> {
                    save(context, Collections.emptySet());
                    Toast.makeText(context, "すべて復元しました", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    private static List<Entry> hiddenEntries(Context context) {
        List<Entry> result = new ArrayList<>();
        for (String flattened : hidden(context)) {
            ComponentName component = ComponentName.unflattenFromString(flattened);
            if (component != null) result.add(new Entry(flattened, label(context, component)));
        }
        result.sort(Comparator.comparing(e -> e.label, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static String label(Context context, ComponentName component) {
        PackageManager pm = context.getPackageManager();
        try {
            ActivityInfo info = pm.getActivityInfo(component, 0);
            CharSequence label = info.loadLabel(pm);
            if (label != null && label.length() > 0) return label.toString();
        } catch (Throwable ignored) {
        }
        try {
            CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(component.getPackageName(), 0));
            if (label != null && label.length() > 0) return label.toString();
        } catch (Throwable ignored) {
        }
        return component.getPackageName();
    }

    private static Set<String> hidden(Context context) {
        SharedPreferences prefs = prefs(context);
        Set<String> set = prefs.getStringSet(KEY_HIDDEN, Collections.emptySet());
        return set == null ? new LinkedHashSet<>() : new LinkedHashSet<>(set);
    }

    private static void save(Context context, Set<String> values) {
        prefs(context).edit().putStringSet(KEY_HIDDEN, new LinkedHashSet<>(values)).apply();
    }

    private static SharedPreferences prefs(Context context) {
        Context app = context.getApplicationContext();
        return (app != null ? app : context).getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static final class Entry {
        final String flattened;
        final String label;
        Entry(String flattened, String label) {
            this.flattened = flattened;
            this.label = label;
        }
    }
}
