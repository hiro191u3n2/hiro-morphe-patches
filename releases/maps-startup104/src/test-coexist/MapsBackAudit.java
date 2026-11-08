import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.*;
import app.hiro.mapsback.patches.MapsBackExitPatch;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/**
 * Actual APK scope, guard, emitted-DEX and native dispatcher regression checks.
 * The small instruction interpreter exercises the emitted Maps branch, constructor
 * and finish call together with the APK's real registration and selection bodies.
 * Android framework/Looper, lazy initialization and reactive recomputation are
 * explicit host boundaries. This is not an Android UI or Recents execution test.
 */
public final class MapsBackAudit {
    static final String LOADER = "Lapp/hiro/mapsback/patches/";
    static final String CALLBACK = MapsBackExitPatch.CALLBACK;
    static final String ACTIVITY = "Landroid/app/Activity;";
    static final String INSTALL = CALLBACK + "->install(Ljava/lang/Object;Lamet;)V";
    static final String INIT = CALLBACK + "-><init>(Landroid/app/Activity;)V";
    static final String BACK = CALLBACK + "->b()V";
    static final String FINISH = "Landroid/app/Activity;->finishAndRemoveTask()V";
    static final String NATIVE_REGISTER = "Lamet;->at(Lqi;)V";
    static final String SELECT = "Likd;->a(I)Lijz;";
    static long checks;
    static int scenarios;

