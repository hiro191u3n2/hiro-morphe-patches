package hiro.swiftkey;

import com.touchtype.keyboard.SpannableCell;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

public final class ClockHistoryHold {
    private ClockHistoryHold() {}
    private static final cm0.d ALWAYS = state -> Boolean.TRUE;

    private static boolean isClock(Supplier<?> binding) {
        return binding instanceof ne0.h && ((ne0.h) binding).a.g.a instanceof ne0.s0;
    }

    public static boolean showVariantIndicator(boolean original, Supplier<?> binding) {
        return original && !isClock(binding);
    }

    public static void bind(hc0.b builder, int timeout, cm0.d variantCondition,
                            ic0.c0[] variantActions, Supplier<?> binding) {
        if (!isClock(binding)) {
            builder.r(timeout, variantCondition, variantActions);
            return;
        }
        ne0.h grid = (ne0.h) binding;
        Hold hold = new Hold(grid, grid.b.u.getContent());
        builder.q(timeout, ALWAYS, new ic0.a[] { hold::arm });
        builder.p(timeout, ALWAYS, new ic0.a[] { hold::release });
    }

    private static final class Hold {
        final ne0.h binding;
        final String emoji;
        boolean armed;

        Hold(ne0.h binding, String emoji) {
            this.binding = binding;
            this.emoji = emoji;
        }

        boolean current() {
            if (emoji == null || !emoji.equals(binding.b.u.getContent())) return false;
            int position = binding.b.c();
            ne0.s0 clock = (ne0.s0) binding.a.g.a;
            return position >= 0 && position < clock.getCount() && emoji.equals(clock.u(position));
        }

        void arm(lh0.a event) {
            armed = current();
            if (armed) binding.b.u.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }

        void release(lh0.a event) {
            if (!armed) return;
            armed = false;
            if (!current()) return;
            ne0.i adapter = binding.a;
            ne0.s0 clock = (ne0.s0) adapter.g.a;
            if (remove(clock.a, adapter.m, emoji)) {
                clock.x();
                adapter.n();
                clock.a.f(2, clock.a.f);
            }
        }
    }

    // The native live list groups presentation variants under the same key.
    // Remove that key's saved representations so reopening cannot restore the tile.
    static boolean remove(ne0.f0 history, ah0.w0 preferences, String emoji) {
        String key = p40.d.f(emoji);
        LinkedHashMap live = history.f.a();
        if (!live.containsKey(key)) return false;
        ArrayList<SpannableCell> saved = new ArrayList<SpannableCell>(preferences.b0());
        for (Iterator<SpannableCell> entries = saved.iterator(); entries.hasNext();) {
            if (key.equals(p40.d.f(entries.next().getCellText()))) entries.remove();
        }
        com.google.gson.j gson = (com.google.gson.j) preferences.f.getValue();
        Type type = (Type) preferences.e.getValue();
        preferences.putString("emoji_recent_tab_keys", gson.j(saved, type));
        live.remove(key);
        return true;
    }
}
