package app.hiro.ulike.patches;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/** Real pinned package bytes, patcher resource fault tests; no native load. */
public final class IntegrationPayload169Test {
    private static int checks;
    private static Path payload, temporary;
    interface Checked {void run()throws Exception;}
    static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    static void rejects(Checked r,String reason)throws Exception{boolean rejected=false;try{r.run();}catch(IOException expected){rejected=true;}check(rejected,reason);}
    static Path workspace(String name)throws Exception{return Files.createDirectory(temporary.resolve(name));}
    static void install(Path root,IntegrationPayload169.Payload loader)throws Exception{
        IntegrationPayload169.installFiles(root.toFile(),s->root.resolve(s).toFile(),loader);
    }
    static InputStream resource(String name)throws Exception{return Files.newInputStream(payload.resolve(name));}
    static long regularFiles(Path root)throws Exception{try(java.util.stream.Stream<Path> files=Files.walk(root)){return files.filter(Files::isRegularFile).count();}}
    static Document manifest(String body,boolean namespace)throws Exception{
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(namespace);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(("<manifest xmlns:android='http://schemas.android.com/apk/res/android' package='com.example'>"+body+"</manifest>").getBytes("UTF-8")));
    }
    public static void main(String[] args)throws Exception {
        payload=Paths.get(args[0]);temporary=Files.createTempDirectory("ulike-runtime-qa-");
        try {
            Path clean=workspace("clean");install(clean,IntegrationPayload169Test::resource);
            check(regularFiles(clean)==6,"six files installed, no stage leftovers");
            for(String line:Files.readAllLines(payload.resolve("resourceitems.tsv"))){String[] fields=line.split("\t");
                check(IntegrationPayload169.digest(clean.resolve(fields[0])).equals(fields[1]),"installed exact hash");
                check(Files.size(clean.resolve(fields[0]))==Long.parseLong(fields[2]),"installed exact length");
            }
            install(clean,s->{throw new AssertionError("Idempotency must not read resource");});check(regularFiles(clean)==6,"idempotent install");
            Path collision=workspace("collision");Files.createDirectories(collision.resolve("assets/ulike169"));Path existing=collision.resolve("assets/ulike169/onnxruntime-LICENSE.txt");Files.write(existing,new byte[]{8,9});
            rejects(()->install(collision,IntegrationPayload169Test::resource),"existing conflicting file rejected");
            check(regularFiles(collision)==1&&Arrays.equals(Files.readAllBytes(existing),new byte[]{8,9}),"conflicting original preserved and stages cleaned");
            for(String mode:new String[]{"truncated","corrupt","oversized","read-failure"}){
                Path broken=workspace(mode);
                rejects(()->install(broken,s->{if(!s.endsWith("0005.bin"))return resource(s);
                    byte[] bytes=Files.readAllBytes(payload.resolve(s));
                    if(mode.equals("truncated"))return new ByteArrayInputStream(Arrays.copyOf(bytes,bytes.length-1));
                    if(mode.equals("corrupt")){bytes[43]^=1;return new ByteArrayInputStream(bytes);}
                    if(mode.equals("oversized"))return new ByteArrayInputStream(Arrays.copyOf(bytes,bytes.length+1));
                    throw new IOException("injected read failure");
                }),mode+" rejected");
                check(regularFiles(broken)==0,mode+" commits no target");
            }
            Path zero=workspace("zero-read");install(zero,s->new FilterInputStream(resource(s)){boolean first=true;public int read(byte[] b,int off,int len)throws IOException{if(first){first=false;return 0;}return super.read(b,off,len);}});
            check(regularFiles(zero)==6,"zero-length reads handled");
            Path escaped=workspace("escaped"),outside=workspace("outside");
            rejects(()->IntegrationPayload169.installFiles(escaped.toFile(),s->outside.resolve(s).toFile(),IntegrationPayload169Test::resource),"resolver escape rejected");
            check(regularFiles(escaped)==0&&regularFiles(outside)==0,"escape commits nothing");
            Path symlink=workspace("symlink");Files.createSymbolicLink(symlink.resolve("lib"),outside);
            rejects(()->install(symlink,IntegrationPayload169Test::resource),"symlink escape rejected");check(regularFiles(outside)==0,"symlink destination untouched");
            Path rollback=workspace("rollback");Files.write(rollback.resolve("assets"),new byte[]{4});
            rejects(()->install(rollback,IntegrationPayload169Test::resource),"later destination failure");
            check(regularFiles(rollback)==1&&Files.readAllBytes(rollback.resolve("assets"))[0]==4,"already moved new targets rolled back, original preserved");
            for(boolean ns:new boolean[]{false,true}) {
                Document d=manifest("<uses-permission android:name='old.permission'/><application android:extractNativeLibs='false'><provider android:name='ordinary.Provider'/></application>",ns);
                IntegrationPayload169.requireManifest(d,false);IntegrationPayload169.requireManifest(d,true);
                check(((org.w3c.dom.Element)d.getElementsByTagName("application").item(0)).getAttributeNS("http://schemas.android.com/apk/res/android","extractNativeLibs").equals("true"),"native extraction set");
                ByteArrayOutputStream serialized=new ByteArrayOutputStream();
                javax.xml.transform.TransformerFactory.newInstance().newTransformer().transform(new javax.xml.transform.dom.DOMSource(d),new javax.xml.transform.stream.StreamResult(serialized));
                DocumentBuilderFactory parsedFactory=DocumentBuilderFactory.newInstance();parsedFactory.setNamespaceAware(true);
                Document parsed=parsedFactory.newDocumentBuilder().parse(new ByteArrayInputStream(serialized.toByteArray()));
                check(((org.w3c.dom.Element)parsed.getElementsByTagName("application").item(0)).getAttributeNS("http://schemas.android.com/apk/res/android","extractNativeLibs").equals("true"),"native extraction survives manifest serialization");
                check(d.getElementsByTagName("uses-permission").getLength()==1,"no permission added");
                rejects(()->IntegrationPayload169.requireManifest(manifest("<application><provider android:name='ai.onnxruntime.TelemetryInitializer'/></application>",ns),false),"startup telemetry provider rejected");
                rejects(()->IntegrationPayload169.requireManifest(manifest("<application><provider android:authorities='x.onnxruntime_telemetry_initializer'/></application>",ns),false),"telemetry authority rejected");
            }
            rejects(()->IntegrationPayload169.requireManifest(manifest("",true),true),"missing application rejected");
            rejects(()->IntegrationPayload169.requireManifest(manifest("<application/><application/>",true),true),"duplicate application rejected");
            System.out.println("PASS IntegrationPayload169 checks="+checks+"; real ORT package bytes; no Android/native runtime execution");
        } finally {try(java.util.stream.Stream<Path> files=Files.walk(temporary)){for(Path p:(Iterable<Path>)files.sorted(Comparator.reverseOrder())::iterator)Files.deleteIfExists(p);}}
    }
}
