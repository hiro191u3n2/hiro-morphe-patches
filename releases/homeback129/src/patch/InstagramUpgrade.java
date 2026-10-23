package app.hiro.oneback.patches;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableField;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.value.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.util.FieldUtil;
import com.android.tools.smali.dexlib2.util.MethodUtil;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/**
 * Explicit runtime replacement for exactly identified Instagram and X OneBack inputs.
 * Morphe extension merge does not replace existing method bodies. A normal reapply is
 * therefore neither an update nor safe: it collides with existing hiroOneBack$ helpers.
 * This path preserves every native method and accepts only byte-for-byte canonical
 * runtime classes from a pinned distributed runtime after normalizing only jumbo string
 * load widths. Unknown patched inputs still fail.
 */
public final class InstagramUpgrade {
    private InstagramUpgrade() {}
    private static final String PREFIX = "Lapp/hiro/oneback/runtime/";
    private static final String MAIN = "Lcom/instagram/mainactivity/InstagramMainActivity;";
    private static final String[][] REFERENCES = {
        {"/upgrades/oneback-runtime120.dex", "d614e7ba8501af6ee65b3b237cf75034d3035a46c3556ef23365363b25955fc2", "1.1.10"},
        {"/upgrades/oneback-runtime121.dex", "52e1817fdaa16591cb5eb7b0383aaa07f578a4a6799dad1cf5ef6e99d9a817d5", "1.1.11"},
        {"/upgrades/oneback-runtime122.dex", "e23a94da3f45accb98d52f3ed3ebbbdf7e4238b7fb6a30cb950199794c1c012b", "1.1.12"},
        {"/upgrades/oneback-runtime123.dex", "cea6c6519bd9c6c70776cddd3e6a26f28709a77d0fed7a8335a7baad7be28a0e", "1.1.13"},
        {"/upgrades/oneback-runtime124.dex", "559d89a6e7c3008cfb14b1e2c5e46285cf3a48365725d87524195a466c02b07f", "1.1.14"},
        {"/upgrades/oneback-runtime125.dex", "f6ba029c8d68954d42e76a8fbda8ae8f9d7ce1b22be29e37719636b7254d8b5f", "1.1.15"},
        {"/upgrades/oneback-runtime127.dex", "818800fec2272345ac6ff60d5fc86ef769b83746aa89544638f7a6ad44a0231c", "1.1.17"}
    };

    /** Called after extension merge, before applyAll. True means skip old hook insertion. */
    public static boolean applyIfKnown(String packageName, Map<String,ClassDef> classes, OneBackPatch.Editor editor) {
        return applyVersionIfKnown(packageName,classes,editor)!=null;
    }

