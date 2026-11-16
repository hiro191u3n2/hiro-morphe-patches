package app.hiro.discord.patches;

import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.RawResourcePatch;
import app.morphe.patcher.patch.ResourcePatchContext;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/** v1.0.1: guarded finalization permits original Discord plus Venus in one patch run. */
public final class DiscordSendButtonPatch {
    private DiscordSendButtonPatch() {}
    public static final String NAME = "送信ボタンの表示消失を修正";
    public static final String PACKAGE = "com.discord.venus";
    public static final String ORIGINAL_PACKAGE = "com.discord";
    public static final String VERSION = "351.11 - Stable";
    public static final String CODE = "351011";
    public static final String BUNDLE_PATH = "assets/index.android.bundle";
    public static final String REACT_CLASS = "Lcom/facebook/react/runtime/ReactInstance;";
    public static final String UPDATER_CLASS = "Lcom/discord/bundle_updater/BundleUpdater;";
    public static final String INPUT_SHA = "1d9e14c841737e987a44a876bec064ce1a029ae142835bdebd6a7e706c69f47b";
    public static final String OUTPUT_SHA = "d13cddd00337b433bd90dd0a63abd2f4e7acaffc5e86fbed96a353ba978ee5b5";
    public static final String STOCK_INPUT_SHA = "13f83cc34d48bf2002e0754cf2cea486b4e1be6b2d09b545584bd8cf0177fc73";
    public static final String STOCK_OUTPUT_SHA = "0c13c0a28c3769d3e98ff407582d34aeb236372aa64c8849cbee3da3eafc4989";
    private static final String LOADER_SHA = "83c7930686791cbeef7d1fe1f18b50b9b59b84dde7505bde3164b3144e815a93";
    private static final String OTA_SHA = "0b3d503cd0dc64936e2abf82dba1e4aac4c7ee745b1c14dc652f3d2617d51846";
    private static final String RUNTIME_ERROR = "Venusの同梱コード読込・自動更新防止処理を確認できません。元のDiscord 351.11 APKMを選び、Venus v1.21.1の「Venus settings」とこの修正を一緒に適用してください。";
    private static final int FUNCTION_COUNT = 160594;
    private static final long MIN_SIZE = 69225271L;
    private static final String HEADER_SHA = "a829a923b78776a5d141a20ce2e2ef19b0a11baf5eb8a6c7b9d8c5ae90741c2b";
    private static final String TABLE_SHA = "0f2e4d3113064f06dcbe7651ace1f1dbdbed547545f776c846eeb193f6b2175a";
    private static final int[] IDS = {12909, 62650, 62651};
    private static final long[] BODY_OFFSETS = {26271995L, 36284573L, 36286037L};
    private static final int[] BODY_SIZES = {451, 1464, 842};
    private static final String[] EXPANDED_SHA = {
        "3de85a249647a0c91481f7831dfd9bfe82fa9290467224ec171a19985cb21b41",
        "904efbf05b111f41c2cab5c046324864282c0a008d8d4dc87a66d6ca127fc9b1",
        "9edc5ecf69c638b66973c8719154a246fdd15d456cd5c4512ed2fdaab5d594fc"
    };
    private static final String[] BODY_SHA = {
        "4bb42bb97b33090922398e7476f84a6cb695255929c8845d8de57af4a94430d8",
        "8bcf62d229de6bd86e7bc6b7b974e4248cd878ac314a823a67fab8d8f8844a85",
        "2e42e88fd2a8bd61b7ed49ac2f33066c06a164d1b4179f56dc31302a1d98fecb"
    };
    private static final String[] PATCHED_BODY_SHA = {
        BODY_SHA[0], "36f00d62414cabe36eeb27b7353b2c37bcb833e7463c690d40e23db7544bf928",
        "102d42f6fd1272794116ce7ef551c4bab3687b5d7aa12cf374c2f86d32ac245a"
    };
    // Preserve relative targets and pad the removed condition with unreachable opcode 0.
    private static final long[] OFFSETS = {36285345L, 36286539L};
    private static final byte[][] BEFORE = {{(byte)0xb1, (byte)0xe5, 0, 0, 0, 8}, {(byte)0xb0, 0x60, 0x10}};
    private static final byte[][] AFTER = {{(byte)0xaf, (byte)0xe5, 0, 0, 0, 0}, {(byte)0xae, 0x60, 0}};

