package hiro.swiftkey;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;

/** Candidate update bridge for SwiftKey Beta 9.13.16.4's expanded grid. */
public final class CandidateScroll {
    private static final Object CANDIDATES_UPDATED = new Object();
    private static final WeakHashMap<rb0.h, Snapshot> snapshots = new WeakHashMap<>();

    private CandidateScroll() {}

    /**
     * Keep an already displayed expanded result stable while more words arrive.
     * The emoji supplement leaves several matches at the end of the result;
     * without this bridge each page places new words before that old tail, which
     * native rb0.j classifies as a reset. Initial results are never reordered.
     */
    public static ib0.a stabilize(rb0.j provider, ib0.a incoming) {
        if (provider == null || incoming == null) return incoming;
        try {
            List<?> previous = provider.b, fresh = incoming.a;
            if (incoming.b != ib0.m.g || previous == null || fresh == null
                    || previous.isEmpty() || fresh.size() <= previous.size()
                    || !(previous.get(0) instanceof m40.b)
                    || !sameQuery(((m40.b) previous.get(0)).subrequest(), incoming.c)) return incoming;

            // Ordinary append already takes the native append path unchanged.
            boolean stable = true;
            for (int i = 0; i < previous.size(); i++) {
                if (!(previous.get(i) instanceof m40.b) || !(fresh.get(i) instanceof m40.b)
                        || !sameCandidate((m40.b) previous.get(i), (m40.b) fresh.get(i))) {
                    stable = false;
                    break;
                }
            }
            if (stable) return incoming;

            HashMap<CandidateKey, ArrayDeque<Integer>> positions = new HashMap<>();
            for (int i = 0; i < fresh.size(); i++) {
                if (!(fresh.get(i) instanceof m40.b)) return incoming;
                CandidateKey key = new CandidateKey((m40.b) fresh.get(i));
                if (!key.valid()) return incoming;
                ArrayDeque<Integer> queue = positions.get(key);
                if (queue == null) {
                    queue = new ArrayDeque<>();
                    positions.put(key, queue);
                }
                queue.addLast(Integer.valueOf(i));
            }
            ArrayList<m40.b> ordered = new ArrayList<>(fresh.size());
            boolean[] selected = new boolean[fresh.size()];
            for (Object value : previous) {
                if (!(value instanceof m40.b)) return incoming;
                ArrayDeque<Integer> queue = positions.get(new CandidateKey((m40.b) value));
                if (queue == null || queue.isEmpty()) return incoming;
                int index = queue.removeFirst().intValue();
                selected[index] = true;
                // Never reuse an old native commit/long-press actor or context.
                ordered.add((m40.b) fresh.get(index));
            }
            for (int i = 0; i < fresh.size(); i++) {
                if (!selected[i]) ordered.add((m40.b) fresh.get(i));
            }
            return new ib0.a(incoming.d, ordered, incoming.b, incoming.c);
        } catch (RuntimeException | LinkageError ignored) {
            return incoming;
        }
    }

    private static boolean sameQuery(l40.c old, l40.c next) {
        // Match native l40.c.equals, except g (the result limit increases while
        // expanding). Compare the native semantic Sequence/TouchHistory equals,
        // rather than relying on the composing spelling alone.
        return old != null && next != null && next.k != null && !next.k.isEmpty()
                && next.d == com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE
                && old.d == next.d && old.f == next.f && old.n == next.n
                && Objects.equals(old.a, next.a) && Objects.equals(old.b, next.b)
                && Objects.equals(old.c, next.c) && Objects.equals(old.e, next.e)
                && Objects.equals(old.h, next.h) && Arrays.equals(old.j, next.j)
                && old.p == next.p
                && Objects.equals(old.k, next.k) && Objects.equals(old.l, next.l)
                && Objects.equals(old.m, next.m) && Objects.equals(old.i, next.i);
    }

