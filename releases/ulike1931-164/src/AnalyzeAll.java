import java.nio.file.*;import java.net.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.analysis.*;import com.android.tools.smali.dexlib2.analysis.reflection.ReflectionClassDef;
public class AnalyzeAll {
 public static void main(String[] a)throws Exception{
 var all=MergePayloads.classes(a[0]);var methods=MergePayloads.methods(all.values());
 try(URLClassLoader sdk=new URLClassLoader(new URL[]{Path.of(a[1]).toUri().toURL()},AnalyzeAll.class.getClassLoader())){
 Map<String,ClassDef> reflected=new HashMap<>();ClassProvider p=type->{ClassDef known=all.get(type);if(known!=null)return known; if(reflected.containsKey(type))return reflected.get(type);try{ClassDef c=new ReflectionClassDef(Class.forName(type.substring(1,type.length()-1).replace('/','.'),false,sdk));reflected.put(type,c);return c;}catch(Throwable e){return null;}};
 ClassPath cp=new ClassPath(List.of(p),true,ClassPath.NOT_ART);int tested=0,failed=0;List<String> lines=new ArrayList<>();
 for(var entry:methods.entrySet()){if(!entry.getKey().startsWith("Lcom/hiro/ulike/")&&!Transform1931.NATIVE.contains(entry.getKey()))continue;Method m=entry.getValue();if(m.getImplementation()==null)continue; tested++;
 try{var analyzer=new MethodAnalyzer(cp,m,null,false);var error=analyzer.getAnalysisException();if(error!=null)throw error; lines.add("PASS\t"+entry.getKey());}catch(Throwable ex){failed++;lines.add("FAIL\t"+entry.getKey()+"\t"+ex);System.out.println(lines.getLast());}
 }
 lines.add("tested="+tested+" failed="+failed);System.out.println(lines.getLast());Files.write(Path.of(a[2]),lines);
 }
 }
}
