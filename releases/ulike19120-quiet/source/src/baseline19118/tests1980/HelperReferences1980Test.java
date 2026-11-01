import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Missing-member fixtures are deliberately invalid DEX, never app payloads. */
public final class HelperReferences1980Test {
 static int assertions;
 static final String P="Lcom/hiro/ulike/", OBJECT="Ljava/lang/Object;";
 static final String[][] EXTERNAL={
  {P+"GpuResidual1961$1;","size()I","Ljava/util/LinkedHashMap;"},
  {P+"GpuSingle1960$1;","size()I","Ljava/util/LinkedHashMap;"},
  {P+"ReflectionCache1945$Bounded;","size()I","Ljava/util/LinkedHashMap;"},
  {P+"MacroUi168$Tulip;","getBounds()Landroid/graphics/Rect;","Landroid/graphics/drawable/Drawable;"},
  {P+"MacroUi168$Tulip;","invalidateSelf()V","Landroid/graphics/drawable/Drawable;"},
  {P+"ShutterFeedback1941$Pulse;","setBounds(IIII)V","Landroid/graphics/drawable/Drawable;"}};
 static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
 static ClassDef type(String name,String parent,List<String> interfaces,Collection<Method> methods,Collection<Field> fields){
  return new ImmutableClassDef(name,1,parent,interfaces,null,List.of(),fields,methods);
 }
 static ClassDef type(String name,String parent,List<String> interfaces){return type(name,parent,interfaces,List.of(),List.of());}
 static int resolution(ClassDef owner,String suffix,boolean field){
  return VerifyAllHelperReferences1980.resolve(owner.getType(),suffix,field,Map.of(owner.getType(),owner),Set.of(),new HashSet<>());
 }
 static Method callable(String owner,String name,boolean field,String target,boolean known){
  MutableMethodImplementation b=new MutableMethodImplementation(1);
  b.addInstruction(new BuilderInstruction11n(Opcode.CONST_4,0,0));
  if(field)b.addInstruction(new BuilderInstruction22c(Opcode.IGET,0,0,new ImmutableFieldReference(target,known?"live":"absent","I")));
  else b.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,1,0,0,0,0,0,
    new ImmutableMethodReference(target,known?"size":"absent",List.of(),known?"I":"V")));
  b.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
  return new ImmutableMethod(owner,name,List.of(),"V",9,Set.of(),Set.of(),b);
 }
 static void dexCheck(Path work,String label,Collection<ClassDef> runtime,boolean rejected,String message)throws Exception{
  Path path=work.resolve(label+".dex");MergePayloads.writeDex(path,runtime);
  boolean failed=false;
  try {VerifyAllHelperReferences1980.main(new String[]{path.toString(),path.toString(),path.toString(),path.toString()});}
  catch(IllegalStateException failure){
   failed=true;check(failure.getMessage().startsWith(message),"Expected member error, not an unrelated failure: "+failure);
  }
  check(failed==rejected,"DEX false-PASS classification: "+label);
 }
 public static void main(String[] args)throws Exception{
  if(args.length!=2)throw new IllegalArgumentException("WORK PREHARDENING");
  Path work=Path.of(args[0]);Files.createDirectories(work);boolean old=Boolean.parseBoolean(args[1]);
  int unknown=old?2:0;
  String fake=P+"FakeTask;",caller=P+"CallSite;";
  for(String external:List.of("Ljava/lang/Runnable;","Ljava/util/concurrent/Callable;")){
   ClassDef task=type(fake,OBJECT,List.of(external));
   check(resolution(task,"absent()V",false)==unknown,"Missing method hidden by external interface "+external);
   check(resolution(task,"absent:I",true)==unknown,"Missing field hidden by external interface "+external);
  }
  for(String external:List.of("Ljava/util/LinkedHashMap;","Landroid/graphics/drawable/Drawable;")){
   ClassDef task=type(fake,external,List.of());
   check(resolution(task,"absent()V",false)==unknown,"Missing method hidden by external superclass "+external);
   check(resolution(task,"absent:I",true)==unknown,"Missing field hidden by external superclass "+external);
  }
  for(String[] row:EXTERNAL){
   ClassDef actual=type(row[0],row[2],List.of());
   check(resolution(actual,row[1],false)==2,"Exact inherited member must remain allowed "+row[0]+row[1]);
   check(resolution(actual,"absent()V",false)==unknown,"Allowed owner cannot authorize another method");
   check(resolution(actual,"absent:I",true)==unknown,"Allowed owner cannot authorize any field");
   check(resolution(type(row[0],OBJECT,List.of("Ljava/lang/Runnable;")),row[1],false)==unknown,"Grant requires actual direct superclass");
   check(resolution(type(fake,row[2],List.of()),row[1],false)==unknown,"Grant requires exact defining class");
  }
  String parent=P+"Parent;",child=P+"Child;";
  Map<String,ClassDef> hierarchy=new TreeMap<>();
  hierarchy.put(parent,type(parent,OBJECT,List.of()));hierarchy.put(child,type(child,parent,List.of("Ljava/lang/Runnable;")));
  for(String suffix:List.of("present()V","live:I")){
   check(VerifyAllHelperReferences1980.resolve(child,suffix,suffix.contains(":"),hierarchy,Set.of(parent+"->"+suffix),new HashSet<>())==1,"Real included inherited member remains resolved");
  }
  check(VerifyAllHelperReferences1980.resolve(child,"<init>()V",false,hierarchy,Set.of(parent+"-><init>()V"),new HashSet<>())==0,"Constructors are not inherited");
  check(VerifyAllHelperReferences1980.resolve(null,"absent()V",false,hierarchy,Set.of(),new HashSet<>())==0,"Null hierarchy terminates");
  ClassDef task=type(fake,OBJECT,List.of("Ljava/lang/Runnable;"));
  dexCheck(work,"missing-method-via-interface",List.of(task,type(caller,OBJECT,List.of(),List.of(callable(caller,"test",false,fake,false)),List.of())),!old,"Unresolved helper member");
  dexCheck(work,"missing-field-via-interface",List.of(task,type(caller,OBJECT,List.of(),List.of(callable(caller,"test",true,fake,false)),List.of())),!old,"Unresolved helper member");
  ClassDef linked=type(EXTERNAL[0][0],EXTERNAL[0][2],List.of());
  dexCheck(work,"allowed-platform-method",List.of(linked,type(caller,OBJECT,List.of(),List.of(callable(caller,"test",false,linked.getType(),true)),List.of())),false,"");
  dexCheck(work,"wrong-platform-parent",List.of(type(linked.getType(),OBJECT,List.of("Ljava/lang/Runnable;")),type(caller,OBJECT,List.of(),List.of(callable(caller,"test",false,linked.getType(),true)),List.of())),!old,"Unresolved helper member");
  dexCheck(work,"missing-helper-type",List.of(type(caller,OBJECT,List.of(),List.of(callable(caller,"test",false,fake,false)),List.of())),true,"Missing helper class");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"prehardening_false_pass_reproduced\":"+old
   +",\"physical_android_tested\":false}");
 }
}
