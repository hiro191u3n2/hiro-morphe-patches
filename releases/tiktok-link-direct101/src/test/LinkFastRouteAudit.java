/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;

/** Independent DEX audit of the original APK and actual Morphe output.
 * Executes the emitted selector/helper instructions, not Java copies of their
 * predicates. Android URI primitives are modeled for explicit host vectors;
 * this is not Android execution, timing measurement, or a device smoke test.
 * The native home-intent factory and configurable native detail policy are
 * boundaries: their bodies are compared unchanged, and both policy results
 * are exercised. The selector must preserve the exact input detail intent.
 */
public final class LinkFastRouteAudit {
    static final String Q="LX/1SxQ;", D="LX/1SyD;", POPUP="LX/0ICN;";
    static final String SELECT="LIZLLL(Landroid/app/Activity;Landroid/content/Intent;Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;Z)Landroid/content/Intent;";
    static final String LABEL="LJII(Landroid/net/Uri;Ljava/lang/String;Z)Z";
    static final String SPEC="LJ(Landroid/app/Activity;Landroid/net/Uri;)V";
    static final String FAST="hiroShouldOpenSharedDetail(Landroid/net/Uri;)Z";
    static final String EXCLUDE="hiroHasSharedRouteExclusion(Landroid/net/Uri;)Z";
    static final String FACTORY="LJIIJ(Landroid/app/Activity;Landroid/content/Intent;Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;Z)Landroid/content/Intent;";
    static final String POLICY="LJIIIIZZ(Landroid/net/Uri;)Z";
    static final String URI_QUERY="Lcom/bytedance/mt/protector/impl/UriProtector;->com_bytedance_mt_protector_impl_UriProtector_com_ss_android_ugc_aweme_lancet_UriLancet_getQueryParameter(Landroid/net/Uri;Ljava/lang/String;)Ljava/lang/String;";
    static final String URI_PARSE="Lcom/bytedance/mt/protector/impl/UriProtector;->parse(Ljava/lang/String;)Landroid/net/Uri;";
    static final String QUERY_PRED="LIZIZ(Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;)Z";
    static final String TYPE_GUARD="LIZJ(Landroid/net/Uri;Ljava/lang/String;)Z";
    static final String NESTED_PRED="LIZJ(Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;)Z";
    static final String SHA="af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b";
    static final Set<String> WANTED=new TreeSet<>(Arrays.asList(Q,D,POPUP,
        "LX/1SyC;","LX/1SyE;","LX/1SyG;","LX/1SyI;","LX/1SyJ;","LX/1SyK;",
        "LX/1Sxg;","LX/0iCu;","LX/15bm;","LX/1Sy1;","LX/15dX;",
        "LX/1SxU;","LX/1SxW;","LX/1SxX;","LX/1Sxm;","LX/15fm;","LX/15fo;","LX/15fp;",
        "LX/1SxO;","LX/1SxP;",
        "Lcom/ss/android/ugc/aweme/services/MainServiceImpl;",
        "Lcom/ss/android/ugc/aweme/detail/route/DetailRouterInterceptor;",
        "Lcom/ss/android/ugc/aweme/detail/ui/DetailActivity;",
        "Lcom/ss/android/ugc/aweme/detail/ui/saf/DetailSafRootFragment;",
        "Lcom/ss/android/ugc/aweme/detail/ui/saf/WithoutShellDetailSafRootFragment;"));
    static long checks;
    static int vectorExecutions;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static String id(MethodReference m){StringBuilder s=new StringBuilder(m.getName()).append('(');for(CharSequence p:m.getParameterTypes())s.append(p);return s.append(')').append(m.getReturnType()).toString();}
    static Map<String,Method> methods(ClassDef c){check(c!=null,"missing class");Map<String,Method> r=new TreeMap<>();for(Method m:c.getMethods())check(r.put(id(m),m)==null,"duplicate method");return r;}
    static Method method(Map<String,ClassDef> cs,String owner,String name){Method m=methods(cs.get(owner)).get(name);check(m!=null,"missing "+owner+"->"+name);return m;}
    static void pin(String path)throws Exception{
        check(Files.size(Paths.get(path))==474921201L,"original APK size mismatch");MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] b=new byte[1048576];
        try(InputStream in=Files.newInputStream(Paths.get(path))){for(int n;(n=in.read(b))>=0;)md.update(b,0,n);}
        StringBuilder hex=new StringBuilder();for(byte x:md.digest())hex.append(String.format("%02x",x));check(SHA.equals(hex.toString()),"original APK digest mismatch");
    }
    static Map<String,ClassDef> read(String path)throws Exception{
        Map<String,ClassDef> cs=new TreeMap<>();try(ZipFile z=new ZipFile(path)){
            AndroidManifestBlock m=AndroidManifestBlock.load(z.getInputStream(z.getEntry("AndroidManifest.xml")));
            check("com.zhiliaoapp.musically".equals(m.getPackageName()),"wrong APK package");
            check("47.4.3".equals(m.getVersionName())&&Integer.valueOf(2024704030).equals(m.getVersionCode()),"wrong APK version");
            for(Enumeration<? extends ZipEntry> es=z.entries();es.hasMoreElements();){ZipEntry e=es.nextElement();if(!e.getName().matches("classes(?:[0-9]+)?\\.dex"))continue;
                try(InputStream in=new BufferedInputStream(z.getInputStream(e))){DexBackedDexFile dex=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),in);
                    for(ClassDef c:dex.getClasses())if(WANTED.contains(c.getType()))check(cs.put(c.getType(),ImmutableClassDef.of(c))==null,"duplicate selected class "+c.getType());}
            }
        }check(cs.keySet().equals(WANTED),"missing native classes: "+difference(WANTED,cs.keySet()));return cs;
    }
    static Set<String> difference(Set<String> a,Set<String> b){Set<String> s=new TreeSet<>(a);s.removeAll(b);return s;}
    static final class Code {
        final Method method;final List<Instruction> ins=new ArrayList<>();final List<Integer> addresses=new ArrayList<>();final Map<Integer,Integer> indexes=new HashMap<>();
        final Map<Integer,List<Integer>> switchOwners=new HashMap<>();int size;
        Code(Method m){method=m;check(m!=null&&m.getImplementation()!=null,"missing code");for(Instruction i:m.getImplementation().getInstructions()){indexes.put(size,ins.size());addresses.add(size);ins.add(i);size+=i.getCodeUnits();}indexes.put(size,ins.size());
            for(int n=0;n<ins.size();n++)if(ins.get(n).getOpcode()==Opcode.PACKED_SWITCH||ins.get(n).getOpcode()==Opcode.SPARSE_SWITCH){int p=destination(n);switchOwners.computeIfAbsent(p,k->new ArrayList<>()).add(n);}}
        int at(int address){Integer n=indexes.get(address);check(n!=null,"unaligned instruction "+address+" in "+id(method));return n;}
        int destination(int n){return at(addresses.get(n)+((OffsetInstruction)ins.get(n)).getCodeOffset());}
        String key(int n,IndexMap map){Instruction i=ins.get(n);String op=i.getOpcode().name();if(op.equals("CONST_STRING_JUMBO"))op="CONST_STRING";if(op.startsWith("GOTO"))op="GOTO";StringBuilder s=new StringBuilder(op);
            if(i instanceof OneRegisterInstruction)s.append(" a=").append(((OneRegisterInstruction)i).getRegisterA());
            if(i instanceof TwoRegisterInstruction)s.append(" b=").append(((TwoRegisterInstruction)i).getRegisterB());
            if(i instanceof ThreeRegisterInstruction)s.append(" c=").append(((ThreeRegisterInstruction)i).getRegisterC());
            if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction q=(FiveRegisterInstruction)i;int[] regs={q.getRegisterC(),q.getRegisterD(),q.getRegisterE(),q.getRegisterF(),q.getRegisterG()};s.append(" regs=");for(int k=0;k<q.getRegisterCount();k++)s.append(regs[k]).append(',');}
            if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction q=(RegisterRangeInstruction)i;s.append(" range=").append(q.getStartRegister()).append(',').append(q.getRegisterCount());}
            if(i instanceof WideLiteralInstruction)s.append(" literal=").append(((WideLiteralInstruction)i).getWideLiteral());
            if(i instanceof OffsetInstruction)s.append(" target=").append(map.get(destination(n)));
            if(i instanceof ReferenceInstruction)s.append(" ref=").append(ref(i));
            if(i instanceof DualReferenceInstruction)s.append(" ref2=").append(((DualReferenceInstruction)i).getReference2());
            if(i instanceof SwitchPayload){List<Integer> owners=switchOwners.get(n);check(owners!=null,"orphan switch payload");for(int owner:owners)for(SwitchElement e:((SwitchPayload)i).getSwitchElements())s.append(" switch=").append(map.get(owner)).append(':').append(e.getKey()).append(':').append(map.get(at(addresses.get(owner)+e.getOffset())));}
            if(i instanceof ArrayPayload){ArrayPayload p=(ArrayPayload)i;s.append(" array=").append(p.getElementWidth()).append(':').append(p.getArrayElements());}
            return s.toString();}
        List<String> keys(IndexMap map,int skip,int count){List<String> r=new ArrayList<>();for(int n=0;n<ins.size();n++)if(n<skip||n>=skip+count)r.add(key(n,map));return r;}
        List<String> tries(IndexMap map){List<String> r=new ArrayList<>();for(TryBlock<? extends ExceptionHandler> t:method.getImplementation().getTryBlocks()){StringBuilder s=new StringBuilder().append(map.get(at(t.getStartCodeAddress()))).append('-').append(map.get(at(t.getStartCodeAddress()+t.getCodeUnitCount())));for(ExceptionHandler h:t.getExceptionHandlers())s.append('|').append(h.getExceptionType()).append(':').append(map.get(at(h.getHandlerCodeAddress())));r.add(s.toString());}return r;}
    }
    interface IndexMap{int get(int n);}
    static final IndexMap IDENTITY=n->n;
    static void shape(ClassDef a,ClassDef b){
        check(a.getType().equals(b.getType())&&a.getAccessFlags()==b.getAccessFlags()&&Objects.equals(a.getSuperclass(),b.getSuperclass())&&a.getInterfaces().equals(b.getInterfaces())&&a.getAnnotations().equals(b.getAnnotations()),"native class header changed "+a.getType());
        Map<String,Field> x=new TreeMap<>(),y=new TreeMap<>();for(Field f:a.getFields())x.put(f.getName()+':'+f.getType(),f);for(Field f:b.getFields())y.put(f.getName()+':'+f.getType(),f);check(x.keySet().equals(y.keySet()),"native fields added/removed");
        for(String n:x.keySet()){Field p=x.get(n),q=y.get(n);check(p.getAccessFlags()==q.getAccessFlags()&&Objects.equals(p.getInitialValue(),q.getInitialValue())&&p.getAnnotations().equals(q.getAnnotations()),"native field changed "+a.getType()+n);}
    }
    static int injectionIndex(Code c,String name){int k=c.at(name.equals(SELECT)?0x1d:0x1e);check(c.ins.get(k).getOpcode()==Opcode.CONST_STRING&&ref(c.ins.get(k)).equals("insert_feed"),"original selector boundary differs");return k;}
    static void originalStructure(Map<String,ClassDef> cs){
        Code q=new Code(method(cs,Q,SPEC));check(q.ins.size()==184&&q.method.getImplementation().getRegisterCount()==13,"short-start method structure changed");
        check(ref(q.ins.get(0)).equals("LX/15dX;->LIZ()Z")&&q.ins.get(1).getOpcode()==Opcode.MOVE_RESULT,"short-start hot guard changed");
        check(q.addresses.get(2)==4&&q.ins.get(2).getOpcode()==Opcode.IF_NEZ&&((OneRegisterInstruction)q.ins.get(2)).getRegisterA()==0&&q.destination(2)==183&&q.addresses.get(183)==0x168&&q.ins.get(183).getOpcode()==Opcode.RETURN_VOID,"short-start native exit changed");
        for(String s:Arrays.asList(SELECT,LABEL)){Code c=new Code(method(cs,D,s));injectionIndex(c,s);check(c.method.getImplementation().getRegisterCount()==(s.equals(SELECT)?8:7),"selector register count changed");check(contains(c,"share_comment_id")&&contains(c,"LX/1Sxg;->"+TYPE_GUARD),"native comment/type guard missing");}
        check(!methods(cs.get(D)).containsKey(FAST)&&!methods(cs.get(D)).containsKey(EXCLUDE),"original contains patch helper");
        Code f=new Code(method(cs,D,FACTORY));check(contains(f,"LX/15bm;->LIZ(Landroid/content/Context;)Landroid/content/Intent;")&&contains(f,"gids")&&contains(f,"HOME"),"home factory identity changed");
        check(contains(new Code(method(cs,"LX/15bm;","LIZ(Landroid/content/Context;)Landroid/content/Intent;")),"Lcom/ss/android/ugc/aweme/services/IMainService;->getMainActivityClass()Ljava/lang/Class;"),"home factory no longer targets Main");
        for(String owner:Arrays.asList("LX/1SyC;","LX/1SyE;","LX/1SyG;")){boolean detail=false,select=false,label=false;for(Method m:cs.get(owner).getMethods())if(m.getImplementation()!=null){Code c=new Code(m);detail|=contains(c,"Lcom/ss/android/ugc/aweme/detail/ui/DetailActivity;");select|=contains(c,D+"->"+SELECT);label|=contains(c,D+"->"+LABEL);}check(detail&&select&&label,"native detail caller missing "+owner);}
        Code caller=new Code(method(cs,Q,"LIZIZ(LX/1Sxc;)V"));check(contains(caller,Q+"->"+SPEC)&&contains(caller,"LX/1KrS;->LIZ(Landroid/net/Uri;LX/1Sxc;)Landroid/net/Uri;"),"short resolver caller changed");
    }
    static boolean contains(Code c,String reference){for(Instruction i:c.ins)if(ref(i).equals(reference))return true;return false;}
    static void injection(Code original,Code patched,int k,String name){
        check(patched.ins.size()==original.ins.size()+4,"selector must contain exactly four inserted instructions");
        Instruction invoke=patched.ins.get(k),result=patched.ins.get(k+1),branch=patched.ins.get(k+2),ret=patched.ins.get(k+3);
        check(invoke.getOpcode()==Opcode.INVOKE_STATIC&&ref(invoke).equals(D+"->"+FAST),"missing independent shared-detail guard");
        FiveRegisterInstruction args=(FiveRegisterInstruction)invoke;check(args.getRegisterCount()==1&&args.getRegisterC()==4,"helper receives wrong URI register");
        check(result.getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)result).getRegisterA()==0,"helper result register changed");
        check(branch.getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)branch).getRegisterA()==0&&patched.destination(k+2)==k+4,"helper false route does not enter original insert_feed instruction");
        check(ret.getOpcode()==(name.equals(SELECT)?Opcode.RETURN_OBJECT:Opcode.RETURN)&&((OneRegisterInstruction)ret).getRegisterA()==3,"helper true route must preserve original intent / native false register");
    }
    static int compare(Map<String,ClassDef> a,Map<String,ClassDef> b){int count=0;
        for(String owner:WANTED){shape(a.get(owner),b.get(owner));Map<String,Method> x=methods(a.get(owner)),y=methods(b.get(owner));Set<String> names=new TreeSet<>(y.keySet());if(owner.equals(D)){check(names.remove(FAST)&&names.remove(EXCLUDE),"both guarded helpers required");}check(x.keySet().equals(names),"unreviewed method inventory change "+owner);
            for(String name:x.keySet()){Method p=x.get(name),q=y.get(name);count++;check(p.getAccessFlags()==q.getAccessFlags()&&p.getAnnotations().equals(q.getAnnotations()),"method header changed "+owner+name);check((p.getImplementation()==null)==(q.getImplementation()==null),"method body presence changed");if(p.getImplementation()==null)continue;
                check(p.getImplementation().getRegisterCount()==q.getImplementation().getRegisterCount(),"register count changed "+owner+name);Code old=new Code(p),now=new Code(q);List<String> expected=old.keys(IDENTITY,-1,0),actual;IndexMap map=IDENTITY;
                if(owner.equals(D)&&(name.equals(SELECT)||name.equals(LABEL))){final int k=injectionIndex(old,name);injection(old,now,k,name);map=n->{if(n>=k&&n<k+4)throw new AssertionError("old control flow targets inserted helper");return n>=k+4?n-4:n;};actual=now.keys(map,k,4);}
                else{check(old.ins.size()==now.ins.size(),"instruction count changed "+owner+name);actual=now.keys(IDENTITY,-1,0);
                    if(owner.equals(Q)&&name.equals(SPEC)){Instruction z=now.ins.get(2);check(z.getOpcode()==Opcode.GOTO_16&&z.getCodeUnits()==2&&now.destination(2)==183,"speculative Main startup is not bypassed");expected.set(2,"GOTO target=183");}
                    if(owner.equals(POPUP)&&name.equals("invoke()Ljava/lang/Object;")){check(old.ins.size()==14&&old.addresses.get(8)==15,"original popup factory mismatch");Instruction z=now.ins.get(8);check(z.getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)z).getRegisterA()==0&&((WideLiteralInstruction)z).getWideLiteral()==0,"popup eligibility regression");expected.set(8,"CONST_4 a=0 literal=0");}
                }
                check(expected.equals(actual),"unreviewed native instruction change "+owner+"->"+name+firstDifference(expected,actual));check(old.tries(IDENTITY).equals(now.tries(map)),"try handlers changed "+owner+name);
            }
        }return count;
    }
    static String firstDifference(List<String> a,List<String>b){for(int n=0;n<Math.min(a.size(),b.size());n++)if(!a.get(n).equals(b.get(n)))return " at index "+n+": "+a.get(n)+" / "+b.get(n);return " lengths "+a.size()+" / "+b.size();}
    static Set<Integer> reachable(Code c){Set<Integer> seen=new TreeSet<>();Deque<Integer> todo=new ArrayDeque<>();todo.add(0);while(!todo.isEmpty()){int n=todo.remove();check(n>=0&&n<c.ins.size(),"CFG falls outside method");if(!seen.add(n))continue;Instruction i=c.ins.get(n);String op=i.getOpcode().name();if(op.startsWith("GOTO"))todo.add(c.destination(n));else if(op.startsWith("IF_")){todo.add(c.destination(n));todo.add(n+1);}else if(!op.startsWith("RETURN")&&!op.equals("THROW"))todo.add(n+1);
            if(i.getOpcode().canThrow())for(TryBlock<? extends ExceptionHandler> t:c.method.getImplementation().getTryBlocks())if(c.addresses.get(n)>=t.getStartCodeAddress()&&c.addresses.get(n)<t.getStartCodeAddress()+t.getCodeUnitCount())for(ExceptionHandler h:t.getExceptionHandlers())todo.add(c.at(h.getHandlerCodeAddress()));}return seen;}
    static void speculation(Map<String,ClassDef> cs,boolean patched){Code c=new Code(method(cs,Q,SPEC));Set<Integer> r=reachable(c);boolean launch=false,main=false,flag=false;for(int n:r){String s=ref(c.ins.get(n));launch|=s.equals(Q+"->LIZLLL(Landroid/app/Activity;Landroid/content/Intent;)V");main|=s.equals("Lcom/ss/android/ugc/aweme/services/IMainService;->getMainActivityIntent(Landroid/content/Context;)Landroid/content/Intent;");flag|=s.equals(Q+"->LIZJ:Z");}check(launch==!patched&&main==!patched&&flag==!patched,"speculative Main reachability differs");if(patched)check(r.equals(new TreeSet<>(Arrays.asList(0,1,2,183))),"speculative startup has unexpected reachable work");}

    /** Small immutable model for the public Uri primitives used by the audited
     * vectors. URI.create is separately invoked as the real Java API; it is
     * not used to make Android Uri.parse stricter than the native API. */
    static final class U {
        final String raw;U(String s){if(s==null)throw new NullPointerException("uri");raw=s;}
        int colon(){return raw.indexOf(':');}
        boolean hierarchical(){int c=colon();return c<0||(c+1<raw.length()&&raw.charAt(c+1)=='/');}
        String scheme(){int c=colon();return c<0?null:raw.substring(0,c);}
        String authority(){if(!hierarchical())return null;int start=colon()+1;if(!raw.startsWith("//",start))return null;start+=2;int end=raw.length();for(char ch:new char[]{'/','?','#'}){int x=raw.indexOf(ch,start);if(x>=0&&x<end)end=x;}return raw.substring(start,end);}
        String host(){String a=authority();if(a==null)return null;int at=a.lastIndexOf('@');if(at>=0)a=a.substring(at+1);if(a.startsWith("[")){int end=a.indexOf(']');return end<0?a:a.substring(0,end+1);}int port=a.indexOf(':');return decode(port<0?a:a.substring(0,port),false);}
        String path(){if(!hierarchical())return null;int start=colon()+1;String a=authority();if(a!=null)start+=2+a.length();int end=raw.length();for(char ch:new char[]{'?','#'}){int x=raw.indexOf(ch,start);if(x>=0&&x<end)end=x;}return decode(raw.substring(start,end),false);}
        String query(String key){if(!hierarchical())throw new UnsupportedOperationException("This isn't a hierarchical URI.");int q=raw.indexOf('?');int hash=raw.indexOf('#');if(q<0||(hash>=0&&q>hash))return null;String s=raw.substring(q+1,hash<0?raw.length():hash);for(String p:s.split("&",-1)){int eq=p.indexOf('=');String k=eq<0?p:p.substring(0,eq);if(decode(k,true).equals(key))return decode(eq<0?"":p.substring(eq+1),true);}return null;}
        public String toString(){return raw;}
    }
    static String decode(String s,boolean plus){try{return URLDecoder.decode(plus?s:s.replace("+","%2B"),"UTF-8");}catch(IllegalArgumentException e){return s;}catch(UnsupportedEncodingException e){throw new AssertionError(e);}}
    static String encode(String s){try{return URLEncoder.encode(s,"UTF-8").replace("+","%20");}catch(UnsupportedEncodingException e){throw new AssertionError(e);}}
    static final class IntentToken {final String kind,id;IntentToken(String k,String i){kind=k;id=i;}}
    static final class Env {final boolean policy;final IntentToken input;int factoryCalls,helperCalls;final Set<String> visited=new HashSet<>();Env(boolean p,String id){policy=p;input=new IntentToken("DETAIL",id);}}
    static int number(Object x){if(x==null)return 0;if(x instanceof Boolean)return (Boolean)x?1:0;return ((Number)x).intValue();}
    static boolean zero(Object x){return x==null||(x instanceof Number&&((Number)x).longValue()==0)||(x instanceof Boolean&&!(Boolean)x);}
    static Object object(Object x){return zero(x)?null:x;}
    static boolean equal(Object a,Object b){if(zero(a)&&zero(b))return true;if(a instanceof Number&&b instanceof Number)return ((Number)a).longValue()==((Number)b).longValue();return a==b;}
    static final class Engine {
        final Map<String,ClassDef> classes;final Map<String,Code> cache=new HashMap<>();Engine(Map<String,ClassDef> c){classes=c;}
        Code code(String owner,String name){return cache.computeIfAbsent(owner+"->"+name,k->new Code(method(classes,owner,name)));}
        Object run(String owner,String name,Env env,Object...args){Code c=code(owner,name);Object[] regs=new Object[c.method.getImplementation().getRegisterCount()];check((c.method.getAccessFlags()&8)!=0,"interpreter expects static method");int first=regs.length-args.length;check(first>=0,"bad argument registers");System.arraycopy(args,0,regs,first,args.length);Object result=null,exception=null;int pc=0;
            for(int step=0;step<2000;step++){check(pc>=0&&pc<c.ins.size(),"interpreter left method");Instruction i=c.ins.get(pc);Opcode op=i.getOpcode();String n=op.name();int next=pc+1;env.visited.add(owner+"->"+name+"@"+pc);
                try{
                    if(n.startsWith("CONST_STRING"))regs[((OneRegisterInstruction)i).getRegisterA()]=((StringReference)((ReferenceInstruction)i).getReference()).getString();
                    else if(n.startsWith("CONST")&&i instanceof WideLiteralInstruction)regs[((OneRegisterInstruction)i).getRegisterA()]=Integer.valueOf((int)((WideLiteralInstruction)i).getWideLiteral());
                    else if(n.startsWith("MOVE_RESULT"))regs[((OneRegisterInstruction)i).getRegisterA()]=result;
                    else if(op==Opcode.MOVE_EXCEPTION)regs[((OneRegisterInstruction)i).getRegisterA()]=exception;
                    else if(n.startsWith("MOVE")&&i instanceof TwoRegisterInstruction){TwoRegisterInstruction z=(TwoRegisterInstruction)i;regs[z.getRegisterA()]=regs[z.getRegisterB()];}
                    else if(op==Opcode.SGET_OBJECT){check(ref(i).equals("LX/1Sxg;->LIZ:LX/1Sxg;"),"unsupported static read "+ref(i));regs[((OneRegisterInstruction)i).getRegisterA()]=this;}
                    else if(n.startsWith("INVOKE_")){int[] registers=arguments(i);Object[] values=new Object[registers.length];for(int k=0;k<registers.length;k++)values[k]=object(regs[registers[k]]);result=call(ref(i),values,env);}
                    else if(n.startsWith("IF_")){boolean take;Object a=regs[((OneRegisterInstruction)i).getRegisterA()];Object b=i instanceof TwoRegisterInstruction?regs[((TwoRegisterInstruction)i).getRegisterB()]:null;
                        if(op==Opcode.IF_EQZ)take=zero(a);else if(op==Opcode.IF_NEZ)take=!zero(a);else if(op==Opcode.IF_EQ)take=equal(a,b);else if(op==Opcode.IF_NE)take=!equal(a,b);else if(op==Opcode.IF_LTZ)take=number(a)<0;else if(op==Opcode.IF_LEZ)take=number(a)<=0;else if(op==Opcode.IF_GTZ)take=number(a)>0;else if(op==Opcode.IF_GEZ)take=number(a)>=0;else throw new AssertionError("unsupported conditional "+op);if(take)next=c.destination(pc);}
                    else if(n.startsWith("GOTO"))next=c.destination(pc);
                    else if(op==Opcode.RETURN||op==Opcode.RETURN_OBJECT)return object(regs[((OneRegisterInstruction)i).getRegisterA()]);
                    else if(op==Opcode.CHECK_CAST||op==Opcode.NOP){}
                    else if(op==Opcode.RETURN_VOID)return null;
                    else throw new AssertionError("unsupported actual DEX instruction "+owner+"->"+name+": "+op);
                }catch(RuntimeException ex){int handler=handler(c,pc,ex);if(handler<0)throw ex;exception=ex;next=handler;}
                pc=next;
            }throw new AssertionError("actual DEX failed to terminate "+owner+name);
        }
        int handler(Code c,int pc,RuntimeException ex){int addr=c.addresses.get(pc);for(TryBlock<? extends ExceptionHandler> t:c.method.getImplementation().getTryBlocks())if(addr>=t.getStartCodeAddress()&&addr<t.getStartCodeAddress()+t.getCodeUnitCount())for(ExceptionHandler h:t.getExceptionHandlers()){String s=h.getExceptionType();if(s==null||s.equals("Ljava/lang/RuntimeException;")||s.equals("Ljava/lang/Exception;")||s.equals("Ljava/lang/Throwable;")||s.equals("L"+ex.getClass().getName().replace('.','/')+";"))return c.at(h.getHandlerCodeAddress());}return -1;}
        Object call(String r,Object[] a,Env e){
            if(r.equals(D+"->"+FAST)||r.equals(D+"->"+EXCLUDE)){if(r.endsWith(FAST))e.helperCalls++;return run(D,r.substring(r.indexOf("->")+2),e,a);}
            if(r.equals("LX/1Sxg;->"+QUERY_PRED))return run("LX/1Sxg;",QUERY_PRED,e,a);
            if(r.equals("LX/1Sxg;->"+TYPE_GUARD))return run("LX/1Sxg;",TYPE_GUARD,e,a);
            if(r.equals("LX/0iCu;->"+NESTED_PRED))return run("LX/0iCu;",NESTED_PRED,e,a);
            if(r.equals(D+"->"+FACTORY)){e.factoryCalls++;return new IntentToken("HOME",e.input.id);}
            if(r.equals(D+"->"+POLICY))return e.policy?1:0;
            if(r.equals(URI_QUERY))return ((U)a[0]).query((String)a[1]);
            if(r.equals(URI_PARSE)||r.equals("Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;"))return new U((String)a[0]);
            if(r.equals("Landroid/net/Uri;->isHierarchical()Z"))return ((U)a[0]).hierarchical()?1:0;
            if(r.equals("Landroid/net/Uri;->getHost()Ljava/lang/String;"))return ((U)a[0]).host();
            if(r.equals("Landroid/net/Uri;->getPath()Ljava/lang/String;"))return ((U)a[0]).path();
            if(r.equals("Landroid/net/Uri;->getScheme()Ljava/lang/String;"))return ((U)a[0]).scheme();
            if(r.equals("Landroid/net/Uri;->toString()Ljava/lang/String;"))return a[0].toString();
            if(r.equals("Ljava/net/URI;->create(Ljava/lang/String;)Ljava/net/URI;"))return URI.create((String)a[0]);
            if(r.equals("Ljava/lang/String;->matches(Ljava/lang/String;)Z"))return ((String)a[0]).matches((String)a[1])?1:0;
            if(r.equals("Ljava/lang/String;->equals(Ljava/lang/Object;)Z"))return a[0].equals(a[1])?1:0;
            if(r.equals("Ljava/lang/String;->length()I"))return ((String)a[0]).length();
            if(r.equals("Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z")||r.equals("Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z"))return Objects.equals(a[0],a[1])?1:0;
            if(r.equals("Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z"))return a[0]==null||((CharSequence)a[0]).length()==0?1:0;
            if(r.equals("Landroid/text/TextUtils;->isDigitsOnly(Ljava/lang/CharSequence;)Z")){CharSequence s=(CharSequence)a[0];for(int k=0;k<s.length();k++)if(!Character.isDigit(s.charAt(k)))return 0;return 1;}
            if(r.equals("Ljava/lang/Object;->getClass()Ljava/lang/Class;")){Objects.requireNonNull(a[0]);return a[0].getClass();}
            throw new AssertionError("unmodeled actual DEX invocation "+r);
        }
    }
    static int[] arguments(Instruction i){if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction q=(FiveRegisterInstruction)i;return Arrays.copyOf(new int[]{q.getRegisterC(),q.getRegisterD(),q.getRegisterE(),q.getRegisterF(),q.getRegisterG()},q.getRegisterCount());}RegisterRangeInstruction q=(RegisterRangeInstruction)i;int[] r=new int[q.getRegisterCount()];for(int k=0;k<r.length;k++)r[k]=q.getStartRegister()+k;return r;}
    static final Set<String> SAFE_HELPER_CALLS=new HashSet<>(Arrays.asList(URI_QUERY,URI_PARSE,
        D+"->"+EXCLUDE,"LX/1Sxg;->"+QUERY_PRED,
        "Landroid/net/Uri;->isHierarchical()Z","Landroid/net/Uri;->getHost()Ljava/lang/String;","Landroid/net/Uri;->getPath()Ljava/lang/String;","Landroid/net/Uri;->getScheme()Ljava/lang/String;",
        "Ljava/net/URI;->create(Ljava/lang/String;)Ljava/net/URI;","Ljava/lang/String;->matches(Ljava/lang/String;)Z",
        "Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z","Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z"));
    static final Set<Opcode> SAFE_HELPER_OPS=new HashSet<>(Arrays.asList(Opcode.CONST_4,Opcode.CONST_16,Opcode.CONST,
        Opcode.CONST_STRING,Opcode.CONST_STRING_JUMBO,Opcode.INVOKE_STATIC,Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,Opcode.MOVE_RESULT_OBJECT,Opcode.MOVE_EXCEPTION,Opcode.MOVE_OBJECT,
        Opcode.IF_EQZ,Opcode.IF_NEZ,Opcode.GOTO,Opcode.GOTO_16,Opcode.GOTO_32,Opcode.RETURN,Opcode.NOP));
    static void helpers(Map<String,ClassDef> cs){for(String name:Arrays.asList(FAST,EXCLUDE)){Code c=new Code(method(cs,D,name));check(c.method.getAccessFlags()==(2|8|4096),"helper must be private static synthetic");check(c.ins.size()<256&&c.method.getImplementation().getRegisterCount()==(name.equals(FAST)?4:2),"unexpected helper register layout or size");
            for(int n=0;n<c.ins.size();n++){Instruction i=c.ins.get(n);String op=i.getOpcode().name();check(SAFE_HELPER_OPS.contains(i.getOpcode()),"unreviewed helper opcode "+op);if(op.startsWith("INVOKE"))check(SAFE_HELPER_CALLS.contains(ref(i)),"unreviewed helper call "+ref(i));if(i instanceof OffsetInstruction)check(c.destination(n)>n,"helper must have finite forward control flow");}
            for(TryBlock<? extends ExceptionHandler> t:c.method.getImplementation().getTryBlocks())for(ExceptionHandler h:t.getExceptionHandlers())check("Ljava/lang/RuntimeException;".equals(h.getExceptionType()),"helper must not swallow Error/Throwable");
            if(name.equals(FAST))check(!c.method.getImplementation().getTryBlocks().isEmpty()&&contains(c,"/detail/[1-9][0-9]{0,19}/?"),"missing strict ID predicate or runtime-exception fallback");
        }}
    static final class Vector {final String name,raw,id;final boolean helper,guarded,push;final String baseline;Vector(String n,String r,boolean h,boolean g,boolean p,String b){name=n;raw=r;helper=h;guarded=g;push=p;baseline=b;String target=null;if(r!=null){U u=new U(r);if(u.hierarchical()){target=u.query("id");if(target==null){String path=u.path();if(path!=null){while(path.endsWith("/"))path=path.substring(0,path.length()-1);int slash=path.lastIndexOf('/');target=path.substring(slash+1);}}}}id=target;}}
    static final String BASE="musically://aweme/detail/7391111111111111111";
    static List<Vector> vectors(){List<Vector> v=new ArrayList<>();
        add(v,"ordinary shared video",BASE+"?utm_campaign=client_share&insert_feed=1",true,false,false,"HOME");
        add(v,"shared default policy",BASE+"?utm_campaign=client_share",true,false,false,null);
        add(v,"shared explicit detail",BASE+"?utm_campaign=client_share&video_detail=1",true,false,false,"DETAIL");
        add(v,"nested insert overrides top zero",BASE+"?utm_campaign=client_share&insert_feed=0&params_url="+encode("https://www.tiktok.com/video/7391111111111111111?insert_feed=1"),true,false,false,"HOME");
        add(v,"nested campaign and insertion",BASE+"?params_url="+encode("https://www.tiktok.com/video/7391111111111111111?utm_campaign=client_share&insert_feed=1"),true,false,false,"HOME");
        add(v,"native nested campaign OR",BASE+"?utm_campaign=other&insert_feed=1&params_url="+encode("https://www.tiktok.com/video/7391111111111111111?utm_campaign=client_share"),true,false,false,"HOME");
        add(v,"notification guard",BASE+"?utm_campaign=client_share&insert_feed=1",true,true,true,"DETAIL");
        add(v,"type40 guard",BASE+"?utm_campaign=client_share&insert_feed=1&aweme_type=40",true,true,false,"DETAIL");
        add(v,"nested type40 guard",BASE+"?utm_campaign=client_share&insert_feed=1&params_url="+encode("https://www.tiktok.com/video/7391111111111111111?aweme_type=40"),true,true,false,"DETAIL");
        add(v,"comment guard",BASE+"?utm_campaign=client_share&insert_feed=1&share_comment_id=987",true,true,false,"DETAIL");
        add(v,"empty comment",BASE+"?utm_campaign=client_share&insert_feed=1&share_comment_id=",true,false,false,"HOME");
        add(v,"no shared marker",BASE+"?insert_feed=1",false,false,false,"HOME");
        add(v,"different marker",BASE+"?utm_campaign=client_share_extra&insert_feed=1",false,false,false,"HOME");
        add(v,"different host","musically://other/detail/123?utm_campaign=client_share&insert_feed=1",false,false,false,"HOME");
        add(v,"web host","https://www.tiktok.com/detail/123?utm_campaign=client_share&insert_feed=1",false,false,false,"HOME");
        for(String path:new String[]{"/detail/0","/detail/0123","/detail/-1","/detail/1,2","/detail/1/2","/detail/","/detail/abc","/detail/123456789012345678901","/profile/123"})add(v,"invalid path "+path,"musically://aweme"+path+"?utm_campaign=client_share&insert_feed=1",false,false,false,"HOME");
        for(String path:new String[]{"/detail/1","/detail/123/","/detail/12345678901234567890"})add(v,"valid path "+path,"musically://aweme"+path+"?utm_campaign=client_share&insert_feed=1",true,false,false,"HOME");
        for(String host:new String[]{"detail","item","musical"}){
            String legacy="musically://"+host;
            add(v,"legacy singleton "+host,legacy+"?id=7391111111111111111&utm_campaign=client_share&insert_feed=1",true,false,false,"HOME");
            add(v,"legacy slash "+host,legacy+"/?id=1&utm_campaign=client_share&insert_feed=1",true,false,false,"HOME");
            add(v,"legacy nested insertion "+host,legacy+"?id=123&utm_campaign=client_share&insert_feed=0&params_url="+encode("https://www.tiktok.com/video/123?insert_feed=1"),true,false,false,"HOME");
            for(String invalid:new String[]{"0","01","-1","abc","1,2","123456789012345678901",""})add(v,"legacy invalid ID "+host+" "+invalid,legacy+"?id="+encode(invalid)+"&utm_campaign=client_share&insert_feed=1",false,false,false,"HOME");
            add(v,"legacy extra path "+host,legacy+"/123?id=123&utm_campaign=client_share&insert_feed=1",false,false,false,"HOME");
            add(v,"legacy missing top ID "+host,legacy+"?utm_campaign=client_share&insert_feed=1&params_url="+encode("https://www.tiktok.com/video/123?id=123"),false,false,false,"HOME");
            add(v,"legacy no marker "+host,legacy+"?id=123&insert_feed=1",false,false,false,"HOME");
            add(v,"legacy notification "+host,legacy+"?id=123&utm_campaign=client_share&insert_feed=1",true,true,true,"DETAIL");
            add(v,"legacy comment "+host,legacy+"?id=123&utm_campaign=client_share&insert_feed=1&share_comment_id=1",true,true,false,"DETAIL");
        }
        for(String host:new String[]{"story","tuwen","detail_list","push"})add(v,"unrelated route "+host,"musically://"+host+"?id=123&utm_campaign=client_share&insert_feed=1",false,false,false,"HOME");
        for(String key:new String[]{"group_invite_id","jump_to_search_detail","is_from_drama_tab_deeplink","forbid_insert_feed","tab_index"}){
            add(v,"excluded "+key,BASE+"?utm_campaign=client_share&insert_feed=1&"+key+"=0",false,false,false,"HOME");
            add(v,"nested excluded "+key,BASE+"?utm_campaign=client_share&insert_feed=1&params_url="+encode("https://www.tiktok.com/video/123?"+key+"=1"),false,false,false,"HOME");
            add(v,"empty exclusion "+key,BASE+"?utm_campaign=client_share&insert_feed=1&"+key+"=",true,false,false,"HOME");
        }
        for(String nested:new String[]{"https://www.tiktok.com/%ZZ","/relative?utm_campaign=client_share","mailto:someone@example.com","https:///missing-host"})add(v,"invalid nested "+nested,BASE+"?utm_campaign=client_share&insert_feed=1&params_url="+encode(nested),false,false,false,null);
        add(v,"empty nested",BASE+"?utm_campaign=client_share&insert_feed=1&params_url=",true,false,false,"HOME");
        add(v,"opaque URI","musically:detail/123?utm_campaign=client_share&insert_feed=1",false,false,false,null);
        add(v,"null URI",null,false,false,false,null);return v;
    }
    static void add(List<Vector> v,String n,String r,boolean h,boolean g,boolean p,String b){v.add(new Vector(n,r,h,g,p,b));}
    static final class Outcome {Object result;RuntimeException error;Env env;String kind(){if(error!=null)return "throws:"+error.getClass().getName();if(result instanceof IntentToken)return ((IntentToken)result).kind;return zero(result)?"false":"true";}}
    static Outcome execute(Engine engine,Vector v,boolean policy,boolean label){Outcome out=new Outcome();out.env=new Env(policy,v.id);U uri=v.raw==null?null:new U(v.raw);try{out.result=label?engine.run(D,LABEL,out.env,uri,v.id,v.push?1:0):engine.run(D,SELECT,out.env,new Object(),out.env.input,uri,"link_direct",v.id,v.push?1:0);}catch(RuntimeException e){out.error=e;}return out;}
    static void exercise(Map<String,ClassDef> original,Map<String,ClassDef> patched){Engine before=new Engine(original),after=patched==null?null:new Engine(patched);List<Vector> vs=vectors();Set<String> covered=new HashSet<>();
        for(Vector v:vs){if(after!=null){Env e=new Env(false,v.id);Object x=after.run(D,FAST,e,v.raw==null?null:new U(v.raw));check((!zero(x))==v.helper,"actual helper scope mismatch: "+v.name);covered.addAll(e.visited);}
            for(boolean policy:new boolean[]{false,true})for(boolean label:new boolean[]{false,true}){Outcome old=execute(before,v,policy,label);vectorExecutions++;
                if(v.baseline!=null){String want=label?(v.baseline.equals("HOME")?"true":"false"):v.baseline;check(old.kind().equals(want),"original DEX baseline mismatch "+v.name+": "+old.kind());}
                if(old.error==null&&!label){check(old.result instanceof IntentToken,"native selector returned non-intent");check((((IntentToken)old.result).kind.equals("HOME"))==(old.env.factoryCalls==1),"original FYP boundary mismatch");}
                if(after==null)continue;Outcome now=execute(after,v,policy,label);covered.addAll(now.env.visited);
                if(v.helper&&!v.guarded){check(now.error==null,"eligible actual DEX route threw "+v.name);check(label?zero(now.result):now.result==now.env.input,"eligible shared video did not preserve original detail intent / false classification: "+v.name);check(now.env.factoryCalls==0,"eligible shared route reaches FYP factory");}
                else {check(old.kind().equals(now.kind()),"native fallback changed "+v.name+": "+old.kind()+" / "+now.kind());check(old.env.factoryCalls==now.env.factoryCalls,"native factory fallback changed "+v.name);if(v.guarded)check(now.env.helperCalls==0,"patch ran before native notification/type/comment guard "+v.name);}
            }
        }
        if(after!=null){for(String name:Arrays.asList(FAST,EXCLUDE)){Code c=after.code(D,name);Set<Integer> seen=new HashSet<>();String prefix=D+"->"+name+"@";for(String s:covered)if(s.startsWith(prefix))seen.add(Integer.parseInt(s.substring(prefix.length())));check(seen.contains(0)&&seen.size()*10>=c.ins.size()*9,"insufficient actual helper instruction coverage "+name);for(int n=0;n<c.ins.size();n++)if(c.ins.get(n).getOpcode()==Opcode.RETURN||c.ins.get(n).getOpcode()==Opcode.MOVE_EXCEPTION)check(seen.contains(n),"helper return/exception path untested "+name+"@"+n);}}
    }
    public static void main(String[] args)throws Exception{
        check(args.length>=2&&(args[0].equals("original")||args[0].equals("output")),"Usage: original ORIGINAL_APK | output ORIGINAL_APK PATCHED_APK");pin(args[1]);Map<String,ClassDef> original=read(args[1]);originalStructure(original);speculation(original,false);
        if(args[0].equals("original")){check(args.length==2,"unexpected original arguments");exercise(original,null);System.out.println("PASS LinkFastRouteAudit original: exact input identity; native shared-link selectors reproduce HOME insertion including nested insert_feed=1; native detail intent and push/type/comment guards verified; "+vectorExecutions+" actual DEX selector executions; not an Android device test");return;}
        check(args.length==3,"patched APK required");Map<String,ClassDef> patched=read(args[2]);int count=compare(original,patched);helpers(patched);speculation(patched,true);exercise(original,patched);
        System.out.println("PASS LinkFastRouteAudit output: one speculative-start branch; two guarded selector insertions; two pure synthetic helpers interpreted from output DEX; original detail intent preserved for scoped client_share links; nested/excluded/invalid routes and native notification/type/comment guards audited; "+WANTED.size()+" native classes and "+count+" original methods compared; "+vectorExecutions+" actual DEX selector executions; checks="+checks+"; not an Android device test");
    }
}
