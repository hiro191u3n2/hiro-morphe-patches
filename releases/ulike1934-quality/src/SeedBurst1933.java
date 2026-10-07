import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.Method;

/** Extract only the exact full-resolution still reader callback from the approved APK. */
public final class SeedBurst1933 {
    static final String STILL = "Li/s/a/w/d0/a$g;->onImageAvailable(Landroid/media/ImageReader;)V";
    public static void main(String[] arguments) throws Exception {
        MergePayloads.require(arguments.length == 3, "original-base.apk output.dex hashes.tsv");
        var methods = MergePayloads.methods(MergePayloads.classes(arguments[0]).values());
        Method original = methods.get(STILL);
        MergePayloads.require(original != null && original.getImplementation() != null, "Missing real still reader callback");
        var selected = new TreeMap<String, Method>();
        selected.put(STILL, original);
        MergePayloads.writeDex(Path.of(arguments[1]), MergePayloads.holders(selected));
        String row = STILL + "\t" + MergePayloads.hash(original) + "\n";
        Files.writeString(Path.of(arguments[2]), row);
        System.out.print(row);
    }
}
