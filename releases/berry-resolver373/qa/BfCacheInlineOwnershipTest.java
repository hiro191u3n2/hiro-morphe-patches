package app.hiro.berrybfcache.patches;

import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import java.io.File;
import java.util.*;

/** Actual 3.84.21 fixture and receiver mutations; never emits an APK. */
public final class BfCacheInlineOwnershipTest {
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static final String SETUP = "Lhm6;->M(Lh26;)V";
    private static final String TAB = "Lh26;->p:Ld71;";
    private static final String WEB = "Ld71;->s:Lty;";
    private static final String FACTORY = "Lby6;->c(Landroid/webkit/WebView;)Ljz6;";
    private static final String BOUNDARY = "Ljz6;->a:Lorg/chromium/support_lib_boundary/WebViewProviderBoundaryInterface;";
    private static final String CALL = "Lorg/chromium/support_lib_boundary/WebViewProviderBoundaryInterface;->addWebMessageListener(Ljava/lang/String;[Ljava/lang/String;Ljava/lang/reflect/InvocationHandler;)V";
    private static final String SETTER = "Ld71;->q(Landroid/webkit/WebViewClient;)V";
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("unmodified 3.84.21 APK");
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(args[0]), Opcodes.forApi(37));
        Method setup = null;
        for (String name : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(name).getDexFile().getClasses())
            for (Method m : c.getMethods()) if (F.getMethodDescriptor(m).equals(SETUP)) {
                if (setup != null) throw new AssertionError("duplicate setup"); setup = m;
            }
        require(setup != null, "exact setup fixture exists");
        require(owns(setup), "original inlined boundary receiver is owned by tab parameter");
        require(BfCacheInlineStructure.ownsControllerReceiver(setup, TAB, SETTER), "original direct client setter receiver is owned");
        List<Instruction> original = instructions(setup);
        int boundaryIndex = index(original, CALL), factoryIndex = lastIndex(original, FACTORY, boundaryIndex);
        int webIndex = lastIndex(original, WEB, factoryIndex), tabIndex = lastIndex(original, TAB, webIndex);
        int providerIndex = lastIndex(original, BOUNDARY, boundaryIndex);
        reject(fieldReceiver(setup,tabIndex), "foreign tab-to-controller load receiver");
        reject(fieldReceiver(setup,webIndex), "foreign controller-to-WebView load receiver");
        reject(callReceiver(setup,factoryIndex), "foreign provider factory WebView argument");
        reject(fieldReceiver(setup,providerIndex), "foreign provider boundary field receiver");
        reject(callReceiver(setup,boundaryIndex), "foreign boundary invocation receiver");
        Method setterWrong = callReceiver(setup,index(original,SETTER));
        require(!BfCacheInlineStructure.ownsControllerReceiver(setterWrong,TAB,SETTER), "reject foreign direct client setter receiver");
        List<Instruction> duplicate = new ArrayList<>(original);
        duplicate.add(boundaryIndex,original.get(boundaryIndex));
        reject(copy(setup,duplicate), "duplicate boundary invocation");
        branchFixture();
        System.out.println("PASS inline ownership checks=" + checks);
    }

    private static void branchFixture() {
        FieldReference tab=new ImmutableFieldReference("Lh26;","p","Ld71;");
        FieldReference web=new ImmutableFieldReference("Ld71;","s","Lty;");
        FieldReference boundary=new ImmutableFieldReference("Ljz6;","a","Lorg/chromium/support_lib_boundary/WebViewProviderBoundaryInterface;");
        MethodReference factory=new ImmutableMethodReference("Lby6;","c",Collections.singletonList("Landroid/webkit/WebView;"),"Ljz6;");
        MethodReference identity=new ImmutableMethodReference("Ljava/lang/System;","identityHashCode",Collections.singletonList("Ljava/lang/Object;"),"I");
        MethodReference call=new ImmutableMethodReference("Lorg/chromium/support_lib_boundary/WebViewProviderBoundaryInterface;","addWebMessageListener",
            Arrays.asList("Ljava/lang/String;","[Ljava/lang/String;","Ljava/lang/reflect/InvocationHandler;"),"V");
        List<Instruction> code=Arrays.asList(
            new ImmutableInstruction22c(Opcode.IGET_OBJECT,0,7,tab),
            new ImmutableInstruction22c(Opcode.IGET_OBJECT,0,0,web),
            new ImmutableInstruction35c(Opcode.INVOKE_STATIC,1,0,0,0,0,0,factory),
            new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT,0),
            new ImmutableInstruction22c(Opcode.IGET_OBJECT,0,0,boundary),
            new ImmutableInstruction35c(Opcode.INVOKE_STATIC,1,6,0,0,0,0,identity),
            new ImmutableInstruction11x(Opcode.MOVE_RESULT,1),
            new ImmutableInstruction11n(Opcode.CONST_4,2,0),
            new ImmutableInstruction11n(Opcode.CONST_4,3,0),
            new ImmutableInstruction11n(Opcode.CONST_4,4,0),
            new ImmutableInstruction21t(Opcode.IF_EQZ,1,3),
            new ImmutableInstruction10x(Opcode.NOP),
            new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE,4,0,2,3,4,0,call),
            new ImmutableInstruction10x(Opcode.RETURN_VOID));
        require(owns(fixture(code)),"branch join retains owned boundary on both paths");
        List<Instruction> corrupted=new ArrayList<>(code);
        corrupted.set(11,new ImmutableInstruction11n(Opcode.CONST_4,0,0));
        reject(fixture(corrupted),"branch join rejects boundary overwritten on one path");
    }
    private static Method fixture(List<Instruction> code) {
        return new ImmutableMethod("Lhm6;","testInlineBoundary",
            Collections.singletonList(new ImmutableMethodParameter("Lh26;",Collections.emptySet(),null)),"V",1,
            Collections.emptySet(),Collections.emptySet(),new ImmutableMethodImplementation(8,code,Collections.emptyList(),Collections.emptyList()));
    }

    private static boolean owns(Method method) {
        return BfCacheInlineStructure.ownsBoundaryReceiver(method,TAB,WEB,FACTORY,BOUNDARY,CALL);
    }
    private static Method fieldReceiver(Method source, int index) {
        List<Instruction> code=instructions(source); Instruction old=code.get(index);
        if (old.getOpcode()!=Opcode.IGET_OBJECT) throw new AssertionError("field fixture opcode");
        TwoRegisterInstruction field=(TwoRegisterInstruction)old;
        code.set(index,new ImmutableInstruction22c(old.getOpcode(),field.getRegisterA(),0,((ReferenceInstruction)old).getReference()));
        return copy(source,code);
    }
    private static Method callReceiver(Method source,int index) {
        List<Instruction> code=instructions(source);Instruction old=code.get(index);
        if (!(old instanceof FiveRegisterInstruction)) throw new AssertionError("call fixture form");
        FiveRegisterInstruction call=(FiveRegisterInstruction)old;
        code.set(index,new ImmutableInstruction35c(old.getOpcode(),call.getRegisterCount(),0,
            call.getRegisterD(),call.getRegisterE(),call.getRegisterF(),call.getRegisterG(),((ReferenceInstruction)old).getReference()));
        return copy(source,code);
    }
    private static List<Instruction> instructions(Method method) {
        List<Instruction> result=new ArrayList<>();for(Instruction i:method.getImplementation().getInstructions())result.add(i);return result;
    }
    private static int index(List<Instruction> code,String reference) {
        int found=-1;for(int i=0;i<code.size();i++)if(ref(code.get(i),reference)){if(found>=0)throw new AssertionError("duplicate fixture anchor");found=i;}
        if(found<0)throw new AssertionError("missing fixture anchor "+reference);return found;
    }
    private static int lastIndex(List<Instruction> code,String reference,int before) {
        for(int i=before-1;i>=0;i--)if(ref(code.get(i),reference))return i;
        throw new AssertionError("missing preceding field "+reference);
    }
    private static boolean ref(Instruction instruction,String descriptor) {
        return instruction instanceof ReferenceInstruction&&descriptor.equals(F.getReference(((ReferenceInstruction)instruction).getReference()));
    }
    private static Method copy(Method method,List<Instruction> instructions) {
        MethodImplementation body=method.getImplementation();
        return new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),method.getAccessFlags(),
            method.getAnnotations(),method.getHiddenApiRestrictions(),new ImmutableMethodImplementation(body.getRegisterCount(),instructions,body.getTryBlocks(),body.getDebugItems()));
    }
    private static void reject(Method method,String label){require(!owns(method),"reject "+label);}
    private static void require(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
}