    private static boolean sameCandidate(m40.b left, m40.b right) {
        return Objects.equals(left.getUserFacingText(), right.getUserFacingText())
                && Objects.equals(left.getCorrectionSpanReplacementText(), right.getCorrectionSpanReplacementText())
                && Objects.equals(left.getPredictionInput(), right.getPredictionInput())
                && Objects.equals(left.getTrailingSeparator(), right.getTrailingSeparator());
    }

    /** Temporary per-call matching key; never stored in the UI geometry map. */
    private static final class CandidateKey {
        final String display, commit, input, separator;
        CandidateKey(m40.b candidate) {
            display = candidate.getUserFacingText();
            commit = candidate.getCorrectionSpanReplacementText();
            input = candidate.getPredictionInput();
            separator = candidate.getTrailingSeparator();
        }
        boolean valid() { return display != null && commit != null && input != null; }
        @Override public int hashCode() { return Objects.hash(display, commit, input, separator); }
        @Override public boolean equals(Object value) {
            if (!(value instanceof CandidateKey)) return false;
            CandidateKey other = (CandidateKey) value;
            return Objects.equals(display, other.display) && Objects.equals(commit, other.commit)
                    && Objects.equals(input, other.input) && Objects.equals(separator, other.separator);
        }
    }

    /** Called after the original full refresh; stores geometry, never candidate text. */
    public static synchronized void snapshot(rb0.h adapter) {
        if (adapter == null) return;
        snapshots.remove(adapter);
        try {
            disableItemAnimations(adapter);
            Snapshot value = read(adapter);
            if (value != null) snapshots.put(adapter, value);
        } catch (RuntimeException | LinkageError ignored) {
            // An unknown native state retains SwiftKey's full refresh behavior.
        }
    }

    /**
     * rb0.j emits reset=false only when an expanded-search result preserves the
     * complete previous text prefix. All other providers and updates use native
     * d(reset). A successful append keeps native keys, positions and bindings.
     */
    public static synchronized boolean tryAppend(rb0.h adapter, boolean reset) {
        if (adapter == null) return false;
        Snapshot old = snapshots.remove(adapter);
        try {
            // Cover reset and first-update fallbacks as well as incremental
            // append. The native full refresh must not restore row animations.
            disableItemAnimations(adapter);
            if (reset || old == null) return false;
            if (!adapter.r || !adapter.o || adapter.g == null
                    || !(adapter.g.d instanceof rb0.j)) return false;
            GridLayoutManager layout = adapter.l;
            RecyclerView view = layout == null ? null : layout.b;
            if (view == null || !view.isAttachedToWindow() || !view.isShown()
                    || view.c0() || view.getAdapter() != adapter
                    || view.getLayoutManager() != layout
                    || old.layout.get() != layout || old.view.get() != view
                    || old.provider.get() != adapter.g.d
                    || old.lookup.get() != layout.L
                    || old.theme.get() != adapter.e.b()
                    || old.columns != layout.G || old.width != view.getWidth()
                    || old.height != view.getHeight()
                    || old.keyHeight != keyHeight(adapter)) return false;
            int count = adapter.j();
            if (count <= old.spans.length || count != adapter.g.d.d()) return false;

            // Use native width measurement and row packing. In particular the
            // old final row may lose its padding as appended words fill it.
            adapter.G();
            Snapshot next = read(adapter);
            if (next == null || next.spans.length != count
                    || !sameEnvironment(old, next)) return false;
            int finalRow = lastRowStart(old.spans, old.columns);
            if (finalRow < 0) return false;
            for (int i = 0; i < finalRow; i++) {
                if (old.spans[i] != next.spans[i]) return false;
            }
            // rb0.j rebuilds native key objects even for an unchanged text
            // prefix. Rebind the old range so commit and long-press metadata
            // stay current. A non-null payload allows existing holders to be
            // reused while preserving the native binding condition and code.
            // This also remeasures the old last row when padding changes, and
            // avoids notifyDataSetChanged's full invalidation/change animation.
            adapter.a.d(0, old.spans.length, CANDIDATES_UPDATED);
            adapter.q(old.spans.length, count - old.spans.length);
            snapshots.put(adapter, next);
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            // d(reset) continues into native H()/notifyDataSetChanged. That also
            // replaces any partially rebuilt lookup after a native failure.
            return false;
        }
    }

