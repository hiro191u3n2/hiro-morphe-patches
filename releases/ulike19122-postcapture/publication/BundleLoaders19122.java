import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Insert the exact reviewed standalone loader into an unchanged current bundle. */
public final class BundleLoaders19122 {
    static final String PREFIX="Lapp/hiro/ulike/patches/";
    static final String LOADER=PREFIX+"UlikeHqMaxPatch;";
    static void require(boolean condition,String message){MergePayloads.require(condition,message);}
    static String quote(String value){return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    public static void main(String[] args)throws Exception{
        require(args.length==5,"CURRENT_BUNDLE_DEX REFERENCE_121_DEX NEW_122_DEX OUTPUT_DEX REPORT_JSON");
        var current=MergePayloads.classes(args[0]);
        var reference=MergePayloads.classes(args[1]);
        var single=MergePayloads.classes(args[2]);
        var owned=new TreeSet<String>();
        for(String type:current.keySet())if(type.startsWith(PREFIX))owned.add(type);
        require(owned.equals(reference.keySet())&&owned.equals(single.keySet()),"Exact ULike loader inventory required");
        int differences=0;
        for(String type:owned){
            require(MergePayloads.classHash(current.get(type)).equals(MergePayloads.classHash(reference.get(type))),"Current bundle ULike loader differs from pinned 121: "+type);
            if(!MergePayloads.classHash(reference.get(type)).equals(MergePayloads.classHash(single.get(type)))){
                require(type.equals(LOADER),"Unexpected standalone loader change: "+type);differences++;
            }
        }
        require(differences==1,"Exactly the reviewed display loader changes");
        var combined=new TreeMap<>(current);combined.putAll(single);
        MergePayloads.writeDex(Path.of(args[3]),combined.values());
        var emitted=MergePayloads.classes(args[3]);
        require(emitted.keySet().equals(current.keySet()),"Bundle class inventory changed");
        int preservedClasses=0,preservedMethods=0;
        var hashes=new TreeMap<String,String>();
        for(var row:emitted.entrySet()){
            String type=row.getKey();ClassDef expected=owned.contains(type)?single.get(type):current.get(type);
            String actual=MergePayloads.classHash(row.getValue());
            require(actual.equals(MergePayloads.classHash(expected)),"Serialized class changed: "+type);
            if(!owned.contains(type)){
                preservedClasses++;for(Method method:expected.getMethods())preservedMethods++;
                hashes.put(type,actual);
            }
        }
        var rows=new ArrayList<String>();for(var row:hashes.entrySet())rows.add(quote(row.getKey())+":"+quote(row.getValue()));
        Files.writeString(Path.of(args[4]),"{\n\"status\":\"passed\",\n\"unrelated_classes\":"+preservedClasses+",\n\"unrelated_methods\":"+preservedMethods+",\n\"ulike_classes\":"+owned.size()+",\n\"unrelated_class_sha256\":{\n"+String.join(",\n",rows)+"\n}\n}\n");
        System.out.println("PASS current bundle merge: unrelated classes="+preservedClasses+" unrelated methods="+preservedMethods+" exact ULike classes="+owned.size());
    }
}
