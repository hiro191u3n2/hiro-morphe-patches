package app.hiro.ulike.patches;
import java.io.*;
import java.nio.file.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
public class IntegrationPayloadIndependentTest {
 public static void main(String[] args)throws Exception {
  for(boolean aware:new boolean[]{false,true}) {
   DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(aware);
   Document d=f.newDocumentBuilder().parse(new ByteArrayInputStream("<manifest xmlns:android='http://schemas.android.com/apk/res/android'><application android:extractNativeLibs='false'/></manifest>".getBytes("UTF-8")));
   IntegrationPayload169.requireManifest(d,true);
   StringWriter w=new StringWriter();TransformerFactory.newInstance().newTransformer().transform(new DOMSource(d),new StreamResult(w));
   
   DocumentBuilderFactory parse=DocumentBuilderFactory.newInstance();parse.setNamespaceAware(true);
   Document fresh=parse.newDocumentBuilder().parse(new ByteArrayInputStream(w.toString().getBytes("UTF-8")));
   if(!fresh.getElementsByTagName("application").item(0).getAttributes().getNamedItemNS("http://schemas.android.com/apk/res/android","extractNativeLibs").getNodeValue().equals("true"))throw new AssertionError("Native extraction value lost on serialization");
   if(d.getElementsByTagName("application").item(0).getAttributes().getLength()!=1)throw new AssertionError("Conflicting duplicate DOM attribute");
   System.out.println("Roundtrip namespaceAware="+aware+" PASS attrs="+d.getElementsByTagName("application").item(0).getAttributes().getLength());
  }
  Path p=Files.createTempFile("morphe-document-",".xml");
  try {
   Files.write(p,"<manifest xmlns:android='http://schemas.android.com/apk/res/android'><application android:extractNativeLibs='false'/></manifest>".getBytes("UTF-8"));
   try(app.morphe.patcher.util.Document d=new app.morphe.patcher.util.Document(p.toFile())){IntegrationPayload169.requireManifest(d,true);}
   DocumentBuilderFactory parse=DocumentBuilderFactory.newInstance();parse.setNamespaceAware(true);Document d=parse.newDocumentBuilder().parse(p.toFile());
   if(!d.getElementsByTagName("application").item(0).getAttributes().getNamedItemNS("http://schemas.android.com/apk/res/android","extractNativeLibs").getNodeValue().equals("true"))throw new AssertionError();
   System.out.println("Actual Morphe Document close/reparse PASS");
  }finally{Files.deleteIfExists(p);}
 }
}
