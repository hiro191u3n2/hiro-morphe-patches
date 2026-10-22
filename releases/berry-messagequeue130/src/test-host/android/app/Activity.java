package android.app;
public class Activity {
    private final Application application;
    public boolean finishing;
    public boolean destroyed;
    public boolean throwOnRequest;
    public int requests;
    public String[] requestedPermissions;
    public int requestCode;
    public int writesAtRequest;
    public Activity(Application application) { this.application = application; }
    public Application getApplication() { return application; }
    public boolean isFinishing() { return finishing; }
    public boolean isDestroyed() { return destroyed; }
    public int checkSelfPermission(String permission) { return application.permission; }
    public void requestPermissions(String[] permissions, int code) {
        requests++;
        requestedPermissions = permissions.clone();
        requestCode = code;
        writesAtRequest = application.preferences.writes;
        if (destroyed || finishing) { throw new AssertionError("Requested on unavailable Activity"); }
        if (throwOnRequest) { throw new SecurityException("private request failure data"); }
    }
}
