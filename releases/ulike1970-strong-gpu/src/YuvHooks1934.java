import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Pinned Q0 calls only. No preview/video converter, RGB coefficients or capture
 * lifecycle logic is replaced. The original false-return handling remains. */
public final class YuvHooks1934 {
    public static final String TARGET="Li/s/a/w/d0/a;->Q0(Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)V";
    public static final String OLD="Li/s/a/w/s;->u(Landroid/media/Image;[B)Z";
    public static final String NEW="Lcom/hiro/ulike/YuvPlanes1934;->copy(Landroid/media/Image;[B)Z";
    private static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    private static void require(boolean b,String s){if(!b)throw new IllegalStateException(s);}
    public static Method repair(Method method) {
        require(MergePayloads.id(method).equals(TARGET),"YUV1934 target");
        MutableMethodImplementation body=new MutableMethodImplementation(method.getImplementation());
        require(body.getRegisterCount()==15,"Q0 YUV1934 register ABI");
        int count=0;
        for(int i=0;i<body.getInstructions().size();i++) {
            Instruction instruction=body.getInstructions().get(i);
            if(!ref(instruction).equals(OLD))continue;
            require(instruction.getOpcode()==Opcode.INVOKE_STATIC && instruction instanceof FiveRegisterInstruction,"YUV1934 static ABI");
            FiveRegisterInstruction r=(FiveRegisterInstruction)instruction;
            require(r.getRegisterCount()==2 && r.getRegisterC()==13 && r.getRegisterD()==(count==0?1:14),"Q0 YUV1934 argument ABI");
            body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,13,r.getRegisterD(),0,0,0,
                new ImmutableMethodReference("Lcom/hiro/ulike/YuvPlanes1934;","copy",Arrays.asList("Landroid/media/Image;","[B"),"Z")));
            count++;
        }
        require(count==2,"Q0 exactly two stock YUV converters");
        return new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),
            method.getAccessFlags(),method.getAnnotations(),method.getHiddenApiRestrictions(),body);
    }
    public static void verify(Method before,Method after) {
        require(MergePayloads.id(before).equals(TARGET) && MergePayloads.id(after).equals(TARGET),"YUV1934 verifier target");
        Iterator<? extends Instruction> a=before.getImplementation().getInstructions().iterator();
        Iterator<? extends Instruction> b=after.getImplementation().getInstructions().iterator();
        int count=0;
        while(a.hasNext() && b.hasNext()) {
            Instruction x=a.next(),y=b.next();
            require(x.getOpcode()==y.getOpcode() && x.getCodeUnits()==y.getCodeUnits(),"YUV hook instruction shape");
            if(ref(x).equals(OLD)) {
                require(ref(y).equals(NEW),"YUV hook exact new target");
                FiveRegisterInstruction xr=(FiveRegisterInstruction)x,yr=(FiveRegisterInstruction)y;
                require(xr.getRegisterCount()==yr.getRegisterCount() && xr.getRegisterC()==yr.getRegisterC() && xr.getRegisterD()==yr.getRegisterD(),"YUV hook register preservation");
                count++;
            } else require(ImmutableInstructionIdentity.same(x,y),"Q0 unrelated instruction changed");
        }
        require(!a.hasNext() && !b.hasNext() && count==2,"YUV hook instruction count");
        MutableMethodImplementation restored=new MutableMethodImplementation(after.getImplementation());
        int inverses=0;
        for(int i=0;i<restored.getInstructions().size();i++) {
            Instruction instruction=restored.getInstructions().get(i);
            if(!ref(instruction).equals(NEW))continue;
            FiveRegisterInstruction r=(FiveRegisterInstruction)instruction;
            restored.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,r.getRegisterC(),r.getRegisterD(),0,0,0,
                new ImmutableMethodReference("Li/s/a/w/s;","u",Arrays.asList("Landroid/media/Image;","[B"),"Z")));
            inverses++;
        }
        Method original=new ImmutableMethod(after.getDefiningClass(),after.getName(),after.getParameters(),after.getReturnType(),
            after.getAccessFlags(),after.getAnnotations(),after.getHiddenApiRestrictions(),restored);
        require(inverses==2 && MergePayloads.hash(before).equals(MergePayloads.hash(original)),
            "YUV hook inverse must restore full registers, branches, handlers and method contract");
    }
    // ImmutableInstruction.equals is deliberately not assumed across dexlib implementations.
    private static final class ImmutableInstructionIdentity {
        static boolean same(Instruction a,Instruction b) {
            if(!ref(a).equals(ref(b)))return false;
            if(a instanceof RegisterRangeInstruction){RegisterRangeInstruction x=(RegisterRangeInstruction)a,y=(RegisterRangeInstruction)b;if(x.getStartRegister()!=y.getStartRegister()||x.getRegisterCount()!=y.getRegisterCount())return false;}
            else if(a instanceof FiveRegisterInstruction){FiveRegisterInstruction x=(FiveRegisterInstruction)a,y=(FiveRegisterInstruction)b;if(x.getRegisterCount()!=y.getRegisterCount()||x.getRegisterC()!=y.getRegisterC()||x.getRegisterD()!=y.getRegisterD()||x.getRegisterE()!=y.getRegisterE()||x.getRegisterF()!=y.getRegisterF()||x.getRegisterG()!=y.getRegisterG())return false;}
            else if(a instanceof ThreeRegisterInstruction){ThreeRegisterInstruction x=(ThreeRegisterInstruction)a,y=(ThreeRegisterInstruction)b;if(x.getRegisterA()!=y.getRegisterA()||x.getRegisterB()!=y.getRegisterB()||x.getRegisterC()!=y.getRegisterC())return false;}
            else if(a instanceof TwoRegisterInstruction){TwoRegisterInstruction x=(TwoRegisterInstruction)a,y=(TwoRegisterInstruction)b;if(x.getRegisterA()!=y.getRegisterA()||x.getRegisterB()!=y.getRegisterB())return false;}
            else if(a instanceof OneRegisterInstruction && ((OneRegisterInstruction)a).getRegisterA()!=((OneRegisterInstruction)b).getRegisterA())return false;
            if(a instanceof WideLiteralInstruction && ((WideLiteralInstruction)a).getWideLiteral()!=((WideLiteralInstruction)b).getWideLiteral())return false;
            if(a instanceof OffsetInstruction && ((OffsetInstruction)a).getCodeOffset()!=((OffsetInstruction)b).getCodeOffset())return false;
            return true;
        }
    }
}
