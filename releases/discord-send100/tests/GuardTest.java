import app.hiro.discord.patches.DiscordSendButtonPatch;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class GuardTest {
    interface Operation { void run() throws Exception; }
    private static int assertions;
    static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    static void reject(Operation op) throws Exception {
        boolean rejected = false;
        try { op.run(); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected, "Unverified input must be rejected");
    }
    static String sha(File file) throws Exception {
        return DiscordSendButtonPatch.hex(DiscordSendButtonPatch.digest(file, "SHA-256", file.length()));
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("bundle work-directory");
        final File original = new File(args[0]);
        Path dir = Paths.get(args[1]); Files.createDirectories(dir);
        String originalHash = sha(original);
        check(originalHash.equals(DiscordSendButtonPatch.INPUT_SHA), "Actual provided bundle");
        DiscordSendButtonPatch.validatePackage("com.discord.venus", "351.11 - Stable", "351011");
        for (final String[] wrong : new String[][] {
            {"com.discord", "351.11 - Stable", "351011"},
            {"com.discord.venus", "351.12 - Stable", "351011"},
            {"com.discord.venus", "351.11 - Stable", "351012"},
            {null, "351.11 - Stable", "351011"}
        }) reject(new Operation() { public void run() { DiscordSendButtonPatch.validatePackage(wrong[0], wrong[1], wrong[2]); } });
        final File candidate = dir.resolve("candidate.bundle").toFile();
        Files.copy(original.toPath(), candidate.toPath(), StandardCopyOption.REPLACE_EXISTING);
        try (RandomAccessFile file = new RandomAccessFile(candidate, "rw")) { file.setLength(file.length() - 1); }
        final String truncatedHash = sha(candidate);
        reject(new Operation() { public void run() throws Exception { DiscordSendButtonPatch.repair(candidate); } });
        check(sha(candidate).equals(truncatedHash), "Truncated input unchanged");
        Files.copy(original.toPath(), candidate.toPath(), StandardCopyOption.REPLACE_EXISTING);
        try (RandomAccessFile file = new RandomAccessFile(candidate, "rw")) { file.seek(40000000); int value = file.read(); file.seek(40000000); file.write(value ^ 1); }
        String mutatedHash = sha(candidate);
        reject(new Operation() { public void run() throws Exception { DiscordSendButtonPatch.repair(candidate); } });
        check(sha(candidate).equals(mutatedHash), "Unknown code unchanged");
        Files.copy(original.toPath(), candidate.toPath(), StandardCopyOption.REPLACE_EXISTING);
        DiscordSendButtonPatch.repair(candidate);
        check(sha(candidate).equals(DiscordSendButtonPatch.OUTPUT_SHA), "Exact repaired output");
        reject(new Operation() { public void run() throws Exception { DiscordSendButtonPatch.repair(candidate); } });
        check(sha(candidate).equals(DiscordSendButtonPatch.OUTPUT_SHA), "Duplicate application unchanged");
        check(sha(original).equals(originalHash), "Original fixture untouched");
        try (java.util.stream.Stream<Path> entries = Files.list(dir)) { check(entries.count() == 1, "Temporary repair files cleaned up"); }
        System.out.println("PASS: " + assertions + " guard and real-bundle assertions");
    }
}
