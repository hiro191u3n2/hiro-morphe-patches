/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;
import java.util.*;
import java.util.zip.*;
import app.hiro.tiktok4753.patches.TikTok4753LegacyPatches;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;

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
    /** Independent expected target inventory recovered from the two original APKs. */
    static final class Targets {
        final String version,leftId,anchor,button,progress,seek,reason,inverse,view;
        final String previewGate,previewCheck,availability,initializer,loader,storyToggle,storyScene;
        final String[] exclusions;
        Targets(String version) {
            check(version.equals("47.4.3")||version.equals("47.5.3"),"unsupported native audit version "+version);
            this.version=version;boolean old=version.equals("47.4.3");
            leftId=old?"p1":"E1";anchor=old?"LX/07HS;":"LX/07Qm;";button=old?"LX/07Oc;":"LX/07Qi;";
            progress=old?"LX/06tY;":"LX/06Xo;";seek=old?"LX/06qw;":"LX/06WL;";reason=old?"LJI":"LJII";
            inverse=old?"LX/09X8;":"LX/09jc;";view=old?"LX/06oF;":"LX/06V9;";
            previewGate=old?"LJII":"LJIIIIZZ";previewCheck=old?"LJIJI":"LJIJJ";
            availability=old?"LX/0F2U;":"LX/0P1m;";initializer=old?"LX/06s5;":"LX/06Wk;";
            loader=old?"LX/0F2V;":"LX/0P1n;";storyToggle=old?"ar":"br";storyScene=old?"Yq":"Vq";
            String paid=old?"LX/09MK;":"LX/09Hp;", subtype=old?"LX/08Be;":"LX/07fd;";
            exclusions=new String[]{(old?"LX/09OF;":"LX/09Xa;")+"->LJFF",subtype+"->LJII",subtype+"->LJI",subtype+"->LJIIJ",
                "Lcom/ss/android/ugc/aweme/commercialize/promote/interfaces/ICommercePromoteService;->LJJ",
                "Lcom/ss/android/ugc/aweme/feed/model/AwemeSaaSExtKt;->isLive","Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;->isPhotoMode",paid+"->LJFF"};
        }
    }
    static Targets target;
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
    static String version(String file)throws Exception {
        try(ZipFile zip=new ZipFile(file);InputStream input=zip.getInputStream(zip.getEntry("AndroidManifest.xml"))){
            AndroidManifestBlock manifest=AndroidManifestBlock.load(input);
            check("com.zhiliaoapp.musically".equals(manifest.getPackageName()),"wrong native-audit APK package");
            String version=manifest.getVersionName();
            check(version.equals("47.4.3")||version.equals("47.5.3"),"unsupported native-audit APK version "+version);
            int expected=version.equals("47.4.3")?2024704030:2024705030;
            check(Integer.valueOf(expected).equals(manifest.getVersionCode()),"wrong native-audit APK version code");
            return version;
        }
    }
    static Input read(String file,boolean findFactory,String factoryOwner)throws Exception {
        Input result=new Input();
        check(target.version.equals(version(file)),"original and patched APK versions must match");
        Set<String> wanted=new HashSet<>(Arrays.asList(LEFT,STORY,UPVOTE,target.anchor,target.button,target.progress,target.seek,target.initializer,target.availability,target.inverse,target.view,R,factoryOwner));
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
    static void original(Input input)throws Exception {
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
            lifecycleRegression(m,owner.equals(STORY));
        }
        Method drag=method(c,target.progress,"LIZIZ",A,"Z");
        Scenario normal=new Scenario();normal.control=null;
        check(runPredicate(drag,normal)==0,"original experiment off with no control hides drag");
        normal.experiment=true;
        check(runPredicate(drag,normal)==1,"native experiment on already permits normal video without VideoControl");
        check(calls(method(c,target.seek,target.previewCheck,"","Z"),target.availability,"LIZ"),"real preview loader availability check present");
        Method initialize=method(c,target.initializer,"invoke","Ljava/lang/Object;","Ljava/lang/Object;");
        check(calls(initialize,target.seek,target.previewGate)&&calls(initialize,target.loader,"<init>")&&calls(initialize,target.availability,"LIZ"),
            "same preview gate guards native loader creation and real image availability");
        check(calls(method(c,target.availability,"LIZ","","Z"),"Lcom/ss/android/ugc/aweme/feed/model/Video;","getBigThumbs"),
            "real availability implementation checks backend thumbnail data");
    }

    /** Concrete dependency values for the native drag predicate, excluding UI behavior. */
    static final class Scenario {
        Object aweme=new Object(),control=new Object();boolean experiment,ad;
        int draft;String excluded;
    }
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
                if(Arrays.asList(target.exclusions).contains(name))result=name.equals(s.excluded);
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
    static int[] addresses(List<Instruction> body) {
        int[] values=new int[body.size()];int address=0;
        for(int i=0;i<body.size();i++){values[i]=address;address+=body.get(i).getCodeUnits();}
        return values;
    }
    static Method roundTrip(Method source)throws Exception {
        ClassDef holder=new ImmutableClassDef(source.getDefiningClass(),1,"Ljava/lang/Object;",Collections.emptyList(),null,
            Collections.emptySet(),Collections.emptyList(),Collections.singletonList(source));
        MemoryDataStore bytes=new MemoryDataStore();
        DexPool.writeTo(bytes,new ImmutableDexFile(Opcodes.getDefault(),Collections.singletonList(holder)));
        DexBackedDexFile read=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new ByteArrayInputStream(bytes.getData()));
        return read.getClasses().iterator().next().getMethods().iterator().next();
    }
    static void lifecycleRegression(Method original,boolean followingOnly)throws Exception {
        // Apply the real mutator to each version's actual lifecycle, serialize it, and
        // inspect the decoded branch destinations. This catches nonzero-offset label bugs.
        java.lang.reflect.Method accessor=TikTok4753LegacyPatches.class.getDeclaredMethod("rootAccessor",Method.class);
        java.lang.reflect.Method hook=TikTok4753LegacyPatches.class.getDeclaredMethod("hideRootAtReturns",MutableMethod.class,MethodReference.class,String.class);
        accessor.setAccessible(true);hook.setAccessible(true);
        MutableMethod changed=new MutableMethod(original);
        hook.invoke(null,changed,accessor.invoke(null,original),followingOnly?target.storyScene:null);
        Method encoded=roundTrip(changed);lifecycle(original,encoded,followingOnly);
        if(followingOnly){
            List<Instruction> broken=code(encoded);int branch=-1;
            for(int i=0;i<broken.size();i++)if(ref(broken.get(i)) instanceof MethodReference&&
                id((MethodReference)ref(broken.get(i))).equals(R+"->hide(Ljava/lang/Object;)V"))branch=i-3;
            check(branch>=0&&broken.get(branch).getOpcode()==Opcode.IF_NE,"regression fixture locates scene exit branch");
            // The previous failure jumped back into native initialization; that must be rejected.
            int[] offsets=addresses(broken);
            broken.set(branch,new ImmutableInstruction22t(Opcode.IF_NE,0,1,-offsets[branch]));
            Method mutation=new ImmutableMethod(encoded.getDefiningClass(),encoded.getName(),encoded.getParameters(),encoded.getReturnType(),encoded.getAccessFlags(),encoded.getAnnotations(),encoded.getHiddenApiRestrictions(),
                new ImmutableMethodImplementation(encoded.getImplementation().getRegisterCount(),broken,Collections.emptyList(),Collections.emptyList()));
            boolean rejected=false;try{lifecycle(original,mutation,true);}catch(AssertionError expected){rejected=expected.getMessage().equals("other scenes land on native return");}
            check(rejected,"regression must reject a scene branch into native initialization");
        }
    }
    static void lifecycle(Method before,Method after,boolean followingOnly) {
        List<Instruction> old=code(before),now=code(after);int[] mapped=new int[old.size()];
        int self=after.getImplementation().getRegisterCount()-2,j=0,returns=0;
        int[] oldAddresses=addresses(old),newAddresses=addresses(now);
        Map<Integer,Integer> oldAt=new HashMap<>();for(int i=0;i<old.size();i++)oldAt.put(oldAddresses[i],i);
        check(now.size()>old.size(),"lifecycle hook must add real instructions");
        // 47.4.3's left lifecycle has native branch blocks after its return. Walk the whole
        // body and verify every original edge, instead of assuming the return is last.
        for(int i=0;i<old.size();i++){
            mapped[i]=j;
            check(j<now.size(),"lifecycle native instruction disappeared "+i);
            if(old.get(i).getOpcode()!=Opcode.RETURN_VOID){
                check(key(old.get(i)).equals(key(now.get(j))),"native initialization changed at "+i+" "+before);j++;continue;
            }
            returns++;int root=j+(followingOnly?4:0),end=root+3;
            check(end<now.size(),"truncated lifecycle exit hook");
            Instruction first=now.get(j);check(first.getOpcode()==Opcode.INVOKE_VIRTUAL_RANGE,"exit hook uses a native accessor");
            check(((RegisterRangeInstruction)first).getStartRegister()==self&&((RegisterRangeInstruction)first).getRegisterCount()==1,"exit hook uses stable p0 only");
            MethodReference call=(MethodReference)ref(first);
            if(followingOnly){
                check(call.getDefiningClass().equals(STORY)&&call.getName().equals(target.storyScene)&&call.getReturnType().equals("I")&&arguments(call).isEmpty(),"following scene guard first");
                check(now.get(j+1).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)now.get(j+1)).getRegisterA()==0,"scene result register");
                check(now.get(j+2).getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)now.get(j+2)).getRegisterA()==1&&((NarrowLiteralInstruction)now.get(j+2)).getNarrowLiteral()==2,"only following scene 2");
                Instruction branch=now.get(j+3);check(branch.getOpcode()==Opcode.IF_NE&&((TwoRegisterInstruction)branch).getRegisterA()==0&&((TwoRegisterInstruction)branch).getRegisterB()==1,"other scenes bypass hide");
                check(newAddresses[j+3]+((OffsetInstruction)branch).getCodeOffset()==newAddresses[end],"other scenes land on native return");
            }
            Instruction rootCall=now.get(root);check(rootCall.getOpcode()==Opcode.INVOKE_VIRTUAL_RANGE&&((RegisterRangeInstruction)rootCall).getStartRegister()==self&&((RegisterRangeInstruction)rootCall).getRegisterCount()==1,"root getter reads p0");
            MethodReference rootRef=(MethodReference)ref(rootCall);
            check(rootRef.getName().equals("getContainerView")&&rootRef.getReturnType().equals(V)&&arguments(rootRef).isEmpty(),"hide native bound root, not reused argument");
            check(now.get(root+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT&&((OneRegisterInstruction)now.get(root+1)).getRegisterA()==0,"root result register");
            Instruction hide=now.get(root+2);check(hide.getOpcode()==Opcode.INVOKE_STATIC&&id((MethodReference)ref(hide)).equals(R+"->hide(Ljava/lang/Object;)V")&&Arrays.equals(invokeRegisters(hide),new int[]{0}),"only retrieved root passed to hide");
            check(now.get(end).getOpcode()==Opcode.RETURN_VOID,"lifecycle returns after hook");j=end+1;
        }
        check(j==now.size(),"unexpected lifecycle instructions outside exit hooks");
        check(returns>0&&callCount(after,R,"hide")==returns,"every native exit receives one target-only hide hook");
        for(int i=0;i<old.size();i++)if(old.get(i) instanceof OffsetInstruction){
            int destination=oldAddresses[i]+((OffsetInstruction)old.get(i)).getCodeOffset();
            Integer oldTarget=oldAt.get(destination);check(oldTarget!=null,"native lifecycle branch target is an instruction");
            Instruction written=now.get(mapped[i]);
            check(written instanceof OffsetInstruction&&newAddresses[mapped[i]]+((OffsetInstruction)written).getCodeOffset()==newAddresses[mapped[oldTarget]],"native lifecycle edge changed at "+i);
        }
    }
    static void output(Input original,Input patched) {
        Map<String,ClassDef> a=original.classes,b=patched.classes;Method factory=original.factories.get(0);
        check(b.containsKey(R),"independent LegacyHooks runtime absent");
        Method anchor=method(b,target.anchor,"LIZIZ",target.anchor+PRIORITY+"Ljava/util/List;","Ljava/lang/String;");
        List<Instruction> ac=code(anchor);check(ac.get(0).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)ac.get(0)).getNarrowLiteral()==0&&ac.get(1).getOpcode()==Opcode.RETURN_OBJECT,"legacy anchor selector must return null");
        emptyCollection(method(b,target.button,"LIZJ","Ljava/lang/String;Ljava/lang/String;"+PRIORITY,"Ljava/util/Set;"),"Ljava/util/LinkedHashSet;");
        emptyCollection(method(b,factory.getDefiningClass(),factory.getName(),arguments(factory),factory.getReturnType()),"Ljava/util/ArrayList;");
        constantResult(method(b,UPVOTE,"xa","Ljava/lang/Object;","Z"),0);
        lifecycle(method(a,LEFT,"onViewCreated",V,"V"),method(b,LEFT,"onViewCreated",V,"V"),false);
        lifecycle(method(a,STORY,"onViewCreated",V,"V"),method(b,STORY,"onViewCreated",V,"V"),true);
        Method storyToggle=method(b,STORY,target.storyToggle,"ZZ","V");retained(method(a,STORY,target.storyToggle,"ZZ","V"),storyToggle,"non-following story animation/native body retained");
        List<Instruction> sb=code(storyToggle);check(ref(sb.get(0)) instanceof MethodReference&&((MethodReference)ref(sb.get(0))).getName().equals(target.storyScene),"toggle scene checked before native animation");
        check(sb.get(2).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)sb.get(2)).getNarrowLiteral()==2&&sb.get(3).getOpcode()==Opcode.IF_NE,"toggle hide is following-only");
        int[] storyAddresses=addresses(sb);
        check(storyAddresses[3]+((OffsetInstruction)sb.get(3)).getCodeOffset()==storyAddresses[8],"other scenes reach original animation entry");
        check(callCount(storyToggle,R,"hide")==1,"following toggle has native runtime hook");
        Method progress=method(b,target.progress,"LIZJ",A,"Z"),drag=method(b,target.progress,"LIZIZ",A,"Z");
        Scenario normal=new Scenario();normal.control=null;check(runPredicate(progress,normal)==1,"normal video progress visible without server control");
        normal.aweme=null;check(runPredicate(progress,normal)==0,"null item remains non-video");
        retained(method(a,target.progress,"LIZIZ",A,"Z"),drag,"native drag exclusions retained");
        for(boolean experiment:Arrays.asList(false,true))for(boolean control:Arrays.asList(false,true))for(int draft:Arrays.asList(0,1)){
            Scenario s=new Scenario();s.experiment=experiment;s.control=control?new Object():null;s.draft=draft;
            check(runPredicate(drag,s)==1,"normal-video drag enabled across experiment/control/draft flags");
            for(String exclusion:target.exclusions){s.excluded=exclusion;s.ad=exclusion.contains("ICommercePromoteService");check(runPredicate(drag,s)==0,"native exclusion retained "+exclusion);}
        }
        Scenario missing=new Scenario();missing.aweme=null;check(runPredicate(drag,missing)==0,"null item remains not draggable");
        constantResult(method(b,target.inverse,"LIZ",A,"Z"),1);
        Method setter=method(b,target.view,"setSeekBarShowType","I","V");retained(method(a,target.view,"setSeekBarShowType","I","V"),setter,"native seekbar rendering body retained");
        List<Instruction> set=code(setter);check(ref(set.get(0)) instanceof MethodReference&&((MethodReference)ref(set.get(0))).getDefiningClass().equals(R)&&((MethodReference)ref(set.get(0))).getName().equals("seekbarType"),"setter normalizes native hide states");
        check(set.get(2).getOpcode()==Opcode.IF_NEZ&&set.get(3).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)set.get(3)).getNarrowLiteral()==-1&&set.get(4).getOpcode()==Opcode.IPUT,"type-zero path invalidates stored equality cache");
        check(((FieldReference)ref(set.get(4))).getName().equals("LLJJJIL"),"correct native show-type field invalidated");
        int[] setterAddresses=addresses(set);
        check(setterAddresses[2]+((OffsetInstruction)set.get(2)).getCodeOffset()==setterAddresses[5],"nonzero seekbar type reaches original setter entry");
        constantResult(method(b,target.seek,target.previewGate,"","Z"),1);
        sameBody(method(a,target.seek,target.previewCheck,"","Z"),method(b,target.seek,target.previewCheck,"","Z"),"real thumbnail availability guard unchanged");
        sameBody(method(a,target.initializer,"invoke","Ljava/lang/Object;","Ljava/lang/Object;"),method(b,target.initializer,"invoke","Ljava/lang/Object;","Ljava/lang/Object;"),"native thumbnail initialization and availability path unchanged");
        sameBody(method(a,target.availability,"LIZ","","Z"),method(b,target.availability,"LIZ","","Z"),"thumbnail images, URLs and URI availability checks unchanged");
        sameBody(method(a,target.seek,target.reason,A,"Z"),method(b,target.seek,target.reason,A,"Z"),"native current-video, mask and story safety orchestration unchanged");
        System.out.println("verified native outputs: legacy + modern anchors, bottom CTA, repost, left root, following stories, progress, drag exclusions, inverse gate, show-type cache, preview availability");
    }
    public static void main(String[] args)throws Exception {
        check(args.length==2||args.length==3,"original ORIGINAL | output ORIGINAL PATCHED");
        target=new Targets(version(args[1]));
        Input original=read(args[1],true,null);original(original);
        if(args[0].equals("output")){
            check(args.length==3,"output requires original and patched APKs");
            output(original,read(args[2],false,original.factories.get(0).getDefiningClass()));
        }else check(args[0].equals("original"),"unknown command");
        System.out.println("PASS LegacyNativeAudit "+args[0]+" version="+target.version+" assertions="+assertions+"; real DEX contracts and branch behavior checked; device UI not asserted");
    }
}
