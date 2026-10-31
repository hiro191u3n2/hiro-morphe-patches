import java.io.*;
import java.nio.file.*;
import java.util.*;
import app.hiro.oneback.patches.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.*;
import com.android.tools.smali.dexlib2.writer.io.*;

/** Independent inverse of the committed-Back completion. All native branches,
 * registers, handlers and method metadata are compared with the original APK.
 * This audit does not model Android's UI or claim a device execution. */
public final class Instagram137ProfileReelsAudit {
    private static final String DISPATCHER="LX/001p;";
    private static final String COMPLETE="completeInstagramDispatcherBack";
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static Map<String,ClassDef> read(String path)throws Exception {
        Map<String,ClassDef> result=new LinkedHashMap<String,ClassDef>();
        MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
        for(String entry:dex.getDexEntryNames())for(ClassDef type:dex.getEntry(entry).getDexFile().getClasses())result.put(type.getType(),type);
        return result;
    }
    static Method find(ClassDef type,String name){for(Method m:type.getMethods())if(name.equals(m.getName()))return m;throw new AssertionError(name);}
    static List<Instruction> list(Method method){List<Instruction> b=new ArrayList<Instruction>();for(Instruction i:method.getImplementation().getInstructions())b.add(i);return b;}
    static Method immutable(Method method,MethodImplementation code){return new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),method.getAccessFlags(),method.getAnnotations(),method.getHiddenApiRestrictions(),code);}
    static MethodReference reference(Instruction i) {
        if(!(i instanceof ReferenceInstruction))return null;
        Reference r=((ReferenceInstruction)i).getReference();return r instanceof MethodReference?(MethodReference)r:null;
    }
    static boolean completion(Instruction i){MethodReference r=reference(i);return r!=null&&OneBackPatch.RUNTIME.equals(r.getDefiningClass())&&COMPLETE.equals(r.getName());}
    static void completionCall(Instruction i) {
        check(i.getOpcode()==Opcode.INVOKE_STATIC&&i instanceof FiveRegisterInstruction,"completion static call shape");
        MethodReference r=reference(i);FiveRegisterInstruction a=(FiveRegisterInstruction)i;
        check(r!=null&&OneBackPatch.RUNTIME.equals(r.getDefiningClass())&&COMPLETE.equals(r.getName())
            &&r.getParameterTypes().equals(Collections.singletonList("Ljava/lang/Object;"))&&r.getReturnType().equals("V"),"completion exact ABI");
        check(a.getRegisterCount()==1&&a.getRegisterC()==4,"completion original dispatcher receiver");
    }
    /** Leave the old entry gate in place so both existing independent inverses
     * can continue checking and removing it. Never silently discard a call. */
    public static Method withoutCompletion(Method method) {
        check(DISPATCHER.equals(method.getDefiningClass())&&"A04".equals(method.getName())
            &&method.getParameterTypes().isEmpty()&&"V".equals(method.getReturnType()),"only452 committed Back may carry completion");
        List<Instruction> b=list(method);Set<Integer> returns=new HashSet<Integer>();int count=0;
        for(int i=0;i<b.size();i++) {
            if(completion(b.get(i))) {completionCall(b.get(i));check(i+1<b.size()&&b.get(i+1).getOpcode()==Opcode.RETURN_VOID,"completion immediately precedes normal return");count++;}
            if(b.get(i).getOpcode()==Opcode.RETURN_VOID) {check(i>0&&completion(b.get(i-1)),"every normal return has completion");returns.add(i);}
        }
        check(count==3&&returns.size()==3,"two native returns and handled gate each complete once");
        for(int i=0;i<b.size();i++)if(b.get(i) instanceof OffsetInstruction)
            check(!returns.contains(Instagram452Audit.target(b,i)),"native branch cannot bypass completion");
        Map<Integer,Integer> index=new HashMap<Integer,Integer>();int pc=0;
        for(int i=0;i<b.size();i++){index.put(pc,i);pc+=b.get(i).getCodeUnits();}
        for(TryBlock<? extends ExceptionHandler> t:method.getImplementation().getTryBlocks())
            for(ExceptionHandler h:t.getExceptionHandlers())check(!returns.contains(index.get(h.getHandlerCodeAddress())),"handler cannot bypass completion");
        MutableMethodImplementation body=new MutableMethodImplementation(method.getImplementation());
        for(int i=b.size()-1;i>=0;i--)if(completion(b.get(i))) {
            body.replaceInstruction(i,new BuilderInstruction10x(Opcode.RETURN_VOID));body.removeInstruction(i+1);
        }
        return immutable(method,body);
    }
    static void compare(Map<String,ClassDef> original,Map<String,ClassDef> rebuilt)throws Exception {
        Method old=find(original.get(DISPATCHER),"A04"),actual=find(rebuilt.get(DISPATCHER),"A04");
        Method gated=withoutCompletion(actual);Instagram452Audit.gate(gated,"tryInstagramDispatcherBack");
        MutableMethodImplementation code=new MutableMethodImplementation(gated.getImplementation());for(int i=0;i<4;i++)code.removeInstruction(0);
        check(Arrays.equals(Instagram452Audit.bytes(old),Instagram452Audit.bytes(immutable(actual,code))),"all original committed-Back executable bytes and metadata restored");
        int calls=0;
        for(ClassDef type:rebuilt.values())for(Method m:type.getMethods())if(m.getImplementation()!=null)
            for(Instruction i:m.getImplementation().getInstructions())if(completion(i)) {
                check(DISPATCHER.equals(type.getType())&&m.getName().equals("A04")&&m.getParameterTypes().isEmpty(),"completion absent from cancellation, predictive and unrelated methods");calls++;
            }
        check(calls==3,"exact emitted completion call count");
    }
    static void rejectsCorruptEmitted(ClassDef type,boolean bypass) {
        MutableClass corrupt=new MutableClass(type);MutableMethod method=(MutableMethod)find(corrupt,"A04");
        MutableMethodImplementation body=method.getImplementation();
        if(bypass)body.replaceInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,body.newLabelForIndex(4)));
        else {Reference r=((ReferenceInstruction)body.getInstructions().get(3)).getReference();body.replaceInstruction(3,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,3,0,0,0,0,r));}
        boolean rejected=false;try{withoutCompletion(method);}catch(AssertionError expected){rejected=true;}
        check(rejected,bypass?"branch bypassing completion rejected":"wrong completion receiver rejected");
    }
    static void rejectNativeChange(Map<String,ClassDef> original,String owner,String name) {
        Map<String,ClassDef> bad=new LinkedHashMap<String,ClassDef>(original);MutableClass type=new MutableClass(original.get(owner));
        ((MutableMethod)find(type,name)).getImplementation().replaceInstruction(0,new BuilderInstruction10x(Opcode.NOP));bad.put(owner,type);
        final int[] edits={0};boolean rejected=false;
        try{InstagramExternalProfilePatch.apply(bad,t->{edits[0]++;return new MutableClass(bad.get(t));},new HashMap<String,Integer>());}
        catch(IllegalStateException expected){rejected=true;}
        check(rejected&&edits[0]==0,"changed native Reels/removal contract rejected before mutation: "+owner+"->"+name);
    }
    public static void main(String[] args)throws Exception {
        if(args.length<1||args.length>2)throw new IllegalArgumentException("Usage: Instagram137ProfileReelsAudit ORIGINAL_APK [EMITTED_APK] (both inputs are read-only)");
        Map<String,ClassDef> original=read(args[0]);InstagramProfileReelsContract.verifyOriginal(original);
        Map<String,MutableClass> changes=new LinkedHashMap<String,MutableClass>();Map<String,Integer> counts=new LinkedHashMap<String,Integer>();
        Instagram452Patch.applyDispatcher(original,t->changes.computeIfAbsent(t,k->new MutableClass(original.get(k))),counts);
        check(counts.size()==6&&counts.get("instagram_androidx_complete")==3,"six452 dispatcher hook kinds, three normal completions");
        Path fixture=Files.createTempFile("instagram137-dispatcher-",".dex");
        try {
            MemoryDataStore store=new MemoryDataStore();
            try {DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),changes.values()));
                Files.write(fixture,Arrays.copyOf(store.getBuffer(),store.getSize()));}
            finally{store.close();}
            Map<String,ClassDef> emitted=read(fixture.toString());compare(original,emitted);
            rejectsCorruptEmitted(emitted.get(DISPATCHER),false);rejectsCorruptEmitted(emitted.get(DISPATCHER),true);
        } finally{Files.deleteIfExists(fixture);}
        rejectNativeChange(original,"Lcom/instagram/mainactivity/InstagramMainActivity;","HNw");
        rejectNativeChange(original,"Landroidx/fragment/app/Fragment;","isRemoving");
        rejectNativeChange(original,"LX/003Q;","A07");
        if(args.length==2)compare(original,read(args[1]));
        System.out.println("PASS Instagram137ProfileReelsAudit: "+checks+" assertions; exact committed-Back body restored, all three normal returns complete, no predictive/cancel completion, native contract changes rejected before mutation; static DEX validation, no device execution");
    }
}
