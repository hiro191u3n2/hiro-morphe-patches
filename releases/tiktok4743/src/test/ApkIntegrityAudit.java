/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;

/** Whole-APK inventory check, complementary to the semantic native-hook audits.
 * It does not pretend that class/member presence proves runtime behavior.
 */
public final class ApkIntegrityAudit {
    interface Visitor { void accept(ClassDef value) throws Exception; }
    static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
    static void visit(String path, Visitor visitor) throws Exception {
        Set<String> entries = new HashSet<>();
        try (ZipFile zip = new ZipFile(path)) {
            Enumeration<? extends ZipEntry> enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                check(entries.add(entry.getName()), "Duplicate ZIP entry " + entry.getName());
                if (!entry.getName().matches("classes(?:[0-9]+)?\\.dex")) continue;
                try (InputStream input = new BufferedInputStream(zip.getInputStream(entry))) {
                    DexBackedDexFile dex = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), input);
                    for (ClassDef value : dex.getClasses()) visitor.accept(value);
                }
            }
        }
    }
    static Set<String> members(ClassDef value) {
        Set<String> result = new TreeSet<>();
        result.add("super " + value.getSuperclass());
        for (String type : value.getInterfaces()) result.add("interface " + type);
        for (Field field : value.getFields()) result.add("field " + field.getName() + ":" + field.getType());
        for (Method method : value.getMethods()) result.add("method " + method.getName() + method.getParameterTypes() + method.getReturnType());
        return result;
    }
    static byte[] fingerprint(Set<String> values) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String value : values) { digest.update(value.getBytes(StandardCharsets.UTF_8)); digest.update((byte)0); }
        return digest.digest();
    }
    static AndroidManifestBlock manifest(String path) throws Exception {
        try (ZipFile zip = new ZipFile(path); InputStream input = zip.getInputStream(zip.getEntry("AndroidManifest.xml"))) {
            return AndroidManifestBlock.load(input);
        }
    }
    public static void main(String[] args) throws Exception {
        check(args.length == 2, "Need ORIGINAL_APK PATCHED_APK");
        AndroidManifestBlock beforeManifest = manifest(args[0]), afterManifest = manifest(args[1]);
        String version = beforeManifest.getVersionName();
        check("47.4.3".equals(version) || "47.5.3".equals(version), "Unverified input version");
        int expectedCode = version.equals("47.4.3") ? 2024704030 : 2024705030;
        int expectedClasses = version.equals("47.4.3") ? 475931 : 480524;
        for (AndroidManifestBlock value : Arrays.asList(beforeManifest, afterManifest)) {
            check("com.zhiliaoapp.musically".equals(value.getPackageName()), "Wrong output/input package");
            check(version.equals(value.getVersionName()), "Wrong output/input version name");
            check(Integer.valueOf(expectedCode).equals(value.getVersionCode()), "Version code changed or wrong APK");
        }
        check(Objects.equals(beforeManifest.getMinSdkVersion(), afterManifest.getMinSdkVersion())
              && Objects.equals(beforeManifest.getTargetSdkVersion(), afterManifest.getTargetSdkVersion()), "SDK declarations changed");
        Set<String> addedPermissions = new TreeSet<>(afterManifest.getUsesPermissions());
        addedPermissions.removeAll(beforeManifest.getUsesPermissions());
        check(addedPermissions.isEmpty(), "Permissions added: " + addedPermissions);
        Map<String,byte[]> original = new HashMap<>();
        visit(args[0], value -> check(original.put(value.getType(), fingerprint(members(value))) == null,
                                     "Duplicate original class " + value.getType()));
        check(original.size() == expectedClasses, "Original native class inventory differs: " + original.size());
        Set<String> seen = new HashSet<>(), additions = new TreeSet<>();
        Map<String,Set<String>> changedMembers = new HashMap<>();
        visit(args[1], value -> {
            String type = value.getType();
            check(seen.add(type), "Duplicate emitted class " + type);
            byte[] expected = original.get(type);
            if (expected == null) {
                additions.add(type);
            } else {
                Set<String> actual = members(value);
                if (!Arrays.equals(expected, fingerprint(actual))) changedMembers.put(type, actual);
            }
        });
        check(seen.containsAll(original.keySet()), "Original native classes were removed");
        visit(args[0], value -> {
            Set<String> actual = changedMembers.get(value.getType());
            if (actual != null) check(actual.containsAll(members(value)), "Native member removed or inheritance changed: " + value.getType());
        });
        check(additions.contains("Lapp/hiro/oneback/runtime/TikTokHome;")
              && additions.contains("Lapp/hiro/tiktok4753/runtime/LegacyHooks;"), "New native runtimes absent");
        System.out.println("PASS APK inventory: original classes=" + original.size() + "; emitted=" + seen.size()
            + "; added=" + additions.size() + "; classes with added members=" + changedMembers.size()
            + "; no original classes/members removed; exact package/version/SDK retained; no permissions added");
    }
}
