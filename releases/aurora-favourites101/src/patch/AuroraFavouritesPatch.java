package app.hiro.aurora.patches;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Add startup routing to the verified official/F-Droid 4.8.4 code; retain native auth. */
public final class AuroraFavouritesPatch {
    private AuroraFavouritesPatch() {}
    public static final String NAME = "毎回お気に入りアプリから起動";
    public static final String MAIN = "Lcom/aurora/store/ComposeActivity;";
    public static final String NAV = "Lcom/aurora/store/compose/navigation/c;";
    public static final String RUNTIME = "Lapp/hiro/aurora/runtime/AuroraFavourites;";
    public static final String SUPER = "Lyk3;";
    public static final String FAVOURITE = "Lcom/aurora/store/compose/navigation/Screen$k;";
    public static final String BIND_HASH = "426f7733244934f19581116cee0c547bb0d2126c808dc9099f92e8cef5a6a027";
    public static final String NAV_HASH = "f2f49d66977ca0dec3feedfad487106aa59f4517b5ae15e46749818b47024317";
    public static final String CREATE_HASH = "eeaba3c0033094ead62276a68e56266dae76821286324e061ae9dd22dc9063d5";
    private static final String ACTIVITY = "Landroid/app/Activity;";
    private static final String INTENT = "Landroid/content/Intent;";
    private static final String BUNDLE = "Landroid/os/Bundle;";
    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "Aurora Store 4.8.4 (76): 通常起動・開き直しでお気に入りアプリを表示。お気に入りで戻ると通常ホームへ移動。初回案内・ログイン・アプリロック・明示リンクを維持。Android実機未検証。",
        true,
        Collections.singleton(new Pair<String, Set<String>>("com.aurora.store", Collections.singleton("4.8.4"))),
        Collections.emptySet(), Collections.emptySet(),
        new Supplier<InputStream>() {
            @Override public InputStream get() {
                InputStream in = AuroraFavouritesPatch.class.getResourceAsStream("/extensions/aurora_favourites.mpe");
                if (in == null) throw new IllegalStateException("Missing Aurora favourites extension");
                return in;
            }
        },
        new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(BytecodePatchContext context) {
                if (!"com.aurora.store".equals(context.getPackageMetadata().getPackageName())
                        || !"4.8.4".equals(context.getPackageMetadata().getVersionName())
                        || !"76".equals(context.getPackageMetadata().getVersionCode()))
                    throw new IllegalStateException("Verified Aurora Store 4.8.4 (76) is required");
                ClassDef main = context.classDefBy(MAIN), nav = context.classDefBy(NAV);
                validate(main, nav);
                validateInherited(context.classDefBy("Lyk3;"), context.classDefBy("Lbb3;"),
                        context.classDefBy("Ly61;"), context.classDefBy("Ld71;"));
                relaxInherited(context.mutableClassDefBy("Lbb3;"), context.mutableClassDefBy("Ly61;"));
                apply(context.mutableClassDefBy(MAIN), context.mutableClassDefBy(NAV));
                return Unit.INSTANCE;
            }
        }, null);

    private static Method method(ClassDef owner, String name, String result, String... parameters) {
        if (owner == null) throw new IllegalStateException("Missing Aurora class");
        Method found = null;
        for (Method candidate : owner.getMethods()) {
            if (!name.equals(candidate.getName()) || !result.equals(candidate.getReturnType())
                    || !candidate.getParameterTypes().toString().equals(Arrays.asList(parameters).toString())) continue;
            if (found != null) throw new IllegalStateException("Ambiguous Aurora method");
            found = candidate;
        }
        if (found == null || found.getImplementation() == null) throw new IllegalStateException("Missing Aurora method: " + name);
        return found;
    }

    /** Stable semantic digest: registers, code/refs, branches and handlers; no debug/writer bytes. */
    public static String codeIdentity(Method method) {
        try {
            MethodImplementation code = method.getImplementation();
            StringBuilder text = new StringBuilder().append(method.getAccessFlags()).append('|')
                    .append(method.getName()).append('|').append(method.getParameterTypes()).append('|')
                    .append(method.getReturnType()).append('|').append(code.getRegisterCount()).append('\n');
            for (Instruction in : code.getInstructions()) {
                text.append(in.getOpcode().name()).append(':').append(in.getCodeUnits());
                if (in instanceof OneRegisterInstruction) text.append(" A").append(((OneRegisterInstruction) in).getRegisterA());
                if (in instanceof TwoRegisterInstruction) text.append(" B").append(((TwoRegisterInstruction) in).getRegisterB());
                if (in instanceof ThreeRegisterInstruction) text.append(" C").append(((ThreeRegisterInstruction) in).getRegisterC());
                if (in instanceof FiveRegisterInstruction) {
                    FiveRegisterInstruction f = (FiveRegisterInstruction) in;
                    text.append(" F").append(f.getRegisterCount()).append(',').append(f.getRegisterC()).append(',')
                            .append(f.getRegisterD()).append(',').append(f.getRegisterE()).append(',')
                            .append(f.getRegisterF()).append(',').append(f.getRegisterG());
                }
                if (in instanceof RegisterRangeInstruction) {
                    RegisterRangeInstruction r = (RegisterRangeInstruction) in;
                    text.append(" R").append(r.getStartRegister()).append(',').append(r.getRegisterCount());
                }
                if (in instanceof WideLiteralInstruction) text.append(" L").append(((WideLiteralInstruction) in).getWideLiteral());
                if (in instanceof OffsetInstruction) text.append(" O").append(((OffsetInstruction) in).getCodeOffset());
                if (in instanceof ReferenceInstruction) text.append(" V").append(((ReferenceInstruction) in).getReference());
                if (in instanceof DualReferenceInstruction) text.append(" W").append(((DualReferenceInstruction) in).getReference2());
                if (in instanceof SwitchPayload) for (SwitchElement e : ((SwitchPayload) in).getSwitchElements())
                    text.append(" S").append(e.getKey()).append(',').append(e.getOffset());
                if (in instanceof ArrayPayload) {
                    ArrayPayload p = (ArrayPayload) in;
                    text.append(" P").append(p.getElementWidth()).append(':').append(p.getArrayElements());
                }
                text.append('\n');
            }
            for (TryBlock<? extends ExceptionHandler> t : code.getTryBlocks()) {
                text.append("TRY ").append(t.getStartCodeAddress()).append(':').append(t.getCodeUnitCount());
                for (ExceptionHandler h : t.getExceptionHandlers())
                    text.append('|').append(h.getExceptionType()).append(':').append(h.getHandlerCodeAddress());
                text.append('\n');
            }
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
            return hex.toString();
        } catch (Exception error) { throw new IllegalStateException("Aurora semantic verification failed", error); }
    }

    public static void validate(ClassDef main, ClassDef nav) {
        if (main == null || nav == null || !MAIN.equals(main.getType()) || !SUPER.equals(main.getSuperclass())
                || !NAV.equals(nav.getType())) throw new IllegalStateException("Unverified Aurora activity/navigation");
        for (Method m : main.getMethods()) {
            if (Arrays.asList("onRestart", "onResume", "startActivityForResult", "startIntentSenderForResult").contains(m.getName()))
                throw new IllegalStateException("Aurora lifecycle already patched or unsupported");
        }
        if (!CREATE_HASH.equals(codeIdentity(method(main, "onCreate", "V", BUNDLE)))
                || !BIND_HASH.equals(codeIdentity(method(nav, "a", "V", "Lx95;", "Lgn1;", "I")))
                || !NAV_HASH.equals(codeIdentity(method(nav, "j", "V", "Ld85;", "Lcom/aurora/store/compose/navigation/b;"))))
            throw new IllegalStateException("Unverified Aurora 4.8.4 startup/navigation code");
    }

    private static boolean signature(Method method, String name, String... args) {
        return name.equals(method.getName()) && "V".equals(method.getReturnType())
                && method.getParameterTypes().toString().equals(Arrays.asList(args).toString());
    }

    private static int hookAccess(Method method) {
        if (signature(method, "onRestart")) return 4;
        if (signature(method, "onResume")) return 1;
        if (signature(method, "startActivityForResult", INTENT, "I", BUNDLE)) return 1;
        if (signature(method, "startIntentSenderForResult", "Landroid/content/IntentSender;", "I", INTENT,
                "I", "I", "I", BUNDLE)) return 1;
        return 0;
    }

    private static boolean reviewedFinal(Method method) {
        return "Lbb3;".equals(method.getDefiningClass()) && signature(method, "onResume")
                || "Ly61;".equals(method.getDefiningClass())
                    && (signature(method, "startActivityForResult", INTENT, "I", BUNDLE)
                        || signature(method, "startIntentSenderForResult", "Landroid/content/IntentSender;", "I", INTENT,
                            "I", "I", "I", BUNDLE));
    }

    /** Verify the entire in-APK superclass chain before enabling the three reviewed overrides. */
    public static void validateInherited(ClassDef hilt, ClassDef fragment, ClassDef component, ClassDef core) {
        ClassDef[] chain = {hilt, fragment, component, core};
        String[] owners = {"Lyk3;", "Lbb3;", "Ly61;", "Ld71;", ACTIVITY};
        int finals = 0;
        for (int index = 0; index < chain.length; index++) {
            ClassDef owner = chain[index];
            if (owner == null || !owners[index].equals(owner.getType())
                    || !owners[index + 1].equals(owner.getSuperclass()))
                throw new IllegalStateException("Unverified Aurora activity superclass chain");
            for (Method method : owner.getMethods()) {
                int visibility = hookAccess(method);
                if (visibility == 0) continue;
                int flags = method.getAccessFlags();
                if ((flags & 8) != 0 || (flags & 1) != 0 && visibility != 1)
                    throw new IllegalStateException("Aurora hook conflicts with inherited visibility/static declaration");
                if ((flags & 16) != 0) {
                    if (!reviewedFinal(method)) throw new IllegalStateException("Unsupported inherited final Aurora method");
                    finals++;
                }
            }
        }
        if (finals != 3
                || !"44e9caa2e03b3325d96bc629e0255f0e9e67eba9bafa3b708a4da34404956f39".equals(
                        codeIdentity(method(fragment, "onResume", "V")))
                || !"1c6a807ee7ab4e3f9c1e34bdbb7f69c53c36e756ca11b7f9a7b0a0053ca83898".equals(
                        codeIdentity(method(component, "startActivityForResult", "V", INTENT, "I", BUNDLE)))
                || !"be8ef36561f2d7af8f008f3f3af1b52162621f26c0620eba2a6945c49e1dd640".equals(
                        codeIdentity(method(component, "startIntentSenderForResult", "V", "Landroid/content/IntentSender;", "I", INTENT,
                                "I", "I", "I", BUNDLE))))
            throw new IllegalStateException("Unverified Aurora inherited lifecycle methods");
    }

    /** Only FINAL is removed; all original instructions, handlers, annotations and access remain. */
    public static void relaxInherited(MutableClass fragment, MutableClass component) {
        if (!"Lbb3;".equals(fragment.getType()) || !"Ly61;".equals(component.getType()))
            throw new IllegalStateException("Unexpected Aurora inherited owner");
        int changed = 0;
        for (MutableClass owner : Arrays.asList(fragment, component)) {
            for (MutableMethod method : owner.getMethods()) if (reviewedFinal(method)) {
                if (method.getAccessFlags() != 17) throw new IllegalStateException("Inherited Aurora final method changed");
                method.setAccessFlags(method.getAccessFlags() & ~16);
                changed++;
            }
        }
        if (changed != 3) throw new IllegalStateException("Missing inherited Aurora final methods");
    }

    private static ImmutableMethodReference runtime(String name, String result, String... args) {
        return new ImmutableMethodReference(RUNTIME, name, Arrays.asList(args), result);
    }

    private static MutableMethod added(String name, int access, List<String> args, int registers,
                                       String helper, boolean range) {
        MutableMethodImplementation code = new MutableMethodImplementation(registers);
        ImmutableMethodReference original = new ImmutableMethodReference(SUPER, name, args, "V");
        if (range) code.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_SUPER_RANGE, 0, registers, original));
        else code.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_SUPER, registers, 0, registers > 1 ? 1 : 0, registers > 2 ? 2 : 0, registers > 3 ? 3 : 0, 0, original));
        code.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0, runtime(helper, "V", ACTIVITY)));
        code.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        List<ImmutableMethodParameter> parameters = new ArrayList<ImmutableMethodParameter>();
        for (String arg : args) parameters.add(new ImmutableMethodParameter(arg, Collections.emptySet(), null));
        return new MutableMethod(new ImmutableMethod(MAIN, name, parameters, "V", access,
                Collections.emptySet(), Collections.emptySet(), code));
    }

    public static void apply(MutableClass main, MutableClass nav) {
        // validate immutable snapshots before any mutation; guard hooks against duplicate application.
        for (Method m : main.getMethods()) if ("onRestart".equals(m.getName()))
            throw new IllegalStateException("Aurora favourites already patched");
        MutableMethod bind = (MutableMethod) method(nav, "a", "V", "Lx95;", "Lgn1;", "I");
        MutableMethod navigate = (MutableMethod) method(nav, "j", "V", "Ld85;", "Lcom/aurora/store/compose/navigation/b;");
        bind.getImplementation().addInstruction(69, new BuilderInstruction21c(Opcode.SGET_OBJECT, 7,
                new ImmutableFieldReference(FAVOURITE, "INSTANCE", FAVOURITE)));
        bind.getImplementation().addInstruction(70, new BuilderInstruction21c(Opcode.NEW_INSTANCE, 8,
                new ImmutableTypeReference("Lcom/aurora/store/compose/navigation/Screen$p;")));
        bind.getImplementation().addInstruction(71, new BuilderInstruction35c(Opcode.INVOKE_DIRECT, 1, 8, 0, 0, 0, 0,
                new ImmutableMethodReference("Lcom/aurora/store/compose/navigation/Screen$p;", "<init>", Collections.<String>emptyList(), "V")));
        bind.getImplementation().addInstruction(72, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 4, 4, 2, 7, 8, 0,
                runtime("bind", "V", "Landroid/content/Context;", "Ljava/util/List;", "Ljava/lang/Object;", "Ljava/lang/Object;")));
        navigate.getImplementation().addInstruction(18, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 2, 0, 0, 0, 0,
                runtime("selectMain", "Ljava/lang/Object;", "Ljava/util/List;", "Ljava/lang/Object;")));
        navigate.getImplementation().addInstruction(19, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        navigate.getImplementation().addInstruction(20, new BuilderInstruction21c(Opcode.CHECK_CAST, 0, new ImmutableTypeReference("Lx95;")));
        main.getMethods().add(added("onRestart", 4, Collections.<String>emptyList(), 1, "restart", false));
        main.getMethods().add(added("onResume", 1, Collections.<String>emptyList(), 1, "resumed", false));
        main.getMethods().add(added("startActivityForResult", 1, Arrays.asList(INTENT, "I", BUNDLE), 4, "external", false));
        main.getMethods().add(added("startIntentSenderForResult", 1,
                Arrays.asList("Landroid/content/IntentSender;", "I", INTENT, "I", "I", "I", BUNDLE), 8, "external", true));
    }
}
