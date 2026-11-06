import java.io.File;
import app.morphe.patcher.apk.ApkMerger;

/** Use the same split merger and defaults as Morphe 2.2's patch pipeline. */
public final class MergeInput {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("MergeInput input.apks output.apk");
        new ApkMerger().merge(new File(args[0]), new File(args[1]), true, null, true, null, false);
        if (!new File(args[1]).isFile()) throw new IllegalStateException("Merged APK missing");
        System.out.println("PASS Morphe 2.2 split merge");
    }
}
