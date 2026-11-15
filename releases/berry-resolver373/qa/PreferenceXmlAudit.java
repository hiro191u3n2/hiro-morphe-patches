import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import java.util.*;
import java.util.zip.*;

/** Parse original/rebuilt binary XML using Morphe's own ARSCLib and prove
 * all existing preference elements and namespaced attributes are retained. */
public final class PreferenceXmlAudit {
    static final String URI="http://schemas.android.com/apk/res/android";
    static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
    static ResXmlDocument read(String apk)throws Exception {
        try(ZipFile z=new ZipFile(apk)){
            ResXmlDocument d=new ResXmlDocument();
            d.readBytes(z.getInputStream(z.getEntry("res/xml/pref_action.xml")));
            need("PreferenceScreen".equals(d.getDocumentElement().getName()),"wrong root");return d;
        }
    }
    static boolean zoom(ResXmlElement e){
        ResXmlAttribute key=e.searchAttributeByResourceId(0x010101e8);
        return key!=null&&"hiro_one_finger_zoom".equals(key.getDataAsString());
    }
    static String tree(ResXmlElement e,boolean skip){
        StringBuilder out=new StringBuilder(e.getName()).append('{');List<String> attrs=new ArrayList<>();
        Iterator<?> a=e.getAttributes();while(a.hasNext()){
            ResXmlAttribute x=(ResXmlAttribute)a.next();
            String type=x.getDataType().toString();
            String value=type.equals("STRING")?x.getDataAsString():Integer.toString(x.getData());
            attrs.add(x.decodeUri()+"|"+x.getName()+"|"+x.getNameId()+"|"+type+"|"+value);
        }
        Collections.sort(attrs);out.append(attrs).append('}');Iterator<?> children=e.getElements();
        while(children.hasNext()){ResXmlElement c=(ResXmlElement)children.next();if(!(skip&&zoom(c)))out.append('[').append(tree(c,skip)).append(']');}
        return out.toString();
    }
    public static void main(String[] a)throws Exception {
        ResXmlDocument old=read(a[0]),fresh=read(a[1]);
        need(tree(old.getDocumentElement(),false).equals(tree(fresh.getDocumentElement(),true)),"existing preference tree changed");
        int count=0;Iterator<?> it=fresh.recursiveElements();while(it.hasNext()){
            ResXmlElement e=(ResXmlElement)it.next();if(!zoom(e))continue;count++;
            need("androidx.preference.SwitchPreferenceCompat".equals(e.getName()),"wrong zoom type");
            ResXmlAttribute key=e.searchAttributeByResourceId(0x010101e8),enabled=e.searchAttributeByResourceId(0x010101ed);
            need(URI.equals(key.decodeUri())&&URI.equals(enabled.decodeUri()),"wrong Android namespace");
            need(enabled.getDataAsBoolean(),"zoom default is not true");
        }
        need(count==1,"not exactly one zoom preference");
        System.out.println("PASS ARSCLib binary XML parse; existing preference tree and namespaces preserved; exactly one enabled zoom switch");
    }
}
