import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;

/** Read-only identity/ABI/site audit of the newly supplied current APK.
 * No code insertion, class merge, clean-original reconstruction or APK rebuild.
 */
public final class UploadIdentityAudit {
  static final String PREFIX="Lapp/hiro/oneback/runtime/";
  static final String MAIN="Lcom/instagram/mainactivity/InstagramMainActivity;";
  static String key(MethodReference m){return ProvidedApkHookAudit.methodKey(m);}
  static Map<String,Object> row(Object...kv){Map<String,Object> r=new LinkedHashMap<>();for(int i=0;i<kv.length;i+=2)r.put(kv[i].toString(),kv[i+1]);return r;}
  static void write(Path p,Object value)throws Exception{Files.write(p,(ProvidedApkHookAudit.json(value)+"\n").getBytes(StandardCharsets.UTF_8));}
  static String classHash(ClassDef c)throws Exception{return c==null?null:ProvidedApkHookAudit.sha(HomeBackAudit.canonical(c));}
  static List<Integer> args(Instruction i){List<Integer> out=new ArrayList<>();if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;int[] a={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};for(int k=0;k<r.getRegisterCount();k++)out.add(a[k]);}else if(i instanceof RegisterRangeInstruction){RegisterRangeInstruction r=(RegisterRangeInstruction)i;for(int k=0;k<r.getRegisterCount();k++)out.add(r.getStartRegister()+k);}return out;}
  public static void main(String[] argv)throws Exception {
    if(argv.length!=4)throw new IllegalArgumentException("CURRENT_APK RELEASED_MPP OLD_V116_APK OUTPUT_DIRECTORY");
    Path input=Paths.get(argv[0]),out=Paths.get(argv[3]);Files.createDirectories(out);
    String initialHash=ProvidedApkHookAudit.sha(input);
    Map<String,ClassDef> released=PipelineProbe.zipDex(argv[1],"extensions/oneback_exit.mpe");
    Map<String,ClassDef> old=ProvidedApkHookAudit.runtime(Paths.get(argv[2])),runtime=new TreeMap<>();
    Map<String,String> classEntries=new HashMap<>();Map<String,ClassDef> principal=new TreeMap<>();
    List<Object> duplicates=new ArrayList<>(),calls=new ArrayList<>(),nativeHooks=new ArrayList<>(),registrationSites=new ArrayList<>(),directRegistrations=new ArrayList<>(),primaryBootstrap=new ArrayList<>();
    Map<String,Integer> hookCounts=new TreeMap<>(),helperNames=new TreeMap<>();
    Map<String,Method> targetMethods=new TreeMap<>();Map<String,Field> targetFields=new TreeMap<>();
    int totalClasses=0,nonRuntimeMethods=0,helperMethods=0;
    MultiDexContainer<? extends DexFile> container=DexFileFactory.loadDexContainer(input.toFile(),Opcodes.getDefault());
    List<String> entries=new ArrayList<>(container.getDexEntryNames());Collections.sort(entries);
    for(String dex:entries)for(ClassDef c:container.getEntry(dex).getDexFile().getClasses()) {
      totalClasses++;String previous=classEntries.putIfAbsent(c.getType(),dex);if(previous!=null)duplicates.add(row("class",c.getType(),"entries",Arrays.asList(previous,dex)));
      if(c.getType().startsWith(PREFIX)){runtime.put(c.getType(),c);for(Method m:c.getMethods())targetMethods.put(key(m),m);for(Field f:c.getFields())targetFields.put(ProvidedApkHookAudit.fieldKey(f),f);continue;}
      if(c.getType().equals(MAIN)||c.getType().startsWith("Landroidx/")&&c.getType().endsWith("ComponentActivity;")||c.getType().equals("Landroidx/fragment/app/FragmentActivity;")||c.getType().equals("Lcom/instagram/base/activity/IgFragmentActivity;"))principal.put(c.getType(),c);
      for(Method m:c.getMethods()) {
        nonRuntimeMethods++;if(m.getName().startsWith("hiroOneBack$")){helperMethods++;String k=m.getName().startsWith("hiroOneBack$terminal$")?"terminal":m.getName();helperNames.put(k,helperNames.getOrDefault(k,0)+1);}
        if(m.getImplementation()==null)continue;int index=0,pc=0;
        for(Instruction i:m.getImplementation().getInstructions()) {
          Reference ref=i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference():null;
          String descriptor=null;boolean field=false;
          if(ref instanceof MethodReference&&((MethodReference)ref).getDefiningClass().startsWith(PREFIX))descriptor=key((MethodReference)ref);
          if(ref instanceof FieldReference&&((FieldReference)ref).getDefiningClass().startsWith(PREFIX)){descriptor=ProvidedApkHookAudit.fieldKey((FieldReference)ref);field=true;}
          if(descriptor!=null) {
            Map<String,Object> site=row("dex",dex,"caller",key(m),"instruction_index",index,"code_unit",pc,"opcode",i.getOpcode().name(),"registers",args(i),"target",descriptor,"field",field);
            calls.add(site);hookCounts.put(descriptor,hookCounts.getOrDefault(descriptor,0)+1);
            if(ref instanceof MethodReference){String name=((MethodReference)ref).getName();
              if(name.startsWith("tryInstagram"))nativeHooks.add(site);
              if(name.equals("register")||name.equals("unregister"))registrationSites.add(site);
              if(principal.containsKey(c.getType())&&Arrays.asList("install","onResume","onDestroy","onNewIntent","enterBack","enterKey","tryBack","key").contains(name))primaryBootstrap.add(site);
            }
          }
          if(ref instanceof MethodReference) {
            MethodReference mr=(MethodReference)ref;
            if(mr.getName().equals("registerOnBackInvokedCallback")||mr.getName().equals("unregisterOnBackInvokedCallback"))directRegistrations.add(row("dex",dex,"caller",key(m),"instruction_index",index,"target",key(mr),"opcode",i.getOpcode().name()));
          }
          index++;pc+=i.getCodeUnits();
        }
      }
    }
    List<Object> identity=new ArrayList<>();List<String> releaseMissing=new ArrayList<>(),releaseMismatch=new ArrayList<>(),extra=new ArrayList<>(),oldOnly=new ArrayList<>();
    int releaseExact=0,oldExact=0;
    Set<String> types=new TreeSet<>();types.addAll(runtime.keySet());types.addAll(released.keySet());
    for(String type:types){String current=classHash(runtime.get(type)),latest=classHash(released.get(type)),oldHash=classHash(old.get(type));boolean eq=Objects.equals(current,latest)&&current!=null,eqOld=Objects.equals(current,oldHash)&&current!=null;
      if(eq)releaseExact++;else if(current==null)releaseMissing.add(type);else if(latest==null)extra.add(type);else releaseMismatch.add(type);
      if(eqOld)oldExact++;if(eqOld&&!eq)oldOnly.add(type);
      identity.add(row("class",type,"dex",classEntries.get(type),"current_sha256",current,"released_v1110_sha256",latest,"old_v116_sha256",oldHash,"equals_released",eq,"equals_old",eqOld));
    }
    List<String> abiErrors=new ArrayList<>();
    for(Object raw:calls){Map<?,?> site=(Map<?,?>)raw;String descriptor=(String)site.get("target"),owner=descriptor.substring(0,descriptor.indexOf("->"));ClassDef ownerClass=runtime.get(owner);
      if(ownerClass==null||(ownerClass.getAccessFlags()&1)==0){abiErrors.add("Missing/non-public owner: "+descriptor);continue;}
      if(Boolean.TRUE.equals(site.get("field"))){Field f=targetFields.get(descriptor);if(f==null||(f.getAccessFlags()&1)==0)abiErrors.add("Missing/non-public field: "+descriptor);}
      else {Method m=targetMethods.get(descriptor);if(m==null||(m.getAccessFlags()&1)==0){abiErrors.add("Missing/non-public method: "+descriptor);continue;}String opcode=(String)site.get("opcode");if(opcode.startsWith("INVOKE_")&&opcode.startsWith("INVOKE_STATIC")!=((m.getAccessFlags()&8)!=0))abiErrors.add("Static/instance mismatch: "+descriptor);}
    }
    Map<String,Object> manifest;
    try(ZipFile z=new ZipFile(input.toFile());InputStream in=z.getInputStream(z.getEntry("AndroidManifest.xml"))){byte[] data=in.readAllBytes();AndroidManifestBlock m=AndroidManifestBlock.load(new ByteArrayInputStream(data));manifest=row("package",m.getPackageName(),"version",m.getVersionName(),"version_code",m.getVersionCode(),"target_sdk",m.getTargetSdkVersion(),"sha256",ProvidedApkHookAudit.sha(data));}
    Map<String,Object> summary=row("schema","current-upload-oneback-identity-audit-v1","input",input.toString(),"sha256",initialHash,"input_bytes",Files.size(input),"manifest",manifest,"dex_entries",entries,"class_count",totalClasses,"non_runtime_method_count",nonRuntimeMethods,"duplicate_class_definitions",duplicates,"runtime_classes",runtime.size(),"release_runtime_classes",released.size(),"runtime_classes_exactly_equal_release",releaseExact,"release_missing_classes",releaseMissing,"release_mismatched_classes",releaseMismatch,"extra_runtime_classes",extra,"old_runtime_classes",old.size(),"runtime_classes_equal_old",oldExact,"old_only_classes_not_equal_release",oldOnly,"external_runtime_callsites",calls.size(),"external_runtime_targets",hookCounts.size(),"hook_counts",hookCounts,"wrapper_helper_methods",helperMethods,"wrapper_helper_kinds",helperNames,"native_hook_sites",nativeHooks,"decorated_registration_sites",registrationSites,"undecorated_public_registration_sites",directRegistrations,"primary_bootstrap_and_event_sites",primaryBootstrap,"actual_runtime_abi_errors",abiErrors,"input_modified",false,"android_execution_tested",false,"scope","Static read-only inspection of newly supplied current APK, including entire runtime class implementations and actual hook call locations. No APK reconstruction or patch application.");
    if(!initialHash.equals(ProvidedApkHookAudit.sha(input)))throw new AssertionError("Input changed during audit");
    write(out.resolve("identity-and-hooks.json"),summary);write(out.resolve("runtime-class-hashes.json"),identity);write(out.resolve("all-external-runtime-callsites.json"),calls);
    System.out.println("runtime="+runtime.size()+"; releaseExact="+releaseExact+"; releaseMissing="+releaseMissing+"; releaseMismatch="+releaseMismatch+"; extra="+extra+"; oldOnly="+oldOnly);
    System.out.println("classes="+totalClasses+"; duplicates="+duplicates.size()+"; externalHooks="+calls.size()+"; targets="+hookCounts.size()+"; ABI errors="+abiErrors.size()+"; nativeHooks="+nativeHooks.size());
    System.out.println("decorated platform registrations="+registrationSites.size()+"; undecorated public registrations="+directRegistrations.size());
    System.out.println("PASS read-only current APK identity audit; input hash unchanged="+initialHash);
  }
}
