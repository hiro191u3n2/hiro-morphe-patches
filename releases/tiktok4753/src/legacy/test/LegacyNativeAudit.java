/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;
import java.util.*;
import java.util.zip.*;
import app.hiro.tiktok4753.patches.TikTok4753LegacyPatches;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;

/** Real input/output DEX audit. Decision fixtures execute the emitted instructions,
 * not a second Java copy of the seekbar predicate. This is not a device UI test. */
public final class LegacyNativeAudit {
    static final String R = "Lapp/hiro/tiktok4753/runtime/LegacyHooks;";
    static final String A = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;";
    static final String V = "Landroid/view/View;";
    static final String LEFT = "Lcom/ss/android/ugc/feed/platform/cell/interact/info/InteractInfoAreaComponent;";
    static final String STORY = "Lcom/ss/android/ugc/aweme/base/ui/assem/FeedSkylightBubbleAssem;";
    static final String UPVOTE = "Lcom/ss/android/ugc/aweme/upvote/detail/whitebar/UpvoteVideoAssemNew;";
    static final String PRIORITY = "Lcom/ss/android/ugc/feed/platform/componentmanager/data/FeedComponentPriorityData;";
    static int assertions;
    static void check(boolean yes,String why) { assertions++; if(!yes)throw new AssertionError(why); }
    static List<Instruction> code(Method m) {
        check(m!=null&&m.getImplementation()!=null,"method body absent "+m);
        List<Instruction> result=new ArrayList<>();for(Instruction i:m.getImplementation().getInstructions())result.add(i);return result;
    }
    static Reference ref(Instruction i) { return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference():null; }
    static String arguments(MethodReference m){StringBuilder s=new StringBuilder();for(CharSequence p:m.getParameterTypes())s.append(p);return s.toString();}
    static String id(MethodReference m){return m.getDefiningClass()+"->"+m.getName()+"("+arguments(m)+")"+m.getReturnType();}
    static Method method(Map<String,ClassDef> classes,String owner,String name,String parameters,String returns) {
        ClassDef c=classes.get(owner);check(c!=null,"native class absent "+owner);Method found=null;
        for(Method m:c.getMethods())if(m.getName().equals(name)&&arguments(m).equals(parameters)&&m.getReturnType().equals(returns)){check(found==null,"duplicate method "+owner+name);found=m;}
        check(found!=null,"native method absent "+owner+"->"+name+"("+parameters+")"+returns);return found;
    }
    static final class Input { final Map<String,ClassDef> classes=new TreeMap<>(); final List<Method> factories=new ArrayList<>(); }
    static Input read(String file,boolean findFactory,String factoryOwner)throws Exception {
        Input result=new Input();
        Set<String> wanted=new HashSet<>(Arrays.asList(LEFT,STORY,UPVOTE,"LX/07Qm;","LX/07Qi;","LX/06Xo;","LX/06WL;","LX/06Wk;","LX/0P1m;","LX/09jc;","LX/06V9;",R,factoryOwner));
        try(ZipFile zip=new ZipFile(file)){
            Enumeration<? extends ZipEntry> entries=zip.entries();
            while(entries.hasMoreElements()){
                ZipEntry e=entries.nextElement();if(!e.getName().matches("classes(?:[0-9]+)?\\.dex"))continue;
                try(InputStream input=new BufferedInputStream(zip.getInputStream(e))){
                    DexBackedDexFile dex=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),input);
                    for(ClassDef c:dex.getClasses()){
                        boolean keep=wanted.contains(c.getType());
                        if(findFactory)for(Method m:c.getMethods())if(TikTok4753LegacyPatches.isModernAnchorFactory(m)){
                            result.factories.add(ImmutableMethod.of(m));keep=true;
                        }
                        if(keep)check(result.classes.put(c.getType(),ImmutableClassDef.of(c))==null,"duplicate selected class "+c.getType());
                    }
                }
            }
        }
        return result;
    }
    static boolean calls(Method m,String owner,String name) {
        for(Instruction i:code(m)){Reference r=ref(i);if(r instanceof MethodReference){MethodReference x=(MethodReference)r;if(x.getDefiningClass().equals(owner)&&x.getName().equals(name))return true;}}return false;
    }
    static int callCount(Method m,String owner,String name) {
        int count=0;for(Instruction i:code(m)){Reference r=ref(i);if(r instanceof MethodReference){MethodReference x=(MethodReference)r;if(x.getDefiningClass().equals(owner)&&x.getName().equals(name))count++;}}return count;
    }
    static String key(Instruction i) {
        StringBuilder s=new StringBuilder(i.getOpcode().name);
        if(i instanceof OneRegisterInstruction)s.append(" a=").append(((OneRegisterInstruction)i).getRegisterA());
        if(i instanceof TwoRegisterInstruction)s.append(" b=").append(((TwoRegisterInstruction)i).getRegisterB());
        if(i instanceof ThreeRegisterInstruction)s.append(" c=").append(((ThreeRegisterInstruction)i).getRegisterC());
        if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction q=(FiveRegisterInstruction)i;s.append(" regs=").append(q.getRegisterCount()).append(':').append(q.getRegisterC()).append(',').append(q.getRegisterD()).append(',').append(q.getRegisterE()).append(',').append(q.getRegisterF()).append(',').append(q.getRegisterG());}
        if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction q=(RegisterRangeInstruction)i;s.append(" range=").append(q.getStartRegister()).append(',').append(q.getRegisterCount());}
        if(i instanceof WideLiteralInstruction)s.append(" literal=").append(((WideLiteralInstruction)i).getWideLiteral());
        if(ref(i)!=null)s.append(" ref=").append(ref(i));
        return s.toString();
    }
    static List<String> keys(List<Instruction> values) {List<String> result=new ArrayList<>();for(Instruction i:values)if(i.getOpcode()!=Opcode.NOP)result.add(key(i));return result;}
    static void sameBody(Method before,Method after,String why) {check(keys(code(before)).equals(keys(code(after))),why);}
    static void retained(Method before,Method after,String why) {
        List<String> old=keys(code(before)),now=keys(code(after));int i=0;
        for(String value:now)if(i<old.size()&&value.equals(old.get(i)))i++;
        check(i==old.size(),why+" original instruction sequence removed at "+i+"/"+old.size());
    }
    static void original(Input input) {
        Map<String,ClassDef> c=input.classes;
        check(input.factories.size()==1,"need exactly one actual modern anchor renderer, found "+input.factories.size());
        Method factory=input.factories.get(0);System.out.println("native modern anchors: "+id(factory));
        // A matching descriptor must not be enough: removing either independent native anchor rejects it.
        for(String methodName:Arrays.asList("getAnchors","getComponentKey")){
            List<Instruction> altered=code(factory);boolean changed=false;
            for(int i=0;i<altered.size();i++){Reference r=ref(altered.get(i));if(r instanceof MethodReference&&((MethodReference)r).getName().equals(methodName)){altered.set(i,new ImmutableInstruction10x(Opcode.NOP));changed=true;}}
            check(changed,"mutation hit "+methodName);
            Method mutation=new ImmutableMethod(factory.getDefiningClass(),factory.getName(),factory.getParameters(),factory.getReturnType(),factory.getAccessFlags(),factory.getAnnotations(),factory.getHiddenApiRestrictions(),new ImmutableMethodImplementation(factory.getImplementation().getRegisterCount(),altered,Collections.emptyList(),Collections.emptyList()));
            check(!TikTok4753LegacyPatches.isModernAnchorFactory(mutation),"changed body with identical signature must be rejected "+methodName);
        }
        for(String owner:Arrays.asList(LEFT,STORY)){
            Method m=method(c,owner,"onViewCreated",V,"V");int self=m.getImplementation().getRegisterCount()-2;
            check(TikTok4753LegacyPatches.registerIsPreserved(m,self),"actual stable lifecycle p0 "+owner);
            List<Instruction> altered=code(m);altered.add(0,new ImmutableInstruction11n(Opcode.CONST_4,self,0));
            Method mutation=new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),new ImmutableMethodImplementation(m.getImplementation().getRegisterCount(),altered,Collections.emptyList(),Collections.emptyList()));
            check(!TikTok4753LegacyPatches.registerIsPreserved(mutation,self),"reject overwritten lifecycle p0 "+owner);
        }
        Method drag=method(c,"LX/06Xo;","LIZIZ",A,"Z");
        Scenario normal=new Scenario();normal.control=null;
        check(runPredicate(drag,normal)==0,"original experiment off with no control hides drag");
        normal.experiment=true;
        check(runPredicate(drag,normal)==1,"native experiment on already permits normal video without VideoControl");
        check(calls(method(c,"LX/06WL;","LJIJJ","","Z"),"LX/0P1m;","LIZ"),"real preview loader availability check present");
        Method initialize=method(c,"LX/06Wk;","invoke","Ljava/lang/Object;","Ljava/lang/Object;");
        check(calls(initialize,"LX/06WL;","LJIIIIZZ")&&calls(initialize,"LX/0P1n;","<init>")&&calls(initialize,"LX/0P1m;","LIZ"),
            "same preview gate guards native loader creation and real image availability");
        check(calls(method(c,"LX/0P1m;","LIZ","","Z"),"Lcom/ss/android/ugc/aweme/feed/model/Video;","getBigThumbs"),
            "real availability implementation checks backend thumbnail data");
    }

    /** Concrete dependency values for the native drag predicate, excluding UI behavior. */
    static final class Scenario {
        Object aweme=new Object(),control=new Object();boolean experiment,ad;
        int draft;String excluded;
    }
    static final String[] EXCLUSIONS={"LX/09Xa;->LJFF","LX/07fd;->LJII","LX/07fd;->LJI","LX/07fd;->LJIIJ",
        "Lcom/ss/android/ugc/aweme/commercialize/promote/interfaces/ICommercePromoteService;->LJJ",
        "Lcom/ss/android/ugc/aweme/feed/model/AwemeSaaSExtKt;->isLive","Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;->isPhotoMode","LX/09Hp;->LJFF"};
    static boolean zero(Object value){return value==null||Boolean.FALSE.equals(value)||(value instanceof Number&&((Number)value).longValue()==0);}
    static int[] invokeRegisters(Instruction i) {
        if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction q=(RegisterRangeInstruction)i;int[] r=new int[q.getRegisterCount()];for(int j=0;j<r.length;j++)r[j]=q.getStartRegister()+j;return r;}
        FiveRegisterInstruction q=(FiveRegisterInstruction)i;return Arrays.copyOf(new int[]{q.getRegisterC(),q.getRegisterD(),q.getRegisterE(),q.getRegisterF(),q.getRegisterG()},q.getRegisterCount());
    }
    static int runPredicate(Method method,Scenario s) {
        List<Instruction> instructions=code(method);Map<Integer,Integer> address=new HashMap<>();int[] offsets=new int[instructions.size()];int n=0;
        for(int i=0;i<instructions.size();i++){address.put(n,i);offsets[i]=n;n+=instructions.get(i).getCodeUnits();}
        Object[] r=new Object[method.getImplementation().getRegisterCount()];r[r.length-1]=s.aweme;Object result=null;int at=0;
        for(int fuel=0;fuel<1000;fuel++){
            Instruction i=instructions.get(at);Opcode op=i.getOpcode();int next=at+1;
            if(op==Opcode.CONST_4||op==Opcode.CONST_16||op==Opcode.CONST)r[((OneRegisterInstruction)i).getRegisterA()]=((NarrowLiteralInstruction)i).getNarrowLiteral();
            else if(op==Opcode.MOVE_RESULT||op==Opcode.MOVE_RESULT_OBJECT)r[((OneRegisterInstruction)i).getRegisterA()]=result;
            else if(op==Opcode.IF_EQZ||op==Opcode.IF_NEZ){boolean z=zero(r[((OneRegisterInstruction)i).getRegisterA()]);if(op==Opcode.IF_EQZ?z:!z)next=address.get(offsets[at]+((OffsetInstruction)i).getCodeOffset());}
            else if(op==Opcode.RETURN)return zero(r[((OneRegisterInstruction)i).getRegisterA()])?0:1;
            else if(op==Opcode.SGET_OBJECT)r[((OneRegisterInstruction)i).getRegisterA()]=new Object();
            else if(op==Opcode.CONST_CLASS)r[((OneRegisterInstruction)i).getRegisterA()]=new Object();
            else if(op==Opcode.CHECK_CAST||op==Opcode.NOP){}
            else if(op==Opcode.IGET){FieldReference f=(FieldReference)ref(i);check(f.getName().equals("draftProgressBar")||f.getName().equals("showProgressBar"),"unknown interpreted field "+f);r[((OneRegisterInstruction)i).getRegisterA()]=s.draft;}
            else if(op.name.startsWith("invoke-")){
                MethodReference call=(MethodReference)ref(i);String name=call.getDefiningClass()+"->"+call.getName();int[] registers=invokeRegisters(i);
                if(Arrays.asList(EXCLUSIONS).contains(name))result=name.equals(s.excluded);
                else if(name.equals(A+"->getVideoControl"))result=s.control;
                else if(name.equals(A+"->getAwemeRawAd"))result=s.ad?new Object():null;
                else if(name.equals("LX/02o5;->getValue"))result=s.experiment;
                else if(name.equals("Ljava/lang/Boolean;->booleanValue"))result=r[registers[0]];
                else if(name.equals("Ljava/lang/Object;->getClass")||name.equals("Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->get")||name.equals("Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->getService"))result=new Object();
                else throw new AssertionError("Unmodeled native dependency "+call);
            }else throw new AssertionError("Unmodeled predicate opcode "+op);
            at=next;
        }
        throw new AssertionError("predicate did not terminate");
    }
    static void constantResult(Method m,int expected) {
        List<Instruction> b=code(m);check(b.get(0).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)b.get(0)).getNarrowLiteral()==expected&&b.get(1).getOpcode()==Opcode.RETURN,"unconditional result mismatch "+m);
    }
    static void emptyCollection(Method m,String type) {
        List<Instruction> b=code(m);check(b.get(0).getOpcode()==Opcode.NEW_INSTANCE&&ref(b.get(0)).toString().equals(type),"fresh empty collection type "+m);
        check(b.get(1).getOpcode()==Opcode.INVOKE_DIRECT&&((MethodReference)ref(b.get(1))).getName().equals("<init>")&&arguments((MethodReference)ref(b.get(1))).isEmpty(),"empty collection constructor "+m);
        check(b.get(2).getOpcode()==Opcode.RETURN_OBJECT,"empty collection returned "+m);
    }
    static void lifecycle(Method before,Method after,boolean followingOnly) {
        List<Instruction> old=code(before),now=code(after);int beforeEnd=old.size()-1;
        check(old.get(beforeEnd).getOpcode()==Opcode.RETURN_VOID,"original lifecycle has final return");
        check(now.size()>old.size(),"lifecycle hook must add real instructions");
        // These audited lifecycle methods have one exit, so retaining this prefix proves
        // superclass initialization, listeners and view model subscriptions still run.
        for(int i=0;i<beforeEnd;i++)check(key(old.get(i)).equals(key(now.get(i))),"native initialization changed at "+i+" "+before);
        for(int i=0;i<beforeEnd;i++)check(old.get(i).getOpcode()!=Opcode.RETURN_VOID,"unexpected earlier lifecycle exit");
        check(callCount(after,R,"hide")==1,"one target-only hide call "+after);
        Instruction first=now.get(beforeEnd);check(first.getOpcode()==Opcode.INVOKE_VIRTUAL_RANGE,"exit hook obtains native container using stable this");
        check(((RegisterRangeInstruction)first).getStartRegister()==after.getImplementation().getRegisterCount()-2,"exit hook uses p0");
        MethodReference call=(MethodReference)ref(first);
        if(followingOnly){
            check(call.getName().equals("Vq"),"following scene guard first");
            check(now.get(beforeEnd+2).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)now.get(beforeEnd+2)).getNarrowLiteral()==2,"only following scene 2");
            check(now.get(beforeEnd+3).getOpcode()==Opcode.IF_NE,"other scenes bypass hide");
        }else check(call.getName().equals("getContainerView"),"hide native bound root, not an arbitrary reused parameter");
    }
    static void output(Input original,Input patched) {
        Map<String,ClassDef> a=original.classes,b=patched.classes;Method factory=original.factories.get(0);
        check(b.containsKey(R),"independent LegacyHooks runtime absent");
        Method anchor=method(b,"LX/07Qm;","LIZIZ","LX/07Qm;"+PRIORITY+"Ljava/util/List;","Ljava/lang/String;");
        List<Instruction> ac=code(anchor);check(ac.get(0).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)ac.get(0)).getNarrowLiteral()==0&&ac.get(1).getOpcode()==Opcode.RETURN_OBJECT,"legacy anchor selector must return null");
        emptyCollection(method(b,"LX/07Qi;","LIZJ","Ljava/lang/String;Ljava/lang/String;"+PRIORITY,"Ljava/util/Set;"),"Ljava/util/LinkedHashSet;");
        emptyCollection(method(b,factory.getDefiningClass(),factory.getName(),arguments(factory),factory.getReturnType()),"Ljava/util/ArrayList;");
        constantResult(method(b,UPVOTE,"xa","Ljava/lang/Object;","Z"),0);
        lifecycle(method(a,LEFT,"onViewCreated",V,"V"),method(b,LEFT,"onViewCreated",V,"V"),false);
        lifecycle(method(a,STORY,"onViewCreated",V,"V"),method(b,STORY,"onViewCreated",V,"V"),true);
        Method storyToggle=method(b,STORY,"br","ZZ","V");retained(method(a,STORY,"br","ZZ","V"),storyToggle,"non-following story animation/native body retained");
        List<Instruction> sb=code(storyToggle);check(ref(sb.get(0)) instanceof MethodReference&&((MethodReference)ref(sb.get(0))).getName().equals("Vq"),"toggle scene checked before native animation");
        check(sb.get(2).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)sb.get(2)).getNarrowLiteral()==2&&sb.get(3).getOpcode()==Opcode.IF_NE,"toggle hide is following-only");
        check(callCount(storyToggle,R,"hide")==1,"following toggle has native runtime hook");
        Method progress=method(b,"LX/06Xo;","LIZJ",A,"Z"),drag=method(b,"LX/06Xo;","LIZIZ",A,"Z");
        Scenario normal=new Scenario();normal.control=null;check(runPredicate(progress,normal)==1,"normal video progress visible without server control");
        normal.aweme=null;check(runPredicate(progress,normal)==0,"null item remains non-video");
        retained(method(a,"LX/06Xo;","LIZIZ",A,"Z"),drag,"native drag exclusions retained");
        for(boolean experiment:Arrays.asList(false,true))for(boolean control:Arrays.asList(false,true))for(int draft:Arrays.asList(0,1)){
            Scenario s=new Scenario();s.experiment=experiment;s.control=control?new Object():null;s.draft=draft;
            check(runPredicate(drag,s)==1,"normal-video drag enabled across experiment/control/draft flags");
            for(String exclusion:EXCLUSIONS){s.excluded=exclusion;s.ad=exclusion.contains("ICommercePromoteService");check(runPredicate(drag,s)==0,"native exclusion retained "+exclusion);}
        }
        Scenario missing=new Scenario();missing.aweme=null;check(runPredicate(drag,missing)==0,"null item remains not draggable");
        constantResult(method(b,"LX/09jc;","LIZ",A,"Z"),1);
        Method setter=method(b,"LX/06V9;","setSeekBarShowType","I","V");retained(method(a,"LX/06V9;","setSeekBarShowType","I","V"),setter,"native seekbar rendering body retained");
        List<Instruction> set=code(setter);check(ref(set.get(0)) instanceof MethodReference&&((MethodReference)ref(set.get(0))).getDefiningClass().equals(R)&&((MethodReference)ref(set.get(0))).getName().equals("seekbarType"),"setter normalizes native hide states");
        check(set.get(2).getOpcode()==Opcode.IF_NEZ&&set.get(3).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)set.get(3)).getNarrowLiteral()==-1&&set.get(4).getOpcode()==Opcode.IPUT,"type-zero path invalidates stored equality cache");
        check(((FieldReference)ref(set.get(4))).getName().equals("LLJJJIL"),"correct native show-type field invalidated");
        constantResult(method(b,"LX/06WL;","LJIIIIZZ","","Z"),1);
        sameBody(method(a,"LX/06WL;","LJIJJ","","Z"),method(b,"LX/06WL;","LJIJJ","","Z"),"real thumbnail availability guard unchanged");
        sameBody(method(a,"LX/06Wk;","invoke","Ljava/lang/Object;","Ljava/lang/Object;"),method(b,"LX/06Wk;","invoke","Ljava/lang/Object;","Ljava/lang/Object;"),"native thumbnail initialization and availability path unchanged");
        sameBody(method(a,"LX/0P1m;","LIZ","","Z"),method(b,"LX/0P1m;","LIZ","","Z"),"thumbnail images, URLs and URI availability checks unchanged");
        sameBody(method(a,"LX/06WL;","LJII",A,"Z"),method(b,"LX/06WL;","LJII",A,"Z"),"native current-video, mask and story safety orchestration unchanged");
        System.out.println("verified native outputs: legacy + modern anchors, bottom CTA, repost, left root, following stories, progress, drag exclusions, inverse gate, show-type cache, preview availability");
    }
    public static void main(String[] args)throws Exception {
        check(args.length==2||args.length==3,"original ORIGINAL | output ORIGINAL PATCHED");
        Input original=read(args[1],true,null);original(original);
        if(args[0].equals("output")){
            check(args.length==3,"output requires original and patched APKs");
            output(original,read(args[2],false,original.factories.get(0).getDefiningClass()));
        }else check(args[0].equals("original"),"unknown command");
        System.out.println("PASS LegacyNativeAudit "+args[0]+" assertions="+assertions+"; real DEX contracts and branch behavior checked; device UI not asserted");
    }
}
