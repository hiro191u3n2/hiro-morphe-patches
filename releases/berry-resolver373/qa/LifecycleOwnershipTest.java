import app.hiro.berry.patches.GenericLifecycleResolver;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import java.io.File;
import java.util.*;
import java.util.function.Function;

/** Tests real lifecycle methods plus local ownership/branch mutations.
 * Does not emit or claim to execute modified APKs. */
public final class LifecycleOwnershipTest {
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static final String WEB = "Landroid/webkit/WebView;";
    private static final String[] ANCHORS = {
        WEB + "->loadUrl(Ljava/lang/String;)V", WEB + "->onResume()V",
        WEB + "->onPause()V", WEB + "->stopLoading()V", WEB + "->destroy()V",
        WEB + "->saveState(Landroid/os/Bundle;)Landroid/webkit/WebBackForwardList;",
        WEB + "->restoreState(Landroid/os/Bundle;)Landroid/webkit/WebBackForwardList;"
    };
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("APK expectedIndirectCount");
        Map<String,ClassDef> classes = new LinkedHashMap<>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(args[0]), Opcodes.forApi(37));
        for (String name : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(name).getDexFile().getClasses())
            classes.put(c.getType(), c);
        ClassDef controller = null;
        Field webField = null;
        for (ClassDef c : classes.values()) for (Field f : c.getFields()) {
            ClassDef type = classes.get(f.getType());
            if (type == null || !WEB.equals(type.getSuperclass()) || (f.getAccessFlags() & 9) != 1) continue;
            int load = 0, destroy = 0;
            for (Method m : c.getMethods()) {
                if (GenericLifecycleResolver.ownsWebReceiver(m, ANCHORS[0], null, F.getFieldDescriptor(f))) load++;
                if (GenericLifecycleResolver.ownsWebReceiver(m, ANCHORS[4], null, F.getFieldDescriptor(f))) destroy++;
            }
            if (load == 1 && destroy == 1) {
                require(controller == null, "controller fixture remains unique"); controller = c; webField = f;
            }
        }
        require(controller != null, "owned WebView controller discovered");
        String webDescriptor = F.getFieldDescriptor(webField);
        int indirect = 0;
        Method indirectRestore = null;
        Field ownerField = null;
        for (String anchor : ANCHORS) {
            int matches = 0;
            for (ClassDef c : classes.values()) {
                Field controllerField = null;
                if (!c.getType().equals(controller.getType())) {
                    List<Field> fields = new ArrayList<>();
                    for (Field f : c.getFields()) if (f.getType().equals(controller.getType()) && (f.getAccessFlags() & 9) == 1) fields.add(f);
                    if (fields.size() != 1) continue;
                    controllerField = fields.get(0);
                }
                String first = controllerField == null ? null : F.getFieldDescriptor(controllerField);
                for (Method m : c.getMethods()) {
                    if (!GenericLifecycleResolver.ownsWebReceiver(m, anchor, first, webDescriptor)) continue;
                    matches++;
                    if (first != null) {
                        indirect++;
                        if (anchor.contains("restoreState")) { indirectRestore = m; ownerField = controllerField; }
                    }
                    System.out.println("MATCH " + anchor + " => " + F.getMethodDescriptor(m) + " via " + first);
                }
            }
            require(matches == 1, "unique owned receiver for " + anchor);
        }
        require(indirect == Integer.parseInt(args[1]), "expected moved lifecycle count " + indirect);
        if (indirectRestore != null) {
            final Method restore = indirectRestore;
            final String first = F.getFieldDescriptor(ownerField);
            final String anchor = ANCHORS[6];
            int foreign = restore.getImplementation().getRegisterCount() - 1; // Bundle argument, never p0/controller/WebView.
            Method wrongOwner = mutate(restore, first, i -> {
                TwoRegisterInstruction r = (TwoRegisterInstruction) i;
                return new ImmutableInstruction22c(i.getOpcode(), r.getRegisterA(), foreign, ((ReferenceInstruction)i).getReference());
            });
            reject(wrongOwner, anchor, first, webDescriptor, "foreign receiver for owner-to-controller load");
            Method wrongWeb = mutate(restore, webDescriptor, i -> {
                TwoRegisterInstruction r = (TwoRegisterInstruction) i;
                return new ImmutableInstruction22c(i.getOpcode(), r.getRegisterA(), foreign, ((ReferenceInstruction)i).getReference());
            });
            reject(wrongWeb, anchor, first, webDescriptor, "foreign receiver for controller-to-WebView load");
            Method wrongCall = mutate(restore, anchor, i -> {
                FiveRegisterInstruction r = (FiveRegisterInstruction) i;
                return new ImmutableInstruction35c(i.getOpcode(), r.getRegisterCount(), foreign,
                    r.getRegisterD(), r.getRegisterE(), r.getRegisterF(), r.getRegisterG(), ((ReferenceInstruction)i).getReference());
            });
            reject(wrongCall, anchor, first, webDescriptor, "foreign Android-call receiver");
            branchFixture(restore.getDefiningClass(), ownerField, webField);
        }
        System.out.println("PASS lifecycle ownership checks=" + checks);
    }

    private static void branchFixture(String owner, Field first, Field second) {
        MethodReference resume = new ImmutableMethodReference(WEB, "onResume", Collections.emptyList(), "V");
        List<Instruction> code = Arrays.asList(
            new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 2, first),
            new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 0, second),
            new ImmutableInstruction21t(Opcode.IF_EQZ, 3, 3),
            new ImmutableInstruction10x(Opcode.NOP),
            new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0, resume),
            new ImmutableInstruction10x(Opcode.RETURN_VOID));
        Method positive = fixture(owner, code);
        String firstDescriptor = F.getFieldDescriptor(first), secondDescriptor = F.getFieldDescriptor(second);
        require(GenericLifecycleResolver.ownsWebReceiver(positive, ANCHORS[1], firstDescriptor, secondDescriptor),
            "branch join preserves owned receiver on both paths");
        List<Instruction> corrupt = new ArrayList<>(code);
        corrupt.set(3, new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
        reject(fixture(owner, corrupt), ANCHORS[1], firstDescriptor, secondDescriptor,
            "branch join rejects receiver replaced on one path");
        List<Instruction> duplicate = new ArrayList<>(code);
        duplicate.add(5, code.get(4));
        reject(fixture(owner, duplicate), ANCHORS[1], firstDescriptor, secondDescriptor, "duplicate Android anchor");
    }
    private static Method fixture(String owner, List<Instruction> instructions) {
        return new ImmutableMethod(owner, "testOwnedReceiver",
            Collections.singletonList(new ImmutableMethodParameter("Z", Collections.emptySet(), null)), "V", 1,
            Collections.emptySet(), Collections.emptySet(), new ImmutableMethodImplementation(4,
                instructions, Collections.emptyList(), Collections.emptyList()));
    }
    private static Method mutate(Method method, String reference, Function<Instruction,Instruction> transform) {
        List<Instruction> instructions = new ArrayList<>(); int changed = 0;
        for (Instruction i : method.getImplementation().getInstructions()) {
            if (i instanceof ReferenceInstruction && reference.equals(F.getReference(((ReferenceInstruction)i).getReference()))) {
                instructions.add(transform.apply(i)); changed++;
            } else instructions.add(i);
        }
        require(changed == 1, "exactly one mutation for " + reference);
        MethodImplementation impl = method.getImplementation();
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
            method.getReturnType(), method.getAccessFlags(), method.getAnnotations(), method.getHiddenApiRestrictions(),
            new ImmutableMethodImplementation(impl.getRegisterCount(), instructions, impl.getTryBlocks(), impl.getDebugItems()));
    }
    private static void reject(Method method, String anchor, String first, String second, String label) {
        require(!GenericLifecycleResolver.ownsWebReceiver(method, anchor, first, second), "rejected " + label);
    }
    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++; System.out.println("PASS " + label);
    }
}
