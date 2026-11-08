package app.hiro.mapsbikes.patches;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Canonical complete DEX classes, including code and metadata, for exact runtime upgrades. */
public final class RuntimeContracts {
    private RuntimeContracts() {}

    public static byte[] canonical(ClassDef type) {
        MapsHideBicycleMarksPatch.require(type != null, "Missing runtime class");
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(type)));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } catch (Exception error) {
            throw new IllegalStateException("Cannot canonicalize bicycle marker runtime", error);
        } finally {
            try { store.close(); } catch (Exception ignored) {}
        }
    }

    public static String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format(Locale.ROOT, "%02x", item & 255));
            return result.toString();
        } catch (Exception error) {
            throw new IllegalStateException("Cannot fingerprint bicycle marker runtime", error);
        }
    }

    public static String classFingerprint(ClassDef type) {
        return sha256(canonical(type));
    }
}
