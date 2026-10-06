import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Scoped, hash-bound mutation of shade layout methods; existing image pipeline preserved. */
public final class Transform1922 {
 static final String S="Lcom/bytedance/corecamera/ui/view/CameraShadeView;", U="Lcom/bytedance/corecamera/ui/view/CameraShadeView$b;", E="Lcom/bytedance/corecamera/ui/view/CameraShadeView$h;";
 static final String H="Lcom/hiro/ulike/PreviewLayout1922;", A="Landroid/animation/ValueAnimator;", AN="Landroid/animation/Animator;";
 static final String R="Lcom/ss/android/vesdk/VEPreviewRadio;";
 static final String DESCRIPTION="v1.9.22（v1.9.21基準）プレビュー黒帯の配置競合を修正。古い表示アニメーションを取消し、遅延処理を世代別に管理。再配置・上下余白変更時に整合性を回復。画質・保存・戻る操作は維持。実機未検証。";
 static final Set<String> TARGETS=Set.of(
  S+"->m("+R+"ZZZ)Z",S+"->onLayout(ZIIII)V",S+"->onDraw(Landroid/graphics/Canvas;)V",
  S+"->onDetachedFromWindow()V",S+"->setTopOffset(I)V",S+"->k()V",S+"->l("+S+")V",
  U+"->onAnimationUpdate("+A+")V",E+"->onAnimationEnd("+AN+")V");
 static final List<String> AUDIT=new ArrayList<>();
 static void require(boolean v,String m){MergePayloads.require(v,m);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction x?x.getReference().toString():"";}
 static BuilderInstruction3rc one(int r,String name){return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,r,1,new ImmutableMethodReference(H,name,List.of(S),"V"));}
 static Method replaced(Method m,MutableMethodImplementation impl) {
  return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl);
 }
 static Method body(Method m,int regs,List<? extends Instruction> ins) {
  return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),new ImmutableMethodImplementation(regs,ins,List.of(),List.of()));
 }
 static Instruction helper(int count,int c,int d,String name,List<String> params,String result) {
  return new ImmutableInstruction35c(Opcode.INVOKE_STATIC,count,c,d,0,0,0,new ImmutableMethodReference(H,name,params,result));
 }
 static Method change(Method m) {
  String id=MergePayloads.id(m);var b=new MutableMethodImplementation(m.getImplementation());
  if(id.equals(S+"->m("+R+"ZZZ)Z")) {
   require(b.getRegisterCount()==27,"m register drift");
   var clear=new ArrayList<Integer>();int creates=0,starts=0;
   for(int i=0;i<b.getInstructions().size();i++) {
    Instruction x=b.getInstructions().get(i);
    if(x.getOpcode()==Opcode.IPUT_OBJECT && ref(x).equals(S+"->w:"+A) && ((TwoRegisterInstruction)x).getRegisterA()==0)clear.add(i);
    if(ref(x).equals(A+"->start()V"))starts++;
    if(ref(x).equals(A+"->ofFloat([F)"+A))creates++;
   }
   require(clear.size()==3 && creates==1 && starts==1,"Native animation lifecycle drift");
   int end=clear.get(2);require(b.getInstructions().get(end+1).getOpcode()==Opcode.RETURN,"Expected obsolete post-start w=null");
   b.removeInstruction(end);
   int returns=0;
   for(int i=b.getInstructions().size()-1;i>=0;i--)if(b.getInstructions().get(i).getOpcode()==Opcode.RETURN){b.addInstruction(i,one(22,"ready"));returns++;}
   require(returns==2,"Ratio returns changed");
   b.addInstruction(0,one(22,"cancel"));
  } else if(id.equals(S+"->onLayout(ZIIII)V")) {
   require(b.getRegisterCount()==13 && b.getInstructions().get(0).getOpcode()==Opcode.INVOKE_SUPER_RANGE,"Layout ABI drift");
   // Preserve superclass arguments, then convert parent coordinates into local dimensions.
   b.addInstruction(1,new BuilderInstruction23x(Opcode.SUB_INT,11,11,9));
   b.addInstruction(2,new BuilderInstruction23x(Opcode.SUB_INT,12,12,10));
  } else if(id.equals(U+"->onAnimationUpdate("+A+")V") || id.equals(E+"->onAnimationEnd("+AN+")V")) {
   boolean update=m.getDefiningClass().equals(U);int self=update?3:4,anim=self+1;
   require(b.getRegisterCount()==self+2,"Callback register drift");
   // Guard code before all native writes and listener notifications. Label native entry.
   Label original=b.newLabelForIndex(0);
   b.addInstruction(0,new BuilderInstruction22c(Opcode.IGET_OBJECT,0,self,new ImmutableFieldReference(m.getDefiningClass(),update?"r":"c",S)));
   b.addInstruction(1,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,0,anim,0,0,0,new ImmutableMethodReference(H,update?"owns":"finish",List.of(S,update?A:AN),"Z")));
   b.addInstruction(2,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
   // Original entry label moves with its first instruction as each insertion occurs.
   b.addInstruction(3,new BuilderInstruction21t(Opcode.IF_NEZ,0,original));
   b.addInstruction(4,new BuilderInstruction10x(Opcode.RETURN_VOID));
  } else if(id.equals(S+"->onDraw(Landroid/graphics/Canvas;)V")) {
   b.addInstruction(0,one(b.getRegisterCount()-2,"reconcile"));
  } else if(id.equals(S+"->onDetachedFromWindow()V")) {
   b.addInstruction(0,one(b.getRegisterCount()-1,"cancel"));
  } else if(id.equals(S+"->k()V") || id.equals(S+"->l("+S+")V")) {
   return body(m,1,List.of(helper(1,0,0,m.getName().equals("k")?"schedule":"startCurrent",List.of(S),"V"),new ImmutableInstruction10x(Opcode.RETURN_VOID)));
  } else if(id.equals(S+"->setTopOffset(I)V")) {
   return body(m,3,List.of(
    new ImmutableInstruction22c(Opcode.IGET,0,1,new ImmutableFieldReference(S,"A","I")),
    new ImmutableInstruction22t(Opcode.IF_EQ,0,2,7),
    new ImmutableInstruction22c(Opcode.IPUT,2,1,new ImmutableFieldReference(S,"A","I")),
    helper(1,1,0,"offsetChanged",List.of(S),"V"),new ImmutableInstruction10x(Opcode.RETURN_VOID)));
  } else throw new IllegalStateException(id);
  return replaced(m,b);
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> before) {
  var out=new TreeMap<String,ClassDef>();int count=0;
  for(ClassDef c:before.values()) {
   boolean edited=false;var methods=new ArrayList<Method>();
   for(Method m:c.getMethods()) {
    if(m.getImplementation()==null){methods.add(m);continue;}
    var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int i=0;i<b.getInstructions().size();i++) {
     Instruction x=b.getInstructions().get(i);
     if(x instanceof ReferenceInstruction ri && ri.getReference() instanceof StringReference sr && sr.getString().startsWith("v1.9.21（v1.9.20基準）")) {
      require(x.getOpcode()==Opcode.CONST_STRING,"Metadata opcode drift");
      b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));count++;hit=true;
     }
    }
    methods.add(hit?replaced(m,b):m);edited|=hit;
   }
   out.put(c.getType(),edited?new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods):c);
  }
  require(count==1,"Expected one loader description");return out;
 }
 public static void main(String[] a)throws Exception {
  require(a.length==6,"BASE HELPER BUNDLE_DEX STOCK_SEED OUT AUDIT");Path base=Path.of(a[0]),out=Path.of(a[4]);Files.createDirectories(out);
  var seed=MergePayloads.methods(MergePayloads.classes(a[3]).values());require(seed.keySet().equals(TARGETS),"Stock seed set mismatch");
  var payload=MergePayloads.methods(MergePayloads.classes(base.resolve("ulike/methods.dex").toString()).values());var prior=new TreeMap<>(payload);
  var rows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());require(rows.keySet().equals(payload.keySet()),"Payload contract mismatch");
  for(var e:payload.entrySet())require(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Input payload hash mismatch");
  for(String id:TARGETS) {
   Method original=seed.get(id),before=payload.getOrDefault(id,original);
   if(rows.containsKey(id))require(MergePayloads.hash(original).equals(rows.get(id)[1]),"Original seed differs "+id);
   Method after=change(before);require(original.getAccessFlags()==after.getAccessFlags(),"Access drift");
   payload.put(id,after);rows.put(id,new String[]{id,MergePayloads.hash(original),MergePayloads.hash(after)});
   AUDIT.add("LAYOUT_FIX\t"+id+"\t"+MergePayloads.hash(before)+"\t"+MergePayloads.hash(after));
  }
  int unchanged=0;for(var e:prior.entrySet())if(!TARGETS.contains(e.getKey())){require(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(payload.get(e.getKey()))),"Unrelated stock method changed");unchanged++;}
  var rt=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var helper=MergePayloads.classes(a[1]);require(rt.size()==222,"Baseline runtime drift");
  for(String name:helper.keySet()){require(name.startsWith("Lcom/hiro/ulike/PreviewLayout1922")&&!rt.containsKey(name),"Leaked stub/runtime collision "+name);}
  require(helper.containsKey(H),"Missing compiled layout helper");var all=new TreeMap<>(rt);all.putAll(helper);
  var standalone=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));var oldbundle=MergePayloads.classes(a[2]);var bundle=metadata(oldbundle);int others=0;
  for(var e:oldbundle.entrySet())if(!e.getKey().startsWith(MergePayloads.PATCH_NS)){require(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Unrelated app loader changed");others++;}
  require(others==220,"Bundle class count drift");
  MergePayloads.writeDex(out.resolve("runtime.dex"),all.values());MergePayloads.writeDex(out.resolve("methods.dex"),MergePayloads.holders(payload));
  MergePayloads.writeDex(out.resolve("loader.dex"),standalone.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var text=new ArrayList<String>();for(var row:rows.values())text.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),text);
  var actual=MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values());for(var e:actual.entrySet())require(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Emitted contract mismatch");
  var actualRt=MergePayloads.classes(out.resolve("runtime.dex").toString());for(var e:rt.entrySet())require(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(actualRt.get(e.getKey()))),"Existing helper changed");
  AUDIT.add("PASS\t9 layout methods repaired\t"+unchanged+" unrelated stock methods unchanged\t222 existing helper classes unchanged\t220 other app loaders unchanged\t"+helper.size()+" layout helper classes added");
  Files.write(Path.of(a[5]),AUDIT);for(String line:AUDIT)System.out.println(line);
 }
}
