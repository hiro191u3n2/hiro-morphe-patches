package hiro.ulike.model;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import ai.onnxruntime.OnnxJavaType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collections;

/** CPU-only FP32 tensor stage. Accepts prepared NCHW model tensors, not camera pixels. */
public final class OrtTensorEngine implements AutoCloseable {
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final PinnedModel.CompiledModel model;
    private boolean closed;
    public OrtTensorEngine(PinnedModel.CompiledModel model) throws OrtException {
        if(model==null)throw new IllegalArgumentException("model required");
        // The process flag must precede ORT initialization; the API call alone
        // does not suppress the initialization event in official ORT 1.30.0.
        require("1".equals(System.getenv("ORT_DISABLE_TELEMETRY")),
                "Set ORT_DISABLE_TELEMETRY=1 before any ONNX Runtime initialization");
        this.model=model;this.environment=OrtEnvironment.getEnvironment();
        // Disable ORT telemetry explicitly before constructing or running sessions.
        this.environment.setTelemetry(false);
        OrtSession created;
        try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(1);options.setInterOpNumThreads(1);
            options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL);
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT);
            // No NNAPI/QNN/GPU provider or implicit float16 conversion is requested.
            created=environment.createSession(model.copyOnnx(),options);
        }
        try {
            require(created.getInputNames().equals(Collections.singleton(model.inputName)),"input name");
            require(created.getOutputNames().equals(Collections.singleton(model.outputName)),"output name");
            Object in=created.getInputInfo().get(model.inputName).getInfo(),out=created.getOutputInfo().get(model.outputName).getInfo();
            require(in instanceof TensorInfo && out instanceof TensorInfo,"tensor metadata");
            require(((TensorInfo)in).type==OnnxJavaType.FLOAT && Arrays.equals(((TensorInfo)in).getShape(),model.inputShape()),"input type/shape");
            require(((TensorInfo)out).type==OnnxJavaType.FLOAT && Arrays.equals(((TensorInfo)out).getShape(),model.outputShape()),"output type/shape");
        } catch(OrtException|RuntimeException|Error failure) {
            try {created.close();} catch(OrtException closeFailure){failure.addSuppressed(closeFailure);}throw failure;
        }
        this.session=created;
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
    /** Caller input is copied. Output is copied before closing native ORT values. */
    public synchronized float[] run(float[] input) throws OrtException {
        if(closed)throw new IllegalStateException("engine closed");
        require(input!=null && input.length==3*256*256,"exact NCHW input tensor required");
        FloatBuffer data=ByteBuffer.allocateDirect(input.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        // Copy and validate the copy, so a caller mutation cannot alter already validated values.
        data.put(input).flip();for(int i=0;i<data.remaining();i++)require(Float.isFinite(data.get(i)),"nonfinite input tensor");
        try(OnnxTensor tensor=OnnxTensor.createTensor(environment,data,model.inputShape());
            OrtSession.Result result=session.run(Collections.singletonMap(model.inputName,tensor),Collections.singleton(model.outputName))) {
            require(result.size()==1 && result.get(0) instanceof OnnxTensor,"unexpected model result");
            OnnxTensor output=(OnnxTensor)result.get(0);TensorInfo info=output.getInfo();
            require(info.type==OnnxJavaType.FLOAT && Arrays.equals(info.getShape(),model.outputShape()),"unexpected output type/shape");
            FloatBuffer values=output.getFloatBuffer();require(values.remaining()==4*256*256,"output tensor size");
            float[] copy=new float[values.remaining()];values.get(copy);for(float v:copy)require(Float.isFinite(v),"nonfinite output tensor");return copy;
        }
    }
    @Override public synchronized void close() throws OrtException {if(!closed){closed=true;session.close();}}
}
