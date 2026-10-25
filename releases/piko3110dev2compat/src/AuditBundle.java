import java.nio.file.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;

/** Canonical semantic audit: every other JVM/DEX method must remain identical. */
public final class AuditBundle {
    static final String TARGET="Lapp/crimera/patches/instagram/misc/distractionFree/HideFeedPostsPatchKt;";
    static final String METHOD="hideFeedPostsPatch$lambda$0$0";
    static void require(boolean b,String msg){if(!b)throw new IllegalStateException(msg);}
    static byte[] canonical(ClassDef c,Collection<? extends Method> methods)throws Exception{
        var copy=new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);
        var memory=new MemoryDataStore();DexPool.writeTo(memory,new ImmutableDexFile(com.android.tools.smali.dexlib2.Opcodes.getDefault(),List.of(copy)));return memory.getData();
    }
    static List<Method> methods(ClassDef c){List<Method> m=new ArrayList<>();c.getMethods().forEach(m::add);return m;}
    static byte[] canonical(ClassNode c,MethodNode m){
        var w=new ClassWriter(0);w.visit(c.version,c.access,c.name,c.signature,c.superName,c.interfaces.toArray(String[]::new));m.accept(w);w.visitEnd();return w.toByteArray();
    }
    public static void main(String[] args)throws Exception{
        var a=DexFileFactory.loadDexFile(args[0],com.android.tools.smali.dexlib2.Opcodes.getDefault());var b=DexFileFactory.loadDexFile(args[1],com.android.tools.smali.dexlib2.Opcodes.getDefault());
        Map<String,ClassDef> other=new HashMap<>();b.getClasses().forEach(c->other.put(c.getType(),c));
        require(a.getClasses().size()==other.size(),"Class count changed");int unchangedClasses=0,unchangedMethods=0,changedMethods=0;
        for(var c:a.getClasses()){
            var d=other.get(c.getType());require(d!=null,"Missing class "+c.getType());
            if(!c.getType().equals(TARGET)){
                require(Arrays.equals(canonical(c,methods(c)),canonical(d,methods(d))),"Other DEX class changed: "+c.getType());unchangedClasses++;
            }else{
                var cm=methods(c);var dm=methods(d);require(cm.size()==dm.size(),"Target method count changed");
                for(int i=0;i<cm.size();i++){
                    var m=cm.get(i);var n=dm.get(i);require(m.getName().equals(n.getName()),"Method order changed");
                    boolean equal=Arrays.equals(canonical(c,List.of(m)),canonical(d,List.of(n)));
                    if(m.getName().equals(METHOD)){require(!equal,"Target DEX method unchanged");changedMethods++;}
                    else{require(equal,"Other target DEX method changed: "+m);unchangedMethods++;}
                }
            }
        }
        require(changedMethods==1,"Expected exactly one changed DEX method");
        ClassNode ca=new ClassNode(),cb=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[2]))).accept(ca,0);new ClassReader(Files.readAllBytes(Path.of(args[3]))).accept(cb,0);
        require(ca.methods.size()==cb.methods.size(),"JVM method count changed");int jvmOther=0,jvmChanged=0;
        for(int i=0;i<ca.methods.size();i++){
            MethodNode m=ca.methods.get(i),n=cb.methods.get(i);require(m.name.equals(n.name)&&m.desc.equals(n.desc),"JVM method signature changed");
            boolean equal=Arrays.equals(canonical(ca,m),canonical(cb,n));
            if(m.name.equals(METHOD)){require(!equal,"JVM target unchanged");jvmChanged++;}
            else{require(equal,"Other JVM method changed: "+m.name);jvmOther++;}
        }
        require(jvmChanged==1,"Expected exactly one changed JVM method");
        System.out.printf("Canonical audit passed: %d other DEX classes, %d other target DEX methods and %d other JVM methods unchanged; exactly one target method changed in each representation.%n",unchangedClasses,unchangedMethods,jvmOther);
    }
}
