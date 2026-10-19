import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import app.hiro.oneback.patches.InstagramUpgrade;
import app.hiro.oneback.patches.OneBackPatch;

/** Exercises the production upgrade against actual APK classes; no Android behavior claim. */
public final class InstagramUpgradeAudit {
    static final String PREFIX="Lapp/hiro/oneback/runtime/";
    static Map<String,ClassDef> classes(String path)throws Exception {
        Map<String,ClassDef> result=new TreeMap<String,ClassDef>();
        MultiDexContainer<? extends DexFile> container=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
        for(String entry:container.getDexEntryNames())for(ClassDef c:container.getEntry(entry).getDexFile().getClasses())
            check(result.put(c.getType(),c)==null,"duplicate class "+c.getType());
        return result;
    }
    static byte[] canonical(ClassDef c)throws Exception {
        MemoryDataStore out=new MemoryDataStore();
        try{DexPool.writeTo(out,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(c)));return Arrays.copyOf(out.getBuffer(),out.getSize());}
        finally{out.close();}
    }
    static class Edit implements OneBackPatch.Editor {
        final Map<String,ClassDef> input;final Map<String,MutableClass> changed=new TreeMap<String,MutableClass>();
        Edit(Map<String,ClassDef> input){this.input=input;}
        public MutableClass mutable(String type){check(type.startsWith(PREFIX),"upgrade touched native class");MutableClass c=changed.get(type);if(c==null){c=new MutableClass(input.get(type));changed.put(type,c);
            // Exercise the previously materialized member caches, not only an untouched lazy proxy.
            c.getDirectMethods();c.getVirtualMethods();c.getStaticFields();c.getInstanceFields();}return c;}
    }
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static void reject(Map<String,ClassDef> input,String name)throws Exception {
        Edit edit=new Edit(input);boolean rejected=false;
        try{InstagramUpgrade.applyIfKnown("com.instagram.android",input,edit);}catch(IllegalStateException expected){rejected=true;}
        check(rejected,name+" was accepted");check(edit.changed.isEmpty(),name+" mutated before validation");
    }
    static boolean hasDispatcherGate(Map<String,ClassDef> input) {
        for(Method m:input.get("LX/01h;").getMethods())if(m.getName().equals("A04"))
            for(com.android.tools.smali.dexlib2.iface.instruction.Instruction i:m.getImplementation().getInstructions())
                if(i instanceof com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction&&((com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)i).getReference().toString().contains("->tryInstagramDispatcherBack("))return true;
        return false;
    }
    static Map<String,ClassDef> nativeBefore122(Map<String,ClassDef> input) {
        Map<String,ClassDef> result=new TreeMap<String,ClassDef>(input);
        boolean hooked=hasDispatcherGate(input);
        if(hooked)OneBackPatch.verifyInstagramDispatcher(input);
        String[][] expected={
            {"Landroidx/activity/ComponentActivity;","CoN[]LX/01h;","731a76d39402668f9037f83b249a28c32a16fdfb0a5254945b132b65844095fa"},
            {"LX/01h;","A04[]V","0d5cd7a38b8e94f4683e97563a277dcadb1ccc60b9118d58b63049564a230d57"},
            {"LX/01h;","A00[LX/01h;]V","f46a16bcff5ec3bebabafc7113d9dc4ef4d9139b507363262cd0961b1c79558b"},
            {"LX/0Ok;","invoke[Ljava/lang/Object;]Ljava/lang/Object;","4e3930ae7d9224f27341caa413f4f9e5e179bcf24de74980a88ac611cd74618c"}
        };
        int validated=0;
        for(String owner:Arrays.asList("Landroidx/activity/ComponentActivity;","LX/01h;","LX/0Ok;")) {
            ClassDef c=input.get(owner);List<Method> methods=new ArrayList<Method>();
            for(Method m:c.getMethods()) {
                Method value=m;
                for(String[] row:expected)if(row[0].equals(owner)&&row[1].equals(InstagramDispatcherAudit.key(m))) {
                    if(hooked)value=InstagramDispatcherAudit.restored(m);
                    check(row[2].equals(OneBackPatch.instagramDispatcherDigest(value)),"historical native model fingerprint differs: "+owner+"->"+row[1]);
                    validated++;
                }
                methods.add(value);
            }
            if(hooked)result.put(owner,new ImmutableClassDef(owner,c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
        }
        check(validated==4,"historical native model must validate all four dispatcher methods");
        return result;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("APK current-runtime.dex; resources supplied by classpath");
        Map<String,ClassDef> input=classes(args[0]),replacement=classes(args[1]);
        Map<String,ClassDef> merged=new TreeMap<String,ClassDef>(input);
        for(ClassDef c:replacement.values())if(!merged.containsKey(c.getType()))merged.put(c.getType(),c);
        Edit edit=new Edit(merged);
        String previous=InstagramUpgrade.applyVersionIfKnown("com.instagram.android",merged,edit);
        check(previous!=null,"known APK was not upgraded");
        check(edit.changed.keySet().equals(replacement.keySet()),"not all distributed runtime classes replaced");
        for(ClassDef c:replacement.values())check(Arrays.equals(canonical(c),canonical(edit.changed.get(c.getType()))),"distributed runtime mismatch "+c.getType());
        // The historical runtime fixtures need the corresponding historical
        // native ABI. Strip only the five verified 122 gates and fingerprint
        // all four restored bodies before modeling the 120/121 inputs.
        Map<String,ClassDef> historicalNative=nativeBefore122(input);
        Map<String,ClassDef> from121=new TreeMap<String,ClassDef>(historicalNative);
        for(String name:new ArrayList<String>(from121.keySet()))if(name.startsWith(PREFIX))from121.remove(name);
        try(InputStream stream=new BufferedInputStream(InstagramUpgradeAudit.class.getResourceAsStream("/upgrades/oneback-runtime121.dex"))){
            for(ClassDef c:DexBackedDexFile.fromInputStream(Opcodes.getDefault(),stream).getClasses())from121.put(c.getType(),c);
        }
        for(ClassDef c:replacement.values())if(!from121.containsKey(c.getType()))from121.put(c.getType(),c);
        Edit second=new Edit(from121);check(InstagramUpgrade.applyIfKnown("com.instagram.android",from121,second),"known121 model was not upgraded");
        for(ClassDef c:replacement.values())check(Arrays.equals(canonical(c),canonical(second.changed.get(c.getType()))),"121 model replacement differs");
        for(String[]model:new String[][]{{"120","1.1.10"},{"122","1.1.12"}}) {
            Map<String,ClassDef> candidate=new TreeMap<String,ClassDef>("122".equals(model[0])?input:historicalNative);
            for(String name:new ArrayList<String>(candidate.keySet()))if(name.startsWith(PREFIX))candidate.remove(name);
            try(InputStream stream=new BufferedInputStream(InstagramUpgradeAudit.class.getResourceAsStream("/upgrades/oneback-runtime"+model[0]+".dex"))){
                for(ClassDef c:DexBackedDexFile.fromInputStream(Opcodes.getDefault(),stream).getClasses())candidate.put(c.getType(),c);
            }
            for(ClassDef c:replacement.values())if(!candidate.containsKey(c.getType()))candidate.put(c.getType(),c);
            if("122".equals(model[0])) {
                if(!hasDispatcherGate(candidate)){final Map<String,ClassDef> snapshot=candidate;final Map<String,MutableClass> nativeEdits=new TreeMap<String,MutableClass>();OneBackPatch.applyInstagramDispatcher(snapshot,t->{MutableClass c=nativeEdits.get(t);if(c==null){c=new MutableClass(snapshot.get(t));nativeEdits.put(t,c);}return c;},new TreeMap<String,Integer>());candidate.putAll(nativeEdits);}
            }
            Edit known=new Edit(candidate);check(model[1].equals(InstagramUpgrade.applyVersionIfKnown("com.instagram.android",candidate,known)),"wrong prior version returned "+model[0]);
            for(ClassDef c:replacement.values())check(Arrays.equals(canonical(c),canonical(known.changed.get(c.getType()))),model[0]+" model replacement differs");
        }
        Map<String,ClassDef> fresh=new TreeMap<String,ClassDef>(replacement);Edit no=new Edit(fresh);
        check(!InstagramUpgrade.applyIfKnown("com.instagram.android",fresh,no)&&no.changed.isEmpty(),"fresh input must take normal applyAll");
        Map<String,ClassDef> altered=new TreeMap<String,ClassDef>(merged);
        MutableClass change=new MutableClass(altered.get(OneBackPatch.RUNTIME));change.setSourceFile("altered.java");altered.put(change.getType(),change);
        reject(altered,"modified previous runtime");
        Map<String,ClassDef> missing=new TreeMap<String,ClassDef>(merged);missing.remove(OneBackPatch.RUNTIME);reject(missing,"incomplete previous runtime");
        Map<String,ClassDef> extra=new TreeMap<String,ClassDef>(merged);
        ClassDef unknown=new ImmutableClassDef(PREFIX+"Unreviewed;",1,"Ljava/lang/Object;",Collections.<String>emptyList(),null,Collections.<Annotation>emptySet(),Collections.<Field>emptyList(),Collections.<Method>emptyList());
        extra.put(unknown.getType(),unknown);reject(extra,"unknown extra runtime class");
        Map<String,ClassDef> wrongBridge=new TreeMap<String,ClassDef>(merged);
        MutableClass reels=new MutableClass(wrongBridge.get("LX/8Zv;"));
        for(MutableMethod m:reels.getMethods())if(m.getName().equals("onBackPressed")&&m.getParameterTypes().isEmpty())m.getImplementation().removeInstruction(17);
        wrongBridge.put(reels.getType(),reels);reject(wrongBridge,"changed existing native Reels authority bridge");
        Map<String,ClassDef> self=new TreeMap<String,ClassDef>(input);
        for(String name:new ArrayList<String>(self.keySet()))if(name.startsWith(PREFIX))self.remove(name);
        self.putAll(replacement);reject(self,"current123 runtime reapply");
        Edit other=new Edit(merged);check(!InstagramUpgrade.applyIfKnown("com.twitter.android",merged,other)&&other.changed.isEmpty(),"upgrade must be Instagram only");
        System.out.println("PASS InstagramUpgradeAudit: actual supplied APK exact runtime replacement from "+previous+"; "+edit.changed.size()+" canonical classes; reference120/121/122 input models accepted; all non-runtime classes untouched; fresh/other-package no-op; modified/missing/extra/current123 runtime and altered native Reels bridge rejected before mutation");
    }
}
