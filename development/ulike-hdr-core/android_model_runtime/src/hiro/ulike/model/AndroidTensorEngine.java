package hiro.ulike.model;

import android.system.ErrnoException;
import android.system.Os;
import ai.onnxruntime.OrtException;

/** Android entry point. Call before any other component initializes ONNX Runtime. */
public final class AndroidTensorEngine {
    private AndroidTensorEngine() {}
    public static OrtTensorEngine open(PinnedModel.CompiledModel model) throws ErrnoException, OrtException {
        if(model==null)throw new IllegalArgumentException("model required");
        // Mandatory manifest removal also prevents the AAR's startup provider
        // from loading ORT before this application-controlled entry point runs.
        Os.setenv("ORT_DISABLE_TELEMETRY","1",true);
        return new OrtTensorEngine(model);
    }
}
