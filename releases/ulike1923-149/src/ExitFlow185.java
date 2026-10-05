package com.hiro.ulike;

/**
 * Existing 185 exit protocol with cancellable, bounded pre-close waiting.
 * No forced timeout termination and no clearing of writer/capture/recording state.
 * Port and legacy public method descriptors remain compatible with the installed payload.
 */
public final class ExitFlow185 {
    public interface Port {
        long now();
        long failureStamp();
        boolean captureBusy();
        boolean saveBusy();
        boolean recording();
        boolean requestRecordStop();
        boolean seal();
        void closeScreens();
        void terminate();
        void waiting();
        void cancelled();
        void schedule(long delay);
    }
    private final Port port;
    private boolean requested, closing, stopRequested, terminated;
    private long requestedAt, quietSince = -1, closedAt, failureAtRequest;

    public ExitFlow185(Port port) { this.port = port; }

    public synchronized boolean requested() { return requested; }

    public synchronized boolean cancelPending1921() {
        if (!requested || closing || terminated) return false;
        requested = false;
        quietSince = -1;
        port.cancelled();
        return true;
    }

    /** A fresh user interaction cancels pre-close exit instead of trapping all input. */
    public synchronized boolean blockInput1921() {
        if (!requested) return false;
        if (closing || terminated) return true;
        cancelPending1921();
        return false;
    }

    /** Window lifecycle callbacks are NOT user interaction and must not cancel exit. */
    public synchronized boolean blockCallback1921(String method) {
        if ("dispatchTouchEvent".equals(method) || "dispatchGenericMotionEvent".equals(method)
                || "dispatchKeyShortcutEvent".equals(method)) return blockInput1921();
        return requested;
    }

    public synchronized void request() {
        if (requested || terminated) return;
        requested = true;
        closing = false;
        stopRequested = false;
        quietSince = -1;
        requestedAt = port.now();
        failureAtRequest = port.failureStamp();
        port.waiting();
        tick();
    }

    public synchronized void tick() {
        if (!requested || terminated) return;
        long now = port.now();
        // Bound only the interactive wait to 15s. Retain the original 90s post-close
        // safety deadline; neither deadline may kill a process with active work.
        long limit = closing ? 90000L : 15000L;
        if (port.failureStamp() != failureAtRequest || now - requestedAt >= limit) {
            requested = false;
            port.cancelled();
            return;
        }
        if (!closing && port.recording()) {
            if (!stopRequested) {
                stopRequested = true;
                if (!port.requestRecordStop()) {
                    requested = false;
                    port.cancelled();
                    return;
                }
            }
            quietSince = -1;
            port.schedule(100);
            return;
        }
        if (port.captureBusy() || port.saveBusy()) {
            quietSince = -1;
            port.schedule(100);
            return;
        }
        if (quietSince < 0) quietSince = now;
        if (!closing) {
            if (now - quietSince < 400) { port.schedule(100); return; }
            // Mark closing before calling lifecycle code, avoiding reentrant cancellation.
            closing = true;
            closedAt = now;
            quietSince = -1;
            port.closeScreens();
            port.schedule(100);
            return;
        }
        if (now - closedAt < 350 || now - quietSince < 350 || !port.seal()) {
            port.schedule(100);
            return;
        }
        terminated = true;
        port.terminate();
    }
}
