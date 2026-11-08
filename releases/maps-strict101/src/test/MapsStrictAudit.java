import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.security.MessageDigest;
import app.hiro.mapsstrict.patches.MapsStrictSearchPatch;
import app.hiro.mapsstrict.patches.UpgradeContracts;
import app.hiro.mapsstrict.patches.RuntimeContracts;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Real-APK structural audit; does not assert Android UI execution. */
public final class MapsStrictAudit {
    static long checks;
    static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    static Map<String,ClassDef> read(String path) throws Exception {
        Map<String,ClassDef> out = new TreeMap<String,ClassDef>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String name : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(name).getDexFile().getClasses())
            check(out.put(c.getType(), c) == null, "Duplicate class " + c.getType());
        return out;
    }
    static MapsStrictSearchPatch.Lookup lookup(final Map<String,ClassDef> all) {
        return new MapsStrictSearchPatch.Lookup() { public ClassDef get(String owner) { return all.get(owner); }};
    }
    static byte[] canonical(ClassDef c) throws Exception {
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(c)));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally { store.close(); }
    }
    static Method method(ClassDef c, String descriptor) { return MapsStrictSearchPatch.find(c, descriptor); }
    static String desc(MethodReference m) { return MapsStrictSearchPatch.descriptor(m); }
    static List<Instruction> instructions(Method m) {
        List<Instruction> out = new ArrayList<Instruction>();
        if (m.getImplementation() != null) for (Instruction i : m.getImplementation().getInstructions()) out.add(i);
        return out;
    }
    static void rejected(Runnable action, String why) {
        boolean failed = false;
        try { action.run(); } catch (IllegalStateException expected) { failed = true; }
        check(failed, why);
    }
    static boolean dexOrSignature(String name) {
        return name.matches("classes(?:[0-9]+)?\\.dex") || name.matches("(?i)META-INF/(?:MANIFEST\\.MF|[^/]+\\.(?:SF|RSA|DSA|EC))");
    }
    static int readChunk(InputStream in, byte[] buf) throws IOException {
        int total = 0, count;
        while (total < buf.length && (count = in.read(buf, total, buf.length - total)) != -1) total += count;
        return total;
    }
    static String fileSha(String path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(path)) {
            byte[] buffer = new byte[65536]; int n;
            while ((n = in.read(buffer)) >= 0) if (n != 0) digest.update(buffer, 0, n);
        }
        StringBuilder result = new StringBuilder();
        for (byte b : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }
    static boolean sameBytes(ZipFile a, ZipEntry x, ZipFile b, ZipEntry y) throws Exception {
        if (x == null || y == null || x.getSize() != y.getSize()) return false;
        byte[] xx = new byte[65536], yy = new byte[65536];
        try (InputStream ax = a.getInputStream(x); InputStream by = b.getInputStream(y)) {
            int ac, bc;
            while ((ac = readChunk(ax, xx)) > 0) {
                bc = readChunk(by, yy);
                if (ac != bc || !Arrays.equals(Arrays.copyOf(xx, ac), Arrays.copyOf(yy, bc))) return false;
            }
            return by.read() == -1;
        }
    }
    static Set<String> bytePreservedClasses(String original, String patched) throws Exception {
        Set<String> result = new HashSet<String>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(original), Opcodes.getDefault());
        try (ZipFile a = new ZipFile(original); ZipFile b = new ZipFile(patched)) {
            for (String name : dex.getDexEntryNames()) if (sameBytes(a, a.getEntry(name), b, b.getEntry(name)))
                for (ClassDef c : dex.getEntry(name).getDexFile().getClasses()) result.add(c.getType());
        }
        return result;
    }
    static int auditResources(String original, String patched) throws Exception {
        int count = 0;
        try (ZipFile a = new ZipFile(original); ZipFile b = new ZipFile(patched)) {
            Set<String> before = new TreeSet<String>(), after = new TreeSet<String>();
            Enumeration<? extends ZipEntry> aa = a.entries(), bb = b.entries();
            while (aa.hasMoreElements()) {
                ZipEntry e = aa.nextElement(); if (e.isDirectory() || dexOrSignature(e.getName())) continue;
                before.add(e.getName()); check(sameBytes(a, e, b, b.getEntry(e.getName())), "Resource changed " + e.getName()); count++;
            }
            while (bb.hasMoreElements()) {
                ZipEntry e = bb.nextElement(); if (!e.isDirectory() && !dexOrSignature(e.getName())) after.add(e.getName());
            }
            check(before.equals(after), "Non-DEX entry inventory changed");
        }
        return count;
    }

    static final String PREFIX = "Lapp/hiro/mapsstrict/patches/";
    static final String RUNTIME_PREFIX = "Lapp/hiro/mapsstrict/";
    static void guards(String apk) throws Exception {
        Map<String,ClassDef> nativeClasses=read(apk);
        MapsStrictSearchPatch.validateNative(lookup(nativeClasses));
        for (String pkg : MapsStrictSearchPatch.PACKAGES) {
            MapsStrictSearchPatch.validatePackage(pkg,MapsStrictSearchPatch.VERSION,MapsStrictSearchPatch.CODE);
            rejected(() -> MapsStrictSearchPatch.validatePackage(pkg,"different",MapsStrictSearchPatch.CODE),"Wrong version accepted");
            rejected(() -> MapsStrictSearchPatch.validatePackage(pkg,MapsStrictSearchPatch.VERSION,"different"),"Wrong code accepted");
        }
        rejected(() -> MapsStrictSearchPatch.validatePackage("other",MapsStrictSearchPatch.VERSION,MapsStrictSearchPatch.CODE),"Wrong package accepted");
        ClassDef original=nativeClasses.get(MapsStrictSearchPatch.OWNER);
        ClassDef originalModel=nativeClasses.get(MapsStrictSearchPatch.MODEL);
        MutableClass patched=new MutableClass(original);
        MutableClass patchedModel=new MutableClass(originalModel);
        MapsStrictSearchPatch.apply(patched,patchedModel);
        auditHook(original,patched);
        auditModel(originalModel,patchedModel);
        rejected(() -> MapsStrictSearchPatch.apply(patched,patchedModel),"Repeated application accepted");
        MutableClass drift=new MutableClass(original);
        MutableClass untouchedModel=new MutableClass(originalModel);
        ((MutableMethod)method(drift,MapsStrictSearchPatch.TARGET)).getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));
        byte[] snapshot=canonical(drift);
        rejected(() -> MapsStrictSearchPatch.apply(drift,untouchedModel),"Unknown native body accepted");
        check(Arrays.equals(snapshot,canonical(drift)),"Rejected patch partially changed original");
        check(Arrays.equals(canonical(originalModel),canonical(untouchedModel)),"Rejected patch changed model");
        MutableClass untouchedRequest=new MutableClass(original);
        MutableClass modelDrift=new MutableClass(originalModel);
        ((MutableMethod)method(modelDrift,app.hiro.mapsstrict.patches.NativeContracts.MERGE)).getImplementation()
            .addInstruction(0,new BuilderInstruction10x(Opcode.NOP));
        byte[] modelSnapshot=canonical(modelDrift);
        byte[] requestSnapshot=canonical(untouchedRequest);
        rejected(() -> MapsStrictSearchPatch.apply(untouchedRequest,modelDrift),"Unknown merge body accepted");
        check(Arrays.equals(requestSnapshot,canonical(untouchedRequest))&&Arrays.equals(modelSnapshot,canonical(modelDrift)),
            "Rejected merge contract partially modified classes");
        System.out.println("PASS native contracts: actual request-response model, unsupported version/package/code, unknown body and duplicate patch rejection; assertions="+checks);
    }
    static void auditHook(ClassDef before,ClassDef after) throws Exception {
        MutableClass expected=new MutableClass(before);
        Method originalMethod=method(before,MapsStrictSearchPatch.TARGET);
        int at=MapsStrictSearchPatch.insertionIndex(originalMethod);
        List<Instruction> code=instructions(method(after,MapsStrictSearchPatch.TARGET));
        check(code.size()==instructions(originalMethod).size()+5,"Unexpected hook instruction count");
        Instruction query=code.get(at), model=code.get(at+1), call=code.get(at+2), result=code.get(at+3), cast=code.get(at+4);
        check(query.getOpcode()==Opcode.IGET_OBJECT && ((TwoRegisterInstruction)query).getRegisterA()==1 && ((TwoRegisterInstruction)query).getRegisterB()==0
            && ((ReferenceInstruction)query).getReference().toString().equals("Lcqgi;->e:Ljava/lang/String;"),"Query is not decoded request.e");
        check(model.getOpcode()==Opcode.IGET_OBJECT && ((TwoRegisterInstruction)model).getRegisterA()==2
            && ((TwoRegisterInstruction)model).getRegisterB()==9
            && ((ReferenceInstruction)model).getReference().toString().equals("Lavds;->g:Lavdv;"),"Model is not request-owned");
        check(call.getOpcode()==Opcode.INVOKE_STATIC && ((FiveRegisterInstruction)call).getRegisterCount()==3
            && ((FiveRegisterInstruction)call).getRegisterC()==10 && ((FiveRegisterInstruction)call).getRegisterD()==1
            && ((FiveRegisterInstruction)call).getRegisterE()==2
            && desc((MethodReference)((ReferenceInstruction)call).getReference()).equals(MapsStrictSearchPatch.FILTER),"Response/query pairing changed");
        check(result.getOpcode()==Opcode.MOVE_RESULT_OBJECT && ((OneRegisterInstruction)result).getRegisterA()==10,"Response not replaced");
        check(cast.getOpcode()==Opcode.CHECK_CAST && ((OneRegisterInstruction)cast).getRegisterA()==10
            && ((ReferenceInstruction)cast).getReference().toString().equals("Lcqgm;"),"Response cast missing");
        MapsStrictSearchPatch.applyRequest(expected);
        check(Arrays.equals(canonical(expected),canonical(after)),"Native request changed beyond five scoped instructions");
    }
    static void auditModel(ClassDef before,ClassDef after) throws Exception {
        String target=app.hiro.mapsstrict.patches.NativeContracts.MERGE;
        Method originalMethod=method(before,target);
        int at=MapsStrictSearchPatch.mergeInsertionIndex(originalMethod);
        List<Instruction> code=instructions(method(after,target));
        check(code.size()==instructions(originalMethod).size()+1,"Unexpected merge hook count");
        Instruction call=code.get(at);
        check(call.getOpcode()==Opcode.INVOKE_STATIC && ((FiveRegisterInstruction)call).getRegisterCount()==2
            && ((FiveRegisterInstruction)call).getRegisterC()==9 && ((FiveRegisterInstruction)call).getRegisterD()==10
            && desc((MethodReference)((ReferenceInstruction)call).getReference()).equals(MapsStrictSearchPatch.RECONCILE),
            "Accepted merge does not reconcile receiver/incoming models");
        int markerCount=0;
        for(Field field:after.getFields()) if(field.getName().equals(MapsStrictSearchPatch.MARKER)) {
            markerCount++;
            check(field.getType().equals("Ljava/lang/Object;")&&field.getAccessFlags()==MapsStrictSearchPatch.MARKER_FLAGS
                &&field.getInitialValue()==null,"Marker is not private transient synthetic Object");
        }
        check(markerCount==1,"Success marker missing or duplicated");
        MutableClass expected=new MutableClass(before);
        MapsStrictSearchPatch.applyModel(expected);
        check(Arrays.equals(canonical(expected),canonical(after)),"Native model changed beyond scoped merge hook and transient marker");
    }
    static void merge(String basePath,String patchPath,String outPath) throws Exception {
        Map<String,ClassDef> base=read(basePath),addition=read(patchPath),merged=new TreeMap<String,ClassDef>();
        int preserved=0,replaced=0;
        for(ClassDef c:base.values())if(!c.getType().startsWith(PREFIX)){merged.put(c.getType(),c);preserved++;}else replaced++;
        check(!addition.isEmpty(),"Empty patch DEX");
        check(replaced>0,"Published strict-search provider missing from baseline");
        for(ClassDef c:addition.values()) {
            check(c.getType().startsWith(PREFIX),"Loader class outside strict-search package");
            check(merged.put(c.getType(),c)==null,"Loader class collision");
        }
        DexPool.writeTo(outPath,new ImmutableDexFile(Opcodes.getDefault(),merged.values()));
        Map<String,ClassDef> output=read(outPath);
        check(output.keySet().equals(merged.keySet()),"Merged loader inventory differs");
        for(ClassDef c:merged.values())check(Arrays.equals(canonical(c),canonical(output.get(c.getType()))),"Merged class changed "+c.getType());
        System.out.println("PASS scoped loader replacement: unrelated provider classes preserved="+preserved+"; old strict classes replaced="+replaced+"; current strict classes="+addition.size());
    }
    static void upgradeGuards(String apk) throws Exception {
        Map<String,ClassDef> input=read(apk);
        UpgradeContracts.validateLegacyRuntime(lookup(input),input.values());
        UpgradeContracts.validateInstalledNative(lookup(input));
        Map<String,ClassDef> changed=new TreeMap<String,ClassDef>(input);
        MutableClass altered=new MutableClass(changed.get(MapsStrictSearchPatch.RUNTIME));
        MutableMethod filter=(MutableMethod)method(altered,MapsStrictSearchPatch.FILTER);
        filter.getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));
        changed.put(altered.getType(),altered);
        rejected(()->UpgradeContracts.validateLegacyRuntime(lookup(changed),changed.values()),"Unknown installed runtime accepted");
        Map<String,ClassDef> missing=new TreeMap<String,ClassDef>(input);
        missing.remove(MapsStrictSearchPatch.RUNTIME);
        rejected(()->UpgradeContracts.validateLegacyRuntime(lookup(missing),missing.values()),"Partial installation accepted");
        System.out.println("PASS verified v1.0.0 upgrade: original native bodies recovered from scoped hook copies; exact runtime required; unknown and partial installations rejected");
    }
    static void audit(String original,String patched,String extension,boolean upgrade) throws Exception {
        Map<String,ClassDef> before=read(original),after=read(patched),runtime=read(extension);
        check(runtime.containsKey(MapsStrictSearchPatch.RUNTIME),"Missing production filter");
        Set<String> expected=new TreeSet<String>(before.keySet());
        for(ClassDef c:runtime.values()) {
            check(c.getType().startsWith(RUNTIME_PREFIX)&&!c.getType().startsWith(PREFIX),"Unexpected extension class");
            if(upgrade)check(before.containsKey(c.getType())||expected.add(c.getType()),"Runtime inventory error");
            else check(expected.add(c.getType()),"Runtime collision");
            check(after.containsKey(c.getType())&&Arrays.equals(canonical(c),canonical(after.get(c.getType()))),"Runtime differs "+c.getType());
        }
        check(expected.equals(after.keySet()),"Class inventory differs");
        if(upgrade){
            UpgradeContracts.validateLegacyRuntime(lookup(before),before.values());
            UpgradeContracts.validateInstalledNative(lookup(before));
            UpgradeContracts.validateInstalledNative(lookup(after));
            rejected(()->UpgradeContracts.validateLegacyRuntime(lookup(after),after.values()),"Current v1.0.1 incorrectly accepted as old runtime");
        }
        Set<String> unchanged=bytePreservedClasses(original,patched);
        int count=0;
        for(ClassDef c:before.values()) {
            if(upgrade&&c.getType().startsWith(RUNTIME_PREFIX))check(runtime.containsKey(c.getType()),"Obsolete runtime helper retained");
            else if(!upgrade&&c.getType().equals(MapsStrictSearchPatch.OWNER))auditHook(c,after.get(c.getType()));
            else if(!upgrade&&c.getType().equals(MapsStrictSearchPatch.MODEL))auditModel(c,after.get(c.getType()));
            else if(!unchanged.contains(c.getType()))check(Arrays.equals(canonical(c),canonical(after.get(c.getType()))),"Unrelated class changed "+c.getType());
            count++;
        }
        int resources=auditResources(original,patched);
        System.out.println("PASS APK scope: original classes="+count+(upgrade?"; verified v1.0.0 upgrade; existing native hooks unchanged; verified old runtime replaced":"; modified native owners=Lavds,Lavdv; hook instructions=5+1; private transient success marker=1")+"; unchanged non-DEX resources="+resources+"; runtime classes="+runtime.size()+"; Android UI not executed");
    }
    public static void main(String[] args) throws Exception {
        if(args[0].equals("guards"))guards(args[1]);
        else if(args[0].equals("upgrade-guards"))upgradeGuards(args[1]);
        else if(args[0].equals("merge"))merge(args[1],args[2],args[3]);
        else if(args[0].equals("audit"))audit(args[1],args[2],args[3],false);
        else if(args[0].equals("audit-upgrade"))audit(args[1],args[2],args[3],true);
        else throw new IllegalArgumentException("Unknown audit mode");
    }
}
