import app.hiro.mapsstartup.patches.MapsStartupLocationPatch;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;

/**
 * Executes the provider's serialized DEX bridge, plus small native methods read
 * from the reported APK, against a controlled object graph. This is a host test,
 * not an Android renderer or device test. Native service boundaries are explicit:
 * fix/camera snapshots, distance calculation, camera dispatch, and callback queue.
 * Unknown instructions, calls and fields fail closed instead of faking success.
 */
public final class MapsStartupBridgeVmTest {
    private static final String OLD = "Lnce;->hiroTryCurrentLocation()I";
    private static final String NEW = "Lnce;->hiroTryCurrentLocation(Z)I";
    private static final String LAYOUT = "Lcom/google/android/apps/gmm/base/layout/MainLayout;";
    private static final String CONFIG = "Lbjoz;-><init>(Lbikz;FFFLbjox;Lbjoy;)V";
    private static final String PROJECT = "Lbjoz;->a(Lbjaw;F)Lbjow;";
    private static final Object WIDE_TAIL = new Object();
    private static final Map<String, Method> NATIVE = new LinkedHashMap<String, Method>();
    private static final Set<String> EXECUTED = new LinkedHashSet<String>();
    private static Method current;
    private static Method legacy;
    private static int assertions;
    private static int scenarios;

