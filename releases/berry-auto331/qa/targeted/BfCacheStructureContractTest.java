package app.hiro.berrybfcache.patches;
import java.io.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
/** Negative tests on real input methods: ambiguous anchors and changed loading-state semantics must fail. */
public final class BfCacheStructureContractTest {
  static Map<String,ClassDef> original=new LinkedHashMap<>();
  static void check(Map<String,ClassDef> c){BfCacheStructure.discover(c,"Lc70;","Lc41;","Ltt5;","Lcx;");}
  static void rejected(Map<String,ClassDef> c,String label){try{check(c);throw new AssertionError("Accepted "+label);}catch(IllegalStateException expected){System.out.println("PASS rejected "+label+": "+expected.getMessage());}}
  static ClassDef replacing(ClassDef c,List<Method> methods){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);}
  public static void main(String[]a)throws Exception {
    MultiDexContainer<? extends DexFile>d=DexFileFactory.loadDexContainer(new File(a[0]),Opcodes.forApi(26));
    for(String n:d.getDexEntryNames())for(ClassDef c:d.getEntry(n).getDexFile().getClasses())original.put(c.getType(),c);
    check(original);System.out.println("PASS known input discovered structurally");
    Map<String,ClassDef> c=new LinkedHashMap<>(original);ClassDef owner=c.get("Lc41;");List<Method> methods=new ArrayList<>();Method listener=null;
    for(Method m:owner.getMethods()){methods.add(m);if(m.getName().equals("w0"))listener=m;}
    if(listener==null)throw new AssertionError();
    methods.add(new ImmutableMethod(listener.getDefiningClass(),"duplicateListenerCandidate",listener.getParameters(),listener.getReturnType(),listener.getAccessFlags(),listener.getAnnotations(),listener.getHiddenApiRestrictions(),listener.getImplementation()));
    c.put(owner.getType(),replacing(owner,methods));rejected(c,"ambiguous listener anchor");
    c=new LinkedHashMap<>(original);owner=c.get("Lc70;");methods=new ArrayList<>();int changes=0;
    for(Method m:owner.getMethods()) {
      if(!m.getName().equals("onPageFinished")){methods.add(m);continue;}
      MethodImplementation impl=m.getImplementation();List<Instruction> ins=new ArrayList<>();
      for(Instruction i:impl.getInstructions()) {
        if(i.getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)i).getRegisterA()==1&&((NarrowLiteralInstruction)i).getNarrowLiteral()==1){ins.add(new ImmutableInstruction11n(Opcode.CONST_4,1,2));changes++;}else ins.add(i);
      }
      methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),new ImmutableMethodImplementation(impl.getRegisterCount(),ins,impl.getTryBlocks(),impl.getDebugItems())));
    }
    if(changes!=1)throw new AssertionError("Expected one state literal, got "+changes);
    c.put(owner.getType(),replacing(owner,methods));rejected(c,"completion state changed from 1 to 2");
  }
}
