import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Class/signature/reflection checks across every DEX of the fully rebuilt APK.
 * The separate member validator checks actual methods and inherited fields. */
public final class VerifyAppliedTypes1981 {
    static Map<String,ClassDef> classes;
    static int types,strings,methods;
    static boolean own(String type){return type.startsWith("Lcom/hiro/ulike/")||type.startsWith("Lhiro/");}
    static void type(String value,String origin){
        if(value==null)return;
        while(value.startsWith("["))value=value.substring(1);
        if(!own(value))return;
        types++;
        if(!classes.containsKey(value))throw new IllegalStateException("Missing helper type "+value+" in "+origin);
    }
    static void reflection(String value,String origin){
        String target=null;
        if(value.matches("(?:com[./]hiro[./]ulike|hiro)[./][A-Za-z0-9_$/.]+"))target="L"+value.replace('.','/')+";";
        else if(value.matches("L(?:com/hiro/ulike|hiro)/[A-Za-z0-9_$/]+;"))target=value;
        if(target!=null){strings++;type(target,origin+" reflection");}
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("APPLIED_APK");
        classes=MergePayloads.classes(args[0]);
        for(ClassDef c:classes.values()){
            type(c.getSuperclass(),c.getType());
            for(String i:c.getInterfaces())type(i,c.getType());
            for(Field f:c.getFields())type(f.getType(),c.getType()+"->"+f.getName());
            for(Method m:c.getMethods()){
                methods++;String origin=MergePayloads.id(m);
                type(m.getReturnType(),origin);
                for(CharSequence parameter:m.getParameterTypes())type(parameter.toString(),origin);
                if(m.getImplementation()==null)continue;
                for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r){
                    Reference ref=r.getReference();
                    if(ref instanceof TypeReference t)type(t.getType(),origin);
                    else if(ref instanceof StringReference s)reflection(s.getString(),origin);
                    else if(ref instanceof FieldReference f){type(f.getDefiningClass(),origin);type(f.getType(),origin);}
                    else if(ref instanceof MethodReference call){
                        type(call.getDefiningClass(),origin);type(call.getReturnType(),origin);
                        for(CharSequence parameter:call.getParameterTypes())type(parameter.toString(),origin);
                    }
                }
            }
        }
        System.out.println("{\"status\":\"passed\",\"apk_classes\":"+classes.size()+",\"apk_methods\":"+methods
            +",\"helper_type_references_checked\":"+types+",\"helper_reflection_class_strings_checked\":"+strings
            +",\"missing_helper_types\":0,\"physical_android_tested\":false}");
    }
}
