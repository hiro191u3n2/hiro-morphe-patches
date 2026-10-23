import java.io.*;
import java.nio.file.*;
import java.util.*;
import app.hiro.oneback.patches.TikTokNativeContract;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Real-APK contract admission, rejection mutations and final output wiring. */
public final class TikTok4753ContractAudit {
    static String VERSION="47.5.3",CONTRACT="4753",NATIVE_BACK_OWNER="LX/03dx;";
    static String[] ROUTE={"IA0","Nu2","EH0","i33"};
    static final String RUNTIME="Lapp/hiro/oneback/runtime/OneBackExit;";
    // Original callback bodies, normalized to their original public flag.
    // These also prove that the private wrapper helpers preserve native fallback.
    static String BACK_BODY="dc2d2cc04d536d14a4f86957b7a10976b6eac1800af8dce2e84bdf3dad7ab48d";
    static String KEY_BODY="9cef2e8d7233ef24f9da0f8102e7e46f1dbe2b9d4769d93477d66181461dc9aa";
    static void profile(String version) {
        if("47.5.3".equals(version))return;
        if(!"47.4.3".equals(version))throw new IllegalArgumentException("Unverified native audit version: "+version);
        VERSION=version;CONTRACT="4743";NATIVE_BACK_OWNER="LX/03uJ;";
        ROUTE=new String[]{"tB0","tv2","jI0","w33"};
        BACK_BODY="1f821718d7f8c4aad2552eff60e0281495efa1506aa8b8391124d862f54c5cac";
        KEY_BODY="e2845f5ce2b7040a25c7fe59dfc8b97a060149e2ef871eee244e13752bb9bccb";
    }
    static int assertions;
    static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    static Map<String,ClassDef> read(String path,boolean all)throws Exception {
        Set<String> wanted=new HashSet<String>(Arrays.asList(TikTokNativeContract.ABILITY,TikTokNativeContract.ASSEM,TikTokNativeContract.HOX,TikTokNativeContract.MAIN,NATIVE_BACK_OWNER));
        Map<String,ClassDef> result=new TreeMap<String,ClassDef>();
        MultiDexContainer<? extends DexFile> container=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
        for(String entry:container.getDexEntryNames())for(ClassDef c:container.getEntry(entry).getDexFile().getClasses())
            if(all||wanted.contains(c.getType())||c.getType().startsWith("Lapp/hiro/oneback/runtime/"))check(result.put(c.getType(),c)==null,"duplicate class "+c.getType());
        return result;
    }
    static void rejected(String pkg,String version,Map<String,ClassDef> classes,String why) {
        boolean rejected=false;try{TikTokNativeContract.verifyIfTikTok(pkg,version,classes);}catch(IllegalStateException expected){rejected=true;}
        check(rejected,why);
    }
    static void admission(Map<String,ClassDef> original) {
        check(CONTRACT.equals(TikTokNativeContract.verifyIfTikTok("com.zhiliaoapp.musically",VERSION,original)),"real "+VERSION+" native contract accepted");
        ClassDef nativeOwner=original.get(NATIVE_BACK_OWNER);check(nativeOwner!=null,"actual native Back owner present");
        check(BACK_BODY.equals(normalizedBody(TikTokNativeContract.method(nativeOwner,"onBackPressed","","V"))),"original native Back body verified");
        check(KEY_BODY.equals(normalizedBody(TikTokNativeContract.method(nativeOwner,"dispatchKeyEvent","Landroid/view/KeyEvent;","Z"))),"original native key body verified");
        for(String version:Arrays.asList("47.4.2","47.4.3","47.4.4","47.5.2","47.5.3","47.5.4",VERSION+"-fake","unknown"))if(!VERSION.equals(version))rejected("com.zhiliaoapp.musically",version,original,"unsupported or mismatched version rejected "+version);
        rejected("com.zhiliaoapp.musically","46.2.3",original,VERSION+" native code must not be mistaken for legacy metadata");
        rejected("com.ss.android.ugc.trill",VERSION,original,"global input must not advertise unaudited trill "+VERSION+" compatibility");
        check(TikTokNativeContract.verifyIfTikTok("com.instagram.android","452.1",Collections.<String,ClassDef>emptyMap())==null,"Instagram scope unchanged");
        for(String[] row:TikTokNativeContract.verified(VERSION)) {
            Map<String,ClassDef> changed=new TreeMap<String,ClassDef>(original);ClassDef c=changed.get(row[0]);
            Method target=TikTokNativeContract.method(c,row[1],row[2],row[3]);List<Method> methods=new ArrayList<Method>();
            for(Method m:c.getMethods())if(m!=target)methods.add(m);
            // Method identity in a DEX iterable is not stable; select by ABI instead.
            methods.clear();for(Method m:c.getMethods())if(!(m.getName().equals(target.getName())&&m.getParameterTypes().equals(target.getParameterTypes())&&m.getReturnType().equals(target.getReturnType())))methods.add(m);
            changed.put(c.getType(),new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
            rejected("com.zhiliaoapp.musically",VERSION,changed,"missing actual dependency rejected "+row[1]);
            // Keep the old signature but replace the implementation: descriptor-only admission is insufficient.
            List<Instruction> code=new ArrayList<Instruction>();for(Instruction i:target.getImplementation().getInstructions())code.add(i);
            code.set(0,new ImmutableInstruction10x(Opcode.NOP));
            methods.add(new ImmutableMethod(target.getDefiningClass(),target.getName(),target.getParameters(),target.getReturnType(),target.getAccessFlags(),target.getAnnotations(),target.getHiddenApiRestrictions(),new ImmutableMethodImplementation(target.getImplementation().getRegisterCount(),code,Collections.emptyList(),Collections.emptyList())));
            changed.put(c.getType(),new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
            rejected("com.zhiliaoapp.musically",VERSION,changed,"same ABI changed body rejected "+row[1]);
        }
    }
    static boolean calls(Method m,String owner,String name) {
        if(m==null||m.getImplementation()==null)return false;
        for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction) {
            Reference r=((ReferenceInstruction)i).getReference();
            if(r instanceof MethodReference){MethodReference v=(MethodReference)r;if(owner.equals(v.getDefiningClass())&&name.equals(v.getName()))return true;}
        }return false;
    }
    static Method hierarchyMethod(Map<String,ClassDef> classes,String type,String callback,String arguments,String returns) {
        Set<String> seen=new HashSet<String>();
        while(type!=null&&seen.add(type)) {
            ClassDef c=classes.get(type);if(c==null)return null;
            Method method=TikTokNativeContract.method(c,callback,arguments,returns);
            if(method!=null)return method;
            type=c.getSuperclass();
        }return null;
    }
    static boolean hierarchyCalls(Map<String,ClassDef> classes,String type,String callback,String arguments,String returns,String hook) {
        return calls(hierarchyMethod(classes,type,callback,arguments,returns),RUNTIME,hook);
    }
    static String normalizedBody(Method method) {
        check(method!=null&&method.getImplementation()!=null,"native fallback body exists");
        return TikTokNativeContract.digest(new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),
            1,method.getAnnotations(),method.getHiddenApiRestrictions(),method.getImplementation()));
    }
    static void opcode(List<Instruction> code,int index,Opcode expected) {
        check(code.get(index).getOpcode()==expected,"wrapper opcode "+index+" is "+expected);
    }
    static void register(List<Instruction> code,int index,int expected) {
        check(code.get(index) instanceof OneRegisterInstruction&&((OneRegisterInstruction)code.get(index)).getRegisterA()==expected,"wrapper register at "+index);
    }
    static void invoke(List<Instruction> code,int index,Opcode expected,String owner,String name,String arguments,String returns,int... registers) {
        opcode(code,index,expected);Instruction instruction=code.get(index);
        check(instruction instanceof ReferenceInstruction&&((ReferenceInstruction)instruction).getReference() instanceof MethodReference,"wrapper method reference at "+index);
        MethodReference ref=(MethodReference)((ReferenceInstruction)instruction).getReference();StringBuilder params=new StringBuilder();
        for(CharSequence p:ref.getParameterTypes())params.append(p);
        check(owner.equals(ref.getDefiningClass())&&name.equals(ref.getName())&&arguments.equals(params.toString())&&returns.equals(ref.getReturnType()),"wrapper exact target at "+index);
        check(instruction instanceof FiveRegisterInstruction,"wrapper compact invoke format at "+index);
        FiveRegisterInstruction args=(FiveRegisterInstruction)instruction;int[] actual={args.getRegisterC(),args.getRegisterD(),args.getRegisterE(),args.getRegisterF(),args.getRegisterG()};
        check(args.getRegisterCount()==registers.length,"wrapper argument count at "+index);
        for(int i=0;i<registers.length;i++)check(actual[i]==registers[i],"wrapper argument register at "+index+":"+i);
    }
    /** Validate emitted behavior, including native fallback and its exception cleanup. */
    static void wrapper(Map<String,ClassDef> classes,String main,boolean key) {
        String name=key?"dispatchKeyEvent":"onBackPressed",params=key?"Landroid/view/KeyEvent;":"",returns=key?"Z":"V";
        Method method=hierarchyMethod(classes,main,name,params,returns);
        check(method!=null&&NATIVE_BACK_OWNER.equals(method.getDefiningClass()),"actual "+VERSION+" callback resolves to native owner "+name);
        MethodImplementation body=method.getImplementation();check(body!=null&&body.getRegisterCount()==(key?4:3),"wrapper register count "+name);
        List<Instruction> code=new ArrayList<Instruction>();for(Instruction instruction:body.getInstructions())code.add(instruction);
        check(code.size()==(key?13:12),"complete bounded wrapper instruction sequence "+name);
        int[] pc=new int[code.size()+1];for(int i=0;i<code.size();i++)pc[i+1]=pc[i]+code.get(i).getCodeUnits();
        String runtimeParams="Landroid/app/Activity;"+params;int[] arguments=key?new int[]{2,3}:new int[]{2};
        invoke(code,0,Opcode.INVOKE_STATIC,RUNTIME,key?"key":"tryBack",runtimeParams,"Z",arguments);
        opcode(code,1,Opcode.MOVE_RESULT);register(code,1,0);
        opcode(code,2,Opcode.IF_EQZ);register(code,2,0);
        check(pc[2]+((OffsetInstruction)code.get(2)).getCodeOffset()==pc[4],"false result reaches native fallback only");
        opcode(code,3,key?Opcode.RETURN:Opcode.RETURN_VOID);if(key)register(code,3,0);
        invoke(code,4,Opcode.INVOKE_STATIC,RUNTIME,key?"enterKey":"enterBack",runtimeParams,"V",arguments);
        invoke(code,5,Opcode.INVOKE_DIRECT,NATIVE_BACK_OWNER,"hiroOneBack$"+name,params,returns,arguments);
        Method helper=TikTokNativeContract.method(classes.get(NATIVE_BACK_OWNER),"hiroOneBack$"+name,params,returns);
        check(helper!=null&&helper.getAccessFlags()==0x1002,"native helper is private synthetic");
        check((key?KEY_BODY:BACK_BODY).equals(normalizedBody(helper)),"native helper body preserved "+name);
        int normal=key?7:6;if(key){opcode(code,6,Opcode.MOVE_RESULT);register(code,6,0);}
        invoke(code,normal,Opcode.INVOKE_STATIC,RUNTIME,"afterNative","","V");
        invoke(code,normal+1,Opcode.INVOKE_STATIC,RUNTIME,"leaveNative","","V");
        opcode(code,normal+2,key?Opcode.RETURN:Opcode.RETURN_VOID);if(key)register(code,normal+2,0);
        int handler=normal+3;opcode(code,handler,Opcode.MOVE_EXCEPTION);register(code,handler,1);
        invoke(code,handler+1,Opcode.INVOKE_STATIC,RUNTIME,"leaveNative","","V");
        opcode(code,handler+2,Opcode.THROW);register(code,handler+2,1);
        List<? extends TryBlock<? extends ExceptionHandler>> blocks=body.getTryBlocks();
        check(blocks.size()==1,"one native fallback cleanup range");TryBlock<? extends ExceptionHandler> block=blocks.get(0);
        check(block.getStartCodeAddress()==pc[5]&&block.getCodeUnitCount()==pc[normal]-pc[5],"catch range covers exact native call/result");
        check(block.getExceptionHandlers().size()==1,"one catch-all cleanup handler");ExceptionHandler caught=block.getExceptionHandlers().get(0);
        check(caught.getExceptionType()==null&&caught.getHandlerCodeAddress()==pc[handler],"fallback exceptions leave native scope and rethrow");
    }
    static void output(Map<String,ClassDef> after) {
        check(CONTRACT.equals(TikTokNativeContract.verifyIfTikTok("com.zhiliaoapp.musically",VERSION,after)),"emitted APK preserves verified native routing methods");
        String main=TikTokNativeContract.MAIN;
        check(hierarchyCalls(after,main,"onCreate","Landroid/os/Bundle;","V","install"),"actual main lifecycle bootstraps runtime");
        check(hierarchyCalls(after,main,"onResume","","V","onResume"),"actual main lifecycle refreshes runtime");
        wrapper(after,main,false);
        wrapper(after,main,true);
        ClassDef route=after.get("Lapp/hiro/oneback/runtime/TikTokHome;");check(route!=null,"TikTokHome runtime included");
        Method method=TikTokNativeContract.method(route,"route","Landroid/app/Activity;","Lapp/hiro/oneback/runtime/TikTokHome$Route;");
        check(method!=null,"TikTokHome route body present");Set<String> strings=new HashSet<String>();
        for(Instruction i:method.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)i).getReference();if(r instanceof StringReference)strings.add(((StringReference)r).getString());}
        for(String name:ROUTE)check(strings.contains(name),"output runtime uses real "+VERSION+" method "+name);
    }
    public static void main(String[] args)throws Exception {
        if(args.length<2||args.length>3)throw new IllegalArgumentException("fingerprints|original|output APK [47.4.3|47.5.3]");
        if(args.length==3)profile(args[2]);
        Map<String,ClassDef> classes=read(args[1],args[0].equals("output"));
        if(args[0].equals("fingerprints")){for(Map.Entry<String,String> e:TikTokNativeContract.fingerprints(VERSION,classes).entrySet())System.out.println(e.getKey()+" "+e.getValue());return;}
        if(args[0].equals("original"))admission(classes);else if(args[0].equals("output"))output(classes);else throw new IllegalArgumentException(args[0]);
        System.out.println("PASS TikTok4753ContractAudit "+args[0]+": assertions="+assertions+"; version="+VERSION+"; real native API verified; device behavior not asserted");
    }
}
