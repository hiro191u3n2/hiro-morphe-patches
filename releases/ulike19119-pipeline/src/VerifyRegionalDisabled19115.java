import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Distributed DEX contract. Positive regional JNI tests are research-only. */
public final class VerifyRegionalDisabled19115 {
 static final String NAME="com.hiro.ulike.GpuRegionalDefault19115",TYPE="Lcom/hiro/ulike/GpuRegionalDefault19115;";
 static final String GSD="Lcom/hiro/ulike/GpuStrongDefault1990;";
 static final Set<String> OPTIONAL_SITES=Set.of(
  GSD+"->regionalState19115(Ljava/lang/String;)Z",
  GSD+"->requestRegional19115()V",
  GSD+"->regionalAccepts19115([I)Z",
  "Lcom/hiro/ulike/GpuStrongDefault1990$Adaptive1994;->regionalBaseline19115(Lcom/hiro/ulike/GpuStrongDefault1990$Fixture;Lcom/hiro/ulike/GpuQualification1961$Cancellation;)Lcom/hiro/ulike/GpuStrongDefault1990$Adaptive1994$Read;",
  "Lcom/hiro/ulike/GpuStrong1960$RegionalHooks19115;->ready(II)Z");
 static void need(boolean value,String message){if(!value)throw new IllegalStateException(message);}
 static boolean forbidden(String value){return value!=null&&value.contains("com/hiro/ulike/GpuRegionalDefault19115");}
 static void type(String value){need(!forbidden(value),"Experimental helper direct type/reference: "+value);}
 static void verify(Map<String,ClassDef> classes){
  Set<String> sites=new TreeSet<>();
  for(ClassDef c:classes.values()){
   type(c.getType());type(c.getSuperclass());for(String i:c.getInterfaces())type(i);
   for(Field f:c.getFields())type(f.getType());
   for(Method m:c.getMethods()){
    type(m.getReturnType());for(CharSequence p:m.getParameterTypes())type(p.toString());
    if(m.getImplementation()==null)continue;
    for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r){
     Reference ref=r.getReference();
     if(ref instanceof StringReference s){String v=s.getString();
      if(v.contains("GpuRegionalDefault19115")){String id=MergePayloads.id(m);need(v.equals(NAME)&&OPTIONAL_SITES.contains(id),"Unreviewed optional reflection "+id+" / "+v);need(sites.add(id),"Repeated optional reflection "+id);}
     }else if(ref instanceof TypeReference t)type(t.getType());
     else if(ref instanceof MethodReference call){type(call.getDefiningClass());type(call.getReturnType());for(CharSequence p:call.getParameterTypes())type(p.toString());}
     else if(ref instanceof FieldReference f){type(f.getDefiningClass());type(f.getType());}
    }
   }
  }
  need(sites.equals(OPTIONAL_SITES),"Exact five optional helper reflection sites required: "+sites);
  var methods=MergePayloads.methods(classes.values());
  for(String name:List.of("profile1994()I","profile1994(II)I","request1994()V","ready()Z"))need(methods.containsKey(GSD+"->"+name),"Existing standard selection API absent: "+name);
  for(String name:List.of("Adaptive1998","Adaptive1994"))need(classes.containsKey(GSD.substring(0,GSD.length()-1)+"$"+name+";"),"Existing adaptive owner absent: "+name);
 }
 static ClassDef cls(String name,Collection<? extends Method> methods,Collection<? extends Field> fields){return new ImmutableClassDef(name,0x11,"Ljava/lang/Object;",List.of(),null,Set.of(),fields,methods);}
 static Method probe(String owner,String result,List<? extends Instruction> instructions){return new ImmutableMethod(owner,"probe",List.of(),result,0x9,Set.of(),Set.of(),new ImmutableMethodImplementation(1,instructions,List.of(),List.of()));}
 static int controls(Map<String,ClassDef> actual){
  int count=0;List<Map<String,ClassDef>> mutations=new ArrayList<>();
  for(String bad:List.of(TYPE,TYPE.substring(0,TYPE.length()-1)+"$Probe;")){var m=new TreeMap<>(actual);m.put(bad,cls(bad,List.of(),List.of()));mutations.add(m);}
  String owner="Lcom/hiro/ulike/Injected19115;";
  var m=new TreeMap<>(actual);m.put(owner,cls(owner,List.of(probe(owner,TYPE,List.of(new ImmutableInstruction11n(Opcode.CONST_4,0,0),new ImmutableInstruction11x(Opcode.RETURN_OBJECT,0)))),List.of()));mutations.add(m);
  m=new TreeMap<>(actual);m.put(owner,cls(owner,List.of(probe(owner,"V",List.of(new ImmutableInstruction21c(Opcode.NEW_INSTANCE,0,new ImmutableTypeReference(TYPE)),new ImmutableInstruction10x(Opcode.RETURN_VOID)))),List.of()));mutations.add(m);
  for(String value:List.of(NAME,NAME+"$Probe")){m=new TreeMap<>(actual);m.put(owner,cls(owner,List.of(probe(owner,"V",List.of(new ImmutableInstruction21c(Opcode.CONST_STRING,0,new ImmutableStringReference(value)),new ImmutableInstruction10x(Opcode.RETURN_VOID)))),List.of()));mutations.add(m);}
  m=new TreeMap<>(actual);m.remove(GSD);mutations.add(m);
  for(var bad:mutations){boolean rejected=false;try{verify(bad);}catch(IllegalStateException expected){rejected=true;}need(rejected,"Experimental-helper DEX mutation accepted");count++;}
  need(count==7,"All DEX negative controls executed");return count;
 }
 public static void main(String[] args)throws Exception{
  need(args.length==1,"RUNTIME_DEX_OR_APK");var classes=MergePayloads.classes(args[0]);verify(classes);int count=controls(classes);
  System.out.println("{\"status\":\"passed\",\"experimental_helper_and_nested_absent\":true,\"direct_experimental_references_absent\":true,\"standard_selection_apis_preserved\":true,\"optional_reflection_sites\":5,\"negative_controls\":"+count+",\"distributed_regional_auto_adoption\":false,\"physical_android_tested\":false}");
 }
}
