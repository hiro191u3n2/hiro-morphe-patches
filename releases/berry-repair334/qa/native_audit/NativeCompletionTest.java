package app.hiro.berrybfcache.patches;
import java.io.*;import java.util.*;import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;
public final class NativeCompletionTest {
 public static void main(String[]a)throws Exception{
  Map<String,ClassDef>c=new LinkedHashMap<>();MultiDexContainer<? extends DexFile>d=DexFileFactory.loadDexContainer(new File(a[0]),Opcodes.forApi(37));
  for(String n:d.getDexEntryNames())for(ClassDef x:d.getEntry(n).getDexFile().getClasses())c.put(x.getType(),x);
  BfCacheRepairPatch.Profile p=BfCacheStructure.discover(c,a[1],a[2],a[3],a[4]);
  Method f=null;for(Method m:c.get(p.client).getMethods())if(m.getName().equals("onPageFinished"))f=m;
  NativeCompletionStructure n=NativeCompletionStructure.discover(c,p,f);
  System.out.println(n.code(p));System.out.println("PASS "+a[0]);
 }
}
