import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
public final class CaptureInvariant19104Test {
 static int assertions;static final String OWNER=VerifyCaptureInvariant19104.OWNER;
 static final List<String> PARAMETERS=List.of("Ljava/lang/Object;","Landroid/media/Image;","Landroid/hardware/camera2/TotalCaptureResult;");
 static final ImmutableMethodReference TIMING=new ImmutableMethodReference("Lcom/hiro/ulike/ShotContext1932;","q0Entry19104",List.of("Ljava/lang/Object;","Landroid/media/Image;","Landroid/hardware/camera2/CaptureResult;"),"V");
 static final ImmutableMethodReference LEGACY=new ImmutableMethodReference(OWNER,"legacyAdmissionDisabled19104",PARAMETERS,"Z");
 static List<Instruction> good(){return List.of(new ImmutableInstruction35c(Opcode.INVOKE_STATIC,3,1,2,3,0,0,TIMING),new ImmutableInstruction11n(Opcode.CONST_4,0,0),new ImmutableInstruction11x(Opcode.RETURN,0));}
 static ImmutableMethod method(String owner,String name,List<String> params,String result,int flags,int regs,List<Instruction> ins){
  List<ImmutableMethodParameter> p=new ArrayList<>();for(String type:params)p.add(new ImmutableMethodParameter(type,Set.of(),null));
  return new ImmutableMethod(owner,name,p,result,flags,Set.of(),Set.of(),new ImmutableMethodImplementation(regs,ins,List.of(),List.of()));
 }
 static ClassDef cls(String owner,Collection<? extends Method> methods){return new ImmutableClassDef(owner,0x11,"Ljava/lang/Object;",List.of(),null,Set.of(),List.of(),methods);}
 static Map<String,ClassDef> fixture(List<Instruction> body,int flags,boolean legacy,boolean incoming){
  Map<String,ClassDef> classes=new HashMap<>();List<Method> methods=new ArrayList<>();methods.add(method(OWNER,"beginImage",PARAMETERS,"Z",flags,4,body));
  if(legacy)methods.add(method(OWNER,"legacyAdmissionDisabled19104",PARAMETERS,"Z",0xa,3,List.of(new ImmutableInstruction11n(Opcode.CONST_4,0,0),new ImmutableInstruction11x(Opcode.RETURN,0))));
  classes.put(OWNER,cls(OWNER,methods));String timing=TIMING.getDefiningClass();classes.put(timing,cls(timing,List.of(method(timing,"q0Entry19104",List.of("Ljava/lang/Object;","Landroid/media/Image;","Landroid/hardware/camera2/CaptureResult;"),"V",0x9,3,List.of(new ImmutableInstruction10x(Opcode.RETURN_VOID))))));
  if(incoming){String caller="Lcom/hiro/ulike/BurstCapture1933$Retained;";classes.put(caller,cls(caller,List.of(method(caller,"unsafe",PARAMETERS,"V",0x9,3,List.of(new ImmutableInstruction35c(Opcode.INVOKE_STATIC,3,0,1,2,0,0,LEGACY),new ImmutableInstruction10x(Opcode.RETURN_VOID))))));}
  return classes;
 }
 static void check(Map<String,ClassDef> classes,boolean accepted){try{VerifyCaptureInvariant19104.verify(classes);if(!accepted)throw new AssertionError("Unsafe single-shot admission accepted");}catch(IllegalStateException|IllegalArgumentException error){if(accepted)throw error;}assertions++;}
 public static void main(String[] args){
  check(fixture(good(),0x9,true,false),true);
  List<Instruction> range=new ArrayList<>(good());range.set(0,new ImmutableInstruction3rc(Opcode.INVOKE_STATIC_RANGE,1,3,TIMING));check(fixture(range,0x9,true,false),true);
  for(int literal:List.of(1,-1)){List<Instruction> bad=new ArrayList<>(good());bad.set(1,new ImmutableInstruction11n(Opcode.CONST_4,0,literal));check(fixture(bad,0x9,true,false),false);}
  for(int flags:List.of(0x1,0xa,0x29,0x109))check(fixture(good(),flags,true,false),false);
  check(fixture(good(),0x9,false,false),false);check(fixture(good(),0x9,true,true),false);
  List<Instruction> bad=new ArrayList<>(good());bad.set(0,new ImmutableInstruction35c(Opcode.INVOKE_STATIC,3,1,2,3,0,0,LEGACY));check(fixture(bad,0x9,true,false),false);
  bad=new ArrayList<>(good());bad.add(0,new ImmutableInstruction10x(Opcode.NOP));check(fixture(bad,0x9,true,false),false);
  bad=new ArrayList<>(good());bad.remove(0);check(fixture(bad,0x9,true,false),false);
  bad=new ArrayList<>(good());bad.set(0,new ImmutableInstruction35c(Opcode.INVOKE_STATIC,3,0,2,3,0,0,TIMING));check(fixture(bad,0x9,true,false),false);
  bad=new ArrayList<>(good());bad.set(2,new ImmutableInstruction11x(Opcode.RETURN,1));check(fixture(bad,0x9,true,false),false);
  Map<String,ClassDef> missing=fixture(good(),0x9,true,false);missing.remove(OWNER);check(missing,false);
  missing=fixture(good(),0x9,true,false);missing.remove(TIMING.getDefiningClass());check(missing,false);
  System.out.println("PASS single-capture-invariant assertions="+assertions);
 }
}
