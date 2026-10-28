import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.reandroid.arsc.chunk.xml.*;
import com.reandroid.arsc.value.ValueType;

/** Independent emitted APK manifest comparison. No Android execution is claimed. */
public final class InstagramManifestAudit {
    private static final int CALLBACK=0x0101066c;
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static String tree(ResXmlElement element) {
        StringBuilder out=new StringBuilder("<").append(element.getName());
        SortedMap<String,String> attrs=new TreeMap<>();
        for(Iterator<ResXmlAttribute> it=element.getAttributes();it.hasNext();) {
            ResXmlAttribute a=it.next();
            if(("application".equals(element.getName())||"activity".equals(element.getName()))
                    &&a.getNameId()==CALLBACK)continue;
            if("manifest".equals(element.getName())&&Arrays.asList("platformBuildVersionCode",
                    "platformBuildVersionName","compileSdkVersion","compileSdkVersionCodename").contains(a.getName()))continue;
            String key=String.valueOf(a.getUri())+"/"+a.getName()+"/"+a.getNameId();
            check(!attrs.containsKey(key),"duplicate manifest attribute");
            // Compare resource IDs as typed integers; resolving their names
            // would require a resource table and could conceal a changed ID.
            attrs.put(key,a.getValueType()+"/"+(a.getValueType()==ValueType.STRING
                    ?a.getValueAsString():Integer.toUnsignedString(a.getData())));
        }
        out.append(attrs).append('>');
        for(Iterator<ResXmlElement> it=element.getElements();it.hasNext();)out.append(tree(it.next()));
        return out.append("</").append(element.getName()).append('>').toString();
    }
    private static AndroidManifestBlock manifest(String path)throws Exception {
        try(ZipFile zip=new ZipFile(path);InputStream in=zip.getInputStream(zip.getEntry("AndroidManifest.xml"))) {
            return AndroidManifestBlock.load(in);
        }
    }
    private static void callback(ResXmlElement element) {
        ResXmlAttribute value=element.searchAttributeByName("enableOnBackInvokedCallback");
        check(value!=null&&value.getNameId()==CALLBACK&&value.getValueAsBoolean(),
                "correct framework Back opt-in on "+element.getName());
    }
    public static void main(String[] args)throws Exception {
        check(args.length==2,"ORIGINAL_APK PATCHED_APK");
        AndroidManifestBlock before=manifest(args[0]),after=manifest(args[1]);
        for(AndroidManifestBlock m:Arrays.asList(before,after)) {
            check("com.instagram.android".equals(m.getPackageName()),"exact package");
            check("452.1.0.53.64".equals(m.getVersionName()),"exact version");
            check(Integer.valueOf(385811006).equals(m.getVersionCode()),"exact versionCode");
            check(Integer.valueOf(37).equals(m.getTargetSdkVersion()),"target SDK retained");
        }
        ResXmlElement app=after.getApplicationElement();callback(app);int count=0;
        for(Iterator<ResXmlElement> it=app.getElements("activity");it.hasNext();){callback(it.next());count++;}
        check(count>0,"activity inventory exists");
        check(tree(before.getManifestElement()).equals(tree(after.getManifestElement())),
                "only Back opt-in and compiler metadata may change");
        System.out.println("PASS InstagramManifestAudit activities="+count+
                "; package/version/SDK/permissions/components/intent filters/task flags retained");
    }
}
