package com.hiro.ulike.hdr.gainmap;
import java.nio.file.Path;
/** Reuse only the previous host x265 fixture; production transaction never executes host tools. */
public final class PhotoTestCodecFactory {
    private PhotoTestCodecFactory(){}
    public static GainmapSave.Codec open(Path work,String name){return new GainmapSaveTest.HostCodec(work,name,false);}
}
