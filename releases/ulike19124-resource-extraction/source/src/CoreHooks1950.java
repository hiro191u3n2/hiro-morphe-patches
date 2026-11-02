import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** One private main-NR kernel is wrapped; its production bytecode is preserved
 * as a package static alias. No stage or setting/mask/sharpening method changes. */
public final class CoreHooks1950 {
 static final String OWNER="Lcom/hiro/ulike/DetailSerial186;",HELPER="Lcom/hiro/ulike/CorePixels1950;";
 public static final String targetId=OWNER+"->bilateral([I[I[IIIIZZZII)V";
 public static final String aliasId=OWNER+"->bilateralBefore1950([I[I[IIIIZZZII)V";
 public static final Set<String> changedIds=Set.of(targetId),newIds=Set.of(aliasId);
 public static Set<String> changedIds(){return changedIds;}
 public static Set<String> newIds(){return newIds;}
 static void need(boolean b,String m){if(!b)throw new IllegalStateException(m);}
 static List<String> args(Method m){List<String>a=new ArrayList<>();m.getParameters().forEach(p->a.add(p.getType()));return a;}
 static Method named(Method m,String name,int flags,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),name,m.getParameters(),m.getReturnType(),flags,m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 public static Method alias(Method m){
  need(MergePayloads.id(m).equals(targetId),"exact original bilateral signature");
  return named(m,"bilateralBefore1950",m.getAccessFlags()&~AccessFlags.PRIVATE.getValue(),m.getImplementation());
 }
 public static Method repair(Method m){
  need(MergePayloads.id(m).equals(targetId)&&m.getImplementation()!=null&&m.getImplementation().getRegisterCount()==40,"pinned bilateral ABI");
  var b=new MutableMethodImplementation(11);b.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,0,11,new ImmutableMethodReference(HELPER,"bilateral",args(m),"V")));b.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
  return named(m,m.getName(),m.getAccessFlags(),b);
 }
 public static Set<String> apply(Map<String,ClassDef> runtime,Map<String,ClassDef> baseline){
  ClassDef c=runtime.get(OWNER);need(c!=null&&MergePayloads.methods(baseline.values()).containsKey(targetId),"original main-NR boundary exists");
  var ms=new ArrayList<Method>();int count=0;for(Method m:c.getMethods()){
   need(!MergePayloads.id(m).equals(aliasId),"fresh bilateral alias required");
   if(MergePayloads.id(m).equals(targetId)){ms.add(repair(m));ms.add(alias(m));count++;}else ms.add(m);
  }
  need(count==1,"exactly one native main-NR hook");runtime.put(OWNER,new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms));return changedIds;
 }
 public static void verify(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  var old=MergePayloads.methods(before.values());var now=MergePayloads.methods(after.values());Method m=old.get(targetId);need(m!=null,"main-NR baseline");
  need(MergePayloads.hash(repair(m)).equals(MergePayloads.hash(now.get(targetId))),"exact native bridge body");
  Method preserved=now.get(aliasId);need(preserved!=null&&MergePayloads.hash(alias(m)).equals(MergePayloads.hash(preserved)),"original bilateral bytecode/exception paths preserved");
  for(var e:old.entrySet())if(e.getValue().getDefiningClass().equals(OWNER)&&!e.getKey().equals(targetId))need(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(now.get(e.getKey()))),"unrelated NR method changed");
  need(before.get(OWNER).getFields().equals(after.get(OWNER).getFields()) || MergePayloads.classHash(before.get(OWNER)).equals(MergePayloads.classHash(new ImmutableClassDef(before.get(OWNER).getType(),before.get(OWNER).getAccessFlags(),before.get(OWNER).getSuperclass(),before.get(OWNER).getInterfaces(),before.get(OWNER).getSourceFile(),before.get(OWNER).getAnnotations(),after.get(OWNER).getFields(),before.get(OWNER).getMethods()))),"NR shell fields preserved");
 }
}
