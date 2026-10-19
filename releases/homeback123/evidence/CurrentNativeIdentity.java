import java.util.*;import com.android.tools.smali.dexlib2.iface.*;
public class CurrentNativeIdentity {
 public static void main(String[]a)throws Exception{Map<String,ClassDef>x=CurrentApkIdentity.read(a[0]),y=CurrentApkIdentity.read(a[1]);x.keySet().retainAll(CurrentApkIdentity.targets);y.keySet().retainAll(CurrentApkIdentity.targets);CurrentApkIdentity.compare("Native hooked classes CURRENT vs verified122 output",x,y);}
}
