package app.hiro.quicksearch.patches;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Reviewed, build-gated manual and automatic exit paths for QuickSearch 0.4.5 (36). */
public final class QuickSearchRecentsPatch {
    private QuickSearchRecentsPatch() {}
    public static final String NAME = "検索後の終了時に履歴へ残さない";
    public static final String PACKAGE_NAME = "jp.ddo.sugihiro.quicksearch";
    public static final String VERSION = "0.4.5";
    public static final long VERSION_CODE = 36L;
    private static final String BASE = "Ljp/ddo/sugihiro/quicksearch/";
    public static final String MAIN = BASE + "activity/MainActivity;";
    public static final String WEB = BASE + "activity/WebActivity;";
    public static final String CONFIG = BASE + "activity/ConfigActivity;";
    public static final String COMMON = BASE + "activity/CommonActivity;";
    public static final String CANCEL = BASE + "activity/MainActivity$10;";
    public static final String HISTORY_CLOSE = BASE + "activity/CommonActivity$3;";
    public static final String HISTORY = BASE + "adapter/HistoryArrayAdapter;";
    public static final String CUSTOM = BASE + "custom/CustomSearhConfigDialog;";
    public static final String FRAGMENT = "Landroidx/fragment/app/DialogFragment;";
    public static final String PLACEHOLDER = BASE + "activity/ui/main/PlaceholderFragment;";
    public static final String WEB_CLIENT = BASE + "activity/ui/main/PlaceholderFragment$1;";
    public static final String AUTO_COMPLETE = "Landroidx/appcompat/widget/AppCompatAutoCompleteTextView;";
    public static final String EDIT_TEXT = "Landroidx/appcompat/widget/AppCompatEditText;";
    public static final String RUNTIME = "Lapp/hiro/quicksearch/runtime/SearchTask;";
    public static final String CLEANUP = "Lapp/hiro/quicksearch/runtime/SearchTask$Cleanup;";
    private static final String ACTIVITY = "Landroid/app/Activity;";
    private static final String INTENT = "Landroid/content/Intent;";
    private static final String VIEW = "Landroid/view/View;";
    private static final String DIALOG = "Landroid/app/Dialog;";
    private static final String ALERT = "Landroid/app/AlertDialog;";
    private static final String BUILDER = "Landroid/app/AlertDialog$Builder;";
    private static final String APP_COMPAT = "Landroidx/appcompat/app/AppCompatActivity;";
    private static final String STRING = "Ljava/lang/String;";
    private static final String KEY = "Landroid/view/KeyEvent;";
    private static final String ON_CREATE = "onCreate(Landroid/os/Bundle;)V";
    private static final String RESULT = "onActivityResult(IILandroid/content/Intent;)V";
    public static final List<String> TARGETS = Collections.unmodifiableList(Arrays.asList(
            MAIN, WEB, CONFIG, CANCEL, COMMON, HISTORY_CLOSE, HISTORY, CUSTOM, FRAGMENT, PLACEHOLDER, WEB_CLIENT, AUTO_COMPLETE, EDIT_TEXT));
    public static final int EXPECTED_EDIT_COUNT = 31;
    public static final Map<String, String> FINGERPRINTS;
    public static final Map<String, String> CLASS_FINGERPRINTS;
    private static final Map<String, String> SUPERS;
    private static final Set<String> PLATFORM_SHOW;
    private static final Set<String> EXTERNAL;

