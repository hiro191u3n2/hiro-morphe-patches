package hiro.swiftkey;

import android.os.Looper;
import android.view.View;
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
    private static final WeakHashMap<rb0.j, Object> updateEpochs = new WeakHashMap<>();
    private static final WeakHashMap<rb0.j, UpdatePlan> preparedUpdates = new WeakHashMap<>();
    private static final WeakHashMap<rb0.h, Viewport> pendingViewports = new WeakHashMap<>();
    private static final ThreadLocal<UpdatePlan> activeUpdate = new ThreadLocal<>();

    private CandidateScroll() {}

    /**
     * Keep an already displayed expanded result stable while more words arrive.
     * The emoji supplement leaves several matches at the end of the result;
     * without this bridge each page places new words before that old tail, which
     * native rb0.j classifies as a reset. Initial results are never reordered.
     */
    public static ib0.a stabilize(rb0.j provider, ib0.a incoming) {
        Object epoch = beginUpdate(provider);
        ib0.a result = stabilizeOrder(provider, incoming);
        try {
            prepareUpdate(provider, result, epoch);
        } catch (RuntimeException | LinkageError ignored) {
            finishUpdate(provider);
        }
        return result;
    }

    private static ib0.a stabilizeOrder(rb0.j provider, ib0.a incoming) {
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

    private static synchronized Object beginUpdate(rb0.j provider) {
        if (provider == null) return null;
        preparedUpdates.remove(provider);
        Object epoch = new Object();
        updateEpochs.put(provider, epoch);
        return epoch;
    }

    /** Also called on native early-return paths that do not notify listeners. */
    public static synchronized void finishUpdate(rb0.j provider) {
        if (provider != null) preparedUpdates.remove(provider);
    }

    /**
     * Only this synchronous native dispatch may suppress the panel's reset.
     * Native listener order is unspecified. Always keep both panel and adapter
     * updates, and never leave an active scope after a nested/throwing callback.
     */
    public static void dispatchUpdate(rb0.j provider, fb0.u notifier, boolean reset) {
        UpdatePlan plan;
        synchronized (CandidateScroll.class) {
            plan = preparedUpdates.remove(provider);
        }
        UpdatePlan outer = activeUpdate.get();
        boolean completed = false;
        try {
            boolean eligible = false;
            try {
                eligible = onMainThread() && plan != null && plan.matches(provider, notifier);
            } catch (RuntimeException | LinkageError ignored) {
                // Unknown native state keeps the original reset/scroll path.
            }
            activeUpdate.set(eligible && reset ? plan : null);
            notifier.h(reset);
            if (eligible && !reset && plan.carried) {
                try {
                    // An ordinary append can arrive before the pending layout.
                    // Rebase its logical positions onto the latest native list.
                    if (plan.matches(provider, notifier)) applyViewport(plan);
                } catch (RuntimeException | LinkageError ignored) {
                    discardViewport(plan);
                }
            }
            completed = true;
        } finally {
            if (!completed && plan != null) discardViewport(plan);
            if (outer == null) activeUpdate.remove(); else activeUpdate.set(outer);
        }
    }

    /** Replaces only rb0.k.a()'s final reset-to-zero call, not other scrolling. */
    public static void scrollPanelToStart(rb0.k panel, RecyclerView view, int position) {
        UpdatePlan plan = activeUpdate.get();
        try {
            if (position == 0 && panel != null && onMainThread() && plan != null
                    && panel.m == plan.adapter.get() && panel.l == view
                    && plan.viewport.view.get() == view
                    && panel.n != null && panel.n.b == plan.viewport.layout.get()
                    && plan.matches(plan.provider.get(), plan.notifier.get())) {
                plan.viewport.listener = new WeakReference<>(panel.n);
                if (applyViewport(plan)) return;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Preserve the exact native call when an anchor cannot be trusted.
        }
        if (panel != null) {
            synchronized (CandidateScroll.class) { pendingViewports.remove(panel.m); }
        }
        view.w0(position);
    }

    /**
     * A restored layout reports a zero-distance visible-range change. Native
     * rb0.l otherwise treats its reset watermark as new scrolling and fetches
     * again. Consume only our completed-layout token, then let native b() run.
     * Real movement always retains the native request-more behavior.
     */
    public static synchronized void rebaseScrollWatermark(rb0.l listener, RecyclerView view, int dx, int dy) {
        if (!onMainThread() || listener == null || view == null) return;
        rb0.h adapter = null;
        try {
            if (!(view.getAdapter() instanceof rb0.h)) return;
            adapter = (rb0.h) view.getAdapter();
            Viewport pending = pendingViewports.get(adapter);
            if (pending == null) return;
            if (pending.listener.get() != listener) return;
            if (dx != 0 || dy != 0) { pendingViewports.remove(adapter); return; }
            Object owner = pending.provider.get();
            if (!(owner instanceof rb0.j)) { pendingViewports.remove(adapter); return; }
            rb0.j provider = (rb0.j) owner;
            GridLayoutManager layout = adapter.l;
            if (listener.b != layout || pending.view.get() != view
                    || !pending.matches(adapter, provider) || pending.list.get() != provider.b
                    || updateEpochs.get(provider) != pending.epoch) {
                pendingViewports.remove(adapter);
                return;
            }
            if (layout.x != -1 || layout.y != Integer.MIN_VALUE) return;
            pendingViewports.remove(adapter);
            int last = layout.W0();
            if (last >= 0 && last < provider.b.size()) listener.c = last;
        } catch (RuntimeException | LinkageError ignored) {
            if (adapter != null) pendingViewports.remove(adapter);
        }
    }

    private static boolean onMainThread() {
        Looper main = Looper.getMainLooper();
        return main != null && Looper.myLooper() == main;
    }

    private static synchronized void prepareUpdate(rb0.j provider, ib0.a incoming, Object epoch) {
        if (provider == null || incoming == null || epoch == null || !onMainThread()) return;
        List<?> previous = provider.b, fresh = incoming.a;
        if (incoming.b != ib0.m.g || previous == null || previous.isEmpty()
                || fresh == null || fresh.isEmpty() || !(previous.get(0) instanceof m40.b)
                || !sameQuery(((m40.b) previous.get(0)).subrequest(), incoming.c)) {
            invalidateViewports(provider);
            return;
        }
        // Native j.a returns without swapping the list or dispatching when
        // count and commit text are unchanged. Do not prepare a future reset.
        boolean prefix = sameCommitPrefix(previous, fresh);
        if (previous.size() == fresh.size() && prefix) {
            // No native swap/dispatch follows. Keep an already scheduled anchor
            // and its one-shot layout watermark valid for this same query.
            for (java.util.Map.Entry<rb0.h, Viewport> entry : pendingViewports.entrySet()) {
                Viewport pending = entry.getValue();
                if (pending.provider.get() == provider && pending.list.get() == previous) {
                    entry.setValue(pending.rebase(previous, epoch));
                }
            }
            return;
        }
        if (prefix) {
            boolean pending = false;
            for (Viewport viewport : pendingViewports.values()) {
                if (viewport.provider.get() == provider) { pending = true; break; }
            }
            // Ordinary appends already preserve order. Avoid building identity
            // maps unless an earlier reset anchor still awaits its layout.
            if (!pending) return;
        }
        fb0.u notifier = provider.d;
        if (notifier == null || notifier.d != provider) return;

        rb0.h owner = null;
        Viewport before = null;
        boolean carried = false;
        for (rb0.h adapter : snapshots.keySet()) {
            if (adapter == null || adapter.g != notifier || !adapter.r) continue;
            GridLayoutManager layout = adapter.l;
            RecyclerView view = layout == null ? null : layout.b;
            if (!usable(adapter, layout, view) || adapter.j() != previous.size()) continue;
            Viewport candidate;
            boolean pending = layout.x != -1 || layout.y != Integer.MIN_VALUE;
            if (pending) {
                candidate = pendingViewports.get(adapter);
                if (candidate == null || !candidate.matches(adapter, provider)
                        || candidate.list.get() != previous
                        || layout.x != candidate.positions[0] || layout.y != candidate.offsets[0]) continue;
            } else {
                pendingViewports.remove(adapter);
                candidate = captureViewport(adapter, provider, previous);
                if (candidate == null) continue;
            }
            if (owner != null) return; // Ambiguous concurrently visible panels.
            owner = adapter;
            before = candidate;
            carried = pending;
        }
        if (owner == null) return;
        int[] mapping = candidateMapping(previous, fresh);
        if (mapping == null) return;
        ArrayList<Integer> positions = new ArrayList<>(), offsets = new ArrayList<>();
        for (int i = 0; i < before.positions.length; i++) {
            int old = before.positions[i];
            if (old >= 0 && old < mapping.length && mapping[old] >= 0) {
                positions.add(Integer.valueOf(mapping[old]));
                offsets.add(Integer.valueOf(before.offsets[i]));
            }
        }
        if (positions.isEmpty()) return;
        Viewport after = before.remap(fresh, integers(positions), integers(offsets));
        preparedUpdates.put(provider, new UpdatePlan(provider, notifier, owner, after,
                fresh, epoch, carried, owner.l.x, owner.l.y));
    }

    private static boolean sameCommitPrefix(List<?> previous, List<?> fresh) {
        if (fresh.size() < previous.size()) return false;
        for (int i = 0; i < previous.size(); i++) {
            if (!(previous.get(i) instanceof m40.b) || !(fresh.get(i) instanceof m40.b)
                    || !Objects.equals(((m40.b) previous.get(i)).getCorrectionSpanReplacementText(),
                            ((m40.b) fresh.get(i)).getCorrectionSpanReplacementText())) return false;
        }
        return true;
    }

    /** Match duplicate occurrences in order, always to fresh native objects. */
    private static int[] candidateMapping(List<?> previous, List<?> fresh) {
        HashMap<CandidateKey, ArrayDeque<Integer>> positions = new HashMap<>();
        for (int i = 0; i < fresh.size(); i++) {
            if (!(fresh.get(i) instanceof m40.b)) return null;
            CandidateKey key = new CandidateKey((m40.b) fresh.get(i));
            if (!key.valid()) return null;
            ArrayDeque<Integer> values = positions.get(key);
            if (values == null) { values = new ArrayDeque<>(); positions.put(key, values); }
            values.addLast(Integer.valueOf(i));
        }
        int[] mapping = new int[previous.size()];
        Arrays.fill(mapping, -1);
        for (int i = 0; i < previous.size(); i++) {
            if (!(previous.get(i) instanceof m40.b)) return null;
            ArrayDeque<Integer> values = positions.get(new CandidateKey((m40.b) previous.get(i)));
            if (values != null && !values.isEmpty()) mapping[i] = values.removeFirst().intValue();
        }
        return mapping;
    }

    private static int[] integers(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i).intValue();
        return result;
    }

    private static boolean usable(rb0.h adapter, GridLayoutManager layout, RecyclerView view) {
        return layout != null && view != null && view.isAttachedToWindow() && view.isShown()
                && !view.c0() && view.getAdapter() == adapter && view.getLayoutManager() == layout
                && view.getWidth() > 0 && view.getHeight() > 0 && layout.G > 0;
    }

    private static Viewport captureViewport(rb0.h adapter, rb0.j provider, List<?> previous) {
        GridLayoutManager layout = adapter.l;
        if (layout.r == null) return null;
        int first = layout.V0(), last = layout.W0();
        if (first < 0 || last < first || last >= previous.size()) return null;
        ArrayList<Integer> positions = new ArrayList<>(), offsets = new ArrayList<>();
        int padding = layout.r.k();
        for (int i = first; i <= last; i++) {
            View child = layout.q(i);
            if (child == null) continue;
            positions.add(Integer.valueOf(i));
            offsets.add(Integer.valueOf(layout.r.e(child) - padding));
        }
        if (positions.isEmpty()) return null;
        Object theme = adapter.e.b();
        int height = keyHeight(adapter);
        if (theme == null || height <= 0) return null;
        return new Viewport(layout, layout.b, provider, theme, previous,
                layout.G, layout.b.getWidth(), layout.b.getHeight(), height,
                integers(positions), integers(offsets), null);
    }

    private static synchronized void invalidateViewports(rb0.j provider) {
        java.util.Iterator<Viewport> values = pendingViewports.values().iterator();
        while (values.hasNext()) if (values.next().provider.get() == provider) values.remove();
    }

    private static synchronized boolean applyViewport(UpdatePlan plan) {
        rb0.h adapter = plan.adapter.get();
        rb0.j provider = plan.provider.get();
        if (adapter == null || provider == null || !plan.matches(provider, plan.notifier.get())) return false;
        GridLayoutManager layout = adapter.l;
        boolean expected = layout.x == plan.pendingPosition && layout.y == plan.pendingOffset;
        boolean alreadyApplied = plan.applied && layout.x == plan.viewport.positions[0]
                && layout.y == plan.viewport.offsets[0];
        if (!expected && !alreadyApplied) return false;
        // l1 sets the pending anchor directly. RecyclerView.w0 would stop fling.
        layout.l1(plan.viewport.positions[0], plan.viewport.offsets[0]);
        pendingViewports.put(adapter, plan.viewport.rebase(provider.b, plan.epoch));
        plan.applied = true;
        return true;
    }

    private static synchronized void discardViewport(UpdatePlan plan) {
        rb0.h adapter = plan.adapter.get();
        Viewport current = pendingViewports.get(adapter);
        if (current != null && current.epoch == plan.epoch) pendingViewports.remove(adapter);
    }

    /** Geometry and weak identities only; no copied candidate/input strings. */
    private static final class Viewport {
        final WeakReference<GridLayoutManager> layout;
        final WeakReference<RecyclerView> view;
        final WeakReference<Object> provider, theme;
        final WeakReference<List<?>> list;
        final int columns, width, height, keyHeight;
        final int[] positions, offsets;
        final Object epoch;
        WeakReference<rb0.l> listener = new WeakReference<>(null);

        Viewport(GridLayoutManager layout, RecyclerView view, Object provider, Object theme,
                List<?> list, int columns, int width, int height, int keyHeight,
                int[] positions, int[] offsets, Object epoch) {
            this.layout = new WeakReference<>(layout); this.view = new WeakReference<>(view);
            this.provider = new WeakReference<>(provider); this.theme = new WeakReference<>(theme);
            this.list = new WeakReference<>(list); this.columns = columns; this.width = width;
            this.height = height; this.keyHeight = keyHeight; this.positions = positions;
            this.offsets = offsets; this.epoch = epoch;
        }
        boolean matches(rb0.h adapter, rb0.j provider) {
            GridLayoutManager layout = this.layout.get(); RecyclerView view = this.view.get();
            return this.provider.get() == provider && adapter.l == layout && usable(adapter, layout, view)
                    && adapter.r && adapter.g != null && adapter.g.d == provider
                    && theme.get() == adapter.e.b() && columns == layout.G
                    && width == view.getWidth() && height == view.getHeight()
                    && keyHeight == keyHeight(adapter);
        }
        Viewport remap(List<?> list, int[] positions, int[] offsets) {
            Viewport result = new Viewport(layout.get(), view.get(), provider.get(), theme.get(), list,
                    columns, width, height, keyHeight, positions, offsets, epoch);
            result.listener = listener;
            return result;
        }
        Viewport rebase(List<?> list, Object epoch) {
            Viewport result = new Viewport(layout.get(), view.get(), provider.get(), theme.get(), list,
                    columns, width, height, keyHeight, positions, offsets, epoch);
            result.listener = listener;
            return result;
        }
    }

    private static final class UpdatePlan {
        final WeakReference<rb0.j> provider;
        final WeakReference<fb0.u> notifier;
        final WeakReference<rb0.h> adapter;
        final WeakReference<?>[] fresh;
        final Viewport viewport;
        final Object epoch;
        final boolean carried;
        final int pendingPosition, pendingOffset;
        boolean applied;

        UpdatePlan(rb0.j provider, fb0.u notifier, rb0.h adapter, Viewport viewport,
                List<?> fresh, Object epoch, boolean carried, int position, int offset) {
            this.provider = new WeakReference<>(provider); this.notifier = new WeakReference<>(notifier);
            this.adapter = new WeakReference<>(adapter); this.viewport = viewport; this.epoch = epoch;
            this.carried = carried; this.pendingPosition = position; this.pendingOffset = offset;
            this.fresh = new WeakReference<?>[fresh.size()];
            for (int i = 0; i < this.fresh.length; i++) this.fresh[i] = new WeakReference<>(fresh.get(i));
        }
        boolean matches(rb0.j provider, fb0.u notifier) {
            if (provider == null || notifier == null || this.provider.get() != provider
                    || this.notifier.get() != notifier || provider.d != notifier || notifier.d != provider) return false;
            synchronized (CandidateScroll.class) {
                if (updateEpochs.get(provider) != epoch) return false;
            }
            rb0.h adapter = this.adapter.get();
            if (adapter == null || adapter.g != notifier || !viewport.matches(adapter, provider)) return false;
            List<?> current = provider.b;
            if (current == null || current.size() != fresh.length) return false;
            for (int i = 0; i < fresh.length; i++) {
                Object expected = fresh[i].get();
                if (expected == null || current.get(i) != expected) return false;
            }
            return true;
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
