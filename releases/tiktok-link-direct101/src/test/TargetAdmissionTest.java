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

/** Forced target rejection executes the shipped action before any DEX access. */
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
    public static void main(String[] args) throws Exception {
        Path folder = Paths.get(args[0]); Files.createDirectories(folder);
        PatcherConfig cfg = config(folder); int count=0;
        for (String[] row : new String[][] {
            {"com.zhiliaoapp.musically","47.5.3"},{"com.zhiliaoapp.musically","46.2.3"},
            {"com.zhiliaoapp.musically","47.4.2"},{"com.zhiliaoapp.musically","47.4.3-beta"},
            {"com.ss.android.ugc.trill","47.4.3"},{"unrelated.app","47.4.3"}}) {
            boolean rejected=false;
            try (BytecodePatchContext context = new BytecodePatchContext(cfg,new PackageMetadata(row[0],row[1],"1",null))) {
                TikTokLinkDirectPatch.DIRECT_SHARED_VIDEO.execute(context);
            } catch (RuntimeException e) {
                for (Throwable x=e;x!=null;x=x.getCause()) if(x.getMessage()!=null && x.getMessage().contains("requires original global TikTok 47.4.3")) rejected=true;
            }
            if (!rejected) throw new AssertionError("Wrong target was not rejected by production action: "+Arrays.toString(row));
            count++;
        }
        try (BytecodePatchContext context = new BytecodePatchContext(cfg,new PackageMetadata("com.zhiliaoapp.musically","47.4.3","2024704030",null))) {
            TikTokLinkDirectPatch.validateTarget(context);
        }
        System.out.println("PASS TargetAdmissionTest forced_rejections="+count+" no_dex_access=true accepted_target=47.4.3");
    }
}
