package app.hiro.newxui.patches;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** X 12.43.0-prod.01. Only the app-header scroll modifier and main-tab scroll hide request. */
public final class PinBarsPatch {
    public static final String TOP="Lcom/x/ui/common/p4;";
    public static final String BOTTOM="Lcom/x/main/tabs/u;";
    public static final String RUNTIME="Lapp/hiro/newxui/UiOptions;";
    public static final String TOP_SHA="e5e316935df6011372d49a344a4968ffd711f64d9af9aa1e88d6843a86e700bd", BOTTOM_SHA="454aa7d4ced154da4ba1d0af8eac90ca7e4f7cc613991bab174262780d6147db";
    private static final ImmutableMethodReference ENABLED=new ImmutableMethodReference(RUNTIME,"pinBars",Collections.<String>emptyList(),"Z");
    private PinBarsPatch() {}
    public static void apply(BytecodePatchContext context) {
        if (!"com.twitter.android".equals(context.getPackageMetadata().getPackageName()) || !"12.43.0-prod.01".equals(context.getPackageMetadata().getVersionName())) throw new IllegalStateException("Pin bars requires verified X 12.43.0-prod.01");
        verify(context.classDefBy(TOP),context.classDefBy(BOTTOM));
        apply(context.mutableClassDefBy(TOP),context.mutableClassDefBy(BOTTOM));
    }
    public static Method top(ClassDef type) { return method(type,"t","Landroidx/compose/ui/Modifier;",Arrays.asList("Landroidx/compose/ui/Modifier;","Landroidx/compose/material3/x4;","Landroidx/compose/runtime/Composer;","I")); }
    public static Method bottom(ClassDef type) { return method(type,"G","J",Arrays.asList("I","J")); }
    private static Method method(ClassDef type,String name,String returnType,List<String> params) {
        if(type==null)throw new IllegalStateException("Missing bars class");
        Method found=null;
        for(Method m:type.getMethods()) if(m.getName().equals(name)&&m.getReturnType().equals(returnType)&&m.getParameterTypes().equals(params)) {
            if(found!=null)throw new IllegalStateException("Ambiguous bars method "+name); found=m;
        }
        if(found==null||found.getImplementation()==null)throw new IllegalStateException("Missing bars method "+type.getType()+"->"+name);
        return found;
    }
    public static List<Instruction> code(Method method) {List<Instruction> out=new ArrayList<Instruction>(); for(Instruction i:method.getImplementation().getInstructions())out.add(i); return out;}
    public static String ref(Instruction instruction) {return instruction instanceof ReferenceInstruction?((ReferenceInstruction)instruction).getReference().toString():"";}
    public static String fingerprint(List<? extends Instruction> instructions) {
        try {
            StringBuilder out=new StringBuilder();
            for(Instruction i:instructions) {
                out.append(i.getOpcode().name()).append(':');
                if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;out.append(r.getRegisterCount()).append(',').append(r.getRegisterC()).append(',').append(r.getRegisterD()).append(',').append(r.getRegisterE()).append(',').append(r.getRegisterF()).append(',').append(r.getRegisterG());}
                else if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;out.append(r.getStartRegister()).append(',').append(r.getRegisterCount());}
                else if(i instanceof ThreeRegisterInstruction){ThreeRegisterInstruction r=(ThreeRegisterInstruction)i;out.append(r.getRegisterA()).append(',').append(r.getRegisterB()).append(',').append(r.getRegisterC());}
                else if(i instanceof TwoRegisterInstruction){TwoRegisterInstruction r=(TwoRegisterInstruction)i;out.append(r.getRegisterA()).append(',').append(r.getRegisterB());}
                else if(i instanceof OneRegisterInstruction)out.append(((OneRegisterInstruction)i).getRegisterA());
                out.append(':').append(ref(i));
                if(i instanceof WideLiteralInstruction)out.append(':').append(((WideLiteralInstruction)i).getWideLiteral());
                if(i instanceof OffsetInstruction)out.append(':').append(((OffsetInstruction)i).getCodeOffset());
                if(i instanceof SwitchPayload)for(SwitchElement e:((SwitchPayload)i).getSwitchElements())out.append(':').append(e.getKey()).append('@').append(e.getOffset());
                out.append('\n');
            }
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(out.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format("%02x",b&255));return hex.toString();
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public static void verify(ClassDef topClass,ClassDef bottomClass) {
        if(topClass==null||bottomClass==null||!TOP.equals(topClass.getType())||!BOTTOM.equals(bottomClass.getType()))throw new IllegalStateException("Wrong bars classes");
        Method t=top(topClass),b=bottom(bottomClass);
        List<Instruction> ti=code(t),bi=code(b);
        if(t.getImplementation().getRegisterCount()!=9||b.getImplementation().getRegisterCount()!=9||!TOP_SHA.equals(fingerprint(ti))||!BOTTOM_SHA.equals(fingerprint(bi)))throw new IllegalStateException("Pin bars code changed or already patched");
        if(ti.size()!=50||bi.size()!=44||ti.get(42).getOpcode()!=Opcode.IF_NEZ||!ref(ti.get(44)).equals("Landroidx/compose/material3/x4;->f:Landroidx/compose/material3/w4;")||ti.get(49).getOpcode()!=Opcode.RETURN_OBJECT||((OneRegisterInstruction)ti.get(49)).getRegisterA()!=5||!ref(bi.get(34)).equals("Lcom/x/ui/common/a3;->e(Lcom/x/ui/common/y2;Z)V"))throw new IllegalStateException("Pin bars boundary changed");
        FiveRegisterInstruction hide=(FiveRegisterInstruction)bi.get(34);
        if(hide.getRegisterCount()!=2||hide.getRegisterC()!=5||hide.getRegisterD()!=3)throw new IllegalStateException("Pin bars hide registers changed");
    }
    public static void apply(MutableClass topClass,MutableClass bottomClass) {
        // Preserve all Compose grouping and the native core.a effect. Only skip attaching
        // EnterAlwaysScrollBehavior to the application header. v8 (flags) and v4 are dead.
        // Reset saved heightOffset through the native setter so restored collapsed state
        // expands too. Its MutableFloatState setter suppresses equal-value writes.
        MutableMethodImplementation top=((MutableMethod)top(topClass)).getImplementation();
        Label original=top.newLabelForIndex(42);
        top.addInstruction(42,new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,ENABLED));
        top.addInstruction(43,new BuilderInstruction11x(Opcode.MOVE_RESULT,8));
        top.addInstruction(44,new BuilderInstruction21t(Opcode.IF_EQZ,8,original));
        top.addInstruction(45,new BuilderInstruction22c(Opcode.IGET_OBJECT,8,6,new ImmutableFieldReference("Landroidx/compose/material3/x4;","a","Landroidx/compose/material3/rf;")));
        top.addInstruction(46,new BuilderInstruction11n(Opcode.CONST_4,4,0));
        top.addInstruction(47,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,2,8,4,0,0,0,new ImmutableMethodReference("Landroidx/compose/material3/rf;","b",Collections.singletonList("F"),"V")));
        top.addInstruction(48,new BuilderInstruction11x(Opcode.RETURN_OBJECT,5));
        // v3 is native false and is dead after the next visibility call. Read preference
        // into it: OFF preserves false; ON uses the same native show path as upward scroll.
        MutableMethodImplementation bottom=((MutableMethod)bottom(bottomClass)).getImplementation();
        bottom.addInstruction(34,new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,ENABLED));
        bottom.addInstruction(35,new BuilderInstruction11x(Opcode.MOVE_RESULT,3));
    }
}
