package com.hiro.ulike.hdr.heif;

import com.hiro.ulike.hdr.encoder.AndroidMain10Encoder;
import com.hiro.ulike.hdr.encoder.HevcProof;
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

/** Minimal single-item HEIF mux for independently verified BT.2020 HLG Main10 data. */
public final class Main10Heif {
    private static final int MAX_STREAM=64*1024*1024;
    private Main10Heif() {}
    /** Optional clean aperture; coordinates reference the SPS conformance-cropped image. */
    public static final class Crop {
        public final int left,top,width,height;
        public Crop(int left,int top,int width,int height) {
            if(left<0 || top<0 || (left&1)!=0 || (top&1)!=0 || width<1 || height<1)
                throw new IllegalArgumentException("4:2:0 crop requires an even origin and positive dimensions");
            this.left=left; this.top=top; this.width=width; this.height=height;
        }
    }
    public static final class Prepared {
        public final int encodedWidth,encodedHeight,displayWidth,displayHeight;
        public final boolean fullRange;
        public final long fileBytes;
        private final byte[] header,sample;
        Prepared(int w,int h,Crop crop,boolean full,byte[] header,byte[] sample) {
            encodedWidth=w; encodedHeight=h; displayWidth=crop==null?w:crop.width;
            displayHeight=crop==null?h:crop.height; fullRange=full;
            this.header=header; this.sample=sample; fileBytes=(long)header.length+sample.length;
        }
        /** Writes at the current stream position; caller must provide an EMPTY destination.
         * Never closes/flushes caller's stream. On I/O error caller must discard partial data.
         */
        public void writeTo(OutputStream output) throws IOException {
            Objects.requireNonNull(output,"output"); output.write(header); output.write(sample);
        }
        /** Complete temp file, fsync, then same-directory atomic replacement; no non-atomic fallback. */
        public void saveAtomic(Path destination) throws IOException {
            Objects.requireNonNull(destination,"destination");
            Path target=destination.toAbsolutePath().normalize(), parent=target.getParent();
            if(parent==null || !Files.isDirectory(parent) || Files.isDirectory(target))
                throw new IOException("destination parent must exist and target must not be a directory");
            Path temp=Files.createTempFile(parent,".ulike-hlg-",".tmp");
            try {
                try(FileOutputStream out=new FileOutputStream(temp.toFile())) {
                    writeTo(out); out.getFD().sync();
                }
                Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temp); }
        }
    }
    /** Production adapter: output metadata is rechecked against actual bytes by prepare(). */
    public static Prepared fromEncoder(AndroidMain10Encoder.Result result,Crop crop) {
        Objects.requireNonNull(result,"result");
        return prepare(result.copyAnnexB(),result.bitstream.width,result.bitstream.height,result.bitstream.fullRange,crop);
    }
    public static Prepared prepare(byte[] annexB,int width,int height,boolean fullRange,Crop crop) {
        if(annexB==null || annexB.length>MAX_STREAM || width<2 || height<2
                || (width&1)!=0 || (height&1)!=0 || (long)width*height>32_000_000)
            throw new IllegalArgumentException("Main10 source must have even dimensions up to 32 MP and bounded bytes");
        if(crop!=null && ((long)crop.left+crop.width>width || (long)crop.top+crop.height>height))
            throw new IllegalArgumentException("crop outside encoded image");
        // Take ownership before validation; later mutation of caller's byte array cannot change the result.
        byte[] owned=annexB.clone();
        HevcProof.inspect(owned,width,height,fullRange);
        List<byte[]> nals=HevcProof.annexB(owned);
        byte[][] params=new byte[3][]; ByteArrayOutputStream samples=new ByteArrayOutputStream();
        for(byte[] nal:nals) {
            int type=(nal[0]>>>1)&63;
            if(type>=32 && type<=34) {
                if(nal.length>65535) throw new IllegalArgumentException("parameter set too large for hvcC");
                if(params[type-32]!=null && !Arrays.equals(params[type-32],nal))
                    throw new IllegalArgumentException("changing HEVC parameter set");
                params[type-32]=nal;
            } else { put(samples,u32(nal.length)); put(samples,nal); }
        }
        ParameterSets.validate(nals,params);
        if(samples.size()>MAX_STREAM) throw new IllegalArgumentException("HEIF sample too large");
        byte[] sample=samples.toByteArray(),config=configuration(params);
        List<byte[]> props=new ArrayList<>();
        props.add(full("ispe",cat(u32(width),u32(height)),0,0));
        props.add(full("pixi",new byte[]{3,10,10,10},0,0));
        props.add(box("hvcC",config));
        props.add(box("colr",cat(ascii("nclx"),u16(9),u16(18),u16(9),new byte[]{(byte)(fullRange?128:0)})));
        if(crop!=null) props.add(box("clap",cat(u32(crop.width),u32(1),u32(crop.height),u32(1),
                i32(2L*crop.left+crop.width-width),u32(2),i32(2L*crop.top+crop.height-height),u32(2))));
        byte[] ftyp=box("ftyp",cat(ascii("heix"),u32(0),ascii("mif1heix")));
        byte[] preliminary=meta(width,height,props,0,sample.length);
        long offset=(long)ftyp.length+preliminary.length+8;
        byte[] metadata=meta(width,height,props,offset,sample.length);
        if(metadata.length!=preliminary.length) throw new AssertionError("unstable HEIF offset");
        byte[] header=cat(ftyp,metadata,u32((long)sample.length+8),ascii("mdat"));
        return new Prepared(width,height,crop,fullRange,header,sample);
    }
    private static byte[] configuration(byte[][] parameters) {
        byte[] sps=ParameterSets.rbsp(parameters[1]);
        if(sps.length<13) throw new IllegalArgumentException("short SPS profile-tier-level");
        int layers=((sps[0]&255)>>>1)&7, nested=sps[0]&1;
        // general PTL is exactly the 12 bytes following the first SPS byte.
        byte[] ptl=Arrays.copyOfRange(sps,1,13);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        put(out,new byte[]{1}); put(out,ptl);
        put(out,new byte[]{(byte)0xf0,0,(byte)0xfc,(byte)0xfd,(byte)0xfa,(byte)0xfa,0,0,
                (byte)(((layers+1)<<3)|(nested<<2)|3),3});
        for(int i=0;i<3;i++) {
            put(out,new byte[]{(byte)(0x80|32+i)}); put(out,u16(1));
            put(out,u16(parameters[i].length)); put(out,parameters[i]);
        }
        return out.toByteArray();
    }
    private static byte[] meta(int width,int height,List<byte[]> props,long offset,int size) {
        ByteArrayOutputStream properties=new ByteArrayOutputStream(); for(byte[] p:props) put(properties,p);
        byte[] associations=props.size()==5 ? new byte[]{5,1,2,(byte)0x83,4,(byte)0x85} : new byte[]{4,1,2,(byte)0x83,4};
        byte[] iprp=box("iprp",cat(box("ipco",properties.toByteArray()),
                full("ipma",cat(u32(1),u16(1),associations),0,0)));
        byte[] infe=full("infe",cat(u16(1),u16(0),ascii("hvc1"),ascii("HLG10\0")),2,0);
        byte[] iinf=full("iinf",cat(u16(1),infe),0,0);
        byte[] iloc=full("iloc",cat(new byte[]{0x44,0},u16(1),u16(1),u16(0),u16(1),u32(offset),u32(size)),0,0);
        return full("meta",cat(full("hdlr",cat(u32(0),ascii("pict"),new byte[12],ascii("hiro HLG10\0")),0,0),
                full("pitm",u16(1),0,0),iloc,iinf,iprp),0,0);
    }
    private static byte[] ascii(String text) { return text.getBytes(StandardCharsets.US_ASCII); }
    private static byte[] u16(int n) {
        if(n<0 || n>65535) throw new IllegalArgumentException("u16 overflow");
        return new byte[]{(byte)(n>>>8),(byte)n};
    }
    private static byte[] u32(long n) {
        if(n<0 || n>0xffff_ffffL) throw new IllegalArgumentException("u32 overflow");
        return new byte[]{(byte)(n>>>24),(byte)(n>>>16),(byte)(n>>>8),(byte)n};
    }
    private static byte[] i32(long n) {
        if(n<Integer.MIN_VALUE || n>Integer.MAX_VALUE) throw new IllegalArgumentException("i32 overflow");
        return u32(n&0xffff_ffffL);
    }
    private static void put(ByteArrayOutputStream out,byte[] data) { out.write(data,0,data.length); }
    private static byte[] cat(byte[]... values) {
        long count=0; for(byte[] a:values) count+=a.length;
        if(count>MAX_STREAM+1024*1024L) throw new IllegalArgumentException("box exceeds supported bound");
        ByteArrayOutputStream out=new ByteArrayOutputStream((int)count); for(byte[] a:values) put(out,a);
        return out.toByteArray();
    }
    private static byte[] box(String type,byte[] payload) { return cat(u32((long)payload.length+8),ascii(type),payload); }
    private static byte[] full(String type,byte[] payload,int version,int flags) {
        return box(type,cat(new byte[]{(byte)version,(byte)(flags>>>16),(byte)(flags>>>8),(byte)flags},payload));
    }
}
