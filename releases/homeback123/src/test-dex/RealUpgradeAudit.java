import java.io.*;import java.util.*;import java.util.zip.*;import java.nio.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.dexbacked.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.instruction.formats.*;import com.android.tools.smali.dexlib2.iface.reference.*;

/** Independent full-APK executable audit. Normalizes only encoding-width choices,
 * no-op padding and debug positions; references, actual argument registers, data,
 * control-flow destinations, exception routes and metadata remain exact. */
public final class RealUpgradeAudit {
 static final String PREFIX="Lapp/hiro/oneback/runtime/";
 static int assertions,classes,methods,runtimeCount;static long jumboDeltas,gotoDeltas,nopDeltas,redundantHandlerDeltas;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static String key(MethodReference m){return m.getDefiningClass()+"->"+m.getName()+m.getParameterTypes()+m.getReturnType();}
 static String fieldKey(FieldReference f){return f.getName()+":"+f.getType();}
 static Map<String,ClassDef> read(String p)throws Exception{Map<String,ClassDef> out=new TreeMap<>();MultiDexContainer<? extends DexFile>d=DexFileFactory.loadDexContainer(new File(p),Opcodes.getDefault());for(String entry:d.getDexEntryNames())for(ClassDef c:d.getEntry(entry).getDexFile().getClasses())check(out.put(c.getType(),c)==null,"duplicate class "+c.getType());return out;}
 static Map<String,ClassDef> runtime(String p)throws Exception{try(ZipFile z=new ZipFile(p)){byte[]b=z.getInputStream(z.getEntry("extensions/oneback_exit.mpe")).readAllBytes();Map<String,ClassDef> out=new TreeMap<>();for(ClassDef c:new DexBackedDexFile(Opcodes.getDefault(),ByteBuffer.wrap(b)).getClasses())out.put(c.getType(),c);return out;}}
 static void classMeta(ClassDef a,ClassDef b){String id=a.getType();check(a.getAccessFlags()==b.getAccessFlags(),id+" flags");check(Objects.equals(a.getSuperclass(),b.getSuperclass()),id+" super");check(a.getInterfaces().equals(b.getInterfaces()),id+" interfaces");check(Objects.equals(a.getSourceFile(),b.getSourceFile()),id+" source");check(a.getAnnotations().equals(b.getAnnotations()),id+" annotations");Map<String,Field>af=new TreeMap<>(),bf=new TreeMap<>();for(Field f:a.getFields())af.put(fieldKey(f),f);for(Field f:b.getFields())bf.put(fieldKey(f),f);check(af.keySet().equals(bf.keySet()),id+" field inventory");for(String k:af.keySet()){Field x=af.get(k),y=bf.get(k);check(x.getAccessFlags()==y.getAccessFlags()&&x.getAnnotations().equals(y.getAnnotations())&&x.getHiddenApiRestrictions().equals(y.getHiddenApiRestrictions())&&Objects.equals(x.getInitialValue(),y.getInitialValue()),id+" field metadata "+k);}}
 static void methodMeta(Method a,Method b){String id=key(a);check(a.getAccessFlags()==b.getAccessFlags()&&a.getAnnotations().equals(b.getAnnotations())&&a.getHiddenApiRestrictions().equals(b.getHiddenApiRestrictions()),id+" method metadata");check(a.getParameters().size()==b.getParameters().size(),id+" parameter count");for(int i=0;i<a.getParameters().size();i++)check(a.getParameters().get(i).getAnnotations().equals(b.getParameters().get(i).getAnnotations()),id+" parameter annotations "+i);}
 static boolean data(Instruction i){return i instanceof SwitchPayload||i instanceof ArrayPayload;}
 static int location(Map<Integer,Integer> map,int address,String why){Integer n=map.get(address);if(n==null)throw new AssertionError("nonboundary "+address+" "+why);return n;}
 static String code(Method method){
  MethodImplementation b=method.getImplementation();if(b==null)return "no body";
  List<Instruction>all=new ArrayList<>();List<Integer> pcs=new ArrayList<>();Map<Integer,Instruction>byPc=new HashMap<>();Map<Integer,Integer> logical=new HashMap<>();int pc=0,n=0;
  for(Instruction i:b.getInstructions()){all.add(i);pcs.add(pc);byPc.put(pc,i);logical.put(pc,n);if(i.getOpcode()!=Opcode.NOP&&!data(i))n++;pc+=i.getCodeUnits();}logical.put(pc,n);
  StringBuilder s=new StringBuilder("registers=").append(b.getRegisterCount()).append('\n');
  for(int k=0;k<all.size();k++){Instruction i=all.get(k);if(i.getOpcode()==Opcode.NOP||data(i))continue;Opcode op=i.getOpcode();s.append(op==Opcode.CONST_STRING_JUMBO?Opcode.CONST_STRING:op==Opcode.GOTO_16||op==Opcode.GOTO_32?Opcode.GOTO:op);
   if(i instanceof OneRegisterInstruction)s.append(" A").append(((OneRegisterInstruction)i).getRegisterA());
   if(i instanceof TwoRegisterInstruction)s.append(" B").append(((TwoRegisterInstruction)i).getRegisterB());
   if(i instanceof ThreeRegisterInstruction)s.append(" C").append(((ThreeRegisterInstruction)i).getRegisterC());
   if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;int[]rs={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};s.append(" ARGS").append(Arrays.toString(Arrays.copyOf(rs,r.getRegisterCount())));}
   if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;s.append(" RANGE ").append(r.getStartRegister()).append(',').append(r.getRegisterCount());}
   if(i instanceof WideLiteralInstruction)s.append(" L").append(((WideLiteralInstruction)i).getWideLiteral());
   if(i instanceof ReferenceInstruction){String r=((ReferenceInstruction)i).getReference().toString();s.append(" REF").append(r.length()).append(':').append(r);}
   if(i instanceof DualReferenceInstruction){String r=((DualReferenceInstruction)i).getReference2().toString();s.append(" REF2").append(r.length()).append(':').append(r);}
   if(i instanceof OffsetInstruction){int dest=pcs.get(k)+((OffsetInstruction)i).getCodeOffset();Instruction p=byPc.get(dest);
    if(op==Opcode.PACKED_SWITCH||op==Opcode.SPARSE_SWITCH){check(p instanceof SwitchPayload,"missing switch payload "+key(method));s.append(" CASES[");for(SwitchElement e:((SwitchPayload)p).getSwitchElements())s.append(e.getKey()).append(':').append(location(logical,pcs.get(k)+e.getOffset(),key(method))).append(',');s.append(']');}
    else if(op==Opcode.FILL_ARRAY_DATA){check(p instanceof ArrayPayload,"missing array payload "+key(method));ArrayPayload ap=(ArrayPayload)p;s.append(" ARRAY").append(ap.getElementWidth()).append(':').append(ap.getArrayElements());}
    else{s.append(" TARGET").append(location(logical,dest,key(method)));check(p==null||!data(p),"branch into data "+key(method));}
   }
   // These optimized-only operands are not silently discarded if present.
   if(i instanceof VerificationErrorInstruction)s.append(" VERIFY").append(((VerificationErrorInstruction)i).getVerificationError());
   if(i instanceof FieldOffsetInstruction)s.append(" FIELD_OFFSET").append(((FieldOffsetInstruction)i).getFieldOffset());
   if(i instanceof InlineIndexInstruction)s.append(" INLINE").append(((InlineIndexInstruction)i).getInlineIndex());
   if(i instanceof VtableIndexInstruction)s.append(" VTABLE").append(((VtableIndexInstruction)i).getVtableIndex());
   s.append('\n');
  }
  int priorStart=-1,priorEnd=-1;String priorHandlers=null;
  for(TryBlock<? extends ExceptionHandler> t:b.getTryBlocks()){
   int from=location(logical,t.getStartCodeAddress(),key(method)),to=location(logical,t.getStartCodeAddress()+t.getCodeUnitCount(),key(method));StringBuilder handlers=new StringBuilder();Set<String>seen=new HashSet<>();
   // DEX searches handlers in order. A repeated exact type after its first
   // handler, or anything after catch-all, cannot be selected. No subtype
   // relationship is inferred and no first handler is discarded.
   for(ExceptionHandler h:t.getExceptionHandlers()){if(!seen.add(h.getExceptionType()))continue;handlers.append(" CATCH ").append(h.getExceptionType()).append(':').append(location(logical,h.getHandlerCodeAddress(),key(method)));if(h.getExceptionType()==null)break;}
   String current=handlers.toString();check(priorEnd<=from,"overlapping exception regions "+key(method));
   if(priorEnd==from&&current.equals(priorHandlers)){priorEnd=to;continue;}
   if(priorHandlers!=null)s.append("TRY ").append(priorStart).append(':').append(priorEnd).append(priorHandlers).append('\n');
   priorStart=from;priorEnd=to;priorHandlers=current;
  }
  if(priorHandlers!=null)s.append("TRY ").append(priorStart).append(':').append(priorEnd).append(priorHandlers).append('\n');
  return s.toString();
 }
 static int count(Method m,Opcode op){if(m.getImplementation()==null)return 0;int n=0;for(Instruction i:m.getImplementation().getInstructions())if(i.getOpcode()==op)n++;return n;}
 static int redundantHandlers(Method m){int count=0;if(m.getImplementation()!=null)for(TryBlock<? extends ExceptionHandler> t:m.getImplementation().getTryBlocks()){Set<String>seen=new HashSet<>();boolean all=false;for(ExceptionHandler h:t.getExceptionHandlers()){if(all||!seen.add(h.getExceptionType()))count++;if(h.getExceptionType()==null)all=true;}}return count;}
 static void recordEncoding(Method a,Method b){jumboDeltas+=Math.abs(count(a,Opcode.CONST_STRING_JUMBO)-count(b,Opcode.CONST_STRING_JUMBO));gotoDeltas+=Math.abs(count(a,Opcode.GOTO_32)-count(b,Opcode.GOTO_32))+Math.abs(count(a,Opcode.GOTO_16)-count(b,Opcode.GOTO_16));nopDeltas+=Math.abs(count(a,Opcode.NOP)-count(b,Opcode.NOP));redundantHandlerDeltas+=Math.abs(redundantHandlers(a)-redundantHandlers(b));}
 static Map<String,Method> methods(ClassDef c){Map<String,Method> out=new TreeMap<>();for(Method m:c.getMethods())out.put(key(m),m);return out;}
 static boolean resolves(Map<String,ClassDef> rt,Reference reference){
  String owner=reference instanceof MethodReference?((MethodReference)reference).getDefiningClass():((FieldReference)reference).getDefiningClass();Set<String>seen=new HashSet<>();ArrayDeque<String>q=new ArrayDeque<>();q.add(owner);
  while(!q.isEmpty()){String type=q.removeFirst();if(!seen.add(type))continue;ClassDef c=rt.get(type);if(c==null)continue;
   if(reference instanceof MethodReference){MethodReference r=(MethodReference)reference;for(Method m:c.getMethods())if(m.getName().equals(r.getName())&&m.getParameterTypes().equals(r.getParameterTypes())&&m.getReturnType().equals(r.getReturnType()))return true;}
   else{FieldReference r=(FieldReference)reference;for(Field f:c.getFields())if(f.getName().equals(r.getName())&&f.getType().equals(r.getType()))return true;}
   if(c.getSuperclass()!=null)q.add(c.getSuperclass());q.addAll(c.getInterfaces());
  }return false;
 }
 static void hookCall(Instruction i,String name,String result,List<String>params,int...args){
  check(i.getOpcode()==Opcode.INVOKE_STATIC&&i instanceof FiveRegisterInstruction,"static runtime hook "+name);MethodReference r=InstagramDispatcherAudit.ref(i);
  check(r.getDefiningClass().equals(PREFIX+"OneBackExit;")&&r.getName().equals(name)&&r.getReturnType().equals(result)&&r.getParameterTypes().equals(params),"runtime hook descriptor "+name);
  FiveRegisterInstruction f=(FiveRegisterInstruction)i;int[]regs={f.getRegisterC(),f.getRegisterD(),f.getRegisterE(),f.getRegisterF(),f.getRegisterG()};check(f.getRegisterCount()==args.length&&Arrays.equals(Arrays.copyOf(regs,args.length),args),"runtime hook registers "+name);
 }
 static void moveResult(Instruction i,int reg){check(i.getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)i).getRegisterA()==reg,"exact hook move-result");}
 static void gateExact(Method method,String name){List<Instruction>b=InstagramDispatcherAudit.ins(method);hookCall(b.get(0),name,"Z",Collections.singletonList("Ljava/lang/Object;"),4);moveResult(b.get(1),0);check(b.get(2).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)b.get(2)).getRegisterA()==0&&InstagramDispatcherAudit.target(b,2)==4,"entry false native continuation");check(b.get(3).getOpcode()==Opcode.RETURN_VOID,"entry true return");}
 static void compare(String id,Method a,Method b)throws Exception{methodMeta(a,b);String x=code(a),y=code(b);if(!x.equals(y)){int n=0;while(n<Math.min(x.length(),y.length())&&x.charAt(n)==y.charAt(n))n++;String before=x.substring(Math.max(0,n-160),Math.min(x.length(),n+400)),after=y.substring(Math.max(0,n-160),Math.min(y.length(),n+400));throw new AssertionError("Executable delta "+id+" at "+n+"\nBEFORE\n"+before+"\nAFTER\n"+after);}recordEncoding(a,b);}
 public static void main(String[]a)throws Exception{
  Map<String,ClassDef> old=read(a[0]),fresh=read(a[1]),rt=runtime(a[2]);Set<String>allowed=new HashSet<>(Arrays.asList("Landroidx/activity/ComponentActivity;->CoN[]LX/01h;","LX/01h;->A04[]V","LX/01h;->A00[LX/01h;]V","LX/0Ok;->invoke[Ljava/lang/Object;]Ljava/lang/Object;"));Set<String>changed=new TreeSet<>();
  Instruction first=InstagramDispatcherAudit.ins(InstagramDispatcherAudit.find(old.get("LX/01h;"),"A04")).get(0);boolean priorDispatcher=first instanceof ReferenceInstruction&&((ReferenceInstruction)first).getReference() instanceof MethodReference&&((MethodReference)((ReferenceInstruction)first).getReference()).getName().equals("tryInstagramDispatcherBack");
  if(priorDispatcher){app.hiro.oneback.patches.OneBackPatch.verifyInstagramDispatcher(old);allowed.clear();}
  String nativeTab="LX/2xZ;->A0V[]Z";allowed.add(nativeTab);
  for(ClassDef c:old.values()){if(c.getType().startsWith(PREFIX))continue;ClassDef n=fresh.get(c.getType());check(n!=null,"removed class "+c.getType());classMeta(c,n);Map<String,Method>om=methods(c),nm=methods(n);check(om.keySet().equals(nm.keySet()),"method inventory "+c.getType());for(String id:om.keySet()){Method x=om.get(id),y=nm.get(id);if(allowed.contains(id)){check(!code(x).equals(code(y)),"expected hook absent "+id);compare(id,x,id.equals(nativeTab)?InstagramTabAudit.restore(y):InstagramDispatcherAudit.restored(y));changed.add(id);}else compare(id,x,y);methods++;}if(++classes%10000==0)System.out.println("Compared appClasses="+classes+" methods="+methods);}
  check(changed.equals(allowed),"exact expected native hook methods");
  for(ClassDef c:rt.values()){ClassDef n=fresh.get(c.getType());check(n!=null,"missing emitted runtime "+c.getType());classMeta(c,n);Map<String,Method>om=methods(c),nm=methods(n);check(om.keySet().equals(nm.keySet()),"runtime inventory "+c.getType());for(String id:om.keySet())compare(id,om.get(id),nm.get(id));runtimeCount++;}
  for(ClassDef c:fresh.values()){check(old.containsKey(c.getType())||rt.containsKey(c.getType()),"unexpected class "+c.getType());if(c.getType().startsWith(PREFIX))check(rt.containsKey(c.getType()),"stale runtime "+c.getType());for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)i).getReference();if(r instanceof MethodReference&&((MethodReference)r).getDefiningClass().startsWith(PREFIX))check(resolves(rt,r),"unresolved runtime method "+r);if(r instanceof FieldReference&&((FieldReference)r).getDefiningClass().startsWith(PREFIX))check(resolves(rt,r),"unresolved runtime field "+r);}}
  // The independent dispatcher auditor validates exact new gates and targets.
  gateExact(InstagramDispatcherAudit.find(fresh.get("LX/01h;"),"A04"),"tryInstagramDispatcherBack");gateExact(InstagramDispatcherAudit.find(fresh.get("LX/01h;"),"A00"),"cancelInstagramDispatcherGesture");
  List<Instruction> getter=InstagramDispatcherAudit.ins(InstagramDispatcherAudit.find(fresh.get("Landroidx/activity/ComponentActivity;"),"CoN"));FiveRegisterInstruction bind=(FiveRegisterInstruction)getter.get(4);check(InstagramDispatcherAudit.ref(getter.get(4)).getName().equals("bindInstagramDispatcher")&&bind.getRegisterC()==1&&bind.getRegisterD()==0,"getter binds actual Activity and dispatcher return");
  List<Instruction> gesture=InstagramDispatcherAudit.ins(InstagramDispatcherAudit.find(fresh.get("LX/0Ok;"),"invoke"));check(InstagramDispatcherAudit.target(gesture,39)==41,"start incoming branch passes hook");check(InstagramDispatcherAudit.ref(gesture.get(41)).getName().equals("startInstagramDispatcherGesture")&&((FiveRegisterInstruction)gesture.get(41)).getRegisterC()==3,"start actual dispatcher");check(InstagramDispatcherAudit.ref(gesture.get(55)).getName().equals("progressInstagramDispatcherGesture")&&((FiveRegisterInstruction)gesture.get(55)).getRegisterC()==0,"progress actual dispatcher");check(InstagramDispatcherAudit.target(gesture,43)==77&&InstagramDispatcherAudit.target(gesture,57)==77,"suppressed start and progress go to Unit return");check(gesture.get(77).getOpcode()==Opcode.SGET_OBJECT&&gesture.get(78).getOpcode()==Opcode.RETURN_OBJECT,"Unit return is preserved");
  hookCall(getter.get(4),"bindInstagramDispatcher","V",Arrays.asList("Landroid/app/Activity;","Ljava/lang/Object;"),1,0);
  hookCall(gesture.get(41),"startInstagramDispatcherGesture","Z",Collections.singletonList("Ljava/lang/Object;"),3);moveResult(gesture.get(42),0);check(gesture.get(43).getOpcode()==Opcode.IF_NEZ&&((OneRegisterInstruction)gesture.get(43)).getRegisterA()==0,"start true Unit branch");
  hookCall(gesture.get(55),"progressInstagramDispatcherGesture","Z",Collections.singletonList("Ljava/lang/Object;"),0);moveResult(gesture.get(56),1);check(gesture.get(57).getOpcode()==Opcode.IF_NEZ&&((OneRegisterInstruction)gesture.get(57)).getRegisterA()==1,"progress true Unit branch");
  check(gesture.get(39).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)gesture.get(39)).getRegisterA()==0,"native branch enters start gate with original condition");
  InstagramTabAudit.validate(InstagramTabAudit.find(fresh.get("LX/2xZ;"),"A0V"));
  System.out.println("PASS whole APK executable semantics: appClasses="+classes+", appMethods="+methods+", nativeHookMethods="+changed+", priorDispatcherPreserved="+priorDispatcher+", emittedRuntimeClasses="+runtimeCount+", assertions="+assertions+", constStringWidthChanges="+jumboDeltas+", gotoWidthChanges="+gotoDeltas+", NopPaddingCountChanges="+nopDeltas+", redundantExceptionHandlerChanges="+redundantHandlerDeltas);
  System.out.println("References, used registers, constants, array data, switch cases, branch targets, selectable exception routes, class/field/method annotations and flags preserved. Normalized unreachable duplicate exact-type handlers and entries after catch-all; adjacent equal-handler ranges coalesced. Debug positions and parameter debug names excluded. Resources audited separately. Android device execution not performed.");
 }
}
