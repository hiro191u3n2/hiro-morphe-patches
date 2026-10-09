package android.content.pm;public class PackageManager {
 private final android.content.Context context;public PackageManager(android.content.Context context){this.context=context;}
 public PackageInfo getPackageInfo(String name,int flags){PackageInfo info=new PackageInfo();info.versionName=context.versionName;info.code=context.code;info.versionCode=(int)context.code;return info;}
}
