import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;

/** Link checks for newly emitted/changed methods. Android framework linkage remains a device gate. */
public final class VerifyHelperReferences {
 static boolean own(String type){return type.startsWith("Lhiro/")||type.startsWith("Lcom/hiro/ulike/")||type.startsWith("Lai/onnxruntime/");}
 static String mid(MethodReference m){return DexFormatter.INSTANCE.getMethodDescriptor(m);}
 static String fid(FieldReference f){return DexFormatter.INSTANCE.getFieldDescriptor(f);}
 // 1 = resolved in included classes; 2 = inherited external superclass needs platform runtime; 0 = absent.
 static int resolve(String type,String suffix,boolean field,Map<String,ClassDef> classes,Set<String> members,Set<String> seen){
  if(type==null||!seen.add(type))return 0;
  if(members.contains(type+"->"+suffix))return 1;
  ClassDef c=classes.get(type);
  if(c==null){
   if(type.equals("Ljava/lang/Object;")){
    if(field)return 0;
    Set<String> methods=Set.of("getClass()Ljava/lang/Class;","hashCode()I","equals(Ljava/lang/Object;)Z","toString()Ljava/lang/String;","clone()Ljava/lang/Object;","notify()V","notifyAll()V","wait()V","wait(J)V","wait(JI)V","finalize()V");
    return methods.contains(suffix)?2:0;
   }
   return 2;
  }
  if(suffix.startsWith("<init>(")||suffix.startsWith("<clinit>("))return 0;
  int result=resolve(c.getSuperclass(),suffix,field,classes,members,seen);
  if(result==1)return 1;
  for(String it:c.getInterfaces()){int v=resolve(it,suffix,field,classes,members,seen);if(v==1)return 1;result=Math.max(result,v);}
  return result;
 }
 public static void main(String[] a)throws Exception{
  if(a.length!=4)throw new IllegalArgumentException("BASE_METHODS BASE_RUNTIME NEW_METHODS NEW_RUNTIME");
  var oldMethods=MergePayloads.methods(MergePayloads.classes(a[0]).values());oldMethods.putAll(MergePayloads.methods(MergePayloads.classes(a[1]).values()));
  var all=MergePayloads.classes(a[3]);var current=MergePayloads.methods(all.values());current.putAll(MergePayloads.methods(MergePayloads.classes(a[2]).values()));
  Set<String> methods=new TreeSet<>(),fields=new TreeSet<>();for(ClassDef c:all.values()){for(Method m:c.getMethods())methods.add(mid(m));for(Field f:c.getFields())fields.add(fid(f));}
  int checked=0,external=0,changed=0;
  for(Method m:current.values()){
   Method old=oldMethods.get(MergePayloads.id(m));if(old!=null&&MergePayloads.hash(old).equals(MergePayloads.hash(m)))continue;
   changed++;if(m.getImplementation()==null)continue;
   for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r){
    Reference ref=r.getReference();String key,type;boolean field;
    if(ref instanceof MethodReference mr){type=mr.getDefiningClass();key=mid(mr);field=false;}
    else if(ref instanceof FieldReference fr){type=fr.getDefiningClass();key=fid(fr);field=true;}
    else continue;
    if(!own(type))continue;
    if(!all.containsKey(type))throw new IllegalStateException("Missing helper class "+type+" referenced from "+MergePayloads.id(m));
    int result=resolve(type,key.substring(key.indexOf("->")+2),field,all,field?fields:methods,new HashSet<>());
    if(result==0)throw new IllegalStateException("Unresolved helper member "+key+" referenced from "+MergePayloads.id(m));
    if(result==1)checked++;else external++;
   }
  }
  System.out.println("PASS helper references in "+changed+" new/changed methods: "+checked+" included members resolved; "+external+" inherited external members deferred to Android runtime.");
 }
}
