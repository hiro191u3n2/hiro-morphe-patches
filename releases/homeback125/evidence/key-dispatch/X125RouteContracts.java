import java.io.*;import java.util.*;import app.hiro.oneback.patches.OneBackPatch;import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;
public class X125RouteContracts {
 public static void main(String[] args)throws Exception {
  Map<String,Set<String>> wanted=new LinkedHashMap<>();
  wanted.put("Lcom/twitter/app/common/base/i;",Set.of("dispatchKeyEvent","onBackPressed","onCreate"));
  wanted.put("Landroidx/appcompat/app/g;",Set.of("dispatchKeyEvent"));
  wanted.put("Landroidx/core/app/h;",Set.of("dispatchKeyEvent","superDispatchKeyEvent","onUserInteraction"));
  wanted.put("Lcom/twitter/app/common/inject/p;",Set.of("onBackPressed","onCreate"));
  wanted.put("Lcom/twitter/app/main/l1;",Set.of("goBack","I3"));
  wanted.put("Landroidx/activity/ComponentActivity;",Set.of("access$onBackPressed$s1027565324"));
  wanted.put("Landroidx/activity/l0;",Set.of("d"));
  for(String arg:args){System.out.println("APK "+arg);var apk=DexFileFactory.loadDexContainer(new File(arg),Opcodes.getDefault());
   for(String entry:apk.getDexEntryNames())for(ClassDef c:apk.getEntry(entry).getDexFile().getClasses())if(wanted.containsKey(c.getType()))
    for(Method m:c.getMethods())if(wanted.get(c.getType()).contains(m.getName())&&m.getImplementation()!=null){int n=0;for(var i:m.getImplementation().getInstructions())n++;System.out.println(m+" instructions="+n+" digest="+OneBackPatch.instagramNavigationDigest(m));}
  }
 }
}
