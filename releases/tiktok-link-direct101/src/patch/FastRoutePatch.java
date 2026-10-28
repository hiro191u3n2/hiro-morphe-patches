/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktoklink.patches;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Native-route changes for the existing shared-link patch, not a separate patch.
 * All admission and helper construction finish before any application code changes.
 * No runtime extension, URL rewriting, player hook, or manual Activity launch is added.
 */
public final class FastRoutePatch {
    public static final String OWNER = "LX/1SyD;";
    public static final String STARTUP = "LX/1SxQ;";
    public static final String HELPER = "hiroShouldOpenSharedDetail";
    public static final String EXCLUSION = "hiroHasSharedRouteExclusion";
    private static final String URI = "Landroid/net/Uri;";
    private static final String INTENT = "Landroid/content/Intent;";
    private static final String ACTIVITY = "Landroid/app/Activity;";
    private static final String STRING = "Ljava/lang/String;";
    private static final String TEXT = "Landroid/text/TextUtils;";
    private static final String PROTECTOR = "Lcom/bytedance/mt/protector/impl/UriProtector;";
    private static final String QUERY = "com_bytedance_mt_protector_impl_UriProtector_com_ss_android_ugc_aweme_lancet_UriLancet_getQueryParameter";
    private static final String[] EXCLUDED = {"group_invite_id", "jump_to_search_detail",
        "is_from_drama_tab_deeplink", "forbid_insert_feed", "tab_index"};
    private static final int SYNTHETIC_STATIC_PRIVATE = 0x100a;

    // Full-body semantic fingerprints generated from the signed original 47.4.3 APK.
    // Cover signature, access flags, registers, all instructions/operands/references,
    // resolved branch targets and exception handlers. DEX pool indices and debug
    // formatting are not admission criteria; the existing metadata is left intact.
    public static final String STARTUP_SHA256 = "6cb9c2e49ca51afc3bbade884da8bececcc3836ae6571123abf316fa2a56c6cb";
    public static final String INTENT_SHA256 = "21e603d6c69b6bc8018505885e706a44cea001738fe43b3c4451d3a3ed712b7e";
    public static final String CLASSIFIER_SHA256 = "c7c1935f8c398e0caf1b6d9c4aaa72b73bbb77403a1338c6c80470a9ca37ae6a";
    public static final String MARKER_SHA256 = "24e4a970baea4b83aabc9aa2d022e83ce31144d9169dbc890bd73ec91d03119a";

    private FastRoutePatch() {}
    private static void require(boolean ok, String why) { TikTokLinkDirectPatch.require(ok, why); }

