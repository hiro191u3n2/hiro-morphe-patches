import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import app.hiro.oneback.patches.*;

/** Verify the current published 125 upgrade and native-contract rejection on
 * real APK classes. Older 123/124 models use their exact distributed runtimes
 * and the independently verified, unchanged native tab/dispatcher contracts. */
public final class Instagram126UpgradeAudit {
  static final String PREFIX = "Lapp/hiro/oneback/runtime/";
  static int checks;
  static void check(boolean ok,String why) { checks++; if(!ok)throw new AssertionError(why); }
  static Map<String,ClassDef> merged(Map<String,ClassDef> input,Map<String,ClassDef> replacement) {
    Map<String,ClassDef> result=new TreeMap<String,ClassDef>(input);
    for(ClassDef c:replacement.values())if(!result.containsKey(c.getType()))result.put(c.getType(),c);
    return result;
  }
  static void verifyReplacement(Map<String,ClassDef> input,Map<String,ClassDef> replacement,String version,String label)throws Exception {
    InstagramUpgradeAudit.Edit editor=new InstagramUpgradeAudit.Edit(input);
    check(version.equals(InstagramUpgrade.applyVersionIfKnown("com.instagram.android",input,editor)),"exact version "+label);
    check(editor.changed.keySet().equals(replacement.keySet()),"complete runtime update "+label);
    for(ClassDef c:replacement.values())check(Arrays.equals(InstagramUpgradeAudit.canonical(c),InstagramUpgradeAudit.canonical(editor.changed.get(c.getType()))),"exact replacement "+label+" "+c.getType());
    System.out.println("PASS "+label+" -> 1.1.16; all native methods retained");
  }
  static void changeRuntimeString(Map<String,ClassDef> input) {
    for(ClassDef original:input.values()) {
      if(!original.getType().startsWith(PREFIX))continue;
      MutableClass changed=new MutableClass(original);
      for(MutableMethod method:changed.getMethods()) {
        if(method.getImplementation()==null)continue;
        for(int n=0;n<method.getImplementation().getInstructions().size();n++) {
          Instruction instruction=method.getImplementation().getInstructions().get(n);
          if(instruction.getOpcode()!=Opcode.CONST_STRING_JUMBO)continue;
          String text=((StringReference)((ReferenceInstruction)instruction).getReference()).getString();
          method.getImplementation().replaceInstruction(n,new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,
              ((OneRegisterInstruction)instruction).getRegisterA(),new ImmutableStringReference(text+":unreviewed")));
          input.put(changed.getType(),changed);return;
        }
      }
    }
    throw new AssertionError("actual125 input must contain a jumbo string to mutate");
  }
  static void changeRuntimeBranch(Map<String,ClassDef> input) {
    for(ClassDef original:input.values()) {
      if(!original.getType().startsWith(PREFIX))continue;
      MutableClass changed=new MutableClass(original);
      for(MutableMethod method:changed.getMethods()) {
        if(method.getImplementation()==null)continue;
        for(int n=0;n<method.getImplementation().getInstructions().size();n++) {
          Instruction instruction=method.getImplementation().getInstructions().get(n);
          if(instruction.getOpcode()!=Opcode.IF_EQZ&&instruction.getOpcode()!=Opcode.IF_NEZ)continue;
          BuilderInstruction21t branch=(BuilderInstruction21t)instruction;
          method.getImplementation().replaceInstruction(n,new BuilderInstruction21t(
              instruction.getOpcode()==Opcode.IF_EQZ?Opcode.IF_NEZ:Opcode.IF_EQZ,branch.getRegisterA(),branch.getTarget()));
          input.put(changed.getType(),changed);return;
        }
      }
    }
    throw new AssertionError("actual125 input must contain a conditional branch to mutate");
  }
  static Map<String,ClassDef> model(Map<String,ClassDef> native125,String version,
                                   Map<String,ClassDef> replacement)throws Exception {
    Map<String,ClassDef> result=new TreeMap<String,ClassDef>(native125);
    for(String name:new ArrayList<String>(result.keySet()))if(name.startsWith(PREFIX))result.remove(name);
    try(InputStream in=Instagram126UpgradeAudit.class.getResourceAsStream("/upgrades/oneback-runtime"+version+".dex")) {
      check(in!=null,"pinned runtime resource "+version);
      for(ClassDef c:DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new BufferedInputStream(in)).getClasses())result.put(c.getType(),c);
    }
    for(ClassDef c:replacement.values())if(!result.containsKey(c.getType()))result.put(c.getType(),c);
    return result;
  }
  public static void main(String[] args)throws Exception {
    if(args.length!=2)throw new IllegalArgumentException("current125.apk replacement-runtime.dex");
    Map<String,ClassDef> input=InstagramUpgradeAudit.classes(args[0]),replacement=InstagramUpgradeAudit.classes(args[1]);
    OneBackPatch.verifyInstagramReelsBoundary(input);
    OneBackPatch.verifyInstagramDispatcher(input);
    OneBackPatch.verifyInstagramPatchedTabBack(input);
    // Exercise the actual DEX serialized into the rebuilt 125 APK, including
    // string-index width changes. Replacing it with the pinned DEX would mask
    // the real-world upgrade regression this test is intended to prevent.
    int jumbo=0;
    for(ClassDef c:input.values())if(c.getType().startsWith(PREFIX))for(Method m:c.getMethods())
      if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())if(i.getOpcode()==Opcode.CONST_STRING_JUMBO)jumbo++;
    check(jumbo>0,"actual125 APK must exercise jumbo string encoding");
    verifyReplacement(merged(input,replacement),replacement,"1.1.15","actual rebuilt 125 APK ("+jumbo+" jumbo strings)");
    for(String[] row:new String[][]{{"123","1.1.13"},{"124","1.1.14"},{"125","1.1.15"}}) {
      Map<String,ClassDef> candidate=model(input,row[0],replacement);
      verifyReplacement(candidate,replacement,row[1],"exact reference runtime "+row[1]);
    }
    Map<String,ClassDef> mutated=model(input,"125",replacement);
    MutableClass navigation=new MutableClass(mutated.get("LX/2xZ;"));
    MutableMethod method=(MutableMethod)InstagramDispatcherAudit.find(navigation,"A0V");
    method.getImplementation().replaceInstruction(165,new BuilderInstruction10x(Opcode.NOP));
    mutated.put(navigation.getType(),navigation);
    InstagramUpgradeAudit.reject(mutated,"modified 125 native tab bridge");checks++;
    mutated=model(input,"125",replacement);
    MutableClass changed=new MutableClass(mutated.get(OneBackPatch.RUNTIME));changed.setSourceFile("unreviewed-runtime.java");mutated.put(changed.getType(),changed);
    InstagramUpgradeAudit.reject(mutated,"modified 125 runtime");checks++;
    mutated=merged(input,replacement);changeRuntimeString(mutated);
    InstagramUpgradeAudit.reject(mutated,"modified actual125 jumbo string value");checks++;
    mutated=merged(input,replacement);changeRuntimeBranch(mutated);
    InstagramUpgradeAudit.reject(mutated,"modified actual125 conditional branch");checks++;
    System.out.println("PASS Instagram126UpgradeAudit: actual rebuilt125 APK and 3 prior runtime contracts; "+checks+" assertions; modified string/branch/runtime metadata/native bridge rejected before mutation; no physical-device claim");
  }
}
