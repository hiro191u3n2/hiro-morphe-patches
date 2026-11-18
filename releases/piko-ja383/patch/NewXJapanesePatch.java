package app.hiro.newx.patches;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.*;
import kotlin.Unit;
import app.morphe.patcher.PatcherContext;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Japanese Android resources and explicitly reviewed Piko UI literals; no new runtime. */
public final class NewXJapanesePatch {
    public static final String NAME = "NewX: Pikoの設定・メニューを日本語化";
    public static final String VERSION = "12.43.0-prod.01";
    public static final String RESOURCE_UTILS = "Lapp/morphe/extension/shared/ResourceUtils;";
    public static final String SETTINGS_HOST = "Lapp/morphe/extension/newx/settings/NewXSettingsHost;";
    public static final String SETTINGS_ACTIVITY = "Lapp/morphe/extension/newx/settings/NewXSettingsActivity;";
    private static final String STRING = "Ljava/lang/String;";
    private static final String[] REQUIRED_DEPENDENCIES = {
        "SettingsPatch.newXSettingsPatch", "SettingsResourcePatch.newXSettingsResourcePatch",
        "ExtensionPatch.newXExtensionPatch", "AddResourcesPatch.addResourcesPatch"
    };
    public static final Patch PATCH = PatchKt.patch(NAME,
        "X 12.43.0-prod.01・Piko 3.12.0-dev.1/dev.2/dev.3のNewX設定と追加メニューを日本語化。日本語以外の表示と設定値を維持。PikoのNewX項目と一緒に選択してください。",
        false, builder -> {
            builder.compatibleWith(new Compatibility("com.twitter.android", "NewX", "Piko NewXと同時適用", ApkFileType.APKM,
                "#000000", null, Collections.singletonList(new AppTarget(VERSION, Collections.emptyMap(), false, null, null)), FormFactor.MOBILE));
            builder.execute(() -> {
                require("com.twitter.android".equals(PatchFunctionsKt.getPackageMetadata().getPackageName())
                    && VERSION.equals(PatchFunctionsKt.getPackageMetadata().getVersionName()), "X 12.43.0-prod.01の未改造APKMを使用してください");
                validateSelection(PatcherContext.Companion.getCurrent$morphe_patcher().getExecutablePatches$morphe_patcher());
                return Unit.INSTANCE;
            });
            builder.finalize(() -> {
                validateSelection(PatcherContext.Companion.getCurrent$morphe_patcher().getExecutablePatches$morphe_patcher());
                final BytecodePatchContext context = PatchFunctionsKt.getCurrentBytecode();
                Editor editor = new Editor() {
                    public ClassDef get(String type) { return context.classDefByOrNull(type); }
                    public MutableClass mutable(String type) { return context.mutableClassDefBy(type); }
                };
                Plan plan = prepare(editor);
                // ARSCLib discovers new string IDs from standard strings.xml filenames.
                // Piko's later resource finalizer appends its names to these files, preserving ours.
                Map<File, byte[]> merged = new LinkedHashMap<File, byte[]>();
                for (String path : NewXJapaneseData.RESOURCE_PATHS) {
                    String destination = path.startsWith("res/values-ja/")
                        ? "res/values-ja/strings.xml" : "res/values/strings.xml";
                    File file = PatchFunctionsKt.resourceFile(destination, true);
                    byte[] before = merged.get(file);
                    if (before == null) {
                        try { before = file.exists() ? Files.readAllBytes(file.toPath())
                            : "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>\n".getBytes(StandardCharsets.UTF_8);
                        } catch (IOException e) { throw new IllegalStateException("既存の文字列リソースを読み込めません", e); }
                    }
                    merged.put(file, mergeStrings(before, readAsset("/newx-ja/" + path)));
                }
                apply(editor, plan);
                for (Map.Entry<File,byte[]> entry : merged.entrySet()) {
                    try {
                        Files.createDirectories(entry.getKey().toPath().getParent());
                        Files.write(entry.getKey().toPath(), entry.getValue(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException e) { throw new IllegalStateException("NewX日本語リソースを書き込めません", e); }
                }
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });

    private NewXJapanesePatch() {}
    public interface Editor { ClassDef get(String type); MutableClass mutable(String type); }
    public static final class Target {
        public final String owner, name, result, source, resource;
        public final List<String> parameters;
        public final int count;
        public Target(String owner, String name, String[] parameters, String result, String source, String resource, int count) {
            this.owner=owner; this.name=name; this.parameters=Arrays.asList(parameters); this.result=result;
            this.source=source; this.resource=resource; this.count=count;
        }
        public String methodKey() { return owner+"->"+name+"("+String.join("", parameters)+")"+result; }
    }
    public static final class Edit {
        public final Target target;
        public final int index, register;
        Edit(Target target, int index, int register) { this.target=target;this.index=index;this.register=register; }
    }
    public static final class Plan {
        public final List<Edit> edits = new ArrayList<Edit>();
        public final Set<String> methods = new LinkedHashSet<String>();
    }
    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("NewX日本語化: " + message);
    }
    private static byte[] readAsset(String path) {
        try (InputStream stream = NewXJapanesePatch.class.getResourceAsStream(path)) {
            require(stream != null, "翻訳データがありません: " + path);
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int n;
            while ((n=stream.read(buffer)) >= 0) out.write(buffer, 0, n);
            return out.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("翻訳データを読み込めません", e); }
    }
    private static Set<String> stringNames(byte[] xml, boolean additions) {
        try {
            String text = new String(xml, StandardCharsets.UTF_8);
            require(!text.contains("<!DOCTYPE"), "文字列XMLにDOCTYPEは使用できません");
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml));
            require("resources".equals(document.getDocumentElement().getTagName()), "不正な文字列XMLです");
            Set<String> names = new HashSet<String>();
            NodeList children = document.getDocumentElement().getChildNodes();
            for (int i=0; i<children.getLength(); i++) {
                Node child = children.item(i);
                if (!(child instanceof Element)) continue;
                Element element = (Element)child;
                if (additions) require("string".equals(element.getTagName()), "文字列以外のリソース追加はできません");
                if (!"string".equals(element.getTagName())) continue;
                String name = element.getAttribute("name");
                require(!name.isEmpty() && names.add(name), "重複する文字列リソース: " + name);
            }
            return names;
        } catch (Exception e) { throw new IllegalStateException("NewX日本語化: 文字列XMLを検証できません", e); }
    }
    /** Append only new string elements; preserve all original XML bytes and Android escapes. */
    public static byte[] mergeStrings(byte[] original, byte[] additions) {
        Set<String> existing = stringNames(original, false), added = stringNames(additions, true);
        require(!added.isEmpty(), "翻訳リソースが空です");
        for (String name : added) require(!existing.contains(name), "日本語化の重複適用または既存翻訳との競合: " + name);
        String before = new String(original, StandardCharsets.UTF_8), extra = new String(additions, StandardCharsets.UTF_8);
        int close = before.lastIndexOf("</resources>"), opening = extra.indexOf("<resources"), end = extra.lastIndexOf("</resources>");
        int start = opening < 0 ? -1 : extra.indexOf('>', opening) + 1;
        require(close >= 0 && start > 0 && end >= start, "文字列XMLのルート境界が見つかりません");
        return (before.substring(0,close) + extra.substring(start,end) + before.substring(close)).getBytes(StandardCharsets.UTF_8);
    }
    private static void visit(Patch patch, Set<Patch> seen, List<Patch> patches) {
        if (!seen.add(patch)) return;
        patches.add(patch);
        for (Patch dependency : patch.getDependencies()) visit(dependency, seen, patches);
    }
    public static void validateSelection(Iterable<Patch> selected) {
        Set<Patch> seen = Collections.newSetFromMap(new IdentityHashMap<Patch,Boolean>());
        List<Patch> patches = new ArrayList<Patch>();
        for (Patch patch : selected) visit(patch, seen, patches);
        PatchBundleInfo piko = null;
        for (String declared : REQUIRED_DEPENDENCIES) {
            List<Patch> matches = new ArrayList<Patch>();
            for (Patch patch : patches) if (declared.equals(patch.getDeclaredAs$morphe_patcher())) matches.add(patch);
            require(matches.size()==1, "Piko 3.12.0-dev.1/dev.2/dev.3のNewX設定を追加する項目と一緒に選択してください（" + declared + "）");
            PatchBundleInfo bundle = matches.get(0).getBundle$morphe_patcher();
            require(bundle != null && "Piko".equals(bundle.getName())
                && ("3.12.0-dev.1".equals(bundle.getVersion()) || "3.12.0-dev.2".equals(bundle.getVersion())
                    || "3.12.0-dev.3".equals(bundle.getVersion())),
                "未検証のPikoソースです（選択: " + (bundle == null ? "不明" : bundle.getName() + " " + bundle.getVersion())
                    + "、対応: Piko 3.12.0-dev.1/dev.2/dev.3）");
            if (piko == null) piko = bundle;
            else require(piko.equals(bundle), "複数のPikoソースが同時に選択されています");
        }
    }
    private static Method method(ClassDef type, Target target) {
        require(type != null, "Pikoの対象クラスがありません: " + target.owner);
        Method found = null;
        for (Method candidate : type.getMethods()) {
            if (candidate.getName().equals(target.name) && candidate.getReturnType().equals(target.result)
                    && candidate.getParameterTypes().toString().equals(target.parameters.toString())) {
                require(found == null, "対象メソッドが重複しています: " + target.methodKey()); found = candidate;
            }
        }
        require(found != null && found.getImplementation() != null, "Pikoの対象メソッドがありません: " + target.methodKey());
        return found;
    }
    public static Target[] targets() { return NewXJapaneseData.targets(); }
    public static Plan prepare(Editor editor) {
        require(editor.get(SETTINGS_HOST) != null && editor.get(SETTINGS_ACTIVITY) != null,
            "PikoのNewX設定が適用されていません。PikoのNewX項目を選択してください");
        ClassDef utils = editor.get(RESOURCE_UTILS);
        Target lookup = new Target(RESOURCE_UTILS, "getStringOrThrow", new String[]{STRING}, STRING, "", "", 0);
        Method getter = method(utils, lookup);
        require((utils.getAccessFlags() & 1) != 0 && (getter.getAccessFlags() & 9) == 9,
            "Pikoの文字列取得APIが変更されています");
        Plan plan = new Plan(); Set<String> locations = new HashSet<String>();
        for (Target target : targets()) {
            require(target.count > 0 && target.owner.startsWith("Lapp/morphe/extension/")
                && target.resource.startsWith("hiro_piko_newx_"), "翻訳対象データが不正です");
            Method current = method(editor.get(target.owner), target);
            int index = 0, found = 0;
            for (Instruction instruction : current.getImplementation().getInstructions()) {
                if ((instruction.getOpcode() == Opcode.CONST_STRING || instruction.getOpcode() == Opcode.CONST_STRING_JUMBO)
                    && target.source.equals(((StringReference)((ReferenceInstruction)instruction).getReference()).getString())) {
                    require(locations.add(target.methodKey()+"@"+index), "翻訳対象が重複しています");
                    int register = ((OneRegisterInstruction)instruction).getRegisterA();
                    require(register >= 0 && register <= 255, "文字列レジスタの範囲が変更されています");
                    plan.edits.add(new Edit(target,index,register)); found++;
                }
                index++;
            }
            require(found == target.count, "対象文字列の数が異なります: " + target.methodKey() + " expected=" + target.count + " actual=" + found);
            plan.methods.add(target.methodKey());
        }
        require(!plan.edits.isEmpty(), "翻訳するPiko UIがありません");
        return plan;
    }
    public static void apply(Editor editor, Plan plan) {
        // Descending instruction positions preserve every preflight match and original branch labels.
        List<Edit> edits = new ArrayList<Edit>(plan.edits);
        Collections.sort(edits, (a,b) -> {
            int method = a.target.methodKey().compareTo(b.target.methodKey());
            return method != 0 ? method : Integer.compare(b.index,a.index);
        });
        ImmutableMethodReference lookup = new ImmutableMethodReference(RESOURCE_UTILS, "getStringOrThrow", Collections.singletonList(STRING), STRING);
        for (Edit edit : edits) {
            MutableMethod mutable = (MutableMethod)method(editor.mutable(edit.target.owner), edit.target);
            MutableMethodImplementation code = mutable.getImplementation();
            Instruction instruction = code.getInstructions().get(edit.index);
            ImmutableStringReference resource = new ImmutableStringReference(edit.target.resource);
            if (instruction.getOpcode() == Opcode.CONST_STRING_JUMBO)
                code.replaceInstruction(edit.index, new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO, edit.register, resource));
            else code.replaceInstruction(edit.index, new BuilderInstruction21c(Opcode.CONST_STRING, edit.register, resource));
            code.addInstruction(edit.index+1, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, edit.register, 1, lookup));
            code.addInstruction(edit.index+2, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, edit.register));
        }
    }
}
