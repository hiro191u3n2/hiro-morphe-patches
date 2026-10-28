/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.*;
import java.util.*;
import app.morphe.patcher.PackageMetadata;
import app.morphe.patcher.PatcherConfig;
import app.morphe.patcher.dex.BytecodeMode;
import app.morphe.patcher.dex.DexVerifier;
import app.morphe.patcher.patch.*;
import app.hiro.tiktoklink.patches.TikTokLinkDirectPatch;

/** Forced target rejection executes the shipped extension-free prerequisite.
 * The named runtime-bearing action must depend on this exact admitted guard. */
public final class TargetAdmissionTest {
    static final DexVerifier NO_DEX = new DexVerifier() {
        public void verifyDexFile(File f) { throw new AssertionError("Rejected target touched DEX"); }
        public void verifyDexDirectory(File f) { throw new AssertionError("Rejected target touched DEX"); }
        public void verifyApkFile(File f) { throw new AssertionError("Rejected target touched APK"); }
    };
    static PatcherConfig config(Path p) throws Exception {
        for (Constructor<?> c : PatcherConfig.class.getConstructors()) {
            Class<?>[] a = c.getParameterTypes();
            if (a.length==9 && a[2]==String.class) return (PatcherConfig)c.newInstance(
                p.resolve("unused.apk").toFile(),p.toFile(),null,null,true,Collections.emptySet(),BytecodeMode.FULL,NO_DEX,null);
            if (a.length==8 && a[2]==Set.class) return (PatcherConfig)c.newInstance(
                p.resolve("unused.apk").toFile(),p.toFile(),Collections.emptySet(),BytecodeMode.FULL,NO_DEX,null,"2.0.0",true);
        }
        throw new AssertionError("Unsupported PatcherConfig");
    }
    static String scope(Patch<?> patch) {
        if(patch.getCompatibility()==null)return "ANY";
        List<String> parts=new ArrayList<String>();
        for(Compatibility compatibility:patch.getCompatibility()){
            List<String> versions=new ArrayList<String>();
            if(compatibility.getTargets()==null)versions.add("ANY");
            else for(AppTarget target:compatibility.getTargets())versions.add(String.valueOf(target.getVersion()));
            Collections.sort(versions);parts.add(compatibility.getPackageName()+":"+versions);
        }
        Collections.sort(parts);return parts.toString();
    }
    /** The legacy getter is synthetic/hidden from javac in the newer SDK.
     * Inspect provider presence without invoking a supplier or opening an extension. */
    static boolean hasExtension(BytecodePatch patch) throws Exception {
        try {
            Object providers=BytecodePatch.class.getMethod("getExtensionStreamProviders$morphe_patcher").invoke(patch);
            if(providers==null)return false;
            if(!(providers instanceof Iterable))throw new AssertionError("Unrecognized extension-provider API");
            return ((Iterable<?>)providers).iterator().hasNext();
        } catch(NoSuchMethodException legacy) {
            return BytecodePatch.class.getMethod("getExtensionInputStream").invoke(patch)!=null;
        }
    }
    public static void main(String[] args) throws Exception {
        Path folder = Paths.get(args[0]); Files.createDirectories(folder);
        PatcherConfig cfg = config(folder); int count=0;
        BytecodePatch patch=TikTokLinkDirectPatch.DIRECT_SHARED_VIDEO;
        if(patch.getDependencies().size()!=1)throw new AssertionError("Named patch must have exactly one target prerequisite");
        Patch<?> dependency=patch.getDependencies().iterator().next();
        if(!(dependency instanceof BytecodePatch))throw new AssertionError("Target prerequisite must be bytecode");
        BytecodePatch guard=(BytecodePatch)dependency;
        if(guard.getName()!=null||hasExtension(guard)||!guard.getDependencies().isEmpty())
            throw new AssertionError("Target prerequisite must be unnamed and extension-free with no earlier dependency");
        if(!scope(guard).equals("[com.zhiliaoapp.musically:[47.4.3]]")
                ||!scope(guard).equals(scope(patch)))
            throw new AssertionError("Prerequisite/named exact target scope differs");
        if(!hasExtension(patch))throw new AssertionError("Named patch runtime supplier missing");
        for (String[] row : new String[][] {
            {"com.zhiliaoapp.musically","47.5.3"},{"com.zhiliaoapp.musically","46.2.3"},
            {"com.zhiliaoapp.musically","47.4.2"},{"com.zhiliaoapp.musically","47.4.3-beta"},
            {"com.ss.android.ugc.trill","47.4.3"},{"unrelated.app","47.4.3"}}) {
            boolean rejected=false;
            try (BytecodePatchContext context = new BytecodePatchContext(cfg,new PackageMetadata(row[0],row[1],"1",null))) {
                guard.execute(context);
            } catch (RuntimeException e) {
                for (Throwable x=e;x!=null;x=x.getCause()) if(x.getMessage()!=null && x.getMessage().contains("requires original global TikTok 47.4.3")) rejected=true;
            }
            if (!rejected) throw new AssertionError("Wrong target was not rejected by production prerequisite: "+Arrays.toString(row));
            count++;
        }
        try (BytecodePatchContext context = new BytecodePatchContext(cfg,new PackageMetadata("com.zhiliaoapp.musically","47.4.3","2024704030",null))) {
            guard.execute(context);
        }
        System.out.println("PASS TargetAdmissionTest forced_rejections="+count+" prerequisite_count=1 unnamed_guard=true guard_extension_absent=true no_dex_access=true accepted_target=47.4.3");
    }
}
