import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Change one helper and prepend one decode call to the existing resource entrypoint. */
public final class LoaderAudit {
    static final String TARGET = "Lapp/hiro/yahootransit/patches/NearMeLayout;";
    static final String ENTRY = "Ljp/hiro/patches/yahootransit/YahooTransitAdPatch;";
    static final ImmutableMethodReference PREPARE = new ImmutableMethodReference(TARGET,
        "prepare", Collections.singletonList("Ljava/lang/Object;"), "V");
    static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
    static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String entry : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(entry).getDexFile().getClasses())
            check(result.put(c.getType(), c) == null, "duplicate class");
        return result;
    }
    static byte[] canonical(ClassDef clazz) throws Exception {
        MemoryDataStore out = new MemoryDataStore();
        try {
            DexPool.writeTo(out, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(clazz)));
            return Arrays.copyOf(out.getBuffer(), out.getSize());
        } finally { out.close(); }
    }
    static Method implementation(Method method, MethodImplementation body) {
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
            method.getReturnType(), method.getAccessFlags(), method.getAnnotations(), method.getHiddenApiRestrictions(), body);
    }
    static ClassDef editEntry(ClassDef input, boolean remove) {
        List<Method> methods = new ArrayList<>();
        int changed = 0;
        for (Method method : input.getMethods()) {
            if (!method.getName().equals("collapseLayouts")) { methods.add(method); continue; }
            check(method.getParameterTypes().equals(Collections.singletonList("Ljava/lang/Object;"))
                && method.getReturnType().equals("V") && (method.getAccessFlags() & 8) != 0,
                "unexpected entrypoint signature");
            MutableMethodImplementation body = new MutableMethodImplementation(method.getImplementation());
            int argument = body.getRegisterCount() - 1;
            if (remove) {
                check(body.getInstructions().get(0).getOpcode() == Opcode.INVOKE_STATIC_RANGE,
                      "first entry instruction must prepare resources");
                Instruction3rc call = (Instruction3rc) body.getInstructions().get(0);
                check(call.getRegisterCount() == 1 && call.getStartRegister() == argument
                    && PREPARE.equals(call.getReference()), "unexpected preparation call");
                body.removeInstruction(0);
            } else {
                body.addInstruction(0, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, argument, 1, PREPARE));
            }
            methods.add(implementation(method, body));
            changed++;
        }
        check(changed == 1, "unique resource entrypoint required");
        return new ImmutableClassDef(input.getType(), input.getAccessFlags(), input.getSuperclass(),
            input.getInterfaces(), input.getSourceFile(), input.getAnnotations(), input.getFields(), methods);
    }
    public static void main(String[] args) throws Exception {
        Map<String, ClassDef> old = read(args[0]), replacement = read(args[1]), combined = new TreeMap<>(old);
        check(replacement.keySet().equals(Collections.singleton(TARGET)), "replacement must contain exactly NearMeLayout");
        check(old.containsKey(TARGET) && old.containsKey(ENTRY), "baseline helper/entrypoint absent");
        boolean prepareExists = false;
        for (Method method : replacement.get(TARGET).getMethods())
            if (PREPARE.equals(method) && (method.getAccessFlags() & 9) == 9) prepareExists = true;
        check(prepareExists, "new public static preparation method missing");
        combined.put(TARGET, replacement.get(TARGET));
        combined.put(ENTRY, editEntry(old.get(ENTRY), false));
        check(Arrays.equals(canonical(old.get(ENTRY)), canonical(editEntry(combined.get(ENTRY), true))),
              "resource entrypoint changed beyond the one prepended call");
        DexPool.writeTo(args[2], new ImmutableDexFile(Opcodes.getDefault(), combined.values()));
        Map<String, ClassDef> written = read(args[2]);
        check(written.keySet().equals(old.keySet()), "class inventory changed");
        int preserved = 0;
        for (String name : old.keySet()) {
            check(Arrays.equals(canonical(combined.get(name)), canonical(written.get(name))), "definition changed on writing: " + name);
            if (!TARGET.equals(name) && !ENTRY.equals(name)) {
                check(Arrays.equals(canonical(old.get(name)), canonical(written.get(name))), "unrelated class changed: " + name);
                preserved++;
            }
        }
        check(Arrays.equals(canonical(old.get(ENTRY)), canonical(editEntry(written.get(ENTRY), true))),
              "written entrypoint differs after removing preparation call");
        System.out.println("PASS LoaderAudit: replaced one helper and prepended exactly one preparation call to collapseLayouts; "
            + preserved + " other DEX classes preserved canonically; exact class inventory and all remaining entrypoint code retained.");
    }
}
