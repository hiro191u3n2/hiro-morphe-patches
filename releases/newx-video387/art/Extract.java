import java.io.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.immutable.*;import com.android.tools.smali.dexlib2.immutable.instruction.*;import com.android.tools.smali.dexlib2.writer.pool.DexPool;
class Extract {
 static List<Instruction> slice(Method m){
    List<Instruction> all=VerifyRoutes.code(m);
    int ctor=VerifyRoutes.unique(all,VerifyRoutes.CTOR),end=ctor+4;
    int start=ctor;
    while(!VerifyRoutes.ref(all.get(start)).equals("postId")) start--;
    var selected = new ArrayList<>(all.subList(start,end)); int units=selected.stream().mapToInt(Instruction::getCodeUnits).sum();
    List<Instruction> code=new ArrayList<>();
    int mapReg=((FiveRegisterInstruction)selected.get(1)).getRegisterC();
    code.add(new ImmutableInstruction22x(Opcode.MOVE_OBJECT_FROM16,mapReg,15));
    int cursor=0;
    for(Instruction i:selected) {
        if(i.getOpcode()==Opcode.IF_EQZ){
            // Both original exits on an invalid ID return null. Relocate just those offsets.
            code.add(new ImmutableInstruction21t(i.getOpcode(),VerifyRoutes.a(i),units-cursor));
        } else {
            if(i instanceof OffsetInstruction)throw new AssertionError("unexpected control flow");
            code.add(i);
        }
        cursor+=i.getCodeUnits();
    }
    code.add(new ImmutableInstruction11n(Opcode.CONST_4,0,0));code.add(new ImmutableInstruction11x(Opcode.RETURN_OBJECT,0));return code;
 }
 public static void main(String[] args)throws Exception{
    var ops=Opcodes.getDefault();var zip=DexFileFactory.loadDexContainer(new File(args[0]),ops);
    Map<String,ClassDef> real=new HashMap<>();
    for(String n:zip.getDexEntryNames())for(ClassDef c:zip.getEntry(n).getDexFile().getClasses())
        if(Arrays.asList("Lcom/x/video/tab/deeplink/c;","Lcom/x/calling/deeplink/a;",VerifyRoutes.TARGET).contains(c.getType()))real.put(c.getType(),c);
    Map<String,Method> methods=Map.of("tab",VerifyRoutes.method(real.get("Lcom/x/video/tab/deeplink/c;"),"d","(Lcom/x/deeplink/route/b;Landroid/net/Uri;)Ljava/util/List;"),
            "detail",VerifyRoutes.method(real.get("Lcom/x/calling/deeplink/a;"),"a","(Lcom/x/deeplink/route/b;)Ljava/util/List;"));
    List<ClassDef> out=new ArrayList<>();
    for(ClassDef c:DexFileFactory.loadDexFile(args[1],ops).getClasses()){
        List<Method> defs=new ArrayList<>();
        for(Method m:c.getMethods()){
            MethodImplementation code=m.getImplementation();
            if(c.getType().equals("Lhiro/Probe;") && methods.containsKey(m.getName()))code=new ImmutableMethodImplementation(16,slice(methods.get(m.getName())),List.of(),List.of());
            if(c.getType().equals(VerifyRoutes.TARGET)&&m.getName().equals("<init>"))code=VerifyRoutes.method(real.get(VerifyRoutes.TARGET),"<init>","(JZLcom/x/navigation/hm;I)V").getImplementation();
            defs.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),code));
        }
        out.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),defs));
    }
    DexPool.writeTo(args[2],new ImmutableDexFile(ops,out));
 }
}
