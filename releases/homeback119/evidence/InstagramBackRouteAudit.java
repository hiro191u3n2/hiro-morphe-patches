import java.io.*;import java.util.*;import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;
public class InstagramBackRouteAudit {
 public static void main(String[] args)throws Exception{
  MultiDexContainer<? extends DexFile> apk=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());Set<String> target=new HashSet<>(Arrays.asList(args).subList(2,args.length));
  for(String n:apk.getDexEntryNames())for(ClassDef c:apk.getEntry(n).getDexFile().getClasses()) {
   boolean cl=target.contains(c.getType());
   if(cl){System.out.println("CLASS "+n+" "+c.getType()+" flags="+c.getAccessFlags()+" EXTENDS "+c.getSuperclass()+" interfaces="+c.getInterfaces());for(Field f:c.getFields())System.out.println("FIELD "+f+" flags="+f.getAccessFlags());for(Method m:c.getMethods())System.out.println("DECLARED "+m+" flags="+m.getAccessFlags());}
   for(Method m:c.getMethods())if(m.getImplementation()!=null){boolean found=cl&&(args[1].equals("all")||m.getName().matches(args[1]));
    if(args[1].equals("calls"))for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)i).getReference();if(r instanceof MethodReference){MethodReference mr=(MethodReference)r;if(mr.getName().equals("onBackPressed")&&mr.getReturnType().equals("Z"))found=true;}}
    if(found){System.out.println("METHOD "+m);int pc=0;for(Instruction i:m.getImplementation().getInstructions()) {String d=i.getOpcode().name();if(i instanceof ReferenceInstruction)d+=" "+((ReferenceInstruction)i).getReference();if(i instanceof NarrowLiteralInstruction)d+=" #"+Integer.toHexString(((NarrowLiteralInstruction)i).getNarrowLiteral());if(i instanceof ThreeRegisterInstruction)d+=" v"+((ThreeRegisterInstruction)i).getRegisterA()+",v"+((ThreeRegisterInstruction)i).getRegisterB()+",v"+((ThreeRegisterInstruction)i).getRegisterC();else if(i instanceof TwoRegisterInstruction)d+=" v"+((TwoRegisterInstruction)i).getRegisterA()+",v"+((TwoRegisterInstruction)i).getRegisterB();else if(i instanceof OneRegisterInstruction)d+=" v"+((OneRegisterInstruction)i).getRegisterA();if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;d+=" {"+r.getRegisterC()+","+r.getRegisterD()+","+r.getRegisterE()+","+r.getRegisterF()+","+r.getRegisterG()+"}x"+r.getRegisterCount();}if(i instanceof OffsetInstruction)d+=" offset "+((OffsetInstruction)i).getCodeOffset();System.out.println(pc+" "+d);pc+=i.getCodeUnits();}}
   }
  }
 }
}
