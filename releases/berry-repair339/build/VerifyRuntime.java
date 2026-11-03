import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.*;
import java.nio.file.Path;
import java.util.*;

/** Check packaged runtime ABI and ensure compile-only Android stubs are absent. */
public final class VerifyRuntime {
    public static void main(String[] args) throws Exception {
        byte[] raw = java.nio.file.Files.readAllBytes(Path.of(args[0]));
        String text = new String(raw, java.nio.charset.StandardCharsets.UTF_8);
        for (String forbidden : Arrays.asList("RenderDiagnostics", "PostScrollDiagnostics", "RestoreDiagnostics",
                "android/util/Log", "android/widget/", "android/app/AlertDialog", "android/content/Clip",
                "OnScrollChangedListener", "addOnScrollChangedListener", "getCurrentWebViewPackage",
                "描画記録", "BerryPostScrollNumericV1", "imagesScanned", "SCROLL_SETTLED", "DRAW_TRAVERSAL",
                "JS_BUSY", "SAMPLE_OK", "MANUAL_INSPECT", "RESTORE_BASELINE"))
            if (text.contains(forbidden)) throw new IllegalStateException("Removed diagnostic reference/string remains: " + forbidden);
        Set<String> types = new TreeSet<>();
        Set<String> methods = new TreeSet<>();
        for (ClassDef c : DexFileFactory.loadDexFile(Path.of(args[0]).toFile(), Opcodes.forApi(26)).getClasses()) {
            if (!c.getType().startsWith("Lhiro/berrybfcache/"))
                throw new IllegalStateException("Unexpected runtime class: " + c.getType());
            if (!types.add(c.getType())) throw new IllegalStateException("Duplicate runtime class");
            for (Method m : c.getMethods()) {
                StringBuilder d = new StringBuilder(c.getType()).append("->").append(m.getName()).append('(');
                for (CharSequence p : m.getParameterTypes()) d.append(p);
                methods.add(d.append(')').append(m.getReturnType()).toString());
            }
        }
        for (String t : Arrays.asList("RestoreCoordinator", "NativeCompletion", "CompletionTarget", "NativeRestoreTracker", "PresentationRecovery"))
            if (!types.contains("Lhiro/berrybfcache/" + t + ";")) throw new IllegalStateException("Missing runtime class: " + t);
        for (String signature : Arrays.asList(
            "started(Landroid/webkit/WebView;Ljava/lang/String;Ljava/lang/Runnable;)V",
            "committed(Landroid/webkit/WebView;Ljava/lang/String;Ljava/lang/Runnable;)V",
            "finished(Landroid/webkit/WebView;Ljava/lang/String;)V",
            "shouldSkipLoading(Landroid/webkit/WebView;)Z",
            "startedBodyFinished(Landroid/webkit/WebView;Ljava/lang/String;)V",
            "listenerName()Ljava/lang/String;", "listener()Ljava/lang/reflect/InvocationHandler;", "script()Ljava/lang/String;"))
            if (!methods.contains("Lhiro/berrybfcache/RestoreCoordinator;->" + signature))
                throw new IllegalStateException("Missing runtime ABI: " + signature);
        for (String signature : Arrays.asList(
            "Lhiro/berrybfcache/CompletionTarget;->hiroCompleteBfcache(Landroid/webkit/WebView;Ljava/lang/String;)Z",
            "Lhiro/berrybfcache/NativeCompletion;->completeAtUrl(Ljava/lang/String;)Z",
            "Lhiro/berrybfcache/NativeRestoreTracker;->install(Landroid/webkit/WebView;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V",
            "Lhiro/berrybfcache/NativeRestoreTracker;->consume(Landroid/webkit/WebView;Ljava/lang/String;)J",
            "Lhiro/berrybfcache/NativeRestoreTracker;->isCurrent(Landroid/webkit/WebView;JLjava/lang/String;)Z",
            "Lhiro/berrybfcache/NativeRestoreTracker;->navigationId(Landroid/webkit/WebView;)J",
            "Lhiro/berrybfcache/PresentationRecovery;->start(Landroid/webkit/WebView;Lhiro/berrybfcache/PresentationRecovery$Proof;)V",
            "Lhiro/berrybfcache/PresentationRecovery;->cancel(Landroid/webkit/WebView;)V"))
            if (!methods.contains(signature)) throw new IllegalStateException("Missing native runtime ABI: " + signature);
        System.out.println("{\"result\":\"PASS_RUNTIME_DEX\",\"classes\":" + types.size() + ",\"compile_only_stubs_absent\":true,\"native_hook_abi_present\":true,\"runtime_diagnostics_absent\":true,\"diagnostic_ui_and_log_calls_absent\":true}");
    }
}
