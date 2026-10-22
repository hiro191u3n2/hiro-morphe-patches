import app.hiro.berry.patches.BerryProfiles;
import app.hiro.berry.patches.BerryProfiles.Profile;
import app.hiro.berry.patches.MethodContract;
import app.morphe.patcher.PackageMetadata;
import app.morphe.patcher.PatcherConfig;
import app.morphe.patcher.dex.BytecodeMode;
import app.morphe.patcher.dex.DexVerifier;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.PatchClasses;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableField;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11n;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Contract tests, never Android execution or device validation.
 *
 * No arguments: tiny, explicitly synthetic definitions exercise the shared
 * validators. Optional APK paths: the original DEX is read and each production
 * CHECK block is invoked directly against a real Morphe context. Negative cases
 * alter only in-memory copies; no APK is changed, compiled, or written.
 *
 * Example: java -ea -cp tests:Berry_Browser_v1.3.1.mpp:morphe.jar
 *     BerryCompatibilityTest original383740.apk original383931.apk
 */
public final class BerryCompatibilityTest {
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private static final String PROBE = "Ltest/Contract;->probe()V";
    // SHA-256 of a separately specified canonical JSON contract, not generated
    // by MethodContract during this test: public static, 1 register, const0/return.
    private static final String PROBE_HASH =
        "e1634aa5ddbb6227656af2c8caca30abd1cdd410eff77582c39ca070a09c9d58";
    private static final String ZOOM_PROVIDER = "app.hiro.berryzoom.patches.OneHandZoomPatch";
    private static final String SHARE_PROVIDER = "app.hiro.berryshare.patches.BrowserVisibilityPatch";
    private static final String FOREGROUND_PROVIDER = "app.hiro.berry.patches.ForegroundTabsPatch";
    private static final String[] PROVIDERS = { FOREGROUND_PROVIDER, SHARE_PROVIDER, ZOOM_PROVIDER };
    private static final String[] TARGET_CLASSES = {
        "Lcx;", "Lz31;", "Lc41;", "Lg60;", "Lc70;", "Ltt5;", "Lb03;", "Lj8;",
        "Lhc1;", "Lz32;", "Lfy;", "Lmo0;", "Ln8;"
    };
    private static int checks;
    private static int realInputs;

