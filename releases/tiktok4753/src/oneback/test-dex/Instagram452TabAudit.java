import java.io.*;import java.nio.file.*;import java.util.*;
import app.hiro.oneback.patches.*;import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.builder.*;import com.android.tools.smali.dexlib2.builder.instruction.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.immutable.reference.*;import com.android.tools.smali.dexlib2.writer.io.*;import com.android.tools.smali.dexlib2.writer.pool.*;
/** Static audit against a locally supplied 452 APK; no APK bytes are embedded. */
public class Instagram452TabAudit {
 static int checks;
 static Map<String,ClassDef> read(String path)throws Exception {
  Map<String,ClassDef> out=new TreeMap<>();
  Set<String> wanted=new HashSet<>(Arrays.asList(Instagram452Tab.NAVIGATION,Instagram452Tab.WRAPPER,Instagram452Tab.TAB,"LX/004L;"));
  MultiDexContainer<? extends DexFile> container=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
  for(String entry:container.getDexEntryNames())for(ClassDef c:container.getEntry(entry).getDexFile().getClasses())
   if(wanted.contains(c.getType()))out.put(c.getType(),c);
  return out;
 }

 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static Method find(ClassDef c,String n){for(Method m:c.getMethods())if(m.getName().equals(n)&&m.getParameterTypes().isEmpty())return m;throw new IllegalArgumentException(n);}
 static List<Instruction> ins(Method m){List<Instruction>b=new ArrayList<>();for(Instruction i:m.getImplementation().getInstructions())b.add(i);return b;}
 static int target(List<Instruction>b,int n){int[]p=new int[b.size()+1];for(int i=0;i<b.size();i++)p[i+1]=p[i]+b.get(i).getCodeUnits();int dst=p[n]+((OffsetInstruction)b.get(n)).getCodeOffset();for(int i=0;i<p.length;i++)if(p[i]==dst)return i;throw new AssertionError("branch target");}
 static String ref(Instruction i){return ((ReferenceInstruction)i).getReference().toString();}
 static void reject(Map<String,ClassDef>b,String why){int[]calls={0};boolean rejected=false;try{Instagram452Tab.apply(b,t->{calls[0]++;return new MutableClass(b.get(t));},new HashMap<>());}catch(IllegalStateException expected){rejected=true;}check(rejected&&calls[0]==0,why);}
 public static void main(String[]a)throws Exception {
  Map<String,ClassDef>original=read(a[0]);Map<String,MutableClass>edits=new TreeMap<>();Map<String,Integer>counts=new TreeMap<>();
  Instagram452Tab.apply(original,t->edits.computeIfAbsent(t,k->new MutableClass(original.get(k))),counts);
  check(edits.keySet().equals(Collections.singleton(Instagram452Tab.WRAPPER)),"only wrapper edited");
  check(counts.equals(Collections.singletonMap("instagram_native_tab_back",1)),"one hook reported");
  MemoryDataStore store=new MemoryDataStore();DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),edits.values()));Files.write(Paths.get(a[1]),Arrays.copyOf(store.getBuffer(),store.getSize()));store.close();
  ClassDef c=read(a[1]).get(Instagram452Tab.WRAPPER);Method m=find(c,"A0T");List<Instruction>b=ins(m);
  check(b.get(163).getOpcode()==Opcode.IGET_OBJECT&&ref(b.get(163)).equals("LX/004K;->A03:Landroidx/fragment/app/FragmentActivity;"),"owner read");
  TwoRegisterInstruction r=(TwoRegisterInstruction)b.get(163);check(r.getRegisterA()==0&&r.getRegisterB()==4,"owner registers");
  check(b.get(164).getOpcode()==Opcode.INVOKE_STATIC&&ref(b.get(164)).equals("Lapp/hiro/oneback/runtime/OneBackExit;->tryInstagramTabBack(Landroid/app/Activity;Ljava/lang/Object;Ljava/lang/Object;)Z"),"hook call");
  FiveRegisterInstruction call=(FiveRegisterInstruction)b.get(164);check(call.getRegisterCount()==3&&call.getRegisterC()==0&&call.getRegisterD()==4&&call.getRegisterE()==1,"hook args");
  check(b.get(165).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)b.get(165)).getRegisterA()==0,"dead result register");
  check(b.get(166).getOpcode()==Opcode.IF_NEZ&&((OneRegisterInstruction)b.get(166)).getRegisterA()==0&&target(b,166)==235,"handled target");
  check(ref(b.get(167)).equals("Ljava/util/Stack;->pop()Ljava/lang/Object;"),"false continuation native pop");
  check(target(b,162)==183,"single history keeps native branch");
  check(ref(b.get(235)).equals("Lcom/facebook/systrace/Systrace;->A0E()Z"),"true preserves trace epilogue");
  check(b.get(240).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)b.get(240)).getRegisterA()==12,"true preserves handled register");
  MutableMethodImplementation restore=new MutableMethodImplementation(m.getImplementation());for(int i=0;i<4;i++)restore.removeInstruction(163);
  Method restored=new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),restore);
  check(OneBackPatch.instagramNavigationDigest(restored).equals(Instagram452Tab.BODY_SHA),"complete original body+try endpoints restored");
  int untouched=0;for(Method after:c.getMethods())if(!after.getName().equals("A0T")){Method before=null;for(Method o:original.get(Instagram452Tab.WRAPPER).getMethods())if(o.toString().equals(after.toString()))before=o;check(before!=null,"method signature");if(before.getImplementation()!=null)check(OneBackPatch.instagramNavigationDigest(before).equals(OneBackPatch.instagramNavigationDigest(after)),"unrelated body");untouched++;}
  Map<String,ClassDef>bad=new HashMap<>(original);MutableClass changed=new MutableClass(original.get(Instagram452Tab.WRAPPER));MutableMethod bm=(MutableMethod)find(changed,"A0T");bm.getImplementation().replaceInstruction(162,new BuilderInstruction22t(Opcode.IF_LE,1,5,bm.getImplementation().newLabelForIndex(179)));bad.put(changed.getType(),changed);reject(bad,"changed branch rejected before mutation");
  bad=new HashMap<>(original);changed=new MutableClass(original.get(Instagram452Tab.NAVIGATION));for(MutableField f:changed.getFields())if(f.getName().equals("A03"))f.setAccessFlags(1);bad.put(changed.getType(),changed);reject(bad,"changed owner rejected before mutation");
  bad=new HashMap<>(original);changed=new MutableClass(original.get(Instagram452Tab.TAB));bm=(MutableMethod)find(changed,"<clinit>");for(int i=0;i<bm.getImplementation().getInstructions().size();i++){Instruction op=bm.getImplementation().getInstructions().get(i);if(op instanceof ReferenceInstruction&&((ReferenceInstruction)op).getReference() instanceof StringReference&&"CLIPS".equals(((StringReference)((ReferenceInstruction)op).getReference()).getString())){bm.getImplementation().replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)op).getRegisterA(),new ImmutableStringReference("CHANGED")));break;}}bad.put(changed.getType(),changed);reject(bad,"changed enum rejected before mutation");
  System.out.println("PASS Instagram452TabAudit: "+checks+" assertions; untouched methods="+untouched+"; exact native body+try restored; rejected branch, owner, enum mutations. Static only.");
 }
}
