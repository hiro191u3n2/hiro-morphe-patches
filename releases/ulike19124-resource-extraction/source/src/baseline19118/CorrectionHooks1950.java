import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.builder.*;

/** H4: save-strip-only correction clone, preserving the general full-output API.
 *
 * Chroma186.finishJava initializes destination[current] from filtered[current]
 * before every possible read of destination[current]. The pinned native kernel
 * does the same. Neither implementation consumes destination halo pixels.
 * QualityPipeline1932.run publishes only the requested rows and residual NR
 * reads only the corrected lo..hi rows. Its focused clone can therefore omit
 * initialising destination halo rows or redundantly copying consumed rows.
 * Validation, cancellation, native admission and arithmetic remain exact.
 */
public final class CorrectionHooks1950 {
 static final String OWNER="Lcom/hiro/ulike/Chroma186;";
 static final String PROTO="([I[I[IIIIII[I[IZ)V";
 static final String ORIGINAL=OWNER+"->finishWorkspace"+PROTO;
 static final String FOCUSED=OWNER+"->finishConsumed1950"+PROTO;
 static final String ORIGINAL_SHA256="8dc6bf3799c33fa6fecf16e05bb906d06e9822a927364c13cc76738263ca1b91";
 static final String JAVA_SHA256="1e1489519bbbd487eb99ab1d74c6a5165315d0d43ef9e934965eadf6c6cc43ec";
 static final String ROW_SHA256="40aaa9607db3ba7d0c92f3cd165c0a3a321ccdea2d86504031089dc0177e79cd";
 static final String NATIVE_ENTRY="ulike186/runtime/0000.bin";
 static final String NATIVE_SHA256="0ac181fac2afbd9260b8c59d922843deead72d3184e78ce97346d57a76b169cb";
 static final String COPY="Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V";
 static void need(boolean b,String message){MergePayloads.require(b,message);}
 static Set<String> changedIds(){return Set.of();}
 static Set<String> newIds(){return Set.of(FOCUSED);}
 static Method withBody(Method m,String name,MethodImplementation body){return new ImmutableMethod(m.getDefiningClass(),name,m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);}
 static ClassDef withMethods(ClassDef c,Collection<Method> methods){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);}
 static Method find(ClassDef c,String id){for(Method m:c.getMethods())if(MergePayloads.id(m).equals(id))return m;throw new IllegalStateException("Missing exact correction method "+id);}
 static int copyIndex(Method m){
  need(m.getImplementation()!=null&&m.getImplementation().getRegisterCount()==27,"Pinned correction register count");
  int count=0,index=-1,at=0;
  for(Instruction op:m.getImplementation().getInstructions()){
   if(op instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference&&r.getReference().toString().equals(COPY)){
    need(op.getOpcode()==Opcode.INVOKE_STATIC&&op instanceof FiveRegisterInstruction,"Pinned copy opcode");
    var f=(FiveRegisterInstruction)op;
    need(f.getRegisterCount()==5&&f.getRegisterC()==1&&f.getRegisterD()==11&&f.getRegisterE()==2&&f.getRegisterF()==11&&f.getRegisterG()==10,"Pinned filtered-to-output full-strip copy");
    index=at;count++;
   }
   at++;
  }
  need(count==1,"Exactly one initial correction copy");return index;
 }
 static Method focused(Method original){
  need(MergePayloads.hash(original).equals(ORIGINAL_SHA256),"Exact .49 correction wrapper pin");
  int index=copyIndex(original);var body=new MutableMethodImplementation(original.getImplementation());
  body.removeInstruction(index);
  Method result=withBody(original,"finishConsumed1950",body);
  var inverse=new MutableMethodImplementation(result.getImplementation());
  var old=new MutableMethodImplementation(original.getImplementation());
  inverse.addInstruction(index,old.getInstructions().get(index));
  need(MergePayloads.hash(withBody(original,original.getName(),inverse)).equals(MergePayloads.hash(original)),"Copy-only focused correction inverse");
  return result;
 }
 static void apply(Map<String,ClassDef> runtime,Map<String,ClassDef> before){
  ClassDef c=before.get(OWNER);need(c!=null,"Pinned production Chroma186 required");
  var methods=new ArrayList<Method>();for(Method m:c.getMethods()){need(!m.getName().equals("finishConsumed1950"),"No existing focused API");methods.add(m);}
  need(MergePayloads.hash(find(c,OWNER+"->finishJava([I[I[IIIIII[I[I)V")).equals(JAVA_SHA256),"Pinned correction Java algorithm");
  need(MergePayloads.hash(find(c,OWNER+"->row([III[I[II)V")).equals(ROW_SHA256),"Pinned correction halo row aggregation");
  methods.add(focused(find(c,ORIGINAL)));runtime.put(OWNER,withMethods(c,methods));verify(before,runtime);
 }
 static void verify(Map<String,ClassDef> before,Map<String,ClassDef> after)throws RuntimeException{
  try{
   ClassDef old=before.get(OWNER),next=after.get(OWNER);need(old!=null&&next!=null,"Both production correction classes required");
   var original=find(old,ORIGINAL);need(MergePayloads.hash(original).equals(ORIGINAL_SHA256),"Exact correction wrapper retained baseline");
   var focused=find(next,FOCUSED);need(MergePayloads.hash(focused).equals(MergePayloads.hash(focused(original))),"Focused correction is exact copy-elision clone");
   var without=new ArrayList<Method>();int newCount=0;
   for(Method m:next.getMethods())if(MergePayloads.id(m).equals(FOCUSED))newCount++;else without.add(m);
   need(newCount==1&&MergePayloads.classHash(old).equals(MergePayloads.classHash(withMethods(next,without))),"All general correction methods and fields byte-identical");
   int calls=0;
   for(ClassDef c:after.values())for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction op:m.getImplementation().getInstructions())if(op instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference&&r.getReference().toString().equals(FOCUSED)){
    need(MergePayloads.id(m).equals("Lcom/hiro/ulike/QualityPipeline1932;->run(Lcom/hiro/ulike/ChromaPipeline186$State;Lcom/hiro/ulike/ChromaPipeline186$Buffer;)V"),"Only save-strip owned-range consumer may omit full copy");calls++;
   }
   need(calls==2,"Exact two correction consumers in saved-image pipeline");
  }catch(RuntimeException ex){throw ex;}catch(Exception ex){throw new IllegalStateException(ex);}
 }
}
