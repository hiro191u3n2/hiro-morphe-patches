import java.util.*;
import java.util.regex.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue;

/** Bind all current diagnostic producers, including nested-class literals, to the MPP. */
public final class VerifyRuntimeVersion19101 {
 static final String OWNER="Lcom/hiro/ulike/ProcessingTiming1947;";
 static final Set<String> ROOTS=Set.of(OWNER,"Lcom/hiro/ulike/VerificationJournal19101;","Lcom/hiro/ulike/CameraTrace1965;");
 static final Pattern VERSION_LITERAL=Pattern.compile("(?<![0-9.])1\\.9\\.[0-9]+(?![0-9.])");
 static boolean diagnosticFamily(String type){
  for(String root:ROOTS)if(type.equals(root)||type.startsWith(root.substring(0,root.length()-1)+"$"))return true;
  return false;
 }
 static void verifyLiteral(String value,String expected,String owner,String method){
  // One old persisted timing-summary format is deliberately readable. This is
  // not permission for stale current headers, field values, nested classes or exporters.
  if(owner.equals(OWNER)&&method.equals("init")&&value.equals("1.9.100"))return;
  Matcher matcher=VERSION_LITERAL.matcher(value);
  while(matcher.find())MergePayloads.require(matcher.group().equals(expected),
      "Stale diagnostic runtime literal in "+owner+"->"+method+": "+value);
 }
 static String verify(Map<String,ClassDef> classes,String expected){
  MergePayloads.require(expected.matches("1\\.9\\.[0-9]+"),"Invalid expected runtime version");
  for(String root:ROOTS){
   ClassDef c=classes.get(root);MergePayloads.require(c!=null,"Diagnostic class missing: "+root);
   int hits=0;
   for(Field f:c.getFields())if(f.getName().equals("VERSION")){
    int mask=root.equals(OWNER)?0x19:0x18;
    MergePayloads.require(f.getType().equals("Ljava/lang/String;")&&(f.getAccessFlags()&mask)==mask,
       "Diagnostic VERSION must remain static final String: "+root);
    MergePayloads.require(f.getInitialValue() instanceof StringEncodedValue v&&expected.equals(v.getValue()),
       "Diagnostic VERSION differs from declared MPP release: "+root);hits++;
   }
   MergePayloads.require(hits==1,"Exactly one diagnostic VERSION required: "+root);
  }
  for(ClassDef c:classes.values())if(diagnosticFamily(c.getType())){
   for(Field f:c.getFields())if(f.getInitialValue() instanceof StringEncodedValue value)
    verifyLiteral(value.getValue(),expected,c.getType(),"field:"+f.getName());
   for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())
    if(i instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s)
     verifyLiteral(s.getString(),expected,c.getType(),m.getName());
  }
  return expected;
 }
 public static void main(String[] a)throws Exception{
  MergePayloads.require(a.length==2,"DEX_OR_APK EXPECTED_VERSION");
  System.out.println("PASS runtime release version: "+verify(MergePayloads.classes(a[0]),a[1]));
 }
}
