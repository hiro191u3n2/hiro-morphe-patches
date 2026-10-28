import java.io.*;
import java.nio.file.*;
import java.util.*;
import app.hiro.oneback.patches.TikTokNativeContract;
import app.hiro.oneback.patches.TikTokNativeBoundaries;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Real 47.4.3 APK admission, guarded native Back boundaries and output control flow.
 * The historical filename is retained for the existing QA command interface. */
public final class TikTok4753ContractAudit {
    static final String VERSION="47.4.3",CONTRACT="4743",NATIVE_BACK_OWNER="LX/03uJ;";
    static final String[] ROUTE={"tB0","tv2","jI0","w33"};
    static final String RUNTIME="Lapp/hiro/oneback/runtime/OneBackExit;";
    static final String NATIVE_HELPER="LX/1AA5;";
    static final String HELPER_BASE="Lcom/ss/android/ugc/aweme/homepage/TiktokBaseMainHelper;";
    static final String NATIVE_STRATEGY="Lcom/ss/android/ugc/aweme/main/assems/BackRefreshStrategyAssem;";
    static final String HELPER_BODY="494520fc0a5911045593632a22d6d666090da7ed3f37e7d7cae010b542583a2c";
    static final String STRATEGY_BODY="2563f7ef1961ebeaf02d9d97b941a66ef0d9a0b246efa1ed8a04508fa9e546f3";
    // Original callback bodies, normalized to their original public flag.
    // These also prove that the private wrapper helpers preserve native fallback.
    static final String BACK_BODY="1f821718d7f8c4aad2552eff60e0281495efa1506aa8b8391124d862f54c5cac";
    static final String KEY_BODY="e2845f5ce2b7040a25c7fe59dfc8b97a060149e2ef871eee244e13752bb9bccb";
    static void profile(String version) {
        if(!"47.4.3".equals(version))throw new IllegalArgumentException("Unverified native audit version: "+version);
    }
    static int assertions;
    static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    static Map<String,ClassDef> read(String path,boolean all)throws Exception {
        Set<String> wanted=new HashSet<String>(Arrays.asList(TikTokNativeContract.ABILITY,TikTokNativeContract.ASSEM,TikTokNativeContract.HOX,TikTokNativeContract.MAIN,TikTokNativeContract.STATE,TikTokNativeContract.OUTER,NATIVE_BACK_OWNER,NATIVE_HELPER,NATIVE_STRATEGY,HELPER_BASE));
        for(String[] row:TikTokNativeContract.verified(VERSION))wanted.add(row[0]);
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
        TikTokNativeBoundaries.verifyOriginal(original);
        check(HELPER_BODY.equals(TikTokNativeContract.digest(TikTokNativeContract.method(original.get(NATIVE_HELPER),"onBackPressed","","Z"))),"actual 47.4.3 comment/intercept/double-Back helper body");
        check(STRATEGY_BODY.equals(TikTokNativeContract.digest(TikTokNativeContract.method(original.get(NATIVE_STRATEGY),"onBackPress","","V"))),"actual 47.4.3 guarded root strategy body");
        ClassDef nativeOwner=original.get(NATIVE_BACK_OWNER);check(nativeOwner!=null,"actual native Back owner present");
        check(BACK_BODY.equals(normalizedBody(TikTokNativeContract.method(nativeOwner,"onBackPressed","","V"))),"original native Back body verified");
        check(KEY_BODY.equals(normalizedBody(TikTokNativeContract.method(nativeOwner,"dispatchKeyEvent","Landroid/view/KeyEvent;","Z"))),"original native key body verified");
        for(String version:Arrays.asList("47.4.2","47.4.3","47.4.4","47.5.2","47.5.3","47.5.4",VERSION+"-fake","unknown"))if(!VERSION.equals(version))rejected("com.zhiliaoapp.musically",version,original,"unsupported or mismatched version rejected "+version);
        rejected("com.zhiliaoapp.musically","46.2.3",original,VERSION+" native code must not be mistaken for legacy metadata");
        rejected("com.ss.android.ugc.trill",VERSION,original,"global input must not advertise unaudited trill "+VERSION+" compatibility");
        check(TikTokNativeContract.verifyIfTikTok("com.instagram.android","452.1",Collections.<String,ClassDef>emptyMap())==null,"Instagram scope unchanged");
        outerBindingMutations(original);
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
        for(String[] row:new String[][]{{NATIVE_HELPER,"onBackPressed","Z"},{NATIVE_STRATEGY,"onBackPress","V"}}) {
            ClassDef c=original.get(row[0]);List<Method> methods=new ArrayList<Method>();
            Method target=TikTokNativeContract.method(c,row[1],"",row[2]);
            for(Method m:c.getMethods())if(!(row[1].equals(m.getName())&&m.getParameterTypes().isEmpty()&&row[2].equals(m.getReturnType())))methods.add(m);
            Map<String,ClassDef> changed=new TreeMap<String,ClassDef>(original);
            changed.put(c.getType(),new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
            boundaryRejected(changed,"missing real guarded Back method rejected "+row[0]);
            List<Instruction> code=instructions(target);code.set(0,new ImmutableInstruction10x(Opcode.NOP));
            methods.add(withBody(target,new ImmutableMethodImplementation(target.getImplementation().getRegisterCount(),code,Collections.emptyList(),Collections.emptyList())));
            changed.put(c.getType(),new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
            boundaryRejected(changed,"changed real guarded Back body rejected "+row[0]);
        }
    }
    static void outerBindingMutations(Map<String,ClassDef> original) {
        ClassDef main=original.get(TikTokNativeContract.MAIN);
        for(String fieldName:new String[]{"stateManager","communication_gen_field_com_ss_android_ugc_aweme_base_HomePageViewPagerAbility"})for(int mutation=0;mutation<3;mutation++) {
            List<Field> fields=new ArrayList<Field>();boolean matched=false;
            for(Field field:main.getFields()) {
                if(!fieldName.equals(field.getName())){fields.add(field);continue;}
                matched=true;
                if(mutation==1)fields.add(new ImmutableField(field.getDefiningClass(),field.getName(),field.getType(),field.getAccessFlags()|8,field.getInitialValue(),field.getAnnotations(),field.getHiddenApiRestrictions()));
                if(mutation==2)fields.add(new ImmutableField(field.getDefiningClass(),field.getName(),"Ljava/lang/Object;",field.getAccessFlags(),field.getInitialValue(),field.getAnnotations(),field.getHiddenApiRestrictions()));
            }
            check(matched,"real outer pager field present "+fieldName);
            Map<String,ClassDef> changed=new TreeMap<String,ClassDef>(original);
            changed.put(main.getType(),new ImmutableClassDef(main.getType(),main.getAccessFlags(),main.getSuperclass(),main.getInterfaces(),main.getSourceFile(),main.getAnnotations(),fields,main.getMethods()));
            rejected("com.zhiliaoapp.musically",VERSION,changed,"missing/static/wrong-type outer field rejected "+fieldName+" mutation="+mutation);
        }
        ClassDef state=original.get(TikTokNativeContract.STATE);
        Map<String,ClassDef> changed=new TreeMap<String,ClassDef>(original);
        changed.put(state.getType(),new ImmutableClassDef(state.getType(),state.getAccessFlags(),"Ljava/lang/Object;",state.getInterfaces(),state.getSourceFile(),state.getAnnotations(),state.getFields(),state.getMethods()));
        rejected("com.zhiliaoapp.musically",VERSION,changed,"wrong native outer state inheritance rejected");
        ClassDef assem=original.get(TikTokNativeContract.OUTER_ASSEM);
        changed=new TreeMap<String,ClassDef>(original);
        changed.put(assem.getType(),new ImmutableClassDef(assem.getType(),assem.getAccessFlags(),assem.getSuperclass(),Collections.<String>emptyList(),assem.getSourceFile(),assem.getAnnotations(),assem.getFields(),assem.getMethods()));
        rejected("com.zhiliaoapp.musically",VERSION,changed,"outer implementation without native interface rejected");
        ClassDef ability=original.get(TikTokNativeContract.OUTER);List<Method> methods=new ArrayList<Method>();
        for(Method method:ability.getMethods())if(!"Ir1".equals(method.getName()))methods.add(method);
        changed=new TreeMap<String,ClassDef>(original);
        changed.put(ability.getType(),new ImmutableClassDef(ability.getType(),ability.getAccessFlags(),ability.getSuperclass(),ability.getInterfaces(),ability.getSourceFile(),ability.getAnnotations(),ability.getFields(),methods));
        rejected("com.zhiliaoapp.musically",VERSION,changed,"outer interface with missing native Back operation rejected");
    }
    static void boundaryRejected(Map<String,ClassDef> classes,String why) {
        boolean failed=false;try{TikTokNativeBoundaries.verifyOriginal(classes);}catch(IllegalStateException expected){failed=true;}
        check(failed,why);
    }
    static List<Instruction> instructions(Method method) {
        check(method!=null&&method.getImplementation()!=null,"native method has executable code");
        List<Instruction> result=new ArrayList<Instruction>();for(Instruction i:method.getImplementation().getInstructions())result.add(i);return result;
    }
    static Method withBody(Method m,MethodImplementation body) {
        return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);
    }
    static int[] addresses(List<Instruction> code) {
        int[] pc=new int[code.size()+1];for(int i=0;i<code.size();i++)pc[i+1]=pc[i]+code.get(i).getCodeUnits();return pc;
    }
    static void branch(List<Instruction> code,int from,Opcode op,int register,int to,String why) {
        opcode(code,from,op);register(code,from,register);int[] pc=addresses(code);
        check(code.get(from) instanceof OffsetInstruction&&pc[from]+((OffsetInstruction)code.get(from)).getCodeOffset()==pc[to],why);
    }
    /** Removing only the inspected gate must reconstruct every native instruction,
     * original branch target and exception region, including code after returns. */
    static void nativeBodyPreserved(Method method,int from,int count,String expected) {
        MutableMethodImplementation restored=new MutableMethodImplementation(method.getImplementation());
        for(int i=0;i<count;i++)restored.removeInstruction(from);
        check(expected.equals(TikTokNativeContract.digest(withBody(method,restored))),"all original native instructions, branches and try regions preserved: "+method.getDefiningClass());
    }
    static void helperGate(Method method) {
        List<Instruction> code=instructions(method);check(method.getAccessFlags()==17&&method.getImplementation().getRegisterCount()==18,"helper flags/registers unchanged");
        // Original v16=0 dominates both the comment guard and our false return.
        opcode(code,6,Opcode.CONST_16);register(code,6,16);
        check(((NarrowLiteralInstruction)code.get(6)).getNarrowLiteral()==0,"helper false-return register is initialized before all guards");
        invoke(code,8,Opcode.INVOKE_INTERFACE,"Lcom/ss/android/ugc/aweme/app/services/InterceptHomeBackPressService;","LIZ","","Z",0);
        invoke(code,21,Opcode.INVOKE_INTERFACE,"Lcom/ss/android/ugc/aweme/comment/services/CommentService;","LJLL","Landroid/content/Context;","Z",1,0);
        opcode(code,24,Opcode.IF_NEZ);register(code,24,0);
        opcode(code,25,Opcode.IGET_OBJECT);register(code,25,0);
        check(((TwoRegisterInstruction)code.get(25)).getRegisterB()==5,"helper reads the real native owner receiver");
        Reference field=((ReferenceInstruction)code.get(25)).getReference();
        check(field instanceof FieldReference&&HELPER_BASE.equals(((FieldReference)field).getDefiningClass())&&"LIZ".equals(((FieldReference)field).getName())&&"Landroid/app/Activity;".equals(((FieldReference)field).getType()),"helper passes its real Activity field");
        invoke(code,26,Opcode.INVOKE_STATIC,RUNTIME,"skipTikTokDoubleBack","Landroid/app/Activity;","Z",0);
        opcode(code,27,Opcode.MOVE_RESULT);register(code,27,0);
        branch(code,28,Opcode.IF_EQZ,0,30,"inactive gate resumes native timer after gate, never loops into gate");
        opcode(code,29,Opcode.RETURN);register(code,29,16);
        invoke(code,30,Opcode.INVOKE_STATIC,"Ljava/lang/System;","currentTimeMillis","","J");
        int[] pc=addresses(code);int commentTarget=pc[24]+((OffsetInstruction)code.get(24)).getCodeOffset();
        check(commentTarget>pc[30]&&commentTarget<pc[code.size()],"consumed comment branch skips timer gate entirely");
        int count=0;for(Instruction i:code)if(i instanceof ReferenceInstruction){Reference ref=((ReferenceInstruction)i).getReference();if(ref instanceof MethodReference&&RUNTIME.equals(((MethodReference)ref).getDefiningClass())&&"skipTikTokDoubleBack".equals(((MethodReference)ref).getName()))count++;}
        check(count==1,"helper contains exactly one guarded timer gate");
        nativeBodyPreserved(method,25,5,HELPER_BODY);
    }
    static void strategyGate(Method method) {
        List<Instruction> code=instructions(method);check(method.getAccessFlags()==17&&method.getImplementation().getRegisterCount()==13,"strategy flags/registers unchanged");
        invoke(code,3,Opcode.INVOKE_INTERFACE,"LX/0aGT;","onBackPressed","","Z",0);
        branch(code,5,Opcode.IF_NEZ,0,27,"native intercept handling returns to native cleanup without root hook");
        invoke(code,23,Opcode.INVOKE_INTERFACE,"LX/0aGT;","LIZ","","Z",1);
        branch(code,25,Opcode.IF_EQZ,0,31,"open-comment flag prevents root hook");
        invoke(code,35,Opcode.INVOKE_INTERFACE,"Lcom/ss/android/ugc/aweme/base/MainBusinessAbility;","f7","","Z",0);
        branch(code,37,Opcode.IF_EQZ,0,39,"swipe-up guide must decline before root hook");
        invoke(code,42,Opcode.INVOKE_INTERFACE,"Lcom/ss/android/ugc/aweme/base/MainBusinessAbility;","Pn","","Z",0);
        branch(code,41,Opcode.IF_EQZ,0,46,"missing native guide ability reaches gate, not the original post-gate instruction");
        branch(code,44,Opcode.IF_EQZ,0,46,"not-interested tutorial must decline before root hook");
        invoke(code,46,Opcode.INVOKE_STATIC,RUNTIME,"tryTikTokRootBack","Landroid/app/Activity;","Z",3);
        opcode(code,47,Opcode.MOVE_RESULT);register(code,47,0);
        branch(code,48,Opcode.IF_NEZ,0,27,"handled root Back reaches existing cleanup without another tab transition");
        invoke(code,49,Opcode.INVOKE_STATIC,"LX/082L;","LIZIZ","LX/02Hm;","Z",3);
        int count=0;for(Instruction i:code)if(i instanceof ReferenceInstruction){Reference ref=((ReferenceInstruction)i).getReference();if(ref instanceof MethodReference&&RUNTIME.equals(((MethodReference)ref).getDefiningClass())&&"tryTikTokRootBack".equals(((MethodReference)ref).getName()))count++;}
        check(count==1,"strategy contains exactly one root exit gate");
        nativeBodyPreserved(method,46,3,STRATEGY_BODY);
    }
    static void rejected4753(Map<String,ClassDef> classes) {
        check(TikTokNativeContract.method(classes.get(TikTokNativeContract.ABILITY),"IA0","Landroid/os/Bundle;,Ljava/lang/String;","V")!=null,"negative test input has actual 47.5.3 native ABI");
        check(TikTokNativeContract.method(classes.get(TikTokNativeContract.ABILITY),"tB0","Landroid/os/Bundle;,Ljava/lang/String;","V")==null,"negative input does not masquerade as 47.4.3 ABI");
        rejected("com.zhiliaoapp.musically","47.5.3",classes,"47.5.3 rejected even with genuine native methods");
        rejected("com.zhiliaoapp.musically","47.4.3",classes,"47.5.3 code relabelled 47.4.3 is rejected");
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
        invoke(code,4,Opcode.INVOKE_STATIC,RUNTIME,key?"enterKey":"enterTikTokBack",runtimeParams,"V",arguments);
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
        helperGate(TikTokNativeContract.method(after.get(NATIVE_HELPER),"onBackPressed","","Z"));
        strategyGate(TikTokNativeContract.method(after.get(NATIVE_STRATEGY),"onBackPress","","V"));
        ClassDef route=after.get("Lapp/hiro/oneback/runtime/TikTokHome;");check(route!=null,"TikTokHome runtime included");
        Method method=TikTokNativeContract.method(route,"route","Landroid/app/Activity;","Lapp/hiro/oneback/runtime/TikTokHome$Route;");
        check(method!=null,"TikTokHome route body present");Set<String> strings=new HashSet<String>();
        for(Instruction i:method.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)i).getReference();if(r instanceof StringReference)strings.add(((StringReference)r).getString());}
        for(String name:ROUTE)check(strings.contains(name),"output runtime uses real "+VERSION+" method "+name);
        for(String removed:new String[]{"IA0","Nu2","EH0","i33"})check(!strings.contains(removed),"output runtime no longer contains 47.5.3 route "+removed);
    }
    public static void main(String[] args)throws Exception {
        if(args.length<2||args.length>3)throw new IllegalArgumentException("fingerprints|original|output|reject4753 APK [47.4.3]");
        if(args.length==3)profile(args[2]);
        Map<String,ClassDef> classes=read(args[1],args[0].equals("output"));
        if(args[0].equals("fingerprints")){for(Map.Entry<String,String> e:TikTokNativeContract.fingerprints(VERSION,classes).entrySet())System.out.println(e.getKey()+" "+e.getValue());return;}
        if(args[0].equals("original"))admission(classes);else if(args[0].equals("output"))output(classes);else if(args[0].equals("reject4753"))rejected4753(classes);else throw new IllegalArgumentException(args[0]);
        System.out.println("PASS TikTok4753ContractAudit "+args[0]+": assertions="+assertions+"; version="+VERSION+"; real native API verified; device behavior not asserted");
    }
}
