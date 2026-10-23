import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;

/** Checks emitted DEX, not Java source. Trace eligibility is evaluated using a
 * deliberately small, read-only DEX interpreter. The photo admission contracts
 * and existing fixed-input validator are pinned to already published payloads. */
public final class VerifyStrongTrace19106 {
 static final String S="Lcom/hiro/ulike/StrongInputs19105;",T="Lcom/hiro/ulike/StrongTrace19106;",R="Lcom/hiro/ulike/ResidentStages19104;",G="Lcom/hiro/ulike/GpuStrong1960;";
 static final String VALID=R+"->validFixed19105([J)Z";
 static final String VALID_HASH="c551d5288aee01a7e4002860df278b91c64cc4632dae1bb15aadae5bcbe411a5";
 static final Map<String,String> SELECTORS=Map.of(
  R+"->checkFixed19105()V","b0d4cfb6d1c1719a930b5e55b363ae357f6d69940a48437aaf2551b73b6f5a81",
  R+"->fixedTarget19105()[Ljava/lang/Object;","a013d964d451ca00bcf5a429a9389797997fe7cd1cd2ee152e5b7c6d9610921c",
  "Lcom/hiro/ulike/GpuNoise1960;->strongExactPrepare1990(II)I","11ead443e34494d1f5694909e3279e6830a8d841b01c059f593cd76e69bb58b9",
  "Lcom/hiro/ulike/GpuNoise1960;->strongExactProgram1973(I)I","491e34c4fefcc1ba4308edd6d8688701691bcbc287ec8c49de51bfc34cfeae12",
  "Lcom/hiro/ulike/GpuNoise1960;->strongProgram(II)I","d40d7eb98c431a0563bb5b0a111dfd91e71e07f9445e36263807111c3313d9e7",
  "Lcom/hiro/ulike/GpuNoise1960;->variant(II)I","3a3e4a2491ff9ae19fc0af168d026e82d98fc7805e840ae1e6bf5312634d5e6e",
  "Lcom/hiro/ulike/GpuStrong1960;->program1971(ZI)I","52984fc9c141bc670d934619edc4cd69273616198f7fe82d1930d21e06859f14",
  "Lcom/hiro/ulike/GpuStrong1960;->program1973(II)I","1bcae512a3c24e3e6fd6de0b5ffa6009177d81d5ab1320e8b5ba1b9e36bf3cd9");
 static void req(boolean value,String message){MergePayloads.require(value,message);}
 static List<Instruction> body(Method m){return VerifyStrongDiagnostic19105.body(m);}
 static Method method(Map<String,ClassDef> cs,String owner,String name){return VerifyStrongDiagnostic19105.method(cs,owner,name);}
 static MethodReference ref(Instruction in){return VerifyStrongDiagnostic19105.ref(in);}
 static int call(List<Instruction> a,String owner,String name){return VerifyStrongDiagnostic19105.call(a,owner,name);}
 static int dest(List<Instruction> a,int i){return VerifyStrongDiagnostic19105.destination(a,i);}
 static int[] args(Instruction in){
  if(in instanceof FiveRegisterInstruction f){int[] a={f.getRegisterC(),f.getRegisterD(),f.getRegisterE(),f.getRegisterF(),f.getRegisterG()};return Arrays.copyOf(a,f.getRegisterCount());}
  req(in instanceof RegisterRangeInstruction,"Unexpected trace call register encoding");RegisterRangeInstruction r=(RegisterRangeInstruction)in;int[] a=new int[r.getRegisterCount()];for(int i=0;i<a.length;i++)a[i]=r.getStartRegister()+i;return a;
 }
 static long number(Object value){req(value instanceof Number,"Non-numeric guard register");return ((Number)value).longValue();}
 static boolean comparison(String op,long a,long b){return switch(op){case "EQ"->a==b;case "NE"->a!=b;case "LT"->a<b;case "LE"->a<=b;case "GT"->a>b;case "GE"->a>=b;default->throw new IllegalStateException("Unexpected conditional "+op);};}
 /** Invoke the immutable old validator as a supplied predicate result: both
  * true and false are tested, while its exact DEX hash is independently pinned. */
 static boolean execute(Method m,long[] value,boolean valid){
  req(m.getParameterTypes().equals(List.of("[J"))&&m.getReturnType().equals("Z")&&(m.getAccessFlags()&0x10f)==0xa,"Trace eligibility must be private static ([J)Z");
  req(m.getImplementation().getTryBlocks().isEmpty(),"Trace eligibility must not catch failures");
  List<Instruction> code=body(m);Object[] regs=new Object[m.getImplementation().getRegisterCount()];regs[regs.length-1]=value;Object result=null;int pc=0,steps=0,validated=0;
  while(pc>=0&&pc<code.size()){
   req(++steps<1024,"Trace eligibility contains an unexpected loop");Instruction in=code.get(pc);String op=in.getOpcode().name();int next=pc+1;
   if(op.startsWith("CONST_WIDE")){regs[((OneRegisterInstruction)in).getRegisterA()]=((WideLiteralInstruction)in).getWideLiteral();}
   else if(op.startsWith("CONST")&&!op.contains("STRING")&&!op.contains("CLASS")){req(in instanceof NarrowLiteralInstruction,"Unexpected constant");regs[((OneRegisterInstruction)in).getRegisterA()]=((NarrowLiteralInstruction)in).getNarrowLiteral();}
   else if(op.equals("MOVE_RESULT")){regs[((OneRegisterInstruction)in).getRegisterA()]=result;result=null;}
   else if(op.startsWith("MOVE")&&!op.contains("RESULT")&&!op.contains("EXCEPTION")){TwoRegisterInstruction x=(TwoRegisterInstruction)in;regs[x.getRegisterA()]=regs[x.getRegisterB()];}
   else if(op.equals("AGET_WIDE")){ThreeRegisterInstruction x=(ThreeRegisterInstruction)in;req(regs[x.getRegisterB()]==value&&value!=null,"Eligibility reads non-input array");int index=(int)number(regs[x.getRegisterC()]);req(index>=0&&index<value.length,"Eligibility reads invalid input index");regs[x.getRegisterA()]=value[index];}
   else if(op.equals("CMP_LONG")){ThreeRegisterInstruction x=(ThreeRegisterInstruction)in;regs[x.getRegisterA()]=Long.compare(number(regs[x.getRegisterB()]),number(regs[x.getRegisterC()]));}
   else if(op.equals("INVOKE_STATIC")||op.equals("INVOKE_STATIC_RANGE")){MethodReference mr=ref(in);req(mr!=null&&mr.toString().equals(VALID),"Eligibility calls a mutable/foreign helper");int[] a=args(in);req(a.length==1&&regs[a[0]]==value&&++validated==1,"Eligibility validator arguments differ");result=valid?1:0;}
   else if(op.startsWith("IF_")){String predicate=op.substring(3);boolean take;if(predicate.endsWith("Z")){predicate=predicate.substring(0,predicate.length()-1);Object a=regs[((OneRegisterInstruction)in).getRegisterA()];take=comparison(predicate,a==null?0:number(a),0);}else{TwoRegisterInstruction x=(TwoRegisterInstruction)in;take=comparison(predicate,number(regs[x.getRegisterA()]),number(regs[x.getRegisterB()]));}if(take)next=dest(code,pc);}
   else if(op.startsWith("GOTO")){next=dest(code,pc);}
   else if(op.equals("RETURN")){req(validated==1,"Eligibility returned without fixed-input validation");long answer=number(regs[((OneRegisterInstruction)in).getRegisterA()]);req(answer==0||answer==1,"Eligibility returned non-boolean value");return answer==1;}
   else if(!op.equals("NOP"))throw new IllegalStateException("Unexpected eligibility opcode "+op);
   pc=next;
  }
  throw new IllegalStateException("Trace eligibility fell through");
 }
 static long[] good(){return new long[]{1,521,1,0,1,1,2617,0xffae9375L,0xffae9375L,0xffae9374L,1,1,3060,3060};}
 static boolean expected(long[] v,boolean valid){return valid&&v[0]==1&&v[2]==1&&v[3]==0&&v[4]>0&&v[5]>0&&v[6]>0&&v[6]<=v[12]&&v[10]==1&&v[11]==1&&v[12]==v[13];}
 static int eligibility(Method m){int checked=0;
  req(!execute(m,null,false),"Null eligibility accepted");checked++;
  for(boolean valid:new boolean[]{false,true}){long[] original=good();req(execute(m,original,valid)==expected(original,valid),"Basic trace eligibility differs");checked++;
   int[] fields={0,2,3,4,5,6,10,11,12,13};long[] values={-1,0,1,2,3,4,5,2617,3060,3061,Long.MAX_VALUE};
   for(int field:fields)for(long v:values){long[] a=good();a[field]=v;req(execute(m,a,valid)==expected(a,valid),"Eligibility field "+field+" value "+v+" differs");checked++;}
  }
  // Simultaneous faults ensure no OR-combination can authorize a rejected row.
  for(int mask=0;mask<256;mask++){long[] a=good();if((mask&1)!=0)a[0]=4;if((mask&2)!=0)a[2]=3;if((mask&4)!=0)a[3]=1;if((mask&8)!=0)a[4]=0;if((mask&16)!=0)a[5]=0;if((mask&32)!=0)a[10]=2;if((mask&64)!=0)a[11]=2;if((mask&128)!=0)a[12]--;req(execute(m,a,true)==expected(a,true),"Combined trace eligibility differs "+mask);checked++;}
  return checked;
 }
 static void onlyCaller(Map<String,Method> methods,String owner,String name,String caller){int found=0;for(Method m:methods.values())if(m.getImplementation()!=null)for(Instruction i:body(m)){MethodReference r=ref(i);if(r!=null&&r.getDefiningClass().equals(owner)&&r.getName().equals(name)){req(MergePayloads.id(m).equals(caller),"Unexpected trace caller "+MergePayloads.id(m)+" -> "+name);found++;}}req(found==1,"Trace entry must have exactly one caller: "+name);}
 static int verify(Map<String,ClassDef> cs){
  VerifyStrongDiagnostic19105.verify(cs);Map<String,Method> methods=MergePayloads.methods(cs.values());req(methods.containsKey(VALID)&&MergePayloads.hash(methods.get(VALID)).equals(VALID_HASH),"Published fixed-input validity contract changed");
  for(var e:SELECTORS.entrySet())req(methods.containsKey(e.getKey())&&MergePayloads.hash(methods.get(e.getKey())).equals(e.getValue()),"Published production GPU program selector changed: "+e.getKey());
  Method input=method(cs,S,"run"),guard=method(cs,S,"traceEligible19106"),trace=method(cs,T,"run");
  VerifyStrongDiagnostic19105.booleanGuard(input,S,"traceEligible19106",T,"run");onlyCaller(methods,T,"run",MergePayloads.id(input));onlyCaller(methods,S,"traceEligible19106",MergePayloads.id(input));
  req(trace.getParameterTypes().equals(List.of("[I","[I","Lcom/hiro/ulike/StrongNoise1958$Model;","[I","[I","[I","[I","[J"))&&trace.getReturnType().equals("V"),"Trace entry signature differs");
  req((trace.getAccessFlags()&0x10f)==9,"Trace entry must remain public static, non-native");
  for(ClassDef c:cs.values())if(c.getType().startsWith("Lcom/hiro/ulike/StrongTrace19106"))for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:body(m)){
   MethodReference r=ref(i);if(r!=null){String owner=r.getDefiningClass();req(!owner.contains("GpuQualification")&&(!owner.equals("Lcom/hiro/ulike/GpuResident1976;")||r.getName().equals("cpuOracle")),"Trace writes/consults photo admission or resident selection");req(!owner.startsWith("Ljava/lang/reflect/")&&!owner.equals("Ljava/lang/Class;"),"Trace may not dynamically enter foreground work");if(owner.equals(G))req(r.getName().equals("trace19106"),"Trace enters foreground GPU selection");}
  }
  onlyCaller(methods,G,"trace19106",MergePayloads.id(trace));
  onlyCaller(methods,"Lcom/hiro/ulike/StrongNoise1958;","diagnosticTrace19106",MergePayloads.id(trace));
  Method internal=method(cs,T,"eligible19106");onlyCaller(methods,T,"eligible19106",MergePayloads.id(trace));
  int entryBranch=VerifyStrongDiagnostic19105.booleanGuard(trace,T,"eligible19106",G,"trace19106");
  List<Instruction> entry=body(trace);int allocation=-1;for(int i=0;i<entry.size();i++)if(entry.get(i).getOpcode()==Opcode.NEW_ARRAY){allocation=i;break;}
  req(allocation>=0,"Trace summary allocation missing");VerifyStrongDiagnostic19105.guarded(entry,entryBranch,allocation,entry.get(entryBranch).getOpcode()==Opcode.IF_EQZ,"Trace eligibility before allocation");
  VerifyStrongDiagnostic19105.booleanGuard(trace,T,"eligible19106","Lcom/hiro/ulike/StrongNoise1958;","diagnosticTrace19106");
  Method wrapper=method(cs,G,"trace19106");VerifyStrongDiagnostic19105.booleanGuard(wrapper,"Lcom/hiro/ulike/GpuResident1976;","cpuOracle","Lcom/hiro/ulike/GpuNoise1960;","open");
  List<Instruction> gpu=body(wrapper);int open=call(gpu,"Lcom/hiro/ulike/GpuNoise1960;","open"),check=-1;for(int i=0;i<open;i++){MethodReference r=ref(gpu.get(i));if(r!=null&&r.getDefiningClass().equals(R)&&r.getName().equals("checkFixed19105"))check=i;}
  req(check>=0&&!VerifyStrongDiagnostic19105.reaches(gpu,0,open,check),"GPU trace bypasses active replay/cancellation check");
  return eligibility(guard)+eligibility(internal);
 }
 static void reject(Map<String,ClassDef> cs){boolean rejected=false;try{verify(cs);}catch(IllegalStateException|IllegalArgumentException expected){rejected=true;}req(rejected,"Actual DEX trace negative control unexpectedly accepted");}
 static int controls(Map<String,ClassDef> cs){int n=0;Map<String,ClassDef> missing=new TreeMap<>(cs);missing.remove(T);reject(missing);n++;
  Method input=method(cs,S,"run");int branch=VerifyStrongDiagnostic19105.booleanGuard(input,S,"traceEligible19106",T,"run");List<Instruction> code=body(input);Instruction b=code.get(branch);code.set(branch,new ImmutableInstruction21t(b.getOpcode()==Opcode.IF_EQZ?Opcode.IF_NEZ:Opcode.IF_EQZ,((OneRegisterInstruction)b).getRegisterA(),((OffsetInstruction)b).getCodeOffset()));reject(VerifyStrongDiagnostic19105.replace(cs,input,code));n++;
  Method eligible=method(cs,S,"traceEligible19106");reject(VerifyStrongDiagnostic19105.replace(cs,eligible,List.of(new ImmutableInstruction11n(Opcode.CONST_4,0,1),new ImmutableInstruction11x(Opcode.RETURN,0))));n++;
  Method fixed=MergePayloads.methods(cs.values()).get(VALID);code=body(fixed);code.add(new ImmutableInstruction10x(Opcode.NOP));reject(VerifyStrongDiagnostic19105.replace(cs,fixed,code));n++;
  Method trace=method(cs,T,"run");branch=VerifyStrongDiagnostic19105.booleanGuard(trace,T,"eligible19106",G,"trace19106");code=body(trace);b=code.get(branch);code.set(branch,new ImmutableInstruction21t(b.getOpcode()==Opcode.IF_EQZ?Opcode.IF_NEZ:Opcode.IF_EQZ,((OneRegisterInstruction)b).getRegisterA(),((OffsetInstruction)b).getCodeOffset()));reject(VerifyStrongDiagnostic19105.replace(cs,trace,code));n++;
  Method internal=method(cs,T,"eligible19106");reject(VerifyStrongDiagnostic19105.replace(cs,internal,List.of(new ImmutableInstruction11n(Opcode.CONST_4,0,1),new ImmutableInstruction11x(Opcode.RETURN,0))));n++;
  Method selector=MergePayloads.methods(cs.values()).get(G+"->program1973(II)I");code=body(selector);code.add(new ImmutableInstruction10x(Opcode.NOP));reject(VerifyStrongDiagnostic19105.replace(cs,selector,code));n++;
  return n;
 }
 public static void main(String[] args)throws Exception{req(args.length==1,"DEX_OR_APK");Map<String,ClassDef> cs=MergePayloads.classes(args[0]);int cases=verify(cs),controls=controls(cs);System.out.println("PASS emitted-strong-trace post_save_guard fixed_reproducer_guard strict_gate_unchanged qualification_writes_absent eligibility_cases="+cases+" negative_controls="+controls);}
}
