import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
public class ConstStrings {
 public static void main(String[] args)throws Exception {
  for(String arg:args) {
   DexFile dex=DexFileFactory.loadDexFile(new File(arg),Opcodes.getDefault());
   for(ClassDef c:dex.getClasses()) for(Method m:c.getMethods()) {
    if(c.getType().equals("Lapp/morphe/extension/shared/ResourceUtils;")&&m.getName().equals("getStringOrThrow"))System.err.println(m+" flags="+m.getAccessFlags()); if(m.getImplementation()==null)continue;
    LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();
    for(Instruction i:m.getImplementation().getInstructions())
     if((i.getOpcode()==Opcode.CONST_STRING||i.getOpcode()==Opcode.CONST_STRING_JUMBO)&&i instanceof ReferenceInstruction&&((ReferenceInstruction)i).getReference() instanceof StringReference) {
      String s=((StringReference)((ReferenceInstruction)i).getReference()).getString();counts.merge(s,1,Integer::sum);
     }
    for(Map.Entry<String,Integer> e:counts.entrySet())System.out.println(arg+"\t"+c.getType()+"\t"+m.getName()+"\t"+String.join("|",m.getParameterTypes())+"\t"+m.getReturnType()+"\t"+e.getValue()+"\t"+Base64.getEncoder().encodeToString(e.getKey().getBytes(StandardCharsets.UTF_8)));
   }
  }
 }
}
