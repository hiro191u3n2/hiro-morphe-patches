import java.nio.file.Path;
import java.util.TreeMap;
import com.android.tools.smali.dexlib2.iface.Method;

/** Optional seed: preserve exact shot context across the stock watermark copy. */
public final class SeedShot1932 {
    public static final String WATERMARK =
            "Li/p/a/t/e;->d(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;IDD)Landroid/graphics/Bitmap;";

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("stock.apk output.dex");
        }
        var methods = MergePayloads.methods(MergePayloads.classes(arguments[0]).values());
        Method watermark = methods.get(WATERMARK);
        if (watermark == null || watermark.getImplementation() == null) {
            throw new IllegalStateException("Missing stock watermark method: " + WATERMARK);
        }
        var selected = new TreeMap<String, Method>();
        selected.put(WATERMARK, watermark);
        MergePayloads.writeDex(Path.of(arguments[1]), MergePayloads.holders(selected));
        System.out.println(WATERMARK + "\t" + MergePayloads.hash(watermark));
    }
}
