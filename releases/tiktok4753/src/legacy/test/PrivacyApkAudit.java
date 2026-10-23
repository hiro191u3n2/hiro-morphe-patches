/* SPDX-License-Identifier: GPL-3.0-only */
import app.hiro.tiktok4753.patches.LegacyPrivacy;
import app.hiro.tiktok4753.patches.PrivacyContainers;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableStringEncodedValue;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21c;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction31c;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import java.security.MessageDigest;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue;
import com.android.tools.smali.dexlib2.immutable.ImmutableField;
import com.android.tools.smali.dexlib2.immutable.value.ImmutableStringEncodedValue;
import com.reandroid.json.JSONObject;
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.value.ValueType;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Verify actual rebuilt APKs, including raw-entry reconstruction and original method constants.
 * Usage: PrivacyApkAudit original.apk patched.apk report.json
 * Classpath: compiled LegacyPrivacy + morphe200.jar. Requires Java 21 for the audit only.
 */
public final class PrivacyApkAudit {
    private static final java.util.regex.Pattern BOOT = java.util.regex.Pattern.compile("(?i)(?<![a-z0-9_.-])log-boot\\.tiktokv\\.com(?![a-z0-9_.-])");
    private static boolean hasBoot(String value) { return BOOT.matcher(value).find(); }
    private static String fieldKey(Field field) { return field.getDefiningClass() + "->" + field.getName() + ":" + field.getType(); }
    private static final String ORIGINAL_SHA = "6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e";
    private static void demand(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static byte[] read(ZipFile zip, String name) throws IOException {
        var entry = zip.getEntry(name); demand(entry != null, "missing APK entry " + name);
        try (var in = zip.getInputStream(entry)) { return in.readAllBytes(); }
    }
    private static List<String> entries(ZipFile zip, String prefix, String suffix) {
        List<String> names = new ArrayList<>(); var entries = zip.entries();
        while (entries.hasMoreElements()) { var e = entries.nextElement(); if (e.getName().startsWith(prefix) && e.getName().endsWith(suffix)) names.add(e.getName()); }
        Collections.sort(names); return names;
    }
    private static String methodKey(Method method) {
        return method.getDefiningClass() + "->" + method.getName() + method.getParameterTypes() + method.getReturnType();
    }
    private static Map<String,Integer> constants(Method method) {
        Map<String,Integer> result = new LinkedHashMap<>();
        if (method.getImplementation() == null) return result;
        for (var insn : method.getImplementation().getInstructions()) if ((insn.getOpcode() == Opcode.CONST_STRING || insn.getOpcode() == Opcode.CONST_STRING_JUMBO) &&
                insn instanceof ReferenceInstruction ref && ref.getReference() instanceof StringReference str)
            result.merge(str.getString(), 1, Integer::sum);
        return result;
    }
    private static Map<String,Object> dex(ZipFile original, ZipFile output) throws Exception {
        Map<String,Map<String,Integer>> expected = new LinkedHashMap<>();
        int originalConstants = 0, retainedBootConstants = 0, originalTrackingFields = 0;
        Map<String,ImmutableField> expectedFields = new LinkedHashMap<>();
        for (String name : entries(original, "classes", ".dex")) {
            var dex = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), new ByteArrayInputStream(read(original,name)));
            for (var clazz : dex.getClasses()) {
                for (var field : clazz.getStaticFields()) if (field.getInitialValue() instanceof StringEncodedValue value) {
                    String replacement = LegacyPrivacy.rewriteTrackingHosts(value.getValue());
                    if (!replacement.equals(value.getValue())) originalTrackingFields++;
                    expectedFields.put(fieldKey(field), new ImmutableField(field.getDefiningClass(),field.getName(),field.getType(),field.getAccessFlags(),
                        new ImmutableStringEncodedValue(replacement),field.getAnnotations(),field.getHiddenApiRestrictions()));
                }
                for (var method : clazz.getMethods()) {
                Map<String,Integer> rewritten = new LinkedHashMap<>(); boolean changed = false;
                for (var entry : constants(method).entrySet()) {
                    String value = entry.getKey(), replacement = LegacyPrivacy.rewriteTrackingHosts(value);
                    if (!value.equals(replacement)) { changed = true; originalConstants += entry.getValue(); }
                    if (hasBoot(value)) retainedBootConstants += entry.getValue();
                    if (!value.equals(replacement) || hasBoot(value)) rewritten.merge(replacement, entry.getValue(), Integer::sum);
                }
                if (changed || !rewritten.isEmpty()) expected.put(methodKey(method), rewritten);
                }
            }
        }
        demand(originalConstants > 0, "no original DEX tracking constants");
        int verifiedMethods = 0, remainingTracking = 0, remainingBoot = 0, remainingTrackingFields = 0;
        Set<String> checked = new HashSet<>(), checkedFields = new HashSet<>();
        for (String name : entries(output, "classes", ".dex")) {
            var dex = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), new ByteArrayInputStream(read(output,name)));
            for (var clazz : dex.getClasses()) {
                for (var field : clazz.getStaticFields()) {
                    if (field.getInitialValue() instanceof StringEncodedValue value && !value.getValue().equals(LegacyPrivacy.rewriteTrackingHosts(value.getValue()))) remainingTrackingFields++;
                    String key = fieldKey(field);
                    if (!expectedFields.containsKey(key)) continue;
                    demand(checkedFields.add(key), "duplicate output static field " + key);
                    var expectedField = expectedFields.get(key);
                    demand(expectedField.getInitialValue().equals(field.getInitialValue()), "static field initializer mismatch " + key);
                    demand(expectedField.getAccessFlags() == field.getAccessFlags() && expectedField.getAnnotations().equals(field.getAnnotations()) &&
                        expectedField.getHiddenApiRestrictions().equals(field.getHiddenApiRestrictions()), "static field metadata changed " + key);
                }
                for (var method : clazz.getMethods()) {
                Map<String,Integer> present = constants(method); String key = methodKey(method);
                for (var entry : present.entrySet()) {
                    if (!entry.getKey().equals(LegacyPrivacy.rewriteTrackingHosts(entry.getKey()))) remainingTracking += entry.getValue();
                    if (hasBoot(entry.getKey())) remainingBoot += entry.getValue();
                }
                if (!expected.containsKey(key)) continue;
                demand(checked.add(key), "duplicate output method " + key);
                for (var entry : expected.get(key).entrySet()) demand(present.getOrDefault(entry.getKey(), 0) >= entry.getValue(),
                    "original tracking rewrite/boot preservation missing in " + key + ": " + entry.getKey());
                verifiedMethods++;
                }
            }
        }
        demand(checkedFields.size() == expectedFields.size(), "original string fields disappeared");
        demand(originalTrackingFields == 2 && remainingTrackingFields == 0, "static tracking policy coverage original=" + originalTrackingFields + " remaining=" + remainingTrackingFields);
        demand(verifiedMethods == expected.size(), "original target methods disappeared");
        demand(remainingTracking == 0, "remaining blocked DEX constants: " + remainingTracking);
        demand(remainingBoot >= retainedBootConstants, "bootstrap allowlist lost: original=" + retainedBootConstants + " output=" + remainingBoot);
        Map<String,Object> report = new LinkedHashMap<>();
        report.put("original_tracking_constants", originalConstants); report.put("verified_target_and_boot_methods", verifiedMethods);
        report.put("original_string_static_fields",expectedFields.size()); report.put("verified_static_fields",checkedFields.size());
        report.put("rewritten_tracking_static_fields",originalTrackingFields); report.put("remaining_blocked_static_fields",remainingTrackingFields);
        report.put("non_target_static_values_and_all_field_metadata_preserved",true);
        report.put("remaining_blocked_constants", remainingTracking); report.put("original_bootstrap_constants", retainedBootConstants); report.put("output_bootstrap_constants", remainingBoot);
        return report;
    }
    private static Map<String,Object> nativeLibraries(ZipFile original, ZipFile output) throws Exception {
        List<String> paths = entries(original,"lib/",".so"); demand(paths.equals(entries(output,"lib/",".so")), "native entry set changed");
        List<Map<String,Object>> changed = new ArrayList<>(); int totalHosts = 0, unchanged = 0, embeddedConstants=0,embeddedFields=0; long bytes = 0;
        Map<String,Integer> formats=new TreeMap<>();
        for (String path : paths) {
            byte[] input = read(original,path), actual = read(output,path);
            var expected=PrivacyContainers.rewrite(input);formats.merge(expected.format,1,Integer::sum);
            demand(Arrays.equals(expected.bytes, actual), "native/container bytes mismatch: " + path);
            bytes += input.length;
            if (expected.changes() == 0) { unchanged++; continue; }
            Map<String,Object> row = new LinkedHashMap<>(); row.put("path",path);row.put("format",expected.format);
            row.put("elf_host_replacements",expected.hosts);row.put("embedded_dex_constants",expected.dexConstants);row.put("embedded_dex_fields",expected.dexFields);
            row.put("original_bytes",input.length);row.put("patched_bytes",actual.length);
            row.put("original_sha256",LegacyPrivacy.sha256(input));row.put("patched_sha256",LegacyPrivacy.sha256(actual));
            if(expected.format.equals("ELF"))demand(input.length==actual.length,"ELF offsets moved: "+path);
            else if(expected.format.equals("ZIP_DEX"))row.put("archive_integrity",verifyArchive(input,actual));
            else if(expected.format.equals("DEX"))row.put("dex_integrity",verifyDex(input,actual));
            else throw new IllegalStateException("modified unsupported format "+path);
            changed.add(row);totalHosts+=expected.hosts;embeddedConstants+=expected.dexConstants;embeddedFields+=expected.dexFields;
        }
        demand(!changed.isEmpty(), "no original native tracking hosts were rewritten");
        Map<String,Object> report = new LinkedHashMap<>(); report.put("scanned_files",paths.size()); report.put("scanned_bytes",bytes);report.put("formats",formats);
        report.put("unchanged_files",unchanged); report.put("changed_files",changed.size()); report.put("elf_rewritten_hosts",totalHosts);
        report.put("embedded_dex_rewritten_constants",embeddedConstants);report.put("embedded_dex_rewritten_fields",embeddedFields);report.put("changed",changed);
        report.put("elf_non_target_bytes_and_offsets_preserved",true);report.put("zip_metadata_alignment_crc_and_dex_semantics_verified",true);return report;
    }
    private static int le16(byte[] a,int at){return(a[at]&255)|((a[at+1]&255)<<8);}
    private static int le32(byte[] a,int at){return le16(a,at)|(le16(a,at+2)<<16);}
    private static Map<String,Integer> offsets(byte[] bytes) {
        int end=-1;for(int i=bytes.length-22;i>=Math.max(0,bytes.length-65557);i--)if(le32(bytes,i)==0x06054b50&&i+22+le16(bytes,i+20)==bytes.length){end=i;break;}
        demand(end>=0,"ZIP EOCD missing");int count=le16(bytes,end+10),at=le32(bytes,end+16);Map<String,Integer> result=new LinkedHashMap<>();
        for(int i=0;i<count;i++){
            demand(le32(bytes,at)==0x02014b50,"ZIP central directory signature");int size=le16(bytes,at+28),local=le32(bytes,at+42);
            String name=new String(bytes,at+46,size,StandardCharsets.UTF_8);demand(le32(bytes,local)==0x04034b50,"ZIP local header signature");
            result.put(name,local+30+le16(bytes,local+26)+le16(bytes,local+28));at+=46+size+le16(bytes,at+30)+le16(bytes,at+32);
        }return result;
    }
    private static Map<String,Object> verifyArchive(byte[] original,byte[] actual)throws Exception {
        Path before=Files.createTempFile("hiro-privacy-original-", ".zip"),after=Files.createTempFile("hiro-privacy-rebuilt-", ".zip");
        try {
            Files.write(before,original);Files.write(after,actual);List<Map<String,Object>> dex=new ArrayList<>();
            try(var left=new ZipFile(before.toFile());var right=new ZipFile(after.toFile())){
                List<String> leftNames=Collections.list(left.entries()).stream().map(ZipEntry::getName).toList();
                List<String> rightNames=Collections.list(right.entries()).stream().map(ZipEntry::getName).toList();
                demand(leftNames.equals(rightNames),"archive names/order changed");demand(Objects.equals(left.getComment(),right.getComment()),"archive comment changed");
                var payloadOffsets=offsets(actual);
                for(String name:leftNames){var a=left.getEntry(name);var b=right.getEntry(name);byte[] input=read(left,name),output=read(right,name);CRC32 crc=new CRC32();crc.update(output);
                    demand(crc.getValue()==b.getCrc()&&output.length==b.getSize(),"ZIP CRC/size mismatch "+name);
                    demand(a.getMethod()==b.getMethod()&&a.getTime()==b.getTime()&&Objects.equals(a.getComment(),b.getComment())&&Arrays.equals(a.getExtra(),b.getExtra()),"ZIP metadata changed "+name);
                    if(name.matches("classes(?:[0-9]+)?\\.dex")){
                        demand(b.getMethod()==ZipEntry.STORED&&(payloadOffsets.get(name)&3)==0,"embedded DEX compression/alignment changed");
                        var proof=verifyDex(input,output);proof.put("name",name);dex.add(proof);
                    }else demand(Arrays.equals(input,output),"non-DEX archive content changed "+name);
                }
            }
            Map<String,Object> proof=new LinkedHashMap<>();proof.put("zip_crc_metadata_order_and_alignment_valid",true);proof.put("non_dex_entries_unchanged",true);proof.put("dex",dex);return proof;
        }finally{Files.deleteIfExists(before);Files.deleteIfExists(after);}
    }
    private static byte[] canonical(Collection<? extends ClassDef> classes)throws Exception{
        var store=new MemoryDataStore();try{DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),classes));return store.getData();}finally{store.close();}
    }
    private static Map<String,Object> verifyDex(byte[] original,byte[] actual)throws Exception{
        demand(le32(actual,32)==actual.length,"DEX file_size header");var adler=new Adler32();adler.update(actual,12,actual.length-12);demand(le32(actual,8)==(int)adler.getValue(),"DEX Adler32 mismatch");
        var sha=MessageDigest.getInstance("SHA-1");sha.update(actual,32,actual.length-32);demand(Arrays.equals(Arrays.copyOfRange(actual,12,32),sha.digest()),"DEX SHA1 mismatch");
        var input=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new ByteArrayInputStream(original));var output=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new ByteArrayInputStream(actual));
        String prior=null;for(var reference:output.getStringReferences()){demand(prior==null||prior.compareTo(reference.getString())<0,"DEX string_ids not sorted/unique");prior=reference.getString();}
        Map<String,ClassDef> originals=new TreeMap<>(),reverted=new TreeMap<>();for(var c:input.getClasses())originals.put(c.getType(),c);for(var c:output.getClasses())reverted.put(c.getType(),c);
        demand(originals.keySet().equals(reverted.keySet()),"embedded DEX classes changed");int constants=0,fields=0;
        for(var clazz:originals.values()){
            ClassDef actualClass=reverted.get(clazz.getType());Map<String,Field> originalFields=new HashMap<>();Map<String,Method> originalMethods=new HashMap<>();
            for(var f:clazz.getFields())originalFields.put(fieldKey(f),f);for(var m:clazz.getMethods())originalMethods.put(methodKey(m),m);
            List<Field> restoredFields=new ArrayList<>();List<Method> restoredMethods=new ArrayList<>();
            for(var field:actualClass.getFields()){
                Field restored=field,old=originalFields.get(fieldKey(field));
                if(old!=null&&(old.getAccessFlags()&8)!=0&&old.getInitialValue() instanceof StringEncodedValue value&&!value.getValue().equals(LegacyPrivacy.rewriteTrackingHosts(value.getValue()))){
                    demand(field.getInitialValue() instanceof StringEncodedValue current&&current.getValue().equals(LegacyPrivacy.rewriteTrackingHosts(value.getValue())),"embedded static rewrite mismatch");
                    restored=new ImmutableField(field.getDefiningClass(),field.getName(),field.getType(),field.getAccessFlags(),new ImmutableStringEncodedValue(value.getValue()),field.getAnnotations(),field.getHiddenApiRestrictions());fields++;
                }restoredFields.add(restored);
            }
            for(var method:actualClass.getMethods()){
                var old=originalMethods.get(methodKey(method));if(old==null||old.getImplementation()==null){restoredMethods.add(method);continue;}
                demand(method.getImplementation()!=null,"embedded implementation disappeared");List<Instruction> previous=new ArrayList<>(),present=new ArrayList<>();
                for(var i:old.getImplementation().getInstructions())previous.add(i);for(var i:method.getImplementation().getInstructions())present.add(i);
                demand(previous.size()==present.size(),"embedded instruction count changed");boolean edited=false;
                for(int index=0;index<previous.size();index++){
                    var insn=previous.get(index);
                    if((insn.getOpcode()==Opcode.CONST_STRING||insn.getOpcode()==Opcode.CONST_STRING_JUMBO)&&insn instanceof ReferenceInstruction ref&&ref.getReference() instanceof StringReference str&&
                            !str.getString().equals(LegacyPrivacy.rewriteTrackingHosts(str.getString()))){
                        var current=present.get(index);
                        demand((current.getOpcode()==Opcode.CONST_STRING||current.getOpcode()==Opcode.CONST_STRING_JUMBO)&&((OneRegisterInstruction)current).getRegisterA()==((OneRegisterInstruction)insn).getRegisterA()&&
                            ((StringReference)((ReferenceInstruction)current).getReference()).getString().equals(LegacyPrivacy.rewriteTrackingHosts(str.getString())),"embedded constant/register rewrite mismatch");
                        int reg=((OneRegisterInstruction)insn).getRegisterA();var originalString=new ImmutableStringReference(str.getString());
                        present.set(index,insn.getOpcode()==Opcode.CONST_STRING?new ImmutableInstruction21c(Opcode.CONST_STRING,reg,originalString):new ImmutableInstruction31c(Opcode.CONST_STRING_JUMBO,reg,originalString));constants++;edited=true;
                    }
                }
                var body=method.getImplementation();restoredMethods.add(edited?new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),method.getAccessFlags(),
                    method.getAnnotations(),method.getHiddenApiRestrictions(),new ImmutableMethodImplementation(body.getRegisterCount(),present,body.getTryBlocks(),body.getDebugItems())):method);
            }
            reverted.put(clazz.getType(),new ImmutableClassDef(actualClass.getType(),actualClass.getAccessFlags(),actualClass.getSuperclass(),actualClass.getInterfaces(),actualClass.getSourceFile(),actualClass.getAnnotations(),restoredFields,restoredMethods));
        }
        if(!Arrays.equals(canonical(originals.values()),canonical(reverted.values()))){
            List<String> changed=new ArrayList<>();
            for(String type:originals.keySet())if(!Arrays.equals(canonical(Collections.singleton(originals.get(type))),canonical(Collections.singleton(reverted.get(type)))))changed.add(type);
            throw new IllegalStateException("non-target embedded DEX semantics changed: classes="+changed);
        }
        Map<String,Object> proof=new LinkedHashMap<>();proof.put("classes",originals.size());proof.put("rewritten_constants",constants);proof.put("rewritten_fields",fields);
        proof.put("sha1_adler32_file_size_and_string_order_valid",true);proof.put("non_target_class_method_field_annotation_debug_and_try_semantics_preserved",true);return proof;
    }
    private static Document manifest(ZipFile zip) throws Exception {
        var xml = new ResXmlDocument(); xml.readBytes(new ByteArrayInputStream(read(zip,"AndroidManifest.xml")));
        var factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().newDocument();
        document.appendChild(manifestElement(document, xml.getDocumentElement())); return document;
    }
    private static Element manifestElement(Document document, ResXmlElement input) {
        Element out = document.createElement(input.getName());
        var attrs = input.getAttributes();
        while (attrs.hasNext()) {
            var attribute = attrs.next(); String name = attribute.getName(); String uri = attribute.getUri();
            String value;
            if (attribute.getValueType() == ValueType.STRING) value = attribute.getValueAsString();
            else if (attribute.getValueType() == ValueType.BOOLEAN) value = attribute.getData() == 0 ? "false" : "true";
            else value = attribute.getValueType().toString() + ":" + Integer.toUnsignedString(attribute.getData());
            if (uri == null || uri.isEmpty()) out.setAttribute(name, value);
            else {
                String prefix = "http://schemas.android.com/apk/res/android".equals(uri) ? "android" : attribute.getPrefix();
                if (name.contains(":")) name = name.substring(name.indexOf(':') + 1);
                out.setAttributeNS(uri, (prefix == null || prefix.isEmpty() ? "" : prefix + ":") + name, value);
            }
        }
        var children = input.getElements(); while (children.hasNext()) out.appendChild(manifestElement(document,(ResXmlElement)children.next()));
        return out;
    }
    private static String canonical(Element element) {
        List<String> attrs = new ArrayList<>(); var list = element.getAttributes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i); String name = node.getNodeName();
            // The independent OneBack patch adds this flag. It is not privacy policy.
            if (name.equals("android:enableOnBackInvokedCallback") || name.startsWith("xmlns")) continue;
            attrs.add(name + "=" + node.getNodeValue());
        }
        Collections.sort(attrs); List<String> children = new ArrayList<>(); var childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) if (childNodes.item(i) instanceof Element child) children.add(canonical(child));
        Collections.sort(children); return element.getTagName() + attrs + children;
    }
    private static Map<String,Object> manifest(ZipFile original, ZipFile output) throws Exception {
        var expected = manifest(original); var counts = LegacyPrivacy.hardenManifest(expected); var actual = manifest(output);
        demand(canonical(expected.getDocumentElement()).equals(canonical(actual.getDocumentElement())), "binary manifest differs from complete privacy policy plus allowed OneBack flags");
        demand(counts.removed == 24 && counts.flags == 12, "unexpected original 47.5.3 manifest coverage");
        Map<String,Object> report = new LinkedHashMap<>(); report.put("removed_tracking_nodes",counts.removed); report.put("false_metadata_flags",counts.flags);
        report.put("all_other_manifest_content_preserved",true); report.put("oneback_flag_excluded_from_comparison",true); return report;
    }
    public static void main(String[] args) throws Exception {
        demand(args.length == 3, "Usage: PrivacyApkAudit original.apk patched.apk report.json");
        String originalSHA = shaFile(Path.of(args[0])); demand(ORIGINAL_SHA.equals(originalSHA), "unreviewed original APK identity");
        Map<String,Object> report = new LinkedHashMap<>(); report.put("schema","hiro-4753-privacy-apk-v1"); report.put("original_sha256",originalSHA); report.put("patched_sha256",shaFile(Path.of(args[1])));
        try (var original = new ZipFile(args[0]); var output = new ZipFile(args[1])) {
            report.put("manifest",manifest(original,output)); System.out.println("PASS privacy actual binary manifest");
            report.put("native",nativeLibraries(original,output)); System.out.println("PASS privacy ELF bytes and ZIP/DEX integrity/semantics");
            report.put("dex",dex(original,output)); System.out.println("PASS privacy original DEX method constants and bootstrap allowlist");
        }
        report.put("physical_android_tested",false); report.put("success",true);
        Files.writeString(Path.of(args[2]), new JSONObject(report).toString(2) + "\n");
        System.out.println("PRIVACY_APK_AUDIT_PASSED " + args[2]);
    }
    private static String shaFile(Path path) throws Exception {
        var digest = java.security.MessageDigest.getInstance("SHA-256"); byte[] bytes = new byte[1024*1024];
        try (var in = Files.newInputStream(path)) { int count; while ((count=in.read(bytes))!=-1) digest.update(bytes,0,count); }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }
}
