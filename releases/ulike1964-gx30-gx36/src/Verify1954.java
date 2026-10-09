import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
/** Independently reload serialized DEX and verify every unrelated class is retained. */
public final class Verify1954 {
 static void need(boolean value,String label){if(!value)throw new IllegalStateException(label);}
 static void exactUnowned(Map<String,ClassDef> before,Map<String,ClassDef> after,boolean runtime)throws Exception{
  for(var row:before.entrySet()){
   String type=row.getKey();boolean allowed=runtime?Transform1954.owned(type):type.equals(Transform1954.LOADER)||type.equals(Transform1954.INSTALLER);
   if(!allowed){need(after.containsKey(type),"Unrelated class missing: "+type);if(runtime)Transform1954.verifyUnownedClass(row.getValue(),after.get(type));else need(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(after.get(type))),"Unrelated class differs: "+type);}
  }
  for(String type:after.keySet())if(!before.containsKey(type))need(runtime&&Transform1954.owned(type),"Unexpected added class: "+type);
 }
 static String digest(Path path)throws Exception{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));}
 static void leaseAudit(Map<String,ClassDef> before,Map<String,ClassDef> after,Path base,Path current)throws Exception{
  var oldMethods=MergePayloads.methods(before.values());var newMethods=MergePayloads.methods(after.values());int calls=0;var pins=new ArrayList<String>();
  for(String id:new TreeSet<>(SaveLeaseHooks1954.TARGETS.keySet())){
   need(oldMethods.containsKey(id)&&newMethods.containsKey(id),"Pinned final-save hook target missing "+id);
   Method old=oldMethods.get(id),now=newMethods.get(id);SaveLeaseHooks1954.verify(old,now);int count=0;
   for(Instruction op:now.getImplementation().getInstructions())if(SaveLeaseHooks1954.ref(op).equals(SaveLeaseHooks1954.GATE))count++;
   need(count==(id.contains("SaveFd186;")?1:3),"Serialized final-save hook count "+id);calls+=count;
   pins.add("\""+id+"\":\""+SaveLeaseHooks1954.TARGETS.get(id)+"\"");
  }
  need(calls==4,"Exactly four serialized final-save disposal calls");
  Files.writeString(current.resolve("save-lease-hooks1954.json"),"{\n\"schema\":\"ulike-save-lease-hooks1954-v1\",\n\"status\":\"passed\",\n\"inverse_bytecode_verified\":true,\n\"serialized_dex_verified\":true,\n\"recycle_call_count\":"+calls+",\n\"target_methods\":"+Transform1954.jsonList(SaveLeaseHooks1954.TARGETS.keySet())+",\n\"published1953_method_pins\":{"+String.join(",",pins)+"},\n\"baseline_runtime_sha256\":\""+digest(base.resolve("ulike/runtime.dex"))+"\",\n\"updated_runtime_sha256\":\""+digest(current.resolve("runtime.dex"))+"\"\n}\n");
 }
 public static void main(String[] args)throws Exception{
  need(args.length==3,"BASE EMITTED BASE_BUNDLE_DEX");Path base=Path.of(args[0]),current=Path.of(args[1]);
  var beforeRuntime=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var afterRuntime=MergePayloads.classes(current.resolve("runtime.dex").toString());
  exactUnowned(beforeRuntime,afterRuntime,true);leaseAudit(beforeRuntime,afterRuntime,base,current);
  exactUnowned(MergePayloads.classes(base.resolve("classes.dex").toString()),MergePayloads.classes(current.resolve("loader.dex").toString()),false);
  exactUnowned(MergePayloads.classes(args[2]),MergePayloads.classes(current.resolve("bundle-loader.dex").toString()),false);
  need(Arrays.equals(Files.readAllBytes(base.resolve("ulike/methods.dex")),Files.readAllBytes(current.resolve("methods.dex"))),"Legacy native DEX differs");
  need(Arrays.equals(Files.readAllBytes(base.resolve("ulike/methods.tsv")),Files.readAllBytes(current.resolve("methods.tsv"))),"Legacy native method inventory differs");
  System.out.println("PASS serialized DEX reviewed roots, four pinned final-save lease hooks with inverse bytecode proof, other loader/runtime classes and legacy native methods preserved");
 }
}
