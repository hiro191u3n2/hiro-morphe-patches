import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.ZipFile;

/** Executes the real Morphe JVM loader, including Manager-style package/name selection. */
public final class LoaderParityProbe {
    private static final String PACKAGE="com.touchtype.swiftkey.beta";
    private static final String NAME="日本語入力・予測変換を改善しSamsung絵文字に統一";
    private static Object call(Object obj,String name)throws Exception{return obj.getClass().getMethod(name).invoke(obj);}
    private static Collection<?> load(String path)throws Exception{
        Method method=Class.forName("app.morphe.patcher.patch.PatchKt").getMethod("loadPatchesFromJar",Set.class);
        return (Collection<?>)method.invoke(null,Collections.singleton(new File(path)));
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static SortedMap<String,Object> map(){return new TreeMap<>();}
    private static Object compatibility(Object patch)throws Exception{
        Object value=call(patch,"getCompatiblePackages");
        if(value==null)return null;
        SortedMap<String,Object> targets=map();
        for(Object pair:(Collection<?>)value){
            Object pkg=call(pair,"getFirst"),versions=call(pair,"getSecond");
            List<String> list=null;
            if(versions!=null){list=new ArrayList<>();for(Object v:(Collection<?>)versions)list.add(String.valueOf(v));Collections.sort(list);}
            targets.put(pkg==null?"*":String.valueOf(pkg),list);
        }
        return targets;
    }
    @SuppressWarnings("unchecked")
    private static boolean compatible(Object patch,String pkg)throws Exception{
        Object targets=compatibility(patch);
        return targets==null||((Map<String,Object>)targets).containsKey(pkg)||((Map<String,Object>)targets).containsKey("*");
    }
    private static SortedMap<String,Object> metadata(Object patch)throws Exception{
        SortedMap<String,Object> data=map();
        data.put("name",call(patch,"getName"));data.put("description",call(patch,"getDescription"));
        data.put("default",call(patch,"getDefault"));data.put("compatibility",compatibility(patch));
        List<String> options=new ArrayList<>();for(Object key:((Map<?,?>)call(patch,"getOptions")).keySet())options.add(String.valueOf(key));
        Collections.sort(options);data.put("option_keys",options);return data;
    }
    private static SortedMap<String,Object> named(Collection<?> patches)throws Exception{
        SortedMap<String,Object> result=map();
        for(Object p:patches){Object name=call(p,"getName");if(name!=null){require(!result.containsKey(name),"duplicate named patch "+name);result.put((String)name,metadata(p));}}
        return result;
    }
    private static List<Object> selected(Collection<?> patches)throws Exception{
        List<Object> relevant=new ArrayList<>();
        for(Object p:patches)if(compatible(p,PACKAGE)&&NAME.equals(call(p,"getName")))relevant.add(p);
        // This name is unique for this package, so Manager keyedFor/uniqueKeys retains the plain name.
        require(relevant.size()<=1,"ambiguous SwiftKey selection key");return relevant;
    }
    private static byte[] read(InputStream input)throws Exception{
        try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            require(in!=null,"missing selected runtime extension");
            byte[] buffer=new byte[8192];int count;
            while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
            return out.toByteArray();
        }
    }
    private static String selectedExtension(Object patch,String path)throws Exception{
        // Open the supplier used by the actual loader, rather than only inspecting
        // metadata or the ZIP. Sequential old/new loads also expose stale resources.
        Supplier<?> provider=(Supplier<?>)call(patch,"getExtensionInputStream");
        require(provider!=null,"selected SwiftKey patch has no extension supplier");
        byte[] actual=read((InputStream)provider.get());
        try(ZipFile archive=new ZipFile(path)){
            require(archive.getEntry("extensions/swiftkey_japanese.mpe")!=null,"missing packaged runtime");
            byte[] expected=read(archive.getInputStream(archive.getEntry("extensions/swiftkey_japanese.mpe")));
            require(Arrays.equals(actual,expected),"selected extension supplier differs from packaged runtime: "+path);
        }
        StringBuilder digest=new StringBuilder();
        for(byte value:MessageDigest.getInstance("SHA-256").digest(actual))
            digest.append(String.format(Locale.ROOT,"%02x",value&255));
        return digest.toString();
    }
    private static Object graph(Object patch,Set<Object> stack)throws Exception{
        require(stack.add(patch),"cyclic patch dependency");
        SortedMap<String,Object> data=metadata(patch);data.put("type",patch.getClass().getName());
        List<Object> children=new ArrayList<>();
        for(Object child:(Collection<?>)call(patch,"getDependencies"))children.add(graph(child,stack));
        stack.remove(patch);
        children.sort(Comparator.comparing(LoaderParityProbe::json));data.put("dependencies",children);return data;
    }
    private static void nodes(Object patch,Set<Object> seen,List<String> types)throws Exception{
        if(!seen.add(patch))return;types.add(patch.getClass().getSimpleName());
        for(Object child:(Collection<?>)call(patch,"getDependencies"))nodes(child,seen,types);
    }
    private static String quote(String text){
        StringBuilder out=new StringBuilder("\"");for(char c:text.toCharArray()){
            switch(c){case '\\':out.append("\\\\");break;case '"':out.append("\\\"");break;case '\n':out.append("\\n");break;case '\r':out.append("\\r");break;case '\t':out.append("\\t");break;default:if(c<32)out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}
        }return out.append('"').toString();
    }
    private static String json(Object value){
        if(value==null)return "null";if(value instanceof Boolean||value instanceof Number)return value.toString();
        if(value instanceof Map){StringJoiner out=new StringJoiner(",","{","}");for(Map.Entry<?,?> e:((Map<?,?>)value).entrySet())out.add(quote(String.valueOf(e.getKey()))+":"+json(e.getValue()));return out.toString();}
        if(value instanceof Collection){StringJoiner out=new StringJoiner(",","[","]");for(Object item:(Collection<?>)value)out.add(json(item));return out.toString();}
        return quote(String.valueOf(value));
    }
    public static void main(String[] args)throws Exception{
        require(args.length==4,"oldBundle standalone combined outputJson");
        Collection<?> old=load(args[0]),single=load(args[1]),updated=load(args[2]);
        List<Object> oldChoice=selected(old),singleChoice=selected(single),newChoice=selected(updated);
        require(old.size()==21&&oldChoice.size()==1,"baseline SwiftKey JVM loader restoration must remain present");
        require(single.size()==1&&singleChoice.size()==1,"standalone SwiftKey JVM loading failed");
        require(updated.size()==old.size()&&newChoice.size()==1,"combined SwiftKey JVM loading failed");
        String oldExtension=selectedExtension(oldChoice.get(0),args[0]);
        String singleExtension=selectedExtension(singleChoice.get(0),args[1]);
        String newExtension=selectedExtension(newChoice.get(0),args[2]);
        require(singleExtension.equals(newExtension),"standalone/combined selected extension differs");
        require(oldExtension.equals(newExtension),"existing SwiftKey runtime changed unexpectedly");
        SortedMap<String,Object> oldNames=named(old),newNames=named(updated);
        require(newNames.keySet().containsAll(oldNames.keySet()),"previous patch disappeared");
        for(String name:oldNames.keySet())require(oldNames.get(name).equals(newNames.get(name)),"previous patch metadata changed: "+name);
        Object oldGraph=graph(oldChoice.get(0),Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()));
        Object singleGraph=graph(singleChoice.get(0),Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()));
        Object newGraph=graph(newChoice.get(0),Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()));
        require(singleGraph.equals(newGraph),"standalone/combined dependency graph differs");
        require(oldGraph.equals(newGraph),"baseline SwiftKey dependency graph changed");
        List<String> types=new ArrayList<>();nodes(newChoice.get(0),Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()),types);Collections.sort(types);
        require(types.equals(Arrays.asList("BytecodePatch","BytecodePatch","RawResourcePatch")),"missing original-check/dictionary dependency: "+types);
        SortedMap<String,Object> target=map();target.put(PACKAGE,Arrays.asList("9.13.16.4"));
        require(target.equals(compatibility(newChoice.get(0))),"target compatibility changed");
        require(!compatible(newChoice.get(0),"com.touchtype.swiftkey"),"stable package falsely admitted");
        SortedMap<String,Object> report=map();report.put("status","PASSED");report.put("requested_swiftkey_keys",1);
        report.put("baseline_loaded_named_patches",old.size());report.put("baseline_selected_swiftkey",oldChoice.size());
        report.put("restored_loaded_named_patches",updated.size());report.put("restored_selected_swiftkey",newChoice.size());
        report.put("standalone_selected_swiftkey",singleChoice.size());report.put("previous_patch_metadata_unchanged",oldNames.size());
        report.put("selected_extension_stream_verified",true);
        report.put("loader_selected_extension_matches_packaged_runtime",true);
        report.put("baseline_extension_sha256",oldExtension);
        report.put("selected_extension_sha256",newExtension);
        report.put("baseline_updated_dependency_graph_equal",true);report.put("standalone_combined_dependency_graph_equal",true);report.put("dependency_types",types);report.put("swiftkey_dependency_graph",newGraph);
        report.put("manager_selection_logic","Real loadPatchesFromJar; package compatibility and unique selected-name filter used by Manager keyedFor/selectedPatches");
        report.put("device_tested",false);report.put("clean_apk_apply_tested",false);
        Files.write(Paths.get(args[3]),(json(report)+"\n").getBytes(StandardCharsets.UTF_8));
        System.out.println("PASS JVM loader: selected extension supplier bytes verified; SwiftKey requested 1 -> before 1, updated 1; prior 21 metadata and 3-node dictionary/original-check graph preserved");
    }
}
