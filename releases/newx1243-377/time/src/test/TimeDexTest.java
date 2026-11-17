import java.io.*;
import java.util.*;
import java.security.*;
import app.hiro.newx.patches.NewXAbsoluteTimePatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

public class TimeDexTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    static String snapshot(Method m){
        StringBuilder b=new StringBuilder(m.toString());if(m.getImplementation()==null)return b.toString();
        b.append(m.getImplementation().getRegisterCount());
        for(Instruction i:m.getImplementation().getInstructions()){
            b.append('|').append(i.getOpcode()).append(':').append(i.getCodeUnits());
            if(i instanceof ReferenceInstruction)b.append(':').append(((ReferenceInstruction)i).getReference());
            if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;b.append(':').append(r.getRegisterCount()).append(':').append(r.getRegisterC()).append(':').append(r.getRegisterD()).append(':').append(r.getRegisterE()).append(':').append(r.getRegisterF()).append(':').append(r.getRegisterG());}
            else if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;b.append(':').append(r.getStartRegister()).append(':').append(r.getRegisterCount());}
            else if(i instanceof ThreeRegisterInstruction){ThreeRegisterInstruction r=(ThreeRegisterInstruction)i;b.append(':').append(r.getRegisterA()).append(':').append(r.getRegisterB()).append(':').append(r.getRegisterC());}
            else if(i instanceof TwoRegisterInstruction){TwoRegisterInstruction r=(TwoRegisterInstruction)i;b.append(':').append(r.getRegisterA()).append(':').append(r.getRegisterB());}
            else if(i instanceof OneRegisterInstruction)b.append(':').append(((OneRegisterInstruction)i).getRegisterA());
            if(i instanceof WideLiteralInstruction)b.append(':').append(((WideLiteralInstruction)i).getWideLiteral());
            if(i instanceof OffsetInstruction)b.append(':').append(((OffsetInstruction)i).getCodeOffset());
        }return b.toString();
    }
    static class Edit implements NewXAbsoluteTimePatch.Editor {
        final Map<String,ClassDef> classes;
        Edit(Map<String,ClassDef> classes){this.classes=classes;}
        public ClassDef get(String t){return classes.get(t);}
        public MutableClass mutable(String t){ClassDef c=classes.get(t);if(!(c instanceof MutableClass)){c=new MutableClass(c);classes.put(t,c);}return (MutableClass)c;}
        public void add(ClassDef c){check(!classes.containsKey(c.getType()),"collision");classes.put(c.getType(),c);}
    }
    public static void main(String[] args)throws Exception {
        Map<String,ClassDef> classes=new LinkedHashMap<>();
        for(File f:new File(args[0]).listFiles())if(f.getName().endsWith(".dex"))for(ClassDef c:DexFileFactory.loadDexFile(f,Opcodes.getDefault()).getClasses())if(NewXAbsoluteTimePatch.TARGETS.contains(c.getType())||c.getType().startsWith("Lcom/x/dm/"))classes.put(c.getType(),ImmutableClassDef.of(c));
        Map<String,String> before=new HashMap<>();for(ClassDef c:classes.values())for(Method m:c.getMethods())before.put(m.toString(),snapshot(m));
        Edit e=new Edit(classes);NewXAbsoluteTimePatch.apply(e);
        Set<String> allowed=new HashSet<>();
        for(String t:Arrays.asList(NewXAbsoluteTimePatch.POST,NewXAbsoluteTimePatch.DETAIL,NewXAbsoluteTimePatch.ACCESS))for(Method m:e.get(t).getMethods())if((t.equals(NewXAbsoluteTimePatch.POST)&&m.getName().equals("invoke"))||(t.equals(NewXAbsoluteTimePatch.DETAIL)&&(m.getName().equals("b")||m.getName().equals("g")))||(t.equals(NewXAbsoluteTimePatch.ACCESS)&&m.getName().equals("a")))allowed.add(m.toString());
        int unchanged=0,changed=0;for(ClassDef c:classes.values())for(Method m:c.getMethods())if(before.containsKey(m.toString())){if(allowed.contains(m.toString())){check(!before.get(m.toString()).equals(snapshot(m)),"expected hook missing");changed++;}else{check(before.get(m.toString()).equals(snapshot(m)),"unrelated method changed: "+m);unchanged++;}}
        check(changed==4,"original methods changed "+changed);
        ClassDef clone=e.get(NewXAbsoluteTimePatch.CLONE);check(clone!=null,"restart clone missing");
        int hooks=0,restarts=0;for(ClassDef c:classes.values())for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)i).getReference();if(r instanceof MethodReference){MethodReference call=(MethodReference)r;if(call.getDefiningClass().equals(NewXAbsoluteTimePatch.RUNTIME))hooks++;if(c.getType().equals(NewXAbsoluteTimePatch.CLONE)&&call.getName().equals(NewXAbsoluteTimePatch.METHOD))restarts++;}}
        check(hooks==4&&restarts==1,"runtime/restart counts "+hooks+"/"+restarts);
        boolean rejected=false;try{NewXAbsoluteTimePatch.apply(e);}catch(IllegalStateException expected){rejected=true;}check(rejected,"repeat patch not rejected");
        List<ClassDef> output=new ArrayList<>();for(String t:NewXAbsoluteTimePatch.TARGETS)output.add(e.get(t));output.add(clone);
        DexPool.writeTo(args[1],new ImmutableDexFile(Opcodes.getDefault(),output));
        check(DexFileFactory.loadDexFile(new File(args[1]),Opcodes.getDefault()).getClasses().size()==8,"roundtrip dex");
        System.out.println("PASS: 4 post-only methods redirected, 4 formatting hooks, Compose restart clone, "+unchanged+" original methods unchanged (including DM and shared utility/byline), dex roundtrip and repeated-apply rejection");
    }
}
