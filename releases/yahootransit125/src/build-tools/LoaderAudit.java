import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Replace exactly one patch-time class; compare every remaining DEX definition canonically. */
public final class LoaderAudit {
    static final String TARGET="Lapp/hiro/yahootransit/patches/NearMeLayout;";
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static Map<String,ClassDef> read(String path)throws Exception{Map<String,ClassDef> result=new TreeMap<>();MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses())check(result.put(c.getType(),c)==null,"duplicate class");return result;}
    static byte[] canonical(ClassDef clazz)throws Exception{MemoryDataStore out=new MemoryDataStore();try{DexPool.writeTo(out,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(clazz)));return Arrays.copyOf(out.getBuffer(),out.getSize());}finally{out.close();}}
    public static void main(String[] args)throws Exception{
        Map<String,ClassDef> old=read(args[0]),replacement=read(args[1]),combined=new TreeMap<>(old);
        check(replacement.keySet().equals(Collections.singleton(TARGET)),"replacement must contain exactly NearMeLayout");check(old.containsKey(TARGET),"active helper absent");
        check(!Arrays.equals(canonical(old.get(TARGET)),canonical(replacement.get(TARGET))),"replacement must change helper");combined.put(TARGET,replacement.get(TARGET));
        DexPool.writeTo(args[2],new ImmutableDexFile(Opcodes.getDefault(),combined.values()));Map<String,ClassDef> written=read(args[2]);check(written.keySet().equals(old.keySet()),"class inventory changed");
        int preserved=0;for(String name:old.keySet()){
            check(Arrays.equals(canonical(combined.get(name)),canonical(written.get(name))),"definition changed on writing: "+name);
            if(!TARGET.equals(name)){check(Arrays.equals(canonical(old.get(name)),canonical(written.get(name))),"unrelated class changed: "+name);preserved++;}
        }
        System.out.println("PASS LoaderAudit: replaced exactly one patch-time helper; "+preserved+" other DEX classes preserved canonically; exact class inventory.");
    }
}