    private interface ThrowingRunnable { void run() throws Exception; }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }

    private static void rejects(String label, ThrowingRunnable action) throws Exception {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            require(expected.getMessage() != null && expected.getMessage().startsWith("Berry"),
                label + " failed outside the production compatibility guard: " + expected);
            return;
        }
        throw new AssertionError(label + " unexpectedly accepted an invalid fixture");
    }

    private static ClassDef fixture(String type, String superclass,
                                    Iterable<? extends Field> fields,
                                    Iterable<? extends Method> methods) {
        return new ImmutableClassDef(type, 1, superclass, Collections.emptyList(),
            "TEST_ONLY_FIXTURE", Collections.emptySet(), fields, methods);
    }

    private static ClassDef empty(String type, String superclass) {
        return fixture(type, superclass, Collections.emptyList(), Collections.emptyList());
    }

    private static Field fixtureField(String type, int access) {
        return new ImmutableField("Ltest/Owner;", "view", type, access, null,
            Collections.emptySet(), Collections.emptySet());
    }

    private static ClassDef fieldOwner(String type, int access) {
        return fixture("Ltest/Owner;", "Ljava/lang/Object;",
            Collections.singletonList(fixtureField(type, access)), Collections.emptyList());
    }

    private static Method probe(int literal) {
        return new ImmutableMethod("Ltest/Contract;", "probe", Collections.emptyList(), "V", 9,
            Collections.emptySet(), Collections.emptySet(),
            new ImmutableMethodImplementation(1, Arrays.asList(
                new ImmutableInstruction11n(Opcode.CONST_4, 0, literal),
                new ImmutableInstruction10x(Opcode.RETURN_VOID)),
                Collections.emptyList(), Collections.emptyList()));
    }

    private static void synthetic() throws Exception {
        Map<String, ClassDef> old = new LinkedHashMap<>();
        old.put("Lcx;", empty("Lcx;", "Landroid/webkit/WebView;"));
        Map<String, ClassDef> current = new LinkedHashMap<>();
        current.put("Lpx;", empty("Lpx;", "Landroid/webkit/WebView;"));
        Profile a = BerryProfiles.select(BerryProfiles.PACKAGE, old::get);
        Profile b = BerryProfiles.select(BerryProfiles.PACKAGE, current::get);
        require("Lcx;".equals(a.type("Lcx;")), "Old layout should retain its WebView descriptor");
        require("Lpx;".equals(b.type("Lcx;")), "Current layout should select its verified WebView");
        require("Lz13;->O0:Lr52;".equals(b.ref("Lb03;->M0:Lz32;")),
            "Browser adapter mapping must use O0; M0 now has another meaning");
        require("Lr51;->j0:Lu51;".equals(b.ref("Lz31;->h0:Lc41;")),
            "Parent touch mapping changed");
        require("Lu51;->r:Lpx;".equals(b.ref("Lc41;->q:Lcx;")),
            "Wrapper WebView field mapping changed");
        require("[[Lz13;".equals(b.type("[[Lb03;")), "Array type remapping failed");
        require("Ln8;".equals(b.type("Ln8;")), "Unchanged chooser item type was remapped");
        require(a.shareRegisters == 19 && a.shareDialogRegister == 6 && a.shareReturnRegister == 5,
            "Original callback register contract changed");
        require(b.shareRegisters == 20 && b.shareDialogRegister == 7 && b.shareReturnRegister == 6,
            "Current callback register contract changed");
        rejects("wrong package", () -> BerryProfiles.select("test.other.package", current::get));
        rejects("missing WebView", () -> BerryProfiles.select(BerryProfiles.PACKAGE, x -> null));
        rejects("wrong WebView parent", () -> BerryProfiles.select(BerryProfiles.PACKAGE,
            x -> "Lpx;".equals(x) ? empty(x, "Ljava/lang/Object;") : null));
        Map<String, ClassDef> ambiguous = new LinkedHashMap<>(old);
        ambiguous.putAll(current);
        rejects("ambiguous layout", () -> BerryProfiles.select(BerryProfiles.PACKAGE, ambiguous::get));

        BerryProfiles.verifyMethod(probe(0), PROBE, PROBE_HASH);
        checks++;
        rejects("changed instruction literal", () -> BerryProfiles.verifyMethod(probe(1), PROBE, PROBE_HASH));
        rejects("missing method", () -> BerryProfiles.verifyMethod(null, PROBE, PROBE_HASH));
        rejects("wrong method descriptor", () -> BerryProfiles.verifyMethod(probe(0),
            "Ltest/Contract;->other()V", PROBE_HASH));
        String field = "Ltest/Owner;->view:Landroid/webkit/WebView;";
        BerryProfiles.verifyField(fieldOwner("Landroid/webkit/WebView;", 1), field);
        checks++;
        rejects("missing field owner", () -> BerryProfiles.verifyField(null, field));
        rejects("missing field", () -> BerryProfiles.verifyField(empty("Ltest/Owner;", "Ljava/lang/Object;"), field));
        rejects("changed field type", () -> BerryProfiles.verifyField(fieldOwner("Landroid/view/View;", 1), field));
        rejects("static field", () -> BerryProfiles.verifyField(fieldOwner("Landroid/webkit/WebView;", 9), field));
        rejects("private field", () -> BerryProfiles.verifyField(fieldOwner("Landroid/webkit/WebView;", 2), field));
    }

    private static Map<String, ClassDef> load(File apk) throws Exception {
        Map<String, ClassDef> result = new LinkedHashMap<>();
        com.android.tools.smali.dexlib2.iface.MultiDexContainer<?> container =
            DexFileFactory.loadDexContainer(apk, Opcodes.getDefault());
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef cls : container.getEntry(entry).getDexFile().getClasses()) {
                if (result.put(cls.getType(), cls) != null)
                    throw new AssertionError("Duplicate original class " + cls.getType());
            }
        }
        return result;
    }

    private static BytecodePatch checker(String provider) throws Exception {
        java.lang.reflect.Field field = Class.forName(provider).getDeclaredField("CHECK");
        field.setAccessible(true);
        return (BytecodePatch) field.get(null);
    }

    private static final DexVerifier NO_OUTPUT = new DexVerifier() {
        public void verifyDexFile(File file) { throw new AssertionError("CHECK tests must never compile DEX"); }
        public void verifyDexDirectory(File file) { throw new AssertionError("CHECK tests must never compile DEX"); }
        public void verifyApkFile(File file) { throw new AssertionError("CHECK tests must never produce an APK"); }
    };

    private static PatcherConfig config(File apk, File temporary) throws Exception {
        // Test-only constructor bridge: both pinned Patcher 21 and 22 execute the
        // same CHECK code. Neither constructor decodes/edits the supplied APK.
        try {
            return PatcherConfig.class.getConstructor(File.class, File.class, String.class,
                String.class, boolean.class, Set.class, BytecodeMode.class, DexVerifier.class,
                File.class).newInstance(apk, temporary, null, null, true,
                    Collections.emptySet(), BytecodeMode.FULL, NO_OUTPUT, null);
        } catch (NoSuchMethodException newer) {
            return PatcherConfig.class.getConstructor(File.class, File.class, Set.class,
                BytecodeMode.class, DexVerifier.class, File.class, String.class, boolean.class)
                .newInstance(apk, temporary, Collections.emptySet(), BytecodeMode.FULL,
                    NO_OUTPUT, null, null, false);
        }
    }

    private static BytecodePatchContext context(File apk, Path temporary,
                                               Map<String, ClassDef> classes) throws Exception {
        // Deliberately synthetic version labels prove structure is checked
        // independently of the stale3.83.74 string gate. Package remains real.
        PackageMetadata metadata = new PackageMetadata(BerryProfiles.PACKAGE,
            "TEST_ONLY_VERSION_LABEL", "0", null);
        BytecodePatchContext context = new BytecodePatchContext(config(apk, temporary.toFile()), metadata);
        context.setPatchClasses$morphe_patcher(new PatchClasses(new LinkedHashSet<>(classes.values())));
        context.setOpcodes$morphe_patcher(Opcodes.getDefault());
        return context;
    }

    private static Map<String, String> snapshot(BytecodePatchContext context, Profile profile) {
        Map<String, String> result = new TreeMap<>();
        context.classDefForEach(cls -> {
            result.put("class:" + cls.getType(), cls.getAccessFlags() + ":" + cls.getSuperclass());
            return kotlin.Unit.INSTANCE;
        });
        for (String original : TARGET_CLASSES) {
            ClassDef cls = context.classDefByOrNull(profile.type(original));
            if (cls == null) continue;
            for (Field field : cls.getFields()) result.put("field:" + field, "" + field.getAccessFlags());
            for (Method method : cls.getMethods()) result.put("method:" + method, MethodContract.sha256(method));
        }
        return result;
    }

    private static void checkFixture(String label, File apk, Path temporary,
                                     Map<String, ClassDef> classes, Profile profile,
                                     String[] providers, boolean accepted) throws Exception {
        try (BytecodePatchContext context = context(apk, temporary, classes)) {
            Map<String, String> before = snapshot(context, profile);
            ThrowingRunnable action = () -> {
                for (String provider : providers) checker(provider).execute(context);
            };
            if (accepted) {
                action.run();
                checks++;
            } else rejects(label, action);
            require(before.equals(snapshot(context, profile)),
                label + " changed bytecode or injected helper classes inside CHECK");
        }
    }

    private static ClassDef replaceMembers(ClassDef cls, Iterable<? extends Field> fields,
                                           Iterable<? extends Method> methods) {
        return new ImmutableClassDef(cls.getType(), cls.getAccessFlags(), cls.getSuperclass(),
            cls.getInterfaces(), cls.getSourceFile(), cls.getAnnotations(), fields, methods);
    }

    private static Map<String, ClassDef> changedMethod(Map<String, ClassDef> original,
                                                     String descriptor, boolean remove) {
        Map<String, ClassDef> result = new LinkedHashMap<>(original);
        String owner = descriptor.substring(0, descriptor.indexOf("->"));
        ClassDef cls = original.get(owner);
        List<Method> methods = new ArrayList<>();
        boolean found = false;
        for (Method method : cls.getMethods()) {
            if (!descriptor.equals(FORMAT.getMethodDescriptor(method))) { methods.add(method); continue; }
            found = true;
            if (remove) continue;
            MutableMethodImplementation implementation = new MutableMethodImplementation(method.getImplementation());
            implementation.addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
            methods.add(new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                method.getReturnType(), method.getAccessFlags(), method.getAnnotations(),
                method.getHiddenApiRestrictions(), implementation));
        }
        require(found, "Negative method fixture did not find its target");
        result.put(owner, replaceMembers(cls, cls.getFields(), methods));
        return result;
    }

    private static Map<String, ClassDef> changedField(Map<String, ClassDef> original,
                                                    String descriptor, boolean makePrivate) {
        Map<String, ClassDef> result = new LinkedHashMap<>(original);
        String owner = descriptor.substring(0, descriptor.indexOf("->"));
        ClassDef cls = original.get(owner);
        List<Field> fields = new ArrayList<>();
        boolean found = false;
        for (Field field : cls.getFields()) {
            if (!descriptor.equals(FORMAT.getFieldDescriptor(field))) { fields.add(field); continue; }
            found = true;
            fields.add(new ImmutableField(field.getDefiningClass(), field.getName(),
                makePrivate ? field.getType() : "Ljava/lang/Object;",
                makePrivate ? (field.getAccessFlags() & ~5) | 2 : field.getAccessFlags(),
                field.getInitialValue(), field.getAnnotations(), field.getHiddenApiRestrictions()));
        }
        require(found, "Negative field fixture did not find its target");
        result.put(owner, replaceMembers(cls, fields, cls.getMethods()));
        return result;
    }

    private static void real(File apk) throws Exception {
        Map<String, ClassDef> original = load(apk);
        Profile profile = BerryProfiles.select(BerryProfiles.PACKAGE, original::get);
        Path temporary = Files.createTempDirectory("berry-compat-check-fixtures-");
        try {
            checkFixture("original " + profile.id, apk, temporary, original, profile, PROVIDERS, true);
            Map<String, ClassDef> missing = new LinkedHashMap<>(original);
            missing.remove(profile.type("Lcx;"));
            checkFixture("missing WebView", apk, temporary, missing, profile,
                new String[] { ZOOM_PROVIDER }, false);
            String touch = profile.ref(BerryProfiles.TOUCH);
            checkFixture("missing original method", apk, temporary, changedMethod(original, touch, true),
                profile, new String[] { ZOOM_PROVIDER }, false);
            checkFixture("changed original method", apk, temporary, changedMethod(original, touch, false),
                profile, new String[] { ZOOM_PROVIDER }, false);
            String adapter = profile.ref("Lb03;->M0:Lz32;");
            checkFixture("changed adapter field type", apk, temporary, changedField(original, adapter, false),
                profile, new String[] { SHARE_PROVIDER }, false);
            checkFixture("private adapter field", apk, temporary, changedField(original, adapter, true),
                profile, new String[] { SHARE_PROVIDER }, false);
            Map<String, ClassDef> already = new LinkedHashMap<>(original);
            already.put("Lhiro/berryzoom/OneHandZoom;", empty("Lhiro/berryzoom/OneHandZoom;", "Ljava/lang/Object;"));
            checkFixture("already patched", apk, temporary, already, profile,
                new String[] { ZOOM_PROVIDER }, false);
            realInputs++;
            System.out.println("CHECK PASS " + profile.id + ":3 providers;6 negative in-memory fixtures;no edits");
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void main(String[] args) throws Exception {
        synthetic();
        for (String path : args) real(new File(path));
        System.out.println("PASS BerryCompatibilityTest: " + checks + " assertions; " + realInputs
            + " real DEX inputs; synthetic metadata/negative fixtures; no Android execution");
    }
}
