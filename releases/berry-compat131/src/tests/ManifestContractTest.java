import app.hiro.berryqueue.patches.BerryMessageQueuePatch;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Behavior contracts run against the implementation inside the shipped MPP. */
public final class ManifestContractTest {
    static final String NS="http://schemas.android.com/apk/res/android";
    static int assertions;
    static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static Document parse(String text)throws Exception{
        return parse(text,true);
    }
    static Document parse(String text,boolean namespaces)throws Exception{
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(namespaces);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }
    static String xml(Document d)throws Exception{StringWriter w=new StringWriter();TransformerFactory.newInstance().newTransformer().transform(new DOMSource(d),new StreamResult(w));return w.toString();}
    static Document fixture(String sdk,String extras,String children)throws Exception{
        return parse("<manifest xmlns:android='"+NS+"' package='jp.ejimax.berrybrowser' android:versionName='99.9'>"+sdk+extras
            +"<application android:name='.OriginalApplication' android:enableOnBackInvokedCallback='true' android:networkSecurityConfig='@xml/original'>"
            +"<activity android:name='.OriginalActivity' android:exported='true'/><service android:name='.KeepService' android:process=':keep'/>"+children+"</application></manifest>");
    }
    static Element first(Document d,String name){return(Element)d.getElementsByTagName(name).item(0);}
    static void assertOutput(Document d,String target,String minimum)throws Exception{
        check(target.equals(first(d,"uses-sdk").getAttributeNS(NS,"targetSdkVersion")),"target SDK");
        check(minimum.equals(first(d,"uses-sdk").getAttributeNS(NS,"minSdkVersion")),"minimum unchanged");
        Element app=first(d,"application");
        check(".OriginalApplication".equals(app.getAttributeNS(NS,"name")),"original Application retained");
        check("true".equals(app.getAttributeNS(NS,"enableOnBackInvokedCallback")),"back behavior explicit setting retained");
        check("@xml/original".equals(app.getAttributeNS(NS,"networkSecurityConfig")),"network security retained");
        check(d.getElementsByTagName("activity").getLength()==1&&d.getElementsByTagName("service").getLength()==1,"original components retained");
        check(d.getElementsByTagName("provider").getLength()==1,"exactly one helper provider");
        Element p=first(d,"provider");check("false".equals(p.getAttributeNS(NS,"exported"))&&"false".equals(p.getAttributeNS(NS,"grantUriPermissions")),"provider inaccessible externally");
        check("app.hiro.berry.runtime.BerryQueueInit".equals(p.getAttributeNS(NS,"name")),"correct runtime entry");
        check("jp.ejimax.berrybrowser.hiro.messagequeue".equals(p.getAttributeNS(NS,"authorities")),"unique authority");
        check(!p.hasAttributeNS(NS,"process"),"run in main app process");
        check(d.getElementsByTagName("uses-permission").getLength()==1,"one LAN permission");
        Element permission=first(d,"uses-permission");check("android.permission.ACCESS_LOCAL_NETWORK".equals(permission.getAttributeNS(NS,"name")),"LAN permission declaration");
        check(!permission.hasAttributeNS(NS,"maxSdkVersion"),"permission active on17");
        String once=xml(d);BerryMessageQueuePatch.applyManifest(d);check(once.equals(xml(d)),"idempotence");
    }
    static void rejected(Document d,String reason)throws Exception{
        String before=xml(d);boolean failed=false;try{BerryMessageQueuePatch.applyManifest(d);}catch(IllegalStateException expected){failed=true;}
        check(failed,"must reject "+reason);check(before.equals(xml(d)),"rejection is atomic: "+reason);
    }
    public static void main(String[]args)throws Exception{
        for(int target:new int[]{32,35,36,37,40}){
            Document d=fixture("<uses-sdk android:minSdkVersion='26' android:targetSdkVersion='"+target+"'/>","","");
            Document raw=parse(xml(d),false);BerryMessageQueuePatch.applyManifest(raw);
            assertOutput(parse(xml(raw)),Integer.toString(Math.max(37,target)),"26");
            BerryMessageQueuePatch.applyManifest(d);assertOutput(d,Integer.toString(Math.max(37,target)),"26");
        }
        Document absent=fixture("","","");BerryMessageQueuePatch.applyManifest(absent);assertOutput(absent,"37","");
        Document missing=fixture("<uses-sdk android:minSdkVersion='38'/>","","");BerryMessageQueuePatch.applyManifest(missing);assertOutput(missing,"38","38");
        Document limited=fixture("<uses-sdk android:minSdkVersion='32' android:targetSdkVersion='37'/>","<uses-permission android:name='android.permission.ACCESS_LOCAL_NETWORK' android:maxSdkVersion='36'/>","");
        BerryMessageQueuePatch.applyManifest(limited);assertOutput(limited,"37","32");
        Document wrong=fixture("","","");wrong.getDocumentElement().setAttribute("package","other.app");rejected(wrong,"wrong package");
        rejected(fixture("<uses-sdk android:targetSdkVersion='Future'/>","",""),"preview SDK");
        rejected(fixture("<uses-sdk android:targetSdkVersion='37' android:maxSdkVersion='36'/>","",""),"maxSdk excludes17");
        rejected(fixture("<uses-sdk/><uses-sdk/>","",""),"duplicate SDK");
        rejected(fixture("","","<provider android:name='.AnotherProvider' android:authorities='jp.ejimax.berrybrowser.hiro.messagequeue'/ >".replace("/ >","/>")),"authority collision");
        rejected(fixture("","","<provider android:name='app.hiro.berry.runtime.BerryQueueInit' android:authorities='wrong' android:exported='true'/>"),"conflicting provider");
        rejected(fixture("","","<provider android:name='app.hiro.berry.runtime.BerryQueueInit' android:authorities='jp.ejimax.berrybrowser.hiro.messagequeue' android:exported='false' android:grantUriPermissions='false' android:enabled='false'/>"),"disabled provider");
        System.out.println("PASS ManifestContractTest: "+assertions+" assertions; SDK upgrade/no downgrade, original settings, LAN/provider, idempotence, atomic invalid-input rejection");
    }
}
