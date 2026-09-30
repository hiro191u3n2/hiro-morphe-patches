import java.nio.file.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.formatter.DexFormatter;import com.android.tools.smali.dexlib2.writer.pool.DexPool;import com.android.tools.smali.dexlib2.writer.io.FileDataStore;import app.hiro.ulike.patches.MethodContract;
/** Replace only the existing optional photo detail helpers, preserving every other class. */
public final class MergeRuntime {
 static boolean helper(String t){return t.startsWith("Lcom/hiro/ulike/PhotoDetail")||t.startsWith("Lcom/hiro/ulike/DetailPixels");}
 static String id(MethodReference m){return DexFormatter.INSTANCE.getMethodDescriptor(m);}
 static void require(boolean b,String m){if(!b)throw new IllegalStateException(m);}
 public static void main(String[] a)throws Exception{
  var old=DexFileFactory.loadDexFile(a[0],Opcodes.forApi(26));var fresh=DexFileFactory.loadDexFile(a[1],Opcodes.forApi(26));
  TreeMap<String,ClassDef> all=new TreeMap<>(),untouched=new TreeMap<>();Map<String,String> expected=new TreeMap<>();int retained=0,removed=0,added=0;
  for(ClassDef c:old.getClasses()){
   if(helper(c.getType())){for(Method m:c.getMethods())removed++;continue;}
   all.put(c.getType(),c);untouched.put(c.getType(),c);for(Method m:c.getMethods()){retained++;expected.put(id(m),MethodContract.sha256(m));}
  }
  require(retained+removed==475&&removed>0,"Expected v1.5.7 runtime: "+retained+" retained + "+removed+" detail");
  for(ClassDef c:fresh.getClasses()){
   require(helper(c.getType()),"unexpected helper/stub: "+c.getType());require(all.putIfAbsent(c.getType(),c)==null,"duplicate type");
   for(Method m:c.getMethods()){added++;expected.put(id(m),MethodContract.sha256(m));}
  }
  require(all.containsKey("Lcom/hiro/ulike/PhotoDetail;")&&all.containsKey("Lcom/hiro/ulike/DetailPixels;"),"missing photo helpers");
  DexPool pool=new DexPool(old.getOpcodes());for(ClassDef c:all.values())pool.internClass(c);var store=new FileDataStore(Path.of(a[2]).toFile());try{pool.writeTo(store);}finally{store.close();}
  var written=DexFileFactory.loadDexFile(a[2],Opcodes.forApi(26));Set<String> ids=new HashSet<>(),fields=new HashSet<>();
  for(ClassDef c:written.getClasses()){
   ClassDef prev=untouched.remove(c.getType());if(prev!=null)require(ImmutableClassDef.of(prev).equals(ImmutableClassDef.of(c)),"changed unrelated class "+c.getType());
   for(Field f:c.getFields())fields.add(DexFormatter.INSTANCE.getFieldDescriptor(f));
   for(Method m:c.getMethods()){String key=id(m);require(ids.add(key),"duplicate method "+key);String hash=expected.remove(key);require(hash!=null&&hash.equals(MethodContract.sha256(m)),"emitted body mismatch "+key);}
  }
  require(expected.isEmpty()&&untouched.isEmpty(),"lost definitions");
  for(ClassDef c:written.getClasses())for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction ins:m.getImplementation().getInstructions())if(ins instanceof ReferenceInstruction ri){
   if(ri.getReference() instanceof MethodReference r&&helper(r.getDefiningClass()))require(ids.contains(id(r)),"unresolved detail method "+r);
   if(ri.getReference() instanceof FieldReference r&&helper(r.getDefiningClass()))require(fields.contains(DexFormatter.INSTANCE.getFieldDescriptor(r)),"unresolved detail field "+r);
  }
  System.out.println("PASS "+retained+" prior non-detail runtime methods and all unrelated classes unchanged; "+removed+" old detail methods replaced with "+added+"; all "+ids.size()+" emitted methods verified and detail references resolve.");
 }
}
