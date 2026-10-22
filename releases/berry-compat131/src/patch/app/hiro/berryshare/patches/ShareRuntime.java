package app.hiro.berryshare.patches;

import app.hiro.berry.patches.BerryProfiles;
import app.hiro.berry.patches.BerryProfiles.Profile;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableAnnotation;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableField;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.DualReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.Reference;
import com.android.tools.smali.dexlib2.iface.reference.TypeReference;
import com.android.tools.smali.dexlib2.iface.value.EncodedValue;
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;
import com.android.tools.smali.dexlib2.immutable.value.ImmutableStringEncodedValue;
import com.android.tools.smali.dexlib2.rewriter.DexRewriter;
import com.android.tools.smali.dexlib2.rewriter.EncodedValueRewriter;
import com.android.tools.smali.dexlib2.rewriter.Rewriter;
import com.android.tools.smali.dexlib2.rewriter.RewriterModule;
import com.android.tools.smali.dexlib2.rewriter.Rewriters;
import com.android.tools.smali.dexlib2.util.MethodUtil;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import kotlin.Unit;

/** Adapt only the five injected chooser classes; the MPE asset is retained verbatim. */
final class ShareRuntime {
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private static final String[] APP_TYPES = {"Lb03;", "Lhc1;", "Ln8;", "Lmo0;", "Lz32;", "Lfy;"};
    private static final String[] APP_FIELDS = {
        "Lb03;->M0:Lz32;", "Lhc1;->a:Landroid/widget/LinearLayout;", "Lhc1;->b:Landroid/widget/TextView;"
    };
    private static final String[] APP_METHODS = {
        "Lb03;->X()Lhc1;", "Lfy;->O(Ljava/util/List;)V",
        "Lmo0;->a()Landroid/content/ComponentName;", "Lmo0;->b()Ljava/lang/CharSequence;"
    };

    private ShareRuntime() {}

    static boolean isHelper(String type) {
        return "LHiroBerryBrowserChooser;".equals(type) || type.startsWith("LHiroBerryBrowserChooser$");
    }

    static void checkTargets(BytecodePatchContext context, Profile profile) {
        Map<String, ClassDef> none = new LinkedHashMap<>();
        for (String oldType : APP_TYPES) checkType(context, none, profile.type(oldType));
        for (String oldField : APP_FIELDS) profile.field(context, oldField);
        for (String oldMethod : APP_METHODS)
            checkMethod(context, none, profile.ref(oldMethod));
        profile.checked(context, "Lb03;->X()Lhc1;");
        profile.checked(context, "Lfy;->O(Ljava/util/List;)V");
        BerryProfiles.require(assignable(context, profile.type("Ln8;"), profile.type("Lmo0;"), new HashSet<>()),
            "共有先のインターフェース構造が異なります");
        BerryProfiles.require(assignable(context, profile.type("Lz32;"), profile.type("Lfy;"), new HashSet<>()),
            "共有一覧アダプターの継承構造が異なります");
    }

    static void adapt(BytecodePatchContext context, Profile profile) {
        DexRewriter rewriter = new DexRewriter(new RewriterModule() {
            @Override public Rewriter<String> getTypeRewriter(Rewriters rewriters) {
                return profile::type;
            }
            @Override public Rewriter<FieldReference> getFieldReferenceRewriter(Rewriters rewriters) {
                return field -> {
                    String mapped = profile.ref(FORMAT.getFieldDescriptor(field));
                    int arrow = mapped.indexOf("->"), colon = mapped.indexOf(':', arrow);
                    return new ImmutableFieldReference(mapped.substring(0, arrow), mapped.substring(arrow + 2, colon), mapped.substring(colon + 1));
                };
            }
            @Override public Rewriter<EncodedValue> getEncodedValueRewriter(Rewriters rewriters) {
                EncodedValueRewriter normal = new EncodedValueRewriter(rewriters);
                return value -> {
                    if (value instanceof StringEncodedValue) {
                        String text = ((StringEncodedValue) value).getValue();
                        String mapped = text;
                        // Generic-signature annotation strings also contain app types.
                        for (String type : APP_TYPES) mapped = mapped.replace(type, profile.type(type));
                        if (!mapped.equals(text)) return new ImmutableStringEncodedValue(mapped);
                    }
                    return normal.rewrite(value);
                };
            }
        });
        Map<String, ClassDef> helpers = new LinkedHashMap<>();
        context.classDefForEach(definition -> {
            if (isHelper(definition.getType())) {
                ClassDef snapshot = ImmutableClassDef.of(definition);
                ClassDef mapped = ImmutableClassDef.of(rewriter.getClassDefRewriter().rewrite(snapshot));
                helpers.put(mapped.getType(), mapped);
            }
            return Unit.INSTANCE;
        });
        BerryProfiles.require(helpers.size() == 5 && helpers.containsKey("LHiroBerryBrowserChooser;"),
            "共有先の実行部品が想定と異なります");
        // Validate the entire rewritten group before replacing even one definition.
        for (ClassDef helper : helpers.values()) validateHelper(context, helpers, helper);
        if (profile.type("Lb03;").equals("Lb03;")) return;
        for (ClassDef helper : helpers.values()) replace(context.mutableClassDefBy(helper.getType()), helper);
    }

