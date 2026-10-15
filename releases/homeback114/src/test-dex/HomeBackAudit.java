import java.io.*;import java.nio.file.*;import java.util.*;
import app.hiro.oneback.patches.OneBackPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Independent structural audit of new fallback wrappers and preservation of unrelated loader classes. */
public final class HomeBackAudit {
 static int assertions;static final String PREFIX="Lapp/hiro/oneback/patches/",RUNTIME="Lapp/hiro/oneback/runtime/OneBackExit;";
 static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
 static Map<String,ClassDef> read(String path)throws Exception {Map<String,ClassDef> result=new TreeMap<>();MultiDexContainer<? extends DexFile> c=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());for(String e:c.getDexEntryNames())for(ClassDef k:c.getEntry(e).getDexFile().getClasses())check(result.put(k.getType(),k)==null,"unique classes");return result;}
 static byte[] canonical(ClassDef c)throws Exception{MemoryDataStore m=new MemoryDataStore();try{DexPool.writeTo(m,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(c)));return Arrays.copyOf(m.getBuffer(),m.getSize());}finally{m.close();}}
 static byte[] methodBytes(Method m)throws Exception{return canonical(new ImmutableClassDef(m.getDefiningClass(),1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.singleton(m)));}
 static String key(MethodReference m){return m.getName()+m.getParameterTypes().toString()+m.getReturnType();}
 static Map<String,Method> methods(ClassDef c){Map<String,Method> r=new TreeMap<>();for(Method m:c.getMethods())check(r.put(key(m),m)==null,"unique methods");return r;}
 static List<Instruction> instructions(Method m){List<Instruction> r=new ArrayList<>();if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())r.add(i);return r;}
 static MethodReference ref(Instruction i){if(!(i instanceof ReferenceInstruction))return null;Reference r=((ReferenceInstruction)i).getReference();return r instanceof MethodReference?(MethodReference)r:null;}
 static int addr(List<Instruction> ins,int pos){int a=0;for(int i=0;i<pos;i++)a+=ins.get(i).getCodeUnits();return a;}
 static MethodImplementation restoreTerminalCalls(Method method,ClassDef clazz) {
  if(method.getImplementation()==null)return null;
  MutableMethodImplementation code=new MutableMethodImplementation(method.getImplementation());
  for(int i=0;i<code.getInstructions().size();i++) {
   Instruction call=code.getInstructions().get(i);MethodReference target=ref(call);
   if(target==null||!target.getDefiningClass().equals(clazz.getType())||!target.getName().startsWith("hiroOneBack$terminal$"))continue;
   Method bridge=methods(clazz).get(key(target));check(bridge!=null,"terminal bridge exists");
   List<Instruction> b=instructions(bridge);MethodReference original=ref(b.get(6));Opcode op=b.get(6).getOpcode();
   if(call instanceof RegisterRangeInstruction) {
    op=op==Opcode.INVOKE_SUPER?Opcode.INVOKE_SUPER_RANGE:op==Opcode.INVOKE_VIRTUAL?Opcode.INVOKE_VIRTUAL_RANGE:Opcode.INVOKE_INTERFACE_RANGE;
    RegisterRangeInstruction r=(RegisterRangeInstruction)call;
    code.replaceInstruction(i,new BuilderInstruction3rc(op,r.getStartRegister(),r.getRegisterCount(),original));
   } else {
    FiveRegisterInstruction r=(FiveRegisterInstruction)call;
    code.replaceInstruction(i,new BuilderInstruction35c(op,r.getRegisterCount(),r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),original));
   }
  }
  return code;
 }
 static void terminalBridge(Method method) {
  List<Instruction> b=instructions(method);
  check(b.size()==8||b.size()==9,"terminal bridge size");
  check(Arrays.asList("tryTerminalBack","tryTaskTerminal").contains(ref(b.get(2)).getName())&&RUNTIME.equals(ref(b.get(2)).getDefiningClass()),"only terminal runtime guard");
  check(b.get(4).getOpcode()==Opcode.IF_EQZ&&addr(b,4)+((OffsetInstruction)b.get(4)).getCodeOffset()==addr(b,6),"native fallback target intact");
  boolean stat=(method.getAccessFlags()&8)!=0;
  check((b.get(6).getOpcode()==Opcode.INVOKE_SUPER)==!stat,"super dispatch kept as super, never virtualized");
  int kind=((NarrowLiteralInstruction)b.get(0)).getNarrowLiteral();
  check(kind>=0&&kind<=5,"terminal operation bounded");
  FiveRegisterInstruction guard=(FiveRegisterInstruction)b.get(2),nativeCall=(FiveRegisterInstruction)b.get(6);
  check(guard.getRegisterC()==3&&(kind==5?guard.getRegisterCount()==1:(guard.getRegisterCount()==3&&guard.getRegisterD()==1&&guard.getRegisterE()==(kind==2?4:2))),"guard receives exact receiver kind and nonRoot flag");
  check((kind==5)=="tryTaskTerminal".equals(ref(b.get(2)).getName()),"AppTask is not an Activity receiver");
  check(nativeCall.getRegisterC()==3&&nativeCall.getRegisterCount()==(kind==2?2:1),"native receiver/argument count preserved");
  check(kind!=2||nativeCall.getRegisterD()==4,"moveTaskToBack nonRoot argument preserved");
  check(ref(b.get(6)).getReturnType().equals(method.getReturnType()),"native result descriptor preserved");
  check((kind==2)=="Z".equals(method.getReturnType()),"moveTaskToBack boolean ABI");
  // Both guard branches are checked independently: handled returns true/void,
  // false reaches exactly one preserved platform invocation.
  for(boolean handled:new boolean[]{false,true}) {
   int pc=handled?5:6;
   check(handled?(b.get(pc).getOpcode()==Opcode.RETURN||b.get(pc).getOpcode()==Opcode.RETURN_VOID)
       :(ref(b.get(pc))!=null&&!RUNTIME.equals(ref(b.get(pc)).getDefiningClass())),"terminal guard branch semantic");
  }
 }
 static void addTerminalFixtures(Map<String,ClassDef> result) {
  String base="Lfixture/BaseActivity;",activity="Landroid/app/Activity;";
  ClassDef before=result.get(base);List<Method> methods=new ArrayList<>();
  for(Method m:before.getMethods()) {
   if(m.getName().equals("onBackPressed")) {
    MutableMethodImplementation code=new MutableMethodImplementation(301);
    code.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_SUPER_RANGE,300,1,new ImmutableMethodReference(activity,"onBackPressed",Collections.emptyList(),"V")));
    code.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
    m=new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code);
   }
   methods.add(m);
  }
  result.put(base,new ImmutableClassDef(before.getType(),before.getAccessFlags(),before.getSuperclass(),before.getInterfaces(),before.getSourceFile(),before.getAnnotations(),before.getFields(),methods));
  String type="Lfixture/TerminalCaller;";methods=new ArrayList<>();
  for(String name:Arrays.asList("moveTaskToBack","finish","finishAffinity","finishAfterTransition","finishAndRemoveTask")) {
   boolean move=name.equals("moveTaskToBack");List<ImmutableMethodParameter> params=new ArrayList<>();params.add(new ImmutableMethodParameter(activity,Collections.emptySet(),null));
   if(move)params.add(new ImmutableMethodParameter("Z",Collections.emptySet(),null));
   MutableMethodImplementation code=new MutableMethodImplementation(302);
   code.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_VIRTUAL_RANGE,302-params.size(),params.size(),new ImmutableMethodReference(activity,name,move?Collections.singletonList("Z"):Collections.emptyList(),move?"Z":"V")));
   if(move)code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
   code.addInstruction(move?new BuilderInstruction11x(Opcode.RETURN,0):new BuilderInstruction10x(Opcode.RETURN_VOID));
   methods.add(new ImmutableMethod(type,name,params,move?"Z":"V",9,Collections.emptySet(),Collections.emptySet(),code));
  }
  String task="Landroid/app/ActivityManager$AppTask;";
  MutableMethodImplementation taskCode=new MutableMethodImplementation(301);
  taskCode.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_VIRTUAL_RANGE,300,1,new ImmutableMethodReference(task,"finishAndRemoveTask",Collections.emptyList(),"V")));
  taskCode.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
  methods.add(new ImmutableMethod(type,"removeAppTask",Collections.singletonList(new ImmutableMethodParameter(task,Collections.emptySet(),null)),"V",9,Collections.emptySet(),Collections.emptySet(),taskCode));
  result.put(type,new ImmutableClassDef(type,1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),methods));
 }
 static void wrapper(Method m){
  List<Instruction> ins=instructions(m);check(ins.size()>=11,"wrapper length");check(ref(ins.get(0))!=null&&RUNTIME.equals(ref(ins.get(0)).getDefiningClass()),"conditional runtime guard");check(ins.get(1).getOpcode()==Opcode.MOVE_RESULT,"guard result");
  check(ins.get(2).getOpcode()==Opcode.IF_EQZ,"false branches to native path");check(addr(ins,2)+((OffsetInstruction)ins.get(2)).getCodeOffset()==addr(ins,4),"branch target correct");
  check(Arrays.asList("enterNative","enterBack","enterKey").contains(ref(ins.get(4)).getName()),"typed native scope before helper");check(ins.get(5).getOpcode()==Opcode.INVOKE_DIRECT||ins.get(5).getOpcode()==Opcode.INVOKE_SUPER,"native helper or inherited original");
  List<? extends TryBlock<? extends ExceptionHandler>> tries=m.getImplementation().getTryBlocks();check(tries.size()==1,"one finally region");TryBlock<? extends ExceptionHandler> t=tries.get(0);
  check(t.getStartCodeAddress()==addr(ins,5),"try begins at original call");check(t.getExceptionHandlers().size()==1&&t.getExceptionHandlers().get(0).getExceptionType()==null,"catch-all preserves exceptions");
  int handler=ins.size()-3;check(t.getExceptionHandlers().get(0).getHandlerCodeAddress()==addr(ins,handler),"handler address");check(ins.get(handler).getOpcode()==Opcode.MOVE_EXCEPTION,"exception captured");check("leaveNative".equals(ref(ins.get(handler+1)).getName()),"scope released on exception");check(ins.get(handler+2).getOpcode()==Opcode.THROW,"original exception rethrown");
  check("leaveNative".equals(ref(ins.get(handler-2)).getName()),"scope released on success");check("afterNative".equals(ref(ins.get(handler-3)).getName()),"async ticket only after successful native return");check(m.getImplementation().getRegisterCount()==m.getParameterTypes().size()+3,"locals distinct from parameter registers");
 }
 @SuppressWarnings("unchecked") static Map<String,ClassDef> fixtures()throws Exception {
  java.lang.reflect.Method maker=DexAudit.class.getDeclaredMethod("fixtures");maker.setAccessible(true);Map<String,ClassDef> result=(Map<String,ClassDef>)maker.invoke(null);
  String type="Lfixture/ModernRegistration;",dispatcher="Landroid/window/OnBackInvokedDispatcher;",callback="Landroid/window/OnBackInvokedCallback;";
  List<Method> methods=new ArrayList<>();
  for(boolean reg:new boolean[]{true,false}) {
   List<String> args=reg?Arrays.asList(dispatcher,"I",callback):Arrays.asList(dispatcher,callback);
   List<ImmutableMethodParameter> params=new ArrayList<>();for(String a:args)params.add(new ImmutableMethodParameter(a,Collections.emptySet(),null));
   MutableMethodImplementation code=new MutableMethodImplementation(302);
   code.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_INTERFACE_RANGE,302-args.size(),args.size(),new ImmutableMethodReference(dispatcher,reg?"registerOnBackInvokedCallback":"unregisterOnBackInvokedCallback",reg?Arrays.asList("I",callback):Collections.singletonList(callback),"V")));
   code.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
   methods.add(new ImmutableMethod(type,reg?"reg":"unreg",params,"V",9,Collections.emptySet(),Collections.emptySet(),code));
  }
  result.put(type,new ImmutableClassDef(type,1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),methods));addTerminalFixtures(result);return result;
 }
 static Map<String,ClassDef> apply(Map<String,ClassDef> original) {
  final Map<String,MutableClass> edited=new HashMap<>();OneBackPatch.applyAll(original,type->{MutableClass v=edited.get(type);if(v==null){v=new MutableClass(original.get(type));edited.put(type,v);}return v;});
  Map<String,ClassDef> out=new TreeMap<>(original);out.putAll(edited);return out;
 }
 static void audit(Map<String,ClassDef> original,Map<String,ClassDef> output)throws Exception {
  int wrappers=0,helpers=0;check(output.keySet().containsAll(original.keySet()),"all original classes retained");
  for(ClassDef before:original.values()) {
   ClassDef after=output.get(before.getType());Map<String,Method> old=methods(before),now=methods(after);
   for(Method m:after.getMethods()) {
    if(m.getName().equals("onUserInteraction"))check((m.getAccessFlags()&1)!=0,"public Activity callback retains public access");
    if(m.getName().startsWith("hiroOneBack$terminal$")) {terminalBridge(m);continue;}
    if(m.getName().startsWith("hiroOneBack$")) {
     String name=m.getName().substring("hiroOneBack$".length());
     Method matching=null;for(Method candidate:before.getMethods())if(candidate.getName().equals(name)&&candidate.getParameterTypes().equals(m.getParameterTypes())&&candidate.getReturnType().equals(m.getReturnType()))matching=candidate;
     check(matching!=null,"every helper has original");
     Method restored=new ImmutableMethod(m.getDefiningClass(),matching.getName(),matching.getParameters(),matching.getReturnType(),matching.getAccessFlags(),matching.getAnnotations(),matching.getHiddenApiRestrictions(),restoreTerminalCalls(m,after));
     check(Arrays.equals(methodBytes(matching),methodBytes(restored)),"original helper body exact after terminal bridge normalization: "+m);helpers++;
    } else {
     List<Instruction> ins=instructions(m);
     if(!ins.isEmpty()&&ref(ins.get(0))!=null&&RUNTIME.equals(ref(ins.get(0)).getDefiningClass())
       &&Arrays.asList("tryBack","tryDialogBack","key","dialogKey","keyFromView").contains(ref(ins.get(0)).getName())) {wrapper(m);wrappers++;}
    }
   }
   if(before.getType().equals("Lfixture/TerminalCaller;"))for(Method m:before.getMethods()) {
    Method patched=now.get(key(m));check(patched!=null,"terminal original method retained");
    Method restored=new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),restoreTerminalCalls(patched,after));
    check(Arrays.equals(methodBytes(m),methodBytes(restored)),"terminal original body/arguments preserved");
    check(instructions(patched).get(0).getOpcode()==Opcode.INVOKE_STATIC_RANGE,"terminal high-register site safely bridged");
   }
   if(before.getType().equals("Lfixture/Unrelated;")||before.getType().startsWith("Lapp/hiro/oneback/runtime/"))check(Arrays.equals(canonical(before),canonical(after)),"unrelated class retained");
  }
  System.out.println("COVERAGE wrappers="+wrappers+", helpers="+helpers);check(wrappers==17&&helpers==9,"expected broad hierarchy coverage");
  for(Method m:output.get("Lfixture/ModernRegistration;").getMethods()) {Instruction i=instructions(m).get(0);check(i.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"modern range call replaced");check(ref(i).getDefiningClass().equals("Lapp/hiro/oneback/runtime/NativeBack;"),"modern decorated runtime");check(((RegisterRangeInstruction)i).getStartRegister()==302-m.getParameterTypes().size(),"high registers unchanged");}
 }
 static void merge(String basePath,String newPath,String outPath)throws Exception {
  Map<String,ClassDef> base=read(basePath),next=read(newPath),combined=new TreeMap<>(base);int removed=0;
  for(Iterator<String> i=combined.keySet().iterator();i.hasNext();)if(i.next().startsWith(PREFIX)){i.remove();removed++;}
  check(removed>0,"existing OneBack loader found");for(ClassDef c:next.values()){check(c.getType().startsWith(PREFIX),"replacement restricted to OneBack");check(combined.put(c.getType(),c)==null,"no unrelated loader collision");}
  DexPool.writeTo(outPath,new ImmutableDexFile(Opcodes.getDefault(),combined.values()));Map<String,ClassDef> written=read(outPath);check(written.keySet().equals(combined.keySet()),"loader inventory exact");
  int retained=0;for(ClassDef c:combined.values()){check(Arrays.equals(canonical(c),canonical(written.get(c.getType()))),"merged definition exact");if(!c.getType().startsWith(PREFIX)){check(Arrays.equals(canonical(base.get(c.getType())),canonical(written.get(c.getType()))),"existing loader unchanged");retained++;}}
  System.out.println("PASS merge: preserved="+retained+", replaced="+removed+", replacement="+next.size()+", assertions="+assertions);
 }
 public static void main(String[] args)throws Exception {
  if(args.length==4&&args[0].equals("merge")){merge(args[1],args[2],args[3]);return;}
  if(args.length==3&&args[0].equals("audit-app")){Map<String,ClassDef> input=read(args[1]),output=read(args[2]);audit(input,output);check(output.containsKey(RUNTIME),"runtime extension included");check(output.containsKey("Lapp/hiro/oneback/runtime/HomeScreen;"),"home recognizer included");check(output.containsKey("Lapp/hiro/oneback/runtime/TikTokHome;"),"home navigator included");check(output.containsKey("Lapp/hiro/oneback/runtime/TikTokBack;")&&output.containsKey("Lapp/hiro/oneback/runtime/TikTokScreen;"),"TikTok transaction and foreground guard included");check(output.containsKey("Lapp/hiro/oneback/runtime/TripBack;"),"Trip terminal observer included");System.out.println("PASS real patcher fixture audit; assertions="+assertions);return;}
  if(args.length!=2||!args[0].equals("selftest"))throw new IllegalArgumentException("selftest OUTDIR | merge BASE NEW OUT | audit-app ORIGINAL PATCHED");
  Path dir=Paths.get(args[1]);Files.createDirectories(dir);Map<String,ClassDef> original=fixtures(),patched=apply(original);audit(original,patched);
  DexPool.writeTo(dir.resolve("fixture-original.dex").toString(),new ImmutableDexFile(Opcodes.getDefault(),original.values()));
  DexPool.writeTo(dir.resolve("fixture-patched.dex").toString(),new ImmutableDexFile(Opcodes.getDefault(),patched.values()));audit(read(dir.resolve("fixture-original.dex").toString()),read(dir.resolve("fixture-patched.dex").toString()));
  boolean collision=false;try{apply(patched);}catch(IllegalStateException expected){collision=true;}check(collision,"double application rejected");
  java.lang.reflect.Method manifest=DexAudit.class.getDeclaredMethod("manifestTests");manifest.setAccessible(true);manifest.invoke(null);
  System.out.println("PASS selftest: wrapper structure, normalized original bodies, terminal bridge dispatch/ABI, exception paths, registration ABI, manifest, collisions; assertions="+assertions);
 }
}
