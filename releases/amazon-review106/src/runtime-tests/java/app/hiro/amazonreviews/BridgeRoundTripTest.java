package app.hiro.amazonreviews;

import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/** Real Java injector -> one persistent JS Window -> Android-style JSON -> real callback. */
public final class BridgeRoundTripTest {
    private static int assertions;
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static String stderr(Process process) throws Exception {
        StringBuilder out = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) out.append(line).append('\n');
        return out.toString();
    }

    private static void scenario(File runner, String name) throws Exception {
        Handler.reset();
        WebView view = new WebView();
        String search = "https://www.amazon.co.jp/s?k=PRIVATE_TEST_NEVER_EXPORT";
        String path = "home-no-search".equals(name) ? "/gp/homepage.html" : "unknown-no-search".equals(name) ? "/mobile/entry/" :
            "cart".equals(name) ? "/gp/cart/view.html" : "product".equals(name) ? "/dp/B000000001" : null;
        view.url = path == null ? search : "https://www.amazon.co.jp" + path;
        boolean forbidden = "cart".equals(name) || "product".equals(name);
        int noticesBefore = Toast.messages.size();
        Process node = new ProcessBuilder("node", runner.getAbsolutePath(), name).start();
        BufferedWriter input = new BufferedWriter(new OutputStreamWriter(node.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader output = new BufferedReader(new InputStreamReader(node.getInputStream(), StandardCharsets.UTF_8));
        int evaluated = 0, steps = 0;
        long previousClock = Handler.clock();
        try {
            int hooks = "js-error".equals(name) ? 1 : 2;
            for (int hook = 0; hook < hooks; hook++) {
                if (hook > 0) {
                    // A later real hook observes the SAME JS Window after scheduled work.
                    check(Handler.pendingCount() == 0, "Previous native request completed before next hook");
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { @Override public void run() {} }, 1500);
                    Handler.runNext();
                }
                ReviewCountRuntime.inject(view, search);
                while (++steps <= 40) {
                view.flush();
                if (view.calls > evaluated) {
                    check(view.calls == evaluated + 1, "Each actual evaluation receives its own receipt");
                    check(view.script != null && view.callback != null, "Injector provides actual script and non-null callback");
                    ValueCallback<String> callback = view.callback;
                    long currentClock = Handler.clock();
                    String wire = Base64.getEncoder().encodeToString(view.script.getBytes(StandardCharsets.UTF_8));
                    input.write(Long.toString(currentClock - previousClock)); input.write('\t'); input.write(wire); input.newLine(); input.flush();
                    previousClock = currentClock;
                    String result = output.readLine();
                    if (result == null) throw new AssertionError("JS bridge worker failed: " + stderr(node));
                    check(result.startsWith("\"") && result.endsWith("\""), "WebView string result has outer JSON serialization");
                    check(!result.contains("PRIVATE_TEST") && !result.contains("PRIVATE_EXCEPTION") && !result.contains("https:"), "Receipt contains no private page data");
                    evaluated = view.calls;
                    callback.onReceiveValue(result);
                    continue;
                }
                    if (Handler.pendingCount() == 0) break;
                    Handler.runNext();
                }
            }
            check(steps <= 40, "Native retries/checkpoints terminate");
            check(evaluated == hooks, "Valid real receipts complete each hook without retries");
            check(Handler.pendingCount() == 0, "No native watchdog/retry remains after receipt handling");
            String expected = "js-error".equals(name) ? "js-error" : "ack";
            check(expected.equals(ReviewCountRuntime.lastStage(view)), "Actual native parser accepts " + name + " receipt");
            if (forbidden) check(Toast.messages.size() == noticesBefore, "Expected inactive route produces no false failure notice");
            if ("js-error".equals(name)) check(Toast.messages.stream().anyMatch(message -> message.contains("E_JS_TypeError")), "Caught JS exception produces its sanitized native diagnostic");
            for (String message : Toast.messages) {
                check(!message.contains("PRIVATE_TEST") && !message.contains("PRIVATE_EXCEPTION") && !message.contains("https:"), "Native diagnostics are sanitized");
            }
        } finally {
            input.close();
            if (!node.waitFor(5, TimeUnit.SECONDS)) { node.destroyForcibly(); throw new AssertionError("JS bridge worker did not terminate"); }
        }
        check(node.exitValue() == 0, "JS worker passed: " + stderr(node));
    }

    public static void main(String[] args) throws Exception {
        check(args.length == 1, "Pass the bridge_vm.js path");
        File runner = new File(args[0]);
        check(runner.isFile(), "Bridge worker exists");
        Toast.reset();
        scenario(runner, "normal");
        scenario(runner, "home-no-search");
        scenario(runner, "unknown-no-search");
        scenario(runner, "cart");
        scenario(runner, "product");
        scenario(runner, "js-error");
        check(Toast.messages.stream().anyMatch(message -> message.contains("1.0.6")), "Native version checkpoint is observable");
        System.out.println("PASS Java-JS-Java bridge assertions=" + assertions + " cases=6 persistent_window=true physical_webview=false");
    }
}
