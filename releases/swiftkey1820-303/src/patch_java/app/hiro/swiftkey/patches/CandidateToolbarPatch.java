package app.hiro.swiftkey.patches;

import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Exact native layout and action-builder bridges for SwiftKey Beta 9.13.16.4. */
public final class CandidateToolbarPatch {
    static final String[] OWNERS = {
        "Lob0/d1;", "Lob0/e;",
        "Lcom/touchtype/keyboard/candidates/view/SequentialCandidateBarLayoutWithECWButton;",
        "Lcom/touchtype/keyboard/candidates/view/SequentialCandidateBarLayoutWithECWButton;",
        "Lcom/touchtype/keyboard/expandedcandidate/ExpandedResultsCloseButton;",
        "Lcom/touchtype/keyboard/expandedcandidate/ExpandedResultsCloseButton;", "Lfc0/h;"
    };
    static final String[] SIGNATURES = {
        "<init>(Landroid/view/ContextThemeWrapper;Lfb0/t1;Ldd0/i0;Lkd0/e0;Lfb0/d;)V",
        "<init>(Landroid/view/ContextThemeWrapper;Lfb0/n0;Lfb0/i0;Lde0/g;Lcm0/d;Lcm0/a;Ldd0/i0;Lob0/d1;)V",
        "onFinishInflate()V", "onMeasure(II)V", "onAttachedToWindow()V", "onMeasure(II)V",
        "invoke(Ljava/lang/Object;)Ljava/lang/Object;"
    };
    private static final String[] SUPERS = {
        "Landroidx/constraintlayout/widget/ConstraintLayout;", "Landroidx/constraintlayout/widget/ConstraintLayout;",
        "Landroid/widget/FrameLayout;", "Landroid/widget/FrameLayout;",
        "Lcom/google/android/material/button/MaterialButton;", "Lcom/google/android/material/button/MaterialButton;",
        "Ljava/lang/Object;"
    };
    private static final String[] HASHES = {
        "9462b4add4665b24dc4c829c878df584fe8c6fe6bfeae0fd390ad1bb967a38c3",
        "d69044b64d21c9093ae5c6a1f7bb67cd8fa771b73519e3048c6eea4faf1ad7cc",
        "1d4e597c7cd5c67684128e559cb6ee6e5bf05c71c83b3abf9438cc34d1fcdfd2",
        "98112c3b1d7a98e210657d9952a5aa2728eb5bc2abc5bc126828a2ec0cad41e9",
        "d99af5755673676cc6ce25d740775d2c7c24cebf7e42fe4d63ea54ef674a72a3",
        "11ebb68b0d9f93f0b3869746a820496a9da212c15a42235a58def6a71d5335e6",
        "430dad544da7bfc43818135785444fc8ecc761d0734ff4790aee6273981ab4a2"
    };
    private static final int[] REGISTERS = {13, 9, 3, 5, 3, 3, 15};
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private static final String LAYOUT = "Lhiro/swiftkey/CandidateToolbar;";
    private CandidateToolbarPatch() {}

    public static void checkOriginal(BytecodePatchContext context) {
        Map<String, ClassDef> classes = new HashMap<String, ClassDef>();
        for (String owner : OWNERS) classes.put(owner, context.classDefByOrNull(owner));
        inspect(classes);
    }

    public static void apply(BytecodePatchContext context) {
        checkOriginal(context);
        MutableMethod[] methods = new MutableMethod[OWNERS.length];
        for (int i = 0; i < methods.length; i++) {
            String descriptor = OWNERS[i] + "->" + SIGNATURES[i];
            for (MutableMethod method : context.mutableClassDefBy(OWNERS[i]).getMethods())
                if (descriptor.equals(FORMAT.getMethodDescriptor(method))) {
                    require(methods[i] == null, "Duplicate toolbar target " + descriptor);
                    methods[i] = method;
                }
            require(methods[i] != null, "Missing mutable toolbar target " + descriptor);
        }
        applyMethods(methods);
    }

