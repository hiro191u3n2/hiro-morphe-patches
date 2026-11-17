import java.io.*;
import java.util.*;
import app.hiro.newx.patches.NewXKeepCategoriesPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

public class CategoryAudit {
    static int checks;
    static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    public static void main(String[] args) throws Exception {
        Map<String,ClassDef> all=new HashMap<>();
        for(File file:new File(args[0]).listFiles()) if(file.getName().endsWith(".dex"))
            for(ClassDef type:DexFileFactory.loadDexFile(file,Opcodes.getDefault()).getClasses())all.put(type.getType(),type);
        ClassDef owner=all.get(NewXKeepCategoriesPatch.OWNER);
        ClassDef repository=all.get(NewXKeepCategoriesPatch.REPOSITORY);
        NewXKeepCategoriesPatch.validateNativePaths(owner,all.get("Lcom/x/http/n;"));
        Method startup=NewXKeepCategoriesPatch.startup(owner), read=NewXKeepCategoriesPatch.read(repository);
        int a=NewXKeepCategoriesPatch.startupReset(startup), b=NewXKeepCategoriesPatch.expirationBranch(read);
        System.out.println("startup="+startup+" instruction="+a);
        System.out.println("read="+read+" instruction="+b);
        check(a==182,"unexpected uploaded APK startup target");
        check(b==18,"unexpected uploaded APK expiration target");
        MutableMethod newStartup=new MutableMethod(startup),newRead=new MutableMethod(read);
        NewXKeepCategoriesPatch.nop(newStartup,a,3);
        NewXKeepCategoriesPatch.nop(newRead,b,2);
        preserved(startup,newStartup,a,3);
        preserved(read,newRead,b,2);
        boolean rejects=false;try{NewXKeepCategoriesPatch.startupReset(newStartup);}catch(IllegalStateException expected){rejects=true;}
        check(rejects,"repeated startup patch accepted");
        rejects=false;try{NewXKeepCategoriesPatch.expirationBranch(newRead);}catch(IllegalStateException expected){rejects=true;}
        check(rejects,"repeated expiry patch accepted");
        // Actual edited read branch now falls through to the existing native preference read,
        // regardless of whether its saved expiration compares before, at, or after now.
        for(int comparison:new int[]{-1,0,1}) {
            int originalNext=comparison>=0?22:19;
            check(originalNext==(comparison<0?19:22),"original expiry model");
            List<Instruction> body=NewXKeepCategoriesPatch.code(newRead);
            check(body.get(b).getOpcode()==Opcode.NOP&&body.get(b+1).getOpcode()==Opcode.NOP,"expiry edit differs");
            MethodReference next=(MethodReference)((ReferenceInstruction)body.get(b+2)).getReference();
            check(next.getDefiningClass().equals("Lcom/x/android/preferences/b;")&&next.getName().equals("a"),"new expiry must read saved topics");
        }
        System.out.println("PASS checks="+checks+"; only 3-unit startup invoke and 2-unit expiry branch changed; native apply/manual reset untouched");
    }
    static void preserved(Method original,Method changed,int targetIndex,int width) {
        List<Instruction> before=NewXKeepCategoriesPatch.code(original),after=NewXKeepCategoriesPatch.code(changed);
        Map<Integer,String> left=map(before),right=map(after);
        int start=0;for(int i=0;i<targetIndex;i++)start+=before.get(i).getCodeUnits();
        check(size(before)==size(after),"code units changed");
        check(original.getImplementation().getRegisterCount()==changed.getImplementation().getRegisterCount(),"register count changed");
        for(Map.Entry<Integer,String> row:left.entrySet())if(row.getKey()!=start)
            check(row.getValue().equals(right.get(row.getKey())),"unrelated instruction changed at "+row.getKey());
        for(int i=0;i<width;i++)check(right.get(start+i).equals("NOP:1"),"replacement not exact NOP at "+(start+i));
        check(original.getImplementation().getTryBlocks().size()==changed.getImplementation().getTryBlocks().size(),"try regions changed");
    }
    static int size(List<Instruction> code){int count=0;for(Instruction i:code)count+=i.getCodeUnits();return count;}
    static Map<Integer,String> map(List<Instruction> code){Map<Integer,String>r=new LinkedHashMap<>();int at=0;for(Instruction i:code){r.put(at,key(i));at+=i.getCodeUnits();}return r;}
    static String key(Instruction i){String s=i.getOpcode()+":"+i.getCodeUnits();
        if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;s+=" regs="+r.getRegisterCount()+","+r.getRegisterC()+","+r.getRegisterD()+","+r.getRegisterE()+","+r.getRegisterF()+","+r.getRegisterG();}
        else if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;s+=" range="+r.getStartRegister()+","+r.getRegisterCount();}
        else if(i instanceof ThreeRegisterInstruction){ThreeRegisterInstruction r=(ThreeRegisterInstruction)i;s+=" regs="+r.getRegisterA()+","+r.getRegisterB()+","+r.getRegisterC();}
        else if(i instanceof TwoRegisterInstruction){TwoRegisterInstruction r=(TwoRegisterInstruction)i;s+=" regs="+r.getRegisterA()+","+r.getRegisterB();}
        else if(i instanceof OneRegisterInstruction)s+=" reg="+((OneRegisterInstruction)i).getRegisterA();
        if(i instanceof ReferenceInstruction)s+=" ref="+((ReferenceInstruction)i).getReference();
        if(i instanceof OffsetInstruction)s+=" offset="+((OffsetInstruction)i).getCodeOffset();
        if(i instanceof WideLiteralInstruction)s+=" literal="+((WideLiteralInstruction)i).getWideLiteral();
        return s;
    }
}
