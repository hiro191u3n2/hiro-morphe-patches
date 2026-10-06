import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
/** Additional emitted-code gates. Old label-safe returns and ABI gate run separately. */
public final class VerifyLayout1924 {
 public static void main(String[] a)throws Exception {
  var stock=MergePayloads.methods(MergePayloads.classes(a[0]).values());
  var seed=MergePayloads.methods(MergePayloads.classes(a[1]).values());
  var patched=MergePayloads.methods(MergePayloads.classes(a[2]).values());
  var rt=MergePayloads.classes(a[3]);var methods=MergePayloads.methods(rt.values());
  String key=Transform1924.S+"->onAttachedToWindow()V";
  MergePayloads.require(seed.size()==1 && MergePayloads.hash(stock.get(key)).equals(MergePayloads.hash(seed.get(key))),"Original attach contract");
  VerifyLayout1923.guardedReturns(patched.get(key),Transform1924.H+"->attached("+Transform1924.S+")V",Opcode.RETURN_VOID);
  String h=Transform1924.H;Field snap=null;
  for(var f:rt.get(h).getFields())if(f.getName().equals("snapshot"))snap=f;
  MergePayloads.require(snap!=null && (snap.getAccessFlags()&0x40)!=0,"Viewport publication not volatile");
  var viewport=methods.get(h+"->viewport()Landroid/graphics/RectF;");
  for(var i:viewport.getImplementation().getInstructions())MergePayloads.require(!VerifyLayout1923.ref(i).contains("CameraShadeView"),"Renderer reads live View");
  var smooth=methods.get(Transform1924.N+"->smoothRange([I[IIIIIIIIIZI)V");
  // Match by name because dex encodes the entire validated width/row/core/halo contract.
  smooth=null;for(var m:rt.get(Transform1924.N).getMethods())if(m.getName().equals("smoothRange"))smooth=m;
  MergePayloads.require(smooth!=null,"Processed-domain smoother absent");
  var run=methods.get(Transform1924.N+"->run("+Transform1924.STATE+Transform1924.BUFFER+")V");
  boolean snapshot=false,detail=false,chroma=false,smoother=false;
  for(var i:run.getImplementation().getInstructions()){
   String ref=VerifyLayout1923.ref(i);
   snapshot|=ref.equals(Transform1924.WORK+"->denoised:[I");
   detail|=ref.contains("DetailSerial186;->filter(");chroma|=ref.contains("Chroma186;->finishWorkspace(");smoother|=ref.contains("ShadowDetail1923;->smoothRange(");
  }
  MergePayloads.require(snapshot&&detail&&chroma&&smoother,"Main pipeline finishing snapshot/stages absent");
  System.out.println("PASS1924 original attach hash and guarded return; volatile immutable viewport; renderer performs no live View reads; processed-domain shadow snapshot and both upstream stages connected.");
 }
}
