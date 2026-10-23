package app.hiro.oneback.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.Label;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;

/** Verified 452.1.0.53.64 tab-history boundary. Native fragment and layer
 * handlers run first; only the history-pop continuation gains an exit gate. */
public final class Instagram452Tab {
    private Instagram452Tab() {}
    public static final String NAVIGATION="LX/004K;", WRAPPER="LX/00M2;", TAB="LX/08kM;";
    public static final String BODY_SHA="c5d368c87a2cdc1084df38b62c4cb03ac39514b898dddb02439abfe1ca028632";
    private static final String ACTIVITY="Landroid/app/Activity;", FRAGMENT="Landroidx/fragment/app/Fragment;";
    private static final List<String> NONE=Collections.emptyList();

    private static Method find(ClassDef clazz,String name,String result) {
        if(clazz!=null)for(Method m:clazz.getMethods())
            if(name.equals(m.getName())&&result.equals(m.getReturnType())&&m.getParameterTypes().isEmpty())return m;
        return null;
    }
    private static void type(Map<String,ClassDef> original,String name,String parent,int flags) {
        ClassDef c=original.get(name);
        if(c==null||c.getAccessFlags()!=flags||!parent.equals(c.getSuperclass()))
            throw new IllegalStateException("Instagram 452 navigation class changed: "+name);
    }
    private static void field(Map<String,ClassDef> original,String owner,String name,String type,int flags) {
        ClassDef c=original.get(owner);
        if(c!=null)for(Field f:c.getFields())if(name.equals(f.getName())&&type.equals(f.getType())&&flags==f.getAccessFlags())return;
        throw new IllegalStateException("Instagram 452 navigation field changed: "+owner+"->"+name);
    }
    private static void body(Map<String,ClassDef> original,String owner,String name,String result,String expected) {
        Method method=find(original.get(owner),name,result);
        if(method==null||!expected.equals(OneBackPatch.instagramNavigationDigest(method)))
            throw new IllegalStateException("Instagram 452 navigation fingerprint changed: "+owner+"->"+name);
    }
    public static void verify(Map<String,ClassDef> original) {
        type(original,NAVIGATION,"Ljava/lang/Object;",17);
        type(original,WRAPPER,"Ljava/lang/Object;",17);
        type(original,TAB,"Ljava/lang/Enum;",16401);
        body(original,WRAPPER,"A0T","Z",BODY_SHA);
        body(original,NAVIGATION,"A05",FRAGMENT,"7dac241fd44a6159e077a7d57dde260cff075d1d4b8edb3cbc8c34baa7283c88");
        body(original,NAVIGATION,"A06",TAB,"6ac0e6d0c70b5641b4ed4a8981d49ef9bcf20de66e53a777fe60403b3e814764");
        body(original,TAB,"<clinit>","V","963b0c8570baa0c11db047f3e268dd74e22ce85e831a148a58a4c3a359c05662");
        field(original,NAVIGATION,"A03","Landroidx/fragment/app/FragmentActivity;",17);
        field(original,NAVIGATION,"A07","Ljava/util/Stack;",17);
        field(original,WRAPPER,"A02",NAVIGATION,1);
        field(original,"LX/004L;","A00",TAB,17);
        field(original,TAB,"A08",TAB,16409);
    }
    public static void apply(Map<String,ClassDef> original,OneBackPatch.Editor editor,Map<String,Integer> counts) {
        verify(original);
        MutableMethod method=(MutableMethod)find(editor.mutable(WRAPPER),"A0T","Z");
        MutableMethodImplementation code=method.getImplementation();
        // At original 163, size > 1, v4 is the navigation and v1 is its current
        // Fragment. v0's size result is dead until the next pop result writes it.
        // v12 remains true from original 92. Jump to 231 to retain systrace end
        // and the existing handled return, without mutating the native stack.
        Label handled=code.newLabelForIndex(231);
        code.addInstruction(163,new BuilderInstruction22c(Opcode.IGET_OBJECT,0,4,
            new ImmutableFieldReference(NAVIGATION,"A03","Landroidx/fragment/app/FragmentActivity;")));
        code.addInstruction(164,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,0,4,1,0,0,
            new ImmutableMethodReference(OneBackPatch.RUNTIME,"tryInstagramTabBack",
                Arrays.asList(ACTIVITY,"Ljava/lang/Object;","Ljava/lang/Object;"),"Z")));
        code.addInstruction(165,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        code.addInstruction(166,new BuilderInstruction21t(Opcode.IF_NEZ,0,handled));
        Integer previous=counts.get("instagram_native_tab_back");
        counts.put("instagram_native_tab_back",previous==null?1:previous+1);
    }
}