    interface Action { void run() throws Exception; }
    static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual), message + ": expected=" + expected + ", actual=" + actual);
    }
    static void rejected(Action action, String message) throws Exception {
        boolean failed = false;
        try { action.run(); } catch (IllegalStateException expected) { failed = true; }
        check(failed, message);
    }
    static Map<String,ClassDef> read(String path) throws Exception {
        Map<String,ClassDef> result = new TreeMap<String,ClassDef>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String entry : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(entry).getDexFile().getClasses())
            check(result.put(c.getType(), c) == null, "Duplicate DEX class " + c.getType());
        return result;
    }
    static MapsBackExitPatch.Lookup lookup(final Map<String,ClassDef> all) {
        return new MapsBackExitPatch.Lookup() { public ClassDef get(String type) { return all.get(type); } };
    }
    static String desc(MethodReference m) { return MapsBackExitPatch.descriptor(m); }
    static String field(FieldReference f) { return f.getDefiningClass() + "->" + f.getName() + ":" + f.getType(); }
    static Method method(ClassDef c, String descriptor) { return MapsBackExitPatch.find(c, descriptor); }
    static Method method(Map<String,ClassDef> all, String descriptor) {
        return method(all.get(MapsBackExitPatch.owner(descriptor)), descriptor);
    }
    static List<Instruction> instructions(Method m) {
        List<Instruction> result = new ArrayList<Instruction>();
        if (m.getImplementation() != null) for (Instruction i : m.getImplementation().getInstructions()) result.add(i);
        return result;
    }
    static List<String> calls(Method m) {
        List<String> result = new ArrayList<String>();
        for (Instruction i : instructions(m)) if (i instanceof ReferenceInstruction) {
            Reference ref = ((ReferenceInstruction)i).getReference();
            if (ref instanceof MethodReference) result.add(desc((MethodReference)ref));
        }
        return result;
    }
    static ClassDef normalize(ClassDef c) {
        List<Method> methods = new ArrayList<Method>();
        for (Method m : c.getMethods()) {
            MethodImplementation b = m.getImplementation();
            if (b != null) b = new ImmutableMethodImplementation(b.getRegisterCount(), b.getInstructions(),
                    b.getTryBlocks(), Collections.emptyList());
            methods.add(new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(), m.getReturnType(),
                    m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), b));
        }
        // Only instruction debug positions are excluded. All declarations,
        // annotations, fields, initial values, try blocks and execution code stay.
        return new ImmutableClassDef(c.getType(), c.getAccessFlags(), c.getSuperclass(), c.getInterfaces(),
                c.getSourceFile(), c.getAnnotations(), c.getFields(), methods);
    }
    static byte[] dexBytes(ClassDef c) throws Exception {
        MemoryDataStore data = new MemoryDataStore();
        try {
            DexPool.writeTo(data, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(c)));
            return Arrays.copyOf(data.getBuffer(), data.getSize());
        } finally { data.close(); }
    }
    static byte[] canonical(ClassDef c) throws Exception { return dexBytes(normalize(c)); }
    static ClassDef roundTrip(ClassDef c) throws Exception {
        DexBackedDexFile dex = new DexBackedDexFile(Opcodes.forApi(29), ByteBuffer.wrap(dexBytes(c)));
        check(dex.getClasses().size() == 1, "Round-trip class inventory changed");
        return dex.getClasses().iterator().next();
    }
    static ClassDef emittedCallback() {
        DexBackedDexFile dex = new DexBackedDexFile(Opcodes.forApi(28), ByteBuffer.wrap(MapsBackExitPatch.extensionDex()));
        check(dex.getClasses().size() == 1, "Extension must contain exactly one callback class");
        return dex.getClasses().iterator().next();
    }

    static void fingerprints(String apk) throws Exception {
        Map<String,ClassDef> all = read(apk);
        for (int i = 0; i < MapsBackExitPatch.CONTRACTS.length; i++) {
            Method m = method(all, MapsBackExitPatch.CONTRACTS[i]);
            System.out.println(i + " " + MapsBackExitPatch.fingerprint(m) + " "
                    + MapsBackExitPatch.fingerprint(new MutableMethod(m)) + " " + m.getAccessFlags() + " "
                    + m.getImplementation().getRegisterCount() + " " + desc(m));
        }
    }

    static void auditRawLayout(Method m) {
        List<Instruction> code = instructions(m);
        Map<Integer,Instruction> byAddress = new HashMap<Integer,Instruction>();
        int size = 0;
        for (Instruction i : code) {
            check(byAddress.put(size, i) == null, "Duplicate instruction address");
            if (i.getOpcode() == Opcode.PACKED_SWITCH_PAYLOAD || i.getOpcode() == Opcode.SPARSE_SWITCH_PAYLOAD
                    || i.getOpcode() == Opcode.ARRAY_PAYLOAD)
                check((size & 1) == 0, "Raw DEX payload is not four-byte aligned in " + desc(m));
            size += i.getCodeUnits();
        }
        int offset = 0;
        for (Instruction i : code) {
            if (i instanceof OffsetInstruction) {
                int target = offset + ((OffsetInstruction)i).getCodeOffset();
                Instruction destination = byAddress.get(target);
                check(destination != null, "Raw branch/payload reference misses instruction boundary in " + desc(m));
                if (i.getOpcode() == Opcode.PACKED_SWITCH || i.getOpcode() == Opcode.SPARSE_SWITCH) {
                    check(destination instanceof SwitchPayload, "Raw switch does not reference a switch payload");
                    for (SwitchElement e : ((SwitchPayload)destination).getSwitchElements()) {
                        Instruction branch = byAddress.get(offset + e.getOffset());
                        check(branch != null && !(branch instanceof SwitchPayload)
                                && branch.getOpcode() != Opcode.ARRAY_PAYLOAD, "Raw switch case misses code instruction");
                    }
                } else if (i.getOpcode() == Opcode.FILL_ARRAY_DATA) {
                    check(destination.getOpcode() == Opcode.ARRAY_PAYLOAD, "Raw array data reference has wrong payload");
                } else check(!(destination instanceof SwitchPayload) && destination.getOpcode() != Opcode.ARRAY_PAYLOAD,
                        "Raw control-flow branch enters a payload");
            }
            offset += i.getCodeUnits();
        }
        for (TryBlock<? extends ExceptionHandler> block : m.getImplementation().getTryBlocks()) {
            check(byAddress.containsKey(block.getStartCodeAddress()), "Raw try start misses instruction");
            int end = block.getStartCodeAddress() + block.getCodeUnitCount();
            check(end == size || byAddress.containsKey(end), "Raw try end misses boundary");
            for (ExceptionHandler handler : block.getExceptionHandlers())
                check(byAddress.containsKey(handler.getHandlerCodeAddress()), "Raw exception handler misses instruction");
        }
    }

    static void auditTarget(ClassDef before, ClassDef after) throws Exception {
        equal(MapsBackExitPatch.OWNER, before.getType(), "Target owner");
        // Inspect the actual serialized offsets before MutableClass can normalize
        // branch instructions or insert padding around a payload.
        auditRawLayout(method(after, MapsBackExitPatch.TARGET_METHOD));
        MutableClass restored = new MutableClass(after);
        Method original = method(before, MapsBackExitPatch.TARGET_METHOD);
        MutableMethod patched = (MutableMethod)method(restored, MapsBackExitPatch.TARGET_METHOD);
        equal(original.getAccessFlags(), patched.getAccessFlags(), "Factory access flags unchanged");
        equal(original.getImplementation().getRegisterCount(), patched.getImplementation().getRegisterCount(),
                "Factory register count unchanged");
        List<Instruction> a = instructions(original), b = instructions(patched);
        equal(a.size() + 2, b.size(), "Exactly one call and one alignment NOP added");
        int at = MapsBackExitPatch.injectionIndex(original);
        Instruction injected = b.get(at);
        check(injected.getOpcode() == Opcode.INVOKE_STATIC && injected instanceof FiveRegisterInstruction,
                "Injected opcode must be invoke-static35c");
        FiveRegisterInstruction r = (FiveRegisterInstruction)injected;
        check(r.getRegisterCount() == 2 && r.getRegisterC() == 12 && r.getRegisterD() == 0,
                "Factory must pass current Activity v12 and dispatcher v0");
        Reference ref = ((ReferenceInstruction)injected).getReference();
        check(ref instanceof MethodReference && INSTALL.equals(desc((MethodReference)ref)), "Unexpected injected call");
        check(b.get(at + 1).getOpcode() == Opcode.NOP, "Alignment NOP missing");
        equal(4, injected.getCodeUnits() + b.get(at + 1).getCodeUnits(), "Payload alignment must be preserved");
        check(b.get(at + 2).getOpcode() == Opcode.SGET, "Fallback must be inserted before SDK33 setup");
        Reference next = ((ReferenceInstruction)b.get(at + 2)).getReference();
        check(next instanceof FieldReference && "Landroid/os/Build$VERSION;->SDK_INT:I".equals(field((FieldReference)next)),
                "Fallback insertion moved past platform setup");
        patched.getImplementation().removeInstruction(at + 1);
        patched.getImplementation().removeInstruction(at);
        check(Arrays.equals(canonical(new MutableClass(before)), canonical(restored)),
                "Factory class differs beyond the permitted two instructions");
    }

    static void auditCallback(ClassDef c) throws Exception {
        equal(CALLBACK, c.getType(), "Generated callback name");
        equal("Lqi;", c.getSuperclass(), "Callback must extend the native back callback");
        equal(17, c.getAccessFlags(), "Callback must be public final");
        check(c.getInterfaces().isEmpty() && c.getAnnotations().isEmpty(), "Unexpected callback interfaces/annotations");
        List<Field> fields = new ArrayList<Field>();
        for (Field f : c.getFields()) fields.add(f);
        equal(1, fields.size(), "One Activity field only");
        Field activity = fields.get(0);
        equal(CALLBACK + "->activity:" + ACTIVITY, field(activity), "Stored Activity field");
        check(activity.getAccessFlags() == 18 && activity.getInitialValue() == null, "Activity must be private final instance field");
        Set<String> names = new TreeSet<String>();
        for (Method m : c.getMethods()) names.add(desc(m));
        equal(new TreeSet<String>(Arrays.asList(INIT, BACK, INSTALL)), names, "Callback method inventory");
        Method init = method(c, INIT), back = method(c, BACK), install = method(c, INSTALL);
        auditRawLayout(init); auditRawLayout(back); auditRawLayout(install);
        equal(65537, init.getAccessFlags(), "Public constructor access");
        equal(17, back.getAccessFlags(), "Public final back handler access");
        equal(9, install.getAccessFlags(), "Public static install access");
        equal(Collections.singletonList("Lqi;-><init>(Z)V"), calls(init), "Constructor must only call native super constructor");
        equal(Collections.singletonList(FINISH), calls(back), "Back must directly finish and remove the task");
        equal(Arrays.asList(INIT, NATIVE_REGISTER), calls(install), "Install must only construct and register callback");
        List<Instruction> body = instructions(back);
        equal(3, body.size(), "Direct back handler instruction count");
        check(body.get(0).getOpcode() == Opcode.IGET_OBJECT && body.get(1).getOpcode() == Opcode.INVOKE_VIRTUAL
                && body.get(2).getOpcode() == Opcode.RETURN_VOID, "Back handler contains unexpected control flow");
        List<String> typeChecks = new ArrayList<String>();
        for (Instruction i : instructions(install)) if (i.getOpcode() == Opcode.INSTANCE_OF)
            typeChecks.add(((TypeReference)((ReferenceInstruction)i).getReference()).getType());
        equal(Collections.singletonList(MapsBackExitPatch.MAIN), typeChecks, "Install applies exclusively to MapsActivity");
        check(Arrays.equals(canonical(emittedCallback()), canonical(c)), "Installed callback differs from emitted DEX");
    }

    static void guards(String apk) throws Exception {
        for (String pkg : MapsBackExitPatch.PACKAGES) {
            MapsBackExitPatch.validatePackage(pkg, MapsBackExitPatch.VERSION, MapsBackExitPatch.CODE);
            rejected(() -> MapsBackExitPatch.validatePackage(pkg, "unexpected", MapsBackExitPatch.CODE), "Wrong app version accepted");
            rejected(() -> MapsBackExitPatch.validatePackage(pkg, MapsBackExitPatch.VERSION, "unexpected"), "Wrong version code accepted");
        }
        rejected(() -> MapsBackExitPatch.validatePackage("com.example.other", MapsBackExitPatch.VERSION, MapsBackExitPatch.CODE),
                "Unrelated package accepted");
        Set<String> advertised = new TreeSet<String>();
        for (Object entry : MapsBackExitPatch.PATCH.getCompatiblePackages()) {
            kotlin.Pair<?,?> p = (kotlin.Pair<?,?>)entry;
            check(advertised.add((String)p.getFirst()), "Duplicate compatible package");
            equal(Collections.singleton(MapsBackExitPatch.VERSION), p.getSecond(), "Advertised app version");
        }
        equal(new TreeSet<String>(MapsBackExitPatch.PACKAGES), advertised, "Manager package selection");
        final Map<String,ClassDef> all = read(apk);
        MapsBackExitPatch.validateNative(lookup(all));
        for (int i = 0; i < MapsBackExitPatch.CONTRACTS.length; i++) {
            final int index = i;
            Method original = method(all, MapsBackExitPatch.CONTRACTS[i]);
            MapsBackExitPatch.validateContract(original, i);
            MutableMethod changed = new MutableMethod(original);
            MapsBackExitPatch.validateContract(changed, i);
            changed.getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
            rejected(() -> MapsBackExitPatch.validateContract(changed, index), "Changed native contract accepted " + i);
        }
        MutableClass applied = new MutableClass(all.get(MapsBackExitPatch.OWNER));
        MapsBackExitPatch.apply(applied);
        auditTarget(all.get(MapsBackExitPatch.OWNER), applied);
        byte[] once = canonical(applied);
        rejected(() -> MapsBackExitPatch.apply(applied), "Repeated patch application accepted");
        check(Arrays.equals(once, canonical(applied)), "Duplicate rejection partially mutated the factory");
        MutableClass drift = new MutableClass(all.get(MapsBackExitPatch.OWNER));
        ((MutableMethod)method(drift, MapsBackExitPatch.TARGET_METHOD)).getImplementation()
                .addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
        byte[] snapshot = canonical(drift);
        rejected(() -> MapsBackExitPatch.apply(drift), "Modified factory accepted");
        check(Arrays.equals(snapshot, canonical(drift)), "Factory guard failure partially mutated code");
        final Map<String,ClassDef> wrong = new TreeMap<String,ClassDef>(all);
        String last = MapsBackExitPatch.CONTRACTS[MapsBackExitPatch.CONTRACTS.length - 1];
        MutableClass dependency = new MutableClass(all.get(MapsBackExitPatch.owner(last)));
        ((MutableMethod)method(dependency, last)).getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
        wrong.put(dependency.getType(), dependency);
        rejected(() -> MapsBackExitPatch.validateNative(lookup(wrong)), "Last native dependency drift accepted");
        wrong.clear(); wrong.putAll(all); wrong.put(CALLBACK, emittedCallback());
        rejected(() -> MapsBackExitPatch.validateNative(lookup(wrong)), "Already installed callback accepted");
        ClassDef base = all.get("Lqi;");
        wrong.clear(); wrong.putAll(all);
        wrong.put("Lqi;", new ImmutableClassDef(base.getType(), base.getAccessFlags() | AccessFlags.FINAL.getValue(),
                base.getSuperclass(), base.getInterfaces(), base.getSourceFile(), base.getAnnotations(), base.getFields(), base.getMethods()));
        rejected(() -> MapsBackExitPatch.validateNative(lookup(wrong)), "Final native callback base accepted");
        auditCallback(emittedCallback());
        behavior(all, roundTrip(applied));
        System.out.println("PASS guards: package/version/code and " + MapsBackExitPatch.CONTRACTS.length
                + " native contracts verified; mismatch/duplicate rejection and factory atomicity verified; assertions=" + checks);
    }

    static boolean dexOrSignature(String name) {
        return name.matches("classes(?:[0-9]+)?\\.dex")
                || name.matches("(?i)META-INF/(?:MANIFEST\\.MF|[^/]+\\.(?:SF|RSA|DSA|EC))");
    }
    static boolean sameBytes(ZipFile a, ZipEntry x, ZipFile b, ZipEntry y) throws IOException {
        if (x == null || y == null || x.getSize() != y.getSize()) return false;
        try (InputStream aa = a.getInputStream(x); InputStream bb = b.getInputStream(y)) {
            byte[] xx = new byte[65536], yy = new byte[65536];
            while (true) {
                int ac = readChunk(aa, xx), bc = readChunk(bb, yy);
                if (ac != bc) return false;
                if (ac == 0) return true;
                for (int i = 0; i < ac; i++) if (xx[i] != yy[i]) return false;
            }
        }
    }
    static int readChunk(InputStream in, byte[] data) throws IOException {
        int n, total = 0;
        while (total < data.length && (n = in.read(data, total, data.length - total)) > 0) total += n;
        return total;
    }
    static Set<String> bytePreservedClasses(String original, String patched) throws Exception {
        Set<String> result = new HashSet<String>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(original), Opcodes.getDefault());
        try (ZipFile a = new ZipFile(original); ZipFile b = new ZipFile(patched)) {
            for (String name : dex.getDexEntryNames()) if (sameBytes(a, a.getEntry(name), b, b.getEntry(name)))
                for (ClassDef c : dex.getEntry(name).getDexFile().getClasses()) result.add(c.getType());
        }
        return result;
    }
    static Set<String> resourceNames(ZipFile z) {
        Set<String> names = new TreeSet<String>(), all = new HashSet<String>();
        Enumeration<? extends ZipEntry> entries = z.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            check(all.add(e.getName()), "Duplicate ZIP entry " + e.getName());
            if (!e.isDirectory() && !dexOrSignature(e.getName())) names.add(e.getName());
        }
        return names;
    }
    static int auditResources(String original, String patched) throws Exception {
        try (ZipFile a = new ZipFile(original); ZipFile b = new ZipFile(patched)) {
            Set<String> before = resourceNames(a), after = resourceNames(b);
            equal(before, after, "Non-DEX/non-signature resource inventory");
            for (String n : before) check(sameBytes(a, a.getEntry(n), b, b.getEntry(n)), "Resource changed " + n);
            return before.size();
        }
    }
    static void audit(String original, String patched) throws Exception {
        Map<String,ClassDef> before = read(original), after = read(patched);
        Set<String> expected = new TreeSet<String>(before.keySet());
        check(expected.add(CALLBACK), "Input already contains callback");
        equal(expected, after.keySet(), "APK class inventory");
        auditCallback(after.get(CALLBACK));
        Set<String> bytePreserved = bytePreservedClasses(original, patched);
        int untouched = 0, targets = 0;
        for (ClassDef c : before.values()) {
            ClassDef out = after.get(c.getType());
            if (MapsBackExitPatch.OWNER.equals(c.getType())) { auditTarget(c, out); targets++; }
            else {
                if (!bytePreserved.contains(c.getType()))
                    check(Arrays.equals(canonical(c), canonical(out)), "Unrelated class changed " + c.getType());
                untouched++;
            }
        }
        equal(1, targets, "Exactly one native owner changed");
        int resources = auditResources(original, patched);
        System.out.println("PASS APK scope: native classes preserved=" + untouched + "; byte-identical DEX classes="
                + bytePreserved.size() + "; native owner changed=Lps; factory instructions added=2; generated callback classes=1"
                + "; non-DEX/non-signature resources preserved=" + resources + "; assertions=" + checks
                + "; signature verification and Android UI execution not claimed");
    }
    static void merge(String basePath, String additionPath, String output) throws Exception {
        check(!new File(output).exists(), "Merge output must be new");
        Map<String,ClassDef> base = read(basePath), add = read(additionPath), merged = new TreeMap<String,ClassDef>(base);
        check(!add.isEmpty() && add.containsKey(LOADER + "MapsBackExitPatch;"), "Missing new patch provider");
        for (ClassDef c : add.values()) {
            check(c.getType().startsWith(LOADER), "New provider outside Maps-back namespace");
            check(merged.put(c.getType(), c) == null, "Loader class collision " + c.getType());
        }
        DexPool.writeTo(output, new ImmutableDexFile(Opcodes.getDefault(), merged.values()));
        Map<String,ClassDef> written = read(output);
        equal(merged.keySet(), written.keySet(), "Merged provider inventory");
        for (ClassDef c : base.values()) check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))),
                "Original loader class changed " + c.getType());
        for (ClassDef c : add.values()) check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))),
                "New loader class changed " + c.getType());
        System.out.println("PASS additive loader merge: original classes preserved=" + base.size()
                + "; new provider classes=" + add.size() + "; assertions=" + checks);
    }

    /** Plain host object graph. Android service calls remain observable boundaries. */
    static final class Node {
        final String type;
        final Map<String,Object> fields = new HashMap<String,Object>();
        Node(String type) { this.type = type; }
        Object get(String key) {
            Object value = fields.get(key);
            return value != null || fields.containsKey(key) ? value : key.endsWith(":Z") || key.endsWith(":I") ? 0 : null;
        }
        public String toString() { return type; }
    }
    static final class Vm {
        final Map<String,ClassDef> classes;
        final Set<String> executed = new TreeSet<String>();
        final IdentityHashMap<Node,Node> lazy = new IdentityHashMap<Node,Node>();
        final List<Node> finished = new ArrayList<Node>();
        final List<String> events = new ArrayList<String>();
        final Node mainLooper = new Node("Landroid/os/Looper;"), otherLooper = new Node("Landroid/os/Looper;");
        int sdk = 37, nestedCalls, registered, unregistered;
        boolean mainThread = true;
        Vm(Map<String,ClassDef> nativeClasses, ClassDef factory) {
            classes = new HashMap<String,ClassDef>(nativeClasses);
            classes.put(CALLBACK, emittedCallback());
            classes.put(MapsBackExitPatch.OWNER, factory);
        }
        Node dispatcherState(Node dispatcher) {
            Node ql = lazy.get(dispatcher);
            if (ql == null) {
                ql = new Node("Lql;");
                Node amee = new Node("Lamee;"), ikd = new Node("Likd;"), set = new Node("Lbuh;");
                set.fields.put("members", new HashSet<Object>());
                ikd.fields.put("Likd;->b:Lcugm;", new ArrayDeque<Object>());
                ikd.fields.put("Likd;->c:Lcugm;", new ArrayDeque<Object>());
                amee.fields.put("Lamee;->a:Z", 0);
                amee.fields.put("Lamee;->b:Z", 1);
                amee.fields.put("Lamee;->c:Ljava/lang/Object;", ikd);
                amee.fields.put("Lamee;->h:Ljava/lang/Object;", set);
                ql.fields.put("Lql;->a:Lamee;", amee);
                lazy.put(dispatcher, ql);
            }
            return (Node)ql.get("Lql;->a:Lamee;");
        }
        Node engine(Node dispatcher) { return (Node)dispatcherState(dispatcher).get("Lamee;->c:Ljava/lang/Object;"); }
        @SuppressWarnings("unchecked")
        Deque<Object> queue(Node dispatcher) { return (Deque<Object>)engine(dispatcher).get("Likd;->c:Lcugm;"); }
        Node factory(Node activity) {
            Node factory = new Node("Lps;");
            factory.fields.put("Lps;->b:I", 3);
            factory.fields.put("Lps;->a:Ljava/lang/Object;", activity);
            return (Node)execute(method(classes, MapsBackExitPatch.TARGET_METHOD), factory);
        }
        Node nested(Node dispatcher, boolean enabled) {
            Node callback = new Node("Ltest/ExistingNativeCallback;");
            execute(method(classes, "Lqi;-><init>(Z)V"), callback, enabled ? 1 : 0);
            execute(method(classes, NATIVE_REGISTER), dispatcher, callback);
            return (Node)queue(dispatcher).getFirst();
        }
        Node select(Node dispatcher, int direction) { return (Node)execute(method(classes, SELECT), engine(dispatcher), direction); }
        void press(Node dispatcher) {
            Node selected = select(dispatcher, -1);
            check(selected != null, "Expected an enabled handler for Back");
            execute(method(classes, "Lqh;->b()V"), selected);
        }
        boolean isInstance(Object value, String expected) {
            if (value == null || value instanceof Number && ((Number)value).intValue() == 0) return false;
            if ("Ljava/lang/Object;".equals(expected)) return true;
            if (value instanceof Deque) return "Lcugm;".equals(expected) || "Ljava/lang/Iterable;".equals(expected);
            if (value instanceof List) return "Ljava/util/List;".equals(expected) || "Ljava/util/Collection;".equals(expected);
            if (!(value instanceof Node)) throw new AssertionError("Unsupported host cast " + expected);
            String type = ((Node)value).type;
            if ("Ltest/ExistingNativeCallback;".equals(type)) type = "Lqi;";
            for (int i = 0; type != null && i < 40; i++) {
                if (type.equals(expected)) return true;
                ClassDef c = classes.get(type);
                type = c == null ? null : c.getSuperclass();
            }
            return false;
        }
        static boolean zero(Object value) { return value == null || value instanceof Number && ((Number)value).intValue() == 0; }
        static int number(Object value) {
            if (value == null) return 0;
            if (!(value instanceof Number)) throw new AssertionError("Expected DEX integer: " + value);
            return ((Number)value).intValue();
        }
        static boolean same(Object a, Object b) {
            return zero(a) && zero(b) || a instanceof Number && b instanceof Number && number(a) == number(b) || a == b;
        }
        static int[] argumentRegisters(Instruction i) {
            if (i instanceof RegisterRangeInstruction) {
                RegisterRangeInstruction r = (RegisterRangeInstruction)i;
                int[] out = new int[r.getRegisterCount()];
                for (int n = 0; n < out.length; n++) out[n] = r.getStartRegister() + n;
                return out;
            }
            FiveRegisterInstruction r = (FiveRegisterInstruction)i;
            return Arrays.copyOf(new int[] {r.getRegisterC(), r.getRegisterD(), r.getRegisterE(), r.getRegisterF(), r.getRegisterG()}, r.getRegisterCount());
        }
        @SuppressWarnings("unchecked")
        Object invoke(MethodReference ref, Object[] args) {
            String d = desc(ref);
            // Precisely named host boundaries; unrecognized calls fail closed.
            if (d.equals("Ljava/lang/Object;-><init>()V")
                    || d.equals("Ljava/util/ArrayList;-><init>()V")
                    || d.equals("Ljava/util/concurrent/CopyOnWriteArrayList;-><init>()V")) return null;
            if (d.equals("Ljava/lang/Object;->getClass()Ljava/lang/Class;")) { check(!zero(args[0]), "Native non-null receiver"); return Object.class; }
            if (d.equals("Ljava/util/Collection;->add(Ljava/lang/Object;)Z")) return ((Collection<Object>)args[0]).add(args[1]) ? 1 : 0;
            if (d.equals("Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;")) return ((Iterable<?>)args[0]).iterator();
            if (d.equals("Ljava/util/Iterator;->hasNext()Z")) return ((Iterator<?>)args[0]).hasNext() ? 1 : 0;
            if (d.equals("Ljava/util/Iterator;->next()Ljava/lang/Object;")) return ((Iterator<?>)args[0]).next();
            if (d.equals("Lcugm;->addFirst(Ljava/lang/Object;)V")) { ((Deque<Object>)args[0]).addFirst(args[1]); events.add("queue-add-first"); return null; }
            if (d.equals("Lbuh;->f(Ljava/lang/Object;)Z")) return ((Set<Object>)((Node)args[0]).fields.get("members")).add(args[1]) ? 1 : 0;
            if (d.equals("Lamet;->as()Lql;")) { dispatcherState((Node)args[0]); return lazy.get((Node)args[0]); }
            if (d.equals("Likd;->b()V")) { events.add("native-state-recompute-boundary"); return null; }
            if (d.equals("Lqj;-><init>(Lqi;Lgre;)V")) { ((Node)args[0]).fields.put("callback", args[1]); return null; }
            if (d.equals("Lav;-><init>(Ljava/lang/Object;I)V")) {
                equal(13, number(args[2]), "Original Activity fallback discriminator");
                ((Node)args[0]).fields.put("activity", args[1]); return null;
            }
            if (d.equals("Lcufk;-><init>(Lcujo;)V")) { ((Node)args[0]).fields.put("provider", args[1]); return null; }
            if (d.equals("Landroid/os/Looper;->myLooper()Landroid/os/Looper;")) return mainThread ? mainLooper : otherLooper;
            if (d.equals("Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;")) return mainLooper;
            if (d.equals("Lcukv;->g(Ljava/lang/Object;Ljava/lang/Object;)Z")) return same(args[0], args[1]) ? 1 : 0;
            if (d.equals("Lpw;->B(Lamet;)V")) { events.add("platform-dispatcher-setup-boundary"); return null; }
            if (d.equals("Landroid/os/Handler;-><init>(Landroid/os/Looper;)V")) return null;
            if (d.equals("Law;-><init>(Ljava/lang/Object;Ljava/lang/Object;I[C)V")) {
                equal(2, number(args[3]), "Original main-thread setup discriminator"); return null;
            }
            if (d.equals("Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z")) { events.add("platform-setup-post-boundary"); return 1; }
            if (d.equals(FINISH)) { check(args[0] instanceof Node && isInstance(args[0], ACTIVITY), "Finish receiver must be Activity"); finished.add((Node)args[0]); return null; }
            if (d.equals("Lqi;->b()V")) {
                Node receiver = (Node)args[0];
                if (receiver.type.equals(CALLBACK)) return execute(method(classes, BACK), args);
                check(receiver.type.equals("Ltest/ExistingNativeCallback;"), "Unknown native callback dispatch"); nestedCalls++; return null;
            }
            if (d.equals("Ld$$ExternalSyntheticApiModelOutline0;->m(Landroid/window/OnBackInvokedDispatcher;ILandroid/window/OnBackInvokedCallback;)V")) { registered++; return null; }
            if (d.equals("Ld$$ExternalSyntheticApiModelOutline0;->m(Landroid/window/OnBackInvokedDispatcher;Landroid/window/OnBackInvokedCallback;)V")) { unregistered++; return null; }
            Set<String> allowed = new HashSet<String>(Arrays.asList(INSTALL, INIT, BACK, NATIVE_REGISTER,
                    "Lps;-><init>(Ljava/lang/Object;I)V", "Lamet;-><init>(Ljava/lang/Runnable;)V",
                    "Lqi;-><init>(Z)V", "Lqi;->g(Lgdz;)Lqh;", "Lqh;-><init>(Lqi;Lgdz;)V", "Lqh;->b()V", "Lqh;->e(Z)V",
                    "Lijz;-><init>(Lgdz;Z)V", "Lijz;->h()Z", "Lijz;->g(Z)V", "Lamee;->f()V", "Lamee;->h(Lijz;)V",
                    SELECT, "Likh;->c(Z)V", "Likh;->e(Z)V"));
            check(allowed.contains(d), "Unexpected VM call " + d);
            return execute(method(classes, d), args);
        }
        Object execute(Method m, Object... args) {
            executed.add(desc(m));
            check(m.getImplementation() != null, "VM method needs body " + desc(m));
            equal(m.getParameterTypes().size() + ((m.getAccessFlags() & 8) == 0 ? 1 : 0), args.length, "VM call arity " + desc(m));
            Object[] regs = new Object[m.getImplementation().getRegisterCount()];
            System.arraycopy(args, 0, regs, regs.length - args.length, args.length);
            List<Instruction> code = instructions(m);
            Map<Integer,Integer> address = new HashMap<Integer,Integer>();
            int[] addresses = new int[code.size()];
            int offset = 0;
            for (int n = 0; n < code.size(); n++) { addresses[n] = offset; address.put(offset, n); offset += code.get(n).getCodeUnits(); }
            Object result = null;
            int pc = 0, steps = 0;
            while (pc < code.size()) {
                check(++steps < 3000, "VM instruction bound " + desc(m));
                Instruction i = code.get(pc); int next = pc + 1;
                int a = i instanceof OneRegisterInstruction ? ((OneRegisterInstruction)i).getRegisterA() : -1;
                int b = i instanceof TwoRegisterInstruction ? ((TwoRegisterInstruction)i).getRegisterB() : -1;
                Reference ref = i instanceof ReferenceInstruction ? ((ReferenceInstruction)i).getReference() : null;
                switch (i.getOpcode()) {
                    case NOP: break;
                    case CONST_4: case CONST_16: case CONST:
                        regs[a] = ((NarrowLiteralInstruction)i).getNarrowLiteral(); break;
                    case MOVE: case MOVE_FROM16: case MOVE_16:
                    case MOVE_OBJECT: case MOVE_OBJECT_FROM16: case MOVE_OBJECT_16:
                        regs[a] = regs[b]; break;
                    case NEW_INSTANCE:
                        String type = ((TypeReference)ref).getType();
                        regs[a] = type.equals("Ljava/util/ArrayList;") || type.equals("Ljava/util/concurrent/CopyOnWriteArrayList;")
                                ? new ArrayList<Object>() : new Node(type); break;
                    case IGET: case IGET_OBJECT: case IGET_BOOLEAN:
                        regs[a] = ((Node)regs[b]).get(field((FieldReference)ref)); break;
                    case IPUT: case IPUT_OBJECT: case IPUT_BOOLEAN:
                        ((Node)regs[b]).fields.put(field((FieldReference)ref), regs[a]); break;
                    case SGET:
                        equal("Landroid/os/Build$VERSION;->SDK_INT:I", field((FieldReference)ref), "VM static integer boundary"); regs[a] = sdk; break;
                    case SGET_OBJECT:
                        equal("Lcugq;->a:Lcugq;", field((FieldReference)ref), "VM static object boundary"); regs[a] = Collections.emptyList(); break;
                    case INSTANCE_OF: regs[a] = isInstance(regs[b], ((TypeReference)ref).getType()) ? 1 : 0; break;
                    case CHECK_CAST:
                        check(zero(regs[a]) || isInstance(regs[a], ((TypeReference)ref).getType()), "VM cast " + ref); break;
                    case IF_EQZ: case IF_NEZ:
                        if (zero(regs[a]) == (i.getOpcode() == Opcode.IF_EQZ)) next = branch(address, addresses[pc], (OffsetInstruction)i); break;
                    case IF_EQ: case IF_NE:
                        if (same(regs[a], regs[b]) == (i.getOpcode() == Opcode.IF_EQ)) next = branch(address, addresses[pc], (OffsetInstruction)i); break;
                    case IF_LT:
                        if (number(regs[a]) < number(regs[b])) next = branch(address, addresses[pc], (OffsetInstruction)i); break;
                    case GOTO: case GOTO_16: case GOTO_32:
                        next = branch(address, addresses[pc], (OffsetInstruction)i); break;
                    case PACKED_SWITCH:
                        Integer payload = address.get(addresses[pc] + ((OffsetInstruction)i).getCodeOffset());
                        check(payload != null && code.get(payload) instanceof SwitchPayload, "VM packed switch payload");
                        for (SwitchElement e : ((SwitchPayload)code.get(payload)).getSwitchElements()) if (e.getKey() == number(regs[a])) {
                            Integer target = address.get(addresses[pc] + e.getOffset());
                            check(target != null, "VM switch target boundary"); next = target; break;
                        }
                        break;
                    case INVOKE_STATIC: case INVOKE_STATIC_RANGE: case INVOKE_DIRECT: case INVOKE_DIRECT_RANGE:
                    case INVOKE_VIRTUAL: case INVOKE_VIRTUAL_RANGE: case INVOKE_INTERFACE: case INVOKE_INTERFACE_RANGE:
                        int[] ids = argumentRegisters(i); Object[] parameters = new Object[ids.length];
                        for (int n = 0; n < ids.length; n++) parameters[n] = regs[ids[n]];
                        result = invoke((MethodReference)ref, parameters); break;
                    case MOVE_RESULT: case MOVE_RESULT_OBJECT: regs[a] = result; break;
                    case RETURN_VOID: return null;
                    case RETURN: case RETURN_OBJECT: return regs[a];
                    default: throw new AssertionError("Unsupported VM instruction " + i.getOpcode() + " in " + desc(m));
                }
                pc = next;
            }
            throw new AssertionError("VM fell off method " + desc(m));
        }
        int branch(Map<Integer,Integer> address, int pc, OffsetInstruction branch) {
            Integer result = address.get(pc + branch.getCodeOffset());
            check(result != null, "VM branch must target instruction boundary"); return result;
        }
    }

    static void behavior(Map<String,ClassDef> original, ClassDef factory) throws Exception {
        final Set<String> exercised = new TreeSet<String>();
        for (int sdk : new int[] {28, 33, 37}) {
            scenarios++;
            Vm vm = new Vm(original, factory); vm.sdk = sdk;
            Node activity = new Node(MapsBackExitPatch.MAIN), dispatcher = vm.factory(activity);
            equal(1, vm.queue(dispatcher).size(), "One Maps fallback at SDK " + sdk);
            equal(0, vm.finished.size(), "Installation must never finish the Activity");
            Node handler = (Node)vm.queue(dispatcher).getFirst();
            Node callback = (Node)handler.get("Lqh;->f:Lqi;");
            check(callback.get(CALLBACK + "->activity:" + ACTIVITY) == activity, "Stored Activity identity");
            equal(1, callback.get("Lqi;->b:Z"), "Native callback starts enabled");
            check(vm.select(dispatcher, -1) == handler && vm.select(dispatcher, 0) == handler, "Root fallback selected in both native directions");
            if (sdk >= 33) check(vm.events.indexOf("queue-add-first") < vm.events.indexOf("platform-dispatcher-setup-boundary"),
                    "Maps fallback must be registered before SDK33 system setup");
            vm.press(dispatcher);
            equal(Collections.singletonList(activity), vm.finished, "Root Back issues exactly one finishAndRemoveTask");
            exercised.addAll(vm.executed);
        }
        for (int sdk : new int[] {28, 37}) {
            scenarios++;
            Vm vm = new Vm(original, factory); vm.sdk = sdk;
            Node other = new Node("Lpw;"), dispatcher = vm.factory(other);
            equal(0, vm.queue(dispatcher).size(), "Other Activity must not receive fallback");
            equal(0, vm.finished.size(), "Other Activity must not finish");
            check(!vm.executed.contains(INIT), "Other Activity must not construct callback");
            exercised.addAll(vm.executed);
        }
        scenarios++;
        Vm posted = new Vm(original, factory); posted.mainThread = false;
        Node postedActivity = new Node(MapsBackExitPatch.MAIN), postedDispatcher = posted.factory(postedActivity);
        equal(1, posted.queue(postedDispatcher).size(), "Off-main factory still installs fallback synchronously");
        check(posted.events.indexOf("queue-add-first") < posted.events.indexOf("platform-setup-post-boundary"),
                "Fallback is registered before deferred platform setup");
        equal(0, posted.finished.size(), "Deferred setup does not exit");
        exercised.addAll(posted.executed);
        scenarios++;
        Vm nullObject = new Vm(original, factory);
        Node emptyDispatcher = new Node("Lamet;");
        nullObject.execute(method(nullObject.classes, INSTALL), null, emptyDispatcher);
        equal(0, nullObject.queue(emptyDispatcher).size(), "Null install object is ignored");
        exercised.addAll(nullObject.executed);

        scenarios++;
        Vm nested = new Vm(original, factory);
        Node activity = new Node(MapsBackExitPatch.MAIN), dispatcher = nested.factory(activity);
        Node fallback = (Node)nested.queue(dispatcher).getFirst();
        Node detail = nested.nested(dispatcher, true);
        Node disabled = nested.nested(dispatcher, false);
        check(nested.select(dispatcher, -1) == detail, "Newer disabled handler is skipped before enabled detail");
        nested.press(dispatcher);
        equal(1, nested.nestedCalls, "Nested Back delegates to native handler");
        equal(0, nested.finished.size(), "Nested Back must not finish Maps");
        Node dialog = nested.nested(dispatcher, true);
        check(nested.select(dispatcher, -1) == dialog, "Most recently registered enabled native handler wins");
        nested.press(dispatcher);
        equal(2, nested.nestedCalls, "Dialog Back stays native");
        nested.execute(method(nested.classes, "Lqh;->e(Z)V"), dialog, 0);
        check(nested.select(dispatcher, -1) == detail, "Stopping dialog restores detail priority");
        nested.execute(method(nested.classes, "Lqh;->e(Z)V"), detail, 0);
        check(nested.select(dispatcher, -1) == fallback, "All nested handlers disabled exposes Maps fallback");
        nested.press(dispatcher);
        equal(Collections.singletonList(activity), nested.finished, "Only root Back issues task removal");
        check(nested.queue(dispatcher).contains(disabled), "Disabled handler remains in queue without causing exit");
        exercised.addAll(nested.executed);

        scenarios++;
        Vm system = new Vm(original, factory);
        Node bridge = new Node("Likh;");
        bridge.fields.put("Likh;->a:Landroid/window/OnBackInvokedDispatcher;", new Node("Landroid/window/OnBackInvokedDispatcher;"));
        bridge.fields.put("Likh;->b:I", 0);
        bridge.fields.put("Likh;->e:Landroid/window/OnBackInvokedCallback;", new Node("Landroid/window/OnBackInvokedCallback;"));
        system.execute(method(system.classes, "Likh;->c(Z)V"), bridge, 1);
        system.execute(method(system.classes, "Likh;->c(Z)V"), bridge, 1);
        equal(1, system.registered, "Native enabled bridge registers platform callback only once");
        equal(0, system.unregistered, "Enabled bridge is retained");
        system.execute(method(system.classes, "Likh;->c(Z)V"), bridge, 0);
        equal(1, system.unregistered, "Native bridge unregisters when disabled");
        equal(0, system.finished.size(), "Platform registration itself never finishes Activity");
        exercised.addAll(system.executed);
        for (String d : Arrays.asList(MapsBackExitPatch.TARGET_METHOD, INSTALL, INIT, BACK, "Lqi;-><init>(Z)V",
                NATIVE_REGISTER, "Lqi;->g(Lgdz;)Lqh;", "Lqh;-><init>(Lqi;Lgdz;)V", "Lijz;-><init>(Lgdz;Z)V",
                "Lamee;->h(Lijz;)V", SELECT, "Lijz;->h()Z", "Lqh;->b()V", "Likh;->c(Z)V", "Likh;->e(Z)V"))
            check(exercised.contains(d), "Native or emitted method was not exercised " + d);
        System.out.println("PASS emitted/native DEX VM: scenarios=" + scenarios + "; methods exercised=" + exercised.size()
                + "; Maps/non-Maps and API28/33/37 factory paths, native addFirst/first-enabled priority, nested callbacks,"
                + " root task-removal call and platform-registration gates verified; host boundaries explicit; Android UI not executed");
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && (args[0].equals("fingerprints") || args[0].equals("fingerprint"))) fingerprints(args[1]);
        else if (args.length == 2 && args[0].equals("guards")) guards(args[1]);
        else if (args.length == 4 && args[0].equals("merge")) merge(args[1], args[2], args[3]);
        else if (args.length == 3 && args[0].equals("audit")) audit(args[1], args[2]);
        else throw new IllegalArgumentException("Usage: MapsBackAudit fingerprints|guards APK; merge BASE_DEX ADDITION_DEX OUTPUT_DEX; audit ORIGINAL_APK PATCHED_APK");
    }
}
