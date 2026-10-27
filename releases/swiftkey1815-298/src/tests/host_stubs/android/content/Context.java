package android.content;
public class Context {
    public static final String CLIPBOARD_SERVICE="clipboard";
    public Object clipboardService=new ClipboardManager();
    public Context getApplicationContext(){return this;}
    public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}
    public Object getSystemService(String name){return CLIPBOARD_SERVICE.equals(name)?clipboardService:null;}
}