    /** Canonical full-body semantics, independent of Morphe's DEX writer version. */
    public static String fingerprint(Method method) {
        require(method != null && method.getImplementation() != null, "cannot fingerprint missing native method");
        MethodImplementation body = new MutableMethod(method).getImplementation();
        StringBuilder text = new StringBuilder(method.getDefiningClass()).append("->").append(method.getName()).append('(');
        for (CharSequence parameter : method.getParameterTypes()) text.append(parameter);
        text.append(')').append(method.getReturnType()).append(" flags=").append(method.getAccessFlags())
            .append(" registers=").append(body.getRegisterCount()).append('\n');
        List<Instruction> instructions = new ArrayList<>();
        Map<Integer,Integer> indices = new HashMap<>();
        int pc = 0;
        for (Instruction i : body.getInstructions()) {
            indices.put(pc, instructions.size()); instructions.add(i); pc += i.getCodeUnits();
        }
        indices.put(pc, instructions.size()); pc = 0;
        for (Instruction i : instructions) {
            Opcode op = i.getOpcode();
            text.append(op == Opcode.CONST_STRING_JUMBO ? Opcode.CONST_STRING
                : op == Opcode.GOTO_16 || op == Opcode.GOTO_32 ? Opcode.GOTO : op);
            if (i instanceof OneRegisterInstruction) text.append(" a=").append(((OneRegisterInstruction)i).getRegisterA());
            if (i instanceof TwoRegisterInstruction) text.append(" b=").append(((TwoRegisterInstruction)i).getRegisterB());
            if (i instanceof ThreeRegisterInstruction) text.append(" c=").append(((ThreeRegisterInstruction)i).getRegisterC());
            if (i instanceof FiveRegisterInstruction) {
                FiveRegisterInstruction r = (FiveRegisterInstruction)i;
                text.append(" args=").append(r.getRegisterCount()).append(',').append(r.getRegisterC())
                    .append(',').append(r.getRegisterD()).append(',').append(r.getRegisterE())
                    .append(',').append(r.getRegisterF()).append(',').append(r.getRegisterG());
            }
            if (i instanceof RegisterRangeInstruction) {
                RegisterRangeInstruction r = (RegisterRangeInstruction)i;
                text.append(" range=").append(r.getStartRegister()).append(',').append(r.getRegisterCount());
            }
            if (i instanceof WideLiteralInstruction) text.append(" literal=").append(((WideLiteralInstruction)i).getWideLiteral());
            if (i instanceof OffsetInstruction) text.append(" target=").append(index(indices, pc + ((OffsetInstruction)i).getCodeOffset()));
            if (i instanceof ReferenceInstruction) {
                String value = ((ReferenceInstruction)i).getReference().toString();
                text.append(" ref=").append(value.length()).append(':').append(value);
            }
            if (i instanceof DualReferenceInstruction) {
                String value = ((DualReferenceInstruction)i).getReference2().toString();
                text.append(" ref2=").append(value.length()).append(':').append(value);
            }
            require(!(i instanceof SwitchPayload)
                && !(i instanceof com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload),
                "unexpected payload in pinned native method");
            text.append('\n'); pc += i.getCodeUnits();
        }
        for (TryBlock<? extends ExceptionHandler> t : body.getTryBlocks()) {
            text.append("try=").append(index(indices, t.getStartCodeAddress())).append(':')
                .append(index(indices, t.getStartCodeAddress() + t.getCodeUnitCount()));
            for (ExceptionHandler h : t.getExceptionHandlers()) text.append(" catch=").append(h.getExceptionType())
                .append(':').append(index(indices, h.getHandlerCodeAddress()));
            text.append('\n');
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : hash) out.append(String.format(Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static int index(Map<Integer,Integer> indices, int pc) {
        Integer value = indices.get(pc);
        require(value != null, "invalid native branch or handler target");
        return value;
    }

    private static MutableMethod pinned(BytecodePatchContext c, String owner, String name,
            String result, String hash, String... params) {
        MutableMethod method = TikTokLinkDirectPatch.method(c, owner, name, result, params);
        require(hash.equals(fingerprint(method)), "native full-method fingerprint changed: " + method);
        return method;
    }

    private static int at(List<Instruction> body, int address) {
        int pc = 0;
        for (int i = 0; i < body.size(); i++) {
            if (pc == address) return i;
            pc += body.get(i).getCodeUnits();
        }
        throw new IllegalStateException("TikTok shared-link contract: missing instruction at " + address);
    }

    private static void nativeStatic(BytecodePatchContext c, String owner, String name,
            String result, String... params) {
        MutableMethod method = TikTokLinkDirectPatch.method(c, owner, name, result, params);
        require((method.getAccessFlags() & 9) == 9, "native helper is not public static: " + method);
    }

    /** No instructions or class member inventories are changed during this phase. */
    public static Plan prepare(BytecodePatchContext c) {
        MutableClass owner = c.mutableClassDefByOrNull(OWNER);
        require(owner != null, "native video-route owner missing");
        for (Method method : owner.getMethods())
            require(!method.getName().startsWith("hiro"), "native route owner already contains a Hiro helper");
        MutableMethod startup = pinned(c, STARTUP, "LJ", "V", STARTUP_SHA256, ACTIVITY, URI);
        MutableMethod intent = pinned(c, OWNER, "LIZLLL", INTENT, INTENT_SHA256,
            ACTIVITY, INTENT, URI, STRING, STRING, "Z");
        MutableMethod classifier = pinned(c, OWNER, "LJII", "Z", CLASSIFIER_SHA256, URI, STRING, "Z");
        MutableMethod marker = pinned(c, "LX/1Sxg;", "LIZIZ", "Z", MARKER_SHA256, URI, STRING, STRING);
        require((marker.getAccessFlags() & 9) == 9, "shared-link marker helper changed access");
        nativeStatic(c, PROTECTOR, QUERY, STRING, URI, STRING);
        nativeStatic(c, PROTECTOR, "parse", URI, STRING);

        List<Instruction> startupCode = TikTokLinkDirectPatch.code(startup);
        int skip = at(startupCode, 0x0004), end = at(startupCode, 0x0168);
        Instruction branch = startupCode.get(skip);
        require(skip == 2 && branch.getOpcode() == Opcode.IF_NEZ
            && ((OneRegisterInstruction)branch).getRegisterA() == 0
            && ((OffsetInstruction)branch).getCodeOffset() == 356
            && startupCode.get(end).getOpcode() == Opcode.RETURN_VOID,
            "speculative Home startup branch changed");
        require(startup.getImplementation().getRegisterCount() == 13, "startup register layout changed");

        List<Instruction> intentCode = TikTokLinkDirectPatch.code(intent);
        List<Instruction> classifierCode = TikTokLinkDirectPatch.code(classifier);
        int intentAt = at(intentCode, 0x001d), classifierAt = at(classifierCode, 0x001e);
        require(intentAt == 14 && classifierAt == 15, "native selector guard layout changed");
        require(intent.getImplementation().getRegisterCount() == 8
            && classifier.getImplementation().getRegisterCount() == 7,
            "native selector argument registers changed");
        require(intentCode.get(intentAt).getOpcode() == Opcode.CONST_STRING
            && classifierCode.get(classifierAt).getOpcode() == Opcode.CONST_STRING
            && "insert_feed".equals(TikTokLinkDirectPatch.ref(intentCode.get(intentAt)))
            && "insert_feed".equals(TikTokLinkDirectPatch.ref(classifierCode.get(classifierAt))),
            "native feed-insertion decision moved");
        require(intent.getImplementation().getTryBlocks().isEmpty()
            && classifier.getImplementation().getTryBlocks().isEmpty(), "unexpected native selector handlers");
        // Construct and resolve helper branches now; errors cannot leave the
        // popup eligibility changed while route preparation is incomplete.
        MutableMethod exclusions = exclusions();
        MutableMethod scope = scope();
        fingerprint(exclusions);
        fingerprint(scope);
        return new Plan(owner, startup, intent, classifier, skip, end, intentAt, classifierAt, exclusions, scope);
    }

    public static final class Plan {
        private final MutableClass owner;
        private final MutableMethod startup, intent, classifier, exclusions, scope;
        private final int skip, end, intentAt, classifierAt;
        private Plan(MutableClass owner, MutableMethod startup, MutableMethod intent, MutableMethod classifier,
                int skip, int end, int intentAt, int classifierAt, MutableMethod exclusions, MutableMethod scope) {
            this.owner = owner; this.startup = startup; this.intent = intent; this.classifier = classifier;
            this.skip = skip; this.end = end; this.intentAt = intentAt; this.classifierAt = classifierAt;
            this.exclusions = exclusions; this.scope = scope;
        }
        /** Called only after the existing popup preflight and prepare() both succeed. */
        void apply() {
            owner.getMethods().add(exclusions);
            owner.getMethods().add(scope);
            insertSelector(intent, intentAt, Opcode.RETURN_OBJECT);
            insertSelector(classifier, classifierAt, Opcode.RETURN);
            MutableMethodImplementation code = startup.getImplementation();
            code.replaceInstruction(skip, new BuilderInstruction20t(Opcode.GOTO_16, code.newLabelForIndex(end)));
            System.out.println("HIRO_LINK_DIRECT native shared-video detail selected after existing guards; speculative Home startup skipped; no URL rewrite or runtime extension");
        }
    }

    private static void insertSelector(MutableMethod method, int at, Opcode result) {
        MutableMethodImplementation code = method.getImplementation();
        // The label remains attached to the original CONST_STRING insert_feed as
        // new instructions are inserted before it. Existing branch targets survive.
        Label original = code.newLabelForIndex(at);
        code.addInstruction(at, call(Opcode.INVOKE_STATIC, reference(OWNER, HELPER, "Z", URI), 4));
        code.addInstruction(at + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        code.addInstruction(at + 2, new BuilderInstruction21t(Opcode.IF_EQZ, 0, original));
        code.addInstruction(at + 3, new BuilderInstruction11x(result, 3));
    }

    private static ImmutableMethodReference reference(String owner, String name, String result, String... params) {
        return new ImmutableMethodReference(owner, name, Arrays.asList(params), result);
    }
    private static BuilderInstruction35c call(Opcode opcode, ImmutableMethodReference reference, int... registers) {
        require(registers.length <= 5, "helper invocation has too many registers");
        int[] r = new int[5];
        for (int i = 0; i < registers.length; i++) {
            require(registers[i] >= 0 && registers[i] < 16, "helper invocation register overflow");
            r[i] = registers[i];
        }
        return new BuilderInstruction35c(opcode, registers.length, r[0], r[1], r[2], r[3], r[4], reference);
    }
    private static void string(MethodImplementationBuilder b, int register, String value) {
        b.addInstruction(new BuilderInstruction21c(Opcode.CONST_STRING, register, new ImmutableStringReference(value)));
    }
    private static void result(MethodImplementationBuilder b, int register, boolean object) {
        b.addInstruction(new BuilderInstruction11x(object ? Opcode.MOVE_RESULT_OBJECT : Opcode.MOVE_RESULT, register));
    }
    private static void branch(MethodImplementationBuilder b, Opcode op, int register, String label) {
        b.addInstruction(new BuilderInstruction21t(op, register, b.getLabel(label)));
    }
    private static MutableMethod helper(String name, int registers, MethodImplementationBuilder b) {
        require(b.getMethodImplementation().getRegisterCount() == registers, "synthetic register layout differs");
        return new MutableMethod(new ImmutableMethod(OWNER, name,
            Collections.singletonList(new ImmutableMethodParameter(URI, Collections.<Annotation>emptySet(), null)),
            "Z", SYNTHETIC_STATIC_PRIVATE, Collections.<Annotation>emptySet(), Collections.emptySet(), b.getMethodImplementation()));
    }

    /** True for any nonempty special-route query; called only inside scope's catch. */
    private static MutableMethod exclusions() {
        MethodImplementationBuilder b = new MethodImplementationBuilder(2); // v1 = Uri
        for (String key : EXCLUDED) {
            string(b, 0, key);
            b.addInstruction(call(Opcode.INVOKE_STATIC, reference(PROTECTOR, QUERY, STRING, URI, STRING), 1, 0));
            result(b, 0, true);
            b.addInstruction(call(Opcode.INVOKE_STATIC, reference(TEXT, "isEmpty", "Z", "Ljava/lang/CharSequence;"), 0));
            result(b, 0, false);
            branch(b, Opcode.IF_EQZ, 0, "excluded");
        }
        b.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
        b.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        b.addLabel("excluded");
        b.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 1));
        b.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        return helper(EXCLUSION, 2, b);
    }

    /** Resolve scope only; never changes a Uri, Intent, application state, or account. */
    private static MutableMethod scope() {
        MethodImplementationBuilder b = new MethodImplementationBuilder(4); // v3 = original Uri
        Label start = b.addLabel("try_start");
        branch(b, Opcode.IF_EQZ, 3, "no");
        b.addInstruction(call(Opcode.INVOKE_VIRTUAL, reference(URI, "isHierarchical", "Z"), 3));
        result(b, 0, false);
        branch(b, Opcode.IF_EQZ, 0, "no");
        b.addInstruction(call(Opcode.INVOKE_VIRTUAL, reference(URI, "getHost", STRING), 3));
        result(b, 2, true);
        string(b, 1, "aweme");
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(TEXT, "equals", "Z", "Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;"), 2, 1));
        result(b, 0, false);
        branch(b, Opcode.IF_EQZ, 0, "legacy");
        b.addInstruction(call(Opcode.INVOKE_VIRTUAL, reference(URI, "getPath", STRING), 3));
        result(b, 0, true);
        branch(b, Opcode.IF_EQZ, 0, "no");
        string(b, 1, "/detail/[1-9][0-9]{0,19}/?");
        b.addInstruction(call(Opcode.INVOKE_VIRTUAL, reference(STRING, "matches", "Z", STRING), 0, 1));
        result(b, 0, false);
        branch(b, Opcode.IF_EQZ, 0, "no");
        b.addInstruction(new BuilderInstruction20t(Opcode.GOTO_16, b.getLabel("shape_ok")));
        // Native 1SyE/1SyG also accept legacy single-video query-ID routes.
        b.addLabel("legacy");
        for (String host : new String[]{"detail", "item", "musical"}) {
            string(b, 1, host);
            b.addInstruction(call(Opcode.INVOKE_STATIC, reference(TEXT, "equals", "Z", "Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;"), 2, 1));
            result(b, 0, false);
            branch(b, Opcode.IF_NEZ, 0, "legacy_path");
        }
        b.addInstruction(new BuilderInstruction20t(Opcode.GOTO_16, b.getLabel("no")));
        b.addLabel("legacy_path");
        b.addInstruction(call(Opcode.INVOKE_VIRTUAL, reference(URI, "getPath", STRING), 3));
        result(b, 0, true);
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(TEXT, "isEmpty", "Z", "Ljava/lang/CharSequence;"), 0));
        result(b, 1, false);
        branch(b, Opcode.IF_NEZ, 1, "legacy_id");
        string(b, 1, "/");
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(TEXT, "equals", "Z", "Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;"), 0, 1));
        result(b, 0, false);
        branch(b, Opcode.IF_EQZ, 0, "no");
        b.addLabel("legacy_id");
        string(b, 0, "id");
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(PROTECTOR, QUERY, STRING, URI, STRING), 3, 0));
        result(b, 0, true);
        branch(b, Opcode.IF_EQZ, 0, "no");
        string(b, 1, "[1-9][0-9]{0,19}");
        b.addInstruction(call(Opcode.INVOKE_VIRTUAL, reference(STRING, "matches", "Z", STRING), 0, 1));
        result(b, 0, false);
        branch(b, Opcode.IF_EQZ, 0, "no");
        b.addLabel("shape_ok");
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(OWNER, EXCLUSION, "Z", URI), 3));
        result(b, 0, false);
        branch(b, Opcode.IF_NEZ, 0, "no");
        string(b, 0, "params_url");
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(PROTECTOR, QUERY, STRING, URI, STRING), 3, 0));
        result(b, 2, true);
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(TEXT, "isEmpty", "Z", "Ljava/lang/CharSequence;"), 2));
        result(b, 0, false);
        branch(b, Opcode.IF_NEZ, 0, "marker");
        // Android Uri.parse is lenient. Validate syntax first, then conservatively
        // reject relative, opaque and authority-less nested URLs before native use.
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference("Ljava/net/URI;", "create", "Ljava/net/URI;", STRING), 2));
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(PROTECTOR, "parse", URI, STRING), 2));
        result(b, 2, true);
        branch(b, Opcode.IF_EQZ, 2, "no");
        b.addInstruction(call(Opcode.INVOKE_VIRTUAL, reference(URI, "isHierarchical", "Z"), 2));
        result(b, 0, false);
        branch(b, Opcode.IF_EQZ, 0, "no");
        b.addInstruction(call(Opcode.INVOKE_VIRTUAL, reference(URI, "getScheme", STRING), 2));
        result(b, 0, true);
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(TEXT, "isEmpty", "Z", "Ljava/lang/CharSequence;"), 0));
        result(b, 0, false);
        branch(b, Opcode.IF_NEZ, 0, "no");
        b.addInstruction(call(Opcode.INVOKE_VIRTUAL, reference(URI, "getHost", STRING), 2));
        result(b, 0, true);
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(TEXT, "isEmpty", "Z", "Ljava/lang/CharSequence;"), 0));
        result(b, 0, false);
        branch(b, Opcode.IF_NEZ, 0, "no");
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference(OWNER, EXCLUSION, "Z", URI), 2));
        result(b, 0, false);
        branch(b, Opcode.IF_NEZ, 0, "no");
        b.addLabel("marker");
        string(b, 0, "utm_campaign");
        string(b, 1, "client_share");
        b.addInstruction(call(Opcode.INVOKE_STATIC, reference("LX/1Sxg;", "LIZIZ", "Z", URI, STRING, STRING), 3, 0, 1));
        result(b, 0, false);
        b.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        Label end = b.addLabel("try_end");
        Label caught = b.addLabel("caught");
        b.addInstruction(new BuilderInstruction11x(Opcode.MOVE_EXCEPTION, 0));
        b.addLabel("no");
        b.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
        b.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        b.addCatch("Ljava/lang/RuntimeException;", start, end, caught);
        return helper(HELPER, 4, b);
    }
}
