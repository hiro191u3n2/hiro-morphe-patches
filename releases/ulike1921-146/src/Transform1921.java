import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Fail-closed, checksum-pinned transform; no changes to image processing or other apps. */
public final class Transform1921 {
 static final String P="Lcom/hiro/ulike/", B=P+"BackExit185;", F=P+"ExitFlow185;", E=P+"ExitBusy1921;", H=P+"BackExit185$Hook;";
 static final String ACT="Landroid/app/Activity;", OBJ="Ljava/lang/Object;";
 static final String DESCRIPTION="v1.9.21（v1.9.20基準）削除済み診断データの参照による保存中の誤判定を修正。終了待ちは再度の戻る・タッチ操作で取消可能、画面を閉じる前の待機は15秒で取消。実際の保存・録画は保護。設定・各パネルは撮影画面へ戻り、撮影画面は完全終了。実機未検証。";
 static final List<String> AUDIT=new ArrayList<>();
 static void check(boolean b,String s){MergePayloads.require(b,s);}
 static List<Instruction> ins(Method m){var out=new ArrayList<Instruction>();m.getImplementation().getInstructions().forEach(out::add);return out;}
 static Method replace(Method m,List<Instruction> code){var i=m.getImplementation();return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),new ImmutableMethodImplementation(i.getRegisterCount(),code,i.getTryBlocks(),i.getDebugItems()));}
 static Method make(String owner,String name,List<String> params,String result,int flags,int regs,List<Instruction> code){var ps=new ArrayList<ImmutableMethodParameter>();for(String s:params)ps.add(new ImmutableMethodParameter(s,Set.of(),null));return new ImmutableMethod(owner,name,ps,result,flags,Set.of(),Set.of(),new ImmutableMethodImplementation(regs,code,List.of(),List.of()));}
 static Instruction call(Opcode op,int count,int c,int d,String cls,String name,List<String> params,String ret){return new ImmutableInstruction35c(op,count,c,d,0,0,0,new ImmutableMethodReference(cls,name,params,ret));}
 static Set<String> strings(Method m){var s=new TreeSet<String>();for(Instruction i:ins(m))if(i instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference x)s.add(x.getString());return s;}
 static Set<String> api(ClassDef c){var out=new TreeSet<String>();for(Method m:c.getMethods())out.add(MergePayloads.id(m));return out;}
 static Map<String,ClassDef> edit(Map<String,ClassDef> before,boolean runtime){
  var out=new TreeMap<String,ClassDef>();int metadata=0,routes=0,guards=0,hooks=0,toasts=0;
  for(ClassDef c:before.values()){
   boolean changed=false;var methods=new ArrayList<Method>();
   for(Method m:c.getMethods()){
    if(m.getImplementation()==null){methods.add(m);continue;}
    if(runtime&&c.getType().equals(B)&&m.getName().equals("captureBusy")){
     check(strings(m).containsAll(Set.of("StyleStill4","callbacks","CaptureYuv","LensRelease163")),"Expected stale guard not found");
     Method n=make(B,"captureBusy",List.of(),"Z",m.getAccessFlags(),2,List.of(new ImmutableInstruction22c(Opcode.IGET_BOOLEAN,0,1,new ImmutableFieldReference(B,"closed","Z")),call(Opcode.INVOKE_STATIC,1,0,0,E,"captureBusy",List.of("Z"),"Z"),new ImmutableInstruction11x(Opcode.MOVE_RESULT,0),new ImmutableInstruction11x(Opcode.RETURN,0)));
     methods.add(n);guards++;changed=true;AUDIT.add("REPLACE_STALE_GUARD\t"+MergePayloads.id(m)+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(n));continue;
    }
    var code=ins(m);boolean edited=false;
    for(int i=0;i<code.size();i++){
     Instruction x=code.get(i);
     if(x instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s){
      String text=s.getString(),next=null;
      if(text.startsWith("v1.9.20 修正候補（v1.9.19基準）")){next=DESCRIPTION;metadata++;}
      if(runtime&&c.getType().equals(P+"BackExit185$1;")&&m.getName().equals("waiting")&&text.contains("完了後に終了")){next="保存・録画の完了後に終了します（戻る・タッチで終了待ちを取消）";toasts++;}
      if(runtime&&c.getType().equals(P+"BackExit185$1;")&&m.getName().equals("cancelled")){next="終了待ちを取り消しました。保存・録画の処理は中断しません。";toasts++;}
      if(next!=null){check(x.getOpcode()==Opcode.CONST_STRING,"String opcode drift");code.set(i,new ImmutableInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(next)));edited=true;}
     }
     if(runtime&&c.getType().equals(B)&&m.getName().equals("request")&&i==0){
      check(x instanceof ReferenceInstruction r&&r.getReference().toString().equals(P+"BackRoute1920;->consume("+B+ACT+")Z"),"Back route drift");
      code.set(i,call(Opcode.INVOKE_STATIC,2,2,3,E,"route",List.of(B,ACT),"Z"));edited=true;routes++;
     }
     if(runtime&&c.getType().equals(H)&&m.getName().equals("invoke")&&x instanceof ReferenceInstruction r&&r.getReference().toString().equals(F+"->requested()Z")){
      check(x instanceof FiveRegisterInstruction f&&f.getRegisterC()==7,"Window input register drift");
      if(hooks==0)code.set(i,call(Opcode.INVOKE_VIRTUAL,1,7,0,F,"blockInput1921",List.of(),"Z"));
      else code.set(i,call(Opcode.INVOKE_VIRTUAL,2,7,0,F,"blockCallback1921",List.of("Ljava/lang/String;"),"Z"));
      hooks++;edited=true;
     }
    }
    Method n=edited?replace(m,code):m;methods.add(n);changed|=edited;
    if(edited)AUDIT.add("CHANGED\t"+MergePayloads.id(m)+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(n));
   }
   if(runtime&&c.getType().equals(B)){
    methods.add(make(B,"cancelExit1921",List.of(B),"Z",9,1,List.of(new ImmutableInstruction22c(Opcode.IGET_OBJECT,0,0,new ImmutableFieldReference(B,"flow",F)),call(Opcode.INVOKE_VIRTUAL,1,0,0,F,"cancelPending1921",List.of(),"Z"),new ImmutableInstruction11x(Opcode.MOVE_RESULT,0),new ImmutableInstruction11x(Opcode.RETURN,0))));changed=true;
   }
   out.put(c.getType(),changed?new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods):c);
  }
  check(runtime?(routes==1&&guards==1&&hooks==2&&toasts==2&&metadata==0):metadata==1,"Transform count drift metadata="+metadata+" routes="+routes+" guard="+guards+" hooks="+hooks+" toasts="+toasts);
  return out;
 }
 static void inventory(Map<String,ClassDef> rt,Path file)throws Exception{
  String[][] expected={{"OpticalZoom","inFlight","Ljava/util/concurrent/atomic/AtomicInteger;"},{"OpticalZoom","stillUntil","J"},{"OpticalZoom","recording","Z"},{"CaptureYuv","states","Ljava/util/Map;"},{"CaptureYuv$State","delivering","Z"},{"CaptureYuv$State","selected","Z"},{"CaptureYuv$State","pending","*"},{"CaptureYuv$State","captureCallback","*"},{"LensRelease163","LOCK",OBJ},{"LensRelease163","callbacks","Ljava/util/Map;"},{"LensRelease163","nativeCallbacks","Ljava/util/Map;"},{"LensRelease163","deliveries","Ljava/util/Map;"},{"LensRelease163","stops","Ljava/util/Map;"},{"SaveQuality2","confirming","Ljava/util/concurrent/atomic/AtomicBoolean;"},{"PhotoDetail","BUSY","Ljava/util/concurrent/atomic/AtomicBoolean;"}};
  var rows=new ArrayList<String>();for(String[] e:expected){ClassDef c=rt.get(P+e[0]+";");check(c!=null,"Missing capture owner "+e[0]);Field found=null;for(Field f:c.getFields())if(f.getName().equals(e[1]))found=f;check(found!=null,"Missing capture field "+Arrays.toString(e));check(e[2].equals("*")||e[2].equals(found.getType()),"Capture field type drift");rows.add(e[0]+"\t"+e[1]+"\t"+found.getType()+"\t"+found.getAccessFlags());}
  for(Field f:rt.get(P+"StyleStill4;").getFields())check(!f.getName().equals("callbacks"),"Root cause no longer matches baseline");
  rows.add("StyleStill4\tcallbacks\tABSENT\t0");Files.write(file,rows);
 }
 public static void main(String[] a)throws Exception{
  check(a.length==5,"BASE HELPER BUNDLE_DEX OUT AUDIT");Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var loader0=MergePayloads.classes(base.resolve("classes.dex").toString());var helper=MergePayloads.classes(a[1]);
  check(before.size()==221&&loader0.size()==13,"1920 class count drift");check(helper.keySet().equals(Set.of(E,F,P+"ExitFlow185$Port;")),"Unexpected compiled helpers or stub leakage");
  inventory(before,out.resolve("busy-schema.tsv"));
  check(api(before.get(P+"ExitFlow185$Port;")).equals(api(helper.get(P+"ExitFlow185$Port;"))),"Exit Port ABI changed");
  check(helper.get(F).getAccessFlags()==before.get(F).getAccessFlags(),"Exit flow class access changed");
  check(api(helper.get(F)).containsAll(api(before.get(F))),"Legacy exit method removed");
  var rt=edit(before,true);rt.put(F,helper.get(F));check(rt.put(E,helper.get(E))==null,"Unexpected helper collision");
  var allowed=Set.of(B,H,P+"BackExit185$1;",F);int unchanged=0;
  for(ClassDef c:before.values())if(!allowed.contains(c.getType())){check(MergePayloads.classHash(c).equals(MergePayloads.classHash(rt.get(c.getType()))),"Unrelated helper changed "+c.getType());unchanged++;}
  var beforeM=MergePayloads.methods(before.values());var afterM=MergePayloads.methods(rt.values());
  for(Method m:before.get(F).getMethods())AUDIT.add("FLOW_REBUILD\t"+MergePayloads.id(m)+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(afterM.get(MergePayloads.id(m))));
  check(!strings(afterM.get(B+"->captureBusy()Z")).contains("StyleStill4"),"Stale reference survived");
  check(!strings(afterM.get(E+"->captureBusy(Z)Z")).contains("StyleStill4"),"Stale reference reintroduced");
  var loader=edit(loader0,false);var bundle0=MergePayloads.classes(a[2]);var bundle=edit(bundle0,false);int others=0;
  for(ClassDef c:bundle0.values())if(!c.getType().startsWith(MergePayloads.PATCH_NS)){check(MergePayloads.classHash(c).equals(MergePayloads.classHash(bundle.get(c.getType()))),"Other app loader changed");others++;}
  check(others==220,"Other loader count drift");
  MergePayloads.writeDex(out.resolve("runtime.dex"),rt.values());MergePayloads.writeDex(out.resolve("loader.dex"),loader.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"));Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"));
  Files.write(Path.of(a[4]),AUDIT);System.out.println("PASS removed obsolete capture guard; verified 15 live reflected fields; fixed2 input routes; "+unchanged+" runtime classes and220 other-app loaders unchanged; final runtime="+rt.size());
 }
}
