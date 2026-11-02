import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Deliberately damage serialized candidate loaders and ensure the full-inverse verifier rejects each. */
public final class ResourceStructure19124 {
 static int checks, rejected;
 static void req(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static Map<String,ClassDef> mutate(Map<String,ClassDef> original,String id){
  var output=new TreeMap<>(original);String type=id.equals("native-resolver")||id.equals("manifest-parent")?ResourceReadHooks19124.INSTALLER:ResourceReadHooks19124.LOADER;
  ClassDef cls=original.get(type);var methods=new ArrayList<Method>();int changes=0;
  for(Method m:cls.getMethods()){
   if(m.getImplementation()==null){methods.add(m);continue;}
   var code=new MutableMethodImplementation(m.getImplementation());boolean edited=false;
   if(id.equals("remove-required-input-guard")&&m.getName().equals("require")&&m.getReturnType().equals("V")&&m.getParameterTypes().toString().equals("[Z, Ljava/lang/String;]")){
    methods.add(ResourceReadHooks19124.body(m,new ImmutableMethodImplementation(m.getImplementation().getRegisterCount(),List.of(new com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x(Opcode.RETURN_VOID)),List.of(),List.of())));changes++;continue;
   }
   if(ResourceReadHooks19124.ASSETS.equals(MergePayloads.id(m))){
    if(id.equals("asset-read")){var get=(FiveRegisterInstruction)code.getInstructions().get(68);req(get.getRegisterE()==10,"healthy asset read uses true register");code.replaceInstruction(68,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,3,get.getRegisterC(),get.getRegisterD(),4,0,0,new ImmutableMethodReference(ResourceReadHooks19124.CONTEXT,"get",List.of("Ljava/lang/String;","Z"),"Ljava/io/File;")));edited=true;}
    if(id.equals("original-native-read")){req(code.getInstructions().get(178).getOpcode()==Opcode.CONST_4,"healthy native bool");code.replaceInstruction(178,new BuilderInstruction10x(Opcode.NOP));edited=true;}
   }
   if(ResourceReadHooks19124.RESOLVER.equals(MergePayloads.id(m))&&id.equals("native-resolver")){code.replaceInstruction(0,new BuilderInstruction11n(Opcode.CONST_4,0,0));edited=true;}
   if(type.equals(ResourceReadHooks19124.INSTALLER)&&m.getName().equals("install")&&id.equals("manifest-parent")){
    int target=-1;
    for(int i=0;i<code.getInstructions().size();i++)if(ResourceReadHooks19124.ref(code.getInstructions().get(i)).equals(ResourceReadHooks19124.GET)){
     req(target==-1,"one manifest parent lookup");var call=(FiveRegisterInstruction)code.getInstructions().get(i);int reg=call.getRegisterE();
     for(int k=i-1;k>=0;k--){Instruction ins=code.getInstructions().get(k);if(ins instanceof OneRegisterInstruction r&&r.getRegisterA()==reg&&ins instanceof NarrowLiteralInstruction lit&&lit.getNarrowLiteral()==0){target=k;break;}}
    }
    req(target>=0,"manifest false definition found");var old=(OneRegisterInstruction)code.getInstructions().get(target);code.replaceInstruction(target,new BuilderInstruction11n(Opcode.CONST_4,old.getRegisterA(),1));edited=true;
   }
   methods.add(edited?ResourceReadHooks19124.body(m,code):m);if(edited)changes++;
  }
  req(changes==1,"single deliberately changed method "+id);output.put(type,ResourceReadHooks19124.members(cls,methods));return output;
 }
 public static void main(String[] args)throws Exception {
  req(args.length==4,"BASE_EXTRACT CANDIDATE_EXTRACT BEHAVIOR_WORK WORK");Path base=Path.of(args[0]),candidate=Path.of(args[1]),behavior=Path.of(args[2]),work=Path.of(args[3]);Files.createDirectories(work);
  String proof=ResourceReadHooks19124.verify(base.resolve("classes.dex"),candidate.resolve("classes.dex"));req(proof.contains("\"exact_inverse_verified\":true"),"healthy DEX inverse");
  for(String name:List.of("UlikeHqMaxPatch","IntegrationPayload186")){String p="app/hiro/ulike/patches/"+name+".class";String v=ResourceClass19124.verify(base.resolve(p),candidate.resolve(p));req(v.contains("\"jvm_inverse_verified\":true"),"healthy JVM inverse "+name);}
  int healthyChecks=checks;
  var dex=MergePayloads.classes(candidate.resolve("classes.dex").toString());
  for(String id:List.of("asset-read","original-native-read","native-resolver","manifest-parent","remove-required-input-guard")){
   Path bad=work.resolve(id+".dex");MergePayloads.writeDex(bad,mutate(dex,id).values());boolean detected=false;
   try{ResourceReadHooks19124.verify(base.resolve("classes.dex"),bad);}catch(IllegalStateException expected){detected=true;}
   req(detected,"DEX mutation escaped inverse "+id);rejected++;
   String name=id.equals("native-resolver")||id.equals("manifest-parent")?"IntegrationPayload186":"UlikeHqMaxPatch";detected=false;
   try{ResourceClass19124.verify(base.resolve("app/hiro/ulike/patches/"+name+".class"),behavior.resolve("structural-mutants/"+id+".class"));}catch(IllegalStateException expected){detected=true;}
   req(detected,"JVM mutation escaped inverse "+id);rejected++;
  }
  System.out.println("{\"status\":\"passed\",\"healthy_structural_checks\":"+healthyChecks+",\"mutation_checks\":"+(checks-healthyChecks)+",\"structural_mutations_rejected\":"+rejected+"}");
 }
}