    /**
     * Apply only to the RecyclerView still owned by this candidate adapter.
     * Native move animations draw newly measured widths at the previous row's
     * translated position. A payload reuses the holder but does not stop those
     * moves. Removing the animator also disables RecyclerView's predictive
     * pre-layout for updates, so width and position take effect together.
     *
     * setItemAnimator(null) ends any running native item animations first. It
     * does not change scroll position, fling state, adapter, layout manager,
     * candidate bindings or the native accessibility adapter observer.
     */
    private static void disableItemAnimations(rb0.h adapter) {
        GridLayoutManager layout = adapter.l;
        RecyclerView view = layout == null ? null : layout.b;
        if (view != null && view.getAdapter() == adapter
                && view.getLayoutManager() == layout && view.getItemAnimator() != null) {
            view.setItemAnimator(null);
        }
    }

    private static boolean sameEnvironment(Snapshot left, Snapshot right) {
        return left.layout.get() == right.layout.get()
                && left.view.get() == right.view.get()
                && left.provider.get() == right.provider.get()
                && left.theme.get() == right.theme.get()
                && left.columns == right.columns && left.width == right.width
                && left.height == right.height && left.keyHeight == right.keyHeight;
    }

    private static int keyHeight(rb0.h adapter) {
        Object value = adapter.h.get();
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    private static Snapshot read(rb0.h adapter) {
        if (!adapter.r || adapter.g == null || !(adapter.g.d instanceof rb0.j)) return null;
        GridLayoutManager layout = adapter.l;
        RecyclerView view = layout == null ? null : layout.b;
        if (view == null || view.getAdapter() != adapter
                || view.getLayoutManager() != layout || !(layout.L instanceof rb0.g)) return null;
        int count = adapter.j(), columns = layout.G, height = keyHeight(adapter);
        if (count <= 0 || count != adapter.g.d.d() || columns <= 0 || height <= 0) return null;
        ArrayList<?> packed = ((rb0.g) layout.L).e;
        if (packed == null || packed.size() != count) return null;
        int[] spans = new int[count];
        for (int i = 0; i < count; i++) {
            Object value = packed.get(i);
            if (!(value instanceof Integer)) return null;
            spans[i] = ((Integer) value).intValue();
        }
        if (lastRowStart(spans, columns) < 0) return null;
        Object theme = adapter.e.b();
        if (theme == null) return null;
        return new Snapshot(layout, view, adapter.g.d, layout.L, theme, columns,
                view.getWidth(), view.getHeight(), height, spans);
    }

    /** Native G() pads every row, including the final one, to the column count. */
    private static int lastRowStart(int[] spans, int columns) {
        int start = -1, used = 0;
        for (int i = 0; i < spans.length; i++) {
            int span = spans[i];
            if (span <= 0 || span > columns) return -1;
            if (used == 0) start = i;
            used += span;
            if (used > columns) return -1;
            if (used == columns) used = 0;
        }
        return used == 0 ? start : -1;
    }

    private static final class Snapshot {
        final WeakReference<GridLayoutManager> layout;
        final WeakReference<RecyclerView> view;
        final WeakReference<Object> provider, lookup, theme;
        final int columns, width, height, keyHeight;
        final int[] spans;

        Snapshot(GridLayoutManager layout, RecyclerView view, Object provider, Object lookup,
                Object theme, int columns, int width, int height, int keyHeight, int[] spans) {
            this.layout = new WeakReference<>(layout);
            this.view = new WeakReference<>(view);
            this.provider = new WeakReference<>(provider);
            this.lookup = new WeakReference<>(lookup);
            this.theme = new WeakReference<>(theme);
            this.columns = columns;
            this.width = width;
            this.height = height;
            this.keyHeight = keyHeight;
            this.spans = spans;
        }
    }
}
