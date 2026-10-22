package app.hiro.berry.runtime;

import android.os.Looper;
import android.util.Log;
import android.util.Printer;

/** Reads only the public dump's implementation marker; never logs queued message data. */
final class BerryQueueDiagnostics {
    private static final String TAG = "HiroBerryQueue";
    private static final String DELI = "(MessageQueue is using DeliQueue implementation)";
    private static final String LEGACY = "(MessageQueue is using Legacy implementation)";

    private BerryQueueDiagnostics() { }

    static void logMainQueue() {
        final int[] selected = {0};
        try {
            Looper main = Looper.getMainLooper();
            if (main != null) {
                main.dump(new Printer() {
                    @Override
                    public void println(String line) {
                        if (line == null || selected[0] != 0) {
                            return;
                        }
                        String marker = line.trim();
                        if (DELI.equals(marker)) {
                            selected[0] = 1;
                        } else if (LEGACY.equals(marker)) {
                            selected[0] = 2;
                        }
                    }
                }, "");
            }
        } catch (RuntimeException ignored) {
            // A different OS dump format, or a failing diagnostic, is not activation evidence.
            selected[0] = 0;
        } catch (LinkageError ignored) {
            selected[0] = 0;
        }
        try {
            if (selected[0] == 1) {
                Log.i(TAG, "main MessageQueue: DeliQueue (lock-free)");
            } else if (selected[0] == 2) {
                Log.i(TAG, "main MessageQueue: Legacy");
            } else {
                Log.i(TAG, "main MessageQueue: unknown (implementation marker unavailable)");
            }
        } catch (RuntimeException ignored) {
            // Diagnostics remain optional even if logging is unavailable.
        }
    }
}
