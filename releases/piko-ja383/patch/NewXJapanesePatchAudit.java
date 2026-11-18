import java.io.*;
import java.util.*;
import app.hiro.newx.patches.NewXJapanesePatch;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;

public final class NewXJapanesePatchAudit {
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static byte[] canonical(Method m)throws Exception{
        ClassDef c=new ImmutableClassDef(m.getDefiningClass(),1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.singleton(m));
        MemoryDataStore out=new MemoryDataStore();try{DexPool.writeTo(out,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(c)));return Arrays.copyOf(out.getBuffer(),out.getSize());}finally{out.close();}
    }
    static final class Editor implements NewXJapanesePatch.Editor {
        final Map<String,ClassDef> classes;
        Editor(Map<String,ClassDef> classes){this.classes=classes;}
        public ClassDef get(String type){return classes.get(type);}
        public MutableClass mutable(String type){ClassDef c=classes.get(type);if(!(c instanceof MutableClass)){c=new MutableClass(c);classes.put(type,c);}return (MutableClass)c;}
    }
    static void rejected(Runnable r,String label){try{r.run();throw new AssertionError("Not rejected: "+label);}catch(IllegalStateException expected){}}
    static void rejected(Runnable r,String label,String reason){
        try{r.run();throw new AssertionError("Not rejected: "+label);}
        catch(IllegalStateException expected){require(expected.getMessage().contains(reason),"Wrong rejection for "+label+": "+expected.getMessage());}
    }
    static void visit(Patch patch,Set<Patch> seen){if(seen.add(patch))for(Patch dependency:patch.getDependencies())visit(dependency,seen);}
    static List<Patch> graph(List<Patch> selected){
        Set<Patch> seen=Collections.newSetFromMap(new IdentityHashMap<Patch,Boolean>());
        for(Patch patch:selected)visit(patch,seen);return new ArrayList<Patch>(seen);
    }
    static void withBundle(List<Patch> patches,PatchBundleInfo bundle,Runnable check){
        Map<Patch,PatchBundleInfo> originals=new IdentityHashMap<Patch,PatchBundleInfo>();
        try{for(Patch patch:patches){originals.put(patch,patch.getBundle$morphe_patcher());patch.setBundle$morphe_patcher(bundle);}check.run();}
        finally{for(Map.Entry<Patch,PatchBundleInfo> entry:originals.entrySet())entry.getKey().setBundle$morphe_patcher(entry.getValue());}
    }
    static void selectionAudit(List<Patch> selected){
        NewXJapanesePatch.validateSelection(selected);
        List<Patch> patches=graph(selected);Patch settings=null;
        for(Patch patch:patches)if("SettingsPatch.newXSettingsPatch".equals(patch.getDeclaredAs$morphe_patcher()))settings=patch;
        require(settings!=null,"Missing real NewX settings dependency");
        PatchBundleInfo original=settings.getBundle$morphe_patcher();
        withBundle(patches,original.copy(original.getFile(),"Piko","3.12.0-dev.99",original.getPatcherVersion(),original.getRetryingFiltersCanChangeMatches()),
            ()->rejected(()->NewXJapanesePatch.validateSelection(selected),"unsupported Piko version","未検証のPikoソースです"));
        withBundle(patches,original.copy(original.getFile(),"Other source",original.getVersion(),original.getPatcherVersion(),original.getRetryingFiltersCanChangeMatches()),
            ()->rejected(()->NewXJapanesePatch.validateSelection(selected),"wrong source name","未検証のPikoソースです"));
        List<Patch> singleton=Collections.singletonList(settings);
        withBundle(singleton,null,()->rejected(()->NewXJapanesePatch.validateSelection(selected),"missing source metadata","未検証のPikoソースです"));
        withBundle(singleton,original.copy(new File(original.getFile().getParentFile(),"different-piko-source.mpp"),original.getName(),original.getVersion(),original.getPatcherVersion(),original.getRetryingFiltersCanChangeMatches()),
            ()->rejected(()->NewXJapanesePatch.validateSelection(selected),"mixed source identity","複数のPikoソース"));
        NewXJapanesePatch.validateSelection(selected);
    }
    public static void main(String[]args)throws Exception{
        String existing="<?xml version=\"1.0\"?><resources>\n<string name=\"native\">\"  Original &amp; %1$s  \"</string>\n</resources>";
        String added="<?xml version=\"1.0\"?><resources>\n<string name=\"hiro_piko_newx_test\" formatted=\"false\">\" of \"</string>\n</resources>";
        byte[] merged=NewXJapanesePatch.mergeStrings(existing.getBytes("UTF-8"),added.getBytes("UTF-8"));
        String mergedText=new String(merged,"UTF-8");
        require(mergedText.startsWith(existing.substring(0,existing.indexOf("</resources>")))&&mergedText.endsWith("</resources>"),"original XML bytes changed");
        require(mergedText.contains("\" of \""),"Android quoted whitespace changed");
        rejected(()->NewXJapanesePatch.mergeStrings(merged,added.getBytes(java.nio.charset.StandardCharsets.UTF_8)),"duplicate resource merge");
        int bundles=0;
        for(String path:args[1].split(java.util.regex.Pattern.quote(File.pathSeparator))){
            PatchLoader.Jar loader=PatchKt.loadPatchesFromJar(Collections.singleton(new File(path)));
            List<Patch> selected=new ArrayList<Patch>();
            for(Patch p:loader)if(p.getName()!=null&&p.getName().startsWith("NewX:")&&p.getDefault())selected.add(p);
            require(!selected.isEmpty(),"No default NewX patches loaded from "+path);
            selectionAudit(selected);bundles++;
        }
        rejected(()->NewXJapanesePatch.validateSelection(Collections.emptyList()),"no selected Piko");
        rejected(()->NewXJapanesePatch.validateSelection(Collections.singletonList(NewXJapanesePatch.PATCH)),"Hiro only");
        Set<String> wanted=new HashSet<String>(Arrays.asList(NewXJapanesePatch.SETTINGS_HOST,NewXJapanesePatch.SETTINGS_ACTIVITY,NewXJapanesePatch.RESOURCE_UTILS));
        for(NewXJapanesePatch.Target t:NewXJapanesePatch.targets())wanted.add(t.owner);
        Map<String,ClassDef> classes=new TreeMap<String,ClassDef>();
        for(String path:args[0].split(java.util.regex.Pattern.quote(File.pathSeparator))){
            MultiDexContainer<? extends DexFile> apk=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
            for(String dex:apk.getDexEntryNames())for(ClassDef c:apk.getEntry(dex).getDexFile().getClasses())if(wanted.contains(c.getType()))require(classes.put(c.getType(),ImmutableClassDef.of(c))==null,"duplicate class");
        }
        require(classes.keySet().equals(wanted),"Missing target classes "+wanted.size()+"/"+classes.size());
        rejected(()->NewXJapanesePatch.prepare(new Editor(Collections.emptyMap())),"missing Piko bytecode");
        Editor editor=new Editor(classes);NewXJapanesePatch.Plan plan=NewXJapanesePatch.prepare(editor);
        Map<String,byte[]> before=new TreeMap<String,byte[]>();for(ClassDef c:classes.values())for(Method m:c.getMethods())before.put(m.toString(),canonical(m));
        NewXJapanesePatch.apply(editor,plan);
        int unchanged=0,changed=0,hooks=0;
        for(ClassDef c:classes.values())for(Method m:c.getMethods()){
            boolean same=Arrays.equals(before.get(m.toString()),canonical(m));
            if(plan.methods.contains(m.toString())){require(!same,"target method unchanged");changed++;}else{require(same,"unrelated method changed: "+m);unchanged++;}
            if(m.getImplementation()==null)continue;
            List<Instruction> code=new ArrayList<Instruction>();for(Instruction i:m.getImplementation().getInstructions())code.add(i);
            for(int i=0;i<code.size();i++){
                Instruction ins=code.get(i);if(!(ins instanceof ReferenceInstruction))continue;
                Reference ref=((ReferenceInstruction)ins).getReference();
                if(!(ref instanceof StringReference)||!((StringReference)ref).getString().startsWith("hiro_piko_newx_"))continue;
                require(ins.getOpcode()==Opcode.CONST_STRING||ins.getOpcode()==Opcode.CONST_STRING_JUMBO,"resource literal opcode");
                int register=((OneRegisterInstruction)ins).getRegisterA();
                Instruction call=code.get(i+1),move=code.get(i+2);
                require(call.getOpcode()==Opcode.INVOKE_STATIC_RANGE&&((RegisterRangeInstruction)call).getStartRegister()==register&&((RegisterRangeInstruction)call).getRegisterCount()==1,"resource call registers");
                require(((ReferenceInstruction)call).getReference().toString().equals(NewXJapanesePatch.RESOURCE_UTILS+"->getStringOrThrow(Ljava/lang/String;)Ljava/lang/String;"),"lookup signature");
                require(move.getOpcode()==Opcode.MOVE_RESULT_OBJECT&&((OneRegisterInstruction)move).getRegisterA()==register,"resource result register");hooks++;
            }
        }
        require(changed==plan.methods.size()&&hooks==plan.edits.size(),"changed methods/hooks differ");
        rejected(()->NewXJapanesePatch.prepare(editor),"repeat application");
        DexPool.writeTo(args[2],new ImmutableDexFile(Opcodes.getDefault(),classes.values()));
        Set<? extends ClassDef> serialized=DexFileFactory.loadDexFile(new File(args[2]),Opcodes.getDefault()).getClasses();
        require(serialized.size()==classes.size(),"serialized class count differs");
        System.out.println("PASS NewXJapanesePatchAudit: "+bundles+" official Piko dependency graphs accepted; unsupported-version/wrong-name/missing-metadata/mixed-source rejection for every bundle; "+NewXJapanesePatch.targets().length+" mappings, "+hooks+" localized literals, "+changed+" scoped methods; "+unchanged+" other methods byte-identical; resource lookup ABI/registers and DEX roundtrip; standard strings.xml merge preserves original bytes/quoted spaces; no-Piko/Hiro-only/missing-extension/repeated-apply rejection");
    }
}
