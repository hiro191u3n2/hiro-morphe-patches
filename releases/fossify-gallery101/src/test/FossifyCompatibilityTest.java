import java.io.*;
import java.util.*;
import app.hiro.fossifygallery.patches.FossifyGalleryExitPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;

/** Run the same compiled harness with each supported patcher/smali dependency. */
public final class FossifyCompatibilityTest {
    private static int assertions;
    private static void check(boolean value, String reason) {
        assertions++;
        if (!value) throw new AssertionError(reason);
    }
    private static void rejects(Runnable action, String reason) {
        boolean failed = false;
        try { action.run(); } catch (IllegalStateException expected) { failed = true; }
        check(failed, reason);
    }
    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String entry : dex.getDexEntryNames())
            for (ClassDef clazz : dex.getEntry(entry).getDexFile().getClasses())
                check(result.put(clazz.getType(), clazz) == null, "unique input classes");
        return result;
    }
    private static Method callback(ClassDef clazz) {
        for (Method method : clazz.getMethods())
            if (FossifyGalleryExitPatch.isCallback(method)) return method;
        throw new AssertionError("Missing official callback");
    }
    private static Method code(Method original, MethodImplementation body) {
        return new ImmutableMethod(original.getDefiningClass(), original.getName(), original.getParameters(),
                original.getReturnType(), original.getAccessFlags(), original.getAnnotations(),
                original.getHiddenApiRestrictions(), body);
    }
    private static ClassDef replace(ClassDef original, Method method) {
        List<Method> methods = new ArrayList<>();
        for (Method existing : original.getMethods())
            methods.add(FossifyGalleryExitPatch.isCallback(existing) ? method : existing);
        return new ImmutableClassDef(original.getType(), original.getAccessFlags(), original.getSuperclass(),
                original.getInterfaces(), original.getSourceFile(), original.getAnnotations(), original.getFields(), methods);
    }
    private static void changed(ClassDef owner, Method method, String reason) {
        rejects(() -> FossifyGalleryExitPatch.validateClass(replace(owner, method)), reason);
    }
    private static int find(MutableMethodImplementation body, Opcode opcode) {
        for (int i = 0; i < body.getInstructions().size(); i++)
            if (body.getInstructions().get(i).getOpcode() == opcode) return i;
        throw new AssertionError("Fixture opcode missing " + opcode);
    }
    private static Method invokeChanged(Method original, boolean changeOpcode) {
        MutableMethodImplementation body = new MutableMethodImplementation(original.getImplementation());
        int at = find(body, Opcode.INVOKE_DIRECT);
        FiveRegisterInstruction invoke = (FiveRegisterInstruction) body.getInstructions().get(at);
        MethodReference reference = (MethodReference) ((ReferenceInstruction) invoke).getReference();
        body.replaceInstruction(at, new BuilderInstruction35c(
                changeOpcode ? Opcode.INVOKE_VIRTUAL : Opcode.INVOKE_DIRECT,
                invoke.getRegisterCount(), changeOpcode ? invoke.getRegisterC() : 0,
                invoke.getRegisterD(), invoke.getRegisterE(), invoke.getRegisterF(), invoke.getRegisterG(), reference));
        return code(original, body);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("OFFICIALAPK REFERENCEDEX");
        Map<String, ClassDef> app = read(args[0]);
        Map<String, Method> expected = FossifyGalleryExitPatch.readReference(new FileInputStream(args[1]));
        check(expected.keySet().equals(FossifyGalleryExitPatch.OWNERS), "reference scope exact");
        FossifyGalleryExitPatch.validatePackage("org.fossify.gallery", "1.14.0", "29");
        for (String owner : FossifyGalleryExitPatch.OWNERS) {
            ClassDef clazz = app.get(owner);
            Method source = callback(clazz);
            check(FossifyGalleryExitPatch.matchesReference(source, expected.get(owner)),
                    "same active writer compares raw official method with frozen reference: " + owner);
            check(FossifyGalleryExitPatch.validateClass(clazz) != null, "immutable original accepted");
            MutableClass normalized = new MutableClass(clazz);
            check(FossifyGalleryExitPatch.matchesReference(callback(normalized), expected.get(owner)),
                    "debug positions normalized by mutable adapter accepted");
            check(FossifyGalleryExitPatch.validateClass(normalized) != null, "materialized original class accepted");
            MethodImplementation sourceBody = source.getImplementation();
            Method noDebug = code(source, new ImmutableMethodImplementation(sourceBody.getRegisterCount(),
                    sourceBody.getInstructions(), sourceBody.getTryBlocks(), Collections.emptyList()));
            check(FossifyGalleryExitPatch.matchesReference(noDebug, expected.get(owner)), "debug only difference allowed");
        }

        ClassDef main = app.get(FossifyGalleryExitPatch.MAIN);
        Method original = callback(main);
        changed(main, invokeChanged(original, true), "changed invoke opcode rejected");
        changed(main, invokeChanged(original, false), "changed Activity receiver rejected");

        MutableMethodImplementation literal = new MutableMethodImplementation(original.getImplementation());
        int literalAt = find(literal, Opcode.CONST_4);
        OneRegisterInstruction literalRegister = (OneRegisterInstruction) literal.getInstructions().get(literalAt);
        literal.replaceInstruction(literalAt, new BuilderInstruction11n(Opcode.CONST_4, literalRegister.getRegisterA(), 0));
        changed(main, code(original, literal), "changed native handled literal rejected");

        MutableMethodImplementation branch = new MutableMethodImplementation(original.getImplementation());
        int branchAt = find(branch, Opcode.IF_EQZ);
        BuilderInstruction21t branchInstruction = (BuilderInstruction21t) branch.getInstructions().get(branchAt);
        branch.replaceInstruction(branchAt, new BuilderInstruction21t(Opcode.IF_NEZ,
                branchInstruction.getRegisterA(), branchInstruction.getTarget()));
        changed(main, code(original, branch), "changed branch decision rejected");

        MutableMethodImplementation target = new MutableMethodImplementation(original.getImplementation());
        int targetAt = find(target, Opcode.IF_EQZ);
        BuilderInstruction21t targetInstruction = (BuilderInstruction21t) target.getInstructions().get(targetAt);
        target.replaceInstruction(targetAt, new BuilderInstruction21t(Opcode.IF_EQZ,
                targetInstruction.getRegisterA(), target.newLabelForIndex(target.getInstructions().size() - 1)));
        changed(main, code(original, target), "changed branch destination rejected");

        MethodImplementation body = original.getImplementation();
        List<ImmutableTryBlock> handlers = Collections.singletonList(new ImmutableTryBlock(0, 3,
                Collections.singletonList(new ImmutableExceptionHandler("Ljava/lang/Throwable;", 16))));
        changed(main, code(original, new ImmutableMethodImplementation(body.getRegisterCount(), body.getInstructions(),
                handlers, body.getDebugItems())), "changed exception handler rejected");
        changed(main, code(original, new ImmutableMethodImplementation(body.getRegisterCount() + 1,
                body.getInstructions(), body.getTryBlocks(), body.getDebugItems())), "changed register count rejected");
        Method flags = new ImmutableMethod(original.getDefiningClass(), original.getName(), original.getParameters(),
                "Z", 17, original.getAnnotations(), original.getHiddenApiRestrictions(), body);
        changed(main, flags, "changed method access flags rejected");
        Set<Annotation> annotations = new LinkedHashSet<Annotation>(original.getAnnotations());
        annotations.add(new ImmutableAnnotation(1, "Lqa/Tampered;", Collections.emptyList()));
        Method annotation = new ImmutableMethod(original.getDefiningClass(), original.getName(), original.getParameters(),
                "Z", 1, annotations, original.getHiddenApiRestrictions(), body);
        changed(main, annotation, "changed method metadata rejected");
        Method renamed = new ImmutableMethod(original.getDefiningClass(), "unverifiedBack", original.getParameters(),
                "Z", 1, original.getAnnotations(), original.getHiddenApiRestrictions(), body);
        changed(main, renamed, "missing callback method rejected");
        rejects(() -> FossifyGalleryExitPatch.validateClass(null), "missing class rejected");
        rejects(() -> FossifyGalleryExitPatch.validatePackage("foreign.app", "1.14.0", "29"), "foreign package rejected");
        rejects(() -> FossifyGalleryExitPatch.validatePackage("org.fossify.gallery", "1.13.1", "29"), "foreign version rejected");
        rejects(() -> FossifyGalleryExitPatch.validatePackage("org.fossify.gallery", "1.14.0", "30"), "foreign versionCode rejected");
        rejects(() -> FossifyGalleryExitPatch.readReference(new ByteArrayInputStream(new byte[]{1, 2, 3})),
                "malformed reference DEX rejected");
        rejects(() -> FossifyGalleryExitPatch.readReference(null), "missing reference rejected");
        MutableClass changedAfterSnapshot = new MutableClass(main);
        MutableMethod old = null;
        for (MutableMethod method : changedAfterSnapshot.getMethods())
            if (FossifyGalleryExitPatch.isCallback(method)) old = method;
        check(old != null, "mutable snapshot fixture callback exists");
        changedAfterSnapshot.getMethods().remove(old);
        changedAfterSnapshot.getMethods().add(new MutableMethod(invokeChanged(original, false)));
        rejects(() -> FossifyGalleryExitPatch.apply(changedAfterSnapshot, ImmutableMethod.of(original)),
                "executable mutation after immutable snapshot rejected");
        System.out.println("PASS Fossify compatibility assertions=" + assertions
                + "; original/debug-normalized methods accepted with active writer; opcode/literal/branch/receiver/"
                + "handler/register/metadata/method/package/version/reference tampering rejected.");
    }
}
