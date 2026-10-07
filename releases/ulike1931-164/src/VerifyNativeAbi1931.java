import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.iface.reference.TypeReference;

/** Independent stock-APK ABI audit for the emitted front-preview helper and its
 * native/runtime hook callers. This reads DEX declarations and instructions;
 * it does not load Android classes, emulate Android/ART or execute camera code.
 * Platform/JDK references are reported separately, not treated as checked native
 * declarations. Reflection contracts are stated here from the observed APK,
 * rather than taken from the compile stubs or Transform1931's expected results.
 *
 * Usage: VerifyNativeAbi1931 STOCK_APK EMITTED_DIR
 */
public final class VerifyNativeAbi1931 {
    static final String FRONT = "Lcom/hiro/ulike/FrontPreview1931;";
    static final String CAPTURE = "Lcom/ss/android/vesdk/VECameraCapture;";
    static final String SETTINGS = "Lcom/ss/android/ttvecamera/TECameraSettings;";
    static final String VE_SETTINGS = "Lcom/ss/android/vesdk/VECameraSettings;";
    static final String FACING = "Lcom/ss/android/vesdk/VECameraSettings$CAMERA_FACING_ID;";
    static final String PIPELINE = "Lcom/ss/android/vesdk/frame/TECapturePipeline;";
    static final String TEXTURE_PIPELINE = "Lcom/ss/android/vesdk/frame/TETextureCapturePipeline;";
    static final String RECORDER_PIPELINE = "Lcom/ss/android/vesdk/frame/TERecorderCapturePipeline;";
    static final String FORMAT = "Li/s/a/w/m$d;";
    static final String SERVER = "Li/s/a/w/q;";
    static final String MODE = "Li/s/a/w/h0/b;";
    static final String CAMERA1 = "Li/s/a/w/d;";
    static final String CAMERA2 = "Li/s/a/w/g;";
    static final String CAMERA_BASE = "Li/s/a/w/a;";
    static final String PROVIDER = "Li/s/a/w/l0/b;";
    static final String MANAGER = "Li/s/a/w/l0/c;";
    static final String OPTICAL = "Lcom/hiro/ulike/OpticalZoom;";
    static final String PROVIDER_HELPER = "Lcom/hiro/ulike/ProviderLifecycle1929;";
    static final String MANUAL = "Lcom/hiro/ulike/ManualLens170;";
    static final int PUBLIC = AccessFlags.PUBLIC.getValue();
    static final int PRIVATE = AccessFlags.PRIVATE.getValue();
    static final int PROTECTED = AccessFlags.PROTECTED.getValue();
    static final int STATIC = AccessFlags.STATIC.getValue();
    static final int FINAL = AccessFlags.FINAL.getValue();
    static final int INTERFACE = AccessFlags.INTERFACE.getValue();
    static final int ABSTRACT = AccessFlags.ABSTRACT.getValue();
    static long assertions;
    static final Set<String> checkedMembers = new TreeSet<>();
    static final Set<String> externalMembers = new TreeSet<>();
    static final Set<String> checkedTypes = new TreeSet<>();
    static final Set<String> externalTypes = new TreeSet<>();
    static final Set<String> reflectionSymbols = new TreeSet<>();
    static final List<String> reflectionRows = new ArrayList<>();
    static Map<String, ClassDef> stock, classes;
    static Map<String, Method> effectiveMethods;
    static int memberInstructions, typeInstructions;

