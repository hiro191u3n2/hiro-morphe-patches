import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Execute the real loader's open/read/hash path on its own complete MPP resources. */
public final class LoaderResourceProbe {
 static void require(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 static Object invoke(Method m,Object...a)throws Exception{try{return m.invoke(null,a);}catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;throw e;}}
 static final class Sized extends InputStream {
  long remaining;Sized(long n){remaining=n;}public int read(){if(remaining==0)return -1;remaining--;return 0;}public int read(byte[] b,int o,int n){if(remaining==0)return -1;int count=(int)Math.min(n,remaining);Arrays.fill(b,o,o+count,(byte)0);remaining-=count;return count;}
 }
 public static void main(String[] a)throws Exception{
  require(a.length==2,"updated.mpp report.json");List<String> verified=new ArrayList<>();
  try(URLClassLoader loader=new URLClassLoader(new URL[]{Path.of(a[0]).toUri().toURL()},LoaderResourceProbe.class.getClassLoader());ZipFile zip=new ZipFile(a[0])){
   Class<?> cls=Class.forName("app.hiro.swiftkey.patches.JapaneseSamsungEmojiPatch",true,loader);
   Method open=cls.getDeclaredMethod("open",String.class),read=cls.getDeclaredMethod("readAsset",InputStream.class),hash=cls.getDeclaredMethod("sha256",byte[].class);open.setAccessible(true);read.setAccessible(true);hash.setAccessible(true);
   Field assetsField=cls.getDeclaredField("ASSETS"),hashesField=cls.getDeclaredField("ASSET_HASHES");assetsField.setAccessible(true);hashesField.setAccessible(true);String[] assets=(String[])assetsField.get(null),hashes=(String[])hashesField.get(null);require(assets.length==8 && hashes.length==8,"asset inventory changed");
   for(int i=0;i<assets.length;i++){
    byte[] actual;try(InputStream stream=(InputStream)invoke(open,"swiftkey/"+assets[i])){actual=(byte[])invoke(read,stream);}
    require(hashes[i].equals(invoke(hash,(Object)actual)),"loader pin differs "+assets[i]);
    try(InputStream expected=zip.getInputStream(zip.getEntry("swiftkey/"+assets[i]))){require(Arrays.equals(actual,expected.readAllBytes()),"loaded bytes differ "+assets[i]);}verified.add(assets[i]);
   }
   byte[] edge=(byte[])invoke(read,new Sized(201326592));require(edge.length==201326592,"exact cap rejected");edge=null;System.gc();
   boolean rejected=false;try{invoke(read,new Sized(201326593));}catch(IllegalStateException expected){rejected=true;}require(rejected,"oversize resource accepted");
  }
  String text="{\"status\":\"PASSED\",\"real_loader_resource_open_read_hash_verified\":true,\"all_eight_resource_hashes_verified\":true,\"exact_192mib_accepted\":true,\"over_192mib_rejected\":true,\"mpp_sha256\":\""+sha(Files.readAllBytes(Path.of(a[0])))+"\",\"device_tested\":false}\n";
  Files.writeString(Path.of(a[1]),text);System.out.println(text);
 }
}
