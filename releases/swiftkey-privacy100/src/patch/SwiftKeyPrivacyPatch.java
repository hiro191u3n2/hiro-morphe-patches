package app.hiro.swiftkeyprivacy.patches;

import java.security.MessageDigest;
import java.util.*;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
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
import org.w3c.dom.*;

/** Optional telemetry-only boundaries for the verified SwiftKey Beta build. */
public final class SwiftKeyPrivacyPatch {
    private SwiftKeyPrivacyPatch() {}
    public static final String NAME = "テレメトリ・解析・エラー報告を停止";
    public static final String PACKAGE = "com.touchtype.swiftkey.beta";
    public static final String VERSION = "9.13.16.4";
    public static final String CODE = "1236271168";
    public static final String PREFIX = "app/hiro/swiftkeyprivacy/";
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    public static final String ADJUST_INIT = "Lcom/adjust/sdk/Adjust;->initSdk(Lcom/adjust/sdk/AdjustConfig;)V";
    public static final String ADJUST_WAIT = "Ltm0/d0;->E(Lcm0/f;)Ljava/lang/Object;";
    public static final String SUCCESS_OWNER = "Lpn0/n;";
    public static final String SUCCESS_INTERFACE = "Lpn0/o;";
    public static final String UNIT_OWNER = "Lnl0/f0;";

    /** Only these six existing method bodies may change. */
    public static final String[] CONTRACTS = {
        "Lwh0/j;->b(Lorg/apache/avro/generic/GenericRecord;)V",
        "Lqd0/a;->b(Ltl0/c;)Ljava/lang/Object;",
        "Lqd0/a;->a(Lqn0/f;Ltl0/c;)Ljava/lang/Object;",
        "Ln70/g;->a(Z)V",
        "Lcs/w;->c()Z",
        "Ln70/h;->run()V"
    };
    public static final int[] ACCESS = {17, 1, 1, 17, 131073, 17};
    public static final String[] ACTIONS = {
        "drop_first_party_record", "complete_primary_upload_without_sending",
        "complete_secondary_upload_without_sending", "skip_adjust_initialization",
        "disable_crashlytics_automatic_collection", "skip_disabled_adjust_attribution_wait"
    };
    // Pinned from the supplied Beta 9.13.16.4 APK. Debug positions are excluded;
    // instructions, exception tables, signatures and annotations remain covered.
    // Both entries, where present, are fixed encodings of the SAME input method
    // from the legacy and 2.2 DexPool versions used by supported Morphe loaders.
    public static final String[][] HASHES = {
        {"aad551814984fb4c35461f5ab73e65b778a6fe13e8f6a385ff2d4723b6551e5e"},
        {"e2af5cb3814ba552d09cad7a1d8067b167fd9e370353f42f3ecf3e1ad3e53cf7",
         "9ae5239718b164da3297a2628d287bc77922dcfaf2df1e40f2965e5ddf902c56"},
        {"2ef5c79ecc345284499e981b18316b13318be2555cb9643a508076ef91287214",
         "fd51e4b098eb5c2030730728e4f2316c74671acd96713a8d922e6962cb100360"},
        {"4709b9379480855a98ca8b50fc51bb17210225a0be8ec130f5ae873775bec3dc",
         "0e6c3fb4e78d3f32c61287ec5ffec603c2755ef580e72f8f73b935515bcd4733"},
        {"b2217d7538d7db6580ef7868034e9133850be0035d1f67f2ab48f96b101cacda"},
        {"108c638636e1174703275ccf15eed9cd3b5ae4d808ed060ebdbbdecf00d75558",
         "bf59e7d132c980e4cae50f57810b2f191f2f04436e0e4f86aa61af70d2784186"}
    };
    public static final String[][] DISABLED_COMPONENTS = {
        {"receiver", "com.google.android.gms.analytics.AnalyticsReceiver"},
        {"service", "com.google.android.gms.analytics.AnalyticsService"},
        {"service", "com.google.android.gms.analytics.AnalyticsJobService"},
        {"provider", "com.adjust.sdk.SystemLifecycleContentProvider"}
    };
    public static final String[] DISABLED_METADATA = {
        "firebase_crashlytics_collection_enabled", "firebase_sessions_enabled"
    };

