import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Narrow candidate hotfix over exact 1919. No withdrawn code or image-math changes. */
public final class Transform1920 {
 static final String P="Lcom/hiro/ulike/", C=P+"CaptureAdvanced3;", STATE=P+"CaptureAdvanced3$State;", B=P+"BackExit185;";
 static final String OBJ="Ljava/lang/Object;", ACT="Landroid/app/Activity;", DEV="Landroid/hardware/camera2/CameraDevice;", CFG="Landroid/hardware/camera2/params/SessionConfiguration;";
 static final String DESCRIPTION="v1.9.20 修正候補（v1.9.19基準）微調整・スタイル・美顔・フィルタ等はULike標準の戻る処理を先に実行し、閉じる画面がない場合のみ従来の完全終了。最大センサーモードのセッション構成が10秒応答しない場合は既存の通常モード復帰処理へ。黒画面の端末固有原因は未確定・実機未検証。";
 static final List<String> AUDIT=new ArrayList<>();
 static void check(boolean b,String message){MergePayloads.require(b,message);}
 static List<Instruction> instructions(Method m){var out=new ArrayList<Instruction>();m.getImplementation().getInstructions().forEach(out::add);return out;}
 static Method method(Method m,String name,MethodImplementation implementation){return new ImmutableMethod(m.getDefiningClass(),name,m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),implementation);}
 static Method make(String owner,String name,List<String> parameters,String result,int flags,int registers,List<Instruction> body){
  var ps=new ArrayList<ImmutableMethodParameter>();for(String p:parameters)ps.add(new ImmutableMethodParameter(p,Set.of(),null));
  return new ImmutableMethod(owner,name,ps,result,flags,Set.of(),Set.of(),new ImmutableMethodImplementation(registers,body,List.of(),List.of()));
 }
 static Instruction call(Opcode op,int count,int c,int d,int e, String owner,String name,List<String> ps,String result){return new ImmutableInstruction35c(op,count,c,d,e,0,0,new ImmutableMethodReference(owner,name,ps,result));}
 static List<Method> additions(String owner){
  if(owner.equals(B)) return List.of(make(B,"requestOnMain1920",List.of(B,ACT),"V",9,2,List.of(call(Opcode.INVOKE_DIRECT,2,0,1,0,B,"request",List.of(ACT),"V"),new ImmutableInstruction10x(Opcode.RETURN_VOID))));
  if(!owner.equals(C)) return List.of();
  return List.of(
   make(C,"state1920",List.of(OBJ),STATE,8,2,List.of(
    new ImmutableInstruction21c(Opcode.SGET_OBJECT,0,new ImmutableFieldReference(C,"states","Ljava/util/Map;")),
    call(Opcode.INVOKE_INTERFACE,2,0,1,0,"Ljava/util/Map;","get",List.of(OBJ),OBJ),
    new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT,0),new ImmutableInstruction21c(Opcode.CHECK_CAST,0,new ImmutableTypeReference(STATE)),new ImmutableInstruction11x(Opcode.RETURN_OBJECT,0))),
   make(C,"current1920",List.of(OBJ,STATE),"Z",8,2,List.of(call(Opcode.INVOKE_STATIC,2,0,1,0,C,"current",List.of(OBJ,STATE),"Z"),new ImmutableInstruction11x(Opcode.MOVE_RESULT,0),new ImmutableInstruction11x(Opcode.RETURN,0))),
   make(C,"retry1920",List.of(OBJ,STATE,"Ljava/lang/Throwable;"),"V",8,3,List.of(call(Opcode.INVOKE_STATIC,3,0,1,2,C,"restore",List.of(OBJ,STATE,"Ljava/lang/Throwable;"),"V"),new ImmutableInstruction10x(Opcode.RETURN_VOID)))
  );
 }
 static Map<String,ClassDef> transform(Map<String,ClassDef> before, boolean runtime){
  Map<String,ClassDef> after=new TreeMap<>();
  for(ClassDef c:before.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(Method m:c.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}
    var ins=instructions(m);boolean edited=false;
    for(int i=0;i<ins.size();i++){
     if(ins.get(i) instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s && s.getString().startsWith("v1.9.19（v1.8.8基準）")){
      int a=((OneRegisterInstruction)ins.get(i)).getRegisterA();check(ins.get(i).getOpcode()==Opcode.CONST_STRING,"String format drift");ins.set(i,new ImmutableInstruction21c(Opcode.CONST_STRING,a,new ImmutableStringReference(DESCRIPTION)));edited=true;
     }
    }
    if(runtime && c.getType().equals(B) && m.getName().equals("request")){
     check(m.getImplementation().getRegisterCount()==4,"Back register drift");
     check(ins.get(0) instanceof ReferenceInstruction r && r.getReference().toString().equals(P+"SettingsReturn1918;->returnToCameraIfSettings("+ACT+")Z"),"Back gate drift");
     ins.set(0,call(Opcode.INVOKE_STATIC,2,2,3,0,P+"BackRoute1920;","consume",List.of(B,ACT),"Z"));edited=true;
    }
    if(runtime && c.getType().equals(P+"CaptureAdvanced3$1;") && m.getName().equals("onConfigured")){
     String callback=c.getType(), session="Landroid/hardware/camera2/CameraCaptureSession;", cb="Landroid/hardware/camera2/CameraCaptureSession$StateCallback;";
     check(m.getImplementation().getRegisterCount()==6,"Configured callback drift");
     var code=List.<Instruction>of(
      new ImmutableInstruction22c(Opcode.IGET_OBJECT,0,4,new ImmutableFieldReference(callback,"val$owner",OBJ)),
      new ImmutableInstruction22c(Opcode.IGET_OBJECT,1,4,new ImmutableFieldReference(callback,"val$s",STATE)),
      new ImmutableInstruction22c(Opcode.IGET_WIDE,2,4,new ImmutableFieldReference(callback,"val$token","J")),
      new ImmutableInstruction35c(Opcode.INVOKE_STATIC,4,0,1,2,3,0,new ImmutableMethodReference(P+"CameraSession1920;","acceptConfigured1920",List.of(OBJ,STATE,"J"),"Z")),
      new ImmutableInstruction11x(Opcode.MOVE_RESULT,0),new ImmutableInstruction21t(Opcode.IF_EQZ,0,8),
      new ImmutableInstruction22c(Opcode.IGET_OBJECT,0,4,new ImmutableFieldReference(callback,"val$cb",cb)),
      call(Opcode.INVOKE_VIRTUAL,2,0,5,0,cb,"onConfigured",List.of(session),"V"),new ImmutableInstruction10x(Opcode.RETURN_VOID),
      call(Opcode.INVOKE_VIRTUAL,1,5,0,0,session,"close",List.of(),"V"),new ImmutableInstruction10x(Opcode.RETURN_VOID));
     ms.add(method(m,m.getName(),new ImmutableMethodImplementation(6,code,List.of(),List.of())));changed=true;
     AUDIT.add("ATOMIC_CALLBACK_TIMEOUT_ARBITRATION\t"+MergePayloads.id(m));continue;
    }
    if(runtime && c.getType().equals(C) && m.getName().equals("session")){
     // Preserve the complete previous implementation, including its try/catch table.
     ms.add(method(m,"sessionCore1920",m.getImplementation()));
     ms.add(method(m,"session",new ImmutableMethodImplementation(3,List.of(call(Opcode.INVOKE_STATIC,3,0,1,2,P+"CameraSession1920;","session",List.of(OBJ,DEV,CFG),"V"),new ImmutableInstruction10x(Opcode.RETURN_VOID)),List.of(),List.of())));
     AUDIT.add("WRAPPED_UNCHANGED_CORE\t"+MergePayloads.id(m)+"\t"+MergePayloads.hash(m));changed=true;continue;
    }
    if(edited){
     var impl=m.getImplementation();Method out=method(m,m.getName(),new ImmutableMethodImplementation(impl.getRegisterCount(),ins,impl.getTryBlocks(),impl.getDebugItems()));
     ms.add(out);changed=true;AUDIT.add("CHANGED\t"+MergePayloads.id(m)+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(out));
    } else ms.add(m);
   }
   if(runtime){var extra=additions(c.getType());ms.addAll(extra);changed|=!extra.isEmpty();for(var m:extra)AUDIT.add("ADDED_BRIDGE\t"+MergePayloads.id(m));}
   var fields=new ArrayList<Field>();c.getFields().forEach(fields::add);
   if(runtime && c.getType().equals(STATE)){
    check(fields.stream().noneMatch(f->f.getName().equals("timedOutToken1920")),"Timeout field already exists");
    fields.add(new ImmutableField(STATE,"timedOutToken1920","J",64,null,Set.of(),Set.of()));changed=true;
    AUDIT.add("ADDED_TIMEOUT_TOKEN\t"+STATE);
   }
   after.put(c.getType(),changed?new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),fields,ms):c);
  }
  return after;
 }
 public static void main(String[] a)throws Exception{
  check(a.length==5,"BASE HELPER BUNDLE_DEX OUTPUT AUDIT");Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var rt0=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var mt0=MergePayloads.classes(base.resolve("ulike/methods.dex").toString());var ld0=MergePayloads.classes(base.resolve("classes.dex").toString());
  check(rt0.size()==216&&MergePayloads.methods(mt0.values()).size()==170&&ld0.size()==13,"1919 counts drift");
  var rt=transform(rt0,true);var mt=transform(mt0,false);var ld=transform(ld0,false);
  var helper=MergePayloads.classes(a[1]);check(helper.size()==5,"Helper count or stub leak");
  for(var c:helper.values()){check(c.getType().startsWith(P+"BackRoute1920")||c.getType().startsWith(P+"CameraSession1920"),"Unexpected helper");check(rt.put(c.getType(),c)==null,"Helper collision");}
  var beforeMethods=MergePayloads.methods(rt0.values());var afterMethods=MergePayloads.methods(rt.values());
  var old=beforeMethods.get(C+"->session("+OBJ+DEV+CFG+")V");var core=afterMethods.get(C+"->sessionCore1920("+OBJ+DEV+CFG+")V");
  check(MergePayloads.hash(old).equals(MergePayloads.hash(method(core,"session",core.getImplementation()))),"Session core changed");
  var backOld=beforeMethods.get(B+"->request("+ACT+")V");var backNew=afterMethods.get(MergePayloads.id(backOld));
  var restored=instructions(backNew);restored.set(0,instructions(backOld).getFirst());
  check(MergePayloads.hash(backOld).equals(MergePayloads.hash(method(backOld,"request",new ImmutableMethodImplementation(4,restored,backOld.getImplementation().getTryBlocks(),backOld.getImplementation().getDebugItems())))),"Exit tail changed");
  int untouched=0;for(var c:rt0.values())if(!Set.of(C,B,STATE,P+"CaptureAdvanced3$1;").contains(c.getType())){
   check(MergePayloads.classHash(c).equals(MergePayloads.classHash(rt.get(c.getType()))),"Unexpected helper change "+c.getType());untouched++;
  }
  for(String name:List.of("FacePrecision3","FacePrecision3$1","FacePrecisionPolicy","ExitFlow185","ExitJobs185","SettingsReturn1918","SettingsReturn1918$1"))check(MergePayloads.classHash(rt0.get(P+name+";")).equals(MergePayloads.classHash(rt.get(P+name+";"))),"Preserved feature drift "+name);
  MergePayloads.writeDex(out.resolve("runtime.dex"),rt.values());MergePayloads.writeDex(out.resolve("methods.dex"),mt.values());MergePayloads.writeDex(out.resolve("loader.dex"),ld.values());
  var contracts=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());var methods=MergePayloads.methods(mt.values());check(methods.keySet().equals(contracts.keySet()),"Contract set drift");var rows=new ArrayList<String>();for(var e:contracts.entrySet())rows.add(e.getKey()+"\t"+e.getValue()[1]+"\t"+MergePayloads.hash(methods.get(e.getKey())));Files.write(out.resolve("methods.tsv"),rows);
  var bundle0=MergePayloads.classes(a[2]);var bundle=new TreeMap<String,ClassDef>();int other=0;
  for(var c:bundle0.values())if(!c.getType().startsWith(MergePayloads.PATCH_NS)){bundle.put(c.getType(),c);other++;}
  check(other==220,"Other application counts drift");bundle.putAll(ld);MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var decoded=MergePayloads.classes(out.resolve("bundle-loader.dex").toString());for(var c:bundle0.values())if(!c.getType().startsWith(MergePayloads.PATCH_NS))check(MergePayloads.classHash(c).equals(MergePayloads.classHash(decoded.get(c.getType()))),"Other app changed");
  AUDIT.add("PASS\tother_app_classes_unchanged="+other+"\tunchanged_runtime_classes="+untouched+"\thelper_classes_added="+helper.size()+"\tnative_camera_callback_recipient_preserved=true;atomic_timeout_arbitration_added=true\tdevice_tested=false\tblack_screen_cause_confirmed=false");
  Files.write(Path.of(a[4]),AUDIT);for(String s:AUDIT)System.out.println(s);
 }
}
