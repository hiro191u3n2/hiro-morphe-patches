import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import app.hiro.oneback.patches.InstagramUpgrade;
import app.hiro.oneback.patches.OneBackPatch;

/** Real supplied X classes, historically patched by the pinned, released 123
 * implementation in an isolated class loader. This is an upgrade model, not a
 * claim that the supplied APK already contains OneBack or was device-tested. */
public final class TwitterUpgradeAudit {
    static final String PREFIX="Lapp/hiro/oneback/runtime/";
    static final String ROOT="Lcom/twitter/app/main/l1;";
    static final String ACTIVITY="Lcom/twitter/app/common/inject/p;";
    static final String PRIOR_MPP_SHA="47a4da428d7cd18fb4e58833043b92a07e17973e45539c367929fb2e7e8e3a96";
    static int assertions, rejections;
    static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}

    static String sha(byte[] bytes)throws Exception {
        StringBuilder hex=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))hex.append(String.format(Locale.ROOT,"%02x",b&255));
        return hex.toString();
    }
    static Map<String,ClassDef> read(String path)throws Exception {
        Map<String,ClassDef> result=new TreeMap<String,ClassDef>();
        MultiDexContainer<? extends DexFile> container=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
        for(String entry:container.getDexEntryNames())for(ClassDef c:container.getEntry(entry).getDexFile().getClasses())
            check(result.put(c.getType(),c)==null,"duplicate class "+c.getType());
        return result;
    }
    static Map<String,ClassDef> runtime(InputStream raw)throws Exception {
        check(raw!=null,"missing runtime resource");
        Map<String,ClassDef> result=new TreeMap<String,ClassDef>();
        try(InputStream in=new BufferedInputStream(raw)) {
            for(ClassDef c:DexBackedDexFile.fromInputStream(Opcodes.getDefault(),in).getClasses()) {
                check(c.getType().startsWith(PREFIX),"non-runtime type in runtime resource");
                check(result.put(c.getType(),c)==null,"duplicate runtime type");
            }
        }
        check(result.containsKey(OneBackPatch.RUNTIME),"incomplete runtime resource");return result;
    }
    static byte[] canonical(ClassDef c)throws Exception {
        MemoryDataStore data=new MemoryDataStore();
        try{DexPool.writeTo(data,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(c)));return Arrays.copyOf(data.getBuffer(),data.getSize());}
        finally{data.close();}
    }
    static byte[] canonical(Method m)throws Exception {
        return canonical(new ImmutableClassDef(m.getDefiningClass(),1,"Ljava/lang/Object;",Collections.<String>emptyList(),null,
            Collections.<Annotation>emptySet(),Collections.<Field>emptyList(),Collections.singleton(m)));
    }
    static void sameClasses(Map<String,ClassDef> a,Map<String,ClassDef> b,String why)throws Exception {
        check(a.keySet().equals(b.keySet()),why+" class inventory");
        for(String type:a.keySet())check(Arrays.equals(canonical(a.get(type)),canonical(b.get(type))),why+" "+type);
    }
    static int helpers(Map<String,ClassDef> classes) {
        int count=0;for(ClassDef c:classes.values())if(!c.getType().startsWith(PREFIX))
            for(Method m:c.getMethods())if(m.getName().startsWith(OneBackPatch.HELPER_PREFIX))count++;
        return count;
    }
    static Method method(ClassDef c,String name) {
        Method found=null;for(Method m:c.getMethods())if(m.getName().equals(name)&&m.getParameterTypes().isEmpty()) {
            check(found==null,"ambiguous no-argument method "+name);found=m;
        }
        check(found!=null,"missing method "+name);return found;
    }

    /** Only the historical patch package is isolated; dexlib and mutable class
     * interfaces retain their parent identity so real objects cross the boundary. */
    static final class PriorLoader extends URLClassLoader {
        PriorLoader(File archive)throws Exception{super(new URL[]{archive.toURI().toURL()},TwitterUpgradeAudit.class.getClassLoader());}
        protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
            synchronized(getClassLoadingLock(name)) {
                Class<?> c=findLoadedClass(name);
                if(c==null)c=name.startsWith("app.hiro.oneback.patches.")?findClass(name):super.loadClass(name,false);
                if(resolve)resolveClass(c);return c;
            }
        }
    }
    @SuppressWarnings("unchecked")
    static Map<String,ClassDef> historical(Map<String,ClassDef> input,Map<String,ClassDef> prior,File mpp)throws Exception {
        check(PRIOR_MPP_SHA.equals(sha(Files.readAllBytes(mpp.toPath()))),"historical MPP is not pinned released 1.1.13");
        try(ZipFile archive=new ZipFile(mpp)) {
            Manifest manifest=new Manifest(archive.getInputStream(archive.getEntry("META-INF/MANIFEST.MF")));
            check("1.1.13".equals(manifest.getMainAttributes().getValue("Version")),"historical patch version");
            sameClasses(prior,runtime(archive.getInputStream(archive.getEntry("extensions/oneback_exit.mpe"))),"pinned 123 runtime and historical MPP runtime");
        }
        final Map<String,ClassDef> snapshot=new TreeMap<String,ClassDef>(input);snapshot.putAll(prior);
        final Map<String,MutableClass> edits=new TreeMap<String,MutableClass>();
        try(PriorLoader loader=new PriorLoader(mpp)) {
            Class<?> patch=loader.loadClass("app.hiro.oneback.patches.OneBackPatch");
            Class<?> editor=loader.loadClass("app.hiro.oneback.patches.OneBackPatch$Editor");
            check(patch!=OneBackPatch.class&&patch.getClassLoader()==loader,"historical production implementation isolated");
            Object proxy=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{editor},(self,call,args)->{
                if(!"mutable".equals(call.getName()))throw new AssertionError("unexpected editor invocation "+call);
                String type=(String)args[0];check(!type.startsWith(PREFIX),"historical generic apply edited runtime");
                MutableClass value=edits.get(type);
                if(value==null){value=new MutableClass(snapshot.get(type));edits.put(type,value);}return value;
            });
            Map<String,Integer> counts;
            try{counts=(Map<String,Integer>)patch.getMethod("applyAll",Map.class,editor).invoke(null,snapshot,proxy);}
            catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError("historical applyAll failed",e.getCause());}
            check(!counts.containsKey("twitter_native_root_back"),"historical implementation unexpectedly inserted current native gate");
            System.out.println("Historical released123 applyAll counts="+counts+", nativeEditedClasses="+edits.size());
        }
        Map<String,ClassDef> result=new TreeMap<String,ClassDef>(snapshot);
        // Freeze model classes before an independent upgrade creates mutable copies.
        for(ClassDef value:edits.values())result.put(value.getType(),ImmutableClassDef.of(value));
        check(helpers(result)>0,"historical model has no real generic hooks");
        check(Arrays.equals(canonical(method(input.get(ROOT),"goBack")),canonical(method(result.get(ROOT),"goBack"))),"old generic apply changed authoritative X goBack body");
        check(method(result.get(ACTIVITY),"hiroOneBack$onBackPressed")!=null,"old Activity native helper missing");
        return result;
    }
    static final class Edit implements OneBackPatch.Editor {
        final Map<String,ClassDef> input;final Map<String,MutableClass> changed=new TreeMap<String,MutableClass>();
        Edit(Map<String,ClassDef> input){this.input=input;}
        public MutableClass mutable(String type) {
            check(type.startsWith(PREFIX),"runtime upgrade touched native class "+type);
            MutableClass c=changed.get(type);
            if(c==null){check(input.containsKey(type),"upgrade edited absent class");c=new MutableClass(input.get(type));changed.put(type,c);
                c.getDirectMethods();c.getVirtualMethods();c.getStaticFields();c.getInstanceFields();}
            return c;
        }
    }
    static void reject(Map<String,ClassDef> input,String name)throws Exception {
        Edit edit=new Edit(input);boolean rejected=false;
        try{InstagramUpgrade.applyVersionIfKnown("com.twitter.android",input,edit);}catch(IllegalStateException expected){rejected=true;}
        check(rejected,name+" accepted");check(edit.changed.isEmpty(),name+" mutated before validation");
        rejections++;System.out.println("Rejected before mutation: "+name);
    }
    static Map<String,ClassDef> replacingRuntime(Map<String,ClassDef> input,Map<String,ClassDef> replacement) {
        Map<String,ClassDef> result=new TreeMap<String,ClassDef>(input);
        for(String type:new ArrayList<String>(result.keySet()))if(type.startsWith(PREFIX))result.remove(type);
        result.putAll(replacement);return result;
    }
    static Map<String,ClassDef> merged(Map<String,ClassDef> input,Map<String,ClassDef> replacement) {
        Map<String,ClassDef> result=new TreeMap<String,ClassDef>(input);
        for(ClassDef c:replacement.values())if(!result.containsKey(c.getType()))result.put(c.getType(),c);return result;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("X-APK current-runtime.dex released123.mpp; current MPP resources supplied by classpath");
        Map<String,ClassDef> input=read(args[0]),replacement=read(args[1]);
        for(String type:input.keySet())check(!type.startsWith(PREFIX),"supplied X APK unexpectedly contains OneBack runtime");
        check(helpers(input)==0,"supplied X APK already has generic OneBack hooks");
        Map<String,ClassDef> distributed=runtime(TwitterUpgradeAudit.class.getResourceAsStream("/extensions/oneback_exit.mpe"));
        sameClasses(distributed,replacement,"requested current runtime and distributed MPP runtime");
        Map<String,ClassDef> prior=runtime(TwitterUpgradeAudit.class.getResourceAsStream("/upgrades/oneback-runtime123.dex"));
        Map<String,ClassDef> historical=historical(input,prior,new File(args[2]));
        Map<String,ClassDef> candidate=merged(historical,replacement);
        int helperCount=helpers(candidate);
        Edit edit=new Edit(candidate);
        String previous=InstagramUpgrade.applyVersionIfKnown("com.twitter.android",candidate,edit);
        check("1.1.13".equals(previous),"exact historical X runtime upgrade not selected");
        Map<String,ClassDef> changed=new TreeMap<String,ClassDef>();changed.putAll(edit.changed);
        sameClasses(replacement,changed,"all replacement classes after pre-materialized mutable caches");
        Map<String,ClassDef> upgraded=new TreeMap<String,ClassDef>(candidate);upgraded.putAll(edit.changed);
        check(helpers(upgraded)==helperCount,"runtime upgrade duplicated generic hooks");
        for(String type:candidate.keySet())if(!type.startsWith(PREFIX))check(upgraded.get(type)==candidate.get(type),"native class changed during runtime upgrade");

        // Match the admitted-X production branch: replace runtime, then insert
        // only the newly reviewed native gate. Never run generic applyAll twice.
        final Map<String,MutableClass> nativeEdits=new TreeMap<String,MutableClass>();
        Map<String,Integer> nativeCounts=new TreeMap<String,Integer>();
        OneBackPatch.applyTwitterRootBack(upgraded,type->{
            check(ROOT.equals(type),"known-X native upgrade touched unrelated class "+type);
            MutableClass value=nativeEdits.get(type);
            if(value==null){value=new MutableClass(upgraded.get(type));nativeEdits.put(type,value);}return value;
        },nativeCounts);
        check(nativeEdits.keySet().equals(Collections.singleton(ROOT)),"known-X upgrade must edit exactly native root class");
        check(nativeCounts.size()==1&&Integer.valueOf(1).equals(nativeCounts.get("twitter_native_root_back")),"known-X upgrade must insert exactly one root gate");
        Method nativeBefore=method(candidate.get(ROOT),"goBack"),nativeAfter=method(nativeEdits.get(ROOT),"goBack");
        TwitterRootAudit.validate(nativeAfter);
        Method restored=TwitterRootAudit.restore(nativeAfter);
        check(OneBackPatch.instagramNavigationDigest(nativeBefore).equals(OneBackPatch.instagramNavigationDigest(restored)),"new root gate removal must recover exact prior goBack executable body");
        List<Method> restoredMethods=new ArrayList<Method>();
        for(Method m:nativeEdits.get(ROOT).getMethods())restoredMethods.add(m.getName().equals("goBack")&&m.getParameterTypes().isEmpty()?restored:m);
        ClassDef nativeClass=nativeEdits.get(ROOT);
        ClassDef restoredClass=new ImmutableClassDef(nativeClass.getType(),nativeClass.getAccessFlags(),nativeClass.getSuperclass(),nativeClass.getInterfaces(),nativeClass.getSourceFile(),nativeClass.getAnnotations(),nativeClass.getFields(),restoredMethods);
        // Raw encoding/debug fields may be re-linked by a mutable body; audit
        // metadata and complete executable semantics using the shared normalizer.
        RealUpgradeAudit.classMeta(candidate.get(ROOT),restoredClass);
        Map<String,Method> beforeMethods=RealUpgradeAudit.methods(candidate.get(ROOT)),afterMethods=RealUpgradeAudit.methods(restoredClass);
        check(beforeMethods.keySet().equals(afterMethods.keySet()),"native root method inventory preserved");
        for(String id:beforeMethods.keySet())RealUpgradeAudit.compare(id,beforeMethods.get(id),afterMethods.get(id));
        Map<String,ClassDef> fullyUpgraded=new TreeMap<String,ClassDef>(upgraded);fullyUpgraded.putAll(nativeEdits);
        check(helpers(fullyUpgraded)==helperCount,"native upgrade duplicated existing generic hooks");
        for(String type:upgraded.keySet())if(!ROOT.equals(type))check(fullyUpgraded.get(type)==upgraded.get(type),"native gate upgrade changed unrelated class");

        Map<String,ClassDef> fresh=merged(input,replacement);Edit freshEdit=new Edit(fresh);
        check(InstagramUpgrade.applyVersionIfKnown("com.twitter.android",fresh,freshEdit)==null&&freshEdit.changed.isEmpty(),"actual fresh X input must fall through to normal applyAll");
        Edit other=new Edit(candidate);
        check(InstagramUpgrade.applyVersionIfKnown("example.unsupported",candidate,other)==null&&other.changed.isEmpty(),"unrelated package must not upgrade");

        Map<String,ClassDef> modified=new TreeMap<String,ClassDef>(candidate);
        MutableClass badRuntime=new MutableClass(modified.get(OneBackPatch.RUNTIME));badRuntime.setSourceFile("altered.java");modified.put(badRuntime.getType(),badRuntime);
        reject(modified,"modified previous runtime");
        Map<String,ClassDef> missing=new TreeMap<String,ClassDef>(candidate);missing.remove(OneBackPatch.RUNTIME);reject(missing,"missing previous runtime class");
        Map<String,ClassDef> extra=new TreeMap<String,ClassDef>(candidate);
        ClassDef unknown=new ImmutableClassDef(PREFIX+"Unreviewed;",1,"Ljava/lang/Object;",Collections.<String>emptyList(),null,Collections.<Annotation>emptySet(),Collections.<Field>emptyList(),Collections.<Method>emptyList());
        extra.put(unknown.getType(),unknown);reject(extra,"unknown extra runtime class");
        Map<String,ClassDef> wrongRoot=new TreeMap<String,ClassDef>(candidate);MutableClass root=new MutableClass(wrongRoot.get(ROOT));
        for(MutableMethod m:root.getMethods())if(m.getName().equals("goBack")&&m.getParameterTypes().isEmpty())m.getImplementation().removeInstruction(0);
        wrongRoot.put(root.getType(),root);reject(wrongRoot,"changed authoritative native goBack body");
        Map<String,ClassDef> noRoot=new TreeMap<String,ClassDef>(candidate);noRoot.remove(ROOT);reject(noRoot,"missing authoritative native root class");
        Map<String,ClassDef> noScope=new TreeMap<String,ClassDef>(candidate);MutableClass scopeActivity=new MutableClass(noScope.get(ACTIVITY));
        for(MutableMethod m:scopeActivity.getMethods())if(m.getName().equals("onBackPressed")&&m.getParameterTypes().isEmpty())m.getImplementation().removeInstruction(4);
        noScope.put(scopeActivity.getType(),scopeActivity);reject(noScope,"missing enterBack in prior Activity Back wrapper");
        Map<String,ClassDef> wrongAbi=new TreeMap<String,ClassDef>(candidate);MutableClass activity=new MutableClass(wrongAbi.get(ACTIVITY));
        for(MutableMethod m:activity.getMethods())if(m.getName().equals("hiroOneBack$onBackPressed"))
            m.getImplementation().addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,new ImmutableMethodReference(OneBackPatch.RUNTIME,"unreviewedNativeHook",Collections.<String>emptyList(),"V")));
        wrongAbi.put(activity.getType(),activity);reject(wrongAbi,"native reference absent from admitted runtime ABI");
        reject(replacingRuntime(historical,replacement),"current124 runtime reapply");
        Map<String,ClassDef> old122=runtime(TwitterUpgradeAudit.class.getResourceAsStream("/upgrades/oneback-runtime122.dex"));
        reject(merged(replacingRuntime(historical,old122),replacement),"X runtime 122 outside exact123 upgrade allowlist");

        System.out.println("PASS TwitterUpgradeAudit: actual supplied X APK has zero OneBack classes/helpers; historical model generated by pinned released 1.1.13 applyAll; exact 123 -> current upgrade replaced "+edit.changed.size()+" canonical runtime classes, then inserted exactly one independently validated native root gate; native helpers preserved="+helperCount+"; fresh/other-package no-op; negative pre-mutation rejections="+rejections+"; assertions="+assertions);
        System.out.println("Historical MPP SHA256="+PRIOR_MPP_SHA+"; runtime-only upgrade did not mutate native classes, native insertion preserved all existing generic hooks, and removing that gate recovered original native class semantics. Whole emitted APK audited separately. Android device execution not performed.");
    }
}
