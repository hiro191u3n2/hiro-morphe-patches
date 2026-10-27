package hiro.swiftkey;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Independent state-machine observations: no replacement production methods. */
public final class EmojiSlideHostTest {
    static int scenarios, assertions;
    static long sequence = 1000;
    static final String[] EMOJI = {"👩🏽‍💻", "🇯🇵", "👨‍👩‍👧‍👦", "❤️", "😀", "🥹", "👍🏿", "🎉", "🐈", "🍜", "🌸", "🚀", "🧑🏻‍🚀", "🌈"};

    static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    static void eq(Object expected, Object actual, String message) {
        check(expected == null ? actual == null : expected.equals(actual), message + " expected=" + expected + " actual=" + actual);
    }
    static void near(float expected, float actual, String message) {
        check(Math.abs(expected - actual) < .01f, message + " expected=" + expected + " actual=" + actual);
    }
    static void scenario(String name, Runnable test) {
        EmojiRecentsSlide.reset();
        test.run();
        EmojiRecentsSlide.reset();
        scenarios++;
        System.out.println("PASS " + name);
    }

    static final class Preferences implements android.content.SharedPreferences {
        int level = 2, intReads, booleanReads;
        boolean fail;
        public boolean getBoolean(String key, boolean value) { booleanReads++; return false; }
        public int getInt(String key, int fallback) {
            intReads++;
            eq("hiro_up_flick_sensitivity", key, "only shared sensitivity setting read");
            if (fail) throw new ClassCastException("corrupt preference");
            return level;
        }
    }
    static final class State implements dc0.a {
        String composition = "", raw = "";
        int start, end;
        public String s() { return composition; }
        public String G() { return raw; }
        public int k() { return start; }
        public int S() { return end; }
    }
    static final class Connection implements android.view.inputmethod.InputConnection {
        String before = "before", after = "after";
        boolean fail;
        public CharSequence getTextBeforeCursor(int count, int flags) {
            if (fail) throw new IllegalStateException("editor unavailable");
            return before;
        }
        public CharSequence getTextAfterCursor(int count, int flags) { return after; }
    }
    static final class Parent implements android.view.ViewParent {
        final List<Boolean> intercept = new ArrayList<Boolean>();
        Runnable onRelease;
        public void requestDisallowInterceptTouchEvent(boolean value) {
            intercept.add(value);
            if (!value && onRelease != null) onRelease.run();
        }
    }
    static final class NativeView extends xd0.f0 {
        final List<Integer> nativeActions = new ArrayList<Integer>();
        MotionEvent cancel;
        Runnable onCancel;
        boolean throwCancel;
        NativeView(android.content.Context context) { super(context); }
        public boolean f(lh0.a token, MotionEvent event) {
            nativeActions.add(event.getActionMasked());
            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                cancel = event;
                if (onCancel != null) onCancel.run();
                if (throwCancel) throw new IllegalStateException("native cancel failure");
            }
            return false;
        }
    }
    static final class Fixture {
        final com.touchtype.KeyboardService service = new com.touchtype.KeyboardService();
        final Connection connection = new Connection();
        final State state = new State();
        final NativeView view = new NativeView(service);
        final Parent parent = new Parent();
        final wb0.x sender = new wb0.x();
        final fb0.b0 provider = new fb0.b0();
        final ne0.f0 historyModel = new ne0.f0();
        final ne0.e0 history = new ne0.e0();
        final fb0.f0 layout = new fb0.f0();
        final fc0.n2 key = new fc0.n2();
        final hc0.b builder = new hc0.b();
        final fc0.h factory = new fc0.h();
        final ic0.a0 original = new ic0.a0();
        final Preferences preferences = new Preferences();
        final lh0.a token = new lh0.a();
        final long downTime = sequence += 1000;
        float startX = 850, startY = 360;

        Fixture() {
            android.os.SystemClock.now = 10000;
            ViewConfiguration.reset();
            service.h = connection;
            service.i = new android.view.inputmethod.EditorInfo();
            view.j = true;
            view.A = new fz.h();
            view.I = new android.util.SparseArray();
            view.parent = parent;
            view.a = layout;
            view.z = sender;
            sender.c = new wb0.n0();
            sender.c.state = state;
            sender.t = provider;
            provider.a = historyModel;
            historyModel.f = history;
            for (String value : EMOJI) history.recents.add(new ne0.p0(value));
            factory.b = new fc0.k1();
            factory.b.a = service;
            factory.b.w = 500;
            builder.a = new bf0.g();
            builder.d = new ArrayList();
            original.a = oz.i6.b;
            builder.d.add(new hc0.o(new cm0.d() { public Object invoke(Object value) { return Boolean.TRUE; } }, new ic0.e(original)));
            CandidateToolbar.closeResult = false;
            CandidateToolbar.toggleResult = true;
            EmojiToolbar.configure(factory, builder);
            check(builder.f != null, "real toolbar configure installs native long press");
            oc0.m behavior = new oc0.m();
            behavior.b = new hc0.a();
            behavior.b.c = builder.f;
            key.b = behavior;
            key.e = new fc0.m1();
            key.e.a = new RectF(.8f, .75f, .9f, 1f);
            layout.d.add(key);
            EmojiRecentsSlide.init(preferences);
        }
        MotionEvent event(int action, float x, float y, long elapsed) {
            return new MotionEvent(action, x, y, downTime, downTime + elapsed);
        }
        boolean send(MotionEvent event) {
            boolean consumed = EmojiRecentsSlide.handle(view, token, event);
            if (!consumed) view.f(token, event);
            return consumed;
        }
        boolean send(int action, float x, float y, long elapsed) { return send(event(action, x, y, elapsed)); }
        void down() { check(!send(MotionEvent.ACTION_DOWN, startX, startY, 0), "DOWN delegated to original native dispatcher"); }
        void capture() {
            down();
            check(send(MotionEvent.ACTION_MOVE, startX, startY - 30, 20), "qualifying MOVE is consumed");
            eq(1, view.overlay.drawables.size(), "one overlay shown");
            eq(Arrays.asList(0, 3), view.nativeActions, "exactly DOWN and one native CANCEL before ownership");
            check(view.cancel.recycled, "copied native CANCEL recycled");
            eq(1, view.attachments.size(), "one detach listener registered");
            eq(Arrays.asList(Boolean.TRUE), parent.intercept, "parent interception disabled during captured stream");
        }
        RectF bounds() {
            RectF rect = key.e.a;
            return EmojiRecentsSlide.popupBounds(view.width, view.height,
                new RectF(rect.left * view.width, rect.top * view.height, rect.right * view.width, rect.bottom * view.height),
                service.getResources().getDisplayMetrics().density, Math.min(history.recents.size(), 12));
        }
        float[] cell(int index) {
            RectF panel = bounds();
            float density = service.getResources().getDisplayMetrics().density;
            int rows = Math.max(1, (Math.min(history.recents.size(), 12) + 3) / 4);
            float width = (panel.width() - 16 * density) / 4;
            float height = (panel.height() - 44 * density) / rows;
            return new float[] {panel.left + 8 * density + (index % 4 + .5f) * width,
                panel.top + 36 * density + (index / 4 + .5f) * height};
        }
        boolean at(int action, int index, long elapsed) {
            float[] point = cell(index);
            return send(action, point[0], point[1], elapsed);
        }
        Canvas draw() {
            Canvas canvas = new Canvas();
            for (Drawable drawable : view.overlay.drawables) drawable.draw(canvas);
            return canvas;
        }
        void closed() {
            eq(0, view.overlay.drawables.size(), "overlay removed");
            eq(0, view.attachments.size(), "detach listener removed");
            if (!parent.intercept.isEmpty()) eq(Boolean.FALSE, parent.intercept.get(parent.intercept.size() - 1), "parent interception restored");
        }
        void noInput() { eq(0, sender.entered.size(), "no emoji input"); eq(0, history.writes, "no history write"); }
    }

    static ic0.a actor(Object entry) { Object value = ((hc0.o) entry).b; return value instanceof ic0.e ? ((ic0.e) value).a : (ic0.a) value; }

    static void bridgeTests() {
        scenario("native recent order, maximum twelve and complete Unicode sequences", () -> {
            Fixture f = new Fixture();
            EmojiRecentsBridge.Snapshot s = EmojiRecentsBridge.snapshot(f.sender, 100);
            eq(12, s.size(), "maximum twelve");
            for (int i = 0; i < 12; i++) eq(EMOJI[i], s.get(i), "native newest-first order and intact grapheme " + i);
            f.history.recents.clear();
            eq(EMOJI[0], s.get(0), "snapshot remains immutable after native list mutates");
            eq(0, EmojiRecentsBridge.snapshot(f.sender, 12).size(), "empty history remains empty");
        });
        scenario("history filtering and graph/read guards", () -> {
            Fixture f = new Fixture();
            f.history.recents.clear();
            ((ArrayList) f.history.recents).add("invalid object");
            f.history.recents.add(null);
            f.history.recents.add(new ne0.p0(null));
            f.history.recents.add(new ne0.p0(""));
            f.history.recents.add(new ne0.p0(EMOJI[0]));
            f.history.recents.add(new ne0.p0(EMOJI[1]));
            eq(1, EmojiRecentsBridge.snapshot(f.sender, 1).size(), "limit applied to valid strings");
            eq(EMOJI[0], EmojiRecentsBridge.snapshot(f.sender, 1).get(0), "first valid string exact");
            eq(2, EmojiRecentsBridge.snapshot(f.sender, 12).size(), "invalid values skipped without defaults");
            check(EmojiRecentsBridge.snapshot(null, 12) == null, "null model rejected");
            check(EmojiRecentsBridge.snapshot(f.sender, 0) == null, "zero limit rejected");
            f.history.failRead = true;
            check(EmojiRecentsBridge.snapshot(f.sender, 12) == null, "native read failure guarded");
            f.history.failRead = false;
            f.provider.a = new Object();
            check(EmojiRecentsBridge.snapshot(f.sender, 12) == null, "wrong native graph rejected");
            f.sender.t = null;
            check(EmojiRecentsBridge.snapshot(f.sender, 12) == null, "missing native provider rejected");
        });
        scenario("native insertion precedes history, full string, exact token and RECENTS source", () -> {
            Fixture f = new Fixture();
            EmojiRecentsBridge.Snapshot s = EmojiRecentsBridge.snapshot(f.sender, 12);
            check(s.commit(f.token, 2), "native insertion succeeds");
            eq(Arrays.asList(EMOJI[2]), f.sender.entered, "complete family sequence inserted once");
            eq(Arrays.asList(f.token), f.sender.tokens, "original breadcrumb passed");
            eq(Arrays.asList(oz.j2.a), f.sender.sources, "native RECENTS source passed");
            eq(Arrays.asList("input:" + EMOJI[2], "history:" + EMOJI[2]), f.history.events, "native callback order");
            eq(1, f.history.writes, "native history callback writes once");
            eq(1, f.history.uses, "native usage callback increments once");
            eq(1, f.history.notifications, "native notify path once");
            eq(EMOJI[2], f.history.recents.get(0).getContent(), "native recency callback moves selected sequence to front");
            check(!s.commit(f.token, 1), "second commit blocked");
            eq(1, f.sender.entered.size(), "second commit cannot send input");
        });
        for (vb0.c privacy : new vb0.c[] {vb0.c.PRIVATE, vb0.c.PENDING_OFF, vb0.c.TEMPORARILY_ON}) {
            scenario("native private-history guard delegation " + privacy, () -> {
                Fixture f = new Fixture();
                f.historyModel.d.b = privacy;
                EmojiRecentsBridge.Snapshot s = EmojiRecentsBridge.snapshot(f.sender, 12);
                check(s.commit(f.token, 5), "input works in private mode");
                eq(Arrays.asList(EMOJI[5]), f.sender.entered, "private-mode input exact");
                eq(0, f.history.writes, "delegated native guard suppresses recents write");
                eq(0, f.history.uses, "delegated native guard suppresses usage write");
                eq(1, f.history.notifications, "delegation still reaches native notify path");
                eq(EMOJI[0], f.history.recents.get(0).getContent(), "private mode preserves recent order");
            });
        }
        for (int kind = 0; kind < 5; kind++) {
            final int variant = kind;
            scenario("single-use invalid or stale snapshot " + kind, () -> {
                Fixture f = new Fixture();
                EmojiRecentsBridge.Snapshot s = EmojiRecentsBridge.snapshot(f.sender, 12);
                if (variant == 0) check(!s.commit(null, 0), "null token rejected");
                if (variant == 1) check(!s.commit(f.token, 12), "out-of-range index rejected");
                if (variant == 2) { f.sender.t = new fb0.b0(); check(!s.commit(f.token, 0), "provider replaced"); f.sender.t = f.provider; }
                if (variant == 3) { f.provider.a = new ne0.f0(); check(!s.commit(f.token, 0), "history model replaced"); f.provider.a = f.historyModel; }
                if (variant == 4) { f.historyModel.f = new ne0.e0(); check(!s.commit(f.token, 0), "history list replaced"); f.historyModel.f = f.history; }
                check(!s.commit(f.token, 0), "failed snapshot remains consumed");
                f.noInput();
            });
        }
        scenario("native input failure never retries or updates history", () -> {
            Fixture f = new Fixture();
            EmojiRecentsBridge.Snapshot s = EmojiRecentsBridge.snapshot(f.sender, 12);
            f.sender.failInput = true;
            check(!s.commit(f.token, 0), "throwing native insertion reported failed");
            check(!s.commit(f.token, 0), "partial native insertion cannot be retried");
            eq(1, f.sender.entered.size(), "only one native input attempt");
            eq(0, f.history.writes, "no history after throwing input");
            eq(1, f.history.events.size(), "history callback not reached");
        });
        scenario("history failure cannot resend already accepted input", () -> {
            Fixture f = new Fixture();
            EmojiRecentsBridge.Snapshot s = EmojiRecentsBridge.snapshot(f.sender, 12);
            f.history.failHistory = true;
            check(s.commit(f.token, 1), "accepted input remains successful despite history failure");
            check(!s.commit(f.token, 1), "history failure cannot cause retry");
            eq(1, f.sender.entered.size(), "only one inserted flag sequence");
        });
        scenario("reentrant native input cannot commit snapshot twice", () -> {
            Fixture f = new Fixture();
            final EmojiRecentsBridge.Snapshot s = EmojiRecentsBridge.snapshot(f.sender, 12);
            f.sender.onInput = () -> check(!s.commit(f.token, 3), "reentrant commit blocked before native input");
            check(s.commit(f.token, 0), "outer commit accepted");
            eq(1, f.sender.entered.size(), "one input despite reentrancy");
            eq(1, f.history.writes, "one history update despite reentrancy");
        });
    }

    static void keyAndThresholdTests() {
        scenario("real toolbar key identity, repeated configure and ordinary tap/hold", () -> {
            Fixture f = new Fixture();
            eq(500, EmojiToolbar.emojiHoldMillis(f.key), "exact installed native deadline");
            Object timer = f.builder.f, click = f.builder.d.get(0);
            EmojiToolbar.configure(f.factory, f.builder);
            check(timer == f.builder.f && click == f.builder.d.get(0), "repeated configure preserves actors");
            eq(1, f.builder.f.b.size(), "no duplicate long-press actor");
            f.down();
            check(!f.send(2, f.startX, f.startY - 10, 20), "subthreshold MOVE preserved");
            check(!f.send(1, f.startX, f.startY - 10, 40), "ordinary UP preserved");
            actor(f.builder.d.get(0)).b(f.token);
            eq(1, f.original.calls, "wrapped native ordinary tap invoked once");
            CandidateToolbar.closeResult = true;
            actor(f.builder.d.get(0)).b(f.token);
            eq(1, f.original.calls, "tap closes open toolbar without native opening");
            int toggles = CandidateToolbar.toggles;
            actor(f.builder.f.b.get(0)).b(f.token);
            actor(f.builder.f.d.get(0)).b(f.token);
            eq(toggles + 1, CandidateToolbar.toggles, "stationary native long-press toolbar toggle retained");
            eq(1, f.original.calls, "successful hold release does not open emoji panel");
            CandidateToolbar.toggleResult = false;
            actor(f.builder.f.b.get(0)).b(f.token);
            actor(f.builder.f.d.get(0)).b(f.token);
            eq(2, f.original.calls, "unbound toolbar retains native hold-release fallback");
            f.closed(); f.noInput();
        });
        scenario("ordinary same-location key cannot impersonate emoji action graph", () -> {
            Fixture f = new Fixture();
            ((oc0.m) f.key.b).b.c = new hc0.k(500);
            eq(0, EmojiToolbar.emojiHoldMillis(f.key), "uninstalled timer rejected");
            f.down();
            check(!f.send(2, f.startX, f.startY - 100, 20), "ordinary key stays native");
            check(!f.send(1, f.startX, f.startY - 100, 30), "ordinary key release stays native");
            f.noInput(); f.closed();
            eq(0, EmojiToolbar.emojiHoldMillis(null), "null key safe");
            f.key.b = null;
            eq(0, EmojiToolbar.emojiHoldMillis(f.key), "missing native behavior safe");
        });
        for (int deadlineCase = 0; deadlineCase < 3; deadlineCase++) {
            final int kind = deadlineCase;
            scenario("native hold deadline wins before capture " + kind, () -> {
                Fixture f = new Fixture(); f.down();
                MotionEvent move = f.event(2, f.startX, f.startY - 80, kind == 0 ? 500 : 50);
                if (kind > 0) android.os.SystemClock.now += 500;
                if (kind == 2) move.history = new float[][] {{f.startX, f.startY - 60, f.downTime + 20}};
                check(!f.send(move), "expired native long press not captured");
                check(!f.send(1, f.startX, f.startY - 80, 510), "expired hold release stays native");
                f.closed(); f.noInput();
            });
        }
        // With a 100px key and deliberately excessive 96px platform slop,
        // the upward jitter floor is 25px. Every setting must scale that floor.
        float[] thresholds = {50f, 37.5f, 25f, 20f, 16.25f};
        for (int level = 0; level < 5; level++) {
            final int setting = level;
            scenario("sensitivity threshold moves without platform slop plateau " + level, () -> {
                Fixture f = new Fixture(); f.preferences.level = setting;
                ViewConfiguration.scaledTouchSlop = 96;
                f.down();
                check(!f.send(2, f.startX, f.startY - thresholds[setting] + .1f, 20), "below level-specific threshold remains native");
                check(f.send(2, f.startX, f.startY - thresholds[setting] - .1f, 30), "above level-specific threshold captures");
                eq(0, f.preferences.booleanReads, "independent of character flick enable switch");
                check(f.send(3, f.startX, f.startY, 40), "captured cancellation consumed");
                f.closed(); f.noInput();
            });
        }
        scenario("sensitivity snapshots DOWN and corrupt preference safely defaults", () -> {
            Fixture f = new Fixture(); f.preferences.level = 0; f.down();
            f.preferences.level = 4;
            check(!f.send(2, f.startX, f.startY - 30, 20), "in-progress gesture retains DOWN threshold");
            check(f.send(2, f.startX, f.startY - 49, 30), "original low-sensitivity threshold crossed");
            f.send(3, f.startX, f.startY, 40); f.closed();
            Fixture g = new Fixture(); g.preferences.fail = true; g.down();
            check(!g.send(2, g.startX, g.startY - 23, 20), "corrupt preference defaults below threshold");
            check(g.send(2, g.startX, g.startY - 25, 30), "corrupt preference defaults above threshold");
            g.send(3, g.startX, g.startY, 40); g.closed();
        });
        scenario("horizontal/downward escape and nonfinite points never claim an upward flick", () -> {
            float[][] paths = {{80, 0}, {0, 80}, {Float.NaN, 0}, {0, Float.NEGATIVE_INFINITY}};
            for (float[] path : paths) {
                Fixture f = new Fixture(); f.down();
                check(!f.send(2, f.startX + path[0], f.startY + path[1], 20), "invalid direction stays native");
                check(!f.send(1, f.startX, f.startY - 100, 30), "abandoned path cannot recapture on UP");
                f.noInput(); f.closed();
            }
        });
    }

    static void selectionTests() {
        scenario("continuous slide renders intact recent strings and inserts last item only on UP", () -> {
            Fixture f = new Fixture(); f.capture();
            Canvas canvas = f.draw();
            eq(13, canvas.texts.size(), "header plus twelve recent items");
            eq("最近使った絵文字", canvas.texts.get(0).text, "picker heading");
            for (int i = 0; i < 12; i++) {
                Canvas.Text drawn = canvas.texts.get(i + 1);
                eq(EMOJI[i], drawn.text, "whole grapheme drawn " + i);
                near(f.cell(i)[0], drawn.x, "drawn emoji uses the same local X coordinates as hit selection");
                check(drawn.width <= 38.01f, "measured whole string fits cell interior, including multi-codepoint sequences index=" + i + " width=" + drawn.width);
            }
            for (int i = 0; i < 12; i++) {
                check(f.at(2, i, 30 + i), "slide MOVE consumed");
                f.noInput();
            }
            check(f.at(1, 11, 100), "selected UP consumed");
            eq(Arrays.asList(EMOJI[11]), f.sender.entered, "only final selected recent inserted");
            eq(1, f.history.writes, "selection updates native history once");
            eq(Arrays.asList(0, 3), f.view.nativeActions, "MOVE and UP never reach original native key after capture");
            f.closed();
        });
        scenario("highlight follows finger and clears outside without committing", () -> {
            Fixture f = new Fixture(); f.capture();
            f.at(2, 1, 30);
            int selected = 0;
            for (Canvas.Box box : f.draw().boxes) if (box.color == 0xff205360) selected++;
            eq(1, selected, "exactly one selection fill");
            f.send(2, 5, 5, 40);
            selected = 0;
            for (Canvas.Box box : f.draw().boxes) if (box.color == 0xff205360) selected++;
            eq(0, selected, "selection cleared outside grid");
            check(f.send(1, 5, 5, 50), "outside UP consumed");
            f.closed(); f.noInput();
        });
        scenario("header and unused cells do not select", () -> {
            Fixture f = new Fixture(); f.history.recents.subList(5, f.history.recents.size()).clear(); f.capture();
            check(f.at(1, 7, 50), "empty grid cell release consumed"); f.noInput(); f.closed();
            Fixture g = new Fixture(); g.capture(); RectF panel = g.bounds();
            check(g.send(1, panel.centerX(), panel.top + 5, 50), "header release consumed"); g.noInput(); g.closed();
        });
        scenario("empty native history shows an empty message without fallback emoji", () -> {
            Fixture f = new Fixture(); f.history.recents.clear(); f.capture();
            Canvas canvas = f.draw();
            eq(2, canvas.texts.size(), "heading plus empty history message");
            eq("履歴がありません", canvas.texts.get(1).text, "empty message exact");
            check(f.at(1, 0, 50), "empty picker release consumed");
            f.closed(); f.noInput();
        });
        scenario("DOWN-UP-only flick cannot select an unseen window", () -> {
            Fixture f = new Fixture(); f.down();
            check(f.at(1, 0, 40), "qualifying UP owned");
            eq(Arrays.asList(0, 3), f.view.nativeActions, "native key cancelled before swallowing UP");
            f.closed(); f.noInput();
        });
        scenario("coalesced move history qualifies while current finger position selects", () -> {
            Fixture f = new Fixture(); f.down();
            MotionEvent move = f.event(2, 5, 5, 30);
            move.history = new float[][] {{f.startX, f.startY - 30, f.downTime + 20}};
            check(f.send(move), "valid historical upward segment captures");
            f.noInput();
            check(f.at(1, 3, 50), "current release coordinates select");
            eq(Arrays.asList(EMOJI[3]), f.sender.entered, "release item selected"); f.closed();
        });
        scenario("displayed recent snapshot is stable while native history changes", () -> {
            Fixture f = new Fixture(); f.capture();
            java.util.Collections.reverse(f.history.recents);
            eq(EMOJI[0], f.draw().texts.get(1).text, "display remains snapshotted");
            check(f.at(1, 0, 50), "snapshotted release consumed");
            eq(Arrays.asList(EMOJI[0]), f.sender.entered, "displayed sequence committed despite later native recency change"); f.closed();
        });
    }

    static void cancellationTests() {
        for (int variant = 0; variant < 14; variant++) {
            final int kind = variant;
            scenario("captured lifecycle/editor invalidation drains original stream " + kind, () -> {
                Fixture f = new Fixture(); f.capture();
                if (kind == 0) EmojiRecentsSlide.reset();
                if (kind == 1) f.view.testDetach();
                if (kind == 2) f.service.i = new android.view.inputmethod.EditorInfo();
                if (kind == 3) f.service.h = new Connection();
                if (kind == 4) f.state.composition = "new composing text";
                if (kind == 5) f.state.raw = "new raw input";
                if (kind == 6) f.state.start++;
                if (kind == 7) f.state.end++;
                if (kind == 8) f.view.width++;
                if (kind == 9) f.view.window = new android.os.IBinder() {};
                if (kind == 10) f.view.shown = false;
                if (kind == 11) f.view.A.popup = true;
                if (kind == 12) f.layout.d.clear();
                if (kind == 13) f.key.e.a = new RectF(.7f, .75f, .8f, 1f);
                check(f.at(2, 0, 40), "invalidated captured MOVE consumed");
                check(f.at(1, 0, 60), "late UP drained");
                eq(Arrays.asList(0, 3), f.view.nativeActions, "no late native emoji click");
                f.noInput(); f.closed();
            });
        }
        for (int variant = 0; variant < 5; variant++) {
            final int kind = variant;
            scenario("captured pointer and terminal cancellation " + kind, () -> {
                Fixture f = new Fixture(); f.capture();
                MotionEvent invalid = f.event(kind == 0 ? 3 : kind == 1 ? 5 : 2, f.startX, f.startY - 60, 40);
                if (kind == 1) invalid.count = 2;
                if (kind == 2) invalid.pointer = 3;
                if (kind == 3) invalid.event = f.downTime - 1;
                if (kind == 4) invalid.x = Float.NaN;
                check(f.send(invalid), "invalid captured event consumed");
                if (kind != 0) check(f.at(1, 0, 70), "remaining captured UP consumed");
                f.noInput(); f.closed();
            });
        }
        scenario("editor surrounding text and callback failures cancel selected release", () -> {
            for (int kind = 0; kind < 3; kind++) {
                Fixture f = new Fixture(); f.capture();
                if (kind == 0) f.connection.before = "changed before";
                if (kind == 1) f.connection.after = "changed after";
                if (kind == 2) f.connection.fail = true;
                check(f.at(1, 0, 40), "changed editor release consumed");
                f.noInput(); f.closed();
            }
        });
        scenario("native CANCEL reentry, reset, failure and overlay-add failure cannot leak UP", () -> {
            for (int kind = 0; kind < 4; kind++) {
                Fixture f = new Fixture();
                if (kind == 0) f.view.onCancel = () -> check(!EmojiRecentsSlide.handle(f.view, f.token, f.view.cancel), "recursive synthetic CANCEL stays on native path");
                if (kind == 1) f.view.onCancel = () -> EmojiRecentsSlide.reset();
                if (kind == 2) f.view.throwCancel = true;
                if (kind == 3) f.view.overlay.failAdd = true;
                f.down();
                check(f.send(2, f.startX, f.startY - 30, 20), "capture-side errors still own stream");
                check(f.view.cancel.recycled, "cancel copy recycled on every path");
                check(f.at(1, 0, 50), "captured release cannot fall through after failure");
                if (kind == 0) eq(1, f.sender.entered.size(), "healthy recursive cancel preserves valid selection");
                else f.noInput();
                f.closed();
            }
        });
        scenario("failed overlay removal hides drawable and removes listener", () -> {
            Fixture f = new Fixture(); f.capture();
            f.view.overlay.failRemove = true;
            EmojiRecentsSlide.reset();
            eq(0, f.draw().texts.size(), "unremoved drawable is inert and draws no text");
            eq(0, f.view.attachments.size(), "listener removed despite overlay removal failure");
            check(f.at(1, 0, 50), "late UP still drained");
            f.noInput();
        });
        scenario("new view does not inherit old capture; old-view UP remains drained", () -> {
            Fixture old = new Fixture(); old.capture();
            Fixture next = new Fixture(); next.capture();
            check(old.at(1, 0, 60), "old captured view UP drained independently");
            check(next.at(1, 2, 70), "new view can still commit own gesture");
            old.noInput(); old.closed(); next.closed();
            eq(Arrays.asList(EMOJI[2]), next.sender.entered, "new active gesture survives old terminal event");
        });
        scenario("duplicate DOWN on captured stream is consumed and later UP drained", () -> {
            Fixture f = new Fixture(); f.capture();
            check(f.send(0, f.startX, f.startY, 0), "duplicate captured DOWN swallowed");
            check(f.at(1, 0, 60), "duplicate stream late UP swallowed");
            f.noInput(); f.closed();
        });
        scenario("native insertion may recreate keyboard without resurrecting overlay", () -> {
            Fixture f = new Fixture(); f.capture();
            f.sender.onInput = () -> { f.closed(); EmojiRecentsSlide.reset(); };
            check(f.at(1, 0, 60), "successful selection consumed during recreation");
            eq(1, f.sender.entered.size(), "one insert during synchronous recreation");
            f.closed();
        });
        scenario("cleanup callback changing editor or lifecycle prevents stale input", () -> {
            for (int hook = 0; hook < 2; hook++) for (int mutation = 0; mutation < 3; mutation++) {
                final int change = mutation;
                Fixture f = new Fixture(); f.capture();
                Runnable duringCleanup = () -> {
                    if (change == 0) f.service.i = new android.view.inputmethod.EditorInfo();
                    if (change == 1) f.service.h = new Connection();
                    if (change == 2) EmojiRecentsSlide.reset();
                };
                if (hook == 0) f.view.overlay.onRemove = duringCleanup;
                else f.parent.onRelease = duringCleanup;
                check(f.at(1, 0, 60), "UP remains consumed after cleanup callback switches context");
                eq(Arrays.asList(0, 3), f.view.nativeActions, "cleanup reentrancy cannot expose native UP");
                f.closed(); f.noInput();
            }
        });
    }

    static void availabilityAndGeometryTests() {
        for (int kind = 0; kind < 12; kind++) {
            final int variant = kind;
            scenario("pre-capture eligibility guard preserves native events " + kind, () -> {
                Fixture f = new Fixture();
                if (variant == 0) f.service.i.inputType = 2;
                if (variant == 1) f.service.i.inputType = 129;
                if (variant == 2) f.service.i.inputType = 145;
                if (variant == 3) f.service.i.inputType = 225;
                if (variant == 4) f.view.attached = false;
                if (variant == 5) f.view.shown = false;
                if (variant == 6) f.view.window = null;
                if (variant == 7) f.layout.d.add(f.key);
                if (variant == 8) f.builder.f.a = 0;
                if (variant == 9) f.state.composition = null;
                if (variant == 10) f.history.failRead = true;
                if (variant == 11) f.sender.t = null;
                f.down();
                check(!f.send(2, f.startX, f.startY - 60, 20), "guarded MOVE stays native");
                check(!f.send(1, f.startX, f.startY - 60, 40), "guarded UP stays native");
                f.noInput(); f.closed();
            });
        }
        scenario("unsupported input tool and missing breadcrumb preserve original dispatch", () -> {
            Fixture f = new Fixture(); MotionEvent down = f.event(0, f.startX, f.startY, 0); down.tool = 3;
            check(!f.send(down), "mouse/tool DOWN preserved");
            check(!f.send(2, f.startX, f.startY - 60, 20), "unsupported tool never captured"); f.noInput();
            Fixture g = new Fixture();
            check(!EmojiRecentsSlide.handle(g.view, null, g.event(0, g.startX, g.startY, 0)), "null breadcrumb DOWN preserved");
            check(!g.send(2, g.startX, g.startY - 60, 20), "missing breadcrumb cannot begin capture"); g.noInput();
        });
        scenario("stylus shares continuous release path without requesting focus", () -> {
            Fixture f = new Fixture(); MotionEvent down = f.event(0, f.startX, f.startY, 0); down.tool = 2;
            check(!f.send(down), "stylus DOWN native");
            MotionEvent move = f.event(2, f.startX, f.startY - 30, 20); move.tool = 2;
            check(f.send(move), "stylus upward MOVE captures");
            check(f.view.focused, "original keyboard focus retained");
            f.send(3, f.startX, f.startY, 30); f.closed(); f.noInput();
        });
        scenario("popup bounds fit original view for landscape, portrait, edge keys and densities", () -> {
            int[][] sizes = {{360, 250}, {1000, 400}, {1600, 300}, {220, 200}};
            for (int[] size : sizes) for (float density : new float[] {1, 1.5f, 2}) for (int count : new int[] {0, 1, 4, 5, 12}) {
                for (float x : new float[] {0, size[0] * .5f, size[0] - 50}) {
                    RectF key = new RectF(x, size[1] - 70, x + 50, size[1]);
                    RectF panel = EmojiRecentsSlide.popupBounds(size[0], size[1], key, density, count);
                    if (panel == null) continue;
                    check(panel.left >= 4 * density - .01f && panel.top >= 4 * density - .01f, "panel starts inside original view margin");
                    check(panel.right <= size[0] - 4 * density + .01f && panel.bottom <= size[1] - 4 * density + .01f, "panel ends inside original view margin");
                    check(panel.width() >= (16 + 96) * density - .01f, "four cells retain minimum width");
                    int rows = Math.max(1, (count + 3) / 4);
                    check((panel.height() - 44 * density) / rows >= 24 * density - .01f, "rows retain minimum hit target height");
                }
            }
            RectF key = new RectF(0, 0, 40, 40);
            check(EmojiRecentsSlide.popupBounds(100, 100, key, 1, 12) == null, "insufficient width safely declines picker");
            check(EmojiRecentsSlide.popupBounds(360, 60, key, 1, 12) == null, "insufficient height safely declines picker");
            check(EmojiRecentsSlide.popupBounds(360, 400, key, Float.NaN, 12) == null, "invalid density rejected");
            check(EmojiRecentsSlide.popupBounds(360, 400, key, 1, 13) == null, "out-of-contract item count rejected");
        });
        scenario("many gestures install and remove exactly one drawable and attach listener", () -> {
            for (int i = 0; i < 20; i++) {
                Fixture f = new Fixture(); f.capture(); f.at(1, i % 12, 40); f.closed();
                eq(1, f.view.overlay.adds, "one add per gesture");
                eq(1, f.view.overlay.removes, "one removal per gesture");
                eq(1, f.sender.entered.size(), "one input per gesture");
            }
        });
    }

    public static void main(String[] args) {
        bridgeTests();
        keyAndThresholdTests();
        selectionTests();
        cancellationTests();
        availabilityAndGeometryTests();
        eq(MotionEvent.copies, MotionEvent.recycles, "all synthetic native CANCEL events recycled");
        System.out.println("PASS EmojiSlide scenarios=" + scenarios + " assertions=" + assertions);
    }
}
