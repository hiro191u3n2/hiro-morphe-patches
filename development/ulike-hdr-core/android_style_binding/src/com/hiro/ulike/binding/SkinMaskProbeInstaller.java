package com.hiro.ulike.binding;
import java.io.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;

/** Private diagnostic output only: sampled mask alpha plus two calibration UV
 * ramps. The photo used for HDR processing is never replaced by this output.
 * Uses the original mask sampler, vertex shader, material and earlier graph.
 */
public final class SkinMaskProbeInstaller {
 private SkinMaskProbeInstaller(){}
 public static final class Probe {
  public final StyleObserverInstaller.Installation observers;
  public final String layout="R=sampled skin mask alpha, G=clip-derived u, B=clip-derived v, A=1";
  public final boolean originalMaskPrecisionPreserved=false,deviceCallbackCalibrated=false;
  private Probe(StyleObserverInstaller.Installation i){observers=i;}
 }
 private static final String MASK_SHADER="precision highp float;\nuniform sampler2D maskTexture;\nvarying vec2 texcoord1;\nvoid main() {\n  float mask = texture2D(maskTexture, vec2(texcoord1.x, 1.0 - texcoord1.y)).a;\n  gl_FragColor = vec4(mask, texcoord1.x, texcoord1.y, 1.0);\n}\n";
 private static final String PASS_SHADER="precision highp float;\nuniform sampler2D inputImageTexture;\nvarying vec2 uv0;\nvoid main() { gl_FragColor = texture2D(inputImageTexture, uv0); }\n";
 private static void require(boolean b,String s){if(!b)throw new IllegalArgumentException(s);}
 private static byte[] read(File f)throws IOException{require(f.isFile()&&f.length()<=1048576,"bounded diagnostic source required");byte[] out=new byte[(int)f.length()];try(DataInputStream in=new DataInputStream(new FileInputStream(f))){in.readFully(out);require(in.read()==-1,"source changed while reading");}return out;}
 private static String digest(byte[] b)throws Exception{StringBuilder s=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(b))s.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return s.toString();}
 private static void pin(File f,String sha)throws Exception{require(digest(read(f)).equals(sha),"diagnostic shader/material pin mismatch");}
 private static void write(File f,String source)throws IOException{try(OutputStream out=new FileOutputStream(f)){out.write(source.getBytes(StandardCharsets.UTF_8));}}
 private static void remove(File f)throws IOException{if(f.isDirectory()){File[] children=f.listFiles();if(children==null)throw new IOException("private cleanup listing");for(File c:children)remove(c);}if(!f.delete())throw new IOException("private cleanup failed");}
 public static Probe install(File archive,String styleId,File appPrivateParent,String newChild,int nonce)throws Exception{
  StyleObserverInstaller.Installation i=StyleObserverInstaller.install(archive,styleId,appPrivateParent,newChild,nonce);
  try{boolean natural=ShotStyleSettings.NATURAL.equals(styleId);String feature=natural?"AmazingFeature2":"AmazingFeature8";File skin=new File(i.directory,feature+"/xshader/skinseg.frag");
   pin(skin,"9168eb81e4b3245952514803ded08f215a994d06cb31403ad83d020e1346574b");pin(new File(i.directory,feature+"/xshader/skinseg.vert"),"f7f225af3695fa06b2158d365858590aea878896385a8b3f3d87d6638660939a");
   pin(new File(i.directory,feature+"/material/SkinSeg.material"),natural?"9a3eac77c30d656d3223ba61158f398d6ca4606ea89f7c834ffd9091ac697134":"c092df1d96053d9a4a4e1bcc8baf763aca5944ebe4bf233a234b8033de01f1b0");
   if(!natural){pin(new File(i.directory,"AmazingFeature9/xshader/pass0.frag"),"89203357634dbd103fca5d8e0c6fe2e2ed7e544f10c57e540be775fcbe41eb96");pin(new File(i.directory,"AmazingFeature9/xshader/pass0.vert"),"b9cb9a031d1e19b6067283c686c60650c613da9051bfe03d75e34749e31351fe");}
   write(skin,MASK_SHADER);if(!natural)write(new File(i.directory,"AmazingFeature9/xshader/pass0.frag"),PASS_SHADER);return new Probe(i);
  }catch(Exception|Error failure){try{remove(i.directory);}catch(Exception cleanup){failure.addSuppressed(cleanup);}throw failure;}
 }
}
