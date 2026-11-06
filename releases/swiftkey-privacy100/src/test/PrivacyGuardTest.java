import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import app.hiro.swiftkeyprivacy.patches.SwiftKeyPrivacyPatch;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.iface.*;

/** Fail-closed checks against the supplied APK. No Android execution is claimed. */
public final class PrivacyGuardTest {
    private static int checks;
    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }
    private static void rejected(Runnable action, String why) {
        try { action.run(); }
        catch (IllegalStateException expected) { checks++; return; }
        throw new AssertionError(why);
    }
    private static Map<String, ClassDef> read(String apk) throws Exception {
        Map<String, ClassDef> classes = new HashMap<String, ClassDef>();
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(new File(apk), Opcodes.getDefault());
        for (String entry : container.getDexEntryNames())
            for (ClassDef clazz : container.getEntry(entry).getDexFile().getClasses())
                if (classes.put(clazz.getType(), clazz) != null) throw new AssertionError("Duplicate class " + clazz.getType());
        check(!classes.isEmpty(), "Empty APK");
        return classes;
    }
    private static SwiftKeyPrivacyPatch.Lookup lookup(final Map<String, ClassDef> classes) {
        return new SwiftKeyPrivacyPatch.Lookup() {
            public ClassDef get(String owner) { return classes.get(owner); }
        };
    }
    private static Method method(Map<String, ClassDef> classes, int index) {
        String contract = SwiftKeyPrivacyPatch.CONTRACTS[index];
        return SwiftKeyPrivacyPatch.find(classes.get(SwiftKeyPrivacyPatch.owner(contract)), contract);
    }
    private static Document xml(String contents) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(contents)));
    }
    private static String manifest(String components) {
        return "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"com.touchtype.swiftkey.beta\">"
            + "<application>" + components + "</application></manifest>";
    }
    private static String components() {
        return "<receiver android:name=\"com.google.android.gms.analytics.AnalyticsReceiver\"/>"
            + "<service android:name=\"com.google.android.gms.analytics.AnalyticsService\"/>"
            + "<service android:name=\"com.google.android.gms.analytics.AnalyticsJobService\"/>"
            + "<provider android:name=\"com.adjust.sdk.SystemLifecycleContentProvider\"/>";
    }
    private static void resourceGuards() throws Exception {
        final Document missing = xml(manifest(components().replace(
            "<service android:name=\"com.google.android.gms.analytics.AnalyticsService\"/>", "")));
        rejected(() -> SwiftKeyPrivacyPatch.patchManifest(missing), "Missing legacy Analytics service was accepted");
        final Document duplicate = xml(manifest(components()
            + "<provider android:name=\"com.adjust.sdk.SystemLifecycleContentProvider\"/>"));
        rejected(() -> SwiftKeyPrivacyPatch.patchManifest(duplicate), "Duplicate Adjust provider was accepted");
        final Document metadata = xml(manifest(components()
            + "<meta-data android:name=\"firebase_sessions_enabled\" android:value=\"true\"/>"
            + "<meta-data android:name=\"firebase_sessions_enabled\" android:value=\"false\"/>"));
        rejected(() -> SwiftKeyPrivacyPatch.patchManifest(metadata), "Ambiguous Sessions override was accepted");
        final Document wrong = xml(manifest(components()).replace("com.touchtype.swiftkey.beta", "com.touchtype.swiftkey"));
        rejected(() -> SwiftKeyPrivacyPatch.patchManifest(wrong), "Normal SwiftKey manifest was accepted");
        final Document noBool = xml("<resources><bool name=\"other\">true</bool></resources>");
        rejected(() -> SwiftKeyPrivacyPatch.patchExceptionResource(noBool), "Missing exception flag was accepted");
        final Document duplicateBool = xml("<resources><bool name=\"exceptions_report_enabled\">true</bool>"
            + "<bool name=\"exceptions_report_enabled\">false</bool></resources>");
        rejected(() -> SwiftKeyPrivacyPatch.patchExceptionResource(duplicateBool), "Ambiguous exception flag was accepted");
        final Document unsupportedBool = xml("<resources><bool name=\"exceptions_report_enabled\">@bool/other</bool></resources>");
        rejected(() -> SwiftKeyPrivacyPatch.patchExceptionResource(unsupportedBool), "Unverified exception alias was accepted");
    }
    public static void main(String[] args) throws Exception {
        String apk = args.length == 1 ? args[0] : args.length == 2 && args[0].equals("guards") ? args[1] : null;
        if (apk == null) throw new IllegalArgumentException("PrivacyGuardTest [guards] <base.apk>");
        SwiftKeyPrivacyPatch.validatePackage("com.touchtype.swiftkey.beta", "9.13.16.4", "1236271168");
        rejected(() -> SwiftKeyPrivacyPatch.validatePackage("com.touchtype.swiftkey", "9.13.16.4", "1236271168"), "Normal SwiftKey accepted");
        rejected(() -> SwiftKeyPrivacyPatch.validatePackage("com.example.other", "9.13.16.4", "1236271168"), "Unrelated package accepted");
        rejected(() -> SwiftKeyPrivacyPatch.validatePackage("com.touchtype.swiftkey.beta", "9.13.13.5", "1236271168"), "Different version accepted");
        rejected(() -> SwiftKeyPrivacyPatch.validatePackage("com.touchtype.swiftkey.beta", "9.13.16.4", "1236271169"), "Different version code accepted");
        final Map<String, ClassDef> original = read(apk);
        SwiftKeyPrivacyPatch.validateNative(lookup(original));
        checks++;
        // Each real native boundary is independently tampered, then the complete
        // public apply entry point must reject it before requesting any editor.
        for (int n = 0; n < SwiftKeyPrivacyPatch.CONTRACTS.length; n++) {
            String contract = SwiftKeyPrivacyPatch.CONTRACTS[n];
            MutableClass altered = new MutableClass(original.get(SwiftKeyPrivacyPatch.owner(contract)));
            MutableMethod changed = (MutableMethod) SwiftKeyPrivacyPatch.find(altered, contract);
            changed.getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
            changed.getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
            final Map<String, ClassDef> drift = new HashMap<String, ClassDef>(original);
            drift.put(altered.getType(), altered);
            final boolean[] editing = {false};
            rejected(() -> SwiftKeyPrivacyPatch.apply(lookup(drift), new SwiftKeyPrivacyPatch.Editor() {
                public MutableClass get(String owner) { editing[0] = true; throw new AssertionError("Edit requested before all native guards passed"); }
            }), "Modified boundary accepted: " + contract);
            check(!editing[0], "Preflight edited a drifted APK");
        }
        // Successful application followed by a second invocation must fail without
        // adding a second prefix or changing another boundary.
        final Map<String, ClassDef> applied = new HashMap<String, ClassDef>(original);
        for (String contract : SwiftKeyPrivacyPatch.CONTRACTS) {
            String owner = SwiftKeyPrivacyPatch.owner(contract);
            if (!(applied.get(owner) instanceof MutableClass)) applied.put(owner, new MutableClass(applied.get(owner)));
        }
        SwiftKeyPrivacyPatch.Editor editor = new SwiftKeyPrivacyPatch.Editor() {
            public MutableClass get(String owner) { return (MutableClass) applied.get(owner); }
        };
        SwiftKeyPrivacyPatch.apply(lookup(applied), editor);
        final List<String> once = new ArrayList<String>();
        for (int n = 0; n < SwiftKeyPrivacyPatch.CONTRACTS.length; n++) once.add(SwiftKeyPrivacyPatch.fingerprint(method(applied, n)));
        rejected(() -> SwiftKeyPrivacyPatch.apply(lookup(applied), editor), "Already applied telemetry patch accepted");
        for (int n = 0; n < SwiftKeyPrivacyPatch.CONTRACTS.length; n++)
            check(once.get(n).equals(SwiftKeyPrivacyPatch.fingerprint(method(applied, n))), "Duplicate rejection changed boundary " + n);
        resourceGuards();
        System.out.println("PASS privacy guards: real APK native contracts, package/version/code rejection, all six boundary drift cases rejected before editing, repeat application rejected without partial changes, missing/ambiguous resource rejection; assertions=" + checks);
    }
}
