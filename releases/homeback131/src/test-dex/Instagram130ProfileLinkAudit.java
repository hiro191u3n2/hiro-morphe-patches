import java.io.File;
import java.util.*;
import app.hiro.oneback.patches.InstagramExternalProfilePatch;
import app.hiro.oneback.patches.OneBackPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Independent original/output audit for the three 452 profile-link boundaries
 * and the unchanged native envelope used to identify their entry routes.
 * No original app body is bundled here. Input comes from the user's original APK.
 * The output gate checks do not call the transformation to manufacture an
 * expected output. They verify live argument registers, all incoming return
 * edges, native true/false handling, then reverse only the proven additions and
 * compare the complete original executable/exception/metadata representation.
 */
public final class Instagram130ProfileLinkAudit {
    static final String ROUTER="LX/02HW;";
    static final String PROFILE="Lcom/instagram/profile/fragment/UserDetailFragment;";
    static final String URL_DELEGATE="LX/0Rts;";
    static final String EXIT="Lapp/hiro/oneback/runtime/OneBackExit;";
    static final String FRAGMENT="Landroidx/fragment/app/Fragment;";
    static final String ACTIVITY="Landroid/app/Activity;", BUNDLE="Landroid/os/Bundle;", OBJECT="Ljava/lang/Object;";
    static final String STRING="Ljava/lang/String;",INTENT="Landroid/content/Intent;",SESSION="LX/08eI;",HOST="Landroidx/fragment/app/FragmentActivity;";
    static final String BASE="LX/01Mx;",MAIN_LINK="LX/003Q;",HANDLER="Lcom/instagram/urlhandler/WebAndUserExternalUrlHandler;";
    static final String USER_KEY="InstagramUrlLaunchConstants.EXTRA_USER_NAME";
    static final String ROUTER_PIN="1292d9a5bdedece0aca3f773d16e7a224535e6279deedf616c6b3d89f46ba9b9";
    static final String PROFILE_PIN="b32adfa6b171c5c5494196820df5e62a873c3964ed3a0d04aaa932cd4f6d5d13";
    static final String URL_PIN="e011c904547ddca7f5fa56fe346bae36f734769e2b3e84a22e525849a19401cc";
    static int checks,mutants;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static List<String> ps(String... items){return Arrays.asList(items);}
    static Method method(ClassDef clazz,String name){
        if(clazz==null)throw new AssertionError("missing class for "+name);
        Method result=null;for(Method method:clazz.getMethods())if(method.getName().equals(name)){
            if(name.equals("onBackPressed")&&(!method.getParameterTypes().isEmpty()||!method.getReturnType().equals("Z")))continue;
            if(name.equals("A09")&&(!method.getParameterTypes().equals(ps(BUNDLE,"Landroidx/fragment/app/FragmentActivity;","LX/08eI;"))||!method.getReturnType().equals("V")))continue;
            check(result==null,"unique audited overload "+name);result=method;
        }check(result!=null,"audited method exists "+name);return result;
    }
    static boolean target(Method method){return ROUTER.equals(method.getDefiningClass())&&method.getName().equals("A09")
            &&method.getParameterTypes().equals(ps(BUNDLE,"Landroidx/fragment/app/FragmentActivity;","LX/08eI;"))&&method.getReturnType().equals("V")
            ||PROFILE.equals(method.getDefiningClass())&&method.getName().equals("onBackPressed")&&method.getParameterTypes().isEmpty()&&method.getReturnType().equals("Z")
            ||URL_DELEGATE.equals(method.getDefiningClass())&&method.getName().equals("A03")&&method.getParameterTypes().isEmpty()&&method.getReturnType().equals("Ljava/lang/Integer;");}
    static List<Instruction> instructions(Method method){List<Instruction> result=new ArrayList<>();for(Instruction instruction:method.getImplementation().getInstructions())result.add(instruction);return result;}
    static MethodReference reference(Instruction instruction){if(!(instruction instanceof ReferenceInstruction))return null;Reference reference=((ReferenceInstruction)instruction).getReference();return reference instanceof MethodReference?(MethodReference)reference:null;}
    static int[] registers(Instruction instruction){
        if(instruction instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)instruction;return Arrays.copyOf(new int[]{r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()},r.getRegisterCount());}
        if(instruction instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)instruction;int[] result=new int[r.getRegisterCount()];for(int i=0;i<result.length;i++)result[i]=r.getStartRegister()+i;return result;}
        throw new AssertionError("expected invocation register format");
    }
    static int address(List<Instruction> body,int index){int address=0;for(int i=0;i<index;i++)address+=body.get(i).getCodeUnits();return address;}
    static int destination(List<Instruction> body,int index){int target=address(body,index)+((OffsetInstruction)body.get(index)).getCodeOffset();for(int i=0;i<body.size();i++)if(address(body,i)==target)return i;throw new AssertionError("branch destination is not instruction boundary");}
    static void invoke(Instruction instruction,Opcode opcode,String owner,String name,List<String> parameters,String result,int... args){
        check(instruction.getOpcode()==opcode,"exact invocation opcode "+name);MethodReference reference=reference(instruction);
        check(reference!=null&&reference.getDefiningClass().equals(owner)&&reference.getName().equals(name)&&reference.getParameterTypes().equals(parameters)&&reference.getReturnType().equals(result),"exact invocation descriptor "+name);
        check(Arrays.equals(registers(instruction),args),"actual live registers "+name);
    }
    static void string(Instruction instruction,int register,String value){
        check((instruction.getOpcode()==Opcode.CONST_STRING||instruction.getOpcode()==Opcode.CONST_STRING_JUMBO)
                &&((OneRegisterInstruction)instruction).getRegisterA()==register,"string register "+value);
        Reference reference=((ReferenceInstruction)instruction).getReference();
        check(reference instanceof StringReference&&value.equals(((StringReference)reference).getString()),"native string "+value);
    }
    static int calls(List<Instruction> body,String owner,String name){
        int count=0;for(Instruction instruction:body){MethodReference ref=reference(instruction);if(ref!=null&&owner.equals(ref.getDefiningClass())&&name.equals(ref.getName()))count++;}return count;
    }
    static void addCall(Map<String,Integer> counts,Method caller){String key=caller.getDefiningClass()+"->"+caller.getName();counts.put(key,counts.containsKey(key)?counts.get(key)+1:1);}
    static Map<String,Integer> callers(String... owners){Map<String,Integer> result=new TreeMap<>();for(String owner:owners)result.put(owner,result.containsKey(owner)?result.get(owner)+1:1);return result;}
    /** Whole input DEX inventory, including concrete E6l forwards. This is a
     * bounded call-path proof, not an assertion about an Intent's sending app. */
    static void nativeCallInventory(Map<String,ClassDef> input){
        Map<String,Integer> dispatch=new TreeMap<>(),profile=new TreeMap<>(),envelope=new TreeMap<>();
        for(ClassDef clazz:input.values())for(Method caller:clazz.getMethods())if(caller.getImplementation()!=null)
            for(Instruction instruction:caller.getImplementation().getInstructions()){
                MethodReference ref=reference(instruction);if(ref==null)continue;
                if(ref.getName().equals("E6l")&&ref.getReturnType().equals("V")&&ref.getParameterTypes().equals(ps(BUNDLE,HOST,SESSION)))addCall(dispatch,caller);
                if(ref.getDefiningClass().equals(ROUTER)&&ref.getName().equals("A09")&&ref.getReturnType().equals("V")&&ref.getParameterTypes().equals(ps(BUNDLE,HOST,SESSION)))addCall(profile,caller);
                if(ref.getDefiningClass().equals(BASE)&&ref.getName().equals("A03")&&ref.getReturnType().equals(BUNDLE)&&ref.getParameterTypes().equals(ps(SESSION,STRING,STRING)))addCall(envelope,caller);
            }
        check(dispatch.equals(callers("LX/0ZaY;->run","Lcom/instagram/urlhandlers/accountsdisavow/AccountsDisavowUrlHandlerActivity;->onCreate",
                URL_DELEGATE+"->A05",MAIN_LINK+"->A04",HANDLER+"->E6l",HANDLER+"->E6l","LX/02GZ;->A03")),"all seven native E6l callsites, including two concrete forwards");
        check(profile.equals(callers("Lcom/instagram/urlhandlers/followexternal/FollowExternalUrlHandlerActivity;->A2W",
                "Lcom/instagram/urlhandlers/live/LiveExternalUrlHandlerActivity;->A2W","LX/0mCj;->A04",HANDLER+"->E6l",HANDLER+"->E6l",
                "Lcom/instagram/urlhandlers/igme/IgMeExternalUrlHandlerActivity;->onCreate",
                "Lcom/instagram/urlhandlers/profilecard/ProfilecardUrlHandlerActivity;->A2W","LX/0334;->invokeSuspend")),"all eight native profile factory callsites");
        check(envelope.equals(callers("Lcom/instagram/urlhandlers/wa_iab_callback/WaIabCallbackUrlHandlerActivity;->A2W","LX/0Pse;->onClick",
                "LX/0Tgf;->A00","LX/0VAa;->BFI","Lcom/instagram/urlhandlers/accountsdisavow/AccountsDisavowUrlHandlerActivity;->onCreate",
                URL_DELEGATE+"->A00",MAIN_LINK+"->A04",BASE+"->A02",BASE+"->A04","LX/02GZ;->A03")),"all ten base-envelope helper callsites, including internal callers");
    }
    /** Independent dataflow checks explain why original_url is required and uri
     * is optional. Full body pins additionally cover every unlisted instruction. */
    static void nativeEnvelope(Map<String,ClassDef> input){
        List<Instruction> base=instructions(method(input.get(BASE),"A03"));
        check(base.size()==13,"complete native envelope builder size");
        check(base.get(0).getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)base.get(0)).getRegisterA()==2
                &&((NarrowLiteralInstruction)base.get(0)).getNarrowLiteral()==1,"native typed entry flag is true");
        string(base.get(5),0,"original_url");
        invoke(base.get(6),Opcode.INVOKE_VIRTUAL,"Landroid/os/BaseBundle;","putString",ps(STRING,STRING),"V",1,0,4);
        string(base.get(7),0,"com.instagram.url.extra.IS_ON_CREATE");
        invoke(base.get(8),Opcode.INVOKE_VIRTUAL,"Landroid/os/BaseBundle;","putBoolean",ps(STRING,"Z"),"V",1,0,2);
        string(base.get(9),0,"com.instagram.url.constants.ARGUMENTS_KEY_ANALYTICS_MODULE_NAME");
        invoke(base.get(10),Opcode.INVOKE_VIRTUAL,"Landroid/os/BaseBundle;","putString",ps(STRING,STRING),"V",1,0,5);
        check(base.get(12).getOpcode()==Opcode.RETURN_OBJECT&&((OneRegisterInstruction)base.get(12)).getRegisterA()==1,"same envelope instance returned");
        List<Instruction> urlBuilder=instructions(method(input.get(URL_DELEGATE),"A00"));
        invoke(urlBuilder.get(4),Opcode.INVOKE_STATIC,BASE,"A03",ps(SESSION,STRING,STRING),BUNDLE,4,9,0);
        check(calls(urlBuilder,BASE,"A03")==1,"URL delegate constructs one native envelope");
        List<Instruction> url=instructions(method(input.get(URL_DELEGATE),"A05"));
        invoke(url.get(11),Opcode.INVOKE_VIRTUAL,INTENT,"getDataString",ps(),STRING,12);
        invoke(url.get(354),Opcode.INVOKE_DIRECT,URL_DELEGATE,"A00",ps(INTENT,STRING),BUNDLE,11,12,2);
        invoke(url.get(403),Opcode.INVOKE_VIRTUAL,BUNDLE,"putAll",ps(BUNDLE),"V",5,0);
        invoke(url.get(418),Opcode.INVOKE_INTERFACE,"LX/0rfy;","E6l",ps(BUNDLE,HOST,SESSION),"V",4,5,6,7);
        check(calls(url,BUNDLE,"putAll")==1&&calls(url,"LX/0rfy;","E6l")==1,"URL parser merges into and dispatches one envelope");
        List<Instruction> main=instructions(method(input.get(MAIN_LINK),"A04"));
        invoke(main.get(1),Opcode.INVOKE_VIRTUAL,INTENT,"getDataString",ps(),STRING,11);
        string(main.get(111),0,"main_activity");
        invoke(main.get(112),Opcode.INVOKE_STATIC,BASE,"A03",ps(SESSION,STRING,STRING),BUNDLE,12,7,0);
        invoke(main.get(116),Opcode.INVOKE_VIRTUAL,BUNDLE,"putAll",ps(BUNDLE),"V",1,0);
        invoke(main.get(123),Opcode.INVOKE_INTERFACE,"LX/0rfy;","E6l",ps(BUNDLE,HOST,SESSION),"V",0,1,3,12);
        check(calls(main,BASE,"A03")==1&&calls(main,BUNDLE,"putAll")==1&&calls(main,"LX/0rfy;","E6l")==1,"Main merges into and dispatches one native envelope");
        List<Instruction> parser=instructions(method(input.get(HANDLER),"AZA"));
        string(parser.get(20),0,"instagram");string(parser.get(35),0,"user");string(parser.get(56),1,USER_KEY);
        invoke(parser.get(57),Opcode.INVOKE_VIRTUAL,"Landroid/os/BaseBundle;","putString",ps(STRING,STRING),"V",0,1,2);
        for(int i=24;i<118;i++)if(parser.get(i) instanceof ReferenceInstruction){Reference ref=((ReferenceInstruction)parser.get(i)).getReference();
            check(!(ref instanceof StringReference)||!((StringReference)ref).getString().equals("uri"),"instagram user branch does not create a uri key");}
        invoke(parser.get(337),Opcode.INVOKE_VIRTUAL,BUNDLE,"putParcelable",ps(STRING,"Landroid/os/Parcelable;"),"V",0,11,8);
        invoke(parser.get(338),Opcode.INVOKE_VIRTUAL,"Landroid/os/BaseBundle;","putString",ps(STRING,STRING),"V",0,10,3);
        string(parser.get(309),2,"_u");
        invoke(parser.get(385),Opcode.INVOKE_VIRTUAL,"Landroid/os/BaseBundle;","putString",ps(STRING,STRING),"V",0,10,4);
        check(destination(parser,386)==339,"_u profile joins after canonical uri write");
        List<Instruction> handler=instructions(method(input.get(HANDLER),"E6l"));
        invoke(handler.get(177),Opcode.INVOKE_STATIC,ROUTER,"A09",ps(BUNDLE,HOST,SESSION),"V",13,14,15);
        invoke(handler.get(225),Opcode.INVOKE_STATIC,ROUTER,"A09",ps(BUNDLE,HOST,SESSION),"V",13,14,15);
        check(calls(handler,ROUTER,"A09")==2,"both native profile branches retain original route bundle and owner");
        string(instructions(method(input.get("Lcom/instagram/url/UrlHandlerActivity;"),"getModuleName")).get(0),0,"url_handler");
        string(instructions(method(input.get("Lcom/instagram/url/UrlHandlerLauncherActivity;"),"getModuleName")).get(0),0,"url_handler_launcher");
        List<Instruction> react=instructions(method(input.get("LX/0ZaY;"),"run"));
        invoke(react.get(37),Opcode.INVOKE_INTERFACE,"LX/0rfy;","E6l",ps(BUNDLE,HOST,SESSION),"V",2,1,6,0);
        check(calls(react,BASE,"A03")==0&&calls(react,BUNDLE,"putAll")==0,"React direct profile route has only parser output");
        List<Instruction> direct=instructions(method(input.get("LX/02GZ;"),"A03"));
        invoke(direct.get(306),Opcode.INVOKE_STATIC,BASE,"A03",ps(SESSION,STRING,STRING),BUNDLE,0,9,7);
        check(direct.get(307).getOpcode()==Opcode.MOVE_RESULT_OBJECT&&((OneRegisterInstruction)direct.get(307)).getRegisterA()==10,"other internal route envelope is in v10");
        check(direct.get(590).getOpcode()==Opcode.IGET_OBJECT&&((TwoRegisterInstruction)direct.get(590)).getRegisterA()==3
                &&((TwoRegisterInstruction)direct.get(590)).getRegisterB()==7
                &&((ReferenceInstruction)direct.get(590)).getReference().toString().equals("LX/0065;->A01:Ljava/lang/Object;"),"direct internal E6l uses parser bundle v3");
        invoke(direct.get(605),Opcode.INVOKE_INTERFACE,"LX/0rfy;","E6l",ps(BUNDLE,HOST,SESSION),"V",2,3,1,0);
        check(calls(direct,BUNDLE,"putAll")==0,"internal envelope is never merged into direct parser route");
    }
    static int onlyHook(List<Instruction> body,String name){int index=-1;for(int i=0;i<body.size();i++){MethodReference ref=reference(body.get(i));if(ref!=null&&EXIT.equals(ref.getDefiningClass())&&name.equals(ref.getName())){check(index<0,"one "+name+" invocation");index=i;}}check(index>=0,"required hook "+name);return index;}
    static Method copy(Method method,MethodImplementation body){return new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),method.getAccessFlags(),method.getAnnotations(),method.getHiddenApiRestrictions(),ActualInstagram452Audit.orderedTries(body));}
    static void original(Method router,Method profile,Method delegate){
        check(router.getAccessFlags()==25&&router.getImplementation().getRegisterCount()==27,"original A09 public static final/register contract");
        check(ROUTER_PIN.equals(OneBackPatch.instagramNavigationDigest(router)),"complete original A09 normalized fingerprint");
        List<Instruction> route=instructions(router);
        invoke(route.get(302),Opcode.INVOKE_VIRTUAL,FRAGMENT,"setArguments",ps(BUNDLE),"V",2,0);
        check(profile.getAccessFlags()==17&&profile.getImplementation().getRegisterCount()==11,"original profile Back public final/register contract");
        check(PROFILE_PIN.equals(OneBackPatch.instagramNavigationDigest(profile)),"complete original profile Back normalized fingerprint");
        List<Instruction> body=instructions(profile);int returns=0;
        for(int i=0;i<body.size();i++)if(body.get(i).getOpcode()==Opcode.RETURN){returns++;check(i==271&&((OneRegisterInstruction)body.get(i)).getRegisterA()==2,"sole original native boolean result v2");}
        check(returns==1,"one original native return boundary");
        check(delegate.getAccessFlags()==17&&delegate.getImplementation().getRegisterCount()==8,"original URL delegate public final/register contract");
        check(URL_PIN.equals(OneBackPatch.instagramNavigationDigest(delegate)),"complete URL delegate normalized fingerprint");
    }
    /** Used also by the whole-APK verifier; only the three new methods are handled. */
    static Method restore(Method after,Method before){
        check(target(before)&&target(after),"matching profile boundary methods");
        check(after.getImplementation().getRegisterCount()==before.getImplementation().getRegisterCount(),"profile hooks add no registers or parameter shifts");
        List<Instruction> body=instructions(after);MutableMethodImplementation restored=new MutableMethodImplementation(after.getImplementation());
        if(ROUTER.equals(before.getDefiningClass())){
            int hook=onlyHook(body,"rememberInstagramExternalProfile");check(hook==303,"capture immediately after shared setArguments");
            invoke(body.get(hook-1),Opcode.INVOKE_VIRTUAL,FRAGMENT,"setArguments",ps(BUNDLE),"V",2,0);
            invoke(body.get(hook),Opcode.INVOKE_STATIC,EXIT,"rememberInstagramExternalProfile",ps(ACTIVITY,OBJECT,BUNDLE),"V",4,2,5);
            check(body.size()==instructions(before).size()+1,"exact one-instruction capture addition");
            ActualInstagram452Audit.noIncoming(after.getImplementation(),hook);
            restored.removeInstruction(hook);
        }else if(URL_DELEGATE.equals(before.getDefiningClass())){
            int gate=onlyHook(body,"dispatchInstagramExternalProfile");check(gate==10,"URL native-chain gate at original decision continuation");
            invoke(body.get(gate),Opcode.INVOKE_STATIC,EXIT,"dispatchInstagramExternalProfile",ps(ACTIVITY),"Z",3);
            check(body.get(11).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)body.get(11)).getRegisterA()==0,"URL gate uses dead decision scratch v0");
            check(body.get(12).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)body.get(12)).getRegisterA()==0&&destination(body,12)==14,"false resumes untouched native decision");
            check(body.get(13).getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)body.get(13)).getRegisterA()==5&&((NarrowLiteralInstruction)body.get(13)).getNarrowLiteral()==1,"true only selects existing native-super result");
            check(body.get(14).getOpcode()==Opcode.IGET_OBJECT&&((TwoRegisterInstruction)body.get(14)).getRegisterA()==4&&((TwoRegisterInstruction)body.get(14)).getRegisterB()==7
                    &&((ReferenceInstruction)body.get(14)).getReference().toString().equals("LX/0Rts;->A03:LX/08eI;"),"original live session load retained");
            check(body.size()==instructions(before).size()+4,"URL gate has only four new instructions");
            for(int edge:new int[]{6,8})check(destination(instructions(before),edge)==10&&destination(body,edge)==10,"both native URL entry branches must run new gate");
            for(int index:new int[]{11,12,13})ActualInstagram452Audit.noIncoming(after.getImplementation(),index);
            for(int i=0;i<4;i++)restored.removeInstruction(10);
        }else{
            int begin=onlyHook(body,"beginInstagramExternalProfileBack"),finish=onlyHook(body,"finishInstagramExternalProfileBack");
            check(begin==0&&finish==272,"entry capture and sole native return boundary positions");
            invoke(body.get(begin),Opcode.INVOKE_STATIC,EXIT,"beginInstagramExternalProfileBack",ps(OBJECT),"V",10);
            invoke(body.get(finish),Opcode.INVOKE_STATIC,EXIT,"finishInstagramExternalProfileBack",ps(OBJECT,"Z"),"Z",10,2);
            check(body.get(finish+1).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)body.get(finish+1)).getRegisterA()==2,"returned decision replaces only native result v2");
            check(body.get(finish+2).getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)body.get(finish+2)).getRegisterA()==2,"exact native boolean return preserved after decision");
            check(body.size()==instructions(before).size()+3,"only entry/finish/result additions");
            ActualInstagram452Audit.noIncoming(after.getImplementation(),begin);
            ActualInstagram452Audit.noIncoming(after.getImplementation(),finish+1);
            ActualInstagram452Audit.noIncoming(after.getImplementation(),finish+2);
            List<Instruction> nativeBody=instructions(before);int edges=0;
            for(int i=0;i<nativeBody.size();i++)if(nativeBody.get(i) instanceof OffsetInstruction){
                Opcode opcode=nativeBody.get(i).getOpcode();check(opcode!=Opcode.PACKED_SWITCH&&opcode!=Opcode.SPARSE_SWITCH&&opcode!=Opcode.FILL_ARRAY_DATA,"known profile Back contains no payload branch");
                if(destination(nativeBody,i)==271){int emitted=i<271?i+1:i+3;check(destination(body,emitted)==finish,"every native return edge enters finish decision, never bypasses it");edges++;}
            }
            check(edges>0,"actual profile multiple native return paths are audited");
            restored.removeInstruction(finish);restored.removeInstruction(finish);restored.removeInstruction(begin);
        }
        return copy(after,restored);
    }
    static void output(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception {
        Method route=method(before.get(ROUTER),"A09"),profile=method(before.get(PROFILE),"onBackPressed"),delegate=method(before.get(URL_DELEGATE),"A03");original(route,profile,delegate);
        for(Method old:new Method[]{route,profile,delegate}){
            Method changed=method(after.get(old.getDefiningClass()),old.getName());Method restored=restore(changed,old);
            RealUpgradeAudit.compare(RealUpgradeAudit.key(old),old,restored);
        }
    }
    static void reject(Map<String,ClassDef> input,String label){
        final int[] touched={0};boolean rejected=false;
        try{InstagramExternalProfilePatch.apply(input,type->{touched[0]++;return new MutableClass(input.get(type));},new LinkedHashMap<String,Integer>());}
        catch(IllegalStateException expected){rejected=true;if(label.equals("already-patched 452 URL chain")){String message=expected.getMessage().toLowerCase(Locale.ROOT);check(message.contains("original")&&message.contains("unpatched"),"rejection directs user to original unpatched input");}}
        check(rejected&&touched[0]==0,"changed contract rejected before mutable proxies: "+label);mutants++;
    }
    static Map<String,ClassDef> mutate(Map<String,ClassDef> base,String type,String name,int index,BuilderInstruction replacement){
        Map<String,ClassDef> changed=new LinkedHashMap<String,ClassDef>(base);MutableClass clazz=new MutableClass(base.get(type));
        ((MutableMethod)method(clazz,name)).getImplementation().replaceInstruction(index,replacement);changed.put(type,clazz);return changed;
    }
    static void envelopeMutations(Map<String,ClassDef> input){
        List<Instruction> base=instructions(method(input.get(BASE),"A03"));
        reject(mutate(input,BASE,"A03",5,new BuilderInstruction21c(Opcode.CONST_STRING,0,new ImmutableStringReference("changed_original_url"))),"native original URL key");
        reject(mutate(input,BASE,"A03",0,new BuilderInstruction11n(Opcode.CONST_4,2,0)),"native typed entry flag becomes false");
        reject(mutate(input,BASE,"A03",6,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,3,1,0,5,0,0,reference(base.get(6)))),"envelope stores module instead of incoming URL");
        List<Instruction> parser=instructions(method(input.get(HANDLER),"AZA"));
        reject(mutate(input,HANDLER,"AZA",385,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,3,0,10,3,0,0,reference(parser.get(385)))),"_u parser stores route prefix instead of username");
        List<Instruction> builder=instructions(method(input.get(URL_DELEGATE),"A00"));
        reject(mutate(input,URL_DELEGATE,"A00",4,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,4,0,0,0,0,reference(builder.get(4)))),"URL envelope receives module in URL argument");
        List<Instruction> url=instructions(method(input.get(URL_DELEGATE),"A05"));
        reject(mutate(input,URL_DELEGATE,"A05",403,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,2,0,5,0,0,0,reference(url.get(403)))),"URL transfer merges into the wrong Bundle");
        List<Instruction> main=instructions(method(input.get(MAIN_LINK),"A04"));
        reject(mutate(input,MAIN_LINK,"A04",116,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,2,0,1,0,0,0,reference(main.get(116)))),"Main transfer merges into the wrong Bundle");
        reject(mutate(input,HANDLER,"E6l",205,new BuilderInstruction21c(Opcode.CONST_STRING,0,new ImmutableStringReference("InstagramUrlLaunchConstants.EXTRA_USER_ID"))),"native handler stops recognizing the username branch");
        reject(mutate(input,"Lcom/instagram/url/UrlHandlerActivity;","getModuleName",0,new BuilderInstruction21c(Opcode.CONST_STRING,0,new ImmutableStringReference("changed_url_handler"))),"URL host analytics module changes");
        reject(mutate(input,"Lcom/instagram/url/UrlHandlerLauncherActivity;","getModuleName",0,new BuilderInstruction21c(Opcode.CONST_STRING,0,new ImmutableStringReference("changed_url_launcher"))),"URL launcher analytics module changes");
    }
    static Map<String,ClassDef> serialized(Collection<? extends ClassDef> classes)throws Exception {
        List<ClassDef> stripped=new ArrayList<ClassDef>();
        for(ClassDef clazz:classes){List<Method> methods=new ArrayList<Method>();for(Method method:clazz.getMethods()){
            MethodImplementation body=method.getImplementation();if(body!=null)body=new ImmutableMethodImplementation(body.getRegisterCount(),body.getInstructions(),body.getTryBlocks(),Collections.emptyList());methods.add(copy(method,body));
        }stripped.add(new ImmutableClassDef(clazz.getType(),clazz.getAccessFlags(),clazz.getSuperclass(),clazz.getInterfaces(),clazz.getSourceFile(),clazz.getAnnotations(),clazz.getFields(),methods));}
        MemoryDataStore store=new MemoryDataStore();DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),stripped));byte[] bytes=Arrays.copyOf(store.getBuffer(),store.getSize());store.close();
        Map<String,ClassDef> result=new LinkedHashMap<String,ClassDef>();for(ClassDef clazz:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses())result.put(clazz.getType(),clazz);return result;
    }
    static void originalAndMutations(Map<String,ClassDef> input)throws Exception {
        original(method(input.get(ROUTER),"A09"),method(input.get(PROFILE),"onBackPressed"),method(input.get(URL_DELEGATE),"A03"));InstagramExternalProfilePatch.verifyOriginal(input);
        nativeEnvelope(input);nativeCallInventory(input);envelopeMutations(input);
        Map<String,MutableClass> changed=new LinkedHashMap<String,MutableClass>();Map<String,Integer> counts=new LinkedHashMap<String,Integer>();
        InstagramExternalProfilePatch.apply(input,type->changed.computeIfAbsent(type,key->new MutableClass(input.get(key))),counts);
        check(changed.keySet().equals(new HashSet<String>(Arrays.asList(ROUTER,PROFILE,URL_DELEGATE))),"only router, profile Back and URL delegate classes changed");
        Map<String,ClassDef> emitted=serialized(changed.values());output(input,emitted);
        int unrelated=0;for(String type:emitted.keySet()){
            ClassDef old=input.get(type),fresh=emitted.get(type);RealUpgradeAudit.classMeta(old,fresh);Map<String,Method> om=RealUpgradeAudit.methods(old),nm=RealUpgradeAudit.methods(fresh);
            check(om.keySet().equals(nm.keySet()),"native method inventory preserved "+type);for(String key:om.keySet())if(!target(om.get(key))){RealUpgradeAudit.compare(key,om.get(key),nm.get(key));unrelated++;}
        }
        Method route=method(input.get(ROUTER),"A09");MethodReference setArguments=reference(instructions(route).get(302));
        reject(mutate(input,ROUTER,"A09",302,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,2,2,1,0,0,0,setArguments)),"capture predecessor register dataflow");
        reject(mutate(input,ROUTER,"A09",0,new BuilderInstruction11n(Opcode.CONST_4,2,1)),"route branch policy literal");
        reject(mutate(input,PROFILE,"onBackPressed",271,new BuilderInstruction11x(Opcode.RETURN,1)),"native boolean return register");
        reject(mutate(input,URL_DELEGATE,"A03",9,new BuilderInstruction11n(Opcode.CONST_4,5,1)),"URL direct-return decision literal");
        Instruction modalEntry=instructions(method(input.get("LX/03CG;"),"onBackPressed")).get(0);
        reject(mutate(input,"LX/03CG;","onBackPressed",0,new BuilderInstruction22c(Opcode.IGET_OBJECT,0,6,((ReferenceInstruction)modalEntry).getReference())),"Main modal native handler receiver dataflow");
        reject(mutate(input,"LX/01MU;","A0B",0,new BuilderInstruction11n(Opcode.CONST_4,0,0)),"read-only native popup getter body");
        Map<String,ClassDef> wrongView=new LinkedHashMap<String,ClassDef>(input);ClassDef fragment=input.get(FRAGMENT);List<Method> fragmentMethods=new ArrayList<Method>();
        for(Method method:fragment.getMethods())fragmentMethods.add(method.getName().equals("getView")&&method.getParameterTypes().isEmpty()
            ?new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),method.getAccessFlags()|16,method.getAnnotations(),method.getHiddenApiRestrictions(),method.getImplementation()):method);
        wrongView.put(FRAGMENT,new ImmutableClassDef(fragment.getType(),fragment.getAccessFlags(),fragment.getSuperclass(),fragment.getInterfaces(),fragment.getSourceFile(),fragment.getAnnotations(),fragment.getFields(),fragmentMethods));
        reject(wrongView,"Fragment.getView must retain its actual public nonfinal ABI");
        Map<String,ClassDef> missing=new LinkedHashMap<String,ClassDef>(input);missing.remove(PROFILE);reject(missing,"missing profile contract");
        Map<String,ClassDef> prior=new LinkedHashMap<String,ClassDef>(input);String urlType="Lcom/instagram/url/UrlHandlerActivity;";ClassDef url=input.get(urlType);List<Method> urlMethods=new ArrayList<Method>();
        Method nativeBack=null;for(Method method:url.getMethods()){urlMethods.add(method);if(method.getName().equals("onBackPressed"))nativeBack=method;}
        check(nativeBack!=null,"URL native Back exists for historical-helper rejection fixture");
        urlMethods.add(new ImmutableMethod(urlType,"hiroOneBack$onBackPressed",nativeBack.getParameters(),nativeBack.getReturnType(),0x1002,nativeBack.getAnnotations(),nativeBack.getHiddenApiRestrictions(),nativeBack.getImplementation()));
        prior.put(urlType,new ImmutableClassDef(url.getType(),url.getAccessFlags(),url.getSuperclass(),url.getInterfaces(),url.getSourceFile(),url.getAnnotations(),url.getFields(),urlMethods));reject(prior,"already-patched 452 URL chain");
        Map<String,ClassDef> duplicate=new LinkedHashMap<String,ClassDef>(input);duplicate.putAll(emitted);reject(duplicate,"already inserted native hooks");
        // Corrupt only an output return edge to bypass finish. A simple strip-and-
        // compare verifier would miss this after removing the two new opcodes.
        MutableClass bypass=new MutableClass(emitted.get(PROFILE));MutableMethod back=(MutableMethod)method(bypass,"onBackPressed");
        List<Instruction> instructions=instructions(back);int edge=-1;for(int i=0;i<instructions.size();i++)if(instructions.get(i) instanceof OffsetInstruction&&destination(instructions,i)==272){edge=i;break;}
        check(edge>=0,"output has native return edge for bypass mutation");Instruction originalEdge=instructions.get(edge);
        check(originalEdge instanceof OneRegisterInstruction,"chosen branch has one condition register");
        back.getImplementation().replaceInstruction(edge,new BuilderInstruction21t(originalEdge.getOpcode(),((OneRegisterInstruction)originalEdge).getRegisterA(),back.getImplementation().newLabelForIndex(274)));
        boolean rejected=false;try{restore(back,method(input.get(PROFILE),"onBackPressed"));}catch(AssertionError expected){rejected=true;}
        check(rejected,"emitted branch bypassing finish is rejected independently");mutants++;
        System.out.println("Contract/output mutation rejections="+mutants+"; unchanged router/profile/URL-delegate methods="+unrelated+"; serialized hook counts="+counts);
    }
    public static void main(String[] args)throws Exception {
        if(args.length<2||!Arrays.asList("original","output").contains(args[0]))throw new IllegalArgumentException("original ORIGINAL_APK | output ORIGINAL_APK PATCHED_APK");
        Map<String,ClassDef> input=RealUpgradeAudit.read(args[1]);
        if(args[0].equals("original")){check(args.length==2,"original arguments");originalAndMutations(input);}
        else{check(args.length==3,"output arguments");output(input,RealUpgradeAudit.read(args[2]));}
        System.out.println("PASS Instagram130ProfileLinkAudit "+args[0]+": assertions="+checks+", rejectedMutations="+mutants+"; exact native dataflow, guards, result and all return edges preserved; no device execution");
    }
}
