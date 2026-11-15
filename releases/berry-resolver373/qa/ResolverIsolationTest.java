import app.hiro.berry.patches.BerryProfiles;
import app.hiro.berry.patches.GenericBerryResolver;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.rewriter.*;
import java.io.File;
import java.util.*;

/** In-memory structural mutations of a real unmodified APK. These are host
 * resolver tests, not installable APK fixtures or Android runtime claims. */
public final class ResolverIsolationTest {
    private static final String PACKAGE = "jp.ejimax.berrybrowser";
    private static final String SCROLL = "Lcx;->onScrollChanged(IIII)V";
    private static final String[] LAZY = {
        "Lc41;->S(Ljava/lang/String;)V", "Lc41;->V()V", "Lc41;->t0()V",
        "Lc41;->m()V", "Lc41;->destroy()V", "Lc41;->r(Landroid/os/Bundle;)V",
        "Lc41;->h(Landroid/os/Bundle;)Z", SCROLL
    };
    private static final String[] ZOOM = {
        BerryProfiles.TOUCH, BerryProfiles.INTERCEPT,
        "Lc41;->q:Lcx;", "Lz31;->h0:Lc41;"
    };
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static int passed;

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("APK [legacy]");
        boolean legacy = args.length == 2 && args[1].equals("legacy");
        Map<String,ClassDef> original = read(args[0]);
        BerryProfiles.Profile reference = resolve(original);
        verifyZoom(reference, reference);
        for (String logical : LAZY) {
            reference.ref(logical);
            reference.expectedHash(logical);
        }
        pass("real APK resolves all existing hooks");

        for (String logical : LAZY) {
            Map<String,ClassDef> missing = removeMethod(original, reference.ref(logical));
            if (legacy) {
                rejects(() -> resolve(missing), "legacy core rejects missing unrelated " + logical);
            } else {
                BerryProfiles.Profile isolated = resolve(missing);
                verifyZoom(reference, isolated);
                // Type and field lookups must not trigger unrelated lifecycle checks.
                if (!reference.type("Lc41;").equals(isolated.type("Lc41;"))) throw new AssertionError("controller type differs");
                for (String other : LAZY) if (!other.equals(logical)) {
                    if (!reference.ref(other).equals(isolated.ref(other))) throw new AssertionError("unrelated hook differs");
                    if (!reference.expectedHash(other).equals(isolated.expectedHash(other))) throw new AssertionError("unrelated hash differs");
                }
                rejects(() -> isolated.ref(logical), "requested missing hook remains guarded " + logical);
                // A failed lazy resolution must not be marked ready.
                rejects(() -> isolated.expectedHash(logical), "failed hook remains rejected on retry " + logical);
                verifyZoom(reference, isolated);
                pass("zoom independent of " + logical);
            }
        }

