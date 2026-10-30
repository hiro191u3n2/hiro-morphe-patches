/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Audits scoped OneBack replacement and preservation of other applications. */
public final class RecoveredSourceAudit {
    
    static final String ONEBACK = "Lapp/hiro/oneback/patches/";
    static boolean replaced(String type) {
        return type.startsWith(ONEBACK);
    }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static Map<String,ClassDef> read(String path) throws Exception {
        Map<String,ClassDef> values = new TreeMap<>();
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String entry : container.getDexEntryNames())
            for (ClassDef value : container.getEntry(entry).getDexFile().getClasses())
                check(values.put(value.getType(), value) == null, "Duplicate class " + value.getType());
        return values;
    }
    static byte[] canonical(Collection<? extends ClassDef> values) throws Exception {
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), values));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally { store.close(); }
    }
    static void equal(Map<String,ClassDef> first, Map<String,ClassDef> second, String why) throws Exception {
        check(first.keySet().equals(second.keySet()), why + " descriptors changed");
        check(Arrays.equals(canonical(first.values()), canonical(second.values())), why + " definitions changed");
    }
    /** Source edits move line/local debug records in otherwise unchanged nested
     * classes. Ignore ONLY MethodImplementation.debugItems for this comparison:
     * instruction encodings, registers, tries/handlers, method parameters and
     * annotations, fields/initial values, class metadata and sourceFile remain.
     * This normalized definition is never shipped; verified matches retain the
     * complete baseline ClassDef, including its original debug information. */
    static List<Method> withoutDebug(Iterable<? extends Method> methods) {
        List<Method> values = new ArrayList<>();
        for (Method method : methods) {
            MethodImplementation body = method.getImplementation();
            if (body != null) body = new ImmutableMethodImplementation(body.getRegisterCount(),
                body.getInstructions(), body.getTryBlocks(), Collections.emptyList());
            values.add(new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                method.getReturnType(), method.getAccessFlags(), method.getAnnotations(),
                method.getHiddenApiRestrictions(), body));
        }
        return values;
    }
    static ClassDef withoutDebug(ClassDef value) {
        return new ImmutableClassDef(value.getType(), value.getAccessFlags(), value.getSuperclass(),
            value.getInterfaces(), value.getSourceFile(), value.getAnnotations(),
            value.getStaticFields(), value.getInstanceFields(),
            withoutDebug(value.getDirectMethods()), withoutDebug(value.getVirtualMethods()));
    }
    static boolean sameDefinition(ClassDef first, ClassDef second) throws Exception {
        return Arrays.equals(canonical(Collections.singleton(first)), canonical(Collections.singleton(second)));
    }
    static final Set<String> RUNTIME_CHANGES = new TreeSet<>(Arrays.asList(
        "Lapp/hiro/oneback/runtime/InstagramExternalProfile;",
        "Lapp/hiro/oneback/runtime/InstagramDispatcherBack;"));
    public static void main(String[] args) throws Exception {
        check(args.length == 6, "SAVED_RUNTIME COMPILED_RUNTIME BASE_RUNTIME SAVED_LOADER COMPILED_LOADER BASE_LOADER");
        Map<String,ClassDef> saved=read(args[0]),compiled=read(args[1]),base=read(args[2]);
        check(saved.size()==93 && base.keySet().equals(saved.keySet()) && saved.keySet().equals(compiled.keySet()),
            "Runtime inventory must match all 93 existing classes");
        int exact=0,debugOnly=0,preserved=0;
        Set<String> changed=new TreeSet<>();
        for(String name:saved.keySet()) {
            ClassDef wanted=saved.get(name), rebuilt=compiled.get(name);
            if(sameDefinition(wanted,rebuilt))exact++;
            else {
                check(sameDefinition(withoutDebug(wanted),withoutDebug(rebuilt)),"Recovered runtime source differs: "+name);
                debugOnly++;
            }
            if(sameDefinition(base.get(name),wanted))preserved++;
            else changed.add(name);
        }
        check(changed.equals(RUNTIME_CHANGES) && preserved==91,"Unrelated runtime changed: "+changed);
        Map<String,ClassDef> savedLoader=read(args[3]),compiledLoader=read(args[4]),baseLoader=read(args[5]);
        Map<String,ClassDef> owned=new TreeMap<>(),kept=new TreeMap<>(),baseKept=new TreeMap<>();
        for(ClassDef c:savedLoader.values())(replaced(c.getType())?owned:kept).put(c.getType(),c);
        for(ClassDef c:baseLoader.values())if(!replaced(c.getType()))baseKept.put(c.getType(),c);
        check(owned.size()==18 && compiledLoader.size()==18,"OneBack loader inventory differs");
        equal(owned,compiledLoader,"Recovered loader source");
        equal(baseKept,kept,"Unrelated application loader classes");
        System.out.println("PASS RecoveredSourceAudit runtime: classes="+saved.size()+"; exact="+exact+"; debug-only="+debugOnly);
        System.out.println("PASS RecoveredSourceAudit baseline: runtime-preserved="+preserved+"; changed="+changed);
        System.out.println("PASS RecoveredSourceAudit loader: source-classes="+owned.size()+"; unrelated-preserved="+kept.size());
    }
}
