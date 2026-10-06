package ctrip.android.pkg.util;
import android.content.Context;
import java.nio.file.*;
public final class Un7zUtil {
    public static Path fixture;
    public static int calls;
    public static String mode = "ok";
    public static String archive;
    public static boolean extractAssets(Context context, String input, String output) throws Exception {
        calls++;
        archive = input;
        if ("throw".equals(mode)) throw new java.io.IOException("test extractor error");
        if ("fail".equals(mode)) return false;
        Path directory = Paths.get(output, "rn_xtaro_ibu_schedule");
        Files.createDirectories(directory);
        byte[] bytes = Files.readAllBytes(fixture);
        if ("corrupt".equals(mode)) bytes[0] ^= 1;
        Files.write(directory.resolve("rn_business.jsbundle"), bytes);
        if ("extra".equals(mode)) Files.write(directory.resolve("rn_business.hbcbundle"), new byte[]{1, 2});
        return true;
    }
}
