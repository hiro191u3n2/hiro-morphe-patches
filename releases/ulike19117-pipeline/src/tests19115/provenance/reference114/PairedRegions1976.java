package com.hiro.ulike;

/** Exact paired sampling for the immutable production face raster. */
public interface PairedRegions1976 {
    int samplePair1976(int x,int y);
    /** Optional caller-owned cache. Unknown/older owners keep their original
     * per-pixel callbacks and cannot declare an unverified zero mask. */
    default Rows19103 pairRows19103(int width){return null;}
    default boolean empty19103(){return false;}
    interface Rows19103 {void prepare(int y);int sample(int x);}
}
