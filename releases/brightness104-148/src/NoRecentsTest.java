import app.hiro.brightnessclick.NoRecents;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public final class NoRecentsTest {
    static int checks;
    static final String NS="http://schemas.android.com/apk/res/android";
    static final String PKG="jp.gr.java_conf.fimyulab.brightnessclick";
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    static Document parse(String xml, boolean aware)throws Exception{
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(aware);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }
    static String fixture(String prefix,String existing){return
        "<manifest xmlns:android='"+NS+"' package='"+PKG+"' android:versionCode='10' android:versionName='1.5'>"+
        "<uses-sdk android:minSdkVersion='23' android:targetSdkVersion='28'/>"+
        "<uses-permission android:name='android.permission.WRITE_SETTINGS'/>"+
        "<application android:label='明るさタッチ'><activity android:name='"+prefix+"MainActivity' android:exported='false' "+existing+"/>"+
        "<activity android:name='"+prefix+"BlankActivity' android:exported='false' android:theme='@android:style/Theme.Translucent.NoTitleBar'/>"+
        "<activity android:name='"+prefix+"PermissionActivity' android:exported='true'><intent-filter>"+
        "<action android:name='android.intent.action.MAIN'/><category android:name='android.intent.category.LAUNCHER'/>"+
        "</intent-filter></activity><service android:name='.UnchangedService'/><receiver android:name='.UnchangedReceiver'/>"+
        "</application></manifest>";}
    public static void main(String[] args)throws Exception{
        for(boolean aware:new boolean[]{true,false})for(String prefix:new String[]{"", ".",PKG+"."})
        for(String existing:new String[]{"", "android:excludeFromRecents='false'", "android:excludeFromRecents='true'"}){
            Document d=parse(fixture(prefix,existing),aware);Document baseline=(Document)d.cloneNode(true);
            Element app=(Element)d.getElementsByTagName("application").item(0);
            NoRecents.apply(app);NodeList all=app.getElementsByTagName("activity");check(all.getLength()==3,"All three activities retained");
            for(int i=0;i<all.getLength();i++){
                Element e=(Element)all.item(i);check("true".equals(e.getAttributeNS(NS,"excludeFromRecents")),"Namespaced excludeFromRecents true");
                check(!e.hasAttribute("android:noHistory"),"No noHistory side effect");
                check(!e.hasAttribute("android:finishOnTaskLaunch"),"No task-killing side effect");
            }
            Document once=(Document)d.cloneNode(true);NoRecents.apply(app);check(d.isEqualNode(once),"Idempotent");
            NodeList before=baseline.getElementsByTagName("activity");
            for(int i=0;i<all.getLength();i++){
                Element a=(Element)all.item(i), b=(Element)before.item(i);a.removeAttribute("android:excludeFromRecents");b.removeAttribute("android:excludeFromRecents");
            }
            check(d.isEqualNode(baseline),"Only recents attribute changed; themes, intents, permission and components preserved");
        }
        Document d=parse(fixture(".",""),true);Element app=(Element)d.getElementsByTagName("application").item(0);
        app.removeChild(app.getElementsByTagName("activity").item(2));Document missing=(Document)d.cloneNode(true);
        try{NoRecents.apply(app);throw new AssertionError("Missing activity accepted");}catch(IllegalStateException expected){checks++;}
        check(d.isEqualNode(missing),"Validation failure makes no edits");
        d.getDocumentElement().setAttribute("package","unrelated.app");
        try{NoRecents.apply(app);throw new AssertionError("Wrong app accepted");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("PASS "+checks+" manifest assertions; host JVM, not Android device execution");
    }
}
