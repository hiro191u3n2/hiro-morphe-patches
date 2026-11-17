import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.hiro.newxui.patches.PinBarsPatch;

public final class PinBarsDexTest {
    static void require(boolean v,String why){if(!v)throw new AssertionError(why);}
    static Method matching(ClassDef c,Method original){for(Method m:c.getMethods())if(m.toString().equals(original.toString()))return m;throw new AssertionError("Missing method "+original);}
    public static void main(String[] args)throws Exception {
        Map<String,ClassDef> all=new HashMap<String,ClassDef>();
        for(File f:new File(args[0]).listFiles())if(f.getName().endsWith(".dex"))for(ClassDef c:DexFileFactory.loadDexFile(f,Opcodes.getDefault()).getClasses())all.put(c.getType(),c);
        ClassDef top=all.get(PinBarsPatch.TOP),bottom=all.get(PinBarsPatch.BOTTOM);
        ClassDef behavior=all.get("Landroidx/compose/material3/x4;"),state=all.get("Landroidx/compose/material3/rf;");
        require(behavior!=null&&state!=null&&AccessFlags.PUBLIC.isSet(behavior.getAccessFlags())&&AccessFlags.PUBLIC.isSet(state.getAccessFlags()),"native header behavior/state are accessible");
        boolean field=false,setter=false;
        for(Field f:behavior.getFields())if(f.getName().equals("a")&&f.getType().equals(state.getType())&&AccessFlags.PUBLIC.isSet(f.getAccessFlags()))field=true;
        for(Method m:state.getMethods())if(m.getName().equals("b")&&m.getReturnType().equals("V")&&m.getParameterTypes().equals(Collections.singletonList("F"))&&AccessFlags.PUBLIC.isSet(m.getAccessFlags()))setter=true;
        require(field&&setter,"native heightOffset field/setter access");
        System.out.println("TOP_SHA="+PinBarsPatch.fingerprint(PinBarsPatch.code(PinBarsPatch.top(top))));
        System.out.println("BOTTOM_SHA="+PinBarsPatch.fingerprint(PinBarsPatch.code(PinBarsPatch.bottom(bottom))));
        if(args.length>1&&args[1].equals("fingerprints"))return;
        PinBarsPatch.verify(top,bottom);
        MutableClass mt=new MutableClass(top),mb=new MutableClass(bottom);
        PinBarsPatch.apply(mt,mb);
        List<Instruction> t=PinBarsPatch.code(PinBarsPatch.top(mt)),b=PinBarsPatch.code(PinBarsPatch.bottom(mb));
        require(t.size()==57,"top instruction count: "+t.size());
        require(b.size()==46,"bottom instruction count");
        require(t.get(42).getOpcode()==Opcode.INVOKE_STATIC&&PinBarsPatch.ref(t.get(42)).equals(PinBarsPatch.RUNTIME+"->pinBars()Z"),"top preference hook");
        require(t.get(43).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)t.get(43)).getRegisterA()==8,"dead flags scratch register");
        require(t.get(44).getOpcode()==Opcode.IF_EQZ&&((OffsetInstruction)t.get(44)).getCodeOffset()==9,"OFF targets original branch");
        require(t.get(45).getOpcode()==Opcode.IGET_OBJECT&&PinBarsPatch.ref(t.get(45)).equals("Landroidx/compose/material3/x4;->a:Landroidx/compose/material3/rf;"),"native state receiver");
        require(t.get(46).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)t.get(46)).getNarrowLiteral()==0,"expanded zero heightOffset");
        require(t.get(47).getOpcode()==Opcode.INVOKE_VIRTUAL&&PinBarsPatch.ref(t.get(47)).equals("Landroidx/compose/material3/rf;->b(F)V"),"native bounded state setter");
        require(t.get(49).getOpcode()==Opcode.IF_NEZ&&((OneRegisterInstruction)t.get(49)).getRegisterA()==0,"OFF preserves native branch");
        require(t.get(48).getOpcode()==Opcode.RETURN_OBJECT&&((OneRegisterInstruction)t.get(48)).getRegisterA()==5,"ON returns original incoming modifier");
        require(b.get(34).getOpcode()==Opcode.INVOKE_STATIC&&PinBarsPatch.ref(b.get(34)).equals(PinBarsPatch.RUNTIME+"->pinBars()Z"),"bottom preference hook");
        require(b.get(35).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)b.get(35)).getRegisterA()==3,"bottom native visibility argument");
        require(PinBarsPatch.ref(b.get(36)).equals("Lcom/x/ui/common/a3;->e(Lcom/x/ui/common/y2;Z)V"),"bottom native controller retained");
        require(PinBarsPatch.ref(b.get(44)).equals("Lcom/x/ui/common/a3;->e(Lcom/x/ui/common/y2;Z)V"),"upward show unchanged");
        int changed=0;
        for(ClassDef[] pair:new ClassDef[][]{{top,mt},{bottom,mb}})for(Method old:pair[0].getMethods())if(old.getImplementation()!=null){Method newer=matching(pair[1],old);if(!PinBarsPatch.fingerprint(PinBarsPatch.code(old)).equals(PinBarsPatch.fingerprint(PinBarsPatch.code(newer))))changed++;}
        require(changed==2,"exactly two methods modified");
        boolean rejected=false;try{PinBarsPatch.verify(mt,mb);}catch(IllegalStateException expected){rejected=true;}require(rejected,"duplicate rejected");
        System.out.println("PASS PinBarsDexTest: exact original fingerprints, two scoped methods, original OFF fallback, native visibility controller, duplicate guard");
    }
}
