import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Resolve production direct native references against every DEX in the actual applied APK. */
public final class VerifyLayoutAbi1937 {
    static int checks;
    static final String SHADE="Lcom/bytedance/corecamera/ui/view/CameraShadeView;";
    static void need(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static boolean helper(String type){return type.startsWith("Lcom/hiro/ulike/PreviewLayout1922") || type.startsWith("Lcom/hiro/ulike/LayoutLifecycle1937");}
    static boolean framework(String type){return type.startsWith("Ljava/") || type.startsWith("Landroid/");}
    static Method findMethod(Map<String,ClassDef> classes,MethodReference reference){
        for(String type=reference.getDefiningClass();type!=null;){
            ClassDef owner=classes.get(type);if(owner==null){need(framework(type),"Unknown referenced native method owner "+type);return null;}
            for(Method method:owner.getMethods())if(method.getName().equals(reference.getName()) && method.getReturnType().equals(reference.getReturnType()) && method.getParameterTypes().equals(reference.getParameterTypes()))return method;
            type=owner.getSuperclass();
        }
        throw new AssertionError("Unresolved method "+reference);
    }
    static Field findField(Map<String,ClassDef> classes,FieldReference reference){
        for(String type=reference.getDefiningClass();type!=null;){
            ClassDef owner=classes.get(type);if(owner==null){need(framework(type),"Unknown referenced native field owner "+type);return null;}
            for(Field field:owner.getFields())if(field.getName().equals(reference.getName()) && field.getType().equals(reference.getType()))return field;
            type=owner.getSuperclass();
        }
        throw new AssertionError("Unresolved field "+reference);
    }
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static Method exact(Map<String,ClassDef> all,String id){
        String owner=id.substring(0,id.indexOf("->"));ClassDef c=all.get(owner);need(c!=null,"Native hook owner exists "+owner);
        for(Method m:c.getMethods())if(m.toString().equals(id))return m;
        throw new AssertionError("Missing native hook "+id);
    }
    static boolean contains(Method method,String reference){for(Instruction i:method.getImplementation().getInstructions())if(ref(i).equals(reference))return true;return false;}
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("APPLIED_APK");
        Map<String,ClassDef> all=new TreeMap<>();var container=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.forApi(26));
        for(String name:container.getDexEntryNames())for(ClassDef c:container.getEntry(name).getDexFile().getClasses())need(all.put(c.getType(),c)==null,"No duplicate class "+c.getType());
        checks=0;int resolved=0,helpers=0;
        for(ClassDef c:all.values())if(helper(c.getType())){
            helpers++;
            for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction instruction:m.getImplementation().getInstructions())if(instruction instanceof ReferenceInstruction){
                Reference reference=((ReferenceInstruction)instruction).getReference();
                if(reference instanceof MethodReference){
                    MethodReference r=(MethodReference)reference;if(framework(r.getDefiningClass()))continue;
                    Method target=findMethod(all,r);if(target==null)continue;resolved++;
                    boolean invokeStatic=instruction.getOpcode()==Opcode.INVOKE_STATIC || instruction.getOpcode()==Opcode.INVOKE_STATIC_RANGE;
                    need(((target.getAccessFlags()&8)!=0)==invokeStatic,"Static/instance call contract "+r);
                    if(!helper(target.getDefiningClass())){
                        need((target.getAccessFlags()&1)!=0,"Native direct method is public "+r);
                        need((all.get(target.getDefiningClass()).getAccessFlags()&1)!=0,"Native direct class is public "+r);
                    }
                }else if(reference instanceof FieldReference){
                    FieldReference r=(FieldReference)reference;if(framework(r.getDefiningClass()))continue;
                    Field target=findField(all,r);if(target==null)continue;resolved++;
                    boolean staticOp=instruction.getOpcode().name().startsWith("SGET") || instruction.getOpcode().name().startsWith("SPUT");
                    need(((target.getAccessFlags()&8)!=0)==staticOp,"Static/instance field contract "+r);
                    if(!helper(target.getDefiningClass()))need((target.getAccessFlags()&1)!=0,"Native direct field is public "+r);
                    if(instruction.getOpcode().name().startsWith("IPUT") && r.getDefiningClass().equals(SHADE))need(!Set.of("D","E","F").contains(r.getName()),"Video wide-screen cached fields are preserved");
                }
            }
        }
        need(helpers>=4 && resolved>40,"Actual production helpers, not compile fixtures, are packaged");
        for(String type:all.keySet())need(!type.contains("LayoutHost1937") && !type.contains("LayoutLifecycleHost1937"),"Host fixtures excluded from app "+type);
        String helper="Lcom/hiro/ulike/PreviewLayout1922;->";
        need(contains(exact(all,SHADE+"->onDraw(Landroid/graphics/Canvas;)V"),helper+"reconcile("+SHADE+")V"),"Draw still repairs active geometry");
        need(contains(exact(all,SHADE+"->onAttachedToWindow()V"),helper+"attached("+SHADE+")V"),"Attachment ownership hook exists");
        need(contains(exact(all,SHADE+"->onDetachedFromWindow()V"),helper+"detached("+SHADE+")V"),"Detach clears owned callbacks");
        need(contains(exact(all,SHADE+"->onLayout(ZIIII)V"),helper+"afterLayout("+SHADE+")V"),"Measured dimensions hook exists");
        need(contains(exact(all,SHADE+"->setTopOffset(I)V"),helper+"offsetChanged("+SHADE+")V"),"Native inset setter remains wired");
        need(contains(exact(all,"Lcom/bytedance/corecamera/ui/view/CameraShadeView$d;->a(Lcom/bytedance/corecamera/ui/view/CameraShadeView$c;)V"),helper+"listenerAdded(Ljava/lang/Object;)V"),"Late native viewport listener receives an owned UI handoff");
        need(contains(exact(all,"Lcom/bytedance/corecamera/ui/view/CameraShadeView$b;->onAnimationUpdate(Landroid/animation/ValueAnimator;)V"),helper+"owns("+SHADE+"Landroid/animation/ValueAnimator;)Z"),"Native stale animation writer remains guarded");
        need(contains(exact(all,"Lcom/bytedance/corecamera/ui/view/CameraShadeView$h;->onAnimationEnd(Landroid/animation/Animator;)V"),helper+"finish("+SHADE+"Landroid/animation/Animator;)Z"),"Native final listener remains guarded");
        Method ratio=exact(all,SHADE+"->m(Lcom/ss/android/vesdk/VEPreviewRadio;ZZZ)Z");boolean duration200=false;
        Instruction previous=null;for(Instruction i:ratio.getImplementation().getInstructions()){
            if(ref(i).equals("Landroid/animation/ValueAnimator;->setDuration(J)Landroid/animation/ValueAnimator;"))duration200=previous instanceof WideLiteralInstruction && ((WideLiteralInstruction)previous).getWideLiteral()==200L;
            previous=i;
        }
        need(duration200,"1200 ms bound must exceed audited native 200 ms transition");
        System.out.println("PASS layout native/direct ABI checks="+checks+" resolvedHelperReferences="+resolved+" helperClasses="+helpers+" apk="+new File(args[0]).getName()+"; no device execution");
    }
}
