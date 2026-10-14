import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Retain exactly the two camera observers already applied to published .65 timing. */
public final class TimingCameraHooks1975 {
 static final String P="Lcom/hiro/ulike/", T=P+"ProcessingTiming1947;", C=P+"CameraSession1965;", D=P+"CameraTrace1965;";
 static final String OLD_TITLE="直近の撮影・工程別処理時間（タップで更新）", TITLE="直近の撮影・工程別処理時間（更新・診断記録）";
 static boolean owned(String type){return type.equals(T)||type.startsWith(P+"ProcessingTiming1947$");}
 static String target(Method m){
  String id=MergePayloads.id(m);
  if(id.equals(T+"->init(Landroid/content/Context;)V"))return C+"->init(Landroid/content/Context;)V";
  if(owned(m.getDefiningClass())&&m.getName().equals("onPreferenceClick")&&m.getParameterTypes().equals(List.of("Landroid/preference/Preference;"))&&m.getReturnType().equals("Z"))return D+"->open(Landroid/preference/Preference;)V";
  return null;
 }
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static String canonical(ClassDef c)throws Exception{return MergePayloads.classHash(ImmutableClassDef.of(c));}
 static ClassDef edit(ClassDef c,boolean inverse)throws Exception {
  MergePayloads.require(owned(c.getType()),"Only timing camera hooks");var methods=new ArrayList<Method>();
  for(Method m:c.getMethods()){
   if(m.getImplementation()==null){methods.add(m);continue;}
   var b=new MutableMethodImplementation(m.getImplementation());String target=target(m);
   if(target!=null){
    String owner=target.substring(0,target.indexOf("->")),name=target.substring(target.indexOf("->")+2,target.indexOf('('));
    var params=new ArrayList<String>();for(CharSequence type:m.getParameterTypes())params.add(type.toString());
    MethodReference ref=new ImmutableMethodReference(owner,name,params,"V");
    if(inverse){
     Instruction first=b.getInstructions().get(0);MergePayloads.require(first.getOpcode()==Opcode.INVOKE_STATIC_RANGE&&first instanceof ReferenceInstruction&&((ReferenceInstruction)first).getReference().toString().equals(ref.toString()),"Exact retained .65 observer prefix");
     RegisterRangeInstruction call=(RegisterRangeInstruction)first;MergePayloads.require(call.getRegisterCount()==params.size()&&call.getStartRegister()==b.getRegisterCount()-params.size(),"Retained observer argument range");b.removeInstruction(0);
    }else{
     b.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,b.getRegisterCount()-params.size(),params.size(),ref));
    }
   }
   if(c.getType().equals(T)&&m.getName().equals("install"))for(int i=0;i<b.getInstructions().size();i++){
    Instruction x=b.getInstructions().get(i);
    if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().equals(inverse?TITLE:OLD_TITLE)){
     MergePayloads.require(x.getOpcode()==Opcode.CONST_STRING,"Retained diagnostic title opcode");
     b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(inverse?OLD_TITLE:TITLE)));
    }
   }
   methods.add(replace(m,b));
  }
  return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);
 }
 static ClassDef patch(ClassDef before)throws Exception{
  ClassDef after=edit(before,false);MergePayloads.require(canonical(before).equals(canonical(edit(after,true))),"Timing observer inverse restores compiled source");return after;
 }
 static Map<String,List<String>> inventory(Map<String,ClassDef> classes){
  var out=new TreeMap<String,List<String>>();
  for(ClassDef c:classes.values())if(owned(c.getType()))for(Method m:c.getMethods())if(m.getImplementation()!=null){
   var calls=new ArrayList<String>();for(Instruction x:m.getImplementation().getInstructions())if(x instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference ref&&(ref.getDefiningClass().equals(C)||ref.getDefiningClass().equals(D))){
    MergePayloads.require(x.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"Retained camera observer instruction");calls.add(ref.toString());
   }
   if(!calls.isEmpty())out.put(MergePayloads.id(m),calls);
  }
  return out;
 }
 static void verifyInventory(Map<String,ClassDef> before,Map<String,ClassDef> after){
  Map<String,List<String>> old=inventory(before),current=inventory(after);
  MergePayloads.require(old.equals(current)&&old.size()==2&&old.values().stream().mapToInt(List::size).sum()==2,"Exact baseline .65 timing camera observer method/signature/count inventory retained");
 }
}
