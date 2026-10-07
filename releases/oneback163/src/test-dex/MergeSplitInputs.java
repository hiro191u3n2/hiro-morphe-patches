import java.io.File;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;

/** Local verification input preparation only. No source APK is shipped in the release. */
public final class MergeSplitInputs {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("SPLIT_DIRECTORY OUTPUT_APK");
        try (ApkBundle bundle = new ApkBundle()) {
            bundle.loadApkDirectory(new File(args[0]), false);
            try (ApkModule merged = bundle.mergeModules(true)) {
                if (!"ctrip.english".equals(merged.getPackageName()) || merged.getVersionCode() != 85402000)
                    throw new IllegalStateException("Expected user's Trip.com 8.54.2 split set");
                merged.writeApk(new File(args[1]));
                System.out.println("Merged authorized Trip.com 8.54.2 split input: " + merged.getPackageName());
            }
        }
    }
}
