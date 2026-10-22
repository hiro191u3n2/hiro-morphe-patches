package app.hiro.berry.patches;

import app.morphe.patcher.patch.AppTarget;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.Compatibility;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Verified bytecode layouts, independent of the advertised version string.
 * A layout only supplies names. Every patch still verifies its complete original
 * method contracts and the fields it uses before editing the application.
 */
public final class BerryProfiles {
    public static final String PACKAGE = "jp.ejimax.berrybrowser";
    public static final String TOUCH = "Lcx;->onTouchEvent(Landroid/view/MotionEvent;)Z";
    public static final String INTERCEPT = "Lz31;->onInterceptTouchEvent(Landroid/view/MotionEvent;)Z";
    public static final String SHARE_VIEW = "Lb03;->onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V";
    public static final String SHARE_LIST = "Lj8;->q(Ljava/lang/Object;)Ljava/lang/Object;";
    public static final Compatibility TARGET = new Compatibility(PACKAGE, "Berry Browser",
        "バージョン表示による制限なし。適用時に検証済みのコード構造を確認します。",
        null, Integer.valueOf(6770852), null,
        Collections.singletonList(new AppTarget(null, false, null)));

    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private static final Profile OLD = oldProfile();
    private static final Profile CURRENT = currentProfile();

    private BerryProfiles() {}

    public static final class Profile {
        public final String id;
        public final int shareRegisters, shareDialogRegister, shareReturnRegister;
        private final Map<String, String> types = new LinkedHashMap<>();
        private final Map<String, String> members = new LinkedHashMap<>();
        private final Map<String, String> hashes = new LinkedHashMap<>();

        private Profile(String id, int registers, int dialog, int returned) {
            this.id = id;
            shareRegisters = registers;
            shareDialogRegister = dialog;
            shareReturnRegister = returned;
        }

        public String type(String descriptor) {
            int arrays = 0;
            while (arrays < descriptor.length() && descriptor.charAt(arrays) == '[') arrays++;
            String base = descriptor.substring(arrays);
            String mapped = types.get(base);
            return mapped == null ? descriptor : descriptor.substring(0, arrays) + mapped;
        }

        /** Rewrite one complete descriptor; exact member mappings precede type mappings. */
        public String ref(String value) {
            String exact = members.get(value);
            if (exact != null) return exact;
            StringBuilder out = new StringBuilder();
            int position = 0;
            while (position < value.length()) {
                int start = value.indexOf('L', position);
                if (start < 0) { out.append(value.substring(position)); break; }
                int end = value.indexOf(';', start);
                if (end < 0) { out.append(value.substring(position)); break; }
                out.append(value.substring(position, start));
                String token = value.substring(start, end + 1);
                out.append(types.containsKey(token) ? types.get(token) : token);
                position = end + 1;
            }
            return out.toString();
        }

        public String expectedHash(String logicalMethod) {
            String expected = hashes.get(logicalMethod);
            require(expected != null, "検証情報がありません: " + logicalMethod);
            return expected;
        }

        public MutableMethod checked(BytecodePatchContext context, String logicalMethod) {
            String descriptor = ref(logicalMethod);
            String owner = descriptor.substring(0, descriptor.indexOf("->"));
            require(context.classDefByOrNull(owner) != null, "対象クラスがありません: " + owner);
            MutableMethod found = null;
            for (MutableMethod method : context.mutableClassDefBy(owner).getMethods()) {
                if (!descriptor.equals(FORMAT.getMethodDescriptor(method))) continue;
                require(found == null, "対象処理が重複しています: " + descriptor);
                found = method;
            }
            verifyMethod(found, descriptor, expectedHash(logicalMethod));
            return found;
        }

        public Map<String, MutableMethod> checked(BytecodePatchContext context, String[][] logicalMethods) {
            Map<String, MutableMethod> result = new LinkedHashMap<>();
            for (String[] item : logicalMethods) result.put(item[0], checked(context, item[0]));
            return result;
        }

        public void field(BytecodePatchContext context, String logicalField) {
            String descriptor = ref(logicalField);
            verifyField(context.classDefByOrNull(descriptor.substring(0, descriptor.indexOf("->"))), descriptor);
        }
    }

    public static Profile select(BytecodePatchContext context) {
        return select(context.getPackageMetadata().getPackageName(), context::classDefByOrNull);
    }