    /** Instagram accepts only exact published 120-125/127 runtimes with their
     * corresponding verified native boundaries. X retains its 123/124 admission. */
    public static String applyVersionIfKnown(String packageName, Map<String,ClassDef> classes, OneBackPatch.Editor editor) {
        boolean instagram="com.instagram.android".equals(packageName),twitter="com.twitter.android".equals(packageName);
        if ((!instagram&&!twitter) || !hasPriorHooks(classes)) return null;
        require(classes.containsKey(twitter?"Lcom/twitter/app/main/MainActivity;":MAIN), "Already-patched application has no supported MainActivity");
        try {
            Map<String,ClassDef> replacement = resource("/extensions/oneback_exit.mpe", null);
            Map<String,ClassDef> previous = null;
            String version = null;
            for (String[] row : REFERENCES) {
                boolean priorTwitter="1.1.13".equals(row[2])||"1.1.14".equals(row[2]);
                if(twitter&&!priorTwitter)continue;
                Map<String,ClassDef> candidate = resource(row[0], row[1]);
                if (matches(classes, candidate, replacement)) {
                    previous = candidate; version = row[2]; break;
                }
            }
            require(previous != null, "OneBack already applied with an unknown or modified runtime; use a clean matching APK");
            require(replacement.keySet().containsAll(previous.keySet()), "Runtime update removes an existing runtime class");
            // Runtime identity alone is insufficient when the replacement
            // trusts a native bridge. Admit its whole body before any mutation.
            if(twitter) {
                if("1.1.14".equals(version))OneBackPatch.verifyTwitterPatchedRootBack(classes);
                else OneBackPatch.verifyTwitterRootBack(classes);
                OneBackPatch.verifyTwitterBackScope(classes);
            }
            else if("1.1.17".equals(version)&&Instagram452Patch.supports(classes)) {
                Instagram452Upgrade.verify(classes);
            } else {
                OneBackPatch.verifyInstagramReelsBoundary(classes);
                boolean tabAlreadyPatched="1.1.13".equals(version)||"1.1.14".equals(version)||"1.1.15".equals(version)||"1.1.17".equals(version);
                if(tabAlreadyPatched)OneBackPatch.verifyInstagramPatchedTabBack(classes);
                else OneBackPatch.verifyInstagramTabBack(classes);
                if("1.1.12".equals(version)||tabAlreadyPatched)OneBackPatch.verifyInstagramDispatcher(classes);
            }
            validateExternalReferences(classes, previous, replacement);
            // Finish all validation before asking the editor for a mutable class.
            for (String type : replacement.keySet())
                require(classes.containsKey(type), "Morphe did not merge new runtime class " + type);
            int replaced = 0;
            for (ClassDef value : replacement.values()) {
                MutableClass target = editor.mutable(value.getType());
                replace(target, value);
                require(Arrays.equals(canonical(target), canonical(value)), "Runtime replacement did not survive mutable caches: " + value.getType());
                replaced++;
            }
            System.out.println("HomeBack128 exact "+(twitter?"X":"Instagram")+" runtime upgrade: " + version + " -> 1.1.18; " + replaced + " classes; existing native hooks preserved");
            return version;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not verify and replace the known application runtime", failure);
        }
    }

    private static boolean hasPriorHooks(Map<String,ClassDef> classes) {
        for (ClassDef c : classes.values()) {
            if (c.getType().startsWith(PREFIX)) continue;
            for (Method m : c.getMethods()) if (m.getName().startsWith(OneBackPatch.HELPER_PREFIX)) return true;
        }
        return false;
    }

    /** All old classes must match, and every extra runtime class must be the new extension's exact class. */
    private static boolean matches(Map<String,ClassDef> actual, Map<String,ClassDef> previous,
                                   Map<String,ClassDef> replacement) throws IOException {
        for (ClassDef old : previous.values()) {
            ClassDef present = actual.get(old.getType());
            if (present == null || !Arrays.equals(canonical(old), canonical(present))) return false;
        }
        for (ClassDef present : actual.values()) {
            if (!present.getType().startsWith(PREFIX) || previous.containsKey(present.getType())) continue;
            ClassDef added = replacement.get(present.getType());
            if (added == null || !Arrays.equals(canonical(added), canonical(present))) return false;
        }
        return true;
    }

