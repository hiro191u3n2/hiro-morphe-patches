import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import app.hiro.mapsbikes.patches.MapsHideBicycleMarksPatch;
import app.hiro.mapsbikes.patches.RendererContracts;
import app.hiro.mapsbikes.patches.UpgradeContracts;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Real APK verification. Structural success does not assert Android UI behavior. */
public final class MapsBikesAudit {
    private static final String RUNTIME_PREFIX = "Lapp/hiro/mapsbikes/";
    private static final String PATCH_PREFIX = RUNTIME_PREFIX + "patches/";
    private static long checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<String, ClassDef>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String name : dex.getDexEntryNames()) {
            for (ClassDef type : dex.getEntry(name).getDexFile().getClasses()) {
                check(result.put(type.getType(), type) == null, "Duplicate class " + type.getType());
            }
        }
        return result;
    }

    private static MapsHideBicycleMarksPatch.Lookup lookup(final Map<String, ClassDef> all) {
        return new MapsHideBicycleMarksPatch.Lookup() {
            public ClassDef get(String type) { return all.get(type); }
        };
    }

    private static byte[] canonical(ClassDef type) throws Exception {
        check(type != null, "Missing class");
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(type)));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally {
            store.close();
        }
    }

    private static Method target(ClassDef owner) {
        return MapsHideBicycleMarksPatch.find(owner, MapsHideBicycleMarksPatch.TARGET);
    }

    private static List<Instruction> instructions(Method method) {
        List<Instruction> result = new ArrayList<Instruction>();
        check(method.getImplementation() != null, "Missing native implementation");
        for (Instruction instruction : method.getImplementation().getInstructions()) result.add(instruction);
        return result;
    }

    private static void rejected(Runnable action, String message) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected, message);
    }

    private static void auditHook(ClassDef before, ClassDef after) throws Exception {
        Method original = target(before), modified = target(after);
        List<Instruction> code = instructions(modified);
        check(code.size() == instructions(original).size() + 5, "Unexpected number of hook instructions");
        check(original.getImplementation().getRegisterCount() == 35
                && modified.getImplementation().getRegisterCount() == 35, "Factory registers changed");
        Instruction call = code.get(0), result = code.get(1), branch = code.get(2), zero = code.get(3), exit = code.get(4);
        check(call.getOpcode() == Opcode.INVOKE_STATIC_RANGE
                && ((RegisterRangeInstruction) call).getRegisterCount() == 1
                && ((RegisterRangeInstruction) call).getStartRegister() == 28
                && MapsHideBicycleMarksPatch.descriptor((MethodReference) ((ReferenceInstruction) call).getReference())
                    .equals(MapsHideBicycleMarksPatch.FILTER), "Hook does not inspect exactly the incoming native mark");
        check(result.getOpcode() == Opcode.MOVE_RESULT && ((OneRegisterInstruction) result).getRegisterA() == 0,
                "Hide result is not stored in the native scratch register");
        check(branch.getOpcode() == Opcode.IF_EQZ && ((OneRegisterInstruction) branch).getRegisterA() == 0
                && ((OffsetInstruction) branch).getCodeOffset() == 4, "Non-bicycle branch does not resume the original entry");
        check(zero.getOpcode() == Opcode.CONST_4 && ((OneRegisterInstruction) zero).getRegisterA() == 0
                && ((NarrowLiteralInstruction) zero).getNarrowLiteral() == 0, "Hidden mark does not produce null");
        check(exit.getOpcode() == Opcode.RETURN_OBJECT && ((OneRegisterInstruction) exit).getRegisterA() == 0,
                "Hidden mark does not return null before construction");
        MutableClass expected = new MutableClass(before);
        MapsHideBicycleMarksPatch.apply(expected);
        check(Arrays.equals(canonical(expected), canonical(after)),
                "Native owner differs beyond the five verified instructions, or original body/try blocks changed");
    }

    private static boolean calls(Instruction instruction, String descriptor) {
        return instruction instanceof ReferenceInstruction
            && ((ReferenceInstruction) instruction).getReference() instanceof MethodReference
            && descriptor.equals(MapsHideBicycleMarksPatch.descriptor(
                (MethodReference) ((ReferenceInstruction) instruction).getReference()));
    }

    private static void auditLegacyHook(ClassDef before, ClassDef after) throws Exception {
        Method original = MapsHideBicycleMarksPatch.find(before, RendererContracts.LEGACY);
        Method changed = MapsHideBicycleMarksPatch.find(after, RendererContracts.LEGACY);
        List<Instruction> code = instructions(changed);
        check(code.size() == instructions(original).size() + 5
                && original.getImplementation().getRegisterCount() == 18
                && changed.getImplementation().getRegisterCount() == 18, "Legacy factory ABI changed");
        Instruction call = code.get(0), result = code.get(1), branch = code.get(2), zero = code.get(3), exit = code.get(4);
        check(call.getOpcode() == Opcode.INVOKE_STATIC && call instanceof FiveRegisterInstruction
                && calls(call, MapsHideBicycleMarksPatch.RUNTIME + "->shouldHideLegacy(Ljava/lang/Object;Ljava/lang/Object;)Z"),
            "Legacy entry does not call the resolved-style filter");
        FiveRegisterInstruction arguments = (FiveRegisterInstruction) call;
        check(arguments.getRegisterCount() == 2 && arguments.getRegisterC() == 14 && arguments.getRegisterD() == 12,
            "Legacy filter must receive actual p3 style followed by p1 feature overrides");
        check(result.getOpcode() == Opcode.MOVE_RESULT && ((OneRegisterInstruction) result).getRegisterA() == 0,
            "Legacy result register changed");
        check(branch.getOpcode() == Opcode.IF_EQZ && ((OneRegisterInstruction) branch).getRegisterA() == 0
                && ((OffsetInstruction) branch).getCodeOffset() == 4, "Legacy false path does not resume original entry");
        check(zero.getOpcode() == Opcode.CONST_4 && ((OneRegisterInstruction) zero).getRegisterA() == 0
                && ((NarrowLiteralInstruction) zero).getNarrowLiteral() == 0
                && exit.getOpcode() == Opcode.RETURN_OBJECT && ((OneRegisterInstruction) exit).getRegisterA() == 0,
            "Legacy true path does not return null before icon construction");
        MutableClass restored = new MutableClass(after);
        MutableMethod method = (MutableMethod) MapsHideBicycleMarksPatch.find(restored, RendererContracts.LEGACY);
        for (int i = 0; i < 5; i++) method.getImplementation().removeInstruction(0);
        // Morphe's mutable proxy normalizes class metadata on construction;
        // compare both sides through that same representation while separately
        // requiring the restored method's original full body fingerprint.
        check(MapsHideBicycleMarksPatch.fingerprint(original).equals(MapsHideBicycleMarksPatch.fingerprint(method))
                && Arrays.equals(canonical(new MutableClass(before)), canonical(restored)),
            "Legacy factory contains changes beyond the independently verified five-instruction entry");
    }

    private static void auditBufferHook(ClassDef before, ClassDef after, String descriptor,
            String callback, int position, int[] ids) throws Exception {
        Method original = MapsHideBicycleMarksPatch.find(before, descriptor);
        Method changed = MapsHideBicycleMarksPatch.find(after, descriptor);
        List<Instruction> code = instructions(changed);
        check(original.getImplementation().getRegisterCount() == 6 && changed.getImplementation().getRegisterCount() == 6
                && code.size() == instructions(original).size() + 2, "Native resource callback ABI changed: " + descriptor);
        String filter = "Lapp/hiro/mapsbikes/BicycleLegendFilter;->filter([B)[B";
        int filters = 0, completions = 0;
        for (Instruction instruction : code) {
            if (calls(instruction, filter)) filters++;
            if (calls(instruction, callback)) completions++;
        }
        check(filters == 1 && completions == 1, "Resource must be filtered and completed exactly once: " + descriptor);
        Instruction call = code.get(position), result = code.get(position + 1), complete = code.get(position + 2);
        check(call.getOpcode() == Opcode.INVOKE_STATIC && call instanceof FiveRegisterInstruction && calls(call, filter),
            "Buffer filter is not immediately before resource completion: " + descriptor);
        FiveRegisterInstruction arguments = (FiveRegisterInstruction) call;
        check(arguments.getRegisterCount() == 1 && arguments.getRegisterC() == 5
                && result.getOpcode() == Opcode.MOVE_RESULT_OBJECT && ((OneRegisterInstruction) result).getRegisterA() == 5,
            "Buffer filter must replace only byte-array register v5: " + descriptor);
        check(complete.getOpcode() == Opcode.INVOKE_STATIC && complete instanceof FiveRegisterInstruction
                && calls(complete, callback), "Native completion target changed: " + descriptor);
        FiveRegisterInstruction completion = (FiveRegisterInstruction) complete;
        check(completion.getRegisterCount() == 5 && completion.getRegisterC() == ids[0]
                && completion.getRegisterD() == ids[1] && completion.getRegisterE() == ids[2]
                && completion.getRegisterF() == ids[3] && completion.getRegisterG() == 5
                && position + 3 == code.size() - 1 && code.get(position + 3).getOpcode() == Opcode.RETURN_VOID,
            "Factory/request identifiers or completion return changed: " + descriptor);
        if (descriptor.equals(RendererContracts.NETWORK)) {
            check(calls(code.get(4), "Lcmyn;->K()[B") && code.get(5).getOpcode() == Opcode.MOVE_RESULT_OBJECT
                    && ((OneRegisterInstruction) code.get(5)).getRegisterA() == 5 && position > 5,
                "Network filter must run after the response byte array has been extracted");
        }
        MutableClass restored = new MutableClass(after);
        MutableMethod method = (MutableMethod) MapsHideBicycleMarksPatch.find(restored, descriptor);
        method.getImplementation().removeInstruction(position);
        method.getImplementation().removeInstruction(position);
        check(MapsHideBicycleMarksPatch.fingerprint(original).equals(MapsHideBicycleMarksPatch.fingerprint(method))
                && Arrays.equals(canonical(new MutableClass(before)), canonical(restored)),
            "Resource owner contains changes beyond the independently verified buffer substitution: " + descriptor);
    }

    private static void auditCacheName(ClassDef before, ClassDef after) throws Exception {
        Method original = MapsHideBicycleMarksPatch.find(before, RendererContracts.CACHE_CONFIG);
        Method changed = MapsHideBicycleMarksPatch.find(after, RendererContracts.CACHE_CONFIG);
        List<Instruction> code = instructions(changed);
        check(code.size() == instructions(original).size() && original.getImplementation().getRegisterCount() == 15
                && changed.getImplementation().getRegisterCount() == 15, "Cache configuration ABI changed");
        int renamed = 0, old = 0, secure = 0, renamedAt = -1;
        for (int i = 0; i < code.size(); i++) {
            Instruction instruction = code.get(i);
            if (!(instruction instanceof ReferenceInstruction)
                    || !(((ReferenceInstruction) instruction).getReference() instanceof StringReference)) continue;
            String name = ((StringReference) ((ReferenceInstruction) instruction).getReference()).getString();
            if (name.equals("geoxp_cache_hiro_bikes101")) { renamed++; renamedAt = i; }
            if (name.equals("geoxp_cache")) old++;
            if (name.equals("geoxp_secure")) secure++;
        }
        check(renamed == 1 && old == 0 && secure == 1 && renamedAt == 132,
            "Cache migration must rename one online directory and preserve geoxp_secure");
        Instruction load = code.get(renamedAt);
        check(load.getOpcode() == Opcode.CONST_STRING && ((OneRegisterInstruction) load).getRegisterA() == 8,
            "Online cache directory destination changed");
        MutableClass restored = new MutableClass(after);
        MutableMethod method = (MutableMethod) MapsHideBicycleMarksPatch.find(restored, RendererContracts.CACHE_CONFIG);
        method.getImplementation().replaceInstruction(renamedAt,
            new BuilderInstruction21c(Opcode.CONST_STRING, 8, new ImmutableStringReference("geoxp_cache")));
        check(MapsHideBicycleMarksPatch.fingerprint(original).equals(MapsHideBicycleMarksPatch.fingerprint(method))
                && Arrays.equals(canonical(new MutableClass(before)), canonical(restored)),
            "Cache configuration changed beyond the single online-directory literal");
    }

    private static void auditRendererHooks(Map<String, ClassDef> before, Map<String, ClassDef> after) throws Exception {
        auditLegacyHook(before.get("Lbkex;"), after.get("Lbkex;"));
        auditBufferHook(before.get("Lbkup;"), after.get("Lbkup;"), RendererContracts.NETWORK,
            RendererContracts.NETWORK_CALLBACK, 15, new int[] {0, 1, 2, 3});
        auditBufferHook(before.get(RendererContracts.FACTORY), after.get(RendererContracts.FACTORY),
            RendererContracts.OFFLINE, RendererContracts.OFFLINE_CALLBACK, 6, new int[] {0, 1, 3, 4});
        String config = MapsHideBicycleMarksPatch.owner(RendererContracts.CACHE_CONFIG);
        auditCacheName(before.get(config), after.get(config));
    }

    private static void guards(String apk) throws Exception {
        Map<String, ClassDef> all = read(apk);
        check(!UpgradeContracts.validateInstallation(lookup(all), all.values()), "Fresh input reported as upgrade");
        for (String packageName : MapsHideBicycleMarksPatch.PACKAGES) {
            MapsHideBicycleMarksPatch.validatePackage(packageName, MapsHideBicycleMarksPatch.VERSION, MapsHideBicycleMarksPatch.CODE);
            rejected(() -> MapsHideBicycleMarksPatch.validatePackage(packageName, "different", MapsHideBicycleMarksPatch.CODE),
                    "Wrong version accepted");
            rejected(() -> MapsHideBicycleMarksPatch.validatePackage(packageName, MapsHideBicycleMarksPatch.VERSION, "different"),
                    "Wrong version code accepted");
        }
        rejected(() -> MapsHideBicycleMarksPatch.validatePackage("other", MapsHideBicycleMarksPatch.VERSION, MapsHideBicycleMarksPatch.CODE),
                "Wrong package accepted");
        ClassDef original = all.get(MapsHideBicycleMarksPatch.OWNER);
        MutableClass patched = new MutableClass(original);
        MapsHideBicycleMarksPatch.apply(patched);
        auditHook(original, patched);
        byte[] patchedSnapshot = canonical(patched);
        rejected(() -> MapsHideBicycleMarksPatch.apply(patched), "Duplicate application accepted");
        check(Arrays.equals(patchedSnapshot, canonical(patched)), "Rejected duplicate application partially modified code");
        MutableClass drift = new MutableClass(original);
        ((MutableMethod) target(drift)).getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
        byte[] snapshot = canonical(drift);
        rejected(() -> MapsHideBicycleMarksPatch.apply(drift), "Unknown native body accepted");
        check(Arrays.equals(snapshot, canonical(drift)), "Rejected unknown body partially modified code");
        Map<String, ClassDef> drifted = new TreeMap<String, ClassDef>(all);
        drifted.put(MapsHideBicycleMarksPatch.OWNER, drift);
        rejected(() -> MapsHideBicycleMarksPatch.validateNative(lookup(drifted)), "Unknown native contract accepted");
        Map<String, ClassDef> missing = new TreeMap<String, ClassDef>(all);
        missing.remove(MapsHideBicycleMarksPatch.OWNER);
        rejected(() -> MapsHideBicycleMarksPatch.validateNative(lookup(missing)), "Missing native owner accepted");
        Map<String, ClassDef> transformed = transformNative(all, false);
        auditRendererHooks(all, transformed);
        for (String owner : MapsHideBicycleMarksPatch.modifiedOwners()) {
            check(!Arrays.equals(canonical(all.get(owner)), canonical(transformed.get(owner))),
                "Declared native owner was not transformed " + owner);
            Map<String, ClassDef> missingNewOwner = new TreeMap<String, ClassDef>(all);
            missingNewOwner.remove(owner);
            rejected(() -> UpgradeContracts.validateInstallation(lookup(missingNewOwner), missingNewOwner.values()),
                "Missing declared native owner accepted " + owner);
        }
        Map<String, byte[]> snapshotAll = nativeSnapshot(transformed);
        rejected(() -> MapsHideBicycleMarksPatch.applyNative(lookup(transformed), false),
            "Duplicate multi-path application accepted");
        assertNativeSnapshot(transformed, snapshotAll, "Duplicate multi-path application mutated input");
        System.out.println("PASS native guards: verified factory/callers/icon decoder/proto schemas; package/version/code mismatch,"
                + " all declared native owners transformed; unknown native body, missing owners, and duplicate application"
                + " rejected without mutation; assertions=" + checks);
    }

    private static Map<String, ClassDef> transformNative(Map<String, ClassDef> input, boolean upgrade) {
        Set<String> owners = MapsHideBicycleMarksPatch.modifiedOwners();
        check(owners != null && owners.contains(MapsHideBicycleMarksPatch.OWNER), "Native owner inventory missing");
        Map<String, ClassDef> transformed = new TreeMap<String, ClassDef>(input);
        for (String owner : owners) {
            check(input.containsKey(owner), "Missing native transformation owner " + owner);
            transformed.put(owner, new MutableClass(input.get(owner)));
        }
        MapsHideBicycleMarksPatch.applyNative(lookup(transformed), upgrade);
        return transformed;
    }

    private static Map<String, byte[]> nativeSnapshot(Map<String, ClassDef> input) throws Exception {
        Map<String, byte[]> result = new TreeMap<String, byte[]>();
        for (String owner : MapsHideBicycleMarksPatch.modifiedOwners()) result.put(owner, canonical(input.get(owner)));
        return result;
    }

    private static void assertNativeSnapshot(Map<String, ClassDef> input, Map<String, byte[]> expected,
                                             String why) throws Exception {
        for (Map.Entry<String, byte[]> entry : expected.entrySet())
            check(Arrays.equals(entry.getValue(), canonical(input.get(entry.getKey()))), why + ": " + entry.getKey());
    }

    private static void upgradeGuards(String apk) throws Exception {
        Map<String, ClassDef> all = read(apk);
        Map<String, byte[]> originalNative = nativeSnapshot(all);
        check(UpgradeContracts.validateInstallation(lookup(all), all.values()), "Published old input was not accepted for upgrade");
        assertNativeSnapshot(all, originalNative, "Successful upgrade preflight mutated installed code");
        ClassDef owner = all.get(MapsHideBicycleMarksPatch.OWNER);
        check(UpgradeContracts.hasSharedHook(owner), "Published old shared hook was not recognized");
        MutableClass restored = UpgradeContracts.originalSharedOwner(owner);
        check(!UpgradeContracts.hasSharedHook(restored), "Detached original copy still has the old shared hook");
        auditHook(restored, owner);
        check(Arrays.equals(canonical(owner), originalNative.get(MapsHideBicycleMarksPatch.OWNER)),
            "Restoring the detached original copy mutated installed owner");

        Map<String, ClassDef> runtimeChanged = new TreeMap<String, ClassDef>(all);
        MutableClass badRuntime = new MutableClass(all.get(MapsHideBicycleMarksPatch.RUNTIME));
        MutableMethod filter = (MutableMethod) MapsHideBicycleMarksPatch.find(badRuntime, MapsHideBicycleMarksPatch.FILTER);
        filter.getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
        runtimeChanged.put(badRuntime.getType(), badRuntime);
        byte[] badRuntimeSnapshot = canonical(badRuntime);
        rejected(() -> UpgradeContracts.validateInstallation(lookup(runtimeChanged), runtimeChanged.values()),
            "Unknown old runtime body accepted");
        check(Arrays.equals(badRuntimeSnapshot, canonical(badRuntime)), "Rejected old runtime validation mutated code");

        for (String runtime : UpgradeContracts.resource(UpgradeContracts.REFERENCE).keySet()) {
            Map<String, ClassDef> partial = new TreeMap<String, ClassDef>(all);
            partial.remove(runtime);
            rejected(() -> UpgradeContracts.validateInstallation(lookup(partial), partial.values()),
                "Partial old runtime accepted: " + runtime);
        }
        Map<String, ClassDef> orphan = new TreeMap<String, ClassDef>(all);
        for (String runtime : UpgradeContracts.resource(UpgradeContracts.REFERENCE).keySet()) orphan.remove(runtime);
        rejected(() -> UpgradeContracts.validateInstallation(lookup(orphan), orphan.values()),
            "Old shared hook without its runtime accepted as fresh");

        Map<String, ClassDef> bodyChanged = new TreeMap<String, ClassDef>(all);
        MutableClass badOwner = new MutableClass(owner);
        ((MutableMethod) target(badOwner)).getImplementation().addInstruction(5, new BuilderInstruction10x(Opcode.NOP));
        bodyChanged.put(badOwner.getType(), badOwner);
        byte[] badOwnerSnapshot = canonical(badOwner);
        rejected(() -> UpgradeContracts.validateInstallation(lookup(bodyChanged), bodyChanged.values()),
            "Unknown native body beneath the old hook accepted");
        check(Arrays.equals(badOwnerSnapshot, canonical(badOwner)), "Rejected old native validation mutated code");

        Map<String, ClassDef> partialHook = new TreeMap<String, ClassDef>(all);
        MutableClass incomplete = new MutableClass(owner);
        ((MutableMethod) target(incomplete)).getImplementation().removeInstruction(0);
        partialHook.put(incomplete.getType(), incomplete);
        byte[] incompleteSnapshot = canonical(incomplete);
        rejected(() -> UpgradeContracts.validateInstallation(lookup(partialHook), partialHook.values()),
            "Partial old shared hook accepted");
        check(Arrays.equals(incompleteSnapshot, canonical(incomplete)), "Rejected partial hook validation mutated code");

        Map<String, ClassDef> transformed = transformNative(all, true);
        auditRendererHooks(all, transformed);
        check(Arrays.equals(canonical(owner), canonical(transformed.get(MapsHideBicycleMarksPatch.OWNER))),
            "Upgrading duplicated or replaced the already valid shared hook");
        for (String changed : MapsHideBicycleMarksPatch.modifiedOwners()) {
            if (!changed.equals(MapsHideBicycleMarksPatch.OWNER))
                check(!Arrays.equals(canonical(all.get(changed)), canonical(transformed.get(changed))),
                    "Upgrade failed to install a new native hook " + changed);
        }
        System.out.println("PASS exact old-runtime upgrade guards: published v1.0.0 accepted; canonical runtime and shared hook"
            + " verified; changed/partial runtime, changed/partial native hook and orphan hook rejected without mutation;"
            + " existing shared hook retained once; new native owners transformed; assertions=" + checks);
    }

    private static void rejectCurrent(String apk) throws Exception {
        Map<String, ClassDef> all = read(apk);
        check(UpgradeContracts.hasRuntime(all.values()), "Current patch runtime missing");
        Map<String, byte[]> before = nativeSnapshot(all);
        rejected(() -> UpgradeContracts.validateInstallation(lookup(all), all.values()),
            "Already current or unsupported bicycle runtime was accepted for reapplication");
        assertNativeSnapshot(all, before, "Current-version rejection mutated native code");
        System.out.println("PASS already-current bicycle patch reapplication rejected without mutation; assertions=" + checks);
    }

    private static boolean dexOrSignature(String name) {
        return name.matches("classes(?:[0-9]+)?\\.dex")
                || name.matches("(?i)META-INF/(?:MANIFEST\\.MF|[^/]+\\.(?:SF|RSA|DSA|EC))");
    }

    private static int readChunk(InputStream source, byte[] buffer) throws IOException {
        int total = 0, count;
        while (total < buffer.length && (count = source.read(buffer, total, buffer.length - total)) != -1) total += count;
        return total;
    }

    private static boolean sameBytes(ZipFile before, ZipEntry first, ZipFile after, ZipEntry second) throws Exception {
        if (first == null || second == null || first.getSize() != second.getSize()) return false;
        byte[] a = new byte[65536], b = new byte[65536];
        try (InputStream left = before.getInputStream(first); InputStream right = after.getInputStream(second)) {
            int count;
            while ((count = readChunk(left, a)) > 0) {
                if (readChunk(right, b) != count || !Arrays.equals(Arrays.copyOf(a, count), Arrays.copyOf(b, count))) return false;
            }
            return right.read() == -1;
        }
    }

    private static Set<String> bytePreservedClasses(String original, String patched) throws Exception {
        Set<String> result = new HashSet<String>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(original), Opcodes.getDefault());
        try (ZipFile before = new ZipFile(original); ZipFile after = new ZipFile(patched)) {
            for (String name : dex.getDexEntryNames()) {
                if (sameBytes(before, before.getEntry(name), after, after.getEntry(name))) {
                    for (ClassDef type : dex.getEntry(name).getDexFile().getClasses()) result.add(type.getType());
                }
            }
        }
        return result;
    }

    private static int auditResources(String original, String patched) throws Exception {
        int count = 0;
        try (ZipFile before = new ZipFile(original); ZipFile after = new ZipFile(patched)) {
            Set<String> oldNames = new TreeSet<String>(), newNames = new TreeSet<String>();
            Enumeration<? extends ZipEntry> oldEntries = before.entries(), newEntries = after.entries();
            while (oldEntries.hasMoreElements()) {
                ZipEntry entry = oldEntries.nextElement();
                if (entry.isDirectory() || dexOrSignature(entry.getName())) continue;
                check(oldNames.add(entry.getName()), "Duplicate original resource " + entry.getName());
                check(sameBytes(before, entry, after, after.getEntry(entry.getName())), "Resource changed " + entry.getName());
                count++;
            }
            while (newEntries.hasMoreElements()) {
                ZipEntry entry = newEntries.nextElement();
                if (!entry.isDirectory() && !dexOrSignature(entry.getName())) {
                    check(newNames.add(entry.getName()), "Duplicate patched resource " + entry.getName());
                }
            }
            check(oldNames.equals(newNames), "Non-DEX resource inventory changed");
        }
        return count;
    }

    private static void audit(String original, String patched, String extension) throws Exception {
        Map<String, ClassDef> before = read(original), after = read(patched), runtime = read(extension);
        check(runtime.containsKey(MapsHideBicycleMarksPatch.RUNTIME), "Production runtime filter missing");
        boolean upgrade = UpgradeContracts.validateInstallation(lookup(before), before.values());
        Map<String, ClassDef> transformed = transformNative(before, upgrade);
        auditRendererHooks(before, after);
        Set<String> expected = new TreeSet<String>(before.keySet());
        if (upgrade) {
            for (String type : new TreeSet<String>(expected)) if (type.startsWith(RUNTIME_PREFIX)) expected.remove(type);
        }
        for (ClassDef type : runtime.values()) {
            check(type.getType().startsWith(RUNTIME_PREFIX) && !type.getType().startsWith(PATCH_PREFIX), "Unexpected runtime namespace");
            check(expected.add(type.getType()), "Runtime collides with an installed class");
            check(Arrays.equals(canonical(type), canonical(after.get(type.getType()))), "Injected runtime differs " + type.getType());
        }
        check(expected.equals(after.keySet()), "APK class inventory differs");
        Set<String> unchanged = bytePreservedClasses(original, patched);
        for (ClassDef type : before.values()) {
            if (type.getType().startsWith(RUNTIME_PREFIX)) continue;
            if (MapsHideBicycleMarksPatch.modifiedOwners().contains(type.getType())) {
                check(Arrays.equals(canonical(transformed.get(type.getType())), canonical(after.get(type.getType()))),
                    "Native owner differs beyond verified transforms " + type.getType());
                if (type.getType().equals(MapsHideBicycleMarksPatch.OWNER))
                    auditHook(upgrade ? UpgradeContracts.originalSharedOwner(type) : type, after.get(type.getType()));
            }
            else if (!unchanged.contains(type.getType())) {
                check(Arrays.equals(canonical(type), canonical(after.get(type.getType()))), "Unrelated class changed " + type.getType());
            }
        }
        int resources = auditResources(original, patched);
        rejected(() -> UpgradeContracts.validateInstallation(lookup(after), after.values()), "Current output unexpectedly accepts reapplication");
        System.out.println("PASS APK scope: original classes=" + before.size() + "; upgrade=" + upgrade
                + "; modified native owners=" + MapsHideBicycleMarksPatch.modifiedOwners()
                + "; exact fresh/upgrade transformations and production runtime replacement verified;"
                + " all unrelated native/runtime classes preserved;"
                + " unchanged non-DEX resources=" + resources + "; bicycle runtime classes=" + runtime.size()
                + "; Android UI not executed; assertions=" + checks);
    }

    private static void compare(String left, String right) throws Exception {
        Map<String, ClassDef> first = read(left), second = read(right);
        check(first.keySet().equals(second.keySet()), "Compared APK class inventories differ");
        Set<String> unchanged = bytePreservedClasses(left, right);
        for (ClassDef type : first.values()) {
            if (!unchanged.contains(type.getType())) {
                check(Arrays.equals(canonical(type), canonical(second.get(type.getType()))),
                        "Compared APK class differs " + type.getType());
            }
        }
        int resources = auditResources(left, right);
        System.out.println("PASS Maps fresh/additive/upgrade equivalence: classes=" + first.size()
                + "; resources=" + resources + "; all class implementations/metadata and non-DEX resources identical; assertions=" + checks);
    }

    public static void main(String[] args) throws Exception {
        check(args.length > 0, "Missing audit mode");
        if (args[0].equals("guards") && args.length == 2) guards(args[1]);
        else if (args[0].equals("upgrade-guards") && args.length == 2) upgradeGuards(args[1]);
        else if (args[0].equals("reject-current") && args.length == 2) rejectCurrent(args[1]);
        else if (args[0].equals("audit") && args.length == 4) audit(args[1], args[2], args[3]);
        else if (args[0].equals("compare") && args.length == 3) compare(args[1], args[2]);
        else throw new IllegalArgumentException("Unknown audit mode or wrong argument count");
    }
}