    /** Exposed for focused contract tests; selection never uses versionName/versionCode. */
    public static Profile select(String packageName, Function<String, ClassDef> classes) {
        require(PACKAGE.equals(packageName), "Berry Browser の元 APK を選択してください");
        Profile found = null;
        for (Profile profile : new Profile[] { OLD, CURRENT }) {
            ClassDef web = classes.apply(profile.type("Lcx;"));
            if (web == null || !"Landroid/webkit/WebView;".equals(web.getSuperclass())) continue;
            require(found == null, "検証済みの構造が複数見つかりました");
            found = profile;
        }
        require(found != null, "未検証の WebView 構造です。対応コードの追加が必要です");
        return found;
    }

    public static void verifyMethod(Method method, String descriptor, String expectedHash) {
        require(method != null && method.getImplementation() != null, "対象処理がありません: " + descriptor);
        require(descriptor.equals(FORMAT.getMethodDescriptor(method)), "対象処理の型が異なります: " + descriptor);
        require(expectedHash.equals(MethodContract.sha256(method)),
            "検証済みの元コードと一致しません: " + descriptor + "。未改造の対応 APK が必要です");
    }

    public static void verifyField(ClassDef owner, String descriptor) {
        require(owner != null, "対象フィールドのクラスがありません: " + descriptor);
        int count = 0;
        for (Field field : owner.getFields()) {
            if (!descriptor.equals(FORMAT.getFieldDescriptor(field))) continue;
            require((field.getAccessFlags() & 9) == 1, "対象フィールドの公開・インスタンス属性が異なります: " + descriptor);
            count++;
        }
        require(count == 1, "検証済みのフィールド構造と一致しません: " + descriptor);
    }

