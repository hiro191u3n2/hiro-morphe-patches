import java.nio.file.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Surgical JVM and DEX equivalent of method.getInstruction(match.index). */
public final class FixBundle {
    static final String TARGET="app/crimera/patches/instagram/misc/distractionFree/HideFeedPostsPatchKt";
    static final String MATCH="app/morphe/patcher/Match";
    static final String IM=MATCH+"$InstructionMatch";
    static final String MM="app/morphe/patcher/util/proxy/mutableTypes/MutableMethod";
    static final String EXT="app/morphe/patcher/extensions/InstructionExtensions";
    static final String INSTR="com/android/tools/smali/dexlib2/iface/instruction/Instruction";
    static void require(boolean b,String m){if(!b)throw new IllegalStateException(m);}
    public static void main(String[] args)throws Exception {
        patchClass(Path.of(args[0]),Path.of(args[1]));
        patchDex(Path.of(args[2]),Path.of(args[3]));
    }
    static void patchClass(Path in,Path out)throws Exception {
        ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(in)).accept(c,0);
        require(c.name.equals(TARGET),"Unexpected JVM class");int changed=0;
        for(MethodNode m:c.methods)if(m.name.equals("hideFeedPostsPatch$lambda$0$0")){
            boolean label=false;
            for(AbstractInsnNode n:m.instructions.toArray()){
                if(n instanceof LdcInsnNode l && "piko_feed_complete".equals(l.cst))label=true;
                if(label && n instanceof MethodInsnNode x && x.owner.equals(IM)&&x.name.equals("getInstruction")){
                    // Original expression starts with ALOAD 8 (the footer Match). Keep the
                    // fingerprint index, then load its current mutable method instruction.
                    AbstractInsnNode p=x.getPrevious();
                    require(p instanceof TypeInsnNode t && t.desc.equals(IM),"JVM match cast changed");
                    p=p.getPrevious();require(p instanceof MethodInsnNode g && g.owner.equals("java/util/List")&&g.name.equals("get"),"JVM list lookup changed");
                    p=p.getPrevious();require(p instanceof IntInsnNode i && i.operand==7,"JVM footer index changed");
                    p=p.getPrevious();require(p instanceof MethodInsnNode g && g.owner.equals(MATCH)&&g.name.equals("getInstructionMatches"),"JVM match lookup changed");
                    p=p.getPrevious();require(p instanceof VarInsnNode v && v.getOpcode()==Opcodes.ALOAD&&v.var==8,"JVM footer local changed");
                    InsnList fix=new InsnList();
                    fix.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,IM,"getIndex","()I",false));
                    fix.add(new VarInsnNode(Opcodes.ALOAD,8));
                    fix.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,MATCH,"getMethod","()L"+MM+";",false));
                    fix.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,MM,"getImplementation","()Lcom/android/tools/smali/dexlib2/builder/MutableMethodImplementation;",false));
                    fix.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"com/android/tools/smali/dexlib2/builder/MutableMethodImplementation","getInstructions","()Ljava/util/List;",false));
                    fix.add(new InsnNode(Opcodes.SWAP));
                    fix.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,"java/util/List","get","(I)Ljava/lang/Object;",true));
                    fix.add(new TypeInsnNode(Opcodes.CHECKCAST,INSTR));
                    m.instructions.insertBefore(x,fix);m.instructions.remove(x);changed++;label=false;
                }
            }
        }
        require(changed==1,"Expected exactly one JVM label fix, got "+changed);
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);Files.write(out,w.toByteArray());
        System.out.println("JVM label fix: 1");
    }
    static boolean call(Instruction i,String owner,String name){return i instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m && m.getDefiningClass().equals("L"+owner+";")&&m.getName().equals(name);}
    static ImmutableMethodReference ref(String owner,String name,List<String> params,String ret){return new ImmutableMethodReference("L"+owner+";",name,params,ret);}
    static void patchDex(Path in,Path out)throws Exception {
        var dex=DexFileFactory.loadDexFile(in.toFile(),com.android.tools.smali.dexlib2.Opcodes.getDefault());
        List<ClassDef> classes=new ArrayList<>();int changed=0;
        for(ClassDef c:dex.getClasses()){
            if(!c.getType().equals("L"+TARGET+";")){classes.add(c);continue;}
            List<Method> methods=new ArrayList<>();
            for(Method m:c.getMethods()){
                if(!m.getName().equals("hideFeedPostsPatch$lambda$0$0")){methods.add(m);continue;}
                var impl=new MutableMethodImplementation(m.getImplementation());var ins=impl.getInstructions();
                // This official-bundle SHA is pinned in build.py. Assert the exact register
                // and instruction context before editing; fail rather than infer a new ABI.
                require(ins.size()==225 && impl.getRegisterCount()==14,"DEX target layout changed");
                require(call(ins.get(172),MATCH,"getMethod"),"DEX mutable footer lookup changed");
                require(ins.get(173) instanceof OneRegisterInstruction r&&r.getRegisterA()==3,"DEX footer mutable register changed");
                require(call(ins.get(200),IM,"getInstruction"),"DEX label getter changed");
                require(ins.get(200) instanceof FiveRegisterInstruction r&&r.getRegisterC()==0,"DEX label register changed");
                require(ins.get(201).getOpcode()==Opcode.MOVE_RESULT_OBJECT && ((OneRegisterInstruction)ins.get(201)).getRegisterA()==0,"DEX result changed");
                require(ins.get(202) instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s&&s.getString().equals("piko_feed_complete")&&((OneRegisterInstruction)ins.get(202)).getRegisterA()==7,"DEX free scratch register changed");
                impl.replaceInstruction(200,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,1,0,0,0,0,0,ref(IM,"getIndex",List.of(),"I")));
                impl.replaceInstruction(201,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
                impl.addInstruction(202,new BuilderInstruction21c(Opcode.SGET_OBJECT,7,new ImmutableFieldReference("L"+EXT+";","INSTANCE","L"+EXT+";")));
                impl.addInstruction(203,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,3,7,3,0,0,0,ref(EXT,"getInstruction",List.of("L"+MM+";","I"),"Lcom/android/tools/smali/dexlib2/builder/BuilderInstruction;")));
                impl.addInstruction(204,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,0));
                methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl));changed++;
            }
            classes.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
        }
        require(changed==1,"Expected exactly one DEX label fix, got "+changed);
        DexPool.writeTo(out.toString(),new ImmutableDexFile(dex.getOpcodes(),classes));
        System.out.println("DEX label fix: 1");
    }
}
