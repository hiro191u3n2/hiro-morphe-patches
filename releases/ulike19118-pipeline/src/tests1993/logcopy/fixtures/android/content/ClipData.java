package android.content;import android.net.Uri;public class ClipData {public final Uri uri;public final CharSequence label,text;
 public ClipData(Uri value){uri=value;label=null;text=null;}private ClipData(CharSequence label,CharSequence text){uri=null;this.label=label;this.text=text;}
 public static ClipData newPlainText(CharSequence label,CharSequence text){return new ClipData(label,text);}
 public static ClipData newUri(ContentResolver r,CharSequence label,Uri u){return new ClipData(u);}public static ClipData newRawUri(CharSequence label,Uri u){return new ClipData(u);}}