    public static Set<Pair<String, Set<String>>> compatibility() {
        return Collections.singleton(new Pair<String, Set<String>>(PACKAGE, Collections.singleton(VERSION)));
    }
    public interface Lookup { ClassDef get(String owner); }
    public interface Editor { MutableClass get(String owner); }
    public static void require(boolean ok, String reason) {
        if (!ok) throw new IllegalStateException("SwiftKey telemetry: " + reason);
    }
    public static void validatePackage(String p, String v, String code) {
        require(PACKAGE.equals(p) && VERSION.equals(v) && CODE.equals(code),
            "Requires SwiftKey Beta 9.13.16.4 (1236271168); normal SwiftKey and other builds are unsupported");
    }
    public static String descriptor(MethodReference method) {
        StringBuilder out = new StringBuilder(method.getDefiningClass()).append("->").append(method.getName()).append('(');
        for (CharSequence p : method.getParameterTypes()) out.append(p);
        return out.append(')').append(method.getReturnType()).toString();
    }
    public static String owner(String method) { return method.substring(0, method.indexOf("->")); }
    public static Method find(ClassDef clazz, String target) {
        require(clazz != null, "Missing class for " + target);
        Method found = null;
        for (Method method : clazz.getMethods()) if (target.equals(descriptor(method))) {
            require(found == null, "Ambiguous method " + target); found = method;
        }
        require(found != null, "Missing method " + target); return found;
    }
    public static String fingerprint(Method method) {
        try {
            MethodImplementation body = method.getImplementation();
            require(body != null, "Missing body " + descriptor(method));
            ImmutableMethodImplementation normalized = new ImmutableMethodImplementation(body.getRegisterCount(),
                body.getInstructions(), body.getTryBlocks(), Collections.emptyList());
            ImmutableMethod copy = new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                method.getReturnType(), method.getAccessFlags(), method.getAnnotations(), method.getHiddenApiRestrictions(), normalized);
            ImmutableClassDef wrapper = new ImmutableClassDef(method.getDefiningClass(), 1, "Ljava/lang/Object;",
                Collections.emptyList(), null, Collections.emptySet(), Collections.emptyList(), Collections.singleton(copy));
            MemoryDataStore store = new MemoryDataStore();
            try {
                DexPool.writeTo(store, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(wrapper)));
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(store.getBuffer(), store.getSize()));
                StringBuilder out = new StringBuilder();
                for (byte v : hash) out.append(String.format(Locale.ROOT, "%02x", v & 255));
                return out.toString();
            } finally { store.close(); }
        } catch (Exception error) { throw new IllegalStateException("Cannot fingerprint SwiftKey telemetry boundary", error); }
    }
    public static void validateContract(Method method, int index) {
        require(CONTRACTS[index].equals(descriptor(method)), "Wrong boundary " + index);
        require(method.getAccessFlags() == ACCESS[index], "Boundary access changed " + CONTRACTS[index]);
        require(Arrays.asList(HASHES[index]).contains(fingerprint(method)), "Unverified or already modified boundary " + CONTRACTS[index]);
    }
    private static void requireSingleton(Lookup lookup, String type) {
        ClassDef clazz = lookup.get(type); require(clazz != null, "Missing return type " + type);
        int matches = 0;
        for (Field f : clazz.getFields()) if ("a".equals(f.getName()) && type.equals(f.getType())) {
            require((f.getAccessFlags() & 25) == 25, "Return singleton is not public static final " + type); matches++;
        }
        require(matches == 1, "Missing or ambiguous return singleton " + type);
    }
    public static int adjustCallIndex(Method method) {
        return staticCallIndex(method, ADJUST_INIT);
    }
    private static int staticCallIndex(Method method, String target) {
        int index = 0, result = -1;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            if (instruction instanceof ReferenceInstruction) {
                Reference ref = ((ReferenceInstruction) instruction).getReference();
                if (ref instanceof MethodReference && target.equals(descriptor((MethodReference) ref))) {
                    require(result == -1, "Multiple boundary calls " + target);
                    require(instruction.getOpcode() == Opcode.INVOKE_STATIC || instruction.getOpcode() == Opcode.INVOKE_STATIC_RANGE,
                        "Unexpected boundary invocation opcode " + target);
                    result = index;
                }
            }
            index++;
        }
        require(result >= 0, "Boundary call is missing " + target); return result;
    }
    public static int adjustWaitCallIndex(Method method) {
        int index = staticCallIndex(method, ADJUST_WAIT);
        List<Instruction> instructions = new ArrayList<Instruction>();
        for (Instruction ins : method.getImplementation().getInstructions()) instructions.add(ins);
        require(index > 0 && index + 1 < instructions.size(), "Unexpected Adjust wait location");
        Instruction previous = instructions.get(index - 1);
        require(previous instanceof ReferenceInstruction
                && ((ReferenceInstruction) previous).getReference() instanceof MethodReference
                && "Lb40/d;-><init>(ILjava/lang/Object;Lrl0/c;)V".equals(
                    descriptor((MethodReference) ((ReferenceInstruction) previous).getReference())),
            "Adjust wait coroutine construction changed");
        Opcode next = instructions.get(index + 1).getOpcode();
        require(next != Opcode.MOVE_RESULT && next != Opcode.MOVE_RESULT_OBJECT && next != Opcode.MOVE_RESULT_WIDE,
            "Adjust wait return value is now used");
        return index;
    }
    public static void validateNative(Lookup lookup) {
        for (int i = 0; i < CONTRACTS.length; i++) validateContract(find(lookup.get(owner(CONTRACTS[i])), CONTRACTS[i]), i);
        ClassDef records = lookup.get("Lwh0/j;");
        require((records.getAccessFlags() & 1024) != 0, "Telemetry dispatcher is no longer abstract");
        find(records, "Lwh0/j;-><init>(Ljava/util/Set;)V");
        requireSingleton(lookup, SUCCESS_OWNER); requireSingleton(lookup, UNIT_OWNER);
        require(lookup.get(SUCCESS_OWNER).getInterfaces().contains(SUCCESS_INTERFACE)
                || SUCCESS_INTERFACE.equals(lookup.get(SUCCESS_OWNER).getSuperclass()),
            "Primary uploader success type changed");
        Method unitName = find(lookup.get(UNIT_OWNER), UNIT_OWNER + "->toString()Ljava/lang/String;");
        boolean unit = false;
        for (Instruction ins : unitName.getImplementation().getInstructions()) if (ins instanceof ReferenceInstruction) {
            Reference ref = ((ReferenceInstruction) ins).getReference();
            if (ref instanceof StringReference && "kotlin.Unit".equals(((StringReference) ref).getString())) unit = true;
        }
        require(unit, "Secondary uploader return type is no longer Kotlin Unit");
        adjustCallIndex(find(lookup.get(owner(CONTRACTS[3])), CONTRACTS[3]));
        adjustWaitCallIndex(find(lookup.get(owner(CONTRACTS[5])), CONTRACTS[5]));
    }
    public static void applyMethod(MutableMethod method, int index) {
        validateContract(method, index);
        MutableMethodImplementation body = method.getImplementation();
        require(body.getRegisterCount() > 0, "No return register available");
        switch (index) {
            case 0:
                // Even code-unit insertion retains all existing payload alignment.
                body.addInstruction(0, new BuilderInstruction10x(Opcode.RETURN_VOID));
                body.addInstruction(1, new BuilderInstruction10x(Opcode.NOP));
                break;
            case 1: case 2:
                String resultType = index == 1 ? SUCCESS_OWNER : UNIT_OWNER;
                body.addInstruction(0, new BuilderInstruction21c(Opcode.SGET_OBJECT, 0,
                    new ImmutableFieldReference(resultType, "a", resultType)));
                body.addInstruction(1, new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
                body.addInstruction(2, new BuilderInstruction10x(Opcode.NOP));
                break;
            case 3:
                // Preserve the rest of FullApplicationDelegate, including its attribution
                // state holder and all keyboard/account/shared initialization.
                body.replaceInstruction(adjustCallIndex(method), new BuilderInstruction10x(Opcode.NOP));
                break;
            case 4:
                // SharedPreferences can override a manifest false. Force only the SDK's
                // automatic-collection predicate through its normal disabled path.
                body.addInstruction(0, new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
                body.addInstruction(1, new BuilderInstruction11x(Opcode.RETURN, 0));
                break;
            case 5:
                // Only the Adjust-specific runBlocking call is removed. Otherwise
                // disabling its SDK causes a 30-second initial attribution wait.
                // The following Play InstallReferrer and pending deep link flow stay.
                body.replaceInstruction(adjustWaitCallIndex(method), new BuilderInstruction10x(Opcode.NOP));
                break;
            default: throw new IllegalArgumentException("Unknown telemetry boundary " + index);
        }
    }
    public static void apply(Lookup lookup, Editor editor) {
        validateNative(lookup);
        List<MutableMethod> methods = new ArrayList<MutableMethod>();
        for (int i = 0; i < CONTRACTS.length; i++) {
            MutableMethod method = (MutableMethod) find(editor.get(owner(CONTRACTS[i])), CONTRACTS[i]);
            validateContract(method, i); methods.add(method);
        }
        // Finish all preflight checks before changing any bytecode.
        for (int i = 0; i < methods.size(); i++) applyMethod(methods.get(i), i);
    }
    public static String androidAttribute(Element node, String name) {
        String value = node.getAttributeNS(ANDROID, name);
        return value.isEmpty() ? node.getAttribute("android:" + name) : value;
    }
    private static void setAndroid(Element node, String name, String value) {
        node.removeAttributeNS(ANDROID, name);
        node.removeAttribute("android:" + name);
        node.setAttributeNS(ANDROID, "android:" + name, value);
    }
    private static Element uniqueNamed(Element root, String tag, String name, boolean optional) {
        NodeList nodes = root.getElementsByTagName(tag); Element found = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (!name.equals(androidAttribute(node, "name"))) continue;
            require(found == null, "Duplicate manifest " + tag + " " + name); found = node;
        }
        require(optional || found != null, "Missing manifest " + tag + " " + name); return found;
    }
    public static void patchManifest(Document document) {
        Element root = document.getDocumentElement();
        require("manifest".equals(root.getTagName()) && PACKAGE.equals(root.getAttribute("package")), "Manifest package changed");
        NodeList apps = root.getElementsByTagName("application");
        require(apps.getLength() == 1, "Expected one manifest application");
        Element app = (Element) apps.item(0);
        List<Element> components = new ArrayList<Element>();
        for (String[] c : DISABLED_COMPONENTS) components.add(uniqueNamed(app, c[0], c[1], false));
        List<Element> metadata = new ArrayList<Element>();
        for (String name : DISABLED_METADATA) metadata.add(uniqueNamed(app, "meta-data", name, true));
        // The FCM listener, Firebase common initialization and multipurpose SwiftKeyJobService
        // are deliberately outside the only four disabled components above.
        for (Element node : components) setAndroid(node, "enabled", "false");
        for (int i = 0; i < DISABLED_METADATA.length; i++) {
            Element node = metadata.get(i);
            if (node == null) {
                node = document.createElement("meta-data");
                setAndroid(node, "name", DISABLED_METADATA[i]); app.appendChild(node);
            }
            // Avoid a preexisting resource-valued alternative silently overriding this value.
            node.removeAttributeNS(ANDROID, "resource"); node.removeAttribute("android:resource");
            setAndroid(node, "value", "false");
        }
    }
    public static void patchExceptionResource(Document document) {
        require("resources".equals(document.getDocumentElement().getTagName()), "Unexpected bool resource root");
        NodeList bools = document.getElementsByTagName("bool"); Element target = null;
        for (int i = 0; i < bools.getLength(); i++) {
            Element b = (Element) bools.item(i);
            if (!"exceptions_report_enabled".equals(b.getAttribute("name"))) continue;
            require(target == null, "Duplicate exceptions_report_enabled resource"); target = b;
        }
        require(target != null, "Missing exceptions_report_enabled resource");
        require("true".equals(target.getTextContent().trim()) || "false".equals(target.getTextContent().trim()),
            "Unexpected exceptions_report_enabled value");
        target.setTextContent("false");
    }
    private static final BytecodePatch GUARD = new BytecodePatch(null, null, false, compatibility(),
        Collections.<Patch<?>>emptySet(), Collections.emptySet(), null,
        new Function1<BytecodePatchContext, Unit>() { public Unit invoke(final BytecodePatchContext context) {
            validatePackage(context.getPackageMetadata().getPackageName(), context.getPackageMetadata().getVersionName(),
                context.getPackageMetadata().getVersionCode());
            validateNative(new Lookup() { public ClassDef get(String owner) { return context.classDefByOrNull(owner); }});
            return Unit.INSTANCE;
        }}, null);
    private static final ResourcePatch RESOURCES = new ResourcePatch(null, null, false, compatibility(),
        Collections.<Patch<?>>singleton(GUARD), Collections.emptySet(),
        new Function1<ResourcePatchContext, Unit>() { public Unit invoke(ResourcePatchContext context) {
            validatePackage(context.getPackageMetadata().getPackageName(), context.getPackageMetadata().getVersionName(),
                context.getPackageMetadata().getVersionCode());
            try (app.morphe.patcher.util.Document manifest = context.document("AndroidManifest.xml");
                 app.morphe.patcher.util.Document bools = context.document("res/values/bools.xml")) {
                patchManifest(manifest); patchExceptionResource(bools);
            }
            return Unit.INSTANCE;
        }}, null);
    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "SwiftKey Beta 9.13.16.4用の独立選択パッチ。Microsoft向けテレメトリの記録・送信、Adjust、Firebase Sessions、従来のGoogle Analytics、アプリ例外報告とCrashlyticsの自動送信を停止します。入力・変換・学習・辞書同期の処理は維持します。Crashlyticsの端末内記録は残ります。",
        false, compatibility(), Collections.<Patch<?>>singleton(RESOURCES), Collections.emptySet(), null,
        new Function1<BytecodePatchContext, Unit>() { public Unit invoke(final BytecodePatchContext context) {
            validatePackage(context.getPackageMetadata().getPackageName(), context.getPackageMetadata().getVersionName(),
                context.getPackageMetadata().getVersionCode());
            apply(new Lookup() { public ClassDef get(String owner) { return context.classDefByOrNull(owner); }},
                new Editor() { public MutableClass get(String owner) { return context.mutableClassDefBy(owner); }});
            return Unit.INSTANCE;
        }}, null);
}
