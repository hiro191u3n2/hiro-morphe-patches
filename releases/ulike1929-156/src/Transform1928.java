import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
/** Small native-facing preference bridge; no camera startup replay or imaging change. */
public final class Transform1928 {
 static final String H="Lcom/hiro/ulike/FacingMemory1928;";
 static final String INITIAL="Li/f/n0/g/c;->t()Z", STORE="Li/f/j0/d/d;->J(Z)V", LIFECYCLE="Li/o/a/b1/a/g/y;->O1()V";
 static final String EXIT="Lcom/hiro/ulike/BackExit185;->finishOwnTasks()V";
 static final Set<String> TARGETS=Set.of(INITIAL,STORE,LIFECYCLE);
 static final String DESCRIPTION="v1.9.28（v1.9.27基準）終了時の前後カメラ選択を保存し次回起動へ反映。状態消失時の背面誤上書きを防止。画質・背面起動対策・他アプリ維持。実機未検証。";
 static void req(boolean b,String m){MergePayloads.require(b,m);}
 static String ref(Instruction i){return Transform1926.ref(i);}
 static Method repl(Method m,MethodImplementation impl){return Transform1926.repl(m,impl);}
 static ImmutableMethodReference call(String n,List<String> p,String r){return new ImmutableMethodReference(H,n,p,r);}
 static BuilderInstruction35c snapshot(){return new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,call("rememberCurrent",List.of(),"V"));}
 static Method apply(Method m){
  String id=MergePayloads.id(m);var b=new MutableMethodImplementation(m.getImplementation());int hits=0;
  if(id.equals(INITIAL)){
   for(int i=b.getInstructions().size()-1;i>=0;i--)if(b.getInstructions().get(i).getOpcode()==Opcode.RETURN){
    int r=((OneRegisterInstruction)b.getInstructions().get(i)).getRegisterA();
    // Replace the return itself so pre-existing branches cannot bypass restoration.
    b.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,r,0,0,0,0,call("initial",List.of("Z"),"Z")));
    b.addInstruction(i+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,r));
    b.addInstruction(i+2,new BuilderInstruction11x(Opcode.RETURN,r));hits++;
   }
  }else if(id.equals(STORE)){
   int p=b.getRegisterCount()-1;b=new MutableMethodImplementation(b.getRegisterCount());
   b.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,p,1,call("remember",List.of("Z"),"V")));
   b.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));hits++;
  }else if(id.equals(LIFECYCLE)){
   req(b.getRegisterCount()==6 && b.getTryBlocks().isEmpty(),"Lifecycle shape");
   int end=-1;for(int i=0;i<b.getInstructions().size();i++)if(ref(b.getInstructions().get(i)).equals(STORE)){end=i;hits++;}
   req(hits==1 && ref(b.getInstructions().get(0)).equals("Li/f/l/n/q/y/m;->a:Li/f/l/n/q/y/m;"),"Lifecycle model prefix");
   req(ref(b.getInstructions().get(end+1)).equals("Li/f/j0/d/e;->a:Li/f/j0/d/e;"),"Unrelated state suffix");
   // Drop only the nullable front-state read and false fallback. Keep the singleton
   // register and original independent settings suffix, including its branches.
   for(int i=end;i>=1;i--)b.removeInstruction(i);
   b.addInstruction(1,new BuilderInstruction11n(Opcode.CONST_4,3,1));
   b.addInstruction(2,new BuilderInstruction11n(Opcode.CONST_4,4,0));
   b.addInstruction(3,snapshot());
  }else throw new IllegalStateException("Unexpected target "+id);
  req(hits==1,"Exactly one native preference bridge "+id);return repl(m,b);
 }
 static Method exit(Method m){var b=new MutableMethodImplementation(m.getImplementation());b.addInstruction(0,snapshot());return repl(m,b);}
 static Map<String,ClassDef> metadata(Map<String,ClassDef> cs){
  var out=new TreeMap<String,ClassDef>();int edits=0;
  for(var c:cs.values()){var ms=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}
    var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int i=0;i<b.getInstructions().size();i++){var x=b.getInstructions().get(i);
     if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith("v1.9.27（v1.9.26基準）")){
      req(x.getOpcode()==Opcode.CONST_STRING,"Metadata encoding");b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));edits++;hit=true;
     }
    }ms.add(hit?repl(m,b):m);changed|=hit;
   }out.put(c.getType(),changed?Transform1926.cls(c,ms):c);
  }req(edits==1,"Exactly one loader version");return out;
 }
 public static void main(String[]a)throws Exception{
  req(a.length==6,"BASE HELPER BUNDLE_DEX SEED OUT AUDIT");Path base=Path.of(a[0]),out=Path.of(a[4]);Files.createDirectories(out);
  var old=MergePayloads.methods(MergePayloads.classes(base.resolve("ulike/methods.dex").toString()).values());var payload=new TreeMap<>(old);
  var rows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());req(old.keySet().equals(rows.keySet()),"Target sets");
  for(var e:old.entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Baseline hash "+e.getKey());
  var seeds=MergePayloads.methods(MergePayloads.classes(a[3]).values());req(seeds.keySet().equals(TARGETS),"Seed target set");var audit=new ArrayList<String>();
  for(var e:seeds.entrySet()){
   req(!old.containsKey(e.getKey()),"New target already patched "+e.getKey());var changed=apply(e.getValue());payload.put(e.getKey(),changed);
   rows.put(e.getKey(),new String[]{e.getKey(),MergePayloads.hash(e.getValue()),MergePayloads.hash(changed)});audit.add("STOCK\t"+String.join("\t",rows.get(e.getKey())));
  }
  for(var e:old.entrySet())req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(payload.get(e.getKey()))),"Existing native patch changed");
  var runtime=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var all=new TreeMap<String,ClassDef>();int ex=0,keptMethods=0;
  for(var c:runtime.values()){var ms=new ArrayList<Method>();boolean changed=false;for(var m:c.getMethods()){
   if(MergePayloads.id(m).equals(EXIT)){ms.add(exit(m));ex++;changed=true;}else{ms.add(m);keptMethods++;}
  }all.put(c.getType(),changed?Transform1926.cls(c,ms):c);}
  req(ex==1,"One exit snapshot hook");var helper=MergePayloads.classes(a[1]);req(helper.keySet().equals(Set.of(H)),"Stub/helper namespace leaked "+helper.keySet());
  req(!all.containsKey(H),"Helper collision");all.putAll(helper);
  var after=MergePayloads.methods(all.values());for(var m:MergePayloads.methods(runtime.values()).values())if(!MergePayloads.id(m).equals(EXIT))req(MergePayloads.hash(m).equals(MergePayloads.hash(after.get(MergePayloads.id(m)))),"Unrelated runtime changed");
  var single=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));var beforeBundle=MergePayloads.classes(a[2]);var bundle=metadata(beforeBundle);int kept=0;
  for(var e:beforeBundle.entrySet())if(!e.getKey().startsWith(MergePayloads.PATCH_NS)){req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Other-app loader changed");kept++;}
  MergePayloads.writeDex(out.resolve("runtime.dex"),all.values());MergePayloads.writeDex(out.resolve("methods.dex"),MergePayloads.holders(payload));MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var lines=new ArrayList<String>();for(var row:rows.values())lines.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),lines);
  for(var e:MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values()).entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Emitted target mismatch");
  audit.add("PASS\tnew_native_hooks=3\texisting_native_retained="+old.size()+"\texisting_runtime_methods_retained="+keptMethods+"\texisting_runtime_methods_changed=1\thelper_classes_added=1\tother_app_loaders_retained="+kept);Files.write(Path.of(a[5]),audit);audit.forEach(System.out::println);
 }
}
