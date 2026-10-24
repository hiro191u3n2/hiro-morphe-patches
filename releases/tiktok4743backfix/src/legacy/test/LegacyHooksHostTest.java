/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import android.view.View;
import android.view.ViewTreeObserver;
import app.hiro.tiktok4753.runtime.LegacyHooks;

/** Executes the actual extension Java implementation against a lifecycle fixture. */
public final class LegacyHooksHostTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        LegacyHooks.hide(null);
        LegacyHooks.hide("not a View");
        View unrelated = new View(), target = new View();
        LegacyHooks.hide(target);
        check(target.getVisibility() == View.GONE, "hide before attachment");
        check(target.attachmentListenerCount() == 1, "install lifecycle observer once");
        check(target.getViewTreeObserver().listenerCount() == 0, "no detached tree observer");
        target.setVisibility(View.VISIBLE);
        target.attach();
        check(target.getVisibility() == View.GONE, "new attachment hides recycled root");
        check(target.getViewTreeObserver().listenerCount() == 1, "one active draw observer");
        for (int i = 0; i < 50; i++) {
            target.setVisibility(i % 2 == 0 ? View.VISIBLE : View.INVISIBLE);
            check(target.getViewTreeObserver().draw(), "do not cancel drawing other views");
            check(target.getVisibility() == View.GONE, "native rebind cannot show the target");
            LegacyHooks.hide(target);
        }
        check(target.attachmentListenerCount() == 1 && target.getViewTreeObserver().listenerCount() == 1,
            "repeated bindings do not accumulate listeners");
        ViewTreeObserver old = target.getViewTreeObserver();
        target.detach();
        check(old.listenerCount() == 0, "remove observer on detach");
        old.kill();
        target.setVisibility(View.VISIBLE);
        target.attach();
        check(target.getViewTreeObserver() != old, "fixture uses a fresh tree after detach");
        check(target.getVisibility() == View.GONE && target.getViewTreeObserver().listenerCount() == 1,
            "reattach installs one observer on new tree");
        View initiallyAttached = new View();
        initiallyAttached.attach();
        LegacyHooks.hide(initiallyAttached);
        check(initiallyAttached.getVisibility() == View.GONE && initiallyAttached.getViewTreeObserver().listenerCount() == 1,
            "already attached views are covered immediately");
        check(unrelated.getVisibility() == View.VISIBLE && unrelated.attachmentListenerCount() == 0,
            "unrelated roots remain untouched");
        for (int type = -1; type <= 7; type++)
            check(LegacyHooks.seekbarType(type) == ((type == 3 || type == 4) ? 0 : type),
                "preserve normal, expanded, drag and unknown native seekbar states " + type);
        System.out.println("PASS LegacyHooksHostTest checks=" + checks + " recycler/attachment/observer lifecycle + seekbar states");
    }
}
