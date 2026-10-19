package app.hiro.fossifygallery.runtime;

import android.app.Activity;

/** Runs only after Gallery's original AndroidX callback reports unconsumed back. */
public final class GalleryExit {
    private GalleryExit() {}

    public static boolean tryExit(Activity activity) {
        if (activity == null) return false;
        try {
            if (!"org.fossify.gallery".equals(activity.getPackageName()) || !activity.isTaskRoot()) return false;
            // A nested photo/folder/picker must follow the native back path. At
            // the root this removes only the current Gallery task and no caller.
            activity.finishAndRemoveTask();
            return true;
        } catch (RuntimeException unavailable) {
            // Preserve the application's native fallback if removal is rejected.
            return false;
        }
    }
}
