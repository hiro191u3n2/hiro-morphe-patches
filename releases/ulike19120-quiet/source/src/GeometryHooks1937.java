import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Caller-specific preview dimensions and successful displayRect delivery caching. */
public final class GeometryHooks1937 {
 static final String API="Li/o/a/b1/a/g/y;", LEGACY="Li/o/a/m/j/f;";
 static final String FRAGMENT="Lcom/bytedance/corecamera/camera/basic/PureCameraFragment;";
 static final String LP="Landroid/widget/RelativeLayout$LayoutParams;", RECT="Landroid/graphics/RectF;";
 static final String H="Lcom/hiro/ulike/LayoutGeometry1937;";
 static final String UPDATE=FRAGMENT+"->O(I"+LP+"ZZ"+RECT+")V";
 public static final Set<String> NATIVE=Set.of(API+"->D(IIIZZ)V",API+"->h2(IIIZZ)V",LEGACY+"->j(IIIZZ)V",LEGACY+"->m(IIIZ)V",UPDATE),RUNTIME=Set.of();
 static void require(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Method wrap(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 public static Method repair(Method m){
  String id=MergePayloads.id(m);require(NATIVE.contains(id),"Unreviewed geometry target "+id);
  MutableMethodImplementation b=new MutableMethodImplementation(m.getImplementation());
  require(b.getTryBlocks().isEmpty(),"Geometry target has unexpected handlers");
  if(id.equals(UPDATE)){
   require(b.getRegisterCount()==11,"Fragment register ABI");int compares=0,marks=0;
   for(int n=b.getInstructions().size()-1;n>=0;n--){Instruction in=b.getInstructions().get(n);String r=ref(in);
    if(r.equals(FRAGMENT+"->M2("+LP+LP+")Z")){
     require(in.getOpcode()==Opcode.INVOKE_VIRTUAL && in instanceof FiveRegisterInstruction,"Fragment equality opcode");
     FiveRegisterInstruction f=(FiveRegisterInstruction)in;require(f.getRegisterCount()==3&&f.getRegisterC()==5&&f.getRegisterD()==7&&f.getRegisterE()==0,"Fragment equality parameter ABI");
     b.replaceInstruction(n,new BuilderInstruction35c(Opcode.INVOKE_STATIC,4,5,7,0,10,0,new ImmutableMethodReference(H,"equivalent",List.of(FRAGMENT,LP,LP,RECT),"Z")));compares++;
    }else if(r.equals("Li/f/l/n/q/y/c;->o0(IZZ"+RECT+")V")){
     b.addInstruction(n+1,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,5,10,0,0,0,new ImmutableMethodReference(H,"markDelivery",List.of(FRAGMENT,RECT),"V")));
     b.addInstruction(n,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,5,10,0,0,0,new ImmutableMethodReference(H,"beginDelivery",List.of(FRAGMENT,RECT),"V")));marks++;
    }
   }
   require(compares==1&&marks==1,"Fragment geometry anchors "+compares+"/"+marks);
  }else{
   boolean api=m.getDefiningClass().equals(API);int args=m.getParameterTypes().size()+1,first=b.getRegisterCount()-args;
   int expectedRegs=id.equals(API+"->D(IIIZZ)V")?22:id.equals(API+"->h2(IIIZZ)V")?25:13;
   require(b.getRegisterCount()==expectedRegs,"Dimensions register ABI "+id);
   int widths=0,heights=0;
   for(int n=b.getInstructions().size()-1;n>=0;n--){String r=ref(b.getInstructions().get(n));
    boolean height=r.equals("Li/n/c/a/o/w/e;->m()I");
    if(height||r.equals("Li/n/c/a/o/w/e;->p()I")){
     require(b.getInstructions().get(n).getOpcode()==Opcode.INVOKE_STATIC,"Dimensions static ABI");
     b.replaceInstruction(n,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,first,1,new ImmutableMethodReference(H,(api?"camera":"preview")+(height?"Height":"Width"),List.of(m.getDefiningClass()),"I")));
     if(height)heights++;else widths++;
    }
   }
   int ew=id.equals(API+"->D(IIIZZ)V")?3:id.equals(API+"->h2(IIIZZ)V")?5:id.equals(LEGACY+"->j(IIIZZ)V")?1:2;
   int eh=id.equals(API+"->h2(IIIZZ)V")?2:id.equals(LEGACY+"->m(IIIZ)V")?1:0;
   require(widths==ew&&heights==eh,"Dimensions anchor count "+id+" "+widths+"/"+heights);
  }
  return wrap(m,b);
 }
 public static void verify(Method before,Method after){
  require(MergePayloads.id(before).equals(MergePayloads.id(after)),"Geometry identity");
  require(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"Exact geometry transform");
  MutableMethodImplementation b=new MutableMethodImplementation(after.getImplementation());
  for(int n=b.getInstructions().size()-1;n>=0;n--){Instruction i=b.getInstructions().get(n);if(!(i instanceof ReferenceInstruction ri)||!(ri.getReference() instanceof MethodReference r)||!r.getDefiningClass().equals(H))continue;
   if(r.getName().equals("markDelivery")||r.getName().equals("beginDelivery"))b.removeInstruction(n);
   else if(r.getName().equals("equivalent"))b.replaceInstruction(n,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,3,5,7,0,0,0,new ImmutableMethodReference(FRAGMENT,"M2",List.of(LP,LP),"Z")));
   else b.replaceInstruction(n,new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,new ImmutableMethodReference("Li/n/c/a/o/w/e;",r.getName().endsWith("Height")?"m":"p",List.of(),"I")));
  }
  require(MergePayloads.hash(wrap(before,b)).equals(MergePayloads.hash(before)),"Inverse preserves every original geometry instruction/register/branch/handler");
 }
}
