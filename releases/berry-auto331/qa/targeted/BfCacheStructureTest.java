package app.hiro.berrybfcache.patches;
import java.io.*;import java.util.*;import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;
public class BfCacheStructureTest {
 public static void main(String[]a)throws Exception{Map<String,ClassDef>c=new LinkedHashMap<>();MultiDexContainer<? extends DexFile>d=DexFileFactory.loadDexContainer(new File(a[0]),Opcodes.forApi(26));for(String n:d.getDexEntryNames())for(ClassDef x:d.getEntry(n).getDexFile().getClasses())c.put(x.getType(),x);BfCacheRepairPatch.Profile p=BfCacheStructure.discover(c,a[1],a[2],a[3],a[4]);System.out.println("PASS "+p.setupOwner+"->"+p.setupName+" setter="+p.tab+"->"+p.completeSetter+" listener="+p.listenerMethod+" document="+p.documentMethod+" feature="+p.feature+"->"+p.featureMethod+" factory="+p.factory+"->"+p.factoryMethod);}
}
