package app.hiro.oneback.patches;

import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;

/** Exact native Home callback and post-guard exit-prompt boundary in 452.
 * No native child/pop/refresh branch is removed or made unconditionally true.
 */
public final class InstagramNativeHomePatch {
    private InstagramNativeHomePatch() {}
    public static final String HOME="LX/00nV;";
    public static final String BACK_SHA="16dc8b50645b59df92f4cec2ddee4ae296f29d42c5b8fd2041704270aad35df5";
    public static final String PROMPT_SHA="574bcd72b1809cb38405965d481858376ed0c46cdd7a9b56dc2c4380b2145adf";
    private static final List<String> NONE=Collections.emptyList(),BOOL=Collections.singletonList("Z");
    private static Method method(Map<String,ClassDef> classes,String owner,String name,List<String> args,String result) {
        ClassDef type=classes.get(owner);if(type!=null)for(Method method:type.getMethods())
            if(OneBackPatch.signature(method,name,args,result))return method;
        throw new IllegalStateException("Instagram 452 native Home method missing: "+owner+"->"+name);
    }
    private static void body(Map<String,ClassDef> classes,String owner,String name,List<String> args,String result,int flags,int registers,String sha) {
        Method method=method(classes,owner,name,args,result);
        if(method.getAccessFlags()!=flags||method.getImplementation()==null||method.getImplementation().getRegisterCount()!=registers
                ||!sha.equals(OneBackPatch.instagramNavigationDigest(method)))
            throw new IllegalStateException("Instagram 452 native Home body changed: "+owner+"->"+name);
    }
    public static void verifyOriginal(Map<String,ClassDef> classes) {
        body(classes,HOME,"onBackPressed",NONE,"Z",17,11,BACK_SHA);
        body(classes,HOME,"F6u",BOOL,"Z",17,8,PROMPT_SHA);
        body(classes,"LX/00M2;","A0T",NONE,"Z",17,25,"c5d368c87a2cdc1084df38b62c4cb03ac39514b898dddb02439abfe1ca028632");
        body(classes,"LX/00VS;","F6u",BOOL,"Z",17,6,"e159fc01a8e66869cd1b080685c72e0de63296a652ec053656c0eeb97665ad9b");
        body(classes,"LX/02Wh;","F6u",BOOL,"Z",17,6,"be104202f70e7aebcc96bcd5b9d4cba4a8045145821cda17cfe25a98db1774fe");
        body(classes,"LX/0A4c;","F6u",BOOL,"Z",17,5,"d4ce5255d38732fb071ba472f1e85ce0acbe0e3f5ed5084e389a2b3a2b5f5a58");
        body(classes,"LX/00ZX;","A03",Arrays.asList("Landroidx/fragment/app/Fragment;","Landroidx/fragment/app/FragmentActivity;","LX/00fk;","Lcom/instagram/common/session/UserSession;"),"Z",25,8,
                "6405ab0275e82faf3aa12f2da6c02d5dd46e68f1a119c7ce369abcfd5271aa8c");
        Set<String> callers=new TreeSet<String>();
        for(ClassDef type:classes.values())for(Method method:type.getMethods())if(method.getImplementation()!=null)
            for(Instruction instruction:method.getImplementation().getInstructions())if(instruction instanceof ReferenceInstruction) {
                Object ref=((ReferenceInstruction)instruction).getReference();
                if(ref instanceof MethodReference&&"F6u".equals(((MethodReference)ref).getName())
                        &&OneBackPatch.signature((MethodReference)ref,"F6u",BOOL,"Z"))
                    callers.add(type.getType()+"->"+method.getName());
            }
        if(!callers.equals(new TreeSet<String>(Arrays.asList("LX/00M2;->A0T","LX/00VS;->F6u","LX/02Wh;->F6u","LX/0A4c;->F6u"))))
            throw new IllegalStateException("Instagram 452 native Home caller set changed");
        List<Instruction> back=list(method(classes,HOME,"onBackPressed",NONE,"Z")),prompt=list(method(classes,HOME,"F6u",BOOL,"Z"));
        List<Integer> returns=new ArrayList<Integer>();for(int i=0;i<back.size();i++)if(back.get(i).getOpcode()==Opcode.RETURN){
            if(((OneRegisterInstruction)back.get(i)).getRegisterA()!=0)throw new IllegalStateException("Native Home return register changed");returns.add(i);}
        if(!returns.equals(Arrays.asList(6,65))||prompt.get(42).getOpcode()!=Opcode.NEW_INSTANCE
                ||!"LX/0BJz;".equals(((ReferenceInstruction)prompt.get(42)).getReference().toString()))
            throw new IllegalStateException("Native Home prompt boundary changed");
        int address=0;Map<Integer,Integer> indexes=new HashMap<Integer,Integer>();
        for(int i=0;i<prompt.size();i++){indexes.put(address,i);address+=prompt.get(i).getCodeUnits();}
        List<Integer> incoming=new ArrayList<Integer>();address=0;
        for(int i=0;i<prompt.size();i++){Instruction instruction=prompt.get(i);
            if(instruction instanceof OffsetInstruction&&Integer.valueOf(42).equals(indexes.get(address+((OffsetInstruction)instruction).getCodeOffset())))incoming.add(i);
            address+=instruction.getCodeUnits();}
        if(!incoming.equals(Collections.singletonList(37)))throw new IllegalStateException("Native Home prompt incoming edges changed");
    }
    private static List<Instruction> list(Method method){List<Instruction> result=new ArrayList<Instruction>();for(Instruction i:method.getImplementation().getInstructions())result.add(i);return result;}
    private static MutableMethod mutable(MutableClass type,String name,List<String> args) {
        for(Method method:type.getMethods())if(OneBackPatch.signature(method,name,args,"Z"))return (MutableMethod)method;
        throw new IllegalStateException("Native Home mutable method missing");
    }
    private static BuilderInstruction35c call(String name,List<String> args,String result,int count,int first,int second) {
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,count,first,second,0,0,0,
                new ImmutableMethodReference(OneBackPatch.RUNTIME,name,args,result));
    }
    public static void apply(Map<String,ClassDef> original,OneBackPatch.Editor editor,Map<String,Integer> counts) {
        verifyOriginal(original);
        MutableClass home=editor.mutable(HOME);
        MutableMethodImplementation back=mutable(home,"onBackPressed",NONE).getImplementation();
        // Replace both native returns so all original incoming branches visit
        // the result hook; their original boolean is returned without changes.
        for(int index:new int[]{65,6}) {
            back.replaceInstruction(index,call("finishInstagramNativeHomeBack",Arrays.asList("Ljava/lang/Object;","Z"),"Z",2,10,0));
            back.addInstruction(index+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
            back.addInstruction(index+2,new BuilderInstruction11x(Opcode.RETURN,0));
        }
        back.addInstruction(0,call("beginInstagramNativeHomeBack",Collections.singletonList("Ljava/lang/Object;"),"V",1,10,0));
        MutableMethodImplementation prompt=mutable(home,"F6u",BOOL).getImplementation();
        // The original prompt/refresh fallthrough and gesture-check-disabled
        // edge at 37 must both reach the gate. Other consuming branches stay.
        prompt.addInstruction(42,call("tryInstagramNativeHomePrompt",Collections.singletonList("Ljava/lang/Object;"),"Z",1,6,0));
        prompt.addInstruction(43,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        prompt.addInstruction(44,new BuilderInstruction10x(Opcode.NOP));
        prompt.addInstruction(45,new BuilderInstruction11x(Opcode.RETURN,0));
        prompt.replaceInstruction(44,new BuilderInstruction21t(Opcode.IF_EQZ,0,prompt.newLabelForIndex(46)));
        prompt.replaceInstruction(37,new BuilderInstruction21t(Opcode.IF_EQZ,0,prompt.newLabelForIndex(42)));
        counts.put("instagram_452_native_home_begin",1);counts.put("instagram_452_native_home_result",2);counts.put("instagram_452_native_home_prompt",1);
    }
}
