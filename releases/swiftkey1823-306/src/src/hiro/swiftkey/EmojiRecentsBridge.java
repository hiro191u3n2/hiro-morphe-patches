package hiro.swiftkey;

import java.util.ArrayList;

/**
 * Uses the current keyboard model's own emoji history and insertion actions.
 * All obfuscated members are pinned to the supported SwiftKey Beta build.
 * There is no alternate preference file, reflection, or direct editor commit.
 */
public final class EmojiRecentsBridge {
    private static final int MAX_VISIBLE = 12;

    private EmojiRecentsBridge() {}

    /**
     * Copies the native newest-first list for one gesture. A missing native
     * graph disables the gesture; an empty native list remains empty.
     */
    public static Snapshot snapshot(wb0.x sender, int requestedLimit) {
        if (sender == null || requestedLimit <= 0) return null;
        try {
            fb0.b0 provider = sender.t;
            if (provider == null || !(provider.a instanceof ne0.f0)) return null;
            ne0.f0 history = (ne0.f0) provider.a;
            ne0.e0 recents = history.f;
            if (recents == null) return null;

            // ne0.e0.b() already reverses the insertion-ordered native map.
            // Do not reverse it again, split graphemes, or substitute defaults.
            ArrayList nativeItems = recents.b();
            if (nativeItems == null) return null;
            int limit = Math.min(requestedLimit, MAX_VISIBLE);
            ArrayList<String> strings = new ArrayList<String>(limit);
            for (Object item : nativeItems) {
                if (!(item instanceof ne0.g0)) continue;
                String content = ((ne0.g0) item).getContent();
                if (content == null || content.length() == 0) continue;
                strings.add(content);
                if (strings.size() == limit) break;
            }
            return new Snapshot(sender, provider, history, recents,
                    strings.toArray(new String[strings.size()]));
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    /** Immutable strings and native identities retained only for one gesture. */
    public static final class Snapshot {
        private final wb0.x sender;
        private final fb0.b0 provider;
        private final ne0.f0 history;
        private final ne0.e0 recents;
        private final String[] strings;
        private boolean used;

        private Snapshot(wb0.x sender, fb0.b0 provider, ne0.f0 history,
                ne0.e0 recents, String[] strings) {
            this.sender = sender;
            this.provider = provider;
            this.history = history;
            this.recents = recents;
            this.strings = strings;
        }

        public int size() { return strings.length; }

        public String get(int index) { return strings[index]; }

        /**
         * Makes at most one insertion attempt, including when native code
         * throws. The caller separately validates its current editor/session.
         * Returns true once the native insertion action returns normally.
         */
        public synchronized boolean commit(lh0.a breadcrumb, int index) {
            if (used) return false;
            used = true;
            if (breadcrumb == null || index < 0 || index >= strings.length) return false;

            try {
                if (sender.t != provider || provider.a != history || history.f != recents) {
                    return false;
                }
                // This is the native emoji action, which handles composing
                // text through SwiftKey. No commitText fallback or retry.
                sender.G(breadcrumb, strings[index], oz.j2.a);
            } catch (RuntimeException | LinkageError ignored) {
                return false;
            }

            try {
                // Native i70.p1 uses this same insertion-before-history order.
                // l() updates recency, usage, and native observers; its m()/o()
                // preserve the native vb0.c.b() incognito suppression guard.
                new xc.d(history, 14).l(new ne0.p0(strings[index]), 1);
            } catch (RuntimeException | LinkageError ignored) {
                // Text has already been handed to the native insertion path.
                // History failure must never cause a second insertion attempt.
            }
            return true;
        }
    }
}
