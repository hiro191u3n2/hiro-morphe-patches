package app.hiro.oneback.patches;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.ResourcePatch;
import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Public Android lifecycle/back hooks plus verified Instagram and X boundaries. */
public final class OneBackPatch {
    private OneBackPatch() {}
    public static final String NAME = "戻る1回で終了・履歴削除";
    public static final String RUNTIME = "Lapp/hiro/oneback/runtime/OneBackExit;";
    public static final String ACTIVITY = "Landroid/app/Activity;";
    public static final String DIALOG = "Landroid/app/Dialog;";
    public static final String VIEW = "Landroid/view/View;";
    public static final String KEY = "Landroid/view/KeyEvent;";
    public static final String HELPER_PREFIX = "hiroOneBack$";
    public static final Set<String> PACKAGES = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
        "com.ss.android.ugc.trill", "com.zhiliaoapp.musically", "com.instagram.android", "com.twitter.android", "ctrip.english")));
    private static final String NS = "http://schemas.android.com/apk/res/android";
    private static final List<String> NONE = Collections.emptyList();
    private static final List<String> KEY_ARGS = Collections.singletonList(KEY);
    private static final List<String> CREATE_ARGS = Collections.singletonList("Landroid/os/Bundle;");
    private static final List<String> PREIME_ARGS = Arrays.asList("I", KEY);
    private static final Set<String> PLATFORM_ACTIVITIES = set(ACTIVITY, "Landroid/app/ListActivity;", "Landroid/app/NativeActivity;",
        "Landroid/app/ActivityGroup;", "Landroid/app/TabActivity;", "Landroid/preference/PreferenceActivity;",
        "Landroid/accounts/AccountAuthenticatorActivity;", "Landroid/app/AliasActivity;");
    private static final Set<String> PLATFORM_DIALOGS = set(DIALOG, "Landroid/app/AlertDialog;", "Landroid/app/ProgressDialog;",
        "Landroid/app/DatePickerDialog;", "Landroid/app/TimePickerDialog;", "Landroid/app/Presentation;");
    private static final Set<String> PLATFORM_TEXT = set("Landroid/widget/TextView;", "Landroid/widget/EditText;",
        "Landroid/widget/AutoCompleteTextView;", "Landroid/widget/MultiAutoCompleteTextView;", "Landroid/widget/Button;",
        "Landroid/widget/CheckedTextView;", "Landroid/widget/CompoundButton;", "Landroid/widget/ToggleButton;",
        "Landroid/widget/CheckBox;", "Landroid/widget/RadioButton;", "Landroid/widget/Switch;");

    private static Set<String> set(String... names) { return new HashSet<String>(Arrays.asList(names)); }
    private static Set<Pair<String, Set<String>>> compatible() {
        Set<Pair<String, Set<String>>> result = new LinkedHashSet<Pair<String, Set<String>>>();
        Set<String> tiktokVersions=Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("46.2.3","47.4.3")));
        for (String name : PACKAGES) result.add(new Pair<String, Set<String>>(name,
            "com.zhiliaoapp.musically".equals(name)?tiktokVersions:
            "com.ss.android.ugc.trill".equals(name)?Collections.singleton("46.2.3"):null));
        return result;
    }
    public static void validatePackage(String name) {
        if (!PACKAGES.contains(name)) throw new IllegalStateException("OneBack is restricted to TikTok, Instagram, X and Trip.com: " + name);
    }

    /** Runs in the resource dependency BEFORE the BytecodePatch merges its extension.
     * Metadata force mode must not turn a removed TikTok version into a partial patch. */
    public static void validateTarget(String packageName,String versionName) {
        validatePackage(packageName);
        if("com.zhiliaoapp.musically".equals(packageName)) {
            if(!"46.2.3".equals(versionName)&&!"47.4.3".equals(versionName))
                throw new IllegalStateException("TikTok OneBack: unsupported TikTok "+versionName+"; use original 47.4.3 (legacy 46.2.3 retained). 47.5.3 support has been removed.");
        } else if("com.ss.android.ugc.trill".equals(packageName)&&!"46.2.3".equals(versionName)) {
            throw new IllegalStateException("TikTok OneBack: com.ss.android.ugc.trill is verified only for legacy 46.2.3; use com.zhiliaoapp.musically 47.4.3.");
        }
    }

    /** Enable the public back dispatcher without changing targetSdk, flags or permissions. */
    public static int patchManifest(Document document, String packageName) {
        validatePackage(packageName);
        Element root = document.getDocumentElement();
        if (!"manifest".equals(root.getTagName()) || !packageName.equals(root.getAttribute("package")))
            throw new IllegalStateException("Manifest package does not match selected application");
        NodeList apps = root.getElementsByTagName("application");
        if (apps.getLength() != 1) throw new IllegalStateException("Expected one manifest application");
        Element app = (Element) apps.item(0);
        enableBack(app);
        int count = 1;
        NodeList activities = app.getElementsByTagName("activity");
        for (int i=0; i<activities.getLength(); i++) {
            enableBack((Element)activities.item(i));
            count++;
        }
        return count;
    }

    private static void enableBack(Element element) {
        // Morphe's Document parser is namespace-unaware. Remove both representations
        // before inserting, or an existing android:...="false" can survive serialization.
        element.removeAttributeNS(NS, "enableOnBackInvokedCallback");
        element.removeAttribute("android:enableOnBackInvokedCallback");
        element.setAttributeNS(NS, "android:enableOnBackInvokedCallback", "true");
    }

    private static final ResourcePatch MANIFEST = new ResourcePatch(null, null, false, compatible(),
        Collections.emptySet(), Collections.emptySet(), new Function1<ResourcePatchContext, Unit>() {
            @Override public Unit invoke(ResourcePatchContext context) {
                validateTarget(context.getPackageMetadata().getPackageName(),context.getPackageMetadata().getVersionName());
                try (app.morphe.patcher.util.Document document = context.document("AndroidManifest.xml")) {
                    patchManifest(document, context.getPackageMetadata().getPackageName());
                } catch (RuntimeException failure) { throw new IllegalStateException("Could not save OneBack manifest", failure); }
                return Unit.INSTANCE;
            }
        }, null);

    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "Instagramはプロフィールから開いた動画では戻ると元のプロフィールへ戻る。ホーム・リールタブで戻ると終了し、最近使ったアプリから削除。コメント欄などが開いている時はその画面だけ閉じる。TikTok 47.4.3はアプリ本来の戻る処理でコメント欄などを閉じ、ホームで戻る1回で終了・履歴削除。プロフィール・メッセージからはホームへ戻り、その操作では終了しない。46.2.3の従来経路も保持。Xは各タブから終了時に履歴を削除し、次の通常起動はホームへ。Trip.comの動作を保持。",
        true, compatible(), Collections.singleton(MANIFEST), Collections.emptySet(), new Supplier<InputStream>() {
            @Override public InputStream get() {
                InputStream stream = OneBackPatch.class.getResourceAsStream("/extensions/oneback_exit.mpe");
                if (stream == null) throw new IllegalStateException("Missing OneBack runtime extension");
                return stream;
            }
        }, new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(final BytecodePatchContext context) {
                final String packageName = context.getPackageMetadata().getPackageName();
                validateTarget(packageName,context.getPackageMetadata().getVersionName());
                final Map<String, ClassDef> original = new LinkedHashMap<String, ClassDef>();
                context.classDefForEach(new Function1<ClassDef, Unit>() {
                    @Override public Unit invoke(ClassDef value) { original.put(value.getType(), value); return Unit.INSTANCE; }
                });
                final String tiktokContract=TikTokNativeContract.verifyIfTikTok(packageName,
                    context.getPackageMetadata().getVersionName(),original);
                if("4743".equals(tiktokContract))TikTokNativeBoundaries.verifyOriginal(original);
                Editor editor = new Editor() {
                    @Override public MutableClass mutable(String type) { return context.mutableClassDefBy(type); }
                };
                Map<String,Integer> counts;
                String previous=InstagramUpgrade.applyVersionIfKnown(context.getPackageMetadata().getPackageName(), original, editor);
                if (previous!=null&&"com.twitter.android".equals(context.getPackageMetadata().getPackageName())) {
                    counts = new LinkedHashMap<String,Integer>();
                    applyTwitterBackScope(original,editor,counts);
                    if("1.1.14".equals(previous)) {
                        verifyTwitterPatchedRootBack(original);
                        counts.put("twitter_native_root_back_preserved",1);
                    } else applyTwitterRootBack(original,editor,counts);
                    if(!counts.containsKey("twitter_native_root_back")&&!counts.containsKey("twitter_native_root_back_preserved"))
                        throw new IllegalStateException("Known X runtime upgrade requires the verified native root exit boundary");
                    counts.put("twitter_exact_runtime_upgrade",1);
                } else if ("1.1.17".equals(previous)&&Instagram452Patch.supports(original)) {
                    // Runtime identity and every distributed 452 native gate
                    // were checked before replacement. Never insert them twice.
                    Instagram452Upgrade.verify(original);
                    counts = new LinkedHashMap<String,Integer>();
                    counts.put("instagram_androidx_preserved",5);
                    counts.put("instagram_native_tab_back_preserved",1);
                    counts.put("instagram_452_native_boundaries_preserved",7);
                    counts.put("instagram_exact_runtime_upgrade",1);
                } else if (previous!=null) {
                    counts = new LinkedHashMap<String,Integer>();
                    boolean tabAlreadyPatched="1.1.13".equals(previous)||"1.1.14".equals(previous)||"1.1.15".equals(previous)||"1.1.17".equals(previous);
                    if("1.1.12".equals(previous)||tabAlreadyPatched) {
                        verifyInstagramDispatcher(original);
                        counts.put("instagram_androidx_preserved",5);
                    } else applyInstagramDispatcher(original, editor, counts);
                    if (!counts.containsKey("instagram_androidx_commit")&&!counts.containsKey("instagram_androidx_preserved"))
                        throw new IllegalStateException("Known runtime upgrade requires the verified Instagram AndroidX entry");
                    if(tabAlreadyPatched) {
                        verifyInstagramPatchedTabBack(original);
                        counts.put("instagram_native_tab_back_preserved",1);
                    } else applyInstagramTabBack(original,editor,counts);
                    if(!counts.containsKey("instagram_native_tab_back")&&!counts.containsKey("instagram_native_tab_back_preserved"))
                        throw new IllegalStateException("Known runtime upgrade requires the verified Instagram tab-history exit boundary");
                    counts.put("instagram_exact_runtime_upgrade", 1);
                } else counts = applyAll(packageName, original, editor);
                if("4743".equals(tiktokContract))TikTokNativeBoundaries.apply(original,editor,counts);
                if(tiktokContract!=null)counts.put("tiktok_verified_native_contract_"+tiktokContract,1);
                System.out.println("TikTok native OneBack hooks: " + counts);
                return Unit.INSTANCE;
            }
        }, null);

    public interface Editor { MutableClass mutable(String type); }

    private static boolean subtype(String type, Set<String> platforms, Map<String, ClassDef> classes) {
        Set<String> seen = new HashSet<String>();
        while (type != null && seen.add(type)) {
            if (platforms.contains(type)) return true;
            ClassDef value = classes.get(type);
            if (value == null) return false;
            type = value.getSuperclass();
        }
        return false;
    }
    private static boolean view(String type, Map<String, ClassDef> classes) {
        Set<String> seen = new HashSet<String>();
        while (type != null && seen.add(type)) {
            if (VIEW.equals(type) || PLATFORM_TEXT.contains(type) || (type.startsWith("Landroid/widget/") && type.endsWith("Layout;"))) return true;
            if ("Landroid/view/ViewGroup;".equals(type) || "Landroid/webkit/WebView;".equals(type)) return true;
            ClassDef value = classes.get(type); if (value == null) return false; type = value.getSuperclass();
        }
        return false;
    }
    private static boolean signature(MethodReference m, String name, List<String> params, String result) {
        if (!name.equals(m.getName()) || !result.equals(m.getReturnType()) || m.getParameterTypes().size()!=params.size()) return false;
        for (int i=0;i<params.size();i++) if (!params.get(i).contentEquals(m.getParameterTypes().get(i))) return false;
        return true;
    }
    private static Method find(ClassDef clazz, String name, List<String> params, String result) {
        for (Method method : clazz.getMethods()) if (signature(method,name,params,result)) return method;
        return null;
    }
    private static boolean instance(Method method) { return (method.getAccessFlags() & (0x8|0x2))==0; }
    private static MethodReference reference(Instruction instruction) {
        if (!(instruction instanceof ReferenceInstruction)) return null;
        Reference ref = ((ReferenceInstruction)instruction).getReference();
        return ref instanceof MethodReference ? (MethodReference)ref : null;
    }
    private static int self(Method method) {
        int words = 1;
        for (CharSequence param : method.getParameterTypes()) words += "J".contentEquals(param)||"D".contentEquals(param) ? 2 : 1;
        return method.getImplementation().getRegisterCount()-words;
    }
    private static BuilderInstruction call(Opcode normal, MethodReference target, int... registers) {
        boolean small=registers.length<=5;
        for (int r:registers) { if(r<0||r>65535)throw new IllegalStateException("Invalid argument register"); if(r>15)small=false; }
        if (small) {
            int[] args=new int[5];System.arraycopy(registers,0,args,0,registers.length);
            return new BuilderInstruction35c(normal,registers.length,args[0],args[1],args[2],args[3],args[4],target);
        }
        for (int i=1;i<registers.length;i++) if(registers[i]!=registers[0]+i)throw new IllegalStateException("Non-contiguous wide call");
        Opcode range=normal==Opcode.INVOKE_STATIC?Opcode.INVOKE_STATIC_RANGE:normal==Opcode.INVOKE_SUPER?Opcode.INVOKE_SUPER_RANGE:Opcode.INVOKE_DIRECT_RANGE;
        return new BuilderInstruction3rc(range,registers[0],registers.length,target);
    }
    private static BuilderInstruction runtime(String name,List<String> params,String result,int...registers) {
        return call(Opcode.INVOKE_STATIC,new ImmutableMethodReference(RUNTIME,name,params,result),registers);
    }
    private static MutableMethod newMethod(String owner,String name,List<String> params,String result,int flags,MethodImplementation code) {
        List<ImmutableMethodParameter> arguments = new ArrayList<ImmutableMethodParameter>();
        for(String p:params)arguments.add(new ImmutableMethodParameter(p,Collections.emptySet(),null));
        return new MutableMethod(new ImmutableMethod(owner,name,arguments,result,flags,Collections.emptySet(),Collections.emptySet(),code));
    }
    private static void bump(Map<String,Integer> counts,String key) { counts.put(key,counts.containsKey(key)?counts.get(key)+1:1); }

    /** One entry call preserves all registers, branches, try ranges and p0 reuse in original body. */
    private static void lifecycle(MutableClass clazz,String name,List<String> params,String hook,boolean boundary,Map<String,Integer> counts) {
        Method found=find(clazz,name,params,"V");
        if(found!=null) {
            if(!instance(found)||found.getImplementation()==null)return;
            MutableMethod m=(MutableMethod)found;
            m.getImplementation().addInstruction(0,runtime(hook,Collections.singletonList(ACTIVITY),"V",self(m)));
            bump(counts,"activity_lifecycle_entry");
        } else if(boundary) {
            int registers=1+params.size(); MutableMethodImplementation code=new MutableMethodImplementation(registers);
            code.addInstruction(runtime(hook,Collections.singletonList(ACTIVITY),"V",0));
            int[] args=new int[registers];for(int i=0;i<registers;i++)args[i]=i;
            code.addInstruction(call(Opcode.INVOKE_SUPER,new ImmutableMethodReference(clazz.getSuperclass(),name,params,"V"),args));
            code.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
            clazz.getMethods().add(newMethod(clazz.getType(),name,params,"V","onUserInteraction".equals(name)?1:4,code));
            bump(counts,"activity_lifecycle_added");
        }
    }

    /** Preserve the incoming launch/deep-link Intent before native onNewIntent changes it. */
    private static void newIntent(MutableClass clazz,boolean boundary,Map<String,Integer> counts) {
        List<String> params=Collections.singletonList("Landroid/content/Intent;");
        Method found=find(clazz,"onNewIntent",params,"V");
        if(found!=null) {
            if(!instance(found)||found.getImplementation()==null)return;
            MutableMethod m=(MutableMethod)found;int receiver=self(m);
            m.getImplementation().addInstruction(0,runtime("onNewIntent",Arrays.asList(ACTIVITY,params.get(0)),"V",receiver,receiver+1));
            bump(counts,"new_intent_entry");
        } else if(boundary) {
            MutableMethodImplementation code=new MutableMethodImplementation(2);
            code.addInstruction(runtime("onNewIntent",Arrays.asList(ACTIVITY,params.get(0)),"V",0,1));
            code.addInstruction(call(Opcode.INVOKE_SUPER,new ImmutableMethodReference(clazz.getSuperclass(),"onNewIntent",params,"V"),0,1));
            code.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
            clazz.getMethods().add(newMethod(clazz.getType(),"onNewIntent",params,"V",4,code));
            bump(counts,"new_intent_added");
        }
    }

    /** Original methods live unchanged in private helpers; native fallback has exception-safe reentry suppression. */
    private static void wrap(MutableClass clazz,String name,List<String> params,String result,
            String ownerType,String hook,boolean boundary,Map<String,Integer> counts) {
        Method old=find(clazz,name,params,result);
        if(old==null&&!boundary)return;
        if(old!=null&&(!instance(old)||old.getImplementation()==null))return;
        String helper=HELPER_PREFIX+name;
        if(find(clazz,helper,params,result)!=null)throw new IllegalStateException("OneBack already applied or helper collision: "+clazz.getType());
        int flags=old==null?1:old.getAccessFlags();
        if(old!=null) {
            MutableMethod moved=new MutableMethod(old); moved.setName(helper);
            moved.setAccessFlags((flags&~(1|4|16|0x400))|2|0x1000);
            clazz.getMethods().remove(old);clazz.getMethods().add(moved);
        }
        boolean bool="Z".equals(result); int words=1+params.size();
        MutableMethodImplementation code=new MutableMethodImplementation(words+2);
        if(bool)code.addInstruction(runtime(hook,Arrays.asList(ownerType,KEY),"Z",2,words+1));
        else code.addInstruction(runtime(hook,Collections.singletonList(ownerType),"Z",2));
        code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        code.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        code.addInstruction(bool?new BuilderInstruction11x(Opcode.RETURN,0):new BuilderInstruction10x(Opcode.RETURN_VOID));
        int nativeIndex=code.getInstructions().size();
        if (ACTIVITY.equals(ownerType)) {
            if (bool) code.addInstruction(runtime("enterKey",Arrays.asList(ACTIVITY,KEY),"V",2,words+1));
            else code.addInstruction(runtime("Lcom/twitter/app/common/inject/p;".equals(clazz.getType())&&"onBackPressed".equals(name)?"enterTwitterBack":"enterBack",Collections.singletonList(ACTIVITY),"V",2));
        } else code.addInstruction(runtime("enterNative",NONE,"V"));
        int tryIndex=code.getInstructions().size();
        int[] args=new int[words];for(int i=0;i<words;i++)args[i]=i+2;
        code.addInstruction(call(old==null?Opcode.INVOKE_SUPER:Opcode.INVOKE_DIRECT,
            new ImmutableMethodReference(old==null?clazz.getSuperclass():clazz.getType(),old==null?name:helper,params,result),args));
        if(bool)code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        int endIndex=code.getInstructions().size();
        code.addInstruction(runtime("afterNative",NONE,"V"));
        code.addInstruction(runtime("leaveNative",NONE,"V"));
        code.addInstruction(bool?new BuilderInstruction11x(Opcode.RETURN,0):new BuilderInstruction10x(Opcode.RETURN_VOID));
        int handlerIndex=code.getInstructions().size();
        code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_EXCEPTION,1));
        code.addInstruction(runtime("leaveNative",NONE,"V"));
        code.addInstruction(new BuilderInstruction11x(Opcode.THROW,1));
        code.replaceInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,code.newLabelForIndex(nativeIndex)));
        code.addCatch(code.newLabelForIndex(tryIndex),code.newLabelForIndex(endIndex),code.newLabelForIndex(handlerIndex));
        MutableMethod wrapper=old==null?newMethod(clazz.getType(),name,params,result,flags,code):
            new MutableMethod(new ImmutableMethod(clazz.getType(),name,old.getParameters(),result,flags,old.getAnnotations(),old.getHiddenApiRestrictions(),code));
        clazz.getMethods().add(wrapper);
        bump(counts,(bool?"key_guard_":"back_")+(old==null?"added":"wrapped"));
    }
    private static void back(MutableClass clazz,String ownerType,String hook,boolean boundary,Map<String,Integer> counts) {
        wrap(clazz,"onBackPressed",NONE,"V",ownerType,ACTIVITY.equals(ownerType)?"tryBack":"tryDialogBack",boundary,counts);
    }
    private static void guard(MutableClass clazz,String name,List<String> params,String ownerType,String hook,boolean boundary,Map<String,Integer> counts) {
        wrap(clazz,name,params,"Z",ownerType,hook,boundary,counts);
    }
    private static final String DISPATCHER="Landroid/window/OnBackInvokedDispatcher;";
    private static final String CALLBACK="Landroid/window/OnBackInvokedCallback;";
    private static String systemBackHook(Instruction instruction) {
        if(!invoke(instruction))return null;MethodReference r=reference(instruction);
        if(r==null||!DISPATCHER.equals(r.getDefiningClass()))return null;
        if(signature(r,"registerOnBackInvokedCallback",Arrays.asList("I",CALLBACK),"V"))return "register";
        if(signature(r,"unregisterOnBackInvokedCallback",Collections.singletonList(CALLBACK),"V"))return "unregister";
        return null;
    }
    private static boolean hasSystemBack(ClassDef clazz) {
        for(Method m:clazz.getMethods())if(m.getImplementation()!=null)
            for(Instruction i:m.getImplementation().getInstructions())if(systemBackHook(i)!=null)return true;
        return false;
    }
    private static void systemBackCalls(MutableClass clazz,Map<String,Integer> counts) {
        for(MutableMethod m:clazz.getMethods()) {
            if(m.getImplementation()==null)continue;
            List<BuilderInstruction> list=m.getImplementation().getInstructions();
            for(int index=0;index<list.size();index++) {
                Instruction instruction=list.get(index);String hook=systemBackHook(instruction);if(hook==null)continue;
                int[] args;
                if(instruction instanceof FiveRegisterInstruction) {
                    FiveRegisterInstruction r=(FiveRegisterInstruction)instruction;
                    args=Arrays.copyOf(new int[]{r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()},r.getRegisterCount());
                } else {
                    RegisterRangeInstruction r=(RegisterRangeInstruction)instruction;
                    args=new int[r.getRegisterCount()];for(int k=0;k<args.length;k++)args[k]=r.getStartRegister()+k;
                }
                List<String> params="register".equals(hook)?Arrays.asList(DISPATCHER,"I",CALLBACK):Arrays.asList(DISPATCHER,CALLBACK);
                m.getImplementation().replaceInstruction(index,call(Opcode.INVOKE_STATIC,
                    new ImmutableMethodReference("Lapp/hiro/oneback/runtime/NativeBack;",hook,params,"V"),args));
                bump(counts,"native_callback_"+hook);
            }
        }
    }

    private static int receiver(Instruction instruction) {
        if(instruction instanceof FiveRegisterInstruction)return ((FiveRegisterInstruction)instruction).getRegisterC();
        if(instruction instanceof RegisterRangeInstruction)return ((RegisterRangeInstruction)instruction).getStartRegister();
        throw new IllegalStateException("Unsupported dialog invoke encoding");
    }
    private static boolean invoke(Instruction i) {
        Opcode op=i.getOpcode();
        return op==Opcode.INVOKE_VIRTUAL||op==Opcode.INVOKE_VIRTUAL_RANGE||op==Opcode.INVOKE_SUPER||op==Opcode.INVOKE_SUPER_RANGE||op==Opcode.INVOKE_INTERFACE||op==Opcode.INVOKE_INTERFACE_RANGE;
    }
    private static boolean showVoid(Instruction i,Map<String,ClassDef> classes) {
        MethodReference ref=reference(i);
        return invoke(i)&&ref!=null&&signature(ref,"show",NONE,"V")&&subtype(ref.getDefiningClass(),PLATFORM_DIALOGS,classes);
    }
    private static boolean showValue(Instruction i,Map<String,ClassDef> classes) {
        MethodReference ref=reference(i);
        return invoke(i)&&ref!=null&&"show".equals(ref.getName())&&ref.getParameterTypes().isEmpty()&&subtype(ref.getReturnType(),PLATFORM_DIALOGS,classes);
    }
    private static boolean hasDialogShow(ClassDef clazz,Map<String,ClassDef> classes) {
        for(Method m:clazz.getMethods())if(m.getImplementation()!=null)
            for(Instruction i:m.getImplementation().getInstructions())if(showVoid(i,classes)||showValue(i,classes))return true;
        return false;
    }
    private static void dialogShows(MutableClass clazz,Map<String,ClassDef> classes,Map<String,Integer> counts) {
        for(MutableMethod m:clazz.getMethods()) {
            if(m.getImplementation()==null)continue;
            MutableMethodImplementation code=m.getImplementation();List<BuilderInstruction> list=code.getInstructions();
            for(int i=list.size()-1;i>=0;i--) {
                Instruction instruction=list.get(i);
                if(showVoid(instruction,classes)) {
                    code.addInstruction(i+1,runtime("dialogShown",Collections.singletonList(DIALOG),"V",receiver(instruction)));
                    bump(counts,"dialog_show_hooks");
                } else if(showValue(instruction,classes)&&i+1<list.size()&&list.get(i+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT) {
                    int register=((OneRegisterInstruction)list.get(i+1)).getRegisterA();
                    code.addInstruction(i+2,runtime("dialogShown",Collections.singletonList(DIALOG),"V",register));
                    bump(counts,"dialog_builder_hooks");
                }
            }
        }
    }

    /** Only terminal public Activity calls are bridged. Original opcode/dispatch is retained
     * in the fallback; invoke-super is never converted into virtual dispatch. */
    private static int terminalKind(Instruction instruction,Map<String,ClassDef> classes) {
        if(!invoke(instruction))return -1;
        MethodReference ref=reference(instruction);if(ref==null)return -1;
        if((instruction.getOpcode()==Opcode.INVOKE_SUPER||instruction.getOpcode()==Opcode.INVOKE_SUPER_RANGE)
                && PLATFORM_ACTIVITIES.contains(ref.getDefiningClass()) && signature(ref,"onBackPressed",NONE,"V"))return 0;
        if("Landroid/app/ActivityManager$AppTask;".equals(ref.getDefiningClass())&&signature(ref,"finishAndRemoveTask",NONE,"V"))return 5;
        if(!subtype(ref.getDefiningClass(),PLATFORM_ACTIVITIES,classes))return -1;
        if(signature(ref,"moveTaskToBack",Collections.singletonList("Z"),"Z"))return 2;
        if(signature(ref,"finishAndRemoveTask",NONE,"V"))return 3;
        if(signature(ref,"finishAffinity",NONE,"V"))return 4;
        if(signature(ref,"finish",NONE,"V")||signature(ref,"finishAfterTransition",NONE,"V"))return 1;
        return -1;
    }
    private static boolean hasTerminalCalls(ClassDef clazz,Map<String,ClassDef> classes) {
        for(Method m:clazz.getMethods())if(m.getImplementation()!=null)
            for(Instruction i:m.getImplementation().getInstructions())if(terminalKind(i,classes)>=0)return true;
        return false;
    }
    private static int[] invokeArgs(Instruction instruction) {
        if(instruction instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction r=(FiveRegisterInstruction)instruction;
            return Arrays.copyOf(new int[]{r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()},r.getRegisterCount());
        }
        RegisterRangeInstruction r=(RegisterRangeInstruction)instruction;int[] args=new int[r.getRegisterCount()];
        for(int i=0;i<args.length;i++)args[i]=r.getStartRegister()+i;return args;
    }
    private static Opcode compactInvoke(Opcode op) {
        if(op==Opcode.INVOKE_SUPER_RANGE)return Opcode.INVOKE_SUPER;
        if(op==Opcode.INVOKE_VIRTUAL_RANGE)return Opcode.INVOKE_VIRTUAL;
        if(op==Opcode.INVOKE_INTERFACE_RANGE)return Opcode.INVOKE_INTERFACE;
        return op;
    }
    private static void terminalCalls(MutableClass clazz,Map<String,ClassDef> classes,Map<String,Integer> counts) {
        Map<String,MethodReference> bridges=new LinkedHashMap<String,MethodReference>();
        List<MutableMethod> additions=new ArrayList<MutableMethod>();
        for(MutableMethod method:new ArrayList<MutableMethod>(clazz.getMethods())) {
            if(method.getImplementation()==null)continue;
            List<BuilderInstruction> code=method.getImplementation().getInstructions();
            for(int index=0;index<code.size();index++) {
                Instruction instruction=code.get(index);int kind=terminalKind(instruction,classes);if(kind<0)continue;
                MethodReference original=reference(instruction);Opcode originalOp=compactInvoke(instruction.getOpcode());
                boolean instanceBridge=originalOp==Opcode.INVOKE_SUPER;
                String identity=originalOp.name()+":"+original.toString();
                MethodReference target=bridges.get(identity);
                if(target==null) {
                    String name=HELPER_PREFIX+"terminal$"+bridges.size();
                    List<String> params=new ArrayList<String>();
                    if(!instanceBridge)params.add(original.getDefiningClass());
                    for(CharSequence p:original.getParameterTypes())params.add(p.toString());
                    int words=1+original.getParameterTypes().size();
                    MutableMethodImplementation body=new MutableMethodImplementation(words+3);
                    body.addInstruction(new BuilderInstruction11n(Opcode.CONST_4,1,kind));
                    body.addInstruction(new BuilderInstruction11n(Opcode.CONST_4,2,0));
                    body.addInstruction(kind==5?runtime("tryTaskTerminal",Collections.singletonList("Landroid/app/ActivityManager$AppTask;"),"Z",3)
                        :runtime("tryTerminalBack",Arrays.asList(ACTIVITY,"I","Z"),"Z",3,1,kind==2?4:2));
                    body.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
                    body.addInstruction(new BuilderInstruction10x(Opcode.NOP));
                    body.addInstruction(kind==2?new BuilderInstruction11x(Opcode.RETURN,0):new BuilderInstruction10x(Opcode.RETURN_VOID));
                    int nativeIndex=body.getInstructions().size();
                    // All bridge locals are low registers, even when the original site used /range.
                    body.addInstruction(new BuilderInstruction35c(originalOp,words,3,words==2?4:0,0,0,0,original));
                    if(kind==2)body.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
                    body.addInstruction(kind==2?new BuilderInstruction11x(Opcode.RETURN,0):new BuilderInstruction10x(Opcode.RETURN_VOID));
                    body.replaceInstruction(4,new BuilderInstruction21t(Opcode.IF_EQZ,0,body.newLabelForIndex(nativeIndex)));
                    additions.add(newMethod(clazz.getType(),name,params,original.getReturnType(),0x1002|(instanceBridge?0:8),body));
                    target=new ImmutableMethodReference(clazz.getType(),name,params,original.getReturnType());bridges.put(identity,target);
                    bump(counts,"terminal_bridges");
                }
                method.getImplementation().replaceInstruction(index,call(instanceBridge?Opcode.INVOKE_DIRECT:Opcode.INVOKE_STATIC,target,invokeArgs(instruction)));
                bump(counts,"terminal_calls");
            }
        }
        clazz.getMethods().addAll(additions);
    }

    private static final String CLIPS_CONFIG="Lcom/instagram/clips/intf/ClipsViewerConfig;";
    /** Semantic fingerprint plus exact guard dataflow, independently checked on v447.
     * No obfuscated class or member names are assumed. Returns the dead scratch register. */
    public static int instagramReelsBoundary(ClassDef clazz,Map<String,ClassDef> classes) {
        if("LX/02Hr;".equals(clazz.getType()))return Instagram452Boundaries.reels(clazz,classes);
        if(!subtype(clazz.getType(),set("Landroidx/fragment/app/Fragment;"),classes))return -1;
        Method m=find(clazz,"onBackPressed",NONE,"Z");
        if(m==null||!instance(m)||m.getImplementation()==null||find(clazz,"getModuleName",NONE,"Ljava/lang/String;")==null)return -1;
        List<Instruction> ins=new ArrayList<Instruction>();Set<String> strings=new HashSet<String>();
        for(Instruction i:m.getImplementation().getInstructions()) {ins.add(i);if(i instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)i).getReference();if(r instanceof StringReference)strings.add(((StringReference)r).getString());}}
        if(!strings.containsAll(Arrays.asList("clipsViewerViewPager","android_purge_26_q3_ClipsViewPagerImpl_scrollToTop","ClipsConstants.ARG_CLIPS_MEDIA_ID")))return -1;
        Opcode[] prefix={Opcode.IGET_OBJECT,Opcode.CONST_4,Opcode.IF_EQZ,Opcode.IGET_BOOLEAN,Opcode.IF_EQZ,Opcode.INVOKE_VIRTUAL,Opcode.RETURN,
            Opcode.IGET_OBJECT,Opcode.IF_EQZ,Opcode.MOVE_OBJECT,Opcode.CHECK_CAST,Opcode.IGET_BOOLEAN,Opcode.IF_EQZ,Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT,Opcode.IF_EQZ,Opcode.RETURN,
            Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT_OBJECT};
        if(ins.size()<prefix.length)throw new IllegalStateException("Reels Back guard fingerprint shortened");
        for(int i=0;i<prefix.length;i++)if(ins.get(i).getOpcode()!=prefix[i])throw new IllegalStateException("Reels Back guard fingerprint changed at "+i);
        int p0=self(m),layer=((TwoRegisterInstruction)ins.get(0)).getRegisterA(),scratch=((TwoRegisterInstruction)ins.get(3)).getRegisterA(),yes=((OneRegisterInstruction)ins.get(1)).getRegisterA();
        boolean valid=scratch<256&&p0!=scratch&&yes!=scratch&&layer!=scratch&&p0!=yes&&p0!=layer
            &&((TwoRegisterInstruction)ins.get(0)).getRegisterB()==p0
            &&((NarrowLiteralInstruction)ins.get(1)).getNarrowLiteral()==1
            &&((TwoRegisterInstruction)ins.get(3)).getRegisterB()==layer
            &&((TwoRegisterInstruction)ins.get(7)).getRegisterA()==layer&&((TwoRegisterInstruction)ins.get(7)).getRegisterB()==p0
            &&((TwoRegisterInstruction)ins.get(9)).getRegisterA()==scratch&&((TwoRegisterInstruction)ins.get(9)).getRegisterB()==layer
            &&((OneRegisterInstruction)ins.get(10)).getRegisterA()==scratch
            &&((TwoRegisterInstruction)ins.get(11)).getRegisterA()==scratch&&((TwoRegisterInstruction)ins.get(11)).getRegisterB()==scratch
            &&((OneRegisterInstruction)ins.get(14)).getRegisterA()==scratch&&((OneRegisterInstruction)ins.get(18)).getRegisterA()==scratch;
        for(int i:new int[]{6,16})valid&=((OneRegisterInstruction)ins.get(i)).getRegisterA()==yes;
        for(int i:new int[]{2,8})valid&=((OneRegisterInstruction)ins.get(i)).getRegisterA()==layer;
        for(int i:new int[]{4,12,15})valid&=((OneRegisterInstruction)ins.get(i)).getRegisterA()==scratch;
        int[] addresses=new int[ins.size()+1];for(int i=0;i<ins.size();i++)addresses[i+1]=addresses[i]+ins.get(i).getCodeUnits();
        for(int i:new int[]{2,4,8,12,15})valid&=addresses[i]+((OffsetInstruction)ins.get(i)).getCodeOffset()==addresses[i<7?7:17];
        for(int i:new int[]{5,13,17}){FiveRegisterInstruction call=(FiveRegisterInstruction)ins.get(i);MethodReference ref=reference(ins.get(i));valid&=call.getRegisterCount()==1&&call.getRegisterC()==(i==17?p0:layer)&&ref!=null&&ref.getParameterTypes().isEmpty()&&(i==17?CLIPS_CONFIG:i==13?"Z":"V").equals(ref.getReturnType());}
        if(!valid)throw new IllegalStateException("Reels Back guard dataflow changed; refusing unsafe insertion");
        return scratch;
    }
    public static void instagramReelsHook(MutableClass clazz,int scratch,Map<String,Integer> counts) {
        if("LX/02Hr;".equals(clazz.getType())){Instagram452Boundaries.hookReels(clazz,scratch,counts);return;}
        MutableMethod m=(MutableMethod)find(clazz,"onBackPressed",NONE,"Z");MutableMethodImplementation code=m.getImplementation();
        // v0 is dead here: the first original instruction writes its returned Config into it.
        // Keep native popup and nested-bottom-sheet handling before our fallback.
        code.addInstruction(17,runtime("tryInstagramReelsBack",Collections.singletonList("Ljava/lang/Object;"),"Z",self(m)));
        code.addInstruction(18,new BuilderInstruction11x(Opcode.MOVE_RESULT,scratch));
        code.addInstruction(19,new BuilderInstruction10x(Opcode.NOP));
        code.addInstruction(20,new BuilderInstruction11x(Opcode.RETURN,scratch));
        code.replaceInstruction(19,new BuilderInstruction21t(Opcode.IF_EQZ,scratch,code.newLabelForIndex(21)));
        // Existing labels still point at the old Config invocation. Route all three
        // "no native layer" branches through the new hook, not around it.
        for(int index:new int[]{8,12,15}){OneRegisterInstruction old=(OneRegisterInstruction)code.getInstructions().get(index);code.replaceInstruction(index,new BuilderInstruction21t(Opcode.IF_EQZ,old.getRegisterA(),code.newLabelForIndex(17)));}
        bump(counts,"instagram_reels_native_boundary");
    }

    private static final String INSTAGRAM_MAIN="Lcom/instagram/mainactivity/InstagramMainActivity;";
    private static final String INSTAGRAM_BASE="Lcom/instagram/base/activity/IgFragmentActivity;";
    private static final String CLIPS_TRACKER="Lcom/instagram/igsignalsproducts/clips/tab/IgSignalsClipsOpenTabTracker;";
    /** The main native dispatcher can hand Back to AndroidX before invoking the
     * Reels boolean handler. Keep its three native overlay guards first, then
     * evaluate the Reels exit before either native navigation branch is chosen. */
    public static final class InstagramMainBoundary {
        public final String methodName,parameterType;
        public final int instruction,scratch;
        public final int[] incomingBranches;
        private InstagramMainBoundary(Method method,int scratch,int[] incoming) {
            methodName=method.getName();parameterType=method.getParameterTypes().get(0).toString();
            instruction=62;this.scratch=scratch;incomingBranches=incoming.clone();
        }
    }
    private static Reference rawReference(Instruction i) {
        return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference():null;
    }
    private static FieldReference fieldReference(Instruction i) {
        Reference r=rawReference(i);return r instanceof FieldReference?(FieldReference)r:null;
    }
    private static boolean register(Instruction i,int a) {
        return i instanceof OneRegisterInstruction&&((OneRegisterInstruction)i).getRegisterA()==a;
    }
    private static boolean registers(Instruction i,int a,int b) {
        return i instanceof TwoRegisterInstruction&&((TwoRegisterInstruction)i).getRegisterA()==a&&((TwoRegisterInstruction)i).getRegisterB()==b;
    }
    private static boolean noArgCall(Instruction i,String owner,String result,int receiver) {
        MethodReference r=reference(i);
        return r!=null&&(owner==null||owner.equals(r.getDefiningClass()))&&r.getParameterTypes().isEmpty()
            &&result.equals(r.getReturnType())&&i instanceof FiveRegisterInstruction
            &&((FiveRegisterInstruction)i).getRegisterCount()==1&&((FiveRegisterInstruction)i).getRegisterC()==receiver;
    }
    public static InstagramMainBoundary instagramMainReelsBoundary(ClassDef clazz,Map<String,ClassDef> classes) {
        if(!INSTAGRAM_MAIN.equals(clazz.getType())||!subtype(clazz.getType(),PLATFORM_ACTIVITIES,classes))return null;
        InstagramMainBoundary found=null;
        for(Method m:clazz.getMethods()) {
            if(!instance(m)||!"V".equals(m.getReturnType())||m.getParameterTypes().size()!=1
                    ||!m.getParameterTypes().get(0).toString().startsWith("L")||m.getImplementation()==null)continue;
            List<Instruction> ins=new ArrayList<Instruction>();boolean tracker=false,newsfeed=false;
            for(Instruction i:m.getImplementation().getInstructions()) {
                ins.add(i);Reference r=rawReference(i);MethodReference method=reference(i);
                tracker|=method!=null&&CLIPS_TRACKER.equals(method.getReturnType());
                newsfeed|=r instanceof StringReference&&"com.instagram.newsfeed.fragment.NewsfeedFragment".equals(((StringReference)r).getString());
            }
            if(!tracker||!newsfeed)continue;
            Opcode[] prefix={Opcode.IGET_OBJECT,Opcode.CONST_4,Opcode.IF_EQZ,Opcode.IGET_OBJECT,Opcode.IF_EQZ,Opcode.SGET_OBJECT,
                Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT,Opcode.IF_NE,Opcode.INVOKE_STATIC,Opcode.MOVE_RESULT_OBJECT,Opcode.IF_EQZ,
                Opcode.IGET_OBJECT,Opcode.CONST_4,Opcode.CONST_16,Opcode.NEW_INSTANCE,Opcode.INVOKE_DIRECT,Opcode.SGET_OBJECT,Opcode.INVOKE_STATIC,
                Opcode.IGET_OBJECT,Opcode.IF_EQZ,Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT,Opcode.IF_NE,Opcode.RETURN_VOID,
                Opcode.IGET_OBJECT,Opcode.IF_EQZ,Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT,Opcode.IF_NE,Opcode.RETURN_VOID,
                Opcode.INVOKE_STATIC,Opcode.MOVE_RESULT_OBJECT,Opcode.CONST_WIDE,Opcode.CHECK_CAST,Opcode.INVOKE_INTERFACE,Opcode.MOVE_RESULT,Opcode.IF_EQZ,
                Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT_OBJECT,Opcode.IGET_OBJECT,Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT_OBJECT,Opcode.INVOKE_STATIC,
                Opcode.INVOKE_INTERFACE,Opcode.MOVE_RESULT_OBJECT,Opcode.INVOKE_INTERFACE,Opcode.MOVE_RESULT,Opcode.IF_EQZ,
                Opcode.INVOKE_INTERFACE,Opcode.MOVE_RESULT_OBJECT,Opcode.CHECK_CAST,Opcode.INSTANCE_OF,Opcode.IF_EQZ,Opcode.CHECK_CAST,
                Opcode.IGET_OBJECT,Opcode.IF_EQZ,Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT,Opcode.IF_NE,Opcode.INVOKE_VIRTUAL,Opcode.RETURN_VOID,
                Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT_OBJECT,Opcode.IGET_BOOLEAN,Opcode.IF_EQZ,Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT_OBJECT,
                Opcode.IF_EQZ,Opcode.INVOKE_STATIC,Opcode.MOVE_RESULT,Opcode.IF_NE,Opcode.INVOKE_VIRTUAL,Opcode.MOVE_RESULT_OBJECT,Opcode.INVOKE_VIRTUAL,Opcode.RETURN_VOID,
                Opcode.IGET_OBJECT};
            if(Instagram452Boundaries.supports(classes)) {
                Instagram452Boundaries.verifyMain(m);
                prefix[7]=Opcode.MOVE_RESULT_OBJECT;
                prefix[14]=Opcode.CONST_4;
            }
            if(ins.size()<prefix.length)throw new IllegalStateException("Instagram main Back guard fingerprint shortened");
            for(int i=0;i<prefix.length;i++)if(ins.get(i).getOpcode()!=prefix[i])throw new IllegalStateException("Instagram main Back guard fingerprint changed at "+i);
            int p0=self(m),scratch=((OneRegisterInstruction)ins.get(63)).getRegisterA(),yes=((OneRegisterInstruction)ins.get(1)).getRegisterA();
            int session=((TwoRegisterInstruction)ins.get(0)).getRegisterA(),iterator=((OneRegisterInstruction)ins.get(45)).getRegisterA(),element=((OneRegisterInstruction)ins.get(50)).getRegisterA();
            FieldReference first=fieldReference(ins.get(19)),second=fieldReference(ins.get(25)),dialog=fieldReference(ins.get(55)),enabled=fieldReference(ins.get(64));
            MethodReference dispatcher=reference(ins.get(62)),manager=reference(ins.get(38));
            Reference dialogType=rawReference(ins.get(54));String dialogOwner=dialogType instanceof TypeReference?((TypeReference)dialogType).getType():null;
            boolean valid=scratch<256&&scratch!=p0&&scratch!=yes&&scratch!=session&&scratch!=iterator&&scratch!=element
                &&p0!=yes&&p0!=session&&((NarrowLiteralInstruction)ins.get(1)).getNarrowLiteral()==1
                &&registers(ins.get(0),session,p0)&&register(ins.get(2),session)
                &&first!=null&&INSTAGRAM_MAIN.equals(first.getDefiningClass())&&registers(ins.get(19),scratch,p0)
                &&second!=null&&INSTAGRAM_BASE.equals(second.getDefiningClass())&&registers(ins.get(25),scratch,p0)
                &&noArgCall(ins.get(21),first.getType(),"Z",scratch)&&noArgCall(ins.get(27),second.getType(),"Z",scratch)
                &&dispatcher!=null&&"Landroidx/activity/ComponentActivity;".equals(dispatcher.getDefiningClass())
                &&dispatcher.getReturnType().startsWith("L")&&noArgCall(ins.get(62),dispatcher.getDefiningClass(),dispatcher.getReturnType(),p0)
                &&dispatcher.equals(reference(ins.get(72)))&&noArgCall(ins.get(72),dispatcher.getDefiningClass(),dispatcher.getReturnType(),p0)
                &&enabled!=null&&dispatcher.getReturnType().equals(enabled.getDefiningClass())&&"Z".equals(enabled.getType())
                &&noArgCall(ins.get(74),dispatcher.getReturnType(),"V",scratch)
                &&manager!=null&&"Landroidx/fragment/app/FragmentActivity;".equals(manager.getDefiningClass())
                &&noArgCall(ins.get(38),manager.getDefiningClass(),manager.getReturnType(),p0)
                &&noArgCall(ins.get(66),INSTAGRAM_MAIN,manager.getReturnType(),p0)
                &&dialog!=null&&DIALOG.equals(dialog.getType())&&dialogOwner!=null&&dialogOwner.equals(dialog.getDefiningClass())
                &&registers(ins.get(55),scratch,element)&&noArgCall(ins.get(57),DIALOG,"Z",scratch)
                &&"isShowing".equals(reference(ins.get(57)).getName())&&noArgCall(ins.get(60),dialogOwner,"V",element)
                &&noArgCall(ins.get(44),"Ljava/util/List;","Ljava/util/Iterator;",scratch)
                &&noArgCall(ins.get(46),"Ljava/util/Iterator;","Z",iterator)
                &&noArgCall(ins.get(49),"Ljava/util/Iterator;","Ljava/lang/Object;",iterator)
                &&m.getImplementation().getTryBlocks().isEmpty();
            for(int i:new int[]{20,22,26,28,36,37,39,42,47,48,53,56,58,63,65,67,68,70,73})valid&=register(ins.get(i),scratch);
            for(int i:new int[]{23,29,59,71})valid&=registers(ins.get(i),scratch,yes);
            valid&=registers(ins.get(40),scratch,scratch)&&registers(ins.get(52),scratch,element)&&registers(ins.get(64),scratch,scratch)
                &&register(ins.get(51),element)&&register(ins.get(54),element);
            int[] addresses=new int[ins.size()+1];for(int i=0;i<ins.size();i++)addresses[i+1]=addresses[i]+ins.get(i).getCodeUnits();
            int[][] edges={{2,19},{4,19},{8,19},{11,19},{20,25},{23,25},{26,31},{29,31},{37,62},{48,62},{53,46},{56,46},{59,46},{65,76},{68,76},{71,76}};
            for(int[] edge:edges)valid&=addresses[edge[0]]+((OffsetInstruction)ins.get(edge[0])).getCodeOffset()==addresses[edge[1]];
            List<Integer> incoming=new ArrayList<Integer>();
            for(int i=0;i<ins.size();i++) {
                Instruction op=ins.get(i);
                if(op instanceof OffsetInstruction&&addresses[i]+((OffsetInstruction)op).getCodeOffset()==addresses[62])incoming.add(i);
                // No switch or exception-handler entry may bypass this verified boundary.
                if(op instanceof SwitchPayload)valid=false;
            }
            valid&=incoming.equals(Arrays.asList(37,48));
            if(!valid)throw new IllegalStateException("Instagram main Back guard dataflow changed; refusing unsafe insertion");
            if(found!=null)throw new IllegalStateException("Ambiguous Instagram main Back dispatcher");
            found=new InstagramMainBoundary(m,scratch,new int[]{37,48});
        }
        return found;
    }
    public static void instagramMainReelsHook(MutableClass clazz,InstagramMainBoundary boundary,Map<String,Integer> counts) {
        MutableMethod m=(MutableMethod)find(clazz,boundary.methodName,Collections.singletonList(boundary.parameterType),"V");
        MutableMethodImplementation code=m.getImplementation();int at=boundary.instruction;
        // The original next call uses only p0 and replaces scratch with its result.
        code.addInstruction(at,runtime("tryInstagramMainReelsBack",Collections.singletonList(ACTIVITY),"Z",self(m)));
        code.addInstruction(at+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,boundary.scratch));
        code.addInstruction(at+2,new BuilderInstruction10x(Opcode.NOP));
        code.addInstruction(at+3,new BuilderInstruction10x(Opcode.RETURN_VOID));
        code.replaceInstruction(at+2,new BuilderInstruction21t(Opcode.IF_EQZ,boundary.scratch,code.newLabelForIndex(at+4)));
        // MutableMethodImplementation keeps existing labels on the old instruction.
        // Retarget every native edge entering the boundary to execute our guard.
        for(int index:boundary.incomingBranches) {
            OneRegisterInstruction branch=(OneRegisterInstruction)code.getInstructions().get(index);
            code.replaceInstruction(index,new BuilderInstruction21t(Opcode.IF_EQZ,branch.getRegisterA(),code.newLabelForIndex(at)));
        }
        bump(counts,"instagram_main_reels_dispatch");
    }

    private static final String IG_DISPATCHER="LX/01h;",IG_GESTURE="LX/0Ok;",COMPONENT_ACTIVITY="Landroidx/activity/ComponentActivity;";
    /** Complete v447 instruction fingerprints, derived from the supplied APK.
     * Include registers, references, literals and relative branch destinations;
     * debug positions are deliberately irrelevant. The new gates use only
     * locals that the exact native continuation assigns before reading. */
    public static String instagramDispatcherDigest(Method method) {
        if(method==null||method.getImplementation()==null)throw new IllegalStateException("Instagram dispatcher method missing");
        MethodImplementation body=method.getImplementation();StringBuilder s=new StringBuilder("registers="+body.getRegisterCount()+"\n");
        if(!body.getTryBlocks().isEmpty())throw new IllegalStateException("Instagram dispatcher try regions changed");
        for(Instruction i:body.getInstructions()) {
            s.append(i.getOpcode().name()).append(':').append(i.getCodeUnits());
            if(i instanceof OneRegisterInstruction)s.append(" a=").append(((OneRegisterInstruction)i).getRegisterA());
            if(i instanceof TwoRegisterInstruction)s.append(" b=").append(((TwoRegisterInstruction)i).getRegisterB());
            if(i instanceof ThreeRegisterInstruction)s.append(" c=").append(((ThreeRegisterInstruction)i).getRegisterC());
            if(i instanceof FiveRegisterInstruction) { FiveRegisterInstruction r=(FiveRegisterInstruction)i;
                s.append(" args=").append(r.getRegisterCount()).append(',').append(r.getRegisterC()).append(',').append(r.getRegisterD()).append(',').append(r.getRegisterE()).append(',').append(r.getRegisterF()).append(',').append(r.getRegisterG()); }
            if(i instanceof RegisterRangeInstruction) { RegisterRangeInstruction r=(RegisterRangeInstruction)i;s.append(" range=").append(r.getStartRegister()).append(',').append(r.getRegisterCount()); }
            if(i instanceof WideLiteralInstruction)s.append(" literal=").append(((WideLiteralInstruction)i).getWideLiteral());
            if(i instanceof OffsetInstruction)s.append(" offset=").append(((OffsetInstruction)i).getCodeOffset());
            if(i instanceof ReferenceInstruction)s.append(" ref=").append(((ReferenceInstruction)i).getReference());
            if(i instanceof DualReferenceInstruction)s.append(" ref2=").append(((DualReferenceInstruction)i).getReference2());
            if(i instanceof SwitchPayload||i instanceof com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload)
                throw new IllegalStateException("Instagram dispatcher payload changed");
            s.append('\n');
        }
        try {
            byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(s.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder();for(byte b:bytes)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();
        } catch(java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void dispatcherFingerprint(ClassDef clazz,String name,List<String> params,String result,String expected) {
        Method method=clazz==null?null:find(clazz,name,params,result);
        int flags="A00".equals(name)?25:"invoke".equals(name)?4177:17;
        if(method==null||method.getAccessFlags()!=flags||!expected.equals(instagramDispatcherDigest(method)))
            throw new IllegalStateException("Instagram v447 dispatcher fingerprint changed: "+(clazz==null?"missing class":clazz.getType())+"->"+name);
    }
    private static void dispatcherVoidGate(MutableMethod method,String hook,int receiver,int scratch) {
        MutableMethodImplementation code=method.getImplementation();
        code.addInstruction(0,runtime(hook,Collections.singletonList("Ljava/lang/Object;"),"Z",receiver));
        code.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT,scratch));
        code.addInstruction(2,new BuilderInstruction10x(Opcode.NOP));
        code.addInstruction(3,new BuilderInstruction10x(Opcode.RETURN_VOID));
        code.replaceInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,scratch,code.newLabelForIndex(4)));
    }
    /** Cover the shared AndroidX route BEFORE callback selection / direct pop.
     * CoN binds the exact owning Activity rather than guessing through an
     * obfuscated object graph or relying on platform-root registration order.
     * This method is also the only new bytecode insertion used by the narrowly
     * admitted already-patched APK upgrade path. */
    public static void applyInstagramDispatcher(Map<String,ClassDef> original,Editor editor,Map<String,Integer> counts) {
        if(Instagram452Patch.supports(original)){Instagram452Patch.applyDispatcher(original,editor,counts);return;}
        if(!original.containsKey(INSTAGRAM_MAIN)||!original.containsKey(IG_DISPATCHER))return;
        // Preflight ALL participating bodies before mutating any one of them.
        dispatcherFingerprint(original.get(COMPONENT_ACTIVITY),"CoN",NONE,IG_DISPATCHER,"731a76d39402668f9037f83b249a28c32a16fdfb0a5254945b132b65844095fa");
        dispatcherFingerprint(original.get(IG_DISPATCHER),"A04",NONE,"V","0d5cd7a38b8e94f4683e97563a277dcadb1ccc60b9118d58b63049564a230d57");
        dispatcherFingerprint(original.get(IG_DISPATCHER),"A00",Collections.singletonList(IG_DISPATCHER),"V","f46a16bcff5ec3bebabafc7113d9dc4ef4d9139b507363262cd0961b1c79558b");
        dispatcherFingerprint(original.get(IG_GESTURE),"invoke",Collections.singletonList("Ljava/lang/Object;"),"Ljava/lang/Object;","4e3930ae7d9224f27341caa413f4f9e5e179bcf24de74980a88ac611cd74618c");
        MutableMethod owner=(MutableMethod)find(editor.mutable(COMPONENT_ACTIVITY),"CoN",NONE,IG_DISPATCHER);
        owner.getImplementation().addInstruction(4,runtime("bindInstagramDispatcher",Arrays.asList(ACTIVITY,"Ljava/lang/Object;"),"V",1,0));
        bump(counts,"instagram_androidx_owner_binding");
        MutableClass dispatcher=editor.mutable(IG_DISPATCHER);
        dispatcherVoidGate((MutableMethod)find(dispatcher,"A04",NONE,"V"),"tryInstagramDispatcherBack",4,0);
        bump(counts,"instagram_androidx_commit");
        // A00 is static: its sole dispatcher argument occupies v4. Suppressed
        // starts must also suppress cancellation to avoid an unmatched native
        // cancel call against a callback that never received native start.
        dispatcherVoidGate((MutableMethod)find(dispatcher,"A00",Collections.singletonList(IG_DISPATCHER),"V"),"cancelInstagramDispatcherGesture",4,0);
        bump(counts,"instagram_androidx_cancel");
        MutableMethod gesture=(MutableMethod)find(editor.mutable(IG_GESTURE),"invoke",Collections.singletonList("Ljava/lang/Object;"),"Ljava/lang/Object;");
        MutableMethodImplementation code=gesture.getImplementation();
        Label unitReturn=code.newLabelForIndex(71);
        // Descending order preserves the original indexes. Capture the start
        // after any old gesture has been cancelled, but before storing/invoking
        // the selected callback. Its sole incoming native edge is redirected.
        code.addInstruction(52,runtime("progressInstagramDispatcherGesture",Collections.singletonList("Ljava/lang/Object;"),"Z",0));
        code.addInstruction(53,new BuilderInstruction11x(Opcode.MOVE_RESULT,1));
        code.addInstruction(54,new BuilderInstruction21t(Opcode.IF_NEZ,1,unitReturn));
        code.addInstruction(41,runtime("startInstagramDispatcherGesture",Collections.singletonList("Ljava/lang/Object;"),"Z",3));
        code.addInstruction(42,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        code.addInstruction(43,new BuilderInstruction21t(Opcode.IF_NEZ,0,unitReturn));
        code.replaceInstruction(39,new BuilderInstruction21t(Opcode.IF_EQZ,0,code.newLabelForIndex(41)));
        bump(counts,"instagram_androidx_start");bump(counts,"instagram_androidx_progress");
    }

    /** Validate, but never insert again, the five already distributed 1.1.12
     * AndroidX gates. A runtime match alone does not establish native hooks. */
    public static void verifyInstagramDispatcher(Map<String,ClassDef> original) {
        dispatcherFingerprint(original.get(COMPONENT_ACTIVITY),"CoN",NONE,IG_DISPATCHER,"2f50f8dad582827c19fc38efc021b1c44beaaa53ab098e46efcea907303d18f0");
        dispatcherFingerprint(original.get(IG_DISPATCHER),"A04",NONE,"V","78a64b108cbd09b324bac779320ba7e4dc599397b7988e20f93cbba3f8d53dca");
        dispatcherFingerprint(original.get(IG_DISPATCHER),"A00",Collections.singletonList(IG_DISPATCHER),"V","133634acc77b032019cf698f48328ea40cae379a6ee94a716ab9d4d456dee822");
        dispatcherFingerprint(original.get(IG_GESTURE),"invoke",Collections.singletonList("Ljava/lang/Object;"),"Ljava/lang/Object;","5b39e1bd9d88ebf2063f10518fbab85636402305f05af9a705ca81e651c97632");
    }

    private static final String IG_NAVIGATION="LX/2yE;",IG_NAVIGATION_WRAPPER="LX/2xZ;",IG_TAB="LX/2yl;";
    /** Complete native body fingerprint, with branch/try endpoints expressed as
     * instruction indexes so harmless jumbo-string and goto widths do not vary
     * with the patcher's DEX pool. No opcode, operand or handler is discarded. */
    public static String instagramNavigationDigest(Method method) {
        if(method==null||method.getImplementation()==null)throw new IllegalStateException("Instagram navigation method missing");
        MethodImplementation body=method.getImplementation();StringBuilder s=new StringBuilder("flags="+method.getAccessFlags()+" registers="+body.getRegisterCount()+"\n");
        List<Instruction> list=new ArrayList<Instruction>();Map<Integer,Integer> indexes=new HashMap<Integer,Integer>();int pc=0;
        for(Instruction i:body.getInstructions()){indexes.put(pc,list.size());list.add(i);pc+=i.getCodeUnits();}indexes.put(pc,list.size());pc=0;
        for(Instruction i:list) {
            Opcode op=i.getOpcode();s.append(op==Opcode.CONST_STRING_JUMBO?Opcode.CONST_STRING:op==Opcode.GOTO_16||op==Opcode.GOTO_32?Opcode.GOTO:op);
            if(i instanceof OneRegisterInstruction)s.append(" a=").append(((OneRegisterInstruction)i).getRegisterA());
            if(i instanceof TwoRegisterInstruction)s.append(" b=").append(((TwoRegisterInstruction)i).getRegisterB());
            if(i instanceof ThreeRegisterInstruction)s.append(" c=").append(((ThreeRegisterInstruction)i).getRegisterC());
            if(i instanceof FiveRegisterInstruction) {FiveRegisterInstruction r=(FiveRegisterInstruction)i;s.append(" args=").append(r.getRegisterCount()).append(',').append(r.getRegisterC()).append(',').append(r.getRegisterD()).append(',').append(r.getRegisterE()).append(',').append(r.getRegisterF()).append(',').append(r.getRegisterG());}
            if(i instanceof RegisterRangeInstruction) {RegisterRangeInstruction r=(RegisterRangeInstruction)i;s.append(" range=").append(r.getStartRegister()).append(',').append(r.getRegisterCount());}
            if(i instanceof WideLiteralInstruction)s.append(" literal=").append(((WideLiteralInstruction)i).getWideLiteral());
            if(i instanceof OffsetInstruction)s.append(" target=").append(navigationIndex(indexes,pc+((OffsetInstruction)i).getCodeOffset()));
            if(i instanceof ReferenceInstruction)s.append(" ref=").append(((ReferenceInstruction)i).getReference());
            if(i instanceof DualReferenceInstruction)s.append(" ref2=").append(((DualReferenceInstruction)i).getReference2());
            if(i instanceof SwitchPayload||i instanceof com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload)throw new IllegalStateException("Instagram navigation payload changed");
            s.append('\n');pc+=i.getCodeUnits();
        }
        for(TryBlock<? extends ExceptionHandler> t:body.getTryBlocks()) {
            s.append("try=").append(navigationIndex(indexes,t.getStartCodeAddress())).append(':').append(navigationIndex(indexes,t.getStartCodeAddress()+t.getCodeUnitCount()));
            for(ExceptionHandler h:t.getExceptionHandlers())s.append(" catch=").append(h.getExceptionType()).append(':').append(navigationIndex(indexes,h.getHandlerCodeAddress()));s.append('\n');
        }
        try {byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(s.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:bytes)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();}
        catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static int navigationIndex(Map<Integer,Integer> indexes,int address) {Integer i=indexes.get(address);if(i==null)throw new IllegalStateException("Navigation target is not an instruction boundary");return i;}
    private static void navigationFingerprint(Map<String,ClassDef> original,String type,String name,String result,String sha) {
        ClassDef clazz=original.get(type);Method method=clazz==null?null:find(clazz,name,NONE,result);
        if(!sha.equals(instagramNavigationDigest(method)))throw new IllegalStateException("Instagram v447 navigation fingerprint changed: "+type+"->"+name);
    }
    private static void navigationField(Map<String,ClassDef> original,String owner,String name,String type,int flags) {
        ClassDef clazz=original.get(owner);if(clazz!=null)for(Field field:clazz.getFields())if(name.equals(field.getName())&&type.equals(field.getType())&&field.getAccessFlags()==flags)return;
        throw new IllegalStateException("Instagram v447 navigation field changed: "+owner+"->"+name);
    }
    private static void navigationClass(Map<String,ClassDef> original,String type,String parent,int flags) {
        ClassDef c=original.get(type);if(c==null||c.getAccessFlags()!=flags||!parent.equals(c.getSuperclass()))throw new IllegalStateException("Instagram v447 navigation class changed: "+type);
    }
    /** Known runtime upgrades may grant the existing Reels bridge more native
     * authority. Verify its complete supplied body, including the two preceding
     * native guards and the exact runtime call, BEFORE replacing that runtime. */
    public static void verifyInstagramReelsBoundary(Map<String,ClassDef> original) {
        navigationFingerprint(original,"LX/8Zv;","onBackPressed","Z","d5272599e19c7ab59756181378f1a892975ba256ac0c852476de316d43347287");
    }
    public static void verifyInstagramTabBack(Map<String,ClassDef> original) {
        verifyInstagramTabBack(original,"ff296df94864c9d5734607cde02cbd341effdbaff4809e3da3b6b3662ab616f2");
    }
    /** Exact 123-125 native tab gate: all original branches, handlers and
     * the existing runtime call must survive before a runtime-only upgrade. */
    public static void verifyInstagramPatchedTabBack(Map<String,ClassDef> original) {
        verifyInstagramTabBack(original,"d06f8a6c223ec706a5539b04de08ef5ccf38e0f0a9b7e47e39c55d14c15cb99b");
    }
    private static void verifyInstagramTabBack(Map<String,ClassDef> original,String bodySha) {
        navigationClass(original,IG_NAVIGATION,"Ljava/lang/Object;",17);
        navigationClass(original,IG_NAVIGATION_WRAPPER,"Ljava/lang/Object;",17);
        navigationClass(original,IG_TAB,"Ljava/lang/Enum;",16401);
        navigationFingerprint(original,IG_NAVIGATION_WRAPPER,"A0V","Z",bodySha);
        navigationFingerprint(original,IG_NAVIGATION,"A05","Landroidx/fragment/app/Fragment;","0fd89b3e11dc2049c4bba6c95c756aab06eb3c7ae15461e46b9842b27889ea90");
        navigationFingerprint(original,IG_NAVIGATION,"A06",IG_TAB,"d3daec900754cf6f69323ee406c183b8d3441c000a5eb42fe58b0172aa20531b");
        navigationFingerprint(original,IG_TAB,"<clinit>","V","d450f265ed916ae4c1ea11e1966f101578aa90ba2e3a026ef65750cf3c540c19");
        navigationField(original,IG_NAVIGATION,"A03","Landroidx/fragment/app/FragmentActivity;",17);
        navigationField(original,IG_NAVIGATION,"A06","Ljava/util/Stack;",17);
        navigationField(original,IG_NAVIGATION_WRAPPER,"A02",IG_NAVIGATION,1);
        navigationField(original,"LX/2yF;","A00",IG_TAB,17);
        navigationField(original,IG_TAB,"A08",IG_TAB,16409);
    }
    public static void applyInstagramTabBack(Map<String,ClassDef> original,Editor editor,Map<String,Integer> counts) {
        if(Instagram452Patch.supports(original)){Instagram452Patch.applyTab(original,editor,counts);return;}
        if(!original.containsKey(INSTAGRAM_MAIN)||!original.containsKey(IG_NAVIGATION_WRAPPER))return;
        verifyInstagramTabBack(original);
        MutableMethod method=(MutableMethod)find(editor.mutable(IG_NAVIGATION_WRAPPER),"A0V",NONE,"Z");MutableMethodImplementation code=method.getImplementation();
        // All native fragment/layer handling has returned unconsumed, and the
        // history has >1 entries. v1 remains the actual currentFragment, v4 the
        // actual navigation owner. v0's old size result is dead after IF_LE.
        Label handled=code.newLabelForIndex(230);
        code.addInstruction(164,new BuilderInstruction22c(Opcode.IGET_OBJECT,0,4,new ImmutableFieldReference(IG_NAVIGATION,"A03","Landroidx/fragment/app/FragmentActivity;")));
        code.addInstruction(165,runtime("tryInstagramTabBack",Arrays.asList(ACTIVITY,"Ljava/lang/Object;","Ljava/lang/Object;"),"Z",0,4,1));
        code.addInstruction(166,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        // Keep the original handled epilogue: its v11 is already true, it ends
        // systrace and returns v5. False falls through to the untouched pop.
        code.addInstruction(167,new BuilderInstruction21t(Opcode.IF_NEZ,0,handled));
        bump(counts,"instagram_native_tab_back");
    }

    private static final String TWITTER_MAIN="Lcom/twitter/app/main/MainActivity;";
    private static final String TWITTER_ROOT="Lcom/twitter/app/main/l1;";
    private static final String URI="Landroid/net/Uri;";
    private static void twitterClass(Map<String,ClassDef> original,String type,String parent,int flags) {
        ClassDef c=original.get(type);
        if(c==null||c.getAccessFlags()!=flags||!parent.equals(c.getSuperclass()))
            throw new IllegalStateException("X native root class changed: "+type);
    }
    private static void twitterField(Map<String,ClassDef> original,String owner,String name,String type,int flags) {
        ClassDef c=original.get(owner);if(c!=null)for(Field f:c.getFields())
            if(name.equals(f.getName())&&type.equals(f.getType())&&f.getAccessFlags()==flags)return;
        throw new IllegalStateException("X native root field changed: "+owner+"->"+name);
    }
    private static void twitterFingerprint(Map<String,ClassDef> original,String owner,String name,String result,String sha) {
        ClassDef c=original.get(owner);Method m=c==null?null:find(c,name,NONE,result);
        if(m==null||!sha.equals(instagramNavigationDigest(m)))
            throw new IllegalStateException("X native root fingerprint changed: "+owner+"->"+name);
    }
    /** Admit the actual v12.19 root Back body and typed current-page contract.
     * This preflight also runs before an exact historical runtime replacement. */
    public static void verifyTwitterRootBack(Map<String,ClassDef> original) {
        verifyTwitterRootBack(original,"a213880a9ad8bb57e0882ffccdc87e1c6fbf3bd448c38db07c491ddf6dca48e7");
    }
    /** The published 124 APK already has the root gate. Its complete native
     * body must match before replacing runtime classes; never insert it twice. */
    public static void verifyTwitterPatchedRootBack(Map<String,ClassDef> original) {
        verifyTwitterRootBack(original,"a31a6182b0b00e1052952a236ffaec02a35e293ab7ac9ef068372a16f995f7c1");
    }
    private static void verifyTwitterRootBack(Map<String,ClassDef> original,String rootDigest) {
        twitterClass(original,TWITTER_ROOT,"Lcom/twitter/app/legacy/m;",17);
        twitterClass(original,"Lcom/twitter/app/legacy/m;","Lcom/twitter/app/legacy/r;",1025);
        twitterClass(original,"Lcom/twitter/app/legacy/r;","Lcom/twitter/app/legacy/h;",1);
        twitterClass(original,"Lcom/twitter/app/legacy/h;","Ljava/lang/Object;",1025);
        twitterClass(original,TWITTER_MAIN,"Lcom/twitter/app/common/inject/p;",1);
        if(!subtype(TWITTER_MAIN,PLATFORM_ACTIVITIES,original)||!subtype("Landroidx/fragment/app/x;",PLATFORM_ACTIVITIES,original))
            throw new IllegalStateException("X native root owner is not an Activity");
        twitterClass(original,"Lcom/twitter/app/main/x;","Lcom/twitter/ui/viewpager/a;",17);
        twitterClass(original,"Lcom/twitter/app/main/viewpager/a;","Ljava/lang/Object;",1537);
        if(!original.get("Lcom/twitter/app/main/x;").getInterfaces().contains("Lcom/twitter/app/main/viewpager/a;")
            ||!original.get("Lcom/twitter/app/main/viewpager/a;").getInterfaces().contains("Lcom/twitter/ui/viewpager/h;"))
            throw new IllegalStateException("X selected-page interface changed");
        ClassDef pager=original.get("Lcom/twitter/ui/viewpager/h;");Method selected=pager==null?null:find(pager,"p",NONE,URI);
        if(selected==null||selected.getAccessFlags()!=1025||selected.getImplementation()!=null)
            throw new IllegalStateException("X selected-page interface method changed");
        twitterField(original,"Lcom/twitter/app/legacy/h;","b","Landroidx/fragment/app/x;",17);
        twitterField(original,TWITTER_ROOT,"B","Lcom/twitter/app/main/viewpager/a;",17);
        twitterFingerprint(original,TWITTER_ROOT,"goBack","Z",rootDigest);
        twitterFingerprint(original,"Lcom/twitter/ui/viewpager/a;","p",URI,"29b22619b9e8778d2e8ea6b48efb2e2c4ab45c75eaaa444b3a3638c5ef4c7c65");
        twitterFingerprint(original,"Lcom/twitter/main/api/b;","<clinit>","V","d57468df55ce439b0ace68e9af975897ece9b221c55f8ed7df4c2ae340f22443");
    }
    /** A prior runtime's identity does not prove that its native Back scope
     * survived. The X bridge requires this exact 123/124 wrapper to establish the
     * real Activity Back attempt before calling the native presenter. */
    public static void verifyTwitterBackScope(Map<String,ClassDef> original) {
        twitterFingerprint(original,"Lcom/twitter/app/common/inject/p;","onBackPressed","V","df90eb5bf08ac67b0490193d1fcbca0fe853cb315295b93f0d4271744215ccdb");
        twitterFingerprint(original,"Lcom/twitter/app/common/inject/p;","hiroOneBack$onBackPressed","V","610fdf9b44c641b3e5fc51f2e22f1768887320888854f035ff26e2c1d09c1809");
    }
    /** Give only the verified native X presenter entry a balanced runtime
     * scope. Native AndroidX/Compose callbacks remain ahead of this fallback. */
    public static void applyTwitterBackScope(Map<String,ClassDef> original,Editor editor,Map<String,Integer> counts) {
        verifyTwitterBackScope(original);
        MutableMethod method=(MutableMethod)find(editor.mutable("Lcom/twitter/app/common/inject/p;"),"onBackPressed",NONE,"V");
        method.getImplementation().replaceInstruction(4,runtime("enterTwitterBack",Collections.singletonList(ACTIVITY),"V",2));
        bump(counts,"twitter_native_back_scope_upgrade");
    }
    public static void applyTwitterRootBack(Map<String,ClassDef> original,Editor editor,Map<String,Integer> counts) {
        if(!original.containsKey(TWITTER_MAIN))return;
        verifyTwitterRootBack(original);
        MutableMethod method=(MutableMethod)find(editor.mutable(TWITTER_ROOT),"goBack",NONE,"Z");
        MutableMethodImplementation code=method.getImplementation();
        // FAB, drawer, typeahead, onboarding and the current child have already
        // had their native chance to consume Back. v0 is the live pager; v2/v3
        // are dead until the original Home selection that follows this gate.
        Label nativeHome=code.newLabelForIndex(45);
        code.addInstruction(45,new BuilderInstruction22c(Opcode.IGET_OBJECT,2,10,new ImmutableFieldReference("Lcom/twitter/app/legacy/h;","b","Landroidx/fragment/app/x;")));
        code.addInstruction(46,new BuilderInstruction35c(Opcode.INVOKE_INTERFACE,1,0,0,0,0,0,new ImmutableMethodReference("Lcom/twitter/ui/viewpager/h;","p",NONE,URI)));
        code.addInstruction(47,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,3));
        code.addInstruction(48,runtime("tryTwitterRootBack",Arrays.asList(ACTIVITY,URI),"Z",2,3));
        code.addInstruction(49,new BuilderInstruction11x(Opcode.MOVE_RESULT,2));
        code.addInstruction(50,new BuilderInstruction21t(Opcode.IF_EQZ,2,nativeHome));
        code.addInstruction(51,new BuilderInstruction11x(Opcode.RETURN,1));
        // Both the absent-child and unconsumed-child edges must enter the gate.
        Label gate=code.newLabelForIndex(45);
        code.replaceInstruction(40,new BuilderInstruction21t(Opcode.IF_EQZ,2,gate));
        code.replaceInstruction(43,new BuilderInstruction21t(Opcode.IF_EQZ,2,gate));
        bump(counts,"twitter_native_root_back");
    }

    public static Map<String,Integer> applyAll(String packageName,Map<String,ClassDef> original,Editor editor) {
        validatePackage(packageName);
        final boolean instagram = "com.instagram.android".equals(packageName);
        final boolean twitter = "com.twitter.android".equals(packageName);
        Map<String,Integer> counts=new LinkedHashMap<String,Integer>();
        // Complete hierarchy and collision preflight before obtaining mutable proxies.
        int activities=0;
        for(ClassDef c:original.values()) {
            if(c.getType().startsWith("Lapp/hiro/oneback/"))continue;
            boolean activity = subtype(c.getType(),PLATFORM_ACTIVITIES,original);
            boolean dialog = subtype(c.getType(),PLATFORM_DIALOGS,original);
            for(Method m:c.getMethods()) {
                if(m.getName().startsWith(HELPER_PREFIX))throw new IllegalStateException("OneBack already applied: "+c.getType());
                boolean callback = (activity && (signature(m,"onNewIntent",Collections.singletonList("Landroid/content/Intent;"),"V") || signature(m,"onCreate",CREATE_ARGS,"V") || signature(m,"onResume",NONE,"V") || signature(m,"onDestroy",NONE,"V") || signature(m,"onUserLeaveHint",NONE,"V") || signature(m,"onUserInteraction",NONE,"V")))
                    || ((activity || dialog) && (signature(m,"onBackPressed",NONE,"V") || signature(m,"dispatchKeyEvent",KEY_ARGS,"Z")))
                    || (view(c.getType(),original) && (signature(m,"onKeyPreIme",PREIME_ARGS,"Z") || signature(m,"dispatchKeyEventPreIme",KEY_ARGS,"Z")));
                if(callback && instance(m) && (m.getAccessFlags() & 0x100) != 0)
                    throw new IllegalStateException("Native Android callback cannot be safely wrapped: " + c.getType() + "->" + m.getName());
            }
            if(activity)activities++;
        }
        if(activities==0)throw new IllegalStateException("No supported Activity hierarchy; refusing a non-functional OneBack patch");
        for(ClassDef snapshot:original.values()) {
            String type=snapshot.getType(); if(type.startsWith("Lapp/hiro/oneback/"))continue;
            boolean activity=subtype(type,PLATFORM_ACTIVITIES,original);
            boolean dialog=subtype(type,PLATFORM_DIALOGS,original);
            boolean text=subtype(type,PLATFORM_TEXT,original);
            boolean preime=view(type,original)&&(find(snapshot,"onKeyPreIme",PREIME_ARGS,"Z")!=null||find(snapshot,"dispatchKeyEventPreIme",KEY_ARGS,"Z")!=null);
            boolean shows=hasDialogShow(snapshot,original);
            boolean systemBack=hasSystemBack(snapshot);
            boolean terminal=hasTerminalCalls(snapshot,original);
            // Short obfuscated names are shared by unrelated apps. Only the
            // selected APK package may enable app-specific fingerprints.
            int reels=instagram ? instagramReelsBoundary(snapshot,original) : -1;
            InstagramMainBoundary mainReels=instagram ? instagramMainReelsBoundary(snapshot,original) : null;
            if(!activity&&!dialog&&!text&&!preime&&!shows&&!systemBack&&!terminal&&reels<0)continue;
            MutableClass clazz=editor.mutable(type);
            if(reels>=0)instagramReelsHook(clazz,reels,counts);
            if(mainReels!=null)instagramMainReelsHook(clazz,mainReels,counts);
            if(shows)dialogShows(clazz,original,counts);
            if(systemBack)systemBackCalls(clazz,counts);
            if(activity) {
                boolean boundary=!original.containsKey(snapshot.getSuperclass());
                lifecycle(clazz,"onCreate",CREATE_ARGS,"install",boundary,counts);
                newIntent(clazz,boundary,counts);
                lifecycle(clazz,"onResume",NONE,"onResume",boundary,counts);
                lifecycle(clazz,"onDestroy",NONE,"onDestroy",boundary,counts);
                lifecycle(clazz,"onUserLeaveHint",NONE,"onUserLeaveHint",boundary,counts);
                lifecycle(clazz,"onUserInteraction",NONE,"onUserInteraction",boundary,counts);
                back(clazz,ACTIVITY,"back",boundary,counts);
                guard(clazz,"dispatchKeyEvent",KEY_ARGS,ACTIVITY,"key",boundary,counts);
                bump(counts,"activity_classes");
            }
            if(dialog) {
                boolean boundary=!original.containsKey(snapshot.getSuperclass());
                back(clazz,DIALOG,"dialogBack",boundary,counts);
                guard(clazz,"dispatchKeyEvent",KEY_ARGS,DIALOG,"dialogKey",boundary,counts);
                bump(counts,"dialog_classes");
            }
            if(text||preime) {
                boolean boundary=text&&!original.containsKey(snapshot.getSuperclass());
                guard(clazz,"onKeyPreIme",PREIME_ARGS,VIEW,"keyFromView",boundary,counts);
                guard(clazz,"dispatchKeyEventPreIme",KEY_ARGS,VIEW,"keyFromView",false,counts);
                bump(counts,"preime_classes");
            }
            terminalCalls(clazz,original,counts);
        }
        if(instagram) {
            applyInstagramDispatcher(original,editor,counts);
            applyInstagramTabBack(original,editor,counts);
        }
        if(twitter)applyTwitterRootBack(original,editor,counts);
        if(!counts.containsKey("activity_lifecycle_entry")&&!counts.containsKey("activity_lifecycle_added"))throw new IllegalStateException("OneBack lifecycle bootstrap missing");
        return counts;
    }
}
