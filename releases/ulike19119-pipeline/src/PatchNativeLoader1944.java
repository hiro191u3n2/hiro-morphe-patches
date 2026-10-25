import java.nio.file.*;
import java.security.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Replace only the existing speed1935 pinned digest/length; preserve both rows. */
public final class PatchNativeLoader1944 {
    static final String OWNER="app/hiro/ulike/patches/IntegrationPayload186";
    static final String TARGET="lib/arm64-v8a/libulike_speed1935.so";
    static final String RESOURCE="ulike1935/runtime/libulike_speed1935.so";
    static void req(boolean value,String text){if(!value)throw new IllegalStateException(text);}
    static ClassNode read(byte[] bytes){ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(bytes).accept(n,0);return n;}
    static byte[] write(ClassNode n){ClassWriter out=new ClassWriter(0);n.accept(out);return out.toByteArray();}
    static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    public static void main(String[] args)throws Exception{
        req(args.length==3,"ORIGINAL_CLASS NATIVE_SO OUTPUT_CLASS");
        byte[] original=Files.readAllBytes(Path.of(args[0])),lib=Files.readAllBytes(Path.of(args[1]));
        req(lib.length>=64 && lib[0]==0x7f && lib[1]=='E' && lib[2]=='L' && lib[3]=='F' &&
            lib[4]==2 && lib[5]==1 && lib[18]==(byte)183 && lib[19]==0,"ELF64AArch64 required");
        ClassNode node=read(original);req(node.name.equals(OWNER),"Wrong installer class");
        LdcInsnNode digest=null,length=null;int targets=0;
        for(MethodNode method:node.methods)if(method.name.equals("<clinit>")){
            List<LdcInsnNode> strings=new ArrayList<>();
            for(AbstractInsnNode n:method.instructions.toArray())if(n instanceof LdcInsnNode l && l.cst instanceof String)strings.add(l);
            for(int i=0;i<strings.size();i++)if(strings.get(i).cst.equals(TARGET)){
                req(i+3<strings.size(),"Incomplete native installer row");
                digest=strings.get(i+1);length=strings.get(i+2);
                req(((String)digest.cst).matches("[0-9a-f]{64}") && ((String)length.cst).matches("[0-9]+") &&
                    strings.get(i+3).cst.equals(RESOURCE),"Exact speed1935 row required");targets++;
            }
        }
        req(targets==1,"Exactly one native speed1935 row required");
        Object oldDigest=digest.cst,oldLength=length.cst;
        digest.cst=sha(lib);length.cst=Integer.toString(lib.length);byte[] result=write(node);
        digest.cst=oldDigest;length.cst=oldLength;
        req(Arrays.equals(write(read(original)),write(node)),"Installer changed beyond speed digest/length");
        Files.write(Path.of(args[2]),result);
        System.out.println("PASS native1944 installer inverse; retained two rows; sha256="+sha(lib)+" bytes="+lib.length);
    }
}
