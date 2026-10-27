package hiro.swiftkey;

/** Only the published wrapper ABI; no key-swap implementation is faked here. */
public final class KeySwap {
    public static final class Wrapped {
        public final java.io.InputStream stream;
        public final long cacheKey;
        public Wrapped(java.io.InputStream stream, long cacheKey) {
            this.stream = stream;
            this.cacheKey = cacheKey;
        }
    }
}
