import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Verify the pinned compiler reproduces the exact published .72 families
 * after each narrowly reviewed source change has been inverted. */
public final class VerifyScope1973 {
 public static void main(String[] args)throws Exception{
  MergePayloads.require(args.length==2,"BASE_RUNTIME INVERSE_HELPERS");
  var baseline=MergePayloads.classes(args[0]);var inverse=MergePayloads.classes(args[1]);
  var expected=new TreeMap<String,ClassDef>();
  for(var row:baseline.entrySet())if(owned(row.getKey()))expected.put(row.getKey(),row.getValue());
  MergePayloads.require(expected.keySet().equals(inverse.keySet()),"Exact inverse .72 GPU family class inventory");
  for(var row:expected.entrySet())MergePayloads.require(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(inverse.get(row.getKey()))),"Inverse source did not reproduce exact .72 GPU family "+row.getKey());
  System.out.println("PASS exact .72 inverse GPU families: "+expected.size()+" classes; original source lines and compiler bodies reproduced");
 }
 static boolean owned(String type){return type.matches("Lcom/hiro/ulike/(GpuNoise1960|GpuQualification1961)(\\$.*)?;");}
}
