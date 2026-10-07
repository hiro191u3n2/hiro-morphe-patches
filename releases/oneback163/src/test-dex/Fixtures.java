import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.value.ValueType;

/** Original synthetic APK containers for real Morphe resource+DEX integration tests. */
public final class Fixtures {
    private static final String NS="http://schemas.android.com/apk/res/android";
    private static final List<String> PACKAGES=Arrays.asList("com.ss.android.ugc.trill","com.zhiliaoapp.musically","com.instagram.android","com.twitter.android","ctrip.english");
    // Public SDK 36 android.R.attr values, read from the build's android.jar.
    private static final int NAME=16842755,LABEL=16842753,VERSION_CODE=16843291,VERSION_NAME=16843292,
        MIN_SDK=16843276,TARGET_SDK=16843376,EXPORTED=16842768,EXCLUDE=16842775,TASK_AFFINITY=16842770,
        ENABLED=16842766,TARGET_ACTIVITY=16843266,CALLBACK=16844396;
    private static int assertions;
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}
    private static ResXmlAttribute android(ResXmlElement element,String name,int id){return element.getOrCreateAndroidAttribute(name,id);}
    private static void string(ResXmlElement element,String name,int id,String value){android(element,name,id).setValueAsString(value);}
    private static void integer(ResXmlElement element,String name,int id,int value){ResXmlAttribute a=android(element,name,id);a.setValueType(ValueType.DEC);a.setData(value);}
    private static void bool(ResXmlElement element,String name,int id,boolean value){android(element,name,id).setValueAsBoolean(value);}
    private static AndroidManifestBlock manifest(String packageName){
        AndroidManifestBlock manifest=new AndroidManifestBlock();ResXmlElement root=manifest.newElement("manifest");
        root.getOrCreateNamespace(NS,"android");ResXmlAttribute pkg=root.getOrCreateAndroidAttribute("package",0);pkg.setNamespace(null,null);pkg.setValueAsString(packageName);
        integer(root,"versionCode",VERSION_CODE,999);string(root,"versionName",VERSION_NAME,"999.0");
        ResXmlElement sdk=root.newElement("uses-sdk");integer(sdk,"minSdkVersion",MIN_SDK,26);integer(sdk,"targetSdkVersion",TARGET_SDK,36);
        ResXmlElement permission=root.newElement("uses-permission");string(permission,"name",NAME,"android.permission.CAMERA");
        ResXmlElement app=root.newElement("application");string(app,"label",LABEL,"Original synthetic OneBack fixture");bool(app,"enableOnBackInvokedCallback",CALLBACK,false);
        ResXmlElement activity=app.newElement("activity");string(activity,"name",NAME,"fixture.EmptyActivity");bool(activity,"exported",EXPORTED,true);bool(activity,"enableOnBackInvokedCallback",CALLBACK,false);
        bool(activity,"excludeFromRecents",EXCLUDE,false);string(activity,"taskAffinity",TASK_AFFINITY,packageName+".original");
        ResXmlElement intent=activity.newElement("intent-filter");string(intent.newElement("action"),"name",NAME,"android.intent.action.MAIN");string(intent.newElement("category"),"name",NAME,"android.intent.category.LAUNCHER");
        ResXmlElement second=app.newElement("activity");string(second,"name",NAME,"fixture.ChildActivity");bool(second,"exported",EXPORTED,false);
        ResXmlElement alias=app.newElement("activity-alias");string(alias,"name",NAME,"fixture.Alias");string(alias,"targetActivity",TARGET_ACTIVITY,"fixture.EmptyActivity");bool(alias,"exported",EXPORTED,false);
        ResXmlElement service=app.newElement("service");string(service,"name",NAME,"fixture.Unrelated");bool(service,"enabled",ENABLED,true);bool(service,"exported",EXPORTED,false);
        manifest.refreshFull();return manifest;
    }
    private static String serialize(AndroidManifestBlock manifest)throws Exception{StringWriter out=new StringWriter();XmlSerializer serializer=XmlPullParserFactory.newInstance().newSerializer();serializer.setOutput(out);manifest.serialize(serializer,false);serializer.flush();return out.toString();}
    private static byte[] bytes(ZipFile apk,String name)throws Exception{ZipEntry entry=apk.getEntry(name);check(entry!=null,"APK entry missing: "+name);try(InputStream in=apk.getInputStream(entry);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[16384];int count;while((count=in.read(buffer))>=0)out.write(buffer,0,count);return out.toByteArray();}}
    private static void put(ZipOutputStream zip,String name,byte[] value)throws Exception{ZipEntry entry=new ZipEntry(name);entry.setTime(0);zip.putNextEntry(entry);zip.write(value);zip.closeEntry();}
    private static void create(String dexPath,String outPath)throws Exception{
        Path out=Paths.get(outPath);Files.createDirectories(out);byte[] dex=Files.readAllBytes(Paths.get(dexPath));check(dex.length>8&&dex[0]=='d'&&dex[1]=='e'&&dex[2]=='x',"not a fixture DEX");
        for(String pkg:PACKAGES){AndroidManifestBlock m=manifest(pkg);TableBlock table=new TableBlock();PackageBlock resources=table.newPackage(0x7f,pkg);com.reandroid.arsc.value.Entry marker=resources.getOrCreateTypeBlock("","string").getOrCreateEntry(0);marker.setValueAsString("original-oneback-fixture");marker.setName("fixture_marker");
            // Morphe's full resource coder expects an ids.xml even when no patch adds IDs.
            com.reandroid.arsc.value.Entry markerId=resources.getOrCreateTypeBlock("","id").getOrCreateEntry(0);markerId.setValueAsBoolean(false);markerId.setName("fixture_id");table.refreshFull();
            try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(out.resolve(pkg+".apk")))){put(zip,"AndroidManifest.xml",m.getBytes());put(zip,"resources.arsc",table.getBytes());put(zip,"classes.dex",dex);put(zip,"assets/fixture-marker.txt","original-oneback-fixture\n".getBytes(StandardCharsets.UTF_8));}
            Files.write(out.resolve(pkg+".manifest.xml"),serialize(m).getBytes(StandardCharsets.UTF_8));
            try(ZipFile read=new ZipFile(out.resolve(pkg+".apk").toFile())){AndroidManifestBlock loaded=AndroidManifestBlock.load(new ByteArrayInputStream(bytes(read,"AndroidManifest.xml")));
                check(pkg.equals(loaded.getPackageName()),"binary manifest package roundtrip");check("999.0".equals(loaded.getVersionName())&&Integer.valueOf(999).equals(loaded.getVersionCode()),"binary manifest version roundtrip");check(Integer.valueOf(36).equals(loaded.getTargetSdkVersion()),"binary target SDK roundtrip");
                check(!loaded.getApplicationElement().searchAttributeByName("enableOnBackInvokedCallback").getValueAsBoolean(),"original callback starts false");check(Arrays.equals(dex,bytes(read,"classes.dex")),"fixture DEX bytes preserved in APK");}
            System.out.println("CREATED "+out.resolve(pkg+".apk"));
        }
        System.out.println("PASS synthetic APK creation: packages="+PACKAGES.size()+"; assertions="+assertions);
    }
    private static String tree(ResXmlElement element,boolean stripCallback){
        StringBuilder out=new StringBuilder("<").append(element.getName());SortedMap<String,String> attributes=new TreeMap<String,String>();
        for(Iterator<ResXmlAttribute> it=element.getAttributes();it.hasNext();){ResXmlAttribute a=it.next();if(stripCallback&&("application".equals(element.getName())||"activity".equals(element.getName()))&&a.getNameId()==CALLBACK)continue;
            // aapt2 can add the platform/compiler version on the root when compiling a resource patch.
            if("manifest".equals(element.getName())&&Arrays.asList("platformBuildVersionCode","platformBuildVersionName","compileSdkVersion","compileSdkVersionCodename").contains(a.getName()))continue;
            attributes.put(String.valueOf(a.getUri())+"/"+a.getName()+"/"+a.getNameId(),a.getValueType()+"/"+a.decodeValue());}
        out.append(attributes).append('>');for(Iterator<ResXmlElement> it=element.getElements();it.hasNext();)out.append(tree(it.next(),stripCallback));return out.append("</").append(element.getName()).append('>').toString();
    }
    private static void verify(String originalPath,String patchedPath,String report)throws Exception{
        try(ZipFile before=new ZipFile(originalPath);ZipFile after=new ZipFile(patchedPath)){
            AndroidManifestBlock original=AndroidManifestBlock.load(new ByteArrayInputStream(bytes(before,"AndroidManifest.xml"))),patched=AndroidManifestBlock.load(new ByteArrayInputStream(bytes(after,"AndroidManifest.xml")));
            String pkg=original.getPackageName();check(PACKAGES.contains(pkg)&&pkg.equals(patched.getPackageName()),"fixture package preserved");
            check("999.0".equals(patched.getVersionName())&&Integer.valueOf(999).equals(patched.getVersionCode()),"fixture version preserved");
            check(Integer.valueOf(36).equals(patched.getTargetSdkVersion())&&Integer.valueOf(26).equals(patched.getMinSdkVersion()),"SDK requirements preserved");
            ResXmlElement app=patched.getApplicationElement();check(app.searchAttributeByName("enableOnBackInvokedCallback").getValueAsBoolean(),"application callback enabled");int activities=0;
            for(Iterator<ResXmlElement> it=app.getElements("activity");it.hasNext();){ResXmlElement activity=it.next();ResXmlAttribute callback=activity.searchAttributeByName("enableOnBackInvokedCallback");check(callback!=null&&callback.getValueAsBoolean()&&callback.getNameId()==CALLBACK,"every Activity opts into the correct framework Back attribute");activities++;}
            check(activities==2,"fixture Activity count preserved");check(tree(original.getManifestElement(),true).equals(tree(patched.getManifestElement(),true)),"unexpected manifest change outside Back callback flags and compiler metadata");
            check(Arrays.equals(bytes(before,"assets/fixture-marker.txt"),bytes(after,"assets/fixture-marker.txt")),"unrelated fixture asset preserved");
            TableBlock oldTable=TableBlock.load(new ByteArrayInputStream(bytes(before,"resources.arsc"))),newTable=TableBlock.load(new ByteArrayInputStream(bytes(after,"resources.arsc")));
            check(newTable.getResource(pkg,"string","fixture_marker")!=null,"original fixture resource retained");
            check(oldTable.getResource(pkg,"string","fixture_marker").getResourceId()==newTable.getResource(pkg,"string","fixture_marker").getResourceId(),"original fixture resource ID preserved");
            String json="{\"result\":\"PASS\",\"blocking_findings\":[],\"package\":\""+pkg+"\",\"version\":\"999.0\",\"activity_callback_flags\":"+activities+",\"assertions\":"+assertions+",\"permissions_flags_assets_and_resource_ids_preserved\":true,\"physical_device_tested\":false}\n";
            Files.write(Paths.get(report),json.getBytes(StandardCharsets.UTF_8));System.out.println("PASS emitted fixture manifest/resources: "+pkg+"; assertions="+assertions);
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length==3&&"create".equals(args[0])){create(args[1],args[2]);return;}
        if(args.length==4&&"verify".equals(args[0])){verify(args[1],args[2],args[3]);return;}
        throw new IllegalArgumentException("Fixtures create FIXTURE_ORIGINAL_DEX OUTPUT_DIRECTORY | verify ORIGINAL_APK PATCHED_APK REPORT_JSON");
    }
}
