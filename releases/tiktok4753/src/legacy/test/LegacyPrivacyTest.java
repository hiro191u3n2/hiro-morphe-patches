/* SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktok4753.patches;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableField;
import com.android.tools.smali.dexlib2.HiddenApiRestriction;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import java.util.zip.*;
import java.security.MessageDigest;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.value.ImmutableStringEncodedValue;
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue;

/** Tests policy boundaries, ELF offset preservation and both Android DOM modes. */
public final class LegacyPrivacyTest {
    private static int assertions;
    private static void check(boolean condition, String message) {
        assertions++; if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        String[] allowed = {
            "log-boot.tiktokv.com", "LOG-BOOT.TIKTOKV.COM.",
            "api16-normal-c-useast1a.tiktokv.com", "api.tiktok.com", "www.tiktok.com",
            "login.tiktokv.com", "im-api.tiktokv.com", "webcast.tiktokv.com", "push.tiktokv.com",
            "v16.tiktokcdn.com", "p16-sign-va.tiktokcdn.com", "p16-sign.tiktokcdn-us.com",
            "v16m-default.akamaized.net", "api.snssdk.com", "p16.ibyteimg.com", "v16.ibytedtos.com",
            "notappsflyer.com", "appsflyer.com.example.org", "adjustment.com", "foo.com",
            "advice.tiktokv.com", "login.byteoversea.com", "metrics.example.org", "localhost"
        };
        for (String host : allowed) {
            check(!LegacyPrivacy.isBlockedHost(host), "normal/boot host blocked: " + host);
            String value = "https://" + host + "/api?token=abc&value=$x";
            check(value.equals(LegacyPrivacy.rewriteTrackingHosts(value)), "normal URL changed: " + host);
        }
        String[] blocked = {
            "appsflyer.com", "sub.appsflyersdk.com", "x.adjust.com", "bnc.lt.branch.io", "app-measurement.com",
            "stats.google-analytics.com", "x.doubleclick.net", "x.googlesyndication.com", "api.crashlytics.com",
            "o123.ingest.sentry.io", "api.bugsnag.com", "api.amplitude.com", "api.mixpanel.com", "api.segment.io",
            "beacon.scorecardresearch.com", "x.apmplus.volces.com", "x.volceapplog.com", "x.pangle-b.io",
            "sf16-telemetry-proxy-oci.tiktokcdn-us.com", "LOG16-NORMAL.TIKTOKV.COM", "log.tiktokv.us",
            "rtlog-boot.tiktokv.com", "ads.tiktokw.us", "mssdk.byteoversea.com", "rtc-logger.bytdance.example.org.adjust.world",
            "analytics.bytedance.net", "ad16.ibyteimg.com", "x-log-y.ibytedtos.com", "pangolin.snssdk.com",
            "toblog.isnssdk.com", "tnc.sgsnssdk.com", "mon.ctobsnssdk.com", "xapplogy.tobsnssdk.com"
        };
        for (String host : blocked) {
            check(LegacyPrivacy.isBlockedHost(host), "tracking host allowed: " + host);
            check("https://blocked.invalid/api?x=1".equals(LegacyPrivacy.rewriteTrackingHosts("https://" + host + "/api?x=1")),
                "DEX rewrite wrong: " + host);
        }
        check("log-boot.tiktokv.com blocked.invalid api.tiktok.com blocked.invalid".equals(
            LegacyPrivacy.rewriteTrackingHosts("log-boot.tiktokv.com log16.tiktokv.com api.tiktok.com appsflyer.com")),
            "mixed constants lost safe spans");
        check("blocked.invalid".equals(LegacyPrivacy.rewriteTrackingHosts("blocked.invalid")), "not idempotent");
        String nativeText = "https://appsflyer.com/a https://api.tiktok.com/v\u0000log-boot.tiktokv.com\u0000https://log16.tiktokv.com/b";
        byte[] prefix = {(byte)0x7f, 'E', 'L', 'F', 0, (byte)0xff, (byte)0x80};
        byte[] raw = nativeText.getBytes(StandardCharsets.ISO_8859_1);
        byte[] before = new byte[prefix.length + raw.length + 1];
        System.arraycopy(prefix, 0, before, 0, prefix.length); System.arraycopy(raw, 0, before, prefix.length, raw.length);
        before[before.length - 1] = (byte)0xfe;
        byte[] result = before.clone();
        check(LegacyPrivacy.rewriteNativeBytes(result) == 2, "native match count");
        String expectedText = "https://xxxxx.invalid/a https://api.tiktok.com/v\u0000log-boot.tiktokv.com\u0000https://xxxxxxxxx.invalid/b";
        byte[] expected = before.clone();
        System.arraycopy(expectedText.getBytes(StandardCharsets.ISO_8859_1), 0, expected, prefix.length, raw.length);
        check(Arrays.equals(expected, result), "native rewrite changed ELF bytes or non-target host bytes");
        check(result.length == before.length, "native ELF offsets changed");
        check(LegacyPrivacy.rewriteNativeBytes(result) == 0, "native not idempotent");
        check(LegacyPrivacy.rewriteNativeBytes(new byte[]{0,(byte)255,1}) == 0, "binary data rewritten");
        staticFields();
        containers();
        for (boolean namespaceAware : new boolean[]{false, true}) manifest(namespaceAware);
        System.out.println("PASS LegacyPrivacyTest assertions=" + assertions + " (host boundaries, native offsets, DOM namespace modes)");
    }
    private static byte[] fixtureDex(String methodHost, String fieldHost) throws Exception {
        ImmutableField field = new ImmutableField("LPrivacyFixture;", "HOST", "Ljava/lang/String;", 25,
            new ImmutableStringEncodedValue(fieldHost), Collections.<Annotation>emptySet(), Collections.<HiddenApiRestriction>emptySet());
        ImmutableMethod method = new ImmutableMethod("LPrivacyFixture;", "endpoint", Collections.<MethodParameter>emptyList(), "Ljava/lang/String;", 9,
            Collections.<Annotation>emptySet(), Collections.<HiddenApiRestriction>emptySet(), new ImmutableMethodImplementation(1,
                Arrays.asList(new ImmutableInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(methodHost)), new ImmutableInstruction11x(Opcode.RETURN_OBJECT,0)),
                Collections.<ImmutableTryBlock>emptyList(), Arrays.asList(new com.android.tools.smali.dexlib2.immutable.debug.ImmutableLineNumber(1,123),new com.android.tools.smali.dexlib2.immutable.debug.ImmutableLineNumber(2,124))));
        ImmutableClassDef clazz = new ImmutableClassDef("LPrivacyFixture;", 1, "Ljava/lang/Object;", Collections.<String>emptyList(), "Fixture.java",
            Collections.<Annotation>emptySet(), Collections.singleton(field), Collections.singleton(method));
        MemoryDataStore store = new MemoryDataStore();
        try { DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(clazz))); return store.getData(); }
        finally { store.close(); }
    }
    private static void storedEntry(ZipOutputStream zip, ByteArrayOutputStream output, String name, byte[] bytes) throws Exception {
        ZipEntry entry = new ZipEntry(name); entry.setMethod(ZipEntry.STORED); entry.setSize(bytes.length); entry.setCompressedSize(bytes.length);
        CRC32 crc = new CRC32(); crc.update(bytes); entry.setCrc(crc.getValue()); entry.setTime(1600000000000L); entry.setComment("retained entry comment");
        int padding = (4-(output.size()+30+name.getBytes(StandardCharsets.UTF_8).length+4)%4)%4;
        byte[] extra = new byte[4+padding]; extra[0]=(byte)0xfe;extra[1]=(byte)0xca;extra[2]=(byte)padding;entry.setExtra(extra);
        zip.putNextEntry(entry);zip.write(bytes);zip.closeEntry();
    }
    private static Map<String,byte[]> readZip(byte[] bytes) throws Exception {
        Map<String,byte[]> entries = new LinkedHashMap<String,byte[]>();
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes));
        try { ZipEntry entry; byte[] buffer = new byte[4096]; while((entry=zip.getNextEntry())!=null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); int count; while((count=zip.read(buffer))!=-1)out.write(buffer,0,count);
            entries.put(entry.getName(),out.toByteArray()); zip.closeEntry();
        }} finally { zip.close(); } return entries;
    }
    private static int le32(byte[] bytes,int offset) {return (bytes[offset]&255)|((bytes[offset+1]&255)<<8)|((bytes[offset+2]&255)<<16)|((bytes[offset+3]&255)<<24);}
    private static void dexIntegrity(byte[] bytes) throws Exception {
        check(le32(bytes,32)==bytes.length,"DEX file size header");
        Adler32 checksum = new Adler32();checksum.update(bytes,12,bytes.length-12);check(le32(bytes,8)==(int)checksum.getValue(),"DEX Adler32");
        MessageDigest sha1=MessageDigest.getInstance("SHA-1");sha1.update(bytes,32,bytes.length-32);check(Arrays.equals(Arrays.copyOfRange(bytes,12,32),sha1.digest()),"DEX SHA1");
        DexBackedDexFile dex=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new ByteArrayInputStream(bytes));String prior=null;
        for(StringReference value:dex.getStringReferences()){check(prior==null||prior.compareTo(value.getString())<0,"DEX string_ids not sorted/unique");prior=value.getString();}
    }
    private static void containers() throws Exception {
        byte[] target = fixtureDex("https://mon.tiktokv.com", "https://mon-va.tiktokv.com");
        byte[] safe = fixtureDex("https://log-boot.tiktokv.com", "https://api.tiktok.com");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes);zip.setComment("retained archive comment");
        storedEntry(zip,bytes,"classes.dex",target);storedEntry(zip,bytes,"assets/keep.txt","unchanged non-DEX payload".getBytes(StandardCharsets.UTF_8));storedEntry(zip,bytes,"classes2.dex",safe);zip.close();
        byte[] original=bytes.toByteArray(),naive=original.clone();check(LegacyPrivacy.rewriteNativeBytes(naive)>0,"ZIP regression fixture has no endpoints");
        boolean corrupt=false;try{readZip(naive);}catch(ZipException expected){corrupt=true;}check(corrupt,"raw .so replacement did not reproduce ZIP CRC corruption");
        PrivacyContainers.Result rewritten=PrivacyContainers.rewrite(original);
        check(rewritten.format.equals("ZIP_DEX")&&rewritten.dexConstants==1&&rewritten.dexFields==1&&rewritten.dexEntries==2,"container rewrite counts");
        Map<String,byte[]> parsed=readZip(rewritten.bytes);check(new ArrayList<String>(parsed.keySet()).equals(Arrays.asList("classes.dex","assets/keep.txt","classes2.dex")),"ZIP names/order changed");
        check(Arrays.equals(parsed.get("assets/keep.txt"),"unchanged non-DEX payload".getBytes(StandardCharsets.UTF_8)),"non-DEX payload changed");
        check(Arrays.equals(parsed.get("classes2.dex"),safe),"unaffected DEX changed");
        check(new String(rewritten.bytes,StandardCharsets.ISO_8859_1).contains("retained archive comment"),"ZIP archive comment lost");
        dexIntegrity(parsed.get("classes.dex"));dexIntegrity(parsed.get("classes2.dex"));
        DexBackedDexFile dex=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new ByteArrayInputStream(parsed.get("classes.dex")));
        ClassDef clazz=dex.getClasses().iterator().next();Field field=clazz.getStaticFields().iterator().next();Method method=clazz.getMethods().iterator().next();
        check(field.getAccessFlags()==25&&((StringEncodedValue)field.getInitialValue()).getValue().equals("https://blocked.invalid"),"embedded field semantics");
        List<Instruction> insns=new ArrayList<Instruction>();for(Instruction insn:method.getImplementation().getInstructions())insns.add(insn);
        check(method.getAccessFlags()==9&&method.getImplementation().getRegisterCount()==1&&insns.size()==2,"embedded method shape changed");
        List<Integer> debugOffsets=new ArrayList<Integer>();for(com.android.tools.smali.dexlib2.iface.debug.DebugItem item:method.getImplementation().getDebugItems())debugOffsets.add(item.getCodeAddress());
        check(debugOffsets.equals(Arrays.asList(1,2)),"interior-code-unit DEX debug addresses changed");
        check(((StringReference)((ReferenceInstruction)insns.get(0)).getReference()).getString().equals("https://blocked.invalid")&&((OneRegisterInstruction)insns.get(0)).getRegisterA()==0&&insns.get(1).getOpcode()==Opcode.RETURN_OBJECT,"embedded instruction semantics");
        check(Arrays.equals(PrivacyContainers.rewrite(rewritten.bytes).bytes,rewritten.bytes),"container not idempotent");
        PrivacyContainers.Result direct=PrivacyContainers.rewrite(target);check(direct.format.equals("DEX")&&direct.changes()==2,"plain DEX classification");dexIntegrity(direct.bytes);
        boolean rejected=false;try{PrivacyContainers.rewrite("mon.tiktokv.com".getBytes(StandardCharsets.UTF_8));}catch(IllegalStateException expected){rejected=true;}check(rejected,"unknown binary with endpoints must fail instead of corrupting");
    }
    private static void staticFields() {
        ImmutableAnnotation annotation = new ImmutableAnnotation(1, "LKeepAnnotation;", Collections.singleton(
            new ImmutableAnnotationElement("value", new ImmutableStringEncodedValue("untouched"))));
        ImmutableField baseline = new ImmutableField("Lcom/bytedance/android/livesdk/livesetting/other/LivePipoMonitorHostSetting;",
            "DEFAULT", "Ljava/lang/String;", 25, new ImmutableStringEncodedValue("https://mon.tiktokv.com"),
            Collections.singleton(annotation), Collections.singleton(HiddenApiRestriction.WHITELIST));
        MutableField changed = new MutableField(baseline);
        check(LegacyPrivacy.rewriteStaticField(changed), "static tracking field not rewritten");
        check("https://blocked.invalid".equals(((StringEncodedValue)changed.getInitialValue()).getValue()), "static field URL wrong");
        check(changed.getDefiningClass().equals(baseline.getDefiningClass()) && changed.getName().equals(baseline.getName()) && changed.getType().equals(baseline.getType()), "static field identity changed");
        check(changed.getAccessFlags() == baseline.getAccessFlags(), "static field flags changed");
        check(changed.getAnnotations().equals(baseline.getAnnotations()), "static field annotations changed");
        check(changed.getHiddenApiRestrictions().equals(baseline.getHiddenApiRestrictions()), "static field restrictions changed");
        check(!LegacyPrivacy.rewriteStaticField(changed), "static field rewrite not idempotent");
        for (String value : Arrays.asList("https://log-boot.tiktokv.com", "https://api.tiktok.com", "normal text")) {
            MutableField safe = new MutableField(new ImmutableField("LTest;", "SAFE", "Ljava/lang/String;", 25,
                new ImmutableStringEncodedValue(value), Collections.singleton(annotation), Collections.<HiddenApiRestriction>emptySet()));
            check(!LegacyPrivacy.rewriteStaticField(safe), "safe field rewritten");
            check(value.equals(((StringEncodedValue)safe.getInitialValue()).getValue()), "safe field value changed");
        }
    }
    private static Document parse(String text, boolean namespaceAware) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance(); f.setNamespaceAware(namespaceAware);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static void manifest(boolean namespaceAware) throws Exception {
        String xml = "<manifest xmlns:android='http://schemas.android.com/apk/res/android' package='com.zhiliaoapp.musically'>" +
            "<uses-permission android:name='android.permission.INTERNET'/><uses-permission android:name='android.permission.POST_NOTIFICATIONS'/>" +
            "<uses-permission android:name='android.permission.ACCESS_LOCAL_NETWORK'/><uses-permission android:name='com.google.android.gms.permission.AD_ID'/>" +
            "<uses-permission-sdk-23 android:name='android.permission.PACKAGE_USAGE_STATS'/><uses-permission-sdk-m android:name='android.permission.QUERY_ALL_PACKAGES'/>" +
            "<permission android:name='com.zhiliaoapp.musically.permission.ATTRIBUTION_INFO'/><permission android:name='com.zhiliaoapp.musically.permission.PUSH'/>" +
            "<queries><intent><action android:name='android.intent.action.VIEW'/></intent><intent><action android:name='com.android.vending.INSTALL_REFERRER'/></intent><package android:name='com.android.chrome'/></queries>" +
            "<application android:name='.App' android:enableOnBackInvokedCallback='true'>" +
            "<activity android:name='.MainActivity'><meta-data android:name='com.facebook.ApplicationId' android:value='safe'/><meta-data android:name='slardar.analytics' android:value='track'/></activity>" +
            "<service android:name='.MessagingService'/><provider android:name='.FileProvider'/><receiver android:name='.PushReceiver'><intent-filter><action android:name='com.google.android.c2dm.intent.RECEIVE'/></intent-filter></receiver>" +
            "<service android:name='com.google.android.gms.measurement.AppMeasurementService'/><provider android:name='com.google.firebase.perf.provider.FirebasePerfProvider'/>" +
            "<receiver android:name='.InstallReporter'><intent-filter><action android:name='com.android.vending.INSTALL_REFERRER'/></intent-filter></receiver>" +
            "<meta-data android:name='com.google.android.gms.ads.APPLICATION_ID' android:value='track'/><property android:name='android.adservices.AD_SERVICES_CONFIG' android:resource='@xml/track'/>" +
            "<meta-data android:name='firebase_analytics_collection_enabled' android:value='true' android:resource='@bool/true'/>" +
            "<meta-data android:name='com.facebook.sdk.AutoLogAppEventsEnabled' android:value='true'/>" +
            "<meta-data android:name='normal.config' android:value='untouched'/></application></manifest>";
        Document d = parse(xml, namespaceAware);
        LegacyPrivacy.ManifestCounts count = LegacyPrivacy.hardenManifest(d);
        check(count.removed == 12, "manifest removal count=" + count.removed + " namespace=" + namespaceAware);
        check(count.flags == 12, "metadata flag count");
        StringWriter out = new StringWriter(); TransformerFactory.newInstance().newTransformer().transform(new DOMSource(d), new StreamResult(out));
        Document roundtrip = parse(out.toString(), namespaceAware);
        Set<String> actualNames = new HashSet<String>(); int falseFlags = 0;
        NodeList nodes = roundtrip.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element)nodes.item(i); String name = LegacyPrivacy.androidAttribute(e, "name"); actualNames.add(name);
            if ("meta-data".equals(e.getTagName()) && !name.equals("normal.config") && !name.equals("com.facebook.ApplicationId")) {
                check("false".equals(LegacyPrivacy.androidAttribute(e, "value")), "old true metadata survived: " + name);
                check(LegacyPrivacy.androidAttribute(e, "resource").isEmpty(), "resource metadata survived: " + name); falseFlags++;
            }
        }
        check(falseFlags == 12, "round-trip metadata not unique");
        for (String retained : Arrays.asList("android.permission.INTERNET", "android.permission.POST_NOTIFICATIONS", "com.zhiliaoapp.musically.permission.PUSH",
                ".MainActivity", ".MessagingService", ".FileProvider", ".PushReceiver", "com.google.android.c2dm.intent.RECEIVE", "com.android.chrome", "com.facebook.ApplicationId", "normal.config"))
            check(actualNames.contains(retained), "normal app/notification functionality removed: " + retained);
        for (String removed : Arrays.asList("android.permission.ACCESS_LOCAL_NETWORK", "com.google.android.gms.permission.AD_ID", "android.permission.PACKAGE_USAGE_STATS",
                "android.permission.QUERY_ALL_PACKAGES", "com.zhiliaoapp.musically.permission.ATTRIBUTION_INFO", "com.android.vending.INSTALL_REFERRER", "slardar.analytics",
                "com.google.android.gms.measurement.AppMeasurementService", "com.google.firebase.perf.provider.FirebasePerfProvider", ".InstallReporter",
                "com.google.android.gms.ads.APPLICATION_ID", "android.adservices.AD_SERVICES_CONFIG"))
            check(!actualNames.contains(removed), "tracking manifest entry retained: " + removed);
        Element app = (Element)roundtrip.getElementsByTagName("application").item(0);
        check("true".equals(LegacyPrivacy.androidAttribute(app, "enableOnBackInvokedCallback")), "other patch's manifest flag changed");
    }
}
