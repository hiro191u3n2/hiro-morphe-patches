package app.hiro.berry.runtime;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

/** Starts the optional Android 17 checks without replacing Berry's Application. */
public final class BerryQueueInit extends ContentProvider {
    @Override
    public boolean onCreate() {
        if (Build.VERSION.SDK_INT < 37) {
            return true;
        }
        try {
            Context context = getContext();
            Context app = context == null ? null : context.getApplicationContext();
            if (app instanceof Application) {
                Application application = (Application) app;
                application.registerActivityLifecycleCallbacks(
                        new BerryQueueLifecycle(application));
            }
        } catch (RuntimeException ignored) {
            // Diagnostics or permission prompting must never stop browser startup.
        }
        return true;
    }

    // This non-exported initialization provider exposes no data operations.
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }
}
