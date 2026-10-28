package android.content.res;
import java.io.*;
public class AssetManager {
  public static byte[] data;
  public static int opens, closes;
  public static boolean failOutOfMemory;
  public InputStream open(String path) throws IOException {
    if (!path.equals("hiro/words_ja.tsv")) throw new IOException("unexpected asset "+path);
    if (failOutOfMemory) throw new OutOfMemoryError("simulated optional dictionary allocation failure");
    opens++;
    return new ByteArrayInputStream(data) { public void close() throws IOException {super.close();closes++;} };
  }
}
