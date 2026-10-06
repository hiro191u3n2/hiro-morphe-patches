package app.hiro.tripcom.extension;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.channels.FileLock;
import java.security.MessageDigest;

/** Pins only the MyPlan business bundle to the reviewed bytes shipped in this APK. */
public final class MyPlanBundleGuard {
    private static final String PRODUCT = "rn_xtaro_ibu_schedule";
    private static final String ARCHIVE = "webapp/rn_xtaro_ibu_schedule-431156475-30041599.7z";
    private static final String BUNDLE = "rn_business.jsbundle";
    private static final String SHA256 = "0194baa9f2e652fc88d7a97de72a5f80993019b7d32743f768e2f0690f7318bc";
    private static final long LENGTH = 971104L;
    private static final String DIRECTORY = PRODUCT + "_hiro_v1_10_15_0194baa9f2e6";
    private static volatile File readyDirectory;

    private MyPlanBundleGuard() { }

    /** Entry hook of ReactInstance.loadBusinessScript; all other products retain their path. */
    public static String loadPath(String original, String product) {
        if (!PRODUCT.equals(product) || !isProductDirectory(original)) return original;
        try {
            File ready = ensure(new File(original));
            return ready == null ? original : ready.getAbsolutePath();
        } catch (Throwable ignored) {
            return original;
        }
    }

    /** Returns null to run the original CRNURL resolver for every unrelated or failed case. */
    public static String businessPath(String originalUrl) {
        if (originalUrl == null) return null;
        try {
            String path = originalUrl;
            if (path.startsWith("file://")) path = path.substring(7);
            int query = path.indexOf('?');
            if (query >= 0) path = path.substring(0, query);
            int fragment = path.indexOf('#');
            if (fragment >= 0) path = path.substring(0, fragment);
            int slash = path.lastIndexOf('/');
            if (slash < 1) return null;
            String directory = path.substring(0, slash);
            if (!isProductDirectory(directory)) return null;
            File ready = ensure(new File(directory));
            return ready == null ? null : new File(ready, BUNDLE).getAbsolutePath();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Capture before the native call overwrites its path parameter with the return value. */
    public static boolean isPinnedPath(String path, String product) {
        if (!PRODUCT.equals(product) || path == null) return false;
        File ready = readyDirectory;
        return ready != null && ready.getAbsolutePath().equals(path);
    }

    /** Result 1 is successful JS fallback; initialize its resource key without changing it. */
    public static boolean resourceKeySucceeded(int result, boolean pinned) {
        return result == 0 || (result == 1 && pinned);
    }

    private static boolean isProductDirectory(String path) {
        return path != null && path.startsWith("/")
                && PRODUCT.equals(new File(path).getName());
    }

    private static synchronized File ensure(File original) throws Exception {
        Context context = (Context) Class.forName("ctrip.foundation.FoundationContextHolder")
                .getField("context").get(null);
        if (context == null) return null;
        Context application = context.getApplicationContext();
        if (application != null) context = application;
        File parent = original.getCanonicalFile().getParentFile();
        if (parent == null || !parent.isDirectory()) return null;
        // Native loadBusinessScript finds business common dependencies as siblings of the
        // business directory. Keep that parent intact; a separate Context.getDir root
        // would strand those dependencies. The new sibling is still private app storage.
        File privateRoot = context.getFilesDir().getCanonicalFile().getParentFile();
        if (privateRoot == null || !parent.getPath().startsWith(privateRoot.getPath() + File.separator)) {
            return null;
        }
        String parentName = parent.getName();
        if (!parentName.equals("app_ctripwebapp6") && !parentName.startsWith("app_ctripwebapp6_")) {
            return null;
        }
        File target = new File(parent, DIRECTORY);
        if (!target.getCanonicalFile().equals(target)) return null;
        File cached = readyDirectory;
        if (target.equals(cached) && valid(target)) return cached;
        File lockFile = new File(parent, DIRECTORY + ".lock");
        if (!lockFile.getCanonicalFile().equals(lockFile)) return null;
        try (RandomAccessFile lockHandle = new RandomAccessFile(lockFile, "rw");
                FileLock lock = lockHandle.getChannel().lock()) {
            if (valid(target)) {
                readyDirectory = target;
                return target;
            }
            // Never erase a normal product work directory, account data, or sibling library.
            if (target.exists() && !removeBundleDirectory(target)) return null;
            File staging = File.createTempFile(DIRECTORY + "_extract_", ".tmp", parent);
            if (!staging.delete() || !staging.mkdir()) return null;
            File extracted = new File(staging, PRODUCT);
            try {
                Method extractor = Class.forName("ctrip.android.pkg.util.Un7zUtil").getMethod(
                        "extractAssets", Context.class, String.class, String.class);
                Object success = extractor.invoke(null, context, ARCHIVE, staging.getAbsolutePath());
                if (!Boolean.TRUE.equals(success) || !valid(extracted)) return null;
                File[] top = staging.listFiles();
                if (top == null || top.length != 1 || !top[0].equals(extracted)) return null;
                if (!extracted.renameTo(target)) return null;
                readyDirectory = target;
                return target;
            } finally {
                removeBundleDirectory(extracted);
                staging.delete();
            }
        }
    }

    private static boolean valid(File directory) throws Exception {
        if (!directory.isDirectory()) return false;
        File[] files = directory.listFiles();
        if (files == null || files.length != 1 || !BUNDLE.equals(files[0].getName())) return false;
        File bundle = files[0];
        if (!bundle.isFile() || bundle.length() != LENGTH
                || !bundle.getCanonicalFile().getParentFile().equals(directory.getCanonicalFile())) {
            return false;
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(bundle)) {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        byte[] actual = digest.digest();
        for (int index = 0; index < actual.length; index++) {
            int expected = Integer.parseInt(SHA256.substring(index * 2, index * 2 + 2), 16);
            if ((actual[index] & 255) != expected) return false;
        }
        return true;
    }

    /** Delete only this implementation's single-file directory, never recursively. */
    private static boolean removeBundleDirectory(File directory) {
        if (!directory.exists()) return true;
        File[] files = directory.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (!BUNDLE.equals(file.getName()) || file.isDirectory()) return false;
        }
        for (File file : files) if (!file.delete()) return false;
        return directory.delete();
    }
}
