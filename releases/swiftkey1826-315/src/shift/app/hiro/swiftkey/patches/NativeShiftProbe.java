package app.hiro.swiftkey.patches;
import java.nio.file.*;import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.smali.*;

/** Interpret the actual selected native DEX instructions, with UI notification objects stubbed.
 * This exercises the patched state machine, not an independently rewritten state-transition model.
 */
public final class NativeShiftProbe {
 static int assertions;static final DexFormatter F=DexFormatter.INSTANCE;
 static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 static Map<String,ClassDef> read(Path p)throws Exception{Map<String,ClassDef> out=new TreeMap<>();MultiDexContainer<? extends DexFile> all=DexFileFactory.loadDexContainer(p.toFile(),Opcodes.forApi(26));for(String n:all.getDexEntryNames())for(ClassDef c:all.getEntry(n).getDexFile().getClasses())out.put(c.getType(),c);return out;}
 static Method find(Map<String,ClassDef> cs,String c,String n){for(Method m:cs.get(c).getMethods())if(m.getName().equals(n))return m;throw new AssertionError(c+n);}
 static ClassDef replace(ClassDef c,Method target){List<Method> ms=new ArrayList<>();for(Method m:c.getMethods())ms.add(F.getMethodDescriptor(m).equals(F.getMethodDescriptor(target))?target:m);return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static final class Obj {Map<String,Object> f=new HashMap<>();Obj put(String n,Object v){f.put(n,v);return this;}}
 static int num(Object v){if(v==null)return 0;if(v instanceof Number)return ((Number)v).intValue();if(v instanceof Boolean)return ((Boolean)v)?1:0;throw new AssertionError("not integer "+v);}
 static boolean equal(Object a,Object b){return a==b || (a instanceof Number&&b instanceof Number&&num(a)==num(b));}
 static final class VM {
  final Map<String,ClassDef> classes;int steps;
  VM(Map<String,ClassDef> c){classes=c;}
  Object call(String c,String n,Object... args){return exec(find(classes,c,n),args);}
  Object exec(Method method,Object...args){
   List<Instruction> code=new ArrayList<>();Map<Integer,Integer> addresses=new HashMap<>();List<Integer> offsets=new ArrayList<>();int address=0;
   for(Instruction i:method.getImplementation().getInstructions()){addresses.put(address,code.size());offsets.add(address);code.add(i);address+=i.getCodeUnits();}
   Object[] r=new Object[method.getImplementation().getRegisterCount()];System.arraycopy(args,0,r,r.length-args.length,args.length);Object result=null;
   for(int pc=0;pc<code.size();pc++){
    if(++steps>20000)throw new AssertionError("VM budget");Instruction i=code.get(pc);Opcode op=i.getOpcode();int a=i instanceof OneRegisterInstruction?((OneRegisterInstruction)i).getRegisterA():-1;int b=i instanceof TwoRegisterInstruction?((TwoRegisterInstruction)i).getRegisterB():-1;Reference ref=i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference():null;
    switch(op){
     case CONST_4:case CONST_16:case CONST:case CONST_HIGH16:r[a]=((NarrowLiteralInstruction)i).getNarrowLiteral();break;
     case CONST_STRING:case CONST_STRING_JUMBO:r[a]=((StringReference)ref).getString();break;
     case MOVE:case MOVE_FROM16:case MOVE_OBJECT:case MOVE_OBJECT_FROM16:r[a]=r[b];break;
     case MOVE_RESULT:case MOVE_RESULT_OBJECT:r[a]=result;break;
     case CHECK_CAST:case NOP:break;
     case SGET_OBJECT:{FieldReference f=(FieldReference)ref;String key=f.getDefiningClass()+f.getName();if(f.getDefiningClass().equals("Lwb0/r1;"))r[a]="abc".indexOf(f.getName());else r[a]=key;break;}
     case IGET:case IGET_BOOLEAN:case IGET_OBJECT:r[a]=((Obj)r[b]).f.get(((FieldReference)ref).getName());break;
     case IPUT:case IPUT_BOOLEAN:case IPUT_OBJECT:((Obj)r[b]).f.put(((FieldReference)ref).getName(),r[a]);break;
     case ADD_INT_2ADDR:r[a]=num(r[a])+num(r[b]);break;
     case ADD_INT_LIT8:r[a]=num(r[b])+((NarrowLiteralInstruction)i).getNarrowLiteral();break;
     case IF_EQ:case IF_NE:case IF_EQZ:case IF_NEZ:case IF_LEZ:{boolean jump=op==Opcode.IF_EQ?equal(r[a],r[b]):op==Opcode.IF_NE?!equal(r[a],r[b]):op==Opcode.IF_EQZ?num(r[a])==0:op==Opcode.IF_NEZ?num(r[a])!=0:num(r[a])<=0;if(jump)pc=addresses.get(offsets.get(pc)+((OffsetInstruction)i).getCodeOffset())-1;break;}
     case GOTO:case GOTO_16:case GOTO_32:pc=addresses.get(offsets.get(pc)+((OffsetInstruction)i).getCodeOffset())-1;break;
     case INVOKE_VIRTUAL:case INVOKE_INTERFACE:case INVOKE_STATIC:case INVOKE_DIRECT:case INVOKE_VIRTUAL_RANGE:case INVOKE_STATIC_RANGE:{
      MethodReference m=(MethodReference)ref;int count;int[] regs;
      if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction q=(RegisterRangeInstruction)i;count=q.getRegisterCount();regs=new int[count];for(int z=0;z<count;z++)regs[z]=q.getStartRegister()+z;}
      else{FiveRegisterInstruction q=(FiveRegisterInstruction)i;count=q.getRegisterCount();regs=new int[]{q.getRegisterC(),q.getRegisterD(),q.getRegisterE(),q.getRegisterF(),q.getRegisterG()};}
      Object[] argv=new Object[count];for(int z=0;z<count;z++)argv[z]=r[regs[z]];
      String owner=m.getDefiningClass(),name=m.getName();
      if(owner.equals("Lwb0/p1;")){result=call(owner,name,argv);}
      else if(owner.equals("Ljava/lang/Enum;")&&name.equals("ordinal"))result=argv[0];
      else if(owner.equals("Lkotlin/jvm/internal/m;")&&name.equals("b"))result=equal(argv[0],argv[1])?1:0;
      else if(owner.equals("Lwm0/v1;")&&name.equals("getValue"))result=((Obj)argv[0]).f.get("value");
      else if(owner.equals("Ldc0/a;")&&(name.equals("h")||name.equals("y")))result=((Obj)argv[0]).f.get(name);
      else if(owner.equals("Ld0/y;")&&name.equals("d"))result=num(argv[0])-1;
      else if(owner.equals("Ljava/lang/String;")&&name.equals("indexOf"))result=((String)argv[0]).indexOf(num(argv[1]));
      else if((owner.equals("Lu50/c;")&&name.equals("g"))||(owner.equals("Lp40/d;")&&name.equals("a"))){check(num(argv[0])==1,"Fixed non-punctuation classifier fixture");result=0;}
      else if(owner.equals("Ltj0/e;")&&name.equals("b0"))result=call(owner,name,argv);
      else if((owner.equals("Lkotlin/jvm/internal/m;")&&(name.equals("f")||name.equals("e")))||owner.equals("Ljava/lang/Object;")||owner.equals("Lwm0/x1;")||owner.equals("Lwb0/b2;")||owner.equals("Lwf/h;")){result=null;}
      else throw new AssertionError("Unmodeled external native call "+owner+name);
      break;
     }
     case RETURN_VOID:return null;
     case RETURN:case RETURN_OBJECT:return r[a];
     default:throw new AssertionError("Unsupported native opcode "+op+" in "+method.getName());
    }
   }
   throw new AssertionError("Native method fell through");
  }
 }
 static Obj controller(int state,int capsMode){return new Obj().put("c",state).put("d",state).put("e",capsMode).put("g",1).put("f",0).put("h",0).put("k",0).put("l",0).put("j",0).put("m",0).put("p",new Obj()).put("n",new Obj()).put("b",new Obj());}
 static void down(VM vm,Obj p){vm.call("Lwb0/p1;","n",p,new Obj());}
 static void up(VM vm,Obj p){vm.call("Lwb0/x;","A",new Obj().put("n",p),new Obj());}
 static void cancel(VM vm,Obj p){vm.call("Lwb0/x;","O",new Obj().put("n",p),new Obj());}
 static int state(VM vm,Obj p){return num(vm.call("Lwb0/p1;","u",p));}
 static void typed(VM vm,Obj p){Obj flow=new Obj().put("a",new Obj().put("value","enabled"));Obj caps=new Obj().put("b",p).put("c",new Obj().put("b",flow));vm.call("Ltj0/e;","c0",caps,new Obj().put("h",1).put("y",1),new Obj());}
 static void behavioral(Map<String,ClassDef> originals,Map<String,ClassDef> patched){
  VM old=new VM(originals),now=new VM(patched);Obj p=controller(0,1);down(old,p);up(old,p);check(state(old,p)==1,"Baseline manual Shift one-shot");typed(old,p);check(state(old,p)==0,"Baseline resets after first letter");
  p=controller(0,1);down(now,p);up(now,p);check(state(now,p)==2,"One tap enters native Caps Lock");for(int x=0;x<12;x++){typed(now,p);check(state(now,p)==2,"Caps survives letter "+x);}down(now,p);up(now,p);check(state(now,p)==0,"Next Shift tap unlocks");typed(now,p);check(state(now,p)==0,"Lowercase remains after unlock");
  p=controller(0,1);down(now,p);cancel(now,p);check(state(now,p)==0,"Cancelled down restores lowercase");p=controller(2,1);down(now,p);cancel(now,p);check(state(now,p)==2,"Cancelled unlock restores Caps Lock");
  p=controller(1,4);typed(now,p);check(state(now,p)==0,"Automatic sentence Shift resets after typing");
  p=controller(1,4);check(num(now.call("Lwb0/p1;","c",p,1,0))==0,"Automatic sentence case remains one-shot");down(now,p);up(now,p);check(state(now,p)==2,"Explicit Shift during auto-capitalization locks");
  p=controller(0,1);down(now,p);now.call("Lwb0/p1;","h",p,new Obj());check(num(p.f.get("o"))==0,"Long-press native release target remains lowercase");check(state(now,p)==0,"Long-press native mode transition remains active");
  p=controller(0,1);down(now,p);now.call("Lwb0/p1;","p",p,new Obj());up(now,p);check(state(now,p)==0,"Held Shift chord restores lowercase on release");
  p=controller(0,2);down(now,p);up(now,p);check(state(now,p)==2,"CAP_CHARACTERS field remains native Caps Lock");
  for(int n=0;n<20;n++){p=controller(0,1);for(int k=0;k<n;k++){down(now,p);up(now,p);}check(state(now,p)==(n%2==0?0:2),"Repeat discrete tap parity "+n);}
 }
 public static void main(String[]args)throws Exception{
  Path fixtures=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectories(out);Path baseline=out.resolve("shift-native-fixture.dex");SmaliOptions opt=new SmaliOptions();opt.apiLevel=26;opt.jobs=1;opt.outputDexFile=baseline.toString();check(Smali.assemble(opt,fixtures.toString()),"Native fixture assembles");
  Map<String,ClassDef> originals=read(baseline);Method original=ShiftLockPatch.inspect(originals.get(ShiftLockPatch.OWNER));MutableMethod target=new MutableMethod(original);ShiftLockPatch.applyMethod(target);
  int changes=0,index=0,changed=-1;List<? extends Instruction> actual=target.getImplementation().getInstructions();for(Instruction before:original.getImplementation().getInstructions()){Instruction after=actual.get(index);if(before instanceof ReferenceInstruction&&after instanceof ReferenceInstruction&&!F.getReference(((ReferenceInstruction)before).getReference()).equals(F.getReference(((ReferenceInstruction)after).getReference()))){changes++;changed=index;}index++;}
  check(changes==1&&index==actual.size(),"One native field-reference changed without instruction additions");
  MutableMethod undo=new MutableMethod(target);undo.getImplementation().replaceInstruction(changed,new BuilderInstruction21c(Opcode.SGET_OBJECT,0,new ImmutableFieldReference(ShiftLockPatch.STATE,"b",ShiftLockPatch.STATE)));check(MethodContract.sha256(undo).equals(MethodContract.sha256(original)),"Restoring one field reference reconstructs full native method");
  Map<String,ClassDef> updated=new TreeMap<>(originals);updated.put(ShiftLockPatch.OWNER,replace(originals.get(ShiftLockPatch.OWNER),target));DexPool pool=new DexPool(Opcodes.forApi(26));for(ClassDef c:updated.values())pool.internClass(c);Path nativeDex=out.resolve("shift-native-patched.dex");FileDataStore store=new FileDataStore(nativeDex.toFile());try{pool.writeTo(store);}finally{store.close();}
  Map<String,ClassDef> loaded=read(nativeDex);for(ClassDef c:updated.values())for(Method m:c.getMethods())if(m.getImplementation()!=null)check(MethodContract.sha256(m).equals(MethodContract.sha256(find(loaded,c.getType(),m.getName()))),"Native DEX roundtrip method preserved "+c.getType()+m.getName());
  behavioral(originals,loaded);
  boolean rejected=false;String before=MethodContract.sha256(target);try{ShiftLockPatch.applyMethod(target);}catch(IllegalStateException e){rejected=true;}check(rejected&&before.equals(MethodContract.sha256(target)),"Duplicate application fails before mutation");
  MutableMethod drift=new MutableMethod(original);drift.getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));before=MethodContract.sha256(drift);rejected=false;try{ShiftLockPatch.applyMethod(drift);}catch(IllegalStateException e){rejected=true;}check(rejected&&before.equals(MethodContract.sha256(drift)),"Native drift fails before mutation");
  rejected=false;try{ShiftLockPatch.inspect(null);}catch(IllegalStateException e){rejected=true;}check(rejected,"Missing native Shift controller rejected");
  String report="{\"status\":\"PASSED\",\"assertions\":"+assertions+",\"native_method\":\"Lwb0/p1;->n(Llh0/a;)V\",\"baseline_method_sha256\":\""+MethodContract.sha256(original)+"\",\"patched_method_sha256\":\""+MethodContract.sha256(target)+"\",\"native_methods_changed\":1,\"native_instructions_replaced\":1,\"inverse_native_method_restoration\":true,\"native_dex_interpreter_tests\":true,\"auto_caps_and_hardware_methods_preserved\":true,\"device_tested\":false,\"original_apk_apply_tested\":false,\"long_press_timing_unchanged\":false}\n";Files.writeString(out.resolve("native-shift-report.json"),report);System.out.print(report);
 }
}
