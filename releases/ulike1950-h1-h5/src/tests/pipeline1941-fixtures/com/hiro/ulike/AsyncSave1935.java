package com.hiro.ulike;
/** Host-only snapshot binding: production AsyncSave1935 remains the .38 source. */
public final class AsyncSave1935 {
 public static final ThreadLocal<PhotoDetail.Settings> CAPTURED=new ThreadLocal<PhotoDetail.Settings>();
 public static final ThreadLocal<Boolean> COLOUR=new ThreadLocal<Boolean>();
 public static PhotoDetail.Settings settings(PhotoDetail.Settings s){PhotoDetail.Settings saved=CAPTURED.get();return saved==null?s:saved;}
 public static boolean chroma(boolean live){Boolean saved=COLOUR.get();return saved==null?live:saved.booleanValue();}
}