    private static void replace(MutableClass target, ClassDef source) {
        target.setSuperClass(source.getSuperclass());
        target.getInterfaces().clear();
        target.getInterfaces().addAll(source.getInterfaces());
        target.getAnnotations().clear();
        for (Annotation annotation : source.getAnnotations()) target.getAnnotations().add(new MutableAnnotation(annotation));
        // Morphe caches the direct/virtual and static/instance subsets separately.
        // Refresh every view so no old descriptor can survive a prior lazy read.
        target.getMethods().clear();
        target.getDirectMethods().clear();
        target.getVirtualMethods().clear();
        for (Method method : source.getMethods()) {
            MutableMethod mutable = new MutableMethod(method);
            target.getMethods().add(mutable);
            (MethodUtil.isDirect(mutable) ? target.getDirectMethods() : target.getVirtualMethods()).add(mutable);
        }
        target.getFields().clear();
        target.getStaticFields().clear();
        target.getInstanceFields().clear();
        for (Field field : source.getFields()) {
            MutableField mutable = new MutableField(field);
            target.getFields().add(mutable);
            ((field.getAccessFlags() & 8) != 0 ? target.getStaticFields() : target.getInstanceFields()).add(mutable);
        }
    }

    private static void validateHelper(BytecodePatchContext context, Map<String, ClassDef> helpers, ClassDef helper) {
        checkType(context, helpers, helper.getSuperclass());
        for (String type : helper.getInterfaces()) checkType(context, helpers, type);
        for (Field field : helper.getFields()) checkType(context, helpers, field.getType());
        for (Method method : helper.getMethods()) {
            checkType(context, helpers, method.getReturnType());
            for (CharSequence type : method.getParameterTypes()) checkType(context, helpers, type.toString());
            if (method.getImplementation() == null) continue;
            for (Instruction instruction : method.getImplementation().getInstructions()) {
                if (instruction instanceof ReferenceInstruction)
                    checkReference(context, helpers, ((ReferenceInstruction) instruction).getReference());
                if (instruction instanceof DualReferenceInstruction)
                    checkReference(context, helpers, ((DualReferenceInstruction) instruction).getReference2());
            }
        }
    }

    private static void checkReference(BytecodePatchContext context, Map<String, ClassDef> helpers, Reference reference) {
        if (reference instanceof TypeReference) {
            checkType(context, helpers, ((TypeReference) reference).getType());
        } else if (reference instanceof MethodReference) {
            MethodReference method = (MethodReference) reference;
            checkType(context, helpers, method.getDefiningClass());
            checkType(context, helpers, method.getReturnType());
            for (CharSequence type : method.getParameterTypes()) checkType(context, helpers, type.toString());
            checkMethod(context, helpers, FORMAT.getMethodDescriptor(method));
        } else if (reference instanceof FieldReference) {
            FieldReference field = (FieldReference) reference;
            checkType(context, helpers, field.getDefiningClass());
            checkType(context, helpers, field.getType());
            if (!platform(field.getDefiningClass())) {
                ClassDef owner = lookup(context, helpers, field.getDefiningClass());
                Field resolved = null;
                for (Field candidate : owner.getFields())
                    if (FORMAT.getFieldDescriptor(field).equals(FORMAT.getFieldDescriptor(candidate))) resolved = candidate;
                BerryProfiles.require(resolved != null, "実行部品のフィールド参照を解決できません: " + FORMAT.getFieldDescriptor(field));
                if (!isHelper(owner.getType())) BerryProfiles.require((resolved.getAccessFlags() & 1) != 0,
                    "実行部品からアクセスできないフィールドです: " + FORMAT.getFieldDescriptor(field));
            }
        }
    }

    private static void checkMethod(BytecodePatchContext context, Map<String, ClassDef> helpers, String descriptor) {
        int arrow = descriptor.indexOf("->");
        String ownerType = descriptor.substring(0, arrow);
        if (platform(ownerType)) return;
        ClassDef owner = lookup(context, helpers, ownerType);
        Method found = null;
        for (Method method : owner.getMethods())
            if (descriptor.equals(FORMAT.getMethodDescriptor(method))) found = method;
        BerryProfiles.require(found != null, "実行部品のメソッド参照を解決できません: " + descriptor);
        if (!isHelper(ownerType)) BerryProfiles.require((found.getAccessFlags() & 9) == 1,
            "実行部品からアクセスできないメソッドです: " + descriptor);
    }

    private static ClassDef lookup(BytecodePatchContext context, Map<String, ClassDef> helpers, String type) {
        ClassDef definition = helpers.containsKey(type) ? helpers.get(type) : context.classDefByOrNull(type);
        BerryProfiles.require(definition != null, "実行部品の型参照を解決できません: " + type);
        return definition;
    }

    private static void checkType(BytecodePatchContext context, Map<String, ClassDef> helpers, String descriptor) {
        if (descriptor == null) return;
        while (descriptor.startsWith("[")) descriptor = descriptor.substring(1);
        if (descriptor.length() == 1 || platform(descriptor)) return;
        ClassDef definition = lookup(context, helpers, descriptor);
        if (!isHelper(descriptor)) BerryProfiles.require((definition.getAccessFlags() & 1) != 0,
            "実行部品からアクセスできない型です: " + descriptor);
    }

    private static boolean platform(String type) {
        return type.startsWith("Ljava/") || type.startsWith("Ljavax/")
            || type.startsWith("Landroid/") || type.startsWith("Ldalvik/");
    }

    private static boolean assignable(BytecodePatchContext context, String from, String to, Set<String> seen) {
        if (from.equals(to)) return true;
        if (!seen.add(from)) return false;
        ClassDef definition = context.classDefByOrNull(from);
        if (definition == null) return false;
        for (String implemented : definition.getInterfaces())
            if (assignable(context, implemented, to, seen)) return true;
        return definition.getSuperclass() != null && assignable(context, definition.getSuperclass(), to, seen);
    }
}
