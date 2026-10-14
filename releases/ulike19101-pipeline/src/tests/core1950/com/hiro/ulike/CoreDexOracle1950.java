package com.hiro.ulike;
import java.io.*;import java.lang.reflect.Array;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.instruction.formats.*;import com.android.tools.smali.dexlib2.iface.reference.*;

/** Executes the retained production DEX instructions directly. This is not a
 * hand-written reimplementation of the bilateral algorithm. Only the VM's
 * instruction semantics and standard Java calls are supplied by the host. */
public final class CoreDexOracle1950 {
 static final String OWNER="Lcom/hiro/ulike/DetailSerial186;";
 static final Map<String,Method> methods=new HashMap<String,Method>();
 static final Map<String,Object> fields=new HashMap<String,Object>();
 static final Map<String,Code> compiled=new HashMap<String,Code>();
 static final class Code{final Instruction[]ops;final Map<Integer,Integer>address=new HashMap<Integer,Integer>();final int[]pos;final int registers;Code(Method m){List<Instruction>ls=new ArrayList<Instruction>();for(Instruction i:m.getImplementation().getInstructions())ls.add(i);ops=ls.toArray(new Instruction[0]);pos=new int[ops.length];int p=0;for(int i=0;i<ops.length;i++){pos[i]=p;address.put(p,i);p+=ops[i].getCodeUnits();}registers=m.getImplementation().getRegisterCount();}}
 public static void init(String path)throws Exception{methods.clear();fields.clear();compiled.clear();DexFile d=DexFileFactory.loadDexFile(new File(path),Opcodes.forApi(35));for(ClassDef c:d.getClasses())if(c.getType().equals(OWNER))for(Method m:c.getMethods()){methods.put(m.toString(),m);if(m.getImplementation()!=null)compiled.put(m.toString(),new Code(m));}if(methods.size()<20)throw new AssertionError("actual production class required");call(OWNER+"-><clinit>()V");}
 static int iv(Object x){return x==null?0:x instanceof Boolean?((Boolean)x?1:0):((Number)x).intValue();}
 static double dv(Object x){return x instanceof Double?((Double)x):Double.longBitsToDouble(((Number)x).longValue());}
 static String ref(Instruction i){return ((ReferenceInstruction)i).getReference().toString();}
 static Object call(String id,Object...args){
  if(!id.startsWith(OWNER)){
   if(id.equals("Ljava/lang/StrictMath;->exp(D)D"))return StrictMath.exp(dv(args[0]));
   if(id.equals("Ljava/lang/Math;->round(D)J"))return Math.round(dv(args[0]));
   if(id.equals("Ljava/lang/Math;->abs(I)I"))return Math.abs(iv(args[0]));
   if(id.equals("Ljava/lang/Math;->min(II)I"))return Math.min(iv(args[0]),iv(args[1]));
   if(id.equals("Ljava/lang/Math;->max(II)I"))return Math.max(iv(args[0]),iv(args[1]));
   if(id.equals("Ljava/lang/reflect/Array;->newInstance(Ljava/lang/Class;[I)Ljava/lang/Object;"))return Array.newInstance((Class<?>)args[0],(int[])args[1]);
   throw new AssertionError("Unimplemented standard call "+id);
  }
  Code c=compiled.get(id);if(c==null)throw new AssertionError("missing production method "+id);
  Object[]r=new Object[c.registers];int start=c.registers;for(Object a:args)start-=a instanceof Double||a instanceof Long?2:1;for(Object a:args){r[start]=a;start+=a instanceof Double||a instanceof Long?2:1;}
  Object result=null;int pc=0;
  while(true){Instruction op=c.ops[pc];Opcode o=op.getOpcode();String n=o.name();int a=op instanceof OneRegisterInstruction?((OneRegisterInstruction)op).getRegisterA():-1,b=op instanceof TwoRegisterInstruction?((TwoRegisterInstruction)op).getRegisterB():-1,z=op instanceof ThreeRegisterInstruction?((ThreeRegisterInstruction)op).getRegisterC():-1;int next=pc+1;
   if(n.startsWith("CONST_WIDE"))r[a]=((WideLiteralInstruction)op).getWideLiteral();
   else if(n.startsWith("CONST_" )||n.equals("CONST"))r[a]=(int)((WideLiteralInstruction)op).getWideLiteral();
   else if(n.startsWith("MOVE_RESULT"))r[a]=result;
   else if(n.startsWith("MOVE"))r[a]=r[b];
   else if(n.equals("NEW_ARRAY"))r[a]=ref(op).equals("[I")?new int[iv(r[b])]:new Object[iv(r[b])];
   else if(n.startsWith("FILLED_NEW_ARRAY")){int[]regs=registers(op);int[]v=new int[regs.length];for(int j=0;j<v.length;j++)v[j]=iv(r[regs[j]]);result=v;}
   else if(n.equals("FILL_ARRAY_DATA")){ArrayPayload pay=(ArrayPayload)c.ops[c.address.get(c.pos[pc]+((OffsetInstruction)op).getCodeOffset())];int[]v=(int[])r[a];List<Number>vs=pay.getArrayElements();for(int j=0;j<vs.size();j++)v[j]=vs.get(j).intValue();}
   else if(n.startsWith("SGET"))r[a]=ref(op).equals("Ljava/lang/Integer;->TYPE:Ljava/lang/Class;")?Integer.TYPE:fields.get(ref(op));
   else if(n.startsWith("SPUT"))fields.put(ref(op),r[a]);
   else if(n.startsWith("AGET"))r[a]=Array.get(r[b],iv(r[z]));
   else if(n.startsWith("APUT"))Array.set(r[b],iv(r[z]),r[a]);
   else if(n.equals("CHECK_CAST")){}
   else if(n.equals("INT_TO_DOUBLE"))r[a]=(double)iv(r[b]);
   else if(n.equals("LONG_TO_INT"))r[a]=((Number)r[b]).intValue();
   else if(n.equals("NEG_DOUBLE"))r[a]=-dv(r[b]);
   else if(n.equals("NEG_INT"))r[a]=-iv(r[b]);
   else if(n.equals("MUL_DOUBLE_2ADDR"))r[a]=dv(r[a])*dv(r[b]);
   else if(n.equals("DIV_DOUBLE_2ADDR"))r[a]=dv(r[a])/dv(r[b]);
   else if(n.startsWith("INVOKE")){int[]regs=registers(op);MethodReference mr=(MethodReference)((ReferenceInstruction)op).getReference();List<? extends CharSequence>params=mr.getParameterTypes();Object[]actual=new Object[params.size()];int j=0;for(int k=0;k<actual.length;k++){actual[k]=r[regs[j]];String type=params.get(k).toString();j+=type.equals("J")||type.equals("D")?2:1;}result=call(ref(op),actual);}
   else if(n.equals("RETURN_VOID"))return null;
   else if(n.startsWith("RETURN"))return r[a];
   else if(n.startsWith("GOTO"))next=c.address.get(c.pos[pc]+((OffsetInstruction)op).getCodeOffset());
   else if(n.startsWith("IF_")){int left=iv(r[a]),right=b<0?0:iv(r[b]);boolean hit=n.startsWith("IF_EQ")?left==right:n.startsWith("IF_NE")?left!=right:n.startsWith("IF_LE")?left<=right:n.startsWith("IF_LT")?left<right:n.startsWith("IF_GE")?left>=right:left>right;if(hit)next=c.address.get(c.pos[pc]+((OffsetInstruction)op).getCodeOffset());}
   else if(n.contains("_INT")){
    int left,right;if(n.contains("_2ADDR")){left=iv(r[a]);right=iv(r[b]);}else if(op instanceof NarrowLiteralInstruction){left=iv(r[b]);right=((NarrowLiteralInstruction)op).getNarrowLiteral();}else{left=iv(r[b]);right=iv(r[z]);}
    if(n.startsWith("ADD"))r[a]=left+right;else if(n.startsWith("SUB"))r[a]=left-right;else if(n.startsWith("RSUB"))r[a]=right-left;else if(n.startsWith("MUL"))r[a]=left*right;else if(n.startsWith("DIV"))r[a]=left/right;else if(n.startsWith("AND"))r[a]=left&right;else if(n.startsWith("OR"))r[a]=left|right;else if(n.startsWith("XOR"))r[a]=left^right;else if(n.startsWith("USHR"))r[a]=left>>>right;else if(n.startsWith("SHR"))r[a]=left>>right;else if(n.startsWith("SHL"))r[a]=left<<right;else throw new AssertionError("unsupported integer "+n);
   }else throw new AssertionError("unimplemented production opcode "+n+" "+id+" "+pc);
   pc=next;
  }
 }
 static int[]registers(Instruction op){if(op instanceof RegisterRangeInstruction){RegisterRangeInstruction rr=(RegisterRangeInstruction)op;int[]v=new int[rr.getRegisterCount()];for(int i=0;i<v.length;i++)v[i]=rr.getStartRegister()+i;return v;}FiveRegisterInstruction rr=(FiveRegisterInstruction)op;int[]all={rr.getRegisterC(),rr.getRegisterD(),rr.getRegisterE(),rr.getRegisterF(),rr.getRegisterG()};return Arrays.copyOf(all,rr.getRegisterCount());}
 public static void bilateral(int[]g,int[]v,int[]o,int width,int rows,int level,boolean h,boolean t,boolean s,int begin,int end){call(OWNER+"->bilateral([I[I[IIIIZZZII)V",g,v,o,width,rows,level,h,t,s,begin,end);}
 public static int[]table(String name,int level){return ((int[][])fields.get(OWNER+"->"+name+"_RANGE:[[I"))[level];}
}