    static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new IllegalStateException(message);
    }
    static boolean flag(int flags, int bit) { return (flags & bit) != 0; }
    static String mid(MethodReference value) { return DexFormatter.INSTANCE.getMethodDescriptor(value); }
    static String fid(FieldReference value) { return DexFormatter.INSTANCE.getFieldDescriptor(value); }
    static String suffix(String member) { return member.substring(member.indexOf("->") + 2); }
    static boolean frontClass(String name) {
        return name.equals(FRONT) || name.startsWith("Lcom/hiro/ulike/FrontPreview1931$");
    }
    static Map<String, ClassDef> load(Path path) throws Exception {
        Map<String, ClassDef> out = new TreeMap<>();
        var container = DexFileFactory.loadDexContainer(path.toFile(), Opcodes.forApi(26));
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef type : container.getEntry(entry).getDexFile().getClasses()) {
                require(out.put(type.getType(), type) == null, "Duplicate DEX class: " + type.getType());
            }
        }
        require(!out.isEmpty(), "No DEX classes: " + path);
        return out;
    }
    static Map<String, Method> methods(Collection<ClassDef> types) {
        Map<String, Method> out = new TreeMap<>();
        for (ClassDef type : types) for (Method method : type.getMethods()) {
            require(out.put(mid(method), method) == null, "Duplicate DEX method: " + mid(method));
        }
        return out;
    }
    static String packageOf(String type) {
        int slash = type.lastIndexOf('/');
        return slash < 0 ? "" : type.substring(0, slash);
    }
    static boolean platform(String type) {
        return type.startsWith("Ljava/") || type.startsWith("Ljavax/") || type.startsWith("Landroid/")
            || type.startsWith("Ldalvik/") || type.startsWith("Lorg/json/")
            || type.startsWith("Lorg/xml/") || type.startsWith("Lorg/w3c/");
    }
    static boolean subtype(String child, String ancestor, Set<String> seen) {
        if (child == null || !seen.add(child)) return false;
        if (child.equals(ancestor)) return true;
        ClassDef type = classes.get(child);
        if (type == null) return false;
        if (subtype(type.getSuperclass(), ancestor, seen)) return true;
        for (String face : type.getInterfaces()) if (subtype(face, ancestor, seen)) return true;
        return false;
    }
    static boolean subtype(String child, String ancestor) { return subtype(child, ancestor, new HashSet<>()); }
    static boolean accessible(int flags, String declaring, String caller) {
        return flag(flags, PUBLIC) || declaring.equals(caller)
            || !flag(flags, PRIVATE) && (packageOf(declaring).equals(packageOf(caller))
                || flag(flags, PROTECTED) && subtype(caller, declaring));
    }
    static void classAccess(String type, String caller) {
        ClassDef definition = classes.get(type);
        require(definition != null, "Missing native/runtime class: " + type);
        require(flag(definition.getAccessFlags(), PUBLIC) || packageOf(type).equals(packageOf(caller)),
                "Inaccessible referenced class " + type + " from " + caller);
    }
    static Field resolveField(String type, String name, String expectedType, Set<String> seen) {
        if (type == null || !seen.add(type)) return null;
        ClassDef definition = classes.get(type);
        if (definition == null) return null;
        for (Field field : definition.getFields()) {
            if (field.getName().equals(name) && field.getType().equals(expectedType)) return field;
        }
        for (String face : definition.getInterfaces()) {
            Field result = resolveField(face, name, expectedType, seen);
            if (result != null) return result;
        }
        return resolveField(definition.getSuperclass(), name, expectedType, seen);
    }
    static Method resolveMethod(String type, String memberSuffix, Set<String> seen) {
        if (type == null || !seen.add(type)) return null;
        Method method = effectiveMethods.get(type + "->" + memberSuffix);
        if (method != null) return method;
        if (memberSuffix.startsWith("<init>(") || memberSuffix.startsWith("<clinit>(")) return null;
        ClassDef definition = classes.get(type);
        if (definition == null) return null;
        Method result = resolveMethod(definition.getSuperclass(), memberSuffix, seen);
        if (result != null) return result;
        for (String face : definition.getInterfaces()) {
            result = resolveMethod(face, memberSuffix, seen);
            if (result != null) return result;
        }
        return null;
    }
    static int argumentWords(MethodReference method, boolean isStatic) {
        int count = isStatic ? 0 : 1;
        for (CharSequence parameter : method.getParameterTypes()) {
            count += parameter.toString().equals("J") || parameter.toString().equals("D") ? 2 : 1;
        }
        return count;
    }
    static void auditType(String descriptor, String caller, String opcode) {
        String type = descriptor;
        while (type.startsWith("[")) type = type.substring(1);
        if (!type.startsWith("L")) return;
        typeInstructions++;
        if (!classes.containsKey(type)) {
            require(platform(type), "Missing non-platform type: " + descriptor + " in " + caller);
            externalTypes.add(type); return;
        }
        classAccess(type, caller);
        if (opcode.equals("NEW_INSTANCE")) {
            require(!flag(classes.get(type).getAccessFlags(), ABSTRACT | INTERFACE), "Instantiate abstract/interface: " + type);
        }
        checkedTypes.add(type);
    }
    static void auditMethodReference(Method caller, Instruction instruction, MethodReference reference) {
        String owner = reference.getDefiningClass(), key = mid(reference), op = instruction.getOpcode().name();
        memberInstructions++;
        if (!classes.containsKey(owner)) {
            require(platform(owner), "Missing non-platform method owner: " + key);
            externalMembers.add(key); return;
        }
        classAccess(owner, caller.getDefiningClass());
        Method target = resolveMethod(owner, suffix(key), new HashSet<>());
        require(target != null, "Unresolved native/runtime method " + key + " from " + mid(caller));
        require(accessible(target.getAccessFlags(), target.getDefiningClass(), caller.getDefiningClass()),
                "Inaccessible method " + mid(target) + " from " + mid(caller));
        require(op.startsWith("INVOKE_"), "Unexpected method reference instruction: " + op);
        boolean isStatic = flag(target.getAccessFlags(), STATIC);
        require(op.startsWith("INVOKE_STATIC") == isStatic, "Static/instance invocation mismatch: " + key);
        if (op.startsWith("INVOKE_DIRECT")) {
            require(target.getName().equals("<init>") || flag(target.getAccessFlags(), PRIVATE), "Non-direct target: " + key);
        } else if (!isStatic) {
            require(!target.getName().equals("<init>") && !flag(target.getAccessFlags(), PRIVATE), "Virtual invocation of direct target: " + key);
        }
        if (op.startsWith("INVOKE_INTERFACE")) {
            require(flag(classes.get(owner).getAccessFlags(), INTERFACE), "Interface invocation names a class: " + key);
        } else if (op.startsWith("INVOKE_VIRTUAL")) {
            require(!flag(classes.get(owner).getAccessFlags(), INTERFACE), "Virtual invocation names an interface: " + key);
        }
        int words = instruction instanceof RegisterRangeInstruction range ? range.getRegisterCount()
                  : instruction instanceof FiveRegisterInstruction list ? list.getRegisterCount() : -1;
        require(words == argumentWords(reference, isStatic), "Invoke register word count mismatch: " + key);
        checkedMembers.add(key);
    }
    static void auditFieldReference(Method caller, Instruction instruction, FieldReference reference) {
        String owner = reference.getDefiningClass(), key = fid(reference), op = instruction.getOpcode().name();
        memberInstructions++;
        if (!classes.containsKey(owner)) {
            require(platform(owner), "Missing non-platform field owner: " + key);
            externalMembers.add(key); return;
        }
        classAccess(owner, caller.getDefiningClass());
        Field target = resolveField(owner, reference.getName(), reference.getType(), new HashSet<>());
        require(target != null, "Unresolved native/runtime field " + key + " from " + mid(caller));
        require(accessible(target.getAccessFlags(), target.getDefiningClass(), caller.getDefiningClass()),
                "Inaccessible field " + fid(target) + " from " + mid(caller));
        require(op.startsWith("IGET") || op.startsWith("IPUT") || op.startsWith("SGET") || op.startsWith("SPUT"),
                "Unexpected field instruction: " + op);
        require((op.startsWith("SGET") || op.startsWith("SPUT")) == flag(target.getAccessFlags(), STATIC),
                "Static/instance field opcode mismatch: " + key);
        String valueType = target.getType();
        boolean object = valueType.startsWith("L") || valueType.startsWith("[");
        require(op.contains("OBJECT") == object, "Object/scalar field opcode mismatch: " + key);
        require(op.contains("WIDE") == (valueType.equals("J") || valueType.equals("D")), "Wide field opcode mismatch: " + key);
        if (op.contains("BOOLEAN")) require(valueType.equals("Z"), "Boolean field opcode mismatch: " + key);
        if (op.contains("BYTE")) require(valueType.equals("B"), "Byte field opcode mismatch: " + key);
        if (op.contains("CHAR")) require(valueType.equals("C"), "Char field opcode mismatch: " + key);
        if (op.contains("SHORT")) require(valueType.equals("S"), "Short field opcode mismatch: " + key);
        if ((op.startsWith("IPUT") || op.startsWith("SPUT")) && flag(target.getAccessFlags(), FINAL)) {
            require(caller.getDefiningClass().equals(target.getDefiningClass())
                    && caller.getName().equals(flag(target.getAccessFlags(), STATIC) ? "<clinit>" : "<init>"),
                    "Final field written outside its declaring initializer: " + key);
        }
        checkedMembers.add(key);
    }
    static void auditReferences(Method method) {
        if (method.getImplementation() == null) return;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            if (!(instruction instanceof ReferenceInstruction holder)) continue;
            var reference = holder.getReference();
            if (reference instanceof MethodReference value) auditMethodReference(method, instruction, value);
            else if (reference instanceof FieldReference value) auditFieldReference(method, instruction, value);
            else if (reference instanceof TypeReference value) auditType(value.getType(), method.getDefiningClass(), instruction.getOpcode().name());
        }
    }

    /** OpticalZoom.field/ProviderLifecycle.get use getDeclaredField starting at
     * the concrete class, so name hiding is checked rather than ignored. */
    static Field reflectionField(String type, String name) {
        Set<String> seen = new HashSet<>();
        while (type != null && seen.add(type)) {
            ClassDef definition = classes.get(type);
            if (definition == null) return null;
            Field result = null;
            for (Field field : definition.getFields()) if (field.getName().equals(name)) {
                require(result == null, "Ambiguous reflection field name: " + type + "->" + name);
                result = field;
            }
            if (result != null) return result;
            type = definition.getSuperclass();
        }
        return null;
    }
    static void fieldContract(String owner, String name, String type, boolean isStatic, boolean mutable, boolean allSubtypes) {
        require(classes.containsKey(owner), "Reflection receiver class absent: " + owner);
        int receivers = 0;
        for (String candidate : classes.keySet()) {
            if (!candidate.equals(owner) && !(allSubtypes && stock.containsKey(candidate) && subtype(candidate, owner))) continue;
            Field field = reflectionField(candidate, name);
            require(field != null && field.getType().equals(type), "Reflection field type/name mismatch: " + candidate + "->" + name + ":" + type);
            require(flag(field.getAccessFlags(), STATIC) == isStatic, "Reflection field staticness mismatch: " + fid(field));
            if (mutable) require(!flag(field.getAccessFlags(), FINAL), "Reflection write to final field: " + fid(field));
            receivers++;
        }
        require(receivers > 0, "Empty reflection receiver contract: " + owner);
        reflectionSymbols.add(name);
        reflectionRows.add("FIELD\t" + owner + "->" + name + ":" + type + "\treceivers=" + receivers + "\tstatic=" + isStatic + "\twrite=" + mutable);
    }
    static void methodContract(String owner, String name, String returnType, boolean allSubtypes) {
        require(classes.containsKey(owner), "Reflection method receiver absent: " + owner);
        int receivers = 0;
        for (String candidate : classes.keySet()) {
            if (!candidate.equals(owner) && !(allSubtypes && stock.containsKey(candidate) && subtype(candidate, owner))) continue;
            Method method = resolveMethod(candidate, name + "()" + returnType, new HashSet<>());
            require(method != null && flag(method.getAccessFlags(), PUBLIC) && !flag(method.getAccessFlags(), STATIC),
                    "Public zero-argument reflection method missing: " + candidate + "->" + name + "()" + returnType);
            receivers++;
        }
        reflectionSymbols.add(name);
        reflectionRows.add("METHOD\t" + owner + "->" + name + "()" + returnType + "\treceivers=" + receivers + "\tpublic_instance=true");
    }
    static void requireCall(String methodId, String referencedId) {
        Method method = effectiveMethods.get(methodId);
        require(method != null && method.getImplementation() != null, "Missing reflection primitive: " + methodId);
        boolean found = false;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            if (instruction instanceof ReferenceInstruction holder && holder.getReference() instanceof MethodReference reference
                    && mid(reference).equals(referencedId)) found = true;
        }
        require(found, "Reflection primitive does not use required resolution API: " + methodId + " -> " + referencedId);
    }
    static void reflectionContracts() {
        fieldContract(CAPTURE, "a", VE_SETTINGS, false, false, true);
        fieldContract(CAPTURE, "c", SETTINGS, false, false, true);
        fieldContract(CAPTURE, "d", "Landroid/content/Context;", false, false, true);
        fieldContract(CAPTURE, "o", "Li/s/a/w/k;", false, false, true);
        fieldContract(CAPTURE, "n", "Lcom/ss/android/vesdk/ConcurrentList;", false, false, true);
        fieldContract(CAPTURE, "p", "Ljava/util/concurrent/atomic/AtomicBoolean;", false, false, true);
        for (String name : List.of("l", "j", "M")) fieldContract(SETTINGS, name, "I", false, false, true);
        fieldContract(SETTINGS, "u0", "Z", false, true, true);
        fieldContract(SERVER, "INSTANCE", SERVER, true, false, false);
        require(flag(reflectionField(SERVER, "INSTANCE").getAccessFlags(), PUBLIC), "q.INSTANCE must be public for Class.getField");
        fieldContract(SERVER, "mCameraClient", "Li/s/a/w/k;", false, false, false);
        fieldContract(SERVER, "mCameraSettings", SETTINGS, false, false, false);
        fieldContract(SERVER, "mCameraInstance", CAMERA_BASE, false, false, false);
        fieldContract(SERVER, "mCurrentCameraState", "I", false, false, false);
        fieldContract(SERVER, "mHandler", "Landroid/os/Handler;", false, false, false);
        fieldContract(SERVER, "mProviderManager", MANAGER, false, false, false);
        // Independently observed on stock 5.6.2(740): pending-close is public
        // volatile, switch/destroyed are private volatile, background is private.
        // All are primitive instance booleans; reflection must retain access.
        for (var entry : Map.of("mIsCameraPendingClose", PUBLIC | AccessFlags.VOLATILE.getValue(),
                "mIsCameraSwitchState", PRIVATE | AccessFlags.VOLATILE.getValue(),
                "mHandlerDestroyed", PRIVATE | AccessFlags.VOLATILE.getValue(),
                "mOnBackGround", PRIVATE).entrySet()) {
            fieldContract(SERVER, entry.getKey(), "Z", false, false, false);
            require(reflectionField(SERVER, entry.getKey()).getAccessFlags() == entry.getValue(),
                    "Native lifecycle reflection field attributes changed: " + entry.getKey());
        }
        fieldContract(CAMERA1, "H", "Landroid/hardware/Camera;", false, false, true);
        fieldContract(CAMERA1, "K", "Ljava/lang/String;", false, false, true);
        fieldContract(CAMERA2, "K", "Landroid/hardware/camera2/CameraDevice;", false, false, true);
        fieldContract(CAMERA2, "H", "Li/s/a/w/i0/c;", false, false, true);
        require(subtype(CAMERA1, CAMERA_BASE) && subtype(CAMERA2, CAMERA_BASE), "Camera1/2 do not implement the server camera type");
        fieldContract(MODE, "h", SETTINGS, false, false, true);
        fieldContract(MODE, "g", CAMERA2, false, false, true);
        fieldContract(MODE, "a", "Landroid/hardware/camera2/CameraCharacteristics;", false, false, true);
        fieldContract(PROVIDER, "d", CAMERA_BASE, false, false, true);
        for (String name : List.of("h", "e")) fieldContract(PROVIDER, name, "Z", false, false, true);
        for (String name : List.of("a", "j", "k")) fieldContract(PROVIDER, name, PROVIDER.substring(0, PROVIDER.length()-1) + "$c;", false, false, true);
        fieldContract(OPTICAL, "capture", "Ljava/lang/ref/WeakReference;", true, false, false);
        fieldContract(OPTICAL, "foreground", "Z", true, false, false);
        fieldContract(OPTICAL, "recording", "Z", true, false, false);
        fieldContract(OPTICAL, "epoch", "J", true, false, false);
        methodContract(VE_SETTINGS, "getCameraFacing", FACING, true);
        methodContract(PIPELINE, "getFormat", FORMAT, true);
        methodContract(PIPELINE, "getSurfaceTexture", "Landroid/graphics/SurfaceTexture;", true);
        methodContract(PIPELINE, "getCaptureListener", "Lcom/ss/android/vesdk/frame/TECapturePipeline$CaptureListener;", true);
        methodContract(TEXTURE_PIPELINE, "getSurface", "Landroid/view/Surface;", true);
        methodContract(RECORDER_PIPELINE, "getRecorderSurface", "Landroid/view/Surface;", true);
        methodContract(MANAGER, "h", PROVIDER, true);
        for (String value : List.of("PIXEL_FORMAT_OpenGL_OES", "PIXEL_FORMAT_Recorder")) {
            fieldContract(FORMAT, value, FORMAT, true, false, false);
            require(flag(reflectionField(FORMAT, value).getAccessFlags(), AccessFlags.ENUM.getValue()), "Missing pixel-format enum constant: " + value);
        }
        fieldContract(FACING, "FACING_FRONT", FACING, true, false, false);
        require(flag(reflectionField(FACING, "FACING_FRONT").getAccessFlags(), AccessFlags.ENUM.getValue()), "Missing front-camera enum constant");
        requireCall(PROVIDER_HELPER + "->get(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                OPTICAL + "->field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
        requireCall(OPTICAL + "->field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                "Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;");
        requireCall(OPTICAL + "->field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                "Ljava/lang/Class;->getSuperclass()Ljava/lang/Class;");
        requireCall(OPTICAL + "->field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                "Ljava/lang/reflect/Field;->setAccessible(Z)V");
        requireCall(PROVIDER_HELPER + "->put(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V",
                "Ljava/lang/reflect/Field;->setAccessible(Z)V");
        requireCall(PROVIDER_HELPER + "->call(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                "Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
        requireCall(MANUAL + "->field(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                "Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;");
        reflectionSymbols.add("ULikeFront1931");
    }
    static void reflectionLiteralCoverage(Collection<Method> helpers) {
        Set<String> seen = new TreeSet<>();
        for (Method method : helpers) {
            if (method.getImplementation() == null) continue;
            for (Instruction instruction : method.getImplementation().getInstructions()) {
                if (!(instruction instanceof ReferenceInstruction holder) || !(holder.getReference() instanceof StringReference text)) continue;
                String value = text.getString();
                if (value.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
                    require(reflectionSymbols.contains(value), "Unreviewed reflection/enum symbol in helper: " + value);
                    seen.add(value);
                } else if (value.equals("i.s.a.w.q")) seen.add(value);
                else require(!value.matches("(?:[a-zA-Z_$][a-zA-Z0-9_$]*\\.){2,}[a-zA-Z_$][a-zA-Z0-9_$]*"),
                             "Unreviewed reflected class name: " + value);
            }
        }
        require(seen.containsAll(Set.of("i.s.a.w.q", "INSTANCE", "H", "K", "mCameraClient", "mCameraSettings", "mCameraInstance",
                "getCameraFacing", "FACING_FRONT", "getSurface", "getRecorderSurface", "u0", "d",
                "mIsCameraPendingClose", "mIsCameraSwitchState", "mHandlerDestroyed", "mOnBackGround")),
                "Key reflection contracts are not used by emitted helper");
        System.out.println("REFLECTION_SYMBOLS\t" + String.join(",", seen));
    }
    static boolean callsFront(Method method) {
        if (method.getImplementation() == null) return false;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            if (instruction instanceof ReferenceInstruction holder && holder.getReference() instanceof MethodReference target
                    && frontClass(target.getDefiningClass())) return true;
        }
        return false;
    }
    static String sha(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
    public static void main(String[] args) throws Exception {
        require(args.length == 2, "Usage: VerifyNativeAbi1931 STOCK_APK EMITTED_DIR");
        Path apk = Path.of(args[0]), emitted = Path.of(args[1]);
        stock = load(apk);
        var runtime = load(emitted.resolve("runtime.dex"));
        var payload = methods(load(emitted.resolve("methods.dex")).values());
        classes = new TreeMap<>(stock);
        for (var entry : runtime.entrySet()) require(classes.put(entry.getKey(), entry.getValue()) == null,
                "Runtime class collides with stock: " + entry.getKey());
        effectiveMethods = methods(classes.values());
        for (var entry : payload.entrySet()) {
            Method original = effectiveMethods.get(entry.getKey());
            require(original != null, "Native payload target missing from original APK: " + entry.getKey());
            require(original.getAccessFlags() == entry.getValue().getAccessFlags(), "Native payload changes method access: " + entry.getKey());
            effectiveMethods.put(entry.getKey(), entry.getValue());
        }
        List<Method> helpers = new ArrayList<>(), nativeHooks = new ArrayList<>(), runtimeHooks = new ArrayList<>();
        for (ClassDef type : runtime.values()) for (Method method : type.getMethods()) {
            if (frontClass(type.getType())) helpers.add(method);
            else if (callsFront(method)) runtimeHooks.add(method);
        }
        for (Method method : payload.values()) if (callsFront(method)) nativeHooks.add(method);
        require(!helpers.isEmpty() && !nativeHooks.isEmpty() && !runtimeHooks.isEmpty(), "Missing emitted front helper or hooked callers");
        for (Method method : helpers) auditReferences(method);
        for (Method method : nativeHooks) auditReferences(method);
        for (Method method : runtimeHooks) auditReferences(method);
        reflectionContracts();
        reflectionLiteralCoverage(helpers);
        for (String row : reflectionRows) System.out.println("REFLECTION_ABI\t" + row);
        System.out.println("ABI_INPUT_SHA256\tstock=" + sha(apk) + "\truntime=" + sha(emitted.resolve("runtime.dex"))
                + "\tmethods=" + sha(emitted.resolve("methods.dex")));
        System.out.println("VERIFY_NATIVE_ABI1931\thelper_methods=" + helpers.size() + "\tnative_hook_methods=" + nativeHooks.size()
                + "\truntime_hook_methods=" + runtimeHooks.size() + "\tdirect_member_instructions=" + memberInstructions
                + "\tresolved_unique_native_runtime_members=" + checkedMembers.size() + "\tchecked_type_instructions=" + typeInstructions
                + "\tresolved_unique_native_runtime_types=" + checkedTypes.size() + "\treflection_contracts=" + reflectionRows.size()
                + "\tplatform_jdk_members_deferred=" + externalMembers.size() + "\tplatform_jdk_types_deferred=" + externalTypes.size()
                + "\tassertions=" + assertions);
        System.out.println("PASS native/runtime ABI declarations, access and reflection contracts. Platform/JDK linkage deferred; "
                + "no Android/ART execution, hardware camera test, or proof of device symptom resolution.");
    }
}