    public static int unique(MutableMethod method, String descriptor) {
        List<? extends Instruction> instructions = method.getImplementation().getInstructions();
        int found = -1;
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (!(instruction instanceof ReferenceInstruction) || !descriptor.equals(
                FORMAT.getReference(((ReferenceInstruction) instruction).getReference()))) continue;
            require(found < 0, "対象参照が重複しています: " + descriptor);
            found = i;
        }
        require(found >= 0, "対象参照がありません: " + descriptor);
        return found;
    }

    public static InputStream extension(Class<?> provider, String path) {
        InputStream stream = provider.getClassLoader().getResourceAsStream(path);
        require(stream != null, "実行部品がありません: " + path);
        return stream;
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Berry パッチを中止: " + message);
    }

    private static Profile oldProfile() {
        Profile p = new Profile("verified-383740-layout", 19, 6, 5);
        for (String[] item : OriginalContracts.METHODS) p.hashes.put(item[0], item[1]);
        p.hashes.put(SHARE_VIEW, "2a3f1cf6e33a1d05a7591716f9a799974eae34bcb256563fd8ec6ab5e9f973e7");
        p.hashes.put(SHARE_LIST, "b58bc4c2d4d2a87cc7f1bb9e65c3cd53cb3dbe09323320aa7e79e69ccd804377");
        p.hashes.put("Lb03;->X()Lhc1;", "f4edd5e48f185d4a2788441385939726d1cf84ad389799ee8c7f5774778c6a86");
        p.hashes.put("Lfy;->O(Ljava/util/List;)V", "0eb6276f02ca7a1af67f99dcd97c1db3be1c02422285c70004600c2f0062d611");
        p.hashes.put(TOUCH, "96221545207825e28efab19481dd0f8402170b29dcb0ee1c30396fbb3927738f");
        p.hashes.put(INTERCEPT, "5e570281233f57105c101b4518eacadcd613fc69ad94530b82a35a16b7e90a56");
        return p;
    }

    private static Profile currentProfile() {
        Profile p = new Profile("verified-383931-layout", 20, 7, 6);
        String[][] types = {
            {"Lcx;", "Lpx;"}, {"Lc41;", "Lu51;"}, {"Lz31;", "Lr51;"},
            {"Lg60;", "Lz60;"}, {"Lc70;", "Lt70;"}, {"Ltt5;", "Lxw5;"},
            {"Lb03;", "Lz13;"}, {"Lhc1;", "Lxd1;"}, {"Lmo0;", "Lvp0;"},
            {"Lz32;", "Lr52;"}, {"Lfy;", "Luy;"}, {"Lal4;", "Lun4;"},
            {"Ltp0;", "Lcr0;"}, {"Lyx0;", "Lhz0;"}
        };
        for (String[] item : types) p.types.put(item[0], item[1]);
        String[][] members = {
            {"Lz31;->h0:Lc41;", "Lr51;->j0:Lu51;"},
            {"Lc41;->q:Lcx;", "Lu51;->r:Lpx;"},
            {"Lg60;->l:B", "Lz60;->n:B"},
            {"Lg60;->m:Ltt5;", "Lz60;->o:Lxw5;"},
            {"Ltt5;->q:Landroid/widget/FrameLayout;", "Lxw5;->s:Landroid/widget/FrameLayout;"},
            {"Lc41;->t0()V", "Lu51;->s0()V"}, {"Lc41;->m()V", "Lu51;->n()V"},
            {"Lc41;->r(Landroid/os/Bundle;)V", "Lu51;->s(Landroid/os/Bundle;)V"},
            {"Lc41;->h(Landroid/os/Bundle;)Z", "Lu51;->i(Landroid/os/Bundle;)Z"},
            {"Lb03;->M0:Lz32;", "Lz13;->O0:Lr52;"}
        };
        for (String[] item : members) p.members.put(item[0], item[1]);
        String[][] hashes = {
            {TOUCH, "e5b3ee9e4d60a46fe330873fde3c9110ce77322fc2989a483deb4d772ba96ae2"},
            {INTERCEPT, "bf3b1c7e747be0103ebca1072d2639b448f863ed7dc9022ed6ee2642c56c65c9"},
            {"Lg60;->run()V", "076a5475e0e993892c74a5005ed2007d349d7dcd4e8d5f10ff88b2f2561066ba"},
            {"Lcx;->onScrollChanged(IIII)V", "dc44a7b3b9d183f9a2f80c7a8eb4125a5b16b720c1a6f762345f589c1271ae9e"},
            {"Lc70;->onPageStarted(Landroid/webkit/WebView;Ljava/lang/String;Landroid/graphics/Bitmap;)V", "3dc8253397f11137b73d54532176e1c720cae711bfee4f53982279eda165c0a1"},
            {"Lc70;->onPageFinished(Landroid/webkit/WebView;Ljava/lang/String;)V", "a9473a0c71e22e39f5bb3f03449d73147f385aab979ae607e8e4f45a423974ca"},
            {"Lc41;->S(Ljava/lang/String;)V", "b61183e7b37501043d45cc40a521e481a3c7e27c1f38ae1ea75daa45b3954684"},
            {"Lc41;->V()V", "73920aad4f5bca3cc2baacdcb9f89de26263dd94e5a6daeb6ff35a2bc0ac1858"},
            {"Lc41;->t0()V", "b09e96c16aaf9868393ba3809541f798bce8ba294eadff147b706ab941514544"},
            {"Lc41;->m()V", "400b6037e2930d7ed24cb949f0b710fee4c29613af52301c1325e82021e8c28c"},
            {"Lc41;->destroy()V", "8661e68f8301f0dee706693b06f0de9c43bcb25874bbcddef5022378b2d54561"},
            {"Lc41;->r(Landroid/os/Bundle;)V", "fb5dedc4822ef58070fcb5266c46f4ce5df3b50855ecf8c96a9c5b6261a6efa0"},
            {"Lc41;->h(Landroid/os/Bundle;)Z", "670868dc19dc2e50b08312d5847cd4a0cb698454e67df952d8c659df3239c74c"},
            {SHARE_VIEW, "697b550a19a890c447e36e112e799f0d67b674b0a76d7911b15c6da3c18ac40f"},
            {SHARE_LIST, "3a9c2fd869b6bad0cda3eec7d70c91e2d80a14dd5fa5ad454229c2e6082aed9e"},
            {"Lb03;->X()Lhc1;", "5c25e6f96e4d8c45a5191729bfbafef47d8818b1936b2af7e93cdd6627250f56"},
            {"Lfy;->O(Ljava/util/List;)V", "5470128bcd898394885cef29c85067c3480f5cab7cfb575aa7148f1712482fc8"}
        };
        for (String[] item : hashes) p.hashes.put(item[0], item[1]);
        return p;
    }
}
