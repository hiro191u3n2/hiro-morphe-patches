import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;

/** Actual emitted/applied DEX safety gate. The comparison gate remains pinned
 * to published .104. The .116 probe pin includes a completed Strong-plan
 * comparison after both exact trials and route revalidation. The .115 pin
 * added owned-copy source geometry
 * and shape-aware reference checks; comparison gates and callback ABI remain
 * unchanged. The .112 probe admitted the reviewed preflight/profile
 * changes over .111 observations and .110 typed-refusal flow. Historical
 * classifier/bridge pins stay unchanged; the active .112 bridge is also pinned.
 * Exact source inverses preserve .112 -> .111 -> .110 -> .108. The .108
 * Finish-memory helper and active String bridge stay pinned unchanged.
 * Independent handler CFG checks and negative controls supplement that pin. */
public final class VerifyStrongDiagnostic19105 {
 static final String P="Lcom/hiro/ulike/QualityPipeline1932;",R="Lcom/hiro/ulike/ResidentStages19104;",S="Lcom/hiro/ulike/StrongInputs19105;",G="Lcom/hiro/ulike/GpuResident1976;";
 static final Map<String,String> STRICT=Map.of(
  G+"->acceptComparison19103(Ljava/lang/String;ZI)Z","f15602578073b0c765b946a9869991b00bfb0be1d2650632778c1f062c89b68a",
  "Lcom/hiro/ulike/GpuResident1976$1;->run(Lcom/hiro/ulike/GpuQualification1961$Cancellation;)V","9a8f8de65bf00dee7a14701d7a1401f0d757993c699837532c74b26ccd8eed28",
  G+"->memoryRefusal19108(Ljava/lang/RuntimeException;)Z","e36bc205bc6e1b21898619b74254bf98a86cbc94aac47d4d2841c5e9646ff2b5",
  G+"->admissionRefusal19110(Ljava/lang/RuntimeException;)Ljava/lang/String;","ff7103f81b68f8b4ba4357c893a98951e6337590065413e6c7e25376a2b393c3",
  G+"->access$1400(Ljava/lang/RuntimeException;)Ljava/lang/String;","ce860555baea65f28f84e532a994a404159c72a0fab4e9012da39accfff937a8",
  "Lcom/hiro/ulike/GpuStrong1960;->residentAdmissionReason19110(Ljava/lang/RuntimeException;)Ljava/lang/String;","d61ff8dbcfade2544ba420a63308ad5b987d6d6e7a2b2fb0dda73d9731ab285d",
  "Lcom/hiro/ulike/GpuStrong1960;->residentReason19110(I)Ljava/lang/String;","9b374981a84aed4d43fb97cec1cd79f008995dfe4287d2175ac48d0219e7c7e3");
 static final String TIMING="Lcom/hiro/ulike/ResidentTiming19111;",CHAIN="Lcom/hiro/ulike/GpuChain1961;",QUALIFICATION="Lcom/hiro/ulike/GpuQualification1961;",PROBE="Lcom/hiro/ulike/GpuResident1976$1;";
 static final Map<String,String> FAIR_COMPARATORS=Map.of(
  QUALIFICATION+"->comparableResidentPlans19116()Z","2aa3a86b7aa3996a90ee1d6ce4fb2b62e90a9d965802d2fedb23a596605aacfb",
  QUALIFICATION+"->comparableResidentPlans19116([J)Z","a1a5fb3edc79bc8f284f62c13a352fe2f0137a27d8ed097d29592945d1793488");
 /** The entire observer and each fault-contained wrapper are immutable. The
  * independent source inverse also proves .111 -> public .110 -> public .108. */
 static final Map<String,String> TIMING_CLASSES=Map.of(
  TIMING,"57d7e4c2b22c45f9a79d9a293428588a7f2e1332de4e21e7f81b2d835b2c02a3",
  "Lcom/hiro/ulike/ResidentTiming19111$Sample;","b9da41f8877b2fbe8a08536c8bd0b5f7d5132fe78a4e978e002c41a1dd8585eb",
  "Lcom/hiro/ulike/ResidentTiming19111$CpuSample19115;","fc913b55198ca5d2b01439c65637c5810fcdcbad3e7e7233dd51ffd581bb8343");
 // Sample's exact whole-class pin changes only because the preceding new CPU
 // observer shifts DEX debug line positions. All three executable method pins
 // below remain the published114 values; no debug mask is used by this gate.
 static final Map<String,String> SAMPLE_METHODS=Map.of(
  "Lcom/hiro/ulike/ResidentTiming19111$Sample;-><init>()V","28ac7647c3197c9d1246f02277681d779793d48b111a1cd7490577287ab36298",
  "Lcom/hiro/ulike/ResidentTiming19111$Sample;->interval(JZ)V","cf25be9bb839d39f5874a39c0e9931620b63474fed119deaaedb70f440db5edc",
  "Lcom/hiro/ulike/ResidentTiming19111$Sample;->stop(JI)V","cbd824da5ed285bf1a965507657a254b65e90ae66aee1722d5e251564099e6f3");
 static final Set<String> CPU_TIMING_OPERATIONS=Set.of("beginCpu19115","stopCpu19115","publishCpu19115");
 static final Map<String,String> TIMING_WRAPPERS=Map.of(
  G,"6c451eb401aecdc456885c3794cfa1f981d5bc4f32f20466277d29bb856c8015",
  P,"4f03a0199893d8f682e93f0f716055bbb2476262532978863f58f66fdca9b062",
  CHAIN,"55488f269c8d8b392579b1590843db564ba19d910a295170daf0bb625bfebf0c");
 static void req(boolean v,String s){MergePayloads.require(v,s);}
 static List<Instruction> body(Method m){req(m!=null&&m.getImplementation()!=null,"Missing diagnostic method");List<Instruction> a=new ArrayList<>();for(Instruction i:m.getImplementation().getInstructions())a.add(i);return a;}
 static Method method(Map<String,ClassDef> cs,String owner,String name){ClassDef c=cs.get(owner);req(c!=null,"Missing diagnostic class "+owner);Method found=null;for(Method m:c.getMethods())if(m.getName().equals(name)){req(found==null,"Ambiguous diagnostic method "+name);found=m;}req(found!=null,"Missing diagnostic method "+name);return found;}
 static MethodReference ref(Instruction i){return i instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference m?m:null;}
 static int call(List<Instruction> a,String owner,String name){int at=-1;for(int i=0;i<a.size();i++){MethodReference r=ref(a.get(i));if(r!=null&&r.getDefiningClass().equals(owner)&&r.getName().equals(name)){req(at<0,"Duplicate guarded call "+name);at=i;}}req(at>=0,"Guarded call missing "+name);return at;}
 static int destination(List<Instruction> a,int from){int address=0;Map<Integer,Integer> indexes=new HashMap<>();for(int i=0;i<a.size();i++){indexes.put(address,i);address+=a.get(i).getCodeUnits();}address=0;for(int i=0;i<from;i++)address+=a.get(i).getCodeUnits();Integer target=indexes.get(address+((OffsetInstruction)a.get(from)).getCodeOffset());req(target!=null,"Invalid diagnostic branch");return target;}
 static boolean reaches(List<Instruction> a,int start,int target,int blocked){Set<Integer> seen=new HashSet<>();ArrayDeque<Integer> todo=new ArrayDeque<>();todo.add(start);while(!todo.isEmpty()){int i=todo.removeFirst();if(i==blocked||i<0||i>=a.size()||!seen.add(i))continue;if(i==target)return true;Instruction in=a.get(i);String op=in.getOpcode().name();if(op.startsWith("RETURN")||op.equals("THROW"))continue;if(op.startsWith("GOTO")){todo.add(destination(a,i));continue;}if(op.startsWith("IF_"))todo.add(destination(a,i));else req(!op.contains("SWITCH"),"Unexpected switch in diagnostic guard");todo.add(i+1);}return false;}
 static void guarded(List<Instruction> a,int branch,int target,boolean failureTaken,String label){req(!reaches(a,0,target,branch),label+" can bypass guard");int jump=destination(a,branch),pass=failureTaken?branch+1:jump,fail=failureTaken?jump:branch+1;req(reaches(a,pass,target,-1),label+" success arm cannot reach work");req(!reaches(a,fail,target,-1),label+" rejected arm reaches work");}
 static int booleanGuard(Method m,String guardOwner,String guard,String targetOwner,String target){List<Instruction>a=body(m);int work=call(a,targetOwner,target),invoke=-1;for(int i=0;i<work;i++){MethodReference r=ref(a.get(i));if(r!=null&&r.getDefiningClass().equals(guardOwner)&&r.getName().equals(guard))invoke=i;}req(invoke>=0&&invoke+2<a.size()&&a.get(invoke+1).getOpcode()==Opcode.MOVE_RESULT,"Boolean guard result absent");int reg=((OneRegisterInstruction)a.get(invoke+1)).getRegisterA(),branch=invoke+2;req(a.get(branch) instanceof OneRegisterInstruction r&&r.getRegisterA()==reg&&(a.get(branch).getOpcode()==Opcode.IF_EQZ||a.get(branch).getOpcode()==Opcode.IF_NEZ),"Boolean guard branch absent");guarded(a,branch,work,a.get(branch).getOpcode()==Opcode.IF_EQZ,"Foreground CPU oracle");return branch;}
 static Integer literal(List<Instruction>a,int before,int register){for(int i=before-1;i>=0;i--){Instruction x=a.get(i);if(x instanceof OneRegisterInstruction r&&r.getRegisterA()==register&&x.getOpcode().setsRegister()){return x instanceof NarrowLiteralInstruction n?n.getNarrowLiteral():null;}}return null;}
 static void fieldGuard(Method m,String field,Integer equals,Opcode unary,int work){List<Instruction>a=body(m);boolean found=false;for(int i=0;i<a.size();i++)if(a.get(i) instanceof ReferenceInstruction rr&&rr.getReference() instanceof FieldReference f&&f.getName().equals(field)&&a.get(i).getOpcode().name().startsWith("IGET")){int reg=((OneRegisterInstruction)a.get(i)).getRegisterA();for(int j=i+1;j<Math.min(a.size(),i+5);j++){Instruction b=a.get(j);if(equals!=null&&b instanceof TwoRegisterInstruction two&&(b.getOpcode()==Opcode.IF_NE||b.getOpcode()==Opcode.IF_EQ)){int other=two.getRegisterA()==reg?two.getRegisterB():two.getRegisterB()==reg?two.getRegisterA():-1;if(other<0||!equals.equals(literal(a,j,other)))continue;guarded(a,j,work,b.getOpcode()==Opcode.IF_NE,"Replay field "+field);found=true;break;}if(equals==null&&(b.getOpcode()==unary||unary==Opcode.IF_EQZ&&b.getOpcode()==Opcode.IF_NEZ||unary==Opcode.IF_LTZ&&b.getOpcode()==Opcode.IF_GEZ)&&b instanceof OneRegisterInstruction one&&one.getRegisterA()==reg){guarded(a,j,work,b.getOpcode()==unary,"Replay field "+field);found=true;break;}}if(found)break;}req(found,"Replay field guard absent: "+field);}
 static int nullGuard(Method m,String owner,String name,String workOwner,String workName){List<Instruction>a=body(m);int at=call(a,owner,name),work=call(a,workOwner,workName);req(a.get(at+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"Target object result absent");int reg=((OneRegisterInstruction)a.get(at+1)).getRegisterA(),branch=at+2;Instruction b=a.get(branch);req(b instanceof OneRegisterInstruction r&&r.getRegisterA()==reg&&(b.getOpcode()==Opcode.IF_EQZ||b.getOpcode()==Opcode.IF_NEZ),"Target-null guard absent");guarded(a,branch,work,b.getOpcode()==Opcode.IF_EQZ,"Absent diagnostic target");return branch;}
 static int[] callArguments(Instruction in){
  if(in instanceof FiveRegisterInstruction f){int[] values={f.getRegisterC(),f.getRegisterD(),f.getRegisterE(),f.getRegisterF(),f.getRegisterG()};return Arrays.copyOf(values,f.getRegisterCount());}
  req(in instanceof RegisterRangeInstruction,"Unexpected resident call encoding");RegisterRangeInstruction r=(RegisterRangeInstruction)in;int[] values=new int[r.getRegisterCount()];for(int i=0;i<values.length;i++)values[i]=r.getStartRegister()+i;return values;
 }
 static Map<Integer,Integer> addressIndex(List<Instruction> code){Map<Integer,Integer> result=new HashMap<>();int address=0;for(int i=0;i<code.size();i++){result.put(address,i);address+=code.get(i).getCodeUnits();}return result;}
 /** Conservative exception edges ensure the typed failure path cannot reenter
  * any exact comparison, replay, timing stage or successful certification. */
 static boolean exceptionReaches(Method m,int start,Set<Integer> forbidden){
  List<Instruction> code=body(m);Map<Integer,Integer> indexes=addressIndex(code);int[] addresses=new int[code.size()];int address=0;for(int i=0;i<code.size();i++){addresses[i]=address;address+=code.get(i).getCodeUnits();}
  Set<Integer> seen=new HashSet<>();ArrayDeque<Integer> todo=new ArrayDeque<>();todo.add(start);
  while(!todo.isEmpty()){int i=todo.removeFirst();if(i<0||i>=code.size()||!seen.add(i))continue;if(forbidden.contains(i))return true;
   for(TryBlock<? extends ExceptionHandler> block:m.getImplementation().getTryBlocks())if(addresses[i]>=block.getStartCodeAddress()&&addresses[i]<block.getStartCodeAddress()+block.getCodeUnitCount())for(ExceptionHandler h:block.getExceptionHandlers()){Integer next=indexes.get(h.getHandlerCodeAddress());req(next!=null,"Invalid resident handler address");todo.add(next);}
   Instruction in=code.get(i);String op=in.getOpcode().name();if(op.startsWith("RETURN")||op.equals("THROW"))continue;if(op.startsWith("GOTO")){todo.add(destination(code,i));continue;}if(op.startsWith("IF_"))todo.add(destination(code,i));else req(!op.contains("SWITCH"),"Unexpected resident handler switch");todo.add(i+1);
  }return false;
 }
 static int residentMemoryGuard(Map<String,ClassDef> cs){
  String owner="Lcom/hiro/ulike/GpuResident1976$1;",q="Lcom/hiro/ulike/GpuQualification1961;";
  Method run=method(cs,owner,"run"),classifier=method(cs,G,"admissionRefusal19110"),bridge=null;
  req((classifier.getAccessFlags()&0x10f)==0xa,"Resident classifier must remain private static");
  List<Instruction> a=body(run);
  // Select the unique classifier bridge actually invoked by this pinned Probe;
  // the active bridge body is independently hash-pinned.
  for(Method m:cs.get(G).getMethods())if(m.getImplementation()!=null)for(Instruction i:body(m)){MethodReference r=ref(i);if(r!=null&&r.getDefiningClass().equals(G)&&r.getName().equals("admissionRefusal19110")&&r.getReturnType().equals("Ljava/lang/String;")){
   boolean used=false;for(Instruction invocation:a){MethodReference target=ref(invocation);if(target!=null&&target.toString().equals(MergePayloads.id(m)))used=true;}
   if(used){req(bridge==null,"Ambiguous active resident classifier bridge");bridge=m;}
  }}
  req(bridge!=null&&(bridge.getAccessFlags()&0x1008)==0x1008&&bridge.getReturnType().equals("Ljava/lang/String;"),"Resident classifier must use its reviewed synthetic String bridge");
  int invoke=call(a,G,bridge.getName());int[] arguments=callArguments(a.get(invoke));req(arguments.length==1,"Resident classifier requires the original exception");
  req(a.get(invoke+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"Resident classifier result missing");int branch=invoke+2,result=((OneRegisterInstruction)a.get(invoke+1)).getRegisterA();
  req(a.get(branch).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)a.get(branch)).getRegisterA()==result,"Resident classifier null arm must rethrow");
  Instruction rethrow=a.get(destination(a,branch));req(rethrow.getOpcode()==Opcode.THROW&&((OneRegisterInstruction)rethrow).getRegisterA()==arguments[0],"Unknown runtime exception must be rethrown unchanged");
  int reject=-1;Set<Integer> forbidden=new HashSet<>();for(int i=0;i<a.size();i++){MethodReference r=ref(a.get(i));if(r==null)continue;
   if(i>branch&&r.getDefiningClass().equals(q)&&r.getName().equals("rejectNonSpeed1988")&&reject<0)reject=i;
   if(r.getDefiningClass().equals(q)&&r.getName().equals("qualified")||r.getDefiningClass().equals(P)&&(r.getName().equals("compareStrongFinish1978")||r.getName().equals("strongFinish1978")||r.getName().equals("strongFinishOwned1978")))forbidden.add(i);
  }
  req(reject>branch&&forbidden.size()>=4,"Resident rejection or preserved comparison/timing calls missing");
  req(reaches(a,branch+1,reject,-1)&&!reaches(a,invoke,reject,branch),"Typed refusal can bypass the classifier or cannot record a refusal");
  int[] rejectionArguments=callArguments(a.get(reject));req(rejectionArguments.length==2&&rejectionArguments[1]==result,"Resident rejection must use the classified reason");
  for(int i=branch+1;i<reject;i++)req(!(a.get(i).getOpcode().setsRegister()&&a.get(i) instanceof OneRegisterInstruction r&&r.getRegisterA()==result),"Classified refusal reason overwritten before rejection");
  for(String guard:new String[]{"cancelled","isInterrupted"}){int at=-1;for(int i=branch+1;i<reject;i++){MethodReference r=ref(a.get(i));if(r!=null&&r.getName().equals(guard))at=i;}req(at>=0&&a.get(at+1).getOpcode()==Opcode.MOVE_RESULT,"Resident cancellation check absent: "+guard);int b=at+2;req(a.get(b).getOpcode()==Opcode.IF_NEZ&&((OneRegisterInstruction)a.get(b)).getRegisterA()==((OneRegisterInstruction)a.get(at+1)).getRegisterA(),"Resident cancellation branch changed: "+guard);req(!reaches(a,branch+1,reject,b)&&!reaches(a,destination(a,b),reject,-1),"Cancellation may be mislabeled as operational refusal: "+guard);}
  Map<Integer,Integer> indexes=addressIndex(a);Set<Integer> handlers=new HashSet<>();for(TryBlock<? extends ExceptionHandler> block:run.getImplementation().getTryBlocks())for(ExceptionHandler h:block.getExceptionHandlers())if("Ljava/lang/RuntimeException;".equals(h.getExceptionType())){Integer entry=indexes.get(h.getHandlerCodeAddress());req(entry!=null,"Invalid resident typed handler");handlers.add(entry);}
  req(!handlers.isEmpty(),"Resident typed catch absent");for(int entry:handlers)req(!exceptionReaches(run,entry,forbidden),"Resident refusal can enter comparison/replay/qualification");
  req(!exceptionReaches(run,invoke,forbidden),"Resident refusal classification can reenter successful validation");return branch;
 }
 static int timingStringSite(List<Instruction> code,int invoke){
  int register=callArguments(code.get(invoke))[0];
  for(int i=invoke-1;i>=0;i--){Instruction value=code.get(i);
   if(value.getOpcode().setsRegister()&&value instanceof OneRegisterInstruction r&&r.getRegisterA()==register){
    if(value.getOpcode().name().startsWith("MOVE_OBJECT")&&value instanceof TwoRegisterInstruction move){register=move.getRegisterB();continue;}
    req(value instanceof ReferenceInstruction ref&&ref.getReference() instanceof StringReference,"Timing operation is not an immutable string");return i;
   }
  }throw new IllegalStateException("Timing operation is unbound");
 }
 static Map<String,List<Integer>> timingScope(Map<String,ClassDef> classes){
  Method run=method(classes,PROBE,"run");List<Instruction> code=body(run);Map<String,List<Integer>> sites=new TreeMap<>();
  for(int i=0;i<code.size();i++){MethodReference ref=ref(code.get(i));if(ref==null||!ref.getDefiningClass().equals(G)||!ref.getName().equals("residentTiming19111"))continue;
   req(ref.getReturnType().equals("V")&&ref.getParameterTypes().equals(List.of("Ljava/lang/String;","J","J","J")),"Timing must not supply processing results");
   int constant=timingStringSite(code,i);String operation=((StringReference)((ReferenceInstruction)code.get(constant)).getReference()).getString();
   sites.computeIfAbsent(operation,k->new ArrayList<>()).add(i);
   req(i+1>=code.size()||!code.get(i+1).getOpcode().name().startsWith("MOVE_RESULT"),"Timing return value enters processing flow");
  }
  // The independent CPU scope below owns only these three additional labels;
  // all original GPU lifecycle ordering/CFG checks and nine controls remain.
  sites.keySet().removeAll(CPU_TIMING_OPERATIONS);
  req(sites.keySet().equals(Set.of("begin","start","stop","publish")),"Unexpected timing scope operation");
  for(String name:new String[]{"begin","start","stop"})req(sites.get(name).size()==1,"Timing scope must have one "+name);
  int begin=sites.get("begin").get(0),start=sites.get("start").get(0),stop=sites.get("stop").get(0);req(begin<start&&start<stop,"Timing lifecycle order differs");
  List<Integer> clocks=new ArrayList<>(),work=new ArrayList<>();
  for(int i=begin+1;i<stop;i++){MethodReference ref=ref(code.get(i));if(ref==null)continue;
   if(ref.getDefiningClass().equals("Ljava/lang/System;")&&ref.getName().equals("nanoTime"))clocks.add(i);
   if(ref.getDefiningClass().equals(P)&&(ref.getName().equals("strongFinish1978")||ref.getName().equals("strongFinishOwned1978")))work.add(i);
  }
  req(clocks.size()==2&&work.size()==2&&clocks.get(0)<start&&start<work.get(0)&&work.get(1)<clocks.get(1),"Timing must retain the original two whole-call clock boundaries");
  for(int invocation:work)req(!reaches(code,0,invocation,begin)&&!reaches(code,0,invocation,start),"Whole speed work bypasses its timing scope");
  req(!reaches(code,begin,stop,start)&&!reaches(code,start,stop,clocks.get(1)),"Timing stop bypasses the unchanged end clock");
  Set<Integer> refusedWork=new HashSet<>(work);refusedWork.add(stop);
  for(int i=0;i<code.size();i++){MethodReference ref=ref(code.get(i));if(ref!=null&&ref.getDefiningClass().equals(QUALIFICATION)&&ref.getName().equals("qualified"))refusedWork.add(i);}
  for(int publish:sites.get("publish")){
   req(!reaches(code,0,publish,begin)&&!reaches(code,0,publish,start),"Timing publication bypasses its speed scope");
   // .112 may refuse the preflight before any timed pixel call. Its finally
   // still publishes an incomplete observation, then must leave the probe;
   // neither normal nor exception edges may reenter work, stop or admission.
   if(publish<stop)req(!exceptionReaches(run,publish,refusedWork),"Refused preflight publication can reenter successful speed validation");
  }
  // The private speed Probe is the only lifecycle entry, including all of its
  // existing return/exception finally arms. Pipeline wrappers only add phases.
  for(Method m:MergePayloads.methods(classes.values()).values())if(m.getImplementation()!=null)for(Instruction i:body(m)){MethodReference ref=ref(i);if(ref!=null&&ref.getDefiningClass().equals(G)&&ref.getName().equals("residentTiming19111"))req(MergePayloads.id(m).equals(MergePayloads.id(run)),"Timing lifecycle called outside the speed Probe");}
  return sites;
 }
 static Map<String,List<Integer>> cpuTimingScope(Map<String,ClassDef> classes){
  Method run=method(classes,PROBE,"run");List<Instruction> code=body(run);Map<String,List<Integer>> sites=new TreeMap<>();
  Set<String> allOperations=new HashSet<>();int wholeCalls=0,ownedCalls=0;
  for(int i=0;i<code.size();i++){
   MethodReference target=ref(code.get(i));if(target==null)continue;
   if(target.getDefiningClass().equals(P)&&target.getName().equals("strongFinish1978"))wholeCalls++;
   if(target.getDefiningClass().equals(P)&&target.getName().equals("strongFinishOwned1978"))ownedCalls++;
   if(!target.getDefiningClass().equals(G)||!target.getName().equals("residentTiming19111"))continue;
   req(target.getReturnType().equals("V")&&target.getParameterTypes().equals(List.of("Ljava/lang/String;","J","J","J")),"CPU observation must return no processing result");
   int constant=timingStringSite(code,i);String operation=((StringReference)((ReferenceInstruction)code.get(constant)).getReference()).getString();allOperations.add(operation);
   req(i+1>=code.size()||!code.get(i+1).getOpcode().name().startsWith("MOVE_RESULT"),"CPU observation result enters processing");
   if(CPU_TIMING_OPERATIONS.contains(operation))sites.computeIfAbsent(operation,k->new ArrayList<>()).add(i);
  }
  req(allOperations.equals(Set.of("begin","start","stop","publish","beginCpu19115","stopCpu19115","publishCpu19115")),"Unreviewed CPU/GPU observer operation");
  req(sites.keySet().equals(CPU_TIMING_OPERATIONS)&&sites.get("beginCpu19115").size()==1&&sites.get("stopCpu19115").size()==1&&sites.get("publishCpu19115").size()==2,"CPU lifecycle must have one begin/stop and normal/exception publication");
  req(wholeCalls==3&&ownedCalls==1,"CPU observation added or removed existing whole pixel work");
  int begin=sites.get("beginCpu19115").get(0),stop=sites.get("stopCpu19115").get(0),normal=sites.get("publishCpu19115").get(0),exceptional=sites.get("publishCpu19115").get(1),work=-1;
  req(begin<stop&&stop<normal&&normal<exceptional,"CPU observation lifecycle order differs");
  for(int i=begin+1;i<stop;i++){
   MethodReference target=ref(code.get(i));if(target==null)continue;
   req(target.getDefiningClass().equals(P)&&target.getName().equals("strongFinish1978")&&work<0,"CPU observation contains additional processing calls");work=i;
  }
  req(work>begin&&code.get(work+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"Original CPU oracle output absent");
  int[] arguments=callArguments(code.get(work));req(arguments.length==12&&Integer.valueOf(0).equals(literal(code,work,arguments[9]))&&Integer.valueOf(1).equals(literal(code,work,arguments[10])),"Observed call is not the original force-CPU oracle");
  req(!reaches(code,0,work,begin)&&!reaches(code,begin,stop,work),"CPU oracle or stop bypasses its observation boundary");
  req(!reaches(code,0,normal,begin)&&!reaches(code,begin,normal,work)&&!reaches(code,begin,normal,stop),"CPU publication bypasses original oracle completion");
  req(!reaches(code,work+1,work,normal),"CPU timing introduces repeated pixel work before publication");
  for(int i=work+1;i<normal;i++){MethodReference target=ref(code.get(i));req(target==null||target.getDefiningClass().equals(G)&&target.getName().equals("residentTiming19111"),"CPU completion performs additional processing");}
  Map<Integer,Integer> indexes=addressIndex(code);int[] addresses=new int[code.size()];int address=0;for(int i=0;i<code.size();i++){addresses[i]=address;address+=code.get(i).getCodeUnits();}
  Set<Integer> caught=new HashSet<>();int workHandler=-1;
  for(int invocation:new int[]{work,stop}){
   Set<Integer> handlers=new HashSet<>();for(TryBlock<? extends ExceptionHandler> block:run.getImplementation().getTryBlocks())if(addresses[invocation]>=block.getStartCodeAddress()&&addresses[invocation]<block.getStartCodeAddress()+block.getCodeUnitCount())for(ExceptionHandler handler:block.getExceptionHandlers())if(handler.getExceptionType()==null){Integer entry=indexes.get(handler.getHandlerCodeAddress());req(entry!=null,"CPU finally target absent");handlers.add(entry);}
   req(handlers.size()==1,"CPU oracle/stop must retain one finally owner");
   for(int entry:handlers){req(reaches(code,entry,exceptional,-1),"CPU failure skips diagnostic publication");caught.add(entry);if(invocation==work)workHandler=entry;}
  }
  req(caught.size()==2,"CPU work and completion finally entries differ");
  Set<Integer> forbidden=new HashSet<>();for(int i=0;i<code.size();i++){MethodReference target=ref(code.get(i));if(target!=null&&(target.getDefiningClass().equals(P)||target.getDefiningClass().equals(QUALIFICATION)&&target.getName().equals("qualified")))forbidden.add(i);}
  req(!exceptionReaches(run,exceptional,forbidden),"CPU exception publication reenters processing or admission");
  // Every lifecycle invocation, including the exception finally, remains in
  // the same private Probe. No new foreground observer entry is allowed.
  for(Method m:MergePayloads.methods(classes.values()).values())if(m.getImplementation()!=null)for(Instruction in:body(m)){MethodReference target=ref(in);if(target!=null&&target.getDefiningClass().equals(G)&&target.getName().equals("residentTiming19111"))req(MergePayloads.id(m).equals(MergePayloads.id(run)),"CPU lifecycle outside original private Probe");}
  sites.put("$work",List.of(work));sites.put("$workHandler",List.of(workHandler));
  return sites;
 }
 static int timingWrapperGuard(Map<String,ClassDef> classes,String owner){
  Method wrapper=method(classes,owner,"residentTiming19111");List<Instruction> code=body(wrapper);
  int branch=booleanGuard(wrapper,QUALIFICATION,"background","Ljava/lang/Class;","forName");
  Map<Integer,Integer> indexes=addressIndex(code);int address=0;
  for(Instruction instruction:code){MethodReference ref=ref(instruction);if(ref!=null){
   req(ref.getDefiningClass().equals(QUALIFICATION)&&ref.getName().equals("background")||ref.getDefiningClass().startsWith("Ljava/lang/"),"Timing wrapper accesses processing or admission state");
   boolean caught=false;for(TryBlock<? extends ExceptionHandler> block:wrapper.getImplementation().getTryBlocks())if(address>=block.getStartCodeAddress()&&address<block.getStartCodeAddress()+block.getCodeUnitCount())for(ExceptionHandler handler:block.getExceptionHandlers())if(handler.getExceptionType()==null||"Ljava/lang/Throwable;".equals(handler.getExceptionType())){
    caught=true;Integer entry=indexes.get(handler.getHandlerCodeAddress());req(entry!=null,"Invalid optional timing handler");
    for(int j=entry;j<code.size();j++)req(code.get(j).getOpcode()!=Opcode.THROW,"Optional timing failure may escape");
   }req(caught,"Timing invocation is outside its optional Throwable handler");
  }address+=instruction.getCodeUnits();}
  return branch;
 }
 static void timingGuard(Map<String,ClassDef> classes){
  timingScope(classes);cpuTimingScope(classes);for(String owner:TIMING_WRAPPERS.keySet())timingWrapperGuard(classes,owner);
  for(var entry:TIMING_WRAPPERS.entrySet())req(MergePayloads.hash(method(classes,entry.getKey(),"residentTiming19111")).equals(entry.getValue()),"Reviewed optional timing wrapper changed");
  for(var entry:TIMING_CLASSES.entrySet()){ClassDef type=classes.get(entry.getKey());req(type!=null,"Bounded timing observer absent");try{req(MergePayloads.classHash(ImmutableClassDef.of(type)).equals(entry.getValue()),"Bounded scalar timing observer changed");}catch(Exception failure){throw new IllegalStateException(failure);}}
  Map<String,Method> methods=MergePayloads.methods(classes.values());for(var entry:SAMPLE_METHODS.entrySet())req(methods.containsKey(entry.getKey())&&MergePayloads.hash(methods.get(entry.getKey())).equals(entry.getValue()),"Published GPU Sample executable method changed");
 }
 /** The final speed decision is reachable only after an exact-photo compare,
  * current route checks and a completed, comparable Strong plan. The two
  * boolean branches reject their failed arms before any qualification or
  * measured-speed rejection. The comparator bodies are separately pinned. */
 static int[] fairPlanGuard(Map<String,ClassDef> classes){
  List<Instruction> code=body(method(classes,PROBE,"run"));
  int exact=call(code,G,"acceptComparison19103");
  int plan=call(code,QUALIFICATION,"comparableResidentPlans19116");
  MethodReference planRef=ref(code.get(plan));
  req(planRef.getParameterTypes().isEmpty()&&"Z".equals(planRef.getReturnType()),"Wrong Strong-plan comparator ABI");
  int currentRoute=-1,routeValue=-1;
  for(int i=exact+1;i<plan;i++){
   MethodReference target=ref(code.get(i));
   if(target!=null&&target.getDefiningClass().equals(G)&&target.getName().equals("access$600"))currentRoute=i;
   if(target!=null&&target.getDefiningClass().equals(CHAIN)&&target.getName().equals("residentBaseline1978"))routeValue=i;
  }
  int qualified=call(code,QUALIFICATION,"qualified"),speedReject=call(code,QUALIFICATION,"rejectMeasuredThreshold1992");
  req(exact>=0&&currentRoute>exact&&routeValue>currentRoute&&plan>routeValue&&
      qualified>plan&&speedReject>qualified,"Exact, route, plan and speed order changed");
  // A bounded two-trial loop has a syntactic exit edge at its entry; the
  // whole-Probe pin fixes its initial value and body. Starting from the
  // compare site proves its successful path enters the final guard, while
  // both final route checks must dominate that guard independently.
  req(reaches(code,exact,plan,-1)&&!reaches(code,0,plan,currentRoute)&&
      !reaches(code,0,plan,routeValue),"Exact or current route can be bypassed");
  int firstBranch=-1,secondBranch=-1;
  for(int i=currentRoute+1;i<routeValue;i++)if(code.get(i).getOpcode()==Opcode.IF_EQZ||code.get(i).getOpcode()==Opcode.IF_NEZ){firstBranch=i;break;}
  for(int i=routeValue+1;i<plan;i++)if(code.get(i).getOpcode()==Opcode.IF_EQZ||code.get(i).getOpcode()==Opcode.IF_NEZ){secondBranch=i;break;}
  req(firstBranch>=0&&secondBranch>=0&&code.get(firstBranch).getOpcode()==Opcode.IF_EQZ&&
      code.get(secondBranch).getOpcode()==Opcode.IF_NEZ,"Route revalidation branches changed");
  guarded(code,firstBranch,plan,true,"Current Strong route");
  guarded(code,secondBranch,plan,false,"Current finish route");
  req(code.get(plan+1).getOpcode()==Opcode.MOVE_RESULT&&plan+2<code.size(),"Strong-plan result absent");
  int branch=plan+2;
  req(code.get(branch).getOpcode()==Opcode.IF_NEZ&&code.get(branch) instanceof OneRegisterInstruction r&&
      r.getRegisterA()==((OneRegisterInstruction)code.get(plan+1)).getRegisterA(),"Strong-plan rejection guard absent");
  guarded(code,branch,qualified,false,"Comparable Strong plan");
  guarded(code,branch,speedReject,false,"Comparable Strong plan before measured-speed rejection");
  int retry=-1;
  for(int i=branch+1;i<qualified;i++){MethodReference ref=ref(code.get(i));
   if(ref!=null&&ref.getDefiningClass().equals(QUALIFICATION)&&ref.getName().equals("rejectNonSpeed1988")){retry=i;break;}}
  req(retry>branch&&reaches(code,branch+1,retry,-1)&&
      !reaches(code,destination(code,branch),retry,-1),"Incomparable plan must use bounded non-speed retry");
  int win=call(code,G,"access$1300");
  req(win>plan&&win<qualified&&code.get(win+1).getOpcode()==Opcode.MOVE_RESULT&&
      code.get(win+2).getOpcode()==Opcode.IF_EQZ&&code.get(win+2) instanceof OneRegisterInstruction r&&
      r.getRegisterA()==((OneRegisterInstruction)code.get(win+1)).getRegisterA(),"Measured-speed decision missing");
  int speedBranch=win+2;
  guarded(code,speedBranch,qualified,true,"Measured speed qualified arm");
  guarded(code,speedBranch,speedReject,false,"Measured speed rejection arm");
  return new int[]{firstBranch,secondBranch,branch,speedBranch};
 }
 static void verify(Map<String,ClassDef> classes){Map<String,Method> methods=MergePayloads.methods(classes.values());for(var e:STRICT.entrySet())req(methods.containsKey(e.getKey())&&MergePayloads.hash(methods.get(e.getKey())).equals(e.getValue()),"Strict photo admission/reject-before-replay flow changed: "+e.getKey());
  for(var e:FAIR_COMPARATORS.entrySet())req(methods.containsKey(e.getKey())&&MergePayloads.hash(methods.get(e.getKey())).equals(e.getValue()),"Strong-plan comparison changed: "+e.getKey());
  fairPlanGuard(classes);
  residentMemoryGuard(classes);
  timingGuard(classes);
  Method pipeline=method(classes,P,"smoothNoiseInPlace1976"),helper=method(classes,P,"fixedStrong19105"),run=method(classes,S,"run"),target=method(classes,R,"fixedTarget19105");
  booleanGuard(pipeline,G,"cpuOracle",P,"fixedStrong19105");req((helper.getAccessFlags()&0x10f)==0xa,"Diagnostic helper must be private static");
  int callers=0;for(Method m:methods.values())if(m.getImplementation()!=null)for(Instruction i:body(m)){MethodReference r=ref(i);if(r!=null&&r.getDefiningClass().equals(P)&&r.getName().equals("fixedStrong19105")){req(MergePayloads.id(m).equals(MergePayloads.id(pipeline)),"Unexpected diagnostic caller");callers++;}}
  req(callers==1,"Diagnostic hook must have one guarded caller");boolean module=false,runName=false,invoke=false;for(Instruction i:body(helper)){if(i instanceof ReferenceInstruction rr&&rr.getReference() instanceof StringReference s){module|=s.getString().equals("com.hiro.ulike.StrongInputs19105");runName|=s.getString().equals("run");}MethodReference r=ref(i);invoke|=r!=null&&r.getDefiningClass().equals("Ljava/lang/reflect/Method;")&&r.getName().equals("invoke");}req(module&&runName&&invoke,"Emitted diagnostic module binding missing");
  nullGuard(run,R,"fixedTarget19105","Landroid/graphics/Bitmap;","getWidth");List<Instruction> t=body(target);int clone=call(t,"[I","clone");fieldGuard(target,"role",2,null,clone);fieldGuard(target,"first",5,null,clone);fieldGuard(target,"row",null,Opcode.IF_LTZ,clone);fieldGuard(target,"actualRow",null,Opcode.IF_EQZ,clone);
  for(Method m:classes.get(S).getMethods())if(m.getImplementation()!=null)for(Instruction i:body(m)){MethodReference r=ref(i);if(r!=null){req(!r.getDefiningClass().contains("GpuQualification"),"Fixed diagnostic writes/consults qualification");if(r.getDefiningClass().equals("Lcom/hiro/ulike/GpuStrong1960;"))req(r.getName().equals("ephemeral1973"),"Fixed diagnostic enters foreground GPU selection");}}
  Method cpu=method(classes,"Lcom/hiro/ulike/StrongNoise1958;","diagnosticCpu19105");for(Instruction i:body(cpu)){MethodReference r=ref(i);req(r==null||!r.getDefiningClass().contains("GpuStrong")&&!r.getDefiningClass().contains("GpuQualification"),"CPU differential recurses through GPU/admission");}
 }
 static Map<String,ClassDef> replace(Map<String,ClassDef> cs,Method old,List<Instruction> code){Map<String,ClassDef> copy=new TreeMap<>(cs);ClassDef c=copy.get(old.getDefiningClass());List<Method> ms=new ArrayList<>();for(Method m:c.getMethods())ms.add(MergePayloads.id(m).equals(MergePayloads.id(old))?new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),new ImmutableMethodImplementation(m.getImplementation().getRegisterCount(),code,m.getImplementation().getTryBlocks(),List.of())):m);copy.put(c.getType(),new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms));return copy;}
 static void reject(Map<String,ClassDef> cs){boolean rejected=false;try{verify(cs);}catch(IllegalStateException|IllegalArgumentException expected){rejected=true;}req(rejected,"Actual DEX negative control unexpectedly accepted");}
 static void rejectFairPlanStructure(Map<String,ClassDef> cs){boolean rejected=false;try{fairPlanGuard(cs);}catch(IllegalStateException|IllegalArgumentException expected){rejected=true;}req(rejected,"Strong-plan structural negative control unexpectedly accepted");}
 static int fairPlanControls(Map<String,ClassDef> cs){int n=0;Method run=method(cs,PROBE,"run");int[] branches=fairPlanGuard(cs);
  List<Instruction> changed=body(run);changed.set(call(changed,G,"acceptComparison19103"),new ImmutableInstruction10x(Opcode.NOP));
  rejectFairPlanStructure(replace(cs,run,changed));n++;
  for(int branch:branches){changed=body(run);Instruction prior=changed.get(branch);
   changed.set(branch,new ImmutableInstruction21t(prior.getOpcode()==Opcode.IF_EQZ?Opcode.IF_NEZ:Opcode.IF_EQZ,
       ((OneRegisterInstruction)prior).getRegisterA(),((OffsetInstruction)prior).getCodeOffset()));
   rejectFairPlanStructure(replace(cs,run,changed));n++;}
  changed=body(run);int branch=branches[2];Instruction prior=changed.get(branch);
  changed.set(branch,new ImmutableInstruction20t(Opcode.GOTO_16,((OffsetInstruction)prior).getCodeOffset()));
  rejectFairPlanStructure(replace(cs,run,changed));n++;
  req(n==6,"Exact, route, plan and speed structural controls required");return n;
 }
 static void flip(Map<String,ClassDef> cs,Method m,int branch){List<Instruction>a=body(m);Instruction b=a.get(branch);Opcode op=b.getOpcode()==Opcode.IF_EQZ?Opcode.IF_NEZ:Opcode.IF_EQZ;a.set(branch,new ImmutableInstruction21t(op,((OneRegisterInstruction)b).getRegisterA(),((OffsetInstruction)b).getCodeOffset()));reject(replace(cs,m,a));}
 static int controls(Map<String,ClassDef> cs){int n=0;Map<String,ClassDef> missing=new TreeMap<>(cs);missing.remove(S);reject(missing);n++;Method p=method(cs,P,"smoothNoiseInPlace1976");flip(cs,p,booleanGuard(p,G,"cpuOracle",P,"fixedStrong19105"));n++;Method r=method(cs,S,"run");flip(cs,r,nullGuard(r,R,"fixedTarget19105","Landroid/graphics/Bitmap;","getWidth"));n++;Method strict=MergePayloads.methods(cs.values()).get(G+"->acceptComparison19103(Ljava/lang/String;ZI)Z");List<Instruction>a=body(strict);a.add(new ImmutableInstruction10x(Opcode.NOP));reject(replace(cs,strict,a));n++;
  Method probe=method(cs,"Lcom/hiro/ulike/GpuResident1976$1;","run");List<Instruction> changed=body(probe);changed.add(new ImmutableInstruction10x(Opcode.NOP));reject(replace(cs,probe,changed));n++;
  int memoryBranch=residentMemoryGuard(cs);changed=body(probe);Instruction prior=changed.get(memoryBranch);changed.set(memoryBranch,new ImmutableInstruction21t(Opcode.IF_NEZ,((OneRegisterInstruction)prior).getRegisterA(),((OffsetInstruction)prior).getCodeOffset()));rejectResidentStructure(replace(cs,probe,changed));n++;
  // These four controls run the independent CFG/data-flow proof directly,
  // without the exact-hash gate masking a missing structural rejection.
  for(String guard:new String[]{"cancelled","isInterrupted"}){changed=body(probe);int b=-1;for(int i=memoryBranch+1;i<changed.size();i++){MethodReference ref=ref(changed.get(i));if(ref!=null&&ref.getName().equals(guard)){b=i+2;break;}}req(b>=0,"Negative-control cancellation branch absent");prior=changed.get(b);changed.set(b,new ImmutableInstruction21t(Opcode.IF_EQZ,((OneRegisterInstruction)prior).getRegisterA(),((OffsetInstruction)prior).getCodeOffset()));rejectResidentStructure(replace(cs,probe,changed));n++;}
  changed=body(probe);int reason=((OneRegisterInstruction)changed.get(memoryBranch-1)).getRegisterA();changed.set(destination(changed,memoryBranch),new ImmutableInstruction11x(Opcode.THROW,reason));rejectResidentStructure(replace(cs,probe,changed));n++;
  changed=body(probe);for(int i=memoryBranch+1;i<changed.size();i++){MethodReference ref=ref(changed.get(i));if(ref!=null&&ref.getDefiningClass().equals("Lcom/hiro/ulike/GpuQualification1961;")&&ref.getName().equals("rejectNonSpeed1988")){int[] args=callArguments(changed.get(i));changed.set(i,new ImmutableInstruction35c(Opcode.INVOKE_STATIC,2,args[0],args[0],0,0,0,ref));break;}}rejectResidentStructure(replace(cs,probe,changed));n++;
  for(String id:new String[]{G+"->admissionRefusal19110(Ljava/lang/RuntimeException;)Ljava/lang/String;",G+"->access$1400(Ljava/lang/RuntimeException;)Ljava/lang/String;","Lcom/hiro/ulike/GpuStrong1960;->residentAdmissionReason19110(Ljava/lang/RuntimeException;)Ljava/lang/String;","Lcom/hiro/ulike/GpuStrong1960;->residentReason19110(I)Ljava/lang/String;"}){Method pinned=MergePayloads.methods(cs.values()).get(id);changed=body(pinned);changed.add(new ImmutableInstruction10x(Opcode.NOP));reject(replace(cs,pinned,changed));n++;}req(n==14,"Published .110 negative controls with the active classifier bridge must remain complete");return n+timingControls(cs);}
 static void rejectTimingStructure(Map<String,ClassDef> classes,String wrapper){boolean rejected=false;try{if(wrapper==null)timingScope(classes);else timingWrapperGuard(classes,wrapper);}catch(IllegalStateException|IllegalArgumentException expected){rejected=true;}req(rejected,"Actual DEX timing structural negative control accepted");}
 static int timingControls(Map<String,ClassDef> classes){int n=0;
  Map<String,ClassDef> missing=new TreeMap<>(classes);missing.remove(TIMING);reject(missing);n++;
  for(Method target:new Method[]{method(classes,TIMING,"dispatch"),method(classes,G,"residentTiming19111")}){List<Instruction> code=body(target);code.add(new ImmutableInstruction10x(Opcode.NOP));reject(replace(classes,target,code));n++;}
  for(String owner:TIMING_WRAPPERS.keySet()){
   Method wrapper=method(classes,owner,"residentTiming19111");int branch=timingWrapperGuard(classes,owner);List<Instruction> code=body(wrapper);Instruction prior=code.get(branch);code.set(branch,new ImmutableInstruction21t(prior.getOpcode()==Opcode.IF_EQZ?Opcode.IF_NEZ:Opcode.IF_EQZ,((OneRegisterInstruction)prior).getRegisterA(),((OffsetInstruction)prior).getCodeOffset()));rejectTimingStructure(replace(classes,wrapper,code),owner);n++;
  }
  Method run=method(classes,PROBE,"run");Map<String,List<Integer>> sites=timingScope(classes);
  for(String operation:new String[]{"begin","start","stop"}){List<Instruction> code=body(run);int at=timingStringSite(code,sites.get(operation).get(0));Instruction prior=code.get(at);req(prior.getOpcode()==Opcode.CONST_STRING,"Unexpected timing string encoding");code.set(at,new ImmutableInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)prior).getRegisterA(),new ImmutableStringReference("phase")));rejectTimingStructure(replace(classes,run,code),null);n++;}
  req(n==9,"Nine independent timing negative controls required");return n;
 }
 static void rejectResidentStructure(Map<String,ClassDef> cs){boolean rejected=false;try{residentMemoryGuard(cs);}catch(IllegalStateException|IllegalArgumentException expected){rejected=true;}req(rejected,"Actual DEX typed-refusal structural negative control accepted");}
 static void rejectCpuTimingStructure(Map<String,ClassDef> classes){boolean rejected=false;try{cpuTimingScope(classes);}catch(IllegalStateException|IllegalArgumentException expected){rejected=true;}req(rejected,"Actual DEX CPU timing structural negative control accepted");}
 static int codeAddress(List<Instruction> code,int index){int address=0;for(int i=0;i<index;i++)address+=code.get(i).getCodeUnits();return address;}
 static void cpuLabel(List<Instruction> code,int invoke,String label){int at=timingStringSite(code,invoke);Instruction prior=code.get(at);req(prior.getOpcode()==Opcode.CONST_STRING,"CPU label encoding changed");code.set(at,new ImmutableInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)prior).getRegisterA(),new ImmutableStringReference(label)));}
 static int cpuTimingControls(Map<String,ClassDef> classes){
  // Every control calls only the independent CPU structure/CFG guard. Neither
  // the Probe whole-method pin nor either observer whole-class pin can hide a
  // missing structural rejection here. Mutations retain instruction widths.
  int n=0;Method run=method(classes,PROBE,"run");Map<String,List<Integer>> sites=cpuTimingScope(classes);
  int begin=sites.get("beginCpu19115").get(0),stop=sites.get("stopCpu19115").get(0),normal=sites.get("publishCpu19115").get(0),exceptional=sites.get("publishCpu19115").get(1),work=sites.get("$work").get(0),handler=sites.get("$workHandler").get(0);
  for(int invocation:new int[]{begin,stop,normal,exceptional}){List<Instruction> code=body(run);cpuLabel(code,invocation,"phase");rejectCpuTimingStructure(replace(classes,run,code));n++;}
  List<Instruction> code=body(run);cpuLabel(code,begin,"stopCpu19115");cpuLabel(code,stop,"beginCpu19115");rejectCpuTimingStructure(replace(classes,run,code));n++;
  code=body(run);int before=timingStringSite(code,begin)-1;while(before>=0&&code.get(before).getOpcode()!=Opcode.CONST_4)before--;req(before>=0,"CPU begin bypass mutation seam absent");int offset=codeAddress(code,work)-codeAddress(code,before);req(offset>=-128&&offset<=127,"CPU bypass branch extent");code.set(before,new ImmutableInstruction10t(Opcode.GOTO,offset));rejectCpuTimingStructure(replace(classes,run,code));n++;
  code=body(run);int branch=work+1;while(branch<stop&&!code.get(branch).getOpcode().name().startsWith("IF_"))branch++;req(branch<stop&&code.get(branch).getCodeUnits()==2,"CPU stop bypass branch absent");code.set(branch,new ImmutableInstruction20t(Opcode.GOTO_16,codeAddress(code,timingStringSite(code,normal))-codeAddress(code,branch)));rejectCpuTimingStructure(replace(classes,run,code));n++;
  code=body(run);int extra=-1;for(int i=0;i<begin;i++){MethodReference target=ref(code.get(i));if(target!=null&&target.getDefiningClass().equals(G)&&target.getName().equals("access$100")){extra=i;break;}}req(extra>=0&&code.get(extra).getCodeUnits()==code.get(work).getCodeUnits(),"Extra CPU work mutation seam absent");code.set(extra,code.get(work));rejectCpuTimingStructure(replace(classes,run,code));n++;
  code=body(run);int force=callArguments(code.get(work))[10],constant=work-1;while(constant>=0&&!(code.get(constant).getOpcode().setsRegister()&&code.get(constant) instanceof OneRegisterInstruction r&&r.getRegisterA()==force))constant--;req(constant>=0&&code.get(constant).getOpcode()==Opcode.CONST_16,"CPU selection mutation seam absent");code.set(constant,new ImmutableInstruction21s(Opcode.CONST_16,force,0));rejectCpuTimingStructure(replace(classes,run,code));n++;
  code=body(run);req(code.get(handler).getCodeUnits()==1,"CPU finally mutation width");offset=codeAddress(code,exceptional+1)-codeAddress(code,handler);req(offset>=-128&&offset<=127,"CPU finally bypass extent");code.set(handler,new ImmutableInstruction10t(Opcode.GOTO,offset));rejectCpuTimingStructure(replace(classes,run,code));n++;
  code=body(run);Instruction prior=code.get(branch);req(prior instanceof OneRegisterInstruction&&prior.getOpcode()==Opcode.IF_NEZ,"CPU repeat-work branch seam absent");code.set(branch,new ImmutableInstruction21t(Opcode.IF_NEZ,((OneRegisterInstruction)prior).getRegisterA(),codeAddress(code,work)-codeAddress(code,branch)));rejectCpuTimingStructure(replace(classes,run,code));n++;
  req(n==11,"Eleven independent CPU timing negative controls required");return n;
 }
 public static void main(String[] a)throws Exception{req(a.length==1,"DEX_OR_APK");Map<String,ClassDef> cs=MergePayloads.classes(a[0]);verify(cs);int controls=controls(cs),cpuControls=cpuTimingControls(cs),fairControls=fairPlanControls(cs);System.out.println("PASS emitted-strong-diagnostic foreground_cpu_guard replay_strong_guard target_null_guard strict_gate_unchanged qualification_writes_absent resident_timing_guard negative_controls="+controls+" cpu_timing_negative_controls="+cpuControls+" fair_plan_negative_controls="+fairControls);}
}
