import java.io.File;
import java.util.*;
import app.hiro.quicksearch.patches.QuickSearchRecentsPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Independent scope audit: undo only reviewed insertions/redirects, then require original DEX bytes. */
public final class DexAudit {
    private static final String P="Ljp/ddo/sugihiro/quicksearch/";
    private static final String MAIN=P+"activity/MainActivity;", WEB=P+"activity/WebActivity;", CONFIG=P+"activity/ConfigActivity;";
    private static final String RUNTIME="Lapp/hiro/quicksearch/runtime/SearchTask;", CLEANUP="Lapp/hiro/quicksearch/runtime/SearchTask$Cleanup;";
    private static final String CREATE="onCreate(Landroid/os/Bundle;)V", RESULT="onActivityResult(IILandroid/content/Intent;)V";
    private static final String DESTROY="onDestroy()V", BACK="onBackPressed()V", KEY="dispatchKeyEvent(Landroid/view/KeyEvent;)Z";
    private static final String CLEAN="hiroQuickSearchCleanup()V", SEARCH="search(Ljava/lang/String;Ljava/lang/String;)V";
    private static final String PRE_IME="onKeyPreIme(ILandroid/view/KeyEvent;)Z";
    private static final String ACTIVITY="Landroid/app/Activity;", DIALOG="Landroid/app/Dialog;", INTENT="Landroid/content/Intent;";
    private static final Map<String,Set<String>> CHANGED=new LinkedHashMap<>(), ADDED=new LinkedHashMap<>();
    private static int assertions;
    static {
        changed(MAIN,"actionAfterSearched()V",SEARCH,CREATE,RESULT,"showHelp()V");
        changed(WEB,CREATE,RESULT,DESTROY);changed(CONFIG,CREATE);
        changed(P+"activity/MainActivity$10;","onCancel(Landroid/content/DialogInterface;)V");
        changed(P+"activity/CommonActivity;","showHistoryDialog("+P+"activity/Searchable;Landroid/widget/EditText;)V");
        changed(P+"activity/CommonActivity$3;","onClick(Landroid/view/View;)V");
        changed(P+"adapter/HistoryArrayAdapter;","delete()V");
        changed(P+"custom/CustomSearhConfigDialog;","show("+P+"bean/CustomSearchBean;)Landroid/view/View;");
        changed("Landroidx/fragment/app/DialogFragment;","onStart()V");
        changed(P+"activity/ui/main/PlaceholderFragment;","navigateDefaultBrowser(Landroid/app/Activity;Ljava/lang/String;)V");
        changed(P+"activity/ui/main/PlaceholderFragment$1;","shouldOverrideUrlLoading(Landroid/webkit/WebView;Landroid/webkit/WebResourceRequest;)Z");
        changed("Landroidx/appcompat/widget/AppCompatAutoCompleteTextView;");
        changed("Landroidx/appcompat/widget/AppCompatEditText;");
        ADDED.put(MAIN,set(BACK,KEY,DESTROY,CLEAN));ADDED.put(WEB,set(BACK,KEY));ADDED.put(CONFIG,set(BACK,KEY,DESTROY));
        ADDED.put("Landroidx/appcompat/widget/AppCompatAutoCompleteTextView;",set(PRE_IME));
        ADDED.put("Landroidx/appcompat/widget/AppCompatEditText;",set(PRE_IME));
    }
    private static Set<String> set(String...items){return new LinkedHashSet<>(Arrays.asList(items));}
    private static void changed(String owner,String...items){CHANGED.put(owner,set(items));}
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);assertions++;}
    private interface Action{void run()throws Exception;}
    private static void rejected(Action action,String reason)throws Exception{
        boolean rejected=false;try{action.run();}catch(IllegalStateException|IllegalArgumentException expected){rejected=true;}
        check(rejected,reason);
    }
    private static Map<String,ClassDef> classes(String file)throws Exception{
        Map<String,ClassDef> result=new TreeMap<>();
        MultiDexContainer<? extends DexFile> container=DexFileFactory.loadDexContainer(new File(file),Opcodes.getDefault());
        for(String entry:container.getDexEntryNames())for(ClassDef c:container.getEntry(entry).getDexFile().getClasses())
            check(result.put(c.getType(),c)==null,"duplicate class "+c.getType());
        return result;
    }
    private static byte[] canonical(ClassDef c)throws Exception{
        check(c!=null,"missing class");MemoryDataStore store=new MemoryDataStore();
        try{DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(c)));
            return Arrays.copyOf(store.getBuffer(),store.getSize());}finally{store.close();}
    }
    private static byte[] canonical(Method m)throws Exception{
        return canonical(new ImmutableClassDef(m.getDefiningClass(),1,"Ljava/lang/Object;",Collections.<String>emptyList(),null,
                Collections.<Annotation>emptySet(),Collections.<Field>emptyList(),Collections.singleton(m)));
    }
    private static String key(MethodReference m){StringBuilder b=new StringBuilder(m.getName()).append('(');
        for(CharSequence p:m.getParameterTypes())b.append(p);return b.append(')').append(m.getReturnType()).toString();}
    private static Map<String,Method> methods(ClassDef c){Map<String,Method> result=new TreeMap<>();
        for(Method m:c.getMethods())check(result.put(key(m),m)==null,"duplicate method");return result;}
    private static List<Instruction> instructions(Method m){List<Instruction> result=new ArrayList<>();
        if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())result.add(i);return result;}
    private static MethodReference call(Instruction i){
        if(!(i instanceof ReferenceInstruction))return null;Reference r=((ReferenceInstruction)i).getReference();
        return r instanceof MethodReference?(MethodReference)r:null;
    }
    private static MethodReference ref(String owner,String name,String result,String...params){
        return new ImmutableMethodReference(owner,name,Arrays.asList(params),result);
    }
    private static int thisRegister(Method m){int words=1;for(CharSequence type:m.getParameterTypes())words+=("J".contentEquals(type)||"D".contentEquals(type))?2:1;
        return m.getImplementation().getRegisterCount()-words;
    }
    private static void requireCall(Instruction i,Opcode opcode,MethodReference target,int...registers){
        check(i.getOpcode()==opcode&&target.equals(call(i)),"unexpected call: "+call(i)+"; expected "+target);
        check(i instanceof FiveRegisterInstruction,"expected reviewed 35c encoding");
        FiveRegisterInstruction r=(FiveRegisterInstruction)i;int[] actual={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};
        check(r.getRegisterCount()==registers.length,"invoke word count");
        for(int n=0;n<registers.length;n++)check(actual[n]==registers[n],"wrong invoke argument "+n+" for "+target);
    }
    private static int address(List<? extends Instruction> list,int index){int address=0;for(int n=0;n<index;n++)address+=list.get(n).getCodeUnits();return address;}
    private static int branchIndex(List<? extends Instruction> list,int index){int target=address(list,index)+((OffsetInstruction)list.get(index)).getCodeOffset();
        for(int n=0;n<list.size();n++)if(address(list,n)==target)return n;return -1;}
    private static void fieldLoad(Instruction i,String name,String type,int destination,int receiver){
        check(i.getOpcode()==Opcode.IGET_OBJECT,"field load opcode");
        check(i instanceof TwoRegisterInstruction&&((TwoRegisterInstruction)i).getRegisterA()==destination
                &&((TwoRegisterInstruction)i).getRegisterB()==receiver,"field load register operands");
        check(new ImmutableFieldReference(MAIN,name,type).equals(((ReferenceInstruction)i).getReference()),"field load target "+name);
    }
    private static MethodReference originalCall(Method original,String name){MethodReference found=null;
        for(Instruction i:instructions(original)){MethodReference c=call(i);if(c!=null&&name.equals(c.getName())){
            check(found==null||found.equals(c),"ambiguous original call "+name);found=c;}}
        check(found!=null,"original call missing "+name);return found;
    }
    private static BuilderInstruction virtualCall(Instruction i,MethodReference target){
        FiveRegisterInstruction r=(FiveRegisterInstruction)i;
        return new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,r.getRegisterCount(),r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),target);
    }
    private static MethodReference expectedRuntime(String name){
        switch(name){
            case "finish":case "attach":case "detach":return ref(RUNTIME,name,"V",ACTIVITY);
            case "isClosing":return ref(RUNTIME,name,"Z",ACTIVITY);
            case "startExternal":return ref(RUNTIME,name,"V",ACTIVITY,INTENT);
            case "showPlatform":return ref(RUNTIME,name,"Landroid/app/AlertDialog;","Landroid/app/AlertDialog$Builder;");
            case "showDialog":case "bindDialog":case "closeDialog":return ref(RUNTIME,name,"V",DIALOG);
            case "bindMain":return ref(RUNTIME,name,"V",ACTIVITY,DIALOG,"Landroid/view/View;");
            default:throw new AssertionError("unexpected runtime hook "+name);
        }
    }

    /** Undo only the approved hook encodings. Any extra change survives and fails byte-for-byte comparison. */
    private static void originalMethodPreserved(Method original,Method patched)throws Exception{
        String owner=original.getDefiningClass(),method=key(original);MutableMethod restored=new MutableMethod(patched);
        check(original.getImplementation().getRegisterCount()==patched.getImplementation().getRegisterCount(),"original register count changed "+owner+method);
        List<BuilderInstruction> list=restored.getImplementation().getInstructions();
        int redirects=0,guards=0,mainBinds=0,attaches=0,detaches=0,dialogBinds=0;
        for(int n=list.size()-1;n>=0;n--){
            Instruction i=list.get(n);MethodReference c=call(i);if(c==null||!RUNTIME.equals(c.getDefiningClass()))continue;
            String hook=c.getName();check(c.equals(expectedRuntime(hook)),"runtime ABI differs at "+owner+method);
            check(i.getOpcode()==Opcode.INVOKE_STATIC&&i instanceof FiveRegisterInstruction,"runtime invoke format");
            int receiver=((FiveRegisterInstruction)i).getRegisterC();
            if("attach".equals(hook)){
                check(CREATE.equals(method)&&(WEB.equals(owner)||CONFIG.equals(owner)),"attach escaped reviewed site");
                requireCall(i,Opcode.INVOKE_STATIC,expectedRuntime(hook),WEB.equals(owner)?2:thisRegister(original));
                if(CONFIG.equals(owner))check(n==1&&list.get(0).getOpcode()==Opcode.INVOKE_SUPER,"Config attach must precede p0 reuse");
                else check(n==list.size()-2&&list.get(n+1).getOpcode()==Opcode.RETURN_VOID,"Web attach must be at final return with v2 receiver");
                restored.getImplementation().removeInstruction(n);attaches++;
            }else if("detach".equals(hook)){
                check(WEB.equals(owner)&&DESTROY.equals(method)&&n==0,"Web detach placement");
                requireCall(i,Opcode.INVOKE_STATIC,expectedRuntime(hook),thisRegister(original));
                restored.getImplementation().removeInstruction(n);detaches++;
            }else if("bindMain".equals(hook)){
                check(MAIN.equals(owner)&&CREATE.equals(method)&&n==list.size()-2,"Main bind placement");
                int self=thisRegister(original);requireCall(i,Opcode.INVOKE_STATIC,expectedRuntime(hook),self,0,1);
                fieldLoad(list.get(n-2),"alertDialog","Landroid/app/AlertDialog;",0,self);
                fieldLoad(list.get(n-1),"inputView","Landroid/view/View;",1,self);
                for(int k=0;k<3;k++)restored.getImplementation().removeInstruction(n-k);n-=2;mainBinds++;
            }else if("bindDialog".equals(hook)){
                check(owner.equals(P+"custom/CustomSearhConfigDialog;"),"standalone bindDialog escaped custom dialog");
                check(n>=2&&list.get(n-1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"custom dialog must bind actual show result");
                int result=((OneRegisterInstruction)list.get(n-1)).getRegisterA();
                requireCall(i,Opcode.INVOKE_STATIC,expectedRuntime(hook),result);
                check(call(list.get(n-2))!=null&&"Landroidx/appcompat/app/AlertDialog$Builder;".equals(call(list.get(n-2)).getDefiningClass())
                        &&"show".equals(call(list.get(n-2)).getName()),"custom dialog binds correct Builder return");
                restored.getImplementation().removeInstruction(n);dialogBinds++;
            }else if("isClosing".equals(hook)){
                check((MAIN.equals(owner)||WEB.equals(owner))&&RESULT.equals(method)&&n==0,"closing guard must precede result processing");
                requireCall(i,Opcode.INVOKE_STATIC,expectedRuntime(hook),thisRegister(original));
                check(list.get(1).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)list.get(1)).getRegisterA()==0,"closing guard result");
                check(list.get(2).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)list.get(2)).getRegisterA()==0
                        &&branchIndex(list,2)==4,"closing=false must enter original result handler");
                check(list.get(3).getOpcode()==Opcode.RETURN_VOID,"closing=true must return immediately");
                for(int k=3;k>=0;k--)restored.getImplementation().removeInstruction(k);guards++;
            }else{
                String oldName;
                if("finish".equals(hook)){
                    check((MAIN.equals(owner)&&"actionAfterSearched()V".equals(method))
                            ||(owner.equals(P+"activity/MainActivity$10;")&&method.startsWith("onCancel(")),"finish redirect escaped reviewed callbacks");oldName="finish";
                }else if("startExternal".equals(hook)){
                    check((MAIN.equals(owner)&&SEARCH.equals(method))||owner.startsWith(P+"activity/ui/main/PlaceholderFragment"),"external launch redirect escaped search/browser handlers");oldName="startActivity";
                }else if("closeDialog".equals(hook)){
                    check(owner.equals(P+"activity/CommonActivity$3;")&&method.equals("onClick(Landroid/view/View;)V"),"history close redirect scope");oldName="cancel";
                }else if("showPlatform".equals(hook)||"showDialog".equals(hook)){oldName="show";
                }else throw new AssertionError("unreviewed hook "+hook);
                MethodReference old=originalCall(original,oldName);
                if("showPlatform".equals(hook))check("Landroid/app/AlertDialog$Builder;".equals(old.getDefiningClass()),"platform dialog wrapper target");
                if("showDialog".equals(hook))check("Landroid/app/Dialog;".equals(old.getDefiningClass()),"DialogFragment wrapper target");
                restored.getImplementation().replaceInstruction(n,virtualCall(i,old));redirects++;
            }
        }
        check(Arrays.equals(canonical(original),canonical(restored)),"non-hook method content changed: "+owner+method);
        if(CREATE.equals(method)&&MAIN.equals(owner))check(mainBinds==1,"Main UI bind count");
        if(CREATE.equals(method)&&(WEB.equals(owner)||CONFIG.equals(owner)))check(attaches==1,"Activity attach count");
        if(RESULT.equals(method))check(guards==1,"result early guard count");
        if(WEB.equals(owner)&&DESTROY.equals(method))check(detaches==1,"Web destroy detach count");
        if(owner.equals(P+"custom/CustomSearhConfigDialog;"))check(dialogBinds==1,"custom dialog bind count");
        check(redirects+guards+mainBinds+attaches+detaches+dialogBinds>0,"approved method did not change");
    }

    private static void newMethodAudit(ClassDef original,Method method){
        String name=key(method);List<Instruction> list=instructions(method);int self=thisRegister(method);
        if(BACK.equals(name)){
            check(list.size()==2,"Back override must be only finish plus return");
            requireCall(list.get(0),Opcode.INVOKE_STATIC,expectedRuntime("finish"),self);
            check(list.get(1).getOpcode()==Opcode.RETURN_VOID,"Back returns after task exit");
        }else if(DESTROY.equals(name)){
            check(list.size()==3,"destroy override shape");
            requireCall(list.get(0),Opcode.INVOKE_STATIC,expectedRuntime("detach"),self);
            requireCall(list.get(1),Opcode.INVOKE_SUPER,ref(original.getSuperclass(),"onDestroy","V"),self);
            check(list.get(2).getOpcode()==Opcode.RETURN_VOID,"destroy returns");
        }else if(KEY.equals(name)||PRE_IME.equals(name)){
            boolean preIme=PRE_IME.equals(name);
            if(preIme)requireCall(list.get(0),Opcode.INVOKE_STATIC,ref(RUNTIME,"handleBackKeyFromView","Z","Landroid/view/View;","Landroid/view/KeyEvent;"),self,self+2);
            else requireCall(list.get(0),Opcode.INVOKE_STATIC,ref(RUNTIME,"handleBackKey","Z",ACTIVITY,"Landroid/view/KeyEvent;"),self,self+1);
            check(list.get(1).getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)list.get(1)).getRegisterA()==0,"key helper result register");
            check(list.get(2).getOpcode()==Opcode.IF_EQZ&&((OneRegisterInstruction)list.get(2)).getRegisterA()==0,"key dispatch false delegates");
            int fallback=branchIndex(list,2);
            check(fallback==5,"key fallback target follows consumed return");
            check(list.get(3).getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)list.get(3)).getRegisterA()==0
                    &&((NarrowLiteralInstruction)list.get(3)).getNarrowLiteral()==1,"consumed Back returns true");
            check(list.get(4).getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)list.get(4)).getRegisterA()==0,"consumed Back returns before superclass");
            if(preIme)requireCall(list.get(fallback),Opcode.INVOKE_SUPER,ref(original.getSuperclass(),"onKeyPreIme","Z","I","Landroid/view/KeyEvent;"),self,self+1,self+2);
            else requireCall(list.get(fallback),Opcode.INVOKE_SUPER,ref(original.getSuperclass(),"dispatchKeyEvent","Z","Landroid/view/KeyEvent;"),self,self+1);
            check(list.get(fallback+1).getOpcode()==Opcode.MOVE_RESULT&&list.get(fallback+2).getOpcode()==Opcode.RETURN&&list.size()==fallback+3,"non-Back returns superclass result");
        }else if(CLEAN.equals(name)){
            cleanupBridgeAudit(method);
        }else throw new AssertionError("unexpected new method "+name);
    }

    private static void cleanupBridgeAudit(Method method){
        List<Instruction> list=instructions(method);int self=thisRegister(method),cancel=-1,stop=-1,handler=-1,clearFlag=-1;
        for(int n=0;n<list.size();n++){
            Instruction i=list.get(n);MethodReference c=call(i);
            if(c!=null&&"cancel".equals(c.getName())&&"Z".equals(c.getReturnType())){
                check("Ljava/util/TimerTask;".equals(c.getDefiningClass())||c.getDefiningClass().equals(P+"activity/MainActivity$ClipBoaedTimerTask;"),"only TimerTask cancel");
                check(cancel<0,"one TimerTask cancel");cancel=n;
            }else if(c!=null&&"stopClipBoardTimer".equals(c.getName())){requireCall(i,Opcode.INVOKE_DIRECT,ref(MAIN,"stopClipBoardTimer","V"),self);stop=n;
            }else if(c!=null&&"removeCallbacksAndMessages".equals(c.getName())){
                check(c.equals(ref("Landroid/os/Handler;","removeCallbacksAndMessages","V","Ljava/lang/Object;")),"handler cleanup target");
                FiveRegisterInstruction r=(FiveRegisterInstruction)i;
                check(r.getRegisterCount()==2&&r.getRegisterD()==1,"handler callback token is v1");
                check(n>0&&list.get(n-1).getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)list.get(n-1)).getRegisterA()==1
                        &&((NarrowLiteralInstruction)list.get(n-1)).getNarrowLiteral()==0,"removeCallbacksAndMessages(null)");handler=n;
            }else if(i.getOpcode()==Opcode.SPUT_BOOLEAN){
                check(new ImmutableFieldReference(MAIN,"isConfig","Z").equals(((ReferenceInstruction)i).getReference()),"only isConfig static flag reset");
                int reg=((OneRegisterInstruction)i).getRegisterA();boolean zero=false;
                for(int k=n-1;k>=0;k--){Instruction before=list.get(k);if(before instanceof OneRegisterInstruction&&((OneRegisterInstruction)before).getRegisterA()==reg){
                    zero=before.getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)before).getNarrowLiteral()==0;break;}}
                check(zero,"isConfig reset to false");clearFlag=n;
            }
        }
        check(cancel>=0&&stop>cancel&&handler>stop&&clearFlag>handler,"cleanup order: task cancel, timer stop, Handler drain, isConfig reset");
        fieldLoad(list.get(0),"timerTask",P+"activity/MainActivity$ClipBoaedTimerTask;",0,self);
        fieldLoad(list.get(stop+1),"handler","Landroid/os/Handler;",0,self);
        check(list.get(1).getOpcode()==Opcode.IF_EQZ&&branchIndex(list,1)>cancel,"null TimerTask guard");
        check(list.get(list.size()-1).getOpcode()==Opcode.RETURN_VOID,"cleanup returns");
    }

    private static void stopTimerAudit(ClassDef main){
        Method m=methods(main).get("stopClipBoardTimer()V");List<Instruction> list=instructions(m);
        boolean cancel=false,zero=false,timerNull=false,taskNull=false;
        for(Instruction i:list){MethodReference c=call(i);
            if(c!=null&&c.equals(ref("Ljava/util/Timer;","cancel","V")))cancel=true;
            if(i.getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)i).getNarrowLiteral()==0)zero=true;
            if(i.getOpcode()==Opcode.IPUT_OBJECT&&((TwoRegisterInstruction)i).getRegisterA()==0){
                Reference r=((ReferenceInstruction)i).getReference();
                if(r.equals(new ImmutableFieldReference(MAIN,"timer","Ljava/util/Timer;")))timerNull=zero;
                if(r.equals(new ImmutableFieldReference(MAIN,"timerTask",P+"activity/MainActivity$ClipBoaedTimerTask;")))taskNull=zero;
            }
        }
        check(cancel&&timerNull&&taskNull,"preserved stopClipBoardTimer cancels Timer and clears timer/timerTask");
    }

    private static void classAudit(ClassDef original,ClassDef patched)throws Exception{
        String owner=original.getType();Set<String> changed=CHANGED.get(owner),added=ADDED.getOrDefault(owner,Collections.<String>emptySet());
        Map<String,Method> before=methods(original),after=methods(patched);Set<String> expected=new TreeSet<>(before.keySet());expected.addAll(added);
        check(after.keySet().equals(expected),"method inventory changed unexpectedly "+owner);
        List<String> interfaces=new ArrayList<>(original.getInterfaces());if(MAIN.equals(owner))interfaces.add(CLEANUP);
        ClassDef expectedHeader=new ImmutableClassDef(owner,original.getAccessFlags(),original.getSuperclass(),interfaces,original.getSourceFile(),original.getAnnotations(),original.getFields(),Collections.<Method>emptyList());
        ClassDef actualHeader=new ImmutableClassDef(owner,patched.getAccessFlags(),patched.getSuperclass(),patched.getInterfaces(),patched.getSourceFile(),patched.getAnnotations(),patched.getFields(),Collections.<Method>emptyList());
        check(Arrays.equals(canonical(expectedHeader),canonical(actualHeader)),"class declaration/fields/annotations changed unexpectedly "+owner);
        for(String name:before.keySet()){
            if(changed.contains(name))originalMethodPreserved(before.get(name),after.get(name));
            else check(Arrays.equals(canonical(before.get(name)),canonical(after.get(name))),"unapproved method changed "+owner+name);
        }
        for(String name:added)newMethodAudit(original,after.get(name));
        if(MAIN.equals(owner))stopTimerAudit(patched);
    }

    private static Map<String,MutableClass> mutableTargets(Map<String,ClassDef> original){Map<String,MutableClass> map=new LinkedHashMap<>();
        for(String type:CHANGED.keySet()){check(original.containsKey(type),"missing reviewed class "+type);map.put(type,new MutableClass(original.get(type)));}return map;}
    private static Map<String,byte[]> snapshot(Map<String,MutableClass> map)throws Exception{Map<String,byte[]> saved=new HashMap<>();
        for(String key:map.keySet())saved.put(key,canonical(map.get(key)));return saved;}
    private static void unchangedSnapshot(Map<String,MutableClass> map,Map<String,byte[]> before)throws Exception{
        for(String type:before.keySet())check(Arrays.equals(before.get(type),canonical(map.get(type))),"partial change after rejected all-class plan "+type);}
    private static void rejectionCases(Map<String,ClassDef> original,Map<String,MutableClass> applied)throws Exception{
        for(final String version:Arrays.asList("0.4.4","0.4.6",""))rejected(()->QuickSearchRecentsPatch.validatePackage("jp.ddo.sugihiro.quicksearch",version,36L),"unsupported version accepted");
        rejected(()->QuickSearchRecentsPatch.validatePackage("other.app","0.4.5",36L),"unsupported package accepted");
        rejected(()->QuickSearchRecentsPatch.validatePackage("jp.ddo.sugihiro.quicksearch","0.4.5",37L),"unsupported code accepted");
        Map<String,byte[]> patchedBefore=snapshot(applied);rejected(()->QuickSearchRecentsPatch.applyAll(applied),"double patch accepted");unchangedSnapshot(applied,patchedBefore);
        final Map<String,MutableClass> invalid=mutableTargets(original);MutableClass last=invalid.get(P+"activity/ui/main/PlaceholderFragment$1;");boolean changed=false;
        for(MutableMethod m:last.getMethods())if(key(m).startsWith("shouldOverrideUrlLoading(")){
            List<BuilderInstruction> list=m.getImplementation().getInstructions();for(int n=0;n<list.size();n++)if(list.get(n).getOpcode()==Opcode.CONST_4){
                Instruction old=list.get(n);m.getImplementation().replaceInstruction(n,new BuilderInstruction11n(Opcode.CONST_4,
                        ((OneRegisterInstruction)old).getRegisterA(),((NarrowLiteralInstruction)old).getNarrowLiteral()^1));changed=true;break;}}
        check(changed,"late-class mutation fixture found");Map<String,byte[]> before=snapshot(invalid);
        rejected(()->QuickSearchRecentsPatch.applyAll(invalid),"unknown code variant accepted");unchangedSnapshot(invalid,before);
    }

    private static void merge(String baseFile,String additionFile,String outputFile)throws Exception{
        Map<String,ClassDef> base=classes(baseFile),addition=classes(additionFile),merged=new TreeMap<>();int oldQuickSearch=0,preserved=0;
        for(ClassDef c:base.values())if(c.getType().startsWith("Lapp/hiro/quicksearch/"))oldQuickSearch++;else{merged.put(c.getType(),c);preserved++;}
        check(oldQuickSearch==3&&preserved==238,"expected base160 loader inventory 3 QuickSearch + 238 retained classes");
        for(ClassDef c:addition.values()){
            check(c.getType().startsWith("Lapp/hiro/quicksearch/"),"new loader class outside QuickSearch namespace "+c.getType());
            check(merged.put(c.getType(),c)==null,"new loader collision");}
        DexPool.writeTo(outputFile,new ImmutableDexFile(Opcodes.getDefault(),merged.values()));Map<String,ClassDef> read=classes(outputFile);
        check(read.keySet().equals(merged.keySet()),"merged loader inventory mismatch");
        for(ClassDef c:merged.values())check(Arrays.equals(canonical(c),canonical(read.get(c.getType()))),"merged loader class changed "+c.getType());
        System.out.println("PASS bundle replacement merge: baseline non-QuickSearch classes preserved="+preserved+"; old QuickSearch classes="+oldQuickSearch+"; new="+addition.size());
    }

    private static void finalApkAudit(Map<String,ClassDef> original,Map<String,ClassDef> patched,Map<String,MutableClass> applied)throws Exception{
        int preserved=0,added=0,runtimeCalls=0;
        for(String type:original.keySet()){
            check(patched.containsKey(type),"input class lost "+type);
            ClassDef expected=applied.containsKey(type)?applied.get(type):original.get(type);
            check(Arrays.equals(canonical(expected),canonical(patched.get(type))),"final APK class differs from reviewed transform "+type);
            if(!applied.containsKey(type))preserved++;
        }
        for(ClassDef c:patched.values()){
            if(!original.containsKey(c.getType())){check(c.getType().startsWith("Lapp/hiro/quicksearch/runtime/SearchTask"),"unexpected added app class "+c.getType());added++;}
            for(Method m:c.getMethods())for(Instruction i:instructions(m)){MethodReference call=call(i);
                if(call!=null&&RUNTIME.equals(call.getDefiningClass())&&!c.getType().startsWith("Lapp/hiro/quicksearch/runtime/")){
                    check(CHANGED.containsKey(c.getType())&&(CHANGED.get(c.getType()).contains(key(m))
                            ||ADDED.getOrDefault(c.getType(),Collections.<String>emptySet()).contains(key(m))),"runtime reference escaped hook inventory");runtimeCalls++;
                }}
        }
        check(added>0,"runtime missing");
        System.out.println("PASS final APK: all "+preserved+" non-target classes unchanged; "+applied.size()+" reviewed classes match; runtime classes="+added+"; runtime hooks="+runtimeCalls);
    }

    public static void main(String[] args)throws Exception{
        if(args.length==4&&"merge".equals(args[0])){merge(args[1],args[2],args[3]);return;}
        if((args.length!=2||!"original".equals(args[0]))&&(args.length!=3||!"patched".equals(args[0])))
            throw new IllegalArgumentException("DexAudit merge BASEDEX NEWDEX OUTDEX | original BASEAPK | patched BASEAPK PATCHEDAPK");
        Map<String,ClassDef> original=classes(args[1]);QuickSearchRecentsPatch.validatePackage("jp.ddo.sugihiro.quicksearch","0.4.5",36L);
        QuickSearchRecentsPatch.validateAll(original);Map<String,MutableClass> applied=mutableTargets(original);
        int operations=QuickSearchRecentsPatch.applyAll(applied);check(operations==31,"expected 31 reviewed operations");
        for(String type:CHANGED.keySet())classAudit(original.get(type),applied.get(type));
        rejectionCases(original,applied);
        if(args.length==3)finalApkAudit(original,classes(args[2]),applied);
        System.out.println("PASS static QA: original classes="+original.size()+"; operations="+operations+"; assertions="+assertions+"; full original-method restoration, new Back/destroy/cleanup bridges, atomic rejection. Android device behavior NOT tested.");
    }
}
