import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.security.MessageDigest;
import app.hiro.mapsstartup.patches.MapsStartupLocationPatch;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** APK/loader scope audit. These host checks do not claim Android UI execution. */
public final class MapsStartupAudit {
    static final String LOADER = "Lapp/hiro/mapsstartup/patches/";
    static final String EXT = "Lapp/hiro/mapsstartup/";
    static final String CATEGORY = "Lafhc;";
    // Published v1.0.360 inputs. Replacement/normalization is a private QA step,
    // never an application-time acceptance path for already-patched APKs.
    static final String LEGACY_LOADER_SHA = "d113da030a7a48f827fe9c1f7e4115cc72cddc246d8764fce4b6462638854d41";
    static final String LEGACY_RUNTIME_SHA = "8bb1a90aca616a1831eb7bbbf2bfb18ce4eb1f4d0bba6b061b4a1a5fbb20fb9d";
    static final String LEGACY_BRIDGE_SHA = "599aa8a1b9ca2352d136a347196158eb5f08610553d82ee87ec6057fce05adb6";
    static long checks;
    static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    static Map<String,ClassDef> read(String path) throws Exception {
        Map<String,ClassDef> out = new TreeMap<String,ClassDef>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String name : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(name).getDexFile().getClasses())
            check(out.put(c.getType(), c) == null, "Duplicate class " + c.getType());
        return out;
    }
    static MapsStartupLocationPatch.Lookup lookup(final Map<String,ClassDef> all) {
        return new MapsStartupLocationPatch.Lookup() { public ClassDef get(String owner) { return all.get(owner); }};
    }
    static byte[] canonical(ClassDef c) throws Exception {
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(c)));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally { store.close(); }
    }
    static Method method(ClassDef c, String descriptor) { return MapsStartupLocationPatch.find(c, descriptor); }
    static String desc(MethodReference m) { return MapsStartupLocationPatch.descriptor(m); }
    static List<Instruction> instructions(Method m) {
        List<Instruction> out = new ArrayList<Instruction>();
        if (m.getImplementation() != null) for (Instruction i : m.getImplementation().getInstructions()) out.add(i);
        return out;
    }
    static void rejected(Runnable action, String why) {
        boolean failed = false;
        try { action.run(); } catch (IllegalStateException expected) { failed = true; }
        check(failed, why);
    }
    static void fingerprints(String apk) throws Exception {
        Map<String,ClassDef> all = read(apk);
        for (int n = 0; n < MapsStartupLocationPatch.CONTRACTS.length; n++) {
            String descriptor = MapsStartupLocationPatch.CONTRACTS[n];
            Method m = method(all.get(MapsStartupLocationPatch.owner(descriptor)), descriptor);
            System.out.println(n + " " + MapsStartupLocationPatch.fingerprint(m) + " "
                + MapsStartupLocationPatch.fingerprint(new MutableMethod(m)) + " " + m.getAccessFlags()
                + " " + m.getImplementation().getRegisterCount() + " " + descriptor);
        }
    }
    static void guards(String apk) throws Exception {
        for (String pkg : MapsStartupLocationPatch.PACKAGES) {
            MapsStartupLocationPatch.validatePackage(pkg, MapsStartupLocationPatch.VERSION, MapsStartupLocationPatch.CODE);
            rejected(() -> MapsStartupLocationPatch.validatePackage(pkg, "different", MapsStartupLocationPatch.CODE), "Wrong version accepted");
            rejected(() -> MapsStartupLocationPatch.validatePackage(pkg, MapsStartupLocationPatch.VERSION, "different"), "Wrong code accepted");
        }
        rejected(() -> MapsStartupLocationPatch.validatePackage("com.example.other", MapsStartupLocationPatch.VERSION, MapsStartupLocationPatch.CODE), "Wrong package accepted");
        Set<String> advertised = new TreeSet<String>();
        for (Object entry : MapsStartupLocationPatch.PATCH.getCompatiblePackages()) {
            kotlin.Pair<?,?> p = (kotlin.Pair<?,?>)entry;
            check(advertised.add((String)p.getFirst()), "Duplicate compatible package");
            check(Collections.singleton(MapsStartupLocationPatch.VERSION).equals(p.getSecond()), "Unexpected compatible versions");
        }
        check(advertised.equals(MapsStartupLocationPatch.PACKAGES), "Manager package selection differs");
        Map<String,ClassDef> all = read(apk);
        MapsStartupLocationPatch.validateNative(lookup(all));
        for (int n = 0; n < MapsStartupLocationPatch.CONTRACTS.length; n++) {
            final int number = n;
            String descriptor = MapsStartupLocationPatch.CONTRACTS[n];
            MutableMethod m = new MutableMethod(method(all.get(MapsStartupLocationPatch.owner(descriptor)), descriptor));
            MapsStartupLocationPatch.validateContract(m, n);
            m.getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
            rejected(() -> MapsStartupLocationPatch.validateContract(m, number), "Modified body accepted " + descriptor);
        }
        MutableClass applied = new MutableClass(all.get(MapsStartupLocationPatch.OWNER));
        MapsStartupLocationPatch.apply(applied);
        auditActivity(all.get(MapsStartupLocationPatch.OWNER), applied);
        rejected(() -> MapsStartupLocationPatch.apply(applied), "Repeated patch application accepted");
        MutableClass drift = new MutableClass(all.get(MapsStartupLocationPatch.OWNER));
        MutableMethod last = (MutableMethod)method(drift, MapsStartupLocationPatch.CONTRACTS[7]);
        last.getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
        byte[] snapshot = canonical(drift);
        rejected(() -> MapsStartupLocationPatch.apply(drift), "Last-hook drift accepted");
        check(Arrays.equals(snapshot, canonical(drift)), "Guard failure partially mutated APK");
        System.out.println("PASS guards: both packages, version/code/body mismatch, eight hook contracts, native mode/copied-fix/direct-camera/acknowledgement contracts, preflight atomicity and duplicate rejection; assertions=" + checks);
    }
    static void auditActivity(ClassDef original, ClassDef patched) throws Exception {
        check(MapsStartupLocationPatch.OWNER.equals(original.getType()), "Wrong activity owner");
        MutableClass restored = new MutableClass(patched);
        check(!original.getInterfaces().contains(MapsStartupLocationPatch.HOST), "Original already has location host");
        check(restored.getInterfaces().remove(MapsStartupLocationPatch.HOST), "Missing location host interface");
        Method bridge = method(restored, MapsStartupLocationPatch.OWNER + "->" + MapsStartupLocationPatch.BRIDGE + "(Z)I");
        check(bridge.getAccessFlags() == 17 && bridge.getImplementation().getRegisterCount() == 14, "Native bridge declaration changed");
        auditBridge(bridge);
        check(restored.getMethods().remove(bridge), "Cannot remove expected bridge");
        for (int n = 0; n < MapsStartupLocationPatch.HOOK_NAMES.length; n++) {
            MutableMethod after = (MutableMethod)method(restored, MapsStartupLocationPatch.CONTRACTS[n]);
            Method before = method(original, MapsStartupLocationPatch.CONTRACTS[n]);
            List<Instruction> a = instructions(after), b = instructions(before);
            check(a.size() == b.size() + 2, "Hook must add one invoke and one alignment NOP " + n);
            check(a.get(1).getOpcode() == Opcode.NOP, "Hook alignment NOP missing " + n);
            Instruction injected = a.get(0);
            check(injected.getOpcode() == Opcode.INVOKE_STATIC_RANGE && injected instanceof RegisterRangeInstruction
                && injected instanceof ReferenceInstruction, "Hook is not a non-clobbering static invoke " + n);
            RegisterRangeInstruction regs = (RegisterRangeInstruction)injected;
            int count = MapsStartupLocationPatch.HOOK_ARGUMENTS[n].isEmpty() ? 1 : 2;
            check(regs.getRegisterCount() == count && regs.getStartRegister() == before.getImplementation().getRegisterCount() - count,
                "Hook references incorrect parameter registers " + n);
            Reference ref = ((ReferenceInstruction)injected).getReference();
            check(ref instanceof MethodReference, "Hook reference is not a method");
            String expected = MapsStartupLocationPatch.RUNTIME + "->" + MapsStartupLocationPatch.HELPER_NAMES[n]
                + "(Landroid/app/Activity;" + MapsStartupLocationPatch.HOOK_ARGUMENTS[n] + ")V";
            check(expected.equals(desc((MethodReference)ref)), "Wrong runtime hook " + n);
            after.getImplementation().removeInstruction(1);
            after.getImplementation().removeInstruction(0);
        }
        check(Arrays.equals(canonical(new MutableClass(original)), canonical(restored)),
            "Activity differs beyond eight entry hooks, one interface and one native bridge");
    }
    static void auditBridge(Method bridge) throws Exception {
        // The complete expected bridge is small enough to compare, plus independent
        // call/field checks ensure there is no alternate force-location or toggle path.
        ClassDef actual = new ImmutableClassDef(bridge.getDefiningClass(), 1, "Ljava/lang/Object;", Collections.emptyList(),
            null, Collections.emptySet(), Collections.emptyList(), Collections.singleton(bridge));
        ClassDef expected = new ImmutableClassDef(bridge.getDefiningClass(), 1, "Ljava/lang/Object;", Collections.emptyList(),
            null, Collections.emptySet(), Collections.emptyList(), Collections.singleton(MapsStartupLocationPatch.bridge()));
        check(Arrays.equals(canonical(expected), canonical(actual)), "Native bridge body differs");
        List<String> calls = new ArrayList<String>();
        Set<String> fields = new TreeSet<String>();
        for (Instruction i : instructions(bridge)) if (i instanceof ReferenceInstruction) {
            Reference ref = ((ReferenceInstruction)i).getReference();
            if (ref instanceof MethodReference) calls.add(desc((MethodReference)ref));
            if (ref instanceof FieldReference) {
                FieldReference f = (FieldReference)ref;
                fields.add(f.getDefiningClass() + "->" + f.getName() + ":" + f.getType());
            }
        }
        check(calls.equals(Arrays.asList("Lcqpp;->a()Ljava/lang/Object;", "Laiau;->K()Z", "Lamej;->h()Lamgk;",
            "Lbjbd;-><init>()V", "Lamej;->I(Lbjbd;)Z", "Lbjja;->c()Lbjjc;", "Lamdp;->b()Lbkmd;",
            "Lbjbd;->v()Lbjaw;", "Lbjaw;->v(Lbjaw;Lbjaw;D)Z", "Lamej;->b()F", "Lamdp;->a()Lbjox;",
            "Lbjoz;-><init>(Lbikz;FFFLbjox;Lbjoy;)V", "Lamdp;->l(Lbjoz;Z)V")), "Unexpected native call sequence");
        check(!calls.contains("Lamcr;->f()V") && !calls.contains("Lamcr;->g()V"), "Asynchronous toggle callback path remains");
        check(fields.equals(new TreeSet<String>(Arrays.asList(
            "Lnce;->bx:Lcom/google/android/apps/gmm/base/layout/MainLayout;",
            "Lcom/google/android/apps/gmm/base/layout/MainLayout;->i:Lcqpp;",
            "Lamcr;->p:Lamdp;", "Lamdp;->d:Lamej;", "Lamgk;->a:Lamgk;", "Lamgk;->l:Lamgk;", "Lamgk;->m:Lamgk;",
            "Lamdp;->g:Lbizr;", "Lamdp;->f:Z", "Lamdp;->k:Lamds;", "Lamdp;->l:Lamdt;", "Lamdp;->p:Lbjja;",
            "Lbjjc;->a:Lbjaw;", "Lbjjc;->h:F", "Lbjjc;->e:F", "Lbjjc;->d:F", "Lbkmd;->b:Lbkmd;", "Lbkmd;->c:Lbkmd;",
            "Lbjoy;->a:Lbjoy;", "Lbjoy;->b:Lbjoy;"))),
            "Native bridge field scope differs");
    }
    static void auditCategory(ClassDef original, ClassDef patched) throws Exception {
        String target = CATEGORY + "->a()Landroid/view/View;";
        MutableClass restored = new MutableClass(patched);
        MutableMethod after = (MutableMethod)method(restored, target);
        Method before = method(original, target);
        List<Instruction> out = instructions(after), in = instructions(before);
        check(out.size() == in.size() + 1, "Category patch instruction count differs");
        int index = out.size() - 2;
        Instruction addition = out.get(index);
        check(addition.getOpcode() == Opcode.CONST_4 && ((OneRegisterInstruction)addition).getRegisterA() == 1
                && ((NarrowLiteralInstruction)addition).getNarrowLiteral() == 0, "Category patch is not original null override");
        check(out.get(index + 1).getOpcode() == Opcode.RETURN_OBJECT
                && ((OneRegisterInstruction)out.get(index + 1)).getRegisterA() == 1, "Category return changed");
        after.getImplementation().removeInstruction(index);
        check(Arrays.equals(canonical(new MutableClass(original)), canonical(restored)), "Category owner changed beyond approved row suppression");
    }
    static boolean dexOrSignature(String name) {
        return name.matches("classes(?:[0-9]+)?\\.dex") || name.matches("(?i)META-INF/(?:MANIFEST\\.MF|[^/]+\\.(?:SF|RSA|DSA|EC))");
    }
    static int readChunk(InputStream in, byte[] buf) throws IOException {
        int total = 0, count;
        while (total < buf.length && (count = in.read(buf, total, buf.length - total)) != -1) total += count;
        return total;
    }
    static String fileSha(String path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(path)) {
            byte[] buffer = new byte[65536]; int n;
            while ((n = in.read(buffer)) >= 0) if (n != 0) digest.update(buffer, 0, n);
        }
        StringBuilder result = new StringBuilder();
        for (byte b : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }
    static boolean sameBytes(ZipFile a, ZipEntry x, ZipFile b, ZipEntry y) throws Exception {
        if (x == null || y == null || x.getSize() != y.getSize()) return false;
        byte[] xx = new byte[65536], yy = new byte[65536];
        try (InputStream ax = a.getInputStream(x); InputStream by = b.getInputStream(y)) {
            int ac, bc;
            while ((ac = readChunk(ax, xx)) > 0) {
                bc = readChunk(by, yy);
                if (ac != bc || !Arrays.equals(Arrays.copyOf(xx, ac), Arrays.copyOf(yy, bc))) return false;
            }
            return by.read() == -1;
        }
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
    static int auditResources(String original, String patched) throws Exception {
        int count = 0;
        try (ZipFile a = new ZipFile(original); ZipFile b = new ZipFile(patched)) {
            Set<String> before = new TreeSet<String>(), after = new TreeSet<String>();
            Enumeration<? extends ZipEntry> aa = a.entries(), bb = b.entries();
            while (aa.hasMoreElements()) {
                ZipEntry e = aa.nextElement(); if (e.isDirectory() || dexOrSignature(e.getName())) continue;
                before.add(e.getName()); check(sameBytes(a, e, b, b.getEntry(e.getName())), "Resource changed " + e.getName()); count++;
            }
            while (bb.hasMoreElements()) {
                ZipEntry e = bb.nextElement(); if (!e.isDirectory() && !dexOrSignature(e.getName())) after.add(e.getName());
            }
            check(before.equals(after), "Non-DEX entry inventory changed");
        }
        return count;
    }
    static void audit(String original, String patched, String runtimePath, boolean category) throws Exception {
        Map<String,ClassDef> before = read(original), after = read(patched), runtime = read(runtimePath);
        check(runtime.containsKey(MapsStartupLocationPatch.RUNTIME) && runtime.containsKey(MapsStartupLocationPatch.HOST), "Missing extension classes");
        Set<String> expected = new TreeSet<String>(before.keySet());
        for (ClassDef c : runtime.values()) {
            check(c.getType().startsWith(EXT) && !c.getType().startsWith(LOADER), "Unexpected runtime class namespace");
            check(expected.add(c.getType()), "Runtime collides with existing app class");
            check(after.containsKey(c.getType()) && Arrays.equals(canonical(c), canonical(after.get(c.getType()))), "Runtime class differs " + c.getType());
        }
        check(expected.equals(after.keySet()), "APK class inventory differs");
        Set<String> bytePreserved = bytePreservedClasses(original, patched);
        int untouched = 0, activityCount = 0, categoryCount = 0;
        for (ClassDef c : before.values()) {
            ClassDef out = after.get(c.getType());
            if (MapsStartupLocationPatch.OWNER.equals(c.getType())) {
                auditActivity(c, out); activityCount++;
            } else if (category && CATEGORY.equals(c.getType())) {
                auditCategory(c, out); categoryCount++;
            } else {
                if (!bytePreserved.contains(c.getType())) check(Arrays.equals(canonical(c), canonical(out)), "Unrelated native class changed " + c.getType());
                untouched++;
            }
        }
        check(activityCount == 1 && categoryCount == (category ? 1 : 0), "Wrong modified owner count");
        int resources = auditResources(original, patched);
        System.out.println("PASS APK scope audit: native classes unchanged=" + untouched + "; byte-identical DEX classes=" + bytePreserved.size()
            + "; lifecycle/touch hooks=8; native bridge=1; preserved category suppression=" + category
            + "; resources unchanged=" + resources + "; runtime classes=" + runtime.size() + "; assertions=" + checks + "; physical Android UI not tested");
    }
    static void merge(String basePath, String additionPath, String output) throws Exception {
        check(LEGACY_LOADER_SHA.equals(fileSha(basePath)), "Replacement baseline is not the pinned v1.0.360 loader");
        Map<String,ClassDef> base = read(basePath), add = read(additionPath), all = new TreeMap<String,ClassDef>(base);
        check(!add.isEmpty(), "Empty provider addition");
        int removed = 0;
        for (String name : base.keySet()) if (name.startsWith(LOADER)) { all.remove(name); removed++; }
        check(removed > 0 && base.containsKey(LOADER + "MapsStartupLocationPatch;"), "Pinned startup provider missing");
        check(add.containsKey(LOADER + "MapsStartupLocationPatch;"), "Missing public patch provider");
        for (ClassDef c : add.values()) {
            check(c.getType().startsWith(LOADER), "New loader class outside provider namespace");
            check(all.put(c.getType(), c) == null, "Provider collision");
        }
        DexPool.writeTo(output, new ImmutableDexFile(Opcodes.getDefault(), all.values()));
        Map<String,ClassDef> written = read(output);
        check(all.keySet().equals(written.keySet()), "Merged loader inventory differs");
        for (ClassDef c : all.values()) check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))), "Loader class changed " + c.getType());
        for (ClassDef c : base.values()) if (!c.getType().startsWith(LOADER))
            check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))), "Unrelated provider changed " + c.getType());
        System.out.println("PASS loader replacement: unrelated provider classes unchanged=" + (base.size() - removed)
            + "; pinned v1.0.360 startup classes replaced=" + removed + "; new startup provider classes=" + add.size() + "; assertions=" + checks);
    }
    static void normalizeLegacy(String input, String output, String legacyRuntimePath) throws Exception {
        check(!new File(input).getCanonicalFile().equals(new File(output).getCanonicalFile()), "Normalization must use a separate test APK");
        check(!new File(output).exists(), "Normalization output already exists");
        check(LEGACY_RUNTIME_SHA.equals(fileSha(legacyRuntimePath)), "Legacy runtime differs from pinned v1.0.360 extension");
        Map<String,ClassDef> before = read(input), runtime = read(legacyRuntimePath);
        Set<String> extensionNames = new TreeSet<String>();
        for (String name : before.keySet()) if (name.startsWith(EXT)) extensionNames.add(name);
        check(extensionNames.equals(runtime.keySet()) && runtime.size() == 3, "Legacy extension class inventory differs");
        for (ClassDef c : runtime.values()) check(Arrays.equals(canonical(c), canonical(before.get(c.getType()))),
            "Legacy extension class changed " + c.getType());
        MutableClass restored = new MutableClass(before.get(MapsStartupLocationPatch.OWNER));
        check(restored.getInterfaces().remove(MapsStartupLocationPatch.HOST), "Legacy host interface missing");
        int bridges = 0;
        for (Method m : restored.getMethods()) if (MapsStartupLocationPatch.BRIDGE.equals(m.getName())) bridges++;
        check(bridges == 1, "Ambiguous legacy bridge");
        Method bridge = method(restored, MapsStartupLocationPatch.OWNER + "->" + MapsStartupLocationPatch.BRIDGE + "()I");
        check(bridge.getAccessFlags() == 17 && bridge.getImplementation().getRegisterCount() == 5
            && LEGACY_BRIDGE_SHA.equals(MapsStartupLocationPatch.fingerprint(bridge)), "Legacy bridge body differs");
        check(restored.getMethods().remove(bridge), "Cannot remove verified legacy bridge");
        for (int n = 0; n < MapsStartupLocationPatch.HOOK_NAMES.length; n++) {
            MutableMethod m = (MutableMethod)method(restored, MapsStartupLocationPatch.CONTRACTS[n]);
            List<Instruction> ins = instructions(m);
            check(ins.size() > 2 && ins.get(0).getOpcode() == Opcode.INVOKE_STATIC_RANGE
                && ins.get(1).getOpcode() == Opcode.NOP, "Legacy hook prefix differs " + n);
            RegisterRangeInstruction regs = (RegisterRangeInstruction)ins.get(0);
            int count = MapsStartupLocationPatch.HOOK_ARGUMENTS[n].isEmpty() ? 1 : 2;
            check(regs.getRegisterCount() == count && regs.getStartRegister() == m.getImplementation().getRegisterCount() - count,
                "Legacy hook parameter registers differ " + n);
            Reference ref = ((ReferenceInstruction)ins.get(0)).getReference();
            String wanted = MapsStartupLocationPatch.RUNTIME + "->" + MapsStartupLocationPatch.HELPER_NAMES[n]
                + "(Landroid/app/Activity;" + MapsStartupLocationPatch.HOOK_ARGUMENTS[n] + ")V";
            check(ref instanceof MethodReference && wanted.equals(desc((MethodReference)ref)), "Legacy hook target differs " + n);
            m.getImplementation().removeInstruction(1);
            m.getImplementation().removeInstruction(0);
            MapsStartupLocationPatch.validateContract(m, n);
        }
        Map<String,ClassDef> expected = new TreeMap<String,ClassDef>(before);
        for (String name : extensionNames) expected.remove(name);
        expected.put(restored.getType(), restored);
        MapsStartupLocationPatch.validateNative(lookup(expected));

        // Only DEX files containing the eight exact hooks or legacy extension are
        // rewritten. Every unrelated archive entry is copied with identical payload.
        Map<String,byte[]> replacements = new HashMap<String,byte[]>();
        Set<String> omittedDex = new HashSet<String>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(input), Opcodes.getDefault());
        for (String name : dex.getDexEntryNames()) {
            DexFile file = dex.getEntry(name).getDexFile();
            List<ClassDef> kept = new ArrayList<ClassDef>(); boolean changed = false;
            for (ClassDef c : file.getClasses()) {
                if (extensionNames.contains(c.getType())) { changed = true; continue; }
                if (restored.getType().equals(c.getType())) { kept.add(restored); changed = true; }
                else kept.add(c);
            }
            if (!changed) continue;
            if (kept.isEmpty()) { omittedDex.add(name); continue; }
            MemoryDataStore store = new MemoryDataStore();
            try {
                DexPool.writeTo(store, new ImmutableDexFile(file.getOpcodes(), kept));
                replacements.put(name, Arrays.copyOf(store.getBuffer(), store.getSize()));
            } finally { store.close(); }
        }
        check(replacements.size() == 1, "Unexpected legacy activity DEX scope");
        boolean completed = false;
        try {
            try (ZipFile source = new ZipFile(input); ZipOutputStream target = new ZipOutputStream(new FileOutputStream(output))) {
                Set<String> names = new HashSet<String>();
                Enumeration<? extends ZipEntry> entries = source.entries();
                byte[] buffer = new byte[65536];
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement(); String name = e.getName();
                    check(names.add(name), "Duplicate input archive entry");
                    if (omittedDex.contains(name) || name.matches("(?i)META-INF/(?:MANIFEST\\.MF|[^/]+\\.(?:SF|RSA|DSA|EC))")) continue;
                    ZipEntry out = new ZipEntry(name);
                    if (e.getTime() >= 0) out.setTime(e.getTime());
                    target.putNextEntry(out);
                    if (replacements.containsKey(name)) target.write(replacements.get(name));
                    else try (InputStream in = source.getInputStream(e)) {
                        int n; while ((n = in.read(buffer)) >= 0) if (n != 0) target.write(buffer, 0, n);
                    }
                    target.closeEntry();
                }
            }
            Map<String,ClassDef> after = read(output);
            check(after.keySet().equals(expected.keySet()), "Normalized class inventory differs");
            Set<String> unchanged = bytePreservedClasses(input, output);
            for (ClassDef c : expected.values()) if (!unchanged.contains(c.getType()))
                check(Arrays.equals(canonical(c), canonical(after.get(c.getType()))), "Normalization changed unrelated code " + c.getType());
            MapsStartupLocationPatch.validateNative(lookup(after));
            int resources = auditResources(input, output);
            completed = true;
            System.out.println("PASS private legacy normalization: exact published v1.0.0 runtime/bridge and eight hook pairs removed; restored native contracts verified; unrelated classes preserved="
                + (after.size() - 1) + "; resources preserved=" + resources + "; signatures stripped; assertions=" + checks);
        } finally {
            if (!completed) new File(output).delete();
        }
    }
    static void equivalent(String a, String b) throws Exception {
        Map<String,ClassDef> aa = read(a), bb = read(b);
        check(aa.keySet().equals(bb.keySet()), "Equivalent APK class inventories differ");
        Set<String> bytePreserved = bytePreservedClasses(a, b);
        for (ClassDef c : aa.values()) if (!bytePreserved.contains(c.getType()))
            check(Arrays.equals(canonical(c), canonical(bb.get(c.getType()))), "Equivalent APK class differs " + c.getType());
        int resources = auditResources(a, b);
        System.out.println("PASS equivalent APK semantic contents: classes=" + aa.size() + "; resources=" + resources + "; assertions=" + checks);
    }
    public static void main(String[] args) throws Exception {
        if (args[0].equals("fingerprint") || args[0].equals("fingerprints")) fingerprints(args[1]);
        else if (args[0].equals("guards") || args[0].equals("guard-tests")) guards(args[1]);
        else if (args[0].equals("merge")) merge(args[1], args[2], args[3]);
        else if (args[0].equals("normalize-legacy")) normalizeLegacy(args[1], args[2], args[3]);
        else if (args[0].equals("audit") || args[0].equals("audit-app")) audit(args[1], args[2], args[3], false);
        else if (args[0].equals("audit-category")) audit(args[1], args[2], args[3], true);
        else if (args[0].equals("equivalent")) equivalent(args[1], args[2]);
        else throw new IllegalArgumentException("Unknown audit mode");
    }
}
