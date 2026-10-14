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
 static void wrapper(Method m){
  List<Instruction> ins=instructions(m);check(ins.size()>=11,"wrapper length");check(ref(ins.get(0))!=null&&RUNTIME.equals(ref(ins.get(0)).getDefiningClass()),"conditional runtime guard");check(ins.get(1).getOpcode()==Opcode.MOVE_RESULT,"guard result");
  check(ins.get(2).getOpcode()==Opcode.IF_EQZ,"false branches to native path");check(addr(ins,2)+((OffsetInstruction)ins.get(2)).getCodeOffset()==addr(ins,4),"branch target correct");
  check("enterNative".equals(ref(ins.get(4)).getName()),"native scope before helper");check(ins.get(5).getOpcode()==Opcode.INVOKE_DIRECT||ins.get(5).getOpcode()==Opcode.INVOKE_SUPER,"native helper or inherited original");
  List<? extends TryBlock<? extends ExceptionHandler>> tries=m.getImplementation().getTryBlocks();check(tries.size()==1,"one finally region");TryBlock<? extends ExceptionHandler> t=tries.get(0);
  check(t.getStartCodeAddress()==addr(ins,5),"try begins at original call");check(t.getExceptionHandlers().size()==1&&t.getExceptionHandlers().get(0).getExceptionType()==null,"catch-all preserves exceptions");
  int handler=ins.size()-3;check(t.getExceptionHandlers().get(0).getHandlerCodeAddress()==addr(ins,handler),"handler address");check(ins.get(handler).getOpcode()==Opcode.MOVE_EXCEPTION,"exception captured");check("leaveNative".equals(ref(ins.get(handler+1)).getName()),"scope released on exception");check(ins.get(handler+2).getOpcode()==Opcode.THROW,"original exception rethrown");
  check("leaveNative".equals(ref(ins.get(handler-2)).getName()),"scope released on success");check(m.getImplementation().getRegisterCount()==m.getParameterTypes().size()+3,"locals distinct from parameter registers");
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
  result.put(type,new ImmutableClassDef(type,1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),methods));return result;
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
    if(m.getName().startsWith("hiroOneBack$")) {
     String name=m.getName().substring("hiroOneBack$".length());
     Method matching=null;for(Method candidate:before.getMethods())if(candidate.getName().equals(name)&&candidate.getParameterTypes().equals(m.getParameterTypes())&&candidate.getReturnType().equals(m.getReturnType()))matching=candidate;
     check(matching!=null,"every helper has original");
     Method restored=new ImmutableMethod(m.getDefiningClass(),matching.getName(),matching.getParameters(),matching.getReturnType(),matching.getAccessFlags(),matching.getAnnotations(),matching.getHiddenApiRestrictions(),m.getImplementation());
     check(Arrays.equals(methodBytes(matching),methodBytes(restored)),"original helper body exact: "+m);helpers++;
    } else {
     List<Instruction> ins=instructions(m);
     if(!ins.isEmpty()&&ref(ins.get(0))!=null&&RUNTIME.equals(ref(ins.get(0)).getDefiningClass())
       &&Arrays.asList("tryBack","tryDialogBack","key","dialogKey","keyFromView").contains(ref(ins.get(0)).getName())) {wrapper(m);wrappers++;}
    }
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
  if(args.length==3&&args[0].equals("audit-app")){Map<String,ClassDef> input=read(args[1]),output=read(args[2]);audit(input,output);check(output.containsKey(RUNTIME),"runtime extension included");check(output.containsKey("Lapp/hiro/oneback/runtime/HomeScreen;"),"home recognizer included");System.out.println("PASS real patcher fixture audit; assertions="+assertions);return;}
  if(args.length!=2||!args[0].equals("selftest"))throw new IllegalArgumentException("selftest OUTDIR | merge BASE NEW OUT | audit-app ORIGINAL PATCHED");
  Path dir=Paths.get(args[1]);Files.createDirectories(dir);Map<String,ClassDef> original=fixtures(),patched=apply(original);audit(original,patched);
  DexPool.writeTo(dir.resolve("fixture-original.dex").toString(),new ImmutableDexFile(Opcodes.getDefault(),original.values()));
  DexPool.writeTo(dir.resolve("fixture-patched.dex").toString(),new ImmutableDexFile(Opcodes.getDefault(),patched.values()));audit(read(dir.resolve("fixture-original.dex").toString()),read(dir.resolve("fixture-patched.dex").toString()));
  boolean collision=false;try{apply(patched);}catch(IllegalStateException expected){collision=true;}check(collision,"double application rejected");
  java.lang.reflect.Method manifest=DexAudit.class.getDeclaredMethod("manifestTests");manifest.setAccessible(true);manifest.invoke(null);
  System.out.println("PASS selftest: wrapper structure, original bodies, exception paths, registration ABI, manifest, collisions; assertions="+assertions);
 }
}
