import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** PARTIAL, NOT INSTALLED. Pinned composer/native allocation/teardown transforms.
 * NativeComposerHooks is compile-time disabled. This program never calls installed().
 */
public final class PrepareComposerObservation171 {
 static final String OWNER="Lcom/ss/android/medialib/RecordInvoker;";
 static final String HOOK="Lcom/hiro/ulike/composer/NativeComposerHooks;";
 static final Map<String,String> PINS=Map.ofEntries(
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->appendComposerNodes([Ljava/lang/String;I)I","89ddb98c1e9e4b383d375d2d30d7309fa7bf5d437fd240cf8e966c0728e5f162"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->initBeautyPlay(IILjava/lang/String;IILjava/lang/String;IZZZ)I","8dc53066a33d64e75cd562ebbaae682d5831f482d020a5f011a8f898a897c95e"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->initBeautyPlayOnlyPreview(Lcom/ss/android/medialib/qr/ScanSettings;)I","5e562ce39fb84c59a47f769ed8ae74b50f34d6ec704e6f79a59e1405d846b1a5"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->reloadComposerNodes([Ljava/lang/String;I)I","a577ed4629ae7214d6b7e845f32bb0c04428643f14aec6dfaa5105c251c6e5cc"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->removeComposerNodes([Ljava/lang/String;I)I","7da08bd3a2ff2987511847c9731628d4dd089ab416677e4f907c1fc25ae0adc9"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->replaceComposerNodes([Ljava/lang/String;I[Ljava/lang/String;I)I","ba4d7aa027eb77268721b90d908334f00bbff7f20d5ebaf0a56ecfc98f9f6964"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->setComposerMode(II)I","38c30ae52c978213fbc50317acdacb20abf8ef12ba667feb6629a50180d32084"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->setComposerNodes([Ljava/lang/String;I)I","af816e0027432ab5d887c870164c4e022390dddb62b3f782362519cc4e460a45"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->setComposerResourcePath(Ljava/lang/String;)I","197d0f841f6bdb14954aa2372c12c585eada35ff4cf54ac844c89182f9e7d397"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->setVEEffectParams(Lcom/ss/android/vesdk/VEEffectParams;)I","9ae6771fe47f4b93dae67950660b4e277cfc92955134f9b214ba8a4bc94d024b"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->uninitBeautyPlay()I","f938253b35f0f7439b60a02df35fd42f006b7bb41117e5269b8e86102411229f"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->updateComposerNode(Ljava/lang/String;Ljava/lang/String;F)I","a6926d7865130877cc4b7ef4db0a18129e824b0153e82757021c08e7d4b29088"),
  Map.entry("Lcom/ss/android/medialib/RecordInvoker;->updateMultiComposerNodes(I[Ljava/lang/String;[Ljava/lang/String;[F)I","07b682c6574a937bd5ad16a08cee7f0b0571acdf748a5355af19f5f7c01148f9"));
 static ImmutableMethodReference helper(String name,String parameter){return new ImmutableMethodReference(HOOK,name,List.of(parameter),"V");}
 static final Map<String,String> ENTRIES=Map.ofEntries(
  Map.entry("initBeautyPlay","beforeInit"),Map.entry("initBeautyPlayOnlyPreview","beforeUnsupportedInit"),Map.entry("uninitBeautyPlay","beforeUninit"),
  Map.entry("setComposerMode","beforeMode"),Map.entry("setComposerResourcePath","beforeResource"),Map.entry("setComposerNodes","beforeSet"),
  Map.entry("appendComposerNodes","beforeAppend"),Map.entry("removeComposerNodes","beforeRemove"),Map.entry("reloadComposerNodes","beforeReload"),
  Map.entry("replaceComposerNodes","beforeReplace"),Map.entry("updateComposerNode","beforeUpdate"),Map.entry("updateMultiComposerNodes","beforeUpdates"),Map.entry("setVEEffectParams","beforeEffectParams"));
 static ImmutableMethodReference entry(Method m){var p=new ArrayList<String>();p.add("Ljava/lang/Object;");for(var type:m.getParameterTypes())p.add(type.toString().startsWith("Lcom/")?"Ljava/lang/Object;":type.toString());return new ImmutableMethodReference(HOOK,ENTRIES.get(m.getName()),p,"V");}
 static int receiver(Method m){int n=1;for(var p:m.getParameterTypes())n+=p.equals("J")||p.equals("D")?2:1;return m.getImplementation().getRegisterCount()-n;}
 static Method transform(Method m) {
  MergePayloads.require(MergePayloads.hash(m).equals(PINS.get(MergePayloads.id(m))),"Composer observation source mismatch: "+MergePayloads.id(m));
  var b=new MutableMethodImplementation(m.getImplementation());int self=receiver(m);
  MergePayloads.require(self>0,"exception scratch v0 overlaps receiver");
  // Receiver must remain live through every original exit; these pinned methods do not write p0.
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
  b.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,self,m.getImplementation().getRegisterCount()-self,entry(m)));
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
  // Scan every direct DEX invocation of the ten underlying native writers.
  // Reflection, native-internal writes and other effect APIs remain unproven.
  var callers=new ArrayList<String>();var nativeNames=new HashSet<String>();var counts=new HashMap<String,Integer>();
  for(String name:ENTRIES.keySet())if(!name.contains("initBeautyPlay") && !name.equals("initBeautyPlay"))nativeNames.add("native"+Character.toUpperCase(name.charAt(0))+name.substring(1));
  nativeNames.remove("nativeUninitBeautyPlay");
  for(var caller:all.values())if(caller.getImplementation()!=null) {
   int offset=0;for(var instruction:caller.getImplementation().getInstructions()) {
    if(instruction instanceof ReferenceInstruction reference && reference.getReference() instanceof MethodReference target && target.getDefiningClass().equals(OWNER) && nativeNames.contains(target.getName())) {
     String source=MergePayloads.id(caller),expected="native"+Character.toUpperCase(caller.getName().charAt(0))+caller.getName().substring(1);
     MergePayloads.require(PINS.containsKey(source) && expected.equals(target.getName()),"Unhooked direct native composer writer: "+source);
     counts.merge(target.getName(),1,Integer::sum);callers.add(source+"\t"+offset+"\t"+target.getName()+"\t"+MergePayloads.hash(caller));
    }offset+=instruction.getCodeUnits();
   }
  }
  MergePayloads.require(counts.size()==10 && counts.values().stream().allMatch(n->n==1),"Pinned native direct-call inventory changed");
  Path out=Path.of(args[1]);Files.createDirectories(out);MergePayloads.writeDex(out.resolve("composer-observation-partial.dex"),MergePayloads.holders(changed));Files.write(out.resolve("composer-method-contracts.tsv"),contracts);Collections.sort(callers);Files.write(out.resolve("composer-native-callers.tsv"),callers);
  System.out.println("PASS generated 13 disabled composer/lifetime observation hooks; all-state coverage=false; native barriers=false; installed=false");
 }
}
