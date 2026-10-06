import com.android.tools.smali.dexlib2.formatter.DexFormatter;import java.nio.file.*;import java.util.*;import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;
public class VerifyRestart1926 {
 static void req(boolean x,String m){MergePayloads.require(x,m);}static String ref(Instruction i){return Transform1926.ref(i);}
 public static void main(String[]a)throws Exception{
  var ms=MergePayloads.methods(MergePayloads.classes(a[0]).values());var rs=MergePayloads.methods(MergePayloads.classes(a[1]).values());
  var m=ms.get(Transform1926.INIT);var ins=new ArrayList<Instruction>();m.getImplementation().getInstructions().forEach(ins::add);int after=-1,store=-1,n=0;
  for(int i=0;i<ins.size();i++){String r=ref(ins.get(i));req(!r.contains("OpticalZoom;->track("),"Early hook survived");if(r.equals("Lcom/ss/android/vesdk/VECameraCapture;->c:Lcom/ss/android/ttvecamera/TECameraSettings;"))store=i;if(r.contains("RearRestart1926;->initialized(")){after=i;n++;}}
  req(n==1&&store>=0&&after>store&&ins.get(after+1).getOpcode()==Opcode.RETURN,"Initializer not after native settings and adjacent to return");
  // A jump must enter the tracking instruction, not skip directly to return.
  var addresses=new ArrayList<Integer>();int pc=0;for(var i:ins){addresses.add(pc);pc+=i.getCodeUnits();}
  for(int i=0;i<ins.size();i++)if(ins.get(i) instanceof OffsetInstruction o)req(addresses.get(i)+o.getCodeOffset()!=addresses.get(after+1),"Branch bypasses completed initialization tracking");
  if(a.length==3){
   var nativeClasses=MergePayloads.classes(a[2]);var fs=new TreeMap<String,Field>();
   for(var c:nativeClasses.values())for(var f:c.getFields())fs.put(DexFormatter.INSTANCE.getFieldDescriptor(f),f);
   for(String f:List.of("Lcom/ss/android/vesdk/VECameraCapture;->d:Landroid/content/Context;","Lcom/ss/android/vesdk/VECameraCapture;->a:Lcom/ss/android/vesdk/VECameraSettings;","Lcom/ss/android/vesdk/VECameraCapture;->c:Lcom/ss/android/ttvecamera/TECameraSettings;","Li/s/a/w/h0/b;->h:Lcom/ss/android/ttvecamera/TECameraSettings;"))req(fs.containsKey(f),"Missing native reflected field "+f);
   for(String f:List.of("Lcom/ss/android/ttvecamera/TECameraSettings;->u0:Z","Lcom/ss/android/ttvecamera/TECameraSettings;->M:I")){Field x=fs.get(f);req(x!=null&&(x.getAccessFlags()&1)!=0&&(x.getAccessFlags()&8)==0,"Native public instance setting mismatch "+f);}
   System.out.println("PASS1926 six native reflected fields resolved; branch cannot bypass completed-init hook");
  }
  int changed=0;for(String id:Transform1926.RUNTIME){m=rs.get(id);req(m!=null,"Missing runtime target");int count=0;for(var i:m.getImplementation().getInstructions()){String r=ref(i);req(!r.equals("Lcom/hiro/ulike/ManualLens170;->ready()Z"),"Old recovery readiness survived");if(r.startsWith(Transform1926.H+"->"))count++;}req(count==1,"Runtime must call bridge once "+id);changed++;}
  req(rs.containsKey(Transform1926.H+"->recoveryWindowReady()Z"),"Helper absent");
  System.out.println("PASS emitted rear-start contracts: 1 init ordering + "+changed+" runtime hooks; other code retained by transformer");
 }
}
