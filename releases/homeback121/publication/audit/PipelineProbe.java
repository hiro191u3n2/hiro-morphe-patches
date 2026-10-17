import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import app.hiro.oneback.patches.OneBackPatch;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Investigation-only: a hybrid fixture, never an original/clean Instagram APK.
 * Copies two complete method bodies from an OLD patched APK into metadata-only
 * hierarchy shells alongside a synthetic Android app. A1s retains its old
 * hiroOneBack$terminal$1 reference, so this is non-executable structural data.
 * The complete distributed MPP is subsequently run through Morphe CLI.
 */
public final class PipelineProbe {
  static final String MAIN="Lcom/instagram/mainactivity/InstagramMainActivity;", REELS="LX/8Zv;";
  // Legacy DexAudit.fixtures() deliberately includes this synthetic namespace
  // sentinel. It is not a shipped extension implementation. Keep its exact
  // original bytes, and reject every other runtime-prefixed input definition.
  static final String SENTINEL="Lapp/hiro/oneback/runtime/AlreadyLoaded;";
  static final String SENTINEL_SHA="b35263953e7f1c0834f89fb7597890619581439bbb75d4bbbd596902b0568f0b";
  static Method find(ClassDef c,String name,String result,List<String> params) {
    for(Method m:c.getMethods())if(m.getName().equals(name)&&m.getReturnType().equals(result)&&m.getParameterTypes().equals(params))return m;
    throw new AssertionError("Missing "+c.getType()+"->"+name);
  }
  static String hash(byte[] b)throws Exception {byte[] d=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder s=new StringBuilder();for(byte v:d)s.append(String.format("%02x",v&255));return s.toString();}
  static void check(boolean value,String why) {if(!value)throw new AssertionError(why);}
  static void addShell(String type,Map<String,ClassDef> original,Map<String,ClassDef> fixture,Map<String,List<Method>> keep) {
    if(type==null||fixture.containsKey(type)||!original.containsKey(type))return;
    ClassDef c=original.get(type);List<Method> methods=keep.getOrDefault(type,Collections.emptyList());
    fixture.put(type,new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
    addShell(c.getSuperclass(),original,fixture,keep);
  }
  static void nativeReferences(Method m) {
    for(Instruction i:HomeBackAudit.instructions(m)) {
      MethodReference r=HomeBackAudit.ref(i);
      if(r!=null&&(r.getDefiningClass().startsWith("Lapp/hiro/oneback/")||r.getName().startsWith("hiroOneBack$")))System.out.println("retainedOldHookReference="+m+" -> "+r+"; this fixture is NOT a clean/original or executable Instagram APK");
    }
  }
  static void fixture(String input,String seed,String output)throws Exception {
    Map<String,ClassDef> original=HomeBackAudit.read(input), fixture=HomeBackAudit.read(seed);
    Method main=find(original.get(MAIN),"A1s","V",Collections.singletonList("LX/4Jy;"));
    Method reels=find(original.get(REELS),"onBackPressed","Z",Collections.emptyList());
    Method module=find(original.get(REELS),"getModuleName","Ljava/lang/String;",Collections.emptyList());
    nativeReferences(main);nativeReferences(reels);nativeReferences(module);
    Map<String,List<Method>> keep=new LinkedHashMap<>();keep.put(MAIN,Collections.singletonList(main));keep.put(REELS,Arrays.asList(reels,module));
    addShell(MAIN,original,fixture,keep);addShell(REELS,original,fixture,keep);
    check(OneBackPatch.instagramMainReelsBoundary(fixture.get(MAIN),fixture)!=null,"Main fingerprint must match synthetic extracted fixture");
    check(OneBackPatch.instagramReelsBoundary(fixture.get(REELS),fixture)>=0,"Reels fingerprint must match synthetic extracted fixture");
    DexFileFactory.writeDexFile(output,new ImmutableDexFile(Opcodes.getDefault(),fixture.values()));
    Map<String,ClassDef> serialized=HomeBackAudit.read(output);
    System.out.println("scope=EXTRACTED_NATIVE_METHOD_SYNTHETIC_FIXTURE_NOT_ORIGINAL_APK");
    System.out.println("sourceAlreadyPatchedOldApk="+input);
    System.out.println("classes="+serialized.size());
    for(Method m:Arrays.asList(main,reels,module)) {
      Method after=find(serialized.get(m.getDefiningClass()),m.getName(),m.getReturnType(),new ArrayList<String>((Collection)m.getParameterTypes()));
      check(Arrays.equals(HomeBackAudit.methodBytes(m),HomeBackAudit.methodBytes(after)),"Copied native method changed in serialization");
      System.out.println("exactCopiedMethod="+m+"; instructions="+HomeBackAudit.instructions(m).size()+"; methodCanonicalSha256="+hash(HomeBackAudit.methodBytes(m)));
    }
    System.out.println("patchClassSource="+OneBackPatch.class.getProtectionDomain().getCodeSource().getLocation());
  }
  static void inspect(String path)throws Exception {
    Map<String,ClassDef> classes=HomeBackAudit.read(path);
    TreeMap<String,Integer> calls=new TreeMap<>();List<String> nativeCalls=new ArrayList<>();
    for(ClassDef c:classes.values())if(!c.getType().startsWith("Lapp/hiro/oneback/runtime/"))for(Method m:c.getMethods())for(Instruction i:HomeBackAudit.instructions(m)) {
      MethodReference r=HomeBackAudit.ref(i);
      if(r!=null&&r.getDefiningClass().startsWith("Lapp/hiro/oneback/runtime/")) {
        String k=r.getDefiningClass()+"->"+r.getName();calls.put(k,calls.getOrDefault(k,0)+1);
        if(r.getName().startsWith("tryInstagram"))nativeCalls.add(m+" -> "+r);
      }
    }
    System.out.println("apk="+path+"; classes="+classes.size());
    System.out.println("externalRuntimeCalls="+calls);
    System.out.println("nativeInstagramCalls="+nativeCalls);
    check(nativeCalls.size()==2,"Both actual targeted native hooks must be included exactly once after applyAll");
    check(classes.containsKey("Lapp/hiro/oneback/runtime/InstagramNativeLayers;"),"v120 extension class is missing");
    check(classes.containsKey("Lapp/hiro/oneback/runtime/OneBackExit;"),"Main extension class missing");
    for(String type:Arrays.asList(MAIN,REELS)) {
      ClassDef c=classes.get(type);System.out.println("outputMethods:"+type);for(Method m:c.getMethods())System.out.println("  "+m);
    }
  }
  static void verify(String beforePath,String afterPath)throws Exception {
    Map<String,ClassDef> before=HomeBackAudit.read(beforePath),after=HomeBackAudit.read(afterPath);
    int runtimeClasses=0;Set<String> expectedRuntimeTypes=new TreeSet<>();
    try(InputStream in=new BufferedInputStream(OneBackPatch.class.getResourceAsStream("/extensions/oneback_exit.mpe"))) {
      for(ClassDef c:com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile.fromInputStream(Opcodes.getDefault(),in).getClasses()) {
        check(after.containsKey(c.getType())&&Arrays.equals(HomeBackAudit.canonical(c),HomeBackAudit.canonical(after.get(c.getType()))),"Distributed extension class must survive full pipeline unchanged: "+c.getType());runtimeClasses++;expectedRuntimeTypes.add(c.getType());
      }
    }
    if(before.containsKey(SENTINEL)) {
      check(hash(HomeBackAudit.canonical(before.get(SENTINEL))).equals(SENTINEL_SHA),"Exact legacy synthetic sentinel input");
      check(after.containsKey(SENTINEL)&&Arrays.equals(HomeBackAudit.canonical(before.get(SENTINEL)),HomeBackAudit.canonical(after.get(SENTINEL))),"Synthetic namespace sentinel must remain unchanged");
      expectedRuntimeTypes.add(SENTINEL);
      System.out.println("PASS unchangedSyntheticNamespaceSentinel="+SENTINEL+"; canonicalSha256="+SENTINEL_SHA);
    }
    Set<String> actualRuntimeTypes=new TreeSet<>();for(String type:after.keySet())if(type.startsWith("Lapp/hiro/oneback/runtime/"))actualRuntimeTypes.add(type);
    check(actualRuntimeTypes.equals(expectedRuntimeTypes),"Exact distributed runtime type set, plus only the original synthetic sentinel when present");
    System.out.println("PASS distributedExtensionClassesByteExact="+runtimeClasses);
    for(String type:Arrays.asList(MAIN,REELS)) {
      boolean main=type.equals(MAIN);String name=main?"A1s":"onBackPressed",result=main?"V":"Z";
      List<String> params=main?Collections.singletonList("LX/4Jy;"):Collections.emptyList();
      Method old=find(before.get(type),name,result,params),now=find(after.get(type),name,result,params);
      List<Instruction> ins=HomeBackAudit.instructions(now),original=HomeBackAudit.instructions(old);int at=main?62:17;
      int[] branches=main?new int[]{37,48}:new int[]{8,12,15};
      check(ins.size()==original.size()+4,"Four and only four native method instructions inserted");
      check(HomeBackAudit.ref(ins.get(at)).getName().equals(main?"tryInstagramMainReelsBack":"tryInstagramReelsBack"),"Correct hook at verified boundary");
      check(HomeBackAudit.addr(ins,at+2)+((OffsetInstruction)ins.get(at+2)).getCodeOffset()==HomeBackAudit.addr(ins,at+4),"False result resumes exact old next instruction");
      for(int branch:branches)check(HomeBackAudit.addr(ins,branch)+((OffsetInstruction)ins.get(branch)).getCodeOffset()==HomeBackAudit.addr(ins,at),"Incoming branch must enter hook "+branch);
      MutableMethodImplementation restored=new MutableMethodImplementation(now.getImplementation());
      for(int i=0;i<4;i++)restored.removeInstruction(at);
      for(int branch:branches) {OneRegisterInstruction i=(OneRegisterInstruction)restored.getInstructions().get(branch);restored.replaceInstruction(branch,new BuilderInstruction21t(Opcode.IF_EQZ,i.getRegisterA(),restored.newLabelForIndex(at)));}
      Method restoredMethod=new ImmutableMethod(old.getDefiningClass(),old.getName(),old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),restored);
      check(Arrays.equals(HomeBackAudit.methodBytes(old),HomeBackAudit.methodBytes(restoredMethod)),"Full distributed pipeline changed only the four inserted instructions and incoming branches");
      System.out.println("PASS emittedNativeHook="+now+"; insertion="+at+"; incoming="+Arrays.toString(branches)+"; exactProvidedBodyRestored=true");
    }
    inspect(afterPath);
  }
  static Map<String,ClassDef> zipDex(String path,String entry)throws Exception {
    Map<String,ClassDef> classes=new TreeMap<>();
    try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(path);InputStream raw=zip.getInputStream(zip.getEntry(entry));InputStream in=new BufferedInputStream(raw)) {
      for(ClassDef c:com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile.fromInputStream(Opcodes.getDefault(),in).getClasses())classes.put(c.getType(),c);
    }
    return classes;
  }
  static void mergeProbe(String oldApk,String mpp,String fixtureApk,String workspace)throws Exception {
    Map<String,ClassDef> old=HomeBackAudit.read(oldApk),replacement=zipDex(mpp,"extensions/oneback_exit.mpe");
    app.morphe.patcher.PatcherConfig config=new app.morphe.patcher.PatcherConfig(new File(fixtureApk),new File(workspace),null,null,true,Collections.emptySet(),app.morphe.patcher.dex.BytecodeMode.FULL,app.morphe.patcher.dex.NoOpDexVerifier.INSTANCE,new File(workspace,"workspace"));
    app.morphe.patcher.patch.BytecodePatchContext ctx=new app.morphe.patcher.patch.BytecodePatchContext(config,new app.morphe.patcher.PackageMetadata("com.instagram.android","999.0","999",null));
    for(String type:Arrays.asList("Lapp/hiro/oneback/runtime/OneBackExit;","Lapp/hiro/oneback/runtime/InstagramBack;","Lapp/hiro/oneback/runtime/InstagramReels;")) {
      ClassDef oldClass=old.get(type),newClass=replacement.get(type);if(oldClass==null||newClass==null)continue;
      ClassDef merged=app.morphe.patcher.util.ClassMerger.INSTANCE.merge(oldClass,newClass,ctx);
      Map<String,Method> originalMethods=HomeBackAudit.methods(oldClass),newMethods=HomeBackAudit.methods(newClass),mergedMethods=HomeBackAudit.methods(merged);
      List<String> missing=new ArrayList<>(),retainedOldChanged=new ArrayList<>(),replaced=new ArrayList<>();
      for(Map.Entry<String,Method> e:newMethods.entrySet()) {
        String key=e.getKey();Method oldMethod=originalMethods.get(key),mergedMethod=mergedMethods.get(key);
        if(mergedMethod==null){missing.add(key);continue;}
        if(oldMethod!=null&&!Arrays.equals(HomeBackAudit.methodBytes(oldMethod),HomeBackAudit.methodBytes(e.getValue()))) {
          if(Arrays.equals(HomeBackAudit.methodBytes(oldMethod),HomeBackAudit.methodBytes(mergedMethod)))retainedOldChanged.add(key);
          else if(Arrays.equals(HomeBackAudit.methodBytes(e.getValue()),HomeBackAudit.methodBytes(mergedMethod)))replaced.add(key);
        }
      }
      System.out.println("classMergerClass="+type+"; oldMethods="+originalMethods.size()+"; replacementMethods="+newMethods.size()+"; mergedMethods="+mergedMethods.size());
      System.out.println("missingNewMethods="+missing);System.out.println("changedBodiesRetainedOld="+retainedOldChanged);System.out.println("changedBodiesReplaced="+replaced);
    }
    ctx.close();
  }
  static void bundle(String path)throws Exception {
    Map<String,ClassDef> classes=zipDex(path,"classes.dex");
    System.out.println("bundle="+path+"; topLevelClassesDexOnly="+classes.size());
    for(ClassDef c:classes.values()) {
      Set<String> strings=new TreeSet<>();
      for(Method m:c.getMethods())for(Instruction i:HomeBackAudit.instructions(m))if(i instanceof ReferenceInstruction) {
        Reference r=((ReferenceInstruction)i).getReference();if(r instanceof StringReference)strings.add(((StringReference)r).getString());
      }
      if(c.getType().toLowerCase().contains("instagram")||strings.contains("com.instagram.android")||c.getType().toLowerCase().contains("packagename")||strings.stream().anyMatch(s->s.contains("パッケージ")||s.toLowerCase().contains("change package")))System.out.println("class="+c.getType()+"; strings="+strings);
    }
  }
  static void manifest(String path)throws Exception {
    try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(path);InputStream in=zip.getInputStream(zip.getEntry("AndroidManifest.xml"))) {
      byte[] data=in.readAllBytes();com.reandroid.arsc.chunk.xml.AndroidManifestBlock m=com.reandroid.arsc.chunk.xml.AndroidManifestBlock.load(new ByteArrayInputStream(data));
      System.out.println("file="+path+"; manifestSha256="+hash(data)+"; package="+m.getPackageName()+"; versionName="+m.getVersionName()+"; versionCode="+m.getVersionCode()+"; targetSdk="+m.getTargetSdkVersion());
      com.reandroid.arsc.chunk.xml.ResXmlElement app=m.getApplicationElement();
      for(String attr:Arrays.asList("name","enableOnBackInvokedCallback","taskAffinity")) {
        com.reandroid.arsc.chunk.xml.ResXmlAttribute value=app.searchAttributeByName(attr);System.out.println("application."+attr+"="+(value==null?"<absent>":value.decodeValue()));
      }
      int activityCount=0,explicitTrue=0,explicitFalse=0;
      for(Iterator<com.reandroid.arsc.chunk.xml.ResXmlElement> it=app.getElements("activity");it.hasNext();) {
        com.reandroid.arsc.chunk.xml.ResXmlElement a=it.next();activityCount++;
        com.reandroid.arsc.chunk.xml.ResXmlAttribute v=a.searchAttributeByName("enableOnBackInvokedCallback"),name=a.searchAttributeByName("name");
        if(v!=null){if(v.getValueAsBoolean())explicitTrue++;else explicitFalse++;}
        if(name!=null&&name.decodeValue().contains("InstagramMainActivity"))System.out.println("mainActivity="+name.decodeValue()+"; enableOnBackInvokedCallback="+(v==null?"<absent>":v.decodeValue()));
      }
      System.out.println("activities="+activityCount+"; explicitCallbackTrue="+explicitTrue+"; explicitCallbackFalse="+explicitFalse);
    }
  }
  static void validateProjection(String path)throws Exception {
    Map<String,ClassDef> classes=HomeBackAudit.read(path);
    check(classes.size()==27,"Exact structural projection class count");
    Set<String> prefixed=new TreeSet<>();for(String type:classes.keySet())if(type.startsWith("Lapp/hiro/oneback/runtime/"))prefixed.add(type);
    check(prefixed.equals(Collections.singleton(SENTINEL)),"Only the original synthetic namespace sentinel may use the runtime prefix");
    check(hash(HomeBackAudit.canonical(classes.get(SENTINEL))).equals(SENTINEL_SHA),"Synthetic sentinel exact class, metadata and one RETURN_VOID method");
    Method main=find(classes.get(MAIN),"A1s","V",Collections.singletonList("LX/4Jy;"));
    Method reels=find(classes.get(REELS),"onBackPressed","Z",Collections.emptyList());
    Method module=find(classes.get(REELS),"getModuleName","Ljava/lang/String;",Collections.emptyList());
    Method[] methods={main,reels,module};
    String[] hashes={"442ef732bc1b95dce22fa99134755f88542b27189ad69f51a9fb169fd1cfa240","5a8eafdf79cbd8e2a78fbf1e62b5fb04f639aeb03e737a705089409e7393fe21","8afed414e9f5b00579f44a5e58a8c275d8d47d06e81e8d235504e2c2f7f59792"};
    int[] counts={148,408,8};
    for(int i=0;i<methods.length;i++)check(HomeBackAudit.instructions(methods[i]).size()==counts[i]&&hash(HomeBackAudit.methodBytes(methods[i])).equals(hashes[i]),"Provided method body changed: "+methods[i]);
    check(HomeBackAudit.instructions(main).stream().anyMatch(i->HomeBackAudit.ref(i)!=null&&HomeBackAudit.ref(i).getName().equals("hiroOneBack$terminal$1")),"Retained old terminal reference must remain; never call this a clean original");
    check(OneBackPatch.instagramMainReelsBoundary(classes.get(MAIN),classes)!=null,"Distributed main fingerprint admission");
    check(OneBackPatch.instagramReelsBoundary(classes.get(REELS),classes)>=0,"Distributed Reels fingerprint admission");
    System.out.println("PASS exact native structural projection: 27 classes, 3 complete provided bodies, old terminal reference retained; not executable or clean original");
    System.out.println("syntheticNamespaceSentinel="+SENTINEL+"; canonicalSha256="+SENTINEL_SHA+"; not a released runtime implementation");
    System.out.println("patchClassSource="+OneBackPatch.class.getProtectionDomain().getCodeSource().getLocation());
  }
  public static void main(String[] args)throws Exception {
    if(args[0].equals("validate-projection"))validateProjection(args[1]);
    else if(args[0].equals("fixture"))fixture(args[1],args[2],args[3]);
    else if(args[0].equals("inspect"))inspect(args[1]);
    else if(args[0].equals("verify"))verify(args[1],args[2]);
    else if(args[0].equals("bundle"))bundle(args[1]);
    else if(args[0].equals("merge-probe"))mergeProbe(args[1],args[2],args[3],args[4]);
    else if(args[0].equals("manifest"))for(int i=1;i<args.length;i++)manifest(args[i]);
    else throw new IllegalArgumentException();
  }
}
