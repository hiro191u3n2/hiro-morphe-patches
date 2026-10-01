package com.hiro.ulike.hdr.heif;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import com.hiro.ulike.hdr.encoder.HevcProof;

public final class Main10HeifTest {
    private static int checks;
    private static void check(boolean b) { checks++; if(!b) throw new AssertionError("check "+checks); }
    private static void rejects(Runnable r) {
        checks++; try { r.run(); } catch(IllegalArgumentException expected) { return; }
        throw new AssertionError("missing rejection "+checks);
    }
    private static final class Observed extends ByteArrayOutputStream {
        boolean closed,flushed;
        @Override public void close() { closed=true; }
        @Override public void flush() { flushed=true; }
    }
    private static byte[] replace(byte[] stream,int type,byte[] replacement) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(byte[] n:HevcProof.annexB(stream)) {
            byte[] chosen=((n[0]>>>1)&63)==type?replacement:n;
            out.write(0); out.write(0); out.write(0); out.write(1);
            out.write(chosen,0,chosen.length);
        }
        return out.toByteArray();
    }
    private static byte[] pack(byte[] header,String bits) {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); out.write(header[0]); out.write(header[1]);
        while(bits.length()%8!=0) bits+="0";
        int zeros=0;
        for(int i=0;i<bits.length();i+=8) {
            int value=Integer.parseInt(bits.substring(i,i+8),2);
            if(zeros>=2 && value<=3) { out.write(3); zeros=0; }
            out.write(value); zeros=value==0?zeros+1:0;
        }
        return out.toByteArray();
    }
    private static String bits(byte[] rbsp) {
        StringBuilder b=new StringBuilder();
        for(byte v:rbsp) for(int n=7;n>=0;n--) b.append((v>>>n)&1);
        // Remove byte-alignment zeroes while retaining the RBSP stop bit.
        while(b.charAt(b.length()-1)=='0') b.setLength(b.length()-1);
        return b.toString();
    }
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]);
        byte[] source=Files.readAllBytes(root.resolve("hlg_limited.hevc"));
        Main10Heif.Prepared full=Main10Heif.prepare(source,64,32,false,null);
        Observed output=new Observed(); full.writeTo(output);
        check(output.size()==full.fileBytes && !output.closed && !output.flushed);
        byte[] expected=output.toByteArray();
        Arrays.fill(source,(byte)0x55);
        Observed second=new Observed(); full.writeTo(second);
        check(Arrays.equals(expected,second.toByteArray()));
        Path fullPath=root.resolve("hlg_limited.heic");
        Files.write(fullPath,new byte[]{1,2,3}); full.saveAtomic(fullPath);
        check(Arrays.equals(expected,Files.readAllBytes(fullPath)));
        check(full.encodedWidth==64 && full.displayWidth==64 && full.displayHeight==32);
        byte[] fullInput=Files.readAllBytes(root.resolve("hlg_full.hevc"));
        Main10Heif.prepare(fullInput,64,32,true,null).saveAtomic(root.resolve("hlg_full.heic"));
        byte[] padded=Files.readAllBytes(root.resolve("hlg_padded.hevc"));
        Main10Heif.prepare(padded,66,34,false,null).saveAtomic(root.resolve("hlg_padded.heic"));
        Main10Heif.Prepared odd=Main10Heif.prepare(padded,66,34,false,new Main10Heif.Crop(0,0,65,33));
        odd.saveAtomic(root.resolve("hlg_odd_crop.heic"));
        check(odd.encodedWidth==66 && odd.encodedHeight==34 && odd.displayWidth==65 && odd.displayHeight==33);
        Main10Heif.prepare(padded,66,34,false,new Main10Heif.Crop(2,2,63,31)).saveAtomic(root.resolve("hlg_offset_crop.heic"));
        rejects(()->Main10Heif.prepare(padded,66,34,false,new Main10Heif.Crop(2,0,65,33)));
        rejects(()->Main10Heif.prepare(padded,66,34,false,new Main10Heif.Crop(0,2,65,33)));
        rejects(()->Main10Heif.prepare(padded,65,33,false,null));
        rejects(()->new Main10Heif.Crop(-1,0,65,33));
        rejects(()->new Main10Heif.Crop(0,0,0,33));
        rejects(()->new Main10Heif.Crop(1,0,65,33));
        rejects(()->new Main10Heif.Crop(0,1,65,33));
        rejects(()->new Main10Heif.Crop(1,1,65,33));
        rejects(()->Main10Heif.prepare(fullInput,64,32,false,null));
        rejects(()->Main10Heif.prepare(fullInput,32,64,true,null));
        rejects(()->Main10Heif.prepare(new byte[0],64,32,false,null));
        rejects(()->Main10Heif.prepare(null,64,32,false,null));
        byte[] sdr=Files.readAllBytes(root.resolve("sdr8.hevc"));
        rejects(()->Main10Heif.prepare(sdr,64,32,false,null));
        byte[] valid=Files.readAllBytes(root.resolve("hlg_limited.hevc"));
        List<byte[]> nals=HevcProof.annexB(valid);
        byte[] vps=nals.stream().filter(n->((n[0]>>>1)&63)==32).findFirst().get();
        byte[] sps=nals.stream().filter(n->((n[0]>>>1)&63)==33).findFirst().get();
        byte[] pps=nals.stream().filter(n->((n[0]>>>1)&63)==34).findFirst().get();
        byte[] differentVps=vps.clone(); differentVps[2]^=0x10;
        rejects(()->Main10Heif.prepare(replace(valid,32,differentVps),64,32,false,null));
        byte[] differentSps=sps.clone(); differentSps[2]^=0x10;
        rejects(()->Main10Heif.prepare(replace(valid,33,differentSps),64,32,false,null));
        String pp=bits(ParameterSets.rbsp(pps)); check(pp.startsWith("11"));
        byte[] differentPps=pack(pps,"010"+pp.substring(1));
        rejects(()->Main10Heif.prepare(replace(valid,34,differentPps),64,32,false,null));
        byte[] missingSps=pack(pps,"1"+"010"+pp.substring(2));
        rejects(()->Main10Heif.prepare(replace(valid,34,missingSps),64,32,false,null));
        for(byte[] parameter:Arrays.asList(vps,pps)) for(int n=3;n<parameter.length;n++) {
            byte[] truncated=Arrays.copyOf(parameter,n); int type=(parameter[0]>>>1)&63;
            rejects(()->Main10Heif.prepare(replace(valid,type,truncated),64,32,false,null));
        }
        byte[] invalidEscape=Arrays.copyOf(pps,pps.length+4);
        invalidEscape[pps.length+2]=3; invalidEscape[pps.length+3]=4;
        rejects(()->Main10Heif.prepare(replace(valid,34,invalidEscape),64,32,false,null));
        Path directory=root.resolve("directory"); Files.createDirectories(directory);
        try { full.saveAtomic(directory); throw new AssertionError("directory overwritten"); }
        catch(IOException expectedFailure) { check(Files.isDirectory(directory)); }
        int[] wrote={0}; boolean[] closed={false};
        OutputStream failure=new OutputStream() {
            @Override public void write(int b) throws IOException { if(++wrote[0]>20) throw new IOException("injected sink failure"); }
            @Override public void close() { closed[0]=true; }
        };
        try { full.writeTo(failure); throw new AssertionError("sink failure swallowed"); }
        catch(IOException expectedFailure) { check(!closed[0]); }
        try(java.util.stream.Stream<Path> children=Files.list(root)) {
            check(children.noneMatch(p->p.getFileName().toString().startsWith(".ulike-hlg-")));
        }
        System.out.println("{\"status\":\"PASS\",\"checks\":"+checks+",\"android_device_executed\":false}");
    }
}
