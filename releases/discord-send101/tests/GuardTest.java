import app.hiro.discord.patches.DiscordSendButtonPatch;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Real stock/Venus fixtures; every rejected operation must leave its source untouched. */
public final class GuardTest {
    interface Operation { void run() throws Exception; }
    private static int assertions;
    static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    static void reject(Operation operation) throws Exception { reject(operation, null); }
    static void reject(Operation operation, String message) throws Exception {
        String error = null;
        try { operation.run(); } catch (IllegalStateException expected) { error = expected.getMessage(); }
        check(error != null, "Unverified input must be rejected");
        if (message != null) check(error.contains(message), "Actionable rejection: " + error);
    }
    static String sha(File file) throws Exception {
        return DiscordSendButtonPatch.hex(DiscordSendButtonPatch.digest(file, "SHA-256", file.length()));
    }
    static void copy(File original, File candidate) throws Exception {
        Files.copy(original.toPath(), candidate.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    static void footer(File file) throws Exception {
        byte[] checksum = DiscordSendButtonPatch.digest(file, "SHA-1", file.length() - 20);
        try (RandomAccessFile output = new RandomAccessFile(file, "rw")) {
            output.seek(output.length() - 20); output.write(checksum);
        }
    }
    static void flip(File file, long offset) throws Exception {
        try (RandomAccessFile output = new RandomAccessFile(file, "rw")) {
            output.seek(offset); int value = output.read(); output.seek(offset); output.write(value ^ 1);
        }
    }
    static void unchangedRejection(File candidate) throws Exception {
        String before = sha(candidate);
        reject(() -> DiscordSendButtonPatch.repair(candidate));
        check(before.equals(sha(candidate)), "Rejected bundle left unchanged");
    }
    static ClassDef changed(ClassDef clazz, String name, boolean remove) {
        List<Method> methods = new ArrayList<>();
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(name)) { methods.add(method); continue; }
            if (remove) continue;
            MethodImplementation implementation = method.getImplementation();
            List<Instruction> instructions = new ArrayList<>();
            for (Instruction instruction : implementation.getInstructions()) instructions.add(instruction);
            // Change executable content while preserving names and visible asset strings.
            instructions.set(0, new ImmutableInstruction10x(Opcode.NOP));
            methods.add(new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                method.getReturnType(), method.getAccessFlags(), method.getAnnotations(), method.getHiddenApiRestrictions(),
                new ImmutableMethodImplementation(implementation.getRegisterCount(), instructions,
                    implementation.getTryBlocks(), Collections.emptyList())));
        }
        return new ImmutableClassDef(clazz.getType(), clazz.getAccessFlags(), clazz.getSuperclass(), clazz.getInterfaces(),
            clazz.getSourceFile(), clazz.getAnnotations(), clazz.getFields(), methods);
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("venus.bundle stock.bundle stock.apk venus.apk work-directory");
        File provider = new File(DiscordSendButtonPatch.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        System.out.println("Provider: " + provider.getCanonicalPath());
        if (System.getProperty("expected.provider") != null)
            check(provider.getCanonicalFile().equals(new File(System.getProperty("expected.provider")).getCanonicalFile()),
                "Provider loaded from the exact final MPP");
        File venus = new File(args[0]), stock = new File(args[1]);
        Path directory = Paths.get(args[4]); Files.createDirectories(directory);
        File candidate = directory.resolve("candidate.bundle").toFile();
        String venusHash = sha(venus), stockHash = sha(stock);
        check(venusHash.equals(DiscordSendButtonPatch.INPUT_SHA), "Exact provided Venus bundle");
        check(stockHash.equals(DiscordSendButtonPatch.STOCK_INPUT_SHA), "Exact provided original bundle");
        DiscordSendButtonPatch.validatePackage("com.discord", "351.11 - Stable", "351011");
        DiscordSendButtonPatch.validatePackage("com.discord.venus", "351.11 - Stable", "351011");
        for (String[] wrong : new String[][] {
            {"com.discord.other", "351.11 - Stable", "351011"},
            {"com.discord", "351.12 - Stable", "351011"},
            {"com.discord.venus", "351.11 - Stable", "351012"},
            {null, "351.11 - Stable", "351011"}
        }) reject(() -> DiscordSendButtonPatch.validatePackage(wrong[0], wrong[1], wrong[2]));

        Map<String, ClassDef> stockClasses = LoaderFixtureProbe.read(args[2]);
        Map<String, ClassDef> venusClasses = LoaderFixtureProbe.read(args[3]);
        ClassDef stockReact = stockClasses.get(LoaderFixtureProbe.REACT), stockUpdater = stockClasses.get(LoaderFixtureProbe.UPDATER);
        ClassDef venusReact = venusClasses.get(LoaderFixtureProbe.REACT), venusUpdater = venusClasses.get(LoaderFixtureProbe.UPDATER);
        DiscordSendButtonPatch.validateRuntime(venusReact, venusUpdater);
        reject(() -> DiscordSendButtonPatch.validateRuntime(stockReact, stockUpdater), "Venus settings");
        reject(() -> DiscordSendButtonPatch.validateRuntime(stockReact, venusUpdater));
        reject(() -> DiscordSendButtonPatch.validateRuntime(venusReact, stockUpdater));
        reject(() -> DiscordSendButtonPatch.validateRuntime(null, venusUpdater));
        reject(() -> DiscordSendButtonPatch.validateRuntime(venusReact, null));
        reject(() -> DiscordSendButtonPatch.validateRuntime(changed(venusReact, "loadJSBundle", true), venusUpdater));
        reject(() -> DiscordSendButtonPatch.validateRuntime(venusReact, changed(venusUpdater, "checkForUpdate", true)));
        reject(() -> DiscordSendButtonPatch.validateRuntime(changed(venusReact, "loadJSBundle", false), venusUpdater));
        reject(() -> DiscordSendButtonPatch.validateRuntime(venusReact, changed(venusUpdater, "checkForUpdate", false)));
        copy(stock, candidate);
        reject(() -> DiscordSendButtonPatch.repairWithRuntime(candidate, stockReact, stockUpdater), "Venus settings");
        check(sha(candidate).equals(stockHash), "Original-only request leaves bundle unchanged");

        reject(() -> DiscordSendButtonPatch.repair(directory.resolve("missing.bundle").toFile()));
        reject(() -> DiscordSendButtonPatch.repair(null));
        for (File original : new File[] {stock, venus}) {
            copy(original, candidate);
            try (RandomAccessFile file = new RandomAccessFile(candidate, "rw")) { file.setLength(file.length() - 1); }
            unchangedRejection(candidate);
            copy(original, candidate); flip(candidate, candidate.length() - 1); unchangedRejection(candidate);
            // Recompute footers to exercise structure/code guards, rather than only checksum failure.
            for (long offset : new long[] {
                8, 12, 40, 128, 140, 155036, 751928, 751940,
                51099376, 51666704, 53861528, 53861572,
                14916476, 26271995, 36284573, 36286037, 36285345, 36286539
            }) {
                copy(original, candidate); flip(candidate, offset); footer(candidate); unchangedRejection(candidate);
            }
            // Incompletely applied compiler path must not modify the classic path.
            copy(original, candidate);
            try (RandomAccessFile file = new RandomAccessFile(candidate, "rw")) {
                file.seek(36285345L); file.write(new byte[] {(byte)0xaf, (byte)0xe5, 0, 0, 0, 0});
            }
            footer(candidate); unchangedRejection(candidate);
            copy(original, candidate);
            // Stock-sized HBC plus final Venus DEX models execution before Venus's finalizer.
            DiscordSendButtonPatch.repairWithRuntime(candidate, venusReact, venusUpdater);
            String expected = original.equals(stock) ? DiscordSendButtonPatch.STOCK_OUTPUT_SHA : DiscordSendButtonPatch.OUTPUT_SHA;
            check(sha(candidate).equals(expected), "Exact four-byte plus footer repair");
            reject(() -> DiscordSendButtonPatch.repairWithRuntime(candidate, venusReact, venusUpdater), "適用済み");
            check(sha(candidate).equals(expected), "Duplicate application leaves output unchanged");
        }
        // The excluded global pointer must still reference a valid known Venus layout and original tail.
        for (long offset : new long[] {73802728L, 73802728L + 32, 73802728L + 40, 73802728L + 48, 73802728L - 8}) {
            copy(venus, candidate); flip(candidate, offset); footer(candidate); unchangedRejection(candidate);
        }
        check(sha(venus).equals(venusHash) && sha(stock).equals(stockHash), "Both source fixtures untouched");
        try (java.util.stream.Stream<Path> entries = Files.list(directory)) {
            check(entries.count() == 1, "All temporary repair files cleaned up");
        }
        System.out.println("PASS: " + assertions + " real-bundle, runtime and atomic-failure assertions");
    }
}
