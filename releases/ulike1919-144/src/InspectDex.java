import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.formats.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
public class InspectDex {
 static String fmt(Instruction i,int addr){
  String s=i.getOpcode().name;
  if(i instanceof FiveRegisterInstruction r){int[] a={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};s+=" {";for(int k=0;k<r.getRegisterCount();k++)s+=(k>0?",":"")+"v"+a[k];s+="}";}
  else if(i instanceof RegisterRangeInstruction r)s+=" {v"+r.getStartRegister()+" .. v"+(r.getStartRegister()+r.getRegisterCount()-1)+"}";
  else if(i instanceof ThreeRegisterInstruction r)s+=" v"+r.getRegisterA()+",v"+r.getRegisterB()+",v"+r.getRegisterC();
  else if(i instanceof TwoRegisterInstruction r)s+=" v"+r.getRegisterA()+",v"+r.getRegisterB();
  else if(i instanceof OneRegisterInstruction r)s+=" v"+r.getRegisterA();
  if(i instanceof WideLiteralInstruction r)s+=" #"+r.getWideLiteral();
  if(i instanceof ReferenceInstruction r)s+=" "+DexFormatter.INSTANCE.getReference(r.getReference());
  if(i instanceof OffsetInstruction r)s+=" ->"+String.format("%04x",addr+r.getCodeOffset());
  if(i instanceof SwitchPayload r)for(SwitchElement e:r.getSwitchElements())s+=" ["+e.getKey()+":"+e.getOffset()+"]";
  if(i instanceof ArrayPayload r)s+=" "+r.getArrayElements();
  return s;
 }
 public static void main(String[] a)throws Exception{
  var d=DexFileFactory.loadDexFile(a[0],Opcodes.forApi(26));Path out=Path.of(a[1]);Files.createDirectories(out);
  List<String> idx=new ArrayList<>();
  for(ClassDef c:d.getClasses()){
   String type=c.getType();List<String> lines=new ArrayList<>();lines.add(type+" extends "+c.getSuperclass()+" implements "+c.getInterfaces()+" flags="+c.getAccessFlags());
   for(Field f:c.getFields())lines.add("FIELD "+DexFormatter.INSTANCE.getFieldDescriptor(f)+" flags="+f.getAccessFlags()+" initial="+f.getInitialValue());
   for(Method m:c.getMethods()){
    String id=DexFormatter.INSTANCE.getMethodDescriptor(m);idx.add(id);lines.add("\nMETHOD "+id+" flags="+m.getAccessFlags());var imp=m.getImplementation();if(imp==null)continue;
    lines.add("registers="+imp.getRegisterCount());int addr=0,index=0;for(Instruction i:imp.getInstructions()){lines.add(String.format("%04x [%04d] ",addr,index++)+fmt(i,addr));addr+=i.getCodeUnits();}
    for(var t:imp.getTryBlocks()){String v="TRY "+String.format("%04x..%04x",t.getStartCodeAddress(),t.getStartCodeAddress()+t.getCodeUnitCount());for(var e:t.getExceptionHandlers())v+=" "+e.getExceptionType()+" ->"+String.format("%04x",e.getHandlerCodeAddress());lines.add(v);}
   }
   Path f=out.resolve(type.substring(1,type.length()-1)+".txt");Files.createDirectories(f.getParent());Files.write(f,lines);
  }
  Files.write(out.resolve("_methods.txt"),idx);System.out.println("Classes="+d.getClasses().size()+" methods="+idx.size());
 }
}
