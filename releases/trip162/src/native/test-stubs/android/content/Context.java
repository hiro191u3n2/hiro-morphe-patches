package android.content;
import java.io.File;
public class Context {
    private final File files;
    public Context(File root) { files = new File(root, "files"); files.mkdirs(); }
    public Context getApplicationContext() { return this; }
    public File getFilesDir() { return files; }
}
