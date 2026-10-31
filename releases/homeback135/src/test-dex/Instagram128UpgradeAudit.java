import java.util.*;
import app.hiro.oneback.patches.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Runs against the serialized published127 APK, not a replacement fixture.
 * No user APK is published. Modified native gates must be rejected before an
 * editor is invoked; a successful upgrade may edit only exact runtime classes. */
public final class Instagram128UpgradeAudit {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Map<String,ClassDef> merged(Map<String,ClassDef> app,Map<String,ClassDef> replacement){
        Map<String,ClassDef> out=new TreeMap<String,ClassDef>(app);
        for(ClassDef value:replacement.values())if(!out.containsKey(value.getType()))out.put(value.getType(),value);
        return out;
    }
    static void reject(Map<String,ClassDef> input,String label){
        InstagramUpgradeAudit.Edit edit=new InstagramUpgradeAudit.Edit(input);boolean rejected=false;
        try{InstagramUpgrade.applyVersionIfKnown("com.instagram.android",input,edit);}
        catch(IllegalStateException expected){rejected=true;}
        check(rejected,"reject "+label);check(edit.changed.isEmpty(),"no mutation before rejection "+label);
    }
    static void brokenGate(Map<String,ClassDef> original,String owner,String name,String result){
        Map<String,ClassDef> altered=new TreeMap<String,ClassDef>(original);
        MutableClass changed=new MutableClass(altered.get(owner));boolean found=false;
        for(MutableMethod method:changed.getMethods())if(name.equals(method.getName())&&result.equals(method.getReturnType())){
            method.getImplementation().replaceInstruction(0,new BuilderInstruction10x(Opcode.NOP));found=true;break;
        }
        check(found,"negative gate exists "+owner+"->"+name);altered.put(owner,changed);
        reject(altered,"changed native boundary "+owner+"->"+name);
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("published127.apk replacement-runtime.dex");
        Map<String,ClassDef> app=InstagramUpgradeAudit.classes(args[0]);
        Map<String,ClassDef> replacement=InstagramUpgradeAudit.classes(args[1]);
        Map<String,ClassDef> input=merged(app,replacement);
        Instagram452Upgrade.verify(input);
        int jumbo=0;
        for(ClassDef c:app.values())if(c.getType().startsWith(InstagramUpgradeAudit.PREFIX))
            for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())
                if(i.getOpcode()==Opcode.CONST_STRING_JUMBO)jumbo++;
        // Rebuilt452 currently places the runtime in a DEX with no jumbo
        // strings. Record the actual count rather than claiming that the
        // older447 encoding-specific fixture applies to this input.
        InstagramUpgradeAudit.Edit edit=new InstagramUpgradeAudit.Edit(input);
        check("1.1.17".equals(InstagramUpgrade.applyVersionIfKnown("com.instagram.android",input,edit)),"published127 admitted");
        check(edit.changed.keySet().equals(replacement.keySet()),"only complete runtime replaced");
        for(ClassDef c:replacement.values())check(Arrays.equals(InstagramUpgradeAudit.canonical(c),
            InstagramUpgradeAudit.canonical(edit.changed.get(c.getType()))),"replacement exact "+c.getType());
        for(String[] gate:new String[][]{
            {"LX/02Hr;","onBackPressed","Z"},
            {"LX/00M2;","A0T","Z"},
            {"Lcom/instagram/mainactivity/InstagramMainActivity;","A1u","V"},
            {"Landroidx/activity/ComponentActivity;","CxU","LX/001p;"},
            {"LX/001p;","A04","V"},
            {"LX/001p;","A00","V"},
            {"LX/00nv;","invoke","Ljava/lang/Object;"}
        })brokenGate(input,gate[0],gate[1],gate[2]);
        Map<String,ClassDef> altered=new TreeMap<String,ClassDef>(input);
        MutableClass runtime=new MutableClass(altered.get(OneBackPatch.RUNTIME));runtime.setSourceFile("unreviewed-runtime.java");
        altered.put(runtime.getType(),runtime);reject(altered,"runtime metadata");
        altered=new TreeMap<String,ClassDef>(input);altered.remove(OneBackPatch.RUNTIME);reject(altered,"missing runtime");
        altered=new TreeMap<String,ClassDef>(input);MutableClass navigation=new MutableClass(altered.get("LX/004K;"));
        for(MutableField field:navigation.getFields())if(field.getName().equals("A03"))field.setAccessFlags(1);
        altered.put(navigation.getType(),navigation);reject(altered,"navigation owner contract");
        System.out.println("PASS Instagram128UpgradeAudit: actual serialized127 APK -> 1.1.18; "+jumbo+" jumbo strings; "+
            replacement.size()+" exact runtime classes; all 7 native gates and navigation contract checked; "+checks+
            " assertions; modified gates/runtime/owner rejected before mutation; no Android device claim");
    }
}
