import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue;

/** Bind the displayed timing version to the exact declared MPP release. */
public final class VerifyRuntimeVersion19101 {
 static final String OWNER="Lcom/hiro/ulike/ProcessingTiming1947;";
 static String verify(Map<String,ClassDef> classes,String expected){
  MergePayloads.require(expected.matches("[0-9]+\\.[0-9]+\\.[0-9]+"),"Invalid expected runtime version");
  ClassDef c=classes.get(OWNER);MergePayloads.require(c!=null,"Timing class missing");
  int hits=0;
  for(Field f:c.getFields())if(f.getName().equals("VERSION")){
   MergePayloads.require(f.getType().equals("Ljava/lang/String;")&&(f.getAccessFlags()&0x19)==0x19,
      "Timing VERSION must remain public static final String");
   MergePayloads.require(f.getInitialValue() instanceof StringEncodedValue v&&expected.equals(v.getValue()),
      "Timing VERSION differs from declared MPP release");hits++;
  }
  MergePayloads.require(hits==1,"Exactly one timing VERSION required");
  for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())
   if(i instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s){
    String value=s.getString();if(value.startsWith("ULike v")&&value.length()>7&&Character.isDigit(value.charAt(7)))
     MergePayloads.require(value.startsWith("ULike v"+expected+" /"),"Stale inlined timing header: "+value);
   }
  return expected;
 }
 public static void main(String[] a)throws Exception{
  MergePayloads.require(a.length==2,"DEX_OR_APK EXPECTED_VERSION");
  System.out.println("PASS runtime release version: "+verify(MergePayloads.classes(a[0]),a[1]));
 }
}
