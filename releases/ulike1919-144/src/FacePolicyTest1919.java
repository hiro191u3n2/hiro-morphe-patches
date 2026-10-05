import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
/** Execute the restored pure policy DEX with a small fail-closed host interpreter (not an Android runtime). */
public final class FacePolicyTest1919 {
 static Map<String,Method> methods;static int tests;
 static int intValue(Object x){return ((Number)x).intValue();}
 static boolean zero(Object x){return x==null||x instanceof Number&&intValue(x)==0;}
 static Object exec(String name,Object... args){
  Method m=methods.values().stream().filter(x->x.getDefiningClass().equals("Lcom/hiro/ulike/FacePrecisionPolicy;")&&x.getName().equals(name)).findFirst().orElseThrow();
  var impl=m.getImplementation();List<Instruction> ins=Transform1919.instructions(impl);Map<Integer,Integer> addresses=new HashMap<>();int[] addr=new int[ins.size()];int off=0;for(int i=0;i<ins.size();i++){addr[i]=off;addresses.put(off,i);off+=ins.get(i).getCodeUnits();}
  Object[] r=new Object[impl.getRegisterCount()];System.arraycopy(args,0,r,r.length-args.length,args.length);Object result=null;
  for(int pc=0,steps=0;steps++<300;){Instruction op=ins.get(pc);int next=pc+1;Opcode code=op.getOpcode();
   switch(code){
    case NOP:break;
    case CONST_4: case CONST_16:r[((OneRegisterInstruction)op).getRegisterA()]=((NarrowLiteralInstruction)op).getNarrowLiteral();break;
    case CONST_STRING:r[((OneRegisterInstruction)op).getRegisterA()]=((StringReference)((ReferenceInstruction)op).getReference()).getString();break;
    case INSTANCE_OF:{var v=(TwoRegisterInstruction)op;String t=((TypeReference)((ReferenceInstruction)op).getReference()).getType();if(!t.equals("Ljava/lang/Boolean;"))throw new AssertionError(t);r[v.getRegisterA()]=r[v.getRegisterB()] instanceof Boolean?1:0;break;}
    case NEW_INSTANCE:{String t=((TypeReference)((ReferenceInstruction)op).getReference()).getType();if(!t.equals("Ljava/lang/StringBuilder;"))throw new AssertionError(t);r[((OneRegisterInstruction)op).getRegisterA()]=new StringBuilder();break;}
    case SGET_OBJECT:{var f=(FieldReference)((ReferenceInstruction)op).getReference();if(!f.getDefiningClass().equals("Ljava/lang/Boolean;"))throw new AssertionError();r[((OneRegisterInstruction)op).getRegisterA()]=f.getName().equals("TRUE")?Boolean.TRUE:Boolean.FALSE;break;}
    case ADD_INT_2ADDR:{var v=(TwoRegisterInstruction)op;r[v.getRegisterA()]=intValue(r[v.getRegisterA()])+intValue(r[v.getRegisterB()]);break;}
    case INVOKE_DIRECT:break; // Only StringBuilder.<init>; object allocated above.
    case INVOKE_STATIC:case INVOKE_VIRTUAL:{var f=(MethodReference)((ReferenceInstruction)op).getReference();var regs=(FiveRegisterInstruction)op;Object a=r[regs.getRegisterC()],b=r[regs.getRegisterD()];
      if(f.getDefiningClass().equals("Lcom/hiro/ulike/FacePrecisionPolicy;"))result=exec(f.getName(),a,b);
      else if(f.getDefiningClass().equals("Ljava/lang/String;"))result=switch(f.getName()){case "equals"->a.equals(b)?1:0;case "startsWith"->((String)a).startsWith((String)b)?1:0;case "endsWith"->((String)a).endsWith((String)b)?1:0;case "length"->((String)a).length();default->throw new AssertionError(f);};
      else if(f.getDefiningClass().equals("Ljava/lang/StringBuilder;"))result=switch(f.getName()){case "append"->((StringBuilder)a).append((String)b);case "toString"->a.toString();default->throw new AssertionError(f);};
      else throw new AssertionError(f);break;}
    case MOVE_RESULT:case MOVE_RESULT_OBJECT:r[((OneRegisterInstruction)op).getRegisterA()]=result;break;
    case IF_EQZ:case IF_NEZ:{boolean test=zero(r[((OneRegisterInstruction)op).getRegisterA()]);if(code==Opcode.IF_NEZ)test=!test;if(test)next=addresses.get(addr[pc]+((OffsetInstruction)op).getCodeOffset());break;}
    case IF_LE:{var v=(TwoRegisterInstruction)op;if(intValue(r[v.getRegisterA()])<=intValue(r[v.getRegisterB()]))next=addresses.get(addr[pc]+((OffsetInstruction)op).getCodeOffset());break;}
    case GOTO:case GOTO_16:next=addresses.get(addr[pc]+((OffsetInstruction)op).getCodeOffset());break;
    case RETURN:case RETURN_OBJECT:return r[((OneRegisterInstruction)op).getRegisterA()];
    default:throw new AssertionError("Unsupported policy opcode "+code);
   }
   pc=next;
  }
  throw new AssertionError("Policy execution limit");
 }
 static void check(String key,Object input,int type,boolean enabled,Object expected){Object got=exec("override",key,input,type,enabled?1:0);if(got!=expected)throw new AssertionError("Policy case "+key+" expected "+expected+" got "+got);tests++;}
 public static void main(String[] a)throws Exception{
  methods=MergePayloads.methods(MergePayloads.classes(a[0]).values());String large="enable_face106_large_resolution",small="enable_face240_small_resolution";
  check(large,Boolean.FALSE,0,true,Boolean.TRUE);check(large,Boolean.TRUE,0,true,Boolean.TRUE);
  check(small,Boolean.TRUE,0,true,Boolean.FALSE);check(small,Boolean.FALSE,0,true,Boolean.FALSE);
  check(large,Boolean.FALSE,0,false,Boolean.FALSE);check(small,Boolean.TRUE,0,false,Boolean.TRUE);
  check(large,Boolean.FALSE,1,true,Boolean.FALSE);check(small,Boolean.TRUE,1,true,Boolean.TRUE);
  check("other",Boolean.FALSE,0,true,Boolean.FALSE);check(null,Boolean.TRUE,0,true,Boolean.TRUE);
  check(large,null,0,true,null);Object notBool=new Object();check(large,notBool,0,true,notBool);
  check("effect_config_camera_"+large,Boolean.FALSE,0,true,Boolean.TRUE);check("effect_config_camera_"+small,Boolean.TRUE,0,true,Boolean.FALSE);
  check("effect_config_"+large,Boolean.FALSE,0,true,Boolean.FALSE);check("effect_config_"+small,Boolean.TRUE,0,true,Boolean.TRUE);
  check("effect_config__"+large,Boolean.FALSE,0,true,Boolean.FALSE);check("effect_config__"+small,Boolean.TRUE,0,true,Boolean.TRUE);
  check("x_effect_config_camera_"+large,Boolean.FALSE,0,true,Boolean.FALSE);check(large+"_other",Boolean.FALSE,0,true,Boolean.FALSE);
  System.out.println("PASS "+tests+" restored-policy DEX host cases; no Android/SDK inference execution claimed.");
 }
}
