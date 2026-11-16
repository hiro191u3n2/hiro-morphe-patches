import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.io.File;
import java.security.MessageDigest;
import java.util.*;

/** Fixture probe: canonicalizes every instruction, register, reference, branch and try handler. */
public final class LoaderFixtureProbe {
    public static final String REACT = "Lcom/facebook/react/runtime/ReactInstance;";
    public static final String UPDATER = "Lcom/discord/bundle_updater/BundleUpdater;";
    public static Map<String, ClassDef> read(String apk) throws Exception {
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(new File(apk), Opcodes.forApi(26));
        Map<String, ClassDef> result = new HashMap<>();
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef clazz : container.getEntry(entry).getDexFile().getClasses()) {
                if (!clazz.getType().equals(REACT) && !clazz.getType().equals(UPDATER)) continue;
                if (result.put(clazz.getType(), clazz) != null) throw new IllegalStateException("Duplicate class");
            }
        }
        if (result.size() != 2) throw new IllegalStateException("Missing fixture class");
        return result;
    }
    public static String canonical(Method method) throws Exception {
        MethodImplementation impl = method.getImplementation();
        List<ImmutableMethodParameter> params = new ArrayList<>();
        for (CharSequence type : method.getParameterTypes())
            params.add(new ImmutableMethodParameter(type.toString(), Collections.emptySet(), null));
        ImmutableMethod copy = new ImmutableMethod(method.getDefiningClass(), method.getName(), params,
            method.getReturnType(), method.getAccessFlags(), Collections.emptySet(), Collections.emptySet(),
            new ImmutableMethodImplementation(impl.getRegisterCount(), impl.getInstructions(), impl.getTryBlocks(), Collections.emptyList()));
        ImmutableClassDef clazz = new ImmutableClassDef(method.getDefiningClass(), 1, "Ljava/lang/Object;",
            Collections.emptyList(), null, Collections.emptySet(), Collections.emptyList(), Collections.singletonList(copy));
        MemoryDataStore out = new MemoryDataStore();
        try {
            DexPool.writeTo(out, new ImmutableDexFile(Opcodes.forApi(26), Collections.singleton(clazz)));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(out.getBuffer(), out.getSize()));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b & 255));
            return hex.toString();
        } finally { out.close(); }
    }
    public static void main(String[] args) throws Exception {
        for (String apk : args) {
            System.out.println(apk);
            for (ClassDef clazz : read(apk).values()) for (Method method : clazz.getMethods())
                if (method.getName().equals("loadJSBundle") || method.getName().equals("checkForUpdate"))
                    System.out.println(method + " " + canonical(method));
        }
    }
}