    // Retain only files, never contexts or DEX graphs. Both context kinds share this workspace.
    // A dependency rerun resets stale state; both finalizers remove it, including failure paths.
    private static final Map<String, File> BUNDLES = new ConcurrentHashMap<String, File>();
    private static String workspace(File file) {
        try { return file.getCanonicalPath(); }
        catch (IOException e) { throw new IllegalStateException("パッチ作業フォルダを確認できません。", e); }
    }
    private static final RawResourcePatch BUNDLE_ACCESS = new RawResourcePatch(
        null, null, false, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(),
        new Function1<ResourcePatchContext, Unit>() {
            @Override public Unit invoke(ResourcePatchContext context) {
                String key = workspace(context.getFileWorkspace());
                BUNDLES.remove(key);
                validatePackage(context.getPackageMetadata().getPackageName(),
                    context.getPackageMetadata().getVersionName(), context.getPackageMetadata().getVersionCode());
                File file = context.get(BUNDLE_PATH, true);
                if (!file.isFile()) throw new IllegalStateException("DiscordのJavaScriptファイルが見つかりません。");
                BUNDLES.put(key, file);
                return Unit.INSTANCE;
            }
        }, new Function1<ResourcePatchContext, Unit>() {
            @Override public Unit invoke(ResourcePatchContext context) {
                BUNDLES.remove(workspace(context.getFileWorkspace()));
                return Unit.INSTANCE;
            }
        });

