package com.hiro.ulike.hdr.gainmap;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Restricted Main10 HEIF: primary SDR base + hidden numerical RGB gainmap + ISO tmap rendition.
 * Both actual streams must be 4:2:0/10bit BT.2020-NCL/full range. A gainmap is LINEAR, never HLG.
 * This mux does not establish the processed-pair/decoded-pixel relationship: use GainmapSave. */
public final class GainmapContainer {
    private GainmapContainer() {}
    private static final int MAX_STREAM=64*1024*1024;
    public static final class Prepared {
        private final byte[] header,base,map,tmap;
        public final long fileBytes;
        private Prepared(byte[] header,byte[] base,byte[] map,byte[] tmap) {
            this.header=header; this.base=base; this.map=map; this.tmap=tmap;
            fileBytes=(long)header.length+base.length+map.length+tmap.length;
        }
        /** Stream must be empty. On failure discard partial output. Never closes caller's stream. */
        public void writeTo(OutputStream output) throws IOException {
            Objects.requireNonNull(output); output.write(header); output.write(base); output.write(map); output.write(tmap);
        }
        public void saveAtomic(Path destination) throws IOException {
            Path target=Objects.requireNonNull(destination).toAbsolutePath().normalize(),parent=target.getParent();
            if(parent==null || !Files.isDirectory(parent) || Files.isDirectory(target)) throw new IOException("invalid destination");
            Path temp=Files.createTempFile(parent,".ulike-tmap-",".tmp");
            try {
                try(FileOutputStream out=new FileOutputStream(temp.toFile())) { writeTo(out); out.getFD().sync(); }
                Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temp); }
        }
    }
    private static final class Item { final byte[] config,sample; Item(byte[] c,byte[] s) { config=c; sample=s; } }
    public static Prepared prepare(byte[] baseAnnexB,byte[] mapAnnexB,int width,int height,IsoMetadata metadata) {
        if(width<2 || height<2 || (width&1)!=0 || (height&1)!=0 || (long)width*height>GainmapMath.MAX_PIXELS)
            GainmapMath.fail("invalid Main10 geometry");
        Objects.requireNonNull(metadata);
        Item base=item(baseAnnexB,width,height,GainmapSave.Role.SDR_BASE);
        Item map=item(mapAnnexB,width,height,GainmapSave.Role.NUMERICAL_GAINMAP);
        byte[] tmap=cat(new byte[]{0},metadata.copyPayload());
        byte[] ftyp=box("ftyp",cat(ascii("heix"),u32(0),ascii("mif1heixtmap")));
        List<byte[]> properties=new ArrayList<>(); List<byte[]> associations=new ArrayList<>();
        for(int id=1;id<=3;id++) {
            List<byte[]> local=new ArrayList<>();
            local.add(full("ispe",cat(u32(width),u32(height)),0,0));
            local.add(full("pixi",new byte[]{3,10,10,10},0,0));
            if(id<3) local.add(box("hvcC",id==1?base.config:map.config));
            int transfer=id==1?1:id==2?8:16,matrix=id==3?0:9;
            local.add(box("colr",cat(ascii("nclx"),u16(9),u16(transfer),u16(matrix),new byte[]{(byte)128})));
            ByteArrayOutputStream assoc=new ByteArrayOutputStream(); put(assoc,u16(id)); assoc.write(local.size());
            for(int p=0;p<local.size();p++) { properties.add(local.get(p)); assoc.write(properties.size()|((id<3 && p==2)?0x80:0)); }
            associations.add(assoc.toByteArray());
        }
        byte[] iprp=box("iprp",cat(box("ipco",cat(properties.toArray(new byte[0][]))),
                full("ipma",cat(u32(3),cat(associations.toArray(new byte[0][]))),0,0)));
        int[] lengths={base.sample.length,map.sample.length,tmap.length};
        byte[] preliminary=meta(iprp,0,lengths),metadataBytes=meta(iprp,(long)ftyp.length+preliminary.length+8,lengths);
        if(preliminary.length!=metadataBytes.length) throw new AssertionError("unstable item offsets");
        byte[] header=cat(ftyp,metadataBytes,u32(8L+lengths[0]+lengths[1]+lengths[2]),ascii("mdat"));
        return new Prepared(header,base.sample,map.sample,tmap);
    }
    private static Item item(byte[] bytes,int width,int height,GainmapSave.Role role) {
        if(bytes==null || bytes.length>MAX_STREAM) GainmapMath.fail("HEVC byte bound");
        byte[] owned=bytes.clone(); GainmapHevcProof.inspect(owned,width,height,role);
        List<byte[]> nals=GainmapHevcProof.annexB(owned); byte[][] params=new byte[3][];
        ByteArrayOutputStream sample=new ByteArrayOutputStream();
        for(byte[] nal:nals) {
            int type=(nal[0]>>>1)&63;
            if(type>=32 && type<=34) {
                if(nal.length>65535) GainmapMath.fail("hvcC parameter-set length overflow");
                if(params[type-32]!=null && !Arrays.equals(params[type-32],nal)) GainmapMath.fail("changing parameter set");
                params[type-32]=nal;
            } else { put(sample,u32(nal.length)); put(sample,nal); }
        }
        ParameterSets.validate(nals,params);
        byte[] sps=ParameterSets.rbsp(params[1]);
        if(sps.length<13) GainmapMath.fail("short SPS PTL");
        int layers=((sps[0]&255)>>>1)&7,nested=sps[0]&1;
        ByteArrayOutputStream config=new ByteArrayOutputStream(); config.write(1); put(config,Arrays.copyOfRange(sps,1,13));
        put(config,new byte[]{(byte)0xf0,0,(byte)0xfc,(byte)0xfd,(byte)0xfa,(byte)0xfa,0,0,
                (byte)(((layers+1)<<3)|(nested<<2)|3),3});
        for(int i=0;i<3;i++) { config.write(0x80|32+i); put(config,u16(1)); put(config,u16(params[i].length)); put(config,params[i]); }
        return new Item(config.toByteArray(),sample.toByteArray());
    }
    private static byte[] meta(byte[] iprp,long offset,int[] lengths) {
        ByteArrayOutputStream info=new ByteArrayOutputStream(); put(info,u16(3));
        for(int id=1;id<=3;id++) put(info,full("infe",cat(u16(id),u16(0),ascii(id<3?"hvc1":"tmap"),
                ascii(id==1?"SDR10\0":id==2?"numerical RGB gainmap10\0":"HDR alternate\0")),2,id==2?1:0));
        ByteArrayOutputStream loc=new ByteArrayOutputStream(); put(loc,new byte[]{0x44,0}); put(loc,u16(3));
        for(int id=1;id<=3;id++) { put(loc,cat(u16(id),u16(0),u16(1),u32(offset),u32(lengths[id-1]))); offset+=lengths[id-1]; }
        byte[] references=full("iref",box("dimg",cat(u16(3),u16(2),u16(1),u16(2))),0,0);
        byte[] groups=box("grpl",full("altr",cat(u32(4),u32(2),u32(3),u32(1)),0,0));
        return full("meta",cat(full("hdlr",cat(u32(0),ascii("pict"),new byte[12],ascii("hiro processed gainmap\0")),0,0),
                full("pitm",u16(1),0,0),full("iloc",loc.toByteArray(),0,0),full("iinf",info.toByteArray(),0,0),
                iprp,references,groups),0,0);
    }
    private static byte[] ascii(String text) { return text.getBytes(StandardCharsets.US_ASCII); }
    private static byte[] u16(int n) { if(n<0 || n>65535) GainmapMath.fail("u16 overflow"); return new byte[]{(byte)(n>>>8),(byte)n}; }
    private static byte[] u32(long n) {
        if(n<0 || n>0xffff_ffffL) GainmapMath.fail("u32 overflow");
        return new byte[]{(byte)(n>>>24),(byte)(n>>>16),(byte)(n>>>8),(byte)n};
    }
    private static void put(ByteArrayOutputStream out,byte[] data) { out.write(data,0,data.length); }
    private static byte[] cat(byte[]... values) {
        long size=0; for(byte[] v:values) size+=v.length;
        if(size>MAX_STREAM+1024*1024L) GainmapMath.fail("box byte bound");
        ByteArrayOutputStream out=new ByteArrayOutputStream((int)size); for(byte[] v:values) put(out,v); return out.toByteArray();
    }
    private static byte[] box(String type,byte[] payload) { return cat(u32(payload.length+8L),ascii(type),payload); }
    private static byte[] full(String type,byte[] payload,int version,int flags) {
        return box(type,cat(new byte[]{(byte)version,(byte)(flags>>>16),(byte)(flags>>>8),(byte)flags},payload));
    }
}
