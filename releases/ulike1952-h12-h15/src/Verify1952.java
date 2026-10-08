import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Independently reload serialized DEX and verify every unrelated class is retained. */
public final class Verify1952 {
 static void need(boolean value,String label){if(!value)throw new IllegalStateException(label);}
 static void exactUnowned(Map<String,ClassDef> before,Map<String,ClassDef> after,boolean runtime){
  for(var row:before.entrySet()){
   String type=row.getKey();boolean allowed=runtime?Transform1952.owned(type):type.equals(Transform1952.LOADER)||type.equals(Transform1952.INSTALLER);
   if(!allowed)need(after.containsKey(type)&&MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(after.get(type))),"Unrelated class differs: "+type);
  }
  for(String type:after.keySet())if(!before.containsKey(type))need(runtime&&Transform1952.owned(type),"Unexpected added class: "+type);
 }
 public static void main(String[] args)throws Exception{
  need(args.length==3,"BASE EMITTED BASE_BUNDLE_DEX");Path base=Path.of(args[0]),current=Path.of(args[1]);
  exactUnowned(MergePayloads.classes(base.resolve("ulike/runtime.dex").toString()),MergePayloads.classes(current.resolve("runtime.dex").toString()),true);
  exactUnowned(MergePayloads.classes(base.resolve("classes.dex").toString()),MergePayloads.classes(current.resolve("loader.dex").toString()),false);
  exactUnowned(MergePayloads.classes(args[2]),MergePayloads.classes(current.resolve("bundle-loader.dex").toString()),false);
  need(Arrays.equals(Files.readAllBytes(base.resolve("ulike/methods.dex")),Files.readAllBytes(current.resolve("methods.dex"))),"Legacy native DEX differs");
  need(Arrays.equals(Files.readAllBytes(base.resolve("ulike/methods.tsv")),Files.readAllBytes(current.resolve("methods.tsv"))),"Legacy native method inventory differs");
  System.out.println("PASS serialized DEX reviewed roots, other loader/runtime classes and legacy native methods preserved");
 }
}
