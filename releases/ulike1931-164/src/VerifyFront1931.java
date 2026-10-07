import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Verify emitted DEX against the old bytes and reverse only the reviewed edits. */
public final class VerifyFront1931 {
 static final String H="Lcom/hiro/ulike/FrontPreview1931;";
 static void req(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static void cancel(Method old,Method now){
  var b=new MutableMethodImplementation(now.getImplementation());var x=b.getInstructions().get(0);
  req(x.getOpcode()==Opcode.INVOKE_STATIC_RANGE&&ref(x).equals(H+"->cancel(Ljava/lang/Object;)V"),"Cancellation must be the first operation "+MergePayloads.id(now));
  var r=(RegisterRangeInstruction)x;
  req(r.getRegisterCount()==1&&r.getStartRegister()==b.getRegisterCount()-Transform1931.words(now),"Cancellation must receive capture identity");
  b.removeInstruction(0);
  req(MergePayloads.hash(old).equals(MergePayloads.hash(Transform1931.repl(now,b))),"Cancellation altered original native body "+MergePayloads.id(now));
 }
 static void call(Method old,Method now,String oldRef){
  MethodReference original=null;
  for(var x:old.getImplementation().getInstructions())if(ref(x).equals(oldRef))original=(MethodReference)((ReferenceInstruction)x).getReference();
  req(original!=null,"Original reviewed call absent");
  var b=new MutableMethodImplementation(now.getImplementation());String newRef=H+oldRef.substring(oldRef.indexOf("->"));int hits=0;
  for(int j=0;j<b.getInstructions().size();j++){
   var x=b.getInstructions().get(j);if(!ref(x).equals(newRef))continue;
   if(x instanceof RegisterRangeInstruction r)b.replaceInstruction(j,new BuilderInstruction3rc(x.getOpcode(),r.getStartRegister(),r.getRegisterCount(),original));
   else if(x instanceof FiveRegisterInstruction r)b.replaceInstruction(j,new BuilderInstruction35c(x.getOpcode(),r.getRegisterCount(),r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),original));
   else throw new IllegalStateException("Unexpected call encoding");hits++;
  }
  req(hits==1,"Expected one bridge call "+MergePayloads.id(now));
  req(MergePayloads.hash(old).equals(MergePayloads.hash(Transform1931.repl(now,b))),"Unreviewed native body change "+MergePayloads.id(now));
 }
 static String array(Collection<String> rows){var a=new ArrayList<String>();for(String s:rows)a.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",a)+"]";}
 public static void main(String[]a)throws Exception{
  req(a.length>=2,"BASE EMITTED [STOCK]");Path base=Path.of(a[0]),out=Path.of(a[1]);
  var oldNative=MergePayloads.methods(MergePayloads.classes(base.resolve("ulike/methods.dex").toString()).values());
  var newNative=MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values());
  var oldRuntime=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var newRuntime=MergePayloads.classes(out.resolve("runtime.dex").toString());
  var oldRows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());var rows=MergePayloads.contracts(out.resolve("methods.tsv").toString());
  req(newNative.keySet().equals(rows.keySet()),"Emitted native inventory mismatch");
  var added=new TreeSet<>(newNative.keySet());added.removeAll(oldNative.keySet());req(added.equals(Transform1931.CANCEL),"Only canonical stop/switch/destroy hooks may be added");
  req(newNative.keySet().containsAll(oldNative.keySet()),"Existing native method removed");
  for(var e:newNative.entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Emitted native hash mismatch");
  int nativeKept=0,runtimeKept=0;
  for(var e:oldNative.entrySet()){
   String key=e.getKey();req(rows.get(key)[1].equals(oldRows.get(key)[1]),"Original APK hash contract overwritten "+key);
   if(Transform1931.CALLS.containsKey(key))call(e.getValue(),newNative.get(key),Transform1931.CALLS.get(key));
   else{req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(newNative.get(key))),"Unrelated native method changed "+key);nativeKept++;}
  }
  var oldMethods=MergePayloads.methods(oldRuntime.values());var newMethods=MergePayloads.methods(newRuntime.values());
  for(var e:oldMethods.entrySet()){
   String key=e.getKey();req(newMethods.containsKey(key),"Runtime method removed "+key);
   if(Transform1931.RUNTIME_CALLS.containsKey(key))call(e.getValue(),newMethods.get(key),Transform1931.RUNTIME_CALLS.get(key));
   else if(Transform1931.RUNTIME_CANCEL.contains(key))cancel(e.getValue(),newMethods.get(key));
   else{req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(newMethods.get(key))),"Unrelated runtime method changed "+key);runtimeKept++;}
  }
  var helpers=new TreeSet<>(newRuntime.keySet());helpers.removeAll(oldRuntime.keySet());
  req(helpers.contains(H),"Front startup helper missing");
  for(String key:helpers)req(key.matches("Lcom/hiro/ulike/FrontPreview1931(\\$[^;]+)?;"),"Unexpected helper or test fixture "+key);
  for(String type:List.of("Lcom/hiro/ulike/FacingMemory1928;","Lcom/hiro/ulike/RearLensUi1930;","Lcom/hiro/ulike/PreviewStart1927;","Lcom/hiro/ulike/ProviderLifecycle1929;","Lcom/hiro/ulike/PreviewInputs1929;"))
   req(MergePayloads.classHash(oldRuntime.get(type)).equals(MergePayloads.classHash(newRuntime.get(type))),"Existing camera helper changed "+type);
  if(a.length>2){
   var stock=MergePayloads.methods(MergePayloads.classes(a[2]).values());
   for(String key:Transform1931.CANCEL){req(stock.containsKey(key),"Stock canonical method missing");req(rows.get(key)[1].equals(MergePayloads.hash(stock.get(key))),"Wrong original seed "+key);cancel(stock.get(key),newNative.get(key));}
  }
  String json="{\n\"changed_native_methods\":"+array(Transform1931.NATIVE)+",\n\"new_native_hooks\":"+array(Transform1931.CANCEL)+",\n\"changed_runtime_methods\":"+array(Transform1931.RUNTIME)+",\n\"new_helper_classes\":"+array(helpers)+"\n}\n";
  Files.writeString(out.resolve("front-inventory.json"),json);
  System.out.println("PASS native bridges="+Transform1931.CALLS.size()+", new cancellation hooks="+added.size()+", native methods retained="+nativeKept+", runtime bridges="+Transform1931.RUNTIME.size()+", runtime methods retained="+runtimeKept+", production helper classes="+helpers.size()+", original stop/switch contracts="+(a.length>2?"verified":"deferred to stock validation"));
 }
}