    static {
        Map<String, String> hashes = new LinkedHashMap<String, String>();
        hashes.put(MAIN + "->actionAfterSearched()V", "fd876b94a1439b10ab4406bcaef35546e332790eaef79589211a3a2da74b743e");
        hashes.put(MAIN + "->search(Ljava/lang/String;Ljava/lang/String;)V", "82eeeb778986963fe1e7bafa3af542e05cec78ada71ac8e7ab9d198809f2793e");
        hashes.put(MAIN + "->showHelp()V", "e27be07dffd98f3e6bb9336f93004329b10b0618dc3f89dd26bd45973b0ac596");
        hashes.put(MAIN + "->stopClipBoardTimer()V", "73709d65eae507c187d46e48ecc9b3817c42246179ce0c528d89e40ecaf71eec");
        hashes.put(MAIN + "->" + RESULT, "aa85b5c8e170e65a672da329be94609ff51e1f43010ff21fc438a50825185afd");
        hashes.put(MAIN + "->" + ON_CREATE, "a5715d7aeb3d7c521b3fba08498478c991d980bd90c25d989d9a125731f053fe");
        hashes.put(WEB + "->" + RESULT, "3efc6d215c2143c4efbe462137f9d15e9e6effe8b24582a609f254b14ae57796");
        hashes.put(WEB + "->" + ON_CREATE, "e00df9e02200144d4f373744b296749a7b7c56ab0112dc362c28549bcedf567c");
        hashes.put(WEB + "->onDestroy()V", "70989a2843e7fe851879990399cc0d65e842b26a93c1a438089f27c6b51ba339");
        hashes.put(CONFIG + "->" + ON_CREATE, "1d54dd0e2511b2984bc8a844d179967082b48d0678567843502590b6f77b04c9");
        hashes.put(CANCEL + "->onCancel(Landroid/content/DialogInterface;)V", "8e723ff3f9112ce5c40eeca9ecb02ec925373acd407a1fef5c1ca1f31aa94d2d");
        hashes.put(COMMON + "->showHistoryDialog(" + BASE + "activity/Searchable;Landroid/widget/EditText;)V", "7ac0112ed964d7ffa8368bc70767ce9946ddcd57eabec5411be2a262b688b6d6");
        hashes.put(HISTORY_CLOSE + "->onClick(Landroid/view/View;)V", "db59e49257e3b754ae56fc77cf4b32713b78d72b553c3b5493fb4b06be2dad5f");
        hashes.put(HISTORY + "->delete()V", "4fa3bd7b19e9d4674663383a2a3c3ce17fd1293dc15397f5175a039c65b88d80");
        hashes.put(CUSTOM + "->show(" + BASE + "bean/CustomSearchBean;)Landroid/view/View;", "c84aab6170586ebaea5ce1fd526db61e7d7ec84a270fa0a9641c518f1ba71d20");
        hashes.put(FRAGMENT + "->onStart()V", "603a60baa792dfdba2983a9eae5473f9dce32240fead1a555af24946befdedd5");
        hashes.put(PLACEHOLDER + "->navigateDefaultBrowser(Landroid/app/Activity;Ljava/lang/String;)V", "7969219a8881b12db5ab5e93e0345c1e3ed52ac77a651f3dd459b5bd7abf36cb");
        hashes.put(WEB_CLIENT + "->shouldOverrideUrlLoading(Landroid/webkit/WebView;Landroid/webkit/WebResourceRequest;)Z", "fdc734d5905dc7da07b637e56169e18de4532a1c6c946aa7933edc242c1a7390");
        FINGERPRINTS = Collections.unmodifiableMap(hashes);
        Map<String, String> classHashes = new LinkedHashMap<String, String>();
        classHashes.put(AUTO_COMPLETE, "6073877220d083f538e0763bfa6fdf8bebab420bd6f5eaeeb96916459dbe3380");
        classHashes.put(EDIT_TEXT, "003edd427560224b400110cce43f7a14c3710b6d79d33ba9f02c4cad34163070");
        CLASS_FINGERPRINTS = Collections.unmodifiableMap(classHashes);
        Map<String, String> supers = new HashMap<String, String>();
        supers.put(MAIN, COMMON); supers.put(WEB, COMMON); supers.put(CONFIG, APP_COMPAT);
        supers.put(COMMON, APP_COMPAT); supers.put(CANCEL, "Ljava/lang/Object;");
        supers.put(HISTORY_CLOSE, "Ljava/lang/Object;"); supers.put(HISTORY, "Landroid/widget/ArrayAdapter;");
        supers.put(CUSTOM, "Ljava/lang/Object;"); supers.put(FRAGMENT, "Landroidx/fragment/app/Fragment;");
        supers.put(PLACEHOLDER, "Landroidx/fragment/app/Fragment;"); supers.put(WEB_CLIENT, "Landroid/webkit/WebViewClient;");
        supers.put(AUTO_COMPLETE, "Landroid/widget/AutoCompleteTextView;"); supers.put(EDIT_TEXT, "Landroid/widget/EditText;");
        SUPERS = Collections.unmodifiableMap(supers);
        PLATFORM_SHOW = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                MAIN + "->" + ON_CREATE, MAIN + "->" + RESULT, MAIN + "->showHelp()V", WEB + "->" + RESULT,
                COMMON + "->showHistoryDialog(" + BASE + "activity/Searchable;Landroid/widget/EditText;)V",
                HISTORY + "->delete()V")));
        EXTERNAL = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                MAIN + "->search(Ljava/lang/String;Ljava/lang/String;)V",
                PLACEHOLDER + "->navigateDefaultBrowser(Landroid/app/Activity;Ljava/lang/String;)V",
                WEB_CLIENT + "->shouldOverrideUrlLoading(Landroid/webkit/WebView;Landroid/webkit/WebResourceRequest;)Z")));
    }

    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "簡単検索くん 0.4.5 (36): ❌・ナビゲーションの戻るで画面と自タスクを終了し履歴から削除。Android 16の戻る、Web・設定・ダイアログにも対応。検索後自動終了設定を維持し、外部ブラウザーは別タスクへ。実機未検証。",
        true,
        Collections.singleton(new Pair<String, Set<String>>(PACKAGE_NAME, Collections.singleton(VERSION))),
        Collections.emptySet(), Collections.emptySet(),
        new Supplier<InputStream>() {
            @Override public InputStream get() {
                InputStream stream = QuickSearchRecentsPatch.class.getResourceAsStream("/extensions/quicksearch_recents.mpe");
                if (stream == null) throw new IllegalStateException("Missing quicksearch exit extension");
                return stream;
            }
        },
        new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(BytecodePatchContext context) {
                validatePackage(context.getPackageMetadata().getPackageName(),
                        context.getPackageMetadata().getVersionName(), context.getPackageMetadata().getVersionCode());
                Map<String, ClassDef> original = new LinkedHashMap<String, ClassDef>();
                for (String type : TARGETS) original.put(type, context.classDefBy(type));
                validateAll(original);
                Map<String, MutableClass> mutable = new LinkedHashMap<String, MutableClass>();
                for (String type : TARGETS) mutable.put(type, context.mutableClassDefBy(type));
                applyAll(mutable);
                return Unit.INSTANCE;
            }
        }, null
    );

    public static void validatePackage(String packageName, String versionName, String versionCode) {
        if (!PACKAGE_NAME.equals(packageName) || !VERSION.equals(versionName)
                || !Long.toString(VERSION_CODE).equals(versionCode)) {
            throw new IllegalStateException("Only original " + PACKAGE_NAME + " " + VERSION + " (36) is supported, including with force enabled.");
        }
    }
    public static void validatePackage(String packageName, String versionName, long versionCode) {
        validatePackage(packageName, versionName, Long.toString(versionCode));
    }

    public static String methodKey(MethodReference method) {
        StringBuilder out = new StringBuilder(method.getDefiningClass()).append("->").append(method.getName()).append('(');
        for (CharSequence type : method.getParameterTypes()) out.append(type);
        return out.append(')').append(method.getReturnType()).toString();
    }
    private static boolean signature(MethodReference call, String name, List<String> parameters, String result) {
        if (!name.equals(call.getName()) || !result.equals(call.getReturnType()) || parameters.size() != call.getParameterTypes().size()) return false;
        for (int i = 0; i < parameters.size(); i++) if (!parameters.get(i).contentEquals(call.getParameterTypes().get(i))) return false;
        return true;
    }
    private static MethodReference ref(Instruction instruction) {
        if (!(instruction instanceof ReferenceInstruction)) return null;
        Reference value = ((ReferenceInstruction) instruction).getReference();
        return value instanceof MethodReference ? (MethodReference) value : null;
    }
    private static MethodReference runtime(String name, List<String> parameters, String result) {
        return new ImmutableMethodReference(RUNTIME, name, parameters, result);
    }

    /** Same-size invocation replacements, limited to the reviewed original methods. */
    public static MethodReference replacement(MethodReference owner, Instruction instruction) {
        if (instruction.getOpcode() != Opcode.INVOKE_VIRTUAL && instruction.getOpcode() != Opcode.INVOKE_VIRTUAL_RANGE) return null;
        MethodReference call = ref(instruction);
        if (call == null) return null;
        String key = methodKey(owner);
        if ((key.equals(MAIN + "->actionAfterSearched()V") || key.equals(CANCEL + "->onCancel(Landroid/content/DialogInterface;)V"))
                && MAIN.equals(call.getDefiningClass()) && signature(call, "finish", Collections.<String>emptyList(), "V")) {
            return runtime("finish", Collections.singletonList(ACTIVITY), "V");
        }
        if (EXTERNAL.contains(key) && (MAIN.equals(call.getDefiningClass()) || ACTIVITY.equals(call.getDefiningClass()))
                && signature(call, "startActivity", Collections.singletonList(INTENT), "V")) {
            return runtime("startExternal", Arrays.asList(ACTIVITY, INTENT), "V");
        }
        if (PLATFORM_SHOW.contains(key) && BUILDER.equals(call.getDefiningClass())
                && signature(call, "show", Collections.<String>emptyList(), ALERT)) {
            return runtime("showPlatform", Collections.singletonList(BUILDER), ALERT);
        }
        if (key.equals(FRAGMENT + "->onStart()V") && DIALOG.equals(call.getDefiningClass())
                && signature(call, "show", Collections.<String>emptyList(), "V")) {
            return runtime("showDialog", Collections.singletonList(DIALOG), "V");
        }
        if (key.equals(HISTORY_CLOSE + "->onClick(Landroid/view/View;)V") && ALERT.equals(call.getDefiningClass())
                && signature(call, "cancel", Collections.<String>emptyList(), "V")) {
            return runtime("closeDialog", Collections.singletonList(DIALOG), "V");
        }
        return null;
    }

    public static String fingerprint(Method method) {
        try {
            ImmutableClassDef wrapper = new ImmutableClassDef(method.getDefiningClass(), 1, "Ljava/lang/Object;",
                    Collections.<String>emptyList(), null, Collections.emptySet(), Collections.emptyList(),
                    Collections.singletonList(ImmutableMethod.of(method)));
            return classFingerprint(wrapper);
        } catch (Exception failure) { throw new IllegalStateException("Could not fingerprint " + methodKey(method), failure); }
    }
    public static String classFingerprint(ClassDef clazz) {
        try {
            MemoryDataStore data = new MemoryDataStore();
            try {
                DexPool.writeTo(data, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(clazz)));
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(data.getBuffer(), 0, data.getSize());
                StringBuilder result = new StringBuilder(64);
                for (byte value : digest.digest()) {
                    result.append(Character.forDigit((value >>> 4) & 15, 16));
                    result.append(Character.forDigit(value & 15, 16));
                }
                return result.toString();
            } finally { data.close(); }
        } catch (Exception failure) { throw new IllegalStateException("Could not fingerprint " + clazz.getType(), failure); }
    }

    private static void field(ClassDef clazz, String name, String type, int flags) {
        int found = 0;
        for (Field field : clazz.getFields()) if (field.getName().equals(name)) {
            if (!type.equals(field.getType()) || flags != field.getAccessFlags()) throw new IllegalStateException("Changed cleanup field: " + name);
            found++;
        }
        if (found != 1) throw new IllegalStateException("Missing cleanup field: " + name);
    }
    private static boolean activity(String type) { return MAIN.equals(type) || WEB.equals(type) || CONFIG.equals(type); }
    private static boolean newMethod(String type, String signature) {
        if (AUTO_COMPLETE.equals(type) || EDIT_TEXT.equals(type)) return signature.equals("onKeyPreIme(ILandroid/view/KeyEvent;)Z");
        if (!activity(type)) return false;
        return signature.equals("onBackPressed()V") || signature.equals("dispatchKeyEvent(Landroid/view/KeyEvent;)Z")
                || ((MAIN.equals(type) || CONFIG.equals(type)) && signature.equals("onDestroy()V"))
                || (MAIN.equals(type) && signature.equals("hiroQuickSearchCleanup()V"));
    }

    public static void validateClass(ClassDef clazz) {
        if (clazz == null || !TARGETS.contains(clazz.getType()) || !SUPERS.get(clazz.getType()).equals(clazz.getSuperclass())) {
            throw new IllegalStateException("Missing or changed quicksearch target class");
        }
        if (clazz.getInterfaces().contains(CLEANUP)) throw new IllegalStateException("Quicksearch exit cleanup already installed");
        String classHash = CLASS_FINGERPRINTS.get(clazz.getType());
        if (classHash != null && !classHash.equals(classFingerprint(clazz))) throw new IllegalStateException("Unverified text widget class: " + clazz.getType());
        Set<String> found = new HashSet<String>();
        int replacements = 0;
        for (Method method : clazz.getMethods()) {
            String key = methodKey(method);
            if (newMethod(clazz.getType(), key.substring(key.indexOf("->") + 2))) throw new IllegalStateException("Existing method collision: " + key);
            String hash = FINGERPRINTS.get(key);
            if (hash != null) {
                if (!found.add(key) || method.getImplementation() == null || !hash.equals(fingerprint(method))) {
                    throw new IllegalStateException("Unverified or patched target method: " + key);
                }
            }
            if (method.getImplementation() == null) continue;
            for (Instruction instruction : method.getImplementation().getInstructions()) {
                MethodReference call = ref(instruction);
                if (call != null && call.getDefiningClass().startsWith("Lapp/hiro/quicksearch/runtime/")) throw new IllegalStateException("Quicksearch exit already patched");
                MethodReference target = replacement(method, instruction);
                if (target != null) {
                    int words = instruction instanceof FiveRegisterInstruction ? ((FiveRegisterInstruction) instruction).getRegisterCount()
                            : instruction instanceof RegisterRangeInstruction ? ((RegisterRangeInstruction) instruction).getRegisterCount() : -1;
                    if (words != target.getParameterTypes().size() || instruction.getCodeUnits() != 3) throw new IllegalStateException("Unsafe hook encoding");
                    replacements++;
                }
            }
        }
        for (String key : FINGERPRINTS.keySet()) if (key.startsWith(clazz.getType() + "->") && !found.contains(key)) throw new IllegalStateException("Missing method: " + key);
        int expected = MAIN.equals(clazz.getType()) ? 5 : CONFIG.equals(clazz.getType()) || CUSTOM.equals(clazz.getType())
                || AUTO_COMPLETE.equals(clazz.getType()) || EDIT_TEXT.equals(clazz.getType()) ? 0 : 1;
        if (replacements != expected) throw new IllegalStateException("Unexpected original hook count: " + clazz.getType());
        if (MAIN.equals(clazz.getType())) {
            field(clazz, "alertDialog", ALERT, 2); field(clazz, "inputView", VIEW, 2);
            field(clazz, "timer", "Ljava/util/Timer;", 2);
            field(clazz, "timerTask", BASE + "activity/MainActivity$ClipBoaedTimerTask;", 2);
            field(clazz, "handler", "Landroid/os/Handler;", 18); field(clazz, "isConfig", "Z", 10);
        }
        if (CUSTOM.equals(clazz.getType())) {
            int sites = 0;
            for (Method method : clazz.getMethods()) if (FINGERPRINTS.containsKey(methodKey(method))) {
                List<? extends Instruction> code = list(method.getImplementation().getInstructions());
                for (int i = 0; i < code.size(); i++) if (customShow(code.get(i))) {
                    if (i + 1 >= code.size() || code.get(i + 1).getOpcode() != Opcode.MOVE_RESULT_OBJECT) throw new IllegalStateException("Unexpected custom Dialog result");
                    sites++;
                }
            }
            if (sites != 1) throw new IllegalStateException("Missing custom Dialog show");
        }
    }
    public static void validateAll(Map<String, ? extends ClassDef> classes) {
        for (String type : TARGETS) validateClass(classes.get(type));
    }
    public static int applyAll(Map<String, MutableClass> classes) {
        validateAll(classes);
        int count = 0;
        for (String type : TARGETS) count += apply(classes.get(type));
        if (count != EXPECTED_EDIT_COUNT) throw new IllegalStateException("Quicksearch plan edit count changed: " + count);
        return count;
    }

    public static BuilderInstruction redirect(Instruction old, MethodReference target) {
        if (old instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction r = (FiveRegisterInstruction) old;
            return new BuilderInstruction35c(Opcode.INVOKE_STATIC, r.getRegisterCount(), r.getRegisterC(), r.getRegisterD(), r.getRegisterE(), r.getRegisterF(), r.getRegisterG(), target);
        }
        if (old instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction r = (RegisterRangeInstruction) old;
            return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, r.getStartRegister(), r.getRegisterCount(), target);
        }
        throw new IllegalStateException("Unsupported invoke encoding");
    }
    private static <T> List<T> list(Iterable<? extends T> values) {
        List<T> out = new ArrayList<T>(); for (T value : values) out.add(value); return out;
    }
    private static BuilderInstruction call(Opcode opcode, MethodReference target, int... r) {
        if (r.length > 5) throw new IllegalStateException("Too many hook registers");
        int[] words = new int[5]; System.arraycopy(r, 0, words, 0, r.length);
        return new BuilderInstruction35c(opcode, r.length, words[0], words[1], words[2], words[3], words[4], target);
    }
    private static BuilderInstruction runtimeCall(String name, List<String> parameters, String result, int... r) {
        return call(Opcode.INVOKE_STATIC, runtime(name, parameters, result), r);
    }
    private static void insert(MutableMethodImplementation code, int index, BuilderInstruction... added) {
        for (int i = 0; i < added.length; i++) code.addInstruction(index + i, added[i]);
    }
    private static int lastReturn(MutableMethod method) {
        List<BuilderInstruction> code = method.getImplementation().getInstructions();
        if (code.get(code.size() - 1).getOpcode() != Opcode.RETURN_VOID) throw new IllegalStateException("Unexpected attach return");
        return code.size() - 1;
    }
    private static void resultGuard(MutableMethod method) {
        MutableMethodImplementation code = method.getImplementation();
        int self = code.getRegisterCount() - 4;
        if (self < 1) throw new IllegalStateException("No local for closing guard");
        Label original = code.newLabelForIndex(0);
        insert(code, 0, runtimeCall("isClosing", Collections.singletonList(ACTIVITY), "Z", self),
                new BuilderInstruction11x(Opcode.MOVE_RESULT, 0), new BuilderInstruction21t(Opcode.IF_EQZ, 0, original),
                new BuilderInstruction10x(Opcode.RETURN_VOID));
    }
    private static boolean customShow(Instruction instruction) {
        MethodReference ref = ref(instruction);
        return ref != null && "Landroidx/appcompat/app/AlertDialog$Builder;".equals(ref.getDefiningClass())
                && signature(ref, "show", Collections.<String>emptyList(), "Landroidx/appcompat/app/AlertDialog;");
    }

    public static int apply(MutableClass clazz) {
        validateClass(clazz);
        String type = clazz.getType();
        int edits = 0;
        for (MutableMethod method : clazz.getMethods()) {
            if (method.getImplementation() == null) continue;
            MutableMethodImplementation code = method.getImplementation();
            List<BuilderInstruction> instructions = code.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                MethodReference target = replacement(method, instructions.get(i));
                if (target != null) { code.replaceInstruction(i, redirect(instructions.get(i), target)); edits++; }
            }
            String localKey = methodKey(method).substring(type.length() + 2);
            if (localKey.equals(ON_CREATE) && MAIN.equals(type)) {
                int self = code.getRegisterCount() - 2;
                insert(code, lastReturn(method),
                        new BuilderInstruction22c(Opcode.IGET_OBJECT, 0, self, new ImmutableFieldReference(MAIN, "alertDialog", ALERT)),
                        new BuilderInstruction22c(Opcode.IGET_OBJECT, 1, self, new ImmutableFieldReference(MAIN, "inputView", VIEW)),
                        runtimeCall("bindMain", Arrays.asList(ACTIVITY, DIALOG, VIEW), "V", self, 0, 1));
                edits++;
            } else if (localKey.equals(ON_CREATE) && WEB.equals(type)) {
                // Original onCreate reuses p0 for TabLayout; its Activity is retained in v2.
                insert(code, lastReturn(method), runtimeCall("attach", Collections.singletonList(ACTIVITY), "V", 2));
                edits++;
            } else if (localKey.equals(ON_CREATE) && CONFIG.equals(type)) {
                // Original Config onCreate overwrites p0 with FragmentTransaction. Attach after super.
                insert(code, 1, runtimeCall("attach", Collections.singletonList(ACTIVITY), "V", code.getRegisterCount() - 2));
                edits++;
            }
            if (localKey.equals(RESULT) && (MAIN.equals(type) || WEB.equals(type))) { resultGuard(method); edits++; }
            if (localKey.equals("onDestroy()V") && WEB.equals(type)) {
                insert(code, 0, runtimeCall("detach", Collections.singletonList(ACTIVITY), "V", code.getRegisterCount() - 1));
                edits++;
            }
            if (CUSTOM.equals(type) && FINGERPRINTS.containsKey(methodKey(method))) {
                for (int i = instructions.size() - 2; i >= 0; i--) if (customShow(instructions.get(i))) {
                    int dialog = ((OneRegisterInstruction) instructions.get(i + 1)).getRegisterA();
                    insert(code, i + 2, runtimeCall("bindDialog", Collections.singletonList(DIALOG), "V", dialog));
                    edits++;
                }
            }
        }
        if (activity(type)) {
            clazz.getMethods().add(backMethod(type)); edits++;
            clazz.getMethods().add(keyMethod(type, clazz.getSuperclass())); edits++;
            if (!WEB.equals(type)) { clazz.getMethods().add(destroyMethod(type, clazz.getSuperclass())); edits++; }
            if (MAIN.equals(type)) {
                clazz.getInterfaces().add(CLEANUP);
                clazz.getMethods().add(cleanupMethod()); edits++;
            }
        }
        if (AUTO_COMPLETE.equals(type) || EDIT_TEXT.equals(type)) {
            clazz.getMethods().add(preImeMethod(type, clazz.getSuperclass())); edits++;
        }
        return edits;
    }

    private static MutableMethod method(String owner, String name, List<String> parameters, String result, int flags, MutableMethodImplementation code) {
        List<ImmutableMethodParameter> args = new ArrayList<ImmutableMethodParameter>();
        for (String parameter : parameters) args.add(new ImmutableMethodParameter(parameter, Collections.emptySet(), null));
        return new MutableMethod(new ImmutableMethod(owner, name, args, result, flags, Collections.emptySet(), Collections.emptySet(), code));
    }
    private static MutableMethod backMethod(String owner) {
        MutableMethodImplementation code = new MutableMethodImplementation(1);
        code.addInstruction(runtimeCall("finish", Collections.singletonList(ACTIVITY), "V", 0));
        code.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return method(owner, "onBackPressed", Collections.<String>emptyList(), "V", 1, code);
    }
    private static MutableMethod destroyMethod(String owner, String parent) {
        MutableMethodImplementation code = new MutableMethodImplementation(1);
        code.addInstruction(runtimeCall("detach", Collections.singletonList(ACTIVITY), "V", 0));
        code.addInstruction(call(Opcode.INVOKE_SUPER, new ImmutableMethodReference(parent, "onDestroy", Collections.<String>emptyList(), "V"), 0));
        code.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return method(owner, "onDestroy", Collections.<String>emptyList(), "V", 4, code);
    }
    private static MutableMethod keyMethod(String owner, String parent) {
        MutableMethodImplementation code = new MutableMethodImplementation(3);
        code.addInstruction(runtimeCall("handleBackKey", Arrays.asList(ACTIVITY, KEY), "Z", 1, 2));
        code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        code.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        code.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 1));
        code.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        code.addInstruction(call(Opcode.INVOKE_SUPER, new ImmutableMethodReference(parent, "dispatchKeyEvent", Collections.singletonList(KEY), "Z"), 1, 2));
        code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        code.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        code.replaceInstruction(2, new BuilderInstruction21t(Opcode.IF_EQZ, 0, code.newLabelForIndex(5)));
        return method(owner, "dispatchKeyEvent", Collections.singletonList(KEY), "Z", 1, code);
    }
    private static MutableMethod cleanupMethod() {
        MutableMethodImplementation code = new MutableMethodImplementation(3);
        code.addInstruction(new BuilderInstruction22c(Opcode.IGET_OBJECT, 0, 2,
                new ImmutableFieldReference(MAIN, "timerTask", BASE + "activity/MainActivity$ClipBoaedTimerTask;")));
        code.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        code.addInstruction(call(Opcode.INVOKE_VIRTUAL, new ImmutableMethodReference("Ljava/util/TimerTask;", "cancel", Collections.<String>emptyList(), "Z"), 0));
        code.addInstruction(call(Opcode.INVOKE_DIRECT, new ImmutableMethodReference(MAIN, "stopClipBoardTimer", Collections.<String>emptyList(), "V"), 2));
        code.addInstruction(new BuilderInstruction22c(Opcode.IGET_OBJECT, 0, 2, new ImmutableFieldReference(MAIN, "handler", "Landroid/os/Handler;")));
        code.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 1, 0));
        code.addInstruction(call(Opcode.INVOKE_VIRTUAL, new ImmutableMethodReference("Landroid/os/Handler;", "removeCallbacksAndMessages", Collections.singletonList("Ljava/lang/Object;"), "V"), 0, 1));
        code.addInstruction(new BuilderInstruction21c(Opcode.SPUT_BOOLEAN, 1, new ImmutableFieldReference(MAIN, "isConfig", "Z")));
        code.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        code.replaceInstruction(1, new BuilderInstruction21t(Opcode.IF_EQZ, 0, code.newLabelForIndex(3)));
        return method(MAIN, "hiroQuickSearchCleanup", Collections.<String>emptyList(), "V", 1, code);
    }
    private static MutableMethod preImeMethod(String owner, String parent) {
        MutableMethodImplementation code = new MutableMethodImplementation(4);
        code.addInstruction(runtimeCall("handleBackKeyFromView", Arrays.asList(VIEW, KEY), "Z", 1, 3));
        code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        code.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        code.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 1));
        code.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        code.addInstruction(call(Opcode.INVOKE_SUPER, new ImmutableMethodReference(parent, "onKeyPreIme", Arrays.asList("I", KEY), "Z"), 1, 2, 3));
        code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        code.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        code.replaceInstruction(2, new BuilderInstruction21t(Opcode.IF_EQZ, 0, code.newLabelForIndex(5)));
        return method(owner, "onKeyPreIme", Arrays.asList("I", KEY), "Z", 1, code);
    }
}
