package app.hiro.berrybfcache.patches;
import java.io.*;import java.util.*;import java.util.function.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.immutable.instruction.*;
public final class NativeCompletionGuardTest {
 static Map<String,ClassDef>original=new LinkedHashMap<>();static BfCacheRepairPatch.Profile profile;
 static Method finish(Map<String,ClassDef>c){for(Method m:c.get("Lc70;").getMethods())if(m.getName().equals("onPageFinished"))return m;throw new AssertionError();}
 static void reject(Map<String,ClassDef>c,String name){try{NativeCompletionStructure.discover(c,profile,finish(c));throw new AssertionError("Accepted "+name);}catch(IllegalStateException ok){System.out.println("PASS rejected "+name);}}
 static Map<String,ClassDef>mutate(String type,String name,BiFunction<Integer,Instruction,Instruction>change){Map<String,ClassDef>c=new LinkedHashMap<>(original);ClassDef owner=c.get(type);List<Method>ms=new ArrayList<>();int changes=0;for(Method m:owner.getMethods()){if(!m.getName().equals(name)){ms.add(m);continue;}MethodImplementation impl=m.getImplementation();List<Instruction>is=new ArrayList<>();int j=0;for(Instruction i:impl.getInstructions()){Instruction n=change.apply(j++,i);if(n!=i)changes++;is.add(n);}ms.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),new ImmutableMethodImplementation(impl.getRegisterCount(),is,impl.getTryBlocks(),impl.getDebugItems())));}if(changes!=1)throw new AssertionError("Expected one mutation, got "+changes);c.put(type,new ImmutableClassDef(owner.getType(),owner.getAccessFlags(),owner.getSuperclass(),owner.getInterfaces(),owner.getSourceFile(),owner.getAnnotations(),owner.getFields(),ms));return c;}
 public static void main(String[]a)throws Exception{
  MultiDexContainer<? extends DexFile>d=DexFileFactory.loadDexContainer(new File(a[0]),Opcodes.forApi(37));for(String n:d.getDexEntryNames())for(ClassDef c:d.getEntry(n).getDexFile().getClasses())original.put(c.getType(),c);
  profile=BfCacheStructure.discover(original,"Lc70;","Lc41;","Ltt5;","Lcx;");NativeCompletionStructure.discover(original,profile,finish(original));System.out.println("PASS positive original");
  reject(mutate("Lc41;","E",(j,i)->j==1?new ImmutableInstruction21t(Opcode.IF_EQZ,((OneRegisterInstruction)i).getRegisterA(),0):i),"refresh branch does not skip to return");
  reject(mutate("Lc41;","E",(j,i)->j==3?new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL,2,0,0,0,0,0,((ReferenceInstruction)i).getReference()):i),"refresh receiver differs from loaded refresh view");
  reject(mutate("La70;","onProgressChanged",(j,i)->j==95?new ImmutableInstruction22c(Opcode.IPUT,((TwoRegisterInstruction)i).getRegisterA(),1,((ReferenceInstruction)i).getReference()):i),"progress write is not to client-owned tab");
  reject(mutate("Lc70;","onPageFinished",(j,i)->j==177?new ImmutableInstruction21t(Opcode.IF_NEZ,((OneRegisterInstruction)i).getRegisterA(),((OffsetInstruction)i).getCodeOffset()):i),"visible-tab branch inverted");
 }
}
