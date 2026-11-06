package app.hiro.mxplayer.patches;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.RawResourcePatch;
import app.morphe.patcher.patch.ResourcePatchContext;

/**
 * Keep MX's native initialization and check results intact while permitting the
 * verified user-modified APK to continue past its re-sign/repack fatal branches.
 * This does not alter the Google licensing, account or payment implementation.
 */
public final class MXPlayerNativeRepair {
    private MXPlayerNativeRepair() {}
    public static final String PATH = "lib/arm64-v8a/libmx-bh.so";
    public static final int SIZE = 432304;
    public static final String ORIGINAL_SHA = "9ac4c0ffc83236b21edc16a4a37265b1d8ae28673701173cabd338eaa6d960a8";
    public static final String REPAIRED_SHA = "5b12f6cc13edca44045e6636fbcabf65051170d8dd4c23c99d21797cb375becf";
    public static final int[] OFFSETS = {0x31eb8, 0x34cdc, 0x34e48};
    public static final byte[][] BEFORE = {
        {(byte)0x21, (byte)0x0b, (byte)0x00, (byte)0x54},
        {(byte)0x21, (byte)0x0b, (byte)0x00, (byte)0x54},
        {(byte)0x21, (byte)0x0b, (byte)0x00, (byte)0x54}
    };
    public static final byte[][] AFTER = {
        {(byte)0x59, (byte)0x00, (byte)0x00, (byte)0x14},
        {(byte)0x59, (byte)0x00, (byte)0x00, (byte)0x14},
        {(byte)0x59, (byte)0x00, (byte)0x00, (byte)0x14}
    };

    // b.ne +0x164 -> b +0x164 at each verified word. The checks still run and
    // retain their actual result and flags. Only result==2 now follows the same
    // continuation as all other results, avoiding the process-destruction block.
    // BH initialization, dynamic AG registration and worker completion stay intact.
    static final RawResourcePatch PATCH = new RawResourcePatch(
        null, null, false,
        Collections.singleton(new Pair<String, Set<String>>(
            MXPlayerTagsPatch.PACKAGE, Collections.singleton(MXPlayerTagsPatch.VERSION))),
        Collections.emptySet(), Collections.emptySet(),
        new Function1<ResourcePatchContext, Unit>() {
            @Override public Unit invoke(ResourcePatchContext context) {
                MXPlayerTagsPatch.validatePackage(context.getPackageMetadata().getPackageName(),
                    context.getPackageMetadata().getVersionName(), context.getPackageMetadata().getVersionCode());
                boolean found = false;
                for (String name : context.listApkEntries("lib/")) {
                    if (name.endsWith("/libmx-bh.so")) {
                        if (!PATH.equals(name))
                            throw new IllegalStateException("MX Player launch repair is verified only for arm64-v8a; another ABI is present: " + name);
                        found = true;
                    }
                }
                if (!found)
                    throw new IllegalStateException("MX Player arm64 native split is missing. Select the complete APK/APKS including arm64-v8a, not base.apk alone.");
                try {
                    File target = context.get(PATH, true);
                    byte[] before = Files.readAllBytes(target.toPath());
                    byte[] after = repair(before);
                    if (!Arrays.equals(before, after)) Files.write(target.toPath(), after);
                } catch (IOException failure) {
                    throw new IllegalStateException("Could not save verified MX Player arm64 launch repair", failure);
                }
                return Unit.INSTANCE;
            }
        }, null);

    public static String sha256(byte[] value) {
        if (value == null) throw new IllegalStateException("Missing MX Player native library");
        try {
            StringBuilder result = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(value))
                result.append(String.format(Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot fingerprint MX Player native library", failure);
        }
    }

    public static byte[] repair(byte[] value) {
        if (value == null || value.length != SIZE)
            throw new IllegalStateException("MX Player 1.93.4 arm64 native library is missing or has an unverified size");
        if (value[0] != 0x7f || value[1] != 'E' || value[2] != 'L' || value[3] != 'F'
            || value[4] != 2 || value[5] != 1 || (value[18] & 255) != 183 || value[19] != 0)
            throw new IllegalStateException("Expected the verified little-endian AArch64 ELF library");
        String digest = sha256(value);
        if (REPAIRED_SHA.equals(digest)) return value.clone();
        if (!ORIGINAL_SHA.equals(digest))
            throw new IllegalStateException("Unverified MX Player native library; original SHA-256 does not match version 1.93.4 arm64");
        byte[] result = value.clone();
        for (int i = 0; i < OFFSETS.length; i++) {
            int off = OFFSETS[i];
            if (!Arrays.equals(Arrays.copyOfRange(value, off, off + BEFORE[i].length), BEFORE[i]))
                throw new IllegalStateException("MX Player native signature/integrity decision changed at " + Integer.toHexString(off));
            System.arraycopy(AFTER[i], 0, result, off, AFTER[i].length);
        }
        if (!REPAIRED_SHA.equals(sha256(result)))
            throw new IllegalStateException("MX Player native repair did not match the independently verified output SHA-256");
        return result;
    }
}
