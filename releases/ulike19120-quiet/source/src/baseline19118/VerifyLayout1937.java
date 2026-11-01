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

/** Check complete emitted contracts, preserve unrelated classes and undo exact native hooks. */
public final class VerifyLayout1937 {
 static int assertions;
 static void req(boolean b,String m){assertions++;if(!b)throw new IllegalStateException(m);}
 static String id(Method m){return MergePayloads.id(m);}
 static void loader(Path baseline,Path emitted)throws Exception{
  var old=MergePayloads.classes(baseline.toString());var next=MergePayloads.classes(emitted.toString());
  req(old.keySet().equals(next.keySet()),"Loader inventory changed");int hits=0;
  for(var e:old.entrySet()){
   ClassDef n=next.get(e.getKey());
   if(!e.getKey().equals(Transform1937.LOADER)){req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(n)),"Other app loader changed");continue;}
   var om=MergePayloads.methods(List.of(e.getValue()));var nm=MergePayloads.methods(List.of(n));
   req(om.keySet().equals(nm.keySet()),"Loader methods changed");var restored=new ArrayList<Method>();
   for(Method m:n.getMethods()){
    Method before=om.get(id(m));
    if(MergePayloads.hash(before).equals(MergePayloads.hash(m))){restored.add(m);continue;}
    String previous=null;int oldHits=0;
    for(Instruction x:before.getImplementation().getInstructions())if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith(Transform1937.OLD_DESCRIPTION)){previous=s.getString();oldHits++;}
    req(oldHits==1,"Unique baseline loader description");var b=new MutableMethodImplementation(m.getImplementation());int found=0;
    for(int i=0;i<b.getInstructions().size();i++){
     Instruction x=b.getInstructions().get(i);
     if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().equals(Transform1937.DESCRIPTION)){
      req(x.getOpcode()==Opcode.CONST_STRING,"Description opcode changed");
      b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(previous)));found++;
     }
    }req(found==1,"Unique replacement loader description");hits++;
    Method undo=Transform1937.replace(m,b);req(MergePayloads.hash(before).equals(MergePayloads.hash(undo)),"Loader changed beyond text");restored.add(undo);
   }
   req(MergePayloads.classHash(ImmutableClassDef.of(e.getValue())).equals(MergePayloads.classHash(ImmutableClassDef.of(Transform1937.members(n,restored)))),"Loader class shell changed");
  }req(hits==1,"Exactly one metadata change per loader");
 }
 static boolean references(Map<String,ClassDef> classes,String target){
  for(ClassDef c:classes.values())for(Method m:c.getMethods())if(m.getImplementation()!=null)
   for(Instruction x:m.getImplementation().getInstructions())if(Transform1937.ref(x).contains(target))return true;
  return false;
 }
 public static void main(String[] args)throws Exception{
  req(args.length==2||args.length==3,"BASE EMITTED [STOCK_APK]");Path base=Path.of(args[0]),out=Path.of(args[1]);
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
    if(!MergePayloads.hash(before).equals(MergePayloads.hash(e.getValue()))){req(Hooks1937.NATIVE.contains(key),"Unreviewed native changed");Hooks1937.verify(before,e.getValue());actualNative.add(key);}
   }else{req(Hooks1937.NATIVE.contains(key)&&seed.containsKey(key),"Unreviewed native added or seed absent");req(MergePayloads.hash(seed.get(key)).equals(rows.get(key)[1]),"New hook original pin differs from exact seed");Hooks1937.verify(seed.get(key),e.getValue());actualNative.add(key);}
  }req(actualNative.equals(Hooks1937.NATIVE),"Exact native hook set differs");
  if(args.length==3){var stock=MergePayloads.methods(MergePayloads.classes(args[2]).values());for(var e:rows.entrySet())req(stock.containsKey(e.getKey())&&MergePayloads.hash(stock.get(e.getKey())).equals(e.getValue()[1]),"Original APK native pin mismatch "+e.getKey());}
  var old=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var next=MergePayloads.classes(out.resolve("runtime.dex").toString());
  var compiler=MergePayloads.classes(out.getParent().resolve("helper-dex/classes.dex").toString());
  req(!compiler.isEmpty(),"No compiled production helpers");
  for(var e:compiler.entrySet())req(Transform1937.owned(e.getKey())&&next.containsKey(e.getKey())&&MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(next.get(e.getKey()))),"Compiler output differs from shipped helper "+e.getKey());
  for(var e:old.entrySet())if(!Transform1937.owned(e.getKey())){
   boolean hook=false;for(Method m:e.getValue().getMethods())hook|=Hooks1937.RUNTIME.contains(id(m));
   req(next.containsKey(e.getKey()),"Unrelated class missing");
   if(!hook)req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(next.get(e.getKey()))),"Unrelated class changed "+e.getKey());
   else{var before=MergePayloads.methods(List.of(e.getValue()));var after=MergePayloads.methods(List.of(next.get(e.getKey())));req(before.keySet().equals(after.keySet()),"Hook class inventory changed");for(var m:before.entrySet())if(Hooks1937.RUNTIME.contains(m.getKey()))Hooks1937.verify(m.getValue(),after.get(m.getKey()));else req(MergePayloads.hash(m.getValue()).equals(MergePayloads.hash(after.get(m.getKey()))),"Unrelated hook class method changed");req(MergePayloads.classHash(ImmutableClassDef.of(e.getValue())).equals(MergePayloads.classHash(ImmutableClassDef.of(Transform1937.members(next.get(e.getKey()),before.values())))),"Hook class fields/annotations/access changed "+e.getKey());}
  }
  for(String type:next.keySet())if(!old.containsKey(type))req(compiler.containsKey(type)&&Transform1937.owned(type),"Unexpected new runtime class");
  for(String name:Transform1937.PROTECTED){String k=Transform1937.P+name+";";req(MergePayloads.classHash(old.get(k)).equals(MergePayloads.classHash(next.get(k))),"Protected startup/recovery changed");}
  for(String family:List.of("FusionPixels1933","BurstCapture1933","FastResize1933","QualityPipeline1932","SpatialNoise1934","LongMoire1934","FaceRegions1934","YuvPlanes1934"))
   req(next.containsKey(Transform1937.P+family+";")&&references(next,Transform1937.P+family+";->"),"Requested helper is absent or unreferenced: "+family);
  loader(base.resolve("classes.dex"),out.resolve("loader.dex"));loader(base.resolve("bundle.dex"),out.resolve("bundle-loader.dex"));
  System.out.println("PASS1937 assertions="+assertions+"; compiled production bytecode equals shipped; native hook inverse verified; unchanged quality, speed, save and other apps; actual original APK contracts checked="+(args.length==3)+"; device untested");
 }
}
