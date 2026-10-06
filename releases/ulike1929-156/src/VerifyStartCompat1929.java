import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
public final class VerifyStartCompat1929 {
 static void req(boolean b,String m){MergePayloads.require(b,m);}
 public static void main(String[]a)throws Exception{
  req(a.length==3||a.length==4,"SEED METHODS RUNTIME [STOCK]");var seed=MergePayloads.methods(MergePayloads.classes(a[0]).values());var ms=MergePayloads.methods(MergePayloads.classes(a[1]).values());var runtime=MergePayloads.classes(a[2]);var rs=MergePayloads.methods(runtime.values());
  int checked=0;
  for(String id:Transform1927.TARGETS){
   Method nativeMethod=seed.get(id), patched=ms.get(id);req(nativeMethod!=null&&patched!=null,"Native target absent");var b=new MutableMethodImplementation(patched.getImplementation());int hits=0;
   for(int i=b.getInstructions().size()-1;i>=0;i--){var ins=b.getInstructions().get(i);String r=Transform1927.ref(ins);
    if(r.equals(Transform1927.H+"->start("+Transform1927.C+")I")){
     var original=new ImmutableMethodReference(Transform1927.C,"startPreview",List.of(),"I");
     if(ins instanceof FiveRegisterInstruction f)b.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,1,f.getRegisterC(),0,0,0,0,original));
     else if(ins instanceof RegisterRangeInstruction f)b.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_VIRTUAL_RANGE,f.getStartRegister(),1,original));else throw new IllegalStateException("invoke encoding");hits++;
    }else if(r.equals(Transform1927.H+"->pipelines("+Transform1927.C+")V")){
     req(i+1==b.getInstructions().size()-1&&b.getInstructions().get(i+1).getOpcode()==Opcode.RETURN_VOID,"Ready hook must be at final successful return");
     int store=-1;for(int j=0;j<i;j++)if(Transform1927.ref(b.getInstructions().get(j)).equals(Transform1927.C+"->n:"+Transform1927.L))store=j;
     req(store>=0,"Ready notification before pipeline store");b.replaceInstruction(i,new BuilderInstruction10x(Opcode.RETURN_VOID));b.removeInstruction(i+1);hits++;
    }
   }
   req(hits==1,"Exactly one handshake per native method");req(MergePayloads.hash(nativeMethod).equals(MergePayloads.hash(Transform1927.repl(patched,b))),"Removing hook did not reconstruct original method exactly "+id);checked++;
  }
  var bridge=rs.get(Transform1927.H+"->start("+Transform1927.C+")I");int nativeCalls=0;for(var i:bridge.getImplementation().getInstructions())if(Transform1927.ref(i).equals(Transform1927.C+"->startPreview()I"))nativeCalls++;
  req(nativeCalls==1,"Bridge recursively calls wrapper or loses original");
  req(Transform1929.ref(ms.get(Transform1927.C+"->startPreview()I").getImplementation().getInstructions().iterator().next()).equals(Transform1929.I+"->before(Ljava/lang/Object;)Z"),"Start entry must use independently verified1929 input guard");
  int external=0;
  if(a.length==4){
   var stock=MergePayloads.classes(a[3]);var nativeMethods=MergePayloads.methods(stock.values());var fields=new TreeMap<String,Field>();for(var c:stock.values())for(var f:c.getFields())fields.put(DexFormatter.INSTANCE.getFieldDescriptor(f),f);
   for(String id:Transform1927.TARGETS)req(MergePayloads.hash(nativeMethods.get(id)).equals(MergePayloads.hash(seed.get(id))),"Seed not stock "+id);
   for(var c:runtime.values())if(c.getType().startsWith("Lcom/hiro/ulike/PreviewStart1927"))for(var m:c.getMethods())if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions()){
    if(i instanceof ReferenceInstruction ri){var r=ri.getReference();if(r instanceof com.android.tools.smali.dexlib2.iface.reference.MethodReference mr&&mr.getDefiningClass().startsWith("Lcom/ss/android/vesdk/")){
     String key=DexFormatter.INSTANCE.getMethodDescriptor(mr);var actual=nativeMethods.get(key);req(actual!=null&&(actual.getAccessFlags()&1)!=0,"Unknown/nonpublic SDK method "+key);external++;
    }else if(r instanceof com.android.tools.smali.dexlib2.iface.reference.FieldReference fr&&fr.getDefiningClass().startsWith("Lcom/ss/android/vesdk/")){
     String key=DexFormatter.INSTANCE.getFieldDescriptor(fr);var actual=fields.get(key);req(actual!=null&&(actual.getAccessFlags()&1)!=0,"Unknown SDK field "+key);external++;
    }}
   }
   for(String key:List.of("Li/s/a/w/h0/b;->j:Landroid/hardware/camera2/CameraDevice;","Li/s/a/w/h0/b;->d:Landroid/hardware/camera2/CameraCaptureSession;","Li/s/a/w/h0/b;->g:Li/s/a/w/g;","Li/s/a/w/g;->I:I","Li/s/a/w/g;->M:Li/s/a/w/h0/b;","Li/s/a/w/g;->K:Landroid/hardware/camera2/CameraDevice;")){
    var f=fields.get(key);req(f!=null&&(f.getAccessFlags()&1)!=0,"Reflected native readiness field mismatch "+key);external++;
   }
  }
  System.out.println("PASS1927 native hooks="+checked+"; inverse transform matches unmodified native methods; start entry guarded by separately verified1929 input bridge; SDK/reflection ABI references="+external);
 }
}