    private MapsStartupBridgeVmTest() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: MapsStartupBridgeVmTest <reported-v360-apk>");
        loadNative(new File(args[0]));
        current = serializedBridge();
        eq(NEW, desc(current), "emitted boolean protocol");
        regressions();
        readiness();
        acknowledgement();
        cameraArguments();
        for (String signature : Arrays.asList(OLD, NEW, "Lamcr;->f()V", "Lamcr;->g()V", "Lamcr;->h()Z",
                "Lbjaw;->v(Lbjaw;Lbjaw;D)Z", CONFIG, PROJECT, "Lamdp;->l(Lbjoz;Z)V",
                "Lamdp;->a()Lbjox;", "Lamdp;->b()Lbkmd;")) {
            check(EXECUTED.contains(signature), "actual method exercised: " + signature);
        }
        System.out.println("PASS emitted DEX bridge VM: scenarios=" + scenarios + " assertions=" + assertions
            + "; actual legacy failure reproduced, native dispatch/acknowledgement separated; Android UI execution not performed.");
    }

    private static void loadNative(File apk) throws Exception {
        Set<String> wanted = new LinkedHashSet<String>(Arrays.asList(OLD, "Lamcr;->f()V", "Lamcr;->g()V",
            "Lamcr;->h()Z", "Lbjaw;->v(Lbjaw;Lbjaw;D)Z", CONFIG, PROJECT, "Lamdp;->l(Lbjoz;Z)V",
            "Lamdp;->a()Lbjox;", "Lamdp;->b()Lbkmd;"));
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(apk, Opcodes.forApi(29));
        Set<String> owners = new HashSet<String>(Arrays.asList("Lnce;", "Lamcr;", "Lbjaw;", "Lbjoz;", "Lamdp;"));
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef c : container.getEntry(entry).getDexFile().getClasses()) {
                if (!owners.contains(c.getType())) continue;
                for (Method m : c.getMethods()) if (wanted.contains(desc(m))) {
                    check(NATIVE.put(desc(m), m) == null, "unique native method");
                }
            }
            if (NATIVE.keySet().containsAll(wanted)) break;
        }
        check(NATIVE.keySet().containsAll(wanted), "reported APK contains legacy bridge and native dependencies");
        legacy = NATIVE.remove(OLD);
        // Pin all dependencies already covered by production compatibility hashes.
        for (Method m : NATIVE.values()) {
            for (int n = 0; n < MapsStartupLocationPatch.CONTRACTS.length; n++) {
                if (desc(m).equals(MapsStartupLocationPatch.CONTRACTS[n])) {
                    MapsStartupLocationPatch.validateContract(m, n);
                }
            }
        }
    }

    private static Method serializedBridge() throws Exception {
        Method emitted = MapsStartupLocationPatch.bridge();
        ImmutableClassDef c = new ImmutableClassDef("Lnce;", 1, "Ljava/lang/Object;", Collections.<String>emptyList(),
            null, Collections.emptySet(), Collections.<Field>emptyList(), Collections.singleton(emitted));
        MemoryDataStore store = new MemoryDataStore();
        byte[] bytes;
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(c)));
            bytes = Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally { store.close(); }
        DexBackedDexFile dex = DexBackedDexFile.fromInputStream(Opcodes.forApi(29), new ByteArrayInputStream(bytes));
        for (ClassDef output : dex.getClasses()) for (Method m : output.getMethods()) if (NEW.equals(desc(m))) return m;
        throw new AssertionError("Serialized bridge missing");
    }

    private static void regressions() {
        scenario("legacy NONE cancels startup", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.mode("l");
            eq(-1, f.old(), "actual legacy bridge returns terminal on NONE");
            eq(0, f.run(true), "new bridge waits on transient NONE");
            f.mode("a");
            eq(2, f.run(true), "NONE-to-MAP becomes eligible");
            eq(0, f.run(false), "dispatch alone is not acknowledgement");
            f.render();
            eq(1, f.run(false), "rendered native target acknowledged");
        }});
        scenario("legacy queued callback reports success before camera moves", new Runnable() { public void run() {
            Fixture f = new Fixture();
            eq(1, f.old(), "actual legacy bridge claims success");
            eq(1, f.callbackSubmissions, "actual f/g reaches native callback boundary");
            eq(0, f.commands.size(), "legacy has issued no direct camera operation");
            check(f.cameraDistance() > 50, "legacy camera still away from fix");
        }});
        scenario("legacy existing tracking reports success without work", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.tracking("b");
            eq(1, f.old(), "legacy f returns success while already tracking");
            eq(0, f.callbackSubmissions, "actual native f skips callback when tracking");
            eq(0, f.commands.size(), "no camera request occurred");
            check(f.cameraDistance() > 50, "camera remains away");
            eq(2, f.run(true), "new bridge directly requests the target");
            eq(1, f.commands.size(), "one native command");
        }});
        scenario("new bridge never enters legacy permission callback path", new Runnable() { public void run() {
            Fixture f = new Fixture();
            eq(2, f.run(true), "request accepted");
            for (int i = 0; i < 8; i++) eq(0, f.run(false), "unrendered camera remains pending");
            eq(0, f.callbackSubmissions, "no permission callback queued");
            eq(0, f.legacyCalls, "no f/g/h route called by new bridge");
            eq(1, f.commands.size(), "read-only polls never duplicate request");
        }});
    }

    private interface Fault { void apply(Fixture f); }

    private static void missing(String name, final Fault fault) {
        scenario("waits for " + name, new Runnable() { public void run() {
            Fixture f = new Fixture(); fault.apply(f);
            eq(0, f.run(true), "missing dependency waits");
            eq(0, f.run(false), "read-only also waits");
            eq(0, f.commands.size(), "no camera command");
            eq(0, f.callbackSubmissions, "no callback");
        }});
    }

    private static void readiness() {
        missing("main layout", new Fault() { public void apply(Fixture f) { f.activity.put("Lnce;->bx:" + LAYOUT, null); }});
        missing("provider", new Fault() { public void apply(Fixture f) { f.layout.put(LAYOUT + "->i:Lcqpp;", null); }});
        missing("provider value", new Fault() { public void apply(Fixture f) { f.providerValue = null; }});
        missing("controller type", new Fault() { public void apply(Fixture f) { f.providerValue = new Node("Ljava/lang/Object;"); }});
        missing("resumed native controller", new Fault() { public void apply(Fixture f) { f.resumed = false; }});
        missing("camera controller", new Fault() { public void apply(Fixture f) { f.controller.put("Lamcr;->p:Lamdp;", null); }});
        missing("map engine", new Fault() { public void apply(Fixture f) { f.dp.put("Lamdp;->d:Lamej;", null); }});
        missing("map mode", new Fault() { public void apply(Fixture f) { f.mapMode = null; }});
        missing("initial NONE mode", new Fault() { public void apply(Fixture f) { f.mode("l"); }});
        missing("renderer", new Fault() { public void apply(Fixture f) { f.dp.put("Lamdp;->g:Lbizr;", null); }});
        missing("old camera controller", new Fault() { public void apply(Fixture f) { f.dp.put("Lamdp;->k:Lamds;", null); }});
        missing("new camera controller", new Fault() { public void apply(Fixture f) { f.dp.put("Lamdp;->f:Z", 1); f.dp.put("Lamdp;->l:Lamdt;", null); }});
        missing("viewport", new Fault() { public void apply(Fixture f) { f.dp.put("Lamdp;->p:Lbjja;", null); }});
        missing("valid fix", new Fault() { public void apply(Fixture f) { f.fixValid = false; }});
        missing("camera snapshot", new Fault() { public void apply(Fixture f) { f.snapshot = null; }});
        missing("tracking state", new Fault() { public void apply(Fixture f) { f.dp.put("Lamdp;->i:Lbkmd;", null); }});
        for (final String route : Arrays.asList("b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "n")) {
            scenario("route mode is terminal: " + route, new Runnable() { public void run() {
                Fixture f = new Fixture(); f.mode(route);
                eq(-1, f.run(true), "route excludes startup movement");
                eq(-1, f.run(false), "route also terminates pending confirmation");
                eq(0, f.commands.size(), "route never sends a camera command");
            }});
        }
        for (final String mode : Arrays.asList("a", "m")) {
            scenario("normal mode supports direct movement: " + mode, new Runnable() { public void run() {
                Fixture f = new Fixture(); f.mode(mode);
                eq(2, f.run(true), "normal map requests movement");
                f.render(); eq(1, f.run(false), "normal map acknowledges rendered target");
            }});
        }
        scenario("camera backend selector uses selected controller", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.dp.put("Lamdp;->f:Z", 1); f.dp.put("Lamdp;->k:Lamds;", null);
            eq(2, f.run(true), "new renderer does not require old renderer");
            Fixture g = new Fixture(); g.dp.put("Lamdp;->l:Lamdt;", null);
            eq(2, g.run(true), "old renderer does not require new renderer");
        }});
        scenario("fix can become ready after native controller", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.fixValid = false;
            eq(0, f.run(true), "invalid fix waits");
            f.fixValid = true;
            eq(2, f.run(true), "valid fix permits direct movement");
            check(f.copyFixCalls >= 2, "each query checks native fix validity");
        }});
    }

    private static void acknowledgement() {
        scenario("OFF with nearby cached target does not complete", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.near(0); f.zoom(16);
            eq(0, f.run(false), "nearby restored camera without tracking stays pending");
            eq(2, f.run(true), "tracking enabled even if cached camera already nearby");
            eq(1, f.commands.size(), "tracking request issued");
            eq(f.enumValue("Lbkmd;", "b"), f.dp.get("Lamdp;->i:Lbkmd;"), "location tracking selected");
            eq(1, f.run(false), "nearby target with tracking acknowledged");
        }});
        for (final String tracking : Arrays.asList("b", "c")) {
            scenario("already centered with tracking: " + tracking, new Runnable() { public void run() {
                Fixture f = new Fixture(); f.near(12); f.zoom(13); f.tracking(tracking);
                eq(1, f.run(false), "read-only confirmation");
                eq(1, f.run(true), "issue-enabled query still avoids redundant movement");
                eq(0, f.commands.size(), "no unnecessary native camera command");
            }});
        }
        scenario("camera request is distinct from renderer acknowledgement", new Runnable() { public void run() {
            Fixture f = new Fixture();
            eq(0, f.run(false), "read-only cannot issue");
            eq(2, f.run(true), "direct request returns issued");
            check(f.cameraDistance() > 50, "native dispatch boundary has not rendered yet");
            eq(0, f.run(false), "unrendered request cannot acknowledge");
            f.render();
            eq(1, f.run(false), "actual copied fix and camera target now match");
            eq(1, f.run(false), "repeated confirmation is read-only");
            eq(1, f.commands.size(), "confirmation has no command side effect");
        }});
        scenario("native distance tolerance is strictly less than fifty", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.tracking("b"); f.zoom(14);
            f.near(49.99); eq(1, f.run(false), "inside threshold acknowledges");
            f.near(50); eq(0, f.run(false), "exact boundary does not acknowledge");
            f.near(50.01); eq(0, f.run(false), "outside boundary does not acknowledge");
            f.near(Double.NaN); eq(0, f.run(false), "invalid distance does not acknowledge");
        }});
        scenario("camera target may be absent", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.tracking("b"); f.camera.put("Lbjjc;->a:Lbjaw;", null);
            eq(0, f.run(false), "native helper rejects missing target");
            eq(2, f.run(true), "camera can be moved from missing target");
        }});
        scenario("validity without copied point cannot falsely acknowledge", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.tracking("b"); f.fix = null;
            eq(0, f.run(false), "null copied point is not camera success");
        }});
        for (final float zoom : new float[] {0f, 12.999f, Float.NaN, Float.NEGATIVE_INFINITY}) {
            scenario("unusable or distant zoom requests useful scale", new Runnable() { public void run() {
                Fixture f = new Fixture(); f.tracking("b"); f.near(0); f.zoom(zoom);
                eq(0, f.run(false), "insufficient or NaN zoom cannot acknowledge");
                eq(2, f.run(true), "insufficient zoom sends a command");
                close(15f, f.last().floating("Lbjoz;->a:F"), "default useful zoom is 15");
                f.render(); eq(1, f.run(false), "new camera scale acknowledges");
            }});
        }
        scenario("fresh copied fix can invalidate previously centered camera", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.tracking("b"); f.near(0); f.zoom(14);
            eq(1, f.run(false), "old fix centered");
            f.fix = point(1000, 0);
            eq(0, f.run(false), "query observes new fix rather than cached success");
            eq(2, f.run(true), "bounded coordinator recovery can reissue against fresh fix");
            f.render(); eq(1, f.run(false), "fresh location acknowledged");
        }});
    }

    private static void cameraArguments() {
        scenario("location movement preserves zoom pitch bearing and offset", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.zoom(17.25f); f.pitch(39.5f); f.heading(212.75f);
            eq(2, f.run(true), "location command issued");
            Node command = f.last();
            check(isZero(command.get("Lbjoz;->f:Lbikz;")), "no alternate fixed target");
            close(17.25f, command.floating("Lbjoz;->a:F"), "existing useful zoom preserved");
            close(39.5f, command.floating("Lbjoz;->b:F"), "pitch preserved");
            close(212.75f, command.floating("Lbjoz;->c:F"), "bearing preserved");
            eq(f.offset, command.get("Lbjoz;->d:Lbjox;"), "native offset preserved");
            eq(f.enumValue("Lbjoy;", "a"), command.get("Lbjoz;->e:Lbjoy;"), "location mode selected");
            f.render(); close(212.75f, f.camera.floating("Lbjjc;->d:F"), "actual native projection preserves bearing");
        }});
        scenario("compass movement preserves relative heading without toggling", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.tracking("c"); f.zoom(16); f.pitch(33); f.heading(211); f.compassBearing = 32;
            eq(2, f.run(true), "compass command issued");
            close(179f, f.last().floating("Lbjoz;->c:F"), "relative bearing passed to native configuration");
            eq(f.enumValue("Lbjoy;", "b"), f.last().get("Lbjoz;->e:Lbjoy;"), "compass mode preserved");
            eq(f.enumValue("Lbkmd;", "c"), f.dp.get("Lamdp;->i:Lbkmd;"), "compass tracking preserved");
            eq(2, f.run(true), "explicit permitted recovery is idempotent");
            eq(f.enumValue("Lbkmd;", "c"), f.dp.get("Lamdp;->i:Lbkmd;"), "recovery does not toggle compass");
            f.render();
            close(211f, f.camera.floating("Lbjjc;->d:F"), "native projection restores absolute bearing");
            close(33f, f.camera.floating("Lbjjc;->e:F"), "native projection preserves pitch");
            eq(1, f.run(false), "compass result acknowledged");
            eq(0, f.callbackSubmissions, "compass recovery did not queue a permission toggle");
        }});
        scenario("native offset fallback remains intact", new Runnable() { public void run() {
            Fixture f = new Fixture(); f.dp.put("Lamdp;->o:Lbjox;", null);
            eq(2, f.run(true), "command with default offset");
            eq(f.enumValue("Lbjox;", "a"), f.last().get("Lbjoz;->d:Lbjox;"), "actual native a() fallback used");
        }});
    }

    private static final class Node {
        final String type;
        final Map<String, Object> fields = new HashMap<String, Object>();
        Node(String type) { this.type = type; }
        Node put(String field, Object value) { fields.put(field, value); return this; }
        Object get(String field) {
            if (!fields.containsKey(field)) throw new AssertionError("Unmodeled field: " + field + " on " + type);
            return fields.get(field);
        }
        float floating(String field) { return Float.intBitsToFloat(number(get(field))); }
        public String toString() { return type; }
    }

    private static Node point(double x, double y) {
        return new Node("Lbjaw;").put("test.x", x).put("test.y", y);
    }

    private static double distance(Node a, Node b) {
        double dx = ((Double) a.get("test.x")) - ((Double) b.get("test.x"));
        double dy = ((Double) a.get("test.y")) - ((Double) b.get("test.y"));
        return Math.hypot(dx, dy);
    }

    private static final class Fixture {
        final Map<String, Node> statics = new HashMap<String, Node>();
        final Node activity = new Node("Lnce;");
        final Node layout = new Node(LAYOUT);
        final Node provider = new Node("Lcqpp;");
        final Node controller = new Node("Lamcr;");
        final Node dp = new Node("Lamdp;");
        final Node engine = new Node("Lamej;");
        final Node viewport = new Node("Lbjja;");
        final Node camera = new Node("Lbjjc;");
        final Node offset = new Node("Lbjox;");
        final Node permission = new Node("Lahkj;");
        final List<Node> commands = new ArrayList<Node>();
        Node providerValue = controller;
        Node snapshot = camera;
        Node fix = point(0, 0);
        Node mapMode;
        boolean resumed = true;
        boolean fixValid = true;
        int callbackSubmissions;
        int legacyCalls;
        int copyFixCalls;
        float compassBearing;

        Fixture() {
            for (String letter : Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n")) enumValue("Lamgk;", letter);
            for (String letter : Arrays.asList("a", "b", "c")) enumValue("Lbkmd;", letter);
            enumValue("Lbjoy;", "a").put("Lbjoy;->c:Lbkmd;", enumValue("Lbkmd;", "b"));
            enumValue("Lbjoy;", "b").put("Lbjoy;->c:Lbkmd;", enumValue("Lbkmd;", "c"));
            enumValue("Lbjox;", "a");
            enumValue("Lbjow;", "a");
            activity.put("Lnce;->bx:" + LAYOUT, layout);
            layout.put(LAYOUT + "->i:Lcqpp;", provider);
            controller.put("Lamcr;->p:Lamdp;", dp).put("Lamcr;->y:Lahkj;", permission);
            dp.put("Lamdp;->d:Lamej;", engine).put("Lamdp;->g:Lbizr;", new Node("Lbizr;"))
                .put("Lamdp;->f:Z", 0).put("Lamdp;->k:Lamds;", new Node("Lamds;"))
                .put("Lamdp;->l:Lamdt;", new Node("Lamdt;")).put("Lamdp;->p:Lbjja;", viewport)
                .put("Lamdp;->b:Ljava/lang/Object;", new Node("Ljava/lang/Object;")).put("Lamdp;->o:Lbjox;", offset);
            mode("a"); tracking("a"); near(1000); zoom(14); pitch(0); heading(0);
        }
        Node enumValue(String type, String name) {
            String field = type + "->" + name + ":" + type;
            Node node = statics.get(field);
            if (node == null) { node = new Node(type); statics.put(field, node); }
            return node;
        }
        void mode(String name) { mapMode = enumValue("Lamgk;", name); }
        void tracking(String name) { dp.put("Lamdp;->i:Lbkmd;", enumValue("Lbkmd;", name)); }
        void near(double meters) { camera.put("Lbjjc;->a:Lbjaw;", point(meters, 0)); }
        void zoom(float value) { camera.put("Lbjjc;->h:F", bits(value)); }
        void pitch(float value) { camera.put("Lbjjc;->e:F", bits(value)); }
        void heading(float value) { camera.put("Lbjjc;->d:F", bits(value)); }
        double cameraDistance() { return distance(fix, (Node) camera.get("Lbjjc;->a:Lbjaw;")); }
        int run(boolean issue) { return number(execute(current, this, new Object[] {activity, issue ? 1 : 0})); }
        int old() { return number(execute(legacy, this, new Object[] {activity})); }
        Node last() { return commands.get(commands.size() - 1); }
        void render() {
            // Execute the APK's Bjoz.a projection, not a duplicate bearing formula.
            Node projected = (Node) execute(NATIVE.get(PROJECT), this, new Object[] {last(), fix, bits(compassBearing)});
            camera.put("Lbjjc;->a:Lbjaw;", projected.get("test.target"));
            camera.put("Lbjjc;->h:F", projected.get("Lbjot;->e:F"));
            camera.put("Lbjjc;->e:F", projected.get("Lbjot;->f:F"));
            camera.put("Lbjjc;->d:F", projected.get("Lbjot;->g:F"));
        }

        Object call(MethodReference reference, Object[] a) {
            String d = desc(reference);
            Method body = NATIVE.get(d);
            if (body != null) {
                if (d.startsWith("Lamcr;")) legacyCalls++;
                return execute(body, this, a);
            }
            if (d.equals("Lcqpp;->a()Ljava/lang/Object;")) { same(provider, a[0], d); return providerValue; }
            if (d.equals("Laiau;->K()Z")) { same(controller, a[0], d); return resumed ? 1 : 0; }
            if (d.equals("Lamej;->h()Lamgk;")) { same(engine, a[0], d); return mapMode; }
            if (d.equals("Lamej;->I(Lbjbd;)Z")) {
                same(engine, a[0], d); copyFixCalls++;
                if (fixValid && !isZero(a[1])) node(a[1]).put("test.fix", fix);
                return fixValid ? 1 : 0;
            }
            if (d.equals("Lamej;->b()F")) { same(engine, a[0], d); return bits(compassBearing); }
            if (d.equals("Lbjja;->c()Lbjjc;")) { same(viewport, a[0], d); return snapshot; }
            if (d.equals("Lbjbd;-><init>()V")) { eq("Lbjbd;", node(a[0]).type, "fix allocation type"); return null; }
            if (d.equals("Lbjbd;->v()Lbjaw;")) return node(a[0]).get("test.fix");
            if (d.equals("Lbjau;->c(Lbjaw;Lbjaw;)D")) return Double.doubleToRawLongBits(distance(node(a[0]), node(a[1])));
            if (d.equals("Ljava/lang/Object;-><init>()V")) { node(a[0]); return null; }
            if (d.equals("Lmdl;-><init>(Ljava/lang/Object;I)V")) {
                node(a[0]).put("test.owner", a[1]).put("test.kind", a[2]); return null;
            }
            if (d.equals("Lahkj;->f(Lahki;)V")) {
                same(permission, a[0], d);
                same(controller, node(a[1]).get("test.owner"), "legacy callback captures controller");
                eq(14, number(node(a[1]).get("test.kind")), "legacy callback discriminator");
                callbackSubmissions++; return null;
            }
            if (d.equals("Lamdp;->j(Lbkmd;Lbjoz;Z)V")) {
                same(dp, a[0], d);
                check(isZero(a[1]), "direct native l delegates mode derivation to relative camera");
                Node command = node(a[2]);
                eq(0, number(a[3]), "native dispatch uses approved non-toggle flag");
                Node tracking = node(node(command.get("Lbjoz;->e:Lbjoy;")).get("Lbjoy;->c:Lbkmd;"));
                check(tracking == enumValue("Lbkmd;", "b") || tracking == enumValue("Lbkmd;", "c"), "direct command selects tracking");
                dp.put("Lamdp;->i:Lbkmd;", tracking);
                commands.add(command); return null;
            }
            if (d.equals("Lbjot;-><init>()V")) { eq("Lbjot;", node(a[0]).type, "native projection builder"); return null; }
            if (d.equals("Lbjot;->e(Lbjaw;)V")) { node(a[0]).put("test.target", a[1]); return null; }
            if (d.equals("Lbjot;->a()Lbjow;")) {
                Node result = new Node("Lbjow;"); result.fields.putAll(node(a[0]).fields); return result;
            }
            throw new AssertionError("Unmodeled native boundary: " + d);
        }
    }

    private static Object execute(Method method, Fixture f, Object[] arguments) {
        if (method == null || method.getImplementation() == null) throw new AssertionError("Missing executable native body");
        EXECUTED.add(desc(method));
        MethodImplementation implementation = method.getImplementation();
        Object[] registers = new Object[implementation.getRegisterCount()];
        System.arraycopy(arguments, 0, registers, registers.length - arguments.length, arguments.length);
        Map<Integer, Instruction> code = new HashMap<Integer, Instruction>();
        int size = 0;
        for (Instruction instruction : implementation.getInstructions()) {
            code.put(size, instruction); size += instruction.getCodeUnits();
        }
        Object result = null;
        int pc = 0;
        int budget = 1000;
        while (budget-- > 0) {
            Instruction i = code.get(pc);
            if (i == null) throw new AssertionError("Invalid code offset in " + desc(method) + ": " + pc);
            int next = pc + i.getCodeUnits();
            int a = i instanceof OneRegisterInstruction ? ((OneRegisterInstruction) i).getRegisterA() : -1;
            int b = i instanceof TwoRegisterInstruction ? ((TwoRegisterInstruction) i).getRegisterB() : -1;
            int c = i instanceof ThreeRegisterInstruction ? ((ThreeRegisterInstruction) i).getRegisterC() : -1;
            switch (i.getOpcode()) {
                case NOP: break;
                case CONST_4: case CONST_16: case CONST: case CONST_HIGH16:
                    registers[a] = ((NarrowLiteralInstruction) i).getNarrowLiteral(); break;
                case CONST_WIDE: case CONST_WIDE_16: case CONST_WIDE_32: case CONST_WIDE_HIGH16:
                    registers[a] = ((WideLiteralInstruction) i).getWideLiteral(); registers[a + 1] = WIDE_TAIL; break;
                case MOVE: case MOVE_FROM16: case MOVE_16:
                case MOVE_OBJECT: case MOVE_OBJECT_FROM16: case MOVE_OBJECT_16:
                    registers[a] = registers[b]; break;
                case MOVE_RESULT: case MOVE_RESULT_OBJECT: registers[a] = result; break;
                case MOVE_RESULT_WIDE: registers[a] = result; registers[a + 1] = WIDE_TAIL; break;
                case IGET: case IGET_OBJECT: case IGET_BOOLEAN:
                    registers[a] = node(registers[b]).get(field((FieldReference) ((ReferenceInstruction) i).getReference())); break;
                case IPUT: case IPUT_OBJECT: case IPUT_BOOLEAN:
                    node(registers[b]).put(field((FieldReference) ((ReferenceInstruction) i).getReference()), registers[a]); break;
                case SGET_OBJECT: {
                    String name = field((FieldReference) ((ReferenceInstruction) i).getReference());
                    if (!f.statics.containsKey(name)) throw new AssertionError("Unmodeled static field " + name);
                    registers[a] = f.statics.get(name); break;
                }
                case INSTANCE_OF: {
                    String target = ((TypeReference) ((ReferenceInstruction) i).getReference()).getType();
                    registers[a] = instance(registers[b], target) ? 1 : 0; break;
                }
                case CHECK_CAST: {
                    String target = ((TypeReference) ((ReferenceInstruction) i).getReference()).getType();
                    if (!isZero(registers[a]) && !instance(registers[a], target)) throw new AssertionError("Invalid cast to " + target);
                    break;
                }
                case NEW_INSTANCE:
                    registers[a] = new Node(((TypeReference) ((ReferenceInstruction) i).getReference()).getType()); break;
                case INVOKE_STATIC: case INVOKE_STATIC_RANGE: case INVOKE_VIRTUAL: case INVOKE_VIRTUAL_RANGE:
                case INVOKE_INTERFACE: case INVOKE_INTERFACE_RANGE: case INVOKE_DIRECT: case INVOKE_DIRECT_RANGE:
                    result = f.call((MethodReference) ((ReferenceInstruction) i).getReference(), invokeArguments(i, registers)); break;
                case IF_EQZ: if (isZero(registers[a])) next = pc + ((OffsetInstruction) i).getCodeOffset(); break;
                case IF_NEZ: if (!isZero(registers[a])) next = pc + ((OffsetInstruction) i).getCodeOffset(); break;
                case IF_EQ: if (equal(registers[a], registers[b])) next = pc + ((OffsetInstruction) i).getCodeOffset(); break;
                case IF_NE: if (!equal(registers[a], registers[b])) next = pc + ((OffsetInstruction) i).getCodeOffset(); break;
                case IF_LTZ: if (number(registers[a]) < 0) next = pc + ((OffsetInstruction) i).getCodeOffset(); break;
                case IF_GEZ: if (number(registers[a]) >= 0) next = pc + ((OffsetInstruction) i).getCodeOffset(); break;
                case GOTO: case GOTO_16: case GOTO_32: next = pc + ((OffsetInstruction) i).getCodeOffset(); break;
                case CMPL_FLOAT: case CMPG_FLOAT: {
                    float left = Float.intBitsToFloat(number(registers[b]));
                    float right = Float.intBitsToFloat(number(registers[c]));
                    registers[a] = Float.isNaN(left) || Float.isNaN(right)
                        ? (i.getOpcode() == com.android.tools.smali.dexlib2.Opcode.CMPL_FLOAT ? -1 : 1)
                        : left < right ? -1 : left > right ? 1 : 0;
                    break;
                }
                case CMPL_DOUBLE: case CMPG_DOUBLE: {
                    double left = Double.longBitsToDouble(((Long) registers[b]).longValue());
                    double right = Double.longBitsToDouble(((Long) registers[c]).longValue());
                    registers[a] = Double.isNaN(left) || Double.isNaN(right)
                        ? (i.getOpcode() == com.android.tools.smali.dexlib2.Opcode.CMPL_DOUBLE ? -1 : 1)
                        : left < right ? -1 : left > right ? 1 : 0;
                    break;
                }
                case SUB_FLOAT_2ADDR:
                    registers[a] = bits(Float.intBitsToFloat(number(registers[a])) - Float.intBitsToFloat(number(registers[b]))); break;
                case ADD_FLOAT_2ADDR:
                    registers[a] = bits(Float.intBitsToFloat(number(registers[a])) + Float.intBitsToFloat(number(registers[b]))); break;
                case MONITOR_ENTER: case MONITOR_EXIT: node(registers[a]); break;
                case RETURN: case RETURN_OBJECT: return registers[a];
                case RETURN_VOID: return null;
                default: throw new AssertionError("Unsupported opcode " + i.getOpcode() + " in " + desc(method));
            }
            pc = next;
        }
        throw new AssertionError("Instruction budget exhausted in " + desc(method));
    }

    private static Object[] invokeArguments(Instruction i, Object[] registers) {
        int count = ((VariableRegisterInstruction) i).getRegisterCount();
        Object[] result = new Object[count];
        if (i instanceof RegisterRangeInstruction) {
            int start = ((RegisterRangeInstruction) i).getStartRegister();
            System.arraycopy(registers, start, result, 0, count);
        } else {
            FiveRegisterInstruction five = (FiveRegisterInstruction) i;
            int[] indices = {five.getRegisterC(), five.getRegisterD(), five.getRegisterE(), five.getRegisterF(), five.getRegisterG()};
            for (int n = 0; n < count; n++) result[n] = registers[indices[n]];
        }
        return result;
    }
    private static String desc(MethodReference m) { return MapsStartupLocationPatch.descriptor(m); }
    private static String field(FieldReference f) { return f.getDefiningClass() + "->" + f.getName() + ":" + f.getType(); }
    private static int bits(float f) { return Float.floatToRawIntBits(f); }
    private static int number(Object value) {
        if (!(value instanceof Integer)) throw new AssertionError("Expected 32-bit register, got " + value);
        return ((Integer) value).intValue();
    }
    private static Node node(Object value) {
        if (!(value instanceof Node)) throw new AssertionError("Expected object register, got " + value);
        return (Node) value;
    }
    private static boolean isZero(Object value) { return value == null || value instanceof Integer && ((Integer) value).intValue() == 0; }
    private static boolean equal(Object a, Object b) { return isZero(a) && isZero(b) || a == b || a instanceof Integer && a.equals(b); }
    private static boolean instance(Object value, String type) {
        return value instanceof Node && (((Node) value).type.equals(type)
            || ((Node) value).type.equals("Lamcr;") && type.equals("Laiau;") || type.equals("Ljava/lang/Object;"));
    }
    private static void same(Object expected, Object actual, String message) { check(expected == actual, message); }
    private static void eq(Object expected, Object actual, String message) {
        check(expected == actual || expected != null && expected.equals(actual), message + ": expected " + expected + ", got " + actual);
    }
    private static void close(float expected, float actual, String message) { check(Math.abs(expected - actual) < 0.0001f, message); }
    private static void check(boolean condition, String message) {
        assertions++; if (!condition) throw new AssertionError(message);
    }
    private static void scenario(String name, Runnable body) {
        try { body.run(); scenarios++; }
        catch (Throwable error) { throw new AssertionError("Scenario failed: " + name, error); }
    }
}
