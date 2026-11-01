import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Direct interpreter for the pinned production correction bytecode. It supplies
 * DEX instruction semantics, not a replacement chroma algorithm. The native
 * entry point is unavailable on this host; its fallback is exercised and its
 * independently pinned ARM64 destination accesses are reviewed separately. */
public final class CorrectionDex1950Test {
 static final String OWNER=CorrectionHooks1950.OWNER;
 static final Map<String,Code> compiled=new HashMap<>();
 static final class Code {
  final Instruction[] ops;final int[]pos;final int registers;final Method method;
  final Map<Integer,Integer>address=new HashMap<>();
  Code(Method m){method=m;var ls=new ArrayList<Instruction>();for(Instruction op:m.getImplementation().getInstructions())ls.add(op);ops=ls.toArray(Instruction[]::new);pos=new int[ops.length];int p=0;for(int i=0;i<ops.length;i++){pos[i]=p;address.put(p,i);p+=ops[i].getCodeUnits();}registers=m.getImplementation().getRegisterCount();}
 }
 static int assertions,invalidTests,pixelComparisons,outputReads,nativeFallbacks;
 static long removedBytes;
 static int[]trackedOutput;static BitSet initialized;static int consumedFirst,consumedLast;
 static void need(boolean b,String reason){assertions++;if(!b)throw new AssertionError(reason);}
 static int iv(Object x){return x==null?0:x instanceof Boolean?((Boolean)x?1:0):((Number)x).intValue();}
 static long lv(Object x){return x==null?0:((Number)x).longValue();}
 static double dv(Object x){return x instanceof Double?(Double)x:Double.longBitsToDouble(lv(x));}
 static String ref(Instruction op){return ((ReferenceInstruction)op).getReference().toString();}
 static boolean eq(Object a,Object b){if(a instanceof Number||a instanceof Boolean||b instanceof Number||b instanceof Boolean)return iv(a)==iv(b);return a==b;}
 static int[]regs(Instruction op){if(op instanceof RegisterRangeInstruction r){int[]v=new int[r.getRegisterCount()];for(int i=0;i<v.length;i++)v[i]=r.getStartRegister()+i;return v;}var r=(FiveRegisterInstruction)op;return Arrays.copyOf(new int[]{r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()},r.getRegisterCount());}
 static Object standard(String id,Object[]a){
  return switch(id){
   case "Ljava/lang/Math;->abs(I)I" -> Math.abs(iv(a[0]));
   case "Ljava/lang/Math;->abs(D)D" -> Math.abs(dv(a[0]));
   case "Ljava/lang/Math;->min(II)I" -> Math.min(iv(a[0]),iv(a[1]));
   case "Ljava/lang/Math;->max(II)I" -> Math.max(iv(a[0]),iv(a[1]));
   case "Ljava/lang/Math;->max(JJ)J" -> Math.max(lv(a[0]),lv(a[1]));
   case "Ljava/lang/Math;->max(DD)D" -> Math.max(dv(a[0]),dv(a[1]));
   case "Ljava/lang/Math;->min(DD)D" -> Math.min(dv(a[0]),dv(a[1]));
   case "Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;" -> Thread.currentThread();
   case "Ljava/lang/Thread;->isInterrupted()Z" -> false;
   case "Ljava/util/Arrays;->fill([IIII)V" -> {Arrays.fill((int[])a[0],iv(a[1]),iv(a[2]),iv(a[3]));yield null;}
   case "Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V" -> {System.arraycopy(a[0],iv(a[1]),a[2],iv(a[3]),iv(a[4]));if(a[2]==trackedOutput)initialized.set(iv(a[3]),iv(a[3])+iv(a[4]));yield null;}
   case "Lcom/hiro/ulike/NativeChroma186;->tryFinish([I[I[IIIIII[I[I)Z" -> {nativeFallbacks++;yield false;}
   case "Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V", "Ljava/util/concurrent/CancellationException;-><init>(Ljava/lang/String;)V" -> null;
   default -> throw new AssertionError("Unsupported production call "+id);
  };
 }
 static Object call(String id,Object...args){
  if(!id.startsWith(OWNER))return standard(id,args);
  Code c=compiled.get(id);if(c==null)throw new AssertionError("Missing actual production body "+id);
  Object[]r=new Object[c.registers];int words=0;for(CharSequence p:c.method.getParameterTypes())words+=p.toString().equals("J")||p.toString().equals("D")?2:1;
  int start=c.registers-words;for(int i=0;i<args.length;i++){r[start]=args[i];String p=c.method.getParameterTypes().get(i).toString();start+=p.equals("J")||p.equals("D")?2:1;}
  Object result=null;int pc=0;
  while(true){
   Instruction op=c.ops[pc];String n=op.getOpcode().name();int a=op instanceof OneRegisterInstruction x?x.getRegisterA():-1,b=op instanceof TwoRegisterInstruction x?x.getRegisterB():-1,z=op instanceof ThreeRegisterInstruction x?x.getRegisterC():-1,next=pc+1;
   if(n.startsWith("CONST_STRING"))r[a]=((StringReference)((ReferenceInstruction)op).getReference()).getString();
   else if(n.startsWith("CONST_WIDE"))r[a]=((WideLiteralInstruction)op).getWideLiteral();
   else if(n.startsWith("CONST_")||n.equals("CONST"))r[a]=(int)((WideLiteralInstruction)op).getWideLiteral();
   else if(n.startsWith("MOVE_RESULT"))r[a]=result;
   else if(n.startsWith("MOVE"))r[a]=r[b];
   else if(n.equals("NEW_INSTANCE"))r[a]=ref(op);
   else if(n.equals("THROW"))throw new IllegalArgumentException(String.valueOf(r[a]));
   else if(n.equals("ARRAY_LENGTH"))r[a]=Array.getLength(r[b]);
   else if(n.equals("AGET")){
    if(r[b]==trackedOutput){int at=iv(r[z]);outputReads++;need(at>=consumedFirst&&at<consumedLast,"Correction must not consume destination halos");need(initialized.get(at),"Destination read must follow per-pixel production initialization");}
    r[a]=Array.get(r[b],iv(r[z]));
   }else if(n.equals("APUT")){
    if(r[b]==trackedOutput){int at=iv(r[z]);need(at>=consumedFirst&&at<consumedLast,"All destination writes are owned consumed pixels");initialized.set(at);}
    Array.set(r[b],iv(r[z]),r[a]);
   }else if(n.equals("INT_TO_LONG"))r[a]=(long)iv(r[b]);
   else if(n.equals("INT_TO_DOUBLE"))r[a]=(double)iv(r[b]);
   else if(n.equals("LONG_TO_DOUBLE"))r[a]=(double)lv(r[b]);
   else if(n.equals("LONG_TO_INT"))r[a]=iv(r[b]);
   else if(n.equals("DOUBLE_TO_INT"))r[a]=(int)dv(r[b]);
   else if(n.equals("NEG_INT"))r[a]=-iv(r[b]);
   else if(n.equals("NEG_LONG"))r[a]=-lv(r[b]);
   else if(n.equals("NEG_DOUBLE"))r[a]=-dv(r[b]);
   else if(n.equals("CMP_LONG"))r[a]=Long.compare(lv(r[b]),lv(r[z]));
   else if(n.equals("CMPG_DOUBLE")||n.equals("CMPL_DOUBLE")){double left=dv(r[b]),right=dv(r[z]);r[a]=Double.isNaN(left)||Double.isNaN(right)?(n.equals("CMPG_DOUBLE")?1:-1):left==right?0:left<right?-1:1;}
   else if(n.startsWith("INVOKE")){
    int[]rs=regs(op);var mr=(MethodReference)((ReferenceInstruction)op).getReference();boolean virtual=n.startsWith("INVOKE_VIRTUAL")||n.startsWith("INVOKE_DIRECT");List<? extends CharSequence>ps=mr.getParameterTypes();Object[]actual=new Object[ps.size()];int j=virtual?1:0;for(int k=0;k<actual.length;k++){actual[k]=r[rs[j]];String t=ps.get(k).toString();j+=t.equals("J")||t.equals("D")?2:1;}result=call(ref(op),actual);
   }else if(n.equals("RETURN_VOID"))return null;
   else if(n.startsWith("RETURN"))return r[a];
   else if(n.startsWith("GOTO"))next=c.address.get(c.pos[pc]+((OffsetInstruction)op).getCodeOffset());
   else if(n.startsWith("IF_")){
    Object left=r[a],right=b<0?null:r[b];boolean hit=n.startsWith("IF_EQ")?eq(left,right):n.startsWith("IF_NE")?!eq(left,right):n.startsWith("IF_LE")?iv(left)<=iv(right):n.startsWith("IF_LT")?iv(left)<iv(right):n.startsWith("IF_GE")?iv(left)>=iv(right):iv(left)>iv(right);if(hit)next=c.address.get(c.pos[pc]+((OffsetInstruction)op).getCodeOffset());
   }else if(n.contains("_INT")){
    int left=n.endsWith("_2ADDR")?iv(r[a]):iv(r[b]),right=n.endsWith("_2ADDR")?iv(r[b]):op instanceof NarrowLiteralInstruction x?x.getNarrowLiteral():iv(r[z]);
    r[a]=switch(n.substring(0,n.indexOf('_'))){case "ADD"->left+right;case "SUB"->left-right;case "RSUB"->right-left;case "MUL"->left*right;case "DIV"->left/right;case "REM"->left%right;case "AND"->left&right;case "OR"->left|right;case "XOR"->left^right;case "USHR"->left>>>right;case "SHR"->left>>right;case "SHL"->left<<right;default->throw new AssertionError("Integer opcode "+n);};
   }else if(n.contains("_LONG")){
    long left=n.endsWith("_2ADDR")?lv(r[a]):lv(r[b]),right=n.endsWith("_2ADDR")?lv(r[b]):lv(r[z]);r[a]=switch(n.substring(0,n.indexOf('_'))){case "ADD"->left+right;case "SUB"->left-right;case "MUL"->left*right;case "DIV"->left/right;default->throw new AssertionError("Long opcode "+n);};
   }else if(n.contains("_DOUBLE")){
    double left=n.endsWith("_2ADDR")?dv(r[a]):dv(r[b]),right=n.endsWith("_2ADDR")?dv(r[b]):dv(r[z]);r[a]=switch(n.substring(0,n.indexOf('_'))){case "ADD"->left+right;case "SUB"->left-right;case "MUL"->left*right;case "DIV"->left/right;default->throw new AssertionError("Double opcode "+n);};
   }else if(!n.equals("NOP"))throw new AssertionError("Unsupported production opcode "+n+" "+id+" @"+Integer.toHexString(c.pos[pc]));
   pc=next;
  }
 }
 static void invoke(String method,int[]guide,int[]filtered,int[]out,int w,int h,int lo,int hi,int radius,boolean nativeAllowed){trackedOutput=out;initialized=new BitSet(out.length);consumedFirst=lo*w;consumedLast=hi*w;try{call(method,guide,filtered,out,w,h,lo,hi,radius,new int[w*7],new int[w*2],nativeAllowed);}finally{trackedOutput=null;}}
 static int mix(int x){x^=x>>>16;x*=0x7feb352d;x^=x>>>15;x*=0x846ca68b;return x^(x>>>16);}
 static int pixel(int x,int y,int mode){int n=mix(x*8273+y*92381+mode*877),r,g,b;int v=(n>>>1)%19-9;switch(mode%5){case 0:r=135+v;g=135+v;b=135+v;break;case 1:r=27+v;g=25+v;b=23+v;break;case 2:r=180+v;g=155+v;b=133+v;break;case 3:r=110+v;g=143+v;b=180+v;break;default:r=(n>>>16)&255;g=(n>>>8)&255;b=n&255;}int alpha=mode>=5&&((x+y)%7==0)?0x60:255;return alpha<<24|r<<16|g<<8|b;}
 static void compare(int w,int h,int lo,int hi,int radius,int mode,boolean nativeAllowed){
  int size=w*h;int[]g=new int[size],f=new int[size],old=new int[size],next=new int[size];
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=y*w+x;g[i]=pixel(x,y,mode);f[i]=g[i];if((f[i]>>>24)==255){int r=(f[i]>>>16)&255,b=f[i]&255;f[i]=(f[i]&0xff00ff00)|Math.max(0,Math.min(255,r+(i%3)-1))<<16|Math.max(0,Math.min(255,b-(i%4)));}old[i]=next[i]=0x12345678;}
  invoke(CorrectionHooks1950.ORIGINAL,g,f,old,w,h,lo,hi,radius,nativeAllowed);
  invoke(CorrectionHooks1950.FOCUSED,g,f,next,w,h,lo,hi,radius,nativeAllowed);
  for(int i=0;i<size;i++){if(i>=lo*w&&i<hi*w){need(old[i]==next[i],"Actual production correction pixel mismatch");pixelComparisons++;}else{need(next[i]==0x12345678,"Focused API must leave unconsumed output halos untouched");need(old[i]==f[i],"General API full-output copy preserved");}}
  removedBytes+=(long)size*4;
 }
 public static void main(String[]args)throws Exception{
  need(args.length==1,"Exact baseline correction DEX required");var cs=MergePayloads.classes(args[0]);var cls=cs.get(OWNER);need(cls!=null,"Pinned production Chroma class");
  for(Method m:cls.getMethods())if(m.getImplementation()!=null)compiled.put(m.toString(),new Code(m));
  var focused=CorrectionHooks1950.focused(CorrectionHooks1950.find(cls,CorrectionHooks1950.ORIGINAL));compiled.put(focused.toString(),new Code(focused));
  need(MergePayloads.hash(CorrectionHooks1950.find(cls,OWNER+"->finishJava([I[I[IIIIII[I[I)V")).equals(CorrectionHooks1950.JAVA_SHA256),"Exact production Java algorithm pin");
  int[][]shapes={{1,1,0,1,1},{3,7,0,2,2},{3,7,5,7,2},{17,21,5,17,4},{31,23,2,22,4},{40,37,10,24,12},{13,39,18,20,7},{64,43,0,43,12},{64,43,14,29,12},{13,8,3,3,4}};
  for(int[]s:shapes)for(int mode=0;mode<10;mode++)compare(s[0],s[1],s[2],s[3],s[4],mode,(mode&1)==1);
  for(String method:new String[]{CorrectionHooks1950.ORIGINAL,CorrectionHooks1950.FOCUSED})for(int bad=0;bad<5;bad++){
   try{int[]g=new int[20],f=new int[20],o=new int[20];if(bad==0)g=null;if(bad==1)o=f;invoke(method,g,f,o,bad==2?0:4,5,bad==3?-1:0,bad==4?6:5,2,false);throw new AssertionError("Invalid input accepted");}catch(IllegalArgumentException expected){invalidTests++;assertions++;}
  }
  need(nativeFallbacks>0,"Native-unavailable fallback executed");need(outputReads>0,"Destination initialization read assertions executed");
  // Measure only the eliminated real memory pass. This is not a benchmark of
  // the correction VM, the Android app, or the complete camera/save path.
  int copyWidth=4080,copyRows=256,copyCount=128;int[]copySource=new int[copyWidth*copyRows],copyTarget=new int[copySource.length];
  for(int i=0;i<copySource.length;i++)copySource[i]=mix(i);
  for(int i=0;i<32;i++)System.arraycopy(copySource,0,copyTarget,0,copySource.length);
  long copyStarted=System.nanoTime(),copyChecksum=0;
  for(int i=0;i<copyCount;i++){copySource[0]=i;System.arraycopy(copySource,0,copyTarget,0,copySource.length);copyChecksum+=copyTarget[(i*3137)%copyTarget.length];}
  long copyElapsed=System.nanoTime()-copyStarted;
  need(copyChecksum!=0&&copyElapsed>0,"Executed eliminated full-strip memory-copy benchmark");
  System.out.println("{\"host_copy_elapsed_nanos\":"+copyElapsed+",\"host_copy_iterations\":"+copyCount+",\"copy_width\":"+copyWidth+",\"copy_rows\":"+copyRows+",\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_production_dex_executed\":true,\"pixel_comparisons\":"+pixelComparisons+",\"destination_reads_after_write\":"+outputReads+",\"native_unavailable_fallback_calls\":"+nativeFallbacks+",\"invalid_cases\":"+invalidTests+",\"removed_copy_bytes_in_test_cases\":"+removedBytes+",\"pixel_equivalence_to_baseline\":true,\"general_correction_contract_preserved\":true,\"consumed_range_only\":true}");
 }
}
