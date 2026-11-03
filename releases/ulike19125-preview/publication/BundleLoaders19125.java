import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Insert the exact reviewed standalone loader into an unchanged current bundle. */
public final class BundleLoaders19125 {
    static final String PREFIX="Lapp/hiro/ulike/patches/";
    static final String LOADER=PREFIX+"UlikeHqMaxPatch;";
    static void require(boolean condition,String message){MergePayloads.require(condition,message);}
    static String quote(String value){return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    static ClassDef restoreMetadata(ClassDef old,ClassDef next){
        String description=null;int oldHits=0,hits=0;
        for(Method m:old.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())
            if(i instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith("v1.9.124（v1.9.123基準）")){description=s.getString();oldHits++;}
        require(oldHits==1,"Exactly one pinned .124 display description required");
        var methods=new ArrayList<Method>();
        for(Method m:next.getMethods()){
            if(m.getImplementation()==null){methods.add(m);continue;}
            var body=new MutableMethodImplementation(m.getImplementation());boolean changed=false;
            for(int j=0;j<body.getInstructions().size();j++){
                Instruction i=body.getInstructions().get(j);
                if(i instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith("v1.9.125（v1.9.124基準）")){
                    require(i.getOpcode()==Opcode.CONST_STRING,"Display metadata opcode changed");
                    body.replaceInstruction(j,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)i).getRegisterA(),new ImmutableStringReference(description)));hits++;changed=true;
                }
            }
            methods.add(changed?new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body):m);
        }
        require(hits==1,"Exactly one new display description required");
        return new ImmutableClassDef(next.getType(),next.getAccessFlags(),next.getSuperclass(),next.getInterfaces(),next.getSourceFile(),next.getAnnotations(),next.getFields(),methods);
    }
    public static void main(String[] args)throws Exception{
        require(args.length==5,"CURRENT_BUNDLE_DEX REFERENCE_123_DEX NEW_124_DEX OUTPUT_DEX REPORT_JSON");
        var current=MergePayloads.classes(args[0]);
        var reference=MergePayloads.classes(args[1]);
        var single=MergePayloads.classes(args[2]);
        var owned=new TreeSet<String>();
        for(String type:current.keySet())if(type.startsWith(PREFIX))owned.add(type);
        require(owned.equals(reference.keySet())&&owned.equals(single.keySet()),"Exact ULike loader inventory required");
        int differences=0;
        for(String type:owned){
            require(MergePayloads.classHash(current.get(type)).equals(MergePayloads.classHash(reference.get(type))),"Current bundle ULike loader differs from pinned 123: "+type);
            if(!MergePayloads.classHash(reference.get(type)).equals(MergePayloads.classHash(single.get(type)))){
                require(type.equals(LOADER)||type.equals(PREFIX+"IntegrationPayload186;"),"Unexpected standalone loader change: "+type);differences++;
            }
        }
        require(differences==1,"Only the reviewed display metadata class may change");
        for(String type:owned){
            ClassDef restored=type.equals(LOADER)?restoreMetadata(reference.get(type),single.get(type)):single.get(type);
            require(MergePayloads.classHash(reference.get(type)).equals(MergePayloads.classHash(restored)),"Executable loader, resource guard or installer changed: "+type);
        }
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
