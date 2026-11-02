import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import app.morphe.patcher.*;
import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.resource.*;
import app.morphe.patcher.dex.*;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Actual emitted JVM loaders and real Morphe 2.2 resource coder; synthetic APK content only. */
public final class ResourceBehavior19124 {
 static final String PREFIX="app.hiro.ulike.patches.", LOADER=PREFIX+"UlikeHqMaxPatch", INSTALLER=PREFIX+"IntegrationPayload186";
 static final String ASSET="assets/81001.zip", NATIVE="lib/arm64-v8a/libttvesdk.so", CHROMA="lib/arm64-v8a/libulike_chroma186.so";
 static final byte[] ORIGINAL="synthetic original material".getBytes(StandardCharsets.UTF_8), REPLACEMENT="synthetic patched material".getBytes(StandardCharsets.UTF_8), INVALID_NATIVE="synthetic incompatible native; must be rejected".getBytes(StandardCharsets.UTF_8);
 static int assertions, negatives; static Path work; static Map<String,byte[]> candidate,baseline;
 static void check(boolean condition,String why){assertions++;if(!condition)throw new AssertionError(why);}
 static String sha(byte[] value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}
 static Map<String,byte[]> archive(Path input)throws Exception{Map<String,byte[]> out=new TreeMap<>();try(ZipFile z=new ZipFile(input.toFile())){for(var e:Collections.list(z.entries()))if(!e.isDirectory())try(var s=z.getInputStream(e)){out.put(e.getName(),s.readAllBytes());}}return out;}
 static final class Loader extends ClassLoader implements AutoCloseable {
  final Map<String,byte[]> data;
  Loader(Map<String,byte[]> raw){super(ResourceBehavior19124.class.getClassLoader());data=raw;}
  protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
   if(!name.startsWith(PREFIX))return super.loadClass(name,resolve);
   Class<?> c=findLoadedClass(name);if(c==null){byte[] raw=data.get(name.replace('.','/')+".class");if(raw==null)throw new ClassNotFoundException(name);c=defineClass(name,raw,0,raw.length);}if(resolve)resolveClass(c);return c;
  }
  public InputStream getResourceAsStream(String name){byte[] raw=data.get(name.startsWith("/")?name.substring(1):name);return raw==null?super.getResourceAsStream(name):new ByteArrayInputStream(raw);}
  public void close(){}
 }
 static byte[] manifest()throws Exception {
  AndroidManifestBlock m=new AndroidManifestBlock();
  m.newElement("manifest").fromJson(new com.reandroid.json.JSONObject("{\"name\":\"manifest\",\"attributes\":[{\"name\":\"package\",\"value_type\":\"STRING\",\"data\":\"com.gorgeous.liteinternational\"},{\"name\":\"versionName\",\"id\":16843292,\"uri\":\"http://schemas.android.com/apk/res/android\",\"prefix\":\"android\",\"value_type\":\"STRING\",\"data\":\"5.6.2\"},{\"name\":\"versionCode\",\"id\":16843291,\"uri\":\"http://schemas.android.com/apk/res/android\",\"prefix\":\"android\",\"value_type\":\"DEC\",\"data\":740}]}"));
  m.getOrCreateApplicationElement();m.refreshFull();return m.getBytes();
 }
 static ResourcePatchContext context(String id,Map<String,byte[]> entries)throws Exception {
  Path root=work.resolve(id);Files.createDirectories(root);Path apk=root.resolve("synthetic.apk");
  TableBlock table=new TableBlock();table.newPackage(0x7f,"com.gorgeous.liteinternational");table.refreshFull();
  Map<String,byte[]> all=new TreeMap<>(entries);all.put("AndroidManifest.xml",manifest());all.put("resources.arsc",table.getBytes());
  try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(apk))){for(var row:all.entrySet()){ZipEntry e=new ZipEntry(row.getKey());e.setTime(315532800000L);z.putNextEntry(e);z.write(row.getValue());z.closeEntry();}}
  PatcherConfig c=new PatcherConfig(apk.toFile(),root.resolve("patcher").toFile(),Set.of(),BytecodeMode.FULL,NoOpDexVerifier.INSTANCE,null,"2.2.0",false);
  c.initializeTemporaryFilesDirectories$morphe_patcher();c.setBehaviors$morphe_patcher(Set.of(PatcherBehavior.ON_REQUEST_RESOURCE_DECODING));
  ResourcePatchContext result=new ResourcePatchContext(c);result.decodeResources$morphe_patcher(ResourceMode.FULL);return result;
 }
 static Throwable invoke(Loader loader,String name,String method,ResourcePatchContext context)throws Exception {
  Method m=loader.loadClass(name).getDeclaredMethod(method,ResourcePatchContext.class);m.setAccessible(true);
  try{m.invoke(null,context);return null;}catch(InvocationTargetException failure){return failure.getCause();}
 }
 static String messages(Throwable error){StringBuilder out=new StringBuilder();while(error!=null){out.append(error.getClass().getSimpleName()).append(": ").append(error.getMessage()).append('\n');error=error.getCause();}return out.toString();}
 static Map<String,byte[]> assets(Map<String,byte[]> input,String inputHash,String outputHash)throws Exception {
  Map<String,byte[]> result=new TreeMap<>(input);result.put("ulike/assets.tsv",(ASSET+"\t"+inputHash+"\t"+outputHash+"\tulike/assets/0000.bin\n").getBytes(StandardCharsets.UTF_8));result.put("ulike/assets/0000.bin",REPLACEMENT);return result;
 }
 static void installCase(String id,Map<String,byte[]> input,byte[] existing,boolean reject)throws Exception {
  Map<String,byte[]> entries=new TreeMap<>();if(existing!=null)entries.put(CHROMA,existing);
  try(Loader loader=new Loader(input);ResourcePatchContext ctx=context(id,entries)){
   check(!ctx.get(CHROMA,false).exists(),id+": original library must start unextracted");
   Throwable error=invoke(loader,INSTALLER,"install",ctx);
   if(reject){check(error!=null&&messages(error).contains("Conflicting existing runtime resource"),id+": incompatible archive library must be rejected");check(Arrays.equals(Files.readAllBytes(ctx.get(CHROMA,false).toPath()),existing),id+": conflicting library must stay unchanged");}
   else {
    check(error==null,id+": accepted installation: "+messages(error));
    // Check every installation descriptor reflected from the actual class, not a fixture copy of the list.
    Field f=loader.loadClass(INSTALLER).getDeclaredField("ITEMS");f.setAccessible(true);String[][] items=(String[][])f.get(null);check(items.length==14,id+": current native inventory");
    for(String[] item:items){byte[] out=Files.readAllBytes(ctx.get(item[0],false).toPath());check(out.length==Long.parseLong(item[2])&&sha(out).equals(item[1]),id+": installed payload "+item[0]);}
   }
  }
 }
 static void assetCase(String id,Map<String,byte[]> input,byte[] original,String inHash,String outHash,String failureKind,boolean replaced)throws Exception {
  Map<String,byte[]> entries=new TreeMap<>();if(original!=null)entries.put(ASSET,original);entries.put(NATIVE,INVALID_NATIVE);
  try(Loader loader=new Loader(assets(input,inHash,outHash));ResourcePatchContext ctx=context(id,entries)){
   check(!ctx.get(ASSET,false).exists(),id+": material starts unextracted");check(!ctx.get(NATIVE,false).exists(),id+": native starts unextracted");
   Throwable failure=invoke(loader,LOADER,"applyAssets",ctx);String text=messages(failure);
   check(failure!=null,id+": synthetic invalid input must not be accepted");
   if(failureKind.equals("missing"))check(text.contains("対象素材がありません"),id+": missing original guard "+text);
   else if(failureKind.equals("input-hash"))check(text.contains("素材")&&!text.contains("対象素材がありません")&&!text.contains(sha(INVALID_NATIVE)),id+": original hash guard "+text);
   else if(failureKind.equals("payload-hash"))check(text.contains("素材")&&!text.contains("対象素材がありません")&&!text.contains(sha(INVALID_NATIVE)),id+": payload hash guard "+text);
   else if(failureKind.equals("native-hash")){check(text.contains(sha(INVALID_NATIVE)),id+": original native must be extracted and rejected by its unchanged hash contract "+text);check(Arrays.equals(Files.readAllBytes(ctx.get(NATIVE,false).toPath()),INVALID_NATIVE),id+": rejected native unchanged");}
   File path=ctx.get(ASSET,false);
   if(original==null)check(!path.exists(),id+": genuinely missing material not fabricated");
   else if(failureKind.equals("missing"))check(!path.exists(),id+": legacy path not materialized");
   else check(Arrays.equals(Files.readAllBytes(path.toPath()),replaced?REPLACEMENT:original),id+": material write atomicity/guard preserved");
  }
 }
 static Map<String,byte[]> mutate(Map<String,byte[]> from,String id)throws Exception {
  Map<String,byte[]> result=new TreeMap<>(from);String owner=id.equals("native-resolver")||id.equals("manifest-parent")?INSTALLER:LOADER;
  String entry=owner.replace('.','/')+".class";ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(from.get(entry)).accept(n,0);int hits=0,site=0;
  for(MethodNode m:n.methods){
   if(id.equals("remove-required-input-guard")&&m.name.equals("require")&&m.desc.equals("(ZLjava/lang/String;)V")){m.instructions.clear();m.instructions.add(new InsnNode(Opcodes.RETURN));m.tryCatchBlocks.clear();m.localVariables=null;m.maxStack=0;hits++;continue;}
   if(id.equals("remove-required-input-guard"))continue;
   boolean selected=m.name.equals(id.equals("native-resolver")?"lambda$install$0":id.equals("manifest-parent")?"install":"applyAssets");if(!selected)continue;
   for(AbstractInsnNode ins:m.instructions.toArray())if(ins instanceof MethodInsnNode call&&call.owner.equals("app/morphe/patcher/patch/ResourcePatchContext")&&call.name.equals("get")&&call.desc.equals("(Ljava/lang/String;Z)Ljava/io/File;")){
    int at=site++;if((id.equals("asset-read")&&at!=0)||(id.equals("original-native-read")&&at!=1))continue;
    AbstractInsnNode operand=ins.getPrevious();while(operand.getOpcode()<0)operand=operand.getPrevious();int old=id.equals("manifest-parent")?Opcodes.ICONST_0:Opcodes.ICONST_1;
    if(operand.getOpcode()!=old)throw new AssertionError("Mutation target changed "+id);
    m.instructions.set(operand,new InsnNode(old==Opcodes.ICONST_0?Opcodes.ICONST_1:Opcodes.ICONST_0));hits++;
   }
  }
  if(hits!=1)throw new AssertionError("Mutation count "+id+" = "+hits);ClassWriter w=new ClassWriter(0);n.accept(w);result.put(entry,w.toByteArray());return result;
 }
 interface Checked {void run()throws Exception;}
 static void negative(String name,Checked test)throws Exception {try{test.run();throw new IllegalStateException("Mutation was not detected: "+name);}catch(AssertionError expected){negatives++;}}
 public static void main(String[] args)throws Exception {
  if(args.length!=3)throw new IllegalArgumentException("BASE_MPP CANDIDATE_MPP WORK");baseline=archive(Path.of(args[0]));candidate=archive(Path.of(args[1]));work=Path.of(args[2]).toAbsolutePath();Files.createDirectories(work);
  String originalHash=sha(ORIGINAL),replacementHash=sha(REPLACEMENT);
  try(ResourcePatchContext c=context("actual-lazy-semantics",Map.of(ASSET,ORIGINAL))){check(!c.get(ASSET,false).exists(),"false must not extract in actual patcher");check(Arrays.equals(Files.readAllBytes(c.get(ASSET,true).toPath()),ORIGINAL),"true must extract exact original bytes");}
  assetCase("baseline-reproduces-missing",baseline,ORIGINAL,originalHash,replacementHash,"missing",false);
  assetCase("candidate-material-and-native-extracted",candidate,ORIGINAL,originalHash,replacementHash,"native-hash",true);
  assetCase("genuinely-missing",candidate,null,originalHash,replacementHash,"missing",false);
  assetCase("wrong-original-hash",candidate,"different material".getBytes(StandardCharsets.UTF_8),originalHash,replacementHash,"input-hash",false);
  assetCase("wrong-replacement-hash",candidate,ORIGINAL,originalHash,"0".repeat(64),"payload-hash",false);
  byte[] expected=candidate.get("ulike186/runtime/0000.bin"),sameLength=expected.clone();sameLength[0]^=1;
  installCase("absent-native-installed",candidate,null,false);installCase("identical-native-retained",candidate,expected,false);
  installCase("wrong-native-length",candidate,new byte[7],true);installCase("wrong-native-hash",candidate,sameLength,true);
  int productionAssertions=assertions;Map<String,Map<String,byte[]>> mutations=new TreeMap<>();for(String id:List.of("asset-read","original-native-read","native-resolver","manifest-parent","remove-required-input-guard"))mutations.put(id,mutate(candidate,id));
  negative("asset-read",()->assetCase("mutant-asset",mutations.get("asset-read"),ORIGINAL,originalHash,replacementHash,"native-hash",true));
  negative("original-native-read",()->assetCase("mutant-original-native",mutations.get("original-native-read"),ORIGINAL,originalHash,replacementHash,"native-hash",true));
  negative("native-resolver",()->installCase("mutant-resolver",mutations.get("native-resolver"),sameLength,true));
  negative("remove-required-input-guard",()->assetCase("mutant-guard",mutations.get("remove-required-input-guard"),"different material".getBytes(StandardCharsets.UTF_8),originalHash,replacementHash,"input-hash",false));
  Path mutants=work.resolve("structural-mutants");Files.createDirectories(mutants);
  for(String id:List.of("asset-read","original-native-read","native-resolver","manifest-parent","remove-required-input-guard")){
   Map<String,byte[]> mutated=mutations.get(id);String owner=id.equals("native-resolver")||id.equals("manifest-parent")?INSTALLER:LOADER;
   Files.write(mutants.resolve(id+".class"),mutated.get(owner.replace('.','/')+".class"));
  }
  assertions=productionAssertions;
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"behavior_mutations_rejected\":"+negatives+",\"real_patcher_version\":\"2.2.0-dev.2\",\"real_resource_context\":true,\"on_request_full_resource_mode\":true,\"private_apk_used\":false}");
 }
}