    static Method[] inspect(Map<String, ClassDef> classes) {
        Method[] methods = new Method[OWNERS.length];
        for (int i = 0; i < methods.length; i++) {
            ClassDef target = classes.get(OWNERS[i]);
            require(target != null && OWNERS[i].equals(target.getType())
                    && SUPERS[i].equals(target.getSuperclass()), "Toolbar target hierarchy changed " + OWNERS[i]);
            String descriptor = OWNERS[i] + "->" + SIGNATURES[i];
            for (Method method : target.getMethods()) if (descriptor.equals(FORMAT.getMethodDescriptor(method))) {
                require(methods[i] == null, "Duplicate toolbar target " + descriptor);
                methods[i] = method;
            }
            verify(methods[i], i);
        }
        return methods;
    }

    private static void verify(Method method, int i) {
        require(method != null && method.getImplementation() != null, "Missing toolbar method " + SIGNATURES[i]);
        require((OWNERS[i] + "->" + SIGNATURES[i]).equals(FORMAT.getMethodDescriptor(method)), "Toolbar method signature changed");
        require(method.getAccessFlags() == (i < 2 ? 0x10001 : 0x11), "Toolbar method access changed");
        require(method.getImplementation().getRegisterCount() == REGISTERS[i], "Toolbar method registers changed");
        require(HASHES[i].equals(MethodContract.sha256(method)), "Toolbar method fingerprint changed " + SIGNATURES[i]);
    }

    private static int receiver(Method method) {
        int words = 1;
        for (CharSequence parameter : method.getParameterTypes())
            words += "J".contentEquals(parameter) || "D".contentEquals(parameter) ? 2 : 1;
        return method.getImplementation().getRegisterCount() - words;
    }

    private static BuilderInstruction bridge(Method method, int i) {
        String owner = LAYOUT, name;
        List<String> parameters;
        if (i == 6) {
            owner = "Lhiro/swiftkey/EmojiToolbar;"; name = "configure";
            parameters = Arrays.asList("Lfc0/h;", "Ljava/lang/Object;");
        } else {
            name = i == 0 ? "bindToolbar" : i == 1 ? "layoutContainer" : i < 4 ? "layoutCollapsed" : "layoutExpandedClose";
            parameters = Collections.singletonList(i == 0 ? "Lob0/d1;" : i < 4 ? "Landroid/view/View;" : OWNERS[4]);
        }
        return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, receiver(method), i == 6 ? 2 : 1,
                new ImmutableMethodReference(owner, name, parameters, "V"));
    }

    static void applyMethods(MutableMethod[] methods) {
        require(methods != null && methods.length == OWNERS.length, "Toolbar hook target count changed");
        for (int i = 0; i < methods.length; i++) verify(methods[i], i);
        for (int i = 0; i < methods.length; i++) {
            MutableMethod method = methods[i];
            MutableMethodImplementation body = method.getImplementation();
            if (i == 3 || i == 5) {
                body.addInstruction(0, bridge(method, i));
            } else {
                int count = 0;
                for (int n = body.getInstructions().size() - 1; n >= 0; n--) {
                    BuilderInstruction original = body.getInstructions().get(n);
                    Opcode expected = i == 6 ? Opcode.RETURN_OBJECT : Opcode.RETURN_VOID;
                    if (original.getOpcode() != expected) continue;
                    // Preserve the original MethodLocation: native gotos and
                    // switch edges targeting a return must enter the bridge.
                    BuilderInstruction terminal = i == 6
                            ? new BuilderInstruction11x(Opcode.RETURN_OBJECT, ((OneRegisterInstruction) original).getRegisterA())
                            : new BuilderInstruction10x(Opcode.RETURN_VOID);
                    body.replaceInstruction(n, bridge(method, i));
                    body.addInstruction(n + 1, terminal);
                    count++;
                }
                require(count == (i == 6 ? 2 : 1), "Toolbar return count changed " + SIGNATURES[i]);
            }
            require(body.getRegisterCount() == REGISTERS[i], "Toolbar bridge changed register count");
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
