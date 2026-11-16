package app.hiro.discord.patches;

import app.morphe.patcher.patch.RawResourcePatch;
import app.morphe.patcher.patch.ResourcePatchContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/** Select Discord's existing static send-button renderer in the inspected Venus bundle. */
public final class DiscordSendButtonPatch {
    private DiscordSendButtonPatch() {}
    public static final String NAME = "送信ボタンの表示消失を修正";
    public static final String PACKAGE = "com.discord.venus";
    public static final String VERSION = "351.11 - Stable";
    public static final String CODE = "351011";
    public static final String BUNDLE_PATH = "assets/index.android.bundle";
    public static final long SIZE = 73802804L;
    public static final String INPUT_SHA = "1d9e14c841737e987a44a876bec064ce1a029ae142835bdebd6a7e706c69f47b";
    public static final String OUTPUT_SHA = "d13cddd00337b433bd90dd0a63abd2f4e7acaffc5e86fbed96a353ba978ee5b5";
    // ChatInputSendButton: compiler variant fn62650 and classic variant fn62651.
    // Keep the relative jump, remove only its condition, pad with unreachable opcode 0.
    // Neither function has an incoming edge into these instruction interiors.
    private static final long[] OFFSETS = {36285345L, 36286539L};
    private static final byte[][] BEFORE = {
        {(byte)0xb1, (byte)0xe5, 0, 0, 0, 8},
        {(byte)0xb0, 0x60, 0x10}
    };
    private static final byte[][] AFTER = {
        {(byte)0xaf, (byte)0xe5, 0, 0, 0, 0},
        {(byte)0xae, 0x60, 0}
    };

    public static final RawResourcePatch PATCH = new RawResourcePatch(
        NAME,
        "Venus v1.21.1 / Discord 351.11用。送信ボタンだけを既存のアニメーションなし表示へ切り替えます。送信可否・送信操作・入力欄拡張・他のVenus設定を保持。確認済みのVenus APKへ、この項目だけ追加適用してください。",
        true,
        Collections.singleton(new Pair<String, Set<String>>(PACKAGE, Collections.singleton(VERSION))),
        Collections.emptySet(), Collections.emptySet(),
        new Function1<ResourcePatchContext, Unit>() {
            @Override public Unit invoke(ResourcePatchContext context) {
                validatePackage(context.getPackageMetadata().getPackageName(),
                    context.getPackageMetadata().getVersionName(), context.getPackageMetadata().getVersionCode());
                try {
                    repair(context.get(BUNDLE_PATH, true));
                } catch (IOException e) {
                    throw new IllegalStateException("送信ボタン修正用ファイルを保存できませんでした。", e);
                }
                return Unit.INSTANCE;
            }
        }, null);

    public static void validatePackage(String name, String version, String code) {
        if (!PACKAGE.equals(name) || !VERSION.equals(version) || !CODE.equals(code))
            throw new IllegalStateException("対象は確認済みのVenus v1.21.1 / 351.11 - Stable (351011)です。");
    }

    public static byte[] digest(File file, String algorithm, long length) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[1024 * 1024];
            try (InputStream in = new FileInputStream(file)) {
                long remaining = length;
                while (remaining > 0) {
                    int n = in.read(buffer, 0, (int)Math.min(buffer.length, remaining));
                    if (n < 0) throw new IOException("Unexpected end of bundle");
                    digest.update(buffer, 0, n);
                    remaining -= n;
                }
            }
            return digest.digest();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Required digest unavailable", e);
        }
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

    /** Stream all checks, mutate a temporary copy, and replace only after exact output verification. */
    public static void repair(File target) throws IOException {
        if (target == null || !target.isFile() || target.length() != SIZE)
            throw new IllegalStateException("対象のVenus JavaScriptファイルが見つからないか、対応版と異なります。");
        String input = hex(digest(target, "SHA-256", SIZE));
        if (OUTPUT_SHA.equals(input))
            throw new IllegalStateException("この送信ボタン修正は適用済みです。修正前のVenus APKを選んでください。");
        if (!INPUT_SHA.equals(input))
            throw new IllegalStateException("Venusのコードが確認済み版と異なります。別バージョンや他の追加改造への適用は中止します。");
        try (RandomAccessFile original = new RandomAccessFile(target, "r")) {
            byte[] footer = new byte[20];
            original.seek(SIZE - 20);
            original.readFully(footer);
            if (!Arrays.equals(footer, digest(target, "SHA-1", SIZE - 20)))
                throw new IllegalStateException("Hermes checksum mismatch");
            for (int i = 0; i < OFFSETS.length; i++) {
                byte[] old = new byte[BEFORE[i].length];
                original.seek(OFFSETS[i]);
                original.readFully(old);
                if (!Arrays.equals(old, BEFORE[i]))
                    throw new IllegalStateException("送信ボタンの表示分岐が一致しません。");
            }
        }
        File temporary = File.createTempFile("hiro-discord-send-", ".tmp", target.getAbsoluteFile().getParentFile());
        try {
            Files.copy(target.toPath(), temporary.toPath(), StandardCopyOption.REPLACE_EXISTING);
            try (RandomAccessFile output = new RandomAccessFile(temporary, "rw")) {
                for (int i = 0; i < OFFSETS.length; i++) {
                    output.seek(OFFSETS[i]);
                    output.write(AFTER[i]);
                }
            }
            byte[] checksum = digest(temporary, "SHA-1", SIZE - 20);
            try (RandomAccessFile output = new RandomAccessFile(temporary, "rw")) {
                output.seek(SIZE - 20);
                output.write(checksum);
            }
            if (temporary.length() != SIZE || !OUTPUT_SHA.equals(hex(digest(temporary, "SHA-256", SIZE))))
                throw new IllegalStateException("送信ボタン修正の出力が検証済み結果と一致しません。");
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }
}