        Map<String,ClassDef> allMissing = original;
        for (String logical : LAZY) allMissing = removeMethod(allMissing, reference.ref(logical));
        final Map<String,ClassDef> noForeground = allMissing;
        if (legacy) rejects(() -> resolve(noForeground), "legacy rejects all unrelated foreground methods missing");
        else {
            verifyZoom(reference, resolve(noForeground));
            pass("zoom works without all unused foreground methods");
        }
        rejects(() -> GenericBerryResolver.resolve("wrong.package", original.values()), "wrong package");
        rejects(() -> resolve(removeMethod(original, reference.ref(BerryProfiles.TOUCH))), "missing touch implementation");
        rejects(() -> resolve(removeMethod(original, reference.ref(BerryProfiles.INTERCEPT))), "missing intercept implementation");
        rejects(() -> resolve(privateField(original, reference.ref("Lc41;->q:Lcx;"))), "nonpublic owned WebView field");
        rejects(() -> resolve(privateField(original, reference.ref("Lz31;->h0:Lc41;"))), "nonpublic parent controller field");
        rejects(() -> resolve(duplicateParent(original, reference.type("Lz31;"))), "ambiguous touch ownership graph");
        System.out.println("PASS total=" + passed + " mode=" + (legacy ? "legacy" : "isolated"));
    }

    private static Map<String,ClassDef> read(String path) throws Exception {
        Map<String,ClassDef> classes = new LinkedHashMap<>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.forApi(37));
        for (String name : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(name).getDexFile().getClasses()) {
            if (classes.put(c.getType(), ImmutableClassDef.of(c)) != null) throw new AssertionError("duplicate input type");
        }
        return classes;
    }
    private static BerryProfiles.Profile resolve(Map<String,ClassDef> classes) {
        return GenericBerryResolver.resolve(PACKAGE, classes.values());
    }
    private static void verifyZoom(BerryProfiles.Profile reference, BerryProfiles.Profile candidate) {
        for (String logical : ZOOM) if (!reference.ref(logical).equals(candidate.ref(logical)))
            throw new AssertionError("zoom mapping changed: " + logical);
        for (String logical : new String[]{BerryProfiles.TOUCH, BerryProfiles.INTERCEPT})
            if (!reference.expectedHash(logical).equals(candidate.expectedHash(logical)))
                throw new AssertionError("zoom method changed: " + logical);
    }
    private static Map<String,ClassDef> removeMethod(Map<String,ClassDef> input, String descriptor) {
        String ownerName = descriptor.substring(0, descriptor.indexOf("->"));
        ClassDef owner = input.get(ownerName);
        List<Method> methods = new ArrayList<>();
        int removed = 0;
        for (Method method : owner.getMethods()) {
            if (F.getMethodDescriptor(method).equals(descriptor)) removed++;
            else methods.add(method);
        }
        if (removed != 1) throw new AssertionError("fixture did not remove exactly one method: " + descriptor);
        Map<String,ClassDef> result = new LinkedHashMap<>(input);
        result.put(ownerName, copy(owner, owner.getFields(), methods));
        return result;
    }
    private static Map<String,ClassDef> privateField(Map<String,ClassDef> input, String descriptor) {
        String ownerName = descriptor.substring(0, descriptor.indexOf("->"));
        ClassDef owner = input.get(ownerName);
        List<Field> fields = new ArrayList<>();
        int changed = 0;
        for (Field field : owner.getFields()) {
            if (!F.getFieldDescriptor(field).equals(descriptor)) fields.add(field);
            else {
                changed++;
                fields.add(new ImmutableField(field.getDefiningClass(), field.getName(), field.getType(),
                    (field.getAccessFlags() & ~1) | 2, field.getInitialValue(), field.getAnnotations(), field.getHiddenApiRestrictions()));
            }
        }
        if (changed != 1) throw new AssertionError("fixture did not change exactly one field");
        Map<String,ClassDef> result = new LinkedHashMap<>(input);
        result.put(ownerName, copy(owner, fields, owner.getMethods()));
        return result;
    }
    private static ClassDef copy(ClassDef owner, Iterable<? extends Field> fields, Iterable<? extends Method> methods) {
        return new ImmutableClassDef(owner.getType(), owner.getAccessFlags(), owner.getSuperclass(),
            owner.getInterfaces(), owner.getSourceFile(), owner.getAnnotations(), fields, methods);
    }
    private static Map<String,ClassDef> duplicateParent(Map<String,ClassDef> input, String parent) {
        String duplicate = "Ltest/berry/SecondTouchParent;";
        if (input.containsKey(duplicate)) throw new AssertionError("fixture type collides");
        DexRewriter rewriter = new DexRewriter(new RewriterModule() {
            @Override public Rewriter<String> getTypeRewriter(Rewriters r) {
                return type -> type.equals(parent) ? duplicate : type;
            }
        });
        Map<String,ClassDef> result = new LinkedHashMap<>(input);
        result.put(duplicate, ImmutableClassDef.of(rewriter.getClassDefRewriter().rewrite(input.get(parent))));
        return result;
    }
    private static void rejects(Runnable action, String label) {
        try { action.run(); }
        catch (IllegalStateException expected) { pass("rejected " + label); return; }
        throw new AssertionError("unexpectedly accepted " + label);
    }
    private static void pass(String label) { passed++; System.out.println("PASS " + label); }
}
