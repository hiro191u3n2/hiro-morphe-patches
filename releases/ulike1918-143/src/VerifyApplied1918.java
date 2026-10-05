import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import app.hiro.ulike.patches.MethodContract;
public final class VerifyApplied1918 {
 public static void main(String[] a)throws Exception {
  var runtime=DexFileFactory.loadDexFile(a[0],Opcodes.forApi(26));
  Map<String,String> expected=new TreeMap<>();for(ClassDef c:runtime.getClasses())for(Method m:c.getMethods())expected.put(DexFormatter.INSTANCE.getMethodDescriptor(m),MethodContract.sha256(m));
  for(String line:Files.readAllLines(Path.of(a[1]))) {String[] cols=line.split("\t");if(cols.length!=3)throw new IllegalStateException();expected.put(cols[0],cols[2]);}
  int total=expected.size();Set<String> types=new HashSet<>();int methods=0;var apk=DexFileFactory.loadDexContainer(new java.io.File(a[2]),Opcodes.forApi(26));
  for(String name:apk.getDexEntryNames())for(ClassDef c:apk.getEntry(name).getDexFile().getClasses()) {
   if(!types.add(c.getType()))throw new IllegalStateException("duplicate class "+c.getType());
   for(Method m:c.getMethods()) {methods++;String desc=DexFormatter.INSTANCE.getMethodDescriptor(m),hash=expected.remove(desc);if(hash!=null&&!hash.equals(MethodContract.sha256(m)))throw new IllegalStateException("Changed emitted method "+desc);}
  }
  if(!expected.isEmpty())throw new IllegalStateException("Missing methods "+expected.keySet());
  System.out.println("PASS final APK: "+types.size()+" unique classes / "+methods+" methods. "+total+" runtime/payload contracts exactly retained, including all correction hooks and helper methods.");
 }
}
