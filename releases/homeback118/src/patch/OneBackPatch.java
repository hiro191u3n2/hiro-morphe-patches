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

/** Public Android lifecycle/back hooks; no app-obfuscated method fingerprints. */
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
        for (String name : PACKAGES) result.add(new Pair<String, Set<String>>(name, null));
        return result;
    }
    public static void validatePackage(String name) {
        if (!PACKAGES.contains(name)) throw new IllegalStateException("OneBack is restricted to TikTok, Instagram, X and Trip.com: " + name);
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
                try (app.morphe.patcher.util.Document document = context.document("AndroidManifest.xml")) {
                    patchManifest(document, context.getPackageMetadata().getPackageName());
                } catch (RuntimeException failure) { throw new IllegalStateException("Could not save OneBack manifest", failure); }
                return Unit.INSTANCE;
            }
        }, null);

    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "Instagramはリールで戻ると終了・履歴削除。コメント欄が開いている時はコメントだけ閉じる。TikTokはプロフィールから実際のホームタブへ移動し、同じ操作では終了しない。Trip.com・Xの動作を保持。",
        true, compatible(), Collections.singleton(MANIFEST), Collections.emptySet(), new Supplier<InputStream>() {
            @Override public InputStream get() {
                InputStream stream = OneBackPatch.class.getResourceAsStream("/extensions/oneback_exit.mpe");
                if (stream == null) throw new IllegalStateException("Missing OneBack runtime extension");
                return stream;
            }
        }, new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(final BytecodePatchContext context) {
                validatePackage(context.getPackageMetadata().getPackageName());
                final Map<String, ClassDef> original = new LinkedHashMap<String, ClassDef>();
                context.classDefForEach(new Function1<ClassDef, Unit>() {
                    @Override public Unit invoke(ClassDef value) { original.put(value.getType(), value); return Unit.INSTANCE; }
                });
                Map<String,Integer> counts = applyAll(original, new Editor() {
                    @Override public MutableClass mutable(String type) { return context.mutableClassDefBy(type); }
                });
                System.out.println("HomeBack118 hooks: " + counts);
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
            else code.addInstruction(runtime("enterBack",Collections.singletonList(ACTIVITY),"V",2));
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

    public static Map<String,Integer> applyAll(Map<String,ClassDef> original,Editor editor) {
        Map<String,Integer> counts=new LinkedHashMap<String,Integer>();
        // Complete hierarchy and collision preflight before obtaining mutable proxies.
        int activities=0;
        for(ClassDef c:original.values()) {
            if(c.getType().startsWith("Lapp/hiro/oneback/"))continue;
            boolean activity = subtype(c.getType(),PLATFORM_ACTIVITIES,original);
            boolean dialog = subtype(c.getType(),PLATFORM_DIALOGS,original);
            for(Method m:c.getMethods()) {
                if(m.getName().startsWith(HELPER_PREFIX))throw new IllegalStateException("OneBack already applied: "+c.getType());
                boolean callback = (activity && (signature(m,"onCreate",CREATE_ARGS,"V") || signature(m,"onResume",NONE,"V") || signature(m,"onDestroy",NONE,"V") || signature(m,"onUserLeaveHint",NONE,"V") || signature(m,"onUserInteraction",NONE,"V")))
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
            if(!activity&&!dialog&&!text&&!preime&&!shows&&!systemBack&&!terminal)continue;
            MutableClass clazz=editor.mutable(type);
            if(shows)dialogShows(clazz,original,counts);
            if(systemBack)systemBackCalls(clazz,counts);
            if(activity) {
                boolean boundary=!original.containsKey(snapshot.getSuperclass());
                lifecycle(clazz,"onCreate",CREATE_ARGS,"install",boundary,counts);
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
        if(!counts.containsKey("activity_lifecycle_entry")&&!counts.containsKey("activity_lifecycle_added"))throw new IllegalStateException("OneBack lifecycle bootstrap missing");
        return counts;
    }
}