    private static Set<Pair<String, Set<String>>> packages() {
        Set<Pair<String, Set<String>>> result = new HashSet<Pair<String, Set<String>>>();
        result.add(new Pair<String, Set<String>>(ORIGINAL_PACKAGE, Collections.singleton(VERSION)));
        result.add(new Pair<String, Set<String>>(PACKAGE, Collections.singleton(VERSION)));
        return result;
    }
    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "Discord 351.11 / Venus v1.21.1用。元のDiscord APKMでVenusの「Venus settings」や普段使う項目と一緒に適用してください。送信ボタンだけを既存のアニメーションなし表示へ切り替え、送信可否・送信操作・入力欄拡張・他のVenus設定を保持します。",
        true, packages(), Collections.<Patch>singleton(BUNDLE_ACCESS), Collections.emptySet(), null,
        new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(BytecodePatchContext context) {
                validatePackage(context.getPackageMetadata().getPackageName(),
                    context.getPackageMetadata().getVersionName(), context.getPackageMetadata().getVersionCode());
                return Unit.INSTANCE;
            }
        }, new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(BytecodePatchContext context) {
                File bundle = BUNDLES.remove(workspace(context.getFileWorkspace()));
                if (bundle == null) throw new IllegalStateException("修正対象のDiscordコードを取得できませんでした。再実行してください。");
                try {
                    repairWithRuntime(bundle, context.classDefByOrNull(REACT_CLASS), context.classDefByOrNull(UPDATER_CLASS));
                } catch (IOException e) {
                    throw new IllegalStateException("送信ボタン修正用ファイルを保存できませんでした。", e);
                }
                return Unit.INSTANCE;
            }
        });

    public static void validatePackage(String name, String version, String code) {
        if ((!PACKAGE.equals(name) && !ORIGINAL_PACKAGE.equals(name)) || !VERSION.equals(version) || !CODE.equals(code))
            throw new IllegalStateException("対象はDiscord / Venus 351.11 - Stable (351011)です。");
    }

    private static Method method(ClassDef clazz, String owner, String name, String returnType, String... params) {
        if (clazz == null || !owner.equals(clazz.getType())) throw new IllegalStateException(RUNTIME_ERROR);
        Method found = null;
        for (Method candidate : clazz.getMethods()) {
            if (!candidate.getName().equals(name) || !candidate.getReturnType().equals(returnType)
                    || candidate.getParameterTypes().size() != params.length) continue;
            boolean match = true;
            for (int i = 0; i < params.length; i++) match &= params[i].contentEquals(candidate.getParameterTypes().get(i));
            if (!match) continue;
            if (found != null) throw new IllegalStateException(RUNTIME_ERROR);
            found = candidate;
        }
        if (found == null || found.getImplementation() == null) throw new IllegalStateException(RUNTIME_ERROR);
        return found;
    }

    /** Whole-method DEX fingerprint: all instructions, operands, registers and exception handlers.
     * Debug/source names and annotations are excluded because they do not control execution. */
    public static String methodFingerprint(Method method) throws IOException {
        MethodImplementation implementation = method.getImplementation();
        if (implementation == null) throw new IllegalStateException(RUNTIME_ERROR);
        List<ImmutableMethodParameter> params = new ArrayList<ImmutableMethodParameter>();
        for (CharSequence type : method.getParameterTypes())
            params.add(new ImmutableMethodParameter(type.toString(), Collections.emptySet(), null));
        ImmutableMethod copy = new ImmutableMethod(method.getDefiningClass(), method.getName(), params,
            method.getReturnType(), method.getAccessFlags(), Collections.emptySet(), Collections.emptySet(),
            new ImmutableMethodImplementation(implementation.getRegisterCount(), implementation.getInstructions(),
                implementation.getTryBlocks(), Collections.emptyList()));
        ImmutableClassDef clazz = new ImmutableClassDef(method.getDefiningClass(), 1, "Ljava/lang/Object;",
            Collections.emptyList(), null, Collections.emptySet(), Collections.emptyList(), Collections.singletonList(copy));
        MemoryDataStore output = new MemoryDataStore();
        try {
            DexPool.writeTo(output, new ImmutableDexFile(Opcodes.forApi(26), Collections.singleton(clazz)));
            return sha(Arrays.copyOf(output.getBuffer(), output.getSize()));
        } finally { output.close(); }
    }

    public static void validateRuntime(ClassDef react, ClassDef updater) throws IOException {
        Method loader = method(react, REACT_CLASS, "loadJSBundle", "V", "Lcom/facebook/react/bridge/JSBundleLoader;");
        Method ota = method(updater, UPDATER_CLASS, "checkForUpdate", "Ljava/util/concurrent/Future;",
            "I", "Lkotlin/jvm/functions/Function0;");
        if (!LOADER_SHA.equals(methodFingerprint(loader)) || !OTA_SHA.equals(methodFingerprint(ota)))
            throw new IllegalStateException(RUNTIME_ERROR);
    }

    public static void repairWithRuntime(File target, ClassDef react, ClassDef updater) throws IOException {
        validateRuntime(react, updater);
        repair(target);
    }

    private static MessageDigest algorithm(String name) {
        try { return MessageDigest.getInstance(name); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException("Required digest unavailable", e); }
    }
    private static String sha(byte[] bytes) { return hex(algorithm("SHA-256").digest(bytes)); }
    public static byte[] digest(File file, String algorithm, long length) throws IOException {
        MessageDigest digest = algorithm(algorithm);
        byte[] buffer = new byte[1024 * 1024];
        try (InputStream input = new FileInputStream(file)) {
            long remaining = length;
            while (remaining > 0) {
                int n = input.read(buffer, 0, (int)Math.min(buffer.length, remaining));
                if (n < 0) throw new IOException("Unexpected end of bundle");
                digest.update(buffer, 0, n);
                remaining -= n;
            }
        }
        return digest.digest();
    }
    public static String hex(byte[] bytes) {
        final char[] digits = "0123456789abcdef".toCharArray();
        char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            result[2 * i] = digits[(bytes[i] & 255) >>> 4];
            result[2 * i + 1] = digits[bytes[i] & 15];
        }
        return new String(result);
    }
    private static long uint(byte[] b, int at) {
        return ((long)b[at] & 255) | (((long)b[at + 1] & 255) << 8)
            | (((long)b[at + 2] & 255) << 16) | (((long)b[at + 3] & 255) << 24);
    }
    private static byte[] read(RandomAccessFile file, long offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > file.length() - 20 - length)
            throw new IllegalStateException("Hermesコードの範囲が対応版と異なります。");
        byte[] bytes = new byte[length]; file.seek(offset); file.readFully(bytes); return bytes;
    }
    private static void require(boolean valid, String error) {
        if (!valid) throw new IllegalStateException(error);
    }

    private static void validateGlobal(RandomAccessFile file, long size) throws IOException {
        final String error = "Hermesのglobal関数が確認済みDiscord / Venusの構造と異なります。";
        final String globalBody = "652434dade35533c99407ee21160d2b5f37e7edb3f03526272b6243848f843d2";
        final String globalHeader = "7f4e3dd25cf6bb6a675d277dbc8c2ef2f4f7cff19c5d9c033366a7e129c907fd";
        byte[] originalHeader = read(file, 51099376L, 40);
        require(globalHeader.equals(sha(originalHeader)), error);
        require(globalBody.equals(sha(read(file, 14916476L, 631883))), error);
        byte[] compact = read(file, 128, 12);
        long expanded = (((uint(compact, 4) >>> 14) & 255) << 24) | (uint(compact, 0) & 0x1ffffff);
        if (expanded == 51099376L) {
            require(size == MIN_SIZE && uint(compact, 0) == 767728L && uint(compact, 4) == 49152L, error);
            return;
        }
        byte[] header = read(file, expanded, 40);
        long bodyOffset = uint(header, 0), bodySize = uint(header, 12), prefixSize = bodySize - 631883L;
        require(bodyOffset == 69225252L && prefixSize >= 11 && expanded == ((bodyOffset + bodySize + 3) & ~3L)
            && expanded + 56 == size - 20 && uint(compact, 0) == (expanded & 0xffffff)
            && uint(compact, 4) == (((expanded >>> 24) & 255) << 14), error);
        require((header[32] & 255) == 27 && (header[36] & 255) == 10, error);
        // Only the relocated offset, extended size, two new read slots and exception/debug flags change.
        System.arraycopy(originalHeader, 0, header, 0, 4);
        System.arraycopy(originalHeader, 12, header, 12, 4);
        header[32] = originalHeader[32]; header[36] = originalHeader[36];
        require(Arrays.equals(header, originalHeader), error);
        require(globalBody.equals(sha(read(file, bodyOffset + prefixSize, 631883))), error);
        byte[] exceptions = read(file, expanded + 40, 16);
        require(uint(exceptions, 0) == 1 && uint(exceptions, 4) == 0
            && uint(exceptions, 8) == prefixSize - 11 && uint(exceptions, 12) == prefixSize - 6, error);
        require(Arrays.equals(read(file, bodyOffset + prefixSize - 11, 11),
            new byte[] {(byte)175, 7, 0, 0, 0, 119, 14, (byte)147, 15, (byte)147, 16}), error);
    }

    /** Normalized structure is identical across stock and all inspected Venus feature selections.
     * Venus alone may change file length and the global function's header pointer. */
    private static void validateBundle(File target, boolean patched) throws IOException {
        long size = target.length();
        require(target.isFile() && size >= MIN_SIZE && size <= Integer.MAX_VALUE, "Discordコードが見つからないか、対応版と異なります。");
        try (RandomAccessFile file = new RandomAccessFile(target, "r")) {
            byte[] header = read(file, 0, 128);
            require(uint(header, 8) == 98 && uint(header, 32) == size && uint(header, 40) == FUNCTION_COUNT,
                "Hermesの形式・ファイル長が対応版と異なります。");
            Arrays.fill(header, 32, 36, (byte)0);
            require(HEADER_SHA.equals(sha(header)), "Hermesのヘッダーが確認済み版と異なります。");
            byte[] table = read(file, 128, FUNCTION_COUNT * 12);
            Arrays.fill(table, 0, 8, (byte)0);
            require(TABLE_SHA.equals(sha(table)), "Hermesの関数テーブルが確認済み版と異なります。");
            byte[] footer = new byte[20]; file.seek(size - 20); file.readFully(footer);
            require(Arrays.equals(footer, digest(target, "SHA-1", size - 20)), "Hermesのチェックサムが一致しません。");
            validateGlobal(file, size);
            for (int i = 0; i < IDS.length; i++) {
                byte[] compact = read(file, 128L + IDS[i] * 12L, 12);
                require((compact[11] & 0x20) != 0, "送信ボタンの関数ヘッダー形式が一致しません。");
                long expanded = (((uint(compact, 4) >>> 14) & 255) << 24) | (uint(compact, 0) & 0x1ffffff);
                byte[] full = read(file, expanded, 40);
                require(EXPANDED_SHA[i].equals(sha(full)) && uint(full, 0) == BODY_OFFSETS[i] && uint(full, 12) == BODY_SIZES[i],
                    "送信ボタンの関数ヘッダーが確認済み版と異なります。");
                String body = sha(read(file, BODY_OFFSETS[i], BODY_SIZES[i]));
                if (!patched && i > 0 && PATCHED_BODY_SHA[i].equals(body))
                    throw new IllegalStateException("この送信ボタン修正は適用済みです。修正前のAPKを選んでください。");
                require((patched ? PATCHED_BODY_SHA[i] : BODY_SHA[i]).equals(body),
                    "送信ボタンのコードが確認済み版と異なります。適用を中止しました。");
            }
            for (int i = 0; i < OFFSETS.length; i++)
                require(Arrays.equals(read(file, OFFSETS[i], BEFORE[i].length), patched ? AFTER[i] : BEFORE[i]),
                    "送信ボタンの表示分岐が一致しません。");
        }
    }

    /** HBC primitive, called only after the final DEX runtime guard by the public patch. */
    public static void repair(File target) throws IOException {
        require(target != null && target.isFile(), "DiscordのJavaScriptファイルが見つかりません。");
        validateBundle(target, false);
        long size = target.length();
        File temporary = File.createTempFile("hiro-discord-send-", ".tmp", target.getAbsoluteFile().getParentFile());
        try {
            Files.copy(target.toPath(), temporary.toPath(), StandardCopyOption.REPLACE_EXISTING);
            try (RandomAccessFile output = new RandomAccessFile(temporary, "rw")) {
                for (int i = 0; i < OFFSETS.length; i++) { output.seek(OFFSETS[i]); output.write(AFTER[i]); }
                byte[] checksum = digest(temporary, "SHA-1", size - 20);
                output.seek(size - 20); output.write(checksum);
            }
            validateBundle(temporary, true);
            require(temporary.length() == size, "送信ボタン修正後のファイル長が一致しません。");
            verifyOnlyExpectedChanges(target, temporary, size);
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary.toPath()); }
    }

    private static void verifyOnlyExpectedChanges(File before, File after, long size) throws IOException {
        try (RandomAccessFile left = new RandomAccessFile(before, "r"); RandomAccessFile right = new RandomAccessFile(after, "r")) {
            byte[] a = new byte[65536], b = new byte[65536];
            for (long start = 0; start < size - 20;) {
                int length = (int)Math.min(a.length, size - 20 - start);
                left.readFully(a, 0, length); right.readFully(b, 0, length);
                for (int i = 0; i < length; i++) if (a[i] != b[i]) {
                    long offset = start + i;
                    require(offset == OFFSETS[0] || offset == OFFSETS[0] + 5 || offset == OFFSETS[1] || offset == OFFSETS[1] + 2,
                        "送信ボタン以外のコード変更を検出したため、保存を中止しました。");
                }
                start += length;
            }
        }
    }
}
