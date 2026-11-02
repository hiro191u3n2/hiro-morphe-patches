import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Three explicit extraction requests; all asset/native validation remains intact. */
public final class ResourceReadHooks19124 {
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final String INSTALLER="Lapp/hiro/ulike/patches/IntegrationPayload186;";
 static final String CONTEXT="Lapp/morphe/patcher/patch/ResourcePatchContext;";
 static final String GET=CONTEXT+"->get(Ljava/lang/String;Z)Ljava/io/File;";
 static final String ASSETS=LOADER+"->applyAssets("+CONTEXT+")V";
 static final String RESOLVER=INSTALLER+"->lambda$install$0("+CONTEXT+"Ljava/lang/String;)Ljava/io/File;";
 static final String ASSET_HASH="b72ee5c7f7297d09ccbaa9ac24c44dd78b5bfbc72eec79ae2c0c20f8c2081f3f";
 static final String RESOLVER_HASH="92cbe0fd06d8ca84b643329ebb926fbb7b98d3794b8208e9945950144e1fbcf6";
 static final String OLD_DESCRIPTION="v1.9.123（v1.9.122基準）";
 static final String DESCRIPTION="v1.9.124（v1.9.123基準）Morpheの必要時展開に対応し、元の素材・ネイティブファイルを検査前に取得。素材ハッシュ・衝突検査と撮影時CPUブースト抑止、画質・解像度・保存経路を維持。実機の操作改善は未確認。";
 static void req(boolean v,String m){MergePayloads.require(v,m);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Method body(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static void literal(Instruction i,int register,int value){req(i.getOpcode()==Opcode.CONST_4&&i instanceof OneRegisterInstruction r&&r.getRegisterA()==register&&i instanceof NarrowLiteralInstruction n&&n.getNarrowLiteral()==value,"Pinned boolean literal differs");}
 static FiveRegisterInstruction invocation(Instruction i,int pathRegister,int boolRegister,int receiver){
  req(i.getOpcode()==Opcode.INVOKE_VIRTUAL&&ref(i).equals(GET)&&i instanceof FiveRegisterInstruction,"Pinned resource get call differs");
  var r=(FiveRegisterInstruction)i;req(r.getRegisterCount()==3&&r.getRegisterC()==receiver&&r.getRegisterD()==pathRegister&&r.getRegisterE()==boolRegister,"Pinned resource get registers differ");return r;
 }
 static Method assets(Method m,boolean inverse){
  req(MergePayloads.id(m).equals(ASSETS)&&m.getImplementation().getRegisterCount()==15,"Pinned asset method ABI differs");
  if(!inverse)req(MergePayloads.hash(m).equals(ASSET_HASH),"Published .123 asset method differs");
  var b=new MutableMethodImplementation(m.getImplementation());var calls=new ArrayList<Integer>();
  for(int i=0;i<b.getInstructions().size();i++)if(ref(b.getInstructions().get(i)).equals(GET))calls.add(i);
  req(calls.equals(List.of(68,180)),"Pinned two original-file reads differ");
  // v10 is the unchanged constant true, defined at 37 before every asset read.
  // All paths entering this call pass that definition; no v10 writes intervene.
  literal(b.getInstructions().get(37),10,1);
  invocation(b.getInstructions().get(68),5,inverse?10:4,14);
  b.replaceInstruction(68,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,3,14,5,inverse?4:10,0,0,
    new ImmutableMethodReference(CONTEXT,"get",List.of("Ljava/lang/String;","Z"),"Ljava/io/File;")));
  int second=calls.get(1);invocation(b.getInstructions().get(second),0,4,14);
  if(inverse){literal(b.getInstructions().get(second-2),4,1);b.replaceInstruction(second-2,new BuilderInstruction10x(Opcode.NOP));}
  else{
   req(ref(b.getInstructions().get(second-1)).equals("lib/arm64-v8a/libttvesdk.so"),"Pinned native tail path differs");
   // At this tail v4 has no further use on either the normal or exceptional path,
   // except this boolean parameter. No parameter/register renumbering is needed.
   req(b.getInstructions().get(second-2).getOpcode()==Opcode.NOP,"Pinned one-word tail NOP differs");
   b.replaceInstruction(second-2,new BuilderInstruction11n(Opcode.CONST_4,4,1));
  }
  Method result=body(m,b);if(inverse)req(MergePayloads.hash(result).equals(ASSET_HASH),"Asset guards/control flow changed outside extraction requests");return result;
 }
 static Method resolver(Method m,boolean inverse){
  req(MergePayloads.id(m).equals(RESOLVER)&&m.getImplementation().getRegisterCount()==3,"Pinned installer resolver ABI differs");
  if(!inverse)req(MergePayloads.hash(m).equals(RESOLVER_HASH),"Published .123 resolver differs");
  var b=new MutableMethodImplementation(m.getImplementation());req(b.getInstructions().size()==4,"Pinned resolver size differs");
  literal(b.getInstructions().get(0),0,inverse?1:0);invocation(b.getInstructions().get(1),2,0,1);
  b.replaceInstruction(0,new BuilderInstruction11n(Opcode.CONST_4,0,inverse?0:1));
  Method result=body(m,b);if(inverse)req(MergePayloads.hash(result).equals(RESOLVER_HASH),"Installer resolver changed outside extraction request");return result;
 }
 static Map<String,ClassDef> reads(Map<String,ClassDef> before,boolean inverse){
  var after=new TreeMap<>(before);int count=0;
  for(String type:List.of(LOADER,INSTALLER)){
   ClassDef c=before.get(type);req(c!=null,"Required loader class absent");var ms=new ArrayList<Method>();
   for(Method m:c.getMethods()){
    if(MergePayloads.id(m).equals(ASSETS)){ms.add(assets(m,inverse));count+=2;}
    else if(MergePayloads.id(m).equals(RESOLVER)){ms.add(resolver(m,inverse));count++;}else ms.add(m);
   }after.put(type,members(c,ms));
  }req(count==3,"Exactly three extraction requests must change");return after;
 }
 static String oldDescription(Map<String,ClassDef> old){String value=null;int count=0;
  for(Method m:old.get(LOADER).getMethods())if(m.getImplementation()!=null)for(Instruction x:m.getImplementation().getInstructions())
   if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith(OLD_DESCRIPTION)){value=s.getString();count++;}
  req(count==1,"Pinned original description missing");return value;
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> before,boolean inverse,String original){
  var after=new TreeMap<>(before);var c=before.get(LOADER);var ms=new ArrayList<Method>();int hits=0;
  for(Method m:c.getMethods()){
   if(m.getImplementation()==null){ms.add(m);continue;}var b=new MutableMethodImplementation(m.getImplementation());boolean edited=false;
   for(int i=0;i<b.getInstructions().size();i++){Instruction x=b.getInstructions().get(i);
    if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&(inverse?s.getString().equals(DESCRIPTION):s.getString().equals(original))){
     req(x.getOpcode()==Opcode.CONST_STRING,"Pinned metadata opcode differs");b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(inverse?original:DESCRIPTION)));edited=true;hits++;
    }
   }ms.add(edited?body(m,b):m);
  }req(hits==1,"Exactly one description must change");after.put(LOADER,members(c,ms));return after;
 }
 static Map<String,ClassDef> patch(Map<String,ClassDef> before){return metadata(reads(before,false),false,oldDescription(before));}
 static String verify(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  req(before.keySet().equals(after.keySet()),"Loader class inventory changed");
  var restored=reads(metadata(after,true,oldDescription(before)),true);
  for(var row:before.entrySet())req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(restored.get(row.getKey()))),"Loader class inverse differs: "+row.getKey());
  return "{\"schema\":\"ulike19124-resource-read-dex-v1\",\"status\":\"passed\",\"read_requests_enabled\":3,\"manifest_parent_lookup_preserved\":true,\"exact_inverse_verified\":true,\"dex_inverse_verified\":true,\"instruction_offsets_preserved\":true,\"guard_instructions_preserved\":true,\"no_parameter_register_renumbering\":true,\"old_asset_method_sha256\":\""+ASSET_HASH+"\",\"old_resolver_method_sha256\":\""+RESOLVER_HASH+"\"}";
 }
 static String verify(Path before,Path after)throws Exception{return verify(MergePayloads.classes(before.toString()),MergePayloads.classes(after.toString()));}
 public static void main(String[] a)throws Exception{req(a.length==3,"BASE_DEX NEXT_DEX REPORT_JSON");String r=verify(Path.of(a[0]),Path.of(a[1]));Files.writeString(Path.of(a[2]),r+"\n");System.out.println(r);}
}
