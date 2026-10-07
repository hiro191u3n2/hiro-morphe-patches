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

/** Exact, reviewed hooks for the six requested still-photo quality changes. */
public final class Transform1932 {
 static final String P="Lcom/hiro/ulike/", B="Landroid/graphics/Bitmap;";
 static final String PIPE=P+"QualityPipeline1932;", SHOT=P+"ShotContext1932;";
 static final String SETTINGS=P+"PhotoDetail$Settings;";
 static final String SETTINGS_FIELD=P+"PhotoDetail;->settings:"+SETTINGS;
 static final String REC="Lcom/ss/android/vesdk/VERecorder;";
 static final String STATE=P+"ChromaPipeline186$State;";
 static final String BUFFER=P+"ChromaPipeline186$Buffer;";
 static final String HELPER="Lcom/hiro/ulike/(QualityPipeline1932|QualityShadow1932|QualityPixels1932|ShotContext1932)(\\$[^;]+)?;";
 static final String LEGACY_SETTINGS=P+"PhotoDetail;->applyDetailLegacy177("+B+B+")"+B;
 static final String STATE_CTOR=STATE+"-><init>("+B+B+"IIIIIIZZZZ)V";
 static final String WATERMARK="Li/p/a/t/e;->d("+B+B+"IDD)"+B;
 static final String CONFIRM_COPY=P+"SaveQuality2;->confirmPublish(Li/o/a/i1/e/c;)V";
 static final String WATERMARK_HASH="ff8ddd52a708366663435721f0c8ef1bc9ac3d6098f2ace2cc1e5db355eaff4a";
 static final String OLD_DESCRIPTION="v1.9.31（v1.9.30基準）";
 static final String DESCRIPTION="v1.9.32（v1.9.31基準）候補1・2・8・12・15・20を統合。拡大前NR・高品質拡縮・撮影条件と実画像ノイズに応じたNR・美肌との強度調整・最終適応シャープ・周期色モアレ判定。既存のカメラ復帰修正と他アプリ保持。実機未確認。";
 static final Map<String,String[]> REDIRECTS=new TreeMap<>();
 static final Map<String,Hook> ENTRY=new TreeMap<>();
 static final Map<String,Observation> OBSERVE=new TreeMap<>();
 static final Map<String,String> ALIASES=new TreeMap<>();
 static final Set<String> INVALIDATE=new TreeSet<>();
 static final Set<String> NATIVE=new TreeSet<>(),RUNTIME=new TreeSet<>();
 static final class Hook {
  final String reference; final int[] registers; final int originalRegisters;
  Hook(String reference,int originalRegisters,int... registers){this.reference=reference;this.originalRegisters=originalRegisters;this.registers=registers;}
 }
 static final class Observation {
  final String anchor;final Opcode resultOpcode;final int resultRegister;final Hook hook;
  Observation(String anchor,Opcode opcode,int result,Hook hook){this.anchor=anchor;resultOpcode=opcode;resultRegister=result;this.hook=hook;}
 }
 static void redirect(String method,String old,String next){REDIRECTS.put(method,new String[]{old,next});}
 static {
  redirect(P+"SaveFd186;->saveStage("+B+"Ljava/io/File;I)Z",P+"SaveQuality2;->normalize186("+B+"IZ)"+B,PIPE+"->normalize("+B+"IZ)"+B);
  redirect(P+"SaveQuality2;->saveFinal("+B+"Ljava/io/File;Landroid/graphics/Bitmap$CompressFormat;I)Z",P+"SaveQuality2;->normalizePhoto("+B+"IZ)"+B,PIPE+"->normalize("+B+"IZ)"+B);
  redirect(P+"PhotoDetail;->applyDetail("+B+B+")"+B,P+"ChromaPipeline177;->apply("+B+B+SETTINGS+")"+B,PIPE+"->applyDetail("+B+B+SETTINGS+")"+B);
  redirect(P+"ChromaPipeline186$Worker;->run()V",P+"ShadowDetail1923;->run("+STATE+BUFFER+")V",PIPE+"->run("+STATE+BUFFER+")V");
  String smooth="->smoothRange([I[IIIIIIIIZI)V";
  redirect(P+"ShadowDetail1923;->run("+STATE+BUFFER+")V",P+"ShadowDetail1923;"+smooth,P+"QualityShadow1932;"+smooth);
  redirect(P+"ShadowDetail1923;->legacy("+P+"DetailPixels$Work;IIIIIIZZZ)V",P+"ShadowDetail1923;"+smooth,P+"QualityShadow1932;"+smooth);
  ENTRY.put(P+"LensRelease163;->cameraPhoto(Ljava/lang/Object;Ljava/lang/Object;)V",new Hook(SHOT+"->begin(Ljava/lang/Object;Ljava/lang/Object;)V",5,3,4));
  ENTRY.put(P+"CaptureYuv;->received(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/CaptureResult;)V",new Hook(SHOT+"->received(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/CaptureResult;)V",9,6,7,8));
  ENTRY.put(P+"LensRelease163;->bindDelivery(Ljava/lang/Object;Ljava/lang/Object;"+B+")V",new Hook(SHOT+"->bindDelivery(Ljava/lang/Object;Ljava/lang/Object;"+B+")V",7,4,5,6));
  ENTRY.put(P+"LensRelease163;->captureFailed(Ljava/lang/Object;I)V",new Hook(SHOT+"->failed(Ljava/lang/Object;I)V",3,1,2));
  OBSERVE.put(REC+"->updateComposerNode(Ljava/lang/String;Ljava/lang/String;F)I",new Observation("Lcom/ss/android/vesdk/TERecorderBase;->updateComposerNode(Ljava/lang/String;Ljava/lang/String;F)I",Opcode.MOVE_RESULT,0,new Hook(SHOT+"->observeBeautyResult(Ljava/lang/Object;Ljava/lang/String;FI)V",8,4,6,7,0)));
  OBSERVE.put(REC+"->updateMultiComposerNodes(I[Ljava/lang/String;[Ljava/lang/String;[F)I",new Observation("Lcom/ss/android/vesdk/TERecorderBase;->updateMultiComposerNodes(I[Ljava/lang/String;[Ljava/lang/String;[F)I",Opcode.MOVE_RESULT,4,new Hook(SHOT+"->observeBeautyArrayResult(Ljava/lang/Object;[Ljava/lang/String;[FI)V",8,3,6,7,4)));
  OBSERVE.put(WATERMARK,new Observation(B+"->createBitmap(IILandroid/graphics/Bitmap$Config;)"+B,Opcode.MOVE_RESULT_OBJECT,0,new Hook(SHOT+"->copy("+B+B+")V",16,9,0)));
  OBSERVE.put(CONFIRM_COPY,new Observation(B+"->copy(Landroid/graphics/Bitmap$Config;Z)"+B,Opcode.MOVE_RESULT_OBJECT,9,new Hook(SHOT+"->copy("+B+B+")V",13,1,9)));
  for(String suffix:List.of("appendComposerNodes([Ljava/lang/String;I)I","appendComposerNodesWithTag([Ljava/lang/String;I[Ljava/lang/String;)I","reloadComposerNodes([Ljava/lang/String;I)I","reloadComposerNodesWithTag([Ljava/lang/String;I[Ljava/lang/String;)I","removeComposerNodes([Ljava/lang/String;I)I","replaceComposerNodes([Ljava/lang/String;I[Ljava/lang/String;I)I","replaceComposerNodesWithTag([Ljava/lang/String;I[Ljava/lang/String;I[Ljava/lang/String;)I","setComposerMode(II)I","setComposerNodes([Ljava/lang/String;I)I","setComposerNodesWithTag([Ljava/lang/String;I[Ljava/lang/String;)I","setComposerResourcePath(Ljava/lang/String;)I"))INVALIDATE.add(REC+"->"+suffix);
  ALIASES.put(P+"PhotoDetail;->snapshot1932()"+SETTINGS,SETTINGS_FIELD);
  ALIASES.put(P+"ChromaPipeline177;->enabled1932()Z",P+"ChromaPipeline177;->enabled:Z");
  RUNTIME.addAll(REDIRECTS.keySet());RUNTIME.addAll(ENTRY.keySet());RUNTIME.add(LEGACY_SETTINGS);RUNTIME.add(STATE_CTOR);RUNTIME.add(CONFIRM_COPY);
  NATIVE.addAll(OBSERVE.keySet());NATIVE.remove(CONFIRM_COPY);NATIVE.addAll(INVALIDATE);
 }
 static void req(boolean b,String m){MergePayloads.require(b,m);}
 static String id(Method m){return MergePayloads.id(m);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Method repl(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef cls(ClassDef c,List<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static int words(Method m){int n=(m.getAccessFlags()&AccessFlags.STATIC.getValue())!=0?0:1;for(CharSequence p:m.getParameterTypes())n+=p.toString().equals("J")||p.toString().equals("D")?2:1;return n;}
 static ImmutableMethodReference mr(String s){
  int arrow=s.indexOf("->"),open=s.indexOf('(',arrow),close=s.indexOf(')',open);req(arrow>0&&open>arrow&&close>open,"Method descriptor "+s);
  var args=new ArrayList<String>();for(int i=open+1;i<close;){int start=i;while(s.charAt(i)=='[')i++;if(s.charAt(i)=='L')i=s.indexOf(';',i)+1;else i++;req(i>start&&i<=close,"Parameter descriptor");args.add(s.substring(start,i));}
  return new ImmutableMethodReference(s.substring(0,arrow),s.substring(arrow+2,open),args,s.substring(close+1));
 }
 static BuilderInstruction invoke(Hook h){
  int[] r=h.registers;boolean contiguous=true;for(int i=1;i<r.length;i++)contiguous&=r[i]==r[0]+i;
  if(contiguous)return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,r[0],r.length,mr(h.reference));
  req(r.length<=5,"Too many explicit registers");int[] v=new int[5];System.arraycopy(r,0,v,0,r.length);for(int x:r)req(x>=0&&x<16,"Explicit register overflow");
  return new BuilderInstruction35c(Opcode.INVOKE_STATIC,r.length,v[0],v[1],v[2],v[3],v[4],mr(h.reference));
 }
 static BuilderInstruction redirect(Instruction x,String next){
  if(x instanceof RegisterRangeInstruction r){req(x.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"Expected static range hook");return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,r.getStartRegister(),r.getRegisterCount(),mr(next));}
  if(x instanceof FiveRegisterInstruction r){req(x.getOpcode()==Opcode.INVOKE_STATIC,"Expected static hook");return new BuilderInstruction35c(Opcode.INVOKE_STATIC,r.getRegisterCount(),r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),mr(next));}
  throw new IllegalStateException("Unexpected invocation "+x.getOpcode());
 }
 static Method repair(Method m){
  String key=id(m);var b=new MutableMethodImplementation(m.getImplementation());int hits=0;
  if(REDIRECTS.containsKey(key)){
   String[] d=REDIRECTS.get(key);for(int i=0;i<b.getInstructions().size();i++){var x=b.getInstructions().get(i);if(ref(x).equals(d[0])){b.replaceInstruction(i,redirect(x,d[1]));hits++;}}
  }else if(ENTRY.containsKey(key)){
   Hook h=ENTRY.get(key);req(b.getRegisterCount()==h.originalRegisters,"Entry register ABI "+key);b.addInstruction(0,invoke(h));hits++;
  }else if(OBSERVE.containsKey(key)){
   Observation o=OBSERVE.get(key);req(b.getRegisterCount()==o.hook.originalRegisters,"Observer register ABI "+key);
   for(int i=0;i<b.getInstructions().size();i++)if(ref(b.getInstructions().get(i)).equals(o.anchor)){
    req(i+1<b.getInstructions().size(),"Missing SDK result");var result=b.getInstructions().get(i+1);req(result.getOpcode()==o.resultOpcode&&((OneRegisterInstruction)result).getRegisterA()==o.resultRegister,"SDK result ABI "+key);
    b.addInstruction(i+2,invoke(o.hook));i+=2;hits++;
   }
  }else if(INVALIDATE.contains(key)){
   int receiver=b.getRegisterCount()-words(m);b.addInstruction(0,invoke(new Hook(SHOT+"->invalidateBeauty(Ljava/lang/Object;)V",b.getRegisterCount(),receiver)));hits++;
  }else if(key.equals(LEGACY_SETTINGS)){
   for(int i=0;i<b.getInstructions().size();i++)if(ref(b.getInstructions().get(i)).equals(SETTINGS_FIELD)&&b.getInstructions().get(i).getOpcode()==Opcode.SGET_OBJECT){
    int r=((OneRegisterInstruction)b.getInstructions().get(i)).getRegisterA();b.addInstruction(i+1,invoke(new Hook(PIPE+"->settingsForLegacy("+SETTINGS+")"+SETTINGS,b.getRegisterCount(),r)));b.addInstruction(i+2,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,r));i+=2;hits++;
   }
  }else if(key.equals(STATE_CTOR)){
   int receiver=b.getRegisterCount()-words(m);for(int i=0;i<b.getInstructions().size();i++)if(b.getInstructions().get(i).getOpcode()==Opcode.RETURN_VOID){b.addInstruction(i,invoke(new Hook(PIPE+"->stateCreated("+STATE+")V",b.getRegisterCount(),receiver)));i++;hits++;}
  }else throw new IllegalStateException("Unreviewed method "+key);
  req(hits==1,"Exactly one approved insertion/redirect "+key+" got "+hits);return repl(m,b);
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> old){
  var out=new TreeMap<String,ClassDef>();int count=0;
  for(var c:old.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int j=0;j<b.getInstructions().size();j++){var x=b.getInstructions().get(j);
     if(x instanceof ReferenceInstruction ri&&ri.getReference() instanceof StringReference sr&&sr.getString().startsWith(OLD_DESCRIPTION)){
      req(c.getType().equals("Lapp/hiro/ulike/patches/UlikeHqMaxPatch;"),"Only ULike metadata can change");req(x.getOpcode()==Opcode.CONST_STRING,"Metadata encoding");b.replaceInstruction(j,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));count++;hit=true;
     }
    }ms.add(hit?repl(m,b):m);changed|=hit;
   }out.put(c.getType(),changed?cls(c,ms):c);
  }req(count==1,"One ULike loader metadata string");return out;
 }
 static String jsonList(Collection<String> values){var a=new ArrayList<String>();for(String s:values)a.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",a)+"]";}
 public static void main(String[] a)throws Exception{
  req(a.length==6,"BASE HELPER BUNDLE_DEX SEED OUT AUDIT");Path base=Path.of(a[0]),out=Path.of(a[4]);Files.createDirectories(out);
  var oldNativeClasses=MergePayloads.classes(base.resolve("ulike/methods.dex").toString());
  var old=MergePayloads.methods(oldNativeClasses.values());var payload=new TreeMap<>(old);
  var rows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());req(old.keySet().equals(rows.keySet()),"Baseline native inventory");
  for(var e:old.entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Baseline native contract "+e.getKey());
  var seed=MergePayloads.methods(MergePayloads.classes(a[3]).values());req(seed.keySet().equals(Set.of(WATERMARK)),"Pinned watermark seed inventory");req(MergePayloads.hash(seed.get(WATERMARK)).equals(WATERMARK_HASH),"Pinned watermark stock method");req(!old.containsKey(WATERMARK),"Watermark is already patched in baseline");
  var audit=new ArrayList<String>();for(String key:NATIVE){
   Method before=old.getOrDefault(key,seed.get(key));req(before!=null,"Native target missing "+key);Method after=repair(before);payload.put(key,after);
   if(!old.containsKey(key))rows.put(key,new String[]{key,MergePayloads.hash(before),MergePayloads.hash(after)});else rows.get(key)[2]=MergePayloads.hash(after);
   audit.add((old.containsKey(key)?"NATIVE_CHANGED":"NATIVE_ADDED")+"\t"+key+"\t"+MergePayloads.hash(before)+"\t"+MergePayloads.hash(after));
  }
  int keptNative=0;for(var e:old.entrySet())if(!NATIVE.contains(e.getKey())){req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(payload.get(e.getKey()))),"Unrelated native changed "+e.getKey());keptNative++;}
  var helper=MergePayloads.classes(a[1]);var helperMethods=MergePayloads.methods(helper.values());
  var oldRuntime=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var all=new TreeMap<String,ClassDef>();var touched=new TreeSet<String>();var aliases=new TreeSet<String>();
  for(var c:oldRuntime.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods())if(RUNTIME.contains(id(m))){Method after=repair(m);ms.add(after);changed=true;touched.add(id(m));audit.add("RUNTIME\t"+id(m)+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(after));}else ms.add(m);
   for(var e:ALIASES.entrySet())if(e.getKey().startsWith(c.getType()+"->")){
    req(ms.stream().noneMatch(m->id(m).equals(e.getKey())),"Alias collision");Method alias=helperMethods.get(e.getKey());req(alias!=null,"Missing compiled alias "+e.getKey());
    boolean field=false;for(Field f:c.getFields())field|=f.toString().equals(e.getValue());req(field,"Actual private settings field absent "+e.getValue());ms.add(alias);aliases.add(e.getKey());changed=true;
   }all.put(c.getType(),changed?cls(c,ms):c);
  }
  req(touched.equals(RUNTIME),"Runtime hook coverage "+touched);req(aliases.equals(ALIASES.keySet()),"Settings alias coverage");
  var newHelpers=new TreeSet<String>();var roots=new TreeSet<String>();for(var e:helper.entrySet()){
   if(e.getKey().matches(HELPER)){req(!all.containsKey(e.getKey()),"Helper collision "+e.getKey());all.put(e.getKey(),e.getValue());newHelpers.add(e.getKey());if(!e.getKey().contains("$"))roots.add(e.getKey());}
   else req(e.getKey().equals(P+"PhotoDetail;")||e.getKey().equals(P+"ChromaPipeline177;"),"Stub leaked "+e.getKey());
  }
  req(roots.equals(Set.of(PIPE,SHOT,P+"QualityPixels1932;",P+"QualityShadow1932;")),"Four quality helper roots required");
  var afterMethods=MergePayloads.methods(all.values());int keptRuntime=0;for(var e:MergePayloads.methods(oldRuntime.values()).entrySet())if(!RUNTIME.contains(e.getKey())){req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(afterMethods.get(e.getKey()))),"Unrelated runtime changed "+e.getKey());keptRuntime++;}
  var single=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));var oldBundle=MergePayloads.classes(a[2]);var bundle=metadata(oldBundle);int keptLoaders=0;
  for(var e:oldBundle.entrySet())if(!e.getKey().startsWith(MergePayloads.PATCH_NS)){req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Other app loader changed");keptLoaders++;}
  var nativeClasses=new TreeMap<String,ClassDef>();
  for(ClassDef holder:MergePayloads.holders(payload)){
   ClassDef previous=oldNativeClasses.get(holder.getType());
   var members=new ArrayList<Method>();holder.getMethods().forEach(members::add);
   boolean modified=members.stream().anyMatch(m->NATIVE.contains(id(m)));
   nativeClasses.put(holder.getType(),previous==null?holder:modified?cls(previous,members):previous);
  }
  MergePayloads.writeDex(out.resolve("runtime.dex"),all.values());MergePayloads.writeDex(out.resolve("methods.dex"),nativeClasses.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var lines=new ArrayList<String>();for(var row:rows.values())lines.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),lines);
  for(var e:MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values()).entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Serialized native contract "+e.getKey());
  Files.writeString(out.resolve("quality-inventory.json"),"{\n\"changed_runtime_methods\":"+jsonList(RUNTIME)+",\n\"changed_native_methods\":"+jsonList(NATIVE)+",\n\"new_helper_classes\":"+jsonList(newHelpers)+",\n\"new_runtime_aliases\":"+jsonList(aliases)+",\n\"new_native_methods\":"+jsonList(Set.of(WATERMARK))+"\n}\n");
  audit.add("PASS\texisting_native_methods_retained="+keptNative+"\texisting_native_methods_changed="+(NATIVE.size()-1)+"\tnew_native_hooks=1\texisting_runtime_methods_retained="+keptRuntime+"\texisting_runtime_methods_changed="+RUNTIME.size()+"\thelper_classes_added="+newHelpers.size()+"\tother_app_loaders_retained="+keptLoaders);
  Files.write(Path.of(a[5]),audit);audit.forEach(System.out::println);
 }
}
