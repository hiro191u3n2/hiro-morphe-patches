import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

public final class VerifyRoutes {
    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
    static String ref(Instruction i) {return i instanceof ReferenceInstruction ? ((ReferenceInstruction)i).getReference().toString() : "";}
    static final String TARGET = "Lcom/x/navigation/bm;";
    static final String CTOR = TARGET+"-><init>(JZLcom/x/navigation/hm;I)V";
    static final String LIST = "Lkotlin/collections/CollectionsKt;->listOf(Ljava/lang/Object;)Ljava/util/List;";
    static long[] IDS = {1L, 4294967297L, 2012345678901234567L, Long.MAX_VALUE};
    static Method method(ClassDef owner,String name,String signature) {
        for(Method m:owner.getMethods()) if(m.toString().equals(owner.getType()+"->"+name+signature)) return m;
        throw new AssertionError(owner.getType()+name+signature);
    }
    static List<Instruction> code(Method m) {
        List<Instruction> out = new ArrayList<>();
        for(Instruction i:m.getImplementation().getInstructions()) out.add(i);
        return out;
    }
    static int unique(List<Instruction> code,String marker) {
        int at=-1;
        for(int i=0;i<code.size();i++) if(marker.equals(ref(code.get(i)))) {check(at==-1,marker);at=i;}
        check(at>=0,marker);return at;
    }
    static int a(Instruction i) {return ((OneRegisterInstruction)i).getRegisterA();}
    static void verify(Method m,String unbox) {
        List<Instruction> c=code(m);
        int ctor=unique(c,CTOR),start=ctor-5;
        check(start>3 && c.get(start-1).getOpcode()==Opcode.MOVE_RESULT_WIDE && unbox.equals(ref(c.get(start-2))),"unbox");
        boolean key=false,parse=false;
        for(int i=Math.max(0,start-18);i<start-2;i++) {
            key |= "postId".equals(ref(c.get(i)));
            parse |= key && ref(c.get(i)).equals("Lkotlin/text/j;->O(ILjava/lang/String;)Ljava/lang/Long;");
        }
        check(key && parse, "decimal post ID source");
        Opcode[] expected = {Opcode.MOVE_WIDE_FROM16, Opcode.NEW_INSTANCE, Opcode.CONST_4, Opcode.CONST_4,
            Opcode.CONST_4, Opcode.INVOKE_DIRECT_RANGE, Opcode.INVOKE_STATIC, Opcode.MOVE_RESULT_OBJECT, Opcode.RETURN_OBJECT};
        for(int i=0;i<expected.length;i++) check(c.get(start+i).getOpcode()==expected[i],"instruction "+i);
        for(long id:IDS) {
            // Evaluate the actual contiguous instructions and operands from the rebuilt APK.
            Object[] regs=new Object[m.getImplementation().getRegisterCount()];
            regs[a(c.get(start-1))]=id;
            Instruction move=c.get(start);regs[a(move)]=regs[((TwoRegisterInstruction)move).getRegisterB()];
            Instruction make=c.get(start+1);check(TARGET.equals(ref(make)),"destination");
            regs[a(make)]=TARGET;
            for(int i=2;i<=4;i++) regs[a(c.get(start+i))]=((NarrowLiteralInstruction)c.get(start+i)).getNarrowLiteral();
            RegisterRangeInstruction init=(RegisterRangeInstruction)c.get(ctor);
            int first=init.getStartRegister();
            check(init.getRegisterCount()==6 && TARGET.equals(regs[first]) && Long.valueOf(id).equals(regs[first+1]),"64-bit ID");
            check(Integer.valueOf(0).equals(regs[first+3]) && Integer.valueOf(0).equals(regs[first+4])
                && Integer.valueOf(4).equals(regs[first+5]),"native default PostIntentAction");
            check(LIST.equals(ref(c.get(ctor+1))) && ((FiveRegisterInstruction)c.get(ctor+1)).getRegisterCount()==1
                && ((FiveRegisterInstruction)c.get(ctor+1)).getRegisterC()==first,"singleton destination");
            check(a(c.get(ctor+2))==a(c.get(ctor+3)),"return before immersive navigation");
        }
    }
    public static void main(String[] args)throws Exception {
        if (args.length > 1) IDS = new long[] {Long.parseLong(args[1])};
        Set<String> wanted=new HashSet<>(Arrays.asList("Lcom/x/video/tab/deeplink/c;","Lcom/x/calling/deeplink/a;",
            "Lcom/x/postdetail/deeplink/a;",TARGET));
        Map<String,ClassDef> classes=new HashMap<>();
        var dex=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());
        for(var name:dex.getDexEntryNames()) for(ClassDef c:dex.getEntry(name).getDexFile().getClasses())
            if(wanted.contains(c.getType())) classes.put(c.getType(),c);
        check(classes.keySet().equals(wanted),"missing target class");
        var tab=classes.get("Lcom/x/video/tab/deeplink/c;");
        var detail=classes.get("Lcom/x/postdetail/deeplink/a;");
        for(String status:new String[]{"status","statuses"}) {
            unique(code(method(tab,"b","(Landroid/net/Uri;)Ljava/util/List;")),"/{handle}/"+status+"/{postId}/video/{number}");
            unique(code(method(detail,"<clinit>","()V")),"/{handle}/"+status+"/{postId}/video/1");
        }
        var tabMethod=method(tab,"d","(Lcom/x/deeplink/route/b;Landroid/net/Uri;)Ljava/util/List;");
        unique(code(tabMethod),"number");unique(code(tabMethod),"1");
        verify(tabMethod,"Ljava/lang/Long;->longValue()J");
        verify(method(classes.get("Lcom/x/calling/deeplink/a;"),"a","(Lcom/x/deeplink/route/b;)Ljava/util/List;"),"Ljava/lang/Number;->longValue()J");
        var ctor=method(classes.get(TARGET),"<init>","(JZLcom/x/navigation/hm;I)V");
        unique(code(ctor),"Lcom/x/navigation/hm;->None:Lcom/x/navigation/hm;");
        unique(code(ctor),TARGET+"->b:J");
        System.out.println("PASS two native first-video routes, both spellings, " + IDS.length + " 64-bit IDs per route");
    }
}
