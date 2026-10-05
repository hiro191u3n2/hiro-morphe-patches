import java.nio.file.*;
import java.net.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.analysis.*;
import com.android.tools.smali.dexlib2.analysis.reflection.ReflectionClassDef;

/** Supplemental register-flow analysis with actual application classes and SDK/JDK signatures. */
public final class Analyze1918 {
 public static void main(String[] a)throws Exception {
  var all=MergePayloads.classes(a[0]);var methods=MergePayloads.methods(all.values());Set<String> targets=new TreeSet<>();
  for(String line:Files.readAllLines(Path.of(a[2])))if(line.startsWith("CHANGED\t")&&!line.contains("Lapp/hiro/ulike/patches/"))targets.add(line.split("\t")[1]);
  for(String id:methods.keySet())if(id.startsWith("Lcom/hiro/ulike/SettingsReturn1918"))targets.add(id);
  try(URLClassLoader sdk=new URLClassLoader(new URL[]{Path.of(a[1]).toUri().toURL()},Analyze1918.class.getClassLoader())){
   Map<String,ClassDef> reflected=new HashMap<>();
   ClassProvider p=type->{ClassDef known=all.get(type);if(known!=null)return known;
    if(reflected.containsKey(type))return reflected.get(type);
    try{ClassDef c=new ReflectionClassDef(Class.forName(type.substring(1,type.length()-1).replace('/','.'),false,sdk));reflected.put(type,c);return c;}
    catch(Throwable e){return null;}};
   ClassPath cp=new ClassPath(List.of(p),true,ClassPath.NOT_ART);List<String> results=new ArrayList<>();int failed=0;
   for(String id:targets){Method m=methods.get(id);if(m==null)throw new IllegalStateException("Missing analysis target "+id);if(m.getImplementation()==null)continue;
    try{MethodAnalyzer analyzer=new MethodAnalyzer(cp,m,null,false);AnalysisException problem=analyzer.getAnalysisException();if(problem!=null)throw problem;results.add("PASS\t"+id);}
    catch(Throwable error){failed++;results.add("FAIL\t"+id+"\t"+error.toString().replace('\n',' '));}
   }
   results.add("SUMMARY\ttested="+targets.size()+"\tfailures="+failed+"\tsdk_signature_provider=android35_jdk21\tandroid_runtime_execution=false");Files.write(Path.of(a[3]),results);System.out.println(results.get(results.size()-1));
   if(failed>0)throw new IllegalStateException("Register-flow analysis failures: "+failed);
  }
 }
}
