import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.baksmali.BaksmaliOptions;
import com.android.tools.smali.baksmali.Adaptors.ClassDefinition;
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter;

/** Canonical executable-code disassembly with the pinned Apktool dependency. */
public final class Dump183 {
    public static void main(String[] args)throws Exception {
        DexBackedDexFile dex=new DexBackedDexFile(Files.readAllBytes(Path.of(args[0])),0);
        int count=0;
        for(Object entry:(List<?>)dex.classSection){
            ClassDef c=(ClassDef)entry;
            if(args.length>2 && !c.getType().matches(args[2]))continue;
            Path path=Path.of(args[1],c.getType().substring(1,c.getType().length()-1)+".smali");
            Files.createDirectories(path.getParent());
            BaksmaliOptions options=new BaksmaliOptions();
            options.debugInfo=false;options.localsDirective=true;options.sequentialLabels=true;
            try(Writer out=Files.newBufferedWriter(path)){
                new ClassDefinition(options,c).writeTo(new BaksmaliWriter(out));
            }
            count++;
        }
        System.out.println("Disassembled "+count+" classes");
    }
}
