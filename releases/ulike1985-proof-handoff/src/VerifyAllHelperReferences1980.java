import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;

/** Check every emitted helper reference; only exact known platform members may be deferred. */
public final class VerifyAllHelperReferences1980 {
 // These six descriptors are the seven framework/JDK calls present in the
 // published .79 runtime. The actual direct superclass is part of each grant;
 // an arbitrary missing member cannot become valid by implementing Runnable
 // or extending an Android class. Physical Android linkage is still untested.
 static final Map<String,String> EXTERNAL_METHODS=Map.of(
  "Lcom/hiro/ulike/GpuResidual1961$1;->size()I","Ljava/util/LinkedHashMap;",
  "Lcom/hiro/ulike/GpuSingle1960$1;->size()I","Ljava/util/LinkedHashMap;",
  "Lcom/hiro/ulike/ReflectionCache1945$Bounded;->size()I","Ljava/util/LinkedHashMap;",
  "Lcom/hiro/ulike/MacroUi168$Tulip;->getBounds()Landroid/graphics/Rect;","Landroid/graphics/drawable/Drawable;",
  "Lcom/hiro/ulike/MacroUi168$Tulip;->invalidateSelf()V","Landroid/graphics/drawable/Drawable;",
  "Lcom/hiro/ulike/ShutterFeedback1941$Pulse;->setBounds(IIII)V","Landroid/graphics/drawable/Drawable;");
 static boolean own(String type){return type.startsWith("Lhiro/")||type.startsWith("Lcom/hiro/ulike/");}
 static String mid(MethodReference m){return DexFormatter.INSTANCE.getMethodDescriptor(m);}
 static String fid(FieldReference f){return DexFormatter.INSTANCE.getFieldDescriptor(f);}
 // 1 = resolved in included classes; 2 = exact permitted platform member; 0 = absent.
 static int resolve(String type,String suffix,boolean field,Map<String,ClassDef> classes,Set<String> members,Set<String> seen){
  if(type==null)return 0;
  if(included(type,suffix,classes,members,seen))return 1;
  if(field)return 0;
  ClassDef owner=classes.get(type);
  String parent=EXTERNAL_METHODS.get(type+"->"+suffix);
  return parent!=null&&owner!=null&&parent.equals(owner.getSuperclass())?2:0;
 }
 static boolean included(String type,String suffix,Map<String,ClassDef> classes,Set<String> members,Set<String> seen){
  if(type==null||!seen.add(type))return false;
  if(members.contains(type+"->"+suffix))return true;
  ClassDef c=classes.get(type);
  if(c==null||suffix.startsWith("<init>(")||suffix.startsWith("<clinit>("))return false;
  if(included(c.getSuperclass(),suffix,classes,members,seen))return true;
  for(String it:c.getInterfaces())if(included(it,suffix,classes,members,seen))return true;
  return false;
 }
 public static void main(String[] a)throws Exception{
  if(a.length!=4)throw new IllegalArgumentException("BASE_METHODS BASE_RUNTIME NEW_METHODS NEW_RUNTIME");
  var oldMethods=MergePayloads.methods(MergePayloads.classes(a[0]).values());oldMethods.putAll(MergePayloads.methods(MergePayloads.classes(a[1]).values()));
  var all=MergePayloads.classes(a[3]);var current=MergePayloads.methods(all.values());current.putAll(MergePayloads.methods(MergePayloads.classes(a[2]).values()));
  Set<String> methods=new TreeSet<>(),fields=new TreeSet<>();for(ClassDef c:all.values()){for(Method m:c.getMethods())methods.add(mid(m));for(Field f:c.getFields())fields.add(fid(f));}
  int checked=0,external=0,methodCount=0;
  for(Method m:current.values()){
   methodCount++;if(m.getImplementation()==null)continue;
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
  System.out.println("PASS helper references in all "+methodCount+" methods: "+checked+" included members resolved; "+external+" inherited external members deferred to Android runtime.");
 }
}
