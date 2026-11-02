import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.formatter.*;
import app.hiro.ulike.patches.MethodContract;
public class IndependentCallsiteReview {
 static final String X="Li/o/a/b1/a/g/y;->X(IZ)Z", OLD="Li/o/a/p/b;->g()V", NEW="Lcom/hiro/ulike/CaptureBoost19123;->suppress(Ljava/lang/Object;)V";
 static final String BEFORE="7915f1efa39a0ef7f69dc54751732002276c53fa6b72699ca5fde655755d2dc8";
 static int assertions, negatives;
 static void check(boolean v,String s){assertions++;if(!v)throw new IllegalStateException(s);}
 static Map<String,Method> methods(Path p)throws Exception {Map<String,Method> r=new TreeMap<>();var d=DexFileFactory.loadDexContainer(p.toFile(),Opcodes.forApi(26));for(String e:d.getDexEntryNames())for(ClassDef c:d.getEntry(e).getDexFile().getClasses())for(Method m:c.getMethods())check(r.put(DexFormatter.INSTANCE.getMethodDescriptor(m),m)==null,"unique method");return r;}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Method wrap(Method m,MethodImplementation impl){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl);}
 static void callsite(Method before,Method next){
  check(MethodContract.sha256(before).equals(BEFORE),"pinned v122 shutter");
  check(before.getAccessFlags()==next.getAccessFlags()&&before.getParameters().equals(next.getParameters())&&before.getAnnotations().equals(next.getAnnotations()),"header preserved");
  var a=new MutableMethodImplementation(before.getImplementation());var b=new MutableMethodImplementation(next.getImplementation());
  check(a.getRegisterCount()==9&&b.getRegisterCount()==9,"all 9 registers preserved");
  check(a.getInstructions().size()==b.getInstructions().size(),"same instruction count");
  int beforeCalls=0, afterCalls=0, offsetA=0, offsetB=0;
  for(int j=0;j<a.getInstructions().size();j++){
   Instruction left=a.getInstructions().get(j),right=b.getInstructions().get(j);
   check(left.getCodeUnits()==right.getCodeUnits(),"instruction width remains at "+j);
   if(ref(left).equals(OLD)){
    beforeCalls++;
    check(left.getOpcode()==Opcode.INVOKE_VIRTUAL&&right.getOpcode()==Opcode.INVOKE_STATIC,"virtual becomes static only at audited boost site");
    check(ref(right).equals(NEW),"only suppression helper called");
    check(left instanceof FiveRegisterInstruction&&right instanceof FiveRegisterInstruction,"non-range invoke shape");
    FiveRegisterInstruction l=(FiveRegisterInstruction)left,r=(FiveRegisterInstruction)right;
    check(l.getRegisterCount()==1&&r.getRegisterCount()==1&&l.getRegisterC()==0&&r.getRegisterC()==0,"same one receiver register v0");
    check(offsetA==0x18&&offsetB==0x18,"pinned boost code unit address");
    b.replaceInstruction(j,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,1,0,0,0,0,0,new ImmutableMethodReference("Li/o/a/p/b;","g",List.of(),"V")));
   }
   if(ref(right).equals(NEW))afterCalls++;
   offsetA+=left.getCodeUnits();offsetB+=right.getCodeUnits();
  }
  check(beforeCalls==1&&afterCalls==1,"exactly one old and one new call");
  check(MethodContract.sha256(wrap(next,b)).equals(BEFORE),"inverse restores entire shutter including permission/readiness/countdown/branches/returns");
 }
 static void negative(String label,Runnable run){try{run.run();throw new AssertionError("accepted negative "+label);}catch(IllegalStateException expected){negatives++;}}
 public static void main(String[]args)throws Exception{
  if(args.length<2)throw new IllegalArgumentException("baseline methods.dex next methods.dex [runtime.dex] [appliedAPK]");
  Map<String,Method> a=methods(Path.of(args[0])),b=methods(Path.of(args[1]));check(a.keySet().equals(b.keySet()),"payload method inventory preserved");
  List<String> changed=new ArrayList<>();for(String key:a.keySet())if(!MethodContract.sha256(a.get(key)).equals(MethodContract.sha256(b.get(key))))changed.add(key);
  check(changed.equals(List.of(X)),"only shutter method changed in original-method payload");callsite(a.get(X),b.get(X));
  Method before=a.get(X),next=b.get(X);
  negative("suppression removed",()->callsite(before,before));
  var wrongReady=new MutableMethodImplementation(next.getImplementation());int ready=0;
  for(int i=0;i<wrongReady.getInstructions().size();i++)if(ref(wrongReady.getInstructions().get(i)).equals("Lcom/hiro/ulike/AsyncSave1935;->readiness(Ljava/lang/Object;)I")){wrongReady.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,1,6,0,0,0,0,new ImmutableMethodReference("Li/o/a/b1/a/g/y;","D1",List.of(),"I")));ready++;}
  check(ready==1,"readiness mutation exercises actual guard");negative("next-capture guard lost",()->callsite(before,wrap(next,wrongReady)));
  var wrongReturn=new MutableMethodImplementation(next.getImplementation());int returns=0;
  for(int i=0;i<wrongReturn.getInstructions().size();i++)if(wrongReturn.getInstructions().get(i).getOpcode()==Opcode.RETURN){int reg=((OneRegisterInstruction)wrongReturn.getInstructions().get(i)).getRegisterA();wrongReturn.replaceInstruction(i,new BuilderInstruction11x(Opcode.RETURN,reg==4?5:4));returns++;break;}
  check(returns==1,"return mutation exercises original rejection result");negative("permission/readiness failure return changed",()->callsite(before,wrap(next,wrongReturn)));
  var wrongReceiver=new MutableMethodImplementation(next.getImplementation());for(int i=0;i<wrongReceiver.getInstructions().size();i++)if(ref(wrongReceiver.getInstructions().get(i)).equals(NEW))wrongReceiver.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,6,0,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/CaptureBoost19123;","suppress",List.of("Ljava/lang/Object;"),"V")));
  negative("wrong receiver",()->callsite(before,wrap(next,wrongReceiver)));
  if(args.length>=3){var runtime=methods(Path.of(args[2]));Method helper=runtime.get(NEW);check(helper!=null,"suppression implementation exists");check((helper.getAccessFlags()&0x9)==0x9,"helper public static");int enters=0,exits=0;for(Instruction op:helper.getImplementation().getInstructions()){String r=ref(op);check(!(op instanceof ReferenceInstruction)||(r.equals("Lcom/hiro/ulike/CaptureBoost19123;->suppressed:J")||r.equals("Lcom/hiro/ulike/CaptureBoost19123;")),"counter-only helper has no receiver or SDK access: "+r);check(op.getOpcode()!=Opcode.IPUT_OBJECT&&op.getOpcode()!=Opcode.SPUT_OBJECT,"no retained receiver");if(op.getOpcode()==Opcode.MONITOR_ENTER)enters++;if(op.getOpcode()==Opcode.MONITOR_EXIT)exits++;}check(enters==1&&exits==2&&helper.getImplementation().getTryBlocks().size()==1,"D8 synchronized monitor and exceptional exit preserved");}
  if(args.length>=4){var apk=methods(Path.of(args[3]));check(MethodContract.sha256(apk.get(X)).equals(MethodContract.sha256(next)),"actual applied shutter matches tested change");check(MethodContract.sha256(apk.get(OLD)).equals("3670c64414db260e1f7d4370f84353d651e79d71013ab7b5f134e5f328bd89bc"),"original g body retained in installed app");}
  System.out.println("PASS independent callsite review assertions="+assertions+" negatives="+negatives+" only_original_payload_change="+X+" full_inverse=true physical_device_tested=false");
 }
}