    private static Map<String,ClassDef> resource(String path, String sha) throws IOException {
        byte[] bytes;
        try (InputStream in = InstagramUpgrade.class.getResourceAsStream(path)) {
            require(in != null, "Missing exact-runtime upgrade resource " + path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] block = new byte[8192]; int length;
            while ((length = in.read(block)) != -1) out.write(block, 0, length);
            bytes = out.toByteArray();
        }
        if (sha != null) require(sha.equals(digest(bytes)), "Pinned runtime upgrade resource differs: " + path);
        Map<String,ClassDef> result = new TreeMap<String,ClassDef>();
        DexFile dex = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), new BufferedInputStream(new ByteArrayInputStream(bytes)));
        for (ClassDef c : dex.getClasses()) {
            require(c.getType().startsWith(PREFIX), "Non-runtime class in upgrade resource " + path);
            require(result.put(c.getType(), c) == null, "Duplicate runtime type in " + path);
        }
        require(result.containsKey(OneBackPatch.RUNTIME), "Incomplete runtime resource " + path);
        return result;
    }

    private static String digest(byte[] bytes) {
        try {
            StringBuilder hex = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }

    private static byte[] canonical(ClassDef value) throws IOException {
        MemoryDataStore out = new MemoryDataStore();
        try {
            DexPool.writeTo(out, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(normalizeStringWidths(value))));
            return Arrays.copyOf(out.getBuffer(), out.getSize());
        } finally { out.close(); }
    }

    /** APK-wide string indexes can widen unchanged loads. Preserve their values,
     * registers and all metadata; labels relocate branches, handlers and debug items. */
    private static ClassDef normalizeStringWidths(ClassDef value) {
        List<Method> methods = new ArrayList<Method>();
        boolean changed = false;
        for (Method method : value.getMethods()) {
            MethodImplementation implementation = method.getImplementation();
            boolean jumbo = false;
            if (implementation != null) for (Instruction instruction : implementation.getInstructions()) {
                if (instruction.getOpcode() == Opcode.CONST_STRING_JUMBO) { jumbo = true; break; }
            }
            if (!jumbo) { methods.add(method); continue; }
            MutableMethodImplementation code = new MutableMethodImplementation(implementation);
            for (int i = 0; i < code.getInstructions().size(); i++) {
                Instruction instruction = code.getInstructions().get(i);
                if (instruction.getOpcode() == Opcode.CONST_STRING_JUMBO) {
                    code.replaceInstruction(i, new BuilderInstruction21c(Opcode.CONST_STRING,
                            ((OneRegisterInstruction)instruction).getRegisterA(),
                            ((ReferenceInstruction)instruction).getReference()));
                }
            }
            methods.add(new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                    method.getReturnType(), method.getAccessFlags(), method.getAnnotations(),
                    method.getHiddenApiRestrictions(), code));
            changed = true;
        }
        if (!changed) return value;
        return new ImmutableClassDef(value.getType(), value.getAccessFlags(), value.getSuperclass(),
                value.getInterfaces(), value.getSourceFile(), value.getAnnotations(), value.getFields(), methods);
    }

    /** MutableClass caches six independent member sets; refresh all, even if already materialized. */
    private static void replace(MutableClass target, ClassDef value) {
        MutableClass copy = new MutableClass(value);
        target.setAccessFlags(value.getAccessFlags()); target.setSuperClass(value.getSuperclass()); target.setSourceFile(value.getSourceFile());
        target.getInterfaces().clear(); target.getInterfaces().addAll(copy.getInterfaces());
        target.getAnnotations().clear(); target.getAnnotations().addAll(copy.getAnnotations());
        Set<MutableMethod> allMethods = target.getMethods(), direct = target.getDirectMethods(), virtual = target.getVirtualMethods();
        allMethods.clear(); direct.clear(); virtual.clear();
        for (MutableMethod method : copy.getMethods()) {
            allMethods.add(method); (MethodUtil.isDirect(method) ? direct : virtual).add(method);
        }
        Set<MutableField> allFields = target.getFields(), statics = target.getStaticFields(), instances = target.getInstanceFields();
        allFields.clear(); statics.clear(); instances.clear();
        for (MutableField field : copy.getFields()) {
            allFields.add(field); (FieldUtil.isStatic(field) ? statics : instances).add(field);
        }
    }

    private static void validateExternalReferences(Map<String,ClassDef> classes, Map<String,ClassDef> previous, Map<String,ClassDef> replacement) {
        for (ClassDef c : classes.values()) {
            if (c.getType().startsWith(PREFIX)) continue;
            type(c.getSuperclass(), replacement);
            for (String iface : c.getInterfaces()) type(iface, replacement);
            for (Field f : c.getFields()) { type(f.getType(), replacement); encoded(f.getInitialValue(), previous, replacement); }
            for (Method method : c.getMethods()) {
                type(method.getReturnType(), replacement);
                for (CharSequence p : method.getParameterTypes()) type(p.toString(), replacement);
                if (method.getImplementation() == null) continue;
                for (Instruction instruction : method.getImplementation().getInstructions()) {
                    if (instruction instanceof ReferenceInstruction) reference(((ReferenceInstruction)instruction).getReference(), previous, replacement);
                    if (instruction instanceof DualReferenceInstruction) reference(((DualReferenceInstruction)instruction).getReference2(), previous, replacement);
                }
            }
        }
    }

    private static void type(String name, Map<String,ClassDef> replacement) {
        if (name == null) return;
        while (name.startsWith("[")) name = name.substring(1);
        if (name.startsWith(PREFIX)) require(replacement.containsKey(name), "Referenced runtime type was removed: " + name);
    }

    private static void reference(Reference ref, Map<String,ClassDef> previous, Map<String,ClassDef> replacement) {
        if (ref instanceof MethodReference) {
            MethodReference r = (MethodReference)ref;
            if (!r.getDefiningClass().startsWith(PREFIX)) return;
            ClassDef target = replacement.get(r.getDefiningClass());
            require(target != null && (target.getAccessFlags() & 1) != 0, "Runtime target class inaccessible: " + r);
            Method found = method(target, r), old = method(previous.get(r.getDefiningClass()), r);
            require(found != null && (found.getAccessFlags() & 1) != 0, "Existing native hook has no public replacement: " + r);
            require(old != null && ((old.getAccessFlags() ^ found.getAccessFlags()) & 8) == 0, "Existing native hook static ABI changed: " + r);
        } else if (ref instanceof FieldReference) {
            FieldReference r = (FieldReference)ref;
            if (!r.getDefiningClass().startsWith(PREFIX)) return;
            ClassDef target = replacement.get(r.getDefiningClass());
            Field found = field(target, r), old = field(previous.get(r.getDefiningClass()), r);
            require(target != null && (target.getAccessFlags() & 1) != 0 && found != null && (found.getAccessFlags() & 1) != 0,
                    "Existing native runtime field has no public replacement: " + r);
            require(old != null && ((old.getAccessFlags() ^ found.getAccessFlags()) & (8 | 16)) == 0, "Runtime field ABI changed: " + r);
        } else if (ref instanceof TypeReference) type(((TypeReference)ref).getType(), replacement);
        else if (ref instanceof MethodHandleReference) reference(((MethodHandleReference)ref).getMemberReference(), previous, replacement);
        else if (ref instanceof MethodProtoReference) {
            MethodProtoReference r = (MethodProtoReference)ref; type(r.getReturnType(), replacement);
            for (CharSequence p : r.getParameterTypes()) type(p.toString(), replacement);
        } else if (ref instanceof CallSiteReference) {
            CallSiteReference r = (CallSiteReference)ref; reference(r.getMethodHandle(), previous, replacement); reference(r.getMethodProto(), previous, replacement);
            for (EncodedValue v : r.getExtraArguments()) encoded(v, previous, replacement);
        }
    }

    private static void encoded(EncodedValue value, Map<String,ClassDef> previous, Map<String,ClassDef> replacement) {
        if (value instanceof MethodEncodedValue) reference(((MethodEncodedValue)value).getValue(), previous, replacement);
        else if (value instanceof FieldEncodedValue) reference(((FieldEncodedValue)value).getValue(), previous, replacement);
        else if (value instanceof EnumEncodedValue) reference(((EnumEncodedValue)value).getValue(), previous, replacement);
        else if (value instanceof TypeEncodedValue) type(((TypeEncodedValue)value).getValue(), replacement);
        else if (value instanceof MethodHandleEncodedValue) reference(((MethodHandleEncodedValue)value).getValue(), previous, replacement);
        else if (value instanceof MethodTypeEncodedValue) reference(((MethodTypeEncodedValue)value).getValue(), previous, replacement);
        else if (value instanceof ArrayEncodedValue) for (EncodedValue child : ((ArrayEncodedValue)value).getValue()) encoded(child, previous, replacement);
        else if (value instanceof AnnotationEncodedValue) {
            AnnotationEncodedValue annotation = (AnnotationEncodedValue)value; type(annotation.getType(), replacement);
            for (AnnotationElement element : annotation.getElements()) encoded(element.getValue(), previous, replacement);
        }
    }

    private static Method method(ClassDef c, MethodReference ref) {
        if (c != null) for (Method m : c.getMethods()) if (MethodUtil.methodSignaturesMatch(m, ref)) return m;
        return null;
    }
    private static Field field(ClassDef c, FieldReference ref) {
        if (c != null) for (Field f : c.getFields()) if (f.getName().equals(ref.getName()) && f.getType().equals(ref.getType())) return f;
        return null;
    }
    private static void require(boolean ok, String reason) { if (!ok) throw new IllegalStateException(reason); }
}
