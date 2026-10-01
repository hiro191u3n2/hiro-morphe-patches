package hiro.ulike.model;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
/** Local QA only. Generated ONNX contains caller model data and must not be redistributed. */
public final class EmitModel {
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("style model bytenn effect output");
        PinnedModel.Style style=PinnedModel.Style.valueOf(args[0]);
        try(FileInputStream model=new FileInputStream(args[1]);FileInputStream bytenn=new FileInputStream(args[2]);FileInputStream effect=new FileInputStream(args[3])) {
            PinnedModel.CompiledModel compiled=PinnedModel.compile(style,model,bytenn,effect);
            Files.write(Paths.get(args[4]),compiled.copyOnnx());
            System.out.println("PASS "+style+" operators="+compiled.operatorCount+" weights="+compiled.weightCount+" graph="+compiled.graphSha256);
        }
    }
}
