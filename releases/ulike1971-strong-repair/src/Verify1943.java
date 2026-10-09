import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Check exact emitted capture/fusion families and preserve all other helper classes, native payloads and loader implementation. */
public final class Verify1943 {
 static int assertions;
 static void req(boolean b,String m){assertions++;if(!b)throw new IllegalStateException(m);}
 static String id(Method m){return MergePayloads.id(m);}
 static void loader(Path baseline,Path emitted)throws Exception{
  var old=MergePayloads.classes(baseline.toString());var next=MergePayloads.classes(emitted.toString());
  req(old.keySet().equals(next.keySet()),"Loader inventory changed");int hits=0;
  for(var e:old.entrySet()){
   ClassDef n=next.get(e.getKey());
   if(!e.getKey().equals(Transform1943.LOADER)){req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(n)),"Other app loader changed");continue;}
   var om=MergePayloads.methods(List.of(e.getValue()));var nm=MergePayloads.methods(List.of(n));
   req(om.keySet().equals(nm.keySet()),"Loader methods changed");var restored=new ArrayList<Method>();
   for(Method m:n.getMethods()){
    Method before=om.get(id(m));
    if(MergePayloads.hash(before).equals(MergePayloads.hash(m))){restored.add(m);continue;}
    String previous=null;int oldHits=0;
    for(Instruction x:before.getImplementation().getInstructions())if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith(Transform1943.OLD_DESCRIPTION)){previous=s.getString();oldHits++;}
    req(oldHits==1,"Unique baseline loader description");var b=new MutableMethodImplementation(m.getImplementation());int found=0;
    for(int i=0;i<b.getInstructions().size();i++){
     Instruction x=b.getInstructions().get(i);
     if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().equals(Transform1943.DESCRIPTION)){
      req(x.getOpcode()==Opcode.CONST_STRING,"Description opcode changed");
      b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(previous)));found++;
     }
    }req(found==1,"Unique replacement loader description");hits++;
    Method undo=Transform1943.replace(m,b);req(MergePayloads.hash(before).equals(MergePayloads.hash(undo)),"Loader changed beyond text");restored.add(undo);
   }
   req(MergePayloads.classHash(ImmutableClassDef.of(e.getValue())).equals(MergePayloads.classHash(ImmutableClassDef.of(Transform1943.members(n,restored)))),"Loader class shell changed");
  }req(hits==1,"Exactly one metadata change per loader");
 }
 static boolean references(Map<String,ClassDef> classes,String target){
  for(ClassDef c:classes.values())for(Method m:c.getMethods())if(m.getImplementation()!=null)
   for(Instruction x:m.getImplementation().getInstructions())if(Transform1943.ref(x).contains(target))return true;
  return false;
 }
 static void retainedIncomingLinks(Map<String,ClassDef> old,Map<String,ClassDef> next,Map<String,Method> nativeMethods){
  var targets=MergePayloads.methods(next.values());var fields=new TreeSet<String>();for(ClassDef c:next.values())for(Field f:c.getFields())fields.add(f.toString());
  var retained=MergePayloads.methods(old.values());retained.putAll(nativeMethods);
  for(Method m:retained.values()){
   if(Transform1943.owned(m.getDefiningClass())||Hooks1943.NATIVE.contains(id(m))||m.getImplementation()==null)continue;
   for(Instruction op:m.getImplementation().getInstructions())if(op instanceof ReferenceInstruction r){
    if(r.getReference() instanceof MethodReference mr && Transform1943.owned(mr.getDefiningClass()))req(targets.containsKey(mr.toString()),"Retained caller's replaced-family method still resolves: "+mr);
    if(r.getReference() instanceof FieldReference fr && Transform1943.owned(fr.getDefiningClass()))req(fields.contains(fr.toString()),"Retained caller's replaced-family field still resolves: "+fr);
   }
  }
 }
 static int calls(Method m,String ref){req(m!=null&&m.getImplementation()!=null,"Save-pipeline method present");int hits=0;for(Instruction op:m.getImplementation().getInstructions())if(Transform1943.ref(op).equals(ref))hits++;return hits;}
 static void finalSaveAudit(Map<String,ClassDef> runtime){
  var ms=MergePayloads.methods(runtime.values());String p=Transform1943.P,b="Landroid/graphics/Bitmap;",settings=p+"PhotoDetail$Settings;";
  String normalize=p+"QualityPipeline1932;->normalize("+b+"IZ)"+b;
  req(calls(ms.get(p+"SaveFd186;->saveStage("+b+"Ljava/io/File;I)Z"),normalize)==1,"HEIF/fd final-save normalize reaches reviewed pipeline exactly once");
  req(calls(ms.get(p+"SaveQuality2;->saveFinal("+b+"Ljava/io/File;Landroid/graphics/Bitmap$CompressFormat;I)Z"),normalize)==1,"Bitmap final-save normalize reaches reviewed pipeline exactly once");
  req(calls(ms.get(p+"PhotoDetail;->applyDetail("+b+b+")"+b),p+"QualityPipeline1932;->applyDetail("+b+b+settings+")"+b)==1,"Final detail stage reaches identity-bound deduplication guard");
  req(calls(ms.get(p+"ChromaPipeline186$Worker;->run()V"),p+"QualityPipeline1932;->run("+p+"ChromaPipeline186$State;"+p+"ChromaPipeline186$Buffer;)V")==1,"Existing strip worker reaches reviewed noise path");
 }
 public static void main(String[] args)throws Exception{
  req(args.length==2||args.length==3,"BASE EMITTED [STOCK_APK]");Path base=Path.of(args[0]),out=Path.of(args[1]);
  req(Files.mismatch(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"))==-1 && Files.mismatch(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"))==-1,"Native payload/contracts must remain byte-identical to .42");
  var oldNative=MergePayloads.methods(MergePayloads.classes(base.resolve("ulike/methods.dex").toString()).values());
  var nextNative=MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values());
  Path seedPath=out.getParent().resolve("baseline-stock-seed.dex");var seed=Files.isRegularFile(seedPath)?MergePayloads.methods(MergePayloads.classes(seedPath.toString()).values()):new TreeMap<String,Method>();
  var oldRows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());var rows=MergePayloads.contracts(out.resolve("methods.tsv").toString());
  req(rows.keySet().equals(nextNative.keySet()),"Emitted native contracts");req(nextNative.keySet().containsAll(oldNative.keySet()),"Native method removed");
  var actualNative=new TreeSet<String>();
  for(var e:nextNative.entrySet()){
   String key=e.getKey();req(MergePayloads.hash(e.getValue()).equals(rows.get(key)[2]),"Output contract mismatch");Method before=oldNative.get(key);
   if(before!=null){
    req(rows.get(key)[1].equals(oldRows.get(key)[1]),"Original native pin changed");
    if(!MergePayloads.hash(before).equals(MergePayloads.hash(e.getValue()))){req(Hooks1943.NATIVE.contains(key),"Unreviewed native changed");Hooks1943.verify(before,e.getValue());actualNative.add(key);}
   }else{req(Hooks1943.NATIVE.contains(key)&&seed.containsKey(key),"Unreviewed native added or seed absent");req(MergePayloads.hash(seed.get(key)).equals(rows.get(key)[1]),"New hook original pin differs from exact seed");Hooks1943.verify(seed.get(key),e.getValue());actualNative.add(key);}
  }req(actualNative.equals(Hooks1943.NATIVE),"Exact native hook set differs");
  if(args.length==3){var stock=MergePayloads.methods(MergePayloads.classes(args[2]).values());for(var e:rows.entrySet())req(stock.containsKey(e.getKey())&&MergePayloads.hash(stock.get(e.getKey())).equals(e.getValue()[1]),"Original APK native pin mismatch "+e.getKey());}
  var old=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var next=MergePayloads.classes(out.resolve("runtime.dex").toString());
  var compiler=MergePayloads.classes(out.getParent().resolve("helper-dex/classes.dex").toString());
  req(!compiler.isEmpty(),"No compiled production helpers");
  var roots=new TreeSet<String>();for(String k:compiler.keySet())if(!k.contains("$"))roots.add(k.substring(Transform1943.P.length(),k.length()-1));
  req(roots.equals(Transform1943.FAMILIES),"Exact production family inventory");
  for(var e:compiler.entrySet())req(Transform1943.owned(e.getKey())&&next.containsKey(e.getKey())&&MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(next.get(e.getKey()))),"Compiler output differs from shipped helper "+e.getKey());
  for(var e:old.entrySet())if(!Transform1943.owned(e.getKey())){
   boolean hook=false;for(Method m:e.getValue().getMethods())hook|=Hooks1943.RUNTIME.contains(id(m));
   req(next.containsKey(e.getKey()),"Unrelated class missing");
   if(!hook)req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(next.get(e.getKey()))),"Unrelated class changed "+e.getKey());
   else{var before=MergePayloads.methods(List.of(e.getValue()));var after=MergePayloads.methods(List.of(next.get(e.getKey())));req(before.keySet().equals(after.keySet()),"Hook class inventory changed");for(var m:before.entrySet())if(Hooks1943.RUNTIME.contains(m.getKey()))Hooks1943.verify(m.getValue(),after.get(m.getKey()));else req(MergePayloads.hash(m.getValue()).equals(MergePayloads.hash(after.get(m.getKey()))),"Unrelated hook class method changed");req(MergePayloads.classHash(ImmutableClassDef.of(e.getValue())).equals(MergePayloads.classHash(ImmutableClassDef.of(Transform1943.members(next.get(e.getKey()),before.values())))),"Hook class fields/annotations/access changed "+e.getKey());}
  }
  for(String type:next.keySet())if(!old.containsKey(type))req(compiler.containsKey(type)&&Transform1943.owned(type),"Unexpected new runtime class");
  for(String name:Transform1943.PROTECTED){String k=Transform1943.P+name+";";req(MergePayloads.classHash(old.get(k)).equals(MergePayloads.classHash(next.get(k))),"Protected startup/recovery changed");}
  for(String family:List.of("FusionPixels1933","BurstCapture1933","FastResize1933","SpatialNoise1934","LongMoire1934","FaceRegions1934","YuvPlanes1934"))
   req(next.containsKey(Transform1943.P+family+";")&&references(next,Transform1943.P+family+";->"),"Requested helper is absent or unreferenced: "+family);
  req(references(MergePayloads.classes(out.resolve("methods.dex").toString()),"Lcom/hiro/ulike/ShutterFeedback1941;->"),"Accepted shutter feedback reachable from native display callback");
  req(references(next,"Lcom/hiro/ulike/QualityPipeline1932;->"),"Retained .42 quality pipeline reachable from retained final-save hooks");
  retainedIncomingLinks(old,next,nextNative);
  finalSaveAudit(next);
  loader(base.resolve("classes.dex"),out.resolve("loader.dex"));loader(base.resolve("bundle.dex"),out.resolve("bundle-loader.dex"));
  System.out.println("PASS1943 assertions="+assertions+"; compiled production bytecode equals shipped; native payload byte-identical; unchanged camera startup/layout, .42 quality pipeline/math, save encoders, shutter feedback and other apps; only capture/fusion families replaced; actual original APK contracts checked="+(args.length==3)+"; device untested");
 }
}
