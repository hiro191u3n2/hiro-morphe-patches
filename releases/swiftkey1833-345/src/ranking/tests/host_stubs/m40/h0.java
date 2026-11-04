package m40;
public class h0 implements b {
    public final String surface, input;
    public final l40.c request;
    public String separator;
    public h0(String surface, String input, l40.c request) {
        this.surface=surface; this.input=input; this.request=request;
    }
    public String getUserFacingText() { return surface; }
    public void setTrailingSeparator(String value) { separator=value; }
}
