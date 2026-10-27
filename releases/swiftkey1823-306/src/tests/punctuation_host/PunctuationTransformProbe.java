import hiro.swiftkey.PunctuationKeys;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Runs the production transform; Python independently parses every output. */
public final class PunctuationTransformProbe {
    public static void main(String[] args) throws Exception {
        Path cases = Paths.get(args[0]), output = Paths.get(args[1]);
        Files.createDirectories(output);
        int scenarios = 0, assertions = 0;
        for (String line : Files.readAllLines(cases, StandardCharsets.UTF_8)) {
            if (line.isEmpty()) continue;
            String[] row = line.split("\t", -1);
            byte[] before = Files.readAllBytes(Paths.get(row[2]));
            byte[] snapshot = before.clone();
            String name = row[1].equals("<null>") ? null : row[1];
            byte[] after = PunctuationKeys.transform(before, name);
            assertions++;
            if (!Arrays.equals(before, snapshot)) throw new AssertionError(row[0] + ": mutated input bytes");
            assertions++;
            if ((after == before) != row[3].equals("same")) throw new AssertionError(row[0] + ": wrong transform/fallback identity");
            Files.write(output.resolve(row[0] + ".zlib"), after);
            scenarios++;
        }
        if (PunctuationKeys.transform(null, "keyboard_layout_standard_romaji") != null)
            throw new AssertionError("null data fallback");
        assertions++;
        System.out.println("REPORT {\"status\":\"PASSED\",\"scenarios\":" + (scenarios + 1)
                + ",\"assertions\":" + assertions + "}");
    }
}
