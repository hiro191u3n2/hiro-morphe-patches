import hiro.swiftkey.FlickSymbols;
import hiro.swiftkey.KeySwap;
import hiro.swiftkey.PunctuationKeys;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.touchtype.KeyboardService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Production helpers and FlickSymbols on explicit host fakes.
 * Native builder branches/actor graph are derived from the pinned static review.
 * Native timer, rendering, fluency-engine side effects and ART are not executed.
 */
public final class PunctuationKeysHostTest {
    static int scenarios, assertions;
    static long clock = 10000;
    static final lh0.a TOKEN = new lh0.a();
    static final long CACHE_TAG = 0x0800000000000000L;
    static final String STANDARD = "keyboard_layout_standard_romaji";
    static final String PC = "keyboard_layout_standard_romaji_pc";

    static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    static void scenario() {
        scenarios++;
        FlickSymbols.reset();
        ViewConfiguration.reset();
    }
    static byte[] read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int count;
        while ((count = input.read(data)) != -1) output.write(data, 0, count);
        return output.toByteArray();
    }
    static Resources resources(String name, byte[] bytes) {
        Resources value = new Resources();
        value.names.put(7, name);
        value.bytes.put(7, bytes);
        return value;
    }
    static fc0.v1 fields(String primary, String alternate) {
        fc0.v1 fields = new fc0.v1();
        fields.a = alternate;
        fields.b = primary;
        fields.g.add(PunctuationKeys.MARKER);
        fields.o = new ArrayList<String>(Collections.singletonList(alternate));
        return fields;
    }
    static fc0.s1 factory() {
        fc0.s1 factory = new fc0.s1();
        factory.i = new fb0.h2();
        factory.i.owner = new Model();
        return factory;
    }
    static fc0.m1 geometry(float left) {
        fc0.m1 geometry = new fc0.m1();
        geometry.a = new RectF(left, .6f, left + .1f, .8f);
        return geometry;
    }
    static final class TrackingStream extends InputStream {
        final byte[] bytes;
        int position, reads, closes;
        boolean failRead, failClose;
        TrackingStream(byte[] bytes) { this.bytes = bytes; }
        public int read() throws IOException {
            reads++;
            if (failRead) throw new IOException("test read");
            return position == bytes.length ? -1 : bytes[position++] & 255;
        }
        public void close() throws IOException {
            closes++;
            if (failClose) throw new IOException("test close");
        }
    }

    static void wrapperTests(Path work) throws Exception {
        byte[] raw = Files.readAllBytes(work.resolve("original-standard.zlib"));
        byte[] swapped = Files.readAllBytes(work.resolve("input/standard-051.zlib"));
        byte[] normalized = Files.readAllBytes(work.resolve("output/standard-051.zlib"));
        byte[] pc = Files.readAllBytes(work.resolve("original-pc.zlib"));
        byte[] normalizedPc = PunctuationKeys.transform(pc, PC);
        scenario();
        TrackingStream sourceStream = new TrackingStream(raw);
        sc0.c source = new sc0.c(7, 0x12345678L, sourceStream);
        Resources resources = resources(STANDARD, raw);
        KeySwap.Wrapped result = PunctuationKeys.wrap(source, resources, null);
        check(result != null && result.cacheKey == (source.b ^ CACHE_TAG), "raw fallback transforms and namespaces native cache");
        check(Arrays.equals(read(result.stream), PunctuationKeys.transform(raw, STANDARD)), "raw fallback returns normalized bytes");
        check(sourceStream.reads == 0 && sourceStream.closes == 1, "raw success never reads descriptor stream and closes it once");

        scenario();
        sourceStream = new TrackingStream(raw);
        source = new sc0.c(7, 17, sourceStream);
        resources = resources(STANDARD, raw);
        ByteArrayInputStream previousStream = new ByteArrayInputStream(swapped);
        KeySwap.Wrapped previous = new KeySwap.Wrapped(previousStream, 0x2033000000000077L);
        int available = previousStream.available();
        result = PunctuationKeys.wrap(source, resources, previous);
        check(result != null && result != previous, "post-keyswap stream is normalized");
        check(Arrays.equals(read(result.stream), normalized), "all enabled key swaps are retained");
        check(previousStream.available() == available && sourceStream.reads == 0 && sourceStream.closes == 0, "success preserves previous fallback cursor and descriptor ownership");
        check(resources.opens == 0 && result.cacheKey == (previous.cacheKey ^ CACHE_TAG), "existing transform is reused without reopening original layout");

        scenario();
        previous = new KeySwap.Wrapped(new ByteArrayInputStream(normalized), previous.cacheKey ^ CACHE_TAG);
        result = PunctuationKeys.wrap(source, resources, previous);
        check(result == previous && result.stream.available() == normalized.length, "idempotent wrapper preserves same cache tag and full stream");

        scenario();
        sourceStream = new TrackingStream(pc);
        source = new sc0.c(7, 119, sourceStream);
        result = PunctuationKeys.wrap(source, resources(PC, pc), null);
        check(result != null && Arrays.equals(read(result.stream), normalizedPc), "PC is supported when existing KeySwap returns null");

        for (int mode = 0; mode < 5; mode++) {
            scenario();
            sourceStream = new TrackingStream(raw);
            source = new sc0.c(7, 13, sourceStream);
            resources = resources(STANDARD, raw);
            if (mode == 0) resources.names.put(7, "keyboard_layout_standard_qwerty");
            if (mode == 1) resources.failName = true;
            if (mode == 2) resources.failOpen = true;
            if (mode == 3) resources.bytes.put(7, new byte[] {1, 2, 3});
            if (mode == 4) resources.bytes.put(7, pc);
            check(PunctuationKeys.wrap(source, resources, null) == null, "raw unknown/failure returns native fallback " + mode);
            check(sourceStream.reads == 0 && sourceStream.closes == 0, "raw fallback descriptor remains pristine " + mode);
        }

        scenario();
        TrackingStream custom = new TrackingStream(swapped);
        previous = new KeySwap.Wrapped(custom, 3);
        check(PunctuationKeys.wrap(source, resources(STANDARD, raw), previous) == previous, "unknown stream type left unchanged");
        check(custom.reads == 0 && custom.closes == 0, "unknown stream is never touched");
        scenario();
        previousStream = new ByteArrayInputStream(swapped);
        previousStream.skip(7);
        previous = new KeySwap.Wrapped(previousStream, 5);
        available = previousStream.available();
        check(PunctuationKeys.wrap(source, resources(STANDARD, raw), previous) == previous, "partly consumed compressed stream fails closed");
        check(previousStream.available() == available, "failed transform restores exact cursor");
        scenario();
        previousStream = new ByteArrayInputStream(new byte[8193]);
        previous = new KeySwap.Wrapped(previousStream, 9);
        check(PunctuationKeys.wrap(source, resources(STANDARD, raw), previous) == previous, "oversize stream fails closed");
        check(previousStream.available() == 8193, "bounded read failure resets prior cursor");
        scenario();
        previous = new KeySwap.Wrapped(null, 7);
        check(PunctuationKeys.wrap(source, resources(STANDARD, raw), previous) == previous, "null previous stream remains fallback");
        check(PunctuationKeys.wrap(null, resources, previous) == previous, "null descriptor remains fallback");
        check(PunctuationKeys.wrap(source, null, previous) == previous, "null resources remains fallback");

        scenario();
        sourceStream = new TrackingStream(raw);
        sourceStream.failClose = true;
        source = new sc0.c(7, 3, sourceStream);
        check(PunctuationKeys.wrap(source, resources(STANDARD, raw), null) != null, "descriptor close failure cannot discard prepared valid replacement");
        scenario();
        resources = resources(PC, pc);
        TrackingStream failing = new TrackingStream(pc);
        failing.failRead = true;
        resources.override = failing;
        sourceStream = new TrackingStream(pc);
        source = new sc0.c(7, 7, sourceStream);
        check(PunctuationKeys.wrap(source, resources, null) == null && sourceStream.reads == 0 && sourceStream.closes == 0,
                "separate raw handle failure leaves native stream intact");
        check(failing.closes == 1, "failed separate raw handle is closed");
    }

    static void scopeTests() {
        scenario();
        fc0.s1 factory = factory();
        fc0.m1 geometry = geometry(.2f);
        fc0.v1 fields = fields("。", "？");
        List<String> originalCandidates = fields.o;
        fb0.f2 period = PunctuationKeys.periodPopup(factory, geometry, fields);
        check(period != null && period.d.size() == 1 && period.l == 0, "period factory builds one native candidate and default index");
        check(factory.i.lastCandidates.equals(Collections.singletonList("？")), "period does not prepend primary or language variants");
        check(factory.i.lastFields == fields && factory.i.lastGeometry == geometry, "period keeps native fields and geometry objects");
        check(fields.o == originalCandidates && fields.g.equals(Collections.singletonList(PunctuationKeys.MARKER)), "period factory does not mutate key fields");

        scenario();
        factory = factory();
        fields = fields("ー", "！");
        xc0.a vowel = PunctuationKeys.longVowelPopup(factory, geometry, fields);
        check(vowel != null && vowel.a == vowel.b && vowel.a instanceof xc0.i, "long vowel retains native direct-singleton route in both shift states");
        check(factory.hCalls == 1 && factory.i.calls == 0 && vowel.c().equals(Collections.singleton("！")), "long vowel delegates exactly once to native H without variant construction");
        check(factory.lastFields == fields && factory.lastGeometry == geometry, "long vowel retains native inputs");
        scenario();
        factory = factory();
        fields = fields("ー", "！");
        fields.a = null;
        vowel = PunctuationKeys.longVowelPopup(factory, geometry, fields);
        check(vowel != null && vowel.c().equals(Collections.singleton("！")),
                "native secondary-label-off state keeps the tagged long-vowel singleton");
        scenario();
        factory = factory();
        factory.accessible = true;
        vowel = PunctuationKeys.longVowelPopup(factory, geometry, fields);
        check(vowel != null && vowel.a instanceof xc0.f && vowel.c().equals(Collections.singleton("！")), "touch exploration keeps native one-candidate popup branch");

        for (String primary : new String[] {"。", "ー"}) {
            String alternate = primary.equals("。") ? "？" : "！";
            for (int mismatch = 0; mismatch < 12; mismatch++) {
                scenario();
                factory = factory();
                fields = fields(primary, alternate);
                if (mismatch == 0) fields.g.clear();
                if (mismatch == 1) fields.g = null;
                if (mismatch == 2) fields.b = "x";
                if (mismatch == 3) fields.c = "foreign-output";
                if (mismatch == 4) fields.a = "?";
                if (mismatch == 5) fields.o = null;
                if (mismatch == 6) fields.o.clear();
                if (mismatch == 7) fields.o.add("duplicate-extra");
                if (mismatch == 8) fields.o.set(0, "!");
                if (mismatch == 9) fields.g.set(0, PunctuationKeys.MARKER + "foreign");
                if (mismatch == 10) fields = null;
                Object result = primary.equals("。")
                        ? PunctuationKeys.periodPopup(factory, mismatch == 11 ? null : geometry, fields)
                        : PunctuationKeys.longVowelPopup(factory, mismatch == 11 ? null : geometry, fields);
                check(result == null, "unrelated/inconsistent key falls through " + primary + " / " + mismatch);
                check(factory.i.calls == 0 && factory.hCalls == 0, "scope rejection calls no native builder");
            }
        }
        for (int failure = 0; failure < 5; failure++) {
            scenario();
            factory = factory();
            if (failure == 0) factory.hNull = true;
            if (failure == 1) factory.hEmpty = true;
            if (failure == 2) factory.failure = new IllegalStateException("native unavailable");
            if (failure == 3) factory.override = new xc0.i("!");
            if (failure == 4) factory.override = new xc0.h() {
                public Set c() { return new java.util.HashSet(Arrays.asList("！", "!")); }
            };
            check(PunctuationKeys.longVowelPopup(factory, geometry, fields("ー", "！")) == null,
                    "invalid native singleton result falls through " + failure);
        }
        scenario();
        factory = factory();
        factory.i.failure = new IllegalStateException("native popup unavailable");
        check(PunctuationKeys.periodPopup(factory, geometry, fields("。", "？")) == null, "period builder exception runs original native fallback");
        check(PunctuationKeys.periodPopup(null, geometry, fields("。", "？")) == null, "period null factory fallback");
        check(PunctuationKeys.longVowelPopup(null, geometry, fields("ー", "！")) == null, "vowel null factory fallback");
    }

    static final class Prefs implements SharedPreferences {
        boolean enabled = true;
        public boolean getBoolean(String key, boolean fallback) { return enabled; }
        public Editor edit() { throw new UnsupportedOperationException(); }
    }
    static final class State implements dc0.a {
        String text = "";
        public String s() { return text; }
        public String G() { return text; }
        public int k() { return text.length(); }
        public int S() { return text.length(); }
    }
    static final class Model extends wb0.x {
        final State state = new State();
        final List<String> insertions = new ArrayList<String>();
        Model() { c = new wb0.n0(); c.state = state; }
        public void P(lh0.a token, String text, String punctuation, ec0.m ignored, boolean alternate, Integer value) {
            insertions.add(text);
            state.text += text;
        }
    }
    static final class KeyView extends xd0.f0 {
        final KeyboardService service;
        final Model model = new Model();
        final Prefs prefs = new Prefs();
        final fc0.n2 key = new fc0.n2();
        final fc0.v1 fields;
        final fc0.s1 factory = factory();
        final ic0.a alternate;
        final fc0.b1 opener;
        int cancels, nativeUps, ordinaryTaps;
        boolean pressed;
        KeyView(fc0.v1 fields, float left) {
            super(new KeyboardService());
            this.fields = fields;
            service = (KeyboardService) getContext();
            service.i = new android.view.inputmethod.EditorInfo();
            service.h = new android.view.inputmethod.InputConnection() {
                public CharSequence getTextBeforeCursor(int n, int flags) { return model.state.text; }
                public CharSequence getTextAfterCursor(int n, int flags) { return ""; }
            };
            j = true;
            A = new fz.h();
            I = new android.util.SparseArray();
            z = model;
            a = new fb0.f0();
            a.c = new fb0.q0();
            a.c.c = qj0.a.x4;
            fb0.f0 layout = (fb0.f0) a;
            layout.keys = new wb0.x0();
            layout.keys.b = com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;
            layout.d.add(key);
            key.d = new fc0.d2() {};
            key.e = geometry(left);
            oc0.m behavior = new oc0.m();
            key.b = behavior;
            behavior.b = new hc0.a();
            behavior.b.c = new hc0.k();
            factory.i.owner = model;
            if (fields.b.equals("。")) {
                fb0.f2 popup = PunctuationKeys.periodPopup(factory, key.e, fields);
                if (popup == null) throw new AssertionError("fixture period helper rejected");
                opener = new fc0.b1();
                opener.a = 1;
                e60.b binding = new e60.b();
                binding.b = key.d;
                androidx.lifecycle.h provider = new androidx.lifecycle.h();
                provider.a = 7;
                provider.b = new xc0.f(popup);
                binding.a = provider;
                opener.b = binding;
                ic0.e wrapper = new ic0.e();
                wrapper.a = opener;
                hc0.o entry = new hc0.o();
                entry.action = wrapper;
                behavior.b.c.b.add(entry);
                fc0.n2 chosen = (fc0.n2) popup.d.get(popup.l);
                List releases = (List) ((oc0.m) chosen.b).b.b.b;
                alternate = (ic0.a) ((hc0.o) releases.get(0)).action;
            } else {
                xc0.a pair = PunctuationKeys.longVowelPopup(factory, key.e, fields);
                if (pair == null || !(pair.a instanceof xc0.i)) throw new AssertionError("fixture vowel helper rejected");
                final fc0.d1 actor = new fc0.d1();
                actor.b = ((xc0.i) pair.a).b;
                actor.c = ((xc0.i) pair.a).c;
                actor.callback = new Runnable() {
                    public void run() { model.P(TOKEN, actor.b, actor.c, null, true, null); }
                };
                ic0.e wrapper = new ic0.e();
                wrapper.a = actor;
                hc0.o entry = new hc0.o();
                entry.action = wrapper;
                behavior.b.c.d.add(entry);
                alternate = actor;
                opener = null;
            }
            FlickSymbols.init(prefs);
        }
        public boolean f(lh0.a token, MotionEvent event) {
            if (event.action == MotionEvent.ACTION_DOWN) pressed = true;
            if (event.action == MotionEvent.ACTION_CANCEL) { cancels++; pressed = false; }
            if (event.action == MotionEvent.ACTION_UP) {
                nativeUps++;
                if (pressed) {
                    ordinaryTaps++;
                    model.P(token, fields.b(), fields.b(), null, false, null);
                }
                pressed = false;
            }
            return true;
        }
        boolean dispatch(int action, float dx, float up, long down, long delta) {
            MotionEvent event = new MotionEvent(action, key.e.a.centerX() * width + dx,
                    key.e.a.centerY() * height - up, down, down + delta);
            boolean consumed = FlickSymbols.handle(this, TOKEN, event, false);
            if (!consumed) f(TOKEN, event);
            return consumed;
        }
        long down() {
            long time = clock;
            clock += 1000;
            check(!dispatch(0, 0, 0, time, 0), "native receives initial press");
            return time;
        }
    }

    static void gestureTests(Path work) throws Exception {
        List<String> fixtures = Files.readAllLines(work.resolve("behavior.tsv"), StandardCharsets.UTF_8);
        for (String line : fixtures) {
            String[] values = line.split("\t", -1);
            String name = values[0], primary = values[1], alternate = values[2];
            float left = Float.parseFloat(values[4]);
            fc0.v1 fields = fields(primary, alternate);
            fields.a = values[3];
            fields.g.clear();
            fields.g.addAll(Arrays.asList(values[5].split(",")));
            scenario();
            KeyView view = new KeyView(fields, left);
            long down = view.down();
            check(view.dispatch(2, 0, 25, down, 40), "real FlickSymbols captures normalized native route " + name);
            check(view.model.insertions.isEmpty() && view.cancels == 1, "capture cancels native without inserting early");
            check(view.dispatch(1, 0, 60, down, 90), "real FlickSymbols consumes release");
            check(view.model.insertions.equals(Collections.singletonList(alternate)), "upward flick commits exact fullwidth alternate " + name);
            check(view.ordinaryTaps == 0 && view.nativeUps == 0, "flick never also emits ordinary punctuation");
            if (view.opener != null) check(view.opener.calls == 0, "flick does not open period popup");

            scenario();
            view = new KeyView(fields, left);
            down = view.down();
            check(!view.dispatch(1, 0, 0, down, 60), "stationary ordinary tap remains native");
            check(view.model.insertions.equals(Collections.singletonList(primary)) && view.cancels == 0,
                    "tap retains original primary output " + name);

            scenario();
            view = new KeyView(fields, left);
            view.alternate.b(TOKEN); // Explicit native candidate/release actor invocation.
            check(view.model.insertions.equals(Collections.singletonList(alternate)), "native longpress candidate actor matches visible small label " + name);
            check(fields.a.equals(alternate), "small label matches sole candidate");
        }

        for (String primary : new String[] {"。", "ー"}) {
            String alternate = primary.equals("。") ? "？" : "！";
            for (float angle : new float[] {0, 60, 67.4f, 67.6f, 90}) {
                scenario();
                KeyView view = new KeyView(fields(primary, alternate), .2f);
                long down = view.down();
                float dx = (float) (Math.sin(Math.toRadians(angle)) * 70);
                float up = (float) (Math.cos(Math.toRadians(angle)) * 70);
                boolean captured = view.dispatch(1, dx, up, down, 70);
                check(captured == (angle < 67.5f), "existing up-sector angle is unchanged: " + primary + " / " + angle);
                check(view.model.insertions.equals(Collections.singletonList(captured ? alternate : primary)), "direction boundary chooses native primary versus sole alternate");
            }
            scenario();
            KeyView view = new KeyView(fields(primary, alternate), .2f);
            long down = view.down();
            check(!view.dispatch(2, 0, 10, down, 30), "below unchanged native-size threshold remains undecided");
            check(view.dispatch(2, 0, 25, down, 50), "crossing unchanged native-size threshold captures");
            check(view.dispatch(1, 100, -20, down, 600), "captured gesture remains owned after drift and timeout");
            check(view.model.insertions.equals(Collections.singletonList(alternate)), "locked gesture emits exactly once");
            scenario();
            view = new KeyView(fields(primary, alternate), .2f);
            down = view.down();
            check(view.dispatch(2, 0, 25, down, 40), "cancel scenario captured");
            check(view.dispatch(3, 0, 25, down, 50), "captured cancellation consumed");
            check(view.model.insertions.isEmpty(), "cancel never inserts alternate");
            scenario();
            view = new KeyView(fields(primary, alternate), .2f);
            view.prefs.enabled = false;
            down = view.down();
            check(!view.dispatch(1, 0, 60, down, 90) && view.cancels == 0, "disabled flick toggle preserves native handling");
        }
        scenario();
        fc0.v1 hidden = fields("ー", "！");
        hidden.a = null;
        KeyView view = new KeyView(hidden, .2f);
        long down = view.down();
        check(view.dispatch(1, 0, 50, down, 70), "hidden secondary label still uses native single actor");
        check(view.model.insertions.equals(Collections.singletonList("！")), "hidden label cannot reintroduce language alternatives");
    }

    public static void main(String[] args) throws Exception {
        Path work = Paths.get(args[0]);
        wrapperTests(work);
        scopeTests();
        gestureTests(work);
        System.out.println("REPORT {\"status\":\"PASSED\",\"scenarios\":" + scenarios
                + ",\"assertions\":" + assertions + "}");
    }
}
