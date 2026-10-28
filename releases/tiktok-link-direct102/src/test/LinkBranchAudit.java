/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;

/** Independent original/output bytecode audit, not an Android UI execution test.
 * Only the dedicated popup eligibility factory constant may differ in these classes.
 * CFG exploration fixes only the proven dedicated eligibility result (and its
 * legacy interception result); every other conditional stays nondeterministic.
 * If the popup remains reachable by any such path, publication must fail.
 */
public final class LinkBranchAudit {
    static final String TARGET="LX/0ICN;";
    static final String MODERN="LX/0aK3;";
    static final String LEGACY="Lcom/ss/android/ugc/aweme/feed/helper/RecFeedShareComponentController;";
    static final String HELPER="Lcom/ss/android/ugc/aweme/feed/ui/share/SharedVideoDeepLinkHelper;";
    static final String OPERATOR="LX/0aKI;";
    static final String SHA="af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b";
    static final Set<String> WANTED=new HashSet<>(Arrays.asList(TARGET,MODERN,HELPER,OPERATOR,"LX/0aKB;","LX/0aK4;","LX/09nu;","LX/0ICM;","LX/0ICO;","LX/17u4;",LEGACY,"LX/07yL;"));
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static String methodId(MethodReference m){StringBuilder b=new StringBuilder(m.getName()).append('(');for(CharSequence p:m.getParameterTypes())b.append(p);return b.append(')').append(m.getReturnType()).toString();}
    static Map<String,Method> methods(ClassDef c){Map<String,Method> r=new TreeMap<>();for(Method m:c.getMethods())check(r.put(methodId(m),m)==null,"duplicate method");return r;}
    static Method target(Map<String,ClassDef> classes){return methods(classes.get(TARGET)).get("invoke()Ljava/lang/Object;");}
    static AndroidManifestBlock manifest(ZipFile z)throws Exception{return AndroidManifestBlock.load(z.getInputStream(z.getEntry("AndroidManifest.xml")));}
    static Map<String,ClassDef> read(String path)throws Exception {
        Map<String,ClassDef> result=new TreeMap<>();
        try(ZipFile z=new ZipFile(path)){
            AndroidManifestBlock m=manifest(z);
            check("com.zhiliaoapp.musically".equals(m.getPackageName()),"wrong APK package");
            check("47.4.3".equals(m.getVersionName()),"wrong APK version");
            check(Integer.valueOf(2024704030).equals(m.getVersionCode()),"wrong APK version code");
            for(Enumeration<? extends ZipEntry> it=z.entries();it.hasMoreElements();){
                ZipEntry e=it.nextElement();if(!e.getName().matches("classes(?:[0-9]+)?\\.dex"))continue;
                try(InputStream in=new BufferedInputStream(z.getInputStream(e))){
                    DexBackedDexFile dex=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),in);
                    for(ClassDef c:dex.getClasses())if(WANTED.contains(c.getType()))
                        check(result.put(c.getType(),ImmutableClassDef.of(c))==null,"duplicate selected class "+c.getType());
                }
            }
        }
        check(result.keySet().equals(WANTED),"missing audited native class "+result.keySet());return result;
    }
    static void pin(String path)throws Exception {
        check(Files.size(Paths.get(path))==474921201L,"wrong original APK length");
        MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] buf=new byte[1048576];
        try(InputStream in=Files.newInputStream(Paths.get(path))){for(int n;(n=in.read(buf))!=-1;)md.update(buf,0,n);}
        StringBuilder b=new StringBuilder();for(byte x:md.digest())b.append(String.format("%02x",x));
        check(SHA.equals(b.toString()),"wrong original APK digest");
    }
    static final class Code {
        final Method method;final List<Instruction> ins=new ArrayList<>();final List<Integer> address=new ArrayList<>();final Map<Integer,Integer> index=new HashMap<>();
        int size;
        Code(Method m){method=m;check(m!=null&&m.getImplementation()!=null,"missing method body");for(Instruction i:m.getImplementation().getInstructions()){index.put(size,ins.size());address.add(size);ins.add(i);size+=i.getCodeUnits();}index.put(size,ins.size());}
        int at(int addr){Integer n=index.get(addr);check(n!=null,"unaligned code address "+addr);return n;}
        int destination(int i){return at(address.get(i)+((OffsetInstruction)ins.get(i)).getCodeOffset());}
        String key(int n){
            Instruction i=ins.get(n);StringBuilder s=new StringBuilder(i.getOpcode()==Opcode.CONST_STRING_JUMBO?"CONST_STRING":i.getOpcode().name());
            if(i instanceof OneRegisterInstruction)s.append(" a=").append(((OneRegisterInstruction)i).getRegisterA());
            if(i instanceof TwoRegisterInstruction)s.append(" b=").append(((TwoRegisterInstruction)i).getRegisterB());
            if(i instanceof ThreeRegisterInstruction)s.append(" c=").append(((ThreeRegisterInstruction)i).getRegisterC());
            if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction q=(FiveRegisterInstruction)i;s.append(" regs=").append(q.getRegisterCount()).append(':').append(q.getRegisterC()).append(',').append(q.getRegisterD()).append(',').append(q.getRegisterE()).append(',').append(q.getRegisterF()).append(',').append(q.getRegisterG());}
            if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction q=(RegisterRangeInstruction)i;s.append(" range=").append(q.getStartRegister()).append(',').append(q.getRegisterCount());}
            if(i instanceof WideLiteralInstruction)s.append(" literal=").append(((WideLiteralInstruction)i).getWideLiteral());
            if(i instanceof OffsetInstruction)s.append(" target-index=").append(destination(n));
            if(i instanceof ReferenceInstruction)s.append(" ref=").append(ref(i));
            if(i instanceof DualReferenceInstruction)s.append(" ref2=").append(((DualReferenceInstruction)i).getReference2());
            if(i instanceof SwitchPayload)for(SwitchElement e:((SwitchPayload)i).getSwitchElements())s.append(" switch=").append(e.getKey()).append(':').append(e.getOffset());
            if(i instanceof ArrayPayload){ArrayPayload p=(ArrayPayload)i;s.append(" width=").append(p.getElementWidth()).append(" data=").append(p.getArrayElements());}
            return s.toString();
        }
        List<String> keys(){List<String> r=new ArrayList<>();for(int i=0;i<ins.size();i++)r.add(key(i));return r;}
        List<String> tries(){List<String> r=new ArrayList<>();for(TryBlock<? extends ExceptionHandler> t:method.getImplementation().getTryBlocks()){StringBuilder b=new StringBuilder().append(at(t.getStartCodeAddress())).append('-').append(at(t.getStartCodeAddress()+t.getCodeUnitCount()));for(ExceptionHandler h:t.getExceptionHandlers())b.append('|').append(h.getExceptionType()).append(':').append(at(h.getHandlerCodeAddress()));r.add(b.toString());}return r;}
    }
    static int originalDecision(Code c){
        check(c.ins.size()==14,"dedicated popup factory structure changed");
        check(c.method.getImplementation().getRegisterCount()==3,"factory registers changed");
        check(ref(c.ins.get(0)).equals("LX/17u4;->LIZ:LX/02o5;"),"raw style source changed");
        check(ref(c.ins.get(1)).equals("LX/02o5;->getValue()Ljava/lang/Object;"),"raw style lazy read changed");
        check(ref(c.ins.get(4)).equals("Ljava/lang/Number;->intValue()I"),"raw style conversion changed");
        check(c.ins.get(6).getOpcode()==Opcode.CONST_4&&((WideLiteralInstruction)c.ins.get(6)).getWideLiteral()==4,"style comparison differs");
        check(c.ins.get(7).getOpcode()==Opcode.IF_NE&&c.destination(7)==12,"factory predicate branch differs");
        check(c.address.get(8)==0xf&&c.ins.get(8).getOpcode()==Opcode.CONST_4
            &&((OneRegisterInstruction)c.ins.get(8)).getRegisterA()==0
            &&((WideLiteralInstruction)c.ins.get(8)).getWideLiteral()==1,"factory true result differs");
        check(ref(c.ins.get(9)).equals("Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;"),"factory boxing changed");
        check(c.ins.get(12).getOpcode()==Opcode.CONST_4&&((WideLiteralInstruction)c.ins.get(12)).getWideLiteral()==0,"factory false result differs");
        check(c.ins.get(13).getOpcode()==Opcode.GOTO&&c.destination(13)==9,"factory false join changed");
        check(c.method.getImplementation().getTryBlocks().isEmpty(),"unexpected factory exception handlers");
        return 8;
    }
    /** Executes only the emitted DEX factory instructions for both partitions of
     * its int==4 predicate. No Java reimplementation of its return expression. */
    static boolean evaluate(Code c,int raw){
        Object[] regs=new Object[c.method.getImplementation().getRegisterCount()];Object result=null;int n=0;
        for(int steps=0;steps<40;steps++){
            Instruction i=c.ins.get(n);Opcode op=i.getOpcode();int next=n+1;
            if(op==Opcode.SGET_OBJECT){check(ref(i).equals("LX/17u4;->LIZ:LX/02o5;"),"unexpected factory static read");regs[((OneRegisterInstruction)i).getRegisterA()]="style-lazy";}
            else if(op==Opcode.INVOKE_INTERFACE){check(ref(i).equals("LX/02o5;->getValue()Ljava/lang/Object;"),"unexpected factory interface call");result=Integer.valueOf(raw);}
            else if(op==Opcode.INVOKE_VIRTUAL){check(ref(i).equals("Ljava/lang/Number;->intValue()I"),"unexpected factory virtual call");result=((Number)regs[((FiveRegisterInstruction)i).getRegisterC()]).intValue();}
            else if(op==Opcode.INVOKE_STATIC){check(ref(i).equals("Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;"),"unexpected factory static call");result=Boolean.valueOf(((Number)regs[((FiveRegisterInstruction)i).getRegisterC()]).intValue()!=0);}
            else if(op==Opcode.MOVE_RESULT||op==Opcode.MOVE_RESULT_OBJECT)regs[((OneRegisterInstruction)i).getRegisterA()]=result;
            else if(op==Opcode.CHECK_CAST){check(ref(i).equals("Ljava/lang/Number;"),"unexpected factory cast");check(regs[((OneRegisterInstruction)i).getRegisterA()] instanceof Number,"bad factory value type");}
            else if(op==Opcode.CONST_4)regs[((OneRegisterInstruction)i).getRegisterA()]=Integer.valueOf((int)((WideLiteralInstruction)i).getWideLiteral());
            else if(op==Opcode.IF_NE){TwoRegisterInstruction q=(TwoRegisterInstruction)i;if(!regs[q.getRegisterA()].equals(regs[q.getRegisterB()]))next=c.destination(n);}
            else if(op==Opcode.GOTO)next=c.destination(n);
            else if(op==Opcode.RETURN_OBJECT){Object x=regs[((OneRegisterInstruction)i).getRegisterA()];check(x instanceof Boolean,"factory did not return Boolean");return (Boolean)x;}
            else throw new AssertionError("unsupported factory instruction "+op);
            n=next;
        }
        throw new AssertionError("factory did not terminate");
    }
    static Set<Integer> reachable(Code c,Map<Integer,Integer> forced){
        Set<Integer> seen=new HashSet<>();ArrayDeque<Integer> todo=new ArrayDeque<>();todo.add(0);
        while(!todo.isEmpty()){
            int n=todo.remove();check(n>=0&&n<c.ins.size(),"flow exits method without return");if(!seen.add(n))continue;
            Instruction i=c.ins.get(n);Opcode op=i.getOpcode();String name=op.name();
            check(!(i instanceof SwitchPayload)&&!(i instanceof ArrayPayload),"execution enters data payload");
            if(forced.containsKey(n)){check(name.startsWith("IF_"),"forced value is not at a conditional");todo.add(forced.get(n));}
            else if(name.startsWith("GOTO")){todo.add(c.destination(n));}
            else if(name.startsWith("IF_")){todo.add(c.destination(n));todo.add(n+1);}
            else if(op==Opcode.PACKED_SWITCH||op==Opcode.SPARSE_SWITCH){
                Instruction p=c.ins.get(c.destination(n));check(p instanceof SwitchPayload,"bad switch payload");
                for(SwitchElement e:((SwitchPayload)p).getSwitchElements())todo.add(c.at(c.address.get(n)+e.getOffset()));todo.add(n+1);
            }else if(!name.startsWith("RETURN")&&op!=Opcode.THROW){todo.add(n+1);}
            if(op.canThrow())for(TryBlock<? extends ExceptionHandler> t:c.method.getImplementation().getTryBlocks())
                if(c.address.get(n)>=t.getStartCodeAddress()&&c.address.get(n)<t.getStartCodeAddress()+t.getCodeUnitCount())
                    for(ExceptionHandler h:t.getExceptionHandlers())todo.add(c.at(h.getHandlerCodeAddress()));
        }
        return seen;
    }
    static boolean reachableReference(Code c,Set<Integer> reach,String value){for(int n:reach)if(ref(c.ins.get(n)).equals(value))return true;return false;}
    static int gateDecision(Code c,int expectedAddress){
        int gate=-1;
        for(int i=0;i<c.ins.size();i++)if(ref(c.ins.get(i)).equals("LX/0ICM;->LIZIZ:LX/02o5;")){check(gate<0,"ambiguous consumer flag read");gate=i;}
        check(gate>=0&&ref(c.ins.get(gate+1)).equals("LX/02o5;->getValue()Ljava/lang/Object;")
            &&ref(c.ins.get(gate+4)).equals("Ljava/lang/Boolean;->booleanValue()Z"),"consumer eligibility read changed");
        check(c.ins.get(gate+5).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)c.ins.get(gate+5)).getRegisterA()==0,"consumer bool result changed");
        int decision=c.at(expectedAddress);
        check(c.ins.get(decision).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)c.ins.get(decision)).getRegisterA()==0,"consumer false branch changed");
        for(int i=gate+6;i<decision;i++)if(c.ins.get(i).getOpcode().setsRegister())
            check(!(c.ins.get(i) instanceof OneRegisterInstruction)||((OneRegisterInstruction)c.ins.get(i)).getRegisterA()!=0,"consumer overwrites false flag before branch");
        return decision;
    }
    static void nativePaths(Map<String,ClassDef> classes,boolean popupEnabled){
        Code modern=new Code(methods(classes.get(MODERN)).get("invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;"));
        int m=gateDecision(modern,0x4d);Map<Integer,Integer> forced=new HashMap<>();
        forced.put(m,popupEnabled?m+1:modern.destination(m));Set<Integer> reach=reachable(modern,forced);
        check(reachableReference(modern,reach,"LX/0aKB;"),"native video insertion path is unreachable");
        check(reachableReference(modern,reach,"LX/0aK4;"),"native API-inserted refresh path is unreachable");
        check(reachableReference(modern,reach,OPERATOR+"->LIZIZ()V"),"native old-dialog cleanup is unreachable");
        boolean constructor=false,show=false;
        for(int n:reach){Instruction i=modern.ins.get(n);if(i.getOpcode()==Opcode.NEW_INSTANCE&&ref(i).equals(OPERATOR))constructor=true;if(ref(i).equals(OPERATOR+"->LIZ()V"))show=true;}
        check(constructor==popupEnabled,"unexpected modern popup construction reachability");check(show==popupEnabled,"unexpected modern popup show reachability");
        Code legacy=new Code(methods(classes.get(LEGACY)).get("LJ(LX/02Hm;)Z"));int l=gateDecision(legacy,0x6c);
        forced.clear();forced.put(l,popupEnabled?l+1:legacy.destination(l));reach=reachable(legacy,forced);
        check(reachableReference(legacy,reach,"intercept shared logic. ")==popupEnabled,"legacy intercept reachability differs");
        check(reachableReference(legacy,reach,"not intercept shared logic. "),"native ordinary shared-link handling is unreachable");
        if(!popupEnabled){
            check(legacy.ins.get(0).getOpcode()==Opcode.CONST_4
                &&((OneRegisterInstruction)legacy.ins.get(0)).getRegisterA()==5
                &&((WideLiteralInstruction)legacy.ins.get(0)).getWideLiteral()==0,"legacy false result initialization changed");
            for(int n:reach){
                Instruction i=legacy.ins.get(n);
                if(i.getOpcode().setsRegister()&&i instanceof OneRegisterInstruction&&((OneRegisterInstruction)i).getRegisterA()==5)
                    check(i.getOpcode()==Opcode.CONST_4&&((WideLiteralInstruction)i).getWideLiteral()==0,"reachable legacy path changes the false result");
                if(i.getOpcode()==Opcode.RETURN)check(((OneRegisterInstruction)i).getRegisterA()==5,"legacy returns a different result register");
            }
        }
        Code observer=new Code(methods(classes.get("LX/07yL;")).get("onChanged(Ljava/lang/Object;)V"));int o=gateDecision(observer,0x111);
        forced.clear();forced.put(o,popupEnabled?o+1:observer.destination(o));
        if(!popupEnabled){int q=observer.at(0xd8);check(observer.ins.get(q).getOpcode()==Opcode.IF_EQZ,"legacy interception result branch changed");forced.put(q,observer.destination(q));}
        reach=reachable(observer,forced);
        check(reachableReference(observer,reach,"aweme://aweme/detail/"),"legacy native video route is unreachable");
        check(reachableReference(observer,reach,"Lcom/bytedance/router/SmartRoute;->open()V"),"legacy native video open is unreachable");
        check(reachableReference(observer,reach,"Lcom/ss/android/ugc/aweme/feed/ui/FeedRecommendFragment;->OX(Ljava/lang/Boolean;)V")==popupEnabled,"legacy dialog-specific branch reachability differs");
    }
    static void classShape(ClassDef before,ClassDef after){
        check(before.getType().equals(after.getType())&&before.getAccessFlags()==after.getAccessFlags()
            &&Objects.equals(before.getSuperclass(),after.getSuperclass())&&before.getInterfaces().equals(after.getInterfaces()),"class header changed "+before.getType());
        Map<String,String> a=new TreeMap<>(),b=new TreeMap<>();
        for(Field f:before.getFields())a.put(f.getName()+':'+f.getType(),f.getAccessFlags()+":"+f.getInitialValue()+":"+f.getAnnotations());
        for(Field f:after.getFields())b.put(f.getName()+':'+f.getType(),f.getAccessFlags()+":"+f.getInitialValue()+":"+f.getAnnotations());
        check(a.equals(b),"fields changed "+before.getType());
        check(before.getAnnotations().equals(after.getAnnotations()),"class annotations changed "+before.getType());
    }
    static int compare(Map<String,ClassDef> before,Map<String,ClassDef> after,int decision){
        int count=0;
        for(String owner:WANTED){
            ClassDef old=before.get(owner),now=after.get(owner);classShape(old,now);
            Map<String,Method> a=methods(old),b=methods(now);check(a.keySet().equals(b.keySet()),"method inventory changed "+owner);
            for(String id:a.keySet()){
                Method x=a.get(id),y=b.get(id);count++;
                check(x.getAccessFlags()==y.getAccessFlags()&&x.getAnnotations().equals(y.getAnnotations()),"method header changed "+owner+id);
                check((x.getImplementation()==null)==(y.getImplementation()==null),"method body presence changed");
                if(x.getImplementation()==null)continue;
                Code xc=new Code(x),yc=new Code(y);
                check(x.getImplementation().getRegisterCount()==y.getImplementation().getRegisterCount(),"register count changed "+owner+id);
                check(xc.ins.size()==yc.ins.size()&&xc.tries().equals(yc.tries()),"code/try structure changed "+owner+id);
                List<String> expected=xc.keys(),actual=yc.keys();
                if(owner.equals(TARGET)&&id.equals("invoke()Ljava/lang/Object;")){
                    check(yc.ins.get(decision).getOpcode()==Opcode.CONST_4
                        &&((OneRegisterInstruction)yc.ins.get(decision)).getRegisterA()==0
                        &&((WideLiteralInstruction)yc.ins.get(decision)).getWideLiteral()==0,"new disabled-popup factory result is absent");
                    check(yc.ins.get(decision).getCodeUnits()==xc.ins.get(decision).getCodeUnits(),"decision width changed");
                    expected.set(decision,"CONST_4 a=0 literal=0");
                }
                check(expected.equals(actual),"unreviewed instruction change "+owner+id);
            }
        }
        return count;
    }
    static void verifyFactory(Code factory,boolean patched){
        for(int raw:new int[]{Integer.MIN_VALUE,-1,0,1,2,3,4,5,99,Integer.MAX_VALUE})
            check(evaluate(factory,raw)==(!patched&&raw==4),"factory returns wrong eligibility for raw style "+raw);
    }
    static void factoryBinding(Map<String,ClassDef> classes){
        Code initializer=new Code(methods(classes.get("LX/0ICM;")).get("<clinit>()V"));
        int factory=-1;
        for(int i=0;i<initializer.ins.size();i++)if(initializer.ins.get(i).getOpcode()==Opcode.NEW_INSTANCE&&ref(initializer.ins.get(i)).equals(TARGET)){check(factory<0,"factory bound more than once");factory=i;}
        check(factory>=0&&ref(initializer.ins.get(factory+1)).equals(TARGET+"-><init>()V")
            &&ref(initializer.ins.get(factory+2)).equals("LX/02o3;->LIZJ(Lkotlin/jvm/functions/Function0;)LX/02o5;")
            &&ref(initializer.ins.get(factory+4)).equals("LX/0ICM;->LIZIZ:LX/02o5;"),"dedicated popup-lazy binding changed");
    }
    public static void main(String[] args)throws Exception{
        check(args.length>=2&&(args[0].equals("original")||args[0].equals("output")),"Usage: original APK | output ORIGINAL PATCHED");
        pin(args[1]);Map<String,ClassDef> original=read(args[1]);Code code=new Code(target(original));int decision=originalDecision(code);
        factoryBinding(original);verifyFactory(code,false);nativePaths(original,true);nativePaths(original,false);
        if(args[0].equals("original")){
            check(args.length==2,"unexpected arguments");
            System.out.println("PASS LinkBranchAudit original: exact input digest; dedicated factory0ICN style==4 predicate; all three consumers verified; true can reach popup and false reaches native video paths; not an Android device test");return;
        }
        check(args.length==3||args.length==4,"patched APK and optional baseline OneBack APK required");
        Map<String,ClassDef> reference=args.length==4?read(args[3]):original;
        Code referenceFactory=new Code(target(reference));
        check(originalDecision(referenceFactory)==decision,"baseline changed popup decision");
        verifyFactory(referenceFactory,false);
        Map<String,ClassDef> patched=read(args[2]);int methodCount=compare(reference,patched,decision);
        factoryBinding(patched);verifyFactory(new Code(target(patched)),true);nativePaths(patched,false);
        System.out.println("PASS LinkBranchAudit output: one CONST_4 1->0 in dedicated popup factory; "+WANTED.size()+" native classes and "+methodCount+" methods audited; all three consumers, helper pause lifecycle, raw style and unrelated invite factory preserved; popup paths unreachable under false eligibility; native direct paths reachable; checks="+checks+"; not an Android device test");
    }
}
