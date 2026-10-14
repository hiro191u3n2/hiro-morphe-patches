import java.util.*;
import java.nio.file.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.value.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.immutable.value.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Independent preservation, timer version-only change, links and exception checks. */
public final class Verify1950 {
 static int checks;
 static void need(boolean value,String reason){checks++;if(!value)throw new IllegalStateException(reason);}
 static String root(String type){int at=type.indexOf('$');return at<0?type:type.substring(0,at)+";";}
 static final Set<String> OWNED=Set.of("Lcom/hiro/ulike/CorePixels1950;","Lcom/hiro/ulike/QualityShadow1932;","Lcom/hiro/ulike/GpuInteger1949;","Lcom/hiro/ulike/QualityPipeline1932;","Lcom/hiro/ulike/NativeSpeed1944;");
 static final String TIMER="Lcom/hiro/ulike/ProcessingTiming1947;";
 static ClassDef restoreVersion(ClassDef c){
  var fields=new ArrayList<Field>();var methods=new ArrayList<Method>();boolean changed=false;
  for(Field f:c.getFields()){
   EncodedValue value=f.getInitialValue();
   if(value instanceof StringEncodedValue s&&s.getValue().contains("1.9.50")){value=new ImmutableStringEncodedValue(s.getValue().replace("1.9.50","1.9.49"));changed=true;fields.add(new ImmutableField(f.getDefiningClass(),f.getName(),f.getType(),f.getAccessFlags(),value,f.getAnnotations(),f.getHiddenApiRestrictions()));}
   else fields.add(f);
  }
  for(Method m:c.getMethods()){
   if(m.getImplementation()==null){methods.add(m);continue;}
   var body=new MutableMethodImplementation(m.getImplementation());boolean methodChanged=false;
   for(int i=0;i<body.getInstructions().size();i++){
    Instruction op=body.getInstructions().get(i);
    if(op instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().contains("1.9.50")){
     StringReference value=new ImmutableStringReference(s.getString().replace("1.9.50","1.9.49"));
     int register=((OneRegisterInstruction)op).getRegisterA();
     if(op.getOpcode()==Opcode.CONST_STRING)body.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,register,value));
     else {need(op.getOpcode()==Opcode.CONST_STRING_JUMBO,"Only string version may normalize");body.replaceInstruction(i,new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,register,value));}
     methodChanged=true;changed=true;
    }
   }
   methods.add(methodChanged?new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body):m);
  }
  return changed?new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),fields,methods):c;
 }
 static void tryRanges(Collection<ClassDef> classes){
  for(ClassDef c:classes)for(Method method:c.getMethods()){
   MethodImplementation body=method.getImplementation();if(body==null)continue;
   int size=0;Set<Integer> boundaries=new HashSet<>();boundaries.add(0);
   for(Instruction op:body.getInstructions()){size+=op.getCodeUnits();boundaries.add(size);}
   for(TryBlock<? extends ExceptionHandler> block:body.getTryBlocks()){
    int first=block.getStartCodeAddress(),last=first+block.getCodeUnitCount();
    need(first>=0&&block.getCodeUnitCount()>0&&block.getCodeUnitCount()<=65535&&last<=size&&boundaries.contains(first)&&boundaries.contains(last),"DEX try span "+MergePayloads.id(method));
    for(ExceptionHandler handler:block.getExceptionHandlers())need(handler.getHandlerCodeAddress()>=0&&handler.getHandlerCodeAddress()<size&&boundaries.contains(handler.getHandlerCodeAddress()),"DEX handler boundary "+MergePayloads.id(method));
   }
  }
 }
 public static void main(String[] args)throws Exception{
  need(args.length==3,"BASE_RUNTIME_DEX EMITTED_RUNTIME_DEX COMPILED_HELPER_DEX");
  var before=MergePayloads.classes(args[0]);var after=MergePayloads.classes(args[1]);var compiled=MergePayloads.classes(args[2]);
  for(var entry:before.entrySet())if(!OWNED.contains(root(entry.getKey()))&&!entry.getKey().equals(TIMER)&&!entry.getKey().equals(CoreHooks1950.OWNER)&&!entry.getKey().equals(CorrectionHooks1950.OWNER))need(after.containsKey(entry.getKey())&&MergePayloads.classHash(entry.getValue()).equals(MergePayloads.classHash(after.get(entry.getKey()))),"Unrelated class / stage timing changed "+entry.getKey());
  CoreHooks1950.verify(before,after);
  CorrectionHooks1950.verify(before,after);
  for(String owner:List.of(CoreHooks1950.OWNER,CorrectionHooks1950.OWNER)){
   ClassDef old=before.get(owner),now=after.get(owner);
   var oldShell=new ImmutableClassDef(old.getType(),old.getAccessFlags(),old.getSuperclass(),old.getInterfaces(),old.getSourceFile(),old.getAnnotations(),old.getFields(),List.of());
   var newShell=new ImmutableClassDef(now.getType(),now.getAccessFlags(),now.getSuperclass(),now.getInterfaces(),now.getSourceFile(),now.getAnnotations(),now.getFields(),List.of());
   need(MergePayloads.classHash(oldShell).equals(MergePayloads.classHash(newShell)),"Hook owner fields and class metadata preserved "+owner);
  }
  Set<String> expected=new TreeSet<>(before.keySet());expected.removeIf(x->OWNED.contains(root(x)));expected.addAll(compiled.keySet());need(after.keySet().equals(expected),"Exact runtime class inventory");
  Set<String> roots=new TreeSet<>();for(var entry:compiled.entrySet()){roots.add(root(entry.getKey()));need(OWNED.contains(root(entry.getKey()))&&after.containsKey(entry.getKey())&&MergePayloads.classHash(entry.getValue()).equals(MergePayloads.classHash(after.get(entry.getKey()))),"Emitted helper differs from compiler "+entry.getKey());}need(roots.equals(OWNED),"Exact reviewed helper families");
  String timer=TIMER;int timerClasses=0;
  for(var entry:before.entrySet())if(root(entry.getKey()).equals(timer)){timerClasses++;need(after.containsKey(entry.getKey())&&MergePayloads.classHash(entry.getValue()).equals(MergePayloads.classHash(restoreVersion(after.get(entry.getKey())))),"Only current version strings may change in timing family "+entry.getKey());}need(timerClasses>1,"Existing timing family classes checked");
  int versionFields=0,versionLiterals=0;for(Field f:after.get(timer).getFields())if(f.getName().equals("VERSION")){need(f.getInitialValue() instanceof StringEncodedValue&&((StringEncodedValue)f.getInitialValue()).getValue().equals("1.9.50"),"Current timer VERSION field");versionFields++;}
  for(Method m:after.get(timer).getMethods())if(m.getImplementation()!=null)for(Instruction op:m.getImplementation().getInstructions())if(op instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s){need(!s.getString().contains("1.9.49"),"Old current timer version literal not retained");if(s.getString().contains("1.9.50"))versionLiterals++;}need(versionFields==1&&versionLiterals==4,"Exactly one version field and four current-version literals");
  var methods=MergePayloads.methods(after.values());Set<String> fields=new HashSet<>();for(ClassDef c:after.values())for(Field f:c.getFields())fields.add(f.toString());
  for(ClassDef c:after.values())for(Method method:c.getMethods())if(method.getImplementation()!=null)for(Instruction op:method.getImplementation().getInstructions())if(op instanceof ReferenceInstruction reference){
   if(reference.getReference() instanceof MethodReference target&&OWNED.contains(root(target.getDefiningClass())))need(methods.containsKey(target.toString()),"Helper method resolves "+target);
   if(reference.getReference() instanceof FieldReference target&&OWNED.contains(root(target.getDefiningClass())))need(fields.contains(target.toString()),"Helper field resolves "+target);
  }
  // New cross-class helper calls must satisfy JVM/ART private and package access rules.
  for(ClassDef c:after.values())for(Method source:c.getMethods())if(source.getImplementation()!=null)for(Instruction op:source.getImplementation().getInstructions())if(op instanceof ReferenceInstruction ref&&ref.getReference() instanceof MethodReference target){
   Method resolved=methods.get(target.toString());if(resolved==null||!target.getDefiningClass().startsWith("Lcom/hiro/ulike/"))continue;
   int flags=resolved.getAccessFlags();String owner=resolved.getDefiningClass(),caller=source.getDefiningClass();
   if((flags&2)!=0)need(owner.equals(caller),"Private helper access from "+caller+" to "+target);
   else if((flags&1)==0&&(flags&4)==0)need(owner.substring(0,owner.lastIndexOf('/')).equals(caller.substring(0,caller.lastIndexOf('/'))),"Package helper access from "+caller+" to "+target);
  }
  for(String type:List.of("Lcom/hiro/ulike/TimedIo1947;","Lcom/hiro/ulike/ShotContext1932;"))need(before.containsKey(type)&&after.containsKey(type)&&MergePayloads.classHash(before.get(type)).equals(MergePayloads.classHash(after.get(type))),"Timing/quality/context preserved "+type);
  tryRanges(after.values());
  System.out.println("PASS_OPTIMIZATION1950 checks="+checks+"; H1-H5 compiled helper families, exact original denoise alias plus version-only timer; stage timing/native/quality settings preserved; device speed unmeasured");
 }
}
