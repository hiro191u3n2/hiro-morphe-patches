package hiro.swiftkey;

import com.touchtype.KeyboardService;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds toolbar access to the native dedicated emoji key action graph.
 *
 * The pinned fc0.h factory is called while the native hc0.b action builder is
 * still mutable. Adding hc0.k here lets SwiftKey's own oc0.p delegate own the
 * timer, slide-out/cancel handling, and suppression of CLICK after LONGPRESS.
 * No key coordinates, touch interceptor, polling, or text data are involved.
 */
public final class EmojiToolbar {
    private EmojiToolbar() {}

    /**
     * Identifies the dedicated emoji key by the installed native action graph,
     * and returns its current native hold deadline. Labels and coordinates are
     * deliberately not used: the same glyph may appear on ordinary text keys.
     */
    public static int emojiHoldMillis(fc0.n2 key) {
        try {
            if (key == null || !(key.b instanceof oc0.m)) return 0;
            hc0.a handler = ((oc0.m) key.b).b;
            if (handler == null || handler.c == null || handler.c.a <= 0) return 0;
            return installed(handler.c) ? handler.c.a : 0;
        } catch (RuntimeException | LinkageError ignored) {
            return 0;
        }
    }

    private static final cm0.d ALWAYS = new cm0.d() {
        public Object invoke(Object ignored) { return Boolean.TRUE; }
    };

    /** Hooked at both returns of fc0.h.invoke; discriminator 1 is untouched. */
    public static void configure(fc0.h factory, Object value) {
        KeyboardService service = null;
        try {
            if (factory == null || factory.b == null || factory.b.a == null) return;
            service = factory.b.a;
            if (factory.a != 0) {
                if (factory.a == 1) CandidateToolbar.setEmojiAccess(service, false);
                return;
            }
            if (!(value instanceof hc0.b)) { disable(service); return; }
            hc0.b builder = (hc0.b) value;
            if (builder.a == null) { disable(service); return; }
            if (builder.f != null) {
                if (installed(builder.f)) CandidateToolbar.setEmojiAccess(service, true);
                else disable(service);
                return;
            }
            int timeout = factory.b.w;
            if (timeout <= 0) { disable(service); return; }

            // Native ordinary CLICK, accessibility click, and accessible
            // release contain the same exact feature-opening actor. Keep each
            // restriction and all surrounding native feedback/reset actors.
            Prepared click = prepare(builder.d, service);
            Prepared accessible = prepare(builder.a.f, service);
            Prepared release = prepare(builder.e, service);
            if (click == null || accessible == null || release == null
                    || click.count + accessible.count + release.count == 0) {
                disable(service);
                return;
            }

            // Equivalent to hc0.b.q(timeout, ALWAYS, LongPress), assembled
            // before touching the live builder so validation is atomic.
            ic0.a fallback = click.original != null ? click.original
                    : accessible.original != null ? accessible.original : release.original;
            LongPress hold = new LongPress(service, fallback);
            hc0.k longPress = new hc0.k(timeout);
            longPress.b.add(new hc0.o(ALWAYS, new ic0.e(hold)));
            // If the keyboard is being recreated and its toolbar is not bound,
            // retain the old hold-then-release emoji behavior. Native CANCEL
            // and slide-out suppress LONGCLICK, so cancellation cannot open it.
            longPress.d.add(new hc0.o(ALWAYS, new ic0.e(new LongRelease(hold))));

            builder.d = click.values;
            builder.a.f = accessible.values;
            builder.e = release.values;
            builder.f = longPress;
            CandidateToolbar.setEmojiAccess(service, true);
        } catch (RuntimeException | LinkageError ignored) {
            // Keep the original emoji action available when a bridge fails.
            disable(service);
        }
    }

    private static void disable(KeyboardService service) {
        if (service == null) return;
        try { CandidateToolbar.setEmojiAccess(service, false); }
        catch (RuntimeException | LinkageError ignored) {}
    }

    private static boolean installed(hc0.k timer) {
        if (timer.b == null) return false;
        for (Object item : timer.b) {
            if (!(item instanceof hc0.o)) continue;
            Object actor = ((hc0.o) item).b;
            if (actor instanceof ic0.e && ((ic0.e) actor).a instanceof LongPress) return true;
        }
        return false;
    }

    private static Prepared prepare(Object value, KeyboardService service) {
        if (value == null) return new Prepared(null, 0, null);
        if (!(value instanceof ArrayList)) return null;
        List source = (List) value;
        ArrayList result = new ArrayList(source.size());
        int count = 0;
        ic0.a original = null;
        for (Object item : source) {
            if (!(item instanceof hc0.o)) return null;
            hc0.o entry = (hc0.o) item;
            Object actor = entry.b;
            boolean wrapped = actor instanceof ic0.e;
            ic0.a plain = wrapped ? ((ic0.e) actor).a
                    : actor instanceof ic0.a ? (ic0.a) actor : null;
            if (plain instanceof Tap || plain instanceof LongPress) return null;
            if (plain instanceof ic0.a0 && ((ic0.a0) plain).a == oz.i6.b) {
                if (entry.a == null) return null;
                ic0.a replacement = new Tap(service, plain);
                result.add(new hc0.o(entry.a, wrapped ? new ic0.e(replacement) : replacement));
                ++count;
                if (original == null) original = plain;
            } else {
                result.add(item);
            }
        }
        return new Prepared(result, count, original);
    }

    private static final class Prepared {
        final ArrayList values;
        final int count;
        final ic0.a original;
        Prepared(ArrayList values, int count, ic0.a original) {
            this.values = values; this.count = count; this.original = original;
        }
    }

    private static final class Tap implements ic0.a {
        final KeyboardService service;
        final ic0.a original;
        Tap(KeyboardService service, ic0.a original) {
            this.service = service;
            this.original = original;
        }
        public void b(lh0.a breadcrumb) {
            boolean consumed = false;
            try { consumed = CandidateToolbar.closeIfOpen(service); }
            catch (RuntimeException | LinkageError ignored) {}
            if (!consumed) original.b(breadcrumb);
        }
    }

    private static final class LongPress implements ic0.a {
        final KeyboardService service;
        final ic0.a fallback;
        boolean fallbackOnRelease;
        LongPress(KeyboardService service, ic0.a fallback) {
            this.service = service; this.fallback = fallback;
        }
        public void b(lh0.a breadcrumb) {
            fallbackOnRelease = true;
            try { fallbackOnRelease = !CandidateToolbar.toggle(service); }
            catch (RuntimeException | LinkageError ignored) {}
        }
    }

    private static final class LongRelease implements ic0.a {
        final LongPress hold;
        LongRelease(LongPress hold) { this.hold = hold; }
        public void b(lh0.a breadcrumb) {
            if (!hold.fallbackOnRelease) return;
            hold.fallbackOnRelease = false;
            hold.fallback.b(breadcrumb);
        }
    }
}
