package app.hiro.ulike.patches;

import app.morphe.patcher.patch.ResourcePatchContext;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.w3c.dom.*;

/** Patcher-side installation of the exact official ORT 1.30.0 arm64 package. */
public final class IntegrationPayload169 {
    private static final String ANDROID="http://schemas.android.com/apk/res/android";
    private static final String[][] ITEMS={
        {"lib/arm64-v8a/libonnxruntime.so","df5d25c72a868dca773597c71e2000756d43fe4d70ade516d3693c54e12e0ada","32990480","ulike169/runtime/0000.bin"},
        {"lib/arm64-v8a/libonnxruntime4j_jni.so","0695257178815f8c58c1719ba07d6d5d91a2b339634328cca2a0a98aa7ff2952","111648","ulike169/runtime/0001.bin"},
        {"assets/ulike169/onnxruntime-LICENSE.txt","2f07c72751aed99790b8a4869cf2311df85a860b22ded05fa22803587a48922c","1073","ulike169/runtime/0002.bin"},
        {"assets/ulike169/onnxruntime-ThirdPartyNotices.txt","143764b952fdb1a7c69ce653bfba74a7744d6a8a573bfb73e235fba356c83de3","338088","ulike169/runtime/0003.bin"},
        {"assets/ulike169/onnxruntime-Privacy.md","9703b86132bbe407a7138f9a45689e6996a8ab0ac670529d3699ce753f67538e","4696","ulike169/runtime/0004.bin"},
        {"assets/ulike169/onnxruntime-LICENSE-1DS.txt","cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30","11358","ulike169/runtime/0005.bin"}
    };
    private IntegrationPayload169(){}
    interface Resolver { File resolve(String target)throws Exception; }
    interface Payload { InputStream open(String resource)throws Exception; }

    /** Hook once from UlikeHqMaxPatch.applyAssets after its normal target checks. */
    public static void install(ResourcePatchContext context) {
        try {
            try(app.morphe.patcher.util.Document manifest=context.document("AndroidManifest.xml")) { requireManifest(manifest,false); }
            // Morphe 1.16's generic fileWorkspace is not its decoded-resource
            // directory and may not exist. Its real manifest and root resources
            // are resolved by the resource coder under the manifest's parent.
            File resourceRoot=context.get("AndroidManifest.xml",false).getCanonicalFile().getParentFile();
            installFiles(resourceRoot,target->context.get(target,false),resource->{
                InputStream in=IntegrationPayload169.class.getClassLoader().getResourceAsStream(resource);
                if(in==null)throw new IOException("Missing ORT package resource: "+resource);return in;
            });
            try(app.morphe.patcher.util.Document manifest=context.document("AndroidManifest.xml")) { requireManifest(manifest,true); }
        }catch(Exception failure){throw new IllegalStateException("ULikeの推論ランタイムを組み込めませんでした",failure);}
    }

    static void requireManifest(org.w3c.dom.Document manifest,boolean modify)throws IOException {
        NodeList applications=manifest.getElementsByTagName("application");
        if(applications.getLength()!=1)throw new IOException("Expected one manifest application");
        NodeList providers=manifest.getElementsByTagName("provider");
        for(int i=0;i<providers.getLength();i++){
            Element provider=(Element)providers.item(i);String name=attribute(provider,"name");
            String authorities=attribute(provider,"authorities");
            if(name.equals("ai.onnxruntime.TelemetryInitializer")||authorities.contains("onnxruntime_telemetry_initializer"))
                throw new IOException("ORT startup provider must not be merged; explicit opt-out must run before native loading");
        }
        if(modify){
            Element app=(Element)applications.item(0);
            // Morphe may parse without namespace awareness. Remove the old
            // qualified attribute as well, otherwise serialization can retain
            // its false value alongside the new namespaced attribute.
            app.removeAttribute("android:extractNativeLibs");
            app.removeAttributeNS(ANDROID,"extractNativeLibs");
            app.setAttributeNS(ANDROID,"android:extractNativeLibs","true");
        }
    }
    private static String attribute(Element e,String name){String n=e.getAttributeNS(ANDROID,name);return n.isEmpty()?e.getAttribute("android:"+name):n;}

    /** Only modifies a patcher's private resource workspace, never the source APK. */
    static void installFiles(File directory,Resolver resolver,Payload payload)throws Exception {
        Path root=directory.getCanonicalFile().toPath();
        if(!Files.isDirectory(root))throw new IOException("Patcher workspace missing");
        List<Path> stages=new ArrayList<>(),targets=new ArrayList<>(),created=new ArrayList<>();
        Throwable primary=null;
        try{
            for(String[] item:ITEMS){
                Path target=resolver.resolve(item[0]).getCanonicalFile().toPath();
                if(!target.startsWith(root)||target.equals(root))throw new IOException("Payload target escapes patcher workspace");
                long size=Long.parseLong(item[2]);
                if(Files.exists(target)){
                    if(!Files.isRegularFile(target)||Files.size(target)!=size||!digest(target).equals(item[1]))
                        throw new IOException("Conflicting existing runtime resource: "+item[0]);
                    continue;
                }
                Path temporary=Files.createTempFile(root,"ulike-ort-stage-",".tmp");stages.add(temporary);targets.add(target);
                try(InputStream input=payload.open(item[3]);OutputStream output=Files.newOutputStream(temporary)){
                    MessageDigest sha=sha();byte[] buffer=new byte[65536];long total=0;
                    for(;;){int n=input.read(buffer);if(n<0)break;if(n==0){int b=input.read();if(b<0)break;buffer[0]=(byte)b;n=1;}
                        if(n>size-total)throw new IOException("Oversized runtime payload");
                        total+=n;sha.update(buffer,0,n);output.write(buffer,0,n);
                    }
                    if(total!=size||!hex(sha.digest()).equals(item[1]))throw new IOException("Runtime payload length/SHA mismatch: "+item[0]);
                }
            }
            // All inputs and pre-existing files pass before adding any destination.
            // The owning patcher must serialize access to its private workspace.
            for(int i=0;i<stages.size();i++){
                Path target=targets.get(i);Files.createDirectories(target.getParent());
                if(Files.exists(target))throw new IOException("Runtime target appeared during staging");
                Files.move(stages.get(i),target);created.add(target);
            }
        }catch(Exception|Error failure){primary=failure;
            for(int i=created.size()-1;i>=0;i--)try{Files.deleteIfExists(created.get(i));}catch(Exception cleanup){failure.addSuppressed(cleanup);}
            throw failure;
        }finally{
            IOException cleanupFailure=null;
            for(Path temporary:stages)try{Files.deleteIfExists(temporary);}catch(IOException cleanup){
                if(primary!=null)primary.addSuppressed(cleanup);else if(cleanupFailure==null)cleanupFailure=cleanup;else cleanupFailure.addSuppressed(cleanup);
            }
            if(primary==null&&cleanupFailure!=null)throw cleanupFailure;
        }
    }
    static String digest(Path file)throws IOException{MessageDigest m=sha();try(InputStream in=Files.newInputStream(file)){byte[] b=new byte[65536];for(int n;(n=in.read(b))!=-1;)m.update(b,0,n);}return hex(m.digest());}
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(Character.forDigit((b&255)>>>4,16)).append(Character.forDigit(b&15,16));return s.toString();}
}
