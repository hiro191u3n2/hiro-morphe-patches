import hiro.swiftkey.JapaneseGlideDecoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/** Run in a fresh JVM: a rejected asset must not publish a partially loaded index. */
public final class DecoderAssetValidationTest {
    static int checks;
    static void reject(byte[] bytes, String description) throws Exception {
        checks++;
        try {
            JapaneseGlideDecoder.load(new ByteArrayInputStream(bytes));
            throw new AssertionError("accepted " + description);
        } catch (IOException expected) {
            if (JapaneseGlideDecoder.isReady()) throw new AssertionError("published invalid " + description);
        }
    }
    public static void main(String[] args) throws Exception {
        byte[] original = Files.readAllBytes(Paths.get(args[0]));
        reject(new byte[0], "empty input");
        reject(Arrays.copyOf(original, 2935), "truncated header");
        byte[] bad = Arrays.copyOf(original, 2936);
        bad[0] = 'X'; reject(bad, "wrong magic");
        bad = Arrays.copyOf(original, 2936);
        Arrays.fill(bad, 12, 16, (byte)0x7f); reject(bad, "excess allocation request");
        reject(Arrays.copyOf(original, original.length - 1), "truncated payload");
        bad = original.clone(); bad[bad.length - 1] ^= 1; reject(bad, "digest mismatch");
        reject(Arrays.copyOf(original, original.length + 1), "trailing data");
        JapaneseGlideDecoder.load(new ByteArrayInputStream(original));
        checks++;
        if (!JapaneseGlideDecoder.isReady()) throw new AssertionError("valid asset rejected after failures");
        System.out.println("PASSED " + checks + " decoder asset assertions");
    }
}
