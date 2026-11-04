package app.hiro.swiftkey.patches;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import app.morphe.patcher.PatcherConfig;
import app.morphe.patcher.PackageMetadata;
import app.morphe.patcher.dex.BytecodeMode;
import app.morphe.patcher.dex.DexVerifier;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.PatchClasses;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.smali.*;

/** Executes old/new patch helpers through real SDK contexts, then verifies serialized native DEX. */
public final class NativeSpaceProbe {
    static int assertions;
    static final DexFormatter F = DexFormatter.INSTANCE;
    static void check(boolean value, String message) { assertions++; if (!value) throw new AssertionError(message); }
    static Map<String,ClassDef> read(Path path) throws Exception {
        Map<String,ClassDef> classes = new TreeMap<>();
        for (ClassDef c : DexFileFactory.loadDexFile(path.toFile(), Opcodes.forApi(26)).getClasses())
            check(classes.put(c.getType(),c) == null, "Unique fixture class");
        return classes;
    }
    static BytecodePatchContext context(Map<String,ClassDef> classes, Path work) throws Exception {
        DexVerifier verifier = new DexVerifier() {
            public void verifyDexFile(File f) { }
            public void verifyDexDirectory(File f) { }
            public void verifyApkFile(File f) { }
        };
        // The two supported SDKs changed only this test harness constructor.
        PatcherConfig config;
        try {
            config = PatcherConfig.class.getConstructor(File.class, File.class, String.class, String.class, boolean.class, Set.class, BytecodeMode.class, DexVerifier.class, File.class)
                    .newInstance(work.resolve("fixture.apk").toFile(), work.toFile(), null, null, true, Collections.emptySet(), BytecodeMode.FULL, verifier, null);
        } catch (NoSuchMethodException currentSdk) {
            config = PatcherConfig.class.getConstructor(File.class, File.class, Set.class, BytecodeMode.class, DexVerifier.class, File.class, String.class, boolean.class)
                    .newInstance(work.resolve("fixture.apk").toFile(), work.toFile(), Collections.emptySet(), BytecodeMode.FULL, verifier, null, null, false);
        }
        BytecodePatchContext context = new BytecodePatchContext(config, new PackageMetadata("com.touchtype.swiftkey.beta", "9.13.16.4", "1", null));
        context.setOpcodes$morphe_patcher(Opcodes.forApi(26));
        context.setPatchClasses$morphe_patcher(new PatchClasses(new LinkedHashSet<ClassDef>(classes.values())));
        return context;
    }
    static Method method(ClassDef owner, int index) {
        for (Method m : owner.getMethods()) if (SpaceConversionPatch.descriptor(index).equals(F.getMethodDescriptor(m))) return m;
        throw new AssertionError("Missing fixture target");
    }
    static Method method(BytecodePatchContext c, int index) { return method(c.mutableClassDefBy(SpaceConversionPatch.OWNERS[index]), index); }
    static List<Instruction> code(Method m) { List<Instruction> a=new ArrayList<>(); for(Instruction i:m.getImplementation().getInstructions())a.add(i);return a; }
    static Map<String,String> hashes(BytecodePatchContext c) {
        Map<String,String> h=new TreeMap<>();for(int i=0;i<SpaceConversionPatch.OWNERS.length;i++)h.put(SpaceConversionPatch.descriptor(i),MethodContract.sha256(method(c,i)));return h;
    }
    static ClassDef replace(ClassDef owner, Method replacement) {
        List<Method> methods=new ArrayList<>();for(Method m:owner.getMethods())methods.add(F.getMethodDescriptor(m).equals(F.getMethodDescriptor(replacement))?replacement:m);
        return new ImmutableClassDef(owner.getType(),owner.getAccessFlags(),owner.getSuperclass(),owner.getInterfaces(),owner.getSourceFile(),owner.getAnnotations(),owner.getFields(),methods);
    }
    static int hookCount(Method m) {int n=0;for(Instruction i:code(m))if(i instanceof ReferenceInstruction && SpaceConversionPatch.SEPARATOR_HOOK.equals(F.getReference(((ReferenceInstruction)i).getReference())))n++;return n;}
    static void verifyPatched(Method original, Method patched, int index) {
        List<Instruction> before=code(original), after=code(patched);
        check(patched.getImplementation().getRegisterCount()==SpaceConversionPatch.REGISTERS[index],"Native register count preserved");
        MutableMethod undo=new MutableMethod(patched);
        if(index==0){
            check(after.size()==before.size()+4,"Only original four-instruction guard added");
            check(after.get(0).getOpcode()==Opcode.INVOKE_STATIC_RANGE,"Space helper invoke");
            RegisterRangeInstruction call=(RegisterRangeInstruction)after.get(0);
            check(call.getStartRegister()==10&&call.getRegisterCount()==4,"Real space arguments preserved");
            check(SpaceConversionPatch.HOOK.equals(F.getReference(((ReferenceInstruction)call).getReference())),"Correct space hook descriptor");
            check(after.get(1).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)after.get(1)).getRegisterA()==0,"Guard result local");
            check(after.get(2).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)after.get(2)).getRegisterA()==0&&((OffsetInstruction)after.get(2)).getCodeOffset()==3,"False resumes exact native first instruction");
            check(after.get(3).getOpcode()==Opcode.RETURN_VOID,"True consumes key");
            for(int n=0;n<4;n++)undo.getImplementation().removeInstruction(0);
        }else{
            check(after.size()==before.size(),"Separator rewrite adds no native instructions");
            check(hookCount(patched)==SpaceConversionPatch.SITES[index].length,"Exact separator hook count");
            for(int site=0;site<SpaceConversionPatch.SITES[index].length;site++){
                int at=SpaceConversionPatch.SITES[index][site];Instruction rewritten=after.get(at);
                check(rewritten.getOpcode()==Opcode.INVOKE_STATIC&&rewritten instanceof FiveRegisterInstruction,"Separator read replaced by static helper");
                FiveRegisterInstruction call=(FiveRegisterInstruction)rewritten;
                check(call.getRegisterCount()==1&&call.getRegisterC()==SpaceConversionPatch.RECEIVERS[index][site],"Same candidate register passed");
                check(SpaceConversionPatch.SEPARATOR_HOOK.equals(F.getReference(((ReferenceInstruction)rewritten).getReference())),"Separator ABI exact");
                check(after.get(at+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"Native result destination retained");
                undo.getImplementation().replaceInstruction(at,new MutableMethod(original).getImplementation().getInstructions().get(at));
            }
        }
        check(MethodContract.sha256(undo).equals(MethodContract.sha256(original)),"Inverse restores all native instructions/branches/catches/debug data");
    }
    static final class OldHelperLoader extends ClassLoader {
        private final byte[] bytes;
        OldHelperLoader(byte[] bytes){super(NativeSpaceProbe.class.getClassLoader());this.bytes=bytes;}
        protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            if(!name.equals("app.hiro.swiftkey.patches.SpaceConversionPatch"))return super.loadClass(name,resolve);
            Class<?> c=findLoadedClass(name);if(c==null)c=defineClass(name,bytes,0,bytes.length);if(resolve)resolveClass(c);return c;
        }
    }
    public static void main(String[] args) throws Exception {
        Path fixtures=Path.of(args[0]),output=Path.of(args[1]);Files.createDirectories(output);
        Path originalDex=output.resolve("space-native-fixture.dex");SmaliOptions options=new SmaliOptions();options.apiLevel=26;options.jobs=1;options.outputDexFile=originalDex.toString();
        check(Smali.assemble(options,fixtures.toString()),"Native fixtures assemble");
        Map<String,ClassDef> original=read(originalDex);check(original.size()==6,"Six native owner fixtures");
        for(int i=0;i<SpaceConversionPatch.OWNERS.length;i++)SpaceConversionPatch.inspect(original.get(SpaceConversionPatch.OWNERS[i]),i);
        // Execute the published1831 helper in an isolated loader: it patches P only.
        BytecodePatchContext oldContext=context(original,output.resolve("old-sdk-context"));
        Class<?> oldHelper=new OldHelperLoader(Files.readAllBytes(Path.of(args[2]))).loadClass("app.hiro.swiftkey.patches.SpaceConversionPatch");
        oldHelper.getMethod("apply",BytecodePatchContext.class).invoke(null,oldContext);
        verifyPatched(method(original.get(SpaceConversionPatch.OWNER),0),method(oldContext,0),0);
        for(int i=1;i<SpaceConversionPatch.OWNERS.length;i++)check(MethodContract.sha256(method(oldContext,i)).equals(SpaceConversionPatch.HASHES[i])&&hookCount(method(oldContext,i))==0,"Published1831 leaves commit separator read untouched");
        BytecodePatchContext updated=context(original,output.resolve("new-sdk-context"));
        SpaceConversionPatch.checkOriginal(updated);SpaceConversionPatch.apply(updated);
        int hooks=0;
        for(int i=0;i<SpaceConversionPatch.OWNERS.length;i++){Method before=method(original.get(SpaceConversionPatch.OWNERS[i]),i),after=method(updated,i);verifyPatched(before,after,i);hooks+=hookCount(after);}
        check(hooks==7,"All seven commit separator reads replaced");
        // q has an unrelated a0 getter on a different event branch: it must survive.
        Instruction untouched=code(method(updated,6)).get(96);
        check(untouched.getOpcode()==Opcode.INVOKE_VIRTUAL&&"Lm40/a0;->getTrailingSeparator()Ljava/lang/String;".equals(F.getReference(((ReferenceInstruction)untouched).getReference())),"Unrelated q branch untouched");
        DexPool pool=new DexPool(Opcodes.forApi(26));for(String owner:original.keySet())pool.internClass(updated.mutableClassDefBy(owner));
        Path patchedDex=output.resolve("space-native-patched.dex");FileDataStore store=new FileDataStore(patchedDex.toFile());try{pool.writeTo(store);}finally{store.close();}
        Map<String,ClassDef> serialized=read(patchedDex);check(serialized.keySet().equals(original.keySet()),"Serialized native inventory preserved");
        for(int i=0;i<SpaceConversionPatch.OWNERS.length;i++){Method after=method(serialized.get(SpaceConversionPatch.OWNERS[i]),i);check(MethodContract.sha256(after).equals(MethodContract.sha256(method(updated,i))),"Patched method survives serialization");verifyPatched(method(original.get(SpaceConversionPatch.OWNERS[i]),i),after,i);}
        Map<String,String> beforeDuplicate=hashes(updated);boolean rejected=false;try{SpaceConversionPatch.apply(updated);}catch(IllegalStateException e){rejected=true;}
        check(rejected&&beforeDuplicate.equals(hashes(updated)),"Duplicate apply rejected before any mutation");
        // Drift EACH target (including the final target), verify the complete transaction is unchanged.
        for(int i=0;i<SpaceConversionPatch.OWNERS.length;i++){
            Map<String,ClassDef> drift=new TreeMap<>(original);String owner=SpaceConversionPatch.OWNERS[i];MutableMethod bad=new MutableMethod(method(drift.get(owner),i));bad.getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));drift.put(owner,replace(drift.get(owner),bad));
            BytecodePatchContext c=context(drift,output.resolve("drift"+i));Map<String,String> before=hashes(c);rejected=false;try{SpaceConversionPatch.apply(c);}catch(IllegalStateException e){rejected=true;}
            check(rejected&&before.equals(hashes(c)),"Every target fingerprint drift fails before any mutation");
        }
        Map<String,ClassDef> missing=new TreeMap<>(original);missing.remove("Lma0/o;");BytecodePatchContext missingContext=context(missing,output.resolve("missing"));rejected=false;try{SpaceConversionPatch.apply(missingContext);}catch(IllegalStateException e){rejected=true;}
        check(rejected&&MethodContract.sha256(method(missingContext,0)).equals(SpaceConversionPatch.HASH),"Missing last owner rejected before P mutation");
        String report="{\"status\":\"PASSED\",\"assertions\":"+assertions+",\"native_methods_changed\":7,\"native_instructions_added\":4,\"separator_getters_rewritten\":7,\"separator_methods_changed\":6,\"old_loader_separator_getters_rewritten\":0,\"old_and_new_sdk_apply_executed\":true,\"native_fixture_dex_serialized\":true,\"original_fallback_preserved\":true,\"inverse_native_method_restoration\":true,\"all_target_drift_rejected_before_mutation\":true,\"candidate_getter_bodies_unchanged\":true,\"android_runtime_executed\":false,\"device_tested\":false,\"original_apk_apply_tested\":false,\"scope\":\"Real old/new JVM helper apply in SDK BytecodePatchContext over exact native method fixtures, then DEX serialization; not ART or handset execution. Runtime regression is separate.\"}\n";
        Files.writeString(output.resolve("native-space-report.json"),report);System.out.print(report);
    }
}
