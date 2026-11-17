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
    public static void main(String[]args)throws Exception{
        String existing="<?xml version=\"1.0\"?><resources>\n<string name=\"native\">\"  Original &amp; %1$s  \"</string>\n</resources>";
        String added="<?xml version=\"1.0\"?><resources>\n<string name=\"hiro_piko_newx_test\" formatted=\"false\">\" of \"</string>\n</resources>";
        byte[] merged=NewXJapanesePatch.mergeStrings(existing.getBytes("UTF-8"),added.getBytes("UTF-8"));
        String mergedText=new String(merged,"UTF-8");
        require(mergedText.startsWith(existing.substring(0,existing.indexOf("</resources>")))&&mergedText.endsWith("</resources>"),"original XML bytes changed");
        require(mergedText.contains("\" of \""),"Android quoted whitespace changed");
        rejected(()->NewXJapanesePatch.mergeStrings(merged,added.getBytes(java.nio.charset.StandardCharsets.UTF_8)),"duplicate resource merge");
        PatchLoader.Jar loader=PatchKt.loadPatchesFromJar(Collections.singleton(new File(args[1])));
        List<Patch> selected=new ArrayList<Patch>();
        for(Patch p:loader)if(p.getName()!=null&&p.getName().startsWith("NewX:")&&p.getDefault())selected.add(p);
        NewXJapanesePatch.validateSelection(selected);
        rejected(()->NewXJapanesePatch.validateSelection(Collections.emptyList()),"no selected Piko");
        rejected(()->NewXJapanesePatch.validateSelection(Collections.singletonList(NewXJapanesePatch.PATCH)),"Hiro only");
        Set<String> wanted=new HashSet<String>(Arrays.asList(NewXJapanesePatch.SETTINGS_HOST,NewXJapanesePatch.SETTINGS_ACTIVITY,NewXJapanesePatch.RESOURCE_UTILS));
        for(NewXJapanesePatch.Target t:NewXJapanesePatch.targets())wanted.add(t.owner);
        Map<String,ClassDef> classes=new TreeMap<String,ClassDef>();
        MultiDexContainer<? extends DexFile> apk=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());
        for(String dex:apk.getDexEntryNames())for(ClassDef c:apk.getEntry(dex).getDexFile().getClasses())if(wanted.contains(c.getType()))require(classes.put(c.getType(),ImmutableClassDef.of(c))==null,"duplicate class");
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
        System.out.println("PASS NewXJapanesePatchAudit: "+NewXJapanesePatch.targets().length+" mappings, "+hooks+" localized literals, "+changed+" scoped methods; "+unchanged+" other methods byte-identical; resource lookup ABI/registers and DEX roundtrip; standard strings.xml merge preserves original bytes/quoted spaces; no-Piko/Hiro-only/missing-extension/repeated-apply rejection");
    }
}
