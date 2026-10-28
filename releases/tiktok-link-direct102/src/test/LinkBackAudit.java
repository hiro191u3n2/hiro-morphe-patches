/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;

/** Independent inspection of ORIGINAL and actual rebuilt APK DEX. Production
 * patch helpers are never imported. Native Back is interpreted from its emitted
 * instructions, with explicit external service results. This is a host contract
 * check, not Android execution, a phone Back test, or a latency measurement. */
public final class LinkBackAudit {
    static final String R="Lapp/hiro/tiktoklink/runtime/SharedLinkExit;";
    static final String RP="Lapp/hiro/tiktoklink/runtime/", OP="Lapp/hiro/oneback/";
    static final String F="Lcom/ss/android/ugc/aweme/detail/ui/saf/DetailSafRootFragment;";
    static final String CHILD="Lcom/ss/android/ugc/aweme/detail/ui/DetailFragment;";
    static final String FACTORY="LIZ(Landroid/app/Activity;Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Landroid/content/Intent;";
    static final String PIPE="LIZJ(LX/1Sxc;Ljava/util/HashMap;LX/15dt;LX/1Sxm;)Lkotlin/Pair;";
    static final String TRANSFER=R+"->transfer(Landroid/content/Intent;Landroid/content/Intent;)V";
    static final String PREPARED=R+"->isPrepared(Landroid/content/Intent;)Z";
    static final String EXIT=R+"->tryExit(Ljava/lang/Object;)Z";
    static final String BIND=R+"->bind(Ljava/lang/Object;)V";
    static final String FORGET=R+"->forget(Ljava/lang/Object;)V";
    static final String SUPPRESS=R+"->suppressNativeReturn(Ljava/lang/Object;)Z";
    static final String NAV="LX/0yWM;->LJIILJJIL()V";
    static final Set<String> NATIVE=new TreeSet<String>(LinkFastRouteAudit.WANTED);
    static {NATIVE.addAll(Arrays.asList("LX/03uJ;","LX/06fM;","LX/19WL;",CHILD));}
    static long checks;static int backExecutions;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static String ref(Instruction i){return LinkFastRouteAudit.ref(i);}
    static String id(MethodReference m){return LinkFastRouteAudit.id(m);}
    static LinkFastRouteAudit.Code code(Map<String,ClassDef> cs,String owner,String name){return new LinkFastRouteAudit.Code(LinkFastRouteAudit.method(cs,owner,name));}
    static int a(Instruction i){return ((OneRegisterInstruction)i).getRegisterA();}
    static int[] args(Instruction i){if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;int[] v={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};return Arrays.copyOf(v,r.getRegisterCount());}RegisterRangeInstruction r=(RegisterRangeInstruction)i;int[] v=new int[r.getRegisterCount()];for(int n=0;n<v.length;n++)v[n]=r.getStartRegister()+n;return v;}
    static void call(Instruction i,String target,int... registers){check((i.getOpcode()==Opcode.INVOKE_STATIC||i.getOpcode()==Opcode.INVOKE_STATIC_RANGE)&&ref(i).equals(target)&&Arrays.equals(args(i),registers),"wrong emitted call/operands: "+target);}
    static void result(Instruction i,int register){check(i.getOpcode()==Opcode.MOVE_RESULT&&a(i)==register,"wrong result register");}
    static void sameRemainder(LinkFastRouteAudit.Code old,LinkFastRouteAudit.Code now,int at,int count,boolean entryLabel){
        check(now.ins.size()==old.ins.size()+count,"unreviewed instruction count delta in "+old.method);
        LinkFastRouteAudit.IndexMap map=n->{if(n>=at&&n<at+count){if(entryLabel&&n==at)return at;throw new AssertionError("native edge enters an injected instruction interior: "+old.method+"@"+n);}return n>=at+count?n-count:n;};
        List<String> expected=old.keys(LinkFastRouteAudit.IDENTITY,-1,0),actual=now.keys(map,at,count);
        check(expected.equals(actual),"native remainder changed "+old.method+LinkFastRouteAudit.firstDifference(expected,actual));
        check(old.tries(LinkFastRouteAudit.IDENTITY).equals(now.tries(map)),"native handler coverage changed "+old.method);
    }

    /** Called by LinkFastRouteAudit as well, so it never broadly exempts a class. */
    static boolean delta(String owner,String name,LinkFastRouteAudit.Code old,LinkFastRouteAudit.Code now){
        if(name.equals(FACTORY)&&(owner.equals("LX/1SyC;")||owner.equals("LX/1SyE;")||owner.equals("LX/1SyG;"))){
            int[] v=owner.equals("LX/1SyC;")?new int[]{0x52f,665,39,10,11,0x35b,11}:owner.equals("LX/1SyE;")?new int[]{0x127,153,26,0,2,0xd8,8}:new int[]{0x7f,64,18,0,4,0x6c,4};
            int k=old.at(v[0]);check(k==v[1],"native transfer boundary moved");
            Instruction move=now.ins.get(k);check(move.getOpcode()==Opcode.MOVE_OBJECT_FROM16&&a(move)==v[3]&&((TwoRegisterInstruction)move).getRegisterB()==v[2],"temporary Intent alias not preserved");
            call(now.ins.get(k+1),TRANSFER,v[3],v[4]);sameRemainder(old,now,k,2,false);
            Instruction previous=old.ins.get(k-1);check(ref(previous).equals("Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;")&&args(previous)[0]==v[4],"transfer must follow native final target write");
            int saved=old.at(v[5]);Instruction alias=old.ins.get(saved);check(alias.getOpcode()==Opcode.MOVE_OBJECT_FROM16&&a(alias)==v[2]&&((TwoRegisterInstruction)alias).getRegisterB()==v[6],"native saved temporary identity changed");
            check(reachingDefinitions(old,v[2],k).equals(Collections.singleton(saved)),"temporary register can hold another value on a route reaching transfer: "+owner+" "+reachingDefinitions(old,v[2],k));
            return true;
        }
        if(owner.equals("LX/1SxW;")&&name.equals(PIPE)){
            int k=old.at(0x127);check(k==149,"native transition boundary moved");call(now.ins.get(k),PREPARED,12);result(now.ins.get(k+1),6);
            check(now.ins.get(k+2).getOpcode()==Opcode.IF_EQZ&&a(now.ins.get(k+2))==6&&now.destination(k+2)==k+4,"unprepared transition must enter native animation call");
            check(now.ins.get(k+3).getOpcode()==Opcode.CONST_4&&a(now.ins.get(k+3))==0&&((NarrowLiteralInstruction)now.ins.get(k+3)).getNarrowLiteral()==0,"prepared animation must use 0/0");
            check(old.ins.get(k-1).getOpcode()==Opcode.CONST&&a(old.ins.get(k-1))==0&&((NarrowLiteralInstruction)old.ins.get(k-1)).getNarrowLiteral()==0x7f0200af,"native animation resource changed");
            check(ref(old.ins.get(k)).equals("LX/00Dv;->LIZIZ(IILandroid/content/Context;)LX/01Yv;")&&Arrays.equals(args(old.ins.get(k)),new int[]{0,0,7}),"native animation call changed");sameRemainder(old,now,k,4,false);
            for(boolean accepted:new boolean[]{false,true}){int resource=0x7f0200af,n=k;int[] registers=new int[26];registers[0]=resource;int steps=0;while(n<k+4){check(++steps<8,"animation gate loop");Instruction i=now.ins.get(n);switch(i.getOpcode()){case INVOKE_STATIC:n++;break;case MOVE_RESULT:registers[a(i)]=accepted?1:0;n++;break;case IF_EQZ:n=registers[a(i)]==0?now.destination(n):n+1;break;case CONST_4:registers[a(i)]=((NarrowLiteralInstruction)i).getNarrowLiteral();n++;break;default:throw new AssertionError("unexpected animation gate");}}check(n==k+4&&registers[0]==(accepted?0:resource),"actual transition operands differ");}
            return true;
        }
        if(!owner.equals(F))return false;
        if(name.equals("onBackPressed()Z")){
            int k=old.at(0xb3);check(k==94&&ref(old.ins.get(k)).equals(NAV)&&Arrays.equals(args(old.ins.get(k)),new int[]{0}),"native navigation boundary moved");
            call(now.ins.get(k),EXIT,6);result(now.ins.get(k+1),1);
            check(now.ins.get(k+2).getOpcode()==Opcode.IF_EQZ&&a(now.ins.get(k+2))==1&&now.destination(k+2)==k+4,"declined exit must retain exact native navigation");
            check(now.ins.get(k+3).getOpcode()==Opcode.RETURN&&a(now.ins.get(k+3))==3,"accepted exit must return native consumed true");sameRemainder(old,now,k,4,false);return true;
        }
        if(name.equals("bY()V")){
            call(now.ins.get(0),SUPPRESS,24);result(now.ins.get(1),0);
            check(now.ins.get(2).getOpcode()==Opcode.IF_EQZ&&a(now.ins.get(2))==0&&now.destination(2)==4&&now.ins.get(3).getOpcode()==Opcode.RETURN_VOID,"native return suppression gate differs");sameRemainder(old,now,0,4,false);return true;
        }
        if(name.equals("onResume()V")){
            check(old.ins.size()==109&&old.ins.get(108).getOpcode()==Opcode.RETURN_VOID,"original resume completion changed");call(now.ins.get(108),BIND,11);sameRemainder(old,now,108,1,true);
            check(now.ins.get(109).getOpcode()==Opcode.RETURN_VOID,"bind must finish at original native return");
            check(!reachable(now,0,Collections.singleton(108)).contains(109),"a native branch can bypass lifecycle binding");
            int arrivals=0;for(int n=0;n<old.ins.size();n++)if(old.ins.get(n) instanceof OffsetInstruction&&!(old.ins.get(n) instanceof SwitchPayload)&&old.destination(n)==108){arrivals++;check(now.destination(n)==108,"old return branch must now target bind");}check(arrivals==4,"expected four native early completion branches");return true;
        }
        if(name.equals("onDestroy()V")){
            call(now.ins.get(0),FORGET,14);check(now.ins.get(1).getOpcode()==Opcode.NOP,"destroy prefix padding must preserve payload alignment");sameRemainder(old,now,0,2,false);return true;
        }
        return false;
    }

    static List<Integer> successors(LinkFastRouteAudit.Code c,int n){
        Instruction i=c.ins.get(n);String op=i.getOpcode().name();List<Integer> r=new ArrayList<Integer>();
        if(op.startsWith("GOTO"))r.add(c.destination(n));
        else if(op.startsWith("IF_")){r.add(c.destination(n));if(n+1<c.ins.size())r.add(n+1);}
        else if(i.getOpcode()==Opcode.PACKED_SWITCH||i.getOpcode()==Opcode.SPARSE_SWITCH){SwitchPayload p=(SwitchPayload)c.ins.get(c.destination(n));for(SwitchElement e:p.getSwitchElements())r.add(c.at(c.addresses.get(n)+e.getOffset()));if(n+1<c.ins.size())r.add(n+1);}
        else if(!op.startsWith("RETURN")&&!op.equals("THROW")&&i.getOpcode().canContinue()&&n+1<c.ins.size())r.add(n+1);
        if(i.getOpcode().canThrow())for(TryBlock<? extends ExceptionHandler> t:c.method.getImplementation().getTryBlocks())if(c.addresses.get(n)>=t.getStartCodeAddress()&&c.addresses.get(n)<t.getStartCodeAddress()+t.getCodeUnitCount())for(ExceptionHandler h:t.getExceptionHandlers())r.add(c.at(h.getHandlerCodeAddress()));
        return r;
    }
    static Set<Integer> reachable(LinkFastRouteAudit.Code c,int start,Set<Integer> removed){Set<Integer> seen=new HashSet<Integer>();ArrayDeque<Integer> todo=new ArrayDeque<Integer>();todo.add(start);while(!todo.isEmpty()){int n=todo.remove();if(removed.contains(n)||!seen.add(n))continue;check(n>=0&&n<c.ins.size(),"CFG leaves native method");todo.addAll(successors(c,n));}return seen;}
    static Set<Integer> reachingDefinitions(LinkFastRouteAudit.Code c,int register,int at){
        List<Set<Integer>> incoming=new ArrayList<Set<Integer>>();for(int n=0;n<c.ins.size();n++)incoming.add(new HashSet<Integer>());incoming.get(0).add(-1);ArrayDeque<Integer> todo=new ArrayDeque<Integer>();todo.add(0);
        while(!todo.isEmpty()){int n=todo.remove();Set<Integer> next=new HashSet<Integer>(incoming.get(n));Instruction i=c.ins.get(n);if(i.getOpcode().setsRegister()&&i instanceof OneRegisterInstruction&&a(i)==register){next.clear();next.add(n);}for(int to:successors(c,n))if(incoming.get(to).addAll(next))todo.add(to);}
        return incoming.get(at);
    }

    static Map<String,ClassDef> read(String file)throws Exception{
        Map<String,ClassDef> out=new TreeMap<String,ClassDef>();try(ZipFile z=new ZipFile(file)){
            AndroidManifestBlock manifest=AndroidManifestBlock.load(z.getInputStream(z.getEntry("AndroidManifest.xml")));
            check("com.zhiliaoapp.musically".equals(manifest.getPackageName())&&"47.4.3".equals(manifest.getVersionName())&&Integer.valueOf(2024704030).equals(manifest.getVersionCode()),"APK identity differs");
            for(Enumeration<? extends ZipEntry> es=z.entries();es.hasMoreElements();){ZipEntry e=es.nextElement();if(!e.getName().matches("classes(?:[0-9]+)?\\.dex"))continue;try(InputStream in=new BufferedInputStream(z.getInputStream(e))){DexBackedDexFile dex=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),in);for(ClassDef c:dex.getClasses())if(NATIVE.contains(c.getType())||c.getType().startsWith("Lapp/hiro/"))check(out.put(c.getType(),ImmutableClassDef.of(c))==null,"duplicate audited class");}}
        }check(out.keySet().containsAll(NATIVE),"missing native back/route classes");return out;
    }
    static byte[] canonical(Collection<? extends ClassDef> classes)throws Exception{MemoryDataStore store=new MemoryDataStore();try{DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),classes));return Arrays.copyOf(store.getBuffer(),store.getSize());}finally{store.close();}}
    static Map<String,ClassDef> namespace(Map<String,ClassDef> classes,String prefix){Map<String,ClassDef> result=new TreeMap<String,ClassDef>();for(Map.Entry<String,ClassDef> e:classes.entrySet())if(e.getKey().startsWith(prefix))result.put(e.getKey(),e.getValue());return result;}
    static void runtime(Map<String,ClassDef> reference,Map<String,ClassDef> output,boolean combined)throws Exception{
        Map<String,ClassDef> before=namespace(reference,"Lapp/hiro/"),after=namespace(output,"Lapp/hiro/");
        for(String n:new ArrayList<String>(after.keySet()))if(n.startsWith(RP))after.remove(n);
        check(before.keySet().equals(after.keySet())&&Arrays.equals(canonical(before.values()),canonical(after.values())),"preexisting runtime classes changed or disappeared");
        check(combined==reference.containsKey(OP+"runtime/OneBackExit;"),"combined reference must actually contain the OneBack runtime");
        Map<String,ClassDef> shared=namespace(output,RP);check(shared.containsKey(R)&&shared.size()>=3,"new runtime extension absent or incomplete");
        Map<String,Method> api=LinkFastRouteAudit.methods(shared.get(R));
        for(String signature:Arrays.asList("prepare(Landroid/content/Intent;Landroid/net/Uri;)V","transfer(Landroid/content/Intent;Landroid/content/Intent;)V","isPrepared(Landroid/content/Intent;)Z","bind(Ljava/lang/Object;)V","forget(Ljava/lang/Object;)V","tryExit(Ljava/lang/Object;)Z","suppressNativeReturn(Ljava/lang/Object;)Z")){
            Method m=api.get(signature);check(m!=null&&(m.getAccessFlags()&9)==9&&m.getImplementation()!=null,"runtime public ABI missing "+signature);
        }
        int kills=0;for(ClassDef c:shared.values())for(Method m:c.getMethods()){
            check(!m.getName().startsWith("hiroOneBack$"),"OneBack rewrote the shared exit runtime");if(m.getImplementation()==null)continue;LinkFastRouteAudit.Code body=new LinkFastRouteAudit.Code(m);
            for(int n=0;n<body.ins.size();n++){Instruction i=body.ins.get(n);String s=ref(i);check(!s.contains(OP),"new runtime acquired OneBack-only dependency");
                if(s.equals("Landroid/os/Process;->killProcess(I)V")){kills++;check(n>=2&&ref(body.ins.get(n-2)).equals("Landroid/os/Process;->myPid()I")&&body.ins.get(n-1).getOpcode()==Opcode.MOVE_RESULT&&Arrays.equals(args(i),new int[]{a(body.ins.get(n-1))}),"kill does not target exactly its own current PID");}
                check(!s.equals("Ljava/lang/System;->exit(I)V")&&!s.contains("->forceStopPackage("),"unrequested exit mechanism");
                if(i instanceof ReferenceInstruction&&((ReferenceInstruction)i).getReference() instanceof MethodReference){String method=((MethodReference)((ReferenceInstruction)i).getReference()).getName();check(!method.equals("registerOnBackInvokedCallback")&&!method.equals("onBackPressed"),"runtime must not register or synthesize another native Back");}
            }
        }check(kills==1,"expected exactly one own-process kill site");
    }

    static void original(Map<String,ClassDef> cs){
        LinkFastRouteAudit.Code back=code(cs,F,"onBackPressed()Z");check(back.method.getImplementation().getRegisterCount()==7&&back.at(0xb3)==94&&ref(back.ins.get(94)).equals(NAV),"original native Back gate contract differs");
        check(back.ins.get(back.at(0x8c)).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)back.ins.get(back.at(0x8c))).getNarrowLiteral()==1&&back.destination(back.at(0xb6))==back.at(0x8c),"native navigation must resolve to consumed true");
        LinkFastRouteAudit.Code route=code(cs,F,"eY()Landroid/content/Intent;");check(LinkFastRouteAudit.contains(route,"LX/06fM;->LJFF(Landroidx/fragment/app/Fragment;)Landroid/content/Intent;"),"SAF-specific Intent accessor missing");
        String nativeEntry=LinkFastRouteAudit.methods(cs.get("LX/03uJ;")).containsKey("hiroOneBack$onBackPressed()V")?"hiroOneBack$onBackPressed()V":"onBackPressed()V";
        LinkFastRouteAudit.Code nativeBack=code(cs,"LX/03uJ;",nativeEntry);check(LinkFastRouteAudit.contains(nativeBack,"LX/04UE;->onBackPressed()Z"),"native Activity does not dispatch root fragment Back");
        if(!nativeEntry.equals("onBackPressed()V"))check(LinkFastRouteAudit.contains(code(cs,"LX/03uJ;","onBackPressed()V"),"LX/03uJ;->"+nativeEntry),"OneBack wrapper does not invoke the exact preserved native Back helper");
        LinkFastRouteAudit.Code child=code(cs,CHILD,"LJIILJJIL()V");boolean onBack=false;for(Instruction i:child.ins)onBack|=ref(i).equals("onBack");check(onBack,"native child navigation event missing");
        for(String owner:Arrays.asList("LX/1SyC;","LX/1SyE;","LX/1SyG;")){LinkFastRouteAudit.Code factory=code(cs,owner,FACTORY);int saved=owner.equals("LX/1SyC;")?0x35b:owner.equals("LX/1SyE;")?0xd8:0x6c;int reg=owner.equals("LX/1SyC;")?39:owner.equals("LX/1SyE;")?26:18;int transfer=owner.equals("LX/1SyC;")?0x52f:owner.equals("LX/1SyE;")?0x127:0x7f;check(reachingDefinitions(factory,reg,factory.at(transfer)).equals(Collections.singleton(factory.at(saved))),"original temporary Intent provenance is not unique: "+owner);}
    }

    static final class Scenario {
        final String name;boolean model=true,navArg=true,thumbnail,thumbnailController=true,key,secondaryKey,keyFeature,keyThrows,comment,child=true,valid=true,childConsumes,paused,accept;
        Scenario(String s){name=s;}
    }
    static final class Run {int returned=-1,probes,navigation;final List<String> trace=new ArrayList<String>();}
    enum Native {MODEL,NAV_ARG,THUMBNAIL,COMMENT_ANCHOR,COMMENT_SERVICE,LOGGER,CHILD,LISTENER,ACTIVITY,EVENT}
    static final class BackVm {
        final LinkFastRouteAudit.Code c;final Scenario s;final Run out=new Run();Object pending,exception;
        BackVm(LinkFastRouteAudit.Code code,Scenario scenario){c=code;s=scenario;}
        int integer(Object v){if(v==null)return 0;if(v instanceof Boolean)return(Boolean)v?1:0;return((Number)v).intValue();}
        boolean zero(Object v){return v==null||(v instanceof Number&&((Number)v).longValue()==0)||(v instanceof Boolean&&!(Boolean)v);}
        boolean eq(Object x,Object y){if(zero(x)||zero(y))return zero(x)&&zero(y);if(x instanceof Number&&y instanceof Number)return((Number)x).longValue()==((Number)y).longValue();return x==y;}
        Object field(String name){switch(name){case "LLLJ":return s.model?Native.MODEL:null;case "LLLIL":return s.thumbnailController?Native.THUMBNAIL:null;case "LLJZ":return s.key||s.secondaryKey||s.keyThrows?new ArrayList<Object>(Collections.singletonList(Native.LISTENER)):null;case "LLJLLIL":return Native.LOGGER;case "LLJLLL":return s.child?Native.CHILD:null;default:throw new AssertionError("unmodeled native Back field "+name);}}
        Object invoke(String r,Object[] v){
            if(r.equals(EXIT)){check(v.length==1&&v[0]==s,"gate must receive current native fragment");out.probes++;return s.accept?1:0;}
            if(r.equals("Ljava/lang/Object;->getClass()Ljava/lang/Class;"))return v[0].getClass();
            if(r.contains("->getNavArgModel()"))return s.navArg?Native.NAV_ARG:null;
            if(r.contains("->isThumbnailScaleUp()"))return Boolean.valueOf(s.thumbnail);
            if(r.equals("Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z"))return Objects.equals(v[0],v[1])?1:0;
            if(r.startsWith("LX/0kTy;->LIZ(")){out.trace.add("thumbnail");return null;}
            if(r.equals(F+"->mY()Z"))return s.paused?1:0;
            if(r.equals(F+"->kY()V")){out.trace.add("time-result");return null;}
            if(r.equals("Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;"))return((ArrayList<?>)v[0]).iterator();
            if(r.equals("Ljava/util/Iterator;->hasNext()Z"))return((Iterator<?>)v[0]).hasNext()?1:0;
            if(r.equals("Ljava/util/Iterator;->next()Ljava/lang/Object;"))return((Iterator<?>)v[0]).next();
            if(r.equals("LX/04Wb;->onKeyDown(ILandroid/view/KeyEvent;)Z")){out.trace.add("key-guard");if(s.keyThrows)throw new IllegalStateException("native listener test failure");return s.key?1:0;}
            if(r.equals("LX/06tg;->LIZ()Z"))return s.keyFeature?1:0;
            if(r.equals("LX/04Wb;->nk()Z")){out.trace.add("secondary-key-guard");return s.secondaryKey?1:0;}
            if(r.equals("LX/04fH;->LIZ()Lcom/ss/android/ugc/aweme/comment/services/CommentService;"))return Native.COMMENT_SERVICE;
            if(r.equals("Landroidx/fragment/app/Fragment;->getActivity()LX/02Hm;"))return Native.ACTIVITY;
            if(r.contains("CommentService;->LJLL(")){out.trace.add("comment-guard");return s.comment?1:0;}
            if(r.equals("Lcom/bytedance/ies/uikit/base/AbsFragment;->isViewValid()Z"))return s.valid?1:0;
            if(r.equals("LX/0yWM;->LLLLILI()Z")){out.trace.add("child-guard");return s.childConsumes?1:0;}
            if(r.equals(NAV)){out.navigation++;out.trace.add("navigate");return null;}
            if(r.startsWith("LX/06rx;-><init>("))return null;
            if(r.startsWith("LX/07K7;->LIZ(")){out.trace.add("event");return v[0];}
            if(r.equals("Ljava/lang/StringBuilder;-><init>()V"))return null;
            if(r.equals("Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V")){((StringBuilder)v[0]).append((String)v[1]);return null;}
            if(r.equals("Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;")){((StringBuilder)v[0]).append(v[1]);return v[0];}
            if(r.equals("Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;")){((StringBuilder)v[0]).append(v[1]);return v[0];}
            if(r.equals("Ljava/lang/StringBuilder;->toString()Ljava/lang/String;"))return v[0].toString();
            if(r.startsWith("LX/04Zl;->LJII")){out.trace.add("log");return null;}
            if(r.startsWith("LX/075r;->LIZJ(")){out.trace.add("missing-child");return null;}
            throw new AssertionError("unmodeled native Back invoke "+r);
        }
        Run run(){Object[] r=new Object[c.method.getImplementation().getRegisterCount()];r[r.length-1]=s;int pc=0,steps=0;
            while(out.returned<0){check(++steps<2000,"native Back CFG loop "+s.name);Instruction i=c.ins.get(pc);int next=pc+1;try{
                switch(i.getOpcode()){
                    case CONST_4:case CONST_16:case CONST:r[a(i)]=((NarrowLiteralInstruction)i).getNarrowLiteral();break;
                    case CONST_STRING:case CONST_STRING_JUMBO:r[a(i)]=((StringReference)((ReferenceInstruction)i).getReference()).getString();break;
                    case MOVE:case MOVE_FROM16:case MOVE_OBJECT:case MOVE_OBJECT_FROM16:r[a(i)]=r[((TwoRegisterInstruction)i).getRegisterB()];break;
                    case MOVE_RESULT:case MOVE_RESULT_OBJECT:r[a(i)]=pending;pending=null;break;
                    case MOVE_EXCEPTION:r[a(i)]=exception;exception=null;break;
                    case IGET_OBJECT:r[a(i)]=field(((FieldReference)((ReferenceInstruction)i).getReference()).getName());break;
                    case SGET_OBJECT:{String f=ref(i);if(f.equals("Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;"))r[a(i)]=Boolean.TRUE;else if(f.startsWith("Lcom/ss/android/ugc/aweme/comment/services/CommentService;"))r[a(i)]=Native.COMMENT_ANCHOR;else if(f.startsWith("LX/075r;"))r[a(i)]=Native.LOGGER;else throw new AssertionError("unmodeled static field "+f);break;}
                    case NEW_INSTANCE:r[a(i)]=ref(i).equals("Ljava/lang/StringBuilder;")?new StringBuilder():Native.EVENT;break;
                    case CHECK_CAST:case NOP:break;
                    case INVOKE_STATIC:case INVOKE_STATIC_RANGE:case INVOKE_VIRTUAL:case INVOKE_VIRTUAL_RANGE:case INVOKE_INTERFACE:case INVOKE_INTERFACE_RANGE:case INVOKE_DIRECT:case INVOKE_DIRECT_RANGE:{int[] regs=args(i);Object[] values=new Object[regs.length];for(int k=0;k<regs.length;k++)values[k]=r[regs[k]];pending=invoke(ref(i),values);break;}
                    case IF_EQZ:if(zero(r[a(i)]))next=c.destination(pc);break;
                    case IF_NEZ:if(!zero(r[a(i)]))next=c.destination(pc);break;
                    case IF_EQ:case IF_NE:{boolean equal=eq(r[a(i)],r[((TwoRegisterInstruction)i).getRegisterB()]);if(equal==(i.getOpcode()==Opcode.IF_EQ))next=c.destination(pc);break;}
                    case GOTO:case GOTO_16:case GOTO_32:next=c.destination(pc);break;
                    case RETURN:out.returned=integer(r[a(i)]);break;
                    default:throw new AssertionError("unmodeled native Back opcode "+i.getOpcode());
                }
            }catch(RuntimeException e){int handler=-1;for(TryBlock<? extends ExceptionHandler> t:c.method.getImplementation().getTryBlocks())if(c.addresses.get(pc)>=t.getStartCodeAddress()&&c.addresses.get(pc)<t.getStartCodeAddress()+t.getCodeUnitCount())for(ExceptionHandler h:t.getExceptionHandlers())if(handler<0){String type=h.getExceptionType();if(type==null||type.equals("Ljava/lang/Exception;")||type.equals("Ljava/lang/RuntimeException;")||type.equals("Ljava/lang/Throwable;"))handler=c.at(h.getHandlerCodeAddress());}check(handler>=0,"native model threw outside original handler: "+e);exception=e;next=handler;}
            pc=next;}return out;
        }
    }
    static List<Scenario> scenarios(){List<Scenario> out=new ArrayList<Scenario>();Scenario s;
        out.add(new Scenario("ordinary shared video"));s=new Scenario("thumbnail consumer");s.thumbnail=true;out.add(s);s=new Scenario("thumbnail without controller");s.thumbnail=true;s.thumbnailController=false;out.add(s);
        s=new Scenario("registered key consumer");s.key=true;out.add(s);s=new Scenario("registered secondary consumer");s.secondaryKey=true;s.keyFeature=true;out.add(s);s=new Scenario("nonconsuming secondary listener");s.secondaryKey=true;out.add(s);
        s=new Scenario("comment consumer");s.comment=true;out.add(s);s=new Scenario("child consumer");s.childConsumes=true;out.add(s);s=new Scenario("native time result then navigation");s.paused=true;out.add(s);
        s=new Scenario("no child");s.child=false;out.add(s);s=new Scenario("invalid child view");s.valid=false;out.add(s);s=new Scenario("missing model");s.model=false;out.add(s);s=new Scenario("missing nav arguments");s.navArg=false;out.add(s);s=new Scenario("throwing key listener retains native handler");s.keyThrows=true;out.add(s);return out;}
    static void exerciseBack(Map<String,ClassDef> original,Map<String,ClassDef> output){LinkFastRouteAudit.Code old=code(original,F,"onBackPressed()Z"),now=output==null?null:code(output,F,"onBackPressed()Z");for(Scenario s:scenarios()){
        s.accept=false;Run baseline=new BackVm(old,s).run();backExecutions++;
        if(s.name.equals("ordinary shared video"))check(baseline.returned==1&&baseline.navigation==1,"native child fallback must dispatch navigation and return consumed true");
        if(now==null)continue;Run declined=new BackVm(now,s).run();backExecutions++;check(declined.returned==baseline.returned&&declined.navigation==baseline.navigation&&declined.trace.equals(baseline.trace),"declined runtime altered native Back: "+s.name);
        s.accept=true;Run accepted=new BackVm(now,s).run();backExecutions++;
        if(baseline.navigation==1){check(accepted.probes==1&&accepted.navigation==0&&accepted.returned==1,"authorized exit must consume exactly at pre-navigation boundary: "+s.name);check(baseline.trace.subList(0,accepted.trace.size()).equals(accepted.trace),"native guards skipped or duplicated before exit: "+s.name);}
        else check(accepted.probes==0&&accepted.returned==baseline.returned&&accepted.trace.equals(baseline.trace),"native consuming guard reached exit: "+s.name);
        s.accept=false;
    }}
    public static void main(String[] args)throws Exception{
        check(args.length>=2&&(args[0].equals("original")||args[0].equals("output")),"original ORIGINAL_APK | output ORIGINAL_APK PATCHED_APK [BASELINE_ONEBACK_APK]");LinkFastRouteAudit.pin(args[1]);Map<String,ClassDef> original=read(args[1]);original(original);
        if(args[0].equals("original")){check(args.length==2,"unexpected original arguments");exerciseBack(original,null);System.out.println("PASS LinkBackAudit original: exact input; native Back guards and consumed navigation interpreted; C/E/G temporary Intent provenance and dedicated/SAF route access verified; "+backExecutions+" actual DEX Back executions; not an Android device test");return;}
        check(args.length==3||args.length==4,"patched APK and optional OneBack reference required");Map<String,ClassDef> reference=args.length==4?read(args[3]):original,output=read(args[2]);
        original(reference);int methods=LinkFastRouteAudit.compare(reference,output);
        for(String type:NATIVE)if(!LinkFastRouteAudit.WANTED.contains(type))check(Arrays.equals(canonical(Collections.singleton(reference.get(type))),canonical(Collections.singleton(output.get(type)))),"unreviewed native host/child/return change "+type);
        runtime(reference,output,args.length==4);exerciseBack(original,output);
        System.out.println("PASS LinkBackAudit output: exact C/E/G final-Intent transfer; positive-only animation; all native resume arrivals bind; destroy revokes; one native Back gate after key/comment/child consumers; owned return suppression; native fallbacks and preexisting OneBack/runtime preserved; "+methods+" original methods compared; "+backExecutions+" actual DEX Back executions; checks="+checks+"; not an Android device test");
    }
}
