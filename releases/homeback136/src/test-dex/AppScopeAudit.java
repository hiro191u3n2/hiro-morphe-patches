import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.*;
import app.hiro.oneback.patches.Instagram452Boundaries;
import app.hiro.oneback.patches.OneBackPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Regression for unrelated applications reusing Instagram's obfuscated names.
 * The old failure is executed from the pinned public 1.0.277 archive. Fixtures
 * are authored synthetic APKs, not a representation of a real TikTok APK. */
public final class AppScopeAudit {
    static final List<String> COLLISIONS=Collections.unmodifiableList(Arrays.asList("LX/02Hr;","LX/001p;","LX/004K;"));
    static final String INSTAGRAM_MAIN="Lcom/instagram/mainactivity/InstagramMainActivity;";
    static final String TWITTER_MAIN="Lcom/twitter/app/main/MainActivity;";
    static final List<String> PACKAGES=Arrays.asList("com.zhiliaoapp.musically","com.ss.android.ugc.trill","com.twitter.android","ctrip.english");
    static final String OLD_SHA="797e102352dfb46fc38f0ffb4a8d65066a843079d5089be8f390306ac3c4281a";
    static final String OLD_ERROR="Instagram 452 Reels Back method missing";
    static int checks;

    static void check(boolean pass,String reason) {checks++;if(!pass)throw new AssertionError(reason);}
    static ClassDef object(String type,List<Method> methods) {
        return new ImmutableClassDef(type,1,"Ljava/lang/Object;",Collections.emptyList(),null,
            Collections.emptySet(),Collections.emptyList(),methods);
    }
    static ClassDef object(String type) {return object(type,Collections.emptyList());}
    static Map<String,ClassDef> collisionFixture(boolean instagramMarker)throws Exception {
        // Collisions come first so the malformed Instagram case proves the
        // strict method check still runs before any mutable proxy is requested.
        Map<String,ClassDef> input=new LinkedHashMap<>();
        for(String type:COLLISIONS)input.put(type,object(type));
        if(instagramMarker)input.put(INSTAGRAM_MAIN,object(INSTAGRAM_MAIN));
        input.putAll(HomeBackAudit.fixtures());
        return input;
    }
    static String sha(byte[] bytes)throws Exception {
        StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();
    }
    static final class PriorLoader extends URLClassLoader {
        PriorLoader(File mpp)throws Exception {super(new URL[]{mpp.toURI().toURL()},AppScopeAudit.class.getClassLoader());}
        @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
            synchronized(getClassLoadingLock(name)) {
                Class<?> c=findLoadedClass(name);
                if(c==null)c=name.startsWith("app.hiro.oneback.patches.")?findClass(name):super.loadClass(name,false);
                if(resolve)resolveClass(c);return c;
            }
        }
    }
    static void reproduce(File mpp)throws Exception {
        check(OLD_SHA.equals(sha(Files.readAllBytes(mpp.toPath()))),"old MPP must be the pinned published 1.0.277");
        try(ZipFile zip=new ZipFile(mpp)) {
            Manifest manifest=new Manifest(zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF")));
            check("1.0.277".equals(manifest.getMainAttributes().getValue("Version")),"published old bundle version");
        }
        Map<String,ClassDef> input=collisionFixture(false);int[] edits={0};
        try(PriorLoader loader=new PriorLoader(mpp)) {
            Class<?> patch=loader.loadClass("app.hiro.oneback.patches.OneBackPatch");
            Class<?> editor=loader.loadClass("app.hiro.oneback.patches.OneBackPatch$Editor");
            check(patch!=OneBackPatch.class&&patch.getClassLoader()==loader,"old implementation is independently loaded from released MPP");
            Object proxy=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{editor},(self,call,args)->{
                if(!"mutable".equals(call.getName()))throw new AssertionError("unexpected old editor call "+call);
                edits[0]++;return new MutableClass(input.get((String)args[0]));
            });
            Throwable failure=null;
            try {patch.getMethod("applyAll",Map.class,editor).invoke(null,input,proxy);}
            catch(java.lang.reflect.InvocationTargetException expected) {failure=expected.getCause();}
            check(failure instanceof IllegalStateException&&OLD_ERROR.equals(failure.getMessage()),"old production code must reproduce the exact user error: "+failure);
            check(edits[0]==0,"old collision reproduces before any mutation");
            List<String> frames=new ArrayList<>();for(StackTraceElement f:failure.getStackTrace())frames.add(f.getClassName()+"."+f.getMethodName());
            check(frames.contains("app.hiro.oneback.patches.Instagram452Boundaries.find")
                    &&frames.contains("app.hiro.oneback.patches.OneBackPatch.instagramReelsBoundary")
                    &&frames.contains("app.hiro.oneback.patches.OneBackPatch.applyAll"),"old stack follows the reported failing route");
        }
        System.out.println("REPRODUCED published 1.0.277: "+OLD_ERROR);
    }
    static Map<String,ClassDef> apply(String pkg,Map<String,ClassDef> input,Map<String,Integer> counts,Set<String> touched) {
        Map<String,MutableClass> changes=new LinkedHashMap<>();
        counts.putAll(OneBackPatch.applyAll(pkg,input,type->{
            touched.add(type);return changes.computeIfAbsent(type,t->new MutableClass(input.get(t)));
        }));
        Map<String,ClassDef> output=new LinkedHashMap<>(input);
        for(ClassDef c:changes.values())output.put(c.getType(),ImmutableClassDef.of(c));
        return output;
    }
    static void successfulNonInstagram(String pkg,boolean marker)throws Exception {
        Map<String,ClassDef> input=collisionFixture(marker);Map<String,Integer> counts=new LinkedHashMap<>();Set<String> touched=new HashSet<>();
        Map<String,ClassDef> output=apply(pkg,input,counts,touched);
        check(counts.containsKey("activity_classes")
                &&(counts.containsKey("activity_lifecycle_entry")||counts.containsKey("activity_lifecycle_added")),"generic lifecycle hooks retained: "+pkg);
        for(String key:counts.keySet())check(!key.startsWith("instagram_"),"non-Instagram must not install Instagram hook: "+pkg+" "+key);
        for(String type:COLLISIONS) {
            check(!touched.contains(type),"unrelated collision never requested mutable proxy: "+pkg+" "+type);
            check(Arrays.equals(HomeBackAudit.canonical(input.get(type)),HomeBackAudit.canonical(output.get(type))),"colliding class unchanged: "+pkg+" "+type);
        }
        if(marker)check(!touched.contains(INSTAGRAM_MAIN),"package metadata overrides inert Instagram marker: "+pkg);
        HomeBackAudit.audit(input,output);
    }
    static void malformedInstagram(boolean changedBody)throws Exception {
        Map<String,ClassDef> input=collisionFixture(true);
        if(changedBody) {
            Method method=new ImmutableMethod(COLLISIONS.get(0),"onBackPressed",Collections.emptyList(),"Z",17,
                Collections.emptySet(),Collections.emptySet(),new ImmutableMethodImplementation(11,
                    Arrays.asList(new ImmutableInstruction11n(Opcode.CONST_4,0,0),new ImmutableInstruction11x(Opcode.RETURN,0)),
                    Collections.emptyList(),Collections.emptyList()));
            input.put(COLLISIONS.get(0),object(COLLISIONS.get(0),Collections.singletonList(method)));
        }
        int[] edits={0};String message=null;
        try {OneBackPatch.applyAll("com.instagram.android",input,type->{edits[0]++;return new MutableClass(input.get(type));});}
        catch(IllegalStateException expected) {message=expected.getMessage();}
        check((changedBody?"Instagram 452 Reels Back contract changed":OLD_ERROR).equals(message),"actual Instagram malformed boundary remains rejected: "+message);
        check(edits[0]==0,"malformed Instagram collision fails before mutation");
    }
    static void wrongPackagePreflight()throws Exception {
        Map<String,ClassDef> input=collisionFixture(true);int[] edits={0};boolean failed=false;
        try {OneBackPatch.applyAll("org.example.unsupported",input,type->{edits[0]++;return new MutableClass(input.get(type));});}
        catch(IllegalStateException expected) {failed=expected.getMessage().contains("OneBack is restricted");}
        check(failed&&edits[0]==0,"unsupported package rejected before scanner and mutation");
    }
    static void twitterMarkerCannotSelectAnotherApp()throws Exception {
        // A stable class marker cannot override selected package metadata either.
        for(String pkg:Arrays.asList("com.zhiliaoapp.musically","com.ss.android.ugc.trill","ctrip.english","com.instagram.android")) {
            Map<String,ClassDef> input=new LinkedHashMap<>(HomeBackAudit.fixtures());input.put(TWITTER_MAIN,object(TWITTER_MAIN));
            Map<String,Integer> counts=new LinkedHashMap<>();Set<String> touched=new HashSet<>();Map<String,ClassDef> output=apply(pkg,input,counts,touched);
            for(String key:counts.keySet())check(!key.startsWith("twitter_"),"non-X package must not install X-specific hooks: "+pkg);
            check(!touched.contains(TWITTER_MAIN)&&Arrays.equals(HomeBackAudit.canonical(input.get(TWITTER_MAIN)),HomeBackAudit.canonical(output.get(TWITTER_MAIN))),"inert X marker unchanged: "+pkg);
        }
    }
    static void selftest(File oldMpp,Path report)throws Exception {
        reproduce(oldMpp);
        check(!Instagram452Boundaries.supports(collisionFixture(false)),"obfuscated names alone no longer identify Instagram452");
        check(Instagram452Boundaries.supports(collisionFixture(true)),"stable Instagram marker retains strict452 dependency selection");
        for(String pkg:PACKAGES)for(boolean marker:new boolean[]{false,true})successfulNonInstagram(pkg,marker);
        malformedInstagram(false);malformedInstagram(true);wrongPackagePreflight();twitterMarkerCannotSelectAnotherApp();
        String summary="PASS AppScopeAudit: old277 exact error reproduced; four non-Instagram packages preserve collision classes with/without Instagram marker; package-scoped Instagram/X routing; missing and changed Instagram452 boundaries rejected; assertions="+checks;
        String json="{\n  \"result\":\"PASS\",\n  \"summary\":\""+summary+"\",\n  \"old_repro\":{\"result\":\"REPRODUCED\",\"error\":\""+OLD_ERROR+"\"},\n"
            +"  \"packages\":[\"com.zhiliaoapp.musically\",\"com.ss.android.ugc.trill\",\"com.twitter.android\",\"ctrip.english\"],\n"
            +"  \"instagram_malformed_rejected\":true,\n  \"colliding_classes_unchanged\":true\n}\n";
        Files.write(report,json.getBytes(StandardCharsets.UTF_8));System.out.println(summary);
    }
    static byte[] dex(Map<String,ClassDef> classes)throws Exception {
        MemoryDataStore out=new MemoryDataStore();try {DexPool.writeTo(out,new ImmutableDexFile(Opcodes.getDefault(),classes.values()));return Arrays.copyOf(out.getBuffer(),out.getSize());}finally{out.close();}
    }
    static byte[] read(InputStream in)throws Exception {
        try(InputStream source=in;ByteArrayOutputStream out=new ByteArrayOutputStream()) {byte[] buffer=new byte[16384];int n;while((n=source.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();}
    }
    static void inject(Path apk,byte[] code)throws Exception {
        SortedMap<String,byte[]> entries=new TreeMap<>();
        try(ZipFile zip=new ZipFile(apk.toFile())) {Enumeration<? extends ZipEntry> all=zip.entries();while(all.hasMoreElements()){ZipEntry entry=all.nextElement();if(!entry.isDirectory())entries.put(entry.getName(),read(zip.getInputStream(entry)));}}
        check(entries.containsKey("classes.dex"),"fixture has primary DEX");entries.put("classes.dex",code);
        try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(apk))) {for(Map.Entry<String,byte[]> item:entries.entrySet()){ZipEntry entry=new ZipEntry(item.getKey());entry.setTime(0);zip.putNextEntry(entry);zip.write(item.getValue());zip.closeEntry();}}
    }
    static void makeFixtures(String originalDex,String dir)throws Exception {
        Fixtures.main(new String[]{"create",originalDex,dir});
        Map<String,ClassDef> input=new LinkedHashMap<>(HomeBackAudit.read(originalDex));
        for(String type:COLLISIONS)check(input.put(type,object(type))==null,"fresh collision name absent from base fixture: "+type);
        check(input.put(INSTAGRAM_MAIN,object(INSTAGRAM_MAIN))==null,"fresh inert Instagram marker absent from base fixture");
        byte[] code=dex(input);for(String pkg:PACKAGES)inject(Paths.get(dir,pkg+".apk"),code);
        Map<String,ClassDef> instagram=HomeBackAudit.read(Paths.get(dir,"com.instagram.android.apk").toString());
        for(String type:COLLISIONS)check(!instagram.containsKey(type),"Instagram generic fixture has no fake452 collision");
        for(String pkg:PACKAGES) {
            Map<String,ClassDef> actual=HomeBackAudit.read(Paths.get(dir,pkg+".apk").toString());
            for(String type:COLLISIONS)check(Arrays.equals(HomeBackAudit.canonical(input.get(type)),HomeBackAudit.canonical(actual.get(type))),"emitted fixture collision exact: "+pkg+" "+type);
        }
        System.out.println("PASS synthetic collision APK fixtures: four non-Instagram packages include three unrelated LX names and inert Instagram marker; Instagram fixture unchanged");
    }
    public static void main(String[] args)throws Exception {
        if(args.length==3&&"selftest".equals(args[0])){selftest(new File(args[1]),Paths.get(args[2]));return;}
        if(args.length==3&&"make-fixtures".equals(args[0])){makeFixtures(args[1],args[2]);return;}
        throw new IllegalArgumentException("AppScopeAudit selftest PINNED_277_MPP REPORT_JSON | make-fixtures ORIGINAL_DEX OUT_DIRECTORY");
    }
}
