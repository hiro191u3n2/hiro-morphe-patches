package app.hiro.yahootransit.patches;

import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.resource.coder.ResourceCoder;
import app.morphe.patcher.util.Document;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Real Morphe Document IO with a test-only directory ResourceCoder, not an Android emulator. */
public final class NearMeLayoutTest {
    static final String NS="http://schemas.android.com/apk/res/android";
    static final String VIEW="jp.co.yahoo.android.apps.transit.ui.compose.components.nearme.NearMeView";
    static final String RESULTS="jp.co.yahoo.android.apps.transit.ui.view.navi.list.SearchResultListView";
    static final String BUTTONS="jp.co.yahoo.android.apps.transit.ui.view.navi.SearchBtnView";
    static final String MARKER="hiro_nearme_pr_hidden_v1";
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void write(Path path,String value)throws Exception{Files.createDirectories(path.getParent());Files.write(path,value.getBytes(StandardCharsets.UTF_8));}
    static String xml(String id){return "<LinearLayout xmlns:android=\""+NS+"\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"+
        "<"+RESULTS+" android:id=\"@+id/results\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"+
        "<TextView android:id=\"@+id/ordinary_content\" android:text=\"normal content\"/>"+
        "<"+VIEW+" android:id=\""+id+"\" android:visibility=\"visible\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"+
        "<"+BUTTONS+" android:id=\"@+id/buttons\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"+
        "</LinearLayout>";}
    static ResourcePatchContext context(Path directory)throws Exception{
        // Only the directory-backed resource provider is replaced. Production context and Document run unchanged.
        Field singleton=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");singleton.setAccessible(true);
        Object unsafe=singleton.get(null);
        ResourcePatchContext context=(ResourcePatchContext)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,ResourcePatchContext.class);
        ResourceCoder coder=(ResourceCoder)Proxy.newProxyInstance(ResourceCoder.class.getClassLoader(),new Class<?>[]{ResourceCoder.class},(proxy,method,args)->{
            if(method.getName().equals("getFile"))return directory.resolve((String)args[0]).toFile();
            if(method.getName().equals("close"))return null;
            throw new UnsupportedOperationException(method.getName());
        });
        Field field=ResourcePatchContext.class.getDeclaredField("resourceCoder");field.setAccessible(true);field.set(context,coder);return context;
    }
    static Path fixture(Path root,String name,String version,String id)throws Exception{
        Path directory=root.resolve(name);write(directory.resolve("AndroidManifest.xml"),"<manifest xmlns:android=\""+NS+"\" package=\"jp.co.yahoo.android.apps.transit\" android:versionName=\""+version+"\"/>");
        write(directory.resolve("res/layout/fragment_search_result_list.xml"),xml(id));return directory;
    }
    static String fingerprint(Node node){
        StringBuilder out=new StringBuilder(node.getNodeName());
        if(node.getAttributes()!=null){List<String> attributes=new ArrayList<>();for(int i=0;i<node.getAttributes().getLength();i++){Node a=node.getAttributes().item(i);attributes.add(a.getNodeName()+"="+a.getNodeValue());}java.util.Collections.sort(attributes);out.append(attributes);}
        for(Node child=node.getFirstChild();child!=null;child=child.getNextSibling())if(child instanceof Element)out.append(fingerprint(child));else if(child.getNodeType()==Node.TEXT_NODE&&!child.getNodeValue().trim().isEmpty())out.append(child.getNodeValue().trim());
        return out.toString();
    }
    static List<String> siblings(Path file)throws Exception{try(Document d=new Document(file.toFile())){List<String> out=new ArrayList<>();for(Node n=d.getDocumentElement().getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element&&!((Element)n).getTagName().equals(VIEW)&&!MARKER.equals(attribute((Element)n,"tag")))out.add(fingerprint(n));return out;}}
    static String attribute(Element e,String name){String value=e.getAttributeNS(NS,name);return value.isEmpty()?e.getAttribute("android:"+name):value;}
    static void verify(Path file,String id)throws Exception{try(Document d=new Document(file.toFile())){
        check(d.getElementsByTagName(VIEW).getLength()==1,"one retained NearMe view");Element view=(Element)d.getElementsByTagName(VIEW).item(0);Element parent=(Element)view.getParentNode();
        check(id.equals(attribute(view,"id")),"original binding id retained");
        check("visible".equals(attribute(view,"visibility")),"original View visibility retained");
        check(parent.getTagName().equals("FrameLayout")&&MARKER.equals(attribute(parent,"tag")),"dedicated hidden parent");
        check(!!attribute(parent,"id").isEmpty(),"wrapper has no binding id");
        for(String a:new String[]{"layout_height","layout_width","layout_margin","padding","paddingTop","paddingBottom"})check("0dp".equals(attribute(parent,a)),"collapsed "+a);
        check("gone".equals(attribute(parent,"visibility"))&&"0".equals(attribute(parent,"alpha")),"hidden even if child redisplays");
        check(d.getElementsByTagName("FrameLayout").getLength()==1,"one wrapper only");
    }}
    static void reject(Path directory,String name)throws Exception{boolean threw=false;try{NearMeLayout.apply(context(directory));}catch(IllegalStateException expected){threw=true;}check(threw,"reject "+name);}
    public static void main(String[] args)throws Exception{
        Path root=new File(args[0]).toPath();Files.createDirectories(root);
        String[] versions={"8.4.1","8.5.0","8.6.0"};String[] ids={"@id/near_me","@+id/near_me","@id/renamed_ad_123"};
        for(int i=0;i<versions.length;i++){
            Path dir=fixture(root,"v"+versions[i],versions[i],ids[i]);Path file=dir.resolve("res/layout/fragment_search_result_list.xml");List<String> before=siblings(file);
            Path landscape=dir.resolve("res/layout-land/fragment_search_result_list.xml");write(landscape,xml(ids[i]));
            Path night=dir.resolve("res/layout-night/fragment_search_result_list.xml");write(night,xml(ids[i]));
            Path station=dir.resolve("res/layout/fragment_stationinfo.xml");write(station,"<FrameLayout xmlns:android=\""+NS+"\"><"+VIEW+" android:id=\"@id/station_ad\"/></FrameLayout>");byte[] stationBytes=Files.readAllBytes(station);
            NearMeLayout.apply(context(dir));verify(file,ids[i]);verify(landscape,ids[i]);verify(night,ids[i]);check(before.equals(siblings(file)),"normal content and result/button siblings unchanged");
            check(java.util.Arrays.equals(stationBytes,Files.readAllBytes(station)),"other screen unchanged");
            String first;try(Document d=new Document(file.toFile())){first=fingerprint(d.getDocumentElement());}
            NearMeLayout.apply(context(dir));verify(file,ids[i]);try(Document d=new Document(file.toFile())){check(first.equals(fingerprint(d.getDocumentElement())),"idempotent semantic resource output");}
        }
        Path bad=fixture(root,"duplicate","8.6.0","@id/new_name");Path file=bad.resolve("res/layout/fragment_search_result_list.xml");write(file,xml("@id/x").replace("</LinearLayout>","<"+VIEW+" android:id=\"@id/duplicate\"/></LinearLayout>"));reject(bad,"duplicate view");
        bad=fixture(root,"missing","8.6.0","@id/x");file=bad.resolve("res/layout/fragment_search_result_list.xml");write(file,"<LinearLayout/>");reject(bad,"missing view");
        bad=fixture(root,"missing-id","8.6.0","");reject(bad,"missing binding id");
        bad=fixture(root,"wrong-parent","8.6.0","@id/x");file=bad.resolve("res/layout/fragment_search_result_list.xml");write(file,xml("@id/x").replace("LinearLayout","FrameLayout"));reject(bad,"wrong parent");
        bad=fixture(root,"nested-buttons","8.6.0","@id/x");file=bad.resolve("res/layout/fragment_search_result_list.xml");write(file,xml("@id/x").replace("<"+BUTTONS,"<FrameLayout><"+BUTTONS).replace("</LinearLayout>","</FrameLayout></LinearLayout>"));reject(bad,"button outside direct topology");
        bad=fixture(root,"wrong-package","8.6.0","@id/x");write(bad.resolve("AndroidManifest.xml"),"<manifest package=\"other.app\"/>");reject(bad,"wrong package");
        bad=fixture(root,"invalid-variant","8.6.0","@id/x");file=bad.resolve("res/layout/fragment_search_result_list.xml");String before;try(Document d=new Document(file.toFile())){before=fingerprint(d.getDocumentElement());}write(bad.resolve("res/layout-land/fragment_search_result_list.xml"),"<LinearLayout/>");reject(bad,"invalid alternate configuration");try(Document d=new Document(file.toFile())){check(before.equals(fingerprint(d.getDocumentElement())),"preflight avoids partial edits");}
        bad=fixture(root,"missing-base","8.6.0","@id/x");Files.delete(bad.resolve("res/layout/fragment_search_result_list.xml"));reject(bad,"missing base layout");
        System.out.println("PASS NearMeLayoutTest: "+checks+" assertions; versions 8.4.1/8.5.0/8.6.0, renamed IDs, 3 layout configurations, idempotence, persistence, sibling preservation and invalid-input preflight. Directory-backed ResourceCoder; real Morphe ResourcePatchContext and Document. No physical Android test.");
    }
}
