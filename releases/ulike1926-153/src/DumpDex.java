import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import java.io.*;
public class DumpDex {
 public static void main(String[] a)throws Exception{
  var container=DexFileFactory.loadDexContainer(new File(a[0]),Opcodes.forApi(26));
  for(var en:container.getDexEntryNames())for(var c:container.getEntry(en).getDexFile().getClasses()){
   if(a.length<2){System.out.println(c.getType());continue;}
   if(!c.getType().matches(a[1]))continue;
   System.out.println("CLASS "+c.getType()+" extends "+c.getSuperclass()+" flags="+c.getAccessFlags());
   for(var f:c.getFields())System.out.println("FIELD "+f+" flags="+f.getAccessFlags()+" value="+f.getInitialValue());
   for(var m:c.getMethods()){
    if(a.length>2&&!m.getName().matches(a[2]))continue;
    System.out.println("\nMETHOD "+m+" flags="+m.getAccessFlags());var im=m.getImplementation();if(im==null)continue;System.out.println("REGISTERS "+im.getRegisterCount());
    int off=0;for(var i:im.getInstructions()){
     String s=String.format("%04x %s",off,i.getOpcode());
     if(i instanceof RegisterRangeInstruction r)s+=" v"+r.getStartRegister()+"..+"+r.getRegisterCount();
     else if(i instanceof FiveRegisterInstruction r)s+=" {"+r.getRegisterC()+","+r.getRegisterD()+","+r.getRegisterE()+","+r.getRegisterF()+","+r.getRegisterG()+"} n="+r.getRegisterCount();
     else if(i instanceof ThreeRegisterInstruction r)s+=" v"+r.getRegisterA()+",v"+r.getRegisterB()+",v"+r.getRegisterC();
     else if(i instanceof TwoRegisterInstruction r)s+=" v"+r.getRegisterA()+",v"+r.getRegisterB();
     else if(i instanceof OneRegisterInstruction r)s+=" v"+r.getRegisterA();
     if(i instanceof ReferenceInstruction r)s+=" "+DexFormatter.INSTANCE.getReference(r.getReference());
     if(i instanceof WideLiteralInstruction l)s+=" #"+l.getWideLiteral();
     if(i instanceof OffsetInstruction b)s+=" ->"+Integer.toHexString(off+b.getCodeOffset());
     if(i instanceof SwitchPayload p)for(var el:p.getSwitchElements())s+=" ["+el.getKey()+":"+el.getOffset()+"]";
     System.out.println(s);off+=i.getCodeUnits();
    }
    for(var t:im.getTryBlocks())System.out.println("TRY "+Integer.toHexString(t.getStartCodeAddress())+".."+Integer.toHexString(t.getStartCodeAddress()+t.getCodeUnitCount())+" "+t.getExceptionHandlers());
   }
  }
 }
}
