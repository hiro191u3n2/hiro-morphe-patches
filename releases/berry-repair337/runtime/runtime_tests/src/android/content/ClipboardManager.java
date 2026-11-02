package android.content;
public class ClipboardManager { public int writes;public ClipData clip;public void setPrimaryClip(ClipData clip){this.clip=clip;writes++;} }
