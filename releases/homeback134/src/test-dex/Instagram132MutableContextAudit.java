import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipFile;
import app.hiro.oneback.patches.InstagramExternalProfilePatch;
import app.hiro.oneback.patches.OneBackPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Actual-APK regression for a mutable-context representation mismatch.
 * No original app implementation is bundled. The known original digest is
 * unchanged; live builders must retain every instruction and selectable catch.
 * This audit also verifies that our pre-edit input snapshot cannot alias the
 * live mutable classes supplied by a preceding patch.
 */
public final class Instagram132MutableContextAudit {
    private static final String MAIN_LINK="LX/003Q;";
    private static final String MAIN="Lcom/instagram/mainactivity/InstagramMainActivity;";
    private static final String RAW_PIN="fd8020d2121e7176f21e095e32c4744fc57c82161350f05e53a599de4018bec9";
    private static int checks,rejected;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Method link(ClassDef type){return Instagram130ProfileLinkAudit.method(type,"A04");}
    private static Method copy(Method before,MethodImplementation body){
        return new ImmutableMethod(before.getDefiningClass(),before.getName(),before.getParameters(),before.getReturnType(),
                before.getAccessFlags(),before.getAnnotations(),before.getHiddenApiRestrictions(),body);
    }
    private static ClassDef replace(ClassDef before,Method changed){
        return replace(before,RealUpgradeAudit.key(changed),changed);
    }
    private static ClassDef replace(ClassDef before,String id,Method changed){
        List<Method> methods=new ArrayList<>();
        for(Method method:before.getMethods())methods.add(RealUpgradeAudit.key(method).equals(id)?changed:method);
        return new ImmutableClassDef(before.getType(),before.getAccessFlags(),before.getSuperclass(),before.getInterfaces(),
                before.getSourceFile(),before.getAnnotations(),before.getFields(),methods);
    }
    private static List<TryBlock<? extends ExceptionHandler>> tries(Method method){return new ArrayList<>(method.getImplementation().getTryBlocks());}
    private static Method withTries(Method method,List<? extends TryBlock<? extends ExceptionHandler>> tries){
        MethodImplementation body=method.getImplementation();
        return copy(method,new ImmutableMethodImplementation(body.getRegisterCount(),body.getInstructions(),tries,body.getDebugItems()));
    }
    private static ExceptionHandler handler(String type,int target){return new ImmutableExceptionHandler(type,target);}
    private static TryBlock<? extends ExceptionHandler> region(TryBlock<? extends ExceptionHandler> old,List<? extends ExceptionHandler> handlers){
        return new ImmutableTryBlock(old.getStartCodeAddress(),old.getCodeUnitCount(),handlers);
    }
    private static boolean pinned(Method method){
        try{return RAW_PIN.equals(OneBackPatch.instagramNavigationDigest(method));}
        catch(IllegalStateException rejected){return false;}
    }
    private static void reject(Method method,String label,Map<String,ClassDef> full){
        check(!pinned(method),"changed exception/body contract is rejected: "+label);
        if(full!=null){
            Map<String,ClassDef> changed=new LinkedHashMap<>(full);changed.put(MAIN_LINK,replace(full.get(MAIN_LINK),method));
            final int[] mutableRequests={0};boolean failure=false;
            try{InstagramExternalProfilePatch.apply(changed,type->{mutableRequests[0]++;return new MutableClass(changed.get(type));},new LinkedHashMap<String,Integer>());}
            catch(IllegalStateException expected){failure=true;}
            check(failure&&mutableRequests[0]==0,"whole contract rejects before obtaining editor: "+label);
        }
        rejected++;
    }
    private static ClassDef serialized(ClassDef clazz)throws Exception {
        MemoryDataStore store=new MemoryDataStore();
        // DexPool performs the real builder-to-DEX conversion; this audit does
        // not pre-normalize exception regions for the writer.
        DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),Collections.singletonList(clazz)));
        byte[] bytes=Arrays.copyOf(store.getBuffer(),store.getSize());store.close();
        Set<? extends ClassDef> classes=new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses();
        check(classes.size()==1,"one serialized native class");return classes.iterator().next();
    }
    private static Method fixtureMethod(String name,int flags,int registers){
        return new ImmutableMethod("Ltest/IncomingMutable;",name,Collections.emptyList(),"V",flags,
                Collections.emptySet(),Collections.emptySet(),new ImmutableMethodImplementation(registers,
                Collections.singletonList(new ImmutableInstruction10x(Opcode.RETURN_VOID)),Collections.emptyList(),Collections.emptyList()));
    }
    private static void snapshotMemberAdditions()throws Exception {
        ClassDef fixture=new ImmutableClassDef("Ltest/IncomingMutable;",1,"Ljava/lang/Object;",Collections.emptyList(),
                null,Collections.emptySet(),Collections.emptyList(),Collections.singletonList(fixtureMethod("nativeBody",1,1)));
        MutableClass live=new MutableClass(fixture);ClassDef saved=OneBackPatch.snapshotIncomingClass(live);
        // The patcher writes getDirectMethods/getVirtualMethods, whereas our
        // transformations add members to getMethods. Snapshot creation must
        // not initialize the live class's cached subsets before these additions.
        live.getMethods().add(new MutableMethod(fixtureMethod("hiroOneBack$terminal$0",0x100a,0)));
        live.getMethods().add(new MutableMethod(fixtureMethod("onUserInteraction",1,1)));
        Map<String,Method> expected=RealUpgradeAudit.methods(live),frozen=RealUpgradeAudit.methods(saved);
        check(expected.size()==3&&frozen.size()==1,"snapshot is separate from later direct and virtual additions");
        ClassDef emitted=serialized(live);Map<String,Method> actual=RealUpgradeAudit.methods(emitted);
        check(expected.keySet().equals(actual.keySet()),"serialized incoming mutable class retains new direct and virtual members");
        RealUpgradeAudit.classMeta(live,emitted);
        for(String id:expected.keySet())RealUpgradeAudit.compare(id,expected.get(id),actual.get(id));
        check(RealUpgradeAudit.methods(saved).keySet().equals(RealUpgradeAudit.methods(fixture).keySet()),"emitting new members cannot mutate the incoming snapshot");
    }
    private static void representations(ClassDef clazz,Map<String,ClassDef> full)throws Exception {
        Method raw=link(clazz);check(pinned(raw),"published original A04 fingerprint remains unchanged");
        check(Instagram130ProfileLinkAudit.instructions(raw).size()==234,"actual original instruction count");
        check(raw.getImplementation().getTryBlocks().size()==3,"actual original has three protected regions");
        MutableClass live=new MutableClass(clazz);Method mutable=link(live);
        check(mutable.getImplementation().getTryBlocks().size()==5,"actual builder splits two multi-handler regions into five entries");
        // This assertion fails with the previously published OneBack loader,
        // despite no application instruction having been edited.
        check(pinned(mutable),"materializing an untouched sibling method class preserves the A04 contract");
        RealUpgradeAudit.compare(RealUpgradeAudit.key(raw),raw,
                copy(mutable,ActualInstagram452Audit.orderedTries(mutable.getImplementation())));
        check(pinned(link(serialized(live))),"real serialized DEX retains the exact original contract");
        if(full!=null){
            Map<String,ClassDef> changed=new LinkedHashMap<>(full);changed.put(MAIN_LINK,live);
            InstagramExternalProfilePatch.verifyOriginal(changed);
            Method sibling=Instagram130ProfileLinkAudit.method(live,"A07");
            ((MutableMethod)sibling).getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));
            check(pinned(link(live)),"an edit in sibling A07 cannot change A04 provenance");
            InstagramExternalProfilePatch.verifyOriginal(changed);
            Map<String,MutableClass> edited=new LinkedHashMap<>();Map<String,Integer> counts=new LinkedHashMap<>();
            InstagramExternalProfilePatch.apply(changed,type->edited.computeIfAbsent(type,key->new MutableClass(changed.get(key))),counts);
            check(edited.size()==3&&counts.size()==4,"materialized context retains only the original three targets and four hooks");
            Instagram130ProfileLinkAudit.output(full,Instagram130ProfileLinkAudit.serialized(edited.values()));
        }
        List<TryBlock<? extends ExceptionHandler>> original=tries(raw);TryBlock<? extends ExceptionHandler> middle=original.get(1),last=original.get(2);
        check(middle.getExceptionHandlers().size()==2&&last.getExceptionHandlers().size()==2,"actual multi-handler regions");
        for(String fault:Arrays.asList("handler_type","handler_target","handler_order","duplicate_handler","catch_all","dropped_handler","range_start","range_end","unequal_overlap","region_order")){
            List<TryBlock<? extends ExceptionHandler>> altered=new ArrayList<>(original);
            List<ExceptionHandler> handlers=new ArrayList<>(last.getExceptionHandlers());
            ExceptionHandler first=handlers.get(0),second=handlers.get(1);
            if(fault.equals("handler_type"))handlers.set(0,handler("Ljava/lang/IllegalArgumentException;",first.getHandlerCodeAddress()));
            else if(fault.equals("handler_target"))handlers.set(0,handler(first.getExceptionType(),second.getHandlerCodeAddress()));
            else if(fault.equals("handler_order"))Collections.reverse(handlers);
            else if(fault.equals("duplicate_handler"))handlers.add(first);
            else if(fault.equals("catch_all"))handlers.set(0,handler(null,first.getHandlerCodeAddress()));
            else if(fault.equals("dropped_handler"))handlers.remove(1);
            if(fault.equals("range_start"))altered.set(1,new ImmutableTryBlock(middle.getStartCodeAddress()+3,middle.getCodeUnitCount()-3,middle.getExceptionHandlers()));
            else if(fault.equals("range_end"))altered.set(1,new ImmutableTryBlock(middle.getStartCodeAddress(),middle.getCodeUnitCount()-1,middle.getExceptionHandlers()));
            else if(fault.equals("unequal_overlap"))altered.add(2,new ImmutableTryBlock(middle.getStartCodeAddress()+3,middle.getCodeUnitCount()-3,middle.getExceptionHandlers()));
            else if(fault.equals("region_order"))Collections.swap(altered,0,1);
            else altered.set(2,region(last,handlers));
            reject(withTries(raw,altered),fault,full);
        }
        MutableClass wrongBody=new MutableClass(clazz);
        ((MutableMethod)link(wrongBody)).getImplementation().replaceInstruction(111,
                new BuilderInstruction21c(Opcode.CONST_STRING,0,new ImmutableStringReference("not_main_activity")));
        reject(link(wrongBody),"changed instruction in a split-handler builder",full);
    }
    private static void snapshot(Map<String,ClassDef> input)throws Exception {
        Map<String,ClassDef> current=new LinkedHashMap<>(input),frozen=new LinkedHashMap<>();
        Set<String> alreadyMutable=new HashSet<>(Arrays.asList(MAIN_LINK,MAIN,Instagram130ProfileLinkAudit.ROUTER,
                Instagram130ProfileLinkAudit.PROFILE,Instagram130ProfileLinkAudit.URL_DELEGATE));
        for(String type:alreadyMutable)current.put(type,new MutableClass(input.get(type)));
        for(Map.Entry<String,ClassDef> entry:current.entrySet()){
            ClassDef before=entry.getValue(),saved=OneBackPatch.snapshotIncomingClass(before);frozen.put(entry.getKey(),saved);
            if(alreadyMutable.contains(entry.getKey())){
                check(saved!=before&&!(saved instanceof MutableClass),"copy already mutable input "+entry.getKey());
                RealUpgradeAudit.classMeta(input.get(entry.getKey()),saved);
                Map<String,Method> rawMethods=RealUpgradeAudit.methods(input.get(entry.getKey())),savedMethods=RealUpgradeAudit.methods(saved);
                check(rawMethods.keySet().equals(savedMethods.keySet()),"snapshot retains complete native method inventory "+entry.getKey());
                for(String id:rawMethods.keySet()){
                    Method raw=rawMethods.get(id),frozenMethod=savedMethods.get(id);
                    Method compared=frozenMethod.getImplementation()==null?frozenMethod:
                            copy(frozenMethod,ActualInstagram452Audit.orderedTries(frozenMethod.getImplementation()));
                    RealUpgradeAudit.compare(id,raw,compared);
                    for(int i=0;i<raw.getParameters().size();i++){
                        MethodParameter a=raw.getParameters().get(i),b=frozenMethod.getParameters().get(i);
                        check(Objects.equals(a.getName(),b.getName())&&Objects.equals(a.getSignature(),b.getSignature()),"snapshot retains parameter metadata "+id);
                    }
                }
            }
            else check(saved==before,"share untouched immutable definition "+entry.getKey());
        }
        Method savedGetter=Instagram130ProfileLinkAudit.method(frozen.get(MAIN),"A08");
        check(MAIN.contentEquals(savedGetter.getParameterTypes().get(0)),"immutable parameter descriptor content remains exact");
        check(!(savedGetter.getParameterTypes().get(0) instanceof String),"actual immutable parameter representation exercised");
        InstagramExternalProfilePatch.verifyOriginal(frozen);
        List<MethodParameter> wrongParameters=Collections.<MethodParameter>singletonList(new ImmutableMethodParameter("Ljava/lang/Object;",Collections.emptySet(),null));
        Method wrongSignature=new ImmutableMethod(savedGetter.getDefiningClass(),savedGetter.getName(),wrongParameters,savedGetter.getReturnType(),
                savedGetter.getAccessFlags(),savedGetter.getAnnotations(),savedGetter.getHiddenApiRestrictions(),savedGetter.getImplementation());
        Map<String,ClassDef> wrongSnapshot=new LinkedHashMap<>(frozen);wrongSnapshot.put(MAIN,replace(frozen.get(MAIN),RealUpgradeAudit.key(savedGetter),wrongSignature));
        final int[] requests={0};boolean signatureRejected=false;
        try{InstagramExternalProfilePatch.apply(wrongSnapshot,type->{requests[0]++;return (MutableClass)current.get(type);},new LinkedHashMap<String,Integer>());}
        catch(IllegalStateException expected){signatureRejected=true;}
        check(signatureRejected&&requests[0]==0,"immutable descriptor content change is rejected before editing");
        // The native Main gate is inspected again after our own Main changes.
        // Its incoming definition must remain stable even when a prior patch
        // caused that class to be mutable before our callback started.
        Method savedMain=Instagram130ProfileLinkAudit.method(frozen.get(MAIN),"A1u");
        app.hiro.oneback.patches.Instagram452Boundaries.verifyMain(savedMain);
        String mainPin=OneBackPatch.instagramNavigationDigest(savedMain);
        MutableMethod liveMain=(MutableMethod)Instagram130ProfileLinkAudit.method(current.get(MAIN),"A1u");
        liveMain.getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));
        check(mainPin.equals(OneBackPatch.instagramNavigationDigest(savedMain)),"incoming Main body does not alias the live editor");
        check(!mainPin.equals(OneBackPatch.instagramNavigationDigest(liveMain)),"live Main actually changed");
        InstagramExternalProfilePatch.verifyOriginal(frozen);
        Map<String,Integer> counts=new LinkedHashMap<>();
        InstagramExternalProfilePatch.apply(frozen,type->(MutableClass)current.get(type),counts);
        InstagramExternalProfilePatch.verifyOriginal(frozen);
        check(counts.size()==4,"repeated verification against frozen incoming definitions survives live insertion");
        Map<String,ClassDef> changed=new LinkedHashMap<>();
        for(String type:Arrays.asList(Instagram130ProfileLinkAudit.ROUTER,Instagram130ProfileLinkAudit.PROFILE,Instagram130ProfileLinkAudit.URL_DELEGATE))changed.put(type,current.get(type));
        Instagram130ProfileLinkAudit.output(frozen,Instagram130ProfileLinkAudit.serialized(changed.values()));
    }
    static void originalAndMutations(Map<String,ClassDef> input)throws Exception {
        snapshotMemberAdditions();representations(input.get(MAIN_LINK),input);snapshot(input);
        System.out.println("PASS Instagram132MutableContextAudit: assertions="+checks+", rejectedMutations="+rejected
                +"; raw/mutable/serialized contracts and incoming mutable snapshot preserved; no device execution");
    }
    public static void main(String[] args)throws Exception {
        if(args.length==1&&args[0].equals("snapshot-members")){
            snapshotMemberAdditions();
            System.out.println("PASS Instagram132MutableContextAudit snapshot-members: assertions="+checks
                    +"; incoming snapshot and serialized new direct/virtual members preserved");return;
        }
        if(args.length!=2||!args[0].equals("digest"))throw new IllegalArgumentException("digest ORIGINAL_APK");
        try(ZipFile zip=new ZipFile(args[1])){
            byte[] raw;try(InputStream in=zip.getInputStream(zip.getEntry("classes2.dex"))){raw=in.readAllBytes();}
            for(ClassDef clazz:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(raw)).getClasses())if(MAIN_LINK.equals(clazz.getType())){
                representations(clazz,null);
                System.out.println("PASS Instagram132MutableContextAudit digest: assertions="+checks+", rejectedMutations="+rejected+"; actual original A04, builder and serialized DEX");return;
            }
        }
        throw new AssertionError("original main-link class absent");
    }
}
