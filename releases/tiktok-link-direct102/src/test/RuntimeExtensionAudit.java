/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.reference.TypeReference;

/** Inspect the serialized extension independently of its implementation helpers. */
public final class RuntimeExtensionAudit {
    private static final String PREFIX = "Lapp/hiro/tiktoklink/runtime/";
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    private static DexBackedDexFile dex(InputStream input) throws Exception {
        return DexBackedDexFile.fromInputStream(Opcodes.getDefault(), new BufferedInputStream(input));
    }
    private static Set<String> inventory(DexBackedDexFile dex) {
        Set<String> result = new TreeSet<>();
        for (ClassDef c : dex.getClasses()) check(result.add(c.getType()), "Duplicate runtime type " + c.getType());
        return result;
    }
    private static String sha(byte[] raw) throws Exception {
        StringBuilder out = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(raw)) out.append(String.format(Locale.ROOT, "%02x", b & 255));
        return out.toString();
    }
    public static void main(String[] args) throws Exception {
        check(args.length == 3, "RUNTIME_DEX COMPILED_RUNTIME_JAR BASELINE_MPP");
        byte[] raw = Files.readAllBytes(Paths.get(args[0]));
        DexBackedDexFile output = dex(new ByteArrayInputStream(raw));
        Set<String> actual = inventory(output), compiled = new TreeSet<>();
        check(!actual.isEmpty(), "Runtime extension is empty");
        for (String type : actual) check(type.startsWith(PREFIX), "Foreign runtime definition " + type);
        for (TypeReference type : output.getTypeReferences())
            check(!type.getType().contains("Lapp/hiro/oneback/"), "Runtime must not depend on OneBack source classes: " + type);
        try (ZipFile jar = new ZipFile(args[1])) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                check(!entry.isDirectory() && entry.getName().endsWith(".class"), "Unexpected runtime compiler entry");
                String type = "L" + entry.getName().substring(0, entry.getName().length() - 6) + ";";
                check(type.startsWith(PREFIX) && compiled.add(type), "Foreign/duplicate compiled runtime type " + type);
            }
        }
        check(actual.equals(compiled), "Serialized runtime class inventory differs from actual Android-API compilation");
        int inspected = 0;
        try (ZipFile base = new ZipFile(args[2])) {
            Enumeration<? extends ZipEntry> entries = base.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.matches("classes[0-9]*\\.dex") && !(name.startsWith("extensions/") && name.endsWith(".mpe"))) continue;
                try (InputStream in = base.getInputStream(entry)) {
                    for (String type : inventory(dex(in))) check(!actual.contains(type), "New runtime collides with baseline " + name + ": " + type);
                }
                inspected++;
            }
        }
        check(inspected > 1, "Baseline runtime extensions were not inspected");
        System.out.println("PASS RuntimeExtensionAudit classes=" + actual.size() + " namespace=app.hiro.tiktoklink.runtime"
            + " exact_compiled_inventory=true baseline_dex_entries=" + inspected
            + " baseline_collisions=0 oneback_type_dependencies=0 sha256=" + sha(raw));
    }
}
