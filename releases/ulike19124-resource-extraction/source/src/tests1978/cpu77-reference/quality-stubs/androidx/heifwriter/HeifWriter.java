package androidx.heifwriter;
import android.os.Handler;
import java.io.IOException;
/** Compile-only ABI stub; never included in production helper DEX. */
public class HeifWriter {
    public void start() throws IOException {}
    public void stop(long timeout) throws Exception {}
    public void close() {}
    public static class Builder {
        public Builder setHandler(Handler handler){return this;}
        public HeifWriter build()throws IOException{return null;}
    }
}
