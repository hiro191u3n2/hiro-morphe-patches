/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only
 * Independent Japanese companion for icysymmetra/tiktok-patches-for-morphe.
 * See the accompanying LICENSE and NOTICE for upstream notices.
 */
package app.hiro.tiktokjapanese.patches;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Add one language without rewriting native TikTok code or existing translations. */
public final class TikTokJapanesePatch {
    public static final String NAME = "TikTok設定を日本語化";
    public static final String TARGET = "Lapp/morphe/extension/tiktok/settings/L10nTranslations;";
    public static final String RUNTIME = "Lapp/hiro/tiktokjapanese/runtime/JapaneseTranslations;";
    public static final String ORIGINAL = "hiroJapanese$originalOf";
    private static final List<String> ARGS = Collections.singletonList("Ljava/lang/String;");
    private static final String MAP = "Ljava/util/Map;";
    private TikTokJapanesePatch() {}

    private static Set<Pair<String, Set<String>>> compatible() {
        return Collections.singleton(new Pair<String, Set<String>>(
            "com.zhiliaoapp.musically", Collections.singleton("47.4.3")));
    }
    public static void validatePackage(String value) {
        if (!"com.zhiliaoapp.musically".equals(value))
            throw new IllegalStateException("TikTok設定の日本語化はTikTok 47.4.3（com.zhiliaoapp.musically）専用です: " + value);
    }
    /** Enforced during patch execution even when the Manager/CLI ignores compatibility. */
    public static void validateTarget(String packageName, String versionName) {
        validatePackage(packageName);
        if (!"47.4.3".equals(versionName))
            throw new IllegalStateException("TikTok設定の日本語化はTikTok 47.4.3専用です: " + versionName);
    }
    // Run before Morphe merges the dictionary extension, including forced selection.
    private static final BytecodePatch TARGET_ADMISSION = new BytecodePatch(null, null, false,
        compatible(), Collections.emptySet(), Collections.emptySet(), null,
        new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(BytecodePatchContext context) {
                validateTarget(context.getPackageMetadata().getPackageName(), context.getPackageMetadata().getVersionName());
                return Unit.INSTANCE;
            }
        }, null);
    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "TikTok 47.4.3のMetraの設定・説明・ダイアログ等2,181項目を日本語化。icysymmetra版TikTokパッチ v0.9.0-dev.1のSettingsと一緒に選択してください。端末／アプリの言語が日本語の場合に表示します。変更履歴・ライセンス原文・国名などの固有名は対象外。",
        false, compatible(), Collections.singleton(TARGET_ADMISSION), Collections.emptySet(), new Supplier<InputStream>() {
            @Override public InputStream get() {
                InputStream in = TikTokJapanesePatch.class.getResourceAsStream("/extensions/hiro_tiktok_japanese.mpe");
                if (in == null) throw new IllegalStateException("日本語辞書の拡張DEXがありません");
                return in;
            }
        }, new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(BytecodePatchContext context) {
                validateTarget(context.getPackageMetadata().getPackageName(), context.getPackageMetadata().getVersionName());
                return Unit.INSTANCE;
            }
        }, new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(BytecodePatchContext context) {
                validateTarget(context.getPackageMetadata().getPackageName(), context.getPackageMetadata().getVersionName());
                // All selected patches have executed before finalization. This allows
                // either MPP source order, including a freshly merged Metra extension.
                MutableClass target = context.mutableClassDefByOrNull(TARGET);
                if (target == null) throw new IllegalStateException(
                    "Metraの多言語設定が見つかりません。icysymmetra版TikTokパッチ v0.9.0-dev.1のSettingsと「TikTok設定を日本語化」を一緒に選択してください。v0.8.1は非対応です。");
                if (context.classDefByOrNull(RUNTIME) == null)
                    throw new IllegalStateException("日本語辞書が組み込まれていません");
                boolean added = apply(target);
                System.out.println("TikTok Japanese: 2181 catalog entries; " + (added ? "connected" : "already connected"));
                return Unit.INSTANCE;
            }
        });

    private static boolean signature(MutableMethod method, String name) {
        return name.equals(method.getName()) && MAP.equals(method.getReturnType())
            && method.getParameterTypes().size() == 1
            && ARGS.get(0).contentEquals(method.getParameterTypes().get(0))
            && (method.getAccessFlags() & 8) != 0 && method.getImplementation() != null;
    }
    private static ImmutableMethodReference ref(String owner, String name) {
        return new ImmutableMethodReference(owner, name, ARGS, MAP);
    }
    private static MutableMethod wrapper(String owner, int flags) {
        MutableMethodImplementation code = new MutableMethodImplementation(2);
        code.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 1, 0, 0, 0, 0, ref(RUNTIME, "forLanguage")));
        code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        code.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
        code.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 1, 0, 0, 0, 0, ref(owner, ORIGINAL)));
        code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        code.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
        Label fallback = code.newLabelForIndex(3);
        code.addInstruction(2, new BuilderInstruction21t(Opcode.IF_EQZ, 0, fallback));
        return new MutableMethod(new ImmutableMethod(owner, "of",
            Collections.singletonList(new ImmutableMethodParameter(ARGS.get(0), Collections.emptySet(), null)),
            MAP, flags, Collections.emptySet(), Collections.emptySet(), code));
    }
    private static boolean validWrapper(MutableMethod value) {
        List<BuilderInstruction> code = value.getImplementation().getInstructions();
        if (value.getImplementation().getRegisterCount() != 2 || code.size() != 7
            || code.get(0).getOpcode() != Opcode.INVOKE_STATIC
            || code.get(1).getOpcode() != Opcode.MOVE_RESULT_OBJECT
            || code.get(2).getOpcode() != Opcode.IF_EQZ
            || code.get(3).getOpcode() != Opcode.RETURN_OBJECT
            || code.get(4).getOpcode() != Opcode.INVOKE_STATIC
            || code.get(5).getOpcode() != Opcode.MOVE_RESULT_OBJECT
            || code.get(6).getOpcode() != Opcode.RETURN_OBJECT) return false;
        for (int index : new int[]{1, 2, 3, 5, 6})
            if (((OneRegisterInstruction) code.get(index)).getRegisterA() != 0) return false;
        Reference first = ((ReferenceInstruction) code.get(0)).getReference();
        Reference next = ((ReferenceInstruction) code.get(4)).getReference();
        return ref(RUNTIME, "forLanguage").equals(first) && ref(value.getDefiningClass(), ORIGINAL).equals(next)
            && ((OffsetInstruction) code.get(2)).getCodeOffset() == 3
            && ((FiveRegisterInstruction) code.get(0)).getRegisterC() == 1
            && ((FiveRegisterInstruction) code.get(4)).getRegisterC() == 1
            && ((FiveRegisterInstruction) code.get(0)).getRegisterCount() == 1
            && ((FiveRegisterInstruction) code.get(4)).getRegisterCount() == 1;
    }
    /** Returns false only for this exact previously installed wrapper. */
    public static boolean apply(MutableClass target) {
        if (!TARGET.equals(target.getType())) throw new IllegalStateException("Unexpected translation class");
        MutableMethod original = null, previous = null;
        for (MutableMethod method : target.getMethods()) {
            if (signature(method, "of")) {
                if (original != null) throw new IllegalStateException("Duplicate translation entry point");
                original = method;
            }
            if (ORIGINAL.equals(method.getName())) {
                if (previous != null || !signature(method, ORIGINAL)) throw new IllegalStateException("Unknown Japanese hook collision");
                previous = method;
            }
        }
        if (original == null) throw new IllegalStateException("Metraの多言語APIが非対応です。確認済み: v0.9.0-dev.1");
        if (previous != null) {
            if (!validWrapper(original)) throw new IllegalStateException("既存の日本語化フックが想定と異なります");
            return false;
        }
        MutableMethod replacement = wrapper(target.getType(), original.getAccessFlags());
        // Remove before renaming: methods may live in a hash-backed collection.
        target.getMethods().remove(original);
        original.setName(ORIGINAL);
        target.getMethods().add(original);
        target.getMethods().add(replacement);
        return true;
    }
}
