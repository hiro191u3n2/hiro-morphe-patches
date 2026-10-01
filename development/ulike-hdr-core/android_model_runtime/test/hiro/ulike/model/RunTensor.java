package hiro.ulike.model;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Host QA only: execute the same wrapper compiled against the official Android API. */
public final class RunTensor {
    private static int assertions;
    private static void expect(Class<? extends Throwable> type,CheckedCall action) throws Exception {
        try {action.run();}catch(Throwable error){if(!type.isInstance(error))throw new AssertionError(error);assertions++;return;}
        throw new AssertionError("expected "+type.getName());
    }
    private interface CheckedCall {void run() throws Exception;}
    public static void main(String[] args) throws Exception {
        if(args.length!=6 && args.length!=7)throw new IllegalArgumentException("style model bytenn effect fixture-dir result-dir [reject-missing-telemetry-flag]");
        final PinnedModel.CompiledModel model;
        try(FileInputStream original=new FileInputStream(args[1]);FileInputStream bytenn=new FileInputStream(args[2]);FileInputStream effect=new FileInputStream(args[3])) {
            model=PinnedModel.compile(PinnedModel.Style.valueOf(args[0]),original,bytenn,effect);
        }
        if(args.length==7) {
            if(!args[6].equals("reject-missing-telemetry-flag") || System.getenv("ORT_DISABLE_TELEMETRY")!=null)
                throw new IllegalArgumentException("invalid rejection test environment");
            expect(IllegalArgumentException.class,()->new OrtTensorEngine(model));
            System.out.println("PASS rejected missing telemetry flag before runtime creation");return;
        }
        expect(IllegalArgumentException.class,()->new OrtTensorEngine(null));
        byte[] snapshot=model.copyOnnx();snapshot[0]^=0x7f;
        if(model.copyOnnx()[0]==snapshot[0])throw new AssertionError("mutable compiled model");assertions++;
        long[] shape=model.inputShape();shape[0]=2;if(model.inputShape()[0]!=1)throw new AssertionError("mutable shape");assertions++;
        OrtTensorEngine engine=new OrtTensorEngine(model);
        try {
            expect(IllegalArgumentException.class,()->engine.run(null));
            expect(IllegalArgumentException.class,()->engine.run(new float[1]));
            final float[] bad=new float[3*256*256];bad[17]=Float.NaN;
            expect(IllegalArgumentException.class,()->engine.run(bad));bad[17]=Float.POSITIVE_INFINITY;
            expect(IllegalArgumentException.class,()->engine.run(bad));
            Files.createDirectories(Paths.get(args[5]));
            for(String fixture:new String[]{"zero","signed_ramps","seeded_signed_random"}) {
                Path path=Paths.get(args[4],fixture+".bin");byte[] bytes=Files.readAllBytes(path);
                if(bytes.length!=3*256*256*4)throw new AssertionError("fixture size");
                float[] tensor=new float[3*256*256];ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(tensor);
                float[] before=tensor.clone(),output=engine.run(tensor),again=engine.run(tensor);
                if(!java.util.Arrays.equals(before,tensor))throw new AssertionError("modified input");assertions++;
                if(!java.util.Arrays.equals(output,again))throw new AssertionError("nondeterministic repeat");assertions++;
                ByteBuffer out=ByteBuffer.allocate(output.length*4).order(ByteOrder.LITTLE_ENDIAN);out.asFloatBuffer().put(output);
                Files.write(Paths.get(args[5],fixture+".bin"),out.array());
            }
        } finally {engine.close();}
        engine.close();assertions++;
        expect(IllegalStateException.class,()->engine.run(new float[3*256*256]));
        System.out.println("PASS "+model.style+" wrapper assertions="+assertions);
    }
}
