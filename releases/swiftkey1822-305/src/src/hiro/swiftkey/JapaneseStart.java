package hiro.swiftkey;

import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import com.touchtype.KeyboardService;
import java.util.Set;
import java.util.WeakHashMap;

/** Select an enabled Japanese layout once when a normal text keyboard opens. */
public final class JapaneseStart {
    // Values contain only lifecycle flags, never editor identities, text or owners.
    private static final WeakHashMap<KeyboardService, Session> SESSIONS = new WeakHashMap<KeyboardService, Session>();
    private static final class Session {
        boolean pending;
        boolean selecting;
    }
    private JapaneseStart() {}

    private static boolean mainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void begin(KeyboardService service, EditorInfo editor, boolean restarting) {
        if (service == null || !mainThread()) return;
        Session session = SESSIONS.get(service);
        if (session != null && session.selecting) return;
        boolean fresh = session == null || !restarting;
        if (session == null) {
            session = new Session();
            SESSIONS.put(service, session);
        }
        // A restart in an open session must not undo a manual language switch.
        // finish() removes the session, so a reopened restarting=true is fresh.
        session.pending = (session.pending || fresh) && editor != null && (editor.inputType & 15) == 1
                && (editor.imeOptions & 0x80000000) == 0;
    }

    public static void finish(KeyboardService service) {
        if (service != null && mainThread()) SESSIONS.remove(service);
    }

    public static qj0.a select(fb0.i0 layout, fb0.f5 languages, qj0.a original) {
        if (!mainThread() || layout == null || layout.a == null) return original;
        Session session = SESSIONS.get(layout.a);
        if (session == null || !session.pending || session.selecting) return original;
        // Consume before any native calls, including queries that can reenter.
        session.pending = false;
        session.selecting = true;
        try {
            if (languages == null || languages != layout.E || original == null
                    || layout.k == null || (layout.k.a != 1 && layout.k.a != 2)
                    || languages.b == null) return original;
            qj0.a selected = null;
            for (Object value : languages.b) {
                if (value != qj0.a.x4 && value != qj0.a.y4) continue;
                qj0.a candidate = (qj0.a) value;
                Set<?> packs = languages.k(candidate);
                if (packs == null || packs.isEmpty()) continue;
                if (candidate == original) { selected = candidate; break; }
                if (selected == null) selected = candidate;
            }
            if (selected == null || selected == original) return original;
            // Native layout selection updates its current index and preference.
            // The original g0.b body then resets secondary mode, assigns m,
            // builds the configured ROMAJI/HIRAGANA layout and notifies listeners.
            languages.q(selected);
            return languages.i() == selected ? selected : original;
        } catch (RuntimeException ignored) {
            return original;
        } catch (LinkageError ignored) {
            return original;
        } finally {
            session.selecting = false;
        }
    }
}
