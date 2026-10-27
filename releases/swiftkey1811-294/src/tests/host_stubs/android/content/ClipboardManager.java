package android.content;
public class ClipboardManager {
    public ClipData primaryClip;public int writes;public boolean fail;
    public void setPrimaryClip(ClipData clip){if(fail)throw new SecurityException("PRIVATE_CLIPBOARD_FAILURE");primaryClip=clip;writes++;}
    public ClipData getPrimaryClip(){return primaryClip;}
}
