import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Independent final-output linkage checks for newly injected app/runtime definitions. */
public final class ReviewNewXApk {
    static final String BYLINE="Lcom/x/ui/common/user/y;";
    static final String CLONE="Lcom/x/ui/common/user/u0HiroAbsolute1243;";
    static final String ORIGINAL="Lcom/x/ui/common/user/u0;";
    static final String POST="Lcom/x/urt/items/post/q1;";
    static final String RUNTIME="Lapp/hiro/newx/runtime/NewXAbsoluteTime;";
    static final String HOOK="hiroAbsolutePostByline1243";
    static Map<String,ClassDef> classes=new HashMap<>();
    static int checks;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static Method find(String type,String name){ClassDef c=classes.get(type);check(c!=null,"Missing type "+type);Method out=null;for(Method m:c.getMethods())if(m.getName().equals(name)){check(out==null,"Ambiguous "+type+name);out=m;}check(out!=null,"Missing method "+type+name);return out;}
    static Method resolveMethod(MethodReference ref,String owner,Set<String> seen){
        if(!seen.add(owner))return null;ClassDef c=classes.get(owner);if(c==null)return null;
        for(Method m:c.getMethods())if(m.getName().equals(ref.getName())&&m.getReturnType().equals(ref.getReturnType())&&m.getParameterTypes().toString().equals(ref.getParameterTypes().toString()))return m;
        if(c.getSuperclass()!=null){Method m=resolveMethod(ref,c.getSuperclass(),seen);if(m!=null)return m;}
        for(String i:c.getInterfaces()){Method m=resolveMethod(ref,i,seen);if(m!=null)return m;}return null;
    }
    static Field resolveField(FieldReference ref,String owner,Set<String> seen){
        if(!seen.add(owner))return null;ClassDef c=classes.get(owner);if(c==null)return null;
        for(Field f:c.getFields())if(f.getName().equals(ref.getName())&&f.getType().equals(ref.getType()))return f;
        if(c.getSuperclass()!=null){Field f=resolveField(ref,c.getSuperclass(),seen);if(f!=null)return f;}
        for(String i:c.getInterfaces()){Field f=resolveField(ref,i,seen);if(f!=null)return f;}return null;
    }
    static String pkg(String type){return type.substring(0,type.lastIndexOf('/')+1);}
    static boolean managed(String type){return type.startsWith("Lcom/x/")||type.startsWith("Lapp/hiro/newx/")||type.startsWith("Lkotlin/")||type.startsWith("Lkotlinx/");}
    static void accessible(ClassDef caller,String referenceOwner,String declaringOwner,int flags,String label){
        ClassDef owner=classes.get(referenceOwner);
        check((owner.getAccessFlags()&1)!=0||pkg(caller.getType()).equals(pkg(referenceOwner)),"Inaccessible referenced class "+label);
        boolean sameOwner=caller.getType().equals(declaringOwner),samePackage=pkg(caller.getType()).equals(pkg(declaringOwner));
        check((flags&1)!=0||sameOwner||(samePackage&&(flags&2)==0),"Inaccessible cross-package member "+label);
    }
    static int calls(Method method,String owner,String name){int n=0;for(Instruction i:method.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)i).getReference();if(r instanceof MethodReference){MethodReference m=(MethodReference)r;if(m.getDefiningClass().equals(owner)&&m.getName().equals(name))n++;}}return n;}
    public static void main(String[]args)throws Exception{
        MultiDexContainer<? extends DexFile> container=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());
        for(String entry:container.getDexEntryNames())for(ClassDef c:container.getEntry(entry).getDexFile().getClasses())check(classes.put(c.getType(),c)==null,"Duplicate class "+c.getType());
        Method byline=find(BYLINE,HOOK),restart=find(CLONE,"invoke"),post=find(POST,"invoke");
        check((byline.getAccessFlags()&9)==9,"Cloned byline must be public static");
        check(calls(byline,RUNTIME,"post")==1,"Missing cloned formatter hook");
        check(calls(byline,CLONE,"<init>")==1,"Missing cloned restart constructor");
        check(calls(restart,BYLINE,HOOK)==1,"Restart must re-enter absolute byline");
        check(calls(post,BYLINE,HOOK)==1,"Post must enter absolute byline");
        check(calls(find(BYLINE,"r"),"Lcom/x/android/utils/i2;","i")==1,"Shared original formatter changed");
        check(calls(find(ORIGINAL,"invoke"),BYLINE,"r")==1,"Shared restart lambda changed");
        int checkedMethods=0,checkedFields=0;
        for(ClassDef c:classes.values())if(c.getType().startsWith("Lapp/hiro/newx/")||c.getType().equals(CLONE))for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){
            Reference r=((ReferenceInstruction)i).getReference();
            if(r instanceof MethodReference){MethodReference target=(MethodReference)r;String owner=target.getDefiningClass();if(!classes.containsKey(owner)){check(!managed(owner),"Missing app method owner "+target);continue;}Method found=resolveMethod(target,owner,new HashSet<>());check(found!=null,"Unresolved app method "+target+" in "+m);boolean invokeStatic=i.getOpcode()==Opcode.INVOKE_STATIC||i.getOpcode()==Opcode.INVOKE_STATIC_RANGE;check(invokeStatic==((found.getAccessFlags()&8)!=0),"Static opcode mismatch "+target);accessible(c,owner,found.getDefiningClass(),found.getAccessFlags(),target.toString());checkedMethods++;}
            else if(r instanceof FieldReference){FieldReference target=(FieldReference)r;String owner=target.getDefiningClass();if(!classes.containsKey(owner)){check(!managed(owner),"Missing app field owner "+target);continue;}Field found=resolveField(target,owner,new HashSet<>());check(found!=null,"Unresolved app field "+target+" in "+m);String opcode=i.getOpcode().name();boolean staticField=opcode.startsWith("SGET")||opcode.startsWith("SPUT");check(staticField==((found.getAccessFlags()&8)!=0),"Static field opcode mismatch "+target);accessible(c,owner,found.getDefiningClass(),found.getAccessFlags(),target.toString());if(c.getType().equals(CLONE))check(!owner.equals(ORIGINAL),"Cloned lambda accesses original receiver fields");checkedFields++;}
        }
        System.out.println("PASS ReviewNewXApk: final cloned method/restart/caller present; shared originals preserved; "+checkedMethods+" app-method and "+checkedFields+" app-field references resolved; "+checks+" checks");
    }
}
