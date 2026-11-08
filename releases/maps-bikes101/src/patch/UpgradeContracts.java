package app.hiro.mapsbikes.patches;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;

/** Only the exact published v1.0.0 runtime and its unchanged SharedLabeler hook can upgrade. */
public final class UpgradeContracts {
    private UpgradeContracts() {}
    public static final String REFERENCE = "/contracts/maps_hide_bicycle_marks_v1.0.0.dex";
    public static final String PREFIX = "Lapp/hiro/mapsbikes/";
    public static final String LEGACY_DEX_SHA256 = "73d1bab89799158d9d2908a5373be2bf25630c8f2e2497b50fcdf26fe4aa0a9c";
    private static final String PATCH_PREFIX = PREFIX + "patches/";
    private static final int MAX_RESOURCE_BYTES = 4 * 1024 * 1024;

    private static void require(boolean value, String reason) {
        MapsHideBicycleMarksPatch.require(value, reason);
    }

    public static Map<String, ClassDef> resource(String name) {
        try (InputStream input = MapsHideBicycleMarksPatch.class.getResourceAsStream(name)) {
            require(input != null, "Missing verified bicycle runtime resource " + name);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) != -1;) {
                require(bytes.size() + read <= MAX_RESOURCE_BYTES, "Bicycle runtime resource is too large");
                bytes.write(buffer, 0, read);
            }
            byte[] raw = bytes.toByteArray();
            if (REFERENCE.equals(name)) {
                require(LEGACY_DEX_SHA256.equals(RuntimeContracts.sha256(raw)),
                    "Published bicycle v1.0.0 runtime contract changed");
            }
            DexFile dex = DexBackedDexFile.fromInputStream(Opcodes.getDefault(),
                new BufferedInputStream(new ByteArrayInputStream(raw)));
            Map<String, ClassDef> result = new TreeMap<String, ClassDef>();
            for (ClassDef type : dex.getClasses()) {
                require(type.getType().startsWith(PREFIX) && !type.getType().startsWith(PATCH_PREFIX),
                    "Unexpected bicycle runtime namespace " + type.getType());
                require(result.put(type.getType(), type) == null, "Duplicate bicycle runtime class");
            }
            require(result.containsKey(MapsHideBicycleMarksPatch.RUNTIME), "Missing main bicycle runtime class");
            return result;
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load verified bicycle marker runtime", error);
        }
    }

    public static boolean hasRuntime(Collection<? extends ClassDef> installed) {
        require(installed != null, "Missing installed bicycle runtime inventory");
        for (ClassDef type : installed) if (type.getType().startsWith(PREFIX)) return true;
        return false;
    }

    /** Pure preflight; does not mutate any class, including on partial or unknown installations. */
    public static boolean validateInstallation(MapsHideBicycleMarksPatch.Lookup lookup,
                                               Collection<? extends ClassDef> installed) {
        require(lookup != null, "Missing installed bicycle class lookup");
        if (!hasRuntime(installed)) {
            require(lookup.get(MapsHideBicycleMarksPatch.RUNTIME) == null,
                "Inconsistent installed bicycle runtime inventory");
            MapsHideBicycleMarksPatch.validateNative(lookup);
            return false;
        }
        validateLegacyRuntime(lookup, installed);
        validateInstalledNative(lookup);
        return true;
    }

    public static void validateLegacyRuntime(MapsHideBicycleMarksPatch.Lookup lookup,
                                             Collection<? extends ClassDef> installed) {
        Map<String, ClassDef> expected = resource(REFERENCE);
        Set<String> actual = new TreeSet<String>();
        for (ClassDef type : installed) {
            if (type.getType().startsWith(PREFIX))
                require(actual.add(type.getType()), "Duplicate installed bicycle runtime class");
        }
        require(actual.equals(expected.keySet()), "Unknown or partial bicycle marker runtime installation");
        for (ClassDef reference : expected.values()) {
            ClassDef current = lookup.get(reference.getType());
            require(current != null && RuntimeContracts.classFingerprint(current)
                    .equals(RuntimeContracts.classFingerprint(reference)),
                "Only the exact published bicycle marker v1.0.0 can be upgraded: " + reference.getType());
        }
    }

    /** Used only after the complete guard has accepted the APK. No runtime-version state is retained. */
    public static boolean hasSharedHook(ClassDef owner) {
        if (owner == null) return false;
        try {
            checkSharedPrefix(MapsHideBicycleMarksPatch.find(owner, MapsHideBicycleMarksPatch.TARGET));
            return true;
        } catch (IllegalStateException invalid) {
            return false;
        }
    }

    private static void checkSharedPrefix(Method method) {
        require(method != null && method.getImplementation() != null, "Missing installed bicycle SharedLabeler hook");
        require(method.getImplementation().getRegisterCount() == 35,
            "Installed bicycle SharedLabeler registers changed");
        java.util.ArrayList<Instruction> code = new java.util.ArrayList<Instruction>();
        for (Instruction instruction : method.getImplementation().getInstructions()) code.add(instruction);
        require(code.size() == 440, "Installed bicycle SharedLabeler hook size changed");
        Instruction call = code.get(0), result = code.get(1), branch = code.get(2), zero = code.get(3), exit = code.get(4);
        require(call.getOpcode() == Opcode.INVOKE_STATIC_RANGE
                && ((RegisterRangeInstruction) call).getStartRegister() == 28
                && ((RegisterRangeInstruction) call).getRegisterCount() == 1
                && ((ReferenceInstruction) call).getReference() instanceof MethodReference
                && MapsHideBicycleMarksPatch.FILTER.equals(MapsHideBicycleMarksPatch.descriptor(
                    (MethodReference) ((ReferenceInstruction) call).getReference())),
            "Unknown installed bicycle SharedLabeler invocation");
        require(result.getOpcode() == Opcode.MOVE_RESULT && ((OneRegisterInstruction) result).getRegisterA() == 0,
            "Unknown installed bicycle SharedLabeler result register");
        require(branch.getOpcode() == Opcode.IF_EQZ && ((OneRegisterInstruction) branch).getRegisterA() == 0
                && ((OffsetInstruction) branch).getCodeOffset() == 4,
            "Unknown installed bicycle SharedLabeler continuation");
        require(zero.getOpcode() == Opcode.CONST_4 && ((OneRegisterInstruction) zero).getRegisterA() == 0
                && ((NarrowLiteralInstruction) zero).getNarrowLiteral() == 0,
            "Unknown installed bicycle SharedLabeler hidden result");
        require(exit.getOpcode() == Opcode.RETURN_OBJECT && ((OneRegisterInstruction) exit).getRegisterA() == 0,
            "Unknown installed bicycle SharedLabeler exit");
    }

    /** Return a detached original-body copy; callers' installed classes are never changed. */
    public static MutableClass originalSharedOwner(ClassDef installedOwner) {
        require(installedOwner != null && MapsHideBicycleMarksPatch.OWNER.equals(installedOwner.getType()),
            "Missing installed bicycle SharedLabeler owner");
        MutableClass original = new MutableClass(installedOwner);
        MutableMethod method = (MutableMethod) MapsHideBicycleMarksPatch.find(original, MapsHideBicycleMarksPatch.TARGET);
        checkSharedPrefix(method);
        for (int i = 0; i < 5; i++) method.getImplementation().removeInstruction(0);
        NativeContracts.validateTarget(method);
        return original;
    }

    public static void validateInstalledNative(final MapsHideBicycleMarksPatch.Lookup lookup) {
        final MutableClass original = originalSharedOwner(lookup.get(MapsHideBicycleMarksPatch.OWNER));
        MapsHideBicycleMarksPatch.validateNative(new MapsHideBicycleMarksPatch.Lookup() {
            public ClassDef get(String type) {
                return MapsHideBicycleMarksPatch.OWNER.equals(type) ? original : lookup.get(type);
            }
        });
    }

    public static void replaceRuntime(BytecodePatchContext context) {
        // Extension merging adds missing members but does not replace old bodies.
        // The explicit mutable replacement is required for STRIP_FAST upgrades.
        for (ClassDef type : resource(MapsHideBicycleMarksPatch.EXTENSION).values())
            context.getPatchClasses$morphe_patcher().addClass$morphe_patcher(new MutableClass(type));
    }
}
