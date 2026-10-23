import com.android.apksig.ApkVerifier;
import java.io.File;
import java.security.MessageDigest;
import java.util.HexFormat;
public final class VerifyOriginal {
 public static void main(String[] args) throws Exception {
  var r = new ApkVerifier.Builder(new File(args[0])).setMinCheckedPlatformVersion(23).setMaxCheckedPlatformVersion(37).build().verify();
  System.out.println("verified="+r.isVerified());
  System.out.println("v1="+r.isVerifiedUsingV1Scheme()+",v2="+r.isVerifiedUsingV2Scheme()+",v3="+r.isVerifiedUsingV3Scheme());
  for(var cert:r.getSignerCertificates()) {
   System.out.println("certificate_sha256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())));
   System.out.println("subject="+cert.getSubjectX500Principal());
  }
  for(var e:r.getErrors())System.out.println("ERROR="+e);
  for(var w:r.getWarnings())System.out.println("WARNING="+w);
  if(!r.isVerified())throw new IllegalStateException("APK signature verification failed");
 }
}
