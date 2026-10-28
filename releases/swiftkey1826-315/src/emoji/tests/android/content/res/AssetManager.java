package android.content.res;
public class AssetManager {
 public java.io.InputStream open(String name)throws java.io.IOException{
  if(!"hiro/emoji_ja.tsv".equals(name))throw new java.io.IOException("Unexpected path "+name);
  return new java.io.FileInputStream(System.getProperty("emoji.dictionary"));
 }
}
