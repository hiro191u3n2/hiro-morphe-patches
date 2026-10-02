import java.nio.file.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.immutable.reference.*;import com.android.tools.smali.dexlib2.builder.*;import com.android.tools.smali.dexlib2.builder.instruction.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;
/** Minimal repair: pure capability-query bridges; early preview-host registration.
 * The original application hooks and all lens/AF/session/save code stay byte-exact. */
public final class Transform177 {
 static final String Z="Lcom/hiro/ulike/OpticalZoom;",P="Lcom/hiro/ulike/Preview177;",U="Lcom/hiro/ulike/OpticalZoomUi;",A="Lcom/hiro/ulike/CaptureAdvanced3;";
 static ImmutableMethod copy(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static boolean calls(Method m,String owner,String name){if(m.getImplementation()==null)return false;for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction ri&&ri.getReference() instanceof MethodReference ref&&ref.getDefiningClass().equals(owner)&&ref.getName().equals(name))return true;return false;}
 public static void main(String[] args)throws Exception{
  var old=MergePayloads.classes(args[0]);var all=new TreeMap<>(old);var helper=MergePayloads.classes(args[1]);Path out=Path.of(args[2]);Files.createDirectories(out);
  MergePayloads.require(helper.size()==1&&helper.containsKey(P)&&!old.containsKey(P),"Only new reviewed helper may be added");all.putAll(helper);
  var before=MergePayloads.methods(old.values());int poison=0;for(Method m:before.values())if(m.getDefiningClass().equals(Z)&&m.getName().equals("outputSizes")){MergePayloads.require(calls(m,Z,"fail")&&calls(m,Z,"common"),"Expected old poison paths not found");poison++;}
  MergePayloads.require(poison==2,"Both predecessor capability-query paths must be recognized");
  int outputs=0,attach=0,version=0;Set<String> allowed=new HashSet<>();
  for(String type:List.of(Z,U,A)){
   ClassDef c=all.get(type);List<Method> methods=new ArrayList<>();
   for(Method m:c.getMethods()){
    var impl=m.getImplementation();boolean changed=false;
    if(type.equals(Z)&&m.getName().equals("outputSizes")){
     var b=new MutableMethodImplementation(2);List<String> params=new ArrayList<>();for(var t:m.getParameterTypes())params.add(t.toString());
     MergePayloads.require(params.size()==2&&params.get(0).equals("Landroid/hardware/camera2/params/StreamConfigurationMap;"),"Unexpected query signature");
     b.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,0,1,0,0,0,new ImmutableMethodReference(P,"outputSizes",params,m.getReturnType())));b.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,0));b.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT,0));impl=b;outputs++;changed=true;
    }
    if(type.equals(U)&&m.getName().equals("attach")){
     MergePayloads.require(!calls(m,Z,"host")&&!calls(m,P,"attached"),"Host was already registered before row readiness");
     var b=new MutableMethodImplementation(impl);int reg=b.getRegisterCount()-1;
     b.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,reg,1,new ImmutableMethodReference(P,"attached",List.of("Landroid/view/View;"),"V")));impl=b;attach++;changed=true;
    }
    if(type.equals(A)&&impl!=null){var b=new MutableMethodImplementation(impl);for(int n=0;n<b.getInstructions().size();n++){
     var i=b.getInstructions().get(n);if(i instanceof ReferenceInstruction ri&&ri.getReference() instanceof StringReference sr&&sr.getString().equals("ULike Capture v1.7.6\n")){
      int reg=((OneRegisterInstruction)i).getRegisterA();var ref=new ImmutableStringReference("ULike Capture v1.7.7\n");b.replaceInstruction(n,i.getOpcode()==Opcode.CONST_STRING_JUMBO?new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,reg,ref):new BuilderInstruction21c(Opcode.CONST_STRING,reg,ref));version++;changed=true;
     }}if(changed)impl=b;
    }
    if(changed)allowed.add(MergePayloads.id(m));methods.add(copy(m,impl));
   }
   all.put(type,new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  MergePayloads.require(outputs==2&&attach==1&&version==1,"Exact patch counts required");MergePayloads.writeDex(out.resolve("runtime.dex"),all.values());
  var emitted=MergePayloads.classes(out.resolve("runtime.dex").toString());var after=MergePayloads.methods(emitted.values());int kept=0,changed=0,added=0;List<String> audit=new ArrayList<>();
  for(var e:before.entrySet()){Method now=after.get(e.getKey());MergePayloads.require(now!=null,"Removed original method "+e.getKey());if(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(now)))kept++;else{MergePayloads.require(allowed.contains(e.getKey()),"Unrelated original changed "+e.getKey());changed++;audit.add("changed\t"+e.getKey());}}
  for(var e:after.entrySet())if(!before.containsKey(e.getKey())){MergePayloads.require(e.getValue().getDefiningClass().equals(P),"Unreviewed added class");added++;}
  for(var e:old.entrySet())if(!Set.of(Z,U,A).contains(e.getKey()))MergePayloads.require(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(emitted.get(e.getKey()))),"Unrelated class changed "+e.getKey());
  for(Method m:emitted.get(P).getMethods())MergePayloads.require(!calls(m,Z,"fail")&&!calls(m,"Lcom/hiro/ulike/LensLifecycle172;","fail"),"Capability inspection must not mark live camera failed");
  audit.add("PASS runtime: preserved="+kept+", changed="+changed+", added="+added+", removed=0, total="+after.size()+"; both query bridges, early host registration, diagnostic version only. All other classes identical.");Files.write(out.resolve("runtime-audit.tsv"),audit);System.out.println(audit.get(audit.size()-1));
  Files.writeString(out.resolve("predecessor-fault-evidence.txt"),"Old DEX has fail() in both outputSizes catch paths and common() on empty intersection. Old UI attach does not register a host; Bar.updateCore168 calls host only after geometry, visibility and collision checks. Verified predecessor method hashes in runtime-audit. Device symptom cause not independently reproduced.\n");
 }
}
