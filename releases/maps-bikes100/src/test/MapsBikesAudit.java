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
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
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

    private static void guards(String apk) throws Exception {
        Map<String, ClassDef> all = read(apk);
        MapsHideBicycleMarksPatch.validateNative(lookup(all));
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
        System.out.println("PASS native guards: verified factory/callers/icon decoder/proto schemas; package/version/code mismatch,"
                + " unknown native body, missing owner, and duplicate application rejected without mutation; assertions=" + checks);
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
        Set<String> expected = new TreeSet<String>(before.keySet());
        for (ClassDef type : runtime.values()) {
            check(type.getType().startsWith(RUNTIME_PREFIX) && !type.getType().startsWith(PATCH_PREFIX), "Unexpected runtime namespace");
            check(expected.add(type.getType()), "Runtime collides with an installed class");
            check(Arrays.equals(canonical(type), canonical(after.get(type.getType()))), "Injected runtime differs " + type.getType());
        }
        check(expected.equals(after.keySet()), "APK class inventory differs");
        Set<String> unchanged = bytePreservedClasses(original, patched);
        for (ClassDef type : before.values()) {
            if (type.getType().equals(MapsHideBicycleMarksPatch.OWNER)) auditHook(type, after.get(type.getType()));
            else if (!unchanged.contains(type.getType())) {
                check(Arrays.equals(canonical(type), canonical(after.get(type.getType()))), "Unrelated class changed " + type.getType());
            }
        }
        int resources = auditResources(original, patched);
        System.out.println("PASS APK scope: original classes=" + before.size() + "; modified native owner=Lbkbv; hook instructions=5;"
                + " native registers=35; original body/try blocks and all other native/runtime classes preserved;"
                + " unchanged non-DEX resources=" + resources + "; bicycle runtime classes=" + runtime.size()
                + "; Android UI not executed; assertions=" + checks);
    }

    private static void compare(String left, String right) throws Exception {
        Map<String, ClassDef> first = read(left), second = read(right);
        check(first.keySet().equals(second.keySet()), "Additive/all-five class inventories differ");
        Set<String> unchanged = bytePreservedClasses(left, right);
        for (ClassDef type : first.values()) {
            if (!unchanged.contains(type.getType())) {
                check(Arrays.equals(canonical(type), canonical(second.get(type.getType()))),
                        "Additive/all-five class differs " + type.getType());
            }
        }
        int resources = auditResources(left, right);
        System.out.println("PASS additive versus all-five Maps equivalence: classes=" + first.size()
                + "; resources=" + resources + "; all class implementations/metadata and non-DEX resources identical; assertions=" + checks);
    }

    public static void main(String[] args) throws Exception {
        check(args.length > 0, "Missing audit mode");
        if (args[0].equals("guards") && args.length == 2) guards(args[1]);
        else if (args[0].equals("audit") && args.length == 4) audit(args[1], args[2], args[3]);
        else if (args[0].equals("compare") && args.length == 3) compare(args[1], args[2]);
        else throw new IllegalArgumentException("Unknown audit mode or wrong argument count");
    }
}
