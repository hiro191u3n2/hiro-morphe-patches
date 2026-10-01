import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** PARTIAL, NOT INSTALLED. Pinned native allocation/teardown method transforms.
 * NativeLifetimeHooks is compile-time disabled. This program never calls installed().
 */
public final class PrepareNativeLifetime169 {
 static final String OWNER="Lcom/ss/android/medialib/RecordInvoker;";
 static final String HOOK="Lcom/hiro/ulike/hdr/stillanalysis/NativeLifetimeHooks;";
 static final Map<String,String> PINS=Map.of(
  OWNER+"->initBeautyPlay(IILjava/lang/String;IILjava/lang/String;IZZZ)I","8dc53066a33d64e75cd562ebbaae682d5831f482d020a5f011a8f898a897c95e",
  OWNER+"->initBeautyPlayOnlyPreview(Lcom/ss/android/medialib/qr/ScanSettings;)I","5e562ce39fb84c59a47f769ed8ae74b50f34d6ec704e6f79a59e1405d846b1a5",
  OWNER+"->uninitBeautyPlay()I","f938253b35f0f7439b60a02df35fd42f006b7bb41117e5269b8e86102411229f");
 static ImmutableMethodReference helper(String name,String parameter){return new ImmutableMethodReference(HOOK,name,List.of(parameter),"V");}
 static int receiver(Method m){int n=1;for(var p:m.getParameterTypes())n+=p.equals("J")||p.equals("D")?2:1;return m.getImplementation().getRegisterCount()-n;}
 static Method transform(Method m) {
  MergePayloads.require(MergePayloads.hash(m).equals(PINS.get(MergePayloads.id(m))),"Native lifetime source mismatch: "+MergePayloads.id(m));
  var b=new MutableMethodImplementation(m.getImplementation());int self=receiver(m);
  MergePayloads.require(self>0,"exception scratch v0 overlaps receiver");
  // Receiver must remain live through every original exit; these three pinned methods do not write p0.
  for(var in:m.getImplementation().getInstructions())if(in.getOpcode().setsRegister()&&in instanceof OneRegisterInstruction r) {
   MergePayloads.require(r.getRegisterA()!=self && !(in.getOpcode().setsWideRegister()&&r.getRegisterA()+1==self),"Original receiver is overwritten");
  }
  int returns=0;
  for(int i=b.getInstructions().size()-1;i>=0;i--) {
   var in=b.getInstructions().get(i);if(in.getOpcode()==Opcode.RETURN) {
    int status=((OneRegisterInstruction)in).getRegisterA();
    // Replace the return location so every original branch to that return executes the hook.
    b.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,status,1,helper("returned","I")));
    b.addInstruction(i+1,new BuilderInstruction11x(Opcode.RETURN,status));returns++;
   }
  }
  MergePayloads.require(returns>0,"No native wrapper return");
  // Entry denial must not enter the original catch region or quarantine a previously admitted outer call.
  b.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,self,1,helper(m.getName().equals("uninitBeautyPlay")?"beforeUninit":"beforeInit","Ljava/lang/Object;")));
  int firstOriginal=b.getInstructions().get(0).getCodeUnits(),oldEnd=0;
  for(var in:b.getInstructions())oldEnd+=in.getCodeUnits();
  // Existing monitor cleanup already owns catch-all ranges. Catch only their
  // complement: native exceptions first execute the unchanged monitor handler,
  // whose final rethrow is covered by our appended outer cleanup.
  List<int[]> covered=new ArrayList<>();
  for(var t:b.getTryBlocks())for(var h:t.getExceptionHandlers())if(h.getExceptionType()==null)covered.add(new int[]{t.getStartCodeAddress(),t.getStartCodeAddress()+t.getCodeUnitCount()});
  covered.sort(Comparator.comparingInt(a->a[0]));List<int[]> gaps=new ArrayList<>();int position=firstOriginal;
  for(int[] range:covered){int begin=Math.max(firstOriginal,range[0]),end=Math.min(oldEnd,range[1]);if(begin>position)gaps.add(new int[]{position,begin});position=Math.max(position,end);}
  if(position<oldEnd)gaps.add(new int[]{position,oldEnd});
  b.addInstruction(new BuilderInstruction11x(Opcode.MOVE_EXCEPTION,0));
  Label handler=b.newLabelForIndex(b.getInstructions().size()-1);
  b.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,self,1,helper("failed","Ljava/lang/Object;")));
  b.addInstruction(new BuilderInstruction11x(Opcode.THROW,0));
  for(int[] gap:gaps)b.addCatch(b.newLabelForAddress(gap[0]),b.newLabelForAddress(gap[1]),handler);
  return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);
 }
 public static void main(String[] args)throws Exception {
  if(args.length!=2)throw new IllegalArgumentException("STOCK_APK PRIVATE_OUT");
  var all=MergePayloads.methods(MergePayloads.classes(args[0]).values());var changed=new TreeMap<String,Method>();var contracts=new ArrayList<String>();
  for(String id:new TreeSet<>(PINS.keySet())){Method m=all.get(id);MergePayloads.require(m!=null,"Missing method "+id);Method n=transform(m);changed.put(id,n);contracts.add(id+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(n));}
  Path out=Path.of(args[1]);Files.createDirectories(out);MergePayloads.writeDex(out.resolve("native-lifetime-partial.dex"),MergePayloads.holders(changed));Files.write(out.resolve("method-contracts.tsv"),contracts);
  System.out.println("PASS generated 3 disabled native lifetime hooks; app admission coverage=false; installed=false");
 }
}
